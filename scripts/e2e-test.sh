#!/bin/bash

set -e

echo "🚀 LuckyPets Logistics End-to-End Test Runner"

# Verzeichnis ermitteln
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# E2E Test Script ausführen
"$PROJECT_ROOT/e2e-tests/scripts/run-e2e-tests.sh"