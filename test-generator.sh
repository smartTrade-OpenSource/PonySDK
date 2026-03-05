#!/bin/bash

# Test script for the Web Component Wrapper Generator

set -e

echo "Building project..."
./gradlew :ponysdk:compileJava

echo ""
echo "Running generator..."
./gradlew :ponysdk:run --args="ponysdk/src/main/data/custom-elements.json build/generated/java build/generated/typescript"

echo ""
echo "Generated files:"
find build/generated -type f | head -20

echo ""
echo "Done!"
