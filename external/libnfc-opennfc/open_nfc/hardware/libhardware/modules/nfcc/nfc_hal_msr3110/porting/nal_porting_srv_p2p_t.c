#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>

#include <arpa/inet.h>
#include <semaphore.h>  /* Semaphore */

#include "nfc_hal.h"
#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "interface.h"
#include "nal_porting_srv_main.h"
#include "nal_porting_srv_p2p_t.h"
#include "nal_porting_srv_util.h"
#include "nal_porting_srv_admin.h"
#include "nal_porting_srv_p2p.h"

// ==================== code control ====================
#define CODE_CONTROL_DETECT_SDDATR_ON 1



// ==================== data structures ====================
#define REPETITION_TIMEOUT_SIZE			4
#define LINK_ACTIVE_MSG_BUF_SIZE		255
typedef struct
{
	unsigned char RepetitionTimeout[REPETITION_TIMEOUT_SIZE];
	unsigned char TypeAFlag;
	unsigned char ActiveModeFlag;
	unsigned char LinkActMsg[LINK_ACTIVE_MSG_BUF_SIZE];
	int LinkActMsgLen;
} _P2pTargetCardConfig;

#define P2P_TARGET_DEP_DATA_HEADER_SIZE 4
#define P2P_TARGET_DEP_DATA_CONTENT_SIZE 1024
typedef struct
{
	unsigned char DepDataHeader[P2P_TARGET_DEP_DATA_HEADER_SIZE];
	unsigned char DepDataContent[P2P_TARGET_DEP_DATA_CONTENT_SIZE];
	int DepDataContentLen;
	int DepDataIdx;
	int DepDataCurSendLen;
} _P2pTargetDepData;

typedef struct
{
	unsigned char Protocol;
	unsigned char OptionFlag;
	unsigned int Timeout;
} _A3P2pTargetSddReqInput;

typedef struct
{
	unsigned char Protocol;
	unsigned char OptionFlag;
	unsigned int Timeout;
	unsigned char *DepData;
	int DepDataLen;
} _A3P2pTargetDepReqInput;

typedef struct
{
	unsigned char Protocal;
	unsigned int SddTimeout;
	unsigned int DepTimeout;
	unsigned char *AtrContent;
	int AtrContenLen;
} _A3P2pTargetSddAtrReqInput;


// ==================== global data members ====================

// NFC Command Code
#define NFC_DEP_INITIATOR_CODE 	0xD4
#define NFC_DEP_TARGET_CODE 	0xD5

#define NFC_CMD_ATR_REQ 0x00
#define NFC_CMD_ATR_RSP 0x01
#define NFC_CMD_PSL_REQ 0xD4
#define NFC_CMD_PSL_RSP 0xD5
#define NFC_CMD_DEP_REQ 0x06
#define NFC_CMD_DEP_RSP 0x07
#define NFC_CMD_DSL_REQ 0x08
#define NFC_CMD_DSL_RSP 0x09
#define NFC_CMD_RLS_REQ 0x0A
//#define NFC_CMD_RLS_REQ 0x0B

#define PFB_INF	0x00
#define PFB_ACK 0x40
#define PFB_SUP 0x80

#define PFB_INF_MI 		( 0x10)
#define PFB_INF_NAD  	( 0x08)
#define PFB_INF_DID  	( 0x04)
#define PFB_INF_PNI  	( 0x03)

#define PFB_ACK_NACK	( 0x10)
#define PFB_ACK_NAD  	( 0x08)
#define PFB_ACK_DID  	( 0x04)
#define PFB_ACK_PNI  	( 0x03)

#define PFB_SUP_ATN  	( 0x00)
#define PFB_SUP_TO  	( 0x10)
#define PFB_SUP_NAD  	( 0x08)
#define PFB_SUP_DID  	( 0x04)
#define PFB_SUP_RFU  	( 0x03)


#define PFB_INF_MASK 		0xE0
#define PFB_ACK_MASK 		0xE0
#define PFB_SUP_MASK 		0xE0

#define PFB_INF_MI_MASK		( 0x10)
#define PFB_INF_NAD_MASK  	( 0x08)
#define PFB_INF_DID_MASK  	( 0x04)
#define PFB_INF_PNI_MASK  	( 0x03)

#define PFB_ACK_NACK_MASK	( 0x10)
#define PFB_ACK_NAD_MASK  	( 0x08)
#define PFB_ACK_DID_MASK  	( 0x04)
#define PFB_ACK_PNI_MASK  	( 0x03)

#define PFB_SUP_ATN_MASK  	( 0x10)
#define PFB_SUP_NAD_MASK  	( 0x08)
#define PFB_SUP_DID_MASK  	( 0x04)
#define PFB_SUP_RFU_MASK  	( 0x03)

//ATR command
static unsigned char NFCID3i[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05};
static unsigned char DIDi = 0x00;
static unsigned char BSi  = 0x00;
static unsigned char BRi  = 0x00;
static unsigned char PPi  = 0x00;

static unsigned char NFCID2t[] = { 0x01, 0xFE, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03};
static unsigned char NFCID3t[] = { 0x01, 0xFE, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05};
static unsigned char DIDt = 0x00;
static unsigned char BSt  = 0x00;
static unsigned char BRt  = 0x00;
static unsigned char TO  = 0x0E;
static unsigned char PPt  = 0x22;


/* ms_open_nfc_p2p_target_cmd_set_param() */
static _P2pTargetCardConfig P2pTargetCardConfig;

// DEP command
#define MaxDepBatchSize 192 //64
static unsigned char DepSendDataBuffer[255];
static int DepSendDataBufferLen;
static int DepSendDataBufferIdx;
static int DepSendCurrentBatchSize;
static unsigned char DepSendCurrentPni;


/* static unsigned int ms_get_detection_timeout() */
//#define DETECTION_TIMEOUT_LIST_CNT 5
//static unsigned short DetectionTimeoutList[] =
//      { 400, 800, 1200, 1600, 2000};
//#define DETECTION_TIMEOUT_LIST_CNT 2
//static unsigned short DetectionTimeoutList[] =
//      { 100, 1000 };
#define DETECTION_TIMEOUT_LIST_CNT 3
static unsigned short DetectionTimeoutList[] =
   { 200, 220, 240 };

static unsigned short DetectionPollingCntList[] =
      { 25, 27, 30 };
/* int ms_open_nfc_p2p_target_detection_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen) */
#define ATR_GEN_DATA_BUFFER_SIZE 256
static int P2pTargetDetctionCount = 0;
#define P2P_TARGET_DETECT_COUNT_MAX 3

/* static int ms_open_nfc_p2p_target_i2t_atr_rsp( tNALMsrComInstance  *pDev ) */
#define P2P_PROTOCAL_SB 0xF0
#define P2P_TARGET_DEP_RCV_TIMEOUT 2000 //miliseconds

/* int ms_open_nfc_p2p_target_i2t_atr_rsp_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen) */
static int P2pTargetI2tAtrRspCallbackCount = 0;
#define P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX 1  // 3

/* static void ms_open_nfc_p2p_target_dep_init() */
static int DepTotalPkg;
static unsigned char DepDataBuffer[255];
static int DepDataBufferLen;
static unsigned char ExpectDepPni = 0x00;

/* int ms_open_nfc_p2p_target_i2t_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen) */
static int P2pTargetI2tDepReqCallbackCount = 0;
#define P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX 0 // 1 jim test

/* int ms_open_nfc_p2p_target_t2i_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen) */
static int P2pTargetT2iDepReqCallbackCount = 0;
#define P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX 0 // 1 jim test

static unsigned char DetectionRfParam;
extern unsigned char Extern_RF_Detect;
#define P2P_TARGET_DEP_EXTEND_TIMEOUT 800 //miliseconds
#define P2P_424_TARGET_DEP_EXTEND_POLLING_CNT 65 //CNT * 10 miliseconds
// ==================== Function List ====================

// private functions
static int ms_open_nfc_p2p_target_cmd_set_param( tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_p2p_target_evt_send_data( tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_p2p_target_i2t_atr_rsp( tNALMsrComInstance  *pDev );
static int ms_open_nfc_p2p_target_i2t_dep_req( tNALMsrComInstance  *pDev, unsigned char *pDepData, int DepDataLen);
static int ms_open_nfc_p2p_target_t2i_dep_req( tNALMsrComInstance  *pDev);
static void ms_open_nfc_p2p_target_dep_init();
static void ms_open_nfc_p2p_target_depsend_init();


// callback functions
int ms_open_nfc_p2p_target_i2t_atr_rsp_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_p2p_target_detection_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int Rcvlen);
int ms_open_nfc_p2p_target_i2t_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_p2p_target_t2i_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);

// utilities for A3
static int ms_a3_p2p_target_sdd_req( tNALMsrComInstance  *pDev, _A3P2pTargetSddReqInput *A3P2pTargetSddReqInput);
static int ms_a3_p2p_target_dep_req( tNALMsrComInstance  *pDev, _A3P2pTargetDepReqInput *A3P2pTargetDepReqInput);
static int ms_a3_recieve_only( tNALMsrComInstance  *pDev);
static int ms_a3_p2p_target_sddatr_req( tNALMsrComInstance  *pDev, _A3P2pTargetSddAtrReqInput *A3P2pTargetSddAtrReqInput);
static int ms_a3_p2p_target_ask_req( tNALMsrComInstance  *pDev);

// utilities for local
static unsigned int ms_get_detection_timeout();







//---- 18092: 212/424 ---//

#define NFCID2_LEN 8
#define NFCID3_LEN 10
typedef struct
{
	DrvP2P_speed_et P2pSpeedType;
	DrvP2P_IorT_et P2pRole;
	unsigned char PollingCount;
	DrvP2P_NFCID_et NfcId2GenType;
	unsigned char *NfcId2;
	DrvP2P_NFCID_et NfcId3GenType;
	unsigned char *NfcId3;
	unsigned char DID;
	unsigned char BS;
	unsigned char BR;
	unsigned char TO;
	unsigned char PP;
	unsigned char LinkActMsgLen;
	unsigned char *LinkActMsg;
} _MsNfcctrl18092Enablep2pReqInput;

typedef struct
{
	bool_t CtrlInfoFlag;
	unsigned char WT;
	unsigned char RTOX;
	unsigned char DepFlag;
	int DepContentLen;
	unsigned char *DepContent;
} _MsNfcctrl18092DepReqInput;

DrvP2P_speed_et g_P2pSpeedType;

static int P2pTargetEnableCount = 0;
#define P2P_TARGET_ENABLE_COUNT_MAX 3

static int P2pTargetEnablePostCallbackCount = 0;
#define P2P_TARGET_ENABLE_POST_CALLBACK_COUNT_MAX 0 //jim test: ori: 1


static int ms_nfcctrl_18092_enablep2p_req( tNALMsrComInstance *pDev, _MsNfcctrl18092Enablep2pReqInput *MsNfcctrl18092Enablep2pReqInput);
static int ms_nfcctrl_18092_dep_req( tNALMsrComInstance *pDev, _MsNfcctrl18092DepReqInput *MsNfcctrl18092DepReqInput);
static int ms_nfcctrl_18092_dep_receive_only_req( tNALMsrComInstance *pDev);
//int ms_nfc_p2p_target_enable_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_nfc_p2p_target_enable_post_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
static void ms_nfc_p2p_target_i2t_dep_init();
static int ms_nfc_p2p_target_i2t_dep_req( tNALMsrComInstance  *pDev, unsigned char *pDepData, int DepDataLen);
int ms_nfc_p2p_target_i2t_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_nfc_p2p_target_t2i_dep_req( tNALMsrComInstance  *pDev);
int ms_nfc_p2p_target_t2i_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);

static int ms_target_pl_config_req( tNALMsrComInstance *pDev, _MsNfcctrl18092Enablep2pReqInput *MsNfcctrl18092Enablep2pReqInput);

// ==================== Functions ====================

// public functions
int ms_open_nfc_p2p_target_disp( tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
    int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_disp() - start ");

	//if ( pRcvBuf== NULL || RcvLen == 0)
    //{
    //	pDev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
	//	pDev->rx_buffer[1] = NAL_RES_BAD_DATA;
	//	pDev->nb_available_bytes = 2;
    //    return -1;
    //}

    switch( pDev->curCmdCode)
    {
        case NAL_CMD_SET_PARAMETER:
            retVal = ms_open_nfc_p2p_target_cmd_set_param( pDev, pRcvBuf, RcvLen);
            break;

        case NAL_EVT_P2P_SEND_DATA:
            retVal = ms_open_nfc_p2p_target_evt_send_data( pDev, pRcvBuf, RcvLen);
            break;

        default:
            //pDev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
            //pDev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
            //pDev->nb_available_bytes = 2;

			// Do Nothing
            break;
    }

	PNALDebugLog( "ms_open_nfc_p2p_target_disp() - end ");

    return retVal;
}

int ms_open_nfc_p2p_target_detection( tNALMsrComInstance  *pDev, unsigned char SddRfParam)
{

#if CODE_CONTROL_DETECT_SDDATR_ON != 1
	PNALDebugLog( "ms_open_nfc_p2p_target_detection() - start ");

	int retVal = 0;

	int interfaceSendRet;
	_A3P2pTargetSddReqInput a3P2pTargetSddReqInput;



	a3P2pTargetSddReqInput.Protocol = 0x00;
	a3P2pTargetSddReqInput.OptionFlag = 0x00;
	a3P2pTargetSddReqInput.Timeout = ms_get_detection_timeout();

	PNALDebugLog( "[ms_open_nfc_p2p_target_detection]detection timeout: %d", a3P2pTargetSddReqInput.Timeout);


	interfaceSendRet = ms_a3_p2p_target_sdd_req( pDev, &a3P2pTargetSddReqInput);
	retVal = interfaceSendRet;
	pDev->cbRecv = (RecvCallBack *)ms_open_nfc_p2p_target_detection_callback;

	PNALDebugLog( "ms_open_nfc_p2p_target_detection() - end ");
	return retVal;
#else
	PNALDebugLog( "ms_open_nfc_p2p_target_detection() - start ");

	int retVal = 0;

	int interfaceSendRet;
	_A3P2pTargetSddAtrReqInput a3P2pTargetSddAtrReqInput;
	unsigned char atrResContent[255];

       DetectionRfParam = SddRfParam;
	a3P2pTargetSddAtrReqInput.Protocal = DetectionRfParam;
       if (SddRfParam & SDD_EXTERN_RF_DETECT)
      {
	    a3P2pTargetSddAtrReqInput.SddTimeout = P2P_TARGET_DEP_EXTEND_TIMEOUT;
      }
      else
      {
          a3P2pTargetSddAtrReqInput.SddTimeout = ms_get_detection_timeout();
      }
	a3P2pTargetSddAtrReqInput.DepTimeout= P2P_TARGET_DEP_RCV_TIMEOUT;

	atrResContent[0] = P2P_PROTOCAL_SB;
	atrResContent[1] = 18 + P2pTargetCardConfig.LinkActMsgLen;
	atrResContent[2] = NFC_DEP_TARGET_CODE;
	atrResContent[3] = NFC_CMD_ATR_RSP;
	memcpy( atrResContent + 4, NFCID3t, 10);
	atrResContent[14] = DIDt;
	atrResContent[15] = BSt;
	atrResContent[16] = BRt;
	atrResContent[17] = TO;
	atrResContent[18] = PPt;
	memcpy( atrResContent + 19, P2pTargetCardConfig.LinkActMsg, P2pTargetCardConfig.LinkActMsgLen);

	a3P2pTargetSddAtrReqInput.AtrContent = atrResContent;
	a3P2pTargetSddAtrReqInput.AtrContenLen = 19 + P2pTargetCardConfig.LinkActMsgLen;


	PNALDebugLog( "[ms_open_nfc_p2p_target_detection]detection timeout: %d", a3P2pTargetSddAtrReqInput.SddTimeout);


	interfaceSendRet = ms_a3_p2p_target_sddatr_req( pDev, &a3P2pTargetSddAtrReqInput);
	retVal = interfaceSendRet;
	pDev->cbRecv = (RecvCallBack *)ms_open_nfc_p2p_target_detection_callback;

	PNALDebugLog( "ms_open_nfc_p2p_target_detection() - end ");
	return retVal;

#endif
}


// private functions
static int ms_open_nfc_p2p_target_cmd_set_param( tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_cmd_set_param() - start ");

	unsigned char paramCode = 0x00;
	int idxForLoop;

	paramCode = pRcvBuf[2];

	PNALDebugLog( "[ms_open_nfc_p2p_target_cmd_set_param]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_p2p_target_cmd_set_param]pRcvBuf: ");
      PNALDebugLogBuffer(pRcvBuf, RcvLen);
      /*
	for( idxForLoop = 0; idxForLoop < RcvLen; idxForLoop++)
	{
		PNALDebugLog( "%02x ", pRcvBuf[idxForLoop]);
	}
	PNALDebugLog( "");
      */
	switch( paramCode)
	{
		case NAL_PAR_CARD_CONFIG:
			PNALDebugLog( "ms_open_nfc_p2p_target_cmd_set_param(): NAL_PAR_CARD_CONFIG - start ");

			memcpy( P2pTargetCardConfig.RepetitionTimeout, pRcvBuf + 3, REPETITION_TIMEOUT_SIZE);
			P2pTargetCardConfig.TypeAFlag = pRcvBuf[7];
			P2pTargetCardConfig.ActiveModeFlag = pRcvBuf[8];
			P2pTargetCardConfig.LinkActMsgLen = RcvLen - 9;
			memcpy( P2pTargetCardConfig.LinkActMsg, pRcvBuf + 9, P2pTargetCardConfig.LinkActMsgLen);

#if (A3_PL_MODE == 0)
			pDev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
			pDev->rx_buffer[1] = NAL_RES_OK;
			pDev->nb_available_bytes = 2;
#else
			// alan temp revise
			pDev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
			pDev->rx_buffer[1] = NAL_RES_OK;
			pDev->nb_available_bytes = 2;
			// alan temp revise
			
            //ms_nfc_p2p_target_enable( pDev, 0x00); // alan temp revise
#endif

			PNALDebugLog( "ms_open_nfc_p2p_target_cmd_set_param(): NAL_PAR_CARD_CONFIG - end ");

			break;

		default:
			PNALDebugLog( "ms_open_nfc_p2p_target_cmd_set_param(): default param- start ");

			pDev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
			pDev->rx_buffer[1] = NAL_RES_UNKNOWN_PARAM;
			pDev->nb_available_bytes = 2;

			PNALDebugLog( "ms_open_nfc_p2p_target_cmd_set_param(): default param - end ");

			break;
	}

	PNALDebugLog( "ms_open_nfc_p2p_target_cmd_set_param() - end ");

	return retVal;
}

static int ms_open_nfc_p2p_target_i2t_atr_rsp( tNALMsrComInstance  *pDev )
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp() - start ");

	int interfaceSendRet;
	unsigned char cmdAtrRspData[255];
	int cmdAtrRspDataLen;
	int idxForLoop;
	_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;

	// prepare ATR response RF data
	cmdAtrRspData[0] = P2P_PROTOCAL_SB;
	//cmdAtrRspData[1] = 19 + P2pTargetCardConfig.LinkActMsgLen;
	cmdAtrRspData[1] = 18 + P2pTargetCardConfig.LinkActMsgLen;
	cmdAtrRspData[2] = NFC_DEP_TARGET_CODE;
	cmdAtrRspData[3] = NFC_CMD_ATR_RSP;
	memcpy( cmdAtrRspData + 4, NFCID3t, 10);
	cmdAtrRspData[14] = DIDt;
	cmdAtrRspData[15] = BSt;
	cmdAtrRspData[16] = BRt;
	cmdAtrRspData[17] = TO;
	cmdAtrRspData[18] = PPt;
	memcpy( cmdAtrRspData + 19, P2pTargetCardConfig.LinkActMsg, P2pTargetCardConfig.LinkActMsgLen);
	cmdAtrRspDataLen = 19 + P2pTargetCardConfig.LinkActMsgLen;

	PNALDebugLog( "[ms_open_nfc_p2p_target_atr_rsp]cmdAtrRspDataLen: %d", cmdAtrRspDataLen);
	PNALDebugLog( "[ms_open_nfc_p2p_target_atr_rsp]cmdAtrRspData:");
    PNALDebugLogBuffer(cmdAtrRspData, cmdAtrRspDataLen);

	a3P2pTargetDepReqInput.Protocol = 0x00;
	a3P2pTargetDepReqInput.OptionFlag = 0x02;
	a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
	a3P2pTargetDepReqInput.DepData = cmdAtrRspData;
	a3P2pTargetDepReqInput.DepDataLen = cmdAtrRspDataLen;

	interfaceSendRet = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
	retVal = interfaceSendRet;

	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp() - end ");

	return retVal;
}

static void ms_open_nfc_p2p_target_dep_init()
{
	PNALDebugLog( "ms_open_nfc_p2p_target_dep_init() - start ");

	DepTotalPkg = 0;
	DepDataBufferLen = 0;
	ExpectDepPni = 0x00;

	PNALDebugLog( "ms_open_nfc_p2p_target_dep_init() - end ");
}

static int ms_open_nfc_p2p_target_i2t_dep_req( tNALMsrComInstance  *pDev, unsigned char *pDepData, int DepDataLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - start ");

	unsigned char cmdCat;
	unsigned char cmdCode;
	unsigned char iPfb;
	unsigned char iDid = 0x00;
	unsigned char iNad = 0x00;
	unsigned char genData[255];
	int genDataLen;
	unsigned char iMi;
	unsigned char iPni;
	int parsingIdx;
	unsigned char cmdRes[255];
	_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;
	unsigned char iAtn;


	// Dep Data: Parsing
	// Data: 0xD4| 0x06| PFB| [DID]| [NAD]| Data
	cmdCat = pDepData[2];
	cmdCode = pDepData[3];
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]cmdCat: %02x", cmdCat);
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]cmdCode: %02x", cmdCode);
	if( ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ)
	{
		// no response
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]wrong command: ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ.");
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
		retVal = ms_a3_recieve_only( pDev);
		return -1;
	}

	iPfb = pDepData[4];
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]iPfb: %02x", iPfb);

	parsingIdx = 5;
	if( ( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID)
	{
		iDid = pDepData[ parsingIdx++];
	}

	if( ( iPfb & PFB_INF_NAD_MASK) == PFB_INF_NAD)
	{
		iNad = pDepData[ parsingIdx++];
	}
	genDataLen = DepDataLen - parsingIdx - 2;
	if( genDataLen > 0)
	{
		memcpy( genData, pDepData + parsingIdx, genDataLen);
	}

	// Dep Data: Check
	// DID check
	if( (( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID))
	{
		if( DIDt == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]Wrong DID Flag in PFB, DIDt: %02x", DIDt);
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
			return 0;
		}

		if( iDid != DIDt)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]Wrong DID, DIDt: %02x, iDid: %02x", DIDt, iDid);
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
			return 0;
		}

	}




	switch( iPfb & PFB_INF_MASK)
	{
		case PFB_INF:
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]PFB: PFB_INF ");
			iMi = iPfb & PFB_INF_MI;
			iPni = iPfb & 0x03;
			if( iPni != ExpectDepPni)
			{
				PNALDebugError( "[ms_open_nfc_p2p_target_i2t_dep_req]wrong PNI ");
				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
				retVal = ms_a3_recieve_only( pDev);
				return 0;
			}
			memcpy( DepDataBuffer + DepDataBufferLen, genData, genDataLen);
			DepDataBufferLen += genDataLen;
			DepTotalPkg++;
			if( ( iMi & PFB_INF_MI_MASK) != PFB_INF_MI)
			{
				// no chaining, send data to AP
				// return
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]no chaining ");
				pDev->temp_rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
				pDev->temp_rx_buffer[1] = NAL_EVT_P2P_SEND_DATA;
				memcpy( pDev->temp_rx_buffer + 2, DepDataBuffer, DepDataBufferLen);
				//pDev->nb_available_bytes = 2 + DepDataBufferLen;
                SetEventAvailableBytes(pDev, 2 + DepDataBufferLen);
				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
				return 0;
			}

			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]chaining ");
			ExpectDepPni = ( ExpectDepPni + 1) % 4;
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]reply ACK ");
			cmdRes[0] = P2P_PROTOCAL_SB;
			//cmdRes[1] = 0;
			cmdRes[2] = NFC_DEP_TARGET_CODE;
			cmdRes[3] = NFC_CMD_DEP_RSP;
			cmdRes[4] = PFB_ACK;
			parsingIdx = 5;
			if( ( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID)
			{
				cmdRes[4] |= PFB_INF_DID;
				cmdRes[parsingIdx++] = iDid;
			}

			if( ( iPfb & PFB_INF_NAD_MASK) == PFB_INF_NAD)
			{
				cmdRes[4] |= PFB_INF_NAD;
				cmdRes[parsingIdx++] = iNad;
			}
			//cmdRes[1] = parsingIdx - 2;
			cmdRes[1] = parsingIdx - 1;

			a3P2pTargetDepReqInput.Protocol = 0x00;
			a3P2pTargetDepReqInput.OptionFlag = 0x02;
			a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
			a3P2pTargetDepReqInput.DepData = cmdRes;
			a3P2pTargetDepReqInput.DepDataLen = parsingIdx;
			retVal = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
			pDev->cbRecv = ( RecvCallBack*)ms_open_nfc_p2p_target_i2t_dep_req_callback;
			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
			return retVal;

			break;

		case PFB_ACK:
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]PFB: PFB_ACK ");
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]wrong PFB: PFB_ACK ");
			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
			retVal = ms_a3_recieve_only( pDev);
			return 0;

			break;

		case PFB_SUP:
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]PFB: PFB_SUP ");
			iAtn = iPfb & PFB_SUP_ATN_MASK;
			if( iAtn == PFB_SUP_TO)
			{
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req]Timeout not support ");
			}
			cmdRes[0] = P2P_PROTOCAL_SB;
			//cmdRes[1] = 0;
			cmdRes[2] = NFC_DEP_TARGET_CODE;
			cmdRes[3] = NFC_CMD_DEP_RSP;
			cmdRes[4] = iPfb;
			parsingIdx = 5;
			if( ( iPfb & PFB_SUP_DID_MASK) == PFB_SUP_DID)
			{
				//cmdRes[2] |= PFB_SUP_DID;
				cmdRes[parsingIdx++] = iDid;
			}

			if( ( iPfb & PFB_SUP_NAD_MASK) == PFB_SUP_NAD)
			{
				//cmdRes[2] |= PFB_SUP_NAD;
				cmdRes[parsingIdx++] = iNad;
			}
			if( genDataLen > 0)
			{
				memcpy( cmdRes + parsingIdx, genData, genDataLen);
				parsingIdx = parsingIdx + genDataLen;
			}
			//cmdRes[1] = parsingIdx - 2;
			cmdRes[1] = parsingIdx - 1;

			a3P2pTargetDepReqInput.Protocol = 0x00;
			a3P2pTargetDepReqInput.OptionFlag = 0x02;
			a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
			a3P2pTargetDepReqInput.DepData = cmdRes;
			a3P2pTargetDepReqInput.DepDataLen = parsingIdx;
			retVal = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
			pDev->cbRecv = ( RecvCallBack*)ms_open_nfc_p2p_target_i2t_dep_req_callback;
			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
			return retVal;

			break;

		default:
			PNALDebugError( "[ms_open_nfc_p2p_target_i2t_dep_req]wrong PFB: others ");
			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req() - end ");
			retVal = ms_a3_recieve_only( pDev);
			return 0;
			break;
	}

	return retVal;

}


static int ms_open_nfc_p2p_target_evt_send_data( tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_evt_send_data - start ");

	//unsigned short p2pCmd;
	int idxForLoop;

	PNALDebugLog ("pRcvBuf: ");
    PNALDebugLogBuffer(pRcvBuf, RcvLen);

	ms_open_nfc_p2p_target_depsend_init();
	DepSendDataBufferLen = RcvLen - 2;
	memcpy( DepSendDataBuffer, pRcvBuf + 2, RcvLen - 2);
	//retVal = ms_open_nfc_p2p_target_t2i_dep_req( pDev);
	retVal = ms_nfc_p2p_target_t2i_dep_req( pDev);

	PNALDebugLog( "[ms_open_nfc_p2p_target_evt_send_data]P2P_CMD_RSP_DEP - end ");

	PNALDebugLog( "ms_open_nfc_p2p_target_evt_send_data - end ");
	return retVal;
}



static void ms_open_nfc_p2p_target_depsend_init()
{
	PNALDebugLog( "ms_open_nfc_p2p_target_depsend_init() - start ");

	DepSendDataBufferLen = 0;
	DepSendDataBufferIdx = 0;
	DepSendCurrentBatchSize = 0;
	//DepSendCurrentPni = 0x00;
      P2pTargetT2iDepReqCallbackCount = 0; //JIM ADD
	PNALDebugLog( "ms_open_nfc_p2p_target_depsend_init() - end ");
}


static int ms_open_nfc_p2p_target_t2i_dep_req( tNALMsrComInstance  *pDev)
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req() - start ");

	_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;
	unsigned char cmdDepRsp[255];
	int interfaceSendRet;
	int remainBufferSize;
	int bufferIdx;

	remainBufferSize = DepSendDataBufferLen - DepSendDataBufferIdx;
	PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req]remainBufferSize: %d", remainBufferSize);
	PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req]DepSendDataBufferLen: %d", DepSendDataBufferLen);
	PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req]DepSendDataBufferIdx: %d", DepSendDataBufferIdx);

	cmdDepRsp[0] = P2P_PROTOCAL_SB;
	//cmdDepRsp[1] = 0;
	cmdDepRsp[2] = NFC_DEP_TARGET_CODE;
	cmdDepRsp[3] = NFC_CMD_DEP_RSP;
	cmdDepRsp[4] = PFB_INF | DepSendCurrentPni;

	bufferIdx = 5;
	if( DIDt != 0x00)
	{
		cmdDepRsp[4] |= PFB_INF_DID;
		cmdDepRsp[ bufferIdx] = DIDt;
		bufferIdx++;
	}


	if( remainBufferSize > MaxDepBatchSize)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req]chaining handling");
		cmdDepRsp[4] |= PFB_INF_MI;
		memcpy( cmdDepRsp + bufferIdx, DepSendDataBuffer + DepSendDataBufferIdx, MaxDepBatchSize);
		a3P2pTargetDepReqInput.Protocol = 0x00;
		a3P2pTargetDepReqInput.OptionFlag = 0x02;
		a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
		a3P2pTargetDepReqInput.DepData = cmdDepRsp;
		a3P2pTargetDepReqInput.DepDataLen = bufferIdx + MaxDepBatchSize;

		DepSendCurrentBatchSize = MaxDepBatchSize;
	}
	else
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req]non-chaining handling");
		memcpy( cmdDepRsp + bufferIdx, DepSendDataBuffer + DepSendDataBufferIdx, remainBufferSize);
		a3P2pTargetDepReqInput.Protocol = 0x00;
		a3P2pTargetDepReqInput.OptionFlag = 0x02;
		a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
		a3P2pTargetDepReqInput.DepData = cmdDepRsp;
		a3P2pTargetDepReqInput.DepDataLen = bufferIdx + remainBufferSize;

		DepSendCurrentBatchSize = remainBufferSize;
	}
    cmdDepRsp[1] = a3P2pTargetDepReqInput.DepDataLen - 1;

	interfaceSendRet = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
	pDev->cbRecv = ( RecvCallBack*)ms_open_nfc_p2p_target_t2i_dep_req_callback;
	retVal = interfaceSendRet;


	PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req() - end ");

	return retVal;
}


// callback functions
int ms_open_nfc_p2p_target_detection_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - start ");

	int retVal = 0;

	int serviceLen = 0;
	unsigned char nfcCmdCat;
	unsigned char nfcCmdAtrCmd;
	unsigned char genData[ATR_GEN_DATA_BUFFER_SIZE];
	int genDataLen = 0;
	int idxForLoop;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]P2pTargetDetctionCount: %d", P2pTargetDetctionCount);
		if( P2pTargetDetctionCount < P2P_TARGET_DETECT_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]well to go to SDD again");
			P2pTargetDetctionCount++;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);
			PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection( pDev);
		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]P2pTargetDetctionCount: %d", P2pTargetDetctionCount);
		if( P2pTargetDetctionCount < P2P_TARGET_DETECT_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]well to go to SDD again");
			P2pTargetDetctionCount++;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);
			PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection( pDev);
		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS| Data| CRC16
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]Internal contral: A3 reset.");
			PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
			P2pTargetDetctionCount = 0;
			ms_card_detection(pDev);
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]Internal contral: unknown STS code.");
		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection(pDev);
		return 0;
	}

	// A3: 0x02 | Len| STS | Data | CRC16
	// Data: 0xF0| LEN| 0xD4| 0x00| NFCID3| DID| BS| BR| PP| Gen Data

	PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
			case 0x16:
				PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]RF field off.");
				PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
				PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
				P2pTargetDetctionCount = 0;
				ms_card_detection(pDev);
				return 0;

			case 0x1A:  //JIMMY ADD
				PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]Do Card Emulation");
				PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
				P2pTargetDetctionCount = 0;
				ms_tag_emulation(pDev);
				return 0;

			default:
				PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]Other error return.");
				PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
				PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
				P2pTargetDetctionCount = 0;
				ms_card_detection(pDev);
				return 0;
		}
	}


	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]No Data or not long enough for CRC16 calculation");

		if( RcvLen == 4 && pRcvBuf[3] == 0x26)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]pRcvBuf[3]: 0x26, under some SDD loop");

			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]P2pTargetDetctionCount: %d", P2pTargetDetctionCount);
			if( P2pTargetDetctionCount < P2P_TARGET_DETECT_COUNT_MAX)
			{
				PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]well to go to SDD again");
				P2pTargetDetctionCount++;
				ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);

				PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
				return 0;
			}
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_18092_106, pRcvBuf + 3, pRcvBuf[1] - 1) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback(): CRC error");

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]P2pTargetDetctionCount: %d", P2pTargetDetctionCount);
		if( P2pTargetDetctionCount < P2P_TARGET_DETECT_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]well to go to SDD again");
			P2pTargetDetctionCount++;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);

			PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		return 0;
	}

	if( RcvLen < 22)
	{
		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback(): Length < 22. Wring ATR command.");

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		return 0;

	}

	// parsing ATR Req
	nfcCmdCat = pRcvBuf[5];
	nfcCmdAtrCmd = pRcvBuf[6];
	//memcpy( NFCID3i, pRcvBuf + 6, 10);
	memcpy( NFCID3i, pRcvBuf + 7, 10);
	DIDi = pRcvBuf[17];
	BSi = pRcvBuf[18];
	BRi = pRcvBuf[19];
	PPi = pRcvBuf[20];
	genDataLen = pRcvBuf[1] - 1 - 18 - 2;
	memcpy( genData, pRcvBuf + 21, genDataLen);
	DIDt = DIDi;

	PNALDebugLog("[ms_open_nfc_p2p_target_detection_callback]NFCID3i: ");
    PNALDebugLogBuffer(NFCID3i, 10);

	PNALDebugLog ("[ms_open_nfc_p2p_target_detection_callback]DIDi: %02x ", DIDi);
	PNALDebugLog ("[ms_open_nfc_p2p_target_detection_callback]BSi: %02x ", BSi);
	PNALDebugLog ("[ms_open_nfc_p2p_target_detection_callback]BRi: %02x ", BRi);
	PNALDebugLog ("[ms_open_nfc_p2p_target_detection_callback]PPi: %02x ", PPi);

	PNALDebugLog ("[ms_open_nfc_p2p_target_detection_callback]gendata lenth: %d ", genDataLen);
	PNALDebugLog ("[ms_open_nfc_p2p_target_detection_callback]gendata: ");
    PNALDebugLogBuffer(genData, genDataLen);


	// check for command code
	if( nfcCmdCat != 0xD4 || nfcCmdAtrCmd != NFC_CMD_ATR_REQ)
	{
		PNALDebugLog("[ms_open_nfc_p2p_target_detection_callback]wrong command: nfcCmdCat != 0xD4 || nfcCmdAtrCmd != NFC_CMD_ATR_REQ");

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]P2pTargetDetctionCount: %d", P2pTargetDetctionCount);
		if( P2pTargetDetctionCount < P2P_TARGET_DETECT_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]well to go to SDD again");
			P2pTargetDetctionCount++;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);

			PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]back to main detection loop");
		P2pTargetDetctionCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");
		return 0;
	}

	// turn off the detection mask
	//ReaderDetectMask = pDev->reader_detect_mask;
	//pDev->reader_detect_mask = 0x0000;
	//CardDetectMask = pDev->card_detect_mask;
	//pDev->card_detect_mask = 0x0000;

	//PNALDebugLog( "ReaderDetectMask: %02x ", ReaderDetectMask);
	//PNALDebugLog( "CardDetectMask: %02x ", CardDetectMask);


	// report event: NAL_EVT_P2P_INITIATOR_DISCOVERED
	pDev->temp_rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
	pDev->temp_rx_buffer[1] = NAL_EVT_P2P_INITIATOR_DISCOVERED;
	pDev->temp_rx_buffer[2] = 0x00;
	pDev->temp_rx_buffer[3] = 0x01;
	pDev->temp_rx_buffer[4] = 0x9E;
	pDev->temp_rx_buffer[5] = 0x10;
	memcpy( pDev->temp_rx_buffer + 6, pRcvBuf + 21, genDataLen);
	serviceLen = 6 + genDataLen;
	//pDev->nb_available_bytes = serviceLen;
	if (SetEventAvailableBytes(pDev, serviceLen))
      {
#if CODE_CONTROL_DETECT_SDDATR_ON != 1
	    // response for ATR_REQ from initiator
	    ms_open_nfc_p2p_target_i2t_atr_rsp( pDev);
	    pDev->cbRecv = (RecvCallBack *)ms_open_nfc_p2p_target_i2t_atr_rsp_callback;
#else
	    ms_a3_p2p_target_ask_req( pDev);
	    pDev->cbRecv = (RecvCallBack *)ms_open_nfc_p2p_target_i2t_atr_rsp_callback;
#endif
       }
       else
       {
           PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]SetEventAvailableBytes is Fail ");
       }

	// Initiator Discover succeed
	// Initial related global parameters
	P2pTargetDetctionCount = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_detection_callback() - end ");

	return retVal;
}

int ms_open_nfc_p2p_target_i2t_atr_rsp_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - start ");

	int idxForLoop;
	unsigned char cmdCode;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);
		if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
			P2pTargetI2tAtrRspCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
		P2pTargetI2tAtrRspCallbackCount = 0;
		//ms_card_detection( pDev);
             CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);
		if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
			P2pTargetI2tAtrRspCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
		P2pTargetI2tAtrRspCallbackCount = 0;
		//ms_card_detection( pDev);
             CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS| Data| CRC16
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]Internal contral: A3 reset.");
			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
			P2pTargetI2tAtrRspCallbackCount = 0;
			//ms_card_detection(pDev);
			CNALWriteDataFinish(pDev);
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]Internal contral: unknown STS code.");
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
		P2pTargetI2tAtrRspCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);
		return 0;
	}


	// A3: 0x02 | Len| STS | Data | CRC16
	// Data: 0XF0| LEN| 0xD4| 0x06| PFB| [DID]| [NAD]| Data

	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
			case 0x16:
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]RF field off.");
				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
				P2pTargetI2tAtrRspCallbackCount = 0;
				//ms_card_detection( pDev);
				CNALWriteDataFinish(pDev);
				return 0;

			default:
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]Other error return.");

				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);
				if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
					P2pTargetI2tAtrRspCallbackCount++;
					retVal = ms_a3_recieve_only( pDev);
					PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
				P2pTargetI2tAtrRspCallbackCount = 0;
				//ms_card_detection( pDev);
                          CNALWriteDataFinish(pDev);
				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
				return 0;
		}
	}


	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]pRcvBuf:");
    PNALDebugLogBuffer(pRcvBuf, RcvLen);


	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]No Data or not long enough for CRC16 calculation");

		if( RcvLen == 4 && pRcvBuf[3] == 0x26)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]pRcvBuf[3]: 0x26, under some SDD loop");

			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]go to SDD again");
			P2pTargetI2tAtrRspCallbackCount = 0;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);
		if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
			P2pTargetI2tAtrRspCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
		P2pTargetI2tAtrRspCallbackCount = 0;
		//ms_card_detection( pDev);
             CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_18092_106, pRcvBuf + 3, pRcvBuf[1] - 1) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback(): CRC error");

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);
		if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
			P2pTargetI2tAtrRspCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
		P2pTargetI2tAtrRspCallbackCount = 0;
		//ms_card_detection( pDev);
             CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
		return 0;
	}

	// parsing command
	cmdCode = pRcvBuf[6];
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]cmdCode: %02x", cmdCode);
	switch(cmdCode)
	{
		case NFC_CMD_DEP_REQ:
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]entering...CMD_1_DEP_REQ");
			ms_open_nfc_p2p_target_dep_init();
			if( ms_open_nfc_p2p_target_i2t_dep_req( pDev, pRcvBuf + 3, RcvLen - 3) < 0)
			{
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);

				if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
					P2pTargetI2tAtrRspCallbackCount++;
					retVal = ms_a3_recieve_only( pDev);

					PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
				P2pTargetI2tAtrRspCallbackCount = 0;
				//ms_card_detection( pDev);

				CNALWriteDataFinish(pDev);

				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
				return 0;
			}
			break;

		case NFC_CMD_ATR_REQ:

			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]entering...CMD_1_ATR_REQ");

			P2pTargetI2tAtrRspCallbackCount = 0;
			//ms_DeviceReset();
			retVal = ms_open_nfc_p2p_target_detection_callback( pDev, pRcvBuf, RcvLen);
			break;

		default:
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]entering...error cmdCode!");

			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]P2pTargetI2tAtrRspCallbackCount: %d", P2pTargetI2tAtrRspCallbackCount);
			if( P2pTargetI2tAtrRspCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
			{
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]well to keep recieve only mode");
				P2pTargetI2tAtrRspCallbackCount++;
				retVal = ms_a3_recieve_only( pDev);

				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
				return 0;
			}

			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_atr_rsp_callback]back to main detection loop");
			P2pTargetI2tAtrRspCallbackCount = 0;
			//ms_card_detection( pDev);
			CNALWriteDataFinish(pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");
			return 0;

			break;
	}


	P2pTargetI2tAtrRspCallbackCount = 0;
	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_atr_rsp_callback() - end ");

	return retVal;

}

int ms_open_nfc_p2p_target_i2t_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - start ");
	int retVal = 0;
	int idxForLoop;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
		if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
			P2pTargetI2tDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
		if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
			P2pTargetI2tDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS| Data| CRC16
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]Internal contral: A3 reset.");
			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
			P2pTargetI2tDepReqCallbackCount = 0;
        		//ms_card_detection(pDev);
        		CNALWriteDataFinish(pDev);
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]Internal contral: unknown STS code.");
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;
    		//ms_card_detection(pDev);
    		CNALWriteDataFinish(pDev);
		return 0;
	}

	// A3: 0x02 | Len| STS | Data | CRC16
	// Data: 0xF0| LEN|0xD4| 0x06| PFB| [DID]| [NAD]| Data

	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
			case 0x16:
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]RF field off.");
				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
				P2pTargetI2tDepReqCallbackCount = 0;
                		//ms_card_detection(pDev);
                		CNALWriteDataFinish(pDev);
				return 0;

			default:
				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]Other error return.");

				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
				if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_ATR_RSP_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
					P2pTargetI2tDepReqCallbackCount++;
					retVal = ms_a3_recieve_only( pDev);
					PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
				P2pTargetI2tDepReqCallbackCount = 0;
                		//ms_card_detection(pDev);
                		CNALWriteDataFinish(pDev);

				PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
				return 0;
		}
	}


	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]pRcvBuf:");
    PNALDebugLogBuffer( pRcvBuf, RcvLen);

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]No Data or not long enough for CRC16 calculation");

		if( RcvLen == 4 && pRcvBuf[3] == 0x26)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]pRcvBuf[3]: 0x26, under some SDD loop");

			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]go to SDD again");
			P2pTargetI2tDepReqCallbackCount = 0;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
		if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
			P2pTargetI2tDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
            P2pTargetI2tDepReqCallbackCount = 0;
    		//ms_card_detection(pDev);
    		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_18092_106, pRcvBuf + 3, pRcvBuf[1] - 1) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback(): CRC error");

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
		if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
			P2pTargetI2tDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;
    		//ms_card_detection(pDev);
    		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	ms_open_nfc_p2p_target_i2t_dep_req( pDev, pRcvBuf + 3, RcvLen - 3);

	PNALDebugLog( "ms_open_nfc_p2p_target_i2t_dep_req_callback() - end ");
	return retVal;

}

int ms_open_nfc_p2p_target_t2i_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - start ");

	int idxForLoop;
	unsigned char cmdCat;
	unsigned char cmdCode;
	unsigned char iPfb;
	unsigned char iDid = 0x00;
	unsigned char iNad = 0x00;
	unsigned char iPni;
	unsigned char iAtn;
	unsigned char cmdRes[255];
	int parsingIdx;
	unsigned char genData[255];
	int genDataLen;
	_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS| Data| CRC16
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Internal contral: A3 reset.");
			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
			P2pTargetT2iDepReqCallbackCount = 0;
			//ms_card_detection(pDev);
			CNALWriteDataFinish(pDev);
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Internal contral: unknown STS code.");
		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);
		return 0;
	}

	// A3: 0x02 | Len| STS | Data | CRC16
	// Data: 0xF0| LEN| 0xD4| 0x06| PFB| [DID]| [NAD]| Data

	PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
			case 0x16:
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]RF field off.");
				PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
				P2pTargetT2iDepReqCallbackCount = 0;
				//ms_card_detection( pDev);
				CNALWriteDataFinish(pDev);
				return 0;

			default:
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Other error return.");

				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
				if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
					P2pTargetT2iDepReqCallbackCount++;
					retVal = ms_a3_recieve_only( pDev);
					PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
				P2pTargetT2iDepReqCallbackCount = 0;
				//ms_card_detection( pDev);
				CNALWriteDataFinish(pDev);

				PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
		}
	}


	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]pRcvBuf:");
    PNALDebugLogBuffer(pRcvBuf, RcvLen);

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]No Data or not long enough for CRC16 calculation");

		if( RcvLen == 4 && pRcvBuf[3] == 0x26)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]pRcvBuf[3]: 0x26, under some SDD loop");

			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]go to SDD again");
			P2pTargetT2iDepReqCallbackCount = 0;
			ms_open_nfc_p2p_target_detection( pDev, DetectionRfParam);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection( pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_18092_106, pRcvBuf + 3, pRcvBuf[1] - 1) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback(): CRC error");

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection( pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	// Dep Data: Parsing
	cmdCat = pRcvBuf[5];
	cmdCode = pRcvBuf[6];
	if( ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ)
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]wrong command code: ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ");

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection( pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	iPfb = pRcvBuf[7];
	PNALDebugLog( " iPfb: %02x ", iPfb);
	parsingIdx = 8;
	if( ( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID)
	{
		iDid = pRcvBuf[ parsingIdx++];
	}

	if( ( iPfb & PFB_INF_NAD_MASK) == PFB_INF_NAD)
	{
		iNad = pRcvBuf[ parsingIdx++];
	}

	// Dep Data: check
	// DID check
	if( (( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID))
	{
		if( DIDt == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Wrong DID Flag in PFB, DIDt: %02x", DIDt);
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		if( iDid != DIDt)
		{
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Wrong DID, DIDt: %02x, iDid: %02x", DIDt, iDid);
			retVal = ms_a3_recieve_only( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

	}

	// A3: 0x02 | Len| STS | Data | CRC16
	// Data: 0XF0| LEN| 0xD4| 0x06| PFB| [DID]| [NAD]| Data
	genDataLen = RcvLen - parsingIdx - 2;
	if( genDataLen > 0)
	{
		memcpy( genData, pRcvBuf + parsingIdx, genDataLen);
	}

	switch( iPfb & PFB_INF_MASK)
	{
		case PFB_INF:
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]PFB: PFB_INF ");
			ms_open_nfc_p2p_target_dep_init();
			ms_open_nfc_p2p_target_i2t_dep_req( pDev, pRcvBuf + 3, RcvLen - 3);
			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
			break;

		case PFB_ACK:
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]PFB: PFB_ACK ");

			iPni = iPfb & 0x03;
			if( iPni != DepSendCurrentPni)
			{
				// wrong pni
				// resend
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]wrong pni");
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]resend");
				ms_open_nfc_p2p_target_t2i_dep_req( pDev);
				PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
			}

			if( ( iPfb & PFB_ACK_NACK_MASK) == PFB_ACK_NACK)
			{
				//NACK
				// resend
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]initiator NACK");
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]resend");
				ms_open_nfc_p2p_target_t2i_dep_req( pDev);
				PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
			}

			DepSendDataBufferIdx += DepSendCurrentBatchSize;
			DepSendCurrentPni = ( DepSendCurrentPni + 1)% 4;
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]DepSendDataBufferIdx: %d", DepSendDataBufferIdx);
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]DepSendCurrentPni: %02x", DepSendCurrentPni);
			retVal = ms_open_nfc_p2p_target_t2i_dep_req( pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
			break;

		case PFB_SUP:
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]PFB: PFB_SUP ");

			iAtn = iPfb & PFB_SUP_ATN_MASK;
			if( iAtn == PFB_SUP_TO)
			{
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]Timeout not support ");
			}
			cmdRes[0] = P2P_PROTOCAL_SB;
			//cmdRes[1] = 0;
			cmdRes[2] = NFC_DEP_TARGET_CODE;
			cmdRes[3] = NFC_CMD_DEP_RSP;
			cmdRes[4] = iPfb;
			parsingIdx = 5;
			if( ( iPfb & PFB_SUP_DID_MASK) == PFB_SUP_DID)
			{
				//cmdRes[2] |= PFB_SUP_DID;
				cmdRes[parsingIdx++] = iDid;
			}

			if( ( iPfb & PFB_SUP_NAD_MASK) == PFB_SUP_NAD)
			{
				//cmdRes[2] |= PFB_SUP_NAD;
				cmdRes[parsingIdx++] = iNad;
			}
			if( genDataLen > 0)
			{
				memcpy( cmdRes + parsingIdx, genData, genDataLen);
				parsingIdx = parsingIdx + genDataLen;
			}
			//cmdRes[1] = parsingIdx - 2;
			cmdRes[1] = parsingIdx - 1;

			a3P2pTargetDepReqInput.Protocol = 0x00;
			a3P2pTargetDepReqInput.OptionFlag = 0x02;
			a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
			a3P2pTargetDepReqInput.DepData = cmdRes;
			a3P2pTargetDepReqInput.DepDataLen = parsingIdx;
                   ms_open_nfc_p2p_target_dep_init();
			retVal = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
			pDev->cbRecv = ( RecvCallBack*)ms_open_nfc_p2p_target_i2t_dep_req_callback;
			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return retVal;
			break;

		default:
			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]PFB: others ");

			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
			if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
			{
				PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
				P2pTargetT2iDepReqCallbackCount++;
				retVal = ms_a3_recieve_only( pDev);

				PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
			}

			PNALDebugLog( "[ms_open_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
			P2pTargetT2iDepReqCallbackCount = 0;
			//ms_card_detection( pDev);
			CNALWriteDataFinish(pDev);

			PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
			break;
	}

	PNALDebugLog( "ms_open_nfc_p2p_target_t2i_dep_req_callback() - end ");

	return retVal;
}


// utilities for A3
static int ms_a3_p2p_target_sdd_req( tNALMsrComInstance  *pDev, _A3P2pTargetSddReqInput *A3P2pTargetSddReqInput)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_p2p_target_sdd_req() - start ");

	//Format: | Cmd(0x42)| Len| Option ID1(0x81)| Option ID2(0x01)| Protocol| Option Flag| Timeout_3| Timeout_2| Timeout_1| Timeout_0
	unsigned char cmdSddReq[] = { 0x42, 0x08, 0x81, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	int cmdSddReqLen;
	int a3IntSendRet;
	unsigned int rfidTimeout;

	rfidTimeout = A3P2pTargetSddReqInput->Timeout * A3_CLOCK_RATE;
	cmdSddReq[4] = A3P2pTargetSddReqInput->Protocol;
	cmdSddReq[5] = A3P2pTargetSddReqInput->OptionFlag;
	cmdSddReq[6] = ( rfidTimeout >> 24) & 0xFF;
	cmdSddReq[7] = ( rfidTimeout >> 16) & 0xFF;
	cmdSddReq[8] = ( rfidTimeout >>  8) & 0xFF;
	cmdSddReq[9] = ( rfidTimeout      ) & 0xFF;
	cmdSddReqLen = 10;

	memcpy( pDev->ant_send_buffer, cmdSddReq, cmdSddReqLen);
	a3IntSendRet = ms_interfaceSend( pDev, cmdSddReqLen);

	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_p2p_target_sdd_req() - end ");
	return retVal;
}

static int ms_a3_p2p_target_dep_req( tNALMsrComInstance  *pDev, _A3P2pTargetDepReqInput *A3P2pTargetDepReqInput)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_p2p_target_dep_req() - start ");

	//Format: | Cmd(0x42)| Len| Option ID1(0x81)| Option ID2(0x00)| Protocol| Option Flag| Timeout_3| Timeout_2| Timeout_1| Timeout_0|Data
	unsigned char cmdDepReq[255];
	int cmdDepReqLen;
	int a3IntSendRet;
	unsigned int rfidTimeout;

	int idx;

	rfidTimeout = A3P2pTargetDepReqInput->Timeout * A3_CLOCK_RATE;
	cmdDepReq[0] = 0x42;
	cmdDepReq[1] = 0x00;
	cmdDepReq[2] = 0x81;
	cmdDepReq[3] = 0x00;
	cmdDepReq[4] = A3P2pTargetDepReqInput->Protocol;
	cmdDepReq[5] = A3P2pTargetDepReqInput->OptionFlag;
	cmdDepReq[6] = ( rfidTimeout >> 24) & 0xFF;
	cmdDepReq[7] = ( rfidTimeout >> 16) & 0xFF;
	cmdDepReq[8] = ( rfidTimeout >>  8) & 0xFF;
	cmdDepReq[9] = ( rfidTimeout      ) & 0xFF;

	PNALDebugLog( "ms_a3_p2p_target_dep_req() - 1 ");
	if( A3P2pTargetDepReqInput->DepDataLen > 0)
	{
		memcpy( cmdDepReq + 10, A3P2pTargetDepReqInput->DepData, A3P2pTargetDepReqInput->DepDataLen);
	}
	cmdDepReqLen = 10 + A3P2pTargetDepReqInput->DepDataLen;
	cmdDepReq[1] = 8 + A3P2pTargetDepReqInput->DepDataLen;

	PNALDebugLog( "ms_a3_p2p_target_dep_req() - 2 ");
	PNALDebugLog( "A3P2pTargetDepReqInput->DepDataLen: %d", A3P2pTargetDepReqInput->DepDataLen);
	PNALDebugLog( "cmdDepReqLen: %d", cmdDepReqLen);
	PNALDebugLog( "cmdDepReq[1]: %02x", cmdDepReq[1]);
	PNALDebugLog( "cmdDepReq: ");
      PNALDebugLogBuffer(cmdDepReq, cmdDepReqLen);
      /*
	for( idx = 0; idx < cmdDepReqLen; idx++)
	{
		PNALDebugLog( "%02x ", cmdDepReq[idx]);
	}
      */
	memcpy( pDev->ant_send_buffer, cmdDepReq, cmdDepReqLen);
	PNALDebugLog( "ms_a3_p2p_target_dep_req() - 3 ");
	a3IntSendRet = ms_interfaceSend( pDev, cmdDepReqLen);

	PNALDebugLog( "ms_a3_p2p_target_dep_req() - 4 ");
	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_p2p_target_dep_req() - end ");

	return retVal;
}

static int ms_a3_recieve_only( tNALMsrComInstance  *pDev)
{
	PNALDebugLog( "ms_a3_recieve_only() - start ");

	int retVal = 0;
	int interfaceSendRet;
	_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;

	a3P2pTargetDepReqInput.Protocol = 0x00;
	a3P2pTargetDepReqInput.OptionFlag = 0x12;
	a3P2pTargetDepReqInput.Timeout = P2P_TARGET_DEP_RCV_TIMEOUT;
	//a3P2pTargetDepReqInput.DepData = 0x00;
	a3P2pTargetDepReqInput.DepDataLen = 0;

	interfaceSendRet = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
	retVal = interfaceSendRet;

	PNALDebugLog( "ms_a3_recieve_only() - end ");
	return retVal;
}

static int ms_a3_p2p_target_sddatr_req( tNALMsrComInstance  *pDev, _A3P2pTargetSddAtrReqInput *A3P2pTargetSddAtrReqInput)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_p2p_target_sddatr_req() - start ");

	//Format: | Cmd(0x42)| Len| Option ID1(0x81)| Option ID2(0x01)| Protocol| Option Flag| Timeout_3| Timeout_2| Timeout_1| Timeout_0
	unsigned char cmdA3Req[255];
	int cmdA3ReqLen;
	int a3IntSendRet;
	unsigned int sddTimeoutClock;
	unsigned int depTimeoutClock;

	sddTimeoutClock = A3P2pTargetSddAtrReqInput->SddTimeout * A3_CLOCK_RATE;
	depTimeoutClock = A3P2pTargetSddAtrReqInput->DepTimeout * A3_CLOCK_RATE;
#if 1	
 	cmdA3Req[0] = 0x42;
	cmdA3Req[1] = 11 + A3P2pTargetSddAtrReqInput->AtrContenLen;
	cmdA3Req[2] = 0x81;
	cmdA3Req[3] = 0x06;
	cmdA3Req[4] = A3P2pTargetSddAtrReqInput->Protocal;
	cmdA3Req[5] = ( sddTimeoutClock >> 24) & 0xFF;
	cmdA3Req[6] = ( sddTimeoutClock >> 16) & 0xFF;
	cmdA3Req[7] = ( sddTimeoutClock >>  8) & 0xFF;
	cmdA3Req[8] = ( sddTimeoutClock      ) & 0xFF;
	cmdA3Req[9] = ( depTimeoutClock >> 24) & 0xFF;
	cmdA3Req[10] = ( depTimeoutClock >> 16) & 0xFF;
	cmdA3Req[11] = ( depTimeoutClock >>  8) & 0xFF;
	cmdA3Req[12] = ( depTimeoutClock      ) & 0xFF;
	if( A3P2pTargetSddAtrReqInput->AtrContenLen > 0)
	{
		memcpy( cmdA3Req + 13, A3P2pTargetSddAtrReqInput->AtrContent, A3P2pTargetSddAtrReqInput->AtrContenLen);
	}
	cmdA3ReqLen = 13 + A3P2pTargetSddAtrReqInput->AtrContenLen;

	memcpy( pDev->ant_send_buffer, cmdA3Req, cmdA3ReqLen);
#else 
    //only for 43b card mode test: added by jimmy, 2012/10/19
	cmdA3Req[0] = 0x42;
	cmdA3Req[1] = 6;
	cmdA3Req[2] = 0x81;
	cmdA3Req[3] = 0x08;
	cmdA3Req[4] = ( sddTimeoutClock >> 24) & 0xFF;
	cmdA3Req[5] = ( sddTimeoutClock >> 16) & 0xFF;
	cmdA3Req[6] = ( sddTimeoutClock >>	8) & 0xFF;
	cmdA3Req[7] = ( sddTimeoutClock 	 ) & 0xFF;
	cmdA3ReqLen = 8;
	memcpy( pDev->ant_send_buffer, cmdA3Req, cmdA3ReqLen);
#endif
	a3IntSendRet = ms_interfaceSend( pDev, cmdA3ReqLen);

	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_p2p_target_sddatr_req() - end ");
	return retVal;
}

static int ms_a3_p2p_target_ask_req( tNALMsrComInstance  *pDev)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_p2p_target_ask_req() - start ");

	//Format: | Cmd(0x42)| Len| Option ID1(0x81)| Option ID2(0x01)| Protocol| Option Flag| Timeout_3| Timeout_2| Timeout_1| Timeout_0
	unsigned char cmdA3Req[] = { 0x42, 0x02, 0x81, 0x07};
	int cmdA3ReqLen;
	int a3IntSendRet;

	cmdA3ReqLen = 4;

	memcpy( pDev->ant_send_buffer, cmdA3Req, cmdA3ReqLen);
	a3IntSendRet = ms_interfaceSend( pDev, cmdA3ReqLen);

	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_p2p_target_ask_req() - end ");
	return retVal;
}


// utilities for local
static unsigned int ms_get_detection_timeout()
{
	int randomIdx = 0;

	randomIdx = rand() % DETECTION_TIMEOUT_LIST_CNT;

	return DetectionTimeoutList[randomIdx];
}

static unsigned int ms_get_detection_polling_cnt()
{
	int randomIdx = 0;

	randomIdx = rand() % DETECTION_TIMEOUT_LIST_CNT;

	return DetectionPollingCntList[randomIdx];
}

//---- 18092: 212/424 ---//

static int ms_nfcctrl_18092_enablep2p_req( tNALMsrComInstance *pDev, _MsNfcctrl18092Enablep2pReqInput *MsNfcctrl18092Enablep2pReqInput)
{
	int retVal = 0;

	PNALDebugLog( "ms_nfcctrl_18092_enablep2p_req() - start ");
    pDev->sendTargetSddEvt = W_FALSE;
	pDev->temp_target_buf_len = 0;
	//unsigned char cmdNfcctrlReq[255];
	int cmdNfcctrlReqLen = 0;
	int nfcctrlIntSendRet = 0;
	int bufferIdx = 0;
	unsigned char paramCount = 0x03;

    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    pDev->ant_send_buffer[0] = 0x4A;
	//pDev->ant_send_buffer[1] = 0;    //LEN

	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  K , Param_n1(n3)
	pDev->ant_send_buffer[2] = DrvP2P_FuncCode_EnaleP2P;
	pDev->ant_send_buffer[3] = paramCount;

	//ParaID(1)[0x00],ParamLen(1)[3], Speed(1),Mode(1),PollingTimes(1)
	pDev->ant_send_buffer[4] = DrvP2P_ModeParam_Mode;
	pDev->ant_send_buffer[5] = 0x03;
	pDev->ant_send_buffer[6] = MsNfcctrl18092Enablep2pReqInput->P2pSpeedType;
	pDev->ant_send_buffer[7] = MsNfcctrl18092Enablep2pReqInput->P2pRole;
	pDev->ant_send_buffer[8] = MsNfcctrl18092Enablep2pReqInput->PollingCount;

	//DrvP2P_ModeParam_Polling_RES= 2,
	//ParaID(1)[0x02],ParamLen(1)[9], NFCID_Type (1),ID(8)
	pDev->ant_send_buffer[9] = DrvP2P_ModeParam_Polling_RES;
	switch( MsNfcctrl18092Enablep2pReqInput->NfcId2GenType)
	{
		case DrvP2P_NFCID_HostSpec:
			pDev->ant_send_buffer[10] = 0x09;
			pDev->ant_send_buffer[11] = MsNfcctrl18092Enablep2pReqInput->NfcId2GenType;
			memcpy( &pDev->ant_send_buffer[12], MsNfcctrl18092Enablep2pReqInput->NfcId2, NFCID2_LEN);
			bufferIdx = 20; // 12 + 8

			break;

		case DrvP2P_NFCID_FwGenerate:
		default:
			pDev->ant_send_buffer[10] = 0x01;
			pDev->ant_send_buffer[11] = MsNfcctrl18092Enablep2pReqInput->NfcId2GenType;
			bufferIdx = 12;

			break;
	}

	//DrvP2P_ModeParam_ATR_RES= 4,
    //ParaID(1)[0x04],Len[n+7],GeneralByteNum(1)[n],IDType(1)[1],Payload(n+5)
	pDev->ant_send_buffer[ bufferIdx++] = DrvP2P_ModeParam_ATR_RES;
	switch( MsNfcctrl18092Enablep2pReqInput->NfcId3GenType)
	{
		case DrvP2P_NFCID_HostSpec:
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen + 17;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->NfcId3GenType;
			memcpy( &pDev->ant_send_buffer[ bufferIdx], MsNfcctrl18092Enablep2pReqInput->NfcId3, NFCID3_LEN);
			bufferIdx += 10;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->DID;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BS;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BR;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->TO;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->PP;
			memcpy( &pDev->ant_send_buffer[ bufferIdx], MsNfcctrl18092Enablep2pReqInput->LinkActMsg, MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen);
			bufferIdx += MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;

			break;

		case DrvP2P_NFCID_FwGenerate:
		default:
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen + 7;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->NfcId3GenType;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->DID;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BS;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BR;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->TO;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->PP;
			memcpy( &pDev->ant_send_buffer[ bufferIdx], MsNfcctrl18092Enablep2pReqInput->LinkActMsg, MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen);
			bufferIdx += MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;

			break;
	}

	cmdNfcctrlReqLen = bufferIdx;
	pDev->ant_send_buffer[1] = cmdNfcctrlReqLen - 2;
	nfcctrlIntSendRet= ms_interfaceSend( pDev, cmdNfcctrlReqLen);
	retVal = nfcctrlIntSendRet;

	PNALDebugLog( "ms_nfcctrl_18092_enablep2p_req() - end ");
	return retVal;
}



static int ms_nfcctrl_18092_dep_req( tNALMsrComInstance *pDev, _MsNfcctrl18092DepReqInput *MsNfcctrl18092DepReqInput)
{
	int retVal = 0;

	PNALDebugLog( "ms_nfcctrl_18092_dep_req() - start ");

	int cmdNfcctrlReqLen = 0;
	int nfcctrlIntSendRet = 0;
	//int bufferIdx = 0;
	unsigned char paramCount = 2;

    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    pDev->ant_send_buffer[0] = 0x4A;
	pDev->ant_send_buffer[1] = 12 + MsNfcctrl18092DepReqInput->DepContentLen - 2;

	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  K , Param_n1(n3)
	pDev->ant_send_buffer[2] = DrvP2P_FuncCode_DEP;
	pDev->ant_send_buffer[3] = paramCount;

	//Param: CtrlInfo.
	//0x05, Len, WT, RTOX
	pDev->ant_send_buffer[4] = DrvP2P_ModeParam_CtrlInfo;
	pDev->ant_send_buffer[5] = 0x02;
	if( MsNfcctrl18092DepReqInput->CtrlInfoFlag)
	{
		pDev->ant_send_buffer[6] = MsNfcctrl18092DepReqInput->WT;
		pDev->ant_send_buffer[7] = MsNfcctrl18092DepReqInput->RTOX;
	}
	else
	{
		//default
		pDev->ant_send_buffer[6] = 0x0D;  //jim test
		pDev->ant_send_buffer[7] = 0x01;
	}

	//Param: Rawdata
	pDev->ant_send_buffer[8] = DrvP2P_ModeParam_RawData;
	pDev->ant_send_buffer[9] = 0x02 + MsNfcctrl18092DepReqInput->DepContentLen;
	pDev->ant_send_buffer[10] = MsNfcctrl18092DepReqInput->DepFlag;
	pDev->ant_send_buffer[11] = MsNfcctrl18092DepReqInput->DepContentLen;
	if( MsNfcctrl18092DepReqInput->DepContentLen > 0)
	{
		memcpy( &pDev->ant_send_buffer[12], MsNfcctrl18092DepReqInput->DepContent, MsNfcctrl18092DepReqInput->DepContentLen);
	}

	cmdNfcctrlReqLen = 12 + MsNfcctrl18092DepReqInput->DepContentLen;
	nfcctrlIntSendRet= ms_interfaceSend( pDev, cmdNfcctrlReqLen);
	retVal = nfcctrlIntSendRet;

	PNALDebugLog( "ms_nfcctrl_18092_dep_req() - end ");
	return retVal;

}


static int ms_nfcctrl_18092_dep_receive_only_req( tNALMsrComInstance *pDev)
{
	int retVal = 0;

	_MsNfcctrl18092DepReqInput msNfcctrl18092DepReqInput;

	msNfcctrl18092DepReqInput.CtrlInfoFlag = W_FALSE;
	msNfcctrl18092DepReqInput.WT = 0;
	msNfcctrl18092DepReqInput.RTOX = 0;
	msNfcctrl18092DepReqInput.DepFlag = DrvP2P_DepFlag_WaitRx;
	msNfcctrl18092DepReqInput.DepContentLen = 0;
	msNfcctrl18092DepReqInput.DepContent = null;

	retVal = ms_nfcctrl_18092_dep_req( pDev, &msNfcctrl18092DepReqInput);

	return retVal;
}




int ms_nfc_p2p_target_enable( tNALMsrComInstance  *pDev, unsigned char SddRfParam)
{
	int retVal = 0;
      DepSendCurrentPni = 0; //Init Current PNI
	_MsNfcctrl18092Enablep2pReqInput msNfcctrl18092Enablep2pReqInput;

	msNfcctrl18092Enablep2pReqInput.P2pSpeedType = g_P2pSpeedType;
	msNfcctrl18092Enablep2pReqInput.P2pRole = DRVP2P_Target;

       if (SddRfParam & SDD_EXTERN_RF_DETECT)
      {
	    msNfcctrl18092Enablep2pReqInput.PollingCount = P2P_424_TARGET_DEP_EXTEND_POLLING_CNT;
      }
      else
      {
          msNfcctrl18092Enablep2pReqInput.PollingCount = ms_get_detection_polling_cnt();
      }

	//msNfcctrl18092Enablep2pReqInput.PollingCount = 20;
	msNfcctrl18092Enablep2pReqInput.NfcId2GenType = DrvP2P_NFCID_HostSpec;
	msNfcctrl18092Enablep2pReqInput.NfcId2 = NFCID2t;
	msNfcctrl18092Enablep2pReqInput.NfcId3GenType = DrvP2P_NFCID_HostSpec;
	msNfcctrl18092Enablep2pReqInput.NfcId3 = NFCID3t;
	msNfcctrl18092Enablep2pReqInput.DID = DIDt;
	msNfcctrl18092Enablep2pReqInput.BS = BSt;
	msNfcctrl18092Enablep2pReqInput.BR = BRt;
	msNfcctrl18092Enablep2pReqInput.TO = TO;
	msNfcctrl18092Enablep2pReqInput.PP = PPt;
	msNfcctrl18092Enablep2pReqInput.LinkActMsgLen = P2pTargetCardConfig.LinkActMsgLen;
	msNfcctrl18092Enablep2pReqInput.LinkActMsg = P2pTargetCardConfig.LinkActMsg;
#if (A3_PL_MODE == 0)  
	retVal = ms_nfcctrl_18092_enablep2p_req( pDev, &msNfcctrl18092Enablep2pReqInput);
	pDev->cbRecv = (RecvCallBack *)ms_nfc_p2p_target_enable_callback;
#else
    g_P2pSpeedType = DRVP2P_sp424;
	msNfcctrl18092Enablep2pReqInput.P2pSpeedType = g_P2pSpeedType;
    retVal = ms_target_pl_config_req( pDev, &msNfcctrl18092Enablep2pReqInput);    
#endif
	return retVal;
}




int ms_nfc_p2p_target_enable_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - start ");

	int retVal = 0;

	int serviceLen = 0;
	unsigned char nfcCmdCat;
	unsigned char nfcCmdAtrCmd;
	unsigned char genData[ATR_GEN_DATA_BUFFER_SIZE];
	int genDataLen = 0;
	int idxForLoop;
	unsigned char fuseIdLen = 0;
	unsigned char fuseIdBuf[8];

    DepSendCurrentPni = 0; //Init Current PNI
	if( pRcvBuf[3] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[3])
		{
			default:
                         if (pRcvBuf[4] == DrvP2P_SDD_T_SDD_Done)
                         {
                             PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]SDD Done, but not Recv ATR, well to go to SDD again");
			          P2pTargetEnableCount++;
			          ms_nfc_p2p_target_enable( pDev, 0x00);
                         }
                         else
                         {
				    PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]Other error return.");
				    PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
				    PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]back to main detection loop");
				    P2pTargetEnableCount = 0;
				    ms_card_detection(pDev);
                         }
				return 0;
		}
	}

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{

		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]No Data or not long enough for CRC16 calculation");
		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]back to main detection loop");
		P2pTargetEnableCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
		return 0;
	}

	if( ms_crc16_chk( CRC_ISO_18092_248, pRcvBuf + 6, pRcvBuf[5]) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_nfc_p2p_target_enable_callback(): CRC error");

		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]P2pTargetDetctionCount: %d", P2pTargetEnableCount);
		if( P2pTargetEnableCount < P2P_TARGET_ENABLE_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]well to go to SDD again");
			P2pTargetEnableCount++;
			ms_nfc_p2p_target_enable( pDev, 0x00);

			PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]back to main detection loop");
		P2pTargetEnableCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
		return 0;
	}

	// parsing ATR Req
	nfcCmdCat = pRcvBuf[7];
	nfcCmdAtrCmd = pRcvBuf[8];
	memcpy( NFCID3i, pRcvBuf + 9, 10);
	DIDi = pRcvBuf[19];
	BSi = pRcvBuf[20];
	BRi = pRcvBuf[21];
	PPi = pRcvBuf[22];
	genDataLen = pRcvBuf[6] - 1 - 16; // 1: LenField, 16:header
	memcpy( genData, pRcvBuf + 23, genDataLen);

	PNALDebugLog("[ms_nfc_p2p_target_enable_callback]NFCID3i: ");
    PNALDebugLogBuffer( NFCID3i, 10);

	PNALDebugLog ("[ms_nfc_p2p_target_enable_callback]DIDi: %02x ", DIDi);
	PNALDebugLog ("[ms_nfc_p2p_target_enable_callback]BSi: %02x ", BSi);
	PNALDebugLog ("[ms_nfc_p2p_target_enable_callback]BRi: %02x ", BRi);
	PNALDebugLog ("[ms_nfc_p2p_target_enable_callback]PPi: %02x ", PPi);

	PNALDebugLog ("[ms_nfc_p2p_target_enable_callback]gendata lenth: %d ", genDataLen);
	PNALDebugLog ("[ms_nfc_p2p_target_enable_callback]gendata: ");
    PNALDebugLogBuffer(genData, genDataLen);

	// check for command code
	if( nfcCmdCat != 0xD4 || nfcCmdAtrCmd != NFC_CMD_ATR_REQ)
	{
		PNALDebugLog("[ms_nfc_p2p_target_enable_callback]wrong command: nfcCmdCat != 0xD4 || nfcCmdAtrCmd != NFC_CMD_ATR_REQ");

		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]P2pTargetDetctionCount: %d", P2pTargetEnableCount);
		if( P2pTargetEnableCount < P2P_TARGET_ENABLE_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]well to go to SDD again");
			P2pTargetEnableCount++;
			ms_nfc_p2p_target_enable( pDev, 0x00);

			PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_enable_callback]back to main detection loop");
		P2pTargetEnableCount = 0;
		ms_card_detection(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
		return 0;
	}

	// report event: NAL_EVT_P2P_INITIATOR_DISCOVERED
	pDev->temp_rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
	pDev->temp_rx_buffer[1] = NAL_EVT_P2P_INITIATOR_DISCOVERED;
	pDev->temp_rx_buffer[2] = 0x00;
	pDev->temp_rx_buffer[3] = 0x01;
	pDev->temp_rx_buffer[4] = 0x9E;
	pDev->temp_rx_buffer[5] = 0x10;
   
	if (ms_scan_gerenal_byte(genDataLen, genData)== W_FALSE)
	{
        fuseIdLen = 0;
		ms_write_efuse_id(0, fuseIdBuf);
	}
	else
	{
	    fuseIdLen = 11;
		memcpy( fuseIdBuf, &genData[genDataLen - 8], 8);
		ms_write_efuse_id(8, fuseIdBuf);
	}
	memcpy( pDev->temp_rx_buffer + 6, genData, genDataLen - fuseIdLen);
	serviceLen = 6 + genDataLen - fuseIdLen;

	if (SetEventAvailableBytes(pDev, serviceLen))
    {
	    //ms_a3_p2p_target_dep_req(pDev, null);
	    ms_nfcctrl_18092_dep_receive_only_req( pDev);
		pDev->cbRecv = (RecvCallBack *)ms_nfc_p2p_target_enable_post_callback;
    }
	else
	{
		PNALDebugLog( "[ms_open_nfc_p2p_target_detection_callback]SetEventAvailableBytes is Fail ");
	}

	P2pTargetEnableCount = 0;
	PNALDebugLog( "ms_nfc_p2p_target_enable_callback() - end ");
	return retVal;
}

int ms_nfc_p2p_target_enable_post_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - start");

	int retVal = 0;

	PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]STS: %02x.", pRcvBuf[3]);
	if( pRcvBuf[3] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[3])
		{
			case 0x1B:
				PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]RF field off.");
				PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
				P2pTargetEnablePostCallbackCount = 0;
                		CNALWriteDataFinish(pDev);
				return 0;
			default:
				PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]Other error return.");

				PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]P2pTargetEnablePostCallbackCount: %d", P2pTargetEnablePostCallbackCount);
				if( P2pTargetEnablePostCallbackCount < P2P_TARGET_ENABLE_POST_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]well to keep recieve only mode");
					P2pTargetEnablePostCallbackCount++;
					retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
					PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]back to main detection loop");
				P2pTargetEnablePostCallbackCount = 0;
				//ms_card_detection( pDev);
                CNALWriteDataFinish(pDev);
				PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
				return 0;
		}
	}

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6) //jim test
	{
		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]No Data or not long enough for CRC16 calculation");

		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]P2pTargetEnablePostCallbackCount: %d", P2pTargetEnablePostCallbackCount);
		if( P2pTargetEnablePostCallbackCount < P2P_TARGET_ENABLE_POST_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]well to keep recieve only mode");
			P2pTargetEnablePostCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]back to main detection loop");
		P2pTargetEnablePostCallbackCount = 0;

        CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
		return 0;
	}

	if( ms_crc16_chk( CRC_ISO_18092_248, pRcvBuf + 5, pRcvBuf[4]) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback(): CRC error");

		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]P2pTargetEnablePostCallbackCount: %d", P2pTargetEnablePostCallbackCount);
		if( P2pTargetEnablePostCallbackCount < P2P_TARGET_ENABLE_POST_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]well to keep recieve only mode");
			P2pTargetEnablePostCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]back to main detection loop");
		P2pTargetEnablePostCallbackCount = 0;

        CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
		return 0;
	}

	// check for recieving SENSF_REQ
	if( pRcvBuf[6] == 0x00)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]recieve SENSF_REQ");
		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]back to main detection loop");
		P2pTargetEnablePostCallbackCount = 0;
		//ms_card_detection( pDev);

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
		return 0;
	}

	ms_nfc_p2p_target_i2t_dep_init();
	ExpectDepPni = 0x00;
	if( ms_nfc_p2p_target_i2t_dep_req( pDev, pRcvBuf + 6, pRcvBuf[5] - 1) < 0)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]P2pTargetEnablePostCallbackCount: %d", P2pTargetEnablePostCallbackCount);

		if( P2pTargetEnablePostCallbackCount < P2P_TARGET_ENABLE_POST_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]well to keep recieve only mode");
			P2pTargetEnablePostCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_enable_post_callback]back to main detection loop");
		P2pTargetEnablePostCallbackCount = 0;
		//ms_card_detection( pDev);

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end ");
		return 0;
	}

	P2pTargetEnablePostCallbackCount = 0;
	PNALDebugLog( "ms_nfc_p2p_target_enable_post_callback() - end");
	return retVal;
}



static void ms_nfc_p2p_target_i2t_dep_init()
{
	PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_init() - start ");

	DepTotalPkg = 0;
	DepDataBufferLen = 0;
	//ExpectDepPni = 0x00;

	PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_init() - end ");
}

static int ms_nfc_p2p_target_i2t_dep_req( tNALMsrComInstance  *pDev, unsigned char *pDepData, int DepDataLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - start ");

	unsigned char cmdCat;
	unsigned char cmdCode;
	unsigned char iPfb;
	unsigned char iDid = 0x00;
	unsigned char iNad = 0x00;
	unsigned char genData[255];
	int genDataLen;
	unsigned char iMi;
	unsigned char iPni;
	int parsingIdx;
	unsigned char cmdRes[255];
	_MsNfcctrl18092DepReqInput msNfcctrl18092DepReqInput;
	unsigned char iAtn;


	// Dep Data: Parsing
	// Data: 0xD4| 0x06| PFB| [DID]| [NAD]| Data
	cmdCat = pDepData[0];
	cmdCode = pDepData[1];
	PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]cmdCat: %02x", cmdCat);
	PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]cmdCode: %02x", cmdCode);
	if( ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ)
	{
		// no response
		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]wrong command: ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ.");
		PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
		//retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
		P2pTargetI2tDepReqCallbackCount = 0;
        CNALWriteDataFinish(pDev);
		return -1;
	}

	iPfb = pDepData[2];
	PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]iPfb: %02x", iPfb);

	parsingIdx = 3;
	if( ( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID)
	{
		iDid = pDepData[ parsingIdx++];
	}

	if( ( iPfb & PFB_INF_NAD_MASK) == PFB_INF_NAD)
	{
		iNad = pDepData[ parsingIdx++];
	}
	genDataLen = DepDataLen - parsingIdx;
	if( genDataLen > 0)
	{
		memcpy( genData, pDepData + parsingIdx, genDataLen);
	}

	// Dep Data: Check
	// DID check
	if( (( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID))
	{
		if( DIDt == 0x00)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]Wrong DID Flag in PFB, DIDt: %02x", DIDt);
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
			return 0;
		}

		if( iDid != DIDt)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]Wrong DID, DIDt: %02x, iDid: %02x", DIDt, iDid);
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
			return 0;
		}

	}

	switch( iPfb & PFB_INF_MASK)
	{
		case PFB_INF:
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]PFB: PFB_INF ");
			iMi = iPfb & PFB_INF_MI;
			iPni = iPfb & 0x03;
			//ExpectDepPni = iPni; // temp solution: keep initiator PNI, and not check

#if 1
			//if( iPni != ExpectDepPni)
                   if( iPni != DepSendCurrentPni)
			{
				PNALDebugError( "[ms_nfc_p2p_target_i2t_dep_req]wrong PNI, DepSendCurrentPni= %x, iPni=%x ", DepSendCurrentPni, iPni);
				PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
				//retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
				ms_nfc_p2p_target_t2i_dep_req(pDev);
				return 0;
			}
#endif
            DepSendCurrentPni = ( DepSendCurrentPni + 1)% 4;
            ExpectDepPni = iPni; // temp solution: keep initiator PNI, and not check
            
			memcpy( DepDataBuffer + DepDataBufferLen, genData, genDataLen);
			DepDataBufferLen += genDataLen;
			DepTotalPkg++;
			if( ( iMi & PFB_INF_MI_MASK) != PFB_INF_MI)
			{
				// no chaining, send data to AP
				// return
				PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]no chaining ");
				pDev->temp_rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
				pDev->temp_rx_buffer[1] = NAL_EVT_P2P_SEND_DATA;
				memcpy( pDev->temp_rx_buffer + 2, DepDataBuffer, DepDataBufferLen);
                pDev->temp_target_buf_len = 0;
                if (pDev->sendTargetSddEvt == W_FALSE)
                {
                    PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]Discover evt not send, dep data into queue");
					memcpy( pDev->temp_target_buffer, pDev->temp_rx_buffer, 2 + DepDataBufferLen);
					pDev->temp_target_buf_len = 2 + DepDataBufferLen;
                }
				else if (!SetEventAvailableBytes(pDev, 2 + DepDataBufferLen))
                {
                    P2pTargetI2tDepReqCallbackCount = 0;
                    CNALWriteDataFinish(pDev);
                }
				
				PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
				return 0;
			}

			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]chaining ");
			//ExpectDepPni = ( ExpectDepPni + 1) % 4;
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]reply ACK ");

			cmdRes[0] = 0;
			cmdRes[1] = NFC_DEP_TARGET_CODE;
			cmdRes[2] = NFC_CMD_DEP_RSP;
			cmdRes[3] = PFB_ACK | iPni;
			parsingIdx = 4;
			if( ( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID)
			{
				cmdRes[3] |= PFB_INF_DID;
				cmdRes[parsingIdx++] = iDid;
			}

			if( ( iPfb & PFB_INF_NAD_MASK) == PFB_INF_NAD)
			{
				cmdRes[3] |= PFB_INF_NAD;
				cmdRes[parsingIdx++] = iNad;
			}
			cmdRes[0] = parsingIdx;

			msNfcctrl18092DepReqInput.CtrlInfoFlag = W_FALSE;
			msNfcctrl18092DepReqInput.WT = 0;
			msNfcctrl18092DepReqInput.RTOX = 0;
			msNfcctrl18092DepReqInput.DepFlag = DrvP2P_DepFlag_WaitRx;
			msNfcctrl18092DepReqInput.DepContentLen = cmdRes[0];
			msNfcctrl18092DepReqInput.DepContent = cmdRes;

			retVal = ms_nfcctrl_18092_dep_req( pDev, &msNfcctrl18092DepReqInput);
			pDev->cbRecv = ( RecvCallBack*)ms_nfc_p2p_target_i2t_dep_req_callback;
			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
			return retVal;

			break;

		case PFB_ACK:
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]PFB: PFB_ACK ");
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]wrong PFB: PFB_ACK ");
			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
			return 0;

			break;

		case PFB_SUP:
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]PFB: PFB_SUP ");
			iAtn = iPfb & PFB_SUP_ATN_MASK;
			if( iAtn == PFB_SUP_TO)
			{
				PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req]Timeout not support ");
			}

			cmdRes[0] = 0;
			cmdRes[1] = NFC_DEP_TARGET_CODE;
			cmdRes[2] = NFC_CMD_DEP_RSP;
			cmdRes[3] = iPfb;
			parsingIdx = 4;
			if( ( iPfb & PFB_SUP_DID_MASK) == PFB_SUP_DID)
			{
				//cmdRes[2] |= PFB_SUP_DID;
				cmdRes[parsingIdx++] = iDid;
			}

			if( ( iPfb & PFB_SUP_NAD_MASK) == PFB_SUP_NAD)
			{
				//cmdRes[2] |= PFB_SUP_NAD;
				cmdRes[parsingIdx++] = iNad;
			}
			if( genDataLen > 0)
			{
				memcpy( cmdRes + parsingIdx, genData, genDataLen);
				parsingIdx = parsingIdx + genDataLen;
			}
			cmdRes[0] = parsingIdx;

			msNfcctrl18092DepReqInput.CtrlInfoFlag = W_FALSE;
			msNfcctrl18092DepReqInput.WT = 0;
			msNfcctrl18092DepReqInput.RTOX = 0;
			msNfcctrl18092DepReqInput.DepFlag = DrvP2P_DepFlag_WaitRx;
			msNfcctrl18092DepReqInput.DepContentLen = cmdRes[0];
			msNfcctrl18092DepReqInput.DepContent = cmdRes;

			retVal = ms_nfcctrl_18092_dep_req( pDev, &msNfcctrl18092DepReqInput);
			pDev->cbRecv = ( RecvCallBack*)ms_nfc_p2p_target_i2t_dep_req_callback;

			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
			return retVal;

			break;

		default:
			PNALDebugError( "[ms_nfc_p2p_target_i2t_dep_req]wrong PFB: others ");
			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
			return 0;
			break;
	}

	PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req() - end ");
	return retVal;

}


int ms_nfc_p2p_target_i2t_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - start ");
	int retVal = 0;
	int idxForLoop;

	PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]STS: %02x.", pRcvBuf[3]);
	if( pRcvBuf[3] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[3])
		{
			default:
				PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]Other error return.");

				PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
				if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
					P2pTargetI2tDepReqCallbackCount++;
					retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
					PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
				P2pTargetI2tDepReqCallbackCount = 0;
                //ms_card_detection(pDev);
                CNALWriteDataFinish(pDev);

				PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
				return 0;
		}
	}

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{

		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]No Data or not long enough for CRC16 calculation");

		if( RcvLen == 4 && pRcvBuf[3] == 0x26)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]pRcvBuf[3]: 0x26, under some SDD loop");

			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]go to SDD again");
			P2pTargetI2tDepReqCallbackCount = 0;
			ms_nfc_p2p_target_enable( pDev, 0x00);

			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
		if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_I2T_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
			P2pTargetI2tDepReqCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;
		//ms_card_detection(pDev);
		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_18092_248, pRcvBuf + 5, pRcvBuf[4]) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback(): CRC error");

		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]P2pTargetI2tDepReqCallbackCount: %d", P2pTargetI2tDepReqCallbackCount);
		if( P2pTargetI2tDepReqCallbackCount < P2P_TARGET_ENABLE_POST_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]well to keep recieve only mode");
			P2pTargetI2tDepReqCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;

        CNALWriteDataFinish(pDev);
		PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	// check for recieving SENSF_REQ
	if( pRcvBuf[6] == 0x00)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]recieve SENSF_REQ");
		PNALDebugLog( "[ms_nfc_p2p_target_i2t_dep_req_callback]back to main detection loop");
		P2pTargetI2tDepReqCallbackCount = 0;
		//ms_card_detection( pDev);

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
		return 0;
	}

	ms_nfc_p2p_target_i2t_dep_req( pDev, pRcvBuf + 6, pRcvBuf[5] - 1);

	PNALDebugLog( "ms_nfc_p2p_target_i2t_dep_req_callback() - end ");
	return retVal;

}

static int ms_nfc_p2p_target_t2i_dep_req( tNALMsrComInstance  *pDev)
{
	int retVal = 0;

	PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req() - start ");

	//_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;
	_MsNfcctrl18092DepReqInput msNfcctrl18092DepReqInput;
	unsigned char cmdDepRsp[255];
	int interfaceSendRet;
	int remainBufferSize;
	int bufferIdx;

	remainBufferSize = DepSendDataBufferLen - DepSendDataBufferIdx;
	PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req]remainBufferSize: %d", remainBufferSize);
	PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req]DepSendDataBufferLen: %d", DepSendDataBufferLen);
	PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req]DepSendDataBufferIdx: %d", DepSendDataBufferIdx);

	cmdDepRsp[0] = 0;
	cmdDepRsp[1] = NFC_DEP_TARGET_CODE;
	cmdDepRsp[2] = NFC_CMD_DEP_RSP;
	cmdDepRsp[3] = PFB_INF | ExpectDepPni; //temp solution

	bufferIdx = 4;
	if( DIDt != 0x00)
	{
		cmdDepRsp[3] |= PFB_INF_DID;
		cmdDepRsp[ bufferIdx++] = DIDt;
	}


	if( remainBufferSize > MaxDepBatchSize)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req]chaining handling");
		cmdDepRsp[3] |= PFB_INF_MI;
		memcpy( cmdDepRsp + bufferIdx, DepSendDataBuffer + DepSendDataBufferIdx, MaxDepBatchSize);

		msNfcctrl18092DepReqInput.CtrlInfoFlag = W_FALSE;
		msNfcctrl18092DepReqInput.WT = 0;
		msNfcctrl18092DepReqInput.RTOX = 0;
		msNfcctrl18092DepReqInput.DepFlag = DrvP2P_DepFlag_WaitRx;
		msNfcctrl18092DepReqInput.DepContentLen = bufferIdx + MaxDepBatchSize;
		msNfcctrl18092DepReqInput.DepContent = cmdDepRsp;

		DepSendCurrentBatchSize = MaxDepBatchSize;
	}
	else
	{
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req]non-chaining handling");
		memcpy( cmdDepRsp + bufferIdx, DepSendDataBuffer + DepSendDataBufferIdx, remainBufferSize);

		msNfcctrl18092DepReqInput.CtrlInfoFlag = W_FALSE;
		msNfcctrl18092DepReqInput.WT = 0;
		msNfcctrl18092DepReqInput.RTOX = 0;
		msNfcctrl18092DepReqInput.DepFlag = DrvP2P_DepFlag_WaitRx;
		msNfcctrl18092DepReqInput.DepContentLen = bufferIdx + remainBufferSize;
		msNfcctrl18092DepReqInput.DepContent = cmdDepRsp;

		DepSendCurrentBatchSize = remainBufferSize;
	}
    cmdDepRsp[0] = msNfcctrl18092DepReqInput.DepContentLen;

	//interfaceSendRet = ms_a3_p2p_target_dep_req( pDev, &a3P2pTargetDepReqInput);
	interfaceSendRet = ms_nfcctrl_18092_dep_req( pDev, &msNfcctrl18092DepReqInput);
	pDev->cbRecv = ( RecvCallBack*)ms_nfc_p2p_target_t2i_dep_req_callback;
	retVal = interfaceSendRet;

	PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req() - end ");

	return retVal;

}

int ms_nfc_p2p_target_t2i_dep_req_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	int retVal = 0;

	PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - start ");

	int idxForLoop;
	unsigned char cmdCat;
	unsigned char cmdCode;
	unsigned char iPfb;
	unsigned char iDid = 0x00;
	unsigned char iNad = 0x00;
	unsigned char iPni;
	unsigned char iAtn;
	unsigned char cmdRes[255];
	int parsingIdx;
	unsigned char genData[255];
	int genDataLen;
	//_A3P2pTargetDepReqInput a3P2pTargetDepReqInput;
	_MsNfcctrl18092DepReqInput msNfcctrl18092DepReqInput;


	PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]STS: %02x.", pRcvBuf[3]);
	if( pRcvBuf[3] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[3])
		{
			case 0x1B:
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]RF field off.");
				PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
				P2pTargetT2iDepReqCallbackCount = 0;
                		CNALWriteDataFinish(pDev);
				return 0;

			default:
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]Other error return.");

				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
				if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
				{
					PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
					P2pTargetT2iDepReqCallbackCount++;
					retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);
					PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
					return 0;
				}

				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
				P2pTargetT2iDepReqCallbackCount = 0;

				CNALWriteDataFinish(pDev);

				PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
		}
	}


	// output the recieved buffer
	PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]pRcvBuf:");
    PNALDebugLogBuffer(pRcvBuf, RcvLen);

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 6)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]No Data or not long enough for CRC16 calculation");

		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_18092_248, pRcvBuf + 5, pRcvBuf[4]) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback(): CRC error");

		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	// check for recieving SENSF_REQ
	if( pRcvBuf[6] == 0x00)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]recieve SENSF_REQ");
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;
		//ms_card_detection( pDev);

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}


	// Dep Data: Parsing
	cmdCat = pRcvBuf[6];
	cmdCode = pRcvBuf[7];
	if( ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ)
	{
		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]wrong command code: ( cmdCat != 0xD4) || cmdCode != NFC_CMD_DEP_REQ");

		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
		if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
			P2pTargetT2iDepReqCallbackCount++;
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
		P2pTargetT2iDepReqCallbackCount = 0;

		CNALWriteDataFinish(pDev);

		PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
		return 0;
	}

	iPfb = pRcvBuf[8];
	PNALDebugLog( " iPfb: %02x ", iPfb);
	parsingIdx = 9;
	if( ( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID)
	{
		iDid = pRcvBuf[ parsingIdx++];
	}

	if( ( iPfb & PFB_INF_NAD_MASK) == PFB_INF_NAD)
	{
		iNad = pRcvBuf[ parsingIdx++];
	}

	// Dep Data: check
	// DID check
	if( (( iPfb & PFB_INF_DID_MASK) == PFB_INF_DID))
	{
		if( DIDt == 0x00)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]Wrong DID Flag in PFB, DIDt: %02x", DIDt);
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

		if( iDid != DIDt)
		{
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]Wrong DID, DIDt: %02x, iDid: %02x", DIDt, iDid);
			retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
		}

	}


	genDataLen = pRcvBuf[1] + 2 - parsingIdx - 2;
	if( genDataLen > 0)
	{
		memcpy( genData, pRcvBuf + parsingIdx, genDataLen);
	}

	switch( iPfb & PFB_INF_MASK)
	{
		case PFB_INF:
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]PFB: PFB_INF ");
			ms_nfc_p2p_target_i2t_dep_init();
			ms_nfc_p2p_target_i2t_dep_req( pDev, pRcvBuf + 6, pRcvBuf[5] - 1);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
			break;

		case PFB_ACK:
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]PFB: PFB_ACK ");

			iPni = iPfb & 0x03;
			ExpectDepPni = iPni;
#if 0
			if( iPni != DepSendCurrentPni)
			{
				// wrong pni
				// resend
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]wrong pni");
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]resend");
				ms_nfc_p2p_target_t2i_dep_req( pDev);
				PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
			}
#endif

			if( ( iPfb & PFB_ACK_NACK_MASK) == PFB_ACK_NACK)
			{
				//NACK
				// resend
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]initiator NACK");
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]resend");
				ms_nfc_p2p_target_t2i_dep_req( pDev);
				PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
			}

			DepSendDataBufferIdx += DepSendCurrentBatchSize;
			DepSendCurrentPni = ( DepSendCurrentPni + 1)% 4;
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]DepSendDataBufferIdx: %d", DepSendDataBufferIdx);
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]DepSendCurrentPni: %02x", DepSendCurrentPni);
			retVal = ms_nfc_p2p_target_t2i_dep_req( pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
			break;

		case PFB_SUP:
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]PFB: PFB_SUP ");

			iAtn = iPfb & PFB_SUP_ATN_MASK;
			if( iAtn == PFB_SUP_TO)
			{
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]Timeout not support ");
			}

			cmdRes[0] = 0;
			cmdRes[1] = NFC_DEP_TARGET_CODE;
			cmdRes[2] = NFC_CMD_DEP_RSP;
			cmdRes[3] = iPfb;
			parsingIdx = 4;
			if( ( iPfb & PFB_SUP_DID_MASK) == PFB_SUP_DID)
			{
				//cmdRes[2] |= PFB_SUP_DID;
				cmdRes[parsingIdx++] = iDid;
			}

			if( ( iPfb & PFB_SUP_NAD_MASK) == PFB_SUP_NAD)
			{
				//cmdRes[2] |= PFB_SUP_NAD;
				cmdRes[parsingIdx++] = iNad;
			}
			if( genDataLen > 0)
			{
				memcpy( cmdRes + parsingIdx, genData, genDataLen);
				parsingIdx = parsingIdx + genDataLen;
			}
			cmdRes[0] = parsingIdx;

			msNfcctrl18092DepReqInput.CtrlInfoFlag = W_FALSE;
			msNfcctrl18092DepReqInput.WT = 0;
			msNfcctrl18092DepReqInput.RTOX = 0;
			msNfcctrl18092DepReqInput.DepFlag = DrvP2P_DepFlag_WaitRx;
			msNfcctrl18092DepReqInput.DepContentLen = cmdRes[0];
			msNfcctrl18092DepReqInput.DepContent = cmdRes;

			ms_nfc_p2p_target_i2t_dep_init();
			retVal = ms_nfcctrl_18092_dep_req( pDev, &msNfcctrl18092DepReqInput);
			pDev->cbRecv = ( RecvCallBack*)ms_nfc_p2p_target_i2t_dep_req_callback;

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return retVal;
			break;

		default:
			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]PFB: others ");

			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]P2pTargetT2iDepReqCallbackCount: %d", P2pTargetT2iDepReqCallbackCount);
			if( P2pTargetT2iDepReqCallbackCount < P2P_TARGET_T2I_DEP_REQ_CALLBACK_COUNT_MAX)
			{
				PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]well to keep recieve only mode");
				P2pTargetT2iDepReqCallbackCount++;
				retVal = ms_nfcctrl_18092_dep_receive_only_req( pDev);

				PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
				return 0;
			}

			PNALDebugLog( "[ms_nfc_p2p_target_t2i_dep_req_callback]back to main detection loop");
			P2pTargetT2iDepReqCallbackCount = 0;
			//ms_card_detection( pDev);
			CNALWriteDataFinish(pDev);

			PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");
			return 0;
			break;
	}

	PNALDebugLog( "ms_nfc_p2p_target_t2i_dep_req_callback() - end ");

	return retVal;

}

//for A3 Polling mode
int ms_target_pl_config_req_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_target_pl_config_req_callback]start, len = %d\n", len);
    PNALDebugLogBuffer(inbuf, len);
    //A3Rsp(1)[0x02] , Len(1)[n], SubFuncCode(1), ParamNum(1) [n1] , ParaID(1)[0xA0],ParamLen(1)[n],Result1
    if (inbuf[6] != RFID_STS_SUCCESS)
    {
        PNALDebugLog("[ms_target_pl_config_req_callback]Result: fail, ecode= %x\n", inbuf[6]);
    }		
	dev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
	dev->rx_buffer[1] = NAL_RES_OK;
	dev->nb_available_bytes = 2;
    PNALDebugLog("[ms_target_pl_config_req_callback]end");	
    return 0;
}

static int ms_target_pl_config_req( tNALMsrComInstance *pDev, _MsNfcctrl18092Enablep2pReqInput *MsNfcctrl18092Enablep2pReqInput)
{
	int retVal = 0;

	PNALDebugLog( "ms_target_pl_config_req() - start ");

	int cmdNfcctrlReqLen = 0;
	int nfcctrlIntSendRet = 0;
	int bufferIdx = 0;
	unsigned char paramCount = 0x03;

    //A3Cmd(1)[0x4B], Len(1)[n],Body(n)
	pDev->ant_send_buffer[0] = PL_CMD_CODE;
	pDev->ant_send_buffer[1] = 0;	  //LEN
		
	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
	pDev->ant_send_buffer[2] = DrvPL_FuncCode_Config; //SubFuncCode.
	pDev->ant_send_buffer[3] = 0x02; 	  //ParamNum.
	/*	
		ParaID(1)[0x00],ParamLen(1)[n1],DrvPL_ConfigTableFlag_et (1),PL_CmdCount(1)(n2), [{XXX} * n2]
		{XXX} format
		ID(1), CmdLen(1)[n3],CmdBuf(n3)
		ID format
		DrvPL_Category_et | ID
	*/
	pDev->ant_send_buffer[4] = DrvPL_ModeParam_ConfigTable;
    pDev->ant_send_buffer[5] = 0;  //ParamLen
    pDev->ant_send_buffer[6] = DrvPL_Category_Target;  //DrvPL_ConfigTableFlag_et	
	pDev->ant_send_buffer[7] = 0x01; 	
	pDev->ant_send_buffer[8] = DrvPL_PLItem_PT_P2P_424;
	pDev->ant_send_buffer[9] = 0;	

    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    pDev->ant_send_buffer[10] = 0x4A;
	//pDev->ant_send_buffer[1] = 0;    //LEN

	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  K , Param_n1(n3)
	pDev->ant_send_buffer[12] = DrvP2P_FuncCode_EnaleP2P;
	pDev->ant_send_buffer[13] = paramCount;

	//ParaID(1)[0x00],ParamLen(1)[3], Speed(1),Mode(1),PollingTimes(1)
	pDev->ant_send_buffer[14] = DrvP2P_ModeParam_Mode;
	pDev->ant_send_buffer[15] = 0x03;
	pDev->ant_send_buffer[16] = MsNfcctrl18092Enablep2pReqInput->P2pSpeedType;
	pDev->ant_send_buffer[17] = MsNfcctrl18092Enablep2pReqInput->P2pRole;
	pDev->ant_send_buffer[18] = MsNfcctrl18092Enablep2pReqInput->PollingCount;

	//DrvP2P_ModeParam_Polling_RES= 2,
	//ParaID(1)[0x02],ParamLen(1)[9], NFCID_Type (1),ID(8)
	pDev->ant_send_buffer[19] = DrvP2P_ModeParam_Polling_RES;
	switch( MsNfcctrl18092Enablep2pReqInput->NfcId2GenType)
	{
		case DrvP2P_NFCID_HostSpec:
			pDev->ant_send_buffer[20] = 0x09;
			pDev->ant_send_buffer[21] = MsNfcctrl18092Enablep2pReqInput->NfcId2GenType;
			memcpy( &pDev->ant_send_buffer[22], MsNfcctrl18092Enablep2pReqInput->NfcId2, NFCID2_LEN);
			bufferIdx = 30; // 12 + 8 + 10

			break;

		case DrvP2P_NFCID_FwGenerate:
		default:
			pDev->ant_send_buffer[20] = 0x01;
			pDev->ant_send_buffer[21] = MsNfcctrl18092Enablep2pReqInput->NfcId2GenType;
			bufferIdx = 22; //12 + 10

			break;
	}

	//DrvP2P_ModeParam_ATR_RES= 4,
    //ParaID(1)[0x04],Len[n+7],GeneralByteNum(1)[n],IDType(1)[1],Payload(n+5)
	pDev->ant_send_buffer[ bufferIdx++] = DrvP2P_ModeParam_ATR_RES;
	switch( MsNfcctrl18092Enablep2pReqInput->NfcId3GenType)
	{
		case DrvP2P_NFCID_HostSpec:
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen + 17;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->NfcId3GenType;
			memcpy( &pDev->ant_send_buffer[ bufferIdx], MsNfcctrl18092Enablep2pReqInput->NfcId3, NFCID3_LEN);
			bufferIdx += 10;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->DID;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BS;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BR;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->TO;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->PP;
			memcpy( &pDev->ant_send_buffer[ bufferIdx], MsNfcctrl18092Enablep2pReqInput->LinkActMsg, MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen);
			bufferIdx += MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;

			break;

		case DrvP2P_NFCID_FwGenerate:
		default:
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen + 7;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->NfcId3GenType;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->DID;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BS;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->BR;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->TO;
			pDev->ant_send_buffer[ bufferIdx++] = MsNfcctrl18092Enablep2pReqInput->PP;
			memcpy( &pDev->ant_send_buffer[ bufferIdx], MsNfcctrl18092Enablep2pReqInput->LinkActMsg, MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen);
			bufferIdx += MsNfcctrl18092Enablep2pReqInput->LinkActMsgLen;

			break;
	}
    pDev->ant_send_buffer[ bufferIdx++] = DrvPL_ModeParam_FlashOp;
    pDev->ant_send_buffer[ bufferIdx++] = 1;    //LEN	
    pDev->ant_send_buffer[ bufferIdx++] = DrvPL_FlashAction_Write;	

	cmdNfcctrlReqLen = bufferIdx;
	pDev->ant_send_buffer[1] = cmdNfcctrlReqLen - 2;
	pDev->ant_send_buffer[5] = cmdNfcctrlReqLen - 6 - 3;
	pDev->ant_send_buffer[9] = cmdNfcctrlReqLen - 10 - 3;  //3
	pDev->ant_send_buffer[11] = cmdNfcctrlReqLen - 2 - 10 - 3;	
	nfcctrlIntSendRet= ms_interfaceSend( pDev, cmdNfcctrlReqLen);
	retVal = nfcctrlIntSendRet;
	pDev->cbRecv = (RecvCallBack *)ms_target_pl_config_req_callback;
	PNALDebugLog( "ms_target_pl_config_req() - end ");
	return retVal;
}












