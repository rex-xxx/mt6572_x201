/*
 * Copyright (C) 2011 The Android Open Source Project
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
#define LOG_TAG "ChromiumHTTPDataSource"
#include <media/stagefright/foundation/ADebug.h>

#include "include/ChromiumHTTPDataSource.h"

#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/MediaErrors.h>

#include "support.h"

#include <cutils/properties.h> // for property_get

namespace android {

ChromiumHTTPDataSource::ChromiumHTTPDataSource(uint32_t flags)
    : mFlags(flags),
      mState(DISCONNECTED),
      mDelegate(new SfDelegate),
      mCurrentOffset(0),
      mIOResult(OK),
      mContentSize(-1),
      mDecryptHandle(NULL),
      mDrmManagerClient(NULL) {
    mDelegate->setOwner(this);
}

ChromiumHTTPDataSource::~ChromiumHTTPDataSource() {
    disconnect();

    delete mDelegate;
    mDelegate = NULL;

    clearDRMState_l();

    if (mDrmManagerClient != NULL) {
        delete mDrmManagerClient;
        mDrmManagerClient = NULL;
    }
}


#ifdef HTTP_STREAM_SUPPORT_PROXY
static const String8 g_strMTKHeaders[] = 
{
    String8("MIN-UDP-PORT"),
    String8("MAX-UDP-PORT"),
    String8("MTK-RTSP-PROXY-HOST"),
    String8("MTK-RTSP-PROXY-PORT"),
    String8("MTK-HTTP-PROXY-HOST"),
    String8("MTK-HTTP-PROXY-PORT")
};
static const int g_strMTKHeadersSize = sizeof(g_strMTKHeaders)/sizeof(String8);

bool IsInStringTable(const String8* strTable, int nTableSize, String8 strFind) {
    for (int i = 0; i < nTableSize; i ++) {
        if (strFind == strTable[i]) {
            return true;
        }
    }
    return false;
}
#endif

status_t ChromiumHTTPDataSource::connect(
        const char *uri,
        const KeyedVector<String8, String8> *headers,
        off64_t offset) {
     
    Mutex::Autolock autoLock(mLock);

    uid_t uid;
    if (getUID(&uid)) {
        mDelegate->setUID(uid);
    }
    LOG_PRI(ANDROID_LOG_VERBOSE, LOG_TAG, "connect on behalf of uid %d", uid);
#ifdef HTTP_STREAM_SUPPORT_PROXY
   String8 headers1;
    char value[PROPERTY_VALUE_MAX];
    property_get("http.streaming.proxy", value, "");

    String8 sProxy = String8(value);
    int nProxyPort = 80;

	KeyedVector<String8, String8> Newheaders;
#if 0
	for (size_t i = 0; i < headers->size(); i++) {
                String8 key = headers->keyAt(i);
                String8 value1 = headers->valueAt(i);
		  LOG_PRI(ANDROID_LOG_DEBUG,LOG_TAG,"old key %s:value %s\n", key.string(), value1.string());
				
		}
#endif	
	
    if ((sProxy == "") || (sProxy == "0"))
    {
    		LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "find proxy setting from headers");
       
        if (headers != NULL) {            

            //find the proxy settings defined in "MTK-***"
            for (size_t i = 0; i < headers->size(); i++) {
                String8 key = headers->keyAt(i);
                String8 value = headers->valueAt(i);

                if (!IsInStringTable(g_strMTKHeaders, g_strMTKHeadersSize, key)) {
                    Newheaders.add(key, headers->valueAt(i));
                } else {
                    if (key == String8("MTK-HTTP-PROXY-HOST")) {
                        sProxy = headers->valueAt(i);
                    } else if (key == String8("MTK-HTTP-PROXY-PORT")) {
                        nProxyPort = atoi(headers->valueAt(i));
                    } 
                }
            }
				
       }
  } else{
    	      property_get("http.streaming.proxy.port", value, "80");
       	 nProxyPort = atoi(value);
		 LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG,"debug proxy setting %s:%d", sProxy.string(), nProxyPort); 
    } 
	if (!((sProxy == "") || (sProxy == "0")) && (nProxyPort != -1))
	{
		mDelegate->setProxy(sProxy.string(), nProxyPort);
		LOG_PRI(ANDROID_LOG_DEBUG,LOG_TAG,"proxy enable %s:%d", sProxy.string(), nProxyPort);
	}
#if 0
	for (size_t i = 0; i < Newheaders.size(); i++) {
                String8 key = Newheaders.keyAt(i);
                String8 value1 = Newheaders.valueAt(i);
		  LOG_PRI(ANDROID_LOG_DEBUG,LOG_TAG,"new key %s:value %s\n", key.string(), value1.string());
				
		}
#endif	
	
	
	return connect_l(uri, &Newheaders, offset);
	
#else
    return connect_l(uri, headers, offset);
#endif

}

status_t ChromiumHTTPDataSource::connect_l(
        const char *uri,
        const KeyedVector<String8, String8> *headers,
        off64_t offset) {
#ifndef ANDROID_DEFAULT_CODE
    while (mState == DISCONNECTING) {
        ALOGD("Waiting previous disconnect complete when connect");
        mCondition.wait(mLock);
    }   
#endif
    
    if (mState != DISCONNECTED) {
        disconnect_l();
    }

    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG,
                "connect to %s @%lld", uri, offset);

    mURI = uri;
    mContentType = String8("application/octet-stream");

    if (headers != NULL) {
        mHeaders = *headers;
    } else {
        mHeaders.clear();
    }

    mState = CONNECTING;
    mContentSize = -1;
    mCurrentOffset = offset;

    mDelegate->initiateConnection(mURI.c_str(), &mHeaders, offset);
    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "waiting to connect ... ");

    while (mState == CONNECTING || mState == DISCONNECTING) {
        mCondition.wait(mLock);
    }

#ifndef ANDROID_DEFAULT_CODE
	if( mState == CONNECT_REDIRECT)
	{
		LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "CONNECT_REDIRECT!!! mURI=%s",mURI.c_str());
		return connect_l(mURI.c_str(),&mHeaders,mCurrentOffset);
	}
		
#endif
    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "\t\tconnect done");
    return mState == CONNECTED ? OK : mIOResult;
}
#ifndef ANDROID_DEFAULT_CODE
void ChromiumHTTPDataSource::OnReceivedRedirect(const char *uri)
{
	  Mutex::Autolock autoLock(mLock);
	  mURI = uri;
	if (mState == CONNECTING) 
	{
		 mState = CONNECT_REDIRECT;
  	 	 mIOResult = -EINTR;	 
		 mCondition.broadcast();
		 LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "ChromiumHTTPDataSource::OnReceivedRedirect:initiateDisconnect,mState=%d",mState);
	}
}
#endif

void ChromiumHTTPDataSource::onConnectionEstablished(
        int64_t contentSize, const char *contentType) {
    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "onConnectEstablished, mState = %d", (int)mState);
    Mutex::Autolock autoLock(mLock);

    if (mState != CONNECTING) {
        // We may have initiated disconnection.
        CHECK_EQ(mState, DISCONNECTING);
        return;
    }
#ifndef ANDROID_DEFAULT_CODE
    if (mState == DISCONNECTING) {
        LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "onConnectEstablished in DISCONNECTING");
    } else {

        mState = CONNECTED;

    }
#else

    mState = CONNECTED;
#endif//#ifndef ANDROID_DEFAULT_CODE
    mContentSize = (contentSize < 0) ? -1 : contentSize + mCurrentOffset;
    mContentType = String8(contentType);
    mCondition.broadcast();
}

void ChromiumHTTPDataSource::onConnectionFailed(status_t err) {
    Mutex::Autolock autoLock(mLock);
    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "onConnectionFailed, err = %d", (int)err);
    mState = DISCONNECTED;
    mCondition.broadcast();

    // mURI.clear();

    mIOResult = err;
}

void ChromiumHTTPDataSource::disconnect() {
    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "disconnect in");

    Mutex::Autolock autoLock(mLock);

    LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "disconnect lock ok");
    disconnect_l();
}

void ChromiumHTTPDataSource::disconnect_l() {
    if ((mState == DISCONNECTED) || (mState == DISCONNECTING)) {
        return;
    }

    if (mState == READING) {
        LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "disconnect when reading");
    }

    if (mState == CONNECTING) {
        LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "disconnect when connecting!!!");
    }

    mState = DISCONNECTING;
    mIOResult = -EINTR;

    mDelegate->initiateDisconnect();

    while (mState == DISCONNECTING) {
        mCondition.wait(mLock);
    }
    if (mState != DISCONNECTED) {
        LOG_PRI(ANDROID_LOG_DEBUG, LOG_TAG, "error: disconnect(), mState = %d", (int)mState);
    }

    CHECK_EQ((int)mState, (int)DISCONNECTED);
}

status_t ChromiumHTTPDataSource::initCheck() const {
    Mutex::Autolock autoLock(mLock);

    return mState == CONNECTED ? OK : NO_INIT;
}

ssize_t ChromiumHTTPDataSource::readAt(off64_t offset, void *data, size_t size) {
    Mutex::Autolock autoLock(mLock);

    if (mState != CONNECTED) {
        return INVALID_OPERATION;
    }

#if 0
    char value[PROPERTY_VALUE_MAX];
    if (property_get("media.stagefright.disable-net", value, 0)
            && (!strcasecmp(value, "true") || !strcmp(value, "1"))) {
        LOG_PRI(ANDROID_LOG_INFO, LOG_TAG, "Simulating that the network is down.");
        disconnect_l();
        return ERROR_IO;
    }
#endif

    if (offset != mCurrentOffset) {
        AString tmp = mURI;
        KeyedVector<String8, String8> tmpHeaders = mHeaders;

        disconnect_l();

        status_t err = connect_l(tmp.c_str(), &tmpHeaders, offset);

        if (err != OK) {
            return err;
        }
    }

    mState = READING;

    int64_t startTimeUs = ALooper::GetNowUs();

    mDelegate->initiateRead(data, size);

    while (mState == READING) {
        mCondition.wait(mLock);
    }

    if (mIOResult < OK) {
        return mIOResult;
    }

    if (mState == CONNECTED) {
        int64_t delayUs = ALooper::GetNowUs() - startTimeUs;

        // The read operation was successful, mIOResult contains
        // the number of bytes read.
        addBandwidthMeasurement(mIOResult, delayUs);

        mCurrentOffset += mIOResult;
        return mIOResult;
    }

    return ERROR_IO;
}

void ChromiumHTTPDataSource::onReadCompleted(ssize_t size) {
    Mutex::Autolock autoLock(mLock);

    mIOResult = size;

    if (mState == READING) {
        mState = CONNECTED;
        mCondition.broadcast();
    }
}

status_t ChromiumHTTPDataSource::getSize(off64_t *size) {
    Mutex::Autolock autoLock(mLock);

    if (mContentSize < 0) {
        return ERROR_UNSUPPORTED;
    }

    *size = mContentSize;

    return OK;
}

uint32_t ChromiumHTTPDataSource::flags() {
    return kWantsPrefetching | kIsHTTPBasedSource;
}

// static
void ChromiumHTTPDataSource::InitiateRead(
        ChromiumHTTPDataSource *me, void *data, size_t size) {
    me->initiateRead(data, size);
}

void ChromiumHTTPDataSource::initiateRead(void *data, size_t size) {
    mDelegate->initiateRead(data, size);
}

void ChromiumHTTPDataSource::onDisconnectComplete() {
    Mutex::Autolock autoLock(mLock);
    LOG_PRI(ANDROID_LOG_INFO, LOG_TAG, "onDisconnectComplete()");
    CHECK_EQ((int)mState, (int)DISCONNECTING);

    mState = DISCONNECTED;
    // mURI.clear();
    mIOResult = -ENOTCONN;

    mCondition.broadcast();
}

sp<DecryptHandle> ChromiumHTTPDataSource::DrmInitialization(const char* mime) {
    Mutex::Autolock autoLock(mLock);

    if (mDrmManagerClient == NULL) {
        mDrmManagerClient = new DrmManagerClient();
    }

    if (mDrmManagerClient == NULL) {
        return NULL;
    }

    if (mDecryptHandle == NULL) {
        /* Note if redirect occurs, mUri is the redirect uri instead of the
         * original one
         */
        mDecryptHandle = mDrmManagerClient->openDecryptSession(
                String8(mURI.c_str()), mime);
    }

    if (mDecryptHandle == NULL) {
        delete mDrmManagerClient;
        mDrmManagerClient = NULL;
    }

    return mDecryptHandle;
}

void ChromiumHTTPDataSource::getDrmInfo(
        sp<DecryptHandle> &handle, DrmManagerClient **client) {
    Mutex::Autolock autoLock(mLock);

    handle = mDecryptHandle;
    *client = mDrmManagerClient;
}

String8 ChromiumHTTPDataSource::getUri() {
    Mutex::Autolock autoLock(mLock);

    return String8(mURI.c_str());
}

String8 ChromiumHTTPDataSource::getMIMEType() const {
    Mutex::Autolock autoLock(mLock);

    return mContentType;
}

void ChromiumHTTPDataSource::clearDRMState_l() {
    if (mDecryptHandle != NULL) {
        // To release mDecryptHandle
        CHECK(mDrmManagerClient);
        mDrmManagerClient->closeDecryptSession(mDecryptHandle);
        mDecryptHandle = NULL;
    }
}

status_t ChromiumHTTPDataSource::reconnectAtOffset(off64_t offset) {
    Mutex::Autolock autoLock(mLock);

    if (mURI.empty()) {
        return INVALID_OPERATION;
    }

    LOG_PRI(ANDROID_LOG_INFO, LOG_TAG, "Reconnecting...");
    status_t err = connect_l(mURI.c_str(), &mHeaders, offset);
    if (err != OK) {
        LOG_PRI(ANDROID_LOG_INFO, LOG_TAG, "Reconnect failed w/ err 0x%08x", err);
    }

    return err;
}

}  // namespace android

