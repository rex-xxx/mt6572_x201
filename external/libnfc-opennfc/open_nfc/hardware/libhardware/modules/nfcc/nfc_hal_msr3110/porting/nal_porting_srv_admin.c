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
#include "nal_porting_srv_admin.h"
#include "interface.h"
#include "nfc_hal.h"
#include "nal_porting_srv_693.h"
#include "nal_porting_srv_43a.h"
#include "nal_porting_srv_p2p_i.h"
#include "nal_porting_srv_43b.h"
#include "nal_porting_srv_p2p_t.h"
#include "nal_porting_srv_43a_4.h"
#include "nal_porting_srv_felica.h"
#include "nal_porting_srv_type1.h"

#include "nal_porting_srv_p2p.h"



unsigned char CUR_CARD_TYPE = 0x00;
unsigned char Extern_RF_Detect = 0x00;
static unsigned char CUR_PARAM_CODE = 0x00; //Current Parameter Code for Recieve Callback function

const unsigned char CST_PARAM_PERSISTENT_POLICY[] =
	{ 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00 };

const unsigned char CST_PARAM_HARDWARE_INFO[] =
	{
		0x07, 0x56, 0x69, 0x72, 0x74, 0x75, 0x61, 0x6C, 0x20, 0x4E, 0x46, 0x43, 0x43, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x30, 0x31, 0x30, 0x32, 0x30, 0x33, 0x30, 0x34, 0x30, 0x35, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x56, 0x69, 0x72, 0x74, 0x75, 0x61, 0x6C, 0x20, 0x4E, 0x46, 0x43, 0x43, 0x20,
		0x6C, 0x6F, 0x61, 0x64, 0x65, 0x72, 0x20, 0x31, 0x2E, 0x32, 0x33, 0x61, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
	};
//		0x00, 0x00, 0x00, 0x00, 0x83, 0x05, 0xFF, 0x08, 0x00, 0x02,
const unsigned char CST_PARAM_FIRMWARE_INFO[] =
	{
	0x07, 0x56, 0x69, 0x72, 0x74, 0x75, 0x61, 0x6C, 0x20, 0x4E,
	0x46, 0x43, 0x43, 0x20, 0x66, 0x69, 0x72, 0x6D, 0x77, 0x61,
	0x72, 0x65, 0x20, 0x33, 0x2E, 0x34, 0x35, 0x61, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x80, 0x01, 0xD7, 0x0c, 0x04, 0x02, //all reader & p2p mode		
	//0x00, 0x00, 0x00, 0x00, 0x80, 0x00, 0x95, 0x0c, 0x04, 0x02, //all reader & p2p mode
	//0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0xD5, 0x0c, 0x04, 0x02, // only reader
	//0x00, 0x00, 0x00, 0x00, 0x80, 0x00, 0x80, 0x0c, 0x04, 0x02, // only p2p
	//0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x08, 0x00, 0x02,//type1
	//0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x0c, 0x04, 0x02, //typeb_4
	//0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x0c, 0x04, 0x02, // only type4
	0x02, 0x02, 0x02, 0x02, 0x02, 0x00, 0x00

	};

const unsigned char CST_PARAM_PERSISTENT_MEMORY[] =
	{0x4F, 0x4E, 0x46, 0x43, 0x30, 0x00, 0x00, 0x00};

#if 0
//for A3 Polling Mode
const unsigned char Config_43a[] =
	{0x46 ,0x04 ,0x01 ,0x00 ,0x27 ,0x10 ,0x82 ,0x00};
const unsigned char Config_693[] =
	{0x45 ,0x02 ,0x00 ,0x0C ,0x53 ,0x00};
const unsigned char Config_Felica[] =
	{0x42 ,0x0E ,0x80 ,0x06 ,0x81 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x06 ,0x00 ,0xFF ,0xFF ,0x01 ,0x00 ,0x5C ,0x03};
const unsigned char Config_Type1[] =
	{0x42 ,0x08 ,0x80 ,0x00 ,0x03 ,0x00 ,0x07 ,0x00 ,0x13 ,0x52 ,0x39 ,0x01};
#endif
const unsigned char INITIATOR_MAX_CNT = 0;
extern unsigned char GIniTryCnt;
unsigned char TargetRfParam = SDD_RF_PARAM_TAG;
unsigned char RFDetectSts = 0;
bool_t DETECT_CMD_RPT = W_FALSE;

const unsigned char RF_DETECT_STS_FIRST = 0x00;
const unsigned char RF_DETECT_STS_LAST = 0x02;

static int ms_open_nfc_admin_ms_reader_test(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_open_nfc_admin_cmd_persistent_policy(tNALMsrComInstance  *dev, unsigned char *outbuf, int len);
static int ms_swp_write_reg(tNALMsrComInstance  *dev);
int ms_detection_polling_req(tNALMsrComInstance  *dev);


static int ms_admin_cbrecv_get_parameter(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
	int retval = 0;
	PNALDebugLog( "ms_admin_cbrecv_get_parameter() - start \n");

	unsigned char firmwaredecp[32];
	unsigned char firmwareinfo[47];
	int idx;

	PNALDebugLog( "current parameter code: 0x%02X\n", CUR_PARAM_CODE);
	switch(CUR_PARAM_CODE)
	{
		case NAL_PAR_FIRMWARE_INFO:
			memset( firmwaredecp, 0, sizeof(firmwaredecp));
			sprintf( (char*)firmwaredecp, "%02x.%02x.20%02d-%02d-%02d-%02d.%02d.%02d",
				inbuf[2], inbuf[3], inbuf[7], inbuf[5], inbuf[6], inbuf[8], inbuf[9], inbuf[10]);

			PNALDebugLog( "Mixed: %02x %02x %02d %02d %02d %02d %02d %02d \n", inbuf[2], inbuf[3], inbuf[7], inbuf[5], inbuf[2], inbuf[8], inbuf[9], inbuf[10]);
			PNALDebugLog( "Mixed Result: %02x.%02x.20%02d-%02d-%02d-%02d.%02d.%02d \n", inbuf[2], inbuf[3], inbuf[7], inbuf[5], inbuf[6], inbuf[8], inbuf[9], inbuf[10]);

			memcpy( firmwareinfo, CST_PARAM_FIRMWARE_INFO, 47);
			memcpy( firmwareinfo + 1, firmwaredecp, 32);

			PNALDebugLog( "firmware info: \n");
			for( idx = 0; idx < 47; idx++)
			{
				PNALDebugLog( "0x%02X ", firmwareinfo[idx]);
			}
			PNALDebugLog( "\n");

			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			memcpy( dev->rx_buffer + 2, firmwareinfo, 47);
			dev->nb_available_bytes = 1 + 1 + 47;

			break;

		default:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_UNKNOWN_PARAM;
			dev->nb_available_bytes = 2;

			PNALDebugError( "unknown parameter\n");
			break;

	}

	PNALDebugLog( "ms_admin_cbrecv_get_parameter() - end \n");
	return retval;
}

int ms_open_nfc_admin_cbrecv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
	PNALDebugLog( " ms_open_nfc_admin_cbrecv() - start \n");

	int retval = 0;

	PNALDebugLog( "command code: 0x%02X\n", dev->curCmdCode);
	switch( dev->curCmdCode)
	{
		case NAL_CMD_GET_PARAMETER:
			retval = ms_admin_cbrecv_get_parameter( dev, inbuf, len);
			break;

		default:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
			dev->nb_available_bytes = 2;

			PNALDebugError( "wrong command code \n");
			break;
	}

	PNALDebugLog( " ms_open_nfc_admin_cbrecv() - end \n");
	return retval;
}

int ms_open_nfc_admin_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = 0;
	int res = 0;

	PNALDebugLog( "ms_open_nfc_admin_disp() - start \n");

    if (outbuf == NULL || len == 0)
    {
    	PNALDebugError( "recieve buffer: NULL \n");
		PNALDebugLog( "ms_open_nfc_admin_disp() - end \n");
        return -1;
    }
    PNALDebugLog( "[ms_open_nfc_admin_disp]dev->curCmdCode:0x%02X\n", dev->curCmdCode);
    switch( dev->curCmdCode)
    {
        case NAL_CMD_GET_PARAMETER:
            retval = ms_open_nfc_admin_cmd_get_parameter( dev, outbuf, len);
            break;

        case NAL_CMD_SET_PARAMETER:
			retval = ms_open_nfc_admin_cmd_set_parameter(dev, outbuf, len);
            break;

		case NAL_CMD_UPDATE_FIRMWARE:
			retval = ms_open_nfc_admin_cmd_update_firmware(dev, outbuf, len);
            break;

		case NAL_CMD_DETECTION:
			retval = ms_open_nfc_admin_cmd_detection(dev, outbuf, len);
            break;

		case NAL_CMD_PRODUCTION_TEST:
			retval = ms_open_nfc_admin_cmd_production_test(dev, outbuf, len);
            break;

		case NAL_CMD_SELF_TEST:
			retval = ms_open_nfc_admin_cmd_self_test(dev, outbuf, len);
            break;

		case NAL_CMD_MS_READER_TEST:
			retval = ms_open_nfc_admin_ms_reader_test(dev, outbuf, len);
            break;
        case NAL_EVT_STANDBY_MODE:

            PNALDebugLog( ">>>>>> Standby Mode <<<<<<");
             if (outbuf[2] == 0x01)
            {
                //CNALComDestroy(dev);
                ms_open_nfc_standby_mode(dev);
				
            }
            else
            {
                PNALDebugLog( "[Standby Mode]wake up, send detect cmd");
				if (dev->bStandbyMode == W_TRUE)
			    {
			        PNALDebugLog("bStandbyMode=W_TRUE\n");
			        dev->bStandbyMode = W_FALSE;
			        MS_PowerOn();
			        dev->card_detect_mask = dev->temp_card_detect_mask;
			        dev->reader_detect_mask = dev->temp_reader_detect_mask;
			        ms_card_detection(dev);  
			    }
				//res = Reset_Write_Queue(dev);
            }
			CNALWriteDataFinish(dev);
            break;
        default:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_UNKNOWN_COMMAND;
			dev->nb_available_bytes = 2;
			PNALDebugLog( "wrong command code \n");
            break;
    }

	PNALDebugLog( "ms_open_nfc_admin_disp() - end \n");
    return retval;
}


#define TAG_EMU_BY_QUERY    0

#if( TAG_EMU_BY_QUERY == 1)
typedef enum {
   HF_State_Null,
   HF_State_Idle,
   HF_State_T_SDD,
   HF_State_R_SDD,
   HF_State_NFC_SDD,
   HF_State_T_DEP,
   HF_State_T_CLT,
   HF_State_R_DEP,
   HF_State_NFC_DEP,
   HF_State_HOST_INT,
   HF_State_Max
}HF_STATE;



int ms_tag_emulation_check_cb(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    if( len == 4 && inbuf[2] == 0x01){
        PNALDebugLog("[ms_tag_emulation_check_cb] state:%d", inbuf[2]);
        if( inbuf[3] == HF_State_T_DEP ||
            inbuf[3] == HF_State_T_CLT ){
            ms_tag_emulation_check(dev);    //shall keep waiting for tag emulation to be done.
        }else{
            ms_card_detection(dev);
        }
    }else{
        PNALDebugLog("[ms_tag_emulation_check_cb] err response for tag check cb");
        ms_card_detection(dev);
    }
    return 0;
}
#define TAG_EMULATION_PERIOD 300000
int ms_tag_emulation_check(tNALMsrComInstance  *dev)
{
    int length;

    PNALDebugLog("[A3_tag_emulation_check] sleep 200ms");
    usleep(TAG_EMULATION_PERIOD);     //sleep for 200ms
    //check tag emulation status, if tag emulation is started.  need to wait for more time.
    //A3 HF status check
    //|CMD(0x50)|01|idx|
    //idx: 0:SDD status, 1:HF_State
    dev->ant_send_buffer[0] = 0x50;
    dev->ant_send_buffer[1] = 0x01;
    dev->ant_send_buffer[2] = 0x01; //query HF_State

    /* Send command to Target */
    length = ms_interfaceSend(dev, 3);
    dev->cbRecv = (RecvCallBack *)ms_tag_emulation_check_cb;
    return length;
}

int ms_tag_emulation_cb(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_tag_emulation_cb]");
    if( len == 3 && inbuf[2] == 1){
        PNALDebugLog("[ms_tag_emulation_cb] tag emulation started.");
        ms_tag_emulation_check(dev);
    }else{
        PNALDebugLog("[ms_tag_emulation_cb] err response for tag emulation");
        ms_card_detection(dev);
    }
    return 0;
}

int ms_tag_emulation(tNALMsrComInstance  *dev)
{
    int length;

    PNALDebugLog("[A3_tag_emulation]\n");
    //A3 Tag emulation
    //|CMD(0x42)|02|80|03|
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x02;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x03;

    /* Send command to Target */
    length = ms_interfaceSend(dev, 4);
    dev->cbRecv = (RecvCallBack *)ms_tag_emulation_cb;
    return length;
}
#else   //ms_tag_emulation using timer instead of querying MSR3110
#define TAG_EMU_FIRST_INTERVAL  300000
#define TAG_EMU_CHECK_INTERVAL  50000
#define TAG_EMU_LAST_INTERVAL  20000
void ms_tag_emulation_handler(tNALMsrComInstance  *dev)
{
    if( MS_GetRFStatus() ){
        PNALDebugLog("RF detected");
        dev->WaitingPeriod = TAG_EMU_CHECK_INTERVAL;
        dev->cbWait = (WaitCallBack *)ms_tag_emulation_handler;
        sem_post(&dev->ant_rx_sem);
    }else{
        if (RFDetectSts != RF_DETECT_STS_LAST)
        {
            RFDetectSts= RF_DETECT_STS_LAST;
            PNALDebugLog("RF last detected");
            dev->WaitingPeriod = TAG_EMU_LAST_INTERVAL;
            dev->cbWait = (WaitCallBack *)ms_tag_emulation_handler;
            sem_post(&dev->ant_rx_sem);
        }
        else
        {
            PNALDebugLog("RF NOT detected");
            ms_card_detection(dev);
        }
    }

}
static int ms_chk_internal_error(unsigned char *inbuf, int len)
{
    if (len == 3 && inbuf[0]==0xAA && inbuf[2] == 0x00)
    {
        PNALDebugError("[ms_tag_emulation_cb]has ms_chk_internal_error\n");
        return 1;
    }
    else
    {
        return 0;
    }
}

int ms_tag_emulation_cb(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_tag_emulation_cb]");
    if( len == 3 && inbuf[2] == 1){
        PNALDebugLog("[ms_tag_emulation_cb] tag emulation started...");
        dev->WaitingPeriod = TAG_EMU_FIRST_INTERVAL;
        dev->cbWait = (WaitCallBack *)ms_tag_emulation_handler;
        sem_post(&dev->ant_rx_sem);
        //ms_tag_emulation_check(dev);
    }else{
        if (ms_chk_internal_error(inbuf, len) == 1)
        {
            PNALDebugLog("[ms_tag_emulation_cb]SPI Error, Stop detect:");
            //return 0;
        }
        PNALDebugLog("[ms_tag_emulation_cb] err response for tag emulation...");
        ms_card_detection(dev);
    }
    return 0;
}

int ms_tag_emulation(tNALMsrComInstance  *dev)
{
#if 1
    dev->WaitingPeriod = TAG_EMU_FIRST_INTERVAL;
    dev->cbWait = (WaitCallBack *)ms_tag_emulation_handler;
    sem_post(&dev->ant_rx_sem);
    RFDetectSts = RF_DETECT_STS_FIRST;
    PNALDebugLog("TAG_EMU Start");
#else
    int length;
    //A3 Tag emulation
    //|CMD(0x42)|02|80|03|
    dev->ant_send_buffer[0] = 0x42;
    dev->ant_send_buffer[1] = 0x02;
    dev->ant_send_buffer[2] = 0x80;
    dev->ant_send_buffer[3] = 0x03;
    PNALDebugLog("TAG_EMU Start");
    /* Send command to Target */
    length = ms_interfaceSend(dev, 4);
    dev->cbRecv = (RecvCallBack *)ms_tag_emulation_cb;
#endif

    return 1;
}
#endif

int ms_detection_polling_getinfo_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    unsigned char infoType;	
	PNALDebugLog("[ms_detection_polling_getinfo_callback]start, len = %d\n", len);

	if (len <= 0)
	{
        //PNALDebugLog("[ms_detection_polling_getinfo_callback]recvlen < 0");
		ms_detection_polling_req(dev);
		return 0;	
	}
	
    PNALDebugLog("[ms_detection_polling_getinfo_callback]inbuf:");
    PNALDebugLogBuffer(inbuf, len);
    
    //A3Rsp(1)[0x02] , Len(1)[n], SubFuncCode(1), ParamNum(1) [n1], ParaID(1)[0x03],ParamLen(1)[n1+1],ID(1), OriginalRsp(n1)
    //PS: ID format: DrvPL_Category_et | DrvPL_PLItem_et    
    if ((inbuf[4] != 0xA1) || (inbuf[5] <= 1)) 
    {
        PNALDebugLog("Queue data is empty, do detection.");
		ms_detection_polling_req(dev);
		return 0;
    }
	infoType = inbuf[6];
	PNALDebugError("SWP Reset Cnt= %d", dev->swp_reset_cnt);
	PNALDebugLog("Info Type= %x", infoType);
	switch(infoType)
	{
	    case DrvPL_PLItem_R_ISO43A:
			ms_A3_RFID_43A_Inventory_Callback(dev, inbuf+7, inbuf[5]-1);
			break;

	    case DrvPL_PLItem_R_ISO43B:
			ms_open_nfc_43b_inventory_cbrecv(dev, inbuf+7, inbuf[5]-1);
			break;			

		case DrvPL_PLItem_R_ISO693:
			ms_A3_RFID_693_Inventory_Callback(dev, inbuf+7, inbuf[5]-1); 
			break;

		case DrvPL_PLItem_R_Felica:
			ms_open_nfc_felica_Inventory_callback(dev, inbuf+7, inbuf[5]-1);
			break;

		case DrvPL_PLItem_R_Type1:
			ms_open_nfc_type1_detection_sens_new_callback(dev, inbuf+7, inbuf[5]-1);
			break;

	    case DrvPL_PLItem_PI_P2P_212:
			break;

		case DrvPL_PLItem_PI_P2P_424:
			ms_open_nfc_initiator_detection_212_424_callback(dev, inbuf+7, inbuf[5]-1);
			break;

		case DrvPL_PLItem_PT_P2P_106:
			break;

		case DrvPL_PLItem_PT_P2P_212:
			break;

		case DrvPL_PLItem_PT_P2P_424:
			dev->sendTargetSddEvt = W_FALSE;
	        dev->temp_target_buf_len = 0;
			ms_nfc_p2p_target_enable_callback(dev, inbuf+7, inbuf[5]-1);
			break;	

		case DrvPL_PLItem_C_ISO43A:
			PNALDebugLog("43A Card Mode Started.");
			Start_IRQ_Detect(dev);
			break;		

		case DrvPL_PLItem_C_ISO43B:
			PNALDebugLog("43B Card Mode Started.");
            Start_IRQ_Detect(dev);    
			break;	

		case DrvPL_PLItem_C_CONN:
			PNALDebugLog("EVT_CONNECTIVITION");
			Start_IRQ_Detect(dev); 
			break;

		case DrvPL_PLItem_C_TRAN:
			PNALDebugLog("EVT_TRANSACTION");
			Start_IRQ_Detect(dev); 
			break;			

		case DrvPL_PLItem_PIT_P2P_424:
			PNALDebugLog("P2P IT Mode Started.");
			if (inbuf[10] == DRVP2P_result_As_Initiator)
			{
                PNALDebugLog("P2P IT Mode Started: Initiator");
				ms_open_nfc_initiator_detection_212_424_callback(dev, inbuf+7, inbuf[5]-1);
			}
			else if (inbuf[10] == DRVP2P_result_As_Target)
			{
                PNALDebugLog("P2P IT Mode Started: Target");
				dev->sendTargetSddEvt = W_FALSE;
	        	dev->temp_target_buf_len = 0;
				ms_nfc_p2p_target_enable_callback(dev, inbuf+7, inbuf[5]-1);
			}
			else
			{
			    PNALDebugError("P2P IT Mode Report Error");
			}
			break;

		case DrvPL_PLItem_Reset:
			PNALDebugLog("Reader need to Reset");
			dev->swp_reset_cnt++;			
			I2C_Reset();
			ms_card_detection(dev);
			break;

		default:
			break;
	}
	PNALDebugLog("[ms_detection_polling_getinfo_callback]end");
	return 1;
}

int ms_detection_polling_getinfo(tNALMsrComInstance  *dev)
{
	int idx = 0;
	int length = 0;
    PNALDebugLog("[ms_detection_polling_getinfo]start...");			
	//A3Cmd(1)[0x4B], Len(1)[n],Body(n)
	dev->ant_send_buffer[idx++] = PL_CMD_CODE;
	dev->ant_send_buffer[idx++] = 0;	  //LEN
	
	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
	dev->ant_send_buffer[idx++] = DrvPL_FuncCode_GetInfo; //SubFuncCode.
	dev->ant_send_buffer[idx++] = 0x01; 	  //ParamNum.

	dev->ant_send_buffer[idx++] = DrvPL_ModeParam_GetInfo;
	dev->ant_send_buffer[idx++] = 2;
	dev->ant_send_buffer[idx++] = DrvPL_InfoID_EvtQueue;
	dev->ant_send_buffer[idx++] = 0x00;	 
	dev->ant_send_buffer[1] = idx - 2;	  //LEN
    length = ms_interfaceSend(dev, idx);
	dev->getIRQFlag = W_FALSE;
	if (length > 0){  			
		dev->cbRecv = (RecvCallBack *)ms_detection_polling_getinfo_callback;	
	}
	else
	{
	    PNALDebugLog("[ms_detection_polling_getinfo]IO is running,can't send");
		dev->getIRQFlag = W_TRUE;
	}
	PNALDebugLog("[ms_detection_polling_getinfo]end...");	
	return idx;
}

int ms_detection_polling_req_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_detection_polling_req_callback]start, len = %d\n", len);
	if (len <= 0)
	{
		ms_detection_polling_req(dev);
		return 0;	
	}	
	
    PNALDebugLog("[ms_detection_polling_req_callback]inbuf:");
    PNALDebugLogBuffer(inbuf, len);

    //A3Rsp(1)[0x02] , Len(1)[n], SubFuncCode(1), ParamNum(1) [n1] , ParaID(1)[0xA0],ParamLen(1)[n],Result1
    if (inbuf[6] != RFID_STS_SUCCESS)
    {
        PNALDebugLog("[ms_detection_polling_req_callback]Result: fail, ecode= %x\n", inbuf[6]);
		ms_detection_polling_req(dev);
		return 0;
    }
	
	if (DETECT_CMD_RPT == W_TRUE)
	{
        PNALDebugLog("[ms_detection_polling_req_callback]DETECT_CMD_RPT= true");
		DETECT_CMD_RPT = W_FALSE;
		dev->sendDetectCmdDone = W_TRUE;
		dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
        dev->rx_buffer[1] = NAL_RES_OK;
        dev->nb_available_bytes = 2;
	}
	else
	{
        PNALDebugLog("[ms_detection_polling_req_callback]DETECT_CMD_RPT= false");
        Start_IRQ_Detect(dev);   //wakeup irq thread   
	}	
	PNALDebugLog("[ms_detection_polling_req_callback]end");
	return 1;
}

 int ms_detection_polling_req(tNALMsrComInstance  *dev)
{
	int idx = 0;
	int length = 0;
	unsigned char extern_mask = 0x00;
	unsigned char card_mode_mask = 0x00;
	unsigned char run_mode = 0x00;
	PNALDebugLog("[ms_detection_polling_req]start...");
	//disabe watchdog
	Stop_Chk_Reader_Alive(dev);

	//A3Cmd(1)[0x4B], Len(1)[n],Body(n)
	dev->ant_send_buffer[idx++] = PL_CMD_CODE;
	dev->ant_send_buffer[idx++] = 0;	  //LEN
	
	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
	dev->ant_send_buffer[idx++] = DrvPL_FuncCode_Action; //SubFuncCode.
	dev->ant_send_buffer[idx++] = 0x03; 	  //ParamNum.
	/*
		Param Format
		ParaID(1)[0x01],ParamLen(1)[2+X],DrvPL_ConfigTableFlag_et (1),TotalBitNum (1)[0x40], Mask0,Mask2,¡K,Mask4
		PS mask is MSBfirst , Mask0 = 0x80 for turning on ID =0
		   Mask1 = 0x80, for turning on ID = 8
			Mask0, Mask1 is R
			Mask2, Mask3 is C
			Mask4 our defined
	*/	
	dev->ant_send_buffer[idx++] = DrvPL_ModeParam_ConfigMask;
	dev->ant_send_buffer[idx++] = 7;
	dev->ant_send_buffer[idx++] = DrvPL_InfoID_ConfigMask;
	dev->ant_send_buffer[idx++] = 40;
	dev->ant_send_buffer[idx++] = (dev->reader_detect_poily & dev->reader_detect_mask & 0x007F); //lo byte: disable P2P_I_106
	dev->ant_send_buffer[idx++] = ((dev->reader_detect_poily & dev->reader_detect_mask) >> 8) & 0x00FF; //hi byte
	card_mode_mask = ((dev->card_detect_poily | dev->card_detect_mask) & 0x007F); //lo byte: disable P2P_T_106
	card_mode_mask &= (NAL_PROTOCOL_CARD_ISO_14443_4_A | NAL_PROTOCOL_CARD_ISO_14443_4_B);  //card emulation: 43a_4:0x01, 43b_4:0x02
	dev->ant_send_buffer[idx++] = card_mode_mask; //lo byte: disable P2P_T_106
	dev->ant_send_buffer[idx++] = (dev->card_detect_mask >> 8) & 0x00FF; //hi byte	

	if ((dev->reader_detect_poily & dev->reader_detect_mask) & NAL_PROTOCOL_READER_P2P_INITIATOR)
 	{
 	    extern_mask|= DrvPL_P2P_Initiator_424;  //I_424
 	}      
 	if ((dev->card_detect_poily & dev->card_detect_mask) & NAL_PROTOCOL_CARD_P2P_TARGET)
 	{
 	    extern_mask|= DrvPL_P2P_Target_424;  //T_424
 	}	
	dev->ant_send_buffer[idx++] = extern_mask;
	
	//ParaID: DrvPL_ModeParam_RunAction(1)[0x02],ParamLen(1)[2],DrvPL_RunAction_et(1),Drv_RunMode_et(1)
	if ((dev->card_detect_mask == 0x0000) && (dev->reader_detect_mask == 0x0000))
	{
        run_mode = DrvPL_RunMode_Card;
	}
	else
	{
	    run_mode = (DrvPL_RunMode_Reader | DrvPL_RunMode_Card | DrvPL_RunMode_Target);// | DrvPL_RunMode_No_Random_Mode );
	}
	dev->ant_send_buffer[idx++] = DrvPL_ModeParam_RunAction;
	dev->ant_send_buffer[idx++] = 2;
	dev->ant_send_buffer[idx++] = DrvPL_RunAction_Enable;	
	dev->ant_send_buffer[idx++] = run_mode;
	
	dev->ant_send_buffer[idx++] = DrvPL_ModeParam_RunAction;
	dev->ant_send_buffer[idx++] = 2;
	dev->ant_send_buffer[idx++] = DrvPL_RunAction_Resume;	
	dev->ant_send_buffer[idx++] = run_mode;
	
	dev->ant_send_buffer[1] = idx - 2;	  //LEN
	dev->recvDetectMask = W_FALSE;
    /* Send command to Target */
    length = ms_interfaceSend(dev, idx);
    dev->cbRecv = (RecvCallBack *)ms_detection_polling_req_callback;	
	PNALDebugLog("[ms_detection_polling_req]end...");
	return idx;
}

int ms_card_detection(tNALMsrComInstance  *dev)
{
    int retval = 0;

#if (A3_PL_MODE == 0)
    PNALDebugLog( "[ms_card_detection]card_detect_mask=%x, reader_detect_mask=%x\n",
        dev->card_detect_mask, dev->reader_detect_mask);
    PNALDebugLog( "[ms_card_detection]CUR_CARD_TYPE=%x\n", CUR_CARD_TYPE);

    while(retval == 0){
        if( (dev->card_detect_mask ==0) && (dev->reader_detect_mask == 0)){
            return 0;
        }
        TargetRfParam = SDD_RF_PARAM_TAG;
        if (Extern_RF_Detect == 0x01)
        {
            PNALDebugLog( "[ms_card_detection]Extern RF Detect = W_TRUE");
            CUR_CARD_TYPE = 0x06;
            Extern_RF_Detect = 0x00;
            TargetRfParam |= SDD_EXTERN_RF_DETECT;
        }
        switch (CUR_CARD_TYPE)
        {
            case 0x00:
                if ( ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_3_A) == NAL_PROTOCOL_READER_ISO_14443_3_A)
                    || ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_4_A) == NAL_PROTOCOL_READER_ISO_14443_4_A))
                {
                    retval = ms_A3_RFID_43A_Inventory(dev, RF_PARAM_43A);
                }
                CUR_CARD_TYPE++;
                break;
            case 0x01:
                if (((dev->reader_detect_mask & NAL_PROTOCOL_READER_P2P_INITIATOR) == NAL_PROTOCOL_READER_P2P_INITIATOR))
                {
                    //retval = ms_A3_RFID_43A_Inventory(dev, RF_PARAM_PAS_I_106);
                    retval = ms_open_nfc_initiator_detection_212_424(dev);  //For 212/424 testing
                }
                CUR_CARD_TYPE++;
                break;
            case 0x02:
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_15693_3) == NAL_PROTOCOL_READER_ISO_15693_3)
                {
                    retval = ms_A3_RFID_693_Inventory(dev);
                }
                CUR_CARD_TYPE++;
                break;
    		case 0x03:
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_ISO_14443_4_B) == NAL_PROTOCOL_READER_ISO_14443_4_B)
                {
                    retval = A3_RFID_43B_Inventory(dev);
                }
                CUR_CARD_TYPE++;
                break;
    		case 0x04:
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_FELICA) == NAL_PROTOCOL_READER_FELICA)
                {
                    retval = ms_open_nfc_felica_Inventory(dev);
                }
                CUR_CARD_TYPE++;
                break;
		case 0x05:
                if ((dev->reader_detect_mask & NAL_PROTOCOL_READER_TYPE_1_CHIP) == NAL_PROTOCOL_READER_TYPE_1_CHIP)
                {
                    retval = ms_open_nfc_type1_detection(dev);
                }
                CUR_CARD_TYPE++;
                break;
		case 0x06:
                if ((dev->card_detect_mask & NAL_PROTOCOL_CARD_P2P_TARGET) == NAL_PROTOCOL_CARD_P2P_TARGET)
                {
                    //retval = ms_open_nfc_p2p_target_detection(dev, TargetRfParam);
			        g_P2pSpeedType = DRVP2P_sp424;
                    retval = ms_nfc_p2p_target_enable( dev, TargetRfParam);
                }
		        CUR_CARD_TYPE++;
		   break;
    		case 0x07:  //tag emulation period
		        //retval = ms_tag_emulation(dev); 	//loop in ms_tag_emulation and cb
		        //retval = ms_open_nfc_p2p_target_detection(dev, TargetRfParam);
				//usleep(1000000);     
                CUR_CARD_TYPE = 0x00;
                break;
            default:
                CUR_CARD_TYPE = 0x00;
                break;
        }
    }
#else   //for IRQ
    retval = ms_detection_polling_req(dev);
#endif
    return retval;
}

static int ms_open_nfc_admin_cmd_get_parameter(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	PNALDebugLog( "ms_open_nfc_admin_cmd_get_parameter() - start \n");

	int interfaceSendRet = 0;

	CUR_PARAM_CODE = outbuf[2];
	switch(CUR_PARAM_CODE)
	{
		case NAL_PAR_HARDWARE_INFO:
			PNALDebugLog( " get NAL_PAR_HARDWARE_INFO - start \n");

			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			memcpy( dev->rx_buffer + 2, CST_PARAM_HARDWARE_INFO, 0xFB);
			dev->nb_available_bytes = 1 + 1 + 0xFB;

			PNALDebugLog( " get NAL_PAR_HARDWARE_INFO - end \n");
			break;

		case NAL_PAR_PERSISTENT_POLICY:
			PNALDebugLog( " get NAL_PAR_PERSISTENT_POLICY - start \n");

			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			memcpy( dev->rx_buffer + 2, CST_PARAM_PERSISTENT_POLICY, 8);
			dev->nb_available_bytes = 1 + 1 + 8;

			PNALDebugLog( " get NAL_PAR_PERSISTENT_POLICY - end \n");
			break;

		case NAL_PAR_FIRMWARE_INFO:
			PNALDebugLog( " get NAL_PAR_FIRMWARE_INFO - start \n");

#if 0 //ALAN_TEST

			dev->ant_send_buffer[0] = 0x05;
			dev->ant_send_buffer[1] = 0x01;
			dev->ant_send_buffer[2] = 0x00;

			//if( ms_interfaceSend(dev, 3) != 0)
			interfaceSendRet = ms_interfaceSend(dev, 3);
			//{
			//	PNALDebugError( "interfaceSend failed!");
			//	return -ENOTCONN;
			//}
			dev->cbRecv = ms_open_nfc_admin_cbrecv;
			retval = interfaceSendRet;
#endif
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			memcpy( dev->rx_buffer + 2, CST_PARAM_FIRMWARE_INFO, 47);
			dev->nb_available_bytes = 2 + 47;

			PNALDebugLog( " get NAL_PAR_FIRMWARE_INFO - end \n");
			break;

		case NAL_PAR_PERSISTENT_MEMORY:
			PNALDebugLog( " get NAL_PAR_PERSISTENT_MEMORY - start \n");

			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			memcpy( dev->rx_buffer + 2, CST_PARAM_PERSISTENT_MEMORY, 8);
			dev->nb_available_bytes = 1 + 1 + 8;
			retval = 0;

			PNALDebugLog( " get NAL_PAR_PERSISTENT_MEMORY - end \n");
			break;

		default:
			PNALDebugError( " get DEFAULT - start \n");
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_UNKNOWN_PARAM;
			dev->nb_available_bytes = 2;

			PNALDebugError( " get DEFAULT - end \n");
			break;
	}

	PNALDebugLog( "ms_open_nfc_admin_cmd_get_parameter() - end \n");
	return retval;
}

#if 0
static int ms_set_pl_config(tNALMsrComInstance  *dev)
{
    int length = 0;
	int offset = 0;
	//A3Cmd(1)[0x4B], Len(1)[n],Body(n)
	dev->ant_send_buffer[0] = PL_CMD_CODE;
	dev->ant_send_buffer[1] = 0;	  //LEN
	
	//Cmd Body Format
	//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
	dev->ant_send_buffer[2] = DrvPL_FuncCode_Config; //SubFuncCode.
	dev->ant_send_buffer[3] = 0x03; 	  //ParamNum.
/*	
	ParaID(1)[0x00],ParamLen(1)[n1],
	DrvPL_ConfigTableFlag_et (1),PL_CmdCount(1)(n2), [{XXX} * n2]
	
	{XXX} format
	ID(1), CmdLen(1)[n3],CmdBuf(n3)
	
	ID format
	DrvPL_Category_et | ID
*/
	dev->ant_send_buffer[4] = DrvPL_ModeParam_ConfigTable;
	dev->ant_send_buffer[5] = 0x04;		
	dev->ant_send_buffer[6] = DrvPL_Category_Reader | PL_ISO14443A;
	dev->ant_send_buffer[7] = sizeof(Config_43a);	
	memcpy( dev->rx_buffer + 8, Config_43a, sizeof(Config_43a));
    offset = 8 + sizeof(Config_43a);
	
    dev->ant_send_buffer[offset++] = DrvPL_Category_Reader | PL_ISO15693;
	dev->ant_send_buffer[offset++] = sizeof(Config_693);
	memcpy( dev->rx_buffer + offset, Config_693, sizeof(Config_693));
	offset += sizeof(Config_693);
	
    dev->ant_send_buffer[offset++] = DrvPL_Category_Reader | PL_Felica;
	dev->ant_send_buffer[offset++] = sizeof(Config_Felica);
	memcpy( dev->rx_buffer + offset, Config_Felica, sizeof(Config_Felica));
	offset += sizeof(Config_Felica);

	dev->ant_send_buffer[offset++] = DrvPL_Category_Reader | PL_Type1;
	dev->ant_send_buffer[offset++] = sizeof(Config_Type1);
	memcpy( dev->rx_buffer + offset, Config_Type1, sizeof(Config_Type1));
	offset += sizeof(Config_Type1);
	
    dev->ant_send_buffer[1] = offset;	  //LEN
	
}
#endif

static int ms_open_nfc_admin_cmd_set_parameter(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;
	unsigned char paramcode;

	paramcode = outbuf[2];
    PNALDebugLog( "ms_open_nfc_admin_cmd_set_parameter() - paramcode= %d \n", paramcode);
	switch(paramcode)
	{
		case NAL_PAR_DETECT_PULSE:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			dev->nb_available_bytes = 2;

			break;

		case NAL_PAR_POLICY:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_OK;
			dev->nb_available_bytes = 2;

			break;

		case NAL_PAR_PERSISTENT_MEMORY:
            dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
            dev->rx_buffer[1] = NAL_RES_OK;
            dev->nb_available_bytes = 2;
			break;

		case NAL_PAR_PERSISTENT_POLICY:
            ms_open_nfc_admin_cmd_persistent_policy(dev, outbuf, len);		
			break;			

		default:
			dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
			dev->rx_buffer[1] = NAL_RES_UNKNOWN_PARAM;
			dev->nb_available_bytes = 2;

			break;
	}
	return retval;
}

int ms_open_nfc_admin_cmd_persistent_policy_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_open_nfc_admin_cmd_persistent_policy_callback]start, len = %d\n", len);
	
	dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
	dev->rx_buffer[1] = NAL_RES_OK;
	dev->nb_available_bytes = 2;

	PNALDebugLog("[ms_open_nfc_admin_cmd_persistent_policy_callback]end");
	return 1;
}

static int ms_open_nfc_admin_cmd_persistent_policy(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int idx = 0;
	int length = 0;
	dev->card_detect_poily = ((outbuf[3] <<8) & 0xFF00) | (outbuf[4] & 0x00FF);
	dev->reader_detect_poily = ((outbuf[5] <<8) & 0xFF00) | (outbuf[6] & 0x00FF);
	PNALDebugLog( "[ms_open_nfc_admin_cmd_persistent_policy] Card poliy %04x, Read poily %04x\n", 
				 dev->card_detect_poily, dev->reader_detect_poily); 	

	if ((dev->temp_card_detect_poily & 0x007F)!= (dev->card_detect_poily & 0x007F))
	{
		PNALDebugLog("[ms_open_nfc_admin_cmd_persistent_policy]write flash");
        MS_PowerOn();
		//A3Cmd(1)[0x4B], Len(1)[n],Body(n)
		dev->ant_send_buffer[idx++] = PL_CMD_CODE;
		dev->ant_send_buffer[idx++] = 0;	  //LEN
		
		//Cmd Body Format
		//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
		dev->ant_send_buffer[idx++] = DrvPL_FuncCode_Action; //SubFuncCode.
		dev->ant_send_buffer[idx++] = 3; 	  //ParamNum

		dev->ant_send_buffer[idx++] = DrvPL_ModeParam_ConfigMask;
		dev->ant_send_buffer[idx++] = 7;
		dev->ant_send_buffer[idx++] = DrvPL_InfoID_ConfigMask;
		dev->ant_send_buffer[idx++] = 40;
		dev->ant_send_buffer[idx++] = 0x00;
		dev->ant_send_buffer[idx++] = 0x00;
		dev->ant_send_buffer[idx++] = (NAL_PROTOCOL_CARD_ISO_14443_4_A | NAL_PROTOCOL_CARD_ISO_14443_4_B);	//card emulation: 43a_4:0x01, 43b_4:0x02
		dev->ant_send_buffer[idx++] = 0x00; //hi byte	
		dev->ant_send_buffer[idx++] = 0x00;

		dev->ant_send_buffer[idx++] = DrvPL_ModeParam_RunAction;
		dev->ant_send_buffer[idx++] = 2;
        if ((dev->card_detect_poily & 0x007F) == 0)
        {
            dev->ant_send_buffer[idx++] = DrvPL_RunAction_Disable;
			dev->ant_send_buffer[idx++] = 0x00;	
        }
		else
		{
		    dev->ant_send_buffer[idx++] = DrvPL_RunAction_Enable;
			dev->ant_send_buffer[idx++] = DrvPL_RunMode_Card;	
		}
				
		dev->ant_send_buffer[idx++] = DrvPL_ModeParam_FlashOp;
		dev->ant_send_buffer[idx++] = 1;	//LEN	
		dev->ant_send_buffer[idx++] = DrvPL_FlashAction_Write;

		dev->ant_send_buffer[1] = idx - 2;	  //LEN
	    /* Send command to Target */
	    length = ms_interfaceSend(dev, idx);
	    dev->cbRecv = (RecvCallBack *)ms_open_nfc_admin_cmd_persistent_policy_callback;			
	}
	else
	{
        PNALDebugLog("[ms_open_nfc_admin_cmd_persistent_policy]no need to write flash");
		dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
	    dev->rx_buffer[1] = NAL_RES_OK;
	    dev->nb_available_bytes = 2;
	}
	dev->temp_card_detect_poily = dev->card_detect_poily;
	dev->temp_reader_detect_poily = dev->reader_detect_poily;
	PNALDebugLog("[ms_open_nfc_admin_cmd_persistent_policy]end");
	return idx;
}

static int ms_open_nfc_admin_cmd_update_firmware(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;
    dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
    dev->rx_buffer[1] = NAL_RES_OK;
    dev->nb_available_bytes = 2;
	return retval;
}

static int ms_open_nfc_admin_cmd_detection(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    PNALDebugLog("[ms_open_nfc_admin_cmd_detection]start...\n");
    int retval = 0;
		
    dev->card_detect_mask = ((outbuf[2] <<8) & 0xFF00) | (outbuf[3] & 0x00FF);
    dev->reader_detect_mask= ((outbuf[4] <<8) & 0xFF00) | (outbuf[5] & 0x00FF);
    PNALDebugLog("[ms_open_nfc_admin_cmd_detection]Card_detect_mask = %04x\n", dev->card_detect_mask);
    PNALDebugLog("[ms_open_nfc_admin_cmd_detection]reader_detect_mask = %04x\n", dev->reader_detect_mask);
#if (A3_PL_MODE == 0)	
    dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
    dev->rx_buffer[1] = NAL_RES_OK;
    dev->nb_available_bytes = 2;
	if( dev->ant_recv_started == W_FALSE) //do actual detect only when it is not running
	{
		PNALDebugLog("[ms_open_nfc_admin_cmd_detection]dev->ant_recv_started = W_FALSE\n");
		ms_card_detection(dev);
	}	
#else	    
    if (dev->bStandbyMode == W_TRUE)
    {
        PNALDebugLog("[ms_open_nfc_admin_cmd_detection]In standbymode\n");
		dev->temp_card_detect_mask = dev->card_detect_mask;
		dev->temp_reader_detect_mask = dev->reader_detect_mask;
        dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
        dev->rx_buffer[1] = NAL_RES_OK;
        dev->nb_available_bytes = 2;	
		return retval;
    }
    DETECT_CMD_RPT = W_TRUE;

	if( dev->ant_recv_started == W_FALSE) //do actual detect only when it is not running
	{
		PNALDebugLog("[ms_open_nfc_admin_cmd_detection]dev->ant_recv_started = W_FALSE\n");
		ms_card_detection(dev);
	}
	else
	{

		PNALDebugLog("[ms_open_nfc_admin_cmd_detection]dev->ant_recv_started = W_TRUE\n");
		dev->recvDetectMask = W_TRUE;
	}    
	
#endif
    PNALDebugLog("[ms_open_nfc_admin_cmd_detection]end...\n");
    return retval;
}

static int ms_open_nfc_admin_cmd_production_test(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}

static int ms_open_nfc_admin_cmd_self_test(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}

int ms_open_nfc_admin_ms_reader_test_cbrecv(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    PNALDebugLog("[ms_open_nfc_admin_ms_reader_test_cbrecv]start...\n");
    PNALDebugLog("callback recvlen:0x%02X\n", len);
    dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
    dev->rx_buffer[1] = NAL_RES_OK;
    memcpy(&dev->rx_buffer[2], inbuf, len);
    dev->nb_available_bytes = 1 + 1+ len;
    PNALDebugLog("[ms_open_nfc_admin_ms_reader_test_cbrecv]end...\n");
    return 0;
}

static int ms_open_nfc_admin_ms_reader_test(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;
      PNALDebugLog("[ms_open_nfc_admin_ms_reader_test]start...\n");
      memcpy(dev->ant_send_buffer, &outbuf[2], len-2);
      retval = ms_interfaceSend(dev, len-2);
      dev->cbRecv = (RecvCallBack *)ms_open_nfc_admin_ms_reader_test_cbrecv;
      PNALDebugLog("[ms_open_nfc_admin_ms_reader_test]end...\n");
	return retval;
}

static int ms_open_nfc_admin_evt_rf_field(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}

static int ms_open_nfc_admin_evt_standby_mode(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}

static int ms_open_nfc_admin_evt_nfcc_error(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}

static int ms_open_nfc_admin_evt_raw_message(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
	int retval = 0;

	return retval;
}

static int ms_open_nfc_standby_mode(tNALMsrComInstance  *dev)
{
    PNALDebugLog("[ms_open_nfc_standby_mode]start...\n");
    dev->temp_card_detect_mask = dev->card_detect_mask;
    dev->temp_reader_detect_mask = dev->reader_detect_mask;
    dev->card_detect_mask = 0;
    dev->reader_detect_mask = 0;
    //ms_swp_write_reg(dev);
	
    //Reset_Write_Queue(dev);
    dev->bStandbyMode = W_TRUE;
    Stop_IRQ_Detect(dev);
    MS_PowerOff();
    return 0;
}

int ms_swp_start_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    unsigned char infoType;	
	PNALDebugLog("[ms_swp_start_callback]start, len = %d\n", len);
	if (dev->curCmdCode == NAL_EVT_STANDBY_MODE)
	{
        PNALDebugLog("[ms_swp_start_callback]standby_mode");
		Reset_Write_Queue(dev);
		dev->bStandbyMode = W_TRUE;
		MS_PowerOff();
	}
	else
	{
        PNALDebugLog("[ms_swp_start_callback]UICC_START_SWP");
		PNALDebugLog( "%s SLEEP", __FUNCTION__);
		usleep( 400000);

		dev->rx_buffer[0] = dev->curSvcCode;
	    dev->rx_buffer[1] = NAL_RES_OK;
	    dev->nb_available_bytes = 2;	
	}
	
	if (len <= 0)
	{
		return 0;	
	}	
    PNALDebugLog("[ms_swp_start_callback]inbuf:");
    PNALDebugLogBuffer(inbuf, len);
    return 1;
}

static int ms_swp_write_reg(tNALMsrComInstance  *dev)
{
    int idx = 0;
	int length = 0;
	PNALDebugLog("[ms_swp_write_reg]start..."); 	
	//write SWP Reg
	dev->ant_send_buffer[idx++] = 0x0B; 
	dev->ant_send_buffer[idx++] = 0x02; 	  
	dev->ant_send_buffer[idx++] = 0x0A;
	dev->ant_send_buffer[idx++] = 0x03;
    length = ms_interfaceSend(dev, idx);
	if (length > 0)
	{
	    dev->cbRecv = (RecvCallBack *)ms_swp_start_callback;
    }
	else
	{
	    PNALDebugLog("[ms_swp_start_callback]only return RES_OK");
		dev->rx_buffer[0] = dev->curSvcCode;
	    dev->rx_buffer[1] = NAL_RES_OK;
	    dev->nb_available_bytes = 2;
	}
	PNALDebugLog("[ms_swp_write_reg]end...");	
	return 1;
}

int ms_open_nfc_uicc_disp(tNALMsrComInstance  *dev, unsigned char *outbuf, int len)
{
    int retval = 0;
	
	int length = 0;
	PNALDebugLog( "ms_open_nfc_uicc_disp() - start \n");

    if (outbuf == NULL || len == 0)
    {
    	PNALDebugError( "recieve buffer: NULL \n");
		PNALDebugLog( "ms_open_nfc_uicc_disp() - end \n");
        return -1;
    }
    PNALDebugLog( "[ms_open_nfc_uicc_disp]dev->curCmdCode:0x%02X\n", dev->curCmdCode);
    switch( dev->curCmdCode)
    {
        case NAL_CMD_UICC_START_SWP:
			PNALDebugLog("[NAL_CMD_UICC_START_SWP]start..."); 		
#if 0			
			//A3Cmd(1)[0x4B], Len(1)[n],Body(n)
			dev->ant_send_buffer[idx++] = PL_CMD_CODE;
			dev->ant_send_buffer[idx++] = 0;	  //LEN
			
			//Cmd Body Format
			//SubFuncCode(1), ParamNum(1) [n1] , Param_1(n2) ,	¡K , Param_n1(n3)
			dev->ant_send_buffer[idx++] = DrvPL_FuncCode_GetInfo; //SubFuncCode.
			dev->ant_send_buffer[idx++] = 0x01; 	  //ParamNum.
			
			dev->ant_send_buffer[idx++] = DrvPL_ModeParam_GetInfo;
			dev->ant_send_buffer[idx++] = 2;
			dev->ant_send_buffer[idx++] = 0x03;
			dev->ant_send_buffer[idx++] = 0x00;  
			dev->ant_send_buffer[1] = idx - 2;	  //LEN
			length = ms_interfaceSend(dev, idx);		
			dev->cbRecv = (RecvCallBack *)ms_swp_start_callback;	
			PNALDebugLog("[NAL_CMD_UICC_START_SWP]end...");				
#else
			MS_PowerOn();
			PNALDebugLog( "%s SLEEP-1", __FUNCTION__);
			usleep( 500000);
			PNALDebugLog( "%s SLEEP-2", __FUNCTION__);
			usleep( 500000);
			PNALDebugLog( "%s SLEEP-3", __FUNCTION__);
			usleep( 500000);
			PNALDebugLog( "%s SLEEP-END", __FUNCTION__);

            ms_swp_write_reg(dev);
#endif
            PNALDebugLog("[NAL_CMD_UICC_START_SWP]end..."); 	
            break;

        default:
			dev->rx_buffer[0] = dev->curSvcCode;
			dev->rx_buffer[1] = NAL_RES_OK;

			dev->nb_available_bytes = 2;
			PNALDebugLog( "wrong command code \n");
            break;
    }

	PNALDebugLog( "ms_open_nfc_admin_disp() - end \n");
    return retval;
}





