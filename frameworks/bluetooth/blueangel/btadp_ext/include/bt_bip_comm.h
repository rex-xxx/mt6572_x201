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
 * bt_bip_comm.h
 *
 * Project:
 * --------
 *
 *
 * Description:
 * ------------
 *   BIP common defination
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
#ifndef __BT_BIP_COMM_H__
#define __BT_BIP_COMM_H__

#include "bt_types.h"
//mtk03036
#define CONSTRUCT_LOCAL_PARAM(x, y)		(U8 *)malloc(x)
#define FREE_LOCAL_PARA(x)				free(x)

#define BT_BIP_MAX_TIME_LEN 18
#define BT_BIP_MAX_PATH_LEN 260
#define BT_BIP_IMG_HANDLE_LEN 8
#define BT_BIP_MAX_IMAGE_NAME_LEN 256 /* unicode encoding */
#define BT_BIP_MAX_DEV_NAME_LEN 80

#define BT_BIP_MAX_PWD_LEN 16
#define BT_BIP_MAX_UID_LEN 20
#define BT_BIP_MAX_REALM_LEN 20

#define BT_BIP_MAX_IMAGE_FORMATES 10

#define BT_BIP_ERR_OK               0
#define BT_BIP_ERR_FAILED           -1
#define BT_BIP_ERR_OBAUTH_NEEDED    -2
#define BT_BIP_ERR_OBAUTH_FAILED    -3
#define BT_BIP_ERR_CONTINUE         -4
#define BT_BIP_ERR_INVALID_PARA     -5
#define BT_BIP_ERR_IO               -6
#define BT_BIP_ERR_PROCESSING       -7 /* operation is on processing */
#define BT_BIP_ERR_STATE            -8
#define BT_BIP_ERR_NO_RESOURCE      -9
#define BT_BIP_ERR_INDICATING       -10 /* indicating response needed */
#define BT_BIP_ERR_THUMBNAIL_NEEDED -11
#define BT_BIP_ERR_USER_ABORT       -12 /* user invoke abort api */
#define BT_BIP_ERR_PARTIAL_CONTENT  -13 /* partial content to indicate thumbnail is needed */
#define BT_BIP_ERR_SERVER_ABORT     -14 /* with DISCONNECT opcode, this value indicate server disconnect */
#define BT_BIP_ERR_CHALLENGE_OK  -15
#define BT_BIP_ERR_CHALLENGE_FAILED -16

typedef enum
{
	BT_BIP_SRV_IMG_PUSH = 0x01,
	BT_BIP_SRV_IMG_PULL = 0x02,
	BT_BIP_SRV_ADVANCED_PRINTING = 0x4,
	BT_BIP_SRV_REMOTE_CAMERA = 0x08,
	BT_BIP_SRV_AUTO_ARCHIVE = 0x10,
	BT_BIP_SRV_REMOTE_DISPLAY = 0x20
} bt_bip_srv_enum;

typedef enum
{
	BT_BIP_OP_NONE,
    BT_BIP_OP_ACTIVATE,
    BT_BIP_OP_DEACTIVATE,
	BT_BIP_OP_CONNECT,
	BT_BIP_OP_GET_CAPABILITIES,
	BT_BIP_OP_GET_IMG_LIST,
	BT_BIP_OP_GET_IMG_PROPERTIES,
	BT_BIP_OP_GET_IMG,
	BT_BIP_OP_GET_LINKED_THUMBNAIL,
	BT_BIP_OP_PUT_IMG,
	BT_BIP_OP_PUT_LINKED_THUMBNAIL,

	BT_BIP_OP_ABORT,
	BT_BIP_OP_DISCONNECT,

	BT_BIP_OP_AUTHORIZE,
    BT_BIP_OP_OBEX_AUTH,

    BT_BIP_OP_CONTINUE,
    BT_BIP_OP_MAX
} bt_bip_opcode_enum;

typedef enum
{
    BT_BIP_OP_STATUS_NONE = 0,
    BT_BIP_OP_STATUS_WAIT_CNF, /* responder only */
    BT_BIP_OP_STATUS_ON_PROCESSING,
    BT_BIP_OP_STATUS_COMPLETE
} bt_bip_op_status_enum;

typedef enum
{
	BT_BIP_TYPE_NONE = 0x00000000,
    BT_BIP_TYPE_JPEG = 0x00000001,
    BT_BIP_TYPE_BMP = 0x00000002,
    BT_BIP_TYPE_GIF = 0x00000004,
    BT_BIP_TYPE_WBMP = 0x00000008,
    BT_BIP_TYPE_PNG = 0x00000010
} bt_bip_img_type_enum;

typedef enum
{
    BT_BIP_IMG_TRANS_NONE,
    BT_BIP_IMG_TRANS_STRECH,
    BT_BIP_IMG_TRANS_CROP,
    BT_BIP_IMG_TRANS_FILL
} bt_bip_image_trans_enum;

typedef struct
{
	U16 width;
	U16 height;
	U16 width2;
	U16 height2;
} bt_bip_pixel;

typedef struct
{
	U8 created[BT_BIP_MAX_TIME_LEN];	/* (YYYYMMDDTHHMMSS)(Z) */
	U8 modified[BT_BIP_MAX_TIME_LEN];	/* (YYYYMMDDTHHMMSS)(Z) */
	U32 encoding;	/* bt_bip_img_type_enum */
	bt_bip_pixel pixel; /* only work when width&height are all none zero */
} bt_bip_handle_desc;

typedef struct
{
	U32 encoding;	/* REQUIRED, 0 means get native format image in GetImage */
	bt_bip_pixel pixel;	/* REQUIRED, only fixed sizes for PutImage */
	U32 size;
	U32 transformation; /* bt_bip_img_trans_enum */
} bt_bip_img_desc;

typedef struct
{
    U32 encoding;
    bt_bip_pixel pixel;
    U32 size;
} bt_bip_img_native_prop;

typedef bt_bip_img_native_prop bt_bip_img_format;

/*
typedef struct
{
    U8 img_handle[BT_BIP_IMG_HANDLE_LEN];
    U8 created[BT_BIP_MAX_TIME_LEN];
    U8 modified[BT_BIP_MAX_TIME_LEN];
} bt_bip_img_list_item;
*/
/* BT_BIP_ERR_INDICATING */
typedef struct
{
    U32 obj_len;
    U32 data_len;
} bt_bip_continue_ind;

/* BT_BIP_OP_DISCONNECT */
typedef struct
{
	U8 force;
} bt_bip_disconnect_req;

typedef struct
{
    U8 pwd[BT_BIP_MAX_PWD_LEN];
    U8 pwd_len;
    U8 uid[BT_BIP_MAX_UID_LEN];
    U8 uid_len;
    U8 realm[BT_BIP_MAX_REALM_LEN];
    U8 realm_len;
} bt_bip_obex_auth_req;

typedef struct
{
    S32 result;
    U8 pwd[BT_BIP_MAX_PWD_LEN];
    U8 pwd_len;
    U8 uid[BT_BIP_MAX_UID_LEN];
    U8 uid_len;
} bt_bip_obex_auth_rsp;

typedef struct
{
    U8 option;
    U8 realm_len;
    U8 realm[BT_BIP_MAX_REALM_LEN];
} bt_bip_obex_auth_ind;

typedef struct
{
    U32 lap;
    U8 uap;
    U16 nap;
} bt_bip_bd_addr;
#endif
