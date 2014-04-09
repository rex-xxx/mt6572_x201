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
 * bluetooth_jsr82_struct.h
 *
 * Project:
 * --------
 *   Maui
 *
 * Description:
 * ------------
 *   struct of local parameter for SIMAP
 *
 * Author:
 * -------
 * Bingyi Chen
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
#ifndef __BLUETOOTH_JSR82_STRUCT_H__
#define __BLUETOOTH_JSR82_STRUCT_H__

#include "bt_mmi.h"
#include "bt_message.h"


#define JSR82_MAX_PSM_NO (10)
#define JSR82_MAX_RFCOMM_NO (10)
#define JSR82_MAX_SESSION_NO (JSR82_MAX_PSM_NO + JSR82_MAX_RFCOMM_NO)
/* JSR82_MAX_PSM_NO MUST < 32 */

#define JBT_MAX_SDPDB_NO JSR82_MAX_SESSION_NO
#define JBT_MAX_SPP_NO (JSR82_MAX_RFCOMM_NO)
#define JBT_MAX_L2CAP_NO (JSR82_MAX_PSM_NO)

#define JSR82_SESSION_PS_RFCOMM_MTU  (1000)
#define JSR82_SESSION_PS_L2CAP_MTU   (339)

/* Add by mtk01411: 2007-1103 */
#define JBT_MAX_SUBSESSION_NO (0x01)

#define JSR82_SESSION_PS_RFCOMM  (0x01)
#define JSR82_SESSION_PS_L2CAP   (0x02)

/* Service Registration, not SDP record registration */
#define JSR82_SESSION_REGISTRARION_SUCCESS  (0x01)
#define JSR82_SESSION_REGISTRARION_FAILED   (0x02)

#define BT_JSR82_SESSION_DISABLED_SUCCESS   (0x01)
#define BT_JSR82_SESSION_DISABLED_FAILED   (0x02)

#define JSR82_SESSION_TURNON_SUCCESS  (0x01)
#define JSR82_SESSION_TURNON_FAILED   (0x02)

#define JSR82_SESSION_TURNOFF_SUCCESS  (0x01)
#define JSR82_SESSION_TURNOFF_FAILED   (0x02)

#define JSR82_SESSION_CONNECT_IND_REQUEST   (0x01)
#define JSR82_SESSION_CONNECT_IND_CONNECTED   (0x02)

#define JSR82_SESSION_CONNECT_CLIENT_SUCCESS   (0x01)
#define JSR82_SESSION_CONNECT_CLIENT_FAILED      (0x02)
#define JSR82_SESSION_CONNECT_CLIENT_INVALID_PARMS      (0x03)

#define JBT_LIST_SEARCH_TYPE_BY_INX 0
#define JBT_LIST_SEARCH_TYPE_BY_TRANSACTION_ID 1
#define JBT_LIST_SEARCH_TYPE_BY_CHNL_NUM 2

/* Definitions for JBT session */
#define JBT_SESSION_TYPE_SPP 1
#define JBT_SESSION_TYPE_L2CAP 2

#define JBT_SESSION_RX_BUF_TYPE 1
#define JBT_SESSION_TX_BUF_TYPE 2

#define JBT_SESSION_FIND_NO_ENTRY -1
#define JBT_SESSION_INVALID_SESSION_TYPE -2
#define JBT_SESSION_INVALID_BUFF_TYPE -3
#define JBT_SESSION_ERR_STATE -4
#define JBT_SESSION_NOT_POWERON -5
#define JBT_SESSION_FAILED_ALLOCATE_RINGBUF -6

#define JSR82_PORT_NUM JSR82_MAX_SESSION_NO

#ifdef BTMTK_ON_LINUX
// JSR82 Share Memory usage type
#define JSR82_ASHM_TYPE_CHANNEL_BUFFER 1
#define JSR82_ASHM_TYPE_DELIVER_PUT 2
#define JSR82_ASHM_TYPE_DELIVER_GET 3

#define JSR82_ASHM_NAME_LENGTH 50
#endif


typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 bd_addr[6];
    kal_uint8 ps_type;
    kal_uint16 mtu;
    kal_uint16 channel;
    kal_uint32 identify;    /* Modify to uint32: 2007-0917 */
    kal_uint8 security_value;
#ifdef BTMTK_ON_LINUX
    char memName[JSR82_ASHM_NAME_LENGTH];
#else
    kal_uint8 *channel_context;
#endif
} bt_jsr82_connect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 bd_addr[6];
    kal_uint16 mtu;
    U16 in_mtu;
    kal_uint8 ps_type;
    kal_uint32 channel;
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint16 l2cap_id;
    kal_uint8 result;
} bt_jsr82_connect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 ps_type;
    kal_uint16 mtu;
    kal_uint32 identify;
    kal_uint8 security_value;
#ifdef BTMTK_ON_LINUX
    char memName[JSR82_ASHM_NAME_LENGTH];
#else
    kal_uint8 *channel_context;
#endif
    /* To support same channel number can be connected via different client devices on multiple MUX */
    kal_uint16 existing_psm_chnl_num;
} bt_jsr82_enable_service_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 result;
    kal_uint8 ps_type;
    kal_uint16 channel;
    kal_uint8 index;    /* When the service is registered, the session_inx must be returned */
    kal_uint32 identify;
} bt_jsr82_enable_service_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index;
    kal_uint8 ps_type;
    kal_uint32 identify;
} bt_jsr82_turnon_service_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index;
    kal_uint8 ps_type;
    kal_uint32 identify;
} bt_jsr82_turnoff_service_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 result;
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint8 ps_type;
} bt_jsr82_turnon_service_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 result;
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint8 ps_type;
} bt_jsr82_turnoff_service_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index; /* Modified from identify to index: For Read and Write Procedure, only session_inx is needed 2007-0917 */
    kal_uint32 identify;
} bt_jsr82_disable_service_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 result;
    kal_uint8 ps_type;
    kal_uint8 index;
    kal_uint32 identify;
} bt_jsr82_disable_service_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 bd_addr[6];
    kal_uint8 ps_type;
    kal_uint16 mtu;
    U16 in_mtu;
    kal_uint32 channel;
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint16 l2cap_id;
    kal_uint8 rsp_result;
} bt_jsr82_connect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 bd_addr[6];
    kal_uint8 ps_type;
    kal_uint16 mtu;
    kal_uint16 channel;
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint16 l2cap_id;
    kal_uint8 result;
} bt_jsr82_connect_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 ps_type;
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint16 l2cap_id;
} bt_jsr82_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 ps_type;
    kal_uint32 identify;
    kal_uint8 index;    /* session_inx */
    kal_uint16 l2cap_id;
} bt_jsr82_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index; /* Modified from identify to index: For Read and Write Procedure, only session_inx is needed 2007-0917 */
    kal_uint16 length;
    kal_uint16 l2cap_id;
    kal_uint8 *data;
} bt_jsr82_tx_data_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index; /* Modified from identify to index: For Read and Write Procedure, only session_inx is needed 2007-0917 */
    kal_uint16 l2cap_id;
    kal_uint8 result;
} bt_jsr82_tx_data_cfn_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index; /* index is con_id */
    kal_uint16 length;
    kal_uint16 l2cap_id;
    kal_uint8 *data;
} bt_jsr82_rx_data_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index; /* Modified as session_index for RX operation : 2007-0917 */
    kal_uint16 l2cap_id;
    U8 ps_type;
	kal_bool isTxEmpty;
	
} bt_jsr82_tx_ready_ind_struct;

/* Add MSG_ID_BT_JSR82_RX_READY_IND */
typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index; /* Modified as session_index for RX operation : 2007-0917 */
    kal_uint16 length;
    kal_uint16 l2cap_id;
    U8 ps_type;
} bt_jsr82_rx_ready_ind_struct;

/* Add MSG_ID_BT_JSR82_SPP_GET_DATA_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 index;
    kal_uint32 identify;
    kal_uint16 length;
    kal_uint16 l2cap_id;
} bt_jsr82_spp_get_data_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 index;
    kal_uint16 l2cap_id;
} bt_jsr82_rx_data_rsp_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 ps_type;
    kal_uint8 index;
    kal_uint16 l2cap_id;
    kal_uint16 psm_chnl_num;
} bt_jsr82_allocate_txrx_buf_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 result;
    kal_uint8 ps_type;
    kal_uint8 index;
    kal_uint16 l2cap_id;
    kal_uint16 psm_chnl_num;
} bt_jsr82_allocate_txrx_buf_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint32 transaction_id;
    kal_uint8 bd_addr[6];
    kal_uint8 security_mode;
} bt_jsr82_set_acl_security_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 bdAddr[6];
} bt_jsr82_set_acl_security_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
#ifndef BTMTK_ON_LINUX
    U8 *buffAddr;
#endif
    U16 length;
} bt_jsr82_put_bytes_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
    U16 length;
} bt_jsr82_put_bytes_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
    U8 *buffAddr;
    U16 length;
} bt_jsr82_get_bytes_req_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
    U16 length;
    U8 *buffAddr;
} bt_jsr82_get_bytes_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
    I16 length;
} bt_jsr82_get_available_data_length_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
    U16 length;
#ifndef BTMTK_ON_LINUX
    U8 *buffAddr;
#endif
} bt_jsr82_data_available_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR 
    U8 ps_type;
    U8 index;
    U16 l2cap_id;
    U16 length;
} bt_jsr82_data_available_ind_rsp_struct;

#ifdef __JSR82_SHARED_MEMORY__
/* MSG_ID_BT_JSR82_ASSIGN_BUFFER_REQ */
typedef struct
{
    LOCAL_PARA_HDR 
    U8 index;
    U16 l2cap_id;
#ifdef BTMTK_ON_LINUX
    char memNamePut[JSR82_ASHM_NAME_LENGTH];
    char memNameGet[JSR82_ASHM_NAME_LENGTH];
#else
    U8 *deliverBufPtr;
#endif
    U16 deliverBufSize;
} bt_jsr82_assign_buffer_req_struct;

/* MSG_ID_BT_JSR82_ASSIGN_BUFFER_CNF */
typedef struct
{
    LOCAL_PARA_HDR 
    U8 index;
    U16 l2cap_id;
} bt_jsr82_assign_buffer_cnf_struct;

#endif	/* __JSR82_SHARED_MEMORY__ */


#endif	// __BLUETOOTH_JSR82_STRUCT_H__


