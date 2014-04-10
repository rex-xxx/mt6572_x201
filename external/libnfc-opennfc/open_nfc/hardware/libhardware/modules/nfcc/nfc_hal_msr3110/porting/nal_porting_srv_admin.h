#ifndef __OPEN_NFC_MSTAR_SRV_ADMIN_H
#define __OPEN_NFC_MSTAR_SRV_ADMIN_H


#include "nfc_hal.h"
#define A3_PL_MODE 1

#define PL_CMD_CODE 0x4B

typedef enum
{
	DrvPL_FuncCode_Config = 0,
	DrvPL_FuncCode_Action = 1,
	DrvPL_FuncCode_GetInfo = 2,
	DrvPL_FuncCode_Err = 0xFF,
}DrvPL_SubFuncCode_et;


typedef enum
{
	DrvPL_ModeParam_ConfigTable= 0,
	DrvPL_ModeParam_ConfigMask= 1,
	DrvPL_ModeParam_RunAction= 2,
	DrvPL_ModeParam_GetInfo = 3,
	DrvPL_ModeParam_FlashOp = 4,
	DrvPL_ModeParam_Result= 0xA0,
	DrvPL_ModeParam_EvtRsp=0xA1,
}DrvPL_ModeParam_et;

typedef enum
{
	DrvPL_ConfigTableFlag_ClearFisrt = 0x01,
	//DrvPL_ConfigTableFlag_WriteToFlash = 0x02,
	
}DrvPL_ConfigTableFlag_et;//BitwiseOr

typedef enum
{
	DrvPL_Category_Reader = 0x00,
	DrvPL_Category_Card = 0x40,
	DrvPL_Category_Target = 0x80,
	DrvPL_Category_Mask = 0xC0,
}DrvPL_Category_et;

typedef enum
{
	
	DrvPL_PLItem_R_Type1 = DrvPL_Category_Reader|0x08,// 	0x08
	DrvPL_PLItem_R_Felica = DrvPL_Category_Reader|0x06, // 	0x06
	DrvPL_PLItem_R_ISO693 = DrvPL_Category_Reader|0x04, //  0x04
	DrvPL_PLItem_R_ISO43B = DrvPL_Category_Reader|0x01, // 0x01
	DrvPL_PLItem_R_ISO43A = DrvPL_Category_Reader|0x00, // 0x00- use level-4 , TO DO discussion...


	DrvPL_PLItem_PI_P2P_106 = DrvPL_Category_Target|0x07,// 	0x87
	DrvPL_PLItem_PI_P2P_212 = DrvPL_Category_Target|0x20,//  0xA0
	DrvPL_PLItem_PI_P2P_424 = DrvPL_Category_Target|0x21,// 0xA1

	DrvPL_PLItem_PT_P2P_106 = DrvPL_Category_Target|0x17, // 0x97
	DrvPL_PLItem_PT_P2P_212 = DrvPL_Category_Target|0x22, // 0xA2
	DrvPL_PLItem_PT_P2P_424 = DrvPL_Category_Target|0x23, // 0xA3
	DrvPL_PLItem_PIT_P2P_212 = DrvPL_Category_Target|0x24, // 0xA4
	DrvPL_PLItem_PIT_P2P_424 = DrvPL_Category_Target|0x25, // 0xA5
	DrvPL_PLItem_Reset = DrvPL_Category_Target|0x27, // 0xA7

	DrvPL_PLItem_C_CONN = DrvPL_Category_Target|0x28, // 0xA8
	DrvPL_PLItem_C_TRAN = DrvPL_Category_Target|0x29, // 0xA9	
	
	DrvPL_PLItem_C_ISO43A = DrvPL_Category_Card|0x10, // 0x50
	DrvPL_PLItem_C_ISO43B = DrvPL_Category_Card|0x11, // 0x50
	
}DrvPL_PLItem_et;


typedef enum
{
	DrvPL_RunAction_Run = 1,
	DrvPL_RunAction_Resume = 2,
	DrvPL_RunAction_Stop = 3,
	DrvPL_RunAction_Enable = 4,
	DrvPL_RunAction_Disable = 5,
}DrvPL_RunAction_et;

typedef enum
{
	DrvPL_InfoID_ConfigTable = 0,
	DrvPL_InfoID_ConfigMask = 1,
	DrvPL_InfoID_EvtQueue = 2,

}DrvPL_InfoID_et;


typedef enum
{
	DrvPL_CmdSrc_FromHost = 1,
	DrvPL_CmdSrc_FromPL_System = 2,
}DrvPL_CmdSrc_et;


typedef enum
{
	DrvPL_NowStaus_Idle = 1,
	DrvPL_NowStaus_HaveIssuePLCmd = 2,
	DrvPL_NowStaus_HaveReportToHost = 3,
	DrvPL_NowStaus_HaveStopByHost = 4,
}DrvPL_NowStaus_et;


typedef enum
{
	DrvPL_PL_Cmd_DecideResult_None = 0,
	DrvPL_PL_Cmd_DecideResult_SendHost = 1,
}DrvPL_PL_Cmd_DecideResult_et;

typedef enum
{
	DrvPL_FlashAction_Clear = 0,
	DrvPL_FlashAction_Write = 1,
	DrvPL_FlashAction_Load = 2,
	DrvPL_FlashAction_None = 3
}DrvPL_FlashAction_et;

typedef enum
{
	DrvPL_RunMode_Reader = 0x01,
	DrvPL_RunMode_Card = 0x02,
	DrvPL_RunMode_Target = 0x04,
	DrvPL_RunMode_Only_Card_Mode = 0x08,
	DrvPL_RunMode_No_Random_Mode = 0x10
}DrvPL_RunMode_et;//BitwiseFlag

typedef enum
{
	DrvPL_P2P_Initiator_212 = 0x01,
	DrvPL_P2P_Initiator_424 = 0x02,
	DrvPL_P2P_Target_212 = 0x04,
	DrvPL_P2P_Target_424 = 0x08,
	DrvPL_P2P_Initiator_Target_212 = 0x10,
	DrvPL_P2P_Initiator_Target_424 = 0x20
}DrvPL_ExternMask_et;

typedef enum 
{
    NFC_IRQ_STS_INIT   = 0x00,
    NFC_IRQ_STS_SUCCESS  = 0x01,
    NFC_IRQ_STS_FAIL  = 0x02,
    NFC_IRQ_STS_WAIT   = 0x03,
    NFC_IRQ_STS_RAISE  = 0x04,
    NFC_IRQ_STS_ABORT  = 0x05
} NFC_IRQ_STS; 




static int ms_admin_cbrecv_get_parameter(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);
int ms_open_nfc_admin_cbrecv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);
int ms_open_nfc_admin_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int ms_card_detection(tNALMsrComInstance  *dev);
int ms_tag_emulation(tNALMsrComInstance  *dev);
int ms_detection_polling_getinfo(tNALMsrComInstance  *dev);
int ms_open_nfc_uicc_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);


static int ms_open_nfc_admin_cmd_get_parameter(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_cmd_set_parameter(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_cmd_update_firmware(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_cmd_detection(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_cmd_production_test(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_cmd_self_test(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);

static int ms_open_nfc_admin_evt_rf_field(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_evt_standby_mode(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_evt_nfcc_error(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_evt_raw_message(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_standby_mode(tNALMsrComInstance  *dev);



#endif

