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

/****************************************************************************
 *
 * File:
 *     $Workfile:hfg.h$ for iAnywhere Blue SDK, Version 2.1.1
 *     $Revision: #1 $
 *
 * Description: This file contains the hands free profile.
 *             
 * Created:     September 20, 2001
 *
 * Copyright 2001-2004 Extended Systems, Inc.

 * Portions copyright 2005 iAnywhere Solutions, Inc.

 * All rights reserved. All unpublished rights reserved.
 *
 * Unpublished Confidential Information of iAnywhere Solutions, Inc.  
 * Do Not Disclose.
 *
 * No part of this work may be used or reproduced in any form or by any means, 
 * or stored in a database or retrieval system, without prior written 
 * permission of iAnywhere Solutions, Inc.
 * 
 * Use of this work is governed by a license granted by iAnywhere Solutions,  
 * Inc.  This work contains confidential and proprietary information of 
 * iAnywhere Solutions, Inc. which is protected by copyright, trade secret, 
 * trademark and other intellectual property rights.
 *
 ****************************************************************************/

#ifndef __BT_CHN_API_H__
#define __BT_CHN_API_H__

#include "bt_types.h"
#include "bt_struct.h"
#include "bluetooth_chn_struct.h"


/****************************************************************************
 *
 * Function Reference
 *
 ****************************************************************************/

/*---------------------------------------------------------------------------
 * btmtk_chn_register()
 *
 *     Registers and initializes a channel for use in creating and receiving
 *     service level connections.  Registers the Hands-Free profile audio
 *     gateway with SDP.  The application callback function is also bound
 *     to the channel.
 *
 * Parameters:
 *     Channel - Contains a pointer to the channel structure that will be
 *         initialized and registered.
 *
 *     Callback - The application callback function that will receive events.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The operation completed successfully.
 *
 *     BT_STATUS_IN_USE - The operation failed because the channel has already
 *         been initialized. 
 *
 *     BT_STATUS_FAILED - The operation failed because either the RFCOMM
 *         channel or the SDP record could not be registered.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_register(ChnChannelContext *Channel, BTMTK_EventCallBack Callback, U16 svc, U16 remote_svc);

/*---------------------------------------------------------------------------
 * btmtk_chn_deregister()
 *
 *     Deregisters the channel.  The channel becomes unbound from RFCOMM and
 *     SDP, and can no longer be used for creating service level connections.
 *
 * Parameters:
 *     Channel - Contains a pointer to the channel structure that will be
 *         deregistered.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The operation completed successfully.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_BUSY - The operation failed because a service level connection 
 *         is still open to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_deregister(ChnChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_chn_create_service_link()
 *
 *     Creates a service level connection with the hands-free unit.
 *     This includes performing SDP Queries to find the appropriate service
 *     and opening an RFCOMM channel.  The success of the operation is indicated 
 *     by the HFG_EVENT_SERVICE_CONNECTED event.  If the connection fails, the
 *     application is notified by the HFG_EVENT_SERVICE_DISCONNECTED event. 
 * 
 *     If an ACL link does not exist to the audio gateway, one will be 
 *     created first.  If desired, however, the ACL link can be established 
 *     prior to calling this function.
 *
 * Parameters:
 *
 *     Channel - Pointer to a registered channel structure.
 *
 *     Addr - The Bluetooth address of the remote device.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the connection has been created (via the callback 
 *         function registered by btmtk_hfgRegister).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_BUSY - The operation failed because a connection is already
 *         open to the remote device, or a new connection is being created.
 *
 *     BT_STATUS_FAILED - The channel has not been registered.
 *
 *     BT_STATUS_CONNECTION_FAILED - The connection failed.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_create_service_link(ChnChannelContext *Channel, U8 *Addr);

/*---------------------------------------------------------------------------
 * btmtk_chn_disconnect_service_link()
 *
 *     Releases the service level connection with the hands-free unit. This will 
 *     close the RFCOMM channel and will also close the SCO and ACL links if no 
 *     other services are active, and no other link handlers are in use 
 *     (ME_CreateLink).  When the operation is complete the application will be 
 *     notified by the HFG_EVENT_SERVICE_DISCONNECTED event.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the service level connection is down (via the callback 
 *         function registered by btmtk_chn_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service link
 *         does not exist to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_disconnect_service_link(ChnChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_chn_create_audio_link()
 *
 *     Creates an audio (SCO) link to the hands-free unit. The success of the 
 *     operation is indicated by the HFG_EVENT_AUDIO_CONNECTED event.  If the 
 *     connection fails, the application is notified by the 
 *     HFG_EVENT_AUDIO_DISCONNECTED event.  
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the audio link has been established (via the callback 
 *         function registered by btmtk_chn_register).
 *
 *     BT_STATUS_SUCCESS - The audio (SCO) link already exists.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service level 
 *         connection does not exist to the audio gateway.
 *
 *     BT_STATUS_FAILED - An audio connection already exists.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_create_audio_link(ChnChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_chn_disconnect_audio_link()
 *
 *     Releases the audio connection with the hands-free unit.  When the 
 *     operation is complete, the application will be notified by the 
 *     HFG_EVENT_SERVICE_DISCONNECTED event.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the audio connection is down (via the callback 
 *         function registered by btmtk_chn_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service link
 *         does not exist to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_disconnect_audio_link(ChnChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_chn_accept_connect()
 *
 *     Accept connection request from remote devices. If operation is successfully completed,
 *     HFG sends HFG_EVENT_SERVICE_CONNECTED event to ADP. If the connection can not be 
 *     created, HFG sends HFG_EVENT_SERVICE_DISCONNECTED event to ADP.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the audio connection is up or down (via the callback 
 *         function registered by btmtk_chn_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_accept_connect(ChnChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_chn_reject_connect()
 *
 *     Reject connection request from remote devices. If operation is successfully completed,
 *     HFG sends HFG_EVENT_SERVICE_CONNECTED event to ADP. 
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the audio connection is down (via the callback 
 *         function registered by btmtk_chn_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_reject_connect(ChnChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_chn_send_at_response()
 *
 *     Sends any AT response.  The 'AtString' parameter must be initialized,
 *     and the AT response must be formatted properly.  It is not necessary
 *     to add CR/LF at the beginning and end of the string.
 *
 *     When the AT response has been sent, the HFG_EVENT_RESPONSE_COMPLETE
 *     event will be received by the application's callback function.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     AtString - An properly formated AT response.
 *   
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_PENDING - The operation has started, the application will be 
 *         notified when the response has been sent (via the callback function 
 *         registered by btmtk_chn_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service link
 *         does not exist to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_chn_send_data(ChnChannelContext *Channel, U8* buf, U16 len);

#endif /* __BT_CHN_H__ */

