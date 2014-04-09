/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*****************************************************************************
 *
 * Filename:
 * ---------
 *   OMXCodec_ut.cpp
 *
 * Project:
 * --------
 *   MT65xx
 *
 * Description:
 * ------------
 *   OMXcodec Video unit test code
 *
 * Author:
 * -------
 *   Bruce Hsu (mtk04278)
 *
 ****************************************************************************/

//reference from OMXHarness.cpp and MtkVideoTranscoder.cpp
#define LOG_TAG "OMXCodecUT"
#include <utils/Log.h>

#include "OMXCodec_ut.h"

#include <sys/time.h>

#include <binder/ProcessState.h>
//#include <binder/MemoryDealer.h>
#include <binder/IServiceManager.h>
#include <media/IMediaPlayerService.h>
//#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaBuffer.h>
//#include <media/stagefright/MediaDebug.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MetaData.h>

#include <sys/types.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "gtest/gtest.h"
#define DEFAULT_TIMEOUT         500000

#define EXPECT(condition, info) \
    if (!(condition)) {         \
        ALOGE(info); printf("\n  * " info "\n"); return UNKNOWN_ERROR; \
    }

namespace android {

YUVSource::YUVSource(const char* inputFile, int32_t inputColorFormat, int32_t inputWidth, int32_t inputHeight, int32_t& status)
{
    pfFin = fopen(inputFile, "rb");
    if(pfFin == NULL)
    {
        ALOGE("Open file %s fail!", inputFile);
        status = UNKNOWN_ERROR;
        return;
    }

    mWidth = inputWidth;
    mHeight= inputHeight;
    mColorFormat = inputColorFormat;
    // init meta data
    mMetaData = new MetaData;
    mMetaData->setInt32(kKeyWidth, mWidth);
    mMetaData->setInt32(kKeyHeight, mHeight);
    mMetaData->setInt32(kKeyColorFormat, mColorFormat);
    mMetaData->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_RAW);
    //mMetaData->setInt64(kKeyDuration, mSourceDurationUs);

    for (int i = 0 ; i < 2 ; i++)
    {
        MediaBuffer *pBuf = new MediaBuffer(inputWidth*inputHeight*3/2);
        mBufferGroup.add_buffer(pBuf);
        ALOGD("buf %d is %X", i, (unsigned int)pBuf);
    }

    pthread_mutex_init(&mFileLock, NULL);

    mTimeUs = 0;
    mFrame = 0;
    mStart = false;
    status = OK;
}
YUVSource::~YUVSource()
{
    if(pfFin)
    {
        fclose(pfFin);
    }
    pthread_mutex_destroy(&mFileLock);
}
sp<MetaData> YUVSource::getFormat()
{
    ALOGE("YUV getFormat");
    return mMetaData;
}
status_t YUVSource::start(MetaData *params)
{
    ALOGE("YUV start");
    mStart = true;
    return OK;
}
status_t YUVSource::stop()
{
    ALOGE("YUV stop");
    mStart = false;
    return OK;
}
status_t YUVSource::read(MediaBuffer **buffer, const MediaSource::ReadOptions *options)
{
    ALOGE("YUV read");
    if(mStart)
    {
        pthread_mutex_lock(&mFileLock);
        if(options != NULL)
        {
            int64_t time_us;
            ALOGE("read get option");
            ReadOptions::SeekMode  seekMode;
            if(options->getSeekTo(&time_us, &seekMode))
            {
                ALOGD("time=%lu, frame=%llu, mode=%d, Don't support seekMode", time_us, time_us/33333, seekMode);
                int iFrameNum = time_us/33333;
                mTimeUs = iFrameNum*33333;
                ALOGD("do file seek pos=%d, tid=%d", iFrameNum*mWidth*mHeight*3/2, gettid());
                fseek(pfFin, iFrameNum*mWidth*mHeight*3/2, SEEK_SET);
                ALOGD("after fseek");
            }
        }

        CHECK_EQ(mBufferGroup.acquire_buffer(buffer), (status_t)OK);
        ALOGD("before YUV read, buf size=%u, tid=%d", (*buffer)->range_length(), gettid());
        int iLen = fread((uint8_t*)(*buffer)->data(), 1, mWidth*mHeight*3/2, pfFin);
        ALOGD("after YUV read, len=%d", iLen);
        (*buffer)->set_range(0, iLen);
        (*buffer)->meta_data()->clear();
        (*buffer)->meta_data()->setInt64(kKeyTime, mTimeUs);
        if(iLen == 0)//EOS
        {
            (*buffer)->release();
            *buffer = NULL;
            pthread_mutex_unlock(&mFileLock);
            return ERROR_END_OF_STREAM;
        }
        else
        {
            mTimeUs += 33333;
        }
        pthread_mutex_unlock(&mFileLock);
        return OK;
    }
    else
    {
        ALOGE("YUVSource Read but doesn't start");
        return UNKNOWN_ERROR;
    }
}

#ifdef MTK_SUPPORT_MJPEG
MJpgSource::MJpgSource(const char* inputFile, int32_t& status)
    :mReadNum(0)
{
    ALOGD("::MJpgSource+");
    pfFin = fopen(inputFile, "rb");
    if(pfFin == NULL)
    {
        ALOGE("Open file %s fail!", inputFile);
        status = UNKNOWN_ERROR;
        return;
    }

    mWidth = 640;
    mHeight= 480;
    mColorFormat = OMX_COLOR_Format32bitARGB8888;
    //mSourceDuration = 0;
    // init meta data
    mMetaData = new MetaData;
    mMetaData->setInt32(kKeyWidth, mWidth);
    mMetaData->setInt32(kKeyHeight, mHeight);
    mMetaData->setInt32(kKeyColorFormat, mColorFormat);
    mMetaData->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MJPEG);
    //mMetaData->setData(kKeyMJPG, 0, &mCodecConfig, 0);
    //mMetaData->setInt64(kKeyDuration, mSourceDurationUs);

    for (int i = 0 ; i < 2 ; i++)
    {
        //MediaBuffer *pBuf = new MediaBuffer(inputWidth*inputHeight*3/2);
        MediaBuffer *pBuf = new MediaBuffer(mBufferSize);
        mBufferGroup.add_buffer(pBuf);
        ALOGD("buf %d is %X", i, (unsigned int)pBuf);
    }

    pthread_mutex_init(&mFileLock, NULL);

    mTimeUs = 0;
    mFrame = 0;
    mStart = false;
    status = OK;
    ALOGD("::MJpgSource-");

}
MJpgSource::~MJpgSource()
{
    if(pfFin)
    {
        fclose(pfFin);
    }
    pthread_mutex_destroy(&mFileLock);
}
sp<MetaData> MJpgSource::getFormat()
{
    ALOGE("MJpg getFormat");
    return mMetaData;
}
status_t MJpgSource::start(MetaData *params)
{
    ALOGE("MJpg start");
    mStart = true;
    return OK;
}
status_t MJpgSource::stop()
{
    ALOGE("MJpg stop");
    mStart = false;
    return OK;
}
status_t MJpgSource::read(MediaBuffer **buffer, const MediaSource::ReadOptions *options)
{
    ALOGE("MJpg read");
    if(mStart)
    {
        pthread_mutex_lock(&mFileLock);
        if(options != NULL)
        {
            int64_t time_us;
            ALOGE("read get option");
            ReadOptions::SeekMode  seekMode;
            if(options->getSeekTo(&time_us, &seekMode))
            {
                ALOGD("time=%lu, frame=%llu, mode=%d, Don't support seekMode", time_us, time_us/33333, seekMode);
                int iFrameNum = time_us/33333;
                mTimeUs = iFrameNum*33333;
                //ALOGD("do file seek pos=%d, tid=%d", iFrameNum*mWidth*mHeight*3/2, gettid());
                //fseek(pfFin, iFrameNum*mWidth*mHeight*3/2, SEEK_SET);
                ALOGD("after fseek");
            }
        }

        CHECK_EQ(mBufferGroup.acquire_buffer(buffer), (status_t)OK);
        ALOGD("before MJpg read, buf size=%u, tid=%d", (*buffer)->range_length(), gettid());
        rewind(pfFin);
        int iLen = fread((uint8_t*)(*buffer)->data(), 1, mBufferSize, pfFin);
        ++mReadNum;
        ALOGD("after MJpg read, len=%d", iLen);
        (*buffer)->set_range(0, iLen);
        (*buffer)->meta_data()->clear();
        (*buffer)->meta_data()->setInt64(kKeyTime, mTimeUs);
        if(iLen == 0 || mReadNum == mEndFrameNo)//EOS
        {
            (*buffer)->release();
            *buffer = NULL;
            pthread_mutex_unlock(&mFileLock);
            return ERROR_END_OF_STREAM;
        }
        else
        {
            mTimeUs += 33333;
        }
        pthread_mutex_unlock(&mFileLock);
        return OK;
    }
    else
    {
        ALOGE("YUVSource Read but doesn't start");
        return UNKNOWN_ERROR;
    }
}
#endif//MTK_SUPPORT_MJPEG

static int getWHFromName(const char *szName, int *piWidth, int *piHeight)
{
    struct NameToMime {
        const char *szName;
        int         iWidth;
        int         iHeight;
    };
    static const NameToMime kNameToMime[] = {
        {"OMX.MTK.VIDEO.ENCODER.AVC", 480, 320},
        {"OMX.MTK.VIDEO.ENCODER.MPEG4", 320, 240}
    };
    for(size_t i=0;i<sizeof(kNameToMime)/sizeof(kNameToMime[0]);i++)
    {
        if(!strcmp(szName, kNameToMime[i].szName))
        {
            *piWidth = kNameToMime[i].iWidth;
            *piHeight= kNameToMime[i].iHeight;
            return 1;
        }
    }
    return 0;
}
static const char *getMimeFromName(const char *szName)
{
    struct NameToMime {
        const char *szName;
        const char *szMime;
    };
    static const NameToMime kNameToMime[] = {
        {"OMX.MTK.VIDEO.DECODER.AVC", "video/avc"},
        {"OMX.MTK.VIDEO.DECODER.MPEG4", "video/mp4v-es"},
        {"OMX.MTK.VIDEO.DECODER.VPX", "video/x-vnd.on2.vp8"},
#ifdef MTK_SUPPORT_MJPEG
        {"OMX.MTK.VIDEO.DECODER.MJPEG", "video/x-motion-jpeg"},
#endif//MTK_SUPPORT_MJPEG
    };
    for(size_t i=0;i<sizeof(kNameToMime)/sizeof(kNameToMime[0]);i++)
    {
        if(!strcmp(szName, kNameToMime[i].szName))
        {
            return kNameToMime[i].szMime;
        }
    }
    return "";
}

static const char *getFileFromName(const char *szName, int iMJpg)
{
    struct NameToFile{
        const char *szName;
        const char *szFile;
    };
    static const NameToFile kNameToFile[] = {
        {"OMX.MTK.VIDEO.DECODER.AVC", "/mnt/sdcard/OMXCodec/h264.mp4"},
        {"OMX.MTK.VIDEO.DECODER.MPEG4", "/mnt/sdcard/OMXCodec/mp4.mp4"},
        {"OMX.MTK.VIDEO.DECODER.VPX", "/mnt/sdcard/OMXCodec/vp8.webm"},
#ifdef MTK_SUPPORT_MJPEG
        {"OMX.MTK.VIDEO.DECODER.MJPEG", "/mnt/sdcard/OMXCodec/mjpg.mymjpg"},
        {"OMX.MTK.VIDEO.DECODER.MJPEG", "/mnt/sdcard/OMXCodec/test.asf"},
        {"OMX.MTK.VIDEO.DECODER.MJPEG", "/mnt/sdcard/OMXCodec/test.avi"},
        {"OMX.MTK.VIDEO.DECODER.MJPEG", "/mnt/sdcard/OMXCodec/test.mp4"},
        {"OMX.MTK.VIDEO.DECODER.MJPEG", "/mnt/sdcard/OMXCodec/test.mkv"},
#endif//MTK_SUPPORT_MJPEG
        {"OMX.MTK.VIDEO.ENCODER.AVC", "/mnt/sdcard/OMXCodec/lab_fast_hvga_212f.yuv"},
        {"OMX.MTK.VIDEO.ENCODER.MPEG4", "/mnt/sdcard/OMXCodec/akiyo_mtk_qvga.yuv"}
    };
    for(size_t i=0;i<sizeof(kNameToFile)/sizeof(kNameToFile[0]);i++)
    {
        //ALOGD("%s %s", szName, kNameToFile[i].szName);
        if(!strcmp(szName, kNameToFile[i].szName))
        {
            if (!strcmp(szName, "OMX.MTK.VIDEO.DECODER.MJPEG"))
            {
                i += iMJpg;
            }
            return kNameToFile[i].szFile;
        }
    }
    return "";
}

OMXCodecTest::OMXCodecTest() 
#ifdef USE_NATIVE_BUFFER
    : mComposerClient(NULL),
    mSurfaceControl(NULL),
    mNativeWindow(NULL),
    mUseNativeBuffer(false)
#endif//USE_NATIVE_BUFFER
{
    mInitStat = init("OMX.MTK.VIDEO.DECODER.AVC");
}

OMXCodecTest::OMXCodecTest(const char *szCompName, bool bUseNativeBuf)
#ifdef USE_NATIVE_BUFFER
    : mComposerClient(NULL),
    mSurfaceControl(NULL),
    mNativeWindow(NULL),
    mUseNativeBuffer(bUseNativeBuf)
#endif//USE_NATIVE_BUFFER
{
    mInitStat = init(szCompName);
}

OMXCodecTest::OMXCodecTest(const char *szCompName, int iMjpgContainer, int redundant)
#ifdef USE_NATIVE_BUFFER
    : mComposerClient(NULL),
    mSurfaceControl(NULL),
    mNativeWindow(NULL),
    mUseNativeBuffer(bUseNativeBuf)
#endif//USE_NATIVE_BUFFER
{
    mjpgContainer = iMjpgContainer;
    mInitStat = init(szCompName);
}

OMXCodecTest::~OMXCodecTest()
{
    ALOGD("~OMXCodecTest");
}

int OMXCodecTest::initOMX()
{
    ALOGE("+initOMX");
    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> binder = sm->getService(String16("media.player"));
    sp<IMediaPlayerService> service = interface_cast<IMediaPlayerService>(binder);
    mOMX = service->getOMX();

    ALOGE("-initOMX");
    return mOMX != 0 ? OK : NO_INIT;
}
#if 0
static sp<MediaExtractor> CreateExtractorFromURI(const char *uri) {
    sp<DataSource> source = DataSource::CreateFromURI(uri);
    ALOGE("create source by uri:%s", uri);

    if (source == NULL) {
        ALOGE("source is NULL");
        return NULL;
    }

    return MediaExtractor::Create(source);
}
static sp<MediaSource> CreateSourceForMime(const char *mime) {
    //const char *url = GetURLForMime(mime);
    const char *url = "file:///sdcard/test.3gp";
    CHECK(url != NULL);

    ALOGE("url=%s", url);
    sp<MediaExtractor> extractor = CreateExtractorFromURI(url);

    if (extractor == NULL) {
        ALOGE("extractor is NULL");
        return NULL;
    }

    for (size_t i = 0; i < extractor->countTracks(); ++i) {
        sp<MetaData> meta = extractor->getTrackMetaData(i);
        CHECK(meta != NULL);

        const char *trackMime;
        CHECK(meta->findCString(kKeyMIMEType, &trackMime));

        if (!strcasecmp("video/avc", trackMime)) {
            return extractor->getTrack(i);
        }
    }

    return NULL;
}
#endif//0
int OMXCodecTest::initSource(const char *szName)
{
    const char *uri = getFileFromName(szName, mjpgContainer-1);
    const char *mime = getMimeFromName(szName);

    printf("source file:%s\n", uri);
    ALOGE("source file:%s\n", uri);
    if(mIsEncoder)
    {
        if(getWHFromName(szName, &mWidth, &mHeight) == 0)
        {
            return UNKNOWN_ERROR;
        }

        status_t err;
        mSource = new YUVSource(uri, (int32_t)OMX_COLOR_FormatYUV420Planar, (int32_t)mWidth, (int32_t)mHeight, (int32_t&)err);
        if(err != OK)
        {
            mSource.clear();
            mSource = NULL;
            ALOGE("mSource is NULL");
            return NO_INIT;
        }
        return OK;
    }
    else
    {
#ifdef MTK_SUPPORT_MJPEG
        if (!strcmp(szName, "OMX.MTK.VIDEO.DECODER.MJPEG") && mjpgContainer == 1)//is motion jpeg
        {
            status_t err;
            mSource = new MJpgSource(uri, (int32_t&)err);
            if (mSource == NULL)
            {
                ALOGE("mSource is NULL");
                return NO_INIT;
            }
            else
            {
                return OK;
            }
        }
#endif//MTK_SUPPORT_MJPEG
        mSource = getSource(uri, mime);
        mSeekSource = getSource(uri, mime);

        if(mSource == NULL || mSeekSource == NULL)
        {
            ALOGE("mSource or mSeekSource is NULL");
            return NO_INIT;
        }
        else
        {
            return OK;
        }
    }
}

int OMXCodecTest::initMetaData(const char *szCompName)
{
    if(mIsEncoder)
    {
        mMetaData = new MetaData;

        mMetaData->setInt32(kKeyWidth, mWidth);
        mMetaData->setInt32(kKeyHeight, mHeight);
        mMetaData->setInt32(kKeyStride, mWidth);        //should be ROUND16(width)
        mMetaData->setInt32(kKeySliceHeight, mHeight);  //should be ROUND16(height)

        mMetaData->setInt32(kKeyFrameRate, mFps);
        mMetaData->setInt32(kKeyBitRate, 512*1024);
        mMetaData->setInt32(kKeyIFramesInterval, 1); //second
        mMetaData->setInt32(kKeyColorFormat, OMX_COLOR_FormatYUV420Planar);    // MT6575
        if(!strcmp(szCompName, "OMX.MTK.VIDEO.ENCODER.MPEG4"))
        {
            mMetaData->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG4);
        }
        else if(!strcmp(szCompName, "OMX.MTK.VIDEO.ENCODER.AVC"))
        {
            mMetaData->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_AVC);
        }
        else
        {
            ALOGE("codec not supported");
            return NO_INIT;
        }
    }
    else
    {
        mMetaData = mSource->getFormat();
    }
    return OK;
}

int OMXCodecTest::init(const char *szCompName)
{
    ALOGE("+init");

    //mCompName = strdup(szCompName);
    checkNameForEncoder(szCompName);

    if(initOMX() != OK)
    {
        printf("initOMX fail\n");
        ALOGE("initOMX fail\n");
        return -1;
    }

    if(initSource(szCompName) != OK)
    {
        printf("initSource fail\n");
        ALOGE("initSource fail\n");
        return -1;
    }

    if(initMetaData(szCompName) != OK)
    {
        printf("initMetaData fail\n");
        ALOGE("initMetaData fail\n");
        return -1;
    }
#ifdef USE_NATIVE_BUFFER
    if(mUseNativeBuffer)
    {
        //init display member
        mComposerClient = new SurfaceComposerClient;
        if(mComposerClient->initCheck() != (status_t)OK)
        {
            ALOGE("SurfaceComposerClient initCheck fail\n");
            return -1;
        }
        mSurfaceControl = mComposerClient->createSurface(String8("dec test surface"), 480, 320, PIXEL_FORMAT_RGB_888, 0);
        ALOGE("Get Surface Control!\n");

        SurfaceComposerClient::openGlobalTransaction();
        mSurfaceControl->setLayer(0x7fffffff);
        mSurfaceControl->show();
        SurfaceComposerClient::closeGlobalTransaction();

        mNativeWindow = mSurfaceControl->getSurface();
        ALOGE("Get window!\n");

        mOmxCodec = OMXCodec::Create(
                mOMX, mMetaData, mIsEncoder/* createEncoder */,
                mSource, szCompName, 0, mNativeWindow);
    }
    else
#endif//USE_NATIVE_BUFFER
    {
        mOmxCodec = OMXCodec::Create(
                mOMX, mMetaData, mIsEncoder/* createEncoder */,
                mSource, szCompName);
    }
    if(mOmxCodec == NULL)
    {
        printf("create OMXCodec fail\n");
        ALOGE("create OMXCodec fail\n");
        return -1;
    }

    ALOGE("-init");
    return 1;
}

bool OMXCodecTest::checkNameForEncoder(const char *szCompName)
{
    mIsEncoder = (strstr(szCompName, "ENCODER")) ? true : false;
    return mIsEncoder;
}

sp<MediaSource> OMXCodecTest::getSource(const char *szUri, const char *szMime)
{
    sp<DataSource> source = DataSource::CreateFromURI(szUri);
    if (source == NULL)
    {
        ALOGE("source is NULL");
        return NULL;
    }

    sp<MediaExtractor> extractor = MediaExtractor::Create(source);
    if (extractor == NULL)
    {
        ALOGE("extractor is NULL");
        return NULL;
    }
    for (size_t i = 0; i < extractor->countTracks(); ++i)
    {
        sp<MetaData> meta = extractor->getTrackMetaData(i);
        CHECK(meta != NULL);

        const char *trackMime;
        CHECK(meta->findCString(kKeyMIMEType, &trackMime));

        if (!strcasecmp(szMime, trackMime))
        {
            return extractor->getTrack(i);
        }
    }
    return NULL;
}
#ifdef USE_NATIVE_BUFFER
int OMXCodecTest::setUseNativeBuffer(bool setter)
{
    mUseNativeBuffer = setter;
    return 1;
}
#endif//USE_NATIVE_BUFFER
int OMXCodecTest::checkInitStat()
{
    return mInitStat;
}
#if 0
int OMXCodecTest::doTest()
{
    if(mOmxCodec == NULL)
    {
        ALOGE("OmxCodec is NULL");
        return -1;
    }

    if(StartAndStopTest()) printf("case StartAndStopTest success\n");
    else printf("case StartAndStopTest fail\n");
    if(GetFormatTest()) printf("case GetFormatTest success\n");
    else printf("case GetFormatTest fail\n");
    
    if(mIsEncoder)
    {
        if(SetForceIFrameTest()) printf("case SetForceIFrameTest success\n");
        else printf("case SetForceIFrameTest fail\n");
        //GetCameraMetaDateTest();
    }

    if(ReadBeforeStartTest()) printf("case ReadBeforeStartTest success\n");
    else printf("case ReadBeforeStartTest fail\n");

    if(ReadAfterStopTest()) printf("case ReadAfterStopTest success\n");
    else printf("case ReadAfterStopTest fail\n");

    if(!mIsEncoder)
    {
        if(TrickPlayTest()) printf("case TrickPlay success\n");
        else printf("case TrickPlay fail\n");
    }

    return 1;
}
#endif//0
int OMXCodecTest::StartAndStopTest()
{
    if(mInitStat < 0) return 0;

    ALOGD("start");
    if(mOmxCodec->start() != OK)
    {
        //printf("start fail\n");
        return -1;
    }

    MediaBuffer *buffer=NULL;
    status_t err;

    for(unsigned int i=0;i<0xffffffff;i++)
    {
        ALOGD("frame %d", i);

        if(i == 0)
        {
            MediaSource::ReadOptions    options;
            options.setSeekTo(0);
            err = mOmxCodec->read(&buffer, &options);
        }
        else
        {
            err = mOmxCodec->read(&buffer);
        }
        if(err == OK)
        {
            if(buffer != NULL)
            {
                int iIsSyncFrame;
                if(!buffer->meta_data()->findInt32(kKeyIsSyncFrame, &iIsSyncFrame))
                {
                    iIsSyncFrame = -1;
                }
                if (i == 15)
                {
                    FILE *pfOut;
                    pfOut = fopen("/sdcard/omxc.raw", "wb");
                    if (pfOut == NULL)
                    {
                        printf("!!!! open file error\n");
                        ALOGE("!!!! open file error");
                    }
                    fwrite(buffer->data(), 1, buffer->range_length(), pfOut);
                    fclose(pfOut);
                }
                //printf("frame %d, size=%d, sync=%d\n", i, buffer->range_length(), iIsSyncFrame);
                buffer->release();
                buffer = NULL;
            }
        }
        else
        {
            if(buffer != NULL)
            {
                ALOGD("impossible!!");
                printf("frame %d, size=%d\n", i, buffer->range_length());
                buffer->release();
                buffer = NULL;
            }
            if(err == ERROR_END_OF_STREAM)
            {
                ALOGD("EOS");
                break;
            }
            else
            {
                ALOGD("err=%X", err);
                mOmxCodec->stop();
                return 0;
            }
        }
    }

    ALOGD("stop");
    if(mOmxCodec->stop() != OK)
    {
        return -1;
    }

    return 1;
}
int OMXCodecTest::GetFormatTest()
{
    if(mInitStat < 0) return 0;

    sp<MetaData> format = mOmxCodec->getFormat();
    if(format == NULL) return 0;
    const char *szMime;
    int width, height;
    if(!format->findCString(kKeyMIMEType, &szMime)) return 0;
    if(!format->findInt32(kKeyWidth, &width)) return 0;
    if(!format->findInt32(kKeyHeight, &height)) return 0;
    ALOGD("Get output format mime=%s, w=%d, h=%d", szMime, width, height);

    return 1;
}
int OMXCodecTest::SetForceIFrameTest()
{
    if(mInitStat < 0) return 0;

#ifndef ANDROID_DEFAULT_CODE
    return 1;
#else
    ALOGD("start");
    mOmxCodec->start();

    MediaBuffer *buffer=NULL;
    status_t err;
    bool bGetIFrame=false;
    for(int i=0;i<mFps;i++)
    {
        ALOGD("frame %d", i);
        reinterpret_cast<OMXCodec*>(mOmxCodec.get())->vEncSetForceIframe(true);
        if(i == 0)
        {
            //int64_t iTime=0;
            MediaSource::ReadOptions    options;
            options.setSeekTo(0);
            err = mOmxCodec->read(&buffer, &options);
        }
        else
        {
            err = mOmxCodec->read(&buffer);
        }
        if(err == OK)
        {
            if(buffer != NULL)
            {
                int iIsSyncFrame;
                if(!buffer->meta_data()->findInt32(kKeyIsSyncFrame, &iIsSyncFrame))
                {
                    iIsSyncFrame = -1;
                }
                if(!bGetIFrame && i > 1 && iIsSyncFrame == 1)
                {
                    ALOGD("get I frame");
                    bGetIFrame = true;
                }
                //printf("frame %d, size=%d, sync=%d\n", i, buffer->range_length(), iIsSyncFrame);
                buffer->release();
                buffer = NULL;
            }
        }
    }

    ALOGD("stop");
    mOmxCodec->stop();
    return bGetIFrame ? 1 : 0;
#endif//not ANDROID_DEFAULT_CODE
}
int OMXCodecTest::GetCameraMetaDateTest()
{
    if(mInitStat < 0) return 0;

#ifndef ANDROID_DEFAULT_CODE
    return 1;
#else
    sp<MetaData> camera = reinterpret_cast<OMXCodec*>(mOmxCodec.get())->getCameraMeta();
    if(camera == NULL)
    {
        ALOGD("Camera meta does not exist");
        return 0;
    }
    else
    {
        int32_t prCameraInfo;
        camera->findInt32(kKeyCamMemInfo, &prCameraInfo);
        ALOGD("Get camera int %X", prCameraInfo);
    }
    return 1;
#endif//not ANDROID_DEFAULT_CODE
}
int OMXCodecTest::ReadBeforeStartTest()
{
    if(mInitStat < 0) return 0;

    MediaBuffer *buffer;
    status_t err;

    err = mOmxCodec->read(&buffer, NULL);
    ALOGD("Read before start err=%X", err);
    return (err == OK) ? 0 : 1;
}
int OMXCodecTest::ReadAfterStopTest()
{
    if(mInitStat < 0) return 0;

    ALOGD("start");
    mOmxCodec->start();

    MediaBuffer *buffer=NULL;
    status_t err;

    for(unsigned int i=0;i<100;i++)
    {
        ALOGD("frame %d", i);

        if(i == 0)
        {
            MediaSource::ReadOptions    options;
            options.setSeekTo(0);
            err = mOmxCodec->read(&buffer, &options);
        }
        else
        {
            err = mOmxCodec->read(&buffer);
        }
        if(err == OK)
        {
            if(buffer != NULL)
            {
                int iIsSyncFrame;
                if(!buffer->meta_data()->findInt32(kKeyIsSyncFrame, &iIsSyncFrame))
                {
                    iIsSyncFrame = -1;
                }
                //printf("frame %d, size=%d, sync=%d\n", i, buffer->range_length(), iIsSyncFrame);
                buffer->release();
                buffer = NULL;
            }
        }
        else
        {
            if(buffer != NULL)
            {
                ALOGD("impossible!!");
                printf("frame %d, size=%d\n", i, buffer->range_length());
                buffer->release();
                buffer = NULL;
            }
            if(err == ERROR_END_OF_STREAM)
            {
                ALOGD("EOS");
                break;
            }
            else
            {
                ALOGD("err=%X", err);
                mOmxCodec->stop();
                return 0;
            }
        }
    }

    ALOGD("stop");
    mOmxCodec->stop();

    err = mOmxCodec->read(&buffer);
    if(buffer != NULL)
    {
        buffer->release();
    }

    return (err == OK) ? 0 : 1;
}
static void *threadfunc1(void *pvData)
{
    int     iRet;
    char    *compName = (char *)pvData;
    sp<OMXCodecTest> hTest = new OMXCodecTest(compName);
    iRet = hTest->StartAndStopTest();

    pthread_exit((void*)iRet);
    return NULL;
}
static int MultiInstancesTest(char *compName)
{
    static const int iNum=3;
    pthread_t   tId[iNum];
    int         iResult=0;
    void        *apResult[iNum];
    for(int i=0;i<iNum;i++)
    {
        pthread_create(&tId[i], NULL, threadfunc1, (void*)compName);
    }

    for(int i=0;i<iNum;i++)
    {
        pthread_join(tId[i], &apResult[i]);
    }

    for(int i=0;i<iNum;i++)
    {
        iResult += (unsigned int)apResult[i];
    }
    //printf("%X %X %X\n", apResult[0], apResult[1], apResult[2]);
    return (iResult == iNum) ? 1 : 0;
}
struct oomTestInput 
{
    char    *compName;
    int     iId;
    bool    bUseNativeBuf;
    int     *aResult;
};
static void *threadfunc2(void *pvData)
{
    int             iRet    = 0;
    oomTestInput    *ptInput= (oomTestInput*)pvData;

    sp<OMXCodecTest> hTest = new OMXCodecTest(ptInput->compName, ptInput->bUseNativeBuf);
    if(hTest->checkInitStat() < 0)
    {
        iRet = 2;
    }
    else
    {
        iRet = hTest->StartAndStopTest();
    }

    //printf("end thread %d\n", iRet);
    if(iRet < 0) iRet = 2;
    ptInput->aResult[ptInput->iId] = iRet;
    pthread_exit(NULL);
    return NULL;
}
static int OOMTest(char *compName, bool bUseNativeBuf)
{
    static const int iNum=32;
    pthread_t       tId[iNum];
    int             iResult=0, iActualNum=0, iBreak=0;
    oomTestInput    tInput[iNum];
    int             aResult[iNum]={0};
    for(int i=0;i<iNum;i++)
    {
        tInput[i].compName = compName;
        tInput[i].iId = i;
        tInput[i].bUseNativeBuf = bUseNativeBuf;
        tInput[i].aResult = aResult;
        pthread_create(&tId[i], NULL, threadfunc2, (void*)&tInput[i]);
        ++iActualNum;
        sleep(1);
        for(int j=0;j<=i;++j)
        {
            //printf("%u ", tInput[i].aResult[j]);
            if((unsigned int)tInput[i].aResult[j] == 2)
            {
                ALOGE("oom!");
                printf("oom!\n");
                ++iBreak;
                break;
            }
        }
        //printf("\n");
        if(iBreak) break;
    }

    for(int i=0;i<iActualNum;i++)
    {
        pthread_join(tId[i], NULL);
    }

    for(int i=0;i<iActualNum;i++)
    {
        iResult += aResult[i];
    }
    //printf("%X %X %X\n", apResult[0], apResult[1], apResult[2]);
    //return (iResult == iActualNum) ? 1 : 0;
    return 1;
}
static double uniform_rand() {
    return (double)rand() / RAND_MAX;
}
static bool CloseEnough(int64_t time1Us, int64_t time2Us) {
#if 0
    int64_t diff = time1Us - time2Us;
    if (diff < 0) {
        diff = -diff;
    }

    return diff <= 50000;
#else
    return time1Us == time2Us;
#endif
}
int OMXCodecTest::TrickPlayTest()
{
    CHECK_EQ(mOmxCodec->start(), (status_t)OK);
    CHECK_EQ(mSeekSource->start(), (status_t)OK);

    int64_t durationUs;
    CHECK(mSource->getFormat()->findInt64(kKeyDuration, &durationUs));

    ALOGI("stream duration is %lld us (%.2f secs)",
         durationUs, durationUs / 1E6);

    static const int32_t kNumIterations = 100;

    // We are always going to seek beyond EOS in the first iteration (i == 0)
    // followed by a linear read for the second iteration (i == 1).
    // After that it's all random.
    for (int32_t i = 0; i < kNumIterations; ++i) {
        int64_t requestedSeekTimeUs;
        int64_t actualSeekTimeUs;
        MediaSource::ReadOptions options;

        double r = uniform_rand();

        if ((i == 1) || (i > 0 && r < 0.5)) {
            // 50% chance of just continuing to decode from last position.

            requestedSeekTimeUs = -1;

            ALOGI("requesting linear read");
        } else {
            if (i == 0 || r < 0.05) {
                // 5% chance of seeking beyond end of stream.

                requestedSeekTimeUs = durationUs;

                ALOGI("requesting seek beyond EOF");
            } else {
                requestedSeekTimeUs =
                    (int64_t)(uniform_rand() * durationUs);

                ALOGI("requesting seek to %lld us (%.2f secs)",
                     requestedSeekTimeUs, requestedSeekTimeUs / 1E6);
            }

            MediaBuffer *buffer = NULL;
            options.setSeekTo(
                    requestedSeekTimeUs, MediaSource::ReadOptions::SEEK_NEXT_SYNC);

            if (mSeekSource->read(&buffer, &options) != OK) {
                CHECK_EQ(buffer, (void *)NULL);
                actualSeekTimeUs = -1;
            } else {
                CHECK(buffer != NULL);
                CHECK(buffer->meta_data()->findInt64(kKeyTime, &actualSeekTimeUs));
                CHECK(actualSeekTimeUs >= 0);

                buffer->release();
                buffer = NULL;
                while(actualSeekTimeUs < requestedSeekTimeUs)
                {
                    mSeekSource->read(&buffer, NULL);
                    buffer->meta_data()->findInt64(kKeyTime, &actualSeekTimeUs);
                    buffer->release();
                    buffer = NULL;
                    //printf("get frame time, %lld\n", actualSeekTimeUs);
                }
            }

            ALOGI("nearest keyframe is at %lld us (%.2f secs)",
                 actualSeekTimeUs, actualSeekTimeUs / 1E6);
        }
        //printf("seek times %d, request time=%lld\n", i, requestedSeekTimeUs);
        status_t err;
        MediaBuffer *buffer;
        for (;;) {
            err = mOmxCodec->read(&buffer, &options);
            options.clearSeekTo();
            if (err == INFO_FORMAT_CHANGED) {
                CHECK_EQ(buffer, (void *)NULL);
                continue;
            }
            if (err == OK) {
                CHECK(buffer != NULL);
                if (buffer->range_length() == 0) {
                    buffer->release();
                    buffer = NULL;
                    continue;
                }
            } else {
                CHECK_EQ(buffer, (void *)NULL);
            }

            break;
        }

        if (requestedSeekTimeUs < 0) {
            // Linear read.
            if (err != OK) {
                CHECK_EQ(buffer, (void *)NULL);
            } else {
                CHECK(buffer != NULL);
                buffer->release();
                buffer = NULL;
            }
        } else if (actualSeekTimeUs < 0) {
            EXPECT(err != OK,
                   "We attempted to seek beyond EOS and expected "
                   "ERROR_END_OF_STREAM to be returned, but instead "
                   "we got a valid buffer.");
            EXPECT(err == ERROR_END_OF_STREAM,
                   "We attempted to seek beyond EOS and expected "
                   "ERROR_END_OF_STREAM to be returned, but instead "
                   "we found some other error.");
            CHECK_EQ(err, (status_t)ERROR_END_OF_STREAM);
            CHECK_EQ(buffer, (void *)NULL);
        } else {
            EXPECT(err == OK,
                   "Expected a valid buffer to be returned from "
                   "OMXCodec::read.");
            CHECK(buffer != NULL);

            int64_t bufferTimeUs;
            CHECK(buffer->meta_data()->findInt64(kKeyTime, &bufferTimeUs));
            if (!CloseEnough(bufferTimeUs, actualSeekTimeUs)) {
                printf("\n  * Attempted seeking to %lld us (%.2f secs)",
                       requestedSeekTimeUs, requestedSeekTimeUs / 1E6);
                printf("\n  * Nearest keyframe is at %lld us (%.2f secs)",
                       actualSeekTimeUs, actualSeekTimeUs / 1E6);
                printf("\n  * Returned buffer was at %lld us (%.2f secs)\n\n",
                       bufferTimeUs, bufferTimeUs / 1E6);

                buffer->release();
                buffer = NULL;

                CHECK_EQ(mOmxCodec->stop(), (status_t)OK);

                return 0;
            }

            buffer->release();
            buffer = NULL;
        }
    }

    CHECK_EQ(mSeekSource->stop(), (status_t)OK);
    CHECK_EQ(mOmxCodec->stop(), (status_t)OK);
    return 1;
}

}  // namespace android
#if 1 //gtest

using namespace std;
using namespace android;
//--- avc decoder ---
TEST(OMXCodecAVCDTest, test1)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.AVC");
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecAVCDTest, test2)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.AVC");
    EXPECT_EQ(hTest->GetFormatTest(), 1);
}
TEST(OMXCodecAVCDTest, test3)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.AVC");
    EXPECT_EQ(hTest->ReadBeforeStartTest(), 1);
}
TEST(OMXCodecAVCDTest, test4)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    unsigned long seed = 0xdeadbeef;
    srand(seed);
    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.AVC");
    EXPECT_EQ(hTest->TrickPlayTest(), 1);
}
TEST(OMXCodecAVCDTest, test5)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.AVC");
    EXPECT_EQ(hTest->ReadAfterStopTest(), 1);
}
TEST(OMXCodecAVCDTest, test6)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    EXPECT_EQ(MultiInstancesTest("OMX.MTK.VIDEO.DECODER.AVC"), 1);
}
TEST(OMXCodecAVCDTest, test7)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    EXPECT_EQ(OOMTest("OMX.MTK.VIDEO.DECODER.AVC", true), 1);
}
//--- mp4 decoder ---
TEST(OMXCodecMP4DTest, test1)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MPEG4");
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecMP4DTest, test2)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MPEG4");
    EXPECT_EQ(hTest->GetFormatTest(), 1);
}
TEST(OMXCodecMP4DTest, test3)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MPEG4");
    EXPECT_EQ(hTest->ReadBeforeStartTest(), 1);
}
TEST(OMXCodecMP4DTest, test4)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    unsigned long seed = 0xdeadbeef;
    srand(seed);
    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MPEG4");
    EXPECT_EQ(hTest->TrickPlayTest(), 1);
}
TEST(OMXCodecMP4DTest, test5)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MPEG4");
    EXPECT_EQ(hTest->ReadAfterStopTest(), 1);
}
TEST(OMXCodecMP4DTest, test6)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    EXPECT_EQ(MultiInstancesTest("OMX.MTK.VIDEO.DECODER.MPEG4"), 1);
}
//--- vp8 decoder ---
TEST(OMXCodecVP8DTest, test1)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.VPX");
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecVP8DTest, test2)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.VPX");
    EXPECT_EQ(hTest->GetFormatTest(), 1);
}
TEST(OMXCodecVP8DTest, test3)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.VPX");
    EXPECT_EQ(hTest->ReadBeforeStartTest(), 1);
}
TEST(OMXCodecVP8DTest, test4)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    unsigned long seed = 0xdeadbeef;
    srand(seed);
    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.VPX");
    EXPECT_EQ(hTest->TrickPlayTest(), 1);
}
TEST(OMXCodecVP8DTest, test5)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.VPX");
    EXPECT_EQ(hTest->ReadAfterStopTest(), 1);
}
TEST(OMXCodecVP8DTest, test6)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    EXPECT_EQ(MultiInstancesTest("OMX.MTK.VIDEO.DECODER.VPX"), 1);
}

#ifdef MTK_SUPPORT_MJPEG
//--- mjpeg decoder ---
TEST(OMXCodecMJPGDTest, test1)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MJPEG", 1, 1);
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecMJPGDTest, test2)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MJPEG", 2, 1);
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecMJPGDTest, test3)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MJPEG", 3, 1);
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecMJPGDTest, test4)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MJPEG", 4, 1);
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecMJPGDTest, test5)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.DECODER.MJPEG", 5, 1);
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
#endif//0

//--- avc encoder ---
TEST(OMXCodecAVCETest, test1)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.AVC");
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecAVCETest, test2)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.AVC");
    EXPECT_EQ(hTest->GetFormatTest(), 1);
}
TEST(OMXCodecAVCETest, test3)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.AVC");
    EXPECT_EQ(hTest->ReadBeforeStartTest(), 1);
}
TEST(OMXCodecAVCETest, test4)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.AVC");
    EXPECT_EQ(hTest->SetForceIFrameTest(), 1);
}
TEST(OMXCodecAVCETest, test5)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.AVC");
    EXPECT_EQ(hTest->ReadAfterStopTest(), 1);
}
TEST(OMXCodecAVCETest, test6)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    EXPECT_EQ(MultiInstancesTest("OMX.MTK.VIDEO.ENCODER.AVC"), 1);
}
//--- mp4 encoder ---
TEST(OMXCodecMP4ETest, test1)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.MPEG4");
    EXPECT_EQ(hTest->StartAndStopTest(), 1);
}
TEST(OMXCodecMP4ETest, test2)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.MPEG4");
    EXPECT_EQ(hTest->GetFormatTest(), 1);
}
TEST(OMXCodecMP4ETest, test3)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.MPEG4");
    EXPECT_EQ(hTest->ReadBeforeStartTest(), 1);
}
TEST(OMXCodecMP4ETest, test4)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.MPEG4");
    EXPECT_EQ(hTest->SetForceIFrameTest(), 1);
}
TEST(OMXCodecMP4ETest, test5)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest("OMX.MTK.VIDEO.ENCODER.MPEG4");
    EXPECT_EQ(hTest->ReadAfterStopTest(), 1);
}
TEST(OMXCodecMP4ETest, test6)
{
    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    EXPECT_EQ(MultiInstancesTest("OMX.MTK.VIDEO.ENCODER.MPEG4"), 1);
}

int main(int argc, char *argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
#endif// gtest

#if 0
int main(int argc, const char *argv[])
{
    using namespace android;

    if(argc != 2)
    {
        printf("usage:%s component_name\n", argv[0]);
        return 0;
    }

    unsigned long seed = 0xdeadbeef;
    srand(seed);

    android::ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<OMXCodecTest> hTest = new OMXCodecTest(argv[1]);

    ALOGE("Test OmxCodec Start");
    hTest->doTest();
    ALOGE("Test OmxCodec End");

    return 0;
}
#endif//0
