# Trading Grid Demo - PonySDK + React AG Grid

This demo showcases PonySDK's real-time server push capabilities combined with modern React UI (AG Grid).

## Architecture

- **Java Server**: Controls everything via `PTradingGrid` component
- **React Client**: AG Grid renders the trading data
- **ComponentTerminal**: Bridges PonySDK protocol with React components
- **Real-time Updates**: Server pushes price updates every 100ms

## Build Steps

### 1. Build ComponentTerminal

```bash
cd ponysdk-component-terminal
npm install
npm run build
```

This creates:
- `dist/component-terminal.browser.js` - Browser bundle for ComponentTerminal

### 2. Copy ComponentTerminal to Sample

```bash
# From project root
cp ponysdk-component-terminal/dist/component-terminal.browser.js sample/src/main/resources/script/component-terminal.js
```

### 3. Build Trading Grid Component

```bash
cd sample/src/main/resources/components
npm install
npm run build
```

This creates:
- `../script/trading-grid.js` - Component implementation
- `../script/trading-grid-register.js` - Registration script

### 4. Build Java Code

```bash
# From project root
./gradlew :sample:build
```

## Run the Demo

### Option 1: Using Gradle

```bash
./gradlew :sample:run -Dconfig=etc/trading_grid_application.xml
```

### Option 2: Using Java Directly

```bash
cd sample
java -cp "build/libs/*:build/classes/java/main" \
  com.ponysdk.impl.spring.MainSpring \
  etc/trading_grid_application.xml
```

### Option 3: Using IDE

Run `MainSpring` with program argument: `etc/trading_grid_application.xml`

## Access the Demo

Open browser to: `http://localhost:8080`

You should see:
- A trading grid with 50 pony stocks
- Real-time price updates with cell flashing
- Click on rows to see console output

## How It Works

### Server Side (Java)

1. `TradingGridEntryPoint` creates `PTradingGrid` component
2. Schedules price updates every 100ms
3. Calls `tradingGrid.updateStock(stock)` to push updates
4. `PTradingGrid` extends `PReactComponent` which handles protocol

### Client Side (React)

1. `component-bridge.js` initializes ComponentTerminal when PonySDK loads
2. `trading-grid-register.js` registers the TradingGrid factory
3. ComponentTerminal receives create/update/destroy messages
4. React AG Grid renders the data with cell flashing animations

### Protocol Flow

```
Java: PTradingGrid.setProps(newProps)
  ↓
PComponent: Serializes props to JSON
  ↓
WebSocket: Binary protocol message
  ↓
ComponentTerminal: Parses message, routes to adapter
  ↓
ReactAdapter: Updates React component
  ↓
AG Grid: Renders with animations
```

## File Structure

```
sample/
├── src/main/java/com/ponysdk/sample/client/
│   ├── TradingGridEntryPoint.java          # Demo entry point
│   └── component/
│       ├── PTradingGrid.java               # Server-side component
│       ├── StockData.java                  # Data record
│       └── TradingGridProps.java           # Props record
├── src/main/resources/
│   ├── components/
│   │   ├── package.json                    # npm config
│   │   └── trading-grid/
│   │       ├── TradingGrid.tsx             # React component
│   │       ├── index.ts                    # Factory
│   │       ├── register.ts                 # Registration
│   │       └── styles.css                  # Styling
│   ├── script/
│   │   ├── component-bridge.js             # PonySDK ↔ ComponentTerminal bridge
│   │   ├── component-terminal.js           # (generated from ponysdk-component-terminal)
│   │   ├── trading-grid.js                 # (generated from components)
│   │   └── trading-grid-register.js        # (generated from components)
│   └── etc/
│       └── trading_grid_application.xml    # Spring config
```

## Troubleshooting

### ComponentTerminal not found

Make sure you built and copied the ComponentTerminal browser bundle:
```bash
cd ponysdk-component-terminal
npm run build
cp dist/component-terminal.browser.js ../sample/src/main/resources/script/component-terminal.js
```

### Trading grid not rendering

Check browser console for:
- "Registering React component: trading-grid"
- "ComponentTerminal initialized"
- Any React errors

### No real-time updates

Check Java console for:
- Scheduler running
- No exceptions in update loop

### Build errors

Make sure all dependencies are installed:
```bash
cd ponysdk-component-terminal && npm install
cd ../sample/src/main/resources/components && npm install
```

## Next Steps

- Add more interactive features (sorting, filtering)
- Implement cell editing with server validation
- Add more chart types (candlestick, line charts)
- Create more component examples (forms, dashboards)
