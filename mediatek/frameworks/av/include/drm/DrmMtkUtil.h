/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#ifndef __DRM_MTK_UTIL_H__
#define __DRM_MTK_UTIL_H__

#include <drm/drm_framework_common.h>
#include <utils/RefBase.h>
#include <utils/String8.h>
#include <utils/threads.h>
#include <stdio.h>

namespace android {

class DrmRights;
class DecryptHandle;
class DrmManagerClient;

class DrmMtkUtil {
private:
    DrmMtkUtil();

public:
    static bool installDrmMsg(const String8& filePath);
    static String8 getContentId(const DrmRights& rights);

private:
    static bool getNextNELineContain(FILE* fp, String8& line, String8 contained);
    static int getNextNELine(FILE* fp, char* line);
    static bool getNextNELineTrimR(FILE* fp, String8& line);
    static bool getLineX(FILE* fp, String8& line);
    static bool getLineXTrimR(FILE* fp, String8& line);
    static bool getFLSignature(FILE* fp, int offset, int dataLen, char* sig);
    static void getDcfTmpFileName(String8 dm, String8& dcf);
    static void renameDcfTmp(String8 dcfTmp);
public:
    static Mutex mROLock;

public:
    static int saveIMEI(const String8& imei);
    static int saveId(const String8& id);
    static String8 loadId();
    static bool isDcf(int fd);
    static bool isDcf(const String8& path);
    static bool isDcf(const String8& path, int fd);
    static String8 getDcfMime(int fd);
    static String8 getDcfMime(const String8& path);
    static String8 toCommonMime(const char* mime);
    static String8 getProcessName(pid_t pid);
    static bool isTrustedClient(const String8& procName);
    static bool isTrustedVideoClient(const String8& procName);
    static long getContentSize(DecryptHandle* handle);
    static long getContentRawSize(sp<DecryptHandle> handle, DrmManagerClient* client);
    static long getContentLength(DecryptHandle* handle, DrmManagerClient* client);
};

}

#endif // __DRM_MTK_UTIL_H__
