/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "TextLayoutCache"

#include "TextLayoutCache.h"
#include "TextLayout.h"
#include "SkFontHost.h"
#include "SkTypeface_android.h"
#include <unicode/unistr.h>
#include <unicode/normlzr.h>
#include <unicode/uchar.h>

#include "hb-icu.h"

#include "GraphiteShaper.h"
#include "HarfbuzzNgShaper.h"
#include "HarfbuzzShaper.h"

extern "C" {
  #include "harfbuzz-unicode.h"
}

#define USE_HARFBUZZ_NG 1

namespace android {

//--------------------------------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(TextLayoutEngine);

//--------------------------------------------------------------------------------------------------

TextLayoutCache::TextLayoutCache(TextLayoutShaper* shaper) :
        mShaper(shaper),
        mCache(GenerationCache<TextLayoutCacheKey, sp<TextLayoutValue> >::kUnlimitedCapacity),
        mSize(0), mMaxSize(MB(DEFAULT_TEXT_LAYOUT_CACHE_SIZE_IN_MB)),
        mCacheHitCount(0), mNanosecondsSaved(0) {
    init();
}

TextLayoutCache::~TextLayoutCache() {
    mCache.clear();
}

void TextLayoutCache::init() {
    mCache.setOnEntryRemovedListener(this);

    mDebugLevel = readRtlDebugLevel();
    mDebugEnabled = mDebugLevel & kRtlDebugCaches;
    ALOGD("Using debug level = %d - Debug Enabled = %d", mDebugLevel, mDebugEnabled);

    mCacheStartTime = systemTime(SYSTEM_TIME_MONOTONIC);

    if (mDebugEnabled) {
        ALOGD("Initialization is done - Start time = %lld", mCacheStartTime);
    }

    mInitialized = true;
}

/**
 *  Callbacks
 */
void TextLayoutCache::operator()(TextLayoutCacheKey& text, sp<TextLayoutValue>& desc) {
    size_t totalSizeToDelete = text.getSize() + desc->getSize();
    mSize -= totalSizeToDelete;
    if (mDebugEnabled) {
        ALOGD("Cache value %p deleted, size = %d", desc.get(), totalSizeToDelete);
    }
}

/*
 * Cache clearing
 */
void TextLayoutCache::purgeCaches() {
    AutoMutex _l(mLock);
    mCache.clear();
    mShaper->purgeCaches();
}

/*
 * Caching
 */
sp<TextLayoutValue> TextLayoutCache::getValue(const SkPaint* paint,
            const jchar* text, jint start, jint count, jint contextCount, jint dirFlags) {
    AutoMutex _l(mLock);
    nsecs_t startTime = 0;
    if (mDebugEnabled) {
        startTime = systemTime(SYSTEM_TIME_MONOTONIC);
    }

    // Create the key
    TextLayoutCacheKey key(paint, text, start, count, contextCount, dirFlags);

    // Get value from cache if possible
    sp<TextLayoutValue> value = mCache.get(key);

    // Value not found for the key, we need to add a new value in the cache
    if (value == NULL) {
        if (mDebugEnabled) {
            startTime = systemTime(SYSTEM_TIME_MONOTONIC);
        }

        value = new TextLayoutValue(contextCount);

        // Compute advances and store them
        mShaper->computeValues(value.get(), paint,
                reinterpret_cast<const UChar*>(key.getText()), start, count,
                size_t(contextCount), int(dirFlags));

        if (mDebugEnabled) {
            value->setElapsedTime(systemTime(SYSTEM_TIME_MONOTONIC) - startTime);
        }

        // Don't bother to add in the cache if the entry is too big
        size_t size = key.getSize() + value->getSize();
        if (size <= mMaxSize) {
            // Cleanup to make some room if needed
            if (mSize + size > mMaxSize) {
                if (mDebugEnabled) {
                    ALOGD("Need to clean some entries for making some room for a new entry");
                }
                while (mSize + size > mMaxSize) {
                    // This will call the callback
                    bool removedOne = mCache.removeOldest();
                    LOG_ALWAYS_FATAL_IF(!removedOne, "The cache is non-empty but we "
                            "failed to remove the oldest entry.  "
                            "mSize = %u, size = %u, mMaxSize = %u, mCache.size() = %u",
                            mSize, size, mMaxSize, mCache.size());
                }
            }

            // Update current cache size
            mSize += size;

            bool putOne = mCache.put(key, value);
            LOG_ALWAYS_FATAL_IF(!putOne, "Failed to put an entry into the cache.  "
                    "This indicates that the cache already has an entry with the "
                    "same key but it should not since we checked earlier!"
                    " - start = %d, count = %d, contextCount = %d - Text = '%s'",
                    start, count, contextCount, String8(key.getText() + start, count).string());

            if (mDebugEnabled) {
                nsecs_t totalTime = systemTime(SYSTEM_TIME_MONOTONIC) - startTime;
                ALOGD("CACHE MISS: Added entry %p "
                        "with start = %d, count = %d, contextCount = %d, "
                        "entry size %d bytes, remaining space %d bytes"
                        " - Compute time %0.6f ms - Put time %0.6f ms - Text = '%s'",
                        value.get(), start, count, contextCount, size, mMaxSize - mSize,
                        value->getElapsedTime() * 0.000001f,
                        (totalTime - value->getElapsedTime()) * 0.000001f,
                        String8(key.getText() + start, count).string());
            }
        } else {
            if (mDebugEnabled) {
                ALOGD("CACHE MISS: Calculated but not storing entry because it is too big "
                        "with start = %d, count = %d, contextCount = %d, "
                        "entry size %d bytes, remaining space %d bytes"
                        " - Compute time %0.6f ms - Text = '%s'",
                        start, count, contextCount, size, mMaxSize - mSize,
                        value->getElapsedTime() * 0.000001f,
                        String8(key.getText() + start, count).string());
            }
        }
    } else {
        // This is a cache hit, just log timestamp and user infos
        if (mDebugEnabled) {
            nsecs_t elapsedTimeThruCacheGet = systemTime(SYSTEM_TIME_MONOTONIC) - startTime;
            mNanosecondsSaved += (value->getElapsedTime() - elapsedTimeThruCacheGet);
            ++mCacheHitCount;

            if (value->getElapsedTime() > 0) {
                float deltaPercent = 100 * ((value->getElapsedTime() - elapsedTimeThruCacheGet)
                        / ((float)value->getElapsedTime()));
                ALOGD("CACHE HIT #%d with start = %d, count = %d, contextCount = %d"
                        "- Compute time %0.6f ms - "
                        "Cache get time %0.6f ms - Gain in percent: %2.2f - Text = '%s'",
                        mCacheHitCount, start, count, contextCount,
                        value->getElapsedTime() * 0.000001f,
                        elapsedTimeThruCacheGet * 0.000001f,
                        deltaPercent,
                        String8(key.getText() + start, count).string());
            }
            if (mCacheHitCount % DEFAULT_DUMP_STATS_CACHE_HIT_INTERVAL == 0) {
                dumpCacheStats();
            }
        }
    }
    return value;
}

void TextLayoutCache::dumpCacheStats() {
    float remainingPercent = 100 * ((mMaxSize - mSize) / ((float)mMaxSize));
    float timeRunningInSec = (systemTime(SYSTEM_TIME_MONOTONIC) - mCacheStartTime) / 1000000000;

    size_t bytes = 0;
    size_t cacheSize = mCache.size();
    for (size_t i = 0; i < cacheSize; i++) {
        bytes += mCache.getKeyAt(i).getSize() + mCache.getValueAt(i)->getSize();
    }

    ALOGD("------------------------------------------------");
    ALOGD("Cache stats");
    ALOGD("------------------------------------------------");
    ALOGD("pid       : %d", getpid());
    ALOGD("running   : %.0f seconds", timeRunningInSec);
    ALOGD("entries   : %d", cacheSize);
    ALOGD("max size  : %d bytes", mMaxSize);
    ALOGD("used      : %d bytes according to mSize, %d bytes actual", mSize, bytes);
    ALOGD("remaining : %d bytes or %2.2f percent", mMaxSize - mSize, remainingPercent);
    ALOGD("hits      : %d", mCacheHitCount);
    ALOGD("saved     : %0.6f ms", mNanosecondsSaved * 0.000001f);
    ALOGD("------------------------------------------------");
}

/**
 * TextLayoutCacheKey
 */
TextLayoutCacheKey::TextLayoutCacheKey(): start(0), count(0), contextCount(0),
        dirFlags(0), typeface(NULL), textSize(0), textSkewX(0), textScaleX(0), flags(0),
        hinting(SkPaint::kNo_Hinting), variant(SkPaint::kDefault_Variant), language()  {
}

TextLayoutCacheKey::TextLayoutCacheKey(const SkPaint* paint, const UChar* text,
        size_t start, size_t count, size_t contextCount, int dirFlags) :
            start(start), count(count), contextCount(contextCount),
            dirFlags(dirFlags) {
    textCopy.setTo(text, contextCount);
    typeface = paint->getTypeface();
    textSize = paint->getTextSize();
    textSkewX = paint->getTextSkewX();
    textScaleX = paint->getTextScaleX();
    flags = paint->getFlags();
    hinting = paint->getHinting();
    variant = paint->getFontVariant();
    language = paint->getLanguage();
}

TextLayoutCacheKey::TextLayoutCacheKey(const TextLayoutCacheKey& other) :
        textCopy(other.textCopy),
        start(other.start),
        count(other.count),
        contextCount(other.contextCount),
        dirFlags(other.dirFlags),
        typeface(other.typeface),
        textSize(other.textSize),
        textSkewX(other.textSkewX),
        textScaleX(other.textScaleX),
        flags(other.flags),
        hinting(other.hinting),
        variant(other.variant),
        language(other.language) {
}

int TextLayoutCacheKey::compare(const TextLayoutCacheKey& lhs, const TextLayoutCacheKey& rhs) {
    int deltaInt = lhs.start - rhs.start;
    if (deltaInt != 0) return (deltaInt);

    deltaInt = lhs.count - rhs.count;
    if (deltaInt != 0) return (deltaInt);

    deltaInt = lhs.contextCount - rhs.contextCount;
    if (deltaInt != 0) return (deltaInt);

    if (lhs.typeface < rhs.typeface) return -1;
    if (lhs.typeface > rhs.typeface) return +1;

    if (lhs.textSize < rhs.textSize) return -1;
    if (lhs.textSize > rhs.textSize) return +1;

    if (lhs.textSkewX < rhs.textSkewX) return -1;
    if (lhs.textSkewX > rhs.textSkewX) return +1;

    if (lhs.textScaleX < rhs.textScaleX) return -1;
    if (lhs.textScaleX > rhs.textScaleX) return +1;

    deltaInt = lhs.flags - rhs.flags;
    if (deltaInt != 0) return (deltaInt);

    deltaInt = lhs.hinting - rhs.hinting;
    if (deltaInt != 0) return (deltaInt);

    deltaInt = lhs.dirFlags - rhs.dirFlags;
    if (deltaInt) return (deltaInt);

    deltaInt = lhs.variant - rhs.variant;
    if (deltaInt) return (deltaInt);

    if (lhs.language < rhs.language) return -1;
    if (lhs.language > rhs.language) return +1;

    return memcmp(lhs.getText(), rhs.getText(), lhs.contextCount * sizeof(UChar));
}

size_t TextLayoutCacheKey::getSize() const {
    return sizeof(TextLayoutCacheKey) + sizeof(UChar) * contextCount;
}

/**
 * TextLayoutCacheValue
 */
TextLayoutValue::TextLayoutValue(size_t contextCount) :
        mTotalAdvance(0), mElapsedTime(0) {
    // Give a hint for advances and glyphs vectors size
    mAdvances.setCapacity(contextCount);
    mGlyphs.setCapacity(contextCount);
    mPos.setCapacity(contextCount * 2);
}

size_t TextLayoutValue::getSize() const {
    return sizeof(TextLayoutValue) + sizeof(jfloat) * mAdvances.capacity() +
            sizeof(jchar) * mGlyphs.capacity() + sizeof(jfloat) * mPos.capacity();
}

void TextLayoutValue::setElapsedTime(uint32_t time) {
    mElapsedTime = time;
}

uint32_t TextLayoutValue::getElapsedTime() {
    return mElapsedTime;
}

TextLayoutShaper::TextLayoutShaper() {
    init();

    // Note that the scaling values (x_ and y_ppem, x_ and y_scale) will be set
    // below, when the paint transform and em unit of the actual shaping font
    // are known.

    mHbBuffer = hb_buffer_create();
}

void TextLayoutShaper::init() {
    mDefaultTypeface = SkFontHost::CreateTypeface(NULL, NULL, NULL, 0, SkTypeface::kNormal);
}

void TextLayoutShaper::unrefTypefaces() {
    SkSafeUnref(mDefaultTypeface);
}

TextLayoutShaper::~TextLayoutShaper() {
    hb_buffer_destroy(mHbBuffer);

    unrefTypefaces();
}

void TextLayoutShaper::computeValues(TextLayoutValue* value, const SkPaint* paint, const UChar* chars,
        size_t start, size_t count, size_t contextCount, int dirFlags) {

    computeValues(paint, chars, start, count, contextCount, dirFlags,
            &value->mAdvances, &value->mTotalAdvance, &value->mGlyphs, &value->mPos);
#if DEBUG_ADVANCES
    ALOGD("Advances - start = %d, count = %d, contextCount = %d, totalAdvance = %f, textSize = %f", start, count,
            contextCount, value->mTotalAdvance, paint->getTextSize());
#endif
}

void TextLayoutShaper::computeValues(const SkPaint* paint, const UChar* chars,
        size_t start, size_t count, size_t contextCount, int dirFlags,
        Vector<jfloat>* const outAdvances, jfloat* outTotalAdvance,
        Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos) {
        *outTotalAdvance = 0;
        if (!count) {
            return;
        }

        if (count > 4096) {
            ALOGW("Long string, count = %d, input chars: ", count);
            ALOGW("******** start = %d: 0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x.", start,
                chars[start], chars[start + 1], chars[start + 2], chars[start + 3],
                chars[start + 4], chars[start + 5], chars[start + 6], chars[start + 7],
                chars[start + 8], chars[start + 9], chars[start + 10], chars[start + 11],
                chars[start + 12], chars[start + 13], chars[start + 14], chars[start + 15]
                );
            ALOGW("******** end = %d: 0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x0x%x.", start + count - 16,
                chars[start + count - 16], chars[start + count - 15], chars[start + count - 14], chars[start + count - 13],
                chars[start + count - 12], chars[start + count - 11], chars[start + count - 10], chars[start + count - 9],
                chars[start + count - 8], chars[start + count - 7], chars[start + count - 6], chars[start + count - 5],
                chars[start + count - 4], chars[start + count - 3], chars[start + count - 2], chars[start + count - 1]
                );
        }

        UBiDiLevel bidiReq = 0;
        bool forceLTR = false;
        bool forceRTL = false;

        switch (dirFlags) {
            case kBidi_LTR: bidiReq = 0; break; // no ICU constant, canonical LTR level
            case kBidi_RTL: bidiReq = 1; break; // no ICU constant, canonical RTL level
            case kBidi_Default_LTR: bidiReq = UBIDI_DEFAULT_LTR; break;
            case kBidi_Default_RTL: bidiReq = UBIDI_DEFAULT_RTL; break;
            case kBidi_Force_LTR: forceLTR = true; break; // every char is LTR
            case kBidi_Force_RTL: forceRTL = true; break; // every char is RTL
        }

        bool useSingleRun = false;
        bool isRTL = forceRTL;
        if (forceLTR || forceRTL) {
            useSingleRun = true;
        } else {
            UBiDi* bidi = ubidi_open();
            if (bidi) {
                UErrorCode status = U_ZERO_ERROR;
#if DEBUG_GLYPHS
                ALOGD("******** ComputeValues -- start");
                ALOGD("      -- string = '%s'", String8(chars + start, count).string());
                ALOGD("      -- start = %d", start);
                ALOGD("      -- count = %d", count);
                ALOGD("      -- contextCount = %d", contextCount);
                ALOGD("      -- bidiReq = %d", bidiReq);
#endif
                ubidi_setPara(bidi, chars, contextCount, bidiReq, NULL, &status);
                if (U_SUCCESS(status)) {
                    int paraDir = ubidi_getParaLevel(bidi) & kDirection_Mask; // 0 if ltr, 1 if rtl
                    ssize_t rc = ubidi_countRuns(bidi, &status);
#if DEBUG_GLYPHS
                    ALOGD("      -- dirFlags = %d", dirFlags);
                    ALOGD("      -- paraDir = %d", paraDir);
                    ALOGD("      -- run-count = %d", int(rc));
#endif
                    if (U_SUCCESS(status) && rc == 1) {
                        // Normal case: one run, status is ok
                        isRTL = (paraDir == 1);
                        useSingleRun = true;
                    } else if (!U_SUCCESS(status) || rc < 1) {
                        ALOGW("Need to force to single run -- string = '%s',"
                                " status = %d, rc = %d",
                                String8(chars + start, count).string(), status, int(rc));
                        isRTL = (paraDir == 1);
                        useSingleRun = true;
                    } else {
                        int32_t end = start + count;
                        for (size_t i = 0; i < size_t(rc); ++i) {
                            int32_t startRun = -1;
                            int32_t lengthRun = -1;
                            UBiDiDirection runDir = ubidi_getVisualRun(bidi, i, &startRun, &lengthRun);

                            if (startRun == -1 || lengthRun == -1) {
                                // Something went wrong when getting the visual run, need to clear
                                // already computed data before doing a single run pass
                                ALOGW("Visual run is not valid");
                                outGlyphs->clear();
                                outAdvances->clear();
                                outPos->clear();
                                *outTotalAdvance = 0;
                                isRTL = (paraDir == 1);
                                useSingleRun = true;
                                break;
                            }

                            if (startRun >= end) {
                                continue;
                            }
                            int32_t endRun = startRun + lengthRun;
                            if (endRun <= int32_t(start)) {
                                continue;
                            }
                            if (startRun < int32_t(start)) {
                                startRun = int32_t(start);
                            }
                            if (endRun > end) {
                                endRun = end;
                            }

                            lengthRun = endRun - startRun;
                            isRTL = (runDir == UBIDI_RTL);
#if DEBUG_GLYPHS
                            ALOGD("Processing Bidi Run = %d -- run-start = %d, run-len = %d, isRTL = %d",
                                    i, startRun, lengthRun, isRTL);
#endif
                            computeRunValues(paint, chars + startRun, lengthRun, isRTL,
                                    outAdvances, outTotalAdvance, outGlyphs, outPos);

                        }
                    }
                } else {
                    ALOGW("Cannot set Para");
                    useSingleRun = true;
                    isRTL = (bidiReq = 1) || (bidiReq = UBIDI_DEFAULT_RTL);
                }
                ubidi_close(bidi);
            } else {
                ALOGW("Cannot ubidi_open()");
                useSingleRun = true;
                isRTL = (bidiReq = 1) || (bidiReq = UBIDI_DEFAULT_RTL);
            }
        }

        // Default single run case
        if (useSingleRun){
#if DEBUG_GLYPHS
            ALOGD("Using a SINGLE BiDi Run "
                    "-- run-start = %d, run-len = %d, isRTL = %d", start, count, isRTL);
#endif
            computeRunValues(paint, chars + start, count, isRTL,
                    outAdvances, outTotalAdvance, outGlyphs, outPos);
        }

#if DEBUG_GLYPHS
        ALOGD("      -- Total returned glyphs-count = %d", outGlyphs->size());
        ALOGD("******** ComputeValues -- end");
#endif
}

void TextLayoutShaper::computeRunValues(const SkPaint* paint, const UChar* chars,
        size_t count, bool isRTL,
        Vector<jfloat>* const outAdvances, jfloat* outTotalAdvance,
        Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos) {
    if (!count) {
        // We cannot shape an empty run.
        return;
    }

    // To be filled in later
    for (size_t i = 0; i < count; i++) {
        outAdvances->add(0);
    }

    // Set the string properties
    const uint16_t *inputed_string = chars;

    // Define shaping paint properties
    mShapingPaint.setTextSize(paint->getTextSize());
    float skewX = paint->getTextSkewX();
    mShapingPaint.setTextSkewX(skewX);
    mShapingPaint.setTextScaleX(paint->getTextScaleX());
    mShapingPaint.setFlags(paint->getFlags());
    mShapingPaint.setHinting(paint->getHinting());
    mShapingPaint.setFontVariant(paint->getFontVariant());
    mShapingPaint.setLanguage(paint->getLanguage());

    // Split the BiDi run into Script runs. Harfbuzz will populate the pos, length and script
    // into the shaperItem
    ssize_t indexFontRun = isRTL ? count - 1 : 0;
    unsigned numCodePoints = 0;
    jfloat totalAdvance = *outTotalAdvance;
    ssize_t startScriptRun;
    size_t countScriptRun;
    hb_script_t script;
    while ((isRTL) ?
            hb_icu_utf16_script_run_prev(&numCodePoints, startScriptRun,
                countScriptRun, script, inputed_string, count, &indexFontRun):
            hb_icu_utf16_script_run_next(&numCodePoints, startScriptRun,
                countScriptRun, script, inputed_string, count, &indexFontRun)) {

        ssize_t endScriptRun = startScriptRun + countScriptRun;

        clearHBBuffer();
        hb_buffer_add_utf16(mHbBuffer, inputed_string, count, startScriptRun, countScriptRun);
        hb_buffer_set_script(mHbBuffer, script);

#if DEBUG_GLYPHS
        ALOGD("-------- Start of Script Run --------");
        ALOGD("Shaping Script Run with (from harfbuzz-ng)");
        ALOGD("         -- isRTL = %d", isRTL);
        ALOGD("         -- hb_script_t = 0x%x", script);
        ALOGD("         -- startFontRun = %d", int(startScriptRun));
        ALOGD("         -- endFontRun = %d", int(endScriptRun));
        ALOGD("         -- countFontRun = %d", countScriptRun);
        ALOGD("         -- indexFontRun = %d", int(indexFontRun));
        ALOGD("         -- run = '%s'", String8(chars + startScriptRun, countScriptRun).string());
        ALOGD("         -- stringLength = %d", count);
        ALOGD("         -- string = '%s'", String8(chars, count).string());
#endif

        // Get the base glyph count for offsetting the glyphIDs
        // and set the correct typeface for shaping paint.
        size_t glyphBaseCount = shapeFontRun(paint, isRTL);

#if DEBUG_GLYPHS
        ALOGD("Got from Skia");
        ALOGD("         -- glyphBaseCount = %d", glyphBaseCount);
#endif

        jfloat totalFontRunAdvance = 0;

        // Get glyph positions (and reverse them in place if RTL)
        const uint16_t *charsScriptStart = inputed_string + startScriptRun;
        ScriptRunLayoutShaper *shaper = getScriptRunLayoutShaper(script);
        totalFontRunAdvance = shaper->shapeScriptRun(&mShapingPaint, charsScriptStart, countScriptRun, isRTL, outAdvances,
                outGlyphs, outPos, totalAdvance, startScriptRun, glyphBaseCount);

        totalAdvance += totalFontRunAdvance;
    }

    *outTotalAdvance = totalAdvance;

#if DEBUG_GLYPHS
    ALOGD("-------- End of Script Run --------");
#endif
}

/** M: Add API to get HBScript from HB-Ng script @{
 *  We will transform the hb_scipt_t passed in to HB_Script,
 * */

struct HBNgScriptHBScriptMapping {
    hb_script_t hbNgScript;
    HB_Script hbScript;
};

static HBNgScriptHBScriptMapping ScriptMappingArray[] {
    {HB_SCRIPT_ARMENIAN,      HB_Script_Armenian},
    {HB_SCRIPT_HEBREW,        HB_Script_Hebrew},
    {HB_SCRIPT_ARABIC,        HB_Script_Arabic},
    {HB_SCRIPT_SYRIAC,        HB_Script_Syriac},
    {HB_SCRIPT_THAANA,        HB_Script_Thaana},
    {HB_SCRIPT_NKO,           HB_Script_Nko},
    {HB_SCRIPT_DEVANAGARI,    HB_Script_Devanagari},
    {HB_SCRIPT_BENGALI,       HB_Script_Bengali},
    {HB_SCRIPT_GURMUKHI,      HB_Script_Gurmukhi},
    {HB_SCRIPT_GUJARATI,      HB_Script_Gujarati},
    {HB_SCRIPT_ORIYA,         HB_Script_Oriya},
    {HB_SCRIPT_TAMIL,         HB_Script_Tamil},
    {HB_SCRIPT_TELUGU,        HB_Script_Telugu},
    {HB_SCRIPT_KANNADA,       HB_Script_Kannada},
    {HB_SCRIPT_MALAYALAM,     HB_Script_Malayalam},
    {HB_SCRIPT_SINHALA,       HB_Script_Sinhala},
    {HB_SCRIPT_THAI,          HB_Script_Thai},
    {HB_SCRIPT_LAO,           HB_Script_Lao},
    {HB_SCRIPT_TIBETAN,       HB_Script_Tibetan},
    {HB_SCRIPT_MYANMAR,       HB_Script_Myanmar},
    {HB_SCRIPT_GEORGIAN,      HB_Script_Georgian},
    // we don't currently support HB_Script_Ethiopic, it is a placeholder for an upstream merge
    //{HB_Script_Ethiopic,    0x1200},
    {HB_SCRIPT_OGHAM,         HB_Script_Ogham},
    {HB_SCRIPT_RUNIC,         HB_Script_Runic},
    {HB_SCRIPT_KHMER,         HB_Script_Khmer},
};

static HB_Script getHBScriptFromHBNgScript(hb_script_t script){
    HB_Script hb_script = HB_Script_Common;

    int scriptCount = sizeof(ScriptMappingArray) / sizeof(HBNgScriptHBScriptMapping);
    for (int i = 0; i < scriptCount; ++ i){
        if(ScriptMappingArray[i].hbNgScript == script) {
            hb_script = ScriptMappingArray[i].hbScript;
            break;
        }
    }

    return hb_script;
}

/** @} */

/**
 * Return the first typeface in the logical change, starting with this typeface,
 * that contains the specified unichar, or NULL if none is found.
 * 
 * Note that this function does _not_ increment the reference count on the typeface, as the
 * assumption is that its lifetime is managed elsewhere - in particular, the fallback typefaces
 * for the default font live in a global cache.
 */
SkTypeface* TextLayoutShaper::typefaceForScript(const SkPaint* paint, SkTypeface* typeface,
        hb_script_t script) {
    SkTypeface::Style currentStyle = SkTypeface::kNormal;
    if (typeface) {
        currentStyle = typeface->style();
    }
    HB_Script shapingScript = getHBScriptFromHBNgScript(script);
    typeface = SkCreateTypefaceForScript(shapingScript, currentStyle);
#if DEBUG_GLYPHS
    ALOGD("Using Harfbuzz-ng Script 0x%x, Style %d => Typeface: %p", script, currentStyle, typeface);
#endif
    return typeface;
}

bool TextLayoutShaper::isComplexScript(hb_script_t script) {
    HB_Script shapingScript = getHBScriptFromHBNgScript(script);
    switch (shapingScript) {
    case HB_Script_Common:
        return false;
    default:
        return true;
    }
}

size_t TextLayoutShaper::shapeFontRun(const SkPaint* paint, bool isRTL) {
    SkTypeface* typeface = paint->getTypeface();

#if DEBUG_GLYPHS
    unsigned int countChars = hb_buffer_get_length (mHbBuffer);
    hb_glyph_info_t *infos = hb_buffer_get_glyph_infos (mHbBuffer, NULL);
    ALOGD ("Input String for Harfbuzz-ng(script): %d\n", countChars);
    for (unsigned int i = 0; i < countChars; i++) {
        hb_glyph_info_t *info = &infos[i];
        ALOGD ("          -- codepoint[%d]: 0x%x", i, info->codepoint);
    }
#endif

    hb_script_t shapingScript = hb_buffer_get_script(mHbBuffer);

    // Get the glyphs base count for offsetting the glyphIDs returned by Harfbuzz
    // This is needed as the Typeface used for shaping can be not the default one
    // when we are shaping any script that needs to use a fallback Font.
    // If we are a "common" script we dont need to shift
    size_t baseGlyphCount = 0;
    SkUnichar firstUnichar = 0;
    if (isComplexScript(shapingScript)) {
        unsigned int countUniChars = hb_buffer_get_length (mHbBuffer);
        hb_glyph_info_t *charInfos = hb_buffer_get_glyph_infos (mHbBuffer, NULL);
        hb_glyph_info_t *charInfosEnd = charInfos + countUniChars;

        firstUnichar = charInfos->codepoint;
        while (firstUnichar == ' ' && charInfos < charInfosEnd) {
            charInfos ++;
            firstUnichar = charInfos->codepoint;
        }
        baseGlyphCount = paint->getBaseGlyphCount(firstUnichar);
#if DEBUG_GLYPHS
        ALOGD("Complex Text, shapingScript: 0x%x, firstUnichar: 0x%x, baseGlyphCount: %d", shapingScript, firstUnichar, baseGlyphCount);
#endif
    }

    if (baseGlyphCount != 0) {
        typeface = typefaceForScript(paint, typeface, shapingScript);
        if (!typeface) {
            typeface = mDefaultTypeface;
            SkSafeRef(typeface);
#if DEBUG_GLYPHS
            ALOGD("Using Default Typeface");
#endif
        }
    } else {
        if (!typeface) {
            typeface = mDefaultTypeface;
#if DEBUG_GLYPHS
            ALOGD("Using Default Typeface");
#endif
        }
        SkSafeRef(typeface);
    }

    mShapingPaint.setTypeface(typeface);
    SkSafeUnref(typeface);

    return baseGlyphCount;
}

void TextLayoutShaper::purgeCaches() {
    unrefTypefaces();
    init();
}

TextLayoutEngine::TextLayoutEngine() {
    mShaper = new TextLayoutShaper();
#if USE_TEXT_LAYOUT_CACHE
    mTextLayoutCache = new TextLayoutCache(mShaper);
#else
    mTextLayoutCache = NULL;
#endif
}

TextLayoutEngine::~TextLayoutEngine() {
    delete mTextLayoutCache;
    delete mShaper;
}

sp<TextLayoutValue> TextLayoutEngine::getValue(const SkPaint* paint, const jchar* text,
        jint start, jint count, jint contextCount, jint dirFlags) {
    sp<TextLayoutValue> value;
#if USE_TEXT_LAYOUT_CACHE
    value = mTextLayoutCache->getValue(paint, text, start, count,
            contextCount, dirFlags);
    if (value == NULL) {
        ALOGE("Cannot get TextLayoutCache value for text = '%s'",
                String8(text + start, count).string());
    }
#else
    AutoMutex _l(mLock);
    value = new TextLayoutValue(count);
    mShaper->computeValues(value.get(), paint,
            reinterpret_cast<const UChar*>(text), start, count, contextCount, dirFlags);
#endif
    return value;
}

void TextLayoutEngine::purgeCaches() {
#if USE_TEXT_LAYOUT_CACHE
    mTextLayoutCache->purgeCaches();
#if DEBUG_GLYPHS
    ALOGD("Purged TextLayoutEngine caches");
#endif
#endif
}

/** M: Added API @{
 * */

void TextLayoutShaper::clearHBBuffer(void) {
    hb_buffer_reset(mHbBuffer);
    hb_unicode_funcs_t * unicode_funcs = hb_icu_get_unicode_funcs();
    hb_buffer_set_unicode_funcs(mHbBuffer, unicode_funcs);
}

ScriptRunLayoutShaper *TextLayoutShaper::getScriptRunLayoutShaper(hb_script_t script) {
    HarfbuzzNgShaper &harfbuzzNgShaper = HarfbuzzNgShaper::getInstance();
    HarfbuzzShaper &harfbuzzShaper = HarfbuzzShaper::getInstance();

    if (!isComplexScript(script)) {
        harfbuzzShaper.setShapingScript(getHBScriptFromHBNgScript(script));
        return &harfbuzzShaper;
    }

    switch (script) {
    case HB_SCRIPT_MYANMAR:
        return &GraphiteLayoutShaper::getInstance();

    case HB_SCRIPT_BENGALI:
        harfbuzzShaper.setShapingScript(getHBScriptFromHBNgScript(script));
        return &harfbuzzShaper;

    default:
        harfbuzzNgShaper.setShapingScript(script);
        return &harfbuzzNgShaper;
    }
}

/** @} */

} // namespace android
