/*
 * Copyright 2006, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "Pattern.h"

#include "GraphicsContext.h"
#include "SkBitmapRef.h"
#include "SkCanvas.h"
#include "SkColorShader.h"
#include "SkShader.h"
#include "SkPaint.h"

namespace WebCore {

static SkShader::TileMode toTileMode(bool doRepeat) {
    return doRepeat ? SkShader::kRepeat_TileMode : SkShader::kClamp_TileMode;
}

void Pattern::platformDestroy()
{
    SkSafeUnref(m_pattern);
    m_pattern = 0;
}

SkShader* Pattern::platformPattern(const AffineTransform&)
{
    if (m_pattern)
        return m_pattern;

    if (!tileImage())
        return 0;
    SkBitmapRef* ref = tileImage()->nativeImageForCurrentFrame();
    /// M: fix w3c pattern relative case @{
    /* Fix case list:
     * 2d.pattern.paint.repeatx.outside.html
     * 2d.pattern.paint.repeaty.outside.html
     */
    if (!ref) {
        m_pattern = new SkColorShader(SkColorSetARGB(0, 0, 0, 0));
    } else if (m_repeatX && m_repeatY) {
        m_pattern = SkShader::CreateBitmapShader(ref->bitmap(),
                                                 SkShader::kRepeat_TileMode,
                                                 SkShader::kRepeat_TileMode);
    } else if ((m_repeatX | m_repeatY) == 0) {
        m_pattern = SkShader::CreateBitmapShader(ref->bitmap(),
                                                 SkShader::kClamp_TileMode,
                                                 SkShader::kClamp_TileMode);
    } else {
        // Skia does not have a "draw the tile only once" option. Clamp_TileMode
        // repeats the last line of the image after drawing one tile. To avoid
        // filling the space with arbitrary pixels, this workaround forces the
        // image to have a line of transparent pixels on the "repeated" edge(s),
        // thus causing extra space to be transparent filled.
        SkShader::TileMode tileModeX = m_repeatX ? SkShader::kRepeat_TileMode : SkShader::kClamp_TileMode;
        SkShader::TileMode tileModeY = m_repeatY ? SkShader::kRepeat_TileMode : SkShader::kClamp_TileMode;
        int expandW = m_repeatX ? 0 : 1;
        int expandH = m_repeatY ? 0 : 1;

        // Create a transparent bitmap 1 pixel wider and/or taller than the
        // original, then copy the orignal into it.
        // FIXME: Is there a better way to pad (not scale) an image in skia?
        SkBitmap bm2;
        bm2.setConfig(SkBitmap::kARGB_8888_Config, ref->bitmap().width() + expandW, ref->bitmap().height() + expandH);
        bm2.allocPixels();
        bm2.eraseARGB(0x00, 0x00, 0x00, 0x00);
        SkCanvas canvas(bm2);
        canvas.drawBitmap(ref->bitmap(), 0, 0);
        m_pattern = SkShader::CreateBitmapShader(bm2, tileModeX, tileModeY);
    }
    /// @}

    m_pattern->setLocalMatrix(m_patternSpaceTransformation);
    return m_pattern;
}

void Pattern::setPlatformPatternSpaceTransform()
{
    if (m_pattern)
        m_pattern->setLocalMatrix(m_patternSpaceTransformation);
}

} //namespace
