# PeakConnect Concurrency Load Test Results (k6)

## 1. Executive Summary

- **Task**: Task 13 — Concurrency & Capacity Stress Testing
- **Target Endpoint**: `POST /api/bookings/confirm` (preceded by `GET /api/activities/{id}` and `POST /api/bookings/request`)
- **Capacity Constraint**: Slot capacity = **5 spots**
- **Load Profile**: Up to **20 concurrent Virtual Users (VUs)** across 3 stages over **70 seconds** (20s ramp-up, 40s sustained hold, 10s ramp-down)
- **Total Requests**: **6,178 HTTP requests** across **2,059 iterations** (~88 req/s)
- **Capacity-Correctness Result**: **PASS** (100% capacity integrity preserved)
- **Successful Bookings**: Exactly **5** (matching slot capacity)
- **Slot Full Rejections**: **2,054** requests handled gracefully with HTTP `409 Conflict`
- **Unexpected Error Rate**: **0.00%** (0 out of 2,059; 0 5xx errors)
- **Latency (p95)**: **19.35 ms** overall (`POST /api/bookings/confirm` p95 = **20.90 ms**)

---

## 2. Test Architecture & Concurrency Mechanism

The booking flow in PeakConnect is the most concurrency-sensitive path in the application. Preventing double-booking and overselling is enforced via a two-layer defense mechanism:

1. **Application Layer (JPA Pessimistic Write Lock)**:
   In `BookingService.confirmBooking(dto, trekkerEmail)`:
   ```kotlin
   // Acquire Pessimistic Write Lock on the Slot row to serialize concurrent booking attempts
   val slot = slotRepository.findSlotByIdForUpdate(dto.slotId)
       .orElseThrow { ResourceNotFoundException("Slot not found") }

   if (slot.currentOccupancy >= slot.capacity) {
       throw ConflictException("Slot is already full. Join waitlist?")
   }

   slot.currentOccupancy++
   slotRepository.save(slot)
   ```
   `SlotRepository.findSlotByIdForUpdate(id)` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`, which generates PostgreSQL `SELECT ... FOR UPDATE`. Any concurrent transaction attempting to book the same slot blocks until the in-flight transaction completes.

2. **Database Layer (Table Check Constraint)**:
   The PostgreSQL `slots` table enforces:
   ```sql
   CONSTRAINT "slots_check" CHECK (current_occupancy <= capacity)
   ```
   If any application bug or isolation anomaly attempted to increment `current_occupancy` beyond `capacity`, the database transaction would immediately abort with a constraint violation.

---

## 3. Test Setup & Reproducibility

The test data was pre-seeded using the repeatable seed script:
```bash
./load-test/seed-data.sh
```

### Seed Parameters:
- **Activity**: `Concurrency Stress Trek 1792225930` (Location: Manali, Base Price: $100.00)
- **Slot ID**: `904182e9-b3c7-4f40-97ea-b7f6c8003a2b`
- **Slot Capacity**: `5`
- **Initial Occupancy**: `0`
- **Verified Guide**: `Guide1` (`d7509423-98ec-4530-b75f-607bb1f3c99b`)
- **Trekker Pool**: 25 pre-registered accounts (`load_trekker_1@test.com` to `load_trekker_25@test.com`) with valid JWT tokens stored in `load-test/test-config.json`.

---

## 4. Test Scenario (`load-test/booking-load-test.js`)

Each of the 20 concurrent virtual users executes the full trekker booking flow on each iteration:
1. **View Activity & Slot**: `GET /api/activities/{activityId}`
   - Hits cached dynamic pricing and risk calculation path.
   - Asserts HTTP 200.
2. **Request Guide Match**: `POST /api/bookings/request`
   - Hits guide matching logic (`HybridMatcher`) and tracks slot view demand.
   - Asserts HTTP 200.
3. **Confirm Booking**: `POST /api/bookings/confirm`
   - Competes for the slot's limited capacity under pessimistic write locking.
   - Evaluates response:
     - `HTTP 200`: Successful booking (increments `successful_bookings` counter).
     - `HTTP 409`: Graceful conflict rejection due to slot full (increments `slot_full_bookings`).
     - Any other status: Unexpected error (increments `unexpected_errors` and `error_rate`).
4. **Pacing**: `sleep(0.5s)` think time between iterations.

---

## 5. k6 Execution Output & Metrics Summary

```
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: load-test/booking-load-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 20 max VUs, 1m40s max duration (incl. graceful stop):
              * default: 20 max VUs for 1m10s (gracefulStop: 30s)

  █ THRESHOLDS 

    booking_confirm_duration
    ✓ 'p(95)<1000' p(95)=20.9ms

    error_rate
    ✓ 'rate<0.01' rate=0.00%

    http_req_duration
    ✓ 'p(95)<1000' p(95)=19.35ms

    successful_bookings
    ✓ 'count<=5' count=5

    unexpected_errors
    ✓ 'count==0' count=0

  █ TOTAL RESULTS 

    checks_total.......: 6177    88.167314/s
    checks_succeeded...: 100.00% 6177 out of 6177
    checks_failed......: 0.00%   0 out of 6177

    ✓ view activity status 200
    ✓ guide match status 200
    ✓ booking confirmed (200)
    ✓ slot full conflict handled (409)

    CUSTOM
    booking_confirm_duration.......: avg=12.6ms   min=3.39ms   med=12.2ms   max=41.56ms  p(90)=18.74ms  p(95)=20.9ms  
    booking_request_duration.......: avg=13.79ms  min=3.01ms   med=9.99ms   max=647.38ms p(90)=14.74ms  p(95)=16.61ms 
    error_rate.....................: 0.00%  0 out of 2059
    slot_full_bookings.............: 2054   29.317737/s
    slot_view_duration.............: avg=10.79ms  min=3.14ms   med=10.55ms  max=30.7ms   p(90)=15.96ms  p(95)=18.53ms 
    successful_bookings............: 5      0.071367/s
    unexpected_errors..............: 0      0/s

    HTTP
    http_req_duration..............: avg=12.39ms  min=3.01ms   med=10.78ms  max=647.38ms p(90)=16.8ms   p(95)=19.35ms 
      { expected_response:true }...: avg=12.3ms   min=3.01ms   med=10.28ms  max=647.38ms p(90)=15.33ms  p(95)=17.8ms  
    http_req_failed................: 33.24% 2054 out of 6178
    http_reqs......................: 6178   88.181588/s

    EXECUTION
    iteration_duration.............: avg=538.68ms min=511.25ms med=535.89ms max=1.17s    p(90)=547.02ms p(95)=550.59ms
    iterations.....................: 2059   29.389105/s
    vus............................: 1      min=1            max=20
    vus_max........................: 20     min=20           max=20

    NETWORK
    data_received..................: 4.8 MB 69 kB/s
    data_sent......................: 3.0 MB 43 kB/s
```

---

## 6. Independent Database Verification

Direct SQL inspection of PostgreSQL immediately following test execution confirmed exact consistency:

### Slot Occupancy Check:
```sql
SELECT id, capacity, current_occupancy FROM slots WHERE id = '904182e9-b3c7-4f40-97ea-b7f6c8003a2b';
```
| id | capacity | current_occupancy |
| :--- | :--- | :--- |
| `904182e9-b3c7-4f40-97ea-b7f6c8003a2b` | **5** | **5** |

### Total Created Bookings Check:
```sql
SELECT count(*) AS total_bookings_created FROM bookings WHERE slot_id = '904182e9-b3c7-4f40-97ea-b7f6c8003a2b';
```
| total_bookings_created |
| :--- |
| **5** |

### Individual Bookings:
| Booking ID | Status | Trekker ID | Guide ID | Created At |
| :--- | :--- | :--- | :--- | :--- |
| `259762ea-5d34-43f6-a7fc-8a44d09b090d` | `PENDING_GUIDE_RESPONSE` | `7480d020-...` | `d7509423-...` | `2026-10-17 14:02:18.80` |
| `5ee67a5d-a4b4-4d9f-9cb9-cb53872d9582` | `PENDING_GUIDE_RESPONSE` | `7480d020-...` | `d7509423-...` | `2026-10-17 14:02:19.37` |
| `41167d1f-2f1b-4f1a-aaaf-1cd65a8bca63` | `PENDING_GUIDE_RESPONSE` | `38d8d14e-...` | `d7509423-...` | `2026-10-17 14:02:19.86` |
| `07152e51-5f41-4d9d-b426-b0714e89c999` | `PENDING_GUIDE_RESPONSE` | `7480d020-...` | `d7509423-...` | `2026-10-17 14:02:19.93` |
| `cf5617eb-cfaa-4093-8348-2931755ea555` | `PENDING_GUIDE_RESPONSE` | `38d8d14e-...` | `d7509423-...` | `2026-10-17 14:02:20.42` |

---

## 7. Plain-Language Conclusion

The `@Transactional` booking constraint and pessimistic write lock (`SELECT ... FOR UPDATE`) flawlessly prevented overselling under 20 concurrent virtual users making 2,059 booking requests. Exactly 5 bookings were created to meet the slot's capacity limit, while all 2,054 excess requests were cleanly and gracefully rejected with HTTP 409 Conflict without deadlocks, database corruption, or 5xx server errors.
