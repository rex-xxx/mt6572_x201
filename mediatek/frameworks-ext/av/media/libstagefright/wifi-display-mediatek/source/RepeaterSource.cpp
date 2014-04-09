//#define LOG_NDEBUG 0
#define LOG_TAG "RepeaterSource"
#include <utils/Log.h>

#include "RepeaterSource.h"

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MetaData.h>

#ifndef ANDROID_DEFAULT_CODE
#ifdef USE_MMPROFILE
#include <linux/mmprofile.h>
MMP_Event MMP_WFD_REPEATER;
#endif
#endif

namespace android {

RepeaterSource::RepeaterSource(const sp<MediaSource> &source, double rateHz)
    : mStarted(false),
      mSource(source),
      mRateHz(rateHz),
      mBuffer(NULL),
      mResult(OK),
      mLastBufferUpdateUs(-1ll),
      mStartTimeUs(-1ll),
      mFrameCount(0) {
#ifdef USE_MMPROFILE
 	
    MMP_Event  MMP_WFD_DEBUG = MMProfileFindEvent(MMP_RootEvent, "WFD_Source");
    if(MMP_WFD_DEBUG !=0){
	    MMP_WFD_REPEATER = MMProfileRegisterEvent(MMP_WFD_DEBUG, "RepeaterSource");
	    MMProfileEnableEvent(MMP_WFD_REPEATER,1); 	
	    
    }else{

		ALOGE("can not find the WFD_Source Event");
	}
    
#endif      
}

RepeaterSource::~RepeaterSource() {
    CHECK(!mStarted);
}

status_t RepeaterSource::start(MetaData *params) {
    CHECK(!mStarted);

    status_t err = mSource->start(params);

    if (err != OK) {
        return err;
    }

    mBuffer = NULL;
    mResult = OK;
    mStartTimeUs = -1ll;
    mFrameCount = 0;

    mLooper = new ALooper;
    mLooper->setName("repeater_looper");
    mLooper->start();

    mReflector = new AHandlerReflector<RepeaterSource>(this);
    mLooper->registerHandler(mReflector);

    postRead();

    mStarted = true;
#ifdef USE_MMPROFILE	
	if(MMP_WFD_REPEATER !=0){
		MMProfileLogMetaString(MMP_WFD_REPEATER, MMProfileFlagPulse, "RepeaterSource Start");
	}
#endif
    return OK;
}

status_t RepeaterSource::stop() {
    CHECK(mStarted);

    ALOGI("stopping");

    if (mLooper != NULL) {
        mLooper->stop();
        mLooper.clear();

        mReflector.clear();
    }

    if (mBuffer != NULL) {
        ALOGV("releasing mbuf %p", mBuffer);
        mBuffer->release();
        mBuffer = NULL;
    }

    status_t err = mSource->stop();

    ALOGI("stopped");

    mStarted = false;

    return err;
}

sp<MetaData> RepeaterSource::getFormat() {
    return mSource->getFormat();
}

status_t RepeaterSource::read(
        MediaBuffer **buffer, const ReadOptions *options) {
    int64_t seekTimeUs;
    ReadOptions::SeekMode seekMode;
    CHECK(options == NULL || !options->getSeekTo(&seekTimeUs, &seekMode));

 //   ALOGI("[video buffer]read+ mBuffer=0x%08x",(mBuffer==NULL)?0:mBuffer);
    for (;;) {
        int64_t bufferTimeUs = -1ll;

        if (mStartTimeUs < 0ll) {
            Mutex::Autolock autoLock(mLock);
            while ((mLastBufferUpdateUs < 0ll || mBuffer == NULL)
                    && mResult == OK) {
                mCondition.wait(mLock);
            }

            
            mStartTimeUs = ALooper::GetNowUs();
            bufferTimeUs = mStartTimeUs;
	     ALOGI("now resuming.mStartTimeUs=%lld ms",mStartTimeUs/1000);
        } else {
            bufferTimeUs = mStartTimeUs + (mFrameCount * 1000000ll) / mRateHz;

            int64_t nowUs = ALooper::GetNowUs();
            int64_t delayUs = bufferTimeUs - nowUs;

            if (delayUs > 0ll) {
                usleep(delayUs);
            }
        }

        bool stale = false;

        {
	   
            Mutex::Autolock autoLock(mLock);
            if (mResult != OK) {
                CHECK(mBuffer == NULL);
		  ALOGI("read return error %d",mResult);
                return mResult;
            }

#if SUSPEND_VIDEO_IF_IDLE
            int64_t nowUs = ALooper::GetNowUs();
            if (nowUs - mLastBufferUpdateUs > 1000000ll) {
                mLastBufferUpdateUs = -1ll;
                stale = true;
		  ALOGI("[video buffer] has not  been updated than >1S");
            } else
#endif
	     {
                mBuffer->add_ref();
                *buffer = mBuffer;
                (*buffer)->meta_data()->setInt64(kKeyTime, bufferTimeUs);
#ifndef ANDROID_DEFAULT_CODE				
	          (*buffer)->meta_data()->setInt32('used', 1);
#endif
		  ALOGV("read one video buffer  framecount = %d, bufferTimeUs = %lld ms", mFrameCount, bufferTimeUs / 1000);
                ++mFrameCount;
		  //workaround for encoder init slow
		  if(mFrameCount == 1)
		  {
		      mFrameCount = 6;
                    ALOGI("read deley 5frames times");
		  }
	         // ALOGI("[video buffer] mBuffer=%p, add ref ,refcount =%d",mBuffer,mBuffer->refcount());

		 
            }
        }

        if (!stale) {
            break;
        }

        mStartTimeUs = -1ll;
        mFrameCount = 0;
        ALOGI("now dormant");
    }

    return OK;
}

void RepeaterSource::postRead() {
    (new AMessage(kWhatRead, mReflector->id()))->post();
}

void RepeaterSource::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatRead:
        {
            MediaBuffer *buffer;
#ifdef USE_MMPROFILE	
	if(MMP_WFD_REPEATER !=0){
		MMProfileLogMetaStringEx(MMP_WFD_REPEATER, MMProfileFlagStart, (uint32_t)buffer, (uint32_t)(mLastBufferUpdateUs / 1000),"read a buffer");
	}
#endif	     
            status_t err = mSource->read(&buffer);

            ALOGI("[video buffer]read MediaBuffer %p", buffer);

            Mutex::Autolock autoLock(mLock);//update mBuffer lock
            if (mBuffer != NULL) {
	           int32_t used=0;
#ifndef ANDROID_DEFAULT_CODE	
		  if(mBuffer->meta_data()->findInt32('used', &used) && used==0){

		 #ifdef USE_MMPROFILE	
			if(MMP_WFD_REPEATER !=0){
				MMProfileLogMetaStringEx(MMP_WFD_REPEATER, MMProfileFlagPulse,0,0,"drop frame");
			}
		   #endif	
			 ALOGW("[video buffer] mBuffer=%p is not used before release,used=%d",mBuffer,used);
		    }
#endif			
				
		    mBuffer->release();
		

                mBuffer = NULL;
            }
            mBuffer = buffer;
            mResult = err;
            mLastBufferUpdateUs = ALooper::GetNowUs();
			
#ifdef USE_MMPROFILE	
	if(MMP_WFD_REPEATER !=0){
		MMProfileLogMetaStringEx(MMP_WFD_REPEATER, MMProfileFlagEnd, (uint32_t)buffer, (uint32_t)(mLastBufferUpdateUs / 1000),"read a buffer");
	}
#endif

            mCondition.broadcast();

            if (err == OK) {
                postRead();
            }
            break;
        }

        default:
            TRESPASS();
    }
}

void RepeaterSource::wakeUp() {
    ALOGV("wakeUp");
    Mutex::Autolock autoLock(mLock);
    if (mLastBufferUpdateUs < 0ll && mBuffer != NULL) {
        mLastBufferUpdateUs = ALooper::GetNowUs();
        mCondition.broadcast();
    }
}

}  // namespace android
