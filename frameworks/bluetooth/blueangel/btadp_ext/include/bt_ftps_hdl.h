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
 * bt_ftps_hdl.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   Handle server msg functions
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
#ifndef __BT_FTPS_HDL_H__
#define __BT_FTPS_HDL_H__
#include "bluetooth_ftps_struct.h"
#include "bt_goep_fs.h"

#define FTPS_FOLDER_CONTENT_PATH_SIZE    (64)    /* should > 2 * "Z:\\@ftp\\folder_content_%d" */

typedef struct
{
    U32 fd;                               /* file handle */
    U32 buflen;                          /* buffer len used */    
    U8 buf[FTPS_MAX_FILE_WRITER_BUF_SIZE]; /* buffer */
    U8 delimitor;
    U32 written;                         /* written len */
    bt_ftp_find_struct *finddata;      /* folder data */
} bt_ftp_file_writer_struct;

typedef struct
{
    bt_ftp_file_writer_struct xml_composer;                /* for generating folder listing object composer */
    U8 dev_name[GOEP_MAX_DEV_NAME];                         /* saving bt device name (utf-8) */
    btbm_bd_addr_t bdaddr;
    U8 current_folder[BTMTK_GOEP_MAX_PATH_LEN * 2];         /* saving current folder */
    U8 new_folder[BTMTK_GOEP_MAX_PATH_LEN * 2];             /* saving current folder */
    U8 ftps_obj_name[BTMTK_GOEP_MAX_OBJ_NAME_LEN * 2];      /* saving the obj name */
    U8 ftps_filepath[BTMTK_GOEP_MAX_PATH_LEN * 2];          /* sending or pulling's filepath */
    U8 folder_content[FTPS_FOLDER_CONTENT_PATH_SIZE];       /* should > 2 * "Z:\\@ftp\\folder_content_%d" */

    U32 send_obex_pkt_size;                                 /* peer Max packet size */
    U32 total_send_obj_len;                                 /* total length of a object */
    U32 remain_send_obj_len;                                /* remains length of a object */
    bt_ftps_obj_type_enum send_obj_type;                     /* save the obj_type in sending */
    bt_ftps_state_enum ftps_state;                          /* ftps state */
    goep_bd_addr_struct ftps_bt_device;                     /* saving bt device address */
    U32 fh_ftps_send;                                   /* fh for sending file */
    U32 fh_ftps_recv;                                   /* fh for receiving file */

    U32 ftps_obj_buff_size;                                               /* ftps buffer size */    
    U8 *ftps_obj_buff;                                                        /* ftps obj memory buffer */
    U8 temp_pushing_file[BTMTK_GOEP_MAX_PATH_LEN * 2];            /* the file that can not fs_close when pushing error happen*/
    BT_BOOL ftps_fh_check_timer_set;
    BT_BOOL dis_from_user;                                       /* false, not from cm, call disconnect_ind*/
    U8 ftps_inbuf_pkt_error;                                   /* BT_TRUE: when write the pkt in internal buffer, error happens*/

    U16                          ftps_browse_depth;                                  //indicate the depth of client browsed, root: 0
    U8                           last_pkttype;
    U32                          last_pktlen;
    BT_FTPS_AUTO_BITMASK         ftps_auto_mask;
    NBUFFER_MEM_POLL ftps_mempool;                 /* memory pool for enhancing read&write */
    S32 ftps_fs_status;                      /* read&write file result */    
    U8  realm[GOEP_MAX_REALM_SIZE];
    U8  challeng_options;
    U8  bConnType;                           /* rfcomm or l2cap */
    U8 flag_abort_req_sent;                                /* have send abort_req*/
} bt_ftps_conn_cntx_struct;

typedef struct
{
    U8 goep_conn_id[FTP_MAX_CLIENT_SUPPORT];            /* goep connection id */
    U32 g_conn_id[FTP_MAX_CLIENT_SUPPORT];              /* global connection id */
    bt_ftp_access_right_enum access_right;             /* FTPS access right setting */
    bt_ftps_conn_cntx_struct conn_cntx[FTP_MAX_CLIENT_SUPPORT];        /* connection cntx */
    U8 root_folder[BTMTK_GOEP_MAX_PATH_LEN *2]; /* saving root folder */
    U8 cur_goep_conn_id;                                /* save active goep_conn_id for authorizing */
    U32 ftps_flag;                                       /* bit-wise flag for FS error summary */
} bt_ftps_cntx_struct;

#ifndef min
#define min(a,b)            (((a) < (b)) ? (a) : (b))
#endif

#define BT_FTPS_STATE_TRANS(i, x)                                                              \
{                                                                                               \
    (act_server_cntx_p+i)->ftps_state = (x);                                                    \
}

#define FTPS_MEMSET(x,y)  btmtk_os_memset(x, 0, y)
 

U32 bt_ftps_find_goep_id(U8 index); //@deprecated
void bt_ftps_set_fs_flag(int ret); //keep the original fs error and pass it to upper

/// FTPS global setting
BT_BOOL bt_ftps_is_auto(BT_FTPS_AUTO_BITMASK action);

/// Active Server's Metthod
void bt_ftps_delete_pushing_file(U32 i);
const U8* bt_ftps_get_current_folder(U8 i);
void bt_ftps_delete_pushing_file(U32 i);
void bt_ftps_set_parent_folder(U32 i);

/// Compose a xml file
int bt_ftps_compose_folder_element( bt_ftp_file_writer_struct *composer, bt_ftp_obj_info_struct *file_info);
int bt_ftps_compose_file_element(bt_ftp_file_writer_struct *composer, bt_ftp_obj_info_struct *file_info);
int bt_ftps_compose_end_element(bt_ftp_file_writer_struct *composer);

/// MSG functions
void bt_ftps_send_push_rsp_msg(U8 goep_conn_id, U8 rsp_code); // msg
void bt_ftps_push_delete_routine_success_msg(U32 result);
void bt_ftps_push_delete_routine_success_msg(U32);
void bt_ftps_send_set_folder_rsp(U8 index, U8 ucRspCode);
void bt_ftps_send_disconnect_req_msg(U8 goep_conn_id);
void bt_ftps_send_auth_rsp_msg(U8 goep_conn_id,const U8 *ucUserId,const U8 *ucPwd, U32 u4PwdLen);
void bt_ftps_send_abortfile_req_msg(U8 goep_conn_id, U8 rsp_code);

/// Handler
void bt_ftps_event_hdlr(U32 msg_id, void* data);
void bt_ftps_usb_plugin_handler(void);
void bt_ftps_push_fh_check_timer_hdler(void);
void bt_ftps_create_folder(U8 i, goep_set_folder_ind_struct *ind);  
void bt_ftps_set_folder(U8 i, goep_set_folder_ind_struct *ind);
void btmtk_ftps_internal_rw_handler(void *data);

/// Routine
BT_BOOL bt_ftps_preprocess_pull_obj_name(U8 *ucObjName );
BT_BOOL bt_ftps_send_obj_routine( U32 u4RspCode,const U8 *ucObjPath, BT_BOOL *bMore);
BT_BOOL bt_ftps_inbuf_write_obj_continue(U32 i);
BT_BOOL bt_ftps_preprocess_pull_obj_name(U8 *ucObjName );
void bt_ftps_pull_continue_routine(U8 i, BT_BOOL *more);
void bt_ftps_push_delete_routine(U8 i, goep_push_ind_struct *ind);
void bt_ftps_push_obj(U8 i, U8 *obj_name, goep_pkt_type_enum pkg_type, U8 *frag_ptr, U32 frag_len, BT_BOOL *bMore);
void bt_ftps_write_obj_continue_routine(U8 i, goep_pkt_type_enum pkt_type, U8 *frag_ptr, U32 frag_len,BT_BOOL *bMore);


/// Internal
void bt_ftps_flush_all_nbuffer_to_file();
void bt_ftps_send_folder_content_routine(U8 index, U32 u4RspCode, const U8 *ucFilePath); //@deprecated



#endif

