/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bluetooth_pan_message.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to define message for communication between BT task and external task.
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
#ifndef __BT_PAN_MESSAGE_H__
#define __BT_PAN_MESSAGE_H__

    /*APP --> PAN*/
    MSG_ID_BT_PAN_ACTIVATE_REQ = MSG_ID_BT_PAN_GROUP_START,
    MSG_ID_BT_PAN_DEACTIVATE_REQ,
    MSG_ID_BT_PAN_CONNECT_REQ,
    MSG_ID_BT_PAN_DISCONNECT_REQ,
    MSG_ID_BT_PAN_CONNECTION_AUTHORIZE_RSP,
    MSG_ID_BT_PAN_SET_NETTYPE_FILETER_REQ,
    MSG_ID_BT_PAN_SET_MULTIADDR_FILETER_REQ,
    MSG_ID_BT_PAN_SEND_PACKET_REQ,
    MSG_ID_BT_PAN_SEND_PENDING_PACKET_REQ,
    MSG_ID_BT_PAN_PTS_TEST_SEND_PACKET_REQ,

    MSG_ID_BT_PAN_CMD,	// used if cmd agent is necessary

    /*APP <-- PAN*/
    MSG_ID_BT_PAN_ACTIVATE_CNF,
    MSG_ID_BT_PAN_DEACTIVATE_CNF,
    MSG_ID_BT_PAN_CONNECT_CNF,
    MSG_ID_BT_PAN_DISCONNECT_CNF,
    MSG_ID_BT_PAN_SET_NETTYPE_FILETER_CNF,
    MSG_ID_BT_PAN_SET_MULTIADDR_FILETER_CNF,
    MSG_ID_BT_PAN_CONNECTION_AUTHORIZE_IND,
    MSG_ID_BT_PAN_CONNECT_IND,
    MSG_ID_BT_PAN_DISCONNECT_IND,
    MSG_ID_BT_PAN_SET_NETTYPE_FILTER_IND,
    MSG_ID_BT_PAN_SET_MULTIADDR_FILTER_IND,
    MSG_ID_BT_PAN_GROUP_END = MSG_ID_BT_PAN_SET_MULTIADDR_FILTER_IND,

#endif
