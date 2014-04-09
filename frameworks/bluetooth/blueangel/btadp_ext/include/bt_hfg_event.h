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

#ifndef __BT_HFG_EVENT_H__
#define __BT_HFG_EVENT_H__

#ifdef SOURCE_INSIGHT_TRACE
typedef enum 
{
#endif

EVENT_HFG_SERVICE_CONNECT_REQ,

/** A service level connection has been established.  This can happen as the
 *  result of a call to HFG_CreateServiceLink, or if the hands-free device 
 *  establishes the service connection.  When this event has been received, a 
 *  data connection is available for sending to the hands-free device.
 *
 *  This event can also occur when an attempt to create a service level 
 *  connection (HFG_CreateServiceLink) fails.
 *
 *  When this callback is received, the "HfgCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.  In addition, the
 *  "HfgCallbackParms.errCode" fields contains the reason for disconnect.
 */
EVENT_HFG_SERVICE_CONNECTED,

/** The service level connection has been released.  This can happen as the
 *  result of a call to HFG_DisconnectServiceLink, or if the hands-free device
 *  releases the service connection.  Communication with the hands-free device 
 *  is no longer possible.  In order to communicate with the hands-free device,
 *  a new service level connection must be established.
 *
 *  This event can also occur when an attempt to create a service level 
 *  connection (HFG_CreateServiceLink) fails.
 *
 *  When this callback is received, the "HfgCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.  In addition, the
 *  "HfgCallbackParms.errCode" fields contains the reason for disconnect.
 */
EVENT_HFG_SERVICE_DISCONNECTED,

/** An audio connection has been established.  This event occurs whenever the
 *  audio channel (SCO) comes up, whether it is initiated by the audio gateway
 *  or the hands-free unit.
 *
 *  When this callback is received, the "HfgCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.
 */
EVENT_HFG_AUDIO_CONNECTED,

/** An audio connection has been released.  This event occurs whenever the
 *  audio channel (SCO) goes down, whether it is terminated by the audio gateway
 *  or the hands-free unit.
 *
 *  When this callback is received, the "HfgCallbackParms.p.remDev" field 
 *  contains a pointer to the remote device context.  In addition, the
 *  "HfgCallbackParms.errCode" fields contains the reason for disconnect.
 */
EVENT_HFG_AUDIO_DISCONNECTED,
 
/** After the service level connection has been established, this event will
 *  indicate the features supported on the hands-free unit.  
 *
 *  When this callback is received, the "HfgCallbackParms.p.features" field 
 *  contains the features (see HfgFeatures).
 */
EVENT_HFG_HANDSFREE_FEATURES,

//#if BT_SCO_HCI_DATA == XA_ENABLED
/** Only valid if BT_SCO_HCI_DATA is set to XA_ENABLED.  Audio data has been 
 *  received from the remote device.  The data is only valid during the
 *  callback.
 *
 *  When this callback is received, the "HfCallbackParms.p.audioData" field 
 *  contains the audio data.
 */
EVENT_HFG_AUDIO_DATA,

/** Only valid if BT_SCO_HCI_DATA is set to XA_ENABLED.  Audio data has been 
 *  sent to the remote device.  This event is received by the application when
 *  the data sent by HFG_SendAudioData has been successfully sent.
 *
 *  When this callback is received, the "HfCallbackParms.p.audioPacket" field 
 *  contains the result.
 */
EVENT_HFG_AUDIO_DATA_SENT,
//#endif

/** The hands-free unit has requested the audio gateway to answer the incoming 
 *  call.  When the call has been answered, the application should call 
 *  HFG_SendOK().  In addtion, the appropriate indicators should be updated.
 */
EVENT_HFG_ANSWER_CALL,

/** The hands-free unit has requested the audio gateway to place a call. The
 *  application should dial the number and respond with a call to HFG_SendOK().
 *  In addtion, the appropriate indicators should be updated 
 *  (see HFG_SetIndicatorValue()).
 *
 *  When this callback is received, the "HfgCallbackParms.p.phoneNumber" 
 *  parameter is valid for this event.
 */
EVENT_HFG_DIAL_NUMBER,

/** The hands-free unit has requested the audio gateway to place a call to 
 *  the phone number associated with the given memory location.  The
 *  application should dial the number and respond with a call to HFG_SendOK().
 *  In addtion, the appropriate indicators should be updated 
 *  (see HFG_SetIndicatorValue()).
 *
 *  When this callback is received, the "HfgCallbackParms.p.memory" parameter 
 *  is valid for this event.
 */
EVENT_HFG_MEMORY_DIAL,

/** The hands-free unit has requested the audio gateway to place a call to 
 *  the last number dialed.  The application should dial the last number dialed
 *  and respond with a call to HFG_SendOK().  In addtion, the appropriate 
 *  indicators should be updated (see HFG_SetIndicatorValue()).
 */
EVENT_HFG_REDIAL,

/** The hands-free unit has requested the audio gateway to place the current 
 *  call on hold.  The application should make the appropriate hold action
 *  and respond with a cll to HFG_SendOK().  In addtion, the appropriate 
 *  indicators should be updated (see HFG_SetIndicatorValue()).
 *
 *  When this callback is received, the "HfgCallbackParms.p.hold" parameter 
 *  is valid for this event.
 */
EVENT_HFG_CALL_HOLD,

/** This event is only available when HFG_USE_RESP_HOLD is set to XA_ENABLED.
 *  The Response and Hold state has been requested from the handsfree device.
 *  The audio gateway should respond by calling HFG_SendResponseHoldState().
 *  If the the audio gateway is in any Response and Hold state, then the 
 *  application should respond by calling HFG_SendResponseHoldState() with
 *  the state set to HFG_RESP_HOLD_STATE_HOLD, followed by a call to
 *  HFG_SendOK(), othwerwise, the application should simply call HFG_SendOK()
 *  
 */
EVENT_HFG_QUERY_RESPONSE_HOLD,

/** This event is only available when HFG_USE_RESP_HOLD is set to XA_ENABLED.
 *  A Response and Hold request has been received from the handsfree device.
 *  The audio gateway should take the appropriate action and respond by calling
 *  HFG_SendResponseHoldState() with the state set to the requested state.  
 *  In addtion, the appropriate indicators should be updated 
 *  (see HFG_SetIndicatorValue()).
 * 
 *  When this callback is received, the "HfCallbackParms.p.respHold" field 
 *  contains the result.
 */
EVENT_HFG_RESPONSE_HOLD,

/** The hands-free unit has requested the audio gateway to hang up the current 
 *  call.  The application should hang up the call and respond with a call
 *  to HFG_SendOK().  In addtion, the appropriate indicators should be updated 
 *  (see HFG_SetIndicatorValue()).
 */
EVENT_HFG_HANGUP,

/** The list of current calls has been requested from the hands-free device.
 *  The audio gateway should respond by calling HFG_SendCallListRsp() once
 *  for each line supported by the device.
 */                                     
EVENT_HFG_LIST_CURRENT_CALLS,

/** The hands-free unit has requested that Calling Line Identification
 * notification be enabled or disabled.  
 *
 * The "HfgCallbackParms.p.enabled" parameter indicates the type of request. 
 * If "enabled" is FALSE, the application may call HFG_SendCallerId(), but no
 * indication will be sent to the remote device.
 */
EVENT_HFG_ENABLE_CALLER_ID,

/** The hands-free unit has requested that Call Waiting notification be enabled
 *  or disabled. 
 *
 *  The "HfgCallbackParms.p.enabled" parameter indicates the type of request.  
 *  If "enabled" is FALSE, the application may call HFG_CallWaiting(), but no
 *  indication will be sent to the remote device.
 */
EVENT_HFG_ENABLE_CALL_WAITING,

/** The hands-free unit has requested the audio gateway to transmit a 
 *  specific DTMF code to its network connection. The "dtmf" parameter 
 *  will contain the requested DTMF code.
 *
 *  When the tone has been sent to the network, call HFG_SendOk().
 */
EVENT_HFG_GENERATE_DTMF,


/** The hands-free unit has requested the phone number associated with the
 *  last voice tag in the audio gateway in order to link its own voice tag to 
 *  the number.  The hands-free unit may then dial the linked phone numbers 
 *  when a voice tag is recognized.  (This procedure is only applicable for 
 *  hands-free units that support their own voice recognition functionality).
 *  The HFG_FEATURE_VOICE_TAG bit must be set in HFG_SDK_FEATURES in order
 *  to use this event.
 *
 *  In response to this event, call HFG_VoiceTagResponse() with a number for
 *  tagging, or call HFG_SendError() to reject the request.
 */
EVENT_HFG_GET_LAST_VOICE_TAG,



/** Enables/disables the voice recognition function resident in the audio
 *  gateway (as indicated by the "enabled" parameter). If the HF enables voice
 *  recognition, the audio gateway must keep the voice recognition enabled 
 *  until either:
 *
 *      1) The HF disables it.
 *      2) The service link is disconnected.
 *      3) The duration of time supported by the audio gateway's 
 *         implementation has elapsed.
 *
 * In this last case the audio gateway must notify the hands-free unit that 
 * it has disabled voice recognition by calling HFG_DisableVoiceRec().
 * The HFG_FEATURE_VOICE_RECOGNITION bit must be set in HFG_SDK_FEATURES in order
 * to receive this event.
 */
EVENT_HFG_ENABLE_VOICE_RECOGNITION,



/** The hands-free unit has requested the audio gateway to disable the noise
 *  reduction and echo canceling (NREC) functions resident in the audio 
 *  gateway.
 *
 *  If the audio gateway supports NREC it must disable these features for 
 *  the duration of the service link.  The HFG_FEATURE_ECHO_NOISE bit must be 
 *  set in HFG_SDK_FEATURES in order to receive this event.
 */
EVENT_HFG_DISABLE_NREC,


/** The hands-free has informed the audio gateway of its microphone volume 
 *  level. 
 *
 *  The "HfgCallbackParms.p.gain" parameter is valid.
 */
EVENT_HFG_REPORT_MIC_VOLUME,

/** The hands-free has informed the audio gateway of its speaker volume 
 *  level. 
 *
 *  The "HfgCallbackParms.p.gain" parameter is valid.
 */
EVENT_HFG_REPORT_SPK_VOLUME,

/** The hands-free device has requested the network operator from the
 *  audio gateway.  The audio gateway should respond by calling
 *  HFG_SendNetworkOperatorRsp().
 */
EVENT_HFG_QUERY_NETWORK_OPERATOR,
 
/** The hands-free device has requested the subscriber number from the
 *  audio gateway.  The audio gateway should respond by calling
 *  HFG_SendSubscriberNumberRsp().
 */
EVENT_HFG_QUERY_SUBSCRIBER_NUMBER,

/** The hands-free device has requested that extended error codes be enabled.
 *  When extended errors are enabled, a call to HFG_SendError() will send
 *  extended errors, otherwise it will only repspond with an "ERROR" response.
 */
EVENT_HFG_ENABLE_EXTENDED_ERRORS,

/** An unsupported AT command has been received from the audio gateway.  This 
 *  event is received for AT commands that are not handled by the internal 
 *  Hands-free AT parser.  The application must make an appropriate response
 *  and call HFG_SendOK() to complete the response.
 *
 *  When this callback is received, the "HfgCallbackParms.p.data" field 
 *  contains the AT command data.
 */
EVENT_HFG_AT_COMMAND_DATA,

/** Whenever a response has been set to the remote device, this event is
 *  received to confirm that the repsonse was sent.
 * 
 *  When this event is received, the "HfgCallbackParms.p.response" field
 *  contains a pointer to the response structure that was used to send
 *  the reponse.
 */
EVENT_HFG_RESPONSE_COMPLETE,

/** 
 *  When RFCOMM is connected, HFP still have to run negotiation process to 
 *  complete the SLC connection. HFP sends EVENT_HFG_RFCOMM_CONNECTED
 *  to inform ADP the RFCOMM channel is connected and ready to negotiate. ADP
 *  can can do some proprietary actions.
 */
EVENT_HFG_RFCOMM_CONNECTED,

/** 
 *  When receive RFEVENT_OPEN_IND, HFP request ADP by sending EVENT_HFG_AUTH_REQ
 *  to confirm if it want to accept the connection request.
 */
EVENT_HFG_AUTH_REQ,

/** 
 *  When creating SLC, HFP need to negotiation with HF device. HF device have to read indicator 
 *  values from AG. HFP will receive AT+CIND CMD and it does not know the current values of indicators.
 *  So HFP will send event EVENT_HFG_READ_INDICATORS to ADP and ADP has to pass current values 
 *  of indicators to HFP.
 */
EVENT_HFG_READ_INDICATORS,

/** 
 *  This event indicate the button on the headset is pressed. It shall be only received when connected 
 *  to headset no HF device
 */
EVENT_HFG_KEYPAD_CONTROL,

/** 
 *  This event is only used when the message based interface is used. It is used to confirm the result 
 *  of activation
 */
EVENT_HFG_ACTIVATE_CONFIRM,

/** 
 *  This event is only used when the message based interface is used. It is used to confirm the result 
 *  of deactivation
 */
EVENT_HFG_DEACTIVATE_CONFIRM,

EVENT_HFG_CONNECT_CONFIRM,
EVENT_HFG_ACCEPT_CHANNEL_CONFIRM,
EVENT_HFG_REJECT_CHANNEL_CONFIRM,
EVENT_HFG_DISCONNECT_CONFIRM,
EVENT_HFG_SCO_CONNECT_CONFIRM,
EVENT_HFG_SCO_DISCONNECT_CONFIRM,
/* End of HfgEvent */
EVENT_HFG_QUERY_SUPPORTED_CHARSET,
EVENT_HFG_QUERY_SELECTED_CHARSET,
EVENT_HFG_SELECT_CHARSET	,

EVENT_HFG_QUERY_MODEL_ID,
EVENT_HFG_QUERY_MANUFACTURE_ID,

/****************************************************************************
*   Phone Book event
****************************************************************************/
/* +CPBS */
EVENT_HFG_QUERY_SUPPORTED_PHONEBOOK,			/* AT+CPBS=<pb> */
EVENT_HFG_SELECT_PHONEBOOK,						/* AT+CPBS=? */
EVENT_HFG_QUERY_SELECTED_PHONEBOOK,			/* AT+CPBS? */
/* +CPBR */
EVENT_HFG_QUERY_READ_PBENTRY_INFO,				/* AT+CPBR=<n> */
EVENT_HFG_READ_PBENTRY,							/* AT+CPBR=? */
/* +CPBF */
EVENT_HFG_QUERY_FIND_PBENTRY_INFO,				/* AT+CPBF=? */
EVENT_HFG_FIND_PBENTRY,							/* AT+CPBF=<n> */
/* +CPBW */
EVENT_HFG_QUERY_WRITE_PBENTRY_INFO,			/* AT+CPBW */
EVENT_HFG_WRITE_PBENTRY,						/* AT+CPBW=? */
/* End of HfgEvent */

/****************************************************************************
*   SMS event
****************************************************************************/
/* AT_SELECT_SMS_SERVICE */
EVENT_HFG_QUERY_SUPPORTED_SMS_SERVICE,		/* AT+CSMS=? */
EVENT_HFG_QUERY_SELECTED_SMS_SERVICE,			/* AT+CSMS? */
EVENT_HFG_SELECT_SMS_SERVICE,					/* AT+CSMS=<service> */
/* AT_PREFERRED_SMS_STORAGE */
EVENT_HFG_QUERY_SUPPORTED_PREF_MSG_STORAGE, /* AT+CPMS=? */
EVENT_HFG_QUERY_SELECTED_PREF_MSG_STORAGE,	/* AT+CPMS? */
EVENT_HFG_SELECT_PREF_MSG_STORAGE,				/* AT+CPMS=<mem1>[,<mem2>[,<mem3>]] */
/* AT_SMS_MESSAGE_FORMAT */
EVENT_HFG_QUERY_SUPPORTED_MSG_FORMAT,		/* AT+CMGF=? */
EVENT_HFG_QUERY_SELECTED_MSG_FORMAT,			/* AT+CMGF? */
EVENT_HFG_SELECT_MSG_FORMAT,					/* AT+CMGF=<mode> */
/* AT_SMS_SERVICE_CENTER */
EVENT_HFG_QUERY_SERVICE_CENTRE	,				/* AT+CSCA? */
EVENT_HFG_SET_SERVICE_CENTRE,					/* AT+CSCA=<sca>[,<tosca>] */
/* AT_SET_TEXT_MODE_PARMS */
EVENT_HFG_QUERY_TEXT_MODE_PARAMS,				/* AT+CSMP? */
EVENT_HFG_SET_TEXT_MODE_PARAMS,				/* AT+CSMP=[<fo>[,<vp>[,<pid>[,<dcs>]]]] */
/* AT_SMS_SHOW_TEXT_MODE */
EVENT_HFG_QUERY_SUPPORTED_SHOW_PARAMS,		/* AT+CSDH=? */
EVENT_HFG_QUERY_SELECTED_SHOW_PARAMS,		/* AT+CSDH? */
EVENT_HFG_SET_SHOW_PARAMS,						/* AT+CSMP=[<fo>[,<vp>[,<pid>[,<dcs>]]]] */
/* AT_NEW_MESSAGE_INDICATION */
EVENT_HFG_QUERY_SUPPORTED_NEW_MSG_INDICATION,	/* AT+CNMI=? */
EVENT_HFG_QUERY_SELECTED_NEW_MSG_INDICATION,		/* AT+CNMI? */
EVENT_HFG_SET_NEW_MSG_INDICATION,					/* AT+CNMI=[<mode>[,<mt>[,<bm>[,<ds>[,<bfr>]]]]] */
/* AT_LIST_MESSAGES */
EVENT_HFG_QUERY_SUPPORTED_LIST_STATUS,		/* AT+CMGL=? */
EVENT_HFG_LIST_MSG,								/* AT+CMGL[=<stat>] */
/* AT_READ_MESSAGE */
EVENT_HFG_READ_MSG,								/* AT+CMGR=<index> */
/* AT_SEND_MESSAGE */
EVENT_HFG_SEND_MSG,								/* TEXT MODE : AT+CMGS=<da>[,<toda>]<CR>text is entered<ctrl-Z/ESC> */
													/* PDU MODE : AT+CMGS=<length><CR>PDU is given<ctrl-Z/ESC> */
/* AT_SEND_STORED_MESSAGE */
EVENT_HFG_SEND_STORED_MSG,						/* AT+CMSS=<index>[,<da>[,<toda>]] */
/* AT_STORE_MESSAGE */
EVENT_HFG_WRITE_MSG,								/* TEXT MODE : AT+CMGW=<oa/da>[,<toda/toda>[,<stat>]]<CR>text is entered<ctrl-Z/ESC> */
													/* PDU MODE : AT+CMGW=<length>[,<stat>]<CR>PDU is given<ctrl-Z/ESC> */
/* AT_DELETE_MESSAGE */
EVENT_HFG_DELETE_MSG,							/* AT+CMGD=<index> */

/* For WISE target, delay sending call related indication to prevent unnecessary indication */
EVENT_HFG_DELAY_CALL_STATE_CHANGE,

#ifdef BT_HFG_UT_TEST
EVENT_HFG_UT_INIT_CNF,
EVENT_HFG_UT_RX_IND,
#endif

#ifdef SOURCE_INSIGHT_TRACE
}HFG_EVENT;
#endif
#endif /* __BT_HFG_EVENT_H__ */