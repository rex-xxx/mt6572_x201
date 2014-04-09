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
//#define LOG_TAG "AnotherPacketSource"
//#include <utils/Log.h>

#include "AnotherPacketSource.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <utils/Vector.h>

#ifndef ANDROID_DEFAULT_CODE
static int kWholeBufSize = 40000000; //40Mbytes
static int kTargetTime = 2000;  //ms
#endif
namespace android {

AnotherPacketSource::AnotherPacketSource(const sp<MetaData> &meta)
    : mIsAudio(false),
      mFormat(meta),
#ifndef ANDROID_DEFAULT_CODE
     mIsEOS(false),
     mIsAVC(false),
     mScanForIDR(true),
     mNeedScanForIDR(true),	//default is opened
#endif
      mEOSResult(OK) {
    const char *mime;
    CHECK(meta->findCString(kKeyMIMEType, &mime));

    if (!strncasecmp("audio/", mime, 6)) {
        mIsAudio = true;
    } else {
        CHECK(!strncasecmp("video/", mime, 6));
    }

#ifndef ANDROID_DEFAULT_CODE
	//for bitrate-adaptation
	m_BufQueSize = kWholeBufSize; 
	m_TargetTime = kTargetTime;
	m_uiNextAduSeqNum = -1;
	// mtk80902: porting from APacketSource
	if (!strcmp(MEDIA_MIMETYPE_VIDEO_AVC, mime)) {
	    ALOGD("This is avc mime!");
	    mIsAVC = true;
	}
#endif
	
}

void AnotherPacketSource::setFormat(const sp<MetaData> &meta) {
    CHECK(mFormat == NULL);
    mFormat = meta;
}

AnotherPacketSource::~AnotherPacketSource() {
}

status_t AnotherPacketSource::start(MetaData *params) {
#ifndef ANDROID_DEFAULT_CODE
    mIsEOS=false;
#endif     
    return OK;
}

status_t AnotherPacketSource::stop() {
#ifndef ANDROID_DEFAULT_CODE
    clear();
    mIsEOS=true;
#endif    
    return OK;
}
#ifndef ANDROID_DEFAULT_CODE
status_t AnotherPacketSource::isEOS() {
        return mIsEOS;
}
void AnotherPacketSource::setScanForIDR(bool enable) {
	mNeedScanForIDR = enable;
}
#endif    

sp<MetaData> AnotherPacketSource::getFormat() {
    return mFormat;
}

status_t AnotherPacketSource::dequeueAccessUnit(sp<ABuffer> *buffer) {
    buffer->clear();

    Mutex::Autolock autoLock(mLock);
    while (mEOSResult == OK && mBuffers.empty()) {
        mCondition.wait(mLock);
    }

    if (!mBuffers.empty()) {
        *buffer = *mBuffers.begin();
        mBuffers.erase(mBuffers.begin());

        int32_t discontinuity;
        if ((*buffer)->meta()->findInt32("discontinuity", &discontinuity)) {
            if (wasFormatChange(discontinuity)) {
                mFormat.clear();
            }

            return INFO_DISCONTINUITY;
        }

        return OK;
    }

    return mEOSResult;
}
#ifndef ANDROID_DEFAULT_CODE
status_t AnotherPacketSource::read(
        MediaBuffer **out, const ReadOptions *options) {
#else
status_t AnotherPacketSource::read(
        MediaBuffer **out, const ReadOptions *) {
#endif
    *out = NULL;

    Mutex::Autolock autoLock(mLock);
    while (mEOSResult == OK && mBuffers.empty()) {
        mCondition.wait(mLock);
    }

    if (!mBuffers.empty()) {
        const sp<ABuffer> buffer = *mBuffers.begin();
		
#ifndef ANDROID_DEFAULT_CODE
		m_uiNextAduSeqNum = buffer->int32Data();
#endif
        mBuffers.erase(mBuffers.begin());

        int32_t discontinuity;
        if (buffer->meta()->findInt32("discontinuity", &discontinuity)) {
            if (wasFormatChange(discontinuity)) {
                mFormat.clear();
            }

            return INFO_DISCONTINUITY;
        } else {
            int64_t timeUs;
            CHECK(buffer->meta()->findInt64("timeUs", &timeUs));

            MediaBuffer *mediaBuffer = new MediaBuffer(buffer);

            mediaBuffer->meta_data()->setInt64(kKeyTime, timeUs);
#ifndef ANDROID_DEFAULT_CODE
            int32_t fgInvalidtimeUs=false;
            if(buffer->meta()->findInt32("invt", &fgInvalidtimeUs))
            {
            	mediaBuffer->meta_data()->setInt32(kInvalidKeyTime, fgInvalidtimeUs);
            }

		 int64_t seekTimeUs;
		 ReadOptions::SeekMode seekMode;
      		 if (options && options->getSeekTo(&seekTimeUs, &seekMode)) {	
			mediaBuffer->meta_data()->setInt64(kKeyTargetTime, seekTimeUs);
              }
#endif

            *out = mediaBuffer;
            return OK;
        }
    }

    return mEOSResult;
}

bool AnotherPacketSource::wasFormatChange(
        int32_t discontinuityType) const {
    if (mIsAudio) {
        return (discontinuityType & ATSParser::DISCONTINUITY_AUDIO_FORMAT) != 0;
    }

    return (discontinuityType & ATSParser::DISCONTINUITY_VIDEO_FORMAT) != 0;
}

void AnotherPacketSource::queueAccessUnit(const sp<ABuffer> &buffer) {
    int32_t damaged;
#ifndef ANDROID_DEFAULT_CODE
        if(mIsEOS)
        {
                 return ;
        }
	// mtk80902: porting from APacketSource
	// wait IDR for 264
	if (mIsAVC && mNeedScanForIDR && mScanForIDR) {
		if ((buffer->data()[0] & 0x1f) != 5) {
			ALOGD("skipping AU while scanning for next IDR frame.");
			return;
		} 
		mScanForIDR = false;
	}
#endif    
    if (buffer->meta()->findInt32("damaged", &damaged) && damaged) {
        // LOG(VERBOSE) << "discarding damaged AU";
        return;
    }

    int64_t timeUs;
    CHECK(buffer->meta()->findInt64("timeUs", &timeUs));
    ALOGV("queueAccessUnit timeUs=%lld us (%.2f secs)", timeUs, timeUs / 1E6);

    Mutex::Autolock autoLock(mLock);
    mBuffers.push_back(buffer);
    mCondition.signal();
}

void AnotherPacketSource::queueDiscontinuity(
        ATSParser::DiscontinuityType type,
        const sp<AMessage> &extra) {
    Mutex::Autolock autoLock(mLock);

#ifndef ANDROID_DEFAULT_CODE
    if (type == ATSParser::DISCONTINUITY_NONE) {
        return;
    }

    if (type == ATSParser::DISCONTINUITY_HTTPLIVE_SEEK) {
        //clear all buffer and pass extra message
        if (!mBuffers.empty()) {
            mBuffers.clear();
        }
        mEOSResult = OK;        
        
        sp<ABuffer> buffer = new ABuffer(0);
        buffer->meta()->setInt32("discontinuity", static_cast<int32_t>(type));
        buffer->meta()->setMessage("extra", extra);

        mBuffers.push_back(buffer);
        mCondition.signal();
        return;
    }

    if (wasFormatChange(type) && mFormat != NULL) {
        int32_t width = 0, height = 0;
        mFormat->findInt32(kKeyWidth, &width);
        mFormat->findInt32(kKeyHeight, &height);
        ALOGD("mFormat clear %d x %d", width , height);
        mFormat.clear();
    }
    if (type & ATSParser::DISCONTINUITY_FLUSH_SOURCE_ONLY) {
        //only flush source, don't queue discontinuity
        if (!mBuffers.empty()) {
            mBuffers.clear();
        }
        mEOSResult = OK;
        mScanForIDR = true;
        ALOGD("found discontinuity flush source only!");
        return;
    }
#endif
    // Leave only discontinuities in the queue.
    List<sp<ABuffer> >::iterator it = mBuffers.begin();
    while (it != mBuffers.end()) {
        sp<ABuffer> oldBuffer = *it;

        int32_t oldDiscontinuityType;
        if (!oldBuffer->meta()->findInt32(
                    "discontinuity", &oldDiscontinuityType)) {
            it = mBuffers.erase(it);
            continue;
        }

        ++it;
    }

    mEOSResult = OK;

    sp<ABuffer> buffer = new ABuffer(0);
    buffer->meta()->setInt32("discontinuity", static_cast<int32_t>(type));
    buffer->meta()->setMessage("extra", extra);

    mBuffers.push_back(buffer);
    mCondition.signal();
}

#ifndef ANDROID_DEFAULT_CODE
void AnotherPacketSource::clear() {
    Mutex::Autolock autoLock(mLock);
    if (!mBuffers.empty()) {
    mBuffers.clear();
	}
    mEOSResult = OK;
}
#endif
void AnotherPacketSource::signalEOS(status_t result) {
    CHECK(result != OK);

    Mutex::Autolock autoLock(mLock);
    mEOSResult = result;
    mCondition.signal();
}

bool AnotherPacketSource::hasBufferAvailable(status_t *finalResult) {
    Mutex::Autolock autoLock(mLock);
    if (!mBuffers.empty()) {
        return true;
    }

    *finalResult = mEOSResult;
    return false;
}

int64_t AnotherPacketSource::getBufferedDurationUs(status_t *finalResult) {
    Mutex::Autolock autoLock(mLock);

    *finalResult = mEOSResult;

    if (mBuffers.empty()) {
        return 0;
    }

    int64_t time1 = -1;
    int64_t time2 = -1;

    List<sp<ABuffer> >::iterator it = mBuffers.begin();
    while (it != mBuffers.end()) {
        const sp<ABuffer> &buffer = *it;

        int64_t timeUs;
        if (buffer->meta()->findInt64("timeUs", &timeUs)) {
            if (time1 < 0) {
                time1 = timeUs;
            }

            time2 = timeUs;
        } else {
            // This is a discontinuity, reset everything.
            time1 = time2 = -1;
        }

        ++it;
    }

    return time2 - time1;
}

status_t AnotherPacketSource::nextBufferTime(int64_t *timeUs) {
    *timeUs = 0;

    Mutex::Autolock autoLock(mLock);

    if (mBuffers.empty()) {
        return mEOSResult != OK ? mEOSResult : -EWOULDBLOCK;
    }

    sp<ABuffer> buffer = *mBuffers.begin();
    CHECK(buffer->meta()->findInt64("timeUs", timeUs));

    return OK;
}

#ifndef ANDROID_DEFAULT_CODE

bool AnotherPacketSource::getNSN(int32_t* uiNextSeqNum){

    Mutex::Autolock autoLock(mLock);
    if(!mBuffers.empty()){
        if(m_uiNextAduSeqNum!= -1){		
            *uiNextSeqNum = m_uiNextAduSeqNum;
            return true;
        }
        *uiNextSeqNum = (*mBuffers.begin())->int32Data();
        return true;
    }
    return false;
}

size_t AnotherPacketSource::getFreeBufSpace(){
    //size_t freeBufSpace = m_BufQueSize;
    size_t bufSizeUsed = 0;
		
    if(mBuffers.empty()){
        return m_BufQueSize;
    }

    List<sp<ABuffer> >::iterator it = mBuffers.begin();
    while (it != mBuffers.end()) {
        bufSizeUsed += (*it)->size();
        it++;	
    }
    if(bufSizeUsed >= m_BufQueSize)
        return 0;

    return 	m_BufQueSize - bufSizeUsed;	  
}

#endif

}  // namespace android
