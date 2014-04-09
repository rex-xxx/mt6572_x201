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

#ifndef __BT_CHN_EVENT_H__
#define __BT_CHN_EVENT_H__

#ifdef SOURCE_INSIGHT_TRACE
typedef enum 
{
#endif

/** A service level connection has been established.  This can happen as the
 *  result of a call to HFG_CreateServiceLink, or if the hands-free device 
 *  establishes the service connection.  When this event has been received, a 
 *  data connection is available for sending to the hands-free device.
 *
 *  This event can also occur when an attempt to create a service level 
 *  connection (HFG_CreateServiceLink) fails.
 *
 *  When this callback is received, the "ChnCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.  In addition, the
 *  "ChnCallbackParms.errCode" fields contains the reason for disconnect.
 */
EVENT_CHN_SERVICE_CONNECTED,

/** The service level connection has been released.  This can happen as the
 *  result of a call to HFG_DisconnectServiceLink, or if the hands-free device
 *  releases the service connection.  Communication with the hands-free device 
 *  is no longer possible.  In order to communicate with the hands-free device,
 *  a new service level connection must be established.
 *
 *  This event can also occur when an attempt to create a service level 
 *  connection (HFG_CreateServiceLink) fails.
 *
 *  When this callback is received, the "ChnCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.  In addition, the
 *  "ChnCallbackParms.errCode" fields contains the reason for disconnect.
 */
EVENT_CHN_SERVICE_DISCONNECTED,

/** An audio connection has been established.  This event occurs whenever the
 *  audio channel (SCO) comes up, whether it is initiated by the audio gateway
 *  or the hands-free unit.
 *
 *  When this callback is received, the "ChnCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.
 */
EVENT_CHN_AUDIO_CONNECTED,

/** An audio connection has been released.  This event occurs whenever the
 *  audio channel (SCO) goes down, whether it is terminated by the audio gateway
 *  or the hands-free unit.
 *
 *  When this callback is received, the "ChnCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.  In addition, the
 *  "ChnCallbackParms.errCode" fields contains the reason for disconnect.
 */
EVENT_CHN_AUDIO_DISCONNECTED,
 
/** An unsupported AT command has been received from the audio gateway.  This 
 *  event is received for AT commands that are not handled by the internal 
 *  Hands-free AT parser.  The application must make an appropriate response
 *  and call HFG_SendOK() to complete the response.
 *
 *  When this callback is received, the "ChnCallbackParms.p.data" field 
 *  contains the AT command data.
 */
EVENT_CHN_RX_DATA,

/** 
 *  When receive RFEVENT_OPEN_IND, HFP request ADP by sending EVENT_CHN_AUTH_REQ
 *  to confirm if it want to accept the connection request.
 */
EVENT_CHN_AUTH_REQ,

/** 
 *  This event is only used when the message based interface is used. It is used to confirm the result 
 *  of activation
 */
EVENT_CHN_ACTIVATE_CONFIRM,

/** 
 *  This event is only used when the message based interface is used. It is used to confirm the result 
 *  of deactivation
 */
EVENT_CHN_DEACTIVATE_CONFIRM,

EVENT_CHN_CONNECT_CONFIRM,
EVENT_CHN_ACCEPT_CHANNEL_CONFIRM,
EVENT_CHN_REJECT_CHANNEL_CONFIRM,
EVENT_CHN_DISCONNECT_CONFIRM,
EVENT_CHN_SCO_CONNECT_CONFIRM,
EVENT_CHN_SCO_DISCONNECT_CONFIRM,
/* End of ChnEvent */

#ifdef SOURCE_INSIGHT_TRACE
}CHN_EVENT;
#endif
#endif /* __BT_CHN_EVENT_H__ */
