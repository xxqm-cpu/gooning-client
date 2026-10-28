#!/bin/sh
command -v gradle >/dev/null 2>&1 || [ -f ./gradlew ] || { echo "Gradle not found. Install it first (brew install gradle)"; exit 1; }
[ -f ./gradlew ] || gradle wrapper --gradle-version 8.12
./gradlew build
echo "Done. Your mod is build/libs/visualplus-1.0.0.jar"
