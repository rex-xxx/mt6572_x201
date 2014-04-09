#!/bin/bash
# Program:
#   Package Email apks by using "email" or "test" and "all"
# History:
# 2011/11/17    1st version

export PATH

emailApk="Exchange-release.apk"
testApk="ExchangeTest-release.apk"
hasExchange=true
hasTest=false

#which ant

function packageApk() {
    if $hasExchange; then
        if (test -e bin/$emailApk); then
            cp bin/$emailApk .
        else
            echo "No $emailApk, please compile it by \"ant clean release\""
            exit 0
        fi
    fi
    if $hasTest; then
        if (test -e bin/$testApk); then
            cp bin/$testApk .
        else
            echo "No $testApk, please compile it by \"ant clean release\""
            exit 0
        fi
    fi
    tar --remove-files -zcvf Exchange.tar.gz *.apk
}

function Usage() {
    echo "Usage: $0 {ex|test|all}  - Default to package Exchange-release.apk
         $0 email   - Package Exchange-release.apk
         $0 test    - Package ExchangeTest-release.apk
         $0 all     - Package Exchange*-release.apk"
}

echo "Package.sh will package apks into Email.tar.gz"

if [ $# -le 1 ]; then
    # Package apks user want to
    case $1 in
        "")
            echo default to package exchange
            packageApk "exchange"
            ;;
        "ex")
            echo "package exchange"
            packageApk "ex"
            ;;
        "test")
            echo "package test"
            hasExchange=false
            hasTest=true
            packageApk "test"
            ;;
        "all")
            echo "package all"
            hasTest=true
            packageApk "all"
            ;;
        "clean")
            echo "package clean"
            adb shell uninstall com.android.exchange
            adb shell uninstall com.android.exchange.tests
            ;;
        *)
            Usage
            ;;
    esac
else
    Usage
fi


