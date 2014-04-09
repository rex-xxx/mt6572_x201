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
//
#include "CamDeviceManager.h"
#include <ICamDevice.h>
//
using namespace android;
using namespace NSCamDevice;
using namespace MtkCamUtils;
//


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
namespace
{
String8                 g_s8ClientAppMode;
}   //namespace


/******************************************************************************
 *
 ******************************************************************************/
static
String8 const
queryClientAppMode()
{
#warning "[TODO] Before opening camera, client must Camera::setProperty for app mode."
/*
    Before opening camera, client must call
    Camera::setProperty(
        String8(MtkCameraParameters::PROPERTY_KEY_CLIENT_APPMODE), 
        String8(MtkCameraParameters::APP_MODE_NAME_MTK_xxx)
    ), 
    where MtkCameraParameters::APP_MODE_NAME_MTK_xxx = one of the following:
        MtkCameraParameters::APP_MODE_NAME_DEFAULT
        MtkCameraParameters::APP_MODE_NAME_MTK_ENG
        MtkCameraParameters::APP_MODE_NAME_MTK_ATV
        MtkCameraParameters::APP_MODE_NAME_MTK_S3D
        MtkCameraParameters::APP_MODE_NAME_MTK_VT
*/
    String8 const s8ClientAppModeKey(MtkCameraParameters::PROPERTY_KEY_CLIENT_APPMODE);
    String8       s8ClientAppModeVal(MtkCameraParameters::APP_MODE_NAME_DEFAULT);
    //
    //  (1) get Client's property.
    MtkCamUtils::Property::tryGet(s8ClientAppModeKey, s8ClientAppModeVal);
    if  ( s8ClientAppModeVal.isEmpty() ) {
        s8ClientAppModeVal = MtkCameraParameters::APP_MODE_NAME_DEFAULT;
    }

    //  (2) reset Client's property.
    MtkCamUtils::Property::set(s8ClientAppModeKey, String8::empty());
    //
    //
    return  s8ClientAppModeVal;
}


/******************************************************************************
 *
 ******************************************************************************/
static
ICamDevice*
createIDevice(
    int32_t const           i4DevOpenId, 
    hw_device_t const&      hwdevice, 
    hw_module_t const*const hwmodule
)
{
    g_s8ClientAppMode = queryClientAppMode();
    //
    MY_LOGI("+ tid:%d OpenID:%d ClientAppMode:%s", ::gettid(), i4DevOpenId, g_s8ClientAppMode.string());
    //
    ICamDevice* pdev = NSCamDevice::createDevice(g_s8ClientAppMode, i4DevOpenId);
    //
    if  ( pdev != 0 )
    {
        pdev->incStrong(pdev);
        //
        hw_device_t* hwdev = pdev->get_hw_device();
        *hwdev = hwdevice;
        hwdev->module = const_cast<hw_module_t*>(hwmodule);
        //
        if  ( ! pdev->init() )
        {
            MY_LOGE("fail to initialize a newly-created instance");
            pdev->uninit();
            pdev = NULL;
        }
    }
    //
    MY_LOGI("- created instance=%p", &(*pdev));
    return  pdev;
}


/******************************************************************************
 *
 ******************************************************************************/
static
void
destroyDevice(ICamDevice* pdev)
{
    pdev->decStrong(pdev);
}


/******************************************************************************
 *
 ******************************************************************************/
CamDeviceManager&
CamDeviceManager::
getInstance()
{
    static  CamDeviceManager singleton;
    return  singleton;
}


/******************************************************************************
 *
 ******************************************************************************/
CamDeviceManager::
CamDeviceManager()
    : mMtxOpenLock()
    , mi4OpenNum(0)
    , mi4DeviceNum(0)
{
}


/******************************************************************************
 *
 ******************************************************************************/
CamDeviceManager::
~CamDeviceManager()
{
}


/******************************************************************************
 *
 ******************************************************************************/
int32_t
CamDeviceManager::
getNumberOfCameras()
{
    static bool bIsInitialized = false;
    if  ( bIsInitialized ) {
        int32_t i4DeviceNum = DevMetaInfo::queryNumberOfDevice();
        MY_LOGD("has init before - %d cameras", i4DeviceNum);
        return  i4DeviceNum;
    }
    bIsInitialized = true;
    //
    CamProfile  profile(__FUNCTION__, "CamDeviceManager");
    android::Mutex::Autolock lock(mMtxOpenLock);
    mi4DeviceNum = NSCamDevice::searchDevice();
    profile.print("");
    return  mi4DeviceNum;
}


/******************************************************************************
*
*******************************************************************************/
int
CamDeviceManager::
getCameraInfo(int const cameraId, camera_info& rInfo)
{
    rInfo = DevMetaInfo::queryCameraInfo(cameraId);
    MY_LOGI("id(%d) (facing,orientation)=(%d,%d)", cameraId, rInfo.facing, rInfo.orientation);
    return  0;
}


/******************************************************************************
*
*******************************************************************************/
int
CamDeviceManager::
openDevice(const hw_module_t* module, const char* name, hw_device_t** device)
{
    int err = OK;
    //
    ICamDevice* pdev = NULL;
    int32_t     i4OpenId = 0;
    //
    Mutex::Autolock lock(mMtxOpenLock);
    //
    MY_LOGI("+ mi4OpenNum(%d), mi4DeviceNum(%d)", mi4OpenNum, mi4DeviceNum);

    if (name != NULL)
    {
        i4OpenId = ::atoi(name);
        //
        if  ( DevMetaInfo::queryNumberOfDevice() < i4OpenId )
        {
            err = -EINVAL;
            goto lbExit;
        }
        //
        if  ( MAX_SIMUL_CAMERAS_SUPPORTED <= mi4OpenNum )
        {
            MY_LOGW("open number(%d) >= maximum number(%d)", mi4OpenNum, MAX_SIMUL_CAMERAS_SUPPORTED);
            MY_LOGE("does not support multi-open");
            err = -ENOMEM;
            goto lbExit;
        }
        //
        pdev = createIDevice(
            i4OpenId, 
            *get_hw_device(), 
            module
        );
        //
        if  ( ! pdev )
        {
            MY_LOGE("camera device allocation fail: pdev(0)");
            err = -ENOMEM;
            goto lbExit;
        }

        *device = pdev->get_hw_device();
        //
        mi4OpenNum++;
    }

lbExit:
    if  ( OK != err )
    {
        if  ( pdev )
        {
            destroyDevice(pdev);
            pdev = NULL;
        }
        //
        *device = NULL;
    }
    MY_LOGI("- mi4OpenNum(%d)", mi4OpenNum);
    return  err;
}


/******************************************************************************
*
*******************************************************************************/
int
CamDeviceManager::
closeDevice(hw_device_t* device)
{
    int err = OK;
    //
    ICamDevice* pdev = ICamDevice::getIDev(reinterpret_cast<camera_device*>(device));
    //
    MY_LOGI("+ device(%p), ICamDevice(%p)", device, pdev);
    //
    Mutex::Autolock lock(mMtxOpenLock);
    //
    //  reset Client's property.
    String8 const s8ClientAppModeKey(MtkCameraParameters::PROPERTY_KEY_CLIENT_APPMODE);
    MtkCamUtils::Property::set(s8ClientAppModeKey, String8::empty());
    //
    if  ( pdev )
    {
        mi4OpenNum--;
        destroyDevice(pdev);
        pdev = NULL;
        err = OK;
    }
    else
    {
        err = -EINVAL;
    }
    //
    MY_LOGI("- status(%d)", err);
    return  err;
}

