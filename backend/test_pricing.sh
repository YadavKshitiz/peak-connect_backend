#!/bin/bash

# Login Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Create Activity (Base Price: 1000.00)
ACTIVITY_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"title": "Pricing Trek", "description": "Trek", "location": "Nepal", "difficultyLevel": "MODERATE", "basePrice": 1000.00}')
ACT_ID=$(echo $ACTIVITY_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

# Create Slot (Capacity: 10, Season: SHOULDER)
SLOT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"date": "2026-11-01T08:00:00", "capacity": 10, "season": "SHOULDER"}')
SLOT_ID=$(echo $SLOT_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo -e "\n--- Before Modification ---"
# Base Price 1000.00
# Season: SHOULDER (x1.00)
# Occupancy: 0/10 < 50% (x1.00)
# Expected Price: 1000.00
curl -s -X GET http://localhost:8080/api/admin/activities/$ACT_ID/slots \
-H "Authorization: Bearer $ADMIN_TOKEN" | jq

echo -e "\n\n--- Modifying DB ---"
PGPASSWORD=localdevpassword psql -h localhost -U peakconnect_user -d peakconnect -c "UPDATE slots SET current_occupancy = 9, season = 'PEAK' WHERE id = '$SLOT_ID';"

echo -e "\n--- After Modification ---"
# Base Price 1000.00
# Season: PEAK (x1.20) -> 1200.00
# Occupancy: 9/10 = 90% >= 80% (x1.25) -> 1500.00
# Expected Price: 1500.00
curl -s -X GET http://localhost:8080/api/admin/activities/$ACT_ID/slots \
-H "Authorization: Bearer $ADMIN_TOKEN" | jq
