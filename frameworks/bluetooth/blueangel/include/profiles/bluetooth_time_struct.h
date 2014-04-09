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
 *   bluetooth_time_struct.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   
 *
 * Author:
 * -------
 *   Jacob Lee
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
#ifndef __BT_TIME_STRUCT_H_
#define __BT_TIME_STRUCT_H_

/*---------------------------------------------------------------------------
 *  Time Constants
 */

#if 1

#define TIME_CHECK_BYTE		0x5a			// 01011010 as a byte-checking
#define BT_PSM_TIMEC_1		0x1203 			// 0x1005
#define BT_PSM_TIMES_1		BT_PSM_AVCTP	// 0x0119 // BT_PSM_AVCTP
#define BT_PSM_TIMES_2		0x1207
#define TIME_RFCOMM_PORT1	13				// Com port number

/* 
 * Max client (receiver) records kept by server
 */ 
#define TIME_MAX_CLIENT_NUM		3
#define TIME_MAX_SERVER_NUM		1

/* Event Ids for callback */
typedef U8 TimeSEventType;
#define TIMES_EVENT_DISCONNECTED			0
#define TIMES_EVENT_INCOMING				1
#define TIMES_EVENT_CONNECTED				2
#define TIMES_EVENT_GET_CTTIME				3
#define TIMES_EVENT_GET_CTTIME_NOTIFY		4
#define TIMES_EVENT_SET_CTTIME_NOTIFY		5
#define TIMES_EVENT_GET_LOCALTIME_INFO		6
#define TIMES_EVENT_GET_REFTIME_INFO		7
#define TIMES_EVENT_GET_DST					8
#define TIMES_EVENT_REQUEST_UPDATE			9
#define TIMES_EVENT_CANCEL_UPDATE			10
#define TIMES_EVENT_GET_UPDATE_STATE		11
#define TIMES_EVENT_UPDATE_CTTIME		12
#define TIMES_EVENT_REGISTER				13
#define TIMES_EVENT_DEREGISTER				14

typedef U8 TimeCEventType;
#define TIMEC_EVENT_DISCONNECTED			0
#define TIMEC_EVENT_CONNECTED				1
#define TIMEC_EVENT_GET_CTTIME				2
#define TIMEC_EVENT_GET_CTTIME_NOTIFY		3
#define TIMEC_EVENT_SET_CTTIME_NOTIFY		4
#define TIMEC_EVENT_GET_LOCALTIME_INFO		5
#define TIMEC_EVENT_GET_REFTIME_INFO		6
#define TIMEC_EVENT_GET_DST					7
#define TIMEC_EVENT_REQUEST_UPDATE			8
#define TIMEC_EVENT_CANCEL_UPDATE			9
#define TIMEC_EVENT_GET_UPDATE_STATE		10
#define TIMEC_EVENT_CTTIME_UPDATED		11
#define TIMEC_EVENT_CONNECT_FAIL			12

#define TIMEASSERT(x)	if ((x) != TRUE) {*((int*)0)=1;}

/*** Start ATT ENUM ***/
#define GATT_TYPE_S8   1

#define GATT_TIME_CTTIME					1
#define GATT_TIME_CTTIME_NOTIFY				2
#define GATT_TIME_LOCAL_TIME_INFO			3
#define GATT_TIME_REF_TIME_INFO				4
#define GATT_TIME_DST_INFO					5
#define GATT_TIME_SERVER_UPDATE_CTRL		6
#define GATT_TIME_SERVER_UPDATE_STATE		7

/*** End ***/

/* Fraction type */
#define TIME_FRACTION_100			0
#define TIME_FRACTION_256			1

/* Adjust Reasons */
#define TIME_AR_DONT_CARE			0x0
#define TIME_AR_MANUAL				0x1
#define TIME_AR_REF_TIME_UPDATED	0X2
#define TIME_AR_TIME_ZONE_CHANGED	0X4
#define TIME_AR_DST_CHANGED			0x8

/* Field index of temp file buffer */
#define TIME_INDEX_YEAR				0
#define TIME_INDEX_MONTH			1
#define TIME_INDEX_DAY_OF_MONTH		2
#define TIME_INDEX_HOURS			3
#define TIME_INDEX_MINUTES			4
#define TIME_INDEX_SECONDS			5
#define TIME_INDEX_DAY_OF_WEEK		6
#define TIME_INDEX_FRAC				7
#define TIME_INDEX_ADJUST_REASON	8
#define TIME_INDEX_RESERVED			9

/* Event Structure for callback */

// #define PRX_CHECK_ID_VALUE 0x05a


/*---------------------------------------------------------------------------
 * Time structures
 *
 * Definition of structures used in Time profile
 */

/**********************************
 * Time Client Message Structures
 **********************************/
/* MSG_ID_BT_TIMEC_CONNECT_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 addr[6];	// "0xaabbccddeeff" => addr[0] is 0xff
} bt_timec_connect_req_struct;

/* MSG_ID_BT_TIMEC_CONNECT_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;	// 0 for success, otherwise fail
} bt_timec_connect_cnf_struct;

/* MSG_ID_BT_TIMEC_DISCONNECT_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_disconnect_req_struct;

/* MSG_ID_BT_TIMEC_DISCONNECT_IND */
typedef struct {
	LOCAL_PARA_HDR
    U8 index;
    U8 rspcode;	// 0 normal otherwise TBD
} bt_timec_disconnect_ind_struct;

/* MSG_ID_BT_TIMEC_GET_CTTIME_REQ */
typedef struct {
	LOCAL_PARA_HDR
    U8 index;
} bt_timec_get_cttime_req_struct;

/* MSG_ID_BT_TIMEC_GET_CTTIME_CNF */
typedef struct {
	LOCAL_PARA_HDR
    U8 index;
	U8 rspcode;
    U16 year;
	U8 month;
	U8 day;
	U8 hours;
	U8 minutes;
	U8 seconds;
	U8 day_of_week;
	U8 frac256;
	U8 adjust_reason;
} bt_timec_get_cttime_cnf_struct;

/* MSG_ID_BT_TIMEC_GET_CTTIME_NOTIFY_REQ */
typedef struct {
	LOCAL_PARA_HDR
    U8 index;
} bt_timec_get_cttime_notify_req_struct;

/* MSG_ID_BT_TIMEC_GET_CTTIME_NOTIFY_CNF */
typedef struct {
	LOCAL_PARA_HDR
    U8 index;
	U8 rspcode;
	U16 notify_config;
} bt_timec_get_cttime_notify_cnf_struct;

/* MSG_ID_BT_TIMEC_SET_CTTIME_NOTIFY_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U16 notify_config;
} bt_timec_set_cttime_notify_req_struct;

/* MSG_ID_BT_TIMEC_SET_CTTIME_NOTIFY_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_timec_set_cttime_notify_cnf_struct;

/* MSG_ID_BT_TIMEC_UPDATE_CTTIME_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U16 year;
	U8 month;
	U8 day;
	U8 hours;
	U8 minutes;
	U8 seconds;
	U8 day_of_week;
	U8 frac256;
	U8 adjust_reason;
} bt_timec_update_cttime_ind_struct;

/* MSG_ID_BT_TIMEC_UPDATE_CTTIME_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_timec_update_cttime_rsp_struct;

/* MSG_ID_BT_TIMEC_GET_LOCAL_TIME_INFO_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_get_local_time_info_req_struct;

/* MSG_ID_BT_TIMEC_GET_LOCAL_TIME_INFO_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U8 time_zone;
	U8 dst;		// Daylight Saving Time
} bt_timec_get_local_time_info_cnf_struct;

/* MSG_ID_BT_TIMEC_GET_REF_TIME_INFO_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_get_ref_time_info_req_struct;

/* MSG_ID_BT_TIMEC_GET_REF_TIME_INFO_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U8 time_source;
	U8 accuracy;
	U8 days_since_update;
	U8 hours_since_update;
} bt_timec_get_ref_time_info_cnf_struct;

/* MSG_ID_BT_TIMEC_GET_DST_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_get_dst_req_struct;

/* MSG_ID_BT_TIMEC_GET_DST_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U16 year;
	U8 month;
	U8 day;
	U8 hours;
	U8 minutes;
	U8 seconds;
	U8 dst;
} bt_timec_get_dst_cnf_struct;

/* MSG_ID_BT_TIMEC_REQUEST_SERVER_UPDATE_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_request_server_update_req_struct;

/* MSG_ID_BT_TIMEC_REQUEST_SERVER_UPDATE_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_timec_request_server_update_cnf_struct;

/* MSG_ID_BT_TIMEC_CANCEL_SERVER_UPDATE_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_cancel_server_update_req_struct;

/* MSG_ID_BT_TIMEC_CANCEL_SERVER_UPDATE_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_timec_cancel_server_update_cnf_struct;

/* MSG_ID_BT_TIMEC_GET_SERVER_UPDATE_STATUS_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_timec_get_server_update_status_req_struct;

/* MSG_ID_BT_TIMEC_GET_SERVER_UPDATE_STATUS_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U8 cur_state;
	U8 result;
} bt_timec_get_server_update_status_cnf_struct;

/**********************************
 * Time Server Message Structures
 **********************************/
/* MSG_ID_BT_TIMES_REGISTER_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_register_req_struct;

/* MSG_ID_BT_TIMES_REGISTER_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_register_cnf_struct;

/* MSG_ID_BT_TIMES_DEREGISTER_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_deregister_req_struct;

/* MSG_ID_BT_TIMES_DEREGISTER_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_deregister_cnf_struct;

/* MSG_ID_BT_TIMES_AUTHORIZE_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 addr[6];
} bt_times_authorize_ind_struct;

/* MSG_ID_BT_TIMES_AUTHORIZE_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_authorize_rsp_struct;

/* MSG_ID_BT_TIMES_CONNECT_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 addr[6];
} bt_times_connect_ind_struct;

/* MSG_ID_BT_TIMES_DISCONNECT_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_disconnect_req_struct;

/* MSG_ID_BT_TIMES_DISCONNECT_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_disconnect_ind_struct;

/* MSG_ID_BT_TIMES_GET_CTTIME_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_get_cttime_ind_struct;

/* MSG_ID_BT_TIMES_GET_CTTIME_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U16 year;
	U8 month;
	U8 day;
	U8 hours;
	U8 minutes;
	U8 seconds;
	U8 day_of_week;
	U8 frac256;
	U8 adjust_reason;
} bt_times_get_cttime_rsp_struct;

/* MSG_ID_BT_TIMES_GET_CTTIME_NOTIFY_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_get_cttime_notify_ind_struct;

/* MSG_ID_BT_TIMES_GET_CTTIME_NOTIFY_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U8 notify_config;
} bt_times_get_cttime_notify_rsp_struct;

/* MSG_ID_BT_TIMES_SET_CTTIME_NOTIFY_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 notify_config;
} bt_times_set_cttime_notify_ind_struct;

/* MSG_ID_BT_TIMES_SET_CTTIME_NOTIFY_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_set_cttime_notify_rsp_struct;

/* MSG_ID_BT_TIMES_UPDATE_CTTIME_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U16 year;
	U8 month;
	U8 day;
	U8 hours;
	U8 minutes;
	U8 seconds;
	U8 day_of_week;
	U8 frac256;
	U8 adjust_reason;
} bt_times_update_cttime_req_struct;

/* MSG_ID_BT_TIMES_UPDATE_CTTIME_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_update_cttime_cnf_struct;

/* MSG_ID_BT_TIMES_SET_LOCAL_TIME_INFO_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 time_zone;
	U8 dst;
} bt_times_set_local_time_info_req_struct;

/* MSG_ID_BT_TIMES_SET_LOCAL_TIME_INFO_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_set_local_time_info_cnf_struct;

/* MSG_ID_BT_TIMES_SET_REF_TIME_INFO_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 time_source;
	U8 accuracy;
	U8 days_since_update;
	U8 hours_since_update;
} bt_times_set_ref_time_info_req_struct;

/* MSG_ID_BT_TIMES_SET_REF_TIME_INFO_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_set_ref_time_info_cnf_struct;

/* MSG_ID_BT_TIMES_SET_DST_REQ */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U16 year;
	U8 month;
	U8 day;
	U8 hours;
	U8 minutes;
	U8 seconds;
	U8 dst;
} bt_times_set_dst_req_struct;

/* MSG_ID_BT_TIMES_SET_DST_CNF */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_set_dst_cnf_struct;

/* MSG_ID_BT_TIMES_REQUEST_SERVER_UPDATE_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_request_server_update_ind_struct;

/* MSG_ID_BT_TIMES_REQUEST_SERVER_UPDATE_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_request_server_update_rsp_struct;

/* MSG_ID_BT_TIMES_CANCEL_SERVER_UPDATE_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_cancel_server_update_ind_struct;

/* MSG_ID_BT_TIMES_CANCEL_SERVER_UPDATE_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
} bt_times_cancel_server_update_rsp_struct;

/* MSG_ID_BT_TIMES_GET_SERVER_UPDATE_STATUS_IND */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
} bt_times_get_server_update_status_ind_struct;

/* MSG_ID_BT_TIMES_GET_SERVER_UPDATE_STATUS_RSP */
typedef struct {
	LOCAL_PARA_HDR
	U8 index;
	U8 rspcode;
	U8 cur_state;
	U8 result;
} bt_times_get_server_update_status_rsp_struct;


#endif // __BLUETOOTH_TIME_STRUCT_H_
#endif
