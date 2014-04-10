#include "nal_porting_os.h"
#include "nal_porting_srv_util.h"

unsigned short CRC16( CRCTypeID type, unsigned short len, unsigned char *buf )
{
    unsigned short i, j, crc = 0;
    unsigned short POLYNOMIAL;

    if( type >= MAX_CRC_TYPE )
    {
        return 1;
    }

    if( type == CRC_ISO_18092_248 	||
            type == CRC_EPC_C1G2 		||
            type == CRC_HWIF			||
            type == CRC_HWIF_SDIO )
    {
        POLYNOMIAL = 0x1021;

        if( type == CRC_EPC_C1G2 ||
                type == CRC_HWIF )
        {
            crc = 0xFFFF;
        }
        else
        {
            crc = 0x0000;
        }

        for( i=0; i<len; i++ )
        {
            crc=(crc)^(((unsigned short)(*buf))<<8);
            for( j=0; j<8; j++ )
            {
                crc=(crc&0x8000)?((crc<<1)^POLYNOMIAL):(crc<<1);
            }
            buf++;
        }
    }
    else
    {
        if( type == CRC_ISO_15693 ||
                type == CRC_ISO_14443B )
        {
            crc = 0xFFFF;
        }
        else if( type == CRC_ISO_14443A ||
                 type == CRC_ISO_18092_106 )
        {
            crc = 0x6363;
        }

        POLYNOMIAL = 0x8408;

        for( i=0; i<len; i++ )
        {
            crc=(crc)^((unsigned short)(*buf));
            for( j=0; j<8; j++ )
            {
                crc=(crc&0x0001)?((crc>>1)^POLYNOMIAL):(crc>>1);
            }
            buf++;
        }

        if( type == CRC_ISO_15693 ||
                type == CRC_ISO_14443B )
        {
            crc=~(crc);
        }
    }

    return crc;
}

int ms_write_efuse_id(int FuseIdLen, unsigned char *pFuseId)
{
    PNALDebugLog("[ms_write_fuse_id]start...");
    
	FILE *fp;
	int idx = 0;
	fp=fopen("/data/nfc_info.txt","w+t");
	if (fp == NULL)
	{
        PNALDebugLog("[ms_write_fuse_id]Can't open FuseId file...");
		return 0;
	}
	PNALDebugLog("[ms_write_fuse_id], len = %d\n", FuseIdLen);
    PNALDebugLogBuffer(pFuseId, FuseIdLen);
	if (FuseIdLen <= 0)
	{			
	    fprintf(fp, "");
	}
	else
	{
		for (idx=0; idx<FuseIdLen; idx++)
		{
		    fprintf(fp, "%02X", pFuseId[idx]);
		}
		fprintf(fp, "\n");
	}
	fclose(fp);
	return 0;
}

bool_t ms_scan_gerenal_byte(int GbLen, unsigned char *pGbBuf)
{
    PNALDebugLog("[ms_scan_gerenal_byte]start...");
    PNALDebugLog("[ms_scan_gerenal_byte], len = %d\n", GbLen);
    PNALDebugLogBuffer(pGbBuf, GbLen);
    int idx = 0;
	for (idx = 0; idx <= GbLen - 3; idx++)
	{        
		if ((pGbBuf[idx] == 0x4D) && (pGbBuf[idx+1] == 0x53) && (pGbBuf[idx+2] == 0x52))
	    {
            PNALDebugLog("[ms_scan_gerenal_byte] scan ok: %x, %x, %x\n", pGbBuf[idx],pGbBuf[idx+1],pGbBuf[idx+2]);
			return W_TRUE;
	    }
	}
	PNALDebugLog("[ms_scan_gerenal_byte] scan fail");
	return W_FALSE;
}




