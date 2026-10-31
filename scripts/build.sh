#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Starting the build..."
../gradlew -p .. clean build

echo "Build done!"