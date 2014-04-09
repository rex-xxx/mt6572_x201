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
 * Bt_a2dp_api.h
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
 * Tina Shen
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
#ifndef __BT_A2DP_API_H__
#define __BT_A2DP_API_H__
#include "bt_types.h"
#include "bluetooth_a2dp_struct.h"

#define A2DP_STREAM_DEBUG FALSE


#ifdef BTMTK_ON_LINUX
void btmtk_a2dp_setSockAddress(struct sockaddr_un *addr, socklen_t addrlen);
#endif
void btmtk_a2dp_send_activate_req(
        int srvcsock, 
        int sockfd);

void btmtk_a2dp_send_deactivate_req(module_type src_mod_id);
void btmtk_a2dp_deactive_req(void);


#ifndef BTMTK_ON_LINUX
void btmtk_a2dp_send_signal_connect_req(module_type src_mod_id, U8 *device_addr, U8 local_role);
#else
void btmtk_a2dp_send_signal_connect_req(bt_addr_struct *device_addr, U8 local_role);
#endif

void btmtk_a2dp_send_signal_disconnect_req(module_type src_mod_id, U16 connect_id);

void btmtk_a2dp_send_sep_discover_req(module_type src_mod_id, U16 connect_id);

void btmtk_a2dp_send_sep_discover_res(
        module_type src_mod_id,
        U16 connect_id,
        U16 result,
        U8 sep_num,
        bt_sep_info_struct *sep_list);

void btmtk_a2dp_send_capabilities_get_req(module_type src_mod_id, U16 connect_id, U8 acp_seid);

void btmtk_a2dp_send_capabilities_get_res(
        module_type src_mod_id,
        U16 connect_id,
        U16 result,
        bt_a2dp_audio_cap_struct *audio_cap_list);

void btmtk_a2dp_send_stream_config_req(
        module_type src_mod_id,
        U16 connect_id,
        U8 acp_seid,
        U8 int_seid,
        bt_a2dp_audio_cap_struct *audio_cap);

void btmtk_a2dp_send_stream_config_res(module_type src_mod_id, U16 result, U8 stream_handle);

void btmtk_a2dp_send_stream_reconfig_req(
        module_type src_mod_id,
        U8 stream_handle,
        bt_a2dp_audio_cap_struct *audio_cap);

void btmtk_a2dp_send_stream_reconfig_res(module_type src_mod_id, U16 result, U8 stream_handle);

void btmtk_a2dp_send_stream_open_req(module_type src_mod_id, bt_addr_struct *device_addr, U8 local_role);

void btmtk_a2dp_send_stream_open_res(module_type src_mod_id, U8 stream_handle, U16 accept);

void btmtk_a2dp_send_stream_start_req(module_type src_mod_id, U8 stream_handle);

#ifdef MTK_BT_FM_OVER_BT_VIA_CONTROLLER
void btmtk_a2dp_fm_controller_start_req(module_type src_mod_id, U8 stream_handle);
void btmtk_a2dp_fm_controller_stop_req(module_type src_mod_id, U8 stream_handle);
void btmtk_a2dp_fm_controller_suspend_req(module_type src_mod_id, U8 stream_handle);
//void btmtk_a2dp_fm_controller_resume_req(module_type src_mod_id, U8 stream_handle);

#endif

void btmtk_a2dp_send_stream_start_res(module_type src_mod_id, U16 result, U8 stream_handle);

void btmtk_a2dp_send_stream_pause_req(module_type src_mod_id, U8 stream_handle);

void btmtk_a2dp_send_stream_pause_res(module_type src_mod_id, U16 result, U8 stream_handle);

//void btmtk_a2dp_send_stream_data_send_req(U8 stream_handle, const U8 *data, U16 length);

void btmtk_a2dp_send_stream_close_req(module_type src_mod_id, U8 stream_handle);

void btmtk_a2dp_send_stream_close_res(module_type src_mod_id, U16 result, U8 stream_handle);

void btmtk_a2dp_send_stream_abort_req(module_type src_mod_id, U8 stream_handle);

void btmtk_a2dp_send_stream_abort_res(module_type src_mod_id, U8 stream_handle);



void btmtk_A2dp_send_appi_bt_disconnect_request (U8* addr);
void btmtk_A2dp_send_appi_bt_start_request (void);
void btmtk_A2dp_send_appi_bt_stop_request (void);
void btmtk_A2dp_send_appi_bt_pause_request (void);
void btmtk_A2dp_send_appi_bt_resume_request (void);
void btmtk_a2dp_authorize_res(U8 accept, U8 stream_handle);

//void btmtk_a2dp_send_next_stream_data_req(U8 stream_handle);

void btmtk_a2dp_avrcp_send_cmd_key_req(U32 keyevent);

void btmtk_a2dp_close_device(bt_addr_struct *addr);

void btmtk_a2dp_send_wifi_connect_req();

void btmtk_a2dp_send_wifi_disconnect_req();

#endif
