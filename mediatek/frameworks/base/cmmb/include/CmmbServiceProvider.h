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

#ifndef CMMB_SERVICE_PROVIDER_H
#define CMMB_SERVICE_PROVIDER_H

// CmmbServiceProvider.h

//#include "CmmbHelper.h"
//#include "CmmbParser.h"
//cmmb
#include "CmmbSPCommon.h"
#include "ICmmbSpObserver.h"
#include "CmmbSpDefs.h"


#define TS0_BPSK_DEMOD			0x04
#define CMMB_FREQ_POINT_BASE	13
#define CMMB_FREQ_POINT_INVALID	0
#define SERVICE_HANDLE_OFFSET	2
#define CMMB_DATA_ANOTHERCHANNEL
//#define CMMB_DATA_SERVICE


#ifdef WIN32
class CmmbEventObserver
{
public:
	virtual void HandleCmmbEvent(
		UINT32 eventId,	// event ID
		UINT8* payload,	// event payload
		UINT32 payloadLen	// payload length
	) = 0;

protected:
	~CmmbEventObserver() {}
};

bool CmmbSetEventObserver(CmmbEventObserver* observer, const char* name);

#else // Android
#include "ICmmbSpObserver.h"
using namespace android;
bool CmmbSetEventObserver(const sp<ICmmbSpObserver>& observer, const char* name);
#endif

bool CmmbInit(bool initUam);
bool CmmbModeSwitch(bool NewMode);
bool CmmbTerminate();
bool CmmbGetProp(UINT32 key, UINT32* value);
bool CmmbGetChipType(UINT32* chiptype);
CmmbResult CmmbGetErrorCode();
bool CmmbTune(UINT32 frequency, bool autoscan);

bool CmmbStartService(
	UINT32 serviceId, 
	UINT16 caSystemId,
	UINT16 videoKILen,
	UINT16 audioKILen,
	UINT32* serviceHdl,
	bool saveMfsFile,
	UINT32 dataserviceId
);

bool CmmbStopService(UINT32 serviceHdl);

UINT32 CmmbGetSignalQuality();                  // 1215

bool CmmbSetCaSaltKeys(
	UINT32 serviceHdl,
	const UINT8 videoSalt[CMMB_CA_SALT_SIZE],
	const UINT8 audioSalt[CMMB_CA_SALT_SIZE],
	const UINT8 dataSalt[CMMB_CA_SALT_SIZE]
);

bool CmmbSetCaControlWords(
	UINT32 serviceHdl,
	UINT32 sbufrmIdx, 
	const TCmmbCaCwPair& controlWords
);

bool CmmbUamDataExchange( 
	const UINT8* pWriteBuf, 
	UINT32 writeLen, 
	UINT8* pOutReadBuf, 
	UINT32 readBufSize, 
	UINT32* pOutReadLen,
	UINT16* pStatusWord
);

void SendESGStopEvent();
bool CmmbGetEsgFile(char filePath[CMMB_FILE_PATH_MAX]);
bool CmmbGetServiceFreq(UINT32 freq);
bool CmmbZeroSpiBuf();
bool CmmbAutoScan();

/*****************************************************************************
 * STREAMS IF Structure
 ****************************************************************************/  
typedef struct  _TCmmbVideoFrame{
  UINT16        VideoFrameLen;
  UINT8         VideoFrameType;
  UINT8         VideoSeq;
  UINT8         IsEnd;
  UINT8         ExistRelatePlayTime;
  UINT16        RelatePlayTime;
  UINT8*        VideoFrameBuf;
  UINT32       timestamp;
} TCmmbVideoFrame;

typedef struct _TCmmbAudioFrame{
  UINT16        AudioFrameLen;
  UINT8         AudioSeq;
  UINT16        RelatePlayTime;
  UINT8*        AudioFrameBuf;
  UINT32       timestamp;
} TCmmbAudioFrame;


typedef struct _CMMB_Metadata_Video{
	UINT8  video_algorithm;
	UINT16 video_bitrate;
	UINT8 video_x_coord;
	UINT8 video_y_coord;
	UINT8 video_disp_priority;
	UINT16 video_x_resolution;
	UINT16 video_y_resolution;
	UINT8 video_frame_rate;	
	cmmb_h264_dec_config* h264_dec_config[CMMB_MAX_ELEMENTARY_STREAMS];
}TVideoMetadata;

typedef struct _CMMB_Metadata_Audio{
	UINT8  audio_algorithm;
	UINT32 audio_sample_rate;
	UINT16 audio_bitrate;
}TAudioMetadata;

typedef struct _CMMB_Metadata{   
	/* video */
	TVideoMetadata* VideoMetadata;
	/* audio */
	TAudioMetadata* AudioMetadata;
}TCmmbMetadata;
extern "C" TCmmbAudioFrame* CmmbReadAudioFrame();
extern "C" void CmmbFreeAudioFrame(TCmmbAudioFrame* frame);
extern "C" TCmmbVideoFrame* CmmbReadVideoFrame();
extern "C" void CmmbFreeVideoFrame(TCmmbVideoFrame* frame);
extern "C" TCmmbMetadata* CmmbGetMetadata();
extern "C" void CmmbFlushAVFrame();
extern "C" void CmmbFlushOldestFrame();

#endif // CMMB_SERVICE_PROVIDER_H


