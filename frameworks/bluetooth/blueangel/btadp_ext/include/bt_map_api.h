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

#ifndef __BT_MAP_PORTING_H__
#define __BT_MAP_PORTING_H__

#ifdef BTMTK_ON_LINUX

#include "bt_mmi.h"

#define BT_MAP_FOLDER_LIST_FILE MAP_ADP_WORK_FOLDER"/maps_fl%d.xml"
#define BT_MAP_MESSAGE_LIST_FILE MAP_ADP_WORK_FOLDER"/maps_ml%d.xml"
#define BT_MAP_MESSAGE_GET_FILE MAP_ADP_WORK_FOLDER"/maps_gm%d.vcf"
#define BT_MAP_EVENT_REPORT_FILE MAP_ADP_WORK_FOLDER"/maps_er%d.xml"
#define BT_MAP_PUSH_MESSAGE_FILE MAP_ADP_WORK_FOLDER"/maps_pm%d.xml"


#define BT_MAP_TMP_FILE              MAP_ADP_WORK_FOLDER"/map%d.tmp"
#define BT_MAP_RCV_MESSAGE_PUSH_FILE MAP_ADP_WORK_FOLDER"/pmr%d.vcf"


#define MBT_MAP_MAX_MCE_NUM				2	// The number of MCE devices
#define MBT_MAP_MAX_MAS_INST_NUM		4	// [MAP_CHECK] The number of MAS Instances (using 4 in BRCM)
//#define MBT_MAP_MAX_CONN_NUM			8	// (MBT_MAP_MAX_MCE_NUM * MBT_MAP_MAX_MAS_INST_NUM)
// todo: temp set the MBT_MAP_MAX_CONN_NUM as 4, instead of 8
#define MBT_MAP_MAX_CONN_NUM			4	// (MBT_MAP_MAX_MCE_NUM * MBT_MAP_MAX_MAS_INST_NUM)
#define MBT_MAP_MAX_SVC_NAME_LEN		256	// [MAP_CHECK] (using 256 in btapp of BRCM)
#define MBT_MAP_MAX_FOLDER_PATH_LEN		512	// This shall be restricted to 512 Bytes in spec.
#define MBT_MAP_MAX_MSG_LIST_ATTR_LEN	256	// This shall not exceed 256 bytes in spec.
#define MBT_MAP_MAX_DATETIME_LEN		17	// The format shall be "YYYYMMDDTHHMMSS" in spec. (using 17 in BRCM)
#define MBT_MAP_MAX_TIME_LEN			21	// The format shall be "YYYYMMDDTHHMMSS+-hhmm" in spec.
#define MBT_MAP_MAX_FILTER_TEXT_LEN		256	// [MAP_CHECK] (using 256 in BRCM)
#define MBT_MAP_MAX_MSG_ENVELOPE_NUM	3	
#define MBT_MAX_FILE_NAME_LEN                   256
#define MBT_MAX_NAME_LEN                59
#define MAP_ADP_BUFFER_SIZE             2048


#define MAP_ADP_MAX_FRAGMENT_SIZE_NUM   255 // long sms message can be divided to 255 at most 


//type definition
#define MBT_VOID void
#define MBT_CHAR char
#define MBT_UINT64 U64
#define MBT_BOOL BT_BOOL
#define MBT_BYTE unsigned char
#define MBT_UINT U32
#define MBT_SHORT U16

#define MBT_BDADDR bt_addr_struct *

enum {
    BTMTK_MAP_STATE_NULL,
    BTMTK_MAP_STATE_INITIALIZING,
    BTMTK_MAP_STATE_INITIALIZED,
    BTMTK_MAP_STATE_DEINITIALIZING,
} ;
typedef MBT_BYTE btmtk_map_state;

enum {
    BTMTK_MASS_STATE_NULL,
    BTMTK_MASS_STATE_REGISTERING,
    BTMTK_MASS_STATE_REGISTERED,
    BTMTK_MASS_STATE_DEREGISTERING,
} ;
typedef MBT_BYTE btmtk_mass_state;


#define MBT_MAP_MSG_TYPE_SMS_GSM			0x01	// Emails on RFC2822 or MIME type basis
#define MBT_MAP_MSG_TYPE_SMS_CDMA			0x02	// GSM short messages
#define MBT_MAP_MSG_TYPE_EMAIL				0x04	// CDMA short messages
#define MBT_MAP_MSG_TYPE_MMS				0x08	// 3GPP MMS messages
typedef MBT_BYTE MBT_MAP_MSG_TYPE;

#define MBT_MAP_INVALID_INSTACE_ID       -1;


enum 
{
	MBT_MAP_NOTIF_STATUS_OFF = 0,
	MBT_MAP_NOTIF_STATUS_ON = 1
};
typedef MBT_BYTE MBT_MAP_NOTIF_STATUS;

enum
{
	MBT_MAP_STATE_NONE = 0,					// Not used
	MBT_MAP_STATE_CONNECTING,
	MBT_MAP_STATE_CONNECTED,
	MBT_MAP_STATE_DISCONNECTING,
	MBT_MAP_STATE_OPERATING
} ;
typedef MBT_BYTE MBT_MAP_STATE;

// MAP Read Status used for message list and bmessage-readstatus-property
enum
{
	MBT_MAP_MSG_STATUS_NO_FILTERING = 0,
	MBT_MAP_MSG_STATUS_UNREAD = 1,		// The message has not yet been read. (default)
	MBT_MAP_MSG_STATUS_READ = 2			// The message has been read.
};
typedef MBT_BYTE MBT_MAP_MSG_READ_STATUS;



enum
{	
	MBTEVT_MAP_SERVER_ENABLE_NONE,
	MBTEVT_MAP_SERVER_ENABLE_SUCCESS,
	MBTEVT_MAP_SERVER_ENABLE_FAIL,
	MBTEVT_MAP_SERVER_DISABLE_SUCCESS,
	MBTEVT_MAP_SERVER_DISABLE_FAIL,
	MBTEVT_MAP_SERVER_REGISTER_SUCCESS,
	MBTEVT_MAP_SERVER_REGISTER_FAIL,
	MBTEVT_MAP_SERVER_DEREGISTER_SUCCESS,
	MBTEVT_MAP_SERVER_DEREGISTER_FAIL,
	MBTEVT_MAP_SERVER_AUTHREQ,                   //
	MBTEVT_MAP_SERVER_CONNECT_IND,
	MBTEVT_MAP_SERVER_CONNECT_FAIL,
	MBTEVT_MAP_SERVER_DISCONNECT_SUCCESS,
//	MBTEVT_MAP_SERVER_DISCONNECT_IND,
	MBTEVT_MAP_SERVER_SET_FOLDER_IND,
	MBTEVT_MAP_SERVER_GET_FOLDER_LIST_IND,
	MBTEVT_MAP_SERVER_GET_MESSAGE_LIST_IND,
	MBTEVT_MAP_SERVER_GET_MESSAGE_IND,
	MBTEVT_MAP_SERVER_PUSH_MESSAGE_IND,
	MBTEVT_MAP_SERVER_SET_NOTIFICATION_IND,
	MBTEVT_MAP_SERVER_SET_MESSAGE_STATUS_IND,
	MBTEVT_MAP_SERVER_UPDATE_INBOX,
	MBTEVT_MAP_SERVER_CANCEL,
	MBTEVT_MAP_SERVER_MNS_CONNECT_SUCCESS,
	MBTEVT_MAP_SERVER_MNS_CONNECT_FAIL,
	MBTEVT_MAP_SERVER_SEND_EVENT_SUCCESS,
	MBTEVT_MAP_SERVER_SEND_EVENT_FAIL,
	MBTEVT_MAP_SERVER_MNS_DISCONNECT_SUCCESS
};
typedef MBT_BYTE MBT_MAP_SERVER_MMI_EVENT;

enum
{
	MBT_MAP_STORAGE_TYPE_NONE = 0,
	MBT_MAP_STORAGE_TYPE_BUFFER = 1,		// struct buffer
	MBT_MAP_STORAGE_TYPE_FILE = 2,			// struct file
	MBT_MAP_STORAGE_TYPE_RAW_BUFFER = 3,	// raw buffer
	MBT_MAP_STORAGE_TYPE_RAW_FILE = 4		// raw file
};
typedef MBT_BYTE MBT_MAP_STORAGE_TYPE;
//typedef MBT_BYTE MBT_MAP_STORAGE_TYPE;


// MAP Message Type Mask for FilterMessageType
#define MBT_MAP_MSG_TYPE_MASK_SMS_GSM	0x01
#define MBT_MAP_MSG_TYPE_MASK_SMS_CDMA	0x02
#define MBT_MAP_MSG_TYPE_MASK_EMAIL		0x04
#define MBT_MAP_MSG_TYPE_MASK_MMS		0x08
typedef MBT_BYTE MBT_MAP_MSG_TYPE_MASK;

// MAP Priority Status used for filtering message list
enum
{
	MBT_MAP_PRI_STATUS_NO_FILTERING = 0,
	MBT_MAP_PRI_STATUS_HIGH = 1,
	MBT_MAP_PRI_STATUS_NON_HIGH = 2
};
typedef MBT_BYTE MBT_MAP_PRI_STATUS;


// MAP Reception Status
enum
{
	MBT_MAP_RCV_STATUS_COMPLETE = 0,
	MBT_MAP_RCV_STATUS_FRACTIONED,
	MBT_MAP_RCV_STATUS_NOTIFICATION
};
typedef MBT_BYTE MBT_MAP_RCV_STATUS;
//typedef MBT_BYTE MBT_MAP_RCV_STATUS;

enum 
{
	MBT_MAP_NEW_MSG_STATUS_OFF = 0,
	MBT_MAP_NEW_MSG_STATUS_ON = 1
};
typedef MBT_BYTE MBT_MAP_NEW_MSG_STATUS;


enum
{
	MBT_FORBID_RES = 0,				// authorization reject
	MBT_ALLOW_RES,					// authorization allow
	MBT_ERROR_RES,					// error response
	MBT_UNAUTHORIZED_RES,			//read only
	MBT_NOT_FOUND_RES,				//file not found
	MBT_UNSUPPORTED_MEDIA_TYPE_RES, //unsupport type
	MBT_SERVICE_UNAVAILABLE_RES,	//unsupport function
	MBT_DATABASE_FULL_RES,			//no space
	MBT_INTERNAL_SERVER_ERROR_RES,	//etc
	MBT_UNSUPPORTED_DEVICE_TYPE_RES,
	MBT_BAD_REQUEST_RES,
	MBT_FORBIDDEN_RES,
	MBT_NOT_ACCEPTABLE_RES,
	MBT_PRECONDITION_FAILED_RES,
	MBT_NOT_IMPLEMENTED_RES,
};
typedef MBT_BYTE MBT_AUTHRES;






// MAP bmessage-version-property
enum
{
	MBT_MAP_MSG_VER_10 = 0,				// bMessage version 1.0
	MBT_MAP_MSG_VER_MAX
};
typedef MBT_BYTE MBT_MAP_MSG_VERSION;



// MAP bmessage-body-encoding-property
enum
{
	MBT_MAP_MSG_ENCOD_8BIT = 0,			// For Email/MMS : 8-Bit-Clean encoding
	MBT_MAP_MSG_ENCOD_G7BIT,			// For GSM-SMS : GSM 7 bit Default Alphabet
	MBT_MAP_MSG_ENCOD_G7BITEXT,			// For GSM-SMS : GSM 7 bit Alphabet with national language extension 
	MBT_MAP_MSG_ENCOD_GUCS2,			// For GSM-SMS
	MBT_MAP_MSG_ENCOD_G8BIT,			// For GSM-SMS 
	MBT_MAP_MSG_ENCOD_C8BIT,			// For CDMA-SMS : Octet, unspecified
	MBT_MAP_MSG_ENCOD_CEPM,				// For CDMA-SMS : Extended Protocol Message
	MBT_MAP_MSG_ENCOD_C7ASCII,			// For CDMA-SMS : 7-bit ASCII
	MBT_MAP_MSG_ENCOD_CIA5,				// For CDMA-SMS : IA5
	MBT_MAP_MSG_ENCOD_CUNICODE,		// For CDMA-SMS : UNICODE
	MBT_MAP_MSG_ENCOD_CSJIS,				// For CDMA-SMS : Shift-JIS
	MBT_MAP_MSG_ENCOD_CKOREAN,			// For CDMA-SMS : Korean
	MBT_MAP_MSG_ENCOD_CLATINHEB,		// For CDMA-SMS : Latin/Hebrew
	MBT_MAP_MSG_ENCOD_CLATIN,			// For CDMA-SMS : Latin
	MBT_MAP_MSG_ENCOD_MAX
};
typedef MBT_BYTE MBT_MAP_MSG_ENCODING;


// MAP bmessage-body-charset-property
enum
{
	MBT_MAP_MSG_CHARSET_NATIVE = 0,
	MBT_MAP_MSG_CHARSET_UTF8 = 1
};
typedef MBT_BYTE MBT_MAP_MSG_CHARSET;

// MAP bmessage-body-language-property
enum
{
	MBT_MAP_MSG_LANG_ENGLISH = 0,		// For CDMA-SMS
	MBT_MAP_MSG_LANG_FRENCH,			// For CDMA-SMS
	MBT_MAP_MSG_LANG_SPANISH,			// For CDMA-SMS and GSM-SMS
	MBT_MAP_MSG_LANG_JAPANESE,			// For CDMA-SMS
	MBT_MAP_MSG_LANG_KOREAN,			// For CDMA-SMS
	MBT_MAP_MSG_LANG_CHINESE,			// For CDMA-SMS
	MBT_MAP_MSG_LANG_HEBREW,			// For CDMA-SMS
	MBT_MAP_MSG_LANG_TURKISH,			// For GSM-SMS
	MBT_MAP_MSG_LANG_PORTUGUESE,		// For GSM-SMS
	MBT_MAP_MSG_LANG_UNKNOWN,			// For GSM-SMS and CDMA-SMS
	MBT_MAP_MSG_LANG_MAX
};
typedef MBT_BYTE MBT_MAP_MSG_LANG;

enum
{
	MBT_MAP_STATUS_IND_READ = 0,
	MBT_MAP_STATUS_IND_DELETE = 1
};
typedef MBT_BYTE MBT_MAP_STATUS_IND;

// MAP Status Value
enum
{
	MBT_MAP_STATUS_VAL_NO = 0,
	MBT_MAP_STATUS_VAL_YES = 1
};
typedef MBT_BYTE MBT_MAP_STATUS_VAL;

// MAP PushMessage Enum ----------------------------------
// MAP Transparent Type
enum
{
	MBT_MAP_TRANSP_TYPE_OFF = 0,			// keep messages in 'Sent' folder (default)
	MBT_MAP_TRANSP_TYPE_ON = 1			// don't keep messages in Sent' folder
};
typedef MBT_BYTE MBT_MAP_TRANSP_TYPE;

// MAP Retry Type
enum
{
	MBT_MAP_RETRY_TYPE_OFF = 0,			// don't retry the successive attempts at sending the message
	MBT_MAP_RETRY_TYPE_ON = 1			// retry (default) check!!
};
typedef MBT_BYTE MBT_MAP_RETRY_TYPE;


// MAP SendEvent Enum ------------------------------------
// MAP Notification Type
enum
{
	MBT_MAP_NOTIF_TYPE_NEW_MSG = 0,     
	MBT_MAP_NOTIF_TYPE_DELIVERY_SUCCESS,        
	MBT_MAP_NOTIF_TYPE_SENDING_SUCCESS,        
	MBT_MAP_NOTIF_TYPE_DELIVERY_FAILURE,     
	MBT_MAP_NOTIF_TYPE_SENDING_FAILURE,      
	MBT_MAP_NOTIF_TYPE_MEMORY_FULL,     
	MBT_MAP_NOTIF_TYPE_MEMORY_AVAILABLE,       
	MBT_MAP_NOTIF_TYPE_MSG_DELETED,     
	MBT_MAP_NOTIF_TYPE_MSG_SHIFT,            
	MBT_MAP_NOTIF_TYPE_MAX
};
typedef MBT_BYTE MBT_MAP_NOTIF_TYPE;
//typedef MBT_BYTE MBT_MAP_NOTIF_TYPE;

// MAP Fraction Request
enum
{
	MBT_MAP_FRAC_REQ_FIRST = 0,
	MBT_MAP_FRAC_REQ_NEXT = 1,	
	MBT_MAP_FRAC_REQ_NO					// This is not a fraction request
};
typedef MBT_BYTE MBT_MAP_FRAC_REQ;

enum
{
	MBT_MAP_ATTACH_TYPE_OFF = 0,			// no attachments
	MBT_MAP_ATTACH_TYPE_ON = 1			// attachments to be delivered
};
typedef MBT_BYTE MBT_MAP_ATTACH_TYPE;


// MAP Fraction Delivery
enum
{
	MBT_MAP_FRAC_DELIVER_MORE = 0,
	MBT_MAP_FRAC_DELIVER_LAST = 1,		// in case of the last email fraction of the message object
	MBT_MAP_FRAC_DELIVER_NO				// This is not a fraction request
};
typedef MBT_BYTE MBT_MAP_FRAC_DELIVER;

enum
{
	MBT_MAP_OPER_NONE = 0,
	MBT_MAP_OPER_SET_NOTIF_REG,
	MBT_MAP_OPER_SET_FOLDER,
	MBT_MAP_OPER_GET_FOLDER_LIST,
	MBT_MAP_OPER_GET_MSG_LIST,
	MBT_MAP_OPER_GET_MSG,
	MBT_MAP_OPER_SET_MSG_STATUS,
	MBT_MAP_OPER_PUSH_MSG,
	MBT_MAP_OPER_UPDATE_INBOX,
	MBT_MAP_OPER_MAX
} ;
typedef MBT_BYTE MBT_MAP_OPERATION;


typedef struct
{
	MBT_MAP_NOTIF_STATUS		State;
	U8							MasInstId;		// MAS Instance ID
} MBT_MAP_MNS_NOTIF_REG;



// [MAP] Structure for GetMessagesListing
typedef struct
{
	MBT_UINT					ParamMask;		// [O] [MAP_CHECK] bit 0..bit 31 in spec. (T_MBT_MAP_ML_MASK)
	MBT_UINT64					MsgHandle;		// [M] The handle shall be a 64 bit unsigned integer in spec.
	MBT_CHAR					Subject[MBT_MAP_MAX_MSG_LIST_ATTR_LEN];		// [M] Title, the first words of the message, or "" (This length shall be used according to the requested value in GetMessagesListing)
	MBT_CHAR					DateTime[MBT_MAP_MAX_DATETIME_LEN];			// [M] The sending time or the reception time in format "YYYYMMDDTHHMMSS"
	MBT_CHAR					SenderName[MBT_MAP_MAX_MSG_LIST_ATTR_LEN];	// [C]
	MBT_CHAR					SenderAddr[MBT_MAP_MAX_MSG_LIST_ATTR_LEN];	// [C] The senders email address or phone number
	MBT_CHAR					ReplyToAddr[MBT_MAP_MAX_MSG_LIST_ATTR_LEN];	// [C] This shall be used only for emails to deliver the sender's reply-to email address.
	MBT_CHAR					RecipientName[MBT_MAP_MAX_MSG_LIST_ATTR_LEN];	// [C] The recipient's email address, a list of email addresses, or phone number
	MBT_CHAR					RecipientAddr[MBT_MAP_MAX_MSG_LIST_ATTR_LEN];	// [M] If the recipient is not known this may be left empty.
	MBT_MAP_MSG_TYPE			MsgType;		// [M]
	MBT_UINT					OriginMsgSize;	// [M] [MAP_CHECK] The overall size in bytes of the original message as received from network (using UINT16 in BRCM)
	MBT_BOOL					bText;			// (default 'no') (The message includes textual content or not)
	MBT_MAP_RCV_STATUS			ReceptionStatus;	// [M]
	MBT_UINT					AttachSize;		// [M] [MAP_CHECK] (using UINT16 in BRCM)
	MBT_BOOL					bPriority;		// (default 'no') The message is of high priority or not.
	MBT_BOOL					bRead;			// (default 'no') The message has already been read on the MSE or not.
	MBT_BOOL					bSent;			// (default 'no') The message has already been sent to the recipient or not.
	MBT_BOOL					bProtected;		// (default 'no') The message is protected by a DRM schem or not.
} MBT_MAP_MSG_LIST_ENTRY;

typedef struct
{
	MBT_BYTE					Mode;
	MBT_CHAR					Time[MBT_MAP_MAX_DATETIME_LEN]; // "yyyymmddTHHMMSSZ", or "" if none
	MBT_UINT					Size;
	MBT_CHAR					Name[MBT_MAX_FILE_NAME_LEN];	
} MBT_MAP_FOLDER_LIST_ENTRY;


//the structure just indicate a mas infomation
typedef struct
{
	U8					MasInstId;		// MAS Instance ID (The value range shall be 0..255 in spec. But zero(0) is not used.)
	U8					SvcName[MBT_MAP_MAX_SVC_NAME_LEN]; // The service name of MAS Instance
	U8					SupMsgType;	// The message type(s) to be supported (SMS, MMS, Email)
	U8					RootPath[MBT_MAX_FILE_NAME_LEN]; // The root path of MAS Server
	
//	U8					curFolderPath[MBT_MAX_FILE_NAME_LEN];	
//	btmtk_map_op_struct ops;                      //opration in MAP server


} MBT_MAP_MAS_INFO;


// [MAP] Structure for GetMessage
// MSE shall deliver only a bBody object if the value of FracReq is 'next'.
// vCard Object (Originator and Recipient)
// 1) vCard 2.1 : VERSION/N(Mandatory), TEL/EMAIL(Optional)
// 2) vCard 3.0 : VERSION/N/FN(Mandatory), TEL/EMAIL(Optional)
// All the other vCard properties shall not be used.
// The properties may be empty if not known by the MSE, e.g. N/FN in case of a SMS.
typedef struct
{	
	// <bmessage-property>
	MBT_MAP_MSG_VERSION			MsgVer;
	MBT_MAP_MSG_READ_STATUS		ReadStatus;
	MBT_MAP_MSG_TYPE			MsgType;
	MBT_CHAR					FolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN]; // The folder name including the path
	// [<bmessage-originator>]*
	MBT_UINT					OriginatorSize;
	MBT_CHAR*					Originator;		// Nested vCard Object
	// <bmessage-envelope>
	struct
	{	// [<bmessage-recipient>]*
		MBT_UINT				RecipientSize;
		MBT_CHAR*				Recipient;		// Nested vCard Object
	} Envelope[MBT_MAP_MAX_MSG_ENVELOPE_NUM];	// bmessage-envelope
	
	// <bmessage-content> (bBody Object)
	MBT_SHORT					PartId;			// This value shall have a part-ID incremented by 1 each in spec. (0 ~ 65535)
	// <bmessage-body-property>
	MBT_MAP_MSG_ENCODING	Encoding;
	MBT_MAP_MSG_CHARSET		Charset;			// This shall be used only if the message contains textual content in spec.
	MBT_MAP_MSG_LANG		Lang;			// This may be used if the message includes textual content in spec.
	// <bmessage-body-content>*
	MBT_UINT					FragmentNum;
	MBT_UINT					FragmentSize[MAP_ADP_MAX_FRAGMENT_SIZE_NUM];
	MBT_UINT					ContentSize;
	MBT_CHAR*					Content;
} MBT_MAP_MSG_OBJECT;



typedef struct
{	// Response Out
	MBT_MAP_STORAGE_TYPE		StorageType;	// The storage type
	MBT_SHORT					TotalFolderNum;	// [C] 2bytes in spec (The actual number of accessible folders)
	MBT_SHORT					TodoFolderNum;	// PI에 전달해야 할 남은 폴더의 수
	union
	{
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_BUFFER
		{
			MBT_UINT			FolderNum;
			MBT_MAP_FOLDER_LIST_ENTRY* FolderList;
		} Buffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_RAW_BUFFER
		{		
			MBT_UINT			Size;
			MBT_CHAR*			RawData;
		} RawBuffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_FILE or MBT_MAP_STORAGE_TYPE_RAW_FILE
		{			
			MBT_UINT			Size;
			MBT_CHAR			FileName[MBT_MAX_FILE_NAME_LEN]; // The file name including the full path
		} File;
	} Data;
}MBT_MAP_GET_FOLDER_LIST_RSP;

typedef struct
{	// Response Out
	// If 'MaxListCount=0' in the request shall response with the headers 'NewMsg' and 'MsgListSize' only in spec.
	MBT_MAP_NEW_MSG_STATUS	NewMsg;		// [C]
	MBT_CHAR					MSETime[MBT_MAP_MAX_TIME_LEN]; // [C] The format shall be "YYYYMMDDTHHMMSS+-hhmm" in spec.

	MBT_MAP_STORAGE_TYPE		StorageType;	// The storage type
	MBT_SHORT					TotalMsgNum;	// [C] 2bytes in spec (The number of accessible messages)
	MBT_SHORT					TodoMsgNum;	// PI에 전달해야 할 남은 메시지의 수
	union
	{
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_BUFFER
		{
			MBT_UINT			MsgNum;
			MBT_MAP_MSG_LIST_ENTRY* MsgList;
		} Buffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_RAW_BUFFER
		{		
			MBT_UINT			Size;
			MBT_CHAR*			RawData;
		} RawBuffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_FILE or MBT_MAP_STORAGE_TYPE_RAW_FILE
		{			
			MBT_UINT			Size;
			MBT_CHAR			FileName[MBT_MAX_FILE_NAME_LEN]; // The file name including the full path
		} File;
	} Data;
} MBT_MAP_GET_MSG_LIST_RSP;

typedef struct
{	// Response Out
	MBT_MAP_FRAC_DELIVER		FracDeliver;		// [C1]

	MBT_MAP_STORAGE_TYPE		StorageType;	// The storage type
	union
	{
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_BUFFER
		{
			MBT_MAP_MSG_OBJECT Msg;
		} Buffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_RAW_BUFFER
		{		
			MBT_UINT			Size;
			MBT_CHAR*			RawData;
		} RawBuffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_FILE or MBT_MAP_STORAGE_TYPE_RAW_FILE
		{
			MBT_MAP_MSG_OBJECT Msg;			// This is used for only MBT_MAP_STORAGE_TYPE_FILE. (Except the message contents)
			MBT_UINT			Size;
			MBT_CHAR			FileName[MBT_MAX_FILE_NAME_LEN]; // The file name including the full path
		} File;
	} Data;
} MBT_MAP_GET_MSG_RSP;



// [MAP] Structure for SendEvent
// MAP Event-Report Object
typedef struct
{
	MBT_MAP_NOTIF_TYPE			NotifType;
	MBT_UINT64					MsgHandle;		// The handle shall be a 64 bit unsigned integer in spec.
	MBT_CHAR					FolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN];
	MBT_CHAR					OldFolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN];
	MBT_MAP_MSG_TYPE			MsgType;
} MBT_MAP_EVT_REPORT_OBJECT;

typedef struct
{	// Request Out
	MBT_BYTE					MasInstId;		// [M] The value range shall be 0..255 in spec.
	MBT_MAP_EVT_REPORT_OBJECT EvtRptObj;		// [M]
} MBT_MAP_SEND_EVENT_REQ;

typedef struct
{	// Request In
	MBT_UINT64					MsgHandle;		// [M] The handle shall be a 64 bit unsigned integer in spec.
	MBT_MAP_ATTACH_TYPE		Attach;			// [M]
	MBT_MAP_MSG_CHARSET		Charset;			// [M]
	MBT_MAP_FRAC_REQ			FracReq;		// [O]
} MBT_MAP_GET_MSG_REQ;

// If 'MaxListCount' and 'ListStartOffset' is requested, then it shall be applied first.
typedef struct
{	// Request In
	MBT_CHAR					FolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN]; // [M] PI should save the full folder path.
	MBT_BYTE					SubjectLen;		// [O] The value range shall be 1...255 in spec.
	MBT_SHORT					MaxListCount;	// [O] 2bytes in spec (PI: This shall be 1,024 if this header is not specified in spec.)
	MBT_SHORT					ListStartOffset;	// [O] 2bytes in spec (Starting from this value)
	MBT_UINT					ParamMask;		// [O] bit 0..bit 31 in spec. (MBT_MAP_ML_MASK)
	MBT_MAP_MSG_TYPE_MASK		FilterMsgType;	// [O]
	MBT_CHAR					FilterPeriodBegin[MBT_MAP_MAX_DATETIME_LEN];	// [O] The format shall be "YYYYMMDDTHHMMSS" in spec.
	MBT_CHAR					FilterPeriodEnd[MBT_MAP_MAX_DATETIME_LEN];		// [O] The format shall be "YYYYMMDDTHHMMSS" in spec.
	MBT_MAP_MSG_READ_STATUS		FilterReadStatus; // [O] 
	MBT_CHAR					FilterRecipient[MBT_MAP_MAX_FILTER_TEXT_LEN];		// [O] 
	MBT_CHAR					FilterOriginator[MBT_MAP_MAX_FILTER_TEXT_LEN];	// [O] 
	MBT_MAP_PRI_STATUS			FilterPriority;	// [O]
} MBT_MAP_GET_MSG_LIST_REQ;

typedef struct
{	// Request In
	MBT_SHORT					MaxListCount;	// [O] 2bytes in spec (default 1024)
	MBT_SHORT					ListStartOffset;	// [O] 2bytes in spec (default 0)
} MBT_MAP_GET_FOLDER_LIST_REQ;


// [MAP] Structure for PushMessage
typedef struct
{	// Request In
	MBT_CHAR					FolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN]; // [M] PI should save the full folder path.
	MBT_MAP_TRANSP_TYPE		Transp;			// [O]
	MBT_MAP_RETRY_TYPE		Retry;			// [O]
	MBT_MAP_MSG_CHARSET		Charset;			// [M]

	MBT_MAP_STORAGE_TYPE		StorageType;	// The storage type
	union
	{
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_BUFFER
		{
			MBT_MAP_MSG_OBJECT Msg;
			MBT_UINT			AllocatedContentSize;
		} Buffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_RAW_BUFFER
		{		
			MBT_UINT			Size;
			MBT_CHAR*			RawData;
		} RawBuffer;
		struct	// StorageType = MBT_MAP_STORAGE_TYPE_FILE or MBT_MAP_STORAGE_TYPE_RAW_FILE
		{			
			MBT_MAP_MSG_OBJECT Msg;			// This is used for only MBT_MAP_STORAGE_TYPE_FILE. (Except the message contents)
			MBT_UINT			Size;
			MBT_CHAR			FileName[MBT_MAX_FILE_NAME_LEN]; // The file name including the full path
		} File;
	} Data;
} MBT_MAP_PUSH_MSG_REQ;

typedef struct
{	// Response Out
	MBT_UINT64					MsgHandle;		// [C] The handle shall be a 64 bit unsigned integer in spec.
} MBT_MAP_PUSH_MSG_RSP;

typedef struct
{	// Request In
	MBT_MAP_NOTIF_STATUS		NotifStatus;		// [M]
} MBT_MAP_SET_NOTIF_REG_REQ;

typedef struct
{	// Request In
	MBT_UINT64					MsgHandle;		// [M] The handle shall be a 64 bit unsigned integer in spec.
	MBT_MAP_STATUS_IND		StatusInicator;	// [M]
	MBT_MAP_STATUS_VAL		StatusVal;		// [M]
} MBT_MAP_SET_MSG_STATUS_REQ;


// [MAP] Structure for SetFolder
typedef struct
{	// Request In
	MBT_CHAR					FolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN]; // [M] PI should save the full folder path.
} MBT_MAP_SET_FOLDER_REQ;





// MAP MAS Status
typedef struct
{

//	U8							MasInstId;		// MAS Instance ID (The value range shall be 0..255 in spec. But zero(0) is not used.)
//	U8							SvcName[MBT_MAP_MAX_SVC_NAME_LEN]; // The service name of MAS Instance
//	U8							SupMsgType;	// The message type(s) to be supported (SMS, MMS, Email)

	/*it is neccessary to keep the MAS info for porting*/
	MBT_MAP_MAS_INFO         	masInfo;

	BT_BOOL						registered;

	MBT_MAP_STATE				State;			// The state for MAS
	
	
	bt_addr_struct				BdAddr;			// The address of the remote device (MCE)
	MBT_CHAR					DevName[MBT_MAX_NAME_LEN]; // The name of the remote device (MCE)
//	MBT_BYTE					MasInfoIdx;		// The index for MasInfo
	
	MBT_MAP_OPERATION			OperType;		// The MAS operation for the access request
	MBT_CHAR					CurFolderPath[MBT_MAP_MAX_FOLDER_PATH_LEN]; // Current folder path
	MBT_AUTHRES					FailReason;		// This should only be used in case of the fail event.	

	MBT_MAP_FRAC_REQ          FracReq;        //only be used when get message
	
	union
	{	
		MBT_MAP_SET_NOTIF_REG_REQ		SetNotifReg;
		MBT_MAP_SET_FOLDER_REQ			SetFolderReq;
		MBT_MAP_SET_MSG_STATUS_REQ		SetMsgStatusReq;

		struct
		{
			MBT_MAP_GET_FOLDER_LIST_REQ	Req;
			MBT_MAP_GET_FOLDER_LIST_RSP	Rsp;
		} GetFolderList;
		struct
		{		
			MBT_MAP_GET_MSG_LIST_REQ	Req;
			MBT_MAP_GET_MSG_LIST_RSP		Rsp;
		} GetMsgList;
		struct
		{		
			MBT_MAP_GET_MSG_REQ			Req;
			MBT_MAP_GET_MSG_RSP			Rsp;
		} GetMsg;
		struct
		{				
			MBT_MAP_PUSH_MSG_REQ		Req;
			MBT_MAP_PUSH_MSG_RSP		Rsp;
		} PushMsg;		
	} Oper;
}MBT_MAP_MAS_STATUS;



// MAP MNS Status
typedef struct
{
	bt_addr_struct				BdAddr;			// The address of the remote device (MCE)
	MBT_MAP_STATE				State;			// The state for MNS
	MBT_MAP_MNS_NOTIF_REG		NotifReg[MBT_MAP_MAX_MAS_INST_NUM];
	MBT_MAP_SEND_EVENT_REQ		SendEvt;
} MBT_MAP_MNS_STATUS;


typedef void(*BTMTK_MAP_MMI_CALLBACK)(U8 event, S8* parameters);

typedef struct {
    btmtk_map_state state;

	
	//the filed may be not be needed 
  //  btmtk_map_op_struct ops[BTMTK_MAP_MAX_OPS];  /* in ascending time order */

//	MBT_MAP_MAS_INFO		masInfo[MBT_MAP_MAX_MAS_INST_NUM];
	MBT_MAP_MAS_STATUS		masStatus[MBT_MAP_MAX_MAS_INST_NUM];
	MBT_MAP_MNS_STATUS		mnsStatus[MBT_MAP_MAX_MCE_NUM];
	
    /* For XML/vCard creation */
    char file_buffer[MAP_ADP_BUFFER_SIZE];
    char builder_buffer[MAP_ADP_BUFFER_SIZE];

	//TODO: save memory;
//	char file_buffer[0];    // when neccessary, allocate the memory
//	char builder_buffer[0]; // when neccessary, allocate the memory

	BTMTK_MAP_MMI_CALLBACK MMI_callback;
} map_ext_cntx_struct;


void btmtk_map_server_init();

BT_BOOL btmtk_map_server_enable();
BT_BOOL btmtk_map_server_disable();
BT_BOOL btmtk_map_server_register(void *message);
BT_BOOL btmtk_map_server_deregister(U8 masId);

BT_BOOL btmtk_maps_authorize_res(bt_map_addr_struct *addr,U8 result);

BT_BOOL btmtk_maps_connect_rsp(U8 masId, kal_bool accept,
					kal_uint32 conn_id, bt_map_addr_struct *addr);
BT_BOOL btmtk_maps_disconnect(S8 masId);
BT_BOOL btmtk_maps_disconnect_server(void);
BT_BOOL btmtk_maps_set_folder(S8 masId, S8 result);
BT_BOOL btmtk_maps_get_folder_list(S8 masId, S8 result);
BT_BOOL btmtk_maps_get_message_list(S8 masId, S8 result);

BT_BOOL btmtk_maps_get_message(S8 masId,S8 result);
BT_BOOL btmtk_maps_push_message(S8 masId, S8 result);
BT_BOOL btmtk_maps_set_notif_reg(S8 masId, S8 result);
BT_BOOL btmtk_maps_set_message_status(S8 masId, S8 result);
BT_BOOL btmtk_maps_update_inbox(S8 masId, S8 result);
BT_BOOL btmtk_maps_send_event(bt_map_addr_struct *addr);
BT_BOOL btmtk_maps_mns_disconnect(bt_addr_struct *addr);
void btmtk_map_register_callback (BTMTK_MAP_MMI_CALLBACK mmicallback);
void btmtk_map_set_socket(int api_socket, int server_socket) ;
void BTCMD_SendMessage(U32 msg_id, module_type dest_mod, void *ptr, U16 size);

#endif


#endif
