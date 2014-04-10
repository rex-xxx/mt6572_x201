#ifndef __OPEN_NFC_MSTAR_SRV_43B_H
#define __OPEN_NFC_MSTAR_SRV_43B_H


#include "nfc_hal.h"



int ms_open_nfc_43b_cbrecv( tNALMsrComInstance  *dev, unsigned char *inbuf, int len);
int ms_open_nfc_43b_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int A3_RFID_43B_Inventory( tNALMsrComInstance  *dev);
int ms_open_nfc_43b_inventory_cbrecv( tNALMsrComInstance  *dev, unsigned char *inbuf, int len);


// Todo-01: need code enhancement in the future for multipackage transmission
int ms_open_nfc_43b_cmd_xchg_data( tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int A3_RFID_AT43B_Read_Block( tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int A3_RFID_AT43B_Write_Block( tNALMsrComInstance  *dev, unsigned char *outbuf, int len);




//static int ms_open_nfc_admin_cmd_get_parameter(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
//static int ms_open_nfc_admin_cmd_set_parameter(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);

#endif

