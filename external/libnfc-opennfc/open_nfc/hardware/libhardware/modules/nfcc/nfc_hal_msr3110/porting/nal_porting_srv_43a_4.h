#ifndef __OPEN_NFC_MSTAR_SRV_443A_4_H
#define __OPEN_NFC_MSTAR_SRV_443A_4_H

typedef struct __t43atype4Instance
{
    uint8_t ATQA[2];
    uint8_t SAK;
    uint8_t ATS_Frame[253];
    uint8_t UID_Len;
    uint8_t UID[10];
}t43atype4Instance;

#define FB_TA_MASK 0x40
#define FB_TB_MASK 0x20
#define FB_TC_MASK 0x10
#define R_BLK_ACK 0x00
#define R_BLK_NACK 0x10
int ms_open_nfc_43a_4_Inventory( tNALMsrComInstance  *dev );
int ms_open_nfc_43a_4_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int ms_open_nfc_43a_4_rats( tNALMsrComInstance  *dev );
#endif