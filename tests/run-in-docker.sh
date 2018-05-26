#!/usr/bin/env bash

GRADLE_VERSION='4.1'

if [ ! -d "${HOME}/gradle-${GRADLE_VERSION}" ] ; then
    wget http://downloads.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O "${HOME}/gradle.zip"
    unzip "${HOME}/gradle.zip" -d "${HOME}"
fi

"${HOME}/gradle-${GRADLE_VERSION}/bin/gradle" -p "$@" --no-daemon check
