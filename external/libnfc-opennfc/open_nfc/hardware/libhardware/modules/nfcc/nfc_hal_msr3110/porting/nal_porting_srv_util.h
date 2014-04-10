#ifndef __OPEN_NFC_MSTAR_SRV_UTIL_H
#define __OPEN_NFC_MSTAR_SRV_UTIL_H

// enumeration for CRC16
typedef enum
{
    CRC_ISO_14443A = 0,
    CRC_ISO_14443B,
    CRC_ISO_15693,
    CRC_ISO_18092_106,
    CRC_ISO_18092_248,
    CRC_EPC_C1G2,
    CRC_HWIF,
    CRC_HWIF_SDIO,
    MAX_CRC_TYPE
} CRCTypeID;


unsigned short CRC16( CRCTypeID type, unsigned short len, unsigned char *buf );
int ms_write_efuse_id(int FuseIdLen, unsigned char *pFuseId);
bool_t ms_scan_gerenal_byte(int GbLen, unsigned char *pGbBuf);



#endif


