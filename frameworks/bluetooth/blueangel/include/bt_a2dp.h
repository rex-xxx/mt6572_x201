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

#ifndef _BT_A2DP_H_
#define _BT_A2DP_H_

#include "bt_a2dp_struct.h"

// activate
extern void bt_a2dp_send_activate_req(module_type src_mod_id, U8 local_role);

// deactivate
extern void bt_a2dp_send_deactivate_req(module_type src_mod_id);

// connect
extern void bt_a2dp_send_signal_connect_req(module_type src_mod_id, bt_addr_struct *device_addr, U8 local_role);

// disconnect
extern void bt_a2dp_send_signal_disconnect_req(module_type src_mod_id, U16 connect_id);

// discover
extern void bt_a2dp_send_sep_discover_req(module_type src_mod_id, U16 connect_id);
extern void bt_a2dp_send_sep_discover_res(module_type src_mod_id, U16 connect_id, U16 result, U8 sep_num, bt_sep_info_struct *sep_list);

// get capabilities
extern void bt_a2dp_send_capabilities_get_req(module_type src_mod_id, U16 connect_id, U8 acp_seid);
extern void bt_a2dp_send_capabilities_get_res(module_type src_mod_id, U16 connect_id, U16 result, U8 audio_cap_num, bt_a2dp_audio_cap_struct *audio_cap_list);

// set configuration
extern void bt_a2dp_send_stream_config_req(module_type src_mod_id, U16 connect_id, U8 acp_seid, U8 int_seid, bt_a2dp_audio_cap_struct *audio_cap);
extern void bt_a2dp_send_stream_config_res(module_type src_mod_id, U16 result, U8 stream_handle);

// reconfigure
extern void bt_a2dp_send_stream_reconfig_req(module_type src_mod_id, U8 stream_handle, bt_a2dp_audio_cap_struct *audio_cap);
extern void bt_a2dp_send_stream_reconfig_res(module_type src_mod_id, U16 result, U8 stream_handle);

// open
extern void bt_a2dp_send_stream_open_req(module_type src_mod_id, U8 stream_handle);
extern void bt_a2dp_send_stream_open_res(module_type src_mod_id, U16 result, U8 stream_handle);

// start
extern void bt_a2dp_send_stream_start_req(module_type src_mod_id, U8 stream_handle);
extern void bt_a2dp_send_stream_start_res(module_type src_mod_id, U16 result, U8 stream_handle);

// pause
extern void bt_a2dp_send_stream_pause_req(module_type src_mod_id, U8 stream_handle);
extern void bt_a2dp_send_stream_pause_res(module_type src_mod_id, U16 result, U8 stream_handle);

// qos
extern U32 bt_a2dp_alg_set_bit_rate_from_qos(U8 qos, U32 bit_rate_prev, U32(*SetCodecBitRate) (U32));
extern void bt_a2dp_alg_set_bit_rate_from_qos_init(U32 bit_rate_cur);

// send data
extern void bt_a2dp_send_stream_data_send_req(module_type src_mod_id, U8 stream_handle, A2DP_codec_struct *codec);

// close
extern void bt_a2dp_send_stream_close_req(module_type src_mod_id, U8 stream_handle);
extern void bt_a2dp_send_stream_close_res(module_type src_mod_id, U16 result, U8 stream_handle);

// abort
extern void bt_a2dp_send_stream_abort_req(module_type src_mod_id, U8 stream_handle);
extern void bt_a2dp_send_stream_abort_res(module_type src_mod_id, U8 stream_handle);

/* utilities */
extern kal_bool bt_a2dp_match_audio_capabilities(bt_a2dp_audio_cap_struct *audio_cap, bt_a2dp_audio_cap_struct *audio_cfg, U16 *result);
extern void bt_a2dp_adp_init(void);
            
#endif /* _BT_A2DP_H_ */
