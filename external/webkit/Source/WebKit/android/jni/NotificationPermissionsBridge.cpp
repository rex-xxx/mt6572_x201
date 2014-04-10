/*
 * Copyright 2009, The Android Open Source Project
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

#include <JNIHelp.h>  // For jniRegisterNativeMethods
#include "NotificationPresenterImpl.h"

#if ENABLE(NOTIFICATIONS)

/**
 * This file provides a set of functions to bridge between the Java and C++
 * NotificationPermissions classes. The java NotificationPermissions object calls
 * the functions provided here, which in turn call static methods on the C++
 * NotificationPermissions class.
 */

namespace android {

static void clearAll(JNIEnv* env, jobject obj)
{
    NotificationPresenterImpl::clearAll();
}

/*
 * JNI registration
 */
static JNINativeMethod gNotificationPermissionsMethods[] = {
     { "nativeClearAll", "()V",
        (void*) clearAll }
};

int registerNotificationPermissions(JNIEnv* env)
{
    const char* kNotificationPermissionsClass = "android/webkit/NotificationPermissionsClassic";
#ifndef NDEBUG
    jclass notificationPermissions = env->FindClass(kNotificationPermissionsClass);
    LOG_ASSERT(notificationPermissions, "Unable to find class");
    env->DeleteLocalRef(notificationPermissions);
#endif

    return jniRegisterNativeMethods(env, kNotificationPermissionsClass,
            gNotificationPermissionsMethods, NELEM(gNotificationPermissionsMethods));
}

}

#endif // ENABLE(NOTIFICATIONS)

