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
- [ ] Task 3: Razorpay Payment Integration (Stub)
- [ ] Task 4: Google Maps API Integration
- [ ] Task 5: Resiliency Patterns (Circuit Breaker / Retry)
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
