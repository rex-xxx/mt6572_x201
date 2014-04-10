
/*
 * Copyright 2007 The Android Open Source Project
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


#include "SkImageDecoder.h"
#include "SkImageEncoder.h"
#include "SkJpegUtility.h"
#include "SkColorPriv.h"
#include "SkDither.h"
#include "SkScaledBitmapSampler.h"
#include "SkStream.h"
#include "SkTemplates.h"
#include "SkUtils.h"
#include "SkRect.h"
#include "SkCanvas.h"

#include <sys/mman.h>
#include <cutils/ashmem.h>

#ifdef USE_MTK_ALMK
  #include "almk_hal.h"
#endif 
#ifdef MTK_JPEG_HW_DECODER
#include "MediaHal.h"
#include "DpBlitStream.h" 
#define ATRACE_TAG ATRACE_TAG_GRAPHICS
#include "Trace.h"
#endif
#include <stdio.h>
extern "C" {
    #include "jpeglib.h"
    #include "jerror.h"
}

#ifdef SK_BUILD_FOR_ANDROID
#include <cutils/properties.h>
#include <cutils/xlog.h>

#undef LOG_TAG
#define LOG_TAG "skia" 
// Key to lookup the size of memory buffer set in system property
static const char KEY_MEM_CAP[] = "ro.media.dec.jpeg.memcap";
#endif

// this enables timing code to report milliseconds for an encode
//#define TIME_ENCODE
//#define TIME_DECODE

// this enables our rgb->yuv code, which is faster than libjpeg on ARM
// disable for the moment, as we have some glitches when width != multiple of 4
#define WE_CONVERT_TO_YUV
// the avoid the reset error when switch hardware to software codec
#define MAX_HEADER_SIZE 64 * 1024
// the limitation of memory to use hardware resize
#define HW_RESIZE_MAX_PIXELS 25 * 1024 * 1024
//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

#define CHECK_LARGE_JPEG_PROG
#define JPEG_PROG_LIMITATION_SIZE MTK_MAX_SRC_JPEG_PROG_PIXELS

#define USE_SKJPGSTREAM 



#ifdef USE_SKJPGSTREAM

class SkJpgStream : public SkStream {

public:


	SkJpgStream(void *hw_buffer, size_t hw_buffer_size, SkStream* Src){
	  XLOGD("SkJpgStream::SkJpgStream %x, %x, %x!!\n", (unsigned int) hw_buffer, hw_buffer_size, (unsigned int)Src);
      srcStream = Src ;
	  hwInputBuf = hw_buffer;
	  hwInputBufSize = hw_buffer_size;
	  total_read_size = 0;
	  
    }

	virtual ~SkJpgStream(){
	  //SkDebugf("SkJpgStream::~SkJpgStream!!\n");		
	}

	
    virtual bool rewind(){
	  //SkDebugf("SkJpgStream::rewind, readSize %x, hwBuffSize %x!!\n",   total_read_size, hwInputBufSize);				
	  if(total_read_size >= hwInputBufSize){
		return false;
	  }else if (total_read_size < hwInputBufSize){
        total_read_size = 0;
	  }
      return true ;
    }
	
	virtual size_t read(void* buffer, size_t size){
		size_t read_start = total_read_size;
		size_t read_end = total_read_size + size ;
		size_t size_1 ;
		size_t size_2 ;
		size_t real_size_2 ;

		//SkDebugf("SkJpgStream::read, buf %x, size %x, tSize %x, st %x, end %x, HWsize %x!!\n", (unsigned int)buffer, (unsigned int) size
		//	, total_read_size, read_start, read_end, hwInputBufSize);			   

		if (buffer == NULL && size == 0){	// special signature, they want the total size
		    fSize = hwInputBufSize + srcStream->getLength();
			return fSize;
		}else if(size == 0){
          return 0;
		}

		// if buffer is NULL, seek ahead by size

        if( read_start <= hwInputBufSize && read_end <= hwInputBufSize){
		  if (buffer) {
            memcpy(buffer, (const char*)hwInputBuf + read_start, size);
		  }

		    total_read_size += size ;
		    //SkDebugf("SkJpgStream::read(HW), size %x, total_size %x!!\n", size, total_read_size);			   		  					  			
		    return size ;					  

		}else if ( read_start >= hwInputBufSize  ){
          real_size_2 = srcStream->read(buffer, size);
		  total_read_size += real_size_2 ;
		  //SkDebugf("SkJpgStream::read(Stream), size_2 %x, real_size %x(%x), total_size %x!!\n", size, real_size_2, srcStream->getLength(),total_read_size);
          return real_size_2;
		}else{
          size_1 = hwInputBufSize - read_start ;
          size_2 = read_end - hwInputBufSize  ;	
		  if (buffer) {
            memcpy(buffer, (const char*)hwInputBuf + read_start, size_1);
		  }
		  total_read_size += size_1 ;
		  real_size_2 = srcStream->read(buffer+size_1, size_2);		  
		  total_read_size += real_size_2 ;
		  //SkDebugf("SkJpgStream::read(HW+Stream), buf %x, size_2 %x, real_size %x(%x), total_size %x!!\n", buffer+size_1, size_2, real_size_2, srcStream->getLength(),total_read_size);  
		  return size_1+ real_size_2 ;		

		}



	}

	size_t seek(size_t offset){
	    //SkDebugf("SkJpgStream::seek size %x!!\n", offset);			
		return 0;
	}
	size_t skip(size_t size)
	{
		/*	Check for size == 0, and just return 0. If we passed that
			to read(), it would interpret it as a request for the entire
			size of the stream.
		*/
        //SkDebugf("SkJpgStream::skip %x!!\n", size);					
		return size ? this->read(NULL, size) : 0;
	}




private:	
  size_t total_read_size ;
  SkStream* srcStream;
  void *hwInputBuf ;
  size_t hwInputBufSize ; 
  size_t fSize;
  



};



class JpgStreamAutoClean {
public:
    JpgStreamAutoClean(): ptr(NULL) {}
    ~JpgStreamAutoClean() {
        if (ptr) {
            delete ptr;
        }
    }
    void set(SkStream* s) {
        ptr = s;
    }
private:
    SkStream* ptr;
};

#endif

class SkJPEGImageIndex {
public:
    SkJPEGImageIndex() {}
    virtual ~SkJPEGImageIndex() {
        jpeg_destroy_huffman_index(index);
        jpeg_finish_decompress(cinfo);
        jpeg_destroy_decompress(cinfo);
        delete cinfo->src;
        free(cinfo);
    }
    jpeg_decompress_struct *cinfo;
    huffman_index *index;
};


class SkJPEGImageDecoder : public SkImageDecoder {
public:
    SkJPEGImageDecoder() {
        index = NULL;
    }
    ~SkJPEGImageDecoder() {
        if (index)
            delete index;
    }
    virtual Format getFormat() const {
        return kJPEG_Format;
    }
protected:
    virtual bool onBuildTileIndex(SkStream *stream,
                                int *width, int *height);
    virtual bool onDecodeRegion(SkBitmap* bitmap, SkIRect rect);
    virtual bool onDecode(SkStream* stream, SkBitmap* bm, Mode);
private:
    SkJPEGImageIndex *index;
    int imageWidth;
    int imageHeight;

#ifdef MTK_JPEG_HW_DECODER
    SkAutoMalloc  fAllocator;
    uint8_t* fSrc;
    uint32_t fSize;
    bool fInitRegion;
    bool fFirstTileDone;
    bool fUseHWResizer;
    bool onDecodeParser(SkBitmap* bm, Mode, uint8_t* srcBuffer, uint32_t srcSize, int srcFD);
    bool onRangeDecodeParser(uint8_t* srcBuffer, uint32_t srcSize, int *width, int *height, bool doRelease);
    bool onDecodeHW(SkBitmap* bm, uint8_t* srcBuffer, uint32_t srcBufSize, uint32_t srcSize, int srcFD);
    bool onDecodeHWRegion(SkBitmap* bm, SkIRect region, uint8_t* srcBuffer, uint32_t srcSize);
    void *fSkJpegDecHandle ;
#endif

};

//////////////////////////////////////////////////////////////////////////

#include "SkTime.h"

class AutoTimeMillis {
public:
    AutoTimeMillis(const char label[]) : fLabel(label) {
        if (!fLabel) {
            fLabel = "";
        }
        fNow = SkTime::GetMSecs();
    }
    ~AutoTimeMillis() {
        SkDebugf("---- Time (ms): %s %d\n", fLabel, SkTime::GetMSecs() - fNow);
    }
private:
    const char* fLabel;
    SkMSec      fNow;
};

/* Automatically clean up after throwing an exception */
class JPEGAutoClean {
public:
    JPEGAutoClean(): cinfo_ptr(NULL) {}
    ~JPEGAutoClean() {
        if (cinfo_ptr) {
            jpeg_destroy_decompress(cinfo_ptr);
        }
    }
    void set(jpeg_decompress_struct* info) {
        cinfo_ptr = info;
    }
private:
    jpeg_decompress_struct* cinfo_ptr;
};

#ifdef SK_BUILD_FOR_ANDROID
/* Check if the memory cap property is set.
   If so, use the memory size for jpeg decode.
*/
static void overwrite_mem_buffer_size(j_decompress_ptr cinfo) {
#ifdef ANDROID_LARGE_MEMORY_DEVICE
    cinfo->mem->max_memory_to_use = 30 * 1024 * 1024;
#else
    cinfo->mem->max_memory_to_use = 5 * 1024 * 1024;
#endif
}
#endif


///////////////////////////////////////////////////////////////////////////////

/*  If we need to better match the request, we might examine the image and
     output dimensions, and determine if the downsampling jpeg provided is
     not sufficient. If so, we can recompute a modified sampleSize value to
     make up the difference.

     To skip this additional scaling, just set sampleSize = 1; below.
 */
static int recompute_sampleSize(int sampleSize,
                                const jpeg_decompress_struct& cinfo) {
    return sampleSize * cinfo.output_width / cinfo.image_width;
}

static bool valid_output_dimensions(const jpeg_decompress_struct& cinfo) {
    /* These are initialized to 0, so if they have non-zero values, we assume
       they are "valid" (i.e. have been computed by libjpeg)
     */
    return cinfo.output_width != 0 && cinfo.output_height != 0;
}

static bool skip_src_rows(jpeg_decompress_struct* cinfo, void* buffer,
                          int count) {
    for (int i = 0; i < count; i++) {
        JSAMPLE* rowptr = (JSAMPLE*)buffer;
        int row_count = jpeg_read_scanlines(cinfo, &rowptr, 1);
        if (row_count != 1) {
            return false;
        }
    }
    return true;
}

static bool skip_src_rows_tile(jpeg_decompress_struct* cinfo,
                          huffman_index *index, void* buffer,
                          int count) {
    for (int i = 0; i < count; i++) {
        JSAMPLE* rowptr = (JSAMPLE*)buffer;
        int row_count = jpeg_read_tile_scanline(cinfo, index, &rowptr);
        if (row_count != 1) {
            return false;
        }
    }
    return true;
}

#ifdef MTK_JPEG_HW_DECODER

static int roundToTwoPower(int a)
{
    int ans = 1;

    if(a>=8) return a; 

    while (a > 0)
    {
        a = a >> 1;
        ans *= 2;
    }

    return (ans >> 1);
}

static int skjdiv_round_up (unsigned int a, unsigned int b)/* Compute a/b rounded up to next integer, ie, ceil(a/b) *//* Assumes a >= 0, b > 0 */
{  
   return (a + b - 1) / b;
}



bool SkJPEGImageDecoder::onDecodeParser(SkBitmap* bm, Mode mode, uint8_t* srcBuffer, uint32_t srcSize, int srcFD)
{
    int width, height;
    int sampleSize = this->getSampleSize();
    int preferSize = 0;    //this->getPreferSize();
    MHAL_JPEG_DEC_INFO_OUT  outInfo;
    MHAL_JPEG_DEC_SRC_IN    srcInfo;

    unsigned int cinfo_output_width, cinfo_output_height;
	int re_sampleSize ;
    sampleSize = roundToTwoPower(sampleSize);
    fSkJpegDecHandle = srcInfo.jpgDecHandle = NULL;
     
    
    SkBitmap::Config config = this->getPrefConfig(k32Bit_SrcDepth, false);
    // only these make sense for jpegs
    if (config != SkBitmap::kARGB_8888_Config &&
        config != SkBitmap::kARGB_4444_Config &&
        //config != SkBitmap::kYUY2_Pack_Config &&
        //config != SkBitmap::kUYVY_Pack_Config &&
        config != SkBitmap::kRGB_565_Config) {
        config = SkBitmap::kARGB_8888_Config;
    }

    if (config != SkBitmap::kARGB_8888_Config &&
        //config != SkBitmap::kYUY2_Pack_Config &&
        //config != SkBitmap::kUYVY_Pack_Config &&
        config != SkBitmap::kRGB_565_Config) {
        XLOGW("HW Not support format: %d\n", config);
        return false;
    }
     

    int result ;
    int try_times = 5;
    // parse the file
    do
    {
        try_times++;
        srcInfo.srcBuffer = srcBuffer;
        srcInfo.srcLength = srcSize;
        srcInfo.srcFD = srcFD;
        result = mHalJpeg(MHAL_IOCTL_JPEG_DEC_PARSER, (void *)&srcInfo, sizeof(srcInfo), NULL, 0, NULL);
        if(result == MHAL_INVALID_RESOURCE && try_times < 5)
        {
            XLOGD("onDecodeParser : HW busy ! Sleep 10ms & try again");
            usleep(10 * 1000);
        }
        else if (MHAL_NO_ERROR != result)
        {
            return false;
        }
    } while(result == MHAL_INVALID_RESOURCE && try_times < 5);
    fSkJpegDecHandle = srcInfo.jpgDecHandle ;

    // get file dimension
    outInfo.jpgDecHandle = fSkJpegDecHandle;
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_GET_INFO, NULL, 0, 
                                   (void *)&outInfo, sizeof(outInfo), NULL))
    {
        if (SkImageDecoder::kDecodeBounds_Mode != mode) {
            XLOGW("mHalJpeg() - JPEG Decoder false get information !!\n");
        }
        return false;
    }

    if (preferSize == 0)
    {
	    if(sampleSize <= 8 ) // 1, 2, 4, 8
		{
			cinfo_output_width = skjdiv_round_up(outInfo.srcWidth, sampleSize);
			cinfo_output_height = skjdiv_round_up(outInfo.srcHeight, sampleSize);

		}
		else //  use 8
		{
            cinfo_output_width = skjdiv_round_up(outInfo.srcWidth, 8);
            cinfo_output_height = skjdiv_round_up(outInfo.srcHeight, 8);
		}
		
		re_sampleSize = sampleSize * cinfo_output_width / outInfo.srcWidth;

     

    if( re_sampleSize != 1 )
		{
          int dx = (re_sampleSize > cinfo_output_width )? cinfo_output_width : re_sampleSize ;
          int dy = (re_sampleSize > cinfo_output_height )? cinfo_output_height : re_sampleSize ;


	
          width  = cinfo_output_width / dx;  
          height = cinfo_output_height / dy; 
        

		}
		else
		{
		  width = cinfo_output_width ;
		  height = cinfo_output_height ;

		}
	
		
#if 0
        width  = outInfo.srcWidth / sampleSize;
        height = outInfo.srcHeight / sampleSize;
        if(outInfo.srcWidth % sampleSize != 0) width++;
        if(outInfo.srcHeight % sampleSize != 0) height++;
#endif		
    }
    else
    {
        int maxDimension = (outInfo.srcWidth > outInfo.srcHeight) ? outInfo.srcWidth : outInfo.srcHeight;
        
        if (maxDimension <= preferSize)
        {
            width  = outInfo.srcWidth / sampleSize;
            height = outInfo.srcHeight / sampleSize;
        }
        else if (outInfo.srcWidth > outInfo.srcHeight)
        {
            width = preferSize;
            height = (int)outInfo.srcHeight * width / (int)outInfo.srcWidth;
        }
        else
        {
            height = preferSize;
            width = (int)outInfo.srcWidth * height / (int)outInfo.srcHeight;
        }
    }    
	if( re_sampleSize != 1  )
    XLOGD("onDecodeParser pSize %d, src %d %d, dst %d %d(%d %d), sample %d->%d!\n", preferSize, outInfo.srcWidth, outInfo.srcHeight,
		width, height,cinfo_output_width, cinfo_output_height, sampleSize, re_sampleSize);	

     

    bm->lockPixels();
    void* rowptr = bm->getPixels();
    bm->unlockPixels();
    bool reuseBitmap = (rowptr != NULL);


    if(reuseBitmap)
    {
        if((bm->width() != width) || (bm->height() != height) || (bm->config() != config))
        {
            if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
            {
                XLOGW("Can not release JPEG HW Decoder\n");
                return false;
            }
            XLOGW("Reuse bitmap but dimensions not match\n");
            return false;            
        }
    }
    else 
    {
        bm->setConfig(config, width, height);
        bm->setIsOpaque(true);
    }
    
    if (SkImageDecoder::kDecodeBounds_Mode == mode) {
        if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
        {
            XLOGW("Can not release JPEG HW Decoder\n");
            return false;
        }
        return true;    
    //} else if(width <= 128 && height <= 128) {
    //    mHalJpeg(MHAL_IOCTL_JPEG_DEC_START, NULL, 0, NULL, 0, NULL);
    //    return false;
    }

    XLOGD("The file input width: %d, height: %d, output width: %d, height: %d, format: %d, prefer size: %d, dither: %d\n", 
           outInfo.srcWidth, outInfo.srcHeight, width, height, config, preferSize, getDitherImage());

    return true;
}

bool SkJPEGImageDecoder::onRangeDecodeParser(uint8_t* srcBuffer, uint32_t srcSize, int *width, int *height, bool doRelease)
{
    MHAL_JPEG_DEC_INFO_OUT outInfo;
    int result ;
    int try_times = 0;
    // parse the file
    do
    {
        try_times++;
        result = mHalJpeg(MHAL_IOCTL_JPEG_DEC_PARSER, (void *)srcBuffer, srcSize, NULL, 0, NULL);
        if(result == MHAL_INVALID_RESOURCE && try_times < 5)
        {
            XLOGD("onRangeDecodeParser : HW busy ! Sleep 100ms & try again");
            usleep(100 * 1000);
        }
        else if (MHAL_NO_ERROR != result)
        {
            return false;
        }
    } while(result == MHAL_INVALID_RESOURCE && try_times < 5);

    // get file dimension
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_GET_INFO, NULL, 0, 
                                   (void *)&outInfo, sizeof(outInfo), NULL))
    {
        // SkDebugf("onRangeDecodeParser : get info Error !");
        return false;
    }

    *width = (int)outInfo.srcWidth;
    *height = (int)outInfo.srcHeight;

    if(doRelease)
    {
        if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
        {
            XLOGW("Can not release JPEG HW Decoder\n");
            return false;
        }
    }
    return true;
}

bool SkJPEGImageDecoder::onDecodeHW(SkBitmap* bm, uint8_t* srcBuffer, uint32_t srcBufSize, uint32_t srcSize, int srcFD)
{
    MHAL_JPEG_DEC_START_IN inParams;

    unsigned long u4PQOpt;
    unsigned long u4Flag = 0;
    char value[PROPERTY_VALUE_MAX];
    property_get("persist.PQ", value, "1");
    u4PQOpt = atol(value);
    if(0 != u4PQOpt)
    {
       u4Flag = this->getPostProcFlag();
       //if(0 == (0x1 & u4Flag))
       //{
       //    XLOGD("Flag is not 1%x" , u4Flag);
       //}
    }

    switch (bm->getConfig())
    {
        case SkBitmap::kARGB_8888_Config: 
            inParams.dstFormat = JPEG_OUT_FORMAT_ARGB8888;
            break;

        case SkBitmap::kRGB_565_Config:
            inParams.dstFormat = JPEG_OUT_FORMAT_RGB565;
            break;
        /*    
        case SkBitmap::kYUY2_Pack_Config:
            inParams.dstFormat = JPEG_OUT_FORMAT_YUY2;
            break;
            
        case SkBitmap::kUYVY_Pack_Config:
            inParams.dstFormat = JPEG_OUT_FORMAT_UYVY;
            break;
*/
        default:
            inParams.dstFormat = JPEG_OUT_FORMAT_ARGB8888;
            break;
    }

    bm->lockPixels();
    JSAMPLE* rowptr = (JSAMPLE*)bm->getPixels();
    bool reuseBitmap = (rowptr != NULL);
    bm->unlockPixels();

    if(!reuseBitmap) {
        if (!this->allocPixelRef(bm, NULL)) {
            if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
            {
                XLOGW("Can not release JPEG HW Decoder\n");
                return false;
            }
            return false;
        }
    }

    //inParams.timeout = outInfo.srcWidth * outInfo.srcHeight / 2000;
    //if (inParams.timeout < 100)  inParams.timeout = 100;

    inParams.srcBuffer = srcBuffer;
    inParams.srcBufSize = srcBufSize ;
    inParams.srcLength= srcSize;
    inParams.srcFD = srcFD;
    
    inParams.dstWidth = bm->width();
    inParams.dstHeight = bm->height();
    inParams.dstVirAddr = (UINT8*) bm->getPixels();
    inParams.dstPhysAddr = NULL;

    inParams.doDithering = getDitherImage() ? 1 : 0;
    inParams.doRangeDecode = 0;
    inParams.doPostProcessing = 0;

#ifdef MTK_6572DISPLAY_ENHANCEMENT_SUPPORT    
    inParams.doPostProcessing = u4Flag;
#endif    

    inParams.PreferQualityOverSpeed = this->getPreferQualityOverSpeed() ;
    inParams.jpgDecHandle = fSkJpegDecHandle ;

    // start decode
    SkAutoLockPixels alp(*bm);
    XLOGW("Skia JPEG HW Decoder trigger!!\n");
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_START, 
                                   (void *)&inParams, sizeof(inParams), 
                                   NULL, 0, NULL))
    {
        //SkDebugf("JPEG HW not support this image\n");
        //if(!reuseBitmap)
        //    bm->setPixels(NULL, NULL);
        XLOGW("JPEG HW Decoder return Fail!!\n");
        return false;
    }
    if (reuseBitmap) {
        bm->notifyPixelsChanged();
    }
    XLOGW("JPEG HW Decoder return Successfully!!\n");
    return true;
}

int index_file = 0;
bool store_raw_data(SkBitmap* bm)
{
    FILE *fp;

    char name[150];

    unsigned long u4PQOpt;
    char value[PROPERTY_VALUE_MAX];
    property_get("decode.hw.dump", value, "0");

    u4PQOpt = atol(value);

    if( u4PQOpt == 0) return false;

    if(bm->getConfig() == SkBitmap::kARGB_8888_Config)
        sprintf(name, "/sdcard/dump_%d_%d_%d.888", bm->width(), bm->height(), index_file++);
    else if(bm->getConfig() == SkBitmap::kRGB_565_Config)
        sprintf(name, "/sdcard/dump_%d_%d_%d.565", bm->width(), bm->height(), index_file++);
    else
        return false;

    SkDebugf("store file : %s ", name);


    fp = fopen(name, "wb");
    if(fp == NULL)
    {
        SkDebugf(" open file error ");
        return false;
    }
    if(bm->getConfig() == SkBitmap::kRGB_565_Config)
    {
        fwrite(bm->getPixels(), 1 , bm->getSize(), fp);
        fclose(fp);
        return true;
    }

    unsigned char* addr = (unsigned char*)bm->getPixels();
    SkDebugf("bitmap addr : 0x%x, size : %d ", addr, bm->getSize());
    for(int i = 0 ; i < bm->getSize() ; i += 4)
    {
        fprintf(fp, "%c", addr[i]);
        fprintf(fp, "%c", addr[i+1]);
        fprintf(fp, "%c", addr[i+2]);
    }
    fclose(fp);
    return true;
        
}

bool SkJPEGImageDecoder::onDecodeHWRegion(SkBitmap* bm, SkIRect region, uint8_t* srcBuffer, uint32_t srcSize)
{
    int width, height, regionWidth, regionHeight;
    int sampleSize = this->getSampleSize();
    MHAL_JPEG_DEC_START_IN inParams;
    MHAL_JPEG_DEC_INFO_OUT outInfo;

    SkBitmap::Config config = this->getPrefConfig(k32Bit_SrcDepth, false);
    // only these make sense for jpeg
    if (config != SkBitmap::kARGB_8888_Config &&
        config != SkBitmap::kARGB_4444_Config &&
        config != SkBitmap::kRGB_565_Config) {
        config = SkBitmap::kARGB_8888_Config;
    }

    if (config != SkBitmap::kARGB_8888_Config &&
        config != SkBitmap::kRGB_565_Config) {
        XLOGW("HW Not support format: %d\n", config);
        if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
        {
            XLOGW("Can not release JPEG HW Decoder\n");
        }  
        return false;
    }

    // get file dimension
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_GET_INFO, NULL, 0, 
                                   (void *)&outInfo, sizeof(outInfo), NULL))
    {
        return false;
    }

    width  = (int)outInfo.srcWidth / sampleSize;
    height = (int)outInfo.srcHeight / sampleSize; 

    regionWidth = region.width() / sampleSize;
    regionHeight = region.height() / sampleSize;

    SkBitmap *bitmap = new SkBitmap;
    SkAutoTDelete<SkBitmap> adb(bitmap);
    
    bitmap->setConfig(config, regionWidth, regionHeight);
    bitmap->setIsOpaque(true);     
            
    if (!this->allocPixelRef(bitmap, NULL)) {
        if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
        {
            XLOGD("Can not release JPEG HW Decoder\n");
        }        
        return false;
    }

    SkAutoLockPixels alp(*bitmap);
    
    if (bitmap->getConfig() == SkBitmap::kARGB_8888_Config) {
        inParams.dstFormat = JPEG_OUT_FORMAT_ARGB8888;
    } else {
        inParams.dstFormat = JPEG_OUT_FORMAT_RGB565;
    }

    XLOGD("The file input width: %d, height: %d, output width: %d, height: %d, format: %d, \n", 
           outInfo.srcWidth, outInfo.srcHeight, width, height, config);
    XLOGD("Range Decode Range %d %d %d %d \n", region.fLeft, region.fTop, region.fRight, region.fBottom);
    inParams.srcBuffer = srcBuffer;
    inParams.srcLength= srcSize;
    inParams.dstWidth = width;
    inParams.dstHeight = height;
    inParams.dstVirAddr = (UINT8*) bitmap->getPixels();
    inParams.dstPhysAddr = NULL;

    inParams.doDithering = getDitherImage() ? 1 : 0;
    inParams.doRangeDecode = 1;
    inParams.rangeLeft = region.fLeft;
    inParams.rangeTop = region.fTop;
    inParams.rangeRight = region.fRight;
    inParams.rangeBottom = region.fBottom;

    // start decode
    
    //XLOGD("Before Trigger HW");
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_START, 
                                   (void *)&inParams, sizeof(inParams), 
                                   NULL, 0, NULL))
    {
        XLOGD("JPEG HW not support this image (Range Decode)\n");
        return false;
    }

    bm->swap(*bitmap);
    
    return true;
}

#endif

// This guy exists just to aid in debugging, as it allows debuggers to just
// set a break-point in one place to see all error exists.
static bool return_false(const jpeg_decompress_struct& cinfo,
                         const SkBitmap& bm, const char msg[]) {
#if 1
    XLOGW("libjpeg error %d <%s> from %s [%d %d]", cinfo.err->msg_code,
           cinfo.err->jpeg_message_table[cinfo.err->msg_code], msg,
           bm.width(), bm.height());
#endif
    return false;   // must always return false
}

#ifdef MTK_JPEG_HW_DECODER
class SkAshmemMalloc
{
public:
    SkAshmemMalloc(): fAddr(NULL), fFD(-1) {}
    ~SkAshmemMalloc() { free(); }
    void* reset(size_t size) 
    {
        if(fAddr != NULL) 
            free();

        fSize = size;
        fFD = ashmem_create_region("decodeSrc", size);
        if (-1 == fFD)
        {
            SkDebugf("------- ashmem create failed %d\n", size);
            return NULL;
        }

        int err = ashmem_set_prot_region(fFD, PROT_READ | PROT_WRITE);
        if (err) 
        {
            SkDebugf("------ ashmem_set_prot_region(%d) failed %d\n", fFD, err);
            close(fFD);
            return NULL;
        }

        fAddr = mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_SHARED, fFD, 0);

        if (-1 == (long)fAddr) 
        {
            fAddr = NULL;
            SkDebugf("------- mmap failed for ashmem size=%d \n", size);
            close(fFD);
            return NULL;
        }

        ashmem_pin_region(fFD, 0, 0);
        
        return fAddr;
    }

    void free()
    {
        if(fAddr != NULL)
        {
            ashmem_unpin_region(fFD, 0, 0);
            munmap(fAddr, fSize);
            close(fFD);
            fAddr = NULL;
        }
    }

    void* getAddr() { return fAddr; }
    int getFD() { return fFD; }
    
private:
    void*   fAddr;
    int     fFD;
    size_t  fSize;
    bool    fPinned;

    
};

bool getEOImarker(unsigned char* start, unsigned char* end, unsigned int *bs_offset)
{
  unsigned int eoi_flag = 0; 
  unsigned char* bs_tail ;

  //test_va = 
  
  //XLOGW("SkiaJpeg:getEOImarker start %x, end %x, L:%d!! \n",(unsigned int)start, (unsigned int) end, __LINE__);
  if((start+1 >= end) || start == NULL || end == NULL || (*(uint8_t*)(end) != 0x00)){
    XLOGW("SkiaJpeg:getEOImarker find no EOI [%x %x], L:%d!! \n", (unsigned int)start, (unsigned int) end, __LINE__);
    return false ;
  }

  bs_tail = start+1;//(uint8_t*)(tSrc + rSize - zSize) ;
  for( ;bs_tail < end ; bs_tail++){
    if( (*(uint8_t*)(bs_tail-1) == 0xFF) && (*(uint8_t*)(bs_tail) == 0xD9) ){
       *bs_offset = bs_tail - start ;
       eoi_flag = 1;
       XLOGW("SkiaJpeg:getEOImarker get EOI at %x, oft %x, [%x %x], L:%d!! \n",(unsigned int)bs_tail, *bs_offset, (unsigned int)start, (unsigned int) end, __LINE__);
    }
  }  
  
  
  if(eoi_flag == 0){
    XLOGW("SkiaJpeg:getEOImarker find no EOI [%x %x], L:%d!! \n", (unsigned int)start, (unsigned int) end, __LINE__);
    return false ;
  }else
    return true ;   
}
#endif

bool SkJPEGImageDecoder::onDecode(SkStream* stream, SkBitmap* bm, Mode mode) {
#ifdef TIME_DECODE
    AutoTimeMillis atm("JPEG Decode");
#endif

#ifdef MTK_JPEG_HW_DECODER
    ATRACE_CALL();
    SkAshmemMalloc    tAllocator;
#ifdef USE_SKJPGSTREAM
	JpgStreamAutoClean jpgStreamAutoClean;
#endif
    
    size_t sLength = stream->getLength() + MAX_HEADER_SIZE + 64;
    size_t tmpLength;
    uint8_t* tSrc = NULL;
    size_t rSize = 0;
    size_t rHWbsSize = 0;
    size_t tmpSize = 0;
    size_t align_rSize =0;
    size_t no_eoi = 0;
    size_t skip_hw_path = 0;
    
		unsigned int try_hw = 0;
		
    unsigned long u4PQOpt;
    unsigned long u4Flag = 0;
    char value[PROPERTY_VALUE_MAX];		

#if 0
					char acBuf[256];
					sprintf(acBuf, "/proc/%d/cmdline", getpid());
					FILE *fp = fopen(acBuf, "r");
					if (fp){
						fread(acBuf, 1, sizeof(acBuf), fp);
						fclose(fp);
						if(strncmp(acBuf, "com.android.gallery3d", 21) == 0){				   
						  try_hw = 1;
						}
						//else 
						//if(strncmp(acBuf, "mJpegTest", 9) == 0){			 	   
						//  try_hw = 1; 
						//}
						//SkDebugf("skia: process name2: %s!!\n", acBuf);
					}
     
#endif

#ifdef MTK_6572DISPLAY_ENHANCEMENT_SUPPORT 
    property_get("persist.PQ", value, "1");
    u4PQOpt = atol(value);
    if(0 != u4PQOpt)
    {
       u4Flag = this->getPostProcFlag();
       //if(0 == (0x1 & u4Flag))
       //{
       //    XLOGD("Flag is not 1%x" , u4Flag);
       //}
    }

    try_hw = u4Flag & 0x01;
#endif

#ifdef MTK_JPEG_HW_DECODER_658X
      try_hw = 1;
#endif

  if(try_hw && mode != SkImageDecoder::kDecodeBounds_Mode){  

    tSrc = (uint8_t*)tAllocator.reset(sLength);
    
    if (tSrc != NULL) 
    {
        if((uint32_t)tSrc % 32 != 0)
        {
            tmpLength = 32 - ((uint32_t)tSrc % 32);
            tSrc += tmpLength;
            sLength -= tmpLength;
        }

        if(sLength % 32 != 0)
            sLength -= (sLength % 32);

        rSize = stream->read(tSrc, MAX_HEADER_SIZE);
    }
     

    if (rSize == 0) 
    {
        if (tSrc != NULL) 
        {
            tAllocator.free();
            if (true != stream->rewind()) 
            {
                XLOGW("onDecode(), rewind fail\n");
                return false;       
            }
        }
    } 
    else 
    {
           
        XLOGW("enter Skia Jpeg try_path %d, PPF %d, mode %d, bsLength %x, L:%d!! \n",try_hw,this->getPostProcFlag(), mode, stream->getLength(),__LINE__);           

        if(try_hw && mode != SkImageDecoder::kDecodeBounds_Mode && true == onDecodeParser(bm, mode, tSrc, rSize, tAllocator.getFD()))
        {
            if(mode == SkImageDecoder::kDecodeBounds_Mode)
            {
                tAllocator.free();
                return true;        
            }
            else
            {
                if(rSize == MAX_HEADER_SIZE)
                {
                    SkAshmemMalloc  tmpAllocator;
                    uint8_t* tmpBuffer = NULL;
                    tmpLength = stream->getLength();
                    //SkDebugf("Readed Size : %d, Buffer Size : %d, Remain Stream Size : %d", rSize, sLength, tmpLength);
                    do
                    {
                        if(sLength <= rSize + 16)
                        {
                            XLOGD("Try to Add Buffer Size");
                            sLength = rSize + tmpLength + MAX_HEADER_SIZE + 64;

                            tmpBuffer = (uint8_t*)tmpAllocator.reset(rSize);
                            memcpy(tmpBuffer, tSrc, rSize);
                            tAllocator.free();
                            tSrc = (uint8_t*)tAllocator.reset(sLength);
                            if((uint32_t)tSrc % 32 != 0)
                            {
                                tmpLength = 32 - ((uint32_t)tSrc % 32);
                                tSrc += tmpLength;
                                sLength -= tmpLength;
                            }

                            if(sLength % 32 != 0)
                                sLength -= (sLength % 32);
            
                            memcpy(tSrc, tmpBuffer, rSize);
                            tmpAllocator.free();
                        }
                        tmpSize = stream->read((tSrc + rSize), (sLength - rSize));
                        rSize += tmpSize;
                        tmpLength = stream->getLength();
                        //SkDebugf("Readed Size : %d, Remain Buffer Size : %d, Remain Stream Size : %d", tmpSize, (sLength - rSize), tmpLength);
                    } while(tmpSize != 0);
                } 

                rHWbsSize = rSize ;
#if 1                
                {
                  uint8_t* bs_tail = (uint8_t*)(tSrc + rSize) ;
                  uint8_t zSize = 128 ;
                  unsigned int ofset = 0 ;
                  if( (*(uint8_t*)(bs_tail-2) != 0xFF) || (*(uint8_t*)(bs_tail-1) != 0xD9) ){
                    //XLOGW("SkiaJpeg:broken bitstream!!\n");  
                    XLOGW("SkiaJpeg: broken_jpeg_bs b %x,s %x, bs: b %x %x, e %x %x %x %x %x, L:%d!!\n", (unsigned int)tSrc, rSize,*tSrc, *(tSrc+1)
                    , *(uint8_t*)(bs_tail-4),*(uint8_t*)(bs_tail-3),*(uint8_t*)(bs_tail-2), *(uint8_t*)(bs_tail-1), *bs_tail,__LINE__);                                    
                    no_eoi =1;
                    if(zSize < rSize){                     
                      if(getEOImarker(bs_tail-zSize, bs_tail-1, &ofset))
                        no_eoi = 0;
                    }                    
                  }
                }                
#endif                
                if( no_eoi 
                    //|| (bm->width() == 200 && bm->height() == 200)
                   ){
                  if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_CANCEL, (void*) fSkJpegDecHandle, 0, NULL, 0, NULL))
                  {
                      XLOGW("Can not release JPEG HW Decoder\n");
                      return false;
                  }
                  skip_hw_path = 1;
                }
                if(sLength > rSize){
                    memset((tSrc + rSize), 0, sLength - rSize);
#ifndef MTK_JPEG_HW_DECODER_658X                    
                    rSize += 64 ;
                    SkDebugf("JPEG_BS mSize %x, rSize %x, align rSize %x, LENGTH %x!!\n", sLength, rSize, (rSize + 31) & (~31), stream->getLength());
                    rSize = (rSize + 31) & (~31);    
#endif                    
                }
                SkDebugf("SkiaJpeg: skip %d, BufSize %x, BitsSize %x, BitsAlignSize %x, GetLength %x, L:%d!!\n",skip_hw_path, sLength, rSize, (rSize + 31) & (~31), stream->getLength(), __LINE__); 
                    
                //if(true != onDecodeHW(bm, tSrc, rSize, tAllocator.getFD()) )
                //if(skip_hw_path || true != onDecodeHW(bm, tSrc, sLength, ((sLength - 256)>rSize) ? sLength-256: rSize, tAllocator.getFD()) )
                if(skip_hw_path || true != onDecodeHW(bm, tSrc, sLength, rSize, tAllocator.getFD()) )
                {
                    XLOGD("SkiaJpeg:TRY_SW_PATH no_eoi %d, mSize %x, rSize %x, align rSize %x, skSize %x!!\n", no_eoi, sLength, rSize, (rSize + 31) & (~31), stream->getLength());
                    if(rSize > MAX_HEADER_SIZE)
                    {
#ifdef USE_SKJPGSTREAM						
                        stream = new SkJpgStream(tSrc, rHWbsSize, stream);
                        jpgStreamAutoClean.set(stream);
#else
                        stream = new SkMemoryStream(tSrc, sLength);   
#endif
                        XLOGW("Use JPEG SW Decoder (temp stream)\n");
                    }
                    else
                    {
#if 0 //def USE_SKJPGSTREAM
			            XLOGD("SkiaJpeg:TRY_SW_PATH tSrc %x, rSize %x, skSize %x, L:%d!!\n", tSrc, rSize,  stream->getLength(), __LINE__);
			            stream = new SkJpgStream(tSrc, rHWbsSize, stream);
			            jpgStreamAutoClean.set(stream);
#else
                        tAllocator.free();
#endif                      						
                        XLOGW("Use JPEG SW Decoder\n");
                        if(true != stream->rewind())
                        {
                            XLOGW("onDecode(), rewind fail\n");
                            return false;       
                        }
                    }
                }
                else
                {
                    return true;
                }
            }
        }
        else
        {
#if 0 //def USE_SKJPGSTREAM
			XLOGD("SkiaJpeg:TRY_SW_PATH tSrc %x, rSize %x, skSize %x, L:%d!!\n", tSrc, rSize,  stream->getLength(), __LINE__);
			stream = new SkJpgStream(tSrc, rSize, stream);
			jpgStreamAutoClean.set(stream);
#else        
            tAllocator.free();
#endif            
            XLOGW("Use JPEG SW Decoder\n");
            if(true != stream->rewind())
            {
                XLOGW("onDecode(), rewind fail\n");
                return false;       
            }
        }
    }
  }
    
#endif
     
    SkAutoMalloc  srcStorage;
    JPEGAutoClean autoClean;

    jpeg_decompress_struct  cinfo;
    skjpeg_error_mgr        sk_err;
    skjpeg_source_mgr       sk_stream(stream, this, false);

    cinfo.err = jpeg_std_error(&sk_err);
    sk_err.error_exit = skjpeg_error_exit;

    // All objects need to be instantiated before this setjmp call so that
    // they will be cleaned up properly if an error occurs.
    if (setjmp(sk_err.fJmpBuf)) {
        return return_false(cinfo, *bm, "setjmp");
    }

    jpeg_create_decompress(&cinfo);
    autoClean.set(&cinfo);

#ifdef SK_BUILD_FOR_ANDROID
    overwrite_mem_buffer_size(&cinfo);
#endif

    //jpeg_stdio_src(&cinfo, file);
    cinfo.src = &sk_stream;

    int status = jpeg_read_header(&cinfo, true);
    if (status != JPEG_HEADER_OK) {
        return return_false(cinfo, *bm, "read_header");
    }

#ifdef CHECK_LARGE_JPEG_PROG 
#ifdef USE_MTK_ALMK 
if(cinfo.progressive_mode)
{
   unsigned int rSize = cinfo.image_width * cinfo.image_height*6;   
   unsigned int maxSize ;
    if(!almkGetMaxSafeSize(getpid(), &maxSize)){
      maxSize = JPEG_PROG_LIMITATION_SIZE ;
      SkDebugf("ALMK::ProgJPEG::get Max Safe bmp (Max %d bytes) for BMP file (%d bytes) fail!!\n", maxSize, rSize);
    }else{
      if(maxSize > rSize)
        SkDebugf("ALMK::ProgJPEG::Max Safe Size (Max %d bytes) for BMP file(%d bytes)=> PASS !! \n", maxSize, rSize);
      else
        SkDebugf("ALMK::ProgJPEG::Max Safe Size (Max %d bytes) for BMP file(%d bytes)=> MemoryShortage!! \n", maxSize, rSize);        
    }
			if(cinfo.progressive_mode && (rSize > maxSize) )
			{
				SkDebugf("too Large Progressive Image (%d, %d %d)", cinfo.progressive_mode,cinfo.image_width, cinfo.image_height);
				return return_false(cinfo, *bm, "Not support too Large Progressive Image!!");		 
			}
}	
#else
			if(cinfo.progressive_mode && (cinfo.image_width * cinfo.image_height > JPEG_PROG_LIMITATION_SIZE) )
			{
				SkDebugf("too Large Progressive Image (%d, %d x %d)> limit(%d)", cinfo.progressive_mode,cinfo.image_width, cinfo.image_height, JPEG_PROG_LIMITATION_SIZE);
				return return_false(cinfo, *bm, "Not support too Large Progressive Image!!");		 
			}
#endif			
		
#endif

     


    /*  Try to fulfill the requested sampleSize. Since jpeg can do it (when it
        can) much faster that we, just use their num/denom api to approximate
        the size.
    */
    int sampleSize = this->getSampleSize();

    if (this->getPreferQualityOverSpeed()) {
        cinfo.dct_method = JDCT_ISLOW;
    } else {
        cinfo.dct_method = JDCT_IFAST;
    }

    cinfo.scale_num = 1;
    cinfo.scale_denom = sampleSize;

    /* this gives about 30% performance improvement. In theory it may
       reduce the visual quality, in practice I'm not seeing a difference
     */
    cinfo.do_fancy_upsampling = 0;

    /* this gives another few percents */
    cinfo.do_block_smoothing = 0;

    /* default format is RGB */
    cinfo.out_color_space = JCS_RGB;

    SkBitmap::Config config = this->getPrefConfig(k32Bit_SrcDepth, false);
    // only these make sense for jpegs
    if (config != SkBitmap::kARGB_8888_Config &&
        config != SkBitmap::kARGB_4444_Config &&
        config != SkBitmap::kRGB_565_Config) {
        config = SkBitmap::kARGB_8888_Config;
    }

#ifdef ANDROID_RGB
    cinfo.dither_mode = JDITHER_NONE;
    if (config == SkBitmap::kARGB_8888_Config) {
        cinfo.out_color_space = JCS_RGBA_8888;
    } else if (config == SkBitmap::kRGB_565_Config) {
        cinfo.out_color_space = JCS_RGB_565;
        if (this->getDitherImage()) {
            cinfo.dither_mode = JDITHER_ORDERED;
        }
    }
#endif
    SkDebugf("jpeg_decoder mode %d, config %d, w %d, h %d, sample %d, bsLength %x!!\n",mode,config,cinfo.image_width, cinfo.image_height, sampleSize, stream->getLength());
    if (sampleSize == 1 && mode == SkImageDecoder::kDecodeBounds_Mode) {
        bm->setConfig(config, cinfo.image_width, cinfo.image_height);
        bm->setIsOpaque(true);
        return true;
    }

    /*  image_width and image_height are the original dimensions, available
        after jpeg_read_header(). To see the scaled dimensions, we have to call
        jpeg_start_decompress(), and then read output_width and output_height.
    */
    if (!jpeg_start_decompress(&cinfo)) {
        /*  If we failed here, we may still have enough information to return
            to the caller if they just wanted (subsampled bounds). If sampleSize
            was 1, then we would have already returned. Thus we just check if
            we're in kDecodeBounds_Mode, and that we have valid output sizes.

            One reason to fail here is that we have insufficient stream data
            to complete the setup. However, output dimensions seem to get
            computed very early, which is why this special check can pay off.
         */
        if (SkImageDecoder::kDecodeBounds_Mode == mode &&
                valid_output_dimensions(cinfo)) {
            SkScaledBitmapSampler smpl(cinfo.output_width, cinfo.output_height,
                                       recompute_sampleSize(sampleSize, cinfo));
            bm->setConfig(config, smpl.scaledWidth(), smpl.scaledHeight());
            bm->setIsOpaque(true);
            return true;
        } else {
            return return_false(cinfo, *bm, "start_decompress");
        }
    }
    sampleSize = recompute_sampleSize(sampleSize, cinfo);

    // should we allow the Chooser (if present) to pick a config for us???
    if (!this->chooseFromOneChoice(config, cinfo.output_width,
                                   cinfo.output_height)) {
        return return_false(cinfo, *bm, "chooseFromOneChoice");
    }

#ifdef ANDROID_RGB
    /* short-circuit the SkScaledBitmapSampler when possible, as this gives
       a significant performance boost.
    */
    if (sampleSize == 1 &&
        ((config == SkBitmap::kARGB_8888_Config && 
                cinfo.out_color_space == JCS_RGBA_8888) ||
        (config == SkBitmap::kRGB_565_Config && 
                cinfo.out_color_space == JCS_RGB_565)))
    {
        bm->lockPixels();
        JSAMPLE* rowptr = (JSAMPLE*)bm->getPixels();
        bm->unlockPixels();
        bool reuseBitmap = (rowptr != NULL);
        if (reuseBitmap && ((int) cinfo.output_width != bm->width() ||
                (int) cinfo.output_height != bm->height())) {
            // Dimensions must match
            SkDebugf("Dimensions must match (%d %d) (%d %d)", bm->width(), bm->height(), cinfo.output_width, cinfo.output_height);
            return false;
        }

        if (!reuseBitmap) {
            bm->setConfig(config, cinfo.output_width, cinfo.output_height);
            bm->setIsOpaque(true);
            if (SkImageDecoder::kDecodeBounds_Mode == mode) {
                return true;
            }
            if (!this->allocPixelRef(bm, NULL)) {
                return return_false(cinfo, *bm, "allocPixelRef");
            }
        } else if (SkImageDecoder::kDecodeBounds_Mode == mode) {
            return true;
        }
        SkAutoLockPixels alp(*bm);
        rowptr = (JSAMPLE*)bm->getPixels();
        INT32 const bpr =  bm->rowBytes();
        
        while (cinfo.output_scanline < cinfo.output_height) {
            int row_count = jpeg_read_scanlines(&cinfo, &rowptr, 1);
            // if row_count == 0, then we didn't get a scanline, so abort.
            // if we supported partial images, we might return true in this case
            if (0 == row_count) {
                return return_false(cinfo, *bm, "read_scanlines");
            }
            if (this->shouldCancelDecode()) {
                return return_false(cinfo, *bm, "shouldCancelDecode");
            }
            rowptr += bpr;
        }
        if (reuseBitmap) {
            bm->notifyPixelsChanged();
        }
        jpeg_finish_decompress(&cinfo);
        
      
    	  XLOGD("jpeg_decoder finish successfully, L:%d, reuse %d!!!\n",__LINE__, reuseBitmap);		
        return true;
    }
#endif
    
    // check for supported formats
    SkScaledBitmapSampler::SrcConfig sc;
    if (3 == cinfo.out_color_components && JCS_RGB == cinfo.out_color_space) {
        sc = SkScaledBitmapSampler::kRGB;
#ifdef ANDROID_RGB
    } else if (JCS_RGBA_8888 == cinfo.out_color_space) {
        sc = SkScaledBitmapSampler::kRGBX;
    } else if (JCS_RGB_565 == cinfo.out_color_space) {
        sc = SkScaledBitmapSampler::kRGB_565;
#endif
    } else if (1 == cinfo.out_color_components &&
               JCS_GRAYSCALE == cinfo.out_color_space) {
        sc = SkScaledBitmapSampler::kGray;
    } else {
        return return_false(cinfo, *bm, "jpeg colorspace");
    }

    SkScaledBitmapSampler sampler(cinfo.output_width, cinfo.output_height,
                                  sampleSize);

    bm->lockPixels();
    JSAMPLE* rowptr = (JSAMPLE*)bm->getPixels();
    bool reuseBitmap = (rowptr != NULL);
    bm->unlockPixels();
    if (reuseBitmap && (sampler.scaledWidth() != bm->width() ||
            sampler.scaledHeight() != bm->height())) {
        // Dimensions must match
        SkDebugf("_Dimensions must match (%d %d) (%d %d) (%d %d)", bm->width(), bm->height(), cinfo.output_width, cinfo.output_height, 
        sampler.scaledWidth(), sampler.scaledHeight());
        return false;
    }

    if (!reuseBitmap) {
        bm->setConfig(config, sampler.scaledWidth(), sampler.scaledHeight());
        // jpegs are always opaque (i.e. have no per-pixel alpha)
        bm->setIsOpaque(true);

        if (SkImageDecoder::kDecodeBounds_Mode == mode) {
            return true;
        }
        if (!this->allocPixelRef(bm, NULL)) {
            return return_false(cinfo, *bm, "allocPixelRef");
        }
    } else if (SkImageDecoder::kDecodeBounds_Mode == mode) {
        return true;
    }

    SkAutoLockPixels alp(*bm);                          
    if (!sampler.begin(bm, sc, this->getDitherImage())) {
        return return_false(cinfo, *bm, "sampler.begin");
    }

    uint8_t* srcRow = (uint8_t*)srcStorage.reset(cinfo.output_width * 4);

    //  Possibly skip initial rows [sampler.srcY0]
    if (!skip_src_rows(&cinfo, srcRow, sampler.srcY0())) {
        return return_false(cinfo, *bm, "skip rows");
    }

    // now loop through scanlines until y == bm->height() - 1
    for (int y = 0;; y++) {
        JSAMPLE* rowptr = (JSAMPLE*)srcRow;
        int row_count = jpeg_read_scanlines(&cinfo, &rowptr, 1);
        if (0 == row_count) {
            return return_false(cinfo, *bm, "read_scanlines");
        }
        if (this->shouldCancelDecode()) {
            return return_false(cinfo, *bm, "shouldCancelDecode");
        }
        
        sampler.next(srcRow);
        if (bm->height() - 1 == y) {
            // we're done
            break;
        }

        if (!skip_src_rows(&cinfo, srcRow, sampler.srcDY() - 1)) {
            return return_false(cinfo, *bm, "skip rows");
        }
    }

    // we formally skip the rest, so we don't get a complaint from libjpeg
    if (!skip_src_rows(&cinfo, srcRow,
                       cinfo.output_height - cinfo.output_scanline)) {
        return return_false(cinfo, *bm, "skip rows");
    }
    if (reuseBitmap) {
        bm->notifyPixelsChanged();
    }
    jpeg_finish_decompress(&cinfo);

      
	  XLOGD("jpeg_decoder finish successfully, L:%d, reuse %d!!!\n",__LINE__, reuseBitmap);

//    SkDebugf("------------------- bm2 size %d [%d %d] %d\n", bm->getSize(), bm->width(), bm->height(), bm->config());
    return true;
}

bool SkJPEGImageDecoder::onBuildTileIndex(SkStream* stream,
                                        int *width, int *height) {
    SkAutoMalloc  srcStorage;
    SkJPEGImageIndex *index = new SkJPEGImageIndex;

#if MTK_JPEG_HW_DECODER
    fFirstTileDone = false;
    fUseHWResizer = false;
    char acBuf[256];
    sprintf(acBuf, "/proc/%d/cmdline", getpid());
    FILE *fp = fopen(acBuf, "r");
    if (fp)
    {
        fread(acBuf, 1, sizeof(acBuf), fp);
        fclose(fp);
        if(strncmp(acBuf, "com.android.cts", 15) == 0)
        {
            fFirstTileDone = true;
            fUseHWResizer = false;
        }
    }
#endif
//#if MTK_JPEG_HW_DECODER
#if 0
    size_t sLength = stream->getLength() + MAX_HEADER_SIZE;
    size_t tmpLength;
    size_t tmpSize = 0;

    fInitRegion = true;
    fSrc = (uint8_t*)fAllocator.reset(sLength);
    fSize = 0;
    if (fSrc != NULL) 
    {
        if((uint32_t)fSrc % 32 != 0)
        {
            tmpLength = 32 - ((uint32_t)fSrc % 32);
            fSrc += tmpLength;
            sLength -= tmpLength;
        }

        if(sLength % 32 != 0)
            sLength -= (sLength % 32);

        fSize = stream->read(fSrc, MAX_HEADER_SIZE);
    }

    if (fSize != 0) 
    {
        if(true == onRangeDecodeParser(fSrc, fSize, width, height, true))
        {

	          android::Tracer::traceBegin(ATRACE_TAG_GRAPHICS,"onDecodeBS");         
            if(fSize == MAX_HEADER_SIZE)
            {
                SkAutoMalloc  tmpAllocator;
                uint8_t* tmpBuffer = NULL;
                tmpLength = stream->getLength();
                //SkDebugf("Readed Size : %d, Buffer Size : %d, Remain Stream Size : %d", rSize, sLength, tmpLength);
                do
                {
                    if(sLength <= fSize + 16)
                    {
                        XLOGD("Try to Add Buffer Size");
                        sLength = tmpLength + MAX_HEADER_SIZE;

                        tmpBuffer = (uint8_t*)tmpAllocator.reset(sLength);
                        memcpy(tmpBuffer, fSrc, fSize);
                        fAllocator.free();
                        fSrc = (uint8_t*)fAllocator.reset(sLength);
                        if((uint32_t)fSrc % 32 != 0)
                        {
                            tmpLength = 32 - ((uint32_t)fSrc % 32);
                            fSrc += tmpLength;
                            sLength -= tmpLength;
                        }

                        if(sLength % 32 != 0)
                            sLength -= (sLength % 32);
            
                        memcpy(fSrc, tmpBuffer, fSize);
                        tmpAllocator.free();
                    }
                    tmpSize = stream->read((fSrc + fSize), (sLength - fSize));
                    fSize += tmpSize;
                    tmpLength = stream->getLength();
                        //SkDebugf("Readed Size : %d, Remain Buffer Size : %d, Remain Stream Size : %d", tmpSize, (sLength - rSize), tmpLength);
                } while(tmpSize != 0);
            } 
            android::Tracer::traceEnd(ATRACE_TAG_GRAPHICS);    
            if(sLength > fSize)
            {
                memset((fSrc + fSize), 0, sLength - fSize);
                //fSize = sLength;
            }

            if(fSize > MAX_HEADER_SIZE)
            {
                stream = new SkMemoryStream(fSrc, sLength);
            }
            else
            {
                if(true != stream->rewind())
                {
                    XLOGW("onBuildTileIndex, rewind fail\n");
                    return false;       
                }
            }
        }
        else
        {
            fSize = 0;
        }
    } 

    if(fSize == 0)
    {
        fAllocator.free();
        fSrc = NULL;
        if(true != stream->rewind())
        {
            SkDebugf("onBuildTileIndex(), rewind fail\n");
            return false;       
        }
    }
    
#endif

    jpeg_decompress_struct  *cinfo = (jpeg_decompress_struct*)
                                        malloc(sizeof(jpeg_decompress_struct));
    skjpeg_error_mgr        sk_err;
    skjpeg_source_mgr       *sk_stream =
        new skjpeg_source_mgr(stream, this, true);
    if (cinfo == NULL || sk_stream == NULL) {
        return false;
    }

    cinfo->err = jpeg_std_error(&sk_err);
    sk_err.error_exit = skjpeg_error_exit;

    // All objects need to be instantiated before this setjmp call so that
    // they will be cleaned up properly if an error occurs.
    if (setjmp(sk_err.fJmpBuf)) {
        return false;
    }

    jpeg_create_decompress(cinfo);
    cinfo->do_fancy_upsampling = 0;
    cinfo->do_block_smoothing = 0;

#ifdef SK_BUILD_FOR_ANDROID
    overwrite_mem_buffer_size(cinfo);
#endif

    cinfo->src = sk_stream;
    int status = jpeg_read_header(cinfo, true);
    if (status != JPEG_HEADER_OK) {
        return false;
    }
    index->index = (huffman_index*)malloc(sizeof(huffman_index));
    jpeg_create_huffman_index(cinfo, index->index);

    cinfo->scale_num = 1;
    cinfo->scale_denom = 1;
    if (!jpeg_build_huffman_index(cinfo, index->index)) {
        return false;
    }
    if (fReporter)
        fReporter->reportMemory(index->index->mem_used);
    jpeg_destroy_decompress(cinfo);


    // Init decoder to image decode mode
    jpeg_create_decompress(cinfo);

#ifdef SK_BUILD_FOR_ANDROID
    overwrite_mem_buffer_size(cinfo);
#endif

    cinfo->src = sk_stream;
    status = jpeg_read_header(cinfo,true);
    if (status != JPEG_HEADER_OK) {
        return false;
    }
    cinfo->out_color_space = JCS_RGBA_8888;
    cinfo->do_fancy_upsampling = 0;
    cinfo->do_block_smoothing = 0;
    //jpeg_start_decompress(cinfo);
    jpeg_start_tile_decompress(cinfo);

    cinfo->scale_num = 1;
    index->cinfo = cinfo;
    *height = cinfo->output_height;
    *width = cinfo->output_width;
    this->imageWidth = *width;
    this->imageHeight = *height;
    this->index = index;
    return true;
}

#if MTK_JPEG_HW_DECODER
#ifdef MTK_JPEG_HW_REGION_RESIZER
bool MDPCrop(void* src, int width, int height, SkBitmap* bm, int tdsp)
{

   
    if((NULL == bm))
    {
        XLOGW("MDP_Crop : null bitmap");
        return false;
    }
    if(NULL == bm->getPixels())
    {
        XLOGW("MDP_Crop : null pixels");
        return false;
    }
    if((bm->config() == SkBitmap::kARGB_8888_Config && bm->isOpaque()) || 
       (bm->config() == SkBitmap::kRGB_565_Config))
    {
        DpBlitStream bltStream;  //mHalBltParam_t bltParam;
        unsigned int src_addr[3];
        unsigned int src_size[3];        
        unsigned int plane_num = 1;
        DpColorFormat dp_out_fmt ;
        DpColorFormat dp_in_fmt ;
        unsigned int src_pByte = 4;
        src_addr[0] = (unsigned int) src ;
        switch(bm->config())
        {
            case SkBitmap::kARGB_8888_Config:
                dp_out_fmt = eRGBX8888; //eABGR8888;    //bltParam.dstFormat = MHAL_FORMAT_ABGR_8888;
                src_pByte = 4;
                break;
            case SkBitmap::kRGB_565_Config:                
                dp_out_fmt = eRGB565;    //bltParam.dstFormat = MHAL_FORMAT_RGB_565;
                src_pByte = 2;
                break;
            default :
                XLOGW("MDP_Crop : unvalid bitmap config d!!\n", bm->config());
                return false;
        }
        
        dp_in_fmt = dp_out_fmt ;
        
        
        src_size[0] = width * height * src_pByte ;
        XLOGW("MDP_Crop: wh (%d %d)->(%d %d), fmt %d, size %d->%d, regionPQ %d!!\n", width, height, bm->width(), bm->height()
        , bm->config(), src_size[0], bm->rowBytes() * bm->height(), tdsp);
        
        bltStream.setTdshp((tdsp == 0)? false:true);
        
        //XLOGW("MDP_Crop: CONFIG_SRC_BUF, go L:%d!!\n", __LINE__);
        bltStream.setSrcBuffer((void**)src_addr, src_size, plane_num);
        DpRect src_roi;
        src_roi.x = 0;
        src_roi.y = 0;
        src_roi.w = width;
        src_roi.h = height;
        //XLOGW("MDP_Crop: CONFIG_SRC_SIZE, go L:%d!!\n", __LINE__);
        bltStream.setSrcConfig(width, height, width * src_pByte, 0, dp_in_fmt, DP_PROFILE_JPEG);

        // set dst buffer
        ///XLOGW("MDP_Crop: CONFIG_DST_BUF, go L:%d!!\n", __LINE__);
        bltStream.setDstBuffer((void *)bm->getPixels(), bm->rowBytes() * bm->height() );  // bm->width() * bm->height() * dst_bitperpixel / 8);
        DpRect dst_roi;
        dst_roi.x = 0;
        dst_roi.y = 0;
        dst_roi.w = bm->width();
        dst_roi.h = bm->height();

        //XLOGW("MDP_Crop: CONFIG_DST_SIZE, go L:%d!!\n", __LINE__);
        bltStream.setDstConfig(bm->width(), bm->height(), bm->rowBytes(), 0, dp_out_fmt, DP_PROFILE_JPEG);

        //XLOGW("MDP_Crop: GO_BITBLIT, go L:%d!!\n", __LINE__);
        if (bltStream.invalidate() < 0) {
            XLOGE("region Resizer: DpBlitStream invalidate failed, L:%d!!\n", __LINE__);
            return false;
        }else{
            return true ;
        }
       
    }
    return false;
}

bool MDPResizer(void* src, int width, int height, SkScaledBitmapSampler::SrcConfig sc, SkBitmap* bm, int tdsp)
{

   
    if((NULL == bm))
    {
        XLOGW("MDPResizer : null bitmap");
        return false;
    }
    if(NULL == bm->getPixels())
    {
        XLOGW("MDPResizer : null pixels");
        return false;
    }
    if((bm->config() == SkBitmap::kARGB_8888_Config && bm->isOpaque()) || 
       (bm->config() == SkBitmap::kRGB_565_Config))
    {
        DpBlitStream bltStream;  //mHalBltParam_t bltParam;
        unsigned int src_addr[3];
        unsigned int src_size[3];        
        unsigned int plane_num = 1;
        DpColorFormat dp_out_fmt ;
        DpColorFormat dp_in_fmt ;
        unsigned int src_pByte = 4;
        src_addr[0] = (unsigned int) src ;
        switch(bm->config())
        {
            case SkBitmap::kARGB_8888_Config:
                dp_out_fmt = eRGBX8888; //eABGR8888;    //bltParam.dstFormat = MHAL_FORMAT_ABGR_8888;
                break;
            case SkBitmap::kRGB_565_Config:                
                dp_out_fmt = eRGB565;    //bltParam.dstFormat = MHAL_FORMAT_RGB_565;
                break;
            default :
                XLOGW("MDPResizer : unvalid bitmap config d", bm->config());
                return false;
        }
        switch(sc)
        {
            case SkScaledBitmapSampler::kRGB:
                dp_in_fmt = eRGB888;         //bltParam.srcFormat = MHAL_FORMAT_BGR_888;
                src_pByte = 3;
                break;
            case SkScaledBitmapSampler::kRGBX:
                dp_in_fmt = eRGBX8888;//eABGR8888;         //bltParam.srcFormat = MHAL_FORMAT_ABGR_8888;
                src_pByte = 4;
                break;
            case SkScaledBitmapSampler::kRGB_565:
                dp_in_fmt = eRGB565;         //bltParam.srcFormat = MHAL_FORMAT_RGB_565;
                src_pByte = 2;
                break;
            case SkScaledBitmapSampler::kGray:
                dp_in_fmt = eGREY;           //bltParam.srcFormat = MHAL_FORMAT_Y800;
                src_pByte = 1;
                break;
            default :
                XLOGW("MDPResizer : unvalid src format %d", sc);
                return false;
            break;
        }
        src_size[0] = width * height * src_pByte ;
        XLOGW("MDPResizer: wh (%d %d)->(%d %d), fmt %d->%d, size %d->%d, regionPQ %d!!\n", width, height, bm->width(), bm->height()
        ,sc, bm->config(), src_size[0], bm->rowBytes() * bm->height(), tdsp);
        
        bltStream.setTdshp((tdsp == 0)? false:true);
        
        //XLOGW("MDPResizer: CONFIG_SRC_BUF, go L:%d!!\n", __LINE__);
        bltStream.setSrcBuffer((void**)src_addr, src_size, plane_num);
        DpRect src_roi;
        src_roi.x = 0;
        src_roi.y = 0;
        src_roi.w = width;
        src_roi.h = height;
        //XLOGW("MDPResizer: CONFIG_SRC_SIZE, go L:%d!!\n", __LINE__);
        //bltStream.setSrcConfig(width, height, dp_in_fmt, eInterlace_None, &src_roi);
        bltStream.setSrcConfig(width, height, width * src_pByte, 0, dp_in_fmt, DP_PROFILE_JPEG);

        // set dst buffer
        ///XLOGW("MDPResizer: CONFIG_DST_BUF, go L:%d!!\n", __LINE__);
        bltStream.setDstBuffer((void *)bm->getPixels(), bm->rowBytes() * bm->height() );  // bm->width() * bm->height() * dst_bitperpixel / 8);
        DpRect dst_roi;
        dst_roi.x = 0;
        dst_roi.y = 0;
        dst_roi.w = bm->width();
        dst_roi.h = bm->height();

        //XLOGW("MDPResizer: CONFIG_DST_SIZE, go L:%d!!\n", __LINE__);
        //bltStream.setDstConfig(bm->width(), bm->height(), dp_out_fmt, eInterlace_None, &dst_roi);
        bltStream.setDstConfig(bm->width(), bm->height(), bm->rowBytes(), 0, dp_out_fmt, DP_PROFILE_JPEG);

        //XLOGW("MDPResizer: GO_BITBLIT, go L:%d!!\n", __LINE__);
        if (bltStream.invalidate() < 0) {
            XLOGE("region Resizer: DpBlitStream invalidate failed, L:%d!!\n", __LINE__);
            return false;
        }else{
            return true ;
        }
/*
        bltParam.orientation = MHAL_BITBLT_ROT_0;
        bltParam.srcAddr = (unsigned int)src;
        bltParam.dstAddr = (unsigned int)bm->getPixels();
        bltParam.srcX = bltParam.srcY = 0;

        
        bltParam.srcW = bltParam.srcWStride = width;
        bltParam.dstW = bm->width();
        
        bltParam.srcH = bltParam.srcHStride = height;
        bltParam.dstH = bm->height();

        bltParam.pitch = bm->width();
        
        if (MHAL_NO_ERROR != mHalMdpIpc_BitBlt(&bltParam))
        {
            return false;
        }
        else
        {
            return true;
        }
*/        
    }
    return false;
}
#endif
#if 0
bool MDPResizer(void* src, int width, int height, SkScaledBitmapSampler::SrcConfig sc, SkBitmap* bm, int tdsp)
{
    if((NULL == bm))
    {
        XLOGW("MDPResizer : null bitmap");
        return false;
    }

    if(NULL == bm->getPixels())
    {
        XLOGW("MDPResizer : null pixels");
        return false;
    }

    if((bm->config() == SkBitmap::kARGB_8888_Config && bm->isOpaque()) || 
       (bm->config() == SkBitmap::kRGB_565_Config))
    {
        mHalBltParam_t bltParam;
        memset(&bltParam, 0, sizeof(mHalBltParam_t));
        
        switch(bm->config())
        {
            case SkBitmap::kARGB_8888_Config:
                bltParam.dstFormat = MHAL_FORMAT_ABGR_8888;
                break;

            case SkBitmap::kRGB_565_Config:                
                bltParam.dstFormat = MHAL_FORMAT_RGB_565;
                break;
                
            default :
                XLOGW("MDPResizer : unvalid bitmap config d", bm->config());
                return false;
        }

        switch(sc)
        {
            case SkScaledBitmapSampler::kRGB:
                bltParam.srcFormat = MHAL_FORMAT_BGR_888;
                break;
                
            case SkScaledBitmapSampler::kRGBX:
                bltParam.srcFormat = MHAL_FORMAT_ABGR_8888;
                break;
                
            case SkScaledBitmapSampler::kRGB_565:
                bltParam.srcFormat = MHAL_FORMAT_RGB_565;
                break;
                
            case SkScaledBitmapSampler::kGray:
                bltParam.srcFormat = MHAL_FORMAT_Y800;
                break;
            
            default :
                XLOGW("MDPResizer : unvalid src format %d", sc);
                return false;
            break;
        }

        bltParam.orientation = MHAL_BITBLT_ROT_0;
        bltParam.srcAddr = (unsigned int)src;
        bltParam.dstAddr = (unsigned int)bm->getPixels();
        bltParam.srcX = bltParam.srcY = 0;

        
        bltParam.srcW = bltParam.srcWStride = width;
        bltParam.dstW = bm->width();
        
        bltParam.srcH = bltParam.srcHStride = height;
        bltParam.dstH = bm->height();

        bltParam.pitch = bm->width();
        
        if (MHAL_NO_ERROR != mHalMdpIpc_BitBlt(&bltParam))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    return false;
}
#endif

#endif
bool SkJPEGImageDecoder::onDecodeRegion(SkBitmap* bm, SkIRect region) {

//#if MTK_JPEG_HW_DECODER
#if 0
    if(fSize != 0 || fInitRegion) 
    {       
        int width, height;
        int try_times = 0;
        do {
            if(true == onRangeDecodeParser(fSrc, fSize, &width, &height, false)) {
                if(true == onDecodeHWRegion(bm, region, fSrc, fSize)) {
                    if(fInitRegion)
                    {
                        fInitRegion = false;
                    }
                    return true;
                }
            }
            if(!fInitRegion)
            {
                SkDebugf("onDecodeRegion HW Failed, try again!!!");
                usleep(100*1000);
            }
            try_times++;    
        } while (!fInitRegion && try_times < 5);
        
        SkDebugf("onDecodeRegion HW Failed, Use SW!!!");
        if(fInitRegion)
        {
            fInitRegion = false;
            fSize = 0;
        }
    }
#endif

    if (index == NULL) {
        return false;
    }
    jpeg_decompress_struct *cinfo = index->cinfo;

    SkIRect rect = SkIRect::MakeWH(this->imageWidth, this->imageHeight);
    if (!rect.intersect(region)) {
        // If the requested region is entirely outsides the image, just
        // returns false
        return false;
    }
    SkAutoMalloc  srcStorage;
    skjpeg_error_mgr        sk_err;
    cinfo->err = jpeg_std_error(&sk_err);
    sk_err.error_exit = skjpeg_error_exit;
    if (setjmp(sk_err.fJmpBuf)) {
        return false;
    }
    int requestedSampleSize = this->getSampleSize();
    
#ifdef MTK_JPEG_HW_REGION_RESIZER //MTK_JPEG_HW_DECODER
    if(this->imageWidth * this->imageHeight > HW_RESIZE_MAX_PIXELS)
    {
        fFirstTileDone = true;
        fUseHWResizer = false;
        XLOGW("It is too large pixels (%d) to use hw resizer! Use sw sampler ", this->imageWidth * this->imageHeight);
    }
    if(!fFirstTileDone || fUseHWResizer)
        cinfo->scale_denom = 1;
    else
        cinfo->scale_denom = requestedSampleSize;
#else
    cinfo->scale_denom = requestedSampleSize;
#endif
    if (this->getPreferQualityOverSpeed()) {
        cinfo->dct_method = JDCT_ISLOW;
    } else {
        cinfo->dct_method = JDCT_IFAST;
    }

    SkBitmap::Config config = this->getPrefConfig(k32Bit_SrcDepth, false);
    if (config != SkBitmap::kARGB_8888_Config &&
        config != SkBitmap::kARGB_4444_Config &&
        config != SkBitmap::kRGB_565_Config) {
        config = SkBitmap::kARGB_8888_Config;
    }

    /* default format is RGB */
    cinfo->out_color_space = JCS_RGB;

#ifdef ANDROID_RGB
    cinfo->dither_mode = JDITHER_NONE;
    if (config == SkBitmap::kARGB_8888_Config) {
        cinfo->out_color_space = JCS_RGBA_8888;
    } else if (config == SkBitmap::kRGB_565_Config) {
        cinfo->out_color_space = JCS_RGB_565;
        if (this->getDitherImage()) {
            cinfo->dither_mode = JDITHER_ORDERED;
        }
    }
#endif
    int startX = rect.fLeft;
    int startY = rect.fTop;
    int width = rect.width();
    int height = rect.height();

    jpeg_init_read_tile_scanline(cinfo, index->index, &startX, &startY, &width, &height);
    int skiaSampleSize = recompute_sampleSize(requestedSampleSize, *cinfo);
    int actualSampleSize = skiaSampleSize * (DCTSIZE / cinfo->min_DCT_scaled_size);

    SkBitmap *bitmap = new SkBitmap;
    SkAutoTDelete<SkBitmap> adb(bitmap);

#ifdef ANDROID_RGB
    /* short-circuit the SkScaledBitmapSampler when possible, as this gives
       a significant performance boost.
    */
    if (skiaSampleSize == 1 &&
        ((config == SkBitmap::kARGB_8888_Config && cinfo->out_color_space == JCS_RGBA_8888) ||
        (config == SkBitmap::kRGB_565_Config && cinfo->out_color_space == JCS_RGB_565)))
    {
        bitmap->setConfig(config, cinfo->output_width, height);
        bitmap->setIsOpaque(true);

        // Check ahead of time if the swap(dest, src) is possible or not.
        // If yes, then we will stick to AllocPixelRef since it's cheaper
        // with the swap happening. If no, then we will use alloc to allocate
        // pixels to prevent garbage collection.
        //
        // Not using a recycled-bitmap and the output rect is same as the
        // decoded region.
        int w = rect.width() / actualSampleSize;
        int h = rect.height() / actualSampleSize;
        bool swapOnly = (rect == region) && bm->isNull() &&
                        (w == bitmap->width()) && (h == bitmap->height()) &&
                        ((startX - rect.x()) / actualSampleSize == 0) &&
                        ((startY - rect.y()) / actualSampleSize == 0);
                        
        if (swapOnly) {
            if (!this->allocPixelRef(bitmap, NULL)) {
                return return_false(*cinfo, *bitmap, "allocPixelRef");
            }
        } else {
            if (!bitmap->allocPixels()) {
                return return_false(*cinfo, *bitmap, "allocPixels");
            }
        }

        SkAutoLockPixels alp(*bitmap);
        JSAMPLE* rowptr = (JSAMPLE*)bitmap->getPixels();
        INT32 const bpr = bitmap->rowBytes();
        int row_total_count = 0;

        #ifdef MTK_JPEG_HW_REGION_RESIZER 
        uint8_t* hwBuffer ;
        if(!fFirstTileDone || fUseHWResizer){
          hwBuffer = (uint8_t*)srcStorage.reset(bitmap->height() * bitmap->rowBytes() );     
          rowptr = hwBuffer ;
        }
        #endif                        


        while (row_total_count < height) {
            int row_count = jpeg_read_tile_scanline(cinfo, index->index, &rowptr);
            // if row_count == 0, then we didn't get a scanline, so abort.
            // if we supported partial images, we might return true in this case
            if (0 == row_count) {
                return return_false(*cinfo, *bitmap, "read_scanlines");
            }
            if (this->shouldCancelDecode()) {
                return return_false(*cinfo, *bitmap, "shouldCancelDecode");
            }
            row_total_count += row_count;
            rowptr += bpr;
        }
        
        #ifdef MTK_JPEG_HW_REGION_RESIZER 
        
        if(!fFirstTileDone || fUseHWResizer)
        {
            unsigned long u4PQOpt;
            unsigned long u4Flag = 0;
            char value[PROPERTY_VALUE_MAX];
            #ifdef MTK_6572DISPLAY_ENHANCEMENT_SUPPORT        
                property_get("persist.PQ", value, "1");
                u4PQOpt = atol(value);
                if(0 != u4PQOpt)
                {
                   u4Flag = this->getPostProcFlag();
                }    
            #endif 
        
            //XLOGD("use hw crop : width height (%d %d)-> (%d %d), L:%d!!\n", width, height, bitmap->width(), bitmap->height(), __LINE__);
            XLOGW("SkRegionJPEG::region crop (%d %d)->(%d %d), region (%d %d %d %d), swap %d, L:%d!!\n", bitmap->width(), bitmap->height(), bm->width(), bm->height()
            ,region.x(), region.y(),region.width(), region.height(),swapOnly,__LINE__);	        
            
            #if 0
            {
                int XOffset = rect.x()/(cinfo->max_h_samp_factor * DCTSIZE)*(cinfo->max_h_samp_factor * DCTSIZE);
                XOffset = XOffset > startX ? (XOffset - startX) : 0;
                {
                    int PaddingWidth = XOffset > actualSampleSize ? actualSampleSize : XOffset;
                    unsigned long u4YIndex , u4XIndex;
                    if(0 < XOffset)
                    {
                        for(u4YIndex = 0 ; u4YIndex < height ; u4YIndex ++)
                        {
                            for(u4XIndex = 1 ; u4XIndex <= PaddingWidth ; u4XIndex ++)
                            {
                                memcpy((hwBuffer + (u4YIndex*width + XOffset - u4XIndex)*bpp) , hwBuffer + (u4YIndex*width + XOffset)*bpp , bpp);
                            }
                        }
                    }
                }
            }
            #endif
            
            int try_times = 5;
            bool result = false;
            do
            {
                result = MDPCrop(hwBuffer, width, height, bitmap, u4Flag);
            
                if(!result && ++try_times < 5)
                {
                    XLOGD("Hardware resize fail, sleep 100 us and then try again, L:%d!!\n", __LINE__);
                    usleep(100*1000);
                }
            }while(!result && try_times < 5);
            
            fFirstTileDone = true;
            if(!result)
            {
                XLOGW("Hardware resize fail, use sw crop, L:%d!!\n", __LINE__);
                rowptr = (JSAMPLE*)bitmap->getPixels();
                memcpy(rowptr, hwBuffer,bitmap->height() * bitmap->rowBytes());
                
            }
            else
            {
                XLOGD("Hardware resize successfully, L:%d!!\n", __LINE__);
                fUseHWResizer = true;
            }
        } 
        #endif //MTK_JPEG_HW_REGION_RESIZER 
        
        if (swapOnly) {
            bm->swap(*bitmap);
        } else {
            cropBitmap(bm, bitmap, actualSampleSize, region.x(), region.y()
            , region.width(), region.height(), startX, startY);
            if (bm->pixelRef() == NULL) {
              XLOGW("SkiaJPEG::cropBitmap allocPixelRef FAIL L:%d !!!!!!\n", __LINE__);			
              return return_false(*cinfo, *bitmap, "cropBitmap Allocate Pixel Fail!! ");
            }  
        }
	
        return true;
    }
#endif
    // check for supported formats
    SkScaledBitmapSampler::SrcConfig sc;
    int bpp;
    if (3 == cinfo->out_color_components && JCS_RGB == cinfo->out_color_space) {
        sc = SkScaledBitmapSampler::kRGB;
        bpp = 3;
#ifdef ANDROID_RGB
    } else if (JCS_RGBA_8888 == cinfo->out_color_space) {
        sc = SkScaledBitmapSampler::kRGBX;
        bpp = 4;
    } else if (JCS_RGB_565 == cinfo->out_color_space) {
        sc = SkScaledBitmapSampler::kRGB_565;
        bpp = 2;
#endif
    } else if (1 == cinfo->out_color_components &&
               JCS_GRAYSCALE == cinfo->out_color_space) {
        sc = SkScaledBitmapSampler::kGray;
        bpp = 1;
    } else {
        return return_false(*cinfo, *bm, "jpeg colorspace");
    }

    SkScaledBitmapSampler sampler(width, height, skiaSampleSize);

    bitmap->setConfig(config, sampler.scaledWidth(), sampler.scaledHeight());
    bitmap->setIsOpaque(true);

    // Check ahead of time if the swap(dest, src) is possible or not.
    // If yes, then we will stick to AllocPixelRef since it's cheaper with the
    // swap happening. If no, then we will use alloc to allocate pixels to
    // prevent garbage collection.
    int w = rect.width() / actualSampleSize;
    int h = rect.height() / actualSampleSize;
    bool swapOnly = (rect == region) && bm->isNull() &&
                    (w == bitmap->width()) && (h == bitmap->height()) &&
                    ((startX - rect.x()) / actualSampleSize == 0) &&
                    ((startY - rect.y()) / actualSampleSize == 0);
    if (swapOnly) {
        if (!this->allocPixelRef(bitmap, NULL)) {
            return return_false(*cinfo, *bitmap, "allocPixelRef");
        }
    } else {
        if (!bitmap->allocPixels()) {
            return return_false(*cinfo, *bitmap, "allocPixels");
        }
    }

    SkAutoLockPixels alp(*bitmap);
    if (!sampler.begin(bitmap, sc, this->getDitherImage())) {
        return return_false(*cinfo, *bitmap, "sampler.begin");
    }

    uint8_t* srcRow = (uint8_t*)srcStorage.reset(width * 4);
	
#ifdef MTK_JPEG_HW_REGION_RESIZER //MTK_JPEG_HW_DECODER
if(!fFirstTileDone || fUseHWResizer)
{
    SkAutoMalloc hwStorage;
    uint8_t* hwBuffer = (uint8_t*)srcStorage.reset(width * height * bpp + 4);

    hwBuffer[width * height * bpp + 4 - 1] = 0xF0;
    hwBuffer[width * height * bpp + 4 - 2] = 0xF0;
    hwBuffer[width * height * bpp + 4 - 3] = 0xF0;
    hwBuffer[width * height * bpp + 4 - 4] = 0xF0;
    int row_total_count = 0;
    int bpr = width * bpp;
    JSAMPLE* rowptr = (JSAMPLE*)hwBuffer;

    unsigned long u4PQOpt;
    unsigned long u4Flag = 0;
    char value[PROPERTY_VALUE_MAX];
#ifdef MTK_6572DISPLAY_ENHANCEMENT_SUPPORT        
    property_get("persist.PQ", value, "1");
    u4PQOpt = atol(value);
    if(0 != u4PQOpt)
    {
       u4Flag = this->getPostProcFlag();
       //if(0 == (0x1 & u4Flag))
       //{
       //    XLOGD("Flag is not 1%x" , u4Flag);
       //}
    }    
#endif    
    
    while (row_total_count < height) {
        int row_count = jpeg_read_tile_scanline(cinfo, index->index, &rowptr);
        // if row_count == 0, then we didn't get a scanline, so abort.
        // if we supported partial images, we might return true in this case
        if (0 == row_count) {
            return return_false(*cinfo, *bitmap, "read_scanlines");
        }
        if (this->shouldCancelDecode()) {
            return return_false(*cinfo, *bitmap, "shouldCancelDecode");
        }
            row_total_count += row_count;
            rowptr += bpr;
    }

#if 1
    int XOffset = rect.x()/(cinfo->max_h_samp_factor * DCTSIZE)*(cinfo->max_h_samp_factor * DCTSIZE);
    XOffset = XOffset > startX ? (XOffset - startX) : 0;
    {
        int PaddingWidth = XOffset > actualSampleSize ? actualSampleSize : XOffset;
        unsigned long u4YIndex , u4XIndex;
        if(0 < XOffset)
        {
            for(u4YIndex = 0 ; u4YIndex < height ; u4YIndex ++)
            {
                for(u4XIndex = 1 ; u4XIndex <= PaddingWidth ; u4XIndex ++)
                {
                    memcpy((hwBuffer + (u4YIndex*width + XOffset - u4XIndex)*bpp) , hwBuffer + (u4YIndex*width + XOffset)*bpp , bpp);
                }
            }
        }
    }
#endif
    XLOGD("use hw resizer : width height (%d %d)-> (%d %d)", width, height, bitmap->width(), bitmap->height());

    int try_times = 5;
    bool result = false;
    do
    {
        result = MDPResizer(hwBuffer, width, height, sc, bitmap, u4Flag);

        if(!result && ++try_times < 5)
        {
            XLOGD("Hardware resize fail, sleep 100 us and then try again ");
            usleep(100*1000);
        }
    }while(!result && try_times < 5);

    
    fFirstTileDone = true;
    if(!result)
    {
        XLOGW("Hardware resize fail, use sw sampler");

        //  Possibly skip initial rows [sampler.srcY0]
        row_total_count = 0;
        rowptr = (JSAMPLE*)hwBuffer;
        rowptr += (bpr * sampler.srcY0());
        row_total_count += sampler.srcY0();
        for (int y = 0;; y++) {

            if (this->shouldCancelDecode()) {
                return return_false(*cinfo, *bitmap, "shouldCancelDecode");
            }

            sampler.next(rowptr);
            if (bitmap->height() - 1 == y) {
                // we're done
                XLOGD("total row count %d\n", row_total_count);
                break;
            }
            rowptr += bpr;
            row_total_count ++;

            rowptr += (bpr * (sampler.srcDY() - 1));
            row_total_count += (sampler.srcDY() - 1);
        }
        
    }
    else
    {
        XLOGD("Hardware resize successfully ");
        fUseHWResizer = true;
    }
} else {
#endif
    //  Possibly skip initial rows [sampler.srcY0]
    if (!skip_src_rows_tile(cinfo, index->index, srcRow, sampler.srcY0())) {
        return return_false(*cinfo, *bitmap, "skip rows");
    }

    // now loop through scanlines until y == bitmap->height() - 1
    for (int y = 0;; y++) {
        JSAMPLE* rowptr = (JSAMPLE*)srcRow;
        int row_count = jpeg_read_tile_scanline(cinfo, index->index, &rowptr);
        if (0 == row_count) {
            return return_false(*cinfo, *bitmap, "read_scanlines");
        }
        if (this->shouldCancelDecode()) {
            return return_false(*cinfo, *bitmap, "shouldCancelDecode");
        }

        sampler.next(srcRow);
        if (bitmap->height() - 1 == y) {
            // we're done
            break;
        }

        if (!skip_src_rows_tile(cinfo, index->index, srcRow, sampler.srcDY() - 1)) {
            return return_false(*cinfo, *bitmap, "skip rows");
        }
    }

#ifdef MTK_JPEG_HW_REGION_RESIZER //MTK_JPEG_HW_DECODER
}
#endif

	
    if (swapOnly) {
        bm->swap(*bitmap);
    } else {
        cropBitmap(bm, bitmap, actualSampleSize, region.x(), region.y(),
                   region.width(), region.height(), startX, startY);
        if (bm->pixelRef() == NULL) {
          XLOGW("SkiaJPEG::cropBitmap allocPixelRef FAIL L:%d !!!!!!\n", __LINE__);			
          return return_false(*cinfo, *bitmap, "cropBitmap Allocate Pixel Fail!! ");
        }       
    }
    return true;
}

///////////////////////////////////////////////////////////////////////////////

#include "SkColorPriv.h"

// taken from jcolor.c in libjpeg
#if 0   // 16bit - precise but slow
    #define CYR     19595   // 0.299
    #define CYG     38470   // 0.587
    #define CYB      7471   // 0.114

    #define CUR    -11059   // -0.16874
    #define CUG    -21709   // -0.33126
    #define CUB     32768   // 0.5

    #define CVR     32768   // 0.5
    #define CVG    -27439   // -0.41869
    #define CVB     -5329   // -0.08131

    #define CSHIFT  16
#else      // 8bit - fast, slightly less precise
    #define CYR     77    // 0.299
    #define CYG     150    // 0.587
    #define CYB      29    // 0.114

    #define CUR     -43    // -0.16874
    #define CUG    -85    // -0.33126
    #define CUB     128    // 0.5

    #define CVR      128   // 0.5
    #define CVG     -107   // -0.41869
    #define CVB      -21   // -0.08131

    #define CSHIFT  8
#endif

static void rgb2yuv_32(uint8_t dst[], SkPMColor c) {
    int r = SkGetPackedR32(c);
    int g = SkGetPackedG32(c);
    int b = SkGetPackedB32(c);

    int  y = ( CYR*r + CYG*g + CYB*b ) >> CSHIFT;
    int  u = ( CUR*r + CUG*g + CUB*b ) >> CSHIFT;
    int  v = ( CVR*r + CVG*g + CVB*b ) >> CSHIFT;

    dst[0] = SkToU8(y);
    dst[1] = SkToU8(u + 128);
    dst[2] = SkToU8(v + 128);
}

static void rgb2yuv_4444(uint8_t dst[], U16CPU c) {
    int r = SkGetPackedR4444(c);
    int g = SkGetPackedG4444(c);
    int b = SkGetPackedB4444(c);

    int  y = ( CYR*r + CYG*g + CYB*b ) >> (CSHIFT - 4);
    int  u = ( CUR*r + CUG*g + CUB*b ) >> (CSHIFT - 4);
    int  v = ( CVR*r + CVG*g + CVB*b ) >> (CSHIFT - 4);

    dst[0] = SkToU8(y);
    dst[1] = SkToU8(u + 128);
    dst[2] = SkToU8(v + 128);
}

static void rgb2yuv_16(uint8_t dst[], U16CPU c) {
#ifndef ANDROID_DEFAULT_CODE
    // use precise computation to get better color transform result
    int r = SkPacked16ToR32(c);
    int g = SkPacked16ToG32(c);
    int b = SkPacked16ToB32(c);

    int  y = ( CYR*r + CYG*g + CYB*b ) >> (CSHIFT);
    int  u = ( CUR*r + CUG*g + CUB*b ) >> (CSHIFT);
    int  v = ( CVR*r + CVG*g + CVB*b ) >> (CSHIFT);
#else
    int r = SkGetPackedR16(c);
    int g = SkGetPackedG16(c);
    int b = SkGetPackedB16(c);

    int  y = ( 2*CYR*r + CYG*g + 2*CYB*b ) >> (CSHIFT - 2);
    int  u = ( 2*CUR*r + CUG*g + 2*CUB*b ) >> (CSHIFT - 2);
    int  v = ( 2*CVR*r + CVG*g + 2*CVB*b ) >> (CSHIFT - 2);
#endif

    dst[0] = SkToU8(y);
    dst[1] = SkToU8(u + 128);
    dst[2] = SkToU8(v + 128);
}

///////////////////////////////////////////////////////////////////////////////

typedef void (*WriteScanline)(uint8_t* SK_RESTRICT dst,
                              const void* SK_RESTRICT src, int width,
                              const SkPMColor* SK_RESTRICT ctable);

static void Write_32_YUV(uint8_t* SK_RESTRICT dst,
                         const void* SK_RESTRICT srcRow, int width,
                         const SkPMColor*) {
    const uint32_t* SK_RESTRICT src = (const uint32_t*)srcRow;
    while (--width >= 0) {
#ifdef WE_CONVERT_TO_YUV
        rgb2yuv_32(dst, *src++);
#else
        uint32_t c = *src++;
        dst[0] = SkGetPackedR32(c);
        dst[1] = SkGetPackedG32(c);
        dst[2] = SkGetPackedB32(c);
#endif
        dst += 3;
    }
}

static void Write_4444_YUV(uint8_t* SK_RESTRICT dst,
                           const void* SK_RESTRICT srcRow, int width,
                           const SkPMColor*) {
    const SkPMColor16* SK_RESTRICT src = (const SkPMColor16*)srcRow;
    while (--width >= 0) {
#ifdef WE_CONVERT_TO_YUV
        rgb2yuv_4444(dst, *src++);
#else
        SkPMColor16 c = *src++;
        dst[0] = SkPacked4444ToR32(c);
        dst[1] = SkPacked4444ToG32(c);
        dst[2] = SkPacked4444ToB32(c);
#endif
        dst += 3;
    }
}

static void Write_16_YUV(uint8_t* SK_RESTRICT dst,
                         const void* SK_RESTRICT srcRow, int width,
                         const SkPMColor*) {
    const uint16_t* SK_RESTRICT src = (const uint16_t*)srcRow;
    while (--width >= 0) {
#ifdef WE_CONVERT_TO_YUV
        rgb2yuv_16(dst, *src++);
#else
        uint16_t c = *src++;
        dst[0] = SkPacked16ToR32(c);
        dst[1] = SkPacked16ToG32(c);
        dst[2] = SkPacked16ToB32(c);
#endif
        dst += 3;
    }
}

static void Write_Index_YUV(uint8_t* SK_RESTRICT dst,
                            const void* SK_RESTRICT srcRow, int width,
                            const SkPMColor* SK_RESTRICT ctable) {
    const uint8_t* SK_RESTRICT src = (const uint8_t*)srcRow;
    while (--width >= 0) {
#ifdef WE_CONVERT_TO_YUV
        rgb2yuv_32(dst, ctable[*src++]);
#else
        uint32_t c = ctable[*src++];
        dst[0] = SkGetPackedR32(c);
        dst[1] = SkGetPackedG32(c);
        dst[2] = SkGetPackedB32(c);
#endif
        dst += 3;
    }
}

static WriteScanline ChooseWriter(const SkBitmap& bm) {
    switch (bm.config()) {
        case SkBitmap::kARGB_8888_Config:
            return Write_32_YUV;
        case SkBitmap::kRGB_565_Config:
            return Write_16_YUV;
        case SkBitmap::kARGB_4444_Config:
            return Write_4444_YUV;
        case SkBitmap::kIndex8_Config:
            return Write_Index_YUV;
        default:
            return NULL;
    }
}

class SkJPEGImageEncoder : public SkImageEncoder {
protected:
    virtual bool onEncode(SkWStream* stream, const SkBitmap& bm, int quality) {
#ifdef TIME_ENCODE
        AutoTimeMillis atm("JPEG Encode");
#endif

        const WriteScanline writer = ChooseWriter(bm);
        if (NULL == writer) {
            return false;
        }

        SkAutoLockPixels alp(bm);
        if (NULL == bm.getPixels()) {
            return false;
        }

        jpeg_compress_struct    cinfo;
        skjpeg_error_mgr        sk_err;
        skjpeg_destination_mgr  sk_wstream(stream);

        // allocate these before set call setjmp
        SkAutoMalloc    oneRow;
        SkAutoLockColors ctLocker;

        cinfo.err = jpeg_std_error(&sk_err);
        sk_err.error_exit = skjpeg_error_exit;
        if (setjmp(sk_err.fJmpBuf)) {
            return false;
        }
        jpeg_create_compress(&cinfo);

        cinfo.dest = &sk_wstream;
        cinfo.image_width = bm.width();
        cinfo.image_height = bm.height();

        //Special opt for ARGB8888, do color convert in libjpeg
        //Fix me, ARGB8888 input is not supported in android default libjpeg
        if(bm.config() == SkBitmap::kARGB_8888_Config)
        {
            cinfo.input_components = 4;
            cinfo.in_color_space = JCS_RGBA_8888;
            cinfo.input_gamma = 1;

            jpeg_set_defaults(&cinfo);
            jpeg_set_quality(&cinfo, quality, TRUE /* limit to baseline-JPEG values */);
            cinfo.dct_method = JDCT_IFAST;

            jpeg_start_compress(&cinfo, TRUE);

            const char*  srcRow = (const char*)bm.getPixels();

            while (cinfo.next_scanline < cinfo.image_height) {
                JSAMPROW row_pointer[1];    // pointer to JSAMPLE row[s]  //one element point to one row

                row_pointer[0] = (JSAMPROW)srcRow;
                (void) jpeg_write_scanlines(&cinfo, row_pointer, 1);
                srcRow = srcRow + bm.rowBytes();
            }
        }
        else
        {
            cinfo.input_components = 3;
#ifdef WE_CONVERT_TO_YUV
            cinfo.in_color_space = JCS_YCbCr;
#else
            cinfo.in_color_space = JCS_RGB;
#endif
            cinfo.input_gamma = 1;

            jpeg_set_defaults(&cinfo);
            jpeg_set_quality(&cinfo, quality, TRUE /* limit to baseline-JPEG values */);
            cinfo.dct_method = JDCT_IFAST;

            jpeg_start_compress(&cinfo, TRUE);

            const int       width = bm.width();
            uint8_t*        oneRowP = (uint8_t*)oneRow.reset(width * 3);

            const SkPMColor* colors = ctLocker.lockColors(bm);
            const void*      srcRow = bm.getPixels();

            while (cinfo.next_scanline < cinfo.image_height) {
                JSAMPROW row_pointer[1];    /* pointer to JSAMPLE row[s] */

                writer(oneRowP, srcRow, width, colors);
                row_pointer[0] = oneRowP;
                (void) jpeg_write_scanlines(&cinfo, row_pointer, 1);
                srcRow = (const void*)((const char*)srcRow + bm.rowBytes());
            }
        }

        jpeg_finish_compress(&cinfo);
        jpeg_destroy_compress(&cinfo);

        return true;
    }
};

///////////////////////////////////////////////////////////////////////////////

#include "SkTRegistry.h"

static SkImageDecoder* DFactory(SkStream* stream) {
    static const char gHeader[] = { 0xFF, 0xD8, 0xFF };
    static const size_t HEADER_SIZE = sizeof(gHeader);

    char buffer[HEADER_SIZE];
    size_t len = stream->read(buffer, HEADER_SIZE);

    if (len != HEADER_SIZE) {
        return NULL;   // can't read enough
    }
    if (memcmp(buffer, gHeader, HEADER_SIZE)) {
        return NULL;
    }
    return SkNEW(SkJPEGImageDecoder);
}

static SkImageEncoder* EFactory(SkImageEncoder::Type t) {
    return (SkImageEncoder::kJPEG_Type == t) ? SkNEW(SkJPEGImageEncoder) : NULL;
}

static SkTRegistry<SkImageDecoder*, SkStream*> gDReg(DFactory);
static SkTRegistry<SkImageEncoder*, SkImageEncoder::Type> gEReg(EFactory);
