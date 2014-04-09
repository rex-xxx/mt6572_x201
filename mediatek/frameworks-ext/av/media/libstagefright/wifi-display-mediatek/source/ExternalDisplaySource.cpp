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
#define LOG_TAG "ExternalDisplaySource"

#include <media/stagefright/foundation/ADebug.h>

#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaDefs.h>
#include <OMX_IVCommon.h>
#include <MetadataBufferType.h>

#include <ui/GraphicBuffer.h>
#include <gui/IGraphicBufferAlloc.h>
#include <OMX_Component.h>

#include <utils/Log.h>
#include <utils/String8.h>

#include "ExternalDisplaySource.h"
#include <cutils/properties.h>

#define DUMP_YUV_FILE (1)
#define USE_BUFFERPOINTER (1)


namespace android {

ExternaldisplaySource::ExternaldisplaySource(uint32_t bufferWidth, uint32_t bufferHeight, uint32_t framerate) :
    mWidth(bufferWidth),
    mHeight(bufferHeight),
    mNumPendingBuffers(0),
    mCurrentTimestamp(0),
    mFrameRate(framerate),
    mStopped(true),
    mNumFramesReceived(0),
    mNumFramesEncoded(0),
    mFirstFrameTimestamp(0),
    mWorkaroundlatebeforencfirsttime(0),
    mFirstFrame_sec(0),
    mFirstFrame_nsec(0),
    mDriverPrevtimestamp_s(-1),
    mDriverPrevtimestamp_ns(-1),
    mDriverCurrenttimestamp_s(-1),
    mDriverCurrenttimestamp_ns(-1),
    fd(NULL),
    mDumpFile(NULL),
    mCountFrames(0),
    mCountFramerate(0),
    mStartSysTime(0),
    mseq(0),
    mFrameSize(0),
    mMaxAcquiredBufferCount(SURFACEBUFFERCOUNT)  // XXX double-check the default
{
    ALOGI("ExternaldisplaySource");

    if (bufferWidth == 0 || bufferHeight == 0) {
        ALOGE("Invalid dimensions %dx%d", bufferWidth, bufferHeight);
    }




    
    minfo.buffer_num = SURFACEBUFFERCOUNT;
    minfo.width = mWidth;
    minfo.height = mHeight;
    minfo.bpp = 3;
    //workaround to YUV420
    mFrameSize = minfo.width * minfo.height * minfo.bpp / 2;
    fd = open("/dev/ext_display", O_RDONLY);
    if(fd < 0) 
    {
         ALOGE("open externaldisplay device fail");
    }
	
    if(ioctl(fd, MTK_EXT_DISPLAY_ENTER) < 0)
    {
        ALOGE("MTK_EXT_DISPLAY_ENTER enter fail");
    }

    if(ioctl(fd, MTK_EXT_DISPLAY_SET_MEMORY_INFO, &minfo) < 0) 
    {
        ALOGE("MTK_EXT_DISPLAY_ENTER set meminfo fail");
    }
     // 2. get buffer address using mmap
     p_ext_buffer = mmap(0, minfo.buffer_num *minfo.width*minfo.height*minfo.bpp, PROT_READ, MAP_SHARED, fd, 0);
     
     if((int32_t)p_ext_buffer == (-1)) 
     {
          ALOGE("mmap fail");
     }
	 

#if DUMP_YUV_FILE
    mDumpFile=NULL;
    char value[PROPERTY_VALUE_MAX];
    if (property_get("media.stagefright_wfd.logsource", value, NULL) 
	    && (!strcmp(value, "1") || !strcasecmp(value, "true"))) {
           ALOGI("open logrgb");
          mDumpFile = fopen("/sdcard/externaldisplay.bin", "wb");
    	}
#endif
}

ExternaldisplaySource::~ExternaldisplaySource() {
    ALOGV("~ExternaldisplaySource");
    CHECK(mStopped == true);

    munmap(p_ext_buffer, minfo.buffer_num *minfo.width*minfo.height*minfo.bpp);
#if 0
    // 5. stop capture buffer
    if(ioctl(fd, MTK_EXT_DISPLAY_STOP) < 0) 
    {
        ALOGI("desconstructor  stop fail");
    }
#endif	
    if(ioctl(fd, MTK_EXT_DISPLAY_LEAVE) < 0) 
    {
        ALOGI("desconstructor  leave fail");
    }

    close(fd);


#if DUMP_YUV_FILE
    if (mDumpFile != NULL) {
        fclose(mDumpFile);
        mDumpFile = NULL;
    }
#endif

}

nsecs_t ExternaldisplaySource::getTimestamp() {
    ALOGV("getTimestamp");
    Mutex::Autolock lock(mMutex);
    return mCurrentTimestamp;
}


void ExternaldisplaySource::dump(String8& result) const
{
    char buffer[1024];
    dump(result, "", buffer, 1024);
}

void ExternaldisplaySource::dump(String8& result, const char* prefix,
        char* buffer, size_t SIZE) const
{

}

status_t ExternaldisplaySource::setFrameRate(int32_t fps)
{
    ALOGV("setFrameRate");
    Mutex::Autolock lock(mMutex);
    const int MAX_FRAME_RATE = 60;
    if (fps < 0 || fps > MAX_FRAME_RATE) {
        return BAD_VALUE;
    }
    mFrameRate = fps;
    return OK;
}


int32_t ExternaldisplaySource::getFrameRate( ) const {
    ALOGV("getFrameRate");
    Mutex::Autolock lock(mMutex);
    return mFrameRate;
}

status_t ExternaldisplaySource::start(MetaData *params)
{
    ALOGI("start");

    Mutex::Autolock lock(mMutex);

    mStartTimeNs = 0;
    int64_t startTimeUs;
    if (params && params->findInt64(kKeyTime, &startTimeUs)) {
        mStartTimeNs = startTimeUs;
	 ALOGI("mStartTimeNs = %lldus", mStartTimeNs);
    }

    mStartSysTime = GetNowUs();
    mNumPendingBuffers = 0;

    	// 3. notify kernel to begin capture buffer
    if(ioctl(fd, MTK_EXT_DISPLAY_START) < 0) 
    {
         ALOGE("start ext display device fail");
    }

    mStopped = false;
    return OK;
}

status_t ExternaldisplaySource::setMaxAcquiredBufferCount(size_t count) {
    ALOGI("setMaxAcquiredBufferCount(%d)", count);
    Mutex::Autolock lock(mMutex);

    CHECK_GT(count, 1);
    mMaxAcquiredBufferCount = count;
    if (mMaxAcquiredBufferCount > SURFACEBUFFERCOUNT)
    {
          mMaxAcquiredBufferCount = SURFACEBUFFERCOUNT;
        ALOGI("mMaxAcquiredBufferCount > SURFACEBUFFERCOUNT=8");
    }


    return OK;
}


status_t ExternaldisplaySource::stop()
{
    ALOGI("stop");
#if 1
    // 5. stop capture buffer
    if(ioctl(fd, MTK_EXT_DISPLAY_STOP) < 0) 
    {
        ALOGI("desconstructor  stop fail");
    }
#endif	


    Mutex::Autolock lock(mMutex);

    mStopped = true;
    mMediaBuffersAvailableCondition.signal();

    ALOGI("stop done");
    

    return OK;
}

sp<MetaData> ExternaldisplaySource::getFormat()
{
    ALOGV("getFormat");

    Mutex::Autolock lock(mMutex);
    sp<MetaData> meta = new MetaData;

    meta->setInt32(kKeyWidth, mWidth);
    meta->setInt32(kKeyHeight, mHeight);
    // The encoder format is set as an opaque colorformat
    // The encoder will later find out the actual colorformat
    // from the GL Frames itself.
    meta->setInt32(kKeyColorFormat, OMX_COLOR_FormatYUV420Planar);//OMX_COLOR_FormatYUV420Planar
    meta->setInt32(kKeyStride, mWidth);
    meta->setInt32(kKeySliceHeight, mHeight);
    meta->setInt32(kKeyFrameRate, mFrameRate);
    meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_RAW);
#if USE_BUFFERPOINTER
    meta->setInt32('usebufferpointer',  true);
#endif	
    return meta;
}


int64_t ExternaldisplaySource::GetNowUs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);

    return (int64_t)tv.tv_sec * 1000000ll + tv.tv_usec;
}


status_t ExternaldisplaySource::read( MediaBuffer **buffer,
                                    const ReadOptions *options)
{
    static int workaournd = 1;
    //ALOGV("read, mNumPendingBuffers = %d, mMaxAcquiredBufferCount = %d", mNumPendingBuffers, mMaxAcquiredBufferCount);
    Mutex::Autolock lock(mMutex);

    int64_t timestamp;

    *buffer = NULL;

    void* framebuffer = NULL;

 #if 1// workaround buffer issue
    while ((!mStopped && mNumPendingBuffers == mMaxAcquiredBufferCount) || (1 == mWorkaroundlatebeforencfirsttime)) {
	 ALOGI("warnning, read buffer full then wait condition");
        mMediaBuffersAvailableCondition.wait(mMutex);
    }
#endif

    if (mStopped) {
        ALOGI("Read: SurfaceMediaSource is stopped. Returning ERROR_END_OF_STREAM.");
        return ERROR_END_OF_STREAM;
    }



    //according to framerate , control when to get buffer.
    //int64_t bufferTimeUs = mFirstFrameTimestamp + (mNumFramesReceived * 1000000ll) / mFrameRate;
    static int64_t prebufferTimeUs = 0;
    int64_t nowUs,readStartUs,readEndUs,deltaUs;
#if 0
    int64_t delayUs;
    if(prebufferTimeUs == 0)
    {
         prebufferTimeUs = GetNowUs();
    }else{
        nowUs = GetNowUs();
        delayUs =  (1000000ll / mFrameRate) - (nowUs - prebufferTimeUs);
        if (delayUs > 0ll) {
    	 ALOGI("control framrate , sleep %lld us", delayUs);
            usleep(delayUs);
        }   
	 prebufferTimeUs = GetNowUs();
    }
#endif

   //count framerate.
   nowUs = GetNowUs();
   if(((mCountFrames % 30) == 0) && (mCountFrames != 0))
   {
        char value[PROPERTY_VALUE_MAX];
        mCountFramerate = (mCountFrames * 1000 * 1000) / (nowUs - mStartSysTime);
	 ALOGI("framerate = %d ", mCountFramerate);
	 //reset frame and time
        if ( (property_get("media.extrenal_device.updateStartT", value, NULL) 
		      && (!strcmp(value, "1") || !strcasecmp(value, "true"))) )
        {
              ALOGI("don't update mStartSysTime to read framerate purely");
	 }else{
       	 mCountFrames = 0;
       	 mStartSysTime = GetNowUs();
	 }
   }


GETBUFFER_LOOP:
    {
	  ALOGI("read ioctl");
	  readStartUs =  GetNowUs();
//        status_t err = mBufferQueue->acquireBuffer(&item);
        int32_t err =ioctl(fd, MTK_EXT_DISPLAY_GET_BUFFER, &cur_buf) ;
        if(err < 0)
        {
            ALOGE("get buffer failed");
	     return ERROR_END_OF_STREAM;
	 }
	 if (0 == workaournd)
	 {
	     ALOGI("workaround first buffer ,get buffer again");
            workaournd = 1;
	     goto GETBUFFER_LOOP;
	 }
		
         //workaround for xc's bug
	 if((cur_buf.ts_nsec == 0) && (cur_buf.ts_sec == 0))
	 {
            usleep(5 * 1000);
	     ALOGI("skip first buffer to workaround xc's bug goto loop again");
            goto GETBUFFER_LOOP;
	 }
		
	 mDriverCurrenttimestamp_ns = cur_buf.ts_nsec;
	 mDriverCurrenttimestamp_s   = cur_buf.ts_sec;
	 
	 if ((mDriverPrevtimestamp_ns != mDriverCurrenttimestamp_ns)
	 	    || (mDriverPrevtimestamp_s != mDriverCurrenttimestamp_s))
	{   
	     framebuffer = (void *)((uint32_t)p_ext_buffer + cur_buf.id*minfo.width*minfo.height*minfo.bpp);
		 
	     if (mNumFramesReceived == 0)
	     {
	         mWorkaroundlatebeforencfirsttime = 0;
                //mFirstFrameTimestamp = cur_buf.ts_sec * 1000000 + cur_buf.ts_nsec;//us
                mFirstFrame_sec = cur_buf.ts_sec;
		  mFirstFrame_nsec = cur_buf.ts_nsec;
		  if (mStartTimeNs != 0)
		  {
		      mCurrentTimestamp = GetNowUs() - mStartTimeNs;
		      mFirstFrameTimestamp = mCurrentTimestamp;
		  }else{
		      mCurrentTimestamp = ((int64_t)(cur_buf.ts_sec) - mFirstFrame_sec) * 1000000 + (int64_t)(cur_buf.ts_nsec) - mFirstFrame_nsec;
		      mFirstFrameTimestamp = 0;
		  }
	     }else{
	         mCurrentTimestamp = ((int64_t)(cur_buf.ts_sec) - mFirstFrame_sec) * 1000000 + (int64_t)(cur_buf.ts_nsec) - mFirstFrame_nsec + mFirstFrameTimestamp;
	     }
	     //ALOGI("read framecount = %d, frameTimems = %lld, cur_buf.s = %x, cur_buf.ns = %x, mFirstFrameTimestampms = %lld, delta = %lldms", mNumFramesReceived, mCurrentTimestamp / 1000, cur_buf.ts_sec, cur_buf.ts_nsec, mFirstFrameTimestamp / 1000,
		//                                                                                                                                        (GetNowUs() - (int64_t)cur_buf.ts_sec * 1000000ll - cur_buf.ts_nsec) / 1000);	

	     deltaUs =(GetNowUs() - (int64_t)cur_buf.ts_sec * 1000000ll - cur_buf.ts_nsec) / 1000;
		 
	     mNumFramesReceived++;
	     mDriverPrevtimestamp_ns = mDriverCurrenttimestamp_ns;
	     mDriverPrevtimestamp_s   = mDriverCurrenttimestamp_s;

        }else{
            if (mStopped)
            {
                 return ERROR_END_OF_STREAM;
	     }
            usleep(5 * 1000);
	     ALOGI("not get new buffer goto loop again");
            goto GETBUFFER_LOOP;
        }
    }

    // If the loop was exited as a result of stopping the recording,
    // it is OK
    if (mStopped) {
        ALOGV("Read: externaldisplaysource is stopped. Returning ERROR_END_OF_STREAM.");
        return ERROR_END_OF_STREAM;
    }
	
#if DUMP_YUV_FILE
	if (mDumpFile != NULL) {
		fwrite(framebuffer, 1, mFrameSize, mDumpFile);
	}
#endif

#if USE_BUFFERPOINTER   
    *buffer = new MediaBuffer(sizeof(framebuffer) + sizeof(mFrameSize));
    char *tmpdata = (char *)(*buffer)->data();
    if (tmpdata == NULL) {
        ALOGE("Cannot allocate memory for metadata buffer!");
        return -1;
    }
    memcpy(tmpdata, &framebuffer, sizeof(framebuffer));
    memcpy((char *)((int32_t)tmpdata + sizeof(framebuffer)), &mFrameSize, sizeof(mFrameSize));
#else
    *buffer = new MediaBuffer(framebuffer, mFrameSize);
    //ALOGI("framebuffer = %x, mFrameSize = %d", framebuffer, mFrameSize);
#endif
     


    (*buffer)->setObserver(this);
    (*buffer)->add_ref();
    (*buffer)->meta_data()->setInt64(kKeyTime, mCurrentTimestamp);

    //protect 
    {
	 Mutex::Autolock lock(mProtectMutex);
        ++mNumPendingBuffers;
    }
    ++mCountFrames;
    mseq = (mseq + 1) &(SURFACEBUFFERCOUNT - 1);

    readEndUs =  GetNowUs();
    ALOGI("[video buffer]read done:MediaBuffer = %p, ,mNumFramesReceived=%d,mCurrentTimestamp=%lld ms,readTimeMs=%lld,deltaUs=%lld", 
		        *buffer,mNumFramesReceived,mCurrentTimestamp/1000,(readEndUs-readStartUs)/1000,deltaUs);
    return OK;
}

void ExternaldisplaySource::signalBufferReturned(MediaBuffer *buffer) {
    ALOGI("[video buffer]signalBufferReturned: mediaBuffer=%p, prev mNumPendingBuffers = %d", buffer,mNumPendingBuffers);
    buffer->setObserver(0);
    buffer->release();

       mWorkaroundlatebeforencfirsttime = 0;
      {
          Mutex::Autolock lock(mProtectMutex);
          --mNumPendingBuffers;
      }
       mMediaBuffersAvailableCondition.broadcast();
}

} // end of namespace android

