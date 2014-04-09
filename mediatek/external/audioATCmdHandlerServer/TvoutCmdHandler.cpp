/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2009
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/


#include "tv_out.h"
#include "tvout_patterns.h"
#include "TvoutCmdHandler.h"
#include "AudioCmdHandler.h"

#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>

using namespace android;


#if defined(MTK_TVOUT_SUPPORT)
static int tvoutIoctl(int code, unsigned int value = 0)
{
	int fd = open("/dev/TV-out", O_RDONLY, 0);
	int ret;
    if (fd >= 0) {
        ret = ioctl(fd, code, value);
        if (ret == -1) {
            ALOGE("[TVOut] [%s] failed. ioctlCode: %d, errno: %d",
                 __func__, code, errno);
            return -1;
        }
        close(fd);
    } else {
        ALOGE("[TVOut] [%s] open TVout failed. errno: %d", __func__, errno);
        return -1;
    }

    return ret;
}
#endif


int DoTVoutOpen(AudioCmdParam &mAduioCmdParams)
{
    ALOGD("%s(), %d %d %d %d",
        __func__,
        mAduioCmdParams.param1,
        mAduioCmdParams.param2,
        mAduioCmdParams.param3,
        mAduioCmdParams.param4);
	int ret = -1;
#if defined MTK_TVOUT_SUPPORT

    if (mAduioCmdParams.param1 != 0) {
        ret  = tvoutIoctl(TVOUT_TURN_ON, true);
    } else {
        ret  = tvoutIoctl(TVOUT_TURN_ON, false);
    }

#endif

    return ret;

}


int DoTVoutOpenDirectly(AudioCmdParam &mAduioCmdParams)
{
    ALOGD("%s(), %d %d %d %d",
        __func__,
        mAduioCmdParams.param1,
        mAduioCmdParams.param2,
        mAduioCmdParams.param3,
        mAduioCmdParams.param4);
	int ret = -1;
#if defined MTK_TVOUT_SUPPORT

    if (mAduioCmdParams.param1 != 0) {
        ret  = tvoutIoctl(TVOUT_PLUG_IN_DIRECTLY);
    } else {
        ret  = tvoutIoctl(TVOUT_PLUG_OUT_DIRECTLY);
    }

#endif

    return ret;

}


int DoTVoutSetSystem(AudioCmdParam &mAduioCmdParams)
{
    ALOGD("%s(), %d %d %d %d",
        __func__,
        mAduioCmdParams.param1,
        mAduioCmdParams.param2,
        mAduioCmdParams.param3,
        mAduioCmdParams.param4);

        int ret = -1;
#if defined MTK_TVOUT_SUPPORT

    if (mAduioCmdParams.param1 != 0) {
        ret = tvoutIoctl(TVOUT_SET_TV_SYSTEM, TVOUT_SYSTEM_PAL);
    } else {
        ret = tvoutIoctl(TVOUT_SET_TV_SYSTEM, TVOUT_SYSTEM_NTSC);
    }

#endif

    return ret;
}

static unsigned char* patBufRGB = NULL;
static unsigned char* patBufYUV = NULL;


int DoTVoutShowPattern(AudioCmdParam &mAduioCmdParams)
{
    ALOGD("%s(), %d %d %d %d",
        __func__,
        mAduioCmdParams.param1,
        mAduioCmdParams.param2,
        mAduioCmdParams.param3,
        mAduioCmdParams.param4);

   int ret = -1;
   int id = mAduioCmdParams.param1;
#if defined (MTK_TVOUT_SUPPORT)
    unsigned int    patBufSize;
    TVOutPatInfo    patInfo;
    TVOUT_HQA_BUF   tvBuf;
    unsigned char**  patBufAddr;

    TVOutPatterns* pattern = new(TVOutPatterns);
    if (pattern == NULL) {
        ALOGE("[TVOut] pattern can not be used");
        goto End;
    }

    if (pattern->checkID(id) == false) {
        ALOGE("[TVOut] pattern id error %d", id);
        delete pattern;
        goto End;
    }

    pattern->getPatternInfo(id, &patInfo);
    if (patInfo.fmt == TVOUT_FMT_RGB565) {
        patBufSize = TVOUT_PATTERN_RGB_SIZE;
        patBufAddr = &patBufRGB;
    } else if (patInfo.fmt == TVOUT_FMT_YUV420_PLANAR) {
        patBufSize = TVOUT_PATTERN_YUV_SIZE;

        patBufAddr = &patBufYUV;
    } else {
        ALOGE("[TVOut] unsupport pattern");
        delete pattern;
        goto End;
    }

    if (*patBufAddr == NULL) {
        ALOGI("allocate pat buffer 0x%x", patBufSize);
        *patBufAddr = (unsigned char*)malloc(patBufSize);
        if (*patBufAddr == NULL) {
            ALOGE("[TVOut] JNI buffer can not be allocated");
            delete pattern;
            goto End;
        }
    }

    ALOGD("%s() id=%d, patBuf=0x%x, size=0x%x\n", __func__, id, *patBufAddr, patBufSize);

    pattern->getUnzippedPattern(id, *patBufAddr, patBufSize);
    delete pattern;

    tvBuf.phy_addr = NULL;
    tvBuf.vir_addr = (void*)(*patBufAddr);
    tvBuf.format = patInfo.fmt;
    tvBuf.width = patInfo.w;
    tvBuf.height = patInfo.h;

    ret = tvoutIoctl(TVOUT_CTL_POST_HQA_BUFFER , (unsigned int)&tvBuf);

#endif

End:
    return ret;

}


int DoTVoutLeavePattern(AudioCmdParam &mAduioCmdParams)
{
    int ret = -1;

#if defined (MTK_TVOUT_SUPPORT)
    ret = tvoutIoctl(TVOUT_CTL_LEAVE_HQA_MODE);

    if (patBufRGB != NULL) {
        ALOGI("free RGB pat buffer");
        free(patBufRGB);
        patBufRGB = NULL;
    }

    if (patBufYUV != NULL) {
        ALOGI("free YUV pat buffer");
        free(patBufYUV);
        patBufYUV = NULL;
    }


#endif
    return ret;

}



