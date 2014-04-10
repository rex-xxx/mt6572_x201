
/*
 * Copyright 2006 The Android Open Source Project
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


#include "SkBlitRow.h"
#include "SkCoreBlitters.h"
#include "SkColorPriv.h"
#include "SkDither.h"
#include "SkShader.h"
#include "SkTemplatesPriv.h"
#include "SkUtils.h"
#include "SkXfermode.h"

#if defined(__ARM_HAVE_NEON) && defined(SK_CPU_LENDIAN)
    #define SK_USE_NEON
    #include <arm_neon.h>
#else
    // if we don't have neon, then our black blitter is worth the extra code
    #define USE_BLACK_BLITTER
#endif

void sk_dither_memset16(uint16_t dst[], uint16_t value, uint16_t other,
                        int count) {
    if (count > 0) {
        // see if we need to write one short before we can cast to an 4byte ptr
        // (we do this subtract rather than (unsigned)dst so we don't get warnings
        //  on 64bit machines)
        if (((char*)dst - (char*)0) & 2) {
            *dst++ = value;
            count -= 1;
            SkTSwap(value, other);
        }
        
        // fast way to set [value,other] pairs
#ifdef SK_CPU_BENDIAN
        sk_memset32((uint32_t*)dst, (value << 16) | other, count >> 1);
#else
        sk_memset32((uint32_t*)dst, (other << 16) | value, count >> 1);
#endif
        
        if (count & 1) {
            dst[count - 1] = value;
        }
    }
}

///////////////////////////////////////////////////////////////////////////////

class SkRGB16_Blitter : public SkRasterBlitter {
public:
    SkRGB16_Blitter(const SkBitmap& device, const SkPaint& paint);
    virtual void blitH(int x, int y, int width);
    virtual void blitAntiH(int x, int y, const SkAlpha* antialias,
                           const int16_t* runs);
    virtual void blitV(int x, int y, int height, SkAlpha alpha);
    virtual void blitRect(int x, int y, int width, int height);
    virtual void blitMask(const SkMask&,
                          const SkIRect&);
    virtual const SkBitmap* justAnOpaqueColor(uint32_t*);
    
protected:
    SkPMColor   fSrcColor32;
    uint32_t    fExpandedRaw16;
    unsigned    fScale;
    uint16_t    fColor16;       // already scaled by fScale
    uint16_t    fRawColor16;    // unscaled
    uint16_t    fRawDither16;   // unscaled
    SkBool8     fDoDither;
    
    // illegal
    SkRGB16_Blitter& operator=(const SkRGB16_Blitter&);
    
    typedef SkRasterBlitter INHERITED;
};

class SkRGB16_Opaque_Blitter : public SkRGB16_Blitter {
public:
    SkRGB16_Opaque_Blitter(const SkBitmap& device, const SkPaint& paint);
    virtual void blitH(int x, int y, int width);
    virtual void blitAntiH(int x, int y, const SkAlpha* antialias,
                           const int16_t* runs);
    virtual void blitV(int x, int y, int height, SkAlpha alpha);
    virtual void blitRect(int x, int y, int width, int height);
    virtual void blitMask(const SkMask&,
                          const SkIRect&);
    
private:
    typedef SkRGB16_Blitter INHERITED;
};

#ifdef USE_BLACK_BLITTER
class SkRGB16_Black_Blitter : public SkRGB16_Opaque_Blitter {
public:
    SkRGB16_Black_Blitter(const SkBitmap& device, const SkPaint& paint);
    virtual void blitMask(const SkMask&, const SkIRect&);
    virtual void blitAntiH(int x, int y, const SkAlpha* antialias,
                           const int16_t* runs);
    
private:
    typedef SkRGB16_Opaque_Blitter INHERITED;
};
#endif

class SkRGB16_Shader_Blitter : public SkShaderBlitter {
public:
    SkRGB16_Shader_Blitter(const SkBitmap& device, const SkPaint& paint);
    virtual ~SkRGB16_Shader_Blitter();
    virtual void blitH(int x, int y, int width);
    virtual void blitAntiH(int x, int y, const SkAlpha* antialias,
                           const int16_t* runs);
    virtual void blitRect(int x, int y, int width, int height);
    
protected:
    SkPMColor*      fBuffer;
    SkBlitRow::Proc fOpaqueProc;
    SkBlitRow::Proc fAlphaProc;
    
private:
    // illegal
    SkRGB16_Shader_Blitter& operator=(const SkRGB16_Shader_Blitter&);
    
    typedef SkShaderBlitter INHERITED;
};

// used only if the shader can perform shadSpan16
class SkRGB16_Shader16_Blitter : public SkRGB16_Shader_Blitter {
public:
    SkRGB16_Shader16_Blitter(const SkBitmap& device, const SkPaint& paint);
    virtual void blitH(int x, int y, int width);
    virtual void blitAntiH(int x, int y, const SkAlpha* antialias,
                           const int16_t* runs);
    virtual void blitRect(int x, int y, int width, int height);
    
private:
    typedef SkRGB16_Shader_Blitter INHERITED;
};

class SkRGB16_Shader_Xfermode_Blitter : public SkShaderBlitter {
public:
    SkRGB16_Shader_Xfermode_Blitter(const SkBitmap& device, const SkPaint& paint);
    virtual ~SkRGB16_Shader_Xfermode_Blitter();
    virtual void blitH(int x, int y, int width);
    virtual void blitAntiH(int x, int y, const SkAlpha* antialias,
                           const int16_t* runs);
    
private:
    SkXfermode* fXfermode;
    SkPMColor*  fBuffer;
    uint8_t*    fAAExpand;
    
    // illegal
    SkRGB16_Shader_Xfermode_Blitter& operator=(const SkRGB16_Shader_Xfermode_Blitter&);
    
    typedef SkShaderBlitter INHERITED;
};

///////////////////////////////////////////////////////////////////////////////
#ifdef USE_BLACK_BLITTER
SkRGB16_Black_Blitter::SkRGB16_Black_Blitter(const SkBitmap& device, const SkPaint& paint)
    : INHERITED(device, paint) {
    SkASSERT(paint.getShader() == NULL);
    SkASSERT(paint.getColorFilter() == NULL);
    SkASSERT(paint.getXfermode() == NULL);
    SkASSERT(paint.getColor() == SK_ColorBLACK);
}

#if 1
#define black_8_pixels(mask, dst)       \
    do {                                \
        if (mask & 0x80) dst[0] = 0;    \
        if (mask & 0x40) dst[1] = 0;    \
        if (mask & 0x20) dst[2] = 0;    \
        if (mask & 0x10) dst[3] = 0;    \
        if (mask & 0x08) dst[4] = 0;    \
        if (mask & 0x04) dst[5] = 0;    \
        if (mask & 0x02) dst[6] = 0;    \
        if (mask & 0x01) dst[7] = 0;    \
    } while (0)
#else
static inline black_8_pixels(U8CPU mask, uint16_t dst[])
{
    if (mask & 0x80) dst[0] = 0;
    if (mask & 0x40) dst[1] = 0;
    if (mask & 0x20) dst[2] = 0;
    if (mask & 0x10) dst[3] = 0;
    if (mask & 0x08) dst[4] = 0;
    if (mask & 0x04) dst[5] = 0;
    if (mask & 0x02) dst[6] = 0;
    if (mask & 0x01) dst[7] = 0;
}
#endif

#define SK_BLITBWMASK_NAME                  SkRGB16_Black_BlitBW
#define SK_BLITBWMASK_ARGS
#define SK_BLITBWMASK_BLIT8(mask, dst)      black_8_pixels(mask, dst)
#define SK_BLITBWMASK_GETADDR               getAddr16
#define SK_BLITBWMASK_DEVTYPE               uint16_t
#include "SkBlitBWMaskTemplate.h"

void SkRGB16_Black_Blitter::blitMask(const SkMask& mask,
                                     const SkIRect& clip) {
    if (mask.fFormat == SkMask::kBW_Format) {
        SkRGB16_Black_BlitBW(fDevice, mask, clip);
    } else {
        uint16_t* SK_RESTRICT device = fDevice.getAddr16(clip.fLeft, clip.fTop);
        const uint8_t* SK_RESTRICT alpha = mask.getAddr8(clip.fLeft, clip.fTop);
        unsigned width = clip.width();
        unsigned height = clip.height();
        unsigned deviceRB = fDevice.rowBytes() - (width << 1);
        unsigned maskRB = mask.fRowBytes - width;

        SkASSERT((int)height > 0);
        SkASSERT((int)width > 0);
        SkASSERT((int)deviceRB >= 0);
        SkASSERT((int)maskRB >= 0);

        do {
            unsigned w = width;
            do {
                unsigned aa = *alpha++;
                *device = SkAlphaMulRGB16(*device, SkAlpha255To256(255 - aa));
                device += 1;
            } while (--w != 0);
            device = (uint16_t*)((char*)device + deviceRB);
            alpha += maskRB;
        } while (--height != 0);
    }
}

void SkRGB16_Black_Blitter::blitAntiH(int x, int y,
                                      const SkAlpha* SK_RESTRICT antialias,
                                      const int16_t* SK_RESTRICT runs) {
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);

    for (;;) {
        int count = runs[0];
        SkASSERT(count >= 0);
        if (count <= 0) {
            return;
        }
        runs += count;

        unsigned aa = antialias[0];
        antialias += count;
        if (aa) {
            if (aa == 255) {
                memset(device, 0, count << 1);
            } else {
                aa = SkAlpha255To256(255 - aa);
                do {
                    *device = SkAlphaMulRGB16(*device, aa);
                    device += 1;
                } while (--count != 0);
                continue;
            }
        }
        device += count;
    }
}
#endif

///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

SkRGB16_Opaque_Blitter::SkRGB16_Opaque_Blitter(const SkBitmap& device,
                                               const SkPaint& paint)
: INHERITED(device, paint) {}

void SkRGB16_Opaque_Blitter::blitH(int x, int y, int width) {
    SkASSERT(width > 0);
    SkASSERT(x + width <= fDevice.width());
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    uint16_t srcColor = fColor16;

    SkASSERT(fRawColor16 == srcColor);
    if (fDoDither) {
        uint16_t ditherColor = fRawDither16;
        if ((x ^ y) & 1) {
            SkTSwap(ditherColor, srcColor);
        }
        sk_dither_memset16(device, srcColor, ditherColor, width);
    } else {
        sk_memset16(device, srcColor, width);
    }
}

// return 1 or 0 from a bool
static inline int Bool2Int(int value) {
	return !!value;
}

void SkRGB16_Opaque_Blitter::blitAntiH(int x, int y,
                                       const SkAlpha* SK_RESTRICT antialias,
                                       const int16_t* SK_RESTRICT runs) {
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    uint16_t    srcColor = fRawColor16;
    uint32_t    srcExpanded = fExpandedRaw16;
    int         ditherInt = Bool2Int(fDoDither);
    uint16_t    ditherColor = fRawDither16;
    // if we have no dithering, this will always fail
    if ((x ^ y) & ditherInt) {
        SkTSwap(ditherColor, srcColor);
    }
    for (;;) {
        int count = runs[0];
        SkASSERT(count >= 0);
        if (count <= 0) {
            return;
        }
        runs += count;

        unsigned aa = antialias[0];
        antialias += count;
        if (aa) {
            if (aa == 255) {
                if (ditherInt) {
                    sk_dither_memset16(device, srcColor,
                                       ditherColor, count);
                } else {
                    sk_memset16(device, srcColor, count);
                }
            } else {
                // TODO: respect fDoDither
                unsigned scale5 = SkAlpha255To256(aa) >> 3;
                uint32_t src32 = srcExpanded * scale5;
                scale5 = 32 - scale5; // now we can use it on the device
                int n = count;
                do {
                    uint32_t dst32 = SkExpand_rgb_16(*device) * scale5;
                    *device++ = SkCompact_rgb_16((src32 + dst32) >> 5);
                } while (--n != 0);
                goto DONE;
            }
        }
        device += count;

    DONE:
        // if we have no dithering, this will always fail
        if (count & ditherInt) {
            SkTSwap(ditherColor, srcColor);
        }
    }
}

#define solid_8_pixels(mask, dst, color)    \
    do {                                    \
        if (mask & 0x80) dst[0] = color;    \
        if (mask & 0x40) dst[1] = color;    \
        if (mask & 0x20) dst[2] = color;    \
        if (mask & 0x10) dst[3] = color;    \
        if (mask & 0x08) dst[4] = color;    \
        if (mask & 0x04) dst[5] = color;    \
        if (mask & 0x02) dst[6] = color;    \
        if (mask & 0x01) dst[7] = color;    \
    } while (0)

#define SK_BLITBWMASK_NAME                  SkRGB16_BlitBW
#define SK_BLITBWMASK_ARGS                  , uint16_t color
#define SK_BLITBWMASK_BLIT8(mask, dst)      solid_8_pixels(mask, dst, color)
#define SK_BLITBWMASK_GETADDR               getAddr16
#define SK_BLITBWMASK_DEVTYPE               uint16_t
#include "SkBlitBWMaskTemplate.h"

static U16CPU blend_compact(uint32_t src32, uint32_t dst32, unsigned scale5) {
    return SkCompact_rgb_16(dst32 + ((src32 - dst32) * scale5 >> 5));
}

void SkRGB16_Opaque_Blitter::blitMask(const SkMask& mask,
                                      const SkIRect& clip) {
    if (mask.fFormat == SkMask::kBW_Format) {
        SkRGB16_BlitBW(fDevice, mask, clip, fColor16);
        return;
    }

    uint16_t* SK_RESTRICT device = fDevice.getAddr16(clip.fLeft, clip.fTop);
    const uint8_t* SK_RESTRICT alpha = mask.getAddr8(clip.fLeft, clip.fTop);
    int width = clip.width();
    int height = clip.height();
    unsigned    deviceRB = fDevice.rowBytes() - (width << 1);
    unsigned    maskRB = mask.fRowBytes - width;
    uint32_t    expanded32 = fExpandedRaw16;

#ifdef SK_USE_NEON
#define	UNROLL	8
    do {
        int w = width;
        if (w >= UNROLL) {
            uint32x4_t color;		/* can use same one */
            uint32x4_t dev_lo, dev_hi;
            uint32x4_t t1, t2;
            uint32x4_t wn1, wn2;
            uint16x4_t odev_lo, odev_hi;
            uint16x4_t alpha_lo, alpha_hi;
            uint16x8_t  alpha_full;
            
            color = vdupq_n_u32(expanded32);
            
            do {
                /* alpha is 8x8, widen and split to get pair of 16x4's */
                alpha_full = vmovl_u8(vld1_u8(alpha));
                alpha_full = vaddq_u16(alpha_full, vshrq_n_u16(alpha_full,7));
                alpha_full = vshrq_n_u16(alpha_full, 3);
                alpha_lo = vget_low_u16(alpha_full);
                alpha_hi = vget_high_u16(alpha_full);
                
                dev_lo = vmovl_u16(vld1_u16(device));
                dev_hi = vmovl_u16(vld1_u16(device+4));
                
                /* unpack in 32 bits */
                dev_lo = vorrq_u32(
                                   vandq_u32(dev_lo, vdupq_n_u32(0x0000F81F)),
                                   vshlq_n_u32(vandq_u32(dev_lo, 
                                                         vdupq_n_u32(0x000007E0)),
                                               16)
                                   );
                dev_hi = vorrq_u32(
                                   vandq_u32(dev_hi, vdupq_n_u32(0x0000F81F)),
                                   vshlq_n_u32(vandq_u32(dev_hi, 
                                                         vdupq_n_u32(0x000007E0)),
                                               16)
                                   );
                
                /* blend the two */
                t1 = vmulq_u32(vsubq_u32(color, dev_lo), vmovl_u16(alpha_lo));
                t1 = vshrq_n_u32(t1, 5);
                dev_lo = vaddq_u32(dev_lo, t1);
                
                t1 = vmulq_u32(vsubq_u32(color, dev_hi), vmovl_u16(alpha_hi));
                t1 = vshrq_n_u32(t1, 5);
                dev_hi = vaddq_u32(dev_hi, t1);
                
                /* re-compact and store */
                wn1 = vandq_u32(dev_lo, vdupq_n_u32(0x0000F81F)),
                wn2 = vshrq_n_u32(dev_lo, 16);
                wn2 = vandq_u32(wn2, vdupq_n_u32(0x000007E0));
                odev_lo = vmovn_u32(vorrq_u32(wn1, wn2));
                
                wn1 = vandq_u32(dev_hi, vdupq_n_u32(0x0000F81F)),
                wn2 = vshrq_n_u32(dev_hi, 16);
                wn2 = vandq_u32(wn2, vdupq_n_u32(0x000007E0));
                odev_hi = vmovn_u32(vorrq_u32(wn1, wn2));
                
                vst1_u16(device, odev_lo);
                vst1_u16(device+4, odev_hi);
                
                device += UNROLL;
                alpha += UNROLL;
                w -= UNROLL;
            } while (w >= UNROLL);
        }
        
        /* residuals (which is everything if we have no neon) */
        while (w > 0) {
            *device = blend_compact(expanded32, SkExpand_rgb_16(*device),
                                    SkAlpha255To256(*alpha++) >> 3);
            device += 1;
            --w;
        }
        device = (uint16_t*)((char*)device + deviceRB);
        alpha += maskRB;
    } while (--height != 0);
#undef	UNROLL
#else   // non-neon code
    do {
        int w = width;
        do {
            *device = blend_compact(expanded32, SkExpand_rgb_16(*device),
                                    SkAlpha255To256(*alpha++) >> 3);
            device += 1;
        } while (--w != 0);
        device = (uint16_t*)((char*)device + deviceRB);
        alpha += maskRB;
    } while (--height != 0);
#endif
}

void SkRGB16_Opaque_Blitter::blitV(int x, int y, int height, SkAlpha alpha) {
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    unsigned    deviceRB = fDevice.rowBytes();
    
    // TODO: respect fDoDither
    unsigned scale5 = SkAlpha255To256(alpha) >> 3;
    uint32_t src32 =  fExpandedRaw16 * scale5;
    scale5 = 32 - scale5;
    do {
        uint32_t dst32 = SkExpand_rgb_16(*device) * scale5;
        *device = SkCompact_rgb_16((src32 + dst32) >> 5);
        device = (uint16_t*)((char*)device + deviceRB);
    } while (--height != 0);
}

void SkRGB16_Opaque_Blitter::blitRect(int x, int y, int width, int height) {
    SkASSERT(x + width <= fDevice.width() && y + height <= fDevice.height());
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    unsigned    deviceRB = fDevice.rowBytes();
    uint16_t    color16 = fColor16;

    if (fDoDither) {
        uint16_t ditherColor = fRawDither16;
        if ((x ^ y) & 1) {
            SkTSwap(ditherColor, color16);
        }
        while (--height >= 0) {
            sk_dither_memset16(device, color16, ditherColor, width);
            SkTSwap(ditherColor, color16);
            device = (uint16_t*)((char*)device + deviceRB);
        }
    } else {  // no dither
        while (--height >= 0) {
            sk_memset16(device, color16, width);
            device = (uint16_t*)((char*)device + deviceRB);
        }
    }
}

///////////////////////////////////////////////////////////////////////////////

SkRGB16_Blitter::SkRGB16_Blitter(const SkBitmap& device, const SkPaint& paint)
    : INHERITED(device) {
    SkColor color = paint.getColor();

    fSrcColor32 = SkPreMultiplyColor(color);
    fScale = SkAlpha255To256(SkColorGetA(color));

    int r = SkColorGetR(color);
    int g = SkColorGetG(color);
    int b = SkColorGetB(color);

    fRawColor16 = fRawDither16 = SkPack888ToRGB16(r, g, b);
    // if we're dithered, use fRawDither16 to hold that.
    if ((fDoDither = paint.isDither()) != false) {
        fRawDither16 = SkDitherPack888ToRGB16(r, g, b);
    }

    fExpandedRaw16 = SkExpand_rgb_16(fRawColor16);

    fColor16 = SkPackRGB16( SkAlphaMul(r, fScale) >> (8 - SK_R16_BITS),
                            SkAlphaMul(g, fScale) >> (8 - SK_G16_BITS),
                            SkAlphaMul(b, fScale) >> (8 - SK_B16_BITS));
}

const SkBitmap* SkRGB16_Blitter::justAnOpaqueColor(uint32_t* value) {
    if (!fDoDither && 256 == fScale) {
        *value = fRawColor16;
        return &fDevice;
    }
    return NULL;
}

static uint32_t pmcolor_to_expand16(SkPMColor c) {
    unsigned r = SkGetPackedR32(c);
    unsigned g = SkGetPackedG32(c);
    unsigned b = SkGetPackedB32(c);
    return (g << 24) | (r << 13) | (b << 2);
}

static inline void blend32_16_row(SkPMColor src, uint16_t dst[], int count) {
    SkASSERT(count > 0);
    uint32_t src_expand = pmcolor_to_expand16(src);
    unsigned scale = SkAlpha255To256(0xFF - SkGetPackedA32(src)) >> 3;
    do {
        uint32_t dst_expand = SkExpand_rgb_16(*dst) * scale;
        *dst = SkCompact_rgb_16((src_expand + dst_expand) >> 5);
        dst += 1;
    } while (--count != 0);
}

#ifndef __arm__
void SkRGB16_Blitter::blitH(int x, int y, int width) {
    SkASSERT(width > 0);
    SkASSERT(x + width <= fDevice.width());
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);

    // TODO: respect fDoDither
    blend32_16_row(fSrcColor32, device, width);
}
#else
void SkRGB16_Blitter::blitH(int x, int y, int width) {
    SkASSERT(width > 0);
    SkASSERT(x + width <= fDevice.width());
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    SkPMColor src32 = fSrcColor32;
    uint32_t src_expand = pmcolor_to_expand16(src32);
    unsigned scale = SkAlpha255To256(0xFF - SkGetPackedA32(src32)) >> 3;

#ifdef SK_USE_NEON
      asm volatile (
            "cmp        %[width], #0                        \n\t"  //width> 0?
            "ble        5f                                  \n\t"

            "pld        [%[device], #0]                     \n\t"
            "movw       r9, #0xf81f                         \n\t"
            "movt       r9, #0x07e0                         \n\t"

            "vdup.32    q7, r9                              \n\t"//q7=mask_vec
            "vdup.32    q6, %[src_expand]                   \n\t"
            "vdup.32    q8, %[scale]                        \n\t"

            "movs       r4, %[width], lsr #4                \n\t"
            "beq        2f                                  \n\t"


            "1:                                             \n\t"
            "vld1.u16   {d0,d1,d2,d3}, [%[device]]          \n\t"
            "pld        [%[device], #128]                   \n\t"
            "subs       r4, r4, #1                          \n\t"
            
            "vand.32    q2, q0, q7                          \n\t"
            "vand.32    q4, q1, q7                          \n\t"
            "vrev32.16  q3, q0                              \n\t"
            "vrev32.16  q5, q1                              \n\t"
            "vand.32    q3, q3, q7                          \n\t"
            "vand.32    q5, q5, q7                          \n\t"

            "vmul.i32   q2, q2, q8                          \n\t"
            "vmul.i32   q4, q4, q8                          \n\t"
            "vmul.i32   q3, q3, q8                          \n\t"
            "vmul.i32   q5, q5, q8                          \n\t"
            "vadd.u32   q2, q2, q6                          \n\t"
            "vadd.u32   q4, q4, q6                          \n\t"
            "vadd.u32   q3, q3, q6                          \n\t"
            "vadd.u32   q5, q5, q6                          \n\t"
            "vshr.u32   q2, q2, #5                          \n\t"
            "vshr.u32   q4, q4, #5                          \n\t"
            "vshr.u32   q3, q3, #5                          \n\t"
            "vshr.u32   q5, q5, #5                          \n\t"

            "vand.u32   q2, q2, q7                          \n\t"
            "vand.u32   q4, q4, q7                          \n\t"
            "vand.u32   q3, q3, q7                          \n\t"
            "vand.u32   q5, q5, q7                          \n\t"
            "vrev32.16  q3, q3                              \n\t"
            "vrev32.16  q5, q5                              \n\t"
            "vorr.u16   q0, q2, q3                          \n\t"
            "vorr.u16   q1, q4, q5                          \n\t"

            "vst1.u16   {d0,d1,d2,d3}, [%[device]]!         \n\t"
            "bne        1b                                  \n\t"

            "2:                                             \n\t"
            "ands       r4, %[width], #0xF                  \n\t"
            "beq        5f                                  \n\t"
            "cmp        r4, #8                              \n\t"
            "blt        3f                                  \n\t"
            
            "vld1.u16   {d0,d1}, [%[device]]                \n\t"
            "vand.32    q2, q0, q7                          \n\t"
            "vrev32.16  q3, q0                              \n\t"
            "vand.32    q3, q3, q7                          \n\t"

            "vmul.i32   q2, q2, q8                          \n\t"
            "vmul.i32   q3, q3, q8                          \n\t"
            "vadd.u32   q2, q2, q6                          \n\t"
            "vadd.u32   q3, q3, q6                          \n\t"
            "vshr.u32   q2, q2, #5                          \n\t"
            "vshr.u32   q3, q3, #5                          \n\t"

            "vand.u32   q2, q2, q7                          \n\t"
            "vand.u32   q3, q3, q7                          \n\t"
            "vrev32.16  q3, q3                              \n\t"
            "vorr.u16   q0, q2, q3                          \n\t"

            "vst1.u16   {d0, d1}, [%[device]]!              \n\t"


            "3:                                             \n\t"
            "ands       r4, %[width], #0x7                  \n\t"
            "beq        5f                                  \n\t"

            "4:                                             \n\t"
            "ldrh       r10, [%[device], #0]                \n\t"  //r10 = current = *device
            "subs       r4, r4, #1                          \n\t"  //width--;
            "pld        [%[device], #8]                     \n\t"
            "orr        r8, r10, r10, lsl #16               \n\t" //(current << 16 | current)
            "and        r10, r8, r9                         \n\t" //r10 = &mask
            "mla        r8, r10, %[scale], %[src_expand]    \n\t"  //r8 = src_expand + dst_expand
            "and        r8, r9, r8, lsr #5                  \n\t" //r8 & mask
            "orr        r10, r8, r8, lsr #16                \n\t" //must trunk the high 16bits, if not, generate error result
            "strh       r10, [%[device]], #2                \n\t"
            "bne        4b                                  \n\t"

            "5:                                             \n\t"
            :[device] "+r" (device)
            :[width] "r" (width), [src_expand] "r" (src_expand), [scale] "r" (scale)
            :"memory", "r4", "r8", "r9", "r10", "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10", "d11", "d12", "d13", "d14", "d15", "d16", "d17"
            );
#else
    asm volatile(
            "pld        [%[device]]                         \n\t"
            "mov        r9, #0x7E0                          \n\t"
            "mov        r10, #0xFF                          \n\t"
            "orr        r10, r10, r10, lsl #8               \n\t" //r10 = 0xFFFF
            "bic        r10, #0x7E0                         \n\t"
            "orr        r9, r10, r9, lsl #16                \n\t"  //r9 = mask

            "1:                                             \n\t"
            "ldrh       r10, [%[device], #0]                \n\t"  //r10 = current = *device
            "subs       %[width], %[width], #1              \n\t"  //width--;
            "pld        [%[device], #8]                     \n\t"
            "orr        r8, r10, r10, lsl #16               \n\t" //(current << 16 | current)
            "and        r10, r8, r9                         \n\t" //r10 = &mask
            "mla        r8, r10, %[scale], %[src_expand]    \n\t"  //r8 = src_expand + dst_expand
            "and        r8, r9, r8, lsr #5                  \n\t" //r8 & mask
            "orr        r10, r8, r8, lsr #16                \n\t" //must trunk the high 16bits, if not, generate error result
            "strh       r10, [%[device]], #2                \n\t"
            "bne        1b                                  \n\t"

            :[device] "+r" (device)
            :[width] "r" (width), [scale] "r" (scale), [src_expand] "r" (src_expand)
            :"memory", "r8", "r9", "r10"
            );
#endif
}
#endif

#ifndef __arm__
void SkRGB16_Blitter::blitAntiH(int x, int y,
                                const SkAlpha* SK_RESTRICT antialias,
                                const int16_t* SK_RESTRICT runs) {
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    uint32_t    srcExpanded = fExpandedRaw16;
    unsigned    scale = fScale;

    // TODO: respect fDoDither
    for (;;) {
        int count = runs[0];
        SkASSERT(count >= 0);
        if (count <= 0) {
            return;
        }
        runs += count;

        unsigned aa = antialias[0];
        antialias += count;
        if (aa) {
            unsigned scale5 = SkAlpha255To256(aa) * scale >> (8 + 3);
            uint32_t src32 =  srcExpanded * scale5;
            scale5 = 32 - scale5;
            do {
                uint32_t dst32 = SkExpand_rgb_16(*device) * scale5;
                *device++ = SkCompact_rgb_16((src32 + dst32) >> 5);
            } while (--count != 0);
            continue;
        }
        device += count;
    }
}
#else
void SkRGB16_Blitter::blitAntiH(int x, int y,
                                const SkAlpha* SK_RESTRICT antialias,
                                const int16_t* SK_RESTRICT runs) {
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    uint32_t    srcExpanded = fExpandedRaw16;
    unsigned    scale = fScale;

    //generate mask
    uint32_t mask = 0;
    asm volatile(
            "mov        %[mask], #0x7E0                     \n\t"
            "mov        r10, #0xFF                          \n\t"
            "orr        r10, r10, r10, lsl #8               \n\t" //r10 = 0xFFFF
            "bic        r10, #0x7E0                         \n\t"
            "orr        %[mask], r10, %[mask], lsl #16      \n\t" //r9 = mask
            :[mask] "+r" (mask)
            :
            :"memory", "r10"
            );

    // TODO: respect fDoDither
    for (;;) {
        int count = runs[0];
        SkASSERT(count >= 0);
        if (count <= 0) {
            return;
        }
        runs += count;

        unsigned aa = antialias[0];
        antialias += count;
        if (aa) {
            unsigned scale5 = SkAlpha255To256(aa) * scale >> (8 + 3);
            uint32_t src32 =  srcExpanded * scale5;
            scale5 = 32 - scale5;

            asm volatile(
                    "2:                                             \n\t" //inner loop
                    "ldrh       r10, [%[device], #0]                \n\t"  //r10 = current = *device
                    "subs       %[count], %[count], #1              \n\t"  //width--;
                    "pld        [%[device], #8]                     \n\t"
                    "orr        r9, r10, r10, lsl #16               \n\t" //(current << 16 | current)
                    "and        r10, r9, %[mask]                    \n\t" //r10 = &mask
                    "mla        r9, r10, %[scale5], %[src32]        \n\t"  //r9 = src_expand + dst_expand
                    "and        r9, %[mask], r9, lsr #5             \n\t" //r9 & mask
                    "orr        r10, r9, r9, lsr #16                \n\t" //must trunk the high 16bits, if not, generate error result
                    "strh       r10, [%[device]], #2                \n\t"
                    "bne        2b                                  \n\t"
                    :[device] "+r" (device)
                    :[count] "r" (count), [mask] "r" (mask), [scale5] "r" (scale5), [src32] "r" (src32)
                    :"memory", "r9", "r10"
                    );

            continue;
        }
        device += count;
    }
}
#endif

static inline void blend_8_pixels(U8CPU bw, uint16_t dst[], unsigned dst_scale,
                                  U16CPU srcColor) {
    if (bw & 0x80) dst[0] = srcColor + SkAlphaMulRGB16(dst[0], dst_scale);
    if (bw & 0x40) dst[1] = srcColor + SkAlphaMulRGB16(dst[1], dst_scale);
    if (bw & 0x20) dst[2] = srcColor + SkAlphaMulRGB16(dst[2], dst_scale);
    if (bw & 0x10) dst[3] = srcColor + SkAlphaMulRGB16(dst[3], dst_scale);
    if (bw & 0x08) dst[4] = srcColor + SkAlphaMulRGB16(dst[4], dst_scale);
    if (bw & 0x04) dst[5] = srcColor + SkAlphaMulRGB16(dst[5], dst_scale);
    if (bw & 0x02) dst[6] = srcColor + SkAlphaMulRGB16(dst[6], dst_scale);
    if (bw & 0x01) dst[7] = srcColor + SkAlphaMulRGB16(dst[7], dst_scale);
}

#define SK_BLITBWMASK_NAME                  SkRGB16_BlendBW
#define SK_BLITBWMASK_ARGS                  , unsigned dst_scale, U16CPU src_color
#define SK_BLITBWMASK_BLIT8(mask, dst)      blend_8_pixels(mask, dst, dst_scale, src_color)
#define SK_BLITBWMASK_GETADDR               getAddr16
#define SK_BLITBWMASK_DEVTYPE               uint16_t
#include "SkBlitBWMaskTemplate.h"

void SkRGB16_Blitter::blitMask(const SkMask& mask,
                               const SkIRect& clip) {
    if (mask.fFormat == SkMask::kBW_Format) {
        SkRGB16_BlendBW(fDevice, mask, clip, 256 - fScale, fColor16);
        return;
    }

    uint16_t* SK_RESTRICT device = fDevice.getAddr16(clip.fLeft, clip.fTop);
    const uint8_t* SK_RESTRICT alpha = mask.getAddr8(clip.fLeft, clip.fTop);
    int width = clip.width();
    int height = clip.height();
    unsigned    deviceRB = fDevice.rowBytes() - (width << 1);
    unsigned    maskRB = mask.fRowBytes - width;
    uint32_t    color32 = fExpandedRaw16;

    unsigned scale256 = fScale;
    do {
        int w = width;
        do {
            unsigned aa = *alpha++;
            unsigned scale = SkAlpha255To256(aa) * scale256 >> (8 + 3);
            uint32_t src32 = color32 * scale;
            uint32_t dst32 = SkExpand_rgb_16(*device) * (32 - scale);
            *device++ = SkCompact_rgb_16((src32 + dst32) >> 5);
        } while (--w != 0);
        device = (uint16_t*)((char*)device + deviceRB);
        alpha += maskRB;
    } while (--height != 0);
}

void SkRGB16_Blitter::blitV(int x, int y, int height, SkAlpha alpha) {
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    unsigned    deviceRB = fDevice.rowBytes();

    // TODO: respect fDoDither
    unsigned scale5 = SkAlpha255To256(alpha) * fScale >> (8 + 3);
    uint32_t src32 =  fExpandedRaw16 * scale5;
    scale5 = 32 - scale5;
    do {
        uint32_t dst32 = SkExpand_rgb_16(*device) * scale5;
        *device = SkCompact_rgb_16((src32 + dst32) >> 5);
        device = (uint16_t*)((char*)device + deviceRB);
    } while (--height != 0);
}

#ifndef __arm__
void SkRGB16_Blitter::blitRect(int x, int y, int width, int height) {
    SkASSERT(x + width <= fDevice.width() && y + height <= fDevice.height());
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    unsigned    deviceRB = fDevice.rowBytes();
    SkPMColor src32 = fSrcColor32;

    while (--height >= 0) {
        blend32_16_row(src32, device, width);
        device = (uint16_t*)((char*)device + deviceRB);
    }
}
#else
void SkRGB16_Blitter::blitRect(int x, int y, int width, int height) {
    SkASSERT(x + width <= fDevice.width() && y + height <= fDevice.height());
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    unsigned    deviceRB = fDevice.rowBytes();
    SkPMColor src32 = fSrcColor32;
    uint32_t src_expand = pmcolor_to_expand16(src32);
    unsigned scale = SkAlpha255To256(0xFF - SkGetPackedA32(src32)) >> 3;

#ifdef SK_USE_NEON
       asm volatile(
            "cmp        %[height], #0                       \n\t"  //height > 0?
            "ble        7f                                  \n\t"

            "cmp        %[width], #0                        \n\t"  //width > 0?
            "ble        7f                                  \n\t"

            "pld        [r7, #0]                            \n\t"
            //r9=mask
            "movw       r9, #0xf81f                         \n\t"
            "movt       r9, #0x07e0                         \n\t"

            "vdup.32    q7, r9                              \n\t"  //q7 = mask_vec
            "vdup.32    q6, %[src_expand]                   \n\t"
            "vdup.32    q8, %[scale]                        \n\t"

            "1:                                             \n\t"
            "movs       r4, %[width], lsr #4                \n\t"//r4 = count / 16
            "mov        r7, %[device]                       \n\t"//r7 = device
            "beq        3f                                  \n\t" 

            "2:                                             \n\t" 
            "vld1.u16   {d0,d1,d2,d3}, [r7]                 \n\t"
            "pld        [r7, #128]                          \n\t"
            "subs       r4, r4, #1                          \n\t"
            "vand.32    q2, q0, q7                          \n\t"
            "vand.32    q4, q1, q7                          \n\t"
            "vrev32.16  q3, q0                              \n\t"
            "vrev32.16  q5, q1                              \n\t"
            "vand.32    q3, q3, q7                          \n\t"
            "vand.32    q5, q5, q7                          \n\t"
            "vmul.i32   q2, q2, q8                          \n\t"
            "vmul.i32   q4, q4, q8                          \n\t"
            "vmul.i32   q3, q3, q8                          \n\t"
            "vmul.i32   q5, q5, q8                          \n\t"
            "vadd.u32   q2, q2, q6                          \n\t"
            "vadd.u32   q4, q4, q6                          \n\t"
            "vadd.u32   q3, q3, q6                          \n\t"
            "vadd.u32   q5, q5, q6                          \n\t"
            "vshr.u32   q2, q2, #5                          \n\t"
            "vshr.u32   q4, q4, #5                          \n\t"
            "vshr.u32   q3, q3, #5                          \n\t"
            "vshr.u32   q5, q5, #5                          \n\t"

            "vand.u32   q2, q2, q7                          \n\t"
            "vand.u32   q4, q4, q7                          \n\t"
            "vand.u32   q3, q3, q7                          \n\t"
            "vand.u32   q5, q5, q7                          \n\t"
            "vrev32.16  q3, q3                              \n\t"
            "vrev32.16  q5, q5                              \n\t"
            "vorr.u16   q0, q2, q3                          \n\t"
            "vorr.u16   q1, q4, q5                          \n\t"
            "vst1.u16   {d0, d1, d2, d3}, [r7]!             \n\t"//device
            "bne        2b                                  \n\t"
            
            "3:                                             \n\t"
            "ands       r4, %[width], #0xF                  \n\t"
            "beq        6f                                  \n\t"
            "cmp        r4, #8                              \n\t"
            "blt        4f                                  \n\t"

            "vld1.u16   {d0,d1}, [r7]                       \n\t"
            "vand.32    q2, q0, q7                          \n\t"
            "vrev32.16  q3, q0                              \n\t"
            "vand.32    q3, q3, q7                          \n\t"

            "vmul.i32   q2, q2, q8                          \n\t"
            "vmul.i32   q3, q3, q8                          \n\t"
            "vadd.u32   q2, q2, q6                          \n\t"
            "vadd.u32   q3, q3, q6                          \n\t"
            "vshr.u32   q2, q2, #5                          \n\t"
            "vshr.u32   q3, q3, #5                          \n\t"

            "vand.u32   q2, q2, q7                          \n\t"
            "vand.u32   q3, q3, q7                          \n\t"
            "vrev32.16  q3, q3                              \n\t"
            "vorr.u16   q0, q2, q3                          \n\t"
            "vst1.u16   {d0, d1}, [r7]!                     \n\t"//device

            "4:                                             \n\t"
            "ands       r4, %[width], #0x7                  \n\t"
            "beq        6f                                  \n\t"

            "5:                                             \n\t"
            "ldrh       r10, [r7, #0]                       \n\t" //r10 = current = *device
            "subs       r4, r4, #1                          \n\t" //width--;
            "pld        [r7, #8]                            \n\t"
            "orr        r8, r10, r10, lsl #16               \n\t" //(current << 16 | current)
            "and        r10, r8, r9                         \n\t" //r10 = &mask
            "mla        r8, r10, %[scale], %[src_expand]    \n\t" //r8 = src_expand + dst_expand
            "and        r8, r9, r8, lsr #5                  \n\t" //r8 & mask
            "orr        r10, r8, r8, lsr #16                \n\t" //must trunk the high 16bits, if not, generate error result
            "strh       r10, [r7], #2                       \n\t"
            "bne        5b                                  \n\t"
            
            "6:                                             \n\t"
            "subs       %[height], %[height], #1            \n\t"
            "addne      %[device], %[device], %[deviceRB]   \n\t"
            "bne        1b                                  \n\t"

            "7:                                             \n\t"
            :[device] "+r" (device), [height] "+r" (height)
            :[width] "r" (width), [deviceRB] "r" (deviceRB), [src_expand] "r" (src_expand), [scale] "r" (scale)
            :"memory", "r4", "r7", "r8", "r9", "r10", "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10", "d11", "d12", "d13", "d14", "d15", "d16", "d17"
            );
             
#else
    //r7 init device
    //r9 mask
    //r10 r8 lr
    asm volatile(
            "pld        [%[device]]                         \n\t"
            "mov        r9, #0x7E0                          \n\t"
            "mov        r10, #0xFF                          \n\t"
            "orr        r10, r10, r10, lsl #8               \n\t" //r10 = 0xFFFF
            "bic        r10, #0x7E0                         \n\t"
            "orr        r9, r10, r9, lsl #16                \n\t"  //r9 = mask

            "cmp        %[height], #0                       \n\t"  //height > 0?
            "ble        3f                                  \n\t"

            "1:                                             \n\t" //outer loop
            "mov        lr, %[width]                        \n\t" //lr = width
            "mov        r7, %[device]                       \n\t" //r7 = init device
            "2:                                             \n\t" //inner loop
            "ldrh       r10, [r7, #0]                       \n\t"  //r10 = current = *device
            "subs       lr, lr, #1                          \n\t"  //width--;
            "pld        [r7, #8]                            \n\t"
            "orr        r8, r10, r10, lsl #16               \n\t" //(current << 16 | current)
            "and        r10, r8, r9                         \n\t" //r10 = &mask
            "mla        r8, r10, %[scale], %[src_expand]    \n\t"  //r8 = src_expand + dst_expand
            "and        r8, r9, r8, lsr #5                  \n\t" //r8 & mask
            "orr        r10, r8, r8, lsr #16                \n\t" //must trunk the high 16bits, if not, generate error result
            "strh       r10, [r7], #2                       \n\t"
            "bne        2b                                  \n\t"

            "subs       %[height], %[height], #1            \n\t"
            "addne      %[device], %[device], %[deviceRB]   \n\t"
            "bne         1b                                 \n\t"

            "3:                                             \n\t"
            :[device] "+r" (device), [height] "+r" (height)
            :[width] "r" (width), [deviceRB] "r" (deviceRB), [src_expand] "r" (src_expand), [scale] "r" (scale)
            :"memory", "r7", "r8", "r9", "r10", "lr"
            );
#endif
}
#endif

///////////////////////////////////////////////////////////////////////////////

SkRGB16_Shader16_Blitter::SkRGB16_Shader16_Blitter(const SkBitmap& device,
                                                   const SkPaint& paint)
    : SkRGB16_Shader_Blitter(device, paint) {
    SkASSERT(SkShader::CanCallShadeSpan16(fShaderFlags));
}

void SkRGB16_Shader16_Blitter::blitH(int x, int y, int width) {
    SkASSERT(x + width <= fDevice.width());

    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);
    SkShader*   shader = fShader;

    int alpha = shader->getSpan16Alpha();
    if (0xFF == alpha) {
        shader->shadeSpan16(x, y, device, width);
    } else {
        uint16_t* span16 = (uint16_t*)fBuffer;
        shader->shadeSpan16(x, y, span16, width);
        SkBlendRGB16(span16, device, SkAlpha255To256(alpha), width);
    }
}

void SkRGB16_Shader16_Blitter::blitRect(int x, int y, int width, int height) {
    SkShader*   shader = fShader;
    uint16_t*   dst = fDevice.getAddr16(x, y);
    size_t      dstRB = fDevice.rowBytes();
    int         alpha = shader->getSpan16Alpha();

    if (0xFF == alpha) {
        if (fShaderFlags & SkShader::kConstInY16_Flag) {
            // have the shader blit directly into the device the first time
            shader->shadeSpan16(x, y, dst, width);
            // and now just memcpy that line on the subsequent lines
            if (--height > 0) {
                const uint16_t* orig = dst;
                do {
                    dst = (uint16_t*)((char*)dst + dstRB);
                    memcpy(dst, orig, width << 1);
                } while (--height);
            }
        } else {    // need to call shadeSpan16 for every line
            do {
                shader->shadeSpan16(x, y, dst, width);
                y += 1;
                dst = (uint16_t*)((char*)dst + dstRB);
            } while (--height);
        }
    } else {
        int scale = SkAlpha255To256(alpha);
        uint16_t* span16 = (uint16_t*)fBuffer;
        if (fShaderFlags & SkShader::kConstInY16_Flag) {
            shader->shadeSpan16(x, y, span16, width);
            do {
                SkBlendRGB16(span16, dst, scale, width);
                dst = (uint16_t*)((char*)dst + dstRB);
            } while (--height);
        } else {
            do {
                shader->shadeSpan16(x, y, span16, width);
                SkBlendRGB16(span16, dst, scale, width);
                y += 1;
                dst = (uint16_t*)((char*)dst + dstRB);
            } while (--height);
        }
    }
}

void SkRGB16_Shader16_Blitter::blitAntiH(int x, int y,
                                         const SkAlpha* SK_RESTRICT antialias,
                                         const int16_t* SK_RESTRICT runs) {
    SkShader*   shader = fShader;
    SkPMColor* SK_RESTRICT span = fBuffer;
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);

    int alpha = shader->getSpan16Alpha();
    uint16_t* span16 = (uint16_t*)span;

    if (0xFF == alpha) {
        for (;;) {
            int count = *runs;
            if (count <= 0) {
                break;
            }
            SkASSERT(count <= fDevice.width()); // don't overrun fBuffer

            int aa = *antialias;
            if (aa == 255) {
                // go direct to the device!
                shader->shadeSpan16(x, y, device, count);
            } else if (aa) {
                shader->shadeSpan16(x, y, span16, count);
                SkBlendRGB16(span16, device, SkAlpha255To256(aa), count);
            }
            device += count;
            runs += count;
            antialias += count;
            x += count;
        }
    } else {  // span alpha is < 255
        alpha = SkAlpha255To256(alpha);
        for (;;) {
            int count = *runs;
            if (count <= 0) {
                break;
            }
            SkASSERT(count <= fDevice.width()); // don't overrun fBuffer

            int aa = SkAlphaMul(*antialias, alpha);
            if (aa) {
                shader->shadeSpan16(x, y, span16, count);
                SkBlendRGB16(span16, device, SkAlpha255To256(aa), count);
            }

            device += count;
            runs += count;
            antialias += count;
            x += count;
        }
    }
}

///////////////////////////////////////////////////////////////////////////////

SkRGB16_Shader_Blitter::SkRGB16_Shader_Blitter(const SkBitmap& device,
                                               const SkPaint& paint)
: INHERITED(device, paint) {
    SkASSERT(paint.getXfermode() == NULL);

    fBuffer = (SkPMColor*)sk_malloc_throw(device.width() * sizeof(SkPMColor));

    // compute SkBlitRow::Procs
    unsigned flags = 0;
    
    uint32_t shaderFlags = fShaderFlags;
    // shaders take care of global alpha, so we never set it in SkBlitRow
    if (!(shaderFlags & SkShader::kOpaqueAlpha_Flag)) {
        flags |= SkBlitRow::kSrcPixelAlpha_Flag;
        }
    // don't dither if the shader is really 16bit
    if (paint.isDither() && !(shaderFlags & SkShader::kIntrinsicly16_Flag)) {
        flags |= SkBlitRow::kDither_Flag;
    }
    // used when we know our global alpha is 0xFF
    fOpaqueProc = SkBlitRow::Factory(flags, SkBitmap::kRGB_565_Config);
    // used when we know our global alpha is < 0xFF
    fAlphaProc  = SkBlitRow::Factory(flags | SkBlitRow::kGlobalAlpha_Flag,
                                     SkBitmap::kRGB_565_Config);
}

SkRGB16_Shader_Blitter::~SkRGB16_Shader_Blitter() {
    sk_free(fBuffer);
}

void SkRGB16_Shader_Blitter::blitH(int x, int y, int width) {
    SkASSERT(x + width <= fDevice.width());

    fShader->shadeSpan(x, y, fBuffer, width);
    // shaders take care of global alpha, so we pass 0xFF (should be ignored)
    fOpaqueProc(fDevice.getAddr16(x, y), fBuffer, width, 0xFF, x, y);
}

void SkRGB16_Shader_Blitter::blitRect(int x, int y, int width, int height) {
    SkShader*       shader = fShader;
    SkBlitRow::Proc proc = fOpaqueProc;
    SkPMColor*      buffer = fBuffer;
    uint16_t*       dst = fDevice.getAddr16(x, y);
    size_t          dstRB = fDevice.rowBytes();

    if (fShaderFlags & SkShader::kConstInY32_Flag) {
        shader->shadeSpan(x, y, buffer, width);
        do {
            proc(dst, buffer, width, 0xFF, x, y);
            y += 1;
            dst = (uint16_t*)((char*)dst + dstRB);
        } while (--height);
    } else {
        do {
            shader->shadeSpan(x, y, buffer, width);
            proc(dst, buffer, width, 0xFF, x, y);
            y += 1;
            dst = (uint16_t*)((char*)dst + dstRB);
        } while (--height);
    }
}

static inline int count_nonzero_span(const int16_t runs[], const SkAlpha aa[]) {
    int count = 0;
    for (;;) {
        int n = *runs;
        if (n == 0 || *aa == 0) {
            break;
        }
        runs += n;
        aa += n;
        count += n;
    }
    return count;
}

void SkRGB16_Shader_Blitter::blitAntiH(int x, int y,
                                       const SkAlpha* SK_RESTRICT antialias,
                                       const int16_t* SK_RESTRICT runs) {
    SkShader*   shader = fShader;
    SkPMColor* SK_RESTRICT span = fBuffer;
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);

    for (;;) {
        int count = *runs;
        if (count <= 0) {
            break;
        }
        int aa = *antialias;
        if (0 == aa) {
            device += count;
            runs += count;
            antialias += count;
            x += count;
            continue;
        }

        int nonZeroCount = count + count_nonzero_span(runs + count, antialias + count);

        SkASSERT(nonZeroCount <= fDevice.width()); // don't overrun fBuffer
        shader->shadeSpan(x, y, span, nonZeroCount);

        SkPMColor* localSpan = span;
        for (;;) {
            SkBlitRow::Proc proc = (aa == 0xFF) ? fOpaqueProc : fAlphaProc;
            proc(device, localSpan, count, aa, x, y);

            x += count;
            device += count;
            runs += count;
            antialias += count;
            nonZeroCount -= count;
            if (nonZeroCount == 0) {
                break;
            }
            localSpan += count;
            SkASSERT(nonZeroCount > 0);
            count = *runs;
            SkASSERT(count > 0);
            aa = *antialias;
        }
    }
}

///////////////////////////////////////////////////////////////////////

SkRGB16_Shader_Xfermode_Blitter::SkRGB16_Shader_Xfermode_Blitter(
                                const SkBitmap& device, const SkPaint& paint)
: INHERITED(device, paint) {
    fXfermode = paint.getXfermode();
    SkASSERT(fXfermode);
    fXfermode->ref();

    int width = device.width();
    fBuffer = (SkPMColor*)sk_malloc_throw((width + (SkAlign4(width) >> 2)) * sizeof(SkPMColor));
    fAAExpand = (uint8_t*)(fBuffer + width);
}

SkRGB16_Shader_Xfermode_Blitter::~SkRGB16_Shader_Xfermode_Blitter() {
    fXfermode->unref();
    sk_free(fBuffer);
}

void SkRGB16_Shader_Xfermode_Blitter::blitH(int x, int y, int width) {
    SkASSERT(x + width <= fDevice.width());

    uint16_t*   device = fDevice.getAddr16(x, y);
    SkPMColor*  span = fBuffer;

    fShader->shadeSpan(x, y, span, width);
    fXfermode->xfer16(device, span, width, NULL);
}

void SkRGB16_Shader_Xfermode_Blitter::blitAntiH(int x, int y,
                                const SkAlpha* SK_RESTRICT antialias,
                                const int16_t* SK_RESTRICT runs) {
    SkShader*   shader = fShader;
    SkXfermode* mode = fXfermode;
    SkPMColor* SK_RESTRICT span = fBuffer;
    uint8_t* SK_RESTRICT aaExpand = fAAExpand;
    uint16_t* SK_RESTRICT device = fDevice.getAddr16(x, y);

    for (;;) {
        int count = *runs;
        if (count <= 0) {
            break;
        }
        int aa = *antialias;
        if (0 == aa) {
            device += count;
            runs += count;
            antialias += count;
            x += count;
            continue;
        }

        int nonZeroCount = count + count_nonzero_span(runs + count,
                                                      antialias + count);

        SkASSERT(nonZeroCount <= fDevice.width()); // don't overrun fBuffer
        shader->shadeSpan(x, y, span, nonZeroCount);

        x += nonZeroCount;
        SkPMColor* localSpan = span;
        for (;;) {
            if (aa == 0xFF) {
                mode->xfer16(device, localSpan, count, NULL);
            } else {
                SkASSERT(aa);
                memset(aaExpand, aa, count);
                mode->xfer16(device, localSpan, count, aaExpand);
            }
            device += count;
            runs += count;
            antialias += count;
            nonZeroCount -= count;
            if (nonZeroCount == 0) {
                break;
            }
            localSpan += count;
            SkASSERT(nonZeroCount > 0);
            count = *runs;
            SkASSERT(count > 0);
            aa = *antialias;
        }
    } 
}

///////////////////////////////////////////////////////////////////////////////

SkBlitter* SkBlitter_ChooseD565(const SkBitmap& device, const SkPaint& paint,
                                void* storage, size_t storageSize) {
    SkBlitter* blitter;
    SkShader* shader = paint.getShader();
    SkXfermode* mode = paint.getXfermode();

    // we require a shader if there is an xfermode, handled by our caller
    SkASSERT(NULL == mode || NULL != shader);

    if (shader) {
        if (mode) {
            SK_PLACEMENT_NEW_ARGS(blitter, SkRGB16_Shader_Xfermode_Blitter,
                                  storage, storageSize, (device, paint));
        } else if (shader->canCallShadeSpan16()) {
            SK_PLACEMENT_NEW_ARGS(blitter, SkRGB16_Shader16_Blitter,
                                  storage, storageSize, (device, paint));
        } else {
            SK_PLACEMENT_NEW_ARGS(blitter, SkRGB16_Shader_Blitter,
                                  storage, storageSize, (device, paint));
        }
    } else {
        // no shader, no xfermode, (and we always ignore colorfilter)
        SkColor color = paint.getColor();
        if (0 == SkColorGetA(color)) {
            SK_PLACEMENT_NEW(blitter, SkNullBlitter, storage, storageSize);
#ifdef USE_BLACK_BLITTER
        } else if (SK_ColorBLACK == color) {
            SK_PLACEMENT_NEW_ARGS(blitter, SkRGB16_Black_Blitter, storage,
                                  storageSize, (device, paint));
#endif
        } else if (0xFF == SkColorGetA(color)) {
            SK_PLACEMENT_NEW_ARGS(blitter, SkRGB16_Opaque_Blitter, storage,
                                  storageSize, (device, paint));
        } else {
            SK_PLACEMENT_NEW_ARGS(blitter, SkRGB16_Blitter, storage,
                                  storageSize, (device, paint));
        }
    }
    
    return blitter;
}
