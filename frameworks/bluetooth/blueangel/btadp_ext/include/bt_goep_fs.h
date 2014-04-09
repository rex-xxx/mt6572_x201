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
 * bt_goep_fs.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   Handle goep-based opp & ftp file access. Extend the basic file I/O functions
 *
 * Author:
 * -------
 * Daylong Chen
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BT_GOEP_FS_H__
#define __BT_GOEP_FS_H__

#include "ext_osal.h"

#define BTMTK_EP_MAX_FILENAME_LEN              260
#define BTMTK_EP_MAX_FOLDERPATH_LEN         260
#define BTMTK_EP_MAX_FILEPATH_LEN              (BTMTK_EP_MAX_FOLDERPATH_LEN+BTMTK_EP_MAX_FILENAME_LEN)

typedef struct
{
    U8 file_name[BTMTK_EP_MAX_FILENAME_LEN * 2]; //ucs2
    U32 name_len;
    U32 file_size;
    U16 year;
    U8 month;
    U8 day;
    U8 hour;
    U8 min;
    U8 sec;
    BT_BOOL isFile;
} bt_ftp_obj_info_struct;

typedef struct {
    U32 hFile;
}bt_ftp_find_struct;

/// Calculate the 
extern void btmtk_goep_sleep_test(U32 interval); /// debugger -> bt_goep_porting.h

/// TEMP file utility
extern FS_STATUS bt_goep_clean_tempfile(FHANDLE fh ,U8 *FilePath, BT_BOOL close_only);

// Porting: filesystem manipulate
extern FS_STATUS btmtk_goep_delete_file(const U16 *pwsFilename); // opp & ftp use
extern U32 btmtk_goep_compose_filepath(const U8 *Folder,const U8 *Filename, U8 *FilePath, U32 u4MaxSize);
extern U32 btmtk_goep_compose_folderpath(const U8 *Folder1,const U8 *Folder2, U8 *FilePath, U32 u4MaxSize);
extern FS_STATUS btmtk_goep_get_file_basename(const U8 *filepath, U8 *basename, U32 u4MaxSize);  //move to goep porting
extern S8 * bt_goep_get_file_name_ext(S8 *name, U32 type);

/// Export for writing. Low Level API
extern FHANDLE btmtk_goep_open_wstream(const U16 *pwsFilename, U32 i4Attr); ///< Open a read stream with attribute setting
extern FS_STATUS btmtk_goep_write_wstream(FHANDLE hFile, void *pData, S32 i4Length, S32 *pi4Written); ///< Write data to a write-stream
extern FS_STATUS btmtk_goep_create_folder(const U16 *pwsFilePath); ///< FS_CreateDir(). If folder has been existed, return ok
extern FS_STATUS btmtk_goep_rename_file(const U16 *pwsOldFilename, const U16 *pwsNewFilename); ///< FS_Rename
extern FS_STATUS btmtk_goep_delete_file(const U16 *pwsFilename);///<FS_Delete(old_name);
extern FS_STATUS btmtk_goep_move_file(const U16 *pwsFilename,const  U16 *pwsNewFilename,  U16 Flag);///<FS_MOVE
extern FS_STATUS btmtk_goep_hide_file(const U16 *pwsFilename);///< FS_GetAttributes and mark the hide bit
extern FS_STATUS btmtk_goep_unhide_file(const U16 *pwsFilename);///< FS_GetAttributes and unmakr the hide bit
extern BT_BOOL btmtk_goep_is_file_exist(const U16 *pwsFilename); ///
extern BT_BOOL btmtk_goep_is_folder_exist(const U16 *pwsFilename); ///
extern FS_STATUS btmtk_goep_delete_folder(const U16 *pwsFilename);


/// Export for reading. Low Level API
extern FHANDLE btmtk_goep_open_rstream(const U16 *pwsFilename, U32 i4Attr); ///< Open a read stream with attribute setting
extern BT_BOOL btmtk_goep_is_valid_handle(FHANDLE hFile); ///< Is a valid File Handle 
extern BT_BOOL btmtk_goep_get_filesize(FHANDLE hFile, U8 *filepath, U32 *len); ///< Get a filesize by a file handle
extern BT_BOOL btmtk_goep_close_rstream(FHANDLE hFile); ///< Close a file handle
extern BT_BOOL btmtk_goep_close_wstream(FHANDLE hFile); ///< Close a file handle
extern FS_STATUS btmtk_goep_read_rstream(FHANDLE hFile, void *pData, S32 i4Length, S32 *pi4Read); ///< Read data from a read-stream
extern BT_BOOL btmtk_goep_isvalid_read(FS_STATUS status); ///< Read success or not. Note: EOF is not a valid read ! read 0 byte is not a valid read!
extern BT_BOOL btmtk_goep_is_eof_stream(FS_STATUS status); ///< Reach EOF. 

/// FilePath manage
extern U32 btmtk_goep_compose_filepath(const U8 *Folder, const U8 *Filename, U8 *FilePath, U32 u4MaxSize);
extern U32 btmtk_goep_compose_folderpath(const U8 *Folder1,const U8 *Folder2, U8 *FilePath, U32 u4MaxSize);
extern BT_BOOL btmtk_goep_get_private_filepath(U8 type, const U8 *ucFilename, U8 *ucOutFilepath, U32 u4MaxLen);
extern BT_BOOL btmtk_goep_clear_filepath(U8 *filepath);
extern S32 btmtk_goep_get_file_basename(const U8 *filepath, U8 *basename, U32 u4MaxSize);
extern void bt_goep_truncate_ntoh_filename(U16 *dst_name, const U16 *src_name, U16 max_dst_len);
extern U32 btmtk_goep_gen_temp_filename(U32 count,const U8 *OrigName, U8 *NewName, U32 u4MaxSize);

/// Find files in a folder 
extern S32 btmtk_goep_fs_findend(void *ptr);
extern S32 btmtk_goep_fs_findnext(void *ptr , bt_ftp_obj_info_struct *ftp_file_info);
extern S32 btmtk_goep_get_fileinfo( bt_ftp_obj_info_struct *info);
extern S32 btmtk_goep_fs_findfirst(U8 *ucFolderPath, bt_ftp_find_struct **findstruct, bt_ftp_obj_info_struct *ftp_file_info);


#endif
