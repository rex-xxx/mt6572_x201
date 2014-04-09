#ifndef _AUDIO_BTCVSD_CONTROL_H_
#define _AUDIO_BTCVSD_CONTROL_H_

#if defined(MT6572) || defined(MT6582)
#define BT_SW_CVSD	//Enable SW BT CVSD
//#define BTCVSD_LOOPBACK_WITH_CODEC
//#define BTCVSD_ENC_DEC_LOOPBACK //copy TX output buf to RX inbuf directly, without writing to kernel
//#define BTCVSD_KERNEL_LOOPBACK //enable this option when early porting in 6589 phone
//#define DUMP_TXSRC_TEST
//#define DUMP_CVSDRX_OUTPUT
//#define TXOUT_RXIN_TEST
//#define BTCVSD_TEST_HW_ONLY
#endif

#ifdef BT_SW_CVSD

#include "AudioType.h"
#include "AudioBTCVSDDef.h"
#ifdef DUMP_TXSRC_TEST
#include "AudioUtility.h"
#endif

extern "C" {
#include "bli_exp.h"
#include "cvsd_codec_exp.h"
}

namespace android
{

enum BT_SCO_STATE{
	BT_SCO_TXSTATE_IDLE=0x0,
	BT_SCO_TXSTATE_INIT,
	BT_SCO_TXSTATE_READY,
	BT_SCO_TXSTATE_RUNNING,
	BT_SCO_TXSTATE_ENDING,
	BT_SCO_RXSTATE_IDLE=0x10,
	BT_SCO_RXSTATE_INIT,
	BT_SCO_RXSTATE_READY,
	BT_SCO_RXSTATE_RUNNING,
	BT_SCO_RXSTATE_ENDING,
	BT_SCO_TXSTATE_DIRECT_LOOPBACK
} ;

class BT_SCO_TX 
{
	public:
		
   void          *pSRCHandle;
   void          *pEncHandle;
   void          *pHPFHandle;

   void          (*pCallback)( void *pData );
   void          *pUserData;

   uint16_t    uSampleRate;
   uint8_t     uChannelNumber;

   bool      fEnableFilter;
   bool      fEnablePLC;

   uint8_t     PcmBuf_64k[SCO_TX_PCM64K_BUF_SIZE];
   uint32_t    uPcmBuf_w;
} ;

class BT_SCO_RX
{
	public:

   //handle
   void          *pDecHandle;
   void          *pHPFHandle;
   void          *pPLCHandle;
   void          *pSRCHandle_1;
   void          *pSRCHandle_2;

   //callback
   void          (*pCallback)( void *pData );
   void          *pUserData;

   uint16_t    uSampleRate;
   uint8_t     uChannelNumber;
   bool      fEnableSRC2;
   bool      fEnableFilter;
   bool      fEnablePLC;

   //temp buffer
   uint8_t     PcmBuf_64k[SCO_RX_PCM64K_BUF_SIZE];
   uint8_t     PcmBuf_8k[SCO_RX_PCM8K_BUF_SIZE];
   uint32_t    uPcmBuf_r; //for PcmBuf_8k
} ;

class BTSCO_CVSD_Context
{
	public:
		
   BT_SCO_TX *pTX;   //btsco.pTx
   BT_SCO_RX *pRX;
   uint8_t *pStructMemory;
   uint8_t *pTXWorkingMemory;
   uint8_t *pRXWorkingMemory;
   uint16_t uAudId;
   BT_SCO_STATE uTXState;
   BT_SCO_STATE uRXState;
   bool  fIsStructMemoryOnMED;
};


class AudioBTCVSDControlInterface
{
    public:
		
		enum BT_SCO_MODE{
		  BT_SCO_MODE_CVSD,
		  BT_SCO_MODE_MSBC
		} ;
		
		enum BT_SCO_LINK{
		  BT_SCO_LINK_TX_ONLY,
		  BT_SCO_LINK_RX_ONLY,
		  BT_SCO_LINK_BOTH,
		} ;
		
		enum BT_SCO_MODULE{
		  BT_SCO_MOD_CVSD_ENCODE,
		  BT_SCO_MOD_CVSD_DECODE,
		  BT_SCO_MOD_FILTER_TX,
		  BT_SCO_MOD_FILTER_RX,
		  BT_SCO_MOD_PLC,
		  BT_SCO_MOD_CVSD_TX_SRC,
		  BT_SCO_MOD_CVSD_RX_SRC1,
		  BT_SCO_MOD_CVSD_RX_SRC2,
		  BT_SCO_MOD_PCM_RINGBUF_TX,
		  BT_SCO_MOD_PCM_RINGBUF_RX,
		} ;
		
		enum BT_SCO_DIRECT{
		  BT_SCO_DIRECT_BT2ARM,
		  BT_SCO_DIRECT_ARM2BT
		} ;
		
		enum BT_SCO_PACKET_LEN{
		  BT_SCO_CVSD_30 =0,
		  BT_SCO_CVSD_60 =1,
		  BT_SCO_CVSD_90 =2,
		  BT_SCO_CVSD_120=3,
		  BT_SCO_CVSD_10 =4,
		  BT_SCO_CVSD_20 =5,
		  BT_SCO_CVSD_MAX=6
		} ;

      static AudioBTCVSDControlInterface *getInstance();
		static void freeInstance();
			void BT_SCO_CVSD_Init(void);
			void BT_SCO_CVSD_DeInit(void);
  			void BT_SCO_SET_TXState(BT_SCO_STATE state);
			void BT_SCO_SET_RXState(BT_SCO_STATE state);
			void BT_SCO_RX_Open(void);
			int BT_SCO_RX_SetHandle(void(*pCallback)( void *pData ), void *pData, uint32_t uSampleRate, uint32_t uChannelNumber, uint32_t uEnableFilter);
			void BT_SCO_RX_Start(void);
			void BT_SCO_RX_Stop(void);
			void BT_SCO_RX_Close(void);
			void BT_SCO_RX_DestroyModule(void);
			void btsco_cvsd_RX_main(void); 
			void btsco_process_RX_CVSD(void *inbuf, uint32_t *insize, void *outbuf, uint32_t *outsize, void *workbuf, const uint32_t workbufsize, uint8_t packetvalid);
			void btsco_process_TX_CVSD(void *inbuf, uint32_t *insize, void *outbuf, uint32_t *outsize, void *workbuf, const uint32_t workbufsize, uint32_t src_fs_s);
			void BT_SCO_TX_Open(void);
			int BT_SCO_TX_SetHandle(void(*pCallback)( void *pData ), void *pData, uint32_t uSampleRate, uint32_t uChannelNumber, uint32_t uEnableFilter);
			void BT_SCO_TX_Start(void);
			void BT_SCO_TX_Stop(void);
			void BT_SCO_TX_Close(void);
			void BT_SCO_TX_DestroyModule(void);
			void BT_SCO_TX_SetCVSDOutBuf(uint8_t *addr);
			uint8_t *BT_SCO_RX_GetCVSDTempInBuf(void);
			void BT_SCO_RX_SetCVSDTempInBuf(uint8_t *addr);
			uint8_t *BT_SCO_RX_GetCVSDInBuf(void);
			void BT_SCO_RX_SetCVSDInBuf(uint8_t *addr);
			uint8_t *BT_SCO_RX_GetCVSDOutBuf(void);
			uint8_t *BT_SCO_RX_GetCVSDWorkBuf(void);
			uint8_t *BT_SCO_TX_GetCVSDOutBuf(void);
			uint8_t *BT_SCO_TX_GetCVSDWorkBuf(void);
			BTSCO_CVSD_Context *mBTSCOCVSDContext;  //btsco

		  private:
			
			AudioBTCVSDControlInterface();
			~AudioBTCVSDControlInterface();
			uint32_t BT_SCO_GetMemorySize_4ByteAlign(BT_SCO_MODULE uModule);
			void BT_SCO_InitialModule(BT_SCO_MODULE uModule, uint8_t *pBuf);
			static AudioBTCVSDControlInterface *UniqueAudioBTCVSDControlInterface;
			uint8_t *mBTCVSDRXTempInBuf;
			uint8_t *mBTCVSDRXInBuf;
			uint8_t *mBTCVSDTXOutBuf;
			#ifdef DUMP_TXSRC_TEST
			FILE *mTXSRCPCMDumpFile;
			#endif
			#ifdef DUMP_CVSDRX_OUTPUT
			FILE *mBTCVSDRXDumpFile;
			#endif
			uint32_t Audio_IIRHPF_GetBufferSize(int a);
			void *Audio_IIRHPF_Init(int8_t *pBuf, const int32_t *btsco_FilterCoeff_8K, int a);
			void Audio_IIRHPF_Process(void *handle, uint16_t *inbuf, uint16_t *InSample, uint16_t *outbuf, uint16_t *OutSample);
			#if 0 //only replace .so API temporily
			uint32_t CVSD_DEC_GetBufferSize(void);
			uint32_t CVSD_ENC_GetBufferSize(void);

			void *CVSD_DEC_Init(int8_t*pBuf);
			void *CVSD_ENC_Init(int8_t*pBuf);

			void CVSD_DEC_Process(
			   void   *pHandle,  //handle
			   char   *pInBuf,   //input CVSD packet
			   int    *pInLen,   //input length (Byte)
			   short  *pOutBuf,  //output Sample
			   int    *pOutLen   //output length (Word)
			);
			void CVSD_ENC_Process(
			   void   *pHandle,  //handle
			   short  *pInBuf,   //input Samples
			   int    *pInLen,   //input length (word)
			   char   *pOutBuf,  //CVSD packet
			   int    *pOutLen,  //output Length (byte)
			   short  iGain      //Digital Gain 0~0x7FFF
			);
			void g711plc_construct(void *pBuf);
			uint32_t g711plc_GetMemorySize(void);
			void g711plc_addtohistory(void *lc, int16_t *s);
			void g711plc_dofe(void *lc,int16_t *out);
			#endif
};
}
#endif

#endif
