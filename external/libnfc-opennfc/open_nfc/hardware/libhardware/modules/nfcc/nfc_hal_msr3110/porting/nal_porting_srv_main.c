#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>

#include <arpa/inet.h>
#include <semaphore.h>  /* Semaphore */

#include "nfc_hal.h"
#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "interface.h"
#include "nal_porting_srv_main.h"



// ==================== data structures ====================

// ==================== global data members ====================

// ==================== function list ====================

/* public functions */
int ms_crc16_chk( CRCTypeID CrcTypeId, unsigned char *pDataContent, int DataContentLen);



/* public functions */
int ms_crc16_chk( CRCTypeID CrcTypeId, unsigned char *pDataContent, int DataContentLen)
{
	int retVal = MS_CRC16_CHK_SUCCESS;

	PNALDebugLog( "ms_crc16_chk() - start ");

	PNALDebugLogBuffer( pDataContent, DataContentLen);

	unsigned short crcDataContent;	//CRC16 in DataContent;
	unsigned short crcCal;			//Calculated CRC16 of DataContent

	switch( CrcTypeId)
	{
		case CRC_ISO_18092_248:
		case CRC_EPC_C1G2:
		case CRC_HWIF:
		case CRC_HWIF_SDIO:
			crcDataContent = ( pDataContent[ DataContentLen - 2] << 8) | pDataContent[ DataContentLen - 1];
			break;
			
		default:
			crcDataContent = ( pDataContent[ DataContentLen - 1] << 8) | pDataContent[ DataContentLen - 2];
			break;
			
	}
	//crcDataContent = ( pDataContent[ DataContentLen - 1] << 8) | pDataContent[ DataContentLen - 2];
	crcCal = CRC16( CrcTypeId, DataContentLen - 2, pDataContent);

	if( crcDataContent != crcCal)
	{

		retVal = MS_CRC16_CHK_FAIL;
	}

	PNALDebugLog( "CRC: 0x %X ", crcDataContent);
	PNALDebugLog( "CRC: 0x %X ", crcCal);

	PNALDebugLog( "ms_crc16_chk() - end ");
	return retVal;

}



