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

#ifndef __BLUETOOTH_HFG_COMMON__
#define __BLUETOOTH_HFG_COMMON__

#include "bt_types.h"

/***************************************************************************** 
* Typedef 
*****************************************************************************/
/*--------------------------------------------------------------------------
 * HfgHandsFreeVersion type
 *      
 *     Handsfree profile version discovered during the SDP query.  The service
 *     connection will be limited to the capabilities of this profile version.
 *-------------------------------------------------------------------------*/
typedef U16  HfgHandsFreeVersion;

/* Unable to determine the Hands Free Profile version that is supported */
#define HFG_HF_VERSION_UNKNOWN 0x0000

/* Supports Version 0.96 of the Hands Free Profile */
#define HFG_HF_VERSION_0_96    0x0100

/* Supports Version 1.0 of the Hands Free Profile */
#define HFG_HF_VERSION_1_0     0x0101

/* Supports Version 1.5 of the Hands Free Profile */
#define HFG_HF_VERSION_1_5     0x0105
/*--------------------------------------------------------------------------
 * End of HfgHandsFreeVersion 
 *-------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------
 * HfgHandsFreeFeatures type
 *      
 *  Bit mask specifying the HF's optional feature set.
 *-------------------------------------------------------------------------*/
typedef U32  HfgHandsFreeFeatures;

/** Echo canceling and/or noise reduction function */
#define HFG_HANDSFREE_FEATURE_ECHO_NOISE            0x00000001

/** Call-waiting and 3-way calling */
#define HFG_HANDSFREE_FEATURE_CALL_WAITING          0x00000002

/** CLI presentation capability */
#define HFG_HANDSFREE_FEATURE_CLI_PRESENTATION      0x00000004

/** Voice recognition function */
#define HFG_HANDSFREE_FEATURE_VOICE_RECOGNITION     0x00000008

/** Remote volume control */
#define HFG_HANDSFREE_FEATURE_VOLUME_CONTROL        0x00000010

/* Enhanced call status */
#define HFG_HANDSFREE_FEATURE_ENHANCED_CALL_STATUS  0x00000020

/* Enhanced call control */
#define HFG_HANDSFREE_FEATURE_ENHANCED_CALL_CTRL    0x00000040
/*--------------------------------------------------------------------------
 * End of HfgHandsFreeFeatures
 *-------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------
 * HfgIndicator type
 *      
 *     Possible indicator events to be reported to the HF via HFG_ReportEvent().
 *-------------------------------------------------------------------------*/
typedef U8  HfgIndicator;

/** Service indicator.  The type associated with this event is a BOOL.  The 
 *  value associated with this event is TRUE if service exists or FALSE
 *  if no service exists.
 */
#define HFG_IND_SERVICE       0

/** Call indicator.  The type associated with this event is a BOOL.  The 
 *  value associated with this event is TRUE if a call exists or FALSE
 *  if no call exists.
 */
#define HFG_IND_CALL          1

/** Call setup.  The type associated with this event is a HfgCallSetup.  
 */
#define HFG_IND_CALL_SETUP    2

/** Call held.  The type associated with this event is a HfgHoldState.  The 
 *  value associated with this event is as follows:
 *   0 - no call is held
 *   1 - one call is held and one call is active
 *   2 - either one call is held and one is inactive, or two calls are held
 */
#define HFG_IND_CALL_HELD     3

/** Battery level.  The type associated with this indicator is a U8.  
 *  The value associated with this indicator is a number between 0 and 5.
 */
#define HFG_IND_BATTERY       4

/** Signal Strength.  The type associated with this indicator is a U8.  
 *  The value associated with this indicator is a number between 0 and 5.
 */
#define HFG_IND_SIGNAL        5

/** Roaming.  The type associated with this indicator is a BOOL.  
 *  The value associated with this indicator is TRUE when roaming and 
 *  FALSE when not roaming.
 */
#define HFG_IND_ROAMING       6

/* Number of supported indicators
*/
#define NUM_OF_HFG_IND          7
/*--------------------------------------------------------------------------
 * End of HfgIndicator
 *-------------------------------------------------------------------------*/

/****************************************************************************
 *   AT related definitions & types
 ***************************************************************************/
/*---------------------------------------------------------------------------
 * HfgCallStatus type
 *
 * Defines the current state of a call. 
 *--------------------------------------------------------------------------*/
typedef U8 HfgCallStatus;

/** An active call exists.
 */
#define HFG_CALL_STATUS_ACTIVE     0

/** The call is held.
 */
#define HFG_CALL_STATUS_HELD       1

/** A call is outgoing.
 */
#define HFG_CALL_STATUS_DIALING    2

/** The remote parting is being alerted.
 */
#define HFG_CALL_STATUS_ALERTING   3

/** A call is incoming.
 */
#define HFG_CALL_STATUS_INCOMING   4

/** The call is waiting.  This state occurs only when the audio gateway supports
 *  3-Way calling.
 */
#define HFG_CALL_STATUS_WAITING    5

/** No active call
 */
#define HFG_CALL_STATUS_NONE       0xFF
/*--------------------------------------------------------------------------
 * End of HfgCallStatus
 *-------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------
 * HfgCallMode type
 *
 * Defines the current mode of a call.
 *-------------------------------------------------------------------------*/
typedef U8 HfgCallMode;

/* Voice Call */
#define HFG_CALL_MODE_VOICE     0

/* Data Call */
#define HFG_CALL_MODE_DATA      1

/* FAX Call */
#define HFG_CALL_MODE_FAX       2
/*--------------------------------------------------------------------------
 * End of HfgCallMode
 *-------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------
 * HfgHoldAction type
 *      
 *     Hold actions 
 *-------------------------------------------------------------------------*/
typedef U8  HfgHoldAction;

/** Releases all held calls or sets User Determined User Busy
 * (UDUB) for a waiting call.
 */
#define HFG_HOLD_RELEASE_HELD_CALLS      0x00

/** Releases all active calls (if any exist) and accepts the other
 * (held or waiting) call.
 */
#define HFG_HOLD_RELEASE_ACTIVE_CALLS    0x01

/** Places all active calls (if any exist) on hold and accepts the
 * other (held or waiting) call.
 */
#define HFG_HOLD_HOLD_ACTIVE_CALLS       0x02

/** Adds a held call to the conversation.
 */
#define HFG_HOLD_ADD_HELD_CALL           0x03

/** Connects the two calls and disconnects the AG from
 * both calls (Explicit Call Transfer).
 */
#define HFG_HOLD_EXPLICIT_TRANSFER       0x04
/*--------------------------------------------------------------------------
 * End of HfgHoldAction
 *-------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------
 * HfgResponseHold type
 *      
 * Defines the action or state of Response and Hold.
 *-------------------------------------------------------------------------*/
typedef U8 HfgResponseHold;

/* Incoming call is put on hold */
#define HFG_RESP_HOLD_STATE_HOLD      0

/* Held Incoming call is accepted */
#define HFG_RESP_HOLD_STATE_ACCEPT    1

/* Held Incoming call is rejected */
#define HFG_RESP_HOLD_STATE_REJECT    2
/*--------------------------------------------------------------------------
 * End of HfgResponseHoldAction
 *-------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------
 * HfgNumberFormat type
 *
 *     This type is used to convey the format of a phone number. The format
 *     is created by or'ing together an ATNUM_TYPE value with an ATNUM_PLAN.
 *     For more information refer to GSM 04.08 section 10.5.4.7.
 *--------------------------------------------------------------------------*/
typedef U8 HfgNumberFormat;

#define HFG_NUM_TYPE_UNKNOWN          0x80
#define HFG_NUM_TYPE_INTERNATIONAL    0x90
#define HFG_NUM_TYPE_NATIONAL         0xA0
#define HFG_NUM_TYPE_NETWORK_SPEC     0xB0
#define HFG_NUM_TYPE_DEDICATED_ACC    0xC0

#define HFG_NUM_PLAN_UNKNOWN          0x00
#define HFG_NUM_PLAN_ISDN_TELEPHONY   0x01
#define HFG_NUM_PLAN_DATA_NUMBERING   0x03
#define HFG_NUM_PLAN_TELEX_NUMBERING  0x04
#define HFG_NUM_PLAN_NATIONAL         0x08
#define HFG_NUM_PLAN_PRIVATE          0x09

/* Commonly used formats */
#define HFG_NUM_INTERNATIONAL_ISDN    (HFG_NUM_TYPE_INTERNATIONAL|HFG_NUM_PLAN_ISDN_TELEPHONY)
#define HFG_NUM_ISDN_TELEPHONY        (HFG_NUM_TYPE_UNKNOWN|HFG_NUM_PLAN_ISDN_TELEPHONY)
#define HFG_NUM_NATIONAL_ISDN         (HFG_NUM_TYPE_NATIONAL|HFG_NUM_PLAN_ISDN_TELEPHONY)
/*--------------------------------------------------------------------------
 * End of AtNumberFormat
 *-------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------
 * HfgOperFormat
 *--------------------------------------------------------------------------*/
typedef U8 HfgOperFormat;

/** */
#define HFG_OF_LONG_ALPHANUM     0

/** */
#define HFG_OF_SHORT_ALPHANUM    1

/** */
#define HFG_OF_NUMERIC           2
/*--------------------------------------------------------------------------
 * End of HfgOperFormat
 *-------------------------------------------------------------------------*/

/***************************************************************************** 
* Structure
*****************************************************************************/
/*---------------------------------------------------------------------------
 * HfgCallWaitParms structure
 *
 *     Used to identify the waiting call.
 *     AT CMD : +CCWA:<number>,<type>,<class>
 *     Covered : <number>,<type>
 *--------------------------------------------------------------------------*/
typedef struct _HfgCallWaitParms 
{
    /* Phone number of the waiting call */
    const char     *number;
    /* Voice parameters */
    U8              classmap;
    /* Type of address */
    U8              type;
} HfgCallWaitParms;

/* HfgReadIndicatorParms structure
 *
 *     Used to send current indicators values.
 *     AT CMD : +CIND=[<ind>[,<ind>[,...]]]
 */
typedef struct _HfgReadIndicatorParms 
{
        /* Number of indicators reported. */
        U8              num;
        struct 
        {
            /* Value of indicator. */
            U8          value;
        } ind[NUM_OF_HFG_IND];
} HfgReadIndicatorParms;

/*---------------------------------------------------------------------------
 * HfgCallerIdParms structure
 *
 *     Used to identify the calling number.
 *     AT CMD : +CLIP: <number>,<type>
 */
typedef struct _HfgCallerIdParms 
{
    /* Phone number of the caller */
    const char     *number;

    /* Type of address */
    U8              type;
} HfgCallerIdParms;


/*---------------------------------------------------------------------------
 * HfgCallListParms structure
 *
 *     Used to identify the listed calls on the audio gateway.
 *     AT CMD : +CLCC: <id1>,<dir>,<stat>,<mode>,<mpty>[,<number>,<type>
 *
 */
typedef struct _HfgCallListParms
{
    /* Index of the call on the audio gateway (1 based) */
    U8              index;

    /* 0 - Mobile Originated, 1 = Mobile Terminated */
    U8              dir;

    /* Call state (see HfgCallStatus) */
    HfgCallStatus   state;

    /* Call mode (see HfgCallMode) */
    HfgCallMode     mode;

    /* 0 - Not Multiparty, 1 - Multiparty */
    U8              multiParty;
    
    /* Phone number of the call */
    const char     *number;

    /* Type of address */
    U8              type;
} HfgCallListParms;

/*---------------------------------------------------------------------------
 * HfgSubscriberNum structure
 *
 *     Used to identify the subscriber number.
 *     AT CMD : +CNUM: [<alpha>],<number>, <type>,[<speed>] ,<service>
 */
typedef struct _HfgSubscriberNum 
{
    /* String phone number of format specified by "type". */
    const char     *number;

    /* Phone number format */
    //AtNumberFormat  type;
	HfgNumberFormat		type;

    /* Service related to the phone number. */
    U8              service;
} HfgSubscriberNum;

/*---------------------------------------------------------------------------
 * HfgNetworkOperator structure
 *
 *     Used to identify the network operator.
 */
typedef struct _HfgNetworkOperator 
{
    /* 0 = automatic, 1 = manual, 2 = deregister, 3 = set format only, 
     * 4 = manual/automatic.
     */
    U8              mode;

    /* Format of "oper" parameter (should be set to 0) */
    //AtOperFormat    format;
	HfgOperFormat	format;

    /* Numeric or long or short alphanumeric name of operator */
    const char     *oper;
} HfgNetworkOperator;

/*---------------------------------------------------------------------------
 * HfgHold structure
 *
 *     Used to describe the hold action and possibly the line for that action.
 */
typedef struct _HfgHold 
{
    /* Hold action to execute */
    HfgHoldAction action;

    /* Index of the call on the audio gateway (1 based).  If 0, the action
     * does not apply to any particular call.
     */
    U8            index;
} HfgHold;

/*---------------------------------------------------------------------------
 * HfgAtData structure
 *
 *     Defines the structure containing raw AT data.
 */
typedef struct _HfgAtData 
{
    U8         *rawData;
    U16          dataLen;
} HfgAtData;


typedef U8 HfgCHarsetType;

#define  HFG_CHARSET_GSM		0x01	/* "GSM" */
//#define  HFG_CHARSET_HEX		0x02		/* "HEX" */
//#define  HFG_CHARSET_IRA		0x04		/* "IRA" */
//#define  HFG_CHARSET_PCCP		0x08		/* "PCCPxxx" */
//#define  HFG_CHARSET_PCDN		0x10		/* "PCDN" */
#define  HFG_CHARSET_UCS2		0x20	/* "UCS2" */
#define  HFG_CHARSET_UTF8		0x40	/* "UTF8" */
//#define  HFG_CHARSET_8859n		0x80		/* "8859n" n=1~6, C, A, G, H */

/****************************************************************************
*   Phonebook related structure
****************************************************************************/
/*---------------------------------------------------------------------------
 * HfgPbStorageType type
 *
 * Types of phonebook memory storage used by the Gateway.
 */
typedef U16 HfgPbStorageType;

/** Gateway dialed calls list. "DC" */
#define HFG_PBS_DIALED_CALLS         0x0001

/** SIM fixed-dialing-phonebook list. "FD" */
#define HFG_PBS_FIXED_DIAL           0x0002

/** SIM last-dialing-phonebook list. "LD" */
#define HFG_PBS_LAST_DIAL            0x0004

/** Gateway missed calls list. "MC" */
#define HFG_PBS_MISSED_CALLS         0x0008

/** Gateway phonebook list. "ME" */
#define HFG_PBS_ME_PHONEBOOK         0x0010

/** Combined Gateway and SIM phonebook list. "MT" */
#define HFG_PBS_ME_SIM_COMBINED      0x0020

/** Gateway received calls list. "RC" */
#define HFG_PBS_RECEIVED_CALLS       0x0040

/** SIM phonebook list. "SM" */
#define HFG_PBS_SIM_PHONEBOOK        0x0080

/* End of AtPbStorageType */



/* TE request */
/* AT_SELECT_PHONEBOOK_STORAGE */
typedef struct
{
    /* Phonebook storage type to select. */
    U16     select;
} HfgPbStorage;

/* AT_READ_PHONEBOOK_ENTRY */
typedef struct
{
    /* First entry to return. */
    U16                 first;

    /* Last entry to return. To return only one entry,
     * set last = first.
     */
    U16                 last;
} HfgPbRead;

/* AT_FIND_PHONEBOOK_ENTRY */
typedef struct
{
    /* Start text to search for. */
    const char         *text;
} HfgPbFind;

/* AT_WRITE_PHONEBOOK_ENTRY */
typedef struct
{
    /* Index of this entry. */
    U16                 index;

    /* Phone number format. */
    U8                  type;

    /* Phone number. */
    const char        *number;

    /* Text associated with phone number. The character set used with
     * this parameter is specified by AT_SELECT_CHARACTER_SET command.
     */
    const char        *text;
} HfgPbWrite;

/* ME response */
/* AT_SELECT_PHONEBOOK_STORAGE|AT_READ */
typedef struct
{
    /* Phonebook storage currently selected. */
    U16     		selected;

    /* Number of used locations. */
    U16                 used;

    /* Total number of locations in the memory. */
    U16                 total;

} HfgPbStorageSelected;

/* AT_SELECT_PHONEBOOK_STORAGE|AT_TEST */
typedef struct 
{
    /* Bitmap of supported storage types. */
    U16     supported;
} HfgPbStorageSupported;

/* AT_READ_PHONEBOOK_ENTRY */
/* AT_FIND_PHONEBOOK_ENTRY */
typedef struct 
{
    /* Index of this entry. */
    U16                 index;

    /* Phone number format. */
    U8					type;

    /* Phone number. */
    const char         *number;

    /* Text associated with phone number. The character set used with
     * this parameter is specified by AT_SELECT_CHARACTER_SET command.
     */
    const char         *text;
} HfgPbEntry;

/* AT_READ_PHONEBOOK_ENTRY|AT_TEST */
typedef struct 
{
    /* Location range supported by the current storage (e.g. "0-99"). 
     * Quotes must not be included in this string.
     */
    const char         *range;
    
    /* The maximum length of the phonebook "number" field. */
    U16                 numLength;

    /* The maximum length of the phonebook "text" field. */
    U16                 textLength;
} HfgPbReadInfo;

/* AT_FIND_PHONEBOOK_ENTRY|AT_TEST */
typedef struct 
{
    /* The maximum length of the phonebook "number" field. */
    U16                 numLength;

    /* The maximum length of the phonebook "text" field. */
    U16                 textLength;
} HfgPbFindInfo;

/* AT_WRITE_PHONEBOOK_ENTRY|AT_TEST */
typedef struct 
{
    /* Location range supported by the current storage (e.g. "0-99"). 
     * Quotes must not be included in this string.
     */
    const char         *range;

    /* The maximum length of the phonebook "number" field. */
    U16                 numLength;

    /* List of supported type by the current storage (e.g. "145,129")
     * Quotes must not be included in this string.
     */	 
    const char         *types;

    /* The maximum length of the phonebook "text" field. */
    U16                 textLength;
} HfgPbWriteInfo;

/****************************************************************************
*   SMS related constant
****************************************************************************/
// Optional Parameter for SMS transmission
typedef U16 HfgSMSMask;
#define HFG_SMS_MASK_ALPHA	0x0001	// string type alphanumeric representation of address (related +CSCS)
#define HFG_SMS_MASK_SCTS		0x0002	// Service Centre Time Stamp in time-string format
#define HFG_SMS_MASK_ADDR	0x0004	// address (destination address in +CGMR SMS-COMMAND
#define HFG_SMS_MASK_TOADDR	0x0008	// type of address (originating address and destination address, 129, 145)
#define HFG_SMS_MASK_LENGTH	0x0010	// length of the message body <data> in integer or actual TP data unit in octets
#define HFG_SMS_MASK_RA		0x0020	// recipient address in string format
#define HFG_SMS_MASK_TORA		0x0040	// type of ra
#define HFG_SMS_MASK_FO		0x0080	// first octect of SMS-DELIVER, SMS-SUBMIT, SMS-STATUS-REPORT, SMS-COMMAND
#define HFG_SMS_MASK_PID		0x0100	// Protocol Identifier in integer format
#define HFG_SMS_MASK_DCS		0x0200	// SMS Data Coding Scheme
#define HFG_SMS_MASK_SCA		0x0400	// RP SC address in string format
#define HFG_SMS_MASK_TOSCA	0x0800	// type of sca in integer format
#define HFG_SMS_MASK_VP		0x1000	// Validity Period (integer or time-string format)
#define HFG_SMS_MASK_MN		0x2000	// Message Number in integer format
#define HFG_SMS_MASK_CDATA	0x4000	// Command data in text mode response
#define HFG_SMS_MASK_ACKPDU	0x8000	// User Data element of RP-ACK PDU (+CMGS in PDU mode)

typedef U8 HfgSMSStat;
#define HFG_SMS_STAT_REC_UNREAD	0
#define HFG_SMS_STAT_REC_READ		1
#define HFG_SMS_STAT_STO_UNSENT	2
#define HFG_SMS_STAT_STO_SENT		3
#define HFG_SMS_STAT_ALL			4
#define NUM_OF_HFG_SMS_STAT		5
#define HFG_SMS_STAT_UNKNOWN		0xFF

typedef U8 HfgSMSStorage;
#define HFG_SMS_STORAGE_BM			(1<<0)
#define HFG_SMS_STORAGE_ME			(1<<1)
#define HFG_SMS_STORAGE_MT			(1<<2)
#define HFG_SMS_STORAGE_SM			(1<<3)
#define HFG_SMS_STORAGE_TA			(1<<4)
#define HFG_SMS_STORAGE_SR			(1<<5)
#define NUM_OF_HFG_SMS_STORAGE		6
#define HFG_SMS_STORAGE_UNKNOWN		0xFF

typedef U8 HfgSMSMsgFormat;
#define HFG_SMS_MSG_FORMAT_PDU		0
#define HFG_SMS_MSG_FORMAT_TEXT		1
#define NUM_OF_HFG_SMS_MSG_FORMAT	2


typedef U8 HfgSMSType;
#define HFG_SMS_TYPE_DELIVER		0
#define HFG_SMS_TYPE_SUBMIT		1
#define HFG_SMS_TYPE_STATUS		2
#define HFG_SMS_TYPE_COMMAND		3
#define HFG_SMS_TYPE_CBM			4
#define HFG_SMS_TYPE_PDU			5
#define NUM_OF_HFG_SMS_TYPE		6

/* ****************************************
*	+CNMI 
******************************************/
/* Parameter <mode> : range (0~3), default=0 */
typedef U8 HfgSMSCNMIMode;
#define HFG_SMS_CNMI_MODE_UNKNOWN	0xFF
/* Parameter <mt> : range (0~3), defaut=0 */
typedef U8 HfgSMSCNMIMt;
#define HFG_SMS_CNMI_MT_UNKNOWN		0xFF
/* Parameter <bm> : range (0~3), defaut=0 */
typedef U8 HfgSMSCNMIBm;
#define HFG_SMS_CNMI_BM_UNKNOWN	0xFF
/* Parameter <ds> : range (0~1), defaut=0 */
typedef U8 HfgSMSCNMIDs;
#define HFG_SMS_CNMI_DS_UNKNOWN		0xFF
/* Parameter <bfr> : range (0~1), defaut=0 */
typedef U8 HfgSMSCNMIBfr;
#define HFG_SMS_CNMI_BFR_UNKNOWN	0xFF

typedef U8 HfgSMSCmdType;


/********************************************************************/
/*   SMS related commandt structure										*/
/********************************************************************/
/********************************************************************/
/* MSG_ID_BT_HFG_SELECT_SMS_SERVICE_IND 							*/
/* AT_SELECT_SMS_SERVICE : AT+CSMS=<service>							*/
/********************************************************************/
typedef U8 HfgSMSService_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SELECT_PREF_MSG_STORAGE_IND 						*/
/* AT_PREFERRED_SMS_STORAGE: AT+CPMS=<mem1>[,<mem2>[,<mem3>]] 	*/
/********************************************************************/
typedef struct _HfgSMSPrefStorage_cmd
{
	HfgSMSStorage	read;
	HfgSMSStorage	write;
	HfgSMSStorage	recv;
}HfgSMSPrefStorage_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SELECT_MSG_FORMAT_IND							*/
/* AT_SMS_MESSAGE_FORMAT: AT+CMGF=[<mode>]						*/
/********************************************************************/
typedef U8 HfgSMSFormat_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SET_SERVICE_CENTRE_IND	 						*/
/* AT_SMS_SERVICE_CENTER: AT+CSCA=<sca>[,<tosca>]					*/
/********************************************************************/
typedef struct _HfgSMSSrviceCentre_cmd
{
	HfgSMSMask		mask;
	const char		*sca;
	U8				tosca;	/* Optional: default 1.First octect of sca is '+' : 145
											  2. Otherwise : 129 */
}HfgSMSSrviceCentre_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SET_TEXT_MODE_PARAMS_IND	 						*/
/* AT_SET_TEXT_MODE_PARMS: AT+CSMP=[<fo>[,<vp>[,<pid>[,<dcs>]]]]		*/
/********************************************************************/
typedef struct _HfgSMSTextModeParam_cmd
{
	HfgSMSMask	mask;
	U8			fo;
	U8			intVp;
	const char*	strVp;
	U8			pid;
	U8			dcs;
}HfgSMSTextModeParam_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SET_SHOW_PARAMS_IND		 						*/
/* AT_SMS_SHOW_TEXT_MODE: AT+CSDH=[<show>]						*/
/********************************************************************/
typedef U8 HfgSMSShowParams_cmd;

/****************************************************************************/
/* MSG_ID_BT_HFG_SET_NEW_MSG_INDICATION_IND		 						*/
/* AT_NEW_MESSAGE_INDICATION: AT+CNMI=[<mode>[,<mt>[,<bm>[,<ds>[,<bfr>]]]]]	*/
/****************************************************************************/
typedef struct _HfgSMSIndSetting_cmd
{
	HfgSMSCNMIMode	mode;	/* Default: 0 */
	HfgSMSCNMIMt	mt;		/* Default: 0 */
	HfgSMSCNMIBm	bm;		/* Default: 0 */
	HfgSMSCNMIDs	ds;		/* Default: 0 */
	HfgSMSCNMIBfr	bfr;		/* Default: 0 */
}HfgSMSIndSetting_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_LIST_MSG_IND				 						*/
/* AT_LIST_MESSAGES: AT+CMGL[=<stat>]								*/
/********************************************************************/
typedef HfgSMSStat HfgSMSList_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_READ_MSG_IND				 						*/
/* AT_READ_MESSAGE: AT+CMGR=<index>								*/
/********************************************************************/
typedef U16 HfgSMSRead_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SEND_MSG_IND				 						*/
/* AT_SEND_MESSAGE: 												*/
/* TEXT MODE : AT+CMGS=<da>[,<toda>]<CR>text is entered<ctrl-Z/ESC> 	*/
/* PDU MODE : AT+CMGS=<length><CR>PDU is given<ctrl-Z/ESC> 			*/
/********************************************************************/
typedef struct _HfgSMSSend_cmd
{
	HfgSMSMask	mask;			// PDU mode if HFG_SMS_MASK_LENGTH is set
	const char	*destAddr;		// Destination address
	U8			addrType;		// Optional. type of  address (toda, ex. 129)	
	U16			pduLen;			// length field of PDU mode
	const char	*msg;			// message from HF
	//U16		msgLen;			// length of message
}HfgSMSSend_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_SEND_STORED_MSG_IND		 						*/
/* AT_SEND_STORED_MESSAGE:	AT+CMSS=<index>[,<da>[,<toda>]]			*/
/********************************************************************/
typedef struct _HfgSMSSendStored_cmd
{
	HfgSMSMask	mask;			// bitwise value for optional values (da, toda)
	U16			index;
	const char	*destAddr;		// Optional. Destination address
	U8			addrType;		// Optional. type of  address (toda, ex. 129)	
}HfgSMSSendStored_cmd;

/********************************************************************************/
/* MSG_ID_BT_HFG_WRITE_MSG_IND				 									*/
/* AT_STORE_MESSAGE:															*/
/* TEXT MODE : AT+CMGW=<oa/da>[,<toda/toda>[,<stat>]]<CR>text is entered<ctrl-Z/ESC>	*/
/* PDU MODE : AT+CMGW=<length>[,<stat>]<CR>PDU is given<ctrl-Z/ESC> 				*/
/********************************************************************************/
typedef struct _HfgSMSWrite_cmd
{
	HfgSMSMask	mask;			// PDU mode if HFG_SMS_MASK_LENGTH is set
	const char	*addr;			// Destination address
	U8			addrType;		// type of  address (toda, ex. 129)	
	U16			pduLen;			// length field of PDU mode
	HfgSMSStat	stat;			// Msg status
	const char	*msg;			// message from HF
}HfgSMSWrite_cmd;

/********************************************************************/
/* MSG_ID_BT_HFG_DELETE_MSG_IND			 						*/
/* AT_DELETE_MESSAGE: AT+CMGD=<index>								*/
/********************************************************************/
typedef U16 HfgSMSDelete_cmd;


/********************************************************************/
/*   SMS related result structure											*/
/********************************************************************/
/********************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_SMS_SERVICE_REQ 						*/
/* AT_SELECT_SMS_SERVICE|AT_TEST									*/
/* +CSMS:(list of supported <service>s)									*/
/********************************************************************/
/* const char* */

/********************************************************************/
/* MSG_ID_BT_HFG_SELECTED_SMS_SERVICE_REQ	 						*/
/* AT_SELECT_SMS_SERVICE|AT_READ									*/
/* +CSMS:<service>,<mt>,<mo>,<bm>									*/
/********************************************************************/
typedef struct _HfgSMSService_read
{
	U8	service;	/* 0 : GSM 03.40 and 03.41, 1...127: reserved, 128...:manufacture specific */
	U8	mt;		/* 0: type not supported, 1:type supported */
	U8	mo;		/* 0: type not supported, 1:type supported */
	U8	bm;		/* 0: type not supported, 1:type supported */
}HfgSMSService_read;

/********************************************************************/
/* MSG_ID_BT_HFG_SMS_SERVICE_REQ			 						*/
/* AT_SELECT_SMS_SERVICE											*/
/* +CSMS:<mt>,<mo>,<bm>											*/
/********************************************************************/
typedef struct _HfgSMSService_result
{
	U8	mt;		/* 0: type not supported, 1:type supported */
	U8	mo;		/* 0: type not supported, 1:type supported */
	U8	bm;		/* 0: type not supported, 1:type supported */
}HfgSMSService_result;

/********************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_PREF_MSG_STORAGE_REQ					*/
/* AT_PREFERRED_SMS_STORAGE|AT_TEST								*/
/* +CPMS:(list of supported <mem1>s),(..),(list of supported <mem3>s)		*/
/********************************************************************/
typedef struct _HfgSMSPrefStorage_test
{
	/* bitmask of supported storage */
	//HfgSMSStorage	mem[3];		/* mem1...mem3 */
	HfgSMSStorage	read;
	HfgSMSStorage	write;
	HfgSMSStorage	recv;
}HfgSMSPrefStorage_test;

/********************************************************************/
/* MSG_ID_BT_HFG_SELECTED_PREF_MSG_STORAGE_REQ					*/
/* AT_PREFERRED_SMS_STORAGE|AT_READ								*/
/* +CPMS:<mem1>,<used1>,<total1>...,<mem3>,<used3>,<total3>			*/
/********************************************************************/
typedef struct _HfgSMSPrefStorage_read
{
	HfgSMSStorage	read;
	U16				readUsed;
	U16				readTotal;
	HfgSMSStorage	write;
	U16				writeUsed;
	U16				writeTotal;
	HfgSMSStorage	recv;
	U16				recvUsed;
	U16				recvTotal;
}HfgSMSPrefStorage_read;

/********************************************************************/
/* MSG_ID_BT_HFG_PREF_MSG_STORAGE_REQ								*/
/* AT_PREFERRED_SMS_STORAGE										*/
/* +CPMS:<used1>,<total1>...,<used3>,<total3>							*/
/********************************************************************/
typedef struct _HfgSMSPrefStorage_result
{
	U8				count;
	U16				readUsed;
	U16				readTotal;
	U16				writeUsed;
	U16				writeTotal;
	U16				recvUsed;
	U16				recvTotal;
}HfgSMSPrefStorage_result;

/********************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_MSG_FORMAT_REQ							*/
/* AT_SMS_MESSAGE_FORMAT|AT_TEST									*/
/* +CMGF:(list of <mode>s)												*/
/********************************************************************/
/* const char* */

/********************************************************************/
/* MSG_ID_BT_HFG_SELECTED_MSG_FORMAT_REQ							*/
/* AT_SMS_MESSAGE_FORMAT|AT_READ									*/
/* +CMGF:<mode>													*/
/********************************************************************/
typedef U8 HfgSMSFormat_read;

/********************************************************************/
/* MSG_ID_BT_HFG_SERVICE_CENTRE_REQ								*/
/* AT_SMS_SERVICE_CENTER|AT_READ									*/
/* +CSCA:<sca>,<tosca>												*/
/********************************************************************/
typedef struct _HfgSMSSrviceCentre_read
{
	const char		*sca;
	U8				tosca;
}HfgSMSSrviceCentre_read;

/********************************************************************/
/* MSG_ID_BT_HFG_TEXT_MODE_PARAMS_REQ								*/
/* AT_SET_TEXT_MODE_PARMS|AT_READ									*/
/* +CSMP:<fo>,<vp>,<pid>,<dcs>										*/
/********************************************************************/
typedef struct _HfgSMSTextModeParam_read
{
	U8			fo;
	U8			intVp;	//valid only if strVp is NULL
	const char*	strVp;	
	U8			pid;
	U8			dcs;
}HfgSMSTextModeParam_read;

/********************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_SHOW_PARAMS_REQ						*/
/* AT_SMS_SHOW_TEXT_MODE|AT_TEST									*/
/* +CSDH:(list of supported <show>s)									*/
/********************************************************************/
/* const char* */

/********************************************************************/
/* MSG_ID_BT_HFG_SELECTED_SHOW_PARAMS_REQ							*/
/* AT_SMS_SHOW_TEXT_MODE|AT_READ									*/
/* +CSDH:<show>													*/
/********************************************************************/
typedef U8 HfgSMSShowParams_read;

/********************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_NEW_MSG_INDICATION_REQ				*/
/* AT_NEW_MESSAGE_INDICATION|AT_TEST								*/
/* +CNMI:(list of supported <mode>s)...(list of supported <bfr>s)				*/
/********************************************************************/
/* const char* */

/********************************************************************/
/* MSG_ID_BT_HFG_SELECTED_NEW_MSG_INDICATION_REQ					*/
/* AT_NEW_MESSAGE_INDICATION|AT_READ								*/
/* +CNMI:<mode>,<mt>,<bm>,<ds>,<bfr>								*/
/********************************************************************/
typedef struct _HfgSMSIndSetting_read
{
	HfgSMSCNMIMode	mode;
	HfgSMSCNMIMt	mt;
	HfgSMSCNMIBm	bm;
	HfgSMSCNMIDs	ds;
	HfgSMSCNMIBfr	bfr;
}HfgSMSIndSetting_read;

/********************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_LIST_STATUS_REQ							*/
/* AT_LIST_MESSAGES|AT_TEST											*/
/* +CNMI:<mode>,<mt>,<bm>,<ds>,<bfr>								*/
/********************************************************************/
/* const char* */

/********************************************************************/
/* MSG_ID_BT_HFG_LIST_MSG_REQ										*/
/* AT_LIST_MESSAGES													*/
/* TEXT MODE & SMS-SUBMITs and/or SMS-DELIVERs : 						*/
/* 	+CMGL:<index>,<stat>,<oa/da>,[<alpha>],[<scts>][,<tooa/toda>,		*/
/*               <length>]<CR><LF><data> 									*/
/* TEXT MODE & SMS-STATUS-REPORTs : 									*/
/*	+CMGL:<index>,<stat>,<sn>,<mid>,<page>,<pages><CR><LF><data> 	*/
/* TEXT MODE & SMS-COMMANDs : 										*/
/*	+CMGL:<index>,<stat>,<fo>,<ct><CR><LF><data> 					*/
/* TEXT MODE & CMB storage : 											*/
/*	+CMGL:<index>,<stat>,<sn>,<mid>,<page>,<pages><CR><LF><data> 	*/
/* PDU MODE : 														*/
/*	+CMGL:<index>,<stat>,[alpha],<length><CR><LF><pdu> 				*/
/********************************************************************/
typedef struct _HfgSMSCMGLDeliver
{
	HfgSMSMask		mask;			// bitwise value for optional values (alpha, scts, tooa/toda, length)
	const char		*addr; 			// originated address, destination address (oa/da)
	U8				addrType;		// type of address (tooa/toda)
	const char		*alpha; 			// string type of address (optional, alpha, ex.UTF-8)
	const char		*SCTimeStamp;	// time stamp "yy/MM/dd,hh:mm:ss+(-)zz" (optional, scts)
	const char		*data;			// data
	U16				length;			// length of data(optional)
}HfgSMSCMGLDeliver, HfgSMSCMGLSubmit;

typedef struct _HfgSMSCMGLStatus
{
	HfgSMSMask	mask;				// bitwise value for optional values (ra, tora)
	U8			fo;					// first octet (default 2)	
	U8			mr;					// Message Reference (mr)
	const char	*ra;					// Recipient  Address (ra)
	U8			addrType;			// type of address (optional, tora)	
	const char	*SCTimeStamp;		//"yy/MM/dd,hh:mm:ss+zz" (scts)	
	const char	*dt;					// Discahrge Time  "yy/MM/dd,hh:mm:ss+(-)zz"(dt)
	U8			st;					// Status of previously sent message	(st)
}HfgSMSCMGLStatus;

typedef struct _HfgSMSCMGLCommand
{
	U8				fo;				// first octet (default 2)
	HfgSMSCmdType	ct;				// Command type in integer format (default 0)
}HfgSMSCMGLCommand;

typedef struct _HfgSMSCbm
{
	U16			sn;					// serial number for particular message (sn)
	U16			mid;					// message identifier (mid)
	U8			page;				// page parameter. 0-3 bit:total number of pages, 4-7 bit: page number
	const char	*data;				// data
	U16			length;				// length of data (not transmitted)
	U8			dcs;					// data coding scheme (only for +CMGR)
}HfgSMSCMGLCbm;

typedef struct _HfgSMSCMGLPdu
{
	HfgSMSMask	mask;		// bitwise value for optional values (alpha)
	const char	*alpha;		// string type of address(optional, alpha, ex.UTF-8)
	const char	*PDU;		// PDU
	U16			length;		// data length of PDU
}HfgSMSCMGLPdu;

typedef struct _HfgSMSList_result
{
	HfgSMSType		type;
	U16				index;
	HfgSMSStat		stat;
	union
	{
		HfgSMSCMGLDeliver	deliver;
		HfgSMSCMGLSubmit 	submit;		
		HfgSMSCMGLStatus 	status;
		HfgSMSCMGLCommand	cmd;
		HfgSMSCMGLCbm		cbm;
		HfgSMSCMGLPdu		pdu;
	}msg;
}HfgSMSList_result;

/********************************************************************/
/* MSG_ID_BT_HFG_READ_MSG_REQ										*/
/* AT_READ_MESSAGE													*/
/* TEXT MODE & SMS-DELIVERs : 										*/
/*	+CMGR:<stat>,<oa>,[<alpha>],[<scts>][,<tooa>,<fo>,<pid>,<dcs>,		*/
/*	            <sca>,<tosca>,<length>]<CR><LF><data> 					*/
/* TEXT MODE & SMS-SUBMIT : 											*/
/*	+CMGR:<stat>,<da>,[<alpha>][,<toda>,<fo>,<pid>,<dcs>,[<vp>],		*/
/*		     <sca>,<tosca>,<length>]<CR><LF><data> 					*/
/* TEXT MODE & SMS-STATUS-REPORT : 									*/
/*	+CMGR:<stat>,<fo>,<mr>,[<ra>],[<tora>],<scts>,<dt>,<st>			*/
/*               <CR><LF><data> 											*/
/* TEXT MODE & SMS-COMMANDs : 										*/
/*	+CMGR:<stat>,<fo>,<ct>[,<pid>,[<mn>],[<da>],[<toda>],<length>		*/
/*		     <CR><LF><cdata>] 										*/
/* TEXT MODE & CMB storage : 											*/
/*	+CMGR:<stat>,<sn>,<mid>,<dcs>,<page>,<pages><CR><LF><data> 	*/
/* PDU MODE : 														*/
/*	+CMGR:<stat>,[<alpha>],<length><CR><LF><pdu> 					*/
/********************************************************************/
// SMS-DELIVER for +CMGR
typedef struct
{
	HfgSMSMask		mask;			// bitwise value for optional values (alpha, tooa, fo, pid, dcs, sca, tosca, length)
	const char		*addr; 			// originated address
	U8				addrType;		// type of address(optional, tooa)
	const char		*alpha; 			// string type of address(optional, alpha)
	const char		*SCTimeStamp;	// "yy/MM/dd,hh:mm:ss+zz" (scts)
	U8				fo;				// first octet (default 17, optional, fo)
	U8				pid;				// Protocol Identifer (default 0, optional)
	U8				dcs;				// data coding scheme (optional)
	const char		*SCAddr; 		// Service Centre address (optional, sca)
	U8				SCAddrType;		// type of Service Centre address(optional, tosca) 
	const char		*data;			// data
	U16				length;			// length of data (optional)	
}HfgSMSCMGRDeliver;

// SMS-SUBMIT  for +CMGR
typedef struct
{
	HfgSMSMask		mask;			// bitwise value for optional values (alpha, toda, fo, pid, dcs, vp, sca, tosca, length)
	const char		*addr; 			// destination address (da)
	U8				addrType;		// type of address(optional, toda)
	const char		*alpha;			// string type of address(optional, alpha)
	U8				fo;				// first octet (default 17, optional)	
	U8				pid;				// Protocol Identifer (default 0, optional)
	U8				dcs;				// data coding scheme (optional)
	U8				intVp;			// valid only if strVp is NULL. Validity period in integer format(default 167, optional)
	const char		*strVp;			// Validity period in time stamp format (yy/MM/dd,hh:mm:ss+-zz)
	const char		*SCAddr; 		// Service Centre address (optional, sca)
	U8				SCAddrType;		// type of Service Centre address(optional, tosca) 
	const char		*data;			// data
	U16				length;			// length of data (optional)	
}HfgSMSCMGRSubmit;

// SMS-STATUS-REPORT  for +CMGR
typedef HfgSMSCMGLStatus HfgSMSCMGRStatus;

// SMS-COMMAND  for +CMGR
typedef struct
{
	HfgSMSMask		mask;		// bitwise value for optional values (pid, mn, da, toda, length, cdata)
	U8				fo;			// first octet (default 2)		
	U8				ct;			// Command type
	U8				pid;			// Protocol Identifer (default 0, optional)
	U8				mn;			// Message Number previously MR value (optional, mn) 
	const char		*destAddr; 	// destnation address (otional, da)
	U8				addrType;	// type of address(optional, toda)	
	const char		*cmdData;	// command data(optional)
	U16				length;		// length of data (optional)		
}HfgSMSCMGRCommand;

// CBM  for +CMGR
typedef HfgSMSCMGLCbm HfgSMSCMGRCbm;

// PDU  for +CMGR
typedef HfgSMSCMGLPdu HfgSMSCMGRPdu;

// Read Message +CMGR
typedef struct
{
	HfgSMSType		type;		// type of SMs
	HfgSMSStat		stat;		// status
	union
	{
		HfgSMSCMGRDeliver	deliver;
		HfgSMSCMGRSubmit	submit;
		HfgSMSCMGRStatus	status;
		HfgSMSCMGRCommand	cmd;
		HfgSMSCMGRCbm		cbm;
		HfgSMSCMGRPdu		pdu;
	}msg;
}HfgSMSRead_result;

/********************************************************************/
/* MSG_ID_BT_HFG_SEND_MSG_REQ										*/
/* AT_SEND_MESSAGE													*/
/* TEXT MODE : +CMGS:<mr>[,<scts>] 									*/
/* PDU MODE : +CMGS:<mr>[,<ackpdu>] 									*/
/********************************************************************/
typedef struct _HfgSMSSend_result
{
	HfgSMSMask	mask;
	U8			mr;
	const char	*scts;		// optional
	const char	*ackpdu;
}HfgSMSSend_result;

/********************************************************************/
/* MSG_ID_BT_HFG_SEND_STORED_MSG_REQ								*/
/* AT_SEND_STORED_MESSAGE											*/
/* TEXT MODE : +CMSS:<mr>[,<scts>] 									*/
/* PDU MODE : +CMSS:<mr>[,<ackpdu>] 									*/
/********************************************************************/
typedef HfgSMSSend_result HfgSMSSendStored_result;

/********************************************************************/
/* MSG_ID_BT_HFG_WRITE_MSG_REQ										*/
/* AT_STORE_MESSAGE													*/
/* +CMGW:<index>				 									*/
/********************************************************************/
typedef U16 HfgSMSWrite_result;

/********************************************************************/
/* MSG_ID_BT_HFG_NEW_MSG_INDICATION_REQ							*/
/* AT_SMS_DELIVER_INDICATION										*/
/* +CMTI:<mem>,<index> 												*/
/********************************************************************/
typedef struct _HfgSMSNewMsgInd
{
	HfgSMSStorage	mem;
	U16				index;
}HfgSMSNewMsgInd;

#endif
