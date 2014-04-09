/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "ARTSPConnection"
#include <utils/Log.h>

#include "ARTSPConnection.h"

#include <cutils/properties.h>

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/base64.h>
#include <media/stagefright/MediaErrors.h>

#include <arpa/inet.h>
#include <fcntl.h>
#include <netdb.h>
#include <openssl/md5.h>
#include <sys/socket.h>

#include "HTTPBase.h"
#ifndef ANDROID_DEFAULT_CODE 
#include <cutils/properties.h>
#include "ARTPSource.h"

#ifdef CUSTOM_UASTRING_FROM_PROPERTY
#include "custom_prop.h"
#endif


#define MTK_SEND_FLAG (MSG_NOSIGNAL)
#endif // #ifndef ANDROID_DEFAULT_CODE

namespace android {

// static
const int64_t ARTSPConnection::kSelectTimeoutUs = 1000ll;

#ifndef ANDROID_DEFAULT_CODE 
// 15s is service ANR timeout, we choose 14s for request timeout
const int64_t ARTSPConnection::kRequestTimeout = 60000000ll;
static int64_t kAccessUnitTimeoutUs = ARTPSource::kAccessUnitTimeoutUs;
#endif // #ifndef ANDROID_DEFAULT_CODE

ARTSPConnection::ARTSPConnection(bool uidValid, uid_t uid)
    : mUIDValid(uidValid),
      mUID(uid),
      mState(DISCONNECTED),
      mAuthType(NONE),
#ifndef ANDROID_DEFAULT_CODE 
      mKeepTCPTrying(false),
      mForceQuitTCP(false),
#endif // #ifndef ANDROID_DEFAULT_CODE
      mSocket(-1),
      mConnectionID(0),
      mNextCSeq(0),
      mReceiveResponseEventPending(false) {
    MakeUserAgent(&mUserAgent);
}

ARTSPConnection::~ARTSPConnection() {
    if (mSocket >= 0) {
        ALOGE("Connection is still open, closing the socket.");
        if (mUIDValid) {
            HTTPBase::UnRegisterSocketUserTag(mSocket);
        }
        close(mSocket);
        mSocket = -1;
    }
}

void ARTSPConnection::connect(const char *url, const sp<AMessage> &reply) {
    sp<AMessage> msg = new AMessage(kWhatConnect, id());
    msg->setString("url", url);
    msg->setMessage("reply", reply);
    msg->post();
}

void ARTSPConnection::disconnect(const sp<AMessage> &reply) {
    sp<AMessage> msg = new AMessage(kWhatDisconnect, id());
    msg->setMessage("reply", reply);
    msg->post();
}

void ARTSPConnection::sendRequest(
        const char *request, const sp<AMessage> &reply) {
    sp<AMessage> msg = new AMessage(kWhatSendRequest, id());
    msg->setString("request", request);
    msg->setMessage("reply", reply);
    msg->post();
#ifndef ANDROID_DEFAULT_CODE 
    msg->setInt32("backup-keep-tcp", mForceQuitTCP);
    int keep;
    if (!reply->findInt32("keep-tcp", &keep)) {
        mForceQuitTCP = true;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE
}

void ARTSPConnection::observeBinaryData(const sp<AMessage> &reply) {
    sp<AMessage> msg = new AMessage(kWhatObserveBinaryData, id());
    msg->setMessage("reply", reply);
    msg->post();
}

void ARTSPConnection::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatConnect:
            onConnect(msg);
            break;

        case kWhatDisconnect:
            onDisconnect(msg);
            break;

        case kWhatCompleteConnection:
            onCompleteConnection(msg);
            break;

        case kWhatSendRequest:
            onSendRequest(msg);
            break;

        case kWhatReceiveResponse:
            onReceiveResponse();
            break;

        case kWhatObserveBinaryData:
        {
            CHECK(msg->findMessage("reply", &mObserveBinaryMessage));
            break;
        }

#ifndef ANDROID_DEFAULT_CODE 
        case kWhatTimeout:
        {
            unsigned long cseq;
            CHECK(msg->findInt32("cseq", (int32_t*)&cseq));
            onTimeout(cseq);
            break;
        }
        case kWhatInjectPacket:
        {
            onInjectPacket(msg);
            break;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        default:
            TRESPASS();
            break;
    }
}

// static
bool ARTSPConnection::ParseURL(
        const char *url, AString *host, unsigned *port, AString *path,
        AString *user, AString *pass) {
    host->clear();
    *port = 0;
    path->clear();
    user->clear();
    pass->clear();

    if (strncasecmp("rtsp://", url, 7)) {
        return false;
    }

    const char *slashPos = strchr(&url[7], '/');

    if (slashPos == NULL) {
        host->setTo(&url[7]);
        path->setTo("/");
    } else {
        host->setTo(&url[7], slashPos - &url[7]);
        path->setTo(slashPos);
    }

    ssize_t atPos = host->find("@");

    if (atPos >= 0) {
        // Split of user:pass@ from hostname.

        AString userPass(*host, 0, atPos);
        host->erase(0, atPos + 1);

        ssize_t colonPos = userPass.find(":");

        if (colonPos < 0) {
            *user = userPass;
        } else {
            user->setTo(userPass, 0, colonPos);
            pass->setTo(userPass, colonPos + 1, userPass.size() - colonPos - 1);
        }
    }

    const char *colonPos = strchr(host->c_str(), ':');

    if (colonPos != NULL) {
        unsigned long x;
        if (!ParseSingleUnsignedLong(colonPos + 1, &x) || x >= 65536) {
            return false;
        }

        *port = x;

        size_t colonOffset = colonPos - host->c_str();
        size_t trailing = host->size() - colonOffset;
        host->erase(colonOffset, trailing);
    } else {
        *port = 554;
    }

    return true;
}

static status_t MakeSocketBlocking(int s, bool blocking) {
    // Make socket non-blocking.
    int flags = fcntl(s, F_GETFL, 0);

    if (flags == -1) {
        return UNKNOWN_ERROR;
    }

    if (blocking) {
        flags &= ~O_NONBLOCK;
    } else {
        flags |= O_NONBLOCK;
    }

    flags = fcntl(s, F_SETFL, flags);

    return flags == -1 ? UNKNOWN_ERROR : OK;
}

void ARTSPConnection::onConnect(const sp<AMessage> &msg) {
    ++mConnectionID;

    if (mState != DISCONNECTED) {
        if (mUIDValid) {
            HTTPBase::UnRegisterSocketUserTag(mSocket);
        }
        close(mSocket);
        mSocket = -1;

        flushPendingRequests();
    }

    mState = CONNECTING;

    AString url;
    CHECK(msg->findString("url", &url));

    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    AString host, path;
    unsigned port;
    if (!ParseURL(url.c_str(), &host, &port, &path, &mUser, &mPass)
            || (mUser.size() > 0 && mPass.size() == 0)) {
        // If we have a user name but no password we have to give up
        // right here, since we currently have no way of asking the user
        // for this information.

        ALOGE("Malformed rtsp url %s", url.c_str());

        reply->setInt32("result", ERROR_MALFORMED);
        reply->post();

        mState = DISCONNECTED;
        return;
    }

    if (mUser.size() > 0) {
        ALOGV("user = '%s', pass = '%s'", mUser.c_str(), mPass.c_str());
    }

#ifndef ANDROID_DEFAULT_CODE 
    if (!mProxyHost.empty()) {
        ALOGI("connect through proxy %s:%d", mProxyHost.c_str(), mProxyPort);
        host = mProxyHost;
        port = mProxyPort;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE

    struct hostent *ent = gethostbyname(host.c_str());
    if (ent == NULL) {
        ALOGE("Unknown host %s", host.c_str());

#ifndef ANDROID_DEFAULT_CODE 
        reply->setInt32("result", -EHOSTUNREACH);
#else
        reply->setInt32("result", -ENOENT);
#endif // #ifndef ANDROID_DEFAULT_CODE
        reply->post();

        mState = DISCONNECTED;
        return;
    }

    mSocket = socket(AF_INET, SOCK_STREAM, 0);

    if (mUIDValid) {
        HTTPBase::RegisterSocketUserTag(mSocket, mUID,
                                        (uint32_t)*(uint32_t*) "RTSP");
    }

    MakeSocketBlocking(mSocket, false);

    struct sockaddr_in remote;
    memset(remote.sin_zero, 0, sizeof(remote.sin_zero));
    remote.sin_family = AF_INET;
    remote.sin_addr.s_addr = *(in_addr_t *)ent->h_addr;
    remote.sin_port = htons(port);
#ifndef ANDROID_DEFAULT_CODE 
    mRemote = remote;
    struct timeval tv;
    tv.tv_sec = kAccessUnitTimeoutUs / 1000000LL;
    tv.tv_usec = 0;
    if (setsockopt(mSocket, SOL_SOCKET, SO_RCVTIMEO, (char *)&tv, sizeof(tv)) < 0) {
        ALOGE("can not set recv timeout");
        reply->setInt32("result", -errno);
        mState = DISCONNECTED;

        close(mSocket);
        mSocket = -1;
        reply->post();
        return;
    }
#endif // #ifndef ANDROID_DEFAULT_CODE

    int err = ::connect(
            mSocket, (const struct sockaddr *)&remote, sizeof(remote));

    reply->setInt32("server-ip", ntohl(remote.sin_addr.s_addr));

    if (err < 0) {
        if (errno == EINPROGRESS) {
            sp<AMessage> msg = new AMessage(kWhatCompleteConnection, id());
            msg->setMessage("reply", reply);
            msg->setInt32("connection-id", mConnectionID);
#ifndef ANDROID_DEFAULT_CODE 
            msg->setInt64("timestamp", ALooper::GetNowUs());
            ALOGI("connection EINPROGRESS");
#endif // #ifndef ANDROID_DEFAULT_CODE
            msg->post();
            return;
        }

        reply->setInt32("result", -errno);
        mState = DISCONNECTED;

        if (mUIDValid) {
            HTTPBase::UnRegisterSocketUserTag(mSocket);
        }
        close(mSocket);
        mSocket = -1;
    } else {
        reply->setInt32("result", OK);
        mState = CONNECTED;
        mNextCSeq = 1;

        postReceiveReponseEvent();
    }

    reply->post();
}

void ARTSPConnection::performDisconnect() {
    if (mUIDValid) {
        HTTPBase::UnRegisterSocketUserTag(mSocket);
    }
    close(mSocket);
    mSocket = -1;

    flushPendingRequests();

    mUser.clear();
    mPass.clear();
    mAuthType = NONE;
    mNonce.clear();
#ifndef ANDROID_DEFAULT_CODE 
    mRealm.clear();
#endif // #ifndef ANDROID_DEFAULT_CODE

    mState = DISCONNECTED;
}

void ARTSPConnection::onDisconnect(const sp<AMessage> &msg) {
    if (mState == CONNECTED || mState == CONNECTING) {
        performDisconnect();
    }

    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    reply->setInt32("result", OK);

    reply->post();
}

void ARTSPConnection::onCompleteConnection(const sp<AMessage> &msg) {
    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    int32_t connectionID;
    CHECK(msg->findInt32("connection-id", &connectionID));

    if ((connectionID != mConnectionID) || mState != CONNECTING) {
        // While we were attempting to connect, the attempt was
        // cancelled.
        reply->setInt32("result", -ECONNABORTED);
        reply->post();
        return;
    }

    struct timeval tv;
    tv.tv_sec = 0;
    tv.tv_usec = kSelectTimeoutUs;

    fd_set ws;
    FD_ZERO(&ws);
    FD_SET(mSocket, &ws);

    int res = select(mSocket + 1, NULL, &ws, NULL, &tv);
    CHECK_GE(res, 0);

    if (res == 0) {
        // Timed out. Not yet connected.

#ifndef ANDROID_DEFAULT_CODE 
        int64_t then, now = ALooper::GetNowUs();
        if (msg->findInt64("timestamp", &then) && now - then > kRequestTimeout) {
            ALOGE("connection timeout %lld > %lld", now, then);
            reply->setInt32("result", -110 /*ETIMEDOUT*/);
            reply->post();
            mState = DISCONNECTED;
            close(mSocket);
            mSocket = -1;
            return;
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        msg->post();
        return;
    }

    int err;
    socklen_t optionLen = sizeof(err);
    CHECK_EQ(getsockopt(mSocket, SOL_SOCKET, SO_ERROR, &err, &optionLen), 0);
    CHECK_EQ(optionLen, (socklen_t)sizeof(err));

    if (err != 0) {
        ALOGE("err = %d (%s)", err, strerror(err));

        reply->setInt32("result", -err);

        mState = DISCONNECTED;
        if (mUIDValid) {
            HTTPBase::UnRegisterSocketUserTag(mSocket);
        }
        close(mSocket);
        mSocket = -1;
    } else {
        reply->setInt32("result", OK);
        mState = CONNECTED;
        mNextCSeq = 1;

        postReceiveReponseEvent();
    }

    reply->post();
}

void ARTSPConnection::onSendRequest(const sp<AMessage> &msg) {
#ifndef ANDROID_DEFAULT_CODE
    int32_t backup;
    if (msg->findInt32("backup-keep-tcp", &backup)) {
        mForceQuitTCP = backup;
    }
#endif
    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    if (mState != CONNECTED) {
        reply->setInt32("result", -ENOTCONN);
        reply->post();
        return;
    }

    AString request;
    CHECK(msg->findString("request", &request));

    // Just in case we need to re-issue the request with proper authentication
    // later, stash it away.
    reply->setString("original-request", request.c_str(), request.size());

    addAuthentication(&request);
    addUserAgent(&request);

    // Find the boundary between headers and the body.
    ssize_t i = request.find("\r\n\r\n");
    CHECK_GE(i, 0);

    int32_t cseq = mNextCSeq++;

    AString cseqHeader = "CSeq: ";
    cseqHeader.append(cseq);
    cseqHeader.append("\r\n");

    request.insert(cseqHeader, i + 2);

#ifndef ANDROID_DEFAULT_CODE
    ALOGI("request: '%s'", request.c_str());
#else
    ALOGV("request: '%s'", request.c_str());
#endif

    size_t numBytesSent = 0;
    while (numBytesSent < request.size()) {
        ssize_t n =
#ifndef ANDROID_DEFAULT_CODE
            send(mSocket, request.c_str() + numBytesSent,
                 request.size() - numBytesSent, MTK_SEND_FLAG);
#else
            send(mSocket, request.c_str() + numBytesSent,
                 request.size() - numBytesSent, 0);
#endif

        if (n < 0 && errno == EINTR) {
            continue;
        }

        if (n <= 0) {
            performDisconnect();

            if (n == 0) {
                // Server closed the connection.
                ALOGE("Server unexpectedly closed the connection.");

                reply->setInt32("result", ERROR_IO);
                reply->post();
            } else {
                ALOGE("Error sending rtsp request. (%s)", strerror(errno));
                reply->setInt32("result", -errno);
                reply->post();
            }

            return;
        }

        numBytesSent += (size_t)n;
    }

    mPendingRequests.add(cseq, reply);
#ifndef ANDROID_DEFAULT_CODE 
    sp<AMessage> timeout = new AMessage(kWhatTimeout, id());
    timeout->setInt32("cseq", cseq);
    int64_t t;
    if (reply->findInt64("timeout", &t)) {
        timeout->post(t);
    } else {
        timeout->post(kRequestTimeout);
    }
#endif // #ifndef ANDROID_DEFAULT_CODE
}

void ARTSPConnection::onReceiveResponse() {
    mReceiveResponseEventPending = false;

    if (mState != CONNECTED) {
        return;
    }

    struct timeval tv;
    tv.tv_sec = 0;
    tv.tv_usec = kSelectTimeoutUs;

    fd_set rs;
    FD_ZERO(&rs);
    FD_SET(mSocket, &rs);

    int res = select(mSocket + 1, &rs, NULL, NULL, &tv);
    CHECK_GE(res, 0);

    if (res == 1) {
        MakeSocketBlocking(mSocket, true);

        bool success = receiveRTSPReponse();

        MakeSocketBlocking(mSocket, false);

        if (!success) {
            // Something horrible, irreparable has happened.
            flushPendingRequests();
            return;
        }
    }

    postReceiveReponseEvent();
}

void ARTSPConnection::flushPendingRequests() {
    for (size_t i = 0; i < mPendingRequests.size(); ++i) {
        sp<AMessage> reply = mPendingRequests.valueAt(i);

        reply->setInt32("result", -ECONNABORTED);
        reply->post();
    }

    mPendingRequests.clear();
}

void ARTSPConnection::postReceiveReponseEvent() {
    if (mReceiveResponseEventPending) {
        return;
    }

    sp<AMessage> msg = new AMessage(kWhatReceiveResponse, id());
    msg->post();

    mReceiveResponseEventPending = true;
}

status_t ARTSPConnection::receive(void *data, size_t size) {
    size_t offset = 0;
    while (offset < size) {
        ssize_t n = recv(mSocket, (uint8_t *)data + offset, size - offset, 0);

        if (n < 0 && errno == EINTR) {
            continue;
        }

        if (n <= 0) {
#ifndef ANDROID_DEFAULT_CODE 
            ALOGW("receive %ld errno %d trying %d, force %d", n, errno, mKeepTCPTrying, mForceQuitTCP);
            if (errno == EAGAIN && mKeepTCPTrying && !mForceQuitTCP) {
                continue;
            }
#endif // #ifndef ANDROID_DEFAULT_CODE
            performDisconnect();

            if (n == 0) {
                // Server closed the connection.
                ALOGE("Server unexpectedly closed the connection.");
                return ERROR_IO;
            } else {
                ALOGE("Error reading rtsp response. (%s)", strerror(errno));
                return -errno;
            }
        }

        offset += (size_t)n;
    }

    return OK;
}

bool ARTSPConnection::receiveLine(AString *line) {
    line->clear();

    bool sawCR = false;
#ifndef ANDROID_DEFAULT_CODE 
    char lastChar;
#endif
    for (;;) {
        char c;
        if (receive(&c, 1) != OK) {
            return false;
        }

#ifndef ANDROID_DEFAULT_CODE 
		//add support of regarding '\n' or '\r\n' as end mark of a line  
		if(c == '\n'){
			if(sawCR && lastChar == '\r'){
				 line->erase(line->size() - 1, 1);  
			}
			return true;		
		}
#else
        if (sawCR && c == '\n') {
            line->erase(line->size() - 1, 1);
            return true;
        }
#endif

        line->append(&c, 1);

#ifndef ANDROID_DEFAULT_CODE
		lastChar = c;
#endif

        if (c == '$' && line->size() == 1) {
            // Special-case for interleaved binary data.
            return true;
        }

        sawCR = (c == '\r');
    }
}

sp<ABuffer> ARTSPConnection::receiveBinaryData() {
    uint8_t x[3];
    if (receive(x, 3) != OK) {
        return NULL;
    }

    sp<ABuffer> buffer = new ABuffer((x[1] << 8) | x[2]);
    if (receive(buffer->data(), buffer->size()) != OK) {
        return NULL;
    }

    buffer->meta()->setInt32("index", (int32_t)x[0]);

    return buffer;
}

static bool IsRTSPVersion(const AString &s) {
#ifndef ANDROID_DEFAULT_CODE
    return s.find("/1.0") >= 0;
#endif
    return s == "RTSP/1.0";
}

bool ARTSPConnection::receiveRTSPReponse() {
    AString statusLine;

    if (!receiveLine(&statusLine)) {
        return false;
    }

    if (statusLine == "$") {
#ifndef ANDROID_DEFAULT_CODE 
        mKeepTCPTrying = true;
#endif // #ifndef ANDROID_DEFAULT_CODE
        sp<ABuffer> buffer = receiveBinaryData();
#ifndef ANDROID_DEFAULT_CODE 
        mKeepTCPTrying = false;
#endif // #ifndef ANDROID_DEFAULT_CODE

        if (buffer == NULL) {
            return false;
        }

        if (mObserveBinaryMessage != NULL) {
            sp<AMessage> notify = mObserveBinaryMessage->dup();
            notify->setBuffer("buffer", buffer);
            notify->post();
        } else {
            ALOGW("received binary data, but no one cares.");
        }

        return true;
    }

    sp<ARTSPResponse> response = new ARTSPResponse;
    response->mStatusLine = statusLine;

    ALOGI("status: %s", response->mStatusLine.c_str());

    ssize_t space1 = response->mStatusLine.find(" ");
    if (space1 < 0) {
        return false;
    }
    ssize_t space2 = response->mStatusLine.find(" ", space1 + 1);
    if (space2 < 0) {
        return false;
    }

    bool isRequest = false;

    if (!IsRTSPVersion(AString(response->mStatusLine, 0, space1))) {
        CHECK(IsRTSPVersion(
                    AString(
                        response->mStatusLine,
                        space2 + 1,
                        response->mStatusLine.size() - space2 - 1)));

        isRequest = true;

        response->mStatusCode = 0;
    } else {
        AString statusCodeStr(
                response->mStatusLine, space1 + 1, space2 - space1 - 1);

        if (!ParseSingleUnsignedLong(
                    statusCodeStr.c_str(), &response->mStatusCode)
                || response->mStatusCode < 100 || response->mStatusCode > 999) {
            return false;
        }
    }

    AString line;
    ssize_t lastDictIndex = -1;
    for (;;) {
        if (!receiveLine(&line)) {
            break;
        }

        if (line.empty()) {
            break;
        }

#ifndef ANDROID_DEFAULT_CODE
        ALOGI("line: '%s'", line.c_str());
#else
        ALOGV("line: '%s'", line.c_str());
#endif

        if (line.c_str()[0] == ' ' || line.c_str()[0] == '\t') {
            // Support for folded header values.

            if (lastDictIndex < 0) {
                // First line cannot be a continuation of the previous one.
                return false;
            }

            AString &value = response->mHeaders.editValueAt(lastDictIndex);
            value.append(line);

            continue;
        }

        ssize_t colonPos = line.find(":");
        if (colonPos < 0) {
            // Malformed header line.
            return false;
        }

        AString key(line, 0, colonPos);
        key.trim();
        key.tolower();

        line.erase(0, colonPos + 1);

        lastDictIndex = response->mHeaders.add(key, line);
    }

    for (size_t i = 0; i < response->mHeaders.size(); ++i) {
        response->mHeaders.editValueAt(i).trim();
    }

    unsigned long contentLength = 0;

    ssize_t i = response->mHeaders.indexOfKey("content-length");

    if (i >= 0) {
        AString value = response->mHeaders.valueAt(i);
        if (!ParseSingleUnsignedLong(value.c_str(), &contentLength)) {
            return false;
        }
    }

    if (contentLength > 0) {
        response->mContent = new ABuffer(contentLength);

        if (receive(response->mContent->data(), contentLength) != OK) {
            return false;
        }
    }

    if (response->mStatusCode == 401) {
        if (mAuthType == NONE && mUser.size() > 0
                && parseAuthMethod(response)) {
            ssize_t i;
            CHECK_EQ((status_t)OK, findPendingRequest(response, &i));
            CHECK_GE(i, 0);

            sp<AMessage> reply = mPendingRequests.valueAt(i);
            mPendingRequests.removeItemsAt(i);

            AString request;
            CHECK(reply->findString("original-request", &request));

            sp<AMessage> msg = new AMessage(kWhatSendRequest, id());
            msg->setMessage("reply", reply);
            msg->setString("request", request.c_str(), request.size());

            ALOGI("re-sending request with authentication headers...");
            onSendRequest(msg);

            return true;
        }
    }

    return isRequest
        ? handleServerRequest(response)
        : notifyResponseListener(response);
}

bool ARTSPConnection::handleServerRequest(const sp<ARTSPResponse> &request) {
    // Implementation of server->client requests is optional for all methods
    // but we do need to respond, even if it's just to say that we don't
    // support the method.

    ssize_t space1 = request->mStatusLine.find(" ");
    CHECK_GE(space1, 0);

    AString response;
    response.append("RTSP/1.0 501 Not Implemented\r\n");

    ssize_t i = request->mHeaders.indexOfKey("cseq");

    if (i >= 0) {
        AString value = request->mHeaders.valueAt(i);

        unsigned long cseq;
        if (!ParseSingleUnsignedLong(value.c_str(), &cseq)) {
            return false;
        }

        response.append("CSeq: ");
        response.append(cseq);
        response.append("\r\n");
    }

    response.append("\r\n");

    size_t numBytesSent = 0;
    while (numBytesSent < response.size()) {
        ssize_t n =
#ifndef ANDROID_DEFAULT_CODE
            send(mSocket, response.c_str() + numBytesSent,
                 response.size() - numBytesSent, MTK_SEND_FLAG);
#else
            send(mSocket, response.c_str() + numBytesSent,
                 response.size() - numBytesSent, 0);
#endif

        if (n < 0 && errno == EINTR) {
            continue;
        }

        if (n <= 0) {
            if (n == 0) {
                // Server closed the connection.
                ALOGE("Server unexpectedly closed the connection.");
            } else {
                ALOGE("Error sending rtsp response (%s).", strerror(errno));
            }

            performDisconnect();

            return false;
        }

        numBytesSent += (size_t)n;
    }

    return true;
}

// static
bool ARTSPConnection::ParseSingleUnsignedLong(
        const char *from, unsigned long *x) {
    char *end;
    *x = strtoul(from, &end, 10);

    if (end == from || *end != '\0') {
        return false;
    }

    return true;
}

status_t ARTSPConnection::findPendingRequest(
        const sp<ARTSPResponse> &response, ssize_t *index) const {
    *index = 0;

    ssize_t i = response->mHeaders.indexOfKey("cseq");

    if (i < 0) {
#ifndef ANDROID_DEFAULT_CODE 
        // still need to set index for notifyResponseListener
        *index = i;
#endif // #ifndef ANDROID_DEFAULT_CODE
        // This is an unsolicited server->client message.
        return OK;
    }

    AString value = response->mHeaders.valueAt(i);

    unsigned long cseq;
    if (!ParseSingleUnsignedLong(value.c_str(), &cseq)) {
        return ERROR_MALFORMED;
    }

    i = mPendingRequests.indexOfKey(cseq);

    if (i < 0) {
        return -ENOENT;
    }

    *index = i;

    return OK;
}

bool ARTSPConnection::notifyResponseListener(
        const sp<ARTSPResponse> &response) {
    ssize_t i;
    status_t err = findPendingRequest(response, &i);

    if (err == OK && i < 0) {
        // An unsolicited server response is not a problem.
        return true;
    }

    if (err != OK) {
        return false;
    }

    sp<AMessage> reply = mPendingRequests.valueAt(i);
    mPendingRequests.removeItemsAt(i);

    reply->setInt32("result", OK);
    reply->setObject("response", response);
    reply->post();

    return true;
}

bool ARTSPConnection::parseAuthMethod(const sp<ARTSPResponse> &response) {
    ssize_t i = response->mHeaders.indexOfKey("www-authenticate");

    if (i < 0) {
        return false;
    }

    AString value = response->mHeaders.valueAt(i);

    if (!strncmp(value.c_str(), "Basic", 5)) {
        mAuthType = BASIC;
    } else {
#if !defined(HAVE_ANDROID_OS)
        // We don't have access to the MD5 implementation on the simulator,
        // so we won't support digest authentication.
        return false;
#endif

        CHECK(!strncmp(value.c_str(), "Digest", 6));
        mAuthType = DIGEST;

#ifndef ANDROID_DEFAULT_CODE 
        {
            i = value.find("realm=");
            CHECK_GE(i, 0);
            CHECK_EQ(value.c_str()[i + 6], '\"');
            ssize_t j = value.find("\"", i + 7);
            CHECK_GE(j, 0);

            mRealm.setTo(value, i + 7, j - i - 7);
        }
#endif // #ifndef ANDROID_DEFAULT_CODE
        i = value.find("nonce=");
        CHECK_GE(i, 0);
        CHECK_EQ(value.c_str()[i + 6], '\"');
        ssize_t j = value.find("\"", i + 7);
        CHECK_GE(j, 0);

        mNonce.setTo(value, i + 7, j - i - 7);
    }

    return true;
}

#if defined(HAVE_ANDROID_OS)
static void H(const AString &s, AString *out) {
    out->clear();

    MD5_CTX m;
    MD5_Init(&m);
    MD5_Update(&m, s.c_str(), s.size());

    uint8_t key[16];
    MD5_Final(key, &m);

    for (size_t i = 0; i < 16; ++i) {
        char nibble = key[i] >> 4;
        if (nibble <= 9) {
            nibble += '0';
        } else {
            nibble += 'a' - 10;
        }
        out->append(&nibble, 1);

        nibble = key[i] & 0x0f;
        if (nibble <= 9) {
            nibble += '0';
        } else {
            nibble += 'a' - 10;
        }
        out->append(&nibble, 1);
    }
}
#endif

static void GetMethodAndURL(
        const AString &request, AString *method, AString *url) {
    ssize_t space1 = request.find(" ");
    CHECK_GE(space1, 0);

    ssize_t space2 = request.find(" ", space1 + 1);
    CHECK_GE(space2, 0);

    method->setTo(request, 0, space1);
#ifndef ANDROID_DEFAULT_CODE 
    url->setTo(request, space1 + 1, space2 - space1 - 1);
#else
    url->setTo(request, space1 + 1, space2 - space1);
#endif // #ifndef ANDROID_DEFAULT_CODE
}

void ARTSPConnection::addAuthentication(AString *request) {
    if (mAuthType == NONE) {
        return;
    }

    // Find the boundary between headers and the body.
    ssize_t i = request->find("\r\n\r\n");
    CHECK_GE(i, 0);

    if (mAuthType == BASIC) {
        AString tmp;
        tmp.append(mUser);
        tmp.append(":");
        tmp.append(mPass);

        AString out;
        encodeBase64(tmp.c_str(), tmp.size(), &out);

        AString fragment;
        fragment.append("Authorization: Basic ");
        fragment.append(out);
        fragment.append("\r\n");

        request->insert(fragment, i + 2);

        return;
    }

#if defined(HAVE_ANDROID_OS)
    CHECK_EQ((int)mAuthType, (int)DIGEST);

    AString method, url;
    GetMethodAndURL(*request, &method, &url);

    AString A1;
    A1.append(mUser);
    A1.append(":");
#ifndef ANDROID_DEFAULT_CODE 
    A1.append(mRealm);
#else
    A1.append("Streaming Server");
#endif // #ifndef ANDROID_DEFAULT_CODE
    A1.append(":");
    A1.append(mPass);

    AString A2;
    A2.append(method);
    A2.append(":");
    A2.append(url);

    AString HA1, HA2;
    H(A1, &HA1);
    H(A2, &HA2);

    AString tmp;
    tmp.append(HA1);
    tmp.append(":");
    tmp.append(mNonce);
    tmp.append(":");
    tmp.append(HA2);

    AString digest;
    H(tmp, &digest);

    AString fragment;
    fragment.append("Authorization: Digest ");
#ifdef ANDROID_DEFAULT_CODE 
    fragment.append("nonce=\"");
    fragment.append(mNonce);
    fragment.append("\", ");
#endif // #ifndef ANDROID_DEFAULT_CODE
    fragment.append("username=\"");
    fragment.append(mUser);
    fragment.append("\", ");
#ifndef ANDROID_DEFAULT_CODE 
    fragment.append("realm=\"");
    fragment.append(mRealm);
    fragment.append("\", ");
    fragment.append("nonce=\"");
    fragment.append(mNonce);
    fragment.append("\", ");
#endif // #ifndef ANDROID_DEFAULT_CODE
    fragment.append("uri=\"");
    fragment.append(url);
    fragment.append("\", ");
    fragment.append("response=\"");
    fragment.append(digest);
    fragment.append("\"");
    fragment.append("\r\n");

    request->insert(fragment, i + 2);
#endif
}

#ifndef ANDROID_DEFAULT_CODE 
// static
void ARTSPConnection::MakeUserAgent(AString *userAgent) {
    userAgent->clear();
#ifdef CUSTOM_UASTRING_FROM_PROPERTY
	char value[MAX_VALUE_LEN];
	if (0 < custom_get_string(MODULE_RTSP_STREAMING, USER_AGENT, value, NULL)) {
        userAgent->setTo("User-Agent: ");
        userAgent->append(value);
        userAgent->append("\r\n");
	} else {
	    userAgent->setTo("User-Agent: stagefright/1.1 (Linux;Android ");
	    #if (PROPERTY_VALUE_MAX < 8)
	    #error "PROPERTY_VALUE_MAX must be at least 8"
	    #endif
	    char prop[PROPERTY_VALUE_MAX];
	    property_get("ro.build.version.release", prop, "Unknown");
	    userAgent->append(prop);
	    userAgent->append(")\r\n");
	}
	memset(value, 0, sizeof(value));
	if (0 < custom_get_string(MODULE_RTSP_STREAMING, UAPROF_URL, value, NULL)) {
		userAgent->append("x-wap-profile: ");
		userAgent->append(value);
		userAgent->append("\r\n");
	} else {
	    userAgent->append("x-wap-profile: http://218.249.47.94/Xianghe/MTK_Athens15_UAProfile.xml\r\n");
	}
#else //#ifdef CUSTOM_UASTRING_FROM_PROPERTY  
#ifdef STR_RTSP_USER_AGENT
    //userAgent->setTo("User-Agent: Athens15_TD/V2 Linux/3.0.13 Android/4.0 Release/02.15.2012 Browser/AppleWebKit534.30 Mobile Safari/534.30 MBBMS/2.2\r\n");
    userAgent->setTo("User-Agent: ");
    userAgent->append(STR_RTSP_USER_AGENT);
    userAgent->append("\r\n");
#else
    userAgent->setTo("User-Agent: stagefright/1.1 (Linux;Android ");
    #if (PROPERTY_VALUE_MAX < 8)
    #error "PROPERTY_VALUE_MAX must be at least 8"
    #endif
    char value[PROPERTY_VALUE_MAX];
    property_get("ro.build.version.release", value, "Unknown");
    userAgent->append(value);
    userAgent->append(")\r\n");
#endif

#ifdef STR_RTSP_WAP_PROFILE
    userAgent->append("x-wap-profile: ");
    userAgent->append(STR_RTSP_WAP_PROFILE);
    userAgent->append("\r\n");
#else
    userAgent->append("x-wap-profile: http://218.249.47.94/Xianghe/MTK_Athens15_UAProfile.xml\r\n");
#endif
#endif //#ifdef CUSTOM_UASTRING_FROM_PROPERTY
}

#else	//#ifndef ANDROID_DEFAULT_CODE
// static
void ARTSPConnection::MakeUserAgent(AString *userAgent) {
    userAgent->clear();

    userAgent->setTo("User-Agent: stagefright/1.1 (Linux;Android ");
#if (PROPERTY_VALUE_MAX < 8)
#error "PROPERTY_VALUE_MAX must be at least 8"
#endif
    char value[PROPERTY_VALUE_MAX];
    property_get("ro.build.version.release", value, "Unknown");
    userAgent->append(value);
    userAgent->append(")\r\n");
}
#endif	//#ifndef ANDROID_DEFAULT_CODE

void ARTSPConnection::addUserAgent(AString *request) const {
    // Find the boundary between headers and the body.
    ssize_t i = request->find("\r\n\r\n");
    CHECK_GE(i, 0);

    request->insert(mUserAgent, i + 2);
}

#ifndef ANDROID_DEFAULT_CODE 
void ARTSPConnection::onTimeout(unsigned long cseq) {
    ssize_t i = mPendingRequests.indexOfKey(cseq);
    if (i < 0) {
        return;
    }
    sp<AMessage> reply = mPendingRequests.valueAt(i);
    mPendingRequests.removeItemsAt(i);

    reply->setInt32("result", -110 /*ETIMEDOUT*/);
    reply->post();
}

void ARTSPConnection::injectPacket(const sp<ABuffer> &buffer) {
    sp<AMessage> msg = new AMessage(kWhatInjectPacket, id());
    msg->setObject("buffer", buffer);
    msg->post();
}

void ARTSPConnection::onInjectPacket(const sp<AMessage> &msg) {
    sp<RefBase> obj;
    CHECK(msg->findObject("buffer", &obj));

    sp<ABuffer> buffer = static_cast<ABuffer *>(obj.get());
#ifndef ANDROID_DEFAULT_CODE
    ssize_t n = send(mSocket, buffer->data(), buffer->size(), MTK_SEND_FLAG);
#else
    ssize_t n = send(mSocket, buffer->data(), buffer->size(), 0);
#endif
    if (n == 0) {
        ALOGW("Server unexpectedly closed the connection when sending RTCP.");
    } else if (n < 0) {
        ALOGW("errno when sending RTCP %d (%s)", errno, strerror(errno));
    }
    return;
}

void ARTSPConnection::stopTCPTrying() {
    ALOGI("stopTCPTrying");
    mForceQuitTCP = true;
}
#endif // #ifndef ANDROID_DEFAULT_CODE
}  // namespace android
