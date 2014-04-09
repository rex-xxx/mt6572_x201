

#ifndef EXTERNAL_DISPLAY_SERVICE_SOURCE_H
#define EXTERNAL_DISPLAY_SERVICE_SOURCE_H


#include <utils/threads.h>
#include <utils/Vector.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaBuffer.h>



#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <linux/fb.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <cutils/log.h>
#include <cutils/atomic.h>


extern "C" {
#include "mtkfb.h"
#include "hdmitx.h"
}

namespace android {
// ----------------------------------------------------------------------------

class String8;



#define SURFACEBUFFERCOUNT (8)

class ExternaldisplaySource : public MediaSource,
                                public MediaBufferObserver{
public:
    enum { MIN_UNDEQUEUED_BUFFERS = 4};
    enum{MAX_NUM_BUFFER_SLOTS = 4};

    ExternaldisplaySource(uint32_t bufferWidth, uint32_t bufferHeight, uint32_t framerate);

    virtual ~ExternaldisplaySource();

    // For the MediaSource interface for use by StageFrightRecorder:
    virtual status_t start(MetaData *params = NULL);

    virtual status_t stop();
    virtual status_t read(MediaBuffer **buffer,
            const ReadOptions *options = NULL);
    virtual sp<MetaData> getFormat();

    // To be called before start()
    status_t setMaxAcquiredBufferCount(size_t count);

    // Get / Set the frame rate used for encoding. Default fps = 30
    status_t setFrameRate(int32_t fps) ;
    int32_t getFrameRate( ) const;

    // The call for the StageFrightRecorder to tell us that
    // it is done using the MediaBuffer data so that its state
    // can be set to FREE for dequeuing
    virtual void signalBufferReturned(MediaBuffer* buffer);
    // end of MediaSource interface

    // getTimestamp retrieves the timestamp associated with the image
    // set by the most recent call to read()
    //
    // The timestamp is in nanoseconds, and is monotonically increasing. Its
    // other semantics (zero point, etc) are source-dependent and should be
    // documented by the source.
    int64_t getTimestamp();


    // dump our state in a String
    void dump(String8& result) const;
    void dump(String8& result, const char* prefix, char* buffer,
                                                    size_t SIZE) const;

    // isMetaDataStoredInVideoBuffers tells the encoder whether we will
    // pass metadata through the buffers. Currently, it is force set to true

protected:

    static bool isExternalFormat(uint32_t format);

private:



    int64_t GetNowUs();
		
    int fd;
    struct ext_memory_info minfo;
    struct ext_buffer cur_buf;
    
    void * p_ext_buffer;


    // The permenent width and height of SMS buffers
    int mWidth;
    int mHeight;
    int mFrameSize;
    int mseq;    //¼ÇÂ¼bufferÊ¹ÓÃ¡£
    int mCountFrames;
    int mCountFramerate;
    int64_t mStartSysTime;//ys

    int32_t mDriverPrevtimestamp_s;
    int32_t mDriverPrevtimestamp_ns;
    int32_t mDriverCurrenttimestamp_s;
    int32_t mDriverCurrenttimestamp_ns;
    FILE *mDumpFile;

    size_t mNumPendingBuffers;

    // mCurrentTimestamp is the timestamp for the current texture. It
    // gets set to mLastQueuedTimestamp each time updateTexImage is called.
    int64_t mCurrentTimestamp;

    // mMutex is the mutex used to prevent concurrent access to the member
    // variables of SurfaceMediaSource objects. It must be locked whenever the
    // member variables are accessed.
    mutable Mutex mMutex;

    int mWorkaroundlatebeforencfirsttime;

    
     mutable Mutex mProtectMutex;
    ////////////////////////// For MediaSource
    // Set to a default of 30 fps if not specified by the client side
    int32_t mFrameRate;

    // mStopped is a flag to check if the recording is going on
    bool mStopped;

    // mNumFramesReceived indicates the number of frames recieved from
    // the client side
    int mNumFramesReceived;
    // mNumFramesEncoded indicates the number of frames passed on to the
    // encoder
    int mNumFramesEncoded;

    // mFirstFrameTimestamp is the timestamp of the first received frame.
    // It is used to offset the output timestamps so recording starts at time 0.
    int64_t mFirstFrameTimestamp;
    int64_t mFirstFrame_sec;
    int64_t mFirstFrame_nsec;
    // mStartTimeNs is the start time passed into the source at start, used to
    // offset timestamps.
    int64_t mStartTimeNs;

    size_t mMaxAcquiredBufferCount;
    // mFrameAvailableCondition condition used to indicate whether there
    // is a frame available for dequeuing
    Condition mFrameAvailableCondition;

    Condition mMediaBuffersAvailableCondition;


};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SURFACEMEDIASOURCE_H

