# swendoc

Dokumentenverwaltung: Spring Boot REST API, PostgreSQL via JPA/Hibernate,
MinIO fuer die Datei-Blobs.

## Starten

Alles in Containern (REST-Server, PostgreSQL, MinIO):

```
docker compose up --build   # API auf http://localhost:8080
```

Nur die Infrastruktur im Container, App lokal:

```
docker compose up -d postgres minio
./mvnw spring-boot:run      # API auf http://localhost:8080
./mvnw test                 # Tests, laufen ohne Docker (H2)
```

PostgreSQL: `localhost:5433` (swendoc / secret), 5432 bleibt fuer die lokale
Homebrew-Instanz frei. MinIO Console: http://localhost:9001 (minioadmin / minioadmin)

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
