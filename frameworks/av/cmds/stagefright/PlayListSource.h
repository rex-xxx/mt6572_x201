#ifndef PLAY_LIST_SOURCE_H_
#define PLAY_LIST_SOURCE_H_

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <utils/String8.h>
#include <utils/List.h>
#include <binder/IMemory.h>
#include <media/IStreamSource.h>

namespace android {
struct PlayListSource : public BnStreamSource {
    PlayListSource(List<String8> &filelist);
public:
    //virtual method for IStreamSource
    virtual void setListener(const sp<IStreamListener> &listener);
    virtual void setBuffers(const Vector<sp<IMemory> > &buffers);

    virtual void onBufferAvailable(size_t index);

protected:
    virtual ~PlayListSource();
    //my method
    void add2List(const char* szFileName);

private:
    bool selectFile();
    List<String8> mFileList;
    int mFd;
    off64_t mLeftInFile;
   

    sp<IStreamListener> mListener;
    Vector<sp<IMemory> > mBuffers;

//    DISALLOW_EVIL_CONSTRUCTORS(PlayListSource);

};

} //namespace android

#endif // PLAY_LIST_SOURCE_H_
