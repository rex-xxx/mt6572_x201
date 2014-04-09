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

#ifndef _MTK_Detection_H
#define _MTK_Detection_H

//#include <system/camera.h>
//#include "MediaHal.h"

typedef unsigned int        MUINT32;
typedef unsigned short      MUINT16;
typedef unsigned char       MUINT8;

typedef signed int          MINT32;
typedef signed short        MINT16;
typedef signed char         MINT8;

#define SmileDetect                            (1)

#define MAX_FACE_NUM                  (15)

typedef enum DRVFDObject_s {
    DRV_FD_OBJ_NONE = 0,
    DRV_FD_OBJ_SW,
    DRV_FD_OBJ_HW,
    DRV_FD_OBJ_UNKNOWN = 0xFF,
} DrvFDObject_e;

typedef enum
{
	FDVT_IDLE_MODE=0,
	FDVT_READY_MODE,
	FDVT_GFD_MODE,	
	FDVT_LFD_MODE,
	FDVT_SD_MODE
} FDVT_OPERATION_MODE_ENUM;

struct result
{
	bool af_face_indicator ; // 1 bit
     int     face_index  ;   //   9 bit
            
     // Result type -> FD(0) or LFD(1)
     int     type        ;   //   1 bit

     // Face candidate position 
     int     x0          ;   //   9 bit
     int     y0          ;   //   8 bit
     int     x1          ;   //   9 bit    
     int     y1          ;   //   8 bit                
     
     int     fcv         ;   //   8 bit  confidence value
     // int     fpose       ;   // 3+4 bit  3 bit (0/1/2/3/4/5 = ROP00/ROP+50/ROP-50/ROP+90/ROP-90) + 4 bit (RIP, 0-11)
     int     rip_dir     ;   //   4 bit
     int     rop_dir     ;   //   3 bit (0/1/2/3/4/5 = ROP00/ROP+50/ROP-50/ROP+90/ROP-90)

     int     size_index     ;   //   5 bit 
     int     face_num;   
 };
 
 typedef struct
{
    MINT16   wLeft;
    MINT16   wTop;
    MINT16   wWidth;
    MINT16   wHeight;
} FACEDETECT_RECT;
 
/*******************************************************************************
*
********************************************************************************/
class MTKDetection {
public:
    static MTKDetection* createInstance(DrvFDObject_e eobject);
    virtual void      destroyInstance() {};
       
    virtual ~MTKDetection() {}
    virtual void FDVTInit(MUINT32 *fd_tuning_data);
    virtual void FDVTMain(MUINT32 image_buffer_address, MUINT32 image_buffer_address1, FDVT_OPERATION_MODE_ENUM fd_state);
    virtual void FDVTReset(void);
    virtual MUINT32 FDVTGetResultSize(void);
    virtual MUINT8 FDVTGetResult(MUINT32 FD_result_Adr);
    //virtual void FDVTGetICSResult(camera_frame_metadata_mtk *FD_ICS_Result, struct result * FD_Results, MUINT32 Width,MUINT32 Heigh, MUINT32 LCM, MUINT32 Sensor, MUINT32 Camera_TYPE, MUINT32 Draw_TYPE);
    virtual void FDVTGetICSResult(MUINT32 FD_ICS_Result, MUINT32 FD_Results, MUINT32 Width,MUINT32 Heigh, MUINT32 LCM, MUINT32 Sensor, MUINT32 Camera_TYPE, MUINT32 Draw_TYPE);
    virtual void FDVTGetFDInfo(MUINT32  FD_Info_Result);
    virtual void FDVTDrawFaceRect(MUINT32 image_buffer_address,MUINT32 Width,MUINT32 Height,MUINT32 OffsetW,MUINT32 OffsetH,MUINT8 orientation);
    #ifdef SmileDetect
    virtual void FDVTSDDrawFaceRect(MUINT32 image_buffer_address,MUINT32 Width,MUINT32 Height,MUINT32 OffsetW,MUINT32 OffsetH,MUINT8 orientation);
    virtual MUINT8 FDVTGetSDResult(MUINT32 FD_result_Adr);
    #endif
private:
    
};

class AppFDTmp : public MTKDetection {
public:
    //
    static MTKDetection* getInstance();
    virtual void destroyInstance();
    //
    AppFDTmp() {}; 
    virtual ~AppFDTmp() {};
};

#endif

