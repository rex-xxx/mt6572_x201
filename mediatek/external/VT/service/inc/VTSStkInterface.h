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

#ifndef VIDEO_TELEPHONE_STK_IF_H
#define VIDEO_TELEPHONE_STK_IF_H

#ifdef __cplusplus
extern "C" {
#endif

#include "VTSUtils.h"
#include "med_vt_struct.h"
#include <pthread.h>


	
	/*VtStk_Status VtStk_CallActivateReq(kal_uint8 call_id);
	VtStk_Status VtStk_CallDeactivateReq(kal_uint8 call_id, kal_uint8 end_type);
	VtStk_Status VtStk_UserInputInd(kal_char* data, kal_uint8 size);
	VtStk_Status VtStk_mediaLoopbackActReq(vt_mdi_loopback_mode_enum mode_option);
	VtStk_Status VtStk_mediaLoopbackDeactReq(void);
	VtStk_Status VtStk_VideoSetPeerQuality(vt_vq_option_enum choice);
	VtStk_Status VtStk_VideoReqFastUpdate(void);
	void VtStk_VideoLoopbackTx(IN kal_uint8 *data,IN kal_uint32 size);
	void VtStk_VideoLoopbackRx(IN kal_uint8 *data,IN kal_uint32 size);			
	kal_int32 VtStk_VideoGetDecConfig(IN kal_int32 type,IN kal_uint8 * buffer,INOUT kal_uint32 * size);
	VtStk_Status VtStk_CallActivateCnf(void* para);	
	VtStk_Status VtStk_CallDeactivateCnf(void* para);			
	void VtStk_CallDiscInd(void * para);		
	VtStk_Status VtStk_mediaChannelConfig(void * para);
	void VtStk_AudioPutRxPacket(IN kal_uint8 *pBuffer,IN kal_uint32 size,IN kal_bool bAnyError);
	void VtStk_AudioSetMaxSkew(IN kal_uint32 skew);
	void VtStk_VideoPutRxPacket(IN kal_uint8 *pBuffer,IN kal_uint32 size,IN kal_bool is_any_error);			
	void VtStk_VideoSetLocalQuality(void * para);	
	void VtStk_VideoSetH263Resolution(void * para);	
	void VtStk_VideoEncFastUpdate();
	#if defined VTS_TEST
		void vtStk_AudioPutTxPacket(IN kal_uint8* data,IN kal_uint32 size,IN kal_uint8  session_id);
		void vtStk_VideoPutTxPacket(IN kal_uint8* data,IN kal_uint32 size,IN kal_uint8  session_id);
	#endif*/

	
#ifdef __cplusplus
}
#endif

#endif
