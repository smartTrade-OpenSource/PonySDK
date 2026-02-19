#!/bin/bash
# Build script for Trading Grid Demo

set -e  # Exit on error

echo "=== Building Trading Grid Demo ==="

# Step 1: Build ComponentTerminal
echo ""
echo "Step 1: Building ComponentTerminal..."
cd ../ponysdk-component-terminal
npm install
npm run build

# Step 2: Copy ComponentTerminal to sample
echo ""
echo "Step 2: Copying ComponentTerminal browser bundle..."
cp dist/component-terminal.browser.js ../sample/src/main/resources/script/component-terminal.js

# Step 3: Build Trading Grid Component
echo ""
echo "Step 3: Building Trading Grid component..."
cd ../sample/src/main/resources/components
npm install
npm run build

# Step 4: Build Java code
echo ""
echo "Step 4: Building Java code..."
cd ../../../..
./gradlew :sample:build

echo ""
echo "=== Build Complete ==="
echo ""
echo "To run the demo:"
echo "  ./gradlew :sample:run -Dconfig=etc/trading_grid_application.xml"
echo ""
echo "Then open: http://localhost:8080"
