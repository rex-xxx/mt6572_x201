/*
 * Copyright 2012, The Android Open Source Project
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

#define LOG_TAG "DataListImpl"

#include "config.h"

#if ENABLE(DATALIST)
#include "DataListImpl.h"

#include "HTMLInputElement.h"
#include "HTMLCollection.h"
#include "HTMLOptionElement.h"
#include "HTMLDataListElement.h"
#include "SkTDArray.h"
#include "WebViewCore.h"

namespace WebCore {

using namespace android;

// Copy from PopupMenuAndroid
//
// Convert a WTF::String into an array of characters where the first
// character represents the length, for easy conversion to java.
static uint16_t* stringConverter(const WTF::String& text)
{
    size_t length = text.length();
    uint16_t* itemName = new uint16_t[length+1];
    itemName[0] = (uint16_t)length;
    uint16_t* firstChar = &(itemName[1]);
    memcpy((void*)firstChar, text.characters(), sizeof(UChar)*length);
    return itemName;
}

DataListImpl::DataListImpl(android::WebViewCore* core)
    : m_webViewCore(core)
{
}

DataListImpl::~DataListImpl()
{
}

void DataListImpl::dataListFocused(HTMLInputElement* input)
{
    if (!input)
        return;

    SkTDArray<const uint16_t*> names;
    PassRefPtr<HTMLCollection> options =
        static_cast<HTMLDataListElement*>(input->list())->options();
    const int size = options->length();
    for (int i = 0; i < size; i++) {
        *names.append() = stringConverter(
            static_cast<HTMLOptionElement*>(options->item(i))->value());
    }
    m_webViewCore->showDataListOptions(names.begin(), size);
}

void DataListImpl::selectOption(int optionId)
{
}

} /* namespace android */

#endif  // ENABLE(DATALIST)