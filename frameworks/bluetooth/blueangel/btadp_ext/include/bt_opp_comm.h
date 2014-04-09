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
 * bt_opp_comm.h
 *
 * Project:
 * --------
 *   Provides common structure definition of OPP Sever and Client
 *
 * Description:
 * ------------
 *
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
#ifndef __BT_OPP_COMMON_H__
#define __BT_OPP_COMMON_H__

#include "bt_adp_fs.h"
#include "bt_adp_fs_ucs2.h"
#include "ext_nbuffer.h"
#include "bluetooth_oppc_struct.h"
#include "bluetooth_opps_struct.h"

#define LOG_TAG "Bluetooth.OPP"

/*****************************************************************************
* Type Declear
*****************************************************************************/
#define BT_OPP_ABORT_REQ_TIMER          (1)  ///<TODO timer id
#define FMGR_PROG_UPDATE_TIMER          (20) ///<TODO file manager timer id
#define OPPC_PROG_UPD_DUR               (30000) ///< timeout value

#define OPP_HIDDEN_TEMP_OBJ

/************************************/
/* Predefine file name and path     */
/***********************************/
#define BT_OPP_RECV_OBJ_FILEPATH          	"\\" //TODO Default FilePath MAUI:FMGR_DEFAULT_FOLDER_RECEIVED
#define BT_OPP_RECV_OBJ_FILENAME          	"OppRecv"
#define BT_OPP_RECV_OBJ_FILEEXT           	"tmp"
#define BT_OPP_SND_DEFAULT_OBJNAME        	L"MTK.obj"
#define BT_OPP_SND_DEFAULT_OBJTYPE        	"text/plain" //"Unknown"
#define BT_OPPC_DEFAULT_VCARD_WFILENAMEL	L"tmpVcard.vcf"
#define OPPS_TMP_BUFF_SIZE                               192
#define OPPC_TMP_BUFF_SIZE                               192
#define BT_OPPC_MAX_NUM_TEMP_FILE                10
#define BT_OPPS_MAX_NUM_TEMP_FILE                10

typedef U32 FS_VOID;

#define GOPP(x)      g_bt_opp_cntx_p->x
// server point
#define GOPS(x)      g_bt_opp_cntx_p->x
// client point
#define GOPC(x)      g_bt_opp_cntx_p->x


/*****************************************************************************
* Macros
*****************************************************************************/
/* MMI usb context bit-wise flag defination */
#define BT_OPP_MASK_FOLDER_ERR      0x00000001
#define BT_OPP_MASK_FS_SANITY       0x00000002
#define BT_OPP_MASK_WRITE_FILE_FAIL 0x00000004
#define BT_OPP_MASK_DISK_FULL       0x00000008
#define BT_OPP_MASK_ROOT_DIR_FULL   0x00000010
#define BT_OPP_MASK_RESERVED_4      0x00000020
#define BT_OPP_MASK_RESERVED_5      0x00000040
#define BT_OPP_MASK_RESERVED_6      0x00000080
#define BT_OPP_MASK_ABORT_PRESS     0x00000100
#define BT_OPP_MASK_SDP_FORMAT      0x00000200
#define BT_OPP_MASK_ICON_BLINK      0x00000400
#define BT_OPP_MASK_DEVICE_BUSY     0x00001000
#define BT_OPP_MASK_MULTI_RECEIV    0x00002000
#define BT_OPP_MASK_SHORT_NAME      0x00004000
#define BT_OPP_MASK_MEDIA_CHANGE    0x00008000
#define BT_OPP_MASK_BAD_FILE        0x00010000
#define BT_OPP_MASK_REJECT          0x00020000
#define BT_OPP_MASK_ASMSTOP         0x00040000
#define BT_OPP_MASK_USEREJECT       0x00080000
#define BT_OPP_MASK_CM_NOTIFY       0x00100000
#define BT_OPP_MASK_WRITE_PROTECT   0x00200000
#define BT_OPP_MASK_FILE_TYPE       0x00400000
#define BT_OPP_MASK_SENDING_DELE    0x00800000
#define BT_OPP_MASK_USB_PLUG_IND    0x01000000
#define BT_OPP_MASK_MULTI_SEND      0x02000000

/* MMI usb context bit-wise flag operation */
#define BT_OPP_FLAG_IS_ON(f)    ((g_bt_opp_cntx_p->flag) & f)
#define BT_OPP_SET_FLAG(f)      ((g_bt_opp_cntx_p->flag) |= f)
#define BT_OPP_RESET_FLAG(f)    ((g_bt_opp_cntx_p->flag) &= ~f)

#define BT_OPS_STATE_TRANS(s)  do {                        \
                                    GOPP(ops_state) = s;    \
                                } while (0);
                                //MMI_OPS_LOG_STATE(s);

#define BT_OPC_STATE_TRANS(s)  do {                        \
                                    GOPP(opc_state) = s;    \
                                } while (0);
                                //MMI_OPC_LOG_STATE(s);

#define BT_OPC_LOG_STATE_ERR()
#define BT_OPS_LOG_STATE_ERR()
#define BT_OPP_LOG_STATE()
#define BT_OPP_PUSH_START_TIMER  1
#define BT_OPP_ASSERT(x)

#define __BT_OPP_DEBUG__

#ifdef __BT_OPP_DEBUG__
#define BT_OPP_LOGD  bt_ext_log
#else
#define BT_OPP_LOGD(fmt, ...)
#endif

#define BT_OPP_LOGI  bt_ext_log


/*****************************************************************************
* Enum Value
*****************************************************************************/
typedef enum
{
    OPP_OBJ_ACTION_NONE,
    OPP_OBJ_ACTION_MOVE,
    OPP_OBJ_ACTION_RENAME
} opp_obj_action_enum;

/* Send result code */
typedef enum
{
    BTSEND_RET_SUCCESS,
    BTSEND_RET_FAIL,
    BTSEND_RET_INVALID_PARA,
    BTSEND_RET_SERV_NOT_AVAILABLE,
    BTSEND_RET_DEVICE_BLOCKED,
    BTSEND_RET_BAD_OBJECT,
    BTSEND_RET_USER_ABORT,
    BTSEND_RET_TIMEOUT,
    BTSEND_RET_REJECT,
    BTSEND_RET_DISCONNECT,
    BTSEND_RET_FORBIDDEN,
    BTSEND_RET_DONE,
    BTSEND_RET_UNSUPPORT_FILE_TYPE
} btsend_ret_enum;

typedef enum
{
    BT_OPP_ERR_SUCCESS,    /* Success */
    BT_OPP_ERR_USER,       /* User termanited */
    BT_OPP_ERR_TIMEOUT,    /* OPP Response time out */
    BT_OPP_ERR_BAD_FILE,   /* File operating error */
    BT_OPP_ERR_EMPTY_FILE, /* File operating error */
    BT_OPP_ERR_REJECT,     /* Reject by Server */
    BT_OPP_ERR_BIDIR,      /* Bidirection connection. Not support currently */
    BT_OPP_ERR_DISCONNECT, /* Under layer disconnect */
    BT_OPP_ERR_SENDING     /* Under layer disconnect */
} BT_OPP_SND_ERR_TYPE;


typedef enum
{
// client event
    BT_OPPC_GROUP_START         = 0,
    BT_OPPC_ENABLE_SUCCESS      = BT_OPPC_GROUP_START +  1,
    BT_OPPC_ENABLE_FAIL         = BT_OPPC_GROUP_START +  2,
    BT_OPPC_DISABLE_SUCCESS     = BT_OPPC_GROUP_START +  3,
    BT_OPPC_DISABLE_FAIL        = BT_OPPC_GROUP_START +  4,
    BT_OPPC_CONNECTED           = BT_OPPC_GROUP_START +  5,
    BT_OPPC_PROGRESS_UPDATE     = BT_OPPC_GROUP_START +  6,
    BT_OPPC_PUSH_START          = BT_OPPC_GROUP_START +  7,
    BT_OPPC_PUSH_SUCCESS        = BT_OPPC_GROUP_START +  8,
    BT_OPPC_PUSH_FAIL           = BT_OPPC_GROUP_START +  9,
    BT_OPPC_PULL_START          = BT_OPPC_GROUP_START + 10,
    BT_OPPC_PULL_SUCCESS        = BT_OPPC_GROUP_START + 11,
    BT_OPPC_PULL_FAIL           = BT_OPPC_GROUP_START + 12,
    BT_OPPC_EXCH_START          = BT_OPPC_GROUP_START + 13,
    BT_OPPC_EXCH_SUCCESS        = BT_OPPC_GROUP_START + 14,
    BT_OPPC_EXCH_FAIL           = BT_OPPC_GROUP_START + 15,
    BT_OPPC_DISCONNECT          = BT_OPPC_GROUP_START + 16,
    BT_OPPC_GROUP_END           = BT_OPPC_GROUP_START + 30,

// server event
	BT_OPPS_GROUP_START			= 100,
	// CNF
	BT_OPPS_ENABLE_SUCCESS		= BT_OPPS_GROUP_START +  1,
	BT_OPPS_ENABLE_FAIL			= BT_OPPS_GROUP_START +  2,
	BT_OPPS_DISABLE_SUCCESS		= BT_OPPS_GROUP_START +  3,
	BT_OPPS_DISABLE_FAIL		= BT_OPPS_GROUP_START +  4,
	BT_OPPS_PROGRESS_UPDATE		= BT_OPPS_GROUP_START +  5,
	BT_OPPS_PUSH_START			= BT_OPPS_GROUP_START +  6,
	BT_OPPS_PUSH_SUCCESS		= BT_OPPS_GROUP_START +  7,
	BT_OPPS_PUSH_FAIL			= BT_OPPS_GROUP_START +  8,
	BT_OPPS_PULL_START			= BT_OPPS_GROUP_START +  9,
	BT_OPPS_PULL_SUCCESS		= BT_OPPS_GROUP_START + 10,
	BT_OPPS_PULL_FAIL			= BT_OPPS_GROUP_START + 11,
	BT_OPPS_DISCONNECT			= BT_OPPS_GROUP_START + 12,
	// IND
	BT_OPPS_PUSH_ACCESS_REQUEST	= BT_OPPS_GROUP_START + 13,	// bdaddr / object-name / mime-type / size
	BT_OPPS_PULL_ACCESS_REQUEST	= BT_OPPS_GROUP_START + 14,
	BT_OPPS_GROUP_END			= BT_OPPS_GROUP_START + 30

} bt_opp_ui_event;

/*****************************************************************************
* Structure
*****************************************************************************/
typedef struct
{
    U8 req_id;
    U32 flag;
    U8 recv_drv;
    U16 recv_path[OPP_MAX_PATH_LENGTH+1];
    U8 dev_name[GOEP_MAX_DEV_NAME];
    BT_BOOL ops_disconnflag;
    BT_BOOL opc_disconnflag;

#ifdef BTMTK_ON_LINUX
	int apisock;	// REQ (sned) + RSP (send)
	int servsock;	// CNF (recv) + IND (recv)
#endif

    /* OPS */
    btbm_bd_addr_t ops_dev_addr;
    BT_OPS_STATE ops_state;
    FHANDLE h_ops_recv;             ///< file handle for server to receive file (push feature)
    FHANDLE h_ops_snd;            ///< file handle for server to send vcard (pull feature)

    BT_OPPS_AUTO_BITMASK ops_auto_accept; ///< auto answer the remote incoming indication. @see BT_OPS_AUTO_BITMASK
    U8 ops_support_feature;    ///<  The support feeature. Exchange mask is composed by push and pull bitmask @see BT_OPC_FEATURE
    U8  ops_goep_conn_id;
    U8  ops_goep_req_id;       ///< increase number for obex instruction
    U32 ops_cm_conn_id;
    S8 ops_obj_path[OPP_MAX_PATH_LENGTH+1];      ///<  the opened/open folder
    U32 ops_push_buff_size;    ///< sizeof ops_push_buff
    U8 *ops_push_buff;         ///< [OPPS_MAX_OBEX_PACKET_LENGTH];
    U16 ops_obj_name[OPP_MAX_OBJ_NAME_LENGTH/sizeof(U16)]; ///< obj name in/out in obex name field
    S8 ops_obj_mime[OPP_MAX_OBJ_MIME_LENGTH]; ///< obj mime type in/out in objex mime field
    U32 ops_pkt_type;          ///< the last packet type of pushing or pulling
    U32 ops_pkt_len;           ///< the last packet data length
    U32 ops_total_obj_len;     ///<
    U32 ops_remain_put_len;    ///< remain size

    U8 ops_mem_mode;
    U8 *ops_snd_buffer;        ///< memory method: MMI  provides buffer to pushing or pulling
    U32 ops_snd_maxsize;       ///< memory method: The buffer's available length
    U32 ops_snd_offset;        ///< memory method: pushing  offset
    U8 *ops_recv_buffer;       ///< memory method: MMI provides buffer to accept pull
    U32 ops_recv_maxsize;      ///< memory method:
    U32 ops_recv_offset;       ///< memory method: pulling offset
    U32 ops_push_acceptsize; ///< check the acceptsize when client pushs data. 0 is unlimit.

    NBUFFER_MEM_POLL ops_mempool;   ///< n-buffer for performance of opp
    FS_STATUS ops_fs_status;        ///< keep the last fs status error
    U32 ops_u4RecvSize;             ///< Receive file's size
    U8 stop_recv;                   ///< flag for stop sending or receiving
    U8 ops_use_nbuffer;
    U32 ops_mtu;

    /* OPC */
    BT_OPC_STATE opc_state;
    BT_OPC_FEATURE opc_feature;
    FHANDLE h_opc_snd;            ///< file handle for sending file (push feature)
    FHANDLE h_opc_recv;       ///< file handle for pulling file (pull feature)
    U8 opc_goep_conn_id;
    U8 opc_goep_req_id;
    U32 opc_cm_conn_id;
    U32 opc_push_buff_size;         ///< size of opc_push_buffs
    U8 *opc_push_buff;              ///< opc buffer for push and pull data
    btbm_bd_addr_t opc_dev_addr;    ///< OPP server data
    S8 opc_obj_path[OPP_MAX_PATH_LENGTH+1];         ///<  the open filepath (absolute path)
    U16 opc_obj_name[OPP_MAX_OBJ_NAME_LENGTH/sizeof(U16)];
    S8 opc_obj_mime[OPP_MAX_OBJ_MIME_LENGTH];
    S8 opc_pull_path[OPP_MAX_PATH_LENGTH+1];        ///<  the pull filepath (absolute path)
    U16 opc_pull_name[OPP_MAX_OBJ_NAME_LENGTH/sizeof(U16)];
    S8 opc_pull_mime[OPP_MAX_OBJ_MIME_LENGTH];
    U32 opc_mtu;
    U32 total_obj_len;      ///< current push/pull object's total len
    U32 remain_put_len;     ///< current remian len of push/pull object
    BT_BOOL isAbort;        ///< refer to OBEX spec, before we send abort opeartion, we need to wait for push_cnf or pull_cnf
    NBUFFER_MEM_POLL opc_mempool;
    FS_STATUS opc_fs_status;
    U8 *opc_snd_mem;
    U32 opc_snd_mem_size;
    U32 opc_snd_mem_offset;
    U8 opc_is_pushmem;
    U8 *opc_recv_mem;
    U32 opc_recv_mem_size;
    U32 opc_recv_mem_offset;
    U32 opc_pull_acceptsize;  ///< check the pull size when server provides data. 0 is unlimit.
    U8 opc_is_pullmem;
    U8 opc_use_nbuffer;
} bt_opp_context_struct;

typedef enum
{
    OPP_FILE_GET_NAME,  /* Extract file name */
    OPP_FILE_GET_EXT,   /* Extract file extension */
    OPP_FILE_GET_NAME_VALID,
    OPP_FILE_GET_LAST
} BT_OPP_GET_FILE_ENUM;

/*****************************************************************************
* Export Functions
*****************************************************************************/
/* Export Variables */
extern bt_opp_context_struct g_bt_opp_cntx;
extern bt_opp_context_struct *const g_bt_opp_cntx_p; ///< client and server use the same context

/* OPP Init and event handle function */
void bt_opp_init(void); ///< Init the oppc and opps. Should be invoked before using OPP
void bt_opp_event_hdlr(U32 msg_id, void* data); ///< Handle the msg for opp client or opp server


/* Utilities */
const S8 *getOPPSState(U32 state);
const S8 *getOPPCState(U32 state);
const S8 *getOPPFeature(U32 feature);
typedef void (*BTMTK_OPP_CALLBACK)(U32 u4OPPUIEvent, U8* ucdata);
typedef void (*BTMTK_OPP_JNI_CALLBACK)(U8 event, S8* parameters[], U8 count);

void btmtk_oppc_ui_notify(U32 u4OPPUIEvent, U8* ucdata);
void btmtk_opps_ui_notify(U32 u4OPPUIEvent, U8* ucdata);

// API for JNI
BT_BOOL btmtk_opp_set_jni_callback(BTMTK_OPP_JNI_CALLBACK pCallback);
void btmtk_opp_client_enable();
void btmtk_opp_client_disable();
void btmtk_opp_client_connect(bt_addr_struct *destAddr);
void btmtk_opp_client_push(const char *mimeType, const char *objectName, const char *filename);
void btmtk_opp_client_pushobject(bt_addr_struct *destAddr, const char *mimeType, const char *objectName, const char *filename);
void btmtk_opp_client_abort();
void btmtk_opp_client_disconnect();
void btmtk_opp_server_enable();
void btmtk_opp_server_disable();
void btmtk_opp_server_access_response(U8 goepStatus, const S8* parameters[]);
void btmtk_opp_server_disconnect();

// API for ASHM
void* btmtk_oppc_get_ashm_buffer();
void btmtk_oppc_return_ashm_buffer();
void* btmtk_opps_get_ashm_buffer();
void btmtk_opps_return_ashm_buffer();

#endif
