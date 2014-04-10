/*
 * Copyright (C) 2009 Google Inc. All rights reserved.
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

#ifndef WebSocketStreamHandleClient_h
#define WebSocketStreamHandleClient_h

#include "config.h"
#include "ChromiumIncludes.h"
#include "SocketStreamHandleBase.h"
#include "SocketStreamHost.h"

#include <platform/text/PlatformString.h>
#include <wtf/ThreadingPrimitives.h>


namespace net {
class SocketStreamJob;
class URLRequestContext;
} //namespace net


namespace WebCore {

class SocketStreamHandleClient;
class SocketStreamHost;

class SocketStreamHandlePrivate
: public base::RefCountedThreadSafe<SocketStreamHandlePrivate>

{
public:


    SocketStreamHandlePrivate(SocketStreamHandle*, const KURL&);
    virtual ~SocketStreamHandlePrivate();
   // This is called from the IO thread, and dispatches the callback to the main thread.
    // (For asynchronous calls, we just delegate to WebKit's callOnMainThread.)
    void maybeCallOnMainThread(Task* task);

    // Called by SocketStreamHost (using maybeCallOnMainThread), should be forwarded to WebCore.
    void onConnected(int);
    void onSentData(int);
    void onReceivedData(const std::vector<char>& data);
    void onClose();

    void socketError(int);

    void connect();
    int send(const char* data, int len);
    void close();

    unsigned int m_port;
    bool m_isSecure;
    std::string m_url;
    SocketStreamHandle* m_streamHandle;
private:

    friend class base::RefCountedThreadSafe<SocketStreamHandlePrivate>;
    scoped_refptr<SocketStreamHost> m_sockhost;

    int m_maxPendingSendingAllowed;
    int m_pendingAmountSent;
};

}//namespace WebCore

#endif
