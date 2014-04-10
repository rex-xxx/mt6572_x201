/*
 * Copyright (C) 2010 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef ColorInputType_h
#define ColorInputType_h

/// M: Color Input Support @{
#include "BaseClickableWithKeyInputType.h"
#include "Color.h"
#include "ColorChooserClient.h"
#include "HTMLFormControlElement.h"
/// @}

namespace WebCore {

/// M: Color Input Support
class ColorInputType : public BaseClickableWithKeyInputType, public ColorChooserClient {
public:
    static PassOwnPtr<InputType> create(HTMLInputElement*);
/// M: Color Input Support @{
    ColorInputType();
    virtual ~ColorInputType();
    virtual Color* getColor();
    // ColorChooserClient implementation.
    virtual void didChooseColor(const Color&);
/// @}

private:
/// M: Color Input Support
    ColorInputType(HTMLInputElement* element)
    : BaseClickableWithKeyInputType(element), m_color(Color::black) {}
    virtual const AtomicString& formControlType() const;
    virtual bool typeMismatchFor(const String&) const;
    virtual bool typeMismatch() const;
    virtual bool supportsRequired() const;
/// M: Color Input Support @{
    virtual bool isColorControl() const;
    virtual String fallbackValue();
    virtual String sanitizeValue(const String&);
    virtual void handleDOMActivateEvent(Event*);
    Color m_color;
/// @}
};

} // namespace WebCore

#endif // ButtonInputType_h