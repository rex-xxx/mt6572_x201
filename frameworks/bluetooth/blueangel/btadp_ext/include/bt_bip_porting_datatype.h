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
 * bt_bip_porting_datatype.h
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
#ifndef __BT_BIP_PORTING_DATATYPE_H__
#define __BT_BIP_PORTING_DATATYPE_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "bt_types.h" 

#define BDADDR_LEN 6
typedef U8 T_BDADDR[BDADDR_LEN];

#define OBEX_AUTH_USERID_LEN    20
#define OBEX_AUTH_PASSWD_LEN    16
typedef struct
{
    BT_BOOL bAuth;
    S8 UserId[OBEX_AUTH_USERID_LEN + 1];
    S8 Passwd[OBEX_AUTH_PASSWD_LEN + 1];
} T_OBEX_AUTH;


//OPP
typedef enum
{
    FORBID_RES = 0,             // authorization reject
    ALLOW_RES,                  // authorization allow
    ERROR_RES,                  // error response
    UNAUTHORIZED_RES,           //read only
    NOT_FOUND_RES,              //file not found
    UNSUPPORTED_MEDIA_TYPE_RES, //unsupport type
    SERVICE_UNAVAILABLE_RES,    //unsupport function
    DATABASE_FULL_RES,          //no space
    INTERNAL_SERVER_ERROR_RES,  //etc
    UNSUPPORTED_DEVICE_TYPE_RES,
    BAD_REQUEST_RES,
    FORBIDDEN_RES,
    NOT_ACCEPTABLE_RES,
    PRECONDITION_FAILED_RES,
    NOT_IMPLEMENTED_RES,
} T_AUTHRES;

#define MAX_FILE_NAME_LEN 256
#define MAX_NUM_OF_NESTED_ATTR  20
// BIP Transformation
typedef enum
{
    BIP_TRANS_NONE = 0,
    BIP_TRANS_STRETCH,
    BIP_TRANS_FILL,
    BIP_TRANS_CROP
} T_BIP_TRANS;

// BIP Image Descriptor
typedef struct
{
    char Version[10];   // Image Descriptor version (exe) "1.0", etc)
    char Encoding[30];  // Image Encoding Type String (ex)"JPEG","GIF","BMP","WBMP","PNG", etc)
//(Width*Height - Width2*Height2) if range and not bound by aspect ratio
//or (Width** - Width2*Height2) if range and bound by aspect ratio (Height is 0)
//or (Width*Height), if not a range (Width2 or Height2 is 0) 
    // BIP PutImage Function is not used Width2 and Height2.
    U32 Width;   // width
    U32 Height;  // height - 0 if use range and ratio
    U32 Width2;  // width - 0 if not range
    U32 Height2; // height - 0 if not range
    U32	Size;    // Image file size
    T_BIP_TRANS Transform;
} T_BIP_IMAGE_DESC;

// BIP Image Format
typedef struct
{
    char Encoding[30]; // Image Encoding Type String (ex)"JPEG","GIF","BMP","WBMP","PNG", etc)
//(Width*Height - Width2*Height2) if range and not bound by aspect ratio
//or (Width** - Width2*Height2) if range and bound by aspect ratio (Height is 0)
//or (Width*Height), if not a range (Width2 or Height2 is 0)
    U32 Width;  // width
    U32 Height; // height - 0 if use range and ratio
    U32 Width2; // width - 0 if not range
    U32 Height2;// height - 0 if not range
    U32 Size;   // Image file size
} T_BIP_IMAGE_FORMAT;

// BIP Object
typedef struct
{
    char DirName[MAX_FILE_NAME_LEN];   // Directory path
    char FileName[MAX_FILE_NAME_LEN];  // Filename
    char ThumbnailFullPath[MAX_FILE_NAME_LEN];       // Thumbnail fullname (*only used the QBT solution of LGE : PushImage . .. .... .)
    U32 ObjectSize;                    // Object Total Size
    U32 AcceptableFileSize;            // Maximum size (UI .. ..)
    T_BIP_IMAGE_DESC ImageDesc;        // Image Descriptor
} T_BIP_OBJECT;


typedef struct
{
    T_BIP_IMAGE_DESC PreferFormat; // Image format prefer to receive
    T_BIP_IMAGE_FORMAT ImageFormats[MAX_NUM_OF_NESTED_ATTR]; // Image format can be retrieved by other devices
    U32 NumImageFormats;
} T_BIP_IMAGING_CAPABILITY;







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


typedef enum
{
    BIP_INITIATOR_ENABLE_SUCCESS= 0, // MBT_BIP_EVT_START,   // 1100, BIP service ... ..
    BIP_INITIATOR_ENABLE_FAIL,               // 1101, BIP service ... ..             //Not used
    BIP_INITIATOR_DISABLE_SUCCESS,           // 1102, BIP service .... ..
    BIP_INITIATOR_DISABLE_FAIL,              // 1103, BIP service .... ..            //Not used
    BIP_INITIATOR_OBEX_AUTHREQ,              // 1104, OBEX Authentication Req ... . ..
    BIP_INITIATOR_CONNECT_SUCCESS,           // 1105, Not send to UI
    BIP_INITIATOR_CONNECT_FAIL,              // 1106, Not send to UI
    BIP_INITIATOR_GET_CAPABILITY_SUCCESS,    // 1107, BIP service. Responder. Capa .... ..
    BIP_INITIATOR_GET_CAPABILITY_FAIL,       // 1108, BIP service. Responder. Capa .... ..
    BIP_INITIATOR_IMAGE_PUSH_START,          // 1109, BIP service. ... .. ..
    BIP_INITIATOR_PROGRESS,                  // 1110
    BIP_INITIATOR_IMAGE_PUSH_SUCCESS,        // 1111, BIP service. ... .. ..
    BIP_INITIATOR_IMAGE_PUSH_FAIL,           // 1112, BIP service. ... .. ..
    BIP_INITIATOR_THUMBNAIL_REQ,             // 1113
    BIP_INITIATOR_THUMBNAIL_PUSH_START,      // 1114, BIP service. Thumbnail .. ..
    BIP_INITIATOR_THUMBNAIL_PUSH_SUCCESS,    // 1115, BIP service. Thumbnail .. ..
    BIP_INITIATOR_THUMBNAIL_PUSH_FAIL,       // 1116, BIP service. Thumbnail .. ..
    BIP_INITIATOR_DISCONNECT_SUCCESS,        // 1117
    BIP_INITIATOR_DISCONNECT_FAIL,           // 1118
    BIP_INITIATOR_CANCEL_SUCCESS,            // 1119, BIP service .. .. ..
    BIP_INITIATOR_CANCEL_FAIL,               // 1120, BIP service .. .. ..   //Not used
    BIP_RESPONDER_ENABLE_SUCCESS,            // 1121
    BIP_RESPONDER_ENABLE_FAIL,               // 1122, Not used
    BIP_RESPONDER_DISABLE_SUCCESS,           // 1123
    BIP_RESPONDER_DISABLE_FAIL,              // 1124, Not used
    BIP_RESPONDER_AUTH_REQ,                  // 1125, BIP Responder Authorize Req
    BIP_RESPONDER_OBEX_AUTHREQ,              // 1126, OBEX Authentication Req ... . ..
    BIP_RESPONDER_CONNECT_SUCCESS,           // 1127, Not send to UI
    BIP_RESPONDER_CONNECT_FAIL,              // 1128, Not send to UI
    BIP_RESPONDER_ACCESS_REQ,                // 1129
    BIP_RESPONDER_GET_CAPABILITY_REQ,        // 1130
    BIP_RESPONDER_CAPABILITY_RES_SUCCESS,    // 1131
    BIP_RESPONDER_IMAGE_RECEIVE_START,       // 1132
    BIP_RESPONDER_PROGRESS,                  // 1133
    BIP_RESPONDER_IMAGE_RECEIVE_SUCCESS,     // 1134
    BIP_RESPONDER_IMAGE_RECEIVE_FAIL,        // 1135
    BIP_RESPONDER_THUMBNAIL_RECEIVE_START,   // 1136
    BIP_RESPONDER_THUMBNAIL_RECEIVE_SUCCESS, // 1137
    BIP_RESPONDER_THUMBNAIL_RECEIVE_FAIL,    // 1138
    BIP_RESPONDER_DISCONNECT_SUCCESS,        // 1139
    BIP_RESPONDER_DISCONNECT_FAIL,           // 1140
    BIP_MAX,

} T_BIP_EVENT;


#define MAX_DIGIT_SIZE  10
typedef enum
{
    PreferFormat_Version = 0,
    PreferFormat_Encoding,
    PreferFormat_Width,
    PreferFormat_Height,
    PreferFormat_Width2,
    PreferFormat_Height2,
    PreferFormat_Size,
    Transform,
    ImageFormats_NumImageFormats,
    ImageFormats_Encoding = PreferFormat_Encoding,
    ImageFormats_Width = PreferFormat_Width,
    ImageFormats_Height = PreferFormat_Height,
    ImageFormats_Width2 = PreferFormat_Width2,
    ImageFormats_Height2 = PreferFormat_Height2,
    ImageFormats_Size = PreferFormat_Size,
}BIP_IMAGE_FORMAT; 



#ifdef __cplusplus
}
#endif
#endif //#ifndef __BT_BIP_PORTING_DATATYPE_H__


