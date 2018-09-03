#!/bin/bash
source "${TRAVIS_BUILD_DIR}"/ci/transfer.sh

if [[ "${TRAVIS_PULL_REQUEST}" != "false" ]]; then
    BUILT_APK="WireGuard-KT-${TRAVIS_PULL_REQUEST}-${TRAVIS_COMMIT}.apk"
    find "${TRAVIS_BUILD_DIR}"/app/build/outputs/apk -name "*.apk" -exec mv {} "${BUILT_APK}" \;
    COMMENT="Test this at [${BUILT_APK}]($(transfer "${BUILT_APK}")).\n\n\n\n _This is an automatically posted message from Travis-CI_"
    curl -H "Authorization: token ${GITHUB_TOKEN}" -X POST -d "{\"body\": \"${COMMENT}\"}" "https://api.github.com/repos/${TRAVIS_REPO_SLUG}/issues/${TRAVIS_PULL_REQUEST}/comments"
fi
