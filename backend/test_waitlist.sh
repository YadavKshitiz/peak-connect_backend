#!/bin/bash
set -e
source .env

echo "=== 1. Login ==="
TREKKER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "trekker_test1@example.com", "password": "password123"}' | jq -r .token)
TREKKER2_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "trekker_test2@example.com", "password": "password123"}' | jq -r .token)
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | jq -r .token)

echo "=== 2. Create Activity ==="
ACT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"title": "Waitlist Test Trek", "location": "Manali", "difficultyLevel": "MODERATE", "basePrice": 100.0, "cancellationPolicy": "FLEXIBLE"}')
ACT_ID=$(echo $ACT_RES | jq -r .id)

echo "=== 3. Create Slot (Capacity = 1) ==="
FUTURE_DATE=$(date -d "+10 days" +"%Y-%m-%dT%H:%M:%S")
SLOT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d "{\"date\": \"$FUTURE_DATE\", \"capacity\": 1, \"season\": \"PEAK\"}")
SLOT_ID=$(echo $SLOT_RES | jq -r .id)
GUIDE_ID="18d34380-0f5d-4be3-a535-288196949461"

echo "=== 4. Trekker 1 Books (Fills Slot) ==="
B1_RES=$(curl -s -X POST http://localhost:8080/api/bookings/confirm -H "Content-Type: application/json" -H "Authorization: Bearer $TREKKER_TOKEN" -d "{\"slotId\": \"$SLOT_ID\", \"guideId\": \"$GUIDE_ID\"}")
B1_ID=$(echo $B1_RES | jq -r .id)
B1_ORDER=$(echo $B1_RES | jq -r .paymentOrderId)

echo "=== 5. Pay Trekker 1 Booking ==="
WEBHOOK_PAYLOAD="{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"order_id\":\"$B1_ORDER\"}}}}"
SIGNATURE=$(echo -n "$WEBHOOK_PAYLOAD" | openssl dgst -sha256 -hmac "$RAZORPAY_KEY_SECRET" | sed 's/^.* //')
curl -s -X POST http://localhost:8080/api/payments/webhook -H "Content-Type: application/json" -H "X-Razorpay-Signature: $SIGNATURE" -d "$WEBHOOK_PAYLOAD" > /dev/null

echo "=== 6. Trekker 2 Tries to Book (Should get Conflict) ==="
curl -s -X POST http://localhost:8080/api/bookings/confirm -H "Content-Type: application/json" -H "Authorization: Bearer $TREKKER2_TOKEN" -d "{\"slotId\": \"$SLOT_ID\", \"guideId\": \"$GUIDE_ID\"}" | jq .

echo "=== 7. Trekker 2 Joins Waitlist ==="
curl -s -X POST http://localhost:8080/api/bookings/waitlist -H "Content-Type: application/json" -H "Authorization: Bearer $TREKKER2_TOKEN" -d "{\"slotId\": \"$SLOT_ID\", \"guideId\": \"$GUIDE_ID\"}" | jq .

echo "=== 8. Verify DB Waitlist State ==="
docker exec -i $(docker ps -qf "name=postgres") psql -U peakconnect_user -d peakconnect -c "SELECT id, status, trekker_id, booking_id FROM waitlists;"

echo "=== 9. Trekker 1 Cancels Booking ==="
curl -s -X POST http://localhost:8080/api/bookings/$B1_ID/cancel -H "Authorization: Bearer $TREKKER_TOKEN" > /dev/null

echo "=== 10. Verify DB Waitlist Promoted State ==="
docker exec -i $(docker ps -qf "name=postgres") psql -U peakconnect_user -d peakconnect -c "SELECT id, status, trekker_id, booking_id FROM waitlists;"
docker exec -i $(docker ps -qf "name=postgres") psql -U peakconnect_user -d peakconnect -c "SELECT id, status, is_from_waitlist FROM bookings WHERE is_from_waitlist = true;"

echo "Done."
