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

#ifndef IDBKey_h
#define IDBKey_h

#if ENABLE(INDEXED_DATABASE)

#include "PlatformString.h"
#include <wtf/Forward.h>
#include <wtf/Threading.h>

/// M: add header file @{
#include <wtf/text/StringConcatenate.h>
/// @}

namespace WebCore {

class IDBKey : public ThreadSafeRefCounted<IDBKey> {
public:

    /// M: add array type
    typedef Vector<RefPtr<IDBKey> > KeyArray;
    static PassRefPtr<IDBKey> createNull()
    {
        RefPtr<IDBKey> idbKey(new IDBKey());
        idbKey->m_type = NullType;
        return idbKey.release();
    }

    static PassRefPtr<IDBKey> createNumber(double number)
    {
        RefPtr<IDBKey> idbKey(new IDBKey());
        idbKey->m_type = NumberType;
        idbKey->m_number = number;
        return idbKey.release();
    }

    static PassRefPtr<IDBKey> createString(const String& string)
    {
        RefPtr<IDBKey> idbKey(new IDBKey());
        idbKey->m_type = StringType;
        idbKey->m_string = string;
        return idbKey.release();
    }

    static PassRefPtr<IDBKey> createDate(double date)
    {
        RefPtr<IDBKey> idbKey(new IDBKey());
        idbKey->m_type = DateType;
        idbKey->m_date = date;
        return idbKey.release();
    }

    /// M: create array @{
    static PassRefPtr<IDBKey> createArray(KeyArray& array)
    {
        RefPtr<IDBKey> idbKey(new IDBKey());
        idbKey->m_type = ArrayType;
        idbKey->m_array = array;
        return idbKey.release();
    }
    /// @}

    ~IDBKey();

    // In order of the least to the highest precedent in terms of sort order.
    enum Type {
        NullType = 0, // FIXME: Phase out support for null keys.
        ArrayType, /// M: add array type
        StringType,
        DateType,
        NumberType
    };

    Type type() const { return m_type; }

    /// M: check if key is valid
    bool isValid() const;

    /// M: add array type @{
    const KeyArray& array() const
    {
        ASSERT(m_type == ArrayType);
        return m_array;
    }
    /// @}

    /// M: convert array to string @{
    String arrayToString() const
    {
        ASSERT(m_type == ArrayType);
        String arrayStr = "";

        for (size_t i = 0; i < m_array.size(); ++i) {
            RefPtr<IDBKey> item = m_array[i];

            if (item->type() == IDBKey::ArrayType) {
                arrayStr = arrayStr + item->arrayToString();
            } else if (item->type() == IDBKey::StringType) {
                arrayStr = arrayStr + item->string();
            } else if (item->type() == IDBKey::DateType) {
                arrayStr = arrayStr + makeString(String::number(item->date()));
            } else if (item->type() == IDBKey::NumberType) {
                arrayStr = arrayStr + makeString(String::number(item->number()));
            }
        }

        return arrayStr;
    }
    /// @}

    const String& string() const
    {
        ASSERT(m_type == StringType);
        return m_string;
    }

    double date() const
    {
        ASSERT(m_type == DateType);
        return m_date;
    }

    double number() const
    {
        ASSERT(m_type == NumberType);
        return m_number;
    }

    /// M: add compare function
    int compare(const IDBKey* other) const;
    bool isLessThan(const IDBKey* other) const;
    bool isEqual(const IDBKey* other) const;

    using ThreadSafeRefCounted<IDBKey>::ref;
    using ThreadSafeRefCounted<IDBKey>::deref;

private:
    IDBKey();

    Type m_type;
    /// M: add array type
    KeyArray m_array;
    String m_string;
    double m_date;
    double m_number;
};

}

#endif // ENABLE(INDEXED_DATABASE)

#endif // IDBKey_h
