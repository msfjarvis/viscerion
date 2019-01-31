#!/usr/bin/env bash

[ -z "$(command -v hub)" ] && { echo "hub not installed; aborting!"; exit 1; }
TAG="${1}"
hub tag -a "${TAG:?}"
./gradlew clean bundleRelease assembleRelease
hub release create "${TAG}" -a app/build/outputs/apk/release/viscerion_"${TAG}"-release.apk
