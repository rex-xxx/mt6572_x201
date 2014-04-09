#!/bin/bash
# Program:
#   automatically run testcases
# History:
# 2011/12/19    1st version

PATH=/proj/mtk54030/Documents/android-4-sdk-linux/platform-tools:$PATH
export $PATH

emailApk="Email.apk"
testApk="EmailTests.apk"
exchangeApk="Exchange.apk"
testExchangeApk="ExchangeTests.apk"
hasExchangeTest=false
needInstall=false
classname="class $1"
emaildir=`pwd`
outputDir=$emaildir/../../../out/target/product/lenovo75/system/app
outputTestDir=$emaildir/../../../out/target/product/lenovo75/data/app

alpsDir=$emaildir/../../../
cd $alpsDir/build
#. $emaildir/../../../build/envsetup.sh
. envsetup.sh
export TARGET_PRODUCT=lenovo75
cd $emaildir

#which ant

function exit_runner {
    if [ $? -ne 0 ]; then
        exit $?
    fi
    sleep 0.5
}

function testcase_runner {
    #adb shell am instrument -w -e class com.android.emailcommon.internet.MimeUtilityTest com.android.email.tests/android.test.InstrumentationTestRunner
    echo testcase: $1  $hasExchangeTest
    if $hasExchangeTest; then
        adb shell am instrument -w com.android.exchange.tests/android.test.InstrumentationTestRunner
        exit_runner
    fi
    cd $emaildir
    if [ "$1" == "email" ] || $hasExchangeTest; then
        adb shell am instrument -w com.android.email.tests/android.test.InstrumentationTestRunner
    else
        echo $classname
        adb shell am instrument -w -e ${classname} com.android.email.tests/android.test.InstrumentationTestRunner
    fi
    exit_runner
}

function install_pkg {
    echo install: $needInstall
    # install email apk
    if (test -e $outputDir/$emailApk) && [ ! $needInstall ]; then
        adb install -r $outputDir/$emailApk
    else
        echo "Email apk need compile"
        mm; adb install -r $outputDir/$emailApk
    fi
    exit_runner
    # install emailtest apk
    if (test -e $outputTestDir/$testApk); then
        adb install -r $outputTestDir/$testApk
    else
        echo "Email tests apk installed"
        mm; adb install -r $outputTestDir/$testApk
    fi

    if $hasExchangeTest; then
        cd $emaildir/../Exchange
        exit_runner "Enter exchange directory..."
        # install exchange apk
        if (test -e $outputDir/$exchangeApk) && [ ! $needInstall ]; then
            adb install -r $outputDir/$exchangeApk
        else
            echo "Email apk need compile"
            cd $emaildir/../Exchange
            mm; adb install -r $outputDir/$exchangeApk
        fi
        exit_runner

        # install emailtest apk
        if (test -e $outputTestDir/$testExchangeApk); then
            adb install -r $outputTestDir/$testExchangeApk
        else
            cd $emaildir/../Exchange
            echo "Exchange tests apk installed"
            mm; adb install -r $outputTestDir/$testExchangeApk
        fi
        exit_runner
    fi
}

function clear_pkg {
    adb remount
    # uninstall default email and its testcase
    adb uninstall com.android.email
    adb uninstall com.android.email.tests

    if $hasExchangeTest; then
    # uninstall default exchange and its testcase
    adb uninstall com.android.exchange
    adb uninstall com.android.exchange.tests
    fi

    # remove Email and testcase database files
    adb shell rm data/app/com.android.email*
    adb shell rm -r data/data/com.android.email*
    adb shell rm -r data/data/com.android.email.tests*

    if $hasExchangeTest; then
    adb shell rm data/app/com.android.exchange*
    adb shell rm -r data/data/com.android.exchange*
    adb shell rm -r data/data/com.android.exchange.tests*
    fi

    # remove Email and testcase apks
    adb shell rm system/app/Email.apk

    if $hasExchangeTest; then
    adb shell rm system/app/Exchange.apk
    fi
}

function Usage() {
    echo "Usage: $0 {commonds} [option]  - Default to run Email unit test
        -commonds:
           $0 email            - Run Email unit test
           $0 clean            - Clean running enviroment of device
           $0 class#method     - Test class. For example (com.android.emailcommon.internet.MimeUtilityTest)
           $0 all              - Run Email and Exchange unit tests
           $0 help             - show this help
        -option:
           $0 {commonds} install - Compile and install apks then run Email and Exchange unit tests"

}

echo "unittest_runner.sh will run testcases automatically........"

if [ $# -le 2 ]; then
    echo $#, $1, $2
    if [ $# == 2 ]; then
        if [ "$2" == "install" ];then
            needInstall=true
        else
            Usage
            exit 1
        fi
    fi
    case $1 in
        "")
            echo "default to run Email unit test"
            clear_pkg
            install_pkg
            sleep 2
            testcase_runner "email"
            ;;
        "email")
            echo "Run Email unit test"
            clear_pkg
            install_pkg
            sleep 2
            testcase_runner "email"
            ;;
        "help")
            Usage
            ;;
        "all")
            echo "Run Email and Exchange unit tests"
            hasExchangeTest=true
            clear_pkg
            install_pkg
            sleep 2
            testcase_runner "all"
            ;;
        "clean")
            echo "Clean Email and Exchange packages"
            clear_pkg
            ;;
        "install")
            Usage
            ;;
        *)
            echo "Test package $1"
            adb shell ls /data/data/ | grep com.android.email
            if [ $? -ne 0 ]; then
                install_pkg
            fi
            testcase_runner $1
            ;;
    esac
else
    Usage
fi
