# LRU Cache Console

A thread-safe named LRU + TTL cache server with proactive background eviction, asynchronous load testing, and an Angular operations UI.

## Run locally

Backend (Java 25):

```bash
mvn spring-boot:run
```

Frontend:

```bash
cd cache-ui
npm install
npm start
```

Open `http://localhost:4200`. The UI expects the API at `http://localhost:8080/api`; change `cache-ui/src/environments/environment.ts` for another deployment.

Alternatively, run both services with `docker compose up --build`.

## REST API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/caches` | List cache summaries and stats |
| `POST` | `/api/caches` | Create a cache (`name`, `capacity`) |
| `DELETE` | `/api/caches/{name}` | Remove a cache |
| `GET` | `/api/caches/{name}/entries/{key}` | Get an entry |
| `PUT` | `/api/caches/{name}/entries` | Put `{ key, value, ttlMillis? }` |
| `DELETE` | `/api/caches/{name}/entries/{key}` | Delete an entry |
| `POST` | `/api/caches/{name}/loadtest` | Start an asynchronous load test |
| `GET` | `/api/caches/{name}/loadtest/{testId}` | Poll live/final load-test stats |

Load-test POST fields are `threadCount`, optional `opsPerSecond` (omit or `null` for unbounded), `keySpaceSize`, `readWriteRatio` (0–1), `durationSeconds`, and `valueSizeBytes`. The start response is HTTP 202 and includes a `testId` and `QUEUED` or `RUNNING` status.

`cache.eviction.interval-ms` controls the single shared eviction scheduler; Spring environment binding also accepts `CACHE_EVICTION_INTERVAL_MS`.

## Design assumptions

The original repository did not contain the described cache CRUD/list/stats controllers, so the UI and backend were aligned on the `/api/caches` shapes documented above. Values are strings, TTL is supplied per put in milliseconds, hit ratio is a number from 0 to 1, and eviction counts include capacity and TTL evictions.
