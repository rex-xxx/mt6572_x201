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

#ifndef _FTM_AUDIO_COMMON_H_
#define _FTM_AUDIO_COMMON_H_

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/mount.h>
#include <sys/statfs.h>
#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MIC1_OFF  0
#define MIC1_ON   1
#define MIC2_OFF  2
#define MIC2_ON   3

// for acoustic loopback
#define ACOUSTIC_STATUS   -1
#define DUAL_MIC_WITHOUT_DMNR_ACS_OFF 0
#define DUAL_MIC_WITHOUT_DMNR_ACS_ON  1
#define DUAL_MIC_WITH_DMNR_ACS_OFF   2
#define DUAL_MIC_WITH_DMNR_ACS_ON    3

#define Output_HS  0
#define Output_HP  1
#define Output_LPK 2

enum audio_device_in
{
	 BUILTIN_MIC,
	 WIRED_HEADSET,
	 MATV_ANALOG,
	 MATV_I2S
};

enum audio_devices
{
    // output devices
    OUT_EARPIECE = 0,
    OUT_SPEAKER = 1,
    OUT_WIRED_HEADSET = 2,
    DEVICE_OUT_WIRED_HEADPHONE = 3,
    DEVICE_OUT_BLUETOOTH_SCO = 4
};

#define WAVE_HEADER_SIZE (44)
#define WAVE_BUFFER_SIZE (4096)

typedef struct{
	unsigned int ChunkID;
	unsigned int ChunkSize;
	unsigned int Format;
	unsigned int Subchunk1ID;
	unsigned int Subchunk1IDSize;
	unsigned short AudioFormat;
	unsigned short NumChannels;
	unsigned int SampleRate;
	unsigned int ByteRate;
	unsigned short BlockAlign;
	unsigned short BitsPerSample;
	unsigned int SubChunk2ID;
	unsigned int SubChunk2Size;
}WaveHdr;

struct WavePlayData{
	char *FileName;
	FILE  *pFile;
	bool ThreadStart;
	bool ThreadExit;
	WaveHdr mWaveHeader;
	pthread_t WavePlayThread;
	int  i4Output;
};


///------------------the following is Factory mode test API------------------
//
//FUNCTION:
//		Common_Audio_init
//DESCRIPTION:
//		this function is called to init factory Audio
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int Common_Audio_init(void);

///------------------the following is Factory mode test API------------------
//
//FUNCTION:
//		Common_Audio_deinit
//DESCRIPTION:
//		this function is called to deinit factory Audio
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int Common_Audio_deinit(void);


///------------------the following is Factory mode test API------------------
//
//FUNCTION:
//		PhoneMic_Receiver_Loopback
//DESCRIPTION:
//		this function is called to test reciever loop back.
//
//PARAMETERS:
//		echoflag: 	[IN]	(char)true mean enable, otherwise 0 is disable
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int PhoneMic_Receiver_Loopback(char echoflag);
int PhoneMic_EarphoneLR_Loopback(char echoflag);
int PhoneMic_SpkLR_Loopback(char echoflag);
int HeadsetMic_EarphoneLR_Loopback(char bEnable, char bHeadsetMic);
int HeadsetMic_SpkLR_Loopback(char echoflag);
#ifdef FEATURE_FTM_ACSLB
///------------------the following is Factory mode test API------------------
//
//FUNCTION:
//		PhoneMic_Receiver_Acoustic_Loopback
//DESCRIPTION:
//		this function is called to test 2-mic acoustic loop back.
//
//PARAMETERS:
//		Acoustic_Type: 	[IN]	(int)0:acoustic loopback off; 1:dualmic acoustic loopback; 2:single mic acoustic loopback
//           Acoustic_Status     [IN] (int)
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int PhoneMic_Receiver_Acoustic_Loopback(int Acoustic_Type, int *Acoustic_Status_Flag, int bHeadset_Output);
#endif

//-------------------------------------------------------------------
//FUNCTION:
//		RecieverTest
//DESCRIPTION:
//		this function is called to test reciever channel using inner sine wave.
//
//PARAMETERS:
//		receiver_test: 	[IN]	(char)true mean enable, otherwise 0 is disable
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
int RecieverTest(char receiver_test);

//-------------------------------------------------------------------
//FUNCTION:
//		LouderSPKTest
//DESCRIPTION:
//		this function is called to test loud speaker channel using inner sine wave.
//
//PARAMETERS:
//		left_channel/right_channel: 	[IN]	(char)true mean enable, otherwise 0 isturnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//------------------------------------------------------------------

int LouderSPKTest(char left_channel, char right_channel);

//read record data while recording
int readRecordData(void * pbuffer,int bytes);
bool recordInit(int device_in);

//-------------------------------------------------------------------
//FUNCTION:
//		EarphoneTest
//DESCRIPTION:
//		this function is called to test earphone speaker channel using inner sinewave.
//
//PARAMETERS:
//		bEnable: 	[IN]	(char)true mean enable, otherwise 0 is turnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int EarphoneTest(char bEnable);

//-------------------------------------------------------------------
//FUNCTION:
//		FMLoopbackTest
//DESCRIPTION:
//		this function is called to enable or disable FM channel.
//
//PARAMETERS:
//		bEnable: 	[IN]	(char)true mean enable, otherwise 0 is turnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

/// bEnable true mean enable, otherwise 0 is disable
int FMLoopbackTest(char bEnable);

//-------------------------------------------------------------------
//FUNCTION:
//		EarphoneMicbiasEnable
//DESCRIPTION:
//		this function is called to Enable Mic bias enable when earphone is insert.
//
//PARAMETERS:
//		bMicEnable: 	[IN]	(char)true mean enable, otherwise 0 is turnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

/// bMicEnable true mean enable, otherwise 0 is disable
//int EarphoneMicbiasEnable(int bMicEnable);

//-------------------------------------------------------------------
//FUNCTION:
//		EarphoneEnable
//DESCRIPTION:
//		this function is called to Enable Mic  enable when earphone is insert.
//
//PARAMETERS:
//		bMicEnable: 	[IN]	(char)true mean enable, otherwise 0 is turnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

/// bMicEnable true mean enable, otherwise 0 is disable

int EarphoneMicEnable(int bMicEnable);

//-------------------------------------------------------------------
//FUNCTION:
//		EarphoneEnable
//DESCRIPTION:
//		this function is called to Enable Mic  enable when earphone is insert.
//
//PARAMETERS:
//		bMicEnable: 	[IN]	(char)true mean enable, otherwise 0 is turnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

/// bMicEnable true mean enable, otherwise 0 is disablez


//-------------------------------------------------------------------
//FUNCTION:
//		Audio_Write_Vibrate_On
//DESCRIPTION:
//		this function will gen sineave and write to audio hardware
//
//PARAMETERS:
//		millisecond: 	[IN]	mill asecond to write data
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
int Audio_Write_Vibrate_On(int millisecond);

//-------------------------------------------------------------------
//FUNCTION:
//		Audio_Write_Vibrate_Off
//DESCRIPTION:
//		this function stop gen sineave and write to audio hardware
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
int Audio_Write_Vibrate_Off(void);


//-------------------------------------------------------------------
//FUNCTION:
//		ATV_AudioWrite
//DESCRIPTION:
//		Write PCM data to audio HW
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
unsigned int ATV_AudioWrite(void* buffer, unsigned int bytes);

//-------------------------------------------------------------------
//FUNCTION:
//		ATV_AudPlay_On
//DESCRIPTION:
//		Start the mATV audio play
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
int ATV_AudPlay_On(void);

//-------------------------------------------------------------------
//FUNCTION:
//		ATV_AudPlay_Off
//DESCRIPTION:
//		Stop the mATV audio play
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
int ATV_AudPlay_Off(void);

//-------------------------------------------------------------------
//FUNCTION:
//		ATV_AudAnalogPath
//DESCRIPTION:
//		Audio Analog Line in play
//
//PARAMETERS:
//
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------
int ATV_AudAnalogPath(char bEnable);

//-------------------------------------------------------------------
//FUNCTION:
//		Audio_I2S_Play
//DESCRIPTION:
//		Audio_I2S_Play
//
//PARAMETERS:
//
//
//RETURN VALUE:
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int Audio_I2S_Play(int enable_flag);
int Audio_MATV_I2S_Play(int enable_flag);
int Audio_FMTX_Play(bool Enable, unsigned int Freq);

int Audio_HDMI_Play(bool Enable, unsigned int Freq);

//-------------------------------------------------------------------
//FUNCTION:
//		Audio_Wave_playback
//DESCRIPTION:
//		Audio_Wave_playback
//
//PARAMETERS:
//
//
//RETURN VALUE:
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int Audio_Wave_playback(void* arg);

//-------------------------------------------------------------------
//FUNCTION:
//		Audio_READ_SPK_OC_STA
//DESCRIPTION:
//		Audio_READ_SPK_OC_STA
//
//PARAMETERS:
//
//
//RETURN VALUE:
//
//DEPENDENCY:
//		Common_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//-------------------------------------------------------------------

int Audio_READ_SPK_OC_STA(void);

//-------------------------------------------------------------------
//FUNCTION:
//		LouderSPKOCTest
//DESCRIPTION:
//		this function is called to test OC function while loud speaker channel uses inner sine wave.
//
//PARAMETERS:
//		left_channel/right_channel: 	[IN]	(char)true mean enable, otherwise 0 isturnoff
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		META_Audio_init must be called before
//
//GLOBALS AFFECTED
//		None
//------------------------------------------------------------------

int LouderSPKOCTest(char left_channel, char right_channel);

#if defined(MTK_VIBSPK_SUPPORT)
//-------------------------------------------------------------------
//FUNCTION:
//		Audio_VSCurrent_Enable
//DESCRIPTION:
//		this function is called to enable/disable vibspk process
//------------------------------------------------------------------
void Audio_VSCurrent_Enable(bool enable);
//-------------------------------------------------------------------
//FUNCTION:
//		Audio_VSCurrent_GetFrequency
//DESCRIPTION:
//		this function is called to get the center frequency of vibspk
//------------------------------------------------------------------
int Audio_VSCurrent_GetFrequency();
//-------------------------------------------------------------------
//FUNCTION:
//		Audio_VSCurrent_WriteRoutine
//DESCRIPTION:
//		this function is called to write silence data to audio HW
//------------------------------------------------------------------
void Audio_VSCurrent_WriteRoutine();
#endif   //#if defined(MTK_VIBSPK_SUPPORT)
#ifdef __cplusplus
};
#endif


#endif
