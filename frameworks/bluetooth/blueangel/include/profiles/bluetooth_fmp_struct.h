/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bluetooth_fmp_struct.h
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
#ifndef __BT_FMP_STRUCT__
#define __BT_FMP_STRUCT__

#include "bttypes.h"

#define FMP_DEVICE_ADDR_EQUAL(dev1, dev2)     ((memcmp(\
							(U8 *)dev1, (U8 *)dev2, \
			                                    sizeof(BD_ADDR)) == 0) ? TRUE : FALSE)


#define FMP_MAX_DEV_NUM     NUM_BT_DEVICES

typedef U8 bt_fmp_service_flag;

#define BT_FMP_SERVICE_LOCATOR_FLAG     0x01
#define BT_FMP_SERVICE_TARGET_FLAG     0x02

typedef enum
{
    BT_FMP_ALERT_LEVEL_NONE,
    BT_FMP_ALERT_LEVEL_MILD,
    BT_FMP_ALERT_LEVEL_HIGH,
} bt_fmp_alert_level_enum;

typedef struct
{
    LOCAL_PARA_HDR
    /* FMP locator and target service can be activated seperately or at once */    
    bt_fmp_service_flag service_flag;    
} bt_fmp_activate_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    bt_fmp_service_flag service_flag;    
    BT_BOOL result;
} bt_fmp_activate_cnf_struct;

typedef bt_fmp_activate_cnf_struct bt_fmp_deactivate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    /* FMP locator and target service can be deactivated seperately or at once */    
    bt_fmp_service_flag service_flag;    
} bt_fmp_deactivate_req_struct;


typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_fmp_connect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_fmp_search_attribute_req_struct;

typedef bt_fmp_connect_req_struct bt_fmp_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_fmp_alert_level_enum level;
} bt_fmp_set_alert_level_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_fmp_remove_dev_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    BT_BOOL result;
} bt_fmp_connect_cnf_struct;

typedef bt_fmp_connect_cnf_struct bt_fmp_disconnect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    BT_BOOL result;
} bt_fmp_search_attribute_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_fmp_alert_level_enum level;
    BT_BOOL result;
} bt_fmp_set_alert_level_cnf_struct;


typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_fmp_connect_ind_struct;

typedef bt_fmp_connect_ind_struct bt_fmp_disconnect_ind_struct;


typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_fmp_alert_level_enum level;
} bt_fmp_set_alert_level_ind_struct;

#endif
