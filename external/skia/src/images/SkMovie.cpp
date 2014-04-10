
/*
 * Copyright 2011 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
#include "SkMovie.h"
#include "SkCanvas.h"
#include "SkPaint.h"

#include "utils/Log.h"
// We should never see this in normal operation since our time values are
// 0-based. So we use it as a sentinal.
#define UNINITIALIZED_MSEC ((SkMSec)-1)

SkMovie::SkMovie()
{
    fInfo.fDuration = UNINITIALIZED_MSEC;  // uninitialized
    fCurrTime = UNINITIALIZED_MSEC; // uninitialized
    fNeedBitmap = true;
}

void SkMovie::ensureInfo()
{
    if (fInfo.fDuration == UNINITIALIZED_MSEC && !this->onGetInfo(&fInfo))
        memset(&fInfo, 0, sizeof(fInfo));   // failure
}

SkMSec SkMovie::duration()
{
    this->ensureInfo();
    return fInfo.fDuration;
}

int SkMovie::width()
{
    this->ensureInfo();
    return fInfo.fWidth;
}

int SkMovie::height()
{
    this->ensureInfo();
    return fInfo.fHeight;
}

int SkMovie::isOpaque()
{
    this->ensureInfo();
    return fInfo.fIsOpaque;
}

bool SkMovie::setTime(SkMSec time)
{
    SkMSec dur = this->duration();
    if (time > dur)
        time = dur;
        
    bool changed = false;
    if (time != fCurrTime)
    {
        fCurrTime = time;
        changed = this->onSetTime(time);
        fNeedBitmap |= changed;
    }
    return changed;
}

//for add gif begin
int SkMovie::getGifFrameDuration(int frameIndex)
{
    return 0;
}

int SkMovie::getGifTotalFrameCount()
{
    return 0;
}

bool SkMovie::setCurrFrame(int frameIndex)
{
    return true;
}

SkBitmap* SkMovie::createGifFrameBitmap()
{
    //get default bitmap, create a new bitmap and returns.
    SkBitmap *copyedBitmap = new SkBitmap();
    bool copyDone = false;
    if (fNeedBitmap)
    {
        if (!this->onGetBitmap(&fBitmap))   // failure
            fBitmap.reset();
    }
    //now create a new bitmap from fBitmap
    if (fBitmap.canCopyTo(SkBitmap::kARGB_8888_Config) )
    {
//LOGE("SkMovie:createGifFrameBitmap:fBitmap can copy to 8888 config, then copy...");
        copyDone = fBitmap.copyTo(copyedBitmap, SkBitmap::kARGB_8888_Config);
    }
    else
    {
        copyDone = false;
//LOGE("SkMovie:createGifFrameBitmap:fBitmap can NOT copy to 8888 config");
    }

    if (copyDone)
    {
        return copyedBitmap;
    }
    else
    {
        return NULL;
    }
}
//for add gif end

const SkBitmap& SkMovie::bitmap()
{
    if (fCurrTime == UNINITIALIZED_MSEC)    // uninitialized
        this->setTime(0);

    if (fNeedBitmap)
    {
        if (!this->onGetBitmap(&fBitmap))   // failure
            fBitmap.reset();
        fNeedBitmap = false;
    }
    return fBitmap;
}

////////////////////////////////////////////////////////////////////

#include "SkStream.h"

SkMovie* SkMovie::DecodeMemory(const void* data, size_t length) {
    SkMemoryStream stream(data, length, false);
    return SkMovie::DecodeStream(&stream);
}

SkMovie* SkMovie::DecodeFile(const char path[])
{
    SkMovie* movie = NULL;

    SkFILEStream stream(path);
    if (stream.isValid()) {
        movie = SkMovie::DecodeStream(&stream);
    }
#ifdef SK_DEBUG
    else {
        SkDebugf("Movie file not found <%s>\n", path);
    }
#endif

    return movie;
}

