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

#include <stdlib.h>

#include "errno.h"
#include "com_android_nfc.h"
#include "com_android_nfc_list.h"

/*
 * JNI Initialization
 */
jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
   JNIEnv *e;

   LogInformation("NFC Service : loading JNI\n");

   // Check JNI version
   if(jvm->GetEnv((void **)&e, JNI_VERSION_1_6))
      return JNI_ERR;

   android::vm = jvm;

   if (android::register_com_android_nfc_NativeNfcManager(e) == -1)
      return JNI_ERR;

   if (android::register_com_android_nfc_NativeNfcTag(e) == -1)
      return JNI_ERR;
   if (android::register_com_android_nfc_NativeP2pDevice(e) == -1)
      return JNI_ERR;

   if (android::register_com_android_nfc_NativeLlcpSocket(e) == -1)
      return JNI_ERR;

   if (android::register_com_android_nfc_NativeLlcpConnectionlessSocket(e) == -1)
      return JNI_ERR;

   if (android::register_com_android_nfc_NativeLlcpServiceSocket(e) == -1)
      return JNI_ERR;

   if (android::register_com_android_nfc_NativeNfcSecureElement(e) == -1)
      return JNI_ERR;


   return JNI_VERSION_1_6;
}

namespace android {

extern struct nfc_jni_native_data *exported_nat;

JavaVM *vm;

JavaVM * getJavaVM()
{
	return vm;
}
/*
 * JNI Utils
 */
JNIEnv *nfc_get_env()
{
    JNIEnv *e;
    if (vm->GetEnv((void **)&e, JNI_VERSION_1_6) != JNI_OK) {
        LogError("Current thread is not attached to VM");
        abort();
    }
    return e;
}



int nfc_jni_cache_object(JNIEnv *e, const char *clsname,
   jobject *cached_obj)
{
   jclass cls;
   jobject obj;
   jmethodID ctor;

   LogInformation("nfc_jni_cache_object(): clsname=%s", clsname);

   cls = e->FindClass(clsname);
   if(cls == NULL)
   {
	  LogInformation("Find class error\n");
      return -1;
   }

   ctor = e->GetMethodID(cls, "<init>", "()V");

   obj = e->NewObject(cls, ctor);
   if(obj == NULL)
   {
	  LogInformation("Create object error\n");
      return -1;
   }

   *cached_obj = e->NewGlobalRef(obj);
   if(*cached_obj == NULL)
   {
      e->DeleteLocalRef(obj);
      LogInformation("Global ref error\n");
      return -1;
   }

   e->DeleteLocalRef(obj);

   return 0;
}


struct nfc_jni_native_data* nfc_jni_get_nat(JNIEnv *e, jobject o)
{
   jclass c;
   jfieldID f;

   /* Retrieve native structure address */
   c = e->GetObjectClass(o);
   f = e->GetFieldID(c, "mNative", "I");
   return (struct nfc_jni_native_data*)e->GetIntField(o, f);
}

struct nfc_jni_native_data* nfc_jni_get_nat_ext(JNIEnv *e)
{
   return NULL;//exported_nat;
}

static nfc_jni_native_monitor_t *nfc_jni_native_monitor = NULL;

nfc_jni_native_monitor_t* nfc_jni_init_monitor(void)
{

   pthread_mutexattr_t recursive_attr;

   pthread_mutexattr_init(&recursive_attr);
   pthread_mutexattr_settype(&recursive_attr, PTHREAD_MUTEX_RECURSIVE_NP);

   if(nfc_jni_native_monitor == NULL)
   {
      nfc_jni_native_monitor = (nfc_jni_native_monitor_t*)malloc(sizeof(nfc_jni_native_monitor_t));
   }

   if(nfc_jni_native_monitor != NULL)
   {
      memset(nfc_jni_native_monitor, 0, sizeof(nfc_jni_native_monitor_t));

      if(pthread_mutex_init(&nfc_jni_native_monitor->reentrance_mutex, &recursive_attr) == -1)
      {
         LogError("NFC Manager Reentrance Mutex creation returned 0x%08x", errno);
         return NULL;
      }

      if(pthread_mutex_init(&nfc_jni_native_monitor->concurrency_mutex, NULL) == -1)
      {
         LogError("NFC Manager Concurrency Mutex creation returned 0x%08x", errno);
         return NULL;
      }

      if(!listInit(&nfc_jni_native_monitor->sem_list))
      {
         LogError("NFC Manager Semaphore List creation failed");
         return NULL;
      }

      LIST_INIT(&nfc_jni_native_monitor->incoming_socket_head);

      if(pthread_mutex_init(&nfc_jni_native_monitor->incoming_socket_mutex, NULL) == -1)
      {
         LogError("NFC Manager incoming socket mutex creation returned 0x%08x", errno);
         return NULL;
      }

      if(pthread_cond_init(&nfc_jni_native_monitor->incoming_socket_cond, NULL) == -1)
      {
         LogError("NFC Manager incoming socket condition creation returned 0x%08x", errno);
         return NULL;
      }

}

   return nfc_jni_native_monitor;
} 

nfc_jni_native_monitor_t* nfc_jni_get_monitor(void)
{
   return nfc_jni_native_monitor;
}
   

jshort nfc_jni_get_p2p_device_mode(JNIEnv *e, jobject o)
{
   jclass c;
   jfieldID f;

   c = e->GetObjectClass(o);
   f = e->GetFieldID(c, "mMode", "S");

   return e->GetShortField(o, f);
}


int nfc_jni_get_connected_tech_index(JNIEnv *e, jobject o)
{

   jclass c;
   jfieldID f;

   c = e->GetObjectClass(o);
   f = e->GetFieldID(c, "mConnectedTechIndex", "I");

   return e->GetIntField(o, f);

}

jint nfc_jni_get_connected_technology(JNIEnv *e, jobject o)
{
   jclass c;
   jfieldID f;
   int connectedTech = -1;

   int connectedTechIndex = nfc_jni_get_connected_tech_index(e,o);
   jintArray techTypes = nfc_jni_get_nfc_tag_type(e, o);

   if ((connectedTechIndex != -1) && (techTypes != NULL) &&
           (connectedTechIndex < e->GetArrayLength(techTypes))) {
       jint* technologies = e->GetIntArrayElements(techTypes, 0);
       if (technologies != NULL) {
           connectedTech = technologies[connectedTechIndex];
           e->ReleaseIntArrayElements(techTypes, technologies, JNI_ABORT);
       }
   }

   return connectedTech;

}

jint nfc_jni_get_connected_technology_libnfc_type(JNIEnv *e, jobject o)
{
   jclass c;
   jfieldID f;
   jint connectedLibNfcType = -1;

   int connectedTechIndex = nfc_jni_get_connected_tech_index(e,o);
   c = e->GetObjectClass(o);
   f = e->GetFieldID(c, "mTechLibNfcTypes", "[I");
   jintArray libNfcTypes =  (jintArray) e->GetObjectField(o, f);

   if ((connectedTechIndex != -1) && (libNfcTypes != NULL) &&
           (connectedTechIndex < e->GetArrayLength(libNfcTypes))) {
       jint* types = e->GetIntArrayElements(libNfcTypes, 0);
       if (types != NULL) {
           connectedLibNfcType = types[connectedTechIndex];
           e->ReleaseIntArrayElements(libNfcTypes, types, JNI_ABORT);
       }
   }
   return connectedLibNfcType;

}

uint32_t nfc_jni_get_connected_handle(JNIEnv *e, jobject o)
{
   jclass c;
   jfieldID f;

   c = e->GetObjectClass(o);
   f = e->GetFieldID(c, "mConnectedHandle", "I");

   return e->GetIntField(o, f);
}

uint32_t nfc_jni_get_nfc_socket_handle(JNIEnv *e, jobject o)
{
   jclass c;
   jfieldID f;

   c = e->GetObjectClass(o);
   f = e->GetFieldID(c, "mHandle", "I");

   return (uint32_t)e->GetIntField(o, f);
}

jintArray nfc_jni_get_nfc_tag_type(JNIEnv *e, jobject o)
{
  jclass c;
  jfieldID f;
  jintArray techtypes;
   
  c = e->GetObjectClass(o);
  f = e->GetFieldID(c, "mTechList","[I");

  /* Read the techtypes  */
  techtypes = (jintArray) e->GetObjectField(o, f);

  return techtypes;
}


#define MAX_NUM_TECHNOLOGIES 32

} // namespace android
