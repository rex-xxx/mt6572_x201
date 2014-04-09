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

#ifndef CMMB_SPCOMMON_H
#define CMMB_SPCOMMON_H

// CmmbSPCommon.h

/***********/
/* Defines    */
/***********/
typedef unsigned char		UINT8;
typedef signed char		INT8;
typedef unsigned short		UINT16;
typedef short				INT16;
typedef unsigned int		UINT32;
typedef int				INT32;
typedef long long			INT64;
typedef unsigned long long	UINT64;
typedef unsigned long		BOOL;

#define FALSE			0
#define TRUE				1
#define null 				0
#define assert


/********************/
/*    error define          */
/********************/

typedef enum
{
	//service provider use
	CMMB_S_OK 							= 0x00000000,
	CMMB_S_ALREADY_ACTIVE			= 0x00000001,
	CMMB_E_UNKNOWN					= 0x00000002,
	CMMB_E_INVALID_ARG				= 0x00000003,
	CMMB_E_TIMEOUT					= 0x00000004,
	CMMB_E_MEM_ALLOC_FAILED    		= 0x00000005,
	CMMB_E_WRONGSTATE				= 0x00000006,

	//parser use
	CMMB_PARSER_S_OK 		  	    		= 0x60000000,
	CMMB_PARSER_E_DATA_CORRUPTED	= 0x60000001,
	CMMB_PARSER_E_INVALID_ARG		= 0x60000002,
	CMMB_PARSER_E_ALREADY_PLAYED	= 0x60000003,
	CMMB_PARSER_E_FAILED_CRC_CHECK	= 0x60000004,
	CMMB_PARSER_E_NOT_SUPPORTED	= 0x60000005,
	CMMB_PARSER_E_EXCEEDED_ALLOCATED_MEMORY= 0x60000006,
	CMMB_PARSER_E_ILLEGAL_DATA		= 0x60000007,
	CMMB_PARSER_E_FALSE				= 0x60000008,
	
	
	//ESG parser use
	CMMB_ESG_S_IN_PROGRESS      	 = 0x70000000,
	CMMB_ESG_E_BAD_DATA 			 = 0x70000001,
	CMMB_ESG_E_TABLE_NOT_FOUND  = 0x70000002,

	//EB parser use
	CMMB_EB_S_COMPLETE  			= 0x80000000
}CmmbResult;


/* audio algorithem */
#define CMMB_AUDIO_ALGORITHM_DRA    0
#define CMMB_AUDIO_ALGORITHM_HE_AAC 1
#define CMMB_AUDIO_ALGORITHM_AAC    2

/* video algorithem */
#define CMMB_VIDEO_ALGORITHM_AVS    0
#define CMMB_VIDEO_ALGORITHM_H264   1

#define MAX_H264_SPS_LEN    64
#define MAX_H264_PPS_LEN    64
#define CMMB_MAX_ELEMENTARY_STREAMS 7

//H.264 decoder configuration
typedef struct cmmb_h264_dec_config_t
{
	UINT32 configuration_version;
	UINT32 avc_profile_indication;
	UINT32 profile_compatibility;
	UINT32 avc_level_indication;
	UINT32 length_size_minus_one;

	INT32 sps_count;                /**< number of SPS */
	INT32 sps_len[1];               /**< array of SPS length */
	UINT8 sps[1][MAX_H264_SPS_LEN]; /**< array of SPS */

	INT32 pps_count;                /**< number of PPS */
	INT32 pps_len[1];               /**< array of PPS length */
	UINT8 pps[1][MAX_H264_PPS_LEN]; /**< array of PPS */
} cmmb_h264_dec_config;

#endif // CMMB_SPCOMMON_H
