/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*******************************************************************************
 *
 * Filename:
 * ---------
 *   radiomod.c
 *
 * Project:
 * --------
 *   MTK Bluetooth Chip
 *
 * Description:
 * ------------
 *   This file contains functions provide the service to Bluetooth Host
 *   make the operation of command and event of MTK Bluetooth chip
 *
 * Author:
 * -------
 *   CH Yeh (mtk01089)
 *
 *******************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <fcntl.h>

/* use nvram */
#include "CFG_BT_File.h"
#include "CFG_BT_Default.h"
#include "CFG_file_lid.h"
#include "libnvram.h"

#include "bt_kal.h"

#define BT_NVRAM_DATA_CLONE_FILE_NAME    "/data/BT_Addr"


typedef BtStatus (*sHciCmdFunc)( void );
typedef BtStatus (*sHciEvtFunc)( HciEvent * );

typedef union {
  ap_nvram_btradio_mt6610_struct fields;
  unsigned char raw[sizeof(ap_nvram_btradio_mt6610_struct)];
} sBT_nvram_data;

typedef struct {
  sHciCmdFunc command_func;
} sHciScriptElement;

typedef struct {
  short tagidx;
  short rawidx;
  sBT_nvram_data bt_nvram;
  sHciScriptElement* cur_script;
} sBTInitVar;

/**************************************************************************
 *              G L O B A L   V A R I A B L E S                           *
***************************************************************************/
//---------------------------------------------------------------------------

static HciCommand  hciCommand;     /* Structure for sending commands.  
                                    * Only one command can be sent at a time.
                                    */
static HciCommandType wOpCode;     /* Opcode of HCI command sent from init script
                                    */
//---------------------------------------------------------------------------

static HANDLE  bt_comm_port;
static sBTInitVar btinit[1];

//--------------------------------6620--------------------------------
#if defined MTK_MT6620
//--------------------------------6622--------------------------------                                     
#elif defined MTK_MT6622
#if BT_PATCH_EXT_ENABLE
static PATCH_SETTING patch_setting = {0x0, 0x0, 0x0, 
                                      0x0, 0x0}; //patch related setting if MT6622 also need patch ext RAM
#endif
//--------------------------------6626--------------------------------
#elif defined MTK_MT6626
#if BT_PATCH_EXT_ENABLE
static PATCH_SETTING patch_setting = {0x0, 0x0, 0x0, 
                                      0x0, 0x0}; //patch related setting if MT6626 also need patch ext RAM
#endif
//--------------------------------6628--------------------------------
#elif defined MTK_MT6628
#endif
//====================================================================

/**************************************************************************
 *              F U N C T I O N   D E C L A R A T I O N S                 *
***************************************************************************/

static BtStatus GORMcmd_HCC_RESET(void);

static BtStatus GORMcmd_HCC_Set_Local_BD_Addr(void);
static BtStatus GORMcmd_HCC_Set_CapID(void);
static BtStatus GORMcmd_HCC_Set_LinkKeyType(void);
static BtStatus GORMcmd_HCC_Set_UnitKey(void);
static BtStatus GORMcmd_HCC_Set_Encryption(void);
static BtStatus GORMcmd_HCC_Set_PinCodeType(void);
static BtStatus GORMcmd_HCC_Set_Voice(void);
static BtStatus GORMcmd_HCC_Set_PCM(void);
static BtStatus GORMcmd_HCC_Set_Radio(void);
static BtStatus GORMcmd_HCC_Set_Sleep_Timeout(void);
static BtStatus GORMcmd_HCC_Set_BT_FTR(void);
static BtStatus GORMcmd_HCC_Set_TX_Power_Offset(void);
static BtStatus GORMcmd_HCC_Set_ECLK(void);


static BtStatus GORMcmd_HCC_WakeUpChip(void);
static BtStatus GORMcmd_HCC_Read_Local_Version(void);
static BtStatus GORMcmd_HCC_Simulate_MT6612(void);
static BtStatus GORMcmd_HCC_Fix_UART_Escape(void);
static BtStatus GORMcmd_HCC_GetHwVersion(void);
static BtStatus GORMcmd_HCC_GetGormVersion(void);
static BtStatus GORMcmd_HCC_ChangeBaudRate(void);
#if BT_PATCH_EXT_ENABLE
static BtStatus GORMcmd_HCC_Set_Patch_Base_Ext(void);
static BtStatus GORMcmd_HCC_Set_Patch_Base(void);
static BtStatus GORMcmd_HCC_Reset_Patch_Len(void);
static BtStatus GORMcmd_HCC_WritePatch_ext(void);
#endif
static BtStatus GORMcmd_HCC_WritePatch(void);
static BtStatus GORMcmd_HCC_Set_Chip_Feature(void);
static BtStatus GORMcmd_HCC_Set_OSC_Info(void);
static BtStatus GORMcmd_HCC_Set_LPO_Info(void);
static BtStatus GORMcmd_HCC_Set_RF_Desense(void);
static BtStatus GORMcmd_HCC_Set_PTA(void);
static BtStatus GORMcmd_HCC_Enable_PTA(void);
static BtStatus GORMcmd_HCC_Set_WiFi_Ch(void);
static BtStatus GORMcmd_HCC_Set_AFH_Mask(void);
static BtStatus GORMcmd_HCC_Set_Sleep_Control_Reg(void);
static BtStatus GORMcmd_HCC_I2S_Switch(void);
static BtStatus GORMcmd_HCC_JTAG_PCM_Switch(void);


static BtStatus GORMcmd_HCC_Set_BLEPTA(void);
static BtStatus GORMcmd_HCC_Set_Internal_PTA_1(void);
static BtStatus GORMcmd_HCC_Set_Internal_PTA_2(void);
static BtStatus GORMcmd_HCC_Set_SLP_LDOD_VCTRL_Reg(void);
static BtStatus GORMcmd_HCC_Set_RF_Reg_100(void);
static BtStatus GORMcmd_HCC_Set_FW_Reg(DWORD, DWORD);


/* E-FUSE BD address mechanism */
static BOOL BT_Get_Local_BD_Addr(PUCHAR ucBDAddr);
static void GetRandomValue(UCHAR string[6]);
static int WriteBDAddrToNvram(PUCHAR ucBDAddr);


//===================================================================
#ifdef MTK_COMBO_SUPPORT
// Combo chip
sHciScriptElement bt_init_script[] = /*MT6620,MT6628*/
{    
    {  GORMcmd_HCC_Set_Local_BD_Addr       }, /*0xFC1A*/
    {  GORMcmd_HCC_Set_LinkKeyType         }, /*0xFC1B*/
    {  GORMcmd_HCC_Set_UnitKey             }, /*0xFC75*/
    {  GORMcmd_HCC_Set_Encryption          }, /*0xFC76*/
    {  GORMcmd_HCC_Set_PinCodeType         }, /*0x0C0A*/
    {  GORMcmd_HCC_Set_Voice               }, /*0x0C26*/
    {  GORMcmd_HCC_Set_PCM                 }, /*0xFC72*/
    {  GORMcmd_HCC_Set_Radio               }, /*0xFC79*/
    {  GORMcmd_HCC_Set_TX_Power_Offset     }, /*0xFC93*/
    /* MT6620 sleep mode doesn't support E1/E2  */
    {  GORMcmd_HCC_Set_Sleep_Timeout       }, /*0xFC7A*/
    {  GORMcmd_HCC_Set_BT_FTR              }, /*0xFC7D*/
    {  GORMcmd_HCC_Set_OSC_Info            }, /*0xFC7B*/
    {  GORMcmd_HCC_Set_LPO_Info            }, /*0xFC7C*/
    {  GORMcmd_HCC_Set_PTA                 }, /*0xFC74*/
    {  GORMcmd_HCC_Set_BLEPTA              }, /*0xFCFC*/
#ifdef MTK_MT6620
    {  GORMcmd_HCC_Set_RF_Desense          }, /*0xFC20*/
#endif
    {  GORMcmd_HCC_RESET                   }, /*0x0C03*/
    {  GORMcmd_HCC_Set_Internal_PTA_1      }, /*0xFCFB*/
    {  GORMcmd_HCC_Set_Internal_PTA_2      }, /*0xFCFB*/
#ifdef MTK_MT6620
    {  GORMcmd_HCC_Set_Sleep_Control_Reg   }, /*0xFCD0*/
    {  GORMcmd_HCC_Set_SLP_LDOD_VCTRL_Reg  }, /*0xFCD0*/
#endif
    {  GORMcmd_HCC_Set_RF_Reg_100          }, /*0xFCB0*/
    {  0  },
};
#else
// Standalone chip, MT661x is phased out
sHciScriptElement bt_init_script[] = /*MT6622,MT6626*/
{
#ifdef MTK_MT6626
    {  GORMcmd_HCC_WakeUpChip              }, /*0xFF*/
#endif
    {  GORMcmd_HCC_Read_Local_Version      }, /*0x1001*/
#ifdef MTK_MT6622
    {  GORMcmd_HCC_Simulate_MT6612         }, /*0xFCCC*/
#endif
    {  GORMcmd_HCC_Fix_UART_Escape         }, /*0xFCD0*/
    {  GORMcmd_HCC_GetHwVersion            }, /*0xFCD1*/
    {  GORMcmd_HCC_GetGormVersion          }, /*0xFCD1*/
    {  GORMcmd_HCC_ChangeBaudRate          }, /*0xFC77*/
    {  GORMcmd_HCC_WritePatch              },
    {  GORMcmd_HCC_Set_Local_BD_Addr       }, /*0xFC1A*/
#ifdef MTK_MT6622
    {  GORMcmd_HCC_Set_CapID               }, /*0xFC7F*/
#endif
    {  GORMcmd_HCC_Set_LinkKeyType         }, /*0xFC1B*/
    {  GORMcmd_HCC_Set_UnitKey             }, /*0xFC75*/
    {  GORMcmd_HCC_Set_Encryption          }, /*0xFC76*/
    {  GORMcmd_HCC_Set_PinCodeType         }, /*0x0C0A*/
    {  GORMcmd_HCC_Set_Voice               }, /*0x0C26*/
    {  GORMcmd_HCC_Set_PCM                 }, /*0xFC72*/
    {  GORMcmd_HCC_Set_Radio               }, /*0xFC79*/
    {  GORMcmd_HCC_Set_Sleep_Timeout       }, /*0xFC7A*/
    {  GORMcmd_HCC_Set_BT_FTR              }, /*0xFC7D*/
#ifdef MTK_MT6626
    {  GORMcmd_HCC_Set_ECLK                }, /*0xFCD0*/
#endif
    {  GORMcmd_HCC_Set_Chip_Feature        }, /*0xFC1E*/
    {  GORMcmd_HCC_Set_OSC_Info            }, /*0xFC7B*/
    {  GORMcmd_HCC_Set_LPO_Info            }, /*0xFC7C*/
    {  GORMcmd_HCC_Set_RF_Desense          }, /*0xFC20*/
    {  GORMcmd_HCC_Set_PTA                 }, /*0xFC74*/
    {  GORMcmd_HCC_RESET                   }, /*0x0C03*/
#ifdef MTK_MT6626
    {  GORMcmd_HCC_Set_CapID               }, /*0xFC1B*/
#endif
    {  GORMcmd_HCC_Enable_PTA              }, /*0xFCD2*/
    {  GORMcmd_HCC_Set_WiFi_Ch             }, /*0xFCD3*/
    {  GORMcmd_HCC_Set_AFH_Mask            }, /*0x0C3F*/
    {  GORMcmd_HCC_Set_Sleep_Control_Reg   }, /*0xFCD0*/
#ifdef MTK_MT6626
    {  GORMcmd_HCC_I2S_Switch              }, /*0xFC85*/
    {  GORMcmd_HCC_JTAG_PCM_Switch         }, /*0xFCD6*/
#endif
    {  0  },
};
#endif

/**************************************************************************
 *                         F U N C T I O N S                              *
***************************************************************************/

static BtStatus GORMcmd_HCC_Set_Local_BD_Addr(void)
{
    wOpCode = 0xFC1A;
    
    LOG_DBG("GORMcmd_HCC_Set_Local_BD_Addr\n");
    
    if (0 == memcmp(btinit->bt_nvram.fields.addr, stBtDefault.addr, 6))
    {
        LOG_WAN("Nvram BD address default value\n");
        
    #ifdef MTK_COMBO_SUPPORT
        /* Want to retrieve module eFUSE address on combo chip */
        BT_Get_Local_BD_Addr(btinit->bt_nvram.fields.addr);
        
        if (0 == memcmp(btinit->bt_nvram.fields.addr, stBtDefault.addr, 6)){
            LOG_WAN("eFUSE address default value\n");
        #ifdef DEV_VERSION 
            GetRandomValue(btinit->bt_nvram.fields.addr);
        #endif
        }
        else {
            LOG_WAN("eFUSE address has valid value\n");
        }
    #else
        #ifdef DEV_VERSION
        GetRandomValue(btinit->bt_nvram.fields.addr);
        #endif
    #endif

        // Save BD address to Nvram and /data/BD_Addr
        WriteBDAddrToNvram(btinit->bt_nvram.fields.addr);
    }
    else {
        LOG_WAN("Nvram BD address has valid value\n");
    }
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.addr[5];
    hciCommand.parms[1] = btinit->bt_nvram.fields.addr[4];
    hciCommand.parms[2] = btinit->bt_nvram.fields.addr[3];
    hciCommand.parms[3] = btinit->bt_nvram.fields.addr[2];
    hciCommand.parms[4] = btinit->bt_nvram.fields.addr[1];
    hciCommand.parms[5] = btinit->bt_nvram.fields.addr[0];
    
    LOG_WAN("Write BD address: %02x-%02x-%02x-%02x-%02x-%02x\n",
            hciCommand.parms[5], hciCommand.parms[4], hciCommand.parms[3],
            hciCommand.parms[2], hciCommand.parms[1], hciCommand.parms[0]);
    
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 6, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_LinkKeyType(void)
{
    wOpCode = 0xFC1B;

#ifdef MTK_COMBO_SUPPORT
    hciCommand.parms[0] = 0x01;   /*00: unit key; 01: combination key*/
#else
    hciCommand.parms[0] = btinit->bt_nvram.fields.LinkKeyType[0];
#endif

    LOG_DBG("GORMcmd_HCC_Set_LinkKeyType\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_UnitKey(void)
{
    wOpCode = 0xFC75;

#ifdef MTK_COMBO_SUPPORT
    hciCommand.parms[0] = 0x00;
    hciCommand.parms[1] = 0x00;
    hciCommand.parms[2] = 0x00;
    hciCommand.parms[3] = 0x00;
    hciCommand.parms[4] = 0x00;
    hciCommand.parms[5] = 0x00;
    hciCommand.parms[6] = 0x00;
    hciCommand.parms[7] = 0x00;
    hciCommand.parms[8] = 0x00;
    hciCommand.parms[9] = 0x00;
    hciCommand.parms[10] = 0x00;
    hciCommand.parms[11] = 0x00;
    hciCommand.parms[12] = 0x00;
    hciCommand.parms[13] = 0x00;
    hciCommand.parms[14] = 0x00;
    hciCommand.parms[15] = 0x00;
#else
    hciCommand.parms[0] = btinit->bt_nvram.fields.UintKey[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.UintKey[1];
    hciCommand.parms[2] = btinit->bt_nvram.fields.UintKey[2];
    hciCommand.parms[3] = btinit->bt_nvram.fields.UintKey[3];
    hciCommand.parms[4] = btinit->bt_nvram.fields.UintKey[4];
    hciCommand.parms[5] = btinit->bt_nvram.fields.UintKey[5];
    hciCommand.parms[6] = btinit->bt_nvram.fields.UintKey[6];
    hciCommand.parms[7] = btinit->bt_nvram.fields.UintKey[7];
    hciCommand.parms[8] = btinit->bt_nvram.fields.UintKey[8];
    hciCommand.parms[9] = btinit->bt_nvram.fields.UintKey[9];
    hciCommand.parms[10] = btinit->bt_nvram.fields.UintKey[10];
    hciCommand.parms[11] = btinit->bt_nvram.fields.UintKey[11];
    hciCommand.parms[12] = btinit->bt_nvram.fields.UintKey[12];
    hciCommand.parms[13] = btinit->bt_nvram.fields.UintKey[13];
    hciCommand.parms[14] = btinit->bt_nvram.fields.UintKey[14];
    hciCommand.parms[15] = btinit->bt_nvram.fields.UintKey[15];
#endif

    LOG_DBG("GORMcmd_HCC_Set_UnitKey\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 16, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Encryption(void)
{
    wOpCode = 0xFC76;

#ifdef MTK_COMBO_SUPPORT
    hciCommand.parms[0] = 0x00;
    hciCommand.parms[1] = 0x02;
    hciCommand.parms[2] = 0x10;
#else
    hciCommand.parms[0] = btinit->bt_nvram.fields.Encryption[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.Encryption[1];
    hciCommand.parms[2] = btinit->bt_nvram.fields.Encryption[2];
#endif

    LOG_DBG("GORMcmd_HCC_Set_Encryption\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 3, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_PinCodeType(void)
{
    wOpCode = 0x0C0A;

#ifdef MTK_COMBO_SUPPORT
    hciCommand.parms[0] = 0x00;   /*00: variable PIN; 01: Fixed PIN*/
#else
    hciCommand.parms[0] = btinit->bt_nvram.fields.PinCodeType[0];
#endif

    LOG_DBG("GORMcmd_HCC_Set_PinCodeType\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Voice(void)
{
    wOpCode = 0x0C26;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.Voice[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.Voice[1];
    
    LOG_DBG("GORMcmd_HCC_Set_Voice\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 2, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_PCM(void)
{
    wOpCode = 0xFC72;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.Codec[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.Codec[1];
    hciCommand.parms[2] = btinit->bt_nvram.fields.Codec[2];
    hciCommand.parms[3] = btinit->bt_nvram.fields.Codec[3];	
    
    LOG_DBG("GORMcmd_HCC_Set_PCM\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 4, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Radio(void)
{
    wOpCode = 0xFC79;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.Radio[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.Radio[1];
    hciCommand.parms[2] = btinit->bt_nvram.fields.Radio[2];
    hciCommand.parms[3] = btinit->bt_nvram.fields.Radio[3];
    hciCommand.parms[4] = btinit->bt_nvram.fields.Radio[4];
    hciCommand.parms[5] = btinit->bt_nvram.fields.Radio[5];
    
    LOG_DBG("GORMcmd_HCC_Set_Radio\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 6, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Sleep_Timeout(void)
{
    wOpCode = 0xFC7A;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.Sleep[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.Sleep[1];
    hciCommand.parms[2] = btinit->bt_nvram.fields.Sleep[2];
    hciCommand.parms[3] = btinit->bt_nvram.fields.Sleep[3];
    hciCommand.parms[4] = btinit->bt_nvram.fields.Sleep[4];
    hciCommand.parms[5] = btinit->bt_nvram.fields.Sleep[5];
    hciCommand.parms[6] = btinit->bt_nvram.fields.Sleep[6];
    
    LOG_DBG("GORMcmd_HCC_Set_Sleep_Timeout\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 7, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_BT_FTR(void)
{
    wOpCode = 0xFC7D;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.BtFTR[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.BtFTR[1];
    
    LOG_DBG("GORMcmd_HCC_Set_BT_FTR\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 2, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_OSC_Info(void)
{
    wOpCode = 0xFC7B;
    
    hciCommand.parms[0] = 0x01;     /* do not care on 6620 */
    hciCommand.parms[1] = 0x01;     /* do not care on 6620 */
    hciCommand.parms[2] = 0x14;     /* clock drift */
    hciCommand.parms[3] = 0x0A;     /* clock jitter */
#if defined MTK_MT6620
    hciCommand.parms[4] = 0x05;     /* OSC stable time */
#elif defined MTK_MT6628
    hciCommand.parms[4] = 0x08;     /* OSC stable time */
#else
    hciCommand.parms[4] = 0x06;     /* OSC stable time */
#endif

    LOG_DBG("GORMcmd_HCC_Set_OSC_Info\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 5, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_LPO_Info(void)
{
    wOpCode = 0xFC7C;
    
#ifdef MTK_MT6626
    hciCommand.parms[0] = 0x00;     /* LPO source = internal */
#else
    hciCommand.parms[0] = 0x01;     /* LPO source = external */
#endif
    hciCommand.parms[1] = 0xFA;     /* LPO clock drift = 250ppm */
    hciCommand.parms[2] = 0x0A;     /* LPO clock jitter = 10us */
    hciCommand.parms[3] = 0x02;     /* LPO calibration mode = manual mode */
    hciCommand.parms[4] = 0x00;     /* LPO calibration interval = 10 mins */
    hciCommand.parms[5] = 0xA6;
    hciCommand.parms[6] = 0x0E;
    hciCommand.parms[7] = 0x00;
    hciCommand.parms[8] = 0x40;     /* LPO calibration cycles = 64 */
    hciCommand.parms[9] = 0x00;
    
    LOG_DBG("GORMcmd_HCC_Set_LPO_Info\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 10, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_RF_Desense(void)
{
    wOpCode = 0xFC20;

#ifdef MTK_COMBO_SUPPORT
    hciCommand.parms[0] = 0x01;    /* DWA is off at Channel 30, others still ON */
    hciCommand.parms[1] = 0x00;    /* Turn Off RX Current Boost Function */
    hciCommand.parms[2] = 0x00;    /* Turn Off 0.8889 interface switch Function */
    hciCommand.parms[3] = 0x00;    /* Turn Off MPLL Hopping Function */
    hciCommand.parms[4] = 0x00;    /* Turn Off DC_Notch Function */
    hciCommand.parms[5] = 0x00;    /* Turn Off EMI_Hopping Function */
#else
    hciCommand.parms[0] = 0x00;    /* DWA is off at Channel 30, others still ON */
    hciCommand.parms[1] = 0x00;    /* Turn On RX Current Boost Function */
    hciCommand.parms[2] = 0x01;    /* Turn Off 0.8889 interface switch Function */
    hciCommand.parms[3] = 0x00;    /* Turn Off MPLL Hopping Function */
    hciCommand.parms[4] = 0x01;    /* Turn Off DC_Notch Function */
    hciCommand.parms[5] = 0x00;    /* Turn Off EMI_Hopping Function */
#endif

    LOG_DBG("GORMcmd_HCC_Set_RF_Desense\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 6, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_PTA(void)
{
    wOpCode = 0xFC74;
    
#ifdef MTK_COMBO_SUPPORT
    hciCommand.parms[0] = 0xC9;
    hciCommand.parms[1] = 0x8B;
    hciCommand.parms[2] = 0xBF;
    hciCommand.parms[3] = 0x00;
    hciCommand.parms[4] = 0x00;
    hciCommand.parms[5] = 0x52;
    hciCommand.parms[6] = 0x0E;
    hciCommand.parms[7] = 0x0E;
    hciCommand.parms[8] = 0x1F;
    hciCommand.parms[9] = 0x1B;
#else
    hciCommand.parms[0] = 0xCF;
    hciCommand.parms[1] = 0x8B;
    hciCommand.parms[2] = 0x1F;
    hciCommand.parms[3] = 0x04;
    hciCommand.parms[4] = 0x08;
    hciCommand.parms[5] = 0xA2;
    hciCommand.parms[6] = 0x62;
    hciCommand.parms[7] = 0x56;
    hciCommand.parms[8] = 0x07;
    hciCommand.parms[9] = 0x1B;
#endif 

    LOG_DBG("GORMcmd_HCC_Set_PTA\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 10, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Enable_PTA(void)
{
    wOpCode = 0xFCD2;
    hciCommand.parms[0] = 0x00;
    
    LOG_DBG("GORMcmd_HCC_Enable_PTA\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Sleep_Control_Reg(void)
{
    DWORD addr, val;
    
    LOG_DBG("GORMcmd_HCC_Set_Sleep_Control_Reg\n");

#ifdef MTK_COMBO_SUPPORT
    addr = 0x81010074;
    val = 0x000029E2;
#else
    addr = 0x80090074;
#if defined MTK_MT6622
    val = 0x00000A82;
#elif defined MTK_MT6626
    val = 0x00002AE2;
#endif
#endif

    return GORMcmd_HCC_Set_FW_Reg(addr, val);
}

static BtStatus GORMcmd_HCC_RESET(void)
{
    wOpCode = HCC_RESET;
    
    LOG_DBG("GORMcmd_HCC_RESET\n");
    
    if(BT_SendHciCommand(bt_comm_port, HCC_RESET, 0, NULL) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

#ifndef MTK_COMBO_SUPPORT
/* Implement in BT_WakeMagic */
static BtStatus GORMcmd_HCC_WakeUpChip(void)
{
    return BT_STATUS_NOT_SUPPORTED;
}

static BtStatus GORMcmd_HCC_Read_Local_Version(void)
{
    wOpCode = 0x1001;
    
    LOG_DBG("GORMcmd_HCC_Read_Local_Version\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 0, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

#ifdef MTK_MT6622
static BtStatus GORMcmd_HCC_Simulate_MT6612(void)
{
    wOpCode = 0xFCCC;
    hciCommand.parms[0] = 0x00; //disable
    
    LOG_DBG("GORMcmd_HCC_Simulate_MT6612\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}
#endif

static BtStatus GORMcmd_HCC_Fix_UART_Escape(void)
{
    wOpCode = 0xFCD0;
    
    hciCommand.parms[0] = 0x40;
    hciCommand.parms[1] = 0x00;
    hciCommand.parms[2] = 0x06;
    hciCommand.parms[3] = 0x80;
    hciCommand.parms[4] = 0x77;
    hciCommand.parms[5] = 0x00;
    hciCommand.parms[6] = 0x00;
    hciCommand.parms[7] = 0x00;
    
    LOG_DBG("GORMcmd_HCC_Fix_UART_Escape\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 8, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_GetHwVersion(void)
{
    wOpCode = 0xFCD1;
    
    hciCommand.parms[0] = 0x00;
    hciCommand.parms[1] = 0x00;
    hciCommand.parms[2] = 0x00;
    hciCommand.parms[3] = 0x80;
    
    LOG_DBG("GORMcmd_HCC_GetHwVersion\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 4, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_GetGormVersion(void)
{
    wOpCode = 0xFCD1;

    hciCommand.parms[0] = 0x04;
    hciCommand.parms[1] = 0x00;
    hciCommand.parms[2] = 0x00;
    hciCommand.parms[3] = 0x80;
    
    LOG_DBG("GORMcmd_HCC_GetGormVersion\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 4, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

/* Implement in BT_SetBaudRate */
static BtStatus GORMcmd_HCC_ChangeBaudRate(void)
{
    return BT_STATUS_NOT_SUPPORTED;
}

#if BT_PATCH_EXT_ENABLE
static BtStatus GORMcmd_HCC_Set_Patch_Base_Ext( void )
{
    LOG_DBG("GORMcmd_HCC_Set_Patch_Base_Ext\r\n");
    return GORMcmd_HCC_Set_FW_Reg(patch_setting.dwPatchAddr, patch_setting.dwPatchExtVal);
}

static BtStatus GORMcmd_HCC_Set_Patch_Base( void )
{
    LOG_DBG("GORMcmd_HCC_Set_Patch_Base\r\n");
    return GORMcmd_HCC_Set_FW_Reg(patch_setting.dwPatchAddr, patch_setting.dwPatchBaseVal);
}

static BtStatus GORMcmd_HCC_Reset_Patch_Len( void )
{
    LOG_DBG("GORMcmd_HCC_Reset_Patch_Len\r\n");
    return GORMcmd_HCC_Set_FW_Reg(patch_setting.dwPatchLenResetAddr, patch_setting.dwPatchLenResetVal);
}

/* Implement in BT_DownPatch */
static BtStatus GORMcmd_HCC_WritePatch_ext( void )
{
    return BT_STATUS_NOT_SUPPORTED;    
}
#endif

/* Implement in BT_DownPatch */
static BtStatus GORMcmd_HCC_WritePatch(void)
{
    return BT_STATUS_NOT_SUPPORTED;
}

static BtStatus GORMcmd_HCC_Set_CapID(void)
{
    wOpCode = 0xFC7F;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.CapId[0];
#ifdef MTK_MT6626
    if(0xFF == btinit->bt_nvram.fields.CapId[0]){
        hciCommand.parms[0] = 0x40;
    }
#endif

    LOG_DBG("GORMcmd_HCC_Set_CapID\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

#ifdef MTK_MT6626
static BtStatus GORMcmd_HCC_Set_ECLK(void)
{
    wOpCode = 0xFCD0;
    
    hciCommand.parms[0] = 0x04;
    hciCommand.parms[1] = 0x07;
    hciCommand.parms[2] = 0x00;
    hciCommand.parms[3] = 0x80;
    hciCommand.parms[4] = ((btinit->bt_nvram.raw[52] & 0x1) << 0x1) | 0x1;   /*(nvram value ECLK_SEL & 0x1<<0x1)|0x1*/
    hciCommand.parms[5] = 0x00;
    hciCommand.parms[6] = 0x00;
    hciCommand.parms[7] = 0x00;
    
    LOG_DBG("GORMcmd_HCC_Set_ECLK [%02X]\r\n", hciCommand.parms[4]);
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 8, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}
#endif

static BtStatus GORMcmd_HCC_Set_Chip_Feature(void)
{
    wOpCode = 0xFC1E;
    
    hciCommand.parms[0] = 0xBF;
    hciCommand.parms[1] = 0xFE;
    hciCommand.parms[2] = 0x8D;
    hciCommand.parms[3] = 0xFE;
    hciCommand.parms[4] = 0x98;
    hciCommand.parms[5] = 0x1F;
    hciCommand.parms[6] = 0x59;
    hciCommand.parms[7] = 0x87;
    
    LOG_DBG("GORMcmd_HCC_Set_Chip_Feature\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 8, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_WiFi_Ch(void)
{
    wOpCode = 0xFCD3;
    
    hciCommand.parms[0] = 0x00;
    hciCommand.parms[1] = 0x0A;   // HB = 0x3c
    
    LOG_DBG("GORMcmd_HCC_Set_WiFi_Ch\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 2, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_AFH_Mask(void)
{
    wOpCode = 0x0C3F;
    
    hciCommand.parms[0] = 0xFF;
    hciCommand.parms[1] = 0xFF;
    hciCommand.parms[2] = 0xFF;
    hciCommand.parms[3] = 0xFF;
    hciCommand.parms[4] = 0xFF;
    hciCommand.parms[5] = 0xFF;
    hciCommand.parms[6] = 0xFF;
    hciCommand.parms[7] = 0xFF;
    hciCommand.parms[8] = 0xFF;
    hciCommand.parms[9] = 0x7F;
    
    LOG_DBG("GORMcmd_HCC_Set_AFH_Mask\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 10, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

#ifdef MTK_MT6626
static BtStatus GORMcmd_HCC_I2S_Switch(void)
{
    wOpCode = 0xFC85;
    hciCommand.parms[0] = 0x02;
    
    LOG_DBG("GORMcmd_HCC_I2S_Switch\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_JTAG_PCM_Switch(void)
{
    wOpCode = 0xFCD6;
    hciCommand.parms[0] = 0x01;
    
    LOG_DBG("GORMcmd_HCC_JTAG_PCM_Switch\r\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 1, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}
#endif
#endif

#ifdef MTK_COMBO_SUPPORT
static BtStatus GORMcmd_HCC_Set_TX_Power_Offset(void)
{
    wOpCode = 0xFC93;
    
    hciCommand.parms[0] = btinit->bt_nvram.fields.TxPWOffset[0];
    hciCommand.parms[1] = btinit->bt_nvram.fields.TxPWOffset[1];
    hciCommand.parms[2] = btinit->bt_nvram.fields.TxPWOffset[2];
    
    LOG_DBG("GORMcmd_HCC_Set_TX_Power_Offset\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 3, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_BLEPTA(void)
{
    wOpCode = 0xFCFC;
    
    hciCommand.parms[0] = 0x16;	
    hciCommand.parms[1] = 0x0E;	
    hciCommand.parms[2] = 0x0E;	
    hciCommand.parms[3] = 0x00; 
    hciCommand.parms[4] = 0x07; 
    
    LOG_DBG("GORMcmd_HCC_Set_BLEPTA\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 5, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Internal_PTA_1(void)
{
    wOpCode = 0xFCFB;   
    
    hciCommand.parms[0] = 0x00;
    hciCommand.parms[1] = 0x01;	    /* bt_pta_high_level_tx */
    hciCommand.parms[2] = 0x0F;	    /* bt_pta_mid_level_tx */
    hciCommand.parms[3] = 0x0F;	    /* bt_pta_low_level_tx */
    hciCommand.parms[4] = 0x01;	    /* bt_pta_high_level_rx */
    hciCommand.parms[5] = 0x0F;	    /* bt_pta_mid_level_rx */
    hciCommand.parms[6] = 0x0F;	    /* bt_pta_low_level_rx */
    hciCommand.parms[7] = 0x01;	    /* ble_pta_high_level_tx */
    hciCommand.parms[8] = 0x0F;	    /* ble_pta_mid_level_tx */
    hciCommand.parms[9] = 0x0F;	    /* ble_pta_low_level_tx */
    hciCommand.parms[10] = 0x01;    /* ble_pta_high_level_rx */
    hciCommand.parms[11] = 0x0F;    /* ble_pta_mid_level_rx */
    hciCommand.parms[12] = 0x0F;    /* ble_pta_low_level_rx */
    hciCommand.parms[13] = 0x02;    /* time_r2g */
    hciCommand.parms[14] = 0x01;    /* always  0x1 */
    
    LOG_DBG("GORMcmd_HCC_Set_Internal_PTA_1\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 15, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

static BtStatus GORMcmd_HCC_Set_Internal_PTA_2(void)
{
    wOpCode = 0xFCFB;   
     
    hciCommand.parms[0] = 0x01;		
    hciCommand.parms[1] = 0x19;     /* wifi20_hb */
    hciCommand.parms[2] = 0x19;     /* wifi20_hb */
    hciCommand.parms[3] = 0x07;     /* next_rssi_update_bt_slots */
    hciCommand.parms[4] = 0xD0;     /* next_rssi_update_bt_slots */
    hciCommand.parms[5] = 0x00;     /* stream_identify_by_host */
    hciCommand.parms[6] = 0x01;     /* enable auto AFH */
    
    LOG_DBG("GORMcmd_HCC_Set_Internal_PTA_2\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 7, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}

#ifdef MTK_MT6620
static BtStatus GORMcmd_HCC_Set_SLP_LDOD_VCTRL_Reg(void)
{
    DWORD addr, val;
    
    LOG_DBG("GORMcmd_HCC_Set_SLP_LDOD_VCTRL_Reg\r\n");
    
    addr = 0x8102001C;
    val = 0x00000879;

    return GORMcmd_HCC_Set_FW_Reg(addr, val);
}
#endif

static BtStatus GORMcmd_HCC_Set_RF_Reg_100(void)
{
    wOpCode = 0xFCB0;
    
    hciCommand.parms[0] = 0x64;	
    hciCommand.parms[1] = 0x01;
    hciCommand.parms[2] = 0x02;
    hciCommand.parms[3] = 0x00;
    hciCommand.parms[4] = 0x00;
    hciCommand.parms[5] = 0x00;
    
    LOG_DBG("GORMcmd_HCC_Set_RF_Reg_100\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 6, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}
#endif


static BtStatus GORMcmd_HCC_Set_FW_Reg(DWORD dwAddr, DWORD dwVal)
{
    wOpCode = 0xFCD0;
    
    hciCommand.parms[0] = (UCHAR)((dwAddr) & 0xFF);		
    hciCommand.parms[1] = (UCHAR)((dwAddr >> 8) & 0xFF);		
    hciCommand.parms[2] = (UCHAR)((dwAddr >> 16) & 0xFF);
    hciCommand.parms[3] = (UCHAR)((dwAddr >> 24) & 0xFF);		
    hciCommand.parms[4] = (UCHAR)((dwVal) & 0xFF);		
    hciCommand.parms[5] = (UCHAR)((dwVal >> 8) & 0xFF);		
    hciCommand.parms[6] = (UCHAR)((dwVal >> 16) & 0xFF);
    hciCommand.parms[7] = (UCHAR)((dwVal >> 24) & 0xFF);		
    
    LOG_DBG("GORMcmd_HCC_Set_FW_Reg\n");
    
    if(BT_SendHciCommand(bt_comm_port, wOpCode, 8, &hciCommand) == TRUE){
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}


DWORD GORM_Init(
    HANDLE  hComFile,
    PUCHAR  pucPatchExtData,
    DWORD   dwPatchExtLen,
    PUCHAR  pucPatchData,
    DWORD   dwPatchLen,
    PBYTE   ucNvRamData,
    DWORD   dwBaud,
    DWORD   dwHostBaud,
    DWORD   dwUseFlowControl
    )
{
    int  i = 0;
    BtStatus btStatus = BT_STATUS_SUCCESS;
    UCHAR    ucEventBuf[MAX_EVENT_SIZE];
    DWORD    dwEventLen;
    
    LOG_DBG("GORM_Init\n");
    
    // Save comm port fd for GORMcmd use
    bt_comm_port = hComFile;
    
    // Copy nvram data
    memcpy(btinit->bt_nvram.raw, ucNvRamData, sizeof(ap_nvram_btradio_mt6610_struct));
    
    // General init script
    btinit->cur_script = bt_init_script;
    
    /* Can not find matching script, simply skip */
    if((btinit->cur_script) == NULL){
        LOG_ERR("No matching init script\n");
        btStatus = BT_STATUS_FAILED;
        return btStatus;
    }
    
    i = 0;
    while(btinit->cur_script[i].command_func){
    
#ifndef MTK_COMBO_SUPPORT
        if(GORMcmd_HCC_ChangeBaudRate == btinit->cur_script[i].command_func)
        {
            if(BT_SetBaudRate(hComFile, dwBaud, dwHostBaud, dwUseFlowControl) == FALSE){
                LOG_ERR("Change baud rate fails\n");
                btStatus = BT_STATUS_FAILED;
                return btStatus;
            }            
            i ++;
            continue;
        }
		
    #if BT_PATCH_EXT_ENABLE
        if(GORMcmd_HCC_WritePatch_ext == btinit->cur_script[i].command_func)
        {
            if((pucPatchExtData == NULL) || (dwPatchExtLen == 0)){
                LOG_ERR("No valid patch ext data!\n");
            }
            else{
                if(BT_DownPatch(hComFile, pucPatchExtData, dwPatchExtLen) == FALSE){
                    LOG_ERR("Download patch ext fails\n");
                    btStatus = BT_STATUS_FAILED;
                    return btStatus;
                }
            }
            i ++;
            continue;
        }
    #endif

        if(GORMcmd_HCC_WritePatch == btinit->cur_script[i].command_func)
        {
            if((pucPatchData == NULL) || (dwPatchLen == 0)){
                LOG_ERR("No valid patch data\n");
            }
            else{
                if(BT_DownPatch(hComFile, pucPatchData, dwPatchLen) == FALSE){
                    LOG_ERR("Download patch fails\n");
                    btStatus = BT_STATUS_FAILED;
                    return btStatus;
                }
            }
            i ++;
            continue;
        }
        
        if(GORMcmd_HCC_WakeUpChip == btinit->cur_script[i].command_func)
        {
            if(BT_WakeMagic(hComFile, TRUE) == FALSE){		         
                btStatus = BT_STATUS_FAILED;
                return btStatus;
            }
            i++;
            continue;
        }	
#endif

        btStatus = btinit->cur_script[i].command_func();
        
        if(btStatus == BT_STATUS_CANCELLED){
            i ++;
            continue;//skip
        }
        
        if(btStatus == BT_STATUS_FAILED){
            LOG_ERR("Command %d fails\n", i);
            return btStatus;
        }

        if(BT_ReadExpectedEvent(
            hComFile, 
            ucEventBuf, 
            MAX_EVENT_SIZE, 
            0x0E, 
            &dwEventLen, 
            TRUE, 
            wOpCode, 
            TRUE, 
            0x00) == FALSE){
             
            LOG_ERR("Read event of command %d fails\n", i);
            btStatus = BT_STATUS_FAILED;
            return btStatus;
        }
        
        i ++;
    }
    
    return btStatus;
}


static BOOL BT_Get_Local_BD_Addr(PUCHAR ucBDAddr)
{
    LOG_DBG("BT_Get_Local_BD_Addr\n");
    
    USHORT  opCode = 0x1009;
    DWORD   dwReadLength = 0;
    UCHAR   pAckEvent[20];
    
    if(BT_SendHciCommand(bt_comm_port, opCode, 0, NULL) == FALSE){
        LOG_ERR("Write get BD address command fails\n");
        return FALSE;
    }
    
    // Read local BD addr in firmware
    if(BT_ReadExpectedEvent(
        bt_comm_port, 
        pAckEvent, 
        sizeof(pAckEvent),
        0x0E,
        &dwReadLength,
        TRUE,
        0x1009,
        TRUE,
        0x0) == FALSE){
        
        LOG_ERR("Read local BD address fails\n");
        return FALSE;
    }
    
    LOG_WAN("Local BD address: %02x-%02x-%02x-%02x-%02x-%02x\n",
            pAckEvent[12], pAckEvent[11], pAckEvent[10], pAckEvent[9], pAckEvent[8], pAckEvent[7]);
    
    ucBDAddr[0] = pAckEvent[12];
    ucBDAddr[1] = pAckEvent[11];
    ucBDAddr[2] = pAckEvent[10];
    ucBDAddr[3] = pAckEvent[9];
    ucBDAddr[4] = pAckEvent[8];
    ucBDAddr[5] = pAckEvent[7];
    
    return TRUE;
}

static void GetRandomValue(UCHAR string[6])
{
    int iRandom = 0;
    int fd = 0;
    unsigned long seed;
    
    LOG_WAN("Enable random generation\n");
    
    /* initialize random seed */
    srand (time(NULL));
    iRandom = rand();
    LOG_WAN("iRandom = [%d]", iRandom);
    string[0] = (((iRandom>>24|iRandom>>16) & (0xFE)) | (0x02));
    
    /* second seed */
    struct timeval tv;
    gettimeofday(&tv, NULL);
    srand (tv.tv_usec);
    iRandom = rand();
    LOG_WAN("iRandom = [%d]", iRandom);
    string[1] = ((iRandom>>8) & 0xFF);
    
    /* third seed */
    fd = open("/dev/urandom", O_RDONLY);
    if (fd > 0){
        if (read(fd, &seed, sizeof(unsigned long)) > 0){
            srand (seed);
            iRandom = rand();
        }
        close(fd);
    }
    LOG_WAN("iRandom = [%d]", iRandom);
    string[5] = (iRandom & 0xFF);
    
    return;
}

static int WriteBDAddrToNvram(PUCHAR ucBDAddr)
{
    F_ID bt_nvram_fd = {0};
    int rec_size = 0;
    int rec_num = 0;
    int bt_cfgfile_fd = -1;
    
    bt_cfgfile_fd = open(BT_NVRAM_DATA_CLONE_FILE_NAME, O_WRONLY);
    if(bt_cfgfile_fd < 0){
        LOG_ERR("Can't open config file %s\n", BT_NVRAM_DATA_CLONE_FILE_NAME);
    }
    else{
        lseek(bt_cfgfile_fd, 0, 0);
        write(bt_cfgfile_fd, ucBDAddr, 6);
        close(bt_cfgfile_fd);
    }
    
    bt_nvram_fd = NVM_GetFileDesc(AP_CFG_RDEB_FILE_BT_ADDR_LID, &rec_size, &rec_num, ISWRITE);
    if(bt_nvram_fd.iFileDesc < 0){
        LOG_ERR("Open BT NVRAM fails errno %d\n", errno);
        return -1;
    }
    
    if(rec_num != 1){
        LOG_ERR("Unexpected record num %d\n", rec_num);
        NVM_CloseFileDesc(bt_nvram_fd);
        return -1;
    }
    
    if(rec_size != sizeof(ap_nvram_btradio_mt6610_struct)){
        LOG_ERR("Unexpected record size %d ap_nvram_btradio_mt6610_struct %d\n",
                 rec_size, sizeof(ap_nvram_btradio_mt6610_struct));
        NVM_CloseFileDesc(bt_nvram_fd);
        return -1;
    }
    
    lseek(bt_nvram_fd.iFileDesc, 0, 0);
    
    /* Update BD address */
    if (write(bt_nvram_fd.iFileDesc, ucBDAddr, 6) < 0){
        LOG_ERR("Write BT NVRAM fails errno %d\n", errno);
        NVM_CloseFileDesc(bt_nvram_fd);
        return -1;
    }

    NVM_CloseFileDesc(bt_nvram_fd);
    return  0;
}
