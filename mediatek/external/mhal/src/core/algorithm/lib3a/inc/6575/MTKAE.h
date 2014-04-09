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

#ifndef _MTK_AE_H_
#define _MTK_AE_H_

#include "aaa_param.h"

/*******************************************************************************
*   MTK AE Class -
********************************************************************************/
class MTKAE {
public:
    /////////////////////////////////////////////////////////////////////////
    //
    //   MTKAE () -
    //! \brief MTK3A module constructor.
    //!
    //
    /////////////////////////////////////////////////////////////////////////
    MTKAE();

    /////////////////////////////////////////////////////////////////////////
    //
    //   MTKAE () -
    //! \brief MTK3A module constructor.
    //!
    //
    /////////////////////////////////////////////////////////////////////////
    virtual ~MTKAE();

    /////////////////////////////////////////////////////////////////////////
    //
    //   eanble AE () -
    //! \brief see anble AE
    //!
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT enableAE();

    /////////////////////////////////////////////////////////////////////////
    //
    //   disableAE AE () -
    //! \brief see disableAE
    //!
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT disableAE();

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEParameter() -
    //!
    //!
    //
    /////////////////////////////////////////////////////////////////////////
    virtual void setAEParameter(struct_AE_Para &a_strAEPara);

    /////////////////////////////////////////////////////////////////////////
    //
    //   getDebugInfo() -
    //!
    //!
    //
    /////////////////////////////////////////////////////////////////////////
//  virtual void getDebugInfo(strAEDebugTag &a_DebugInfo);
     virtual void getDebugInfo(AE_DEBUG_INFO_T &a_DebugInfo);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEMeteringMode () -
    //!
    //!  input : enum ae metering mode
    //   output: error message
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setAEMeteringMode(LIB3A_AE_METERING_MODE_T a_eMeteringMode);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEMode () -
    //!
    //!  input : enum AE mode
    //   output: error message
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setAEMode(LIB3A_AE_MODE_T a_eAEMode);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAvValue () -
    //!
    //!  input : enum Aperture value (Fno.)
    //   output: error message
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setAvValue(eApetureValue  a_eAV);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setTvValue () -
    //!
    //!  input : enum TV value (exposure time .)
    //   output: error message
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setTvValue(eTimeValue a_eTV);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setIsoSpeed() -
    //!
    //!  input : enum ISO speed
    //   output: error message
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setIsoSpeed(LIB3A_AE_ISO_SPEED_T  a_eISO);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setStrobeMode() -
    //!
    //!  input : enum strobe mode
    //   output: error message
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setStrobeMode(LIB3A_AE_STROBE_MODE_T  a_eStrobeMode);

    /////////////////////////////////////////////////////////////////////////
    //
    //   enableRedEye() -
    //!
    //!  input :  enum Red eye
    //   output: void
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setRedEye(LIB3A_AE_REDEYE_MODE_T a_eRedEye );

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEFlickerMode() -
    //!
    //!  input :  enum flicker
    //   output: void
    /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAEFlickerMode(LIB3A_AE_FLICKER_MODE_T a_eAEFlickerMode);

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEFlickerAutoMode() -
    //!
    //!  input :  enum flicker
    //   output: void
    /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAEFlickerAutoMode(LIB3A_AE_FLICKER_AUTO_MODE_T a_eAEFlickerAutoMode);

    /////////////////////////////////////////////////////////////////////////
    //
    //	 setAEPreviewMode () -
    //! \brief set AE preview mode
    //!
    //! \param [in] a_eAEPreviewMode  enum preview mode setting
    //!
    //!
    //!
    //! \return 0 if success, 1 if failed.
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setAEPreviewMode(LIB3A_AE_PREVIEW_MODE_T a_eAEPreviewMode);
    
    /////////////////////////////////////////////////////////////////////////
    //
    //   getAEFlickerAutoMode() -
    //!
    //!  input :  enum flicker
    //   output: void
    /////////////////////////////////////////////////////////////////////////
     virtual MRESULT getAEFlickerAutoMode(LIB3A_AE_FLICKER_AUTO_MODE_T *a_eAEFlickerAutoMode);

    /////////////////////////////////////////////////////////////////////////
    //
    //	 getAEPreviewMode() -
    //!
    //!  input :  none
    //	output: preview mode value
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT getAEPreviewMode(LIB3A_AE_PREVIEW_MODE_T *a_eAEPreviewMode);

     /////////////////////////////////////////////////////////////////////////
     //
     //   setAEFrameRateMode() -
     //!
     //!  input :  enum frame rate
     //   output: void
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAEFrameRateMode(MINT32 a_eAEFrameRateMode);

     /////////////////////////////////////////////////////////////////////////
     //
     //   setAEMaxFrameRate() -
     //!
     //!  input :  max frame rate value
     //   output: void
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAEMaxFrameRate(MINT32 a_eAEMaxFrameRate);

     /////////////////////////////////////////////////////////////////////////
     //
     //   setAEMinFrameRate() -
     //!
     //!  input :  min frame rate value
     //   output: void
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAEMinFrameRate(MINT32 a_eAEMinFrameRate);

     /////////////////////////////////////////////////////////////////////////
     //
     //   getAEMaxFrameRate() -
     //!
     //!  input :  none
     //   output: max frame rate value
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT getAEMaxFrameRate(MINT32 *a_eAEMaxFrameRate);

     /////////////////////////////////////////////////////////////////////////
     //
     //   getAEMinFrameRate() -
     //!
     //!  input :  none
     //   output: min frame rate value
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT getAEMinFrameRate(MINT32 *a_eAEMinFrameRate);

     /////////////////////////////////////////////////////////////////////////
     //
     //   getAESupportFrameRateNum() -
     //!
     //!  input :  none
     //   output: support frame rate num
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT getAESupportFrameRateNum(MINT32 *a_eAEFrameRateNum);

     /////////////////////////////////////////////////////////////////////////
     //
     //   getAESupportFrameRateRange() -
     //!
     //!  input :  none
     //   output: support frame rate range
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT getAESupportFrameRateRange(MINT32 *a_eAEFrameRateRange);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getSoftwareHist() -
     //  return Software histogram data
     //!
     //!  input :  void
     //  output:  a_pu4Histgram , u4 256  buffer
     /////////////////////////////////////////////////////////////////////////
     virtual void getSoftwareHist(MUINT32* a_pu4Histgram);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getAECWValue() -
     //
     // input :  void
     // output:  AE Central weighting value
     /////////////////////////////////////////////////////////////////////////
     virtual MUINT32 getAECwValue(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getAECondition() -
     //
     // input :  void
     // output:  getAECondition
     /////////////////////////////////////////////////////////////////////////
     virtual MUINT32 getAECondition(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //   getAEFaceDiffIndex() -
     //
     //  input :  void
     //  output:  getAEFaceDiffIndex
     /////////////////////////////////////////////////////////////////////////
     virtual MINT16 getAEFaceDiffIndex(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getAEBlockVaule() -
     //
     // input :  void
     // output:  a_pAEBlockResult , buffer to store AE nxn block result
     /////////////////////////////////////////////////////////////////////////
     virtual void  getAEBlockVaule(MUINT32 *a_pAEBlockResult);

     /////////////////////////////////////////////////////////////////////////
     //
     //  setAESatisticBufferAddr() -
     //  set AE statistic buffer addresss
     //!
     //!  input :  AE statistic buffer
     //  output:  void
     /////////////////////////////////////////////////////////////////////////
//     virtual void setAESatisticBufferAddr(void* a_pAEBuffer, void* a_AWBBuffer);
     virtual void setAESatisticBufferAddr(void* a_pAEBuffer, void* a_FlareBuffer, void* a_AEHisBuffer, MUINT32 u4BlockCnt);

     /////////////////////////////////////////////////////////////////////////
     //
     //  handleAE()
     //
     //!  Handle AE
     //  input : None
     //  output : Error code
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT handleAE(strAEInput* a_Input,strAEOutput* a_Output);

     /////////////////////////////////////////////////////////////////////////
     //
     //  handlePreFlash()
     //
     //!  Handle preflah
     //  input : None
     //  output : Error code
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT handlePreFlash(strAEOutput &a_pStoEvSetting,strPreFlashExtraInfo &a_pStoExInfo,ePreFlashOState &a_pPFOState);

     /////////////////////////////////////////////////////////////////////////
     //
     //   initAE () -
     //! \brief Initialize AE module, set Initial exposure time, Iris ,Gain
     //!
     //!
     //!
     //!
     //!
     //! \return 0 if success, 1 if failed.
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT initAE(strAEOutput *a_aeout);

     /////////////////////////////////////////////////////////////////////////
     //
     //  setEVCompensate () -
     //! \brief set AE EV Compensateion value
     //!
     //! \param [in] a_eEVComp  enum EV compensation setting
     //!
     //!
     //!
     //! \return 0 if success, 1 if failed.
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setEVCompensate(LIB3A_AE_EVCOMP_T a_eEVComp);

     /////////////////////////////////////////////////////////////////////////
     //
     //  setAEConvergeSpeed () -
     //! \brief set AE converge speed
     //!
     //! \param [in] BrightConSpeed -AE converge spped
     //!
     //!
     //!
     //! \return 0 if success, 1 if failed.
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAEConvergeSpeed(MUINT32 BrightConSpeed, MUINT32 DarkConSpeed);

     /////////////////////////////////////////////////////////////////////////
     //
     //  switchAETable () -
     //! \brief run time swithc AE table
     //!
     //! \param [in]  strAETable a_AeTable  - AE table structure
     //!
     //!
     //! \return 0 if success, 1 if failed.
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT switchAETable( strAETable &a_AeTable);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getEVValue ()-
     //! \brief GET EV value in current AE condition
     //!
     //! \param void
     //!
     //!
     //! \return EV value 10base
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MUINT32 getEVValue(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getAETableIndex ()-
     //! \brief Get index in AE table
     //! \param none
     //!
     //!
     //! \return index
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MUINT32 getAETableIndex( ) {return S_AE_OK;}

     /////////////////////////////////////////////////////////////////////////
     //
     //  getAEIndexEVSetting ()-
     //! \brief get EV setting according table index
     //! \param [in] u4indx AE table index
     //!     [out] a_aeout EV setting
     //!
     //! \return MRESULT
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT getAEIndexEVSetting(MUINT32 u4idx,strAEOutput *a_aeout);

     /////////////////////////////////////////////////////////////////////////
     //
     //  turnOnAE ()-
     //! \brief Test Funtion Turn on AE
     //!
     //! \param void
     //!
     //!
     //! \return void
     //
     /////////////////////////////////////////////////////////////////////////
     virtual void turnOnAE(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //  turnOffAE ()-
     //! \brief Test Function Turn off AE
     //!
     //! \param void
     //!
     //!
     //! \return void
     //
     /////////////////////////////////////////////////////////////////////////
     virtual void turnOffAE(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //  set3AFactor
     //! \brief Pass All 3A factor to sub module
     //!
     // param in  struct_3A_Factor / NVRam data / 3A mode
     //!
     //! \return void
     //
     /////////////////////////////////////////////////////////////////////////
//     virtual void set3AFactor(const AAA_PARAM_T *a_p3AFactor);
//     virtual void set3AFactor(const AAA_PARAM_T *a_p3AFactor, const AE_NVRAM_T *a_pAENVramFactor, AAA_CMD_SET_T *a_p3AMode);
	 virtual void set3AFactor(const AAA_PARAM_T *a_p3AFactor, const AE_NVRAM_T *a_pAENVramFactor, AE_STAT_CONFIG_T *a_p3AConfig, AAA_CMD_SET_T *a_p3AMode);

     /////////////////////////////////////////////////////////////////////////
     //
     //!    getFlare(void);
     //! \brief get flare level in 8 bit domain
     //!
     //! \param[none]
     //!
     //! \return flare
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MUINT32 getFlare(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //!  getBVvalue(void);
     //! \brief get BV value
     //!
     //! \param[none]
     //!
     //! \return BV
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MINT32 getBVvalue(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //!    getAEPlineEVvalue(void);
     //! \brief get AE Pline EV value
     //!
     //! \param[none]
     //!
     //! \return AE Pline EV
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MINT32 getAEPlineEVvalue(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //!   getFlareGainOffset();
     //! \brief get flare gain and offset in 8 bit domain
     //!
     //! \param in AE mode and flare structure
     //!
     //! \return none
     //
     /////////////////////////////////////////////////////////////////////////
     virtual void getFlareGainOffset(eAESTATE eAEMode, strFlareCFG *a_FlareGainOffset);

     /////////////////////////////////////////////////////////////////////////
     //
     //  setAETable ()-
     //! \brief set AE table
     //!
     //! \param a_AETableID	AE Table ID
     //!
     //!
     //! \return 0 if success, errcode if failed.
     //
     ////////////////////////////////////////////////////////////////////////
     virtual MRESULT setAETable(eAETableID a_AETableID);

     /////////////////////////////////////////////////////////////////////////
     //
     //  getAEMaxNumMeteringAreas ()-
     //! \brief get AE max number of metering area
     //!
     //! \param null
     //!
     //!
     //! \return the numbers.
     //
     ////////////////////////////////////////////////////////////////////////
     virtual MINT32 getAEMaxNumMeteringAreas() = 0;
     
     /////////////////////////////////////////////////////////////////////////
     //
     //  getAEMeteringAreas ()-
     //! \brief get AE metering area information
     //!
     //! \param structure of area
     //!
     //!
     //! \return 0 if success, errcode if failed.
     //
     ////////////////////////////////////////////////////////////////////////
     virtual MVOID getAEMeteringAreas(MINT32 &a_i4Cnt, AREA_T **a_psAEArea) = 0;

     /////////////////////////////////////////////////////////////////////////
     //
     //  setAEMeteringAreas ()-
     //! \brief set AE metering area information
     //!
     //! \param structure of area
     //!
     //!
     //! \return NULL.
     //
     ////////////////////////////////////////////////////////////////////////     
     virtual MVOID setAEMeteringAreas(MINT32 a_i4Cnt, AREA_T const *a_psAEArea) = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   modifyAEWindowSetting
    //! \brief  modify the AE window information
    //!
    //! \param
    //!
    //! \return void.
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT modifyAEWindowSetting(AE_STAT_CONFIG_T &strAEStatCFG, EZOOM_WIN_T &sEZoomSize, FD_INFO_T &sFaceWinInfo, MBOOL *bAECFGUpdate, MBOOL *bFaceAEWinUpdate) = 0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   isAEMeteringWinEnable() -
    //! \brief AE meterwindow enable or not.
    //!
    //! \param NONE
    //!
    //!
    //! \return MBOOL
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MBOOL isAEMeteringWinEnable(void) {return FALSE;}

    /////////////////////////////////////////////////////////////////////////
    //
    //  lockAE () -
    //! \brief Lock AE state
    //!
    //!
    //! \return 0 
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MVOID lockAE(BOOL bLockAE) = 0;

     /////////////////////////////////////////////////////////////////////////
     //
     //  testAE ()-
     //! \brief set AE table
     //!
     //! \param a_AETableID	AE Table ID
     //!
     //!
     //! \return 0 if success, errcode if failed.
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MRESULT testAE(MUINT32 testID) {return S_AE_CANNOT_FIND_TABLE;}

     /////////////////////////////////////////////////////////////////////////
     //
     //  isAFLampAutoOn()-
     //! \brief need AFLampAutoOn or not.
     //!
     //! \param NONE
     //!
     //!
     //! \return MBOOL
     //
     /////////////////////////////////////////////////////////////////////////
	 virtual MBOOL isAFLampAutoOn(void);

     /////////////////////////////////////////////////////////////////////////
     //
     //  isAEFlashOn()-
     //! \brief need preflash metering or not.
     //!
     //! \param NONE
     //!
     //!
     //! \return MBOOL
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MBOOL isAEFlashOn( );

     /////////////////////////////////////////////////////////////////////////
     //
     //  getPreFlashInfo()-
     //! \brief get preflash stat. buffer and target level.
     //!
     //! \param a_pInfo
     //!
     //!
     //! \return MBOOL
     //
     /////////////////////////////////////////////////////////////////////////
     virtual MUINT32 getPreFlashInfo(strFlashAWBInfo *a_pInfo);

     /////////////////////////////////////////////////////////////////////////
     //
     //  setFaceROI ()-
     //! \brief set Face ROI
     //!
     //! \param a_FaceROI face ROI base on window information
     //!
     //!
     //! \return MRESULT
     //
     /////////////////////////////////////////////////////////////////////////
// To be implement later
//    virtual MRESULT setFaceROI(srtNu3AFaceRoi &a_FaceROI,MBOOL bWithFace);

    /////////////////////////////////////////////////////////////////////////
    //
    //  enableFaceAE ()-
    //! \brief enable Face AE
    //!
    //! \param a_EnableFaceAE face AE
    //!
    //! \return MRESULT
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT enableFaceAE(MBOOL a_EnableFaceAE);

    /////////////////////////////////////////////////////////////////////////
    //
    //   getAEInterInfo
    //! \brief get AE internal information
    //!
    //! \param [out] a_AEInterInfo AE internal information
    //!
    //! \return MRESULT
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT getAEInterInfo(strAEInterInfo  *a_AEInterInfo);

    /////////////////////////////////////////////////////////////////////////
    //
    //  enableAEDebugInfo();
    //! \brief enable AE debug information print out CLI
    //!
    //! \param [in]  MBOOL enable
    //!
    //! \return MBOOL
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MVOID enableAEDebugInfo(MBOOL enable) { }

    /////////////////////////////////////////////////////////////////////////
    //
    //  setAECondition();
    //! \brief set AE condition
    //!
    //! \param [in]  strSetAEConditionIn , structure of AE control flag
    //!
    //! \return MBOOL
    //
    /////////////////////////////////////////////////////////////////////////
    typedef struct
    {
        MBOOL    bBlackLight;       //enable back light detector
        MBOOL    bHistStretch;      //enable histogram stretch
        MBOOL    bAntiOverExposure; //enable anti over exposure
        MBOOL    bTimeLPF;          //enable time domain LPF
        MBOOL    bSaturationCheck;  //if toward high saturation scene , then reduce AE target
    }strSetAEConditionIn;

    virtual MVOID setAECondition(strSetAEConditionIn &a_Condition) { }

    /////////////////////////////////////////////////////////////////////////
    //
    //   getFaceYforLCE
    //! \brief get Face Y for LCE (after gamma)
    //!
    //! \param [in]strFaceYforLCEin
    //!    [out] FaceY;
    //!
    //! \return MRESULT
    //
    /////////////////////////////////////////////////////////////////////////
// To be implement later
//    virtual MRESULT getFaceYforLCE(strFaceYforLCEin* strIn, MUINT32 *FaceY)=0;

    /////////////////////////////////////////////////////////////////////////
    //
    //   getLCEIndicator
    //! \brief AE to decide turn on LCE or not
    //!
    //! \param
    //!
    //! \return MBOOL
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MBOOL getLCEIndicator( ) {return S_AE_OK;}

    /////////////////////////////////////////////////////////////////////////
    //
    //  getAESetting
    //! \brief get AE setting in different state
    //!
    //! \param
    //!
    //! \return MBOOL
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT getAESetting(strAEOutput* a_Output,eAESTATE  a_AeState = AE_STATE_CAPTURE) {return E_AE_PARAMETER_ERROR;}


    /////////////////////////////////////////////////////////////////////////
    //
    //  switchCapureDiffEVState () -
    //! \brief switch ae  to "different EV capture" state
    //!
    //!
    //! \return 0 if success, 1 if failed.
    //
    /////////////////////////////////////////////////////////////////////////
    virtual void switchCapureDiffEVState(strAEOutput *aeoutput, MINT8 iDiffIdx) { }

    /////////////////////////////////////////////////////////////////////////
    //
    //   switchSensorExposureGain () -
    // \brief switch sensor exposure time and gain to fit sensor requirement.
    //
    //
    // \return 0 if success, 1 if failed.
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT switchSensorExposureGain(AE_EXP_GAIN_MODIFY_T &rInputData, AE_EXP_GAIN_MODIFY_T &rOutputData) {return S_AE_OK;}

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEMeteringModeStatus() -
    //! \brief set AE metering mode to let AE understand the current mode.
    //!
    //! \param [in] a_i4AEMeteringModeStatus 0 : video preview, 1:video recording, 2:camera preview
    //!
    //!
    //!
    //! \return 0 if success, 1 if failed.
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setAEMeteringModeStatus(MINT32 a_i4AEMeteringModeStatus) {return S_AE_OK;}

    /////////////////////////////////////////////////////////////////////////
    //
    //   setAEStrobeModeStatus() -
    //! \brief set AE strobe mode status to let AE understand the strobe mode.
    //!
    //! \param [in] a_i4AEMeteringModeStatus 0 : preview mode, 1 : pre-capture mode
    //!
    //!
    //!
    //! \return 0 if success, 1 if failed.
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MRESULT setAEStrobeModeStatus(MINT32 a_i4AEStrobeModeStatus) {return S_AE_OK;}

    /////////////////////////////////////////////////////////////////////////
    //
    //   gethandleAEIdx() -
    //! \brief get handle AE wait index.
    //!
    //! \param NONE
    //!
    //!
    //! \return MBOOL
    //
    /////////////////////////////////////////////////////////////////////////
    virtual MUINT32 gethandleAEIdx() {return S_AE_OK;}

    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    //1   Protected
    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    protected:

    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    //1   Private
    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    private:

};

#endif
// end of file
