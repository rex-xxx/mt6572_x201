LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# files that live under /system/etc/...

PRODUCT_COPY_FILES += $(LOCAL_PATH)/auto_pairing.conf:system/etc/bluetooth/auto_pairing.conf

PRODUCT_COPY_FILES += $(LOCAL_PATH)/blacklist.conf:system/etc/bluetooth/blacklist.conf

PRODUCT_COPY_FILES += $(LOCAL_PATH)/audio.conf:system/etc/bluetooth/audio.conf

PRODUCT_COPY_FILES += $(LOCAL_PATH)/input.conf:system/etc/bluetooth/input.conf

PRODUCT_COPY_FILES += $(LOCAL_PATH)/network.conf:system/etc/bluetooth/network.conf