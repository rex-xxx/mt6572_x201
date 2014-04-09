 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */


#ifndef MASTERCLEAR_BP_H_
#define MASTERCLEAR_BP_H_

#define CMD_ENGINE_FD_NOT_INIT -1
#define CMD_ENGINE_DEVICE        "/dev/ttyUSB3"
/* Number of times to try to connect to aux engine */
#define CMD_ENGINE_CONNECT_MAX_TRY   30
/* Delay between each aux engine connect try, in microseconds */
#define CMD_ENGINE_CONNECT_TRY_DELAY 1000000
#define CMD_CMN_DRV_BP_MASTERCLEAR_OPCODE   0x0012
#define CMD_BP_MASTER_CLEAR    0x01
#define CMD_BP_MASTER_RESET    0x00
#define CMD_BP_MASTER_RESET_DATALENTH    0x01

#define MDM_CTRL_DEVICE          "/sys/class/radio/mdm6600/command"
#define MDM_CMD_SHUTDONW         "shutdown"
#define MDM_CMD_POWERUP          "powerup"
#define MDM_CMD_FLASH_MODE       "bootmode_flash"
#define MDM_CMD_NORMAL_MODE      "bootmode_normal"

#define CMD_DEFS_RSP_FLAG_FAIL   0x04
#define TC_DBG_MAX_DUMP_COLS     16
/* CMD header values */
#define CMD_DEFS_HDR_FLAG_CMD_RSP_COMMAND       0 /**< Header indicates a command */
#define CMD_DEFS_HDR_FLAG_CMD_RSP_RESPONSE      1 /**< Header indicates a response */

#define CMD_DEFS_HDR_FLAG_RESPONSE_EXPECTED     0 /**< Header indicates a response is expected */
#define CMD_DEFS_HDR_FLAG_RESPONSE_NOT_EXPECTED 1 /**< Header indicates a response is not expected */

#define CMD_DEFS_HDR_FLAG_RESPONSE_SOLICITED    0 /**< Header indicates a solicited response */
#define CMD_DEFS_HDR_FLAG_RESPONSE_UNSOLICITED  1 /**< Header indicates a unsolicited response */

#define CMD_DEFS_HDR_FLAG_DATA_NOT_PRESENT      0 /**< Header indicates response data present */
#define CMD_DEFS_HDR_FLAG_DATA_PRESENT          1 /**< Header indicates no response data present */

#define CMD_DEFS_HDR_FLAG_CMD_NOT_FAILED        0 /**< Header indicates command did not fail */
#define CMD_DEFS_HDR_FLAG_CMD_FAILED            1 /**< Header indicates command failed */

#define CMD_DBG_MAX_DUMP_COLS 16

#define CMD_DEFS_OPCODE_T UINT16

#ifndef TRUE
    #define TRUE   1
    #define FALSE  0
#endif


static const char *USB1_SETUP_FILE = "/sys/bus/usb/devices/usb2/power/control";
static const char setup_cmd[] = "on";

typedef unsigned char        UINT8;      /**< Unsigned 8 bit integer */
typedef signed char          INT8;       /**< Signed 8 bit integer */
typedef unsigned short int   UINT16;     /**< Unsigned 16 bit integer */
typedef signed short int     INT16;      /**< Signed 16 bit integer */
typedef unsigned  int        UINT32;     /**< Unsigned 32 bit integer */
typedef signed int           INT32;      /**< Signed 32 bit integer */
typedef signed long long     INT64;      /**< Signed 64 bit integer */
typedef unsigned long long   UINT64;     /**< Unsigned 64 bit integer */
typedef unsigned char        BOOLEAN;    /**< Boolean type */
typedef BOOLEAN              BOOL;       /**< Boolean type */
typedef unsigned short       W_CHAR;     /**< Wide char */

typedef enum
{
    CMD_RSP_CODE_PAR_ERR_LENGTH       = 0x00,     /**< Parser length error        */
    CMD_RSP_CODE_PAR_ERR_SECUR        = 0x01,     /**< Parser security error      */
    CMD_RSP_CODE_PAR_ERR_PROT         = 0x02,     /**< Parser protocol error      */
    CMD_RSP_CODE_PAR_ERR_MODE         = 0x03,     /**< Parser mode error          */
    CMD_RSP_CODE_PAR_ERR_OPCODE       = 0x04,     /**< Parser opcode error        */
    CMD_RSP_CODE_PAR_ERR_PARM         = 0x05,     /**< Parser parameter error     */
    CMD_RSP_CODE_CMD_RSP_GENERIC      = 0x06,     /**< Generic Response           */
    CMD_RSP_CODE_CMD_RSP_GEN_FAIL     = 0x07,     /**< General failure            */
    CMD_RSP_CODE_CMD_MALLOC_FAIL      = 0x0A,     /**< Error allocating memory    */
    CMD_RSP_CODE_CMD_INTL_ERR         = 0x0B,     /**< Tcmd internal error        */
    CMD_RSP_CODE_CMD_RSP_TIMEOUT      = 0x0C,     /**< Timeout error              */
    CMD_RSP_CODE_CMD_PAR_ERR_SUBMODE  = 0x0D,     /**< Parser submode error       */
    CMD_RSP_CODE_CMD_TRANS_LEN_ERR    = 0x0E,     /**< Transport length error     */
    CMD_RSP_CODE_CMD_RSP_IRRE_ERR     = 0x0F,     /**< Irrecoverable error        */
    CMD_RSP_CODE_CMD_RSP_MUX_ERR      = 0x11,     /**< Open mux channel error     */

    CMD_RSP_CODE_ASCII_ERR_LENGTH     = 0x80,     /**< ASCII length error         */
    CMD_RSP_CODE_ASCII_ERR_MODE       = 0x83,     /**< ASCII mode error           */
    CMD_RSP_CODE_ASCII_ERR_OPCODE     = 0x84,     /**< ASCII opcode error         */
    CMD_RSP_CODE_ASCII_ERR_PARM       = 0x85,     /**< ASCII parameter error      */
    CMD_RSP_CODE_ASCII_RSP_GEN_FAIL   = 0x87,     /**< ASCII Generic Failure      */
    CMD_RSP_CODE_ASCII_MALLOC_FAIL    = 0x8A,     /**< ASCII allocating memory error */
    CMD_RSP_CODE_ASCII_RSP_TIMEOUT    = 0x8C,     /**< ASCII timeout error        */
    CMD_RSP_CODE_ASCII_RSP_MUX_ERR    = 0x91,     /**< ASCII mux channel error    */

    /** Add all new standard response codes before CMD_RSP_CODE_NOT_SET */
    CMD_RSP_CODE_NOT_SET
} CMD_RSP_CODE_T;

static int cmd_engine_fd = CMD_ENGINE_FD_NOT_INIT; /**< Handle to the aux engine */

typedef enum
{
    CMD_ENGINE_INIT_SUCCESS     = 0,
    CMD_ENGINE_INIT_FAIL        = 1,
    CMD_ENGINE_INIT_NOT_PRESENT = 2
} CMD_ENGINE_INIT_T;
/** The Command Protocol Header (Bulk Endpoint/12 byte) - Request Header */
typedef struct
{
    UINT8             reserved1         : 7;  /**< Reserved */
    UINT8             cmd_rsp_flag      : 1;  /**< Command/Response Flag */
    UINT8             seq_tag;                /**< Sequence Tag */
    CMD_DEFS_OPCODE_T  opcode;                 /**< Opcode */
    UINT8             reserved2;              /**< Reserved */
    UINT8             no_rsp_reqd_flag  : 1;  /**< No Response Required Flag */
    UINT8             reserved3         : 7;  /**< Reserved */
    UINT16            reserved4;              /**< Reserved */
    UINT32            length;                 /**< Data Length of Request */
} CMD_DEFS_CMD_REQ_HDR_T;

/** The  Command Protocol Header (Bulk Endpoint/12 byte) - Response Header */
typedef struct
{
    UINT8             unsol_rsp_flag  : 1;    /**< Unsolicited Response Flag */
    UINT8             data_flag       : 1;    /**< Response Data Flag */
    UINT8             fail_flag       : 1;    /**< Fail Flag */
    UINT8             cmd_version    : 4;    /**< CMD Version Number */
    UINT8             cmd_rsp_flag    : 1;    /**< Command/Response Flag */
    UINT8             seq_tag;                /**< Sequence Tag */
    CMD_DEFS_OPCODE_T  opcode;                 /**< Opcode */
    UINT8             reserved1;              /**< Reserved */
    UINT8             rsp_code;               /**< Response Code.  For real */
    UINT16            reserved2;              /**< Reserved */
    UINT32            length;                 /**< Data Length of Response */

} CMD_DEFS_CMD_RSP_HDR_T;

/*
 * BP master clear
 * BP master clear
 * Parameters:
 *    none
 * Return code:
 *    0     - BP master clear successfully
 *    other - error code
 */

int bp_master_clear(void);


#endif
