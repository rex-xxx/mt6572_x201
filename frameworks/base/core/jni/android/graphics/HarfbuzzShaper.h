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

#ifndef HARFBUZZSHAPER_H_
#define HARFBUZZSHAPER_H_

#include "RtlProperties.h"
#include "ScriptRunLayoutShaper.h"

#include <utils/KeyedVector.h>
#include <utils/Singleton.h>

#include "SkTypeface.h"

extern "C" {
#include "harfbuzz-shaper.h"
}

#define UNICODE_ZWSP                    0x200b

namespace android {

class HarfbuzzShaper : public Singleton<HarfbuzzShaper>, public ScriptRunLayoutShaper {
public:
    HarfbuzzShaper();
    ~HarfbuzzShaper();
    virtual jfloat shapeScriptRun(const SkPaint* paint, const UChar* chars,
                size_t count, bool isRTL, Vector<jfloat>* const outAdvances,
                Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos,
                jfloat startXPosition, uint32_t startScriptRun, size_t glyphBaseCount);
    virtual void purgeCaches();
    void setShapingScript(HB_Script shapingScript);

private:
    const UChar* normalizeString(const UChar *chars, size_t count, bool isRTL);

    HB_Face getCachedHBFace(SkTypeface* typeface);

    void initFaceAndFont(const SkPaint* paint);
    void initShapingItem(const UChar *chars, size_t count, bool isRTL);
    bool doShaping(size_t size);
    void createShaperItemGlyphArrays(size_t size);
    void deleteShaperItemGlyphArrays();

    /**
     * Harfbuzz shaper item
     */
    HB_ShaperItem mShaperItem;

    /**
     * Harfbuzz font
     */
    HB_FontRec mFontRec;

    /**
     * Skia Paint used for shaping
     */
    SkPaint mShapingPaint;

    /**
         * Cache of Harfbuzz faces
         */
    KeyedVector<SkFontID, HB_Face> mCachedHBFaces;

    /**
     * Cache of glyph array size
     */
    size_t mShaperItemGlyphArraySize;

    /**
     * Buffer for containing the ICU normalized form of a run
     */
    UnicodeString mNormalizedString;

    HB_Script mShapingScript;

    /**
     * Buffer for normalizing a piece of a run with ICU
     */
    UnicodeString mBuffer;

};

}

#endif /* HARFBUZZSHAPER_H_ */
