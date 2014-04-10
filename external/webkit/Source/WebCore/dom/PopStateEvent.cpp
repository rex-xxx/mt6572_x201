/*
 * Copyright (C) 2009 Apple Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */

#include "config.h"
#include "PopStateEvent.h"

#include "EventNames.h"

#if ENABLE(HTML5_HISTORY_API)
/// M: enable HTML5 History.
#include "History.h"
#endif

namespace WebCore {

#if ENABLE(HTML5_HISTORY_API)
/// M: enable HTML5 History. @{
PopStateEvent::PopStateEvent()
    : Event(eventNames().popstateEvent, false, true)
    , m_stateObject(0)
    , m_history(0)
{
}

PopStateEvent::PopStateEvent(PassRefPtr<SerializedScriptValue> stateObject, PassRefPtr<History> history)
    : Event(eventNames().popstateEvent, false, true)
    , m_stateObject(stateObject)
    , m_history(history)
{
}
/// @}
#endif

PopStateEvent::PopStateEvent(PassRefPtr<SerializedScriptValue> stateObject)
    : Event(eventNames().popstateEvent, false, true)
    , m_stateObject(stateObject)
{
}

PopStateEvent::~PopStateEvent()
{
}

#if ENABLE(HTML5_HISTORY_API)
/// M: enable HTML5 History. @{
PassRefPtr<PopStateEvent> PopStateEvent::create()
{
    return adoptRef(new PopStateEvent);
}

PassRefPtr<PopStateEvent> PopStateEvent::create(PassRefPtr<SerializedScriptValue> serializedState, PassRefPtr<History> history)
{
    return adoptRef(new PopStateEvent(serializedState, history));
}
/// @}
#endif

void PopStateEvent::initPopStateEvent(const AtomicString& type, bool canBubble, bool cancelable, PassRefPtr<SerializedScriptValue> stateObject)
{
    if (dispatched())
        return;
    
    initEvent(type, canBubble, cancelable);

    m_stateObject = stateObject;
}

} // namespace WebCore
