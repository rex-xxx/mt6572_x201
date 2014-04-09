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


#ifndef _APP_GIA_H
#define _APP_GIA_H


#include "MTKGia.h"
#include "Gia_Core.h"
#define GIA_DEBUG

#define IMG_DESCRIPTOR_TYPE_X			1			// 0: Gradient Response 1 Gradient Vector (for Directions) 
#define IMG_DATA_PROJECTION_ND_X		4			// from 1~4 // if DESCRIPTOR_TYPE_X = 1, set 2 here, otherwise 4 for image
#define IMG_MATCH_SAMPLE_TYPE_X			1			// 0: Regular,	1: Content-aware
#define IMG_MATCH_SAMPLE_RATE_X			4			// Sample rate, no larger than 8, [1-8]

#define VDO_DESCRIPTOR_TYPE_X			0			// 0: Gradient Response 1 Gradient Vector (for Directions)
#define VDO_DATA_PROJECTION_ND_X		4			// from 1~4 // if DESCRIPTOR_TYPE_X = 1, set 2 here, otherwise 4 for image
#define VDO_MATCH_SAMPLE_TYPE_X			1			// 0: Regular,	1: Content-aware
#define VDO_MATCH_SAMPLE_RATE_X			4			// Sample rate, no larger than 8, [1-8]

// Could be changed by tester
#define MATCH_SEARCH_TYPE_Y				0			// 0: Sequential, 1: Jumpping
#define MATCH_SEARCH_STEP_Y				1			// from 1~4, no larger than 4 
#define MATCH_SEARCH_RANGE_Y			0.028		// percentage, will multiplied by image height, 0.01-0.05

#define MATCH_SEARCH_TYPE_X				1			// 0: Sequential, 1: Jumpping
#define MATCH_SEARCH_STEP_X				4			// from 1~4, no larger than 4 , [1-4]
#define MATCH_SEARCH_RANGE_X_L			0.25		// percentage, will multiplied by image width, 0.01-0.3

// 2012-0518 ---
#define MATCH_SEARCH_RANGE_X_R_VDO		0.09		// percentage, will multiplied by image width, 0.01-0.1
#define MATCH_SEARCH_RANGE_X_R_IMG		0.14		// percentage, will multiplied by image width, 0.01-0.3
// 2012-0518

#define THR_SMOOTHNESS_GRAD				1.85
#define BLOCK_SIZE_PERCENTAGE_X			0.1125		// percentage, will multiplied by image height,bound [0.1~0.5]

// 2012-0518 ---
#define BLOCK_SIZE_PERCENTAGE_Y_VDO		0.2			// percentage, will multiplied by image height,bound [0.1~0.5]
#define BLOCK_SIZE_PERCENTAGE_Y_IMG		0.31		// percentage, will multiplied by image height,bound [0.1~0.5]
// 2012-0518

#define N_SAMPLE_ROWS					3			// number of sample rows, bound 1~4
#define N_SAMPLE_COLS					5			// number of sample cols, bound 1~6

#define THR_MATCHED_RANGE				3			// hot-zone: define matched region for cross-verification
#define THR_RESCUING_RANGE				10			// rescuring-zone: test +/- 1 pixel
#define THR_SUPPORT_RANGE_BG			20			// used for outlier estimation, bound >=0
#define THR_SUPPORT_RANGE_FG			40			// used for outlier estimation, bound >=0

//------------------- 2012-0510 -------------------------------------------
#define IMG_CONVERGENCE_SPEED			2			// 0: Slow 1:Normal 2:Fast
#define VDO_CONVERGENCE_SPEED			1			// 
#define MOVING_RATIO					20			// > 0
#define TOTAL_CALCULATION_BOUND			2500		// defined for 720p (a 1280x720 side-by-side image), each match compare 72 elements

// 2012-0605
#define CONVERGENCE_MIN_INC				-12
#define CONVERGENCE_MAX_INC				24
#define CONVERGENCE_DEF_INC				10			// 2012-0711
#define CONVERGENCE_MIN_DEGREE			2			// RANGE 1~4(-12, -24, -36, -48), minimal convergence for foreground
#define CONVERGENCE_MAX_DEGREE			2			// RANGE 1~4( 24,  48,  72,  96), maximal convergence for background
#define CONVERGENCE_DEF_DEGREE			0			// RANGE -3~3( 30, 20, 10, 0, -10, -20, -30), default convergence is defined for width=1280, 2012-0711
#define IMG_CONVERGENCE_EFFECT			1			// 0: Normal 1:Strong
#define VDO_CONVERGENCE_EFFECT			0
#define IMG_CONVERGENCE_SENSING			2			// 0: Slow 1:Normal 2:Fast
#define VDO_CONVERGENCE_SENSING			1
//


#define GIA_LOG_BUFFER_SIZE 636
/*****************************************************************************
	Class Define
******************************************************************************/
class AppGia : public MTKGia {
public:    
    static MTKGia* getInstance();
    virtual void destroyInstance();
    
    AppGia();
    virtual ~AppGia();   
    // Process Control
    MRESULT GiaInit(void* InitInData);
    MRESULT GiaMain();
    MRESULT GiaReset();   //Reset
            
	// Feature Control        
	MRESULT GiaFeatureCtrl(MUINT32 FeatureID, void* pParaIn, void* pParaOut);
private:
};


#endif
