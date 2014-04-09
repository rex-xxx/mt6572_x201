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
 *   bluetooth_map_struct.h
 *
 * Project:
 * --------
 *   Maui_Software
 *
 * Description:
 * ------------
 *   This file contains structure definition for corresponding SAP for MTK Bluetooth MAP.
 *
 * Author:
 * -------
 *   Autumn Li
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime$
 * $Log$
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BLUETOOTH_MAP_STRUCT_H_
#define __BLUETOOTH_MAP_STRUCT_H_

#include "bluetooth_struct.h"

#define BT_MAP_MAX_SRV_NAME_LEN 256
#define BT_MAP_MAX_DEV_NAME_LEN 80
#define BT_MAP_MAX_FOLDER_NAME_LEN 256
#define BT_MAP_MAX_PATH_NAME_LEN 512
#define BT_MAP_MAX_TEMP_FILE_NAME_LEN 80
#define BT_MAP_MAX_DATE_STR_LEN 16
#define BT_MAP_MAX_DATETIME_STR_LEN 20
#define BT_MAP_MAX_CONTACT_STR_LEN 80
#define BT_MAP_MAX_HANDLE_STR_LEN 32

#ifdef BTMTK_ON_WISE
#define MAP_ADP_WORK_FOLDER    "Z:\\@btmtk\\profile"
#elif defined(BTMTK_ON_LINUX)
#define MAP_ADP_WORK_FOLDER "/data/@btmtk/profile"
#else
#define MAP_ADP_WORK_FOLDER    "\\Component\\MBT\\HWAdapt\\MTK\\LIB\\@btmtk\\profile"
#endif

#ifdef BTMTK_ON_LINUX
#define BT_MAPS_FOLDER_LIST_FILE MAP_ADP_WORK_FOLDER"/maps_fl%d.xml"
#define BT_MAPS_MESSAGE_LIST_FILE MAP_ADP_WORK_FOLDER"/maps_ml%d.xml"
#define BT_MAPS_EVENT_REPORT_FILE MAP_ADP_WORK_FOLDER"/maps_er%d.xml"
#define BT_MAPS_MESSAGE_GET_FILE MAP_ADP_WORK_FOLDER"/maps_gm%d.vcf"
#define BT_MAPS_MESSAGE_PUSH_FILE MAP_ADP_WORK_FOLDER"/maps_pm%d.vcf"

#define BT_MAPC_FOLDER_LIST_FILE MAP_ADP_WORK_FOLDER"/mapc_fl%d.xml"
#define BT_MAPC_MESSAGE_LIST_FILE MAP_ADP_WORK_FOLDER"/mapc_ml%d.xml"
#define BT_MAPC_EVENT_REPORT_FILE MAP_ADP_WORK_FOLDER"/mapc_er%d.xml"
#define BT_MAPC_MESSAGE_GET_FILE MAP_ADP_WORK_FOLDER"/mapc_gm%d.vcf"
#define BT_MAPC_MESSAGE_PUSH_FILE MAP_ADP_WORK_FOLDER"/mapc_pm%d.vcf"

#else

#define BT_MAPS_FOLDER_LIST_FILE MAP_ADP_WORK_FOLDER"\\maps_fl%d.xml"
#define BT_MAPS_MESSAGE_LIST_FILE MAP_ADP_WORK_FOLDER"\\maps_ml%d.xml"
#define BT_MAPS_EVENT_REPORT_FILE MAP_ADP_WORK_FOLDER"\\maps_er%d.xml"
#define BT_MAPS_MESSAGE_GET_FILE MAP_ADP_WORK_FOLDER"\\maps_gm%d.vcf"
#define BT_MAPS_MESSAGE_PUSH_FILE MAP_ADP_WORK_FOLDER"\\maps_pm%d.vcf"

#define BT_MAPC_FOLDER_LIST_FILE MAP_ADP_WORK_FOLDER"\\mapc_fl%d.xml"
#define BT_MAPC_MESSAGE_LIST_FILE MAP_ADP_WORK_FOLDER"\\mapc_ml%d.xml"
#define BT_MAPC_EVENT_REPORT_FILE MAP_ADP_WORK_FOLDER"\\mapc_er%d.xml"
#define BT_MAPC_MESSAGE_GET_FILE MAP_ADP_WORK_FOLDER"\\mapc_gm%d.vcf"
#define BT_MAPC_MESSAGE_PUSH_FILE MAP_ADP_WORK_FOLDER"\\mapc_pm%d.vcf"
#endif

typedef enum
{
    BT_MAP_MSG_NONE = 0x0,
    BT_MAP_MSG_SMS_GSM = 0x1,
    BT_MAP_MSG_SMS_CDMA = 0x2,
    BT_MAP_MSG_EMAIL = 0x4,
    BT_MAP_MSG_MMS = 0x8
} bt_map_msg_enum;

typedef enum
{
    BT_MAP_SUCCESS,
    BT_MAP_FAIL,
    BT_MAP_FAIL_BUSY,
    BT_MAP_FAIL_NOT_FOUND,
    BT_MAP_FAIL_NOT_SUPPORT,
    BT_MAP_FAIL_FORBIDDEN,
    BT_MAP_FAIL_TIMEOUT,
    BT_MAP_FAIL_NO_RESOURCE,
    BT_MAP_FAIL_UNAUTHORIZED,
    BT_MAP_FAIL_BAD_FORMAT,
    BT_MAP_FAIL_INVALID_PARAMETER,
    BT_MAP_FAIL_STORAGE_FULL
} bt_map_result_enum;

typedef enum
{
    BT_MAP_FOLDER_OP_ROOT,  /* set to root folder */
    BT_MAP_FOLDER_OP_NEXT,  /* set to peer folder ex. cd ../xxx */
    BT_MAP_FOLDER_OP_DOWN,  /* set to child folder */
    BT_MAP_FOLDER_OP_UP     /* set to parent folder */
} bt_map_folder_op_enum;

typedef enum
{
    BT_MAP_MSGLIST_MASK_NONE        = 0x0000,
    BT_MAP_MSGLIST_MASK_SUBJECT     = 0x0001,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_DATETIME    = 0x0002,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_SENDER_N    = 0x0004,
    BT_MAP_MSGLIST_MASK_SENDER_ADDR = 0x0008,
    BT_MAP_MSGLIST_MASK_REC_N       = 0x0010,
    BT_MAP_MSGLIST_MASK_REC_ADDR    = 0x0020,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_TYPE        = 0x0040,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_SIZE        = 0x0080,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_REC_STATUS  = 0x0100,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_TEXT        = 0x0200,  /* default: no */
    BT_MAP_MSGLIST_MASK_ATTACH_SIZE = 0x0400,  /* REQUIRED */
    BT_MAP_MSGLIST_MASK_PRIO        = 0x0800,  /* default: no */
    BT_MAP_MSGLIST_MASK_READ        = 0x1000,  /* default: no */
    BT_MAP_MSGLIST_MASK_SENT        = 0x2000,  /* default: no */
    BT_MAP_MSGLIST_MASK_DRM         = 0x4000,  /* default: no */
    BT_MAP_MSGLIST_MASK_REPLY_ADDR  = 0x8000,
    BT_MAP_MSGLIST_MASK_ALL         = 0xFFFF
} bt_map_msg_list_mask_enum;

typedef enum
{
    BT_MAP_FILTER_MSG_SMS_GSM = 0x1,
    BT_MAP_FILTER_MSG_SMS_CDMA = 0x2,
    BT_MAP_FILTER_MSG_EMAIL = 0x4,
    BT_MAP_FILTER_MSG_MMS = 0x8
} bt_map_filter_msg_enum;

typedef enum
{
    BT_MAP_FILTER_STATUS_ALL = 0,
    BT_MAP_FILTER_STATUS_UNREAD = 0x1,
    BT_MAP_FILTER_STATUS_READ = 0x2
} bt_map_filter_status_enum;

typedef enum
{
    BT_MAP_FILTER_PRIO_ALL = 0,
    BT_MAP_FILTER_PRIO_HIGH = 0x1,
    BT_MAP_FILTER_PRIO_NOT_HIGH = 0x2
} bt_map_filter_prio_enum;

typedef enum
{
    BT_MAP_CHARSET_NATIVE = 0,
    BT_MAP_CHARSET_UTF8 = 0x1
} bt_map_charset_enum;

typedef enum
{
    BT_MAP_FRACTION_REQ_FIRST = 0,
    BT_MAP_FRACTION_REQ_NEXT = 0x1,
    BT_MAP_FRACTION_REQ_NO
} bt_map_fraction_req_enum;

typedef enum
{
    BT_MAP_FRACTION_RSP_MORE = 0,
    BT_MAP_FRACTION_RSP_LAST = 0x1,
    BT_MAP_FRACTION_RSP_NO
} bt_map_fraction_rsp_enum;

typedef enum
{
    BT_MAP_MSG_STATUS_READ,
    BT_MAP_MSG_STATUS_UNREAD,
    BT_MAP_MSG_STATUS_DELETE,
    BT_MAP_MSG_STATUS_UNDELETE
} bt_map_msg_status_enum;

typedef enum
{
    BT_MAP_SAVE_AND_SENT,
    BT_MAP_SENT
} bt_map_sent_op_enum;

typedef bt_addr_struct bt_map_addr_struct;

typedef struct
{
    LOCAL_PARA_HDR
    bt_map_result_enum result;
} bt_maps_activate_cnf_struct;

typedef bt_maps_activate_cnf_struct bt_maps_deactivate_cnf_struct;
typedef bt_maps_activate_cnf_struct bt_mapc_activate_cnf_struct;
typedef bt_maps_activate_cnf_struct bt_mapc_deactivate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    kal_uint8 srv_name[BT_MAP_MAX_SRV_NAME_LEN + 1];  /* UTF8 zero-terminated string */
    kal_uint16 srv_name_len;
    bt_map_msg_enum msg_type;
} bt_maps_register_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_maps_register_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_maps_deregister_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_maps_deregister_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
    kal_uint8 dev_name[BT_MAP_MAX_DEV_NAME_LEN + 1];  /* UTF8 zero-terminated string */
} bt_maps_authorize_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_bool accept;  /* TRUE: accept, FALSE: reject */
    bt_map_addr_struct addr;
} bt_maps_authorize_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint32 conn_id;
    bt_map_addr_struct addr;
    kal_uint8 dev_name[BT_MAP_MAX_DEV_NAME_LEN + 1];  /* UTF8 zero-terminated string */
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_maps_connect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_bool accept;  /* TRUE: accept, FALSE: reject */
    kal_uint8 mas_id;  /* Range 0~255 */
    kal_uint32 conn_id;
    bt_map_addr_struct addr;
} bt_maps_connect_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;
    bt_map_addr_struct addr;
} bt_maps_abort_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_abort_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;
    bt_map_addr_struct addr;
} bt_maps_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_disconnect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint8 folder[BT_MAP_MAX_FOLDER_NAME_LEN + 1];  /* UTF8 zero-terminated string. Only used when NEXT and DOWN */
    bt_map_folder_op_enum flag;
} bt_maps_set_folder_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_set_folder_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint16 list_size;
    kal_uint16 list_offset;
} bt_maps_get_folder_listing_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint16 list_size;
    kal_uint16 data_size;
    kal_uint8 file[BT_MAP_MAX_TEMP_FILE_NAME_LEN + 1];  /* UTF8 zero-terminated string "fl[0~7].xml" */
} bt_maps_get_folder_listing_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint8 child_folder[BT_MAP_MAX_FOLDER_NAME_LEN + 1];  /* UTF8 zero-terminated string. Empty if current folder */
    kal_uint16 list_size;
    kal_uint16 list_offset;
    kal_uint8 max_subject_len;  /* Range 1~255 */
    bt_map_msg_list_mask_enum mask;
    bt_map_filter_msg_enum filter_msg;
    kal_uint8 filter_begin[BT_MAP_MAX_DATE_STR_LEN + 1];  /* UTF8 zero-terminated string */
    kal_uint8 filter_end[BT_MAP_MAX_DATE_STR_LEN + 1];  /* UTF8 zero-terminated string */
    bt_map_filter_status_enum filter_status;
    kal_uint8 filter_rec[BT_MAP_MAX_CONTACT_STR_LEN];  /* UTF8 zero-terminated string */
    kal_uint8 filter_orig[BT_MAP_MAX_CONTACT_STR_LEN];  /* UTF8 zero-terminated string */
    bt_map_filter_prio_enum filter_prio;
} bt_maps_get_message_listing_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_bool unread;  /* TRUE: unread, FALSE: read */
    kal_uint8 datetime[BT_MAP_MAX_DATETIME_STR_LEN + 1];  /* UTF8 zero-terminated string */
    kal_uint16 list_size;
    kal_uint16 data_size;
    kal_uint8 file[BT_MAP_MAX_TEMP_FILE_NAME_LEN + 1];  /* zero-terminated string "ml[conn].xml" */
} bt_maps_get_message_listing_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint8 handle[BT_MAP_MAX_HANDLE_STR_LEN + 1];  /* UTF8 zero-terminated string */
    kal_bool attachment;  /* TRUE: has attachment, FALSE: no attachment */
    bt_map_charset_enum charset;
    bt_map_fraction_req_enum fraction_req;
} bt_maps_get_message_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    bt_map_fraction_rsp_enum fraction_rsp;
    kal_uint16 data_size;
    kal_uint8 file[BT_MAP_MAX_TEMP_FILE_NAME_LEN + 1];  /* UTF8 zero-terminated string "gm[conn].xml" */
} bt_maps_get_message_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_bool on;  /* TRUE: on, FALSE: off */
} bt_maps_set_notif_registration_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_set_notif_registration_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint8 handle[BT_MAP_MAX_HANDLE_STR_LEN + 1];  /* UTF8 zero-terminated string */
    bt_map_msg_status_enum status;
} bt_maps_set_message_status_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_set_message_status_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint8 child_folder[BT_MAP_MAX_FOLDER_NAME_LEN + 1];  /* UTF8 zero-terminated string. Empty if current folder */
    bt_map_sent_op_enum sent_op;
    kal_bool retry;  /* FALSE: no retry, TRUE: retry */
    bt_map_charset_enum charset;
    kal_uint8 file[BT_MAP_MAX_TEMP_FILE_NAME_LEN + 1];  /* UTF8 zero-terminated string "pm[conn].xml" */
    kal_uint16 data_size;
} bt_maps_push_message_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
    kal_uint8 handle[BT_MAP_MAX_HANDLE_STR_LEN + 1];  /* UTF8 zero-terminated string */
} bt_maps_push_message_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_update_inbox_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_update_inbox_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
} bt_maps_mns_connect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 dev_name[BT_MAP_MAX_DEV_NAME_LEN + 1];  /* UTF8 zero-terminated string */
} bt_maps_mns_connect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
} bt_maps_mns_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
} bt_maps_mns_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
} bt_maps_mns_disconnect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;

    kal_uint16 data_size;
    kal_uint8 file[BT_MAP_MAX_TEMP_FILE_NAME_LEN + 1];  /* UTF8 zero-terminated string "er[conn].xml" */
} bt_maps_mns_send_event_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    kal_uint8 mas_id;  /* Range 0~255 */
    bt_map_addr_struct addr;
} bt_maps_mns_send_event_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_connect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
    kal_uint8 dev_name[BT_MAP_MAX_DEV_NAME_LEN + 1];  /* UTF8 zero-terminated string */
} bt_mapc_connect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_disconnect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_get_folder_listing_size_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
    kal_uint16 list_size;
} bt_mapc_get_folder_listing_size_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
    kal_uint8 child_folder[BT_MAP_MAX_FOLDER_NAME_LEN + 1];  /* UTF8 zero-terminated string. Empty if current folder */
} bt_mapc_get_message_listing_size_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
    kal_uint16 list_size;
    kal_bool unread;
    kal_uint8 datetime[BT_MAP_MAX_DATETIME_STR_LEN + 1];  /* UTF8 zero-terminated string */
} bt_mapc_get_message_listing_size_cnf_struct;


typedef bt_maps_authorize_ind_struct bt_mapc_mns_authorize_ind_struct;
typedef bt_maps_authorize_rsp_struct bt_mapc_mns_authorize_rsp_struct;
typedef bt_maps_set_folder_ind_struct bt_mapc_set_folder_req_struct;
typedef bt_maps_set_folder_rsp_struct bt_mapc_set_folder_cnf_struct;
typedef bt_maps_get_folder_listing_ind_struct bt_mapc_get_folder_listing_req_struct;
typedef bt_maps_get_folder_listing_rsp_struct bt_mapc_get_folder_listing_cnf_struct;
typedef bt_maps_get_message_listing_ind_struct bt_mapc_get_message_listing_req_struct;
typedef bt_maps_get_message_listing_rsp_struct bt_mapc_get_message_listing_cnf_struct;
typedef bt_maps_get_message_ind_struct bt_mapc_get_message_req_struct;
typedef bt_maps_get_message_rsp_struct bt_mapc_get_message_cnf_struct;
typedef bt_maps_set_notif_registration_ind_struct bt_mapc_set_notif_registration_req_struct;
typedef bt_maps_set_notif_registration_rsp_struct bt_mapc_set_notif_registration_cnf_struct;
typedef bt_maps_set_message_status_ind_struct bt_mapc_set_message_status_req_struct;
typedef bt_maps_set_message_status_rsp_struct bt_mapc_set_message_status_cnf_struct;
typedef bt_maps_push_message_ind_struct bt_mapc_push_message_req_struct;
typedef bt_maps_push_message_rsp_struct bt_mapc_push_message_cnf_struct;
typedef bt_maps_update_inbox_ind_struct bt_mapc_update_inbox_req_struct;
typedef bt_maps_update_inbox_rsp_struct bt_mapc_update_inbox_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_abort_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    bt_map_result_enum result;
    bt_map_addr_struct addr;
    kal_uint8 mas_id;  /* Range 0~255 */
} bt_mapc_abort_cnf_struct;

typedef bt_maps_mns_disconnect_req_struct bt_mapc_mns_disconnect_req_struct;
typedef bt_maps_mns_disconnect_cnf_struct bt_mapc_mns_disconnect_cnf_struct;
typedef bt_maps_mns_send_event_req_struct bt_mapc_mns_send_event_ind_struct;
typedef bt_maps_mns_send_event_cnf_struct bt_mapc_mns_send_event_rsp_struct;

#endif /* __BLUETOOTH_MAP_STRUCT_H_ */ 

