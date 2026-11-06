# WatchManagement (MSS301 Slot 18)

Four services: API Gateway :8080, MSAccount :8081, MSBrand :8082 and MSWatch :8083. Each service uses a separate SQL Server database. Java 21 / Spring Boot 4.1.1.

## Run locally

1. Start SQL Server and create DBAccount, DBBrand and DBWatch (see `sql/create-databases.sql`).
2. Copy `.env.example` to `.env` at this folder and set credentials and a strong JWT secret.
3. In each service folder run `./mvnw spring-boot:run`; start account, brand, watch, then gateway.
4. Import `WatchManagement_Saga.postman_collection.json` into Postman and configure `baseUrl`, `adminToken` as prompted by the login request.

Or run `docker compose up --build` from this directory. Seeded logins use password `@1`; do not use the sample secrets outside local development.

Saga design and compensation order: `docs/SAGA_DESIGN.md`.
