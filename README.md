# swendoc

Dokumentenverwaltung: Spring Boot REST API, PostgreSQL via JPA/Hibernate,
MinIO fuer die Datei-Blobs.

## Starten

Alles in Containern (Web-UI/nginx, REST-Server, PostgreSQL, MinIO):

```
docker compose up --build   # UI auf http://localhost, API auf http://localhost:8080/api
```

Sobald nginx, REST-Server und Datenbank antworten, gibt der Einmal-Service `ready`
"swendoc started successfully ... Access the web-ui on http://localhost:80" aus.
Mit `-d` steht die Meldung in `docker compose logs ready`. Anderer Host-Port fuer
die UI: `WEBUI_PORT=8081 docker compose up --build`.

Nur die Infrastruktur im Container, App lokal:

```
docker compose up -d postgres minio
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
Homebrew-Instanz frei. MinIO Console: http://localhost:9001 (minioadmin / minioadmin)

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

Alle Images sind auf feste Versionen gepinnt. MinIO kommt aus `pgsty/minio`
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

```
Controller -> Service (Interface) -> ServiceImpl -> Repository (JPA)
                                                 -> FileStorage (MinIO)
```

- Controller (`DocumentController`, `DocumentGroupController`, `UserController`)
  kennen nur die Service-Interfaces `DocumentService`, `DocumentGroupService` und
  `UserService`, nicht die Implementierungen (`...ServiceImpl`).
- Dateien werden ueber das Interface `FileStorage` gespeichert. `MinioFileStorage`
  ist die einzige Klasse, die MinIO direkt aufruft (Package `storage`).
- Eingaben prueft `Validator` (Bean Validation); dieselben Regeln holt sich das
  Frontend ueber `/api/validation/{form}`.

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

## Tests

`./mvnw test` laeuft ohne Docker: H2 statt PostgreSQL, Repositories und
`FileStorage` werden mit Mockito gemockt.

- Service-Tests: `DocumentServiceTest`, `DocumentGroupServiceTest`, `UserServiceTest`
- Controller-Tests (`@WebMvcTest`): `DocumentControllerTest`, `UserControllerTest`,
  `ValidationRulesControllerTest`
- `MinioFileStorageTest`, `PasswordsTest`, `SwendocApplicationTests` (Context startet)

JaCoCo schreibt den Coverage-Report nach `target/site/jacoco/index.html`;
`./mvnw verify` schlaegt fehl, wenn die Line Coverage unter 70% faellt.
