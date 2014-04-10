#ifndef __OPEN_NFC_MSTAR_SRV_P2P_I_H
#define __OPEN_NFC_MSTAR_SRV_P2P_I_H

#define CMD_0_REQ    0xD4
#define CMD_1_ATR_REQ    0x00
#define CMD_1_DEP_REQ    0x06
#define CMD_1_DSL_REQ    0x08
#define CMD_1_RLS_REQ    0x0A


#define DIDi   0x00
#define BSi    0x00
#define BRi    0x00
#define PPi    0x20

#define PDU_MASK 0xE0
#define PDU_INF  0x00
#define PDU_ACK 0x40
#define PDU_SUP 0x80
#define DEP_RES_NACK 0x10
#define DEP_RES_ACK 0x00
#define DEP_MI_MASK 0x10
#define DEP_MI_ENABLE 0x10
#define DEP_MI_DISABLE 0x00
#define DEP_DID_ENABLE 0x04
#define DEP_DID_DISABLE 0x00
#define SUP_TYPE_ATN 0x00
#define SUP_TYPE_TO 0x10
#define SUP_TYPE_TO_MASK 0x10
#define ATR_GB_ENABLE 0x02

#define ATR_REQ_MAX_LEN 0x40
#define SB 0xF0
#define SoD_LEN 2




int ms_open_nfc_initiator_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
//int ms_open_nfc_initiator_detection( tNALMsrComInstance  *dev );
int ms_open_nfc_initiator_atr_req( tNALMsrComInstance  *dev );
int ms_open_nfc_initiator_detection_212_424_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);
int ms_open_nfc_initiator_detection_212_424( tNALMsrComInstance  *dev );


#endif


