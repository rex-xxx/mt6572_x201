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

#ifndef _MTK_Pano_COMMON_H
#define _MTK_Pano_COMMON_H

#include "MTKPanoType.h"
#include "MTKPanoErrCode.h"

#define RGB565_R_MASK (0xF800)
#define RGB565_G_MASK (0x07E0)
#define RGB565_B_MASK (0x001F)

#define RGB565_TO_RGB888_R(VALUE)   ((((VALUE) & RGB565_R_MASK)>>8))
#define RGB565_TO_RGB888_G(VALUE)   ((((VALUE) & RGB565_G_MASK)>>3))
#define RGB565_TO_RGB888_B(VALUE)   ((((VALUE) & RGB565_B_MASK)<<3))

#define RGB888_TO_YUV_Y(R, G, B)    ((  306 * (R) + 601 * (G) + 117 * (B) + 512) >> 10)
#define	RGB888_TO_RGB565(R,G,B)		(((((R)+4) >> 3)<<11) | ((((G)+2)>>2)<<5) | (((B)+4)>>3))

#define abs(a) (((a) < 0) ? -(a) : (a)) 
#define max(a,b) (((a)>(b))? (a) : (b))
#define min(a,b) (((a)<(b))? (a) : (b))

#define OVLP_RATIO                      16                      // overlap ratio (base = 64, OVLP_RATIO = 16=>16/64=1/4, OVLP_RATIO = 21=>21/64~=1/3)
#define BLEND_SCALE	                8
#define	MAX_PANO_IMG_WIDTH		4092			// maximum panorama img width
#define	MAX_PANO_IMG_HEIGHT		3070			// maximum panorama img width
#define MAX_IMG_NUM			3			// maximum stitch image number
#define	PANO_MAX_IMG_NUM		8
#define	MAX_SEAM_SIZE			1600			// maximum seam array size
#define D        			2			// subsample for motion estimation
#define	BLEND_WIDTH			20
#define	SEAM_COST_THRES			5       		// clamp cost to 0 if cost below threshold

#define FEATHER_BLEND                   0
#define	BLEND_ENABLE			1					// blending enable
#define SHADING_BOUNDARY_ENABLE		1
#define VERTICAL_MARGIN_RATIO  		10			// means 1/10
#define HORIZONTAL_MARGIN_RATIO 	10	
#define	PANO_PROCESS_MAX_COUNT	        32	                /* process times */

typedef enum
{
	ERROR_NONE=0,
	ERROR_MSDK_IS_ACTIVED,
	ERROR_INVALID_DRIVER_MOD_ID,
	ERROR_INVALID_FEATURE_ID,
	ERROR_INVALID_SCENARIO_ID,
	ERROR_INVALID_CTRL_CODE,
	ERROR_VIDEO_ENCODER_BUSY,
	ERROR_INVALID_PARA,
	ERROR_OUT_OF_BUFFER_NUMBER,
	ERROR_INVALID_ISP_STATE,
	ERROR_INVALID_MSDK_STATE,
	ERROR_PHY_VIR_MEM_MAP_FAIL,
	ERROR_ENQUEUE_BUFFER_NOT_FOUND,
	ERROR_MSDK_BUFFER_ALREADY_INIT,
	ERROR_MSDK_BUFFER_OUT_OF_MEMORY,
	ERROR_SENSOR_POWER_ON_FAIL,
	ERROR_SENSOR_CONNECT_FAIL,
	ERROR_MSDK_IO_CONTROL_CODE,
	ERROR_MSDK_IO_CONTROL_MSG_QUEUE_OPEN_FAIL,
	ERROR_DRIVER_INIT_FAIL,
	ERROR_WRONG_NVRAM_CAMERA_VERSION,
	ERROR_NVRAM_CAMERA_FILE_FAIL,
	ERROR_IMAGE_DECODE_FAIL,
	ERROR_IMAGE_ENCODE_FAIL,
	ERROR_LED_FLASH_POWER_ON_FAIL,
	ERROR_MAX
} MSDK_ERROR_CODE_ENUM;

typedef enum
{
	PANO_IDLE = 0,
	PANO_INIT_START,
	PANO_CLIP_PHASE1_START,
	PANO_CONVERT_GRAY_START,
	PANO_MOTIONESTIMATION_START,
	PANO_SEAMSELECTION_START,
	PANO_ADD_IMAGE_END,
	PANO_CALCULATEPANOSIZE_START,	
	PANO_CLIP_PHASE2_START,
	PANO_IMAGESTITCH_START,
	PANO_FINISH,
	PANO_STATE_MAX	
} PANO_OPERATION_STATE_ENUM;

typedef struct
{
	kal_int32 op_v;
	kal_int32 op_h;
	const kal_int32 *search_pos_pt;
	kal_int32 dir;
	kal_int32 op_dir;
	kal_int32 v_ori; 
	kal_int32 h_ori;
	kal_int32 kk;
	kal_int32 op_kk;
	kal_uint32 total_step_count;
	float op_ts;
	kal_bool first_time;
	kal_bool first_round;
	kal_bool small_search;
} pano_motion_estimation_struct;

typedef enum
{	
	PANO_RIGHT_DIR=0,
	PANO_LEFT_DIR,
	PANO_UP_DIR,
	PANO_DOWN_DIR,
	PANO_DIR_NO
} PANO_DIRECTION_ENUM;

typedef struct
{
	kal_uint8  *jpeg_src_buffer_addr;	// source encoded jpeg buffer address to be clip
	kal_uint32 jpeg_src_buffer_size;	// source encoded jpeg buffer size to be clip	
	kal_uint32 src_width;				// source encoded jpeg image size
	kal_uint32 src_height;
	kal_uint32 src_roi_x;				// start point x of ROI of source image
	kal_uint32 src_roi_y;				// start point y of ROI of source image
	kal_uint32 roi_w;					// width of ROI
	kal_uint32 roi_h;					// height of ROI
	kal_uint8  *jpeg_dst_buffer_addr;	// dest decoded jpeg buffer to clip to, RGB565
	kal_uint8  jpeg_dst_type;			/* RGB565/RGB888 */		
	kal_uint32 dst_width;				// dest decoded jpeg image size
	kal_uint32 dst_height;					
	kal_uint32 dst_roi_x;				// start point x of ROI of dest image
	kal_uint32 dst_roi_y;				// start point y of ROI of dest image
	kal_bool is_switch_cachable;		// is switch cachable after decode(bool)
} clip_struct;

typedef struct
{
	kal_uint32 snapshot_number;				// the number of images to be stitched
	kal_uint32 img_width;					// the source image width
	kal_uint32 img_height;  				// the source image height
	kal_uint32 work_mem_addr;				// the address of working memory
	kal_uint32 work_mem_size;				// working memory size
	kal_uint8   jpeg_dst_type;				// RGB565/RGB888
	kal_uint8  *jpeg_src_buffer_addr[PANO_MAX_IMG_NUM];	// source buffer address 
	kal_uint32  jpeg_src_buffer_size[PANO_MAX_IMG_NUM];	// source buffer size 			
	PANO_DIRECTION_ENUM pano_direction;				/* pano capture direction(PANO_DIRECTION_ENUM) */	
	kal_int32 (*cam_pano_decode_cb) (clip_struct *clip_data);	/* call back function for pano process */	
	kal_uint32	max_pano_img_width;
	kal_uint32	max_pano_img_height;
	kal_int32	blend_scale;				        /* pixel-based blending scale */
	kal_int32 	upper_margin;					/* work around for worse lens shading */
	kal_int32 	lower_margin;
	kal_int32 	left_margin;
	kal_int32 	right_margin;	
} panoinfo_struct; 

typedef struct
{
	kal_uint8  *jpeg_src_buffer_ptr;		/* dest buffer address,RGB565, dst buffer size (11.52/2 = 5.76MB[MMI]) */
	kal_uint16  target_width;				/* the width of target image */
	kal_uint16  target_height;				/* the height of target image */  
}CAMERA_PANO_RESULT_STRUCT, camera_pano_result_struct;

typedef struct
{
	kal_uint32 	extmem_start_address;	                        /* external memory start address for hardware engine buffer and algorithm */
	kal_uint32 	extmem_size;		                        /* external memory size, 9.6MB+8B+4.8KB(MED,RGB565) */
	kal_uint32	source_width;			                /* the image width of resizer */
	kal_uint32	source_height;			                /* the image height of resizer */
	kal_uint8	jpeg_dst_type;		                        /* RGB565/RGB888 */		
	kal_uint8       *jpeg_src_buffer_addr[PANO_MAX_IMG_NUM];	/* source buffer address */
	kal_uint32      jpeg_src_buffer_size[PANO_MAX_IMG_NUM];	        /* source buffer size */			
	kal_uint8 	snapshot_number;	                 	/* total number of pano pictures */	
	PANO_DIRECTION_ENUM pano_direction;	                        /* pano capture direction(PANO_DIRECTION_ENUM) */
	kal_int32       (*camera_pano_cb) (clip_struct *clip_data);     /* call back function for pano process */	
	kal_int32	pano_blend_scale;	                	/* pixel-based blending scale */
	kal_int32	pano_upper_margin;	                        /* work around for worse lens shading based on 2M size (< 2M*1/10) */
	kal_int32	pano_lower_margin;                              
	kal_int32	pano_left_margin;	                        /* work around for worse lens shading based on 2M size (< 2M*1/10) */
	kal_int32	pano_right_margin;	
} CAMERA_PANO_PROCESS_STRUCT, camera_pano_process_struct;

typedef enum
{
	PANO_IDLE_STATE = 0,		// before initialize state
	PANO_ADD_IMAGE_STATE,		// add images for pano algo
	PANO_ADD_IMAGE_READY_STATE,	// add images for pano algo	
	PANO_STITCH_STATE,			// initialize for stitch
	PANO_BUSY_STATE,			// clip non-overlap regions to output image
	PANO_RESET_STATE,			// Pano reset state
	PANO_READY_STATE			// finish panorama process
} PANO_STATE_ENUM;

typedef struct
{
	// for Camera APP
	kal_uint16 MaxWidth;		// maximum capture widht in panoram capture mode
	kal_uint16 MaxHeight;		// maximum capture height in panoram capture mode
	kal_uint16 MaxPanoNo;		// maximum number of stitched pictures.
} CAMERA_PANORAMA_INFO_STRUCT, *PCAMERA_PANORAMA_INFO_STRUCT;

typedef struct
{
	kal_uint32 	WorkingBufAddr;		/* external memroy start address for fd working buffer (cachable) */
	kal_uint32 	WorkingBufSize;		/* external memory size for fd working buffer (cachable) */
	kal_uint32	SourceWidth;		/* image source width */
	kal_uint32	SourceHeight;		/*image source height */
	kal_uint8 	SnapshotNumber;
	PANO_DIRECTION_ENUM	PanoDirection;
	//CAMERA_TUNING_OPERATION_PARA_STRUCT *pTuningPara;	/* tuning parameters from custom folder */
} CAMERA_PANO_PROCESS_PARA_IN_STRUCT, *PCAMERA_PANO_PROCESS_PARA_IN_STRUCT;

typedef struct 
{
	kal_uint32 src_roi_x;				// start point x of ROI of source image
	kal_uint32 src_roi_y;				// start point y of ROI of source image
	kal_uint32 roi_w;					// width of ROI
	kal_uint32 roi_h;					// height of ROI
	kal_uint8  *jpeg_dst_buffer_addr;	// dest decoded jpeg buffer to clip to, RGB565
	kal_uint8  jpeg_dst_type;			/* RGB565/RGB888 */		
	kal_uint32 dst_width;				// dest decoded jpeg image size
	kal_uint32 dst_height;					
	kal_uint32 dst_roi_x;				// start point x of ROI of dest image
	kal_uint32 dst_roi_y;	
} CAMERA_PANO_CLIP_STRUCT;


typedef enum
{
	PANO_FEATURE_GET_INFO,
	PANO_FEATURE_GET_RESULT,
	PANO_FEATURE_GET_CUE_RATIO,
	PANO_FEATURE_SAVE_LOG,
	PANO_FEATURE_MAX
}	MSDK_PANO_FEATURE_ENUM;

typedef enum
{
	MSDK_CONTROL_CODE_CONFIG,
	MSDK_CONTROL_CODE_BGSTITCH,
	MSDK_CONTROL_CODE_STITCH,
	MSDK_CONTROL_CODE_STOP
}	MSDK_PANO_CONTROL_ENUM;

typedef enum
{
	PANO_STATE_IDLE,		// PANO driver is closed.
	PANO_STATE_STANDBY,	// PANO driver is opened
	PANO_STATE_READY,		// PANO driver is waiting data for processing
	PANO_STATE_BGSTITCH,	// PANO driver in background stitch.
	PANO_STATE_PROCESSING,     // PANO driver in stitch processing .
	PANO_STATE_STITCH_DONE
}MSDK_PANO_STATE_ENUM; 

enum img_color_fmt_rgb_enum_t
{
	IMG_COLOR_FMT_RGB565,
	IMG_COLOR_FMT_BGR565,
	IMG_COLOR_FMT_RGB888,
	IMG_COLOR_FMT_BGR888,
	IMG_COLOR_FMT_ARGB8888,
	IMG_COLOR_FMT_ABGR8888,
	IMG_COLOR_FMT_IRT1ARGB8888,
	IMG_COLOR_FMT_IRT3ARGB8888
};
#endif

