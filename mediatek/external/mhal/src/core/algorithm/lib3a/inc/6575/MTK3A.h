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

/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#ifndef _MTK_3A_H
#define _MTK_3A_H

#include "aaa_param.h"
#include "camera_custom_af.h"

/*******************************************************************************
*
********************************************************************************/
class MTK3A {
public:
    MTK3A();
    virtual ~MTK3A();
    virtual MRESULT send3ACmd(MINT32 a_i4CmdID, MINT32 a_i4CmdParam); // Send 3A UI command
    virtual MRESULT get3ACmd(MINT32 a_i4CmdID, MINT32 *a_i4CmdParam); // get 3A UI command
    virtual MRESULT init3A(AAA_INIT_INPUT_PARAM_T &a_r3AInitInput, 
                           AAA_OUTPUT_PARAM_T &a_r3AOutput,
                           AAA_STAT_CONFIG_T &a_r3AStatConfig); // Init 3A module
    virtual MRESULT deinit3A(); // De-init 3A module
    virtual MRESULT handle3A(AAA_FRAME_INPUT_PARAM_T &a_r3AInput, AAA_OUTPUT_PARAM_T &a_r3AOutput); // Perform 3A algorithm
    virtual MRESULT set3AState(AAA_STATE_T a_e3AState); // Set 3A state
    virtual MRESULT	get3ADebugInfo(AAA_DEBUG_INFO_T &a_r3ADebugInfo); // Get 3A debug info    
    virtual void    setAFMoveSpotPos(MUINT32 a_u4Xoffset,MUINT32 a_u4Yoffset,MUINT32 a_u4Width,MUINT32 a_u4Height,MUINT32 a_u4OffsetW,MUINT32 a_u4OffsetH,MUINT8 a_uOri);        
    virtual void    setMFPos(MINT32 a_i4Pos);
    virtual void    getAFWinResult(AF_WIN_RESULT_T &a_sAFWinResult);	
    virtual MINT32  getAFBestPos();
    virtual MINT32  setFocusDistanceRange(MINT32 a_i4Distance_N, MINT32 a_i4Distance_M);    
    virtual MINT32  getFocusDistance(MINT32 &a_i4Near, MINT32 &a_i4Curr, MINT32 &a_i4Far);
    virtual MUINT32 getAFValue();
    virtual void    setAFFullStep(MINT32 a_i4Step);
    virtual void    setAFCoef(AF_COEF_T a_sAFCoef);
    virtual MBOOL   isAFFinish();
	virtual void    setDoFocusState();    
	virtual void    pauseFocus();
	virtual MBOOL   isFocused();
	virtual void    resetFocus();	
	virtual void    setFocusAreas(MINT32 a_i4Cnt, AREA_T *a_psFocusArea);
	virtual void    getFocusAreas(MINT32 &a_i4Cnt, AREA_T **a_psFocusArea);
    virtual MINT32  getMaxNumFocusAreas();
    virtual void    AFDrawRect(MUINT32 a_u4BuffAddr,MUINT32 a_u4Width,MUINT32 a_u4Height,MUINT32 a_u4OffsetW,MUINT32 a_u4OffsetH,MUINT8 a_uOri);
    virtual void    handleAF(AAA_FRAME_INPUT_PARAM_T &a_r3AInput, AAA_OUTPUT_PARAM_T &a_r3AOutput);
    virtual MRESULT enableAE();
    virtual MRESULT disableAE();
    virtual MBOOL isAEEnable() = 0;
    virtual MRESULT lockAE() = 0;
    virtual MRESULT unlockAE() = 0;
    virtual MBOOL isAELocked() = 0;
    virtual MBOOL isStrobeOn() = 0;
    virtual MRESULT enableAF();
    virtual MRESULT disableAF();
	virtual MBOOL isAFEnable() = 0;
    virtual MRESULT enableAWB();
    virtual MRESULT disableAWB();
    virtual MBOOL isAWBEnable() = 0;
    virtual MRESULT lockAWB() = 0;
    virtual MRESULT unlockAWB() = 0;
    virtual MBOOL isAWBLocked() = 0;
    virtual MINT32 getSceneLV() = 0;
    virtual MINT32 getSceneBV() = 0;
    virtual MINT32 getAEPlineEV() = 0;  
    virtual MINT32 getCCT() = 0;
	virtual MRESULT lockHalfPushAEAWB() = 0;
	virtual MRESULT unlockHalfPushAEAWB() = 0;
	virtual MRESULT resetHalfPushState() = 0;
	virtual MRESULT applyAWBParam(AWB_NVRAM_T &a_rAWBNVRAM, AWB_STAT_CONFIG_T &a_rAWBStatConfig) = 0;
    virtual MRESULT applyAEParam(AE_NVRAM_T &a_rAENVRAM) = 0;    
    virtual MRESULT applyAFParam(NVRAM_LENS_PARA_STRUCT &a_rAFNVRAM) = 0;
    virtual MRESULT get3AEXIFInfo(AAA_EXIF_INFO_T *a_p3AEXIFInfo) = 0;
    virtual MRESULT getAEModeSetting(AE_MODE_CFG_T &a_rAEOutput, AAA_STATE_T  a_3AState) = 0;
    virtual MRESULT getASDInfo(AAA_ASD_INFO_T &a_ASDInfo) = 0;
    virtual void  setAFLampInfo(MBOOL a_bAFLampOn, MBOOL a_bAFLampIsAutoMode);
    virtual MBOOL getAFLampIsAutoOn();
    virtual MRESULT getAWBLightProb(AWB_LIGHT_PROBABILITY_T &a_rLightProb) = 0;
    virtual MRESULT switchSensorExposureGain(AE_EXP_GAIN_MODIFY_T &rInputData, AE_EXP_GAIN_MODIFY_T &rOutputData) = 0;
    virtual MINT32 getAEMaxNumMeteringAreas() = 0;
    virtual MVOID getAEMeteringAreas(MINT32 &a_i4Cnt, AREA_T **a_psAEArea) = 0;
    virtual MVOID setAEMeteringAreas(MINT32 a_i4Cnt, AREA_T const *a_psAEArea) = 0;
    virtual MRESULT set3AMeteringModeStatus(MINT32 a_i4AEMeteringModeStatus) = 0;

    //cotta : add for strobe protection
	virtual void setPrevMFEndTime(MUINT32 uNewTime) = 0;	
	virtual void setStrobeProtectionIntv(MUINT32 uNewTime) = 0;	
	virtual MUINT32 getPrevMFEndTime() = 0;		
	virtual MUINT32 getStrobeProtectionIntv() = 0;
    virtual void setStrobeWDTValue(MUINT32 timeMS) = 0; //added for WDT customize
    virtual void setZSDMode(MBOOL ZSDFlag)=0;
	virtual void setIsBurstMode(MINT32 isBurst)=0;

private:
    
};

extern "C" MTK3A* openLib3A();

#endif

