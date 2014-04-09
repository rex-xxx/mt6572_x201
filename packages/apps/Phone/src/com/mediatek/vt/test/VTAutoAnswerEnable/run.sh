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

  # mediatek_external_VT	
  # VT	
  adb push $PRODUCT_OUT/system/lib/libmtk_vt_utils.so /system/lib
  adb push $PRODUCT_OUT/system/lib/libmtk_vt_client.so /system/lib
  adb push $PRODUCT_OUT/system/lib/libmtk_vt_em.so /system/lib
  adb push $PRODUCT_OUT/system/lib/libmtk_vt_service.so /system/lib
  adb push $PRODUCT_OUT/system/lib/libmtk_vt_swip.so /system/lib
  adb shell sync
  
  #  push APK
  adb install -r $SRC_ROOT/___test_report/VTAutoAnswerEnable.apk
  adb shell sync
  
  # restart VM
  adb shell "stop;sleep 5;start"
  sleep 30
    
  # launch it  
  adb shell am start -n com.mtk.vtautoanswer/com.mtk.vtautoanswer.VTAutoAnswerEnable
    
fi
