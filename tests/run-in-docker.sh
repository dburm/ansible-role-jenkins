#!/usr/bin/env bash

GRADLE_VERSION='4.1'

apt-get update;
apt-get -y install ca-certificates unzip openjdk-8-jdk wget

if [ ! -d "/opt/gradle-$GRADLE_VERSION" ] ; then
    wget http://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip /tmp/gradle.zip
    unzip /tmp/gradle.zip -d /opt/
fi

/opt/gradle-$GRADLE_VERSION/bin/gradle -p "$@" --no-daemon check
