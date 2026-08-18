#!/bin/bash
set -a && source .env && set +a
mvn spring-boot:run > startup.log 2>&1 &
PID=$!
echo "Started with PID $PID"
sleep 15
kill $PID
grep "WeatherRefreshJob completed" startup.log
