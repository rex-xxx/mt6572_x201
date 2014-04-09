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
*  permission of MediaTek Inc. (C) 2005
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
/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bt_types.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to
 *
 * Author:
 * -------
 * Dlight Ting
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime:
 * $Log: 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
#ifndef __BT_KAL_RELEASE_H
#define __BT_KAL_RELEASE_H

#include "bt_config.h"

#ifdef BTMTK_ON_WISE
#include "kal_release.h"    /* for kal types */
#include "app_ltlcom.h"     /* for ilm_struct */
#include "stack_config.h"   /* for module_type */
#endif /* BTMTK_ON_WISE*/

#ifndef BTMTK_ON_WISE
typedef unsigned long kal_uint32;
typedef unsigned short kal_uint16;
typedef unsigned char kal_uint8;

typedef unsigned char kal_bool;

typedef long kal_int32;
typedef short kal_int16;
typedef char kal_int8;
typedef char kal_char;

typedef unsigned short WCHAR;
#endif  /* BTMTK_ON_WISE */

//#ifndef BOOL
//#define BOOL unsigned char  /* IGNORESTYLE */
//#endif 

#ifndef U64
#define U64 unsigned long long
#endif

#ifndef U32
#define U32 kal_uint32
#endif

#ifndef U16
#define U16 kal_uint16
#endif

#ifndef U8
#define U8 kal_uint8
#endif


#ifndef S32
#define S32 signed int
#endif

#ifndef S16
#define S16 signed short
#endif

#ifndef S8
#define S8 char
#endif

#ifndef BT_BOOL
#define BT_BOOL char
#endif

/**
* String could be unicode or codepage.
* Codepage should only used in external platform's porting implement.
* ASCII should use U8 to present a character.(1B)
* Unicode has UCS2(Always 2 byte, Mapping of Unicode character planes),
*    UTF8(1,2,4B), UTF16(2,4B), UTF32 formats.
* Note: WCHAR primitive type has be defined in Linux(4B) - UTF32 format
* Note: WCHAR primitive type has be defined in WINDOS(2B) - UCS2 format
*/
/* UCS2 String in little endian */
#ifndef W16
#define W16 
#endif

/* UTF8, UTF16, ASCII use U8 to present it */


/*
 * Variable sized integers. Used to optimize processor efficiency by
 * using the most efficient data size for counters, arithmatic, etc.
 */
#ifndef I32
#define I32 unsigned long
#endif


#if XA_INTEGER_SIZE == 4
#ifndef I16
#define I16 unsigned long
#endif

#ifndef I8
#define I8 unsigned long
#endif


#elif XA_INTEGER_SIZE == 2

#ifndef I16
#define I16 unsigned short
#endif

#ifndef I8
#define I8 unsigned short
#endif


#elif XA_INTEGER_SIZE == 1
#ifndef I16
#define I16 unsigned short
#endif

#ifndef I8
#define I8 unsigned char
#endif

#else 
#error No XA_INTEGER_SIZE specified!
#endif 

#ifndef TRUE
#define TRUE  (1==1)
#endif 

#ifndef FALSE
#define FALSE (0==1)
#endif 


#ifndef BTMTK_ON_WISE
#define KAL_TRUE (1)
#define KAL_FALSE (0)
#endif  /* BTMTK_ON_WISE */

#ifndef NULL
#define NULL 0
#endif

#ifndef BTMTK_ON_WISE
#define UART_PORT kal_uint8
#endif  /* BTMTK_ON_WISE */

#endif /* BT_KAL_RELEASE */ 

