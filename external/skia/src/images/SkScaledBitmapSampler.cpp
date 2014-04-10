
/*
 * Copyright 2007 The Android Open Source Project
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


#include "SkScaledBitmapSampler.h"
#include "SkBitmap.h"
#include "SkColorPriv.h"
#include "SkDither.h"

// 8888

static bool Sample_Gray_D8888(void* SK_RESTRICT dstRow,
                              const uint8_t* SK_RESTRICT src,
                              int width, int deltaSrc, int, const SkPMColor[]) {
    SkPMColor* SK_RESTRICT dst = (SkPMColor*)dstRow;
    for (int x = 0; x < width; x++) {
        dst[x] = SkPackARGB32(0xFF, src[0], src[0], src[0]);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_RGBx_D8888(void* SK_RESTRICT dstRow,
                              const uint8_t* SK_RESTRICT src,
                              int width, int deltaSrc, int, const SkPMColor[]) {
    SkPMColor* SK_RESTRICT dst = (SkPMColor*)dstRow;
    for (int x = 0; x < width; x++) {
        dst[x] = SkPackARGB32(0xFF, src[0], src[1], src[2]);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_RGBA_D8888(void* SK_RESTRICT dstRow,
                              const uint8_t* SK_RESTRICT src,
                              int width, int deltaSrc, int, const SkPMColor[]) {
    SkPMColor* SK_RESTRICT dst = (SkPMColor*)dstRow;
    unsigned alphaMask = 0xFF;
#if defined(__ARM_HAVE_NEON) && defined(SK_CPU_LENDIAN)
    if (deltaSrc == 4)
    {
        asm volatile(
                "subs       %[width], %[width], #8                  \n\t"
                "blt        2f                                      \n\t"
                "vmov.i8    d18, #0xFF                              \n\t"
                "vmov.u16   q8, #0x80                               \n\t"
                "1:                                                 \n\t"
                "vld4.8     {d0, d1, d2, d3}, [%[src]]!             \n\t"
                //"pld        [%[src], #64]                           \n\t"
                "subs       %[width], %[width], #8                  \n\t"
                "vand       d18, d18, d3                            \n\t"
                "vmull.u8   q2, d0, d3                              \n\t"//a*b
                "vmull.u8   q3, d1, d3                              \n\t"
                "vmull.u8   q4, d2, d3                              \n\t"
                
                "vadd.u16   q2, q2, q8                              \n\t"//prod = a*b+128
                "vadd.u16   q3, q3, q8                              \n\t"
                "vadd.u16   q4, q4, q8                              \n\t"

                "vshr.u16   q5, q2, #8                              \n\t"//prod >> 8
                "vshr.u16   q6, q3, #8                              \n\t"
                "vshr.u16   q7, q4, #8                              \n\t"

                "vaddhn.u16  d0, q2, q5                             \n\t"
                "vaddhn.u16  d1, q3, q6                             \n\t"
                "vaddhn.u16  d2, q4, q7                             \n\t"
                "vst4.8     {d0, d1, d2, d3}, [%[dst]]!             \n\t"
                "bge        1b                                      \n\t"
                "vmov       r5, r6, d18                             \n\t"
                "and        r5, r6, r5                              \n\t"
                "and        r5, r5, r5, lsr #16                     \n\t"
                "and        r5, r5, r5, lsr #8                      \n\t"
                "mov        %[alphaMask], r5                        \n\t"
                "2:                                                 \n\t"
                "add        %[width], %[width], #8                  \n\t"
                :[width] "+r" (width), [src] "+r" (src), [dst] "+r" (dst), [alphaMask] "+r" (alphaMask)
                :
                :"memory", "r5", "r6", "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10", "d11", "d12", "d13", "d14", "d15", "d16", "d17", "d18"
                );
        for (int x = 0; x < width; x++) {
            unsigned alpha = src[3];
            dst[x] = SkPreMultiplyARGB(alpha, src[0], src[1], src[2]);
            src += deltaSrc;
            alphaMask &= alpha;
        }
        return alphaMask != 0xFF;
    }
    else
#endif
    {
        for (int x = 0; x < width; x++) {
            unsigned alpha = src[3];
            dst[x] = SkPreMultiplyARGB(alpha, src[0], src[1], src[2]);
            src += deltaSrc;
            alphaMask &= alpha;
        }
        return alphaMask != 0xFF;
    }
}

// 565

static bool Sample_Gray_D565(void* SK_RESTRICT dstRow,
                             const uint8_t* SK_RESTRICT src,
                             int width, int deltaSrc, int, const SkPMColor[]) {
    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    for (int x = 0; x < width; x++) {
        dst[x] = SkPack888ToRGB16(src[0], src[0], src[0]);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_Gray_D565_D(void* SK_RESTRICT dstRow,
                               const uint8_t* SK_RESTRICT src,
                           int width, int deltaSrc, int y, const SkPMColor[]) {
    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    DITHER_565_SCAN(y);
    for (int x = 0; x < width; x++) {
        dst[x] = SkDitherRGBTo565(src[0], src[0], src[0], DITHER_VALUE(x));
        src += deltaSrc;
    }
    return false;
}

static bool Sample_RGBx_D565(void* SK_RESTRICT dstRow,
                             const uint8_t* SK_RESTRICT src,
                             int width, int deltaSrc, int, const SkPMColor[]) {
    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    for (int x = 0; x < width; x++) {
        dst[x] = SkPack888ToRGB16(src[0], src[1], src[2]);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_D565_D565(void* SK_RESTRICT dstRow,
                             const uint8_t* SK_RESTRICT src,
                             int width, int deltaSrc, int, const SkPMColor[]) {
    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    uint16_t* SK_RESTRICT castedSrc = (uint16_t*) src;
    for (int x = 0; x < width; x++) {
        dst[x] = castedSrc[0];
        castedSrc += deltaSrc >> 1;
    }
    return false;
}

static bool Sample_RGBx_D565_D(void* SK_RESTRICT dstRow,
                               const uint8_t* SK_RESTRICT src,
                           int width, int deltaSrc, int y, const SkPMColor[]) {
    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    DITHER_565_SCAN(y);
    for (int x = 0; x < width; x++) {
        dst[x] = SkDitherRGBTo565(src[0], src[1], src[2], DITHER_VALUE(x));
        src += deltaSrc;
    }
    return false;
}

// 4444

static bool Sample_Gray_D4444(void* SK_RESTRICT dstRow,
                              const uint8_t* SK_RESTRICT src,
                              int width, int deltaSrc, int, const SkPMColor[]) {
    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    for (int x = 0; x < width; x++) {
        unsigned gray = src[0] >> 4;
        dst[x] = SkPackARGB4444(0xF, gray, gray, gray);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_Gray_D4444_D(void* SK_RESTRICT dstRow,
                                const uint8_t* SK_RESTRICT src,
                            int width, int deltaSrc, int y, const SkPMColor[]) {
    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    DITHER_4444_SCAN(y);
    for (int x = 0; x < width; x++) {
        dst[x] = SkDitherARGB32To4444(0xFF, src[0], src[0], src[0],
                                      DITHER_VALUE(x));
        src += deltaSrc;
    }
    return false;
}

static bool Sample_RGBx_D4444(void* SK_RESTRICT dstRow,
                              const uint8_t* SK_RESTRICT src,
                              int width, int deltaSrc, int, const SkPMColor[]) {
    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    for (int x = 0; x < width; x++) {
        dst[x] = SkPackARGB4444(0xF, src[0] >> 4, src[1] >> 4, src[2] >> 4);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_RGBx_D4444_D(void* SK_RESTRICT dstRow,
                                const uint8_t* SK_RESTRICT src,
                            int width, int deltaSrc, int y, const SkPMColor[]) {
    SkPMColor16* dst = (SkPMColor16*)dstRow;
    DITHER_4444_SCAN(y);

    for (int x = 0; x < width; x++) {
        dst[x] = SkDitherARGB32To4444(0xFF, src[0], src[1], src[2],
                                      DITHER_VALUE(x));
        src += deltaSrc;
    }
    return false;
}

static bool Sample_RGBA_D4444(void* SK_RESTRICT dstRow,
                              const uint8_t* SK_RESTRICT src,
                              int width, int deltaSrc, int, const SkPMColor[]) {
    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    unsigned alphaMask = 0xFF;

    for (int x = 0; x < width; x++) {
        unsigned alpha = src[3];
        SkPMColor c = SkPreMultiplyARGB(alpha, src[0], src[1], src[2]);
        dst[x] = SkPixel32ToPixel4444(c);
        src += deltaSrc;
        alphaMask &= alpha;
    }
    return alphaMask != 0xFF;
}

static bool Sample_RGBA_D4444_D(void* SK_RESTRICT dstRow,
                                const uint8_t* SK_RESTRICT src,
                            int width, int deltaSrc, int y, const SkPMColor[]) {
    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    unsigned alphaMask = 0xFF;
    DITHER_4444_SCAN(y);

    for (int x = 0; x < width; x++) {
        unsigned alpha = src[3];
        SkPMColor c = SkPreMultiplyARGB(alpha, src[0], src[1], src[2]);
        dst[x] = SkDitherARGB32To4444(c, DITHER_VALUE(x));
        src += deltaSrc;
        alphaMask &= alpha;
    }
    return alphaMask != 0xFF;
}

// Index

#define A32_MASK_IN_PLACE   (SkPMColor)(SK_A32_MASK << SK_A32_SHIFT)

static bool Sample_Index_D8888(void* SK_RESTRICT dstRow,
                               const uint8_t* SK_RESTRICT src,
                       int width, int deltaSrc, int, const SkPMColor ctable[]) {

    SkPMColor* SK_RESTRICT dst = (SkPMColor*)dstRow;
    SkPMColor cc = A32_MASK_IN_PLACE;
    for (int x = 0; x < width; x++) {
        SkPMColor c = ctable[*src];
        cc &= c;
        dst[x] = c;
        src += deltaSrc;
    }
    return cc != A32_MASK_IN_PLACE;
}

static bool Sample_Index_D565(void* SK_RESTRICT dstRow,
                               const uint8_t* SK_RESTRICT src,
                       int width, int deltaSrc, int, const SkPMColor ctable[]) {

    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    for (int x = 0; x < width; x++) {
        dst[x] = SkPixel32ToPixel16(ctable[*src]);
        src += deltaSrc;
    }
    return false;
}

static bool Sample_Index_D565_D(void* SK_RESTRICT dstRow,
                                const uint8_t* SK_RESTRICT src, int width,
                                int deltaSrc, int y, const SkPMColor ctable[]) {

    uint16_t* SK_RESTRICT dst = (uint16_t*)dstRow;
    DITHER_565_SCAN(y);

    for (int x = 0; x < width; x++) {
        SkPMColor c = ctable[*src];
        dst[x] = SkDitherRGBTo565(SkGetPackedR32(c), SkGetPackedG32(c),
                                  SkGetPackedB32(c), DITHER_VALUE(x));
        src += deltaSrc;
    }
    return false;
}

static bool Sample_Index_D4444(void* SK_RESTRICT dstRow,
                               const uint8_t* SK_RESTRICT src, int width,
                               int deltaSrc, int y, const SkPMColor ctable[]) {

    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    SkPMColor cc = A32_MASK_IN_PLACE;
    for (int x = 0; x < width; x++) {
        SkPMColor c = ctable[*src];
        cc &= c;
        dst[x] = SkPixel32ToPixel4444(c);
        src += deltaSrc;
    }
    return cc != A32_MASK_IN_PLACE;
}

static bool Sample_Index_D4444_D(void* SK_RESTRICT dstRow,
                                 const uint8_t* SK_RESTRICT src, int width,
                                int deltaSrc, int y, const SkPMColor ctable[]) {

    SkPMColor16* SK_RESTRICT dst = (SkPMColor16*)dstRow;
    SkPMColor cc = A32_MASK_IN_PLACE;
    DITHER_4444_SCAN(y);

    for (int x = 0; x < width; x++) {
        SkPMColor c = ctable[*src];
        cc &= c;
        dst[x] = SkDitherARGB32To4444(c, DITHER_VALUE(x));
        src += deltaSrc;
    }
    return cc != A32_MASK_IN_PLACE;
}

static bool Sample_Index_DI(void* SK_RESTRICT dstRow,
                            const uint8_t* SK_RESTRICT src,
                            int width, int deltaSrc, int, const SkPMColor[]) {
    if (1 == deltaSrc) {
        memcpy(dstRow, src, width);
    } else {
        uint8_t* SK_RESTRICT dst = (uint8_t*)dstRow;
        for (int x = 0; x < width; x++) {
            dst[x] = src[0];
            src += deltaSrc;
        }
    }
    return false;
}

///////////////////////////////////////////////////////////////////////////////

#include "SkScaledBitmapSampler.h"

SkScaledBitmapSampler::SkScaledBitmapSampler(int width, int height,
                                             int sampleSize) {
    if (width <= 0 || height <= 0) {
        sk_throw();
    }
    
    if (sampleSize <= 1) {
        fScaledWidth = width;
        fScaledHeight = height;
        fX0 = fY0 = 0;
        fDX = fDY = 1;
        return;
    }
    
    int dx = SkMin32(sampleSize, width);
    int dy = SkMin32(sampleSize, height);
    
    fScaledWidth = width / dx;
    fScaledHeight = height / dy;
    
    SkASSERT(fScaledWidth > 0);
    SkASSERT(fScaledHeight > 0);
    
    fX0 = dx >> 1;
    fY0 = dy >> 1;
    
    SkASSERT(fX0 >= 0 && fX0 < width);
    SkASSERT(fY0 >= 0 && fY0 < height);
    
    fDX = dx;
    fDY = dy;
    
    SkASSERT(fDX > 0 && (fX0 + fDX * (fScaledWidth - 1)) < width);
    SkASSERT(fDY > 0 && (fY0 + fDY * (fScaledHeight - 1)) < height);
    
    fRowProc = NULL;
    fCTable = NULL;
}

bool SkScaledBitmapSampler::begin(SkBitmap* dst, SrcConfig sc, bool dither,
                                  const SkPMColor ctable[]) {
    static const RowProc gProcs[] = {
        // 8888 (no dither distinction)
        Sample_Gray_D8888,  Sample_Gray_D8888,
        Sample_RGBx_D8888,  Sample_RGBx_D8888,
        Sample_RGBA_D8888,  Sample_RGBA_D8888,
        Sample_Index_D8888, Sample_Index_D8888,
        NULL,               NULL,
        // 565 (no alpha distinction)
        Sample_Gray_D565,   Sample_Gray_D565_D,
        Sample_RGBx_D565,   Sample_RGBx_D565_D,
        Sample_RGBx_D565,   Sample_RGBx_D565_D,
        Sample_Index_D565,  Sample_Index_D565_D,
        Sample_D565_D565,   Sample_D565_D565,
        // 4444
        Sample_Gray_D4444,  Sample_Gray_D4444_D,
        Sample_RGBx_D4444,  Sample_RGBx_D4444_D,
        Sample_RGBA_D4444,  Sample_RGBA_D4444_D,
        Sample_Index_D4444, Sample_Index_D4444_D,
        NULL,               NULL,
        // Index8
        NULL,               NULL,
        NULL,               NULL,
        NULL,               NULL,
        Sample_Index_DI,    Sample_Index_DI,
        NULL,               NULL,
    };

    fCTable = ctable;

    int index = 0;
    if (dither) {
        index += 1;
    }
    switch (sc) {
        case SkScaledBitmapSampler::kGray:
            fSrcPixelSize = 1;
            index += 0;
            break;
        case SkScaledBitmapSampler::kRGB:
            fSrcPixelSize = 3;
            index += 2;
            break;
        case SkScaledBitmapSampler::kRGBX:
            fSrcPixelSize = 4;
            index += 2;
            break;
        case SkScaledBitmapSampler::kRGBA:
            fSrcPixelSize = 4;
            index += 4;
            break;
        case SkScaledBitmapSampler::kIndex:
            fSrcPixelSize = 1;
            index += 6;
            break;
        case SkScaledBitmapSampler::kRGB_565:
            fSrcPixelSize = 2;
            index += 8;
            break;
        default:
            return false;
    }

    switch (dst->config()) {
        case SkBitmap::kARGB_8888_Config:
            index += 0;
            break;
        case SkBitmap::kRGB_565_Config:
            index += 10;
            break;
        case SkBitmap::kARGB_4444_Config:
            index += 20;
            break;
        case SkBitmap::kIndex8_Config:
            index += 30;
            break;
        default:
            return false;
    }
    
    fRowProc = gProcs[index];
    fDstRow = (char*)dst->getPixels();
    fDstRowBytes = dst->rowBytes();
    fCurrY = 0;
    return fRowProc != NULL;
}

bool SkScaledBitmapSampler::next(const uint8_t* SK_RESTRICT src) {
    SkASSERT((unsigned)fCurrY < (unsigned)fScaledHeight);

    bool hadAlpha = fRowProc(fDstRow, src + fX0 * fSrcPixelSize, fScaledWidth,
                             fDX * fSrcPixelSize, fCurrY, fCTable);
    fDstRow += fDstRowBytes;
    fCurrY += 1;
    return hadAlpha;
}
