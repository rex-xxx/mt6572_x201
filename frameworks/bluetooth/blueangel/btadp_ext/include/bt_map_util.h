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
 * Bt_map_util.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file contains utility functions used by MAP
 *
 * Author:
 * -------
 * Autumn Li
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
#ifndef __BT_MAP_UTIL_H__
#define __BT_MAP_UTIL_H__

#if defined(BTMTK_ON_WISE) || defined(BTMTK_ON_WISE)
void map_postevent(U32 event, S16 index);
#else
void map_postevent(U32 event, S8* arg);
#endif

void btmtk_map_util_log_string10(char *str);

BT_BOOL btmtk_map_util_is_valid_bdaddr(MBT_BDADDR addr);
bt_map_result_enum btmtk_map_util_translate_result_mbt2bt(MBT_AUTHRES from);
bt_map_msg_enum btmtk_map_util_translate_msg_type_mbt2bt(MBT_MAP_MSG_TYPE from);
MBT_MAP_MSG_TYPE btmtk_map_util_translate_msg_type_bt2mbt(bt_map_msg_enum from);
MBT_MAP_MSG_READ_STATUS btmtk_map_util_translate_filter_msg_type_bt2mbt(bt_map_filter_msg_enum from);
MBT_MAP_MSG_READ_STATUS btmtk_map_util_translate_filter_status_bt2mbt(bt_map_filter_status_enum from);
MBT_MAP_PRI_STATUS btmtk_map_util_translate_filter_priority_bt2mbt(bt_map_filter_prio_enum from);
MBT_MAP_MSG_CHARSET btmtk_map_util_translate_charset_bt2mbt(bt_map_charset_enum from);
MBT_MAP_FRAC_REQ btmtk_map_util_translate_fraction_req_bt2mbt(bt_map_fraction_req_enum from);
bt_map_fraction_rsp_enum btmtk_map_util_translate_fraction_rsp_mbt2bt(MBT_MAP_FRAC_DELIVER from);
MBT_MAP_TRANSP_TYPE btmtk_map_util_translate_transparent_bt2mbt(bt_map_sent_op_enum from);
MBT_MAP_MAS_STATUS *btmtk_map_util_search_mas_instance(MBT_BYTE instance);
S8 btmtk_map_util_search_mas_instance_index(MBT_BYTE instance);
MBT_MAP_MAS_INFO *btmtk_map_util_search_mas_instance_by_name(MBT_CHAR *name);
MBT_MAP_MAS_STATUS *btmtk_map_util_search_unregistered_mas_instance();
MBT_MAP_MAS_STATUS *btmtk_map_util_search_mas_client(MBT_BYTE mas_index, MBT_BDADDR addr);
S8 btmtk_map_util_search_mas_client_index(MBT_BYTE mas_instance, MBT_BDADDR addr);
MBT_MAP_MNS_STATUS *btmtk_map_util_search_mns(MBT_BDADDR addr);
S8 btmtk_map_util_search_mns_index(MBT_BDADDR addr);
MBT_MAP_MNS_STATUS *btmtk_map_util_new_mns(void);
S8 btmtk_map_util_new_mns_index(void);
S8 btmtk_map_util_get_connection_num(MBT_BDADDR addr);
MBT_CHAR *btmtk_map_util_get_virtual_folder_path(MBT_CHAR *physical_path, MBT_CHAR *physical_root_path);
BT_BOOL btmtk_map_util_buffer2file(const char *path, char *buffer, U32 size);
BT_BOOL btmtk_map_util_file2buffer(const char *path, char *buffer, U32 size);
BT_BOOL btmtk_map_util_file_copy(const char *to, const char *from, U32 from_size);
U64 btmtk_map_util_str2ull(char *str);
U64 btmtk_map_util_str2xll(char *str);


#endif  /* __BT_MAP_UTIL_H__ */
