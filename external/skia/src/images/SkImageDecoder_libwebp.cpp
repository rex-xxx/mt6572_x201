/*
 * Copyright 2010, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "SkImageDecoder.h"
#include "SkImageEncoder.h"
#include "SkColorPriv.h"
#include "SkScaledBitmapSampler.h"
#include "SkStream.h"
#include "SkTemplates.h"
#include "SkUtils.h"
#include "SkTScopedPtr.h"

#include <sys/mman.h>
#include <cutils/ashmem.h>

#ifdef MTK_WEBP_HW_DECODER
#include "MediaHal.h"
#endif

// A WebP decoder only, on top of (subset of) libwebp
// For more information on WebP image format, and libwebp library, see:
//   http://code.google.com/speed/webp/
//   http://www.webmproject.org/code/#libwebp_webp_image_decoder_library
//   http://review.webmproject.org/gitweb?p=libwebp.git

#include <stdio.h>
extern "C" {
// If moving libwebp out of skia source tree, path for webp headers must be
// updated accordingly. Here, we enforce using local copy in webp sub-directory.
#include "webp/decode.h"
#include "webp/encode.h"
}




#ifdef SK_BUILD_FOR_ANDROID
#include <cutils/properties.h>
#include <cutils/xlog.h>

#undef LOG_TAG
#define LOG_TAG "skia" 

#endif

#ifdef ANDROID
#include <cutils/properties.h>

// Key to lookup the size of memory buffer set in system property
static const char KEY_MEM_CAP[] = "ro.media.dec.webp.memcap";
#endif

// this enables timing code to report milliseconds for a decode
//#define TIME_DECODE

//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

// Define VP8 I/O on top of Skia stream

//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

static const size_t WEBP_VP8_HEADER_SIZE = 64;
static const size_t WEBP_IDECODE_BUFFER_SZ = (1 << 16);


//=======================================================================
// MTK Define
#define MAX_HEADER_SIZE 64 * 1024
#define USE_SKWEBPSTREAM 

// MTK define End
//=======================================================================
//MTK function start


#ifdef MTK_WEBP_HW_DECODER

//#define STREAM_DEBUG		  			

#ifdef USE_SKWEBPSTREAM

class SkWebpStream : public SkStream {

public:


	SkWebpStream(void *hw_buffer, size_t hw_buffer_size, SkStream* Src){
	  XLOGD("SkWebpStream::SkWebpStream %x, %x, %x, length %x!!\n", (unsigned int) hw_buffer, hw_buffer_size, (unsigned int)Src, Src->getLength());  
      srcStream = Src ;
	  hwInputBuf = hw_buffer;
	  hwInputBufSize = hw_buffer_size;
	  total_read_size = 0;
	  
    }

	virtual ~SkWebpStream(){
	  XLOGD("SkWebpStream::~SkWebpStream!!\n");		
	}

	
    virtual bool rewind(){
	  XLOGD("SkWebpStream::rewind, readSize %x, hwBuffSize %x!!\n",   total_read_size, hwInputBufSize);				
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
		unsigned char *base ;
#ifdef STREAM_DEBUG
		XLOGD("SkWebpStream::read, buf %x, size %x, tSize %x, st %x, end %x, HWsize %x!!\n", (unsigned int)buffer, (unsigned int) size
			, total_read_size, read_start, read_end, hwInputBufSize);			   
#endif
		if (buffer == NULL && size == 0){	// special signature, they want the total size
		    XLOGD("SkWebpStream::getLength, HWsize %x, stream_size %x!!\n", hwInputBufSize, srcStream->getLength());
		   if(srcStream->getLength() == hwInputBufSize)
		     fSize = hwInputBufSize ;
 		   else if(srcStream->getLength() > hwInputBufSize)
		     fSize = srcStream->getLength() ;
		   else
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
#ifdef STREAM_DEBUG		  			
		    XLOGD("SkWebpStream::read(HW), size %x, total_size %x!!\n", size, total_read_size);			   		  					  			
#endif
		    return size ;					  

		}else if ( read_start >= hwInputBufSize  ){
          real_size_2 = srcStream->read(buffer, size);
		  total_read_size += real_size_2 ;
#ifdef STREAM_DEBUG		  
		  XLOGD("SkWebpStream::read(Stream), size_2 %x, real_size %x(%x), total_size %x!!\n", size, real_size_2, srcStream->getLength(),total_read_size);
#endif 
          return real_size_2;
		}else{
      size_1 = hwInputBufSize - read_start ;
      size_2 = read_end - hwInputBufSize  ;	
		  if (buffer) {
            memcpy(buffer, (const char*)hwInputBuf + read_start, size_1);
            //base = (unsigned char*)(buffer+size_1-4) ;
            //SkDebugf("BUF_DUMP:dst [%x], ofst %x %x %x %x %x %x %x %x, L:%d!!\n ", base, *base, *(base+1), *(base+2), *(base+3), *(base+4), *(base+5), *(base+6), *(base+7), __LINE__);
            //base = (unsigned char*)(hwInputBuf + read_start +size_1-4) ;               
            //SkDebugf("BUF_DUMP:src [%x], ofst %x %x %x %x %x %x %x %x, L:%d!!\n ", base, *base, *(base+1), *(base+2), *(base+3), *(base+4), *(base+5), *(base+6), *(base+7), __LINE__);
		  }
		  total_read_size += size_1 ;
		  real_size_2 = srcStream->read(buffer+size_1, size_2);		  
      
      //base = (unsigned char*) (buffer+size_1-4) ;
      //SkDebugf("BUF_DUMP:dst [%x], ofst %x %x %x %x %x %x %x %x, L:%d!!\n ", base, *base, *(base+1), *(base+2), *(base+3), *(base+4), *(base+5), *(base+6), *(base+7), __LINE__);
      //base = (unsigned char*)(hwInputBuf + read_start +size_1-4) ;                
      //SkDebugf("BUF_DUMP:src [%x], ofst %x %x %x %x %x %x %x %x, L:%d!!\n ", base, *base, *(base+1), *(base+2), *(base+3), *(base+4), *(base+5), *(base+6), *(base+7), __LINE__);
		  
		  total_read_size += real_size_2 ;
		  XLOGD("SkWebpStream::read(HW+Stream), buf %x, size_2 %x, real_size %x(%x), total_size %x!!\n", buffer+size_1, size_2, real_size_2, srcStream->getLength(),total_read_size);  
		  return size_1+ real_size_2 ;		

		}



	}

	size_t seek(size_t offset){
	    XLOGD("SkWebpStream::seek size %x!!\n", offset);			
		return 0;
	}
	size_t skip(size_t size)
	{
		/*	Check for size == 0, and just return 0. If we passed that
			to read(), it would interpret it as a request for the entire
			size of the stream.
		*/
        XLOGD("SkWebpStream::skip %x!!\n", size);					
		return size ? this->read(NULL, size) : 0;
	}




private:	
  size_t total_read_size ;
  SkStream* srcStream;
  void *hwInputBuf ;
  size_t hwInputBufSize ; 
  size_t fSize;
  



};



class WebpStreamAutoClean {
public:
    WebpStreamAutoClean(): ptr(NULL) {}
    ~WebpStreamAutoClean() {
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




#endif

 
//MTK function End
//========================================================================

// Parse headers of RIFF container, and check for valid Webp (VP8) content.
static bool webp_parse_header(SkStream* stream, int* width, int* height,
                              int* alpha) {
    unsigned char buffer[WEBP_VP8_HEADER_SIZE];
    const uint32_t contentSize = stream->getLength();
    const size_t len = stream->read(buffer, WEBP_VP8_HEADER_SIZE);
    const uint32_t read_bytes = (contentSize < WEBP_VP8_HEADER_SIZE) ?
        contentSize : WEBP_VP8_HEADER_SIZE;
    if (len != read_bytes) {
        return false; // can't read enough
    }

    WebPBitstreamFeatures features;
    VP8StatusCode status = WebPGetFeatures(buffer, read_bytes, &features);
    if (status != VP8_STATUS_OK) {
        return false; // Invalid WebP file.
    }
    *width = features.width;
    *height = features.height;
    *alpha = features.has_alpha;

    // sanity check for image size that's about to be decoded.
    {
        Sk64 size;
        size.setMul(*width, *height);
        if (size.isNeg() || !size.is32()) {
            //SkDebugf("webpSwDec: invalid size (%d %d) %d %d , L:%d !!\n", *width, *height,size.isNeg(),size.is32(), __LINE__);
            return false;
        }
        // now check that if we are 4-bytes per pixel, we also don't overflow
        if (size.get32() > (0x7FFFFFFF >> 2)) {
            //SkDebugf("webpSwDec: invalid size(%d %d) %d, L:%d !!\n", *width, *height, size.get32(),__LINE__);
            return false;
        }
    }
    return true;
}

class SkWEBPImageDecoder: public SkImageDecoder {
public:
    virtual Format getFormat() const {
        return kWEBP_Format;
    }

protected:
    virtual bool onBuildTileIndex(SkStream *stream, int *width, int *height);
    virtual bool onDecodeRegion(SkBitmap* bitmap, SkIRect rect);
    virtual bool onDecode(SkStream* stream, SkBitmap* bm, Mode);

private:
    bool setDecodeConfig(SkBitmap* decodedBitmap, int width, int height);
    SkStream *inputStream;
    int origWidth;
    int origHeight;
    int hasAlpha;

#ifdef MTK_WEBP_HW_DECODER
    SkAutoMalloc  fAllocator;
    uint8_t* fSrc;
    uint32_t fSize;
    //bool fInitRegion;
    //bool fFirstTileDone;
    //bool fUseHWResizer;
    bool onDecodeParser(SkBitmap* bm, Mode, uint8_t* srcBuffer, uint32_t srcSize, int srcFD, uint32_t *srcW, uint32_t *srcH);
    //bool onRangeDecodeParser(uint8_t* srcBuffer, uint32_t srcSize, int *width, int *height, bool doRelease);
    bool onDecodeHW(SkBitmap* bm, uint8_t* srcBuffer, uint32_t srcBufSize, uint32_t srcSize, int srcFD);
    //bool onDecodeHWRegion(SkBitmap* bm, SkIRect region, uint8_t* srcBuffer, uint32_t srcSize);
#endif    
    
    
};

//////////////////////////////////////////////////////////////////////////

#include "SkTime.h"

class AutoTimeMillis {
public:
    AutoTimeMillis(const char label[]) :
        fLabel(label) {
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
    SkMSec fNow;
};

///////////////////////////////////////////////////////////////////////////////

// This guy exists just to aid in debugging, as it allows debuggers to just
// set a break-point in one place to see all error exists.
static bool return_false(const SkBitmap& bm, const char msg[]) {
#if 0
    SkDebugf("libwebp error %s [%d %d]", msg, bm.width(), bm.height());
#endif
    return false; // must always return false
}

static WEBP_CSP_MODE webp_decode_mode(SkBitmap* decodedBitmap, int hasAlpha) {
    WEBP_CSP_MODE mode = MODE_LAST;
    SkBitmap::Config config = decodedBitmap->config();
    // For images that have alpha, choose appropriate color mode (MODE_rgbA,
    // MODE_rgbA_4444) that pre-multiplies RGB pixel values with transparency
    // factor (alpha).
    if (config == SkBitmap::kARGB_8888_Config) {
      mode = hasAlpha ? MODE_rgbA : MODE_RGBA;
    } else if (config == SkBitmap::kARGB_4444_Config) {
      mode = hasAlpha ? MODE_rgbA_4444 : MODE_RGBA_4444;
    } else if (config == SkBitmap::kRGB_565_Config) {
      mode = MODE_RGB_565;
    }
    SkASSERT(mode != MODE_LAST);
    return mode;
}

// Incremental WebP image decoding. Reads input buffer of 64K size iteratively
// and decodes this block to appropriate color-space as per config object.
static bool webp_idecode(SkStream* stream, WebPDecoderConfig& config) {
	
	//XLOGD("webp_decoder stream %x, go L:%d!!!\n",(unsigned int)stream,__LINE__);	   

    WebPIDecoder* idec = WebPIDecode(NULL, 0, &config);
    if (idec == NULL) {
        WebPFreeDecBuffer(&config.output);
        return false;
    }

    stream->rewind();
    const uint32_t contentSize = stream->getLength();
    const uint32_t read_buffer_size = (contentSize < WEBP_IDECODE_BUFFER_SZ) ?
        contentSize : WEBP_IDECODE_BUFFER_SZ;
    SkAutoMalloc srcStorage(read_buffer_size);
    unsigned char* input = (uint8_t*)srcStorage.get();
    if (input == NULL) {
        WebPIDelete(idec);
        WebPFreeDecBuffer(&config.output);
        return false;
    }

    uint32_t bytes_remaining = contentSize;
    while (bytes_remaining > 0) {
        const uint32_t bytes_to_read =
            (bytes_remaining < WEBP_IDECODE_BUFFER_SZ) ?
                bytes_remaining : WEBP_IDECODE_BUFFER_SZ;

        const size_t bytes_read = stream->read(input, bytes_to_read);
        if (bytes_read == 0) {
         	//XLOGD("webp_decoder read %x, go L:%d!!!\n", bytes_to_read,__LINE__);	  			
            break;
        }

        VP8StatusCode status = WebPIAppend(idec, input, bytes_read);
        if (status == VP8_STATUS_OK || status == VP8_STATUS_SUSPENDED) {
    	      //XLOGD("webp_decoder decode status %d, remian %x, read %x, L:%d!!!\n",status, bytes_remaining, bytes_read,__LINE__);	
            bytes_remaining -= bytes_read;
        } else {
            break;
        }
    }
    srcStorage.free();
    WebPIDelete(idec);
    WebPFreeDecBuffer(&config.output);

    if (bytes_remaining > 0) {
    	  XLOGD("webp_decoder finish bytes_remaining %x, content %x, L:%d!!!\n",bytes_remaining, contentSize,__LINE__);		
        return false;
    } else {
    	  XLOGD("webp_decoder finish successfully, L:%d!!!\n",__LINE__);		
        return true;
    }
}

static bool webp_get_config_resize(WebPDecoderConfig& config,
                                   SkBitmap* decodedBitmap,
                                   int width, int height, int hasAlpha) {
    WEBP_CSP_MODE mode = webp_decode_mode(decodedBitmap, hasAlpha);
    if (mode == MODE_LAST) {
        return false;
    }

    if (WebPInitDecoderConfig(&config) == 0) {
        return false;
    }

    config.output.colorspace = mode;
    config.output.u.RGBA.rgba = (uint8_t*)decodedBitmap->getPixels();
    config.output.u.RGBA.stride = decodedBitmap->rowBytes();
    config.output.u.RGBA.size = decodedBitmap->getSize();
    config.output.is_external_memory = 1;

    if (width != decodedBitmap->width() ||
        height != decodedBitmap->height()) {
        config.options.use_scaling = 1;
        config.options.scaled_width = decodedBitmap->width();
        config.options.scaled_height = decodedBitmap->height();
    }

    return true;
}

static bool webp_get_config_resize_crop(WebPDecoderConfig& config,
                                        SkBitmap* decodedBitmap,
                                        SkIRect region, int hasAlpha) {

    if (!webp_get_config_resize(
        config, decodedBitmap, region.width(), region.height(), hasAlpha)) {
      return false;
    }

    config.options.use_cropping = 1;
    config.options.crop_left = region.fLeft;
    config.options.crop_top = region.fTop;
    config.options.crop_width = region.width();
    config.options.crop_height = region.height();

    return true;
}

#ifdef MTK_WEBP_HW_DECODER

static int skjdiv_round_up (unsigned int a, unsigned int b)/* Compute a/b rounded up to next integer, ie, ceil(a/b) *//* Assumes a >= 0, b > 0 */
{  
   return (a + b - 1) / b;
}


bool SkWEBPImageDecoder::onDecodeParser(SkBitmap* bm, Mode mode, uint8_t* srcBuffer, uint32_t srcSize, int srcFD, uint32_t *srcW, uint32_t *srcH)
{
    int width, height;
    int sampleSize = this->getSampleSize();
    int preferSize = 0;    //this->getPreferSize();
    MHAL_JPEG_DEC_INFO_OUT  outInfo;
    MHAL_JPEG_DEC_SRC_IN    srcInfo;

    unsigned int cinfo_output_width, cinfo_output_height;
	int re_sampleSize ;
    //sampleSize = roundToTwoPower(sampleSize);
     
    
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
        result = mHalJpeg(MHAL_IOCTL_WEBP_DEC_PARSER, (void *)&srcInfo, sizeof(srcInfo), NULL, 0, NULL);
        if(result == MHAL_INVALID_RESOURCE && try_times < 5)
        {
            XLOGD("onHWParser : HW busy ! Sleep 10ms & try again");
            usleep(10 * 1000);
        }
        else if (MHAL_NO_ERROR != result)
        {
            return false;
        }
    } while(result == MHAL_INVALID_RESOURCE && try_times < 5);
         

    // get file dimension
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_WEBP_DEC_GET_INFO, NULL, 0, 
                                   (void *)&outInfo, sizeof(outInfo), NULL))
    {
        if (SkImageDecoder::kDecodeBounds_Mode != mode) {
            XLOGW("mHalJpeg() - WEBP Decoder false get information!!\n");
        }
        return false;
    }
    *srcW = outInfo.srcWidth;
    *srcH = outInfo.srcHeight ;
    
    
#if 1 
    if (preferSize == 0)
    {
      int dx = 0;
      int dy = 0;
#if 0      
	     //if(sampleSize <= 8 ) // 1, 2, 4, 8
		   {
		   	cinfo_output_width = skjdiv_round_up(outInfo.srcWidth, sampleSize);
		   	cinfo_output_height = skjdiv_round_up(outInfo.srcHeight, sampleSize);
       
		   }
		   //else //  use 8
		   //{
       //        cinfo_output_width = skjdiv_round_up(outInfo.srcWidth, 8);
       //        cinfo_output_height = skjdiv_round_up(outInfo.srcHeight, 8);
		   //}
		   
		   re_sampleSize = sampleSize * cinfo_output_width / outInfo.srcWidth;
#endif          
		   	cinfo_output_width  = outInfo.srcWidth ;
		   	cinfo_output_height = outInfo.srcHeight;                 
        re_sampleSize = sampleSize ;
       
       
       if( re_sampleSize != 1 )
		   {
             dx = (re_sampleSize > cinfo_output_width )? cinfo_output_width : re_sampleSize ;
             dy = (re_sampleSize > cinfo_output_height )? cinfo_output_height : re_sampleSize ;
       
        
	     
             width  = cinfo_output_width / dx;  
             height = cinfo_output_height / dy; 
           
       
		   }
		   else
		   {
		     width = cinfo_output_width ;
		     height = cinfo_output_height ;
       
		   }
	
		   SkDebugf("webpSwDec:: resample %d->%d, %d=%d/%d, %d=%d/%d !!\n", sampleSize, re_sampleSize
		   , width , cinfo_output_width , dx, height, cinfo_output_height, dy);
#if 0
        width  = outInfo.srcWidth / sampleSize;
        height = outInfo.srcHeight / sampleSize;
        if(outInfo.srcWidth % sampleSize != 0) width++;
        if(outInfo.srcHeight % sampleSize != 0) height++;
#endif		
    }
//    else
//    {
//        int maxDimension = (outInfo.srcWidth > outInfo.srcHeight) ? outInfo.srcWidth : outInfo.srcHeight;
//        
//        if (maxDimension <= preferSize)
//        {
//            width  = outInfo.srcWidth / sampleSize;
//            height = outInfo.srcHeight / sampleSize;
//        }
//        else if (outInfo.srcWidth > outInfo.srcHeight)
//        {
//            width = preferSize;
//            height = (int)outInfo.srcHeight * width / (int)outInfo.srcWidth;
//        }
//        else
//        {
//            height = preferSize;
//            width = (int)outInfo.srcWidth * height / (int)outInfo.srcHeight;
//        }
//    }    
	if( re_sampleSize != 1  )
    XLOGD("onHWParser pSize %d, src %d %d, dst %d %d(%d %d), sample %d->%d!\n", preferSize, outInfo.srcWidth, outInfo.srcHeight,
		width, height,cinfo_output_width, cinfo_output_height, sampleSize, re_sampleSize);	

     
#if 0
    bm->lockPixels();
    void* rowptr = bm->getPixels();
    bm->unlockPixels();
    bool reuseBitmap = (rowptr != NULL);


    if(reuseBitmap)
    {
        if((bm->width() != width) || (bm->height() != height) || (bm->config() != config))
        {
            if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_WEBP_DEC_START, NULL, 0, NULL, 0, NULL))
            {
                XLOGW("Can not release WEBP HW Decoder\n");
                return false;
            }
            XLOGW("Reuse bitmap but dimensions not match\n");
            return false;            
        }
    }
    else 
#endif      
    {
        bm->setConfig(config, width, height);
        bm->setIsOpaque(true);
    }
    
    if (SkImageDecoder::kDecodeBounds_Mode == mode) {
        if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_WEBP_DEC_START, NULL, 0, NULL, 0, NULL))
        {
            XLOGW("Can not release WEBP HW Decoder\n");
            return false;
        }
        return true;    
    //} else if(width <= 128 && height <= 128) {
    //    mHalJpeg(MHAL_IOCTL_WEBP_DEC_START, NULL, 0, NULL, 0, NULL);
    //    return false;
    }
#endif
    XLOGD("The file input width: %d, height: %d, output width: %d, height: %d, format: %d, prefer size: %d, dither: %d\n", 
           outInfo.srcWidth, outInfo.srcHeight, width, height, config, preferSize, getDitherImage());

    return true;
}



bool SkWEBPImageDecoder::onDecodeHW(SkBitmap* bm, uint8_t* srcBuffer, uint32_t srcBufSize, uint32_t srcSize, int srcFD)
{
    MHAL_JPEG_DEC_START_IN inParams;


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
#if 0
    bm->lockPixels();
    JSAMPLE* rowptr = (JSAMPLE*)bm->getPixels();
    bool reuseBitmap = (rowptr != NULL);
    bm->unlockPixels();

    if(!reuseBitmap) 
#endif      
    {
        if (!this->allocPixelRef(bm, NULL)) {
            if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_WEBP_DEC_START, NULL, 0, NULL, 0, NULL))
            {
                XLOGW("Can not release WEBP HW Decoder\n");
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

    // start decode
    SkAutoLockPixels alp(*bm);
    XLOGW("Skia WEBP HW Decoder trigger!!\n");
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_WEBP_DEC_START, 
                                   (void *)&inParams, sizeof(inParams), 
                                   NULL, 0, NULL))
    {
        //SkDebugf("WEBP HW not support this image\n");
        //if(!reuseBitmap)
        //    bm->setPixels(NULL, NULL);
        XLOGW("WEBP HW Decoder return Fail!!\n");
        return false;
    }
#if 0    
    if (reuseBitmap) {
        bm->notifyPixelsChanged();
    }
#endif    
    XLOGW("WEBP HW Decoder return Successfully!!\n");
    return true;
}

#endif

bool SkWEBPImageDecoder::setDecodeConfig(SkBitmap* decodedBitmap,
                                         int width, int height) {
    SkBitmap::Config config = this->getPrefConfig(k32Bit_SrcDepth, hasAlpha);

    // YUV converter supports output in RGB565, RGBA4444 and RGBA8888 formats.
    if (hasAlpha) {
        if (config != SkBitmap::kARGB_4444_Config) {
            config = SkBitmap::kARGB_8888_Config;
        }
    } else {
        if (config != SkBitmap::kRGB_565_Config &&
            config != SkBitmap::kARGB_4444_Config) {
            config = SkBitmap::kARGB_8888_Config;
        }
    }

    if (!this->chooseFromOneChoice(config, width, height)) {
        return false;
    }

    decodedBitmap->setConfig(config, width, height, 0);

    decodedBitmap->setIsOpaque(!hasAlpha);

    return true;
}

bool SkWEBPImageDecoder::onBuildTileIndex(SkStream* stream,
                                          int *width, int *height) {
    int origWidth, origHeight, hasAlpha;
    if (!webp_parse_header(stream, &origWidth, &origHeight, &hasAlpha)) {
        return false;
    }

    stream->rewind();
    *width = origWidth;
    *height = origHeight;

    this->inputStream = stream;
    this->origWidth = origWidth;
    this->origHeight = origHeight;
    this->hasAlpha = hasAlpha;

    return true;
}

static bool isConfigCompatible(SkBitmap* bitmap) {
    SkBitmap::Config config = bitmap->config();
    return config == SkBitmap::kARGB_4444_Config ||
           config == SkBitmap::kRGB_565_Config ||
           config == SkBitmap::kARGB_8888_Config;
}

bool SkWEBPImageDecoder::onDecodeRegion(SkBitmap* decodedBitmap,
                                        SkIRect region) {
    SkIRect rect = SkIRect::MakeWH(origWidth, origHeight);

    if (!rect.intersect(region)) {
        // If the requested region is entirely outsides the image, just
        // returns false
        return false;
    }

    const int sampleSize = this->getSampleSize();
    SkScaledBitmapSampler sampler(rect.width(), rect.height(), sampleSize);
    const int width = sampler.scaledWidth();
    const int height = sampler.scaledHeight();

    // The image can be decoded directly to decodedBitmap if
    //   1. the region is within the image range
    //   2. bitmap's config is compatible
    //   3. bitmap's size is same as the required region (after sampled)
    bool directDecode = (rect == region) &&
                        (decodedBitmap->isNull() ||
                         (isConfigCompatible(decodedBitmap) &&
                         (decodedBitmap->width() == width) &&
                         (decodedBitmap->height() == height)));
    SkTScopedPtr<SkBitmap> adb;
    SkBitmap *bitmap = decodedBitmap;

    if (!directDecode) {
        // allocates a temp bitmap
        bitmap = new SkBitmap;
        adb.reset(bitmap);
    }

    if (bitmap->isNull()) {
        if (!setDecodeConfig(bitmap, width, height)) {
            return false;
        }
        // alloc from native heap if it is a temp bitmap. (prevent GC)
        bool allocResult = (bitmap == decodedBitmap)
                               ? allocPixelRef(bitmap, NULL)
                               : bitmap->allocPixels();
        if (!allocResult) {
            return return_false(*decodedBitmap, "allocPixelRef");
        }
    } else {
        // This is also called in setDecodeConfig in above block.
        // i.e., when bitmap->isNull() is true.
        if (!chooseFromOneChoice(bitmap->config(), width, height)) {
            return false;
        }
    }

    SkAutoLockPixels alp(*bitmap);
    WebPDecoderConfig config;
    if (!webp_get_config_resize_crop(config, bitmap, rect, hasAlpha)) {
        return false;
    }

    // Decode the WebP image data stream using WebP incremental decoding for
    // the specified cropped image-region.
    if (!webp_idecode(this->inputStream, config)) {
        return false;
    }

    if (!directDecode) {
        cropBitmap(decodedBitmap, bitmap, sampleSize, region.x(), region.y(),
                  region.width(), region.height(), rect.x(), rect.y());
    }
    return true;
}

bool SkWEBPImageDecoder::onDecode(SkStream* stream, SkBitmap* decodedBitmap,
                                  Mode mode) {
#ifdef TIME_DECODE
    AutoTimeMillis atm("WEBP Decode");
#endif

    int origWidth, origHeight, hasAlpha;
    

//=========================================================================    
//
//=========================================================================    

#ifdef MTK_WEBP_HW_DECODER
    SkAshmemMalloc    tAllocator;
#ifdef USE_SKWEBPSTREAM
	WebpStreamAutoClean jpgStreamAutoClean;
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
    size_t image_width = 0;
    size_t image_height = 0;
    
		unsigned int try_hw = 0;
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
					}
     
#endif
  
#ifdef MTK_WEBP_HW_DECODER_ENABLE
      try_hw = 1; 
#endif
 
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
           
        XLOGW("enter Skia Webp try_path %d, mode %d, L:%d!! \n",try_hw, mode, __LINE__);           

        if(try_hw && mode != SkImageDecoder::kDecodeBounds_Mode && true == onDecodeParser(decodedBitmap, mode, tSrc, rSize, tAllocator.getFD(), &image_width, &image_height ) )
        {
            //if(mode == SkImageDecoder::kDecodeBounds_Mode)
            //{
            //    tAllocator.free();
            //    return true;        
            //}
            //else
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
                  //if( (*(uint8_t*)(bs_tail-2) != 0xFF) || (*(uint8_t*)(bs_tail-1) != 0xD9) )
                  {
                    //XLOGW("SkiaWebp:broken bitstream!!\n");  
                    XLOGW("SkiaWebp: check_webp_bs b %x,s %x, bs: b %x %x, e %x %x %x %x %x, L:%d!!\n", (unsigned int)tSrc, rSize,*tSrc, *(tSrc+1)
                    , *(uint8_t*)(bs_tail-4),*(uint8_t*)(bs_tail-3),*(uint8_t*)(bs_tail-2), *(uint8_t*)(bs_tail-1), *bs_tail,__LINE__);                                    
                    //no_eoi =1;
                  }
                }                
#endif                
                if( no_eoi 
                    //|| (decodedBitmap->width() == 200 && decodedBitmap->height() == 200)
                   ){
                  if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_WEBP_DEC_START, NULL, 0, NULL, 0, NULL))
                  {
                      XLOGW("Can not release WEBP HW Decoder\n");
                      return false;
                  }
                  skip_hw_path = 1;
                }
                if(sLength > rSize){
                    memset((tSrc + rSize), 0, sLength - rSize);
#if 1 //ndef MTK_WEBP_HW_DECODER_658X                    
                    rSize += 64 ;
                    SkDebugf("WEBP_BS mSize %x, rSize %x, align rSize %x, LENGTH %x!!\n", sLength, rSize, (rSize + 31) & (~31), stream->getLength());
                    rSize = (rSize + 31) & (~31);    
#endif                    
                }
                SkDebugf("SkiaWebp: skip %d, BufSize %x, BitsSize %x, BitsAlignSize %x, GetLength %x, L:%d!!\n",skip_hw_path, sLength, rSize, (rSize + 31) & (~31), stream->getLength(), __LINE__); 
                    
                //if(true != onDecodeHW(decodedBitmap, tSrc, rSize, tAllocator.getFD()) )
                //if(skip_hw_path || true != onDecodeHW(decodedBitmap, tSrc, sLength, ((sLength - 256)>rSize) ? sLength-256: rSize, tAllocator.getFD()) )
                if(skip_hw_path || true != onDecodeHW(decodedBitmap, tSrc, sLength, rSize, tAllocator.getFD()) )
                {
                    XLOGD("SkiaWebp:TRY_SW_PATH no_eoi %d, mSize %x, rSize %x, align rSize %x, skSize %x, stream %x!!\n", no_eoi, sLength, rSize, (rSize + 31) & (~31), stream->getLength(),(unsigned int) stream);
                    if(rSize > MAX_HEADER_SIZE)
                    {
#ifdef USE_SKWEBPSTREAM						
                        stream = new SkWebpStream(tSrc, rHWbsSize, stream);
                        jpgStreamAutoClean.set(stream);
#else
                        stream = new SkMemoryStream(tSrc, sLength);   
#endif
                        XLOGW("Use WEBP SW Decoder (temp stream %x)\n", (unsigned int)stream);
                    }
                    else
                    {
#if 0 //def USE_SKWEBPSTREAM
			            XLOGD("SkiaWebp:TRY_SW_PATH tSrc %x, rSize %x, skSize %x, L:%d!!\n", tSrc, rSize,  stream->getLength(), __LINE__);
			            stream = new SkWebpStream(tSrc, rHWbsSize, stream);
			            jpgStreamAutoClean.set(stream);
#else
                        tAllocator.free();
#endif                      						
                        XLOGW("Use WEBP SW Decoder, L:%d\n", __LINE__);
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
#if 0 //def USE_SKWEBPSTREAM
			XLOGD("SkiaWebp:TRY_SW_PATH tSrc %x, rSize %x, skSize %x, L:%d!!\n", tSrc, rSize,  stream->getLength(), __LINE__);
			stream = new SkWebpStream(tSrc, rSize, stream);
			jpgStreamAutoClean.set(stream);
#else        
            tAllocator.free();
#endif            
            XLOGW("Use WEBP SW Decoder, L:%d!!\n", __LINE__);
            if(true != stream->rewind())
            {
                XLOGW("onDecode(), rewind fail\n");
                return false;       
            }
        }
    }
    
#endif


    
//=========================================================================    
//
//=========================================================================
    
    if (!webp_parse_header(stream, &origWidth, &origHeight, &hasAlpha)) {
        return false;
    }
    this->hasAlpha = hasAlpha;

    const int sampleSize = this->getSampleSize();
    SkScaledBitmapSampler sampler(origWidth, origHeight, sampleSize);



    // If only bounds are requested, done
    if (SkImageDecoder::kDecodeBounds_Mode == mode) {
        if (!setDecodeConfig(decodedBitmap, sampler.scaledWidth(),
                             sampler.scaledHeight())) {
            SkDebugf("webpSwDec: setDecodeConfig fail %d %d , L:%d !!\n", sampler.scaledWidth(), sampler.scaledHeight(),__LINE__);
            return false;
        }
        return true;
    }
    
#ifdef SK_BUILD_FOR_ANDROID
    // No Bitmap reuse supported for this format
    if (!decodedBitmap->isNull()) { 
#ifdef MTK_WEBP_HW_DECODER         
        // Dimensions must match
        if( try_hw && ( sampler.scaledWidth() != decodedBitmap->width() || sampler.scaledHeight() != decodedBitmap->height() ) ){
          SkDebugf("webpSwDec::Dimensions must match (%d %d) (%d %d)", decodedBitmap->width(), decodedBitmap->height()
          , sampler.scaledWidth(), sampler.scaledHeight());
          return false ;
        }
#else
        return false;
#endif                    
    }
#endif

#ifdef MTK_WEBP_HW_DECODER
    if(decodedBitmap->isNull()) 
#endif      
    if (!setDecodeConfig(decodedBitmap, sampler.scaledWidth(),
                         sampler.scaledHeight())) {
                           
        SkDebugf("webpSwDec: setDecodeConfig fail %d %d , L:%d !!\n", sampler.scaledWidth(), sampler.scaledHeight(), __LINE__);
        return false;
    }
	SkDebugf("skiaWebpDecoder:: org:w %d, h %d, out:%d %d, decBuf %d %d, sample %d!!\n",origWidth, origHeight, 
		sampler.scaledWidth(), sampler.scaledHeight(), 
		decodedBitmap->width(), decodedBitmap->height(),sampleSize);	

    if (!this->allocPixelRef(decodedBitmap, NULL)) {
        return return_false(*decodedBitmap, "allocPixelRef");
    }

    SkAutoLockPixels alp(*decodedBitmap);

    WebPDecoderConfig config;
    if (!webp_get_config_resize(config, decodedBitmap, origWidth, origHeight,
                                hasAlpha)) {
        return false;
    }

    // Decode the WebP image data stream using WebP incremental decoding.
    return webp_idecode(stream, config);
}

///////////////////////////////////////////////////////////////////////////////

typedef void (*ScanlineImporter)(const uint8_t* in, uint8_t* out, int width,
                                 const SkPMColor* SK_RESTRICT ctable);

static void ARGB_8888_To_RGB(const uint8_t* in, uint8_t* rgb, int width,
                             const SkPMColor*) {
  const uint32_t* SK_RESTRICT src = (const uint32_t*)in;
  for (int i = 0; i < width; ++i) {
      const uint32_t c = *src++;
      rgb[0] = SkGetPackedR32(c);
      rgb[1] = SkGetPackedG32(c);
      rgb[2] = SkGetPackedB32(c);
      rgb += 3;
  }
}

static void RGB_565_To_RGB(const uint8_t* in, uint8_t* rgb, int width,
                           const SkPMColor*) {
  const uint16_t* SK_RESTRICT src = (const uint16_t*)in;
  for (int i = 0; i < width; ++i) {
      const uint16_t c = *src++;
      rgb[0] = SkPacked16ToR32(c);
      rgb[1] = SkPacked16ToG32(c);
      rgb[2] = SkPacked16ToB32(c);
      rgb += 3;
  }
}

static void ARGB_4444_To_RGB(const uint8_t* in, uint8_t* rgb, int width,
                             const SkPMColor*) {
  const SkPMColor16* SK_RESTRICT src = (const SkPMColor16*)in;
  for (int i = 0; i < width; ++i) {
      const SkPMColor16 c = *src++;
      rgb[0] = SkPacked4444ToR32(c);
      rgb[1] = SkPacked4444ToG32(c);
      rgb[2] = SkPacked4444ToB32(c);
      rgb += 3;
  }
}

static void Index8_To_RGB(const uint8_t* in, uint8_t* rgb, int width,
                          const SkPMColor* SK_RESTRICT ctable) {
  const uint8_t* SK_RESTRICT src = (const uint8_t*)in;
  for (int i = 0; i < width; ++i) {
      const uint32_t c = ctable[*src++];
      rgb[0] = SkGetPackedR32(c);
      rgb[1] = SkGetPackedG32(c);
      rgb[2] = SkGetPackedB32(c);
      rgb += 3;
  }
}

static ScanlineImporter ChooseImporter(const SkBitmap::Config& config) {
    switch (config) {
        case SkBitmap::kARGB_8888_Config:
            return ARGB_8888_To_RGB;
        case SkBitmap::kRGB_565_Config:
            return RGB_565_To_RGB;
        case SkBitmap::kARGB_4444_Config:
            return ARGB_4444_To_RGB;
        case SkBitmap::kIndex8_Config:
            return Index8_To_RGB;
        default:
            return NULL;
    }
}

static int StreamWriter(const uint8_t* data, size_t data_size,
                        const WebPPicture* const picture) {
  SkWStream* const stream = (SkWStream*)picture->custom_ptr;
  return stream->write(data, data_size) ? 1 : 0;
}

class SkWEBPImageEncoder : public SkImageEncoder {
protected:
    virtual bool onEncode(SkWStream* stream, const SkBitmap& bm, int quality);
};

bool SkWEBPImageEncoder::onEncode(SkWStream* stream, const SkBitmap& bm,
                                  int quality) {
    const SkBitmap::Config config = bm.getConfig();
    const ScanlineImporter scanline_import = ChooseImporter(config);
    if (NULL == scanline_import) {
        return false;
    }

    SkAutoLockPixels alp(bm);
    SkAutoLockColors ctLocker;
    if (NULL == bm.getPixels()) {
        return false;
    }

    WebPConfig webp_config;
    if (!WebPConfigPreset(&webp_config, WEBP_PRESET_DEFAULT, quality)) {
        return false;
    }

    WebPPicture pic;
    WebPPictureInit(&pic);
    pic.width = bm.width();
    pic.height = bm.height();
    pic.writer = StreamWriter;
    pic.custom_ptr = (void*)stream;

    const SkPMColor* colors = ctLocker.lockColors(bm);
    const uint8_t* src = (uint8_t*)bm.getPixels();
    const int rgb_stride = pic.width * 3;

    // Import (for each scanline) the bit-map image (in appropriate color-space)
    // to RGB color space.
    uint8_t* rgb = new uint8_t[rgb_stride * pic.height];
    for (int y = 0; y < pic.height; ++y) {
        scanline_import(src + y * bm.rowBytes(), rgb + y * rgb_stride,
                        pic.width, colors);
    }

    bool ok = WebPPictureImportRGB(&pic, rgb, rgb_stride);
    delete[] rgb;

    ok = ok && WebPEncode(&webp_config, &pic);
    WebPPictureFree(&pic);

    return ok;
}


///////////////////////////////////////////////////////////////////////////////

#include "SkTRegistry.h"

static SkImageDecoder* DFactory(SkStream* stream) {
    int width, height, hasAlpha;
    if (!webp_parse_header(stream, &width, &height, &hasAlpha)) {
        return NULL;
    }

    // Magic matches, call decoder
    return SkNEW(SkWEBPImageDecoder);
}

SkImageDecoder* sk_libwebp_dfactory(SkStream* stream) {
    return DFactory(stream);
}

static SkImageEncoder* EFactory(SkImageEncoder::Type t) {
      return (SkImageEncoder::kWEBP_Type == t) ? SkNEW(SkWEBPImageEncoder) : NULL;
}

SkImageEncoder* sk_libwebp_efactory(SkImageEncoder::Type t) {
    return EFactory(t);
}

static SkTRegistry<SkImageDecoder*, SkStream*> gDReg(sk_libwebp_dfactory);
static SkTRegistry<SkImageEncoder*, SkImageEncoder::Type> gEReg(sk_libwebp_efactory);
