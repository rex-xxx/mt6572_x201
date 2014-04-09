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

/*****************************************************************************
 *
 * Filename:
 * ---------
 * bt_goep_porting.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   Porting GOEP service
 *
 * Author:
 * -------
 * Daylong Chen
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BT_GOEP_PORTING_H__
#define __BT_GOEP_PORTING_H__

#if defined(BTMTK_ON_WIN32) || defined(BTMTK_ON_WISESDK)
#include "windows.h"
#include "stdio.h"
#elif defined(BTMTK_ON_WISE)
#include "stdio.h"
#endif

#if 1
#include "bt_mmi.h"        // external mmi header
#include "bt_types.h"      // basic variable type definition
#include "bt_message.h"    // msg_type type
#include "bt_mmi_msg.h"    // send msg function declear

#endif

#if defined(BTMTK_ON_WIN32) || defined(BTMTK_ON_WISESDK)
#define GOEP_MEMALLOC(n)			malloc(n)
#define GOEP_MEMFREE(p)			free(p)
#define GOEP_MEMSET(dest, c, n) 	       memset(dest, c, n)
#define GOEP_MEMCPY(dest, src, n)	memcpy(dest, src, n)
#define GOEP_MEMCMP(p1, n, p2, n2)	       memcmp(p1, p2, n)
#define GOEP_STRLEN(s)			       strlen(s)
#define GOEP_STRNCPY(dest, src, n)	strncpy(dest, src, n)
#define GOEP_SPRINTF				sprintf
#else
#define GOEP_MEMALLOC(n)			malloc(n)//get_ctrl_buffer(n)
#define GOEP_MEMFREE(p)			free(p)//free_ctrl_buffer(p)
#define GOEP_MEMSET(dest, c, n)	       memset(dest, c, n)
#define GOEP_MEMCPY(dest, src, n)	memcpy(dest, src, n)
#define GOEP_MEMCMP(p1, n, p2, n2)	       memcmp(p1, p2, n)
#define GOEP_STRLEN(s)			       strlen(s)
#define GOEP_STRNCPY(dest, src)		strcpy(dest, src)
#define GOEP_SPRINTF				sprintf
#endif

#if defined(_DEBUG) 
#define GOEP_ASSERT(x)                                   if( FALSE == (x) ){ *((int *)0) = 0; }
#else
#define GOEP_ASSERT(x)                                    
#endif

#if defined(BTMTK_ON_WIN32) || defined(BTMTK_ON_WISESDK)
#define GET_CTRL_BUFFER(x)                               get_ctrl_buffer(x)
#define FREE_CTRL_BUFFER(x)                             free_ctrl_buffer(x)
#define CONSTRUCT_LOCAL_PARAM(x, y)              construct_local_para(x, y)
#define FREE_LOCAL_PARA(x)                                   free_local_para(x)
#else
#define GET_CTRL_BUFFER(x)                              (U8 *)malloc(x)
#define FREE_CTRL_BUFFER(x)                             free(x)
#define CONSTRUCT_LOCAL_PARAM(x, y)                     (U8 *)malloc(x)
#define FREE_LOCAL_PARA(x)                              free(x)
#endif



extern void OS_Report(const char *format, ...);

#define OPP_API_TRANSFER(x,y,z)  { btmtk_goep_show_id(x, TRUE, 2);              \
	                               BTCMD_SendMessage(x, MOD_BT, y,z);           \
	                               if( NULL != y ){                  \
								        FREE_LOCAL_PARA((void*)y);   \
    								}                                           \
	                               } 
#define BT_BTH_OBEX_OBJECT_PUSH_SERVICE_UUID      (0x1105 )
#define BT_OBEX_DUP_FILENAME_MAX_NUM                    100

/// Basic utility
#define BTMTK_Report  OS_Report

#define BT_FTP_TRC_CLASS        BT_TRACE_G6_OBEX
#define BT_OPP_TRC_CLASS        BT_TRACE_G6_OBEX
#define GOEP_TRACE(s)			
#define GOEP_TRACE_ERR(s)		
#define GOEP_TRACE_WRN(s)		

#if 1
#define GOEP_TRACE_PATH(x, y, unicode_path)               \
{                                                            \
U8 asc_path[128];                              \
ext_chset_ucs2_to_utf8_string((U8 *)asc_path, 128-1, (const U8 *)unicode_path);  \
asc_path[128-1]=0;                             \
OS_Report("[GOEP] trace_path:(%s)", asc_path );\
}
#endif

extern U16 bt_goep_ntohs(U16 s);

/// Utility for debugging in console
extern void btmtk_goep_show_id( U32 u4EventID, BT_BOOL bSent, U8 user);
extern const U8* bt_goep_get_id_name(U32 u4MsgId);
extern void btmtk_goep_sleep_test(U32 interval);

#endif 
