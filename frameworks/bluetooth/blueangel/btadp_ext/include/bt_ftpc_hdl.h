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
 * bt_ftpc_hdl.h
 *
 * Project:
 * --------
 *   
 *
 * Description:
 * ------------
 *   Handle msg of FTP Client
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
#ifndef __BT_FTPC_HDL_H__
#define __BT_FTPC_HDL_H__

typedef struct
{
    //bt_ftp_file_writer_struct folder_content;      /* parsing xml has been move to MMI */
    U32 *xml_parser_p; //TODO XML_PARSER_STRUCT *xml_parser_p;                /* xml parser for parsing folder content */
    //bt_ftp_obj_entity_struct obj_entity;           /* temp buffer for receiving object */
    bt_ftpc_state_enum ftpc_state;                 /* ftpc state */
    U32 fh_ftpc_recv;                         /* fh for receiving file */
    U32 fh_ftpc_push;                          /* fh for pushing file */
    U32 ftpc_total_len;                         /* total length of a object */
    U8* ftpc_obj_buff;                                /* ftpc obj memory buffer */
    U32 ftpc_obj_buff_size;                         /* buffer size */
    U32 ftpc_peer_mru;                              /* peer mru from connect_cnf @see goep_connect_cnf_struct */
    U8  ftpc_remote_srm; /* remote peer default srm mode */
    U8  ftpc_conntype;   /* connect type rfcomm or l2cap */
    U32 ftpc_remote_mtu;   /* connect type rfcomm or l2cap */
    U32 ftpc_reamin_len;                        /* remains length of a object */
    U16 entity_cnt_cur_folder;                      /* entity count in current folder */
    U16 browse_depth;                               /* depth from root folder */
    goep_bd_addr_struct ftpc_bt_device;             /* saving bt device address */
    U8 folder_obj[FTPC_FOLDER_OBJ_PATH_SIZE];       /* should > 2 * "Z:\\@ftp\\folder_object_%d"  the folder list path */
    U16 ftpc_obj_name[FMGR_MAX_PATH_LEN];    /* push file's name and apth */
    U16 push_local_path[FMGR_MAX_PATH_LEN];    /* push file's name and apth */
    U8 ftpc_filepath[FMGR_MAX_PATH_LEN *ENCODING_LENGTH]; /* pull file's filepath */
    U32 ftpc_push_remain;                             /* push file's remain size */
    U8 total_len_type;                              /* flag for display receiving: no zero total len, zero total len, first pkt not arrived */
    U8 got_root;                                    /* flag for root folder content */
    U8 tmpname_fcnt;                                /* the fcnt of  FtpRecv00.tmp (e.g. 00)*/
    U32 ftpc_data_len;                             /* only save received length of a file, not folder-content file */
    U8 flag_abort_req_sent;                                /* have send abort_req*/
    U8 inbuf_pkt_error;                              /* BT_TRUE: when write the pkt in internal buffer, error happens*/
    U8* ftpc_obj_int_buff;                                /* 2 buffer design: this is internal buffer, ftpc_obj_buff is external buf*/
    U32 inbuf_len;
    BT_BOOL discon_from_cm;                           /*true: disconnect req is sent from cm, call disconn_cnf*/
    NBUFFER_MEM_POLL ftpc_mempool;                 /* memory pool for enhancing read&write */
    FS_STATUS ftpc_fs_status;                      /* read&write status */
    U8 ftpc_use_nbuffer;
    U8  realm[GOEP_MAX_REALM_SIZE];
    U8  challeng_options;
} bt_ftpc_conn_cntx_struct;

typedef struct
{
    U8 goep_conn_id[FTP_MAX_SERVER_SUPPORT];    /* goep connection id */
    U32 g_conn_id[FTP_MAX_SERVER_SUPPORT];      /* global connection id */
    U8 req_id[FTP_MAX_SERVER_SUPPORT];          /* req from CM id */
    bt_ftpc_conn_cntx_struct conn_cntx[FTP_MAX_SERVER_SUPPORT];        /* connection cntx */
    U32 ftpc_flag;                               /* bit-wise flag for FS error summary */
    U8 curr_idx;                                 /* save the current active connection */
    U8 enable;                                   /* active client */
} bt_ftpc_cntx_struct;

/* for saving unparsed folder content */
#define FTPC_MAKE_FOLDER_OBJECT(x, i) \
        ext_ucs2ncpy((S8*) x, (const S8*) L"/data/@btmtk/ftpc_folder_obj.xml", 60);
	//ext_ucs2ncpy((S8*) x, (const S8*) L"/data/data/com.mediatek.bluetooth/ftpc_folder_obj.xml", 60);
// #define FTPC_MAKE_FOLDER_OBJECT(x, i)    ext_ucs2ncpy((U8*)x, (const U8*) L"d:\\temp\\ftp\\folder_obj_X.txt", 40);
//kal_wsprintf( (U16 *)x, "Z:\\@ftp\\folder_object_%d", i);

/// INIT and Event handler
void bt_ftpc_cntx_init(void);

/// TODO: catagory this
U32 bt_ftpc_find_goep_id(U8 index);
void bt_ftpc_set_fs_flag(S32 ret);
void bt_ftpc_reset_abort_req_sent_flag(void);
void bt_ftpc_set_abort_req_sent_flag(void);
//int bt_ftpc_intbuf_write_obj_continue(U32 i);

/// Manipulate FTPC context
BT_BOOL bt_ftpc_start_push_a_file_routine(const U8 *ucFolderpath, const U8 *ucFilename);
BT_BOOL bt_ftpc_delete_folder_routine(const U16 *l_u2FolderName);
void bt_ftpc_pushing_file_routine(void *msg);
BT_BOOL bt_ftpc_start_get_folder_content_routine(U8 index);
BT_BOOL bt_ftpc_create_folder_routine(const U16 * l_u2FolderName);
void bt_ftpc_connection_terminated(U8 index, U8 role);


/// Handler
void btmtk_ftpc_connect_cnf_handler(void *msg);
void btmtk_ftpc_receiving_aborted_routine(U32 i);
void btmtk_ftpc_internal_rw_handler(void *data);

/// Message
void bt_ftpc_send_abort_message(U8 goep_conn_id);
void bt_ftpc_send_disconnect_message(void);
void bt_ftpc_send_tpdisconnect_message(U32 i);
void bt_ftpc_send_auth_rsp_msg(U8 goep_conn_id,const U8 *ucUserId,const U8 *ucPwd, U32 u4PwdLen);
void bt_ftpc_send_auth_req_msg(U8 goep_conn_id,const U8 *realm,const U8 *ucPwd, U32 u4PwdLen);

#endif
