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

#ifndef _MHAL_INC_CAMERA_PARAMETERS_H_
#define _MHAL_INC_CAMERA_PARAMETERS_H_


#include <mhal/inc/camera/areas.h>
/******************************************************************************
*
*******************************************************************************/


/**
 * @par Structure
 *   mhalCamExifParam_t
 * @par Description
 *   This is a structure which store camera EXIF information
 */
typedef struct mhalCamExifParam_s {
    int gpsIsOn;
    int gpsAltitude;
    char gpsLatitude[32];
    char gpsLongitude[32];
    char gpsTimeStamp[32];
    char gpsProcessingMethod[64];
    int orientation;
} mhalCamExifParam_t;


/**
 * @par Structure
 *   mhalZsdParam_t
 * @par Description
 *   This is a structure which store camera ZSD information
 */
typedef struct mhalZsdParam_s {
    MUINT32 u4ZsdEnable;        // 0: disable,1: enable
    MUINT32 u4ZsdWidth;
    MUINT32 u4ZsdHeight;
    MUINT32 u4ZsdZoomWidth;
    MUINT32 u4ZsdZoomHeigth;
    MUINT32 u4ZsdAddr;
    MUINT32 u4ZsdSkipPrev;
    MUINT32 u4ZsdEVShot;
    MUINT32 u4ZsdDump;          //save file to: sdcard/zsd_cap.bin
} mhalZsdParam_t;

/**
 * @par Structure
 *   mhalVSSParam_t
 * @par Description
 *   This is a structure which store video snapshot information
 */
typedef struct mhalVSSJpgEncParam_s {
    MUINT32 u4ImgWidth;
    MUINT32 u4ImgHeight;
    MUINT32 u4ImgRgbVirAddr;
    MUINT32 u4ImgYuvVirAddr;
    MUINT32 u4ImgJpgVirAddr;
    MUINT32 u4ImgJpgSize;
} mhalVSSJpgEncParam_t;

/**
 * @par Structure
 *   mhalCam3AParam_t
 * @par Description
 *   This is a structure which store camera 3A information
 */
typedef struct mhalCam3AParam_s {
    // Do not change order
    MINT32 strobeMode;
    MINT32 sceneMode;
    MINT32 antiBandingMode;
    MINT32 afMode;
    MINT32 afMeterMode;
    MINT32 aeMeterMode;
    MINT32 aeExpMode;
    MINT32 awbMode;
    MINT32 isoSpeedMode;
    MINT32 effectMode;
    MINT32 brightnessMode;
    MINT32 hueMode;
    MINT32 saturationMode;
    MINT32 edgeMode;
    MINT32 contrastMode;
    MINT32 aeMode;
    //
    MINT32 prvFps;
    MINT32 afIndicator;
    //
    MINT32 eisMode; 
    //
    MINT32 afX;
    MINT32 afY;
    MINT32 afLampMode;
    //
    MINT32 eisW; 
    MINT32 eisH; 
    // AE, AWB Lock 
    MINT32 isAELock;                //Android 4.0 
    MINT32 isAWBLock;             //Android 4.0 	
    // Eng
    MINT32 afEngMode;
    MINT32 afEngPos;
    MINT32 isoSpeedModeEng;
    MINT32 EngMode;
    //
    camera_focus_areas_t focusAreas; 
    camera_metering_areas_t meteringAreas; 
    
    
    MINT32 isBurstShotMode;
} mhalCam3AParam_t;


/**
 * @par Structure
 *   mhalCamFrame_t
 * @par Description
 *   This is the sturcture for cam frame 
 */
typedef struct mhalCamFrame_s {
    MUINT32 virtAddr;
    MUINT32 phyAddr;
    MUINT32 w;
    MUINT32 h;
    MUINT32 frmCount;
    MUINT32 frmSize;
    MUINT32 bufSize;
    MINT32 frmFormat;
} mhalCamFrame_t;

/**
 *  mHalCam 3A support feature 
 */
typedef struct mHalCam3AFeature_s {
     MUINT32  u4ExposureLock; 
     MUINT32  u4AutoWhiteBalanceLock; 
     MUINT32  u4FocusAreaNum; 
     MUINT32  u4MeterAreaNum; 
}mHalCam3ASupportFeature_t; 


/**
 *  mHalCam shot parameter
 */
struct mhalCamShotParam
{
    mHalCamObserver     shotObserver;
    MUINT32             u4ShotMode;
    MUINT32             u4BusrtNo;
    MUINT32             u4BusrtCnt;
    //
    //  Zoom Ratio x 100
    //  For example, 100, 114, and 132 refer to 1.00, 1.14, and 1.32 respectively.
    MUINT32             u4ZoomRatio;
    //
    MUINT32             u4JpgQValue;    //  JPEG Quality value
    MUINT32             u4ThumbW;
    MUINT32             u4ThumbH;
    //
    mhalCamFrame_t      frmQv;
    mhalCamFrame_t      frmJpg;
    //
    MUINT8              uShotFileName[64];
    //
    MUINT32             u4CapPreFlag;   //  0:capture full size; 1:capture the preview size
    MUINT32             u4IsDumpRaw;
    MUINT32             u4Scalado;      //  0:disable; 1:enable
    MUINT32             u4DumpYuvData;  //  0:disable; 1:enable
    MUINT32             u4JPEGOrientation;       //For jpeg bitstream orientation 0:disable; 1:enable 
    MUINT32             u4ContinuousShotSpeed;   //For continuous shot speed 0:normal; 1:fast; 2: slow 
    MUINT32 	        u4strobeon;
	MUINT32             u4awb2pass;
};


/**
 * @par Structure
 *   mhalCamParam_t
 * @par Description
 *   This is a structure which store camera configuration
 */
typedef struct mhalCamParam_s : public mhalCamShotParam
{
    mHalCamObserver mhalObserver;
    mhalCamFrame_t  frmYuv;
    mhalCamFrame_t  frmRgb;
    mhalCamFrame_t  frmBS;
    MUINT32 u4ZoomVal;
    MUINT32 u4FpsMode;          ///< Frame rate mode, 0: Normal, 1: Fixed
    MUINT32 u4CamMode;          ///< Preview mode, 0: Default, 1: mtk-preivew, 2: mtk-video
    MUINT8  u1FileName[128];
    mhalCamExifParam_t camExifParam;
    mhalCam3AParam_t cam3AParam;
    MUINT32 u4Rotate;
    MUINT32 u4IsDrawAFIndicator;
    MUINT32 u4VEncBitrate;      // Video encode bitrate
    MUINT32 eVEncFormat;        // Video encode format [Remove]
//    VDO_ENC_FORMAT eVEncFormat;       // Video encode format
    MUINT32 u4CamIspMode;       // 0: normal, 1: pure raw
    mhalZsdParam_t camZsdParam;
    mhalCamFrame_t frmVdo;     //video frame
} mhalCamParam_t;


/**
 *
 */
typedef struct mhalCamRawImageInfo_s {
    MUINT32 u4Width;
    MUINT32 u4Height;
    MUINT32 u4BitDepth;
    MUINT32 u4IsPacked;
    MUINT32 u4Size;
    MUINT32 u1Order;         
} mhalCamRawImageInfo_t;


/**
 *
 */
typedef struct mhalCamSensorInfo_s {
    MINT32 facing;
    MINT32 orientation;
    MINT32 devType;
} mhalCamSensorInfo_t;


/**
 *
 */
typedef struct mhalCamJpgConfigInfo_s {
    MUINT32 VirtAddr;
    MUINT32 PhyAddr;
    MUINT32 Size;
    MUINT32 Width;
    MUINT32 Height;
    MUINT32 Quality;
} mhalCamJpgConfigInfo_t;

/**
 *
 */
#define MHAL_CAM_MODE_DEFAULT   0
#define MHAL_CAM_MODE_MPREVIEW  1
#define MHAL_CAM_MODE_MVIDEO    2
#define MHAL_CAM_MODE_VT        3


/**
 *
 */
enum
{
    MHAL_CAM_CAP_MODE_UNKNOWN       =   0xFFFFFFFF, 
    MHAL_CAM_CAP_MODE_NORMAL        =   0, 
    MHAL_CAM_CAP_MODE_EV_BRACKET, 
    MHAL_CAM_CAP_MODE_BEST_SHOT, 
    MHAL_CAM_CAP_MODE_BURST_SHOT, 
    MHAL_CAM_CAP_MODE_SMILE_SHOT, 
    MHAL_CAM_CAP_MODE_AUTORAMA, 
    MHAL_CAM_CAP_MODE_MAV, 
    MHAL_CAM_CAP_MODE_HDR, 
    MHAL_CAM_CAP_MODE_ASD, 
    MHAL_CAM_CAP_MODE_PANO_3D,
    MHAL_CAM_CAP_MODE_SINGLE_3D,
    MHAL_CAM_CAP_MODE_FACE_BEAUTY,
    MHAL_CAM_CAP_MODE_CONTINUOUS_SHOT
};


//
#define HAL_PANO_DIR_RIGHT      0
#define HAL_PANO_DIR_LEFT       1
#define HAL_PANO_DIR_UP         2
#define HAL_PANO_DIR_DOWN       3
//
#define MHAL_CAM_SENSOR_DEV_MAIN        0x01
#define MHAL_CAM_SENSOR_DEV_SUB         0x02
#define MHAL_CAM_SENSOR_DEV_ATV         0x04
#define MHAL_CAM_SENSOR_DEV_MAIN_2      0x08



////////////////////////////////////////////////////////////////////////////////
#endif  //  _MHAL_INC_CAMERA_PARAMETERS_H_

