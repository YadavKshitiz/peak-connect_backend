# PeakConnect V2

## Infrastructure & Local Setup

To run PeakConnect V2 locally, you need the following infrastructure services running:
1. **PostgreSQL** (Port 5432)
2. **Redis** (Port 6379)

These are managed via `docker-compose.yml`. You can start them by running:
```bash
docker compose up -d
```

### Environment Variables
The application relies on a `.env` file at the project root for secrets and configuration. It must contain the following variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `OPENWEATHER_API_KEY`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `GOOGLE_MAPS_API_KEY`
- `REDIS_HOST`
- `REDIS_PORT`

Before running the app locally, load the environment variables:
```bash
set -a && source .env && set +a
```
Then start the application:
```bash
mvn spring-boot:run
```

---

## V2 Part 1 Progress Checklist
- [x] Task 1: Environment, Infrastructure & Project Setup
- [x] Task 2: Redis Caching Setup
- [x] Task 3: Scheduled Jobs (Weather Refresh & Cleanup)
- [x] Task 4: Google Maps API Integration (OSRM Routing Placeholder Added)
- [x] Task 5: Resiliency Patterns (Circuit Breaker / Retry)
- [ ] Task 6: Payment Processing Implementation
- [ ] Task 7: Activity Discovery & Caching Implementation
- [ ] Task 8: Notification Service (Stub)
- [ ] Task 9: Real-time Updates (WebSockets / SSE)
- [ ] Task 10: Advanced Guide Matching Rules
- [ ] Task 11: Dynamic Pricing & Discount Strategies
- [ ] Task 12: Analytics & Metrics Setup
- [ ] Task 13: End-to-End Testing & QA
- [ ] Task 14: Containerization & Deployment Setup

---

## Caching Strategy (Two-Level Cache)

PeakConnect V2 employs a dual-layer caching strategy:
1. **L1 Cache (Caffeine):** A fast, local, in-memory cache with a short TTL (30 seconds) to avoid network trips to Redis for heavily accessed, recently computed values.
2. **L2 Cache (Redis):** A distributed cache that ensures consistency across multiple app instances and handles slightly longer TTLs.

### Configured Caches
- **`priceCache`**: Caches computed final slot prices. Keyed by Slot ID. (L2 TTL: 5 minutes)
- **`riskCache`**: Caches computed weather risk levels. Keyed by Slot ID + Location. (L2 TTL: 15 minutes)
- **`guideAvailabilityCache`**: Caches ranked available guides for a slot. Keyed by Slot ID. (L2 TTL: 2 minutes)

### Task 2 Bug Fixes
During manual verification, two bugs were found and resolved:
- **Bug 1 (Serialization):** Reconfigured `RedisCacheConfiguration` in `CacheConfig.kt` to explicitly use `GenericJackson2JsonRedisSerializer` with a custom `ObjectMapper` (including `KotlinModule`) as the default cache serializer, replacing Java's default serialization.
- **Bug 2 (Lazy Initialization):** Fixed a `JsonMappingException` by explicitly materializing lazy collections (`guide.skills.toList()`, `guide.languages.toList()`) inside the transactional boundary of `GuideMatchingService.matchGuidesForSlot` before mapping to `MatchedGuideResponse`.

---

## Scheduled Jobs
PeakConnect V2 uses Spring `@Scheduled` background jobs to proactively manage caching and orchestrate delayed booking actions.

- **`WeatherRefreshJob` (Every 30 minutes):** Proactively fetches OpenWeatherMap risk levels for all upcoming slots and directly repopulates `riskCache`. This provides a consistent "warm" cache without user-induced latency.
- **`AutoCancelUnpaidBookingsJob` (Every 5 minutes):** Scans for bookings in the newly introduced `AWAITING_PAYMENT` state that are older than 15 minutes. It auto-cancels them, restores slot capacity, and evicts `guideAvailabilityCache` for the slot to ensure fresh guide matching metrics.

*Note on Cache Expiry:* No standalone cron job is implemented to manually "clear" expired cache entries. We rely safely on Caffeine and Redis TTL configurations (e.g., 5 min/15 min) which automatically evict old data. Explicit jobs like `WeatherRefreshJob` simply overwrite keys, making a manual expiry job redundant.

---

## Known Limitations
- **Database Migrations:** Currently, the project relies on Hibernate's `ddl-auto=update` for schema management and lacks a dedicated migration tool like Flyway or Liquibase. While `ddl-auto` adds new columns, it does **not** automatically update existing `CHECK` constraints (e.g., when adding `AWAITING_PAYMENT` to `BookingStatus`). Schema-level constraint updates require manual `ALTER TABLE` intervention on existing databases. We plan to integrate Flyway before production deployment (Task 14) to properly address this.

---

## Resilience Layer (Resilience4j)
We use Resilience4j with AOP (`@CircuitBreaker`, `@Retry`) to wrap all external dependencies, preventing cascading failures.
Each API is configured with:
- **Circuit Breaker:** Sliding Window Size of 10, Failure Rate Threshold of 50%, Wait Duration of 10s, and 3 permitted calls in half-open state.
- **Retry:** Maximum of 3 attempts with 500ms wait duration.

### Configured Clients & Fallbacks
1. **`weatherApi` (`RiskCalculator.kt`)**: On OpenWeatherMap failure, falls back to the last-known cached value from `riskCache`. If no cached value exists, returns `"UNKNOWN (Degraded Mode)"`.
2. **`osrmApi` (`OsrmRoutingClient.kt` - Task 9 Placeholder)**: On routing failure, falls back to returning `-1.0` distance to indicate unavailability safely.
3. **`paymentApi` (`PaymentApiClient.kt` - Task 5 Placeholder)**: On Razorpay failure, falls back to returning `"UNAVAILABLE_DEGRADED"`.

### Manual Testing
To verify the circuit breaker behavior manually:
1. Edit `.env` and set `OPENWEATHER_API_KEY=invalid_key`.
2. Hit any slot-detail endpoint (or run `WeatherRefreshJob`).
3. You will see the fallback returning `"UNKNOWN (Degraded Mode)"` instead of returning a 500 server error. Actuator metrics at `/actuator/metrics` and `/actuator/health` will also reflect the degraded state and circuit breaker transitions.
