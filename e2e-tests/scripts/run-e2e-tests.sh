#!/bin/bash
set -e

echo "🚀 Starte E2E Tests für LuckyPets Logistics..."

# Projekt-Root ermitteln
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# 1. Services bauen
echo "📦 Baue Services..."
mvn clean package -DskipTests -q

# 2. Test-Umgebung starten
echo "🐳 Starte Test-Umgebung..."
docker-compose -f docker-compose.test.yml up -d

# 3. Warten auf Services
echo "⏳ Warte auf Services..."
sleep 30

# Health Checks
for port in 8081 8082 8083 8084 8085; do
    echo "Prüfe Service auf Port $port..."
    until curl -f -s http://localhost:$port/actuator/health > /dev/null 2>&1; do
        echo "Warte auf Service Port $port..."
        sleep 5
    done
    echo "✅ Service Port $port bereit"
done

# 4. E2E Tests ausführen
echo "🧪 Führe E2E Tests aus..."
cd e2e-tests

# Beginne mit einfachen Tests
mvn test -Dtest=SimpleE2ETest
if [ $? -eq 0 ]; then
    echo "✅ Einfache Tests erfolgreich"

    # Erweiterte Tests
    mvn test -Dtest=EndToEndTestSuite
    test_exit_code=$?
else
    echo "❌ Einfache Tests fehlgeschlagen"
    test_exit_code=1
fi

# 5. Cleanup
echo "🧹 Cleanup..."
cd "$PROJECT_ROOT"
docker-compose -f docker-compose.test.yml down

if [ $test_exit_code -eq 0 ]; then
    echo "🎉 E2E Tests erfolgreich!"
else
    echo "❌ E2E Tests fehlgeschlagen"
fi

exit $test_exit_code