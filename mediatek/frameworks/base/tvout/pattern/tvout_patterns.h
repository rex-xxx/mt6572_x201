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

#ifndef __TVOUT_ZIPPED_PATTERNS_H__
#define __TVOUT_ZIPPED_PATTERNS_H__

#define TVOUT_PATTERN_CNT (7)
#define TVOUT_PATTERN_WIDTH   720
#define TVOUT_PATTERN_HEIGHT  480

//RGB565 & YUV420, TVOUT_PATTERN_BUFFER_SIZE is the bigger one of them.
#define TVOUT_PATTERN_BUFFER_SIZE (TVOUT_PATTERN_WIDTH*TVOUT_PATTERN_HEIGHT*2)
#define TVOUT_PATTERN_RGB_SIZE (TVOUT_PATTERN_WIDTH*TVOUT_PATTERN_HEIGHT*2)
#define TVOUT_PATTERN_YUV_SIZE (TVOUT_PATTERN_WIDTH*TVOUT_PATTERN_HEIGHT*3/2)


#if defined (MTK_TVOUT_SUPPORT)


typedef struct {
    unsigned int w;
    unsigned int h;
    unsigned int size;
    TVOUT_SRC_FORMAT fmt;
} TVOutPatInfo;


class TVOutPatterns
{
    public:
        TVOutPatterns();
        ~TVOutPatterns();

        bool checkID(int ID);

        int getUnzippedPattern(int ID, unsigned char* memAddr, unsigned int& size);
        void getPatternInfo(int ID, TVOutPatInfo* Info);

    private:

        void getZippedPatternInfoByID(int ID, unsigned int & Addr, unsigned int & InLenth, unsigned int & OutLenth);
        bool unZipPattern(unsigned char *in, unsigned char *out, int inlen, int outlen);

    private:
        static unsigned char zp_wl_yuv[528];
        static unsigned char zp_lb_rgb565[4188];
        static unsigned char zp_lld_rgb565[3104];
        static unsigned char zp_cs_rgb565[694];
        static unsigned char zp_dg_yuv[2866];
        static unsigned char zp_cb_yuv[2604];
        static unsigned char zp_ln_yuv[1886];

};

#endif

#endif

