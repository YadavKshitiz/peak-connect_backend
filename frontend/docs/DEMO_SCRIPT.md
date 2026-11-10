# PeakConnect — 3–4 Minute Demo Walkthrough Script

This script provides an engaging, step-by-step walkthrough to present the PeakConnect Android app to stakeholders, evaluators, or users.

> **Target Duration**: 3 to 4 minutes  
> **Key Value Proposition**: Offline-first Himalayan trekking platform connecting trekkers with verified local guides, featuring trail maps, local notifications, test payments, and Hindi localization.

---

## Pre-Demo Checklist & Setup

1. **Wake up the Backend Server**:
   - The backend runs on a Render free-tier instance which spins down after inactivity.
   - **Demo Tip**: Launch the PeakConnect app once or run `curl https://peakconnect-backend.onrender.com/health` 1–2 minutes before the demo so the cold start is out of the way.
2. **Configuration**:
   - Ensure `local.properties` contains a valid Razorpay test key (`RAZORPAY_KEY_ID=rzp_test_...`).
   - *Security Reminder*: **Never commit `local.properties` or real credentials to Git or any public repository!**
3. **Accounts Ready**:
   - **Trekker Account**: `trekker@example.com` (or create via Register)
   - **Guide Account**: `guide@example.com` (or use Postman / curl for guide acceptance)
4. **Razorpay Test Credentials Reference**:
   - **Test Card**:
     - Card Number: `4000 0000 0000 0002` (or `4111 1111 1111 1111`)
     - Expiry: Any future date (e.g. `12/28`)
     - CVV: `123`
     - OTP: `123456` (or any 6-digit number)
   - **Test UPI ID**: `success@razorpay`

---

## Step-by-Step Demo Script

### 1. Launch & Cold-Start Resilience (0:00 – 0:20)
* **Action**: Launch the app from the Android launcher.
* **Narration**:
  > *"Notice the smooth launch experience. PeakConnect checks server health during startup. If the server is waking from a cold start, an animated status indicator reassures the user while attempting automatic background retries without blocking the UI."*

---

### 2. Activity Discovery & Filtering (0:20 – 0:50)
* **Action**:
  - Show the Explore feed with high-altitude Himalayan treks.
  - Tap the **Trekking** category filter chip.
  - Type `"Hampta"` into the search bar.
  - Tap on the **Hampta Pass Trek** card.
* **Narration**:
  > *"Trekkers can browse trails across categories like Trekking, Rafting, and Camping. Cards feature difficulty badges, duration, pricing, and cached trail imagery. The search bar responds with instant filtering."*

---

### 3. Trail Details, OpenStreetMap & Packing Checklist (0:50 – 1:20)
* **Action**:
  - Scroll through the Day-by-Day itinerary.
  - Scroll to the embedded **OpenStreetMap (OSMDroid)** map showing the trailhead location.
  - Tap the **Open in Maps** button to showcase external navigation intent.
  - Tap **Packing Checklist** to open the bottom sheet with gear recommendations.
* **Narration**:
  > *"Every trek includes a comprehensive itinerary, gear checklist, and an embedded OpenStreetMap widget rendered without proprietary Google Play Services dependencies. Trekkers can launch navigation directly to the trailhead coordinates."*

---

### 4. Smart Guide Matching & Booking Request (1:20 – 1:55)
* **Action**:
  - Select an available date slot.
  - Tap **Select Guide**.
  - Show the Matched Guides list with algorithmic match scores (e.g. 95%), spoken languages, and badge indicators.
  - Select a certified guide and proceed to Booking Review.
  - Point out the 20% deposit breakdown (e.g., ₹2,400 deposit of ₹12,000 total).
  - Tap **Submit Booking Request**.
* **Narration**:
  > *"Our guide-matching algorithm scores certified local guides based on language proficiency, experience level, and geographic proximity to the trailhead. After choosing a guide, the trekker reviews transparent pricing with an upfront 20% deposit requirement and submits the request."*

---

### 5. Guide Request Acceptance (1:55 – 2:25)
* **Action**:
  - *Option A (Second Device / Guide Login)*: Open Guide Dashboard, view incoming booking card with response countdown timer, tap **Accept**.
  - *Option B (Postman / curl)*: Execute `PATCH /bookings/{bookingId}/accept` with Guide JWT token.
  - *On Trekker device*: Observe state update to `AWAITING_PAYMENT` and the local notification banner.
* **Narration**:
  > *"Guides receive incoming booking requests with a countdown deadline to accept. When the guide accepts, the trekker receives a local notification alerting them to complete deposit payment within 24 hours."*

---

### 6. Seamless Test Payment via Razorpay (2:25 – 2:55)
* **Action**:
  - Tap **Pay Deposit (₹2,400)**.
  - The Razorpay Checkout SDK dialog appears in test mode.
  - Enter Test Card: `4000 0000 0000 0002`, Expiry: `12/28`, CVV: `123`.
  - Enter OTP `123456` and submit.
  - Show the **Payment Success** confirmation screen with Razorpay Payment ID and updated `CONFIRMED` status badge.
* **Narration**:
  > *"We integrate Razorpay Checkout SDK in test mode. Trekkers can pay via simulated cards, net banking, or UPI (`success@razorpay`). On successful callback, the signature is verified, updating the booking to CONFIRMED."*

---

### 7. Booking Timeline, Calendar Export & Cancellation Policy (2:55 – 3:25)
* **Action**:
  - Open **My Bookings** -> Select the confirmed booking.
  - Point out the visual Stepper Timeline showing progress.
  - Tap **Add to Calendar** to demonstrate calendar event intent.
  - Tap **Cancel Booking** to trigger the Cancellation Preview dialog showing the calculated refund percentage based on days until departure.
  - Dismiss without cancelling (or cancel to demonstrate instant refund calculation).
* **Narration**:
  > *"Trekkers can track their booking lifecycle through an interactive timeline, export dates directly into their device calendar, or inspect our transparent cancellation policy with automated refund calculation."*

---

### 8. Offline Resilience, Hindi Localization & Dark Mode (3:25 – 4:00)
* **Action**:
  - **Offline Mode**: Toggle Airplane Mode ON.
    - Navigate to Explore and Bookings: cached data renders instantly from Room.
    - Point out that destructive or network actions are safely gated via `OnlineOnlyButton`.
    - Turn Airplane Mode OFF: app re-syncs state automatically.
  - **Hindi Localization**:
    - Go to Profile / Settings -> Language -> Select **हिन्दी**.
    - Watch the entire UI translate immediately (खोजें, मेरी बुकिंग, प्रोफ़ाइल).
  - **Dark Mode**:
    - Switch theme to Dark Mode in settings to show high-contrast, WCAG AA compliant visual styling.
* **Narration**:
  > *"PeakConnect is built offline-first. Remote Himalayan trails frequently lack connectivity, so all itineraries, checklists, and confirmed bookings are stored in a local Room database. Finally, full Hindi localization ensures accessibility for regional guides and trekkers across India."*

---

## Quick Reference Summary

| Flow | Key Feature Demonstrated |
|---|---|
| **Cold Start** | Resilient startup & health check retry logic |
| **Explore** | Multi-category filtering, instant search, difficulty badges |
| **Detail** | Itineraries, packing checklists, embedded OSMDroid map |
| **Matching** | Match score percentage, language badges, 20% deposit model |
| **Payment** | Razorpay test card `4000 0000 0000 0002` / UPI `success@razorpay` |
| **Status** | Visual stepper timeline, add-to-calendar, refund preview |
| **Offline** | Room local database caching, `OnlineOnlyButton` gating |
| **Localization** | 100% Hindi (हिन्दी) string coverage and instant theme switching |

