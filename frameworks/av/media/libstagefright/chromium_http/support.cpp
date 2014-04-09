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
#define LOG_TAG "ChromiumHTTPDataSourceSupport"
#include <utils/Log.h>

#include <media/stagefright/foundation/AString.h>

#include "support.h"

#include "android/net/android_network_library_impl.h"
#include "base/logging.h"
#include "base/threading/thread.h"
#include "net/base/cert_verifier.h"
#include "net/base/cookie_monster.h"
#include "net/base/host_resolver.h"
#include "net/base/ssl_config_service.h"
#include "net/http/http_auth_handler_factory.h"
#include "net/http/http_cache.h"
#include "net/proxy/proxy_config_service_android.h"

#include "include/ChromiumHTTPDataSource.h"

#include <cutils/log.h>
#include <cutils/properties.h>
#include <media/stagefright/MediaErrors.h>
#include <string>

#ifndef ANDROID_DEFAULT_CODE
#ifdef CUSTOM_UASTRING_FROM_PROPERTY 
#include "custom_prop.h"
#endif
#endif


namespace android {

static Mutex gNetworkThreadLock;
static base::Thread *gNetworkThread = NULL;
static scoped_refptr<net::URLRequestContext> gReqContext;
static scoped_ptr<net::NetworkChangeNotifier> gNetworkChangeNotifier;

#ifndef ANDROID_DEFAULT_CODE
static AString gCustomizeHeaders = "";
#endif

#ifdef HTTP_STREAM_SUPPORT_PROXY
static scoped_ptr<net::ProxyConfigServiceAndroid>   gProxyConfigService;
#endif

bool logMessageHandler(
        int severity,
        const char* file,
        int line,
        size_t message_start,
        const std::string& str) {
    int androidSeverity = ANDROID_LOG_VERBOSE;
    switch(severity) {
    case logging::LOG_FATAL:
        androidSeverity = ANDROID_LOG_FATAL;
        break;
    case logging::LOG_ERROR_REPORT:
    case logging::LOG_ERROR:
        androidSeverity = ANDROID_LOG_ERROR;
        break;
    case logging::LOG_WARNING:
        androidSeverity = ANDROID_LOG_WARN;
        break;
    default:
        androidSeverity = ANDROID_LOG_VERBOSE;
        break;
    }
    android_printLog(androidSeverity, "chromium-libstagefright",
                    "%s:%d: %s", file, line, str.c_str());
    return false;
}
static void MY_LOGI(const char *s) {
    LOG_PRI(ANDROID_LOG_INFO, LOG_TAG, "%s", s);
}

static void MY_LOGV(const char *s) {
#if !defined(LOG_NDEBUG) || LOG_NDEBUG == 0
    LOG_PRI(ANDROID_LOG_VERBOSE, LOG_TAG, "%s", s);
#endif
}

static void MY_LOGD(const char *s) {
    LOG_PRI(ANDROID_LOG_INFO, LOG_TAG, "%s", s);
}

struct AutoPrioritySaver {
    AutoPrioritySaver()
        : mTID(androidGetTid()),
          mPrevPriority(androidGetThreadPriority(mTID)) {
        androidSetThreadPriority(mTID, ANDROID_PRIORITY_NORMAL);
    }

    ~AutoPrioritySaver() {
        androidSetThreadPriority(mTID, mPrevPriority);
    }

private:
    pid_t mTID;
    int mPrevPriority;

    DISALLOW_EVIL_CONSTRUCTORS(AutoPrioritySaver);
};

static void InitializeNetworkThreadIfNecessary() {
    Mutex::Autolock autoLock(gNetworkThreadLock);

    if (gNetworkThread == NULL) {
        // Make sure any threads spawned by the chromium framework are
        // running at normal priority instead of inheriting this thread's.
        AutoPrioritySaver saver;

        gNetworkThread = new base::Thread("network");
        base::Thread::Options options;
        options.message_loop_type = MessageLoop::TYPE_IO;
        CHECK(gNetworkThread->StartWithOptions(options));

        gReqContext = new SfRequestContext;

        gNetworkChangeNotifier.reset(net::NetworkChangeNotifier::Create());

        net::AndroidNetworkLibrary::RegisterSharedInstance(
                new SfNetworkLibrary);
        logging::SetLogMessageHandler(logMessageHandler);
    } else {
        MY_LOGD("@debug: gNetworkThread already exist");
    }
}


SfNetLog::SfNetLog()
    : mNextID(1) {
}

void SfNetLog::AddEntry(
        EventType type,
        const base::TimeTicks &time,
        const Source &source,
        EventPhase phase,
        EventParameters *params) {
#if 0
    MY_LOGI(StringPrintf(
                "AddEntry time=%s type=%s source=%s phase=%s\n",
                TickCountToString(time).c_str(),
                EventTypeToString(type),
                SourceTypeToString(source.type),
                EventPhaseToString(phase)).c_str());
#endif
}

uint32 SfNetLog::NextID() {
    return mNextID++;
}

net::NetLog::LogLevel SfNetLog::GetLogLevel() const {
    return LOG_ALL;
}

////////////////////////////////////////////////////////////////////////////////

SfRequestContext::SfRequestContext() {
#if (PROPERTY_VALUE_MAX < 8)
#error "PROPERTY_VALUE_MAX must be at least 8"
#endif


#ifndef ANDROID_DEFAULT_CODE
    mWapProfile = "";
    mUserAgent = "";
    initStreamingInfo();
#else
    AString ua;
    ua.append("stagefright/1.2 (Linux;Android ");

    char value[PROPERTY_VALUE_MAX];
    property_get("ro.build.version.release", value, "Unknown");
    ua.append(value);
    ua.append(")");

    mUserAgent = ua.c_str();


#endif



    set_net_log(new SfNetLog());

    set_host_resolver(
        net::CreateSystemHostResolver(
                net::HostResolver::kDefaultParallelism,
                NULL /* resolver_proc */,
                net_log()));

    set_ssl_config_service(
        net::SSLConfigService::CreateSystemSSLConfigService());


#ifdef HTTP_STREAM_SUPPORT_PROXY
	gProxyConfigService.reset( new (net::ProxyConfigServiceAndroid));
	
    set_proxy_service(net::ProxyService::CreateWithoutProxyResolver(
        gProxyConfigService.get(), net_log()));
#else
    set_proxy_service(net::ProxyService::CreateWithoutProxyResolver(
        new net::ProxyConfigServiceAndroid, net_log()));
#endif
    set_http_transaction_factory(new net::HttpCache(
            host_resolver(),
            new net::CertVerifier(),
            dnsrr_resolver(),
            dns_cert_checker(),
            proxy_service(),
            ssl_config_service(),
            net::HttpAuthHandlerFactory::CreateDefault(host_resolver()),
            network_delegate(),
            net_log(),
            NULL));  // backend_factory

    set_cookie_store(new net::CookieMonster(NULL, NULL));
}

const std::string &SfRequestContext::GetUserAgent(const GURL &url) const {
    return mUserAgent;
}

#ifndef ANDROID_DEFAULT_CODE


#if 0
#define STREAMING_CFG_PATH "/system/etc/player.cfg"
#define STREAMING_CFG_MAX_LENGTH 128
#endif

void SfRequestContext::initStreamingInfo() {   
#ifdef CUSTOM_UASTRING_FROM_PROPERTY
	char value[MAX_VALUE_LEN];
	if (0 < custom_get_string(MODULE_HTTP_STREAMING, USER_AGENT, value, NULL)) {
        mUserAgent = value;
	} else {
	    mUserAgent = "";
	    mUserAgent.append("stagefright/1.2 (Linux;Android ");

    	char prop[PROPERTY_VALUE_MAX];
	    property_get("ro.build.version.release", prop, "Unknown");
	    mUserAgent.append(prop);
	    mUserAgent.append(")");
	}
    mWapProfile = "";
	memset(value, 0, sizeof(value));
	if (0 < custom_get_string(MODULE_HTTP_STREAMING, UAPROF_URL, value, NULL)) {
		mWapProfile = value;
	    gCustomizeHeaders = StringPrintf("x-wap-profile: %s", mWapProfile.c_str());
        MY_LOGD(gCustomizeHeaders.c_str());	
	}
#else //#ifdef CUSTOM_UASTRING_FROM_PROPERTY
#ifdef STR_HTTP_USER_AGENT
    mUserAgent = STR_HTTP_USER_AGENT;
#else
    mUserAgent = "";
    mUserAgent.append("stagefright/1.2 (Linux;Android ");

    char value[PROPERTY_VALUE_MAX];
    property_get("ro.build.version.release", value, "Unknown");
    mUserAgent.append(value);
    mUserAgent.append(")");
#endif

#ifdef STR_HTTP_WAP_PROFILE
    mWapProfile = STR_HTTP_WAP_PROFILE;
    if (mWapProfile != "") {
        gCustomizeHeaders = StringPrintf("x-wap-profile: %s", mWapProfile.c_str());
        MY_LOGD(gCustomizeHeaders.c_str());
    }
#else
    mWapProfile = "";
#endif
#endif  //#ifdef CUSTOM_UASTRING_FROM_PROPERTY

    MY_LOGD(StringPrintf("http user agent: %s", mUserAgent.c_str()).c_str());

    MY_LOGD(StringPrintf("http wap profile: %s", mWapProfile.c_str()).c_str());

#if 0
    //loading from cfg file
    const char filename[] = STREAMING_CFG_PATH;
    FILE *pFile = fopen(filename, "r");
    bool bValidCfg = false;
    if (pFile != NULL) {
        char szString[STREAMING_CFG_MAX_LENGTH];
        char* szName = NULL;
        char* szValue = NULL;

        MY_LOGD(StringPrintf("loading streaming cfg: %s", filename).c_str());
        while (fgets(szString, STREAMING_CFG_MAX_LENGTH, pFile) != NULL) {
            szName = strtok(szString, "=");
            szValue = strtok(NULL, "\r\n");

            MY_LOGD(StringPrintf("\t\t%s = %s", szName, szValue).c_str());

            //load user-agent
            if (!strcmp(szName, "User-Agent")) {
                mUserAgent = szValue;
                bValidCfg = true;
            } else if (!strcmp(szName, "x-wap-profile")) { //load x-wap-profile
                mWapProfile = szValue;
            }
        }
        fclose(pFile);
    } else {
        MY_LOGD(StringPrintf("\t\tstreaming config file %s not found", filename).c_str());
    }
#endif

}

#endif

////////////////////////////////////////////////////////////////////////////////

SfNetworkLibrary::SfNetworkLibrary() {}

SfNetworkLibrary::VerifyResult SfNetworkLibrary::VerifyX509CertChain(
        const std::vector<std::string>& cert_chain,
        const std::string& hostname,
        const std::string& auth_type) {
    return VERIFY_OK;
}

////////////////////////////////////////////////////////////////////////////////

SfDelegate::SfDelegate()
    : mOwner(NULL),
      mURLRequest(NULL),
      mReadBuffer(new net::IOBufferWithSize(8192)),
      mNumBytesRead(0),
      mNumBytesTotal(0),
      mDataDestination(NULL),
      mAtEOS(false) {
    InitializeNetworkThreadIfNecessary();
}

SfDelegate::~SfDelegate() {
    CHECK(mURLRequest == NULL);
}

void SfDelegate::setOwner(ChromiumHTTPDataSource *owner) {
    mOwner = owner;
}

void SfDelegate::setUID(uid_t uid) {
    gReqContext->setUID(uid);
}

bool SfDelegate::getUID(uid_t *uid) const {
    return gReqContext->getUID(uid);
}
#ifdef HTTP_STREAM_SUPPORT_PROXY 
void SfDelegate::setProxy(const char *proxy, int port)
{
       std::string host = proxy;
	 std::string list = "";

	 MY_LOGD(StringPrintf("setProxy:host=%s,port=%d",host.c_str() ,port).c_str());
    
	gProxyConfigService.get()->UpdateProxySettings(host,list);

}
#endif	
void SfDelegate::OnReceivedRedirect(
            net::URLRequest *request, const GURL &new_url, bool *defer_redirect) {

#ifndef ANDROID_DEFAULT_CODE
     const char *uri =NULL;   
      uri = new_url.spec().c_str();
      MY_LOGD(StringPrintf("OnReceivedRedirect,original_url=%s ",request->original_url().spec().c_str()).c_str());
      MY_LOGD(StringPrintf("OnReceivedRedirect, new_url=%s", new_url.spec().c_str()).c_str());
      MY_LOGD(StringPrintf("OnReceivedRedirect,defer_redirect=%d",*defer_redirect).c_str());
    
      mOwner->OnReceivedRedirect( uri);
#else
	MY_LOGD("OnReceivedRedirect");
#endif
}

void SfDelegate::OnAuthRequired(
            net::URLRequest *request, net::AuthChallengeInfo *auth_info) {
    MY_LOGD("OnAuthRequired");

    inherited::OnAuthRequired(request, auth_info);
}

void SfDelegate::OnCertificateRequested(
            net::URLRequest *request, net::SSLCertRequestInfo *cert_request_info) {
    MY_LOGD("OnCertificateRequested");

    inherited::OnCertificateRequested(request, cert_request_info);
}

void SfDelegate::OnSSLCertificateError(
            net::URLRequest *request, int cert_error, net::X509Certificate *cert) {
    fprintf(stderr, "OnSSLCertificateError cert_error=%d\n", cert_error);

    inherited::OnSSLCertificateError(request, cert_error, cert);
}

void SfDelegate::OnGetCookies(net::URLRequest *request, bool blocked_by_policy) {
    MY_LOGD("OnGetCookies");
}

void SfDelegate::OnSetCookie(
        net::URLRequest *request,
        const std::string &cookie_line,
        const net::CookieOptions &options,
        bool blocked_by_policy) {
    MY_LOGD("OnSetCookie");
}

void SfDelegate::OnResponseStarted(net::URLRequest *request) {
    if (request->status().status() != net::URLRequestStatus::SUCCESS) {
        MY_LOGI(StringPrintf(
                    "Request failed with status %d and os_error %d",
                    request->status().status(),
                    request->status().os_error()).c_str());

        delete mURLRequest;
        mURLRequest = NULL;

        mOwner->onConnectionFailed(ERROR_IO);
        return;
#ifndef ANDROID_DEFAULT_CODE//Listen.apk, server return 200 when we request partial content
	} else if (mRangeRequested && request->GetResponseCode() != 206 && request->GetResponseCode() != 200) {
#else
    } else if (mRangeRequested && request->GetResponseCode() != 206) {
#endif
        MY_LOGI(StringPrintf(
                    "We requested a content range, but server didn't "
                    "support that. (responded with %d)",
                    request->GetResponseCode()).c_str());

        delete mURLRequest;
        mURLRequest = NULL;

        mOwner->onConnectionFailed(-EPIPE);
        return;
    } else if ((request->GetResponseCode() / 100) != 2) {
        MY_LOGI(StringPrintf(
                    "Server responded with http status %d",
                    request->GetResponseCode()).c_str());
#ifndef ANDROID_DEFAULT_CODE
        bool bResNotFound = false;
        if ((request->GetResponseCode() == 404) || (request->GetResponseCode() == 410)){
            bResNotFound = true;
        }

#endif


        delete mURLRequest;
        mURLRequest = NULL;

#ifndef ANDROID_DEFAULT_CODE
        //if the ResponseCode is 404, notify that it's our_of_range 
       mOwner->onConnectionFailed(bResNotFound ? ERROR_OUT_OF_RANGE : ERROR_IO);
#else
        mOwner->onConnectionFailed(ERROR_IO);
#endif
        return;
    }

    MY_LOGD("OnResponseStarted");

    std::string headers;
    request->GetAllResponseHeaders(&headers);

    MY_LOGD(StringPrintf("response headers: %s", headers.c_str()).c_str());

    std::string contentType;
    request->GetResponseHeaderByName("Content-Type", &contentType);

#ifndef ANDROID_DEFAULT_CODE
	MY_LOGD(StringPrintf("contentType org=%s", contentType.c_str()).c_str());
	size_t pos = contentType.find(';');
	if (pos != std::string::npos)
		contentType.erase(pos);//Remove parameters from content type
	MY_LOGD(StringPrintf("contentType now=%s", contentType.c_str()).c_str());
#endif

    mOwner->onConnectionEstablished(
            request->GetExpectedContentSize(), contentType.c_str());
}

void SfDelegate::OnReadCompleted(net::URLRequest *request, int bytes_read) {
    if (bytes_read == -1) {
        MY_LOGI(StringPrintf(
                    "OnReadCompleted, read failed, status %d",
                    request->status().status()).c_str());

        mOwner->onReadCompleted(ERROR_IO);
        return;
    }

    MY_LOGV(StringPrintf("OnReadCompleted, read %d bytes", bytes_read).c_str());

    if (bytes_read < 0) {
        MY_LOGI(StringPrintf(
                    "Read failed w/ status %d\n",
                    request->status().status()).c_str());

        mOwner->onReadCompleted(ERROR_IO);
        return;
    } else if (bytes_read == 0) {
        mAtEOS = true;
        mOwner->onReadCompleted(mNumBytesRead);
        return;
    }

    CHECK_GT(bytes_read, 0);
    CHECK_LE(mNumBytesRead + bytes_read, mNumBytesTotal);

    memcpy((uint8_t *)mDataDestination + mNumBytesRead,
           mReadBuffer->data(),
           bytes_read);

    mNumBytesRead += bytes_read;

    readMore(request);
}

void SfDelegate::readMore(net::URLRequest *request) {
    while (mNumBytesRead < mNumBytesTotal) {
        size_t copy = mNumBytesTotal - mNumBytesRead;
        if (copy > mReadBuffer->size()) {
            copy = mReadBuffer->size();
        }

        int n;
        if (request->Read(mReadBuffer, copy, &n)) {
            MY_LOGV(StringPrintf("Read %d bytes directly.", n).c_str());

            CHECK_LE((size_t)n, copy);

            memcpy((uint8_t *)mDataDestination + mNumBytesRead,
                   mReadBuffer->data(),
                   n);

            mNumBytesRead += n;

            if (n == 0) {
                mAtEOS = true;
                break;
            }
        } else {
            MY_LOGV("readMore pending read");

            if (request->status().status() != net::URLRequestStatus::IO_PENDING) {
                MY_LOGI(StringPrintf(
                            "Direct read failed w/ status %d\n",
                            request->status().status()).c_str());

                mOwner->onReadCompleted(ERROR_IO);
                return;
            }

            return;
        }
    }

    mOwner->onReadCompleted(mNumBytesRead);
}

void SfDelegate::initiateConnection(
        const char *uri,
        const KeyedVector<String8, String8> *headers,
        off64_t offset) {
    GURL url(uri);

    MessageLoop *loop = gNetworkThread->message_loop();
    loop->PostTask(
            FROM_HERE,
            NewRunnableFunction(
                &SfDelegate::OnInitiateConnectionWrapper,
                this,
                url,
                headers,
                offset));

}

// static
void SfDelegate::OnInitiateConnectionWrapper(
        SfDelegate *me, GURL url,
        const KeyedVector<String8, String8> *headers,
        off64_t offset) {
    me->onInitiateConnection(url, headers, offset);
}

void SfDelegate::onInitiateConnection(
        const GURL &url,
        const KeyedVector<String8, String8> *extra,
        off64_t offset) {
    CHECK(mURLRequest == NULL);

    mURLRequest = new net::URLRequest(url, this);
    mAtEOS = false;

    mRangeRequested = false;

#ifndef ANDROID_DEFAULT_CODE

    if (offset != 0 || extra != NULL || !gCustomizeHeaders.empty()) {
#else
    if (offset != 0 || extra != NULL) {
#endif
        net::HttpRequestHeaders headers =
            mURLRequest->extra_request_headers();

        if (offset != 0) {
            headers.AddHeaderFromString(
                    StringPrintf("Range: bytes=%lld-", offset).c_str());

            mRangeRequested = true;
        }

        if (extra != NULL) {
            for (size_t i = 0; i < extra->size(); ++i) {
                AString s;
                s.append(extra->keyAt(i).string());
                s.append(": ");
                s.append(extra->valueAt(i).string());

                headers.AddHeaderFromString(s.c_str());
            }
        }
#ifndef ANDROID_DEFAULT_CODE
        if (!gCustomizeHeaders.empty()) {
            MY_LOGD(StringPrintf("Customize headers, %s", gCustomizeHeaders.c_str()).c_str());
            headers.AddHeaderFromString(gCustomizeHeaders.c_str());
        }
#endif

        mURLRequest->SetExtraRequestHeaders(headers);
    }

    mURLRequest->set_context(gReqContext);

    mURLRequest->Start();
}

void SfDelegate::initiateDisconnect() {
    MessageLoop *loop = gNetworkThread->message_loop();
    loop->PostTask(
            FROM_HERE,
            NewRunnableFunction(
                &SfDelegate::OnInitiateDisconnectWrapper, this));
}

// static
void SfDelegate::OnInitiateDisconnectWrapper(SfDelegate *me) {
    me->onInitiateDisconnect();
}

void SfDelegate::onInitiateDisconnect() {
    if (mURLRequest == NULL) {
        return;
    }

    mURLRequest->Cancel();

    delete mURLRequest;
    mURLRequest = NULL;

    mOwner->onDisconnectComplete();
}

void SfDelegate::initiateRead(void *data, size_t size) {
    MessageLoop *loop = gNetworkThread->message_loop();
    loop->PostTask(
            FROM_HERE,
            NewRunnableFunction(
                &SfDelegate::OnInitiateReadWrapper, this, data, size));
}

// static
void SfDelegate::OnInitiateReadWrapper(
        SfDelegate *me, void *data, size_t size) {
    me->onInitiateRead(data, size);
}

void SfDelegate::onInitiateRead(void *data, size_t size) {
    CHECK(mURLRequest != NULL);

    mNumBytesRead = 0;
    mNumBytesTotal = size;
    mDataDestination = data;

    if (mAtEOS) {
        mOwner->onReadCompleted(0);
        return;
    }

    readMore(mURLRequest);
}

}  // namespace android

