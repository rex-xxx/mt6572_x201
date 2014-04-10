#ifndef __OPEN_NFC_SRV_693_H
#define __OPEN_NFC_SRV_693_H

#define RTURN_BUF_LEN               64

/* Status Code */
typedef enum{
    RFID_STS_FAILED = 0,
    RFID_STS_SUCCESS
} RFID_Status;

/* Operation */
enum
{
    SET_RF_PARA = 0,
    GET_RF_PARA,
    TURN_OFF_RF,
    TURN_ON_RF,
    MAX_OPERATION_ID = TURN_ON_RF
};

/* ISO15693 Protocol Command Definition */
#define ISO15693_INVENTORY                                  0x01
#define ISO15693_STAY_QUIET                                 0x02
#define ISO15693_READ_SINGLE_BLOCK                          0x20
#define ISO15693_WRITE_SINGLE_BLOCK                         0x21
#define ISO15693_LOCK_BLOCK                                 0x22
#define ISO15693_READ_MULTIPLE_BLOCKS                       0x23
#define ISO15693_WRITE_MULTIPLE_BLOCKS                      0x24
#define ISO15693_SELECT                                     0x25
#define ISO15693_RESET_TO_READY                             0x26
#define ISO15693_WRITE_AFI                                  0x27
#define ISO15693_LOCK_AFI                                   0x28
#define ISO15693_WRITE_DSFID                                0x29
#define ISO15693_LOCK_DSFID                                 0x2A
#define ISO15693_GET_SYSTEM_INFORMATION                     0x2B
#define ISO15693_GET_MULTIPLE_BLOCKS_SECURITY_STATUS        0x2C

#define ISO15693_MSR3260_READ_EAS							0xB0
#define ISO15693_MSR3260_WRITE_EAS							0xB1
#define ISO15693_MSR3260_LOCK_EAS							0xB2
#define ISO15693_MSR3260_SECURE_READ						0xB3
#define ISO15693_MSR3260_SECURE_WRITE						0xB4
#define ISO15693_MSR3260_GET_OWNER_ID                       0xB5
#define ISO15693_MSR3260_WRITE_PASSWORD                     0xB6
#define ISO15693_MSR3260_LOCK_PASSWORD              		0xB7

#define ISO15693_MS_READ_EAS                                0xA0
#define ISO15693_MS_WRITE_EAS                               0xA1
#define ISO15693_MS_LOCK_EAS                                0xA2
#define ISO15693_MS_SECURE_READ                             0xA3
#define ISO15693_MS_SECURE_WRITE                            0xA4
#define ISO15693_MS_GET_OWNER_ID                            0xA5
#define ISO15693_MS_WRITE_PASSWORD                          0xA6
#define ISO15693_MS_LOCK_PASSWORD                           0xA7
#define ISO15693_MS_READ_KEY                                0xA8
#define ISO15693_MS_WRITE_KEY                               0xA9
#define ISO15693_MS_LOCK_KEY                                0xAA
#define ISO15693_MS_READ_REG                                0xAB
#define ISO15693_MS_WRITE_REG                               0xAC
#define ISO15693_MS_INIT_READ_SINGLE_BLOCK                  0xC0
#define ISO15693_MS_INIT_WRITE_SINGLE_BLOCK                 0xC1

#define ISO15693_FAST_INVENTORY                             0xB1
#define ISO15693_FAST_READ_SINGLE_BLOCK                     0xC0
#define ISO15693_FAST_WRITE_SINGLE_BLOCK                    0xC1

/* 15693 Flag Bits */
#define USE_TWO_SUBCARRIES                                  0x01
#define USE_HIGH_DATA_RATE                                  0x02
#define DO_INVENTORY                                        0x04
#define PROTOCOL_EXTENSION                                  0x08

/* Flag Bits when  DO_INVENTORY bit was not set */
#define SELECT_FLAG                                         0x10
#define ADDRESS_FLAG                                        0x20

/* Flag Bits when  DO_INVENTORY bit was set */
#define AFI_FLAG                                            0x10
#define ONE_SLOT_ONLY                                       0x20

#define OPTION_FLAG                                         0x40

/* 15693 Flag Bits Offset */
#define SUBCARRIES_FLAG_OFFSET                              0
#define DATA_RATE_FLAG_OFFSET                               1
#define INVENTORY_FLAG_OFFSET                               2
#define PROTOCOL_EXTENSION_FLAG_OFFSET                      3

/* When Inventory Flag was NOT set */
#define SELECT_FLAG_OFFSET                                  4
#define ADDRESS_FLAG_OFFSET                                 5

/* When Inventory Flag was set */
#define AFI_FLAG_OFFSET                                     4
#define NO_SLOTS_FLAG_OFFSET                                5

#define OPTION_FLAG_OFFSET                                  6

#define MSTAR_ID                                            0x2F
#define FUJITSU_ID                                          0x08

/* ISO15693 Protocol SOF and EOF pattern */
#define ISO15693_SOF                                        0x7B
#define ISO15693_EOF                                        0xDF

/* Option Offset */
#define ISO15693_SPEED_FLAG_OFFSET          0
#define ISO15693_MOD_INDEX_FLAG_OFFSET      1

/* 15693 Response Flag */
#define HAS_ERROR           0x01

/* Information Flag of Get Sys Info response */
#define SUPPORT_DSFID           0x01
#define SUPPORT_AFI             0x02
#define SUPPORT_VICC_MEM        0x04
#define SUPPORT_IC_REF          0x08

/* ISO15693: Structure for INVENTORY Command */
typedef struct cmd_inventory_s_type_tag
{
    unsigned char Flag;
    unsigned char AFI;
    unsigned char Mask_Len;
    unsigned char Mask[8];
}cmd_inventory_s_type;

typedef cmd_inventory_s_type* pCmd_Inventory;

/* ISO15693: Structure for READ SINGLE BLOCK Command */
typedef struct cmd_read_single_block_s_type_tag
{
    unsigned char Flag;
    unsigned char UID[8];
    unsigned char Block_ID;
    unsigned char SecureFW;
}cmd_read_single_block_s_type;

typedef cmd_read_single_block_s_type* pCmd_Read_Single_Block;

/* ISO15693: Structure for WRITE SINGLE BLOCK Command */
typedef struct cmd_write_single_block_s_type_tag
{
    unsigned char Flag;
    unsigned char UID[8];
    unsigned char Block_ID;
    unsigned char Data_Len;    // Byte Unit
    unsigned char Data[8];
    unsigned char SecureFW;
}cmd_write_single_block_s_type;

typedef cmd_write_single_block_s_type* pCmd_Write_Single_Block;

/* Structure for GET SYS INFO Command */
typedef struct cmd_get_sys_info_s_type_tag
{
    unsigned char Flag;
    unsigned char UID[8];
}cmd_get_sys_info_s_type;

typedef cmd_get_sys_info_s_type* pCmd_Get_Sys_Info;

/* Structure for SRF Command */
typedef struct cmd_SRF_s_type_tag
{
    unsigned char op;
    unsigned char mode;
    unsigned char *para;
    unsigned char para_len;
}cmd_SRF_s_type;

typedef cmd_SRF_s_type* pCmd_SRF;

int ms_open_nfc_693_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int ms_A3_RFID_693_Inventory(tNALMsrComInstance *dev);
int ms_A3_RFID_Turn_Off_HF( tNALMsrComInstance  *dev );
int ms_open_nfc_693_recv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);
int ms_A3_RFID_693_Inventory_Callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);
#endif /* __OPEN_NFC_SRV_693_H */


