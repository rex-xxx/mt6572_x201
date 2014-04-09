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

#ifndef _MTK_AF_H
#define _MTK_AF_H

#include "aaa_param.h"
#include "camera_custom_af.h"

/*******************************************************************************
*
********************************************************************************/
class MTKAF {
public:

    /////////////////////////////////////////////////////////////////////////
    //
    //   MTKAF () -
    //! \brief MTKAF module constructor.
    //!
    //
    /////////////////////////////////////////////////////////////////////////

    MTKAF();

    /////////////////////////////////////////////////////////////////////////
    //
    //   ~MTKAF () -
    //! \brief MTKAF module destructor.
    //!
    //
    /////////////////////////////////////////////////////////////////////////

    virtual ~MTKAF();

    /////////////////////////////////////////////////////////////////////////
    //
    //   initAF () -
    //! \brief initAF
    //!
    //
    /////////////////////////////////////////////////////////////////////////

	virtual MRESULT initAF(AF_INPUT_T &a_sAFInput, AF_OUTPUT_T &a_sAFOutput) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 triggerAF	() -
	//! \brief se  triggerAF
	//!
	//
	/////////////////////////////////////////////////////////////////////////

	virtual MRESULT triggerAF() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 stopAF  () -
	//! \brief se  stopAF
	//!
	//
	/////////////////////////////////////////////////////////////////////////

	virtual MRESULT stopAF() = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   handleAF() - AF Entry Point
    //
    //   input : AF_INPUT_T &a_sAFInput, AF_OUTPUT_T &a_sAFOutput
    //   output: error message
    /////////////////////////////////////////////////////////////////////////

    virtual MRESULT handleAF(AF_INPUT_T &a_sAFInput, AF_OUTPUT_T &a_sAFOutput) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getDebugInfo() -
	//!
	//!
	//
	/////////////////////////////////////////////////////////////////////////

	virtual void getDebugInfo(AF_DEBUG_INFO_T &a_DebugInfo) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 setAFFactor() -
	//!
	//!
	//
	/////////////////////////////////////////////////////////////////////////

	virtual void setAFFactor(const AF_PARAM_T &a_sAFParam, const AF_NVRAM_T &a_sAFNvram, const AF_STAT_CONFIG_T &a_sAFStatConfig) = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAFMode() - input AF Mode
    //
    //   input : enum AF mode
    //   output: error message
    /////////////////////////////////////////////////////////////////////////

    virtual MRESULT setAFMode(LIB3A_AF_MODE_T a_eAFMode) = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAFMeter() - input AF Meter
    //
    //   input : enum AF Meter
    //   output: error message
    /////////////////////////////////////////////////////////////////////////

    virtual MRESULT setAFMeter(LIB3A_AF_METER_T a_eAFMeter) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 setAFZone () -
	//!
	//!  input : enum AF zone
	//	output: error message
	/////////////////////////////////////////////////////////////////////////

	virtual MRESULT setAFZone(LIB3A_AF_ZONE_T a_eAFZone) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////////////

	virtual LIB3A_AF_MODE_T getAFMode() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////////////

	virtual LIB3A_AF_METER_T getAFMeter() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////////////

        virtual void setAFCoef(AF_COEF_T a_sAFCoef) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 isAFFinish() -
	//
	//	 input : none
	//	 output: 1 for finish, 0 for not yet
	/////////////////////////////////////////////////////////////////////////

	virtual MBOOL isAFFinish() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 setFDWin () -
	//!
	//!  input : FD Win info
	//	 output: error message
	/////////////////////////////////////////////////////////////////////////

	virtual MRESULT setFDWin(const FD_INFO_T a_sFDInfo) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 AFDrawRect () -
	//!
	//!  input :
	//	output: void
	/////////////////////////////////////////////////////////////////////////

	virtual void AFDrawRect(MUINT32 a_u4BuffAddr,MUINT32 a_u4Width,MUINT32 a_u4Height,MUINT32 a_u4OffsetW,MUINT32 a_u4OffsetH,MUINT8 a_uOri) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
    //   setAFMoveSpotPos () -                                                                      
    //!                                                                   
    //!  input :                                  
    //   output: 
    /////////////////////////////////////////////////////////////////////////                  
    
    virtual void setAFMoveSpotPos(MUINT32 a_u4Xoffset,MUINT32 a_u4Yoffset,MUINT32 a_u4Width,MUINT32 a_u4Height,MUINT32 a_u4OffsetW,MUINT32 a_u4OffsetH,MUINT8 a_uOri) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 setMFPos() - set MF Pos
	//
	//	 input : MINT32 a_i4Pos
	//	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual void setMFPos(MINT32 a_i4Pos) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
    //   clearAFWinResult() - clear AF Window result
    //
    //   input : none
    //   output: none
    /////////////////////////////////////////////////////////////////////////
    
    virtual void clearAFWinResult() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getAFWinResult() - output AF Window result
	//
	//	 input : AF_WIN_RESULT_T
	//	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual void getAFWinResult(AF_WIN_RESULT_T &a_sAFWinResult) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getAFBestPos() - get AF Best Pos
	//
	//	 input : none
	//	 output: MINT32
	/////////////////////////////////////////////////////////////////////////

	virtual MINT32 getAFBestPos() = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   setFocusDistanceRange() -
    //
    //   input : none
    //   output: MINT32
    /////////////////////////////////////////////////////////////////////////
    
    virtual MINT32 setFocusDistanceRange(MINT32 a_i4Distance_N, MINT32 a_i4Distance_M) = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   getFocusDistance() -
    //
    //   input : none
    //   output: MINT32
    /////////////////////////////////////////////////////////////////////////

    virtual MINT32 getFocusDistance(MINT32 &a_i4Near, MINT32 &a_i4Curr, MINT32 &a_i4Far) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getAFValue() - get AF Value
	//
	//	 input : none
	//	 output: MUINT32
	/////////////////////////////////////////////////////////////////////////

	virtual MUINT32 getAFValue() = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAFFullStep() -
    //
    //   input : MINT32 a_i4Step
    //   output: none
    /////////////////////////////////////////////////////////////////////////

    virtual void setAFFullStep(MINT32 a_i4Step) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getAFPos() - get AF Pos
	//
	//	 input : none
	//	 output: MINT32
	/////////////////////////////////////////////////////////////////////////

	virtual MINT32 getAFPos() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 pauseFocus() - pauseFocus
	//
	//	 input : none
	// 	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual void pauseFocus() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 isFocused() - return Focus OK of NG
	//
	//	 input : none
	// 	 output: MBOOL
	/////////////////////////////////////////////////////////////////////////

	virtual MBOOL isFocused() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 resetFocus() - reset Focus Position
	//
	//	 input : none
	//	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual void resetFocus() = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 setFocusAreas() - setFocusAreas
	//
	//	 input : none
	//	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual void setFocusAreas(MINT32 a_i4Cnt, AREA_T *a_psFocusArea) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getFocusAreas() - getFocusAreas
	//
	//	 input : none
	//	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual void getFocusAreas(MINT32 &a_i4Cnt, AREA_T **a_psFocusArea) = 0;

	/////////////////////////////////////////////////////////////////////////
	//
	//	 getMaxNumFocusAreas() - getMaxNumFocusAreas
	//
	//	 input : none
	//	 output: none
	/////////////////////////////////////////////////////////////////////////

	virtual MINT32 getMaxNumFocusAreas() = 0;

    virtual void setZSDMode(MBOOL ZSDFlag) = 0;
    virtual void isAEStable(MBOOL a_bAEStable) = 0;

    virtual void setISO(MINT32 a_i4ISO) = 0;

private:

};

#endif

