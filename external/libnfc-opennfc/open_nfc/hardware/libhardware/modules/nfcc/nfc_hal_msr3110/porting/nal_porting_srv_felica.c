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
#include "nal_porting_srv_felica.h"
#include "nal_porting_srv_693.h"
#include "nal_porting_srv_util.h"

typedef unsigned short u16;
typedef unsigned char u8;

//static u8 SYSTEM_CODE[2] = {0x88, 0xB4};
static u8 SYSTEM_CODE[2] = {0xFF, 0xFF};
u8 id[8]= {0x01, 0x27, 0x00, 0x5C, 0xA4, 0xE1, 0x4A, 0xAE};
u8 pm[8]= {0x00, 0xF0, 0x00, 0x00, 0x02, 0x06, 0x03, 0x00};
u8 blk_data[16]= {0x18, 0x17, 0x16, 0x15, 0x14, 0x13, 0x12, 0x11, 0x18, 0x17, 0x16, 0x15, 0x14, 0x13, 0x12, 0x11};
extern unsigned char CUR_CARD_TYPE;
extern unsigned char Extern_RF_Detect;

extern int ms_card_detection(tNALMsrComInstance  *dev);
static int ms_open_nfc_felica_xchg_data( tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
int ms_open_nfc_felica_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = 0;

    if (outbuf == NULL || len == 0)
    {
        return -1;
    }
    PNALDebugLog("[ms_open_nfc_felica_disp], len = %d\n", len);
    PNALDebugLogBuffer(outbuf, len);
    switch( dev->curCmdCode)
    {
        case NAL_CMD_SET_PARAMETER:
            SYSTEM_CODE[0] = outbuf[3];
            SYSTEM_CODE[1] = outbuf[4];
            dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
            dev->rx_buffer[1] = NAL_RES_OK;
            dev->nb_available_bytes = 2;
            break;

        case NAL_CMD_GET_PARAMETER:
            if (outbuf[2] == NAL_PAR_READER_CONFIG)
            {
                dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
                dev->rx_buffer[1] = NAL_RES_OK;
                dev->rx_buffer[2] = SYSTEM_CODE[0];
                dev->rx_buffer[3] = SYSTEM_CODE[1];
                dev->nb_available_bytes = 4;
            }
            else
            {
                dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
                dev->rx_buffer[1] = NAL_RES_OK;
                dev->nb_available_bytes = 2;
            }
            break;

        case NAL_CMD_READER_XCHG_DATA:
            PNALDebugLog("[ms_open_nfc_felica_disp]NAL_CMD_READER_XCHG_DATA, op_code= %x\n", outbuf[4]);
            retval = ms_open_nfc_felica_xchg_data(dev, outbuf, len);
            /*
            if (outbuf[4] == 0x06)
            {
                dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
                dev->rx_buffer[1] = NAL_RES_OK;
                dev->rx_buffer[2] = 29;
                dev->rx_buffer[3] = 0x07;
                memcpy( &dev->rx_buffer[4], id, 8 );
                dev->rx_buffer[12] = 0x00;
                dev->rx_buffer[13] = 0x00;
                dev->rx_buffer[14] = outbuf[16];
                memcpy( &dev->rx_buffer[15], blk_data, 16 );
                dev->nb_available_bytes = 31;
            }
            else
            {
                dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
                dev->rx_buffer[1] = NAL_RES_OK;
                dev->rx_buffer[2] = 10;
                dev->rx_buffer[3] = 0x09;
                memcpy( &dev->rx_buffer[4], id, 8 );
                dev->nb_available_bytes = 12;
            }
            */
            break;

        default:
            dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
            dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
            dev->nb_available_bytes = 2;
            break;
    }

    return retval;
}

int ms_open_nfc_felica_Inventory_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_open_nfc_felica_Inventory_callback], len=%d\n", len);
    PNALDebugLogBuffer(inbuf, len);
#if 1
    //|0x02|LEN|TagNo|Tag1 response(20)|Tag2 response(20)|
    if (inbuf == NULL || len <= 0 || inbuf[0]!=0x02 || inbuf[2] == 0)
    {
        ms_card_detection(dev);
        return 0;
    }
    if(inbuf[2] == 0x08)  //extern RF
    {
        PNALDebugLog("[ms_open_nfc_felica_Inventory_callback]Extern RF, Do Target:");
        //CUR_CARD_TYPE = 0x05;
        Extern_RF_Detect = 0x01;
        ms_card_detection(dev);
        return 0;
    }
    if ((inbuf[5]== 0x01) && (inbuf[6]== 0xFE))
    {
        PNALDebugLog("[ms_open_nfc_felica_Inventory_callback]Not Felica, it read for NFC-DEP Protoco");
        ms_card_detection(dev);
        return 0;
    }

    if (inbuf[2] == 1 && inbuf[1]>20)
    {
        //Recv Buf Example:
        //buffer[27] = { 0x 02 17 01 14 01 01 27 00 65 5B 45 A6 AF 00 F0 00 00 02 06 03 00 88 B4 15 12 0F 05 }
        dev->temp_rx_buffer[0] = NAL_SERVICE_READER_FELICA;
        dev->temp_rx_buffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;
        memcpy( &dev->temp_rx_buffer[2], &inbuf[5], 8 );
        memcpy( &dev->temp_rx_buffer[10], &inbuf[13], 8 );
        dev->temp_rx_buffer[18] = inbuf[22];
        dev->temp_rx_buffer[19] = inbuf[21];
        //dev->nb_available_bytes = 20;
        SetEventAvailableBytes(dev, 20);
    }
    else
    {
        ms_card_detection(dev);
        return 0;
    }

#else
    dev->rx_buffer[0] = NAL_SERVICE_READER_FELICA;
    dev->rx_buffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;
    memcpy( &dev->rx_buffer[2], id, 8 );
    memcpy( &dev->rx_buffer[10], pm, 8 );
    dev->rx_buffer[18] = SYSTEM_CODE[0];
    dev->rx_buffer[19] = SYSTEM_CODE[1];
    dev->nb_available_bytes = 20;
#endif
    return 0;
}

int ms_open_nfc_felica_Inventory( tNALMsrComInstance  *dev )
{
    int length;
    PNALDebugLog("[ms_open_nfc_felica_Inventory]\n");
    PNALDebugLog("[ms_open_nfc_felica_Inventory]system_code=%x, %x\n",SYSTEM_CODE[0],SYSTEM_CODE[1]);
    /*
    |Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x06)|Flag1|Flag2|Timeout_3|Timeout_2|Timeout_1|Timeout_0|Cmd|
    Example:
    |0x42|0x0E|0x80|0x06|0x81|0x00|0x00|0x00|0x00|0x00|0x06|0x00|0xFF|0xFF|0x00|0x00|
    */
    length = 16;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x0E;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x06;
    dev->ant_send_buffer[4] = 0x81;
    dev->ant_send_buffer[5] = 0x00;
    dev->ant_send_buffer[6] = 0x00;
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = 0x00;
    dev->ant_send_buffer[10] = 0x06;
    dev->ant_send_buffer[11] = 0x00;
    dev->ant_send_buffer[12] = SYSTEM_CODE[0];
    dev->ant_send_buffer[13] = SYSTEM_CODE[1];
    dev->ant_send_buffer[14] = 0x01;
    dev->ant_send_buffer[15] = 0x00;
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_felica_Inventory_callback;
    return length;
}

static int ms_open_nfc_felica_xchg_data_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0;
    u8 payLoadLen =0, i;
    //u16 cal_CRC, chk_CRC;
    //Response format: |LEN|Status|Data…|    //LEN的長度為Status+Data長度但不含LEN自己。
    PNALDebugLog("[ms_open_nfc_felica_xchg_data_callback]. len=%d\n", len);
    PNALDebugLogBuffer(inbuf, len);
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_FELICA;
    if (inbuf == NULL || len <= 0 || inbuf[2]!= 0x01)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }
    //payLoadLen = inbuf[1] - 1;
    payLoadLen = inbuf[3];
    dev->rx_buffer[idx++] = NAL_RES_OK;
    for (i=0; i<payLoadLen; i++)
    {
        dev->rx_buffer[idx++] = inbuf[3 + i];
    }
    dev->nb_available_bytes = 2 + payLoadLen;
    return 0;
}

static int ms_open_nfc_felica_xchg_data( tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int length;
    /*
    |Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x06)|Flag1|Flag2|Timeout_3|Timeout_2|Timeout_1|Timeout_0|Cmd|
    */
    length = 10 + len - 3;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 8 + len - 3;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x06;
    dev->ant_send_buffer[4] = 0x85;
    dev->ant_send_buffer[5] = 0x00;
    dev->ant_send_buffer[6] = 0x00;
    dev->ant_send_buffer[7] = 0x67;
    dev->ant_send_buffer[8] = 0x74;
    dev->ant_send_buffer[9] = 0x60;
    memcpy(&dev->ant_send_buffer[10], &outbuf[3], len - 3);
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = ms_open_nfc_felica_xchg_data_callback;
    return length;
}
