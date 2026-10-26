# PeakConnect API Contract & Specification

This document serves as the single source of truth for the PeakConnect REST API consumed by the Android client application.
**Base URL**: `BuildConfig.API_BASE_URL` (default: `https://peak-connect-backend.onrender.com/`)

---

## 1. Key Architectural Notes & Findings

### (a) `GET /api/activities` Slot Population
- **Finding**: `GET /api/activities` returns a list of `ActivityResponse` objects where **each activity includes its list of slots (`slots: List<SlotResponse>`)**. Each slot includes its dynamic price (`currentPrice`) and live weather risk evaluation (`riskLevel`).

### (b) Exact Backend Enum Values
1. **DifficultyLevel**: `EASY`, `MODERATE`, `CHALLENGING`, `EXTREME`
2. **ExperienceLevel**: `BEGINNER`, `INTERMEDIATE`, `EXPERT`
3. **Season**: `LOW`, `SHOULDER`, `PEAK`
4. **CancellationPolicyType**: `FLEXIBLE`, `MODERATE`, `STRICT`
5. **Role**: `TREKKER`, `GUIDE`, `ADMIN`
6. **BookingStatus**: `PENDING`, `PENDING_GUIDE_RESPONSE`, `AWAITING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `NO_SHOW`, `REMATCH_FAILED`
7. **RiskLevel** (API string returned in `SlotResponse.riskLevel`):
   - `"LOW"`, `"MODERATE"`, `"HIGH"`
   - Fallback/unknown strings (e.g., `"UNKNOWN (Degraded Mode)"`) must safely parse to domain `RiskLevel.UNKNOWN`.

### (c) Waitlist Promoted Bookings & Nullable Guide Fields
- When a slot is full, a trekker joins the waitlist with a selected guide (`POST /api/bookings/waitlist`).
- When capacity opens, `WaitlistService.promoteNext` assigns that selected guide and sets status to `PENDING_GUIDE_RESPONSE`.
- If that guide declines or times out, the backend attempts to auto-rematch. If no matching verified guide is available, the backend sets `booking.guide = null` and status to `REMATCH_FAILED`.
- **CRITICAL**: In DTOs, domain models, and Room entities, `guideId` and `guideName` **MUST be nullable**.

### (d) Capabilities Required by App but Missing in Backend
1. **Booking Created-at Timestamp**: Backend `BookingResponse` does not include `createdAt` (only the slot's activity `date`). The app will record local timestamp `statusSinceMillis` and `lastSyncMillis` in Room.
2. **Trekker Profile / Preferred Languages Endpoint**: Backend allows setting `preferredLanguages` only during `POST /api/auth/register`. There is no `GET /api/users/me` or `PUT /api/users/me` to view or update trekker profile or language preferences.
3. **Waitlist Listing Endpoint**: Backend provides `POST /api/bookings/waitlist`, but does not provide `GET /api/waitlist` or `GET /api/bookings/waitlist/my` to list a trekker's active waitlist entries.
4. **Trekker Cancel Reasons**: Backend cancel endpoint takes no body/reason parameter. Cancellation reason is tracked locally or submitted as note if future APIs allow.

---

## 2. Authentication & Health Endpoints

### 2.1 Health Check
- **Method & Path**: `GET /actuator/health`
- **Auth**: None (Public)
- **Timeouts**: Dedicated 10s client
- **Response**: `200 OK`
  ```json
  {
    "status": "UP"
  }
  ```
- **Error States**: `Down` (HTTP 503 or status != "UP"), `Unreachable` (`SocketTimeoutException`, `ConnectException`).

### 2.2 Register
- **Method & Path**: `POST /api/auth/register`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "name": "Alex Trekker",
    "email": "alex@example.com",
    "password": "Password123!",
    "role": "TREKKER",
    "preferredLanguages": ["English", "Hindi"]
  }
  ```
  - Validation: `name` NotBlank, `email` NotBlank + valid email, `password` NotBlank, `role` NotNull (`TREKKER` or `GUIDE`; registration as `ADMIN` is rejected with 400).
- **Response Body**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
- **Errors**:
  - `400 Bad Request`: Validation failure or attempt to register as `ADMIN`.
  - `409 Conflict`: `{"error": "Email is already taken"}`.

### 2.3 Login
- **Method & Path**: `POST /api/auth/login`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "email": "alex@example.com",
    "password": "Password123!"
  }
  ```
  - Validation: `email` NotBlank + valid email, `password` NotBlank.
- **Response Body**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
- **Errors**:
  - `401 Unauthorized`: `{"error": "Authentication failed: Bad credentials"}` -> App maps to user-friendly "Incorrect email or password".

---

## 3. Public Activities & Slots

### 3.1 List Activities
- **Method & Path**: `GET /api/activities`
- **Auth**: None (Public)
- **Query Parameters**:
  - `location`: Optional string (case-insensitive filter).
  - `date`: Optional date string formatted as `yyyy-MM-dd`.
- **Response Body**: `200 OK` List of:
  ```json
  [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Hampta Pass Trek",
      "description": "Cross from Kullu valley into the arid Chandra valley.",
      "location": "Manali",
      "latitude": 32.2396,
      "longitude": 77.1887,
      "difficultyLevel": "MODERATE",
      "basePrice": 9500.00,
      "cancellationPolicy": "MODERATE",
      "slots": [
        {
          "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
          "activityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          "date": "2026-11-01T06:00:00",
          "capacity": 15,
          "currentOccupancy": 4,
          "season": "SHOULDER",
          "currentPrice": 10450.00,
          "riskLevel": "LOW"
        }
      ]
    }
  ]
  ```

### 3.2 Activity Detail
- **Method & Path**: `GET /api/activities/{id}`
- **Auth**: None (Public)
- **Path Parameter**: `id` (UUID)
- **Response Body**: `200 OK` `ActivityResponse` (same structure as single item above).
- **Errors**: `404 Not Found` if activity does not exist.

---

## 4. Bookings & Matching (Trekker)

### 4.1 Request Matching Guides
- **Method & Path**: `POST /api/bookings/request`
- **Auth**: `hasRole('TREKKER')`
- **Request Body**:
  ```json
  {
    "slotId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "skillWeight": 0.40,
    "locationWeight": 0.30,
    "languageWeight": 0.30
  }
  ```
  - Weights are optional `Double?`. If omitted, default V1 weights are applied.
- **Response Body**: `200 OK` List of matched verified guides sorted descending by score:
  ```json
  [
    {
      "guideId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "name": "Tenzing Norgay",
      "skills": ["High Altitude", "Wilderness First Aid", "Rock Climbing"],
      "languages": ["English", "Hindi", "Tibetan"],
      "location": "Manali",
      "experienceLevel": "EXPERT",
      "score": 0.92
    }
  ]
  ```
- **Errors**:
  - `401 Unauthorized` / `403 Forbidden`
  - `404 Not Found` if slot does not exist.

### 4.2 Confirm Booking
- **Method & Path**: `POST /api/bookings/confirm`
- **Auth**: `hasRole('TREKKER')`
- **Request Body**:
  ```json
  {
    "slotId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "guideId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
  }
  ```
- **Response Body**: `200 OK`
  ```json
  {
    "id": "e0b5c1a2-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
    "slotId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "activityTitle": "Hampta Pass Trek",
    "trekkerId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "trekkerName": "Alex Trekker",
    "guideId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "guideName": "Tenzing Norgay",
    "status": "PENDING_GUIDE_RESPONSE",
    "date": "2026-11-01T06:00:00",
    "paymentOrderId": null,
    "depositAmount": null,
    "refundAmount": null,
    "refundPercentage": null
  }
  ```
- **Errors**:
  - `404 Not Found`: Slot or Guide not found.
  - `409 Conflict`: Slot is full (`currentOccupancy >= capacity`). Trekker must join waitlist.

### 4.3 My Bookings
- **Method & Path**: `GET /api/bookings/my`
- **Auth**: `hasRole('TREKKER')`
- **Response Body**: `200 OK` List of `BookingResponse`.

### 4.4 Cancel Booking
- **Method & Path**: `POST /api/bookings/{id}/cancel`
- **Auth**: `hasRole('TREKKER')`
- **Response Body**: `200 OK` `BookingResponse` with `status: "CANCELLED"`, `refundPercentage`, and `refundAmount`.
- **Errors**: `400 Bad Request` if booking cannot be cancelled or is already cancelled; `404 Not Found`.

### 4.5 Join Waitlist
- **Method & Path**: `POST /api/bookings/waitlist`
- **Auth**: `hasRole('TREKKER')`
- **Request Body**:
  ```json
  {
    "slotId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "guideId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
  }
  ```
- **Response Body**: `200 OK`
  ```json
  {
    "message": "Successfully joined waitlist"
  }
  ```
- **Errors**:
  - `409 Conflict`: Slot is not full yet (must book directly).
  - `404 Not Found`: Slot or Guide not found.

---

## 5. Guide Endpoints

### 5.1 Get My Profile
- **Method & Path**: `GET /api/guides/me`
- **Auth**: `hasRole('GUIDE')`
- **Response Body**: `200 OK`
  ```json
  {
    "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "name": "Tenzing Norgay",
    "email": "tenzing@example.com",
    "skills": ["High Altitude", "Wilderness First Aid"],
    "languages": ["English", "Hindi"],
    "location": "Manali",
    "latitude": 32.2396,
    "longitude": 77.1887,
    "experienceLevel": "EXPERT",
    "isVerified": true
  }
  ```

### 5.2 Update My Profile
- **Method & Path**: `PUT /api/guides/me`
- **Auth**: `hasRole('GUIDE')`
- **Request Body**:
  ```json
  {
    "skills": ["High Altitude", "First Aid", "Navigation"],
    "languages": ["English", "Hindi", "French"],
    "location": "Manali",
    "latitude": 32.2396,
    "longitude": 77.1887,
    "experienceLevel": "EXPERT"
  }
  ```
  - Validation: `experienceLevel` is `@NotNull`.
- **Response Body**: `200 OK` Updated `GuideResponse`.

### 5.3 Get Guide Assigned Bookings
- **Method & Path**: `GET /api/guides/me/bookings`
- **Auth**: `hasRole('GUIDE')`
- **Response Body**: `200 OK` List of `BookingResponse`.

### 5.4 Accept Booking
- **Method & Path**: `POST /api/bookings/{id}/accept`
- **Auth**: `hasRole('GUIDE')`
- **Response Body**: `200 OK` `BookingResponse` with `status: "AWAITING_PAYMENT"`, `paymentOrderId: "order_..."`, and `depositAmount`.
- **Errors**: `403 Forbidden` if not assigned to this guide; `409 Conflict` if booking is not in `PENDING_GUIDE_RESPONSE`.

### 5.5 Decline Booking
- **Method & Path**: `POST /api/bookings/{id}/decline`
- **Auth**: `hasRole('GUIDE')`
- **Response Body**: `200 OK` `BookingResponse` with updated status (rematched to another guide or `REMATCH_FAILED`).
- **Errors**: `403 Forbidden` if not assigned to this guide; `409 Conflict` if not pending response.

### 5.6 Mark No-Show
- **Method & Path**: `POST /api/bookings/{id}/no-show`
- **Auth**: `hasRole('GUIDE')`
- **Response Body**: `200 OK` `BookingResponse` with `status: "NO_SHOW"`, `refundPercentage: 0.0`, `refundAmount: 0.0`.
- **Errors**: `400 Bad Request` if activity date has not passed or status is not `CONFIRMED`.

---

## 6. Error Response Schema

All error responses return JSON in one of these two structures:

### Structure A (Standard Error)
```json
{
  "error": "Conflict occurred"
}
```

### Structure B (Validation Error with Details)
```json
{
  "error": "Validation failed",
  "details": {
    "email": "must be a well-formed email address",
    "password": "must not be blank"
  }
}
```

### Structure C (Alternative Message)
```json
{
  "message": "Resource not found"
}
```

The app's `ErrorMapper` will inspect both `error` and `message` properties and extract validation `details` if present.
