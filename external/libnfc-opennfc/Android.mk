ifeq ($(MTK_NFC_INSIDE), yes)
MTK_NFC_OPENNFC := true
endif

ifeq ($(MTK_NFC_SUPPORT), yes)
MTK_NFC_OPENNFC := true
endif

ifeq ($(MTK_NFC_OPENNFC), true)
# flag to enable OpenNFCExtensions
EXTENDED_OPEN_NFC := true

###########"Hardware

# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License. 

# Modified by Inside Secure

#include external/libnfc-opennfc/OpenNFCFlags.mk 

$(info) 
$(info ============= NFC Configuration parameters ===============)
$(info DISABLE_OPEN_NFC_EXTENSIONS_API=$(DISABLE_OPEN_NFC_EXTENSIONS_API))
$(info DISABLE_NFCC_AUTO_FIRMWARE_UPDATE=$(DISABLE_NFCC_AUTO_FIRMWARE_UPDATE))
$(info CARD_EMULATION_FOR_UICC_DISABLED_BY_DEFAULT=$(CARD_EMULATION_FOR_UICC_DISABLED_BY_DEFAULT))
$(info ENABLE_NFC_TAG_APPLICATION=$(ENABLE_NFC_TAG_APPLICATION))
$(info ENABLE_OPEN_NFC_SIMULATOR=$(ENABLE_OPEN_NFC_SIMULATOR))
$(info ==========================================================)


###########"Hardware
ifeq ($(MTK_NFC_INSIDE), yes)
$(info  Building NFC HAL library)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifdef KERNEL_DIR
$(info KERNEL_DIR=$(KERNEL_DIR))
else
KERNEL_DIR=kernel
endif

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng
LOCAL_ARM_MODE := arm

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/sources) \
						 $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/porting)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/interfaces	\
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/porting	\
						$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/include	\
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread \
						$(KERNEL_DIR)/drivers/nfc
										
						
LOCAL_MODULE := libnfc_hal_microread

include $(BUILD_SHARED_LIBRARY)
endif

################""SIMULATOR

ifdef ENABLE_OPEN_NFC_SIMULATOR

$(info  "Building Open NFC Simulator library")

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/sources) \
						 $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/porting)


LOCAL_C_INCLUDES := $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/interfaces	\
						  $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/porting	\
						  $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/driver	\
						$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
						$(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
						$(LOCAL_PATH)/open_nfc/open_nfc/sources \
						$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/include	
						

LOCAL_CFLAGS += -DDO_NOT_USE_LWRAP_UNICODE

LOCAL_MODULE := nfc_hal_simulator


include $(BUILD_SHARED_LIBRARY)
endif

###########   MSR3110        #################
ifeq ($(MTK_NFC_SUPPORT), yes)
$(info  "Building MSR3110 library")

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng
LOCAL_ARM_MODE := arm

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

LOCAL_SRC_FILES := $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_msr3110/sources) \
                         $(call all-c-files-under, open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_msr3110/porting)


LOCAL_C_INCLUDES := $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_msr3110/interfaces   \
                        $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_msr3110/porting  \
                        $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_msr3110/sources  \
                        $(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
                        $(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
                        $(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
                        $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/include \


$(info  "C path")
$(info $(LOCAL_C_INCLUDES))

LOCAL_MODULE := libnfc_hal_msr3110

include $(BUILD_SHARED_LIBRARY)
endif


################""NFCC


$(info Building NFCC library)

#LOCAL_PATH := $(call my-dir)

# HAL module implemenation, not prelinked and stored in
# hw/<OVERLAY_HARDWARE_MODULE_ID>.<ro.product.board>.so
include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := liblog libdl

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS = optional eng

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := open_nfc/hardware/libhardware/modules/nfcc/nfcc/nfcc.c


LOCAL_C_INCLUDES :=	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces		\
					$(LOCAL_PATH)/open_nfc/open_nfc/porting/common	\
					$(LOCAL_PATH)/open_nfc/open_nfc/porting/server	\
					$(LOCAL_PATH)/open_nfc/hardware/libhardware/include

LOCAL_MODULE := nfcc.default


include $(BUILD_SHARED_LIBRARY)



##############CLIENT


$(info Building Open NFC Client JNI library)
#LOCAL_PATH:= $(call my-dir)

#---------------------------------------------------------------------------------
# Open NFC client dynamic library
#---------------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng
LOCAL_ARM_MODE := arm

# This is the target being built.
LOCAL_MODULE:= libopen_nfc_client_jni

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
	$(call all-c-files-under, open_nfc/open_nfc/sources) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/common) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/client)
	
#Additionnal headers

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
	$(LOCAL_PATH)/open_nfc/open_nfc/sources \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/client

LOCAL_CFLAGS := -D DO_NOT_USE_LWRAP_UNICODE=1

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils


# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true.
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#---------------------------------------------------------------------------------
# Open NFC server dynamic library
#---------------------------------------------------------------------------------

$(info Building Open NFC Server JNI library)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

# This is the target being built.
LOCAL_MODULE:= libopen_nfc_server_jni

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
	$(call all-c-files-under, open_nfc/open_nfc/sources) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/common) \
	$(call all-c-files-under, open_nfc/open_nfc/porting/server)

#Additionnal headers
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common \
	$(LOCAL_PATH)/open_nfc/open_nfc/porting/server \
	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include \
	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include/hardware \
	$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/porting \
	$(LOCAL_PATH)/open_nfc/open_nfc/sources \

ifeq ($(MTK_NFC_INSIDE), yes)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_microread/porting
endif

ifeq ($(MTK_NFC_SUPPORT), yes)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_msr3110/porting
endif

LOCAL_CFLAGS += -D DO_NOT_USE_LWRAP_UNICODE=1

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libhardware \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils  \

ifeq ($(MTK_NFC_INSIDE), yes)
LOCAL_SHARED_LIBRARIES += libnfc_hal_microread
endif

ifeq ($(MTK_NFC_SUPPORT), yes)
LOCAL_SHARED_LIBRARIES += libnfc_hal_msr3110
endif

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true.
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)


########################SERVER

$(info Building standalone Open NFC server application)
#LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)    

LOCAL_MODULE_TAGS	:=	optional	eng
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES		:=	open_nfc/standalone_server/src/standalone_server.c

LOCAL_C_INCLUDES	:=	$(LOCAL_PATH)/open_nfc/open_nfc/porting/common
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/open_nfc/porting/server
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/open_nfc/interfaces
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/include/hardware
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/porting
LOCAL_C_INCLUDES	+=	$(LOCAL_PATH)/open_nfc/hardware/libhardware/modules/nfcc/nfc_hal_simulator/porting


LOCAL_MODULE := server_open_nfc

LOCAL_STATIC_LIBRARIES := libcutils libc 

LOCAL_SHARED_LIBRARIES	:=	libopen_nfc_server_jni  \
	libhardware \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils


PRODUCT_COPY_FILES += $(LOCAL_PATH)/connection_center_access:data/connection_center_access

# Copy the firmware binary file in the system image
ifndef DISABLE_NFCC_AUTO_FIRMWARE_UPDATE
#PRODUCT_COPY_FILES += \
#    $(LOCAL_PATH)/microread_fw.bin:system/vendor/firmware/microread_fw.bin
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/MSR3110.BIN:system/vendor/firmware/MSR3110.BIN
endif

include $(BUILD_EXECUTABLE)


######################## MSR3110 TEST TOOL
ifeq ($(MTK_NFC_SUPPORT), yes)
include $(CLEAR_VARS)    

LOCAL_MODULE_TAGS	:=	optional	eng
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES		:=	msr3110_test/msr3110_dev_test.c


LOCAL_MODULE := msr3110_dev_test

LOCAL_STATIC_LIBRARIES := libcutils libc 

LOCAL_SHARED_LIBRARIES	:=	\
	libhardware \
	libandroid_runtime \
	libnativehelper \
	libcutils \
	libutils

include $(BUILD_EXECUTABLE)

endif


$(info Building NFC NDEF library)
#
# libnfc_ndef
#

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE := false

LOCAL_SRC_FILES := ndef/phFriNfc_NdefRecord.c

LOCAL_CFLAGS := -I$(LOCAL_PATH)/ndef

LOCAL_MODULE:= libnfc_ndef
LOCAL_MODULE_TAGS := optional eng
LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_SHARED_LIBRARY)


############################## OpenNFCExtensions API (to be used by external Java applications)
ifndef DISABLE_OPEN_NFC_EXTENSIONS_API
$(info Building Open NFC Extensions java library)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_SUB_PATH			:= java/src/com/opennfc/extension

LOCAL_SRC_FILES			:=	\
	        $(call all-java-files-under, $(LOCAL_SUB_PATH)) \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtSecureElement.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtService.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtFirmwareUpdate.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtCardEmulation.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtCardEmulationEventHandler.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtVirtualTag.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtVirtualTagEventHandler.aidl \
			$(LOCAL_SUB_PATH)/engine/IOpenNfcExtRoutingTable.aidl


LOCAL_AIDL_INCLUDES := external/libnfc-opennfc/java/src
$(info "LOCAL_AIDL_INCLUDES=$(LOCAL_AIDL_INCLUDES)") 							
$(info "LOCAL_SRC_FILES=$(LOCAL_SRC_FILES)") 

LOCAL_MODULE := NfcExt
LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := nfc_ext_jni

LOCAL_PROGUARD_ENABLED := disabled

#Manually copy the optional library XML files in the system image.

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/NfcExt.xml:system/etc/permissions/NfcExt.xml

include $(BUILD_JAVA_LIBRARY)
endif

#****************** include OpenNFC Extensions Service
ifndef DISABLE_OPEN_NFC_EXTENSIONS_API
include $(call all-makefiles-under,$(LOCAL_PATH))
endif

endif
