# Compile and run the component viewer

Write-Host "Compiling..." -ForegroundColor Cyan
./gradlew :ponysdk:compileJava -q

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running viewer..." -ForegroundColor Green
    
    # Get all JAR dependencies
    $classpath = (./gradlew :ponysdk:dependencies --configuration runtimeClasspath -q | Select-String "\.jar" | ForEach-Object { $_.ToString().Trim() }) -join ";"
    
    # Add compiled classes
    $fullClasspath = "ponysdk/build/classes/java/main;$classpath"
    
    # Run the viewer
    java -cp $fullClasspath com.ponysdk.core.ui.codegen.GeneratorEntryPoint ponysdk/src/main/data/custom-elements.json
} else {
    Write-Host "Compilation failed!" -ForegroundColor Red
}
