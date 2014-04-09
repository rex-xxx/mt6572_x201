##
## Copyright (C) 2012 The Android Open Source Project
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##

LOCAL_PATH:= $(call my-dir)

#############################################################
#   build the harfbuzz library
#

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
    src/Bidi.cpp \
    src/CachedFace.cpp \
    src/CmapCache.cpp \
    src/Code.cpp \
    src/direct_machine.cpp \
    src/Face.cpp \
    src/FeatureMap.cpp \
    src/FileFace.cpp \
    src/Font.cpp \
    src/GlyphCache.cpp \
    src/GlyphFace.cpp \
    src/gr_face.cpp \
    src/gr_font.cpp \
    src/gr_logging.cpp \
    src/gr_segment.cpp \
    src/gr_slot.cpp \
    src/json.cpp \
    src/Justifier.cpp \
    src/NameTable.cpp \
    src/Pass.cpp \
    src/SegCache.cpp \
    src/SegCacheEntry.cpp \
    src/SegCacheStore.cpp \
    src/Segment.cpp \
    src/Silf.cpp \
    src/Slot.cpp \
    src/Sparse.cpp \
    src/TtfUtil.cpp \
    src/UtfCodec.cpp

LOCAL_CPP_EXTENSION := .cpp

LOCAL_SHARED_LIBRARIES := \
        libstdc++ \
        libstlport \
        libcutils \
        libutils

LOCAL_C_INCLUDES += \
        bionic \
        external/stlport/stlport \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/src

LOCAL_CFLAGS += 

LOCAL_LDLIBS += -lpthread

LOCAL_MODULE:= libgraphite2

include $(BUILD_SHARED_LIBRARY)


