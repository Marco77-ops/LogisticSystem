#!/bin/bash

set -e

echo "🧪 Schneller Workflow-Test..."

# Basis-URLs
SHIPMENT_URL="http://localhost:8081"
SCAN_URL="http://localhost:8082"
DELIVERY_URL="http://localhost:8083"
NOTIFICATION_URL="http://localhost:8085"

# 1. Health Checks
echo "🔍 Prüfe Service-Status..."
curl -f "$SHIPMENT_URL/actuator/health" > /dev/null
curl -f "$SCAN_URL/actuator/health" > /dev/null
curl -f "$DELIVERY_URL/actuator/health" > /dev/null
curl -f "$NOTIFICATION_URL/actuator/health" > /dev/null
echo "✅ Alle Services sind erreichbar"

# 2. Sendung erstellen
echo "📦 Erstelle Testsendung..."
SHIPMENT_RESPONSE=$(curl -s -X POST "$SHIPMENT_URL/api/v1/shipments" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "ScriptOrigin",
    "destination": "ScriptDestination",
    "customerId": "script-customer"
  }')

SHIPMENT_ID=$(echo "$SHIPMENT_RESPONSE" | jq -r '.id')
if [ "$SHIPMENT_ID" = "null" ]; then
    echo "❌ Sendungserstellung fehlgeschlagen"
    exit 1
fi
echo "✅ Sendung erstellt: $SHIPMENT_ID"

# 3. Sendung scannen
echo "📱 Scanne Sendung..."
curl -s -X POST "$SCAN_URL/api/v1/scans" \
  -H "Content-Type: application/json" \
  -d "{
    \"shipmentId\": \"$SHIPMENT_ID\",
    \"location\": \"ScriptDestination\"
  }" > /dev/null
echo "✅ Sendung gescannt"

# 4. Zustellung prüfen
echo "🚚 Prüfe Zustellungsstatus..."
for i in {1..10}; do
    DELIVERY_STATUS=$(curl -s "$DELIVERY_URL/deliveries/$SHIPMENT_ID" | jq -r '.status')
    if [ "$DELIVERY_STATUS" = "DELIVERED" ]; then
        echo "✅ Sendung zugestellt"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "❌ Zustellung nicht bestätigt nach 10 Versuchen"
        exit 1
    fi
    echo "⏳ Warte auf Zustellung... (Versuch $i/10)"
    sleep 3
done

# 5. Benachrichtigungen prüfen
echo "📧 Prüfe Benachrichtigungen..."
NOTIFICATION_COUNT=$(curl -s "$NOTIFICATION_URL/api/notifications" | jq 'length')
if [ "$NOTIFICATION_COUNT" -gt 0 ]; then
    echo "✅ $NOTIFICATION_COUNT Benachrichtigungen gefunden"
else
    echo "⚠️ Keine Benachrichtigungen gefunden"
fi

echo "🎉 Workflow-Test erfolgreich abgeschlossen!"