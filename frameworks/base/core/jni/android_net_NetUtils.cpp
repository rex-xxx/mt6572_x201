/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "NetUtils"

#include "jni.h"
#include <utils/misc.h>
#include <android_runtime/AndroidRuntime.h>
#include <utils/Log.h>
#include <arpa/inet.h>
#include <cutils/properties.h>

/// M: support MTU configuration function in JNI
#include <sys/socket.h>
#include <sys/ioctl.h> 
#include <net/if.h> 

extern "C" {
int ifc_enable(const char *ifname);
int ifc_disable(const char *ifname);
int ifc_reset_connections(const char *ifname, int reset_mask);

/// M: support MTU configuration function in JNI
static jboolean android_net_utils_setMtuByInterface(JNIEnv* env, jobject clazz, jstring ifname, jint mtu);
/// M: support reset socket connections by the UID of process
int ifc_reset_connection_by_uid(int uid, int err);

int dhcp_do_request(const char *ifname,
                    const char *ipaddr,
                    const char *gateway,
                    uint32_t *prefixLength,
                    const char *dns1,
                    const char *dns2,
                    const char *server,
                    uint32_t *lease,
                    const char *vendorInfo);

int dhcp_do_request_renew(const char *ifname,
                    const char *ipaddr,
                    const char *gateway,
                    uint32_t *prefixLength,
                    const char *dns1,
                    const char *dns2,
                    const char *server,
                    uint32_t *lease,
                    const char *vendorInfo);

int dhcp_stop(const char *ifname);
int dhcp_release_lease(const char *ifname);
char *dhcp_get_errmsg();


char *dhcpv6_get_errmsg();

/*add dhcpv6 corresponding c method declaration*/
int dhcpv6_do_request(const char *interface, char *ipaddr,
        char *dns1,
        char *dns2,
        uint32_t *lease);
int dhcpv6_stop(const char *interface);
int dhcpv6_do_request_renew(const char *interface, char *ipaddr,
        char *dns1,
        char *dns2,
        uint32_t *lease);

}


#define NETUTILS_PKG_NAME "android/net/NetworkUtils"

namespace android {

/*
 * The following remembers the jfieldID's of the fields
 * of the DhcpInfo Java object, so that we don't have
 * to look them up every time.
 */
static struct fieldIds {
    jmethodID constructorId;
    jfieldID ipaddress;
    jfieldID prefixLength;
    jfieldID dns1;
    jfieldID dns2;
    jfieldID serverAddress;
    jfieldID leaseDuration;
    jfieldID vendorInfo;
} dhcpInfoInternalFieldIds;

static jint android_net_utils_enableInterface(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_enable(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jint android_net_utils_disableInterface(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_disable(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jint android_net_utils_resetConnections(JNIEnv* env, jobject clazz,
      jstring ifname, jint mask)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);

    ALOGD("android_net_utils_resetConnections in env=%p clazz=%p iface=%s mask=0x%x\n",
          env, clazz, nameStr, mask);

    result = ::ifc_reset_connections(nameStr, mask);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jboolean android_net_utils_runDhcpCommon(JNIEnv* env, jobject clazz, jstring ifname,
        jobject info, bool renew)
{
    int result;
    char  ipaddr[PROPERTY_VALUE_MAX];
    uint32_t prefixLength;
    char gateway[PROPERTY_VALUE_MAX];
    char    dns1[PROPERTY_VALUE_MAX];
    char    dns2[PROPERTY_VALUE_MAX];
    char  server[PROPERTY_VALUE_MAX];
    uint32_t lease;
    char vendorInfo[PROPERTY_VALUE_MAX];

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    if (nameStr == NULL) return (jboolean)false;

    if (renew) {
        result = ::dhcp_do_request_renew(nameStr, ipaddr, gateway, &prefixLength,
                dns1, dns2, server, &lease, vendorInfo);
    } else {
        result = ::dhcp_do_request(nameStr, ipaddr, gateway, &prefixLength,
                dns1, dns2, server, &lease, vendorInfo);
    }

    env->ReleaseStringUTFChars(ifname, nameStr);
    if (result == 0) {
        env->SetObjectField(info, dhcpInfoInternalFieldIds.ipaddress, env->NewStringUTF(ipaddr));

        // set the gateway
        jclass cls = env->FindClass("java/net/InetAddress");
        jmethodID method = env->GetStaticMethodID(cls, "getByName",
                "(Ljava/lang/String;)Ljava/net/InetAddress;");
        jvalue args[1];
        args[0].l = env->NewStringUTF(gateway);
        jobject inetAddressObject = env->CallStaticObjectMethodA(cls, method, args);

        if (!env->ExceptionOccurred()) {
            cls = env->FindClass("android/net/RouteInfo");
            method = env->GetMethodID(cls, "<init>", "(Ljava/net/InetAddress;)V");
            args[0].l = inetAddressObject;
            jobject routeInfoObject = env->NewObjectA(cls, method, args);

            cls = env->FindClass("android/net/DhcpInfoInternal");
            method = env->GetMethodID(cls, "addRoute", "(Landroid/net/RouteInfo;)V");
            args[0].l = routeInfoObject;
            env->CallVoidMethodA(info, method, args);
        } else {
            // if we have an exception (host not found perhaps), just don't add the route
            env->ExceptionClear();
        }

        env->SetIntField(info, dhcpInfoInternalFieldIds.prefixLength, prefixLength);
        env->SetObjectField(info, dhcpInfoInternalFieldIds.dns1, env->NewStringUTF(dns1));
        env->SetObjectField(info, dhcpInfoInternalFieldIds.dns2, env->NewStringUTF(dns2));
        env->SetObjectField(info, dhcpInfoInternalFieldIds.serverAddress,
                env->NewStringUTF(server));
        env->SetIntField(info, dhcpInfoInternalFieldIds.leaseDuration, lease);
        env->SetObjectField(info, dhcpInfoInternalFieldIds.vendorInfo, env->NewStringUTF(vendorInfo));
    }
    return (jboolean)(result == 0);
}

static jboolean android_net_utils_runDhcp(JNIEnv* env, jobject clazz, jstring ifname, jobject info)
{
    return android_net_utils_runDhcpCommon(env, clazz, ifname, info, false);
}

static jboolean android_net_utils_runDhcpRenew(JNIEnv* env, jobject clazz, jstring ifname, jobject info)
{
    return android_net_utils_runDhcpCommon(env, clazz, ifname, info, true);
}


static jboolean android_net_utils_stopDhcp(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcp_stop(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
}

static jboolean android_net_utils_releaseDhcpLease(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcp_release_lease(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
}

static jstring android_net_utils_getDhcpError(JNIEnv* env, jobject clazz)
{
    return env->NewStringUTF(::dhcp_get_errmsg());    
}

/** M: support MTU size configuration by interface */
static jboolean android_net_utils_setMtuByInterface(JNIEnv* env, jobject clazz, jstring ifname, jint mtu)
{
    struct ifreq ifr;
    int s;
    jboolean bResult = JNI_FALSE;
        
    s = socket(PF_INET, SOCK_DGRAM, 0);
    if (s < 0) {
        return bResult;
    }
    
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    memset(&ifr, 0, sizeof(ifr));
    strncpy(ifr.ifr_name, nameStr, IFNAMSIZ);
    ifr.ifr_mtu = mtu;
    if (ioctl(s, SIOCSIFMTU, &ifr) >= 0) {
        bResult = JNI_TRUE;
    }else{
        ALOGE("[Error]android_net_utils_setMtuByInterface:%d", errno);
    }
    close(s);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return bResult;
}
/** @} */

/** M: support reset socket connections by the uid of process */
static jint android_net_utils_resetConnectionByUid(JNIEnv* env, jobject clazz, jint uid)
{
    int etimeout = 110;    /* ETIMEDOUT */
    int result = ::ifc_reset_connection_by_uid(uid, etimeout);  
    ALOGD("android_net_utils_resetConnectionByUid:%d", result);
    return (jint)result;
}
/** @} */

/** M: support reset socket connections by the uid of process with special error number */
static jint android_net_utils_resetConnectionByUid_err(JNIEnv* env, jobject clazz, jint uid, jint err)
{
    int result = ::ifc_reset_connection_by_uid(uid, err);
    ALOGE("android_net_utils_resetConnectionByUid_err:%d", result);
    return (jint)result;
}
/** @} */

/*add dhcpv6 corresponding method as dhcp*/
static jboolean android_net_utils_runDhcpv6Common(JNIEnv* env, jobject clazz, jstring ifname,
        jobject info, bool renew)
{
    int result;
    char  ipaddr[PROPERTY_VALUE_MAX];
    char    dns1[PROPERTY_VALUE_MAX];
    char    dns2[PROPERTY_VALUE_MAX];
    uint32_t lease;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    if (nameStr == NULL) return (jboolean)false;


    if (renew) {
        result = ::dhcpv6_do_request_renew(nameStr, ipaddr, dns1, dns2, &lease);
    } else {
        result = ::dhcpv6_do_request(nameStr, ipaddr, dns1, dns2, &lease);
    }


    env->ReleaseStringUTFChars(ifname, nameStr);

    if (result == 0) {
        env->SetObjectField(info, dhcpInfoInternalFieldIds.ipaddress, env->NewStringUTF(ipaddr));
        env->SetObjectField(info, dhcpInfoInternalFieldIds.dns1, env->NewStringUTF(dns1));
        env->SetObjectField(info, dhcpInfoInternalFieldIds.dns2, env->NewStringUTF(dns2));
        env->SetIntField(info, dhcpInfoInternalFieldIds.leaseDuration, lease);
    }
    
    return (jboolean)(result == 0);
}

static jboolean android_net_utils_runDhcpv6(JNIEnv* env, jobject clazz, jstring ifname, jobject info)
{
    return android_net_utils_runDhcpv6Common(env, clazz, ifname, info, false);
}

static jboolean android_net_utils_runDhcpv6Renew(JNIEnv* env, jobject clazz, jstring ifname, jobject info)
{
    return android_net_utils_runDhcpv6Common(env, clazz, ifname, info, true);
}

static jboolean android_net_utils_stopDhcpv6(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcpv6_stop(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
}

static jstring android_net_utils_getDhcpv6Error(JNIEnv* env, jobject clazz)
{
    return env->NewStringUTF(::dhcpv6_get_errmsg());
}

// ----------------------------------------------------------------------------

/*
 * JNI registration.
 */
static JNINativeMethod gNetworkUtilMethods[] = {
    /* name, signature, funcPtr */

    { "enableInterface", "(Ljava/lang/String;)I",  (void *)android_net_utils_enableInterface },
    { "disableInterface", "(Ljava/lang/String;)I",  (void *)android_net_utils_disableInterface },
    { "resetConnections", "(Ljava/lang/String;I)I",  (void *)android_net_utils_resetConnections },
    { "runDhcp", "(Ljava/lang/String;Landroid/net/DhcpInfoInternal;)Z",  (void *)android_net_utils_runDhcp },
    { "runDhcpRenew", "(Ljava/lang/String;Landroid/net/DhcpInfoInternal;)Z",  (void *)android_net_utils_runDhcpRenew },
    { "stopDhcp", "(Ljava/lang/String;)Z",  (void *)android_net_utils_stopDhcp },
    { "releaseDhcpLease", "(Ljava/lang/String;)Z",  (void *)android_net_utils_releaseDhcpLease },
    { "getDhcpError", "()Ljava/lang/String;", (void*) android_net_utils_getDhcpError },
    /// M: MTK network utility functions
    { "resetConnectionByUidErrNum", "(II)I",  (void *)android_net_utils_resetConnectionByUid_err },
    { "resetConnectionByUid", "(I)I",  (void *)android_net_utils_resetConnectionByUid },
    { "setMtuByInterface", "(Ljava/lang/String;I)Z",  (void *)android_net_utils_setMtuByInterface},
    /// add dhcpv6 corresponding JNI declaration
    { "runDhcpv6", "(Ljava/lang/String;Landroid/net/DhcpInfoInternal;)Z",  (void *)android_net_utils_runDhcpv6 },
    { "runDhcpv6Renew", "(Ljava/lang/String;Landroid/net/DhcpInfoInternal;)Z",  (void *)android_net_utils_runDhcpv6Renew },
    { "stopDhcpv6", "(Ljava/lang/String;)Z",  (void *)android_net_utils_stopDhcpv6 },
    { "getDhcpv6Error", "()Ljava/lang/String;", (void*) android_net_utils_getDhcpv6Error },
};

int register_android_net_NetworkUtils(JNIEnv* env)
{
    jclass dhcpInfoInternalClass = env->FindClass("android/net/DhcpInfoInternal");
    LOG_FATAL_IF(dhcpInfoInternalClass == NULL, "Unable to find class android/net/DhcpInfoInternal");
    dhcpInfoInternalFieldIds.constructorId = env->GetMethodID(dhcpInfoInternalClass, "<init>", "()V");
    dhcpInfoInternalFieldIds.ipaddress = env->GetFieldID(dhcpInfoInternalClass, "ipAddress", "Ljava/lang/String;");
    dhcpInfoInternalFieldIds.prefixLength = env->GetFieldID(dhcpInfoInternalClass, "prefixLength", "I");
    dhcpInfoInternalFieldIds.dns1 = env->GetFieldID(dhcpInfoInternalClass, "dns1", "Ljava/lang/String;");
    dhcpInfoInternalFieldIds.dns2 = env->GetFieldID(dhcpInfoInternalClass, "dns2", "Ljava/lang/String;");
    dhcpInfoInternalFieldIds.serverAddress = env->GetFieldID(dhcpInfoInternalClass, "serverAddress", "Ljava/lang/String;");
    dhcpInfoInternalFieldIds.leaseDuration = env->GetFieldID(dhcpInfoInternalClass, "leaseDuration", "I");
    dhcpInfoInternalFieldIds.vendorInfo = env->GetFieldID(dhcpInfoInternalClass, "vendorInfo", "Ljava/lang/String;");

    return AndroidRuntime::registerNativeMethods(env,
            NETUTILS_PKG_NAME, gNetworkUtilMethods, NELEM(gNetworkUtilMethods));
}

}; // namespace android
