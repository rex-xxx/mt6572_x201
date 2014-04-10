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
#include "nal_porting_srv_admin.h"

#include "nal_porting_srv_p2p_i.h"
#include "nal_porting_srv_p2p.h"
#include "nal_porting_srv_693.h"


#include "nal_porting_srv_util.h"

typedef unsigned short u16;
typedef unsigned char u8;

const unsigned char NFCID3i[] =
      //{ 0x26, 0xE4, 0x8E, 0x99, 0x25, 0xA2, 0x26, 0xB9, 0x85, 0xE9 };
	{0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05};
const unsigned char PAYLOAD_MAX_SIZE = 192;  //64
const unsigned char ATR_REQ_HEADER_SIZE = 0x10;
const unsigned char DEP_HEADER_SZIE = 0x03;
static unsigned char LINK_PARAM[255];
static unsigned char LINK_PARAM_LEN = 0;
static unsigned char DEP_BUF[1024];
static unsigned char DEP_RES_BUF[1024];
static int DEP_BUF_LEN;
static int DEP_SEND_TOTAL_LEN;
static int DEP_SEND_LEN;
static int DEP_RECV_TOTAL_LEN;
//static int DEP_RECV_LEN;
static unsigned char PNI;
const unsigned char ATR_MAX_TRY_CNT = 0x01;
const unsigned char DEP_MAX_TRY_CNT = 0x01;
const unsigned char ATN_MAX_TRY_CNT = 0x02;
unsigned char ATR_CUR_CNT=0x00;
unsigned char DEP_CUR_CNT=0x00;
u16 READER_MASK = 0x0000;
u16 CARD_MASK = 0x0000;
unsigned char GIniTryCnt = 0;
bool_t DEP_NACK_SEND_FLAG = W_FALSE;
unsigned char ATN_CUR_CNT = 0x00;
unsigned char DEP_NACK_CUR_CNT = 0x00;
u8 test_gbytes[13] = {0x46, 0x66, 0x6D, 0x01, 0x01, 0x10, 0x03, 0x02, 0x00, 0x01, 0x04, 0x01, 0x96};
const unsigned char DEP_CMD_FLAG = 0xDA; // 2sec  0xDA
const unsigned char ATR_CMD_FLAG = 0x9A; // 1sec
unsigned char DEP_MI_Flag = 0;
//global var
DrvP2P_speed_et g_P2P_Speed; //for set P2P speed when initiator detected
//bool_t SetParamFlag = W_FALSE;
bool_t SetParamFlag = W_TRUE;//Alan_Test

static unsigned char DEP_WT = 0x0B;  //0x0C

extern int ms_card_detection(tNALMsrComInstance  *dev);
static int ms_open_nfc_initiator_dep_req( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
static int ms_open_nfc_initiator_dep_req_212_424( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
static int ms_open_nfc_initiator_pl_config(tNALMsrComInstance  *dev);



//static int ms_open_nfc_initiator_dsl_req( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
//static int ms_open_nfc_initiator_rls_req( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen );
//int ms_open_nfc_initiator_atr_req( tNALMsrComInstance  *dev );

static int ms_open_nfc_initiator_time_out(tNALMsrComInstance  *dev)
{
    PNALDebugLog("[ms_open_nfc_initiator_time_out]\n");
    dev->rx_buffer[0] = NAL_SERVICE_P2P_INITIATOR;
    dev->rx_buffer[1] = NAL_RES_TIMEOUT;
    dev->nb_available_bytes = 2;
    return 2;
}
int ms_open_nfc_initiator_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = len;
    PNALDebugLog("[ms_open_nfc_initiator_disp]\n");
    if (outbuf == NULL || len == 0)
    {
        return -1;
    }
    PNALDebugLog("[ms_open_nfc_initiator_disp]dev->curCmdCode=%x\n", dev->curCmdCode);
    PNALDebugLog("[ms_open_nfc_initiator_disp]dev->len=%d\n", len);
    PNALDebugLog("[ms_open_nfc_initiator_disp]outbuf=\n");
    PNALDebugLogBuffer(outbuf, len);
    switch(dev->curCmdCode)
    {
        case NAL_CMD_SET_PARAMETER:
            if (NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS==outbuf[2])
            {
                LINK_PARAM_LEN = len - 3;
                memcpy(LINK_PARAM, &outbuf[3], LINK_PARAM_LEN);
                PNALDebugLog("[ms_open_nfc_initiator_disp]NAL_CMD_SET_PARAMETER");
                //PNALDebugLogBuffer(LINK_PARAM, LINK_PARAM_LEN);
#if (A3_PL_MODE == 0)              
                dev->rx_buffer[0] = NAL_SERVICE_P2P_INITIATOR;
                dev->rx_buffer[1] = NAL_RES_OK;
                dev->nb_available_bytes = 2;
#else 
                if (!SetParamFlag){
                    ms_open_nfc_initiator_pl_config(dev);
                }
				else
				{
                    PNALDebugLog("[ms_open_nfc_initiator_disp]SetParamFlag = true, only return");
					dev->rx_buffer[0] = NAL_SERVICE_P2P_INITIATOR;
                    dev->rx_buffer[1] = NAL_RES_OK;
                    dev->nb_available_bytes = 2;
				}
               
#endif

            }
            break;

        case NAL_EVT_P2P_SEND_DATA:
            ATN_CUR_CNT = 0x00;
            DEP_NACK_CUR_CNT = 0x00;
            DEP_NACK_SEND_FLAG = W_FALSE;
            //PNI = 0;
            DEP_SEND_TOTAL_LEN = 0;
            DEP_RECV_TOTAL_LEN = 0;
            DEP_BUF_LEN = len - 2;
            PNALDebugLog("[NAL_EVT_P2P_SEND_DATA]DEP pure data buf len=%d\n", DEP_BUF_LEN);
            memcpy(DEP_BUF, &outbuf[2], DEP_BUF_LEN);
            PNALDebugLog("[NAL_EVT_P2P_SEND_DATA]g_P2P_Speed=%d\n", g_P2P_Speed);
            if (g_P2P_Speed == DRVP2P_sp106)
            {
                retval = ms_open_nfc_initiator_dep_req(dev, DEP_BUF, DEP_BUF_LEN);
            }
            else
            {
                retval = ms_open_nfc_initiator_dep_req_212_424(dev, DEP_BUF, DEP_BUF_LEN);
            }
            break;

        default:
            dev->rx_buffer[0] = NAL_SERVICE_P2P_INITIATOR;
            dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
            dev->nb_available_bytes = 2;
            break;
    }
    return retval;
}

static int ms_chk_internal_error(unsigned char *inbuf, int len)
{
    if (len == 3 && inbuf[0]==0xAA && inbuf[2] == 0x00)
    {
        PNALDebugError("[ms_open_nfc_initiator_dep_req_callback]has ms_chk_internal_error\n");
        return 1;
    }
    else
    {
        return 0;
    }
}

int ms_open_nfc_initiator_atr_req_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0, i;

    u16 rcv_byte_len;
    unsigned short rcvCrc;
    unsigned short calCrc;
    int sodlen = 0, payloadIdx = 0;
    PNALDebugLog("[ms_open_nfc_initiator_atr_req_callback], len = %d\n", len);
    PNALDebugLogBuffer(inbuf, len);
    dev->rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;
    if (ms_chk_internal_error(inbuf, len) == 1)
    {
        ms_card_detection(dev);
        return 0;
    }
    if (inbuf == NULL || len <= 0 || inbuf[2] != RFID_STS_SUCCESS) //|| inbuf[0]!=0x02)
    {
RESEND:
        if (ATR_CUR_CNT >= ATR_MAX_TRY_CNT)
        {
            PNALDebugLog("ATR_REQ Fail, detect resume");
            ATR_CUR_CNT = 0;
            ms_card_detection(dev);
        }
        else
        {
            ms_open_nfc_initiator_atr_req(dev);
        }
        return 0;
    }
    if (inbuf[0] == 0xFF)
    {
        PNALDebugLog("[ms_open_nfc_initiator_atr_req_callback]change 0xFF -> 0x02\n");
        inbuf[0] = 0x02;
    }
    rcv_byte_len = ((u16)(inbuf[3]<<8)|inbuf[4])/8;
    PNALDebugLog("[ms_open_nfc_initiator_atr_req_callback]rcv_byte_len= %d", rcv_byte_len);
    if (rcv_byte_len > ATR_REQ_MAX_LEN || rcv_byte_len == 0)
    {
        //dev->rx_buffer[idx++] = NAL_EVT_NFCC_ERROR;
        PNALDebugLog("rcv_byte_len > ATR_REQ_MAX_LEN or rcv_byte_len = 0");
        goto RESEND;
    }
    if (rcv_byte_len > 0)
    {
        // check return CRC
        sodlen = inbuf[6];
        rcvCrc = (inbuf[ 5 + 1 + sodlen + 1] << 8) | inbuf[ 5 + 1 + sodlen ];
        calCrc = CRC16( CRC_ISO_18092_106,  sodlen + 1, inbuf + 5);
        /*
        rcvCrc = ( inbuf[ len - 1] << 8) | inbuf[ len - 2];
        calCrc = CRC16( CRC_ISO_18092_106,  inbuf[1] - 5, inbuf + 5);
        */
        PNALDebugLog("rcvCrc=%d, calCrc=%d", rcvCrc, calCrc);
        if( rcvCrc != calCrc)
        {
            //invalid CRC
            PNALDebugLog("CRC Check Error, atr again");
            ms_open_nfc_initiator_atr_req(dev);  //resent
            return 0;
        }
    }
    if (inbuf[7]!=0xD5 || inbuf[8]!=0x01)
    {
        PNALDebugLog("ATR_RES CMD_0/CMD_1 Error, CMD_0=%x, CMD_1=%x", inbuf[7], inbuf[8]);
        goto RESEND;
    }
    dev->temp_rx_buffer[idx++]=NAL_EVT_P2P_TARGET_DISCOVERED;

    dev->temp_rx_buffer[idx++]=0x00;
    dev->temp_rx_buffer[idx++]=0x01;
    dev->temp_rx_buffer[idx++]=0x9E;
    dev->temp_rx_buffer[idx++]=0x10;

    PNALDebugLog("[ms_open_nfc_initiator_atr_req_callback]NAL_EVT_P2P_TARGET_DISCOVERED...\n");
    PNALDebugLog("[ms_open_nfc_initiator_atr_req_callback]gerenal bytes:");
    PNALDebugLogBuffer(inbuf + 24, sodlen - 17 - 1); // 1: SodLen
    memcpy( &dev->temp_rx_buffer[idx], inbuf + 24, sodlen - 17 - 1 );  //only return gerenal bytes of ATR_RES
    idx+=(sodlen - 17 - 1);

    //dev->nb_available_bytes = idx;
    SetEventAvailableBytes(dev, idx);
    GIniTryCnt = 0xFF;
    ATR_CUR_CNT = 0;
    g_P2P_Speed = DRVP2P_sp106;
    return 0;
}

int ms_open_nfc_initiator_atr_req( tNALMsrComInstance  *dev )
{
    int length;
    u16 cmd_bit_len;
    ATR_CUR_CNT ++;
    PNALDebugLog("[ms_open_nfc_initiator_atr_req]CUR TRY CNT=%d\n", ATR_CUR_CNT);
    PNALDebugLog("[ms_open_nfc_initiator_atr_req]LINK_PARAM_LEN=%d\n", LINK_PARAM_LEN);
    //Format:
    //|Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x00)|Flag(0xB1)|cmd bit len (hi)|cmd bit len(lo)|rpt bit len(hi)|rpt bit len(lo)|Cmd|...
    if (LINK_PARAM_LEN > (PAYLOAD_MAX_SIZE - ATR_REQ_HEADER_SIZE))
    {
        LINK_PARAM_LEN = PAYLOAD_MAX_SIZE - ATR_REQ_HEADER_SIZE;
        PNALDebugLog("[ms_open_nfc_initiator_atr_req]LINK_PARAM_LEN Change=%d\n", LINK_PARAM_LEN);
    }

    length = 27 + LINK_PARAM_LEN;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 25 + LINK_PARAM_LEN;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = ATR_CMD_FLAG; // 1sec
    dev->ant_send_buffer[5] = 0x00; //cmd bit len (hi)
    dev->ant_send_buffer[6] = 0x00; //cmd bit len(lo)
    dev->ant_send_buffer[7] = 0x00; //rpt bit len(hi)
    dev->ant_send_buffer[8] = 0x00; //rpt bit len(lo)
    //dev->ant_send_buffer[7] = 0x01; //rpt bit len(hi)
    //dev->ant_send_buffer[8] = 0x9F; //rpt bit len(lo)
    dev->ant_send_buffer[9] = SB;
    //dev->ant_send_buffer[10] = 1 + 16 + LINK_PARAM_LEN + SoD_LEN;
    dev->ant_send_buffer[10] = 1 + 16 + LINK_PARAM_LEN;
    dev->ant_send_buffer[11] = CMD_0_REQ;
    dev->ant_send_buffer[12] = CMD_1_ATR_REQ;
    memcpy(&dev->ant_send_buffer[13], NFCID3i, 10);
    dev->ant_send_buffer[23] = DIDi; //DIDi
    dev->ant_send_buffer[24] = BSi;
    dev->ant_send_buffer[25] = BRi;
    if (LINK_PARAM_LEN > 0)
    {
        dev->ant_send_buffer[26] = PPi | ATR_GB_ENABLE ;
        memcpy(&dev->ant_send_buffer[27], LINK_PARAM, LINK_PARAM_LEN);
    }
    else
    {
        dev->ant_send_buffer[26] = PPi;
    }
    cmd_bit_len = 144 + (LINK_PARAM_LEN * 8); //144: 18 * 8 (SB, len, cmd_0, cmd_1, nfcid3i(10), did, bs, br, pp)
    dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0xFF;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = (cmd_bit_len & 0xFF);        //cmd_bit_len_lo
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_atr_req_callback;
    return length;
}
#if 0
int ms_open_nfc_initiator_detection_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0, i;

    u8 TagNo = 0; //, uid_len = 0;
    PNALDebugLog("[ms_open_nfc_initiator_detection_callback]\n");
    PNALDebugLog("[ms_open_nfc_initiator_detection_callback], len = %d, inbuf:\n", len);
    PNALDebugLogBuffer(inbuf, len);

    dev->rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;

    if (inbuf == NULL || len <= 0) // || inbuf[0]!=0x02)
    {
        ms_card_detection(dev);
        return 0;
    }
    if (inbuf[0] == 0xFF)
    {
        PNALDebugLog("[ms_open_nfc_initiator_detection_callback]change 0xFF -> 0x02\n");
        inbuf[0] = 0x02;
    }
    if(inbuf[2] == RFID_STS_SUCCESS)
    {
        TagNo = inbuf[3];

        if(TagNo > 1)
        {
            //Not need assign another data field.
            dev->rx_buffer[idx++] = NAL_EVT_READER_TARGET_COLLISION;
            //dev->nb_available_bytes = idx;
            SetEventAvailableBytes(dev, idx);
            ms_card_detection(dev);
            return 0;
        }
        else if (TagNo == 1)
        {
            if (inbuf[24] != 0x40)
            {
                PNALDebugLog("[ms_open_nfc_initiator_detection_callback]not target, is 43a tag");
                ms_card_detection(dev);
                return 0;
            }
            PNALDebugLog("[ms_open_nfc_initiator_detection_callback]detect ok");
            ATR_CUR_CNT = 0;
            //dev->reader_detect_mask = 0x0000;
            //dev->card_detect_mask = 0x0000;
            //Do ATR_REQ
            ms_open_nfc_initiator_atr_req( dev );
        }
        else
        {
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
int ms_open_nfc_initiator_detection( tNALMsrComInstance  *dev )
{
    int length;

    PNALDebugLog("[ms_open_nfc_initiator_detection]\n");
    //READER_MASK = dev->reader_detect_mask;
    //CARD_MASK = dev->card_detect_mask;

    /* Build A3 ISO14443A Inventory Command */
    //A3 Inventory Commmand:
    //|CMD(0x46)|LEN|TagLimit| ===> |0x46|0x01|0x01|
    //length = A3_Build593_Inventory(dev->ant_send_buffer, &cmd);
    length = 6;
    dev->ant_send_buffer[0] = 0x46;
    dev->ant_send_buffer[1] = 0x04;    //LEN
    dev->ant_send_buffer[2] = 0x01;    //AntiCollision flag disable.
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
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_detection_callback;
    GIniTryCnt++;
    return length;
}
#endif
static int ms_gen_ack_pdu(tNALMsrComInstance  *dev, unsigned char pni, unsigned char ack_type)
{
    u16 cmd_bit_len;
    //Format:
    //|Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x00)|Flag(0xB1)|cmd bit len (hi)|cmd bit len(lo)|rpt bit len(hi)|rpt bit len(lo)|Cmd|...
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 12;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = DEP_CMD_FLAG;
    dev->ant_send_buffer[5] = 0x00;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = 0x00; //cmd_bit_len_lo
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = SB;
    dev->ant_send_buffer[10] = DEP_HEADER_SZIE + 1;
    dev->ant_send_buffer[11] = CMD_0_REQ;
    dev->ant_send_buffer[12] = CMD_1_DEP_REQ;
    dev->ant_send_buffer[13] = PDU_ACK | ack_type | pni;
    //dev->ant_send_buffer[14] = DIDi;
    cmd_bit_len = (SoD_LEN + DEP_HEADER_SZIE) * 8;
    dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0x00FF;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = (cmd_bit_len & 0x00FF);        //cmd_bit_len_lo
    return 14;
}

static int ms_gen_sup_pdu(tNALMsrComInstance  *dev, unsigned char pfb, unsigned char to)
{
    u16 cmd_bit_len;
    //Format:
    //|Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x00)|Flag(0xB1)|cmd bit len (hi)|cmd bit len(lo)|rpt bit len(hi)|rpt bit len(lo)|Cmd|...
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 12;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = DEP_CMD_FLAG;
    dev->ant_send_buffer[5] = 0x00;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = 0x00; //cmd_bit_len_lo
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = SB;
    dev->ant_send_buffer[10] = 1;
    dev->ant_send_buffer[11] = CMD_0_REQ;
    dev->ant_send_buffer[12] = CMD_1_DEP_REQ;
    dev->ant_send_buffer[13] = pfb;
    //dev->ant_send_buffer[14] = DIDi;
    if ((pfb & SUP_TYPE_TO_MASK) == SUP_TYPE_TO)
    {
        dev->ant_send_buffer[14] = to;
        dev->ant_send_buffer[1] = 13;
        dev->ant_send_buffer[10] = 5;
        cmd_bit_len = (SoD_LEN + DEP_HEADER_SZIE) * 8 + 8;
        dev->ant_send_buffer[5] = ((cmd_bit_len >> 8) & 0x00FF);  //cmd_bit_len_hi
        dev->ant_send_buffer[6] = (cmd_bit_len & 0x00FF);        //cmd_bit_len_lo
        return 15;
    }
    else
    {
        dev->ant_send_buffer[10] = 4;
        cmd_bit_len = (SoD_LEN+ DEP_HEADER_SZIE) * 8;
        dev->ant_send_buffer[5] = ((cmd_bit_len >> 8) & 0x00FF);  //cmd_bit_len_hi
        dev->ant_send_buffer[6] = (cmd_bit_len & 0x00FF);        //cmd_bit_len_lo
        return 14;
    }
}
int ms_open_nfc_initiator_dep_req_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0, cmd_len = 0, i = 0;
    unsigned short rcvCrc;
    unsigned short calCrc;
    unsigned char pfb = 0x00, rptlen, pni, didt, to;
    u16 rcv_byte_len, cmd_bit_len;
    int sodlen = 0, payloadIdx = 0;

    PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;
    if (ms_chk_internal_error(inbuf, len) == 1)
    {
        //ms_card_detection(dev);
        CNALWriteDataFinish(dev);
        //ms_open_nfc_initiator_time_out(dev);
        return 0;
    }
    if (inbuf == NULL || len <= 0) // || inbuf[0]!=0x02)
    {
        //ms_card_detection(dev);
        CNALWriteDataFinish(dev);
        //ms_open_nfc_initiator_time_out(dev);
        return 0;
    }
    if (inbuf[0] == 0xFF)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]change 0xFF -> 0x02\n");
        inbuf[0] = 0x02;
    }
    //UniProtocol report format:
    //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback], len = %d\n", len);
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]inbuf:");
    PNALDebugLogBuffer(inbuf, len);

    PNALDebugLog("Status= %x\n", inbuf[2]);
    if(inbuf[2] != RFID_STS_SUCCESS)
    {
RESEND:
        if (ATN_CUR_CNT >= ATN_MAX_TRY_CNT || DEP_NACK_CUR_CNT >= DEP_MAX_TRY_CNT)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP Fail, resume");
            //ms_card_detection(dev);
            CNALWriteDataFinish(dev);
            //ms_open_nfc_initiator_time_out(dev);
        }
        else if (DEP_NACK_SEND_FLAG == W_FALSE)
        {
            //Send ATN pdu
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]Send ATN PDU, ATN_CUR_CNT:%d", ATN_CUR_CNT);
            pfb = PDU_SUP | SUP_TYPE_ATN ;
            cmd_len = ms_gen_sup_pdu(dev, pfb, 0);
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_callback;
            ATN_CUR_CNT++;
        }
        else
        {
            //Send Nack
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]Send NACK PDU, DEP_NACK_CUR_CNT:%d", DEP_NACK_CUR_CNT);
            cmd_len = ms_gen_ack_pdu(dev, PNI, DEP_RES_NACK);
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_callback;
            DEP_NACK_CUR_CNT++;
        }
        return 0;
    }
    //check recv len
    DEP_NACK_SEND_FLAG = W_TRUE;
    rcv_byte_len = ((u16)(inbuf[3]<<8)|inbuf[4])/8;
    if (rcv_byte_len <= 0)
    {
        PNALDebugError("[ms_open_nfc_initiator_dep_req_callback]rcv_byte_len<=0");
        goto RESEND;
        return 0;
    }
    //check payload crc
    //buffer[18] = { 0x 02 0E 01 00 51 F0 07 D5 07 00 92 00 00 E0 8C 39 6C 04
    //buffer[15] = { 0x 02 0D 01 00 50 F0 06 D5 07 04 00 8D 90 25 F5
    sodlen = inbuf[6];
    rcvCrc = ( inbuf[ 5 + 1 + sodlen + 1] << 8) | inbuf[ 5 + 1 + sodlen ];
    calCrc = CRC16( CRC_ISO_18092_106,  sodlen + 1, inbuf + 5);
    //rcvCrc = ( inbuf[ len - 1] << 8) | inbuf[ len - 2];
    //calCrc = CRC16( CRC_ISO_18092_106,  inbuf[1] - 5, inbuf + 5);
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]rcvCrc=%d, calCrc=%d", rcvCrc, calCrc);
    pni = pfb & 0x03;
    if( (rcvCrc != calCrc))// || (PNI != pni))
    {
        //invalid CRC
        // return an NACK
        PNALDebugError("[ms_open_nfc_initiator_dep_req_callback]CRC Check Error");
        goto RESEND;
        return 0;
    }
    rptlen =  inbuf[1];
    pfb = inbuf[9];
    //didt = inbuf[10];
    if (inbuf[7]!=0xD5 || inbuf[8]!=0x07)
    {
        PNALDebugError("[ms_open_nfc_initiator_dep_req_callback]DEP_RES CMD_0/CMD_1 Error");
        PNALDebugError("[ms_open_nfc_initiator_dep_req_callback]DEP_RES CMD_0=%x, CMD_1=%x", inbuf[7], inbuf[8]);
        goto RESEND;
        return 0;
    }
    payloadIdx = 3;
    //if (((pfb & DEP_DID_ENABLE) != DEP_DID_ENABLE) || (didt != DIDi))
    if ((pfb & DEP_DID_ENABLE) == DEP_DID_ENABLE)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP_DID_ENABLE");
        didt = inbuf[10];
        payloadIdx = 4;
        //protocol error
        if (didt != DIDi)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]Protocol Error");
            goto RESEND;
            return 0;
        }
    }
    if ((pfb & PDU_MASK) == PDU_ACK)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP TYPE: ACK\n");
        if ((pfb & DEP_RES_NACK) == DEP_RES_NACK) //Resend
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP_RES: Get NACK\n");
            ms_open_nfc_initiator_dep_req(dev, DEP_BUF, DEP_BUF_LEN);
            return 0;
        }
        DEP_SEND_TOTAL_LEN += DEP_SEND_LEN;
        if (DEP_SEND_TOTAL_LEN >= DEP_BUF_LEN)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP_SEND_TOTAL_LEN >= DEP_BUF_LEN, DoNothing\n");
            //ms_card_detection(dev);
            CNALWriteDataFinish(dev);
            //ms_open_nfc_initiator_time_out(dev);
            return 0;
        }
        else
        {
            if (++PNI == 4)
            {
                PNI = 0;
            }
            ms_open_nfc_initiator_dep_req(dev, DEP_BUF, DEP_BUF_LEN);
        }
    }
    else if ((pfb & PDU_MASK) == PDU_INF)
    {
        if (DEP_MI_Flag == 1)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]Recv unknow data");
            CNALWriteDataFinish(dev);
            return 0;
        }
        //DEP_RECV_LEN = rptlen - 3 - 4 - 2;
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP TYPE: INFO\n");
        memcpy(&DEP_RES_BUF[DEP_RECV_TOTAL_LEN], &inbuf[5 + SoD_LEN + payloadIdx], sodlen - 1 - payloadIdx);
        DEP_RECV_TOTAL_LEN += (sodlen - 1 - payloadIdx);
        if ((pfb & DEP_MI_MASK) == DEP_MI_DISABLE)
        {
            //memcpy(&DEP_RES_BUF[DEP_RECV_TOTAL_LEN], &inbuf[11], rcv_byte_len - DEP_HEADER_SZIE - 2 - SoD_LEN);//6=4(dep_header)+2(crc)
            //DEP_RECV_TOTAL_LEN += (rcv_byte_len - DEP_HEADER_SZIE - 2 - SoD_LEN);
            dev->temp_rx_buffer[idx++] = NAL_EVT_P2P_SEND_DATA;
            for (i=0; i<DEP_RECV_TOTAL_LEN; i++)
            {
                dev->temp_rx_buffer[idx++] = DEP_RES_BUF[i];
            }
            //dev->nb_available_bytes = idx;
            SetEventAvailableBytes(dev, idx);
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]Get DEP_RES Success");
        }
        else
        {
            //memcpy(&DEP_RES_BUF[DEP_RECV_TOTAL_LEN], &inbuf[11], rcv_byte_len - DEP_HEADER_SZIE - 2 - SoD_LEN); //CRC: 2
            //DEP_RECV_TOTAL_LEN += (rcv_byte_len - DEP_HEADER_SZIE - 2 - SoD_LEN);
            pni = pfb & 0x03;
            cmd_len = ms_gen_ack_pdu(dev, pni, DEP_RES_ACK);
            /* Send command to Target */
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_callback;
        }
    }
    else if ((pfb & PDU_MASK) == PDU_SUP)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP TYPE: SUP\n");
        if ((pfb & SUP_TYPE_TO) == SUP_TYPE_TO)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP_RES: Get TO pdu\n");
            //to = inbuf[11];
            to = inbuf[5 + SoD_LEN + payloadIdx];
            cmd_len = ms_gen_sup_pdu(dev, pfb, to);
            /* Send command to Target */
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_callback;
        }
        else //recv ATN, need to resend
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP_RES: Get ATN pdu\n");
            ms_open_nfc_initiator_dep_req(dev, DEP_BUF, DEP_BUF_LEN);
            return 0;
        }
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_callback]DEP TYPE: ERROR\n");
        goto RESEND;
    }
    return 0;
}

static int ms_open_nfc_initiator_dep_req( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int retval = SendLen, i;
    u16 length, datalen = 0, cmd_bit_len, dep_max_size;
    PNALDebugLog("[ms_open_nfc_initiator_dep_req]\n");
    //Format:
    //|Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x00)|Flag(0xB1)|cmd bit len (hi)|cmd bit len(lo)|rpt bit len(hi)|rpt bit len(lo)|Cmd|...
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x00;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = DEP_CMD_FLAG;
    dev->ant_send_buffer[5] = 0x00;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = 0x00; //cmd_bit_len_lo
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = SB;
    dev->ant_send_buffer[10] = 1;  //Initial
    dev->ant_send_buffer[11] = CMD_0_REQ;
    dev->ant_send_buffer[12] = CMD_1_DEP_REQ;
    dep_max_size = PAYLOAD_MAX_SIZE - DEP_HEADER_SZIE;
    PNALDebugLog("[ms_open_nfc_initiator_dep_req]DEP_SEND_TOTAL_LEN = %d\n", DEP_SEND_TOTAL_LEN);
#if 0
    if ((DEP_BUF_LEN - DEP_SEND_TOTAL_LEN) <= dep_max_size)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req]MI_DISABLE\n");
        dev->ant_send_buffer[13] = PDU_INF | DEP_MI_DISABLE | DEP_DID_ENABLE | PNI;
        dev->ant_send_buffer[14] = 0x00;
        PNALDebugLog("[ms_open_nfc_initiator_dep_req]DEP_BUF_DATA: \n");
        PNALDebugLogBuffer(&pSendBuf[DEP_SEND_TOTAL_LEN], DEP_BUF_LEN - DEP_SEND_TOTAL_LEN);
        for (i=0; i<DEP_BUF_LEN - DEP_SEND_TOTAL_LEN; i++)
        {
            dev->ant_send_buffer[i+15] = pSendBuf[DEP_SEND_TOTAL_LEN + i];
            //PNALDebugLog("DEP_DATA: 0x%x \n", pSendBuf[DEP_SEND_TOTAL_LEN + i]);
        }
        //memcpy(&dev->ant_send_buffer[13], pSendBuf+6+DEP_SEND_TOTAL_LEN, DEP_BUF_LEN - DEP_SEND_TOTAL_LEN);
        datalen = DEP_BUF_LEN - DEP_SEND_TOTAL_LEN;
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req]MI_ENABLE\n");
        dev->ant_send_buffer[13] = PDU_INF | DEP_MI_ENABLE| DEP_DID_ENABLE | PNI;
        dev->ant_send_buffer[14] = 0x00;
        //memcpy(&dev->ant_send_buffer[13], pSendBuf+6+DEP_SEND_TOTAL_LEN, dep_max_size);
        memcpy(&dev->ant_send_buffer[15], pSendBuf+DEP_SEND_TOTAL_LEN, dep_max_size);
        datalen = dep_max_size;
    }
    PNALDebugLog("[ms_open_nfc_initiator_dep_req]DataLen= %d\n", datalen);
    cmd_bit_len = 48 + (datalen * 8); //48: 6*8(SB, len, cmd0, cmd1, PFB, DID)
    dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0x00FF;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = (cmd_bit_len & 0x00FF);        //cmd_bit_len_lo
    DEP_SEND_LEN = datalen;
    dev->ant_send_buffer[1] = 7 + SoD_LEN + DEP_HEADER_SZIE + datalen;
    dev->ant_send_buffer[10] = 1 + DEP_HEADER_SZIE + datalen;
    length = 2 + dev->ant_send_buffer[1]; // 2: uniprotocol: cmd(1) + len(1)
#else
    //SendLen = 2;
    //pSendBuf[0] = 0x00;
    //pSendBuf[1] = 0x00;
    //pSendBuf[2] = 0x55;
    //pSendBuf[3] = 0xAA;
    //pSendBuf[4] = 0x55;
    //pSendBuf[5] = 0xAA;
    //DEP_BUF_LEN = 2;
    if ((DEP_BUF_LEN - DEP_SEND_TOTAL_LEN) <= dep_max_size)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req]MI_DISABLE\n");
        dev->ant_send_buffer[13] = PDU_INF | DEP_MI_DISABLE | PNI;
        //dev->ant_send_buffer[14] = 0x00;
        PNALDebugLog("[ms_open_nfc_initiator_dep_req]DEP_BUF_DATA: \n");
        PNALDebugLogBuffer(&pSendBuf[DEP_SEND_TOTAL_LEN], DEP_BUF_LEN - DEP_SEND_TOTAL_LEN);
        for (i=0; i<DEP_BUF_LEN - DEP_SEND_TOTAL_LEN; i++)
        {
            dev->ant_send_buffer[i+14] = pSendBuf[DEP_SEND_TOTAL_LEN + i];
            //PNALDebugLog("DEP_DATA: 0x%x \n", pSendBuf[DEP_SEND_TOTAL_LEN + i]);
        }
        //memcpy(&dev->ant_send_buffer[13], pSendBuf+6+DEP_SEND_TOTAL_LEN, DEP_BUF_LEN - DEP_SEND_TOTAL_LEN);
        datalen = DEP_BUF_LEN - DEP_SEND_TOTAL_LEN;
        DEP_MI_Flag = 0;
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req]MI_ENABLE\n");
        dev->ant_send_buffer[13] = PDU_INF | DEP_MI_ENABLE | PNI;
        //dev->ant_send_buffer[14] = 0x00;
        //memcpy(&dev->ant_send_buffer[13], pSendBuf+6+DEP_SEND_TOTAL_LEN, dep_max_size);
        memcpy(&dev->ant_send_buffer[14], pSendBuf+DEP_SEND_TOTAL_LEN, dep_max_size);
        datalen = dep_max_size;
        DEP_MI_Flag = 1;
    }
    PNALDebugLog("[ms_open_nfc_initiator_dep_req]DataLen= %d\n", datalen);
    cmd_bit_len = 40 + (datalen * 8); //40: 5*8(SB, len, cmd0, cmd1, PFB)
    dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0x00FF;  //cmd_bit_len_hi
    dev->ant_send_buffer[6] = (cmd_bit_len & 0x00FF);        //cmd_bit_len_lo
    DEP_SEND_LEN = datalen;
    dev->ant_send_buffer[1] = 7 + SoD_LEN + DEP_HEADER_SZIE + datalen;
    dev->ant_send_buffer[10] = 1 + DEP_HEADER_SZIE + datalen;
    length = 2 + dev->ant_send_buffer[1]; // 2: uniprotocol: cmd(1) + len(1)
#endif
    PNALDebugLog("[ms_open_nfc_initiator_dep_req]send len= %d\n", length);
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    /*
    if( ms_interfaceSend(dev->ant_send_buffer, length) != 0 ){
        PNALDebugError( "interfaceSend failed.");
        return -ENOTCONN;
    }
    */
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_callback;
    //DEP_CUR_CNT++;
    return retval;
}


//For 212/424 SDD & DEP method
int ms_open_nfc_initiator_detection_212_424_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0, i;
    u16 rcv_byte_len;
    unsigned short rcvCrc;
    unsigned short calCrc;
    unsigned short sddLen = 0, atrResLen = 0, cmdLen = 0;
    int sodlen = 0, payloadIdx = 0;
	unsigned char fuseIdLen = 0;
	unsigned char fuseIdBuf[8];
	
    PNALDebugLog("[ms_open_nfc_initiator_detection_212_424_callback], len = %d\n", len);
    PNALDebugLogBuffer(inbuf, len);
    dev->temp_rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;
    if (ms_chk_internal_error(inbuf, len) == 1)
    {
        ms_card_detection(dev);
        return 0;
    }
    //P2P Rsp Format:
    //Fail:
    //  A3Rsp(1)[0x02], Len(1)[n],SubFuncCode(1),RF_resultCode(1)
    //Success:
    //  A3Rsp(1)[0x02], Len(1)[n],SubFuncCode(1),DrvP2P_EnableResult_et(1)
    //  RF_resultCode(1),RF_signal_Len((1)[n],RF_signal_Buf(Polling_Res)(n),
    //  RF_resultCode(1),RF_signal_Len((1)[n],RF_signal_Buf(ATR_Res)(n)
    if (inbuf == NULL || len <= 0 || (inbuf[2] == 0xFF)
        || (inbuf[3] != DRVP2P_result_As_Initiator)) //|| inbuf[0]!=0x02)
    {
        ms_card_detection(dev);
        return 0;
    }
    sddLen = inbuf[5];
    atrResLen = inbuf[5 + sddLen + 2];
        // check return CRC

        cmdLen = inbuf[5 + sddLen + 3];
        rcvCrc = ( inbuf[5 + sddLen + 3 + cmdLen + 1]) | (inbuf[ 5 + sddLen + 3 + cmdLen ]<<8);
        calCrc = CRC16( CRC_ISO_18092_248,  cmdLen, inbuf + 5 + sddLen + 3);
        PNALDebugLog("rcvCrc=%d, calCrc=%d", rcvCrc, calCrc);
        if( rcvCrc != calCrc)
        {
            //invalid CRC
            PNALDebugLog("CRC Check Error, atr again");
            ms_open_nfc_initiator_detection_212_424(dev);  //resent
            return 0;
        }

    if (inbuf[5 + sddLen + 2 + 2]!=0xD5 || inbuf[5 + sddLen + 2 + 3]!=0x01)
    {
        PNALDebugLog("ATR_RES CMD_0/CMD_1 Error, CMD_0=%x, CMD_1=%x", inbuf[7], inbuf[8]);
        ms_card_detection(dev);
        return 0;
    }
    dev->temp_rx_buffer[idx++]=NAL_EVT_P2P_TARGET_DISCOVERED;
    dev->temp_rx_buffer[idx++]=0x00;
    dev->temp_rx_buffer[idx++]=0x01;
    dev->temp_rx_buffer[idx++]=0x9E;
    dev->temp_rx_buffer[idx++]=0x10;
	
	if (ms_scan_gerenal_byte(atrResLen - 20, inbuf + 46)== W_FALSE)
	{
        fuseIdLen = 0;
		ms_write_efuse_id(0, fuseIdBuf);
	}
	else
	{
	    fuseIdLen = 11;
		memcpy( fuseIdBuf, inbuf + 46 + atrResLen - 20 - 8, 8);
		ms_write_efuse_id(8, fuseIdBuf);
	}
	
    PNALDebugLog("[ms_open_nfc_initiator_detection_212_424_callback]gerenal bytes:");
	PNALDebugLogBuffer(inbuf + 46 , atrResLen - 20 - fuseIdLen); 
    memcpy( &dev->temp_rx_buffer[idx], inbuf + 46, atrResLen - 20 - fuseIdLen);  //only return gerenal bytes of ATR_RES
    idx+=(atrResLen - 20 - fuseIdLen);

    g_P2P_Speed = DRVP2P_sp424;
    SetEventAvailableBytes(dev, idx);
    PNI = 0;
    return 0;
}
/* P2P 212/424 Inventory High Layer API */
int ms_open_nfc_initiator_detection_212_424( tNALMsrComInstance  *dev )
{
    int length;
    PNALDebugLog("[ms_open_nfc_initiator_detection_212_424]\n");
#if 1
    length = 20 + LINK_PARAM_LEN;
    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    dev->ant_send_buffer[0] = 0x4A;
    dev->ant_send_buffer[1] = 18 + LINK_PARAM_LEN;    //LEN

    //Cmd Body Format
    //SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  ¡K , Param_n1(n3)
    dev->ant_send_buffer[2] = DrvP2P_FuncCode_EnaleP2P; //SubFuncCode.
    dev->ant_send_buffer[3] = 0x03;       //ParamNum.

    //ParaID(1)[0x00],ParamLen(1)[3], Speed(1),Mode(1),PollingTimes(1)
    dev->ant_send_buffer[4] = DrvP2P_ModeParam_Mode;
    dev->ant_send_buffer[5] = 0x03;
    dev->ant_send_buffer[6] = DRVP2P_sp424;
    dev->ant_send_buffer[7] = DRVP2P_Initiator;
    dev->ant_send_buffer[8] = 1;

    //ParaID(1)[0x01],ParamLen(1)[1], TSN(1)
    dev->ant_send_buffer[9] = DrvP2P_ModeParam_Polling_REQ;
    dev->ant_send_buffer[10] = 0x01;
    dev->ant_send_buffer[11] = 0x00;

    //ParaID(1)[0x01],Len[n+6],GeneralByteNum(1)[n],IDType(1)[1],Payload(n+4)
    dev->ant_send_buffer[12] = DrvP2P_ModeParam_ATR_REQ;
    dev->ant_send_buffer[13] = LINK_PARAM_LEN + 6;
    dev->ant_send_buffer[14] = LINK_PARAM_LEN;
    dev->ant_send_buffer[15] = DrvP2P_NFCID_FwGenerate;
    dev->ant_send_buffer[16] = DIDi; //DIDi
    dev->ant_send_buffer[17] = BSi;
    dev->ant_send_buffer[18] = BRi;
    if (LINK_PARAM_LEN > 0)
    {
        dev->ant_send_buffer[19] = PPi | ATR_GB_ENABLE ;
        memcpy(&dev->ant_send_buffer[20], LINK_PARAM, LINK_PARAM_LEN);
    }
    else
    {
        dev->ant_send_buffer[19] = PPi;
    }
#else
    length = 30 + LINK_PARAM_LEN;
    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    dev->ant_send_buffer[0] = 0x4A;
    dev->ant_send_buffer[1] = 28 + LINK_PARAM_LEN;    //LEN

    //Cmd Body Format
    //SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  ¡K , Param_n1(n3)
    dev->ant_send_buffer[2] = DrvP2P_FuncCode_EnaleP2P; //SubFuncCode.
    dev->ant_send_buffer[3] = 0x03;       //ParamNum.

    //ParaID(1)[0x00],ParamLen(1)[3], Speed(1),Mode(1),PollingTimes(1)
    dev->ant_send_buffer[4] = DrvP2P_ModeParam_Mode;
    dev->ant_send_buffer[5] = 0x03;
    dev->ant_send_buffer[6] = DRVP2P_sp424;
    dev->ant_send_buffer[7] = DRVP2P_Initiator;
    dev->ant_send_buffer[8] = 10;

    //ParaID(1)[0x01],ParamLen(1)[1], TSN(1)
    dev->ant_send_buffer[9] = DrvP2P_ModeParam_Polling_REQ;
    dev->ant_send_buffer[10] = 0x01;
    dev->ant_send_buffer[11] = 0x00;

    //ParaID(1)[0x01],Len[n+6],GeneralByteNum(1)[n],IDType(1)[1],Payload(n+4)
    dev->ant_send_buffer[12] = DrvP2P_ModeParam_ATR_REQ;
    dev->ant_send_buffer[13] = LINK_PARAM_LEN + 16;
    dev->ant_send_buffer[14] = LINK_PARAM_LEN;

      dev->ant_send_buffer[15] = DrvP2P_NFCID_HostSpec;
      memcpy( &dev->ant_send_buffer[16], NFCID3i, 10);

    dev->ant_send_buffer[26] = DIDi; //DIDi
    dev->ant_send_buffer[27] = BSi;
    dev->ant_send_buffer[28] = BRi;
    if (LINK_PARAM_LEN > 0)
    {
        dev->ant_send_buffer[29] = PPi | ATR_GB_ENABLE ;
        memcpy(&dev->ant_send_buffer[30], LINK_PARAM, LINK_PARAM_LEN);
    }
    else
    {
        dev->ant_send_buffer[29] = PPi;
    }
#endif
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_detection_212_424_callback;
    return length;
}

static int ms_gen_ack_pdu_212_424(tNALMsrComInstance  *dev, unsigned char pni, unsigned char ack_type)
{
    //Format:
    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    dev->ant_send_buffer[0] = 0x4A;
    dev->ant_send_buffer[1] = 14;    //LEN

    //Cmd Body Format
    //SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  ¡K , Param_n1(n3)
    dev->ant_send_buffer[2] = DrvP2P_FuncCode_DEP; //SubFuncCode.
    dev->ant_send_buffer[3] = 0x02;       //ParamNum.

    //DrvP2P_ModeParam_CtrlInfo= 6,
    //ParaID(1)[0x06],ParamLen(1)[2], WT (1),RTOX(1)
    dev->ant_send_buffer[4] = DrvP2P_ModeParam_CtrlInfo;
    dev->ant_send_buffer[5] = 0x02;
    dev->ant_send_buffer[6] = DEP_WT;
    dev->ant_send_buffer[7] = 0x01;

    //DrvP2P_ModeParam_RawData= 5,
    //ParaID(1)[0x05],ParamLen(1)[n+2], Flag (1),CmdLen(1), Cmd(n)
    dev->ant_send_buffer[8] = DrvP2P_ModeParam_RawData;
    dev->ant_send_buffer[9] = 0x06;
    dev->ant_send_buffer[10] = DrvP2P_DepFlag_WaitRx;
    dev->ant_send_buffer[11] = 0x04;
    dev->ant_send_buffer[12] = 0x04;
    dev->ant_send_buffer[13] = CMD_0_REQ;
    dev->ant_send_buffer[14] = CMD_1_DEP_REQ;
    dev->ant_send_buffer[15] = PDU_ACK | ack_type | pni;
    return 16;
}

static int ms_gen_sup_pdu_212_424(tNALMsrComInstance  *dev, unsigned char pfb, unsigned char to)
{
    //Format:
    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    dev->ant_send_buffer[0] = 0x4A;
    dev->ant_send_buffer[1] = 0;    //LEN

    //Cmd Body Format
    //SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  ¡K , Param_n1(n3)
    dev->ant_send_buffer[2] = DrvP2P_FuncCode_DEP; //SubFuncCode.
    dev->ant_send_buffer[3] = 0x02;       //ParamNum.

    //DrvP2P_ModeParam_CtrlInfo= 6,
    //ParaID(1)[0x06],ParamLen(1)[2], WT (1),RTOX(1)
    dev->ant_send_buffer[4] = DrvP2P_ModeParam_CtrlInfo;
    dev->ant_send_buffer[5] = 0x02;
    dev->ant_send_buffer[6] = DEP_WT;
    dev->ant_send_buffer[7] = 0x01;

    //DrvP2P_ModeParam_RawData= 5,
    //ParaID(1)[0x05],ParamLen(1)[n+2], Flag (1),CmdLen(1), Cmd(n)
    dev->ant_send_buffer[8] = DrvP2P_ModeParam_RawData;
    dev->ant_send_buffer[9] = 0x00;
    dev->ant_send_buffer[10] = DrvP2P_DepFlag_WaitRx;
    dev->ant_send_buffer[11] = 0x00;
    dev->ant_send_buffer[12] = 0x00;
    dev->ant_send_buffer[13] = CMD_0_REQ;
    dev->ant_send_buffer[14] = CMD_1_DEP_REQ;
    dev->ant_send_buffer[15] = pfb;
    if ((pfb & SUP_TYPE_TO_MASK) == SUP_TYPE_TO)
    {
        dev->ant_send_buffer[16] = to;
        dev->ant_send_buffer[1] = 15;
        dev->ant_send_buffer[11] = 5;
        dev->ant_send_buffer[12] = 5;
        dev->ant_send_buffer[9] = 7;
        return 17;
    }
    else
    {
        dev->ant_send_buffer[1] = 14;
        dev->ant_send_buffer[11] = 4;
        dev->ant_send_buffer[12] = 4;
        dev->ant_send_buffer[9] = 6;
        return 16;
    }
}
int ms_open_nfc_initiator_dep_req_212_424_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0, cmd_len = 0, i = 0;
    unsigned short rcvCrc;
    unsigned short calCrc;
    unsigned char pfb = 0x00, rptlen, pni, didt, to;
    unsigned char rcv_byte_len;
    int sodlen = 0, payloadIdx = 0;

    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;
    if (ms_chk_internal_error(inbuf, len) == 1)
    {
        CNALWriteDataFinish(dev);
        //ms_open_nfc_initiator_time_out(dev);
        return 0;
    }
    if (inbuf == NULL || len <= 0)
    {
        CNALWriteDataFinish(dev);
        //ms_open_nfc_initiator_time_out(dev);
        return 0;
    }
    //A3Rsp(1)[0x02], Len(1)[n],SubFuncCode(1),RF_resultCode(1)
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback], len = %d\n", len);
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]inbuf:");
    PNALDebugLogBuffer(inbuf, len);

    PNALDebugLog("Status= %x\n", inbuf[3]);

    if(inbuf[3] != RFID_STS_SUCCESS)
    {
RESEND:
        if (ATN_CUR_CNT >= ATN_MAX_TRY_CNT || DEP_NACK_CUR_CNT >= DEP_MAX_TRY_CNT)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP Fail, resume");
            CNALWriteDataFinish(dev);
            //ms_open_nfc_initiator_time_out(dev);
        }
        else if (DEP_NACK_SEND_FLAG == W_FALSE)
        {
            //Send ATN pdu
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]Send ATN PDU, ATN_CUR_CNT:%d", ATN_CUR_CNT);
            pfb = PDU_SUP | SUP_TYPE_ATN ;
            cmd_len = ms_gen_sup_pdu_212_424(dev, pfb, 0);
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_212_424_callback;
            ATN_CUR_CNT++;
        }
        else
        {
            //Send Nack
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]Send NACK PDU, DEP_NACK_CUR_CNT:%d", DEP_NACK_CUR_CNT);
            cmd_len = ms_gen_ack_pdu_212_424(dev, PNI, DEP_RES_NACK);
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_212_424_callback;
            DEP_NACK_CUR_CNT++;
        }
        return 0;
    }
    //P2P Rsp Format:
    //Fail: A3Rsp(1)[0x02], Len(1)[n],SubFuncCode(1),RF_resultCode(1)
    //Success: A3Rsp(1)[0x02], Len(1)[n],SubFuncCode(1),RF_resultCode(1),RF_signal_Len(1)[n],RF_signal_Buf(n)
    //02 20 01 01 1D
    //1B D4 06 00 05 31 02 02 00 80 06 0F 63 6F 6D 2E 61 6E 64 72 6F 69 64 2E 6E 70 70 A0 CA
    //check recv len
    DEP_NACK_SEND_FLAG = W_TRUE;
    rcv_byte_len = inbuf[4];
    if (rcv_byte_len <= 0)
    {
        PNALDebugError("[ms_open_nfc_initiator_dep_req_212_424_callback]rcv_byte_len<=0");
        goto RESEND;
        return 0;
    }
    //check payload crc
    sodlen = inbuf[5];
    rcvCrc = inbuf[ 4 + rcv_byte_len ] | (inbuf[ 4 + rcv_byte_len - 1 ] << 8);
    calCrc = CRC16( CRC_ISO_18092_248,  rcv_byte_len - 2, inbuf + 5);
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]rcvCrc=%d, calCrc=%d", rcvCrc, calCrc);
    //pni = pfb & 0x03;
    if( (rcvCrc != calCrc))// || (PNI != pni))
    {
        //invalid CRC
        PNALDebugError("[ms_open_nfc_initiator_dep_req_212_424_callback]CRC Check Error");
        goto RESEND;
        return 0;
    }
    rptlen =  inbuf[1];
    pfb = inbuf[8];
    if (inbuf[6]!=0xD5 || inbuf[7]!=0x07)
    {
        PNALDebugError("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_RES CMD_0/CMD_1 Error");
        PNALDebugError("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_RES CMD_0=%x, CMD_1=%x", inbuf[7], inbuf[8]);
        goto RESEND;
        return 0;
    }
    payloadIdx = 3;
    if ((pfb & DEP_DID_ENABLE) == DEP_DID_ENABLE)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_DID_ENABLE");
        didt = inbuf[9];
        payloadIdx = 4;
        //protocol error
        if (didt != DIDi)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]Protocol Error");
            goto RESEND;
            return 0;
        }
    }
    if ((pfb & PDU_MASK) == PDU_ACK)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP TYPE: ACK\n");
        if ((pfb & DEP_RES_NACK) == DEP_RES_NACK) //Resend
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_RES: Get NACK\n");
            ms_open_nfc_initiator_dep_req_212_424(dev, DEP_BUF, DEP_BUF_LEN);
            return 0;
        }
        DEP_SEND_TOTAL_LEN += DEP_SEND_LEN;
        if (DEP_SEND_TOTAL_LEN >= DEP_BUF_LEN)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_SEND_TOTAL_LEN >= DEP_BUF_LEN, DoNothing\n");
            CNALWriteDataFinish(dev);
            return 0;
        }
        else
        {

            if (++PNI == 4)
            {
                PNI = 0;
            }

            ms_open_nfc_initiator_dep_req_212_424(dev, DEP_BUF, DEP_BUF_LEN);
        }
    }
    else if ((pfb & PDU_MASK) == PDU_INF)
    {
        if (DEP_MI_Flag == 1)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]Recv unknow data");
            CNALWriteDataFinish(dev);
            return 0;
        }
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP TYPE: INFO.., payloadIdx=%d\n", payloadIdx);
        memcpy(&DEP_RES_BUF[DEP_RECV_TOTAL_LEN], &inbuf[5 + 1 + payloadIdx], sodlen - 1 - payloadIdx);
        DEP_RECV_TOTAL_LEN += (sodlen - 1 - payloadIdx);
        if (++PNI == 4)
        {
            PNI = 0;
        }
        if (((pfb & DEP_MI_MASK) == DEP_MI_DISABLE) || (sodlen < PAYLOAD_MAX_SIZE))
        {
            dev->temp_rx_buffer[idx++] = NAL_EVT_P2P_SEND_DATA;
            for (i=0; i<DEP_RECV_TOTAL_LEN; i++)
            {
                dev->temp_rx_buffer[idx++] = DEP_RES_BUF[i];
            }
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]SEND DATA:");
            PNALDebugLogBuffer(dev->temp_rx_buffer, idx);
            if (!SetEventAvailableBytes(dev, idx))
            {
                CNALWriteDataFinish(dev);
            }
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]Get DEP_RES Success");
        }
        else
        {
            pni = pfb & 0x03;
            pni = PNI;
            cmd_len = ms_gen_ack_pdu_212_424(dev, pni, DEP_RES_ACK);
            /* Send command to Target */
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_212_424_callback;
        }
    }
    else if ((pfb & PDU_MASK) == PDU_SUP)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP TYPE: SUP\n");
        if ((pfb & SUP_TYPE_TO) == SUP_TYPE_TO)
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_RES: Get TO pdu\n");
            //to = inbuf[11];
            to = inbuf[5 + 1 + payloadIdx];
            cmd_len = ms_gen_sup_pdu_212_424(dev, pfb, to);
            /* Send command to Target */
            ms_interfaceSend(dev, cmd_len);
            dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_212_424_callback;
        }
        else //recv ATN, need to resend
        {
            PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP_RES: Get ATN pdu\n");
            ms_open_nfc_initiator_dep_req_212_424(dev, DEP_BUF, DEP_BUF_LEN);
            return 0;
        }
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424_callback]DEP TYPE: ERROR\n");
        goto RESEND;
    }
    return 0;
}

static int ms_open_nfc_initiator_dep_req_212_424( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int retval = SendLen, i;
    u16 length = 0, datalen = 0, cmd_bit_len, dep_max_size;
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]\n");
    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    dev->ant_send_buffer[0] = 0x4A;
    dev->ant_send_buffer[1] = 0;    //LEN

    //Cmd Body Format
    //SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  ¡K , Param_n1(n3)
    dev->ant_send_buffer[2] = DrvP2P_FuncCode_DEP; //SubFuncCode.
    dev->ant_send_buffer[3] = 0x02;       //ParamNum.

    //DrvP2P_ModeParam_CtrlInfo= 6,
    //ParaID(1)[0x06],ParamLen(1)[2], WT (1),RTOX(1)
    dev->ant_send_buffer[4] = DrvP2P_ModeParam_CtrlInfo;
    dev->ant_send_buffer[5] = 0x02;
    dev->ant_send_buffer[6] = DEP_WT;
    dev->ant_send_buffer[7] = 0x01;

    //DrvP2P_ModeParam_RawData= 5,
    //ParaID(1)[0x05],ParamLen(1)[n+2], Flag (1),CmdLen(1), Cmd(n)
    dev->ant_send_buffer[8] = DrvP2P_ModeParam_RawData;
    dev->ant_send_buffer[9] = 0x00;
    dev->ant_send_buffer[10] = DrvP2P_DepFlag_WaitRx;  //flag
    dev->ant_send_buffer[11] = 0x00;       //cmdlen
    dev->ant_send_buffer[12] = 0x00;
    dev->ant_send_buffer[13] = CMD_0_REQ;
    dev->ant_send_buffer[14] = CMD_1_DEP_REQ;
    dep_max_size = PAYLOAD_MAX_SIZE - DEP_HEADER_SZIE -1;  // 1: len
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]DEP_SEND_TOTAL_LEN = %d\n", DEP_SEND_TOTAL_LEN);

    if ((DEP_BUF_LEN - DEP_SEND_TOTAL_LEN) <= dep_max_size)
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]MI_DISABLE\n");
        dev->ant_send_buffer[15] = PDU_INF | DEP_MI_DISABLE | PNI;
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]DEP_BUF_DATA: \n");
        PNALDebugLogBuffer(&pSendBuf[DEP_SEND_TOTAL_LEN], DEP_BUF_LEN - DEP_SEND_TOTAL_LEN);
        for (i=0; i<DEP_BUF_LEN - DEP_SEND_TOTAL_LEN; i++)
        {
            dev->ant_send_buffer[i+16] = pSendBuf[DEP_SEND_TOTAL_LEN + i];
        }
        datalen = DEP_BUF_LEN - DEP_SEND_TOTAL_LEN;
        DEP_MI_Flag = 0;
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]MI_ENABLE\n");
        dev->ant_send_buffer[15] = PDU_INF | DEP_MI_ENABLE | PNI;
        memcpy(&dev->ant_send_buffer[16], pSendBuf+DEP_SEND_TOTAL_LEN, dep_max_size);
        datalen = dep_max_size;
        DEP_MI_Flag = 1;
    }
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]DataLen= %d\n", datalen);
    dev->ant_send_buffer[11] = 4 + datalen; // 4: (len, cmd0, cmd1, PFB)
    dev->ant_send_buffer[12] = 4 + datalen; // 4: (len, cmd0, cmd1, PFB)
    dev->ant_send_buffer[9] = dev->ant_send_buffer[11] + 2; // Flag (1),CmdLen(1)
    dev->ant_send_buffer[1] = 14 + datalen;    //LEN
    length = 2 + dev->ant_send_buffer[1]; // 2: uniprotocol: cmd(1) + len(1)
    DEP_SEND_LEN = datalen;
    PNALDebugLog("[ms_open_nfc_initiator_dep_req_212_424]send len= %d\n", length);
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dep_req_212_424_callback;
    return retval;
}


int ms_open_nfc_initiator_pl_config_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_open_nfc_initiator_pl_config_callback]start, len = %d\n", len);
    PNALDebugLogBuffer(inbuf, len);
    //A3Rsp(1)[0x02] , Len(1)[n], SubFuncCode(1), ParamNum(1) [n1] , ParaID(1)[0xA0],ParamLen(1)[n],Result1
    if (inbuf[6] != RFID_STS_SUCCESS)
    {
        PNALDebugLog("[ms_open_nfc_initiator_pl_config_callback]Result: fail, ecode= %x\n", inbuf[6]);
    }	
	SetParamFlag = W_TRUE;
	dev->rx_buffer[0] = NAL_SERVICE_P2P_INITIATOR;
	dev->rx_buffer[1] = NAL_RES_OK;
	dev->nb_available_bytes = 2;
    PNALDebugLog("[ms_open_nfc_initiator_pl_config_callback]end");
    return 0;
}

static int ms_open_nfc_initiator_pl_config(tNALMsrComInstance  *dev)
{
    int length = 0;
	//int offset = 0;
	PNALDebugLog("[ms_open_nfc_initiator_pl_config]start");	
	
	//A3Cmd(1)[0x4B], Len(1)[n],Body(n)
	dev->ant_send_buffer[0] = PL_CMD_CODE;
	dev->ant_send_buffer[1] = 0;	  //LEN
	
	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
	dev->ant_send_buffer[2] = DrvPL_FuncCode_Config; //SubFuncCode.
	dev->ant_send_buffer[3] = 0x02; 	  //ParamNum.
/*	
	ParaID(1)[0x00],ParamLen(1)[n1], DrvPL_ConfigTableFlag_et (1),PL_CmdCount(1)(n2), [{XXX} * n2]
	{XXX} format
	ID(1), CmdLen(1)[n3],CmdBuf(n3)
	ID format
	DrvPL_Category_et | ID
*/
	dev->ant_send_buffer[4] = DrvPL_ModeParam_ConfigTable;
    dev->ant_send_buffer[5] = 24 + LINK_PARAM_LEN;  //ParamLen
    dev->ant_send_buffer[6] = DrvPL_Category_Target;  //DrvPL_ConfigTableFlag_et
	dev->ant_send_buffer[7] = 0x01;		
	dev->ant_send_buffer[8] = DrvPL_PLItem_PI_P2P_424;
	dev->ant_send_buffer[9] = 20 + LINK_PARAM_LEN;	

    length = 33 + LINK_PARAM_LEN;
    //A3Cmd(1)[0x4A], Len(1)[n],Body(n)
    dev->ant_send_buffer[10] = 0x4A;
    dev->ant_send_buffer[11] = 18 + LINK_PARAM_LEN;    //LEN

    //Cmd Body Format
    //SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,  ¡K , Param_n1(n3)
    dev->ant_send_buffer[12] = DrvP2P_FuncCode_EnaleP2P; //SubFuncCode.
    dev->ant_send_buffer[13] = 0x03;       //ParamNum.

    //ParaID(1)[0x00],ParamLen(1)[3], Speed(1),Mode(1),PollingTimes(1)
    dev->ant_send_buffer[14] = DrvP2P_ModeParam_Mode;
    dev->ant_send_buffer[15] = 0x03;
    dev->ant_send_buffer[16] = DRVP2P_sp424;
    dev->ant_send_buffer[17] = DRVP2P_Initiator;
    dev->ant_send_buffer[18] = 1;

    //ParaID(1)[0x01],ParamLen(1)[1], TSN(1)
    dev->ant_send_buffer[19] = DrvP2P_ModeParam_Polling_REQ;
    dev->ant_send_buffer[20] = 0x01;
    dev->ant_send_buffer[21] = 0x00;

    //ParaID(1)[0x01],Len[n+6],GeneralByteNum(1)[n],IDType(1)[1],Payload(n+4)
    dev->ant_send_buffer[22] = DrvP2P_ModeParam_ATR_REQ;
    dev->ant_send_buffer[23] = LINK_PARAM_LEN + 6;
    dev->ant_send_buffer[24] = LINK_PARAM_LEN;
    dev->ant_send_buffer[25] = DrvP2P_NFCID_FwGenerate;
    dev->ant_send_buffer[26] = DIDi; //DIDi
    dev->ant_send_buffer[27] = BSi;
    dev->ant_send_buffer[28] = BRi;
    if (LINK_PARAM_LEN > 0)
    {
        dev->ant_send_buffer[29] = PPi | ATR_GB_ENABLE ;
        memcpy(&dev->ant_send_buffer[30], LINK_PARAM, LINK_PARAM_LEN);
    }
    else
    {
        dev->ant_send_buffer[29] = PPi;
    }
    dev->ant_send_buffer[30 + LINK_PARAM_LEN] = DrvPL_ModeParam_FlashOp;
    dev->ant_send_buffer[31 + LINK_PARAM_LEN] = 1;    //LEN	
    dev->ant_send_buffer[32 + LINK_PARAM_LEN] = DrvPL_FlashAction_Write;	

    dev->ant_send_buffer[1] = length - 2;	  //LEN
    /* Send command to Target */
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_pl_config_callback;
	PNALDebugLog("[ms_open_nfc_initiator_pl_config]end");	
    return length;	
}

/*
int ms_open_nfc_initiator_dsl_req_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0;

    PNALDebugLog("[ms_open_nfc_initiator_dsl_req_callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;
    if (inbuf == NULL || len <= 0)
    {
        //dev->rx_buffer[idx++] = NAL_EVT_NFCC_ERROR;
        return 0;
    }
    //UniProtocol report format:
    //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
    if(inbuf[2] != RFID_STS_SUCCESS)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }

    if((inbuf[5] == 0xD5) && (inbuf[6] == 0x09))
    {
        dev->rx_buffer[idx++]=NAL_RES_OK;   //success
    }
    else
    {
        dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
    }
    dev->nb_available_bytes = idx;
    return 0;
}

static int ms_open_nfc_initiator_dsl_req( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int length;

    PNALDebugLog("[ms_open_nfc_initiator_dsl_req]\n");

    length = 12;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x0A;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = 0x51;
    dev->ant_send_buffer[5] = 0x00;
    dev->ant_send_buffer[6] = 0x18;
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = CMD_0_REQ;
    dev->ant_send_buffer[10] = CMD_1_DSL_REQ;
    dev->ant_send_buffer[11] = DIDi; //DIDi

    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_dsl_req_callback;
    return length;
}

int ms_open_nfc_initiator_rls_req_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    int idx  = 0;

    PNALDebugLog("[ms_open_nfc_initiator_rls_req_callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_P2P_INITIATOR;
    if (inbuf == NULL || len <= 0)
    {
        //dev->rx_buffer[idx++] = NAL_EVT_NFCC_ERROR;
        return 0;
    }
    //UniProtocol report format:
    //|xxx|Len|Status|rpt_bit_len_hi|rpt_bit_len_lo|DATA...|
    if(inbuf[2] != RFID_STS_SUCCESS)
    {
        dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        dev->nb_available_bytes = idx;
        return 0;
    }

    if((inbuf[5] == 0xD5) && (inbuf[6] == 0x0B))
    {
        dev->rx_buffer[idx++]=NAL_RES_OK;   //success
    }
    else
    {
        dev->rx_buffer[idx++]=NAL_RES_TIMEOUT;
    }
    dev->nb_available_bytes = idx;
    return 0;
}

static int ms_open_nfc_initiator_rls_req( tNALMsrComInstance  *dev, unsigned char *pSendBuf, int SendLen )
{
    int length;

    PNALDebugLog("[ms_open_nfc_initiator_rls_req]\n");

    length = 12;
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x0A;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x00;
    dev->ant_send_buffer[4] = 0x51;
    dev->ant_send_buffer[5] = 0x00;
    dev->ant_send_buffer[6] = 0x18;
    dev->ant_send_buffer[7] = 0x00;
    dev->ant_send_buffer[8] = 0x00;
    dev->ant_send_buffer[9] = CMD_0_REQ;
    dev->ant_send_buffer[10] = CMD_1_RLS_REQ;
    dev->ant_send_buffer[11] = DIDi; //DIDi

    //Send command to Target
    length = ms_interfaceSend(dev, length);
    dev->cbRecv = (RecvCallBack *)ms_open_nfc_initiator_rls_req_callback;
    return length;

}
*/
