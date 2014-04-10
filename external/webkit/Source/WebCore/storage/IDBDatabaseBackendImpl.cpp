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
#include "IDBDatabaseBackendImpl.h"

#if ENABLE(INDEXED_DATABASE)

#include "CrossThreadTask.h"
#include "DOMStringList.h"
#include "IDBBackingStore.h"
#include "IDBDatabaseException.h"
#include "IDBFactoryBackendImpl.h"
#include "IDBObjectStoreBackendImpl.h"
#include "IDBTransactionBackendImpl.h"
#include "IDBTransactionCoordinator.h"

/// M: add header file @{
#include <wtf/text/StringConcatenate.h>
/// @}

namespace WebCore {

class IDBDatabaseBackendImpl::PendingSetVersionCall : public RefCounted<PendingSetVersionCall> {
public:
    static PassRefPtr<PendingSetVersionCall> create(const String& version, PassRefPtr<IDBCallbacks> callbacks, PassRefPtr<IDBDatabaseCallbacks> databaseCallbacks)
    {
        return adoptRef(new PendingSetVersionCall(version, callbacks, databaseCallbacks));
    }
    String version() { return m_version; }
    PassRefPtr<IDBCallbacks> callbacks() { return m_callbacks; }
    PassRefPtr<IDBDatabaseCallbacks> databaseCallbacks() { return m_databaseCallbacks; }

private:
    PendingSetVersionCall(const String& version, PassRefPtr<IDBCallbacks> callbacks, PassRefPtr<IDBDatabaseCallbacks> databaseCallbacks)
        : m_version(version)
        , m_callbacks(callbacks)
        , m_databaseCallbacks(databaseCallbacks)
    {
    }
    String m_version;
    RefPtr<IDBCallbacks> m_callbacks;
    RefPtr<IDBDatabaseCallbacks> m_databaseCallbacks;
};

/// M: create a class for pending delete calls @{
class IDBDatabaseBackendImpl::PendingDeleteCall : public RefCounted<PendingDeleteCall> {
public:
    static PassRefPtr<PendingDeleteCall> create(PassRefPtr<IDBCallbacks> callbacks)
    {
        return adoptRef(new PendingDeleteCall(callbacks));
    }
    PassRefPtr<IDBCallbacks> callbacks() { return m_callbacks; }

private:
    PendingDeleteCall(PassRefPtr<IDBCallbacks> callbacks)
        : m_callbacks(callbacks)
    {
    }
    RefPtr<IDBCallbacks> m_callbacks;
};
/// @}

IDBDatabaseBackendImpl::IDBDatabaseBackendImpl(const String& name, IDBBackingStore* backingStore, IDBTransactionCoordinator* coordinator, IDBFactoryBackendImpl* factory, const String& uniqueIdentifier)
    : m_backingStore(backingStore)
    , m_id(InvalidId)
    , m_name(name)
    , m_version("")
    , m_identifier(uniqueIdentifier)
    , m_factory(factory)
    , m_transactionCoordinator(coordinator)
{
    /// M: initialize prevObjectStoreId and prevTimestamp @{
    m_prevObjectStoreId = -1;
    time_t now_t = time(NULL);
    getLocalTime(&now_t, &m_prevTimestamp);
    /// @}

    /// M: initialize upgradeNeeded to false.
    m_upgradeNeeded = false;

    ASSERT(!m_name.isNull());

    bool success = m_backingStore->extractIDBDatabaseMetaData(m_name, m_version, m_id);
    ASSERT_UNUSED(success, success == (m_id != InvalidId));
    if (!m_backingStore->setIDBDatabaseMetaData(m_name, m_version, m_id, m_id == InvalidId))
        ASSERT_NOT_REACHED(); // FIXME: Need better error handling.
    loadObjectStores();
}

IDBDatabaseBackendImpl::~IDBDatabaseBackendImpl()
{
    m_factory->removeIDBDatabaseBackend(m_identifier);
}

PassRefPtr<IDBBackingStore> IDBDatabaseBackendImpl::backingStore() const
{
    return m_backingStore;
}

PassRefPtr<DOMStringList> IDBDatabaseBackendImpl::objectStoreNames() const
{
    RefPtr<DOMStringList> objectStoreNames = DOMStringList::create();
    for (ObjectStoreMap::const_iterator it = m_objectStores.begin(); it != m_objectStores.end(); ++it)
        objectStoreNames->append(it->first);
    return objectStoreNames.release();
}

PassRefPtr<IDBObjectStoreBackendInterface> IDBDatabaseBackendImpl::createObjectStore(const String& name, const String& keyPath, bool autoIncrement, IDBTransactionBackendInterface* transactionPtr, ExceptionCode& ec)
{
    ASSERT(transactionPtr->mode() == IDBTransaction::VERSION_CHANGE);

    if (m_objectStores.contains(name)) {
        ec = IDBDatabaseException::CONSTRAINT_ERR;
        return 0;
    }

    RefPtr<IDBObjectStoreBackendImpl> objectStore = IDBObjectStoreBackendImpl::create(m_backingStore.get(), m_id, name, keyPath, autoIncrement);
    ASSERT(objectStore->name() == name);

    RefPtr<IDBDatabaseBackendImpl> database = this;
    RefPtr<IDBTransactionBackendInterface> transaction = transactionPtr;
    if (!transaction->scheduleTask(createCallbackTask(&IDBDatabaseBackendImpl::createObjectStoreInternal, database, objectStore, transaction),
                                   createCallbackTask(&IDBDatabaseBackendImpl::removeObjectStoreFromMap, database, objectStore))) {
        ec = IDBDatabaseException::NOT_ALLOWED_ERR;
        return 0;
    }

    m_objectStores.set(name, objectStore);
    return objectStore.release();
}

void IDBDatabaseBackendImpl::createObjectStoreInternal(ScriptExecutionContext*, PassRefPtr<IDBDatabaseBackendImpl> database, PassRefPtr<IDBObjectStoreBackendImpl> objectStore,  PassRefPtr<IDBTransactionBackendInterface> transaction)
{
    int64_t objectStoreId;

    if (!database->m_backingStore->createObjectStore(database->id(), objectStore->name(), objectStore->keyPath(), objectStore->autoIncrement(), objectStoreId)) {
        transaction->abort();
        return;
    }

    objectStore->setId(objectStoreId);
    transaction->didCompleteTaskEvents();
}

PassRefPtr<IDBObjectStoreBackendInterface> IDBDatabaseBackendImpl::objectStore(const String& name)
{
    /// M: update object store timestamp when it is being accessed @{
    RefPtr<IDBObjectStoreBackendImpl> objectStore = m_objectStores.get(name);

    if (!objectStore)
        return 0;

    int64_t objectStoreId = objectStore->id();

    struct tm now;
    time_t now_t = time(NULL);
    getLocalTime(&now_t, &now);

    // If it has already been updated, do not trigger again
    if (objectStoreId == m_prevObjectStoreId) {
        if (now.tm_year != m_prevTimestamp.tm_year
            || now.tm_mon != m_prevTimestamp.tm_mon
            || now.tm_mday != m_prevTimestamp.tm_mday) {

            m_backingStore->updateObjectStoreTimestamp(objectStoreId);
        }
    } else {
        m_prevObjectStoreId = objectStoreId;
        m_backingStore->updateObjectStoreTimestamp(objectStoreId);
    }

    m_prevTimestamp.tm_year = now.tm_year;
    m_prevTimestamp.tm_mon = now.tm_mon;
    m_prevTimestamp.tm_mday = now.tm_mday;

    return objectStore.release();
    /// @}
}

void IDBDatabaseBackendImpl::deleteObjectStore(const String& name, IDBTransactionBackendInterface* transactionPtr, ExceptionCode& ec)
{
    RefPtr<IDBObjectStoreBackendImpl> objectStore = m_objectStores.get(name);
    if (!objectStore) {
        ec = IDBDatabaseException::NOT_FOUND_ERR;
        return;
    }
    RefPtr<IDBDatabaseBackendImpl> database = this;
    RefPtr<IDBTransactionBackendInterface> transaction = transactionPtr;
    if (!transaction->scheduleTask(createCallbackTask(&IDBDatabaseBackendImpl::deleteObjectStoreInternal, database, objectStore, transaction),
                                   createCallbackTask(&IDBDatabaseBackendImpl::addObjectStoreToMap, database, objectStore))) {
        ec = IDBDatabaseException::NOT_ALLOWED_ERR;
        return;
    }
    m_objectStores.remove(name);
}

void IDBDatabaseBackendImpl::deleteObjectStoreInternal(ScriptExecutionContext*, PassRefPtr<IDBDatabaseBackendImpl> database, PassRefPtr<IDBObjectStoreBackendImpl> objectStore, PassRefPtr<IDBTransactionBackendInterface> transaction)
{
    database->m_backingStore->deleteObjectStore(database->id(), objectStore->id());
    transaction->didCompleteTaskEvents();
}

/// M: rename parameter 'version' to 'versionStr'
void IDBDatabaseBackendImpl::setVersion(const String& versionStr, PassRefPtr<IDBCallbacks> prpCallbacks, PassRefPtr<IDBDatabaseCallbacks> prpDatabaseCallbacks, ExceptionCode& ec)
{
    RefPtr<IDBCallbacks> callbacks = prpCallbacks;
    RefPtr<IDBDatabaseCallbacks> databaseCallbacks = prpDatabaseCallbacks;

    if (!m_databaseCallbacksSet.contains(databaseCallbacks)) {
        callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::ABORT_ERR, "Connection was closed before set version transaction was created"));
        return;
    }

    /// M: handle callbacks in different ways when database is opened with the specified version:
    /// 1. less than the database version: return onError callback.
    /// 2. greater than the database version: return onUpgradeNeeded callback and then onSuccess callback.
    /// 3. equal to the database version: return onSuccess callback.
    /// @{

    // If the database was deleted then re-opened immediately, we need to create the database in the backing store.
    if (m_id == InvalidId) {
        m_version = "";
        m_backingStore->setIDBDatabaseMetaData(m_name, m_version, m_id, m_id == InvalidId);
    }

    // Keep version in a non-constant variable
    String version = versionStr;

    if (version == "-1") {
        if (m_version.isEmpty()) { // new database
            version = "1";
        } else {
            callbacks->onSuccess(this);
            return;
        }
    }

    m_upgradeNeeded = false;
    double oldVersion;

    if (m_version.isEmpty())
        oldVersion = 0;
    else
        oldVersion = m_version.toDouble();

    double newVersion = version.toDouble();

    if (newVersion < oldVersion) {
        // Remove the redundant callback from m_databaseCallbacksSet which was being added in 'initOpen' function.
        m_databaseCallbacksSet.remove(databaseCallbacks);
        callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::VERSION_ERR, "The specified version is less than the existing version"));
        return;
    }

    if (newVersion == oldVersion) {
        // Remove the redundant callback from m_databaseCallbacksSet which was being added in 'initOpen' function. The callback will be added again in 'onSuccess' callback.
        m_databaseCallbacksSet.remove(databaseCallbacks);
        callbacks->onSuccess(this);
        return;
    }

    if (newVersion > oldVersion)
        m_upgradeNeeded = true;
    /// @}

    for (DatabaseCallbacksSet::const_iterator it = m_databaseCallbacksSet.begin(); it != m_databaseCallbacksSet.end(); ++it) {
        if (*it != databaseCallbacks)
            (*it)->onVersionChange(version);
    }

    if (m_databaseCallbacksSet.size() > 1) {
        callbacks->onBlocked();
        RefPtr<PendingSetVersionCall> pendingSetVersionCall = PendingSetVersionCall::create(version, callbacks, databaseCallbacks);
        m_pendingSetVersionCalls.append(pendingSetVersionCall);
        return;
    }

    RefPtr<DOMStringList> objectStoreNames = DOMStringList::create();
    RefPtr<IDBDatabaseBackendImpl> database = this;
    RefPtr<IDBTransactionBackendInterface> transaction = IDBTransactionBackendImpl::create(objectStoreNames.get(), IDBTransaction::VERSION_CHANGE, this);
    if (!transaction->scheduleTask(createCallbackTask(&IDBDatabaseBackendImpl::setVersionInternal, database, version, callbacks, transaction),
                                   createCallbackTask(&IDBDatabaseBackendImpl::resetVersion, database, m_version))) {
        ec = IDBDatabaseException::NOT_ALLOWED_ERR;
    }
}

/// M: rename parameter 'callbacks' to 'prpCallbacks'
void IDBDatabaseBackendImpl::setVersionInternal(ScriptExecutionContext*, PassRefPtr<IDBDatabaseBackendImpl> database, const String& version, PassRefPtr<IDBCallbacks> prpCallbacks, PassRefPtr<IDBTransactionBackendInterface> transaction)
{
    /// M: convert to RefPtr
    RefPtr<IDBCallbacks> callbacks = prpCallbacks;

    int64_t databaseId = database->id();
    database->m_version = version;
    if (!database->m_backingStore->setIDBDatabaseMetaData(database->m_name, database->m_version, databaseId, databaseId == InvalidId)) {
        // FIXME: The Indexed Database specification does not have an error code dedicated to I/O errors.
        callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::UNKNOWN_ERR, "Error writing data to stable storage."));
        transaction->abort();
        return;
    }

    /// M: trigger onUpgradeNeeded callback if version upgrade is needed @{
    if (database->m_upgradeNeeded) {
        ASSERT(!database->m_pendingSecondHalfOpen);
        database->m_pendingSecondHalfOpen  = PendingSetVersionCall::create(version, callbacks, database->getDatabaseCallbacks());
        callbacks->onUpgradeNeeded(transaction);
        return;
    }
    /// @}

    callbacks->onSuccess(transaction);
}

/// M: process m_pendingSecondHalfOpen call when transaction finished and abort fired @{
void IDBDatabaseBackendImpl::transactionFinishedAndAbortFired(PassRefPtr<IDBTransactionBackendImpl> prpTransaction)
{
    RefPtr<IDBTransactionBackendImpl> transaction = prpTransaction;
    if (transaction->mode() == IDBTransaction::VERSION_CHANGE) {
        if (m_pendingSecondHalfOpen) {
            m_pendingSecondHalfOpen->callbacks()->onError(IDBDatabaseError::create(IDBDatabaseException::ABORT_ERR, "Version change transaction was aborted in upgradeneeded event handler."));
            m_pendingSecondHalfOpen.release();
        }

        processPendingCalls();
    }
}
/// @}

/// M: process m_pendingSecondHalfOpen call when transaction finished and complete fired @{
void IDBDatabaseBackendImpl::transactionFinishedAndCompleteFired(PassRefPtr<IDBTransactionBackendImpl> prpTransaction)
{
    RefPtr<IDBTransactionBackendImpl> transaction = prpTransaction;
    if (transaction->mode() == IDBTransaction::VERSION_CHANGE) {
        processPendingCalls();
    }
}
/// @}

/// M: fire onSuccess event after onUpgradeNeeded event is fired @{
void IDBDatabaseBackendImpl::processPendingCalls()
{
    if (m_pendingSecondHalfOpen) {
        ASSERT(m_pendingSecondHalfOpen->version() == m_version);
        ASSERT(m_id != InvalidId);
        m_pendingSecondHalfOpen->callbacks()->onSuccess(this);
        m_pendingSecondHalfOpen.release();
    }
}
/// @}

PassRefPtr<IDBTransactionBackendInterface> IDBDatabaseBackendImpl::transaction(DOMStringList* objectStoreNames, unsigned short mode, ExceptionCode& ec)
{
    for (size_t i = 0; i < objectStoreNames->length(); ++i) {
        if (!m_objectStores.contains(objectStoreNames->item(i))) {
            ec = IDBDatabaseException::NOT_FOUND_ERR;
            return 0;
        }
    }

    // FIXME: Return not allowed err if close has been called.
    return IDBTransactionBackendImpl::create(objectStoreNames, mode, this);
}

void IDBDatabaseBackendImpl::open(PassRefPtr<IDBDatabaseCallbacks> callbacks)
{
    m_databaseCallbacksSet.add(RefPtr<IDBDatabaseCallbacks>(callbacks));
}

/// M: implement deleteDatabse function @{
void IDBDatabaseBackendImpl::deleteDatabase(PassRefPtr<IDBCallbacks> callbacks)
{
    if (m_backingStore) {

        // Make sure all databases are closed.
        if (m_databaseCallbacksSet.size() > 0) {
            callbacks->onBlocked();
            RefPtr<PendingDeleteCall> pendingDeleteCall = PendingDeleteCall::create(callbacks);
            m_pendingDeleteCalls.append(pendingDeleteCall);
            return;
        }

        if (!m_backingStore->deleteDatabase(m_name)) {
            callbacks->onError(IDBDatabaseError::create(IDBDatabaseException::UNKNOWN_ERR, "Cannot delete database."));
            return;
        }

        m_objectStores.clear();
        m_id = InvalidId;

        callbacks->onSuccess(SerializedScriptValue::undefinedValue());
    }
}
/// @}

void IDBDatabaseBackendImpl::close(PassRefPtr<IDBDatabaseCallbacks> prpCallbacks)
{
    RefPtr<IDBDatabaseCallbacks> callbacks = prpCallbacks;
    ASSERT(m_databaseCallbacksSet.contains(callbacks));
    m_databaseCallbacksSet.remove(callbacks);

    /// M: check pending second half open calls @{
    if (m_pendingSecondHalfOpen && m_pendingSecondHalfOpen->databaseCallbacks() == callbacks) {
        m_pendingSecondHalfOpen->callbacks()->onError(IDBDatabaseError::create(IDBDatabaseException::ABORT_ERR, "The connection was closed."));
        m_pendingSecondHalfOpen.release();
    }
    /// @}

    if (m_databaseCallbacksSet.size() > 1)
        return;

    /// M: check if there still exists pendingDeleteCalls and connections to database.
    if (!m_pendingDeleteCalls.isEmpty() && m_databaseCallbacksSet.size() > 0)
        return;

    /// M: add checking for pending delete calls @{
    while (!m_pendingDeleteCalls.isEmpty()) {
        RefPtr<PendingDeleteCall> pendingDeleteCall = m_pendingDeleteCalls.takeFirst();
        deleteDatabase(pendingDeleteCall->callbacks());
    }
    /// @}

    while (!m_pendingSetVersionCalls.isEmpty()) {
        ExceptionCode ec = 0;
        RefPtr<PendingSetVersionCall> pendingSetVersionCall = m_pendingSetVersionCalls.takeFirst();
        setVersion(pendingSetVersionCall->version(), pendingSetVersionCall->callbacks(), pendingSetVersionCall->databaseCallbacks(), ec);
        ASSERT(!ec);
    }
}

void IDBDatabaseBackendImpl::loadObjectStores()
{
    Vector<int64_t> ids;
    Vector<String> names;
    Vector<String> keyPaths;
    Vector<bool> autoIncrementFlags;

    /// M: add one more field to get database version
    m_backingStore->getObjectStores(m_id, ids, names, keyPaths, autoIncrementFlags, m_version);

    ASSERT(names.size() == ids.size());
    ASSERT(keyPaths.size() == ids.size());
    ASSERT(autoIncrementFlags.size() == ids.size());

    for (size_t i = 0; i < ids.size(); i++)
        m_objectStores.set(names[i], IDBObjectStoreBackendImpl::create(m_backingStore.get(), m_id, ids[i], names[i], keyPaths[i], autoIncrementFlags[i]));
}

void IDBDatabaseBackendImpl::removeObjectStoreFromMap(ScriptExecutionContext*, PassRefPtr<IDBDatabaseBackendImpl> database, PassRefPtr<IDBObjectStoreBackendImpl> objectStore)
{
    ASSERT(database->m_objectStores.contains(objectStore->name()));
    database->m_objectStores.remove(objectStore->name());
}

void IDBDatabaseBackendImpl::addObjectStoreToMap(ScriptExecutionContext*, PassRefPtr<IDBDatabaseBackendImpl> database, PassRefPtr<IDBObjectStoreBackendImpl> objectStore)
{
    RefPtr<IDBObjectStoreBackendImpl> objectStorePtr = objectStore;
    ASSERT(!database->m_objectStores.contains(objectStorePtr->name()));
    database->m_objectStores.set(objectStorePtr->name(), objectStorePtr);
}

void IDBDatabaseBackendImpl::resetVersion(ScriptExecutionContext*, PassRefPtr<IDBDatabaseBackendImpl> database, const String& version)
{
    database->m_version = version;
}


} // namespace WebCore

#endif // ENABLE(INDEXED_DATABASE)
