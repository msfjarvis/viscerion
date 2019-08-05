#!/usr/bin/env bash

if [[ $(git diff --name-only HEAD origin/$(git rev-parse --abbrev-ref HEAD) | grep -e '\.kt[s]\?$' -e '\.xml\?$') != "" ]]; then
    gradle spotlessApply
    gradle assembleDebug
fi
if [ $? -ne 0 ]; then exit 1; fi
