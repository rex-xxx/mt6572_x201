/*
 * Copyright (C) 2010 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "DOMSettableTokenList.h"

#if ENABLE(MICRODATA)
#include "HTMLElement.h"
#include <wtf/text/StringBuilder.h>
#endif

namespace WebCore {

DOMSettableTokenList::DOMSettableTokenList()
    : m_value()
    , m_tokens()
#if ENABLE(MICRODATA)
    , m_attName(0)
    , m_observeNode(0)
#endif
{
}

DOMSettableTokenList::~DOMSettableTokenList()
{
}

const AtomicString DOMSettableTokenList::item(unsigned index) const
{
    if (index >= length())
        return AtomicString();
    return m_tokens[index];
}

bool DOMSettableTokenList::contains(const AtomicString& token, ExceptionCode& ec) const
{
    if (!validateToken(token, ec))
        return false;
    return m_tokens.contains(token);
}

void DOMSettableTokenList::add(const AtomicString& token, ExceptionCode& ec)
{
    if (!validateToken(token, ec) || m_tokens.contains(token))
        return;
    addInternal(token);
}

#if ENABLE(MICRODATA)
String DOMSettableTokenList::addTokenEx(const AtomicString& input, const AtomicString& token)
{
    if (input.isEmpty())
        return token;

    StringBuilder builder;
    builder.append(input);
    if (input[input.length()-1] != ' '
        && input[input.length()-1] != '\t'
        && input[input.length()-1] != '\r'
        && input[input.length()-1] != '\n'
        && input[input.length()-1] != '\f')
        builder.append(' ');
    builder.append(token);
    return builder.toString();
}
#endif

void DOMSettableTokenList::addInternal(const AtomicString& token)
{
#if ENABLE(MICRODATA)
    m_value = addTokenEx(m_value, token);
#else
    m_value = addToken(m_value, token);
#endif

    if (m_tokens.isNull())
        m_tokens.set(token, false);
    else
        m_tokens.add(token);
#if ENABLE(MICRODATA)
    // react to node's attribute
    if (m_observeNode && m_observeNode->isHTMLElement()) {
        HTMLElement* elem = static_cast<HTMLElement*>(m_observeNode);
        elem->setAttribute(*m_attName, AtomicString(m_value));
    }
#endif
}

void DOMSettableTokenList::remove(const AtomicString& token, ExceptionCode& ec)
{
    if (!validateToken(token, ec) || !m_tokens.contains(token))
        return;
    removeInternal(token);
}

void DOMSettableTokenList::removeInternal(const AtomicString& token)
{
    m_value = removeToken(m_value, token);
    m_tokens.remove(token);
#if ENABLE(MICRODATA)
    // react to node's attribute
    if (m_observeNode && m_observeNode->isHTMLElement()) {
        HTMLElement* elem = static_cast<HTMLElement*>(m_observeNode);
        elem->setAttribute(*m_attName, AtomicString(m_tokens.toString()));
    }
#endif
}

bool DOMSettableTokenList::toggle(const AtomicString& token, ExceptionCode& ec)
{
    if (!validateToken(token, ec))
        return false;
    if (m_tokens.contains(token)) {
        removeInternal(token);
        return false;
    }
    addInternal(token);
    return true;
}

void DOMSettableTokenList::setValue(const String& value)
{
    m_value = value;
    m_tokens.set(value, false);
}

} // namespace WebCore
