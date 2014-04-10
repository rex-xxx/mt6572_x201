#include <stdlib.h>
#include "com_mediatek_nfcdemo.h"

jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
   JNIEnv *e;

   ALOGD("NFC Service : loading JNI\n");

   // Check JNI version
   if(jvm->GetEnv((void **)&e, JNI_VERSION_1_6))
      return JNI_ERR;

   android::vm = jvm;

   if (android::register_com_mediatek_nfcdemo_NativeNfcManager(e) == -1)
      return JNI_ERR;

   return JNI_VERSION_1_6;
}

namespace android {

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
        LOGD("Current thread is not attached to VM");
        abort();
    }
    return e;
}
    
}
