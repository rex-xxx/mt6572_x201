
/*
 * Copyright 2006 The Android Open Source Project
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


#include "SkImageDecoder.h"
#include "SkBitmap.h"
#include "SkPixelRef.h"
#include "SkStream.h"
#include "SkTemplates.h"
#include "SkCanvas.h"
#include <stdio.h>

//#define MTK_75DISPLAY_ENHANCEMENT_SUPPORT
#ifdef MTK_75DISPLAY_ENHANCEMENT_SUPPORT
#include "MediaHal.h"
#endif

#ifdef MTK_89DISPLAY_ENHANCEMENT_SUPPORT
#include "DpBlitStream.h"
#endif

#include <cutils/properties.h>
#include <cutils/xlog.h>

#define LOG_TAG "skia"



SkVMMemoryReporter::~SkVMMemoryReporter() {
}

const char *SkImageDecoder::kFormatName[] = {
    "Unknown Format",
    "BMP",
    "GIF",
    "ICO",
    "JPEG",
    "PNG",
    "WBMP",
    "WEBP",
};

static SkBitmap::Config gDeviceConfig = SkBitmap::kNo_Config;

SkBitmap::Config SkImageDecoder::GetDeviceConfig()
{
    return gDeviceConfig;
}

void SkImageDecoder::SetDeviceConfig(SkBitmap::Config config)
{
    gDeviceConfig = config;
}

///////////////////////////////////////////////////////////////////////////////

SkImageDecoder::SkImageDecoder()
    : fReporter(NULL), fPeeker(NULL), fChooser(NULL), fAllocator(NULL),
      fSampleSize(1), fDefaultPref(SkBitmap::kNo_Config), fDitherImage(true),
      fUsePrefTable(false), fPreferSize(0), fPostProc(0),fPreferQualityOverSpeed(false) {
}

SkImageDecoder::~SkImageDecoder() {
    SkSafeUnref(fPeeker);
    SkSafeUnref(fChooser);
    SkSafeUnref(fAllocator);
    SkSafeUnref(fReporter);
}

SkImageDecoder::Format SkImageDecoder::getFormat() const {
    return kUnknown_Format;
}

SkImageDecoder::Peeker* SkImageDecoder::setPeeker(Peeker* peeker) {
    SkRefCnt_SafeAssign(fPeeker, peeker);
    return peeker;
}

SkImageDecoder::Chooser* SkImageDecoder::setChooser(Chooser* chooser) {
    SkRefCnt_SafeAssign(fChooser, chooser);
    return chooser;
}

SkBitmap::Allocator* SkImageDecoder::setAllocator(SkBitmap::Allocator* alloc) {
    SkRefCnt_SafeAssign(fAllocator, alloc);
    return alloc;
}

SkVMMemoryReporter* SkImageDecoder::setReporter(SkVMMemoryReporter* reporter) {
    SkRefCnt_SafeAssign(fReporter, reporter);
    return reporter;
}

void SkImageDecoder::setSampleSize(int size) {
    if (size < 1) {
        size = 1;
    }
    fSampleSize = size;
}

void SkImageDecoder::setPreferSize(int size) {
    if (size < 0) {
        size = 0;
    }
    fPreferSize = size;
}

void SkImageDecoder::setPostProcFlag(int flag) {

    fPostProc = flag;
}

bool SkImageDecoder::chooseFromOneChoice(SkBitmap::Config config, int width,
                                         int height) const {
    Chooser* chooser = fChooser;

    if (NULL == chooser) {    // no chooser, we just say YES to decoding :)
        return true;
    }
    chooser->begin(1);
    chooser->inspect(0, config, width, height);
    return chooser->choose() == 0;
}

bool SkImageDecoder::allocPixelRef(SkBitmap* bitmap,
                                   SkColorTable* ctable) const {
    return bitmap->allocPixels(fAllocator, ctable);
}

///////////////////////////////////////////////////////////////////////////////

void SkImageDecoder::setPrefConfigTable(const SkBitmap::Config pref[6]) {
    if (NULL == pref) {
        fUsePrefTable = false;
    } else {
        fUsePrefTable = true;
        memcpy(fPrefTable, pref, sizeof(fPrefTable));
    }
}

SkBitmap::Config SkImageDecoder::getPrefConfig(SrcDepth srcDepth,
                                               bool srcHasAlpha) const {
    SkBitmap::Config config;

    if (fUsePrefTable) {
        int index = 0;
        switch (srcDepth) {
            case kIndex_SrcDepth:
                index = 0;
                break;
            case k16Bit_SrcDepth:
                index = 2;
                break;
            case k32Bit_SrcDepth:
                index = 4;
                break;
        }
        if (srcHasAlpha) {
            index += 1;
        }
        config = fPrefTable[index];
    } else {
        config = fDefaultPref;
    }

    if (SkBitmap::kNo_Config == config) {
        config = SkImageDecoder::GetDeviceConfig();
    }
    return config;
}

#ifdef MTK_75DISPLAY_ENHANCEMENT_SUPPORT
void PostProcess(SkImageDecoder * decoder, SkBitmap* bm)
{
    unsigned long u4TimeOut = 0;
    unsigned long u4PQOpt;
    unsigned long u4Flag = 0;
    char value[PROPERTY_VALUE_MAX];
    property_get("persist.PQ", value, "1");

    u4PQOpt = atol(value);

    if((NULL == decoder) || (NULL == bm))
    {
        XLOGD("decoder or bm is null\n");
        return ;
    }

    if(0 == u4PQOpt)
    {
        return ;
    }

    u4Flag = decoder->getPostProcFlag();
    if(0 == (0x1 & u4Flag))
    {
        return ;
    }

    if((SkImageDecoder::kPNG_Format == decoder->getFormat()) && (0 == (u4Flag >> 4)))
    {
        return ;
    }

    bm->lockPixels();

    if(NULL == bm->getPixels())
    {
        XLOGD("bm does not get any pixels\n");
        goto NULL_EXIT;
    }

    if((bm->config() == SkBitmap::kARGB_8888_Config) || 
       (bm->config() == SkBitmap::kRGB_565_Config))
    {
        mHalBltParam_t bltParam;

        memset(&bltParam, 0, sizeof(bltParam));
        
        switch(bm->config())
        {
            case SkBitmap::kARGB_8888_Config:
                bltParam.srcFormat = MHAL_FORMAT_ABGR_8888;
                bltParam.dstFormat = MHAL_FORMAT_ABGR_8888;
                break;
/*
            case SkBitmap::kYUY2_Pack_Config:
                bltParam.srcFormat = MHAL_FORMAT_YUY2;
                bltParam.dstFormat = MHAL_FORMAT_YUY2;
                break;

            case SkBitmap::kUYVY_Pack_Config:
                bltParam.srcFormat = MHAL_FORMAT_UYVY;
                bltParam.dstFormat = MHAL_FORMAT_UYVY;
                break;
*/
            case SkBitmap::kRGB_565_Config:
                bltParam.srcFormat = MHAL_FORMAT_RGB_565;
                bltParam.dstFormat = MHAL_FORMAT_RGB_565;
                break;
            default :
                goto NULL_EXIT;
            break;
        }

        bltParam.doImageProcess = ((SkImageDecoder::kJPEG_Format == decoder->getFormat() ? 1 : 2) + 0x10);
        bltParam.orientation = MHAL_BITBLT_ROT_0;
        bltParam.srcAddr = bltParam.dstAddr = (unsigned int)bm->getPixels();
        bltParam.srcX = bltParam.srcY = 0;

        if(2 == u4PQOpt)
        {
//Debug and demo mode
            bltParam.srcWStride = bm->width();
            bltParam.srcW = bltParam.dstW = (bm->width() >> 1);
        }
        else
        {
//Normal mode
            bltParam.srcW = bltParam.srcWStride = bltParam.dstW = bm->width();
        }

        bltParam.srcH = bltParam.srcHStride = bltParam.dstH = bm->height();
        bltParam.pitch = bm->rowBytesAsPixels();//bm->width();

        XLOGD("Image Processing\n");

        // start image process
        while(MHAL_NO_ERROR != mHalMdpIpc_BitBlt(&bltParam))
        {
            if(10 < u4TimeOut)
            {
                break;
            }
            else
            {
                u4TimeOut += 1;
                XLOGD("Image Process retry : %d\n" , u4TimeOut);
            }
        }

        if(10 < u4TimeOut)
        {
            XLOGD("Image Process is skipped\n");
        }
        else
        {
            XLOGD("Image Process Done\n");
        }
    }

NULL_EXIT:
    bm->unlockPixels();

    return ;

}
#endif

#ifdef MTK_89DISPLAY_ENHANCEMENT_SUPPORT
void PostProcess(SkImageDecoder * decoder, SkBitmap* bm)
{
    unsigned long u4TimeOut = 0;
    unsigned long u4PQOpt;
    unsigned long u4Flag = 0;
    unsigned long *u4RGB888pointer;
    unsigned int  *u4RGB565pointer;
    unsigned long bkPoints[8];
    
    char value[PROPERTY_VALUE_MAX];
    property_get("persist.PQ", value, "1");

    u4PQOpt = atol(value);

    if((NULL == decoder) || (NULL == bm))
    {
        XLOGD("decoder or bm is null\n");
        return ;
    }


    if(0 == u4PQOpt)
    {
        return ;
    }

    u4Flag = decoder->getPostProcFlag();
    if(0 == (0x1 & u4Flag))
    {
        XLOGD("Flag is not 1%x" , u4Flag);
        return ;
    }

    if((SkImageDecoder::kPNG_Format == decoder->getFormat()) && (0 == (u4Flag >> 4)))
    {
        XLOGD("PNG , and flag does not force to do PQ Sharpeness %x" , u4Flag);
        return ;
    }
    
    if(bm->width()>1280)
    {
        XLOGD("Cannot support PQ Sharpeness when picture width %d > 1280 \n" , bm->width());
        return ;
    }

    bm->lockPixels();

    if(NULL == bm->getPixels())
    {
        XLOGD("bm does not get any pixels\n");
        goto NULL_EXIT;
    }

    if((bm->config() == SkBitmap::kARGB_8888_Config) || 
       (bm->config() == SkBitmap::kRGB_565_Config))
    {
        
        DpColorFormat fmt;
        
        switch(bm->config())
        {
            case SkBitmap::kARGB_8888_Config:
                fmt = eARGB8888;
                if(bm->width()%2==1)
                {     
                   u4RGB888pointer = (unsigned long *)bm->getPixels();
                   bkPoints[0] = *(u4RGB888pointer + (bm->width()*bm->height()-1));
                   bkPoints[1] = *(u4RGB888pointer + (bm->width()*bm->height()-2));
                   bkPoints[2] = *(u4RGB888pointer + (bm->width()*bm->height()-3));
                   bkPoints[3] = *(u4RGB888pointer + (bm->width()*bm->height()-4));
                   bkPoints[4] = *(u4RGB888pointer + (bm->width()*(bm->height()-1)-1));
                   bkPoints[5] = *(u4RGB888pointer + (bm->width()*(bm->height()-1)-2));
                   bkPoints[6] = *(u4RGB888pointer + (bm->width()*(bm->height()-1)-3));
                   bkPoints[7] = *(u4RGB888pointer + (bm->width()*(bm->height()-1)-4));
                }
                break;
/*
            case SkBitmap::kYUY2_Pack_Config:
                bltParam.srcFormat = MHAL_FORMAT_YUY2;
                bltParam.dstFormat = MHAL_FORMAT_YUY2;
                break;

            case SkBitmap::kUYVY_Pack_Config:
                bltParam.srcFormat = MHAL_FORMAT_UYVY;
                bltParam.dstFormat = MHAL_FORMAT_UYVY;
                break;
*/
            case SkBitmap::kRGB_565_Config:
                fmt = eRGB565;
                if(bm->width()%2==1)
                {     
                    u4RGB565pointer = (unsigned int *)bm->getPixels();
                    bkPoints[0] = (unsigned long)*(u4RGB565pointer + (bm->width()*bm->height()-1));
                    bkPoints[1] = (unsigned long)*(u4RGB565pointer + (bm->width()*bm->height()-2));
                    bkPoints[2] = (unsigned long)*(u4RGB565pointer + (bm->width()*bm->height()-3));
                    bkPoints[3] = (unsigned long)*(u4RGB565pointer + (bm->width()*bm->height()-4));
                    bkPoints[4] = (unsigned long)*(u4RGB565pointer + (bm->width()*(bm->height()-1)-1));
                    bkPoints[5] = (unsigned long)*(u4RGB565pointer + (bm->width()*(bm->height()-1)-2));
                    bkPoints[6] = (unsigned long)*(u4RGB565pointer + (bm->width()*(bm->height()-1)-3));
                    bkPoints[7] = (unsigned long)*(u4RGB565pointer + (bm->width()*(bm->height()-1)-4));
                }  
                break;
            default :
                goto NULL_EXIT;
            break;
        }

        XLOGD("Image Processing %d %d\n",bm->width(),bm->height());
        
        ////////////////////////////////////////////////////////////////////
        
        if(5 == u4PQOpt)
        {                    
        	XLOGE("Output Pre-EE Result...\n");       
        	FILE *fp;          
        	fp = fopen("/sdcard/testOri.888", "w");
        	
        	if(fp!=NULL)
        		fwrite(bm->getPixels(),1,bm->getSize(),fp);
        	else
        		XLOGE("Output Pre-EE Result fail !\n");
        		
        	fclose(fp);
        	XLOGE("Output Pre-EE Result Done!\n");
      	}
        
        DpBlitStream* stream = 0;
    
        stream = new DpBlitStream();

        stream->setSrcBuffer(bm->getPixels(), bm->getSize());
        stream->setSrcConfig(bm->width(), bm->height(), fmt);
        stream->setDstBuffer(bm->getPixels(), bm->getSize());
        stream->setDstConfig(bm->width(), bm->height(), fmt);

        stream->setRotate(0);
        stream->setTdshp(1);
        
        if(!stream->invalidate())
        {
            XLOGE("TDSHP Bitblt Stream Failed!\n");
        }
        
        if((bm->width()%2==1) && fmt == eARGB8888)
        {
            XLOGD("6589 PQ EE Workaround at odd width picture\n");
            *(u4RGB888pointer + (bm->width()*bm->height()-1))      = bkPoints[0];
            *(u4RGB888pointer + (bm->width()*bm->height()-2))      = bkPoints[1];
            *(u4RGB888pointer + (bm->width()*bm->height()-3))      = bkPoints[2];
            *(u4RGB888pointer + (bm->width()*bm->height()-4))      = bkPoints[3];
            *(u4RGB888pointer + (bm->width()*(bm->height()-1)-1))  = bkPoints[4];
            *(u4RGB888pointer + (bm->width()*(bm->height()-1)-2))  = bkPoints[5];
            *(u4RGB888pointer + (bm->width()*(bm->height()-1)-3))  = bkPoints[6];
            *(u4RGB888pointer + (bm->width()*(bm->height()-1)-4))  = bkPoints[7];
        }                                                         
        else if((bm->width()%2==1) && fmt == eRGB565)
        {          
            XLOGD("6589 PQ EE Workaround at odd width picture\n");
            *(u4RGB565pointer + (bm->width()*bm->height()-1))      = bkPoints[0];
            *(u4RGB565pointer + (bm->width()*bm->height()-2))      = bkPoints[1];
            *(u4RGB565pointer + (bm->width()*bm->height()-3))      = bkPoints[2];
            *(u4RGB565pointer + (bm->width()*bm->height()-4))      = bkPoints[3];
            *(u4RGB565pointer + (bm->width()*(bm->height()-1)-1))  = bkPoints[4];
            *(u4RGB565pointer + (bm->width()*(bm->height()-1)-2))  = bkPoints[5];
            *(u4RGB565pointer + (bm->width()*(bm->height()-1)-3))  = bkPoints[6];
            *(u4RGB565pointer + (bm->width()*(bm->height()-1)-4))  = bkPoints[7];
        }   
            
            
        if(5 == u4PQOpt)
        {                    
        	XLOGE("Output EE Result...\n");       
        	FILE *fp;          
        	fp = fopen("/sdcard/test.888", "w");
        	
        	if(fp!=NULL)
        		fwrite(bm->getPixels(),1,bm->getSize(),fp);
        	else
        		XLOGE("Output EE Result fail !\n");
        		
        	fclose(fp);
        	XLOGE("Output EE Result Done!\n");
      	}

        delete stream;

    }

    XLOGD("Image Process Done\n");

NULL_EXIT:
    bm->unlockPixels();

    return ;
}
#endif

bool SkImageDecoder::decode(SkStream* stream, SkBitmap* bm,
                            SkBitmap::Config pref, Mode mode, bool reuseBitmap) {
    // pass a temporary bitmap, so that if we return false, we are assured of
    // leaving the caller's bitmap untouched.
    SkBitmap    tmp;

    // we reset this to false before calling onDecode
    fShouldCancelDecode = false;
    // assign this, for use by getPrefConfig(), in case fUsePrefTable is false
    fDefaultPref = pref;

    if (reuseBitmap) {
        SkAutoLockPixels alp(*bm);
        if (bm->getPixels() != NULL) {
            return this->onDecode(stream, bm, mode);
        }
    }
    if (!this->onDecode(stream, &tmp, mode)) {
        return false;
    }

#ifdef MTK_75DISPLAY_ENHANCEMENT_SUPPORT
    PostProcess(this , &tmp);
#endif

#ifdef MTK_89DISPLAY_ENHANCEMENT_SUPPORT
    PostProcess(this , &tmp);
#endif

    bm->swap(tmp);
    return true;
}

bool SkImageDecoder::decodeRegion(SkBitmap* bm, SkIRect rect,
                                  SkBitmap::Config pref) {
    // we reset this to false before calling onDecodeRegion
    fShouldCancelDecode = false;
    // assign this, for use by getPrefConfig(), in case fUsePrefTable is false
    fDefaultPref = pref;

    if (!this->onDecodeRegion(bm, rect)) {
        return false;
    }

#ifdef MTK_75DISPLAY_ENHANCEMENT_SUPPORT
    PostProcess(this , bm);
#endif

#ifdef MTK_89DISPLAY_ENHANCEMENT_SUPPORT
    PostProcess(this , bm);
#endif
    
    return true;
}

bool SkImageDecoder::buildTileIndex(SkStream* stream,
                                int *width, int *height) {
    // we reset this to false before calling onBuildTileIndex
    fShouldCancelDecode = false;

    return this->onBuildTileIndex(stream, width, height);
}

void SkImageDecoder::cropBitmap(SkBitmap *dest, SkBitmap *src,
                                    int sampleSize, int destX, int destY,
                                    int width, int height, int srcX, int srcY) {
    int w = width / sampleSize;
    int h = height / sampleSize;
    // if the destination has no pixels then we must allocate them.
    if (dest->isNull()) {
        dest->setConfig(src->getConfig(), w, h);
        dest->setIsOpaque(src->isOpaque());

        if (!this->allocPixelRef(dest, NULL)) {
            XLOGW("Skia::cropBitmap allocPixelRef FAIL W %d, H %d!!!!!!\n", w, h);
            SkDEBUGF(("failed to allocate pixels needed to crop the bitmap"));
            return;
        }
    }
    // check to see if the destination is large enough to decode the desired
    // region. If this assert fails we will just draw as much of the source
    // into the destination that we can.
    SkASSERT(dest->width() >= w && dest->height() >= h);

    // Set the Src_Mode for the paint to prevent transparency issue in the
    // dest in the event that the dest was being re-used.
    SkPaint paint;
    paint.setXfermodeMode(SkXfermode::kSrc_Mode);

    SkCanvas canvas(*dest);
    canvas.drawSprite(*src, (srcX - destX) / sampleSize,
                            (srcY - destY) / sampleSize,
                            &paint);
}

///////////////////////////////////////////////////////////////////////////////

bool SkImageDecoder::DecodeFile(const char file[], SkBitmap* bm,
                            SkBitmap::Config pref,  Mode mode, Format* format) {
    SkASSERT(file);
    SkASSERT(bm);

    SkFILEStream    stream(file);
    if (stream.isValid()) {
        if (SkImageDecoder::DecodeStream(&stream, bm, pref, mode, format)) {
            bm->pixelRef()->setURI(file);
            return true;
        }
    }
    return false;
}

bool SkImageDecoder::DecodeMemory(const void* buffer, size_t size, SkBitmap* bm,
                          SkBitmap::Config pref, Mode mode, Format* format) {
    if (0 == size) {
        return false;
    }
    SkASSERT(buffer);

    SkMemoryStream  stream(buffer, size);
    return SkImageDecoder::DecodeStream(&stream, bm, pref, mode, format);
}

bool SkImageDecoder::DecodeStream(SkStream* stream, SkBitmap* bm,
                          SkBitmap::Config pref, Mode mode, Format* format) {
    SkASSERT(stream);
    SkASSERT(bm);

    bool success = false;
    SkImageDecoder* codec = SkImageDecoder::Factory(stream);

    if (NULL != codec) {
        success = codec->decode(stream, bm, pref, mode);
        if (success && format) {
            *format = codec->getFormat();
        }
        delete codec;
    }
    return success;
}
