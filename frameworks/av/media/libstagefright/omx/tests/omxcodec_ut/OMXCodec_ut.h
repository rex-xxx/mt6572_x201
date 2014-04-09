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
 *   OMXCodec_ut.h
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

#ifndef OMXCODEC_TEST_H_

#define OMXCODEC_TEST_H_

#include <media/IOMX.h>
#include <utils/List.h>
#include <utils/Vector.h>
#include <utils/threads.h>

//#include <OMX_Component.h>

//#define USE_NATIVE_BUFFER

#include <media/stagefright/OMXCodec.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/DataSource.h>
#ifdef USE_NATIVE_BUFFER
#include <ui/GraphicBuffer.h>
#include <SurfaceComposerClient.h>
#endif//USE_NATIVE_BUFFER
#include <utils/String8.h>

namespace android {

class YUVSource : public MediaSource
{
public:
    YUVSource(const char* input_file, int32_t output_color_format, int32_t targetWidth, int32_t targetHeight, int32_t& status);
        
    virtual sp<MetaData> getFormat();       
    virtual status_t start(MetaData *params);
    virtual status_t stop();
    virtual status_t read(MediaBuffer **buffer, const MediaSource::ReadOptions *options);
    
    //bool cut(int64_t begin_ts, int64_t end_ts);
    //void getActualTimeBoundary(int64_t &begin_ts, int64_t &end_ts, int64_t& clip_duration);

protected:
    virtual ~YUVSource(); 

private:
    bool            mStart;
    sp<MetaData>    mMetaData;

    FILE            *pfFin;
    pthread_mutex_t  mFileLock;

    MediaBufferGroup mBufferGroup;
    int32_t mWidth, mHeight;
    int32_t mColorFormat;
    bool    mUseResizer;
    bool    mInitialRead;
    int64_t mTimeUs;
    int     mFrame;
    //int64_t mSourceDurationUs;
    //int64_t mBeginTs;
    //int64_t mEndTs;

    YUVSource(const YUVSource &);
    YUVSource &operator=(const YUVSource &);
};

#ifdef MTK_SUPPORT_MJPEG
class MJpgSource : public MediaSource
{
    public:
    MJpgSource(const char* input_file, int32_t& status);
        
    virtual sp<MetaData> getFormat();       
    virtual status_t start(MetaData *params);
    virtual status_t stop();
    virtual status_t read(MediaBuffer **buffer, const MediaSource::ReadOptions *options);
    
    //bool cut(int64_t begin_ts, int64_t end_ts);
    //void getActualTimeBoundary(int64_t &begin_ts, int64_t &end_ts, int64_t& clip_duration);

    //int     mCodecConfig;

protected:
    virtual ~MJpgSource(); 

private:
    static const int mBufferSize = 1024*1024;
    static const int mEndFrameNo = 31;
    bool            mStart;
    sp<MetaData>    mMetaData;

    FILE            *pfFin;
    pthread_mutex_t  mFileLock;

    MediaBufferGroup mBufferGroup;
    int32_t mWidth, mHeight;
    int32_t mColorFormat;
    bool    mUseResizer;
    bool    mInitialRead;
    int64_t mTimeUs;
    int     mFrame;
    int     mReadNum;
    //int64_t mSourceDurationUs;
    //int64_t mBeginTs;
    //int64_t mEndTs;

    MJpgSource (const MJpgSource &);
    MJpgSource &operator=(const MJpgSource &);
};
#endif//MTK_SUPPORT_MJPEG

class OMXCodecTest : public RefBase
{
public:
    OMXCodecTest();
    OMXCodecTest(const char *szCompName, bool bUseNativeBuf = false);
    OMXCodecTest(const char *szCompName, int iMjpgContainer, int redundant);
    //int doTest();

    //settings
#ifdef USE_NATIVE_BUFFER
    int setUseNativeBuffer(bool setter);
#endif//USE_NATIVE_BUFFER
    int checkInitStat();

    //basic
    int StartAndStopTest();
    int GetFormatTest();
    int SetForceIFrameTest();
    int GetCameraMetaDateTest();
    //error handling
    int ReadBeforeStartTest();
    int ReadAfterStopTest();
    //trick play
    int TrickPlayTest();

    int mjpgContainer;

protected:
    virtual ~OMXCodecTest();

private:
    sp<IOMX>        mOMX;
    sp<MediaSource> mOmxCodec;
    sp<MediaSource> mSource;
    sp<MediaSource> mSeekSource;
    sp<MetaData>    mMetaData;
    bool            mIsEncoder;

    int             mWidth; //for encoder
    int             mHeight;//for encoder
static const int    mFps=30;
    //char            *mCompName;
    int             mInitStat;

    int initOMX();
    int initSource(const char *szName);
    int initMetaData(const char *szName);
    int init(const char *szCompName);
    bool checkNameForEncoder(const char *szCompName);
    sp<MediaSource> getSource(const char *szUri, const char *szMime);
#ifdef USE_NATIVE_BUFFER
    sp<SurfaceComposerClient>   mComposerClient;
    sp<SurfaceControl>          mSurfaceControl;
    sp<ANativeWindow>           mNativeWindow;
    bool                        mUseNativeBuffer;
#endif//USE_NATIVE_BUFFER

    OMXCodecTest(const OMXCodecTest &);
    OMXCodecTest &operator=(const OMXCodecTest &);
};

}  // namespace android

#endif  // OMXCODEC_TEST_H_
