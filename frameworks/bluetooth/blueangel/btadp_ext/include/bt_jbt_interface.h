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
 *  jwa_internal.h
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
 * Jul 14 2009 mtk02126
 * [MAUI_01720950] Solve JSR82 build error for JSR82_SESSION_PS_L2CAP_MTU
 * 
 *
 * Jul 11 2009 mtk02126
 * [MAUI_01703270] Modify traces
 * 
 *
 * Oct 17 2008 mtk01624
 * [MAUI_00780660] fix daily build error
 * 
 *
 * Oct 16 2008 mtk01624
 * [MAUI_01238166] JAVA JSR82_BT is on,but this MIDlet show BT is off
 * 
 *
 * Sep 24 2008 mtk01411
 * [MAUI_01243263] [JSR82] JBT Add reject cmds handling in VM Minimize mode
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
 * Aug 21 2007 mtk00727
 * [MAUI_00536361] [Java] Add BT folders
 * 
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!! 
 *==============================================================================
 *******************************************************************************/

#if defined(__BT_JSR82__) || defined(__BT_JSR82_L2RF__)
#ifndef _JBT_INTERFACE_H
#define _JBT_INTERFACE_H

#include "bt_types.h"


#define JBT_OPCODE_SESSION_CON_IND  0x51
#define JBT_OPCODE_SESSION_CON_CNF  0x52
#define JBT_OPCODE_SESSION_RX_READY_IND 0x53
#define JBT_OPCODE_SESSION_TX_READY_IND 0x54
#define JBT_OPCODE_SESSION_DISCONNECT_IND 0x55
#define JBT_OPCODE_SESSION_REGISTRATION_RESULT 0x56
#define JBT_OPCODE_SESSION_DEREGISTRATION_RESULT 0x57
#define JBT_OPCODE_SESSION_TURN_ON_RESULT 0x58
#define JBT_OPCODE_SESSION_TURN_OFF_RESULT 0x59


#define JBT_CMD_SUCCESS                 0x00
#define JBT_CMD_FAILED                  0x01
#define JBT_CMD_SDP_REGISTER_FAILED        0x02
/* JBT_CMD_INVALID_HANDLE: it is used if the handle for registration or de-registration sdp record  is invalid */
#define JBT_CMD_INVALID_HANDLE  0x03 
/* JBT_CMD_SDP_DEREGISTER_FAILED: It is used if this handle for de-registration sdp record is valid 
  * And it is allocated previously, during de-registration sdp record: 
  * 1.reset this handle first 
  * 2.but its corresponding sdp record is never registered 
  */
#define JBT_CMD_SDP_DEREGISTER_FAILED      0x04
/* JBT_CMD_SDP_DDB_FULL: it is used if no avaliable handle for getting record handle request */
#define JBT_CMD_SDP_DDB_FULL        0x05
#define JBT_CMD_SDP_RECORD_SYNTAX_ERROR     0x06
/* JBT_CMD_SDP_RECORD_TOO_LARGE: It is used if sdp record wanted to be registered is too large */
#define JBT_CMD_SDP_RECORD_TOO_LARGE        0x07
#define JBT_CMD_SDP_RECORD_ATTRIBUTE_BUFFER_TOO_SMALL       0x08
#define JBT_CMD_NOT_SUPPORT     0x09
#define JBT_CMD_WRONG_STATE     0x0A
/* JBT_CMD_SIZE_TOO_LARGE: it is used if search pattern size is too large or registration sdp record size too larger
  * return from jbt_sdap_ss_cmd(), jbt_sdap_sa_cmd(), jbt_sdap_ssa_cmd() and jbt_register_record_cmd() function directly
  */
#define JBT_CMD_SIZE_TOO_LARGE    0x0B
#define JBT_CMD_REJECT_DUE_TO_INQUIRY   0x0C

#define JBT_CMD_REJECT_DUE_TO_POWEROFF 0x0D
#define JBT_CMD_REJECT_CON_REQ_ALREADY_EXISTED  0x0E
#define JBT_CMD_REJECT_NO_RESOURCE  0x0F
#define JBT_CMD_REJECT_WRONG_STATE 0x10

#define JBT_CMD_REJECT_DUE_TO_VM_MINIMIZE   0x11

typedef enum 
{
    JBT_POWER_OFF = 0,
    JBT_POWER_ON,
    /* Bluetooth is in the switch state such as ON to OFF or OFF to ON */ /*COMMENTS*/
    JBT_POWER_OTHERS    
}JBT_POWER_STATE_ENUM;

/* Add by mtk01411: 2007-1128 */
typedef enum
{
    JBT_POWER_OFF_RESTORE_LINK_BIT =0,
    JBT_POWER_OFF_RESTORE_MMI_BIT,
    JBT_POWER_OFF_RESTORE_COD_BIT,
    JBT_POWER_OFF_RESTORE_MEM_BIT,
    JBT_POWER_ON_WAITING_BIT

}JBT_POWER_OFF_EVENT_ENUM;


#define BT_JSR82_RESTORE_NONE   (0x00)
#define BT_JSR82_RESTORE_LINK  (0x01)
#define BT_JSR82_RESTORE_MMI   (0x02)
#define BT_JSR82_RESTORE_COD   (0x03)
#define BT_JSR82_RESTORE_MEM   (0x04)


typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 ps_type;
    kal_uint16 psm_channel;
    kal_uint8 con_id;
}jbt_session_registration_result;

typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 ps_type;
    kal_uint8 con_id;
}jbt_session_deregistration_result;


typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 con_id;
}jbt_session_turnon_result;

typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 con_id;
}jbt_session_turnoff_result;

typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 bd_addr[6];
    kal_uint16 con_id;
    kal_uint8 ps_type;
    kal_uint16 mtu;
    kal_uint16 channel;
    kal_uint16 l2cap_id;    
}jbt_session_connect_ind;

typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 con_id;
    kal_uint8 l2cap_id;
}jbt_session_connect_cnf;


/* RX_READY_IND: Application can start to read data from ReadRingBuf again */
typedef struct{
    kal_uint8 con_id;
    kal_uint16 l2cap_id;
    kal_uint16 length;
}jbt_session_rx_ready_ind;

/* TX_READY_IND: Application can start to write data to WriteRingBuf again */
typedef struct{
    kal_uint8 con_id;
    kal_uint16 l2cap_id;
}jbt_session_tx_ready_ind;

typedef struct{
    kal_uint8 result;
    kal_uint32 transaction_id;
    kal_uint8 con_id;
    kal_uint16 l2cap_id;
}jbt_session_disconnect_ind;


extern kal_bool jbt_session_service_registration(kal_uint8 ps_type, kal_uint16 mtu, kal_uint8 security, kal_uint32 transaction_id, kal_uint8* status_result);
extern kal_bool jbt_session_service_turn_on(kal_uint8 ps_type, kal_uint8 con_id, kal_uint32 transaction_id, kal_uint8* status_result);
extern kal_bool jbt_session_service_turn_off(kal_uint8 ps_type, kal_uint8 con_id, kal_uint32 transaction_id, kal_uint8* status_result);

extern kal_bool jbt_session_service_deregistration(kal_uint8 ps_type, kal_uint32 transaction_id, kal_uint8 con_id, kal_uint8* status_result);
extern kal_bool jbt_session_connect_req(kal_uint32 transaction_id,
                                                    kal_uint8* bd_addr, 
                                                    kal_uint8 ps_type,
                                                    kal_uint16 psm_channel, 
                                                    kal_uint16 mtu, 
                                                    kal_uint8 security_value,
                                                    kal_uint8* status_result);
extern kal_bool jbt_session_disconnect_req(kal_uint32 transaction_id,kal_uint8 ps_type, kal_uint8 con_id, kal_uint16 l2cap_id, kal_uint8* status_result);
#endif /* _JBT_INTERFACE_H */ 
#endif /* __BT_JSR82__  || __BT_JSR82_L2RF__ */
