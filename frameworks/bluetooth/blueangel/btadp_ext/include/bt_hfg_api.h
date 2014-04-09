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
 *     $Revision: #3 $
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

#ifndef __BT_HFG_API_H__
#define __BT_HFG_API_H__

#include "bt_types.h"
#include "bt_struct.h"
#include "bluetooth_hfg_struct.h"
//#define ENABLE_SYNC_INTERFACE 1

#ifdef __cplusplus
extern "C" {
#endif

#if defined(EXT_DYNAMIC_LOADING)
typedef BtStatus (*hfg_register_api)(HfgChannelContext *, BTMTK_EventCallBack, int, kal_bool);
typedef BtStatus (*hfg_deregister_api)(HfgChannelContext *);
typedef BtStatus (*hfg_create_service_link_api)(HfgChannelContext *, MTK_BD_ADDR *);
typedef BtStatus (*hfg_disconnect_service_link_api)(HfgChannelContext *);
typedef BtStatus (*hfg_create_audio_link_api)(HfgChannelContext *);
typedef BtStatus (*hfg_disconnect_audio_link_api)(HfgChannelContext *);
typedef BtStatus (*hfg_accept_connect_api)(HfgChannelContext *);
typedef BtStatus (*hfg_reject_connect_api)(HfgChannelContext *);
typedef BtStatus (*hfg_send_data_api)(HfgChannelContext *, const char *, U16);
typedef struct _hfg_api_table
{
    hfg_register_api          hfg_register;
    hfg_deregister_api      hfg_deregister;
    hfg_create_service_link_api hfg_create_service_link;
    hfg_disconnect_service_link_api hfg_disconnect_service_link;
    hfg_create_audio_link_api hfg_create_audio_link;
    hfg_disconnect_audio_link_api hfg_disconnect_audio_link;
    hfg_accept_connect_api hfg_accept_connect;
    hfg_reject_connect_api hfg_reject_connect;
    hfg_send_data_api hfg_send_data;
}hfg_api_table;
#endif /* EXT_DYNAMIC_LOADING */
/****************************************************************************
 *
 * Function Reference
 *
 ****************************************************************************/

/*---------------------------------------------------------------------------
 * btmtk_hfginit()
 *
 *     Initialize the Audio Gateway SDK.  This function should only be called
 *     once, normally at sytem initialization time.  The calling of this 
 *     function can be specified in overide.h using the XA_LOAD_LIST macro
 *     (i.e. #define XA_LOAD_LIST XA_MODULE(HFG)).
 *
 * Returns:
 *     TRUE - Initialization was successful
 *
 *     FALSE - Initialization failed.
 */
kal_bool btmtk_hfg_init(void);

/*---------------------------------------------------------------------------
 * btmtk_hfg_register()
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
//BtStatus btmtk_hfg_register(HfgChannelContext *Channel, HfgCallback Callback, kal_bool bHeadset);
BtStatus btmtk_hfg_register(HfgChannelContext *Channel, /*HfgCallback*/BTMTK_EventCallBack Callback, int sockfd, /*int servsockfd,*/ kal_bool bHeadset);

/*---------------------------------------------------------------------------
 * btmtk_hfg_deregister()
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
BtStatus btmtk_hfg_deregister(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_create_service_link()
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
BtStatus btmtk_hfg_create_service_link(HfgChannelContext *Channel, MTK_BD_ADDR *Addr);

/*---------------------------------------------------------------------------
 * btmtk_hfg_disconnect_service_link()
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
 *         function registered by btmtk_hfg_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service link
 *         does not exist to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_disconnect_service_link(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_create_audio_link()
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
 *         function registered by btmtk_hfg_register).
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
BtStatus btmtk_hfg_create_audio_link(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_disconnect_audio_link()
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
 *         function registered by btmtk_hfg_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service link
 *         does not exist to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_disconnect_audio_link(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_accept_connect()
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
 *         function registered by btmtk_hfg_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_accept_connect(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_reject_connect()
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
 *         function registered by btmtk_hfg_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_reject_connect(HfgChannelContext *Channel);

#if defined(BT_SCO_HCI_DATA) && BT_SCO_HCI_DATA == XA_ENABLED
/*---------------------------------------------------------------------------
 * btmtk_hfg_send_audio_data()
 *
 *     Sends the specified audio data on the audio link.
 *
 * Requires:
 *     BT_SCO_HCI_DATA enabled.
 *
 * Parameters:
 *     Channel - The Channel over which to send the audio data.
 *
 *     packet - The packet of data to send. After this call, the Hands-free
 *         SDK owns the packet. When the packet has been transmitted
 *         to the host controller, HFG_EVENT_AUDIO_DATA_SENT is sent to the
 *         application
 *
 * Returns:
 *     BT_STATUS_PENDING - The packet was queued successfully.
 *
 *     BT_STATUS_NO_CONNECTION - No audio connection exists.
 *
 *     BT_STATUS_INVALID_PARM - Invalid parameter (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_audio_data(HfgChannelContext *Channel, BtPacket *Packet);
#define btmtk_hfg_send_audio_data(c, p) SCO_SendData(c->cmgrHandler.scoConnect, p)
#endif

/*---------------------------------------------------------------------------
 * btmtk_hfg_set_indicator_value()
 *
 *     Sets the current value for an indicator.  If a service level connection
 *     is active and indicator reporting is currently enabled, the the state 
 *     of the modified indicator is reported to the hands-free device.  If no 
 *     service level connection exists, the current value is changed and will 
 *     be reported during the establishment of the service level connection.
 *     If indicator reporting is disabled, the value of the indicator will only
 *     be reported when requested by the hands-free unit (AT+CIND).
 *
 *     Upon registration of an Audio Gateway (btmtk_hfg_register()), all indicators
 *     are initialized to 0.  To properly initialize all indicators, this
 *     function must be called once for each indicator prior to establishing
 *     a service level connection.
 *
 *     Indicators must be sent to the hands-free device as specified by the
 *     hands-free v1.5 specification.  Indicators are sent in the order that
 *     calls are made to this function.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Indicator - Indicator type.
 *
 *     Value - The value of the indicator.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The indicator value was set and the Response
 *         structure is available for use.
 *
 *     BT_STATUS_PENDING - The indicator value was set and queued for
 *         sending to the hands-free unit.  When the response has been sent, 
 *         the HFG_EVENT_RESPONSE_COMPLETE event will be received by the
 *         application.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized.
 */
BtStatus btmtk_hfg_set_indicator_value(HfgChannelContext *Channel, HfgIndicator Indicator, U8 value);


BtStatus btmtk_hfg_send_data(HfgChannelContext *Channel, const char *atStr, U16 atsize);
/*---------------------------------------------------------------------------
 * btmtk_hfg_get_indicator_value()
 *
 *     Gets the current value of the specified indicator.  
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Indicator - Indicator type.
 *
 *     Value - Receives the value of the indicator.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The indicator value was set and the Response
 *         structure is available for use.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized.
 */
BtStatus btmtk_hfg_get_indicator_value(HfgChannelContext *Channel, HfgIndicator Indicator, 
                               U8 *value);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_ok
 *     Sends an OK response to the hands-free device.  This function must
 *     be called after receiving several events (see the description of each
 *     event).
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_PENDING - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_ok(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_error()
 *
 *     Sends an ERROR result code to the HF.  This function may be called 
 *     after receiving several events when an error condition exists (see 
 *     the description of each event).  If extended error codes are enabled,
 *     the value specified in the 'Error' parameter will be sent with the
 *     extended error response.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Error - Extended error to be sent (if enabled).
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_error(HfgChannelContext *Channel, /*HfgCmeError*/U8 Error);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_ring()
 *
 *     Notifies the HF of an incoming call.  This call is repeated periodically
 *     as long as the call is still incoming.  If caller ID is enabled, a
 *     call to HFG_SendCallerId() should be called after calling HFG_SendRing().
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_ring(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_call_list_rsp
 *
 *     Sends the call listing response to the hands-free device (see
 *     HFG_EVENT_LIST_CURRENT_CALLS).  This function should be called for each 
 *     line supported on the audio gateway with the state of any call set 
 *     appropriately.  If no call is active on the specified line, a response 
 *     (+CLCC) will not be sent.  If a call is is any state besides 
 *     HFG_CALL_STATUS_NONE, then a response will be sent.  On the final call 
 *     to this function, FinalFlag should be set.  This will send an OK response 
 *     in addtion to +CLCC (if sent).
 *
 *     If it is known that no call exists on any line, it is acceptable to call 
 *     HFG_SendOK() instead of calling this function.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Parms - A structure containing the call status information for the
 *         specified line.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 *     FinalFlag -  Set to TRUE when the final call is make to this function.
 *
 * Returns:
 *     BT_STATUS_PENDING - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_call_list_rsp(HfgChannelContext *Channel, HfgCallListParms *Parms, kal_bool FinalFlag);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_caller_id()
 *
 *     Sends a Calling Line Identification result code containing the phone
 *     number and type of the incoming call.  This function should be called
 *     immediately after HFG_SendRing() if Calling Line Identification Notification
 *     has been enabled by the HF.  If caller ID notification has been disabled
 *     by the remote device, no notification will be sent even if this funcion
 *     is called.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     CallId - A structure containing the number and type of the 
 *         incoming call.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_PENDING - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NOT_SUPPORTED - Caller ID notification is disabled.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_caller_id(HfgChannelContext *Channel, HfgCallerIdParms *CallId);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_subscriber_number_rsp
 *
 *     This function is called in response to a request for the subscriber
 *     number (see HFG_EVENT_QUERY_SUBSCRIBER_NUMBER).  It is not necessary 
 *     to call HFG_SendOK() after calling this function.
 * 
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     SbuNum - A structure containing the subscriber number information.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_PENDING - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_subscriber_number_rsp(HfgChannelContext *Channel, HfgSubscriberNum *SubNum, kal_bool FinalFlag);

/*---------------------------------------------------------------------------
 * HFG_SendNetworkOperatorRsp
 *     This function is called in response to a request for the network 
 *     operator information (see HFG_EVENT_QUERY_NETWORK_OPERATOR).
 *     It is not necessary to call HFG_SendOK() after calling this function.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Oper - A structure containing the operator information.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_PENDING - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_network_operator_rsp(HfgChannelContext *Channel, HfgNetworkOperator *Oper);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_mic_volume()
 *
 * Notifies the HF of the AG's current microphone volume level. 
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     gain - current volume level.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The speaker volume level has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_mic_volume(HfgChannelContext *Channel, U8 Gain);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_speaker_volume()
 *
 * Notifies the HF of the AG's current speaker volume level.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     gain - current volume level.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The speaker volume level has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_speaker_volume(HfgChannelContext *Channel, U8 Gain);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_ringtone_status()
 *
 *     Notifies the HF of in-band ring tone status.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Enabled - TRUE if in-band ring tone enabled, FALSE otherwise.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_ringtone_status(HfgChannelContext *Channel, kal_bool Enabled);

//#if defined(HFG_USE_RESP_HOLD) && HFG_USE_RESP_HOLD == XA_ENABLED
/*---------------------------------------------------------------------------
 * btmtk_hfg_send_ringtone_status()
 *
 *     Notifies the HF of state of Response and Hold.  This function is called
 *     to report the Response and Hold state in response to a request by
 *     the hands-free unit (see HFG_RESPONSE_HOLD), or upon an action taken on 
 *     the audio gateway.
 *
 *     This function is also called in respone to a query for the Response
 *     and Hold state from the hands-free unit (see HFG_QUERY_RESPONSE_HOLD).
 *     This function should be called with the 'State' parameter set to 
 *     HFG_RESP_HOLD_STATE_HOLD if the audio gateway is in the Response and 
 *     Hold state, followed by a call to HFG_SendOK().  Otherwise, the 
 *     application should simply call HFG_SendOK().
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     state - The current Resonse and Hold state.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_response_hold_state(HfgChannelContext *Channel, HfgResponseHold State);
//#endif

/*---------------------------------------------------------------------------
 * btmtk_hfg_call_waiting()
 *
 *     Notifies the HF of a waiting call (if the HF has enabled the Call 
 *     Waiting Notification feature)
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     CallWait - A structure containing the number, type, and class of the 
 *         incoming call.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_call_waiting(HfgChannelContext *Channel, HfgCallWaitParms *CallWait);


/*---------------------------------------------------------------------------
 * btmtk_hfg_read_indicator()
 *
 *     Response of read indicator CMD (AT+CIND?). Returns current values of all indicators
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     ReadIndicator - A structure containing values of all supported indicators 
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_read_indicator(HfgChannelContext *Channel, HfgReadIndicatorParms *ReadIndicator);

/*---------------------------------------------------------------------------
 * btmtk_hfg_enable_vr()
 *
 *     Notifies the HF that voice recognition has been disabled (if the HF has
 *     activated the voice recognition functionality in the AG)
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Enabled - TRUE if voice recognition is active, otherwise FALSE>
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_enable_vr(HfgChannelContext *Channel, kal_bool Enabled);

/*---------------------------------------------------------------------------
 * btmtk_hfg_voice_tag_rsp()
 *
 *     Called by the app to return the phone number associated with the VoiceTag
 *     request to the HF.  It is not necessary to call HFG_SendOK() after 
 *     calling this function.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Number - A structure containing the phone number associated with the
 *         last voice tag.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_voice_tag_rsp(HfgChannelContext *Channel, const char *number);


BtStatus btmtk_hfg_supported_charset_response(HfgChannelContext *Channel, const char *supported);

BtStatus btmtk_hfg_selected_charset_response(HfgChannelContext *Channel, const char *selected);

BtStatus btmtk_hfg_model_id(HfgChannelContext *Channel, const char *modelId);

BtStatus btmtk_hfg_manufacture_id(HfgChannelContext *Channel, const char *manufactureId);

/*---------------------------------------------------------------------------
 * btmtk_hfg_no_carrier_rsp()
 *
 *     Called by the app to send the "NO CARRIER" response to the HF.  This
 *     response can be sent in addition to the "+CME ERROR:" response.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_no_carrier_rsp(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_busy_rsp()
 *
 *     Called by the app to send the "BUSY" response to the HF.  This
 *     response can be sent in addition to the "+CME ERROR:" response.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_busy_rsp(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_no_answer_rsp()
 *
 *     Called by the app to send the "NO ANSER" response to the HF.  This
 *     response can be sent in addition to the "+CME ERROR:" response.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_no_answer_rsp(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_delayed_rsp()
 *
 *     Called by the app to send the "DELAYED" response to the HF.  This
 *     response can be sent in addition to the "+CME ERROR:" response.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_delayed_rsp(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_blacklisted_rsp()
 *
 *     Called by the app to send the "BLACKLISTED" response to the HF.  This
 *     response can be sent in addition to the "+CME ERROR:" response.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Response - A response structure to be used for transmitting the
 *         response.
 *
 * Returns:
 *     BT_STATUS_SUCCESS - The result code has been sent to the HF.
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - A Service Level Connection does not exist.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_blacklisted_rsp(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_send_at_response()
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
 *         registered by btmtk_hfg_register).
 *
 *     BT_STATUS_NOT_FOUND - The specifiec channel has not been registered.
 *
 *     BT_STATUS_NO_CONNECTION - The operation failed because a service link
 *         does not exist to the audio gateway.
 *
 *     BT_STATUS_INVALID_PARM - A parameter is invalid or not properly 
 *         initialized (XA_ERROR_CHECK only).
 */
BtStatus btmtk_hfg_send_at_response(HfgChannelContext *Channel, const char *AtString);

/*---------------------------------------------------------------------------
 * btmtk_hfg_is_nrec_enabled()
 *
 *     Returns TRUE if Noise Reduction and Echo Cancelling is enabled in the
 *     audio gateway.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     TRUE - NREC is enabled in the AG.
 *
 *     FALSE - NREC is disabled in the AG.
 */
kal_bool btmtk_hfg_is_nrec_enabled(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_is_inbandring_Enabled()
 *
 *     Returns TRUE if In-band Ringing is enabled in the audio gateway.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     TRUE - In-band ringing is enabled in the AG.
 *
 *     FALSE - In-band ringing is disabled in the AG.
 */
kal_bool btmtk_hfg_is_inbandring_Enabled(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_is_callidnotify_enabled()
 *
 *     Returns TRUE if Caller ID notification is enabled in the audio gateway.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     TRUE - Caller ID notification is enabled in the AG.
 *
 *     FALSE - Caller ID notification is disabled in the AG.
 */
kal_bool btmtk_hfg_is_callidnotify_enabled(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_is_vr_active()
 *
 *     Returns TRUE if Voice Recognition is active in the audio gateway.  
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     TRUE - Voice Recognition is active in the AG.
 *
 *     FALSE - Voice Recognition is inactive in the AG.
 */
kal_bool btmtk_hfg_is_vr_active(HfgChannelContext *Channel);

/*---------------------------------------------------------------------------
 * btmtk_hfg_is_callwaiting_active()
 *
 *     Returns TRUE if Call Waiting is active in the audio gateway.  
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     TRUE - Call Waiting is active in the AG.
 *
 *     FALSE - Call Waiting is inactive in the AG.
 */
kal_bool btmtk_hfg_is_callwaiting_active(HfgChannelContext *Channel);

#if HFG_SNIFF_TIMER >= 0
/*---------------------------------------------------------------------------
 * btmtk_hfg_enable_sniff_mode
 *
 *     Enables/Disables placing link into sniff mode.
 *
 * Requires:
 *     HFG_SNIFF_TIMER >= 0.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Enabled - If TRUE, sniff mode will be used, otherwise sniff mode is
 *               disabled
 */
BtStatus btmtk_hfg_enable_sniff_mode(HfgChannelContext *Channel, kal_bool Enable);

/*---------------------------------------------------------------------------
 * btmtk_hfg_is_sniff_mode_enabled
 *
 *     Returns TRUE when sniff mode is enabled on the specified handler.
 *
 * Requires:
 *     HFG_SNIFF_TIMER >= 0.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 * Returns:
 *     TRUE if sniff mode is enabled.
 *
 *     FALSE if sniff mode is not enabled.
 */
kal_bool btmtk_hfg_is_sniff_mode_enabled(HfgChannelContext *Channel);
#define btmtk_hfg_is_sniff_mode_enabled(c) (CMGR_GetSniffTimer(&((c)->cmgrHandler)) > 0)
#endif

/*---------------------------------------------------------------------------
 * btmtk_hfg_set_sniff_exit_policy()
 *
 *     Sets the policy for exiting sniff mode on the specified channel.  The 
 *     policy can be set to HFG_SNIFF_EXIT_ON_SEND or HFG_SNIFF_EXIT_ON_AUDIO_LINK.
 *     These values can also be OR'd together to enable both (See 
 *     HfgSniffExitPolicy).
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 * 
 *     Policy - Bitmask that defines the policy for exiting sniff mode.
 *
 * Returns:
 *
 *     BT_STATUS_SUCCESS - The policy was set successfully.
 *
 *     BT_STATUS_NOT_FOUND - Could not set the sniff policy, because  
 *         Handler is not registered.
 */
#if 0
BtStatus btmtk_hfg_set_sniff_exit_policy(HfgChannelContext *Channel, HfgSniffExitPolicy Policy);
#define btmtk_hfg_set_sniff_exit_policy(c, p) CMGR_SetSniffExitPolicy(&((c)->cmgrHandler), (p));
#endif
/*---------------------------------------------------------------------------
 * btmtk_hfg_get_sniff_exit_policy()
 *
 *     Gets the policy for exiting sniff mode on the specified channel.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 * 
 * Returns:
 *
 *     HfSniffExitPolicy
 */
#if 0
HfgSniffExitPolicy btmtk_hfg_get_sniff_exit_policy(HfgChannelContext *Channel);
#define btmtk_hfg_get_sniff_exit_policy(c)  CMGR_GetSniffExitPolicy(&((c)->cmgrHandler))
#endif
/*---------------------------------------------------------------------------
 * btmtk_hfg_set_master_role()
 *
 *     Attempts to keep the local device in the Master role.
 *
 * Parameters:
 *     Channel - Pointer to a registered channel structure.
 *
 *     Flag - TRUE if this device wants to be the master, otherwise FALSE.
 * 
 * Returns:
 *
 *     BtStatus
 */
BtStatus btmtk_hfg_set_master_role(HfgChannelContext *Channel, kal_bool Flag);
#define btmtk_hfg_set_master_role(c, f)  CMGR_SetMasterRole(&((c)->cmgrHandler), f)


/*******************************************************************
*   Phonebook related result
*******************************************************************/
BtStatus btmtk_hfg_send_pb_supported_storage_rsp(HfgChannelContext *Channel, HfgPbStorageSupported *supported);
BtStatus btmtk_hfg_send_pb_selected_storage_rsp(HfgChannelContext *Channel, HfgPbStorageSelected *selected);
BtStatus btmtk_hfg_send_pb_read_info_rsp(HfgChannelContext *Channel, HfgPbReadInfo *info);
BtStatus btmtk_hfg_send_pb_read_rsp(HfgChannelContext *Channel, HfgPbEntry *entry, kal_bool FinalFLag);
BtStatus btmtk_hfg_send_pb_find_info_rsp(HfgChannelContext *Channel, HfgPbFindInfo *info);
BtStatus btmtk_hfg_send_pb_find_rsp(HfgChannelContext *Channel, HfgPbEntry *entry, kal_bool FinalFLag);
BtStatus btmtk_hfg_send_pb_write_info_rsp(HfgChannelContext *Channel, HfgPbWriteInfo *info);

/*******************************************************************
*   SMS related result
*******************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_SMS_SERVICE_REQ	*/
BtStatus btmtk_hfg_send_sms_supported_service_rsp(HfgChannelContext *Channel, const char *supported);
/* MSG_ID_BT_HFG_SELECTED_SMS_SERVICE_REQ		*/	
BtStatus btmtk_hfg_send_sms_selected_service_rsp(HfgChannelContext *Channel, HfgSMSService_read *selected);
/* MSG_ID_BT_HFG_SMS_SERVICE_REQ				*/
BtStatus btmtk_hfg_send_sms_select_service_rsp(HfgChannelContext *Channel, HfgSMSService_result *service);
/* MSG_ID_BT_HFG_SUPPORTED_PREF_MSG_STORAGE_REQ 			*/
BtStatus btmtk_hfg_send_sms_supported_pref_storage_rsp(HfgChannelContext *Channel, HfgSMSPrefStorage_test *supported);
/* MSG_ID_BT_HFG_SELECTED_PREF_MSG_STORAGE_REQ				*/
BtStatus btmtk_hfg_send_sms_selected_pref_storage_rsp(HfgChannelContext *Channel, HfgSMSPrefStorage_read *selected);
/* MSG_ID_BT_HFG_PREF_MSG_STORAGE_REQ							*/
BtStatus btmtk_hfg_send_sms_select_pref_storage_rsp(HfgChannelContext *Channel, HfgSMSPrefStorage_result *pref);
/* MSG_ID_BT_HFG_SUPPORTED_MSG_FORMAT_REQ						*/	
BtStatus btmtk_hfg_send_sms_supported_format_rsp(HfgChannelContext *Channel, const char *supported);
/* MSG_ID_BT_HFG_SELECTED_MSG_FORMAT_REQ						*/
BtStatus btmtk_hfg_send_sms_selected_format_rsp(HfgChannelContext *Channel, U8 format);
/* MSG_ID_BT_HFG_SERVICE_CENTRE_REQ 						*/
BtStatus btmtk_hfg_send_sms_service_centre_rsp(HfgChannelContext *Channel, HfgSMSSrviceCentre_read *sc);
/* MSG_ID_BT_HFG_TEXT_MODE_PARAMS_REQ							*/
BtStatus btmtk_hfg_send_sms_text_mode_params_rsp(HfgChannelContext *Channel, HfgSMSTextModeParam_read *params);
/* MSG_ID_BT_HFG_SUPPORTED_SHOW_PARAMS_REQ					*/
BtStatus btmtk_hfg_send_sms_supported_show_params_rsp(HfgChannelContext *Channel, const char *supported);
/* MSG_ID_BT_HFG_SELECTED_SHOW_PARAMS_REQ						*/
BtStatus btmtk_hfg_send_sms_selected_show_params_rsp(HfgChannelContext *Channel, U8 show);
/* MSG_ID_BT_HFG_SUPPORTED_NEW_MSG_INDICATION_REQ			*/
BtStatus btmtk_hfg_send_sms_supported_new_msg_indication_rsp(HfgChannelContext *Channel, const char *supported);
/* MSG_ID_BT_HFG_SELECTED_NEW_MSG_INDICATION_REQ				*/
BtStatus btmtk_hfg_send_sms_selected_new_msg_indication_rsp(HfgChannelContext *Channel, HfgSMSIndSetting_read *selected);
/* MSG_ID_BT_HFG_NEW_MSG_INDICATION_REQ				*/
BtStatus btmtk_hfg_send_sms_new_msg_indication(HfgChannelContext *Channel, HfgSMSNewMsgInd *newMsg);
/* MSG_ID_BT_HFG_SUPPORTED_LIST_STATUS_REQ						*/
BtStatus btmtk_hfg_send_sms_supported_list_status_rsp(HfgChannelContext *Channel, const char *supported);
/* MSG_ID_BT_HFG_LIST_MSG_REQ									*/
BtStatus btmtk_hfg_send_sms_list_msg_rsp(HfgChannelContext *Channel, HfgSMSList_result *info, kal_bool FinalFlag);
/* MSG_ID_BT_HFG_READ_MSG_REQ									*/
BtStatus btmtk_hfg_send_sms_read_msg_rsp(HfgChannelContext *Channel, HfgSMSRead_result *info);
/* MSG_ID_BT_HFG_SEND_MSG_REQ									*/
BtStatus btmtk_hfg_send_sms_send_msg_rsp(HfgChannelContext *Channel, HfgSMSSend_result *rsp);
/* MSG_ID_BT_HFG_SEND_STORED_MSG_REQ							*/
BtStatus btmtk_hfg_send_sms_send_stored_msg_rsp(HfgChannelContext *Channel, HfgSMSSendStored_result *rsp);
/* MSG_ID_BT_HFG_WRITE_MSG_REQ									*/
BtStatus btmtk_hfg_send_sms_write_msg_rsp(HfgChannelContext *Channel, U16 index);
/* MSG_ID_BT_HFG_SMS_ERROR_REQ									*/
BtStatus btmtk_hfg_send_sms_error(HfgChannelContext *Channel, U16 error);

#ifdef BT_HFG_UT_TEST
BtStatus btmtk_hfg_send_ut_init_req(HfgChannelContext *Channel);
/* MSG_ID_BT_HFG_UT_TX_REQ */
BtStatus btmtk_hfg_send_ut_tx_req(HfgChannelContext *Channel, const char *buf, U16 len);
#endif /* BT_HFG_UT_TEST */

#ifdef __cplusplus
}
#endif

#endif /* __BT_HFG_H__ */
