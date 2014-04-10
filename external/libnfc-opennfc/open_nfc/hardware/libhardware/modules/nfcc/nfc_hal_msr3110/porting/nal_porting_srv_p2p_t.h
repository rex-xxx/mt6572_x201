#ifndef __OPEN_NFC_MSTAR_SRV_P2P_T_H
#define __OPEN_NFC_MSTAR_SRV_P2P_T_H

#include "nal_porting_srv_p2p.h"


#define SDD_RF_PARAM_TARGET ( 0x00)
#define SDD_EXTERN_RF_DETECT (0x02)
#define SDD_RF_PARAM_TAG  	   ( 0x04)

extern DrvP2P_speed_et g_P2pSpeedType;
int ms_open_nfc_p2p_target_disp( tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_p2p_target_detection( tNALMsrComInstance  *pDev,  unsigned char SddRfParam);

int ms_nfc_p2p_target_enable_callback(tNALMsrComInstance  *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_nfc_p2p_target_enable( tNALMsrComInstance  *pDev, unsigned char SddRfParam);

#endif


