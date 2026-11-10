#!/usr/bin/env bash
# PeakConnect: add 10 demo activities with slots via the admin API.
# Usage: bash add_activities.sh   (wake the server first; needs python3 and curl)
API="${API:-https://peak-connect-backend.onrender.com}"

read -p "Admin email: " ADMIN_EMAIL
read -s -p "Admin password: " ADMIN_PW; echo

TOKEN=$(curl -s --max-time 90 -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PW\"}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
[ -z "$TOKEN" ] && { echo "Login failed. Check the email/password and that the server is awake."; exit 1; }
echo "Logged in."

EXISTING=$(curl -s --max-time 90 "$API/api/activities" \
  | python3 -c "import sys,json;print('\n'.join(a['title'] for a in json.load(sys.stdin)))" 2>/dev/null)

LAST_ID=""

# add_activity TITLE LOCATION LAT LNG DIFFICULTY BASE_PRICE POLICY DESCRIPTION
add_activity() {
  LAST_ID=""
  if echo "$EXISTING" | grep -qxF "$1"; then echo "skipped (already exists): $1"; return; fi
  local body resp
  body=$(python3 -c 'import json,sys
t,l,la,lo,d,p,c,desc=sys.argv[1:]
print(json.dumps({"title":t,"location":l,"latitude":float(la),"longitude":float(lo),"difficultyLevel":d,"basePrice":float(p),"cancellationPolicy":c,"description":desc}))' "$@")
  resp=$(curl -s --max-time 90 -X POST "$API/api/admin/activities" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$body")
  LAST_ID=$(echo "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
  if [ -z "$LAST_ID" ]; then echo "FAILED: $1 -> $resp"; else echo "created: $1"; fi
}

# add_slot DATE CAPACITY SEASON   (applies to the activity just created)
add_slot() {
  [ -z "$LAST_ID" ] && return
  curl -s --max-time 90 -o /dev/null -w "   slot $1 (cap $2, $3) -> HTTP %{http_code}\n" \
    -X POST "$API/api/admin/activities/$LAST_ID/slots" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"date\":\"$1\",\"capacity\":$2,\"season\":\"$3\"}"
}

add_activity "Hampta Pass Trek" "Manali" 32.2396 77.1887 MODERATE 9500 MODERATE \
  "Cross from the green Kullu valley into the dry Chandra valley over a high mountain pass."
add_slot "2026-12-14T06:00:00" 12 LOW
add_slot "2027-06-20T06:00:00" 15 PEAK

add_activity "Beas Kund Trek" "Manali" 32.3592 77.0911 EASY 6500 FLEXIBLE \
  "A gentle trail along the Beas river to a glacial lake beneath the Pir Panjal peaks."
add_slot "2026-12-20T06:30:00" 15 LOW
add_slot "2027-05-10T06:30:00" 20 SHOULDER

add_activity "Ganges River Rafting" "Rishikesh" 30.0869 78.2676 MODERATE 1800 FLEXIBLE \
  "A half-day white water rafting run through the rapids of the Ganges with certified guides."
add_slot "2026-12-12T09:00:00" 20 SHOULDER
add_slot "2027-03-15T09:00:00" 25 PEAK

add_activity "Kedarkantha Winter Trek" "Sankri" 31.0647 78.1860 MODERATE 8500 STRICT \
  "A classic snow trek through pine forests to a summit with a 360 degree Himalayan view."
add_slot "2026-12-26T06:00:00" 10 PEAK
add_slot "2027-01-23T06:00:00" 12 PEAK

add_activity "Valley of Flowers Trek" "Govindghat" 30.7600 79.5667 CHALLENGING 12000 MODERATE \
  "A long, rewarding trek into a national park famous for alpine flowers in full bloom."
add_slot "2027-07-15T06:00:00" 12 PEAK
add_slot "2027-08-12T06:00:00" 12 PEAK

add_activity "Chadar Frozen River Trek" "Leh" 34.1526 77.5771 EXTREME 28000 STRICT \
  "Walk on the frozen Zanskar river in extreme cold. For experienced and well-prepared trekkers."
add_slot "2027-01-25T07:00:00" 8 PEAK
add_slot "2027-02-08T07:00:00" 8 PEAK

add_activity "Pangong Lake Camping" "Leh" 33.7595 78.6674 EASY 7500 FLEXIBLE \
  "Overnight lakeside camping at the high altitude blue waters of Pangong Tso."
add_slot "2027-05-18T10:00:00" 20 SHOULDER
add_slot "2027-06-25T10:00:00" 25 PEAK

add_activity "Kheerganga Trek" "Kasol" 32.0100 77.3150 MODERATE 5500 FLEXIBLE \
  "A forest and meadow trail in Parvati Valley that ends at natural hot springs."
add_slot "2026-12-18T07:00:00" 15 LOW
add_slot "2027-04-12T07:00:00" 18 SHOULDER

add_activity "Rupin Pass Trek" "Sangla" 31.4240 78.2650 CHALLENGING 14500 STRICT \
  "A varied high pass trek with waterfalls, glaciers and a dramatic snowy crossing."
add_slot "2027-05-22T06:00:00" 10 SHOULDER
add_slot "2027-06-12T06:00:00" 12 PEAK

add_activity "Nag Tibba Weekend Trek" "Dehradun" 30.6153 78.1500 EASY 3500 FLEXIBLE \
  "A short weekend trek close to Dehradun with views of the Garhwal Himalaya."
add_slot "2026-12-06T06:30:00" 15 LOW
add_slot "2027-02-14T06:30:00" 15 SHOULDER

echo
echo "Done. Slugs the app will look for (location / activity):"
curl -s --max-time 90 "$API/api/activities" | python3 -c "
import sys,json,re
s=lambda t:re.sub(r'-+','-',re.sub(r'[^a-z0-9]','-',t.lower())).strip('-')
for a in json.load(sys.stdin): print(' ',s(a['location']),'/',s(a['title']))"
