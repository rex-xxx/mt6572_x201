#ifndef __COM_MEDIATEK_NFC_DEMO_H__
#define __COM_MEDIATEK_NFC_DEMO_H__

#include <JNIHelp.h>
#include <jni.h>

#include <stdlib.h>
#include <stdio.h>
#include <getopt.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <pthread.h>
#include <ctype.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "utilities.h"
#include <android/log.h>
#undef LOG_TAG
#define LOG_TAG "NfcDemo"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG,__VA_ARGS__)

/***************************************************************************** 
 * macro for /dev/msr3110
 *****************************************************************************/ 
#define MSR3110_DEV_MAGIC_ID 0xCD
#define MSR3110_IOCTL_FW_UPGRADE _IOW( MSR3110_DEV_MAGIC_ID, 0x00, int)
#define MSR3110_IOCTL_SET_VEN _IOW( MSR3110_DEV_MAGIC_ID, 0x01, int)
#define MSR3110_IOCTL_SET_RST _IOW( MSR3110_DEV_MAGIC_ID, 0x02, int)
#define MSR3110_IOCTL_IRQ _IOW( MSR3110_DEV_MAGIC_ID, 0x03, int)
#define MSR3110_IOCTL_IRQ_ABORT _IOW( MSR3110_DEV_MAGIC_ID, 0x04, int)

#define MSR3110_DEV_NAME    "/dev/msr3110"
#define MSR3110_READ_BUF_SIZE_MAX 256

typedef enum {
    DrvPL_Category_Reader = 0x00,
    DrvPL_Category_Card = 0x40,
    DrvPL_Category_Target = 0x80,
    DrvPL_Category_Mask = 0xC0,
} DrvPL_Category_et;

typedef enum {
    DrvPL_PLItem_R_Type1 = DrvPL_Category_Reader|0x08,  // 0x08
    DrvPL_PLItem_R_Felica = DrvPL_Category_Reader|0x06, // 0x06
    DrvPL_PLItem_R_ISO693 = DrvPL_Category_Reader|0x04, // 0x04
    DrvPL_PLItem_R_ISO43A = DrvPL_Category_Reader|0x00, // 0x00- use level-4 , TO DO discussion...

    DrvPL_PLItem_PI_P2P_106 = DrvPL_Category_Target|0x07, // 0x87
    DrvPL_PLItem_PI_P2P_212 = DrvPL_Category_Target|0x20, // 0xA0
    DrvPL_PLItem_PI_P2P_424 = DrvPL_Category_Target|0x21, // 0xA1

    DrvPL_PLItem_PT_P2P_106 = DrvPL_Category_Target|0x17, // 0x97
    DrvPL_PLItem_PT_P2P_212 = DrvPL_Category_Target|0x22, // 0xA2
    DrvPL_PLItem_PT_P2P_424 = DrvPL_Category_Target|0x23, // 0xA3
    DrvPL_PLItem_C_ISO43A = DrvPL_Category_Card|0x10, // 0x50
} DrvPL_PLItem_et;

typedef enum {
    TAG_TYPE_ALL = 1000,
    TAG_TYPE_FELICA,    //1001
    TAG_TYPE_14443A,    //1002
    TAG_TYPE_TYPE1,
    TAG_TYPE_15693,
    TAG_TYPE_P2P_I,
    TAG_TYPE_P2P_T,
} Tag_Type_et;

typedef enum {
    CARD_TYPE_ALL = 3000,
    CARD_TYPE_A,    //3001
    CARD_TYPE_B,    //3002
    CARD_TYPE_AB,
} Card_Type_et;

typedef enum {
    CRC_ISO_14443A = 0,
    CRC_ISO_14443B,
    CRC_ISO_15693,
    CRC_ISO_18092_106,
    CRC_ISO_18092_248,
    CRC_EPC_C1G2,
    CRC_HWIF,
    CRC_HWIF_SDIO,
    MAX_CRC_TYPE
} CRCTypeID;

typedef enum {
    SUCCESS = 0,
    I2C_SEND_ERROR,
    I2C_RECEIVE_ERROR,
    DEP_ERROR,
    FIELD_OFF,
    CHECKSUM_ERROR,
    CRC_ERROR,
    JNI_ERROR
} Error_Code;



#ifdef __cplusplus
} // extern c
#endif

namespace android {

extern JavaVM *vm;

int register_com_mediatek_nfcdemo_NativeNfcManager(JNIEnv *e);
JavaVM * getJavaVM();
JNIEnv *nfc_get_env();

}

#endif

