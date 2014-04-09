/*
 * Copyright (C) 2009 The Android Open Source Project
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

#define LOG_TAG "VibratorService"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#if defined(MTK_VIBSPK_SUPPORT)
#include "VibSpkAudioPlayer.h"
#endif

#include <utils/misc.h>
#include <utils/Log.h>
#include <hardware_legacy/vibrator.h>

#include <stdio.h>

namespace android
{

static jboolean vibratorExists(JNIEnv *env, jobject clazz)
{
#if defined(MTK_VIBSPK_SUPPORT)
    return JNI_TRUE;
#else
    return vibrator_exists() > 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

static void vibratorOn(JNIEnv *env, jobject clazz, jlong timeout_ms)
{
    // ALOGI("vibratorOn\n");
#if defined(MTK_VIBSPK_SUPPORT)
    if(timeout_ms == 0)
        VIBRATOR_SPKOFF();
    else
        VIBRATOR_SPKON((unsigned int)timeout_ms);
#else    
    vibrator_on(timeout_ms);
#endif
}

static void vibratorOff(JNIEnv *env, jobject clazz)
{
    // ALOGI("vibratorOff\n");
#if defined(MTK_VIBSPK_SUPPORT)
    VIBRATOR_SPKOFF();
#else
    vibrator_off();
#endif
}

static JNINativeMethod method_table[] = {
    { "vibratorExists", "()Z", (void*)vibratorExists },
    { "vibratorOn", "(J)V", (void*)vibratorOn },
    { "vibratorOff", "()V", (void*)vibratorOff }
};

int register_android_server_VibratorService(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, "com/android/server/VibratorService",
            method_table, NELEM(method_table));
}

};
