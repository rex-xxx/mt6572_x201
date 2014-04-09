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


#ifndef _MTK_FACEBEAUTY_H
#define _MTK_FACEBEAUTY_H

#include "MTKFaceBeautyType.h"
#include "MTKFaceBeautyErrCode.h"

typedef enum DRVFaceBeautyObject_s {
    DRV_FACEBEAUTY_OBJ_NONE = 0,
    DRV_FACEBEAUTY_OBJ_SW,
    DRV_FACEBEAUTY_OBJ_SW_NEON,    
    DRV_FACEBEAUTY_OBJ_UNKNOWN = 0xFF,
} DrvFaceBeautyObject_e;


typedef enum
{
    MTKFACEBEAUTY_STATE_STANDBY=0,	                           // After create Obj or Reset
    MTKFACEBEAUTY_STATE_READY_TO_SET_ALPHA_MAP_PROC_INFO,      // After call FaceBeautyInit()
    MTKFACEBEAUTY_STATE_READY_TO_ALPHA_MAP,			           // After set proc info with MTKFACEBEAUTY_CTRL_ALPHA_MAP
    MTKFACEBEAUTY_STATE_READY_TO_SET_TEXTURE_BLEND_PROC_INFO,  // After get the result with MTKFACEBEAUTY_CTRL_ALPHA_MAP
    MTKFACEBEAUTY_STATE_READY_TO_TEXTURE_BLEND,			       // After set proc info with MTKFACEBEAUTY_CTRL_BLEND_TEXTURE_IMG
    MTKFACEBEAUTY_STATE_READY_TO_SET_COLOR_BLEND_PROC_INFO,    // After get the result with MTKFACEBEAUTY_CTRL_BLEND_TEXTURE_IMG
    MTKFACEBEAUTY_STATE_READY_TO_COLOR_BLEND,	               // After set proc info with MTKFACEBEAUTY_CTRL_BLEND_COLOR_IMG
    MTKFACEBEAUTY_STATE_READY_TO_GETRESULT,		               // After call FaceBeautyMain()
} MTKFACEBEAUTY_STATE_ENUM;


/*****************************************************************************
    Feature Control Enum and Structure
******************************************************************************/

typedef enum
{
	MTKFACEBEAUTY_FEATURE_BEGIN,              // minimum of feature id
	MTKFACEBEAUTY_FEATURE_SET_ENV_INFO,       // feature id to setup environment information
    MTKFACEBEAUTY_FEATURE_SET_PROC_INFO,      // feature id to setup processing information
    MTKFACEBEAUTY_FEATURE_GET_ENV_INFO,       // feature id to retrieve environment information
    MTKFACEBEAUTY_FEATURE_GET_RESULT,         // feature id to get result
    MTKFACEBEAUTY_FEATURE_GET_LOG,            // feature id to get debugging log
    MTKFACEBEAUTY_FEATURE_MAX                 // maximum of feature id
}	MTKFACEBEAUTY_FEATURE_ENUM;


typedef enum
{
    MTKFACEBEAUTY_CTRL_IDLE,
    MTKFACEBEAUTY_CTRL_ALPHA_MAP,             // 1. generate the alpha texture map and alpha color map 2. generate the alpha maps destinate size
    MTKFACEBEAUTY_CTRL_BLEND_TEXTURE_IMG,     // 2. blend the original image and blur image by alpha texture map. 2. generate the YSH table for PCA
    MTKFACEBEAUTY_CTRL_BLEND_COLOR_IMG,       // 3. blend the smooth image and PCA image by alpha color map.
    MTKFACEBEAUTY_CTRL_MAX
} MTKFACEBEAUTY_CTRL_ENUM;                    // specify in set proc info 

typedef enum
{
    MTKFACEBEAUTY_IMAGE_YUV422,                 // input image format
    MTKFACEBEAUTY_IMAGE_MAX
} MTKFACEBEAUTY_IMAGE_FORMAT_ENUM;

struct MTKFaceBeautyTuningPara
{
    MINT32 WorkMode;                             //0:Extract skin mask + apply wrinkle removal, 1:Extract skin mask only    
    MINT32 SkinToneEn;                           //0:close skin tone adjustment; 1:open skin tone adjustment
    MINT32 BlurLevel;                            //0~4 (weak~strongest)
    MINT32 AlphaBackground;                      //0~255 (non-smooth to strongest smooth)
    MINT32 ZoomRatio;                            //zoom ratio of down-sampled image
    MINT32 TargetColor;                          //0: red, 1: white, 2: natural
};

struct MTKFaceBeautyEnvInfo
{
    MUINT16  SrcImgDSWidth;                      // input DS image width
    MUINT16  SrcImgDSHeight;                     // input DS image height
    MUINT16  SrcImgWidth;                        // input image width
    MUINT16  SrcImgHeight;                       // input image height
    MUINT16  FDWidth;                            // FD width
    MUINT16  FDHeight;                           // FD height
    MTKFACEBEAUTY_IMAGE_FORMAT_ENUM SrcImgFormat;// source image format
    MUINT32  WorkingBufAddr;                     // working buffer
    MUINT32  WorkingBufSize;                     // working buffer size
    MTKFaceBeautyTuningPara *pTuningPara;        // tuning parameters
};

struct MTKFaceBeautyProcInfo
{
    MTKFACEBEAUTY_CTRL_ENUM FaceBeautyCtrlEnum;  // control the process in fb_core
    MUINT8* SrcImgDsAddr;                        // 1/4 size original image addr
    MUINT8* SrcImgAddr;                          // full size original image addr
    MUINT8* SrcImgBlurAddr;                      // full size blur image addr
    MINT32  FDLeftTopPointX1[15];                    // start x position of face in FD image (320x240)
    MINT32  FDLeftTopPointY1[15];                    // start y position of face in FD image (320x240)
    MINT32  FDBoxSize[15];		                     // size of face in FD image (320x240)
    MINT32  FDPose[15];                              // Direction of face (0: 0 degree, 1: 15 degrees, 2: 30 degrees, and the like
    MINT32  FaceCount;                           // Number of face in current image
    MUINT8*  AlphaMap;                           // full size alpha texture map
    MUINT8*  TexBlendResultAddr;                 // Address for storing texture Blend Result without Y and S tuning
    MUINT8*  TexBlendAndYSResultAddr;            // Address for storing texture Blend Result with Y and S tuning, 
    MUINT8*  AlphaMapColor;                      // full size alpha color map
    MUINT8*  PCAImgAddr;                         // full size PCAed image addr
    MUINT8*  ColorBlendResultAddr;               // Address for storing final color blend Result without Y and S tuning
};


struct MTKFaceBeautyResultInfo
{
    MUINT32 AlphaMapDsAddr;                      // DS alpha texture map addr
    MUINT32 AlphaMapColorDsAddr;                 // DS alpha color map addr
    MUINT32 AlphaMapDsCrzWidth;                  // down sample the DS alpha texture map to this width then resize to full size
    MUINT32 AlphaMapDsCrzHeight;                 // down sample the DS alpha texture map to this height then resize to full size
    MUINT32 AlphaMapColorDsCrzWidth;             // down sample the DS alpha color map to this width then resize to full size
    MUINT32 AlphaMapColorDsCrzHeight;            // down sample the DS alpha color map to this height then resize to full size
    MINT32  AngleRange[2];                       // PCA Hue angle range
    MUINT32 PCAYTable;                           // PCA Y table
    MUINT32 PCASTable;                           // PCA Sat table
    MUINT32 PCAHTable;                           // PCA Hue table
    MUINT8* BlendTextureImgAddr;                 // result addr for texture blending without Y and S tuning, this is for debug
    MUINT8* BlendTextureAndYSImgAddr;            // result addr for texture blending with Y and S tuning, this will pass to PCA for tuning Hue.
    MUINT8* BlendColorImgAddr;                   // result addr for blending the smooth and PCAed image with alpha color map
};

class MTKFaceBeauty {
public:
    static MTKFaceBeauty* createInstance(DrvFaceBeautyObject_e eobject);
    virtual void   destroyInstance() = 0;
       
    virtual ~MTKFaceBeauty(){}
    // Process Control
    virtual MRESULT FaceBeautyInit(void *InitInData, void *InitOutData);	
    virtual MRESULT FaceBeautyMain(void);					
    virtual MRESULT FaceBeautyExit(void);					

	// Feature Control        
	virtual MRESULT FaceBeautyFeatureCtrl(MUINT32 FeatureID, void* pParaIn, void* pParaOut);
private:
    
};

class AppFaceBeautyTmp : public MTKFaceBeauty {
public:
    //
    static MTKFaceBeauty* getInstance();
    virtual void destroyInstance();
    //
    AppFaceBeautyTmp() {}; 
    virtual ~AppFaceBeautyTmp() {};
};

#endif
