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
#include "nal_porting_srv_util.h"
#include "nal_porting_srv_type1.h"


// ==================== data structures ====================



// ==================== global data members ====================
#define NFC_READER_TYPE1_CMD_RID 0x78
#define NFC_READER_TYPE1_CMD_RALL 0x00
#define NFC_READER_TYPE1_CMD_READ 0x01
#define NFC_READER_TYPE1_CMD_WRITE_E 0x53
#define NFC_READER_TYPE1_CMD_WRITE_NE 0x1A
#define NFC_READER_TYEP1_CMD_RSEG 0x10
#define NFC_READER_TYPE1_CMD_READ8 0x02
#define NFC_READER_TYPE1_CMD_WRITE_E8 0x54
#define NFC_READER_TYPE1_CMD_WRITE_NE8 0x1B

#define NFC_READER_TYPE1_SENS_RES_MASK 0x0C

extern unsigned char CUR_CARD_TYPE;
extern unsigned char Extern_RF_Detect;

// ==================== function list ====================

extern int ms_card_detection(tNALMsrComInstance  *dev);

/* public function */
int ms_open_nfc_type1_detection( tNALMsrComInstance *pDev);
int ms_open_nfc_type1_disp( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_cmd_xchg_data( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);

/* private function */
static int ms_open_nfc_type1_sens_req( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_all_req( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_rid( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_rall( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_read( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_write_e( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_write_ne( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_rseg( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_read8( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_write_e8( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
static int ms_open_nfc_type1_write_ne8( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);

/* public callback function */
int ms_open_nfc_type1_detection_sens_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_detection_rid_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_cmd_xchg_data_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);

int ms_open_nfc_type1_sens_req_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_all_req_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_rid_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_rall_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_read_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_write_e_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_write_ne_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_rseg_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_read8_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_write_e8_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);
int ms_open_nfc_type1_write_ne8_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen);

/* utilities with A3 */
static int ms_a3_type1_sens_req( tNALMsrComInstance  *pDev);
static int ms_a3_type1_all_req( tNALMsrComInstance  *pDev);
static int ms_a3_type1_rid_req( tNALMsrComInstance  *pDev);
static int ms_a3_type1_rall_req( tNALMsrComInstance  *pDev);
static int ms_a3_type1_read_req( tNALMsrComInstance  *pDev);
static int ms_a3_type1_write_e_req( tNALMsrComInstance  *pDev);
static int ms_a3_type1_write_ne_req( tNALMsrComInstance  *pDev);

// utilities in local



// ==================== function content ====================

/* public functions */
int ms_open_nfc_type1_detection( tNALMsrComInstance *pDev)
{
	PNALDebugLog( "ms_open_nfc_type1_detection() - start ");
	int retVal = 0;

	retVal = ms_a3_type1_sens_req( pDev);
	pDev->cbRecv = (RecvCallBack *)ms_open_nfc_type1_detection_sens_callback;

	PNALDebugLog( "ms_open_nfc_type1_detection() - end ");
	return retVal;
}

static unsigned char Atqa[2] = { 0x00, 0x00};
int ms_open_nfc_type1_detection_sens_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - start ");
	int retVal = 0;
	int xchgDataLen = 0;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS|
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]Internal contral: A3 reset.");

			PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]back to main detection loop");
			ms_card_detection( pDev);

			PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]Internal contral: unknown STS code.");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
		return 0;

	}

	// Reader: 0x02 | Len(1)| STS(1) | Rpt_Bit_Len(1)| Rpt_Bit_Len(1)|Data(ATQA: 2 bytes) |

	PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
                   case 0x08:  //extern RF
                          PNALDebugLog("[ms_open_nfc_type1_detection_sens_callback]Extern RF, Do Target:");
                          //CUR_CARD_TYPE = 0x05;
                          Extern_RF_Detect = 0x01;
                          ms_card_detection( pDev);
                          return 0;

			default:
				PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]Other error return.");

				PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]back to main detection loop");
				ms_card_detection( pDev);

				PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
				return 0;
		}
	}

	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]pRcvBuf:");
    PNALDebugLogBuffer( pRcvBuf, RcvLen);

	// Data not long enough
	if( RcvLen < 7)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]Data not long enough: less than 7");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
		return 0;
	}

	//|0x02|Len|Status|0x00|0x10|ATQA(2)|
	memcpy( Atqa, pRcvBuf + 5, 2);
	if ((Atqa[1] & NFC_READER_TYPE1_SENS_RES_MASK) == NFC_READER_TYPE1_SENS_RES_MASK)
	{
		retVal = ms_a3_type1_rid_req( pDev);
	    pDev->cbRecv = (RecvCallBack *)ms_open_nfc_type1_detection_rid_callback;
	}
	else
	{
	    PNALDebugLog( "[ms_open_nfc_type1_detection_sens_callback]not type1 tag, back to main detection loop");
		ms_card_detection( pDev);
	}

	PNALDebugLog( "ms_open_nfc_type1_detection_sens_callback() - end ");
	return retVal;
}

int ms_open_nfc_type1_detection_sens_new_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - start ");
	int retVal = 0;
	int xchgDataLen = 0;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS|
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]Internal contral: A3 reset.");

			PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]back to main detection loop");
			ms_card_detection( pDev);

			PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]Internal contral: unknown STS code.");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
		return 0;

	}

	// Reader: 0x02 | Len(1)| STS(1) | Rpt_Bit_Len(1)| Rpt_Bit_Len(1)|Data(ATQA: 2 bytes) |

	PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
                   case 0x08:  //extern RF
                          PNALDebugLog("[ms_open_nfc_type1_detection_sens_new_callback]Extern RF, Do Target:");
                          //CUR_CARD_TYPE = 0x05;
                          Extern_RF_Detect = 0x01;
                          ms_card_detection( pDev);
                          return 0;

			default:
				PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]Other error return.");

				PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]back to main detection loop");
				ms_card_detection( pDev);

				PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
				return 0;
		}
	}

	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]pRcvBuf:");
    PNALDebugLogBuffer( pRcvBuf, RcvLen);

	// Data not long enough
	if( RcvLen < 7)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]Data not long enough: less than 7");

		PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
		return 0;
	}
    //Recv Format
	//|0x02|Len|Status|0x00|0x10|ATQA(2)|Status|0x00|0x10|HR0|HR1|UID0|UID1|UID2|UID3|CRC_L|CRC_H|
	memcpy( Atqa, pRcvBuf + 5, 2);
	if ((Atqa[1] & NFC_READER_TYPE1_SENS_RES_MASK) == NFC_READER_TYPE1_SENS_RES_MASK)
	{
		pDev->temp_rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
		pDev->temp_rx_buffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;
		memcpy( pDev->temp_rx_buffer + 2, pRcvBuf + 5 + 2 + 5, 4); //copy UID
		memcpy( pDev->temp_rx_buffer + 6, pRcvBuf + 5 + 5, 2); // copy HR
		memcpy( pDev->temp_rx_buffer + 8, Atqa, 2);
		//pDev->nb_available_bytes = 10;
		SetEventAvailableBytes(pDev, 10);
	}
	else
	{
	    PNALDebugLog( "[ms_open_nfc_type1_detection_sens_new_callback]not type1 tag, back to main detection loop");
		ms_card_detection( pDev);
	}

	PNALDebugLog( "ms_open_nfc_type1_detection_sens_new_callback() - end ");
	return retVal;
}


int ms_open_nfc_type1_detection_rid_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - start ");
	int retVal = 0;
	int xchgDataLen = 0;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]recieved buffer is null");

		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]recieved buffer length is less than 3.");

		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS| Data| CRC16
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]Internal contral: A3 reset.");

			PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
			ms_card_detection( pDev);

			PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]Internal contral: unknown STS code.");

		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
		return 0;

	}

	// Reader: 0x02 | Len| STS | Rpt_Bit_Len| Rpt_Bit_Len|Data | CRC16

	PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
			default:
				PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]Other error return.");

				PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
				ms_card_detection( pDev);

				PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
				return 0;
		}
	}

	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]pRcvBuf:");
    PNALDebugLogBuffer( pRcvBuf, RcvLen);

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 8)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]No Data or not long enough for CRC16 calculation");

		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
		return 0;
	}

	// check CRC
	if( ms_crc16_chk( CRC_ISO_14443B, pRcvBuf + 5, pRcvBuf[1] - 3) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback] CRC error");

		PNALDebugLog( "[ms_open_nfc_type1_detection_rid_callback]back to main detection loop");
		ms_card_detection( pDev);

		PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
		return 0;
	}

	//|0x02|Len|Status|0x00|0x10|HR0|HR1|UID0|UID1|UID2|UID3|CRC_L|CRC_H|
	pDev->temp_rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
    pDev->temp_rx_buffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;
	memcpy( pDev->temp_rx_buffer + 2, pRcvBuf + 5 + 2, 4); //copy UID
	memcpy( pDev->temp_rx_buffer + 6, pRcvBuf + 5, 2); // copy HR
	memcpy( pDev->temp_rx_buffer + 8, Atqa, 2);
    //pDev->nb_available_bytes = 10;
       SetEventAvailableBytes(pDev, 10);
	PNALDebugLog( "ms_open_nfc_type1_detection_rid_callback() - end ");
	return retVal;
}


int ms_open_nfc_type1_disp( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_type1_disp() - start ");

    int retVal = 0;

	PNALDebugLog( "[ms_open_nfc_type1_disp]pRcvBuf: ");
	PNALDebugLogBuffer( pRcvBuf, RcvLen);
	PNALDebugLog( "[ms_open_nfc_type1_disp]RcvLen: %d", RcvLen);

    switch( pDev->curCmdCode)
    {
        case NAL_CMD_READER_XCHG_DATA:
            retVal = ms_open_nfc_type1_cmd_xchg_data( pDev, pRcvBuf + 3, RcvLen - 3);
            break;

        default:
            //pDev->rx_buffer[0] = NAL_SERVICE_P2P_TARGET;
            //pDev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
            //pDev->nb_available_bytes = 2;

			// Do Nothing
            break;
    }

	PNALDebugLog( "ms_open_nfc_type1_disp() - end ");

    return retVal;
}


int ms_open_nfc_type1_cmd_xchg_data( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data() - start");
	int retVal = 0;

	int a3IntSendRet = 0;
	unsigned short dataCrc16;
	int rptBitLen;

	PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data]pRcvBuf: ");
	PNALDebugLogBuffer( pRcvBuf, RcvLen);
	PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data]RcvLen: %d", RcvLen);

	//Format: | Cmd(1)| Len(1)| Option ID1(1)| Option ID2(1)| UniProtocol(1)| req bit len(2)| rcv bit len(2)| data(n)|CRC16(L)|CRC16(H)
	unsigned char cmdReaderReq[255];
	unsigned char cmdReaderReqHeader[] = {0x42, 0x00, 0x80, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00};
	int cmdReaderReqLen = 9;

	memcpy( cmdReaderReq, cmdReaderReqHeader, 9);
	if( RcvLen <= 0)
	{
		//do nothing
		return retVal;
	}

	memcpy( cmdReaderReq + 9, pRcvBuf, RcvLen);
	dataCrc16 = CRC16( CRC_ISO_14443B, RcvLen, pRcvBuf);
	cmdReaderReq[ 9 + RcvLen    ] = ( dataCrc16     )& 0xFF;
	cmdReaderReq[ 9 + RcvLen + 1] = ( dataCrc16 >> 8)& 0xFF;
	cmdReaderReq[1] = 7 + RcvLen + 2;
	rptBitLen = ( RcvLen + 2) * 8;
	cmdReaderReq[5] = ( rptBitLen >> 8) & 0xFF;
	cmdReaderReq[6] = ( rptBitLen     ) & 0xFF;
	cmdReaderReqLen = 9 + RcvLen + 2;
	PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data]cmdReaderReqLen: %d", cmdReaderReqLen);

	memcpy( pDev->ant_send_buffer, cmdReaderReq, cmdReaderReqLen);
	a3IntSendRet = ms_interfaceSend( pDev, cmdReaderReqLen);
	retVal = a3IntSendRet;
	pDev->cbRecv = (RecvCallBack *)ms_open_nfc_type1_cmd_xchg_data_callback;

	PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data() - end");
	return retVal;
}

int ms_open_nfc_type1_cmd_xchg_data_callback( tNALMsrComInstance *pDev, unsigned char *pRcvBuf, int RcvLen)
{
	PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - start ");
	int retVal = 0;
	int xchgDataLen = 0;
	unsigned char recRfByteLen = 0;

	if( pRcvBuf == NULL || RcvLen <= 0)
	{
		PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]recieved buffer is null");

		pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
		pDev->rx_buffer[1] = NAL_RES_TIMEOUT;
		pDev->nb_available_bytes = 2;

		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
		return 0;
	}

	if( RcvLen < 3)
	{
		PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]recieved buffer length is less than 3.");

		pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
		pDev->rx_buffer[1] = NAL_RES_TIMEOUT;
		pDev->nb_available_bytes = 2;

		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
		return 0;
	}

	// Internal: 0xAA | Len| STS| Data| CRC16
	if( pRcvBuf[0] == 0xAA)
	{
		PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]Return from internal control...");
		if( pRcvBuf[3] == 0x00)
		{
			PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]Internal contral: A3 reset.");

        	pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
			pDev->rx_buffer[1] = NAL_RES_TIMEOUT;
			pDev->nb_available_bytes = 2;

			PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
			return 0;
		}

		PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]Internal contral: unknown STS code.");

    	pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
		pDev->rx_buffer[1] = NAL_RES_TIMEOUT;
		pDev->nb_available_bytes = 2;

		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
		return 0;
	}

	// Reader: 0x02 | Len| STS | Rpt_Bit_Len| Rpt_Bit_Len|Data | CRC16

	PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]STS: %02x.", pRcvBuf[2]);
	if( pRcvBuf[2] != RFID_STS_SUCCESS)
	{
		PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]STS != RFID_STS_SUCCESS.");
		switch( pRcvBuf[2])
		{
			default:
				PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]Other error return.");

				pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
				pDev->rx_buffer[1] = NAL_RES_TIMEOUT;
				pDev->nb_available_bytes = 2;

				PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
				return 0;
		}
	}

	// output the recieved buffer
	PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]RcvLen: %d", RcvLen);
	PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]pRcvBuf:");
    PNALDebugLogBuffer( pRcvBuf, RcvLen);

	// No Data or not long enough for CRC16 calculation
	if( RcvLen < 8)
	{
		PNALDebugLog( "[ms_open_nfc_type1_cmd_xchg_data_callback]No Data or not long enough for CRC16 calculation");

		pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
		pDev->rx_buffer[1] = NAL_RES_PROTOCOL_ERROR;
		pDev->nb_available_bytes = 2;

		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
		return 0;
	}
	if (pRcvBuf[4] % 8 == 0)
	{
	    recRfByteLen = pRcvBuf[1] - 3;
	}
	else
	{
	    recRfByteLen = pRcvBuf[1] - 4;
		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - recv noise, len mod 8 <> 0 ");
	}
	// check CRC
	//if( ms_crc16_chk( CRC_ISO_14443B, pRcvBuf + 5, pRcvBuf[1] - 3) != MS_CRC16_CHK_SUCCESS)
	if( ms_crc16_chk( CRC_ISO_14443B, pRcvBuf + 5, recRfByteLen) != MS_CRC16_CHK_SUCCESS)
	{
		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback(): CRC error");

		pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
		pDev->rx_buffer[1] = NAL_RES_PROTOCOL_ERROR;
		pDev->nb_available_bytes = 2;

		PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
		return 0;
	}

	pDev->rx_buffer[0] = NAL_SERVICE_READER_TYPE_1;
	pDev->rx_buffer[1] = NAL_RES_OK;
	memcpy( pDev->rx_buffer + 2, pRcvBuf + 5, recRfByteLen - 2);
	pDev->nb_available_bytes = 2 + ( recRfByteLen - 2);


	PNALDebugLog( "ms_open_nfc_type1_cmd_xchg_data_callback() - end ");
	return retVal;
}


/* private functions */

/* utilities with A3 */
static int ms_a3_type1_sens_req( tNALMsrComInstance  *pDev)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_type1_sens_req() - start ");

	//Format: | Cmd(1)| Len(1)| Option ID1(1)| Option ID2(1)| UniProtocol(1)| req bit len(2)| rcv bit len(2)| data(1)
	unsigned char cmdA3Req[] = { 0x42, 0x08, 0x80, 0x00, 0x03, 0x00, 0x07, 0x00, 0x13, 0x52};
	//unsigned char cmdA3Req[] = { 0x42, 0x08, 0x80, 0x00, 0x01, 0x00, 0x07, 0x00, 0x13, 0x52};
	int cmdA3ReqLen;
	int a3IntSendRet;

	cmdA3ReqLen = 10;

	memcpy( pDev->ant_send_buffer, cmdA3Req, cmdA3ReqLen);
	a3IntSendRet = ms_interfaceSend( pDev, cmdA3ReqLen);

	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_type1_sens_req() - end ");
	return retVal;
}

static int ms_a3_type1_all_req( tNALMsrComInstance  *pDev)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_p2p_target_all_req() - start ");

	//Format: | Cmd(1)| Len(1)| Option ID1(1)| Option ID2(1)| UniProtocol(1)| req bit len(2)| rcv bit len(2)| data(1)
	unsigned char cmdA3Req[] = { 0x42, 0x08, 0x80, 0x00, 0x03, 0x00, 0x07, 0x00, 0x13, 0x26};
	//unsigned char cmdA3Req[] = { 0x42, 0x08, 0x80, 0x00, 0x01, 0x00, 0x07, 0x00, 0x13, 0x26};
	int cmdA3ReqLen;
	int a3IntSendRet;

	cmdA3ReqLen = 10;

	memcpy( pDev->ant_send_buffer, cmdA3Req, cmdA3ReqLen);
	a3IntSendRet = ms_interfaceSend( pDev, cmdA3ReqLen);

	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_p2p_target_all_req() - end ");
	return retVal;
}

#define A3_NFC_TYPE1_RID_CMD_LEN 18
#define NFC_TYPE1_RID_CMD_LEN 7
static int ms_a3_type1_rid_req( tNALMsrComInstance  *pDev)
{
	int retVal = 0;

	PNALDebugLog( "ms_a3_p2p_target_rid_req() - start ");

	//Format: | Cmd(1)| Len(1)| Option ID1(1)| Option ID2(1)| UniProtocol(1)| req bit len(2)| rcv bit len(2)| data(7)  |CRC16(L)| CRC16(H)
	unsigned char cmdA3Req[] = { 0x42, 0x10, 0x80, 0x00, 0x03, 0x00, 0x48, 0x00, 0x00,
								 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xD0, 0x43};
	int cmdA3ReqLen;
	int a3IntSendRet;

	cmdA3ReqLen = A3_NFC_TYPE1_RID_CMD_LEN;

	memcpy( pDev->ant_send_buffer, cmdA3Req, cmdA3ReqLen);
	a3IntSendRet = ms_interfaceSend( pDev, cmdA3ReqLen);

	//pDev->cbRecv = (RecvCallBack *)ms_open_nfc_p2p_target_detection_callback;

	retVal = a3IntSendRet;

	PNALDebugLog( "ms_a3_p2p_target_rid_req() - end ");
	return retVal;
}








