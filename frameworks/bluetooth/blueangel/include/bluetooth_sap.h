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
    *   bluetooth_sap.h
    *
    * Project:
    * --------
    *   Maui_Software
    *
    * Description:
    * ------------
    *   This file is defines SAP for BT.
    *
    * Author:
    * -------
    *   Brad Chang
    *
    *============================================================================
    *             HISTORY
    * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
    *------------------------------------------------------------------------------
    * $Revision: #1 $
    * $Modtime$
    * $Log$
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
 * 10 27 2010 sh.lai
 * [ALPS00022255] [Need Patch] [Volunteer Patch] Porting PAL interface for BT task
 * New feature ALPS00022255 : [Need Patch] [Volunteer Patch] Porting PAL interface for BT task.
 *
 * 09 03 2010 sh.lai
 * [ALPS00003522] [BLUETOOTH] Android 2.2 BLUETOOTH porting
 * Integration BT solution.
 *
 * 09 01 2010 sh.lai
 * NULL
 * Integration change. into 1036OF
    *
    * Feb 20 2009 mtk80049
    * [MAUI_01398653] [BT][PBAP] PBAP Revised Code check in
    *
    *
    * Dec 26 2008 mbj06032
    * [MAUI_01483740] MTK:Bluetooth_[1] Fatal Error (305, 8888005d) - MMI
    * add BIP SAP msg
    *
    * Dec 2 2008 mtk02126
    * [MAUI_01288486] chech in SPP/DUN new architecture
    *
    *
    * Dec 1 2008 mtk02126
    * [MAUI_01288486] chech in SPP/DUN new architecture
    *
    *
    * Nov 6 2008 mtk02126
    * [MAUI_01269441] change trace format
    *
    *
    * Nov 3 2008 mbj06038
    * [MAUI_01256277] [BT HID] Disconnect HID from PC, MS won't display disconnect message and MS also can
    *
    *
    * Oct 16 2008 mtk01624
    * [MAUI_01238166] JAVA JSR82_BT is on,but this MIDlet show BT is off
    *
    *
    * Sep 19 2008 mtk01239
    * [MAUI_01198727] MTK:Bluetooth_handset popup "device busy" and play video handset no sound .
    *
    *
    * Sep 2 2008 mbj06032
    * [MAUI_01106205] [BT GOEP] Send OPP Client SDP result (OPP server's supported format list) to MMI
    *
    *
    * Aug 10 2008 mbj07029
    * [MAUI_00792520] BT-device power off but can connectPBAP
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
    * Jun 22 2008 mtk01411
    * [MAUI_00787780] [Bluetooth] SIMAP PTS EM Mode
    *
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
    * Apr 18 2008 mbj07029
    * [MAUI_00664265] [Phonebook][PBAP] pbap profile new feature check in
    *
    *
    * Feb 19 2008 mtk01411
    * [MAUI_00621026] [Bluetooth] Check in JSR82 codes
    *
    *
    * Jan 28 2008 mbj06053
    * [MAUI_00611793] [Simple push][1]Fatal Error(305):msg_send_ext_queue() failed(88880026)-MED
    *
    *
    * Jan 9 2008 mbj06053
    * [MAUI_00068499] [Simple Push] Print failed pop up appears.
    *
    *
    * Dec 28 2007 mbj06053
    * [MAUI_00511488] BPP Simple push new feature check in
    *
    *
    * Nov 27 2007 mtk00511
    * [MAUI_00495498] [BT]Should delete the bip_r.tmp file
    *
    *
    * Oct 22 2007 mtk01239
    * [MAUI_00563448] [Bluetooth] patch BT code.
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
    * Aug 27 2007 mtk01239
    * [MAUI_00538404] [Bluetooth] Checkin Disconnect Event with reason
    *
    *
    * Jul 28 2007 mtk00560
    * [MAUI_00527043] [BT][HFP] PTS V2.1.1 TWC-BV-02 with unknown AT commands
    *
    *
    * Jul 16 2007 mtk01239
    * [MAUI_00417454] [Bluetooth][MT6601] Check in BT2.0 patch
    *
    *
    * Jul 16 2007 mtk01239
    * [MAUI_00417454] [Bluetooth][MT6601] Check in BT2.0 patch
    *
    *
    * Jul 16 2007 mtk01239
    * [MAUI_00417220] [EM] Bluetooth RF Test Item Support
    *
    *
    * Jul 1 2007 mtk01239
    * [MAUI_00411326] [Bluetooth] SPP with SCO
    *
    *
    * May 7 2007 mtk01239
    * [MAUI_00389758] [Bluetooth] Checkin JSR82 code of BTStack
    *
    *
    * Apr 23 2007 mtk01411
    * [MAUI_00384849] [Bluetooth SPP Client] SPP Client and number of virtual ports modification
    *
    *
    * Apr 20 2007 mtk00560
    * [MAUI_00384677] [MMI][BT] to check-in for CR423159
    *
    *
    * Apr 16 2007 MTK01476
    * [MAUI_00381908] [New Feature]Bluetooth Basic Printing Profile check in
    *
    *
    * Mar 27 2007 mtk01239
    * [MAUI_00376540] [Bluetooth] patch for SAP error checkin
    *
    *
    * Mar 25 2007 mtk01239
    * [MAUI_00375665] [Bluetooth] GAVDTP, AVCTP, AVDTP v1.2
    *
    *
    * Mar 25 2007 mtk01239
    * [MAUI_00375665] [Bluetooth] GAVDTP, AVCTP, AVDTP v1.2
    *
    *
    * Mar 25 2007 mtk01239
    * [MAUI_00375665] [Bluetooth] GAVDTP, AVCTP, AVDTP v1.2
    *
    *
    * Mar 19 2007 mtk01239
    * [MAUI_00373398] [Bluetooth] open panic mechenism
    *
    *
    * Feb 16 2007 mtk00560
    * [MAUI_00367691] [BT][HID] HID new feature check-in
    *
    *
    * Jan 15 2007 mtk01411
    * [MAUI_00356818] [1] Fatal Error: msg_send_ext_queue () failed 1=305 2=88880026 - L4
    *
    *
    * Dec 3 2006 mtk01411
    * [MAUI_00347961] Add Bluetooth SIM Access Profile related codes
    * Add SIM Access Profile's SAP
    *
    * Dec 3 2006 mtk01411
    * [MAUI_00347950] Add Bluetooth Fax Profile related codes
    * Add Fax Profile related SAP
    *
    * Nov 6 2006 mtk01239
    * [MAUI_00340829] Bluetooth, Report connection number to MMI
    *
    *
    * Nov 4 2006 mtk01239
    * [MAUI_00340620] [Bluetooth] patch for Wifi coexistence turn AFH and PTA
    *
    *
    * Oct 28 2006 mtk00560
    * [MAUI_00336162] [1] Assert fail: g_mmi_bth_cntx.paired_dev BTMMICm.c 3304 - MMI
    *
    *
    * Sep 11 2006 mtk00511
    * [MAUI_00329160] [GOEP]Check in GOEP profile and OBEX stack
    *
    *
    * Aug 14 2006 mtk01239
    * [MAUI_00321407] [BT]update Bluetooth code
    *
    *
    * Aug 14 2006 mtk01239
    * [MAUI_00321407] [BT]update Bluetooth code
    *
    *
    * Aug 14 2006 mtk01239
    * [MAUI_00321407] [BT]update Bluetooth code
    *
    *
    * Jul 31 2006 mtk01239
    * [MAUI_00214015] [BT] update BT code and add OPP, FTP SDP record
    *
    *
    * Jul 24 2006 mtk01411
    * [MAUI_00212512] [BT]Add Test module codes
    * Add new msg type
    *
    * Jul 16 2006 mtk01239
    * [MAUI_00210782] [Bluetooth][ESI]update Bluetooth codes
    *
    *
    * Jul 16 2006 MTK00758
    * [MAUI_00210638] [BT]IT check in
    *
    *
    * Jun 19 2006 mtk00511
    * [MAUI_00203742] [AT][NewFeature]command for read BT address
    *
    *
    * Jun 12 2006 mtk00560
    * [MAUI_00201895] [ESI]to integrate HSP/HFP FSM code
    *
    *
    * Jun 5 2006 mtk01239
    * [MAUI_00199721] [Bluetooth][ESI] update bt code update
    *
    *
    * May 22 2006 mtk00560
    * [MAUI_00195576] [ESI]HSP/HFP adp integration
    *
    *
    * May 22 2006 mtk00676
    * [MAUI_00194161] [AVRCP] Initial version for MTK  AVRCP solutions
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
    * Apr 28 2006 mtk00560
    * [MAUI_00190871] [ESI] esi MMI CM, HFP, and HSP adp integration
    *
    *
    * Apr 24 2006 mtk01239
    * [MAUI_00189553] ESI Bluetooth project update
    *
    *
    * Apr 24 2006 mtk01239
    * [MAUI_00189553] ESI Bluetooth project update
    * update
    *
    *
    * Apr 24 2006 mtk01239
    * [MAUI_00189553] ESI Bluetooth project update
    * update
    *
    * Apr 24 2006 mtk01239
    * [MAUI_00189553] ESI Bluetooth project update
    * update
    *
    * Apr 18 2006 mtk00560
    * [MAUI_00188317] [MMI][BCHS] to move the bt sap definition from bt_sap.h to bluetooth_sap.h file
    *
    *
    * Apr 17 2006 MTK00758
    * [MAUI_00188141] [Bluetooth]Power on/off and reset procedure
    *
    *
    * Apr 4 2006 MTK00758
    * [MAUI_00184507] [BT]IT check in
    *
    *
    * Apr 3 2006 mtk00758
    * [MAUI_00184485] [New feature] Add task to simulate UART driver for unitest
    *
    *
    * Mar 13 2006 mtk00758
    * [MAUI_00178684] [Bluetooth]ESI bluetooth protocol stack
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
#ifdef BTMTK_ON_WISE
    MSG_ID_BT_NOTIFY_EVM_IND = BT_MSG_CODE_BEGIN,
#else   /*  BTMTK_ON_WISE */
    /* After add/delete message group, pls modify btmtk_adp_check_msg_id_range() accordingly */
    MSG_ID_BT_NOTIFY_EVM_IND = MSG_ID_BT_START,
    MSG_ID_TIMER_EXPIRY,
#endif  /*  BTMTK_ON_WISE */
    MSG_ID_BT_HOST_WAKE_UP_IND,
#ifdef BTMTK_ON_WISE
    MSG_ID_BT_L2CAP_TEST_CMD_REQ,
    MSG_ID_BT_SDP_TEST_ACTIVATE_REQ,
    MSG_ID_BT_DATA_TO_READ_IND,
    MSG_ID_BT_READY_IND,
    MSG_ID_BT_TEST_CMD_REQ,
    MSG_ID_BT_L4C_AT_GENERAL_CNF,
    MSG_ID_BT_L4C_TEST_CMD_CNF,
    MSG_ID_BT_PCM_LB_REQ,
    MSG_ID_BT_PCM_LB_CNF,
    MSG_ID_BT_TEST_MODE_REQ,
    MSG_ID_BT_SET_UART_RX_SPEED_LOW_REQ,
    MSG_ID_BT_SET_UART_RX_SPEED_HIGH_REQ,
    MSG_ID_BT_GET_BT_VERSION_REQ,
    MSG_ID_BT_GET_BT_VERSION_CNF,
    MSG_ID_BT_ENGINEER_MODE_LOG_MASK_REQ,
    MSG_ID_BT_ENGINEER_MODE_LOG_MASK_CNF,
    MSG_ID_BT_ENGINEER_MODE_TXRX_TEST_CNF,
    MSG_ID_BT_ENGINEER_MODE_POWER_TEST_CNF,
    MSG_ID_BT_END = MSG_ID_BT_ENGINEER_MODE_POWER_TEST_CNF,
#else  /*  BTMTK_ON_WISE */    
    /****************************************************************************
     *  Request Messages: WMT -> BT
     ****************************************************************************/
    MSG_ID_WMT_BT_QUERY_STATE_REQ,
    MSG_ID_WMT_BT_QUERY_STATE_CNF,
    MSG_ID_WMT_BT_SET_BWCS_REQ,
    MSG_ID_WMT_BT_QUERY_RSSI_REQ,
    MSG_ID_WMT_BT_QUERY_RSSI_IND,
    MSG_ID_WMT_BT_SET_RX_RANGE_REQ,
    MSG_ID_WMT_BT_SET_RX_RANGE_CNF,
    MSG_ID_WMT_BT_SET_DEFAULT_TX_POWER_REQ,
    MSG_ID_WMT_BT_SET_DEFAULT_TX_POWER_CNF,
    MSG_ID_WMT_BT_UPDATE_CONN_TX_POWER_REQ,
    MSG_ID_WMT_BT_UPDATE_CONN_TX_POWER_CNF,
    MSG_ID_WMT_BT_STATUS_IND,

    MSG_ID_BT_SSP_DEBUG_MODE_REQ,
    MSG_ID_BT_SSP_DEBUG_MODE_CNF,
    /* + Debug message */
    MSG_ID_BT_DBG_QUERY_CONTEXT_REQ,
    MSG_ID_BT_DBG_QUERY_CONTEXT_CNF,
    MSG_ID_BT_DBG_HCI_CONTEXT_IND,
    MSG_ID_BT_DBG_ME_CONTEXT_IND,
    MSG_ID_BT_DBG_RMG_CONTEXT_IND,
    MSG_ID_BT_DBG_L2CAP_CONTEXT_IND,
    MSG_ID_BT_DBG_RXBUF_CONTEXT_IND,
    MSG_ID_BT_DBG_SDPS_CONTEXT_IND,
    MSG_ID_BT_DBG_SDPC_CONTEXT_IND,
    MSG_ID_BT_DBG_RFC_CONTEXT_IND,
    MSG_ID_BT_DBG_A2MP_CONTEXT_IND,
    MSG_ID_BT_DBG_ATT_CONTEXT_IND,
    MSG_ID_BT_DBG_GATT_CONTEXT_IND,
    MSG_ID_BT_DBG_WDDEV_CONTEXT_IND,
    MSG_ID_BT_DBG_WDTRANS_CONTEXT_IND,
    MSG_ID_BT_DBG_MANUAL_DUMP_REQ,
    /* - Debug message */
    MSG_ID_BT_ENGINEER_MODE_LOG_MASK_REQ,
    MSG_ID_BT_ENGINEER_MODE_LOG_MASK_CNF,
    MSG_ID_BT_ENGINEER_MODE_LOG_TO_FILE_REQ,
    MSG_ID_BT_ENGINEER_MODE_LOG_TO_FILE_CNF,
    MSG_ID_BT_SET_SCO_ACCEPT_REQ,
    MSG_ID_BT_SET_SCO_ACCEPT_CNF,
    MSG_ID_BT_SET_DUAL_PCM_SETTING_REQ,
    MSG_ID_BT_SET_DUAL_PCM_SETTING_CNF,
    MSG_ID_BT_SET_DUAL_PCM_SWITCH_REQ,
    MSG_ID_BT_SET_DUAL_PCM_SWITCH_CNF,
    MSG_ID_BT_TEST_CMD_REQ,
    MSG_ID_BT_TEST_CMD_CNF,
    MSG_ID_BT_TEST_MODE_REQ,
    MSG_ID_BT_TEST_MODE_CNF,
    MSG_ID_BT_BTTM_SWITCH_PORT_REQ,
    MSG_ID_BT_BTTM_SWITCH_PORT_CNF,
    MSG_ID_BT_END = MSG_ID_BT_BTTM_SWITCH_PORT_CNF,
#endif

    /* GAP */
    #include "bluetooth_gap_message.h"

    /* SDP */
    #include "bluetooth_sdp_message.h"

    /* A2DP */
    #include "bluetooth_a2dp_message.h"

    /* AVRCP */
    #include "bluetooth_avrcp_message.h"

    /* BIP */
    #include "bluetooth_bipi_message.h"
    #include "bluetooth_bipr_message.h"
    /* BPP */
    #include "bluetooth_bpp_message.h"
    /* DUN */
    /* FAX */
    /* FTP */
    #include "bluetooth_ftpc_message.h"
    #include "bluetooth_ftps_message.h"
    /* GOEP */
    #include "bluetooth_goepc_message.h"
    #include "bluetooth_goeps_message.h"
    /* HFG */
    #include "bluetooth_hfg_message.h"
    /* HID */
    #include "bluetooth_hid_message.h"
    /* HSG */
    /* OPP */
    #include "bluetooth_oppc_message.h"
    #include "bluetooth_opps_message.h"
    /* PBAP */
    #include "bluetooth_pbap_message.h"
    /* SIMAP */
    #include "bluetooth_simap_message.h"
    /* SPP */
    #include "bluetooth_spp_message.h"
    /* JSR82 */
    #include "bluetooth_jsr82_message.h"
    /* CHN */
    #include "bluetooth_chn_message.h"

    /* MAP */
    #include "bluetooth_map_message.h"

    #include "bluetooth_prx_message.h"

    /* PAN */
    #include "bluetooth_pan_message.h"	
   
	/* TIME */
	#include "bluetooth_time_message.h"

    /* HDP */
    #include "bluetooth_hdp_message.h"

    /* HTP */
    #include "bluetooth_htp_message.h"	

    /* FMP */
    #include "bluetooth_fmp_message.h"	

    MSG_ID_BT_CUSTOM_MSG_ID_BEGIN,

    /* Below is the message interface for other modules */
    /* PAL */
    #include "pal_sap.h"

	/* SDAP Tester */
	#include "bluetooth_sdap_tester_message.h"

    #ifndef BTMTK_ON_WISE
    MSG_ID_END = 0xFFFFFFFF
    #else   /* BTMTK_ON_WISE */
    MSG_TAG_BT_SAP_END_MSG,
    #endif  /* BTMTK_ON_WISE */
