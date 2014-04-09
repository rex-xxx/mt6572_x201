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

/*****************************************************************************
 *
 * Filename:
 * ---------
 * bt_ftp_porting.h
 *
 * Project:
 * --------
 *   MMI
 *
 * Description:
 * ------------
 *   
 *
 * Author:
 * -------
 * Daylong 
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/

#ifndef __BT_FTP_PORTNIG_H__
#define __BT_FTP_PORTNIG_H__

#include "bluetooth_ftps_struct.h"
#include "bluetooth_ftpc_struct.h"

#define FTPC_CS_INIT()       btmtk_ftpc_init_cs()
#define FTPC_CS_ENTER()      btmtk_ftpc_enter_cs()
#define FTPC_CS_LEAVE()      btmtk_ftpc_leave_cs()
#define FTPS_CS_INIT()       btmtk_ftps_init_cs()
#define FTPS_CS_ENTER()      btmtk_ftps_enter_cs()
#define FTPS_CS_LEAVE()      btmtk_ftps_leave_cs()

#ifdef BTMTK_ON_LINUX
#define	FTP_FS_PATH_DELIMITOR	L"/"
#else
#define FTP_FS_PATH_DELIMITOR	L"\\"
#endif
#define FTP_SDCARD				"sdcard"

#define FTPC_AUTO_CREATE_BACK_FOLDER 1
#define MSG_ID_BT_FTP_TERMINATE_SERVICE -1

void btmtk_ftpc_init_cs();
void btmtk_ftpc_enter_cs();
void btmtk_ftpc_leave_cs();
void btmtk_ftps_init_cs();
void btmtk_ftps_enter_cs();
void btmtk_ftps_leave_cs();

/// FTPS
U8 *btmtk_ftps_get_profile_shared_int_buffer(U32 u4Size);
BT_BOOL btmtk_get_default_root_folder_path(U8 *path, U32 u4MaxLen);
void btmtk_ftps_create_tmp_objname(U8 *name, U32 u4Random );
BT_BOOL btmtk_ftps_get_xml_filepath(U32 index, U8 *filepath, U32 u4MaxSize);

/// FTPS: configure methods
BT_BOOL btmtk_ftps_reset_read_only_permission();
BT_BOOL btmtk_ftps_setup_read_only(BT_BOOL read_only, BT_BOOL *result);
BT_BOOL btmtk_ftps_is_read_only();
BT_BOOL btmtk_ftps_is_allow_create_folder(const U16 *ucFolderPath);
BT_BOOL btmtk_ftps_force_clear();
BT_BOOL btmtk_ftps_is_sdcard(const U16 *ucPath);

/// FTPC
U8 *btmtk_ftpc_get_profile_shared_int_buffer(U32 u4Size);
BT_BOOL btmtk_ftpc_auto_get_listing_xml(void); /// is auto get the listing xml after ftpc obex-functions
const U8 *btmtk_ftpc_get_default_receive_filepath();
S32 btmtk_ftpc_get_push_progress(long long *c, long long *t);
S32 btmtk_ftpc_get_pull_progress(long long *c, long long *t);
U32 btmtk_ftpc_get_current_filesize();
U32 btmtk_ftps_get_current_filesize();
BT_BOOL btmtk_ftpc_force_clear();

/// Internal ftp utilities
extern BT_BOOL btmtk_ftp_delete_file(const U16 *filename);
extern BT_BOOL btmtk_ftp_delete_folder(const U16 *foldername);
extern BT_BOOL btmtk_ftp_util_get_parent_folder(const U8 *ucFolderpath, U8 *ucParent, U32 maxSize);
extern void bt_ftp_util_delete_file(const U8* absolute_path, const U8* file_name);
extern void bt_ftp_ucs2_cut_name_with_ext(U8 *dest, U32  dest_size, const U8 *src);
extern void bt_ftp_ucs2_cut_name_without_ext(U8 *dest, U32  dest_size, const U8 *src);
extern void bt_ftp_ucs2_htons(U8 *h_order_name, U8 *n_order_name);
extern U32 bt_ftp_compose_filepath(const U8 *folder, const U8 *file_name, U8 *res_path, U32 u4MaxSize);
extern U32 bt_ftp_compose_folderpath(const U8 *folder1,const U8 *folder2, U8 *res_path, U32 u4MaxSize);
extern U32 bt_ftp_compose_path(const U8 *base, const U8 *target, U8 *res_path, U32 u4MaxSize);

/// Message
extern int bt_ftp_init_socket();
extern int bt_ftp_clear_socket();
extern void bt_ftp_send_msg(U32 msg_id, void *p_local_para, U32 u4Size);
extern void bt_ftp_send_msg_toself(U32 msg_id, void *p_local_para, U32 u4Size);

/// Exported message handler
extern void btmtk_ftps_handle_message(ilm_struct* message);
extern void btmtk_ftpc_handle_message(ilm_struct* message);

/// Debug
extern const char *btmtk_ftpc_get_event_name(U32 u4Event);
extern const char *btmtk_ftps_get_event_name(U32 u4Event);

/// Shared memory functions
extern void* btmtk_ftpc_get_ashm_buffer();
extern void btmtk_ftpc_return_ashm_buffer();
extern void* btmtk_ftps_get_ashm_buffer();
extern void btmtk_ftps_return_ashm_buffer();

extern S32 btmtk_ftpc_get_conntype();
extern S32 btmtk_ftpc_get_mru();
extern S32 btmtk_ftpc_get_srm();
#endif
