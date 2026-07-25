#!/bin/bash

# Register Trekker
curl -s -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{"name": "Test Trekker", "email": "trekker_new1@example.com", "password": "password123", "role": "TREKKER"}'

# Login Trekker
TREKKER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "trekker_new1@example.com", "password": "password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Login Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Create Activity
ACTIVITY_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"title": "Public Endpoint Trek", "description": "Trek", "location": "Nepal", "difficultyLevel": "MODERATE", "basePrice": 500.00}')
ACT_ID=$(echo $ACTIVITY_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

# Create Slot
curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"date": "2026-12-01T08:00:00", "capacity": 15, "season": "PEAK"}' > /dev/null

echo "--- Fetching Activity with Trekker Token ---"
curl -s -X GET http://localhost:8080/api/activities/$ACT_ID \
-H "Authorization: Bearer $TREKKER_TOKEN" | jq
