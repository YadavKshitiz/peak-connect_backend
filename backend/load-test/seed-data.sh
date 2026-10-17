#!/usr/bin/env bash
# ==============================================================================
# Seed Test Data for PeakConnect Concurrency Load Testing
# ==============================================================================
# This script creates an Activity and a Slot with a deliberately constrained
# capacity (default: 5) and provisions a pool of authenticated trekker accounts
# with pre-issued JWT tokens. The resulting configuration is saved to
# load-test/test-config.json for consumption by booking-load-test.js.
# ==============================================================================

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
SLOT_CAPACITY="${SLOT_CAPACITY:-5}"
NUM_TREKKERS="${NUM_TREKKERS:-25}"
OUTPUT_FILE="${OUTPUT_FILE:-load-test/test-config.json}"

echo "=========================================================="
echo "PeakConnect Load Test: Seeding Test Activity & Slot Data"
echo "Target Base URL: $BASE_URL"
echo "Slot Capacity:   $SLOT_CAPACITY"
echo "Trekker Pool:    $NUM_TREKKERS virtual users"
echo "Output Config:   $OUTPUT_FILE"
echo "=========================================================="

# 1. Verify backend health
echo "[1/5] Checking backend health..."
HEALTH_STATUS=$(curl -sSf "$BASE_URL/actuator/health" 2>/dev/null | jq -r '.status // "DOWN"' || echo "DOWN")
if [ "$HEALTH_STATUS" != "UP" ]; then
  echo "Error: Backend at $BASE_URL is not healthy (status: $HEALTH_STATUS)." >&2
  exit 1
fi
echo "Backend is UP."

# 2. Login as Admin
echo "[2/5] Authenticating as Admin ($ADMIN_EMAIL)..."
ADMIN_AUTH_RES=$(curl -sSf -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")

ADMIN_TOKEN=$(echo "$ADMIN_AUTH_RES" | jq -r '.token')
if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
  echo "Error: Failed to obtain Admin JWT token." >&2
  exit 1
fi
echo "Admin authenticated successfully."

# 3. Create fresh test activity & slot
TIMESTAMP=$(date +%s)
ACTIVITY_TITLE="Concurrency Stress Trek $TIMESTAMP"
echo "[3/5] Creating test activity '$ACTIVITY_TITLE'..."

ACTIVITY_RES=$(curl -sSf -X POST "$BASE_URL/api/admin/activities" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"$ACTIVITY_TITLE\",
    \"description\": \"Automated load test activity to verify pessimistic write locking and capacity constraints under high concurrency.\",
    \"location\": \"Manali\",
    \"latitude\": 32.2432,
    \"longitude\": 77.1892,
    \"difficultyLevel\": \"MODERATE\",
    \"basePrice\": 100.00,
    \"cancellationPolicy\": \"MODERATE\"
  }")

ACTIVITY_ID=$(echo "$ACTIVITY_RES" | jq -r '.id')
echo "Created Activity ID: $ACTIVITY_ID"

echo "Creating slot with capacity=$SLOT_CAPACITY..."
SLOT_RES=$(curl -sSf -X POST "$BASE_URL/api/admin/activities/$ACTIVITY_ID/slots" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"date\": \"2026-11-20T09:00:00\",
    \"capacity\": $SLOT_CAPACITY,
    \"season\": \"PEAK\"
  }")

SLOT_ID=$(echo "$SLOT_RES" | jq -r '.id')
echo "Created Slot ID: $SLOT_ID (capacity=$SLOT_CAPACITY, currentOccupancy=0)"

# 4. Provision & Authenticate Trekker Pool
echo "[4/5] Provisioning $NUM_TREKKERS trekker accounts and obtaining JWT tokens..."
USERS_JSON="[]"

for i in $(seq 1 "$NUM_TREKKERS"); do
  USER_EMAIL="load_trekker_${i}@test.com"
  USER_PASS="password123"
  USER_NAME="Load Trekker $i"

  # Attempt login first
  LOGIN_BODY="{\"email\":\"$USER_EMAIL\",\"password\":\"$USER_PASS\"}"
  LOGIN_RES=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_BODY" || true)

  USER_TOKEN=$(echo "$LOGIN_RES" | jq -r '.token // empty')

  # If login failed, register the user
  if [ -z "$USER_TOKEN" ]; then
    REGISTER_BODY="{\"name\":\"$USER_NAME\",\"email\":\"$USER_EMAIL\",\"password\":\"$USER_PASS\",\"role\":\"TREKKER\"}"
    REG_RES=$(curl -s -X POST "$BASE_URL/api/auth/register" \
      -H "Content-Type: application/json" \
      -d "$REGISTER_BODY" || true)
    USER_TOKEN=$(echo "$REG_RES" | jq -r '.token // empty')
  fi

  if [ -z "$USER_TOKEN" ]; then
    echo "Warning: Could not provision or authenticate $USER_EMAIL" >&2
    continue
  fi

  USER_ENTRY=$(jq -n \
    --arg email "$USER_EMAIL" \
    --arg name "$USER_NAME" \
    --arg token "$USER_TOKEN" \
    '{email: $email, name: $name, token: $token}')

  USERS_JSON=$(echo "$USERS_JSON" | jq --argjson entry "$USER_ENTRY" '. + [$entry]')
done

USER_COUNT=$(echo "$USERS_JSON" | jq 'length')
echo "Provisioned $USER_COUNT authenticated trekker tokens."

# 5. Resolve Guide for the Slot
FIRST_TOKEN=$(echo "$USERS_JSON" | jq -r '.[0].token')
echo "[5/5] Resolving guide match for slot..."
MATCH_RES=$(curl -s -X POST "$BASE_URL/api/bookings/request" \
  -H "Authorization: Bearer $FIRST_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"slotId\":\"$SLOT_ID\"}")

GUIDE_ID=$(echo "$MATCH_RES" | jq -r '.[0].guideId // "d7509423-98ec-4530-b75f-607bb1f3c99b"')
GUIDE_NAME=$(echo "$MATCH_RES" | jq -r '.[0].name // "Guide1"')
echo "Selected Guide: $GUIDE_NAME ($GUIDE_ID)"

# 6. Save Configuration JSON
mkdir -p "$(dirname "$OUTPUT_FILE")"
jq -n \
  --arg baseUrl "$BASE_URL" \
  --arg activityId "$ACTIVITY_ID" \
  --arg slotId "$SLOT_ID" \
  --argjson slotCapacity "$SLOT_CAPACITY" \
  --arg guideId "$GUIDE_ID" \
  --arg guideName "$GUIDE_NAME" \
  --arg adminToken "$ADMIN_TOKEN" \
  --argjson users "$USERS_JSON" \
  '{
    baseUrl: $baseUrl,
    activityId: $activityId,
    slotId: $slotId,
    slotCapacity: $slotCapacity,
    guideId: $guideId,
    guideName: $guideName,
    adminToken: $adminToken,
    users: $users
  }' > "$OUTPUT_FILE"

echo "=========================================================="
echo "Seeding completed successfully!"
echo "Configuration written to: $OUTPUT_FILE"
echo "Activity ID: $ACTIVITY_ID"
echo "Slot ID:     $SLOT_ID"
echo "Capacity:    $SLOT_CAPACITY"
echo "Users:       $USER_COUNT"
echo "Ready to run: k6 run load-test/booking-load-test.js"
echo "=========================================================="
