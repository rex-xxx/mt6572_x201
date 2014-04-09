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
* bt_bipr_struct.h
*
* Project:
* --------
*   
*
* Description:
* ------------
*   BIP Responder Exported structure
*
* Author:
* -------
* Zhigang Yu
*
*============================================================================
*             HISTORY
* Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
*------------------------------------------------------------------------------
* $Log: $
*------------------------------------------------------------------------------
* Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
*============================================================================
****************************************************************************/
#ifndef __BT_BIPR_STRUCT_H__
#define __BT_BIPR_STRUCT_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "bt_bip_comm.h"

typedef struct
{
    U16 year;     
    U16 month; 
    U16 day;     
    U16 hour;     
    U16 minute; 
    U16 second; 
} bt_bip_date;

typedef struct
{
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
    bt_bip_date created_date;
    bt_bip_date modified_date;
} bt_bip_img_list_item;

/* BT_BIP_OP_AUTHORIZE_IND */
typedef struct
{
    bt_bip_bd_addr addr;
    U8 dev_name[BT_BIP_MAX_DEV_NAME_LEN + 1];
} bt_bip_authorize_ind;

/* BT_BIP_OP_CONNECT */
typedef struct
{
    bt_bip_bd_addr addr;
    U8 dev_name[BT_BIP_MAX_DEV_NAME_LEN + 1];
} bt_bip_connect_ind;

/* BT_BIP_OP_GET_CAPABILITIES */
typedef struct
{
    S32 result;
    bt_bip_img_desc preferred_format;
    bt_bip_img_format supported_formats[BT_BIP_MAX_IMAGE_FORMATES];    
    U8 created_time_filter;
    U8 modified_time_filter;
    U8 encoding_filter;
    U8 pixel_filter;
} bt_bip_capabilities_rsp;

/* BT_BIP_OP_GET_IMG_LIST */
typedef struct
{
    bt_bip_handle_desc handle_desc;
    U32 max_img_number;
    U32 start_index;
    U8 latest_captured;
} bt_bip_get_img_list_ind;

typedef struct
{
    S32 result; /* BT_BIP_ERR_FAILED, BT_BIP_ERR_CONTINUE, BT_BIP_ERR_OK */
    bt_bip_img_list_item* item;
    U32 item_count;
} bt_bip_get_img_list_rsp;

/* BT_BIP_OP_GET_IMG_PROPERTIES
<!ELEMENT image-properties(native, variant*, attachment*)>
<!ATTLIST image-properties
version CDATA #FIXED "1.0"
handle CDATA #REQUIRED
friendly-name CDATA #IMPLIED>
<!ATTLIST native
encoding CDATA #REQUIRED
pixel CDATA #REQUIRED
size CDATA #IMPLIED>
*/

typedef struct
{
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
} bt_bip_get_img_prop_ind;

typedef struct
{
    S32 result;
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
    U16 friendly_name[BT_BIP_MAX_IMAGE_NAME_LEN]; /* unicode */
    bt_bip_img_native_prop native;
} bt_bip_get_img_prop_rsp;

/* BT_BIP_OP_GET_IMG */
typedef struct
{
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
    bt_bip_img_desc img_desc;
} bt_bip_get_img_ind;

typedef struct
{
    S32 result;
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
    U16 img_path[BT_BIP_MAX_PATH_LEN];
} bt_bip_get_img_rsp;

/* BT_BIP_OP_GET_LINKED_THUMBNAIL */
typedef struct
{
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
} bt_bip_get_linked_thum_ind;

typedef struct
{
    S32 result;
    U16 img_path[BT_BIP_MAX_PATH_LEN];
} bt_bip_get_linked_thum_rsp;

/* BT_BIP_OP_PUT_IMG */
typedef struct
{
    U16 img_name[BT_BIP_MAX_IMAGE_NAME_LEN];
    bt_bip_img_desc img_desc;
} bt_bip_put_img_ind;

typedef struct
{
    S32 result;
    U8 handle[BT_BIP_IMG_HANDLE_LEN];
    U16 img_path[BT_BIP_MAX_PATH_LEN];
} bt_bip_put_img_rsp;

/* BT_BIP_OP_PUT_LINKED_THUMBNAIL */
typedef struct
{
    U16 handle[BT_BIP_IMG_HANDLE_LEN];
} bt_bip_put_linked_thum_ind;

typedef struct
{
    S32 result;
    U16 img_path[BT_BIP_MAX_PATH_LEN];
} bt_bip_put_linked_thum_rsp;

/* BT_BIP_OP_ABORT */
/* BT_BIP_OP_DISCONNECT */


/* BT_BIP_OP_DISCONNECT_IND */

typedef union
{
    bt_bip_authorize_ind auth_ind;
    bt_bip_obex_auth_ind obauth_ind;
    bt_bip_connect_ind conn_ind;
    bt_bip_get_img_list_ind img_list;
    bt_bip_get_img_prop_ind img_prop;
    bt_bip_get_img_ind get_img;
    bt_bip_get_linked_thum_ind get_thum;
    bt_bip_put_img_ind put_img;
    bt_bip_put_linked_thum_ind put_thum;
    S32 result; /* status : BT_BIPR_OPS_RESPONDING */
    bt_bip_continue_ind cnt_ind; /* status : BT_BIPR_OPS_RESPONDING */
} bt_bipr_data;

typedef enum
{
    BT_BIPR_OPS_NONE,
    BT_BIPR_OPS_INDICATING,
    BT_BIPR_OPS_RESPONDING,
    BT_BIPR_OPS_COMPLETE,
    BT_BIPR_OPS_REQUESTING,
    BT_BIPR_OPS_CONFIRMING
} bt_bipr_op_status_enum;

typedef void* HMTKBIPR;

typedef struct
{
    HMTKBIPR handle;
    U32 opcode; /* bt_bip_opcode_enum */
    U32 status; /* bt_bip_op_status_enum */
    bt_bipr_data data;
} bt_bipr_para;

typedef void (*BTMTKBIPRCALLBACK)(void* para, bt_bipr_para* param);

typedef struct
{
	BTMTKBIPRCALLBACK callback;
    void* para;
    U32 service;
} bt_bipr_active_info;

#ifdef __cplusplus
}
#endif

#endif
