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

/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bt_pan_struct.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is internal used header file for external adp layer
 *
 * Author:
 * -------
 * Ting Zheng
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime:
 * $Log: 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/

#ifndef __BT_PAN_STRUCT_H__
#define __BT_PAN_STRUCT_H__

#include "bt_struct.h"

typedef enum 
{
    BT_PAN_OP_ACTIVATE = 0,
    BT_PAN_OP_DEACTIVATE,
    BT_PAN_OP_CONNECT,
    BT_PAN_OP_DISCONNECT
} bt_pan_op_enum;

typedef enum
{
    BT_PAN_APP_STAT_NO_CONNECTION = 0,
    BT_PAN_APP_STAT_CONNECTED,	

    BT_PAN_APP_STAT_CONNECTING,	
    BT_PAN_APP_STAT_AUTHORIZING,	
    BT_PAN_APP_STAT_DISCONNECTING,	
    BT_PAN_APP_STAT_CONN_CANCEL
} BT_PAN_APP_STATE;

typedef struct _BT_PAN_DEVICE
{
    BD_ADDR	addr;
    BT_PAN_APP_STATE	state;
} BT_PAN_DEVICE;

typedef struct _BT_PAN_APP_CTX
{
    BT_BOOL 	enabled;
    BT_BOOL	do_disable;	
    BT_PAN_DEVICE	device[PAN_MAX_DEV_NUM];
} BT_PAN_APP_CTX;

typedef struct
{
    LOCAL_PARA_HDR
    U8 op;
    bt_pan_service_enum service;
    BD_ADDR addr;
} bt_pan_op_struct;


extern BTMTK_EventCallBack g_PAN_MMI_Callback;
extern BT_PAN_APP_CTX g_pan_cntx;


void pan_op_activate(void);
void pan_op_deactivate(void);
void pan_op_connect(bt_pan_service_enum service_type, BD_ADDR *bt_addr);
void pan_op_disconnect(BD_ADDR *bt_addr);
void pan_deactivate(void);
BT_PAN_DEVICE *pan_find_device_by_addr(U8 *device_addr);
BT_PAN_DEVICE *pan_find_free_device(U8 *device_addr);
BT_PAN_DEVICE *pan_find_connected_device(void);
void pan_set_state(BT_PAN_DEVICE *dev, BT_PAN_APP_STATE state);
void pan_handle_cmd(bt_pan_op_struct *cmd);


#endif
