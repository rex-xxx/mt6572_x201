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
#ifndef __BT_FMP_MESSAGE_H__
#define __BT_FMP_MESSAGE_H__

    /*APP --> FMP*/
    MSG_ID_BT_FMP_ACTIVATE_REQ = MSG_ID_BT_FMP_GROUP_START,
    MSG_ID_BT_FMP_DEACTIVATE_REQ,
    MSG_ID_BT_FMP_CONNECT_REQ,
    MSG_ID_BT_FMP_DISCONNECT_REQ,
    MSG_ID_BT_FMP_SET_ALERT_LEVEL_REQ,
    MSG_ID_BT_FMP_SEARCH_ATTRIBUTE_REQ,
    MSG_ID_BT_FMP_REMOVE_DEV_REQ,
    
    /*APP <-- FMP*/
    MSG_ID_BT_FMP_ACTIVATE_CNF,
    MSG_ID_BT_FMP_DEACTIVATE_CNF,
    MSG_ID_BT_FMP_CONNECT_CNF,
    MSG_ID_BT_FMP_DISCONNECT_CNF,
    MSG_ID_BT_FMP_SET_ALERT_LEVEL_CNF,
    MSG_ID_BT_FMP_SEARCH_ATTRIBUTE_CNF,
    MSG_ID_BT_FMP_CONNECT_IND,
    MSG_ID_BT_FMP_DISCONNECT_IND,
    MSG_ID_BT_FMP_SET_ALERT_LEVEL_IND,
    MSG_ID_BT_FMP_GROUP_END = MSG_ID_BT_FMP_SET_ALERT_LEVEL_IND,

#endif
