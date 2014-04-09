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
 * bt_hidd_struct.h
 *
 * Project:
 * --------
 *   Maui
 *
 * Description:
 * ------------
 *   struct of local parameter for hidd adp sap
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
 * 09 03 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integration BT solution.
 *
 * 09 01 2010 sh.lai
 * NULL
 * Integration change. into 1036OF
 *
 * Jan 7 2009 mbj06038
 * [MAUI_01544791] Screen always displays Connecting
 * 
 *
 * Nov 18 2008 mbj06038
 * [MAUI_01338633] [BT]The device name in HID Requeat screen disappeared
 * 
 *
 * Nov 3 2008 mbj06038
 * [MAUI_01256277] [BT HID] Disconnect HID from PC, MS won't display disconnect message and MS also can
 * 
 *
 * Nov 2 2007 MBJ06017
 * [MAUI_00033624] "Connect failed" pop up appears on selecting Remote control.
 * when connection failed becasue SCO, it will tell user the detail reason
 *
 * Feb 26 2007 mtk00560
 * [MAUI_00367691] [BT][HID] HID new feature check-in
 * 
 *
 * Feb 26 2007 mtk00560
 * [MAUI_00367691] [BT][HID] HID new feature check-in
 * 
 *
 * Feb 16 2007 mtk00560
 * [MAUI_00367691] [BT][HID] HID new feature check-in
 * 
  *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef BT_HIDD_STRUCT_H
#define BT_HIDD_STRUCT_H

//#ifdef __BT_HIDD_PROFILE__

#include "bttypes.h"
//#include "bt_hid_api.h"

//#ifndef BT_DM_STRUCT_H
//#error "bt_dm_struct.h should be included"
//#endif
/***************************************************************************** 
* Definations
*****************************************************************************/
#define HID_MAX_DEV_NAME    80
#define HID_DESC_MAX_SIZE	1024
#define HID_MAX_DEV_NUM		NUM_BT_DEVICES

#define HID_INVALID_CONN_ID	0xFFFFFFFF
#define HID_USING_CONN_ID		0xEEEEEEEE

#define BT_HIDD_ACTIVATE_REQ_EXT		0
#define BT_HIDD_DEACTIVATE_REQ_EXT	1
#define BT_HIDD_CONNECT_REQ_EXT		2
#define BT_HIDD_DISCONNECT_REQ_EXT	3
#define BT_HIDH_ACTIVATE_REQ_EXT		4
#define BT_HIDH_DEACTIVATE_REQ_EXT	5
#define BT_HIDH_CONNECT_REQ_EXT		6
#define BT_HIDH_DISCONNECT_REQ_EXT	7

#define HIDDA_TRANSA_FLAG_GET_REPORT        0x00000001
#define HIDDA_TRANSA_FLAG_SET_REPORT        0x00000002
#define HIDDA_TRANSA_FLAG_GET_PROTO         0x00000004
#define HIDDA_TRANSA_FLAG_SET_PROTO         0x00000008
#define HIDDA_TRANSA_FLAG_GET_IDLE_RATE     0x00000010
#define HIDDA_TRANSA_FLAG_SET_IDLE_RATE     0x00000020
//Add by stanley: 2007-0607
#define HIDDA_TRANSA_FLAG_HANDSHAKE     	0x00000040
#define HIDDA_TRANSA_FLAG_CONTROL		0x00000080
#define HIDDA_TRANSA_FLAG_GET_DESCLIST		0x00000100
//Add by shuguang 2010.08.19
#define	KEY_CAPS_LOCK_USAGE	57
#define	KEY_NUM_LOCK_USAGE	83
#define	KEY_SCROLL_LOCK_USAGE	71
#define	KEY_LEFT_SHIFT_USAGE	225
#define	KEY_LEFT_ALT_USAGE	226
#define	POINTER_X_USAGE	0x30
#define	POINTER_Y_USAGE	0x31
#define	WHEEL_USAGE	0x38



#define	LED_CAPS_LOCK_USAGE		0x0200
#define	LED_NUM_LOCK_USAGE		0x0100
#define	LED_SCROLL_LOCK_USAGE	0x0400


#define HID_DEVICE_ADDR_EQUAL(dev1, dev2)     (btmtk_os_memcmp(\
                                                                                                         (U8 *)dev1,  (U8 *)dev2, \
                                                                                                         sizeof(BD_ADDR)) ? TRUE : FALSE)


#define HID_MEM_MALLOC(size)		bt_win_malloc(size)
#define HID_MEM_FREEIF(p)			do{if(p){bt_win_free(p); p = NULL;}}while(0)		
#define HID_MEM_FREE(p)			bt_win_free(p)	




/*

#define HID_MEM_MALLOC(size)		malloc(size)
#define HID_MEM_FREEIF(p)			do{if(p){free(p); p = NULL;}}while(0)		
#define HID_MEM_FREE(p)			free(p)	
*/




#ifndef min
#define min	(((a) < (b)) ? (a) : (b))
#endif

/***************************************************************************** 
* Typedef 
*****************************************************************************/

/***************************************************************************** 
* Structure
*****************************************************************************/
typedef enum
{
	hidd_state_idle, /* hidd_deactivated */
	hidd_state_activated,
	hidd_state_connected,

	//hidd_state_activating,
	hidd_state_deactivating,
	hidd_state_connecting,
	hidd_state_disconnecting,
	hidd_state_authorizing,	
	hidd_state_waitconnect,
	hidd_state_total
		
} bt_hidd_state;


typedef enum
{
    //Add by stanley:2007-0608 hidda_handshake
    hidda_handshake,
    hidda_control,
    hidda_get_report,
    hidda_set_report,
    hidda_get_protocol,
    hidda_set_protocol,
    hidda_get_idle_rate,
    hidda_set_idle_rate,
    hidda_trasation_type_total
    
} hidda_trasation_type_enum;


typedef enum
{
	hidd_result_ok,
	hidd_result_failed,
	hidd_result_sco_reject,
	hidd_result_no_resource,
	hidd_result_total
	
} bt_hidd_result_enum;

typedef enum
{
	hidd_connect_authorization_result_accepted,
	hidd_connect_authorization_result_rejected,
	hidd_connect_authorization_total
	
} bt_hidd_connect_authorization_enum;

typedef enum 
{
	hidda_ctrl_eq_unknow,
	hidda_ctrl_eq_keyboard,
	hidda_ctrl_eq_mouse,
	hidda_ctrl_eq_total

} hidda_ctrl_eq_struct;

typedef enum
{
	hidd_ctrl_op_nop = 0,
	hidd_ctrl_op_hard_reset = 1,
	hidd_ctrl_op_soft_reset = 2,
	hidd_ctrl_op_suspend = 3,
	hidd_ctrl_op_exit_suspend = 4,
	hidd_ctrl_op_virtual_cable_unplug = 5,
	hidd_ctrl_op_total
	
} bt_hidd_ctrl_op_enum;

typedef enum
{
	hidd_report_other,
	hidd_report_input,
	hidd_report_output,
	hidd_report_feature,
	hidd_report_total
	
} bt_hidd_report_type_enum;

typedef enum
{
	hidd_protocol_boot,
	hidd_protocol_report,
	hidd_protocol_total
	
} bt_hidd_protocol_type_enum;

typedef struct
{
	LOCAL_PARA_HDR
    U8 command;
    BD_ADDR addr;
    void *param;	
} bt_hidd_req_ext_struct;

typedef bt_hidd_req_ext_struct bt_hidh_req_ext_struct;

typedef struct
{
    U8		*DescStr;			// my descriptor string
    U16	DescLen;								// my descriptor string length
    U16	ParserVersion;						// my hid parser version
    U8		CountryCode;						// my hid country code
    U16	LangBase;							// my language base
    U16	VendorID;							// my vendor id
    U16	DeviceID;							// my device id
    U16	ProductVersion;						// my product version
} bt_hidd_sdp_attribute_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_sdp_attribute_struct	sdpAttr;
} bt_hidd_activate_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_sdp_attribute_struct	sdpAttr;
    bt_hidd_result_enum	result;
} bt_hidd_activate_cnf_struct;


typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
} bt_hidd_deactivate_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
} bt_hidd_connect_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR	bt_addr;
    kal_uint32   connection_id;
    U8			*descList;
    U16		descLen;
} bt_hidd_connect_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR	bt_addr;
} bt_hidd_query_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR	bt_addr;
    kal_uint32   connection_id;
    U8			*descList;
    U16		descLen;
} bt_hidd_query_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum result;
    BD_ADDR	bt_addr;
    kal_uint32   connection_id;    
} bt_hidd_connect_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    kal_uint8   dev_name[HID_MAX_DEV_NAME];
} bt_hidd_connection_authorize_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_connect_authorization_enum	result;
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
} bt_hidd_connection_authorize_rsp_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR   bt_addr;
    kal_uint32   connection_id;    
} bt_hidd_disconnect_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
} bt_hidd_disconnect_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
} bt_hidd_disconnect_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
} bt_hidh_reconnect_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
} bt_hidh_unplug_ind_struct;


/* device role could only send unplug request */
typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum result;
} bt_hidd_unplug_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_ctrl_op_enum ctrl_op;
} bt_hidd_control_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_ctrl_op_enum ctrl_op;
} bt_hidh_send_control_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_ctrl_op_enum ctrl_op;
} bt_hidd_control_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint8		*data_ptr;    
} bt_hidd_interrupt_data_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    //kal_uint8		*data_ptr;    
    kal_uint8		data_ptr[0];    
} bt_hidh_interrupt_data_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    //bt_hidd_report_type_enum	report_type;    
} bt_hidd_interrupt_data_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint8		*data_ptr;    
} bt_hidd_interrupt_data_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint16		data;    
} bt_hidh_interrupt_data_ind_struct;

//typedef bt_hidd_interrupt_data_ind_struct bt_hidh_interrupt_data_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint8	report_id;
    kal_uint16	buffer_size;
    BT_BOOL      use_rpt_id;	
} bt_hidd_get_report_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint8	report_id;
    kal_uint16	buffer_size;
    BT_BOOL      use_rpt_id;	
} bt_hidh_get_report_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint8		*data_ptr;    
} bt_hidd_get_report_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint8		*data_ptr;    
} bt_hidh_get_report_cnf_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint8		*data_ptr;    
} bt_hidd_set_report_ind_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    kal_uint8		*data_ptr;    
} bt_hidd_set_report_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR  bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
    kal_uint16	data_len;
    //kal_uint8		*data_ptr;    
    kal_uint8		data_ptr[0];    
} bt_hidh_set_report_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_report_type_enum	report_type;    
} bt_hidd_set_report_rsp_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
} bt_hidd_get_protocol_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
} bt_hidh_get_protocol_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_protocol_type_enum protocol_type;
} bt_hidd_get_protocol_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_protocol_type_enum protocol_type;
} bt_hidh_get_protocol_cnf_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_protocol_type_enum protocol_type;
} bt_hidd_set_protocol_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    bt_hidd_protocol_type_enum protocol_type;
} bt_hidh_set_protocol_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
} bt_hidd_get_idle_rate_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
} bt_hidh_get_idle_rate_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
} bt_hidh_get_desclist_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint8	idle_rate;
} bt_hidd_get_idle_rate_cnf_struct;

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint8	idle_rate;
} bt_hidh_get_idle_rate_cnf_struct;


typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint8	idle_rate;
} bt_hidd_set_idle_rate_req_struct;

typedef struct
{
	LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint8	idle_rate;
} bt_hidh_set_idle_rate_req_struct;


typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint32	data;
} bt_hidd_set_cmd_cnf_struct;  /* With HANDSHAKE response */

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint32	data;
} bt_hidh_set_cmd_cnf_struct;  /* With HANDSHAKE response */

typedef struct
{
	LOCAL_PARA_HDR
    bt_hidd_result_enum	result;
    BD_ADDR bt_addr;
    kal_uint32   connection_id;
    kal_uint32	data;
} bt_hidh_descinfo_cnf_struct;  /* With HANDSHAKE response */



typedef enum
{
	MBTEVT_HID_HOST_ENABLE_SUCCESS,
	MBTEVT_HID_HOST_ENABLE_FAIL,
	MBTEVT_HID_HOST_DISABLE_SUCCESS,
	MBTEVT_HID_HOST_DISABLE_FAIL,
	
	MBTEVT_HID_HOST_CONNECT_SUCCESS,
	MBTEVT_HID_HOST_CONNECT_FAIL,
	MBTEVT_HID_HOST_DISCONNECT_SUCCESS,
	MBTEVT_HID_HOST_DISCONNECT_FAIL,
	
	MBTEVT_HID_HOST_GET_DESC_SUCCESS,
	MBTEVT_HID_HOST_GET_DESC_FAIL,
	MBTEVT_HID_HOST_SEND_CONTROL_SUCCESS,
	MBTEVT_HID_HOST_SEND_CONTROL_FAIL,
	MBTEVT_HID_HOST_SET_REPORT_SUCCESS,
	MBTEVT_HID_HOST_SET_REPORT_FAIL,
	MBTEVT_HID_HOST_GET_REPORT_SUCCESS,
	MBTEVT_HID_HOST_GET_REPORT_FAIL,
	MBTEVT_HID_HOST_SET_PROTOCOL_SUCCESS,
	MBTEVT_HID_HOST_SET_PROTOCOL_FAIL,
	MBTEVT_HID_HOST_GET_PROTOCOL_SUCCESS,
	MBTEVT_HID_HOST_GET_PROTOCOL_FAIL,
	MBTEVT_HID_HOST_SET_IDLE_SUCCESS,
	MBTEVT_HID_HOST_SET_IDLE_FAIL,
	MBTEVT_HID_HOST_GET_IDLE_SUCCESS,
	MBTEVT_HID_HOST_GET_IDLE_FAIL,
	MBTEVT_HID_HOST_SEND_REPORT_SUCCESS,
	MBTEVT_HID_HOST_SEND_REPORT_FAIL,
	MBTEVT_HID_HOST_RECEIVE_UNPLUG,
	MBTEVT_HID_HOST_RECEIVE_AUTHORIZE,

	MBTEVT_HID_DEVICE_ENABLE_SUCCESS,
	MBTEVT_HID_DEVICE_DISABLE_SUCCESS,
	MBTEVT_HID_DEVICE_CONNECT_FAIL,
	MBTEVT_HID_DEVICE_DISCONNECT_FAIL,
	MBTEVT_HID_DEVICE_ENABLE_FAIL,
	MBTEVT_HID_DEVICE_DISABLE_FAIL,
	MBTEVT_HID_DEVICE_CONNECT_SUCCESS,
	MBTEVT_HID_DEVICE_DISCONNECT_SUCCESS,
	MBTEVT_HID_DEVICE_SEND_UNPLUG_SUCCESS,
	MBTEVT_HID_DEVICE_SEND_UNPLUG_FAIL,
	MBTEVT_HID_DEVICE_SEND_REPORT_SUCCESS,
	MBTEVT_HID_DEVICE_SEND_REPORT_FAIL,
	MBTEVT_HID_DEVICE_RECEIVE_CONTROL,
	MBTEVT_HID_DEVICE_RECEIVE_REPORT,
	MBTEVT_HID_DEVICE_CHANGE_KEYBOARD_SUCCESS,
	MBTEVT_HID_DEVICE_CHANGE_MOUSE_SUCCESS,
	MBTEVT_HID_DEVICE_CHANGE_PHONE_SUCCESS,
}bt_hid_host_event_type;
/*****************************************************************************              
* Extern Global Variable                                                                    
*****************************************************************************/             
                                                                                           
/*****************************************************************************              
* Functions                                                                    
*****************************************************************************/     


//#endif //__BT_HIDD_PROFILE__

#endif//BT_HIDD_STRUCT_H
