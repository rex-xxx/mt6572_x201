/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
 

#ifndef _MTK_GIA_H
#define _MTK_GIA_H

#include "MTKGiaType.h"
#include "MTKGiaErrCode.h"

typedef enum
{
    GIA_STATE_STANDBY,
    GIA_STATE_INIT,
    GIA_STATE_PROC,
    GIA_STATE_READY,
    GIA_STATE_IDLE,
    GIA_STATE_MAX
}GIA_STATE_ENUM;

typedef enum
{
    GIA_FEATURE_BEGIN = 0,
    GIA_FEATURE_GET_RESULT,
    GIA_FEATURE_GET_STATUS,
    GIA_FEATURE_SAVE_LOG,

    GIA_FEATURE_SET_PROC_INFO,
    
    GIA_FEATURE_GET_WORK_BUF_INFO,
    GIA_FEATURE_SET_WORK_BUF_INFO,

    GIA_FEATURE_MAX
}	GIA_FEATURE_ENUM;



typedef enum
{
	GIA_SCENARIO_IMAGE_PREVIEW,
    GIA_SCENARIO_IMAGE_CAPTURE,
    GIA_SCENARIO_IMAGE_PLAYBACK,
    GIA_SCENARIO_VIDEO_RECORD,
    GIA_SCENARIO_VIDEO_PLAYBACK 
}GIA_SCENARIO_ENUM;

typedef enum
{
	GIA_SOURCE_FORMAT_RGBA,
	GIA_SOURCE_FORMAT_YUYV
}GIA_SOURCE_FORMAT_ENUM;

//2012-0605 ----
typedef enum
{
	GIA_CONV_EFFECT_NORMAL = 0,
	GIA_CONV_EFFECT_STRONG
}GIA_CONV_EFFECT_ENUM;

typedef enum
{
	GIA_CONV_SPEED_SLOW = 0,
	GIA_CONV_SPEED_NORMAL,
	GIA_CONV_SPEED_FAST
}GIA_CONV_SPEED_ENUM ;

typedef enum
{
	GIA_CONV_SENSING_SLOW = 0,
	GIA_CONV_SENSING_NORMAL,
	GIA_CONV_SENSING_FAST
}GIA_CONV_SENSING_ENUM ;

typedef struct
{
	MUINT32 enable_tuning ;		// 0: disable, 1: enable
	MUINT32 conv_effect ;		// 0: normal, 1: strong
	MUINT32 conv_speed ;		// 0: slow, 1: normal, 2: fast
	MUINT32 conv_sensing ;		// 0: slow, 1: normal, 2: fast
	MINT32 conv_def_deg ;		// -3-3, default 0 //2011-0713
	MUINT32 conv_min_deg ;		// 1-4, default 2
	MUINT32 conv_max_deg ;		// 1-4, default 2
}GIA_TUNING_PARA_STRUCT;
//
typedef struct
{
    MUINT32 source_image_width;
    MUINT32 source_image_height;
    MUINT32 crop_image_width;
    MUINT32 crop_image_height;
    GIA_SCENARIO_ENUM scenario; 
	GIA_SOURCE_FORMAT_ENUM format;
    MUINT32 *learning_data; //for Capture side only
	MUINT32 stride;
	MUINT32 working_buffer_size;
	//2012-0601
	GIA_TUNING_PARA_STRUCT tuning_para;
	//
} GIA_SET_ENV_INFO_STRUCT, *P_GIA_SET_ENV_INFO_STRUCT;


typedef struct
{
    MUINT32 left_offset_x;
    MUINT32 left_offset_y;
    MUINT32 right_offset_x;
    MUINT32 right_offset_y;
	MINT32 cropping_interval_L[9]; //for manual adjustment offsetX1
	MINT32 cropping_interval_R[9]; //for manual adjustment offsetX2
	MUINT32 cropping_interval_default ; // index for default position
	MUINT32 active_flag[9] ; // 0 or 1 for indicating active position // 2012-0627
}GIA_RESULT_STRUCT, *P_GIA_RESULT_STRUCT;


typedef struct
{
    MUINT32 ext_mem_size;
    MUINT32 ext_mem_start_addr; //working buffer start address
}GIA_SET_WORK_BUF_INFO_STRUCT, *P_GIA_SET_WORK_BUF_INFO_STRUCT;



typedef struct
{

    MUINT32 left;
    MUINT32 right;
    MUINT32 upper;
    MUINT32 down;
}GIA_AF_FOCUS_INFO;

typedef struct
{
    MUINT32 source_image_left_addr;
    MUINT32 source_image_right_addr;
    GIA_AF_FOCUS_INFO af_location_info;
    MUINT32 frame_rate;

}GIA_SET_PROC_INFO_STRUCT, *P_GIA_SET_PROC_INFO_STRUCT;



class MTKGia {
public:
    static MTKGia* createInstance();
    virtual void   destroyInstance() = 0;
       
    virtual ~MTKGia(){};
    // Process Control
    virtual MRESULT GiaInit(void* InitInData);
    virtual MRESULT GiaMain();	// START
    virtual MRESULT GiaReset();   //Reset
            
	// Feature Control        
	virtual MRESULT GiaFeatureCtrl(MUINT32 FeatureID, void* pParaIn, void* pParaOut);
private:
    
};


#endif
