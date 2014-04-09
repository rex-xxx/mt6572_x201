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

#define LOG_TAG "MtkCam/CamDevice"
//
#include <camera/CameraParameters.h>
#include <camera/MtkCameraParameters.h>
//
#include <CamUtils.h>
#include <ICamDevice.h>
//
using namespace android;
using namespace NSCamDevice;
using namespace MtkCamUtils;
//
/******************************************************************************
 *
 ******************************************************************************/
#if '1'==MTKCAM_HAVE_SENSOR_HAL
    #include <drv/sensor_hal.h>
#else
    #warning "[Warn] Not support Sensor Hal"
#endif


/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[CamDeviceManager::%s] "fmt, __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)


/******************************************************************************
 *
 ******************************************************************************/
namespace android {
namespace NSCamDevice {
int32_t
searchDevice()
{
    int32_t i4DeviceNum = 0;
    //
    CamProfile  profile(__FUNCTION__, "CamDeviceManager");
    //
//------------------------------------------------------------------------------
#if '1'==MTKCAM_HAVE_SENSOR_HAL

    DevMetaInfo::clear();
    int32_t isFakeOrientation = 0;
    int32_t i4DevSetupOrientation = 0;
    camera_info camInfo;
    camInfo.device_version = CAMERA_DEVICE_API_VERSION_CURRENT;
    camInfo.static_camera_characteristics = NULL;
    //
    SensorHal* pSensorHal = SensorHal::createInstance();
    if  ( ! pSensorHal )
    {
        MY_LOGE("pSensorHal == NULL");
        return 0;
    }
    //
    int32_t const iSensorsList = pSensorHal->searchSensor();
    //
    //
    if  ( iSensorsList & SENSOR_DEV_MAIN_3D )
    {
        MY_LOGI("Stereo 3D Camera found");
#warning "[TODO] Stereo 3D Camera"
    }
    //
    if  ( iSensorsList & SENSOR_DEV_MAIN )
    {
        halSensorDev_e const eHalSensorDev = SENSOR_DEV_MAIN;
        pSensorHal->sendCommand(eHalSensorDev, SENSOR_CMD_GET_FAKE_ORIENTATION, (int)&isFakeOrientation);
        pSensorHal->sendCommand(eHalSensorDev, SENSOR_CMD_GET_SENSOR_ORIENTATION_ANGLE, (int)&i4DevSetupOrientation);
        pSensorHal->sendCommand(eHalSensorDev, SENSOR_CMD_GET_SENSOR_FACING_DIRECTION, (int)&camInfo.facing);
        camInfo.orientation = i4DevSetupOrientation;
        if  ( isFakeOrientation )
        {
            camInfo.orientation = (0==camInfo.facing) ? 90 : 270;
            MY_LOGW("Fake orientation:%d instead of %d, facing=%d HalSensorDev=%#x", camInfo.orientation, i4DevSetupOrientation, camInfo.facing, eHalSensorDev);
        }
        DevMetaInfo::add(i4DeviceNum, camInfo, i4DevSetupOrientation, eDevId_ImgSensor, eHalSensorDev);
        //
        i4DeviceNum++;
    }
    //
    if  ( iSensorsList & SENSOR_DEV_SUB )
    {
        halSensorDev_e const eHalSensorDev = SENSOR_DEV_SUB;
        pSensorHal->sendCommand(eHalSensorDev, SENSOR_CMD_GET_FAKE_ORIENTATION, (int)&isFakeOrientation);
        pSensorHal->sendCommand(eHalSensorDev, SENSOR_CMD_GET_SENSOR_ORIENTATION_ANGLE, (int)&i4DevSetupOrientation);
        pSensorHal->sendCommand(eHalSensorDev, SENSOR_CMD_GET_SENSOR_FACING_DIRECTION, (int)&camInfo.facing);
        camInfo.orientation = i4DevSetupOrientation;
        if  ( isFakeOrientation )
        {
            camInfo.orientation = (0==camInfo.facing) ? 90 : 270;
            MY_LOGW("Fake orientation:%d instead of %d, facing=%d HalSensorDev=%#x", camInfo.orientation, i4DevSetupOrientation, camInfo.facing, eHalSensorDev);
        }
        DevMetaInfo::add(i4DeviceNum, camInfo, i4DevSetupOrientation, eDevId_ImgSensor, eHalSensorDev);
        //
        i4DeviceNum++;
    }
    //
//    if  ( iSensorsList & SENSOR_DEV_ATV )
    {
        halSensorDev_e const eHalSensorDev = SENSOR_DEV_ATV;
        camInfo.facing = 0;
        camInfo.orientation = 0;
        DevMetaInfo::add(i4DeviceNum, camInfo, camInfo.orientation, eDevId_AtvSensor, eHalSensorDev);
        //
        i4DeviceNum++;
    }

lbExit:
    //
    if  ( pSensorHal )
    {
        pSensorHal->destroyInstance();
        pSensorHal = NULL;
    }
    //
    MY_LOGI("iSensorsList=0x%08X, i4DeviceNum=%d", iSensorsList, i4DeviceNum);
    for (int i = 0; i < i4DeviceNum; i++) {
        camera_info const camInfo           = DevMetaInfo::queryCameraInfo(i);
        int32_t const i4DevSetupOrientation = DevMetaInfo::queryDeviceSetupOrientation(i);
        int32_t const i4DevId               = DevMetaInfo::queryDeviceId(i);
        int32_t const i4HalSensorDev        = DevMetaInfo::queryHalSensorDev(i);
        MY_LOGI(
            "[%d] (facing/DevId/HalSensorDev)=(%d/%d/%#x), orientation(wanted/setup)=(%d/%d)", 
            i, camInfo.facing, i4DevId, i4HalSensorDev, camInfo.orientation, i4DevSetupOrientation
        );
    }

#else   //----------------------------------------------------------------------

    #warning "[WARN] Simulation for CamDeviceManager::getNumberOfCameras()"

    DevMetaInfo::clear();
    camera_info camInfo;
    camInfo.device_version  = CAMERA_DEVICE_API_VERSION_CURRENT;
    camInfo.static_camera_characteristics = NULL;
    //
    camInfo.facing      = 0;
    camInfo.orientation = 90;
    DevMetaInfo::add(0, camInfo, camInfo.orientation, eDevId_ImgSensor, 0x01/*SENSOR_DEV_MAIN*/);
    //
    camInfo.facing      = 0;
    camInfo.orientation = 0;
    DevMetaInfo::add(1, camInfo, camInfo.orientation, eDevId_AtvSensor, 0x04/*SENSOR_DEV_ATV*/);
    //
    i4DeviceNum = 2;    //  ImgSensor0, ATV

#endif  //----------------------------------------------------------------------

    profile.print("");
    return  i4DeviceNum;
}
}}

