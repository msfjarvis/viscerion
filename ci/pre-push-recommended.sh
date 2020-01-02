#!/usr/bin/env bash

if [[ $(git diff --name-only HEAD origin/$(git rev-parse --abbrev-ref HEAD) | grep -E 'buildSrc|native|app|crypto|config|util|gradle') != "" ]]; then
    gradle spotlessCheck || exit 1
    gradle assembleDebug || exit 1
fi
