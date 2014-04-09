//#define LOG_NDEBUG 0
#define LOG_TAG "Test/PlayListSource"

#include "utils/Log.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "PlayListSource.h"




using namespace android;

PlayListSource::PlayListSource(List<String8> &filelist) {
    mFd = -1;
    mFileList = filelist;
    mLeftInFile = 0;
}

PlayListSource::~PlayListSource() {
    if (mFd >= 0) {
        close(mFd);
        mFd = -1;
    }
}

void PlayListSource::add2List(const char* szFileName) {
    mFileList.push_back(String8(szFileName));
}

void PlayListSource::setListener(const sp<IStreamListener> &listener) {
    mListener = listener;
}

void PlayListSource::setBuffers(const Vector<sp<IMemory> > &buffer) {
    mBuffers = buffer;
}

off64_t getFileSize(int fd) {
    off64_t size = 0;
    off64_t curPos = lseek64(fd, 0, SEEK_CUR);
    size = lseek64(fd, 0, SEEK_END);
    curPos = lseek64(fd, curPos, SEEK_SET);
    return size;
}

bool PlayListSource::selectFile() {
//    ALOGD("@debug: %d, mFd = %d", (int)mFileList.empty(), mFd);
    while ((mFd < 0) && (!mFileList.empty())) {
        const char* filename = mFileList.begin()->string();
        ALOGD("@debug: %s", filename);
        if ((mFd = open(filename, O_LARGEFILE | O_RDONLY)) < 0) {
           ALOGE("failed to open file %s", filename); 
        } 
        mLeftInFile = getFileSize(mFd);
        ALOGD("open file %d(%s) size = %lld", mFd, filename, mLeftInFile);
        mFileList.erase(mFileList.begin());
    }
    return mFd >= 0;
}

//copy from ATSParser
    enum DiscontinuityType {
        DISCONTINUITY_NONE              = 0,
        DISCONTINUITY_TIME              = 1,
        DISCONTINUITY_AUDIO_FORMAT      = 2,
        DISCONTINUITY_VIDEO_FORMAT      = 4,

        DISCONTINUITY_SEEK              = DISCONTINUITY_TIME,

        // For legacy reasons this also implies a time discontinuity.
        DISCONTINUITY_FORMATCHANGE      =
            DISCONTINUITY_AUDIO_FORMAT
                | DISCONTINUITY_VIDEO_FORMAT
                | DISCONTINUITY_TIME,
    };

void PlayListSource::onBufferAvailable(size_t index) {
    CHECK_LT(index, mBuffers.size());
   if (!selectFile()) {
        mListener->issueCommand(IStreamListener::EOS, false);
        ALOGD("no more file in playlist");
        return;
    }

    sp<IMemory> mem = mBuffers.itemAt(index);
    bool eos = mLeftInFile < mem->size();
    size_t readbytes = eos ? mLeftInFile : mem->size();

//    ALOGD("@debug: (eos = %d) to readbytes = %d", (int)eos, readbytes);

    ssize_t n = ::read(mFd, mem->pointer(), readbytes);
    mLeftInFile -= n;
    ALOGD("@debug: read %d, curPos = %lld,  left = %lld", n, lseek64(mFd, 0, SEEK_CUR), mLeftInFile);

    if (eos) {
        ALOGD("read EOS, mark discontinuity");
        sp<AMessage> msg = new AMessage;
        msg->setInt32(IStreamListener::kKeyDiscontinuityMask, (int)DISCONTINUITY_FORMATCHANGE);

//        mListener->issueCommand(IStreamListener::DISCONTINUITY, false);
        mListener->issueCommand(IStreamListener::DISCONTINUITY, false, msg);
        CHECK_GE(mFd, 0);
        close(mFd);
        mFd = -1;
    } else {
        mListener->queueBuffer(index, n);
    }
}
