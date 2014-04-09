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
 * Bt_avrcp_api.h
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
#ifndef __BT_AVRCP_API_H__
#define __BT_AVRCP_API_H__
#include "bt_types.h"

#include "bluetooth_avrcp_struct.h"
#include "bt_avrcp_struct.h"

/* AVRCP common */
void btmtk_avrcp_send_activate_req( AvrcpChannelContext *cntx, U8 chnl_num, U8 local_role );
void btmtk_avrcp_send_deactivate_req( AvrcpChannelContext *cntx, U8 chnl_num );
void btmtk_avrcp_send_connect_req( AvrcpChannelContext *cntx, U8 chnl_num, U8 *addr, U8 local_role );
void btmtk_avrcp_send_connect_ind_rsp(AvrcpChannelContext *cntx, U8 chnl_num, BT_BOOL accept);
void btmtk_avrcp_send_disconnect_req( AvrcpChannelContext *cntx, U8 chnl_num);
void btmtk_avrcp_send_cmd_frame_req(AvrcpChannelContext *cntx,
                                            U8 chnl_num, 
                                            U8 seq_id, 
                                            U8 c_type, 
                                            U8 subunit_type, 
                                            U8 subunit_id, 
                                            U16 data_len, 
                                            U8* frame_data);
void btmtk_avrcp_send_cmd_frame_ind_rsp( AvrcpChannelContext *cntx, U8 chnl_num, U8 seq_id, U16 profile_id,U8 c_type, U8 subunit_type, U8 subunit_id, U16 data_len, U8* frame_data);

void btmtk_avrcp_authorize_res(AvrcpChannelContext *cntx, U8 accept);


/* AVRCP 1.0 CT */
void btmtk_avrcp_send_keyvent_req( AvrcpChannelContext *cntx, U16 keycode, U8 press);

/* AVRCP 1.0 TG */
void btmtk_avrcp_send_pass_through_rsp( AvrcpChannelContext *cntx,
                                                U8 chnl_num, 
                                                U8 seq_id, 
                                                U16 profile_id,
                                                U8 c_type, 
                                                U8 subunit_type, 
                                                U8 subunit_id, 
                                                U16 data_len, 
                                                U8* frame_data);
/* AVRCP 1.3 TG */
void btmtk_avrcp_send_get_capabilities_rsp(AvrcpChannelContext *cntx, U8 error, U8 count, U8 *events);
void btmtk_avrcp_send_list_player_attrs_rsp(AvrcpChannelContext *cntx, U8 error, U8 count, U8 *attr_ids);
void btmtk_avrcp_send_list_player_values_rsp(AvrcpChannelContext *cntx, U8 error, U8 attr_id, U8 count, U8 *values);
void btmtk_avrcp_send_get_curplayer_value_rsp(AvrcpChannelContext *cntx, U8 error, U8 count, U8 *attr_ids, U8 *values);
void btmtk_avrcp_send_set_player_value_rsp(AvrcpChannelContext *cntx, U8 error);
void btmtk_avrcp_send_get_player_attr_text_rsp(AvrcpChannelContext *cntx, U8 error, U8 index, U8 total, U8 attr_id, U16 charset, U8 strlen, U8 *strText);
void btmtk_avrcp_send_get_player_value_text_value_rsp(AvrcpChannelContext *cntx, U8 error, U8 index, U8 total, U8 attr_id, U8 value_id, U16 charset, U8 strlen, U8 *strText);

void btmtk_avrcp_send_inform_charsetset_rsp(AvrcpChannelContext *cntx, U8 error);
void btmtk_avrcp_send_battery_status_rsp(AvrcpChannelContext *cntx, U8 error);
void btmtk_avrcp_send_get_element_attributes_rsp(AvrcpChannelContext *cntx, U8 error, U8 index, U8 total, U8 attr_id, U16 charset, U16 strlen, U8 *strText);
void btmtk_avrcp_send_get_playstatus_rsp(AvrcpChannelContext *cntx, U8 error, U32 song_length, U32 song_position, U8 status);

void btmtk_avrcp_send_reg_notievent_playback_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U8 status);
void btmtk_avrcp_send_reg_notievent_track_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U8 *identifier);
void btmtk_avrcp_send_reg_notievent_reached_end_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim);
void btmtk_avrcp_send_reg_notievent_reached_start_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim);
void btmtk_avrcp_send_reg_notievent_pos_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U32 position);
void btmtk_avrcp_send_reg_notievent_battery_status_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U8 status);
void btmtk_avrcp_send_reg_notievent_system_status_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U8 status);
void btmtk_avrcp_send_reg_notievent_now_playing_content_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim);
void btmtk_avrcp_send_reg_notievent_player_appsettings_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U8 count, U8 *attr_ids, U8 *value_ids);

void btmtk_avrcp_send_reg_notievent_volume_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U8 volume);
void btmtk_avrcp_send_reg_notievent_addredplayer_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U16 player_id, U16 uid_counter);
void btmtk_avrcp_send_reg_notievent_availplayers_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim);
void btmtk_avrcp_send_reg_notievent_uids_changed_rsp(AvrcpChannelContext *cntx, U8 error, U8 interim, U16 uid_counter);

/* AVRCP 1.4 TG */
void btmtk_avrcp_send_browse_connect_req(AvrcpChannelContext *cntx, U8 chnl_num, U8 local_role );
void btmtk_avrcp_send_browse_disconnect_req(AvrcpChannelContext *cntx, U8 chnl_num);

void btmtk_avrcp_send_set_absolute_volume_rsp(AvrcpChannelContext *cntx, U8 error, U8 status, U8 volume);
void btmtk_avrcp_send_set_addressedplayer_rsp(AvrcpChannelContext *cntx, U8 error, U8 status);

void btmtk_avrcp_reset_send_get_folderitems_rsp(AvrcpChannelContext *cntx); // reset send_folderitems
void btmtk_avrcp_config_send_get_folderitems_attribute_rsp(AvrcpChannelContext *cntx, U8 item, U8 attritem, U32 attrID, U16 charset, U16 name_len, const U8 *name); // config send-folderitems' attributes
void btmtk_avrcp_config_send_get_folderitems_rsp(AvrcpChannelContext *cntx, U8 item, U8 total, avrcp_folder_mixed_item* folder, U16 name_len, const U8 *name); // config send-folderitem's item
void btmtk_avrcp_send_get_folderitems_rsp(AvrcpChannelContext *cntx, U8 error, U8 status, U16 uid_counter); // send out the message

void btmtk_avrcp_config_set_browsedplayer_rsp(AvrcpChannelContext *cntx, U8 item, U8 total, U16 name_len, U8 *foldername);
void btmtk_avrcp_send_set_browsedplayer_rsp(AvrcpChannelContext *cntx, U8 error, U8 status, U16 uid_counter, U32 num, U16 charset);

void btmtk_avrcp_send_change_path_rsp(AvrcpChannelContext *cntx, U8 error, U8 status, U32 num_of_items);
void btmtk_avrcp_send_get_itemattributes_rsp(AvrcpChannelContext *cntx, U8 error, U8 status, U8 item, U8 total, U32 attr_id, U16 charset, U16 len, U8 *string);
void btmtk_avrcp_send_play_items_rsp(AvrcpChannelContext *cntx, U8 error, U8 status);
void btmtk_avrcp_send_search_rsp(AvrcpChannelContext *cntx, U8 error, U8 status, U16 uid_counter, U32 found_num_of_items);
void btmtk_avrcp_send_add_tonowplaying_rsp(AvrcpChannelContext *cntx, U8 error, U8 status);

#endif
