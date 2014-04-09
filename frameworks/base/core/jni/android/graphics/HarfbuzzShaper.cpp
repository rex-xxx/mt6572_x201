/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#define LOG_TAG "TextLayoutCache"

#include "HarfbuzzShaper.h"

#include <unicode/normlzr.h>
#include <unicode/uchar.h>

#include "SkFontHost.h"

#include "SkPath.h"

static inline float HBFixedToFloat(HB_Fixed v) {
    // Harfbuzz uses 26.6 fixed point values for pixel offsets
    return v * (1.0f / 64);
}

static inline HB_Fixed SkScalarToHBFixed(SkScalar value) {
    // HB_Fixed is a 26.6 fixed point format.
    return SkScalarToFloat(value) * 64.0f;
}

static HB_Bool stringToGlyphs(HB_Font hbFont, const HB_UChar16* characters, hb_uint32 length,
        HB_Glyph* glyphs, hb_uint32* glyphsSize, HB_Bool isRTL)
{
    SkPaint* paint = static_cast<SkPaint*>(hbFont->userData);
    paint->setTextEncoding(SkPaint::kUTF16_TextEncoding);

    uint16_t* skiaGlyphs = reinterpret_cast<uint16_t*>(glyphs);
    int numGlyphs = paint->textToGlyphs(characters, length * sizeof(uint16_t), skiaGlyphs);

    // HB_Glyph is 32-bit, but Skia outputs only 16-bit numbers. So our
    // |glyphs| array needs to be converted.
    for (int i = numGlyphs - 1; i >= 0; --i) {
        glyphs[i] = skiaGlyphs[i];
    }

    *glyphsSize = numGlyphs;
    return 1;
}

static void glyphsToAdvances(HB_Font hbFont, const HB_Glyph* glyphs, hb_uint32 numGlyphs,
        HB_Fixed* advances, int flags)
{
    SkPaint* paint = static_cast<SkPaint*>(hbFont->userData);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t* glyphs16 = new uint16_t[numGlyphs];
    if (!glyphs16)
        return;
    for (unsigned i = 0; i < numGlyphs; ++i)
        glyphs16[i] = glyphs[i];
    SkScalar* scalarAdvances = reinterpret_cast<SkScalar*>(advances);
    paint->getTextWidths(glyphs16, numGlyphs * sizeof(uint16_t), scalarAdvances);

    // The |advances| values which Skia outputs are SkScalars, which are floats
    // in Chromium. However, Harfbuzz wants them in 26.6 fixed point format.
    // These two formats are both 32-bits long.
    for (unsigned i = 0; i < numGlyphs; ++i) {
        advances[i] = SkScalarToHBFixed(scalarAdvances[i]);
#if DEBUG_CALLBACK
        ALOGD("glyphsToAdvances -- advances[%d]=%d", i, advances[i]);
#endif
    }
    delete glyphs16;
}

static HB_Bool canRender(HB_Font hbFont, const HB_UChar16* characters, hb_uint32 length)
{
    SkPaint* paint = static_cast<SkPaint*>(hbFont->userData);
    paint->setTextEncoding(SkPaint::kUTF16_TextEncoding);

    uint16_t* glyphs16 = new uint16_t[length];
    int numGlyphs = paint->textToGlyphs(characters, length * sizeof(uint16_t), glyphs16);

    bool result = true;
    for (int i = 0; i < numGlyphs; ++i) {
        if (!glyphs16[i]) {
            result = false;
            break;
        }
    }
    delete glyphs16;
    return result;
}

static HB_Error getOutlinePoint(HB_Font hbFont, HB_Glyph glyph, int flags, hb_uint32 point,
        HB_Fixed* xPos, HB_Fixed* yPos, hb_uint32* resultingNumPoints)
{
    if (flags & HB_ShaperFlag_UseDesignMetrics)
        // This is requesting pre-hinted positions. We can't support this.
        return HB_Err_Invalid_Argument;

    SkPaint* paint = static_cast<SkPaint*>(hbFont->userData);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t glyph16 = glyph;
    SkPath path;
    paint->getTextPath(&glyph16, sizeof(glyph16), 0, 0, &path);
    uint32_t numPoints = path.getPoints(0, 0);
    if (point >= numPoints)
        return HB_Err_Invalid_SubTable;
    SkPoint* points = static_cast<SkPoint*>(malloc(sizeof(SkPoint) * (point + 1)));
    if (!points)
        return HB_Err_Invalid_SubTable;
    // Skia does let us get a single point from the path.
    path.getPoints(points, point + 1);
    *xPos = SkScalarToHBFixed(points[point].fX);
    *yPos = SkScalarToHBFixed(points[point].fY);
    *resultingNumPoints = numPoints;
    delete points;

    return HB_Err_Ok;
}

static void getGlyphMetrics(HB_Font hbFont, HB_Glyph glyph, HB_GlyphMetrics* metrics)
{
    SkPaint* paint = static_cast<SkPaint*>(hbFont->userData);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t glyph16 = glyph;
    SkScalar width;
    SkRect bounds;
    paint->getTextWidths(&glyph16, sizeof(glyph16), &width, &bounds);

    metrics->x = SkScalarToHBFixed(bounds.fLeft);
    metrics->y = SkScalarToHBFixed(bounds.fTop);
    metrics->width = SkScalarToHBFixed(bounds.width());
    metrics->height = SkScalarToHBFixed(bounds.height());

    metrics->xOffset = SkScalarToHBFixed(width);
    // We can't actually get the |y| correct because Skia doesn't export
    // the vertical advance. However, nor we do ever render vertical text at
    // the moment so it's unimportant.
    metrics->yOffset = 0;
}

static HB_Fixed getFontMetric(HB_Font hbFont, HB_FontMetric metric)
{
    SkPaint* paint = static_cast<SkPaint*>(hbFont->userData);

    SkPaint::FontMetrics skiaMetrics;
    paint->getFontMetrics(&skiaMetrics);

    switch (metric) {
    case HB_FontAscent:
        return SkScalarToHBFixed(-skiaMetrics.fAscent);
    // We don't support getting the rest of the metrics and Harfbuzz doesn't seem to need them.
    default:
        return 0;
    }
    return 0;
}

const HB_FontClass harfbuzzSkiaClass = {
    stringToGlyphs,
    glyphsToAdvances,
    canRender,
    getOutlinePoint,
    getGlyphMetrics,
    getFontMetric,
};

HB_Error harfbuzzSkiaGetTable(void* font, const HB_Tag tag, HB_Byte* buffer, HB_UInt* len)
{
    SkTypeface* typeface = static_cast<SkTypeface*>(font);

    if (!typeface) {
        ALOGD("Typeface cannot be null");
        return HB_Err_Invalid_Argument;
    }
    const size_t tableSize = SkFontHost::GetTableSize(typeface->uniqueID(), tag);
    if (!tableSize)
        return HB_Err_Invalid_Argument;
    // If Harfbuzz specified a NULL buffer then it's asking for the size of the table.
    if (!buffer) {
        *len = tableSize;
        return HB_Err_Ok;
    }

    if (*len < tableSize)
        return HB_Err_Invalid_Argument;
    SkFontHost::GetTableData(typeface->uniqueID(), tag, 0, tableSize, buffer);
    return HB_Err_Ok;
}

namespace android {

//--------------------------------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(HarfbuzzShaper);

//--------------------------------------------------------------------------------------------------

HarfbuzzShaper::HarfbuzzShaper() : mShaperItemGlyphArraySize(0) {
    mFontRec.klass = &harfbuzzSkiaClass;
    mFontRec.userData = 0;

    // Note that the scaling values (x_ and y_ppem, x_ and y_scale) will be set
    // below, when the paint transform and em unit of the actual shaping font
    // are known.

    memset(&mShaperItem, 0, sizeof(mShaperItem));

    mShaperItem.font = &mFontRec;
}

HarfbuzzShaper::~HarfbuzzShaper() {
    purgeCaches();
    deleteShaperItemGlyphArrays();
}

void HarfbuzzShaper::purgeCaches() {
    size_t cacheSize = mCachedHBFaces.size();
    for (size_t i = 0; i < cacheSize; i++) {
        HB_FreeFace(mCachedHBFaces.valueAt(i));
    }
    mCachedHBFaces.clear();
}

static void logGlyphs(HB_ShaperItem shaperItem) {
    ALOGD("         -- glyphs count=%d", shaperItem.num_glyphs);
    for (size_t i = 0; i < shaperItem.num_glyphs; i++) {
        ALOGD("         -- glyph[%d] = %d, offset.x = %0.2f, offset.y = %0.2f", i,
                shaperItem.glyphs[i],
                HBFixedToFloat(shaperItem.offsets[i].x),
                HBFixedToFloat(shaperItem.offsets[i].y));
    }
}

void HarfbuzzShaper::initFaceAndFont(const SkPaint* paint) {
    mShaperItem.font->userData = const_cast<SkPaint *>(paint);

    SkTypeface *typeface = paint->getTypeface();
    mShaperItem.face = getCachedHBFace(typeface);

    unsigned int x_ppem, y_ppem;
    int x_scale, y_scale;
    getConversionFactor(x_ppem, y_ppem, x_scale, y_scale, paint);
    mFontRec.x_ppem = x_ppem;
    mFontRec.y_ppem = y_ppem;
    mFontRec.x_scale = x_scale;
    mFontRec.y_scale = y_scale;
}

void HarfbuzzShaper::initShapingItem(const UChar *chars, size_t count, bool isRTL) {
    // Initialize the string of shaper item.
    mShaperItem.string = chars;
    mShaperItem.stringLength = count;

    mShaperItem.item.pos = 0;
    mShaperItem.item.length = count;
    mShaperItem.item.bidiLevel = isRTL;
    mShaperItem.item.script = mShapingScript;

    mShaperItem.kerning_applied = false;
}

jfloat HarfbuzzShaper::shapeScriptRun (const SkPaint* paint, const UChar* chars,
        size_t count, bool isRTL, Vector<jfloat>* const outAdvances,
        Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos,
        jfloat startXPosition, uint32_t startScriptRun, size_t glyphBaseCount) {
#if DEBUG_GLYPHS
    ALOGD("Shape Scrit Run with Harfbuzz");
#endif
    // Normalize the script run.
    const UChar* normalizedChars = normalizeString(chars, count, isRTL);

    initShapingItem(normalizedChars, count, isRTL);

    initFaceAndFont(paint);

    // Shape
    assert(mShaperItem.item.length > 0); // Harfbuzz will overwrite other memory if length is 0.
    size_t size = mShaperItem.item.length * 2;
    while (!doShaping(size)) {
        // We overflowed our glyph arrays. Resize and retry.
        // HB_ShapeItem fills in shaperItem.num_glyphs with the needed size.
        size = mShaperItem.num_glyphs * 2;
    }

#if DEBUG_GLYPHS
    ALOGD("Got from Harfbuzz");
    ALOGD("         -- glyphBaseCount = %d", glyphBaseCount);
    ALOGD("         -- num_glypth = %d", mShaperItem.num_glyphs);
    ALOGD("         -- kerning_applied = %d", mShaperItem.kerning_applied);
    ALOGD("         -- isDevKernText = %d", paint->isDevKernText());
    ALOGD("         -- startXPosition = %f", startXPosition);

    logGlyphs(mShaperItem);
#endif

    if (mShaperItem.advances == NULL || mShaperItem.num_glyphs == 0) {
#if DEBUG_GLYPHS
        ALOGD("Advances array is empty or num_glypth = 0");
#endif
        return 0.0f;
    }

#if DEBUG_GLYPHS
    ALOGD("Returned logclusters");
    for (size_t i = 0; i < mShaperItem.num_glyphs; i++) {
        ALOGD("         -- lc[%d] = %d, hb-adv[%d] = %0.2f", i, mShaperItem.log_clusters[i],
                i, HBFixedToFloat(mShaperItem.advances[i]));
    }
#endif

    jfloat totalFontRunAdvance = 0;
    size_t clusterStart = 0;
    for (size_t i = 0; i < count; i++) {
        size_t cluster = mShaperItem.log_clusters[i];
        size_t clusterNext = i == count - 1 ? mShaperItem.num_glyphs :
                mShaperItem.log_clusters[i + 1];
        if (cluster != clusterNext) {
            jfloat advance = 0;
            // The advance for the cluster is the sum of the advances of all glyphs within
            // the cluster.
            for (size_t j = cluster; j < clusterNext; j++) {
                advance += HBFixedToFloat(mShaperItem.advances[j]);
            }
            totalFontRunAdvance += advance;
            outAdvances->replaceAt(advance, startScriptRun + clusterStart);
            clusterStart = i + 1;
        }
    }

#if DEBUG_ADVANCES
    ALOGD("Returned advances");
    for (size_t i = 0; i < count; i++) {
        ALOGD("         -- hb-adv[%d] = %0.2f, log_clusters = %d, total = %0.2f", i,
                (*outAdvances)[startScriptRun + i], mShaperItem.log_clusters[i], totalFontRunAdvance);
    }
#endif

    // Get Glyphs and reverse them in place if RTL
    if (outGlyphs) {
        size_t countGlyphs = mShaperItem.num_glyphs;
#if DEBUG_GLYPHS
        ALOGD("Returned script run glyphs -- count = %d", countGlyphs);
#endif
        for (size_t i = 0; i < countGlyphs; i++) {
            jchar glyph = glyphBaseCount +
                    (jchar) mShaperItem.glyphs[(!isRTL) ? i : countGlyphs - 1 - i];
#if DEBUG_GLYPHS
            ALOGD("         -- glyph[%d] = %d", i, glyph);
#endif
            outGlyphs->add(glyph);
        }
    }

    // Get glyph positions (and reverse them in place if RTL)
    float skewX = paint->getTextSkewX();
    if (outPos) {
        size_t countGlyphs = mShaperItem.num_glyphs;
        jfloat x = startXPosition;
        for (size_t i = 0; i < countGlyphs; i++) {
            size_t index = (!isRTL) ? i : countGlyphs - 1 - i;
            float xo = HBFixedToFloat(mShaperItem.offsets[index].x);
            float yo = HBFixedToFloat(mShaperItem.offsets[index].y);
            // Apply skewX component of transform to position offsets. Note
            // that scale has already been applied through x_ and y_scale
            // set in the mFontRec.
            outPos->add(x + xo + yo * skewX);
            outPos->add(yo);
#if DEBUG_GLYPHS
            ALOGD("         -- hb adv[%d] = %f, log_cluster[%d] = %d, x_position = %f", index,
                    HBFixedToFloat(mShaperItem.advances[index]), index,
                    mShaperItem.log_clusters[index], x + xo + yo * skewX);
#endif
            x += HBFixedToFloat(mShaperItem.advances[index]);
        }
    }

    return totalFontRunAdvance;
}

const UChar *HarfbuzzShaper::normalizeString(const UChar *chars, size_t count, bool isRTL) {
    UErrorCode error = U_ZERO_ERROR;
    bool useNormalizedString = false;
    for (ssize_t i = count - 1; i >= 0; --i) {
        UChar ch1 = chars[i];
        if (::ublock_getCode(ch1) == UBLOCK_COMBINING_DIACRITICAL_MARKS) {
            // So we have found a diacritic, let's get now the main code point which is paired
            // with it. As we can have several diacritics in a row, we need to iterate back again
#if DEBUG_GLYPHS
            ALOGD("The BiDi run '%s' is containing a Diacritic at position %d",
                    String8(chars, count).string(), int(i));
#endif
            ssize_t j = i - 1;
            for (; j >= 0;  --j) {
                UChar ch2 = chars[j];
                if (::ublock_getCode(ch2) != UBLOCK_COMBINING_DIACRITICAL_MARKS) {
                    break;
                }
            }

            // We could not found the main code point, so we will just use the initial chars
            if (j < 0) {
                break;
            }

#if DEBUG_GLYPHS
            ALOGD("Found main code point at index %d", int(j));
#endif
            // We found the main code point, so we can normalize the "chunk" and fill
            // the remaining with ZWSP so that the Paint.getTextWidth() APIs will still be able
            // to get one advance per char
            mBuffer.remove();
            Normalizer::normalize(UnicodeString(chars + j, i - j + 1),
                    UNORM_NFC, 0 /* no options */, mBuffer, error);
            if (U_SUCCESS(error)) {
                if (!useNormalizedString) {
                    useNormalizedString = true;
                    mNormalizedString.setTo(false /* not terminated*/, chars, count);
                }
                // Set the normalized chars
                for (ssize_t k = j; k < j + mBuffer.length(); ++k) {
                    mNormalizedString.setCharAt(k, mBuffer.charAt(k - j));
                }
                // Fill the remain part with ZWSP (ZWNJ and ZWJ would lead to weird results
                // because some fonts are missing those glyphs)
                for (ssize_t k = j + mBuffer.length(); k <= i; ++k) {
                    mNormalizedString.setCharAt(k, UNICODE_ZWSP);
                }
            }
            i = j - 1;
        }
    }

    // Reverse "BiDi mirrored chars" in RTL mode only
    // See: http://www.unicode.org/Public/6.0.0/ucd/extracted/DerivedBinaryProperties.txt
    // This is a workaround because Harfbuzz is not able to do mirroring in all cases and
    // script-run splitting with Harfbuzz is splitting on parenthesis
    if (isRTL) {
        for (ssize_t i = 0; i < ssize_t(count); i++) {
            UChar32 ch = chars[i];
            if (!u_isMirrored(ch)) continue;
            if (!useNormalizedString) {
                useNormalizedString = true;
                mNormalizedString.setTo(false /* not terminated*/, chars, count);
            }
            UChar result =  (UChar) u_charMirror(ch);
            mNormalizedString.setCharAt(i, result);
#if DEBUG_GLYPHS
            ALOGD("Rewriting codepoint '%d' to '%d' at position %d",
                    ch, mNormalizedString[i], int(i));
#endif
        }
    }

#if DEBUG_GLYPHS
    if (useNormalizedString) {
        ALOGD("Will use normalized string '%s', length = %d",
                    String8(mNormalizedString.getTerminatedBuffer(),
                            mNormalizedString.length()).string(),
                    mNormalizedString.length());
    } else {
        ALOGD("Normalization is not needed or cannot be done, using initial string");
    }
#endif

    assert(mNormalizedString.length() == count);

    return useNormalizedString ? mNormalizedString.getTerminatedBuffer() : chars;
}

HB_Face HarfbuzzShaper::getCachedHBFace(SkTypeface* typeface) {
    SkFontID fontId = typeface->uniqueID();
    ssize_t index = mCachedHBFaces.indexOfKey(fontId);
    if (index >= 0) {
        return mCachedHBFaces.valueAt(index);
    }
    HB_Face face = HB_NewFace(typeface, harfbuzzSkiaGetTable);
    if (face) {
#if DEBUG_GLYPHS
        ALOGD("Created HB_NewFace %p from paint typeface = %p", face, typeface);
#endif
        mCachedHBFaces.add(fontId, face);
    }
    return face;
}

bool HarfbuzzShaper::doShaping(size_t size) {
    if (size > mShaperItemGlyphArraySize) {
        deleteShaperItemGlyphArrays();
        createShaperItemGlyphArrays(size);
    }
    mShaperItem.num_glyphs = mShaperItemGlyphArraySize;
    memset(mShaperItem.offsets, 0, mShaperItem.num_glyphs * sizeof(HB_FixedPoint));
    return HB_ShapeItem(&mShaperItem);
}

void HarfbuzzShaper::createShaperItemGlyphArrays(size_t size) {
#if DEBUG_GLYPHS
    ALOGD("Creating Glyph Arrays with size = %d", size);
#endif
    mShaperItemGlyphArraySize = size;

    // These arrays are all indexed by glyph.
    mShaperItem.glyphs = new HB_Glyph[size];
    mShaperItem.attributes = new HB_GlyphAttributes[size];
    mShaperItem.advances = new HB_Fixed[size];
    mShaperItem.offsets = new HB_FixedPoint[size];

    // Although the log_clusters array is indexed by character, Harfbuzz expects that
    // it is big enough to hold one element per glyph.  So we allocate log_clusters along
    // with the other glyph arrays above.
    mShaperItem.log_clusters = new unsigned short[size];
}

void HarfbuzzShaper::deleteShaperItemGlyphArrays() {
    delete[] mShaperItem.glyphs;
    delete[] mShaperItem.attributes;
    delete[] mShaperItem.advances;
    delete[] mShaperItem.offsets;
    delete[] mShaperItem.log_clusters;
}

void HarfbuzzShaper::setShapingScript(HB_Script shapingScript) {
    mShapingScript = shapingScript;
}

}
