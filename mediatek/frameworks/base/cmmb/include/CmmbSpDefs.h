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

#ifndef CMMB_SP_DEFS_H
#define CMMB_SP_DEFS_H

// CmmbSpDefs.h
// for stagefright and JNI use

#include "utils/Log.h"
#include "CmmbErrorCode.h"

// Control word and salt maximum sizes for F/W descrabmler 
#define CMMB_CA_CONTROL_WORD_SIZE		16
#define CMMB_CA_SALT_SIZE				8

#define CMMB_FILE_PATH_MAX				260
#define INVALID_CA_SYS_ID				0xFFFF

// CMMB notification event IDs
#define CMMB_BITSTREAM_ARRIVAL			100
#define CMMB_SIGNAL_CHANGED				101
#define CMMB_EB_MSG						102
#define CMMB_SERVICE_CHANGED			103
#define CMMB_MTK_DECRYPT_REQ			104
#define CMMB_INITIALIZE_OK				105
#define CMMB_INITIALIZE_FAIL			106
#define CMMB_UAM_ERROR					107
#define CMMB_CHANNEL_FOUND				108
#define CMMB_AUTO_SCAN_DONE				109
#define CMMB_ESG_READY					110
#define CMMB_ESG_STOP                   111
#define CMMB_ESG_NO_UPDATE                   112
#define CMMB_DATA_UPDATE                   113
#define CMMB_UAM_SET_CW                   114
#define CMMB_MSK_UPDATE					115

// CMMB property keys
#define CMMB_SIGNAL_QUALITY             200
#define CMMB_MODEM_STATE                202
#define CMMB_SYSTEM_TIME                203
#define CMMB_SIGNAL_FREQUENCY           201
#define CMMB_SIGNAL_STRENGTH            204
#define CMMB_SNR_COUNT                  205
#define CMMB_BER_COUNT                  206
#define CMMB_INBANDPWR                  207
#define CMMB_CARRIEROFFSET              208

// CMMB EB
#define CMMB_EB_MAX_TEXT_BUF_SIZE ((4095) * sizeof(char))
#define CMMB_EB_MAX_TEXT_SIZE ((4096) * sizeof(char))

enum ECmmbUamSimType
{
	CMMB_UAM_SIM_TYPE_UNKNOWN = 0,
	CMMB_UAM_SIM_TYPE_2G = 2,
	CMMB_UAM_SIM_TYPE_3G = 3 	
};

struct TCmmbCaCw
{
	unsigned int	 id; 
	unsigned char	 cw[CMMB_CA_CONTROL_WORD_SIZE]; 
};

struct TCmmbCaCwPair
{
	TCmmbCaCw 	odd; 
	TCmmbCaCw 	even; 
};


typedef struct cmmb_eb_msg_for_ap
{
	unsigned char	 net_level;
	unsigned short	 net_id;
	unsigned short	 msg_id;
	unsigned char	 msg_type;
	unsigned char	 msg_level;
	unsigned short	 msg_dat;
	unsigned int  msg_time;
	unsigned short	 msg_len;
	char msg[CMMB_EB_MAX_TEXT_SIZE];
}cmmb_eb_msg_for_ap;

typedef struct
{
	unsigned int 	domainIDLen;
	unsigned char	domainID[32];
	unsigned int	mskIDLen;
	unsigned char	mskID[32];
	unsigned int 	UAMError;
}mbbms_mtk_MSKID_struct;

#endif // CMMB_SP_DEFS_H
