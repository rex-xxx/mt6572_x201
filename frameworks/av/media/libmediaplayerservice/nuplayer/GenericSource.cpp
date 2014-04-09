/*
 * Copyright (C) 2012 The Android Open Source Project
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
#define LOG_TAG "NuPlayer/GenericSource"
#include <utils/Log.h>

#include "GenericSource.h"

#include "AnotherPacketSource.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaSource.h>

#ifndef ANDROID_DEFAULT_CODE
#include <ASessionDescription.h>
#endif

namespace android {

NuPlayer::GenericSource::GenericSource(
        const char *url,
        const KeyedVector<String8, String8> *headers,
        bool uidValid,
        uid_t uid)
    : mDurationUs(0ll),
      mAudioIsVorbis(false) {
    DataSource::RegisterDefaultSniffers();
	
#ifndef ANDROID_DEFAULT_CODE
	mInitCheck = OK;
#endif

    sp<DataSource> dataSource =
        DataSource::CreateFromURI(url, headers);

#ifndef ANDROID_DEFAULT_CODE
	if(dataSource == NULL){
		mInitCheck  = ERROR_CANNOT_CONNECT;
		ALOGW("DataSource::CreateFromURI fail!!!");
		return;
	}
#else
	CHECK(dataSource != NULL);
#endif

    initFromDataSource(dataSource);
}

NuPlayer::GenericSource::GenericSource(
        int fd, int64_t offset, int64_t length)
    : mDurationUs(0ll),
      mAudioIsVorbis(false) {
    DataSource::RegisterDefaultSniffers();
#ifndef ANDROID_DEFAULT_CODE
	mInitCheck = OK;
#endif
    sp<DataSource> dataSource = new FileSource(dup(fd), offset, length);

    initFromDataSource(dataSource);
}

void NuPlayer::GenericSource::initFromDataSource(
        const sp<DataSource> &dataSource) {
    sp<MediaExtractor> extractor = MediaExtractor::Create(dataSource);
	
#ifndef ANDROID_DEFAULT_CODE
    if (extractor == NULL) {
	ALOGE("initFromDataSource,can't create extractor!");
	mUri.clear();
	mInitCheck = ERROR_UNSUPPORTED;
	return;
    }
    sp<MetaData> mMetaData = extractor->getMetaData();
    void *sdp = NULL;
    sp<ASessionDescription> pSessionDesc;
    if (extractor->getMetaData().get()!= NULL && extractor->getMetaData()->findPointer(kKeySDP, &sdp)) {
        pSessionDesc = (ASessionDescription*)sdp;
	ALOGI("initFromDataSource,is application/sdp");
        if (!pSessionDesc->isValid()){
	    ALOGE("initFromDataSource,sdp file is not valid!");
	    pSessionDesc.clear();
	    mInitCheck = ERROR_UNSUPPORTED;
            return;  //notify not supported sdp
        }
        if (pSessionDesc->countTracks() == 1u){
	    ALOGE("initFromDataSource,sdp file contain only root description");
	    pSessionDesc.clear();
	    mInitCheck = ERROR_UNSUPPORTED;
	    return;
        }
        status_t err = pSessionDesc->getSessionUrl(mUri);
        if (err != OK){
	    ALOGE("initFromDataSource,can't get new url from sdp!!!");
	    pSessionDesc.clear();
	    mUri.clear();
	    mInitCheck = ERROR_UNSUPPORTED;
	    return;
        }
	mSessionDesc = pSessionDesc;
	return;
    }
#endif
    CHECK(extractor != NULL);

    for (size_t i = 0; i < extractor->countTracks(); ++i) {
        sp<MetaData> meta = extractor->getTrackMetaData(i);

        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        sp<MediaSource> track;

        if (!strncasecmp(mime, "audio/", 6)) {
            if (mAudioTrack.mSource == NULL) {
                mAudioTrack.mSource = track = extractor->getTrack(i);

                if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_VORBIS)) {
                    mAudioIsVorbis = true;
                } else {
                    mAudioIsVorbis = false;
                }
            }
        } else if (!strncasecmp(mime, "video/", 6)) {
            if (mVideoTrack.mSource == NULL) {
                mVideoTrack.mSource = track = extractor->getTrack(i);
            }
        }

        if (track != NULL) {
            int64_t durationUs;
            if (meta->findInt64(kKeyDuration, &durationUs)) {
                if (durationUs > mDurationUs) {
                    mDurationUs = durationUs;
                }
            }
        }
    }
}

NuPlayer::GenericSource::~GenericSource() {
}

void NuPlayer::GenericSource::start() {
    ALOGI("start");

    if (mAudioTrack.mSource != NULL) {
        CHECK_EQ(mAudioTrack.mSource->start(), (status_t)OK);

        mAudioTrack.mPackets =
            new AnotherPacketSource(mAudioTrack.mSource->getFormat());

        readBuffer(true /* audio */);
    }

    if (mVideoTrack.mSource != NULL) {
        CHECK_EQ(mVideoTrack.mSource->start(), (status_t)OK);

        mVideoTrack.mPackets =
            new AnotherPacketSource(mVideoTrack.mSource->getFormat());

        readBuffer(false /* audio */);
    }
}

status_t NuPlayer::GenericSource::feedMoreTSData() {
    return OK;
}

sp<MetaData> NuPlayer::GenericSource::getFormatMeta(bool audio) {
    sp<MediaSource> source = audio ? mAudioTrack.mSource : mVideoTrack.mSource;
	
#ifndef ANDROID_DEFAULT_CODE
	if(mUri.string()&& mSessionDesc.get()){
		//if is sdp
		sp<MetaData> meta = new MetaData();
		meta->setCString(kKeyMIMEType,MEDIA_MIMETYPE_APPLICATION_SDP);
		meta->setCString(kKeyUri,mUri.string());
		meta->setPointer(kKeySDP,mSessionDesc.get());
		return meta;
	}
#endif
    if (source == NULL) {
        return NULL;
    }

    return source->getFormat();
}

status_t NuPlayer::GenericSource::dequeueAccessUnit(
        bool audio, sp<ABuffer> *accessUnit) {
    Track *track = audio ? &mAudioTrack : &mVideoTrack;

    if (track->mSource == NULL) {
        return -EWOULDBLOCK;
    }

    status_t finalResult;
    if (!track->mPackets->hasBufferAvailable(&finalResult)) {
        return finalResult == OK ? -EWOULDBLOCK : finalResult;
    }

    status_t result = track->mPackets->dequeueAccessUnit(accessUnit);

    readBuffer(audio, -1ll);

    return result;
}

status_t NuPlayer::GenericSource::getDuration(int64_t *durationUs) {
    *durationUs = mDurationUs;
    return OK;
}

status_t NuPlayer::GenericSource::seekTo(int64_t seekTimeUs) {
    if (mVideoTrack.mSource != NULL) {
        int64_t actualTimeUs;
        readBuffer(false /* audio */, seekTimeUs, &actualTimeUs);

        seekTimeUs = actualTimeUs;
    }

    if (mAudioTrack.mSource != NULL) {
        readBuffer(true /* audio */, seekTimeUs);
    }

    return OK;
}

void NuPlayer::GenericSource::readBuffer(
        bool audio, int64_t seekTimeUs, int64_t *actualTimeUs) {
    Track *track = audio ? &mAudioTrack : &mVideoTrack;
    CHECK(track->mSource != NULL);

    if (actualTimeUs) {
        *actualTimeUs = seekTimeUs;
    }

    MediaSource::ReadOptions options;

    bool seeking = false;

    if (seekTimeUs >= 0) {
        options.setSeekTo(seekTimeUs);
        seeking = true;
    }

    for (;;) {
        MediaBuffer *mbuf;
        status_t err = track->mSource->read(&mbuf, &options);

        options.clearSeekTo();

        if (err == OK) {
            size_t outLength = mbuf->range_length();

            if (audio && mAudioIsVorbis) {
                outLength += sizeof(int32_t);
            }

            sp<ABuffer> buffer = new ABuffer(outLength);

            memcpy(buffer->data(),
                   (const uint8_t *)mbuf->data() + mbuf->range_offset(),
                   mbuf->range_length());

            if (audio && mAudioIsVorbis) {
                int32_t numPageSamples;
                if (!mbuf->meta_data()->findInt32(
                            kKeyValidSamples, &numPageSamples)) {
                    numPageSamples = -1;
                }

                memcpy(buffer->data() + mbuf->range_length(),
                       &numPageSamples,
                       sizeof(numPageSamples));
            }

            int64_t timeUs;
            CHECK(mbuf->meta_data()->findInt64(kKeyTime, &timeUs));

            buffer->meta()->setInt64("timeUs", timeUs);
            ALOGV("read %s buffer %.2f", audio ? "audio":"video", timeUs / 1E6);

            if (actualTimeUs) {
                *actualTimeUs = timeUs;
            }

            mbuf->release();
            mbuf = NULL;

            if (seeking) {
#ifndef ANDROID_DEFAULT_CODE
                track->mPackets->queueDiscontinuity(
                        ATSParser::DISCONTINUITY_FLUSH_SOURCE_ONLY, NULL);
#else
                track->mPackets->queueDiscontinuity(
                        ATSParser::DISCONTINUITY_SEEK, NULL);
#endif
            }

            track->mPackets->queueAccessUnit(buffer);
            break;
        } else if (err == INFO_FORMAT_CHANGED) {
#if 0
            track->mPackets->queueDiscontinuity(
                    ATSParser::DISCONTINUITY_FORMATCHANGE, NULL);
#endif
        } else {
            track->mPackets->signalEOS(err);
            break;
        }
    }
}

uint32_t NuPlayer::GenericSource::flags() const {
    return FLAG_SEEKABLE;
}
#ifndef ANDROID_DEFAULT_CODE
status_t NuPlayer::GenericSource::initCheck() const{
	return mInitCheck;
}
#endif
}  // namespace android
