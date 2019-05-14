#!/usr/bin/env bash

trap 'exit 1' SIGINT SIGTERM

[ -z "$(command -v hub)" ] && { echo "hub not installed; aborting!"; exit 1; }
TAG="${1}"
hub tag -a "${TAG:?}"
./gradlew clean bundlePieRelease assemblePieRelease assembleQuinoaRelease
hub release create "${TAG}" -a app/build/outputs/apk/release/viscerion_"${TAG}"-pie-release.apk -a app/build/outputs/apk/release/viscerion_"${TAG}"-quinoa-release.apk
