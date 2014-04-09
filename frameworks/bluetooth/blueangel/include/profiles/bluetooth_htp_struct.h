/*******************************************************************************
 *
 * Filename:
 * ---------
 * Bluetooth_htp_struct.h
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
#ifndef __BT_HTP_STRUCT__
#define __BT_HTP_STRUCT__

#include "bttypes.h"


#define HTP_MAX_DEV_NUM	NUM_BT_DEVICES


#define HTP_MEM_MALLOC(size)		malloc(size)
#define HTP_MEM_FREEIF(p)			do{if(p){free(p); p = NULL;}}while(0)		
#define HTP_MEM_FREE(p)			free(p)	

#define HTP_DEVICE_ADDR_EQUAL(dev1, dev2)     ((memcmp(\
							(U8 *)dev1, (U8 *)dev2, \
			                                    sizeof(BD_ADDR)) == 0) ? TRUE : FALSE)


/*
* It is for Collector only.
* Features supported on Remote Thermometer which is included in
* query cnf message.
*/
typedef U8 bt_htp_remote_features;

/* Remote thermometer supports static temperature type
*/
#define BT_HTP_FEATURE_TEMPERATURE_TYPE_READABLE				0x01

/* Client characteristic configuration for intermediate characteristic
*   can be configured to notication or not
*/
#define BT_HTP_FEATURE_INTERMEDIATE_MEASUREMENT_NOTIFIABLE		0x02    

/* Remote thermometer supports measurement interval characteristic
*/
#define BT_HTP_FEATURE_INTERVAL_READABLE	        0x04

/* Measurement interval can be configured by Collector, and 
*   Valid Range Descriptor is supported.
*/
#define BT_HTP_FEATURE_INTERVAL_WRITABLE	        0x08

/* Client characteristic configuration for Measurement Interval characteristic
*   is supported, and it can be configured to indicaiton or not.
*/
#define BT_HTP_FEATURE_INTERVAL_INDICATABLE     0x10


/*
* GATT Client Characteristic Configuration Value Definition
*/
typedef U16 bt_htp_desc_config;

#define BT_HTP_DESCRIPTOR_CONFIG_NONE		0x0000
#define BT_HTP_DESCRIPTOR_CONFIG_NOTIFICATION		0x0001
#define BT_HTP_DESCRIPTOR_CONFIG_INDICATION		0x0002


/*
* Common Data Structure Definition
*/

typedef enum
{
    BT_HTP_TEMPERATURE_MEASUREMENT,
    BT_HTP_INTERMEDIATE_TEMPERATURE,
    BT_HTP_MEASUREMENT_INTERVAL,
} bt_htp_char_enum;

typedef struct
{
    bt_htp_char_enum char_type;
    bt_htp_desc_config flag;
} bt_htp_desc_config_struct;

typedef enum
{
    BT_HTP_MEASUREMENT_FAHRENHEIT,
    BT_HTP_MEASUREMENT_CELSIUS
} bt_htp_measurement_uinit_enum;

/* enum value shall be compliant with spec */
typedef enum
{
    BT_HTP_TEMPERATURE_TYPE_UNKNOWN = 0x00,
    BT_HTP_TEMPERATURE_TYPE_ARMPIT = 0x01,		//Armpit
    BT_HTP_TEMPERATURE_TYPE_BODY,		//General
    BT_HTP_TEMPERATURE_TYPE_EAR,		//Ear (usually ear lobe)
    BT_HTP_TEMPERATURE_TYPE_FINGER,		//Finger
    BT_HTP_TEMPERATURE_TYPE_GIT,		//Gastro-intestinal Tract
    BT_HTP_TEMPERATURE_TYPE_MOUTH,		//Mouth
    BT_HTP_TEMPERATURE_TYPE_RECT,		//Rectum
    BT_HTP_TEMPERATURE_TYPE_TOE,		//Toe
    BT_HTP_TEMPERATURE_TYPE_TYMP,		//Tympanum (ear drum)
    BT_HTP_TEMPERATURE_TYPE_MAX,
} bt_htp_temperature_type_enum;


typedef struct
{
    U16  year;
    U8  month;
    U8	  day;
    U8	  hour;
    U8  minute;
    U8	  second;
} bt_htp_timestamp_struct;

typedef U8 bt_htp_temperature_flags;

#define BT_HTP_TP_TIMESTAMP_FLAG    0x01
#define BT_HTP_TP_TEMPERATURE_TYPE_FLAG                 0x02

typedef struct
{
    bt_htp_measurement_uinit_enum unit_type;
    float value;

    bt_htp_temperature_flags flags;
    /**
    *   If there is no time stamp, set to all zero which shall be treated
    *   as invalid parameter and ignored 
    */
    bt_htp_timestamp_struct timestamp;
    /* *
    *   If there is no temperature type parameter, set to 
    *   BT_HTP_TEMPERATURE_TYPE_INVALID which shall be treated
    *   as invalid parameter and ignored 
    */
    bt_htp_temperature_type_enum temperature_type;	
} bt_htp_temperature_struct;

typedef struct
{
    /* Provide configurable service attribute */
    U16 interval;
    U16 interval_range_max;
    U16 interval_range_min;
    /**
    *   It may be configured if static temperature type is used. 
    *   If it is set as invalid value, 0x02(Body) shall be used as default.
    */	
    bt_htp_temperature_type_enum temperature_type;	
} bt_htp_server_parm_struct;

/** 
 *Data structure definition for Collector 
 */

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_htpc_connect_req_struct;

typedef bt_htpc_connect_req_struct bt_htpc_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_htpc_get_temperature_type_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_htp_char_enum char_type;
} bt_htpc_get_config_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_htpc_get_interval_value_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_htpc_get_interval_range_req_struct;


typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_htp_desc_config_struct config;
} bt_htpc_set_config_req_struct;


typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    U16 interval;
} bt_htpc_set_interval_value_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_htpc_remove_dev_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BT_BOOL result;
} bt_htpc_activate_cnf_struct;

typedef bt_htpc_activate_cnf_struct bt_htpc_deactivate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    BT_BOOL result;	
} bt_htpc_connect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    BT_BOOL result;
    bt_htp_remote_features remote_features;
    
    bt_htp_temperature_type_enum temperature_type;
    bt_htp_desc_config temperature_config;
    bt_htp_desc_config intermediate_temperature_config;
    bt_htp_desc_config interval_config;
    U16 interval;
    U16 interval_range_min;
    U16 interval_range_max;
} bt_htpc_search_attribute_cnf_struct;

typedef bt_htpc_connect_cnf_struct bt_htpc_disconnect_cnf_struct;


typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    BT_BOOL result;	
    bt_htp_char_enum char_type;
} bt_htpc_set_config_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    BT_BOOL result;	
} bt_htpc_set_interval_value_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
} bt_htpc_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_htp_temperature_struct temperature;		
} bt_htpc_temperature_ind_struct;

typedef bt_htpc_temperature_ind_struct bt_htpc_intermediate_temperature_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    U16 interval;
} bt_htpc_set_interval_value_ind_struct;


/** 
 *Data structure definition for Thermometer 
 */

typedef struct 
{
    LOCAL_PARA_HDR
    bt_htp_server_parm_struct parms;
} bt_htpt_activate_req_struct;

typedef bt_htpc_disconnect_req_struct bt_htpt_disconnect_req_struct;

typedef bt_htpc_temperature_ind_struct bt_htpt_send_temperature_struct;
typedef bt_htpc_temperature_ind_struct bt_htpt_send_intermediate_temperature_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U16 interval;
} bt_htpt_set_interval_value_req_struct;

typedef struct 
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;    
} bt_htpt_send_interval_value_indication_struct;


typedef bt_htpc_activate_cnf_struct bt_htpt_activate_cnf_struct;
typedef bt_htpc_deactivate_cnf_struct bt_htpt_deactivate_cnf_struct;

typedef bt_htpc_disconnect_cnf_struct bt_htpt_disconnect_cnf_struct;


typedef bt_htpc_set_interval_value_cnf_struct bt_htpt_send_temperature_cnf_struct;
typedef bt_htpc_set_interval_value_cnf_struct bt_htpt_send_intermediate_temperature_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BT_BOOL result;
} bt_htpt_set_interval_value_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;    
    BT_BOOL result;
} bt_htpt_send_interval_value_indication_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    BD_ADDR bt_addr;
    bt_htp_desc_config temperature_config;
    bt_htp_desc_config intermediate_temperature_config;
    bt_htp_desc_config interval_config;
} bt_htpt_connect_ind_struct;

typedef bt_htpc_disconnect_ind_struct bt_htpt_disconnect_ind_struct;

typedef bt_htpc_set_config_req_struct bt_htpt_set_config_ind_struct;

typedef bt_htpc_set_interval_value_req_struct bt_htpt_set_interval_value_ind_struct;


#endif
