import json

with open('peakconnect_postman_collection.json', 'r') as f:
    data = json.load(f)

# Find Bookings folder or create it
bookings_folder = next((item for item in data['item'] if item['name'] == 'Bookings'), None)
if not bookings_folder:
    bookings_folder = {"name": "Bookings", "item": []}
    data['item'].append(bookings_folder)

# Add Webhook folder if not exists
webhook_folder = {"name": "Payments", "item": []}

webhook_success = {
    "name": "Simulate Webhook Success",
    "request": {
        "method": "POST",
        "header": [
            { "key": "Content-Type", "value": "application/json" },
            { "key": "X-Razorpay-Signature", "value": "dummy_signature_for_testing_only" }
        ],
        "body": {
            "mode": "raw",
            "raw": "{\n  \"event\": \"order.paid\",\n  \"payload\": {\n    \"payment\": {\n      \"entity\": {\n        \"order_id\": \"order_123456789\"\n      }\n    }\n  }\n}"
        },
        "url": {
            "raw": "{{baseUrl}}/api/payments/webhook",
            "host": ["{{baseUrl}}"],
            "path": ["api", "payments", "webhook"]
        }
    }
}

webhook_failure = {
    "name": "Simulate Webhook Failure",
    "request": {
        "method": "POST",
        "header": [
            { "key": "Content-Type", "value": "application/json" },
            { "key": "X-Razorpay-Signature", "value": "dummy_signature_for_testing_only" }
        ],
        "body": {
            "mode": "raw",
            "raw": "{\n  \"event\": \"payment.failed\",\n  \"payload\": {\n    \"payment\": {\n      \"entity\": {\n        \"order_id\": \"order_123456789\"\n      }\n    }\n  }\n}"
        },
        "url": {
            "raw": "{{baseUrl}}/api/payments/webhook",
            "host": ["{{baseUrl}}"],
            "path": ["api", "payments", "webhook"]
        }
    }
}

webhook_folder['item'].extend([webhook_success, webhook_failure])
data['item'].append(webhook_folder)

with open('peakconnect_postman_collection.json', 'w') as f:
    json.dump(data, f, indent=2)
print("Updated Postman collection.")
