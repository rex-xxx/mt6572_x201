/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_TAG "AmrInputStream"
#include "utils/Log.h"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include "gsmamr_enc.h"

#ifndef ANDROID_DEFAULT_CODE
#include "utils/KeyedVector.h"
extern "C" {
#include "amr_exp.h"
}
#endif

// ----------------------------------------------------------------------------

using namespace android;

// Corresponds to max bit rate of 12.2 kbps.
static const int MAX_OUTPUT_BUFFER_SIZE = 32;
static const int FRAME_DURATION_MS = 20;
static const int SAMPLING_RATE_HZ = 8000;
static const int SAMPLES_PER_FRAME = ((SAMPLING_RATE_HZ * FRAME_DURATION_MS) / 1000);
static const int BYTES_PER_SAMPLE = 2;  // Assume 16-bit PCM samples
static const int BYTES_PER_FRAME = (SAMPLES_PER_FRAME * BYTES_PER_SAMPLE);

#ifndef ANDROID_DEFAULT_CODE
struct GsmAmrResource {
    GsmAmrResource()
        : m_bitrate(BR122),
          m_dtx(0),
          int_buf_size(0),
          tmp_buf_size(0),
          pcm_buf_size(0),
          bs_buf_size(0),
          int_buffer(NULL),
          tmp_buffer(NULL),
          amr_enc_handle(NULL) {
    }
    ~GsmAmrResource() {}
    AMR_BitRate     m_bitrate;
    int             m_dtx ;
    unsigned int    int_buf_size;
    unsigned int    tmp_buf_size;
    unsigned int    pcm_buf_size;
    unsigned int    bs_buf_size;
    void           *int_buffer;
    void           *tmp_buffer;
    AMR_ENC_HANDLE *amr_enc_handle;
};

static  KeyedVector<jint,GsmAmrResource*> EncoderSource;
 /*
    AMR_BitRate m_bitrate = BR122;
    int m_dtx = 0;
    
    unsigned int int_buf_size = 0;
    unsigned int tmp_buf_size = 0;
    unsigned int pcm_buf_size = 0;
    unsigned int bs_buf_size = 0;
    void *int_buffer = NULL;
    void *tmp_buffer = NULL;
    AMR_ENC_HANDLE *amr_enc_handle = NULL;
    */
#endif

struct GsmAmrEncoderState {
    GsmAmrEncoderState()
        : mEncState(NULL),
          mSidState(NULL),
          mLastModeUsed(0) {
    }

    ~GsmAmrEncoderState() {}

    void*   mEncState;
    void*   mSidState;
    int32_t mLastModeUsed;
};

static jint android_media_AmrInputStream_GsmAmrEncoderNew
        (JNIEnv *env, jclass clazz) {
    GsmAmrEncoderState* gae = new GsmAmrEncoderState();
    ALOGD("android_media_AmrInputStream_GsmAmrEncoderNew");
    if (gae == NULL) {
        jniThrowRuntimeException(env, "Out of memory");
    }
    return (jint)gae;
}

static void android_media_AmrInputStream_GsmAmrEncoderInitialize
        (JNIEnv *env, jclass clazz, jint gae) {

#ifndef ANDROID_DEFAULT_CODE
    // AMR Encoder: Step 1
    // To know the required buffer size
    GsmAmrResource * enc = new GsmAmrResource();
    AMREnc_GetBufferSize(&enc->int_buf_size,&enc->tmp_buf_size, 
        &enc->bs_buf_size ,&enc->pcm_buf_size);
    enc->int_buffer = (unsigned char *)malloc(enc->int_buf_size);
    enc->tmp_buffer = (unsigned char *)malloc(enc->tmp_buf_size);
    
    memset(enc->int_buffer, 0, enc->int_buf_size*sizeof(unsigned char));
    memset(enc->tmp_buffer, 0, enc->tmp_buf_size*sizeof(unsigned char));
    // AMR Encoder: Step 2
    // Assign buffer and initialize
    enc->m_dtx = 0;
    enc->m_bitrate = BR122;
    enc->amr_enc_handle = AMREnc_Init(enc->int_buffer,enc->m_bitrate,enc->m_dtx);
    ALOGD("android_media_AmrInputStream_GsmAmrEncoderInitialize,bitrate= %d",enc->m_bitrate);
    EncoderSource.add(gae,enc);
    /*
    AMREnc_GetBufferSize(&int_buf_size,&tmp_buf_size, &bs_buf_size ,&pcm_buf_size);
    int_buffer = (unsigned char *)malloc(int_buf_size);
    tmp_buffer = (unsigned char *)malloc(tmp_buf_size);
    
    memset(int_buffer, 0, int_buf_size*sizeof(unsigned char));
    memset(tmp_buffer, 0, tmp_buf_size*sizeof(unsigned char));
    // AMR Encoder: Step 2
    // Assign buffer and initialize
    m_dtx = 0;
    m_bitrate = BR122;
    amr_enc_handle = AMREnc_Init(int_buffer,m_bitrate,m_dtx);
    ALOGD("android_media_AmrInputStream_GsmAmrEncoderInitialize,bitrate= %d",m_bitrate);
    */
#else
    GsmAmrEncoderState *state = (GsmAmrEncoderState *) gae;
    int32_t nResult = AMREncodeInit(&state->mEncState, &state->mSidState, false);
    if (nResult != OK) {
        jniThrowExceptionFmt(env, "java/lang/IllegalArgumentException",
                "GsmAmrEncoder initialization failed %d", nResult);
    }
#endif

}

static jint android_media_AmrInputStream_GsmAmrEncoderEncode
        (JNIEnv *env, jclass clazz,
         jint gae, jbyteArray pcm, jint pcmOffset, jbyteArray amr, jint amrOffset) {

    jbyte inBuf[BYTES_PER_FRAME];
    jbyte outBuf[MAX_OUTPUT_BUFFER_SIZE];

    env->GetByteArrayRegion(pcm, pcmOffset, sizeof(inBuf), inBuf);
    int32_t length = 0;
//

#ifndef ANDROID_DEFAULT_CODE
    GsmAmrResource * enc= EncoderSource.valueFor(gae); 
    ALOG_ASSERT(enc!=NULL,"encresouce is null");

    length = AMR_Encode(enc->amr_enc_handle, enc->tmp_buffer, (int16_t *)inBuf,(unsigned char*)outBuf, enc->m_bitrate);
#else
    GsmAmrEncoderState *state = (GsmAmrEncoderState *) gae;
    length = AMREncode(state->mEncState, state->mSidState,
                                (Mode) MR122,
                                (int16_t *) inBuf,
                                (unsigned char *) outBuf,
                                (Frame_Type_3GPP*) &state->mLastModeUsed,
                                AMR_TX_WMF);
#endif
    if (length < 0) {
        jniThrowExceptionFmt(env, "java/io/IOException",
                "Failed to encode a frame with error code: %d", length);
        return -1;
    }


    // The 1st byte of PV AMR frames are WMF (Wireless Multimedia Forum)
    // bitpacked, i.e.;
    //    [P(4) + FT(4)]. Q=1 for good frame, P=padding bit, 0
    // Here we are converting the header to be as specified in Section 5.3 of
    // RFC 3267 (AMR storage format) i.e.
    //    [P(1) + FT(4) + Q(1) + P(2)].
/*
    if (length > 0) {
      outBuf[0] = (outBuf[0] << 3) | 0x4;
    }
*/
    env->SetByteArrayRegion(amr, amrOffset, length, outBuf);

    return length;
}

static void android_media_AmrInputStream_GsmAmrEncoderCleanup
        (JNIEnv *env, jclass clazz, jint gae) {
#ifndef ANDROID_DEFAULT_CODE
   ALOGD("android_media_AmrInputStream_GsmAmrEncoderCleanup");
   GsmAmrResource * enc= EncoderSource.valueFor(gae);
   ALOG_ASSERT(enc!=NULL,"encresouce is null");

   if(enc->int_buffer != NULL)
   {
      free(enc->int_buffer);
      enc->int_buffer = NULL;
   }
   if(enc->tmp_buffer != NULL)
   {
      free(enc->tmp_buffer);
      enc->tmp_buffer = NULL;
   }
   EncoderSource.removeItem(gae);
   delete enc;
#else
    GsmAmrEncoderState *state = (GsmAmrEncoderState *)gae;
    AMREncodeExit(&state->mEncState, &state->mSidState);
    state->mEncState = NULL;
    state->mSidState = NULL;
#endif
}

static void android_media_AmrInputStream_GsmAmrEncoderDelete
        (JNIEnv *env, jclass clazz, jint gae) {
    delete (GsmAmrEncoderState*)gae;
    ALOGD("android_media_AmrInputStream_GsmAmrEncoderDelete");
}

// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
    {"GsmAmrEncoderNew",        "()I",        (void*)android_media_AmrInputStream_GsmAmrEncoderNew},
    {"GsmAmrEncoderInitialize", "(I)V",       (void*)android_media_AmrInputStream_GsmAmrEncoderInitialize},
    {"GsmAmrEncoderEncode",     "(I[BI[BI)I", (void*)android_media_AmrInputStream_GsmAmrEncoderEncode},
    {"GsmAmrEncoderCleanup",    "(I)V",       (void*)android_media_AmrInputStream_GsmAmrEncoderCleanup},
    {"GsmAmrEncoderDelete",     "(I)V",       (void*)android_media_AmrInputStream_GsmAmrEncoderDelete},
};


int register_android_media_AmrInputStream(JNIEnv *env)
{
    const char* const kClassPathName = "android/media/AmrInputStream";

    return AndroidRuntime::registerNativeMethods(env,
            kClassPathName, gMethods, NELEM(gMethods));
}
