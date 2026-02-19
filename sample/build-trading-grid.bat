@echo off
REM Build script for Trading Grid Demo (Windows)

echo === Building Trading Grid Demo ===

REM Step 1: Build ComponentTerminal
echo.
echo Step 1: Building ComponentTerminal...
cd ..\ponysdk-component-terminal
call npm install
call npm run build

REM Step 2: Copy ComponentTerminal to sample
echo.
echo Step 2: Copying ComponentTerminal browser bundle...
copy dist\component-terminal.browser.js ..\sample\src\main\resources\script\component-terminal.js

REM Step 3: Build Trading Grid Component
echo.
echo Step 3: Building Trading Grid component...
cd ..\sample\src\main\resources\components
call npm install
call npm run build

REM Step 4: Build Java code
echo.
echo Step 4: Building Java code...
cd ..\..\..\..
call gradlew.bat :sample:build

echo.
echo === Build Complete ===
echo.
echo To run the demo:
echo   gradlew.bat :sample:run -Dconfig=etc/trading_grid_application.xml
echo.
echo Then open: http://localhost:8080
