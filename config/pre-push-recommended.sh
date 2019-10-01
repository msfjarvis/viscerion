#!/usr/bin/env bash

if [[ $(git diff --name-only HEAD origin/$(git rev-parse --abbrev-ref HEAD) | grep -E 'buildSrc|native|app|crypto|gradle') != "" ]]; then
    gradle spotlessCheck
    gradle assembleDebug
fi
if [ $? -ne 0 ]; then exit 1; fi
