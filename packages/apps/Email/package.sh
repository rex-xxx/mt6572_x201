#!/bin/bash
# Program:
#   Package Email apks by using "email" or "test" and "all"
# History:
# 2011/11/17    1st version

export PATH

emailApk="Email-release.apk"
testApk="EmailTest-release.apk"
hasEmail=true
hasTest=false

#which ant

function packageApk() {
    if $hasEmail; then
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
    tar --remove-files -zcvf Email.tar.gz *.apk
}

function Usage() {
    echo "Usage: $0 {email|test|all}  - Default to package Email-release.apk
         $0 email   - Package Email-release.apk
         $0 test    - Package EmailTest-release.apk
         $0 all     - Package Email*-release.apk"
}

echo "Package.sh will package apks into Email.tar.gz"

if [ $# -le 1 ]; then
    # Package apks user want to
    case $1 in
        "")
            echo default to package email
            packageApk "email"
            ;;
        "email")
            echo "package email"
            packageApk "email"
            ;;
        "test")
            echo "package test"
            hasEmail=false
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
            adb shell uninstall com.android.email
            adb shell uninstall com.android.email.tests
            ;;
        *)
            Usage
            ;;
    esac
else
    Usage
fi


