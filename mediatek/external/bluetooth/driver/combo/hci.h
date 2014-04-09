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

#ifndef _HCI_H
#define _HCI_H

#define HCE_INQUIRY_COMPLETE               0x01
#define HCE_INQUIRY_RESULT                 0x02
#define HCE_CONNECT_COMPLETE               0x03
#define HCE_CONNECT_REQUEST                0x04
#define HCE_DISCONNECT_COMPLETE            0x05
#define HCE_AUTH_COMPLETE                  0x06
#define HCE_REMOTE_NAME_REQ_COMPLETE       0x07
#define HCE_ENCRYPT_CHNG                   0x08
#define HCE_CHNG_CONN_LINK_KEY_COMPLETE    0x09
#define HCE_MASTER_LINK_KEY_COMPLETE       0x0A
#define HCE_READ_REMOTE_FEATURES_COMPLETE  0x0B
#define HCE_READ_REMOTE_VERSION_COMPLETE   0x0C
#define HCE_QOS_SETUP_COMPLETE             0x0D
#define HCE_COMMAND_COMPLETE               0x0E
#define HCE_COMMAND_STATUS                 0x0F
#define HCE_HARDWARE_ERROR                 0x10
#define HCE_FLUSH_OCCURRED                 0x11
#define HCE_ROLE_CHANGE                    0x12
#define HCE_NUM_COMPLETED_PACKETS          0x13
#define HCE_MODE_CHNG                      0x14
#define HCE_RETURN_LINK_KEYS               0x15
#define HCE_PIN_CODE_REQ                   0x16
#define HCE_LINK_KEY_REQ                   0x17
#define HCE_LINK_KEY_NOTIFY                0x18
#define HCE_LOOPBACK_COMMAND               0x19
#define HCE_DATA_BUFFER_OVERFLOW           0x1A
#define HCE_MAX_SLOTS_CHNG                 0x1B
#define HCE_READ_CLOCK_OFFSET_COMPLETE     0x1C
#define HCE_CONN_PACKET_TYPE_CHNG          0x1D
#define HCE_QOS_VIOLATION                  0x1E
#define HCE_PAGE_SCAN_MODE_CHANGE          0x1F /* Not in 1.2 */
#define HCE_PAGE_SCAN_REPETITION_MODE      0x20
#define HCE_FLOW_SPECIFICATION_COMPLETE    0x21 /* 1.2 */
#define HCE_INQUIRY_RESULT_WITH_RSSI       0x22 /* 1.2 */
#define HCE_READ_REMOTE_EXT_FEAT_COMPLETE  0x23 /* 1.2 */
#define HCE_FIXED_ADDRESS                  0x24 /* 1.2 */
#define HCE_ALIAS_ADDRESS                  0x25 /* 1.2 */
#define HCE_GENERATE_ALIAS_REQ             0x26 /* 1.2 */
#define HCE_ACTIVE_ADDRESS                 0x27 /* 1.2 */
#define HCE_ALLOW_PRIVATE_PAIRING          0x28 /* 1.2 */
#define HCE_ALIAS_ADDRESS_REQ              0x29 /* 1.2 */
#define HCE_ALIAS_NOT_RECOGNIZED           0x2A /* 1.2 */
#define HCE_FIXED_ADDRESS_ATTEMPT          0x2B /* 1.2 */
#define HCE_SYNC_CONNECT_COMPLETE          0x2C /* 1.2 */
#define HCE_SYNC_CONN_CHANGED              0x2D /* 1.2 */
#define HCE_INQUIRY_RESULT_WITH_EIR        0x2F /* 2.1 */
#define HCE_IO_CAPABILITY_REQUEST          0x31 /* 2.1 */
#define HCE_IO_CAPABILITY_RESPONSE         0x32 /* 2.1 */
#define HCE_USER_CONFIRM_REQUSEST          0x33 /* 2.1 */
#define HCE_SIMPLE_PAIRING_COMPLETE        0x36 /* 2.1 */
#define HCE_LINK_SUPERVISION_TIMEOUT_CHNG  0x38 /* 2.1 */
#define HCE_ENHANCED_FLUSH_COMPLETE        0x39 /* 2.1 */
#define HCE_USER_PASSKEY_NOTIFICATION      0x3B /* 2.1 */
#define HCE_USER_KEYPRESS                  0x3C /* 2.1 */

#define HCE_UART_SETTING_COMPLETE          0xF0
#define HCE_BLUETOOTH_LOGO                 0xFE
#define HCE_VENDOR_SPECIFIC                0xFF
/* End of HciEventType */

/* Group: Link control commands */
#define HCC_INQUIRY                      0x0401
#define HCC_INQUIRY_CANCEL               0x0402
#define HCC_START_PERIODIC_INQ_MODE      0x0403
#define HCC_EXIT_PERIODIC_INQ_MODE       0x0404
#define HCC_CREATE_CONNECTION            0x0405
#define HCC_DISCONNECT                   0x0406
#define HCC_ADD_SCO_CONNECTION           0x0407 /* Not in 1.2 */
#define HCC_CREATE_CONNECTION_CANCEL     0x0408 /* 1.2 */
#define HCC_ACCEPT_CON_REQ               0x0409
#define HCC_REJECT_CON_REQ               0x040A
#define HCC_LINK_KEY_REQ_REPL            0x040B
#define HCC_LINK_KEY_REQ_NEG_REPL        0x040C
#define HCC_PIN_CODE_REQ_REPL            0x040D
#define HCC_PIN_CODE_REQ_NEG_REPL        0x040E
#define HCC_CHNG_CONN_PACKET_TYPE        0x040F
#define HCC_AUTH_REQ                     0x0411
#define HCC_SET_CONN_ENCRYPT             0x0413
#define HCC_CHNG_CONN_LINK_KEY           0x0415
#define HCC_MASTER_LINK_KEY              0x0417
#define HCC_REM_NAME_REQ                 0x0419
#define HCC_REM_NAME_REQ_CANCEL          0x041A /* 1.2 */
#define HCC_READ_REMOTE_FEATURES         0x041B
#define HCC_READ_REMOTE_EXT_FEATURES     0x041C /* 1.2 */
#define HCC_READ_REMOTE_VERSION          0x041D
#define HCC_READ_CLOCK_OFFSET            0x041F
#define HCC_READ_LMP_HANDLE              0x0420 /* 1.2 */
#define HCC_EXCHANGE_FIXED_INFO          0x0421 /* 1.2 */
#define HCC_EXCHANGE_ALIAS_INFO          0x0422 /* 1.2 */
#define HCC_PRIVATE_PAIRING_REQ_REPL     0x0423 /* 1.2 */
#define HCC_PRIVATE_PAIRING_REQ_NEG_REPL 0x0424 /* 1.2 */
#define HCC_GENERATED_ALIAS              0x0425 /* 1.2 */
#define HCC_ALIAS_ADDRESS_REQ_REPL       0x0426 /* 1.2 */
#define HCC_ALIAS_ADDRESS_REQ_NEG_REPL   0x0427 /* 1.2 */
#define HCC_SETUP_SYNC_CONNECTION        0x0428 /* 1.2 */
#define HCC_ACCEPT_SYNC_CON_REQ          0x0429 /* 1.2 */
#define HCC_REJECT_SYNC_CON_REQ          0x042A /* 1.2 */
#define HCC_IO_CAPABILITY_REPL           0x042B /* 2.1 */
#define HCC_USER_CONFIRM_REPL            0x042C /* 2.1 */
#define HCC_USER_CONFIRM_NEG_REPL        0x042D /* 2.1 */

#define HCC_IO_CAPABILITY_NEG_REPL       0x0434 /* 2.1 */

/* Group: Link policy commands */
#define HCC_HOLD_MODE                    0x0801
#define HCC_SNIFF_MODE                   0x0803
#define HCC_EXIT_SNIFF_MODE              0x0804
#define HCC_PARK_MODE                    0x0805
#define HCC_EXIT_PARK_MODE               0x0806
#define HCC_QOS_SETUP                    0x0807
#define HCC_ROLE_DISCOVERY               0x0809
#define HCC_SWITCH_ROLE                  0x080B
#define HCC_READ_LINK_POLICY             0x080C
#define HCC_WRITE_LINK_POLICY            0x080D
#define HCC_READ_DEFAULT_LINK_POLICY     0x080E /* 1.2 */
#define HCC_WRITE_DEFAULT_LINK_POLICY    0x080F /* 1.2 */
#define HCC_FLOW_SPECIFICATION           0x0810 /* 1.2 */
#define HCC_SNIFF_SUBRATING              0x0811 /* 2.1 */

/* Group: Host controller and baseband commands */
#define HCC_SET_EVENT_MASK               0x0C01
#define HCC_RESET                        0x0C03
#define HCC_EVENT_FILTER                 0x0C05
#define HCC_FLUSH                        0x0C08
#define HCC_READ_PIN_TYPE                0x0C09
#define HCC_WRITE_PIN_TYPE               0x0C0A
#define HCC_CREATE_NEW_UNIT_KEY          0x0C0B
#define HCC_READ_STORED_LINK_KEY         0x0C0D
#define HCC_WRITE_STORED_LINK_KEY        0x0C11
#define HCC_DEL_STORED_LINK_KEY          0x0C12
#define HCC_CHNG_LOCAL_NAME              0x0C13
#define HCC_READ_LOCAL_NAME              0x0C14
#define HCC_READ_CONN_ACCEPT_TIMEOUT     0x0C15
#define HCC_WRITE_CONN_ACCEPT_TIMEOUT    0x0C16
#define HCC_READ_PAGE_TIMEOUT            0x0C17
#define HCC_WRITE_PAGE_TIMEOUT           0x0C18
#define HCC_READ_SCAN_ENABLE             0x0C19
#define HCC_WRITE_SCAN_ENABLE            0x0C1A
#define HCC_READ_PAGE_SCAN_ACTIVITY      0x0C1B
#define HCC_WRITE_PAGE_SCAN_ACTIVITY     0x0C1C
#define HCC_READ_INQ_SCAN_ACTIVITY       0x0C1D
#define HCC_WRITE_INQ_SCAN_ACTIVITY      0x0C1E
#define HCC_READ_AUTH_ENABLE             0x0C1F
#define HCC_WRITE_AUTH_ENABLE            0x0C20
#define HCC_READ_ENCRYPT_MODE            0x0C21
#define HCC_WRITE_ENCRYPT_MODE           0x0C22
#define HCC_READ_CLASS_OF_DEVICE         0x0C23
#define HCC_WRITE_CLASS_OF_DEVICE        0x0C24
#define HCC_READ_VOICE_SETTING           0x0C25
#define HCC_WRITE_VOICE_SETTING          0x0C26
#define HCC_READ_AUTO_FLUSH_TIMEOUT      0x0C27
#define HCC_WRITE_AUTO_FLUSH_TIMEOUT     0x0C28
#define HCC_READ_NUM_BC_RETRANSMIT       0x0C29
#define HCC_WRITE_NUM_BC_RETRANSMIT      0x0C2A
#define HCC_READ_HOLD_MODE_ACTIVITY      0x0C2B
#define HCC_WRITE_HOLD_MODE_ACTIVITY     0x0C2C
#define HCC_READ_XMIT_POWER_LEVEL        0x0C2D
#define HCC_READ_SCO_FC_ENABLE           0x0C2E
#define HCC_WRITE_SCO_FC_ENABLE          0x0C2F
#define HCC_SET_CTRLR_TO_HOST_FLOW_CTRL  0x0C31
#define HCC_HOST_BUFFER_SIZE             0x0C33
#define HCC_HOST_NUM_COMPLETED_PACKETS   0x0C35
#define HCC_READ_LINK_SUPERV_TIMEOUT     0x0C36
#define HCC_WRITE_LINK_SUPERV_TIMEOUT    0x0C37
#define HCC_READ_NUM_IAC                 0x0C38
#define HCC_READ_CURRENT_IAC_LAP         0x0C39
#define HCC_WRITE_CURRENT_IAC_LAP        0x0C3A
#define HCC_READ_PAGE_SCAN_PERIOD_MODE   0x0C3B
#define HCC_WRITE_PAGE_SCAN_PERIOD_MODE  0x0C3C
#define HCC_READ_PAGE_SCAN_MODE          0x0C3D /* Not in 1.2 */
#define HCC_WRITE_PAGE_SCAN_MODE         0x0C3E /* Not in 1.2 */
#define HCC_SET_AFH_HOST_CHAN_CLASS      0x0C3F /* 1.2 */
#define HCC_READ_INQ_SCAN_TYPE           0x0C42 /* 1.2 */
#define HCC_WRITE_INQ_SCAN_TYPE          0x0C43 /* 1.2 */
#define HCC_READ_INQ_MODE                0x0C44 /* 1.2 */
#define HCC_WRITE_INQ_MODE               0x0C45 /* 1.2 */
#define HCC_READ_PAGE_SCAN_TYPE          0x0C46 /* 1.2 */
#define HCC_WRITE_PAGE_SCAN_TYPE         0x0C47 /* 1.2 */
#define HCC_READ_AFH_CHAN_ASSESS_MODE    0x0C48 /* 1.2 */
#define HCC_WRITE_AFH_CHAN_ASSESS_MODE   0x0C49 /* 1.2 */
#define HCC_READ_ANONYMITY_MODE          0x0C4A /* 1.2 */
#define HCC_WRITE_ANONYMITY_MODE         0x0C4B /* 1.2 */
#define HCC_READ_ALIAS_AUTH_ENABLE       0x0C4C /* 1.2 */
#define HCC_WRITE_ALIAS_AUTH_ENABLE      0x0C4D /* 1.2 */
#define HCC_READ_ANON_ADDR_CHNG_PARM     0x0C4E /* 1.2 */
#define HCC_WRITE_ANON_ADDR_CHNG_PARM    0x0C4F /* 1.2 */
#define HCC_RESET_FIXED_ADDR_COUNTER     0x0C50 /* 1.2 */
#define HCC_WRITE_EXT_INQUIRY_RESPONSE   0x0C52 /* 2.1 */
#define HCC_WRITE_SIMPLE_PAIRING_MODE    0x0C56 /* 2.1 */

/* Group: Informational parameters */
#define HCC_READ_LOCAL_VERSION           0x1001
#define HCC_READ_LOCAL_COMMANDS          0x1002 /* 1.2 */
#define HCC_READ_LOCAL_FEATURES          0x1003
#define HCC_READ_LOCAL_EXT_FEATURES      0x1004 /* 1.2 */
#define HCC_READ_BUFFER_SIZE             0x1005
#define HCC_READ_COUNTRY_CODE            0x1007 /* Not in 1.2 */
#define HCC_READ_BD_ADDR                 0x1009

/* Group: Status parameters */
#define HCC_READ_FAILED_CONTACT_COUNT    0x1401
#define HCC_RESET_FAILED_CONTACT_COUNT   0x1402
#define HCC_GET_LINK_QUALITY             0x1403
#define HCC_READ_RSSI                    0x1405
#define HCC_READ_AFH_CHANNEL_MAP         0x1406 /* 1.2 */
#define HCC_READ_CLOCK                   0x1407 /* 1.2 */

/* Group: Testing commands */
#define HCC_READ_LOOPBACK_MODE           0x1801
#define HCC_WRITE_LOOPBACK_MODE          0x1802
#define HCC_ENABLE_DUT                   0x1803


#define HCI_CMD_PARM_LEN         248
typedef USHORT HciCommandType;
typedef UCHAR  HciEventType;

typedef struct _HciCommand {
    UCHAR parms[HCI_CMD_PARM_LEN];
} HciCommand;

typedef struct _HciEvent {
    HciEventType event;    /* The type of HCI event received  */
    UCHAR len;             /* Length of the event parameters */
    UCHAR *parms;          /* Event-specific parameters */
} HciEvent;

#endif
