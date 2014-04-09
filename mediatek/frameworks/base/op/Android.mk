# limitations under the License.
#

# This makefile shows how to build your own shared library that can be
# shipped on the system of a phone, and included additional examples of
# including JNI code with the library and writing client applications against it.

ifndef OPTR_SPEC_SEG_DEF 
    LOCAL_PATH := $(call my-dir)
    
    # MediaTek op library.
    # ============================================================
    include $(CLEAR_VARS)
    
    LOCAL_MODULE := mediatek-op
    
    LOCAL_SRC_FILES := Dummy.java

    # Specify install path for MTK CIP solution.
    ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
    LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/framework
    endif
    
    include $(BUILD_JAVA_LIBRARY)
else
    ifeq ($(OPTR_SPEC_SEG_DEF),NONE)
        LOCAL_PATH := $(call my-dir)
        
        # MediaTek op library.
        # ============================================================
        include $(CLEAR_VARS)
        
        LOCAL_MODULE := mediatek-op
        
        LOCAL_SRC_FILES := Dummy.java

        # Specify install path for MTK CIP solution.
        ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
        LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/framework
        endif
        
        include $(BUILD_JAVA_LIBRARY)
    endif    
endif
