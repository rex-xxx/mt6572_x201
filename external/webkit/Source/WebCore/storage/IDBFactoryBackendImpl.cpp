/*
 * Copyright (C) 2011 Google Inc. All rights reserved.
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
#include "IDBFactoryBackendImpl.h"

#include "DOMStringList.h"
#include "IDBDatabaseBackendImpl.h"
#include "IDBDatabaseException.h"
#include "IDBLevelDBBackingStore.h"
#include "IDBSQLiteBackingStore.h"
#include "IDBTransactionCoordinator.h"
#include "SecurityOrigin.h"
#include <wtf/Threading.h>
#include <wtf/UnusedParam.h>

/// M: add header file @{
#include <wtf/text/StringConcatenate.h>
/// @}

#if ENABLE(INDEXED_DATABASE)

namespace WebCore {

IDBFactoryBackendImpl::IDBFactoryBackendImpl()
    : m_transactionCoordinator(IDBTransactionCoordinator::create())
{
}

IDBFactoryBackendImpl::~IDBFactoryBackendImpl()
{
}

void IDBFactoryBackendImpl::removeIDBDatabaseBackend(const String& uniqueIdentifier)
{
    ASSERT(m_databaseBackendMap.contains(uniqueIdentifier));
    m_databaseBackendMap.remove(uniqueIdentifier);
}

void IDBFactoryBackendImpl::addIDBBackingStore(const String& uniqueIdentifier, IDBBackingStore* backingStore)
{
    ASSERT(!m_backingStoreMap.contains(uniqueIdentifier));
    m_backingStoreMap.set(uniqueIdentifier, backingStore);
}

void IDBFactoryBackendImpl::removeIDBBackingStore(const String& uniqueIdentifier)
{
    ASSERT(m_backingStoreMap.contains(uniqueIdentifier));
    m_backingStoreMap.remove(uniqueIdentifier);
}

/// M: modify open function with no version parameter @{
void IDBFactoryBackendImpl::open(const String& name, PassRefPtr<IDBCallbacks> callbacks, PassRefPtr<SecurityOrigin> securityOrigin, Frame* frame, const String& dataDir, int64_t maximumSize, BackingStoreType backingStoreType)
{
    open(name, IDBDatabaseBackendImpl::unspecifiedVersion, callbacks, securityOrigin, frame, dataDir, maximumSize, backingStoreType);
}
/// @}

/// M: add overload 'open' function with one more parameter 'version' @{
void IDBFactoryBackendImpl::open(const String& name, int64_t version, PassRefPtr<IDBCallbacks> callbacks, PassRefPtr<SecurityOrigin> securityOrigin, Frame*, const String& dataDir, int64_t maximumSize, BackingStoreType backingStoreType)
{
    ExceptionCode ec = 0;
    String newVersion = makeString(String::number(version));

    // Original Google code start @{
    String fileIdentifier = securityOrigin->databaseIdentifier();
    String uniqueIdentifier = fileIdentifier + "@" + name;
    IDBDatabaseBackendMap::iterator it = m_databaseBackendMap.find(uniqueIdentifier);
    RefPtr<IDBDatabaseBackendImpl> databaseBackend;

    if (it == m_databaseBackendMap.end()) {
        // FIXME: Everything from now on should be done on another thread.

        RefPtr<IDBBackingStore> backingStore;
        IDBBackingStoreMap::iterator it2 = m_backingStoreMap.find(fileIdentifier);
        if (it2 != m_backingStoreMap.end())
            backingStore = it2->second;
        else {
            if (backingStoreType == DefaultBackingStore)
                backingStore = IDBSQLiteBackingStore::open(securityOrigin.get(), dataDir, maximumSize, fileIdentifier, this);
#if ENABLE(LEVELDB)
            else if (backingStoreType == LevelDBBackingStore)
                backingStore = IDBLevelDBBackingStore::open(securityOrigin.get(), dataDir, maximumSize, fileIdentifier, this);
#endif
            if (!backingStore) {
                callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::UNKNOWN_ERR, "Internal error."));
                return;
            }
        }

        databaseBackend = IDBDatabaseBackendImpl::create(name, backingStore.get(), m_transactionCoordinator.get(), this, uniqueIdentifier);

        if (databaseBackend)
            m_databaseBackendMap.set(uniqueIdentifier, databaseBackend.get());
        else
            callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::UNKNOWN_ERR, "Internal error creating database backend for indexedDB.open."));
    } else
        databaseBackend = it->second;
    // Original Google code end @}

    // Initialize open to prepare database object and return callback object before database can be used.
    RefPtr<IDBDatabaseCallbacks> databaseCallbacks = callbacks->initOpen(databaseBackend.get());

    if (databaseCallbacks)
        databaseBackend->setVersion(newVersion, callbacks, databaseCallbacks, ec);
}
/// @}

/// M: implement deleteDatabase @{
void IDBFactoryBackendImpl::deleteDatabase(const String& name, PassRefPtr<IDBCallbacks> callbacks, PassRefPtr<SecurityOrigin> securityOrigin, Frame*, const String& dataDir, int64_t maximumSize, BackingStoreType backingStoreType)
{
    String fileIdentifier = securityOrigin->databaseIdentifier();
    String uniqueIdentifier = fileIdentifier + "@" + name;
    IDBDatabaseBackendMap::iterator it = m_databaseBackendMap.find(uniqueIdentifier);

    if (it != m_databaseBackendMap.end()) {
        it->second->deleteDatabase(callbacks);
        return;
    }

    RefPtr<IDBBackingStore> backingStore;
    IDBBackingStoreMap::iterator it2 = m_backingStoreMap.find(fileIdentifier);

    if (it2 != m_backingStoreMap.end())
        backingStore = it2->second;
    else {
        if (backingStoreType == DefaultBackingStore)
            backingStore = IDBSQLiteBackingStore::open(securityOrigin.get(), dataDir, maximumSize, fileIdentifier, this);

        if (!backingStore) {
            callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::UNKNOWN_ERR, "The database is not existed for deletion."));
            return;
        }
    }

    RefPtr<IDBDatabaseBackendImpl> databaseBackend = IDBDatabaseBackendImpl::create(name, backingStore.get(), m_transactionCoordinator.get(), this, uniqueIdentifier);

    if (databaseBackend) {
        m_databaseBackendMap.set(uniqueIdentifier, databaseBackend.get());
        databaseBackend->deleteDatabase(callbacks);
        m_databaseBackendMap.remove(uniqueIdentifier);
    } else
        callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::UNKNOWN_ERR, "Internal error."));
}
/// @}

} // namespace WebCore

#endif // ENABLE(INDEXED_DATABASE)
