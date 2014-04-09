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
#include <dlfcn.h>
//
#include <camera/CameraParameters.h>
#include <camera/MtkCameraParameters.h>
//
#include <CamUtils.h>
//
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
namespace android {
namespace NSCamDevice {
ICamDevice*
createDevice(
    String8 const&          s8ClientAppMode, 
    int32_t const           i4DevOpenId
)
{
    ICamDevice* pdev = NULL;
    //
    EDeviceId const eDeviceId = (EDeviceId)DevMetaInfo::queryDeviceId(i4DevOpenId);
    //
    MY_LOGI("+ DevOpenID:%d, eDeviceId:%d", i4DevOpenId, eDeviceId);
    //
    //
    if  ( s8ClientAppMode == MtkCameraParameters::APP_MODE_NAME_MTK_ENG )
    {
        MY_LOGI("APP_MODE_NAME_MTK_ENG");
        String8 const s8CamDeviceInstFactory = String8::format("createCamDevice_Default");
        void* pCreateInstance = ::dlsym(RTLD_DEFAULT, s8CamDeviceInstFactory.string());
        MY_LOGF_IF(0==pCreateInstance, "Not exist: %s for %s", s8CamDeviceInstFactory.string(), s8ClientAppMode.string());
        pdev =  reinterpret_cast<ICamDevice* (*)(String8 const&, int32_t const)>
                    (pCreateInstance)(s8ClientAppMode, i4DevOpenId)
                    ;
    }
    //
    else
    if  ( s8ClientAppMode == MtkCameraParameters::APP_MODE_NAME_MTK_ATV )
    {
        MY_LOGF_IF(eDeviceId != eDevId_AtvSensor, "eDeviceId(%d) != eDevId_AtvSensor", eDeviceId);
        String8 const s8CamDeviceInstFactory = String8::format("createCamDevice_MtkAtv");
        void* pCreateInstance = ::dlsym(RTLD_DEFAULT, s8CamDeviceInstFactory.string());
        MY_LOGF_IF(0==pCreateInstance, "Not exist: %s for %s", s8CamDeviceInstFactory.string(), s8ClientAppMode.string());
        pdev =  reinterpret_cast<ICamDevice* (*)(String8 const&, int32_t const)>
                    (pCreateInstance)(s8ClientAppMode, i4DevOpenId)
                    ;
    }
    //
    else
    if  ( s8ClientAppMode == MtkCameraParameters::APP_MODE_NAME_MTK_S3D )
    {
        String8 const s8CamDeviceInstFactory = String8::format("createCamDevice_MtkS3d");
        void* pCreateInstance = ::dlsym(RTLD_DEFAULT, s8CamDeviceInstFactory.string());
        MY_LOGF_IF(0==pCreateInstance, "Not exist: %s for %s", s8CamDeviceInstFactory.string(), s8ClientAppMode.string());
        pdev =  reinterpret_cast<ICamDevice* (*)(String8 const&, int32_t const)>
                    (pCreateInstance)(s8ClientAppMode, i4DevOpenId)
                    ;
    }
    //
//    if  ( s8ClientAppMode == MtkCameraParameters::APP_MODE_NAME_DEFAULT )
    else
    {
        String8 const s8CamDeviceInstFactory = String8::format("createCamDevice_Default");
        void* pCreateInstance = ::dlsym(RTLD_DEFAULT, s8CamDeviceInstFactory.string());
        MY_LOGF_IF(0==pCreateInstance, "Not exist: %s for %s", s8CamDeviceInstFactory.string(), s8ClientAppMode.string());
        pdev =  reinterpret_cast<ICamDevice* (*)(String8 const&, int32_t const)>
                    (pCreateInstance)(s8ClientAppMode, i4DevOpenId)
                    ;
    }
    //
    //
    MY_LOGI("- %p", &(*pdev));
    return  pdev;
}
}}

