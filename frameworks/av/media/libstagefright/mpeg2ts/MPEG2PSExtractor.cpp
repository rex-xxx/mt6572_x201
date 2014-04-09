/*
 * Copyright (C) 2011 The Android Open Source Project
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
#define LOG_TAG "MPEG2PSExtractor"
#include <utils/Log.h>

#include "include/MPEG2PSExtractor.h"

#include "AnotherPacketSource.h"
#include "ESQueue.h"

#include <media/stagefright/foundation/ABitReader.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>
#include <utils/String8.h>

//MP2PS
#include "include/avc_utils.h"
#ifndef ANDROID_DEFAULT_CODE
#include <sys/time.h>
#define BUFFERING_THRESHOLD_MS 20
#ifdef MTK_MTKPS_PLAYBACK_SUPPORT
#define PS_SNIFF_LENGTH 0xE1000	//enlarge from 1k to 900k for .dat files, reference BD experience
#else
#define PS_SNIFF_LENGTH 0x400   //sniff function will be executed even in ps-not-support model, keep use 1k to reduce unnecessary .dat parsing time
#endif
#endif

namespace android {

#ifndef ANDROID_DEFAULT_CODE
static const size_t kChunkSize = 16384;
#endif //#ifndef ANDROID_DEFAULT_CODE

struct MPEG2PSExtractor::Track : public MediaSource {
    Track(MPEG2PSExtractor *extractor,
          unsigned stream_id, unsigned stream_type);

    virtual status_t start(MetaData *params);
    virtual status_t stop();
    virtual sp<MetaData> getFormat();

    virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options);

protected:
    virtual ~Track();

private:
    friend struct MPEG2PSExtractor;

    MPEG2PSExtractor *mExtractor;

    unsigned mStreamID;
    unsigned mStreamType;
    ElementaryStreamQueue *mQueue;
    sp<AnotherPacketSource> mSource;
    
    #ifndef ANDROID_DEFAULT_CODE
    bool seeking;
    int64_t mMaxTimeUs;
    bool mFirstPTSValid;
    uint64_t mFirstPTS;
    bool mSeekable;
    int64_t mTimeUsPrev;
    #endif

    #ifndef ANDROID_DEFAULT_CODE
    int64_t getPTS();
    bool isVideo();
    bool isAudio();
    int64_t convertPTSToTimestamp(uint64_t PTS);
    void signalDiscontinuity(); 
    #endif

    status_t appendPESData(
            unsigned PTS_DTS_flags,
            uint64_t PTS, uint64_t DTS,
            const uint8_t *data, size_t size);

    DISALLOW_EVIL_CONSTRUCTORS(Track);
};

struct MPEG2PSExtractor::WrappedTrack : public MediaSource {
    WrappedTrack(const sp<MPEG2PSExtractor> &extractor, const sp<Track> &track);

    virtual status_t start(MetaData *params);
    virtual status_t stop();
    virtual sp<MetaData> getFormat();

    virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options);

protected:
    virtual ~WrappedTrack();

private:
    sp<MPEG2PSExtractor> mExtractor;
    sp<MPEG2PSExtractor::Track> mTrack;

    DISALLOW_EVIL_CONSTRUCTORS(WrappedTrack);
};

////////////////////////////////////////////////////////////////////////////////
#ifndef ANDROID_DEFAULT_CODE
int64_t getTickCountMs()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)(tv.tv_sec*1000LL + tv.tv_usec/1000);
}
#endif

MPEG2PSExtractor::MPEG2PSExtractor(const sp<DataSource> &source)
    : mDataSource(source),
      mOffset(0),
      mFinalResult(OK),
      mBuffer(new ABuffer(0)),
      mScanning(true),
      #ifndef ANDROID_DEFAULT_CODE
      mDurationUs(0),
      mSeekTimeUs(0),
      mSeeking(false),
      mSeekingOffset(0),
      mFileSize(0),
      mMinOffset(0),
      mMaxOffset(0),
      mMaxcount(0),
      mhasVTrack(false),
	  mhasATrack(false),
      mNeedDequeuePES(true),
	  bisPlayable(true),
	  mSearchPTS(0),
	  mAverageByteRate(0),
	  mSystemHeaderValid(false),
	  mParseMaxTime(false),
      #endif //#ifndef ANDROID_DEFAULT_CODE
      mProgramStreamMapValid(false) {


      #ifndef ANDROID_DEFAULT_CODE
      init();

	  if(countTracks()>0){
        parseMaxPTS();
      }
	  else{
		mDurationUs=0;
	  }
	  /*else{
	  	  mOffset = 0;
		  for (size_t i = 0; i < 100; ++i) {
	        if (feedMore() != OK) {
	            break;
	        }
	      }
	  }*/
      signalDiscontinuity();
    
      //Init Offset
      mOffset = 0;
      mSystemHeaderValid = false;
      #else //#ifndef ANDROID_DEFAULT_CODE
      for (size_t i = 0; i < 500; ++i) {
        if (feedMore() != OK) {
            break;
        }
      }
      #endif //#ifndef ANDROID_DEFAULT_CODE
      

    // Remove all tracks that were unable to determine their format.
    for (size_t i = mTracks.size(); i-- > 0;) {
        if (mTracks.valueAt(i)->getFormat() == NULL) {
	    #ifndef ANDROID_DEFAULT_CODE
	    ALOGE("NULL Foramt: %d \n", i);
	    #endif //#ifndef ANDROID_DEFAULT_CODE
           mTracks.removeItemsAt(i);
        }
    }

    mScanning = false;
}

MPEG2PSExtractor::~MPEG2PSExtractor() {
}

size_t MPEG2PSExtractor::countTracks() {
    return mTracks.size();
}

sp<MediaSource> MPEG2PSExtractor::getTrack(size_t index) {
    if (index >= mTracks.size()) {
        return NULL;
    }
    
    #ifndef ANDROID_DEFAULT_CODE
    bool seekable = true;
    	
    if (mTracks.size() > 1) {
        //CHECK_EQ(mTracks.size(), 2u);

        sp<MetaData> meta = mTracks.editValueAt(index)->getFormat();
        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));
        
	 if (!strncasecmp("audio/", mime, 6))
	 {
	   mTracks.editValueAt(index)->mSeekable = false;
	 }
    }
    #endif //#ifndef ANDROID_DEFAULT_CODE

    return new WrappedTrack(this, mTracks.valueAt(index));
}

sp<MetaData> MPEG2PSExtractor::getTrackMetaData(size_t index, uint32_t flags) {
    if (index >= mTracks.size()) {
        return NULL;
    }
#ifndef ANDROID_DEFAULT_CODE
	sp<MetaData> meta = mTracks.editValueAt(index)->getFormat()  ;
	meta->setInt64(kKeyDuration,  getDurationUs());
#endif
    return mTracks.valueAt(index)->getFormat();
}

sp<MetaData> MPEG2PSExtractor::getMetaData() {
    sp<MetaData> meta = new MetaData;

#ifndef ANDROID_DEFAULT_CODE
	size_t index = 0;
	off64_t u8SearchCount = 0;
	int MaxTry=1;
	bool found=false;

//ALOGD("[get meta in] mDurationUs:%lld, Track Number: %d ", mDurationUs, mTracks.size());   	

    mDataSource->getSize(&mFileSize);
	setDequeueState(false);
	mSystemHeaderValid = true;
	
	if(bisPlayable==true && mParseMaxTime==false){
		if (mFileSize <= kChunkSize)
		{
		  u8SearchCount = 1;
		}
		else
		{
	  	  u8SearchCount = mFileSize/kChunkSize;	  
		  MaxTry = u8SearchCount/4;
		}
		for (off64_t i = 1; i <= MaxTry; i++)
		{
		  //Set Search Start Offset
		  mOffset = (off64_t)((u8SearchCount - i) * kChunkSize);
	      mOffset = SearchValidOffset(mOffset);
		  if(mOffset < 0){
			//LOGD("for CONTINUE");
			continue;
		  }
	      mFinalResult = OK;
	      mBuffer->setRange(0, 0);
	            
	      while (feedMore() == OK) 
	      {    
	      }
	      
	      mDurationUs = getMaxPTS();
	      if (mDurationUs)
	      {
			found = true;			
			break;
	      }
		}
		if(found != true){
		  signalDiscontinuity();
		  mFinalResult = OK;
          mBuffer->setRange(0, 0);
		  mOffset = 0;
	      mOffset = SearchValidOffset(mOffset);
	      mFinalResult = OK;

		  int counter;
		  while (feedMore() == OK) 
	      {    
	      	 if (counter>MaxTry) {
        		break;
    		}
	      }
	      mDurationUs = getMaxPTS();
		}
	}

	meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_CONTAINER_MPEG2PS);
	/*if(mhasVTrack == false)
	  meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_MPEG2PS);
	else
	  meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG2PS);*/



	meta->setInt64(kKeyDuration,  getDurationUs());
	
	setDequeueState(true);
	mFinalResult = OK;
	mBuffer->setRange(0, 0);

	//Init Max PTS
	for (index=0; index<mTracks.size(); index++)
	{
	  mTracks.valueAt(index)->mMaxTimeUs = 0x0;
	}

	signalDiscontinuity();
	mOffset = 0;
	mSystemHeaderValid = false;
	//mDurationUs= 0;

	ALOGD("[get meta] mDurationUs:%lld, Track Number: %d ", mDurationUs, mTracks.size());
#else
    meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_CONTAINER_MPEG2PS);
#endif
    return meta;
}

uint32_t MPEG2PSExtractor::flags() const {
    #ifndef ANDROID_DEFAULT_CODE
    uint32_t flags = 0x0;

    flags = CAN_PAUSE | CAN_SEEK_BACKWARD | CAN_SEEK_FORWARD | CAN_SEEK;
    	
    return flags;
    #else //#ifndef ANDROID_DEFAULT_CODE
    return CAN_PAUSE;
    #endif //#ifndef ANDROID_DEFAULT_CODE
}

status_t MPEG2PSExtractor::feedMore() {
    Mutex::Autolock autoLock(mLock);
    #ifdef ANDROID_DEFAULT_CODE
    // How much data we're reading at a time
    static const size_t kChunkSize = kChunkSize;		//MP2PS changed on 2012/07/25
    #endif //#ifndef ANDROID_DEFAULT_CODE
    
    #ifndef ANDROID_DEFAULT_CODE
    int64_t pts_prev;
    int64_t pts;
    
	int64_t ret = -1;
	if (mSeeking) {
		pts_prev = mSearchPTS;

		if(pts = mhasVTrack?getMaxVideoPTS():getMaxPTS() > 0)
		{
			mSearchPTS = pts;
		}
		
		ALOGE("feedMore - Check Time Diff: %lld %lld ",pts/1000, mSeekTimeUs/1000);

		  if (pts > 0) {
			mMaxcount++;
			if (((pts - mSeekTimeUs < 50000) && (pts - mSeekTimeUs > -50000)) ||     //Sync with TS Extractor
			     mMinOffset == mMaxOffset || 
			     mMaxcount > 13) 
			{
				signalDiscontinuity();
				if( mhasVTrack == true ){
					  //ALOGE("feedMore - Seek to Target: %lld %lld, mMaxcount %d  ", pts/1000, mSeekTimeUs/1000, mMaxcount);
					  //mBuffer->setRange(0, 0);    //Need to Reset mBuffer Data		
					  
				      //ALOGE("[MP2PS] AAAA feedMore - Seek to Target: %lld", mOffset);
					  ret = getLastPESWithIFrame(mOffset);  //MP2PS'
					  //ret = getNextPESWithIFrame(mOffset);      //MP2PS for test
					  if(ret != -1 )
					  {
						mOffset = ret; 
						//ALOGE("B feedMore - Seek to Target: %lld", mOffset);			  	
					  }
					 else
					  {
						signalDiscontinuity();
						ret = getNextPESWithIFrame(mOffset);
						//ALOGE("mOffset wont be changed( %lld ) because findI returned -1", mOffset);			

						if(ret != -1 )
						{
						  mOffset = ret; 
						  //ALOGE("feedMore - Seek to Target: %lld", mOffset);			  
						}
						else
						{
						  //mOffset wont be changed
						  mOffset -= kChunkSize;/////////////////////////////////////////////////////////
						  signalDiscontinuity();
						  mOffset = SearchValidOffset(mOffset);
						  //ALOGE("[MP2PS]E mOffset wont be changed( %lld ) because findI returned -1", mOffset);		  
						}

					  }
				  }
				  else{
				  	mOffset = SearchValidOffset(mOffset);
				  }
				  
				  //signalDiscontinuity();
				  mBuffer->setRange(0, 0);    //Need to Reset mBuffer Data
				  mSeeking = false;
				  setDequeueState(true);
                  mSearchPTS = 0;
			}
			else 
		       {
			  signalDiscontinuity();
			  //ALPS00433667
			  if(pts!=pts_prev || 
                (mOffset>=mMaxOffset || mOffset<=mMinOffset))  //find until valid PTS
			  {                  
				  if(pts - mSeekTimeUs > 0)
				  {
				  	mMaxOffset = mSeekingOffset + kChunkSize;
				  	mSeekingOffset = (mMinOffset + mMaxOffset)/2;
				  }
				  else
				  {
				  	mMinOffset = mSeekingOffset + kChunkSize;
				  	mSeekingOffset = (mMinOffset + mMaxOffset)/2;
				  }
                       
				  mOffset = mSeekingOffset;
			  }
			  else
			  {
				  mMaxcount--;
			  }
			  mBuffer->setRange(0, 0);    //Need to Reset mBuffer Data
              mOffset = SearchValidOffset(mOffset);
			}
			ALOGD("pts=%lld, mSeekTimeUs=%lld, mMaxcount=%lld, mMinOffset=%lld, mMaxOffset=%lld, mSeekingOffset=%lld, mOffset = %lld ", 
				    pts, mSeekTimeUs, mMaxcount, mMinOffset, mMaxOffset, mSeekingOffset, mOffset);
		}
	}
#endif

    for (;;) {
        status_t err = dequeueChunk();
		//ALOGD("[FEED MORE BACK FROM DEQUEUE CHUNK] ERR =%d",err);
        if (err == -EAGAIN && mFinalResult == OK) {

			memmove(mBuffer->base(), mBuffer->data(), mBuffer->size());
            mBuffer->setRange(0, mBuffer->size());
            if (mBuffer->size() + kChunkSize > mBuffer->capacity()) {

                size_t newCapacity = (mBuffer->capacity()==0)?kChunkSize:(mBuffer->capacity()<<1);// + kChunkSize;
                ALOGD("Capacity %d->%d\n", mBuffer->capacity(), newCapacity);
                sp<ABuffer> newBuffer = new ABuffer(newCapacity);
                memcpy(newBuffer->data(), mBuffer->data(), mBuffer->size());
                newBuffer->setRange(0, mBuffer->size());
                mBuffer = newBuffer;
            }
			//ALOGD("[MP2PS] FeedMore read from file by offset %lld.", mOffset);


            ssize_t n = mDataSource->readAt(
                    mOffset, mBuffer->data() + mBuffer->size(), kChunkSize);

            if (n < (ssize_t)kChunkSize) {
                mFinalResult = (n < 0) ? (status_t)n : ERROR_END_OF_STREAM;
                return mFinalResult;
            }
            mBuffer->setRange(mBuffer->offset(), mBuffer->size() + n);
            mOffset += n;
        } 
		else if (err != OK) {
            mFinalResult = err;
            return err;
        } else {
            return OK;
        }
    }
}

status_t MPEG2PSExtractor::dequeueChunk() {
    if (mBuffer->size() < 4) {
        return -EAGAIN;
    }
/*    if (memcmp("\x00\x00\x01", mBuffer->data(), 3)) {
	 #ifndef ANDROID_DEFAULT_CODE
         ALOGE("dequeueChunk: Error: %x %x %x \n", *(mBuffer->data()), *(mBuffer->data()+1), *(mBuffer->data()+2));
	 #endif //#ifndef ANDROID_DEFAULT_CODE
        return ERROR_MALFORMED;
    }*/

#ifndef ANDROID_DEFAULT_CODE
	bool found=false;
	while (mBuffer->size() > 3){
		if(memcmp("\x00\x00\x01", mBuffer->data(), 3)) {
         //LOGE("dequeueChunk: Error: %x %x %x \n", *(mBuffer->data()), *(mBuffer->data()+1), *(mBuffer->data()+2));
		 /*if(mBuffer->size() == 1)
		 	break;*/
		 mBuffer->setRange(mBuffer->offset() + 1, mBuffer->size() - 1);
		}
		else{
			found = true;
			break;
		}
    }
	if(found == false){		//go forward for following in file
		ALOGD("dequeueChunk found no 000001");
//		return mBuffer->size();
//		mBuffer->setRange(0, mBuffer->size());	//AVSEQ05_MPEG.dat
		return -EAGAIN;
	}
#else
	if (memcmp("\x00\x00\x01", mBuffer->data(), 3)) {
		   return ERROR_MALFORMED;
	}
#endif //#ifndef ANDROID_DEFAULT_CODE

    unsigned chunkType = mBuffer->data()[3];

    ssize_t res;

    switch (chunkType) {
        case 0xba:
        {
            res = dequeuePack();
            break;
        }

        case 0xbb:
        {
            res = dequeueSystemHeader();
            break;
        }

        default:
        {
            res = dequeuePES();
            break;
        }
    }

    if (res > 0) {
        if (mBuffer->size() < (size_t)res) {
			//ALOGD("dequeueChunkError mBuffer->size is %d res is %d",mBuffer->size(),res);
			return -EAGAIN;
        }

        mBuffer->setRange(mBuffer->offset() + res, mBuffer->size() - res);
        res = OK;
    }
	//ALOGD("dequeueChunk mBuffer->size is %d res is %d",mBuffer->size(),res);

    return res;
}

#ifndef ANDROID_DEFAULT_CODE
ssize_t MPEG2PSExtractor::dequeuePack() {
    // 32 + 2 + 3 + 1 + 15 + 1 + 15+ 1 + 9 + 1 + 22 + 1 + 1 | +5
    unsigned ConstantHdrLength = 14;
    	
    if (mBuffer->size() < 12) {  //14->12
        return -EAGAIN;
    }

    if ((*(mBuffer->data()+4) >> 6) != 1)
    {
      ConstantHdrLength = 12;
      return ConstantHdrLength;    
		//ALOGD("MPEG1 File Format \n");
    }
    else
    {
      //ALOGD("MPEG2 File Format \n");
      unsigned pack_stuffing_length = mBuffer->data()[13] & 7;	  
	  return pack_stuffing_length + ConstantHdrLength;
    }
}
#else
ssize_t MPEG2PSExtractor::dequeuePack() {
    	
    if (mBuffer->size() < 14) {
        return -EAGAIN;
    }

    unsigned pack_stuffing_length = mBuffer->data()[13] & 7;

    return pack_stuffing_length + 14;
}
#endif //#ifndef ANDROID_DEFAULT_CODE


ssize_t MPEG2PSExtractor::dequeueSystemHeader() {
    if (mBuffer->size() < 6) {
        return -EAGAIN;
    }

    unsigned header_length = U16_AT(mBuffer->data() + 4);
    #ifndef ANDROID_DEFAULT_CODE
    mSystemHeaderValid = true;
    #endif
    return header_length + 6;
}
#ifndef ANDROID_DEFAULT_CODE
///////////////////////////////
int MPEG2PSExtractor::findNextPES(const void* data,int length)
{
  //Search Start Code & StreamID
  uint8_t* p = (uint8_t*)data;
  int index = 0;
  int off=0;
  unsigned stream_id_Video = 0xFF;
  unsigned stream_id_Audio = 0xFF;
  unsigned temp;
  //ALOGE("feedMore - Search Start = %lld \n", offset);
  if (length <= 6)
  {
    return -1;
  }
  for (index=0; index<mTracks.size(); index++)
  {
	   if (mTracks.valueAt(index)->isVideo())
	   {
            stream_id_Video = mTracks.valueAt(index)->mStreamID;
	   }
	   if (mTracks.valueAt(index)->isAudio())
	   {
            stream_id_Audio = mTracks.valueAt(index)->mStreamID;
	   }
  }
    
	while (off < length - 3) {
		temp = p[off+3];
		if( p[off] == 0x00 && p[off+1] == 0x00 && p[off+2] == 0x01 &&
		((mhasVTrack == true && temp == stream_id_Video)||
		   (mhasATrack == true && temp == stream_id_Audio)||
		   temp == 0xb9 || temp == 0xbe || temp == 0xbc || temp == 0xbf ||
		   temp == 0xf0 || temp == 0xf1 || temp == 0xff || temp == 0xf2 || temp == 0xf8)){
		     return off;
		}
		off++;
	}
  return -1;
}

///////////////////////////////
ssize_t MPEG2PSExtractor::dequeuePES() {
    if (mBuffer->size() < 6) {
        return -EAGAIN;
    }
    unsigned PTS_DTS_flags;
    uint64_t PTS = 0;
    uint64_t DTS = 0; 
    unsigned dataLength;
    unsigned PES_packet_length = U16_AT(mBuffer->data() + 4);
	unsigned substid;

    //ALOGD("dequeuePES: %02x %02x %02x %02x %02x %02x ",
    //          *(mBuffer->data()+0),*(mBuffer->data()+1),*(mBuffer->data()+2),*(mBuffer->data()+3),*(mBuffer->data()+4),*(mBuffer->data()+5));
	unsigned stream_id_tmp = U16_AT(mBuffer->data() + 2);
	if(stream_id_tmp == 0x01b9){
		return 4;
	}
	while(PES_packet_length < 3 || stream_id_tmp < 0x01bc){	//ALPS00440695 seek to 3:06,  file offset 0x7EAD2E9 has PES_packet_length = 1;.dat files have invalid stream_id
		ALOGD("Hdr Err PES_packet_length=%d, stream_id=0x%x\n", PES_packet_length, stream_id_tmp);
		//CHECK_NE(PES_packet_length, 0u);   //monica0826
		int find = -1;
		//ALPS00433594
		mBuffer->setRange(mBuffer->offset()+3, mBuffer->size()-3); //ignore current PES because it's length==0
        
		find = findNextPES(mBuffer->data(),mBuffer->size());
		if(find > mBuffer->size() || find < 0){
			//ALOGD("find ofst %d/%d, len %d, ret\n", find, mBuffer->size(), PES_packet_length);
			return -EAGAIN;
		}
		else{
			mBuffer->setRange(mBuffer->offset()+find, mBuffer->size()-find);
			PES_packet_length = U16_AT(mBuffer->data() + 4);
			stream_id_tmp = U16_AT(mBuffer->data() + 2);
		}
		//ALOGD("find ofst %d/%d, len %d, %x %x %x %x %x %x \n", 
			//find, mBuffer->size(), PES_packet_length,
			//*(mBuffer->data()+0), *(mBuffer->data()+1), *(mBuffer->data()+2),
			//*(mBuffer->data()+3), *(mBuffer->data()+4), *(mBuffer->data()+5));

	}    
    size_t n = PES_packet_length + 6;

    //ALOGD("dequeuePES, bufSz %d/PES_Len %d\n", mBuffer->size(), n);

    if (mBuffer->size() < n) {
        return -EAGAIN;
    }

    ABitReader br(mBuffer->data(), n);

    unsigned packet_startcode_prefix = br.getBits(24);

    ALOGV("packet_startcode_prefix = 0x%08x", packet_startcode_prefix);

    if (packet_startcode_prefix != 1) {
        ALOGD("Supposedly payload_unit_start=1 unit does not start "
             "with startcode.");

        return ERROR_MALFORMED;
    }

    //CHECK_EQ(packet_startcode_prefix, 0x000001u);   //already checked in dequeueChunk

    unsigned stream_id = br.getBits(8);
    ALOGV("stream_id = 0x%02x, len %d, mOffset %lld\n", stream_id, PES_packet_length, mOffset);

    /* unsigned PES_packet_length = */br.getBits(16);

    //ALOGD("dequeuePES Header: %02x %02x %02x %02x %02x %02x ",
    //          *(br.data()+0),*(br.data()+1),*(br.data()+2),*(br.data()+3),*(br.data()+4),*(br.data()+5));
        	
    if (stream_id == 0xbc) {
        // program_stream_map

        if (!mScanning) {
            return n;
        }

        mStreamTypeByESID.clear();

        /* unsigned current_next_indicator = */br.getBits(1);
        /* unsigned reserved = */br.getBits(2);
        /* unsigned program_stream_map_version = */br.getBits(5);
        /* unsigned reserved = */br.getBits(7);
        /* unsigned marker_bit = */br.getBits(1);
        unsigned program_stream_info_length = br.getBits(16);

        size_t offset = 0;
        while (offset < program_stream_info_length) {
            if (offset + 2 > program_stream_info_length) {
                return ERROR_MALFORMED;
            }

            unsigned descriptor_tag = br.getBits(8);
            unsigned descriptor_length = br.getBits(8);

            ALOGI("found descriptor tag 0x%02x of length %u",
                 descriptor_tag, descriptor_length);

            if (offset + 2 + descriptor_length > program_stream_info_length) {
                return ERROR_MALFORMED;
            }

            br.skipBits(8 * descriptor_length);

            offset += 2 + descriptor_length;
        }

        unsigned elementary_stream_map_length = br.getBits(16);

        offset = 0;
        while (offset < elementary_stream_map_length) {
            if (offset + 4 > elementary_stream_map_length) {
                return ERROR_MALFORMED;
            }

            unsigned stream_type = br.getBits(8);
            unsigned elementary_stream_id = br.getBits(8);

	     //ALOGI("elementary stream id 0x%02x has stream type 0x%02x",
            //     elementary_stream_id, stream_type);
	     
            mStreamTypeByESID.add(elementary_stream_id, stream_type);

            unsigned elementary_stream_info_length = br.getBits(16);

            if (offset + 4 + elementary_stream_info_length
                    > elementary_stream_map_length) {
                return ERROR_MALFORMED;
            }

            offset += 4 + elementary_stream_info_length;
        }

        /* unsigned CRC32 = */br.getBits(32);

        mProgramStreamMapValid = true;
    } else if (stream_id != 0xbe  // padding_stream
            //&& stream_id != 0xbd  // private_stream_1
            && stream_id != 0xbf  // private_stream_2
            && stream_id != 0xf0  // ECM
            && stream_id != 0xf1  // EMM
            && stream_id != 0xff  // program_stream_directory
            && stream_id != 0xf2  // DSMCC
            && stream_id != 0xf8
            && ((stream_id == 0xbd)||(stream_id >= 0xc0 && stream_id <= 0xdf) || (stream_id >= 0xe0 && stream_id <= 0xef))) {	 

	//check if PCM
	if(stream_id == 0xbd ){
		unsigned peek_hdr_length = *(mBuffer->data() + 8);
		substid =*(mBuffer->data() + 9 + peek_hdr_length);
		if( substid < 0xA0 || substid > 0xA7 ){ // not between 0xA0 to 0xA7
			//ALOGD("FALLLL peek_hdr_length=%02x substid = %02x QQQ %02x %02x %02x %02x", peek_hdr_length,substid,*(mBuffer->data()+5),*(mBuffer->data()+6),*(mBuffer->data()+7),*(mBuffer->data()+8));
			br.skipBits(PES_packet_length * 8);
			return n;
		}
    }
	//			
	
	 unsigned next2bits = *(br.data())>>6;
	 if(next2bits == 2u)
	 {
	 //MPEG2 Spec
	 //ALOGD("MPEG2 PES \n");
	 
	 //CHECK_EQ(br.getBits(2), 2u);
	 br.getBits(2);
     mMPEG1Flag = false;
        /* unsigned PES_scrambling_control = */br.getBits(2);
        /* unsigned PES_priority = */br.getBits(1);
        /* unsigned data_alignment_indicator = */br.getBits(1);
        /* unsigned copyright = */br.getBits(1);
        /* unsigned original_or_copy = */br.getBits(1);

        PTS_DTS_flags = br.getBits(2);
        //unsigned PTS_DTS_flags = br.getBits(2);
        ALOGV("PTS_DTS_flags = %u", PTS_DTS_flags);

        unsigned ESCR_flag = br.getBits(1);
        ALOGV("ESCR_flag = %u", ESCR_flag);

        unsigned ES_rate_flag = br.getBits(1);
        ALOGV("ES_rate_flag = %u", ES_rate_flag);

        unsigned DSM_trick_mode_flag = br.getBits(1);
        ALOGV("DSM_trick_mode_flag = %u", DSM_trick_mode_flag);

        unsigned additional_copy_info_flag = br.getBits(1);
        ALOGV("additional_copy_info_flag = %u", additional_copy_info_flag);

        /* unsigned PES_CRC_flag = */br.getBits(1);
        /* PES_extension_flag = */br.getBits(1);

        unsigned PES_header_data_length = br.getBits(8);
        ALOGV("PES_header_data_length = %u", PES_header_data_length);

        unsigned optional_bytes_remaining = PES_header_data_length; 

        if (PTS_DTS_flags == 2 || PTS_DTS_flags == 3) {
            //CHECK_GE(optional_bytes_remaining, 5u);
			br.getBits(4);
            //CHECK_EQ(br.getBits(4), PTS_DTS_flags);

            PTS = ((uint64_t)br.getBits(3)) << 30;
			br.getBits(1);
            //CHECK_EQ(br.getBits(1), 1u);
            PTS |= ((uint64_t)br.getBits(15)) << 15;
			br.getBits(1);
			//CHECK_EQ(br.getBits(1), 1u);
            PTS |= br.getBits(15);
			br.getBits(1);
			//CHECK_EQ(br.getBits(1), 1u);

            //LOGD("PTS = %llu", PTS);
            
            optional_bytes_remaining -= 5;

            if (PTS_DTS_flags == 3) {
                //CHECK_GE(optional_bytes_remaining, 5u);
				br.getBits(4);
                //CHECK_EQ(br.getBits(4), 1u);

                DTS = ((uint64_t)br.getBits(3)) << 30;
				br.getBits(1);
				//CHECK_EQ(br.getBits(1), 1u);
                DTS |= ((uint64_t)br.getBits(15)) << 15;
				br.getBits(1);
				//CHECK_EQ(br.getBits(1), 1u);
                DTS |= br.getBits(15);
				br.getBits(1);
				//CHECK_EQ(br.getBits(1), 1u);

                ALOGV("DTS = %llu", DTS);

                optional_bytes_remaining -= 5;
            }
        }

        if (ESCR_flag) {
            //CHECK_GE(optional_bytes_remaining, 6u);

            br.getBits(2);

            uint64_t ESCR = ((uint64_t)br.getBits(3)) << 30;
			br.getBits(1);
			//CHECK_EQ(br.getBits(1), 1u);
            ESCR |= ((uint64_t)br.getBits(15)) << 15;
			br.getBits(1);
			//CHECK_EQ(br.getBits(1), 1u);
            ESCR |= br.getBits(15);
			br.getBits(1);
			//CHECK_EQ(br.getBits(1), 1u);

            ALOGV("ESCR = %llu", ESCR);
            /* unsigned ESCR_extension = */br.getBits(9);
			br.getBits(1);
            //CHECK_EQ(br.getBits(1), 1u);

            optional_bytes_remaining -= 6;
        }

        if (ES_rate_flag) {
            //CHECK_GE(optional_bytes_remaining, 3u);
			br.getBits(1);
            //CHECK_EQ(br.getBits(1), 1u);
            /* unsigned ES_rate = */br.getBits(22);
			br.getBits(1);
			//CHECK_EQ(br.getBits(1), 1u);

            optional_bytes_remaining -= 3;
        }

        br.skipBits(optional_bytes_remaining * 8);
		 
        // ES data follows.

        //CHECK_GE(PES_packet_length, PES_header_data_length + 3);

        dataLength = PES_packet_length - 3 - PES_header_data_length;

        if (br.numBitsLeft() < dataLength * 8) {
            ALOGE("PES packet does not carry enough data to contain "
                 "payload. (numBitsLeft = %d, required = %d)",
                 br.numBitsLeft(), dataLength * 8);

            return ERROR_MALFORMED;
        }

        //CHECK_GE(br.numBitsLeft(), dataLength * 8);
	 }
	 else  
	 {
	   //MPEG1 Spec
       //ALOGD("MPEG1 PES \n");
	   
	   unsigned offset = 0;
	   mMPEG1Flag = true;
	   while(offset < 17 && *(br.data())== 0xff)
	   {
	     br.skipBits(8);
	     offset++;
	   }
	   
	   if(offset == 17)//stuffing bytes no more than 16 bytes
	   {
	     ALOGD("*********************parsePES ERROR:too much MPEG-1 stuffing*********************");
	     return offset;
	   }

	   next2bits = *(br.data())>>6;
          if(next2bits== 0x01)
	   {
	     br.getBits(2);	//mtk02420
	     unsigned STD_buffer_scale = br.getBits(1);
	     uint32_t STD_buffer_size = br.getBits(13);
	     offset += 2;
	   }
          
	   PTS_DTS_flags = *(br.data())>>4;
	   //ALOGD("Mpeg1 - PTS_DTS_flags = %d %x", PTS_DTS_flags, *(br.data()));
	   
	   if (PTS_DTS_flags == 2 || PTS_DTS_flags == 3) 
	   {
	     offset += 5;
	     br.skipBits(4);
	     PTS = ((uint64_t)br.getBits(3)) << 30;
		 
	     br.getBits(1);
	     //CHECK_EQ(br->getBits(1), 1u);
		            
	     PTS |= ((uint64_t)br.getBits(15)) << 15;

	     br.getBits(1);
	     //CHECK_EQ(br->getBits(1), 1u);
		            
	     PTS |= br.getBits(15);

	     br.getBits(1);
	     //CHECK_EQ(br->getBits(1), 1u);

	     //ALOGD("mpeg1 - PTS = %llu", PTS);

	     if (PTS_DTS_flags == 3) 
	     {
 	       offset += 5;
				  
		br.getBits(4);
		//CHECK_EQ(br->getBits(4), 1u);

		DTS = ((uint64_t)br.getBits(3)) << 30;

		br.getBits(1);
		//CHECK_EQ(br->getBits(1), 1u);
		                
		DTS |= ((uint64_t)br.getBits(15)) << 15;

		br.getBits(1);
		//CHECK_EQ(br->getBits(1), 1u);
		                
		DTS |= br.getBits(15);

		br.getBits(1);
		//CHECK_EQ(br->getBits(1), 1u);
//ALOGD("mpeg1 - DTS = %llu", DTS);	//bella
	     }
	   }
	   else 
	   {
	     offset += 1;
	     unsigned NO_PTSDTS_FFData = br.getBits(8) & 0xF;   
	     if (NO_PTSDTS_FFData != 0xF)
	     {
              ALOGD("parsePES: Skip No PTS/DTS Error = %x \n", NO_PTSDTS_FFData);
	     }
	   }

	   dataLength = PES_packet_length - offset;
	 }

        if (PTS_DTS_flags != 2 && PTS_DTS_flags != 3) {
            ALOGV("dequeuePES PTS_DTS_flags %d, PTS %llu, fileOffs 0x%x", PTS_DTS_flags, PTS/90, mOffset);
        }

        ssize_t index = mTracks.indexOfKey(stream_id);
        if (index < 0 && mScanning && mSystemHeaderValid) {
            unsigned streamType;

            ssize_t streamTypeIndex;
            if (mProgramStreamMapValid && (streamTypeIndex = mStreamTypeByESID.indexOfKey(stream_id)) >= 0) {
                streamType = mStreamTypeByESID.valueAt(streamTypeIndex);
            } else if ((stream_id & ~0x1f) == 0xc0) {
                // ISO/IEC 13818-3 or ISO/IEC 11172-3 or ISO/IEC 13818-7
                // or ISO/IEC 14496-3 audio
                streamType = ATSParser::STREAMTYPE_MPEG2_AUDIO;
            }else if (stream_id == 0xbd){
                // uncompressed auio, LPCM
                streamType = ATSParser::STREAMTYPE_AUDIO_PSLPCM;
				 /*if( channel_configuration <= 2 ){
				   streamType = ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS;
				 }
				 else{
				   streamType = ATSParser::STREAMTYPE_RESERVED;
				 }*/
            }else if ((stream_id & ~0x0f) == 0xe0) {
                // ISO/IEC 13818-2 or ISO/IEC 11172-2 or ISO/IEC 14496-2 video
                streamType = ATSParser::STREAMTYPE_MPEG2_VIDEO;
            } else {
                streamType = ATSParser::STREAMTYPE_RESERVED;
            }
	     
	     //Add Check For Audio/Video Code When No Program Stream Map
	     if (mProgramStreamMapValid == false)
	     {
	       if (streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO)
	       {
	          //Video Part  
			  unsigned StartCode[5];
			  StartCode[0] = *(br.data());
			  StartCode[1] = *(br.data()+1);
			  StartCode[2] = *(br.data()+2);
			  StartCode[3] = *(br.data()+3);
			  StartCode[4] = *(br.data()+4);

			  if ((StartCode[0] == 0x0) && (StartCode[1] == 0x0) && (StartCode[2] == 0x01) && (StartCode[3] == 0xB3))    //MPEG2 or MPEG1
			  {
			    if(mMPEG1Flag == true)		//[MP2PS] add to distinct MPEG1 and MPEG2
				{
				  streamType = ATSParser::STREAMTYPE_MPEG1_VIDEO;			  
				}
				else
				{
				  streamType = ATSParser::STREAMTYPE_MPEG2_VIDEO;
				}
			  }
			  else if ((StartCode[0] == 0x0) && (StartCode[1] == 0x0) && (StartCode[2] == 0x00) && (StartCode[3] == 0x01) && (StartCode[4] == 0x67))    //H264
			  {
			    streamType = ATSParser::STREAMTYPE_H264;
			  }
			  else if ((StartCode[0] == 0x0) && (StartCode[1] == 0x0) && (StartCode[2] == 0x01) && (StartCode[3] == 0xB0))    //MP4
			  {
			    streamType = ATSParser::STREAMTYPE_MPEG4_VIDEO;
			  }
			  else
			  {
			    //TODO
			    streamType = ATSParser::STREAMTYPE_RESERVED;

				//try to sequentially search 128 bytes
				uint8_t* paucTmpBuf = (uint8_t*)br.data();
				int32_t idx, buf_size = ((br.numBitsLeft()>>3) > 128)?128:(br.numBitsLeft()>>3);
				for(idx = 0;idx < buf_size-5; idx++)
				{
					if (!memcmp("\x00\x00\x00\x01", &paucTmpBuf[idx], 4))
					{
						if(paucTmpBuf[idx + 4] == 0x67)
						{
							streamType = ATSParser::STREAMTYPE_H264;
							break;
						}
					}
					else if(!memcmp("\x00\x00\x01", &paucTmpBuf[idx], 3))
					{
						if(paucTmpBuf[idx + 3] == 0xB3)
						{
							if(mMPEG1Flag == true)		//[MP2PS] add to distinct MPEG1 and MPEG2
							{
							  streamType = ATSParser::STREAMTYPE_MPEG1_VIDEO;			  
							}
							else
							{
							  streamType = ATSParser::STREAMTYPE_MPEG2_VIDEO;
							}
							break;

						}
						else if(paucTmpBuf[idx + 3] == 0xB0)
						{
							streamType = ATSParser::STREAMTYPE_MPEG4_VIDEO;
							break;
						}
					}
				}
				ALOGD("V:No PMT and search type %d\n",streamType);

			  }
	       }
	       else if (streamType == ATSParser::STREAMTYPE_MPEG2_AUDIO)
	       {
	         //Audio Part
	         if (IsSeeminglyValidADTSHeader(br.data(), dataLength))
	         {
				 streamType = ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS;
	         }
			  else if (IsSeeminglyValidMPEGAudioHeader(br.data(), dataLength))
			  {
			    streamType = ATSParser::STREAMTYPE_MPEG2_AUDIO;
			  }
			  else
			  {
			    //TODO
			    streamType = ATSParser::STREAMTYPE_RESERVED;
				ALOGD("A:No PMT and startcode unknown\n");
			  }
	       }
	     }

//mtk02420 2012/08/07
//filter audiostream with more than 6 channels
		//ALOGD("streamType=%d",streamType);

		if( streamType == ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS || (streamType == ATSParser::STREAMTYPE_MPEG2_AUDIO && IsSeeminglyValidADTSHeader(br.data(), dataLength) )){
			//AAC Audio
			/*peer the channel number for filttering
			 adts_fixed_header()//7bytes
				 {
					 syncword; 12 bslbf
					 ID; 1 bslbf
					 layer; 2 uimsbf
					 protection_absent; 1 bslbf
					 profile; 2 uimsbf
					 sampling_frequency_index; 4 uimsbf
					 private_bit; 1 bslbf
					 channel_configuration; 3 uimsbf
					 original_copy; 1 bslbf
					 home; 1 bslbf
				 }				
			 */
			 unsigned ADTSChannelConfig[2];
			 ADTSChannelConfig[0] = *(br.data()+2);
			 ADTSChannelConfig[1] = *(br.data()+3);
//ALOGD(" streamType=%d",streamType);
			 unsigned channel_configuration = ((ADTSChannelConfig[0]&0x01)<<2)+((ADTSChannelConfig[1]&0xC0)>>6);
			 ALOGD("found AAC codec config %d channels)", channel_configuration);
			 
			 if( channel_configuration <= 2 ){
			   streamType = ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS;
			 }
			 else{
			   streamType = ATSParser::STREAMTYPE_RESERVED;
			 }
      	}
		
	     //ALOGD("PES - Add Track: %x %x %x \n", stream_id, streamType, mProgramStreamMapValid);

	     if (streamType != ATSParser::STREAMTYPE_RESERVED)
	     {
			ALOGD("mhasATrack=%d mhasVTrack=%d",mhasATrack,mhasVTrack);
			/*if( bisPlayable == false ){
				bisPlayable = true;
				index = mTracks.add(stream_id, new Track(this, stream_id, streamType));
				ALOGD("[mtk02420]Add to bend a file");
			}
			else*/ if(mhasVTrack == false &&
				(streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || 
				 streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO || 
				 streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO || 
				 streamType == ATSParser::STREAMTYPE_H264)){
			  index = mTracks.add(stream_id, new Track(this, stream_id, streamType));
			  mhasVTrack = true;
			  ALOGD("PES - add video track %02X",stream_id);
			}
			else if( mhasATrack == false && 
			  (streamType == ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS|| 
			   streamType == ATSParser::STREAMTYPE_MPEG1_AUDIO|| 
			   streamType == ATSParser::STREAMTYPE_MPEG2_AUDIO ||
			   streamType == ATSParser::STREAMTYPE_AUDIO_PSLPCM)){
			  index = mTracks.add(stream_id, new Track(this, stream_id, streamType));
			  mhasATrack= true;
  			  ALOGD("PES - add audio track %02X",stream_id);
			}
	     }
        }

        status_t err = OK;
        if (index >= 0 && mSystemHeaderValid) {
//ALOGD("append %c, len %d, pts %lld, moffset %lld\n", 
//	mTracks.editValueAt(index)->isVideo()?'V':'A', dataLength, PTS*100/9, mOffset);
            err =
                mTracks.editValueAt(index)->appendPESData(
                    PTS_DTS_flags, PTS, DTS, br.data(), dataLength);
        }
        br.skipBits(dataLength * 8);

        if (err != OK) {
            return err;
        }
    } else if (stream_id == 0xbe) {  // padding_stream
        //CHECK_NE(PES_packet_length, 0u);
        br.skipBits(PES_packet_length * 8);
    } else {
        //CHECK_NE(PES_packet_length, 0u);
        br.skipBits(PES_packet_length * 8);
    }

    return n;
}

#else //#ifndef ANDROID_DEFAULT_CODE
ssize_t MPEG2PSExtractor::dequeuePES() {
    if (mBuffer->size() < 6) {
        return -EAGAIN;
    }

    unsigned PES_packet_length = U16_AT(mBuffer->data() + 4);
    CHECK_NE(PES_packet_length, 0u);

    size_t n = PES_packet_length + 6;

    if (mBuffer->size() < n) {
        return -EAGAIN;
    }

    ABitReader br(mBuffer->data(), n);

    unsigned packet_startcode_prefix = br.getBits(24);

    ALOGV("packet_startcode_prefix = 0x%08x", packet_startcode_prefix);

    if (packet_startcode_prefix != 1) {
        ALOGV("Supposedly payload_unit_start=1 unit does not start "
             "with startcode.");

        return ERROR_MALFORMED;
    }

    CHECK_EQ(packet_startcode_prefix, 0x000001u);

    unsigned stream_id = br.getBits(8);
    ALOGV("stream_id = 0x%02x", stream_id);

    /* unsigned PES_packet_length = */br.getBits(16);

    if (stream_id == 0xbc) {
        // program_stream_map

        if (!mScanning) {
            return n;
        }

        mStreamTypeByESID.clear();

        /* unsigned current_next_indicator = */br.getBits(1);
        /* unsigned reserved = */br.getBits(2);
        /* unsigned program_stream_map_version = */br.getBits(5);
        /* unsigned reserved = */br.getBits(7);
        /* unsigned marker_bit = */br.getBits(1);
        unsigned program_stream_info_length = br.getBits(16);

        size_t offset = 0;
        while (offset < program_stream_info_length) {
            if (offset + 2 > program_stream_info_length) {
                return ERROR_MALFORMED;
            }

            unsigned descriptor_tag = br.getBits(8);
            unsigned descriptor_length = br.getBits(8);

            ALOGI("found descriptor tag 0x%02x of length %u",
                 descriptor_tag, descriptor_length);

            if (offset + 2 + descriptor_length > program_stream_info_length) {
                return ERROR_MALFORMED;
            }

            br.skipBits(8 * descriptor_length);

            offset += 2 + descriptor_length;
        }

        unsigned elementary_stream_map_length = br.getBits(16);

        offset = 0;
        while (offset < elementary_stream_map_length) {
            if (offset + 4 > elementary_stream_map_length) {
                return ERROR_MALFORMED;
            }

            unsigned stream_type = br.getBits(8);
            unsigned elementary_stream_id = br.getBits(8);

            ALOGI("elementary stream id 0x%02x has stream type 0x%02x",
                 elementary_stream_id, stream_type);

            mStreamTypeByESID.add(elementary_stream_id, stream_type);

            unsigned elementary_stream_info_length = br.getBits(16);

            if (offset + 4 + elementary_stream_info_length
                    > elementary_stream_map_length) {
                return ERROR_MALFORMED;
            }

            offset += 4 + elementary_stream_info_length;
        }

        /* unsigned CRC32 = */br.getBits(32);

        mProgramStreamMapValid = true;
    } else if (stream_id != 0xbe  // padding_stream
            && stream_id != 0xbf  // private_stream_2
            && stream_id != 0xf0  // ECM
            && stream_id != 0xf1  // EMM
            && stream_id != 0xff  // program_stream_directory
            && stream_id != 0xf2  // DSMCC
            && stream_id != 0xf8) {  // H.222.1 type E
        CHECK_EQ(br.getBits(2), 2u);

        /* unsigned PES_scrambling_control = */br.getBits(2);
        /* unsigned PES_priority = */br.getBits(1);
        /* unsigned data_alignment_indicator = */br.getBits(1);
        /* unsigned copyright = */br.getBits(1);
        /* unsigned original_or_copy = */br.getBits(1);

        unsigned PTS_DTS_flags = br.getBits(2);
        ALOGV("PTS_DTS_flags = %u", PTS_DTS_flags);

        unsigned ESCR_flag = br.getBits(1);
        ALOGV("ESCR_flag = %u", ESCR_flag);

        unsigned ES_rate_flag = br.getBits(1);
        ALOGV("ES_rate_flag = %u", ES_rate_flag);

        unsigned DSM_trick_mode_flag = br.getBits(1);
        ALOGV("DSM_trick_mode_flag = %u", DSM_trick_mode_flag);

        unsigned additional_copy_info_flag = br.getBits(1);
        ALOGV("additional_copy_info_flag = %u", additional_copy_info_flag);

        /* unsigned PES_CRC_flag = */br.getBits(1);
        /* PES_extension_flag = */br.getBits(1);

        unsigned PES_header_data_length = br.getBits(8);
        ALOGV("PES_header_data_length = %u", PES_header_data_length);

        unsigned optional_bytes_remaining = PES_header_data_length;

        uint64_t PTS = 0, DTS = 0;

        if (PTS_DTS_flags == 2 || PTS_DTS_flags == 3) {
            CHECK_GE(optional_bytes_remaining, 5u);

            CHECK_EQ(br.getBits(4), PTS_DTS_flags);

            PTS = ((uint64_t)br.getBits(3)) << 30;
            CHECK_EQ(br.getBits(1), 1u);
            PTS |= ((uint64_t)br.getBits(15)) << 15;
            CHECK_EQ(br.getBits(1), 1u);
            PTS |= br.getBits(15);
            CHECK_EQ(br.getBits(1), 1u);

            ALOGV("PTS = %llu", PTS);
            // ALOGI("PTS = %.2f secs", PTS / 90000.0f);

            optional_bytes_remaining -= 5;

            if (PTS_DTS_flags == 3) {
                CHECK_GE(optional_bytes_remaining, 5u);

                CHECK_EQ(br.getBits(4), 1u);

                DTS = ((uint64_t)br.getBits(3)) << 30;
                CHECK_EQ(br.getBits(1), 1u);
                DTS |= ((uint64_t)br.getBits(15)) << 15;
                CHECK_EQ(br.getBits(1), 1u);
                DTS |= br.getBits(15);
                CHECK_EQ(br.getBits(1), 1u);

                ALOGV("DTS = %llu", DTS);

                optional_bytes_remaining -= 5;
            }
        }

        if (ESCR_flag) {
            CHECK_GE(optional_bytes_remaining, 6u);

            br.getBits(2);

            uint64_t ESCR = ((uint64_t)br.getBits(3)) << 30;
            CHECK_EQ(br.getBits(1), 1u);
            ESCR |= ((uint64_t)br.getBits(15)) << 15;
            CHECK_EQ(br.getBits(1), 1u);
            ESCR |= br.getBits(15);
            CHECK_EQ(br.getBits(1), 1u);

            ALOGV("ESCR = %llu", ESCR);
            /* unsigned ESCR_extension = */br.getBits(9);

            CHECK_EQ(br.getBits(1), 1u);

            optional_bytes_remaining -= 6;
        }

        if (ES_rate_flag) {
            CHECK_GE(optional_bytes_remaining, 3u);

            CHECK_EQ(br.getBits(1), 1u);
            /* unsigned ES_rate = */br.getBits(22);
            CHECK_EQ(br.getBits(1), 1u);

            optional_bytes_remaining -= 3;
        }

        br.skipBits(optional_bytes_remaining * 8);

        // ES data follows.

        CHECK_GE(PES_packet_length, PES_header_data_length + 3);

        unsigned dataLength =
            PES_packet_length - 3 - PES_header_data_length;

        if (br.numBitsLeft() < dataLength * 8) {
            ALOGE("PES packet does not carry enough data to contain "
                 "payload. (numBitsLeft = %d, required = %d)",
                 br.numBitsLeft(), dataLength * 8);

            return ERROR_MALFORMED;
        }

        CHECK_GE(br.numBitsLeft(), dataLength * 8);

        ssize_t index = mTracks.indexOfKey(stream_id);
        if (index < 0 && mScanning) {
            unsigned streamType;

            ssize_t streamTypeIndex;
            if (mProgramStreamMapValid
                    && (streamTypeIndex =
                            mStreamTypeByESID.indexOfKey(stream_id)) >= 0) {
                streamType = mStreamTypeByESID.valueAt(streamTypeIndex);
            } else if ((stream_id & ~0x1f) == 0xc0) {
                // ISO/IEC 13818-3 or ISO/IEC 11172-3 or ISO/IEC 13818-7
                // or ISO/IEC 14496-3 audio
                streamType = ATSParser::STREAMTYPE_MPEG2_AUDIO;
            } else if ((stream_id & ~0x0f) == 0xe0) {
                // ISO/IEC 13818-2 or ISO/IEC 11172-2 or ISO/IEC 14496-2 video
                streamType = ATSParser::STREAMTYPE_MPEG2_VIDEO;
            } else {
                streamType = ATSParser::STREAMTYPE_RESERVED;
            }

            index = mTracks.add(
                    stream_id, new Track(this, stream_id, streamType));
        }

        status_t err = OK;

        if (index >= 0) {
            err =
                mTracks.editValueAt(index)->appendPESData(
                    PTS_DTS_flags, PTS, DTS, br.data(), dataLength);
        }

        br.skipBits(dataLength * 8);

        if (err != OK) {
            return err;
        }
    } else if (stream_id == 0xbe) {  // padding_stream
        CHECK_NE(PES_packet_length, 0u);
        br.skipBits(PES_packet_length * 8);
    } else {
        CHECK_NE(PES_packet_length, 0u);
        br.skipBits(PES_packet_length * 8);
    }

    return n;
}
#endif //#ifndef ANDROID_DEFAULT_CODE

#ifndef ANDROID_DEFAULT_CODE
void MPEG2PSExtractor::setDequeueState(bool needDequeuePES) {  //For Seek  //true: When Enable Seek, false: When Get Target Seek Time
	mNeedDequeuePES = needDequeuePES;
}

bool MPEG2PSExtractor::getDequeueState() {    //For Seek
	return mNeedDequeuePES;
}

//get duration
int64_t MPEG2PSExtractor::getMaxPTS() {     //For Seek
	int64_t maxPTS=0;
	for (size_t i = 0; i < mTracks.size(); ++i) {
		int64_t pts = mTracks.editValueAt(i)->getPTS();
		if (maxPTS < pts) {
			maxPTS = pts;
		}
	}
	return maxPTS;
}


int64_t MPEG2PSExtractor::getMaxVideoPTS() {     //For Seek
	int64_t maxPTS=0;
	for (size_t i = 0; i < mTracks.size(); ++i) {
		int64_t pts = mTracks.editValueAt(i)->getPTS();
		if (maxPTS < pts && mTracks.editValueAt(i)->isVideo()) {
			maxPTS = pts;
		}
	}
	return maxPTS;
}


void MPEG2PSExtractor::seekTo(int64_t seekTimeUs, unsigned StreamID) {
    Mutex::Autolock autoLock(mLock);

    ALOGE("seekTo:mDurationMs =%lld,seekTimeMs= %lld",mDurationUs/1000,seekTimeUs/1000);
	
    if (seekTimeUs == 0) 
    {
        mOffset = 0;
	 mSeeking = false;
	 
	 signalDiscontinuity();
    } 
    else if((mDurationUs-seekTimeUs) < 200000)
    {
      mOffset = mFileSize;
      mSeeking = false;
	  
      signalDiscontinuity(); 
    }
    else 
    {
      signalDiscontinuity();
	  
      mSeekingOffset = mOffset;
      mSeekTimeUs=seekTimeUs;
      mMinOffset = 0;
      mMaxOffset = mFileSize;
      mMaxcount=0;     
      setDequeueState(false);	  
      mSeeking=true;
      mSeekStreamID = StreamID;
    }
    mBuffer->setRange(0, 0);    //Need to Reset mBuffer Data
    mFinalResult = OK;    //Reset mFinalResult Status for Repeat Flow

    ALOGE("seekTo: moffset: %lld %lld ", mOffset, mMaxOffset);

    return;
}

void MPEG2PSExtractor::parseMaxPTS() {
	size_t index = 0;
	off64_t u8SearchCount = 0;
	int MaxTry =1;
	bool found=false;
	ALOGD("parseMaxPTS in \n");
   
	mDataSource->getSize(&mFileSize);
	
       setDequeueState(false);
	signalDiscontinuity();

	if (mFileSize <= kChunkSize)
	{
	  u8SearchCount = 1;
	}
	else{
	   u8SearchCount = mFileSize/kChunkSize;
	   MaxTry = u8SearchCount /4;
	}
    for (off64_t i = 1; i <= MaxTry; i++)
	{
	  //Set Search Start Offset
	  mOffset = (off64_t)((u8SearchCount - i) * kChunkSize);
      mOffset = SearchValidOffset(mOffset);
	  
	  if(mOffset <0){
		continue;
	  }
      mFinalResult = OK;
	  mBuffer->setRange(0, 0);
         	      
	  while (feedMore() == OK) 
	  {    
	  }

	  mDurationUs = getMaxPTS();
	  if (mDurationUs)
	  {
		found = true;
		break;
	  }
    }

	if(found != true){
	  signalDiscontinuity();
      mFinalResult = OK;
      mBuffer->setRange(0, 0);
	  //Set Search Start Offset
	  mOffset = 0;
      mOffset = SearchValidOffset(mOffset);
      mFinalResult = OK;
	  int counter;
	  while (feedMore() == OK) 
      {    
      	 if (counter>MaxTry) {
    		break;
		}
      }
      mDurationUs = getMaxPTS();
	}


	setDequeueState(true);
	mFinalResult = OK;
	mBuffer->setRange(0, 0);
	
	//Init Max PTS
	for (index=0; index<mTracks.size(); index++)
	{
	  mTracks.valueAt(index)->mMaxTimeUs = 0x0;
	}
	
	mAverageByteRate = (mDurationUs>0)?(mFileSize*1000000/mDurationUs):0;
	mParseMaxTime = true;
	ALOGD("getMaxPTS->mDurationUs:%lld, Track Number: %d, AverageByteRate %lld/s", mDurationUs, mTracks.size(), mAverageByteRate);
    ALOGD("parseMaxPTS out \n");
}

uint64_t MPEG2PSExtractor::getDurationUs() {
	return mDurationUs;
}

void MPEG2PSExtractor::init() {
    bool haveAudio = false;
    bool haveVideo = false;
    size_t index = 0;
    int numPacketsParsed = 0;
    ALOGD("init in \n");
	
    mOffset = 0;
    while (feedMore() == OK) 
    {
        if (haveAudio && haveVideo) 
	 {
            break;
        }

	 for (index=0; index<mTracks.size(); index++)
	 {
	   if (mTracks.valueAt(index)->isVideo() && 
	   	 (mTracks.valueAt(index)->getFormat() != NULL))
	   {
	     haveVideo = true;
	     
	     ALOGD("haveVideo=%d", haveVideo);
	   }
	   else
	   {
	     if (mTracks.valueAt(index)->isVideo())
	     {
	       ALOGD("have Video, But no format !! \n");
	     }
	   }
	   
	   if (mTracks.valueAt(index)->isAudio() && 
	   	 (mTracks.valueAt(index)->getFormat() != NULL))
	   {
	     haveAudio = true;
		 
	     ALOGD("haveAudio=%d", haveAudio);
	   }
	   else
	   {
	     if (mTracks.valueAt(index)->isAudio())
	     {
	       ALOGD("have Audio, But no format !! \n");
	     }
	   }

	 }

	 if (++numPacketsParsed > 500) {
            break;
        }
    }

    mFinalResult = OK;
    mBuffer->setRange(0, 0);
	if(haveAudio == 0 && haveVideo == 0){
		ALOGD("bisplable is false");
		bisPlayable = false;
	}

    ALOGI("haveAudio=%d, haveVideo=%d, numPacketsParsed %d, mOffset 0x%x", haveAudio, haveVideo, numPacketsParsed, mOffset);
    ALOGD("init out \n");
}

bool MPEG2PSExtractor::getSeeking() {
    return mSeeking;
}

void MPEG2PSExtractor::signalDiscontinuity() 
{
  mBuffer->setRange(0, 0);
  
  for (size_t i = 0; i < mTracks.size(); ++i) 
  {
    mTracks.valueAt(i)->signalDiscontinuity();
  }
}
////////// mtk 02420 refine seeking/////////////
int64_t MPEG2PSExtractor::getLastPESWithIFrame(off64_t end)
{
  unsigned stream_id_Video = 0xFF;
  int streamType = -1;
  bool mfindI = false;
  int mMaxTryCount = 0;
  off64_t PESlength = 0;
  off64_t offset = end;	//chunk start
  off64_t displace;		//PES start(count from the chunk start)
  off64_t length = 0;	//supposed to be the chunck size, unless it's not enought bits there
  off64_t lastPESoffset = -1;
  off64_t lastValidPES = -1;

  int bufsize = kChunkSize;
  char* buf = new char[bufsize];
  char* last5 = new char[5];
  int index = 0;

  //the state in this chunck helping memorize STATE mashine status
  bool hasPES = false;
  bool hasI = false;		//for H264 files, this variable stands for SPS
  bool hasH264I = false;		//for H264 files, this variable stands for I
  bool hasH264SPSnI = false;
  bool firstChunk = true;
  bool waitForTheLastPES = false;
 
  int previousPESSCState = 0;
  int previousMPEGState = 0;
  int previousMPEG4State = 0;
  int previousH264State = 0;
  
   enum {
	 NEXT_MPEG_INIT_STATE,
	 NEXT_MPEG_FOUND00_STATE,
	 NEXT_MPEG_FOUND0000_STATE,
	 NEXT_MPEG_FOUND000001_STATE,
	 NEXT_MPEG_FOUND00000100_STATE,
	 NEXT_MPEG_FOUND00000100XX_STATE,
	 NEXT_MPEG_FOUNDI_STATE,
  } MPEGNextSeekState;
  enum {
	NEXT_MPEG4_INIT_STATE,
	NEXT_MPEG4_FOUND00_STATE,
	NEXT_MPEG4_FOUND0000_STATE,
	NEXT_MPEG4_FOUND000001_STATE,
	NEXT_MPEG4_FOUND000001B6_STATE,
	NEXT_MPEG4_FOUNDI_STATE,
  } MPEG4NextSeekState;
  
  enum {
	NEXT_H264_INIT_STATE,
	NEXT_H264_FOUND00_STATE,
	NEXT_H264_FOUND0000_STATE,
	NEXT_H264_FOUND000001_STATE,
	//NEXT_H264_FOUND_SPS_STATE,
	NEXT_H264_FOUND_I_STATE,
	NEXT_H264_FOUND_SPSnI_STATE,
  } H264NextSeekState;
  
  for (index=0; index<mTracks.size(); index++)
  {
    if (mTracks.valueAt(index)->isVideo())
    {
      stream_id_Video = mTracks.valueAt(index)->mStreamID;
      streamType = mTracks.valueAt(index)->mStreamType;
    }
  }
  
  //get bits from DataSource indexed by "offset"
	while(1){
		mMaxTryCount++;	
		ALOGD("[MP2PS]TryCount is %d, offset 0x%x\n",mMaxTryCount, offset);
		if(mMaxTryCount>50){
			return -1;
		} 	

		if( offset >= 0 ){
			if ( (length = mDataSource->readAt(offset, buf, bufsize)) <= 0){
				//ALOGD("read file error length=%lld",length);
				return 0;
			}
		}

		//initialization for a new chunk
		displace = 0;			//index of the chunk bytes
		lastPESoffset = -1;
		lastValidPES = -1;
		hasPES = false;
		hasI = false;
		hasH264I = false;
		hasH264SPSnI = false;
		waitForTheLastPES = false;


		
		previousMPEGState = MPEGNextSeekState;
		previousMPEG4State = MPEG4NextSeekState;
		previousH264State = H264NextSeekState;

		MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
		MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
		H264NextSeekState = NEXT_H264_INIT_STATE;
		//ALOGD("[MP2PS] new offset from file is %lld length is%lld result is %lld",offset,length,displace);

		//check edge data; peek 5
		//see if PES start code
		if(firstChunk == true){
		  firstChunk = false;
		}
		else{
			if( ( (streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO) && previousMPEGState == NEXT_MPEG_FOUNDI_STATE )||
				( streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO && previousMPEG4State == NEXT_MPEG4_FOUNDI_STATE )||
				( streamType == ATSParser::STREAMTYPE_H264 && previousH264State == NEXT_H264_FOUND_SPSnI_STATE ) )
			{	
			  //ALOGD("check if PES on the edge is %02X %02X %02X %02X",buf[length-3],buf[length-2],buf[length-1] ,last5[0]);
			  if( buf[length-3] == 0x00 && buf[length-2] == 0x00 && buf[length-1] == 0x01 && last5[0] == stream_id_Video)
			  {
				lastPESoffset = offset + length  - 3;
				mfindI = true;
				break;
			  }
			  else if(buf[length-2] == 0x00 && buf[length-1] == 0x00 && last5[0] == 0x01 && last5[1] == stream_id_Video)
			  {
				lastPESoffset = offset + length - 2;
				mfindI = true;
				break;

			  }
			  else if(buf[length-1] == 0x00 && last5[0] == 0x00 && last5[1] == 0x01 && last5[2] == stream_id_Video)
			  {
				lastPESoffset = offset + length - 1;
				mfindI = true;
				break;
			  }
			}

			if(streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO) 	//MPEG
			{
				 if(previousMPEGState == NEXT_MPEG_FOUNDI_STATE){
					waitForTheLastPES = true;
				 }
				 else if( (buf[length-5] == 0x00 && buf[length-4]== 0x00 && buf[length-3]== 0x01 && buf[length-2] == 0x00 && ((last5[0]>>3)&0x07) == 0x01) ||
					 (buf[length-4] == 0x00 && buf[length-3] == 0x00 && buf[length-2] == 0x01 && buf[length-1] == 0x00 && ((last5[1]>>3)&0x07) == 0x01)||
					 (buf[length-3] == 0x00 && buf[length-2] == 0x00 && buf[length-1] == 0x01 && last5[0] == 0x00 && ((last5[2]>>3)&0x07) == 0x01)||
					 (buf[length-2] == 0x00 && buf[length-1] == 0x00 && last5[0] == 0x01 && last5[1] == 0x00 && ((last5[3]>>3)&0x07) == 0x01)||
					 (buf[length-1] == 0x00 && last5[0] == 0x00 && last5[1] == 0x01 && last5[2] == 0x00 && ((last5[4]>>3)&0x07) == 0x01))
				 {
					//ALOGD("mpeg I on the edge");
					previousMPEGState = NEXT_MPEG_FOUNDI_STATE;
					waitForTheLastPES = true;
				 }
			 }
			else if(streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO)		//MPEG4
			{
					if(previousMPEG4State == NEXT_MPEG4_FOUNDI_STATE){
					   waitForTheLastPES = true;
					}
					else if( (buf[length-4] == 0x00 && buf[length-3]== 0x00 && buf[length-2]== 0x01 && buf[length-1] == 0xB6 && (last5[0]&0xC0) == 0x00) ||
						(buf[length-3] == 0x00 && buf[length-2] == 0x00 && buf[length-1] == 0x01 && last5[0] == 0xB6 && (last5[1]&0xC0) == 0x00)||
						(buf[length-2] == 0x00 && buf[length-1] == 0x00 && last5[0] == 0x01 && last5[1] == 0xB6 && (last5[2]&0xC0) == 0x00)||
						(buf[length-1] == 0x00 && last5[0] == 0x00 && last5[1] == 0x01 && last5[2] == 0xB6 && (last5[3]&0xC0) == 0x00))
					{
					   //ALOGD("mpeg4 I on the edge");
					   previousMPEG4State = NEXT_MPEG4_FOUNDI_STATE;
					   waitForTheLastPES = true;
					}

			 }
			else if(streamType == ATSParser::STREAMTYPE_H264)		//H264
			{
				if(previousH264State == NEXT_H264_FOUND_SPSnI_STATE){
				   waitForTheLastPES = true;
				 }	
				else if( previousH264State == NEXT_H264_FOUND_I_STATE ){
					waitForTheLastPES = true;
					if( (buf[length-3] == 0x00 && buf[length-2] == 0x00 && buf[length-1] == 0x01 &&(last5[0]& 0x1f) == 7)||
					    (buf[length-2] == 0x00 && buf[length-1] == 0x00 && last5[0] == 0x01 &&(last5[1]& 0x1f) == 7)||
					    (buf[length-1] == 0x00 && last5[0] == 0x00 && last5[1] == 0x01 && (last5[2]& 0x1f) == 7))
					 {
						//ALOGD("h264 SPS on the edge");
						previousH264State = NEXT_H264_FOUND_SPSnI_STATE;
					 }
				}
		 		else if( (buf[length-3] == 0x00 && buf[length-2] == 0x00 && buf[length-1] == 0x01 &&(last5[0]& 0x1f) == 5)||
					 (buf[length-2] == 0x00 && buf[length-1] == 0x00 && last5[0] == 0x01 &&(last5[1]& 0x1f) == 5)||
					 (buf[length-1] == 0x00 && last5[0] == 0x00 && last5[1] == 0x01 && (last5[2]& 0x1f) == 5))
				 {
					//ALOGD("h264 SPS on the edge");
					previousH264State = NEXT_H264_FOUND_I_STATE;
					waitForTheLastPES = true;
				 } 
			 }
		}	

		//ALOGD("[MP2PS] new offset from file is %lld length is%lld result is %lld",offset,length,displace);
		//ALOGD("[MP2PS] findI frame %d from %02X %02X %02X %02X %02X %02X %02X", mfindI, buf[0], buf[1],buf[2],buf[3],buf[4],buf[5],buf[6]);

		while(displace < length){    ////////////////////////////////////////////////////////////////////////////////////////
	//ALOGD("INNER LOOP %lld",displace);
			if( streamType == ATSParser::STREAMTYPE_H264)
			{
				  if(H264NextSeekState == NEXT_H264_INIT_STATE && buf[displace]!=0x00){
					 if( displace < (length - 9) && buf[displace + 1] != 0x00 && buf[displace + 2] != 0x00 && buf[displace + 3] != 0x00 && buf[displace + 4] != 0x00 && 
						 buf[displace + 5] != 0x00 && buf[displace + 6] != 0x00 && buf[displace + 7] != 0x00 && buf[displace + 8] != 0x00 && buf[displace + 9] != 0x00) {   // speed-up; expecting picture data contains no 0x00
						displace += 10;
						continue;
					}
					displace++;
					continue;
				  }
				   else if(buf[displace]==0x00){
					   if( H264NextSeekState == NEXT_H264_INIT_STATE){
						 H264NextSeekState= NEXT_H264_FOUND00_STATE;
					   }
					   else if(H264NextSeekState == NEXT_H264_FOUND00_STATE){
						 H264NextSeekState = NEXT_H264_FOUND0000_STATE;
					   }
					   else if(H264NextSeekState == NEXT_H264_FOUND0000_STATE){
						 H264NextSeekState = NEXT_H264_FOUND0000_STATE;
					   }
					   else{
						 H264NextSeekState = NEXT_H264_INIT_STATE;
					   }

				   }
				   else if(buf[displace]==0x01 && H264NextSeekState == NEXT_H264_FOUND0000_STATE ){
						if(displace<length-1){
						   if(buf[displace+1] == stream_id_Video){
							   lastPESoffset = offset + displace -2; //+1-3=-2
							   //ALOGD("lastPESoffset is %lld",lastPESoffset);
							   hasPES = true;
							   H264NextSeekState = NEXT_H264_INIT_STATE;
							   displace += 2;
							   continue;
						   }
						   
						   else if((buf[displace+1]& 0x1f) == 0x07){//SPS
							   hasI = true;
							   H264NextSeekState = NEXT_H264_INIT_STATE;
							   if( hasPES == true){
								 //ALOGD("[L]find SPS validlastPES is %lld,displace is %lld", lastPESoffset,displace);
								 lastValidPES = lastPESoffset;
								 displace += 2;
								 continue;
								}
							   else {
								  //hasI = true;
								  lastValidPES = -1;
								  //ALOGD("find I but no PES found");
								 displace += 2;
								 continue;
							   }
						   }
						   
						   else if((buf[displace+1]& 0x1f) == 0x05){//IDR
							 hasH264I = true;
							 H264NextSeekState = NEXT_H264_INIT_STATE;
						   
							 if( hasI == true){//SPS appeared before
								 if(hasPES == true && lastValidPES != -1){
									 //ALOGD("[L]find I break inner while lastPESoffset is %lld,displace is %lld", lastPESoffset,displace);
									 mfindI = true;
									 lastPESoffset = lastValidPES;
									 if(waitForTheLastPES != true){
									   break;
									 }
								   }
								   else{
									   //ALOGD("no PES found before SPS is found Valid:ast %lld",lastValidPES);
								   }
								   hasH264SPSnI = true;
								   displace += 2;
								   continue;
							 }
							 displace += 2;
							 continue;
						   }
					   	}
					   H264NextSeekState = NEXT_H264_INIT_STATE;
					   displace += 2;
					   continue;
				   }
				   else{
					   H264NextSeekState = NEXT_H264_INIT_STATE;
				   }

			}	
			else if( streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO)
			{
				if(MPEGNextSeekState == NEXT_MPEG_INIT_STATE && buf[displace]!=0x00){
					if( displace < (length - 9) && buf[displace + 1] != 0x00 && buf[displace + 2] != 0x00 && buf[displace + 3] != 0x00 && buf[displace + 4] != 0x00 && 
						buf[displace + 5] != 0x00 && buf[displace + 6] != 0x00 && buf[displace + 7] != 0x00 && buf[displace + 8] != 0x00 && buf[displace + 9] != 0x00) {   // speed-up; expecting picture data contains no 0x00
					  displace += 10;
					  continue;
					}
					displace++;
					continue;

				}
				else if(MPEGNextSeekState == NEXT_MPEG_FOUND00000100_STATE)
				{
				   MPEGNextSeekState = NEXT_MPEG_FOUND00000100XX_STATE;
				}
				else if(buf[displace] == 0x00){
				  if(MPEGNextSeekState == NEXT_MPEG_INIT_STATE){
					MPEGNextSeekState = NEXT_MPEG_FOUND00_STATE;
				  }
				  else if(MPEGNextSeekState == NEXT_MPEG_FOUND00_STATE){
					MPEGNextSeekState = NEXT_MPEG_FOUND0000_STATE;
				  }
				  else if(MPEGNextSeekState == NEXT_MPEG_FOUND0000_STATE){
					 MPEGNextSeekState = NEXT_MPEG_FOUND0000_STATE;
				  }
				  else if(MPEGNextSeekState == NEXT_MPEG_FOUND000001_STATE){
					 MPEGNextSeekState = NEXT_MPEG_FOUND00000100_STATE;
				  }
				  else{
					 MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
				  }
				}
				else if( buf[displace] == 0x01 && MPEGNextSeekState == NEXT_MPEG_FOUND0000_STATE) {
				  MPEGNextSeekState = NEXT_MPEG_FOUND000001_STATE;
				}
				else if( buf[displace] == stream_id_Video && MPEGNextSeekState == NEXT_MPEG_FOUND000001_STATE){
					lastPESoffset = offset + displace - 3;
					//ALOGD("lastPESoffset is %lld",lastPESoffset);
					hasPES = true;
					MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
				}
				else if(((buf[displace]>>3) & 0x07) == 0x01 && MPEGNextSeekState == NEXT_MPEG_FOUND00000100XX_STATE){
				   if( hasPES == true)
				   {
					 //ALOGD("[L]find I break inner while lastPESoffset is %lld", lastPESoffset);
					 mfindI = true;
					 if(waitForTheLastPES != true)
					   break;
				   }
				   else
				   {
					 hasI = true;
					 MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
					 //ALOGD("find I but no PES found");
				   }
				}
				else
				{
				  MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
				}
			}  
			else if( streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO)
			{
				if(MPEG4NextSeekState == NEXT_MPEG4_INIT_STATE && buf[displace]!=0x00){
					if( displace < (length - 9) && buf[displace + 1] != 0x00 && buf[displace + 2] != 0x00 && buf[displace + 3] != 0x00 && buf[displace + 4] != 0x00 && 
						buf[displace + 5] != 0x00 && buf[displace + 6] != 0x00 && buf[displace + 7] != 0x00 && buf[displace + 8] != 0x00 && buf[displace + 9] != 0x00) {   // speed-up; expecting picture data contains no 0x00
					  displace += 10;
					  continue;
					}
					displace++;
					continue;
				 }
				 else if(buf[displace] == 0x00){
				   if(MPEG4NextSeekState == NEXT_MPEG4_INIT_STATE){
					 MPEG4NextSeekState = NEXT_MPEG4_FOUND00_STATE;
				   }
				   else if(MPEG4NextSeekState == NEXT_MPEG4_FOUND00_STATE){
					 MPEG4NextSeekState = NEXT_MPEG4_FOUND0000_STATE;
				   }
				   else if(MPEG4NextSeekState == NEXT_MPEG4_FOUND0000_STATE){
					 MPEG4NextSeekState = NEXT_MPEG4_FOUND0000_STATE;
				   }
				   else{
					  MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
				   }
				 }
				  else if( buf[displace] == 0x01 && MPEG4NextSeekState == NEXT_MPEG4_FOUND0000_STATE) {
						if(displace<length-1){
							  if( buf[displace + 1] == stream_id_Video){
								   lastPESoffset = offset + displace - 2;
								   //ALOGD("lastPESoffset is %lld",lastPESoffset);
								   hasPES = true;
								   MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
							  }
							  else if( buf[displace+1] == 0xB6){
								  MPEG4NextSeekState = NEXT_MPEG4_FOUND000001B6_STATE;
							   }
							  else{
								  MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
							  }
							  displace+=2;
							  continue;
						}
						else{
							MPEG4NextSeekState = NEXT_MPEG4_FOUND000001_STATE;
							break;
						}
				   }
				
				   else if( ((buf[displace] & 0xC0) == 0x00) && MPEG4NextSeekState == NEXT_MPEG4_FOUND000001B6_STATE){
					  if( hasPES == true)
					  {
						//ALOGD("[L]find I break inner while lastPESoffset is %lld", lastPESoffset);
						mfindI = true;
						break;
					  }
					  else
					  {
						  hasI = true;
						  MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
						  //ALOGD("find I but no PES found");
					  }
				  }
				  else{
					   MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
				  }
			}
			else
			{
			  //ALOGE("Unknow stream type.");
			  return -1;
			}
			//sum up the offset and return if something found
			displace ++;
		}

		if( mfindI == true ) 
		{
		  //ALOGD("[L] find I break outter while");
		  break;					//stop read from file
		}

		//sum up all previous results and up-to-date the current chunk so far
		if(streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO)
		{
		  if(previousMPEGState == NEXT_MPEG_FOUNDI_STATE){
			  if( hasPES == true )  //found PES previously in this chunk
			  {
				mfindI = true;
				//ALOGD("last chunck had I this chunk has PES");
				break;			
			  }
			  MPEGNextSeekState = NEXT_MPEG_FOUNDI_STATE;
		  }
		  else if( hasI == 1 ){
			MPEGNextSeekState = NEXT_MPEG_FOUNDI_STATE;
		  }
		  else{
			MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
		  }
		}
		else if(streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO)  //found PES previously in this chunk
		{
		  if(previousMPEG4State == NEXT_MPEG4_FOUNDI_STATE){
			  if( hasPES == true )
			  {
				mfindI = true;
				//ALOGD("last chunck had I this chunk has PES");
				break;
			  }
			  MPEG4NextSeekState = NEXT_MPEG4_FOUNDI_STATE;
		  }
		  else if( hasI == 1 ){		//has i but no pes
			MPEG4NextSeekState = NEXT_MPEG4_FOUNDI_STATE;
		  }
		  else{
			MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
		  }
		}
		else if((streamType == ATSParser::STREAMTYPE_H264))   //found PES with SPS previously in this chunk
		{
			if(previousH264State == NEXT_H264_FOUND_SPSnI_STATE)
			{

				if( hasPES == true )
				{
				  mfindI = true;
				  //ALOGD("H264 last chunck had I and SPS this chunk has PES");
				  break;
				}
				H264NextSeekState = NEXT_H264_FOUND_SPSnI_STATE;
			}
			else if(previousH264State == NEXT_H264_FOUND_I_STATE)
			{

				if( hasPES == true && hasI == true && lastValidPES != -1)
				{
				  lastPESoffset = lastValidPES;
				  mfindI = true;
				  //ALOGD("H264 last chunck had I this chunk has PES");
				  break;
				}
				H264NextSeekState = NEXT_H264_FOUND_I_STATE;
			}
			else if( hasH264SPSnI == 1 ){
			  H264NextSeekState = NEXT_H264_FOUND_SPSnI_STATE;
			}
			else if( hasH264I == 1 ){
			  H264NextSeekState = NEXT_H264_FOUND_I_STATE;
			}
		
			else{
			  H264NextSeekState = NEXT_H264_INIT_STATE;
			}

		}
		//reset
		 last5[0]=buf[0];
		 last5[1]=buf[1];
		 last5[2]=buf[2];
		 last5[3]=buf[3];
		 last5[4]=buf[4];

		offset -= length;	       //chunck start changes for the next read file start	
		if(offset < 0)
			offset = 0;
	}
	  
	  if(buf != NULL)
	  {
		delete[] buf;
		buf = NULL;
	  }
	  
	  if(mfindI == true && lastPESoffset != -1)
	  {
		//ALOGD("getLastPESWithIreturn offset %lld %lld", offset,displace);
		return lastPESoffset;
	  }
	  else
	  {
		//ALOGD("NEXT find returns -1: MPEGSTATE is %d MPEG4STATE is %dH264STATE is %d", MPEGNextSeekState, MPEG4NextSeekState, H264NextSeekState);
		return -1;
	  }
}

int64_t MPEG2PSExtractor::getNextPESWithIFrame(off64_t begin)
{
  unsigned stream_id_Video = 0xFF;
  int streamType = -1;
  bool mfindI = false;
  
  off64_t PESlength = 0;
  off64_t offset = begin;	//chunk start
  off64_t displace;		//PES start(count from the chunk start)
  off64_t length = 0;	//supposed to be the chunck size, unless it's not enought bits there
  off64_t lastPESoffset = -1;
  off64_t lastValidPES = -1;

  int bufsize = kChunkSize;
  char* buf = new char[bufsize];
  char* last5 = new char[5];
  int index = 0;
  bool hasPES = false;
  bool hasPESnSPS = false;
  int mMaxTryCount = 0;
  bool firstChunk = true;

  enum {
	  NEXT_MPEG_INIT_STATE,
	  NEXT_MPEG_FOUND00_STATE,
	  NEXT_MPEG_FOUND0000_STATE,
	  NEXT_MPEG_FOUND000001_STATE,
	  NEXT_MPEG_FOUND00000100_STATE,
	  NEXT_MPEG_FOUND00000100XX_STATE,
	  NEXT_MPEG_FOUNDI_STATE,
  } MPEGNextSeekState;
  enum {
	NEXT_MPEG4_INIT_STATE,
	NEXT_MPEG4_FOUND00_STATE,
	NEXT_MPEG4_FOUND0000_STATE,
	NEXT_MPEG4_FOUND000001_STATE,
	NEXT_MPEG4_FOUND000001B6_STATE,
	NEXT_MPEG4_FOUNDI_STATE,
  } MPEG4NextSeekState;
  
  enum {
	NEXT_H264_INIT_STATE,
	NEXT_H264_FOUND00_STATE,
	NEXT_H264_FOUND0000_STATE,
	NEXT_H264_FOUND000001_STATE,
	//NEXT_H264_FOUND_SPS_STATE,
	//NEXT_H264_FOUND_PESnSPS_STATE,
  } H264NextSeekState;

 	H264NextSeekState = NEXT_H264_INIT_STATE;
	MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
	MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE; 
	
  for (index=0; index<mTracks.size(); index++)
  {
    if (mTracks.valueAt(index)->isVideo())
    {
      stream_id_Video = mTracks.valueAt(index)->mStreamID;
      streamType = mTracks.valueAt(index)->mStreamType;
    }
  }



//get bits from DataSource indexed by "offset"
  while(1)
  {
    mMaxTryCount++;	
	//ALOGD("[MP2PS]TryCount is %d",mMaxTryCount);
	//marked for I far far away
	/*if(mMaxTryCount>=200){
		return -1;
	} */

	if( offset < mFileSize - 1 ){
	  if((length = mDataSource->readAt(offset, buf, bufsize )) <= 0 )
	    break;
	}
	else{
		break;
	}
	
	displace = 0;			//index of the chunk bytes
	
	//ALOGD("[MP2PS] new offset from file is %lld length is%lld result is %lld",offset,length,displace);
	//ALOGD("[MP2PS] findI frame %d from %02X %02X %02X %02X %02X %02X %02X", mfindI, buf[0], buf[1],buf[2],buf[3],buf[4],buf[5],buf[6]);


	if(firstChunk == true){
	  firstChunk = false;
	}
	else{
      if(hasPES==true){
          if(streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO)  //MPEG
		  {
			   if( (last5[0] == 0x00 && last5[1]== 0x00 && last5[2]== 0x01 && last5[3] == 0x00 && ((buf[0]>>3)&0x07) == 0x01) ||
				   (last5[1] == 0x00 && last5[2] == 0x00 && last5[3] == 0x01 && last5[4] == 0x00 && ((buf[1]>>3)&0x07) == 0x01)||
				   (last5[2] == 0x00 && last5[3] == 0x00 && last5[4] == 0x01 && buf[0] == 0x00 && ((buf[2]>>3)&0x07) == 0x01)||
				   (last5[3] == 0x00 && last5[4] == 0x00 && buf[0] == 0x01 && buf[1] == 0x00 && ((buf[3]>>3)&0x07) == 0x01)||
				   (last5[4] == 0x00 && buf[0] == 0x00 && buf[1] == 0x01 && buf[2] == 0x00 && ((buf[4]>>3)&0x07) == 0x01))
			   {
				  //ALOGD("mpeg I on the edge");
				  mfindI =true;
				  break;
			   }
		   }
		  else if(streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO)		  //MPEG4
		  {
			  if( (last5[1] == 0x00 && last5[2]== 0x00 && last5[3]== 0x01 && last5[4] == 0xB6 && (buf[0]&0xC0) == 0x00) ||
				  (last5[2] == 0x00 && last5[3] == 0x00 && last5[4] == 0x01 && buf[0] == 0xB6 && (buf[1]&0xC0) == 0x00)||
				  (last5[3] == 0x00 && last5[4] == 0x00 && buf[0] == 0x01 && buf[1] == 0xB6 && (buf[2]&0xC0) == 0x00)||
				  (last5[4] == 0x00 && buf[0] == 0x00 && buf[1] == 0x01 && buf[2] == 0xB6 && (buf[3]&0xC0) == 0x00))
			  {
				 //ALOGD("mpeg4 I on the edge");
				 mfindI =true;
				 break;
			  }
		   }
		  else if(streamType == ATSParser::STREAMTYPE_H264) 	  //H264
		  {
			   if( (last5[2] == 0x00 && last5[3] == 0x00 && last5[4] == 0x01 &&(buf[0]& 0x1f) == 7)||
				   (last5[3] == 0x00 && last5[4] == 0x00 && buf[0] == 0x01 &&(buf[1]& 0x1f) == 7)||
				   (last5[4] == 0x00 && buf[0] == 0x00 && buf[1] == 0x01 && (buf[2]& 0x1f) == 7))
			   {
				  //ALOGD("h264 SPS on the edge");
				  lastValidPES = lastPESoffset;
				  hasPESnSPS= true;
			   }	   
			   else if( hasPESnSPS == true&& lastValidPES != -1 &&
			   	   (last5[2] == 0x00 && last5[3] == 0x00 && last5[4] == 0x01 &&(buf[0]& 0x1f) == 5)||
				   (last5[3] == 0x00 && last5[4] == 0x00 && buf[0] == 0x01 &&(buf[1]& 0x1f) == 5)||
				   (last5[4] == 0x00 && buf[0] == 0x00 && buf[1] == 0x01 && (buf[2]& 0x1f) == 5))
			   {
				  //ALOGD("h264 I on the edge");
				  lastPESoffset = lastValidPES;
				  mfindI =true;
				  break;
			   }
		  }
      }


	  //check PES on edge
	  if( last5[2] == 0x00 && last5[3] == 0x00 && last5[4] == 0x01 && buf[0] == stream_id_Video)
	  {
		lastPESoffset = offset - 3;
		hasPES = true;
	  }
	  else if(last5[3] == 0x00 && last5[4] == 0x00 && buf[0] == 0x01 && buf[1] == stream_id_Video)
	  {
		 lastPESoffset = offset - 2;
		 hasPES = true;
	  }
	  else if(last5[4] == 0x00 && buf[0] == 0x00 && buf[1] == 0x01 && buf[2] == stream_id_Video)
	  {
		 lastPESoffset = offset - 1;
		 hasPES = true;
	  }
	}	

	//parse the chunck indexed by "result"
	while(displace < length){
		if( streamType == ATSParser::STREAMTYPE_H264)
		{
			if(H264NextSeekState == NEXT_H264_INIT_STATE && buf[displace]!=0x00){
				H264NextSeekState = NEXT_H264_INIT_STATE;
				if( (displace < (length-4) )&& buf[displace + 1] != 0x00 && buf[displace + 2] != 0x00 && buf[displace + 3] != 0x00 && buf[displace + 4] != 0x00) {   // speed-up; expecting picture data contains no 0x00
				  displace += 5;
				  continue;
				}
			}
			else if(buf[displace]==0x00){
				if( H264NextSeekState == NEXT_H264_INIT_STATE){
				  H264NextSeekState= NEXT_H264_FOUND00_STATE;
				}
				else if(H264NextSeekState == NEXT_H264_FOUND00_STATE){
				  H264NextSeekState = NEXT_H264_FOUND0000_STATE;
				}
				else if(H264NextSeekState == NEXT_H264_FOUND0000_STATE){
				  H264NextSeekState = NEXT_H264_FOUND0000_STATE;
				}
				else{
				  H264NextSeekState = NEXT_H264_INIT_STATE;
				}

			}
			else if(buf[displace]==0x01 && H264NextSeekState == NEXT_H264_FOUND0000_STATE ){
				if(displace<length-1){
					if(buf[displace+1] == stream_id_Video){
						lastPESoffset = offset + displace - 2;
						//ALOGD("lastPESoffset is %lld",lastPESoffset);
						hasPES = true;
						H264NextSeekState = NEXT_H264_INIT_STATE;
					}
					else if((buf[displace+1]& 0x1f) == 0x07){
						if( hasPES == true){
						  //ALOGD("[L]find I break inner while lastPESoffset is %lld", lastPESoffset);
						  //H264NextSeekState = NEXT_H264_FOUND_PESnSPS_STATE;
						  hasPESnSPS = true;
						  lastValidPES = lastPESoffset;
						 }
						else {
						   lastValidPES = -1;
						   H264NextSeekState = NEXT_H264_INIT_STATE;
						   //ALOGD("find I but no PES found");
						}
					}
					else if((buf[displace+1]& 0x1f) == 0x05){		//IDR
						if( hasPESnSPS == true && lastValidPES != -1){
						  //ALOGD("[L]find I break inner while lastPESoffset is %lld", lastPESoffset);
						  lastPESoffset = lastValidPES;
						  mfindI = true;
						  break;
						 }
						else {
						   H264NextSeekState = NEXT_H264_INIT_STATE;
						   //ALOGD("find I but no PES found");
						}
					}
					else{
						H264NextSeekState = NEXT_H264_INIT_STATE;
					}
					displace+=2;
					continue;
				}
				else{
				  H264NextSeekState = NEXT_H264_FOUND000001_STATE;
                  break;
				}
			}
			else
				H264NextSeekState = NEXT_H264_INIT_STATE;
		}
		else if( streamType == ATSParser::STREAMTYPE_MPEG1_VIDEO || streamType == ATSParser::STREAMTYPE_MPEG2_VIDEO)
		{
			if(MPEGNextSeekState == NEXT_MPEG_INIT_STATE && buf[displace]!=0x00){
				 MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
				 if( (displace < (length-4) )&& buf[displace + 1] != 0x00 && buf[displace + 2] != 0x00 && buf[displace + 3] != 0x00 && buf[displace + 4] != 0x00) {   // speed-up; expecting picture data contains no 0x00
				   displace += 5;
				   continue;
				 }
			}
			else if(MPEGNextSeekState == NEXT_MPEG_FOUND00000100_STATE)
			{
			   MPEGNextSeekState = NEXT_MPEG_FOUND00000100XX_STATE;
			}
			else if(buf[displace] == 0x00){
			  if(MPEGNextSeekState == NEXT_MPEG_INIT_STATE){
				MPEGNextSeekState = NEXT_MPEG_FOUND00_STATE;
			  }
			  else if(MPEGNextSeekState == NEXT_MPEG_FOUND00_STATE){
				MPEGNextSeekState = NEXT_MPEG_FOUND0000_STATE;
			  }
			  else if(MPEGNextSeekState == NEXT_MPEG_FOUND0000_STATE){
				 MPEGNextSeekState = NEXT_MPEG_FOUND0000_STATE;
			  }
			  else if(MPEGNextSeekState == NEXT_MPEG_FOUND000001_STATE){
				 MPEGNextSeekState = NEXT_MPEG_FOUND00000100_STATE;
			  }
			  else{
				 MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
			  }
			}
			else if( buf[displace] == 0x01 && MPEGNextSeekState == NEXT_MPEG_FOUND0000_STATE) {
			  MPEGNextSeekState = NEXT_MPEG_FOUND000001_STATE;
			}
			else if( buf[displace] == stream_id_Video && MPEGNextSeekState == NEXT_MPEG_FOUND000001_STATE){
				lastPESoffset = offset + displace - 3;
				//ALOGD("lastPESoffset is %lld",lastPESoffset);
				hasPES = true;
				MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
			}
			else if(((buf[displace]>>3) & 0x07) == 0x01 && MPEGNextSeekState == NEXT_MPEG_FOUND00000100XX_STATE){
			   if( hasPES == true)
			   {
				 //ALOGD("[L]find I break inner while lastPESoffset is %lld", lastPESoffset);
				 mfindI = true;
				 break;
			   }
			   else
			   {
				 MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
				 //ALOGD("find I but no PES found");
			   }
			}
			else
			{
			  MPEGNextSeekState = NEXT_MPEG_INIT_STATE;
			}
		}
		else if( streamType == ATSParser::STREAMTYPE_MPEG4_VIDEO)
		{
			if(MPEG4NextSeekState == NEXT_MPEG4_INIT_STATE && buf[displace]!=0x00){
				MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
				if( (displace < (length-4) )&& buf[displace + 1] != 0x00 && buf[displace + 2] != 0x00 && buf[displace + 3] != 0x00 && buf[displace + 4] != 0x00 ) {  // speed-up; expecting picture data contains no 0x00
				  displace += 5;
				  continue;
				}
			 }
			 else if(buf[displace] == 0x00){
			   if(MPEG4NextSeekState == NEXT_MPEG4_INIT_STATE){
				 MPEG4NextSeekState = NEXT_MPEG4_FOUND00_STATE;
			   }
			   else if(MPEG4NextSeekState == NEXT_MPEG4_FOUND00_STATE){
				 MPEG4NextSeekState = NEXT_MPEG4_FOUND0000_STATE;
			   }
			   else if(MPEG4NextSeekState == NEXT_MPEG4_FOUND0000_STATE){
				 MPEG4NextSeekState = NEXT_MPEG4_FOUND0000_STATE;
			   }
			   else{
				  MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
			   }
			 }
			 else if( buf[displace] == 0x01 && MPEG4NextSeekState == NEXT_MPEG4_FOUND0000_STATE) {
			 	if(displace<length-1){
				 	if( buf[displace + 1] == stream_id_Video){
						 lastPESoffset = offset + displace - 2;
						 //ALOGD("lastPESoffset is %lld",lastPESoffset);
						 hasPES = true;
						 MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
				 	}
					else if( buf[displace+1] == 0xB6){
						MPEG4NextSeekState = NEXT_MPEG4_FOUND000001B6_STATE;
					 }
					else{
						MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
					}
					displace+=2;
					continue;
			 	}
			 	else{
					MPEG4NextSeekState = NEXT_MPEG4_FOUND000001_STATE;
					break;
				}
			 }
			 else if( ((buf[displace] & 0xC0) == 0x00) && MPEG4NextSeekState == NEXT_MPEG4_FOUND000001B6_STATE){
				if( hasPES == true)
				{
				  //ALOGD("[L]find I break inner while lastPESoffset is %lld", lastPESoffset);
				  mfindI = true;
				  break;
				}
				else
				{
				  MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
				  //ALOGD("find I but no PES found");
				}
			 }
			 else
			 {
			   MPEG4NextSeekState = NEXT_MPEG4_INIT_STATE;
			 }
		}
		else
		{
		  ALOGE("Unknow stream type.");
		  return -1;
		}			
        displace++;
	}

	
	if( mfindI == true ) 

	{
	  //ALOGD("Nfind I break outter while");
	  break;					//stop read from file
	}

    last5[0] = buf[length - 1];
    last5[1] = buf[length - 2];
    last5[2] = buf[length - 3];
    last5[3] = buf[length - 4];
    last5[4] = buf[length - 5];

	offset += length;	       //chunck start changes for the next read file start
	
  }
  
  if(buf != NULL)
  {
	delete[] buf;
    buf = NULL;
  }
  
  if(mfindI == true)
  {
    //ALOGD("getNextPESWithIreturn offset %lld %lld", offset,displace);
	return lastPESoffset;
  }
  else
  {
	//ALOGD("NEXT find returns -1: MPEGSTATE is %d MPEG4STATE is %dH264STATE is %d", MPEGNextSeekState, MPEG4NextSeekState, H264NextSeekState);
	return -1;
  }

}

#if 0
int MPEG2PSExtractor::findH264NextIframe(uint8_t data, unsigned stream_id_Video) {
    ALOGI("findH264NextIframe in");
	if(H264NextSeekState == NEXT_H264_INIT_STATE)
	{
	  	if( data == 0x00 )
		{
		  H264NextSeekState = NEXT_H264_FOUND00_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_INIT_STATE;
		}	
	}
	else if(H264NextSeekState == NEXT_H264_FOUND00_STATE)
	{
		if( data == 0x00 )
		{
		  H264NextSeekState = NEXT_H264_FOUND0000_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_INIT_STATE;
		}
	}
	else if(H264NextSeekState == NEXT_H264_FOUND0000_STATE)
	{
		if( data == 0x01 )
		{
		  H264NextSeekState = NEXT_H264_FOUND000001_STATE;
		}
		else if( data == 0x00 )
		{
		  H264NextSeekState = NEXT_H264_FOUND0000_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_INIT_STATE;
		}
	}
	else if(H264NextSeekState == NEXT_H264_FOUND000001_STATE)
	{	
		if( data == stream_id_Video )
		{
		  PESSCNextState = NEXT_PESSC_FOUND_STATE;
		  H264NextSeekState = NEXT_H264_INIT_STATE;
		}
		else if( data& 0x1f == 7 )
		{
		  H264NextSeekState = NEXT_H264_FOUND_SPS_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_INIT_STATE;
		}
	}
	else if(H264NextSeekState == NEXT_H264_FOUND_SPS_STATE)
	{
	  	if( data == 0x00 )
		{
		  H264NextSeekState = NEXT_H264_FOUNDS00_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_FOUND_SPS_STATE;		//parse those between SPS and I
		}	
	}
	else if(H264NextSeekState == NEXT_H264_FOUNDS00_STATE)
	{
		if( data == 0x00 )
		{
		  H264NextSeekState = NEXT_H264_FOUNDS0000_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_FOUND_SPS_STATE;
		}
	}
	else if(H264NextSeekState == NEXT_H264_FOUNDS0000_STATE)
	{
		if( data == 0x01 )
		{
		  H264NextSeekState = NEXT_H264_FOUNDS000001_STATE;
		}
		else if( data == 0x00 )
		{
		  H264NextSeekState = NEXT_H264_FOUNDS0000_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_FOUND_SPS_STATE;
		}
	}
	else if(H264NextSeekState == NEXT_H264_FOUNDS000001_STATE)
	{	
		if( data == stream_id_Video )
		{
		  PESSCNextState = NEXT_PESSC_FOUND_STATE;		//but not remmeber this offset(should be the one with SPS)
		  H264NextSeekState = NEXT_H264_FOUND_SPS_STATE;
		}
		else if( data& 0x1f == 5 )
		{
		  H264NextSeekState = NEXT_H264_FOUND_SPSnI_STATE;
		}
		else
		{
		  H264NextSeekState = NEXT_H264_FOUND_SPS_STATE;
		}
	}

    return;
}
//LOCAL DIFFERENT BEHAVIOR BECAUSE OF  BUFFER CONSTRAIN!!!!
//MONICA 2012/07/20
status_t MPEG2PSExtractor::getNextNALUnit_local(uint8_t **_data, size_t *_size, const uint8_t **nalStart, size_t *nalSize, bool startCodeFollows) {
    uint8_t *data = *_data;
    size_t size = *_size;
	
    *nalStart = NULL;
    *nalSize = 0;
    bool mReachEnd = false;

    if (size == 0) {
		ALOGD("getNextNALUnit_local: size = 0");
        return -EAGAIN;
    }

    // Skip any number of leading 0x00.

    size_t offset = 0;
    while (offset < size && data[offset] == 0x00) {
        ++offset;
    }

    if (offset == size) {
		ALOGD("getNextNALUnit_local: all zero");
		return -EAGAIN;
    }

    // A valid startcode consists of at least two 0x00 bytes followed by 0x01.

    if (offset < 2 || data[offset] != 0x01) {
        return ERROR_MALFORMED;
    }

    ++offset;

    size_t startOffset = offset;

    for (;;) {
        while (offset < size && data[offset] != 0x01) {
            ++offset;
        }

        if (offset == size) {
            if (startCodeFollows) {
                offset = size + 2;
                break;
            }
			mReachEnd = true;
            break;
        }

        if (data[offset - 1] == 0x00 && data[offset - 2] == 0x00) {
            break;
        }

        ++offset;
    }

    size_t endOffset = offset - 2;
    while (endOffset > startOffset + 1 && data[endOffset - 1] == 0x00) {
        --endOffset;
    }

    *nalStart = &data[startOffset];
    *nalSize = endOffset - startOffset;

    if (offset + 2 < size) {
        *_data = &data[offset - 2];
        *_size = size - offset + 2;
    } else {
        *_data = NULL;
        *_size = 0;
    }
	
    if(mReachEnd == true)
		return -1;
	else
      return OK;
}
#endif

/////////////////////////////////////////////
int64_t MPEG2PSExtractor::SearchPES(const void* data, int size)
{
  uint8_t* p = (uint8_t*)data;
  int offset = 0;
  unsigned stream_id_Video = 0xFF;
  unsigned stream_id_Audio = 0xFF;
  size_t index = 0;

  for (index=0; index<mTracks.size(); index++)
  {
	   if (mTracks.valueAt(index)->isVideo())
	   {
            stream_id_Video = mTracks.valueAt(index)->mStreamID;
	   }
	   if (mTracks.valueAt(index)->isAudio())
	   {
            stream_id_Audio = mTracks.valueAt(index)->mStreamID;
	   }
  }
  
  while(offset < size - 4)
  {
    //find start code 
    if(p[0] == 0x00 && p[1] == 0x00 && p[2] == 0x01 && 
	 ( (mhasVTrack == true && p[3] == stream_id_Video)||(mhasATrack == true && p[3] == stream_id_Audio)))
    {
      return offset;	
    }
    else
    {
      p++;
      offset++;
    }
  }
  
  return -1;
}

int64_t MPEG2PSExtractor::SearchValidOffset(off64_t currentoffset)
{
  //Search Start Code & StreamID
  int length = 0;
  int bufsize = kChunkSize;
  char* buf = new char[bufsize];
  off64_t offset = currentoffset;
  bool found=false;
			
  //ALOGE("feedMore - Search Start = %lld \n", offset);
  if (buf == NULL)
  {
    ALOGE("Working Alloc Fail for Seek\n");
  }
  
  while((length = mDataSource->readAt(offset, buf, bufsize)) == bufsize)
  {
    int64_t result = SearchPES(buf, length);

    if (result >= 0)
    {
      offset = offset + result;
	  found = true;
      break;
    } 
    else
    {
      offset = offset + length;
    }
  }
  
  if(buf != NULL)
  {
    free(buf);
    buf = NULL;
  }
  if(found == true)
   return offset;
  else
  	return -1;
}

bool MPEG2PSExtractor::IsSeeminglyValidADTSHeader(const uint8_t *ptr, size_t size) {
    if (size < 3) {
        // Not enough data to verify header.
        return false;
    }

    if (ptr[0] != 0xff || (ptr[1] >> 4) != 0x0f) {
        return false;
    }

    unsigned layer = (ptr[1] >> 1) & 3;

    if (layer != 0) {
        return false;
    }

    unsigned ID = (ptr[1] >> 3) & 1;
    unsigned profile_ObjectType = ptr[2] >> 6;

    if (ID == 1 && profile_ObjectType == 3) {
        // MPEG-2 profile 3 is reserved.
        return false;
    }

    return true;
}

bool MPEG2PSExtractor::IsSeeminglyValidMPEGAudioHeader(const uint8_t *ptr, size_t size) {
	if (size < 3) {
        // Not enough data to verify header.
        return false;
    }

    /*if (ptr[0] != 0xff || (ptr[1] >> 5) != 0x07) {
        return false;
    }*/

    unsigned ID = (ptr[1] >> 3) & 3;

    if (ID == 1) {
        return false;  // reserved
    }
    unsigned layer = (ptr[1] >> 1) & 3;

    if (layer == 0) {
        return false;  // reserved
    }
    unsigned bitrateIndex = (ptr[2] >> 4);

    if (bitrateIndex == 0x0f) {
        return false;  // reserved
    }
    unsigned samplingRateIndex = (ptr[2] >> 2) & 3;

    if (samplingRateIndex == 3) {
        return false;  // reserved
    }
    return true;
}
#endif


////////////////////////////////////////////////////////////////////////////////

MPEG2PSExtractor::Track::Track(
        MPEG2PSExtractor *extractor, unsigned stream_id, unsigned stream_type)
    : mExtractor(extractor),
      mStreamID(stream_id),
      mStreamType(stream_type),
      #ifndef ANDROID_DEFAULT_CODE
      seeking(false),
      mMaxTimeUs(0),
      mFirstPTSValid(false),
      mSeekable(true),    //Default: Seekable. Will Change in getTrack() When Video/Audio Case (Disable Audio Seek)      
      mTimeUsPrev(0),
      #endif
      mQueue(NULL) {
    bool supported = true;
    ElementaryStreamQueue::Mode mode;

    switch (mStreamType) {
        case ATSParser::STREAMTYPE_H264:
            mode = ElementaryStreamQueue::H264;
            break;
        case ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS:
            mode = ElementaryStreamQueue::AAC;
            break;
        case ATSParser::STREAMTYPE_MPEG1_AUDIO:
        case ATSParser::STREAMTYPE_MPEG2_AUDIO:
            mode = ElementaryStreamQueue::MPEG_AUDIO;
            break;

        case ATSParser::STREAMTYPE_MPEG1_VIDEO:
        case ATSParser::STREAMTYPE_MPEG2_VIDEO:
            mode = ElementaryStreamQueue::MPEG_VIDEO;
            break;

        case ATSParser::STREAMTYPE_MPEG4_VIDEO:
            mode = ElementaryStreamQueue::MPEG4_VIDEO;
            break;
#ifndef ANDROID_DEFAULT_CODE
	    case ATSParser::STREAMTYPE_AUDIO_PSLPCM:
        	mode = ElementaryStreamQueue::PSLPCM;
        	break;
#endif
		default:
            supported = false;
            break;
    }

    if (supported) {
        mQueue = new ElementaryStreamQueue(mode);
#if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_OGM_PLAYBACK_SUPPORT)  
        if(mode == ElementaryStreamQueue::MPEG_VIDEO)
        {
        	mQueue->setSearchSCOptimize(true);
        }
#endif
    } else {
        ALOGI("unsupported stream ID 0x%02x", stream_id);
    }
}

MPEG2PSExtractor::Track::~Track() {
    delete mQueue;
    mQueue = NULL;
}

status_t MPEG2PSExtractor::Track::start(MetaData *params) {
    if (mSource == NULL) {
        return NO_INIT;
    }

    return mSource->start(params);
}

status_t MPEG2PSExtractor::Track::stop() {
    if (mSource == NULL) {
        return NO_INIT;
    }

    return mSource->stop();
}

sp<MetaData> MPEG2PSExtractor::Track::getFormat() {
    if (mSource == NULL) {
        return NULL;
    }

    return mSource->getFormat();
}

status_t MPEG2PSExtractor::Track::read(
        MediaBuffer **buffer, const ReadOptions *options) {
    if (mSource == NULL) {
        return NO_INIT;
    }

    #ifndef ANDROID_DEFAULT_CODE
    int64_t seekTimeUs;
    ReadOptions::SeekMode seekMode;
    
    if (mSeekable && options && options->getSeekTo(&seekTimeUs, &seekMode)) {     
      //ALOGD("read - seekto: %x \n", mStreamID);
      mExtractor->seekTo(seekTimeUs, mStreamID);
    }
    #endif //#ifndef ANDROID_DEFAULT_CODE

    status_t err = OK;	
    #ifndef ANDROID_DEFAULT_CODE
    int64_t timeS = getTickCountMs(), timeE = 0;
    uint8_t u1FeedCnt = 0, u1BufferCnt = 0;
    #endif
    
    status_t finalResult=OK;
    #ifndef ANDROID_DEFAULT_CODE
    while (!mSource->hasBufferAvailable(&finalResult) || mExtractor->getSeeking()) 
    #else //#ifndef ANDROID_DEFAULT_CODE
    while (!mSource->hasBufferAvailable(&finalResult)) 
    #endif //#ifndef ANDROID_DEFAULT_CODE
    {
        if (finalResult != OK) {
	     #ifndef ANDROID_DEFAULT_CODE
	     ALOGE("read:ERROR_END_OF_STREAM this=%p",this );     
	     mExtractor->setDequeueState(true);
	     mSource->clear();
	     #endif //#ifndef ANDROID_DEFAULT_CODE
		 
            return ERROR_END_OF_STREAM;
        }

    	err = mExtractor->feedMore();
        if (err != OK) {
	   #ifndef ANDROID_DEFAULT_CODE
	   ALOGE("read:signalEOS this=%p",this );
	   #endif //#ifndef ANDROID_DEFAULT_CODE
            mSource->signalEOS(err);
        }

        #ifndef ANDROID_DEFAULT_CODE
        u1FeedCnt++;
        #endif
    }

    //video buffering for bitrate variation case
    #if 0//ndef ANDROID_DEFAULT_CODE
    status_t Result;

    timeE = getTickCountMs();
    if(isVideo())
    {
        while((err==OK) && 
            ((timeE - timeS) < BUFFERING_THRESHOLD_MS)
            /*&& ((mExtractor->mAverageByteRate==0) || 
                    (mExtractor->mAverageByteRate>0 && mSource->getUsedBufSpace()<(mExtractor->mAverageByteRate<<1)))*/)
        {
            err = mExtractor->feedMore();
			
            timeE = getTickCountMs();
            u1BufferCnt++;
        }
        ALOGD("feed/buffered(%lld ms/%lld),Cnt %d/%d,Sz %d", timeE - timeS, mSource->getBufferedDurationUs(&Result), u1FeedCnt, u1BufferCnt, mSource->getFreeBufSpace());
    }
    #endif

    return mSource->read(buffer, options);
}

status_t MPEG2PSExtractor::Track::appendPESData(
        unsigned PTS_DTS_flags,
        uint64_t PTS, uint64_t DTS,
        const uint8_t *data, size_t size) {
    if (mQueue == NULL) {
        return OK;
    }

    int64_t timeUs;

//ALOGD("append track %02X %02X %02X %02X %02X %02X %02X %02X %02X",data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8]);
    if (PTS_DTS_flags == 2 || PTS_DTS_flags == 3) {
	 #ifndef ANDROID_DEFAULT_CODE
	 timeUs = convertPTSToTimestamp(PTS);
	 //ALOGD("PTS US: %lld, ID: %x \n", timeUs, mStreamID);
     mTimeUsPrev = timeUs;
	 #else //#ifndef ANDROID_DEFAULT_CODE
        timeUs = (PTS * 100) / 9;
        #endif //#ifndef ANDROID_DEFAULT_CODE

    } else {
        //ALPS00433420, decoder will not modify PTS value, use 0 or mTimeUsPrev are able to display
        #ifndef ANDROID_DEFAULT_CODE
        timeUs = mTimeUsPrev;//0xFFFFFFFF;
        #else //#ifndef ANDROID_DEFAULT_CODE
        timeUs = 0;
	 #endif //#ifndef ANDROID_DEFAULT_CODE
    }

    #ifndef ANDROID_DEFAULT_CODE
    if ((timeUs > mMaxTimeUs) && (timeUs != 0xFFFFFFFF) && (timeUs != 0))
    {
      mMaxTimeUs = timeUs;
    }

    if (!mExtractor->getDequeueState()) 
    {
      return OK;
    }
    #endif
 //ALOGD("apeend track %02X %02X %02X %02X %02X %02X %02X %02X %02X",data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8]);   
    status_t err = mQueue->appendData(data, size, timeUs, (PTS_DTS_flags == 2 || PTS_DTS_flags == 3)?false:true);

    if (err != OK) {
        return err;
    }
//ALOGD("end apeend queue data");

    sp<ABuffer> accessUnit;
    while ((accessUnit = mQueue->dequeueAccessUnit()) != NULL) {
        if (mSource == NULL) {
            sp<MetaData> meta = mQueue->getFormat();
            if (meta != NULL) {
//ALOGD("Stream ID 0x%02x now has data.", mStreamID);
                mSource = new AnotherPacketSource(meta);
                mSource->queueAccessUnit(accessUnit);
            }
        } else if (mQueue->getFormat() != NULL) {
            mSource->queueAccessUnit(accessUnit);
        }
    }
    return OK;
}

#ifndef ANDROID_DEFAULT_CODE
int64_t MPEG2PSExtractor::Track::getPTS() {
  return mMaxTimeUs;
}

bool MPEG2PSExtractor::Track::isVideo(){
    switch (mStreamType) {
        case ATSParser::STREAMTYPE_H264:
        case ATSParser::STREAMTYPE_MPEG1_VIDEO:
        case ATSParser::STREAMTYPE_MPEG2_VIDEO:
        case ATSParser::STREAMTYPE_MPEG4_VIDEO:
            return true;

        default:
            return false;
    }
}

bool MPEG2PSExtractor::Track::isAudio(){
    switch (mStreamType) {
        case ATSParser::STREAMTYPE_MPEG1_AUDIO:
        case ATSParser::STREAMTYPE_MPEG2_AUDIO:
        case ATSParser::STREAMTYPE_MPEG2_AUDIO_ADTS:
        case ATSParser::STREAMTYPE_AUDIO_PSLPCM:
            return true;

        default:
            return false;
    }
}

int64_t MPEG2PSExtractor::Track::convertPTSToTimestamp(uint64_t PTS) {
  if (!mFirstPTSValid) 
  {
    mFirstPTSValid = true;
    mFirstPTS = PTS;
    PTS = 0;
  } 
  else if (PTS < mFirstPTS) 
  {
    PTS = 0;
  } 
  else 
  {
    PTS -= mFirstPTS;
  }
  
  return (PTS * 100) / 9;
}

void MPEG2PSExtractor::Track::signalDiscontinuity() 
{
    mTimeUsPrev = 0;

    if (mQueue == NULL) {
        return;
    }
    
    if (!mExtractor->getDequeueState()) 
    {
      mMaxTimeUs = 0;
      return;
    }
	
    mQueue->clear(false);
	
    mQueue->setSeeking();
    if(mSource.get())
    {
      mSource->clear();
    }
    else
    {
      ALOGE("[error]this stream has no source\n");
    }

    return;
}
#endif

////////////////////////////////////////////////////////////////////////////////

MPEG2PSExtractor::WrappedTrack::WrappedTrack(
        const sp<MPEG2PSExtractor> &extractor, const sp<Track> &track)
    : mExtractor(extractor),
      mTrack(track) {
}

MPEG2PSExtractor::WrappedTrack::~WrappedTrack() {
}

status_t MPEG2PSExtractor::WrappedTrack::start(MetaData *params) {
    return mTrack->start(params);
}

status_t MPEG2PSExtractor::WrappedTrack::stop() {
    return mTrack->stop();
}

sp<MetaData> MPEG2PSExtractor::WrappedTrack::getFormat() {

    #ifndef ANDROID_DEFAULT_CODE
    sp<MetaData> meta = mTrack->getFormat();

    meta->setInt64(kKeyDuration, mExtractor->getDurationUs());   //Need to Enable Seek Feature
    meta->setInt64(kKeyThumbnailTime,0);                                   //Need to Enable Seek Feature

    return meta;
    #else //#ifndef ANDROID_DEFAULT_CODE
    return mTrack->getFormat();
    #endif //#ifndef ANDROID_DEFAULT_CODE
}

status_t MPEG2PSExtractor::WrappedTrack::read(
        MediaBuffer **buffer, const ReadOptions *options) {
    return mTrack->read(buffer, options);
}
////////////////////////////////////////////////////////////////////////////////

#ifndef ANDROID_DEFAULT_CODE
#define  PACK_START_CODE              0X000001BA 
#define  SYSTEM_START_CODE          0X000001BB 

int  parsePackHeader(ABitReader * br)
{
	int currentByte = -1;
	int length = -1;
	enum {
		INIT_STATE,
		FIND00_STATE,
		FIND0000_STATE,
		FIND000001_STATE,
		FIND000001BA_STATE,
		
	} currentState;
	currentState = INIT_STATE;
   //search for pack header start code 
   while (1) {	  
	  length = br->numBitsLeft()/8;
	  //ALOGD("*********************parsePackHeader in :length = %d*********************",length);
	  if(length < 9)
	  {
		  ALOGD("data too less");
		  return 0;
	  }

	  currentByte = br->getBits(8);

	  switch(currentState)
	  {
		case INIT_STATE:
		{
			if(length < 12) 			//can't be less than 12 byte( including startcode)
			{
			  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
			  return 0;
			}
			if(currentByte == 0x00)
			{
				currentState = FIND00_STATE; 
				if(length < 11) 		  //can't be less than 12 byte( including startcode)
				{
				  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
				  return 0;
				}
			}
			else
			{
				currentState = INIT_STATE;	//reset
			}
			break;
		}
		case FIND00_STATE:
		{
			if(length < 11) 			//can't be less than 12 byte( including startcode)
			{
			  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
			  return 0;
			}

			if(currentByte == 0x00)
			{
				currentState = FIND0000_STATE; 
				if(length < 10) 			//can't be less than 12 byte( including startcode)
				{
				  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
				  return 0;
				}
			}
			else
			{
				currentState = INIT_STATE;		//reset
			}
			break;
		}
		case FIND0000_STATE:
		{
			if(length < 10) 			//can't be less than 12 byte( including startcode)
			{
			  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
			  return 0;
			}

			if(currentByte == 0x01)
			{
				currentState = FIND000001_STATE; 
				if(length < 9)					  //can't be less than 12 byte( including startcode)
				{
				  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
				  return 0;
				}
			}
			else if(currentByte == 0x00)
			{
				currentState = FIND0000_STATE;	
				if(length < 10) 			//can't be less than 12 byte( including startcode)
				{
				  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
				  return 0;
				}
			}
			else
			{
				currentState = INIT_STATE;	//reset
			}
			break;
		}
		case FIND000001_STATE:
		{
			if(length < 9) 			//can't be less than 12 byte( including startcode)
			{
			  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
			  return 0;
			}

			if(currentByte == 0xBA)
			{
				currentState = FIND000001BA_STATE; 
				if(length < 10) 			//can't be less than 12 byte( including startcode)
				{
				  ALOGD("********************* parsePackHeader can't find pack header start code *********************");
				  return 0;
				}
			}
			else
			{
				currentState = INIT_STATE;	//reset
			}
			break;
		}
		default:
		{
			ALOGD("Unknow Pack header parsing state.");
			return 0;
		}
	  }
	  if(currentState == FIND000001BA_STATE){
		ALOGD("PACK HEADER FOUND");
		break;
	  }
	}


	/*int length = br->numBitsLeft()/8;
	//ALOGD("*********************parsePackHeader in :length = %d*********************",length);
	if (length < 12)
	{
		//ALOGD("*********************parsePackHeader ERROR:the length less then 12 byte*********************");
		return 0;
	}

	if (br->getBits(32) != PACK_START_CODE)
	{
		//ALOGD("*********************parsePackHeader ERROR:the start code isn't 0x000001ba *********************");
		return 0;
	}*/
	uint64_t SRC = 0;
	int muxrate = 0;
	if (br->getBits(2) == 1u)//mpeg2
	{
		if (length < 14)
			return 0;
        SRC = ((uint64_t)br->getBits(3)) << 30;

		if(br->getBits(1) != 1u){
			return 0;
		}

		SRC |= ((uint64_t)br->getBits(15)) << 15;

		if(br->getBits(1) != 1u){
			return 0;
		}

		SRC |= br->getBits(15);

		if(br->getBits(1) != 1u){
			return 0;
		}

		int SRC_Ext = br->getBits(9);

		if(br->getBits(1) != 1u){
			return 0;
		}

		muxrate = br->getBits(22);
		br->skipBits(7);
		size_t pack_stuffing_length = br->getBits(3);

		if (pack_stuffing_length <= 7)
		{
			if (br->numBitsLeft() < pack_stuffing_length * 8)
				return 0;
			br->skipBits(pack_stuffing_length * 8);
		}
		
	}
	else//mpeg1
	{
		br->skipBits(2);
        SRC = ((uint64_t)br->getBits(3)) << 30;
        if(br->getBits(1)!= 1u){
			return 0;
		}
        SRC |= ((uint64_t)br->getBits(15)) << 15;
        if(br->getBits(1) != 1u){
			return 0;
		}
        SRC |= br->getBits(15);

		if(br->getBits(1) != 1u){
			return 0;
		}

		if(br->getBits(1) != 1u){
			return 0;
		}

		muxrate = br->getBits(22);
		
        if(br->getBits(1) != 1u){
			return 0;
		}
	}
	int offset = length - br->numBitsLeft()/8;
	//ALOGD("*********************parsePackHeader out:offset = %d*********************",offset);
	return offset;
}


int  parseSystemHeader(ABitReader * br)
{
		int currentByte = -1;

		int length = br->numBitsLeft()/8;
		if(length < 6)
		{
			ALOGD("data too less");
			return 0;
		}

		enum {
			INIT_STATE,
			FIND00_STATE,
			FIND0000_STATE,
			FIND000001_STATE,
			FIND000001BB_STATE,
			
		} currentState;
		currentState = INIT_STATE;
		
	   //search for pack header start code 
	   while (1) {	  
		  currentByte = br->getBits(8);	
		  length--;
		  switch(currentState)
		  {
			case INIT_STATE:
			{
				if(length < 5)
				{
					ALOGD("data too less");
					return 0;
				}

				if(currentByte == 0x00)
				{
					currentState = FIND00_STATE; 
				}
				else
				{
					currentState = INIT_STATE;	//reset
				}
				break;
			}
			case FIND00_STATE:
			{
				if(length < 4)
				{
					ALOGD("data too less");
					return 0;
				}

				if(currentByte == 0x00)
				{
					currentState = FIND0000_STATE; 
				}
				else
				{
					currentState = INIT_STATE;		//reset
				}
				break;
			}
			case FIND0000_STATE:
			{
				if(length < 3)
				{
					ALOGD("data too less");
					return 0;
				}

				if(currentByte == 0x01)
				{
					currentState = FIND000001_STATE; 
				}
				else if(currentByte == 0x00)
				{
					currentState = FIND0000_STATE;	
				}
				else
				{
					currentState = INIT_STATE;	//reset
				}
				break;
			}
			case FIND000001_STATE:
			{
				if(length < 2)
				{
					ALOGD("data too less");
					return 0;
				}

				if(currentByte == 0xBB)
				{
					currentState = FIND000001BB_STATE; 
				}
				else
				{
					currentState = INIT_STATE;	//reset
				}
				break;
			}
			default:
			{
				ALOGD("Unknow Pack header parsing state.");
				return 0;
			}
		  }
		  if(currentState == FIND000001BB_STATE){
			ALOGD("SYSTEM HEADER FOUND");
			break;
		  }
		}

/*	int length = br->numBitsLeft()/8;
	if(length < 6)
	{
		ALOGD("data too less");
		return 0;
	}
	//ALOGD("*********************parseSystemHeader in :length = %d*********************",length);
	if((br->getBits(32))!=SYSTEM_START_CODE)
      {
		ALOGE("SYSTEM_START_CODE check error");
		return 0;
	}
*/
	size_t header_lenth = br->getBits(16);
	if(header_lenth > br->numBitsLeft()/8)
	{
		ALOGD("data too less");
		return 0;
	}
	//ALOGD("-------------------header_lenth = %d--------------------",header_lenth);
    if(br->getBits(1) != 1u){
      return 0;
	}

	int rate_bound = br->getBits(22);

	if(br->getBits(1) != 1u){
	  return 0;
	}
	int audio_bound = br->getBits(6);
	unsigned fixed_flag = br->getBits(1);
	unsigned CSPS_flag = br->getBits(1);
	unsigned system_audio_lock_flag = br->getBits(1);
	unsigned system_video_lock_flag = br->getBits(1);

	if(br->getBits(1) != 1u){
		return 0;
	}

	unsigned video_bound = br->getBits(5);
	unsigned packet_rate_restriction_flag = br->getBits(1);
	br->skipBits(7);//skip reserved_bits
	int leftsize = header_lenth - 6;
	//ALOGD("-------------------leftsize = %d--------------------",leftsize);
	while(leftsize >= 3)
	{
		unsigned stream_id = br->getBits(8);
		//ALOGD("------------------------stream_id = %d-----------------",stream_id);
		if(stream_id >= 0xBC || stream_id == 0xB8 || stream_id == 0xB9)
		{
			br->skipBits(16);
			leftsize -= 3;
			continue;
		}
    	if(br->getBits(2) != 3u){
			return 0;
		}
		unsigned P_STD_buffer_bound_scale = br->getBits(1);
		int P_STD_buffer_size_bound = br->getBits(13);
		//save stream info

		leftsize -= 3;
	}
	//ALOGD("-------------------------mESSources.size = %d----------------",mESSources.size());
	//ALOGD("*********************parseSystemHeader out:%d*********************",length - br->numBitsLeft()/8);
	//return length - br->numBitsLeft()/8;
	return 1;

}
#endif




bool SniffMPEG2PS(
        const sp<DataSource> &source, String8 *mimeType, float *confidence,
        sp<AMessage> *) {

#ifndef ANDROID_DEFAULT_CODE

    ALOGD("*******************SniffMPEGPS in *************");
	 
	uint8_t readbuff[PS_SNIFF_LENGTH];
	memset(readbuff,0,PS_SNIFF_LENGTH);
	int length = source->readAt(0,readbuff,PS_SNIFF_LENGTH);
	//ALOGD("*********************source->readAt length = %d*********************",length);
	ALOGD("*********************readbuff:%02x %02x %02x %02x %02x ;%02x %02x %02x %02x %02x *********************",*(readbuff+0),*(readbuff+1),*(readbuff+2),*(readbuff+3),*(readbuff+4),*(readbuff+5),*(readbuff+6),*(readbuff+7),*(readbuff+8),*(readbuff+9));
	if(length < 0)
	{
		ALOGV("*********************source->readAt ERROR:length = %d*********************",length);
		return false;
	}
	ABitReader br(readbuff,(size_t)length);
	if (!parsePackHeader(&br))
	{
		ALOGV("*********************parsePackHeader ERROR*********************");
		return false;
	}
	if(!parseSystemHeader(&br))
	{
		ALOGV("*********************parseSystemHeader ERROR*********************");
		return false;
	}
	
	/*MPEG2PSExtractor* a = NULL;
	a = new MPEG2PSExtractor(source);
	if(a->bisPlayable==false){
		ALOGD("correct syntax but no track to play");
		return false;
	}*/

    *confidence = 0.5f;
    mimeType->setTo(MEDIA_MIMETYPE_CONTAINER_MPEG2PS);

#else
        
    uint8_t header[5];
    if (source->readAt(0, header, sizeof(header)) < (ssize_t)sizeof(header)) {
         ALOGE("SniffMPEG2PS: error1");
        return false;
    }

    if (memcmp("\x00\x00\x01\xba", header, 4) || (header[4] >> 6) != 1) {
        ALOGE("SniffMPEG2PS: error2");
        return false;
    }

    *confidence = 0.25f;  // Slightly larger than .mp3 extractor's confidence

    mimeType->setTo(MEDIA_MIMETYPE_CONTAINER_MPEG2PS);
    ALOGE("SniffMPEG2PS: is MPEG2PS");
#endif
    return true;
}

}  // namespace android

