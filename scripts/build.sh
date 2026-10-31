#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "Starting the build..."
../gradlew clean build

echo "Build done!"