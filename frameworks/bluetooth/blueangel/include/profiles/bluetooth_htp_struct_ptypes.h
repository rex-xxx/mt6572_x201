
/* Currently the enum-types are replaced by U8.
 * And if the entry number grows in the future, we might have to modify this.
 */

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_connect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_get_temperature_type_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    U32 char_type;
} bt_htpc_get_config_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_get_interval_value_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_get_interval_range_req_struct;


typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
	U32 char_type;
	U16 flag;
} bt_htpc_set_config_req_struct;


typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    U16 interval;
} bt_htpc_set_interval_value_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_remove_dev_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    S8 result;
} bt_htpc_activate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    S8 result;
} bt_htpc_deactivate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
} bt_htpc_connect_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;
	U8 remote_features;
    
    U32 temperature_type;
    U16 temperature_config;
    U16 intermediate_temperature_config;
    U16 interval_config;
    U16 interval;
    U16 interval_range_min;
    U16 interval_range_max;
} bt_htpc_search_attribute_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
} bt_htpc_disconnect_cnf_struct;


typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
    U32 char_type;
} bt_htpc_set_config_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
} bt_htpc_set_interval_value_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpc_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];

    U32 unit_type;

    float value;

	U8 flags;

    U16 year;
    U8 month;
    U8 day;
    U8 hour;
    U8 minute;
    U8 second;

	U32 temperature_type;
} bt_htpc_temperature_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];

    U32 unit_type;

    float value;

	U8 flags;

    U16 year;
    U8 month;
    U8 day;
    U8 hour;
    U8 minute;
    U8 second;

	U32 temperature_type;
} bt_htpc_intermediate_temperature_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    U16 interval;
} bt_htpc_set_interval_value_ind_struct;


/** 
 *Data structure definition for Thermometer 
 */

typedef struct 
{
    LOCAL_PARA_HDR
    U16 interval;
    U16 interval_range_max;
    U16 interval_range_min;
    U32 temperature_type;
} bt_htpt_activate_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpt_disconnect_req_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];

    U32 unit_type;

    float value;

	U8 flags;

    U16 year;
    U8 month;
    U8 day;
    U8 hour;
    U8 minute;
    U8 second;

	U32 temperature_type;
} bt_htpt_send_temperature_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];

    U32 unit_type;

    float value;

	U8 flags;

    U16 year;
    U8 month;
    U8 day;
    U8 hour;
    U8 minute;
    U8 second;

	U32 temperature_type;
} bt_htpt_send_intermediate_temperature_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U16 interval;
} bt_htpt_set_interval_value_req_struct;

typedef struct 
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];    
} bt_htpt_send_interval_value_indication_struct;


typedef struct
{
    LOCAL_PARA_HDR
    S8 result;
} bt_htpt_activate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    S8 result;
} bt_htpt_deactivate_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
} bt_htpt_disconnect_cnf_struct;


typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
} bt_htpt_send_temperature_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    S8 result;	
} bt_htpt_send_intermediate_temperature_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    S8 result;
} bt_htpt_set_interval_value_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];    
    S8 result;
} bt_htpt_send_interval_value_indication_cnf_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    U16 temperature_config;
    U16 intermediate_temperature_config;
    U16 interval_config;
} bt_htpt_connect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
} bt_htpt_disconnect_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
	U32 char_type;
	U16 flag;
} bt_htpt_set_config_ind_struct;

typedef struct
{
    LOCAL_PARA_HDR
    U8 bt_addr[6];
    U16 interval;
} bt_htpt_set_interval_value_ind_struct;


