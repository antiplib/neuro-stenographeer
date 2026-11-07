#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
CHECK_ONLY=1 sh scripts/fetch-models.sh
sh scripts/fetch-deps.sh
cd src
sh gradlew --no-daemon :app:assembleDebug
cd ..
mkdir -p artifacts
cp src/app/build/outputs/apk/debug/app-debug.apk artifacts/neuro-stenographer-v8-aqua.apk
printf '%s\n' 'Built artifacts/neuro-stenographer-v8-aqua.apk with embedded models'
