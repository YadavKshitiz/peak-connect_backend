#!/bin/bash
set -e
source .env

echo "=== 1. Login Admin ==="
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | jq -r .token)

echo "=== 2. Create Activity (Base Price = 100) ==="
ACT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"title": "Pricing Test Trek", "location": "Manali", "difficultyLevel": "MODERATE", "basePrice": 100.0, "cancellationPolicy": "FLEXIBLE"}')
ACT_ID=$(echo $ACT_RES | jq -r .id)

echo "=== 3. Create Slot for TOMORROW (LeadTime < 3 days) ==="
TOMORROW_DATE=$(date -d "+1 day" +"%Y-%m-%dT%H:%M:%S")
SLOT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d "{\"date\": \"$TOMORROW_DATE\", \"capacity\": 10, \"season\": \"SHOULDER\"}")
SLOT_ID=$(echo $SLOT_RES | jq -r .id)

echo "=== 4. Check Initial Price (Should be 100 * 1.15 = 115) ==="
INITIAL_PRICE=$(echo $SLOT_RES | jq -r .computedPrice)
echo "Initial Price: $INITIAL_PRICE"

echo "=== 5. Inflate Views (>100 times) ==="
for i in {1..101}
do
   curl -s -X GET http://localhost:8080/api/activities/$ACT_ID -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
done
echo "Views inflated."

echo "=== 6. Evict Cache (Update Slot) ==="
SLOT_UPDATE=$(curl -s -X PUT http://localhost:8080/api/admin/activities/$ACT_ID/slots/$SLOT_ID -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d "{\"date\": \"$TOMORROW_DATE\", \"capacity\": 10, \"season\": \"SHOULDER\"}")
NEW_PRICE=$(echo $SLOT_UPDATE | jq -r .computedPrice)

echo "=== 7. Check New Price (Should be 115 * 1.10 = 126.50) ==="
echo "New Price: $NEW_PRICE"

echo "Done."
