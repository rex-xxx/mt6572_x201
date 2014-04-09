LOCAL_PATH:= $(call my-dir)

# Build the Phone app which includes the emergency dialer. See Contacts
# for the 'other' dialer.
include $(CLEAR_VARS)

ifeq ($(MTK_BT_SUPPORT), yes)
### generate AndroidManifest.xml
$(warning $(LOCAL_PATH)/build/blueangel.py])
PY_RES := $(shell python $(LOCAL_PATH)/build/blueangel.py)
endif

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# comment for MR1 migration
# keep it for switching android BT & MTK BT
#ifeq ($(MTK_BT_SUPPORT), yes)
#LOCAL_SRC_FILES := $(filter-out src/com/android/phone/BluetoothAtPhonebook.java, $(LOCAL_SRC_FILES))
#LOCAL_SRC_FILES := $(filter-out src/com/android/phone/BluetoothHandsfree.java, $(LOCAL_SRC_FILES))
#LOCAL_SRC_FILES := $(filter-out src/com/android/phone/BluetoothHeadsetService.java, $(LOCAL_SRC_FILES))
LOCAL_SRC_FILES := $(filter-out src/com/android/phone/BluetoothPhoneService.java, $(LOCAL_SRC_FILES))
#else
#LOCAL_SRC_FILES := $(filter-out src/com/mediatek/blueangel/BluetoothAtPhonebook.java, $(LOCAL_SRC_FILES))
#LOCAL_SRC_FILES := $(filter-out src/com/mediatek/blueangel/BluetoothHandsfree.java, $(LOCAL_SRC_FILES))
#LOCAL_SRC_FILES := $(filter-out src/com/mediatek/blueangel/BluetoothHeadsetService.java, $(LOCAL_SRC_FILES))
#LOCAL_SRC_FILES := $(filter-out src/com/mediatek/blueangel/BluetoothPhoneService.java, $(LOCAL_SRC_FILES))
#endif

LOCAL_SRC_FILES += \
        src/com/android/phone/EventLogTags.logtags \
        src/com/android/phone/INetworkQueryService.aidl \
        src/com/android/phone/INetworkQueryServiceCallback.aidl \
        src/com/mediatek/phone/recording/IPhoneRecorder.aidl\
        src/com/mediatek/phone/recording/IPhoneRecordStateListener.aidl

LOCAL_PACKAGE_NAME := Phone
LOCAL_CERTIFICATE := platform
LOCAL_STATIC_JAVA_LIBRARIES := com.android.phone.common \
                               CellConnUtil \
                               com.mediatek.phone.ext

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += mediatek-common

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Flag for LCA ram optimize.
ifeq (yes, strip$(MTK_LCA_RAM_OPTIMIZE))
    LOCAL_AAPT_FLAGS := --utf16
endif

include $(BUILD_PACKAGE)

# Build the test package
include $(call all-makefiles-under,$(LOCAL_PATH))
