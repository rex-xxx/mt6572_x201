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

#ifndef ANDROID_ICMMBSP_H
#define ANDROID_ICMMBSP_H
 
#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include "CmmbSpDefs.h"
#include "ICmmbSpObserver.h"
#include "CmmbServiceProvider.h"
#include "CmmbSPCommon.h"

namespace android {

class Parcel;

class ICmmbSp: public IInterface
{
public:
    DECLARE_META_INTERFACE(CmmbSp);

	virtual bool	init(bool initUam = true) = 0;
	virtual bool     modeswitch(bool NewMode) = 0;
	virtual bool	term() = 0;
	virtual bool	getProp(int32_t key, int32_t& value) = 0;
	virtual bool	getChipType(int32_t& chiptype) = 0;
	virtual bool	setEventObserver(const sp<ICmmbSpObserver>& observer, char* name) = 0;
	virtual bool	tune(uint32_t frequency, bool autoscan) = 0;
	virtual bool	startService(uint32_t serviceId, uint16_t caSystemId,uint16_t videoKILen,uint16_t audioKILen, uint32_t& serviceHdl, bool saveMfsFile, uint32_t dataServceID) = 0;
	virtual bool	stopService(uint32_t serviceHdl) = 0;
	virtual bool     getServiceFreq(uint32_t freqId) = 0;
	virtual bool     zeroSpiBuf() = 0;
	virtual bool     autoScan() = 0;

	virtual bool	setCaSaltKeys(
						uint32_t serviceHdl,
						uint8_t videoSalt[CMMB_CA_SALT_SIZE],
						uint8_t audioSalt[CMMB_CA_SALT_SIZE],
						uint8_t dataSalt[CMMB_CA_SALT_SIZE]
					) = 0;

	virtual bool     setCaControlWords(
						uint32_t serviceHdl,
						TCmmbCaCwPair controlWords
					) = 0;

	virtual bool	uamDataExchange( 
						const uint8_t* pWriteBuf, 
						uint32_t writeLen, 
						uint8_t* pOutReadBuf, 
						uint32_t readBufSize, 
						uint32_t& outReadLen,
						uint16_t& statusWord
					) = 0;

	virtual bool	getEsgFile(char filePath[CMMB_FILE_PATH_MAX]) = 0;

       virtual CmmbResult getErrorCode() = 0;
    virtual bool CmmbReadAudioFrame(TCmmbAudioFrame* frame) = 0;
    virtual bool CmmbFreeAudioFrame(TCmmbAudioFrame* frame) = 0;
    virtual bool CmmbReadVideoFrame(TCmmbVideoFrame* frame) = 0;
    virtual bool CmmbFreeVideoFrame(TCmmbVideoFrame* frame) = 0;
    virtual bool CmmbGetMetaData(TCmmbMetadata* metadata) =0;
    virtual bool CmmbFlushAVFrame() = 0;
    virtual bool CmmbFlushOldestFrame() = 0;
    virtual bool CmmbReadFrameBuffer(void* srcbuf, void* dstbuf, uint32_t size) = 0;
};

// ----------------------------------------------------------------------------

class BnCmmbSp : public BBinder //BnInterface<ICmmbSp>
{
public:
    virtual status_t	onTransact( uint32_t code,
															const Parcel& data,
															Parcel* reply,
															uint32_t flags = 0);
    static  void			instantiate();
};

}; // namespace android

#endif // ANDROID_ICMMBSP_H
