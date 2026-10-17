# PeakConnect V2

> [!NOTE]
> ### V2 Part 1 (Backend) — COMPLETE
> - **Live Deployment URL:** [https://peak-connect-backend.onrender.com](https://peak-connect-backend.onrender.com)
> - **Completion Date:** October 17, 2026
> - **Status:** Fully integrated, containerized, and deployed to Render Free with Neon PostgreSQL and Upstash Redis. Verified with live regression, dynamic pricing/weather risk caching, guide matching, pessimistic write locking, Razorpay test webhooks, and tiered cancellation.

## Infrastructure & Local Setup

To run PeakConnect V2 locally, you need the following infrastructure services running:
1. **PostgreSQL** (Port 5432)
2. **Redis** (Port 6379)
3. **Prometheus** (Port 9090)
4. **Grafana** (Port 3000)

These are managed via `docker-compose.yml`. You can start them by running:
```bash
docker compose up -d
```

### Accessing Observability Stack Locally
- **Grafana UI**: [http://localhost:3000](http://localhost:3000)
  - **Default Username**: `admin`
  - **Default Password**: `admin`
  - Pre-provisioned Prometheus datasource and dashboards are ready out of the box in the `PeakConnect` folder.
- **Prometheus UI**: [http://localhost:9090](http://localhost:9090)
  - Scrapes the Spring Boot application at `http://host.docker.internal:8080/actuator/prometheus` every 15 seconds.

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
- [x] Task 6: Payment Processing Implementation (Razorpay)
- [x] Task 7: Cancellation Policies + Refund + No-Show
- [x] Task 8: Waitlist Service
- [x] Task 8: Activity Discovery & Caching Implementation
- [x] Task 9: Guide Matching Additions
- [x] Task 10: Advanced Guide Matching Rules
- [x] Task 11: Admin Aggregate-Stats Endpoints
- [x] Task 12: Observability & Metrics Setup (Prometheus + Grafana)
- [x] Task 13: Concurrency & Load Testing (k6)
- [x] Task 14: Containerization & Deployment Setup (Render + Neon + Upstash)

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
- **Cold Starts & Sleep Behavior (Render Free):** Free web services sleep automatically after ~15 minutes of inactivity. The initial incoming HTTP request after an idle window experiences a 45–60 second cold start latency while compute resumes. Spring background scheduled jobs (`WeatherRefreshJob`, `AutoCancelUnpaidBookingsJob`, `WaitlistExpiryJob`, `GuideResponseTimeoutJob`) run only while the container instance is awake, and catch up on their next designated tick once awakened.
- **Free-Tier Limits & Ephemeral Neon Storage:** Neon PostgreSQL (0.5 GiB limit, compute autosuspends on idle) and Upstash Redis (10,000 commands/day). Long idle stretches may cause cloud platforms to pause branches, so free-tier cloud data should be considered non-permanent.
- **Admin Registration Endpoint Exposure:** While `AuthService` rejects registration requests specifying `Role.ADMIN`, user registration via the public `/api/auth/register` endpoint allows open self-registration for `TREKKER` and `GUIDE`. Role assignment and administrative privileges should be migrated to an invitation/approval token system before distributing the production URL broadly.
- **Public OSRM Demo Server:** Distance calculations in `LocationMatcher` rely on the public OSRM demo API (`https://router.project-osrm.org`), which is unauthenticated and intended strictly for light demonstration traffic; high request volumes will be rate-limited.
- **Database Migrations:** Currently, the project relies on Hibernate's `ddl-auto=update` for schema management and lacks a dedicated migration tool like Flyway or Liquibase. While `ddl-auto` adds new columns, it does not automatically update existing `CHECK` constraints on modified enums. Schema migrations with Flyway are recommended for future long-term production evolution.

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

---

## Payment Integration (Razorpay)
We integrate with Razorpay to collect a non-refundable deposit to secure a guide booking.

### Booking Flow
1. Trekker confirms a booking.
2. The slot's live price is calculated, and a `DepositCalculator` determines the required non-refundable deposit (default: 20%, configurable via `payment.deposit-percentage`).
3. The server communicates with Razorpay to create an Order and stores the order ID. The booking is saved with status `AWAITING_PAYMENT`.
4. The client uses the Razorpay frontend widget to complete the payment.
5. Razorpay sends a webhook event (`order.paid` / `payment.captured` or `payment.failed`) to our server.
6. The `PaymentWebhookController` receives the event, **verifies the cryptographic signature**, and updates the booking status to `CONFIRMED` or `CANCELLED`. If cancelled, slot capacity is freed.

### Webhook & Testing
- Endpoint: `POST /api/payments/webhook`
- Requires `X-Razorpay-Signature` header.
- You can test this manually using the provided `PeakConnect.postman_collection.json`, which includes requests for "Simulate Webhook Success" and "Simulate Webhook Failure" using dummy signatures. Note: Since signature verification is active, passing a dummy signature will result in a `400 Bad Request`. For end-to-end testing, use the actual Razorpay test dashboard to dispatch webhooks to an exposed local URL (e.g. ngrok).

---

## Cancellation Policies & No-Show
PeakConnect V2 includes customizable cancellation policies per `Activity`.

### Policies
1. **FLEXIBLE**: 100% refund (of non-deposit base amount) > 48h before activity, 50% refund between 12h and 48h, 0% within 12h.
2. **MODERATE**: 100% refund > 7 days before, 50% refund between 48h and 7 days, 0% within 48h.
3. **STRICT**: 50% refund > 14 days before, 0% within 14 days.

*Note: The deposit amount is never refunded, regardless of the policy.*
The thresholds are fully configurable via `application.yml` under `cancellation.*`.

### Refund Handling
When a trekker cancels a `CONFIRMED` booking, the server calculates the refund percentage and the final `refundAmount` based on the activity's policy.
**Currently, the system only records the calculated refund.** A real gateway refund call (e.g. Razorpay refunds API) is pending integration and is logged to the console instead.

### No-Show
Guides have access to a dedicated endpoint (`POST /api/bookings/{id}/no-show`) to flag a booking as a NO_SHOW if the trekker fails to arrive. This endpoint can only be called *after* the slot's scheduled time has passed. A `NO_SHOW` yields a 0% refund, overriding any cancellation policy.

---

## Waitlist Service
PeakConnect V2 includes waitlist functionality for fully booked activities.

### Waitlist Flow
1. **Join Waitlist**: When a Trekker attempts to book a fully occupied slot, the system returns a `409 Conflict` prompting them to join the waitlist. They can hit the `/api/bookings/waitlist` endpoint to join, specifying the slot and their preferred guide.
2. **Promotion (`WaitlistService.promoteNext`)**: When an existing `CONFIRMED` booking is cancelled, the system frees the slot capacity and immediately promotes the earliest `WAITING` waitlist entry. The user is granted a provisional `AWAITING_PAYMENT` booking, reserving the slot for them.
3. **Confirmation**: The promoted user has a configurable window to pay for the booking (default 60 minutes, configurable via `waitlist.confirmation-window-minutes`). Upon payment webhook confirmation, the waitlist entry is marked `CONFIRMED`.

### Background Jobs
To prevent waitlist promotions from blocking slots indefinitely:
- **`WaitlistExpiryJob`**: Runs automatically to scan for `PROMOTED` waitlist entries that have exceeded their 60-minute payment window. These entries are marked `EXPIRED`, their provisional bookings are cancelled, and the slot is immediately offered to the next person on the waitlist.
- **Job Separation**: The generic `AutoCancelUnpaidBookingsJob` (Task 3) ignores waitlist-promoted bookings. This ensures waitlisted users get their dedicated 60-minute window without interference from the standard 15-minute unpaid booking timeout.

---

## Dynamic Pricing Strategies
PeakConnect V2 adds two new dynamic pricing strategies that plug seamlessly into the original `PriceCalculator` composition without altering the base logic:

1. **`LeadTimeStrategy`** (`@Order(3)`): Adjusts price based on how far in advance a booking is made.
   - **Far Advance**: Bookings made >30 days out receive a 5% discount (Multiplier `0.95`).
   - **Last Minute**: Bookings made <3 days out incur a 15% premium (Multiplier `1.15`).
   - *Defaults configurable via:* `pricing.lead-time.far-advance-days` / `pricing.lead-time.last-minute-multiplier` etc.

2. **`DemandStrategy`** (`@Order(4)`): Adjusts price based on recent search/interest volume.
   - **High Demand**: If a slot has >100 views in the rolling 24-hour window, it incurs a 10% premium (Multiplier `1.10`).
   - *Defaults configurable via:* `pricing.demand.high-demand-threshold` / `pricing.demand.high-demand-multiplier`.

### Demand Tracking Mechanism
Demand tracking is powered by a lightweight Redis counter (`DemandTrackerService`).
- Every time a user requests a booking (`POST /api/bookings/request`) or views an activity's details (`GET /api/activities/{id}`), the counter for the respective slot(s) increments (`slot:views:{slotId}`).
- The counter is given a 24-hour Time-to-Live (TTL) upon first increment.
- The 5-minute TTL on the `priceCache` (configured in Task 2) has intentionally been left exactly as-is. It provides a natural smoothing effect to dynamic pricing, preventing thundering herds on the database/Redis, and ensures prices do not fluctuate wildly by the second for users currently checking out.

---

## Guide Matching Additions (Task 9)
PeakConnect V2 adds advanced, highly configurable guide matching logic to the existing rule-based system. 

### Matchers Added/Updated
1. **`LocationMatcher` (Upgraded)**: Originally a simple string comparison. Now uses real route distance from the free public OSRM driving API (`OsrmRoutingClient`) using the Guide and Activity `latitude`/`longitude`. Safely falls back to string comparison on missing coordinates or OSRM failure.
2. **`LanguageMatcher` (New)**: Scores guides dynamically based on the percentage of their languages that overlap with the authenticated Trekker's `preferredLanguages`.
3. **`HybridMatcher` (New)**: Instead of the default static V1 weighting (60% skill / 40% location), `HybridMatcher` aggregates all matchers and applies weights (`skillWeight`, `locationWeight`, `languageWeight`) dynamically passed per request.

### Caching Considerations
Because Trekker language preferences and request weights affect the outcome, the `@Cacheable` key for `guideAvailabilityCache` has been expanded from `#slot.id.toString()` to include `#trekkerEmail`, `#skillWeight`, `#locationWeight`, and `#languageWeight`. This cleanly prevents cross-user and cross-weight cache poisoning.

---

## Guide Accept/Decline & Auto-Rematch (Task 10)
PeakConnect V2 adds interactive guide assignments to the previously read-only booking system, ensuring active acceptance before trekking payments are processed.

### Reordered Payment Flow
1. **Assignment (`/confirm`)**: When a Trekker selects a guide, the booking enters `PENDING_GUIDE_RESPONSE` (and records `lastGuideAssignedAt`). No Razorpay payment order is created yet.
2. **Acceptance (`/accept`)**: The assigned guide reviews the booking and hits the accept endpoint. At this point, the system calculates the required deposit based on the locked slot price, creates the Razorpay payment order, and sets the booking to `AWAITING_PAYMENT`.
3. **Payment**: Trekker completes checkout (existing flow).

### Auto-Rematch & Decline
- **Manual Decline (`/decline`)**: If a guide declines, their ID is saved to the booking's `declinedGuideIds` list. The system then automatically triggers `rematchGuide`, reusing the `HybridMatcher` (with V1 default weights) to find the next best available guide, excluding the declined guides. The new guide is assigned and status resets to `PENDING_GUIDE_RESPONSE`.
- **Auto-Decline (Timeout)**: The `GuideResponseTimeoutJob` runs periodically to scan for bookings in `PENDING_GUIDE_RESPONSE` older than `guide.response-timeout-minutes` (default: 20 minutes). If found, the guide is automatically considered declined, added to the exclusion list, and the same auto-rematch logic fires.
- **Failure**: If no other guides are available during a rematch, the booking status falls to `REMATCH_FAILED`, awaiting manual admin/trekker intervention.

---

## Admin Aggregate-Stats Endpoints (Task 11)
PeakConnect V2 includes new internal endpoints to power a future operational dashboard. These endpoints summarize daily activity and operational health. All endpoints require the `ADMIN` role.

### Endpoints Added:
1. **`GET /api/admin/stats/bookings-today`**: Returns the count of bookings created and the subset that are confirmed within the current calendar day.
2. **`GET /api/admin/stats/cache-hit-rate`**: Returns absolute hits, misses, and the overall hit rate (0.0 to 1.0) derived directly from custom `AtomicLong` counters embedded within the Two-Level Cache architecture.
3. **`GET /api/admin/stats/waitlists`**: Returns the count of waitlist entries currently in `WAITING` or `PROMOTED` status, broken down by `Slot ID`.
4. **`GET /api/admin/stats/guide-responses`**: Analyzes the historical interaction of guides across all bookings (including declined/timed-out historical assignments preserved in the `declinedGuideIds` / `timedOutGuideIds` collections) to provide an aggregate `acceptRate` and `declineRate`.

---

## Observability & Metrics Setup (Task 12)
PeakConnect V2 includes a production-grade observability and metrics stack using **Micrometer**, **Prometheus**, and **Grafana**.

### Custom Micrometer Metrics Added
1. **`booking.events` (Counter)**:
   - Tracks booking lifecycle outcomes.
   - Tags:
     - `status`: `"success"` or `"failure"`
     - `reason`:
       - `"confirmed"`: Recorded when a booking payment is completed via Razorpay webhook.
       - `"payment-failed"`: Recorded when Razorpay webhook issues a payment failure event.
       - `"auto-cancelled"`: Recorded when `AutoCancelUnpaidBookingsJob` or `WaitlistExpiryJob` auto-cancels unpaid bookings.
       - `"guide-declined-no-rematch"`: Recorded when auto-rematch exhausts all available guides and fails.
       - `"user-cancelled"`: Recorded when a trekker cancels a booking via `/api/bookings/{id}/cancel`.
2. **`booking.endpoint.latency` (Timer)**:
   - Measures latency and throughput for booking API endpoints with p50/p95 percentiles and histogram buckets.
   - Tag: `endpoint` (`"request"`, `"confirm"`, `"my"`, `"cancel"`, `"no-show"`, `"accept"`, `"decline"`, `"waitlist"`).
3. **`cache.requests` (Counter)**:
   - Reuses and extends Task 2 / Task 11's two-level cache (`TwoLevelCache`) without duplicating logic.
   - Tags:
     - `cache`: Cache name (`"riskCache"`, `"guideAvailabilityCache"`, `"priceCache"`).
     - `result`: `"hit"` or `"miss"`.
4. **`external.api.latency` (Timer)**:
   - Measures external partner latency, call counts, and failure rates with p50/p95 percentiles.
   - Tags:
     - `api`: `"weather"` (OpenWeatherMap), `"osrm"` (OSRM routing), `"payment"` (Razorpay).
     - `status`: `"success"` or `"error"`.
5. **Circuit Breakers & Retries (Resilience4j + Micrometer)**:
   - Pre-initializes circuit breakers (`weatherApi`, `osrmApi`, `paymentApi`) at application startup.
   - Publishes gauges: `resilience4j_circuitbreaker_state` (1 for current state: CLOSED, OPEN, HALF_OPEN), `resilience4j_circuitbreaker_failure_rate`, and call counts.
   - Logs state transition events in application logs.

### Docker Compose Services Added
- **`prometheus`**:
  - Image: `prom/prometheus:latest` on port `9090`.
  - Scrapes `/actuator/prometheus` on a 15-second interval via host gateway (`host.docker.internal:8080`).
- **`grafana`**:
  - Image: `grafana/grafana:latest` on port `3000`.
  - Credentials: `admin` / `admin`.
  - Configured with automated datasource provisioning pointing to Prometheus.
  - Automatically loads pre-built dashboards on container startup.

### Provisioned Grafana Dashboards
Dashboards are stored in `grafana/dashboards/` and pre-loaded into the **PeakConnect** folder:

1. **Dashboard 1: "Booking & Cache Health"** (`peakconnect-booking-cache-health`):
   - **Booking Success vs Failure Rate Over Time** (*Time Series*): Real-time rate of confirmed bookings vs failures broken down by failure reason.
   - **Total Booking Events by Reason** (*Stat*): Aggregate distribution of booking outcomes (`confirmed`, `payment-failed`, `auto-cancelled`, `guide-declined-no-rematch`).
   - **Cache Hit Ratio per Cache (%)** (*Time Series*): Percentage hit rates for `riskCache`, `guideAvailabilityCache`, and `priceCache`.
   - **Cache Request Volume (Hits vs Misses)** (*Time Series*): Absolute request throughput showing hits vs misses.
   - **Circuit Breaker Current States** (*Stat*): Real-time state indicator for `weatherApi`, `osrmApi`, and `paymentApi` (CLOSED = Healthy).
   - **Circuit Breaker Failure Rate (%)** (*Time Series*): Failure rate percentages tracked against circuit breaker thresholds.

2. **Dashboard 2: "API Latency & Load"** (`peakconnect-api-latency-load`):
   - **Booking Endpoint Latency (p50 / p95 in ms)** (*Time Series*): 50th and 95th percentile response times per booking action.
   - **Booking Endpoint Request Rate (req/sec)** (*Time Series*): Throughput volume per booking action.
   - **External API Call Latency (Weather / OSRM / Payment)** (*Time Series*): Average and p95 latency for partner integrations.
   - **External API Call Rate & Errors** (*Time Series*): Success and error rates for partner integrations.
   - **System-wide HTTP Request Rate by Status** (*Time Series*): HTTP request rates categorized by response status code (2xx, 4xx, 5xx).
   - **System-wide HTTP Latency (p50 / p95 in ms)** (*Time Series*): Overall application response time distribution.

*Note on Data Visualization:* Metrics are generated dynamically as users navigate activities, request/confirm bookings, or when webhooks/jobs execute. Real dashboard charts update within seconds of requests being processed.

---

## Concurrency & Load Testing (k6)

Full load test results and methodology are documented in [load-test/RESULTS.md](load-test/RESULTS.md).

The pessimistic write locking constraint on the booking endpoint passed the concurrency stress test with 100% correctness: exactly 5 bookings succeeded matching the slot's capacity limit, 2,054 excess requests were safely rejected with HTTP 409 Conflict, and zero overselling occurred across 20 concurrent virtual users generating 6,178 requests.

### Running the Load Test
1. **Seed Test Activity & Slot**:
   ```bash
   ./load-test/seed-data.sh
   ```
2. **Execute k6 Stress Test**:
   ```bash
   k6 run load-test/booking-load-test.js
   ```

---

## Deploying V2 Part 1 Backend (Render + Neon + Upstash)

This section provides the end-to-end guide for deploying the PeakConnect V2 backend to Render Free Web Service, backed by Neon PostgreSQL Free and Upstash Redis Free.

### Architecture & Infrastructure Stack
- **Compute:** Render Web Service (Free tier, 512 MB RAM, Docker runtime).
- **Relational Database:** Neon PostgreSQL (Free tier, direct non-pooled connection, SSL required).
- **Distributed Cache:** Upstash Redis (Free tier, TLS required, token-based auth).
- **Payment Gateway:** Razorpay (Test mode).
- **External Integrations:** OpenWeatherMap API & OSRM public demo routing (`router.project-osrm.org`).
- **Observability:** Prometheus & Grafana remain local-development only (Task 12). In production, `/actuator/prometheus` and `/actuator/metrics` are intentionally disabled to protect sensitive business and system telemetry on Render's public network; only `/actuator/health` and `/actuator/info` are exposed.

### Render Free Resource Constraints & Behaviors
1. **Instance Sleep & Cold Starts:** On Render Free, instances spin down after 15 minutes of inbound HTTP inactivity. The first request after a sleep period may take 45–60 seconds while the container and database compute awake. Subsequent requests respond with sub-50ms latency.
2. **Scheduled Background Jobs:** Spring `@Scheduled` background jobs (`WeatherRefreshJob`, `AutoCancelUnpaidBookingsJob`, `WaitlistExpiryJob`, `GuideResponseTimeoutJob`) run only while the instance is awake. When the service awakens from sleep upon incoming traffic, the scheduled jobs catch up on their next designated tick.
3. **Database Idle Suspension (Neon):** Neon automatically suspends compute when idle. The HikariCP connection pool is configured with `connection-timeout: 30000` (30 seconds) and `max-lifetime: 600000` (10 minutes) so connections re-establish smoothly without timeout exceptions.
4. **Free Tier Quota Management:** Neon provides 0.5 GiB storage and Upstash provides 10,000 requests/day on their free tiers. Avoid upgrading plans; monitor dashboards periodically.

---

### Step-by-Step Deployment Instructions

#### 1. Provision Cloud Databases
1. **Neon PostgreSQL**:
   - Create a free project on [neon.tech](https://neon.tech).
   - Copy the direct connection string. Ensure it uses JDBC format with SSL mode:
     `jdbc:postgresql://<neon-hostname>/neondb?sslmode=require`
   - Note the database username and password.
2. **Upstash Redis**:
   - Create a free Redis database on [upstash.com](https://upstash.com).
   - Ensure **TLS (SSL)** is enabled.
   - Note the endpoint hostname (`<endpoint>.upstash.io`), port (`6379`), and password.

#### 2. Create Render Web Service
1. Log in to [render.com](https://render.com) and click **New + > Web Service**.
2. Connect your GitHub repository (`PeakConnect`).
3. Configure service settings:
   - **Name:** `peakconnect-backend` (or your chosen service name)
   - **Branch:** `v2-part1-backend`
   - **Root Directory:** `backend` *(CRITICAL: The Spring Boot Maven project and Dockerfile live in `backend/`)*
   - **Runtime:** `Docker`
   - **Instance Type:** `Free`
   - **Health Check Path:** `/actuator/health`

#### 3. Configure Environment Variables in Render
Under the **Environment** tab of your Render Web Service, add the following environment variables (values should match your cloud credentials):

| Render Environment Variable | Source / Mapping from Local `.env` | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates `application-prod.yml` configuration |
| `DB_URL` | `DB_URL` | Neon JDBC URL (`jdbc:postgresql://.../neondb?sslmode=require`) |
| `DB_USERNAME` | `DB_USERNAME` | Neon database username |
| `DB_PASSWORD` | `DB_PASSWORD` | Neon database password |
| `REDIS_HOST` | `REDIS_HOST` | Upstash Redis host (`...upstash.io`) |
| `REDIS_PORT` | `REDIS_PORT` | Upstash Redis port (`6379`) |
| `REDIS_PASSWORD` | *(New for Cloud)* | Upstash Redis password / token |
| `REDIS_SSL` | `true` | Enforces TLS connection to Upstash Redis |
| `JWT_SECRET` | `JWT_SECRET` | 256/384-bit secret for signing JWT tokens |
| `OPENWEATHER_API_KEY` | `OPENWEATHER_API_KEY` | OpenWeatherMap API key |
| `OSRM_BASE_URL` | `OSRM_BASE_URL` | `https://router.project-osrm.org` |
| `RAZORPAY_KEY_ID` | `RAZORPAY_KEY_ID` | Razorpay Test Key ID (`rzp_test_...`) |
| `RAZORPAY_KEY_SECRET` | `RAZORPAY_KEY_SECRET` | Razorpay Test Key Secret (also used for webhook signatures) |
| `JAVA_OPTS` | *(Optional)* | Memory flags for 512MB RAM: `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC` |

*(Note: Render automatically injects `PORT=10000`, which our `Dockerfile` and `application-prod.yml` read via `${PORT:8080}`)*

#### 4. Configure Razorpay Webhook
1. Go to [Razorpay Dashboard](https://dashboard.razorpay.com/) > **Settings** > **Webhooks**.
2. Click **Add New Webhook**:
   - **Webhook URL:** `https://<your-render-service-url>.onrender.com/api/payments/webhook`
   - **Secret:** Enter the exact value set in `RAZORPAY_KEY_SECRET`.
   - **Active Events:** Check `order.paid`, `payment.captured`, and `payment.failed`.
3. Save the webhook.

---

## First-Run Data Setup (Empty Production Database)

When deploying to a fresh Neon database for the first time, Hibernate automatically builds the schema tables via `ddl-auto: update`, and the Spring Boot `DataSeeder` creates the initial administrator account.

Follow these steps using the existing REST endpoints to populate the system:

### 1. Authenticate as the Default Administrator
The default administrator is automatically initialized on the first boot:
```bash
curl -X POST https://<your-render-url>/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "admin123"}'
```
*Save the returned `token` as `ADMIN_TOKEN`.*

### 2. Create the First Activity
```bash
curl -X POST https://<your-render-url>/api/admin/activities \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Hampta Pass Trek",
    "description": "High altitude Himalayan pass connecting Kullu and Lahaul valleys.",
    "location": "Manali",
    "latitude": 32.2432,
    "longitude": 77.1892,
    "difficultyLevel": "MODERATE",
    "basePrice": 150.00,
    "cancellationPolicy": "MODERATE"
  }'
```
*Save the returned `id` as `ACTIVITY_ID`.*

### 3. Create a Slot for the Activity
```bash
curl -X POST https://<your-render-url>/api/admin/activities/$ACTIVITY_ID/slots \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-11-15T08:00:00",
    "capacity": 8,
    "season": "PEAK"
  }'
```
*Save the returned `id` as `SLOT_ID`.*

### 4. Register a Guide & Complete Profile
```bash
# Register guide user
GUIDE_TOKEN=$(curl -s -X POST https://<your-render-url>/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Tenzing Norgay", "email": "guide_tenzing@example.com", "password": "password123", "role": "GUIDE"}' | jq -r .token)

# Update guide profile with skills, languages, and coordinates
GUIDE_ID=$(curl -s -X PUT https://<your-render-url>/api/guides/me \
  -H "Authorization: Bearer $GUIDE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "skills": ["High Altitude Trekking", "First Aid", "Navigation"],
    "languages": ["English", "Hindi", "Nepali"],
    "location": "Manali",
    "latitude": 32.2432,
    "longitude": 77.1892,
    "experienceLevel": "EXPERT"
  }' | jq -r .id)

# Admin approves guide
curl -X POST https://<your-render-url>/api/admin/guides/$GUIDE_ID/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 5. Register a Trekker
```bash
curl -X POST https://<your-render-url>/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Alex Explorer", "email": "alex@example.com", "password": "password123", "role": "TREKKER"}'
```

> [!CAUTION]
> **Do NOT run `load-test/seed-data.sh` directly against production!**
> The load test seed script registers 25 test users and generates high-volume test traffic intended exclusively for local stress testing. Running it in production will rapidly exhaust your Neon and Upstash free-tier limits.


