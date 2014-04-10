// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CHROME_BROWSER_RENDERER_HOST_SOCKET_STREAM_HOST_H_
#define CHROME_BROWSER_RENDERER_HOST_SOCKET_STREAM_HOST_H_
#pragma once

#include <vector>

#include "ChromiumIncludes.h"
#include "net/socket_stream/socket_stream.h"
#include "SocketStreamHandleAndroid.h"

class GURL;

namespace net {
class SocketStreamJob;
}

// Host of SocketStreamHandle.
// Each SocketStreamHandle will have an unique socket_id assigned by
// SocketStreamHost constructor. If socket id is chrome_common_net::kNoSocketId,
// there is no SocketStreamHost.
// Each SocketStreamHost has SocketStream to manage bi-directional
// communication over socket stream.
// The lifetime of an instance of this class is completely controlled by the
// SocketStreamDispatcherHost.
namespace WebCore {

class SocketStreamHandlePrivate;

class SocketStreamHost
: public net::SocketStream::Delegate
, public base::RefCountedThreadSafe <SocketStreamHost>
{
public:
    SocketStreamHost( SocketStreamHandlePrivate* ,
                   int socket_id);
    ~SocketStreamHost();

    // Gets SocketStreamHost associated with |socket|.
    static SocketStreamHost* GetSocketStreamHost(net::SocketStream* socket);

    int socket_id() const { return socket_id_; }

    // Starts to open connection to |url|.
    void connect(const GURL& url);

    // Sends |data| over the socket stream.
    // socket stream must be open to send data.
    // Returns true if the data is put in transmit buffer in socket stream.
    // Returns false otherwise (transmit buffer exceeds limit, or socket
    // stream is closed).
    bool sendData(const std::vector<char>& data);

    // Closes the socket stream.
    void close();

//Socket Stream::Delegate methods
    virtual void OnConnected(net::SocketStream*, int);
    virtual void OnSentData(net::SocketStream*, int);
    virtual void OnReceivedData(net::SocketStream*, const char*, int);
    virtual void OnClose(net::SocketStream*);

 private:
    net::SocketStream::Delegate* delegate_;
    friend class base::RefCountedThreadSafe<SocketStreamHost>;
    scoped_refptr<SocketStreamHandlePrivate> m_sockstream;
    int socket_id_;
    scoped_refptr<net::SocketStreamJob> socket_;

    DISALLOW_COPY_AND_ASSIGN(SocketStreamHost);
};

}//namespace WebCore
#endif  // CHROME_BROWSER_RENDERER_HOST_SOCKET_STREAM_HOST_H_
