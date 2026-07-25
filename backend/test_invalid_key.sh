#!/bin/bash

# Login Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Get the first activity and slot
ACTIVITY_JSON=$(curl -s -X GET http://localhost:8080/api/activities -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[0]')
ACT_ID=$(echo $ACTIVITY_JSON | jq -r '.id')
SLOT_ID=$(echo $ACTIVITY_JSON | jq -r '.slots[0].id')

echo -e "\n--- Testing with Invalid API Key ---"
curl -s -X GET http://localhost:8080/api/activities/$ACT_ID \
-H "Authorization: Bearer $ADMIN_TOKEN" | jq '.slots[0]'
