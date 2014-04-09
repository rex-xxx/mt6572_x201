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

/* 
 * bt_spp_porting.h
 * 
 * This file is the header file of Porting layer API of SPP profile used by MMI.
 * Flow direction: APP --> Porting layer API
 */

#ifndef __BT_SPP_PORTING_H__
#define __BT_SPP_PORTING_H__

#include "bluetooth_gap_struct.h" 

#include "MBTDataType.h"
#include "MBTType.h"
#include "mbt_debugmsg.h"


#define SPP_MBT_ERR		MBT_ERR
#define SPP_MBT_ERR1	MBT_ERR1
#define SPP_MBT_ERR2	MBT_ERR2
#define SPP_MBT_ERR3	MBT_ERR3
#define SPP_SDC_LOG		MBT_LOG
#define SPP_SDC_LOG1	MBT_LOG1
#define SPP_SDC_LOG2	MBT_LOG2
#define SPP_SDC_LOG3	MBT_LOG3
#ifdef BTMTK_WISE_MBT_LOG
#define SPP_MBT_LOG		MBT_LOG
#define SPP_MBT_LOG1	MBT_LOG1
#define SPP_MBT_LOG2	MBT_LOG2
#define SPP_MBT_LOG3	MBT_LOG3
#else
#define SPP_MBT_LOG
#define SPP_MBT_LOG1
#define SPP_MBT_LOG2
#define SPP_MBT_LOG3
#endif

typedef struct
{
	MBT_BYTE port;
	MBT_BOOL bServer;
	MBT_CHAR svcName[MBT_SPP_MAX_SVC_NAME_LEN];
	MBT_SERVICE_ID svcUUID;
	MBT_SPP_CBACK *pCBack;
	T_MBT_BDADDR bdAddr;
	MBT_BOOL inUse;
	MBT_BOOL authReq;
	MBT_BYTE* bufSend;
}btmtk_spp_mmi_context_struct;

typedef struct
{
	MBT_BYTE port;
	T_MBT_BDADDR bdAddr;
	MBT_BOOL inUse;
	MBT_BOOL authReq;
}btmtk_dun_mmi_context_struct;


MBT_VOID btmtk_spp_mbt_convert_array2bdaddr(btbm_bd_addr_t *dest, U8 *src);
MBT_VOID btmtk_spp_mbt_convert_bdaddr2array(U8 *dest, btbm_bd_addr_t *source);
MBT_VOID btmtk_spp_mbt_cb_enable_cnf(MBT_VOID);
MBT_VOID btmtk_spp_mbt_cb_disable_cnf(MBT_VOID);
MBT_VOID btmtk_spp_mbt_cb_listen_cnf(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_listen_stop_cnf(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_connect_cnf(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_disconnect_cnf(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_send_data_cnf(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_receive_data_ind(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_connect_ind_req(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_connect_ind(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_disconnect_ind(MBT_VOID *parms);
MBT_VOID btmtk_dun_mbt_cb_enable_cnf(MBT_VOID);
MBT_VOID btmtk_dun_mbt_cb_disable_cnf(MBT_VOID);
MBT_VOID btmtk_dun_mbt_cb_listen_cnf(MBT_VOID *parms);
MBT_VOID btmtk_dun_mbt_cb_listen_stop_cnf(MBT_VOID *parms);
MBT_VOID btmtk_dun_mbt_cb_connect_ind_req(MBT_VOID *parms);
MBT_VOID btmtk_dun_mbt_cb_connect_ind(MBT_VOID *parms);
MBT_VOID btmtk_dun_mbt_cb_disconnect_ind(MBT_VOID *parms);
MBT_VOID btmtk_dun_mbt_cb_disconnect_cnf(MBT_VOID *parms);
MBT_VOID btmtk_spp_mbt_cb_event_handler(MBT_VOID *context, BT_CALLBACK_EVENT event, MBT_VOID *parms, U16 datasize);
MBT_VOID btmtk_spp_mbt_enable (MBT_VOID);
MBT_VOID btmtk_spp_mbt_disable (MBT_VOID);
MBT_VOID btmtk_spp_mbt_connect(T_MBT_BDADDR BdAddr,T_MBT_SPP_SVC_INFO * SvcInfo, MBT_SPP_CBACK * pcback);
MBT_VOID btmtk_spp_mbt_disconnect(MBT_SHORT Handle);
MBT_VOID btmtk_spp_mbt_listen(T_MBT_SPP_SVC_INFO * SvcInfo, MBT_SPP_CBACK * pCback);
MBT_VOID btmtk_spp_mbt_listenstop(MBT_SHORT Handle);
MBT_VOID btmtk_spp_mbt_senddata(MBT_SHORT Handle,T_MBT_SPP_DATA *txData);
MBT_BOOL btmtk_spp_is_connected(MBT_SERVICE_ID AuthSvc);
MBT_BOOL btmtk_spp_is_dev_connected(T_MBT_BDADDR RemoteBDAddr);
MBT_VOID btmtk_spp_authorize_res(MBT_BYTE result);
MBT_VOID btmtk_dun_mbt_enable(MBT_VOID);
MBT_VOID btmtk_dun_mbt_disable(MBT_VOID);
MBT_VOID btmtk_dun_mbt_disconnect(T_MBT_BDADDR BdAddr);
MBT_VOID btmtk_dun_mbt_listen(MBT_VOID);
MBT_VOID btmtk_dun_mbt_listenstop(MBT_VOID);
MBT_BOOL btmtk_dun_is_connected(MBT_VOID);
MBT_BOOL btmtk_dun_is_dev_connected(T_MBT_BDADDR RemoteBDAddr);
MBT_VOID btmtk_dun_authorize_res(MBT_BYTE result);


#endif	/* __BT_SPP_PORTING_H__ */

