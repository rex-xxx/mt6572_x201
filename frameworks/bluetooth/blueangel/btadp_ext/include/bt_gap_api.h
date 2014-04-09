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
/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bt_gap_api.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to
 *
 * Author:
 * -------
 * Dlight Ting
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
#ifndef __GAP_API_H__
#define __GAP_API_H__
#include "bt_types.h"
#include "bt_struct.h"

#if defined(BTMTK_ON_WISE) || defined(BTMTK_ON_WISESDK)
void btmtk_gap_get_device_name(T_MBT_BDADDR RemoteBDAddr, MBT_CHAR *NameBuf, MBT_INT BufSize);
void btmtk_gap_get_device_nickname(T_MBT_BDADDR RemoteBDAddr, MBT_CHAR *NameBuf, MBT_INT BufSize);

MBT_VOID btmtk_gap_power_on_req(MBT_VOID);
MBT_VOID btmtk_gap_power_off_req(MBT_VOID);

MBT_VOID btmtk_gap_discovery_request(MBT_SERVICE_ID MBTSvcID, MBT_INT nMaxCount);
MBT_VOID btmtk_gap_discovery_filter_request(T_MBT_DISCOVERY_FILTER *filter, MBT_INT nMaxCount);
MBT_BOOL btmtk_gap_is_connected(MBT_VOID);
MBT_BOOL btmtk_gap_is_profile_connected(MBT_SERVICE_ID MBTSvcID);
MBT_BOOL btmtk_gap_get_link_state_request(T_MBT_BDADDR RemoteBDAddr);
MBT_VOID btmtk_gap_discovery_cancel_request(MBT_VOID);
MBT_BOOL btmtk_gap_write_local_name_request(MBT_CHAR *name);
MBT_BOOL btmtk_gap_write_remote_name_request(T_MBT_BDADDR PairedDevAddr, MBT_CHAR* NickName);
MBT_BOOL btmtk_gap_is_authorized(T_MBT_BDADDR RemoteBDAddr);
MBT_BOOL btmtk_gap_set_authorize(T_MBT_BDADDR RemoteBDAddr, MBT_BOOL bAuthorize);
MBT_VOID btmtk_gap_authorize_response(MBT_BYTE AuthMode, MBT_SERVICE_ID AuthSvc);
MBT_VOID btmtk_gap_read_remote_name_request(T_MBT_BDADDR RemoteBDAddr);
MBT_VOID btmtk_gap_read_remote_name_cancel_request(MBT_VOID);
MBT_BOOL btmtk_gap_set_visible_request(MBT_BOOL bVisible);
MBT_BOOL btmtk_gap_set_connectable_request(MBT_BOOL bConnectable);
MBT_VOID btmtk_gap_bonding_request(T_MBT_BDADDR RemoteBDAddr, T_MBT_PIN PinReq, MBT_INT PinLength);
MBT_VOID btmtk_gap_bonding_cancel_request(MBT_VOID);
MBT_VOID btmtk_gap_pairing_request(MBT_BOOL bAccept, T_MBT_PIN PinRes, MBT_INT PinLength);
MBT_VOID btmtk_gap_service_search_request(T_MBT_BDADDR RemoteBDAddr);
MBT_VOID btmtk_gap_service_search_filter_request(T_MBT_BDADDR RemoteBDAddr, T_MBT_SERVICE_DISCOVERY_FILTER *filter);
MBT_VOID btmtk_gap_service_search_cancel_request(MBT_VOID);
MBT_BOOL btmtk_gap_delete_trust_request(T_MBT_BDADDR RemoteBDAddr);
MBT_BOOL btmtk_gap_delete_trust_all_request(MBT_VOID);
MBT_VOID btmtk_gap_security_user_confirm_response(MBT_BOOL bAccept);
MBT_BOOL btmtk_gap_block_list_add_request(T_MBT_BDADDR RemoteBDAddr);
MBT_BOOL btmtk_gap_block_list_remove_request(T_MBT_BDADDR RemoteBDAddr);

#else /* defined(BTMTK_ON_WISE) || defined(BTMTK_ON_WISESDK) */
void btmtk_gap_read_local_addr_request(void);
void btmtk_gap_read_local_cod_request(void);
void btmtk_gap_search_raw_request(
        btbm_bd_addr_t *addr, 
        U8 type, 
        U8 *byte, 
        U8 size);
void btmtk_gap_read_local_name_request(void);
void btmtk_gap_write_scanenable_mode_request(U8 mode);
void btmtk_gap_read_scanenable_mode_request(void);
void btmtk_gap_write_local_cod_request(U32 cod, btbm_write_cod_type type);
void btmtk_gap_write_authentication_mode_request(btbm_authentication_mode mode);
void btmtk_gap_pin_code_response(btbm_bd_addr_t *addr, U8 *pin);
void btmtk_gap_search_attribute_request(
        btbm_bd_addr_t *addr, 
        U32 uuid, 
        U16 attr_id);
void btmtk_gap_link_allow_request(void);
void btmtk_gap_link_disallow_request(void);
void btmtk_gap_link_connect_accept_response(U8 accept, btbm_bd_addr_t *addr);
void btmtk_gap_security_keypress_notify_response(void);
void btmtk_gap_link_connect_accept_not_auto_request(void);
void btmtk_gap_security_keypress_notify_cancel_request(btbm_bd_addr_t *addr);
void btmtk_gap_block_active_link_request(btbm_bd_addr_t *addr);
void btmtk_gap_block_list_update_request(U8 count, btbm_bd_addr_t *addr);
#endif /* defined(BTMTK_ON_WISE) || defined(BTMTK_ON_WISESDK) */
#endif

