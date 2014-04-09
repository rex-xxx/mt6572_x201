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
 * bt_spp_hdl.h
 * 
 * This file is the header file of External Adaptation handler of SPP profile used by Application.
 * Flow direction: APP <-- external ADP handler
 */


#ifndef __BT_SPP_HDL_H__
#define __BT_SPP_HDL_H__

 
void btmtk_spp_handle_connect_ind(ilm_struct *message);
void btmtk_spp_handle_connect_ind_req(ilm_struct *message);
void btmtk_spp_handle_disconnect_ind(ilm_struct *message);
void btmtk_spp_handle_activate_cnf(ilm_struct *message);
void btmtk_spp_handle_deactivate_cnf(ilm_struct *message);
void btmtk_spp_handle_disconnect_cnf(ilm_struct *message);
void btmtk_spp_handle_auth_req(ilm_struct *message);
void btmtk_spp_handle_connect_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_owner_ind(ilm_struct *message);
void btmtk_spp_handle_uart_plugout_ind(ilm_struct *message);
void btmtk_spp_handle_uart_ready_to_read_ind(ilm_struct *message);
void btmtk_spp_handle_uart_ready_to_write_ind(ilm_struct *message);
void btmtk_spp_handle_initialize_cnf(ilm_struct *message);
void btmtk_spp_handle_register_callback_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_open_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_close_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_get_bytes_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_put_bytes_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_set_owner_cnf(ilm_struct *message);
void btmtk_spp_handle_enable_cnf(ilm_struct *message);
void btmtk_spp_handle_disable_cnf(ilm_struct *message);
void btmtk_spp_handle_uart_data_available_ind(ilm_struct *message);
void btmtk_dun_handle_connect_ind(ilm_struct *message);
void btmtk_dun_handle_connect_ind_req(ilm_struct *message);
void btmtk_dun_handle_disconnect_ind(ilm_struct *message);
void btmtk_dun_handle_activate_cnf(ilm_struct *message);
void btmtk_dun_handle_deactivate_cnf(ilm_struct *message);
void btmtk_dun_handle_disconnect_cnf(ilm_struct *message);
void btmtk_dun_handle_auth_req(ilm_struct *message);
void btmtk_dun_handle_connect_cnf(ilm_struct *message);
void btmtk_fax_handle_connect_ind(ilm_struct *message);
void btmtk_fax_handle_connect_ind_req(ilm_struct *message);
void btmtk_fax_handle_disconnect_ind(ilm_struct *message);
void btmtk_fax_handle_activate_cnf(ilm_struct *message);
void btmtk_fax_handle_deactivate_cnf(ilm_struct *message);
void btmtk_fax_handle_disconnect_cnf(ilm_struct *message);
void btmtk_fax_handle_auth_req(ilm_struct *message);
void btmtk_fax_handle_connect_cnf(ilm_struct *message);
#ifdef __SPP_SHARED_MEMORY__
void btmtk_spp_handle_uart_assign_buffer_cnf(ilm_struct *message);
#endif	/* __SPP_SHARED_MEMORY__ */
void btmtk_spp_handle_message(ilm_struct *message);


#endif	/* __BT_SPP_HDL_H__ */

