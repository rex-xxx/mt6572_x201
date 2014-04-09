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
 * bt_chn_struct.h
 *
 * Project:
 * --------
 *   Maui
 *
 * Description:
 * ------------
 *   struct of local parameter for hfg adp sap
 *
 * Author:
 * -------
 * Elvis Lin
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: #1 $
 * $Modtime: $
 * $Log: $
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BLUETOOTH_CHN_STRUCT_H__
#define __BLUETOOTH_CHN_STRUCT_H__

#include "bt_message.h"

/***************************************************************************** 
* Definations
*****************************************************************************/
#define HFG_CONNECT_GUARD_TIMER         20000 /* 20 sec */
#define HFG_SCO_CONNECT_GUARD_TIMER     5000000  /* 5 sec */

#define MAX_PHONE_NUMBER			64 /* does not include '\0' char */
#define MAX_PB_NUMBER_LEN			32 /* does not include '\0' char */
#define MAX_PB_TEXT                        	64 /* does not include '\0' char */
#define MAX_OPER_NAME                               36 /* does not include '\0' char */
#define MAX_FIND_TEXT                                 36 /* does not include '\0' char */
#define MAX_CGM_LEN				40 /* does not include '\0' char */
/* SMS */
#define MAX_LIST_STRING                        	64	/* does not include '\0' char */
#define MAX_ADDR_LEN	                        	32 	/* does not include '\0' char */
#define MAX_CHARSET_LEN				8 	/* does not include '\0' char */
#define MAX_SMS_TEXT_LEN				120	/* does not include '\0' char */
/***************************************************************************** 
* Typedef 
*****************************************************************************/
#if 0
#define MTK_BD_ADDR_SIZE    6

typedef struct _MTK_BD_ADDR
{
    U8 addr[MTK_BD_ADDR_SIZE];
} MTK_BD_ADDR;
#endif
/***************************************************************************** 
* Structure
*****************************************************************************/
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
}bt_chn_header_struct;

typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U16                   result;
}bt_chn_general_cnf_struct;

/* MSG_ID_BT_CHN_ACTIVATE_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                	req_context;
    U16				svc;
    U16				remote_svc;
} bt_chn_activate_req_struct;

/* MSG_ID_BT_CHN_ACTIVATE_CNF */
typedef bt_chn_general_cnf_struct bt_chn_activate_cnf_struct;

/* MSG_ID_BT_CHN_DEACTIVATE_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_chn_deactivate_req_struct;

/* MSG_ID_BT_CHN_DEACTIVATE_CNF */
typedef bt_chn_general_cnf_struct bt_chn_deactivate_cnf_struct;

/* MSG_ID_BT_CHN_CONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U8					  bt_addr[6];
} bt_chn_connect_req_struct;

/* MSG_ID_BT_CHN_CONNECT_CNF */
typedef bt_chn_general_cnf_struct bt_chn_connect_cnf_struct;

/* MSG_ID_BT_CHN_CONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    U8					bt_addr[6];
} bt_chn_connected_ind_struct;

/* MSG_ID_BT_CHN_CONNECT_REQ_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    U8					bt_addr[6];
} bt_chn_connect_req_ind_struct;

/* MSG_ID_BT_CHN_ACCEPT_CHANNEL_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               req_context;
} bt_chn_accept_channel_req_struct;

/* MSG_ID_BT_CHN_ACCEPT_CHANNEL_CNF */
typedef bt_chn_general_cnf_struct bt_chn_accept_channel_cnf_struct;

/* MSG_ID_BT_CHN_REJECT_CHANNEL_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_chn_reject_channel_req_struct;

/* MSG_ID_BT_CHN_REJECT_CHANNEL_CNF */
typedef bt_chn_general_cnf_struct bt_chn_reject_channel_cnf_struct;

/* MSG_ID_BT_CHN_DISCONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_chn_disconnect_req_struct;

/* MSG_ID_BT_CHN_DISCONNECT_CNF */
typedef bt_chn_general_cnf_struct bt_chn_disconnect_cnf_struct;

/* MSG_ID_BT_CHN_DISCONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    U8					bt_addr[6];
} bt_chn_disconnected_ind_struct;

/* MSG_ID_BT_CHN_SCO_CONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_chn_sco_connect_req_struct;

/* MSG_ID_BT_CHN_SCO_CONNECT_CNF */
typedef bt_chn_general_cnf_struct bt_chn_sco_connect_cnf_struct;

/* MSG_ID_BT_CHN_SCO_CONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    U16		      status;
} bt_chn_sco_connected_ind_struct;

/* MSG_ID_BT_CHN_SCO_DISCONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_chn_sco_disconnect_req_struct;

/* MSG_ID_BT_CHN_SCO_DISCONNECT_CNF */
typedef bt_chn_general_cnf_struct bt_chn_sco_disconnect_cnf_struct;

/* MSG_ID_BT_CHN_SCO_DISCONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
} bt_chn_sco_disconnected_ind_struct;

/* MSG_ID_BT_CHN_SEND_DATA_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U16			 size;
    U8			 data[200];
} bt_chn_send_data_req_struct;

/* MSG_ID_BT_CHN_SEND_DATA_CNF */
typedef bt_chn_general_cnf_struct bt_chn_send_data_cnf_struct;

/* MSG_ID_BT_CHN_RX_DATA_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*               	 user_context;
    U16			 size;
    U8			 data[200];
} bt_chn_rx_data_ind_struct;

#endif//BT_CHN_STRUCT_H

