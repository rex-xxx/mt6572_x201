#!/bin/bash -e

ROOTFS=android_rootfs.bin
ANDROID=android_sd.tar.gz
PROJECT=mt6572_fpga

if [ "$1" != "" ]; then
	PROJECT=$1
fi
PROJECT_FOLDER=out/target/product/$PROJECT/


echo "populate root folder"

if [ "$PROJECT" = "mt6572_fpga" ]; then
    # cp init scripts into root folder
    cp mediatek/config/$PROJECT/pwr_mng_cfg.sh ./out/target/product/$PROJECT/root
fi

echo "Prepare SD image for $PROJECT ..."
rm -rf out/target/product/$PROJECT/system/bin/ccci_mdinit
rm -rf out/target/product/$PROJECT/root/system
rm -rf out/target/product/$PROJECT/root/data

if [ "$PROJECT" = "mt6589_fpga" -o "$PROJECT" = "mt6582_fpga" ]; then
#if [ "$PROJECT" = "mt6589_fpga" ]; then
    echo "To shorten the booting time. Move unnecessary *.apk for $PROJECT ..."
    unnecessary_apks=("CalendarProvider.apk" "ContactsProvider.apk" "MediaProvider.apk" "SogouInput.apk" "Provision.apk" "OOBE.apk"
    "Contacts.apk" "SystemUI.apk" "LatinIME.apk" "UserDictionaryProvider.apk" "dm.apk" "SmsReg.apk" "ActivityNetwork.apk" "DownloadProvider.apk"
    "AtciService.apk" "DeskClock.apk" "Email.apk" "Exchange.apk" "GoogleOta.apk" "Log2Server.apk" "Mms.apk" "MobileLog.apk" "Omacp.apk" "MtkWeatherProvider.apk" "MtkWeatherWidget.apk" "ModemLog.apk" "DmvProvider.apk" "MtkBt.apk" "QQBrowser.apk" "QQGame.apk" "QQIM.apk" "MTKThemvalManager.apk"
    )
    mkdir -p out/target/product/$PROJECT/system/app_unnecessary
    for apk in ${unnecessary_apks[@]}
    do
        if [ -e $PROJECT_FOLDER/system/app/$apk ]; then
            echo "mv $PROJECT_FOLDER/system/app/$apk $PROJECT_FOLDER/system/app_unnecessary/$apk"
            mv $PROJECT_FOLDER/system/app/$apk $PROJECT_FOLDER/system/app_unnecessary/
        fi
    done
fi

if [ "$PROJECT" = "mt6572_fpga" ]; then
    echo "To shorten the booting time. Move unnecessary *.apk for $PROJECT ..."
    unnecessary_apks=("CalendarProvider.apk" "ContactsProvider.apk" "MediaProvider.apk" "Provision.apk" "OOBE.apk"
    "Contacts.apk" "UserDictionaryProvider.apk" "dm.apk" "SmsReg.apk" "ActivityNetwork.apk" "DownloadProvider.apk"
    "AtciService.apk" "DeskClock.apk" "Email.apk" "Exchange.apk" "GoogleOta.apk" "Log2Server.apk" "Mms.apk" "MobileLog.apk" "Omacp.apk" "MtkWeatherProvider.apk" "MtkWeatherWidget.apk" "ModemLog.apk" "DmvProvider.apk" "MtkBt.apk" "QQBrowser.apk" "QQGame.apk" "QQIM.apk" "MTKThemvalManager.apk"
    )
    mkdir -p out/target/product/$PROJECT/system/app_unnecessary
    for apk in ${unnecessary_apks[@]}
    do
        if [ -e $PROJECT_FOLDER/system/app/$apk ]; then
            echo "mv $PROJECT_FOLDER/system/app/$apk $PROJECT_FOLDER/system/app_unnecessary/$apk"
            mv $PROJECT_FOLDER/system/app/$apk $PROJECT_FOLDER/system/app_unnecessary/
        fi
    done
fi

# switch to root folder
cd ./out/target/product/$PROJECT/root
if [ "$PROJECT" = "mt6582_fpga" ]; then
# To prevent "skipping insecure file" issue
chmod 750 *.rc
chmod 750 *.prop
chmod 750 ../system/*.prop
fi
find . -print | cpio -H newc -o > ../$ROOTFS

cd ../
cp -r data system/
mkdir -p system/data/local
touch system/data/local/enable_menu_key
cd system

if [ "$PROJECT" = "mt6572_fpga" ]; then
    # instead of tar directly, we fix the permission and uid/gid during tar:
    # move up to $PROJECT so that fs_get_stats can query correct filename
    # note that mktarball_mtk is modified from alps/build/tool/mktarball.sh
    cd ../
    pwd
    ../../../../mediatek/build/tools/mktarball_mtk.sh ../../../host/linux-x86/bin/fs_get_stats system . android_sd_test.tar $ANDROID
else
tar zcvf ../$ANDROID .
fi

cp ../$ANDROID ~/tmp/

