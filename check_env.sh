#!/bin/bash

echo "Checking execution environment..."

# Check Java
if command -v java >/dev/null 2>&1; then
    echo "✅ Java is installed: $(java -version 2>&1 | head -n 1)"
else
    echo "❌ Java is NOT installed."
fi

# Check Node.js
if command -v node >/dev/null 2>&1; then
    echo "✅ Node.js is installed: $(node -v)"
else
    echo "❌ Node.js is NOT installed. (Required for Frontend)"
fi

# Check Gradle
if command -v gradle >/dev/null 2>&1; then
    echo "✅ Gradle is installed: $(gradle -v | grep Gradle | head -n 1)"
else
    echo "❌ Gradle is NOT installed. (Required for Backend)"
fi

# Check Docker
if command -v docker >/dev/null 2>&1; then
    echo "✅ Docker is installed: $(docker -v)"
else
    echo "❌ Docker is NOT installed. (Required for Containerization)"
fi

echo ""
echo "Summary:"
echo "To run this project, please ensure Node.js and Gradle (or Docker) are installed."
