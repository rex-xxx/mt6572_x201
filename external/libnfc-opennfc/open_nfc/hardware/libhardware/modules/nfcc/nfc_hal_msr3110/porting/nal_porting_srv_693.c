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

#include "nfc_hal.h"
#include "interface.h"

#include "nal_porting_srv_693.h"

typedef unsigned char u8;
typedef unsigned short u16;

static u8 AFI = 0x00;
extern unsigned char Extern_RF_Detect;
extern int ms_card_detection(tNALMsrComInstance  *dev);
static int ms_A3_BuildHostCmd_SRF(u8 *pHostCmdBuf, pCmd_SRF cmd_buf);
static int ms_A3_Build593_Inventory(u8 *pHostCmdBuf, pCmd_Inventory cmd_buf);
static int ms_A3_RFID_693_Xchg_Data(tNALMsrComInstance  *dev, const uint8_t * pSendBuf, int SendLen );
//static int ms_open_nfc_693_recv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len);

int ms_open_nfc_693_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = len;

    if (outbuf == NULL || len == 0)
    {
        return -1;
    }
    PNALDebugLog("[ms_open_nfc_693_disp], cmd_code=%x\n", dev->curCmdCode);
    retval = ms_A3_RFID_693_Xchg_Data(dev, outbuf+3, len-3);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_693_recv;  
    return retval;
}

int ms_open_nfc_693_recv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0, dataLen = 0;
    PNALDebugLog("[ms_open_nfc_693_recv]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_15_3;
    PNALDebugLog("[ms_open_nfc_693_recv], recvlen=%d\n", len);
    PNALDebugLogBuffer(inbuf, len);
	if (inbuf == NULL || len <= 0)
	{
		dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
		dev->nb_available_bytes = idx;
		return idx;
	}

	if(*(inbuf+2) == RFID_STS_SUCCESS)
	{
		dev->rx_buffer[idx++]=NAL_RES_OK;
		dataLen = ((inbuf[3] <<8) & 0xFF00) | (inbuf[4] & 0x00FF);
		dataLen = (dataLen/8) - 2;
		memcpy( &dev->rx_buffer[idx], inbuf+5, dataLen );
		idx += dataLen;
	}
	else
	{
		dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
	}
	dev->nb_available_bytes = idx;

    return 0;
}

int ms_A3_RFID_693_Inventory_Callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0, dataLen = 0;
    PNALDebugLog("[ms_A3_RFID_693_Inventory_Callback]\n");
    dev->temp_rx_buffer[idx++] = NAL_SERVICE_READER_15_3;
    PNALDebugLog("[ms_A3_RFID_693_Inventory_Callback], recvlen=%d\n", len);
    PNALDebugLogBuffer(inbuf, len);

    if (inbuf == NULL || len <= 0 || inbuf[0]!=0x02)
    {
        //dev->rx_buffer[idx++] = NAL_EVT_NFCC_ERROR;
        ms_card_detection(dev);
        return 0;
    }
    if(inbuf[2] == 0x08)  //extern RF
    {
        PNALDebugLog("[ms_open_nfc_693_recv]Extern RF, Do Target:");
        Extern_RF_Detect = 0x01;
        ms_card_detection(dev);
        return 0;
    }
    //A3:Response
    //|xxx|Len|Status|Tag No|UID0|UID1|...|UID7|...
    //UID0 is 10 Bytes == Flags(1)+DSFID(1)+UID(8)

    if(inbuf[2] == RFID_STS_SUCCESS)
    {
        if(inbuf[3] == 1 && inbuf[1] == 0x0C)
        {
            dev->temp_rx_buffer[idx++]=NAL_EVT_READER_TARGET_DISCOVERED;
            memcpy( &dev->temp_rx_buffer[idx], inbuf+4, 10 );
            idx += 10;
            //dev->nb_available_bytes = idx;
            SetEventAvailableBytes(dev, idx);
            return 0;
        }
    }
    //#warning need to run detect procedure here.
    ms_card_detection(dev);
    return 0;
}


int ms_A3_RFID_Turn_Off_HF_Callback( tNALMsrComInstance  *dev, unsigned char *inbuf, int len )
{
    PNALDebugLog("[ms_A3_RFID_Turn_Off_HF_Callback]");

    if (len>0)
        return 0;
    else
        return -1;
}

int ms_A3_RFID_Turn_Off_HF( tNALMsrComInstance  *dev )
{
    cmd_SRF_s_type cmd;
    int length;

    PNALDebugLog("[A3_RFID_Turn_Off_HF]");

    /* Build SRF Command */
    cmd.op = TURN_OFF_RF;
    cmd.mode = 0;
    cmd.para = NULL;
    cmd.para_len = 0;
    length = ms_A3_BuildHostCmd_SRF(dev->ant_send_buffer, &cmd );

    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_A3_RFID_Turn_Off_HF_Callback;
    return length;
}

/* ISO15693 Inventory High Layer API */
int ms_A3_RFID_693_Inventory(tNALMsrComInstance  *dev)
{
    int length;
    cmd_inventory_s_type cmd;
    //struct spidev_data	*spidev = dev->spidev;

    PNALDebugLog("[A3_RFID_693_Inventory]\n");
    memset(&cmd,0,sizeof(cmd));

    cmd.Flag = ONE_SLOT_ONLY|USE_HIGH_DATA_RATE|DO_INVENTORY; //0x26;
    cmd.Mask_Len = 0;
    cmd.AFI= AFI;

    /* Build A3 ISO15693 Inventory Command */
    //A3 Inventory Commmand:
    //|CMD(0x45)|LEN|OPTION|AFI|
    length = ms_A3_Build593_Inventory(dev->ant_send_buffer, &cmd);

    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_A3_RFID_693_Inventory_Callback;
    return length;
}

static int ms_A3_Build593_Inventory(u8 *pHostCmdBuf, pCmd_Inventory cmd_buf)
{
    //A3: |CMD(0x45)|LEN|OPTION|AFI|
    //Cmd(1)    : 0x45
    //Len(1)    :
    //Option(2) : 0x0002
    //AFI(1)    :
    u16 Index  = 0;
    //u16 option = 0x0002;
    //u16 option = 0x0006;  //Mod 10% and 100%.
    //u16 option = 0x0246;  //Mod 10% and 100%. Tag limit 8 tags. bit9 is stay quiet assign.
    u16 option = 0x000C;  //Mod 100%. Tag limit 8 tags

    if(cmd_buf->Flag & AFI_FLAG )
    {
        option |= 0x0100;
    }


    pHostCmdBuf[Index++] = 0x45;    //ISO15693 INVENTORY.
    pHostCmdBuf[Index++] = 0x00;	//Len

    pHostCmdBuf[Index++] = (option & 0xFF00) >> 8;    //option_H
    pHostCmdBuf[Index++] = option & 0x00FF;           //option_L

    //Check enable AFI.
    if(cmd_buf->Flag & AFI_FLAG )
    {
        pHostCmdBuf[Index++] = cmd_buf->AFI;
    }

    pHostCmdBuf[1] = Index -2;  //Len assign
    return Index;
}

static int ms_A3_BuildHostCmd_SRF(u8 *pHostCmdBuf, pCmd_SRF cmd_buf)
{
    u16 Index  = 0;
    pHostCmdBuf[Index++] = 0x42;
    pHostCmdBuf[Index++] = 0x02;
    pHostCmdBuf[Index++] = 0x80;  //OPID1
    if(cmd_buf->op == TURN_OFF_RF)
    {
        pHostCmdBuf[Index++] = 0x04;  //OPID2
    }
    else if(cmd_buf->op == TURN_ON_RF)
    {
        pHostCmdBuf[Index++] = 0x05;  //OPID2
    }
    return Index;
}

static int ms_A3_RFID_693_Xchg_Data(tNALMsrComInstance  *dev, const uint8_t * pSendBuf, int SendLen )
{
    int retval = 0;
    u16 cmd_bit_len = 0;
		
    PNALDebugLog("[ms_A3_RFID_693_Send_Raw_Data]\n");
    /* Send command to Target */
    cmd_bit_len = SendLen * 8;
	
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 7 + SendLen;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = 0x40;
    dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0x00FF;   //cmd_bit_len_hi
    dev->ant_send_buffer[6] = (cmd_bit_len & 0x00FF);        //cmd_bit_len_lo
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
	memcpy( dev->ant_send_buffer + 9, pSendBuf, SendLen);
    retval = ms_interfaceSend(dev, dev->ant_send_buffer[1] + 2);

    return retval;
}

