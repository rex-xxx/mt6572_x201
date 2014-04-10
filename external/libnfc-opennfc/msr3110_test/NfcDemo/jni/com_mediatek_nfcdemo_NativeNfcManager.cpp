#include <stdlib.h>
#include <stdio.h>
#include <getopt.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <pthread.h>
#include <ctype.h>
#include <semaphore.h>

#include "com_mediatek_nfcdemo.h"

static jmethodID com_mediatek_nfcdemo_NativeNfcManager_detectCard;

namespace android {

int msr3110_fd = -1;
int isCancelIRQ = -1;

sem_t g_irq_sem;
pthread_t hDevIRQThread = {0};
RecvIRQCallBack *IRQCallBack;

jobject g_thiz;


int nfc_chk_dev_IRQ(int fd)
{
    int res = 0;
    int val = 1;
    if(fd < 0) {
        LOGD("%s: fd <= 0", __FUNCTION__);
        return res;
    }
    res = ioctl( fd, MSR3110_IOCTL_IRQ, &val);
    return res;
}

int nfc_cancel_dev_IRQ(int fd)
{
    int res = -1;
    int val = 1;
    if(fd < 0) {
        LOGD("%s: fd <= 0", __FUNCTION__);
        return res;
    }
    res = ioctl( fd, MSR3110_IOCTL_IRQ_ABORT, &val);
    return res;
}

void nfc_msr3110_print_buf(const char *message ,unsigned char *print_buf, int length)
{
    //char temp[500] = {0};
    char * temp = new char[ (length + 1)*3];
    char ascii_chars[] = "0123456789ABCDEF";
    int i, j;
    
    for(i=0, j=0; i < length; i++) {
        temp[j++] = ascii_chars[ ( print_buf[i] >> 4 ) & 0x0F ];
        temp[j++] = ascii_chars[ print_buf[i] & 0x0F ];
        temp[j++] = ' ';
    }
    temp[j] = '\0';
    LOGD("%s : %s", message, temp);
    delete [] temp;
}

static void * static_dev_IRQ_thread(void *arg)
{
    int irqSts = 0;
    int pinVal = 0;
    while(1){
        LOGD("++[wait irq_sem]");
        sem_wait(&g_irq_sem);          
        LOGD("++[check IRQ start]");
        irqSts = nfc_chk_dev_IRQ( msr3110_fd);     
        LOGD("++[check IRQ end]");
        if ( isCancelIRQ == 1) {
            LOGD("Cancel : do not run callback.");
            isCancelIRQ = -1;
            break;
        }
        if ( irqSts == 1){
            IRQCallBack();
            break;
        }
    }
    return NULL;
} 

unsigned char *reverse_array(unsigned char *src, unsigned short int tail) 
{ 
    unsigned short int head = 0; 
    unsigned char swap; 
      
    for(head ; head < --tail ;head++ ) { 
        swap = src[head];       
        src[head] = src[tail];    
        src[tail] = swap;       
    }
    return src;    
} 

unsigned short crc16( CRCTypeID type, unsigned short len, unsigned char *buf )
{
    unsigned short i, j, crc = 0;
    unsigned short POLYNOMIAL;

    if( type >= MAX_CRC_TYPE ) {
        return 1;
    }

    if( type == CRC_ISO_18092_248 || type == CRC_EPC_C1G2 || type == CRC_HWIF || type == CRC_HWIF_SDIO ) {
        POLYNOMIAL = 0x1021;
        if( type == CRC_EPC_C1G2 || type == CRC_HWIF ) {
            crc = 0xFFFF;
        } else {
            crc = 0x0000;
        }

        for( i=0; i<len; i++ ) {
            crc=(crc)^(((unsigned short)(*buf))<<8);
            for( j=0; j<8; j++ ) {
                crc=(crc&0x8000)?((crc<<1)^POLYNOMIAL):(crc<<1);
            }
            buf++;
        }
    } else {
        if( type == CRC_ISO_15693 || type == CRC_ISO_14443B ) {
            crc = 0xFFFF;
        } else if( type == CRC_ISO_14443A || type == CRC_ISO_18092_106 ) {
            crc = 0x6363;
        }
        POLYNOMIAL = 0x8408;
        
        for( i=0; i<len; i++ ) {
            crc=(crc)^((unsigned short)(*buf));
            for( j = 0; j < 8; j++ ) {
                crc=(crc&0x0001)?((crc>>1)^POLYNOMIAL):(crc>>1);
            }
            buf++;
        }

        if( type == CRC_ISO_15693 || type == CRC_ISO_14443B ) {
            crc=~(crc);
        }
    }
    return crc;
}

int crc16_check( CRCTypeID CrcTypeId, unsigned char *pDataContent, int DataContentLen)
{
    LOGD( "ms_crc16_chk() - start ");
	int retVal = 0;
	unsigned short crcDataContent;	//CRC16 in DataContent;
	unsigned short crcCal;			//Calculated CRC16 of DataContent

    nfc_msr3110_print_buf("crc16_chk", pDataContent, DataContentLen);
    switch( CrcTypeId) {
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
	crcCal = crc16( CrcTypeId, DataContentLen - 2, pDataContent);

	if( crcDataContent != crcCal) {
		retVal = -1;
	}
	LOGD( "CRC: 0x %X ", crcDataContent);
	LOGD( "CRC: 0x %X ", crcCal);
	LOGD( "ms_crc16_chk() - end ");
	return retVal;
}

bool check_checksum(unsigned char *buf, unsigned short length )
{
    int i = 0;
    unsigned short sum1 = 0;
    unsigned short sum2 = 0;

    for( i=0; i< length; i++ ) {
        sum1 += *( buf + i );
    }

    sum2 = *(buf + length + 1 );
    sum2 <<= 8;
    sum2 += *(buf + length);

    return (sum1 == sum2);
}

static int nfc_msr3110_interface_send_fd( int fd, unsigned char *SendBuf, int SendLen)
{
    int retVal = 0;
    int loopIdx = 0;
    for( loopIdx =0; loopIdx < 100; loopIdx++) {
        retVal = write( fd, SendBuf, SendLen);  
        if( retVal == SendLen ) {
            LOGD( "SEND SUCCESS: read len. \n");
            break;
        }
        if( retVal < 0) {
            LOGD( "SEND CONTINUE. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
            usleep(500);
            continue;       
        }
        LOGE( "SEND ERROR: read len. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
        retVal = -1;
        goto end;
    }
    
end:
    return retVal;  
}
    
static int nfc_msr3110_interface_recv_fd( int fd, unsigned char *RecvBuf, int RecvBufLen)
{
    int retVal = 0;
    int loopIdx = 0;
  
    for( loopIdx = 0; loopIdx < 100; loopIdx++) {
        retVal = read( fd, RecvBuf, 2);
        if( retVal == 2) {
            LOGD( "RECV SUCCESS: read len. \n");
            nfc_msr3110_print_buf( "[read length]:", RecvBuf, 2);
            break;
        }
  
        if( retVal < 0) {
            LOGD( "RECV CONTINUE. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
            usleep(500);
            continue;       
        }
        LOGE( "RECV ERROR: read len. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
        retVal = -1;
        goto end;
    }
    if( retVal < 0) {
        LOGE( "RECV ERROR: read len, out of retry. retVal: %d.\n", retVal);
        retVal = -1;
        goto end;
    }
      
    if( RecvBuf[ 0] != 0x02)
    {
        LOGE( "RECV ERROR: RecvBuf[ 0]: %02X != 0x02.\n", RecvBuf[ 0]);
        retVal = -2;
        goto end;       
    }
  
    LOGD( "RecvBuf, len: %d. \n", RecvBuf[1]);
    if( RecvBuf[1] <= 0) {
        LOGE( "RECV ERROR: len <= 0. \n");
        retVal = -1;
        goto end;
    }
  
    for( loopIdx = 0; loopIdx < 100; loopIdx++) {
        retVal = read( fd, &RecvBuf[2], RecvBuf[1] + 2);
        if( retVal == ( RecvBuf[1] + 2)) {
            LOGD( "RECV SUCCESS: read data. \n");
            nfc_msr3110_print_buf( "[read buff]:", RecvBuf, RecvBuf[1] + 4);
            break;
        }
  
        if( retVal < 0) {
            LOGD( "RECV CONTINUE. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
            usleep(500);
            continue;       
        }
        LOGE( "RECV ERROR: read data. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
        retVal = -1;
        goto end;
    }
    if( retVal < 0) {
        LOGE( "RECV ERROR: read len out of retry. retVal: %d.\n", retVal);
        retVal = -1;
        goto end;
    }
    retVal = RecvBuf[1] + 4;
      
end: 
    return retVal;
}

void nfc_msr3110_polling_getinfo( void)
{
    JavaVM * javaVM = getJavaVM();
    JNIEnv * env = NULL;
    jclass cls;
    jfieldID fid;
    jbyteArray byteUID;

    int retVAl = 0;
    int uid_length = 0;
    int cmd_length = 0;
    int sdd_length = 0;
    unsigned short rcv_crc = 0;
    unsigned short cal_crc = 0;
    unsigned char cmd_getinfo[] = { 0x4b, 0x06, 0x02, 0x01, 0x03, 0x02, 0x02, 0x00, 0x5b, 0x00 };
    unsigned char response_buffer[MSR3110_READ_BUF_SIZE_MAX] = {0};
    unsigned char infoType;
    unsigned char uid[10] = {0};
    bool attached = false;

    //------------------Send getinfo command ------------------------------------------//
    nfc_msr3110_print_buf( "write buff", cmd_getinfo, sizeof( cmd_getinfo));
    retVAl = nfc_msr3110_interface_send_fd( msr3110_fd, cmd_getinfo, sizeof(cmd_getinfo));
    if( retVAl < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVAl);
        goto end;
    }    
    
    usleep( 50000);
    
    retVAl = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVAl < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVAl); 
        goto end;
    } 
    
    memset(uid, 0, 10);  //reset
    
    //------------------ Parse info ---------------------------------------------------//
    if ((response_buffer[4] != 0xA1) || (response_buffer[5] <= 1)) {
        LOGD("Queue data is empty, do detection.");
		goto end;
    }    
    infoType = response_buffer[6];
    switch(infoType) {
    case DrvPL_PLItem_R_ISO43A:
        uid_length = response_buffer[ 7 + 4 ];  //length
        LOGD(" uid length = %d \n", uid_length);
        memcpy( uid, response_buffer + 7 + 14, uid_length);
        nfc_msr3110_print_buf( "[43a uid]", uid, uid_length);
        break;
        
    case DrvPL_PLItem_R_ISO693:
        uid_length = 8;
        memcpy( uid, response_buffer + 7 + 4 + 2, uid_length);
        nfc_msr3110_print_buf( "[693 uid] [before]:", uid, uid_length);
        reverse_array(uid, uid_length);
        nfc_msr3110_print_buf( "[693 uid] [after]:", uid, uid_length);       
        break;
        
    case DrvPL_PLItem_R_Felica:
        uid_length = 8;     
        memcpy( uid, response_buffer + 7 + 5, uid_length);
        nfc_msr3110_print_buf( "[felica uid]", uid, uid_length);
        break;
        
    case DrvPL_PLItem_R_Type1:
        uid_length = 4;
        memcpy( uid, response_buffer + 7 + 12, 4);
        nfc_msr3110_print_buf( "[Type1 uid] [before]:", uid, uid_length);       
        break;

    case DrvPL_PLItem_PI_P2P_106:
    case DrvPL_PLItem_PI_P2P_212:
    case DrvPL_PLItem_PI_P2P_424:
        //if( (response_buffer[2] == 0xFF) || (response_buffer[3] != 0x00)) {
        //    LOGD("ERROR response");          
        //    goto end;        
        //}
        sdd_length = response_buffer[7 + 5];
        uid_length = response_buffer[7 + 5 + sdd_length + 2];
        //check CRC
        cmd_length = response_buffer[7 + 5 + sdd_length + 3];
        rcv_crc = (response_buffer[7 + 5 + sdd_length + 3 + cmd_length + 1]) | (response_buffer[7 + 5 + sdd_length + 3 + cmd_length ]<<8);
        cal_crc = crc16( CRC_ISO_18092_248,  cmd_length, response_buffer + 7 + 5 + sdd_length + 3);
        if( rcv_crc != cal_crc) {
            LOGD("ERROR CRC");           
            goto end;            
        }
        if (response_buffer[ 7 + 5 + sdd_length + 2 + 2]!=0xD5 || response_buffer[7 + 5 + sdd_length + 2 + 3] != 0x01) {
            LOGD("ATR_RES CMD_0/CMD_1 Error, CMD_0=%x, CMD_1=%x", response_buffer[7 + 7], response_buffer[7 + 8]);            
            goto end;            
        }
        uid_length -= 20;
        memcpy( uid, response_buffer + 7 + 46, uid_length);
              
        break;

    case DrvPL_PLItem_PT_P2P_106:
    case DrvPL_PLItem_PT_P2P_212:
    case DrvPL_PLItem_PT_P2P_424:
        if( response_buffer[7 + 3] != 0x01) {
            LOGD("ERROR response");
            goto end;        
        }
        //check CRC
        if (crc16_check( CRC_ISO_18092_248, response_buffer + 7 + 6, response_buffer[7 + 5]) != 0) {
            LOGD("ERROR CRC");
            goto end;             
        }      
        uid_length = response_buffer[7 + 6] - 1 - 16;
        memcpy( uid, response_buffer + 7 + 23, uid_length);
        nfc_msr3110_print_buf( "[P2P-T]:", uid, uid_length);     
        break;        
        
    default:
        LOGD( "%s: no support type", __FUNCTION__);
        break;
    }
   
end:
    if (javaVM == NULL) {
        LOGD("Java VM is NULL");
        return;
    }
    javaVM->GetEnv( (void **)&env, JNI_VERSION_1_6);
    if (env == NULL) {
        attached = true;
        JavaVMAttachArgs thread_args;
        thread_args.name = "nfc_msr3110_polling_getinfo";
        thread_args.version = JNI_VERSION_1_6;
        thread_args.group = NULL;
        javaVM->AttachCurrentThread(&env, &thread_args);
    }
    
    if (env != NULL) {
        cls = env->GetObjectClass(g_thiz);
        if (cls == NULL) {
            LOGD("failed to find the object");
            return;
        }
        // get mLength
        fid = env->GetFieldID(cls, "mLength", "I");
        if (fid == NULL) {
            LOGD("failed to find the field");
            return;
        }
        env->SetIntField(g_thiz, fid, uid_length);
        
        //get mUid
    	fid = env->GetFieldID(cls, "mUid", "[B");
    	if (fid == NULL) {
    		LOGD("failed to find the field");
    		return;
    	}

        byteUID = (jbyteArray) env->GetObjectField(g_thiz, fid);
    	if (byteUID == NULL) {
    		LOGD("failed to find the g_byteUID");
            return;
    	}        
	    env->SetByteArrayRegion(byteUID, 0, uid_length, (jbyte *)uid);
        // call java method from native c++
        env->CallVoidMethod( g_thiz, com_mediatek_nfcdemo_NativeNfcManager_detectCard);
    }

    if (attached == true) {
        javaVM->DetachCurrentThread();
    }
   
	byteUID = NULL;
    g_thiz = NULL;    
    env = NULL;
    return;

}

static jint com_mediatek_nfcdemo_NfcManager_initialize(JNIEnv *env, jobject thiz)
{
    LOGD("com_mediatek_nfcdemo_NfcManager_initialize");
    int pinVal = 0;
    int retVal = 0;
    jclass cls;
    // -------------  INIT --------------------------------//
    LOGD( "------ init ------\n");
    if ( msr3110_fd > 0) {
        close( msr3110_fd);
    }
    msr3110_fd = open( MSR3110_DEV_NAME, O_RDWR);
    if( msr3110_fd < 0) {
        LOGD( "Can not open MSR3110\n");
        retVal = -1;
        goto end;
    }

    //-------------Reset MSR3110 --------------------------//
    // Power on
    //pinVal = 1;
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
    //pinVal = 0;
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    //usleep( 100000);
    //Power off
    //pinVal = 1;
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    //usleep( 500);
    //pinVal = 0;
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    //usleep( 100000);
    //Power on
    pinVal = 1;
    ioctl( msr3110_fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
    pinVal = 0;
    ioctl( msr3110_fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);    
    usleep( 300000);
    //----------------------------------------------------//
    
    retVal = msr3110_fd;

	cls = env->GetObjectClass(thiz);
	if (cls == NULL) {
		LOGD("failed to find the object");
		retVal = -1; 
		goto end;
	}

    com_mediatek_nfcdemo_NativeNfcManager_detectCard = env->GetMethodID(cls,
        "detectCard","()V");    
    
end:
    return retVal;
}

static jint com_mediatek_nfcdemo_NfcManager_deinitialize(JNIEnv *env, jobject thiz)
{
    LOGD("com_mediatek_nfcdemo_NfcManager_deinitialize");
    int retVal = 0;
    int pinVal = 0;
    //unsigned char cmd_write_swp_reg [] = {
    //    0x0b, 0x02, 0x0a, 0x03, 0x1a, 0x00 };  //write swp reg command

    //if( msr3110_fd < 0) {
    //    msr3110_fd = open( MSR3110_DEV_NAME, O_RDWR);
    //}
    //unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    
    //-----write swp reg----------
    //nfc_msr3110_print_buf( "[SWP]write buff", cmd_write_swp_reg, sizeof(cmd_write_swp_reg));
    //retVal = nfc_msr3110_interface_send_fd( msr3110_fd, cmd_write_swp_reg, sizeof(cmd_write_swp_reg));
    //if( retVal < 0) {
    //    LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
    //    goto end;
    //}   
    
    //usleep( 50000);
    
    //retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    //if( retVal < 0) {
    //    LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal);
    //    goto end;
    //} 

    //usleep( 300000);
    //---------------------------
    
    // Power off
    //pinVal = 1;
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    //usleep( 500);
    pinVal = 0;
    ioctl( msr3110_fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
    //ioctl( msr3110_fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    usleep( 200000);

    //------

    if( msr3110_fd > 0) {
        close(msr3110_fd);
    }

end:
    return retVal;
}


static jint com_mediatek_nfcdemo_NfcManager_singlePolling(JNIEnv *env, jobject thiz, jint type)
{
    LOGD("com_mediatek_nfcdemo_NfcManager_singlePolling");
	jclass cls;
	jfieldID fid;
	jbyteArray byteUID;
    int retVal = -1;
    unsigned char polling_43a[] = {
        //0x46, 0x04, 0x01, 0x00, 0x27, 0x10, 0x82 ,0x00};     // timeout : 10sec
        //0x46, 0x04, 0x01, 0x00, 0x0B, 0xB8, 0x0E, 0x01};    // timeout : 3sec
        0x46, 0x04, 0x01, 0x00, 0x03, 0xE8, 0x36, 0x01};  //timeout : 1sec
    unsigned char polling_15693[] = {
        0x45, 0x02, 0x00, 0x0C, 0x53, 0x00}; 
    unsigned char polling_felica[] = {
        0x42, 0x0E, 0x80, 0x06, 0x81, 0x00, 0x00, 0x00, 
        0x00, 0x00, 0x06, 0x00, 0xFF, 0xFF, 0x01, 0x00, 0x5C, 0x03};
    unsigned char polling_topaz[] = {
        0x4C, 0x08, 0x80, 0x00, 0x03, 0x00, 0x07, 0x00, 0x13, 0x52, 0x43, 0x01};
    unsigned char polling_p2p_i[] = { 
        0x4A, 0x26, 0x00, 0x03, 0x00, 0x03, 0x02, 0x01, 0x01, 0x01,
        0x01, 0x00, 0x03, 0x1A, 0x14, 0x01, 0x00, 0x00, 0x00, 0x22,
        0x46, 0x66, 0x6D, 0x01, 0x01, 0x11, 0x02, 0x02, 0x00, 0x40,
        0x03, 0x02, 0x00, 0x03, 0x04, 0x01, 0xFA, 0x07, 0x01, 0x03,
        0x52, 0x03};
    unsigned char polling_p2p_t[] = {
        0x4A, 0x39, 0x00, 0x03, 0x00, 0x03, 0x02, 0x02, 0x19, 0x02,
        0x09, 0x00, 0x01, 0xFE, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03,
        0x04, 0x25, 0x14, 0x00, 0x01, 0xFE, 0x03, 0x04, 0x05, 0x01,
        0x02, 0x03, 0x04, 0x05, 0x00, 0x00, 0x00, 0x0E, 0x22, 0x46,
        0x66, 0x6D, 0x01, 0x01, 0x11, 0x02, 0x02, 0x00, 0x40, 0x03,
        0x02, 0x00, 0x03, 0x04, 0x01, 0xFA, 0x07, 0x01, 0x03,
        0xCB, 0x05};
        
    unsigned char * polling_cmd = NULL;
	unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
	unsigned char uid[64] = {0};
	int length = 0;
    int cmd_length = 0;
    int sdd_length = 0;
    unsigned short rcv_crc;
    unsigned short cal_crc;

    switch( type) {
    case TAG_TYPE_FELICA:
        LOGD("TAG_TYPE_FELICA");
        polling_cmd = polling_felica;
		cmd_length = sizeof(polling_felica);
        break;
    case TAG_TYPE_14443A:
        LOGD("TAG_TYPE_14443A");
        polling_cmd = polling_43a;
		cmd_length = sizeof(polling_43a);
        break;
    case TAG_TYPE_TYPE1:
        LOGD("TAG_TYPE_TYPE1");
        polling_cmd = polling_topaz;
		cmd_length = sizeof(polling_topaz);
        break;
    case TAG_TYPE_15693:
        LOGD("TAG_TYPE_15693");
        polling_cmd = polling_15693;
		cmd_length = sizeof(polling_15693);
        break;
    case TAG_TYPE_P2P_I:
        LOGD("TAG_TYPE_P2P_I");
        polling_cmd = polling_p2p_i;
		cmd_length = sizeof(polling_p2p_i);        
        break;
    case TAG_TYPE_P2P_T:
        LOGD("TAG_TYPE_P2P_T");
        polling_cmd = polling_p2p_t;
		cmd_length = sizeof(polling_p2p_t);         
        break;       
    default:
        LOGD("ghost command");
        break;
    }    

    memset(uid, 0, 64);  //reset

	cls = env->GetObjectClass(thiz);
	if (cls == NULL) {
		LOGD("failed to find the object");
		retVal = -1; 
		goto end;
	}

 	fid = env->GetFieldID(cls, "mReadUIDSuccess", "Z");
	if (fid == NULL) {
		LOGD("failed to find the field");
		retVal = -1;
		goto end;
	}

    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", polling_cmd, cmd_length);
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, polling_cmd, cmd_length);
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);

    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }
            
	switch( type) {
	case TAG_TYPE_FELICA:
		LOGD("TAG_TYPE_FELICA");
        length = 8;
        if (response_buffer[2] != 0x01 ) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;
        }
		memcpy( uid, response_buffer + 5, length);
        env->SetBooleanField(thiz, fid, true); 
		nfc_msr3110_print_buf( "[felica uid]", uid, length);
		break;
        
	case TAG_TYPE_14443A:
		LOGD("TAG_TYPE_14443A");
        if (response_buffer[2] != 0x01 ) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;
        }
        length = response_buffer[4];
        memcpy( uid, response_buffer + 14, length);
        env->SetBooleanField(thiz, fid, true);
        nfc_msr3110_print_buf( "[14443a uid]", uid, length);
		break;
        
	case TAG_TYPE_TYPE1:
		LOGD("TAG_TYPE_TYPE1");
        length = 4;
        if (response_buffer[2] != 0x01 ) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;
        }
        memcpy( uid, response_buffer + 12, length);
        env->SetBooleanField(thiz, fid, true);
        nfc_msr3110_print_buf( "[type1 uid]", uid, length);        
		break;
        
	case TAG_TYPE_15693:
		LOGD("TAG_TYPE_15693");
        length = 8;
        if (response_buffer[2] != 0x01 ) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;
        }
        memcpy( uid, response_buffer + 6, length);
        reverse_array(uid, length);
        env->SetBooleanField(thiz, fid, true);
        nfc_msr3110_print_buf( "[15693 uid]", uid, length);        
		break;

    case TAG_TYPE_P2P_I:
        LOGD("TAG_TYPE_P2P_I");
        if( (response_buffer[2] == 0xFF) || (response_buffer[3] != 0x00)) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;        
        }
        sdd_length = response_buffer[5];
        length = response_buffer[5 + sdd_length + 2];
        //check CRC
        cmd_length = response_buffer[5 + sdd_length + 3];
        rcv_crc = (response_buffer[5 + sdd_length + 3 + cmd_length + 1]) | (response_buffer[ 5 + sdd_length + 3 + cmd_length ]<<8);
        cal_crc = crc16( CRC_ISO_18092_248,  cmd_length, response_buffer + 5 + sdd_length + 3);
        if( rcv_crc != cal_crc) {
            LOGD("ERROR CRC");
            env->SetBooleanField(thiz, fid, false); 
            goto end;            
        }
        if (response_buffer[5 + sdd_length + 2 + 2]!=0xD5 || response_buffer[5 + sdd_length + 2 + 3] != 0x01) {
            LOGD("ATR_RES CMD_0/CMD_1 Error, CMD_0=%x, CMD_1=%x", response_buffer[7], response_buffer[8]);
            env->SetBooleanField(thiz, fid, false); 
            goto end;            
        }
        length -= 20;
        memcpy( uid, response_buffer + 46, length);
        env->SetBooleanField(thiz, fid, true);
        break;
        
    case TAG_TYPE_P2P_T:
        LOGD("TAG_TYPE_P2P_T");
        if( response_buffer[3] != 0x01) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;        
        }
        //check CRC
        if (crc16_check( CRC_ISO_18092_248, response_buffer + 6, response_buffer[5]) != 0) {
            LOGD("ERROR CRC");
            env->SetBooleanField(thiz, fid, false); 
            goto end;             
        }      
        length = 20;
        memcpy( uid, response_buffer + 23, length);
        nfc_msr3110_print_buf( "[P2P-T]:", uid, length);
        env->SetBooleanField(thiz, fid, true);
        break;
        
	default:
		LOGD("ghost command");
		break;
	}	 
    retVal = length;
    
end: 
	fid = env->GetFieldID(cls, "mUid", "[B");
	if (fid == NULL) {
		LOGD("failed to find the field");
		retVal = -1;
	}
    byteUID = (jbyteArray) env->GetObjectField(thiz, fid);
	if (byteUID == NULL) {
		LOGD("failed to find the byteUID");
		retVal = -1;
	}
	env->SetByteArrayRegion(byteUID, 0, length, (jbyte *)uid);
	byteUID = NULL;
	polling_cmd = NULL;

    LOGD("%s : retVal = %d",__FUNCTION__, retVal);
    return retVal;
}

static jint com_mediatek_nfcdemo_NfcManager_pollingLoop(JNIEnv *env, jobject thiz, jint type)
{
    LOGD("com_mediatek_nfcdemo_NfcManager_pollingLoop");
    //95
    unsigned char polling_43a[] = {
           0x4b, 0x13,    // +4
           0x01, 0x03,    // +1
           0x01, 0x07, 0x01, 0x28, 0x01, 0x00, 0x00, 0x00, 0x00,
           0x02, 0x02, 0x04, 0x01,   // +9
           0x02, 0x02, 0x02, 0x01,  
           0xa4, 0x00 };
    unsigned char polling_15693[] = {           
           0x4b, 0x13,
           0x01, 0x03,
           0x01, 0x07, 0x01, 0x28, 0x10, 0x00, 0x00, 0x00, 0x00,
           0x02, 0x02, 0x04, 0x01,   // +9
           0x02, 0x02, 0x02, 0x01,
           0xb3, 0x00 };
    unsigned char polling_felica[] = {           
           0x4b, 0x13,
           0x01, 0x03,
           0x01, 0x07, 0x01, 0x28, 0x40, 0x00, 0x00, 0x00, 0x00,        
           0x02, 0x02, 0x04, 0x01,   // +9
           0x02, 0x02, 0x02, 0x01, 
           0xe3, 0x00 };
    unsigned char polling_topaz[] = {
           0x4b, 0x13, 
           0x01, 0x03,
           0x01, 0x07, 0x01, 0x28, 0x00, 0x01, 0x00, 0x00, 0x00,        
           0x02, 0x02, 0x04, 0x01,   // +9
           0x02, 0x02, 0x02, 0x01,
           0xa4, 0x00 };        
    unsigned char polling_p2p_i[] = {
           0x4b, 0x13,
           0x01, 0x03, 
           0x01, 0x07, 0x01, 0x28, 0x00, 0x00, 0x00, 0x00, 0x02,
           0x02, 0x02, 0x04, 0x01,   // +9
           0x02, 0x02, 0x02, 0x04,
           0xa8, 0x00 };        
    unsigned char polling_p2p_t[] = {
           0x4b, 0x13,
           0x01, 0x03,
           0x01, 0x07, 0x01, 0x28, 0x00, 0x00, 0x00, 0x00, 0x08,        
           0x02, 0x02, 0x04, 0x01,   // +9
           0x02, 0x02, 0x02, 0x04, 
           0xae, 0x00 };  

    unsigned char * polling_cmd = NULL;
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    unsigned char uid[64] = {0};
    int length = 0;
    int cmd_length = 0;
    int retVal = -1;
	jclass cls;
	jfieldID fid;    
    
    switch( type) {
    case TAG_TYPE_FELICA:
        LOGD("TAG_TYPE_FELICA");
        polling_cmd = polling_felica;
        cmd_length = sizeof(polling_felica);
        break;
        
    case TAG_TYPE_14443A:
        LOGD("TAG_TYPE_14443A");
        polling_cmd = polling_43a;
        cmd_length = sizeof(polling_43a);
        break;
        
    case TAG_TYPE_TYPE1:
        LOGD("TAG_TYPE_TYPE1");
        polling_cmd = polling_topaz;
        cmd_length = sizeof(polling_topaz);
        break;
        
    case TAG_TYPE_15693:
        LOGD("TAG_TYPE_15693");
        polling_cmd = polling_15693;
        cmd_length = sizeof(polling_15693);
        break;
        
    case TAG_TYPE_P2P_I:
        LOGD("TAG_TYPE_P2P_I");
        polling_cmd = polling_p2p_i;
        cmd_length = sizeof(polling_p2p_i);        
        break;
        
    case TAG_TYPE_P2P_T:
        LOGD("TAG_TYPE_P2P_T");
        polling_cmd = polling_p2p_t;
        cmd_length = sizeof(polling_p2p_t);        
        break;  
        
    default:
        LOGD("ghost command");
        break;
    }    

    memset(uid, 0, 64);  //reset
    
    // register callback function.
    IRQCallBack = ( RecvIRQCallBack *) nfc_msr3110_polling_getinfo;

    isCancelIRQ = -1;
    
    //--------------    Init IRQ thread -----------------------//
    sem_init( &g_irq_sem, 0, 0);
    if (pthread_create(&hDevIRQThread, NULL, static_dev_IRQ_thread, NULL) == -1) {
        LOGD( "%s: Can not create the device-ioctl thread\n", __FUNCTION__);
        goto end;
    }

    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", polling_cmd, cmd_length);
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, polling_cmd, cmd_length);
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);

    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }

    //------------ notify ISR Thread -------------------------//
    sem_post( &g_irq_sem);
    
end:
    LOGD("%s : retVal = %d",__FUNCTION__, retVal);
    polling_cmd = NULL;
    g_thiz = thiz;
    return retVal;
}

static jint com_mediatek_nfcdemo_NfcManager_stopPollingLoop(JNIEnv *env, jobject thiz)
{
    int retVal = -1;
	unsigned char cmd_polling_stop[] = { 
		0x4B, 0x0F, 0x01, 0x02, 0x01, 
		0x07, 0x01, 0x28, 0x00, 0x00, 
		0x00, 0x00, 0x00, 0x02,	0x02, 
		0x03, 0x01, 0x96, 0x00}; 
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    isCancelIRQ = 1;    
    retVal = nfc_cancel_dev_IRQ(msr3110_fd);

    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", cmd_polling_stop, sizeof(cmd_polling_stop));
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, cmd_polling_stop,  sizeof(cmd_polling_stop));
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);

    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }
    
end:
    g_thiz = NULL;
    return retVal;
}


static jint com_mediatek_nfcdemo_NfcManager_getPollingInfo(JNIEnv *env, jobject thiz)
{
    int fd = -1;
    int retVal = -1;
    int length = -1;
    unsigned char cmd_get_info[] = { 0x4B ,0x06 ,0x02 ,0x01 ,0x03 ,0x02 ,0x03 ,0x00 ,0x5C ,0x00};
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
	jclass cls;
	jfieldID fid;
	jbyteArray byteResult;

    fd = open( MSR3110_DEV_NAME, O_RDWR);
    if( fd < 0) {
        LOGD( "Can not open MSR3110\n");
        retVal = -1;
        goto end;
    }   


    nfc_msr3110_print_buf( "write buff", cmd_get_info, sizeof(cmd_get_info));
    retVal = nfc_msr3110_interface_send_fd( fd, cmd_get_info, sizeof(cmd_get_info));
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);
    memset(response_buffer, 0, MSR3110_READ_BUF_SIZE_MAX);  //reset
    retVal = nfc_msr3110_interface_recv_fd( fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }

end:    

    if (retVal < 0 ) {
        return retVal;
    }
	cls = env->GetObjectClass(thiz);
	if (cls == NULL) {
		LOGD("failed to find the object");
		retVal = -1;
	}

	fid = env->GetFieldID(cls, "mResponse", "[B");
	if (fid == NULL) {
		LOGD("failed to find the field");
		retVal = -1;
	}
    byteResult = (jbyteArray) env->GetObjectField(thiz, fid);
	if (byteResult == NULL) {
		LOGD("failed to find the byteUID");
		retVal = -1;
	}
    length = 2 + response_buffer[1] + 2;
	env->SetByteArrayRegion(byteResult, 0, length, (jbyte *)response_buffer);
	byteResult = NULL;
    retVal = length;
    
    return retVal;
}


static jint com_mediatek_nfcdemo_NfcManager_cardMode(JNIEnv *env, jobject thiz, jint type)
{
    int retVal = -1;
    unsigned char card_mode_typeA[] = {
            0x4b, 0x0f, 0x01, 0x02, 0x01, 0x07,
            0x01, 0x28, 0x00, 0x00,   
            0x01, 0x00,   //card mask  Type A
            0x00, 0x02, 0x02,
            0x04, 0x0a,
            0xa1, 0x00 }; //checksum
            
    unsigned char card_mode_typeB[] = {
            0x4b, 0x0f, 0x01, 0x02, 0x01, 0x07,
            0x01, 0x28, 0x00, 0x00,   
            0x02, 0x00,   //card mask  Type B
            0x00, 0x02, 0x02,
            0x04, 0x0a,
            0xa2, 0x00 }; //checksum

    unsigned char card_mode_typeAB[] = {
            0x4b, 0x0f, 0x01, 0x02, 0x01, 0x07,
            0x01, 0x28, 0x00, 0x00,   
            0x03, 0x00,   //card mask  Type A , B
            0x00, 0x02, 0x02,
            0x04, 0x02,
            0x99, 0x00 }; //checksum
    unsigned char * polling_cmd = NULL;
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    int cmd_length = sizeof(card_mode_typeA);

    switch( type) {
    case CARD_TYPE_A:
        LOGD("CARD_TYPE_A");
        polling_cmd = card_mode_typeA;
        break;
        
    case CARD_TYPE_B:
        LOGD("CARD_TYPE_B");
        polling_cmd = card_mode_typeB;
        break;
        
    case CARD_TYPE_AB:
        LOGD("CARD_TYPE_AB");
        polling_cmd = card_mode_typeAB;
        break;
        
    default:
        LOGD("Ghost Type");
        goto end;
        break;
    }

    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", polling_cmd, cmd_length);
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, polling_cmd, cmd_length);
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);

    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }

end:
    polling_cmd = NULL;
    return retVal;
}

static jint com_mediatek_nfcdemo_NfcManager_stopCardMode(JNIEnv *env, jobject thiz)
{
    int retVal = -1;
    unsigned char card_mode_stop[] = {
            0x4b, 0x0f, 0x01, 0x02, 0x01, 0x07,   
            0x01, 0x28, 0x00, 0x00,   
            0x00, 0x00,   //card mask
            0x00, 0x02, 0x02,   
            0x05, 0x00,
            0x97, 0x00 }; //checksum
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    
    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", card_mode_stop, sizeof(card_mode_stop));
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, card_mode_stop, sizeof(card_mode_stop));
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);
    
    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }

end:

    return retVal;
}


static jint com_mediatek_nfcdemo_NfcManager_singleDeviceDetect(JNIEnv *env, jobject thiz, jint type)
{  
    unsigned char polling_p2p_i[] = { 
        0x4A, 0x26, 0x00, 0x03,
              0x00, 0x03, 0x02, 0x01, 0x01, 
              0x01, 0x01, 0x00, 
              0x03, 0x1A, 0x14, 0x01, 0x00, 0x00, 0x00, 0x22,
                    0x46, 0x66, 0x6D, 0x01, 0x01, 0x11, 0x02, 0x02, 0x00, 0x40,
                    0x03, 0x02, 0x00, 0x03, 0x04, 0x01, 0xFA, 0x07, 0x01, 0x03,
        0x52, 0x03};
    unsigned char polling_p2p_t[] = {
        0x4A, 0x39, 0x00, 0x03, 
              0x00, 0x03, 0x02, 0x02, 0xFA,   //19
              0x02, 0x09, 0x00, 0x01, 0xFE, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03,
              0x04, 0x25, 0x14, 0x00, 0x01, 0xFE, 0x03, 0x04, 0x05, 0x01,
                    0x02, 0x03, 0x04, 0x05, 0x00, 0x00, 0x00, 0x0E, 0x22, 0x46,
                    0x66, 0x6D, 0x01, 0x01, 0x11, 0x02, 0x02, 0x00, 0x40, 0x03,
                    0x02, 0x00, 0x03, 0x04, 0x01, 0xFA, 0x07, 0x01, 0x03,
        0xAC, 0x06};        //cb , 5
    unsigned char * polling_cmd = NULL;
	unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    unsigned char atr[64] = {0};
 	jclass cls;
	jfieldID fid;
    jbyteArray byteATR;
    int retVal = -1;   
    int length = 0;
    int cmd_length = 0;
    int sdd_length = 0;
    unsigned short rcv_crc;
    unsigned short cal_crc;    

    switch(type) {
    case TAG_TYPE_P2P_I:
        LOGD("P2P_I");
        polling_cmd = polling_p2p_i;
        cmd_length = sizeof(polling_p2p_i);
        break;
        
    case TAG_TYPE_P2P_T:
        LOGD("P2P_T");
        polling_cmd = polling_p2p_t;
        cmd_length = sizeof(polling_p2p_t);
        break;
        
    default:
        LOGD("Ghost Command");
        break;
    }
    memset(atr, 0, 64);  //reset

	cls = env->GetObjectClass(thiz);
	if (cls == NULL) {
		LOGD("failed to find the object");
		retVal = -1; 
		goto end;
	}

 	fid = env->GetFieldID(cls, "mReadUIDSuccess", "Z");
	if (fid == NULL) {
		LOGD("failed to find the field");
		retVal = -1;
		goto end;
	}

    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", polling_cmd, cmd_length);
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, polling_cmd, cmd_length);
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);

    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }

    switch(type) {
    case TAG_TYPE_P2P_I:
        LOGD("P2P_I");
        if( (response_buffer[2] == 0xFF) || (response_buffer[3] != 0x00)) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;        
        }
        sdd_length = response_buffer[5];
        length = response_buffer[5 + sdd_length + 2];
        //check CRC
        cmd_length = response_buffer[5 + sdd_length + 3];
        rcv_crc = (response_buffer[5 + sdd_length + 3 + cmd_length + 1]) | (response_buffer[ 5 + sdd_length + 3 + cmd_length ]<<8);
        cal_crc = crc16( CRC_ISO_18092_248,  cmd_length, response_buffer + 5 + sdd_length + 3);
        if( rcv_crc != cal_crc) {
            LOGD("ERROR CRC");
            env->SetBooleanField(thiz, fid, false); 
            goto end;            
        }
        if (response_buffer[5 + sdd_length + 2 + 2]!=0xD5 || response_buffer[5 + sdd_length + 2 + 3] != 0x01) {
            LOGD("ATR_RES CMD_0/CMD_1 Error, CMD_0=%x, CMD_1=%x", response_buffer[7], response_buffer[8]);
            env->SetBooleanField(thiz, fid, false); 
            goto end;            
        }
        length -= 20;
        memcpy( atr, response_buffer + 46, length);
        env->SetBooleanField(thiz, fid, true);

        break;
        
    case TAG_TYPE_P2P_T:
        LOGD("P2P_T");
        if( response_buffer[3] != 0x01) {
            LOGD("ERROR response");
            env->SetBooleanField(thiz, fid, false); 
            goto end;        
        }
        //check CRC
        if (crc16_check( CRC_ISO_18092_248, response_buffer + 6, response_buffer[5]) != 0) {
            LOGD("ERROR CRC");
            env->SetBooleanField(thiz, fid, false); 
            goto end;             
        }      
        length = 20;
        memcpy( atr, response_buffer + 23, length);
        nfc_msr3110_print_buf( "[P2P-T]:", atr, length);
        env->SetBooleanField(thiz, fid, true);
        break;
        
    default:
        LOGD("Ghost Command");
        break;
    }
        retVal = length;
        
end: 
    fid = env->GetFieldID(cls, "mUid", "[B");
    if (fid == NULL) {
        LOGD("failed to find the field");
        retVal = -1;
    }
    byteATR = (jbyteArray) env->GetObjectField(thiz, fid);
    if (byteATR == NULL) {
        LOGD("failed to find the byteUID");
        retVal = -1;
    }
    env->SetByteArrayRegion(byteATR, 0, length, (jbyte *)atr);
    byteATR = NULL;
    polling_cmd = NULL;

    LOGD("%s : retVal = %d",__FUNCTION__, retVal);
    return retVal;
}



static jint com_mediatek_nfcdemo_NfcManager_setReceiveOnlyMode(JNIEnv *env, jobject thiz)
{
    int retVal = -1;
    unsigned char cmd_receive_only [] = { 
        0x4A, 0x0A, 0x01, 0x02, 0x06, 0x02, 0x0C, 0x01, 0x05, 0x02,
        0x01, 0x00, 0x74, 0x00 };
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};

    // --------------- Send Command ---------//
    nfc_msr3110_print_buf( "write buff", cmd_receive_only, sizeof(cmd_receive_only));
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, cmd_receive_only, sizeof(cmd_receive_only));
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        goto end;
    }   

    usleep( 50000);
    
    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal); 
        goto end;        
    }    

end:
    return retVal;
}


static jint com_mediatek_nfcdemo_NfcManager_dataExchange
(JNIEnv *env, jobject thiz, jint type, jbyte header_counter, jint data_length, jbyteArray data)
{
    int retVal = -1;
    int length = 0;
    int temp_length = 0;
    int checksum = 0;
    int error_code = SUCCESS;
    int i;
 	jclass cls;
	jfieldID fid;
    jbyteArray byte_array = NULL;
    jbyteArray raw_data = NULL;
    jbyteArray header = NULL;
    jbyteArray data_payload = NULL;
    //I - send    : 4A 10 01 02 06 02 0C 01 05 08 01 06 06  D4 06 00  00 00  66 01
    //T - receive: 4A 10 01 02 06 02 0C 01 05 08 01 06 06  D5 07 00  00 00  68 01
    //unsigned char cmd_dep_send_initiator [] = {
    //    0x4A, 0x10, 0x01, 0x02, 0x06, 0x02, 0x0C, 0x01, 0x05, 0x08, 0x01, 0x06, 0x06,
    //    0xD4, 0x06, 0x00, // header
    //    0x00, 0x00,     //data payload
    //    0x66, 0x01 };   //check sum
    //unsigned char cmd_dep_send_target [] = {
    //    0x4A, 0x10, 0x01, 0x02, 0x06, 0x02, 0x0C, 0x01, 0x05, 0x08, 0x01, 0x06, 0x06,
    //    0xD5, 0x07, 0x00,    //header
    //    0x00, 0x00,     // data payload
    //    0x68, 0x01 };   //check sum
    
    unsigned char cmd_dep_send_initiator [] = {
        0x4A, 0x10, 0x01, 0x02, 0x06, 0x02, 0x0C, 0x01, 0x05, 0x08, 0x01, 0x06, 0x06,
        0xD4, 0x06};    // header   
    unsigned char cmd_dep_send_target [] = {
        0x4A, 0x10, 0x01, 0x02, 0x06, 0x02, 0x0D, 0x01, 0x05, 0x08, 0x01, 0x06, 0x06,
        0xD5, 0x07};    //header   
        
    unsigned char * cmd = NULL;
	unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};
    unsigned char * temp_array = NULL;
    unsigned char * data_byte = NULL;
    unsigned char * receive_data = NULL;
    
    switch(type) {
    case TAG_TYPE_P2P_I:
        LOGD("P2P_I");
        temp_length = 0;
        length = sizeof(cmd_dep_send_initiator) + 1 + data_length + 2;
        cmd = new unsigned char [length];
                
        //--- command
        temp_length = sizeof(cmd_dep_send_initiator);
        memcpy( cmd, cmd_dep_send_initiator , temp_length);
        //nfc_msr3110_print_buf( ">> 1", cmd, temp_length);
        
        //--- update length
        cmd[1] = length - 4;

        //--- update (header + payload) length 
        cmd[11] = data_length + 4 ;
        cmd[12] = data_length + 4 ;
        cmd[9] = cmd[11] + 2;
                
        //--- header counter
        LOGD("->header_counter = %d", header_counter);
        header_counter = (header_counter + 1 )%4 ;
        LOGD("<-header_counter = %d", header_counter);

        cmd[temp_length] = header_counter;
        temp_length += 1;
        //nfc_msr3110_print_buf( ">> 2", cmd, temp_length);
        
        //--- data
        data_byte = (unsigned char *)env->GetByteArrayElements(data, NULL);
        //nfc_msr3110_print_buf( ">> DATA", data_byte, data_length);
        memcpy( cmd + temp_length, data_byte , data_length);
        //nfc_msr3110_print_buf( ">> 3", cmd, temp_length + data_length); 
        
        //---checksum
        for(i = 0; i < length - 2 ;i++) {
            checksum += cmd[i];
        }
        cmd[length - 2] = checksum & 0xFF;
        cmd[length - 2 + 1] = (checksum>>8)&0xFF;
        break;
        
    case TAG_TYPE_P2P_T:
        LOGD("P2P_T");      
        temp_length = 0;
        length = sizeof(cmd_dep_send_target) + 1 + data_length + 2;
        cmd = new unsigned char [length];
        
        //--- command
        temp_length = sizeof(cmd_dep_send_initiator);
        memcpy( cmd, cmd_dep_send_target , temp_length);
       
        //--- update length
        cmd[1] = length - 4;

        //--- update (header + payload) length 
        cmd[11] = data_length + 4 ;
        cmd[12] = data_length + 4 ;
        cmd[9] = cmd[11] + 2;
        
        //---- header counter
        LOGD("->header_counter = %d", header_counter);
        cmd[temp_length] = header_counter;
        temp_length += 1;

        //--- data      
        data_byte = (unsigned char *)env->GetByteArrayElements(data, NULL);
        //nfc_msr3110_print_buf( ">> DATA", data_byte, data_length);
        memcpy( cmd + temp_length, data_byte , data_length);
        
        //---checksum
        for(i = 0; i < length - 2 ; i++) {
            checksum += cmd[i];
        }
        cmd[length - 2] = checksum & 0xFF;
        cmd[length - 2 + 1] = (checksum>>8)&0xFF;       
        break;
        
    default:
        LOGD("Ghost Command");
        retVal = -1;
        goto end;
        break;
    }   

    nfc_msr3110_print_buf( ">>SEND DATA", cmd, length);

	cls = env->GetObjectClass(thiz);
	if (cls == NULL) {
		LOGD("failed to find the object");
		retVal = -1; 
        //JNI error
        error_code = JNI_ERROR;
		goto end;
	}
    
    // --------------- Send  command ---------//
    nfc_msr3110_print_buf( "write buff", cmd, length);
    retVal = nfc_msr3110_interface_send_fd( msr3110_fd, cmd, length);
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        //I2C send error
        error_code = I2C_SEND_ERROR;
        goto end;
    }   

    usleep( 50000);

    // --------------- Receive Response ---------//
    retVal = nfc_msr3110_interface_recv_fd( msr3110_fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal);
        //I2C receive error
        error_code = I2C_RECEIVE_ERROR;
        goto end;        
    }    

    //set raw data
    fid = env->GetFieldID( cls, "mRawData", "[B");
    length = response_buffer[1] + 4;
    raw_data = env->NewByteArray(length);
    env->SetByteArrayRegion(raw_data, 0, length, (jbyte *) response_buffer);
    env->SetObjectField(thiz, fid, raw_data);


    //Target Field Off
    if (response_buffer[3] == 0x1B) {
        LOGD( "%s: Fail : Field OFF", __FUNCTION__);
        retVal = -1;
        error_code = FIELD_OFF;
        //Field off
        goto end;        
    }   

    //check success or fail
    if (response_buffer[3] != 0x01) {
        LOGD( "%s: Fail : DEP Fail", __FUNCTION__);
        retVal = -1;
        error_code = DEP_ERROR;
        //DEP fail
        goto end;        
    }

    //check checksum
    if (check_checksum(response_buffer, response_buffer[1] + 2) == false) {
        LOGD( "%s: Fail : checksum error", __FUNCTION__);
        retVal = -1;
        error_code = CHECKSUM_ERROR;
        //checksum error
        goto end;
    }

    //check CRC
    if (crc16_check( CRC_ISO_18092_248, response_buffer + 5, response_buffer[4]) != 0) {
        LOGD("ERROR CRC");
        retVal = -1;
        error_code = CRC_ERROR;
        //crc error
        goto end;             
    }   

    //--- mHeader ----
    fid = env->GetFieldID( cls, "mHeader", "[B");
	if (fid == NULL) {
		LOGD("failed to find the field");
		retVal = -1;
        //JNI error
        error_code = JNI_ERROR;
        goto end;
	}
    length = 3;
    temp_array = new unsigned char[length];
    
    header = env->NewByteArray(length);
    memcpy( temp_array, response_buffer + 6, length);
    env->SetByteArrayRegion(header, 0, length, (jbyte *) temp_array);
    env->SetObjectField(thiz, fid, header);
    delete [] temp_array;
    
    //memset(temp_array, 0, 10);  //reset
    //--- mDataPayload ----
    fid = env->GetFieldID( cls, "mDataPayload", "[B");
	if (fid == NULL) {
		LOGD("failed to find the field");
		retVal = -1;
        //JNI error
        error_code = JNI_ERROR;
        goto end;
	}    
    length = response_buffer[1] - 3 - 4 - 2;
    temp_array = new unsigned char[length];
    data_payload = env->NewByteArray(length);
    memcpy( temp_array, response_buffer + 9, length);
    env->SetByteArrayRegion(data_payload, 0, length, (jbyte *) temp_array);
    env->SetObjectField(thiz, fid, data_payload);
    delete[] temp_array;
    
    //I - receive :  02 0B 01 01 08 06  D5 07 00 00 00 C8 79 3A 02
    //T - receive : 02 0B 01 01 08 06  D4 06 00 00 00 14 9C A7 01
    
end:
    //set error code
	fid = env->GetFieldID(cls, "mErrorCode", "I");
	if (fid == NULL) {
		LOGD("failed to find the field");       
	}    
    env->SetIntField(thiz, fid, error_code); 
    
    if (data_byte != NULL) {
       env->ReleaseByteArrayElements(data, (jbyte*)data_byte, JNI_ABORT);
    }
    
    delete [] cmd;
 	byte_array = NULL;
 	cls = NULL;
	fid = NULL;
    return retVal;
}


static jint com_mediatek_nfcdemo_NfcManager_writeSwpReg(JNIEnv *env, jobject thiz)
{
    int retVal = -1;
    int fd = -1;
    unsigned char cmd_write_swp_reg [] = { 0x0b, 0x02, 0x0a, 0x03, 0x1a, 0x00 };  //write swp reg command
    unsigned char response_buffer[ MSR3110_READ_BUF_SIZE_MAX] = {0};

    fd = open( MSR3110_DEV_NAME, O_RDWR);
    if( fd < 0) {
        LOGD( "Can not oprn MSR3110\n");
        retVal = -1;
        goto end;
    }    
    
    //-----write swp reg----------
    nfc_msr3110_print_buf( "[SWP]write buff", cmd_write_swp_reg, sizeof(cmd_write_swp_reg));
    retVal = nfc_msr3110_interface_send_fd( fd, cmd_write_swp_reg, sizeof(cmd_write_swp_reg));
    if( retVal < 0) {
        LOGD( "%s: Fail : send command (%d)", __FUNCTION__, retVal);
        retVal = -1;
        goto end;
    }   
    
    usleep( 50000);
    
    retVal = nfc_msr3110_interface_recv_fd( fd, response_buffer, MSR3110_READ_BUF_SIZE_MAX);
    if( retVal < 0) {
        LOGD( "%s: Fail : recv command (%d)", __FUNCTION__, retVal);
        retVal = -1;
        goto end;
    } 
    usleep( 300000);
    //---------------------------   

end:
    // deinit

    if(fd > 0) {
        close(fd);
    }
    return retVal;
}


//---------------------------------------------------------------------------------------//
//    JNI Define
//---------------------------------------------------------------------------------------//
static JNINativeMethod gMethods[] =
{
   {"doInitialize", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_initialize},
   {"doDeinitialize", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_deinitialize},
   {"doSinglePolling", "(I)I",
        (void *)com_mediatek_nfcdemo_NfcManager_singlePolling},
   {"doPollingLoop", "(I)I",
        (void *)com_mediatek_nfcdemo_NfcManager_pollingLoop},
   {"doStopPollingLoop", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_stopPollingLoop},
   {"doPollingInfo", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_getPollingInfo},
   {"doCardMode", "(I)I",
        (void *)com_mediatek_nfcdemo_NfcManager_cardMode}, 
   {"doStopCardMode", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_stopCardMode},
   {"doSingleDeviceDetect", "(I)I",
        (void *)com_mediatek_nfcdemo_NfcManager_singleDeviceDetect},
   {"doSetReceiveOnlyMode", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_setReceiveOnlyMode},
   {"doDataExchange", "(IBI[B)I",
        (void *)com_mediatek_nfcdemo_NfcManager_dataExchange},        
   {"doWriteSwpReg", "()I",
        (void *)com_mediatek_nfcdemo_NfcManager_writeSwpReg},          
};


int register_com_mediatek_nfcdemo_NativeNfcManager(JNIEnv *e)
{
   return jniRegisterNativeMethods(e,
      "com/mediatek/nfcdemo/nfc/NativeNfcManager",
      gMethods, NELEM(gMethods));
}

} //namespace android
