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

echo -e "\n--- Test with Invalid API Key ---"
export OPENWEATHER_URL=http://localhost:8081
export OPENWEATHER_API_KEY=invalid_key

# Restart spring boot context so it picks up the env var? Wait, application is already running!
# The Spring Boot server is already running, so `export OPENWEATHER_API_KEY` in this script will NOT affect the already running server process!
