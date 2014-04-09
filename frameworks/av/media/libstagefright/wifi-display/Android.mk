LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MTK_PATH:=../../../../../mediatek/frameworks-ext/av/media/libstagefright/wifi-display-mediatek

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
endif


ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_SRC_FILES:= \
        ANetworkSession.cpp             \
        Parameters.cpp                  \
        ParsedMessage.cpp               \
        sink/LinearRegression.cpp       \
        sink/RTPSink.cpp                \
        sink/TunnelRenderer.cpp         \
        sink/WifiDisplaySink.cpp        \
        source/Converter.cpp            \
        source/MediaPuller.cpp          \
        source/PlaybackSession.cpp      \
        source/RepeaterSource.cpp       \
        source/Sender.cpp               \
        source/TSPacketizer.cpp         \
        TimeSeries.cpp                  \
        source/WifiDisplaySource.cpp
else

LOCAL_SRC_FILES:= \
        $(LOCAL_MTK_PATH)/ANetworkSession.cpp             \
        $(LOCAL_MTK_PATH)/Parameters.cpp                  \
        $(LOCAL_MTK_PATH)/ParsedMessage.cpp               \
        $(LOCAL_MTK_PATH)/sink/LinearRegression.cpp       \
        $(LOCAL_MTK_PATH)/sink/RTPSink.cpp                \
        $(LOCAL_MTK_PATH)/sink/TunnelRenderer.cpp         \
        $(LOCAL_MTK_PATH)/sink/WifiDisplaySink.cpp        \
        $(LOCAL_MTK_PATH)/source/Converter.cpp            \
        $(LOCAL_MTK_PATH)/source/MediaPuller.cpp          \
        $(LOCAL_MTK_PATH)/source/PlaybackSession.cpp      \
        $(LOCAL_MTK_PATH)/source/RepeaterSource.cpp       \
        $(LOCAL_MTK_PATH)/source/Sender.cpp               \
        $(LOCAL_MTK_PATH)/source/TSPacketizer.cpp         \
        $(LOCAL_MTK_PATH)/source/WifiDisplaySource.cpp    \
        $(LOCAL_MTK_PATH)/uibc/UibcMessage.cpp            \
        $(LOCAL_MTK_PATH)/YUVFilesource.cpp               \
        $(LOCAL_MTK_PATH)/Serializer.cpp   \
        $(LOCAL_MTK_PATH)/TimeSeries.cpp
        


#check if use MTK Source
#ifeq ($(strip $(MTK_WFD_SUPPORT)),yes)
LOCAL_SRC_FILES+= \
        $(LOCAL_MTK_PATH)/source/ExternalDisplaySource.cpp

LOCAL_CFLAGS += -DUSE_MTKSOURCE
#endif


endif



ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        $(TOP)/frameworks/av/media/libstagefright/mpeg2ts \
        $(TOP)/frameworks/av/media/libstagefright/wifi-display
else
LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        $(TOP)/frameworks/av/media/libstagefright/mpeg2ts \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/wifi-display-mediatek\
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/wifi-display-mediatek\uibc
        
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/kernel/drivers/video
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/kernel/drivers/hdmitx   
LOCAL_C_INCLUDES += \
        $(TOP)/frameworks/native/include/media/hardware \
        $(TOP)/external/expat/lib \
        $(TOP)/external/flac/include \
        $(TOP)/external/tremolo \
        $(MTK_PATH_SOURCE)/frameworks/av/media/libstagefright/include \
        $(TOP)/mediatek/frameworks/av/media/libstagefright/include/omx_core \
        $(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright \
        $(TOP)/external/openssl/include \
        $(TOP)/frameworks/av/media/libstagefright/include/omx_core \
        $(TOP)/frameworks/av/media/libstagefright/include \
        $(TOP)/mediatek/frameworks-ext/av/media/libstagefright/include \
        $(MTK_PATH_SOURCE)/kernel/include \
        $(TOP)/external/skia/include/images \
        $(TOP)/external/skia/include/core \
        $(TOP)/system/core/include/system \
        $(TOP)/hardware/libhardware_legacy/include/hardware_legacy
endif

LOCAL_SHARED_LIBRARIES:= \
        libbinder                       \
        libcutils                       \
        libgui                          \
        libmedia                        \
        libstagefright                  \
        libstagefright_foundation       \
        libui                           \
        libutils
        
#add for mmprofile code        
ifneq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)

LOCAL_C_INCLUDES += \
	 $(MTK_PATH_SOURCE)/kernel/include \
        $(TOPDIR)/kernel/include \
        $(TOPDIR)/system/core/include \

LOCAL_SHARED_LIBRARIES += \
           libmmprofile
           
           
LOCAL_CFLAGS += -DUSE_MMPROFILE

endif   

LOCAL_MODULE:= libstagefright_wfd

LOCAL_MODULE_TAGS:= optional

include $(BUILD_SHARED_LIBRARY)

################################################################################

include $(CLEAR_VARS)

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_CFLAGS += -DANDROID_DEFAULT_CODE
endif

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_SRC_FILES:= \
        wfd.cpp                 
else
LOCAL_SRC_FILES:= \
        $(LOCAL_MTK_PATH)/wfd.cpp                 
endif

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        $(TOP)/frameworks/av/media/libstagefright/mpeg2ts \
        $(TOP)/frameworks/av/media/libstagefright/wifi-display
else
LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        $(TOP)/frameworks/av/media/libstagefright/mpeg2ts \
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/wifi-display-mediatek\
        $(TOP)/$(MTK_PATH_SOURCE)/frameworks-ext/av/media/libstagefright/wifi-display-mediatek\uibc
endif

LOCAL_SHARED_LIBRARIES:= \
        libbinder                       \
        libgui                          \
        libmedia                        \
        libstagefright                  \
        libstagefright_foundation       \
        libstagefright_wfd              \
        libutils                        \

LOCAL_MODULE:= wfd

LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)

################################################################################

include $(CLEAR_VARS)

ifeq ($(strip $(MTK_USE_ANDROID_MM_DEFAULT_CODE)),yes)
LOCAL_SRC_FILES:= \
        udptest.cpp                 
else
LOCAL_SRC_FILES:= \
        $(LOCAL_MTK_PATH)/udptest.cpp                 
endif

LOCAL_SHARED_LIBRARIES:= \
        libbinder                       \
        libgui                          \
        libmedia                        \
        libstagefright                  \
        libstagefright_foundation       \
        libstagefright_wfd              \
        libutils                        \

LOCAL_MODULE:= udptest

LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)

