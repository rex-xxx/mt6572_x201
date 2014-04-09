#include "AudioMTKStreamInManager.h"
#include "AudioResourceManager.h"
#include "AudioResourceFactory.h"
#include "AudioAnalogType.h"
#include <utils/String16.h>
#include "AudioUtility.h"

#include "SpeechDriverFactory.h"
#include "AudioBTCVSDControl.h"

#ifdef MTK_AUDIO_HD_REC_SUPPORT
#include "AudioCustParam.h"
#endif

extern "C" {
#include "bli_exp.h"
}

#define LOG_TAG "AudioMTKStreamInManager"
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

#define RECORD_DROP_MS_MAX (200)

#define MTK_STREAMIN_VOLUEM_MAX (0x1000)
#define MTK_STREAMIN_VOLUME_VALID_BIT (12)
namespace android
{

AudioMTKStreamInManager *AudioMTKStreamInManager::UniqueStreamInManagerInstance = NULL;


#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
bool AudioMTKStreamInManager::checkFmMicConflict(AudioMTKStreamInClient *Client, bool status)
{
    bool FmMicConflict = false;
    uint32 sameGroupCount = 0;
    ALOGD("+%s(), Client->mAdcGroup = %d, mAudioInput.size() = %d\n", __FUNCTION__, Client->mAdcGroup, mAudioInput.size());

    if(AudioAnalogType::ADC_GROUP_LINE_IN != Client->mAdcGroup &&
        AudioAnalogType::ADC_GROUP_MIC_IN != Client->mAdcGroup) {
        return FmMicConflict;
    }

    for (int i = 0 ; i < mAudioInput.size() ; i ++) {
        AudioMTKStreamInClient *temp = mAudioInput.valueAt(i);
        if(temp->mAdcGroup == AudioAnalogType::ADC_GROUP_NONE)
            continue;
        if(Client->mAdcGroup != temp->mAdcGroup) {
            ALOGD("%s, find conflict about Mic & FM, device 0x%x - 0x%x", __FUNCTION__,
                    Client->mAttributeClient->mdevices, temp->mAttributeClient->mdevices);
            FmMicConflict = true;
        }
        else if(temp != Client){
            sameGroupCount++; // Other then current client, other clients with the same Group
        }
    }
    for (int i = 0 ; i < mAudioInput.size() ; i ++) {
        AudioMTKStreamInClient *temp = mAudioInput.valueAt(i);
        if(temp->mAdcGroup == AudioAnalogType::ADC_GROUP_NONE)
            continue;
        if(Client->mAdcGroup != temp->mAdcGroup) {
            if(temp->mAdcGroup == AudioAnalogType::ADC_GROUP_LINE_IN){
                if(status == false && sameGroupCount == 0){//When no other stream will conflict Line In
                    temp->mConflict = false;
                    ALOGD("%s, set FM stream as non-conflict device 0x%x ", __FUNCTION__,
                    temp->mAttributeClient->mdevices);
                }
                else if(status == true){
                    temp->mConflict = true;
                    ALOGD("%s, set FM stream as conflict device 0x%x ", __FUNCTION__,
                    temp->mAttributeClient->mdevices);
                }
            }
            else{
                Client->mConflict = status;
                break;
            }
        }
    }

    if(true == FmMicConflict && true == status) {
        ALOGD("%s, set drop count, device 0x%x", __FUNCTION__, Client->mAttributeClient->mdevices);
        Client->mLock.lock();
        Client->mDropCount = 3;
        Client->mLock.unlock();
    }
    return FmMicConflict;
}
#endif

AudioMTKStreamInManager *AudioMTKStreamInManager::getInstance()
{
    if (UniqueStreamInManagerInstance == NULL) {
        ALOGD("+AudioMTKStreamInManager");
        UniqueStreamInManagerInstance = new AudioMTKStreamInManager();
        ALOGD("-AudioMTKStreamInManager");
    }
    ALOGV("getInstance()");
    return UniqueStreamInManagerInstance;
}

void AudioMTKStreamInManager::freeInstance()
{
    return;
}

AudioMTKStreamInManager::AudioMTKStreamInManager()
{
    mClientNumber = 1  ;
    ALOGD("AudioMTKStreamInManager constructor");

    // allcoate buffer
    mAWBbuffer = new char[MemAWBBufferSize];
    mVULbuffer = new char[MemVULBufferSize];
#if DAI_MEMIF_SUPPORT
    mDAIbuffer = new char[MemDAIBufferSize];
#endif
    mMODDAIbuffer = new char[MemDAIBufferSize];
    // get digital control
    mAudioDigital = AudioDigitalControlFactory::CreateAudioDigitalControl();
    mAudioAnalog =  AudioAnalogControlFactory::CreateAudioAnalogControl();
    mAudioResourceManager = AudioResourceManagerFactory::CreateAudioResource();

    mMicMute = false;
    mMuteTransition = false;

    PreLoadHDRecParams();
}

AudioMTKStreamInManager::~AudioMTKStreamInManager()
{
    ALOGD("AudioMTKStreamInManager destructor");
}

status_t  AudioMTKStreamInManager::initCheck()
{
    return NO_ERROR;
}

void AudioMTKStreamInManager::PreLoadHDRecParams(void)
{
#ifdef MTK_AUDIO_HD_REC_SUPPORT
    ALOGD("PreLoadHDRecParams+++");
    //for NVRAM create file first to reserve the memory
    AUDIO_HD_RECORD_SCENE_TABLE_STRUCT DummyhdRecordSceneTable;
    AUDIO_HD_RECORD_PARAM_STRUCT DummyhdRecordParam;
    GetHdRecordSceneTableFromNV(&DummyhdRecordSceneTable);
    GetHdRecordParamFromNV(&DummyhdRecordParam);
    ALOGD("PreLoadHDRecParams---");
#endif
}

uint32_t AudioMTKStreamInManager::CopyBufferToClient(uint32 mMemDataType, void *buffer , uint32 copy_size)
{
    /*
    ALOGD("CopyBufferToClient mMemDataType = %d buffer = %p copy_size = %d mAudioInput.size() = %d buffer = 0x%x",
         mMemDataType, buffer, copy_size, mAudioInput.size(), *(unsigned int *)buffer);*/

    int ret = 0;
    ret = mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMINMANAGER_LOCK, 200);
    if (ret) {
        ALOGW("EnableAudioLock AUDIO_STREAMINMANAGER_LOCK fail ret = %d", ret);
        return 0;
    }
    int FreeSpace = 0;
    for (int i = 0 ; i < mAudioInput.size() ; i ++) {
        AudioMTKStreamInClient *temp = mAudioInput.valueAt(i) ;
        if (temp->mMemDataType == mMemDataType && true == temp->mEnable) {
            temp->mLock.lock();
            FreeSpace =  RingBuf_getFreeSpace(&temp->mRingBuf);
            if (FreeSpace >= copy_size) {
#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
                if(true == temp->mConflict || temp->mDropCount != 0) {
                    RingBuf_fillZero(&temp->mRingBuf, copy_size);
                    if(temp->mDropCount > 0) {
                        temp->mDropCount -= 1;
                        ALOGD("%s, drop count %d, device 0x%x", __FUNCTION__, temp->mDropCount, temp->mAttributeClient->mdevices);
                    }
                } else
#endif
                {
                    //ALOGD("1 RingBuf_copyToLinear FreeSpace = %d temp = %p copy_size = %d mRingBuf = %p", FreeSpace, temp, copy_size, &temp->mRingBuf);
                    RingBuf_copyFromLinear(&temp->mRingBuf, (char *)buffer, copy_size);
                }
            }
            else {
                // do not copy , let buffer keep going
            }
            temp->mWaitWorkCV.signal();
            temp->mLock.unlock();
        }
    }
    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMINMANAGER_LOCK);
    return 0;
}

uint32_t AudioMTKStreamInManager::CopyBufferToClientIncall(RingBuf ul_ring_buf)
{
    mAudioResourceManager->EnableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMINMANAGER_LOCK, 1000);

    // get M2A share buffer record data
    const uint32_t kNumModemData = RingBuf_getDataCount(&ul_ring_buf);
    char *p_modem_data = new char[kNumModemData];
    RingBuf_copyToLinear(p_modem_data, &ul_ring_buf, kNumModemData);

    for (size_t i = 0; i < mAudioInput.size(); ++i) { // copy data to all clients
        AudioMTKStreamInClient *pClient = mAudioInput.valueAt(i) ;
        if (pClient->mEnable) {
            pClient->mLock.lock();

            uint32_t num_free_space = RingBuf_getFreeSpace(&pClient->mRingBuf);

            if (pClient->mBliHandlerBuffer == NULL) { // No need SRC
                //ASSERT(num_free_space >= kNumModemData);
                if (num_free_space < kNumModemData) {
                    ALOGW("%s(), num_free_space(%u) < kNumModemData(%u)", __FUNCTION__, num_free_space, kNumModemData);
                }
                else {
                    RingBuf_copyFromLinear(&pClient->mRingBuf, p_modem_data, kNumModemData);
                }
            }
            else { // Need SRC
                char *p_read = p_modem_data;
                uint32_t num_modem_left_data = kNumModemData;
                uint32_t num_converted_data = num_free_space; // max convert num_free_space

                p_read += BLI_Convert(pClient->mBliHandlerBuffer,
                                      (int16_t *)p_read, &num_modem_left_data,
                                      (int16_t *)pClient->mBliOutputLinearBuffer, &num_converted_data);
                SLOGV("%s(), num_modem_left_data = %u, num_converted_data = %u", __FUNCTION__, num_modem_left_data, num_converted_data);

                if (num_modem_left_data > 0) ALOGW("%s(), num_modem_left_data(%u) > 0", __FUNCTION__, num_modem_left_data);
                //ASSERT(num_modem_left_data == 0);

                RingBuf_copyFromLinear(&pClient->mRingBuf, pClient->mBliOutputLinearBuffer, num_converted_data);
                SLOGV("%s(), pRead:%u, pWrite:%u, dataCount:%u", __FUNCTION__,
                      pClient->mRingBuf.pRead - pClient->mRingBuf.pBufBase,
                      pClient->mRingBuf.pWrite - pClient->mRingBuf.pBufBase,
                      RingBuf_getDataCount(&pClient->mRingBuf));
            }

            pClient->mWaitWorkCV.signal();

            pClient->mLock.unlock();
        }
    }

    // free linear UL data buffer
    delete[] p_modem_data;

    mAudioResourceManager->DisableAudioLock(AudioResourceManagerInterface::AUDIO_STREAMINMANAGER_LOCK);
    return 0;
}

status_t AudioMTKStreamInManager::StartModemRecord(AudioMTKStreamInClient *Client)
{
    // start modem record only for the first client
    if (mAudioInput.size() != 1) {
        ALOGW("%s(), mAudioInput.size() = %u != 1", __FUNCTION__, mAudioInput.size());
        return ALREADY_EXISTS;
    }
    else {
        return SpeechDriverFactory::GetInstance()->GetSpeechDriver()->RecordOn();
    }
}

status_t AudioMTKStreamInManager::StopModemRecord()
{
    // stop modem record only for the last client
    if (mAudioInput.size() != 1) {
        ALOGW("%s(), mAudioInput.size() = %u != 1", __FUNCTION__, mAudioInput.size());
        return ALREADY_EXISTS;
    }
    else {
        return SpeechDriverFactory::GetInstance()->GetSpeechDriver()->RecordOff();
    }
}

bool AudioMTKStreamInManager::checkMemInUse(AudioMTKStreamInClient *Client)
{
    ALOGD("checkMemInUse Client = %p", Client);
    for (int i = 0; i < mAudioInput.size(); i++) {
        AudioMTKStreamInClient *mTemp = mAudioInput.valueAt(i);
        // if mem is the same  and other client is enable , measn this mem is alreay on
        if (mTemp->mMemDataType == Client->mMemDataType && mTemp->mEnable) {
            ALOGD("vector has same memif in use Client->mem = ", Client->mMemDataType);
            return true;
        }
    }
    return false;
}

// this function should start a thread to read kernel sapce buffer. , base on memory type
status_t AudioMTKStreamInManager::StartStreamInThread(uint32 mMemDataType)
{

    ALOGD("StartStreamInThread mMemDataType = %d", mMemDataType);
    switch (mMemDataType) {
        case AudioDigitalType::MEM_VUL:
            mVULThread = new AudioMTkRecordThread(this, mMemDataType , mVULbuffer, MemVULBufferSize);
            if (mVULThread.get()) {
                mVULThread->run();
            }
            break;
        case AudioDigitalType::MEM_DAI:
            mDAIThread = new AudioMTkRecordThread(this, mMemDataType , NULL, 0);
            if (mDAIThread.get()) {
                mDAIThread->run();
            }
            break;
        case AudioDigitalType::MEM_AWB:
            mAWBThread = new AudioMTkRecordThread(this, mMemDataType , mAWBbuffer, MemAWBBufferSize);

            if (mAWBThread.get()) {
                mAWBThread->run();
            }
            break;
        case AudioDigitalType::MEM_MOD_DAI:
            mMODDAIThread = new AudioMTkRecordThread(this, mMemDataType , mMODDAIbuffer, MemDAIBufferSize);
            if (mMODDAIThread.get()) {
                mMODDAIThread->run();
            }
            break;
        default:
            break;
    }
    return NO_ERROR;
}

status_t AudioMTKStreamInManager::AcquireMemBufferLock(AudioDigitalType::Digital_Block MemBlock, bool bEnable)
{
    switch (MemBlock) {
        case AudioDigitalType::MEM_VUL: {
            if (bEnable) {
                mAULBufferLock.lock();
            }
            else {

                mAULBufferLock.unlock();
            }
            break;
        }
#if DAI_MEMIF_SUPPORT
        case AudioDigitalType::MEM_DAI: {
            if (bEnable) {
                mDAIBufferLock.lock();
            }
            else {
                mDAIBufferLock.unlock();
            }
            break;
        }
#endif        
        case AudioDigitalType::MEM_AWB: {
            if (bEnable) {
                mAWBBufferLock.lock();
            }
            else {
                mAWBBufferLock.unlock();
            }
            break;
        }
        case AudioDigitalType::MEM_MOD_DAI: {
            if (bEnable) {
                mMODDAIBufferLock.lock();
            }
            else {
                mMODDAIBufferLock.unlock();
            }
            break;
        }
        default:
            ALOGE("AcquireMemBufferLock with wrong bufer lock");
            return INVALID_OPERATION;
    }
    return NO_ERROR;
}

status_t  AudioMTKStreamInManager::Do_input_standby(AudioMTKStreamInClient *Client)
{
    ALOGD("+Do_input_standby Client = %p", Client);
    uint32 AudioIn1 = 0, AudioIn2 = 0 , AudioOut1 = 0, AudioOut2 = 0;
    int ret = 0;
    Client->mEnable = false;

#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
    bool FmMicConflict = false;
    FmMicConflict = checkFmMicConflict(Client, false);
#endif

    switch (mAudioResourceManager->GetAudioMode()) {
        case AUDIO_MODE_NORMAL:
        case AUDIO_MODE_RINGTONE:
        case AUDIO_MODE_IN_COMMUNICATION: { // fix me, is that mode in communication needs to be care more??
            switch (Client->mMemDataType) {
                case AudioDigitalType::MEM_VUL:
                    AudioOut1 = AudioDigitalType::O09;
                    AudioOut2 = AudioDigitalType::O10;
                    if (mVULThread.get() && !checkMemInUse(Client)) {
                        ret = mVULThread->requestExitAndWait();
                        if (ret == WOULD_BLOCK) {
                            mVULThread->requestExit();
                        }
                        mVULThread.clear();
                        mAudioResourceManager->StopInputDevice();
                        mAudioDigital->SetI2SAdcEnable(false);
                    }
#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
                    if(FmMicConflict && AudioAnalogType::ADC_GROUP_MIC_IN == Client->mAdcGroup) {
                        mAudioResourceManager->SelectInputDevice(AUDIO_DEVICE_IN_FM);
                    }
#endif
                    break;
#if DAI_MEMIF_SUPPORT
                case AudioDigitalType::MEM_DAI:
                    AudioOut1 = AudioDigitalType::O11;
                    AudioOut2 = AudioDigitalType::O11;
                    AudioIn1 = AudioDigitalType::I02;
                    AudioIn2 = AudioDigitalType::I02;
                    if (mDAIThread.get() && !checkMemInUse(Client)) {
                        ret = mDAIThread->requestExitAndWait();
                        if (ret == WOULD_BLOCK) {
                            mDAIThread->requestExit();
                        }
                        mDAIThread.clear();
                    }
                    break;
#else
				case AudioDigitalType::MEM_DAI:
					if (mDAIThread.get() && !checkMemInUse(Client)) {
						ret = mDAIThread->requestExitAndWait();
						if (ret == WOULD_BLOCK) {
							mDAIThread->requestExit();
						}
						mDAIThread.clear();
					}
					break;
#endif                    
                case AudioDigitalType::MEM_AWB:
                    AudioOut1 = AudioDigitalType::O05;
                    AudioOut2 = AudioDigitalType::O06;
                    ALOGD("+Do_input_standby mAWBThread->requestExitAndWait()");
                    if (mAWBThread.get() && !checkMemInUse(Client)) {
                        ret = mAWBThread->requestExitAndWait();
                        if (ret == WOULD_BLOCK) {
                            mAWBThread->requestExit();
                        }
                        mAWBThread.clear();
                    }
                    break;
                default:
                    ALOGD("NO support for memory interface");
                    break;
            }
            // disable memif
            if (!checkMemInUse(Client)) {
                mAudioDigital->SetMemIfEnable(Client->mMemDataType, false);
            }
            // ih no user is used , disable irq2
            if (mAudioInput.size() == 1) {
                mAudioDigital->SetIrqMcuEnable(AudioDigitalType::IRQ2_MCU_MODE, false);
            }

            // AFE_ON = false
            mAudioDigital->SetAfeEnable(false);
            break;
        }
        case AUDIO_MODE_IN_CALL:
        case AUDIO_MODE_IN_CALL_2: {
            StopModemRecord();
            break;
        }
        default:
            break;
    }
    ALOGD("-Do_input_standby Client = %p", Client);
    return NO_ERROR;
}

status_t AudioMTKStreamInManager::I2SAdcInSet(AudioDigtalI2S *AdcI2SIn, AudioStreamAttribute *AttributeClient)
{
    AdcI2SIn->mLR_SWAP = AudioDigtalI2S::NO_SWAP;
    AdcI2SIn->mBuffer_Update_word = 8;
    AdcI2SIn->mFpga_bit_test = 0;
    AdcI2SIn->mFpga_bit = 0;
    AdcI2SIn->mloopback = 0;
    AdcI2SIn->mINV_LRCK = AudioDigtalI2S::NO_INVERSE;
    AdcI2SIn->mI2S_FMT = AudioDigtalI2S::I2S;
    AdcI2SIn->mI2S_WLEN = AudioDigtalI2S::WLEN_16BITS;
    AdcI2SIn->mI2S_SAMPLERATE = (AttributeClient->mSampleRate);
    AdcI2SIn->mI2S_EN = false;
    return NO_ERROR;
}

status_t AudioMTKStreamInManager::Set2ndI2SIn(AudioDigtalI2S *m2ndI2SIn, unsigned int mSampleRate, unsigned char i2s_in_pad_sel, bool bIsSlaveMode)
{
    ALOGD("+%s()\n", __FUNCTION__);
    m2ndI2SIn->mLR_SWAP = AudioDigtalI2S::NO_SWAP;
    if(bIsSlaveMode)
        m2ndI2SIn->mI2S_SLAVE = AudioDigtalI2S::SLAVE_MODE;
    else
    m2ndI2SIn->mI2S_SLAVE = AudioDigtalI2S::MASTER_MODE;
    m2ndI2SIn->mINV_LRCK = AudioDigtalI2S::NO_INVERSE;
    m2ndI2SIn->mI2S_FMT = AudioDigtalI2S::I2S;
    m2ndI2SIn->mI2S_WLEN = AudioDigtalI2S::WLEN_16BITS;
    m2ndI2SIn->mI2S_SAMPLERATE = mSampleRate;
    m2ndI2SIn->mI2S_IN_PAD_SEL = i2s_in_pad_sel;
    mAudioDigital->Set2ndI2SIn(m2ndI2SIn);
    return NO_ERROR;
}

status_t AudioMTKStreamInManager::Enable2ndI2SIn(bool bEnable)
{
    ALOGD("+%s(), bEnable = %d\n", __FUNCTION__, bEnable);
    mAudioDigital->Set2ndI2SInEnable(bEnable);
    return NO_ERROR;
}

status_t  AudioMTKStreamInManager::Do_input_start(AudioMTKStreamInClient *Client)
{
    // savfe interconnection
    ALOGD("+%s(), client = %p\n", __FUNCTION__, Client);
    uint32 AudioIn1 = 0, AudioIn2 = 0 , AudioOut1 = 0, AudioOut2 = 0;
    uint32 MemIfSamplerate = 0, MemIfChannel = 0;
    int ret = 0;


#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
    bool FmMicConflict = false;
    FmMicConflict = checkFmMicConflict(Client, true);
    ALOGD("%s(), FmMicConflict = %d\n", __FUNCTION__, FmMicConflict);
#endif

    switch (mAudioResourceManager->GetAudioMode()) {
        case AUDIO_MODE_NORMAL:
        case AUDIO_MODE_RINGTONE:
        case AUDIO_MODE_IN_COMMUNICATION:
        { // fix me, is that mode in communication needs to be care more??
            ALOGD("%s(), Client->mSourceType = %d, Client->mMemDataType = %d\n", __FUNCTION__, Client->mSourceType, Client->mMemDataType);
            switch (Client->mSourceType)
            {
                    // fix me:: pcm recording curretn get data from modem.
                case AudioDigitalType::MODEM_PCM_1_O:
                case AudioDigitalType::MODEM_PCM_2_O:
                    break;
                case AudioDigitalType::I2S_IN_ADC:
                    I2SAdcInSet(&mAdcI2SIn, Client->mAttributeClient);
                    mAudioDigital->SetI2SAdcIn(&mAdcI2SIn);
                    mAudioAnalog->SetFrequency(AudioAnalogType::DEVICE_IN_ADC, Client->mAttributeClient->mSampleRate);
                    mAudioDigital->SetI2SAdcEnable(true);
#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
                    if(FmMicConflict && AUDIO_DEVICE_IN_FM == Client->mAttributeClient->mdevices) {
                    } else
#endif
                    {
                        // here open analog control
                        mAudioResourceManager->StartInputDevice();
                    }
                    AudioIn1 = AudioDigitalType::I03;
                    AudioIn2 = AudioDigitalType::I04;
                    break;
#if 1
                case AudioDigitalType::I2S_IN_2:
#else
                case AudioDigitalType::I2S_INOUT_2:
#endif                    
                    AudioIn1 = AudioDigitalType::I00;
                    AudioIn2 = AudioDigitalType::I01;
                    if(Client->mAttributeClient->mSource == AUDIO_SOURCE_FM)
                    {
                        //Designer suggest ASRC open first
                        
                        //MT6582 should be Slave Mode I2S, and 32K sampling rate
                        mAudioDigital->Set2ndI2SInConfig(32000,true);//FM Rx I2S in from connsys is 32K/Master
                        mAudioDigital->SetI2SASRCConfig(true,Client->mAttributeClient->mSampleRate);// Covert to 44100                        
                        mAudioDigital->SetI2SASRCEnable(true);        
                        Set2ndI2SIn(&m2ndI2S, Client->mAttributeClient->mSampleRate, AudioDigtalI2S::I2S_IN_FROM_CONNSYS,true);
                        Enable2ndI2SIn(true);
                    }
                    else
                    {
                        //MATV config  NingFeng comment Matv chip is slave mode, audiosys is master mode
                        Set2ndI2SIn(&m2ndI2S, Client->mAttributeClient->mSampleRate, AudioDigtalI2S::I2S_IN_FROM_IO_MUX,false);
                        Enable2ndI2SIn(true);
                    }
                    break;
#if 0                    
                case AudioDigitalType::MRG_I2S_IN:
#if defined(MTK_MERGE_INTERFACE_SUPPORT)
                    mAudioDigital->SetMrgI2SEnable(true, Client->mAttributeClient->mSampleRate);
#endif
                    AudioIn1 = AudioDigitalType::I15;
                    AudioIn2 = AudioDigitalType::I16;
                    break;
                case AudioDigitalType::DAI_BT:
                    AudioIn1 = AudioDigitalType::I02;
                    AudioIn2 = AudioDigitalType::I02;
                    // here for SW MIC gain calculate
                    mAudioResourceManager->StartInputDevice();
                    break;
#endif                    
                default:
                    break;
            }

            switch (Client->mMemDataType)
            {
                case AudioDigitalType::MEM_VUL:
                    AudioOut1 = AudioDigitalType::O09;
                    AudioOut2 = AudioDigitalType::O10;
                    MemIfSamplerate = MemVULSamplerate;
                    MemIfChannel = 2;
                    break;
#if DAI_MEMIF_SUPPORT
                case AudioDigitalType::MEM_DAI:
                    AudioOut1 = AudioDigitalType::O11;
                    AudioOut2 = AudioDigitalType::O11;
                    AudioIn1 = AudioDigitalType::I02;
                    AudioIn2 = AudioDigitalType::I02;
                    MemIfSamplerate = MemDAISamplerate;
                    MemIfChannel = 1;
                    ALOGD("!!!Do_input_start MEM_DAI MemIfChannel=1");
                    break;
#endif                    
                case AudioDigitalType::MEM_AWB:
                    AudioOut1 = AudioDigitalType::O05;
                    AudioOut2 = AudioDigitalType::O06;
#ifdef FM_DIGITAL_IN_SUPPORT
#if 1
                    if(Client->mSourceType == AudioDigitalType::I2S_IN_2)
#else
                    if(Client->mSourceType == AudioDigitalType::MRG_I2S_IN)
#endif                        
                    {
                        MemIfSamplerate = Client->mAttributeClient->mSampleRate; // Use MRGIF interface to record
                    }
                    else
#endif
                    if(Client->mAttributeClient->mSource == AUDIO_SOURCE_MATV)
                        MemIfSamplerate = Client->mAttributeClient->mSampleRate; // mATV default sampling rate
                    else
                        MemIfSamplerate = MemAWBSamplerate;
                    MemIfChannel = 2;
                    break;
                default:
                    ALOGD("NO support for memory interface");
                    break;
            }

            // set digital memif attribute
            if (!checkMemInUse(Client)) {
                ALOGD("checkMemInUse Start memtype = %d", Client->mMemDataType);
				#ifdef BT_SW_CVSD
				if(Client->mMemDataType != AudioDigitalType::MEM_DAI)
				#endif
				{
                	mAudioDigital->SetMemIfSampleRate(Client->mMemDataType, MemIfSamplerate);
                	mAudioDigital->SetMemIfChannelCount(Client->mMemDataType, MemIfChannel);
                	
				}
                StartStreamInThread(Client->mMemDataType);
                #ifdef BT_SW_CVSD
				if(Client->mMemDataType != AudioDigitalType::MEM_DAI)
				#endif
                {
                    mAudioDigital->SetMemIfEnable(Client->mMemDataType, true);
                }
            }

            // set irq enable , need handle with irq2 mcu mode.
            AudioIrqMcuMode IrqStatus;
            mAudioDigital->GetIrqStatus(AudioDigitalType::IRQ2_MCU_MODE, &IrqStatus);
            if (IrqStatus.mStatus == false)
            {
                ALOGD("SetIrqMcuSampleRate mSampleRate = %d", Client->mAttributeClient->mSampleRate);
                #ifndef BT_SW_CVSD
                if (Client->mMemDataType == AudioDigitalType::MEM_DAI)
                {
                    ALOGD("Do_input_start SetIrqMcuSampleRate = 8000, SetIrqMcuCounter=256 ");
                    mAudioDigital->SetIrqMcuSampleRate(AudioDigitalType::IRQ2_MCU_MODE, 8000);
                    mAudioDigital->SetIrqMcuCounter(AudioDigitalType::IRQ2_MCU_MODE, 256); //ccc 1107
                }
                else
                {
                    mAudioDigital->SetIrqMcuSampleRate(AudioDigitalType::IRQ2_MCU_MODE, 16000);
                    mAudioDigital->SetIrqMcuCounter(AudioDigitalType::IRQ2_MCU_MODE, 800); // 50ms
                	mAudioDigital->SetIrqMcuEnable(AudioDigitalType::IRQ2_MCU_MODE, true);
                }
				#else
				if (Client->mMemDataType != AudioDigitalType::MEM_DAI)
                {
					mAudioDigital->SetIrqMcuSampleRate(AudioDigitalType::IRQ2_MCU_MODE, 16000);
                    mAudioDigital->SetIrqMcuCounter(AudioDigitalType::IRQ2_MCU_MODE, 800); // 50ms
                	mAudioDigital->SetIrqMcuEnable(AudioDigitalType::IRQ2_MCU_MODE, true);
				}
                #endif
            }
            else
            {
                ALOGD("IRQ2_MCU_MODE is enabled , use original irq2 interrupt mode");
            }

            // set interconnection
            if (!checkMemInUse(Client)) {
				#ifdef BT_SW_CVSD
				if (Client->mMemDataType != AudioDigitalType::MEM_DAI)
				#endif
				{
                	mAudioDigital->SetinputConnection(AudioDigitalType::Connection, AudioIn1, AudioOut1);
                	mAudioDigital->SetinputConnection(AudioDigitalType::Connection, AudioIn2, AudioOut2);
                	mAudioDigital->SetAfeEnable(true);
				}
            }
            break;
        }
        case AUDIO_MODE_IN_CALL:
        case AUDIO_MODE_IN_CALL_2: {
            StartModemRecord(Client);
            break;
        }
        default:
            break;
    }

    Client->mEnable = true;

    ALOGD("-Do_input_start client = %p", Client);
    return NO_ERROR;
}

AudioMTKStreamInClient *AudioMTKStreamInManager::RequestClient(uint32_t Buflen)
{
    AudioMTKStreamInClient *Client = NULL;
    Client = new AudioMTKStreamInClient(Buflen, mClientNumber);
    ALOGD("RequestClient Buflen = %d Client = %p AudioInput.size  = %d ", Buflen, Client, mAudioInput.size());
    if (Client == NULL) {
        ALOGW("RequestClient but return NULL");
        return NULL;
    }
    //until start should add output
    mClientNumber++;
    mAudioInput.add(mClientNumber, Client);
    Client->mClientId = mClientNumber;
    Client->mEnable = false;
#ifdef ENABLE_SUPPORT_FM_MIC_CONFLICT
    Client->mConflict = false;
    Client->mDropCount = 0;
    Client->mAdcGroup = AudioAnalogType::ADC_GROUP_NONE;
#endif
    ALOGD("%s(), mAudioInput.size() = %d, mClientNumber = %d", __FUNCTION__, mAudioInput.size(), mClientNumber);
    return Client;
}

status_t AudioMTKStreamInManager::ReleaseClient(AudioMTKStreamInClient *Client)
{
    // remove from vector
    uint32_t clientid = Client->mClientId;
    ssize_t index = mAudioInput.indexOfKey(clientid);
    ALOGD("ReleaseClient Client = %p clientid = %d mAudioInput.size  = %d", Client, clientid, mAudioInput.size());
    if (Client != NULL) {
        ALOGD("remove  mAudioInputcloient index = %d", index);
        delete mAudioInput.valueAt(index);
        mAudioInput.removeItem(clientid);
        //Client is deleted;
        Client = NULL;
    }
    ALOGD("ReleaseClient mAudioInput.size = %d", mAudioInput.size());
    return NO_ERROR;
}

void AudioMTKStreamInManager::SetInputMute(bool bEnable)
{
    if(mMicMute != bEnable)
    {
        mMicMute =  bEnable;
        mMuteTransition = false;
    }
}

static short clamp16(int sample)
{
    if ((sample>>15) ^ (sample>>31))
        sample = 0x7FFF ^ (sample>>31);
    return sample;
}

status_t AudioMTKStreamInManager::ApplyVolume(void* Buffer , uint32 BufferSize)
{
    // cehck if need apply mute
    if(mMicMute == true)
    {
        // do ramp down
        if(mMuteTransition == false)
        {
            uint32 count = BufferSize >> 1;
            float Volume_inverse =(float) (MTK_STREAMIN_VOLUEM_MAX/count)*-1;
            short *pPcm = (short*)Buffer;
            int ConsumeSample = 0;
            int value = 0;
            while(count)
            {
                value = *pPcm * (MTK_STREAMIN_VOLUEM_MAX+(Volume_inverse*ConsumeSample));
                *pPcm = clamp16(value>>MTK_STREAMIN_VOLUME_VALID_BIT);
                pPcm++;
                count--;
                ConsumeSample ++;
                //ALOGD("ApplyVolume Volume_inverse = %f ConsumeSample = %d",Volume_inverse,ConsumeSample);
            }
            mMuteTransition = true;
        }
        else
        {
            memset(Buffer,0,BufferSize);
        }
    }
    else if(mMicMute== false)
    {
        // do ramp up
        if(mMuteTransition == false)
        {
            uint32 count = BufferSize >> 1;
            float Volume_inverse = (float)(MTK_STREAMIN_VOLUEM_MAX/count);
            short *pPcm = (short*)Buffer;
            int ConsumeSample = 0;
            int value = 0;
            while(count)
            {
                value = *pPcm * (Volume_inverse*ConsumeSample);
                *pPcm = clamp16(value>>MTK_STREAMIN_VOLUME_VALID_BIT);
                pPcm++;
                count--;
                ConsumeSample ++;
                //ALOGD("ApplyVolume Volume_inverse = %f ConsumeSample = %d",Volume_inverse,ConsumeSample);
            }
            mMuteTransition = true;
        }
    }
    return NO_ERROR;
}


AudioMTKStreamInManager::AudioMTkRecordThread::AudioMTkRecordThread(AudioMTKStreamInManager *AudioManager, uint32 Mem_type, char *RingBuffer, uint32 BufferSize)
{
    ALOGD("AudioMTkRecordThread constructor Mem_type = %d", Mem_type);
    mFd = 0;
    mMemType = Mem_type;
    mManager = AudioManager;
	#ifdef BT_SW_CVSD
	mFd2 = 0;

	mAudioBTCVSDControl = AudioBTCVSDControlInterface::getInstance();
	if(!mAudioBTCVSDControl){
		ALOGE("AudioBTCVSDControlInterface::getInstance() fail");
	}

	if(mMemType==AudioDigitalType::MEM_DAI)
	{
		if (mFd2 == 0) {
		   mFd2 =  ::open(kBTDeviceName, O_RDWR);
		   if (mFd2 <= 0)
			   ALOGW("open device fail");
		}
	}
	#endif

    switch (mMemType) {
        case AudioDigitalType::MEM_VUL:
            mName = String8("AudioMTkRecordThreadVUL");
            mPAdcPCMDumpFile = NULL;
            mPAdcPCMDumpFile = AudioOpendumpPCMFile(streaminmanager, streamin_propty);
            break;
        case AudioDigitalType::MEM_DAI:
            mName = String8("AudioMTkRecordThreadDAI");
            mPDAIInPCMDumpFile = NULL;
            mPDAIInPCMDumpFile = AudioOpendumpPCMFile(streaminDAIBT, streamin_propty);
			#ifdef BT_SW_CVSD
			mAudioBTCVSDControl->BT_SCO_RX_Open();
			::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 2); //allocate RX working buffers in kernel
			mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_INIT);
			mAudioBTCVSDControl->BT_SCO_RX_SetHandle(NULL, NULL, 8000, 1, 0);
			mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_READY);
			mAudioBTCVSDControl->BT_SCO_RX_Start();
			mAudioBTCVSDControl->BT_SCO_SET_RXState(BT_SCO_RXSTATE_RUNNING);
			::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_RXSTATE_RUNNING); //set state to kernel
			#endif
            break;
        case AudioDigitalType::MEM_AWB:
            mName = String8("AudioMTkRecordThreadAWB");
            mPI2SPCMDumpFile = NULL;
            mPI2SPCMDumpFile = AudioOpendumpPCMFile(streaminI2S, streamin_propty); //ccc
            break;
        default:
            ALOGD("NO support for memory interface");
            break;
    }
	if (mFd == 0) {
        mFd =  ::open(kAudioDeviceName, O_RDWR);
        if (mFd <= 0)
            ALOGW("open device fail");
    }
#ifdef BT_SW_CVSD
	if(mMemType != AudioDigitalType::MEM_DAI)
#endif
	{
    	::ioctl(mFd, START_MEMIF_TYPE, mMemType);
	}
    // ring buffer to copy data into this ring buffer
    mRingBuffer = RingBuffer;
    mBufferSize = BufferSize;
}

AudioMTKStreamInManager::AudioMTkRecordThread::~AudioMTkRecordThread()
{
    ALOGD("+AudioMTkRecordThread()");
    ClosePcmDumpFile();

	#ifdef BT_SW_CVSD
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
	else
	#endif
    // do thread exit routine
	{
    if (mFd) {
        ALOGD("threadLoop exit STANDBY_MEMIF_TYPE mMemType = %d", mMemType);
        ::ioctl(mFd, STANDBY_MEMIF_TYPE, mMemType);
    }
	}
	
    if (mFd) {
        ::close(mFd);
        mFd = 0;
    }
    ALOGD("-AudioMTkRecordThread()");
}

void AudioMTKStreamInManager::AudioMTkRecordThread::onFirstRef()
{
    ALOGD("AudioMTkRecordThread onFirstRef");
    tempdata = 0;
    if(mMemType == AudioDigitalType::MEM_VUL)
    {
        mRecordDropms = AUDIO_RECORD_DROP_MS;
    }
    else
    {
        mRecordDropms =0;
    }
    run(mName, ANDROID_PRIORITY_URGENT_AUDIO);
}

// Good place to do one-time initializations
status_t  AudioMTKStreamInManager::AudioMTkRecordThread::readyToRun()
{
    ALOGD("AudioMTkRecordThread::readyToRun()");
    
    if(mMemType == AudioDigitalType::MEM_VUL)
    {
        DropRecordData();
    }
    return NO_ERROR;
}

void AudioMTKStreamInManager::AudioMTkRecordThread::WritePcmDumpData()
{
    int written_data = 0;
    switch (mMemType) {
        case AudioDigitalType::MEM_VUL:
            if (mPAdcPCMDumpFile) {
                written_data = fwrite((void *)mRingBuffer, 1, mBufferSize, mPAdcPCMDumpFile);
            }
            break;
        case AudioDigitalType::MEM_DAI:
            if (mPDAIInPCMDumpFile) {
                written_data = fwrite((void *)mRingBuffer, 1, mBufferSize, mPDAIInPCMDumpFile);
            }
            break;
        case AudioDigitalType::MEM_AWB:
            if (mPI2SPCMDumpFile) {
                written_data = fwrite((void *)mRingBuffer, 1, mBufferSize, mPI2SPCMDumpFile); //ccc
            }
            break;
    }
}

void AudioMTKStreamInManager::AudioMTkRecordThread::ClosePcmDumpFile()
{
    ALOGD("ClosePcmDumpFile");
    switch (mMemType) {
        case AudioDigitalType::MEM_VUL:
            if (mPAdcPCMDumpFile) {
                AudioCloseDumpPCMFile(mPAdcPCMDumpFile);
                ALOGD("ClosePcmDumpFile mPAdcPCMDumpFile");
            }
            break;
        case AudioDigitalType::MEM_DAI:
            if (mPDAIInPCMDumpFile) {
                AudioCloseDumpPCMFile(mPDAIInPCMDumpFile);
                ALOGD("ClosePcmDumpFile mPDAIInPCMDumpFile");
            }
            break;
        case AudioDigitalType::MEM_AWB:
            if (mPI2SPCMDumpFile) {
                AudioCloseDumpPCMFile(mPI2SPCMDumpFile);
                ALOGD("ClosePcmDumpFile mPI2SPCMDumpFile");
            }
            break;
    }
}

void AudioMTKStreamInManager::AudioMTkRecordThread::DropRecordData()
{
    int Read_Size = 0;
    int Read_Buffer_Size = 0;
    // drop data for pop
    if(mRecordDropms != 0)
    {
          if(mRecordDropms > RECORD_DROP_MS_MAX){
              mRecordDropms = RECORD_DROP_MS_MAX;
          }
          Read_Buffer_Size = ((AudioMTKStreamInManager::MemVULSamplerate * mRecordDropms << 2) / 1000);
          ALOGD("1. DropRecordData Read_Buffer_Size = %d Read_Size = %d",Read_Buffer_Size,Read_Size);
    }
    while(Read_Buffer_Size > 0)
    {
        if(Read_Buffer_Size > mBufferSize){
            Read_Size = ::read(mFd, mRingBuffer, mBufferSize);
        }
        else
        {
            Read_Size = ::read(mFd, mRingBuffer, Read_Buffer_Size);
        }
        Read_Buffer_Size -= mBufferSize;
        ALOGD("DropRecordData Read_Buffer_Size = %d Read_Size = %d",Read_Buffer_Size,Read_Size);
    }
}

#ifdef BT_SW_CVSD
void AudioMTKStreamInManager::AudioMTkRecordThread::btsco_cvsd_RX_main(void)
{
	uint8_t packetvalid, *outbuf, *workbuf, *tempbuf, *inbuf;
	uint32_t i,Read_Size, outsize, workbufsize, insize, bytes, offset;

	ALOGD("btsco_cvsd_RX_main(+)");

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
	do
	{

				packetvalid = *((char *)tempbuf+SCO_RX_PLC_SIZE+offset+i*BTSCO_CVSD_PACKET_VALID_SIZE); //parser packet valid info for each 30-byte packet
				//packetvalid	= 1; //force packvalid to 1 for test
				memcpy(inbuf+offset, tempbuf+offset+i*BTSCO_CVSD_PACKET_VALID_SIZE, SCO_RX_PLC_SIZE);
		
		#ifdef BTCVSD_TEST_HW_ONLY
			#if 0
				int j;
				ALOGD("btsco_cvsd_RX_main print RX inbuf \n");
				for(j=0;j<SCO_RX_PLC_SIZE/5;j++)
				{
					ALOGD("%d %d %d %d %d", *((uint8_t *)inbuf+offset+j*5),*((uint8_t *)inbuf+offset+j*5+1),*((uint8_t *)inbuf+offset+j*5+2),*((uint8_t *)inbuf+offset+j*5+3),*((uint8_t *)inbuf+offset+j*5+4));
		
				}
			#endif
			WritePcmDumpData(inbuf+offset,insize);
		#endif
		
				ALOGD("btsco_process_RX_CVSD(+) insize=%d,outsize=%d,packetvalid=%d ",insize,outsize,packetvalid);
				mAudioBTCVSDControl->btsco_process_RX_CVSD(inbuf+offset, &insize, outbuf, &outsize, workbuf, workbufsize, packetvalid);
				offset += SCO_RX_PLC_SIZE;
				bytes -= insize;
				ALOGD("btsco_process_RX_CVSD(-) consumed=%d,outsize=%d, bytes=%d",insize,outsize,bytes);
				//WritePcmDumpData();

				mManager->ApplyVolume(outbuf,outsize);
				mManager->CopyBufferToClient(mMemType, (void *)outbuf, outsize);
		
				outsize = SCO_RX_PCM8K_BUF_SIZE;
				insize = SCO_RX_PLC_SIZE;
				i++;
	}while(bytes>0);
	ALOGD("btsco_cvsd_RX_main(-)");
}
#endif
bool AudioMTKStreamInManager::AudioMTkRecordThread::threadLoop()
{
    uint32 Read_Size = 0;
    while (!(exitPending() == true)) {
		#ifdef BT_SW_CVSD
		if(mMemType==AudioDigitalType::MEM_DAI)
		{
			btsco_cvsd_RX_main();
	        return true;
		}
		else
		#endif
		{
        //ALOGD("AudioMTkRecordThread threadLoop() read mBufferSize = %d mRingBuffer = %p ", mBufferSize, mRingBuffer);
        Read_Size = ::read(mFd, mRingBuffer, mBufferSize);
        WritePcmDumpData();
        mManager->ApplyVolume(mRingBuffer,mBufferSize);
        mManager->CopyBufferToClient(mMemType, (void *)mRingBuffer, mBufferSize);
        return true;
		}

    }
    ALOGD("threadLoop exit");
    return false;
}

}




