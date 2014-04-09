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

/*siano_uam_api.h*/

#ifndef __SIANO_UAM_API_H__
#define __SIANO_UAM_API_H__

#define __MBBMS_UAM_SUPPORT__
#ifdef __MBBMS_UAM_SUPPORT__

//#include "smshostliblitecmmb.h"
//#include "SmsPlatDefs.h"
#include "ICmmbSp.h"

#include "CmmbUamCommon.h"
using namespace android;

#define SMS_UAM_MAX_ATR_LEN		(64)
#define SMS_UAM_DEFAULT_ATR_LEN	(21)
#if 0                       // Move to CmmbUamCommon.h

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
#endif                            // Move to CmmbUamCommon.h
namespace android{
	#if 0                              // Move to CmmbUamCommon.h
//*******************************************************************************
//				Typedefs/structs/enums
////
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

typedef unsigned char MBBMS_TEL_UINT8;
typedef unsigned short MBBMS_TEL_UINT16;
typedef unsigned int MBBMS_TEL_UINT32;

typedef char MBBMS_TEL_INT8;
typedef short MBBMS_TEL_INT16;
typedef int MBBMS_TEL_INT32;

typedef int MBBMS_TEL_BOOL;
typedef int MBBMS_TEL_BOOLEAN;

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

typedef MBBMS_TEL_GSM_USER_AUTH_RSP_S MBBMS_TEL_3G_USER_AUTH_RSP_S;
#endif                                   // Move to CmmbUamCommon.h
/* APIs */

/*
 * \brief Open session before using facilities in library
 * \param
 * \return MBBMS_TEL_INT32
 * 	<0 - Error
 * 	>=0 - Session ID
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_OpenSession_Siano(sp<ICmmbSp> spt);

/*
 * \brief Close session before quit
 * \param MBBMS_TEL_INT32 - Session ID
 * \return MBBMS_TEL_INT32
 * 	<0 - Error
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_CloseSession_Siano(MBBMS_TEL_INT32 sessionId);

/*
 * \brief Get the sim type
 * \param
 * \return MBBMS_TEL_INT32
 * 	<0 - Error
 * 	>=0 - Session ID
 * \note
 */
MBBMS_TEL_ENV_E MBBMS_TEL_Get_Env_Siano(MBBMS_TEL_INT32 sessionId,sp<ICmmbSp> spt);

/*
 * \brief Get IMSI
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - imsi
 * \return MBBMS_TEL_RESULT_E
 * \note
 */
MBBMS_TEL_RESULT_E MBBMS_TEL_Get_IMSI_Siano(MBBMS_TEL_INT32 sessionId, char imsi[MBBMS_TEL_IMSI_LEN],sp<ICmmbSp> spt);

/*
 * \brief Get IMEI
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - imei
 * \return MBBMS_TEL_RESULT_E
 * \note
 */
MBBMS_TEL_RESULT_E MBBMS_TEL_Get_IMEI_Siano(MBBMS_TEL_INT32 sessionId, char imei[MBBMS_TEL_IMEI_LEN],sp<ICmmbSp> spt);

/*
 * \brief Get LAC (Location Area Code) and CID (Cell ID)
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - lac
 * \param char* - cid
 */
MBBMS_TEL_RESULT_E MBBMS_TEL_Get_LACCID_Siano(MBBMS_TEL_INT32 sessionId, char lac[MBBMS_TEL_LAC_LEN], char cid[MBBMS_TEL_CID_LEN],sp<ICmmbSp> spt);

/*
 * \brief Check SIM card support Mobile TV feature or not
 * \param MBBMS_TEL_INT32 - Session ID
 * \return MBBMS_TEL_STATUS_E
 * \note
 */
MBBMS_TEL_STATUS_E MBBMS_TEL_IsEnabled_Siano(MBBMS_TEL_INT32 sessionId,sp<ICmmbSp> spt);

/*
 * \brief Check need to start a GBA Auth process or not
 * \param MBBMS_TEL_INT32 - Session ID
 * \return MBBMS_TEL_BOOLEAN - TRUE/FALSE
 * \note
 */
MBBMS_TEL_BOOLEAN MBBMS_TEL_IsGBANeeded_Siano(MBBMS_TEL_INT32 sessionId,sp<ICmmbSp> spt);        

/*
 * \brief Check a MSK is available and valid by Domain ID, MSK ID
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_UINT8* - Domain ID
 * \param MBBMS_TEL_UINT8* - MSK ID
 * \return MBBMS_TEL_BOOLEAN - TRUE/FALSE
 * \note
 */
MBBMS_TEL_BOOLEAN MBBMS_TEL_IsMSKValid_Siano(MBBMS_TEL_INT32 sessionId,         
			    MBBMS_TEL_UINT8 *domain_id,
			    MBBMS_TEL_UINT8 *msk_id,sp<ICmmbSp> spt);
/*
 * \brief Start a GBA Auth process
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_GBA_REQ_S - req
 * \param MBBMS_TEL_GBA_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_StartGBA_Siano(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_GBA_REQ_S req,
			MBBMS_TEL_GBA_RSP_S *rsp,sp<ICmmbSp> spt);

/*******************************************************************************
* GSM Authentication.
*******************************************************************************/

/*
 * \brief Execute an authentication of GSM security context process
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_GSM_AUTH_REQ_S - req
 * \param MBBMS_TEL_GSM_AUTH_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_GSM_Authenticate_Siano(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_GSM_AUTH_REQ_S req,
			MBBMS_TEL_GSM_AUTH_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Execute an authentication of user's GSM security context process
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_GSM_USER_AUTH_REQ_S - req
 * \param MBBMS_TEL_GSM_USER_AUTH_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_GSM_User_Authenticate_Siano(MBBMS_TEL_INT32 sessionId, MBBMS_TEL_GSM_USER_AUTH_REQ_S *req, MBBMS_TEL_GSM_USER_AUTH_RSP_S *rsp,sp<ICmmbSp> spt);  

/*******************************************************************************
* 3G Authentication.
*******************************************************************************/
/*
 * \brief Execute an authentication of 3G security context process
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_3G_AUTH_REQ_S - req
 * \param MBBMS_TEL_3G_AUTH_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_3G_Authenticate_Siano(MBBMS_TEL_INT32 sessionId,
			MBBMS_TEL_3G_AUTH_REQ_S req,
			MBBMS_TEL_3G_AUTH_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Execute an authentication of user's 3G security context process
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_3G_USER_AUTH_REQ_S - req
 * \param MBBMS_TEL_3G_USER_AUTH_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_3G_User_Authenticate_Siano(MBBMS_TEL_INT32 sessionId, MBBMS_TEL_3G_USER_AUTH_REQ_S *req, MBBMS_TEL_3G_USER_AUTH_RSP_S *rsp,sp<ICmmbSp> spt);  

/*
 * \brief Update B-TID and Ks lifetime when GBA Auth done
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_BTID_REQ_S - req
 * \param MBBMS_TEL_BTID_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_UpdateBTID_Siano(MBBMS_TEL_INT32 sessionId,                 
			  MBBMS_TEL_BTID_REQ_S *req,
			  MBBMS_TEL_BTID_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Generate a MRK
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_MRK_REQ_S - req
 * \param MBBMS_TEL_MRK_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_GetMRK_Siano(MBBMS_TEL_INT32 sessionId,                       
		      MBBMS_TEL_MRK_REQ_S *req,
		      MBBMS_TEL_MRK_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Update MSK
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_MSK_REQ_S - req
 * \param MBBMS_TEL_MSK_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_UpdateMSK_Siano(MBBMS_TEL_INT32 sessionId,                     
			 MBBMS_TEL_MSK_REQ_S *req,
			 MBBMS_TEL_MSK_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Generate Control Words by a MTK
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_MTK_REQ_S - req
 * \param MBBMS_TEL_MTK_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_GetMTK_Siano(MBBMS_TEL_INT32 sessionId,                             
		      MBBMS_TEL_MTK_REQ_S *req,
		      MBBMS_TEL_MTK_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Seek timestampe
 * \param MBBMS_TEL_INT32 - Session ID
 * \param MBBMS_TEL_TS_REQ_S - req
 * \param MBBMS_TEL_TS_RSP_S - rsp
 * \return MBBMS_TEL_INT32 <0 - Error, check result in response structure for detial
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_SeekTimeStamp_Siano(MBBMS_TEL_INT32 sessionId,
			     MBBMS_TEL_TS_REQ_S req,
			     MBBMS_TEL_TS_RSP_S *rsp,sp<ICmmbSp> spt);

/*
 * \brief Get B-TID
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - b-tid
 * \return MBBMS_TEL_RESULT_E
 * \note
 */
MBBMS_TEL_RESULT_E MBBMS_TEL_Get_BTID_Siano(MBBMS_TEL_INT32 sessionId, char btid[MBBMS_TEL_BTID_LEN],sp<ICmmbSp> spt);    

/*
 * \brief Get SN
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - mtvsn
 * \return MBBMS_TEL_RESULT_E
 * \note
 */
MBBMS_TEL_RESULT_E MBBMS_TEL_GetCMMBSN_Siano(MBBMS_TEL_INT32 sessionId, char cmmbsn[MBBMS_TEL_CMMBSN_RSP_LEN],sp<ICmmbSp> spt);   

/*
 * \brief Get User Authentication Device version
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - sdversion
 * \return MBBMS_TEL_RESULT_E
 * \note
 */
MBBMS_TEL_RESULT_E MBBMS_TEL_User_Auth_GetVersion_Siano(MBBMS_TEL_INT32 sessionId, char version[MBBMS_TEL_USER_AUTH_VERSION_LEN],sp<ICmmbSp> spt);     
/*
 * \brief SetCaControlWords
 * \param MBBMS_TEL_INT32 - Session ID
 * \param char* - sdversion
 * \return MBBMS_TEL_RESULT_E
 * \note
 */
MBBMS_TEL_INT32 MBBMS_TEL_SetCaControlWords_Siano(MBBMS_TEL_INT32 sessionId,TCmmbUamCaCwPair* req,sp<ICmmbSp> spt);

BOOL SetSimType_Siano(UAM_SIM_TYPE_ENUM sim_type);

MBBMS_TEL_INT32 MBBMS_TEL_Send_RetValue_Siano(UINT16 nStatus,sp<ICmmbSp> spt);
}
#endif           // __MBBMS_UAM_SUPPORT__
#endif

