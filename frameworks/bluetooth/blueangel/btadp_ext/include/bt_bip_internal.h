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
 * bt_bip_internal.c
 *
 * Project:
 * --------
 *
 *
 * Description:
 * ------------
 *   Common internal data type defination.
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
#if defined __BT_BIPI_PROFILE__ || defined __BT_BIPR_PROFILES__

#if 0 /* External ADP API handler and BT message handler in differnt task */
#define __BT_BIP_CMD_AGENT__
#endif

/* LOCAL_PARA_HDR, one-byte alignment will be 3!!!
 * + handle 4 
 * + opcode 4
*/
#define BIP_CMD_HDR_SIZE (4 + 4 + 4)

#define BT_BIP_GET_CMD_SIZE(t) (sizeof(t) + BIP_CMD_HDR_SIZE)
//#define BT_BIP_GET_CMD_PTR(t, cmd) ((t##*)((char*)cmd + BIP_CMD_HDR_SIZE))
#define BT_BIP_GET_CMD_PTR(cmd) ((void*)((char*)cmd + BIP_CMD_HDR_SIZE))


/* assert macro */
#define BIP_PROCESS_DEAD_ASSERT(e)
#define BIP_ASSERT(s)

typedef enum
{
    BT_BIP_OP_OBEX_AUTH_REQ = BT_BIP_OP_MAX, /* since in some case, in a operation package */
    BT_BIP_OP_OBEX_AUTH_RSP /* there are maybe challenge and response */
} bt_bip_authentication_opcode_enum;

//////////////////////////////////////////////////////////////////////////////
// Initiator
/////////////////////////////////////////////////////////////////////////////

#define BIPI_SDP_UUID 0x111A

#define BIPI_MAX_INSTANCE   1

typedef enum
{
    BIPI_STATUS_IDLE,
    BIPI_STATUS_ACTIVATED,
    BIPI_STATUS_CONNECTING,
    /* be challenge response in case of connection */
    BIPI_STATUS_CN_BE_CHAL,
    BIPI_STATUS_CONNECTED,
    BIPI_STATUS_REQUESTING, /* invoke API */
    /* waiting for challenge response in case of requestion */
    BIPI_STATUS_REQ_AUTH,
    BIPI_STATUS_INDICATING,      /* continue ind */
    BIPI_STATUS_CONTINUE,   /* continue rsp */
    BIPI_STATUS_ABORTING,
    BIPI_STATUS_DISCONNECTING,
    BIPI_STATUS_DEACTIVATING /* deactiate in none-activated-status */
} bt_bipi_status_enum;

typedef struct
{
    U32 status;
    U32 opcode;

    U8 req_id;
    U32 connect_id;
    bt_bip_connect_req connect; /* use for obex authenctication */
    U16 file_name[BT_BIP_MAX_PATH_LEN]; /* save file name, used to delete file in abort */

    bt_bipi_active_info init; /* invoker information */
} bt_bipi_context;

bt_bipi_context* btmtk_bipi_alloc_handle(void);
U8 btmtk_bipi_verify_handle(HMTKBIPI hBipI);
void btmtk_bipi_free_handle(bt_bipi_context* hBipI);
void btmtk_bipi_status_trans(bt_bipi_context* context, U32 status);
void btmtk_bipi_status_trans(bt_bipi_context* cntx, U32 status);
void btmtk_bipi_notify_app(bt_bipi_context* contxt, bt_bipi_para* para, U8 force);

/////////////////////////////////////////////////////////////////////////////////////
// Responder
/////////////////////////////////////////////////////////////////////////////////////

#define BIPR_SDP_UUID 0x111B

#define BIPR_MAX_INSTANCE   1

#ifdef WIN32
#define BIPR_IMG_LIST_FILE_NAME L"D:\\BIP\\IMG_LIST.xml"
#elif defined BTMTK_ON_WISE
#define BIPR_IMG_LIST_FILE_NAME L"C:\\BIP\\IMG_LIST.xml"
#elif defined BTMTK_ON_LINUX
#define BIPR_IMG_LIST_FILE_NAME L"/data/@btmtk/profile/IMG_LIST.xml"
#endif

typedef enum
{
    BIPR_STATUS_IDLE, /* 0 */
    BIPR_STATUS_ACTIVATING, /* 1 */
    BIPR_STATUS_REG_DEACTIVATING, /* 2 deacting in case of SDB failure */
    BIPR_STATUS_REGISTERING, /*3*/
    BIPR_STATUS_DEREGISTERING,  /* 4 */
    BIPR_STATUS_DEACTIVATING,   /* 5 */
    BIPR_STATUS_LISTENING, /* 6 there is no data link in above status */
    BIPR_STATUS_AUTHORIZING, /* 7 */
    BIPR_STATUS_AUTHORIZED,
    BIPR_STATUS_ACCEPTING, /* waiting for connect indication */
    BIPR_STATUS_CN_BE_CHAL, /* be challenged */
    BIPR_STATUS_ACCEPTED, /* client connected */
    BIPR_STATUS_RESPONDING, /* waiting app responding */
    BIPR_STATUS_RSP_AUTH,
    BIPR_STATUS_CONTINUE,
    BIPR_STATUS_INDICATING, /* waiting continue cnf */
    BIPR_STATUS_ABORTING,
    BIPR_STATUS_DISCONNECTING,
    BIPR_STATUS_DEAC_DISCONNECTING /* deactive in case of connection exsit */
} bt_bipr_status_enum;

typedef struct
{
    U32 status;
    U32 opcode;
    U8 req_id;
    U32 connect_id;
    U16 file_name[BT_BIP_MAX_PATH_LEN]; /* save file name, used to delete file in abort */
    bt_bipr_active_info init;
} bt_bipr_context;

bt_bipr_context* btmtk_bipr_alloc_handle(void);
U8 btmtk_bipr_verify_handle(HMTKBIPR hBipR);
void btmtk_bipr_free_handle(bt_bipr_context* hBipR);
void btmtk_bipr_status_trans(bt_bipr_context* context, U32 status);
void btmtk_bipr_status_trans(bt_bipr_context* cntx, U32 status);
void btmtk_bipr_send_request(msg_type msg_id, void* para, U32 size);
void btmtk_bipr_send_deregister_req(bt_bipr_context* context);
void btmtk_bipr_notify_app(bt_bipr_context* contxt, bt_bipr_para* para, U8 force);

void* btmtk_bip_alloc_local_para(U32 size);



#endif /* __BT_BIPI_PROFILE__ */
