/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#ifndef _BLUETOOTH_HDP_STRUCT_H_
#define _BLUETOOTH_HDP_STRUCT_H_

#define BT_HDP_INSTANCE_MAX_DESC_LEN 255
#define BT_HDP_INSTANCE_INVALID_ID 0xFF

#ifndef BOOL
typedef unsigned char BOOL;
#endif

typedef enum
{
	BT_HDP_SUCCESS,
	BT_HDP_FAIL,
	BT_HDP_FAIL_BUSY,
	BT_HDP_FAIL_NOT_ALLOW,
	BT_HDP_FAIL_TIMEOUT,
	BT_HDP_FAIL_REMOTE_REJECT,
	BT_HDP_FAIL_NO_RESOURCE,
} bt_hdp_status;


typedef enum
{
	BT_HDP_ROLE_SOURCE = 0x00,
	BT_HDP_ROLE_SINK 	= 0x01,
	BT_HDP_ROLE_INVALID = 0xFF,
} bt_hdp_role;

typedef enum 
{
	BT_HDP_CHANNEL_TYPE_NO_PREFERENCE	= 0x00,
	BT_HDP_CHANNEL_TYPE_RELIABLE		= 0x01,
	BT_HDP_CHANNEL_TYPE_STREAMING		= 0x02,	
} bt_hdp_channel_type;

/*Notes: indicate which type is chosed to reopen channel*/
typedef enum 
{
	BT_HDP_REOPEN_TYPE_DEFAULT			= 0x00,	//based on main channels
	BT_HDP_REOPEN_TYPE_INDEX			= 0x01,	//based on index
	BT_HDP_REOPEN_TYPE_MDL_ID			= 0x02,	//based on MDL ID
} bt_hdp_reopen_type;

typedef struct 
{
	LOCAL_PARA_HDR
	U8							role;
	U16 						dataType;
	U8  				    channelType;
	char 	description[BT_HDP_INSTANCE_MAX_DESC_LEN];	
} bt_hdp_register_instance_req_struct;


typedef struct 
{
	LOCAL_PARA_HDR
	U8	result;
	U8   mdepId;
} bt_hdp_register_instance_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	U8   mdepId;	
} bt_hdp_deregister_instance_req_struct;


typedef struct 
{
	LOCAL_PARA_HDR
	U8	result;
	U8   mdepId;
} bt_hdp_deregister_instance_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U8 				mdepId;
	U8				config;
	U32				index;   //internal indicator
} bt_hdp_connect_req_struct;


typedef struct 
{
	LOCAL_PARA_HDR
	U8 				result;
	bt_addr_struct 	bdaddr;
	U8 				mdepId;
	U16				mdlId;
	BOOL			mainChannel;
	U32				index; 
} bt_hdp_connect_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U16				mdlId;
	U32				index; 
} bt_hdp_disconnect_req_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	U8 				result;
	bt_addr_struct 	bdaddr;
	U16 			mdlId;
	U32				index;
} bt_hdp_disconnect_cnf_struct;

/*Force to disconnect L2CAP connection*/
typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U16				mdlId;
} bt_hdp_remove_connection_req_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	U8 				result;
	bt_addr_struct 	bdaddr;
} bt_hdp_remove_connection_cnf_struct;


typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U8 				mdepId;
	U16				mdlId;
	BOOL			mainChannel;
	U16 			l2capId;
} bt_hdp_channel_opened_ind_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U16 			mdlId;
} bt_hdp_channel_closed_ind_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
} bt_hdp_get_main_channel_req_struct;


typedef struct 
{
	LOCAL_PARA_HDR
	U8 result;
	bt_addr_struct 	bdaddr;
	U16 mdlId;
} bt_hdp_get_main_channel_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U16				mdlId;
} bt_hdp_get_instance_req_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	U8 result;
	U8 mdepId;
} bt_hdp_get_instance_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U16 mdlId;
} bt_hdp_get_l2cap_channel_req_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	U8 result;
	bt_addr_struct 	bdaddr;
	U16 mdlId;
	U16 l2capId;
} bt_hdp_get_l2cap_channel_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U8				role;
} bt_hdp_echo_req_struct;
typedef struct 
{
	LOCAL_PARA_HDR
	U8 result;
	bt_addr_struct 	bdaddr;
	U8				role;
} bt_hdp_echo_cnf_struct;

typedef struct 
{
	LOCAL_PARA_HDR
	bt_addr_struct 	bdaddr;
	U8				type;     //bt_hdp_reopen_type
	U16				value;
} bt_hdp_reopen_connection_req_struct;


#endif
