/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bluetooth_htp_message.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to define message for communication between BT task and external task.
 *
 * Author:
 * -------
 * Ting Zheng
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime:
 * $Log: 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
#ifndef __BT_HTP_MESSAGE_H__
#define __BT_HTP_MESSAGE_H__

    /*APP --> HTP Collector*/
    MSG_ID_BT_HTPC_GROUP_START = MSG_ID_BT_HTP_GROUP_START,
    MSG_ID_BT_HTPC_ACTIVATE_REQ = MSG_ID_BT_HTPC_GROUP_START,
    MSG_ID_BT_HTPC_DEACTIVATE_REQ,
    MSG_ID_BT_HTPC_CONNECT_REQ,
    MSG_ID_BT_HTPC_DISCONNECT_REQ,
    MSG_ID_BT_HTPC_SEARCH_ATTRIBUTE_REQ,
    MSG_ID_BT_HTPC_GET_TEMPERATURE_TYPE_REQ,    
    MSG_ID_BT_HTPC_GET_CONFIG_REQ,                          
    MSG_ID_BT_HTPC_GET_INTERVAL_VALUE_REQ,     
    MSG_ID_BT_HTPC_GET_INTERVAL_RANGE_REQ,
    MSG_ID_BT_HTPC_SET_CONFIG_REQ,
    MSG_ID_BT_HTPC_SET_INTERVAL_VALUE_REQ,
    MSG_ID_BT_HTPC_REMOVE_DEV_REQ,
    
    /*APP <-- HTP Collector*/
    MSG_ID_BT_HTPC_ACTIVATE_CNF,
    MSG_ID_BT_HTPC_DEACTIVATE_CNF,
    MSG_ID_BT_HTPC_CONNECT_CNF,
    MSG_ID_BT_HTPC_DISCONNECT_CNF,
    MSG_ID_BT_HTPC_SEARCH_ATTRIBUTE_CNF,
    MSG_ID_BT_HTPC_SET_CONFIG_CNF,
    MSG_ID_BT_HTPC_SET_INTERVAL_VALUE_CNF,
    MSG_ID_BT_HTPC_DISCONNECT_IND,
    MSG_ID_BT_HTPC_TEMPERATURE_IND,
    MSG_ID_BT_HTPC_INTERMEDIATE_TEMPERATURE_IND,
    MSG_ID_BT_HTPC_SET_INTERVAL_VALUE_IND,
    MSG_ID_BT_HTPC_GROUP_END = MSG_ID_BT_HTPC_SET_INTERVAL_VALUE_IND,


    /*APP --> HTP Thermometer*/
    MSG_ID_BT_HTPT_GROUP_START,
    MSG_ID_BT_HTPT_ACTIVATE_REQ = MSG_ID_BT_HTPT_GROUP_START,
    MSG_ID_BT_HTPT_DEACTIVATE_REQ,
    MSG_ID_BT_HTPT_DISCONNECT_REQ,
    MSG_ID_BT_HTPT_SEND_TEMPERATURE,
    MSG_ID_BT_HTPT_SEND_INTERMEDIATE_TEMPERATURE,
    MSG_ID_BT_HTPT_SET_INTERVAL_VALUE_REQ,
     MSG_ID_BT_HTPT_SEND_INTERVAL_VALUE_INDICATION,
    
    /*APP <-- HTP Thermometer*/
    MSG_ID_BT_HTPT_ACTIVATE_CNF,
    MSG_ID_BT_HTPT_DEACTIVATE_CNF,
    MSG_ID_BT_HTPT_DISCONNECT_CNF,
    MSG_ID_BT_HTPT_SEND_TEMPERATURE_CNF,
    MSG_ID_BT_HTPT_SEND_INTERMEDIATE_TEMPERATURE_CNF,
    MSG_ID_BT_HTPT_SET_INTERVAL_VALUE_CNF,
    MSG_ID_BT_HTPT_SEND_INTERVAL_VALUE_INDICATION_CNF,
    MSG_ID_BT_HTPT_CONNECT_IND,
    MSG_ID_BT_HTPT_DISCONNECT_IND,
    MSG_ID_BT_HTPT_SET_CONFIG_IND,  // receive write request from collector
    MSG_ID_BT_HTPT_SET_INTERVAL_VALUE_IND,  // receive write request from collector
    MSG_ID_BT_HTPT_GROUP_END = MSG_ID_BT_HTPT_SET_INTERVAL_VALUE_IND,

    MSG_ID_BT_HTP_CMD,	// used if cmd agent is necessary

    MSG_ID_BT_HTP_GROUP_END = MSG_ID_BT_HTP_CMD,
#endif
