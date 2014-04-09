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
*  permission of MediaTek Inc. (C) 2008
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

/*******************************************************************************
 * Filename:
 * ---------
 *  pal_hci_struct.h
 *
 * Project:
 * --------
 *  MAUI
 *
 * Description:
 * ------------
 *  PAL related MSG structure
 *
 * Author:
 * -------
 *  Nelson Chang (mtk02783)
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Log$
 *
 * 05 27 2011 dlight.ting
 * [ALPS00049038] [3.0HS] Use two ZTE73 open WiFi then test ftp ZTE73 A transfer 9.6MB file to ZTE73 B (2min11s) slower than only BT FTP (1min 41s) transfer speed.
 * .
 *
 * 10 27 2010 sh.lai
 * [ALPS00022255] [Need Patch] [Volunteer Patch] Porting PAL interface for BT task
 * New feature ALPS00022255 : [Need Patch] [Volunteer Patch] Porting PAL interface for BT task.
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
#ifndef __PAL_HCI_STRUCT_H__
#define __PAL_HCI_STRUCT_H__

#ifndef _PAL_MAUI_
//#include "mtkpal_porting.h"

/*******************************************************************************
*                              C O N S T A N T S
********************************************************************************
*/

#define PAL_STATUS_SUCCESS  0x00000000
#define PAL_STATUS_FAILED   0x00000001

/* src mac addr + dest mac addr + proto type */
#define PAL_ETHERNET_MAC_SPACE 14
#define PAL_ETHERNET_HDR_LEN    8

#define PAL_MAC_ADDR_LEN    6
#define PAL_OUI_LEN         3

#define PAL_SSID_MAX_LEN    32

#define PAL_KEY_MAX_LEN     32

#define PAL_ROLE_ORIGINATOR 0
#define PAL_ROLE_RESPONDER  1

/* hci commnads */
#define PAL_AMP_ASSOC_FRAG_SIZE (248)

//#define PAL_AMP_KEY_SIZE        (32)
#define PAL_AMP_KEY_SIZE        (248)

#define PAL_FLOW_SPEC_SIZE      (16)

#define PAL_MAX_PDU_SIZE        (1500-8)
#define PAL_MAX_SDU_SIZE        (1500-8)

#define PAL_MAX_ASSOC_LEN       (672)

#define PAL_LOGICAL_LINK_NUM    (10)

/* HCI Command OP Code Definitions */
#define HCI_Read_Local_Version_Information_Op       0x0001
#define HCI_Read_Local_AMP_Info_Op                  0x0009
#define HCI_Reset_Op                                0x0003
#define HCI_Read_Local_AMP_ASSOC_Op                 0x000A
#define HCI_Create_Physical_Link_Op                 0x0035
#define HCI_Write_Remote_AMP_ASSOC_Op               0x000B
#define HCI_Accept_Physical_Link_Op                 0x0036
#define HCI_Disconnect_Physical_Link_Op             0x0037
#define HCI_Read_Link_Quality_Op                    0x0003
#define HCI_Read_RSSI_Op                            0x0005
#define HCI_Create_Logical_Link_Op                  0x0038
#define HCI_Accept_Logical_Link_Op                  0x0039
#define HCI_Flow_Spec_Modify_Op                     0x003C
#define HCI_Disconnect_Logical_Link_Op              0x003A
#define HCI_Logical_Link_Cancel_Op                  0x003B
#define HCI_Read_Logical_Link_Accept_Timeout_Op     0x0061
#define HCI_Write_Logical_Link_Accept_Timeout_Op    0x0062
#define HCI_Read_Link_Supervision_Timeout_Op        0x0036
#define HCI_Write_Link_Supervision_Timeout_Op       0x0037
#define HCI_Set_Event_Mask_Op                       0x0001
#define HCI_Set_Event_Mask_Page_2_Op                0x0063
#define HCI_Enhanced_Flush_Command_Op               0x005F
#define HCI_Read_Data_Block_Size_Op                 0x000A

/* HCI Event OP Code Definitions */
#define HCI_Command_Complete_Event_Op               0x0E
#define HCI_Command_Status_Event_Op                 0x0F
#define HCI_Enhanced_Flush_Complete_Event_Op        0x39
#define HCI_Physical_Link_Complete_Event_Op         0x40
#define HCI_Channel_Selected_Event_Op               0x41
#define HCI_Disconnect_Physical_Link_Complete_Op    0x42
#define HCI_Physical_Link_Loss_Early_Warning_Op     0x43
#define HCI_Logical_Link_Complete_Event_Op          0x45
#define HCI_Disconnection_Logical_Link_Complete_Op  0x46
#define HCI_Flow_Spec_Modify_Complete_Event_Op      0x47
#define HCI_Number_of_Complete_Data_Blocks_Event_Op 0x48


/*******************************************************************************
*                             D A T A   T Y P E S
********************************************************************************
*/

/*
   NOTICE :
   If you want to use MSG_ID_PAL_RAW_DATA,
   the data structure must be pal_raw_data_struct !!!
*/

/***************************************************************************
*  PRIMITIVE STRUCTURE
*     ref_hdr_struct
*
*  DESCRIPTION
*     Reference header for message.
***************************************************************************/
typedef struct
{   
    unsigned char ref_count;
    unsigned short msg_len;
} ref_hdr_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_local_version_info_command_struct
*
*  DESCRIPTION
*     Read PAL version information command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
} pal_bt_read_local_version_info_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_local_amp_info_command_struct
*
*  DESCRIPTION
*     Read Local AMP Info command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
} pal_bt_read_local_amp_info_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_reset_command_struct
*
*  DESCRIPTION
*     PAL Reset command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
} pal_bt_reset_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_data_block_size_command_struct
*
*  DESCRIPTION
*     Read data block size command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
} pal_bt_read_data_block_size_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_link_quality_command_struct
*
*  DESCRIPTION
*     Read Link Quality command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
   kal_uint16	u2Handle;
} pal_bt_read_link_quality_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_rssi_command_struct
*
*  DESCRIPTION
*     Read RSSI command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
   kal_uint16	u2Handle;
} pal_bt_read_rssi_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_local_amp_assoc_command_struct
*
*  DESCRIPTION
*     Read Local AMP ASSOC command.
***************************************************************************/
typedef struct
{
	ref_hdr_struct   ref_hdr;
   	kal_uint8	ucPhysical_link_handle;
   	kal_uint16	u2Length_so_far;
   	kal_uint16	u2Amp_assoc_length;
} pal_bt_read_local_amp_assoc_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_write_remote_amp_assoc_command_struct
*
*  DESCRIPTION
*     Write Remote AMP ASSOC command.
***************************************************************************/
typedef struct
{
	ref_hdr_struct  ref_hdr;   	   	
   	kal_uint8	ucPhysical_link_handle;
    kal_uint16	u2Length_so_far; // size of this assoc fragment
   	kal_uint16	u2Amp_assoc_remaining_length;
    kal_uint8   ucAmp_assoc_fragment_size;
   	kal_uint8	aucAmp_assoc_fragment[ PAL_AMP_ASSOC_FRAG_SIZE ];
} pal_bt_write_remote_amp_assoc_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_create_physical_link_command_struct
*
*  DESCRIPTION
*     Create Physical Link command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;	
    kal_uint8	ucPhysical_link_handle;
    kal_uint8	ucDedicate_amp_key_length;
    kal_uint8	ucDedicate_amp_key_type;
    kal_uint8	aucDedicate_amp_key[ PAL_AMP_KEY_SIZE ];
} pal_bt_create_physical_link_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_accept_physical_link_command_struct
*
*  DESCRIPTION
*     Accept Physical Link command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;	
    kal_uint8	ucPhysical_link_handle;
    kal_uint8	ucDedicate_amp_key_length;
    kal_uint8	ucDedicate_amp_key_type;
    kal_uint8	aucDedicate_amp_key[ PAL_AMP_KEY_SIZE ];
} pal_bt_accept_physical_link_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_link_supervision_timeout_command_struct
*
*  DESCRIPTION
*     Read Link Supervision Timeout command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;	
   kal_uint16	u2Handle;
} pal_bt_read_link_supervision_timeout_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_disconnect_physical_link_command_struct
*
*  DESCRIPTION
*     Disconnect Physical Link command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;	
   kal_uint8	ucPhysical_link_handle;
   kal_uint8	ucReason;
} pal_bt_disconnect_physical_link_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_write_link_supervision_timeout_command_struct
*
*  DESCRIPTION
*     Write Link Supervision Timeout command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;
   kal_uint16	u2Handle;
   kal_uint16	u2Link_supervision_timeout;
} pal_bt_write_link_supervision_timeout_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_create_logical_link_command_struct
*
*  DESCRIPTION
*     Create Logical Link command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;	
   kal_uint8	ucPhysical_link_handle;
   kal_uint8	aucTx_flow_spec[ PAL_FLOW_SPEC_SIZE ];
   kal_uint8	aucRx_flow_spec[ PAL_FLOW_SPEC_SIZE ];
} pal_bt_create_logical_link_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_accept_logical_link_command_struct
*
*  DESCRIPTION
*     Accept Logical Link command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;	
   kal_uint8	ucPhysical_link_handle;
   kal_uint8	aucTx_flow_spec[ PAL_FLOW_SPEC_SIZE ];
   kal_uint8	aucRx_flow_spec[ PAL_FLOW_SPEC_SIZE ];
} pal_bt_accept_logical_link_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_flow_spec_modify_command_struct
*
*  DESCRIPTION
*     Flow Spec Modify command.
***************************************************************************/
typedef struct
{
   ref_hdr_struct   ref_hdr;	
   kal_uint16	u2Logical_link_handle;
   kal_uint8	aucTx_flow_spec[ PAL_FLOW_SPEC_SIZE ];
   kal_uint8	aucRx_flow_spec[ PAL_FLOW_SPEC_SIZE ];
} pal_bt_flow_spec_modify_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_disconnect_logical_link_command_struct
*
*  DESCRIPTION
*     Disconnect Logical Link command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;	
   kal_uint16	u2Logical_link_handle;
} pal_bt_disconnect_logical_link_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_logical_link_cancel_command_struct
*
*  DESCRIPTION
*      Logical Link Cancel command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;	
   kal_uint8	ucPhysical_link_handle;
   kal_uint8	ucTx_flow_spec_id;
} pal_bt_logical_link_cancel_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_logical_link_accept_timeout_command_struct
*
*  DESCRIPTION
*      Read Logical Link Accept Timeout command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
} pal_bt_read_logical_link_accept_timeout_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_write_logical_link_accept_timeout_command_struct
*
*  DESCRIPTION
*      Write Logical Link Accept Timeout command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
   kal_uint16	u2Logical_link_accept_timeout;
} pal_bt_write_logical_link_accept_timeout_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_data_command_struct
*
*  DESCRIPTION
*      Data Tx command.
***************************************************************************/
#if 0
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint16	u2Handle;				// 12 bits are meaningful  (Logical link)
    kal_uint8	ucPb_flag;				// 2 bits are meaningful
    kal_uint8	ucBc_flag;				// 2 bits are meaningful
    kal_uint16	u2Data_total_len;		// 2 bits are meaningful
    void*           ucData_p;                       // used for MAUI
    kal_uint8  		aucData_p[PAL_MAX_SDU_SIZE];    // used for Android
} pal_bt_data_command_struct;
#endif
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint16	u2Handle;				// 12 bits are meaningful  (Logical link)
    kal_uint8	ucPb_flag;				// 2 bits are meaningful
    kal_uint8	ucBc_flag;				// 2 bits are meaningful
    kal_uint8 data_count;                               // 2 bits are meaningful
    kal_uint16	u2Data_total_len;		// 2 bits are meaningful
    void*           ucData_p;                       // used for MAUI
    kal_uint8  		aucData_p[PAL_MAX_SDU_SIZE];    // used for Android
    kal_uint16        u2Data_total_len1;                   // 2 bits are meaningful
    kal_uint8                 aucData_p1[PAL_MAX_SDU_SIZE];    // used for Android
    kal_uint16        u2Data_total_len2;                   // 2 bits are meaningful
    kal_uint8                 aucData_p2[PAL_MAX_SDU_SIZE];    // used for Android
    kal_uint16        u2Data_total_len3;                   // 2 bits are meaningful
    kal_uint8                 aucData_p3[PAL_MAX_SDU_SIZE];    // used for Android

} pal_bt_data_command_struct;


/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_set_event_mask_page2_command_struct
*
*  DESCRIPTION
*      Set event mask page 2 command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8       aucEvent_Mask_Page_2[8];
} pal_bt_set_event_mask_page2_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_set_event_mask_page2_command_struct
*
*  DESCRIPTION
*      Set event make page 2 command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
    kal_uint16       u2Handle;
    kal_uint8        ucPacket_type;
} pal_bt_enhanced_flush_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_short_range_mode_command_struct
*
*  DESCRIPTION
*      Short range mode command.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
    kal_uint8        ucPhysical_link_handle;
    kal_uint8        ucShort_range_mode;
} pal_bt_short_range_mode_command_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_local_version_complete_event_struct
*
*  DESCRIPTION
*      Read local AMP version complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
	kal_uint8	ucHci_version;
	kal_uint16	u2Hci_revision;
	kal_uint8	ucPal_version;
	kal_uint16	u2Manufacturer_name;
	kal_uint16	u2Pal_subversion;
} pal_bt_read_local_version_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_local_amp_info_complete_event_struct
*
*  DESCRIPTION
*      Read local AMP Info complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint8	ucAmp_status;
    kal_uint32	u4Total_bandwidth;
    kal_uint32	u4Max_guaranteed_bandwidth;
    kal_uint32	u4Min_latency;
    kal_uint32	u4Max_pdu_size;
    kal_uint8	ucController_type;
    kal_uint16	u2Pal_capabilities;
    kal_uint16	u2Max_amp_assoc_length;
    kal_uint32	u4Max_flush_timeout;
    kal_uint32	u4Best_effort_flush_timeout;
} pal_bt_read_local_amp_info_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_local_amp_assoc_complete_event_struct
*
*  DESCRIPTION
*      Read local AMP ASSOC complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint8	ucPhysical_link_handle;
    kal_uint16	u2Amp_assoc_remaining_length;
    kal_uint8   aucAmp_assoc_fragment_size;
    kal_uint8	aucAmp_assoc_fragment[ PAL_AMP_ASSOC_FRAG_SIZE ];
} pal_bt_read_local_amp_assoc_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_reset_complete_event_struct
*
*  DESCRIPTION
*      HCI Reset complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
} pal_bt_reset_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_data_block_size_complete_event_struct
*
*  DESCRIPTION
*     Read data block size complete event.
***************************************************************************/
typedef struct
{
   ref_hdr_struct    ref_hdr;
   kal_uint8	ucStatus;
   kal_uint16	u2Max_acl_data_packet_length;
   kal_uint16	u2Data_block_length;
   kal_uint16	u2Total_num_data_blocks;
} pal_bt_read_data_block_size_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_channel_selected_event_struct
*
*  DESCRIPTION
*      Channel Selected event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucPhysical_link_handle;
} pal_bt_channel_selected_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_link_quality_complete_event_struct
*
*  DESCRIPTION
*      Read link quality complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint16	u2Handle;
    kal_uint8	ucLink_quality;
} pal_bt_read_link_quality_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_rssi_complete_event_struct
*
*  DESCRIPTION
*      Read RSSI complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint16	u2Handle;
    kal_uint8	ucRssi;
} pal_bt_read_rssi_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_write_remote_amp_assoc_complete_event_struct
*
*  DESCRIPTION
*      Write remote AMP ASSOC complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint8	ucPhysical_link_handle;
} pal_bt_write_remote_amp_assoc_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_create_physical_link_status_event_struct
*
*  DESCRIPTION
*      Create physical link status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
} pal_bt_create_physical_link_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_accept_physical_link_status_event_struct
*
*  DESCRIPTION
*      Create physical link status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
} pal_bt_accept_physical_link_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_physical_link_complete_event_struct
*
*  DESCRIPTION
*      Physical link complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint8	ucPhysical_link_handle;
} pal_bt_physical_link_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_link_supervision_timeout_complete_event_struct
*
*  DESCRIPTION
*      Read link supervision timeout complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
	kal_uint8	ucStatus;
    kal_uint16	u2Handle;
    kal_uint16	u2Link_supervision_timeout;
} pal_bt_read_link_supervision_timeout_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_disconnect_physical_link_status_event_struct
*
*  DESCRIPTION
*      Disconnect physical link status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
} pal_bt_disconnect_physical_link_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_disconnect_physical_link_complete_event_struct
*
*  DESCRIPTION
*      Disconnect physical link complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
	kal_uint8	ucStatus;
    kal_uint8	ucPhysical_link_handle;
    kal_uint8	ucReason;
} pal_bt_disconnect_physical_link_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_physical_link_loss_early_warning_event_struct
*
*  DESCRIPTION
*      Physical link loss early warning event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucPhysical_link_handle;
    kal_uint8	ucLink_loss_reason;
} pal_bt_physical_link_loss_early_warning_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_write_link_supervision_timeout_complete_event_struct
*
*  DESCRIPTION
*      Write link supervision timeout complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
    kal_uint16	u2Handle;
} pal_bt_write_link_supervision_timeout_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_flow_spec_modify_status_event_struct
*
*  DESCRIPTION
*      Flow spec modify status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
} pal_bt_flow_spec_modify_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_flow_spec_modify_complete_event_struct
*
*  DESCRIPTION
*      Flow spec modify complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
    kal_uint16	u2handle;
} pal_bt_flow_spec_modify_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_create_logical_link_status_event_struct
*
*  DESCRIPTION
*      Create logical link status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
    kal_uint8	ucStatus;
} pal_bt_create_logical_link_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_accept_logical_link_status_event_struct
*
*  DESCRIPTION
*      Accept logical link status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
    kal_uint8	ucStatus;
} pal_bt_accept_logical_link_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_logical_link_complete_event_struct
*
*  DESCRIPTION
*      Logical link complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
    kal_uint16	u2Logical_link_handle;
    kal_uint8	ucPhysical_link_handle;
    kal_uint8	ucTx_flow_spec_id;
} pal_bt_logical_link_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_disconnect_logical_link_status_event_struct
*
*  DESCRIPTION
*      Disconnect logical link status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
    kal_uint8	ucStatus;
} pal_bt_disconnect_logical_link_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_disconnect_logical_link_complete_event_struct
*
*  DESCRIPTION
*      Disconnect logical link complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
    kal_uint16	u2Logical_link_handle;
    kal_uint8	ucReason;
} pal_bt_disconnect_logical_link_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_logical_link_cancel_status_event_struct
*
*  DESCRIPTION
*      Logical link cancel status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct   ref_hdr;
    kal_uint8	ucStatus;
} pal_bt_logical_link_cancel_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_logical_link_cancel_complete_event_struct
*
*  DESCRIPTION
*      Logical link cancel complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
    kal_uint8	ucPhysical_link_handle;
    kal_uint8	ucTx_flow_spec_id;
} pal_bt_logical_link_cancel_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_read_logical_link_accept_timeout_complete_event_struct
*
*  DESCRIPTION
*      Read logical link accept timeout complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
    kal_uint16	ucLogical_link_accept_timeout;
} pal_bt_read_logical_link_accept_timeout_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_write_logical_link_accept_timeout_complete_event_struct
*
*  DESCRIPTION
*      Write logical link accept timeout complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8	ucStatus;
} pal_bt_write_logical_link_accept_timeout_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_data_event_struct
*
*  DESCRIPTION
*      Data Rx event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint16	u2Handle;				// 8 bits are meaningful  (Physical link)
    kal_uint8	ucPb_flag;				// 2 bits are meaningful
    kal_uint8	ucBc_flag;				// 2 bits are meaningful
    kal_uint16	u2Data_total_len;		// 2 bits are meaningful    
    void*           ucData_p;                       // used for MAUI
    kal_uint8  		aucData_p[PAL_MAX_SDU_SIZE];    // used for Android
} pal_bt_data_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_amp_status_change_event_struct
*
*  DESCRIPTION
*      Data Rx event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8   ucStatus;
    kal_uint8   ucAmp_status;
} pal_bt_amp_status_change_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_num_of_complete_data_blocks_event_struct
*
*  DESCRIPTION
*      Number of Completed Data Blocks event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint16  u2Total_num_data_blocks;
    kal_uint8   ucNum_of_handles;
    kal_uint16  au2Handle[PAL_LOGICAL_LINK_NUM];
    kal_uint16  au2Num_of_completed_packet[PAL_LOGICAL_LINK_NUM];
    kal_uint16  au2Num_of_completed_blocks[PAL_LOGICAL_LINK_NUM];
} pal_bt_num_of_complete_data_blocks_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_num_of_complete_data_blocks_event_struct
*
*  DESCRIPTION
*      Number of Completed Data Blocks event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8   ucPhysical_link_handle;
    kal_uint8   ucLink_loss_reason;
} pal_bt_phy_link_loss_early_warning_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_set_event_mask_page2_complete_event_struct
*
*  DESCRIPTION
*      Set HCI Event mask page 2 complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8       ucStatus;
} pal_bt_set_event_mask_page2_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_enhanced_flush_status_event_struct
*
*  DESCRIPTION
*      Set HCI Enhanced Flush status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8       ucStatus;
} pal_bt_enhanced_flush_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_enhanced_flush_complete_event_struct
*
*  DESCRIPTION
*      Set HCI Enhanced Flush complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint16      u2Handle;
} pal_bt_enhanced_flush_complete_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_short_range_mode_status_event_struct
*
*  DESCRIPTION
*      Short range mode status event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8       ucStatus;
} pal_bt_short_range_mode_status_event_struct;

/***************************************************************************
*  PRIMITIVE STRUCTURE
*			pal_bt_short_range_mode_change_complete_event_struct
*
*  DESCRIPTION
*      Short range mode change complete event.
***************************************************************************/
typedef struct
{
    ref_hdr_struct  ref_hdr;
    kal_uint8       ucStatus;
    kal_uint8       ucPhysical_link_handle;
    kal_uint8       ucShort_range_mode;
} pal_bt_short_range_mode_change_complete_event_struct;

#endif

#endif /* __PAL_STRUCT_H__ */

