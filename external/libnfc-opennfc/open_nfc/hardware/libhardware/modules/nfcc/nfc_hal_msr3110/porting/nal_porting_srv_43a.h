#ifndef __OPEN_NFC_MSTAR_SRV_443A_H
#define __OPEN_NFC_MSTAR_SRV_443A_H

#define UL_READ_BLK                                  0x30
#define UL_WRITE_BLK                                 0xA2
#define RF_PARAM_PAS_I_106                      0x40
#define RF_PARAM_43A                                0x00

typedef enum
{
    CmdNull = 0,
    CmdStaticMin = 0x01,
        CmdStaticEcho   = 0x01,
        CmdStaticHostIF = 0x02,
        CmdStaticFlashOp = 0x03,
        CmdStaticCipherOp = 0x04,
        CmdStaticGetVer = 0x05,
        CmdStaticMemRW = 0x06,
        CmdStatic_ISO7816_GoConfig_Cmd = 0x07,
        CmdStatic_ISO7816_GoConfig_Rpt = 0x08,
        CmdStatic_ISO7816_XfrApdu_Cmd = 0x09,
        CmdStatic_ISO7816_XfrApdu_Rpt = 0x0a,
        CmdStaticSWPHCICmd = 0x0b,
        CmdStatic_PLMode_Setting = 0x0c,
        CmdStatic_HF_Write_Flash = 0x0d,
        CmdStatic_EventQueue = 0x0e,
        CmdStatic_NFC_Initiator = 0x0f,
        CmdStaticRegistryCmd = 0x10,
        CmdStaticPM_Config_Cmd = 0x11,
        CmdStaticNFC_Raw = 0x12,
        CmdStaticEFuseTestCmd = 0x13,
        //below cmd are for testing only.
        CmdStaticTestDispatchStarter = 0x2c,
        CmdStaticTestDispatchLevelLow = 0x2d,
        CmdStaticTestDispatchLevelHigh = 0x2e,
    CmdStaticMax = 0x2f,
    CmdIndexedMin = CmdStaticMax+1,     //0x30
    CmdIndexedMax = CmdIndexedMin+15,   //0x3f
    CmdDynamicMin = CmdIndexedMax+1,    //0x40
        CmdDynamicTRNG = 0x41,
        CmdDynamicHF_UniProtocol_Entry = 0x42,
        CmdDynamicMFReader = 0x43,
        CmdDynamicCiperAP = 0x44,

        CmdDynamicISO15693_Inventory = 0x45,
        CmdDynamicISO14443A_Inventory = 0x46,
        CmdDynamicISO14443B_Inventory = 0x47,
        CmdDynamicFELICA_Inventory = 0x48,

        CmdISO7816PCSC_Min = 0x60,
/*
            CmdDynamic_ISO7816_P2R_POWER_ON = PC_TO_RDR_ICC_POWER_ON,
            CmdDynamic_ISO7816_P2R_POWER_OFF = PC_TO_RDR_ICC_POWER_OFF,
            CmdDynamic_ISO7816_P2R_SLOT_STATUS = PC_TO_RDR_GET_SLOT_STATUS ,
            CmdDynamic_ISO7816_P2R_XFR_BLOCK = PC_TO_RDR_XFR_BLOCK ,
            CmdDynamic_ISO7816_P2R_GET_PARAMETERS = PC_TO_RDR_GET_PARAMETERS ,
            CmdDynamic_ISO7816_P2R_RESET_PARAMETERS= PC_TO_RDR_RESET_PARAMETERS,
            CmdDynamic_ISO7816_P2R_SET_PARAMETERS = PC_TO_RDR_SET_PARAMETERS,
            CmdDynamic_ISO7816_P2R_ESCAPE = PC_TO_RDR_ESCAPE,
            CmdDynamic_ISO7816_P2R_ICC_CLOCK = PC_TO_RDR_ICC_CLOCK ,
            CmdDynamic_ISO7816_P2R_T0APDU = PC_TO_RDR_T0APDU ,
            CmdDynamic_ISO7816_P2R_SECURE = PC_TO_RDR_SECURE ,
            CmdDynamic_ISO7816_P2R_MECHANICAL = PC_TO_RDR_MECHANICAL ,
            CmdDynamic_ISO7816_P2R_ABORT = PC_TO_RDR_ABORT,
            CmdDynamic_ISO7816_P2R_SET_DATARATE_AND_CLOCK_FREQUENCY = PC_TO_RDR_SET_DATARATE_AND_CLOCK_FREQUENCY,
*/
            //CmdDynamic_ISO7816_R2P_DATABLOCK = RDR_TO_PC_DATABLOCK,
            //CmdDynamic_ISO7816_R2P_SLOT_STATUS = RDR_TO_PC_SLOT_STATUS,
            //CmdDynamic_ISO7816_R2P_PARAMETERS = RDR_TO_PC_PARAMETERS ,
            //CmdDynamic_ISO7816_R2P_ESCAPE = RDR_TO_PC_ESCAPE ,
            //CmdDynamic_ISO7816_R2P_DATARATE_AND_CLOCK_FREQUENCY = RDR_TO_PC_DATARATE_AND_CLOCK_FREQUENCY,
            //CmdDynamic_ISO7816_R2P_NOTIFY_SLOT_CHANGE = RDR_TO_PC_NOTIFY_SLOT_CHANGE,
            //CmdDynamic_ISO7816_R2P_HARDWARE_ERROR = RDR_TO_PC_HARDWARE_ERROR ,
        CmdISO7816PCSC_Max = 0x7f,

        CmdDynamic_SWP_SWITCH_7816 = 0x81,
        CmdDynamic_Disable_H32iTimer = 0x82,
        CmdDynamic_Enable_H32iTimer = 0x83,

        CmdDynamicTester = 0xf0,        //  testers: 0xf0~0xfe
        CmdDynamicTesterSWPHCI,

    CmdDynamicMax = 0xfe,
    CmdCodeMax = 0xff
} A3_CmdCode_et;

enum
{
    MF_OP_SEARCH_TAG = 0,
    MF_OP_AUTH,
    MF_OP_WRITE,
    MF_OP_READ,
    MF_OP_DECREMENT,
    MF_OP_INCREMENT,
    MF_OP_RESTORE,
    MF_OP_TRANSFER,
    MF_OP_HALT,
    NUM_MF_OP
};

int ms_open_nfc_43a_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int ms_A3_RFID_43A_Inventory( tNALMsrComInstance  *dev, unsigned char rf_param );
int ms_A3_MFSearchTag( tNALMsrComInstance  *dev );
int ms_A3_RFID_43A_Inventory_Callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);

#endif


