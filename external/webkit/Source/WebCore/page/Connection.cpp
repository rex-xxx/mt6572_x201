/*
 * Copyright 2010, The Android Open Source Project
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

#include "config.h"
#include "Connection.h"
/// M: add Network Information API.
#include "Frame.h"
#include "NetworkStateNotifier.h"
/// M: add Network Information API.
#include "WebViewCore.h"

using namespace android;

namespace WebCore {

Connection::ConnectionType Connection::type() const
{
    return networkStateNotifier().type();
}

/// M: add Network Information API. @{
double Connection::bandwidth() const
{
    if (!networkStateNotifier().onLine())
        return 0;
    else
        //till the necessary network api to measure bandwidth becomes available, we return infinity as per the specifications
        return std::numeric_limits<double>::infinity();
}

bool Connection::metered() const
{
    WebViewCore* webViewCore = WebViewCore::getWebViewCore(m_frame->view());

    if (!networkStateNotifier().onLine())
        return 0;
    else
        return webViewCore->isNetworkMetered();
}

ScriptExecutionContext* Connection::scriptExecutionContext() const
{
    return ActiveDOMObject::scriptExecutionContext();
}

void Connection::fireEvent(const AtomicString& type)
{
    dispatchEvent(Event::create(type, false, false));
}

bool Connection::hasPendingActivity() const
{
    return (ActiveDOMObject::hasPendingActivity());
}

bool Connection::canSuspend() const
{
    return false;
}

void Connection::stop() const
{
    //Not Implemented
}
/// @}

};
