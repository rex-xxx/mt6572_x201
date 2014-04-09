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

#ifndef _CMMBUAMCOMMON_H__
#define _CMMBUAMCOMMON_H__


using namespace android;
#define SMS_UAM_MAX_ATR_LEN		(64)
#define SMS_UAM_DEFAULT_ATR_LEN	(21)

#define MBBMS_TEL_IMSI_LEN (16+1)
#define MBBMS_TEL_IMEI_LEN (16+1)
#define MBBMS_TEL_RAND_LEN (16*2+1)
#define MBBMS_TEL_KS_INPUT_LEN (16*2+1)
#define MBBMS_TEL_RSP_LEN (16*2+2*2+1) // include DB10
#define MBBMS_TEL_BTID_LEN (64*2+1)
#define MBBMS_TEL_KS_LEN (8*2+1)
#define MBBMS_TEL_NAF_ID_LEN (32*2+1)
#define MBBMS_TEL_IMPI_LEN (64*2+1)
#define MBBMS_TEL_MRK_LEN (32*2+2*2+1) // include DB20
#define MBBMS_TEL_MSK_LEN (512*2+1)
#define MBBMS_TEL_MSK_RSP_LEN (128*2+1) // include DB80
#define MBBMS_TEL_MTK_LEN (256*2+1)
#define MBBMS_TEL_CW_LEN (16*2+2*2+1) // include DB10
#define MBBMS_TEL_TS_KEY_LEN (64*2+1)
#define MBBMS_TEL_TS_LEN (8*2+1)
#define MBBMS_TEL_DOMAIN_ID_LEN (3*2+1)
#define MBBMS_TEL_MSK_ID_LEN (4*2+1)
#define MBBMS_TEL_CMMBSN_RSP_LEN (16*2+1)

// Location area code in hexadecimal format
#define MBBMS_TEL_LAC_LEN (2*2+1)
// Cell ID
#define MBBMS_TEL_CID_LEN (2*2+1)

#define MBBMS_TEL_KC_LEN (12*2+1)

// GSM 4 bytes, 3G 8 bytes, auts 14 bytes
#define MBBMS_TEL_SRES_LEN (16*2+1)
#define MBBMS_TEL_RSP_SD_MAX_LEN (16*2+10*2+2*2+1*2+1)

// 3G
#define MBBMS_TEL_CK_LEN (16*2+1)
#define MBBMS_TEL_IK_LEN (16*2+1)

#define MBBMS_TEL_RES_LEN (16*2+1)
#define MBBMS_TEL_CNONCE_LEN (16*2+1)
#define MBBMS_TEL_STATUS_LEN (1*2+1)

#define MBBMS_TEL_USER_AUTH_VERSION_LEN (4*2+1)


#define APN_LEN 100

#define TRUE 1

#define FALSE 0


typedef unsigned char MBBMS_TEL_UINT8;
typedef unsigned short MBBMS_TEL_UINT16;
typedef unsigned int MBBMS_TEL_UINT32;

typedef char MBBMS_TEL_INT8;
typedef short MBBMS_TEL_INT16;
typedef int MBBMS_TEL_INT32;

typedef int MBBMS_TEL_BOOL;
typedef int MBBMS_TEL_BOOLEAN;

namespace android{
typedef enum MBBMS_TEL_SIM_MODE_EN{
    MBBMS_TEL_SIM_UNKNOWN = 0,
    MBBMS_TEL_SIM_2G = 2,
    MBBMS_TEL_SIM_3G= 3 	
} MBBMS_TEL_SIM_MODE_E;

typedef enum UAM_ERR_CODES_EN{
    SMS_UAM_OK		= 0,
    SMS_UAM_ERR		= 1,
}UAM_ERR_CODES_E;

typedef struct MbmsUamState_S{	
    BOOL	IsInit;
    int		SessionNum;
    MBBMS_TEL_SIM_MODE_E	SimMode;
    UINT8	pAidCmd[17];

    BOOL NeedSelectFile;
    volatile UINT32 UamMutex;
}MbbmsUamState_ST;


typedef enum _MBBMS_TEL_RESULT_E
{
	MBBMS_TEL_RESULT_OK_EV       = 0,    //Acknowledges execution of a command
	MBBMS_TEL_RESULT_CONNECT_EV  = 1,    /*A connection has been established, the DCE is moving
					       from command state to online data state*/
	MBBMS_TEL_RESULT_ERROR_EV    = 4,    //Acknowledges execution of a command

	MBBMS_TEL_RESULT_NO_ACK      = 5,

	MBBMS_TEL_RESULT_CME_ERROR_BASE_EV = 1000,   // CME ERROR
	MBBMS_TEL_RESULT_CME_ERROR_MAC_ERROR_EV =MBBMS_TEL_RESULT_CME_ERROR_BASE_EV +48,  //MAC Error
	MBBMS_TEL_RESULT_CME_ERROR_KEY_EXPIRED_EV =MBBMS_TEL_RESULT_CME_ERROR_BASE_EV +50, // Confidential key is expired
	MBBMS_TEL_RESULT_CME_ERROR_NO_KEY_EV =MBBMS_TEL_RESULT_CME_ERROR_BASE_EV +53,     //Cannot find Confidential key

} MBBMS_TEL_RESULT_E;
typedef enum _MBBMS_TEL_ENV_E
{
	MBBMS_TEL_ENV_UNKNOWN = 0,	// Unknown environment.
	MBBMS_TEL_ENV_GSM = 1,		// GSM environment.
	MBBMS_TEL_ENV_3G = 2		// 3G environment.
} MBBMS_TEL_ENV_E;

/* Indicate MTV feature is supported or not in SIM */
typedef enum _MBBMS_TEL_STATUS_E
{
	MBBMS_TEL_STATUS_DISABLED_EV = 0,
	MBBMS_TEL_STATUS_ENABLED_EV
} MBBMS_TEL_STATUS_E;


typedef enum _MBBMS_TEL_KEY_TYPE_E
{
	MBBMS_TEL_KEY_TYPE_MUK_EV = 0,
	MBBMS_TEL_KEY_TYPE_MSK_EV = 1
} MBBMS_TEL_KEY_TYPE_E;
/* request/response data structure */

/* GBA */
typedef struct _MBBMS_TEL_GBA_REQ_S
{
	MBBMS_TEL_UINT8 rand[MBBMS_TEL_RAND_LEN];
	MBBMS_TEL_UINT8 ks_input[MBBMS_TEL_KS_INPUT_LEN];
} MBBMS_TEL_GBA_REQ_S;

typedef struct _MBBMS_TEL_GBA_RSP_S
{
	MBBMS_TEL_RESULT_E result;
	MBBMS_TEL_UINT8 rsp[MBBMS_TEL_RSP_LEN];
} MBBMS_TEL_GBA_RSP_S;

/* B-TID */
typedef struct _MBBMS_TEL_BTID_REQ_S
{
	MBBMS_TEL_UINT8 btid[MBBMS_TEL_BTID_LEN]; /* <= 64 bytes */
	MBBMS_TEL_UINT8 ks[MBBMS_TEL_KS_LEN];
} MBBMS_TEL_BTID_REQ_S;

typedef struct _MBBMS_TEL_BTID_RSP_S
{
	MBBMS_TEL_RESULT_E result;
} MBBMS_TEL_BTID_RSP_S;

/* generate MRK */
typedef struct _MBBMS_TEL_MRK_REQ_S
{
	MBBMS_TEL_UINT8 naf_id[MBBMS_TEL_NAF_ID_LEN];
	MBBMS_TEL_UINT8 impi[MBBMS_TEL_IMPI_LEN];
} MBBMS_TEL_MRK_REQ_S;

typedef struct _MBBMS_TEL_MRK_RSP_S
{
	MBBMS_TEL_RESULT_E result;
	MBBMS_TEL_UINT8 mrk[MBBMS_TEL_MRK_LEN];
} MBBMS_TEL_MRK_RSP_S;

/* update MSK */
typedef struct _MBBMS_TEL_MSK_REQ_S
{
	MBBMS_TEL_UINT8 msk[MBBMS_TEL_MSK_LEN]; /* unlimited, 512 bytes are suggested */
} MBBMS_TEL_MSK_REQ_S;

typedef struct _MBBMS_TEL_MSK_RSP_S
{
	MBBMS_TEL_RESULT_E result;
	MBBMS_TEL_UINT8 rsp[MBBMS_TEL_MSK_RSP_LEN];
} MBBMS_TEL_MSK_RSP_S;

/* get Control Words by MTK */
typedef struct _MBBMS_TEL_MTK_REQ_S
{
	MBBMS_TEL_UINT8 mtk[MBBMS_TEL_MTK_LEN]; /* <= 256 bytes */
} MBBMS_TEL_MTK_REQ_S;

typedef struct _MBBMS_TEL_MTK_RSP_S
{
	MBBMS_TEL_RESULT_E result;
	MBBMS_TEL_UINT8 cw[MBBMS_TEL_CW_LEN];
} MBBMS_TEL_MTK_RSP_S;

typedef struct _MBBMS_TEL_TS_REQ_S
{
	MBBMS_TEL_KEY_TYPE_E type;
	MBBMS_TEL_UINT8 key[MBBMS_TEL_TS_KEY_LEN]; /* <=64 bytes */
} MBBMS_TEL_TS_REQ_S;

typedef struct _MBBMS_TEL_TS_RSP_S
{
	MBBMS_TEL_RESULT_E result;
	MBBMS_TEL_UINT8 ts[MBBMS_TEL_TS_LEN];
} MBBMS_TEL_TS_RSP_S;

typedef enum _MBBMS_TEL_GPRS_APN_TYPE_E
{
	MBBMS_TEL_GPRS_APN_TYPE_CMNET_EV = 0,
	MBBMS_TEL_GPRS_APN_TYPE_CMWAP_EV
} MBBMS_TEL_GPRS_APN_TYPE_E;


typedef enum _MBBMS_TEL_GPRS_DEVICE_MODE_E
{
	MBBMS_TEL_GPRS_DEVICE_PPP_MODE_EV = 0,
	MBBMS_TEL_GPRS_DEVICE_DIRECTIP_MODE_EV
} MBBMS_TEL_GPRS_DEVICE_MODE_E;

/*******************************************************************************
* GSM Authentication.
*******************************************************************************/

typedef struct _MBBMS_TEL_GSM_AUTH_REQ_S
{
	MBBMS_TEL_UINT8 rand[MBBMS_TEL_RAND_LEN];
} MBBMS_TEL_GSM_AUTH_REQ_S;

typedef struct _MBBMS_TEL_GSM_AUTH_RSP_S
{
	MBBMS_TEL_RESULT_E result;

	MBBMS_TEL_UINT8 sres[MBBMS_TEL_SRES_LEN];
	MBBMS_TEL_UINT8 kc[MBBMS_TEL_KC_LEN];
} MBBMS_TEL_GSM_AUTH_RSP_S;

typedef struct _MBBMS_TEL_GSM_USER_AUTH_REQ_S
{
	MBBMS_TEL_UINT8 rand[MBBMS_TEL_RAND_LEN];
	MBBMS_TEL_UINT8 ks_input[MBBMS_TEL_KS_INPUT_LEN];

	MBBMS_TEL_UINT8 kc[MBBMS_TEL_KC_LEN];
	MBBMS_TEL_UINT8 sres[MBBMS_TEL_SRES_LEN];
} MBBMS_TEL_GSM_USER_AUTH_REQ_S;

typedef struct _MBBMS_TEL_GSM_USER_AUTH_RSP_S
{
	MBBMS_TEL_RESULT_E result;
	MBBMS_TEL_UINT8 status[MBBMS_TEL_STATUS_LEN];
	MBBMS_TEL_UINT8 res[MBBMS_TEL_RES_LEN];
	MBBMS_TEL_UINT8 cnonce[MBBMS_TEL_CNONCE_LEN];
} MBBMS_TEL_GSM_USER_AUTH_RSP_S;

/*******************************************************************************
* 3G Authentication.
*******************************************************************************/
typedef struct _MBBMS_TEL_3G_AUTH_REQ_S
{
	MBBMS_TEL_UINT8 rand[MBBMS_TEL_RAND_LEN];
	MBBMS_TEL_UINT8 ks_input[MBBMS_TEL_KS_INPUT_LEN];
} MBBMS_TEL_3G_AUTH_REQ_S;

typedef struct _MBBMS_TEL_3G_AUTH_RSP_S
{
	MBBMS_TEL_RESULT_E result;

	MBBMS_TEL_UINT8 status;
	MBBMS_TEL_UINT8 res[MBBMS_TEL_SRES_LEN];	// RES/AUTS
	MBBMS_TEL_UINT8 ck[MBBMS_TEL_CK_LEN];
	MBBMS_TEL_UINT8 ik[MBBMS_TEL_IK_LEN];
	MBBMS_TEL_UINT8 kc[MBBMS_TEL_KC_LEN];		// Reserved.
} MBBMS_TEL_3G_AUTH_RSP_S;

typedef struct _MBBMS_TEL_3G_USER_AUTH_REQ_S
{
	MBBMS_TEL_UINT8 rand[MBBMS_TEL_RAND_LEN];
	MBBMS_TEL_UINT8 ks_input[MBBMS_TEL_KS_INPUT_LEN];

	MBBMS_TEL_UINT8 res[MBBMS_TEL_SRES_LEN];
	MBBMS_TEL_UINT8 ck[MBBMS_TEL_CK_LEN];
	MBBMS_TEL_UINT8 ik[MBBMS_TEL_IK_LEN];

} MBBMS_TEL_3G_USER_AUTH_REQ_S;

#define UAM_CW_LEN 16
typedef MBBMS_TEL_GSM_USER_AUTH_RSP_S MBBMS_TEL_3G_USER_AUTH_RSP_S;
 struct TCmmbCaCw
{
	unsigned int	 Id; 
	unsigned char	 Cw[UAM_CW_LEN]; 
};
struct TCmmbUamCaCwPair
{
	TCmmbCaCw 	odd; 
	TCmmbCaCw 	even; 
};
typedef enum
{
    UAM_SIM_TYPE_UNKNOWN=0,
    UAM_SIM_TYPE_2G = 2,
    UAM_SIM_TYPE_3G = 3,
} UAM_SIM_TYPE_ENUM;

}      // android namespace
typedef struct _CMMBUAMFunc
{
	MBBMS_TEL_INT32 (*MBBMS_TEL_CloseSession)(MBBMS_TEL_INT32);
	MBBMS_TEL_INT32 (*MBBMS_TEL_OpenSession)(sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_GetMTK)(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_MTK_REQ_S *req,MBBMS_TEL_MTK_RSP_S *rsp,sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_SetCaControlWords)(MBBMS_TEL_INT32 sessionId,
			TCmmbUamCaCwPair* req,sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_UpdateMSK)(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_MSK_REQ_S *req,
			MBBMS_TEL_MSK_RSP_S *rsp,sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_GetMRK)(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_MRK_REQ_S *req,
			MBBMS_TEL_MRK_RSP_S *rsp,sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_UpdateBTID)(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_BTID_REQ_S *req,
			MBBMS_TEL_BTID_RSP_S *rsp,sp<ICmmbSp> spt);
	MBBMS_TEL_RESULT_E (*MBBMS_TEL_Get_BTID)(MBBMS_TEL_INT32 sessionId, 
			char btid[MBBMS_TEL_BTID_LEN],sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_3G_User_Authenticate)( MBBMS_TEL_INT32 sessionId, 
			MBBMS_TEL_3G_USER_AUTH_REQ_S *req, MBBMS_TEL_3G_USER_AUTH_RSP_S *rsp,sp<ICmmbSp> spt);
	MBBMS_TEL_INT32 (*MBBMS_TEL_GSM_User_Authenticate)( MBBMS_TEL_INT32 sessionId, 
			MBBMS_TEL_GSM_USER_AUTH_REQ_S *req, MBBMS_TEL_GSM_USER_AUTH_RSP_S *rsp ,sp<ICmmbSp> spt);
	MBBMS_TEL_BOOLEAN (*MBBMS_TEL_IsGBANeeded)(MBBMS_TEL_INT32 sessionId,sp<ICmmbSp> spt);
	MBBMS_TEL_RESULT_E (*MBBMS_TEL_User_Auth_GetVersion)(MBBMS_TEL_INT32 sessionId, 
			char version[MBBMS_TEL_USER_AUTH_VERSION_LEN],sp<ICmmbSp> spt);
	MBBMS_TEL_RESULT_E (*MBBMS_TEL_GetCMMBSN)(MBBMS_TEL_INT32 sessionId,
			char cmmbsn[MBBMS_TEL_CMMBSN_RSP_LEN],sp<ICmmbSp> spt);
        BOOL (*SetSimType)(UAM_SIM_TYPE_ENUM sim_type);
} CMMBUAMFuncStruct;
#endif   //_CMMBUAMCOMMON_H__
