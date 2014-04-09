/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "ARTPSource"
#include <utils/Log.h>

#include "ARTPSource.h"

#include "AAMRAssembler.h"
#include "AAVCAssembler.h"
#include "AH263Assembler.h"
#include "AMPEG2TSAssembler.h"
#include "AMPEG4AudioAssembler.h"
#include "AMPEG4ElementaryAssembler.h"
#include "ARawAudioAssembler.h"
#include "ASessionDescription.h"
#ifndef ANDROID_DEFAULT_CODE
#include "APacketSource.h"
#include "AnotherPacketSource.h" //for bitrate adaptation
#endif

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>

namespace android {

static const uint32_t kSourceID = 0xdeadbeef;

#ifndef ANDROID_DEFAULT_CODE 
static bool GetClockRate(const AString &desc, uint32_t *clockRate) {
    ssize_t slashPos = desc.find("/");
    if (slashPos < 0) {
        return false;
    }

    const char *s = desc.c_str() + slashPos + 1;

    char *end;
    unsigned long x = strtoul(s, &end, 10);

    if (end == s || (*end != '\0' && *end != '/')) {
        return false;
    }

    *clockRate = x;

    return true;
}
#endif // #ifndef ANDROID_DEFAULT_CODE

ARTPSource::ARTPSource(
        uint32_t id,
        const sp<ASessionDescription> &sessionDesc, size_t index,
        const sp<AMessage> &notify)
    : mID(id),
      mHighestSeqNumber(0),
      mNumBuffersReceived(0),
      mLastNTPTime(0),
      mLastNTPTimeUpdateUs(0),
      mIssueFIRRequests(false),
      mLastFIRRequestUs(-1),
      mNextFIRSeqNo((rand() * 256.0) / RAND_MAX),
#ifndef ANDROID_DEFAULT_CODE 
      mNotify(notify),
      mEstablished(false),
      mHighestSeqNumberSet(false),
      mLastPacketRtpTime(0),
      mLastPacketRecvTimeUs(0),
      mUIInterarrivalJitter(0),
      mDInterarrivalJitter(0),
      mNumLastRRPackRecv(0),
      mLastRRPackRecvSeqNum(0),
      mFirstPacketSeqNum(0),
      mExpectedTimeoutUs(0) {
#else
      mNotify(notify) {
#endif // #ifndef ANDROID_DEFAULT_CODE
    unsigned long PT;
    AString desc;
    AString params;
    sessionDesc->getFormatType(index, &PT, &desc, &params);
#ifndef ANDROID_DEFAULT_CODE 
    CHECK(GetClockRate(desc, &mClockRate));
#endif // #ifndef ANDROID_DEFAULT_CODE

    if (!strncmp(desc.c_str(), "H264/", 5)) {
        mAssembler = new AAVCAssembler(notify);
        mIssueFIRRequests = true;
    } else if (!strncmp(desc.c_str(), "MP4A-LATM/", 10)) {
        mAssembler = new AMPEG4AudioAssembler(notify, params);
    } else if (!strncmp(desc.c_str(), "H263-1998/", 10)
            || !strncmp(desc.c_str(), "H263-2000/", 10)) {
        mAssembler = new AH263Assembler(notify);
        mIssueFIRRequests = true;
    } else if (!strncmp(desc.c_str(), "AMR/", 4)) {
        mAssembler = new AAMRAssembler(notify, false /* isWide */, params);
    } else  if (!strncmp(desc.c_str(), "AMR-WB/", 7)) {
        mAssembler = new AAMRAssembler(notify, true /* isWide */, params);
    } else if (!strncmp(desc.c_str(), "MP4V-ES/", 8)
            || !strncasecmp(desc.c_str(), "mpeg4-generic/", 14)) {
        mAssembler = new AMPEG4ElementaryAssembler(notify, desc, params);
        mIssueFIRRequests = true;
    } else if (ARawAudioAssembler::Supports(desc.c_str())) {
        mAssembler = new ARawAudioAssembler(notify, desc.c_str(), params);
    } else if (!strncasecmp(desc.c_str(), "MP2T/", 5)) {
        mAssembler = new AMPEG2TSAssembler(notify, desc.c_str(), params);
    } else {
        TRESPASS();
    }
}

static uint32_t AbsDiff(uint32_t seq1, uint32_t seq2) {
    return seq1 > seq2 ? seq1 - seq2 : seq2 - seq1;
}

void ARTPSource::processRTPPacket(const sp<ABuffer> &buffer) {
    if (queuePacket(buffer) && mAssembler != NULL) {
#ifndef ANDROID_DEFAULT_CODE
        mAssembler->updatePacketReceived(this, buffer);
        if (mEstablished)
#endif
        mAssembler->onPacketReceived(this);
    }
}

void ARTPSource::timeUpdate(uint32_t rtpTime, uint64_t ntpTime) {
    mLastNTPTime = ntpTime;
    mLastNTPTimeUpdateUs = ALooper::GetNowUs();

    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("time-update", true);
    notify->setInt32("rtp-time", rtpTime);
    notify->setInt64("ntp-time", ntpTime);
    notify->post();
}

#ifndef ANDROID_DEFAULT_CODE 
static uint32_t extendSeqNumber(uint32_t seqNum, uint32_t mHighestSeqNumber) {
    uint32_t seq1 = seqNum | (mHighestSeqNumber & 0xffff0000);
    uint32_t seq2 = seqNum | ((mHighestSeqNumber & 0xffff0000) + 0x10000);
    uint32_t seq3 = seqNum | ((mHighestSeqNumber & 0xffff0000) - 0x10000);
    uint32_t diff1 = AbsDiff(seq1, mHighestSeqNumber);
    uint32_t diff2 = AbsDiff(seq2, mHighestSeqNumber);
    uint32_t diff3 = AbsDiff(seq3, mHighestSeqNumber);

    if (diff1 < diff2) {
        if (diff1 < diff3) {
            // diff1 < diff2 ^ diff1 < diff3
            seqNum = seq1;
        } else {
            // diff3 <= diff1 < diff2
            seqNum = seq3;
        }
    } else if (diff2 < diff3) {
        // diff2 <= diff1 ^ diff2 < diff3
        seqNum = seq2;
    } else {
        // diff3 <= diff2 <= diff1
        seqNum = seq3;
    }
    return seqNum;
}
#endif // #ifndef ANDROID_DEFAULT_CODE

bool ARTPSource::queuePacket(const sp<ABuffer> &buffer) {
    uint32_t seqNum = (uint32_t)buffer->int32Data();

#ifndef ANDROID_DEFAULT_CODE 
	//calculate interarrival jitter
	int32_t iArrivalJitter; 

	uint32_t uiRtpTimeStamp = 0;
	sp<AMessage> meta_pack = buffer->meta();
	CHECK(meta_pack->findInt32("rtp-time", (int32_t *)&uiRtpTimeStamp));

	int64_t iPacketRecvTimeUs = ALooper::GetNowUs();

	if((mLastPacketRtpTime == 0) && (mLastPacketRecvTimeUs ==0)){
		ALOGD("queuePacket,mLastPacketRtpTime == 0,uiRtpTimeStamp=%d,mLastPacketRecvTimeUs=0,iPacketRecvTimeUs=%lld",\
			uiRtpTimeStamp,iPacketRecvTimeUs);
		mLastPacketRtpTime = uiRtpTimeStamp;
		mLastPacketRecvTimeUs = iPacketRecvTimeUs;
	}else{
		int32_t rtpTs_diff = uiRtpTimeStamp - mLastPacketRtpTime; //count in clock tick
		int32_t rec_diff = (iPacketRecvTimeUs - mLastPacketRecvTimeUs)* mClockRate /1000000LL ; //change to count in clock tick
				
		iArrivalJitter = rtpTs_diff > rec_diff ? rtpTs_diff - rec_diff : rec_diff - rtpTs_diff;
		
		mDInterarrivalJitter += (double)((iArrivalJitter - mDInterarrivalJitter)/16.0);
		mUIInterarrivalJitter = (uint32_t)(mDInterarrivalJitter + 0.5);

		mLastPacketRtpTime = uiRtpTimeStamp;
		mLastPacketRecvTimeUs = iPacketRecvTimeUs;
	}	
#endif // #ifndef ANDROID_DEFAULT_CODE

    if (mNumBuffersReceived++ == 0) {
#ifndef ANDROID_DEFAULT_CODE 
        if (!mHighestSeqNumberSet) {
            ALOGI("set highest %d", seqNum);
        mHighestSeqNumber = seqNum;
        }
		mFirstPacketSeqNum = seqNum;
#else
        mHighestSeqNumber = seqNum;
#endif // #ifndef ANDROID_DEFAULT_CODE
        mQueue.push_back(buffer);
        return true;
    }

#ifndef ANDROID_DEFAULT_CODE
    seqNum = extendSeqNumber(seqNum, mHighestSeqNumber);
#else
    // Only the lower 16-bit of the sequence numbers are transmitted,
    // derive the high-order bits by choosing the candidate closest
    // to the highest sequence number (extended to 32 bits) received so far.

    uint32_t seq1 = seqNum | (mHighestSeqNumber & 0xffff0000);
    uint32_t seq2 = seqNum | ((mHighestSeqNumber & 0xffff0000) + 0x10000);
    uint32_t seq3 = seqNum | ((mHighestSeqNumber & 0xffff0000) - 0x10000);
    uint32_t diff1 = AbsDiff(seq1, mHighestSeqNumber);
    uint32_t diff2 = AbsDiff(seq2, mHighestSeqNumber);
    uint32_t diff3 = AbsDiff(seq3, mHighestSeqNumber);

    if (diff1 < diff2) {
        if (diff1 < diff3) {
            // diff1 < diff2 ^ diff1 < diff3
            seqNum = seq1;
        } else {
            // diff3 <= diff1 < diff2
            seqNum = seq3;
        }
    } else if (diff2 < diff3) {
        // diff2 <= diff1 ^ diff2 < diff3
        seqNum = seq2;
    } else {
        // diff3 <= diff2 <= diff1
        seqNum = seq3;
    }
#endif

    if (seqNum > mHighestSeqNumber) {
        mHighestSeqNumber = seqNum;
    }

    buffer->setInt32Data(seqNum);

    List<sp<ABuffer> >::iterator it = mQueue.begin();
    while (it != mQueue.end() && (uint32_t)(*it)->int32Data() < seqNum) {
        ++it;
    }

    if (it != mQueue.end() && (uint32_t)(*it)->int32Data() == seqNum) {
        ALOGW("Discarding duplicate buffer");
        return false;
    }

    mQueue.insert(it, buffer);

#ifndef ANDROID_DEFAULT_CODE
    if (!mEstablished) {
        if (mQueue.size() >= kVotePacketNumber) {
            int32_t seq = (*mQueue.begin())->int32Data();
            mAssembler->setNextExpectedSeqNo(seq);
            mEstablished = true;
            ALOGI("vote establish %d seq no %d", mQueue.size(), seq);
        }
    }
#endif
    return true;
}

void ARTPSource::byeReceived() {
    mAssembler->onByeReceived();
}

void ARTPSource::addFIR(const sp<ABuffer> &buffer) {
    if (!mIssueFIRRequests) {
        return;
    }

    int64_t nowUs = ALooper::GetNowUs();
    if (mLastFIRRequestUs >= 0 && mLastFIRRequestUs + 5000000ll > nowUs) {
        // Send FIR requests at most every 5 secs.
        return;
    }

    mLastFIRRequestUs = nowUs;

    if (buffer->size() + 20 > buffer->capacity()) {
        ALOGW("RTCP buffer too small to accomodate FIR.");
        return;
    }

    uint8_t *data = buffer->data() + buffer->size();

    data[0] = 0x80 | 4;
    data[1] = 206;  // PSFB
    data[2] = 0;
    data[3] = 4;
    data[4] = kSourceID >> 24;
    data[5] = (kSourceID >> 16) & 0xff;
    data[6] = (kSourceID >> 8) & 0xff;
    data[7] = kSourceID & 0xff;

    data[8] = 0x00;  // SSRC of media source (unused)
    data[9] = 0x00;
    data[10] = 0x00;
    data[11] = 0x00;

    data[12] = mID >> 24;
    data[13] = (mID >> 16) & 0xff;
    data[14] = (mID >> 8) & 0xff;
    data[15] = mID & 0xff;

    data[16] = mNextFIRSeqNo++;  // Seq Nr.

    data[17] = 0x00;  // Reserved
    data[18] = 0x00;
    data[19] = 0x00;

    buffer->setRange(buffer->offset(), buffer->size() + 20);

    ALOGV("Added FIR request.");
}

#ifndef ANDROID_DEFAULT_CODE
static void fakeSSRC(int ssrc, uint8_t* data) {
    ssrc = ~ssrc;
    data[0] = ssrc >> 24;
    data[1] = (ssrc >> 16) & 0xff;
    data[2] = (ssrc >> 8) & 0xff;
    data[3] = ssrc & 0xff;
}
#endif // #ifndef ANDROID_DEFAULT_CODE

void ARTPSource::addReceiverReport(const sp<ABuffer> &buffer) {
    if (buffer->size() + 32 > buffer->capacity()) {
        ALOGW("RTCP buffer too small to accomodate RR.");
        return;
    }

    uint8_t *data = buffer->data() + buffer->size();

    data[0] = 0x80 | 1;
    data[1] = 201;  // RR
    data[2] = 0;
    data[3] = 7;
#ifndef ANDROID_DEFAULT_CODE 
    // maybe we should move RR header + our SSRC out of this function
    fakeSSRC(mID, data + 4);
#else
    data[4] = kSourceID >> 24;
    data[5] = (kSourceID >> 16) & 0xff;
    data[6] = (kSourceID >> 8) & 0xff;
    data[7] = kSourceID & 0xff;
#endif // #ifndef ANDROID_DEFAULT_CODE

    data[8] = mID >> 24;
    data[9] = (mID >> 16) & 0xff;
    data[10] = (mID >> 8) & 0xff;
    data[11] = mID & 0xff;

#ifndef ANDROID_DEFAULT_CODE
	//fractin lost--start
	uint8_t fractionlost = 0;
	int32_t iPacketLostSinceLastRR = 0;
	if( mNumLastRRPackRecv == 0) {
		//haven't sent a RR before
		mLastRRPackRecvSeqNum = mHighestSeqNumber;
		mNumLastRRPackRecv = mNumBuffersReceived;
		fractionlost = 0;
	} else{
		iPacketLostSinceLastRR = (mHighestSeqNumber - mLastRRPackRecvSeqNum)- \
										( mNumBuffersReceived - mNumLastRRPackRecv);
		if(iPacketLostSinceLastRR > 0)
			fractionlost = (iPacketLostSinceLastRR * 256)/(mHighestSeqNumber - mLastRRPackRecvSeqNum);
		else
			fractionlost = 0;
	}
	
	data[12] = fractionlost;	
	mNumLastRRPackRecv = mNumBuffersReceived;
	mLastRRPackRecvSeqNum = mHighestSeqNumber;
	//fractin lost--end

	//cumulative lost --start
        const uint32_t SIGN_EXTENSION = 0xFF800000;
	const uint32_t CUMULATIVE_LOST_MAX = 0x007FFFFF;
	int32_t iCumulativeLost = (mHighestSeqNumber - mFirstPacketSeqNum +1) - mNumBuffersReceived ;
	if(iCumulativeLost < 0){
		//iCumulativeLost |= SIGN_EXTENSION;
		iCumulativeLost = 0;
	}
	if(iCumulativeLost > (int32_t)CUMULATIVE_LOST_MAX){
		iCumulativeLost = CUMULATIVE_LOST_MAX;
	}

	ALOGI("mFirstPacketSeqNum=%d,mHighestSeqNumber=%d,mNumBuffersReceived=%d",mFirstPacketSeqNum,mHighestSeqNumber,mNumBuffersReceived);
	ALOGI("addReceiverReport,iCumulativeLost=%d",iCumulativeLost);
	
	data[13] = (iCumulativeLost >> 16) & 0xFF;  // cumulative lost
    data[14] = iCumulativeLost >> 8 & 0xFF;
    data[15] = iCumulativeLost & 0xFF;
	//cumulative lost --end	

#else
    data[12] = 0x00;  // fraction lost

    data[13] = 0x00;  // cumulative lost
    data[14] = 0x00;
    data[15] = 0x00;
#endif

    data[16] = mHighestSeqNumber >> 24;
    data[17] = (mHighestSeqNumber >> 16) & 0xff;
    data[18] = (mHighestSeqNumber >> 8) & 0xff;
    data[19] = mHighestSeqNumber & 0xff;

#ifndef ANDROID_DEFAULT_CODE
	data[20] = mUIInterarrivalJitter >> 24;  // Interarrival jitter
    data[21] = (mUIInterarrivalJitter >> 16) & 0xff;
    data[22] = (mUIInterarrivalJitter >> 8) & 0xff;
    data[23] = mUIInterarrivalJitter & 0xff;	
#else
    data[20] = 0x00;  // Interarrival jitter
    data[21] = 0x00;
    data[22] = 0x00;
    data[23] = 0x00;
#endif

    uint32_t LSR = 0;
    uint32_t DLSR = 0;
    if (mLastNTPTime != 0) {
        LSR = (mLastNTPTime >> 16) & 0xffffffff;

        DLSR = (uint32_t)
            ((ALooper::GetNowUs() - mLastNTPTimeUpdateUs) * 65536.0 / 1E6);
    }

    data[24] = LSR >> 24;
    data[25] = (LSR >> 16) & 0xff;
    data[26] = (LSR >> 8) & 0xff;
    data[27] = LSR & 0xff;

    data[28] = DLSR >> 24;
    data[29] = (DLSR >> 16) & 0xff;
    data[30] = (DLSR >> 8) & 0xff;
    data[31] = DLSR & 0xff;

    buffer->setRange(buffer->offset(), buffer->size() + 32);
}

#ifndef ANDROID_DEFAULT_CODE 
void ARTPSource::addSDES(const AString& cname, const sp<ABuffer> &buffer) {
    int32_t size = cname.size() + 1 + 2 + 8;
    size = (size + 3) & (uint32_t)(-4);

    if (buffer->size() + size > buffer->capacity()) {
        ALOGW("RTCP buffer too small to accomodate SDES.");
        return;
    }

    uint8_t *data = buffer->data() + buffer->size();
    data[0] = 0x80 | 1;
    data[1] = 202;  // SDES

    size_t numWords = (size / 4) - 1;
    data[2] = numWords >> 8;
    data[3] = numWords & 0xff;

    fakeSSRC(mID, data + 4);

    int32_t offset = 8;

    data[offset++] = 1;  // CNAME
    data[offset++] = cname.size();

    memcpy(&data[offset], cname.c_str(), cname.size());
    offset += cname.size();

    while(offset < size) {
        data[offset++] = 0;
    }

    buffer->setRange(buffer->offset(), buffer->size() + size);
}

void ARTPSource::updateExpectedTimeoutUs(const int32_t& samples) {
    int64_t duration = samples * 1000000LL / mClockRate;
    return updateExpectedTimeoutUs(duration);
}

void ARTPSource::updateExpectedTimeoutUs(const int64_t& duration) {
    if (duration < kAccessUnitTimeoutUs)
        return;
    int64_t timeUs = ALooper::GetNowUs() + duration;
    if (timeUs > mExpectedTimeoutUs)
        mExpectedTimeoutUs = timeUs;
}

void ARTPSource::flushRTPPackets() {
    if (mAssembler != NULL) {
        mAssembler->onPacketReceived(this, true);
    }
}

void ARTPSource::setHighestSeqNumber(uint32_t rtpSeq) {
    if (!mHighestSeqNumberSet) {
        // FIXME: we always trust rtp seq instead of first packet's seq,
        // Bug will happen when first packet is 0x0001, and rtp seq is 0xffff,
        // if we received 0x0001 before than we get rtp seq 0xffff,
        // the first packet will be flushed after we reset highest to 0xffff
        mHighestSeqNumberSet = true;
        mHighestSeqNumber = rtpSeq;
    } else {
        rtpSeq = extendSeqNumber(rtpSeq, mHighestSeqNumber);
    }
    ALOGI("timeUpdate seq %d establish %d", rtpSeq, mEstablished);
    if (!mEstablished) {
        mAssembler->setNextExpectedSeqNo(rtpSeq);
        mEstablished = true;
    }
}

void ARTPSource::addNADUApp(sp<APacketSource> &pApacketSource,const sp<ABuffer> &buffer){
	 if (buffer->size() + 24 > buffer->capacity()) {
        ALOGW("RTCP buffer too small to accomodate RR.");
        return;
    }
	if(!pApacketSource.get()){
		ALOGE("ApacketSource pointer is NULL");
		return;
	}
    uint8_t *data = buffer->data() + buffer->size();

    data[0] = 0x80; //NADU
    data[1] = 204;  // APP
    data[2] = 0; 
    data[3] = 5; // number of 32 bit words -1 ,is 24 bytes

    fakeSSRC(mID, data + 4);

	data[8] = 0x50; //NADU name ''PSS0"
	data[9] = 0x53;
	data[10] = 0x53;
	data[11] = 0x30;	
	/*
	//data[8] = 0x4E; //NADU name ''NADU"
	data[8] = 0x44; //NADU name "DADU"
	data[9] = 0x41;
	data[10] = 0x44;
	data[11] = 0x55;
	*/
	
    data[12] = mID >> 24;
    data[13] = (mID >> 16) & 0xff;
    data[14] = (mID >> 8) & 0xff;
    data[15] = mID & 0xff;

	//playout delay, it 's not easy get mVideoTimeUs	
	data[16]=0xFF; 
	data[17]=0xFF;
	
	//NSN, Next Sequence Numbler of next ADU to de decoded.
	int32_t ui32NSN;
	uint16_t ui16NSN;
		
	bool isAvailable = pApacketSource->getNSN(&ui32NSN);		
	if(!isAvailable){
		//if the buffer queue is empty
		ui16NSN = (uint16_t)(mHighestSeqNumber & 0xFFFF) +1;
		//ui16NSN = (uint16_t)(mHighestSeqNumber & 0xFFFF);
	}
	else{
		ui16NSN = (uint16_t)(ui32NSN & 0xFFFF);
	}
	data[18]= (ui16NSN) >> 8 & 0xFF;
	data[19]= ui16NSN & 0xFF;

	//reseved 11bit and NUM 5 bit, here we always set NUM with 0
	data[20]= 0;
	data[21]= 0;

	//free  Buffer space
	uint16_t uiFreeBufSpace; //count in 64 bytes, so max free size is 4194304
	size_t uiRealFreeBufSpace = pApacketSource->getFreeBufSpace();
	if(uiRealFreeBufSpace > 4194304)  // if overflow of 16bit  
		uiFreeBufSpace = 0xFFFF; //65536;
	else
		uiFreeBufSpace = uiRealFreeBufSpace;

	data[22] = (uiFreeBufSpace >> 8) & 0xff;
	data[23] = uiFreeBufSpace & 0xff;

	buffer->setRange(buffer->offset(), buffer->size() + 24);
}

void ARTPSource::addNADUApp(const sp<AnotherPacketSource> &pAnotherPacketSource,const sp<ABuffer> &buffer){
	 if (buffer->size() + 24 > buffer->capacity()) {
        ALOGW("RTCP buffer too small to accomodate RR.");
        return;
    }
	if(!pAnotherPacketSource.get()){
		ALOGE("ApacketSource pointer is NULL");
		return;
	}
    uint8_t *data = buffer->data() + buffer->size();

    data[0] = 0x80; //NADU
    data[1] = 204;  // APP
    data[2] = 0; 
    data[3] = 5; // number of 32 bit words -1 ,is 24 bytes

    fakeSSRC(mID, data + 4);

	data[8] = 0x50; //NADU name ''PSS0"
	data[9] = 0x53;
	data[10] = 0x53;
	data[11] = 0x30;	
	/*
	//data[8] = 0x4E; //NADU name ''NADU"
	data[8] = 0x44; //NADU name "DADU"
	data[9] = 0x41;
	data[10] = 0x44;
	data[11] = 0x55;
	*/
	
    data[12] = mID >> 24;
    data[13] = (mID >> 16) & 0xff;
    data[14] = (mID >> 8) & 0xff;
    data[15] = mID & 0xff;

	//playout delay, it 's not easy get mVideoTimeUs	
	data[16]=0xFF; 
	data[17]=0xFF;
	
	//NSN, Next Sequence Numbler of next ADU to de decoded.
	int32_t ui32NSN;
	uint16_t ui16NSN;
		
	bool isAvailable = pAnotherPacketSource->getNSN(&ui32NSN);		
	if(!isAvailable){
		//if the buffer queue is empty
		ui16NSN = (uint16_t)(mHighestSeqNumber & 0xFFFF) +1;
		//ui16NSN = (uint16_t)(mHighestSeqNumber & 0xFFFF);
	}
	else{
		ui16NSN = (uint16_t)(ui32NSN & 0xFFFF);
	}
	data[18]= (ui16NSN) >> 8 & 0xFF;
	data[19]= ui16NSN & 0xFF;

	//reseved 11bit and NUM 5 bit, here we always set NUM with 0
	data[20]= 0;
	data[21]= 0;

	//free  Buffer space
	uint16_t uiFreeBufSpace; //count in 64 bytes, so max free size is 4194304
	size_t uiRealFreeBufSpace = pAnotherPacketSource->getFreeBufSpace();
	if(uiRealFreeBufSpace > 4194304)  // if overflow of 16bit  
		uiFreeBufSpace = 0xFFFF; //65536;
	else
		uiFreeBufSpace = uiRealFreeBufSpace;

	data[22] = (uiFreeBufSpace >> 8) & 0xff;
	data[23] = uiFreeBufSpace & 0xff;

	buffer->setRange(buffer->offset(), buffer->size() + 24);
}


#endif // #ifndef ANDROID_DEFAULT_CODE
}  // namespace android


