#!/bin/bash
set -a && source .env && set +a

ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"name": "Test Trekker", "email": "trekker_new999@example.com", "password": "password123", "role": "TREKKER"}' > /dev/null
TREKKER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "trekker_new999@example.com", "password": "password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

ACT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"title":"Test2","description":"Test","location":"Nepal","difficultyLevel":"EASY","basePrice":100.00}')
ACT_ID=$(echo $ACT_RES | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

SLOT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" -d '{"date":"2026-11-01T08:00:00","capacity":10,"season":"PEAK"}')
SLOT_ID=$(echo $SLOT_RES | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

echo "Slot ID: $SLOT_ID"
echo "--- First Call ---"
curl -s -X POST http://localhost:8080/api/bookings/request -H "Content-Type: application/json" -H "Authorization: Bearer $TREKKER_TOKEN" -d "{\"slotId\":\"$SLOT_ID\"}"
echo -e "\n--- Second Call ---"
curl -s -X POST http://localhost:8080/api/bookings/request -H "Content-Type: application/json" -H "Authorization: Bearer $TREKKER_TOKEN" -d "{\"slotId\":\"$SLOT_ID\"}"
echo ""
