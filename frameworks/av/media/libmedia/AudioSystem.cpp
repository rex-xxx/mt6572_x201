/*
 * Copyright (C) 2006-2007 The Android Open Source Project
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

#define LOG_TAG "AudioSystem"
///#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <binder/IServiceManager.h>
#include <media/AudioSystem.h>
#include <media/IAudioPolicyService.h>
#include <math.h>

#include <system/audio.h>
 
#include <media/AudioParameter.h>
#ifndef ANDROID_DEFAULT_CODE
#include <cutils/xlog.h>
#endif
#ifdef MTK_AUDIO
#include <AudioIoctl.h>
#endif
// ----------------------------------------------------------------------------

namespace android {

// client singleton for AudioFlinger binder interface
Mutex AudioSystem::gLock;
sp<IAudioFlinger> AudioSystem::gAudioFlinger;
sp<AudioSystem::AudioFlingerClient> AudioSystem::gAudioFlingerClient;
audio_error_callback AudioSystem::gAudioErrorCallback = NULL;
// Cached values

DefaultKeyedVector<audio_io_handle_t, AudioSystem::OutputDescriptor *> AudioSystem::gOutputs(0);

// Cached values for recording queries, all protected by gLock
uint32_t AudioSystem::gPrevInSamplingRate = 16000;
audio_format_t AudioSystem::gPrevInFormat = AUDIO_FORMAT_PCM_16_BIT;
audio_channel_mask_t AudioSystem::gPrevInChannelMask = AUDIO_CHANNEL_IN_MONO;
size_t AudioSystem::gInBuffSize = 0;

#ifdef MTK_AUDIO
static const char* forceToSpeaker = "AudioSetForceToSpeaker";
static const char* keySetFmEnable = "AudioSetFmEnable";
static const char* keySetFmDigitalEnable = "AudioSetFmDigitalEnable";
static const char* keySetMatvEnable = "AtvAudioLineInEnable";
static String8 keyFmForce = String8(forceToSpeaker);
static String8 keyFmEnable = String8(keySetFmEnable);
static String8 keyFmDigitalEnable = String8(keySetFmDigitalEnable);
static String8 keyMatvEnable = String8(keySetMatvEnable);
#endif

// establish binder interface to AudioFlinger service
const sp<IAudioFlinger>& AudioSystem::get_audio_flinger()
{
    Mutex::Autolock _l(gLock);
    if (gAudioFlinger == 0) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;
        do {
            binder = sm->getService(String16("media.audio_flinger"));
            if (binder != 0)
                break;
            ALOGW("AudioFlinger not published, waiting...");
            usleep(500000); // 0.5 s
        } while (true);
        if (gAudioFlingerClient == NULL) {
            gAudioFlingerClient = new AudioFlingerClient();
        } else {
            if (gAudioErrorCallback) {
                gAudioErrorCallback(NO_ERROR);
            }
        }
        binder->linkToDeath(gAudioFlingerClient);
        gAudioFlinger = interface_cast<IAudioFlinger>(binder);
        gAudioFlinger->registerClient(gAudioFlingerClient);
    }
    ALOGE_IF(gAudioFlinger==0, "no AudioFlinger!?");

    return gAudioFlinger;
}

status_t AudioSystem::muteMicrophone(bool state) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->setMicMute(state);
}

status_t AudioSystem::isMicrophoneMuted(bool* state) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *state = af->getMicMute();
    return NO_ERROR;
}

status_t AudioSystem::setMasterVolume(float value)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setMasterVolume(value);
    return NO_ERROR;
}

status_t AudioSystem::setMasterMute(bool mute)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setMasterMute(mute);
    return NO_ERROR;
}

status_t AudioSystem::getMasterVolume(float* volume)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *volume = af->masterVolume();
    return NO_ERROR;
}

status_t AudioSystem::getMasterMute(bool* mute)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *mute = af->masterMute();
    return NO_ERROR;
}

status_t AudioSystem::setStreamVolume(audio_stream_type_t stream, float value,
        audio_io_handle_t output)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setStreamVolume(stream, value, output);
    return NO_ERROR;
}

status_t AudioSystem::setStreamMute(audio_stream_type_t stream, bool mute)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    af->setStreamMute(stream, mute);
    return NO_ERROR;
}

status_t AudioSystem::getStreamVolume(audio_stream_type_t stream, float* volume,
        audio_io_handle_t output)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *volume = af->streamVolume(stream, output);
    return NO_ERROR;
}

status_t AudioSystem::getStreamMute(audio_stream_type_t stream, bool* mute)
{
    if (uint32_t(stream) >= AUDIO_STREAM_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    *mute = af->streamMute(stream);
    return NO_ERROR;
}

status_t AudioSystem::setMode(audio_mode_t mode)
{
    if (uint32_t(mode) >= AUDIO_MODE_CNT) return BAD_VALUE;
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->setMode(mode);
}

status_t AudioSystem::setParameters(audio_io_handle_t ioHandle, const String8& keyValuePairs) {
    status_t ret =NO_ERROR;
#ifdef MTK_AUDIO
    ALOGD("+setParameters(): %s ", keyValuePairs.string());
    int value =0;
    audio_io_handle_t FmDigitaloutput = 0;
    AudioParameter param = AudioParameter(keyValuePairs);
    if (param.getInt(keyFmForce, value) == NO_ERROR){
        //Don't move this out of if, policyManager construct use this 
        //function and will be in deadloop.
        const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if(aps != 0)
        {
            aps->SetPolicyManagerParameters (POLICY_SET_FM_SPEAKER,value,0,0);
        }
    }
    else if(param.getInt(keyFmEnable, value) == NO_ERROR)
    {  
        const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if(aps != 0)
        {
            audio_io_handle_t output = aps->getOutput(AUDIO_STREAM_FM);           
            ALOGD("setParameters(): out=%d, value=%d", output, value);
            if(value != 0)
            {
                aps->startOutput(output, AUDIO_STREAM_FM);
            }
            else
            {
                aps->stopOutput(output, AUDIO_STREAM_FM);
            }
        }
    }
    else if(param.getInt(keyFmDigitalEnable, value) == NO_ERROR)
    {  
        const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if(aps != 0)
        {
            audio_io_handle_t output = aps->getOutput(AUDIO_STREAM_FM);
            FmDigitaloutput = output;
            ALOGD("setParameters(): out=%d, value=%d", output, value);
            if(value != 0)
            {
                aps->startOutput(output, AUDIO_STREAM_FM);
            }
            else
            {
                aps->stopOutput(output, AUDIO_STREAM_FM);
            }
        }
    }
    else if(param.getInt(keyMatvEnable, value) == NO_ERROR)
    {  
        const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if(aps != 0)
        {
            audio_io_handle_t output = aps->getOutput(AUDIO_STREAM_MATV);
            ALOGD("setParameters(): out=%d, value=%d", output, value);
            if(value != 0)
            {
                aps->startOutput(output,AUDIO_STREAM_MATV);
            }
            else
            {
                aps->stopOutput(output,AUDIO_STREAM_MATV);
            }
        }
    }
#endif
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    ret = af->setParameters(ioHandle, keyValuePairs);
    
#ifdef MTK_AUDIO
    if(param.getInt(keyFmDigitalEnable, value) == NO_ERROR)
    {
        const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
        if(aps != 0)
        {
            ALOGD("Send POLICY_CHECK_FM_PRIMARY_KEY_ROUTING");
            aps->SetPolicyManagerParameters(POLICY_CHECK_FM_PRIMARY_KEY_ROUTING,value,FmDigitaloutput,0);
        }
    }
#endif

#ifndef ANDROID_DEFAULT_CODE 
    ALOGD("-setParameters(): %s ", keyValuePairs.string());
#endif
    return ret;
}

String8 AudioSystem::getParameters(audio_io_handle_t ioHandle, const String8& keys) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    String8 result = String8("");
    if (af == 0) return result;

    result = af->getParameters(ioHandle, keys);
    return result;
}

// convert volume steps to natural log scale

// change this value to change volume scaling
#ifdef VOLUME_NEWMAP
#ifdef MTK_AUDIO_GAIN_TABLE
static const float dBPerStep = 0.25f;
static const float VOLUME_MAPPING_STEP = 255.0f;
#else
static const float dBPerStep = 0.25f;
static const float VOLUME_MAPPING_STEP = 256.0f;
#endif
#else
static const float dBPerStep = 0.5f;
static const float VOLUME_MAPPING_STEP = 100.0f;
#endif
// shouldn't need to touch these
static const float dBConvert = -dBPerStep * 2.302585093f / 20.0f;
static const float dBConvertInverse = 1.0f / dBConvert;

float AudioSystem::linearToLog(int volume)
{
    // float v = volume ? exp(float(100 - volume) * dBConvert) : 0;
    // ALOGD("linearToLog(%d)=%f", volume, v);
    // return v;
    return volume ? exp(float(VOLUME_MAPPING_STEP - volume) * dBConvert) : 0;
}

int AudioSystem::logToLinear(float volume)
{
    // int v = volume ? 100 - int(dBConvertInverse * log(volume) + 0.5) : 0;
    // ALOGD("logTolinear(%d)=%f", v, volume);
    // return v;
    return volume ? VOLUME_MAPPING_STEP - int(dBConvertInverse * log(volume) + 0.5) : 0;
}

// DEPRECATED
status_t AudioSystem::getOutputSamplingRate(int* samplingRate, int streamType) {
    return getOutputSamplingRate(samplingRate, (audio_stream_type_t)streamType);
}

status_t AudioSystem::getOutputSamplingRate(int* samplingRate, audio_stream_type_t streamType)
{
    audio_io_handle_t output;

    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    output = getOutput(streamType);
    if (output == 0) {
        return PERMISSION_DENIED;
    }

    return getSamplingRate(output, streamType, samplingRate);
}

status_t AudioSystem::getSamplingRate(audio_io_handle_t output,
                                      audio_stream_type_t streamType,
                                      int* samplingRate)
{
    OutputDescriptor *outputDesc;

    gLock.lock();
    outputDesc = AudioSystem::gOutputs.valueFor(output);
    if (outputDesc == NULL) {
        ALOGV("getOutputSamplingRate() no output descriptor for output %d in gOutputs", output);
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;
        *samplingRate = af->sampleRate(output);
    } else {
        ALOGV("getOutputSamplingRate() reading from output desc");
        *samplingRate = outputDesc->samplingRate;
        gLock.unlock();
    }

    ALOGV("getSamplingRate() streamType %d, output %d, sampling rate %d", streamType, output, *samplingRate);

    return NO_ERROR;
}

// DEPRECATED
status_t AudioSystem::getOutputFrameCount(int* frameCount, int streamType) {
    return getOutputFrameCount(frameCount, (audio_stream_type_t)streamType);
}

status_t AudioSystem::getOutputFrameCount(int* frameCount, audio_stream_type_t streamType)
{
    audio_io_handle_t output;

    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    output = getOutput(streamType);
    if (output == 0) {
        return PERMISSION_DENIED;
    }

    return getFrameCount(output, streamType, frameCount);
}

status_t AudioSystem::getFrameCount(audio_io_handle_t output,
                                    audio_stream_type_t streamType,
                                    int* frameCount)
{
    OutputDescriptor *outputDesc;

    gLock.lock();
    outputDesc = AudioSystem::gOutputs.valueFor(output);
    if (outputDesc == NULL) {
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;
        *frameCount = af->frameCount(output);
    } else {
        *frameCount = outputDesc->frameCount;
        gLock.unlock();
    }

    ALOGV("getFrameCount() streamType %d, output %d, frameCount %d", streamType, output, *frameCount);

    return NO_ERROR;
}

status_t AudioSystem::getOutputLatency(uint32_t* latency, audio_stream_type_t streamType)
{
    audio_io_handle_t output;

    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    output = getOutput(streamType);
    if (output == 0) {
        return PERMISSION_DENIED;
    }

    return getLatency(output, streamType, latency);
}

status_t AudioSystem::getLatency(audio_io_handle_t output,
                                 audio_stream_type_t streamType,
                                 uint32_t* latency)
{
    OutputDescriptor *outputDesc;

    gLock.lock();
    outputDesc = AudioSystem::gOutputs.valueFor(output);
    if (outputDesc == NULL) {
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;
        *latency = af->latency(output);
    } else {
        *latency = outputDesc->latency;
        gLock.unlock();
    }

    ALOGV("getLatency() streamType %d, output %d, latency %d", streamType, output, *latency);

    return NO_ERROR;
}

status_t AudioSystem::getInputBufferSize(uint32_t sampleRate, audio_format_t format,
        audio_channel_mask_t channelMask, size_t* buffSize)
{
    gLock.lock();
    // Do we have a stale gInBufferSize or are we requesting the input buffer size for new values
    size_t inBuffSize = gInBuffSize;
    if ((inBuffSize == 0) || (sampleRate != gPrevInSamplingRate) || (format != gPrevInFormat)
        || (channelMask != gPrevInChannelMask)) {
        gLock.unlock();
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) {
            return PERMISSION_DENIED;
        }
        inBuffSize = af->getInputBufferSize(sampleRate, format, channelMask);
        gLock.lock();
        // save the request params
        gPrevInSamplingRate = sampleRate;
        gPrevInFormat = format;
        gPrevInChannelMask = channelMask;

        gInBuffSize = inBuffSize;
    }
    gLock.unlock();
    *buffSize = inBuffSize;

    return NO_ERROR;
}

status_t AudioSystem::setVoiceVolume(float value)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->setVoiceVolume(value);
}

status_t AudioSystem::getRenderPosition(uint32_t *halFrames, uint32_t *dspFrames, audio_stream_type_t stream)
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;

    if (stream == AUDIO_STREAM_DEFAULT) {
        stream = AUDIO_STREAM_MUSIC;
    }

    return af->getRenderPosition(halFrames, dspFrames, getOutput(stream));
}

unsigned int AudioSystem::getInputFramesLost(audio_io_handle_t ioHandle) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    unsigned int result = 0;
    if (af == 0) return result;
    if (ioHandle == 0) return result;

    result = af->getInputFramesLost(ioHandle);
    return result;
}

int AudioSystem::newAudioSessionId() {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return 0;
    return af->newAudioSessionId();
}

void AudioSystem::acquireAudioSessionId(int audioSession) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af != 0) {
        af->acquireAudioSessionId(audioSession);
    }
}

void AudioSystem::releaseAudioSessionId(int audioSession) {
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af != 0) {
        af->releaseAudioSessionId(audioSession);
    }
}

// ---------------------------------------------------------------------------

void AudioSystem::AudioFlingerClient::binderDied(const wp<IBinder>& who) {
    Mutex::Autolock _l(AudioSystem::gLock);

    AudioSystem::gAudioFlinger.clear();
    // clear output handles and stream to output map caches
    AudioSystem::gOutputs.clear();

    if (gAudioErrorCallback) {
        gAudioErrorCallback(DEAD_OBJECT);
    }
    ALOGW("AudioFlinger server died!");
}

void AudioSystem::AudioFlingerClient::ioConfigChanged(int event, audio_io_handle_t ioHandle,
        const void *param2) {
    ALOGV("ioConfigChanged() event %d", event);
    const OutputDescriptor *desc;
    audio_stream_type_t stream;

    if (ioHandle == 0) return;

    Mutex::Autolock _l(AudioSystem::gLock);

    switch (event) {
    case STREAM_CONFIG_CHANGED:
        break;
    case OUTPUT_OPENED: {
        if (gOutputs.indexOfKey(ioHandle) >= 0) {
            ALOGV("ioConfigChanged() opening already existing output! %d", ioHandle);
            break;
        }
        if (param2 == NULL) break;
        desc = (const OutputDescriptor *)param2;

        OutputDescriptor *outputDesc =  new OutputDescriptor(*desc);
        gOutputs.add(ioHandle, outputDesc);
        ALOGV("ioConfigChanged() new output samplingRate %d, format %d channels %#x frameCount %d latency %d",
                outputDesc->samplingRate, outputDesc->format, outputDesc->channels, outputDesc->frameCount, outputDesc->latency);
        } break;
    case OUTPUT_CLOSED: {
        if (gOutputs.indexOfKey(ioHandle) < 0) {
            ALOGW("ioConfigChanged() closing unknow output! %d", ioHandle);
            break;
        }
        ALOGV("ioConfigChanged() output %d closed", ioHandle);

        gOutputs.removeItem(ioHandle);
        } break;

    case OUTPUT_CONFIG_CHANGED: {
        int index = gOutputs.indexOfKey(ioHandle);
        if (index < 0) {
            ALOGW("ioConfigChanged() modifying unknow output! %d", ioHandle);
            break;
        }
        if (param2 == NULL) break;
        desc = (const OutputDescriptor *)param2;

        ALOGV("ioConfigChanged() new config for output %d samplingRate %d, format %d channels %#x frameCount %d latency %d",
                ioHandle, desc->samplingRate, desc->format,
                desc->channels, desc->frameCount, desc->latency);
        OutputDescriptor *outputDesc = gOutputs.valueAt(index);
        delete outputDesc;
        outputDesc =  new OutputDescriptor(*desc);
        gOutputs.replaceValueFor(ioHandle, outputDesc);
    } break;
    case INPUT_OPENED:
    case INPUT_CLOSED:
    case INPUT_CONFIG_CHANGED:
        break;

    }
}

void AudioSystem::setErrorCallback(audio_error_callback cb) {
    Mutex::Autolock _l(gLock);
    gAudioErrorCallback = cb;
}

bool AudioSystem::routedToA2dpOutput(audio_stream_type_t streamType) {
    switch (streamType) {
    case AUDIO_STREAM_MUSIC:
    case AUDIO_STREAM_VOICE_CALL:
    case AUDIO_STREAM_BLUETOOTH_SCO:
    case AUDIO_STREAM_SYSTEM:
        return true;
    default:
        return false;
    }
}


// client singleton for AudioPolicyService binder interface
sp<IAudioPolicyService> AudioSystem::gAudioPolicyService;
sp<AudioSystem::AudioPolicyServiceClient> AudioSystem::gAudioPolicyServiceClient;


// establish binder interface to AudioFlinger service
const sp<IAudioPolicyService>& AudioSystem::get_audio_policy_service()
{
    gLock.lock();
    if (gAudioPolicyService == 0) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;
        do {
            binder = sm->getService(String16("media.audio_policy"));
            if (binder != 0)
                break;
            ALOGW("AudioPolicyService not published, waiting...");
            usleep(500000); // 0.5 s
        } while (true);
        if (gAudioPolicyServiceClient == NULL) {
            gAudioPolicyServiceClient = new AudioPolicyServiceClient();
        }
        binder->linkToDeath(gAudioPolicyServiceClient);
        gAudioPolicyService = interface_cast<IAudioPolicyService>(binder);
        gLock.unlock();
    } else {
        gLock.unlock();
    }
    return gAudioPolicyService;
}

status_t AudioSystem::setDeviceConnectionState(audio_devices_t device,
                                               audio_policy_dev_state_t state,
                                               const char *device_address)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    const char *address = "";

    if (aps == 0) return PERMISSION_DENIED;

    if (device_address != NULL) {
        address = device_address;
    }

    return aps->setDeviceConnectionState(device, state, address);
}

audio_policy_dev_state_t AudioSystem::getDeviceConnectionState(audio_devices_t device,
                                                  const char *device_address)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return AUDIO_POLICY_DEVICE_STATE_UNAVAILABLE;

    return aps->getDeviceConnectionState(device, device_address);
}

status_t AudioSystem::setPhoneState(audio_mode_t state)
{
    if (uint32_t(state) >= AUDIO_MODE_CNT) return BAD_VALUE;
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;

    return aps->setPhoneState(state);
}

status_t AudioSystem::setForceUse(audio_policy_force_use_t usage, audio_policy_forced_cfg_t config)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setForceUse(usage, config);
}

audio_policy_forced_cfg_t AudioSystem::getForceUse(audio_policy_force_use_t usage)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return AUDIO_POLICY_FORCE_NONE;
    return aps->getForceUse(usage);
}


audio_io_handle_t AudioSystem::getOutput(audio_stream_type_t stream,
                                    uint32_t samplingRate,
                                    audio_format_t format,
                                    audio_channel_mask_t channelMask,
                                    audio_output_flags_t flags)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return 0;
    return aps->getOutput(stream, samplingRate, format, channelMask, flags);
}

status_t AudioSystem::startOutput(audio_io_handle_t output,
                                  audio_stream_type_t stream,
                                  int session)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->startOutput(output, stream, session);
}

status_t AudioSystem::stopOutput(audio_io_handle_t output,
                                 audio_stream_type_t stream,
                                 int session)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->stopOutput(output, stream, session);
}

void AudioSystem::releaseOutput(audio_io_handle_t output)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return;
    aps->releaseOutput(output);
}

audio_io_handle_t AudioSystem::getInput(audio_source_t inputSource,
                                    uint32_t samplingRate,
                                    audio_format_t format,
                                    audio_channel_mask_t channelMask,
                                    int sessionId)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return 0;
    return aps->getInput(inputSource, samplingRate, format, channelMask, sessionId);
}

status_t AudioSystem::startInput(audio_io_handle_t input)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->startInput(input);
}

status_t AudioSystem::stopInput(audio_io_handle_t input)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->stopInput(input);
}

void AudioSystem::releaseInput(audio_io_handle_t input)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return;
    aps->releaseInput(input);
}

status_t AudioSystem::initStreamVolume(audio_stream_type_t stream,
                                    int indexMin,
                                    int indexMax)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->initStreamVolume(stream, indexMin, indexMax);
}

status_t AudioSystem::setStreamVolumeIndex(audio_stream_type_t stream,
                                           int index,
                                           audio_devices_t device)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setStreamVolumeIndex(stream, index, device);
}

status_t AudioSystem::getStreamVolumeIndex(audio_stream_type_t stream,
                                           int *index,
                                           audio_devices_t device)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->getStreamVolumeIndex(stream, index, device);
}

uint32_t AudioSystem::getStrategyForStream(audio_stream_type_t stream)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return 0;
    return aps->getStrategyForStream(stream);
}

audio_devices_t AudioSystem::getDevicesForStream(audio_stream_type_t stream)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return (audio_devices_t)0;
    return aps->getDevicesForStream(stream);
}

audio_io_handle_t AudioSystem::getOutputForEffect(const effect_descriptor_t *desc)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->getOutputForEffect(desc);
}

status_t AudioSystem::registerEffect(const effect_descriptor_t *desc,
                                audio_io_handle_t io,
                                uint32_t strategy,
                                int session,
                                int id)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->registerEffect(desc, io, strategy, session, id);
}

status_t AudioSystem::unregisterEffect(int id)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->unregisterEffect(id);
}

status_t AudioSystem::setEffectEnabled(int id, bool enabled)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    return aps->setEffectEnabled(id, enabled);
}

status_t AudioSystem::isStreamActive(audio_stream_type_t stream, bool* state, uint32_t inPastMs)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    if (state == NULL) return BAD_VALUE;
    *state = aps->isStreamActive(stream, inPastMs);
    return NO_ERROR;
}

status_t AudioSystem::isSourceActive(audio_source_t stream, bool* state)
{
    const sp<IAudioPolicyService>& aps = AudioSystem::get_audio_policy_service();
    if (aps == 0) return PERMISSION_DENIED;
    if (state == NULL) return BAD_VALUE;
    *state = aps->isSourceActive(stream);
    return NO_ERROR;
}

int32_t AudioSystem::getPrimaryOutputSamplingRate()
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return 0;
    return af->getPrimaryOutputSamplingRate();
}

int32_t AudioSystem::getPrimaryOutputFrameCount()
{
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return 0;
    return af->getPrimaryOutputFrameCount();
}

void AudioSystem::clearAudioConfigCache()
{
    Mutex::Autolock _l(gLock);
    ALOGV("clearAudioConfigCache()");
    gOutputs.clear();
}

// ---------------------------------------------------------------------------

void AudioSystem::AudioPolicyServiceClient::binderDied(const wp<IBinder>& who) {
    Mutex::Autolock _l(AudioSystem::gLock);
    AudioSystem::gAudioPolicyService.clear();

    ALOGW("AudioPolicyService server died!");
}

//// Interfaces mtk added 
int AudioSystem::xWayPlay_Start(int sample_rate)
{  
#ifdef MTK_AUDIO
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;     
    return af->xWayPlay_Start(sample_rate);
#endif
    return 0;
}

int AudioSystem::xWayPlay_Stop()
{
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;     
    return af->xWayPlay_Stop();
#endif
    return 0;
}

int AudioSystem::xWayPlay_Write(void *buffer, int size_bytes)
{
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;   
    return af->xWayPlay_Write(buffer,size_bytes);
#endif
    return 0;
}

int AudioSystem::xWayPlay_GetFreeBufferCount()
{  
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;         
    return af->xWayPlay_GetFreeBufferCount();
#endif
    return 0;
}

int AudioSystem::xWayRec_Start(int sample_rate)
{
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;     
    return af->xWayRec_Start(sample_rate);
#endif
    return 0;
}

int AudioSystem::xWayRec_Stop()
{
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;    
    return af->xWayRec_Stop();
#endif
    return 0;
}

int AudioSystem::xWayRec_Read(void *buffer, int size_bytes)
{
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;     
    return af->xWayRec_Read(buffer,size_bytes);
#endif
    return 0;
}
//add by wendy
int AudioSystem::ReadRefFromRing(void*buf, uint32_t datasz,void* DLtime)
{
#ifndef ANDROID_DEFAULT_CODE  
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;  
        ALOGV("af->ReadRefFromRing");
        return af->ReadRefFromRing(buf, datasz, DLtime);
#else
        return 0;
#endif    
}
int AudioSystem::GetVoiceUnlockULTime(void* DLtime)
{
#ifndef ANDROID_DEFAULT_CODE  
        const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
        if (af == 0) return PERMISSION_DENIED;  
        ALOGV("af->GetVoiceUnlockULTime");
        return af->GetVoiceUnlockULTime( DLtime);
#else
        return 0;
#endif    
}
int AudioSystem::SetVoiceUnlockSRC(uint outSR, uint outChannel)
{
#ifndef ANDROID_DEFAULT_CODE  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;  
    ALOGD("af->SetVoiceUnlockSRC");
    return af->SetVoiceUnlockSRC(outSR, outChannel);
#else
    return 0;
#endif    
}
bool AudioSystem::startVoiceUnlockDL()
{
#ifndef ANDROID_DEFAULT_CODE  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) 
	{	
	    ALOGE("startVoiceUnlockDL::PERMISSION_DENIED");
		return PERMISSION_DENIED;
    }	
    ALOGD("af->startVoiceUnlockDL");
    return af->startVoiceUnlockDL();
#else
    return 0;
#endif    
}
bool AudioSystem:: stopVoiceUnlockDL()
{
#ifndef ANDROID_DEFAULT_CODE  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
	if (af == 0) 
	{	
	    ALOGE("stopVoiceUnlockDL::PERMISSION_DENIED");
		return PERMISSION_DENIED;
    }
    ALOGD("af->stopVoiceUnlockDL");
    return af->stopVoiceUnlockDL();
#else
    return 0;
#endif    
}
void AudioSystem::freeVoiceUnlockDLInstance()
{
#ifndef ANDROID_DEFAULT_CODE  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return ;  
    ALOGD("af->freeVoiceUnlockDLInstance");
    return af->freeVoiceUnlockDLInstance();
#else
    return;
#endif    

}
 int AudioSystem::GetVoiceUnlockDLLatency()
 {
#ifndef ANDROID_DEFAULT_CODE  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) 
	{	
	    ALOGE("GetVoiceUnlockDLLatency::PERMISSION_DENIED");
		return PERMISSION_DENIED;
    }	
    ALOGD("af->GetVoiceUnlockDLLatency");
    return af->GetVoiceUnlockDLLatency();
#else
    return 0;
#endif    
 }
 bool AudioSystem::getVoiceUnlockDLInstance()
{
#ifndef ANDROID_DEFAULT_CODE  
     const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
     if (af == 0) 
	 {	
	    ALOGE("getVoiceUnlockDLInstance::PERMISSION_DENIED");
	 	return PERMISSION_DENIED ;  
     }	
     ALOGD("af->getVoiceUnlockDLInstance");
     return af->getVoiceUnlockDLInstance();
#else
     return 0;
#endif    
}

//add , for EM mode
status_t AudioSystem::GetEMParameter(void *ptr,size_t len)
{ 
#ifdef MTK_AUDIO
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;
    return af->GetEMParameter(ptr,len);
#endif
    return OK;   
}

status_t AudioSystem::SetEMParameter(void *ptr,size_t len)
{
#ifdef MTK_AUDIO  
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0) return PERMISSION_DENIED;   
    return af->SetEMParameter(ptr,len);
#endif
    return OK; 
}

status_t AudioSystem::SetACFPreviewParameter(void *ptr,size_t len)
{
#ifdef MTK_AUDIO    
    ALOGD("AudioSystem::SetACFPreviewParameter!! 01");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        ALOGE("AudioSystem::SetACFPreviewParameter Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }  
    return af->SetACFPreviewParameter(ptr,len);
#endif
    return OK;    
}

status_t AudioSystem::SetHCFPreviewParameter(void *ptr,size_t len)
{  
#ifdef MTK_AUDIO

    ALOGD("AudioSystem::SetHCFPreviewParameter!! 01");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        ALOGE("AudioSystem::SetHCFPreviewParameter Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetHCFPreviewParameter(ptr,len);
#endif
    return OK; 
}

status_t AudioSystem::SetAudioCommand(int par1,int par2)
{
#ifdef MTK_AUDIO

    ALOGD("AudioSystem::SetAudioCommand");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        ALOGE("AudioSystem::SetAudioCommand Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetAudioCommand(par1,par2);
#endif
    return OK;   
}

status_t AudioSystem::GetAudioCommand(int par1,int* par2)
{
#ifdef MTK_AUDIO  
    ALOGD("AudioSystem::GetAudioCommand");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        ALOGE("AudioSystem::GetAudioCommand Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }  
    *par2 =  af->GetAudioCommand(par1);
#endif
    return NO_ERROR;
}

status_t AudioSystem::SetAudioData(int par1,size_t byte_len,void *ptr)
{
#ifdef MTK_AUDIO   
    ALOGD("SetAudioData");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        ALOGE("AudioSystem::SetAAudioData Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }
    return af->SetAudioData(par1,byte_len,ptr);
#endif
    return OK;  
}

status_t AudioSystem::GetAudioData(int par1,size_t byte_len,void *ptr)
{
#ifdef MTK_AUDIO 
    ALOGD("GetAudioData");
    const sp<IAudioFlinger>& af = AudioSystem::get_audio_flinger();
    if (af == 0)
    {
        ALOGE("AudioSystem::GetAAudioData Error!! PERMISSION_DENIED");
        return PERMISSION_DENIED;
    }   
    return af->GetAudioData(par1,byte_len,ptr);
#endif
    return OK;  
}

}; // namespace android

