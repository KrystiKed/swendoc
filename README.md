# swendoc

Dokumentenverwaltung: Spring Boot REST API, PostgreSQL via JPA/Hibernate,
MinIO fuer die Datei-Blobs.

## Starten

Alles in Containern (Web-UI/nginx, REST-Server, PostgreSQL, MinIO):

```
docker compose up --build   # UI auf http://localhost, API auf http://localhost:8080
```

Nur die Infrastruktur im Container, App lokal:

```
docker compose up -d postgres minio
./mvnw spring-boot:run      # API auf http://localhost:8080
./mvnw test                 # Tests, laufen ohne Docker (H2)
```

Frontend (Angular, `frontend/`) im Dev-Modus, API-Calls werden per
`proxy.conf.json` an `localhost:8080` weitergeleitet:

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

- `/docs`, `/documents/`, `/session`, `/users` -> Reverse Proxy an `app:8080`
- alles andere -> `index.html` (Angular-Routing, z.B. Reload auf `/documents`)
- Uploads bis 50 MB (`client_max_body_size`)

Das Backend wird ueber die Stage `app` gebaut (`target: app` in der Compose).

## REST API

```
POST   /users             {"username": "...", "password": "..."}  -> 201
POST   /session           {"username": "...", "password": "..."}  -> Token

POST   /docs              multipart: title, file
GET    /docs
GET    /docs/{id}         Metadaten
GET    /docs/{id}/content Datei-Download
PUT    /docs/{id}         {"title": "..."}
DELETE /docs/{id}

GET    /documents/group       ["WORD", "PDF", "EXCEL"]
GET    /documents/group/{id}  alle Dokumente eines Typs: word | pdf | excel, sonst 404
```

Der Typ wird beim Upload automatisch erkannt (Content-Type, sonst Dateiendung
.doc/.docx, .pdf, .xls/.xlsx). Andere Dateien werden gespeichert, haben aber
keinen Typ und tauchen in keiner Gruppe auf.

Request- und Response-Bodies gehen ueber DTOs, nicht ueber die Entities:
`DocumentResponse`, `UserResponse`, `SessionResponse`, `CredentialsRequest`,
`TitleRequest`. So verlaesst weder `passwordHash` noch der MinIO-`objectKey`
jemals den Server.

## Persistenz

JPA/Hibernate mappt `Document`, `User` und `Session` auf PostgreSQL. Der Zugriff
laeuft ueber das Repository Pattern: `DocumentRepository`, `UserRepository` und
`SessionRepository` erweitern `JpaRepository`. Die Daten liegen im Docker-Volume
`postgres-data` und ueberleben `docker compose down`.

Passwoerter werden mit PBKDF2-HMAC-SHA256 (210k Iterationen, zufaelligem Salt)
gehasht. Das Session-Token wird ausgegeben, aber noch nicht auf `/docs` erzwungen.
