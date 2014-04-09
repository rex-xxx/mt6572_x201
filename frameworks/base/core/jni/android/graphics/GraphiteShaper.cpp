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

#define LOG_TAG "GraphiteShaper"

#include "GraphiteShaper.h"

#include "SkFontHost.h"

#include "RtlProperties.h"

#include <utils/KeyedVector.h>
#include <utils/Log.h>

static const void *graphiteSkiaGetTable(const void* appFaceHandle, unsigned int name, size_t *len) {
    const SkTypeface* typeface = static_cast<const SkTypeface*>(appFaceHandle);

    if (!typeface) {
        ALOGD("Typeface cannot be null");
        return NULL;
    }
    const size_t tableSize = SkFontHost::GetTableSize(typeface->uniqueID(), name);
    if (!tableSize)
        return NULL;

    void * tableBuffer = malloc(tableSize);

    *len = tableSize;
    // If Harfbuzz specified a NULL buffer then it's asking for the size of the table.
    if (!tableBuffer) {
        return NULL;
    }

#if DEBUG_CALLBACK
    ALOGD("graphiteSkiaGetTable, name = 0x%x, tableSize = %d, tableBuffer = %p", name, tableSize, tableBuffer);
#endif
    SkFontHost::GetTableData(typeface->uniqueID(), name, 0, tableSize, tableBuffer);
    return tableBuffer;
}


static void graphiteSkiaReleaseTable(const void* appFaceHandle, const void *table_buffer) {
    free((void *)table_buffer);          /* simply free the allocated memory */
}

static const gr_face_ops GraphiteFaceOps = {
        sizeof(gr_face_ops),
        graphiteSkiaGetTable,
        graphiteSkiaReleaseTable
};

static float getXAdvances(const void* appFontHandle, gr_uint16 glyphid){
    const SkPaint* constPaint =  static_cast<const SkPaint*>(appFontHandle);
    SkPaint* paint = const_cast<SkPaint*>(constPaint);
    paint->setTextEncoding(SkPaint::kGlyphID_TextEncoding);

    uint16_t skiaGlyph = static_cast<uint16_t>(glyphid);
    SkScalar scalarAdvance;
    paint->getTextWidths(&skiaGlyph, sizeof(uint16_t), &scalarAdvance);

#if DEBUG_CALLBACK
        ALOGD("glyphHAdvance -- glyph = 0x%x => advance=%f", skiaGlyph, scalarAdvance);
#endif
    return static_cast<float>(scalarAdvance);
}

static const gr_font_ops GraphiteFontOps = {
        sizeof(gr_font_ops),
        getXAdvances,
        NULL
};

static void logGlyphs (gr_segment *segment, gr_face *grFace, gr_font *grFont) {

}

namespace android {
//--------------------------------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(GraphiteLayoutShaper);

//--------------------------------------------------------------------------------------------------

GraphiteLayoutShaper::GraphiteLayoutShaper() {

}

GraphiteLayoutShaper::~GraphiteLayoutShaper() {
    purgeCaches();
}

cluster_t *GraphiteLayoutShaper::getCluster(gr_segment *segment, size_t numCodePoints) {
    const gr_slot *is;
    int ic, ci = 0;

    cluster_t *clusters = (cluster_t *) malloc(numCodePoints * sizeof(cluster_t));
    memset(clusters, 0, numCodePoints * sizeof(cluster_t));
    for (is = gr_seg_first_slot(segment), ic = 0; is; is = gr_slot_next_in_segment(is), ic++) {
        unsigned int before = gr_slot_before(is);
        unsigned int after = gr_slot_after(is);

        while (clusters[ci].base_char > before && ci) {
            clusters[ci - 1].num_chars += clusters[ci].num_chars;
            clusters[ci - 1].num_glyphs += clusters[ci].num_glyphs;
            --ci;
        }

        if (gr_slot_can_insert_before(is) && clusters[ci].num_chars
                && before >= clusters[ci].base_char + clusters[ci].num_chars) {
            cluster_t *c = clusters + ci + 1;
            c->base_char = clusters[ci].base_char + clusters[ci].num_chars;
            c->num_chars = before - c->base_char;
            c->base_glyph = ic;
            c->num_glyphs = 0;
            ++ci;
        }
        ++clusters[ci].num_glyphs;

        if (clusters[ci].base_char + clusters[ci].num_chars < after + 1) {
            clusters[ci].num_chars = after + 1 - clusters[ci].base_char;
        }
    }
    return clusters;
}

jfloat GraphiteLayoutShaper::shapeScriptRun(const SkPaint* paint, const UChar* chars,
        size_t count, bool isRTL, Vector<jfloat>* const outAdvances,
        Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos,
        jfloat startXPosition, uint32_t startScriptRun, size_t glyphBaseCount) {
    ALOGD("Shape Scrit Run with Graphite");
    SkTypeface *typeface = paint->getTypeface();
    mShapingGrFace = getCachedGrFace(typeface);

    unsigned int x_ppem;
    unsigned int y_ppem;
    int x_scale;
    int y_scale;
    getConversionFactor(x_ppem, y_ppem, x_scale, y_scale, paint);

    mShapingGrFont = gr_make_font_with_ops(x_ppem, paint, &GraphiteFontOps, mShapingGrFace);

    const uint16_t *charsScriptStart = chars ;
    const uint16_t *charsScriptEnd = chars + count;

    size_t numCP;
    char *pError;

    numCP = gr_count_unicode_characters(gr_utf16, charsScriptStart, charsScriptEnd, (const void **)(&pError));

#if DEBUG_GLYPHS
    ALOGD("Myanmar shape with Graphite, numCP = %d, grFont = %p, grFace = %p, pError = %p",
            numCP, mShapingGrFont, mShapingGrFace, pError);
#endif

    if(!mShapingGrFont || !mShapingGrFace || pError) {
        ALOGD("Invalid grFont is NULL, grFace is NULL or parse string error.");
        return 0.0;
    }

    mShapingSegment = gr_make_seg(mShapingGrFont, mShapingGrFace, 0, 0,
            gr_utf16, charsScriptStart, numCP, isRTL);
    cluster_t *clusters = getCluster(mShapingSegment, numCP);

#if DEBUG_GLYPHS
        ALOGD("Got from Graphite");
#endif

    const gr_slot *slot;
    size_t glyphIndex, clusterIndex = 0;
    jfloat totalFontRunAdvance = 0;
    for (slot = gr_seg_first_slot(mShapingSegment), glyphIndex = 0; slot;
            slot = gr_slot_next_in_segment(slot), ++ glyphIndex) {
#if DEBUG_GLYPHS
        ALOGD("         -- Glyph = %d, origin = (%f, %f), advance = (%f, %f)\n",
                gr_slot_gid(slot), gr_slot_origin_X(slot),
                gr_slot_origin_Y(slot),
                gr_slot_advance_X(slot, mShapingGrFace, mShapingGrFont),
                gr_slot_advance_Y(slot, mShapingGrFace, mShapingGrFont));
#endif
        if(glyphIndex == clusters[clusterIndex].base_glyph + clusters[clusterIndex].num_glyphs) {
            uint32_t clusterStart = clusters[clusterIndex].base_char + startScriptRun;
            jfloat startOrigin = outAdvances->itemAt(clusterStart);
            jfloat advance = gr_slot_origin_X(slot) - startOrigin;
            outAdvances->replaceAt(advance, clusterStart);

            ++ clusterIndex;
            clusterStart = clusters[clusterIndex].base_char + startScriptRun;
            outAdvances->replaceAt(gr_slot_origin_X(slot), clusterStart);
        }

        jfloat charRight = gr_slot_origin_X(slot) + gr_slot_advance_X(slot, mShapingGrFace, mShapingGrFont);
        if (charRight > totalFontRunAdvance) {
            totalFontRunAdvance = charRight;
        }

        jchar glyph = glyphBaseCount + gr_slot_gid(slot);
        outGlyphs->add(glyph);

        outPos->add(startXPosition + gr_slot_origin_X(slot));
        outPos->add(gr_slot_origin_Y(slot));
    }

    uint32_t clusterStart = clusters[clusterIndex].base_char + startScriptRun;
    jfloat startOrigin = outAdvances->itemAt(clusterStart);
    jfloat advance = totalFontRunAdvance - startOrigin;
    outAdvances->replaceAt(advance, clusterStart);

    if(mShapingSegment) {
        gr_seg_destroy(mShapingSegment);
        mShapingSegment = NULL;
    }

    if(mShapingGrFont) {
        gr_font_destroy(mShapingGrFont);
        mShapingGrFont = NULL;
    }
#if DEBUG_GLYPHS
    ALOGD("         -- totalFontRunAdvance = %f", totalFontRunAdvance);
#endif
    return totalFontRunAdvance;
}

gr_face * GraphiteLayoutShaper::getCachedGrFace(SkTypeface* typeface) {
    SkFontID fontId = typeface->uniqueID();
    ssize_t index = mCachedGrFaces.indexOfKey(fontId);
    if (index >= 0) {
        return mCachedGrFaces.valueAt(index);
    }
    gr_face * face = gr_make_face_with_ops(typeface, &GraphiteFaceOps, gr_face_preloadAll);
    if (face) {
#if DEBUG_GLYPHS
        ALOGD("Created hb_face_t %p from paint typeface = %p", face, typeface);
#endif
        mCachedGrFaces.add(fontId, face);
    }
    return face;
}

void GraphiteLayoutShaper::purgeCaches() {
    size_t cacheSize = mCachedGrFaces.size();
    for (size_t i = 0; i < cacheSize; ++i) {
        gr_face_destroy(mCachedGrFaces.valueAt(i));
    }
    mCachedGrFaces.clear();
}

}
