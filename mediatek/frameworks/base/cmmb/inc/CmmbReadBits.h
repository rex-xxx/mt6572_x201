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

#ifndef _CMMB_BITS_H
#define _CMMB_BITS_H

#include "CmmbSPCommon.h"

typedef UINT32 (*cmmb_bits_fill_t)(void* param, UINT8* buf, UINT32 bytes);

#define cmmb_checkbit(a)		(1 << (a))

typedef struct cmmb_bits_t
{
    UINT8* buf;     /**< buffer pointer */
    UINT32 buf_len; /**< buffer length */

    UINT8* read;    /**< pointer to the next read position */
    UINT8* end;     /**< pointer to the end of buffer */

    cmmb_bits_fill_t fill;
    void* param;        /**< opaque parameter for the callback */

    UINT32 cache;   /**< cached bits */
    UINT32 bits;    /**< number of bits in the cache */

} cmmb_bits_t;

extern void cmmb_bits_init(cmmb_bits_t* self, UINT8* buf, UINT32 bytes, cmmb_bits_fill_t fill, void* param);
extern void cmmb_bits_refill(cmmb_bits_t* self, UINT32 min_bytes);
extern void cmmb_bits_get_block(cmmb_bits_t* self, UINT8* buf, UINT32 bytes);
extern void cmmb_bits_discard(cmmb_bits_t* self, UINT32 bytes);
extern UINT32 cmmb_bits_show(cmmb_bits_t* self, UINT32 n);
extern UINT32 cmmb_bits_get(cmmb_bits_t* self, UINT32 n);
extern UINT32 cmmb_bits_get_byte(cmmb_bits_t* self);
extern UINT32 cmmb_bits_get_bytes(cmmb_bits_t* self, UINT32 n);
extern UINT8* cmmb_bits_head(cmmb_bits_t* self);

#endif /* _CMMB_BITS_H */



