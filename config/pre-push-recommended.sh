#!/usr/bin/env bash
gradle spotlessCheck || ./gradlew spotlessCheck
gradle :app:assembleDebug || ./gradlew :app:assembleDebug
