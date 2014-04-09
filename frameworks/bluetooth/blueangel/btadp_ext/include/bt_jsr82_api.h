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
 * bt_jsr82_api.h
 * 
 * This file is the header file of External Adaptation API of JSR82 used by Virtual Machine.
 * Flow direction: VM --> external ADP API
 */

#ifndef __BT_JSR82_API_H__
#define __BT_JSR82_API_H__

#include "bt_struct.h"
#include "bluetooth_jsr82_struct.h"

#include "assert.h"
#ifndef ASSERT
#define ASSERT assert
#endif

#ifdef __SPP_SHARED_MEMORY__
typedef struct _JSR82SharedMem
{
    U8 index;			// port number
    U8 *buffer;		// buffer pointer
    BT_BOOL inUse;
    U16 waitPutBytesCnfLength;	// Used only for Send Deliver buffer management
#ifdef BTMTK_ON_LINUX
    char memName[JSR82_ASHM_NAME_LENGTH];
#endif
} JSR82SharedMem;

#endif	/* __SPP_SHARED_MEMORY__ */
#define JSR82_PUT_DATA_SHARED_BUFFER_SIZE (1024 * 16)
#define JSR82_GET_DATA_SHARED_BUFFER_SIZE (1024 * 16)
#define JSR82_DELIVER_GET_DATA_SHARED_BUFFER_SIZE (1024 * 12)
#define JSR82_DELIVER_PUT_DATA_SHARED_BUFFER_SIZE (1024 * 12)


#define BD_ADDR_LENGTH 6


#ifdef BTMTK_ON_LINUX
typedef I8 BtStatus;

#define BT_STATUS_SUCCESS 0
#define BT_STATUS_FAILED  1
#define BT_STATUS_PENDING 2


void btmtk_jsr82_setExtSockAddress(struct sockaddr_un *addr, socklen_t addrlen);
void btmtk_jsr82_setSockFd(int srvcsockfd, int sockfd);
BtStatus JSR82_SendMessage(msg_type msg_id, module_type mod_id, ilm_struct * ilm, U16 size);
#endif	// BTMTK_ON_LINUX
void btmtk_jsr82_register_mmi_callback_req(BTMTK_EventCallBack callback);
void btmtk_jbt_init(void);
#ifdef BTMTK_ON_LINUX
void *btmtk_jsr82_malloc_ashm(U32 size, U8 index, U8 type, U8 *memName, U32 transaction_ID);
void btmtk_jsr82_free_ashm(U8 index, U8 type);
void btmtk_jsr82_update_ashm_index(U8 index, U32 transaction_ID);
#endif // BTMTK_ON_LINUX
U8 *btmtk_jsr82_malloc(U32 size);
void btmtk_jsr82_free(void *ptr);
#ifdef __JSR82_SHARED_MEMORY__
void btmtk_jsr82_shared_buffer_init(void);
void btmtk_jsr82_shared_buffer_deinit(void);
U8 *btmtk_jsr82_get_shared_put_buffer_pointer(U8 port);
void btmtk_jsr82_release_shared_put_buffer(U8 port);
U8 *btmtk_jsr82_get_shared_get_buffer_pointer(U8 port);
void btmtk_jsr82_release_shared_get_buffer(U8 port);
U8 *btmtk_jsr82_get_deliver_shared_put_buffer_pointer(U8 index);
void btmtk_jsr82_release_deliver_shared_put_buffer(U8 index);
U8 *btmtk_jsr82_get_deliver_shared_get_buffer_pointer(U8 index);
void btmtk_jsr82_release_deliver_shared_get_buffer(U8 index);
U16 btmtk_jsr82_get_wait_put_bytes_cnf_length(U8 index);
void btmtk_jsr82_update_wait_put_bytes_cnf_length(U8 index, U16 length);
#ifdef BTMTK_ON_LINUX
void btmtk_jsr82_get_deliver_shared_put_buffer_name(U8 index, char *memName);
void btmtk_jsr82_get_deliver_shared_get_buffer_name(U8 index, char *memName);
#endif // BTMTK_ON_LINUX
#endif	/* __JSR82_SHARED_MEMORY__ */
BT_BOOL btmtk_jsr82_session_service_registration(
            kal_uint8 ps_type,
            kal_uint16 mtu,
            kal_uint8 security,
            kal_uint32 transaction_id,
            kal_uint8* status_result);
BT_BOOL btmtk_jsr82_session_service_registration_use_existing_chnl_num(
            kal_uint8 ps_type,
            kal_uint16 mtu,
            kal_uint8 security,
            kal_uint32 transaction_id,
            U16 existing_chnl_num);
BT_BOOL btmtk_jsr82_session_service_turn_on(kal_uint8 ps_type, U8 con_id, kal_uint32 transaction_id, kal_uint8* status_result);
BT_BOOL btmtk_jsr82_session_service_turn_off(kal_uint8 ps_type, U8 con_id, kal_uint32 transaction_id, kal_uint8* status_result);
BT_BOOL btmtk_jsr82_session_service_deregistration(kal_uint8 ps_type, kal_uint32 transaction_id, U8 con_id, kal_uint8* status_result);
BT_BOOL btmtk_jsr82_session_connect_req(
            kal_uint32 transaction_id,
            kal_uint8 *bd_addr,
            kal_uint8 ps_type,
            kal_uint16 psm_channel,
            kal_uint16 mtu,
            kal_uint8 security_value,
            kal_uint8* status_result);
BT_BOOL btmtk_jsr82_session_disconnect_req(kal_uint32 transaction_id, kal_uint8 ps_type, U8 con_id, kal_uint16 l2cap_id, kal_uint8* status_result);
I16 btmtk_jsr82_session_PutBytes(
            U8 ps_type,
            U8 session_inx,
            U16 subsession_id,
            U8 *Buffaddr,
            U16 Length);
I16 btmtk_jsr82_session_GetBytes(
            U8 ps_type,
            U8 session_inx,
            U16 subsession_id,
            U8 *Buffaddr,
            U16 Length);
I16 btmtk_jsr82_session_GetAvailableDataLength(
            U8 ps_type,
            U8 session_inx,
            U16 subsession_id,
            U8 buf_type);
void btmtk_jsr82_data_available_ind_rsp(U8 index, U8 ps_type, U16 l2cap_id, U16 length);
I16 btmtk_jsr82_SendThroughDeliver(U8 index, U8 ps_type, U16 l2cap_id);
#ifdef __JSR82_SHARED_MEMORY__
void btmtk_jsr82_assign_buffer_req(U8 port);
void btmtk_jsr82_release_buffer_req(U8 port);
#endif	/* __JSR82_SHARED_MEMORY__ */


#endif	/* __BT_JSR82_API_H__ */


