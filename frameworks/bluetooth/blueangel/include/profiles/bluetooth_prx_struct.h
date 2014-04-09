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
 *   bluetooth_a2dp_struct.h
 *
 * Project:
 * --------
 *   Maui_Software
 *
 * Description:
 * ------------
 *   This file is defines SAP for MTK Bluetooth.
 *
 * Author:
 * -------
 *   Daylong
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: #1 $
 * $Modtime$
 * $Log$
  *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BLUETOOTH_PRX_STRUCT_H_
#define __BLUETOOTH_PRX_STRUCT_H_

#define PRX_TRANSTYPE_CHECK    0x5b
#define BT_PSM_PRXM_1 0x1103 // 0x1005
#define BT_PSM_PRXR_1 BT_PSM_AVCTP //0x0119 // BT_PSM_AVCTP
#define BT_PSM_PRXR_2 0x1107 // local
#define PRX_RFCOMM_PORT1    5   //com port

/*---------------------------------------------------------------------------
 * Proximity Constant
 */
  
/* 
 * Max Support Report number
 */ 
#define PRX_MAX_REPORTER_NUM 3
#define PRX_MAX_MONITOR_NUM 3

/* LinkLoss Alert LEVEL */
#define LINKLOSS_NO_ALERT    0
#define LINKLOSS_MILD_ALERT  1
#define LINKLOSS_HIGH_ALERT  2

/* PathLoss Alert LEVEL */
#define PATHLOSS_NO_ALERT    0
#define PATHLOSS_MILD_ALERT  1
#define PATHLOSS_HIGH_ALERT  2

/* debug message */
#define PRX_DBG(s)            OS_Report(s)
#define PRX_DBG1(s, x)        OS_Report(s, x)
#define PRX_DBG2(s, x, y)     OS_Report(s, x, y)
#define PRX_DBG3(s, x, y, z)  OS_Report(s, x, y, z)

/* Event Ids for callback */
typedef U8 PRXREventType;
#define PRXRE_DISCONNECTED     0
#define PRXRE_INCOMING         1
#define PRXRE_CONNECTED        2
#define PRXRE_GETCAPABILITY    3
#define PRXRE_GETTXPOWER       4
#define PRXRE_SETPATHLOSS      5
#define PRXRE_SETLINKLOSS      6
#define PRXRE_GETRSSI          7
#define PRXRE_REGISTER         8
#define PRXRE_DEREGISTER       9

typedef U8 PrxMEventType;
#define PRXME_DISCONNECTED     0
#define PRXME_CONNECTED        1
#define PRXME_GETCAPABILITY    2
#define PRXME_GETTXPOWER       3
#define PRXME_SETPATHLOSS      4
#define PRXME_SETLINKLOSS      5
#define PRXME_GETRSSI          6
#define PRXME_CONNECTFAIL      7
#define PRXME_GETLINKLOSS      8


#define PRXASSERT(x)   if(x!= TRUE){ *((int*)0)=1;}

/*** Start ATT ENUM ***/
#define GATT_TYPE_S8   1

#define GATT_PRX_TXPOWER              1
#define GATT_PRX_IMMEDIATE_SERVICE    2
#define GATT_PRX_LINKLOSS_SERVICE     3

#define GATT_PRX_WRITE_TXPOWER              4
#define GATT_PRX_WRITE_IMMEDIATE_SERVICE    5
#define GATT_PRX_WRITE_LINKLOSS_SERVICE     6


#define GATT_CHARACTER_VALUE          9
/*** End ***/

#define PRX_INVALID_RSSI             -125
#define PRX_INVALID_TXPOWER          -125
#define PRX_INVALID_LINKLOSS         4


/* Event Structure for callback */

#define PRX_CHECK_ID_VALUE 0x05a


/*---------------------------------------------------------------------------
 * Proximity structure
 *
 * Used to describe the codec type and elements.
 */

/* MSG_ID_BT_PRXM_CONNECT_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   addr[6];  /* "0xaabbccddeeff" => addr[0] = 0xff */
} bt_prxm_connect_req_struct;

/* MSG_ID_BT_PRXM_CONNECT_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode; /* 0 success otherwise fail */
} bt_prxm_connect_cnf_struct;

/* MSG_ID_BT_PRXM_DISCONNECT_REQ  */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxm_disconnect_req_struct;

/* MSG_ID_BT_PRXM_DISCONNECT_IND */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode; /* 0 normal otherwise TBD */
} bt_prxm_disconnect_ind_struct;

/* MSG_ID_BT_PRXM_GET_CAPABILITY_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxm_get_capability_req_struct;

/* MSG_ID_BT_PRXM_GET_CAPABILITY_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
	U8   rspcode; /* 0 normal otherwise TBD */
    U32  capability;
} bt_prxm_get_capability_cnf_struct;

/* MSG_ID_BT_PRXM_GET_REMOTE_TXPOWER_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxm_get_remote_txpower_req_struct;

/* MSG_ID_BT_PRXM_GET_REMOTE_TXPOWER_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
	U8   rspcode;
    S8  txpower;
} bt_prxm_get_remote_txpower_cnf_struct;

/* MSG_ID_BT_PRXM_SET_PATHLOSS_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   level;
} bt_prxm_set_pathloss_req_struct;

/* MSG_ID_BT_PRXM_SET_PATHLOSS_CNF	 */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxm_set_pathloss_cnf_struct;

/* MSG_ID_BT_PRXM_SET_LINKLOSS_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   level;
} bt_prxm_set_linkloss_req_struct;

/* MSG_ID_BT_PRXM_SET_LINKLOSS_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxm_set_linkloss_cnf_struct;

/* MSG_ID_BT_PRXM_GET_RSSI_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxm_get_rssi_req_struct;

/* MSG_ID_BT_PRXM_GET_RSSI_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
	U8   rspcode;
    S8  rssi;
} bt_prxm_get_rssi_cnf_struct;

/* MSG_ID_BT_PRXM_GET_REMOTE_LINKLOSS_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxm_get_linkloss_req_struct;

/* MSG_ID_BT_PRXM_GET_REMOTE_LINKLOSS_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
	U8   rspcode;
	U8   level;
} bt_prxm_get_linkloss_cnf_struct;



/* MSG_ID_BT_PRXR_REGISTER_REQ  */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxr_register_req_struct;

/* MSG_ID_BT_PRXR_REGISTER_CNF  */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxr_register_cnf_struct;

/* MSG_ID_BT_PRXR_DEREGISTER_REQ  */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxr_deregister_req_struct;

/* MSG_ID_BT_PRXR_DEREGISTER_CNF  */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxr_deregister_cnf_struct;

/* MSG_ID_BT_PRXR_AUTHORIZE_IND */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   addr[6];
} bt_prxr_authorize_ind_struct;

/* MSG_ID_BT_PRXR_AUTHORIZE_RSP */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxr_authorize_rsp_struct;

/* MSG_ID_BT_PRXR_CONNECT_IND */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   addr[6];
} bt_prxr_connect_ind_struct;

#if 0
/* MSG_ID_BT_PRXR_CONNECT_RSP */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxr_connect_rsp_struct;
#endif

/* MSG_ID_BT_PRXR_DISCONNECT_REQ  */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxr_disconnect_req_struct;

/* MSG_ID_BT_PRXR_DISCONNECT_IND */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   rspcode;
} bt_prxr_disconnect_ind_struct;

/* MSG_ID_BT_PRXR_PATHLOSS_IND */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
	U8   level;    
} bt_prxr_pathloss_ind_struct;

/* MSG_ID_BT_PRXR_LINKLOSS_IND */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    U8   level;
} bt_prxr_linkloss_ind_struct;

/* MSG_ID_BT_PRXR_UPDATE_TXPOWER_REQ */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
} bt_prxr_update_txpower_req_struct;

/* MSG_ID_BT_PRXR_UPDATE_TXPOWER_CNF */
typedef struct
{
	LOCAL_PARA_HDR
    U8   index;
    S8  txpower;
} bt_prxr_update_txpower_cnf_struct;

#endif // __BLUETOOTH_PRX_STRUCT_H_
