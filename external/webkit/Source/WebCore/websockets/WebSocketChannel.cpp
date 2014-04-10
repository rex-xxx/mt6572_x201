/*
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

#if ENABLE(WEB_SOCKETS)

#include "WebSocketChannel.h"

#include "CookieJar.h"
#include "Document.h"
#include "InspectorInstrumentation.h"
#include "Logging.h"
#include "Page.h"
#include "PlatformString.h"
#include "ProgressTracker.h"
#include "ScriptCallStack.h"
#include "ScriptExecutionContext.h"
#include "SocketStreamError.h"
#include "SocketStreamHandle.h"
#include "WebSocketChannelClient.h"
#include "WebSocketHandshake.h"

#include <wtf/text/CString.h>
#include <wtf/text/StringConcatenate.h>
#include <wtf/text/StringHash.h>
#include <wtf/Deque.h>
#include <wtf/FastMalloc.h>
#include <wtf/HashMap.h>
/// M: HTML5 web socket @{
#include <wtf/OwnPtr.h>
#include <wtf/PassOwnPtr.h>
#include <wtf/text/WTFString.h>
#include <cutils/xlog.h>
#define XLOG_TAG "WebCore/WebSocket"
/// @}

namespace WebCore {

WebSocketChannel::WebSocketChannel(ScriptExecutionContext* context, WebSocketChannelClient* client, const KURL& url, const String& protocol)
    : m_context(context)
    , m_client(client)
    , m_buffer(0)
    , m_bufferSize(0)
    , m_resumeTimer(this, &WebSocketChannel::resumeTimerFired)
    , m_suspended(false)
    , m_closed(false)
    , m_shouldDiscardReceivedData(false)
    , m_unhandledBufferedAmount(0)
    , m_identifier(0)
/// M: HTML5 web socket @{
    , m_hasContinuousFrame(false)
    , m_closeEventCode(CloseEventCodeAbnormalClosure)
/// @}
{
    if (m_context->isDocument())
        if (Page* page = static_cast<Document*>(m_context)->page())
            m_identifier = page->progress()->createUniqueIdentifier();

    if (m_identifier)
        InspectorInstrumentation::didCreateWebSocket(m_context, m_identifier, url, m_context->url());
/// M: HTML5 web socket
    m_handshake = adoptPtr(new WebSocketHandshake(url, protocol, context));
}

WebSocketChannel::~WebSocketChannel()
{
    fastFree(m_buffer);
}

void WebSocketChannel::connect()
{
    LOG(Network, "WebSocketChannel %p connect", this);
    ASSERT(!m_handle);
    ASSERT(!m_suspended);
/// M: HTML5 web socket
    m_handshake->reset();
    ref();
    m_handle = SocketStreamHandle::create(m_handshake->url(), this);
}


ThreadableWebSocketChannel::SendResult WebSocketChannel::send(const String& message)
{
    LOG(Network, "WebSocketChannel %p send %s", this, message.utf8().data());
    CString utf8 = message.utf8(true);
    if (utf8.isNull() && message.length())
        return InvalidMessage;
    enqueueTextFrame(utf8);
    // According to WebSocket API specification, WebSocket.send() should return void instead
    // of boolean. However, our implementation still returns boolean due to compatibility
    // concern (see bug 65850).
    // m_channel->send() may happen later, thus it's not always possible to know whether
    // the message has been sent to the socket successfully. In this case, we have no choice
    // but to return true.
    return ThreadableWebSocketChannel::SendSuccess;
}
/// @}

unsigned long WebSocketChannel::bufferedAmount() const
{
    LOG(Network, "WebSocketChannel %p bufferedAmount", this);
    ASSERT(m_handle);
    ASSERT(!m_suspended);
    return m_handle->bufferedAmount();
}

void WebSocketChannel::close()
{
    LOG(Network, "WebSocketChannel %p close", this);
    ASSERT(!m_suspended);
    if (m_handle)
        m_handle->close();  // will call didClose()
}

/// M: HTML5 web socket @{
void WebSocketChannel::fail(const String& reason)
{
    LOG(Network, "WebSocketChannel %p fail: %s", this, reason.utf8().data());
    ASSERT(!m_suspended);
/// M: HTML5 web socket
    if (m_context)
        m_context->addMessage(JSMessageSource, LogMessageType, ErrorMessageLevel, reason, 0, m_handshake->clientOrigin(), 0);

    m_hasContinuousFrame = false;
    m_continuousFrameData.clear();

    if (m_handle && !m_closed)
        m_handle->close(); // Will call didClose().
}
/// @}

void WebSocketChannel::disconnect()
{
    LOG(Network, "WebSocketChannel %p disconnect", this);
    if (m_identifier && m_context)
        InspectorInstrumentation::didCloseWebSocket(m_context, m_identifier);
/// M: HTML5 web socket
    if (m_handshake)
        m_handshake->clearScriptExecutionContext();
    m_client = 0;
    m_context = 0;
    if (m_handle)
        m_handle->close();
}

void WebSocketChannel::suspend()
{
    m_suspended = true;
}

void WebSocketChannel::resume()
{
    m_suspended = false;
    if ((m_buffer || m_closed) && m_client && !m_resumeTimer.isActive())
        m_resumeTimer.startOneShot(0);
}

void WebSocketChannel::didOpen(SocketStreamHandle* handle)
{
    LOG(Network, "WebSocketChannel %p didOpen", this);
    ASSERT(handle == m_handle);
    if (!m_context)
        return;
/// M: HTML5 web socket
    if (m_identifier)
        InspectorInstrumentation::willSendWebSocketHandshakeRequest(m_context, m_identifier, m_handshake->clientHandshakeRequest());
    CString handshakeMessage = m_handshake->clientHandshakeMessage();
    if (!handle->send(handshakeMessage.data(), handshakeMessage.length())) {
        m_context->addMessage(JSMessageSource, LogMessageType, ErrorMessageLevel, "Error sending handshake message.", 0, m_handshake->clientOrigin(), 0);
        handle->close();
    }
}

void WebSocketChannel::didClose(SocketStreamHandle* handle)
{
    LOG(Network, "WebSocketChannel %p didClose", this);
    if (m_identifier && m_context)
        InspectorInstrumentation::didCloseWebSocket(m_context, m_identifier);
    ASSERT_UNUSED(handle, handle == m_handle || !m_handle);
    m_closed = true;
    if (m_handle) {
        m_unhandledBufferedAmount = m_handle->bufferedAmount();
        if (m_suspended)
            return;
        WebSocketChannelClient* client = m_client;
        m_client = 0;
        m_context = 0;
        m_handle = 0;
        if (client)
            client->didClose(m_unhandledBufferedAmount);
    }
    deref();
}

void WebSocketChannel::didReceiveData(SocketStreamHandle* handle, const char* data, int len)
{
    LOG(Network, "WebSocketChannel %p didReceiveData %d", this, len);
    RefPtr<WebSocketChannel> protect(this); // The client can close the channel, potentially removing the last reference.
    ASSERT(handle == m_handle);
    if (!m_context) {
        return;
    }
    if (!m_client) {
        m_shouldDiscardReceivedData = true;
        handle->close();
        return;
    }
    if (m_shouldDiscardReceivedData)
        return;
    if (!appendToBuffer(data, len)) {
        m_shouldDiscardReceivedData = true;
        handle->close();
        return;
    }
    while (!m_suspended && m_client && m_buffer)
        if (!processBuffer())
            break;
}

void WebSocketChannel::didFail(SocketStreamHandle* handle, const SocketStreamError& error)
{
    LOG(Network, "WebSocketChannel %p didFail", this);
    ASSERT(handle == m_handle || !m_handle);
    if (m_context) {
        String message;
        if (error.isNull())
            message = "WebSocket network error";
        else if (error.localizedDescription().isNull())
            message = makeString("WebSocket network error: error code ", String::number(error.errorCode()));
        else
            message = makeString("WebSocket network error: ", error.localizedDescription());
        m_context->addMessage(OtherMessageSource, NetworkErrorMessageType, ErrorMessageLevel, message, 0, error.failingURL(), 0);
    }
    m_shouldDiscardReceivedData = true;
    handle->close();
}

void WebSocketChannel::didReceiveAuthenticationChallenge(SocketStreamHandle*, const AuthenticationChallenge&)
{
}

void WebSocketChannel::didCancelAuthenticationChallenge(SocketStreamHandle*, const AuthenticationChallenge&)
{
}

bool WebSocketChannel::appendToBuffer(const char* data, size_t len)
{
    size_t newBufferSize = m_bufferSize + len;
    if (newBufferSize < m_bufferSize) {
        LOG(Network, "WebSocket buffer overflow (%lu+%lu)", static_cast<unsigned long>(m_bufferSize), static_cast<unsigned long>(len));
        return false;
    }
    char* newBuffer = 0;
    if (tryFastMalloc(newBufferSize).getValue(newBuffer)) {
        if (m_buffer)
            memcpy(newBuffer, m_buffer, m_bufferSize);
        memcpy(newBuffer + m_bufferSize, data, len);
        fastFree(m_buffer);
        m_buffer = newBuffer;
        m_bufferSize = newBufferSize;
        return true;
    }
    m_context->addMessage(JSMessageSource, LogMessageType, ErrorMessageLevel, makeString("WebSocket frame (at ", String::number(static_cast<unsigned long>(newBufferSize)), " bytes) is too long."), 0, m_handshake->clientOrigin(), 0);
    return false;
}

void WebSocketChannel::skipBuffer(size_t len)
{
    ASSERT(len <= m_bufferSize);
    m_bufferSize -= len;
    if (!m_bufferSize) {
        fastFree(m_buffer);
        m_buffer = 0;
        return;
    }
    memmove(m_buffer, m_buffer + len, m_bufferSize);
}

bool WebSocketChannel::processBuffer()
{
    ASSERT(!m_suspended);
    ASSERT(m_client);
    ASSERT(m_buffer);
    if (m_shouldDiscardReceivedData)
        return false;

    /// M: HTML5 web socket @{
    RefPtr<WebSocketChannel> protect(this); // The client can close the channel, potentially removing the last reference.

    if (m_handshake->mode() == WebSocketHandshake::Incomplete) {
        int headerLength = m_handshake->readServerHandshake(m_buffer, m_bufferSize);
        if (headerLength <= 0)
            return false;
        if (m_handshake->mode() == WebSocketHandshake::Connected) {
            if (m_identifier)
                InspectorInstrumentation::didReceiveWebSocketHandshakeResponse(m_context, m_identifier, m_handshake->serverHandshakeResponse());
            if (!m_handshake->serverSetCookie().isEmpty()) {
                if (m_context->isDocument()) {
                    Document* document = static_cast<Document*>(m_context);
                    if (cookiesEnabled(document)) {
                        ExceptionCode ec; // Exception (for sandboxed documents) ignored.
                        document->setCookie(m_handshake->serverSetCookie(), ec);
                    }
                }
            }
            // FIXME: handle set-cookie2.
            LOG(Network, "WebSocketChannel %p connected", this);
            skipBuffer(headerLength);
            m_client->didConnect();
            LOG(Network, "remaining in read buf %lu", static_cast<unsigned long>(m_bufferSize));
            return m_buffer;
        }
        LOG(Network, "WebSocketChannel %p connection failed", this);
        skipBuffer(headerLength);
        m_shouldDiscardReceivedData = true;
        if (!m_closed)
            m_handle->close();
        return false;
    }
    if (m_handshake->mode() != WebSocketHandshake::Connected)
        return false;
    /// @}

    /// M: HTML5 web socket 
    return processFrame();
}

void WebSocketChannel::resumeTimerFired(Timer<WebSocketChannel>* timer)
{
    ASSERT_UNUSED(timer, timer == &m_resumeTimer);

    RefPtr<WebSocketChannel> protect(this); // The client can close the channel, potentially removing the last reference.
    while (!m_suspended && m_client && m_buffer)
        if (!processBuffer())
            break;
    if (!m_suspended && m_client && m_closed && m_handle)
        didClose(m_handle.get());
}

/// M: HTML5 web socket @{
void WebSocketChannel::startClosingHandshake(int code, const String& reason)
{
    LOG(Network, "WebSocketChannel %p closing %d %d", this, m_closing, m_receivedClosingHandshake);
    if (m_closing)
        return;
    ASSERT(m_handle);

    Vector<char> buf;
    if (!m_receivedClosingHandshake && code != CloseEventCodeNotSpecified) {
        unsigned char highByte = code >> 8;
        unsigned char lowByte = code;
        buf.append(static_cast<char>(highByte));
        buf.append(static_cast<char>(lowByte));
        buf.append(reason.utf8().data(), reason.utf8().length());
    }
    enqueueRawFrame(WebSocketFrame::OpCodeClose, buf.data(), buf.size());

    m_closing = true;
    if (m_client)
        m_client->didStartClosingHandshake();
}

bool WebSocketChannel::processFrame()
{
    ASSERT(m_buffer);

    WebSocketFrame frame;
    const char* frameEnd;
    String errorString;
    WebSocketFrame::ParseFrameResult result = WebSocketFrame::parseFrame(m_buffer, m_bufferSize, frame, frameEnd, errorString);
    if (result == WebSocketFrame::FrameIncomplete)
        return false;
    if (result == WebSocketFrame::FrameError) {
        fail(errorString);
        return false;
    }

    ASSERT(m_buffer < frameEnd);
    ASSERT(frameEnd <= m_buffer + m_bufferSize);

    OwnPtr<InflateResultHolder> inflateResult = m_deflateFramer.inflate(frame);
    if (!inflateResult->succeeded()) {
        fail(inflateResult->failureReason());
        return false;
    }

    // Validate the frame data.
    if (WebSocketFrame::isReservedOpCode(frame.opCode)) {
        fail("Unrecognized frame opcode: " + String::number(frame.opCode));
        return false;
    }

    if (frame.reserved2 || frame.reserved3) {
        fail("One or more reserved bits are on: reserved2 = " + String::number(frame.reserved2) + ", reserved3 = " + String::number(frame.reserved3));
        return false;
    }

    if (frame.masked) {
        fail("A server must not mask any frames that it sends to the client.");
        return false;
    }

    // All control frames must not be fragmented.
    if (WebSocketFrame::isControlOpCode(frame.opCode) && !frame.final) {
        fail("Received fragmented control frame: opcode = " + String::number(frame.opCode));
        return false;
    }

    // All control frames must have a payload of 125 bytes or less, which means the frame must not contain
    // the "extended payload length" field.
    if (WebSocketFrame::isControlOpCode(frame.opCode) && WebSocketFrame::needsExtendedLengthField(frame.payloadLength)) {
        fail("Received control frame having too long payload: " + String::number(frame.payloadLength) + " bytes");
        return false;
    }

    // A new data frame is received before the previous continuous frame finishes.
    // Note that control frames are allowed to come in the middle of continuous frames.
    if (m_hasContinuousFrame && frame.opCode != WebSocketFrame::OpCodeContinuation && !WebSocketFrame::isControlOpCode(frame.opCode)) {
        fail("Received new data frame but previous continuous frame is unfinished.");
        return false;
    }

    switch (frame.opCode) {
    case WebSocketFrame::OpCodeContinuation:
        // An unexpected continuation frame is received without any leading frame.
        if (!m_hasContinuousFrame) {
            fail("Received unexpected continuation frame.");
            return false;
        }
        m_continuousFrameData.append(frame.payload, frame.payloadLength);
        skipBuffer(frameEnd - m_buffer);
        if (frame.final) {
            // onmessage handler may eventually call the other methods of this channel,
            // so we should pretend that we have finished to read this frame and
            // make sure that the member variables are in a consistent state before
            // the handler is invoked.
            // Vector<char>::swap() is used here to clear m_continuousFrameData.
            OwnPtr<Vector<char> > continuousFrameData = adoptPtr(new Vector<char>);
            m_continuousFrameData.swap(*continuousFrameData);
            m_hasContinuousFrame = false;
            if (m_continuousFrameOpCode == WebSocketFrame::OpCodeText) {
                String message;
                if (continuousFrameData->size())
                    message = String::fromUTF8(continuousFrameData->data(), continuousFrameData->size());
                else
                    message = "";
                if (message.isNull())
                    fail("Could not decode a text frame as UTF-8.");
                else
                    m_client->didReceiveMessage(message);
            }
        }
        break;

    case WebSocketFrame::OpCodeText:
        if (frame.final) {
            String message;
            if (frame.payloadLength)
                message = String::fromUTF8(frame.payload, frame.payloadLength);
            else
                message = "";
            skipBuffer(frameEnd - m_buffer);
            if (message.isNull())
                fail("Could not decode a text frame as UTF-8.");
            else
                m_client->didReceiveMessage(message);
        } else {
            m_hasContinuousFrame = true;
            m_continuousFrameOpCode = WebSocketFrame::OpCodeText;
            ASSERT(m_continuousFrameData.isEmpty());
            m_continuousFrameData.append(frame.payload, frame.payloadLength);
            skipBuffer(frameEnd - m_buffer);
        }
        break;


    case WebSocketFrame::OpCodeClose:
        if (frame.payloadLength >= 2) {
            unsigned char highByte = static_cast<unsigned char>(frame.payload[0]);
            unsigned char lowByte = static_cast<unsigned char>(frame.payload[1]);
            m_closeEventCode = highByte << 8 | lowByte;
        } else
            m_closeEventCode = CloseEventCodeNoStatusRcvd;
        if (frame.payloadLength >= 3)
            m_closeEventReason = String::fromUTF8(&frame.payload[2], frame.payloadLength - 2);
        else
            m_closeEventReason = "";
        skipBuffer(frameEnd - m_buffer);
        m_receivedClosingHandshake = true;
        startClosingHandshake(m_closeEventCode, m_closeEventReason);
        if (m_closing) {
            m_outgoingFrameQueueStatus = OutgoingFrameQueueClosing;
            processOutgoingFrameQueue();
        }
        break;

    case WebSocketFrame::OpCodePing:
        enqueueRawFrame(WebSocketFrame::OpCodePong, frame.payload, frame.payloadLength);
        skipBuffer(frameEnd - m_buffer);
        break;

    case WebSocketFrame::OpCodePong:
        // A server may send a pong in response to our ping, or an unsolicited pong which is not associated with
        // any specific ping. Either way, there's nothing to do on receipt of pong.
        skipBuffer(frameEnd - m_buffer);
        break;

    default:
        ASSERT_NOT_REACHED();
        skipBuffer(frameEnd - m_buffer);
        break;
    }

    return m_buffer;
}

void WebSocketChannel::enqueueTextFrame(const CString& string)
{

    XLOGD2("WebSocketChannel enqueueTextFrame");

    ASSERT(m_outgoingFrameQueueStatus == OutgoingFrameQueueOpen);

    QueuedFrame frame = { WebSocketFrame::OpCodeText, QueuedFrameTypeString, string };
    m_outgoingFrameQueue.append(frame);

    processOutgoingFrameQueue();
}

void WebSocketChannel::enqueueRawFrame(WebSocketFrame::OpCode opCode, const char* data, size_t dataLength)
{
    ASSERT(m_outgoingFrameQueueStatus == OutgoingFrameQueueOpen);

    QueuedFrame frame = { opCode, QueuedFrameTypeVector, data };
    m_outgoingFrameQueue.append(frame);

    processOutgoingFrameQueue();
}

void WebSocketChannel::processOutgoingFrameQueue()
{

    XLOGD2("WebSocketChannel processOutgoingFrameQueue");


    if (m_outgoingFrameQueueStatus == OutgoingFrameQueueClosed)
        return;

    while (!m_outgoingFrameQueue.isEmpty()) {
        QueuedFrame frame = m_outgoingFrameQueue.takeFirst();
        switch (frame.frameType) {
        case QueuedFrameTypeString: {
            if (!sendFrame(frame.opCode, frame.stringData.data(), frame.stringData.length()))
                fail("Failed to send WebSocket frame.");
            break;
        }

        case QueuedFrameTypeVector:
            if (!sendFrame(frame.opCode, frame.vectorData.data(), frame.vectorData.size()))
                fail("Failed to send WebSocket frame.");
            break;


        default:
            ASSERT_NOT_REACHED();
            break;
        }
    }

    ASSERT(m_outgoingFrameQueue.isEmpty());
    if (m_outgoingFrameQueueStatus == OutgoingFrameQueueClosing) {
        m_outgoingFrameQueueStatus = OutgoingFrameQueueClosed;
        m_handle->close();
    }
}

bool WebSocketChannel::sendFrame(WebSocketFrame::OpCode opCode, const char* data, size_t dataLength)
{

    XLOGD2("WebSocketChannel sendFrame");


    ASSERT(m_handle);
    ASSERT(!m_suspended);

    WebSocketFrame frame(opCode, true, false, true, data, dataLength);

    OwnPtr<DeflateResultHolder> deflateResult = m_deflateFramer.deflate(frame);
    if (!deflateResult->succeeded()) {
        fail(deflateResult->failureReason());
        return false;
    }

    Vector<char> frameData;
    frame.makeFrameData(frameData);

    return m_handle->send(frameData.data(), frameData.size());
}

/// @}

}  // namespace WebCore

#endif  // ENABLE(WEB_SOCKETS)
