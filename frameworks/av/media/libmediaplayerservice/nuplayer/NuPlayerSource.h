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

#ifndef NUPLAYER_SOURCE_H_

#define NUPLAYER_SOURCE_H_

#include "NuPlayer.h"

namespace android {

struct ABuffer;

struct NuPlayer::Source : public RefBase {
    enum Flags {
        FLAG_SEEKABLE           = 1,
        FLAG_DYNAMIC_DURATION   = 2,
    };

#ifndef ANDROID_DEFAULT_CODE
    enum {
        kWhatConnDone		= 'cdon',
        kWhatBufferNotify   = 'buff',
        kWhatSeekDone       = 'sdon',
        kWhatPauseDone		= 'psdn',
        kWhatPlayDone		= 'pldn',
    };
#endif
    Source() {}

    virtual void start() = 0;
    virtual void stop() {}

    // Returns OK iff more data was available,
    // an error or ERROR_END_OF_STREAM if not.
    virtual status_t feedMoreTSData() = 0;

    virtual sp<AMessage> getFormat(bool audio);

    virtual status_t dequeueAccessUnit(
            bool audio, sp<ABuffer> *accessUnit) = 0;

    virtual status_t getDuration(int64_t *durationUs) {
        return INVALID_OPERATION;
    }

    virtual status_t seekTo(int64_t seekTimeUs) {
        return INVALID_OPERATION;
    }

    virtual uint32_t flags() const = 0;
    virtual bool isSeekable() {
        return false;
    }
#ifndef ANDROID_DEFAULT_CODE
    //if Source::start is implemented asynchronously, return -EWOULDBLOCK
    //asyn start must send kWhatConnDone to listener
    virtual status_t start_withCheck() {start(); return OK;};

    virtual status_t play() {return OK;};
    virtual status_t pause() {return OK;};
    virtual void setNotificationMessage(const sp<AMessage> &msg) {mNotify = msg;}

    virtual status_t initCheck() const {return OK;}
    virtual void setParams(const sp<MetaData> &meta) {};
    virtual status_t getBufferedDuration(bool audio, int64_t *durationUs) {return INVALID_OPERATION;};
    virtual sp<MetaData> getMetaData() {return mMetaData;};
#endif

protected:
#ifndef ANDROID_DEFAULT_CODE
    sp<AMessage> mNotify;
    sp<MetaData> mMetaData;
#endif
    virtual ~Source() {}

    virtual sp<MetaData> getFormatMeta(bool audio) { return NULL; }

private:
    DISALLOW_EVIL_CONSTRUCTORS(Source);
};

}  // namespace android

#endif  // NUPLAYER_SOURCE_H_

