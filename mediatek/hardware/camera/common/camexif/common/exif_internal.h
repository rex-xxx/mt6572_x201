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

/********************************************************************************************
 *     LEGAL DISCLAIMER 
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES 
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED 
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS 
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, 
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR 
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY 
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, 
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK 
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION 
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *     
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH 
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, 
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE 
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE. 
 *     
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS 
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.  
 ************************************************************************************************/
#ifndef _EXIF_INTERNAL_H_
#define _EXIF_INTERNAL_H_

#define LOG_TAG "mExif"

#include <cutils/log.h>
#include "exif_type.h"
#include "exif_api.h"
#include "exif_errcode.h"


/*******************************************************************************
*
********************************************************************************/
// Macro
#define READ32(addr)        *(unsigned int *) (addr)
#define WRITE32(addr, val)  *(unsigned int *) (addr) = (val)

#define MEXIF_LOG(...) \
        do { \
            printf("[%d] ", __LINE__); \
            printf(LOG_TAG ": " __VA_ARGS__); \
            ALOGD(__VA_ARGS__); \
        } while (0)

//LOGD(__VA_ARGS__); 

/*******************************************************************************
*
********************************************************************************/
// exif_init.cpp
unsigned int exifInit();

// exif_ifdinit.cpp
unsigned int ifdValueInit();
unsigned int ifdZeroIFDValInit(ifdNode_t *pnode, struct zeroIFDList *plist);
unsigned int ifdExifIFDValInit(ifdNode_t *pnode, struct exifIFDList *plist);
unsigned int ifdGpsIFDValInit(ifdNode_t *pnode, struct gpsIFDList *plist);
unsigned int ifdFirstIFDValInit(ifdNode_t *pnode, struct firstIFDList *plist);
unsigned int ifdItopIFDValInit(ifdNode_t *pnode, struct itopIFDList *plist);

// exif_ifdlist.cpp
unsigned int ifdListInit();
ifdNode_t* ifdListNodeAlloc(unsigned int ifdType);
unsigned int ifdListNodeInsert(unsigned int ifdType, ifdNode_t *pnode, void *pdata);
unsigned int ifdListNodeModify(unsigned short ifdType, unsigned short tagId, void *pdata);
unsigned int ifdListNodeDelete(unsigned int ifdType, unsigned short tagId);
unsigned int ifdListNodeInfoGet(unsigned short ifdType, unsigned short tagId, ifdNode_t **pnode, unsigned int *pbufAddr);
zeroIFDList_t* ifdZeroListGet();
exifIFDList_t* ifdExifListGet();
gpsIFDList_t* ifdGpsListGet();
firstIFDList_t* ifdFirstListGet();
itopIFDList_t* ifdItopListGet();
ifdNode_t* idfListHeadNodeGet(unsigned int ifdType);
unsigned int ifdListHeadNodeSet(unsigned int ifdType, ifdNode_t *pheadNode);

// exif_ifdmisc.cpp
unsigned int ifdListSizeof();
unsigned char* ifdListValBufGet(unsigned int ifdType);
unsigned int ifdListValBufSizeof(unsigned int ifdType);
unsigned int ifdListNodeCntGet(unsigned int ifdType);

// exif_hdr.cpp
unsigned int exifAPP1Write(unsigned char *pdata, unsigned int *pretSize);
unsigned int exifSOIWrite(unsigned char *pdata, unsigned int *pretSize);
unsigned char* exifHdrTmplAddrGet();
void exifHdrTmplAddrSet(unsigned char *paddr);

// exif_misc.cpp
unsigned short swap16(unsigned short x);
unsigned short read16(void *psrc);
unsigned int read32(void *psrc);
void write16(void *pdst, unsigned short src);
void write32(void *pdst, unsigned int src);
unsigned int exifApp1Sizeof();
unsigned int exifIFDValueSizeof(unsigned short type, unsigned int count);
void exifErrPrint(unsigned char *pname, unsigned int err);

// exif_make.cpp
INT32 exifIsGpsOnFlag();

/*******************************************************************************
*
********************************************************************************/

#endif /* _EXIF_INTERNAL_H_ */



