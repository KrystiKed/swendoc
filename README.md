# swendoc

Dokumentenverwaltung: Spring Boot REST API, PostgreSQL via JPA/Hibernate,
MinIO fuer die Datei-Blobs, RabbitMQ als Queue zum OCR-Worker, Angular-Web-UI
hinter nginx.

[![CI](https://github.com/KrystiKed/swendoc/actions/workflows/ci.yml/badge.svg)](https://github.com/KrystiKed/swendoc/actions/workflows/ci.yml)

## Starten

Alles in Containern (Web-UI/nginx, REST-Server, OCR-Worker, PostgreSQL, MinIO, RabbitMQ):

```
docker compose up --build   # UI auf http://localhost, API auf http://localhost:8080/api
```

Sobald nginx, REST-Server und Datenbank antworten, gibt der Einmal-Service `ready`
"swendoc started successfully ... Access the web-ui on http://localhost:80" aus.
Mit `-d` steht die Meldung in `docker compose logs ready`. Anderer Host-Port fuer
die UI: `WEBUI_PORT=8081 docker compose up --build`.

Nur die Infrastruktur im Container, App lokal:

```
docker compose up -d postgres minio rabbitmq
./mvnw spring-boot:run      # API auf http://localhost:8080/api
./mvnw test                 # Tests, laufen ohne Docker (H2)
./mvnw verify               # Tests + JaCoCo-Check (min. 70% Line Coverage)
```

Frontend (Angular, `frontend/`) im Dev-Modus, API-Calls werden per
`proxy.conf.json` (`/api`) an `localhost:8080` weitergeleitet:

```
cd frontend && npm install
npx ng serve                # UI auf http://localhost:4200
npx ng test                 # Frontend-Tests (Vitest)
```

Im Docker-Setup wird das Frontend gebaut und vom nginx-Container (`webui`)
unter http://localhost ausgeliefert. Erstes Login: "New Account..." legt einen
User an (Passwort min. 8 Zeichen).

PostgreSQL: `localhost:5433` (swendoc / secret), 5432 bleibt fuer die lokale
Homebrew-Instanz frei. MinIO Console: http://localhost:9001 (minioadmin / minioadmin),
RabbitMQ Console: http://localhost:15672 (rabbitmqadmin / rabbitmqadmin)

## Web-UI (nginx)

Der Service `webui` in `docker-compose.yml` wird aus der Stage `webui` im
`Dockerfile` gebaut: Angular wird gebaut und von nginx ausgeliefert. Die Config
liegt in `frontend/nginx.conf`:

- `/api/` -> Reverse Proxy an `app:8080` (die ganze REST-API liegt unter `/api`)
- alles andere -> `index.html` (Angular-Routing, z.B. Reload auf `/documents`,
  `/documents/{id}` (Detailseite), `/groups` oder `/profile`)
- Uploads bis 50 MB (`client_max_body_size`)

Das Backend wird ueber die Stage `app` gebaut (`target: app` in der Compose) und
ist eine reine REST-API: es braucht keinen Frontend-Build und liefert keine UI aus.

Die Images sind auf feste Versionen gepinnt, ausser `rabbitmq:4-management` und den
Basis-Images in `ocrworker/Dockerfile` (nur Major-Version). MinIO kommt aus `pgsty/minio`
(Community-Fork), weil `quay.io/minio/minio` nicht mehr veroeffentlicht wird.

## REST API

Alle Pfade liegen unter `/api`. Ausser Registrierung, Login und `/api/validation`
brauchen alle Requests `Authorization: Bearer {token}` (sonst 401).

```
POST   /api/users             {"username": "...", "password": "..."}  -> 201
POST   /api/session           {"username": "...", "password": "..."}  -> Token
DELETE /api/session           (Bearer-Token) -> 204, Token ist danach ungueltig
GET    /api/users/me          (Bearer-Token) -> eigenes Profil
PUT    /api/users/me/password (Bearer-Token) {"currentPassword": "...", "newPassword": "..."} -> 204, 403 bei falschem Passwort

POST   /api/docs              multipart: title, file
GET    /api/docs              nur eigene Dokumente
GET    /api/docs/{id}         Metadaten
GET    /api/docs/{id}/content Datei-Download
PUT    /api/docs/{id}         {"title": "..."}
DELETE /api/docs/{id}

GET/POST /api/groups, GET/PUT/DELETE /api/groups/{id},
PUT/DELETE /api/groups/{id}/documents/{documentId}   eigene Gruppen
GET    /api/validation/{form} Validierungsregeln fuers Frontend
```

Dokumente und Gruppen anderer User existieren fuer einen User nicht (404 statt 403,
damit keine IDs geleakt werden).

Request- und Response-Bodies gehen ueber DTOs, nicht ueber die Entities:
`DocumentResponse`, `DocumentGroupResponse`, `UserResponse`, `SessionResponse`,
`CredentialsRequest`, `TitleRequest`, ... So verlaesst weder `passwordHash` noch
der MinIO-`objectKey` jemals den Server. Entity -> DTO mappen MapStruct-Mapper
(`DocumentMapper`, `DocumentGroupMapper`, `UserMapper`); die Implementierung wird
beim Kompilieren generiert. Fehlt ein Feld im Mapping, bricht der Build ab.

## Architektur

### Container

```
 Browser
    │ http://localhost
    ▼
 ┌────────────────┐  /api/*   ┌──────────────────────┐  Metadaten  ┌────────────┐
 │ webui (nginx)  │ ────────► │ app (Spring Boot)    │ ──────────► │ postgres   │
 │ Angular-UI     │           │ REST-Server :8080    │             └────────────┘
 └────────────────┘           │                      │  Dateien    ┌────────────┐
                              │                      │ ──────────► │ minio      │
                              └──────────┬───────────┘             └────────────┘
                                         │ OcrJobMessage (JSON), Queue "ocr.jobs"
                                         ▼
                              ┌──────────────────────┐             ┌──────────────┐
                              │ rabbitmq             │ ──────────► │ ocrworker    │
                              └──────────────────────┘             │ (Spring Boot)│
                                                                   └──────────────┘
```

Alle Services laufen ueber `docker-compose.yml` in einem Netzwerk, der Service-Name
ist der Hostname (`postgres`, `minio`, `rabbitmq`, `app`). `app` startet erst, wenn die
Healthchecks von Postgres, MinIO und RabbitMQ gruen sind.

### Schichten im REST-Server

```
HTTP -> Controller -> Validator -> Service (Interface) -> ServiceImpl -> Repository (JPA)
                                                                     -> FileStorage (MinIO)
                                                                     -> OcrJobPublisher (RabbitMQ)
        Controller <- Mapper (Entity -> DTO) <-----------------------
Exceptions jeder Schicht -> GlobalExceptionHandler -> HTTP-Status
```

- Controller (`DocumentController`, `DocumentGroupController`, `UserController`)
  kennen nur die Service-Interfaces `DocumentService`, `DocumentGroupService` und
  `UserService`, nicht die Implementierungen (`...ServiceImpl`).
- Dateien werden ueber das Interface `FileStorage` gespeichert. `MinioFileStorage`
  ist die einzige Klasse, die MinIO direkt aufruft (Package `storage`).
- Eingaben prueft `Validator` (Bean Validation); dieselben Regeln holt sich das
  Frontend ueber `/api/validation/{form}`.
- Dependency Injection ueber Spring, immer per Konstruktor (`final`-Felder, in Tests
  einfach mit Mocks instanziierbar).

### Design-Entscheidungen

| Entscheidung | Warum |
|---|---|
| Service- und Storage-Interfaces | Controller/Services haengen nur an Abstraktionen: MinIO austauschbar, alles mockbar |
| DTOs + MapStruct statt Entities in der API | `passwordHash` und `objectKey` koennen nicht leaken; fehlende Felder brechen den Build (`unmappedTargetPolicy=ERROR`) |
| Validierungsregeln nur als Annotationen, Frontend holt sie per `/api/validation/{form}` | eine Quelle fuer Regeln und Meldungen (DE/EN), kein Auseinanderlaufen von Front- und Backend |
| Erst Datei in MinIO, dann DB-Zeile | faellt MinIO aus, entsteht keine Zeile ohne Datei |
| OCR-Job erst nach dem DB-Commit senden | der Worker bekommt nie eine ID, die (noch) nicht in der DB steht |
| Fremde Dokumente/Gruppen -> 404 statt 403 | IDs anderer User lassen sich nicht erraten |
| Eigene Session-Tokens (UUID in Tabelle `sessions`, 12 h) statt JWT | Logout ist serverseitig sofort wirksam, kein Secret-Management |
| `ddl-auto=update` statt Flyway | reicht fuer das Semesterprojekt; Migrationen erst, wenn das Schema stabil ist |

## Queue und OCR-Worker

Nach jedem Upload schickt der REST-Server eine `OcrJobMessage {documentId, objectKey}`
als JSON an die durable Queue `ocr.jobs` (`Config/Rabbitmqconfig`,
`messaging/OcrJobPublisher`). `DocumentServiceImpl` registriert das Senden als
`afterCommit`-Hook der Transaktion: ein zurueckgerollter Upload schickt nichts.

Der OCR-Worker (`ocrworker/`) ist eine eigene Spring-Boot-App ohne Webserver und
ein eigener Container. `OcrJobListener` (`@RabbitListener`) empfaengt die Jobs und
loggt sie; Datei aus MinIO holen und Tesseract folgen in Sprint 4. Queue-Name und
Message-Format muessen in beiden Apps gleich sein (`ocr.jobs`).

Pruefen:

```
docker compose up --build
# in der UI ein PDF hochladen, dann:
docker compose logs ocrworker   # -> "Received OCR job: documentId=..., objectKey=..."
```

Die Queue ist in der RabbitMQ Console unter http://localhost:15672 sichtbar.

## Fehlerbehandlung

Jede Schicht hat eigene Exceptions; `GlobalExceptionHandler` (`@RestControllerAdvice`)
uebersetzt sie zentral in HTTP-Antworten:

| Schicht    | Exception                                                 | Status |
|------------|-----------------------------------------------------------|--------|
| Validierung| `ValidationFailedException`                               | 400 `{"errors": [...]}` |
| Service    | `InvalidCredentialsException`                             | 401    |
| Service    | `WrongPasswordException`                                  | 403    |
| Service    | `DocumentNotFoundException`, `DocumentGroupNotFoundException` | 404 |
| Service    | `UsernameTakenException`                                  | 409    |
| Storage    | `StorageException` (MinIO nicht erreichbar o.ae.)         | 503    |
| sonstige   | jede andere Exception                                     | 500    |

Ausser bei Validierungsfehlern ist der Body ein `ProblemDetail` (RFC 9457).
Bei 503 und 500 steht nur eine allgemeine Meldung im Body, die Ursache landet im Log.

## Logging

SLF4J/Logback (Spring Boot Default), Level in `application.properties`
(`logging.level.at.technikum.swendoc`):

- `INFO`: Registrierung, Login, Logout, Passwortwechsel, Upload/Umbenennen/Loeschen
  von Dokumenten und Gruppen, MinIO-Bucket beim Start, empfangene OCR-Jobs (Worker)
- `WARN`: fehlgeschlagener Login, falsches aktuelles Passwort
- `ERROR`: Storage-Fehler (503) und unerwartete Exceptions (500) mit Stacktrace
- `DEBUG`: einzelne MinIO-Aufrufe, Validierungsfehler, 404

Geloggt werden IDs statt Titeln/Dateinamen, nie Passwoerter oder Tokens.
Usernamen werden ohne Zeilenumbrueche geloggt, damit niemand Log-Zeilen faelschen kann.

## Persistenz

JPA/Hibernate mappt `Document`, `DocumentGroup`, `User` und `Session` auf
PostgreSQL. Der Zugriff laeuft ueber das Repository Pattern: `DocumentRepository`,
`DocumentGroupRepository`, `UserRepository` und `SessionRepository` erweitern
`JpaRepository`. Die Daten liegen im Docker-Volume `postgres-data` und ueberleben
`docker compose down`.

Passwoerter werden mit PBKDF2-HMAC-SHA256 (210k Iterationen, zufaelligem Salt)
gehasht. Ein unbekannter Username wird gegen einen Dummy-Hash geprueft, damit die
Antwortzeit nicht verraet, ob ein User existiert. Usernamen werden ohne fuehrende/
abschliessende Leerzeichen gespeichert. Das Frontend schickt das Token per
HTTP-Interceptor mit; jede 401 und der Ablauf von `expiresAt` beenden die Session.

## Teststrategie

Ziel: jede Business-Regel und jeder HTTP-Vertrag ist durch einen schnellen Test
abgesichert, der ohne Docker laeuft. `./mvnw test` nutzt H2 statt PostgreSQL;
Repositories, `FileStorage` und `OcrJobPublisher` werden mit Mockito gemockt.

| Ebene | Tests | Was sie absichern |
|---|---|---|
| Business-Logik (Unit, Mockito) | `DocumentServiceTest`, `DocumentGroupServiceTest`, `UserServiceTest` | Ownership (404 fuer Fremde), Upload-Reihenfolge, OCR-Job wird gesendet, Loeschen raeumt Gruppen + Blob auf, Login/Passwort, Log-Injection |
| HTTP-Vertrag (`@WebMvcTest`) | `DocumentControllerTest`, `UserControllerTest`, `ValidationRulesControllerTest` | Statuscodes, JSON-Form, Validierungsmeldungen DE/EN, kein `passwordHash` im Body |
| Adapter / Hilfsklassen | `MinioFileStorageTest`, `PasswordsTest` | MinIO-Fehler -> `StorageException`, Hashing mit Salt |
| Smoke | `SwendocApplicationTests`, `OcrworkerApplicationTests` | Spring-Context startet |
| Frontend (Vitest) | `frontend/src/app/**/*.spec.ts` | API-Client, Validierung, Komponenten |

JaCoCo schreibt den Coverage-Report nach `target/site/jacoco/index.html`;
`./mvnw verify` schlaegt fehl, wenn die Line Coverage unter 70% faellt.
Ein End-to-End-Integrationstest ueber alle Container folgt in Sprint 6.

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) laeuft bei jedem Push und Pull Request:

1. `backend`: `./mvnw -B verify` (Tests + 70%-Coverage-Gate), JaCoCo-Report als Artefakt
2. `ocrworker`: `./mvnw -B verify` im Worker
3. `frontend`: `npm ci`, `ng test`, `ng build`
4. `docker`: `docker compose build`, wenn 1-3 gruen sind (alle Images bauen)

## Projektverwaltung

- Repository: https://github.com/KrystiKed/swendoc
- Kanban-Board (GitHub Project): Github Board
- Commits nach Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, ...)