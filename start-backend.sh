#!/bin/bash

# Load environment variables from .env file and start backend
# Usage: ./start-backend.sh

set -a  # automatically export all variables
source .env
set +a

echo "🔧 Environment variables loaded from .env"
echo "📦 Starting backend..."
echo ""

cd backend
mvn spring-boot:run

# Made with Bob
