#include "AudioMTKStreamOut.h"
#include "AudioResourceFactory.h"
#include "AudioResourceManagerInterface.h"
#include "AudioIoctl.h"
#include "AudioDigitalType.h"
#include "SpeechDriverFactory.h"
#include "SpeechBGSPlayer.h"
#include "SpeechPhoneCallController.h"
#include "AudioBTCVSDControl.h"
#include "LoopbackManager.h"

#include "audio_custom_exp.h"
#include "AudioVUnlockDL.h"

#include "AudioCustParam.h"
#include "CFG_AUDIO_File.h"

#define LOG_TAG  "AudioMTKStreamOut"
#ifndef ANDROID_DEFAULT_CODE
    #include <cutils/xlog.h>
    #ifdef ALOGE
    #undef ALOGE
    #endif
    #ifdef ALOGW
    #undef ALOGW
    #endif ALOGI
    #undef ALOGI
    #ifdef ALOGD
    #undef ALOGD
    #endif
    #ifdef ALOGV
    #undef ALOGV
    #endif
    #define ALOGE XLOGE
    #define ALOGW XLOGW
    #define ALOGI XLOGI
    #define ALOGD XLOGD
    #define ALOGV XLOGV
#else
    #include <utils/Log.h>
#endif

//Configure ACF Work Mode
#if defined(ENABLE_AUDIO_COMPENSATION_FILTER) && defined(ENABLE_AUDIO_DRC_SPEAKER)
//5
#define AUDIO_COMPENSATION_FLT_MODE AUDIO_CMP_FLT_LOUDNESS_COMP_BASIC

#elif defined(ENABLE_AUDIO_COMPENSATION_FILTER)
// 4
#define AUDIO_COMPENSATION_FLT_MODE AUDIO_CMP_FLT_LOUDNESS_COMP

#elif defined(ENABLE_AUDIO_DRC_SPEAKER)
// 3
#define AUDIO_COMPENSATION_FLT_MODE AUDIO_CMP_FLT_LOUDNESS_LITE

#endif


namespace android
{

AudioMTKStreamOut::AudioMTKStreamOut()
{
    ALOGD("+AudioMTKStreamOut default constructor");
    mStreamOutCompFltEnable[AUDIO_COMP_FLT_AUDIO] = mStreamOutCompFltEnable[AUDIO_COMP_FLT_HEADPHONE] = mStreamOutCompFltEnable[AUDIO_COMP_FLT_AUDENH] = false;
    mpClsCompFltObj[AUDIO_COMP_FLT_AUDIO] = mpClsCompFltObj[AUDIO_COMP_FLT_HEADPHONE] = mpClsCompFltObj[AUDIO_COMP_FLT_AUDENH] = NULL;
    mdCompFltMode[AUDIO_COMP_FLT_AUDIO] = mdCompFltMode[AUDIO_COMP_FLT_HEADPHONE] = mdCompFltMode[AUDIO_COMP_FLT_AUDENH] = AUDIO_CMP_FLT_LOUDNESS_NONE;
    mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_AUDIO] = mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_HEADPHONE] = mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_AUDENH] = false;
#if defined(MTK_VIBSPK_SUPPORT)
    mStreamOutCompFltEnable[AUDIO_COMP_FLT_VIBSPK]      = false;
    mpClsCompFltObj[AUDIO_COMP_FLT_VIBSPK]              = NULL;
    mdCompFltMode[AUDIO_COMP_FLT_VIBSPK]                = AUDIO_CMP_FLT_LOUDNESS_NONE;
    mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_VIBSPK] = false;
#endif
    mPDacPCMDumpFile = NULL;
    mPFinalPCMDumpFile = NULL;
    mSteroToMono = false;
    mForceStandby = false;
    ALOGD("-AudioMTKStreamOut default constructor");
}

AudioMTKStreamOut::AudioMTKStreamOut(uint32_t devices, int *format, uint32_t *channels, uint32_t *sampleRate, status_t *status)
{
    mStreamOutCompFltEnable[AUDIO_COMP_FLT_AUDIO] = mStreamOutCompFltEnable[AUDIO_COMP_FLT_HEADPHONE] = mStreamOutCompFltEnable[AUDIO_COMP_FLT_AUDENH] = false;
    mpClsCompFltObj[AUDIO_COMP_FLT_AUDIO] = mpClsCompFltObj[AUDIO_COMP_FLT_HEADPHONE] = mpClsCompFltObj[AUDIO_COMP_FLT_AUDENH] = NULL;
    mdCompFltMode[AUDIO_COMP_FLT_AUDIO] = mdCompFltMode[AUDIO_COMP_FLT_HEADPHONE] = mdCompFltMode[AUDIO_COMP_FLT_AUDENH] = AUDIO_CMP_FLT_LOUDNESS_NONE;
    mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_AUDIO] = mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_HEADPHONE] = mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_AUDENH] = false;
#if defined(MTK_VIBSPK_SUPPORT)
    mStreamOutCompFltEnable[AUDIO_COMP_FLT_VIBSPK]      = false;
    mpClsCompFltObj[AUDIO_COMP_FLT_VIBSPK]              = NULL;
    mdCompFltMode[AUDIO_COMP_FLT_VIBSPK]                = AUDIO_CMP_FLT_LOUDNESS_NONE;
    mStreamOutCompFltApplyStatus[AUDIO_COMP_FLT_VIBSPK] = false;
#endif
    mFd = 0;

#ifdef BT_SW_CVSD
    mFd2 = 0;

    mFd2 = ::open(kBTDeviceName, O_RDWR);
    if (mFd2 <= 0) {
        ALOGE("AudioMTKStreamOut open mFd2 fail");
    }
    ALOGD("+%s(), open cvsd kernel, mFd2: %d, AP errno: %d",__FUNCTION__, mFd2, errno);

    mAudioBTCVSDControl = AudioBTCVSDControlInterface::getInstance();
    if(!mAudioBTCVSDControl){
        ALOGE("AudioBTCVSDControlInterface::getInstance() fail");
    }

    mAudioBTCVSDControl->BT_SCO_SET_TXState(BT_SCO_TXSTATE_IDLE);
    mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_IDLE);
#endif

    mPDacPCMDumpFile = NULL;
    mPFinalPCMDumpFile = NULL;
    // here open audio hardware for register setting
    mFd = ::open(kAudioDeviceName, O_RDWR);
    if (mFd == 0) {
        ALOGE("AudioMTKStreamOut open mFd fail");
    }

    ALOGD("+AudioMTKStreamOut constructor devices = %x format = %x channels = %x sampleRate = %d",
          devices, *format, *channels, *sampleRate);
    mDL1Attribute = new AudioStreamAttribute();
#if defined(MTK_VIBSPK_SUPPORT)
    mVibSpk      = AudioVIBSPKControl::getInstance();
    mFdVibspk    = ::open(kAudioDeviceName, O_RDWR);
    mDL2Vibspk   = false;
    mDL2DelayOff = 0;
#endif
    mDL1Out = new AudioDigtalI2S();
    m2ndI2SOut = new AudioDigtalI2S();
    mDaiBt = new AudioDigitalDAIBT();
    mAudioDigitalControl = AudioDigitalControlFactory::CreateAudioDigitalControl();
    mAudioAnalogControl = AudioAnalogControlFactory::CreateAudioAnalogControl();
    mAudioResourceManager = AudioResourceManagerFactory::CreateAudioResource();
    if (!mAudioResourceManager) {
        ALOGE("mAudioResourceManager get fail  = %p", mAudioResourceManager);
    }

    mAudioSpeechEnhanceInfoInstance = AudioSpeechEnhanceInfo::getInstance();
    if(!mAudioSpeechEnhanceInfoInstance){
        ALOGE("mAudioSpeechEnhanceInfoInstance get fail");
    }

    mAudioSpeechEnhanceInfoInstance->SetStreamOutPointer(this);
    mEcho_reference = NULL;

    ALOGD("format = %d, channels = %d, rate = %d", *format, *channels, *sampleRate);

    if (*format == AUDIO_FORMAT_PCM_16_BIT)
        mDL1Attribute->mFormat = AUDIO_FORMAT_PCM_16_BIT;
    else if (*format == AUDIO_FORMAT_PCM_8_BIT)
        mDL1Attribute->mFormat = AUDIO_FORMAT_PCM_8_BIT;
    else {
        ALOGE("Format is not a valid number");
        mDL1Attribute->mFormat = AUDIO_FORMAT_PCM_16_BIT;
    }

    if (*channels == AUDIO_CHANNEL_OUT_MONO) {
        mDL1Attribute->mChannels = 1;
    }
    else if (*channels == AUDIO_CHANNEL_OUT_STEREO) {
        mDL1Attribute->mChannels = 2;
    }
    else {
        ALOGE("Channelsis not a valid number");
        mDL1Attribute->mChannels = 2;
    }
    mDL1Attribute->mSampleRate = *sampleRate;
#if defined(MTK_VIBSPK_SUPPORT)
    mVibSpkFreq = GetVibSpkCalibrationStatus();
    ALOGD("VibSpkReadFrequency:%x",mVibSpkFreq);
    if( mVibSpkFreq == 0)
    {
        SetVibSpkDefaultParam();
        mVibSpkFreq = VIBSPK_DEFAULT_FREQ;
    }
    mVibSpk->setParameters(44100, mVibSpkFreq, MOD_FREQ, DELTA_FREQ);
    mVibSpkEnable = false;
#endif

    // save the original sample rate & channels before SRC
    mSourceSampleRate  = *sampleRate;
    mSourceChannels    = mDL1Attribute->mChannels;

    mStarting = false;
    mSuspend = 0;
    mSteroToMono = false;
    mDL1Attribute->mBufferSize = 4096;
    mHwBufferSize = mAudioDigitalControl->GetMemBufferSize(AudioDigitalType::MEM_DL1);
    mLatency = (mHwBufferSize * 1000) / (mDL1Attribute->mSampleRate * mDL1Attribute->mChannels *
                                        (mDL1Attribute->mFormat == AUDIO_FORMAT_PCM_8_BIT ? 1 : 2));
    AudioVUnlockDL* VUnlockhdl = AudioVUnlockDL::getInstance();
    if(VUnlockhdl !=NULL)
    {
        VUnlockhdl->GetStreamOutLatency(mLatency);
    }
    calInterrupttime();

    Set2ndI2SOutAttribute();
    SetI2SOutDACAttribute();
    SetDAIBTAttribute();

    mBliSrc = NULL;
    mBliSrc = new BliSrc();

    mSwapBufferTwo = NULL;
    mSwapBufferTwo = new uint8_t[bufferSize()];
    if (mSwapBufferTwo == NULL) {
        ALOGE("mSwapBufferTwo for BliSRC allocate fail1!!! \n");
    }

    mSwapBufferThree = NULL;
    mForceStandby = false;

#if defined(ENABLE_AUDIO_COMPENSATION_FILTER)||defined(ENABLE_AUDIO_DRC_SPEAKER)    //For reduce resource
    StreamOutCompFltCreate(AUDIO_COMP_FLT_AUDIO,AUDIO_COMPENSATION_FLT_MODE);
#endif

#if defined(ENABLE_HEADPHONE_COMPENSATION_FILTER)   //For reduce resource
    StreamOutCompFltCreate(AUDIO_COMP_FLT_HEADPHONE,AUDIO_CMP_FLT_LOUDNESS_COMP_HEADPHONE);
#endif

#if defined(MTK_AUDENH_SUPPORT) //For reduce resource
    mSwapBufferThree = new uint8_t[bufferSize()];
    if (mSwapBufferThree == NULL) {
        ALOGE("mSwapBufferThree for AudEnh allocate fail1!!! \n");
    }

    StreamOutCompFltCreate(AUDIO_COMP_FLT_AUDENH,AUDIO_CMP_FLT_LOUDNESS_COMP_AUDENH);

    #if 0
    char value[PROPERTY_VALUE_MAX];
    int result = 0 ;
    property_get("persist.af.audenh.ctrl", value, "1");
    result = atoi(value);
    #else

    unsigned int result = 0 ;
    AUDIO_AUDENH_CONTROL_OPTION_STRUCT audioParam;
    if(GetAudEnhControlOptionParamFromNV(&audioParam))
        result = audioParam.u32EnableFlg;
    #endif

    mUseAudCompFltHeadphoneWithFixedParameter = (result?true:false);
#else
    mUseAudCompFltHeadphoneWithFixedParameter = false;
#endif
#if defined(MTK_VIBSPK_SUPPORT)
    mSwapBufferVibSpk = new uint8_t[bufferSize()];
    if (mSwapBufferVibSpk == NULL) {
        ALOGE("mSwapBufferVibSpk for VibSpk allocate fail1!!! \n");
    }
    StreamOutCompFltCreate(AUDIO_COMP_FLT_VIBSPK, AUDIO_CMP_FLT_LOUDNESS_COMP);
    
    mSwapBufferTone = new uint8_t[bufferSize()];
    if (mSwapBufferTone == NULL) {
        ALOGE("mSwapBufferTone for VibSpk allocate fail1!!! \n");
    }
#endif 
    ALOGD("Init mUseAudCompFltHeadphoneWithFixedParameter [%d]\n",mUseAudCompFltHeadphoneWithFixedParameter);
    *status = NO_ERROR;
    ALOGD("-AudioMTKStreamOut constructor \n");
}

AudioMTKStreamOut::~AudioMTKStreamOut()
{
    ALOGD("AudioMTKStreamOut desstructor \n");

    StreamOutCompFltDestroy(AUDIO_COMP_FLT_AUDIO);
    StreamOutCompFltDestroy(AUDIO_COMP_FLT_HEADPHONE);
    StreamOutCompFltDestroy(AUDIO_COMP_FLT_AUDENH);
#if defined(MTK_VIBSPK_SUPPORT)
    StreamOutCompFltDestroy(AUDIO_COMP_FLT_VIBSPK);
#endif

    if (mBliSrc) {
        mBliSrc->close();
        delete mBliSrc;
        mBliSrc = NULL;
    }
    if (mSwapBufferTwo) {
        delete []mSwapBufferTwo;
        mSwapBufferTwo = NULL;
    }

    if (mSwapBufferThree) {
        delete []mSwapBufferThree;
        mSwapBufferThree = NULL;
    }
#if defined(MTK_VIBSPK_SUPPORT)
    if (mSwapBufferVibSpk) {
        delete []mSwapBufferVibSpk;
        mSwapBufferVibSpk = NULL;
    }
    
    if(mSwapBufferTone){
        delete []mSwapBufferTone;
        mSwapBufferTone = NULL;
    }
    mVibSpk->setVibSpkEnable(false);
#endif
    if(mEcho_reference)
        mEcho_reference = NULL;
    if (mFd2>0) {
        ::close(mFd2);
        mFd2 = 0;
    }
}

uint32_t AudioMTKStreamOut::calInterrupttime()
{
    int SampleCount = mHwBufferSize / mDL1Attribute->mChannels;
    ALOGD("calInterrupttime bufferSize = %d mDL1Attribute->mChannels = %d", bufferSize(), mDL1Attribute->mChannels);

    if (mDL1Attribute->mFormat == AUDIO_FORMAT_PCM_16_BIT)
        SampleCount = SampleCount >> 1;

    mDL1Attribute->mInterruptSample =  SampleCount >> 1;
    ALOGD("calInterrupttime mInterruptCounter = %d", mDL1Attribute->mInterruptSample);
    return  mDL1Attribute->mInterruptSample ;
}

status_t AudioMTKStreamOut::SetMEMIFAttribute(AudioDigitalType::Digital_Block Mem_IF, AudioStreamAttribute *Attribute)
{
    ALOGD("SetMEMIFAttribute Mem_IF = %d sampleRate = %d mInterruptSample = %d", Mem_IF, Attribute->mSampleRate, Attribute->mInterruptSample);

    mAudioDigitalControl->SetMemIfSampleRate(Mem_IF, Attribute->mSampleRate);
    mAudioDigitalControl->SetMemIfChannelCount(Mem_IF, Attribute->mChannels);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetMEMIFEnable(AudioDigitalType::Digital_Block Mem_IF, bool bEnable)
{
    ALOGD("SetMEMIFEnable Mem_IF = %d bEnable = %d", Mem_IF, bEnable);
    mAudioDigitalControl->SetMemIfEnable(Mem_IF, bEnable);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetI2SOutDACAttribute()
{
    ALOGD("SetI2SOutDACAttribute");
    mDL1Out->mLR_SWAP = AudioDigtalI2S::NO_SWAP;
    mDL1Out->mI2S_SLAVE = AudioDigtalI2S::MASTER_MODE;
    mDL1Out->mINV_LRCK = AudioDigtalI2S::NO_INVERSE;
    mDL1Out->mI2S_FMT = AudioDigtalI2S::I2S;
    mDL1Out->mI2S_WLEN = AudioDigtalI2S::WLEN_16BITS;
    mDL1Out->mI2S_SAMPLERATE = sampleRate();
    mAudioDigitalControl->SetI2SDacOut(mDL1Out);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::Set2ndI2SOutAttribute()
{
    ALOGD("Set2ndI2SOutAttribute");
    m2ndI2SOut->mLR_SWAP = AudioDigtalI2S::NO_SWAP;
    m2ndI2SOut->mI2S_SLAVE = AudioDigtalI2S::MASTER_MODE;
    m2ndI2SOut->mINV_LRCK = AudioDigtalI2S::NO_INVERSE;
    m2ndI2SOut->mI2S_FMT = AudioDigtalI2S::I2S;
    m2ndI2SOut->mI2S_WLEN = AudioDigtalI2S::WLEN_16BITS;
    m2ndI2SOut->mI2S_SAMPLERATE = sampleRate();
    mAudioDigitalControl->Set2ndI2SOut(m2ndI2SOut);
    return NO_ERROR;
}


status_t AudioMTKStreamOut::Set2ndI2SOutAttribute(
                                               AudioDigtalI2S::LR_SWAP LRswap ,
                                               AudioDigtalI2S::I2S_SRC mode ,
                                               AudioDigtalI2S::INV_LRCK inverse,
                                               AudioDigtalI2S::I2S_FORMAT format,
                                               AudioDigtalI2S::I2S_WLEN Wlength,
                                               int samplerate )
{
    ALOGD("Set2ndI2SOutAttribute with dedicated define");
    m2ndI2SOut->mLR_SWAP = LRswap;
    m2ndI2SOut->mI2S_SLAVE = mode;
    m2ndI2SOut->mINV_LRCK = inverse;
    m2ndI2SOut->mI2S_FMT = format;
    m2ndI2SOut->mI2S_WLEN = Wlength;
    m2ndI2SOut->mI2S_SAMPLERATE = samplerate;
    mAudioDigitalControl->Set2ndI2SOut(m2ndI2SOut);
    return NO_ERROR;
}


status_t AudioMTKStreamOut::SetI2SDACOut(bool bEnable)
{
    ALOGD("+%s(), bEnable = %d\n", __FUNCTION__, bEnable);
    mAudioDigitalControl->SetI2SDacEnable(bEnable);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::Set2ndI2SOut(bool bEnable)
{
    ALOGD("+%s(), bEnable = %d\n", __FUNCTION__, bEnable);
    #if 1
    mAudioDigitalControl->Set2ndI2SOutEnable(bEnable);
    #else
    mAudioDigitalControl->Set2ndI2SEnable(bEnable);
    #endif
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetDAIBTAttribute()
{
    // fix me , ned to base on actual situation
#if defined(MTK_MERGE_INTERFACE_SUPPORT)
    mDaiBt->mUSE_MRGIF_INPUT = AudioDigitalDAIBT::FROM_MGRIF;
#else
    mDaiBt->mUSE_MRGIF_INPUT = AudioDigitalDAIBT::FROM_BT;
#endif
    mDaiBt->mDAI_BT_MODE = AudioDigitalDAIBT::Mode8K;
    mDaiBt->mDAI_DEL = AudioDigitalDAIBT::HighWord;
    mDaiBt->mBT_LEN  = 0;
    mDaiBt->mDATA_RDY = true;
    mDaiBt->mBT_SYNC = AudioDigitalDAIBT::Short_Sync;
    mDaiBt->mBT_ON = true;
    mDaiBt->mDAIBT_ON = false;
    mAudioDigitalControl->SetDAIBBT(mDaiBt);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetDAIBTOut(bool bEnable)
{
    mAudioDigitalControl->SetDAIBTEnable(bEnable);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetIMcuIRQ(AudioDigitalType::IRQ_MCU_MODE IRQ_mode, AudioStreamAttribute *Attribute)
{
    ALOGD("SetIMcuIRQ1 IRQ_mode = %d sampleRate = %d mInterruptSample = %d", IRQ_mode, Attribute->mSampleRate, Attribute->mInterruptSample);
    mAudioDigitalControl->SetIrqMcuSampleRate(IRQ_mode, Attribute->mSampleRate);
    mAudioDigitalControl->SetIrqMcuCounter(IRQ_mode , Attribute->mInterruptSample);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::EnableIMcuIRQ(AudioDigitalType::IRQ_MCU_MODE IRQ_mode , bool bEnable)
{
    ALOGD("EnableIMcuIRQ1 IRQ_mode = %d bEnable = %d", IRQ_mode, bEnable);
    mAudioDigitalControl->SetIrqMcuEnable(IRQ_mode, bEnable);
    return NO_ERROR;
}

uint32_t AudioMTKStreamOut::sampleRate() const
{
    return mDL1Attribute->mSampleRate;
}

size_t AudioMTKStreamOut::bufferSize() const
{
    return mDL1Attribute->mBufferSize;
}

uint32_t AudioMTKStreamOut::channels() const // TODO(Harvey): WHY? 1 & 2 ??? or 0x1 & 0x3 ???
{
    if (mDL1Attribute->mChannels == 1) {
        return AUDIO_CHANNEL_OUT_MONO;
    }
    else {
        return AUDIO_CHANNEL_OUT_STEREO;
    }
    return AUDIO_CHANNEL_OUT_STEREO;
}

int AudioMTKStreamOut::format() const
{
    return mDL1Attribute->mFormat;
}

uint32_t AudioMTKStreamOut::latency() const
{
    return mLatency;
}

status_t AudioMTKStreamOut::setVolume(float left, float right)
{
    return NO_ERROR;
}

status_t AudioMTKStreamOut::RequesetPlaybackclock()
{
    mAudioResourceManager->EnableAudioClock(AudioResourceManagerInterface::CLOCK_AUD_ANA, true);
    mAudioResourceManager->EnableAudioClock(AudioResourceManagerInterface::CLOCK_AUD_AFE, true);
    return NO_ERROR;
}
status_t AudioMTKStreamOut::ReleasePlaybackclock()
{
    mAudioResourceManager->EnableAudioClock(AudioResourceManagerInterface::CLOCK_AUD_AFE, false);
    mAudioResourceManager->EnableAudioClock(AudioResourceManagerInterface::CLOCK_AUD_ANA, false);
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetPlayBackPinmux()
{
    ALOGD("SetPlayBackPinmux");
    if (mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINR) ||
        mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINL)) {
        if(!mAudioResourceManager->IsWiredHeadsetOn()){
            mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_SPEAKERR, AudioAnalogType::MUX_AUDIO);
            mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_SPEAKERL, AudioAnalogType::MUX_AUDIO);
            ALOGD("SetPlayBackPinmux spk pinmux as MUX_AUDIO");
        } else {
            ALOGD("SetPlayBackPinmux keep the same");
        }
        // keep the same pinmux
    }
    else {
        ALOGD("SetPlayBackPinmux set audio pinmux");
        // set pin mux with playback only for performance
        mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_EARPIECEL, AudioAnalogType::MUX_VOICE);
        mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_HEADSETR, AudioAnalogType::MUX_AUDIO);
        mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_HEADSETL, AudioAnalogType::MUX_AUDIO);
        mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_SPEAKERR, AudioAnalogType::MUX_AUDIO);
        mAudioAnalogControl->AnalogSetMux(AudioAnalogType::DEVICE_OUT_SPEAKERL, AudioAnalogType::MUX_AUDIO);
    }
    return NO_ERROR;
}

bool AudioMTKStreamOut::DoStereoMonoConvert(void *buffer, size_t byte)
{
    //ALOGD("DoStereoMonoConvert mSteroToMono = %d",mSteroToMono);
    if(mSteroToMono == true)
    {
        short *Sample = (short*)buffer;
        int FinalValue  =0;
        while(byte > 0)
        {
            FinalValue = ((*Sample) + (*(Sample+1)));
            *Sample++= FinalValue>>1;
            *Sample++ = FinalValue >>1;
            byte -= 4;
        }
    }
    return true;
}

status_t AudioMTKStreamOut::SetStereoToMonoFlag(int device)
{
    if((device&AUDIO_DEVICE_OUT_SPEAKER)&&(IsStereoSpeaker()== false))
    {
        mSteroToMono = true;
    }
    else
    {
        mSteroToMono = false;
    }
    return NO_ERROR;
}

void  AudioMTKStreamOut::OpenPcmDumpFile()
{
    if(mPDacPCMDumpFile == NULL){
       mPDacPCMDumpFile = AudioOpendumpPCMFile(streamout, streamout_propty);
    }
    if(mPFinalPCMDumpFile == NULL){
        mPFinalPCMDumpFile = AudioOpendumpPCMFile(streamoutfinal, streamout_propty);
    }
}

void  AudioMTKStreamOut::ClosePcmDumpFile()
{
    AudioCloseDumpPCMFile(mPDacPCMDumpFile);
    AudioCloseDumpPCMFile(mPFinalPCMDumpFile);
    mPDacPCMDumpFile = NULL;
    mPFinalPCMDumpFile = NULL;
}

int AudioMTKStreamOut::WriteOriginPcmDump(void *buffer, size_t bytes)
{
    int written_data =0;
    if (mPDacPCMDumpFile)
    {
        written_data = fwrite((void *)buffer, 1, bytes, mPDacPCMDumpFile);
    }
    return written_data;
}

bool AudioMTKStreamOut::IsStereoSpeaker()
{
    #ifdef ENABLE_STEREO_SPEAKER
    return true;
    #else
    return false;
    #endif
}
#ifdef BT_SW_CVSD
void AudioMTKStreamOut::BTCVSD_Init(void)
{
    mAudioBTCVSDControl->BT_SCO_TX_Open(); //Allocate btsco_cvsd_tx_outbuf and working buffer
    ALOGD("ioctl mFd2=0x%x, cmd=0x%x",mFd2,ALLOCATE_FREE_BTCVSD_BUF);
    ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 0); //allocate TX working buffers in kernel
    mAudioBTCVSDControl->BT_SCO_SET_TXState(BT_SCO_TXSTATE_INIT);
    mAudioBTCVSDControl->BT_SCO_TX_SetHandle(NULL, NULL, mSourceSampleRate, mSourceChannels, 0);
    mAudioBTCVSDControl->BT_SCO_SET_TXState(BT_SCO_TXSTATE_READY);
    mAudioBTCVSDControl->BT_SCO_TX_Start();
    mAudioBTCVSDControl->BT_SCO_SET_TXState(BT_SCO_TXSTATE_RUNNING);
    ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_TXSTATE_RUNNING); //set state to kernel
    
    
#if defined(BTCVSD_LOOPBACK_WITH_CODEC) // create MTKRecordthread for test BTCVSD RX
    ALOGD("****************BTCVSD TEST create AudioMTkRecordThread************** \n");
    mBTCVSDRxTestThread = new AudioMTkRecordThread(this, AudioDigitalType::MEM_DAI , NULL, 0);
    if (mBTCVSDRxTestThread.get()) {
        mBTCVSDRxTestThread->run();
    }
#endif
}
#endif

bool AudioMTKStreamOut::NeedAFEDigitalAnalogControl(uint32 DigitalPart)
{
    #if defined(BTCVSD_ENC_DEC_LOOPBACK) || defined(BTCVSD_KERNEL_LOOPBACK)
    // BTCVSD TEST: turn on irq 1 INTERRUPT_IRQ1_MCU for simulate BT interrupt
    // AFE_IRQ_CON does not support 64000 samplerate, use 1080/48000=22.5ms to simulate BTCVSD interrupt
    mDL1Attribute->mSampleRate = 48000;
    mDL1Attribute->mInterruptSample = 1080;
    #endif
    
    #if defined(BTCVSD_ENC_DEC_LOOPBACK) || defined(BTCVSD_KERNEL_LOOPBACK) || defined(BTCVSD_LOOPBACK_WITH_CODEC)
    return false; // if test by AFE IRQ (TEST_USE_AFE_IRQ in kernel ), need to enable this condition
    #elif defined(BT_SW_CVSD)
    if (DigitalPart == AudioDigitalType::DAI_BT) //no need to enable IRQ1 , AFE, MemIntf, analogDev  in BTCVSD case
    {
        return false;
    }
    #endif

    return true;
}

ssize_t AudioMTKStreamOut::write(const void *buffer, size_t bytes)
{
    int ret =0;
    ALOGD("%s(), buffer = %p bytes = %d mLatency = %d", __FUNCTION__, buffer, bytes, mLatency);
    // here need write data to hardware
    ssize_t WrittenBytes = 0;
    if (mSuspend || (LoopbackManager::GetInstance()->GetLoopbackType() != NO_LOOPBACK && LoopbackManager::GetInstance()->GetLoopbackType() !=AP_BT_LOOPBACK)) {
        usleep(mLatency * 1000);//slee for a while
        ALOGD("%s() suspend write", __FUNCTION__);
        return bytes;
    }
    AudioVUnlockDL* VUnlockhdl = AudioVUnlockDL::getInstance();
    // need lock first
    ret=  mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_HARDWARE_LOCK, 3000);
     if(ret)
     {
         ALOGW("write EnableAudioLock  AUDIO_HARDWARE_LOCK fail");
         usleep(50 * 1000);
         return bytes;
    }
    ret = mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK, 3000);
    if(ret)
    {
        ALOGW("write EnableAudioLock AUDIO_STREAMOUT_LOCK fail");
        usleep(50 * 1000);
        return bytes;
    }

    audio_mode_t Mode = mAudioResourceManager->GetAudioMode();
    //staring DL digital part.
    if (GetStreamRunning() == false) {
        RequesetPlaybackclock();
        OpenPcmDumpFile();
        SetStreamRunning(true);
        switch (Mode) {
            case AUDIO_MODE_NORMAL:
            case AUDIO_MODE_RINGTONE:
            case AUDIO_MODE_IN_COMMUNICATION:
            {
                ::ioctl(mFd, START_MEMIF_TYPE, AudioDigitalType::MEM_DL1); // fp for write indentify
                uint32 DigitalPart = mAudioDigitalControl->DlPolicyByDevice(mDL1Attribute->mdevices);
                SetAnalogFrequency(DigitalPart);
                SetPlayBackPinmux();

                writeDataToEchoReference(buffer, bytes);
                DoStereoMonoConvert((void*)buffer,bytes);

#if defined(BTCVSD_LOOPBACK_WITH_CODEC)
                if( mSourceSampleRate == 8000 )
#else                    
                if (DigitalPart == AudioDigitalType::DAI_BT) 
#endif
                {
#ifdef BT_SW_CVSD
                    BTCVSD_Init();
                    WrittenBytes = WriteDataToBTSCOHW(buffer, bytes);
#else
                    WrittenBytes = WriteDataToBTSCOHW(buffer, bufferSize());
#endif
                }
                else 
                {
                    WrittenBytes =WriteDataToAudioHW(buffer, bufferSize());
                    if(VUnlockhdl != NULL)
                    {
                        VUnlockhdl->SetInputStandBy(false);
                        VUnlockhdl->GetSRCInputParameter(mDL1Attribute->mSampleRate, mDL1Attribute->mChannels);
                        VUnlockhdl->GetStreamOutLatency(mLatency);
                    }
                    //WrittenBytes =::write(mFd, buffer, bufferSize());
                }

                if(NeedAFEDigitalAnalogControl(DigitalPart))
                {
                    // turn on irq 1 INTERRUPT_IRQ1_MCU
                    SetIMcuIRQ(AudioDigitalType::IRQ1_MCU_MODE, mDL1Attribute);
                    EnableIMcuIRQ(AudioDigitalType::IRQ1_MCU_MODE, true);

                    TurnOnAfeDigital(DigitalPart);
                    mAudioDigitalControl->SetAfeEnable(true); // SetAfeEnable after all audSys setting finished
                    
                    mAudioResourceManager->StartOutputDevice();  // open analog device

                    SetMEMIFAttribute(AudioDigitalType::MEM_DL1, mDL1Attribute);
                    SetMEMIFEnable(AudioDigitalType::MEM_DL1, true);
                }
                if(VUnlockhdl != NULL)
                {
                    VUnlockhdl->GetFirstDLTime();
                }
                mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK);
                mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_HARDWARE_LOCK);
                return WrittenBytes;
            }
            case AUDIO_MODE_IN_CALL:
            case AUDIO_MODE_IN_CALL_2: {
                SpeechDriverInterface *pSpeechDriver = SpeechDriverFactory::GetInstance()->GetSpeechDriver();
                BGSPlayer *pBGSPlayer = BGSPlayer::GetInstance();
                pBGSPlayer->mBGSMutex.lock();
                pBGSPlayer->CreateBGSPlayBuffer(mSourceSampleRate , mSourceChannels, format()); // TODO(Harvey): use channels() // Set target sample rate = 16000 Hz
                pBGSPlayer->Open(pSpeechDriver, 0x0, 0xFF);
                pBGSPlayer->mBGSMutex.unlock();
                break;
            }
            default: {
                break;
            }
        }
    }

    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_HARDWARE_LOCK);
    WriteOriginPcmDump((void*)buffer,bytes);

    switch (Mode) {
        case AUDIO_MODE_NORMAL:
        case AUDIO_MODE_RINGTONE:
        case AUDIO_MODE_IN_COMMUNICATION:
        {
            uint32 DigitalPart = mAudioDigitalControl->DlPolicyByDevice(mDL1Attribute->mdevices);
            writeDataToEchoReference(buffer, bytes);
            DoStereoMonoConvert((void*)buffer,bytes);
#if defined(BTCVSD_LOOPBACK_WITH_CODEC)
            if ( mSourceSampleRate == 8000 ) 
#else
            if (DigitalPart == AudioDigitalType::DAI_BT) 
#endif
            {
#ifdef BT_SW_CVSD
                WrittenBytes = WriteDataToBTSCOHW(buffer, bytes);
#else
                WrittenBytes = WriteDataToBTSCOHW(buffer, bufferSize());
#endif
            }
            else 
            {
                //WrittenBytes =::write(mFd, buffer, bufferSize());
                VUnlockhdl->SetInputStandBy(false);
                WrittenBytes =WriteDataToAudioHW(buffer, bufferSize());
            }
            break;
        }
        case AUDIO_MODE_IN_CALL:
        case AUDIO_MODE_IN_CALL_2: {
            WrittenBytes = BGSPlayer::GetInstance()->Write(const_cast<void *>(buffer), bytes);
            break;
        }
        default: {
            break;
        }
    }

    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK);

    return WrittenBytes;
}

ssize_t AudioMTKStreamOut::WriteDataToBTSCOHW(const void *buffer, size_t bytes)
{
    ssize_t WrittenBytes = 0;
    size_t outputSize = 0;
    uint8_t *outbuffer, *inbuf, *workbuf, i;
    uint32_t insize, outsize, workbufsize, total_outsize, src_fs_s, original_insize;
    original_insize = bytes;

#ifdef BT_SW_CVSD
    inbuf = (uint8_t *)buffer;
    do
    {
        outbuffer = mAudioBTCVSDControl->BT_SCO_TX_GetCVSDOutBuf();
        outsize = SCO_TX_ENCODE_SIZE;
        insize = bytes;
        workbuf = mAudioBTCVSDControl->BT_SCO_TX_GetCVSDWorkBuf();
        workbufsize = SCO_TX_PCM64K_BUF_SIZE;
        src_fs_s = mSourceSampleRate;//source sample rate for SRC
        total_outsize = 0;
        i = 0;
        do
        {
            mAudioBTCVSDControl->btsco_process_TX_CVSD(inbuf, &insize, outbuffer, &outsize, workbuf, workbufsize, src_fs_s); //return insize is consumed size
            ALOGD("WriteDataToBTSCOHW, do encode outsize=%d, consumed size=%d, bytes=%d",outsize,insize,bytes);
            outbuffer += outsize;
            inbuf += insize;
            bytes -= insize;
            insize = bytes;
            ASSERT(bytes>=0);
            total_outsize += outsize;
            i++;
        }while((total_outsize<BTSCO_CVSD_TX_OUTBUF_SIZE) && (outsize!=0));
        
        ALOGD("WriteDataToBTSCOHW write to kernel(+) total_outsize=%d",total_outsize);
        if(total_outsize)
        {
            WrittenBytes =::write(mFd2, mAudioBTCVSDControl->BT_SCO_TX_GetCVSDOutBuf(), total_outsize);  //total_outsize should be BTSCO_CVSD_TX_OUTBUF_SIZE!!!
        }
        ALOGD("WriteDataToBTSCOHW write to kernel(-) remaining bytes=%d",bytes);
        
    }while(bytes>0);
    return original_insize;

#else //!BT_SW_CVSD

    outputSize = DoBTSCOSRC(buffer, bytes, (void **)&outbuffer);
    WrittenBytes =::write(mFd, outbuffer, outputSize);
    return WrittenBytes;

#endif
}

ssize_t AudioMTKStreamOut::DoBTSCOSRC(const void *buffer, size_t bytes, void **outbuffer)
{
    size_t outputSize = 0;

    if (mBliSrc) {
        if (mBliSrc->initStatus() != OK) {
            // 6628 only support 8k BTSCO
            ALOGD("DoBTSCOSRC Init BLI_SRC,mDL1Attribute->mSampleRate=%d, target=8000",mDL1Attribute->mSampleRate);
            mBliSrc->init(44100, mDL1Attribute->mChannels, 8000, mDL1Attribute->mChannels);
        }
        *outbuffer = mSwapBufferTwo;
        outputSize = mBliSrc->process(buffer, bytes, *outbuffer);
        if (outputSize <= 0) {
            outputSize = bytes;
            *outbuffer = (void *)buffer;
        }
        return outputSize;
    }
    else {
        ALOGW("DoBTSCOSRC() mBliSrc=NULL!!!");
        *outbuffer = (void *)buffer;
        return bytes;
    }
}

status_t AudioMTKStreamOut::setForceStandby(bool bEnable)
{
    ALOGD("+%s(), bEnable = %d\n", __FUNCTION__, bEnable);
    mForceStandby = bEnable;
    return NO_ERROR;
}

status_t AudioMTKStreamOut::standby()
{
    ALOGD("+AudioMTKStreamOut standby");
    mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK, 3000);
    ALOGD("+AudioMTKStreamOut standby() EnableAudioLock AUDIO_STREAMOUT_LOCK");
    AudioVUnlockDL* VUnlockhdl = AudioVUnlockDL::getInstance();
    //staring DL digital part.
    if (GetStreamRunning() == true) {
        SetStreamRunning(false);
        ClosePcmDumpFile();
        switch (mAudioResourceManager->GetAudioMode())
        {
            case AUDIO_MODE_NORMAL:
            case AUDIO_MODE_RINGTONE:
            case AUDIO_MODE_IN_COMMUNICATION: {
                if(mForceStandby) {
                    mAudioResourceManager->StopOutputDevice();
                }
                else if (mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINR) ||
                    mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINL) ||
                    mAudioDigitalControl->GetI2SConnectStatus() ) {
                    // when analog lin in enable ......
                    ALOGD("AudioMTKStreamOut standby() FM is on, LineInR(%d), LineInL(%d), I2S(%d), do nothing!!!!!!", mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINR), mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINL) , mAudioDigitalControl->GetI2SConnectStatus());
                }
                else {
                    mAudioResourceManager->StopOutputDevice();
                }
                uint32 DigitalPart = mAudioDigitalControl->DlPolicyByDevice(mAudioResourceManager->getDlOutputDevice());
                
                #ifdef BT_SW_CVSD
                #if defined(BTCVSD_LOOPBACK_WITH_CODEC)
                if( mSourceSampleRate == 8000  )
                #else
                if (DigitalPart == AudioDigitalType::DAI_BT)
                #endif
                {
                    mAudioBTCVSDControl->BT_SCO_SET_TXState(BT_SCO_TXSTATE_ENDING);
                    ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_TXSTATE_ENDING); //set kernel state to ENDING  for push remaining TX datat to BT HW
                       mAudioBTCVSDControl->BT_SCO_TX_Stop();
                    mAudioBTCVSDControl->BT_SCO_TX_Close();
                    mAudioBTCVSDControl->BT_SCO_SET_TXState(BT_SCO_TXSTATE_IDLE);
                    ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_TXSTATE_IDLE);
                    ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 1); //free TX working buffers in kernel

                    #if defined(BTCVSD_LOOPBACK_WITH_CODEC) // close AudioMTKStreamOut::AudioMTkRecordThread
                    int ret = 0;
                    if (mBTCVSDRxTestThread.get()) {
                        //ret = mBTCVSDRxTestThread->requestExitAndWait();
                        //if (ret == WOULD_BLOCK) {
                            mBTCVSDRxTestThread->requestExit();
                            //}
                            mBTCVSDRxTestThread.clear();
                        }
                    #endif
                }
                else
                #endif
                {
                if( mAudioDigitalControl->GetI2SConnectStatus() )
                    TurnOffAfeDigital(DigitalPart, true);
                else
                    TurnOffAfeDigital(DigitalPart, false);
                SetMEMIFEnable(AudioDigitalType::MEM_DL1, false); // disable irq 1
                
                EnableIMcuIRQ(AudioDigitalType::IRQ1_MCU_MODE, false);
                if( !mAudioDigitalControl->GetI2SConnectStatus() )
                    mAudioDigitalControl->SetAfeEnable(false);
                ::ioctl(mFd, STANDBY_MEMIF_TYPE, AudioDigitalType::MEM_DL1); // disable mem interface mem1
                #if defined(MTK_VIBSPK_SUPPORT)
                    if(mDL2Vibspk)
                    {
                        SetMEMIFEnable(AudioDigitalType::MEM_DL2, false); // disable irq 1                
                        ::ioctl(mFdVibspk, STANDBY_MEMIF_TYPE, AudioDigitalType::MEM_DL2); // disable mem interface mem1
                        mDL2Vibspk = false;
                        mDL2DelayOff = 0;
                        ALOGD("Vibspk Disable DL2 in standby");
                    }
                #endif
                if(VUnlockhdl != NULL)
                {
                    VUnlockhdl->SetInputStandBy(true);
                }
                }
                break;
            }

            case AUDIO_MODE_IN_CALL:
            case AUDIO_MODE_IN_CALL_2: {
                SpeechDriverInterface *pSpeechDriver = SpeechDriverFactory::GetInstance()->GetSpeechDriver();
                BGSPlayer *pBGSPlayer = BGSPlayer::GetInstance();
                pBGSPlayer->mBGSMutex.lock();
                pBGSPlayer->Close();
                pBGSPlayer->DestroyBGSPlayBuffer();
                pBGSPlayer->mBGSMutex.unlock();
                break;
            }
        }
//        #if defined(BTCVSD_LOOPBACK_WITH_CODEC) 
//        usleep(30*1000); // add delay to avoid audio afe clk is turn off before last ISR comes!
//        #endif
        ReleasePlaybackclock();
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDIO,false);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_HEADPHONE,false);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDENH,false);
        if (mBliSrc) {
            mBliSrc->close();
        }
        StopWriteDataToEchoReference();
    }
    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK);
    ALOGD("-AudioMTKStreamOut standby");
    return NO_ERROR;
}


status_t AudioMTKStreamOut::SetAnalogFrequency(uint32 AfeDigital)
{
    ALOGD("SetAnalogFrequency AfeDigital = %d", AfeDigital);
    switch (AfeDigital) {
        case (AudioDigitalType::DAI_BT): {
            break;
        }
        case (AudioDigitalType::I2S_OUT_DAC): {
            mAudioAnalogControl->SetFrequency(AudioAnalogType::DEVICE_OUT_DAC, mDL1Out->mI2S_SAMPLERATE);
            break;
        }
#if 1
        case (AudioDigitalType::I2S_OUT_2): {
#else
        case (AudioDigitalType::I2S_INOUT_2): {
#endif
            mAudioAnalogControl->SetFrequency(AudioAnalogType::DEVICE_OUT_DAC, mDL1Out->mI2S_SAMPLERATE);
            break;
        }
        default: {
            ALOGD("Turn on default I2S out DAC part");
        }
    }
    return NO_ERROR;
}

status_t AudioMTKStreamOut::TurnOnAfeDigital(uint32 AfeDigital)
{
    ALOGD("TurnOnAfeDigital AfeDigital = %d", AfeDigital);
    switch (AfeDigital) {
        case (AudioDigitalType::DAI_BT): {
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I05, AudioDigitalType::O02);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I06, AudioDigitalType::O02);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::ConnectionShift, AudioDigitalType::I05, AudioDigitalType::O02);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::ConnectionShift, AudioDigitalType::I06, AudioDigitalType::O02);
            SetDAIBTAttribute();
            SetDAIBTOut(true);
            // turn on dai_out
            break;
        }
        case (AudioDigitalType::I2S_OUT_DAC): {
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I05, AudioDigitalType::O03);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I06, AudioDigitalType::O04);
        #if defined(MTK_VIBSPK_SUPPORT)
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I07, AudioDigitalType::O03);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I08, AudioDigitalType::O04);
        #endif
            SetMEMIFEnable(AudioDigitalType::I2S_OUT_DAC, true);
            // turn on DAC_I2S out
            SetI2SOutDACAttribute();
            SetI2SDACOut(true);
            break;
        }
#if 1
        case (AudioDigitalType::I2S_OUT_2): {
#else
        case (AudioDigitalType::I2S_INOUT_2): {
#endif            
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I05, AudioDigitalType::O00);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I06, AudioDigitalType::O01);
#if 1
            SetMEMIFEnable(AudioDigitalType::I2S_OUT_2, true);
#else
            SetMEMIFEnable(AudioDigitalType::I2S_INOUT_2, true);
#endif
            // turn on 2nd_I2S out
#if defined(HDMI_2NDI2S_32BIT)
            // MT8193 need 64*fs , wordlength =32 bits.
            Set2ndI2SOutAttribute(AudioDigtalI2S::NO_SWAP,AudioDigtalI2S::MASTER_MODE,AudioDigtalI2S::NO_INVERSE,AudioDigtalI2S::I2S,AudioDigtalI2S::WLEN_32BITS,sampleRate());
#else
            Set2ndI2SOutAttribute();
#endif
            Set2ndI2SOut(true);
            break;
        }
        default: {
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I05, AudioDigitalType::O03);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I06, AudioDigitalType::O04);
            SetI2SOutDACAttribute();
            SetI2SDACOut(true);
            ALOGD("Turn on default I2S out DAC part");
        }
    }
    return NO_ERROR;
}

status_t AudioMTKStreamOut::TurnOffAfeDigital(uint32 AfeDigital, bool keepDacOpen)
{
    ALOGD("TurnOffAfeDigital AfeDigital = %d", AfeDigital);
    switch (AfeDigital) {
        case (AudioDigitalType::DAI_BT): {
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I05, AudioDigitalType::O02);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I06, AudioDigitalType::O02);
            SetDAIBTOut(false);
            // turn off dai_out
            break;
        }
#if 1
        case (AudioDigitalType::I2S_OUT_2): {
            SetMEMIFEnable(AudioDigitalType::I2S_OUT_2, false);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I05, AudioDigitalType::O00);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I06, AudioDigitalType::O01);
            Set2ndI2SOut(false);
            break;
        }
#else
        case (AudioDigitalType::I2S_INOUT_2): {
            SetMEMIFEnable(AudioDigitalType::I2S_INOUT_2, false);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I05, AudioDigitalType::O00);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I06, AudioDigitalType::O01);
            Set2ndI2SOut(false);
            break;
        }
#endif        
        case (AudioDigitalType::I2S_OUT_DAC): {
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I05, AudioDigitalType::O03);
            mAudioDigitalControl->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I06, AudioDigitalType::O04);
            SetMEMIFEnable(AudioDigitalType::I2S_OUT_DAC, false);
            if(!keepDacOpen)
                SetI2SDACOut(false);
            break;
        }
        default: {
            ALOGD("TurnOffAfeDigital no setting is available");
        }
    }
    return NO_ERROR;
}

void AudioMTKStreamOut::dokeyRouting(uint32_t new_device)
{
    mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_HARDWARE_LOCK, 3000);
    mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_MODE_LOCK, 3000);
    mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK, 3000); // handle for output new_device change , need lock streamout first

    const uint32_t current_device = mAudioResourceManager->getDlOutputDevice();
    ALOGD("%s(), current_device = %d, new_device = %d", __FUNCTION__, current_device, new_device);

    SpeechPhoneCallController::GetInstance()->SetRoutingForTty((audio_devices_t)new_device);

    if ((new_device == 0 || new_device == current_device) ||
        (LoopbackManager::GetInstance()->GetLoopbackType() != NO_LOOPBACK)) {
#ifdef MTK_FM_SUPPORT_WFD_OUTPUT
        if(new_device == AUDIO_DEVICE_NONE && mAudioDigitalControl->GetFmDigitalStatus())
        {
            ALOGD("Replace NonDevice with Speaker for AP-path routing");
            new_device = AUDIO_DEVICE_OUT_SPEAKER;
        }
        else
#endif            
        goto EXIT_SETPARAMETERS;
    }

    SetStereoToMonoFlag(new_device);
    // for DAC path
    //   Turn Off : 6320 off (analog off -> digital off) -> 6589 off
    //   Turn On  : 6589 on -> 6320 on  (digital on -> analog on)
    if (mAudioResourceManager->IsModeIncall() == true)
    {
        SpeechPhoneCallController::GetInstance()->ChangeDeviceForModemSpeechControlFlow(mAudioResourceManager->GetAudioMode(), (audio_devices_t)new_device);

        // TODO(Harvey): Reduce copy & paste code here...
        if (android_audio_legacy::AudioSystem::isBluetoothScoDevice((android_audio_legacy::AudioSystem::audio_devices)new_device) == true) { // BT
            mDL1Attribute->mSampleRate = 8000;
            mDL1Attribute->mInterruptSample = 256;
        }
        else {
            mDL1Attribute->mSampleRate = mSourceSampleRate;
            calInterrupttime();
        }

        mDL1Attribute->mdevices = new_device;
    }
    else if (mStarting == true)
    {
        // only do with outputdevicechanged
        bool outputdevicechange = mAudioDigitalControl->CheckDlDigitalChange(current_device, new_device);
        if (true == outputdevicechange)
        {
            if(!(current_device&AUDIO_DEVICE_OUT_ALL_SCO)&&(new_device&AUDIO_DEVICE_OUT_ALL_SCO)) // change to BTSCO device
            {
                mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK); // disable AUDIO_STREAMOUT_LOCK since standby() will use itemAt
                standby();
                mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK, 3000);
                mDL1Attribute->mSampleRate = 8000;
                mDL1Attribute->mInterruptSample = 256;
                ALOGD("setParameters mStarting=true change to BTSCO device, mDL1Attribute->mSampleRate=%d,mDL1Attribute->mInterruptSample=%d ",mDL1Attribute->mSampleRate,mDL1Attribute->mInterruptSample);
            }
            else if((current_device&AUDIO_DEVICE_OUT_ALL_SCO)&&!(new_device&AUDIO_DEVICE_OUT_ALL_SCO)) // change from BTSCO device
            {
                mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK); // disable AUDIO_STREAMOUT_LOCK since standby() will use it
                standby();
                mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK, 3000);
                mDL1Attribute->mSampleRate = mSourceSampleRate; //mSourceSampleRate is from AudioMTKStreamOut constructor
                calInterrupttime();
                SetI2SOutDACAttribute();
                ALOGD("setParameters mStarting=true change from BTSCO device, mDL1Attribute->mSampleRate=%d,mDL1Attribute->mInterruptSample=%d ",mDL1Attribute->mSampleRate,mDL1Attribute->mInterruptSample);
            }
            else
            {
                int OuputPreDevice = 0 , OutPutNewDevice = 0;
                OuputPreDevice = mAudioDigitalControl->DlPolicyByDevice(current_device);
                OutPutNewDevice = mAudioDigitalControl->DlPolicyByDevice(new_device);
                TurnOffAfeDigital(OuputPreDevice, false);
                TurnOnAfeDigital(OutPutNewDevice);
            }
        }
        //select output new_device
        mAudioResourceManager->SelectOutputDevice(new_device);
        mDL1Attribute->mdevices = new_device;
    }
    else if( mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINR) ||
                 mAudioAnalogControl->GetAnalogState(AudioAnalogType::DEVICE_IN_LINEINL) ||
                 mAudioDigitalControl->GetFmDigitalStatus())
    {
            mAudioResourceManager->SelectOutputDevice(new_device);
            mDL1Attribute->mdevices = new_device;
    }
    else
    {
        if(!(current_device&AUDIO_DEVICE_OUT_ALL_SCO)&&(new_device&AUDIO_DEVICE_OUT_ALL_SCO)) // change to BTSCO device
        {
            mDL1Attribute->mSampleRate = 8000;
            mDL1Attribute->mInterruptSample = 256;
            ALOGD("setParameters mStarting=false change to BTSCO device, mDL1Attribute->mSampleRate=%d,mDL1Attribute->mInterruptSample=%d ",mDL1Attribute->mSampleRate,mDL1Attribute->mInterruptSample);
        }
        else if((current_device&AUDIO_DEVICE_OUT_ALL_SCO)&&!(new_device&AUDIO_DEVICE_OUT_ALL_SCO)) // change from BTSCO device
        {
            mDL1Attribute->mSampleRate = mSourceSampleRate; //mSourceSampleRate is from AudioMTKStreamOut constructor
            SetI2SOutDACAttribute();
            calInterrupttime();
            ALOGD("setParameters mStarting=false change from BTSCO device, mDL1Attribute->mSampleRate=%d,mDL1Attribute->mInterruptSample=%d ",mDL1Attribute->mSampleRate,mDL1Attribute->mInterruptSample);
        }
        mAudioResourceManager->setDlOutputDevice(new_device);
        mDL1Attribute->mdevices = new_device;  //mDL1Attribute->mdevices to be used in AudioMTKStreamOut::write()
    }
EXIT_SETPARAMETERS:
    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMOUT_LOCK);
    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_MODE_LOCK);
    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_HARDWARE_LOCK);
    return ;
}

status_t AudioMTKStreamOut::setParameters(const String8 &keyValuePairs)
{
    AudioParameter param = AudioParameter(keyValuePairs);
    String8 keyRouting = String8(AudioParameter::keyRouting);
    status_t status = NO_ERROR;
    int value = 0;
    ALOGD("setParameters() %s", keyValuePairs.string());
    if (param.getInt(keyRouting, value) == NO_ERROR) {
        param.remove(keyRouting);
        dokeyRouting(value);
        mAudioResourceManager->doSetMode();
    }
    if (param.size()) {
        status = BAD_VALUE;
    }
    return status;
}


String8 AudioMTKStreamOut::getParameters(const String8 &keys)
{
    ALOGD("AudioMTKHardware getParameters\n");
    AudioParameter param = AudioParameter(keys);
    return param.toString();
}

status_t AudioMTKStreamOut::getRenderPosition(uint32_t *dspFrames)
{
    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetStreamRunning(bool bEnable)
{
    ALOGD("+AudioMTKStreamOut SetStreamRunning bEnable = %d",bEnable);
    mStarting = bEnable;
    return NO_ERROR;
}

bool  AudioMTKStreamOut::GetStreamRunning()
{
    return mStarting;
}

uint32_t AudioMTKStreamOut::StreamOutCompFltProcess(AudioCompFltType_t eCompFltType, void *Buffer, uint32 bytes, void *pOutputBuf)
{
    // if return 0, means CompFilter can't do anything. Caller should use input buffer to write to Hw.
    // do post process
    if (mStreamOutCompFltEnable[eCompFltType]) {
        if (mpClsCompFltObj[eCompFltType]) {
            int dBytePerSample = ((format() == AUDIO_FORMAT_PCM_16_BIT) ? sizeof(int16_t) : sizeof(int8_t));
            int inputSampleCount = bytes / dBytePerSample;
            if (inputSampleCount >= 1024) {
                int consumedSampleCount  = inputSampleCount;
                int outputSampleCount = 0;
                mpClsCompFltObj[eCompFltType]->Process((const short *)Buffer, &consumedSampleCount, (short *)pOutputBuf, &outputSampleCount);
                size_t outputbytes = outputSampleCount * dBytePerSample;
                return outputbytes;
            }
        }
    }

    return 0;
}

status_t AudioMTKStreamOut::SetStreamOutCompFltStatus(AudioCompFltType_t eCompFltType, bool bEnable)
{

    if (NULL == mpClsCompFltObj[eCompFltType])
        return INVALID_OPERATION;

    if (false == mStreamOutCompFltEnable[eCompFltType]) {
        if (true == bEnable) {
            mpClsCompFltObj[eCompFltType]->SetWorkMode(channels()==AUDIO_CHANNEL_OUT_MONO?1:2, sampleRate(), mdCompFltMode[eCompFltType]);
            mpClsCompFltObj[eCompFltType]->Start();
            ALOGD("SetStreamOutCompFltStatus eCompFltType = %d  bEnable = %d", eCompFltType, bEnable);
            ALOGD("SetStreamOutCompFltStatus Start CompFilter");
            if(AUDIO_COMP_FLT_AUDENH==eCompFltType)
                ALOGD("AudEnh: Start\n");
        }
    }
    else {
        if (false == bEnable) {
            mpClsCompFltObj[eCompFltType]->Stop();
            mpClsCompFltObj[eCompFltType]->ResetBuffer();
                ALOGD("SetStreamOutCompFltStatus eCompFltType = %d  bEnable = %d", eCompFltType, bEnable);
                ALOGD("SetStreamOutCompFltStatus Stop CompFilter");
            if(AUDIO_COMP_FLT_AUDENH==eCompFltType)
                ALOGD("AudEnh: Stop\n");
        }
    }

    mStreamOutCompFltEnable[eCompFltType] = mStreamOutCompFltApplyStatus[eCompFltType] = bEnable;
    return NO_ERROR;
}

bool AudioMTKStreamOut::GetStreamOutCompFltStatus(AudioCompFltType_t eCompFltType)
{
    if (NULL == mpClsCompFltObj[eCompFltType])
        return false;

    return mStreamOutCompFltEnable[eCompFltType];
}

status_t AudioMTKStreamOut::SetStreamOutCompFltApplyStauts(AudioCompFltType_t eCompFltType, bool bEnable)
{

    if (NULL == mpClsCompFltObj[eCompFltType])
        return INVALID_OPERATION;

    if (false == mStreamOutCompFltEnable[eCompFltType]) {
        if (true == bEnable) {
                SetStreamOutCompFltStatus(eCompFltType,bEnable);
        }
    }
    else {

        if (true == mStreamOutCompFltApplyStatus[eCompFltType])
        {
            if (false == bEnable) {
                ALOGD("AudEnh: Pause\n");
                mpClsCompFltObj[eCompFltType]->Pause();
            }
        }
        else
        {
            if (true == bEnable) {
                ALOGD("AudEnh: Resume\n");
                mpClsCompFltObj[eCompFltType]->Resume();
            }
        }

        mStreamOutCompFltApplyStatus[eCompFltType] = bEnable;
    }

    return NO_ERROR;
}


status_t AudioMTKStreamOut::StreamOutCompFltCreate(AudioCompFltType_t eCompFltType, AudioComFltMode_t eCompFltMode)
{
    ALOGD("StreamOutFLTCreate eCompFltType = %d eCompFltMode = %d", eCompFltType, eCompFltMode);

    if (NULL != mpClsCompFltObj[eCompFltType])
        return ALREADY_EXISTS;

    if (eCompFltType >= AUDIO_COMP_FLT_NUM)
        return BAD_TYPE;

    mpClsCompFltObj[eCompFltType] = new AudioCompensationFilter(eCompFltType,bufferSize());

    if (NULL == mpClsCompFltObj[eCompFltType])
        return NO_INIT;

    mpClsCompFltObj[eCompFltType]->Init();
    mpClsCompFltObj[eCompFltType]->LoadACFParameter();
    mdCompFltMode[eCompFltType] = eCompFltMode;

    return NO_ERROR;

}

status_t AudioMTKStreamOut::StreamOutCompFltDestroy(AudioCompFltType_t eCompFltType)
{
    ALOGD("StreamOutACFDestroy eCompFltType = %d", eCompFltType);

    if (NULL == mpClsCompFltObj[eCompFltType])
        return DEAD_OBJECT;

    mpClsCompFltObj[eCompFltType]->Stop();//For Memory Leak if StreamOut doesn't stop Flt in this timing
    mpClsCompFltObj[eCompFltType]->Deinit();
    delete mpClsCompFltObj[eCompFltType];
    mpClsCompFltObj[eCompFltType] = NULL;
    mStreamOutCompFltEnable[eCompFltType] = false;
    mdCompFltMode[eCompFltType] = AUDIO_CMP_FLT_LOUDNESS_NONE;
    return NO_ERROR;
}

status_t AudioMTKStreamOut::StreamOutCompFltPreviewParameter(AudioCompFltType_t eCompFltType,void *ptr , int len)
{
    ALOGD("StreamOutACFDestroy eCompFltType = %d", eCompFltType);

    if (NULL == mpClsCompFltObj[eCompFltType])
        return DEAD_OBJECT;

    //Stop ?
    mpClsCompFltObj[eCompFltType]->SetACFPreviewParameter((AUDIO_ACF_CUSTOM_PARAM_STRUCT *)ptr);
    //Start ?

    return NO_ERROR;
}

status_t AudioMTKStreamOut::SetSuspend(bool suspend)
{
    if (suspend) {
        mSuspend++;
    }
    else {
        mSuspend--;
        if(mSuspend <0)
        {
            ALOGW("mSuspend = %d",mSuspend);
            mSuspend =0;
        }
    }
    ALOGD("SetSuspend mSuspend = %d suspend = %d", mSuspend, suspend);
    return NO_ERROR;
}

bool AudioMTKStreamOut::GetSuspend()
{
    return mSuspend;
}

status_t AudioMTKStreamOut::dump(int fd, const Vector<String16> &args)
{
    return NO_ERROR;
}

AudioMTKStreamOut::BliSrc::BliSrc()
    : mHandle(NULL), mBuffer(NULL), mInitCheck(NO_INIT)
{
}

AudioMTKStreamOut::BliSrc::~BliSrc()
{
    close();
}

status_t AudioMTKStreamOut::BliSrc::initStatus()
{
    return mInitCheck;
}

status_t  AudioMTKStreamOut::BliSrc::init(uint32 inSamplerate, uint32 inChannel, uint32 OutSamplerate, uint32 OutChannel)
{
    if (mHandle == NULL) {
        uint32_t workBufSize;
        BLI_GetMemSize(inSamplerate, inChannel, OutSamplerate, OutChannel, &workBufSize);
        ALOGD("BliSrc::init InputSampleRate=%u, inChannel=%u, OutputSampleRate=%u, OutChannel=%u, mWorkBufSize = %u",
              inSamplerate, inChannel, OutSamplerate, OutChannel, workBufSize);
        mBuffer = new uint8_t[workBufSize];
        if (!mBuffer) {
            ALOGE("BliSrc::init Fail to create work buffer");
            return NO_MEMORY;
        }
        memset((void *)mBuffer, 0, workBufSize);
        mHandle = BLI_Open(inSamplerate, 2, OutSamplerate, 2, (char *)mBuffer, NULL);
        if (!mHandle) {
            ALOGE("BliSrc::init Fail to get blisrc handle");
            if (mBuffer) {
                delete []mBuffer;
                mBuffer = NULL;
            }
            return NO_INIT;
        }
        mInitCheck = OK;
    }
    return NO_ERROR;

}

size_t  AudioMTKStreamOut::BliSrc::process(const void *inbuffer, size_t inBytes, void *outbuffer)
{
    if (mHandle) {
        size_t inputLength = inBytes;
        size_t outputLength = inBytes;
        size_t consume = BLI_Convert(mHandle, (short *)inbuffer, &inputLength, (short *)outbuffer, &outputLength);
        ALOGD_IF(consume != inBytes, "inputLength=%d,consume=%d,outputLength=%d", inputLength, consume, outputLength);
        return outputLength;
    }
    ALOGW("BliSrc::process src not initialized");
    return 0;
}

status_t  AudioMTKStreamOut::BliSrc::close(void)
{
    if (mHandle) {
        BLI_Close(mHandle, NULL);
        mHandle = NULL;
    }
    if (mBuffer) {
        delete []mBuffer;
        mBuffer = NULL;
    }
    mInitCheck = NO_INIT;
    return NO_ERROR;
}

int AudioMTKStreamOut::GetSampleRate(void)
{
    return mDL1Attribute->mSampleRate;
}
int AudioMTKStreamOut::GetChannel(void)
{
    return mDL1Attribute->mChannels;
}

bool AudioMTKStreamOut::EffectMutexLock(void)
{
    return mEffectLock.lock ();
}
bool AudioMTKStreamOut::EffectMutexUnlock(void)
{
    mEffectLock.unlock ();
    return true;
}

void AudioMTKStreamOut::add_echo_reference(struct echo_reference_itfe *reference)
{
    Mutex::Autolock _l(mEffectLock);
    ALOGD("add_echo_reference %p",reference);
    mEcho_reference = reference;

}
void AudioMTKStreamOut::remove_echo_reference(struct echo_reference_itfe *reference)
{
    Mutex::Autolock _l(mEffectLock);
    ALOGD("remove_echo_reference %p",reference);
    if (mEcho_reference == reference) {
        /* stop writing to echo reference */
        reference->write(reference, NULL);
        mEcho_reference = NULL;
    }
    else
        ALOGW("remove wrong echo reference %p",reference);
}

int AudioMTKStreamOut::get_playback_delay(size_t frames, struct echo_reference_buffer *buffer)
{
    struct timespec tstamp;
    size_t kernel_frames;

    //FIXME:: calculate for more precise time delay

    int rc = clock_gettime(CLOCK_MONOTONIC, &tstamp);
    if (rc != 0) {
        buffer->time_stamp.tv_sec  = 0;
        buffer->time_stamp.tv_nsec = 0;
        buffer->delay_ns           = 0;
        ALOGW("get_playback_delay(): pcm_get_htimestamp error,"
                "setting playbackTimestamp to 0");
        return 0;
    }

    /* adjust render time stamp with delay added by current driver buffer.
     * Add the duration of current frame as we want the render time of the last
     * sample being written. */
    buffer->delay_ns = (long)(((int64_t)(frames)* 1000000000)/
                            mDL1Attribute->mSampleRate);

    buffer->time_stamp = tstamp;

//    ALOGD("get_playback_delay time_stamp = [%ld].[%ld], delay_ns: [%d]",buffer->time_stamp.tv_sec , buffer->time_stamp.tv_nsec, buffer->delay_ns);
    return 0;
}

size_t AudioMTKStreamOut::writeDataToEchoReference(const void* buffer, size_t bytes)
{
#ifdef NATIVE_AUDIO_PREPROCESS_ENABLE
    //push the output data to echo reference
    Mutex::Autolock _l(mEffectLock);
    if (mEcho_reference != NULL) {
        ALOGV("writeDataToEchoReference echo_reference %p",mEcho_reference);
        struct echo_reference_buffer b;
        b.raw = (void *)buffer;
        b.frame_count = bytes/sizeof(int16_t)/mDL1Attribute->mChannels;

       get_playback_delay(b.frame_count, &b);
       mEcho_reference->write(mEcho_reference, &b);
    }
#endif
    return bytes;
}

void AudioMTKStreamOut::StopWriteDataToEchoReference()
{
    /* stop writing to echo reference */
#ifdef NATIVE_AUDIO_PREPROCESS_ENABLE
    Mutex::Autolock _l(mEffectLock);
    ALOGD("StopWriteDataToEchoReference %p",mEcho_reference);
    if (mEcho_reference != NULL) {
        mEcho_reference->write(mEcho_reference, NULL);
        mEcho_reference = NULL;
    }
#endif
}

void AudioMTKStreamOut::SetMusicPlusStatus(bool bEnable)
{
    #if defined(MTK_AUDENH_SUPPORT)

    mUseAudCompFltHeadphoneWithFixedParameter = bEnable;
    #if 0
    if(bEnable)
        property_set("persist.af.audenh.ctrl", "1");
    else
        property_set("persist.af.audenh.ctrl", "0");
    #else
    AUDIO_AUDENH_CONTROL_OPTION_STRUCT audioParam;
    audioParam.u32EnableFlg = bEnable?1:0;    
    SetAudEnhControlOptionParamToNV(&audioParam);
    #endif

    ALOGD("AudEnh: Set Control [%d]\n",bEnable);
    #else
    ALOGW("System Unsupport AudEnh Feature\n");
    #endif

    return;
}

bool AudioMTKStreamOut::GetMusicPlusStatus(void)
{
    ALOGD("AudEnh: Get Control [%d]\n",mUseAudCompFltHeadphoneWithFixedParameter);
    return mUseAudCompFltHeadphoneWithFixedParameter;
}

size_t AudioMTKStreamOut::WriteDataToAudioHW(const void *buffer, size_t bytes)
{
    const uint32_t current_device = mAudioResourceManager->getDlOutputDevice();
    size_t outputSize = 0;
    void * inbuffer = (void *)buffer; // mSwapBufferOne  or mixerbufer of audioflinger
    void * outbuffer =mSwapBufferTwo;
    void * outbuffer2 =mSwapBufferThree;
#if defined(MTK_VIBSPK_SUPPORT)
    void * outbuf_vibspk = mSwapBufferVibSpk;
    void * outbuf_tone = mSwapBufferTone;
#endif
    void * output_final;
    AudioVUnlockDL* VUnlockhdl = AudioVUnlockDL::getInstance();
    //Remove All Filter Definition for Clear trace. However it won't affect Performance if the define is disabled (Return from process function,right now)
    if (current_device & AUDIO_DEVICE_OUT_SPEAKER)
    {
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDIO,true);
        outputSize = StreamOutCompFltProcess(AUDIO_COMP_FLT_AUDIO,inbuffer,bytes,outbuffer);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_HEADPHONE,false);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDENH,false);
    #if defined(MTK_VIBSPK_SUPPORT)
        if(outputSize==0)
            outputSize = bytes;
        else
            inbuffer = outbuffer;
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_VIBSPK,true);
        outputSize = StreamOutCompFltProcess(AUDIO_COMP_FLT_VIBSPK,inbuffer,outputSize,outbuf_vibspk);
        output_final = outbuf_vibspk;
        if(mVibSpk->getVibSpkEnable())
        {
            if(!mDL2Vibspk)
            {
                ::ioctl(mFdVibspk, START_MEMIF_TYPE, AudioDigitalType::MEM_DL2); // fp for write indentify
                memset(outbuf_tone, 0, bytes);
                ::write(mFdVibspk, outbuf_tone, bytes);
                SetMEMIFAttribute(AudioDigitalType::MEM_DL2, mDL1Attribute);
                SetMEMIFEnable(AudioDigitalType::MEM_DL2, true);
                mDL2Vibspk = true;
                ALOGD("Vibspk Enable DL2");
            }
            if(mVibSpkEnable == false)
            {
                mVibSpkEnable = true;
                mVibSpk->VibSpkRampControl(2);
            }
            mVibSpk->VibSpkProcess(bytes, outbuf_tone, mDL1Attribute->mChannels);
            mDL2DelayOff = 5;
        }
        else
        {
            if(mVibSpkEnable == true)
            {
                mVibSpkEnable = false;
                mVibSpk->VibSpkRampControl(1);
                mVibSpk->VibSpkProcess(bytes, outbuf_tone, mDL1Attribute->mChannels);
            }
            else
            {
                mVibSpkEnable = false;
                memset(outbuf_tone, 0, bytes);
                if(mDL2DelayOff > 0)
                {
                    mDL2DelayOff--;
                    ALOGD("Vibspk Disable DL2 CountDown:%x", mDL2DelayOff);
                }
                if(mDL2Vibspk && mDL2DelayOff == 0)
                {
                    SetMEMIFEnable(AudioDigitalType::MEM_DL2, false); // disable irq 1                
                    ::ioctl(mFdVibspk, STANDBY_MEMIF_TYPE, AudioDigitalType::MEM_DL2); // disable mem interface mem1
                    mDL2Vibspk = false;
                    ALOGD("Vibspk Disable DL2");
                }
            }
        }
        if(mDL2Vibspk)
            ::write(mFdVibspk, outbuf_tone, bytes);
        
    #else
        output_final = outbuffer;
    #endif
    }
    else if ((current_device & AUDIO_DEVICE_OUT_WIRED_HEADSET) || (current_device & AUDIO_DEVICE_OUT_WIRED_HEADPHONE))
    {   
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_HEADPHONE,true);

        if (true == mUseAudCompFltHeadphoneWithFixedParameter)
            SetStreamOutCompFltApplyStauts(AUDIO_COMP_FLT_AUDENH,true);
        else
            SetStreamOutCompFltApplyStauts(AUDIO_COMP_FLT_AUDENH,false);

        outputSize = StreamOutCompFltProcess(AUDIO_COMP_FLT_AUDENH,inbuffer,bytes,outbuffer2);

        if(outputSize==0)
            outputSize = bytes;
        else
            inbuffer = outbuffer2;

        outputSize = StreamOutCompFltProcess(AUDIO_COMP_FLT_HEADPHONE,inbuffer,outputSize,outbuffer);

        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDIO,false);
        output_final = outbuffer;
    }
#if defined(MTK_VIBSPK_SUPPORT) && defined(USING_2IN1_SPEAKER)
    else if(current_device & AUDIO_DEVICE_OUT_EARPIECE)
    {
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_VIBSPK,true);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_HEADPHONE,false);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDENH,false);
        SetStreamOutCompFltStatus(AUDIO_COMP_FLT_AUDIO,false);
        outputSize = StreamOutCompFltProcess(AUDIO_COMP_FLT_VIBSPK,inbuffer,bytes,outbuf_vibspk);
        output_final = outbuf_vibspk;
    }
#endif
    else//Other ?
    {
        output_final = (void *)buffer;
    }

    if(outputSize==0)
    {
        output_final=inbuffer; //acf not handle inbuffer;
        outputSize = bytes;
    }
    if (mPFinalPCMDumpFile) {
        int written_data = fwrite((void *)output_final, 1, outputSize, mPFinalPCMDumpFile);
    }

    outputSize =::write(mFd, output_final, outputSize);
    if(VUnlockhdl != NULL)
    {
        //VUnlockhdl->SetInputStandBy(false);
         if ((current_device & AUDIO_DEVICE_OUT_WIRED_HEADSET) || (current_device & AUDIO_DEVICE_OUT_WIRED_HEADPHONE))
        {
            memset(output_final, 0,outputSize);
        }
        VUnlockhdl->WriteStreamOutToRing(output_final, outputSize);
    }

    return  outputSize;
}
#if defined(MTK_VIBSPK_SUPPORT)
const int32_t AUD_VIBR_FILTER_COEF_Table[VIBSPK_FILTER_NUM][2][6][3] = 
{
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_141,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_144,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_147,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_150,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_153,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_156,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_159,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_162,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_165,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_168,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_171,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_174,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_177,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_180,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_183,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_186,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_189,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_192,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_195,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_198,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_201,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_204,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_207,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_210,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_213,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_216,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_219,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_222,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_225,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_228,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_231,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_234,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_237,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_240,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_243,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_246,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_249,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_252,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_255,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_258,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_261,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_264,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_267,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_270,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_273,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_276,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_279,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_282,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_285,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_288,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_291,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_294,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_297,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_300,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_303,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_306,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_309,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_312,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_315,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_318,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_321,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_324,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_327,
DEFAULT_AUD_VIBR_LOUDNESS_FILTER_COEF_330
};

status_t AudioMTKStreamOut::SetVibSpkDefaultParam()
{
   AUDIO_ACF_CUSTOM_PARAM_STRUCT cali_param;
   memset(&cali_param, 0, sizeof(AUDIO_ACF_CUSTOM_PARAM_STRUCT));
   memcpy(&cali_param.bes_loudness_bpf_coeff, &AUD_VIBR_FILTER_COEF_Table[(VIBSPK_DEFAULT_FREQ-VIBSPK_FREQ_LOWBOUND+1)/VIBSPK_FILTER_FREQSTEP],sizeof(uint32_t)*VIBSPK_AUD_PARAM_SIZE); 
   cali_param.bes_loudness_WS_Gain_Min = VIBSPK_DEFAULT_FREQ;
   cali_param.bes_loudness_WS_Gain_Max = VIBSPK_SETDEFAULT_VALUE;
   SetAudioCompFltCustParamToNV(AUDIO_COMP_FLT_VIBSPK, &cali_param);
   ALOGD("[VibSpk] SetDefaultFreq");
   return NO_ERROR;
}

uint32_t AudioMTKStreamOut::GetVibSpkCalibrationStatus()
{
    AUDIO_ACF_CUSTOM_PARAM_STRUCT audioParam;
    GetAudioCompFltCustParamFromNV(AUDIO_COMP_FLT_VIBSPK,&audioParam);
    if(audioParam.bes_loudness_WS_Gain_Max != VIBSPK_CALIBRATION_DONE && audioParam.bes_loudness_WS_Gain_Max != VIBSPK_SETDEFAULT_VALUE)
        return 0;
    else
        return audioParam.bes_loudness_WS_Gain_Min;
}
#endif

#if defined(BT_SW_CVSD)&& defined(BTCVSD_LOOPBACK_WITH_CODEC)  //TEST CODE of BTCVSD

#define CVSD_LOOPBACK_BUFFER_SIZE (180*1000)//BTSCO_CVSD_RX_FRAME*SCO_RX_PCM8K_BUF_SIZE * 10
static uint8_t cvsd_temp_buffer[CVSD_LOOPBACK_BUFFER_SIZE]; //temp buf only for dump to file
static uint32_t cvsd_temp_w = 0;
static uint32_t cvsd_temp_r = 0;
const static uint32_t cvsd_temp_size = CVSD_LOOPBACK_BUFFER_SIZE;

void CVSDLoopbackGetWriteBuffer( uint8_t **buffer, uint32_t *buf_len )// in bytes
{
   int32_t count;

   if( cvsd_temp_r > cvsd_temp_w )
      count = cvsd_temp_r - cvsd_temp_w - 1;
   else if( cvsd_temp_r == 0 )
      count = cvsd_temp_size - cvsd_temp_w - 1;
   else
      count = cvsd_temp_size - cvsd_temp_w;

   *buffer = (uint8_t *)&cvsd_temp_buffer[cvsd_temp_w];
   *buf_len = count;
   ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackGetWriteBuffer: buf_len: %d", count);
}

void CVSDLoopbackGetReadBuffer( uint8_t **buffer, uint32_t *buf_len )// in bytes
{
   int32_t count;

   if( cvsd_temp_w >= cvsd_temp_r )
      count = cvsd_temp_w - cvsd_temp_r;
   else
      count = cvsd_temp_size - cvsd_temp_r;

   *buffer = (uint8_t *)&cvsd_temp_buffer[cvsd_temp_r];
   *buf_len = count;
   ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackGetReadBuffer: buf_len: %d", count);
}

void CVSDLoopbackReadDataDone( uint32_t len ) // in bytes
{
   cvsd_temp_r += len;
   if( cvsd_temp_r == cvsd_temp_size )
      cvsd_temp_r = 0;
   ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackReadDataDone: len: %d", len);
}

void CVSDLoopbackWriteDataDone( uint32_t len ) // in bytes
{
   cvsd_temp_w += len;
   if( cvsd_temp_w == cvsd_temp_size )
      cvsd_temp_w = 0;
   ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackWriteDataDone: len: %d", len);
}

void CVSDLoopbackResetBuffer() // in bytes
{
    memset(cvsd_temp_buffer, 0, CVSD_LOOPBACK_BUFFER_SIZE);
    cvsd_temp_w = 180*100; //if 0, deadlock 
    cvsd_temp_r = 0;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: CVSDLoopbackResetBuffer");
}

int32_t CVSDLoopbackGetFreeSpace()
{
   int32_t count;

   count = cvsd_temp_r - cvsd_temp_w - 1;
   if( count < 0 )
      count += cvsd_temp_size;
   return count; // free size in byte
}

int32_t CVSDLoopbackGetDataCount()
{
   return (cvsd_temp_size - CVSDLoopbackGetFreeSpace() - 1);
}

AudioMTKStreamOut::AudioMTkRecordThread::AudioMTkRecordThread(void *mAudioManager, uint32 Mem_type, char *RingBuffer, uint32 BufferSize)
{
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: AudioMTkRecordThread(+) constructor Mem_type = %d", Mem_type);
    mMemType = Mem_type;
    //mManager = AudioManager;
    mFd2 = -1;
    mAudioBTCVSDControl = AudioBTCVSDControlInterface::getInstance();
    if(!mAudioBTCVSDControl){
        ALOGE("BT_SW_CVSD CODEC LOOPBACK record thread: AudioBTCVSDControlInterface::getInstance() fail");
    }
    if(mMemType==AudioDigitalType::MEM_DAI)
    {
       mFd2 =  ::open(kBTDeviceName, O_RDWR);
       if (mFd2 <= 0)
           ALOGW("BT_SW_CVSD CODEC LOOPBACK record thread: open fail");
    }

    switch (mMemType) {
        case AudioDigitalType::MEM_DAI:
            mName = String8("AudioMTkRecordThreadDAI");
            mAudioBTCVSDControl->BT_SCO_RX_Open();
            ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 2); //allocate RX working buffers in kernel
            mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_INIT);
            mAudioBTCVSDControl->BT_SCO_RX_SetHandle(NULL, NULL, 8000, 1, 0);
            mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_READY);
            mAudioBTCVSDControl->BT_SCO_RX_Start();
            mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_RUNNING);
            ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_RXSTATE_RUNNING); //set state to kernel
            break;
         default:
            ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread:  NO support for memory interface");
            break;
    }
    // ring buffer to copy data into this ring buffer
    mRingBuffer = RingBuffer;
    mBufferSize = BufferSize;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: AudioMTkRecordThread(-)");
}

AudioMTKStreamOut::AudioMTkRecordThread::~AudioMTkRecordThread()
{
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread:  ~AudioMTkRecordThread(+)");
    if(mMemType==AudioDigitalType::MEM_DAI)
    {
        mAudioBTCVSDControl->BT_SCO_RX_Stop();
        mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_ENDING);
        ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_RXSTATE_ENDING); //set kernel state 
        mAudioBTCVSDControl->BT_SCO_RX_Close();
        mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_IDLE);
        ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_RXSTATE_IDLE);
        ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 3); //free RX working buffers in kernel
        
        if (mFd2) {
        ::close(mFd2);
        mFd2 = 0;
        }
    }
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread:  ~AudioMTkRecordThread(-)");
}

void AudioMTKStreamOut::AudioMTkRecordThread::onFirstRef()
{
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: onFirstRef");
    run(mName, ANDROID_PRIORITY_URGENT_AUDIO);
}

// Good place to do one-time initializations
status_t  AudioMTKStreamOut::AudioMTkRecordThread::readyToRun()
{
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: readyToRun");
    return NO_ERROR;
}

void AudioMTKStreamOut::AudioMTkRecordThread::btsco_cvsd_RX_main(void)
{
    uint8_t packetvalid, *outbuf, *workbuf, *tempbuf, *inbuf;
    uint32_t i,Read_Size, outsize, workbufsize, insize, bytes, offset, dump_size;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: btsco_cvsd_RX_main(+)");
    Read_Size = ::read(mFd2, mAudioBTCVSDControl->BT_SCO_RX_GetCVSDTempInBuf(), BTSCO_CVSD_RX_TEMPINPUTBUF_SIZE);

    outbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDOutBuf();
    outsize = SCO_RX_PCM8K_BUF_SIZE;
    workbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDWorkBuf();
    workbufsize = SCO_RX_PCM64K_BUF_SIZE;
    tempbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDTempInBuf();
    inbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDInBuf();
    insize = SCO_RX_PLC_SIZE;
    bytes = BTSCO_CVSD_RX_INBUF_SIZE;
    i = 0;
    offset = 0;
    dump_size = 0;
    do
    {
        packetvalid = *((char *)tempbuf+SCO_RX_PLC_SIZE+offset+i*BTSCO_CVSD_PACKET_VALID_SIZE); //parser packet valid info for each 30-byte packet
        //packetvalid    = 1; //force packvalid to 1 for test
        memcpy(inbuf+offset, tempbuf+offset+i*BTSCO_CVSD_PACKET_VALID_SIZE, SCO_RX_PLC_SIZE);
        ALOGD("btsco_process_RX_CVSD(+) insize=%d,outsize=%d,packetvalid=%d ",insize,outsize,packetvalid);
        mAudioBTCVSDControl->btsco_process_RX_CVSD(inbuf+offset, &insize, outbuf, &outsize, workbuf, workbufsize, packetvalid);
        offset += SCO_RX_PLC_SIZE;
        bytes -= insize;
        ALOGD("btsco_process_RX_CVSD(-) consumed=%d,outsize=%d, bytes=%d",insize,outsize,bytes);
        uint8_t *pWriteBuffer;
        uint32_t uWriteByte;
        uint32_t uTotalWriteByte;
        CVSDLoopbackGetWriteBuffer(&pWriteBuffer, &uWriteByte);
        if(uWriteByte)
        {
            uint32_t uCopyByte = 0;
            if(uWriteByte >= outsize)
            {
                memcpy(pWriteBuffer, outbuf, outsize);
                uCopyByte += outsize;
                CVSDLoopbackWriteDataDone(outsize);
            }
            else
            {
                memcpy(pWriteBuffer, outbuf, uWriteByte);
                uCopyByte += uWriteByte;
                CVSDLoopbackWriteDataDone(uWriteByte);
                CVSDLoopbackGetWriteBuffer(&pWriteBuffer, &uWriteByte);
                if(uWriteByte==0)
                {
                    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: btsco_cvsd_RX_main underflow: uWriteByte: %d, datalen:%d",uWriteByte, outsize-uCopyByte);
                }
                else if(outsize-uCopyByte >= uWriteByte)
                {    //overflow
                    memcpy(pWriteBuffer, outbuf+uCopyByte, uWriteByte);
                    uCopyByte += uWriteByte;
                    CVSDLoopbackWriteDataDone(uWriteByte);
                }
                else
                {
                    memcpy(pWriteBuffer, outbuf+uCopyByte, outsize-uCopyByte);
                    uCopyByte += outsize-uCopyByte;
                    CVSDLoopbackWriteDataDone(outsize-uCopyByte);
                }
            }
        }
#if 0 //TODO: MUST Enale this in non-test code(MTKStreamInManager.cpp))
        //mManager->ApplyVolume(outbuf,outsize);
        //mManager->CopyBufferToClient(mMemType, (void *)outbuf, outsize);
#endif
        outsize = SCO_RX_PCM8K_BUF_SIZE;
        insize = SCO_RX_PLC_SIZE;
        i++;
    }while(bytes>0);
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: btsco_cvsd_RX_main(-)");
}

bool AudioMTKStreamOut::AudioMTkRecordThread::threadLoop()
{
    uint32 Read_Size = 0;
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: threadLoop(+)");
    while (!(exitPending() == true)) {
        if(mMemType==AudioDigitalType::MEM_DAI)
        {
            btsco_cvsd_RX_main();
            return true;
        }
    }
    ALOGD("BT_SW_CVSD CODEC LOOPBACK record thread: threadLoop(-), threadLoop exit");
    return false;
}

#endif

}
