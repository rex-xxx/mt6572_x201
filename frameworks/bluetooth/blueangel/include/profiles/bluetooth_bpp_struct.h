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
 * bluetooth_bpp_struct.h
 *
 * Project:
 * --------
 *   Maui
 *
 * Description:
 * ------------
 *   struct of local parameter for BPP sap
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
#ifndef __BLUETOOTH_BPP_STRUCT_H__
#define __BLUETOOTH_BPP_STRUCT_H__

//#include "bt_message.h"

#include "bluetooth_bpp_common.h"

#ifdef __ON_MAUI__
#define I32 int
#endif

/* commen cnf */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;
    I32 cnf_code;
} bt_bpp_common_cnf_struct;

/* commen rsp */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;
} bt_bpp_common_rsp_struct;

typedef struct _BPP_BD_ADDR
{
    U8 addr[6];
} BPP_BD_ADDR;


/* connect req */
typedef struct
{
    LOCAL_PARA_HDR 
		
	/* target device address */
    BPP_BD_ADDR addr;

	/* printer handle to identify the session*/
    PRINTER_HANDLE hprinter; 
	
} bt_bpp_connect_req_struct;

/* connect cnf */
typedef struct
{
    LOCAL_PARA_HDR 

	/* printer handle to identify the session*/
    PRINTER_HANDLE hprinter;

	/* cm_conn_id: maybe MMI will use it */	
    U32 cm_conn_id;

	/* confirm code */
    I32 cnf_code;
	
} bt_bpp_connect_cnf_struct;

/* disconnect req */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;
} bt_bpp_disconnect_req_struct;

/* disconnect cnf */
typedef bt_bpp_common_cnf_struct bt_bpp_disconnect_cnf_struct;

/* disconnect ind */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;
} bt_bpp_disconnect_ind_struct;

/* get printer attributes req */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	/* attributes to be retrieve.
	 * see "bt_bpp_printer_attribute_bitmask" defination
	 */	
	U32 attr_bitmask;
	
} bt_bpp_get_printer_attr_req_struct;

/* get printer attributes cnf */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	/* cnf code */
    I32 cnf_code;

	/* printer attributes */
	bt_bpp_printer_attributes printer_attributes;
	
} bt_bpp_get_printer_attr_cnf_struct;

/* print doc req */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	/* bpp object to print */
	bt_bpp_object print_object;

} bt_bpp_print_doc_req_struct;

/* print doc cnf */
typedef bt_bpp_common_cnf_struct bt_bpp_print_doc_cnf_struct;

/* authentication ind */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	bt_bpp_obex_auth_chal_info chal_info;
	
} bt_bpp_auth_ind_struct;

/* authentication rsp */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	/* authentication response */
	bt_bpp_obex_auth_resp auth_resp;
	
} bt_bpp_auth_rsp_struct;

/* progress ind */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	/* progress status */
    bt_bpp_progress_status progress_status;

} bt_bpp_progress_ind_struct;

/* progress rsp */
typedef bt_bpp_common_rsp_struct bt_bpp_progress_rsp_struct;

/* job state ind */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

	/* job status */
	bt_bpp_job_status job_status;
		
} bt_bpp_job_status_ind_struct;


/* cancel(GetAttr, printing) req */
typedef struct
{
    LOCAL_PARA_HDR 
    PRINTER_HANDLE hprinter;

} bt_bpp_cancel_req_struct;

/* print doc cnf */
typedef bt_bpp_common_cnf_struct bt_bpp_cancel_cnf_struct;


#endif /* __BLUETOOTH_BPP_STRUCT_H__ */

