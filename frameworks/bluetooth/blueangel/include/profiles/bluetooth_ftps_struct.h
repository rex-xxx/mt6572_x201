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
 * bluetooth_ftps_struct.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   struct of local parameter for FTP Server
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
#ifndef __BLUETOOTH_FTPS_STRUCT_H__
#define __BLUETOOTH_FTPS_STRUCT_H__

#define FTPS_MEM_BUFF_SIZE               (24576)    // 14336 -> 24576

#define FTP_NOTY_STR_SIZE                (32)
#define FTP_MAX_CLIENT_SUPPORT           (1)
#define BTMTK_GOEP_MAX_PATH_LEN           (260)
#define FTPS_MAX_FILE_WRITER_BUF_SIZE    (1024)
#define BTMTK_GOEP_MAX_OBJ_NAME_LEN            ((BTMTK_GOEP_MAX_PATH_LEN + 1))     // MAUI: BTMTK_GOEP_MAX_OBJ_NAME_LEN

#define FTPS_ASHM_NAME "mtk.bt.profile.ftps.ashm"

/// EXPRT CONSTANT

typedef enum
{
    BT_FTPS_OBJ_TYPE_NONE,
    BT_FTPS_OBJ_TYPE_FILE,
    BT_FTPS_OBJ_TYPE_FOLDER
} bt_ftps_obj_type_enum;

typedef enum
{
    BT_FTP_AUTO_AUTHORIZATION =1, //No need UI to accept the authorization
    BT_FTP_AUTO_BROWSE = 2, 
    BT_FTP_AUTO_PUSH = 4,         // No need UI to accept the pushing
    BT_FTP_AUTO_PULL = 8,         // No need UI to accept the pullig, Return with 
    BT_FTP_AUTO_CREATE_FOLDER = 16, // No need UI to accept the create new 
    BT_FTP_AUTO_DELETE = 32, // No need UI to accept the delete file/folder action
    BT_FTP_AUTO_SETPATH = 64,
    BT_FTP_AUTO_ALL = 0xff,
} BT_FTPS_AUTO_BITMASK;

typedef enum
{
    BT_FTPS_STATE_IDLE,            /* FTPS Initial State */
    BT_FTPS_STATE_REGISTERING,     /* Register goep and sdp */
    BT_FTPS_STATE_ACTIVE,          /* goep and sdp is ready to service */
    BT_FTPS_STATE_AUTHORIZING,     /* RFCOMM connection confirm state */
    BT_FTPS_STATE_CONNECTED,       /* Connection established state */
    BT_FTPS_STATE_RECEIVE_WAIT,
    BT_FTPS_STATE_RECEIVING,       /* FTPS is receiving obj from FTPC */
    BT_FTPS_STATE_SEND_WAIT,
    BT_FTPS_STATE_SENDING,         /* FTPS is sending folder content or obj to FTPC */
    BT_FTPS_STATE_SETPATH_WAIT,
    BT_FTPS_STATE_SETPATHING,      /* FTPS is handling set-path */
    BT_FTPS_STATE_ACTION_WAIT,
    BT_FTPS_STATE_ACTION,
    BT_FTPS_STATE_DISCONNECTING,   /* FTPS is disconnecting the connection */
    BT_FTPS_STATE_DEACTIVATING     /* FTPS service is going to stop */
} bt_ftps_state_enum;

/* identify the access right option */
typedef enum
{
    BT_FTP_ACCESS_RIGHT_FULLY_CONTROL,
    BT_FTP_ACCESS_RIGHT_READ_ONLY
} bt_ftp_access_right_enum;


#define BT_FTP_ASSERT(x)   
typedef void (*BTMTK_FTPS_CALLBACK)(U32 u4OPPUIEvent, U8* ucdata);

#endif
