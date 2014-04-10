#ifndef __OPEN_NFC_MSTAR_SRV_type1
#define __OPEN_NFC_MSTAR_SRV_type1


#include "nfc_hal.h"



int ms_open_nfc_type1_disp( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_inventory( tNALMsrComInstance *pDev);
int ms_open_nfc_type1_cmd_xchg_data( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_detection_sens_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_detection_sens_new_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);



#endif

