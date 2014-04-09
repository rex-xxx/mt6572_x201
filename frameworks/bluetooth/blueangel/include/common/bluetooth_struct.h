/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2005
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

/*****************************************************************************
 *
 * Filename:
 * ---------
 *   bluetooth_struct.h
 *
 * Project:
 * --------
 *   Maui_Software
 *
 * Description:
 * ------------
 *   This file is defines SAP for MTK Bluetooth.
 *
 * Author:
 * -------
 *   Max Chen
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: #4 $
 * $Modtime$
 * $Log$
 *
 * 04 24 2012 dexiang.jiang
 * [ALPS00270904] [ICS TDD][BT] happen NE at system/bin/mtkbt
 * Use special req_id(large value) to pervent multi obex-based profiles conexistence conflict.
 *
 * 10 24 2011 sh.lai
 * [ALPS00080967] [Need Patch] [Volunteer Patch] Integrate DUAL PCM set/switch interface.
 * Expand value parameter from 1 byte to max 32 bytes.
 *
 * 12 09 2010 sh.lai
 * [ALPS00027747] [Need Patch] [Volunteer Patch] Enable BT task to log into a file.
 * .
 *
 * 12 06 2010 autumn.li
 * [ALPS00025445] [BT] SSP Debug mode
 * SSP debug mode
 *
 * 10 30 2010 sh.lai
 * [ALPS00131769] [BWCS] enable BWCS on Android plus
 * .
 *
 * 10 30 2010 sh.lai
 * [ALPS00131769] [BWCS] enable BWCS on Android plus
 * .
 *
 * 09 03 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integration BT solution.
 *
 * 09 01 2010 sh.lai
 * NULL
 * Integration change. into 1036OF
 *
 * Jun 11 2009 MTK01174
 * [MAUI_01674439] [Engineer Mode] [Modify] [BT] Move the bluetooth chip ID table to custom folder
 * EM SAP modification: pass version string instead of enum
 *
 * Mar 19 2009 mtk80049
 * [MAUI_01410827] remove the old pbap code from 09.13 on 08B,09A, Maui
 *
 *
 * Feb 26 2009 mtk80049
 * [MAUI_01401495] PBAP issue5
 *
 *
 * Feb 20 2009 mtk80049
 * [MAUI_01398653] [BT][PBAP] PBAP Revised Code check in
 *
 *
 * Jan 22 2009 mtk02126
 * [MAUI_01320514] check in __BTMODULE_MT6612__ compile option
 *
 *
 * Jan 15 2009 mtk80049
 * [MAUI_01534098] Fails to delete image type *bmp from PC.
 *
 *
 * Dec 26 2008 mtk01933
 * [MAUI_01284868] [WNDRV][New] Add WiFi MT5921 driver code
 * Add PTA channel information function
 *
 * Dec 2 2008 mtk02126
 * [MAUI_01288486] chech in SPP/DUN new architecture
 *
 *
 * Dec 1 2008 mtk02126
 * [MAUI_01288486] chech in SPP/DUN new architecture
 *
 *
 * Sep 23 2008 mbj06032
 * [MAUI_01116233] [BT] Check received DCFs: if no Rights info, delete it
 *
 *
 * Sep 8 2008 mbj06032
 * [MAUI_01111918] [BT]The prompt is abnormal.
 * add BIP error code
 *
 * Sep 2 2008 mbj06032
 * [MAUI_01106205] [BT GOEP] Send OPP Client SDP result (OPP server's supported format list) to MMI
 *
 *
 * Aug 10 2008 mbj07029
 * [MAUI_00792520] BT-device power off but can connectPBAP
 *
 *
 * Jul 19 2008 mtk00560
 * [MAUI_00804575] [BT] to add sequence numer for opening HF virtual port and using SBC encoder
 *
 *
 * Jul 18 2008 mbj06032
 * [MAUI_01091714] [BT BIP] server show connect confirm when RFCOMM Open Indication
 * modify SAP between BIP ADP & BIP MMI : add server authorize msg
 *
 * Jul 15 2008 mtk01411
 * [MAUI_00804954] [Bluetooth] Support Read RSSI value request from MMI
 *
 *
 * Jul 9 2008 mtk01411
 * [MAUI_00790838] [Bluetooth] Revise BT code and format it.
 *
 *
 * Jul 2 2008 mbj06032
 * [MAUI_01171562] MTK:Bluetooth OBEX_it can't connect FTP succeed
 *
 *
 * Jun 28 2008 mbj06053
 * [MAUI_00793305] BT-always stay"Getting printer capabilities"
 *
 *
 * Jun 16 2008 mbj06053
 * [MAUI_01070863] BPP revise check in
 *
 *
 * Jun 12 2008 mbj06053
 * [MAUI_01070863] BPP revise check in
 *
 *
 * Jun 11 2008 mbj06053
 * [MAUI_01070863] BPP revise check in
 *
 *
 * Jun 2 2008 mbj06032
 * [MAUI_01144797] MTK:Bluetooth_the popup message not play tone
 *
 *
 * May 29 2008 mbj06032
 * [MAUI_01145940] MTK:Bluetooth_can't connect BIP
 * add BIP error code
 *
 * May 26 2008 mbj06032
 * [MAUI_01047668] [BT BIP] code revise
 * add "data_len" member to bip_get_image_cnf msg
 *
 * May 22 2008 mbj06032
 * [MAUI_00991010] MTK:Bluetooth BIP_can't disconnect BIP
 * BIP: add session role member(RESPONDER or INITIATOR) to DISCONNECT_IND msg & DISCONNECT_REQ msg
 *
 * May 18 2008 mbj07029
 * [MAUI_01041172] [PBAP]authentication check in
 *
 *
 * May 14 2008 mtk01239
 * [MAUI_00772282] [Bluetooth] Bluetooth EM for UPF testing
 *
 *
 * May 14 2008 mtk01239
 * [MAUI_00771864] [Bluetooth] BT 2.1 new feature TW side
 *
 *
 * Apr 28 2008 mbj07029
 * [MAUI_00672285] [PBAP]Call number shows abnormal on the earphone side while PBAP connected.
 *
 *
 * Apr 18 2008 mbj07029
 * [MAUI_00664265] [Phonebook][PBAP] pbap profile new feature check in
 *
 *
 * Apr 18 2008 mbj06032
 * [MAUI_00241227] MTK:Bluetooth FTP_the file name display incompletely and can't get
 *
 *
 * Mar 25 2008 mbj06032
 * [MAUI_00285864] the both popup message different
 *
 *
 * Mar 24 2008 mtk01411
 * [MAUI_00737053] Change MAUI_00732473 Tiltle from Chinse description to BT assert when ME is in engin
 *
 *
 * Mar 17 2008 mtk01411
 * [MAUI_00732473] BT Assert when ME is in engineer mode
 *
 *
 * Feb 19 2008 mtk01411
 * [MAUI_00621026] [Bluetooth] Check in JSR82 codes
 *
 *
 * Jan 9 2008 mbj06053
 * [MAUI_00068499] [Simple Push] Print failed pop up appears.
 *
 *
 * Dec 30 2007 mbj06053
 * [MAUI_00511488] BPP Simple push new feature check in
 *
 *
 * Dec 29 2007 mbj06053
 * [MAUI_00511488] BPP Simple push new feature check in
 *
 *
 * Dec 28 2007 mbj06053
 * [MAUI_00511488] BPP Simple push new feature check in
 *
 *
 * Dec 3 2007 mtk00511
 * [MAUI_00481330] [BT]Should remind "Disk Full"
 *
 *
 * Oct 22 2007 mtk00511
 * [MAUI_00483634] [BT]It always display "Send (0%)"
 *
 *
 * Oct 22 2007 mtk00511
 * [MAUI_00481867] [BT]The sum of all files display error
 *
 *
 * Oct 22 2007 mtk01239
 * [MAUI_00563448] [Bluetooth] patch BT code.
 *
 *
 * Oct 22 2007 mtk01239
 * [MAUI_00563448] [Bluetooth] patch BT code.
 *
 *
 * Oct 22 2007 mtk01239
 * [MAUI_00563448] [Bluetooth] patch BT code.
 *
 *
 * Oct 15 2007 mtk00511
 * [MAUI_00559539] BT-Screen always in "Connecting "
 *
 *
 * Sep 28 2007 mbj06032
 * [MAUI_00470068] [BT][GOEP] when GET/PUT failed, tell application the response code from server
 * add struct member to some GOEP SAP
 *
 * Sep 16 2007 mtk01411
 * [MAUI_00548801] [Bluetooth] Add Read RSSI value for SPP SCO feature
 *
 *
 * Sep 14 2007 mtk00511
 * [MAUI_00469578] [BT] [BIP] ( For TW ) Check in
 *
 *
 * Sep 12 2007 mtk01239
 * [MAUI_00546740] [Bluetooth] Checkin JSR82 code
 *
 *
 * Sep 3 2007 mbj06032
 * [MAUI_00459090] [BT]Can't connect Dongle with FTP profile
 * define GOEP_STATUS_SCO_REJECT
 *
 * Jul 16 2007 mtk01239
 * [MAUI_00417454] [Bluetooth][MT6601] Check in BT2.0 patch
 *
 *
 * Jul 16 2007 mtk01239
 * [MAUI_00417220] [EM] Bluetooth RF Test Item Support
 *
 *
 * Jul 15 2007 mtk01411
 * [MAUI_00416972] [Bluetooth] SPP with SCO functionality
 *
 *
 * Jul 10 2007 MTK01476
 * [MAUI_00235389] BPP_press cancel, it can not return to normal
 *
 *
 * May 24 2007 MTK01476
 * [MAUI_00394809] BT-"Another print Job is Printing" pop up
 *
 *
 * May 21 2007 mbj06032
 * [MAUI_00431668] GOEP authetication practice
 * add code for GOEP practice. Compile option is GOEP_AUTH
 *
 * May 14 2007 mtk01411
 * [MAUI_00392241] [Bluetooth] Add two fields for test cmd struct in bluetooth_struct.h
 *
 *
 * May 8 2007 MTK01476
 * [MAUI_00233014] MTK BPP_print picture fail
 *
 *
 * May 7 2007 mtk01239
 * [MAUI_00389758] [Bluetooth] Checkin JSR82 code of BTStack
 *
 *
 * Apr 25 2007 MTK01476
 * [MAUI_00381908] [New Feature]Bluetooth Basic Printing Profile check in
 *
 *
 * Apr 23 2007 mtk01411
 * [MAUI_00384849] [Bluetooth SPP Client] SPP Client and number of virtual ports modification
 *
 *
 * Apr 16 2007 MTK01476
 * [MAUI_00381908] [New Feature]Bluetooth Basic Printing Profile check in
 *
 *
 * Jan 16 2007 mtk01411
 * [MAUI_00356818] [1] Fatal Error: msg_send_ext_queue () failed 1=305 2=88880026 - L4
 *
 *
 * Jan 15 2007 mtk01411
 * [MAUI_00357847] SAP_It should not connect SAP profile when MS is in call
 *
 *
 * Dec 3 2006 mtk01411
 * [MAUI_00347961] Add Bluetooth SIM Access Profile related codes
 * Add struct for SIM Access Profile
 *
 * Dec 3 2006 mtk01411
 * [MAUI_00347950] Add Bluetooth Fax Profile related codes
 * Add struct for Fax Profile's SAP
 *
 * Nov 13 2006 mtk00511
 * [MAUI_00343002] [GOEP] Add OBEX authentication support
 *
 *
 * Nov 4 2006 mtk01239
 * [MAUI_00340620] [Bluetooth] patch for Wifi coexistence turn AFH and PTA
 *
 *
 * Oct 23 2006 mtk00511
 * [MAUI_00336114] [1] Fatal Error (305): msg_send_ext_queue() failed - SYSTEM H
 *
 *
 * Oct 3 2006 mtk00511
 * [MAUI_00332583] [1]Assert fail:0 BTMMIFtp.c 1514-MMI
 *
 *
 * Sep 18 2006 mtk00511
 * [MAUI_00330296] [GOEP]Fix OPP IOT issue with Widcomm
 *
 *
 * Sep 17 2006 mtk00511
 * [MAUI_00330296] [GOEP]Fix OPP IOT issue with Widcomm
 *
 *
 * Sep 11 2006 mtk00511
 * [MAUI_00329160] [GOEP]Check in GOEP profile and OBEX stack
 *
 *
 * Sep 3 2006 MTK00758
 * [MAUI_00326545] [BT][Factory mode]Check in
 *
 *
 * Jul 24 2006 mtk01411
 * [MAUI_00212512] [BT]Add Test module codes
 * Add new msg type struct
 *
 * Jun 19 2006 mtk00511
 * [MAUI_00203742] [AT][NewFeature]command for read BT address
 *
 *
 * May 26 2006 MTK00758
 * [MAUI_00197646] [BT][ESI]Add SPP/DUN MMI adaptor code
 *
 *
 * May 10 2006 MTK00758
 * [MAUI_00192509] [BT]Add DUN interface
 *
 *
 * May 1 2006 MTK00758
 * [MAUI_00191069] [New feature]Modify L4C and MTK Bluetooth interface to common interface
 *
 *
 * Apr 24 2006 mtk01239
 * [MAUI_00189553] ESI Bluetooth project update
 *
 *
 * Apr 17 2006 MTK00758
 * [MAUI_00188141] [Bluetooth]Power on/off and reset procedure
 *
 *
 * Apr 3 2006 mtk00758
 * [MAUI_00184485] [New feature] Add task to simulate UART driver for unitest
 *
 *
 * Mar 11 2006 mtk00758
 * [MAUI_00178684] [Bluetooth]ESI bluetooth protocol stack
 *
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __BLUETOOTH_STRUCT_H_
#define __BLUETOOTH_STRUCT_H_

#ifdef BTMTK_ON_WISE
#include "uart_sw.h"
//#include "xatypes.h" /* for type S8 */
#include "bluetooth_spp_struct.h"
#include "bluetooth_simap_struct.h"
#endif  /* BTMTK_ON_WISE */

#include "bt_mmi.h"
#include "bt_types.h"

#define HFG_SEQUENCE_NUM_LENGHT 16
#define BT_MAX_VERSION_STR_SIZE 20

/* Common */
typedef struct
{
    kal_uint32 lap;
    kal_uint8 uap;
    kal_uint16 nap;
} bt_addr_struct;

#include "bluetooth_avrcp_struct.h"

/* MSG_ID_BT_SPP_CONNECT_IND */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    UART_PORT port; /* virtual port number */
	kal_uint8    seq1[HFG_SEQUENCE_NUM_LENGHT];
	kal_uint8    seq2[HFG_SEQUENCE_NUM_LENGHT];

} bt_hf_connect_ind_struct;

/* MSG_ID_BT_HFG_SEND_DATA_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    UART_PORT port; /* virtual port number */
} bt_hf_send_data_req_struct;

/* MSG_ID_BT_POWERON_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;    /* TRUE: successful, FALSE: failed */
} bt_poweron_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;    /* TRUE: successful, FALSE: failed */
} bt_engineer_mode_poweron_cnf_struct;

/* MSG_ID_BT_POWEROFF_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;    /* TRUE: successful, FALSE: failed */
} bt_poweroff_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    kal_bool afh;    /* True: send AFH command */
#ifdef __DIRECT_CHNL_MAP__
    kal_uint8 chnl_map[10];
#endif /* __DIRECT_CHNL_MAP__ */
    kal_uint32 freq;
//#if defined (WIFI_BB_MT5921) && (defined (__BT_CHIP_VERSION_1__))
    kal_uint8 hb;
//#endif
    kal_bool pta;    /* True: send PTA command */    
    kal_uint8 pta_action;
} bt_wifi_set_chnl_req_struct;

/* MSG_ID_BT_MMI_RESET_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;    /* TRUE: successful, FALSE: failed */
} bt_mmi_reset_cnf_struct;

/* MSG_ID_BT_TEST_MODE_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    /* mode=0 : query current test mode, mode=1 : enable test mode */
    kal_uint8 mode;                     /* 1: enter test mode, 2: leave test mode */
#ifdef __GEMINI__
    kal_uint8 dual_sim_uart_setting;    /* 0: L4C_UART_TO_SIM1, 1: L4C_UART_UART_TO_SIM2 */
#endif
} bt_test_mode_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    /* mode=0 : 0 -> test mode disabled, 1 -> test mode enabled */
    /* mode=1 : 0 -> failed, 1 -> success */
    kal_bool result;
} bt_test_mode_cnf_struct;


/* MSG_ID_BT_SSP_DEBUG_MODE_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 on;                     /* 0: debug mode off, 1: debug mode on */
} bt_ssp_debug_mode_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;    /* TRUE: successful, FALSE: failed */
} bt_ssp_debug_mode_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 name[32];
    kal_uint8 bd_addr[6];
    kal_uint8 dcs;
} bt_set_bd_param_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 index;
    kal_uint8 external_pa;
    kal_uint8 internal_pa;
    kal_uint16 output_power;
} bt_set_power_table_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 trim_value;
} bt_set_ana_trim_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
} bt_pcm_lb_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;    /* TRUE: successful, FALSE: failed */
#ifdef BTMTK_ON_WISE
    kal_int8 chip_version[BT_MAX_VERSION_STR_SIZE];  /* ASCII */
    kal_int8 lmp_version[BT_MAX_VERSION_STR_SIZE];   /* ASCII */
    kal_int8 patch_version[BT_MAX_VERSION_STR_SIZE]; /* ASCII */
#else   /* BTMTK_ON_WISE */
    S8 chip_version[BT_MAX_VERSION_STR_SIZE];  /* ASCII */
    S8 lmp_version[BT_MAX_VERSION_STR_SIZE];   /* ASCII */
    S8 patch_version[BT_MAX_VERSION_STR_SIZE]; /* ASCII */
#endif  /* BTMTK_ON_WISE */
} bt_get_bt_version_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
} bt_l4c_at_general_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
} bt_engineer_mode_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 trace;
    kal_uint8 primitive;
    kal_uint8 prompt;
} bt_engineer_mode_log_mask_req_struct;

typedef bt_engineer_mode_cnf_struct bt_engineer_mode_log_mask_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8  name[128];   /* log to <name> */
} bt_engineer_mode_log_to_file_req_struct;

typedef bt_engineer_mode_cnf_struct bt_engineer_mode_log_to_file_cnf_struct;

/* MSG_ID_BT_BTTM_SWITCH_PORT_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool bttm;
} bt_bttm_switch_port_req_struct;

/* MSG_ID_BT_BTTM_SWITCH_PORT_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
} bt_bttm_switch_port_cnf_struct;


typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 pattern;
    kal_uint8 channel_hopping;
    kal_uint8 tx_freq;
    kal_uint8 rx_freq;
    kal_uint8 poll_period;
    kal_uint8 packet_type;
    kal_uint16 packet_length;
} bt_engineer_mode_txrx_test_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
} bt_engineer_mode_txrx_test_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 level;
} bt_engineer_mode_power_test_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
} bt_engineer_mode_txrx_power_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 addr[12];
} bt_engineer_mode_device_test_req_struct;

#ifdef __GEMINI__
/* MSG_ID_BT_ENGINEER_MODE_POWERON_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 dual_sim_uart_setting;
} bt_engineer_mode_poweron_req_struct;
#endif /* __GEMINI__ */

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
#ifdef __GEMINI__
    kal_uint8 dual_sim_uart_setting;    /* 0: L4C_UART_TO_SIM1, 1: L4C_UART_UART_TO_SIM2 */
#endif
} bt_read_bd_addr_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_bool result;
    kal_uint8 bd_addr[6];
} bt_read_bd_addr_cnf_struct;

/* MSG_ID_BT_TEST_CMD_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 test_cmd_content[256];
} bt_test_cmd_req_struct;

/* MSG_ID_BT_TEST_CMD_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 result;
    kal_uint8 test_cmd_content[256];
} bt_test_cmd_cnf_struct;

/* MSG_ID_BT_L4C_TEST_CMD_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 test_cmd_rsp_buf[256];
    kal_uint16 length;
} bt_test_cmd_rsp_struct;

/* MSG_ID_BT_READ_RSSI_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 lap;
    kal_uint8  uap;
    kal_uint16 nap;
}bt_read_rssi_req_struct;

/* MSG_ID_BT_READ_RSSI_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 result;
    kal_int8 rssi_value;
    kal_uint32 lap;
    kal_uint8  uap;
    kal_uint16 nap;
}bt_read_rssi_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 value;
}bt_set_sco_accept_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 value;
}bt_set_sco_accept_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 val_len;
    kal_uint8 value[32];
}bt_set_dual_pcm_setting_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 val_len;
    kal_uint8 value[32];
}bt_set_dual_pcm_switch_req_struct;


#define OBRC_SCO_REJECT 0xFF    /* SCO REJECT */

/* BIP definitions */
#define BT_BIP_MAX_PATH_LEN     260
#define BT_BIP_MAX_IMG_FORMATS  10
#define BT_BIP_MAX_IMG_NAME_LEN  GOEP_MAX_OBJ_NAME      /* 256 */
#define BT_BIP_MAX_TIME_LEN     18  /* (YYYYMMDDTHHMMSS)(Z) */
#define BT_BIP_IMG_HANDLE_LEN   8
#define BT_BIP_MAX_DEV_NAME_LEN 80
#define BT_BIP_MAX_ATTR_NAME_LEN 40
#define BT_BIP_MAX_IMG_LIST_ITEMS 32
#define BT_BIP_ENCODING_LENGTH 2

/* GOEP definitions */
#define GOEP_MAX_UUID_SIZE 16
#define GOEP_MAX_PASSWD_SIZE 16
#define GOEP_MAX_REALM_SIZE 20
#define GOEP_MAX_USERID_SIZE 20
#define GOEP_MAX_OBJ_NAME (256 * 2)
#define GOEP_MAX_FOLDER_NAME (256 * 2)
#define GOEP_MAX_DEV_NAME 80
#define GOEP_MAX_DESCRIPTION 80
#define GOEP_MAX_MIME_TYPE 80
#define GOEP_MAX_SERVER_NUM 16
#define GOEP_MAX_CLIENT_NUM 16
#define GOEP_CONNECTION_NUM (GOEP_MAX_SERVER_NUM + GOEP_MAX_CLIENT_NUM)
//#define GOEPL2_MAX_RETRANS_NUM 3
#if L2CAP_FLOW_NON_BASIC_MODE == XA_ENABLED
//#define RF_L2CAP_MAX_SREJ_SIZE					10
#endif

/* The function call was successful. */
#define GOEP_STATUS_SUCCESS          0
/* The operation has failed to start. */
#define GOEP_STATUS_FAILED           1
/* The operation is pending to wait for authentication information */
#define GOEP_STATUS_PENDING          2
/* The transport layer link is disconnected. */
#define GOEP_STATUS_DISCONNECT       3
/* No connection exists, when one is required */
#define GOEP_STATUS_NO_CONNECT       4
/* A request is pending or resources are in use. */
#define GOEP_STATUS_BUSY             11
/* The necessary resources are not available. */
#define GOEP_STATUS_NO_RESOURCES     12
/* An invalid function parameter was provided. */
#define GOEP_STATUS_INVALID_PARM     18
/*
 * Bluetooth Only: The request failed because the specified device
 * is in the process of being disconnected.
 */
#define GOEP_STATUS_IN_PROGRESS      19
/* Feature not supported */
#define GOEP_STATUS_NOT_SUPPORTED    23
/*
 * Bluetooth Only : Feature is restricted, due to a
 * Bluetooth security failure
 */
#define GOEP_STATUS_RESTRICTED       20
/* The OBEX packet is too small to support the request. */
#define GOEP_STATUS_PACKET_TOO_SMALL 31
/* No active session exists, when one is required. */
#define GOEP_STATUS_NO_SESSION       32

/* SCO_REJECT */
#define GOEP_STATUS_SCO_REJECT       25

typedef U8 GoepRspCode;

#define GOEP_CREATED              0x21  /* Created */
#define GOEP_ACCEPTED             0x22  /* Accepted */
#define GOEP_NON_AUTHOR_INFO      0x23  /* Non-Authoritative Information */
#define GOEP_NO_CONTENT           0x24  /* No Content */
#define GOEP_RESET_CONTENT        0x25  /* Reset Content */
#define GOEP_PARTIAL_CONTENT      0x26  /* Partial Content */

#define GOEP_MULTIPLE_CHOICES     0x30  /* Multiple Choices */
#define GOEP_MOVED_PERMANENT      0x31  /* Moved Permanently */
#define GOEP_MOVED_TEMPORARY      0x32  /* Moved Temporarily */
#define GOEP_SEE_OTHER            0x33  /* See Other */
#define GOEP_NOT_MODIFIED         0x34  /* Not Modified */
#define GOEP_USE_PROXY            0x35  /* Use Proxy */

#define GOEP_BAD_REQUEST          0x40  /* Bad Request */
#define GOEP_UNAUTHORIZED         0x41  /* Unauthorized */
#define GOEP_PAYMENT_REQUIRED     0x42  /* Payment Required */
#define GOEP_FORBIDDEN            0x43  /* Forbidden - operation is understood but refused */
#define GOEP_NOT_FOUND            0x44  /* Not Found */
#define GOEP_METHOD_NOT_ALLOWED   0x45  /* Method Not Allowed */
#define GOEP_NOT_ACCEPTABLE       0x46  /* Not Acceptable */
#define GOEP_PROXY_AUTHEN_REQ     0x47  /* Proxy Authentication Required */
#define GOEP_REQUEST_TIME_OUT     0x48  /* Request Timed Out */
#define GOEP_CONFLICT             0x49  /* Conflict */

#define GOEP_GONE                 0x4a  /* Gone */
#define GOEP_LENGTH_REQUIRED      0x4b  /* Length Required */
#define GOEP_PRECONDITION_FAILED  0x4c  /* Precondition Failed */
#define GOEP_REQ_ENTITY_TOO_LARGE 0x4d  /* Requested entity is too large */
#define GOEP_REQ_URL_TOO_LARGE    0x4e  /* Requested URL is too large */
#define GOEP_UNSUPPORT_MEDIA_TYPE 0x4f  /* Unsupported Media Type */

#define GOEP_INTERNAL_SERVER_ERR  0x50  /* Internal Server Error */
#define GOEP_NOT_IMPLEMENTED      0x51  /* Not Implemented */
#define GOEP_BAD_GATEWAY          0x52  /* Bad Gateway */
#define GOEP_SERVICE_UNAVAILABLE  0x53  /* Service Unavailable */
#define GOEP_GATEWAY_TIMEOUT      0x54  /* Gateway Timeout */
#define GOEP_HTTP_VER_NO_SUPPORT  0x55  /* HTTP version not supported */

#define GOEP_DATABASE_FULL        0x60  /* Database Full */
#define GOEP_DATABASE_LOCKED      0x61  /* Database Locked */


#define GOEP_USER_CANCEL          0x70 /* User cancel the pushing/pulling/connect */
#define GOEP_USER_UNKNOW          0x71 /* Remote User cancel the pushing/pulling/connect */
/* End of GOEPStatus */

/* GOEP ENUM */
typedef enum
{
    GOEP_TP_BT = 0,
    GOEP_TP_IRDA
} goep_tp_type_enum;

typedef enum
{
    GOEP_FIRST_PKT = 0,
    GOEP_NORMAL_PKT,
    GOEP_FINAL_PKT,
    GOEP_SINGLE_PKT
} goep_pkt_type_enum;

typedef enum
{
    GOEP_FORWARD_FOLDER = 0,
    GOEP_BACK_FOLDER,
    GOEP_ROOT_FOLDER,
    GOEP_CREATE_FOLDER
} goep_set_folder_type_enum;

typedef enum
{
    GOEP_PUT_NORMAL = 0,
    GOEP_PUT_DELETE,
    GOEP_CREATEEMPTY
} goep_put_type_enum;

typedef enum
{
    GOEP_SERVER_ROLE = 0,
    GOEP_CLIENT_ROLE
} goep_role_enum;


/**** Start of BIP Enum ****/
typedef enum
{
    BT_BIP_IMAGE_PUSH = 0x01,
    BT_BIP_IMAGE_PULL = 0x02,
    BT_BIP_ADVANCED_PRINTING = 0x04,
    BT_BIP_REMOTE_CAMERA = 0x08,
    BT_BIP_AUTO_ARCHIVE = 0x10,
    BT_BIP_REMOTE_DISPLAY = 0x20
} bt_bip_service_enum;

typedef enum
{
    BT_BIP_CNF_SUCCESS = 0x70,
    BT_BIP_CNF_FAILED,
    BT_BIP_PARTIAL_CONTENT,
    BT_BIP_XML_GEN_FAIL,
    BT_BIP_FILE_OPEN_FAIL,
    BT_BIP_FILE_READ_FAIL,
    BT_BIP_FILE_WRITE_FAIL,
    BT_BIP_FILE_MOVE_FAIL,
    BT_BIP_INVALID_PARM,
    BT_BIP_DISK_FULL,
    BT_BIP_ROOT_DIR_FULL,
    BT_BIP_FS_MEDIA_CHANGED,
    BT_BIP_SCO_REJECT,
    BT_BIP_BTCHIP_REJECT,
    BT_BIP_XML_PARSE_FAIL,
    BT_BIP_FS_QUOTA_FULL,
    BT_BIP_DISCONNECTED,
    BT_BIP_FS_WRITE_PROTECTION,
    BT_BIP_DRM_NO_RIGHTS,
    BT_BIP_CNF_CHALLENGE_OK,
    BT_BIP_CNF_CHALLENGE_FAILED,
    BT_BIP_AUTHENTICATE_FAILED,

    BT_BIP_LAST_CNF_CODE
} bt_bip_cnf_enum;

typedef enum
{
    BT_BIP_IMG_TYPE_UNSUPPORT = 0x00000000,
    BT_BIP_IMG_TYPE_JPEG = 0x00000001,
    BT_BIP_IMG_TYPE_BMP = 0x00000002,
    BT_BIP_IMG_TYPE_GIF = 0x00000004,
    BT_BIP_IMG_TYPE_WBMP = 0x00000008,
    BT_BIP_IMG_TYPE_PNG = 0x00000010
} bt_bip_img_format_enum;

/**** End of BIP Enum ****/

/* GOEP structure */

typedef bt_addr_struct goep_bd_addr_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    goep_tp_type_enum tp_type;
    kal_bool need_auth;
    kal_uint8 *buf_ptr;
    kal_uint16 buf_size;
    kal_uint8 rawheader;
    kal_uint8 tptype;
    kal_uint8 obexver;    /* obex version field 0x10 1.0 0x12 1.2 */
} goep_register_server_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 rsp_code;
    kal_uint8 goep_conn_id;
} goep_register_server_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
} goep_deregister_server_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_bd_addr_struct bd_addr;
    kal_uint8 dev_name[GOEP_MAX_DEV_NAME];
} goep_authorize_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_bd_addr_struct bd_addr;
    kal_uint8 dev_name[GOEP_MAX_DEV_NAME];
    kal_uint16 peer_mru;
    kal_uint32 cm_conn_id;
    kal_uint8 client_auth;
    kal_uint8 server_auth;
    kal_uint8 tptype;
    kal_uint8 obexver;    /* obex version field 0x10 1.0 0x12 1.2 */
} goep_connect_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
    kal_uint8 client_pwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 client_pwd_len;
    kal_uint8 client_userid[GOEP_MAX_USERID_SIZE];
    kal_uint8 client_userid_len;
    kal_uint8 server_pwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 server_pwd_len;
    kal_uint8 server_realm[GOEP_MAX_REALM_SIZE];
    kal_uint8 server_realm_len;
} goep_connect_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_pkt_type_enum pkt_type;
    goep_put_type_enum put_type;
    kal_uint32 total_obj_len;
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME];
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    kal_uint8 *frag_ptr;
    kal_uint16 frag_len;
#ifdef __BT_GOEPS_AUTO_ACK__
    kal_uint8 buff_index;
#endif
} goep_push_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME];
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    goep_pkt_type_enum pkt_type;
} goep_pull_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
    goep_pkt_type_enum pkt_type;
    kal_uint8 rsp_code;
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    kal_uint32 total_obj_len;
    kal_uint8 *frag_ptr;
    kal_uint16 frag_len;
} goep_pull_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 folder_name[GOEP_MAX_FOLDER_NAME];
    goep_set_folder_type_enum setpath_flag;
} goep_set_folder_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
} goep_abort_ind_struct;


typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 server_supported_formats;
} goep_opp_supported_formats_ind_struct;


typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 goep_conn_id;
    kal_uint8 passwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 passwd_len;
    kal_uint8 realm_str[GOEP_MAX_REALM_SIZE];
    kal_uint8 realm_len;
    goep_role_enum goep_role;
} goep_auth_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    goep_bd_addr_struct bd_addr;
    goep_tp_type_enum tp_type;
    kal_uint8 *buf_ptr;
    kal_uint16 buf_size;
    kal_bool auth_use;
    kal_uint8 client_pwd[GOEP_MAX_PASSWD_SIZE];   /// client-challenge pwd
    kal_uint8 client_pwd_len;
    kal_uint8 client_realm[GOEP_MAX_REALM_SIZE];
    kal_uint8 client_realm_len;
    kal_uint8 tptype; 
    kal_uint8 obexver;    /* obex version field 0x10 1.0 0x12 1.2 */
} goep_connect_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
    kal_uint16 peer_mru;
    kal_uint32 cm_conn_id;
    kal_uint8 tptype;
    kal_uint8 obexver;    /* obex version field 0x10 1.0 0x12 1.2 */
    kal_uint8 srmdefault; /* useless. in spec, it may false */
} goep_connect_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE]; // NEW
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_pkt_type_enum pkt_type;
    goep_put_type_enum put_type;
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    kal_uint32 total_obj_len;
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME];
    kal_uint8 *frag_ptr;
    kal_uint16 frag_len;
} goep_push_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
    kal_uint8 reason;   /* failed reason. added for application */
#ifdef __BT_GOEPC_AUTO_ACK__
    kal_uint8 buff_index;
#endif
} goep_push_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE]; // NEW
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_pkt_type_enum pkt_type;
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME];
} goep_pull_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_pkt_type_enum pkt_type;
    kal_uint8 rsp_code;
    kal_uint32 total_obj_len;
    kal_uint8 *frag_ptr;
    kal_uint16 frag_len;
    kal_bool to_self;   /* added for continue parsing large folder content */
    kal_uint8 reason;   /* failed reason. added for application */
} goep_pull_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE]; //NEW
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    goep_set_folder_type_enum setpath_flag;
    kal_uint8 folder_name[GOEP_MAX_FOLDER_NAME];
} goep_set_folder_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
    kal_uint8 reason;   /* failed reason. added for application */
} goep_set_folder_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE]; //NEW
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 folder_name[GOEP_MAX_FOLDER_NAME];
    kal_uint8 dst_name[GOEP_MAX_FOLDER_NAME];
    kal_uint8 action_id;
    kal_uint32 permission;
} goep_action_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
    kal_uint8 reason;   /* failed reason. added for application */
} goep_action_cnf_struct;


typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE]; //NEW
    kal_uint8 uuid_len;    
    kal_uint8 goep_conn_id;
    kal_uint8 folder_name[GOEP_MAX_FOLDER_NAME];
    kal_uint8 dst_name[GOEP_MAX_FOLDER_NAME];
    kal_uint8 action_id;
    kal_uint32 permission;
} goep_action_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
    kal_uint8 reason;   /* failed reason. added for application */
} goep_action_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE]; //NEW
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
} goep_abort_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 goep_conn_id;
} goep_disconnect_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
} goep_disconnect_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 goep_conn_id;
    kal_uint8 reason;
} goep_disconnect_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 req_id;
    kal_uint8 realm[GOEP_MAX_REALM_SIZE];
    kal_uint8 realm_len;
    kal_uint8 dev_name[GOEP_MAX_DEV_NAME];
    goep_bd_addr_struct bd_addr;
    kal_uint8 options; ///< OBEX challenge options
} goep_auth_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 passwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 passwd_len;
    kal_uint8 userid[GOEP_MAX_USERID_SIZE];
    kal_uint8 userid_len;
} goep_auth_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 goep_conn_id;
    kal_uint32 timeticker;
} goep_register_client_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 req_id;
    kal_uint8 goep_conn_id;
    kal_uint32 timeticker;
} goep_register_client_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
} goep_cnf_struct;

typedef goep_cnf_struct goep_deregister_server_cnf_struct;
typedef goep_cnf_struct goep_auth_cnf_struct;
typedef goep_cnf_struct goep_abort_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
    kal_uint8 rsp_code;
} goep_rsp_struct;

typedef goep_rsp_struct goep_authorize_rsp_struct;
typedef goep_rsp_struct goep_push_rsp_struct;
typedef goep_rsp_struct goep_set_folder_rsp_struct;
typedef goep_rsp_struct goep_abort_rsp_struct;

/****** GOEP PROFILE****/
#define OBEX_EXTRA_SERVER_NUM 2
#define FTP_INTERNAL_RW_BUFFER_SIZE      (1024*32)
#define FTP_LARGE_FILE_SIZE             (1024*1024)
/****** END of GOEP PROFILE****/

/* FTP definitions */
#define BT_FTP_MAX_OBS_NUMBER     4

/* GOEP for catcher */
/* goeps for catcher*/
typedef  goep_register_server_req_struct  bt_goeps_register_server_req_struct;
typedef  goep_register_server_cnf_struct  bt_goeps_register_server_cnf_struct;
typedef  goep_deregister_server_req_struct  bt_goeps_deregister_server_req_struct;
typedef  goep_deregister_server_cnf_struct  bt_goeps_deregister_server_cnf_struct;
typedef  goep_connect_ind_struct  bt_goeps_connect_ind_struct;
typedef  goep_connect_rsp_struct  bt_goeps_connect_rsp_struct;
typedef  goep_push_ind_struct  bt_goeps_push_ind_struct;
typedef  goep_push_rsp_struct  bt_goeps_push_rsp_struct;
typedef  goep_pull_ind_struct  bt_goeps_pull_ind_struct;
typedef  goep_pull_rsp_struct  bt_goeps_pull_rsp_struct;
typedef  goep_set_folder_ind_struct  bt_goeps_set_folder_ind_struct;
typedef  goep_set_folder_rsp_struct  goeps_set_folder_rsp_struct;
typedef  goep_action_ind_struct  bt_goeps_action_ind_struct;
typedef  goep_action_rsp_struct  goeps_action_rsp_struct;
typedef  goep_abort_ind_struct  bt_goeps_abort_ind_struct;
typedef  goep_abort_rsp_struct  bt_goeps_abort_rsp_struct;
typedef  goep_auth_req_struct  bt_goeps_auth_req_struct;
typedef  goep_auth_cnf_struct  bt_goeps_auth_cnf_struct;
typedef  goep_disconnect_req_struct  bt_goeps_disconnect_req_struct;
typedef  goep_disconnect_ind_struct  bt_goeps_disconnect_ind_struct;
typedef  goep_auth_ind_struct  bt_goeps_auth_ind_struct;
typedef  goep_auth_rsp_struct  bt_goeps_auth_rsp_struct;
typedef  goep_authorize_ind_struct  bt_goeps_authorize_ind_struct;
typedef  goep_authorize_rsp_struct  bt_goeps_authorize_rsp_struct;
typedef  goep_disconnect_ind_struct  bt_goeps_tpdisconnect_ind_struct;
/* goepc for catcher*/

typedef  goep_connect_req_struct  bt_goepc_connect_req_struct;
typedef  goep_connect_cnf_struct  bt_goepc_connect_cnf_struct;
typedef  goep_push_req_struct  bt_goepc_push_req_struct;
typedef  goep_push_cnf_struct  bt_goepc_push_cnf_struct;
typedef  goep_pull_req_struct  bt_goepc_pull_req_struct;
typedef  goep_pull_cnf_struct  bt_goepc_pull_cnf_struct;
typedef  goep_set_folder_req_struct  bt_goepc_set_folder_req_struct;
typedef  goep_set_folder_cnf_struct  bt_goepc_set_folder_cnf_struct;
typedef  goep_action_req_struct  bt_goepc_action_req_struct;
typedef  goep_action_cnf_struct  bt_goepc_action_cnf_struct;
typedef  goep_abort_req_struct  bt_goepc_abort_req_struct;
typedef  goep_abort_cnf_struct  bt_goepc_abort_cnf_struct;
typedef  goep_disconnect_req_struct  bt_goepc_disconnect_req_struct;
typedef  goep_disconnect_cnf_struct  bt_goepc_disconnect_cnf_struct;
typedef  goep_disconnect_ind_struct  bt_goepc_disconnect_ind_struct;
typedef  goep_auth_ind_struct  bt_goepc_auth_ind_struct;
typedef  goep_auth_rsp_struct  bt_goepc_auth_rsp_struct;
typedef  goep_opp_supported_formats_ind_struct  bt_goepc_opp_supported_formats_ind_struct;
typedef  goep_disconnect_ind_struct  bt_goepc_tpdisconnect_ind_struct;
/****** opps for catcher ******/
typedef  goep_register_server_req_struct  bt_opps_register_server_req_struct;
typedef  goep_register_server_cnf_struct  bt_opps_register_server_cnf_struct;
typedef  goep_deregister_server_req_struct  bt_opps_deregister_server_req_struct;
typedef  goep_deregister_server_cnf_struct  bt_opps_deregister_server_cnf_struct;
typedef  goep_connect_ind_struct  bt_opps_connect_ind_struct;
typedef  goep_connect_rsp_struct  bt_opps_connect_rsp_struct;
typedef  goep_push_ind_struct  bt_opps_push_ind_struct;
typedef  goep_push_rsp_struct  bt_opps_push_rsp_struct;
typedef  goep_pull_ind_struct  bt_opps_pull_ind_struct;
typedef  goep_pull_rsp_struct  bt_opps_pull_rsp_struct;
typedef  goep_set_folder_ind_struct  bt_opps_set_folder_ind_struct;
typedef  goep_set_folder_rsp_struct  bt_opps_set_folder_rsp_struct;
typedef  goep_action_ind_struct  bt_opps_action_ind_struct;
typedef  goep_action_rsp_struct  bt_opps_action_rsp_struct;
typedef  goep_abort_ind_struct  bt_opps_abort_ind_struct;
typedef  goep_abort_rsp_struct  bt_opps_abort_rsp_struct;
typedef  goep_auth_req_struct  bt_opps_auth_req_struct;
typedef  goep_auth_cnf_struct  bt_opps_auth_cnf_struct;
typedef  goep_disconnect_req_struct  bt_opps_disconnect_req_struct;
typedef  goep_disconnect_ind_struct  bt_opps_disconnect_ind_struct;
typedef  goep_auth_ind_struct  bt_opps_auth_ind_struct;
typedef  goep_auth_rsp_struct  bt_opps_auth_rsp_struct;
typedef  goep_authorize_ind_struct  bt_opps_authorize_ind_struct;
typedef  goep_authorize_rsp_struct  bt_opps_authorize_rsp_struct;
typedef  goep_disconnect_ind_struct  bt_opps_tpdisconnect_ind_struct;
/* oppc for catcher*/
typedef  goep_connect_req_struct  bt_oppc_connect_req_struct;
typedef  goep_connect_cnf_struct  bt_oppc_connect_cnf_struct;
typedef  goep_push_req_struct  bt_oppc_push_req_struct;
typedef  goep_push_cnf_struct  bt_oppc_push_cnf_struct;
typedef  goep_pull_req_struct  bt_oppc_pull_req_struct;
typedef  goep_pull_cnf_struct  bt_oppc_pull_cnf_struct;
typedef  goep_set_folder_req_struct  bt_oppc_set_folder_req_struct;
typedef  goep_set_folder_cnf_struct  bt_oppc_set_folder_cnf_struct;
typedef  goep_action_req_struct  bt_oppc_action_req_struct;
typedef  goep_action_cnf_struct  bt_oppc_action_cnf_struct;
typedef  goep_abort_req_struct  bt_oppc_abort_req_struct;
typedef  goep_abort_cnf_struct  bt_oppc_abort_cnf_struct;
typedef  goep_disconnect_req_struct  bt_oppc_disconnect_req_struct;
typedef  goep_disconnect_cnf_struct  bt_oppc_disconnect_cnf_struct;
typedef  goep_disconnect_ind_struct  bt_oppc_disconnect_ind_struct;
typedef  goep_auth_ind_struct  bt_oppc_auth_ind_struct;
typedef  goep_auth_rsp_struct  bt_oppc_auth_rsp_struct;
typedef  goep_opp_supported_formats_ind_struct  bt_oppc_opp_supported_formats_ind_struct;
typedef  goep_disconnect_ind_struct  bt_oppc_tpdisconnect_ind_struct;

/* ftps for catcher*/
typedef  goep_register_server_req_struct  bt_ftps_register_server_req_struct;
typedef  goep_register_server_cnf_struct  bt_ftps_register_server_cnf_struct;
typedef  goep_deregister_server_req_struct  bt_ftps_deregister_server_req_struct;
typedef  goep_deregister_server_cnf_struct  bt_ftps_deregister_server_cnf_struct;
typedef  goep_connect_ind_struct  bt_ftps_connect_ind_struct;
typedef  goep_connect_rsp_struct  bt_ftps_connect_rsp_struct;
typedef  goep_push_ind_struct  bt_ftps_push_ind_struct;
typedef  goep_push_rsp_struct  bt_ftps_push_rsp_struct;
typedef  goep_pull_ind_struct  bt_ftps_pull_ind_struct;
typedef  goep_pull_rsp_struct  bt_ftps_pull_rsp_struct;
typedef  goep_set_folder_ind_struct  bt_ftps_set_folder_ind_struct;
typedef  goep_set_folder_rsp_struct  bt_ftps_set_folder_rsp_struct;
typedef  goep_action_ind_struct  bt_ftps_action_ind_struct;
typedef  goep_action_rsp_struct  bt_ftps_action_rsp_struct;
typedef  goep_abort_ind_struct  bt_ftps_abort_ind_struct;
typedef  goep_abort_rsp_struct  bt_ftps_abort_rsp_struct;
typedef  goep_auth_req_struct  bt_ftps_auth_req_struct;
typedef  goep_auth_cnf_struct  bt_ftps_auth_cnf_struct;
typedef  goep_disconnect_req_struct  bt_ftps_disconnect_req_struct;
typedef  goep_disconnect_ind_struct  bt_ftps_disconnect_ind_struct;
typedef  goep_auth_ind_struct  bt_ftps_auth_ind_struct;
typedef  goep_auth_rsp_struct  bt_ftps_auth_rsp_struct;
typedef  goep_authorize_ind_struct  bt_ftps_authorize_ind_struct;
typedef  goep_authorize_rsp_struct  bt_ftps_authorize_rsp_struct;
typedef  goep_disconnect_ind_struct  bt_ftps_tpdisconnect_ind_struct;
/* ftpc for catcher*/
//typedef  goep_register_client_req_struct  bt_ftpc_register_client_req_struct;
//typedef  goep_register_client_cnf_struct  bt_ftpc_register_client_cnf_struct;
typedef  goep_connect_req_struct  bt_ftpc_connect_req_struct;
typedef  goep_connect_cnf_struct  bt_ftpc_connect_cnf_struct;
typedef  goep_push_req_struct  bt_ftpc_push_req_struct;
typedef  goep_push_cnf_struct  bt_ftpc_push_cnf_struct;
typedef  goep_pull_req_struct  bt_ftpc_pull_req_struct;
typedef  goep_pull_cnf_struct  bt_ftpc_pull_cnf_struct;
typedef  goep_set_folder_req_struct  bt_ftpc_set_folder_req_struct;
typedef  goep_set_folder_cnf_struct  bt_ftpc_set_folder_cnf_struct;
typedef  goep_action_req_struct  bt_ftpc_action_req_struct;
typedef  goep_action_cnf_struct  bt_ftpc_action_cnf_struct;
typedef  goep_abort_req_struct  bt_ftpc_abort_req_struct;
typedef  goep_abort_cnf_struct  bt_ftpc_abort_cnf_struct;
typedef  goep_disconnect_req_struct  bt_ftpc_disconnect_req_struct;
typedef  goep_disconnect_cnf_struct  bt_ftpc_disconnect_cnf_struct;
typedef  goep_disconnect_ind_struct  bt_ftpc_disconnect_ind_struct;
typedef  goep_auth_ind_struct  bt_ftpc_auth_ind_struct;
typedef  goep_auth_rsp_struct  bt_ftpc_auth_rsp_struct;
typedef  goep_opp_supported_formats_ind_struct  bt_ftpc_opp_supported_formats_ind_struct;
typedef  goep_disconnect_ind_struct  bt_ftpc_tpdisconnect_ind_struct;
/***** for catcher ******/

/* GOEPv2 FTP */
#define FTP_MAX_FILEPATH 512
typedef  goep_register_server_req_struct  ftps_register_server_req_struct;
typedef  goep_register_server_cnf_struct  ftps_register_server_cnf_struct;
typedef  goep_deregister_server_req_struct  ftps_deregister_server_req_struct;
typedef  goep_deregister_server_cnf_struct  ftps_deregister_server_cnf_struct;
typedef  goep_connect_ind_struct  ftps_connect_ind_struct;
typedef  goep_connect_rsp_struct  ftps_connect_rsp_struct;
typedef  goep_push_ind_struct  ftps_push_ind_struct;
typedef  goep_push_rsp_struct  ftps_push_rsp_struct;
typedef  goep_pull_ind_struct  ftps_pull_ind_struct;
typedef  goep_pull_rsp_struct  ftps_pull_rsp_struct;
typedef  goep_set_folder_ind_struct  ftps_set_folder_ind_struct;
typedef  goep_set_folder_rsp_struct  ftps_set_folder_rsp_struct;
typedef  goep_action_ind_struct  ftps_action_ind_struct;
typedef  goep_action_rsp_struct  ftps_action_rsp_struct;
typedef  goep_abort_ind_struct  ftps_abort_ind_struct;
typedef  goep_abort_rsp_struct  ftps_abort_rsp_struct;
typedef  goep_auth_req_struct  ftps_auth_req_struct;
typedef  goep_auth_cnf_struct  ftps_auth_cnf_struct;
typedef  goep_disconnect_req_struct  ftps_disconnect_req_struct;
typedef  goep_disconnect_ind_struct  ftps_disconnect_ind_struct;
typedef  goep_auth_ind_struct  ftps_auth_ind_struct;
typedef  goep_auth_rsp_struct  ftps_auth_rsp_struct;
typedef  goep_authorize_ind_struct  ftps_authorize_ind_struct;
//typedef  goep_authorize_rsp_struct  ftps_authorize_rsp_struct;
typedef  goep_disconnect_ind_struct  ftps_tpdisconnect_ind_struct;
/* ftpc for catcher*/
typedef  goep_connect_req_struct  ftpc_connect_req_struct;
typedef  goep_connect_cnf_struct  ftpc_connect_cnf_struct;
typedef  goep_push_req_struct  ftpc_push_req_struct;
typedef  goep_push_cnf_struct  ftpc_push_cnf_struct;
typedef  goep_pull_req_struct  ftpc_pull_req_struct;
typedef  goep_pull_cnf_struct  ftpc_pull_cnf_struct;
typedef  goep_set_folder_req_struct  ftpc_set_folder_req_struct;
typedef  goep_set_folder_cnf_struct  ftpc_set_folder_cnf_struct;
typedef  goep_action_req_struct  ftpc_action_req_struct;
typedef  goep_action_cnf_struct  ftpc_action_cnf_struct;
typedef  goep_abort_req_struct  ftpc_abort_req_struct;
typedef  goep_abort_cnf_struct  ftpc_abort_cnf_struct;
typedef  goep_disconnect_req_struct  ftpc_disconnect_req_struct;
typedef  goep_disconnect_cnf_struct  ftpc_disconnect_cnf_struct;
typedef  goep_disconnect_ind_struct  ftpc_disconnect_ind_struct;
typedef  goep_auth_ind_struct  ftpc_auth_ind_struct;
typedef  goep_auth_rsp_struct  ftpc_auth_rsp_struct;
typedef  goep_opp_supported_formats_ind_struct  ftpc_opp_supported_formats_ind_struct;
typedef  goep_disconnect_ind_struct  ftpc_tpdisconnect_ind_struct;

typedef goep_cnf_struct ftp_cnf_struct;
//typedef goep_cnf_struct ftps_deregister_server_cnf_struct;
//typedef goep_cnf_struct ftps_auth_cnf_struct;
typedef goep_cnf_struct ftps_abort_cnf_struct;
typedef goep_rsp_struct ftps_authorize_rsp_struct;

//typedef goep_rsp_struct ftps_push_rsp_struct;
//typedef goep_rsp_struct ftps_set_folder_rsp_struct;
//typedef goep_rsp_struct ftps_abort_rsp_struct;

/* end of GOEPv2 ftp */

// MSG_ID_BT_FTPC_PUSHFILE_REQ
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE]; /* mime header */
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME]; /* name header */
    U32 filepath_len;
    U8 filepath[FTP_MAX_FILEPATH]; /* OS dependented filepath */
    U8 srm_enable;
    U8 srm_waitcount;   /* for testing only */
} bt_ftpc_pushfile_req_struct;

// MSG_ID_BT_FTPC_PUSHFILE_CNF
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code;
    kal_uint8 goep_conn_id;
    U8 reason;
    U8 srm_enable;
} bt_ftpc_pushfile_cnf_struct;	

// MSG_ID_BT_FTPC_PULLFILE_REQ
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;    
    kal_uint8 goep_conn_id;
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE]; /* mime header */
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME]; /* name header */
    U32 filepath_len;
    U8 filepath[FTP_MAX_FILEPATH]; /* OS dependented filepath */    
    U8 srm_enable;
    U8 srm_waitcount;    /* for testing only */
} bt_ftpc_pullfile_req_struct;

// MSG_ID_BT_FTPC_PULLFILE_CNF
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8         rsp_code;
    kal_uint8 goep_conn_id;
    kal_uint32 total_obj_len; /* final retrieve data length */    
    U8         reason;
    U8 srm_enable;
} bt_ftpc_pullfile_cnf_struct;

// MSG_ID_BT_FTPC_ABORTFILE_REQ
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8         rsp_code;
    kal_uint8  goep_conn_id;
} bt_ftpc_abortfile_req_struct;

// MSG_ID_BT_FTPC_ABORTFILE_CNF
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8         rsp_code;
    kal_uint8  goep_conn_id;    
} bt_ftpc_abortfile_cnf_struct;

// MSG_ID_BT_FTPS_PUSHFILE_IND
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint32 total_obj_len;
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME];
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    U8 srm_enable;
} bt_ftps_pushfile_ind_struct;

// MSG_ID_BT_FTPS_PUSHFILE_RSP
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code; /* first response code */
    kal_uint8 goep_conn_id;
    U8 filepath[FTP_MAX_FILEPATH]; /* OS dependented filepath */   
    U16 filepath_len;    
    U8 srm_enable;
    U8 srm_waitcount; /* for testing only */
} bt_ftps_pushfile_rsp_struct;

// MSG_ID_BT_FTPS_PUSHFILE_RSP_IND
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code; /* final response code */
    kal_uint8 goep_conn_id;
    U32 total_obj_len; /* final file size */
} bt_ftps_pushfile_rsp_ind_struct;

// MSG_ID_BT_FTPS_PULLFILE_IND
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 goep_conn_id;
    kal_uint8 obj_name[GOEP_MAX_OBJ_NAME];
    kal_uint8 obj_mime_type[GOEP_MAX_MIME_TYPE];
    U8 srm_enable;
} bt_ftps_pullfile_ind_struct;

// MSG_ID_BT_FTPS_PULLFILE_RSP
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code; /* first response code */
    kal_uint8 goep_conn_id;
    U8 filepath[FTP_MAX_FILEPATH]; /* OS dependented filepath */
    U16 filepath_len;
    U8 srm_enable;
    U8 srm_waitcount; /* for testing only */    
} bt_ftps_pullfile_rsp_struct;

// MSG_ID_BT_FTPS_PULLHFILE_RSP_IND,	
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code; /* final response code */
    kal_uint8 goep_conn_id;
} bt_ftps_pullfile_rsp_ind_struct;

// MSG_ID_BT_FTPS_ABORTFILE_REQ
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code;
    kal_uint8 goep_conn_id;
} bt_ftps_abortfile_req_struct;

// MSG_ID_BT_FTPS_ABORTFILE_CNF
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    U8 rsp_code; /* final response code */    
    kal_uint8 goep_conn_id;
} bt_ftps_abortfile_cnf_struct;

// MSG_ID_BT_FTPS_FILE_PROGRESS_IND
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;    
    kal_uint8 operation; /* GOEP_OPER_PUSH or GOEP_OPER_PULL */
    U32 total_length;
    U32 curr_length;
} bt_ftps_file_progress_ind_struct;

// MSG_ID_BT_FTPC_FILE_PROGRESS_IND
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
    kal_uint8 operation;
    U32 total_length;
    U32 curr_length;
} bt_ftpc_file_progress_ind_struct;

// MSG_ID_BT_FTPS_SERVER_INTERNAL_RW_IND,	
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
	kal_uint8 param;
} bt_ftps_server_internal_rw_ind_struct;

// MSG_ID_BT_FTPC_CLIENT_INTERNAL_RW,	
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 goep_conn_id;
	kal_uint8 param;
} bt_ftpc_client_internal_rw_ind_struct;


typedef enum
{
    BT_OPP_READY_FEATURE ,     /* Not a auto fature. User could compose it*/
    BT_OPP_PULL_FEATURE = 1,   /* pull a vcard */
    BT_OPP_PUSH_FEATURE = 2,   /* push a object */
    BT_OPP_EXCHANGE_FEATURE    /* Exchange object */
} BT_OPC_FEATURE;

/****** OPP PROFILE****/
#define OPP_INVALID_CONN_ID  0xff
#define OPP_DEFAULT_CONN_ID  0xff
#define OPP_INVALID_CM_ID    0xff
#define OPP_DEFAULT_CM_ID    0xff
#define OPP_INVALID_REQ_ID   0xff
//#define OPP_DEFAULT_REQ_ID   0xff
#define OPP_DEFAULT_REQ_ID   0x64  // For ALPS00270904, use a large req_id to prevent muilt obex-based profiles coexistence conflict.

#define OPP_MAX_FLDR_LENGTH     41     // FMGR_MAX_FILE_LEN 41
#define OPP_MAX_PATH_LENGTH     640   // FMGR_MAX_PATH_LEN 260
#define OPP_MAX_OBJ_NAME_LENGTH (256 * 2)
#define OPP_MAX_OBJ_MIME_LENGTH 80 // GOEP_MAX_MIME_TYPE
#define OPPC_MAX_OBEX_PACKET_LENGTH 24576 // 14336 24576 L2CAP_MTU*OBEX_PERFORMANCE_MULTIPLIER
#define OPPS_MAX_OBEX_PACKET_LENGTH 24576 // 14336 24576 L2CAP_MTU*OBEX_PERFORMANCE_MULTIPLIER
#ifdef BTMTK_ON_LINUX
#define OPPC_ASHM_NAME "mtk.bt.profile.oppc.ashm"
#define OPPS_ASHM_NAME "mtk.bt.profile.opps.ashm"
#endif
/****** END OF OPP PROFILE****/


/****** FTP PROFILE****/
#define FS_FOLDER_DELIMITOR             L"\\"      /// OS FOLDER of OS type
#define BTMTK_FTPS_MAX_FILEPATH            512

/****** END OF FTP PROFILE****/




/**** Start of BIP Structure ****/

typedef enum
{
    BT_BIP_NULL,
    BT_BIP_INITIATOR_ROLE,
    BT_BIP_RESPONDER_ROLE
} bt_bip_session_role_enum;

typedef enum
{
    BT_BIP_TRANS_NONE,
    BT_BIP_TRANS_STRECH,
    BT_BIP_TRANS_CROP,
    BT_BIP_TRANS_FILL,
    BT_BIP_TRANS_MAX
} bt_bip_img_trans_enum;

typedef struct
{
    bt_bip_img_format_enum encoding;
    kal_bool define_pixel_with_range;
    kal_uint16 max_pixel_width;
    kal_uint16 min_pixel_width;
    kal_uint16 max_pixel_height;
    kal_uint16 min_pixel_height;
    kal_uint16 specified_pixel_width;
    kal_uint16 specified_pixel_height;
    kal_uint8 img_handle[8];
    kal_uint8 friendly_name[BT_BIP_MAX_IMG_NAME_LEN];
    kal_uint32 size;
    kal_uint8 created[BT_BIP_MAX_TIME_LEN];     /* (YYYYMMDDTHHMMSS)(Z) */
    kal_uint8 modified[BT_BIP_MAX_TIME_LEN];    /* (YYYYMMDDTHHMMSS)(Z) */
} bt_bip_img_info_struct;

typedef struct
{
	U16 width;
	U16 height;
	U16 width2;
	U16 height2;
} bt_bip_pixel_struct;

typedef struct
{
	U32 encoding;	/* REQUIRED, 0 means get native format image in GetImage */
	bt_bip_pixel_struct pixel;	/* REQUIRED, only fixed sizes for PutImage */
	U32 size;
	U32 transformation; /* bt_bip_img_trans_enum */
} bt_bip_img_desc_struct;

typedef bt_addr_struct bt_bip_bd_addr_struct;

/*   MSG_ID_BT_BIP_OBEX_AUTH_IND */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    kal_uint8 result;
    kal_uint8 option;
    kal_uint8 realm_len;
    kal_uint8 realm[GOEP_MAX_REALM_SIZE];
} bt_bip_obex_auth_ind_struct;

typedef struct
{
    kal_uint8 uid[GOEP_MAX_USERID_SIZE];
    kal_int8 uid_len;
    kal_uint8 pwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 pwd_len;
} bt_bip_dig_response_struct;

/*  MSG_ID_BT_BIP_OBEX_AUTH_RSP */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    kal_uint8 result;
    /* bt_bip_session_role_enum session_role; */
    bt_bip_dig_response_struct dig_rsp;
} bt_bip_obex_auth_rsp_struct;

typedef struct
{
    kal_uint8 uid[GOEP_MAX_USERID_SIZE];
    kal_int8 uid_len;
    kal_uint8 pwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 pwd_len;
    kal_uint8 realm[GOEP_MAX_REALM_SIZE];
    kal_uint8 realm_len;
} bt_bip_dig_challenge_struct;

/*  MSG_ID_BT_BIP_OBEX_AUTH_REQ */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    /* bt_bip_session_role_enum session_role; */
    bt_bip_dig_challenge_struct dig_chal;
} bt_bip_obex_auth_req_struct;

/*  MSG_ID_BT_BIP_OBEX_AUTH_CNF */
typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    /* bt_bip_session_role_enum session_role; */
    kal_uint8 cnf_code;
} bt_bip_obex_auth_cnf_struct;

/**** End of BIP Structure ****/

/* for OBEX authentication */
typedef struct
{
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;
    kal_uint8 passwd[GOEP_MAX_PASSWD_SIZE];
    kal_uint8 passwd_len;
    kal_uint8 realm_str[GOEP_MAX_REALM_SIZE];
    kal_uint8 realm_len;
} auth_chal_struct;

typedef kal_uint8(*AUTH_IND_HANDLER) (goep_auth_ind_struct *auth_ind);

struct auth_ind_handler_struct
{
    kal_uint8 uuid[GOEP_MAX_UUID_SIZE];
    kal_uint8 uuid_len;

    AUTH_IND_HANDLER auth_ind_handler;

    struct auth_ind_handler_struct *next;

};

typedef struct auth_ind_handler_struct auth_ind_handler_struct;

/* END for OBEX authentication */

/* csj */
/* struct used by pbap */
/*#if defined(__BT_PBAP_NEW_PROFILE__) || defined(__MMI_PBAP_NEW_SUPPORT__) */
#ifndef __BT_PBAP_PROFILE__

#define PBAP_FILTER_SIZE                8
#define BT_PBAP_MAX_DEV_NAME_LEN        80

typedef struct _PbapVcardFilter
{
    /* Array of 8 bytes for this 64-bit filter value */
    kal_uint8 byte[PBAP_FILTER_SIZE];
} PbapVcardFilter;

typedef kal_uint8 PbapVcardFormat;

#define VCARD_FORMAT_21        0x00     /* Version 2.1 format */
#define VCARD_FORMAT_30        0x01     /* Version 3.0 format */

/*SearchAttributes*/
#define PBAP_SEARCH_ATTRIB_NAME        0x00    /* Search by Name */
#define PBAP_SEARCH_ATTRIB_NUMBER      0x01    /* Search by Number */
#define PBAP_SEARCH_ATTRIB_SOUND       0x02    /* Search by Sound */

/*vcard sort order in listing*/
#define PBAP_SORT_ORDER_INDEXED        0x00    /* Indexed sorting */
#define PBAP_SORT_ORDER_ALPHA          0x01    /* Alphabetical sorting */
#define PBAP_SORT_ORDER_PHONETICAL     0x02    /* Phonetical sorting */

/*---------------------------------------------------------------------------
 * PB_MAX_NAME_LEN constant
 *
 *     Maximum number of characters allowed for pathnames + 1 (null-
 *     terminating character).
 */
#define PBAP_MAX_NAME_LEN         128
#define MAX_PBAP_SEARCH_VALUE_LENGTH PBAP_MAX_NAME_LEN

/*---------------------------------------------------------------------------
 * PbStatus type
 *
 *     This type is returned from most phonebook APIs to indicate the success
 *     or failure of the operation. In many cases, BT_STATUS_PENDING
 *     is returned, meaning that a future callback will indicate the
 *     result of the operation.
 */

typedef kal_uint8 PbStatus;

#define PB_STATUS_SUCCESS           0   /* XA_STATUS_SUCCESS */
#define PB_STATUS_FAILED            1   /* XA_STATUS_FAILED */
#define PB_STATUS_NO_RESOURCES      12  /* XA_STATUS_NO_RESOURCES */
#define PB_STATUS_NOT_FOUND         13  /* XA_STATUS_NOT_FOUND */
#define PB_STATUS_UNKNOWN_REMOTE    14  /* XA_STATUS_DEVICE_NOT_FOUND */
#define PB_STATUS_INUSE             5   /* XA_STATUS_IN_USE */
#define PB_STATUS_NOT_SUPPORTED     23  /* XA_STATUS_NOT_SUPPORTED */
#define PB_STATUS_PENDING           2   /* XA_STATUS_PENDING */
/* End of PbStatus */
typedef kal_uint8 PbapRespCode;

/* Group: Successful response codes */

#define PBRC_CONTINUE              0x10 /* Continue */
#define PBRC_STATUS_SUCCESS        0x20 /* Success */

/* Group: Failure response codes */

#define PBRC_BAD_REQUEST           0x40 /* Bad Request */
#define PBRC_UNAUTHORIZED          0x41 /* Unauthorized */
#define PBRC_FORBIDDEN             0x43 /* Forbidden - operation is understood */
#define PBRC_NOT_FOUND             0x44 /* Not Found */
#define PBRC_NOT_ACCEPTABLE        0x46 /* Not Acceptable */
#define PBRC_PRECONDITION_FAILED   0x4c /* Precondition Failed */
#define PBRC_NOT_IMPLEMENTED       0x51 /* Not Implemented */
#define PBRC_SERVICE_UNAVAILABLE   0x53 /* Service Unavailable */
#define PBRC_LINK_DISCONNECT       0x80 /* Transport connection has been disconnected. */
#define PBRC_INTERNAL_SERVER_ERR   0x50 /* OBRC_INTERNAL_SERVER_ERR */

/* End of PbapRespCode */

#define PBAP_ADP_WORK_FOLDER     (L"Z:\\@pbap\\")
#define PBAP_ADP_FOLDER_FILE        (L"Z:\\@pbap\\folder.tmp")
#define PBAP_ADP_LIST_FILE   (L"Z:\\@pbap\\list.tmp")
#define PBAP_ADP_ENTRY_FILE   (L"Z:\\@pbap\\entry.tmp")

/*---------------------------------------------------------------------------
 * PbapAppParmsTag type
 *
 *     Describes the tag values used in the Application Parameters OBEX header
 *     that are used on both the Phonebook Access client and server side.
 */
typedef kal_uint8 PbapAppParmsTag;

#define PBAP_TAG_ORDER              0x01        /* 1-byte, 0x00 (indexed), 0x01 (alpha), or 0x02 (phonetic) */
#define PBAP_TAG_SEARCH_VALUE       0x02        /* Variable length text string */
#define PBAP_TAG_SEARCH_ATTRIB      0x03        /* 1-byte, 0x00 (Name), 0x01 (Number), or 0x02 (Sound) */
#define PBAP_TAG_MAX_LIST_COUNT     0x04        /* 2-bytes, 0x0000 to 0xFFFF */
#define PBAP_TAG_LIST_OFFSET        0x05        /* 2-bytes, 0x0000 to 0xFFFF */
#define PBAP_TAG_FILTER             0x06        /* 8-bytes, 64 bit mask */
#define PBAP_TAG_FORMAT             0x07        /* 1-byte, 0x00 = 2.1, 0x01 = 3.0 */
#define PBAP_TAG_PHONEBOOK_SIZE     0x08        /* 2-bytes, 0x0000 to 0xFFFF */
#define PBAP_TAG_MISSED_CALLS       0x09        /* 1-byte, 0x00 to 0xFF */

/* End of PbapAppParmsTag */

/*---------------------------------------------------------------------------
 * pbap_set_folder_type_enum type
 *
 *     Flags used in the SetFolder operation.  PBAP specification requires
 *     that the PBAP_SETPB_DONT_CREATE flag is always set.
 */
typedef enum
{
    PBAP_FORWARD_FOLDER = 0,
    PBAP_BACK_FOLDER,
    PBAP_ROOT_FOLDER
} pbap_set_folder_type_enum;


typedef enum
{
    PBAP_CNF_SUCCESS,
    PBAP_CNF_FAILED,
    PBAP_CNF_TOTAL
} bt_pbap_cnf_enum;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 security_level;
} bt_pbap_register_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 register_result;
} bt_pbap_register_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
} bt_pbap_deregister_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 register_result;
} bt_pbap_deregister_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
} bt_pbap_abort_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
} bt_pbap_disconnect_ind_struct;

typedef bt_addr_struct bt_pbap_bd_addr_struct;

typedef struct
{
    kal_uint8               ref_count;
    kal_uint16              msg_len;
    bt_pbap_bd_addr_struct   bd_addr;
    kal_uint8               dev_name[BT_PBAP_MAX_DEV_NAME_LEN];
} bt_pbap_authorize_ind_struct;

typedef struct
{
    kal_uint8               ref_count;
    kal_uint16              msg_len;
    kal_uint8               cnf_code;
} bt_pbap_authorize_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    bt_pbap_bd_addr_struct bd_addr;
    kal_uint8 dev_name[BT_PBAP_MAX_DEV_NAME_LEN];
} bt_pbap_client_connect_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    kal_bool disconnect_tp_directly;
} bt_pbap_disconnect_req_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
} bt_pbap_disconnect_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint32 cm_conn_id;
    kal_uint8 cnf_code;
} bt_pbap_client_connect_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 dev_name[BT_PBAP_MAX_DEV_NAME_LEN];
} bt_pbap_client_auth_challenge_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 cancel;
    /* OBEX Authentication password */
    kal_uint8 password[20];
    /* OBEX Authentication userId */
    kal_uint8 userId[20];
    /* OBEX Authentication realm */
    kal_uint8 realm[20];
} bt_pbap_auth_cnf_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    pbap_set_folder_type_enum type;
    kal_uint16 name[PBAP_MAX_NAME_LEN/2 + 1];
} bt_pbap_set_path_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 result;
} bt_pbap_set_path_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 objectName[PBAP_MAX_NAME_LEN/2 + 1];  /* (*.vcf) */
    PbapVcardFilter filter;
    PbapVcardFormat format;
} bt_pbap_read_entry_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 result;   /* PBSTATUS */
} bt_pbap_read_entry_rsp_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 pbName[PBAP_MAX_NAME_LEN/2 + 1];
    PbapVcardFilter filter;
    PbapVcardFormat format;
    kal_uint16 maxListCount;
    kal_uint16 listStartOffset;
    kal_uint8 to_self;
} bt_pbap_read_folder_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint16 folderName[PBAP_MAX_NAME_LEN/2 + 1];
    kal_uint8 searchAttribute;
    kal_uint8 searchValue[MAX_PBAP_SEARCH_VALUE_LENGTH + 1];
    kal_uint8 searchValueLength;
    kal_uint8 order;
    kal_uint16 maxListCount;
    kal_uint16 listStartOffset;
    kal_uint8 to_self;
} bt_pbap_read_list_ind_struct;

typedef struct
{
    kal_uint8 ref_count;
    kal_uint16 msg_len;
    kal_uint8 result;
    kal_uint16 phoneBookSize; /*0xFFFF indicates have only data*/
    kal_uint16 newMissedCalls; /*0xFFFF indicates the type is not MCH*/
} bt_pbap_read_rsp_struct;

typedef bt_pbap_read_rsp_struct bt_pbap_read_folder_rsp_struct;
typedef bt_pbap_read_rsp_struct bt_pbap_read_list_rsp_struct;

#endif /* __BT_PBAP_PROFILE__ */

#endif /* __BLUETOOTH_STRUCT_H_ */

