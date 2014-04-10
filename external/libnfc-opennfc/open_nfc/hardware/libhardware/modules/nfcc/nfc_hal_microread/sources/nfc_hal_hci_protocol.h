/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __NFC_HAL_HCI_PROTOCOL_H
#define __NFC_HAL_HCI_PROTOCOL_H

#define P_HCI_RECEIVED_FRAME_MAX_LENGTH 280

#define P_UNKNOW_HCI_VALUE                    0xFF
/*******************************************************************************
   Error codes
*******************************************************************************/

#define ETSI_ERR_ANY_OK                       0x00   /* command completed successfully */
#define ETSI_ERR_ANY_E_NOT_CONNECTED          0x01   /* the destination host is not connected */
#define ETSI_ERR_ANY_E_CMD_PAR_UNKNOWN        0x02   /* the format of the command parameters is wrong */
#define ETSI_ERR_ANY_E_NOK                    0x03   /* command was rejected and/or not completed */
#define ETSI_ERR_ADM_E_NO_PIPES_AVAILABLE     0x04   /* the requested pipe is not available */
#define ETSI_ERR_ANY_E_REG_PAR_UNKNOWN        0x05   /* the registry parameter identifier is either
                                                        unknown to the registry or an optional registry
                                                        parameter is not implemented */
#define ETSI_ERR_ANY_E_PIPE_NOT_OPENED        0x06   /* the pipe is not open */
#define ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED      0x07   /* the command is not supported by the gate */
#define ETSI_ERR_ANY_E_TIMEOUT                0x09   /* an application level time out occurred */
#define ETSI_ERR_ANY_E_REG_ACCESS_DENIED      0x0A   /* permission denied to write a value to a registry */
#define ETSI_ERR_ANY_E_PIPE_ACCESS_DENIED     0x0B   /* permission denied to access to a pipe */

#define ETSI_ERR_WR_RF_ERROR                  0x10   /* RF Error */

#define HCI2_ERR_HARDWARE                     0x37   /* hardware failure */

/*******************************************************************************
   Commands for all gates
*******************************************************************************/

#define ETSI_CMD_ANY_SET_PARAMETER           0x01 /* the command to set a parameter in a registry */
#define ETSI_CMD_ANY_GET_PARAMETER           0x02 /* the command to get a parameter from a registry */
#define ETSI_CMD_ANY_OPEN_PIPE               0x03 /* the command to open a pipe */
#define ETSI_CMD_ANY_CLOSE_PIPE              0x04 /* the command to close a pipe */
#define ETSI_ADM_CREATE_PIPE                 0x10 /* the command to create a pipe */

/*******************************************************************************
   Adiministration Parameters
*******************************************************************************/

#define HCI2_PAR_ADM_SESSION_ID              0x01
#define HCI2_PAR_ADM_SESSION_ID_SIZE            8

#define HCI2_PAR_ADM_WHITE_LIST              0x03

/*******************************************************************************
   Card Events
*******************************************************************************/

#define ETSI_EVT_SEND_DATA                   0x10
#define ETSI_EVT_FIELD_ON                    0x11
#define ETSI_EVT_CARD_DEACTIVATED            0x12
#define ETSI_EVT_CARD_ACTIVATED              0x13
#define ETSI_EVT_FIELD_OFF                   0x14

/*******************************************************************************
   Card Parameters A
*******************************************************************************/

#define HCI2_PAR_MCARD_ISO_A_MODE            0x01
#define HCI2_PAR_MCARD_ISO_A_UID             0x02
#define HCI2_PAR_MCARD_ISO_A_SAK             0x03
#define HCI2_PAR_MCARD_ISO_A_ATQA            0x04
#define HCI2_PAR_MCARD_ISO_A_APPLI_DATA      0x05
#define HCI2_PAR_MCARD_ISO_A_APPLI_DATA_MSG_SIZE      0x14
#define HCI2_PAR_MCARD_ISO_A_FWI_SFGI        0x06
#define HCI2_PAR_MCARD_ISO_A_CID_SUPPORT     0x07
#define HCI2_PAR_MCARD_ISO_A_CLT_SUPPORT     0x08
#define HCI2_PAR_MCARD_ISO_A_DATARATEMAX     0x09

/*******************************************************************************
   Card Parameters B
*******************************************************************************/

#define HCI2_PAR_MCARD_ISO_B_MODE            0x01
#define HCI2_PAR_MCARD_ISO_B_UID             0x02
#define HCI2_PAR_MCARD_ISO_B_AFI             0x03
#define HCI2_PAR_MCARD_ISO_B_ATQB            0x04
#define HCI2_PAR_MCARD_ISO_B_ATTRIB_INF_RESP 0x05
#define HCI2_PAR_MCARD_ISO_B_ATTRIB_INF_RESP_MSG_SIZE      0x14
#define HCI2_PAR_MCARD_ISO_B_DATARATEMAX     0x06

/*******************************************************************************
   Reader Commands
*******************************************************************************/

#define ETSI_CMD_WR_XCHG_DATA                   0x10
#define ETSI_TIMEOUT_READER_XCHG_DATA_ENABLE    0x10

/*******************************************************************************
   Reader Events
*******************************************************************************/

#define ETSI_EVT_READER_REQUESTED               0x10
#define ETSI_EVT_END_OPERATION                  0x11
#define ETSI_EVT_TARGET_DISCOVERED              0x10

/*******************************************************************************
   Host identifiers
*******************************************************************************/
#define ETSI_HOST_HDS								  0x01	/* Handset */
#define ETSI_HOST_UICC                         0x02   /* UICC */
#define ETSI_HOST_RFU3                         0x03   /* RFU */

/*******************************************************************************
   Message types
*******************************************************************************/

#define ETSI_MSG_TYPE_MASK          0xC0 /* */
#define ETSI_MSG_INFO_MASK          0x3F /* */
#define ETSI_MSG_TYPE_CMD           0x00 /* */
#define ETSI_MSG_TYPE_EVT           0x40 /* */
#define ETSI_MSG_TYPE_ANS           0x80 /* */

/*******************************************************************************
********************************************************************************

   Specifics Constants

*******************************************************************************
*******************************************************************************/

/*******************************************************************************
   NFC Controller Management Parameters
*******************************************************************************/

#define HCI2_PAR_MGT_VERSION_HARDWARE             0x01
#define HCI2_PAR_MGT_VERSION_LOADER               0x02
#define HCI2_PAR_MGT_PATCH_STATUS                 0x05

#define HCI2_PAR_MGT_AVAILABLE_MODE_CARD          0x07
#define HCI2_PAR_MGT_AVAILABLE_MODE_CARD_MASK     0x003F

#define HCI2_PAR_MGT_AVAILABLE_MODE_READER        0x08
#define HCI2_PAR_MGT_AVAILABLE_MODE_READER_MASK   0x09FF

#define HCI2_PAR_MGT_INIT_CONFIG_CURRENT          0x0A
#define HCI2_PAR_MGT_INIT_CONFIG_CURRENT_MSG_SIZE    4

#define HCI2_PAR_MGT_INIT_CONFIG_BACKUP           0x0B
#define HCI2_PAR_MGT_INIT_CONFIG_BACKUP_MSG_SIZE     4

/* #define HCI2_PAR_MGT_CARD_DETECT_PULSE            0x0E */

#define HCI2_PAR_MGT_SERIAL_ID                    0x0F
#define HCI2_PAR_MGT_INIT_STATUS                  0x10

#define HCI2_PAR_MGT_HDS_OWNER_CARD               0x17
#define HCI2_PAR_MGT_HDS_OWNER_CARD_MSG_SIZE      0x09

#define HCI2_PAR_MGT_HDS_OWNER_READER             0x18
#define HCI2_PAR_MGT_HDS_OWNER_READER_MSG_SIZE    0x0C

#define HCI2_PAR_MGT_SIM_OWNER_CARD               0x19
#define HCI2_PAR_MGT_SIM_OWNER_CARD_MSG_SIZE      0x09

#define HCI2_PAR_MGT_SIM_OWNER_READER             0x1A

#define HCI2_PAR_MGT_SWP_GATE_ACCESS              0x1D
#define HCI2_PAR_MGT_CURRENT_RF_LOCK_CARD         0x1E
#define HCI2_PAR_MGT_CURRENT_RF_LOCK_READER       0x1F
#define HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD         0x21
#define HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER       0x22
#define HCI2_PAR_MGT_CARD_DETECT_CONFIG_CURRENT   0x23
#define HCI2_PAR_MGT_CARD_DETECT_STATE            0x25
#define HCI2_PAR_MGT_STANDBY_RF_LOCK_CARD         0x2C

/* RF lock values */
#define HCI2_VAL_LOCKED                           0x01
#define HCI2_VAL_UNLOCKED                         0x00

/*******************************************************************************
   NFC Controller Instance Parameters
*******************************************************************************/

#define HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM     0x01
#define HCI2_PAR_INSTANCES_CURRENT_MREAD_GRANTED_TO_SIM     0x02
#define HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM     0x03
#define HCI2_PAR_INSTANCES_CURRENT_MCARD_GRANTED_TO_SIM     0x04
#define HCI2_PAR_INSTANCES_STANDBY_MCARD_GRANTED_TO_SIM     0x05

/*******************************************************************************
   NFC Controller Management Commands, Event & answer
*******************************************************************************/

#define HCI2_CMD_MGT_IDENTITY                   0x39
#define HCI2_ANS_MGT_IDENTITY_MSG_SIZE            36

#define HCI2_CMD_MGT_TEST_SELF_CHIP             0x34
#define HCI2_CMD_MGT_STANDBY                    0x3B     /* Command used to request standby mode */

#define HCI2_EVT_MGT_HDS_SET_UP_SWPLINE         0x3F     /* 0x7F & 0x3F */

#define HCI2_EVT_MGT_HCI_INACTIVITY             0x20     /* Not a MR event, a pseudo event generated internally when HCI inactivity timeout occurs */
#define HCI2_EVT_MGT_HCI_ACTIVITY               0x21     /* Not a MR event, a pseudo event generated internally when HCI activity is required   */
#define HCI2_EVT_MGT_HCI_WAKE_UP                0x22     /* Not a MR event, a pseudo event generated internally when HCI link is restarted by MR   */

#define HCI2_VAL_MGT_PATCH_START                0xC0
#define HCI2_VAL_MGT_PATCH_CONFIRMED            0x40
#define HCI2_VAL_MGT_PATCH_LOADER               0x80

#define HCI2_VAL_MGT_INIT_CONFIRMED             0x20

#define HCI2_VAL_MGT_TEST_SWP_NONE              0x00
#define HCI2_VAL_MGT_TEST_SWP_LOOPBACK          0x01
#define HCI2_VAL_MGT_TEST_SWP_FORCE_0           0x02

#define HCI2_VAL_MGT_SWP_POWER_LEVEL_00         0x00
#define HCI2_VAL_MGT_SWP_POWER_LEVEL_18         0x18
#define HCI2_VAL_MGT_SWP_POWER_LEVEL_30         0x30

/*******************************************************************************
   Reader Commands / Parameters
*******************************************************************************/

#define HCI2_EVT_MREADER_DISCOVERY_OCCURED      0x10
#define HCI2_EVT_MREAD_CARD_FOUND               0x3D
#define HCI2_EVT_MREADER_DISCOVERY_START_SOME   0x3E
#define HCI2_EVT_MREADER_SIM_REQUEST            0x3F

#define HCI2_CMD_MREAD_DEACTIVATION				   0x3B	/* Request deactivation for ISO A and ISO B */
#define HCI2_CMD_MREAD_IS_CARD_FOR_SIM          0x3C
#define HCI2_CMD_MREAD_SELECT_HOST              0x3E
#define HCI2_CMD_MREAD_SUBSCRIBE                0x3F


#define HCI2_PAR_MREADER_ISO_B_HIGH_LAYER_DATA_MSG_SIZE  0x14

#define HCI2_PAR_MREAD_GEN_POLLING_OPTION       0x22
#define HCI2_PAR_MREAD_GEN_COLLISION_FOUND      0x25
#define HCI2_PAR_MREAD_GEN_P2P_ACTIVE           0x28

#define HCI2_PAR_MREAD_FELICA_LIST_CARD         0x0B

#define HCI2_PAR_MREAD_GEN_PICOREAD_OPTION      0x20

#define HCI2_PAR_MREAD_GEN_NFC_TCL_TIMEOUT_MAX  0x29
/*******************************************************************************
   Card Commands
*******************************************************************************/
#ifdef DCMD_CARD_CONFIG
 #define HCI2_CMD_MCARD_SUBSCRIBE               0x3F
#else
 #define HCI2_CMD_MCARD_SUBSCRIBE               P_HCI_SERVICE_CMD_SET_PROPERTIES
#endif /* DCMD_CARD_CONFIG*/

#define HCI2_PAR_MCARD_GEN_ROUTING_TABLE					0x23		
#define HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED		0x24
#define HCI2_VAL_MCARD_MODE_DISABLE             0xFF
#define HCI2_VAL_MCARD_MODE_ENABLE              0x02

/*******************************************************************************
   UICC
*******************************************************************************/

#define HCI2_VAL_HANDSET_AUTH_YES               0x01
#define HCI2_VAL_HANDSET_AUTH_NO                0x00

#define HCI2_EVT_CONNECTIVITY                   0x10
#define HCI2_EVT_TRANSACTION                    0x12
#define HCI2_EVT_OPERATION_ENDED                0x13

#define HCI2_PAR_MGT_SWP_SHDLC_STATUS           0x20

/*******************************************************************************
   P2P: cmd, event & parameters
*******************************************************************************/
#define HCI2_EVT_P2P_INITIATOR_EXCHANGE_TO_RF   0x20
#define HCI2_EVT_P2P_INITIATOR_EXCHANGE_FROM_RF 0x21

#define HCI2_PAR_P2P_INITIATOR_GI               0x01
#define HCI2_PAR_P2P_INITIATOR_BRS              0x02
#define HCI2_PAR_P2P_INITIATOR_GT               0x03
#define HCI2_PAR_P2P_INITIATOR_MIUX_WKS_LTO     0x05

#define HCI2_PAR_P2P_TARGET_MODE                0x01
#define HCI2_PAR_P2P_TARGET_TO                  0x03
#define HCI2_PAR_P2P_TARGET_GT                  0x04
#define HCI2_PAR_P2P_TARGET_MIUX_WKS_LTO        0x05
#define HCI2_PAR_P2P_TARGET_ISOA_ACTIVE         0x06

/*******************************************************************************
   Load Firmware Commands
*******************************************************************************/

#define HCI2_CMD_OS_LOADER_CONFIRM              0x13
#define HCI2_CMD_OS_LOADER_ENTER                0x14
#define HCI2_CMD_OS_LOADER_LOADPAGE             0x16
#define HCI2_CMD_OS_LOADER_CHECKCRC             0x17
#define HCI2_CMD_OS_LOADER_QUIT                 0x18
#define HCI2_CMD_OS_LOADER_SET_VTHSET           0x19

/*******************************************************************************
   SE Parameters
*******************************************************************************/

#define HCI2_PAR_SE_SETTINGS_DEFAULT            0x03
#define HCI2_PAR_SE_SETTINGS_CURRENT            0x04
#define HCI2_PAR_SE_SETTINGS_STANDBY            0x06
#define HCI2_EVT_SE_STATUS                      0x3C  /* 0x7C & 0x3F */

/*******************************************************************************
   Protocols
*******************************************************************************/

#define HCI2_PROTOCOL_READER_ISO_14443_4_B    0x0001  /* Reader ISO 14443 B level 4 */
#define HCI2_PROTOCOL_READER_TYPE_1           0x0002  /* Reader Type 1 */
#define HCI2_PROTOCOL_READER_ISO_14443_4_A    0x0004  /* Reader ISO 14443 A level 4 */
#define HCI2_PROTOCOL_READER_ISO_15693_3      0x0008  /* Reader ISO 15693 level 3 */
#define HCI2_PROTOCOL_READER_ISO_15693_2      0x0010  /* Reader ISO 15693 level 2 */
#define HCI2_PROTOCOL_READER_FELICA           0x0020  /* Reader FeliCa */
#define HCI2_PROTOCOL_READER_ISO_14443_3_B    0x0040  /* Reader ISO 14443 B level 3 */
#define HCI2_PROTOCOL_READER_B_PRIME          0x0080  /* Reader B PRIME             */
#define HCI2_PROTOCOL_READER_ISO_14443_3_A    0x0100  /* Reader ISO 14443 A level 3 */
#define HCI2_PROTOCOL_READER_P2P_INITIATOR    0x0200  /* P2P Initiator */
#define HCI2_PROTOCOL_READER_KOVIO            0x0800  /* Kovio */

#define HCI2_PROTOCOL_CARD_ISO_14443_4_B      0x0001  /* Card ISO 14443 B level 4 */
#define HCI2_PROTOCOL_CARD_BPRIME             0x0002  /* Card B Prime */
#define HCI2_PROTOCOL_CARD_ISO_14443_4_A      0x0004  /* Card ISO 14443 A level 4 */
#define HCI2_PROTOCOL_CARD_ISO_15693_3        0x0008  /* Card ISO 15693 level 3 */
#define HCI2_PROTOCOL_CARD_ISO_15693_2        0x0010  /* Card ISO 15693 level 2 */
#define HCI2_PROTOCOL_CARD_FELICA             0x0020  /* Card FeliCa */
#define HCI2_PROTOCOL_CARD_ISO_14443_B_2      0x0040  /* Card ISO 14443 B level 2 */
#define HCI2_PROTOCOL_CARD_CUSTOM             0x0080  /* Card customized protocol */
#define HCI2_PROTOCOL_CARD_P2P_TARGET         0x0100  /* P2P Target */

/* HCI2_PAR_MGT_AVAILABLE_MODE_P2P mapping */
#define HCI2_PROTOCOL_P2P_TARGET              0x0001  /* P2P Target */
#define HCI2_PROTOCOL_P2P_INITIATOR           0x0002  /* P2P Initiator */

/*******************************************************************************
   Pipes
*******************************************************************************/

#define HCI2_PIPE_ID_LMS                        0x00  /* Pipe ID of Link management            */
#define HCI2_PIPE_ID_ADMIN                      0x01  /* Pipe ID of Administration             */
#define HCI2_PIPE_ID_MGT                        0x02  /* Pipe ID of Pipe Management            */
#define HCI2_PIPE_ID_OS                         0x03  /* Pipe ID of OS (patchs)                */
#define HCI2_PIPE_ID_HDS_IDT                    0x05  /* Pipe ID of Identity                   */

#define HCI2_PIPE_ID_HDS_MCARD_ISO_B            0x08  /* Pipe ID of Card ISO 14443 B level 4   */
#define HCI2_PIPE_ID_HDS_MCARD_ISO_BPRIME       0x09  /* Pipe ID of Card B Prime */
#define HCI2_PIPE_ID_HDS_MCARD_ISO_A            0x0A  /* Pipe ID of Card ISO 14443 A level 4   */
#define HCI2_PIPE_ID_HDS_MCARD_ISO_15_3         0x0B  /* Pipe ID of Card ISO 15693 level 3     */
#define HCI2_PIPE_ID_HDS_MCARD_ISO_15_2         0x0C  /* Pipe ID of Card ISO 15693 level 2     */
#define HCI2_PIPE_ID_HDS_MCARD_NFC_T3           0x0D  /* Pipe ID of Card T3 */
#define HCI2_PIPE_ID_HDS_MCARD_ISO_B_2          0x0E  /* Pipe ID of Card ISO 14443 B level 2   */
#define HCI2_PIPE_ID_HDS_MCARD_CUSTOM           0x0F  /* Pipe ID of Card customized protocol   */
#define HCI2_PIPE_ID_HDS_P2P_TARGET             0x1F /* Pipe ID of P2P Target                  */

#define HCI2_PIPE_ID_HDS_MREAD_ISO_B            0x10  /* Pipe ID of Reader ISO 14443 B level 4 */
#define HCI2_PIPE_ID_HDS_MREAD_NFC_T1           0x11  /* Pipe ID of Reader Type 1 */
#define HCI2_PIPE_ID_HDS_MREAD_ISO_A            0x12  /* Pipe ID of Reader ISO 14443 A level 4 */
#define HCI2_PIPE_ID_HDS_MREAD_ISO_15_3         0x13  /* Pipe ID of Reader ISO 15693 level 3   */
#define HCI2_PIPE_ID_HDS_MREAD_ISO_15_2         0x14  /* Pipe ID of Reader ISO 15693 level 2   */
#define HCI2_PIPE_ID_HDS_MREAD_NFC_T3           0x15  /* Pipe ID of Reader T3 */
#define HCI2_PIPE_ID_HDS_MREAD_ISO_B_3          0x16  /* Pipe ID of Reader ISO 14443 B level 3 */
#define HCI2_PIPE_ID_HDS_MREAD_BPRIME           0x17  /* Pipe ID of Reader Bprime */
#define HCI2_PIPE_ID_HDS_MREAD_ISO_A_3          0x18  /* Pipe ID of Reader ISO 14443 A level 3 */
#define HCI2_PIPE_ID_HDS_MREAD                  0x1A  /* Pipe ID of Generic Reader             */
#define HCI2_PIPE_ID_HDS_P2P_INITIATOR          0x20  /* Pipe ID of P2P Initiator              */
#define HCI2_PIPE_ID_HDS_MCARD                  0x21  /* Pipe ID of Generic Card               */
#define HCI2_PIPE_ID_HDS_MREAD_KOVIO            0x22  /* Pipe ID of Reader Kovio               */

#define HCI2_PIPE_ID_HDS_SIM_CONNECTIVITY       0x07  /* Pipe ID of UICC Gate                  */
#define HCI2_PIPE_ID_HDS_MREAD_GEN              0x1B  /* Pipe ID of generic Reader Service     */
#define HCI2_PIPE_ID_HDS_STACKED_ELEMENT        0x1C  /* Pipe ID of Stacked Element            */
#define HCI2_PIPE_ID_HDS_INSTANCES              0x1D  /* Pipe ID of Instances                  */
#define HCI2_PIPE_ID_HDS_TEST_RF                0x1E  /* Pide ID of Test RF                    */

#define HCI2_PIPE_ID_NULL                       0x7F
/*******************************************************************************
   ELEMENT
*******************************************************************************/

#define HCI2_ELT_ID_HDS                       0x01   /* SIM ELEMENT */
#define HCI2_ELT_ID_SIM                       0x02   /* SIM ELEMENT */

#endif /* __NFC_HAL_HCI_PROTOCOL_H */
