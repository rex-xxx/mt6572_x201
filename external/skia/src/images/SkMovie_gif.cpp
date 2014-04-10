
/*
 * Copyright 2006 The Android Open Source Project
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


#include "SkMovie.h"
#include "SkColor.h"
#include "SkColorPriv.h"
#include "SkStream.h"
#include "SkTemplates.h"
#include "SkUtils.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "gif_lib.h"


#ifndef MTK_IMAGE_LARGE_MEM_LIMIT
#define MTK_MGIF_MAX_SRC_FILE_SIZE (10*1024*1024)
#define MTK_MGIF_MAX_SRC_FRAME_PIXELS (1536*1024)
#define MTK_MGIF_MAX_SRC_PIXELS (10*1024*1024)
#else
#define MTK_MGIF_MAX_SRC_FILE_SIZE (12*1024*1024)
#define MTK_MGIF_MAX_SRC_FRAME_PIXELS (1920*1088)
#define MTK_MGIF_MAX_SRC_PIXELS (5*MTK_MGIF_MAX_SRC_FRAME_PIXELS)
#endif

//#include "utils/Log.h"
#define LOGE SkDebugf 

class SkGIFMovie : public SkMovie {
public:
    SkGIFMovie(SkStream* stream);
    virtual ~SkGIFMovie();

protected:
    virtual bool onGetInfo(Info*);
    virtual bool onSetTime(SkMSec);
    virtual bool onGetBitmap(SkBitmap*);

//for add gif begin
//the following methods are intented for no one but Movie to use.
//please see Movie.cpp for information
    virtual int getGifFrameDuration(int frameIndex);
    virtual int getGifTotalFrameCount();
    virtual bool setCurrFrame(int frameIndex);
private:
    bool checkGifStream(SkStream* stream);
    bool getWordFromStream(SkStream* stream,int* word);
    bool getRecordType(SkStream* stream,GifRecordType* Type);
    bool checkImageDesc(SkStream* stream,char* buf);
    bool skipExtension(SkStream* stream,char* buf);
    bool skipComment(SkStream* stream,char* buf);
    bool skipGraphics(SkStream* stream,char* buf);
    bool skipPlaintext(SkStream* stream,char* buf);
    bool skipApplication(SkStream* stream,char* buf);
    bool skipSubblocksWithTerminator(SkStream* stream,char* buf);
//for add gif end
    
private:
    GifFileType* fGIF;
    int fCurrIndex;
    int fLastDrawIndex;
    SkBitmap fBackup;
    SkColor paintingColor;
};

static int Decode(GifFileType* fileType, GifByteType* out, int size) {
    SkStream* stream = (SkStream*) fileType->UserData;
    return (int) stream->read(out, size);
}

SkGIFMovie::SkGIFMovie(SkStream* stream)
{
    unsigned int streamLength = stream->getLength();
    fGIF = NULL;
    fCurrIndex = -1;
    fLastDrawIndex = -1;    
    LOGE("SkGIFMovie:SkGIFMovie: GIF %x,Cidx %d,LDidx %d\n", (unsigned int)fGIF, fCurrIndex, fLastDrawIndex);
    //if length of SkStream is below zero, no need for further parsing
    if (streamLength <= 0) {
        LOGE("SkGIFMovie:SkGIFMovie: GIF source file length is below 0");
        return;
    }

    //allocate a buffer to hold content of SkStream
    void * streamBuffer = malloc(streamLength+1);
    if (streamBuffer == 0) {
        LOGE("SkGIFMovie:SkGIFMovie: malloc Memory stream buffer failed");
        return;
    }

    //Fetch SkStream content into buffer
    if (streamLength != stream->read(streamBuffer, stream->getLength())) {
        LOGE("SkGIFMovie:SkGIFMovie: read GIF source to Memory Buffer failed");
        free(streamBuffer);
        return;
    }

    //we wrap stream with SkmemoryStream, cause
    //its rewind does not make mark on InputStream be
    //invalid.
    SkStream* memStream = new SkMemoryStream(streamBuffer,streamLength);
    bool bRewindable = memStream->rewind();
    if (bRewindable)
    {
        //check if GIF file is valid to decode
        bool bGifValid = checkGifStream(memStream);
        if (! bGifValid)
        {
            free(streamBuffer);
            fGIF = NULL;
            return;
        }
        //GIF file stream seems to be OK, 
        // rewind stream for gif decoding
        memStream->rewind();
    }

    fGIF = DGifOpen( memStream, Decode );
    if (NULL == fGIF) {
        free(streamBuffer);
        return;
    }

    if (DGifSlurp(fGIF) != GIF_OK)
    {
        DGifCloseFile(fGIF);
        fGIF = NULL;
    }
    fCurrIndex = -1;
    fLastDrawIndex = -1;

    //release stream buffer when decoding is done.
    free(streamBuffer);
}

SkGIFMovie::~SkGIFMovie()
{
    if (fGIF)
        DGifCloseFile(fGIF);
}

static SkMSec savedimage_duration(const SavedImage* image)
{
    for (int j = 0; j < image->ExtensionBlockCount; j++)
    {
        if (image->ExtensionBlocks[j].Function == GRAPHICS_EXT_FUNC_CODE)
        {
            int size = image->ExtensionBlocks[j].ByteCount;
            SkASSERT(size >= 4);
            const uint8_t* b = (const uint8_t*)image->ExtensionBlocks[j].Bytes;
            return ((b[2] << 8) | b[1]) * 10;
        }
    }
    return 0;
}

//for add gif begin
int SkGIFMovie::getGifFrameDuration(int frameIndex)
{
    //for wrong frame index, return 0
    if (frameIndex < 0 || NULL == fGIF || frameIndex >= fGIF->ImageCount)
        return 0;
    return savedimage_duration(&fGIF->SavedImages[frameIndex]);
}

int SkGIFMovie::getGifTotalFrameCount()
{
    //if fGIF is not valid, return 0
    if (NULL == fGIF)
        return 0;
    return fGIF->ImageCount < 0 ? 0 : fGIF->ImageCount;
}

bool SkGIFMovie::setCurrFrame(int frameIndex)
{
    if (NULL == fGIF)
        return false;

    if (frameIndex >= 0 && frameIndex < fGIF->ImageCount)
        fCurrIndex = frameIndex;
    else
        fCurrIndex = 0;
    return true;
}

bool SkGIFMovie::getWordFromStream(SkStream* stream,int* word)
{
    unsigned char buf[2];

    if (stream->read(buf, 2) != 2) {
        LOGE("SkGIFMovie:getWordFromStream: read from stream failed");
        return false;
    }

    *word = (((unsigned int)buf[1]) << 8) + buf[0];
    return true;
}

bool SkGIFMovie::getRecordType(SkStream* stream,GifRecordType* Type)
{
    unsigned char buf;
    //read a record type to buffer
    if (stream->read(&buf, 1) != 1) {
        LOGE("SkGIFMovie:getRecordType: read from stream failed");
        return false;
    }
    //identify record type
    switch (buf) {
      case ',':
          *Type = IMAGE_DESC_RECORD_TYPE;
          break;
      case '!':
          *Type = EXTENSION_RECORD_TYPE;
          break;
      case ';':
          *Type = TERMINATE_RECORD_TYPE;
          break;
      default:
          *Type = UNDEFINED_RECORD_TYPE;
          LOGE("SkGIFMovie:getRecordType: wrong gif record type");
          return false;
    }
    return true;
}

/******************************************************************************
 * calculate all image frame count without decode image frame.
 * SkStream associated with GifFile and GifFile state is not
 * affected
 *****************************************************************************/
bool SkGIFMovie::checkGifStream(SkStream* stream)
{
    char buf[16];
    int screenWidth, screenHeight;
    int BitsPerPixel = 0;
    int frameCount = 0;
    GifRecordType RecordType;

    //maximum stream length is set to be 10M, if the stream is 
    //larger than that, no further action is needed
    size_t length = stream->getLength();
    if (length > MTK_MGIF_MAX_SRC_FILE_SIZE) {
        LOGE("SkGIFMovie:checkGifStream: stream length(%d) larger than %d!!", length, MTK_MGIF_MAX_SRC_FILE_SIZE);
        return false;
    }

    if (GIF_STAMP_LEN != stream->read(buf, GIF_STAMP_LEN)) {
        LOGE("SkGIFMovie:checkGifStream: read GIF STAMP failed");
        return false;
    }

    //Check whether the first three charactar is "GIF", version 
    // number is ignored.
    buf[GIF_STAMP_LEN] = 0;
    if (strncmp(GIF_STAMP, buf, GIF_VERSION_POS) != 0) {
        LOGE("SkGIFMovie:checkGifStream: check GIF stamp failed");
        return false;
    }

    //read screen width and height from stream
    screenWidth = 0;
    screenHeight = 0;
    if (! getWordFromStream(stream,&screenWidth) ||
        ! getWordFromStream(stream,&screenHeight)) {
        LOGE("SkGIFMovie:checkGifStream: get screen dimension failed");
        return false;
    }

    //check whether screen dimension is too large
    //maximum pixels in a single frame is constrained to 1.5M
    //which is aligned withSkImageDecoder_libgif.cpp
    if (screenWidth*screenHeight > MTK_MGIF_MAX_SRC_FRAME_PIXELS) {
        LOGE("SkGIFMovie:checkGifStream: screen dimension(%d %d) is larger than %d",screenWidth,screenHeight, MTK_MGIF_MAX_SRC_FRAME_PIXELS);
        return false;
    }

    //read screen color resolution and color map information
    if (3 != stream->read(buf, 3)) {
        LOGE("SkGIFMovie:checkGifStream: read color info failed");
        return false;
    }
    BitsPerPixel = (buf[0] & 0x07) + 1;
    if (buf[0] & 0x80) {    
        //If we have global color table, skip it
        unsigned int colorTableBytes = (unsigned)(1 << BitsPerPixel) * 3;
        if (colorTableBytes != stream->skip(colorTableBytes)) {
            LOGE("SkGIFMovie:checkGifStream: skip global color table failed");
            return false;
        }
    } else {
    }
//DGifOpen is over, now for DGifSlurp
    do {
        if (getRecordType(stream, &RecordType) == false)
            return false;

        switch (RecordType) {
          case IMAGE_DESC_RECORD_TYPE:
              if (checkImageDesc(stream,buf) == false)
                  return false;
              frameCount ++;
              //skip code size for LZW
              if (1 != stream->skip(1)) {
                  LOGE("SkGIFMovie:checkGifStream: skip code size failed");
                  return false;
              }
              if (skipSubblocksWithTerminator(stream,buf) == false) {
                  LOGE("SkGIFMovie:checkGifStream: skip compressed image data failed");
                  return false;
              }
              break;

          case EXTENSION_RECORD_TYPE:
              if (skipExtension(stream,buf) == false) {
                  LOGE("SkGIFMovie:checkGifStream: skip extensions failed");
                  return false;
              }
              break;

          case TERMINATE_RECORD_TYPE:
              break;

          default:    /* Should be trapped by DGifGetRecordType */
              break;
        }
    } while (RecordType != TERMINATE_RECORD_TYPE);

    //maximum pixels in all gif frames is constrained to 5M
    //although each frame has its own dimension, we estimate the total
    //pixels which the decoded gif file had be screen dimension multiply
    //total image count, this should be the worst case
    if (screenWidth * screenHeight * frameCount > MTK_MGIF_MAX_SRC_PIXELS) {
        LOGE("SkGIFMovie:checkGifStream: total pixels(%d) is larger than %d!!", (screenWidth * screenHeight * frameCount),MTK_MGIF_MAX_SRC_PIXELS);
        return false;
    }

    return true;
}

bool SkGIFMovie::checkImageDesc(SkStream* stream,char* buf)
{
    int imageWidth,imageHeight;
    int BitsPerPixel;
    if (4 != stream->skip(4)) {
        LOGE("SkGIFMovie:getImageDesc: skip image left-top position");
        return false;
    }
    if (! getWordFromStream(stream,&imageWidth)||
        ! getWordFromStream(stream,&imageHeight)) {
        LOGE("SkGIFMovie:getImageDesc: read image width & height");
        return false;
    }
    if (1 != stream->read(buf, 1)) {
        LOGE("SkGIFMovie:getImageDesc: read image info failed");
        return false;
    }

    BitsPerPixel = (buf[0] & 0x07) + 1;
    if (buf[0] & 0x80) {    
        //If this image have local color map, skip it
        unsigned int colorTableBytes = (unsigned)(1 << BitsPerPixel) * 3;
        if (colorTableBytes != stream->skip(colorTableBytes)) {
            LOGE("SkGIFMovie:getImageDesc: skip global color table failed");
            return false;
        }
    } else {
    }
    return true;
}


bool SkGIFMovie::skipExtension(SkStream* stream,char* buf)
{
    int imageWidth,imageHeight;
    int BitsPerPixel;
    if (1 != stream->read(buf, 1)) {
        LOGE("SkGIFMovie:skipExtension: read extension type failed");
        return false;
    }
    switch (buf[0]) {
      case COMMENT_EXT_FUNC_CODE:
          if (skipComment(stream,buf)==false) {
              LOGE("SkGIFMovie:skipExtension: skip comment failed");
              return false;
          }
          break;
      case GRAPHICS_EXT_FUNC_CODE:
          if (skipGraphics(stream,buf)==false) {
              LOGE("SkGIFMovie:skipExtension: skip graphics failed");
              return false;
          }
          break;
      case PLAINTEXT_EXT_FUNC_CODE:
          if (skipPlaintext(stream,buf)==false) {
              LOGE("SkGIFMovie:skipExtension: skip plaintext failed");
              return false;
          }
          break;
      case APPLICATION_EXT_FUNC_CODE:
          if (skipApplication(stream,buf)==false) {
              LOGE("SkGIFMovie:skipExtension: skip application failed");
              return false;
          }
          break;
      default:
          LOGE("SkGIFMovie:skipExtension: wrong gif extension type");
          return false;
    }
    return true;
}

bool SkGIFMovie::skipComment(SkStream* stream,char* buf)
{
     return skipSubblocksWithTerminator(stream,buf);
}

bool SkGIFMovie::skipGraphics(SkStream* stream,char* buf)
{
     return skipSubblocksWithTerminator(stream,buf);
}

bool SkGIFMovie::skipPlaintext(SkStream* stream,char* buf)
{
     return skipSubblocksWithTerminator(stream,buf);
}

bool SkGIFMovie::skipApplication(SkStream* stream,char* buf)
{
     return skipSubblocksWithTerminator(stream,buf);
}

bool SkGIFMovie::skipSubblocksWithTerminator(SkStream* stream,char* buf)
{
    do {//skip the whole compressed image data.
        //read sub-block size
        if (1 != stream->read(buf,1)) {
            LOGE("SkGIFMovie:skipSubblocksWithTerminator: read sub block size failed");
            return false;
        }
        if (buf[0] > 0) {
            if (buf[0] != stream->skip(buf[0])) {
                LOGE("SkGIFMovie:skipSubblocksWithTerminator: skip sub block failed");
                return false;
            }
        }
    } while(buf[0]!=0);
    return true;
}

//for add gif end

bool SkGIFMovie::onGetInfo(Info* info)
{
    if (NULL == fGIF)
        return false;

    SkMSec dur = 0;
    for (int i = 0; i < fGIF->ImageCount; i++)
        dur += savedimage_duration(&fGIF->SavedImages[i]);

    info->fDuration = dur;
    info->fWidth = fGIF->SWidth;
    info->fHeight = fGIF->SHeight;
    info->fIsOpaque = false;    // how to compute?
    return true;
}

bool SkGIFMovie::onSetTime(SkMSec time)
{
    if (NULL == fGIF)
        return false;

    SkMSec dur = 0;
    for (int i = 0; i < fGIF->ImageCount; i++)
    {
        dur += savedimage_duration(&fGIF->SavedImages[i]);
        if (dur >= time)
        {
            fCurrIndex = i;
            return fLastDrawIndex != fCurrIndex;
        }
    }
    fCurrIndex = fGIF->ImageCount - 1;
    return true;
}

static void copyLine(uint32_t* dst, const unsigned char* src, const ColorMapObject* cmap,
                     int transparent, int width)
{
    for (; width > 0; width--, src++, dst++) {
        if (*src != transparent) {
            const GifColorType& col = cmap->Colors[*src];
            *dst = SkPackARGB32(0xFF, col.Red, col.Green, col.Blue);
        }
    }
}

static void copyInterlaceGroup(SkBitmap* bm, const unsigned char*& src,
                               const ColorMapObject* cmap, int transparent, int copyWidth,
                               int copyHeight, const GifImageDesc& imageDesc, int rowStep,
                               int startRow)
{
    int row;
    // every 'rowStep'th row, starting with row 'startRow'
    for (row = startRow; row < copyHeight; row += rowStep) {
        uint32_t* dst = bm->getAddr32(imageDesc.Left, imageDesc.Top + row);
        copyLine(dst, src, cmap, transparent, copyWidth);
        src += imageDesc.Width;
    }

    // pad for rest height
    src += imageDesc.Width * ((imageDesc.Height - row + rowStep - 1) / rowStep);
}

static void blitInterlace(SkBitmap* bm, const SavedImage* frame, const ColorMapObject* cmap,
                          int transparent)
{
    int width = bm->width();
    int height = bm->height();
    GifWord copyWidth = frame->ImageDesc.Width;
    if (frame->ImageDesc.Left + copyWidth > width) {
        copyWidth = width - frame->ImageDesc.Left;
    }

    GifWord copyHeight = frame->ImageDesc.Height;
    if (frame->ImageDesc.Top + copyHeight > height) {
        copyHeight = height - frame->ImageDesc.Top;
    }

    // deinterlace
    const unsigned char* src = (unsigned char*)frame->RasterBits;

    // group 1 - every 8th row, starting with row 0
    copyInterlaceGroup(bm, src, cmap, transparent, copyWidth, copyHeight, frame->ImageDesc, 8, 0);

    // group 2 - every 8th row, starting with row 4
    copyInterlaceGroup(bm, src, cmap, transparent, copyWidth, copyHeight, frame->ImageDesc, 8, 4);

    // group 3 - every 4th row, starting with row 2
    copyInterlaceGroup(bm, src, cmap, transparent, copyWidth, copyHeight, frame->ImageDesc, 4, 2);

    copyInterlaceGroup(bm, src, cmap, transparent, copyWidth, copyHeight, frame->ImageDesc, 2, 1);
}

static void blitNormal(SkBitmap* bm, const SavedImage* frame, const ColorMapObject* cmap,
                       int transparent)
{
    int width = bm->width();
    int height = bm->height();
    const unsigned char* src = (unsigned char*)frame->RasterBits;
    uint32_t* dst = bm->getAddr32(frame->ImageDesc.Left, frame->ImageDesc.Top);
    GifWord copyWidth = frame->ImageDesc.Width;
    if (frame->ImageDesc.Left + copyWidth > width) {
        copyWidth = width - frame->ImageDesc.Left;
    }

    GifWord copyHeight = frame->ImageDesc.Height;
    if (frame->ImageDesc.Top + copyHeight > height) {
        copyHeight = height - frame->ImageDesc.Top;
    }

    int srcPad, dstPad;
    dstPad = width - copyWidth;
    srcPad = frame->ImageDesc.Width - copyWidth;
    for (; copyHeight > 0; copyHeight--) {
        copyLine(dst, src, cmap, transparent, copyWidth);
        src += frame->ImageDesc.Width;
        dst += width;
    }
}

static void fillRect(SkBitmap* bm, GifWord left, GifWord top, GifWord width, GifWord height,
                     uint32_t col)
{
    int bmWidth = bm->width();
    int bmHeight = bm->height();
    uint32_t* dst = bm->getAddr32(left, top);
    GifWord copyWidth = width;
    if (left + copyWidth > bmWidth) {
        copyWidth = bmWidth - left;
    }

    GifWord copyHeight = height;
    if (top + copyHeight > bmHeight) {
        copyHeight = bmHeight - top;
    }

    for (; copyHeight > 0; copyHeight--) {
        sk_memset32(dst, col, copyWidth);
        dst += bmWidth;
    }
}

static void drawFrame(SkBitmap* bm, const SavedImage* frame, const ColorMapObject* cmap)
{
    int transparent = -1;

    for (int i = 0; i < frame->ExtensionBlockCount; ++i) {
        ExtensionBlock* eb = frame->ExtensionBlocks + i;
        if (eb->Function == GRAPHICS_EXT_FUNC_CODE &&
            eb->ByteCount == 4) {
            bool has_transparency = ((eb->Bytes[0] & 1) == 1);
            if (has_transparency) {
                transparent = (unsigned char)eb->Bytes[3];
            }
        }
    }

    if (frame->ImageDesc.ColorMap != NULL) {
        // use local color table
        cmap = frame->ImageDesc.ColorMap;
    }

    if (cmap == NULL || cmap->ColorCount != (1 << cmap->BitsPerPixel)) {
        SkDEBUGFAIL("bad colortable setup");
        return;
    }

    if (frame->ImageDesc.Interlace) {
        blitInterlace(bm, frame, cmap, transparent);
    } else {
        blitNormal(bm, frame, cmap, transparent);
    }
}

static bool checkIfWillBeCleared(const SavedImage* frame)
{
    for (int i = 0; i < frame->ExtensionBlockCount; ++i) {
        ExtensionBlock* eb = frame->ExtensionBlocks + i;
        if (eb->Function == GRAPHICS_EXT_FUNC_CODE &&
            eb->ByteCount == 4) {
            // check disposal method
            int disposal = ((eb->Bytes[0] >> 2) & 7);
            if (disposal == 2 || disposal == 3) {
                return true;
            }
        }
    }
    return false;
}

static void getTransparencyAndDisposalMethod(const SavedImage* frame, bool* trans, int* disposal)
{
    *trans = false;
    *disposal = 0;
    for (int i = 0; i < frame->ExtensionBlockCount; ++i) {
        ExtensionBlock* eb = frame->ExtensionBlocks + i;
        if (eb->Function == GRAPHICS_EXT_FUNC_CODE &&
            eb->ByteCount == 4) {
            *trans = ((eb->Bytes[0] & 1) == 1);
            *disposal = ((eb->Bytes[0] >> 2) & 7);
        }
    }
}

// return true if area of 'target' is completely covers area of 'covered'
static bool checkIfCover(const SavedImage* target, const SavedImage* covered)
{
    if (target->ImageDesc.Left <= covered->ImageDesc.Left
        && covered->ImageDesc.Left + covered->ImageDesc.Width <=
               target->ImageDesc.Left + target->ImageDesc.Width
        && target->ImageDesc.Top <= covered->ImageDesc.Top
        && covered->ImageDesc.Top + covered->ImageDesc.Height <=
               target->ImageDesc.Top + target->ImageDesc.Height) {
        return true;
    }
    return false;
}

static void disposeFrameIfNeeded(SkBitmap* bm, const SavedImage* cur, const SavedImage* next,
                                 SkBitmap* backup, SkColor color)
{
    // We can skip disposal process if next frame is not transparent
    // and completely covers current area
    bool curTrans;
    int curDisposal;
    getTransparencyAndDisposalMethod(cur, &curTrans, &curDisposal);
    bool nextTrans;
    int nextDisposal;
    getTransparencyAndDisposalMethod(next, &nextTrans, &nextDisposal);
    if ((curDisposal == 2 || curDisposal == 3)
        && (nextTrans || !checkIfCover(next, cur))) {
        switch (curDisposal) {
        // restore to background color
        // -> 'background' means background under this image.
        case 2:
            fillRect(bm, cur->ImageDesc.Left, cur->ImageDesc.Top,
                     cur->ImageDesc.Width, cur->ImageDesc.Height,
                     color);
            break;

        // restore to previous
        case 3:
            bm->swap(*backup);
            break;
        }
    }

    // Save current image if next frame's disposal method == 3
    if (nextDisposal == 3) {
        const uint32_t* src = bm->getAddr32(0, 0);
        uint32_t* dst = backup->getAddr32(0, 0);
        int cnt = bm->width() * bm->height();
        memcpy(dst, src, cnt*sizeof(uint32_t));
    }
}

bool SkGIFMovie::onGetBitmap(SkBitmap* bm)
{
    const GifFileType* gif = fGIF;
    if (NULL == gif)
        return false;

    if (gif->ImageCount < 1) {
        return false;
    }

    const int width = gif->SWidth;
    const int height = gif->SHeight;
    if (width <= 0 || height <= 0) {
        return false;
    }

    // no need to draw
    if (fLastDrawIndex >= 0 && fLastDrawIndex == fCurrIndex) {
        return true;
    }

    int startIndex = fLastDrawIndex + 1;
    if (fLastDrawIndex < 0 || !bm->readyToDraw()) {
        // first time

        startIndex = 0;

        // create bitmap
        bm->setConfig(SkBitmap::kARGB_8888_Config, width, height, 0);
        if (!bm->allocPixels(NULL)) {
            return false;
        }
        // create bitmap for backup
        fBackup.setConfig(SkBitmap::kARGB_8888_Config, width, height, 0);
        if (!fBackup.allocPixels(NULL)) {
            return false;
        }
    } else if (startIndex > fCurrIndex) {
        // rewind to 1st frame for repeat
        startIndex = 0;
    }

    int lastIndex = fCurrIndex;
    if (lastIndex < 0) {
        // first time
        lastIndex = 0;
    } else if (lastIndex > fGIF->ImageCount - 1) {
        // this block must not be reached.
        lastIndex = fGIF->ImageCount - 1;
    }

    SkColor bgColor = SkPackARGB32(0, 0, 0, 0);
    if (gif->SColorMap != NULL) {
        const GifColorType& col = gif->SColorMap->Colors[fGIF->SBackGroundColor];
        bgColor = SkColorSetARGB(0xFF, col.Red, col.Green, col.Blue);
    }

    //static SkColor paintingColor = SkPackARGB32(0, 0, 0, 0);
    paintingColor = SkPackARGB32(0, 0, 0, 0);
    // draw each frames - not intelligent way
    for (int i = startIndex; i <= lastIndex; i++) {
        const SavedImage* cur = &fGIF->SavedImages[i];
        if (i == 0) {
            bool trans;
            int disposal;
            getTransparencyAndDisposalMethod(cur, &trans, &disposal);
            if (!trans && gif->SColorMap != NULL) {
                paintingColor = bgColor;
            } else {
                paintingColor = SkColorSetARGB(0, 0, 0, 0);
            }

            bm->eraseColor(paintingColor);
            fBackup.eraseColor(paintingColor);
        } else {
            // Dispose previous frame before move to next frame.
            const SavedImage* prev = &fGIF->SavedImages[i-1];
            disposeFrameIfNeeded(bm, prev, cur, &fBackup, paintingColor);
        }

        // Draw frame
        // We can skip this process if this index is not last and disposal
        // method == 2 or method == 3
        if (i == lastIndex || !checkIfWillBeCleared(cur)) {
            drawFrame(bm, cur, gif->SColorMap);
        }
    }

    // save index
    fLastDrawIndex = lastIndex;
    return true;
}

///////////////////////////////////////////////////////////////////////////////

#include "SkTRegistry.h"

SkMovie* Factory(SkStream* stream) {
    char buf[GIF_STAMP_LEN];
    if (stream->read(buf, GIF_STAMP_LEN) == GIF_STAMP_LEN) {
        if (memcmp(GIF_STAMP,   buf, GIF_STAMP_LEN) == 0 ||
                memcmp(GIF87_STAMP, buf, GIF_STAMP_LEN) == 0 ||
                memcmp(GIF89_STAMP, buf, GIF_STAMP_LEN) == 0) {
            // must rewind here, since our construct wants to re-read the data
            stream->rewind();
            return SkNEW_ARGS(SkGIFMovie, (stream));
        }
    }
    return NULL;
}

static SkTRegistry<SkMovie*, SkStream*> gReg(Factory);
