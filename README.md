# Horror Marathon Ranker

A small web app for a horror movie marathon with friends. Everyone secretly picks one
film nobody has seen, the films are watched in a random order, everyone rates each one,
and the app ranks them.

## Run it

You need Docker, and a `.env` file in this folder:

```
POSTGRES_DB=horror
POSTGRES_USER=horror
POSTGRES_PASSWORD=choose-a-password
TMDB_API_TOKEN=your-tmdb-api-read-access-token
```

Then:

```bash
docker compose up --build
```

Open http://localhost:8080. Without a TMDB token the app still runs; film search is
unavailable and films are added by hand.

## Develop

Run the database in Docker and the two halves on your machine, so changes reload quickly:

```bash
docker compose up -d db
cd backend && ./mvnw spring-boot:run     # API on http://localhost:8080
cd frontend && npm install && npm run dev # UI on http://localhost:5173
```

Run the backend tests with `./mvnw test` from `backend/` (Docker must be running).

## How the front end is served

The React app is built to static files and copied into Spring Boot's `static/` folder
when the image is built (see `backend/Dockerfile`), so the jar serves both the UI and
the API.

This was chosen over a separate web server container because it gives one container,
one URL and one thing to deploy, and the browser talks to a single origin, so no CORS
configuration is needed. The cost is that a front-end change rebuilds the whole image,
which is acceptable for an app this size. In development the Vite dev server proxies
`/api` to Spring Boot instead, to keep hot reloading.

This product uses the TMDB API but is not endorsed or certified by TMDB.
