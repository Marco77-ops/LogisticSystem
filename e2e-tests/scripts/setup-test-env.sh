#!/bin/bash

set -e

echo "🛠️ Setup Test-Umgebung für LuckyPets Logistics..."

# Verzeichnis ermitteln
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

# Services bauen
echo "📦 Baue Services..."
mvn clean package -DskipTests -q

# Test-Umgebung starten
echo "🐳 Starte Test-Umgebung..."
docker-compose -f docker-compose.test.yml up -d

# Warten auf Services
echo "⏳ Warte auf Services..."
services=("8081" "8082" "8083" "8084" "8085")

for port in "${services[@]}"; do
    echo "Prüfe Service auf Port $port..."
    until curl -f -s http://localhost:$port/actuator/health > /dev/null 2>&1; do
        echo "Warte auf Service auf Port $port..."
        sleep 5
    done
    echo "✅ Service auf Port $port ist bereit"
done

echo "🎉 Test-Umgebung ist bereit!"
echo ""
echo "Service URLs:"
echo "- Shipment Service: http://localhost:8081"
echo "- Scan Service: http://localhost:8082"
echo "- Delivery Service: http://localhost:8083"
echo "- Analytics Service: http://localhost:8084"
echo "- Notification Service: http://localhost:8085"
echo ""
echo "Zum Stoppen: docker-compose -f docker-compose.test.yml down"