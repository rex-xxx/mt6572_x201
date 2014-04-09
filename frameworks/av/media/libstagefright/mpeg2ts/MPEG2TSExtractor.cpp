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
#define LOG_TAG "MPEG2TSExtractor"
#include <utils/Log.h>

#include "include/MPEG2TSExtractor.h"
#include "include/LiveSession.h"
#include "include/NuCachedSource2.h"

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <utils/String8.h>

#include "AnotherPacketSource.h"
#include "ATSParser.h"

#ifndef ANDROID_DEFAULT_CODE
const static int64_t kMaxPTSTimeOutUs = 3000000LL;    //  handle find Duration for ANR
#endif
#ifndef ANDROID_DEFAULT_CODE
#include <media/stagefright/foundation/AMessage.h>
#define SUPPORT_M2TS  
//#undef SUPPORT_M2TS
#endif
namespace android {

static const size_t kTSPacketSize = 188;
#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
static const size_t kM2TSPacketSize = 192;
static size_t kFillPacketSize = 188;
#endif  //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

struct MPEG2TSSource : public MediaSource {
    MPEG2TSSource(
            const sp<MPEG2TSExtractor> &extractor,
            const sp<AnotherPacketSource> &impl,
            bool seekable);

    virtual status_t start(MetaData *params = NULL);
    virtual status_t stop();
    virtual sp<MetaData> getFormat();

    virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options = NULL);

private:
    sp<MPEG2TSExtractor> mExtractor;
    sp<AnotherPacketSource> mImpl;

    // If there are both audio and video streams, only the video stream
    // will be seekable, otherwise the single stream will be seekable.
    bool mSeekable;
#ifndef ANDROID_DEFAULT_CODE
    bool mIsVideo;
#endif

    DISALLOW_EVIL_CONSTRUCTORS(MPEG2TSSource);
};

MPEG2TSSource::MPEG2TSSource(
        const sp<MPEG2TSExtractor> &extractor,
        const sp<AnotherPacketSource> &impl,
        bool seekable)
    : mExtractor(extractor),
      mImpl(impl),
	  #ifndef ANDROID_DEFAULT_CODE
       mIsVideo(true),
#endif
      mSeekable(seekable) {
}

status_t MPEG2TSSource::start(MetaData *params) {
    return mImpl->start(params);
}

status_t MPEG2TSSource::stop() {
#ifndef ANDROID_DEFAULT_CODE
           ALOGD("Stop Video=%d track",mIsVideo);
            if(mIsVideo==true)
            {
                    mExtractor->setVideoState ( true);
            }
#endif  
    return mImpl->stop();
}

sp<MetaData> MPEG2TSSource::getFormat() {
    sp<MetaData> meta = mImpl->getFormat();

    int64_t durationUs;
    if (mExtractor->mLiveSession != NULL
            && mExtractor->mLiveSession->getDuration(&durationUs) == OK) {
        meta->setInt64(kKeyDuration, durationUs);
    }
#ifndef ANDROID_DEFAULT_CODE
    else if (mExtractor->mLiveSession == NULL) {
		meta->setInt64(kKeyDuration, mExtractor->getDurationUs());
    }

     const char* mime;
    CHECK(meta->findCString(kKeyMIMEType, &mime));

    if (!strncasecmp("audio/", mime, 6)) {
        mIsVideo = false;
    } else {
        CHECK(!strncasecmp("video/", mime, 6));
        mIsVideo =true;
    }
#endif

    return meta;
}

status_t MPEG2TSSource::read(
        MediaBuffer **out, const ReadOptions *options) {
    *out = NULL;

    int64_t seekTimeUs;
    ReadOptions::SeekMode seekMode;
#ifndef ANDROID_DEFAULT_CODE
        if (options && options->getSeekTo(&seekTimeUs, &seekMode)) {
                if(mExtractor->getVideoState() && !mIsVideo  && !mSeekable)
                {
                        mSeekable=true;
                       ALOGE("video Audio can seek now");
                } 
                if(mSeekable)
                {
                        mExtractor->seekTo(seekTimeUs);
                }
        }
	       

    
#else
    if (mSeekable && options && options->getSeekTo(&seekTimeUs, &seekMode)) {
        mExtractor->seekTo(seekTimeUs);
    }
#endif

    status_t finalResult;
#ifndef ANDROID_DEFAULT_CODE
	while ( !mImpl->hasBufferAvailable(&finalResult) || mExtractor->getSeeking() )  {
#else
    while (!mImpl->hasBufferAvailable(&finalResult)) {
#endif
        if (finalResult != OK) {
            ALOGI("line:%d, ERROR_END_OF_STREAM", __LINE__);
            return ERROR_END_OF_STREAM;
        }

        status_t err = mExtractor->feedMore();
        if (err != OK) {
            ALOGI("line:%d, signalEOS", __LINE__);
            mImpl->signalEOS(err);
        }
    }

    return mImpl->read(out, options);
}

////////////////////////////////////////////////////////////////////////////////

#ifndef ANDROID_DEFAULT_CODE
int32_t  findSyncCode(const void *data, size_t size)
{
      uint32_t i=0;
	for( i=0;i<size;i++)
      {
      		if(((uint8_t*)data)[i]==0x47u) 
			  return i;
	}
	return -1;
}
#endif
MPEG2TSExtractor::MPEG2TSExtractor(const sp<DataSource> &source)
    : mDataSource(source),
#ifndef ANDROID_DEFAULT_CODE    
      mParser(new ATSParser(0x40000000)),//TS_SOURCE_IS_LOCAL)),
       mDurationUs(0),
      mSeekTimeUs(0),
      mSeeking(false),
      mSeekingOffset(0),
      mFileSize(0),
      mMinOffset(0),
      mMaxOffset(0),
      mMaxcount(0),
      mVideoUnSupportedByDecoder(false),
#else
      mParser(new ATSParser),
#endif
      mOffset(0) {
#ifndef ANDROID_DEFAULT_CODE

   ALOGD("=====================================\n"); 
   ALOGD("[MPEG2TS Playback capability info]£º\n"); 
   ALOGD("=====================================\n"); 
   ALOGD("Resolution = \"[(8,8) ~ (1280£¬720)]\" \n"); 
   ALOGD("Support Codec = \"Video:MPEG4, H264, MPEG1,MPEG2 ; Audio: AAC,MP3\" \n"); 
   ALOGD("Profile_Level = \"MPEG4: ASP ;  H264: Baseline/3.1, Main/3.1,High/3.1\" \n"); 
   ALOGD("Max frameRate =  120fps \n"); 
   ALOGD("Max Bitrate  = H264: 2Mbps  (720P@30fps) ; MPEG4/H263: 4Mbps (720P@30fps)\n"); 
   ALOGD("=====================================\n");
	
   status_t err = parseMaxPTS();//parse all the TS packet of this file?
   if (err != OK) {
        return;
   }
 //[qian]may be we should add the seek table creation this section
 //when 2st parse whole file
	ALOGE("MPEG2TSExtractor: after parseMaxPTS  mOffset=%lld",mOffset);	 
#endif
    init();
ALOGE("MPEG2TSExtractor: after init  mOffset=%lld",mOffset);	 
}

size_t MPEG2TSExtractor::countTracks() {
    return mSourceImpls.size();
}

sp<MediaSource> MPEG2TSExtractor::getTrack(size_t index) {
    if (index >= mSourceImpls.size()) {
        return NULL;
    }

    bool seekable = true;
    if (mSourceImpls.size() > 1) {
        CHECK_EQ(mSourceImpls.size(), 2u);

        sp<MetaData> meta = mSourceImpls.editItemAt(index)->getFormat();
        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        if (!strncasecmp("audio/", mime, 6)) {
            seekable = false;
        }
    }

    return new MPEG2TSSource(this, mSourceImpls.editItemAt(index), seekable);
}

sp<MetaData> MPEG2TSExtractor::getTrackMetaData(
        size_t index, uint32_t flags) {
#ifndef ANDROID_DEFAULT_CODE
    if(index >= mSourceImpls.size()) return NULL;
   
    sp<MetaData> meta  =    mSourceImpls.editItemAt(index)->getFormat()  ;
   
    meta->setInt64(kKeyDuration,  getDurationUs());
	 
#endif
    return index < mSourceImpls.size()
        ? mSourceImpls.editItemAt(index)->getFormat() : NULL;
}

sp<MetaData> MPEG2TSExtractor::getMetaData() {
    sp<MetaData> meta = new MetaData;
#ifndef ANDROID_DEFAULT_CODE
	bool hasVideo = false;
	for (int index = 0; index < mSourceImpls.size(); index++) {
		sp<MetaData> meta = mSourceImpls.editItemAt(index)->getFormat();
		const char *mime;
		CHECK(meta->findCString(kKeyMIMEType, &mime));
		if (!strncasecmp("video/", mime, 6)) {
			hasVideo = true;
		}
	}
	//[qian]can set the hasvideo to be class member, not need to read meta
	//has parsed the hasvideo value in init() funtion
	if (hasVideo) {
		meta->setCString(kKeyMIMEType, "video/mp2ts");
	} else {
		meta->setCString(kKeyMIMEType, "audio/mp2ts");
	}
 
   // set flag for handle the case: video too long to audio
   meta->setInt32(kKeyVideoPreCheck, 1);
 
#else
    meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_CONTAINER_MPEG2TS);
#endif
    return meta;
}

void MPEG2TSExtractor::init() {
    bool haveAudio = false;
    bool haveVideo = false;
    int numPacketsParsed = 0;

#ifndef ANDROID_DEFAULT_CODE
    mOffset = 0;
#endif
    while (feedMore() == OK) {
        ATSParser::SourceType type;
        if (haveAudio && haveVideo) {
            break;
        }
        if (!haveVideo) {
            sp<AnotherPacketSource> impl =
                (AnotherPacketSource *)mParser->getSource(
                        ATSParser::VIDEO).get();

            if (impl != NULL) {
                haveVideo = true;
                mSourceImpls.push(impl);
            }
        }

        if (!haveAudio) {
            sp<AnotherPacketSource> impl =
                (AnotherPacketSource *)mParser->getSource(
                        ATSParser::AUDIO).get();

            if (impl != NULL) {
                haveAudio = true;
                mSourceImpls.push(impl);
            }
        }

#ifndef ANDROID_DEFAULT_CODE
        if (++numPacketsParsed > 20000) {
#else
        if (++numPacketsParsed > 10000) {
#endif
            break;
        }
    }

    ALOGI("haveAudio=%d, haveVideo=%d", haveAudio, haveVideo);
}

status_t MPEG2TSExtractor::feedMore() {
    Mutex::Autolock autoLock(mLock);
#ifndef ANDROID_DEFAULT_CODE
	if (mSeeking) {
		int64_t pts = mParser->getMaxPTS();//[qian] get the max pts in the had read data  

		if (pts > 0) {
			mMaxcount++;
			if ((pts - mSeekTimeUs < 50000 && pts - mSeekTimeUs > -50000)
					|| mMinOffset == mMaxOffset || mMaxcount > 13) {
				//ALOGE("pts=%lld,mSeekTimeUs=%lld,mMaxcount=%lld,mMinOffset=0x%x,mMaxOffset=0x%x",pts/1000,mSeekTimeUs/1000,mMaxcount, mMinOffset,mMaxOffset );
				mSeeking = false;
				mParser->setDequeueState(true);//
			} else {
				mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK /* isSeek */);
				if (pts < mSeekTimeUs) {
					mMinOffset = mSeekingOffset;//[qian], 1 enter this will begin with the mid of file

				} else {
					mMaxOffset = mSeekingOffset;
				}
#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
				mSeekingOffset = (off64_t)((((mMinOffset + mMaxOffset) / 2) / kFillPacketSize) * kFillPacketSize);
#else  //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

				mSeekingOffset = (off64_t)((((mMinOffset + mMaxOffset) / 2) / kTSPacketSize) * kTSPacketSize);
#endif //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

				mOffset = mSeekingOffset;
			}
		ALOGE("pts=%lld,mSeekTimeUs=%lld,mMaxcount=%lld,mOffset=%lld,mMinOffset=%lld,mMaxOffset=%lld",pts/1000,mSeekTimeUs/1000,mMaxcount, mOffset,mMinOffset,mMaxOffset );
				
		}
	}
#endif
#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
     uint8_t packet[kFillPacketSize];
     status_t retv=OK;
    ssize_t n = mDataSource->readAt(mOffset, packet, kFillPacketSize);

    if (n < (ssize_t)kFillPacketSize) {
	ALOGE(" mOffset=%lld,n =%ld",mOffset,n);
        return (n < 0) ? (status_t)n : ERROR_END_OF_STREAM;
    }
ALOGV("feedMore:mOffset = %lld  packet=0x%x,0x%x,0x%x,0x%x",mOffset,packet[0],packet[1],packet[2],packet[3]);
	
   	mOffset += n;
	if(kFillPacketSize == kM2TSPacketSize)	 
	{	
		
		retv = mParser->feedTSPacket(packet+4, kFillPacketSize-4);
	      
		if(retv== BAD_VALUE)
	      {
			int32_t syncOff=0;
			syncOff = findSyncCode(packet+4, kFillPacketSize-4);
 			if(syncOff>=0) 
		      { 
		      		mOffset -= n;
				mOffset+=syncOff;
			}
			return OK;
		}
		else
	      {			
			return retv;
	      }	
			
    	}
	else
	{
		
    		retv =  mParser->feedTSPacket(packet, kFillPacketSize);
	      
		if(retv== BAD_VALUE)
	      {
			int32_t syncOff=0;
			syncOff = findSyncCode(packet, kFillPacketSize);
 			if(syncOff>=0) 
		      { 
		      		mOffset -= n;
				mOffset+=syncOff;
			}
		ALOGE("[TS_ERROR]correction once offset mOffset=%lld",mOffset);
			return OK;
		}
		else
	      {			
			return retv;
	      }	
	}
#else //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

#ifndef ANDROID_DEFAULT_CODE
    uint8_t packet[kTSPacketSize];
    status_t retv=OK;
    
    ssize_t n = mDataSource->readAt(mOffset, packet, kTSPacketSize);

    if (n < (ssize_t)kTSPacketSize) {
	ALOGE(" mOffset=%lld,n =%ld",mOffset,n);
        return (n < 0) ? (status_t)n : ERROR_END_OF_STREAM;
    }

   ALOGV("mOffset= %lld  packet=0x%x,0x%x,0x%x,0x%x",mOffset,packet[0],packet[1],packet[2],packet[3]);
    mOffset += n;
    retv =  mParser->feedTSPacket(packet, kTSPacketSize);
	
   if(retv== BAD_VALUE)
   {
	int32_t syncOff=0;
	syncOff = findSyncCode(packet, kTSPacketSize);
		if(syncOff>=0) 
      { 
      		mOffset -= n;
		mOffset+=syncOff;
	}
ALOGE("[TS_ERROR]correction once offset mOffset=%lld",mOffset);
	return OK;
  }
  else
  {			
	return retv;
  }	
#else
    uint8_t packet[kTSPacketSize];
    ssize_t n = mDataSource->readAt(mOffset, packet, kTSPacketSize);

    if (n < (ssize_t)kTSPacketSize) {
        return (n < 0) ? (status_t)n : ERROR_END_OF_STREAM;
    }

    mOffset += n;
    return mParser->feedTSPacket(packet, kTSPacketSize);
#endif
#endif  //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
}
#ifndef ANDROID_DEFAULT_CODE

bool MPEG2TSExtractor::getSeeking() {
    return mSeeking;
}
void   MPEG2TSExtractor::setVideoState(bool state){
      mVideoUnSupportedByDecoder=state;
     ALOGE("setVideoState  mVideoUnSupportedByDecoder=%d",mVideoUnSupportedByDecoder);
}
bool MPEG2TSExtractor::getVideoState(void){
     ALOGE("getVideoState  mVideoUnSupportedByDecoder=%d",mVideoUnSupportedByDecoder);
      return mVideoUnSupportedByDecoder ;
     
}
#endif
void MPEG2TSExtractor::setLiveSession(const sp<LiveSession> &liveSession) {
    Mutex::Autolock autoLock(mLock);

    mLiveSession = liveSession;
}

#ifndef ANDROID_DEFAULT_CODE
bool MPEG2TSExtractor::findPAT() {
	Mutex::Autolock autoLock(mLock);

#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
	uint8_t packet[kFillPacketSize];
	ssize_t n = mDataSource->readAt(mOffset, packet, kFillPacketSize);
ALOGV("findPAT mOffset= %lld  packet=0x%x,0x%x,0x%x,0x%x",mOffset,packet[0],packet[1],packet[2],packet[3]);
	if(kFillPacketSize == kM2TSPacketSize)	 
	{	
		return mParser->findPAT(packet+4, kFillPacketSize-4);
    }
	else
	{
		return mParser->findPAT(packet, kFillPacketSize);
	}
#else //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

	uint8_t packet[kTSPacketSize];

	ssize_t n = mDataSource->readAt(mOffset, packet, kTSPacketSize);
     ALOGV("findPAT mOffset=0x%lld,packet=0x%x,0x%x,0x%x,0x%x",mOffset,packet[0],packet[1],packet[2],packet[3]);
	return mParser->findPAT(packet, kTSPacketSize);
	
#endif //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

}

status_t MPEG2TSExtractor::parseMaxPTS() {
	mDataSource->getSize(&mFileSize);
#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
	off64_t counts = mFileSize / kFillPacketSize;
#else //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
	off64_t counts = mFileSize / kTSPacketSize;
#endif  //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

	int64_t maxPTSStart = systemTime()/1000;
	//really dequeue data?
	mParser->setDequeueState(false);
	//[qian]set false, when parse the ts pakect, will not exec the  main function of onPayloadData
	//only parse the PAT, PMT,PES header, save parse time
	
	//if (!(mParser->mFlags & TS_TIMESTAMPS_ARE_ABSOLUTE)) {
	   //get first pts(pts in in PES packet)
            bool foundFirstPTS = false;
	    while (feedMore() == OK ) {
		    if (mParser->firstPTSIsValid()) {
			ALOGD("parseMaxPTS:firstPTSIsValid, mOffset",mOffset);
                        foundFirstPTS = true;
			    break;
		    }
	    }
	    if (!foundFirstPTS) {
		ALOGI("not found first PTS");
		return OK;
	    }
	    //clear

	    mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
	//}

	//get duration
	for (off64_t i = 1; i <= counts; i++) {
		int64_t maxPTSDuration = systemTime()/1000 - maxPTSStart;
		if (maxPTSDuration > kMaxPTSTimeOutUs) {
		    ALOGD("TimeOut find PTS, start time=%lld, duration=%lld", maxPTSStart, maxPTSDuration);
		    return UNKNOWN_ERROR;
		}
#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
		
		mOffset = (off64_t)((counts - i) * kFillPacketSize);

#else //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

		mOffset = (off64_t)((counts - i) * kTSPacketSize);
#endif  //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))

		if (findPAT()) {//find last PAT
			//start searching from the last PAT
		ALOGD("parseMaxPTS:findPAT done, mOffset=%lld",mOffset);
			mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
			while (feedMore() == OK) {//[qian]the end of file? parse all the TS packet of this file?
			//may be we should add the seek table when 2st parse whole file
			}
			mDurationUs = mParser->getMaxPTS();
			if (mDurationUs)
				break;
		}
	}
	//clear data queue
	mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
	mParser->setDequeueState(true);//
        return OK;
ALOGD("getMaxPTS->mDurationUs:%lld", mDurationUs);
}
uint64_t MPEG2TSExtractor::getDurationUs() {
	return mDurationUs;
}
#endif
void MPEG2TSExtractor::seekTo(int64_t seekTimeUs) {
    Mutex::Autolock autoLock(mLock);

    if (mLiveSession == NULL) {
#ifndef ANDROID_DEFAULT_CODE
	ALOGE("seekTo:mDurationMs =%lld,seekTimeMs= %lld, mOffset:%lld",mDurationUs/1000,seekTimeUs/1000, mOffset);
		if (seekTimeUs == 0) {
			mOffset = 0;
			mSeeking = false;
                        // clear MaxPTS
		        mParser->setDequeueState(false);
			mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
                        // clear buffer queue
		        mParser->setDequeueState(true);
			mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
		} else if((mDurationUs-seekTimeUs) < 10000)//seek to end
	     {
			mOffset = mFileSize;
			mSeeking = false;
                        // set ATSParser MaxTimeUs to mDurationUs
		        mParser->setDequeueState(false);
		            sp<AMessage> maxTimeUsMsg = new AMessage;
                        maxTimeUsMsg->setInt64("MaxtimeUs", mDurationUs);
			mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK,  maxTimeUsMsg);
                        // clear buffer queue
		        mParser->setDequeueState(true);
			mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
				 
		}else {
    	mParser->signalDiscontinuity(ATSParser::DISCONTINUITY_SEEK  /* isSeek */);
		
		//[qian] firstly find the rough offset by packet size 
		//I thinks we should use the 
		//mSeekingOffset=(off64_t)((seekTimeUs*mFileSize/mDuration)/kTSPacketSize)* kTSPacketSize;
		mSeekingOffset = mOffset;
		
		mSeekTimeUs=seekTimeUs;
			mMinOffset = 0;
			mMaxOffset = mFileSize;
	    mMaxcount=0;
		mParser->setDequeueState(false);//[qian] will start search mode, not read data mode
		mSeeking=true;
		}
#endif
        return;
    }

    mLiveSession->seekTo(seekTimeUs);
}

uint32_t MPEG2TSExtractor::flags() const {
    Mutex::Autolock autoLock(mLock);

    uint32_t flags = CAN_PAUSE;
#ifndef ANDROID_DEFAULT_CODE
    if ((mLiveSession != NULL && mLiveSession->isSeekable()) || mLiveSession == NULL) {
#else
    if (mLiveSession != NULL && mLiveSession->isSeekable()) {
#endif
        flags |= CAN_SEEK_FORWARD | CAN_SEEK_BACKWARD | CAN_SEEK;
    }

    return flags;
}

////////////////////////////////////////////////////////////////////////////////
#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
bool SniffMPEG2TS(
        const sp<DataSource> &source, String8 *mimeType, float *confidence,
        sp<AMessage> *) {
    bool retb=true;
	
    for (int i = 0; i < 5; ++i) {
        char header;
        if (source->readAt(kTSPacketSize * i, &header, 1) != 1
                || header != 0x47) {
            retb = false;
			break;
        }
    }
	if(retb)
	{
	ALOGD("this is ts file\n");
		kFillPacketSize = kTSPacketSize;
	}
	else
	{
		retb=true;
		for (int i = 0; i < 5; ++i) {
	        char header[5];
	        if (source->readAt(kM2TSPacketSize * i, &header, 5) != 5
	                || header[4] != 0x47) {
	            retb = false;
				return retb;
	        }
    	}
		if(retb)
		{
		ALOGD("this is m2ts file\n");
			kFillPacketSize = kM2TSPacketSize;
		}
	}
	
	

    *confidence = 0.3f;

    mimeType->setTo(MEDIA_MIMETYPE_CONTAINER_MPEG2TS);

    return true;
}


#else

bool SniffMPEG2TS(
        const sp<DataSource> &source, String8 *mimeType, float *confidence,
        sp<AMessage> *) {
    for (int i = 0; i < 5; ++i) {
        char header;
        if (source->readAt(kTSPacketSize * i, &header, 1) != 1
                || header != 0x47) {
            return false;
        }
    }
#ifndef ANDROID_DEFAULT_CODE
    *confidence = 0.3f;
#else
    *confidence = 0.1f;
#endif
    mimeType->setTo(MEDIA_MIMETYPE_CONTAINER_MPEG2TS);

    return true;
}

#endif  //#if !defined(ANDROID_DEFAULT_CODE) && (defined(SUPPORT_M2TS))
}  // namespace android
