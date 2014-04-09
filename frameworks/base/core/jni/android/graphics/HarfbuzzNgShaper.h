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

#ifndef HARFBUZZNGLAYOUTENGINE_H_
#define HARFBUZZNGLAYOUTENGINE_H_

#include "ScriptRunLayoutShaper.h"

#include "SkTypeface.h"

#include <utils/KeyedVector.h>
#include <utils/Singleton.h>

#include "hb.h"

namespace android {

class HarfbuzzNgShaper : public Singleton<HarfbuzzNgShaper>, public ScriptRunLayoutShaper {
public:
    HarfbuzzNgShaper();
    ~HarfbuzzNgShaper();

    void setShapingScript(hb_script_t script);

    virtual void purgeCaches();

    virtual jfloat shapeScriptRun(const SkPaint* paint, const UChar* chars,
                size_t count, bool isRTL, Vector<jfloat>* const outAdvances,
                Vector<jchar>* const outGlyphs, Vector<jfloat>* const outPos,
                jfloat startXPosition, uint32_t startScriptRun, size_t glyphBaseCount);


private:
    hb_face_t * getCachedHBNgFace(SkTypeface* typeface);
    void clearHBBuffer(void);
    bool isRtlScript(hb_script_t script);
    void prepareBuffer(const UChar* chars, size_t count);
    hb_font_t *getHBNgFont(const SkPaint* paint);

    hb_buffer_t *mHbBuffer;
    hb_script_t mShapingScript;
    /**
     * Cache of hb_face_t
     */
    KeyedVector<SkFontID, hb_face_t*> mCachedHBNgFaces;
};

}

#endif /* HARFBUZZNGLAYOUTENGINE_H_ */
