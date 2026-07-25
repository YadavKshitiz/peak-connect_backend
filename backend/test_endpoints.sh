#!/bin/bash
# Register Guide
echo "--- Registering Guide ---"
curl -s -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{"name": "Test Guide", "email": "guide_new999@example.com", "password": "password123", "role": "GUIDE"}'

echo -e "\n\n--- Login Guide ---"
GUIDE_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "guide_new999@example.com", "password": "password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Guide Token: ${GUIDE_TOKEN:0:15}..."

echo -e "\n\n--- Login Admin ---"
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email": "admin@example.com", "password": "admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Admin Token: ${ADMIN_TOKEN:0:15}..."

echo -e "\n\n--- Create Activity ---"
ACTIVITY_RES=$(curl -s -X POST http://localhost:8080/api/admin/activities \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"title": "Everest Base Camp", "description": "Classic trek", "location": "Nepal", "difficultyLevel": "CHALLENGING", "basePrice": 1200.00}')
echo $ACTIVITY_RES
ACT_ID=$(echo $ACTIVITY_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo -e "\n\n--- Create Slot ---"
curl -s -X POST http://localhost:8080/api/admin/activities/$ACT_ID/slots \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-d '{"date": "2026-10-01T08:00:00", "capacity": 10, "season": "PEAK"}'

echo -e "\n\n--- Guide Update Profile ---"
UPDATE_RES=$(curl -s -X PUT http://localhost:8080/api/guides/me \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $GUIDE_TOKEN" \
-d '{"skills": ["First Aid", "Mountaineering"], "languages": ["English", "Nepali"], "location": "Kathmandu", "experienceLevel": "EXPERT"}')
echo $UPDATE_RES
GUIDE_ID=$(echo $UPDATE_RES | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo -e "\n\n--- Admin Approve Guide ---"
curl -s -X POST http://localhost:8080/api/admin/guides/$GUIDE_ID/approve \
-H "Authorization: Bearer $ADMIN_TOKEN"
