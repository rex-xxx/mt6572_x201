/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#define LOG_TAG "TVOUT_JNI"

#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>

#include <utils/misc.h>
#include <utils/Log.h>
#include <cutils/xlog.h>


#include "jni.h"
#include "JNIHelp.h"

#include "mtkfb.h"
#include "tv_out.h"
#include "tvout_patterns.h"


#define LOG_TAG "TV/JNI"


using namespace android;

#ifdef __cplusplus
    extern "C" {
#endif

#if defined(MTK_TVOUT_SUPPORT)
static jboolean tvoutIoctl(int code, unsigned int value = 0)
{
	int fd = open("/dev/TV-out", O_RDONLY, 0);
	int ret;
    if (fd >= 0) {
        ret = ioctl(fd, code, value);
        if (ret == -1) {
            XLOGE("[TVOut] [%s] failed. ioctlCode: %d, errno: %d",
                 __func__, code, errno);
            return 0;
        }
        close(fd);
    } else {
        XLOGE("[TVOut] [%s] open mtkfb failed. errno: %d", __func__, errno);
        return 0;
    }
    return ret;
}

#endif


static jboolean enableTvOut(JNIEnv *env, jobject clazz, jboolean enable) {
    bool ret = false;
#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_TURN_ON, enable);
#endif

	XLOGI("[TVOut] %s(%d) ret = %d\n", __func__, enable, ret);
    return ret;
}

static jboolean IPOPowerOff(JNIEnv *env, jobject clazz) {
    bool ret = false;
#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_IPO_POWER_OFF);
#endif

	XLOGI("[TVOut] %s ret = %d\n", __func__, ret);
    return ret;
}

static jboolean setTvSystem(JNIEnv* env, jobject clazz, jint tvSystem) {
    bool ret = false;
#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_SET_TV_SYSTEM, tvSystem);
#endif
	XLOGI("[TVOut] %s(%d) ret = %d\n", __func__, tvSystem, ret);
    return ret;
}


static jboolean isShowButton(JNIEnv* env, jobject clazz) {
    bool ret = false;
#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_ISSHOW_TVOUTBUTTON);
#endif
	XLOGI("[TVOut] %s ret = %d\n", __func__, ret);
    return ret;
}

static jboolean disableVideoMode(JNIEnv* env, jobject clazz, jboolean enable) {
    bool ret = false;
#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_DISABLE_VIDEO_MODE, enable);
#endif
	XLOGI("[TVOut] %s(%d) ret = %d\n", __func__, enable, ret);
    return ret;
}


static unsigned char* patBufRGB = NULL;
static unsigned char* patBufYUV = NULL;

static jboolean showPattern(JNIEnv* env, jobject clazz, jint id) {
    unsigned int ret;

#if defined (MTK_TVOUT_SUPPORT)
    unsigned int    patBufSize;
    TVOutPatInfo    patInfo;
    TVOUT_HQA_BUF   tvBuf;
    unsigned char**  patBufAddr;

    TVOutPatterns* pattern = new(TVOutPatterns);
    if (pattern == NULL) {
        XLOGE("[TVOut] pattern can not be used");
        return false;
    }

    if (pattern->checkID(id) == false) {
        XLOGE("[TVOut] pattern id error %d", id);
        delete pattern;
        return false;
    }

    pattern->getPatternInfo(id, &patInfo);
    if (patInfo.fmt == TVOUT_FMT_RGB565) {
        patBufSize = TVOUT_PATTERN_RGB_SIZE;
        patBufAddr = &patBufRGB;
    } else if (patInfo.fmt == TVOUT_FMT_YUV420_PLANAR) {
        patBufSize = TVOUT_PATTERN_YUV_SIZE;

        patBufAddr = &patBufYUV;
    } else {
        XLOGE("[TVOut] unsupport pattern");
        delete pattern;
        return false;
    }

    if (*patBufAddr == NULL) {
        XLOGI("allocate pat buffer 0x%x", patBufSize);
        *patBufAddr = (unsigned char*)malloc(patBufSize);
        if (*patBufAddr == NULL) {
            XLOGE("[TVOut] JNI buffer can not be allocated");
            delete pattern;
            return false;
        }
    }

    XLOGD("%s() id=%d, patBuf=0x%x, size=0x%x\n", __func__, id, *patBufAddr, patBufSize);

    pattern->getUnzippedPattern(id, *patBufAddr, patBufSize);
    delete pattern;

    tvBuf.phy_addr = NULL;
    tvBuf.vir_addr = (void*)(*patBufAddr);
    tvBuf.format = patInfo.fmt;
    tvBuf.width = patInfo.w;
    tvBuf.height = patInfo.h;

    ret = tvoutIoctl(TVOUT_CTL_POST_HQA_BUFFER , (unsigned int)&tvBuf);
    if (ret < 0)
        return false;

#endif

    return true;


}

static jboolean leavePattern(JNIEnv* env, jobject clazz) {
    unsigned int ret;

#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_CTL_LEAVE_HQA_MODE);

    if (patBufRGB != NULL) {
        XLOGI("free RGB pat buffer");
        free(patBufRGB);
        patBufRGB = NULL;
    }

    if (patBufYUV != NULL) {
        XLOGI("free YUV pat buffer");
        free(patBufYUV);
        patBufYUV = NULL;
    }

    if (ret < 0)
        return false;

#endif
    return true;

}


static jboolean enableTvOutManual(JNIEnv *env, jobject clazz, jboolean enable) {
    bool ret = false;
	XLOGI("[TVOut] %s(%d) ret = %d\n", __func__, enable, ret);
    return ret;
}

static jboolean tvoutPowerEnable(JNIEnv *env, jobject clazz, jboolean enable) {
    bool ret = false;
#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_POWER_ENABLE, enable);
#endif
	XLOGI("[TVOut] %s(%d) ret = %d\n", __func__, enable, ret);
    return ret;
}




// --------------------------------------------------------------------------

static JNINativeMethod gNotify[] = {
    { "enableTvOut", "(Z)Z", (void*)enableTvOut },
    { "setTvSystem", "(I)Z", (void*)setTvSystem },
    { "isShowButton", "()Z", (void*)isShowButton },
    { "disableVideoMode", "(Z)Z", (void*)disableVideoMode },
    { "showPattern", "(I)Z", (void*)showPattern },
    { "leavePattern", "()Z", (void*)leavePattern },
    { "enableTvOutManual", "(Z)Z", (void*)enableTvOutManual },
    { "tvoutPowerEnable", "(Z)Z", (void*)tvoutPowerEnable },
    { "IPOPowerOff", "()Z", (void*)IPOPowerOff }
};

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    jint result = -1;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        XLOGE("GetEnv failed!");
        return result;
    }
    ALOG_ASSERT(env, "[TVOut] Could not retrieve the env!");

    int ret = jniRegisterNativeMethods(
        env, "com/mediatek/tvout/TVOUTNative", gNotify, NELEM(gNotify));

    if (ret) {
        XLOGE("[TVOut] call jniRegisterNativeMethods() failed, ret:%d\n", ret);
    }

    return JNI_VERSION_1_4;
}

#ifdef __cplusplus
    }
#endif


