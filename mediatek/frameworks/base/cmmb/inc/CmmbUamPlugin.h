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

/*CmmbUamPlugin.h*/
#ifndef  MBBMS_UAM_Siano_H__
#define  MBBMS_UAM_Siano_H__

#define __MBBMS_UAM_SUPPORT__
#ifdef __MBBMS_UAM_SUPPORT__
/*-------------------------------------------------------------------------*/
#include "Innofidei_uam_api.h"
#include "siano_uam_api.h"

#include "CmmbSPCommon.h"
#include "ICmmbSp.h"
#include "CmmbHelper.h"
#include <utils/RefBase.h>
//using namespace android;

#define UAM_RAND_LEN  MBBMS_TEL_RAND_LEN
#define UAM_KS_INPUT_LEN MBBMS_TEL_KS_INPUT_LEN
#define UAM_KC_LEN   MBBMS_TEL_KC_LEN
#define UAM_SRES_LEN MBBMS_TEL_SRES_LEN

#define UAM_RES_LEN MBBMS_TEL_SRES_LEN
#define UAM_CK_LEN  MBBMS_TEL_CK_LEN
#define UAM_IK_LEN  MBBMS_TEL_IK_LEN

#define UAM_CNONCE_LEN	 MBBMS_TEL_CNONCE_LEN
#define UAM_STATUS_LEN MBBMS_TEL_STATUS_LEN
	
#define UAM_CMMBSN_LEN MBBMS_TEL_CMMBSN_RSP_LEN

#define UAM_USER_AUTH_VERION_LEN MBBMS_TEL_USER_AUTH_VERSION_LEN

#define UAM_BTID_LEN MBBMS_TEL_BTID_LEN
#define UAM_KS_LEN MBBMS_TEL_KS_LEN

#define UAM_MRK_LEN MBBMS_TEL_MRK_LEN
#define UAM_IMPI_LEN MBBMS_TEL_IMPI_LEN
#define UAM_NAF_ID_LEN MBBMS_TEL_NAF_ID_LEN

#define UAM_MSK_LEN MBBMS_TEL_MSK_LEN
#define UAM_VER_MSG_LEN MBBMS_TEL_MSK_RSP_LEN

#define UAM_DOMAIN_ID_LEN MBBMS_TEL_DOMAIN_ID_LEN
#define UAM_MSK_ID_LEN   MBBMS_TEL_MSK_ID_LEN

#define UAM_MTK_LEN (MBBMS_TEL_MTK_LEN)
#define UAM_CW_LEN 16
namespace android{
/*UAM GSM authentication request structure.*/
typedef struct _TCmmbUam2GAuthReq
{
	UINT8 rand[UAM_RAND_LEN];                    //Buffer contains RAND.
	UINT8 ks_input[UAM_KS_INPUT_LEN];       //Buffer contains Ks-Input.
	UINT8 kc[UAM_KC_LEN];                           //Buffer contains Kc.
	UINT8 sres[UAM_SRES_LEN];                    //Buffer contains SRES.
}TCmmbUam2GAuthReq;

/*UAM 3G authentication request structure.*/
typedef struct _TCmmbUam3GAuthReq
{ 
	UINT8 rand[UAM_RAND_LEN];                    //Buffer contains RAND.
	UINT8 ks_input[UAM_KS_INPUT_LEN];       //Buffer contains Ks-Input.
	UINT8 res[UAM_RES_LEN];                         //Buffer contains RES.
	UINT8 ck[UAM_CK_LEN];                            //Buffer contains CK.
	UINT8 ik[UAM_IK_LEN];                              //Buffer contains IK.
}TCmmbUam3GAuthReq;

/*UAM GSM and 3G authentication response structure.*/
typedef struct _TCmmbUamAuthRsp
{ 
	UINT8 status[UAM_STATUS_LEN];          //Buffer contains status.
	UINT8 res[UAM_RES_LEN];                    //Buffer contains RES.
	UINT8 cnonce[UAM_CNONCE_LEN];       //Buffer contains cnonce.
}TCmmbUamAuthRsp;

/*sim type*/
/*
typedef enum
{
    UAM_SIM_TYPE_UNKNOWN=0,
    UAM_SIM_TYPE_2G = 2,
    UAM_SIM_TYPE_3G = 3,
} UAM_SIM_TYPE_ENUM;
*/
/*mtk type*/
typedef struct _TMtkStr
{
	UINT32 KI;                           //Buffer KI.
	UINT8 len;                           //Buffer length.
	UINT8 mtk[UAM_MTK_LEN];                    //Buffer mtk.
}TMtkStr;
/*
   Judge set which mtk, note: set all when first startservice
*/
typedef enum
{
    UAM_SET_ODD = 0,
    UAM_SET_EVEN = 1,
    UAM_SET_ALL =2
} UAM_SET_MTK_TYPE;

typedef struct _TCmmbUamMtkReq
{
	TMtkStr mtkodd;                    //Buffer .
	TMtkStr mtkeven;                    //Buffer.
        UAM_SET_MTK_TYPE mtkSetType;
}TCmmbUamMtkReq;
/*
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
*/

/* CmmbUam interface */
class CmmbUamPlugin:public RefBase
{
public:
	CmmbUamPlugin(const sp<ICmmbSp> spt,UAM_SIM_TYPE_ENUM sim_type)                             //get ICmmbSp pointer
	{
		spCmmb = spt;
		TurnOnUam(sim_type);
	}
	~CmmbUamPlugin()
	{
	       TurnOffUam();
	}
	BOOL TurnOnUam(UAM_SIM_TYPE_ENUM sim_type);
	BOOL TurnOffUam();
	
	BOOL GetCmmbSn(UINT8 cmmbsn[UAM_CMMBSN_LEN]);
	BOOL GetVersion (UINT8 version[UAM_USER_AUTH_VERION_LEN]);
       BOOL IsGBANeeded ();
	BOOL G2Authenticate (TCmmbUam2GAuthReq req, TCmmbUamAuthRsp &rsp);
	BOOL G3Authenticate (TCmmbUam3GAuthReq req, TCmmbUamAuthRsp &rsp);
	BOOL GetBTID (UINT8 btid[UAM_BTID_LEN]);
	BOOL SaveBTID (UINT8 btid[UAM_BTID_LEN],UINT8 ks[UAM_KS_LEN]);
	BOOL GetMRK (UINT8 nafid[UAM_NAF_ID_LEN], UINT8 impi[UAM_IMPI_LEN],UINT8 mrk[UAM_MRK_LEN]);
	BOOL SaveMSK (UINT8 msk[UAM_MSK_LEN], UINT8 verMsg[UAM_VER_MSG_LEN]);
	BOOL IsMSKValid (UINT8 domainid[UAM_DOMAIN_ID_LEN], UINT8 mskid[UAM_MSK_ID_LEN]);
      	BOOL GetControlWords (void *mtk,void* tt);
private:
	sp<ICmmbSp> spCmmb;
        CMutex g_uamMutex;		// mutex for API call		
        CMMBUAMFuncStruct* CMMBUamFunc;
};

}
#endif          //__MBBMS_UAM_SUPPORT__
#endif //MBBMS_UAM_Siano_H__


