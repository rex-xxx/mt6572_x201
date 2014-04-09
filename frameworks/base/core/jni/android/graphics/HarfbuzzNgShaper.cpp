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

#define LOG_TAG "HarfbuzzNgShaper"

#include "HarfbuzzNgShaper.h"
#include "RtlProperties.h"

#include "SkFontHost.h"

#include "SkPaint.h"
#include "SkStream.h"
#include "SkTypeface.h"
#include "SkUtils.h"

#include <utils/Log.h>

// As the user fo Harfbuzz-ng, we know we need to use icu Unicode callbacks.
#include "hb-icu.h"

static inline hb_position_t SkScalarToHBFixedNg(SkScalar value) {
    // HB_Fixed is a 26.6 fixed point format.
    return static_cast<hb_position_t>(SkScalarToFloat(value) * 2048);
}

static inline float HBFixedNgToFloat(hb_position_t v) {
    // Harfbuzz uses fixed point values for pixel offsets
    return v * (1.0f / 2048);
}

static inline float HBFixedNgToAdvFloat(hb_position_t v) {
    // Harfbuzz uses fixed point values for pixel offsets
    int value = static_cast<int>(v * (1.0f / 2048) + 0.5);
    return static_cast<float>(value);
}

hb_bool_t getGlyph (hb_font_t *font, void *font_data,
                           hb_codepoint_t unicode, hb_codepoint_t variation_selector,
                           hb_codepoint_t *glyph,
                           void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kUTF16_TextEncoding);

    uint16_t character[2];
    uint16_t num = SkUTF16_FromUnichar(unicode, character);
    uint16_t skiaGlyph;
    paint->textToGlyphs(character, sizeof(uint16_t) * num, &skiaGlyph);
    if (skiaGlyph == 0) {
        num = SkUTF16_FromUnichar(unicode, character);
        paint->textToGlyphs(character, sizeof(uint16_t) * num, &skiaGlyph);
    }
    *glyph = skiaGlyph;
#if DEBUG_CALLBACK
    ALOGD("getGlyph() unicode=0x%x, variation_selector=0x%x => skiaGlyph=0x%x\n", unicode, variation_selector, skiaGlyph);
#endif
    return true;
}

hb_position_t getGlyphHAdvance(hb_font_t *font, void *font_data, hb_codepoint_t glyph, void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t skiaGlyph = static_cast<uint16_t>(glyph);
    SkScalar scalarAdvance;
    paint->getTextWidths(&skiaGlyph, sizeof(uint16_t), &scalarAdvance);

#if DEBUG_CALLBACK
        ALOGD("glyphHAdvance -- glyph: 0x%x => advance=%f", glyph, scalarAdvance);
#endif
    return SkScalarToHBFixedNg(scalarAdvance);
}

hb_position_t getGlyphVAdvance(hb_font_t *font, void *font_data, hb_codepoint_t glyph, void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t skiaGlyph = static_cast<uint16_t>(glyph);
    SkScalar scalarAdvance;
    paint->getTextWidths(&skiaGlyph, sizeof(uint16_t), &scalarAdvance);

#if DEBUG_CALLBACK
        ALOGD("glyph: 0x%x, glyphVAdvance -- advance=%f", glyph, scalarAdvance);
#endif
    return SkScalarToHBFixedNg(scalarAdvance);
}

hb_bool_t getGlyphHOrigin(hb_font_t *font, void *font_data,
                              hb_codepoint_t glyph,
                              hb_position_t *x, hb_position_t *y,
                              void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t glyph16 = glyph;
    SkScalar width;
    SkRect bounds;
    paint->getTextWidths(&glyph16, sizeof(glyph16), &width, &bounds);
//    *x = SkScalarToHBFixedNg(bounds.fLeft);
//    *y = SkScalarToHBFixedNg(bounds.fTop);
#if DEBUG_CALLBACK
    ALOGD("getGlyphHOrigin: glyph=0x%x => bounds.fLeft=%f, bounds.fTop=%f, bounds.fRight=%f, bounds.fBottom=%f", glyph, bounds.fLeft, bounds.fTop, bounds.fRight, bounds.fBottom);
    ALOGD("getGlyphHOrigin: glyph=0x%x => x_origin=%d, y_origint=%d", glyph, *x, *y );
#endif
    return false;
}

hb_bool_t getGlyphVOrigin(hb_font_t *font, void *font_data,
                              hb_codepoint_t glyph,
                              hb_position_t *x, hb_position_t *y,
                              void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t glyph16 = glyph;
    SkScalar width;
    SkRect bounds;
    paint->getTextWidths(&glyph16, sizeof(glyph16), &width, &bounds);
//    *x = SkScalarToHBFixedNg(bounds.fLeft);
//    *y = SkScalarToHBFixedNg(bounds.fTop);
#if DEBUG_CALLBACK
    ALOGD("getGlyphVOrigin: glyph=0x%x, x_origin=%d, y_origin=%d", glyph, *x, *y );
#endif
    return false;
}

hb_position_t getGlyphHKerning (hb_font_t *font, void *font_data,
                               hb_codepoint_t first_glyph, hb_codepoint_t second_glyph,
                               void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

#if DEBUG_CALLBACK
    ALOGD("getGlyphHKerning: first_glyph=0x%x, second_glyph=0x%x", first_glyph, second_glyph);
#endif

    return 0;
}

hb_position_t getGlyphVKerning (hb_font_t *font, void *font_data,
                               hb_codepoint_t first_glyph, hb_codepoint_t second_glyph,
                               void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);
#if DEBUG_CALLBACK
    ALOGD("getGlyphVKerning: first_glyph=0x%x, second_glyph=0x%x", first_glyph, second_glyph);
#endif
    return 0;
}

hb_bool_t getGlyphExtents (hb_font_t *font, void *font_data,
                               hb_codepoint_t glyph,
                               hb_glyph_extents_t *extents,
                               void *user_data){
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t glyph16 = glyph;
    SkScalar width;
    SkRect bounds;
    paint->getTextWidths(&glyph16, sizeof(glyph16), &width, &bounds);

    extents->height = bounds.height();
    extents->width = bounds.width();
    extents->x_bearing = bounds.fLeft;
    extents->y_bearing = bounds.fTop;
#if DEBUG_CALLBACK
    ALOGD("extents->height=%d, extents->width=%d", extents->height, extents->width);
#endif
    return true;
}

hb_bool_t getGlyphContourPoint (hb_font_t *font, void *font_data,
                                 hb_codepoint_t glyph, unsigned int point_index,
                                 hb_position_t *x, hb_position_t *y,
                                 void *user_data){
    ALOGD("Shape Scrit Run with HarfbuzzNg");
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t glyph16 = glyph;

    SkPath path;
    paint->getTextPath(&glyph16, sizeof(glyph16), 0, 0, &path);
    uint32_t numPoints = path.getPoints(0, 0);

    if(point_index >= numPoints) {
        return false;
    }

    SkPoint* points = static_cast<SkPoint*>(malloc(sizeof(SkPoint) * (point_index + 1)));
    if(! points) {
        return false;
    }
    path.getPoints(points, point_index + 1);
    *x = SkScalarToHBFixedNg(points[point_index].fX);
    *y = SkScalarToHBFixedNg(points[point_index].fY);
#if DEBUG_CALLBACK
    ALOGD("Point: x=%d, y=%d", *x, *y );
#endif
    delete points;

    return true;
}

hb_bool_t getGlyphName (hb_font_t *font, void *font_data,
                            hb_codepoint_t glyph,
                            char *name, unsigned int size,
                            void *user_data){
#if DEBUG_CALLBACK
    ALOGD("getGlyphName, glyph=0x%x", glyph);
#endif
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    return false;
}

hb_bool_t getGlyphFromName (hb_font_t *font, void *font_data,
                             const char *name, int len, /* -1 means nul-terminated */
                             hb_codepoint_t *glyph,
                             void *user_data){
#if DEBUG_CALLBACK
    ALOGD("getGlyphFromName");
#endif
    SkPaint* paint = static_cast<SkPaint*>(font_data);
    return false;
}

hb_font_funcs_t *harfbuzzSkiaNgGetClass() {
    hb_font_funcs_t * hb_funcs = hb_font_funcs_create();

    hb_font_funcs_set_glyph_contour_point_func(hb_funcs, getGlyphContourPoint, NULL, NULL);
    hb_font_funcs_set_glyph_extents_func(hb_funcs, getGlyphExtents, NULL, NULL);
    hb_font_funcs_set_glyph_from_name_func(hb_funcs, getGlyphFromName, NULL, NULL);
    hb_font_funcs_set_glyph_func(hb_funcs, getGlyph, NULL, NULL);
    hb_font_funcs_set_glyph_h_advance_func(hb_funcs, getGlyphHAdvance, NULL, NULL);
    hb_font_funcs_set_glyph_h_kerning_func(hb_funcs, getGlyphHKerning, NULL, NULL);
    hb_font_funcs_set_glyph_h_origin_func(hb_funcs, getGlyphHOrigin, NULL, NULL);
    hb_font_funcs_set_glyph_name_func(hb_funcs, getGlyphName, NULL, NULL);
    hb_font_funcs_set_glyph_v_advance_func(hb_funcs, getGlyphVAdvance, NULL, NULL);
    hb_font_funcs_set_glyph_v_kerning_func(hb_funcs, getGlyphVKerning, NULL, NULL);
    hb_font_funcs_set_glyph_v_origin_func(hb_funcs, getGlyphVOrigin, NULL, NULL);

    return hb_funcs;
}

hb_font_funcs_t *harfbuzzSkiaNgClass = harfbuzzSkiaNgGetClass();

void harfbuzzSkiaNgSetDirection (hb_buffer_t *buffer, bool isRTL){
    if(isRTL) {
        hb_buffer_set_direction(buffer, HB_DIRECTION_RTL);
    } else {
        hb_buffer_set_direction(buffer, HB_DIRECTION_LTR);
    }
}

static hb_blob_t * harfbuzzSkiaNgGetTable(hb_face_t *face, hb_tag_t tag, void *user_data){
    SkTypeface* typeface = static_cast<SkTypeface*>(user_data);

    if (!typeface) {
        ALOGD("Typeface cannot be null");
        return NULL;
    }

    const size_t tableSize = SkFontHost::GetTableSize(typeface->uniqueID(), tag);
    if (!tableSize)
        return NULL;

    char *table = (char *)malloc(tableSize);
    SkFontHost::GetTableData(typeface->uniqueID(), tag, 0, tableSize, table);

    hb_blob_t *blob = hb_blob_create(table, tableSize, HB_MEMORY_MODE_READONLY, table, free);

    return blob;
}

void unrefSkStream (void *user_data) {
    SkStream* stream = static_cast<SkStream *>(user_data);
    stream->unref();
}

static void logGlyphs(hb_buffer_t * hbBuffer) {
    unsigned int countNgGlyphs = hb_buffer_get_length (hbBuffer);
    hb_glyph_info_t *glyphInfos = hb_buffer_get_glyph_infos (hbBuffer, NULL);
    hb_glyph_position_t *glyphPositions = hb_buffer_get_glyph_positions (hbBuffer, NULL);
    ALOGD ("Glyph number from Harfbuzz-ng(script) -- count = %d\n", countNgGlyphs);

    for (unsigned int i = 0; i < countNgGlyphs; i++) {
      hb_glyph_info_t *info = &glyphInfos[i];
      hb_glyph_position_t *pos = &glyphPositions[i];
      ALOGD ("          -- Glyph at [%d] info: cluster %d glyph 0x%x at   (%f,%f)+(%f,%f)\n", i,
          info->cluster, info->codepoint,
          HBFixedNgToFloat(pos->x_offset),
          HBFixedNgToFloat(pos->y_offset),
          HBFixedNgToAdvFloat(pos->x_advance),
          HBFixedNgToAdvFloat(pos->y_advance));
    }
}

namespace android {

//--------------------------------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(HarfbuzzNgShaper);

//--------------------------------------------------------------------------------------------------

HarfbuzzNgShaper::HarfbuzzNgShaper() {
    mHbBuffer = hb_buffer_create();
}

HarfbuzzNgShaper::~HarfbuzzNgShaper() {
    hb_buffer_destroy(mHbBuffer);
    purgeCaches();
}

void HarfbuzzNgShaper::setShapingScript(hb_script_t script) {
    mShapingScript = script;
}

bool HarfbuzzNgShaper::isRtlScript(hb_script_t script) {
    switch(script) {
    case HB_SCRIPT_ARABIC:
    case HB_SCRIPT_HEBREW:
        return true;
    default:
        return false;
    }
}

void HarfbuzzNgShaper::prepareBuffer(const UChar* chars, size_t count) {
    clearHBBuffer();
    hb_buffer_add_utf16(mHbBuffer, chars, count, 0, count);
    hb_buffer_set_script(mHbBuffer, mShapingScript);
    harfbuzzSkiaNgSetDirection(mHbBuffer, isRtlScript(mShapingScript));
}

hb_font_t *HarfbuzzNgShaper::getHBNgFont(const SkPaint *paint) {
    unsigned int x_ppem;
    unsigned int y_ppem;
    int x_scale;
    int y_scale;
    getConversionFactor(x_ppem, y_ppem, x_scale, y_scale, paint);

    SkTypeface *typeface = paint->getTypeface();

    hb_face_t * hb_face = getCachedHBNgFace(typeface);
    hb_font_t * hb_font = hb_font_create(hb_face);
    hb_font_set_scale(hb_font, x_scale, y_scale);
    hb_font_set_ppem(hb_font, x_ppem, y_ppem);
    hb_font_set_funcs(hb_font, harfbuzzSkiaNgClass, (void *)paint, NULL);

#if DEBUG_GLYPHS
    ALOGD("Run typeface = %p, uniqueID = %d, hb_face_t = %p",
            typeface, typeface->uniqueID(), hb_face);
#endif

    return hb_font;
}

jfloat HarfbuzzNgShaper::shapeScriptRun(const SkPaint* paint, const UChar* chars,
        size_t count, bool isRTL, Vector<jfloat>* const outAdvances,
        Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos,
        jfloat startXPosition, uint32_t startScriptRun, size_t glyphBaseCount) {
#if DEBUG_GLYPHS
    ALOGD("HarfbuzzNgShaper::shapeScriptRun text = '%s'", String8(chars, count).string());
#endif
    prepareBuffer(chars, count);

    hb_font_t * hb_font = getHBNgFont(paint);

    // Shape
    hb_shape(hb_font, mHbBuffer, NULL, 0);
    hb_font_destroy(hb_font);

    unsigned int countNgGlyphs = hb_buffer_get_length (mHbBuffer);
    hb_glyph_info_t *glyphInfos = hb_buffer_get_glyph_infos (mHbBuffer, NULL);
    hb_glyph_position_t *glyphPositions = hb_buffer_get_glyph_positions (mHbBuffer, NULL);

#if DEBUG_GLYPHS
    ALOGD("Got from Harfbuzz-ng");
    ALOGD("         -- glyphBaseCount = %d", glyphBaseCount);
    ALOGD("         -- num_glypth = %d", countNgGlyphs);
    ALOGD("         -- isDevKernText = %d", paint->isDevKernText());
    ALOGD("         -- startXPosition = %f", startXPosition);

    logGlyphs(mHbBuffer);
#endif

    if (glyphInfos == NULL || glyphPositions == 0) {
#if DEBUG_GLYPHS
        ALOGD("Advances array is empty or num_glypth = 0");
#endif
        return 0.0f;
    }

    jfloat totalFontRunAdvance = 0;
    for (size_t i = 0; i < countNgGlyphs; i++) {
        uint32_t loc = glyphInfos[i].cluster + startScriptRun;
        if(loc < count + startScriptRun) {
            jfloat advance = outAdvances->itemAt(loc);
            advance += HBFixedNgToAdvFloat(glyphPositions[i].x_advance);
            totalFontRunAdvance += HBFixedNgToAdvFloat(glyphPositions[i].x_advance);
            outAdvances->replaceAt(advance, loc);
        }
    }

    // Get Glyphs
    // Harfbuzz-ng have helped us reverse the glyphs when RTL.
    if (glyphInfos) {
#if DEBUG_GLYPHS
        ALOGD("Returned script run glyphs -- count = %d", countNgGlyphs);
#endif
        for (size_t i = 0; i < countNgGlyphs; i++) {
            jchar glyph = glyphBaseCount + glyphInfos[i].codepoint;
#if DEBUG_GLYPHS
            ALOGD("         -- glyph[%d] = %d", i, glyph);
#endif
            outGlyphs->add(glyph);
        }
    }

    float skewX = paint->getTextSkewX();
    // Get glyph positions (and reverse them in place if RTL)
    if (glyphPositions) {
        jfloat x = startXPosition;
        for (size_t i = 0; i < countNgGlyphs; i++) {
            float xo = HBFixedNgToFloat(glyphPositions[i].x_offset);
            float yo = HBFixedNgToFloat(glyphPositions[i].y_offset);
            // Apply skewX component of transform to position offsets. Note
            // that scale has already been applied through x_ and y_scale
            // set in the mFontRec.
            outPos->add(x + xo + yo * skewX);
            outPos->add(yo);
#if DEBUG_GLYPHS
            ALOGD("         -- hb adv[%d] = %f, log_cluster[%d] = %d",
                    i, HBFixedNgToAdvFloat(glyphPositions[i].x_advance),
                    i, glyphInfos[i].cluster);
#endif
            x += HBFixedNgToAdvFloat(glyphPositions[i].x_advance);
        }
    }

#if DEBUG_GLYPHS
            ALOGD("totalFontRunAdvance = %f", totalFontRunAdvance);
#endif
    return totalFontRunAdvance;
}

void HarfbuzzNgShaper::purgeCaches() {
    size_t cacheSize = mCachedHBNgFaces.size();
    for (size_t i = 0; i < cacheSize; ++ i) {
        hb_face_destroy(mCachedHBNgFaces.valueAt(i));
    }
    mCachedHBNgFaces.clear();
}

void HarfbuzzNgShaper::clearHBBuffer(void) {
    hb_buffer_reset(mHbBuffer);
    hb_unicode_funcs_t * unicode_funcs = hb_icu_get_unicode_funcs();
    hb_buffer_set_unicode_funcs(mHbBuffer, unicode_funcs);
}

hb_face_t * HarfbuzzNgShaper::getCachedHBNgFace(SkTypeface* typeface) {
    SkFontID fontId = typeface->uniqueID();
    ssize_t index = mCachedHBNgFaces.indexOfKey(fontId);
    if (index >= 0) {
        return mCachedHBNgFaces.valueAt(index);
    }

    SkStream* stream = SkFontHost::OpenStream(fontId);
    stream->ref();

    hb_face_t * face;
    const char* memoryBase = static_cast<const char*>(stream->getMemoryBase());
    if (memoryBase != NULL) {
        hb_blob_t *blob;
        blob = hb_blob_create(memoryBase, stream->getLength(),
                HB_MEMORY_MODE_READONLY, stream, unrefSkStream);

        face = hb_face_create(blob, 0);
        hb_blob_destroy(blob);
        ALOGD("Created hb_face_t %p from memoryBase = %p", face, memoryBase);
    } else {
        ALOGW("Cannot create hb_face_t with memory map");
        face = hb_face_create_for_tables(harfbuzzSkiaNgGetTable, typeface, NULL);
    }

    if (face) {
#if DEBUG_GLYPHS
        ALOGD("Created hb_face_t %p from typeface = %p", face, typeface);
#endif
        mCachedHBNgFaces.add(fontId, face);
    }
    return face;
}

}
