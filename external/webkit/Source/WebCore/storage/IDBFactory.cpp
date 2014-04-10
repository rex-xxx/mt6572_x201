/*
 * Copyright (C) 2010 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "IDBFactory.h"

#if ENABLE(INDEXED_DATABASE)

#include "DOMStringList.h"
#include "Document.h"
#include "ExceptionCode.h"
#include "Frame.h"
#include "GroupSettings.h"
#include "IDBDatabase.h"
#include "IDBDatabaseException.h"
#include "IDBFactoryBackendInterface.h"
#include "IDBKeyRange.h"
#include "IDBRequest.h"
#include "Page.h"
#include "PageGroup.h"

namespace WebCore {

IDBFactory::IDBFactory(IDBFactoryBackendInterface* factory)
    : m_factoryBackend(factory)
{
    // We pass a reference to this object before it can be adopted.
    relaxAdoptionRequirement();
}

IDBFactory::~IDBFactory()
{
}

/// M: check if context is valid or not @{
static bool isContextValid(ScriptExecutionContext* context)
{
    ASSERT(context->isDocument() || context->isWorkerContext());

    if (context->isDocument()) {
        Document* document = static_cast<Document*>(context);
        return document->frame() && document->page();
    }

#if !ENABLE(WORKERS)
    if (context->isWorkerContext())
        return false;
#endif

    return true;
}
/// @}

/// M: get indexedDB database path @{
static String getIndexedDBDatabasePath(ScriptExecutionContext* context)
{
    ASSERT(isContextValid(context));

    if (context->isDocument()) {
        Document* document = static_cast<Document*>(context);
        return document->page()->group().groupSettings()->indexedDBDatabasePath();
    }

#if ENABLE(WORKERS)
    OwnPtr<GroupSettings> groupSettings = GroupSettings::create();

    if (groupSettings)
        return groupSettings->indexedDBDatabasePath();
#endif

    return String();
}
/// @}

/// M: get indexedDB quota bytes @{
static int64_t getIndexedDBQuotaBytes(ScriptExecutionContext* context)
{
    ASSERT(isContextValid(context));

    if (context->isDocument()) {
        Document* document = static_cast<Document*>(context);
        return document->page()->group().groupSettings()->indexedDBQuotaBytes();
    }

#if ENABLE(WORKERS)
    OwnPtr<GroupSettings> groupSettings = GroupSettings::create();

    if (groupSettings)
        return groupSettings->indexedDBQuotaBytes();
#endif

    return 0;
}
/// @}

PassRefPtr<IDBRequest> IDBFactory::open(ScriptExecutionContext* context, const String& name, ExceptionCode& ec)
{
    // FIXME: Raise a NON_TRANSIENT_ERR if the name is invalid.

    /// M: check if context is valid or not and open a database @{
    if (!isContextValid(context))
        return 0;

    /// M: the source of the request is null for indexedDB.open()
    RefPtr<IDBRequest> request = IDBRequest::create(context, IDBAny::createNull(), 0);
    m_factoryBackend->open(name, request, context->securityOrigin(), NULL, getIndexedDBDatabasePath(context), getIndexedDBQuotaBytes(context), IDBFactoryBackendInterface::DefaultBackingStore);
    /// @}

    return request;
}

/// M: add overload 'open' function with one more parameter 'version' @{
PassRefPtr<IDBRequest> IDBFactory::open(ScriptExecutionContext* context, const String& name, int64_t version, ExceptionCode& ec)
{
    const int64_t maxJSInteger = 0x20000000000000LL - 1; // max javascipt integer

    if (version < 1 || version > maxJSInteger) {
        ec = IDBDatabaseException::TYPE_ERR;
        return 0;
    }

    if (!isContextValid(context))
        return 0;

    /// M: the source of the request is null for indexedDB.open()
    RefPtr<IDBRequest> request = IDBRequest::create(context, IDBAny::createNull(), 0);
    m_factoryBackend->open(name, version, request, context->securityOrigin(), NULL, getIndexedDBDatabasePath(context), getIndexedDBQuotaBytes(context), IDBFactoryBackendInterface::DefaultBackingStore);

    return request;
}
/// @}

/// M: implement deleteDatabase @{
PassRefPtr<IDBRequest> IDBFactory::deleteDatabase(ScriptExecutionContext* context, const String& name, ExceptionCode& ec)
{
    if (!isContextValid(context))
        return 0;

    RefPtr<IDBRequest> request = IDBRequest::create(context, IDBAny::createNull(), 0);
    m_factoryBackend->deleteDatabase(name, request, context->securityOrigin(), NULL, getIndexedDBDatabasePath(context), getIndexedDBQuotaBytes(context), IDBFactoryBackendInterface::DefaultBackingStore);

    return request;
}
/// @}

/// M: add compare function @{
short IDBFactory::cmp(PassRefPtr<IDBKey> first, PassRefPtr<IDBKey> second, ExceptionCode& ec)
{
    ASSERT(first);
    ASSERT(second);

    if (!first->isValid() || !second->isValid()) {
        ec = IDBDatabaseException::DATA_ERR;
        return 0;
    }

    return static_cast<short>(first->compare(second.get()));
}
/// @}

} // namespace WebCore

#endif // ENABLE(INDEXED_DATABASE)
