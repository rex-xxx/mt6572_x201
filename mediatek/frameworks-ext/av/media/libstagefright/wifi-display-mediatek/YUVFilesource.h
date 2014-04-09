
#include <utils/threads.h>
#include <utils/Vector.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaBuffer.h>

#include <stdio.h>

namespace android {
// ----------------------------------------------------------------------------

#define YUVBUFFERCOUNT (16)

class YuvFileSource : public MediaSource,
                                         public MediaBufferObserver{

public:

    YuvFileSource();

    virtual ~YuvFileSource();

    // For the MediaSource interface for use by StageFrightRecorder:
    virtual status_t start(MetaData *params = NULL);

    virtual status_t stop();
    virtual status_t read(MediaBuffer **buffer,
            const ReadOptions *options = NULL);
    virtual sp<MetaData> getFormat();

    virtual void signalBufferReturned(MediaBuffer *buffer);


    // To be called before start()
    status_t setMaxAcquiredBufferCount(size_t count);



private:


    int mWidth;
    int mHeight;
    int mFrameSize;
    int mseq;    //¼ÇÂ¼bufferÊ¹ÓÃ¡£
    int mframecount;

    int mFd;

    void *mYUVBuffer[YUVBUFFERCOUNT];
    bool mStopped;


    // mMutex is the mutex used to prevent concurrent access to the member
    // variables of SurfaceMediaSource objects. It must be locked whenever the
    // member variables are accessed.
    mutable Mutex mMutex;

    size_t mNumPendingBuffers;



    // mStartTimeNs is the start time passed into the source at start, used to
    // offset timestamps.
    int64_t mStartTimeNs;

    size_t mMaxAcquiredBufferCount;

    // mFrameAvailableCondition condition used to indicate whether there
    // is a frame available for dequeuing

    Condition mMediaBuffersAvailableCondition;

};

// ----------------------------------------------------------------------------
}; // namespace android


