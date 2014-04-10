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

#include "config.h"
#include "ColorInputType.h"

#include "HTMLInputElement.h"
#include <wtf/PassOwnPtr.h>
#include <wtf/text/WTFString.h>

/// M: Color Input Support @{
#include "Chrome.h"
#include "MouseEvent.h"
#include "ScriptController.h"
#include "RenderObject.h"
#include "RenderStyle.h"
/// @}

namespace WebCore {

static bool isValidColorString(const String& value)
{
    if (value.isEmpty())
        return false;
    if (value[0] != '#')
        return false;

    // We don't accept #rgb and #aarrggbb formats.
    if (value.length() != 7)
        return false;
    Color color(value);
    return color.isValid() && !color.hasAlpha();
}

PassOwnPtr<InputType> ColorInputType::create(HTMLInputElement* element)
{
    return adoptPtr(new ColorInputType(element));
}

/// M: Color Input Support @{
ColorInputType::~ColorInputType()
{
}

bool ColorInputType::isColorControl() const
{
    return true;
}
/// @}

const AtomicString& ColorInputType::formControlType() const
{
    return InputTypeNames::color();
}

bool ColorInputType::typeMismatchFor(const String& value) const
{
    // FIXME: Should not accept an empty value. Remove it when we implement value
    // sanitization for type=color.
    if (value.isEmpty())
        return false;
    return !isValidColorString(value);
}

bool ColorInputType::typeMismatch() const
{
    // FIXME: Should return false. We don't implement value sanitization for
    // type=color yet.
    String value = element()->value();
    return !value.isEmpty() && !isValidColorString(value);
}

bool ColorInputType::supportsRequired() const
{
    return false;
}

/// M: Color input support @{
String ColorInputType::fallbackValue()
{
    //To pass html5test.com made changes to return null string instead of simple color
    return String(" ");
}

String ColorInputType::sanitizeValue(const String& proposedValue)
{
    if (!isValidColorString(proposedValue))
        return fallbackValue();

    return proposedValue.lower();
}

void ColorInputType::handleDOMActivateEvent(Event* event)
{
    if (element()->disabled() || element()->readOnly() || !element()->renderer())
        return;

    if (!ScriptController::processingUserGesture())
        return;

    IntRect absBounds = element()->renderer()->absoluteBoundingBoxRect();
    Chrome* chrome = this->chrome();
    if (chrome)
        chrome->createColorChooser(this, absBounds);

    event->setDefaultHandled();
}

Color* ColorInputType::getColor() {
    return &m_color;
}

void ColorInputType::didChooseColor(const Color& color)
{
    if (element()->disabled() || element()->readOnly())
        return;
    element()->setValueFromRenderer(color.serialized());
    element()->renderer()->style()->setBackgroundColor(color);
    element()->renderer()->node()->clearNeedsStyleRecalc();
    element()->dispatchFormControlChangeEvent();
    m_color = color;
}
/// @}

} // namespace WebCore