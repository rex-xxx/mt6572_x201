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
 * Filename:
 * ---------
 *  jbt_internal.h
 *
 * Project:
 * --------
 *  MAUI
 *
 * Description:
 * ------------
 *  
 *
 * Author:
 * -------
 *  
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!! 
 *------------------------------------------------------------------------------
 * $Log$
 *
 * Jul 11 2009 mtk02126
 * [MAUI_01703270] Modify traces
 * 
 *
 * Mar 11 2009 mtk02126
 * [MAUI_01644724] [JSR82] bt_jsr82_HandleSdpReq
 * 
 *
 * Oct 21 2008 mtk01411
 * [MAUI_01259338] [JSR82] Update System Abnormal Reset Callback timing in JBT
 * 
 *
 * Oct 17 2008 mtk01624
 * [MAUI_00780660] fix daily build error
 * 
 *
 * Sep 22 2008 mtk01411
 * [MAUI_01241533] [JSR82] Enable ADM management in JBT
 * 
 *
 * Sep 4 2008 mtk01411
 * [MAUI_01233160] [JSR82] Change jbt_malloc to jvm_malloc directly in jbt files
 * 
 *
 * Apr 13 2008 mtk01411
 * [MAUI_00755198] [JSR82] Update JSR82 codes
 * 
 *
 * Apr 4 2008 mtk01411
 * [MAUI_00750480] [JSR82] Remove two function prototype to header file jbt_interface
 * 
 *
 * Apr 4 2008 mtk01411
 * [MAUI_00750480] [JSR82] Remove two function prototype to header file jbt_interface
 * 
 *
 * Apr 1 2008 mtk01411
 * [MAUI_00289832] [JSR82] Assert fail: jbt_cmdhdl.c 446 - JDAEMON (remove set_cod_cmd from queue but d
 * 
 *
 * Mar 30 2008 mtk01411
 * [MAUI_00725508] JSR82_Press "Exit" and Reject a MT call,then the screen will stay at "Call End" abou
 * 
 *
 * Mar 18 2008 mtk01411
 * [MAUI_00734083] [JSR82] Add name cache mechanism in JBT
 * 
 *
 * Mar 2 2008 mtk01411
 * [MAUI_00091408] It takes more than 20 secs to Terminate Java.
 * 
 *
 * Feb 22 2008 mtk01411
 * [MAUI_00622206] [Java] Support JSR-82 on Aplix soluiton
 * 
 *
 * Aug 21 2007 mtk00727
 * [MAUI_00536361] [Java] Add BT folders
 * 
 *
 * Aug 21 2007 mtk00727
 * [MAUI_00536361] [Java] Add BT folders
 * 
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!! 
 *==============================================================================
 *******************************************************************************/

#if defined(__BT_JSR82__) || defined(__BT_JSR82_L2RF__)
#ifndef _JBT_INTERNAL_H
#define _JBT_INTERNAL_H

#if 0
#define __USE_BT_RING_BUF_API__
#endif

 #ifdef __JSR82_MAUI__
#include "bluetooth_struct.h"
#include "bluetooth_bm_struct.h"
#ifdef __USE_BT_RING_BUF_API__
#include "ext_ring.h" /* Add btstack\inc folder in jal.inc file due to struct RingBuf */
#else
#include "xatypes.h"
#endif
#include "mmiapi_bt_struct.h"
#endif

//#include "bt_common.h"
#include "ext_ring.h"
#include "bluetooth_jsr82_struct.h"
//#include "btconfig.h" /* RF_MAX_FRAME_SIZE */
#include "bt_types.h"


#define SUPPORT_JSR_82 1
#define SEND_FROM_JASYNC 1
#define JBT_UT 1
#define JBT_CACHE_DEV_NAME 1
#define JBT_RETURN_CACHE_NAME_DIRECTLY 0

#if (JBT_RETURN_CACHE_NAME_DIRECTLY == 1)
    #if (JBT_CACHE_DEV_NAME == 0)
        #error "Set JBT_CACHE_DEV_NAME as 1 first"
    #endif
#endif
/* If this value is 1, it will invoke jbt_finalize_for_jvm() to help JVM refresh its UI */
#define JBT_FINALIZE_REFRESH_UI_SUPPORT 1
#define TRACE_JBT_GROUP TRACE_GROUP_7

#ifndef JBT_MEMORY_DEBUG
#define JBT_MEMORY_DEBUG
#define JBT_SUPPORT_ADM_MEM_ENABLED 1
#define JBT_CREATE_ADM_INIT_ENABLED 1

#if JBT_SUPPORT_ADM_MEM_ENABLED
#define JBT_SUPPORT_ADM_MEM
#endif /* JBT_SUPPORT_ADM_MEM_ENABLED */

#ifdef JBT_SUPPORT_ADM_MEM
#if JBT_CREATE_ADM_INIT_ENABLED
#define JBT_CREATE_ADM_INIT
#endif /* JBT_CREATE_ADM_INIT_ENABLED */
#endif /* JBT_SUPPORT_ADM_MEM */

#endif /* JBT_MEMORY_DEBUG */

#define JBT_MAX_COMMAND_QUEUE (0x03)
#define JBT_MAX_COMMAND_NO (0x01)

#define JBT_MAX_BTSTACK_USED_BUFFER_SIZE (1024)

#define JBT_OPERATION_STATE_NONE           0x00
#define JBT_OPERATION_STATE_INQUEUE     0x01
#define JBT_OPERATION_STATE_ONGOING     0x02


#define JBT_STACK_CMD_CONCURRENT    0x00
#define JBT_STACK_CMD_NOCONCURRENT    0x01


/* Add by mtk01411: 2007-0917 */
#define JBT_LIST_TYPE_SPP_SESSION 1
#define JBT_LIST_TYPE_L2CAP_SESSION 2

/* TX/RX_CREDIT=6(ADM_MEM_SIZE=100K) TX/RX_CREDIT=24/12(ADM_MEM_SIZE=140K) */
#define JBT_RF_MAX_FRAME_SIZE 990	// This value should be synced with RF_MAX_FRAME_SIZE in btconfig.h
#define JBT_SPP_RX_CREDIT (12)
#define JBT_SPP_TX_CREDIT (24)
#define JBT_SESSION_SPP_RX_BUFSIZE (JBT_RF_MAX_FRAME_SIZE*JBT_SPP_RX_CREDIT)  
#define JBT_SESSION_SPP_TX_BUFSIZE (JBT_RF_MAX_FRAME_SIZE*JBT_SPP_TX_CREDIT)

/* Add by mtk01411: 2007-1103 */
#define JBT_SESSION_L2CAP_TX_BUFSIZE    (1024*24)
#define JBT_SESSION_L2CAP_RX_BUFSIZE    (1024*24)
/* JBT_DELAY_SDP_TICKS is depended on BT_L2CAP_DISCONNECT_TIMEOUT_500MS (108 ticks) */
#define JBT_DELAY_SDP_TICKS 120
#define JBT_RESTORE_FROM_J2ME   0x01
#define JBT_RESTORE_FROM_JASYNC   0x02

#if JBT_CACHE_DEV_NAME
#define JBT_MAX_CACHE_NAME_TABLE_SIZE 30
#define JBT_MAX_CACHE_NAME_ENTRY_LEN  256
#endif

#ifdef JBT_MEMORY_DEBUG

#if JBT_CACHE_DEV_NAME
/* Extra 30 memory blocks are left for storing device's name */
#define JBT_MAX_MEMORY_BLOCK (0x60)
#else
#define JBT_MAX_MEMORY_BLOCK (0x30)
#endif

#endif /* JBT_MEMORY_DEBU */

#if 0
#define __JBT_SESSION_LINK_LIST__
#endif

/* This compile option is used to turn on TX Testing on JSR Server Side */
#if 0 
#define __JBT_UT_SERVER_TX__
#endif

typedef void (*JbtEventCBF)(kal_uint8 opcode, kal_uint32 transaction_id, void *parm);

#define JBT_SESSION_INITIATOR_FALSE (0x00)
#define JBT_SESSION_INITIATOR_TRUE (0x01)

/* Add by mtk01411: 2007-1112 */
typedef enum
{
    JBT_SESSION_STATE_IDLE=0,
    JBT_SESSION_STATE_CONNECTING,
    JBT_SESSION_STATE_CONNECTED,
    JBT_SESSION_STATE_DISCONNECTING
}JBT_SESSION_SM;

typedef struct {
    kal_uint8   operation_state;
    kal_uint32 transaction_id;
}jbt_spp_op_info;

/* Add by mtk01411: 2007-0917 */
typedef struct{
    kal_uint8   operation_state;
    kal_uint32 transaction_id;
}jbt_session_op_info;
    
/* Add by mtk01411: 2007-0916 */
typedef struct _jbt_list{
    struct _jbt_list* Flink;
    struct _jbt_list* Blink;
}jbt_list;

#ifndef __USE_BT_RING_BUF_API__
typedef struct _JBTRingBuf{
    kal_uint8 *pStart;          /* 1st byte in ring buffer */
    kal_uint8 *pEnd;            /* last byte in ring buffer + 1, the size of Ring buf can be computed via "pEnd - pStart" */
    kal_uint8 *pRead;          /* 1st byte of date to be removed, ie., read pointer */
    kal_uint8 *pWrite;         /* 1st byte of free space,i.e., write pointer */
} JBTRingBuf;
#endif

typedef struct _jbt_subsession {
    /* The following is for each sub_session */
    kal_uint8 used;
    /* Add this field subsession_state: 2007-1112 */
    kal_uint8 subsession_state;
    /* For connection_request, it must use transaction_id to match which subsession is allocated for this connect request */
    /* 
     * In Enable_Service_CNF & CONNECT_CNF, Transaction_ID is used to search corresponding context. 
     * If more than one VMs use the same Transaction_ID, this search could be incorrect.
     * The Transaction_ID in context should be cleared after the transaction is finished.
     */
    kal_uint32 transaction_id;
    kal_uint16 subsession_id;
    kal_bool is_tx_empty;

	/*The flag indicates tx buffer status in lower layer.*/
    kal_bool is_lower_tx_empty;
    /* Add 2007-1022 */
    kal_bool notifyReadyToWrite; 
#ifdef __USE_BT_RING_BUF_API__
    ext_RingBuf WriteRingBuf; 
#else
    JBTRingBuf WriteRingBuf;
#endif
    kal_bool readyToRead;
#ifdef __USE_BT_RING_BUF_API__
    ext_RingBuf ReadRingBuf;
#else
    JBTRingBuf ReadRingBuf;
#endif
}jbt_subsession;

typedef struct{
    jbt_list node;
    kal_uint8 used;
    kal_uint8 ps_type;
    kal_uint8 initiator;
    kal_uint16 psm_channel;
    kal_uint32 transaction_id;
    kal_uint8 index;
    kal_uint16 subsession_count;
    kal_uint16 active_count;
    jbt_session_op_info turn_on_op;
    /* Add Read and Write information for a Session connection usage: mtk01411 2007-0831 */
    jbt_session_op_info con_req_op;
    kal_uint8* channel_buffer;
    kal_uint8 bd_addr[6];
    /* Modiftied by mtk01411: 2007-1103 */
    jbt_subsession subsession[JBT_MAX_SUBSESSION_NO];
#ifdef BTMTK_ON_LINUX
    char memName[JSR82_ASHM_NAME_LENGTH];	
#endif

}jbt_session_info;

/* Modify by mtk01411: 2007-0916 */

typedef struct {
    jbt_session_info spp_session;
}jbt_spp_info;

typedef struct {
    jbt_session_info l2cap_session;
}jbt_l2cap_info;

typedef struct {
    jbt_list freeList; /* This freeList field is used to maintain the available entry not used */
    jbt_list activeList; /* This activeList field is used to maintain active SPP connection's spp entry context */
    jbt_spp_info spp[JBT_MAX_SPP_NO];
}JBT_SPP_CNTX;


typedef struct {
    jbt_list freeList;      /* This freeList field is used to maintain the available entry not used */
    jbt_list activeList;    /* This activeList field is used to maintain active SPP connection's spp entry context */
    jbt_l2cap_info l2cap[JBT_MAX_L2CAP_NO];
}JBT_L2CAP_CNTX;

void jbt_l2cap_init(void);
void jbt_spp_init(void);


#ifdef __JSR82_MAUI__
extern void jbt_send_msg (
                    msg_type msg, 
                    module_type srcMod ,
                    module_type dstMod ,
                    sap_type sap,
                    local_para_struct *local_para,
                    peer_buff_struct* peer_buff);
#endif

void insert_node_to_targetList_tail(jbt_list* target_list, jbt_list* node);
void remove_node_from_targetList(jbt_list* target_list, jbt_list* node);
void jbt_free_one_existing_entry(kal_uint8 list_type, jbt_list *node);
jbt_list* jbt_allocate_one_available_entry(jbt_list* freeList, jbt_list* activeList, kal_uint8 list_type);
kal_bool jbt_check_already_connect_chnl_and_addr(kal_uint8 *bd_addr, kal_uint8 list_type, kal_uint16 ps_chnl_num);
jbt_list* jbt_search_an_existing_entry(jbt_list* targetList, kal_uint8 search_type, kal_uint32 value,kal_uint8 list_type);
jbt_session_info *jbt_search_existing_entry_with_psm_chnl_bdaddr(
                    jbt_list *targetList,
                    kal_uint16 psm_channel,
                    kal_uint8 *bd_addr,
                    kal_uint8 list_type);
jbt_subsession *jbt_allocate_one_available_subsession_entry(jbt_session_info *session_entry);
jbt_subsession *jbt_search_existing_subsession_entry(
                    jbt_session_info *session_entry,
                    kal_uint8 search_type,
                    kal_uint32 value);
jbt_subsession *jbt_server_find_one_subsession_entry(jbt_session_info *session_entry);
kal_bool jbt_session_check_this_psm_chnl_nun_existed(kal_uint8 ps_type, kal_uint16 existing_chnl_num);
kal_bool jbt_session_general_service_registration(
            kal_uint8 ps_type,
            kal_uint16 mtu,
            kal_uint8 security,
            kal_uint32 transaction_id,
            jbt_list *list_entry,
            jbt_subsession *subsession_entry,
            kal_uint8 list_type);
void jbt_reset_subsession_entry_except_used(jbt_subsession *subsession_entry);
void jbt_reset_subsession_entry(jbt_subsession *subsession_entry);
void jbt_reset_session_entry(kal_uint8 list_type, jbt_list *existing_entry, jbt_subsession *subsession_entry);

#ifndef __USE_BT_RING_BUF_API__
extern void JBTRING_BufInit(JBTRingBuf *ring, char buf[], kal_int16 len);
extern void JBTRING_BufFlush(JBTRingBuf *ring);
extern kal_int16 JBTRING_WriteData(JBTRingBuf *ring, char *buf, kal_int16 wanted_to_write_len);
extern kal_int16 JBTRING_ReadData(JBTRingBuf *ring, char *buf, kal_int16 wanted_to_read_len);
extern void JBTRING_GetDataPtr(JBTRingBuf *ring, char **data, kal_int16 *len);
extern void JBTRING_BufDelete(JBTRingBuf *ring, kal_int16 len);
extern kal_int16 JBTRING_FreeSpace(JBTRingBuf *ring);
extern kal_int16 JBTRING_DataLen(JBTRingBuf *ring);
#endif

kal_int16 jbt_session_RxBytes(kal_uint8 session_type, U8 session_inx, kal_uint16 subsession_id);
kal_int16 jbt_session_RxFree(kal_uint8 search_type, kal_uint8 session_type, kal_uint16 subsession_id, kal_uint32 value);
kal_int8 jbt_session_DevRX(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            char buf[],
            U16 *len);
void jbt_session_CheckReadyToWrite(U8 session_type, U8 session_inx, kal_uint16 subsession_id, void *one_session);
void jbt_session_ReturnBuf(kal_uint8 session_type, U8 session_inx, kal_uint16 subsession_id, U16 len);
kal_int8 jbt_session_DevTX(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            char **buf,
            U16 *len);
void jbt_session_DevTxEmpty(kal_uint8 session_type, U8 session_inx, kal_uint16 subsession_id);
kal_int8 jbt_session_allocate_RWRingBuf(kal_uint8 session_type, jbt_subsession *subsession_entry, U8 index);
kal_uint8 jbt_session_free_RWRingBuf(jbt_subsession *subsession_entry, U8 index);
kal_int8 jbt_session_free_RWRingBuf_with_inx(kal_uint8 session_type, U8 session_inx);
kal_int16 jbt_session_PutBytes(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            kal_uint8 *Buffaddr,
            kal_int16 Length);
I16 jbt_session_PutDeliverBytes(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            kal_uint8 *Buffaddr,
            kal_int16 Length);
void jbt_session_RemoveWriteBuf(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            kal_int16 Length);
kal_int16 jbt_session_GetBytes(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            kal_uint8 *Buffaddr,
            kal_int16 Length);
kal_int16 jbt_session_BufFreeSpace(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            kal_uint8 buf_type);
kal_int16 jbt_session_BufAvailableDataLen(
            kal_uint8 session_type,
            U8 session_inx,
            kal_uint16 subsession_id,
            kal_uint8 buf_type);
kal_int16 jbt_session_BufSize(kal_uint8 session_type);
void jbt_ConvertBdAddrOrder(kal_uint8 *dest, kal_uint8 *src);

#endif /* _JBT_INTERNAL_H */ 
#endif /* __BT_JSR82__  || __BT_JSR82_L2RF__ */
