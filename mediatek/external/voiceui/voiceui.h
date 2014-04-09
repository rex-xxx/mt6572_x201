#ifndef _VOICEUI_H
#define _VOICEUI_H


#define ___open_new_interface_with_cust__
#define ___use_dual_mic_uiEndPointDetection__

#ifdef __cplusplus
extern "C" {
#endif

int getVoiceUiVersion();

typedef struct __VoiceUiCustomParameters
{
    unsigned char Cust_Para[8][32];

    short  *voiceFIRCoefMic1;
    short  *voiceFIRCoefMic2;
    int  voiceGainMic1;
    int  voiceGainMic2;
    int  micNumFlag;

} VOICE_UI_CUSTOM_INFO;

/*Initialize Keyword Spotting Engine, (dir: resource directory where model files are stored)*/
#ifdef ___open_new_interface_with_cust__
void initKWS(const char* dir, 
             VOICE_UI_CUSTOM_INFO *voiceCustomInfo);
#else
void initKWS(const char* dir);
#endif

/*Release Keyword Spotting Engine*/
void releaseKWS();

/*Set Downlink Latency, (downlink_latency: the downlink latency)*/
void setUIDLLatency(int downlink_latency);

/*Start Keyword Spotting, (pBuff: a buffer for relevant command patterns, length: the length of the buffer, ncommands: the number of commands in the buffer) (return value: <0 fail; 0 not keyword; >0 recognized keyword;)*/
int doKeywordSpotting(void *pBuff, int length, int ncommands);

/*Judge If The Current Frame Is Endpoint, i.e End Of Speech Segment, (pMicBuf1: main uplink recorded voice, pMicBuf2: reference uplink recorded voice, pDLBuf, downlink recorded voice)*/
#ifdef ___use_dual_mic_uiEndPointDetection__
int uiEndPointDetection (short *pMicBuf1, short *pMicBuf2, short *pDLBuf);
#else
int uiEndPointDetection (short *pMicBuf1, short *pDLBuf);
#endif


#ifdef __cplusplus
}
#endif

#endif

