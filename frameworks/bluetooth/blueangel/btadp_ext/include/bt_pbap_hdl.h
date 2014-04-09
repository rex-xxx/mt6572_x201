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
 * bt_pbap_hdl.h
 *
 * Project:
 * -------- 
 *  MAUI
 *
 * Description:
 * ------------
 *  phonebook access profile
 *
 * Author:
 * -------
 *  Xueling Li
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Log$
 *
 *
 * 
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/

#ifndef __BT_PBAP_HDL_H__
#define __BT_PBAP_HDL_H__

#include "bt_types.h"
#include "bluetooth_struct.h"
#include "bluetooth_pbap_struct.h"
  


#define BEGIN_XML_DECL      "<?xml version=\"1.0\"?>\r\n<!DOCTYPE vcard-listing SYSTEM \"vcard-listing.dtd\">\r\n<vcard-listing version=\"1.0\">\r\n"
//#define BEGIN_FOLDER_DECL   "<!DOCTYPE vcard-listing SYSTEM \"vcard-listing.dtd\">\r\n<vcard-listing version=\"1.0\">"
#define END_FOLDER_DECL     "</vcard-listing>\r\n"
#define BEGIN_HANDLE        "<card handle = \""
#define MIDDLE_HANDLE       "\" name = \""
#define END_HANDLE          "\"/>\r\n"


/*---------------------------------------------------------------------------
 * PbapVcardFilterBit type
 * 
 *     Describes the bit location pertaining to each filter value in 
 *     the 64-bit vCard filter.
 */
typedef U8 PbapVcardFilterBit;

#define PBAP_VCARD_FILTER_VER       0        /* Version (Bit 0) */
#define PBAP_VCARD_FILTER_FN        1        /* Formatted Name (Bit 1) */
#define PBAP_VCARD_FILTER_N         2        /* Structured Presentation of Name (Bit 2) */
#define PBAP_VCARD_FILTER_PHOTO     3        /* Associated Image or Photo (Bit 3) */
#define PBAP_VCARD_FILTER_BDAY      4        /* Birthday (Bit 4) */
#define PBAP_VCARD_FILTER_ADR       5        /* Delivery Address (Bit 5) */
#define PBAP_VCARD_FILTER_LABEL     6        /* Delivery (Bit 6) */
#define PBAP_VCARD_FILTER_TEL       7        /* Telephone (Bit 7) */
#define PBAP_VCARD_FILTER_EMAIL     8        /* Electronic Mail Address (Bit 8) */
#define PBAP_VCARD_FILTER_MAILER    9        /* Electronic Mail (Bit 9) */
#define PBAP_VCARD_FILTER_TZ        10       /* Time Zone (Bit 10) */
#define PBAP_VCARD_FILTER_GEO       11       /* Geographic Position (Bit 11) */
#define PBAP_VCARD_FILTER_TITLE     12       /* Job (Bit 12) */
#define PBAP_VCARD_FILTER_ROLE      13       /* Role within the Organization (Bit 13) */
#define PBAP_VCARD_FILTER_LOGO      14       /* Organization Logo (Bit 14) */
#define PBAP_VCARD_FILTER_AGENT     15       /* vCard of Person Representing (Bit 15) */
#define PBAP_VCARD_FILTER_ORG       16       /* Name of Organization (Bit 16) */
#define PBAP_VCARD_FILTER_NOTE      17       /* Comments (Bit 17) */
#define PBAP_VCARD_FILTER_REV       18       /* Revision (Bit 18) */
#define PBAP_VCARD_FILTER_SOUND     19       /* Pronunciation of Name (Bit 19) */
#define PBAP_VCARD_FILTER_URL       20       /* Uniform Resource Locator (Bit 20) */
#define PBAP_VCARD_FILTER_UID       21       /* Unique ID (Bit 21) */
#define PBAP_VCARD_FILTER_KEY       22       /* Public Encryption Key (Bit 22) */
#define PBAP_VCARD_FILTER_NICK      23       /* Nickname (Bit 23) */
#define PBAP_VCARD_FILTER_CAT       24       /* Categories (Bit 24) */
#define PBAP_VCARD_FILTER_PRODID    25       /* Product Id (Bit 25) */
#define PBAP_VCARD_FILTER_CLASS     26       /* Class Information (Bit 26) */
#define PBAP_VCARD_FILTER_SORT_STR  27       /* Sort string (Bit 27) */
#define PBAP_VCARD_FILTER_TIMESTAMP 28       /* Time stamp (Bit 28) */
/* Bits 29-38 Reserved for future use */
#define PBAP_VCARD_FILTER_PROP      39       /* Use of a proprietary filter (Bit 39) */
/* Bits 40-63 Reserved for proprietary filter usage */


typedef enum
{
    BT_PBAP_APP_STATE_IDLE,                                /*0*/
    BT_PBAP_APP_STATE_AUTHORIZING,                 /*1*/
    BT_PBAP_APP_STATE_AUTHORIZED,                   /*2*/
    BT_PBAP_APP_STATE_ACTIVE,                            /*3*/
    BT_PBAP_APP_STATE_CONNECTED,                     /*4*/
    BT_PBAP_APP_STATE_BUILDING_FOLDER,          /*5*/
    BT_PBAP_APP_STATE_BUILDING_ENTRY,            /*6*/
    BT_PBAP_APP_STATE_BUILDING_LIST,               /*7*/
    BT_PBAP_APP_STATE_DISCONNECTING,             /*8*/
    BT_PBAP_APP_STATE_DEACTIVATING,                /*9*/
    BT_PBAP_APP_STATE_ACTIVATING,                   /*10*/
    BT_PBAP_APP_STATE_TATOL
} BT_PBAP_APP_STATE;

typedef enum
{
    BT_PBAP_FOLDER,                               /*0*/
    BT_PBAP_LIST,                 /*1*/
    BT_PBAP_ENTRY,                  /*2*/
 
} BT_PBAP_FILE_TYPE;

/****************************************************************************
 *
 * Constants
 *
 ****************************************************************************/

 /****************************************************************************
 *
 * Types
 *
 ****************************************************************************/


/*--------------------------------------------------------------------------
 * PbPullPhonebookOp structure
 *
 *     Description
 */
typedef struct _PbPullPhonebookOp
{
    U8 storage;
    U8 pb_name;
    PbapVcardFilter filter;
    PbapVcardFormat format;
    U16 maxListCount;
    U16 listStartOffset;
 } PbPullPhonebookOp;

/*--------------------------------------------------------------------------
 * PbPullVcardListingOp structure
 *
 *     Description
 */
typedef struct _PbPullVcardListingOp
{
    U8  storage;
    U8  dir;
    U8 searchAttribute;
    U8 searchValue[MAX_PBAP_SEARCH_VALUE_LENGTH + 1]; //ucs encoding
    U16 searchValueLength ;
    U8 order;
    U16 maxListCount;
    U16 listStartOffset;
    PbapVcardFormat format;                              /* Format of vCard (2.1 or 3.0) */
} PbPullVcardListingOp;

/*--------------------------------------------------------------------------
 * PbPullVcardEntryOp structure
 *
 *     Description
 */
typedef struct _PbPullVcardEntryOp
{
    U8 storage;
    U8 dir;
    U32 entry_index;
    PbapVcardFilter filter;                 /* Filter of the required vCard fields */
    PbapVcardFormat format;                              /* Format of vCard (2.1 or 3.0) */
} PbPullVcardEntryOp;

typedef struct _PbPhonebookPath
{
    PbapPhonebookStrorage storage;
    PbapPhonebookName dir;
} PbPhonebookPath;


/*---------------------------------------------------------------------------
 * PBAP_IsSetFilterBit()
 *
 *     Returns the status of the appropriate filter bit in the 64-bit 
 *     vCard filter.
 *
 * Parameters:
 *     Bit - Bit to check in the vCard filter.
 *
 *     Filter - vCard filter structure.
 *
 * Returns:
 *     TRUE or FALSE
 */

typedef struct _BT_PBAP_APP_CTX
{
    BT_PBAP_APP_STATE  server_state;
    PbPhonebookPath  current_path;  
    PbPullVcardListingOp vcard_listing;
    PbPullPhonebookOp vcard_folder;
    PbPullVcardEntryOp vcard_entry;
    S16 cur_list_Count;
    U8 auth_option;
    U32 cm_conn_id;
    EvmTimer obex_auth_timer;
    bt_pbap_bd_addr_struct pbap_bt_device;
    S8 pbap_dev_name[BT_PBAP_MAX_DEV_NAME_LEN];
   BT_BOOL disconnect_from_cm;
} BT_PBAP_APP_CTX;

void btmtk_int_pbap(void);
void btmtk_deint_pbap(void);

void btmtk_pbap_active_req(U8 security_level, U8 support_repos);
void btmtk_pbap_deactive_req(void);


void btmtk_pbap_authrize_rsp(U8 cnf_code);

void btmtk_pbap_disconnect_req(BT_BOOL disconnect_tp_directly);


void btmtk_pbap_read_entry_rsp(U8 result);
void btmtk_pbap_read_folder_rsp(U8 result,
								U16 phoneBookSize,
								U16 newMissedCalls);


void btmtk_pbap_read_list_rsp(U8 result,
							  U16 phoneBookSize,
							  U16 newMissedCalls);



void btmtk_pbap_obex_auth_challege_rsp(U8 cancel,
									   U8* password,  U16 password_length,
									   U8* userID, U16 userID_length);

//U8  btmtk_pbap_set_app_callback(BTMTK_PBAP_CALLBACK callback);

void btmtk_pbap_set_filename(U8 * path,  U16 len, U8 file_type);

#endif /* __BT_MMI_PBAP_H__ */ 

