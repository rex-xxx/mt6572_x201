#ifndef __OPEN_NFC_SRV_FELICA_H
#define __OPEN_NFC_SRV_FELICA_H

int ms_open_nfc_felica_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int ms_open_nfc_felica_Inventory( tNALMsrComInstance  *dev );
int ms_open_nfc_felica_Inventory_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);

#endif