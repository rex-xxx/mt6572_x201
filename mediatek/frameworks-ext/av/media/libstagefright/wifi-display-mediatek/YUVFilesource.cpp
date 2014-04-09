
//#define LOG_NDEBUG 0
#define LOG_TAG "YUVFileSource"

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaDefs.h>
#include <OMX_IVCommon.h>
//#include <MetadataBufferType.h>


#include <OMX_Component.h>

#include <utils/Log.h>
#include <utils/String8.h>


#include"YUVFilesource.h"

#include <media/stagefright/foundation/ADebug.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define USE_BUFFERPOINTER (0)
namespace android {

YuvFileSource::YuvFileSource() :
     mFd(-1),
    mNumPendingBuffers(0),
    mStopped(false),
    mMaxAcquiredBufferCount(YUVBUFFERCOUNT)  // XXX double-check the default
{
    ALOGI("YUVFileSource");

    mWidth = 1280; 
    mHeight = 720; 
    mframecount = 0;


    mFrameSize = mWidth * mHeight * 3 / 2;
	
    mFd = open("/sdcard/yuv.bin", O_LARGEFILE | O_RDONLY);

    if (mFd >= 0) {
        //mLength = lseek64(mFd, 0, SEEK_END);
    } else {
        ALOGE("Failed to open file. (%s)", strerror(errno));
    }

    for(int i = 0; i < YUVBUFFERCOUNT; i++) 
    {
         mYUVBuffer[i] = NULL;
         mYUVBuffer[i] = (void *)malloc(mWidth * mHeight * 3 / 2);
	  ALOGI("YuvFileSource construct buff ptr = %x", mYUVBuffer[i]);
    }

    mseq = 0;
}

YuvFileSource::~YuvFileSource() {

    if (mFd >= 0) {
        close(mFd);
        mFd = -1;
    }

    for(int i = 0; i < YUVBUFFERCOUNT; i++)
    {
       free(mYUVBuffer[i]);
	mYUVBuffer[i] = NULL;
    }
}


status_t YuvFileSource::start(MetaData *params)
{
    ALOGI("start");
    return OK;
}



status_t YuvFileSource::stop()
{
    ALOGV("stop");
    Mutex::Autolock lock(mMutex);

    mStopped = true;
#ifndef ANDROID_DEFAULT_CODE
    mMediaBuffersAvailableCondition.broadcast();
#endif
    return OK;
}

sp<MetaData> YuvFileSource::getFormat()
{
    ALOGV("getFormat");

    Mutex::Autolock lock(mMutex);
    sp<MetaData> meta = new MetaData;

    meta->setInt32(kKeyWidth, mWidth);
    meta->setInt32(kKeyHeight, mHeight);

    // from the GL Frames itself.
    meta->setInt32(kKeyColorFormat, OMX_COLOR_FormatYUV420Planar);
    meta->setInt32(kKeyStride, mWidth);
    meta->setInt32(kKeySliceHeight, mHeight);
    meta->setInt32(kKeyFrameRate, 30);
    meta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_RAW);
#if  USE_BUFFERPOINTER
    meta->setInt32('usebufferpointer',  true);
#endif
    return meta;
}


status_t YuvFileSource::setMaxAcquiredBufferCount(size_t count) {
    ALOGI("setMaxAcquiredBufferCount(%d)", count);
    Mutex::Autolock lock(mMutex);

    CHECK_GT(count, 1);
    mMaxAcquiredBufferCount = count;

    if (mMaxAcquiredBufferCount > YUVBUFFERCOUNT)
    {
         mMaxAcquiredBufferCount = YUVBUFFERCOUNT;
    }

    return OK;
}


status_t YuvFileSource::read( MediaBuffer **buffer,
                                    const ReadOptions *options)
{

    void * data;
    int64_t frameTimeus;
    ALOGI("read");

    Mutex::Autolock lock(mMutex);

    *buffer = NULL;
	
    while (!mStopped && mNumPendingBuffers == mMaxAcquiredBufferCount) {
        mMediaBuffersAvailableCondition.wait(mMutex);
    }

    if (mStopped) {
        ALOGV("Read: SurfaceMediaSource is stopped. Returning ERROR_END_OF_STREAM.");
        return ERROR_END_OF_STREAM;
    }

    data = mYUVBuffer[mseq];

    usleep(20*1000);
    frameTimeus = mframecount * 33 * 1000;

    if(0 == ::read(mFd, data, mFrameSize))
    {
       //again.
       lseek64(mFd, 0, SEEK_SET);
	::read(mFd, data, mFrameSize);
    }

    
    
    mframecount += 1;
    mseq = (mseq + 1) & (YUVBUFFERCOUNT - 1);
    
    
#if USE_BUFFERPOINTER   
    *buffer = new MediaBuffer(sizeof(data) + sizeof(mFrameSize));
    ALOGI("read framecount = %d, frameTimeUs = %lld, mediabuffer = %x", mframecount, frameTimeus / 1000, *buffer);
    char *tmpdata = (char *)(*buffer)->data();
    if (tmpdata == NULL) {
        ALOGE("Cannot allocate memory for metadata buffer!");
        return -1;
    }
    memcpy(tmpdata, &data, sizeof(data));
    memcpy((char *)((int32_t)tmpdata + sizeof(data)), &mFrameSize, sizeof(mFrameSize));
#else
    *buffer = new MediaBuffer(data, mFrameSize);
#endif
    (*buffer)->setObserver(this);
    (*buffer)->add_ref();
    (*buffer)->meta_data()->setInt64(kKeyTime, frameTimeus);
ALOGI("read framecount = %d, frameTimeUs = %lld ", mframecount, frameTimeus / 1000);

    ++mNumPendingBuffers;

    return OK;

}



void YuvFileSource::signalBufferReturned(MediaBuffer *buffer) {
    ALOGI("signalBufferReturned, framecount = %d, buf_refcnt = %d, mediabuffer = %x", mframecount, buffer->refcount(), buffer);
     Mutex::Autolock lock(mMutex);

    buffer->setObserver(0);
    buffer->release();


    --mNumPendingBuffers;
    mMediaBuffersAvailableCondition.broadcast();


}


} // end of namespace android

