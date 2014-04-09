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

//MTK_OP01_PROTECT_START
#ifndef CMMB_PROVIDER_TEST_H_

#define CMMB_PROVIDER_TEST_H_

#include <stdio.h>

#include <media/stagefright/MediaErrors.h>
#include <utils/threads.h>
#include <media/stagefright/FileSource.h>

namespace android {

#define MAX_H264_SPS_LEN  (100)
#define MAX_H264_PPS_LEN (100)
#define MAX_BUFFER  (5000 * 1024)//(100 * 1024)
#define PREFIX_SEI_SIZE  (25)//(9)//(25)

#define CMMB_AUDIO_ALGORITHM_HE_AAC 1
#define CMMB_AUDIO_ALGORITHM_AAC    2

#define CMMB_VIDEO_ALGORITHM_H264   1

struct TCmmbFrameHeader {
	uint32_t sample_count;  // num of this frame
	uint32_t time_stamp;    // time stamp of this frame
	uint32_t time_scale;    // timescale of this track.
	uint32_t frame_size;    // size of the frame    
};

typedef struct {
	uint32_t sps_count;                /**< number of SPS */
	uint32_t sps_len[1];               /**< array of SPS length */
	uint8_t sps[1][MAX_H264_SPS_LEN];  /**< array of SPS */
	uint32_t pps_count;                /**< number of PPS */
	uint32_t pps_len[1];               /**< array of PPS length */
	uint8_t pps[1][MAX_H264_PPS_LEN];  /**< array of PPS */
} h264_dec_config_t;

typedef struct {
	uint32_t algorithm_type;
	uint32_t sample_rate;
	uint32_t bitrate;    
} cmmb_subframe_audio_stream_t;

typedef struct {
    uint32_t algorithm_type;
    uint32_t bitrate;
    uint8_t x_coord;
    uint8_t y_coord;
    uint8_t disp_priority;
    uint16_t x_resolution;
    uint16_t y_resolution;
    uint8_t frame_rate;
    h264_dec_config_t* h264;
} cmmb_subframe_video_stream_t;

typedef struct {
    cmmb_subframe_video_stream_t* metadata_video;
    cmmb_subframe_audio_stream_t* metadata_audio;
} cmmb_metadata;






class CMMBFileSource : public RefBase {
public:
    CMMBFileSource();

    TCmmbFrameHeader* CmmbReadAudioFrame ();
    TCmmbFrameHeader* CmmbReadVideoFrame ();
    bool CmmbFreeFrame (TCmmbFrameHeader* frame);
    cmmb_metadata* CmmbGetMetadata ();
    bool CmmbFreeMetadata (cmmb_metadata* data);
	
protected:
     ~CMMBFileSource();

private:
     //FILE *  hSrcFile;
     sp<DataSource> filesource_audio;
     off_t filesize_audio;
     sp<DataSource> filesource_video;
     off_t filesize_video;
     
     h264_dec_config_t Fileh264;
     
     cmmb_metadata  FileMetadata;
     cmmb_subframe_video_stream_t Filemetadata_video;
     cmmb_subframe_audio_stream_t Filemetadata_audio;
     
     uint8_t * fullbuffer_audio;
     //uint32_t fullbuffer_size; 
     uint32_t readposition_fullbuffer_audio;
	 
     uint8_t * fullbuffer_video;
     //uint32_t fullbuffer_size; 
     uint32_t readposition_fullbuffer_video;
     
     uint8_t  AACHeader[3];   //find ADTS header.

     bool FindFrameBoundary(void *pStream,  uint32_t MaxBuffSize, uint32_t * frameEndByteOffset);
    CMMBFileSource(const CMMBFileSource &);
    CMMBFileSource &operator=(const CMMBFileSource &);
};

}  // namespace android

#endif  // CMMB_PROVIDER_TEST_H_

//MTK_OP01_PROTECT_END
