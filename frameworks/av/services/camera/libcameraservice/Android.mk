LOCAL_PATH:= $(call my-dir)

#
# libcameraservice
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=               \
    CameraService.cpp \
    CameraClient.cpp \
    Camera2Client.cpp \
    Camera2Device.cpp \
    camera2/CameraMetadata.cpp \
    camera2/Parameters.cpp \
    camera2/FrameProcessor.cpp \
    camera2/StreamingProcessor.cpp \
    camera2/JpegProcessor.cpp \
    camera2/CallbackProcessor.cpp \
    camera2/ZslProcessor.cpp \
    camera2/BurstCapture.cpp \
    camera2/JpegCompressor.cpp \
    camera2/CaptureSequencer.cpp

LOCAL_SHARED_LIBRARIES:= \
    libui \
    libutils \
    libbinder \
    libcutils \
    libmedia \
    libmedia_native \
    libcamera_client \
    libgui \
    libhardware \
    libsync \
    libcamera_metadata \
    libjpeg

LOCAL_C_INCLUDES += \
    system/media/camera/include \
    external/jpeg




#//!++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
ifeq "yes" "$(strip $(MTK_CAMERA_BSP_SUPPORT))"

    LOCAL_SRC_FILES += $(call all-c-cpp-files-under, ../../../../../$(MTK_ROOT)/frameworks-ext/av/services/camera/libcameraservice)

    LOCAL_C_INCLUDES += $(TOP)/$(MTK_ROOT)/frameworks-ext/av/include

    LOCAL_SHARED_LIBRARIES += libdl

ifneq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
    LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/camera/inc/common/camutils
    LOCAL_SHARED_LIBRARIES += libcam.utils
    LOCAL_CFLAGS += -DMTK_CAMERAPROFILE_SUPPORT
endif  
 
endif

ifeq "yes" "$(strip $(MTK_CAMERA_BSP_SUPPORT))"
ifneq ($(strip $(MTK_EMULATOR_SUPPORT)),yes)
#ifeq ($(HAVE_MATV_FEATURE),yes)
    LOCAL_CFLAGS += -DATVCHIP_MTK_ENABLE
#endif
endif
endif
#//!----------------------------------------------------------------------------

LOCAL_MODULE:= libcameraservice

include $(BUILD_SHARED_LIBRARY)
