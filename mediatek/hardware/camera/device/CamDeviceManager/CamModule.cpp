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

#define LOG_TAG "MtkCam/module"
//
#include <hardware/hardware.h>
//
#include <common/CamLog.h>
#include <common/camutils/CamProperty.h>
#include "CamDeviceManager.h"
//
using namespace android;


/*******************************************************************************
 *  Implementation of camera_module
 ******************************************************************************/
static
camera_module
instantiate_camera_module()
{
    CAM_LOGD("[%s]", __FUNCTION__);
    //
    //  (1) Prepare One-shot init.
    MtkCamUtils::Property::clear();

    //  (2)
    camera_module module = {
        common: {
             tag:                   HARDWARE_MODULE_TAG,
             module_api_version:    1,
             hal_api_version:       0,
             id:                    CAMERA_HARDWARE_MODULE_ID,
             name:                  "MTK Camera Module",
             author:                "MTK",
             methods:               CamDeviceManager::get_module_methods(),
             dso:                   NULL, 
             reserved:              {0}, 
        }, 
        get_number_of_cameras:  CamDeviceManager::get_number_of_cameras, 
        get_camera_info:        CamDeviceManager::get_camera_info, 
    };
    return  module;
}


/*******************************************************************************
* Implementation of camera_module
*******************************************************************************/
camera_module HAL_MODULE_INFO_SYM = instantiate_camera_module();


/*******************************************************************************
 *
 ******************************************************************************/
extern "C"
status_t
MtkCam_getProperty(String8 const& key, String8& value)
{
    return  MtkCamUtils::Property::tryGet(key, value)
        ?   OK
        :   NAME_NOT_FOUND
            ;
}


/*******************************************************************************
 *
 ******************************************************************************/
extern "C"
status_t
MtkCam_setProperty(String8 const& key, String8 const& value)
{
    MtkCamUtils::Property::set(key, value);
    return  OK;
}

