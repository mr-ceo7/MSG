#!/bin/bash

# Test payload with single recipient
PAYLOAD='{"recipients":["1234567890"],"message":"Test message from host","timestamp":'$(date +%s)'000}'

# Calculate HMAC-SHA256 using the dev secret from instructions
HMAC=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "your-experimental-secret" -binary | base64)

# Full request body
REQUEST="{\"payload\":$PAYLOAD,\"hmac\":\"$HMAC\"}"

echo "Sending POST to http://127.0.0.1:8080/broadcast"
echo "Request body:"
echo "$REQUEST"

# Send POST request
curl -v -X POST \
  -H "Content-Type: application/json" \
  -d "$REQUEST" \
  http://127.0.0.1:8080/broadcast