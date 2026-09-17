# swendoc

*REST API Struktur*

POST/users
POST/session(Auth)

Todo MinIO
POST{metadaten}/docs
GET{metadten}/docs
GET{metadaten}/docs/{id}
PUT{metadaten}/docs/{id}
DELETE{metadaten}/docs/{id}

## Starten

Docker Desktop muss laufen. `./mvnw spring-boot:run` startet Postgres und MinIO
via `compose.yaml` automatisch mit und faehrt sie beim Beenden wieder herunter.

```
./mvnw spring-boot:run     # App auf http://localhost:8080
./mvnw test                # Tests, laufen ohne Docker (H2)
```

MinIO Console: http://localhost:9001 (minioadmin / minioadmin)

## Implementiert

```
POST   /docs              multipart: title, file
GET    /docs
GET    /docs/{id}         Metadaten
GET    /docs/{id}/content Datei-Download
PUT    /docs/{id}         {"title": "..."}
DELETE /docs/{id}
```

Offen: `POST /users`, `POST /session`.
