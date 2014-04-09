#!/bin/sh

TOPFILE=build/core/envsetup.mk
# We redirect cd to /dev/null in case it's aliased to
# a command that prints something as a side-effect
# (like pushd)
HERE=`/bin/pwd`
T=
while [ \( ! \( -f $TOPFILE \) \) -a \( $PWD != "/" \) ]; do
	cd .. > /dev/null
	T=`PWD= /bin/pwd`
done
if [ -f "$T/$TOPFILE" ]; then
	SRC_ROOT=$T
else
	echo "Error: source tree was not found."
	exit
fi

PRODUCT=`cat $SRC_ROOT/.product`
PRODUCT_OUT=$SRC_ROOT/out/target/product/$PRODUCT
#INTERMEDIATES=$SRC_ROOT/out/target/common/obj/APPS/Weather3DWidget_intermediates
TEST_RUNNER=com.zutubi.android.junitreport.JUnitReportTestRunner

if adb remount
then
  # Prevent insufficient storage space
  adb shell rm -r /data/core

  # Copy binary to device
  #adb uninstall com.android.contacts
  #adb install -r $PRODUCT_OUT/system/app/Weather3DWidget.apk

  # frameworks_base
  # SMS, CONTACT, STK	
  adb push $PRODUCT_OUT/system/framework/framework.jar /system/framework
  adb push $PRODUCT_OUT/system/framework/secondary-framework.jar /system/framework

  # mediatek_hardware_ril	
  # SMS, CONTACT, STK	
  adb push $PRODUCT_OUT/system/lib/mtk-ril.so /system/lib
  adb push $PRODUCT_OUT/system/lib/mtk-rilmd2.so /system/lib

  # mediatek_protect_hardware_ril	
  # SMS, CONTACT	
  adb push $PRODUCT_OUT/system/lib/mtk-ril.so /system/lib
  adb push $PRODUCT_OUT/system/lib/mtk-rilmd2.so /system/lib

  # packages_apps_Phone	
  # CONTACT	
  adb push $PRODUCT_OUT/system/app/Phone.apk /system/app
  adb push $PRODUCT_OUT/system/framework/framework.jar /system/framework
  adb push $PRODUCT_OUT/system/framework/secondary-framework.jar /system/framework
  
  # install test APK
  adb uninstall com.mediatek.contactstest
  adb install -r $SRC_ROOT/___test_report/ContactsTest.apk

  # before restart, clean log
  # if clean log after restart, logger will stop
  adb shell rm -rf /sdcard/mtklog
  adb shell rm -rf /sdcard2/mtklog  
  rm -rf $SRC_ROOT/___test_report/mtklog_contact

  # restart VM then system will use new Weather3DWidget.apk
  #adb shell "stop;sleep 5;start"
  #sleep 30
  
  # restart phone 
  adb reboot
  sleep 60
  
  PACKAGE=com.android.contacts
  TEST_PACKAGE=com.mediatek.contactstest

  # remove junit-report and coverage.ec
  adb shell rm /data/data/$PACKAGE/files/coverage.ec
  adb shell rm /data/data/$PACKAGE/files/junit-report.xml
    
  # Run instrumentation test
  adb shell am instrument -e coverage true -w $TEST_PACKAGE/$TEST_RUNNER
  
  # get the report to PC
  #rm $SRC_ROOT/___test_report/junit-report-sms.xml
  #rm $SRC_ROOT/___test_report/junit-report-contact.xml
  #rm $SRC_ROOT/___test_report/junit-report-stk.xml
  #rm $SRC_ROOT/___test_report/junit-report-vt.xml
  adb pull /data/data/$PACKAGE/files/junit-report.xml $SRC_ROOT/___test_report/junit-report-contact.xml
  adb pull /sdcard/mtklog $SRC_ROOT/___test_report/mtklog_contact

  log_path=`cat $SRC_ROOT/.log_path`
  echo $log_path
  if [ "$log_path" = "persist.mtklog.log2sd.path = /mnt/sdcard2" ]; then              
    adb pull /sdcard2/mtklog $SRC_ROOT/___test_report/mtklog_contact  
  else
    adb pull /sdcard/mtklog $SRC_ROOT/___test_report/mtklog_contact
  fi
  
  # Pull performance test data
  #adb pull /data/data/$PACKAGE/app_perf $SRC_ROOT/perf-$PACKAGE

  # Generate emma code coverage report
  #cd $INTERMEDIATES
  #adb pull /data/data/$PACKAGE/files/coverage.ec
  #java -cp ~/local/emma/lib/emma.jar emma report -r xml -in coverage.ec -in coverage.em
fi
