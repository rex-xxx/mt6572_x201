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
 * bt_hfg_struct.h
 *
 * Project:
 * --------
 *   Maui
 *
 * Description:
 * ------------
 *   struct of local parameter for hfg adp sap
 *
 * Author:
 * -------
 * Elvis Lin
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: #2 $
 * $Modtime: $
 * $Log: $
 *
 * 09 22 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integrate bluetooth code from //ALPS_SW_PERSONAL/sh.lai/10YW1040OF_CB/ into //ALPS_SW/TRUNK/ALPS/.
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BLUETOOTH_HFG_STRUCT_H__
#define __BLUETOOTH_HFG_STRUCT_H__

#include "bt_message.h"
#include "bluetooth_hfg_common.h"
#if defined(BTMTK_ON_LINUX) && !defined(GEN_FOR_PC)
#include <sys/socket.h>
#include <sys/un.h>
#endif

#if defined(GEN_FOR_PC)
#include "tst_codegen.h"
#endif
/***************************************************************************** 
* Definations
*****************************************************************************/
#define HFG_CONNECT_GUARD_TIMER         20000 /* 20 sec */
#define HFG_SCO_CONNECT_GUARD_TIMER     5000000  /* 5 sec */

#define MAX_PHONE_NUMBER			64 /* does not include '\0' char */
#define MAX_PB_NUMBER_LEN			32 /* does not include '\0' char */
#define MAX_PB_TEXT                        	64 /* does not include '\0' char */
#define MAX_OPER_NAME                               36 /* does not include '\0' char */
#define MAX_FIND_TEXT                                 36 /* does not include '\0' char */
#define MAX_CGM_LEN				40 /* does not include '\0' char */
/* SMS */
#define MAX_LIST_STRING                        	64	/* does not include '\0' char */
#define MAX_ADDR_LEN	                        	32 	/* does not include '\0' char */
#define MAX_CHARSET_LEN				8 	/* does not include '\0' char */
#define MAX_SMS_TEXT_LEN				120	/* does not include '\0' char */

/* AT settings */
#define MAX_AT_STRING_LEN                   256
/***************************************************************************** 
* Typedef 
*****************************************************************************/
#define MTK_BD_ADDR_SIZE    6

typedef struct _MTK_BD_ADDR
{
    U8 addr[MTK_BD_ADDR_SIZE];
} MTK_BD_ADDR;
/***************************************************************************** 
* Structure
*****************************************************************************/
typedef enum
{
    hfg_status_nrec,
    hfg_status_inbandring,
    hfg_status_callidnotify,
    hfg_status_vr,
    hfg_status_callwaiting,
    num_of_hfg_status,
}hfg_status_type;

typedef union
{
    kal_bool nrec;
    kal_bool inbandring;
    kal_bool callidnotify;
    kal_bool vr;
    kal_bool callwaiting;
}hfg_status;

/* For A2DP use to avoid from compile error */
typedef enum
{
	hsg_idle, /* hsg_deactivated */
	hsg_activated,
	hsg_connected,
	hsg_sco_connected,

	hsg_activating,
	hsg_deactivating,
	hsg_connecting,
	hsg_disconnecting,
	hsg_sco_connecting,
	hsg_sco_disconnecting,
	hsg_authorizing
		
} bt_hsg_state;

typedef enum
{
	hfg_idle, /* hfg_deactivated */
	hfg_activated,
	hfg_connected,
	hfg_sco_connected,

	hfg_activating,
	hfg_deactivating,
	hfg_connecting,
	hfg_disconnecting,
	hfg_sco_connecting,
	hfg_sco_disconnecting,
	hfg_authorizing		
		
} bt_hfg_state;

typedef enum
{
	hfg_connect_accept_cnf_result_accepted,
	hfg_connect_accept_cnf_result_rejected,
	hfg_connect_accept_cnf_total
	
} bt_hfg_connect_accept_confirm_enum;


typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
}bt_hfg_header_struct;

typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U16                   result;
}bt_hfg_general_cnf_struct;

/* MSG_ID_BT_HFG_ACTIVATE_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                pContext;        // not used, just for sync with other structures
    void*                req_context;
    kal_bool           bHeadset;
#if defined(BTMTK_ON_LINUX)    
/*
    struct sockaddr_un     addr;
    socklen_t          addrlen;
*/
#endif
} bt_hfg_activate_req_struct;

/* MSG_ID_BT_HFG_ACTIVATE_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_activate_cnf_struct;

/* MSG_ID_BT_HFG_DEACTIVATE_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_hfg_deactivate_req_struct;

/* MSG_ID_BT_HFG_DEACTIVATE_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_deactivate_cnf_struct;

/* MSG_ID_BT_HFG_CONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    MTK_BD_ADDR           bt_addr;
} bt_hfg_connect_req_struct;

/* MSG_ID_BT_HFG_CONNECT_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_connect_cnf_struct;

/* MSG_ID_BT_HFG_CONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    MTK_BD_ADDR         bt_addr;
} bt_hfg_connected_ind_struct;

/* MSG_ID_BT_HFG_CONNECT_REQ_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    MTK_BD_ADDR         bt_addr;
} bt_hfg_connect_req_ind_struct;

/* MSG_ID_BT_HFG_RFCOMM_CONNECTED_IND*/
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    MTK_BD_ADDR         bt_addr;
} bt_hfg_rfcomm_connected_ind_struct;

#if 1
/* MSG_ID_BT_HFG_ACCEPT_CHANNEL_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               req_context;
} bt_hfg_accept_channel_req_struct;

/* MSG_ID_BT_HFG_ACCEPT_CHANNEL_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_accept_channel_cnf_struct;

/* MSG_ID_BT_HFG_REJECT_CHANNEL_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_hfg_reject_channel_req_struct;

/* MSG_ID_BT_HFG_REJECT_CHANNEL_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_reject_channel_cnf_struct;
#else
/* MSG_ID_BT_HFG_CONNECT_REQ_RES */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    bt_hfg_connect_accept_confirm_enum result;
    MTK_BD_ADDR         bt_addr;
    U32        connection_id;
} bt_hfg_connect_req_res_struct;
#endif

/* MSG_ID_BT_HFG_DISCONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    //MTK_BD_ADDR           bt_addr;
} bt_hfg_disconnect_req_struct;

/* MSG_ID_BT_HFG_DISCONNECT_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_disconnect_cnf_struct;

/* MSG_ID_BT_HFG_DISCONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    MTK_BD_ADDR         bt_addr;
} bt_hfg_disconnected_ind_struct;

/* MSG_ID_BT_HFG_SCO_CONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_hfg_sco_connect_req_struct;

/* MSG_ID_BT_HFG_SCO_CONNECT_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_sco_connect_cnf_struct;

/* MSG_ID_BT_HFG_SCO_CONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
    U16		      status;
} bt_hfg_sco_connected_ind_struct;

/* MSG_ID_BT_HFG_SCO_DISCONNECT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
} bt_hfg_sco_disconnect_req_struct;

/* MSG_ID_BT_HFG_SCO_DISCONNECT_CNF */
typedef bt_hfg_general_cnf_struct bt_hfg_sco_disconnect_cnf_struct;

/* MSG_ID_BT_HFG_SCO_DISCONNECTED_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*               user_context;
} bt_hfg_sco_disconnected_ind_struct;

/* MSG_ID_BT_HFG_GET_STATUS_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    hfg_status_type  type;
}bt_hfg_get_status_req_struct;

/* MSG_ID_BT_HFG_GET_STATUS_CNF */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U16                   result;    
    hfg_status          status;
} bt_hfg_get_status_cnf_struct;

typedef enum
{
	BT_HFG_RESULT_OK = 0,
	BT_HFG_RESULT_FAIL,
	BT_HFG_RESULT_CHIP_REASON,
	BT_HFG_RESULT_TIMEOUT,
	BT_HFG_RESULT_MED_ERROR /* Error cause by MED state error, not BT */
} BT_HFG_RESULT_ENUM;


/********************************** 
 * AT CMD request / confirm
 **********************************/
 /* MSG_ID_BT_HFG_SEND_RESULT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 pAtContext;   /* Specific context for the result. it will returned when result is sent. */
    //AtCommand        type;              /* Result type like OK or ERROR */
	U16					  type;			/* Result type like OK or ERROR */
    kal_bool                 done;
    U16                   parmLen;      /* length of parameter data */
    U32                   parms;
    U32                   data_len;       /* length of the data (Use U32 to force the data is 4bytes aligned) */
    U8                    data[1];          /* extra data that is pointed by internal pointer in parms */
} bt_hfg_send_result_req_struct;

#if 0
/* MSG_ID_BT_HFG_AT_RESULT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 pAtContext;   /* Specific context for the result. it will returned when result is sent. */
    //AtCommand        type;              /* Result type like OK or ERROR */
	U16					  type;			/* Result type like OK or ERROR */
    U32                   data_len;       /* length of the data (Use U32 to force the data is 4bytes aligned) */
    U8                    data[1];          /* parameter data */
} bt_hfg_at_result_req_struct;
#else

typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;   /* Specific context for the result. it will returned when result is sent. */
} bt_hfg_common_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                   data;
} bt_hfg_common_int_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;   /* Specific context for the result. it will returned when result is sent. */
    kal_bool                 enable;
} bt_hfg_common_bool_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U16                      result;           /* result of sending AT CMD */
} bt_hfg_req_complete_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 user_context;
} bt_hfg_common_ind_struct;

/* OK */
/* MSG_ID_BT_HFG_OK_REQ */
typedef bt_hfg_common_req_struct bt_hfg_ok_req_struct;
/* MSG_ID_BT_HFG_OK_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_ok_cnf_struct;

/* ERROR or +CME ERROR:<err> */
/* MSG_ID_BT_HFG_ERROR_REQ */
typedef bt_hfg_common_int_req_struct bt_hfg_error_req_struct;
/* MSG_ID_BT_HFG_ERROR_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_error_cnf_struct;

/* NO CARRIER */
/* MSG_ID_BT_HFG_NO_CARRIER_REQ */
typedef bt_hfg_common_req_struct bt_hfg_no_carrier_req_struct;
/* MSG_ID_BT_HFG_NO_CARRIER_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_no_carrier_cnf_struct;

/* BUSY */
/* MSG_ID_BT_HFG_BUSY_REQ */
typedef bt_hfg_common_req_struct bt_hfg_busy_req_struct;
/* MSG_ID_BT_HFG_BUSY_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_busy_cnf_struct;

/* NO ANSWER */
/* MSG_ID_BT_HFG_NO_ANSWER_REQ */
typedef bt_hfg_common_req_struct bt_hfg_no_answer_req_struct;
/* MSG_ID_BT_HFG_NO_ANSWER_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_no_answer_cnf_struct;

/* DELAYED */
/* MSG_ID_BT_HFG_DELAYED_REQ */
typedef bt_hfg_common_req_struct bt_hfg_delayed_req_struct;
/* MSG_ID_BT_HFG_DELAYED_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_delayed_cnf_struct;

/* BLACKLISTED */
/* MSG_ID_BT_HFG_BLACKLISTED_REQ */
typedef bt_hfg_common_req_struct bt_hfg_blacklisted_req_struct;
/* MSG_ID_BT_HFG_BLACKLISTED_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_blacklisted_cnf_struct;

/* RING */
/* MSG_ID_BT_HFG_RING_REQ */
typedef bt_hfg_common_req_struct bt_hfg_ring_req_struct;
/* MSG_ID_BT_HFG_RING_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_ring_cnf_struct;

/* +CCWA:<number>,<type>,<class> */
/* MSG_ID_BT_HFG_CALL_WAITING_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* waiting call info */
    U16                       numsize;
    U8                        number[MAX_PHONE_NUMBER+1];    /* Phone number of the waiting call */
    U8                        classmap;    /* Voice parameters */
    U8                        type;           /* Type of address */
    /* End of waiting call info */
    //HfgCallWaitParms   wait;               /* waiting call */
} bt_hfg_call_waiting_req_struct;
/* MSG_ID_BT_HFG_CALL_WAITING_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_call_waiting_cnf_struct;

/* +CIND=[<ind>[,<ind>[,...]]] */
/* MSG_ID_BT_HFG_READ_INDICATOR_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           req_context;   /* Specific context for the result. it will returned when result is sent. */
    HfgReadIndicatorParms   indicators;     /* list of indicator values */
} bt_hfg_read_indicator_req_struct;
/* MSG_ID_BT_HFG_READ_INDICATOR_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_read_indicator_cnf_struct;

/* +CLCC: <id1>,<dir>,<stat>,<mode>,<mpty>[,<number>,<type> */
/* MSG_ID_BT_HFG_CALL_LIST_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    //HfgCallListParms    call;
    /* Call list infon */
    U8                  index;          /* Index of the call on the audio gateway (1 based) */
    U8                  dir;              /* 0 - Mobile Originated, 1 = Mobile Terminated */
    HfgCallStatus   state;          /* Call state (see HfgCallStatus) */
    HfgCallMode     mode;         /* Call mode (see HfgCallMode) */
    U8                  multiParty;   /* 0 - Not Multiparty, 1 - Multiparty */
    U16                numsize;     /* size of phone number (does not inc. '\0') */   
    U8                  number[MAX_PHONE_NUMBER+1];    /* Phone number of the call */
    U8                  type;           /* Type of address */
    /* End of call info */
    kal_bool                    bFinal;
} bt_hfg_call_list_req_struct;
/* MSG_ID_BT_HFG_CALL_LIST_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_call_list_cnf_struct;

/* +CCWA:<number>,<type>,<class> */
/* MSG_ID_BT_HFG_CALL_ID_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                      data_size;       /* size of the callid */
    //HfgCallerIdParms    callid;               /* call id */
    U16             numsize;        /* size of phone number (does not inc. '\0') */
    U8               number[MAX_PHONE_NUMBER+1];      /* Phone number of the caller */
    U8               type;            /* Type of address */
} bt_hfg_call_id_req_struct;
/* MSG_ID_BT_HFG_CALL_ID_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_call_id_cnf_struct;

/* +CIEV: <ind>,<value> */
/* MSG_ID_BT_HFG_INDICATOR_REPORT_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    HfgIndicator          indicator;        /* indicator id (not index) */
    U8                       value;             /* indicator value */
} bt_hfg_indicator_report_req_struct;
/* MSG_ID_BT_HFG_INDICATO_REPORT_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_indicator_report_cnf_struct;

/* +CNUM: [<alpha>],<number>, <type>,[<speed>] ,<service> */
/* MSG_ID_BT_HFG_SUBSCRIBER_NUMBER_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;      /* Specific context for the result. it will returned when result is sent. */
    /*HfgSubscriberNum   subscriber;*/          /* subscriber number */
    /* End of subscriber info */
    U16                      numsize;
    U8                        number[MAX_PHONE_NUMBER+1];    /* String phone number of format specified by "type". */
    HfgNumberFormat   type;             /* Phone number format */
    U8                        service;          /* Service related to the phone number. */
    /* End of subscriber info */
    kal_bool                    bFinal;
} bt_hfg_subscriber_number_req_struct;
/* MSG_ID_BT_HFG_SUBSCRIBER_NUMBER_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_subscriber_number_cnf_struct;

/* +COPS: <mode>[,<format>,<oper>] */
/* MSG_ID_BT_HFG_OPERATOR_NAME_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;      /* Specific context for the result. it will returned when result is sent. */
    /* operator name info */
    U8                        mode;    /* 0 = automatic, 1 = manual, 2 = deregister, 3 = set format only, 
                                               * 4 = manual/automatic.
                                               */
    HfgOperFormat	     format;    /* Format of "oper" parameter (should be set to 0) */
    U16                      opersize;
    U8                        oper[MAX_OPER_NAME+1];    /* Numeric or long or short alphanumeric name of operator */
    /* End of operator name info */
} bt_hfg_operator_name_req_struct;
/* MSG_ID_BT_HFG_OPERATOR_NAME_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_operator_name_cnf_struct;

/* +BINP:<number> */
/* MSG_ID_BT_HFG_VOICE_TAG_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                      data_size;       /* size of the phone number inc. '\0' */
    char                      number[MAX_PHONE_NUMBER+1];    /* phone number */
} bt_hfg_voice_tag_req_struct;
/* MSG_ID_BT_HFG_VOICE_TAG_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_voice_tag_cnf_struct;

/* +BVRA: <vrect> */
/* MSG_ID_BT_HFG_VOICE_REC_REQ */
typedef bt_hfg_common_bool_req_struct bt_hfg_voice_rec_req_struct;
/* MSG_ID_BT_HFG_VOICE_REC_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_voice_rec_cnf_struct;

/* +BTRH: <n> */
/* MSG_ID_BT_HFG_RESPONSE_HOLD_REQ */
typedef bt_hfg_common_int_req_struct bt_hfg_response_hold_req_struct;
/* MSG_ID_BT_HFG_RESPONSE_HOLD_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_response_hold_cnf_struct;

/* +VGS: <gain> */
/* MSG_ID_BT_HFG_SPEAKER_GAIN_REQ */
typedef bt_hfg_common_int_req_struct bt_hfg_speaker_gain_req_struct;
/* MSG_ID_BT_HFG_SPEAKER_GAIN_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_speaker_gain_cnf_struct;

/* +VGM: <gain> */
/* MSG_ID_BT_HFG_MIC_GAIN_REQ */
typedef bt_hfg_common_int_req_struct bt_hfg_mic_gain_req_struct;
/* MSG_ID_BT_HFG_MIC_GAIN_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_mic_gain_cnf_struct;

/* AT+VGS: <gain> */
/* MSG_ID_BT_HFG_SPEAKER_GAIN_IND */
typedef bt_hfg_common_int_req_struct bt_hfg_speaker_gain_ind_struct;

/* AT+VGM: <gain> */
/* MSG_ID_BT_HFG_MIC_GAIN_IND */
typedef bt_hfg_common_int_req_struct bt_hfg_mic_gain_ind_struct;

/* +BSIR:<enable> */
/* MSG_ID_BT_HFG_RINGTONE_STATUS_REQ */
typedef bt_hfg_common_bool_req_struct bt_hfg_ringtone_status_req_struct;
/* MSG_ID_BT_HFG_RINGTONE_STATUS_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_ringtone_status_cnf_struct;

/* MSG_ID_BT_HFG_SUPPORTED_CHARSET_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                      data_size;       /* size of the phone number inc. '\0' */
    char                      supported_charset[MAX_LIST_STRING+1];    /* phone number */
} bt_hfg_supported_charset_req_struct;
/* MSG_ID_BT_HFG_SUPPORTED_CHARSET_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_supported_charset_cnf_struct;

/* MSG_ID_BT_HFG_SELECTED_CHARSET_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                      data_size;       /* size of the phone number inc. '\0' */
    char                      selected_charset[MAX_CHARSET_LEN+1];    /* phone number */
} bt_hfg_selected_charset_req_struct;
/* MSG_ID_BT_HFG_SELECTED_CHARSET_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_selected_charset_cnf_struct;

/* MSG_ID_BT_HFG_MODEL_ID_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                      data_size;       /* size of the phone number inc. '\0' */
    char                      modelId[MAX_CGM_LEN+1];    /* phone number */
} bt_hfg_model_id_req_struct;
/* MSG_ID_BT_HFG_MODEL_ID_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_model_id_cnf_struct;

/* MSG_ID_BT_HFG_MANUFACTURE_ID_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    U32                      data_size;       /* size of the phone number inc. '\0' */
    char                      manufactureId[MAX_CGM_LEN+1];    /* phone number */
} bt_hfg_manufacture_id_req_struct;
/* MSG_ID_BT_HFG_MANUFACTURE_ID_CNF */
typedef bt_hfg_req_complete_cnf_struct bt_hfg_manufacture_id_cnf_struct;

/* MSG_ID_BT_HFG_SEND_DATA_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    kal_bool                    bDone;           /* if TRUE, HFG will send OK automatically  */
    U32                      data_size;       /* size of AT CMD string(inc. null terminator) */
    char                      atresult[MAX_AT_STRING_LEN];           /* AT result string */
} bt_hfg_send_data_req_struct;
typedef bt_hfg_common_int_req_struct bt_hfg_send_data_ind_struct;

/***************************************
*   Phonebook related
***************************************/
/* AT+CPBS=? */
/* MSG_ID_BT_HFG_SUPPORTED_PHONEBOOK_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbStorageSupported */
    U16                      supported;
} bt_hfg_supported_phonebook_req_struct;

/* AT+CPBS? */
/* MSG_ID_BT_HFG_SELECTED_PHONEBOOK_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbStorageSelected */
    /* Phonebook storage currently selected. */
    U16                      selected;
    /* Number of used locations. */
    U16                      used;
    /* Total number of locations in the memory. */
    U16                      total;
} bt_hfg_selected_phonebook_req_struct;

/* AT+CPBR=? */
/* MSG_ID_BT_HFG_READ_PHONEBOOK_INFO_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*               pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbReadInfo */
    /* Location range supported by the current storage (e.g. "0-99"). 
     * Quotes must not be included in this string.
     */
    U16                      rangesize;
    char                      range[36];
    /* The maximum length of the phonebook "number" field. */
    U16                      numLength;
    /* The maximum length of the phonebook "text" field. */
    U16                      textLength;
} bt_hfg_read_phonebook_info_req_struct;

/* AT+CPBR=<index1>[,<index2>] */
/* MSG_ID_BT_HFG_READ_PHONEBOOK_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbEntry */
    /* Index of this entry. */
    U16                      index;
    /* Phone number format. */
    HfgNumberFormat  type;
    /* Phone number. */
    char                     number[MAX_PHONE_NUMBER+1];
    /* Text associated with phone number. The character set used with
     * this parameter is specified by AT_SELECT_CHARACTER_SET command.
     */
    char                     text[MAX_PB_TEXT+1];
    kal_bool                   bFinal;
} bt_hfg_read_phonebook_req_struct;


/* AT+CPBF=? */
/* MSG_ID_BT_HFG_FIND_PHONEBOOK_INFO_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbFindInfo */
    /* The maximum length of the phonebook "number" field. */
    U16                      numLength;
    /* The maximum length of the phonebook "text" field. */
    U16                      textLength;
} bt_hfg_find_phonebook_info_req_struct;

/* AT+CPBF=<findtext> */
/* MSG_ID_BT_HFG_FIND_PHONEBOOK_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbEntry */
    /* Index of this entry. */
    U16                      index;
    /* Phone number format. */
    HfgNumberFormat  type;
    /* Phone number. */
    char                     number[MAX_PHONE_NUMBER+1];
    /* Text associated with phone number. The character set used with
     * this parameter is specified by AT_SELECT_CHARACTER_SET command.
     */
    char                     text[MAX_PB_TEXT+1];
    kal_bool                   bFinal;
} bt_hfg_find_phonebook_req_struct;

/* AT+CPBW=? */
/* MSG_ID_BT_HFG_WRITE_PHONEBOOK_INFO_REQ */
typedef struct
{
    LOCAL_PARA_HDR
    void*                    pContext;
    void*                    req_context;   /* Specific context for the result. it will returned when result is sent. */
    /* HfgPbWriteInfo */
    /* Location range supported by the current storage (e.g. "0-99"). 
     * Quotes must not be included in this string.
     */
     U16                    rangesize;
    char                     range[MAX_LIST_STRING+1];
    /* The maximum length of the phonebook "number" field. */
    U16                     numLength;
    /* List of supported type by the current storage (e.g. "145,129")
     * Quotes must not be included in this string.
     */
     U16                    typesize;
     char                   types[MAX_LIST_STRING+1];
    /* The maximum length of the phonebook "text" field. */
    U16                 textLength;
} bt_hfg_write_phonebook_info_req_struct;

/********************************** 
 * AT CMD indication 
 **********************************/
 /* AT+BRSF */
/* MSG_ID_BT_HFG_HANDSFREE_FEATURES_IND */
/* HFG profile already response this AT CMD, the indication is just inform the 
    the supported features*/
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;    
    HfgHandsFreeFeatures    hfFeatures;
} bt_hfg_handsfree_features_ind_struct;

/* AT+CIND? */
/* MSG_ID_BT_HFG_QUERY_INDICATOR_VALUE_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_indicator_value_ind_struct;

/* MSG_ID_BT_HFG_ENABLE_EXTENDED_ERRORS_IND */
typedef bt_hfg_common_ind_struct bt_hfg_enable_extended_errors_ind_struct;

/* MSG_ID_BT_HFG_ANSWER_CALL_IND */
typedef bt_hfg_common_ind_struct bt_hfg_answer_call_ind_struct;

/* MSG_ID_BT_HFG_DIAL_NUMBER_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U16                             datasize;  /* size of number inc. '\0' */
    char                             number[MAX_PHONE_NUMBER+1];
} bt_hfg_dial_number_ind_struct;

/* MSG_ID_BT_HFG_MEMORY_DIAL_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U16                             index;
} bt_hfg_memory_dial_ind_struct;

/* MSG_ID_BT_HFG_REDIAL_IND */
typedef bt_hfg_common_ind_struct bt_hfg_redial_ind_struct;

/* AT+CHLD=xx */
/* MSG_ID_BT_HFG_CALL_HOLD_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    HfgHold                        hold;
    /*
    HfgHoldAction               action;
    U8                               index;
    */
} bt_hfg_call_hold_ind_struct;

/* AT+BTRH? */
/* MSG_ID_BT_HFG_QUERY_RESPONSE_HOLD_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
} bt_hfg_query_response_hold_ind_struct;

/* AT+BTRH=<n> */
/* MSG_ID_BT_HFG_SET_RESPONSE_HOLD_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    HfgResponseHold	    respHold;
} bt_hfg_set_response_hold_ind_struct;

/* MSG_ID_BT_HFG_HANGUP_IND */
typedef bt_hfg_common_ind_struct bt_hfg_hang_up_ind_struct;

/* MSG_ID_BT_HFG_GENERATE_DTMF_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U8                               dtmf;
} bt_hfg_generate_dtmf_ind_struct;

/* MSG_ID_BT_HFG_GET_LAST_VOICE_TAG_IND */
typedef bt_hfg_common_ind_struct bt_hfg_get_last_voice_tag_ind_struct;

/* MSG_ID_BT_HFG_LIST_CURRENT_CALLS_IND */
typedef bt_hfg_common_ind_struct bt_hfg_list_current_calls_ind_struct;

/* MSG_ID_BT_HFG_QUERY_SUBSCRIBER_NUMBER_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_subscriber_number_ind_struct;

/* MSG_ID_BT_HFG_QUERY_NETWORK_OPERATOR_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_network_operator_ind_struct;

/* MSG_ID_BT_HFG_REPORT_MIC_VOLUME_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U8                               vol;
} bt_hfg_report_mic_volume_ind_struct;

/* MSG_ID_BT_HFG_REPORT_SPK_VOLUME_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U8                               vol;
} bt_hfg_report_speaker_volume_ind_struct;

/* MSG_ID_BT_HFG_ENABLE_CALLER_ID_IND */
typedef bt_hfg_common_ind_struct bt_hfg_enable_caller_id_ind_struct;

/* MSG_ID_BT_HFG_ENABLE_CALL_WAITING_IND */
//typedef bt_hfg_common_ind_struct bt_hfg_enable_call_waiting_ind_struct;
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    kal_bool                           enable;
} bt_hfg_enable_call_waiting_ind_struct;


/* MSG_ID_BT_HFG_DISABLE_NREC_IND */
typedef bt_hfg_common_ind_struct bt_hfg_disable_nrec_ind_struct;

/* MSG_ID_BT_HFG_ENABLE_VOICE_RECOGNITION_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    kal_bool                           enable;
} bt_hfg_enable_voice_recognition_ind_struct;

/* MSG_ID_BT_HFG_KEYPAD_CONTROL_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U8                               button;
} bt_hfg_keypad_control_ind_struct;

/* MSG_ID_BT_HFG_QUERY_SUPPORTED_CHARSET_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_charset_ind_struct;

/* MSG_ID_BT_HFG_QUERY_SELECTED_CHARSET_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_charset_ind_struct;

/* MSG_ID_BT_HFG_SELECT_CHARSET_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    char		     			charset[MAX_CHARSET_LEN+1];
} bt_hfg_select_charset_ind_struct;

/* MSG_ID_BT_HFG_QUERY_MODEL_ID_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_model_id_ind_struct;

/* MSG_ID_BT_HFG_QUERY_MANUFACTURE_ID_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_manufacture_id_ind_struct;

/* MSG_ID_BT_HFG_AT_COMMAND_DATA_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U16                             datasize;   /* size of atcmd inc. '\0' */
    char                            atcmd[MAX_AT_STRING_LEN];
} bt_hfg_at_command_data_ind_struct;

/*******************************************************
*   Phonebook related message
*******************************************************/
/* AT+CPBS=? : AT_SELECT_PHONEBOOK_STORAGE */
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_PHONEBOOK_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_phonebook_ind_struct;

/* AT+CPBS=<storage> : AT_SELECT_PHONEBOOK_STORAGE */
/* MSG_ID_BT_HFG_SELECT_PHONEBOOK_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U16                             selected;
} bt_hfg_select_phonebook_ind_struct;

/* AT+CPBS? : AT_SELECT_PHONEBOOK_STORAGE */
/* MSG_ID_BT_HFG_QUERY_SELECTED_PHONEBOOK_IND */
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_phonebook_ind_struct;

/* AT+CPBR=? : AT_READ_PHONEBOOK_ENTRY|AT_TEST */
/* MSG_ID_BT_HFG_READ_PHONEBOOK_INFO_IND */
typedef bt_hfg_common_ind_struct bt_hfg_read_phonebook_info_ind_struct;

/* AT+CPBR=<index1>[,<index2>] : AT_READ_PHONEBOOK_ENTRY */
/* MSG_ID_BT_HFG_READ_PHONEBOOK_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    HfgPbRead                   read;
} bt_hfg_read_phonebook_ind_struct;

/* AT+CPBF=? : AT_FIND_PHONEBOOK_ENTRY|AT_TEST */
/* MSG_ID_BT_HFG_FIND_PHONEBOOK_INFO_IND */
typedef bt_hfg_common_ind_struct bt_hfg_find_phonebook_info_ind_struct;

/* AT+CPBF=<findtext> : AT_FIND_PHONEBOOK_ENTRY */
/* MSG_ID_BT_HFG_FIND_PHONEBOOK_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    char                             text[MAX_FIND_TEXT+1];
} bt_hfg_find_phonebook_ind_struct;

/* AT+CPBW=? : AT_WRITE_PHONEBOOK_ENTRY|AT_TEST */
/* MSG_ID_BT_HFG_WRITE_PHONEBOOK_INFO_IND */
typedef bt_hfg_common_ind_struct bt_hfg_write_phonebook_info_ind_struct;

/* AT+CPBW=[<index>][,<number>[,<type>[,<text>]]] : AT_WRITE_PHONEBOOK_ENTRY */
/* MSG_ID_BT_HFG_WRITE_PHONEBOOK_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
    U16                             index;  /* Index of this entry. */
    U8                               type;    /* Phone number format. */
    char                             number[MAX_PB_NUMBER_LEN+1];    /* Phone number. */
    /* Text associated with phone number. The character set used with
     * this parameter is specified by AT_SELECT_CHARACTER_SET command.
     */
    char                             text[MAX_PB_TEXT+1];
} bt_hfg_write_phonebook_ind_struct;

/*******************************************************
*   SMS related message
*******************************************************/
/************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_SMS_SERVICE_REQ 	*/
/* AT_SELECT_SMS_SERVICE|AT_TEST 				*/
/* +CSMS:(list of supported <service>s) 				*/
/************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	U8					supportedServiceLen;
	char					supportedService[MAX_LIST_STRING+1];
} bt_hfg_suppoted_sms_service_req_struct;

/************************************************/
/* MSG_ID_BT_HFG_SELECTED_SMS_SERVICE_REQ 		*/
/* AT_SELECT_SMS_SERVICE|AT_READ 				*/
/* +CSMS:<service>,<mt>,<mo>,<bm>				*/
/************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Selected service */
	U8					type;	/* service type */
	U8					mt;	/* Support for receiving (mobile terminated) messages. */
	U8					mo;	/* Support for sending (mobile originated) messages. */ 
	U8					bm;	/* Support for cell broadcast messages.*/
} bt_hfg_selected_sms_service_req_struct;

/************************************************/
/* MSG_ID_BT_HFG_SMS_SERVICE_REQ 				*/
/* AT_SELECT_SMS_SERVIC			 				*/
/* +CSMS:<mt>,<mo>,<bm>						*/
/************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Selected service */
	U8					mt;	/* Support for receiving (mobile terminated) messages. */
	U8					mo;	/* Support for sending (mobile originated) messages. */ 
	U8					bm;	/* Support for cell broadcast messages.*/
} bt_hfg_sms_service_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_PREF_MSG_STORAGE_REQ				*/
/* AT_PREFERRED_SMS_STORAGE|AT_TEST	 						*/
/* +CPMS:(list of supported <mem1>s),(..),(list of supported <mem3>s)	*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Supported storages */
	/* bitmask of HfgSMSStorage */
	HfgSMSStorage   		read;	/* Memory from which messages are read and deleted. */	
	HfgSMSStorage   		write;	/* Memory to which writing and sending operations are made. */
	HfgSMSStorage   		recv;	/* Memory to which received SMs are preferred to be stored. */
} bt_hfg_supported_pref_msg_storage_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SELECTED_PREF_MSG_STORAGE_REQ				*/
/* AT_PREFERRED_SMS_STORAGE|AT_READ	 						*/
/* +CPMS:<mem1>,<used1>,<total1>...,<mem3>,<used3>,<total3>		*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* selected storages */
	/* Memory from which messages are read and deleted. */
	HfgSMSStorage read;
	U16 			readUsed;
	U16 			readTotal;	
	/* Memory to which writing and sending operations are made. */
	HfgSMSStorage	write;
	U16 			writeUsed;
	U16 			writeTotal;
	/* Memory to which received SMs are preferred to be stored. */
	HfgSMSStorage	recv;
	U16 			recvUsed;
	U16 			recvTotal;
} bt_hfg_selected_pref_msg_storage_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_PREF_MSG_STORAGE_REQ							*/
/* AT_PREFERRED_SMS_STORAGE			 						*/
/* +CPMS:<used1>,<total1>...,<used3>,<total3>						*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* response to set storages cmd */
	U8			count;
	/* Memory from which messages are read and deleted. */
	U16 			readUsed;
	U16 			readTotal;	
	/* Memory to which writing and sending operations are made. */
	U16 			writeUsed;
	U16 			writeTotal;
	/* Memory to which received SMs are preferred to be stored. */
	U16 			recvUsed;
	U16 			recvTotal;
} bt_hfg_pref_msg_storage_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_MSG_FORMAT_REQ						*/
/* AT_SMS_MESSAGE_FORMAT|AT_TEST		 						*/
/* +CMGF:(list of <mode>s)											*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* supported format */
	U16					supportedFormatLen;
	char					supportedFormat[MAX_LIST_STRING+1];
} bt_hfg_supported_msg_format_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SELECTED_MSG_FORMAT_REQ						*/
/* AT_SMS_MESSAGE_FORMAT|AT_READ		 						*/
/* +CMGF:<mode>												*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* selected format : 0: PDU, 1:TEXT */
	U8					mode;
} bt_hfg_selected_msg_format_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SERVICE_CENTRE_REQ							*/
/* AT_SMS_SERVICE_CENTER|AT_READ		 						*/
/* +CSCA:<sca>[,<tosca>] 											*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Service centre address */
	U8					addrLen;
	char 				addr[MAX_ADDR_LEN+1];
	U8			 		addrType;
} bt_hfg_service_centre_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_TEXT_MODE_PARAMS_REQ							*/
/* AT_SET_TEXT_MODE_PARMS|AT_READ		 						*/
/* +CSMP:<fo>,<vp>,<pid>,<dcs>									*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Text mode parameters */
	U8					fo;
	U8					intVp;			/* valid if strVp is NULL */
	char					strVp[21];
	U8					pid;
	U8					dcs;	
} bt_hfg_text_mode_params_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_SHOW_PARAMS_REQ					*/
/* AT_SMS_SHOW_TEXT_MODE|AT_TEST		 						*/
/* +CSDH:(list of supported <show>s)								*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* supported show text mode parameters */
	U16					supportedShowLen;
	char					supportedShow[MAX_LIST_STRING+1];
} bt_hfg_supported_show_text_mode_params_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SELECTED_SHOW_PARAMS_REQ						*/
/* AT_SMS_SHOW_TEXT_MODE|AT_READ		 						*/
/* +CSDH:<show>													*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* supported show text mode parameters */
	U8					show;	/* 0: do not show, 1: show */
} bt_hfg_selected_show_text_mode_params_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_NEW_MSG_INDICATION_REQ			*/
/* AT_NEW_MESSAGE_INDICATION|AT_TEST		 					*/
/* +CNMI:(list of supported <mode>s)...(list of supported <bfr>s)			*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	U16					supportedNewMsgIndLen;
	char					supportedNewMsgInd[MAX_LIST_STRING+1];
}bt_hfg_supported_new_msg_indication_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SELECTED_NEW_MSG_INDICATION_REQ				*/
/* AT_NEW_MESSAGE_INDICATION|AT_READ		 					*/
/* +CNMI:<mode>,<mt>,<bm>,<ds>,<bfr>							*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Selected new msg indication */
	U8 					mode;	/* 0~3 */
	U8 					mt;		/* 0~3 */
	U8 					bm;		/* 0~3 */
	U8 					ds;		/* 0~2 */
	U8 					bfr;		/* 0~1 */
}bt_hfg_selected_new_msg_indication_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_NEW_MSG_INDICATION_REQ						*/
/* AT_SMS_DELIVER_INDICATION				 					*/
/* +CMTI:<mem>,<index>											*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* new msg indication */
	HfgSMSStorage		mem;
	U16 					index;
}bt_hfg_new_msg_indication_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SUPPORTED_LIST_STATUS_REQ						*/
/* AT_LIST_MESSAGES|AT_TEST					 					*/
/* +CMGL:(list of supported <stat>s)									*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Supported SMS list status */	
	U16					supportedListStatusLen;
	char					supportedListStatus[MAX_LIST_STRING+1];
}bt_hfg_supported_list_status_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_LIST_MSG_REQ									*/
/* AT_LIST_MESSAGES					 							*/
/* TEXT MODE & SMS-SUBMITs and/or SMS-DELIVERs:					*/
/* +CMGL:<index>,<stat>,<oa/da>,[<alpha>],[<scts>]					*/
/*               [,<tooa/toda>,<length>]<CR><LF><data> 				*/
/* TEXT MODE & SMS-STATUS-REPORTs : 								*/
/* +CMGL:<index>,<stat>,<sn>,<mid>,<page>,<pages>				*/
/*            <CR><LF><data> 										*/
/* TEXT MODE & SMS-COMMANDs : 									*/
/* +CMGL:<index>,<stat>,<fo>,<ct><CR><LF><data> 				*/
/* TEXT MODE & CMB storage :										*/
/* +CMGL:<index>,<stat>,<sn>,<mid>,<page>,<pages>				*/
/*            <CR><LF><data> 										*/
/* PDU MODE : 													*/
/* +CMGL:<index>,<stat>,[alpha],<length><CR><LF><pdu> 			*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* List message */
	HfgSMSType			type;
	U16					index;
	HfgSMSStat			stat;
	union{
		struct{
			HfgSMSMask	mask;						// bitwise value for optional values (alpha, scts, tooa/toda, length)
			char 		addr[MAX_ADDR_LEN+1]; 		// originated address, destination address (oa/da)
			U8			addrType;					// type of address (tooa/toda)
			char			alpha[MAX_CHARSET_LEN+1]; 	// string type of address (optional, alpha, ex.UTF-8)
			char 		SCTimeStamp[21];			// time stamp "yy/MM/dd,hh:mm:ss+(-)zz" (optional, scts)
			char 		data[MAX_SMS_TEXT_LEN+1];	// data
			U16			length;						// length of data(optional)			
		}deliver;
		
		struct{
			HfgSMSMask	mask;						// bitwise value for optional values (alpha, scts, tooa/toda, length)
			char 		addr[MAX_ADDR_LEN+1];		// originated address, destination address (oa/da)
			U8			addrType;					// type of address (tooa/toda)
			char			alpha[MAX_CHARSET_LEN+1]; 	// string type of address (optional, alpha, ex.UTF-8)
			char 		SCTimeStamp[21];			// time stamp "yy/MM/dd,hh:mm:ss+(-)zz" (optional, scts)
			char 		data[MAX_SMS_TEXT_LEN+1];	// data
			U16 			length; 						// length of data(optional)
		}submit;
		
		struct{
			HfgSMSMask	mask;				// bitwise value for optional values (ra, tora)
			U8			fo; 					// first octet (default 2)	
			U8			mr; 					// Message Reference (mr)
			char 		ra[MAX_ADDR_LEN+1];	// Recipient  Address (ra)
			U8			addrType;			// type of address (optional, tora) 
			char 		SCTimeStamp[21];	//"yy/MM/dd,hh:mm:ss+zz" (scts) 
			char 		dt[21];				// Discahrge Time  "yy/MM/dd,hh:mm:ss+(-)zz"(dt)
			U8			st; 					// Status of previously sent message	(st)
		}status;
		struct{
			U8			fo; 			// first octet (default 2)
			U8			ct; 			// Command type in integer format (default 0)
		}command;
		struct{
			U16 			sn; 							// serial number for particular message (sn)
			U16 			mid;							// message identifier (mid)
			U8			page;						// page parameter. 0-3 bit:total number of pages, 4-7 bit: page number
			char 		data[MAX_SMS_TEXT_LEN+1];	// data
			U16 			length; 						// length of data (not transmitted)
			U8			dcs;							// data coding scheme (only for +CMGR)
		}cbm;
		struct{
			HfgSMSMask	mask;						// bitwise value for optional values (alpha)
			char			alpha[MAX_CHARSET_LEN+1];	// string type of address(optional, alpha, ex.UTF-8)
			char 		data[MAX_SMS_TEXT_LEN+1];	// PDU
			U16 			length; 						// data length of PDU
		}pdu;
	}msg;
	kal_bool				bFinal;
}bt_hfg_list_msg_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_READ_MSG_REQ									*/
/* AT_READ_MESSAGE					 							*/
/* TEXT MODE & SMS-DELIVERs : 									*/
/* +CMGR:<stat>,<oa>,[<alpha>],[<scts>][,<tooa>,<fo>,<pid>,		*/
/*            <dcs>,<sca>,<tosca>,<length>]<CR><LF><data> 			*/
/* TEXT MODE & SMS-SUBMIT : 										*/
/* +CMGR:<stat>,<da>,[<alpha>][,<toda>,<fo>,<pid>,<dcs>,[<vp>],	*/
/*            <sca>,<tosca>,<length>]<CR><LF><data> 					*/
/* TEXT MODE & SMS-STATUS-REPORT : 								*/
/* +CMGR:<stat>,<fo>,<mr>,[<ra>],[<tora>],<scts>,<dt>,<st>			*/
/*            <CR><LF><data> 										*/
/* TEXT MODE & SMS-COMMANDs : 									*/
/* +CMGR:<stat>,<fo>,<ct>[,<pid>,[<mn>],[<da>],[<toda>],			*/
/*            <length><CR><LF><cdata>] 								*/
/* TEXT MODE & CMB storage : 										*/
/* +CMGR:<stat>,<sn>,<mid>,<dcs>,<page>,<pages><CR><LF><data> */
/* PDU MODE : 													*/
/* +CMGR:<stat>,[<alpha>],<length><CR><LF><pdu> 				*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* read message */
	HfgSMSType			type;
	HfgSMSStat			stat;
	union{
		struct{
			HfgSMSMask 	mask;						// bitwise value for optional values (alpha, tooa, fo, pid, dcs, sca, tosca, length)
			char 		addr[MAX_ADDR_LEN+1];		// originated address
			U8			addrType;					// type of address(optional, tooa)
			char			alpha[MAX_CHARSET_LEN+1]; 	// string type of address(optional, alpha)
			char			SCTimeStamp[21];			// "yy/MM/dd,hh:mm:ss+zz" (scts)
			U8			fo; 							// first octet (default 17, optional, fo)
			U8			pid;							// Protocol Identifer (default 0, optional)
			U8			dcs;							// data coding scheme (optional)
			char			SCAddr[MAX_ADDR_LEN+1];	// Service Centre address (optional, sca)
			U8			SCAddrType; 				// type of Service Centre address(optional, tosca) 
			char 		data[MAX_SMS_TEXT_LEN+1];	// data
			U16 			length; 						// length of data (optional)				
		}deliver;
		
		struct{
			HfgSMSMask 	mask;						// bitwise value for optional values (alpha, scts, tooa/toda, length)
			char			addr[MAX_ADDR_LEN+1];		// originated address, destination address (oa/da)
			U8			addrType;					// type of address (tooa/toda)
			char			alpha[MAX_CHARSET_LEN+1];	// string type of address (optional, alpha, ex.UTF-8)
			U8			fo;							// first octet (default 17, optional)	
			U8			pid;							// Protocol Identifer (default 0, optional)
			U8			dcs;							// data coding scheme (optional)
			U8			intVp;						// valid only if length of strVp is 0.Validity period in integer format(default 167, optional)
			char			strVp[21];					// Validity period in time stamp format (yy/MM/dd,hh:mm:ss+-zz)
			char			SCAddr[MAX_ADDR_LEN+1];	// Service Centre address (optional, sca)
			U8			SCAddrType;					// type of Service Centre address(optional, tosca) 
			char			data[MAX_SMS_TEXT_LEN+1];		// data
			U16 			length; 						// length of data(optional)
		}submit;
				
		struct{
			HfgSMSMask 	mask;				// bitwise value for optional values (ra, tora)
			U8			fo; 					// first octet (default 2)	
			U8			mr; 					// Message Reference (mr)
			char 		ra[MAX_ADDR_LEN+1];	// Recipient  Address (ra)
			U8			addrType;			// type of address (optional, tora) 
			char 		SCTimeStamp[21];	//"yy/MM/dd,hh:mm:ss+zz" (scts) 
			char 		dt[21];				// Discahrge Time  "yy/MM/dd,hh:mm:ss+(-)zz"(dt)
			U8			st; 					// Status of previously sent message	(st)			
		}status;
		
		struct{
			HfgSMSMask	mask;							// bitwise value for optional values (pid, mn, da, toda, length, cdata)
			U8			fo; 								// first octet (default 2)		
			U8			ct; 								// Command type
			U8			pid;								// Protocol Identifer (default 0, optional)
			U8			mn; 							// Message Number previously MR value (optional, mn) 
			char			destAddr[MAX_ADDR_LEN+1];		// destnation address (otional, da)
			U8			addrType;						// type of address(optional, toda)	
			char			cmdData[MAX_SMS_TEXT_LEN+1];	// command data(optional)
			U16 			length; 							// length of data (optional)	
		}command;
		
		struct{
			U16 			sn; 							// serial number for particular message (sn)
			U16 			mid;							// message identifier (mid)
			U8			page;						// page parameter. 0-3 bit:total number of pages, 4-7 bit: page number
			char 		data[MAX_SMS_TEXT_LEN+1];	// data
			U16 			length; 						// length of data (not transmitted)
			U8			dcs;							// data coding scheme (only for +CMGR)
		}cbm;
		
		struct{
			HfgSMSMask	mask;						// bitwise value for optional values (alpha)
			char			alpha[MAX_CHARSET_LEN+1];	// string type of address(optional, alpha, ex.UTF-8)
			char 		data[MAX_SMS_TEXT_LEN+1];	// PDU
			U16 			length; 						// data length of PDU
		}pdu;
	}msg;

}bt_hfg_read_msg_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SEND_MSG_REQ									*/
/* AT_SEND_MESSAGE		 										*/
/* TEXT MODE : +CMGS:<mr>[,<scts>] 								*/
/* PDU MODE : +CMGS:<mr>[,<ackpdu>] 								*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	/* Send message response */	
	HfgSMSMask			mask;						// HFG_SMS_MASK_SCTS and HFG_SMS_MASK_ACKPDU
	U8					mr;
	char					scts[21];						// optional in Text mode
	char					ackpdu[MAX_SMS_TEXT_LEN+1];	// optional in PDU mode
}bt_hfg_send_msg_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SEND_STORED_MSG_REQ							*/
/* AT_SEND_MESSAGE		 										*/
/* TEXT MODE : +CMSS:<mr>[,<scts>] 								*/
/* PDU MODE : +CMSS:<mr>[,<ackpdu>] 								*/
/****************************************************************/
typedef bt_hfg_send_msg_req_struct bt_hfg_send_stored_msg_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_WRITE_MSG_REQ									*/
/* AT_STORE_MESSAGE		 										*/
/* TEXT MODE : +CMGW:<index> 									*/
/* PDU MODE : +CMGW:<index> 									*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	U16					index;
}bt_hfg_write_msg_req_struct;

/****************************************************************/
/* MSG_ID_BT_HFG_SMS_ERROR_REQ									*/
/* AT_SMS_FAILURE_RESULT										*/
/* +CMS ERROR: <err> 									*/
/****************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	U16					error;
}bt_hfg_sms_error_req_struct;

/* SMS Indications */
/************************************************/
/* AT_SELECT_SMS_SERVICE 						*/
/************************************************/
/********************************************************/
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_SMS_SERVICE_IND 		*/
/* AT+CSMS=?											*/
/********************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_sms_service_ind_struct;

/********************************************************/
/* MSG_ID_BT_HFG_QUERY_SELECTED_SMS_SERVICE_IND 		*/
/* AT+CSMS?												*/
/********************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_sms_service_ind_struct;

/********************************************************/
/* MSG_ID_BT_HFG_SELECT_SMS_SERVICE_IND			 		*/
/* AT+CSMS=<service>									*/
/********************************************************/
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
	/* service type */
	U8					service;
} bt_hfg_select_sms_service_ind_struct;


/************************************************/
/* AT_PREFERRED_SMS_STORAGE					*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_PREF_MSG_STORAGE_IND	*/
/* AT+CPMS=?												*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_pref_msg_storage_ind_struct;

/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SELECTED_PREF_MSG_STORAGE_IND		*/
/* AT+CPMS?													*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_pref_msg_storage_ind_struct;
/********************************************************/
/* MSG_ID_BT_HFG_SELECT_PREF_MSG_STORAGE_IND	 		*/
/* AT+CPMS=<mem1>[,<mem2>[,<mem3>]]					*/
/********************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	//U8							count;	/* 1~2. 1: only read provided, 2 : read and write are provided. 3: all provided */
	/* HFG_SMS_STORAGE_UNKNOWN indicates the field is not specified */
	HfgSMSStorage				read;	/* set storage type */
	HfgSMSStorage				write;	/* Memory to which writing and sending operations are made. */
	HfgSMSStorage				recv;	/* Memory to which received SMs are preferred to be stored. */
} bt_hfg_select_pref_msg_storage_ind_struct;

/************************************************/
/* AT_SMS_MESSAGE_FORMAT						*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_MSG_FORMAT_IND			*/
/* AT+CMGF=?												*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_msg_format_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SELECTED_MSG_FORMAT_IND			*/
/* AT+CMGF?													*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_msg_format_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_SELECT_MSG_FORMAT_IND 					*/
/* AT+CMGF=<mode>											*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	U8							format;			/* 0: PDU mode. 1: Text mode */
} bt_hfg_select_msg_format_ind_struct;

/************************************************/
/* AT_SMS_SERVICE_CENTER						*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SERVICE_CENTRE_IND 					*/
/* AT+CSCA?													*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_service_centre_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_SET_SERVICE_CENTRE_IND 					*/
/* AT+CSCA=<sca>[,<tosca>]									*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	HfgSMSMask					mask;
	char 						addr[MAX_ADDR_LEN+1];
	U8		   					addrType;	  		// optional parameter <tosca>
} bt_hfg_set_service_centre_ind_struct;


/************************************************/
/* AT_SET_TEXT_MODE_PARMS 						*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_TEXT_MODE_PARAMS_IND				*/
/* AT+CSMP?													*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_text_mode_params_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_SET_TEXT_MODE_PARAMS_IND 					*/
/* AT+CSMP=[<fo>[,<vp>[,<pid>[,<dcs>]]]]						*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	HfgSMSMask					mask;
	U8	 						fo;
	U8							intVp;		//valid if length of strVp is 0
	char							strVp[21];
	U8	  						pid;
	U8	  						dcs;
} bt_hfg_set_text_mode_params_ind_struct;

/************************************************/
/* AT_SMS_SHOW_TEXT_MODE 						*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_SHOW_PARAMS_IND 		*/
/* AT+CSDH=?												*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_show_params_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SELECTED_SHOW_PARAMS_IND 			*/
/* AT+CSDH?													*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_show_params_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_SET_SHOW_PARAMS_IND 						*/
/* AT+CSDH=[<show>]											*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	U8							show;
} bt_hfg_set_show_params_ind_struct;


/************************************************/
/* AT_NEW_MESSAGE_INDICATION					*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_NEW_MSG_INDICATION_IND 	*/
/* AT+CNMI=?												*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_new_msg_indication_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SELECTED_NEW_MSG_INDICATION_IND 	*/
/* AT+CNMI?													*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_selected_new_msg_indication_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_SET_NEW_MSG_INDICATION_IND 				*/
/* AT+CNMI=[<mode>[,<mt>[,<bm>[,<ds>[,<bfr>]]]]]				*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	/* If the the fields are not given, assign 0 (default value) */
	U8 							mode;
	U8 							mt;
	U8 							bm;
	U8 							ds;
	U8 							bfr;
} bt_hfg_set_new_msg_indication_ind_struct;

/************************************************/
/* AT_LIST_MESSAGES								*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_QUERY_SUPPORTED_LIST_STATUS_IND 			*/
/* AT+CMGL=?												*/
/************************************************************/
typedef bt_hfg_common_ind_struct bt_hfg_query_supported_list_status_ind_struct;
/************************************************************/
/* MSG_ID_BT_HFG_LIST_MSG_IND 								*/
/* AT+CMGL[=<stat>]											*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	/* If the the field stat is not given, assign 0 (default value) */
	HfgSMSStat					stat;
} bt_hfg_list_msg_ind_struct;


/************************************************/
/* AT_READ_MESSAGE								*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_READ_MSG_IND 								*/
/* AT+CMGR=<index>											*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	U16 							index;
} bt_hfg_read_msg_ind_struct;

/************************************************/
/* AT_SEND_MESSAGE								*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_SEND_MSG_IND 								*/
/* TEXT MODE : 												*/
/* AT+CMGS=<da>[,<toda>]<CR>text is entered<ctrl-Z/ESC> 		*/
/* PDU MODE : 												*/
/* AT+CMGS=<length><CR>PDU is given<ctrl-Z/ESC> 				*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	HfgSMSMask					mask;
	char  						addr[MAX_ADDR_LEN+1];		/* PDU mode parameters */
	U8			   				addrType;
	U16	   						pduLen;					/* PDU mode parameters */
	char 						msg[MAX_SMS_TEXT_LEN+1];	/* msg data */
} bt_hfg_send_msg_ind_struct;


/************************************************/
/* AT_SEND_STORED_MESSAGE						*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_SEND_STORED_MSG_IND 						*/
/* AT+CMSS=<index>[,<da>[,<toda>]]							*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	HfgSMSMask					mask;
	U16 							index;
	char 						addr[MAX_ADDR_LEN+1];
	U8 							addrType;
} bt_hfg_send_stored_msg_ind_struct;


/************************************************/
/* AT_STORE_MESSAGE								*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_WRITE_MSG_IND 							*/
/* TEXT MODE : 												*/
/* AT+CMGW=<oa/da>[,<toda/toda>[,<stat>]]					*/
/*                  <CR>text is entered<ctrl-Z/ESC> 					*/
/* PDU MODE : AT+CMGW=<length>[,<stat>]<CR>					*/
/*                   PDU is given<ctrl-Z/ESC> 							*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	HfgSMSMask					mask;
	char  						addr[MAX_ADDR_LEN+1];		/* PDU mode parameters */
	U8			   				addrType;
	U16	   						pduLen;					/* PDU mode parameters */
	HfgSMSStat					stat;
	char 						msg[MAX_SMS_TEXT_LEN+1];	/* msg data */
} bt_hfg_write_msg_ind_struct;


/************************************************/
/* AT_DELETE_MESSAGE							*/
/************************************************/
/************************************************************/
/* MSG_ID_BT_HFG_DELETE_MSG_IND 							*/
/* AT+CMGD=<index> 											*/
/************************************************************/
typedef struct
{
	LOCAL_PARA_HDR
	void*						pContext;
	void*						user_context;
	U16 							index;
} bt_hfg_delete_msg_ind_struct;

/************************************************/
/* UT test message structures						*/
/************************************************/
#ifdef BT_HFG_UT_TEST
/* MSG_ID_BT_HFG_UT_INIT_REQ */
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
} bt_hfg_ut_init_req_struct;
/* MSG_ID_BT_HFG_UT_INIT_CNF */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U16                   result;
}bt_hfg_ut_init_cnf_struct;
/* Simulate RF data send to HFG */
/* MSG_ID_BT_HFG_UT_TX_REQ */
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
	U16					len;
	char					buf[200];
} bt_hfg_ut_tx_req_struct;
/* MSG_ID_BT_HFG_UT_TX_CNF */
typedef struct
{
    LOCAL_PARA_HDR
    void*                 pContext;
    void*                 req_context;
    U16                   result;
}bt_hfg_ut_tx_cnf_struct;
/* Simulate RF data send by HFG */
/* MSG_ID_BT_HFG_UT_RX_IND */
typedef struct
{
    LOCAL_PARA_HDR
    void*                           pContext;
    void*                           user_context;
	U16					len;
	char					buf[200];
} bt_hfg_ut_rx_ind_struct;
/* MSG_ID_BT_HFG_UT_RX_RSP */
typedef struct
{
	LOCAL_PARA_HDR
	void*				pContext;
	void*				req_context;	/* Specific context for the result. it will returned when result is sent. */
} bt_hfg_ut_rx_rsp_struct;
#endif /* BT_HFG_UT_TEST */

#endif 
#endif//BT_HFG_STRUCT_H

