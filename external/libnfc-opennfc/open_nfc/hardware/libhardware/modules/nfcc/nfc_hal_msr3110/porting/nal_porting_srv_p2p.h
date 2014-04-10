#ifndef __OPEN_NFC_MSTAR_SRV_P2P_H
#define __OPEN_NFC_MSTAR_SRV_P2P_H

/* SubFuncCode */
typedef enum
{
    DrvP2P_FuncCode_EnaleP2P = 0x00,
    DrvP2P_FuncCode_DEP,
    DrvP2P_FuncCode_DisableP2P
}SubFuncCode;

/* SubFuncCode */
typedef enum
{
    DrvP2P_ModeParam_Mode = 0x00,
    DrvP2P_ModeParam_Polling_REQ,
    DrvP2P_ModeParam_Polling_RES,
    DrvP2P_ModeParam_ATR_REQ,
    DrvP2P_ModeParam_ATR_RES,
    DrvP2P_ModeParam_RawData,
    DrvP2P_ModeParam_CtrlInfo
}ModeParam;

typedef enum
{
	DRVP2P_sp106 = 0,
	DRVP2P_sp212 = 1,
	DRVP2P_sp424 = 2,
	DRVP2P_sp848 = 3,
	DRVP2P_sp1696 = 4,
	DRVP2P_sp3392 = 5,
}DrvP2P_speed_et;

typedef enum
{
	DRVP2P_Initiator = 0x1,
	DRVP2P_Target = 0x2,
	DRVP2P_I_After_T = 0x3,
	DRVP2P_Unknown = 0xFF,
}DrvP2P_IorT_et;

typedef enum
{
	DrvP2P_NFCID_HostSpec = 0,
	DrvP2P_NFCID_FwGenerate = 1
}DrvP2P_NFCID_et;

typedef enum
{
	DRVP2P_result_As_Initiator = 0,
	DRVP2P_result_As_Target = 1,
	DRVP2P_result_Sense_Other_RF = 2,
	DRVP2P_result_NoFind = 3,
	DRVP2P_result_Err = 4
}DrvP2P_EnableResult_et;

typedef enum
{
	DrvP2P_DepFlag_WaitRx = 0x01,
} DrvP2P_DEP_Flag_et; // bitwise flag

typedef enum
{
	DrvP2P_SDD_UnKnown = 0,
	DrvP2P_SDD_I_No_PollingRsp = 1,
	DrvP2P_SDD_I_PollingRspLenWrong = 2,
	DrvP2P_SDD_I_ATR_Fail = 3,
	DrvP2P_SDD_T_SenseRf = 4,
	DrvP2P_SDD_T_SDD_Start = 5,
	DrvP2P_SDD_T_SDD_Done = 6,
	DrvP2P_SDD_T_ATR_Done = 7,
	DrvP2P_SDD_ATR_Fail = 1,
}DrvP2P_FailCause_et;

#endif


