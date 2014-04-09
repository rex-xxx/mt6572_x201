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
 * bt_ftpc_hdl.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   Handle msg of FTP Client
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
#ifndef __BT_FTPS_STRUCT_H__
#define __BT_FTPS_STRUCT_H__
#include "ext_nbuffer.h"
#include "bluetooth_ftps_struct.h"

#define BTMTK_FTPS_FILTER_MAX_NUBMER 10

typedef enum {
    BT_FTPSUI_EVENT_NONE,
    BT_FTPSUI_SHUTDOWNING, /* deinit Param: none */
    BT_FTPSUI_READY, /* server register ok Param: none */
    BT_FTPSUI_AUTHORIZING, /* server receive a incoming */
    BT_FTPSUI_AUTHEN_WAIT,  /* Param: none. server receive a client-challenge */
    BT_FTPSUI_CONNECTING,
    BT_FTPSUI_CONNECTED,                 /* Param: rspcode */
    BT_FTPSUI_SET_FOLDER_START,      /* Param: none */
    BT_FTPSUI_SET_FOLDERED,             /* Param: rspcode */
    BT_FTPSUI_ACTION_START,      /* Param: none */
    BT_FTPSUI_ACTIONED,             /* Param: rspcode */    
    BT_FTPSUI_BROWSE_START,            /* Param: none */
    BT_FTPSUI_BROWSING, 
    BT_FTPSUI_BROWSED,                     /* Param: rspcode */
    BT_FTPSUI_PUSH_FILE_START,         /* Param: none */
    BT_FTPSUI_PUSHING,                      /* Param: Percentage */
    BT_FTPSUI_PUSHED,                       /* Param: rspcode */
    BT_FTPSUI_PULL_FILE_START,          /* Param: none */
    BT_FTPSUI_PULLING,                        /* Param: Percentage */
    BT_FTPSUI_PULLED,                         /* Param: rspcode */
    BT_FTPSUI_FILE_DELETE,
    BT_FTPSUI_FOLDER_DELETE,
    BT_FTPSUI_FILE_CREATE,
    BT_FTPSUI_FOLDER_CREAT_START,
    BT_FTPSUI_ABORTED,
    BT_FTPSUI_DISCONNECTED,
    BT_FTPSUI_ERROR, 
} BT_FTPSUI_EVNT_T;


typedef struct
{
    U32 size;
    btbm_bd_addr_t bdaddr;
    U32 ftps_state;
    U8 ftps_filepath[BTMTK_GOEP_MAX_PATH_LEN *2] ;// FMGR_MAX_PATH_LEN * 2];
    U8 new_folder[BTMTK_GOEP_MAX_PATH_LEN *2]; // new folder name
    U8 obj_name[BTMTK_GOEP_MAX_PATH_LEN *2];
    U32 totalsize;
    U32 remain_len;
    U8  realm[GOEP_MAX_REALM_SIZE];
    U8  challeng_options; 
}bt_ftps_status_struct;


typedef struct
{
	U8	ucAdd;								// TRUE:Add, FALSE:remove form xml
	U8	ucFolder;							// TRUE:Folder, FALSE:File
	U8 	ucObjName[BTMTK_GOEP_MAX_PATH_LEN]; //obje name
	U8  ucMark;                             //Internal use by xml-generate function
}bt_ftps_filter_record;

typedef struct
{
	U32 u4NumFilter;
	bt_ftps_filter_record  records[BTMTK_FTPS_FILTER_MAX_NUBMER];
}bt_ftps_folder_filter;

/* for saving folder content */
#define FTPS_MAKE_FOLDER_CONTENT(x, i)   ext_ucs2ncpy((S8*)x,  (const S8*)L"d:\\temp\\ftp\\ftpxml.txt", 40);
   //kal_wsprintf( (U16 *)x, "Z:\\@ftp\\folder_content_%d", i);


/* for saving parsed folder content */
#define FTPC_MAKE_FOLDER_PARSED(x, i) ext_ucs2ncpy((U8*)x, (const U8*)L"d:\\temp\\ftp\\fc.tmp_X", 40);             
   //kal_wsprintf( (U16 *)x, "Z:\\@ftp\\fc.tmp_%d", i);

/* for saving the long name when parsing */
#define FTPC_MAKE_FOLDER_PARSED_LONG_NAME(x, i) ext_ucs2ncpy((U8*)x, L"d:\\temp\\ftp\\fc.tmp_lname_X", 40);
   //kal_wsprintf( (U16 *)x, "Z:\\@ftp\\fc.tmp_lname_%d", i);

void btmtk_ftps_ui_callback(U32 u4Event, U8 *ucData);
extern U8 g_ftps_root_folder[];

#endif
