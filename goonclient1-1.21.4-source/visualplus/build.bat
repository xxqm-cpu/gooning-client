@echo off
where gradle >nul 2>nul || (echo Gradle not found. Install it first: winget install Gradle.Gradle & pause & exit /b 1)
if not exist gradlew.bat gradle wrapper --gradle-version 8.12
call gradlew.bat build
echo.
echo Done. Your mod is build\libs\visualplus-1.0.0.jar
pause
