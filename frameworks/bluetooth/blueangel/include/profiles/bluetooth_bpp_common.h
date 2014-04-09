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
 * bluetooth_bpp_common.h
 *
 * Project:
 * --------
 *   Maui
 *
 * Description:
 * ------------
 *   struct 
 *
 * Author:
 * -------
 * Yufeng chu
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: #1 $
 * $Modtime: $
 * $Log: $
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/

#ifndef __BLUETOOTH_BPP_COMMON_H__
#define __BLUETOOTH_BPP_COMMON_H__

/* commen defination & data type/structure */

#ifdef __ON_MAUI__
typedef kal_bool BT_BOOL;
#endif

/* Printer Handle */
typedef int PRINTER_HANDLE;

#define BPP_INVALID_PRINTER_HANDLE (-1)

#define BPP_MAX_FILE_NAME_LEN 128

/* data size */
#define BTMTK_MAX_FILE_NAME_LEN 256

#define BTMTK_OBEX_AUTH_USERID_LEN 20
#define BTMTK_OBEX_AUTH_PASSWD_LEN 20

#define BTMTK_BPP_MAX_PRINTER_NAME_LEN 80
#define BTMTK_BPP_MAX_PRINTER_LOCT_LEN 80

#define BPP_MAX_MEDIA_SIZE_LEN		32


/* for JobName & OriginatingUseName */
#define BPP_MAX_JOB_NAME_LEN  64
#define BPP_MAX_USER_NAME_LEN  64

/* max nested attr number, 
 * such as MediaTypeSupported in 
 * GetPrinterAttributes response SOAP */
#define BPP_MAX_NESTED_ATTR_NUM  20
	
#define BPP_MAX_IMGFMT_SUPPORTED_NUM  8
#define BPP_MAX_LOADED_MEDIA_NUM  4


/* printer attributes to get: U32 BITMASK */
#define BPP_PRINTER_NAME_MASK		 		 	0x00000001		
#define BPP_PRINTER_LOCATION_MASK			 	0x00000002
#define BPP_PRINTER_STATE_MASK				 	0x00000004
#define BPP_PRINTER_STATEREASONS_MASK		 	0x00000008
#define BPP_DOCUMENT_FORMATS_SUPPORTED_MASK 	0x00000010
#define BPP_COLOR_SUPPORTED_MASK			 	0x00000020
#define BPP_MAX_COPIES_SUPPORTED_MASK		 	0x00000040
#define BPP_SIDES_SUPPORTED_MASK			 	0x00000080
#define BPP_NUMBERUP_SUPPORTED_MASK		 		0x00000100
#define BPP_ORIENTATIONS_SUPPORTED_MASK	 		0x00000200
#define BPP_MEDIA_SIZES_SUPPORTED_MASK		 	0x00000400
#define BPP_MEDIA_TYPES_SUPPORTED_MASK		 	0x00000800
#define BPP_MEDIA_LOADED_MASK				 	0x00001000
#define BPP_PRINT_QUALITY_SUPPORTED_MASK	 	0x00002000
#define BPP_QUEUED_JOB_COUNT_MASK			 	0x00004000
#define BPP_IMAGE_FORMATS_SUPPORTED_MASK	 	0x00008000
#define BPP_BASIC_TEXT_PAGE_WIDTH_MASK		 	0x00010000
#define BPP_BASIC_TEXT_PAGE_HEIGHT_MASK	 		0x00020000
#define BPP_PRINTER_GENERALCURRENTOPERATOR_MASK	0x00040000


typedef enum
{

    BPP_ERROR_LINK_DISC = -127,
    BPP_ERROR_PEER_ABORT,
    BPP_ERROR_USER_ABORT,
    BPP_ERROR_CREATE_JOB_FAILED,
    BPP_ERROR_SCO_REJECT,
    BPP_ERROR_SERVICE_NOT_FOUND,
    BPP_ERROR_INTERNAL_ERR,
    BPP_ERROR_DEVICE_BUSY,
    BPP_ERROR_DISC_STATUS_FIRST,
    BPP_ERROR_INVALID_PARAM,

    BPP_ERROR = -1,
    BPP_SUCCESS = 0,
    BPP_PENDING
    
} BPP_ERROR_CODE;


/* MIME Media Type */
typedef enum
{
	BPP_MIME_TYPE_UNKNOWN = 0,

	/* xhtml-print*/
	BPP_MIME_APPLICATION_XHTML_PRINT_095 = 1, // XHTML-Print 0.95(application/vnd.pwg-xhtml-print+xml:0.95)
	BPP_MIME_APPLICATION_XHTML_PRINT_100,	// XHTML-Print 1.0(application/vnd.pwg-xhtml-print+xml:1.0)

	/* multiplexed */
	BPP_MIME_APPLICATION_MULTIPLEXED,		// Multiplexed (application/vnd.pwg-multiplexed)

	/* text */
	BPP_MIME_TEXT_PLAIN,				// Basic Text
	BPP_MIME_TEXT_VCARD,			// vCard 2.1 (text/x-vcard:2.1)
	BPP_MIME_TEXT_VCARD30,			// vCard 3.0 (text/x-vcard:3.0)
	BPP_MIME_TEXT_VCALENDAR,			// vCal 1.0 (text/x-vcalendar:1.0)
	BPP_MIME_TEXT_ICALENDAR20,			// iCal 2.0 (text/calendar:2.0)
	BPP_MIME_TEXT_VMESSAGE,			// vMessage 1.1 (text/x-vmessage:1.1)
	BPP_MIME_TEXT_VNOTE,			// vNote 1.1 (text/x-vnote:1.1)

	/* image */
	BPP_MIME_IMAGE_JPEG,				// (image/jpeg)
	BPP_MIME_IMAGE_GIF,				// (image/gif)
	BPP_MIME_IMAGE_BMP,				// (image/bmp)
	BPP_MIME_IMAGE_WBMP,			// (image/wbmp)
	BPP_MIME_IMAGE_PNG,				// (image/png)
	BPP_MIME_IMAGE_SVG,				// (image/svg)

	BPP_MIME_TYPE_MAX_ENUM
	
} bt_bpp_mime_type;


/* print mode */
typedef enum
{
	BPP_MODEL_SIMPLE_PUSH, /* Simple Push */
		
	BPP_MODEL_JOB_BASE     /* Job-base */
	
}bt_bpp_print_model;


/* printing side */
typedef enum
{
	BPP_SIDES_IGNORED 			= 0x00,
    BPP_ONE_SIDED 				= 0x01,
    BPP_TWO_SIDED_LONG_EDGE 	= 0x02,
    BPP_TWO_SIDED_SHORT_EDGE 	= 0x04
    
} bt_bpp_sided_enum;

/* printing orientation */
typedef enum
{
	BPP_ORIENT_IGNORED 				= 0x00,
    BPP_ORIENT_PORTRAIT 			= 0x01,
    BPP_ORIENT_LANDSCAPE 			= 0x02,
    BPP_ORIENT_REVERSE_PORTRAIT 	= 0x04,
    BPP_ORIENT_REVERSE_LANDSCAPE 	= 0x08
    
} bt_bpp_orient_enum;

/* printing quality */
typedef enum
{
    BPP_QUALITY_IGNORED 	= 0x00,
    BPP_QUALITY_NORMAL 		= 0x01,
    BPP_QUALITY_DRAFT              = 0x02,
    BPP_QUALITY_HIGH 		= 0x04
    
} bt_bpp_quality_enum;

/* printing number up */
typedef enum
{
    BPP_ONE_PAGE_PER_SIDE = 1,
    BPP_TWO_PAGE_PER_SIDE = 2,
    BPP_FOUR_PAGE_PER_SIDE = 4,
    
    BPP_NUMBERUP_MAX_ENUM
    
} bt_bpp_numberup_enum;


/* printer media type */
typedef enum
{
    BPP_MEDIA_TYPE_UNDEF = 0,
    BPP_MEDIA_TYPE_STATIONERY,
    BPP_MEDIA_TYPE_STATIONERY_COATED,
    BPP_MEDIA_TYPE_STATIONERY_INKJET,
    BPP_MEDIA_TYPE_STATIONERY_PREPRINTED,
    BPP_MEDIA_TYPE_STATIONERY_LETTERHEAD,
    BPP_MEDIA_TYPE_STATIONERY_PREPUNCHED,
    BPP_MEDIA_TYPE_STATIONERY_FINE,
    BPP_MEDIA_TYPE_STATIONERY_HEAVYWEIGHT,
    BPP_MEDIA_TYPE_STATIONERY_LIGHTWEIGHT,
    BPP_MEDIA_TYPE_TRANSPARENCY,
    BPP_MEDIA_TYPE_ENVELOPE,
    BPP_MEDIA_TYPE_ENVELOPE_PLAIN,
    BPP_MEDIA_TYPE_ENVELOPE_WINDOW,
    BPP_MEDIA_TYPE_CONTINUOUS,
    BPP_MEDIA_TYPE_CONTINUOUS_LONG,
    BPP_MEDIA_TYPE_CONTINUOUS_SHORT,
    BPP_MEDIA_TYPE_TAB_STOCK,
    BPP_MEDIA_TYPE_PRE_CUT_TABS,
    BPP_MEDIA_TYPE_FULL_CUT_TABS,
    BPP_MEDIA_TYPE_MULTI_PART_FORM,
    BPP_MEDIA_TYPE_LABELS,
    BPP_MEDIA_TYPE_MULTI_LAYER,
    BPP_MEDIA_TYPE_SCREEN,
    BPP_MEDIA_TYPE_SCREEN_PAGED,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC_GLOSSY,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC_HIGH_GLOSS,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC_SEMI_GLOSS,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC_SATIN,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC_MATTE,
    BPP_MEDIA_TYPE_PHOTOGRAPHIC_FILM,
    BPP_MEDIA_TYPE_BACK_PRINT_FILM,
    BPP_MEDIA_TYPE_CARDSTOCK,
    BPP_MEDIA_TYPE_ROLL,
    
    BPP_MEDIA_TYPE_MAX_ENUM
    
} bt_bpp_media_type_enum;

/* printer media size */
typedef enum
{
    BPP_MEDIA_SIZE_A10,
    BPP_MEDIA_SIZE_A9,
    BPP_MEDIA_SIZE_A8,
    BPP_MEDIA_SIZE_A7,
    BPP_MEDIA_SIZE_A6,
    BPP_MEDIA_SIZE_A5,
    BPP_MEDIA_SIZE_A5_EXTRA,
    BPP_MEDIA_SIZE_A4,
    BPP_MEDIA_SIZE_A4_TAB,
    BPP_MEDIA_SIZE_A4_EXTRA,
    BPP_MEDIA_SIZE_A3,
    BPP_MEDIA_SIZE_A2,
    BPP_MEDIA_SIZE_A1,
    BPP_MEDIA_SIZE_A0,
    BPP_MEDIA_SIZE_2A0,
    BPP_MEDIA_SIZE_B10,
    BPP_MEDIA_SIZE_B9,
    BPP_MEDIA_SIZE_B8,
    BPP_MEDIA_SIZE_B7,
    BPP_MEDIA_SIZE_B6,
    BPP_MEDIA_SIZE_B6_C4,
    BPP_MEDIA_SIZE_B5,
    BPP_MEDIA_SIZE_B5_EXTRA,
    BPP_MEDIA_SIZE_B4,
    BPP_MEDIA_SIZE_B3,
    BPP_MEDIA_SIZE_B2,
    BPP_MEDIA_SIZE_B1,
    BPP_MEDIA_SIZE_B0,
    BPP_MEDIA_SIZE_C10,
    BPP_MEDIA_SIZE_C9,
    BPP_MEDIA_SIZE_C8,
    BPP_MEDIA_SIZE_C7,
    BPP_MEDIA_SIZE_C7_C6,
    BPP_MEDIA_SIZE_C6,
    BPP_MEDIA_SIZE_C6_C5,
    BPP_MEDIA_SIZE_C5,
    BPP_MEDIA_SIZE_C4,
    BPP_MEDIA_SIZE_C3,
    BPP_MEDIA_SIZE_C2,
    BPP_MEDIA_SIZE_C1,
    BPP_MEDIA_SIZE_C0,
    BPP_MEDIA_SIZE_4X6_POSTCARD,
    BPP_MEDIA_SIZE_LETTER,
    
    BPP_MEDIA_SIZE_MAX_ENUM
    
} bt_bpp_media_size_enum;



/* printer state */
typedef enum
{
    BPP_PRINTER_STATE_UNKNOWN = 0,
    BPP_PRINTER_STATE_IDLE,
    BPP_PRINTER_STATE_PROCESSING,
    BPP_PRINTER_STATE_STOPPED
    
} bt_bpp_printer_state_enum;

/* printer state reason */
typedef enum
{
    BPP_STRN_NONE = 0, //"none",
    BPP_STRN_ATTENTION_REQUIRED,//"attention-required",
    BPP_STRN_MEDIA_JAM,	//"media-jam",
    BPP_STRN_PAUSED,		//"paused",
    BPP_STRN_DOOR_OPEN,		//"door-open",
    BPP_STRN_MEDIA_LOW,		//"media-low",
    BPP_STRN_MEDIA_EMPTY,	//"media-empty",
    BPP_STRN_OUTPUT_AREA_ALMOST_FULL,//"output-area-almost-full",
    BPP_STRN_OUTPUT_AREA_FULL,	//"output-area-full",
    BPP_STRN_MARKER_SUPPLY_LOW,	//"marker-supply-low",
    BPP_STRN_MARKER_SUPPLY_EMPTY,//"marker-supply-empty",
    BPP_STRN_MARKER_FAILURE		//"marker-failure",
    
}bt_bpp_printer_state_reason;

/* printing-job state */
typedef enum
{
    BPP_JOB_STATE_UNKNOWN = 0,
    BPP_JOB_STATE_PRINTING,
    BPP_JOB_STATE_WAITING,
    BPP_JOB_STATE_STOPPED,
    BPP_JOB_STATE_COMPLETED,
    BPP_JOB_STATE_ABORTED,
    BPP_JOB_STATE_CANCELED
} bt_bpp_job_state_enum;


/* OBEX authentication challenge info */

#define BPP_AUTH_OPTION_FLAG_USERID_REQ  0x01
#define BPP_AUTH_OPTION_FLAG_READ_ONLY   0x02
#define BPP_MAX_REALM_LEN  20

typedef struct
{
	U8      options;

    U8          realm[BPP_MAX_REALM_LEN];
    U8          realm_len;
	
}bt_bpp_obex_auth_chal_info;


/* OBEX authentication response */
typedef struct
{
	BT_BOOL	cancel;
	char	userid[BTMTK_OBEX_AUTH_USERID_LEN + 1];
	U8		userid_len;
	char	passwd[BTMTK_OBEX_AUTH_PASSWD_LEN + 1];
	U8		passwd_len;
	
}bt_bpp_obex_auth_resp;

/* BPP print object with attribute: */
typedef struct
{
	U16						copies;
	bt_bpp_numberup_enum	numberup;
	bt_bpp_sided_enum		sides;
	bt_bpp_orient_enum		orient;
	bt_bpp_quality_enum		quality;
	bt_bpp_media_size_enum	media_size;
	//char                    media_size[BPP_MAX_MEDIA_SIZE_LEN];
	bt_bpp_media_type_enum 	media_type;
	
} bt_bpp_job_configuration;


/* object to be printed */
typedef struct
{
	//U16			dir_name[BTMTK_MAX_FILE_NAME_LEN];
	U16			file_name[BTMTK_MAX_FILE_NAME_LEN];
	U32			object_size;
	
	bt_bpp_mime_type 	mime_type;
	
	bt_bpp_print_model 	print_model;

	/* for job-base model: JobName & OriginatingUserName */
    char job_name[BPP_MAX_JOB_NAME_LEN];
    char originating_user_name[BPP_MAX_USER_NAME_LEN];

	/* printer configuration */	
	bt_bpp_job_configuration	job_config;
	
} bt_bpp_object;


/* printer capabilites */
typedef struct
{
	U8 						printer_name[BTMTK_BPP_MAX_PRINTER_NAME_LEN];
	U8 						printer_location[BTMTK_BPP_MAX_PRINTER_LOCT_LEN];
	
	BT_BOOL 	color_supported;
    U32 		max_copies_supported;
    U32			max_numberup_supported;

	U8			sides_supported; 		/* bitmask of BPP_SIDES_xx */
	U8			orientations_supported; /* bitmask of BPP_ORIENT_xx */
	U8			print_quality_supported; /* bitmask of BPP_QUALITY_xx */

	/* DocumentFormatsSupported */
	U8 			docfmt_num;
    bt_bpp_mime_type	doc_format_supported[BPP_MAX_NESTED_ATTR_NUM];

	/* ImageFormatsSupported */
	U8			imgfmt_num;
    bt_bpp_mime_type	image_format_supported[BPP_MAX_IMGFMT_SUPPORTED_NUM];

	/* MediaSizeSupported */
	U8			mediasize_num;
    bt_bpp_media_size_enum media_size_supported[BPP_MAX_NESTED_ATTR_NUM];
    //char        media_size_supported[BPP_MAX_NESTED_ATTR_NUM][BPP_MAX_MEDIA_SIZE_LEN];

	/* MediaTypeSupported */
	U8			mediatype_num;
    bt_bpp_media_type_enum media_type_supported[BPP_MAX_NESTED_ATTR_NUM];

	/* Loaded Media Type */
	U8			loaded_mediatype_num;
    bt_bpp_media_type_enum loaded_media_type[BPP_MAX_LOADED_MEDIA_NUM];


	U16			queued_job_count;

    U32 					basic_text_page_width;
    U32 					basic_text_page_height;

	U8			printer_general_current_operator[BPP_MAX_USER_NAME_LEN];     
	
} bt_bpp_printer_capability;

/* printer attributes */
typedef struct
{
    bt_bpp_printer_state_enum 	printer_state;
    bt_bpp_printer_state_reason state_reason;
	
    bt_bpp_printer_capability 	capability;
	
	U32							operation_status;
	
} bt_bpp_printer_attributes;

/* job status */
typedef struct
{
    bt_bpp_printer_state_enum 	printer_state;
    bt_bpp_job_state_enum 		job_state;
    bt_bpp_printer_state_reason	state_reason;
	
} bt_bpp_job_status;

/* progress status */
typedef struct
{
	U32 sent_data_len; 
	U32 total_data_len;
	
}bt_bpp_progress_status;



typedef struct 
{
	bt_bpp_mime_type type_id;
	char*             type_str;
	
}bpp_mime_id_str_struct;

extern bpp_mime_id_str_struct bpp_mime_type_table[BPP_MIME_TYPE_MAX_ENUM + 1];


#endif /* __BLUETOOTH_BPP_COMMON_H__ */

