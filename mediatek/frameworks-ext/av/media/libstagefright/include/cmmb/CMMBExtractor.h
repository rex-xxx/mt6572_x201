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
#ifndef CMMB_EXTRACTOR_H_

#define CMMB_EXTRACTOR_H_


#include <media/stagefright/MediaExtractor.h>
#include <utils/Vector.h>
#include "CmmbServiceProvider.h"

namespace android {

#define ADTS_HEADER_LENGTH_CMMB                (7)

const uint32_t ADTSSampleFreqTable[16] = {

	96000, /* 96000 Hz */
    88200, /* 88200 Hz */
    64000, /* 64000 Hz */
    48000, /* 48000 Hz */
    44100, /* 44100 Hz */
    32000, /* 32000 Hz */
    24000, /* 24000 Hz */
    22050, /* 22050 Hz */
    16000, /* 16000 Hz */
    12000, /* 12000 Hz */
    11025, /* 11025 Hz */
    8000, /*  8000 Hz */
    7350, /*  7350 Hz */
    -1, /* future use */
    -1, /* future use */
    -1  /* escape value */
};

class DataSource;
class MediaSource;
class Metadata;

class CMMBExtractor : public MediaExtractor {
public:

    CMMBExtractor(const sp<DataSource> &source);

    virtual size_t countTracks();
    virtual sp<MediaSource> getTrack(size_t index);
    virtual sp<MetaData> getTrackMetaData(size_t index, uint32_t flags);

    virtual sp<MetaData> getMetaData();

protected:
    virtual ~CMMBExtractor();

private:
    struct Track {
        sp<MetaData> meta;
        uint32_t timescale;
    };


	struct ESDSStruct_Audio {
		uint8_t   tag_esdescriptor;  //kTag_ESDescriptor 
		uint8_t   size_esdescriptor; //largest is 127  total size of esdescriptior.
		uint16_t es_id;  //"es1"
		uint8_t   flag;    // 0
		uint8_t   tag_DecoderConfigDescriptor; //kTag_DecoderConfigDescriptor
		uint8_t   size_DecoderConfigDescriptor; //largest is 127  total size of decoderconfigdescriptior.
		uint8_t   ObjectTypeIndication; //32
		uint8_t   reserve[12];
		uint8_t   tag_decoderspecificinfo;//kTag_DecoderSpecificInfo
		uint8_t   size_specificInfo; //specific info size;
		//uint8_t * specificInfo;
	};

#if 0
    struct ESDSStruct {
		uint8_t   tag_esdescriptor;  //kTag_ESDescriptor 
		uint8_t   size_esdescriptor; //largest is 127  total size of esdescriptior.
		uint16_t es_id;  //"es1"
		uint8_t   flag;    // 0
		uint8_t   tag_DecoderConfigDescriptor; //kTag_DecoderConfigDescriptor
		uint8_t   size_DecoderConfigDescriptor; //largest is 127  total size of decoderconfigdescriptior.
		uint8_t   ObjectTypeIndication; //32
		uint8_t   reserve[12];
		uint8_t   tag_decoderspecificinfo;//kTag_DecoderSpecificInfo
		uint8_t   size_specificInfo; //specific info size;
		//uint8_t * specificInfo;
    };
#else
    struct ESDSStruct {   //keyacc
		uint8_t version;  //configurationVersion == 1
		uint8_t profile;
		uint8_t reserve1;
		uint8_t level;
		uint8_t reserve2;
		uint8_t numsps;   //max is 31
		uint16_t spslength;  //just support only one;
		//uint8_t* sps;
    };

    struct ESDSSuffixStruct {
		uint8_t numpps;
		uint16_t ppslength; //just support only one.
		//uint8_t *pps;	
    };
#endif

    Track *VideoTrack;
    Track *AudioTrack;
    //ESDSStruct esds;
    sp<DataSource> mDataSource;
    sp<MetaData> CMMBMetaData;
    bool mHaveMetadata;  //already parse cmmbmetadata to metadata structure.
    
    status_t readMetaData();
    CMMBExtractor(const CMMBExtractor &);
    CMMBExtractor &operator=(const CMMBExtractor &);
};

#define MAX_CMMB_VIDEO_FRAMESIZE (100 * 1024)
#define MAX_CMMB_AUDIO_FRAMESIZE (100 * 1024)

}  // namespace android

#endif  // CMMB_EXTRACTOR_H_

//MTK_OP01_PROTECT_END
