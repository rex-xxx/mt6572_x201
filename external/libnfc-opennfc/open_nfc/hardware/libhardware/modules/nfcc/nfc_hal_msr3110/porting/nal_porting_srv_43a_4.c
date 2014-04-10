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
#include "nal_porting_srv_43a_4.h"
#include "nal_porting_srv_693.h"
#include "nal_porting_srv_util.h"

typedef unsigned short u16;
typedef unsigned char u8;

t43atype4Instance ISO43A_4_TAG;
static unsigned char DEP_BUF_43A[512];
static unsigned char DEP_LEN_43A = 0;
static unsigned char BLK_NUM_43A;
static unsigned char TEMP_OUT_BUF_43A[128];
static unsigned char TEMP_OUT_BUF_LEN_43A;
static u8 CUR_DEP_CNT_43A;
static bool_t NACK_SEND_FLAG_43A = W_FALSE;
static u8 MAX_DEP_TRY_CNT_43A = 0;

extern int ms_card_detection(tNALMsrComInstance  *dev);
static int ms_open_nfc_43a_4_xchg_data( tNALMsrComInstance  *dev, unsigned char *outbuf, int len);

int ms_open_nfc_43a_4_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
  int retval = 0;

  if (outbuf == NULL || len == 0)
  {
      return -1;
  }

  switch( dev->curCmdCode)
  {
      case NAL_CMD_SET_PARAMETER:
	    dev->rx_buffer[0] = NAL_SERVICE_READER_14_A_4;
	    dev->rx_buffer[1] = NAL_RES_OK;
	    dev->nb_available_bytes = 2;
          break;

      case NAL_CMD_READER_XCHG_DATA:
          //BLK_NUM_43A = 0;
          DEP_LEN_43A = 0;
          CUR_DEP_CNT_43A = 0;
          memcpy(TEMP_OUT_BUF_43A, outbuf, len);
          TEMP_OUT_BUF_LEN_43A = len;
	    retval = ms_open_nfc_43a_4_xchg_data(dev, outbuf, len);
          break;

      default:
	    dev->rx_buffer[0] = NAL_SERVICE_READER_14_A_4;
	    dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
	    dev->nb_available_bytes = 2;
          break;

  }

  return retval;
}


static int ms_43a_gen_ack_pdu(tNALMsrComInstance  *dev, unsigned char ack_type)
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
    dev->ant_send_buffer[9] = 0xA2 | BLK_NUM_43A | ack_type;
    return 10;
}
static int ms_open_nfc_43a_4_xchg_data_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    u16 idx  = 0;
    u16 cal_CRC, chk_CRC;
    u8 rcv_byte_len, pfb, cmd_len;

    PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]\n");
    dev->rx_buffer[idx++] = NAL_SERVICE_READER_14_A_4;
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
        PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]CUR_DEP_CNT_43A=%x", CUR_DEP_CNT_43A);
		if (NACK_SEND_FLAG_43A == W_FALSE)
        {
            NACK_SEND_FLAG_43A = W_TRUE;
            PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]A3 Recv Err = %x\n", inbuf[2]);
            cmd_len = ms_43a_gen_ack_pdu(dev, R_BLK_NACK);
            ms_interfaceSend( dev, cmd_len);
    	      dev->cbRecv = ms_open_nfc_43a_4_xchg_data_callback;
        }
        else if (CUR_DEP_CNT_43A >= MAX_DEP_TRY_CNT_43A)
        {
            dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
            dev->nb_available_bytes = idx;
            PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]DEP Fail");
            //CNALWriteDataFinish(dev);
            CUR_DEP_CNT_43A = 0;
        }
        else
        {
            PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]DEP RETRY\n");
            CUR_DEP_CNT_43A++;
            ms_open_nfc_43a_4_xchg_data(dev, TEMP_OUT_BUF_43A, TEMP_OUT_BUF_LEN_43A);
        }
        return 0;
    }
    //CRC check
    rcv_byte_len = ((u16)(inbuf[3]<<8)|inbuf[4])/8;
	PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]rcv_byte_len = %d\n", rcv_byte_len);
    if (rcv_byte_len <= 2)
    {
        PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]rcv_byte_len < 2, Send NACK");
        cmd_len = ms_43a_gen_ack_pdu(dev, R_BLK_NACK);
        ms_interfaceSend( dev, cmd_len);
	    dev->cbRecv = ms_open_nfc_43a_4_xchg_data_callback;
        return 0;
    }
    cal_CRC = CRC16( CRC_ISO_14443A, (rcv_byte_len-2), inbuf+5 );
    chk_CRC = ((u16)(inbuf[5+(rcv_byte_len - 1)]<<8))|inbuf[5+(rcv_byte_len - 2)];
    if(cal_CRC != chk_CRC)
    {
        PNALDebugLog("CRC err, cal_CRC = %04X", cal_CRC);
        PNALDebugLog("CRC err, chk_CRC = %04X", chk_CRC);
        cmd_len = ms_43a_gen_ack_pdu(dev, R_BLK_NACK);
        ms_interfaceSend( dev, cmd_len);
	    dev->cbRecv = ms_open_nfc_43a_4_xchg_data_callback;
        //dev->rx_buffer[idx++] = NAL_RES_TIMEOUT;
        //dev->nb_available_bytes = idx;
        return 0;
    }
    pfb = inbuf[5];
    BLK_NUM_43A++;
    if (BLK_NUM_43A >=2)
    {
        BLK_NUM_43A = 0;
    }
    PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]pfb=%x", pfb);
    if ((inbuf[5] & 0x10) == 0x10) //chaining
    {
        PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]Get chaining pdu");
        memcpy(&DEP_BUF_43A[DEP_LEN_43A], inbuf+5+1, rcv_byte_len - 2 -1);
        DEP_LEN_43A += rcv_byte_len - 2 -1;
        cmd_len = ms_43a_gen_ack_pdu(dev, R_BLK_ACK);
        ms_interfaceSend( dev, cmd_len);
	    dev->cbRecv = ms_open_nfc_43a_4_xchg_data_callback;
	    return 0;
    }
    else
    {
        PNALDebugLog("[ms_open_nfc_43a_4_xchg_data_callback]Get DEP ok");
        memcpy(&DEP_BUF_43A[DEP_LEN_43A], inbuf+5+1, rcv_byte_len - 2 -1);
        DEP_LEN_43A += rcv_byte_len - 2 -1;
        dev->rx_buffer[idx++]=NAL_RES_OK;
        //memcpy( &dev->rx_buffer[idx], inbuf+5+1, rcv_byte_len - 2 -1 );
        memcpy( &dev->rx_buffer[idx], DEP_BUF_43A, DEP_LEN_43A );
        //idx+=(rcv_byte_len - 2 - 1);
        idx+=DEP_LEN_43A;
        dev->nb_available_bytes = idx;
        DEP_LEN_43A = 0;
    }
    return 0;
}

static int ms_open_nfc_43a_4_xchg_data( tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;
	unsigned char pcb;
	unsigned char nad;
	int datalen = 0;
	int dataidx;
      int cmd_bit_len;
	pcb = outbuf[2];
	dataidx = 3;
	datalen = len - 3;
    /*
	if( ( pcb && 0x0080))
	{
		nad = outbuf[3];
		dataidx = 4;
		datalen = len - 4;
	}
     */
	dev->ant_send_buffer[0] = 0x42;
	dev->ant_send_buffer[1] = 8 + datalen;
	dev->ant_send_buffer[2] = 0x80;
	dev->ant_send_buffer[3] = 0x00;
	dev->ant_send_buffer[4] = 0x91;  //0xd1
      cmd_bit_len = ( 1 + datalen) * 8;
      dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0xFF;  //cmd_bit_len_hi
      dev->ant_send_buffer[6] = (cmd_bit_len & 0xFF);        //cmd_bit_len_lo
	dev->ant_send_buffer[7] = 0x00; //rcv bit length, high
	dev->ant_send_buffer[8] = 0x00; //rcv bit length, low
	dev->ant_send_buffer[9] = (0x02 | BLK_NUM_43A);
      //dev->ant_send_buffer[10] = 0x00;
	memcpy( dev->ant_send_buffer + 10, outbuf + dataidx, datalen);

	retval = ms_interfaceSend( dev, 10 + datalen);
	dev->cbRecv = ms_open_nfc_43a_4_xchg_data_callback;
       NACK_SEND_FLAG_43A = W_FALSE;
	return retval;
}

static int ms_open_nfc_43a_4_Inventory_Callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{


  u8 TagNo = 0, idx = 0;

  PNALDebugLog("[ms_open_nfc_43a_4_Inventory_Callback], len = %d\n", len);

  PNALDebugLog("[ms_open_nfc_43a_4_Inventory_Callback]inbuf:");

  PNALDebugLogBuffer(inbuf, len);


  if (inbuf == NULL || len <= 0 || inbuf[0]!=0x02)

  {

      //dev->rx_buffer[idx++] = NAL_EVT_NFCC_ERROR;

      ms_card_detection(dev);

      return 0;

  }

  if(inbuf[2] == RFID_STS_SUCCESS)

  {

      TagNo = inbuf[3];


      if(TagNo > 1)

      {

          //Not need assign another data field.
          dev->temp_rx_buffer[idx++] = NAL_SERVICE_READER_14_A_4;
          dev->temp_rx_buffer[idx++] = NAL_EVT_READER_TARGET_COLLISION;

          //dev->reader_detect_mask = 0x0000;

          //dev->nb_available_bytes = idx;
          SetEventAvailableBytes(dev, idx);
          ms_card_detection(dev);

      }

      else if (TagNo == 1)

      {

          if (inbuf[24] != 0x20)

          {

              PNALDebugLog("[ms_open_nfc_43a_4_Inventory_Callback]not 43a_4 tag");

              ms_card_detection(dev);

              return 0;

          }


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

  }

  else

  {

      ms_card_detection(dev);

  }

  return 0;
}


/*
 ISO14443A Inventory High Layer API */
int ms_open_nfc_43a_4_Inventory( tNALMsrComInstance  *dev )
{


  int length;


  PNALDebugLog("[open_nfc_43a_4_Inventory]\n");


  /* Build A3 ISO14443A Inventory Command */

  //A3 Inventory Commmand:

  //|CMD(0x46)|LEN|TagLimit| ===> |0x46|0x01|0x01|

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

  dev->cbRecv = (RecvCallBack *)ms_open_nfc_43a_4_Inventory_Callback;

  return length;
}


static int ms_open_nfc_43a_4_rats_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{


  int idx  = 0, i;

  u16 rcv_byte_len;
  unsigned short rcvCrc;
  unsigned short calCrc;
  PNALDebugLog("[ms_open_nfc_43a_4_rats_callback], len = %d\n", len);
  PNALDebugLogBuffer(inbuf, len);
  if (inbuf == NULL || len <= 0 || inbuf[2] != RFID_STS_SUCCESS) //|| inbuf[0]!=0x02)
  {
      PNALDebugLog("[ms_open_nfc_43a_4_rats_callback]RATS Fail, detect resume");
      ms_card_detection(dev);
      return 0;
  }

  if (inbuf[0] == 0xFF)
  {
      PNALDebugLog("[ms_open_nfc_43a_4_rats_callback]change 0xFF -> 0x02\n");
      inbuf[0] = 0x02;
  }

  rcv_byte_len = ((u16)(inbuf[3]<<8)|inbuf[4])/8;
  if ((rcv_byte_len > 0) && (inbuf[1]>=5))
  {
      // check return CRC
      rcvCrc = ( inbuf[ len - 1] << 8) | inbuf[ len - 2];
      calCrc = CRC16( CRC_ISO_14443A,  inbuf[1] - 5, inbuf + 5);
      PNALDebugLog("rcvCrc=%d, calCrc=%d", rcvCrc, calCrc);
      if( rcvCrc != calCrc)
      {
          //invalid CRC
          PNALDebugLog("CRC Check Error, atr again");
          ms_open_nfc_43a_4_rats(dev);  //resent
          return 0;
      }
  }
  else
  {
      PNALDebugLog("[ms_open_nfc_43a_4_rats_callback]rcv_byte_len = 0");
      ms_card_detection(dev);
      return 0;
  }

  dev->temp_rx_buffer[idx++] = NAL_SERVICE_READER_14_A_4;
  dev->temp_rx_buffer[idx++]=NAL_EVT_READER_TARGET_DISCOVERED;
  dev->temp_rx_buffer[idx++]=ISO43A_4_TAG.ATQA[0];
  dev->temp_rx_buffer[idx++]=ISO43A_4_TAG.ATQA[1];
  dev->temp_rx_buffer[idx++]=ISO43A_4_TAG.SAK;
  //ATS Frame
  memcpy( &dev->temp_rx_buffer[idx], inbuf+5, inbuf[5] );
  idx+= inbuf[5];
  //UID
  memcpy( &dev->temp_rx_buffer[idx], ISO43A_4_TAG.UID, ISO43A_4_TAG.UID_Len);
  idx+= ISO43A_4_TAG.UID_Len;
  //dev->nb_available_bytes = idx;
  SetEventAvailableBytes(dev, idx);
  BLK_NUM_43A = 0;
  return 0;
}


int ms_open_nfc_43a_4_rats( tNALMsrComInstance  *dev )
{
  int length;
  u16 cmd_bit_len;
  PNALDebugLog("[ms_open_nfc_43a_4_rats]start...");

  //Format:
  //|Cmd(0x42)|Len|Option ID1(0x80)|Option ID2(0x00)|Flag(0xB1)|cmd bit len (hi)|cmd bit len(lo)|rpt bit len(hi)|rpt bit len(lo)|Cmd|...
  dev->ant_send_buffer[0] = 0x42;
  dev->ant_send_buffer[1] = 0x09;
  dev->ant_send_buffer[2] = 0x80;
  dev->ant_send_buffer[3] = 0x00;
  dev->ant_send_buffer[4] = 0x91; // 1sec
  dev->ant_send_buffer[5] = 0x00; //cmd bit len (hi)
  dev->ant_send_buffer[6] = 0x00; //cmd bit len(lo)
  dev->ant_send_buffer[7] = 0x00; //rpt bit len(hi)
  dev->ant_send_buffer[8] = 0x00; //rpt bit len(lo)
  dev->ant_send_buffer[9] = 0xE0; //start byte
  dev->ant_send_buffer[10] = 0x80;

  cmd_bit_len = 16; //128: 8 * 2 (start byte, param byte)
  dev->ant_send_buffer[5] = (cmd_bit_len >> 8) & 0xFF;  //cmd_bit_len_hi
  dev->ant_send_buffer[6] = (cmd_bit_len & 0xFF);        //cmd_bit_len_lo
  /* Send command to Target */
  length = ms_interfaceSend(dev, 11);
  dev->cbRecv = (RecvCallBack *)ms_open_nfc_43a_4_rats_callback;
  return 0;
}
