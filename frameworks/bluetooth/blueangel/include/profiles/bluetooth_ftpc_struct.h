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
 * bluetooth_ftpc_struct.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   struct of local parameter for FTP Client
 *
 * Author:
 * -------
 * Daylong
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: #1 $
 * $Modtime: $
 * $Log: $
 *
 * 09 03 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integration Bluetooth solution.
 *
 * 09 01 2010 sh.lai
 * NULL
 * Integration change. into 1036OF
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/

#ifndef __BLUETOOTH_FTPC_STRUCT_H__
#define __BLUETOOTH_FTPC_STRUCT_H__
// from btmmiftpgprot.c

#define FMGR_MAX_FILE_LEN           (MAX_SUBMENU_CHARACTERS)    /* Same size as menuitem's length */
#define FMGR_MAX_PATH_LEN           (260)
#define FMGR_MAX_EXT_LEN            (5)
#define FMGR_MAX_INPUT_FILE_LEN     (FMGR_MAX_FILE_LEN - FMGR_MAX_EXT_LEN)
#define FMGR_SORT_SORTING_BUFFER_SIZE   2152
#define FMGR_SORT_WORKING_BUFFER_SIZE   3676
#define FMGR_MAX_FILE_DISPLAY           512
#define FMGR_MAX_HASH_TABLE_SIZE        32

#define FTP_MAX_OBJ_NAME_LEN            ((FMGR_MAX_PATH_LEN + 1) * 2)
#define FTP_MAX_OBJ_CREATED_DATE_LEN    (80)
#define FTP_MAX_OBJ_SIZE_LEN            (16)
#define FTP_MAX_FILE_WRITER_BUF_SIZE    (1024)
#define FTP_WRITER_BUF_THRESHOLD        (512)
#define FTP_MAX_SERVER_SUPPORT          (1)

#define FTPC_MEM_BUFF_SIZE		(24576)	    // 14336 -> 24576
#define FTP_MAX_PARSE_FILE_INFO_NUMBER  (100)
#define FTP_MAX_PARSE_THRESHOLD         (30)
#define FTPC_PROG_UPD_DUR               (2000)
#define FTPC_FOLDER_OBJ_PATH_SIZE       (128)    /* should > 2 * "Z:\\@ftp\\folder_object_%d" */

#define FTPC_ASHM_NAME "mtk.bt.profile.ftpc.ashm"

#define ENCODING_LENGTH 2

#define FTPC_MEMSET(x,y)  memset(x, 0, y)   // linux use memset

typedef enum
{
    BT_FTPC_STATE_IDLE,                /* 0 FTPC Initial State */
    BT_FTPC_STATE_CONNECTING,          /* 1 FTPC is connecting to FTPS */
    BT_FTPC_STATE_CONNECTED,           /* 2 FTPC has established the connection to FTPS */
    BT_FTPC_STATE_GETTING_FOLDER,      /* 3 FTPC is getting the folder content for browsing */
//    BT_FTPC_STATE_PARSING_FOLDER,          /* 4  FTPC is parsing the folder content */
    BT_FTPC_STATE_GETTING_OBJ,         /* 5 FTPC is getting obj from FTPS */
    BT_FTPC_STATE_ABORTING,            /* 6 FTPC is aborting the getting obj behavior */
    BT_FTPC_STATE_SETTING_FOLDER,      /* 7 FTPC is setting the current folder of connected FTPS */
    BT_FTPC_STATE_SETTING_BACK_FOLDER, /* 8 FTPC is setting the current folder of connected FTPS */
    BT_FTPC_STATE_SETTING_ROOT_FOLDER, /* 9 FTPC is setting the current folder of connected FTPS */
    BT_FTPC_STATE_CREATE_FOLDER,
    BT_FTPC_STATE_DEL_FOLDER,
    BT_FTPC_STATE_PUSHING_OBJ,
    BT_FTPC_STATE_DISCONNECTING,        /* 10 FTPC is disconnecting the connection */
    BT_FTPC_STATE_TPDISCONNECTING,            /* 11 */
    BT_FTPC_STATE_ACTION
} bt_ftpc_state_enum;

/* FTPC  get obj total length num */
typedef enum
{
    BT_FTPC_TOTAL_LEN_NO_FIRST_PKT,
    BT_FTPC_TOTAL_LEN_NO_ZERO,
    BT_FTPC_TOTAL_LEN_ZERO
} bt_ftpc_total_len_type;

typedef enum
{
    BT_FTP_OBJ_TYPE_NONE,
    BT_FTP_OBJ_TYPE_FILE,
    BT_FTP_OBJ_TYPE_FOLDER
} bt_ftp_obj_type_enum;

/* object entity structure is used to temp buffering the object received from FTPS */
typedef struct
{
    /* short name is only for display*/
    U16 name[101] ;//MAX_SUBMENU_CHARACTERS + 1];  

    /* the length of the actual name */
    U16 actual_name_len;

    /* Indicate the start addr in file parsed_folder_lname, 
      * when the file name is more than MAX_SUBMENU_SIZE */
    U32 offset;
    
    U16 created_date[FTP_MAX_OBJ_CREATED_DATE_LEN / 2];
    U16 size[FTP_MAX_OBJ_SIZE_LEN / 2];
    bt_ftp_obj_type_enum type;
} bt_ftp_obj_entity_struct;


#ifndef min
#define min(a,b)            (((a) < (b)) ? (a) : (b))
#endif 

//#define BT_FTP_ASSERT(x)

#define BT_FTPC_STATE_TRANS(i, x) (act_client_cntx_p+i)->ftpc_state = (x);
#define BT_FTPC_STATE_CHECK(i, x) ( (act_client_cntx_p+i)->ftpc_state == (x) )

#endif
