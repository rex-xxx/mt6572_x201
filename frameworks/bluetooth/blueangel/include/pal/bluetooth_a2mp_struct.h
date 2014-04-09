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


#ifndef __BT_A2MP_STRUCT_H_
#define __BT_A2MP_STRUCT_H_

#include "pal_hci_struct.h"

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 status;
	kal_uint8 amp_status;
	kal_uint32 total_bandwidth;
	kal_uint32 max_guarantee_bandwidth;
	kal_uint32 min_latency;
	kal_uint32 max_pdu_size;
	kal_uint8 controller_type;
	kal_uint16 pal_capability;
	kal_uint16 max_amp_assoc_length;
	kal_uint32 max_flush_timeout;
    kal_uint32 best_effort_flush_timeout;
} bt_a2mp_read_local_amp_info_cmd_cnf_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 physical_link_hdl;
	kal_uint16 length_so_far;
	kal_uint16 amp_assoc_length;
} bt_a2mp_read_local_amp_assoc_cmd_req_struct;

/* Dlight check different with PAL. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
	kal_uint8 physical_link_hdl;
	kal_uint16 amp_assoc_remaining_length;
	kal_uint8 amp_assoc_fragment_size;
	kal_uint8 amp_assoc_fragment[248];
} bt_a2mp_read_local_amp_assoc_cmd_cnf_struct;

/* Dlight check different with PAL. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 physical_link_hdl;
	kal_uint16 length_so_far;
	kal_uint16 amp_assoc_remaining_length;
	kal_uint8 amp_assoc_fragment_size;
	kal_uint8 amp_assoc_fragment[248];
} bt_a2mp_write_remote_amp_assoc_cmd_req_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
	kal_uint8 physical_link_hdl;
} bt_a2mp_write_remote_amp_assoc_cmd_cnf_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 physical_link_hdl;
	kal_uint8 dedicated_amp_key_length;
	kal_uint8 dedicated_amp_key_type;    
	kal_uint8 dedicated_amp_key[248];
} bt_a2mp_create_physical_link_cmd_req_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_create_physical_link_cmd_cnf_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 physical_link_hdl;
	kal_uint8 dedicated_amp_key_length;
	kal_uint8 dedicated_amp_key_type;    
	kal_uint8 dedicated_amp_key[248];
} bt_a2mp_accept_physical_link_cmd_req_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_accept_physical_link_cmd_cnf_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 physical_link_hdl;
	kal_uint8 reason;
} bt_a2mp_physical_link_disconnect_cmd_req_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 physical_link_hdl;
} bt_a2mp_channel_selected_evt_ind_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 status;
    kal_uint8 physical_link_hdl;
} bt_a2mp_physical_link_completed_evt_ind_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_physical_link_disconnect_cmd_status_cnf_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
    kal_uint8 physical_link_hdl;
    kal_uint8 reason;
} bt_a2mp_physical_link_disconnect_completed_evt_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 physical_link_hdl;
    kal_uint8 tx_flow_spec[16];
    kal_uint8 rx_flow_spec[16];    
} bt_a2mp_create_logical_cmd_req_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 physical_link_hdl;
    kal_uint8 tx_flow_spec[16];
    kal_uint8 rx_flow_spec[16];    
} bt_a2mp_accept_logical_cmd_req_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_create_logical_cmd_cnf_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_accept_logical_cmd_cnf_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
    kal_uint16 logical_link_hdl;
    kal_uint8 physical_link_hdl;
    kal_uint8 tx_flow_spec_id;
} bt_a2mp_logical_link_completed_evt_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint16 logical_link_hdl;
} bt_a2mp_logical_link_disconnect_cmd_req_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_logical_link_disconnect_cmd_status_cnf_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
    kal_uint16 logical_link_hdl;
    kal_uint8 reason;
} bt_a2mp_logical_link_disconnect_completed_evt_struct;


/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint16 logical_link_hdl;
    kal_uint8 tx_flow_spec[16];
    kal_uint8 rx_flow_spec[16];
} bt_a2mp_flow_spec_modify_cmd_req_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
} bt_a2mp_flow_spec_modify_cmd_status_cnf_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
	kal_uint8 status;
    kal_uint16 logical_link_hdl;
} bt_a2mp_flow_spec_modify_completed_evt_struct;

/* Dlight check done. */
typedef struct
{
    LOCAL_PARA_HDR
    kal_uint8 status;
    kal_uint8 amp_status;
} bt_a2mp_amp_status_change_evt_struct;

typedef struct
{
    LOCAL_PARA_HDR
    kal_uint16  u2Total_num_data_blocks;
    kal_uint8   ucNum_of_handles;
    kal_uint16  au2Handle[PAL_LOGICAL_LINK_NUM];
    kal_uint16  au2Num_of_completed_packet[PAL_LOGICAL_LINK_NUM];
    kal_uint16  au2Num_of_completed_blocks[PAL_LOGICAL_LINK_NUM];
} bt_a2mp_amp_num_of_data_block_evt_struct;


#endif /* __BT_A2MP_STRUCT_H_ */

