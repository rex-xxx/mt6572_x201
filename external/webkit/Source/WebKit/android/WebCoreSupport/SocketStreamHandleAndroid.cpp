/*
 * Copyright (C) 2009 Brent Fulgham.  All rights reserved.
 * Copyright (C) 2009 Google Inc.  All rights reserved.
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
#include <wtf/text/CString.h>
#include "WebUrlLoaderClient.h"
#include "SocketStreamHandle.h"

#include "KURL.h"
#include "Logging.h"
#include "SocketStreamHandleClient.h"
#include "SocketStreamHandleAndroid.h"
#include "net/url_request/url_request_context_getter.h"
#include "net/websockets/websocket_job.h"
#include "net/socket_stream/socket_stream_job.h"

#include <sys/socket.h>
#include <netinet/in.h>



#include <cutils/xlog.h>
#define XLOG_TAG "WebCore/WebSocket"


using namespace android;

namespace WebCore {


SocketStreamHandlePrivate::~SocketStreamHandlePrivate()
{
}

SocketStreamHandlePrivate::SocketStreamHandlePrivate(SocketStreamHandle* streamHandle, const KURL& url)
    :m_streamHandle(streamHandle)
    ,m_maxPendingSendingAllowed(0)
    ,m_pendingAmountSent(0)
{
    m_isSecure = url.protocolIs("wss");

    m_port = url.hasPort() ? url.port() : (m_isSecure ? 443 : 80);
    m_url = std::string(url.string().utf8().data(), url.string().utf8().length());

    m_sockhost = new SocketStreamHost(this, 0);

    XLOGD2("SocketStreamHandlePrivate, m_port = %d", m_port);

    connect();

}

namespace {
// Trampoline to wrap a Chromium Task* in a WebKit-style static function + void*.
static void RunTask(void* v) {
    OwnPtr<Task> task(static_cast<Task*>(v));
    task->Run();
}
}
// This is called from the IO thread, and dispatches the callback to the main thread.
void SocketStreamHandlePrivate::maybeCallOnMainThread(Task* task)
{
    callOnMainThread(RunTask, task);
}

void SocketStreamHandlePrivate::onConnected(int maxPendingSendAllowed)
{
    XLOGD2("SocketStreamHandlePrivate onConnected");
    if (m_streamHandle && m_streamHandle->client()) {
        m_maxPendingSendingAllowed = maxPendingSendAllowed;
        m_streamHandle->m_state = SocketStreamHandleBase::Open;
        m_streamHandle->client()->didOpen(m_streamHandle);
    }
}

void SocketStreamHandlePrivate::onSentData(int amountSent)
{
    XLOGD2("SocketStreamHandlePrivate onSentData , amountSent = %d", amountSent);
    if (m_streamHandle && m_streamHandle->client()) {
        m_pendingAmountSent -= amountSent;
        m_streamHandle->sendPendingData();
    }
}

void SocketStreamHandlePrivate::onClose()
{
    XLOGD2("SocketStreamHandlePrivate onClose");
    if (m_streamHandle && m_streamHandle->client()) {
        SocketStreamHandle* streamHandle = m_streamHandle;
        m_streamHandle = 0;
        // This following call deletes _this_. Nothing should be after it.
        streamHandle->client()->didClose(streamHandle);
    }
}

void SocketStreamHandlePrivate::onReceivedData(const std::vector<char>& data )
{
    XLOGD2("SocketStreamHandlePrivate onReceivedData");
    if (m_streamHandle && m_streamHandle->client())
        m_streamHandle->client()->didReceiveData(m_streamHandle, &data[0], data.size());
}

void SocketStreamHandlePrivate::connect()
{

    XLOGD2("SocketStreamHandlePrivate connect");

    GURL gurl(m_url);

    base::Thread* thread = WebUrlLoaderClient::ioThread();
    if (thread)
        thread->message_loop()->PostTask(FROM_HERE, NewRunnableMethod(m_sockhost.get(), &SocketStreamHost::connect, gurl));

}

int SocketStreamHandlePrivate::send(const char* data, int len)
{
    XLOGD2("SocketStreamHandlePrivate send, len = %d", len);

    if(m_pendingAmountSent + len >= m_maxPendingSendingAllowed)
        len = m_maxPendingSendingAllowed - m_pendingAmountSent -1;

    if(len <= 0)
        return len;

    base::Thread* thread = WebUrlLoaderClient::ioThread();

    if (thread) {
        std::string str(data, len);
        const std::vector<char> vdata(str.begin(), str.end());
        thread->message_loop()->PostTask(FROM_HERE, NewRunnableMethod(m_sockhost.get(), &SocketStreamHost::sendData, vdata));
        m_pendingAmountSent += len;
        return len;
    }

    return 0; //sentSize;
}

void SocketStreamHandlePrivate::close()
{
    XLOGD2("SocketStreamHandlePrivate close");
    base::Thread* thread = WebUrlLoaderClient::ioThread();

    if (thread)
        thread->message_loop()->PostTask(FROM_HERE, NewRunnableMethod(m_sockhost.get(), &SocketStreamHost::close));

}

void SocketStreamHandlePrivate::socketError(int error)
{

    XLOGD2("SocketStreamHandlePrivate socketError");

    /*// FIXME - in the future, we might not want to treat all errors as fatal.
    if (m_streamHandle && m_streamHandle->client()) {
        SocketStreamHandle* streamHandle = m_streamHandle;
        m_streamHandle = 0;
        // This following call deletes _this_. Nothing should be after it.
        streamHandle->client()->didClose(streamHandle);
    }*/
}



SocketStreamHandle::SocketStreamHandle(const KURL& url, SocketStreamHandleClient* client)
    : SocketStreamHandleBase(url, client)
{

    /// M : HTML5 web socket
    m_p = new SocketStreamHandlePrivate(this, url);
}

SocketStreamHandle::~SocketStreamHandle()
{
    setClient(0);
}

int SocketStreamHandle::platformSend(const char* data, int len)
{
    LOG(Network, "SocketStreamHandle %p platformSend", this);
    return m_p->send(data, len);
}

void SocketStreamHandle::platformClose()
{
    LOG(Network, "SocketStreamHandle %p platformClose", this);
    m_p->close();
}

void SocketStreamHandle::didReceiveAuthenticationChallenge(const AuthenticationChallenge&)
{
    notImplemented();
}

void SocketStreamHandle::receivedCredential(const AuthenticationChallenge&, const Credential&)
{
    notImplemented();
}

void SocketStreamHandle::receivedRequestToContinueWithoutCredential(const AuthenticationChallenge&)
{
    notImplemented();
}

void SocketStreamHandle::receivedCancellation(const AuthenticationChallenge&)
{
    notImplemented();
}

} // namespace WebCore
