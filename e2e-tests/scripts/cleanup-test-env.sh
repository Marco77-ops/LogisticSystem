#!/bin/bash

echo "🧹 Räume Test-Umgebung auf..."

# Verzeichnis ermitteln
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

# Test-Container stoppen und entfernen
docker-compose -f docker-compose.test.yml down -v

# Nicht verwendete Images aufräumen
docker image prune -f

# Nicht verwendete Volumes aufräumen
docker volume prune -f

echo "✅ Test-Umgebung aufgeräumt!"