# LRU Cache with TTL — Spring Boot REST API + Angular UI

A thread-safe LRU (Least Recently Used) Cache implementation with TTL expiry, exposed as a Spring Boot REST API with an Angular frontend. Built as a systems design and concurrency learning project.

---

## Features

- **LRU Eviction** — least recently used entries are evicted when capacity is exceeded
- **TTL Expiry** — entries can be set to expire after a given time
- **Thread Safe** — uses `ReentrantReadWriteLock` for concurrent read/write access
- **Background Eviction** — scheduled daemon thread cleans up expired entries
- **Named Caches** — manage multiple independent caches via `CacheManager`
- **REST API** — interact with and load test your cache via HTTP endpoints
- **Angular UI** — dashboard, load test form, and real-time metrics visualization

---

## Tech Stack

**Backend** — Java 21+, Spring Boot 3.5.0, Maven

**Frontend** — Angular, Angular Material, Chart.js

---

## Getting Started

### Backend
```bash
cd lru-cache
mvn clean spring-boot:run
```
Runs on http://localhost:8080

### Frontend
```bash
cd lru-cache-ui
npm install
ng serve
```
Runs on http://localhost:4200

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cache/{name}/get?key=` | Get a cache entry |
| POST | `/api/cache/{name}/put` | Add a cache entry |
| DELETE | `/api/cache/{name}/remove?key=` | Remove a cache entry |
| POST | `/api/cache-manager/create` | Create a named cache |
| GET | `/api/cache-manager/caches` | List all caches |
| POST | `/api/load-test/run` | Run a load test with parameters |
| POST | `/api/load-test/run/quick?cacheName=users` | Quick run with defaults |

---

## Key Design Decisions

1. **LinkedHashMap** `accessOrder=true` — maintains LRU order automatically
2. **ReentrantReadWriteLock** — concurrent reads, exclusive writes
3. **No lock upgrade** — read lock released before acquiring write lock
4. **AtomicLong counters** — lock-free metrics collection across threads
5. **CORS configured** — Angular (4200) can call Spring Boot (8080)

---

## License

MIT
