#!/bin/bash

# Login Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Create Activity
ACTIVITY_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"title": "Risk Trek", "description": "Trek", "location": "Nepal", "difficultyLevel": "MODERATE", "basePrice": 1000.00}')
ACT_ID=$(echo $ACTIVITY_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

# Create Slot
SLOT_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"date": "2026-11-01T08:00:00", "capacity": 10, "season": "SHOULDER"}')
SLOT_ID=$(echo $SLOT_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo -e "\n--- Testing with valid key (Mock Server) ---"
# The backend is currently configured to hit http://localhost:8081 with default_key
curl -s -X GET http://localhost:8080/api/activities/$ACT_ID \
-H "Authorization: Bearer $ADMIN_TOKEN" | jq '.slots[0]'

