#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>

#include <arpa/inet.h>
#include <semaphore.h>

#include "nfc_hal.h"
#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "linux_porting_hal.h"


#include "interface.h"
#include "nal_porting_srv_43a.h"
#include "nal_porting_srv_693.h"
#include "nal_porting_srv_43a_4.h"
#include "nal_porting_srv_util.h"
#include "nal_porting_srv_p2p.h"
#include "nal_porting_srv_p2p_i.h"



typedef unsigned short u16;
typedef unsigned char u8;

static u8 CUR_43a_CMD_ID = 0x00;
static u8 CUR_RF_PARAM = 0x00;
extern unsigned char ATR_CUR_CNT;
extern t43atype4Instance ISO43A_4_TAG;
extern unsigned char GIniTryCnt;
extern unsigned char Extern_RF_Detect;

extern DrvP2P_speed_et g_P2P_Speed; //for set P2P speed when initiator detected

extern int ms_card_detection(tNALMsrComInstance  *dev);
int ms_A3_RFID_43A_UL_Read_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
int ms_A3_RFID_43A_UL_Write_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
int ms_A3_RFID_43A_MF_Read_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
int ms_A3_RFID_43A_MF_Write_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );

int ms_open_nfc_43a_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = len;
    if (outbuf == NULL || len == 0)
    {
        return -1;
    }
    CUR_43a_CMD_ID = outbuf[3];
    switch(dev->curCmdCode)
    {
        case NAL_CMD_READER_XCHG_DATA:
            if (UL_READ_BLK==outbuf[3])
            {
                retval = ms_A3_RFID_43A_UL_Read_Block(dev, outbuf, len);
            }
            else if (UL_WRITE_BLK==outbuf[3])
            {
                retval = ms_A3_RFID_43A_UL_Write_Block(dev, outbuf, len);
            }
/*disable mifare function
            else if (MF_OP_READ==outbuf[3])
            {
                retval = ms_A3_RFID_43A_MF_Read_Block(dev, outbuf, len);
            }
            else if (MF_OP_WRITE==outbuf[3])
            {
                retval = ms_A3_RFID_43A_MF_Write_Block(dev, outbuf, len);
            }
*/
            else
            {
                dev->rx_buffer[0] = NAL_SERVICE_READER_14_A_3;
                dev->rx_buffer[1] = NAL_RES_FEATURE_NOT_SUPPORTED;
                dev->nb_available_bytes = 2;
                return 0;
            }
            break;

        default:
            dev->rx_buffer[0] = NAL_SERVICE_READER_14_A_3;
            dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
            dev->nb_available_bytes = 2;
            break;
    }
    return retval;
}

int ms_open_nfc_43a_recv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0;
    u16 cal_CRC, chk_CRC;
    u8 rcv_byte_len;

    
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_14_A_3;
    if (inbuf == NULL || len <= 0)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }
	//PNALDebugLog("[ms_open_nfc_43a_recv], 43a_CUR_CMD_ID= 0x%2x", CUR_43a_CMD_ID);
    switch(CUR_43a_CMD_ID)
    {
        case UL_READ_BLK :
            //UniProtocol report format:
            //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
            if(inbuf[2] != RFID_STS_SUCCESS)
            {
                dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
                dev->nb_available_bytes = idx;
                return 0;
            }
            //CRC check
            rcv_byte_len = ((u16)(inbuf[3]<<8)|inbuf[4])/8;
            if (rcv_byte_len < 2)
            {
                PNALDebugError("[ms_open_nfc_43a_recv]rcv_byte_len < 2");
                dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
                dev->nb_available_bytes = idx;
                return 0;
            }
            cal_CRC = CRC16( CRC_ISO_14443A, (rcv_byte_len-2), inbuf+5 );
            chk_CRC = ((u16)(inbuf[5+(rcv_byte_len - 1)]<<8))|inbuf[5+(rcv_byte_len - 2)];
            if(cal_CRC != chk_CRC)
            {
                PNALDebugLog("CRC err, cal_CRC = %04X", cal_CRC);
                PNALDebugLog("CRC err, chk_CRC = %04X", chk_CRC);
                dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
                dev->nb_available_bytes = idx;
                return 0;
            }
            dev->rx_buffer[idx++]=NAL_RES_OK;
            memcpy( &dev->rx_buffer[idx], inbuf+5, 16 );
            idx+=16;
            dev->nb_available_bytes = idx;
            break;
        case UL_WRITE_BLK:
        //UniProtocol report format:
        //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
            if(inbuf[2] != RFID_STS_SUCCESS)
            {
                dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
                dev->nb_available_bytes = idx;
                return 0;
            }

            PNALDebugLog("response[4] = %02X", inbuf[4]);
	      PNALDebugLog("response[5] = %02X", inbuf[5]);
            //AK check (AK is 4bits 0xA)
            if(( (inbuf[5]&0x0f) == 0x0A) && (inbuf[4] == 4))
            {
                dev->rx_buffer[idx++]=NAL_RES_OK;   //success
            }
            else
            {
                PNALDebugLog("[ULW]NAK");
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            dev->nb_available_bytes = idx;
            break;

        default:
            break;
    }
    return 0;
}

static int ms_chk_internal_error(unsigned char *inbuf, int len)
{
    if (len == 3 && inbuf[0]==0xAA && inbuf[2] == 0x00)
    {
        PNALDebugError("[ms_A3_RFID_43A_Inventory_Callback]has ms_chk_internal_error\n");
        return 1;
    }
    else
    {
        return 0;
    }
}

int ms_A3_RFID_43A_Inventory_Callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0;
    u8 TagNo = 0, uid_len = 0;
    PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback], len = %d\n", len);
    PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]inbuf:");
    PNALDebugLogBuffer(inbuf, len);

    if (inbuf == NULL || len <= 0)
    {
        //dev->rx_buffer[idx++] = NAL_EVT_NFCC_ERROR;
        ms_card_detection(dev);
        return 0;
    }
    if (inbuf[0] == 0xFF)
    {
        PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]change 0xFF -> 0x02\n");
        inbuf[0] = 0x02;
    }
    if (ms_chk_internal_error(inbuf, len) == 1)
    {
        ms_card_detection(dev);
        PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]SPI Error, Stop detect:");
        return 0;
    }
    if(inbuf[2] == 0x08)  //extern RF
    {
        PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]Extern RF, Do Target:");
        Extern_RF_Detect = 0x01;
        ms_card_detection(dev);
        return 0;
    }
    if(inbuf[2] == RFID_STS_SUCCESS)
    {
        TagNo = inbuf[3];
		PNALDebugLog( "[ms_A3_RFID_43A_Inventory_Callback]TagNo: %d, inbuf[24]: 0x%2x" , TagNo, inbuf[24]);
        if(TagNo > 1)
        {
            //Not need assign another data field.
            //dev->rx_buffer[idx++] = NAL_EVT_READER_TARGET_COLLISION;
            //dev->nb_available_bytes = idx;
            ms_card_detection(dev);
        }
        else if (TagNo == 1)
        {
            GIniTryCnt = 0xFF;
			PNALDebugLog( "[ms_A3_RFID_43A_Inventory_Callback]CUR_RF_PARAM: %02x, reader_detect_mask: %04x\n" , CUR_RF_PARAM, dev->reader_detect_mask);
            if (inbuf[24] & 0x40) // || ((inbuf[24] & 0xF0) == 0x60) || (inbuf[24] == 0x68))
            {
                PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]Get P2P target");
                //if ((CUR_RF_PARAM ==  RF_PARAM_PAS_I_106) &&
                //    ((dev->reader_detect_mask & NAL_PROTOCOL_READER_P2P_INITIATOR) == NAL_PROTOCOL_READER_P2P_INITIATOR))
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_P2P_INITIATOR) == NAL_PROTOCOL_READER_P2P_INITIATOR)                
                {
                    ATR_CUR_CNT = 0;
                    //Do ATR_REQ
                    ms_open_nfc_initiator_atr_req( dev );
                }
                else
                {
                    ms_card_detection(dev);
                }
                return 0;
            }
            //else if ((inbuf[13] == 0x44 && inbuf[12] == 0x03) || (inbuf[24] = 0x20))
            else if (inbuf[24] & 0x20)
            {
                PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]43a type4 tag");
                //if ((CUR_RF_PARAM ==  RF_PARAM_43A) &&
                //     ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_4_A) == NAL_PROTOCOL_READER_ISO_14443_4_A))
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_4_A) == NAL_PROTOCOL_READER_ISO_14443_4_A)                
                {
                    ISO43A_4_TAG.UID_Len = inbuf[4];
                    //ATQA
                    ISO43A_4_TAG.ATQA[0]=inbuf[13];
                    ISO43A_4_TAG.ATQA[1]=inbuf[12];
                    //SAK
                    ISO43A_4_TAG.SAK=inbuf[14 +10];
                    //UID
                    memcpy( ISO43A_4_TAG.UID, inbuf+14, ISO43A_4_TAG.UID_Len );
                    ms_open_nfc_43a_4_rats(dev);
                }
                else
                {
                    ms_card_detection(dev);
                }
                return 0;
                //dev->rx_buffer[0] = NAL_SERVICE_READER_14_A_4;
            }
            else //43a_3 tag
            {
                //if ((CUR_RF_PARAM ==  RF_PARAM_43A) &&
                //    ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_3_A) == NAL_PROTOCOL_READER_ISO_14443_3_A))
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_3_A) == NAL_PROTOCOL_READER_ISO_14443_3_A)                
                {
                    dev->temp_rx_buffer[idx++] = NAL_SERVICE_READER_14_A_3;
                    uid_len = inbuf[4];
                    dev->temp_rx_buffer[idx++]=NAL_EVT_READER_TARGET_DISCOVERED;
                    //ATQA
                    dev->temp_rx_buffer[idx++]=inbuf[13];
                    dev->temp_rx_buffer[idx++]=inbuf[12];
                    //SAK
                    dev->temp_rx_buffer[idx++]=inbuf[14 +10];
                    //UID
                    memcpy( &dev->temp_rx_buffer[idx], inbuf+14, uid_len );
                    idx+= uid_len;
                    //dev->nb_available_bytes = idx;
                    SetEventAvailableBytes(dev, idx);
                }
                else
                {
                    ms_card_detection(dev);
                }
                return 0;
            }
        }
        else
        {
           PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]Not Expect Result\n");
           ms_card_detection(dev);
        }
    }
    else
    {
        ms_card_detection(dev);
    }
    return 0;
}

/* ISO14443A Inventory High Layer API */
int ms_A3_RFID_43A_Inventory( tNALMsrComInstance  *dev, unsigned char rf_param )
{
    int length;

    PNALDebugLog("[A3_RFID_43A_Inventory]rf_param = %x\n", rf_param);
    CUR_RF_PARAM = rf_param;
    /* Build A3 ISO14443A Inventory Command */
    //A3 Inventory Commmand:
    //|CMD(0x46)|LEN|TagLimit| ===> |0x46|0x01|0x01|
    //length = A3_Build593_Inventory(dev->ant_send_buffer, &cmd);
    length = 6;
    dev->ant_send_buffer[0] = 0x46;
    dev->ant_send_buffer[1] = 0x04;    //LEN
    dev->ant_send_buffer[2] = (0x01 | rf_param);
    dev->ant_send_buffer[3] = 0x00;       //AC retry cont.

    //Timeout 0x0BB8(3000) around 3sec.
    //dev->ant_send_buffer[4] = 0x0B;    //Timeout(ms) high byte.
    //dev->ant_send_buffer[5] = 0xB8;    //Timeout(ms) high byte.
    dev->ant_send_buffer[4] = 0x27;    //Timeout(ms) high byte.
    dev->ant_send_buffer[5] = 0x10;    //Timeout(ms) high byte.

    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_A3_RFID_43A_Inventory_Callback;
    GIniTryCnt++;
    return length;
}

 //Read Blk Cmd
 //0x03 0x03 0x3B 0x30 0x02
int ms_A3_RFID_43A_UL_Read_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int retval = SendLen;
    u16 length;

    PNALDebugLog("[A3_RFID_43A_UL_Read_Block]\n");

    length = 11;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x09;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = 0x51;
    dev->ant_send_buffer[5] = 0x00;
    dev->ant_send_buffer[6] = 0x10;
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = 0x30;
    dev->ant_send_buffer[10] = pSendBuf[4];
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_43a_recv;
    return retval;
}

//Wriet Blk Cmd
//0x03 0x03 0x9B 0xA2 0x04 0x03 0x00 0xFE 0x00
int ms_A3_RFID_43A_UL_Write_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int retval = SendLen;
    int length;
    PNALDebugLog("[ms_A3_RFID_43A_UL_Write_Block]\n");
    length = 15;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x0D;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = 0x91;
    dev->ant_send_buffer[5] = 0x00;
    dev->ant_send_buffer[6] = 0x30;
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = 0xA2;
    dev->ant_send_buffer[10] = pSendBuf[4];
    dev->ant_send_buffer[11] = pSendBuf[5];
    dev->ant_send_buffer[12] = pSendBuf[6];
    dev->ant_send_buffer[13] = pSendBuf[7];
    dev->ant_send_buffer[14] = pSendBuf[8];

    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_43a_recv;
    return retval;
}

int ms_A3_RFID_43A_MF_Inventory_Callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0;
    int i;
    PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback]\n");
    PNALDebugLog("[ms_A3_RFID_43A_Inventory_Callback] - response_len = %d",len);
    dev->temp_rx_buffer[idx++] = NAL_SERVICE_READER_14_A_3;
    if (inbuf == NULL || len <= 0)
    {
        //dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        //dev->nb_available_bytes = idx;
        return 0;
    }
    for (i=0; i<len; i++)
    {
        PNALDebugLog("res = %x",inbuf[i]);
    }
    dev->temp_rx_buffer[idx++] = NAL_EVT_READER_TARGET_DISCOVERED;
    if (inbuf == NULL || len <= 0)
    {
        return 0;
    }
    switch (inbuf[2])
    {
        case 0 :
            if (len != 7)
            {
                //status = MF_ST_TAG_NOT_MF_CLASSIC;
                //Do Nothing
            }
            else
            {
                dev->temp_rx_buffer[idx++] = NAL_EVT_READER_TARGET_COLLISION;
                //Tag ID.
                for( i=0; i<4; i++)
                {
                    dev->temp_rx_buffer[idx++] = inbuf[3+i];
                }
                PNALDebugLog("[A3_MFSearchTag One tag found]\n");
            }
            //dev->nb_available_bytes = idx;
            SetEventAvailableBytes(dev, idx);
        case 1 :
            //status = MF_ST_NO_TAG_FOUND;
            break;
        case 2 :
            //status = MF_ST_MORE_THAN_ONE_TAG_FOUND;
            dev->temp_rx_buffer[idx++] = NAL_EVT_READER_TARGET_COLLISION;
            //dev->nb_available_bytes = idx;
            SetEventAvailableBytes(dev, idx);
            break;
        default :
            //status = MF_UNEXPECTED_STATUS;
            break;
    }
    //up(&dev->detect_sem); //wakeup detection thread
    //#warning call detection procedure here.
    ms_card_detection(dev);
    return 0;
}

int ms_A3_MFSearchTag( tNALMsrComInstance  *dev )
{
    int length;
    int cmd_len;
    PNALDebugLog("[A3_MFSearchTag]\n");

    //vm_rtkSleep(VM_RTK_MS_TO_TICK(500));

    dev->ant_send_buffer[0] = CmdDynamicMFReader;
    dev->ant_send_buffer[1] = 0x01;
    dev->ant_send_buffer[2] = MF_OP_SEARCH_TAG;

    cmd_len = 3;
    /* Send command to Target */
    length = ms_interfaceSend(dev, cmd_len);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, cmd_len) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_A3_RFID_43A_MF_Inventory_Callback;
    return length;
}

int ms_A3_RFID_43A_MF_Read_Block_Callback( tNALMsrComInstance  *dev, unsigned char *inbuf, int len )
{
    u16 idx  = 0;
    u8 cont;

    PNALDebugLog("[ms_A3_RFID_43A_MF_Read_Block_Callback]\n");
    PNALDebugLog("response_len = %02X\n", len);
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_14_A_3;
    if (inbuf == NULL || len <= 0)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }
    for(cont=0; cont<len; cont++)
    {
        PNALDebugLog("response = %02X\n", inbuf[cont]);
    }
    //UniProtocol report format:
    //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
    switch (inbuf[2])
    {
        case 0x00 :
            if (inbuf[3] == 0x00)
            {
                if (len == 20)
                {
                    dev->rx_buffer[idx++]=NAL_RES_OK;
                    //status = MF_RD_OK;
                    for(cont=0; cont<16; cont++)
                    {
                        dev->rx_buffer[idx++] = inbuf[4+cont];
                    }
                    PNALDebugLog("[A3_RFID_43A_MF_Read_Block OK]");
                }
                else
                {
                    //status = MF_RD_FAIL_WRONG_LENGTH;
                    dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
                }
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x01 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_RD_NO_TAG_TIMEOUT;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x02 :
            if (inbuf[3] == 0x0A)
            {
                //status = MF_RD_FAIL_GOT_ACK;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else if (inbuf[3] == 0x04)
            {
                //status = MF_RD_FAIL_NACK_NOT_ALLOW;
                dev->rx_buffer[idx++]=NAL_RES_PROTOCOL_ERROR;
            }
            else if (inbuf[3] == 0x05)
            {
                //status = MF_RD_FAIL_NACK_TRANSMISSION_ERROR;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x03 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_RD_FAIL_WRONG_LENGTH;
                dev->rx_buffer[idx++]=NAL_RES_BAD_LENGTH;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x04 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_RD_FAIL_WRONG_PARITY;
                dev->rx_buffer[idx++]=NAL_RES_BAD_DATA;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x05 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_RD_FAIL_WRONG_CRC;
                dev->rx_buffer[idx++]=NAL_RES_BAD_DATA;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        default :
            //status = MF_UNEXPECTED_STATUS;
            dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            break;
    }
    dev->nb_available_bytes = idx;
    return 0;
}

int ms_A3_RFID_43A_MF_Write_Block_Callback( tNALMsrComInstance  *dev, unsigned char *inbuf, int len )
{
    u16 idx  = 0;

    PNALDebugLog("[ms_A3_RFID_43A_MF_Write_Block_Callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_14_A_3;
    if (inbuf == NULL || len <= 0)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }
    //UniProtocol report format:
    //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
    switch (inbuf[2])
    {
        case 0x00 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_WR_OK;
                dev->rx_buffer[idx++]=NAL_RES_OK;
                PNALDebugLog("[A3_RFID_43A_MF_Write_Block OK]\n");
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x01 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_WR_NO_TAG_TIMEOUT_FOR_1ST_ACK;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x02 :
            if (inbuf[3] == 0x04)
            {
                //status = MF_WR_FAIL_1ST_NACK_NOT_ALLOW;
                dev->rx_buffer[idx++]=NAL_RES_PROTOCOL_ERROR;
            }
            else if (inbuf[3] == 0x05)
            {
                //status = MF_WR_FAIL_1ST_NACK_TRANSMISSION_ERROR;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x03 :
            if (inbuf[3] == 0x00)
            {
                //status = MF_WR_FAIL_TIMEOUT_FOR_2ND_ACK;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        case 0x04 :
            if (inbuf[3] == 0x04)
            {
                //status = MF_WR_FAIL_2ND_NACK_NOT_ALLOW;
                dev->rx_buffer[idx++]=NAL_RES_PROTOCOL_ERROR;
            }
            else if (inbuf[3] == 0x05)
            {
                //status = MF_WR_FAIL_2ND_NACK_TRANSMISSION_ERROR;
                dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
            }
            else
            {
                //status = MF_UNEXPECTED_STATUS;
                dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            }
            break;
        default :
            //status = MF_UNEXPECTED_STATUS;
            dev->rx_buffer[idx++]=NAL_RES_BAD_STATE;
            break;
    }
    dev->nb_available_bytes = idx;
    return 0;
}
int ms_A3_RFID_43A_MF_Read_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int retval = SendLen;
    int length;

    PNALDebugLog("[A3_RFID_43A_MF_Read_Block]\n");

    length = 4;
    dev->ant_send_buffer[0] = CmdDynamicMFReader;
    dev->ant_send_buffer[1] = 0x02;
    dev->ant_send_buffer[2] = MF_OP_READ;
    dev->ant_send_buffer[3] = pSendBuf[4];;
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_A3_RFID_43A_MF_Read_Block_Callback;
    return retval;
}

int ms_A3_RFID_43A_MF_Write_Block( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int retval = SendLen;
    int length, i;
    PNALDebugLog("[ms_A3_RFID_43A_MF_Write_Block]\n");
    length = 0x14;
    dev->ant_send_buffer[0] = CmdDynamicMFReader;
    dev->ant_send_buffer[1] = 0x12;
    dev->ant_send_buffer[2] = MF_OP_WRITE;
    dev->ant_send_buffer[3] = pSendBuf[4];
    for (i=0; i<16; i++)
    {
        dev->ant_send_buffer[4+i] = pSendBuf[5+i];
    }

    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_A3_RFID_43A_MF_Write_Block_Callback;
    return retval;
}

