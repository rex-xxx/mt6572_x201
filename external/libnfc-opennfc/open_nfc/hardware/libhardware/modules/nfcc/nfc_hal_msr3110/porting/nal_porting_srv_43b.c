#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>

#include <arpa/inet.h>
#include <semaphore.h>  /* Semaphore */

#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "interface.h"
#include "nfc_hal.h"
#include "nal_porting_srv_main.h"
#include "nal_porting_srv_43b.h"
#include "nal_porting_srv_43a_4.h"



// local action code control table
#define L_ACT_ISO1443B_IVENTORY_AT 			0x01
#define L_ACT_ISO1443B_IVENTORY_ST 			0x02
#define L_ACT_ISO1443B_READ_BLOCK_AT		0x11
#define L_ACT_ISO1443B_READ_BLOCK_ST		0x12
#define L_ACT_ISO1443B_WRITE_BLOCK_AT		0x21
#define L_ACT_ISO1443B_WRITE_BLOCK_ST		0x22
#define L_ACT_ISO1443B_XCHG_DATA			0x31

// global command code control table
#define G_CMD_ISO14443B_READ_BLOCK_AT		0x12
#define G_CMD_ISO14443B_WRITE_BLOCK_AT		0x11
#define G_CMD_ISO14443B_READ_BLOCK_ST		0x08
#define G_CMD_ISO14443B_WRITE_BLOCK_ST		0x09


// global variables
static unsigned char CUR_ACT_CODE = 0x00;

typedef unsigned char u8;
typedef unsigned short u16;
static unsigned char DEP_BUF[1024];
static unsigned char DEP_LEN = 0;
static unsigned char BLK_NUM;
static unsigned char TEMP_OUT_BUF[128];
static unsigned char TEMP_OUT_BUF_LEN;
static u8 CUR_DEP_CNT;
static bool_t NACK_SEND_FLAG = W_FALSE;
static u8 MAX_DEP_TRY_CNT = 0;



extern int ms_card_detection(tNALMsrComInstance  *dev);

int ms_open_nfc_43b_cbrecv( tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
	int retval = 0;
    unsigned char mblival = 0x00;
	PNALDebugLog( "ms_open_nfc_43b_cbrecv() - start");

	int hlrdlen = 0; // length of high layer response data

	PNALDebugLog( "[ms_open_nfc_43b_cbrecv]CUR_ACT_CODE: %02X \n", CUR_ACT_CODE);
	switch( CUR_ACT_CODE)
	{
		case L_ACT_ISO1443B_IVENTORY_AT:
			PNALDebugLog( "[ms_open_nfc_43b_cbrecv]L_ACT_ISO1443B_IVENTORY_AT - start\n");
			if( len > 0)
			{
				if( inbuf[2] == RFID_STS_SUCCESS)
				{
					PNALDebugLog( "[ms_open_nfc_43b_cbrecv]L_ACT_ISO1443B_IVENTORY_AT: RFID_STS_SUCCESS \n");
					dev->temp_rx_buffer[0] = NAL_SERVICE_READER_14_B_4;
					dev->temp_rx_buffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;
					memcpy( dev->temp_rx_buffer +  2, inbuf +   3, 11); //ATQB
					memcpy( dev->temp_rx_buffer + 13, inbuf +  14,  1); //MBLI+CID
					mblival = inbuf[14] & 0xF0;
					if (mblival > 0)
					{
						hlrdlen = len - 13 - 2; // 2: crc
						if( hlrdlen > 0 )
						{
							memcpy( dev->temp_rx_buffer + 14, inbuf + 15, hlrdlen); //High Layer Response Data
						}
					}
					//dev->nb_available_bytes = 14 + hlrdlen;
					SetEventAvailableBytes(dev, 14 + hlrdlen);
					BLK_NUM = 0;
				}
				else
				{
					PNALDebugLog( "[ms_open_nfc_43b_cbrecv]L_ACT_ISO1443B_IVENTORY_AT: return to card detection \n");
					//#warning "need to run detect procedure here"
					ms_card_detection(dev);
				}

			}
			PNALDebugLog( "[ms_open_nfc_43b_cbrecv]L_ACT_ISO1443B_IVENTORY_AT - end\n");
			break;

		case L_ACT_ISO1443B_XCHG_DATA:
			dev->rx_buffer[0] = NAL_SERVICE_READER_14_B_4;
			if( len > 0)
			{
				dev->rx_buffer[1] = NAL_RES_OK;
				memcpy( dev->rx_buffer + 2, inbuf + 3, len - 3);
				dev->nb_available_bytes = len - 1;
			}
			else
			{
				dev->rx_buffer[1] = NAL_RES_PROTOCOL_ERROR;
				dev->nb_available_bytes = 2;
			}
			break;

		default:
			dev->rx_buffer[0] = NAL_SERVICE_READER_14_B_4;
			dev->rx_buffer[1] = NAL_RES_PROTOCOL_ERROR;
			dev->nb_available_bytes = 2;
			break;

	}

	PNALDebugLog( "ms_open_nfc_43b_cbrecv() - end");
	return retval;

}


int ms_open_nfc_43b_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = 0;
    if (outbuf == NULL || len == 0)
    {
        return -1;
    }

    switch( dev->curCmdCode)
    {
        case NAL_CMD_GET_PARAMETER:
            //retval = ms_open_nfc_admin_cmd_get_parameter( dev, outbuf, len);
            break;

        case NAL_CMD_SET_PARAMETER:
			//retval = ms_open_nfc_admin_cmd_set_parameter(dev, outbuf, len);
            break;

		case NAL_CMD_READER_XCHG_DATA:
            DEP_LEN = 0;
            CUR_DEP_CNT = 0;
            memcpy(TEMP_OUT_BUF, outbuf, len);
            TEMP_OUT_BUF_LEN = len;			
			retval = ms_open_nfc_43b_cmd_xchg_data(dev, outbuf, len);
            break;

        default:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
			dev->nb_available_bytes = 2;
            break;
    }
    return retval;
}

int ms_open_nfc_43b_inventory_cbrecv( tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
	int retval = 0;
    unsigned char mblival = 0x00;
	PNALDebugLog( "ms_open_nfc_43b_inventory_cbrecv() - start");

	int hlrdlen = 0; // length of high layer response data
	if( len > 0)
	{
		if( inbuf[2] == RFID_STS_SUCCESS)
		{
			PNALDebugLog( "[ms_open_nfc_43b_inventory_cbrecv]RFID_STS_SUCCESS \n");
			dev->temp_rx_buffer[0] = NAL_SERVICE_READER_14_B_4;
			dev->temp_rx_buffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;
			memcpy( dev->temp_rx_buffer +  2, inbuf +   3, 11); //ATQB
			memcpy( dev->temp_rx_buffer + 13, inbuf +  14,  1); //MBLI+CID
			mblival = inbuf[14] & 0xF0;
			if (mblival > 0)
			{
				hlrdlen = len - 13 - 2; // 2: crc
				if( hlrdlen > 0 )
				{
					memcpy( dev->temp_rx_buffer + 14, inbuf + 15, hlrdlen); //High Layer Response Data
				}
			}
			//dev->nb_available_bytes = 14 + hlrdlen;
			SetEventAvailableBytes(dev, 14 + hlrdlen);
			BLK_NUM = 0;
		}
		else
		{
			PNALDebugLog( "[ms_open_nfc_43b_inventory_cbrecv]return to card detection \n");
			//#warning "need to run detect procedure here"
			ms_card_detection(dev);
		}

	}

	PNALDebugLog( "ms_open_nfc_43b_inventory_cbrecv() - end");
	return retval;

}



int A3_RFID_43B_Inventory( tNALMsrComInstance  *dev)
{
	int retval = 0;

	PNALDebugLog( "A3_RFID_43B_Inventory() - start \n");


	int interfaceSendRet = 0;

	dev->ant_send_buffer[0] = 0x48;  //0x48 is new for OpenNFC, 0x47 kept for old application
	dev->ant_send_buffer[1] = 0x03;
	dev->ant_send_buffer[2] = 0x10;
	dev->ant_send_buffer[3] = 0x00;
	dev->ant_send_buffer[4] = 0x03;

	//if( ms_interfaceSend( dev, 5) != 0)
	interfaceSendRet = ms_interfaceSend( dev, 5);
	retval = interfaceSendRet;
	//{
	//	PNALDebugError( "interfaceSend failed!");
	//	return -ENOTCONN;
	//}
	CUR_ACT_CODE = L_ACT_ISO1443B_IVENTORY_AT;
	PNALDebugLog( "[A3_RFID_43B_Inventory]CUR_ACT_CODE: %02X \n", CUR_ACT_CODE);

	dev->cbRecv = ms_open_nfc_43b_cbrecv;

	PNALDebugLog( "A3_RFID_43B_Inventory() - end \n");

	return retval;
}

static int ms_43b_gen_ack_pdu(tNALMsrComInstance  *dev, unsigned char ack_type)
{
    //Format:
    //|Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x00)|Flag(0xB1)|cmd bit len (hi)|cmd bit len(lo)|rpt bit len(hi)|rpt bit len(lo)|Cmd|...
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x08;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = 0x91;  //0xd1
    dev->ant_send_buffer[5] = 0x00;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = 0x08; //cmd_bit_len_lo
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = 0xA2 | BLK_NUM | ack_type;
    return 10;
}

int ms_open_nfc_43b_4_xchg_data_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0;
    u16 cal_CRC, chk_CRC;
    u8 rcv_byte_len, pfb, cmd_len;

    PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_14_B_4;
    if (inbuf == NULL || len <= 0)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }
    //UniProtocol report format:
    //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
    if(inbuf[2] != RFID_STS_SUCCESS)
    {
        PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]CUR_DEP_CNT=%x", CUR_DEP_CNT);
		if (NACK_SEND_FLAG == W_FALSE)
        {
            NACK_SEND_FLAG = W_TRUE;
            PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]A3 Recv Err = %x\n", inbuf[2]);
            cmd_len = ms_43b_gen_ack_pdu(dev, R_BLK_NACK);
            ms_interfaceSend( dev, cmd_len);
    	    dev->cbRecv = ms_open_nfc_43b_4_xchg_data_callback;
        }
        else if (CUR_DEP_CNT >= MAX_DEP_TRY_CNT)
        {
            dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
            dev->nb_available_bytes = idx;
            PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]DEP Fail");
            //CNALWriteDataFinish(dev);
            CUR_DEP_CNT = 0;
        }
        else
        {
            PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]DEP RETRY\n");
            CUR_DEP_CNT++;
            ms_open_nfc_43b_cmd_xchg_data(dev, TEMP_OUT_BUF, TEMP_OUT_BUF_LEN);
        }
        return 0;
    }
    //CRC check
    rcv_byte_len = ((u16)(inbuf[3]<<8)|inbuf[4])/8;
    if (rcv_byte_len <= 2)
    {	
        cmd_len = ms_43b_gen_ack_pdu(dev, R_BLK_NACK);
        ms_interfaceSend( dev, cmd_len);
	    dev->cbRecv = ms_open_nfc_43b_4_xchg_data_callback;	    
        return 0;
    }
    cal_CRC = CRC16( CRC_ISO_14443A, (rcv_byte_len-2), inbuf+5 );
    chk_CRC = ((u16)(inbuf[5+(rcv_byte_len - 1)]<<8))|inbuf[5+(rcv_byte_len - 2)];
    if(cal_CRC != chk_CRC)
    {
        PNALDebugLog("CRC err, cal_CRC = %04X", cal_CRC);
        PNALDebugLog("CRC err, chk_CRC = %04X", chk_CRC);
        cmd_len = ms_43b_gen_ack_pdu(dev, R_BLK_NACK);
        ms_interfaceSend( dev, cmd_len);
	    dev->cbRecv = ms_open_nfc_43b_4_xchg_data_callback;
        //dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        //dev->nb_available_bytes = idx;
        return 0;
    }
    pfb = inbuf[5];
    BLK_NUM++;
    if (BLK_NUM >=2)
    {
        BLK_NUM = 0;
    }
    PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]pfb=%x", pfb);
    if ((inbuf[5] & 0x10) == 0x10) //chaining
    {
        PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]Get chaining pdu");
        memcpy(&DEP_BUF[DEP_LEN], inbuf+5+1, rcv_byte_len - 2 -1);
        DEP_LEN += rcv_byte_len - 2 -1;
        cmd_len = ms_43b_gen_ack_pdu(dev, R_BLK_ACK);
        ms_interfaceSend( dev, cmd_len);
	    dev->cbRecv = ms_open_nfc_43b_4_xchg_data_callback;
	    return 0;
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_43B_4_xchg_data_callback]Get DEP ok");
        memcpy(&DEP_BUF[DEP_LEN], inbuf+5+1, rcv_byte_len - 2 -1);
        DEP_LEN += rcv_byte_len - 2 -1;
        dev->rx_buffer[idx++]=NAL_RES_OK;
        //memcpy( &dev->rx_buffer[idx], inbuf+5+1, rcv_byte_len - 2 -1 );
        memcpy( &dev->rx_buffer[idx], DEP_BUF, DEP_LEN );
        //idx+=(rcv_byte_len - 2 - 1);
        idx+=DEP_LEN;
        dev->nb_available_bytes = idx;
        DEP_LEN = 0;
    }
    return 0;
}



// Todo-01: need code enhancement in the future for multipackage transmission
int ms_open_nfc_43b_cmd_xchg_data( tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;
	int interfaceSendRet = 0;
	unsigned char pcb;
	unsigned char nad;
	int datalen = 0;
	int dataidx;
    int cmd_bit_len;

	pcb = outbuf[2];
	dataidx = 3;
	datalen = len - 3;
#if 0	
	if( ( pcb && 0x0080))
	{
		nad = outbuf[3];
		dataidx = 4;
		datalen = len - 4;
	}
	dev->ant_send_buffer[0] = 0x42;
	dev->ant_send_buffer[1] = 0x0D;
	dev->ant_send_buffer[2] = 0x80;
	dev->ant_send_buffer[3] = 0x00;
	dev->ant_send_buffer[4] = 0x02;
	dev->ant_send_buffer[5] = 0x00;
	dev->ant_send_buffer[6] = ( 1 + datalen) * 8; //bit length, see Toda-01
	dev->ant_send_buffer[7] = 0x00; //rcv bit length, high
	dev->ant_send_buffer[8] = 0x00; //rcv bit length, low
	dev->ant_send_buffer[9] = pcb;
	memcpy( dev->ant_send_buffer + 10, outbuf + dataidx, datalen);
	
	//if( ms_interfaceSend( dev, 10 + datalen) != 0)
	interfaceSendRet = ms_interfaceSend( dev, 10 + datalen);
	retval = interfaceSendRet;
	//{
	//	PNALDebugError( "interfaceSend failed!");
	//	return -ENOTCONN;
	//}
	CUR_ACT_CODE = L_ACT_ISO1443B_XCHG_DATA;
	dev->cbRecv = ms_open_nfc_43b_cbrecv;	
#else
	dev->ant_send_buffer[0] = 0x42;
	dev->ant_send_buffer[1] = 8 + datalen;
	dev->ant_send_buffer[2] = 0x80;
	dev->ant_send_buffer[3] = 0x00;
	dev->ant_send_buffer[4] = 0x91;  //0xd1
	cmd_bit_len = ( 1 + datalen) * 8;
	dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0xFF;	//cmd_bit_len_hi
	dev->ant_send_buffer[6] = (cmd_bit_len & 0xFF);		 //cmd_bit_len_lo
	dev->ant_send_buffer[7] = 0x00; //rcv bit length, high
	dev->ant_send_buffer[8] = 0x00; //rcv bit length, low
	dev->ant_send_buffer[9] = (0x02 | BLK_NUM);
	memcpy( dev->ant_send_buffer + 10, outbuf + dataidx, datalen);

	retval = ms_interfaceSend( dev, 10 + datalen);
	dev->cbRecv = ms_open_nfc_43b_4_xchg_data_callback;
	NACK_SEND_FLAG = W_TRUE;

#endif


	return retval;
}


int A3_RFID_AT43B_Read_Block( tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	//dev->ant_send_buffer[0] = 0x42;
	//dev->ant_send_buffer[1] = 0x0D;
	//dev->ant_send_buffer[2] = 0x08;
	//dev->ant_send_buffer[3] = 0x00;
	//dev->ant_send_buffer[4] = 0x02;
	//dev->ant_send_buffer[5] = 0x00;
	//dev->ant_send_buffer[6] = 0x30;

	//dev->ant_send_buffer[7] = 0x02;
	//dev->ant_send_buffer[8] = 0x02;

	//dev->ant_send_buffer[9] = 0x02;
	//dev->ant_send_buffer[10] = 0x02;
	//dev->ant_send_buffer[11] = 0x02;
	//dev->ant_send_buffer[12] = 0x02;
	//dev->ant_send_buffer[13] = 0x02;
	//dev->ant_send_buffer[14] = 0x02;

	//if( ms_interfaceSend( dev->ant_send_buffer, 5) != 0)
	//{
	//	PNALDebugError( "interfaceSend failed!");
	//	return -ENOTCONN;
	//}
	//CUR_ACT_CODE = L_ACT_ISO1443B_IVENTORY_AT;
	//dev->cbRecv = ms_open_nfc_43b_cbrecv;


	return retval;
}

int A3_RFID_AT43B_Write_Block( tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}










