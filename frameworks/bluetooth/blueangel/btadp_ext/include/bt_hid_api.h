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
 * Bt_hid_api.h
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
 * Ting Zheng
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
#ifndef __BT_HID_API_H__
#define __BT_HID_API_H__
#include "bt_types.h"
#include <cutils/xlog.h>


typedef enum{
	BTMTK_HID_KEYBOARD_ROLE,
	BTMTK_HID_MOUSE_ROLE,
	BTMTK_HID_PHONE_ROLE,
}bt_hid_roll;

#undef LOG_TAG
#define LOG_TAG "[BT][HID][JNI]"

#define EXT_DEBUG 1

#ifdef EXT_DEBUG
#define HID_LOG(fmt, ...) XLOGI("[BT][HID][EXT]%s:" fmt, __FUNCTION__, ## __VA_ARGS__)
#else
#define HID_LOG(fmt, ...) {}
#endif

#define HID_ERR(fmt, ...) XLOGE("[BT][HID][EXT]%s:" fmt, __FUNCTION__, ## __VA_ARGS__)

/*
#ifdef BTMTK_WISE_MBT_LOG
#define HID_LOG(x) MBT_LOG(x)
#else
#define HID_LOG(x) OS_Report(x)
#endif
*/
#ifdef BTMTK_WISE_MBT_LOG
#define HID_LOG1(x, p1) MBT_LOG1(x, p1)
#else
#define HID_LOG1(x, p1) OS_Report(x, p1)
#endif

#ifdef BTMTK_WISE_MBT_LOG
#define HID_LOG2(x, p1, p2) MBT_LOG2(x, p1 ,p2)
#else
#define HID_LOG2(x, p1, p2) OS_Report(x, p1, p2)
#endif

#ifdef BTMTK_WISE_MBT_LOG
#define HID_LOG3(x, p1, p2, p3) MBT_LOG3(x, p1 ,p2, p3)
#else
#define HID_LOG3(x, p1, p2, p3) OS_Report(x, p1 ,p2, p3)
#endif
/*
#ifdef BTMTK_WISE_MBT_LOG
#define HID_ERR(x) MBT_ERR(x)
#else
#define HID_ERR(x) OS_Report(x)
#endif
*/

#ifdef BTMTK_WISE_MBT_LOG
#define HID_ERR1(x, p1) MBT_ERR1(x, p1)
#else
#define HID_ERR1(x, p1) OS_Report(x, p1)
#endif

#ifdef BTMTK_WISE_MBT_LOG
#define HID_ERR2(x, p1, p2) MBT_ERR2(x, p1, p2)
#else
#define HID_ERR2(x, p1, p2) OS_Report(x, p1, p2)
#endif

#define BT_MEM_GUARD1 0xABABABAB
#define BT_MEM_GUARD2 0xCDCDCDCD
#define BT_MEM_GUARD3 0xEEEEEEEE
#define BT_MEM_GUARD4 0xFFFFFFFF


typedef void (*BTMTK_HID_CALLBACK)(U32 HIDUIEvent, BD_ADDR* device_addr);

void btmtk_hidd_activate_req_ext();
void btmtk_hidd_deactivate_req_ext();
void btmtk_hidd_connect_req_ext(BD_ADDR device_addr);
void btmtk_hidd_disconnect_req_ext();
void btmtk_hidd_send_unplug_req();
void btmtk_hidd_send_input_report(bt_hidd_report_type_enum rpt_type, U8 *rpt_data, U16 rpt_size);  // input, send on interrupt channel
void btmtk_hidd_change_role(bt_hid_roll  device_role);

void btmtk_hidh_activate_req_ext(void *pfnCB);
void btmtk_hidh_deactivate_req_ext(void);
void btmtk_hidh_connect_req_ext(BD_ADDR device_addr);
void btmtk_hidh_disconnect_req_ext(BD_ADDR device_addr);
void btmtk_hidh_get_descInfo_req(BD_ADDR device_addr);
void btmtk_hidh_send_control_req(BD_ADDR device_addr, bt_hidd_ctrl_op_enum control);
void btmtk_hidh_set_report_req(BD_ADDR device_addr, bt_hidd_report_type_enum rpt_type, U8 *rpt_data, U16 rpt_size);
void btmtk_hidh_get_report_req(BD_ADDR device_addr, U8 rpt_type, U8 rpt_id);
void btmtk_hidh_set_protocol_req(BD_ADDR device_addr, BT_BOOL boot_mode);
void btmtk_hidh_get_protocol_req(BD_ADDR device_addr);
void btmtk_hidh_set_idle_rate_req(BD_ADDR device_addr,  U8 idleRate);
void btmtk_hidh_get_idle_rate_req(BD_ADDR device_addr);
void btmtk_hidh_send_output_report(BD_ADDR device_addr, bt_hidd_report_type_enum rpt_type, U8 *rpt_data, U16 rpt_size);

void btmtk_hid_convert_btaddr(BD_ADDR dst, BD_ADDR src);

void btmtk_hidd_activate_req(void);
void btmtk_hidd_deactivate_req(void);
void btmtk_hidd_connect_req(U8 *device_addr);
void btmtk_hidd_disconnect_req(U8 *device_addr, U32 conn_id);
void btmtk_hidd_authorize_rsp(U8 *device_addr, U32 conn_id, BT_BOOL result);

void btmtk_hidh_activate_req(void);
void btmtk_hidh_deactivate_req(void);
void btmtk_hidh_connect_req(U8 *device_addr);
void btmtk_hidh_disconnect_req(U8 *device_addr);
void btmtk_hidh_authorize_rsp(U8 *device_addr, U32 conn_id, BT_BOOL result);
BT_BOOL btmtk_hidh_set_callback(BTMTK_HID_CALLBACK callback);
void btmtk_hidh_UI_callback(bt_hid_host_event_type  Evt, BD_ADDR* device_addr);
U8 bt_hid_init_socket();
U8 bt_hid_close_socket();

void BTCMD_SendMessage(msg_type msg_id, module_type dest_mod, void *ptr, U16 size);
void *bt_hid_malloc(U32 size);
void bt_hid_free(void *ptr);




#endif
