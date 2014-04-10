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

/// M: add constructor callback for class StorageEvent @{
#include "config.h"

#if ENABLE(DOM_STORAGE)

#include "StorageEvent.h"
#include "V8Binding.h"
#include "V8StorageEvent.h"
#include "V8BindingMacros.h"
#include "V8Storage.h"
#include "V8Utilities.h"

namespace WebCore
{

v8::Handle<v8::Value> V8StorageEvent::constructorCallback(const v8::Arguments& args)
{
    INC_STATS("DOM.StorageEvent.Constructor");

    if (!args.IsConstructCall())
        return throwError("DOM object constructor cannot be called as a function.",
                          V8Proxy::TypeError);

    // If constructorCallback() is called for allocating an object from the WebCore context and
    // trying to wrap the object for JS, return immediately.
#if ENABLE(WORKERS)
    if (AllowAllocation::current())
        return args.Holder();
#else
    if (AllowAllocation::m_current)
        return args.Holder();
#endif

    if (args.Length() < 1 || args.Length() > 2)
        return throwError("Invalid arguments", V8Proxy::SyntaxError);

    AtomicString type = AtomicString(toWebCoreString(args[0]));

    String key = "";
    String oldValue = "";
    String newValue = "";
    String url = "";
    Storage* storageArea = 0;

    if (args.Length() == 2)
    {
        v8::Local<v8::Object> storageObj = args[1]->ToObject();
        EXCEPTION_BLOCK(v8::Local<v8::Value>, keyArg, storageObj->Get(v8::String::New("key")));
        EXCEPTION_BLOCK(v8::Local<v8::Value>, oldValueArg,
                        storageObj->Get(v8::String::New("oldValue")));
        EXCEPTION_BLOCK(v8::Local<v8::Value>, newValueArg,
                        storageObj->Get(v8::String::New("newValue")));
        EXCEPTION_BLOCK(v8::Local<v8::Value>, urlArg, storageObj->Get(v8::String::New("url")));
        EXCEPTION_BLOCK(v8::Local<v8::Value>, storageAreaArg,
                        storageObj->Get(v8::String::New("storageArea")));

        key = toWebCoreString(keyArg);
        oldValue = toWebCoreString(oldValueArg);
        newValue = toWebCoreString(newValueArg);
        url = toWebCoreString(urlArg);

        if (storageAreaArg->IsObject())
        {
            v8::Handle<v8::Object> wrapper = v8::Handle<v8::Object>::Cast(storageAreaArg);
            v8::Handle<v8::Object> storage =
                V8DOMWrapper::lookupDOMWrapper(V8Storage::GetTemplate(), wrapper);
            if (!storage.IsEmpty())
                storageArea = V8Storage::toNative(storage);
        }
    }

    RefPtr<StorageEvent> storageEvent = StorageEvent::create(type, key, oldValue, newValue, url,
                                        storageArea);
    V8DOMWrapper::setDOMWrapper(args.Holder(), &info, storageEvent.get());

    // Add object to the wrapper map.
    storageEvent->ref();
    V8DOMWrapper::setJSWrapperForActiveDOMObject(storageEvent.get(),
            v8::Persistent<v8::Object>::New(args.Holder()));
    return args.Holder();
}
} // namespace WebCore

#endif // ENABLE(DOM_STORAGE)
/// @}