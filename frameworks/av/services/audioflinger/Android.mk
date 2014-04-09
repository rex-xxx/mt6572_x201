LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    ISchedulingPolicyService.cpp \
    SchedulingPolicyService.cpp

# FIXME Move this library to frameworks/native
LOCAL_MODULE := libscheduling_policy

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=               \
    AudioFlinger.cpp            \
    AudioMixer.cpp.arm          \
    AudioResampler.cpp.arm      \
    AudioPolicyService.cpp      \
    ServiceUtilities.cpp        \
	AudioResamplerCubic.cpp.arm \
    AudioResamplerSinc.cpp.arm

LOCAL_SRC_FILES += StateQueue.cpp

# uncomment for debugging timing problems related to StateQueue::push()
LOCAL_CFLAGS += -DSTATE_QUEUE_DUMP

LOCAL_C_INCLUDES := \
    $(call include-path-for, audio-effects) \
    $(call include-path-for, audio-utils)

# FIXME keep libmedia_native but remove libmedia after split
LOCAL_SHARED_LIBRARIES := \
    libaudioutils \
    libcommon_time_client \
    libcutils \
    libutils \
    libbinder \
    libmedia \
    libmedia_native \
    libnbaio \
    libhardware \
    libhardware_legacy \
    libeffects \
    libdl \
    libpowermanager

LOCAL_STATIC_LIBRARIES := \
    libscheduling_policy \
    libcpustats \
    libmedia_helper

#mtk added
ifeq ($(strip $(BOARD_USES_MTK_AUDIO)),true)
    AudioDriverIncludePath := aud_drv
    LOCAL_MTK_PATH:=../../../../mediatek/frameworks-ext/av/services/audioflinger

    LOCAL_CFLAGS += -DMTK_AUDIO

    LOCAL_C_INCLUDES += \
        $(TOP)/mediatek/frameworks-ext/av/include/media \
        $(TOP)/mediatek/frameworks-ext/av/services/audioflinger \
        $(TOP)/mediatek/kernel/include \
        $(MTK_PATH_SOURCE)/platform/common/hardware/audio/aud_drv \
        $(MTK_PATH_SOURCE)/platform/common/hardware/audio/ \
        $(MTK_PATH_PLATFORM)/hardware/audio/aud_drv \
        $(MTK_PATH_PLATFORM)/hardware/audio

    LOCAL_SRC_FILES += \
        $(LOCAL_MTK_PATH)/AudioHeadsetDetect.cpp \
        $(LOCAL_MTK_PATH)/AudioResamplermtk.cpp \
        $(LOCAL_MTK_PATH)/AudioUtilmtk.cpp

    LOCAL_SHARED_LIBRARIES += \
        libblisrc

# SRS Processing
    ifeq ($(strip $(HAVE_SRSAUDIOEFFECT_FEATURE)),yes)
        LOCAL_CFLAGS += -DHAVE_SRSAUDIOEFFECT
        include mediatek/binary/3rd-party/free/SRS_AudioEffect/srs_processing/AF_PATCH.mk
    endif
# SRS Processing

    ifeq ($(strip $(TARGET_BUILD_VARIANT)),eng)
  LOCAL_CFLAGS += -DDEBUG_AUDIO_PCM
    endif

# MATV ANALOG SUPPORT
    ifeq ($(HAVE_MATV_FEATURE),yes)
  ifeq ($(MTK_MATV_ANALOG_SUPPORT),yes)
    LOCAL_CFLAGS += -DMATV_AUDIO_LINEIN_PATH
  endif
    endif
# MATV ANALOG SUPPORT

# MTKLOUDNESS_EFFECT
    ifeq ($(strip $(HAVE_MTKLOUDNESS_EFFECT)),yes)
        LOCAL_CFLAGS += -DHAVE_MTKLOUDNESS_EFFECT
    endif
# MTKLOUDNESS_EFFECT

# MTK_DOWNMIX_ENABLE
	LOCAL_CFLAGS += -DMTK_DOWNMIX_ENABLE
# MTK_DOWNMIX_ENABLE	
endif

LOCAL_MODULE:= libaudioflinger

LOCAL_SRC_FILES += FastMixer.cpp FastMixerState.cpp

# uncomment for reducing memory
# LOCAL_CFLAGS += -DFAST_MIXER_STATISTICS

# uncomment to display CPU load adjusted for CPU frequency
# LOCAL_CFLAGS += -DCPU_FREQUENCY_STATISTICS

LOCAL_CFLAGS += -DSTATE_QUEUE_INSTANTIATIONS='"StateQueueInstantiations.cpp"'

LOCAL_CFLAGS += -UFAST_TRACKS_AT_NON_NATIVE_SAMPLE_RATE

# uncomment for systrace
# LOCAL_CFLAGS += -DATRACE_TAG=ATRACE_TAG_AUDIO

# uncomment for dumpsys to write most recent audio output to .wav file
# 47.5 seconds at 44.1 kHz, 8 megabytes
# LOCAL_CFLAGS += -DTEE_SINK_FRAMES=0x200000

# uncomment to enable the audio watchdog
# LOCAL_SRC_FILES += AudioWatchdog.cpp
# LOCAL_CFLAGS += -DAUDIO_WATCHDOG

include $(BUILD_SHARED_LIBRARY)

#
# build audio resampler test tool
#
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=               \
	test-resample.cpp 			\
    AudioResampler.cpp.arm      \
	AudioResamplerCubic.cpp.arm \
    AudioResamplerSinc.cpp.arm

LOCAL_SHARED_LIBRARIES := \
	libdl \
    libcutils \
    libutils

LOCAL_MODULE:= test-resample

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)


include $(call all-makefiles-under,$(LOCAL_PATH))
