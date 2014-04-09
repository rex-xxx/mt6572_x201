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
 * bt_spp_api.h
 * 
 * This file is the header file of External Adaptation API of SPP profile used by Application.
 * Flow direction: APP --> external ADP API
 */

#ifndef __BT_SPP_API_H__
#define __BT_SPP_API_H__

#include "bt_types.h"
#include "bt_message.h"
#include "bluetooth_spp_struct.h" 
//#include "bt_adp_spp_event.h"
#include "bt_struct.h"
#include "bluetooth_gap_struct.h" 


#if 1   /* The following is only for inject msg to UT purpose */
#define __SPP_TESTER_
#endif /* 0 */ 

#ifdef __SPP_SHARED_MEMORY__
typedef struct _SPPSharedMem
{
    U8 port;			// port number
    U8 *buffer;		// buffer pointer
    BT_BOOL inUse;
} SPPSharedMem;

#define SPP_PUT_DATA_SHARED_BUFFER_SIZE (1024 * 16)
#define SPP_GET_DATA_SHARED_BUFFER_SIZE (1024 * 16)
#define SPP_PORT_NUM 7
#endif	/* __SPP_SHARED_MEMORY__ */

#define BD_ADDR_LENGTH 6

#ifdef __SPP_SHARED_MEMORY__
void btmtk_shared_buffer_init(void);
void btmtk_shared_buffer_deinit(void);
U8 *btmtk_get_shared_put_buffer_pointer(U8 port);
void btmtk_release_shared_put_buffer(U8 port);
U8 *btmtk_get_shared_get_buffer_pointer(U8 port);
void btmtk_release_shared_get_buffer(U8 port);
#endif	/* __SPP_SHARED_MEMORY__ */

#ifdef __SPP_TESTER_
BT_BOOL spp_get_bdaddr_from_string(U8 *bd_addr, char *string);
void spp_CONVERT_ARRAY2BDADDR(btbm_bd_addr_t *dest, U8 *src);
void sppa_handler_inject_msg(char *string, kal_uint8 index);
#endif /* __SPP_TESTER_ */
//kal_bool CheckCustomSPPFlowControlSetting(module_type owner);

//#if BT_SPP_AUTHORIZE_BY_MMI
void btmtk_spp_connect_ind_rsp(U8 port, U32 lap, U8 uap, U16 nap, U8 result);
//#else /* BT_SPP_AUTHORIZE_BY_MMI */ 
void btmtk_spp_auth_rsp(U8 port, U8 result);
//#endif /* BT_SPP_AUTHORIZE_BY_MMI */
void btmtk_spp_uart_owner_cnf(U8 port);
void btmtk_spp_uart_plugout_cnf(U8 port);
#if SPP_CLIENT == XA_ENABLED
void btmtk_spp_connect_req(U32 lap, U8 uap, U16 nap, U8 server_chnl_num, U16 uuid);
#endif /* SPP_CLIENT == XA_ENABLED */
void btmtk_spp_activate_req(char *svcName, U16 svcUUID);
void btmtk_spp_send_data_req(U8 port);
void btmtk_spp_get_data_req(U8 port);
void btmtk_spp_deactivate_req(U8 port);
void btmtk_spp_disconnect_req(U8 port);
void btmtk_spp_initialize_req(void);
#if 0
void btmtk_spp_register_callback_req(SPPCallback callback);
#endif
void btmtk_spp_uart_put_bytes_req(U8 port, U8* buffer, U16 length);
void btmtk_spp_uart_get_bytes_req(U8 port, U16 length);
void btmtk_spp_uart_open_req(U8 port);
void btmtk_spp_uart_close_req(U8 port);
void btmtk_spp_uart_set_owner_req(U8 port);
void btmtk_spp_register_mmi_callback_req(BTMTK_EventCallBack callback);

/* DUN related API */
//#if BT_SPP_AUTHORIZE_BY_MMI
void btmtk_dun_connect_ind_rsp(U8 port, U32 lap, U8 uap, U16 nap, U8 result);
//#else /* BT_SPP_AUTHORIZE_BY_MMI */ 
void btmtk_dun_auth_rsp(U8 port, U8 result);
//#endif /* BT_SPP_AUTHORIZE_BY_MMI */
#if DUN_CLIENT == XA_ENABLED
void btmtk_dun_connect_req(U32 lap, U8 uap, U16 nap, U8 server_chnl_num);
#endif /* SPP_CLIENT == XA_ENABLED */
void btmtk_dun_activate_req(void);
void btmtk_dun_deactivate_req(U8 port);
void btmtk_dun_disconnect_req(U8 port);
void btmtk_dun_uart_put_bytes_req(U8 port, U8* buffer, U16 length);
void btmtk_dun_uart_get_bytes_req(U8 port, U16 length);
void btmtk_dun_register_mmi_callback_req(BTMTK_EventCallBack callback);
/* FAX related API */
//#if BT_SPP_AUTHORIZE_BY_MMI
void btmtk_fax_connect_ind_rsp(U8 port, U32 lap, U8 uap, U16 nap, U8 result);
//#else /* BT_SPP_AUTHORIZE_BY_MMI */ 
void btmtk_fax_auth_rsp(U8 port, U8 result);
//#endif /* BT_SPP_AUTHORIZE_BY_MMI */
#if FAX_CLIENT == XA_ENABLED
void btmtk_fax_connect_req(U32 lap, U8 uap, U16 nap, U8 server_chnl_num);
#endif /* FAX_CLIENT == XA_ENABLED */
void btmtk_fax_activate_req(void);
void btmtk_fax_deactivate_req(U8 port);
void btmtk_fax_disconnect_req(U8 port);
void btmtk_fax_uart_put_bytes_req(U8 port, U8* buffer, U16 length);
void btmtk_fax_uart_get_bytes_req(U8 port, U16 length);
void btmtk_fax_register_mmi_callback_req(BTMTK_EventCallBack callback);
void btmtk_spp_enable_req(void);
void btmtk_spp_disable_req(void);
void btmtk_spp_uart_data_available_ind_rsp(U8 port, U16 length);
#ifdef __SPP_SHARED_MEMORY__
void btmtk_spp_uart_assign_buffer_req(U8 port);
void btmtk_spp_uart_release_buffer_req(U8 port);
#endif	/* __SPP_SHARED_MEMORY__ */

#ifdef BTMTK_ON_LINUX
void btmtk_spp_set_sockfd(int sockfd);
#endif

#endif	/* __BT_SPP_API_H__ */

