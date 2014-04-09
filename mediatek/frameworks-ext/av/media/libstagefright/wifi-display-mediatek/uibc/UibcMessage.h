
#ifndef UIBC_MESSAGE_H
#define UIBC_MESSAGE_H

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ABase.h>
#include <utils/RefBase.h>
#include <utils/Thread.h>

#include "WifiDisplayUibcType.h"
#include <linux/input.h>


namespace android {

struct IRemoteDisplayClient;

struct UibcMessage : public RefBase {
    UibcMessage(const sp<IRemoteDisplayClient> &client);
    status_t init();

    status_t handleUIBCMessage(const sp<ABuffer> &buffer);

    status_t destroy();

protected:
    virtual ~UibcMessage();

private:
    int mKeyFd;
    int mTpdFd;

    sp<IRemoteDisplayClient> mClient;
    
    status_t handleGenericInput(const sp<ABuffer> &buffer);
    status_t handleHIDCInput(const sp<ABuffer> &buffer);

    short mapKeyCode(UINT16 uibcCode);

    status_t sendEvent(int fd, struct input_event* event);
    status_t sendKeyEvent(UINT16 code, int isDown);
    status_t sendTouchEvent(UINT16 x, UINT16 y, UINT16 type);
    
    DISALLOW_EVIL_CONSTRUCTORS(UibcMessage);

};

}

#endif
