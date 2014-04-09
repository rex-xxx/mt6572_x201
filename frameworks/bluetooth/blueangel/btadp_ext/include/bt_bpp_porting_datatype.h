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
/*****************************************************************************
 *
 * Filename:
 * ---------
 * bt_bpp_porting_datatype.h
 *
 * Project:
 * --------
 * YuSu
 *
 * Description:
 * ------------
 * Data Type for APIs
 *
 * Author:
 * -------
 * Paul Chuang
 *
 ****************************************************************************/
#ifndef __BT_BPP_PORTING_DATATYPE_H__
#define __BT_BPP_PORTING_DATATYPE_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "bt_types.h" 

#define MAX_DIGIT_SIZE 10


#define MAX_FILE_NAME_LEN 256
#define MAX_DOC_FORMAT_LEN 63
#define MAX_MEDIA_SIZE_LEN 31
#define MAX_ATTR_VAL_LEN	32
#define MAX_NUM_OF_NESTED_ATTR	20
#define MAX_MIME_VAL_LEN	64

//GAP
#define BDADDR_LEN 6
typedef U8 T_BDADDR[BDADDR_LEN];
#define MAX_NAME_LEN		80	
//GAP end

//OBEX
#define OBEX_AUTH_USERID_LEN	20	// OBEX Auth User ID Length
#define OBEX_AUTH_PASSWD_LEN	16	// OBEX Auth Password Length

	
typedef struct
{
	BT_BOOL	bAuth;
	S8	UserId[OBEX_AUTH_USERID_LEN + 1];
	S8	Passwd[OBEX_AUTH_PASSWD_LEN + 1];
} T_OBEX_AUTH;
//OBEX end


//OPP
typedef enum
{
	FORBID_RES = 0,				// authorization reject
	ALLOW_RES,					// authorization allow
	ERROR_RES,					// error response
	UNAUTHORIZED_RES,			//read only
	NOT_FOUND_RES,				//file not found
	UNSUPPORTED_MEDIA_TYPE_RES, //unsupport type
	SERVICE_UNAVAILABLE_RES,	//unsupport function
	DATABASE_FULL_RES,			//no space
	INTERNAL_SERVER_ERROR_RES,	//etc
	UNSUPPORTED_DEVICE_TYPE_RES,
	BAD_REQUEST_RES,
	FORBIDDEN_RES,
	NOT_ACCEPTABLE_RES,
	PRECONDITION_FAILED_RES,
	NOT_IMPLEMENTED_RES,
} T_AUTHRES;


typedef enum
{
	MIME_TYPE_APPLICATION_XHTML_PRINT = 0,	// XHTML-Print 0.95(application/vnd.pwg-xhtml-print+xml:0.95)
	MIME_TYPE_APPLICATION_XHTML_PRINT10,	// XHTML-Print 0.95(application/vnd.pwg-xhtml-print+xml:1.0)
	MIME_TYPE_APPLICATION_MULTIPLEXED,		// Multiplexed (application/vnd.pwg-multiplexed)
	MIME_TYPE_TEXT_PLAIN,					// Basic Text
	MIME_TYPE_TEXT_VCARD,					// vCard 2.1 (text/x-vcard:2.1)
	MIME_TYPE_TEXT_VCARD30,					// vCard 3.0 (text/x-vcard:3.0)
	MIME_TYPE_TEXT_VCALENDAR,				// vCal 1.0 (text/x-vcalendar:1.0)
	MIME_TYPE_TEXT_ICALENDAR20,				// iCal 2.0 (text/calendar:2.0)
	MIME_TYPE_TEXT_VMESSAGE,				// vMessage 1.1 (text/x-vmessage:1.1)
	MIME_TYPE_TEXT_VNOTE,					// vNote 1.1 (text/x-vnote:1.1)
	MIME_TYPE_IMAGE_JPEG,					// (image/jpeg)
	MIME_TYPE_IMAGE_GIF,					// (image/gif)
	MIME_TYPE_APPLICATION_POSTSCRIPT,		// (application/postscript)
	MIME_TYPE_APPLICATION_HP_PCL_5E,
	MIME_TYPE_APPLICATION_HP_PCL_3C,
	MIME_TYPE_APPLICATION_PDF,
	MIME_TYPE_REF_SIMPLE,
	MIME_TYPE_REF_XML,
	MIME_TYPE_REF_LIST,
	MIME_TYPE_SOAP,
	MIME_TYPE_REFERENCED_OBJ,
	MIME_TYPE_RUI,
	MIME_TYPE_IMG_IMG,
	MIME_TYPE_IMG_HEADER
} T_MIME_MEDIA;


typedef enum
{
	PRINT_JOBSTATE_PRINTING = 1,	// When the remote device receives and prints data
	PRINT_JOBSTATE_WAITING,			// When the sender waits for the printer (remote device)
	PRINT_JOBSTATE_STOPPED,			// The print-session has stopped for some reason
	PRINT_JOBSTATE_COMPLETED,		// When the print-session has ended successfully
	PRINT_JOBSTATE_ABORTED,			// The print-session has been aborted
	PRINT_JOBSTATE_CANCELLED,		// The print-session has been canceled
	PRINT_JOBSTATE_UNKNOWN			// An unknown event has happen during a print-session
} T_BPP_PRINT_JOB_STATE;

typedef enum
{
	PRINTER_ST_UNKNOWN = 0,	// Unknown
	PRINTER_ST_IDLE,		// Idle
	PRINTER_ST_PROCESSING,	// Processing
	PRINTER_ST_STOPPED		// Stopped
} T_BPP_PRINTER_STATE;

typedef enum
{
	PRINTER_SR_NONE = 0,			// No reason given
	PRINTER_SR_ATT_REQ,				// Attention required on the printer
	PRINTER_SR_MED_JAM,				// Media Jam
	PRINTER_SR_PAUSED,				// Paused
	PRINTER_SR_DOOR_OPEN,			// One or more covers on device are open
	PRINTER_SR_MED_LOW,				// At least one media tray is low
	PRINTER_SR_MED_EMPTY,			// At least one media tray is empty
	PRINTER_SR_OUTAREA_ALMOSTFULL,	// Output area almost full
	PRINTER_SR_OUTAREA_FULL,		// Output area full
	PRINTER_SR_MARKER_LOW,			// Device low on ink or toner
	PRINTER_SR_MARKER_EMPTY,		// Device out of ink or toner
	PRINTER_SR_MARKER_FAILURE		// Device ink cartridge or toner ribbon error
} T_BPP_PRINTER_STATE_REASON;

typedef enum
{
	MTYPE_UNDEF = 0,
	MTYPE_STATIONERY,
	MTYPE_STATIONERY_COATED,
	MTYPE_STATIONERY_INKJET,
	MTYPE_STATIONERY_PREPRINTED,
	MTYPE_STATIONERY_LETTERHEAD,
	MTYPE_STATIONERY_PREPUNCHED,
	MTYPE_STATIONERY_FILE,
	MTYPE_STATIONERY_HEAVYWEIGHT,
	MTYPE_STATIONERY_LIGHTWEIGHT,
	MTYPE_TRANSPARENCY,
	MTYPE_ENVELOPE,
	MTYPE_ENVELOPE_PLAIN,
	MTYPE_ENVELOPE_WINDOW,
	MTYPE_CONTINUOUS,
	MTYPE_CONTINUOUS_LONG,
	MTYPE_CONTINUOUS_SHORT,
	MTYPE_TAB_STOCK,
	MTYPE_PRE_CUT_TABS,
	MTYPE_FULL_CUT_TABS,
	MTYPE_MULTI_PART_FORM,
	MTYPE_LABELS,
	MTYPE_MULTI_LAYER,
	MTYPE_SCREEN,
	MTYPE_SCREEN_PAGED,
	MTYPE_PHOTOGRAPHIC,
	MTYPE_PHOTOGRAPHIC_GLOSSY,
	MTYPE_PHOTOGRAPHIC_HIGH_GLOSS,
	MTYPE_PHOTOGRAPHIC_SEMI_GLOSS,
	MTYPE_PHOTOGRAPHIC_SATIN,
	MTYPE_PHOTOGRAPHIC_MATTE,
	MTYPE_PHOTOGRAPHIC_FILM,
	MTYPE_BACK_PRINT_FILM,
	MTYPE_CARDSTOCK,
	MTYPE_ROLL
} T_BPP_MEDIA;


typedef enum
{
	BPP_STATE_DISABLED = 0,
	BPP_STATE_ENABLED,	
	BPP_STATE_CONNECTING,	
	BPP_STATE_CONNECTED,		
	BPP_STATE_DISCONNECTING,
	BPP_STATE_GETATTRIBUTE,
	BPP_STATE_PRINTING			
} T_BPP_STATE;

typedef struct
{
	U16		Copies;
	U16		NumberUp;
	U8		Sides;
	U8		Orient;
	U8		Quality;
	char  DocFmt[MAX_DOC_FORMAT_LEN];
	char	MediaSize[MAX_MEDIA_SIZE_LEN];
	T_BPP_MEDIA MediaType;
} T_BPP_ATTRIBUTE;


typedef struct
{
	S8		PrinterName[MAX_ATTR_VAL_LEN];
	S8		PrinterLocation[MAX_ATTR_VAL_LEN];
	T_BPP_PRINTER_STATE		PrinterState;
	T_BPP_PRINTER_STATE_REASON	PrinterStateReasons;
	U8		NumDocFmtSupported;
	S8		DocFmtSupported[MAX_NUM_OF_NESTED_ATTR][MAX_MIME_VAL_LEN];
	BT_BOOL		bColorSupported;
	U8		SidesSupported;
	U16		MaxCopiesSupported;
	U16		MaxNumberUp;
	U8		OrientationsSupported;
	U8		NumMediaSizesSupported;
	S8		MediaSizesSupported[MAX_NUM_OF_NESTED_ATTR][MAX_ATTR_VAL_LEN];
	U8		NumMediaTypesSupported;
	T_BPP_MEDIA	MediaTypesSupported[MAX_NUM_OF_NESTED_ATTR];
	T_BPP_MEDIA	LoadedMediaType[MAX_NUM_OF_NESTED_ATTR];
	U8		NumLoadedMedia;
	U8		PrintQualitySupported;
	U8		NumImageFmtSupported;
	S8		ImageFmtSupported[MAX_NUM_OF_NESTED_ATTR][MAX_ATTR_VAL_LEN];
	U16		QueuedJobCount;
	U16		BasicTextPageWidth;
	I32			OperationStatus;
	U16		BasicTextPageHeight;
	S8		PrinterGeneralCurrentOperator[MAX_ATTR_VAL_LEN];
} T_BPP_CAPABILITY;



typedef struct
{
	T_BPP_PRINT_JOB_STATE		PrintJobState;			// Waiting, stopped, cancelled, unknown, etc.
	T_BPP_PRINTER_STATE		PrinterState;			// State - idle, processing, or stopped
	T_BPP_PRINTER_STATE_REASON	PrinterStateReasons;	// Reason in current state
} T_BPP_JOB_STATUS;

typedef struct
{
  char             DirName[MAX_FILE_NAME_LEN];
  char             FileName[MAX_FILE_NAME_LEN];
  T_MIME_MEDIA     MimeType;
  BT_BOOL             bJobBasedPrinting;
  U32              ObjectSize;
  T_BPP_ATTRIBUTE  PrintingAttribute;
  T_BPP_JOB_STATUS JobStatus;
} T_BPP_OBJECT;


typedef struct
{
	BT_BOOL				bEnabled;
	BT_BOOL				bIsObexUserID;						
	char				RemoteDevName[MAX_NAME_LEN];	
	T_BDADDR			BDAddr;			
	U32				TxProgress;		
	T_BPP_OBJECT		PrintingFile;	
	T_BPP_CAPABILITY	PrinterCapability;
	T_BPP_STATE			State;			
	T_AUTHRES			FailReason;		
} T_BPP_STATUS;


/*
//g_state
typedef enum
{
    BIP_STATE_DISABLED = 0,     // BIP .... ..... ..  (Default)
    BIP_STATE_ENABLED,          // BIP .... ... . ..
    BIP_STATE_CONNECTING,       // BIP .... remote device. .... ..
    BIP_STATE_CONNECTED,        // BIP .... remote device. ... ..
    BIP_STATE_DISCONNECTING,    // BIP ... .. .... ..
    BIP_STATE_SENDING,          // .. .. .. ..
    BIP_STATE_RECEIVING         // .. .. .. ..
} T_BIP_STATE;
*/

typedef enum
{
    BPP_ENABLE_SUCCESS = 0,     // 600, BPP Service Enable Success
    BPP_ENABLE_FAIL,            // 601, BPP Service Enable Fail
    BPP_DISABLE_SUCCESS,        // 602, BPP Service Disable Success
    BPP_DISABLE_FAIL,           // 603, BPP Service Disable Fail
    BPP_OBEX_AUTHREQ,           // 604, OBEX Authentication Request
    BPP_CONNECT_SUCCESS,        // 605, BPP Connection Success //(Not send to UI)
    BPP_CONNECT_FAIL,           // 606, BPP Connection Fail  //(Not send to UI)
    BPP_GET_PRINT_ATTR_SUCCESS, // 607
    BPP_GET_PRINT_ATTR_FAIL,    // 608
    BPP_PROGRESS,               // 609, BPP service
    BPP_PRINT_STATUS,           // 610, BPP Print Status (Only Job-Based Transfer)
    BPP_PRINT_COMPLETE_SUCCESS, // 611, BPP service
    BPP_PRINT_COMPLETE_FAIL,    // 612, BPP service
    BPP_DISCONNECT_SUCCESS,     // 613, BPP Connection Release Success
    BPP_DISCONNECT_FAIL,        // 614, BPP Connection Release Fail
    BPP_CANCEL_SUCCESS,         // 615, BPP service
    BPP_CANCEL_FAIL,            // 616, BPP service
    BPP_MAX,
} T_BPP_EVENT;




#ifdef __cplusplus
}
#endif
#endif //#ifndef __BT_BPP_PORTING_DATATYPE_H__
