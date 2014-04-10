/*---------------------------------------------------------------------------
	Copyright (c) 2010 MStar Semiconductor, Inc.  All rights reserved.
---------------------------------------------------------------------------*/
//---------------------------------------------------------------------------
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <unistd.h>
//#include "drvSPI_if.h"
#include <android/log.h>
//#include "open_nfc_main.h"
#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#define AnsiString char[];
//#define void void
#define LOG_TAG "MStar-spi"

//#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, fmt, ##args)
//#define LOGI(fmt, args...)

typedef unsigned char   U8;
typedef unsigned short  u16;
typedef unsigned int    U32;


bool_t Checksum_is_correct( unsigned char *buf, unsigned short len );
void SPI_Reset( void );
int I2C_Init( int index );


#define SPI_DEBUG   1
#if(SPI_DEBUG == 1)
#define SPI_DUMPBUF(h,b,l)  { PNALDebugLog(h); PNALDebugLogBuffer(b,l);}
#else
#define SPI_DUMPBUF(h,b,l)
#endif


#define SPI_WC 8
#define I2C_WC 8


#define AP_USE_ANTARES3_FORMAT_TO_DECIDE_RCV_CNT 0xFFFFFFFF
#ifndef MIN
#define MIN(x,y) ((x)>(y))?(y):(x)
#endif

/************manipulate constant *******************/
#define SPI_NO_USE_INFORM_LINE_BUT_POLLING
#define SPI_RETURN_DATA_READY_INDICATOR 0xCC


#define WAITING_PROCESSING_MAX_TIMES 3000
//#define WAITING_PROCESSING_MAX_TIMES 100000
/************************************
        A3 protocol
************************************/
#define A3_PROTOCOL_WRITE_ACTION    0x80
#define A3_PROTOCOL_READ_ACTION     0x00


#define A3_PROTOCOL_AP_READY_POS    0
//#define A3_PROTOCOL_LEN_POS         1
#define A3_PROTOCOL_DATA_POS        1
#define A3_PROTOCOL_BURST_BYTECNT   14
#define A3_PROTOCOL_FW_READY_POS    15

static int spi_fd = -1;
//int spi_fd;

int spi_errorno;
//struct mutex spi_mutex;
int spi_tx_cnt;
int spi_tx_err_cnt;
int spi_rx_cnt;
int spi_rx_err_cnt;
int spi_rx_hlt_cnt;
int i2c_reset_cnt;


#define A3_TX_BUF_LEN   300
unsigned char _SPI_TX_BUFFER[A3_TX_BUF_LEN];
int           _SPI_TX_Len;

#define K7806_FLOW	1
#define TCL_FLOW	2
#define MT65x7_FLOW	3

#define INIT_FLOW	MT65x7_FLOW

#define DEV_NAME_SPI_DEV "/dev/spidev3.1"
#define DEV_NAME_SPI_CON "/dev/nfc-control"
static int controlfd = -1;
 

//I2C use
static int i2c_fd = -1;
#define DEV_NAME_I2C_DEV "/dev/msr3110"
#define A3_I2C_RECV_MAX  255
#define I2C_SEND_TRY_CNT_MAX  1000

//IRQ
#define IRQ_ABORT_TRY_CNT_MAX  5



//#if( INIT_FLOW == K7806_FLOW )
#if 0
#define MSR3110_CONTROL_IO         0xCD
#define MSR3110_IOCTL_NFCON        _IOW(MSR3110_CONTROL_IO, 0x00, int)
#define MSR3110_IOCTL_NFCRESET     _IOW(MSR3110_CONTROL_IO, 0x01, int)
#define MSR3110_IOCTL_VDDSDIO_CONTROL _IOW(MSR3110_CONTROL_IO, 0x05, int)
#define MSR3110_IOCTL_GET_GPIO6_STATE _IOW(MSR3110_CONTROL_IO, 0x06, int)

static void K7806_IO_Control(int pin, int val)
{
	int fd=MS_GetControlFD();
	if( pin == 0){		//NFC_ON
		PNALDebugLog("Set VEN: %d",val);
		ioctl(fd,  MSR3110_IOCTL_NFCON, &val);
	}
	else if(pin == 1){	//NFC_RST
		PNALDebugLog("Set RST: %d",val);
		ioctl(fd,  MSR3110_IOCTL_NFCRESET, &val);
	}
	else if(pin == 2){	//NFC_VDDSDIO
		PNALDebugLog("Set VDDSDIO: %d",val);
		ioctl(fd,  MSR3110_CONTROL_IO, &val);
	}
}
#define VEN_ON()	K7806_IO_Control(0,1)
#define VEN_OFF()	K7806_IO_Control(0,0)
#define RST_ON()	K7806_IO_Control(1,1)
#define RST_OFF()	K7806_IO_Control(1,0)
#define VDD_ON()	K7806_IO_Control(2,1)
#define VDD_OFF()	K7806_IO_Control(2,0)

int MS_GPIO_Control(int pin, int val)
{
	K7806_IO_Control(pin, val);
	return 0;
}
#endif
#if( INIT_FLOW == MT65x7_FLOW )
#define MSR3110_CONTROL_IO         0xCD
#define MSR3110_IOCTL_FW_UPGRADE  _IOW(MSR3110_CONTROL_IO, 0x00, int)
#define MSR3110_IOCTL_NFCON       _IOW(MSR3110_CONTROL_IO, 0x01, int)
#define MSR3110_IOCTL_NFCRESET    _IOW(MSR3110_CONTROL_IO, 0x02, int)
#define MSR3110_IOCTL_IRQ         _IOW( MSR3110_CONTROL_IO, 0x03, int)
#define MSR3110_IOCTL_IRQ_ABORT _IOW( MSR3110_CONTROL_IO, 0x04, int)


int MS_GetDevFD()
{
    if( i2c_fd<= 0 ){
        i2c_fd = open(DEV_NAME_I2C_DEV, O_RDWR);
    }
    return i2c_fd;
}

static int MT65x7_IO_Control(int pin, int val)
{
    int res = 0;
	int fd=MS_GetDevFD();
	if( pin == 0){		//NFC_ON
		PNALDebugLog("Set VEN: %d",val);
		res = ioctl(fd,  MSR3110_IOCTL_NFCON, &val);
	}
	else if(pin == 1){	//NFC_RST
		PNALDebugLog("Set RST: %d",val);
		res = ioctl(fd,  MSR3110_IOCTL_NFCRESET, &val);
	}
	else if(pin == 2){	//IRQ 
		PNALDebugLog("Set IRQ: %d",val);
		res = ioctl(fd,  MSR3110_IOCTL_IRQ, &val);
	}
	else if(pin == 3){	//IRQ 
		PNALDebugLog("Set IRQ ABORT: %d",val);
		res = ioctl(fd,  MSR3110_IOCTL_IRQ_ABORT, &val);
	}	
	return res;
}
#define VEN_ON()	MT65x7_IO_Control(0,1)
#define VEN_OFF()	MT65x7_IO_Control(0,0)
#define RST_ON()	MT65x7_IO_Control(1,1)
#define RST_OFF()	MT65x7_IO_Control(1,0)
#define IRQ_ON()	MT65x7_IO_Control(2,1)
#define IRQ_ABORT()	MT65x7_IO_Control(3,1)

#endif

int MS_GetControlFD()
{
    if( controlfd <= 0 ){
        controlfd = open(DEV_NAME_SPI_CON, O_RDWR);
    }
    return controlfd;
}


#define VEN_ON_WAIT_TIME 	300000
#define VEN_OFF_WAIT_TIME 	200000
#define RST_ON_WAIT_TIME 	100000
#define RST_OFF_WAIT_TIME	0
int MS_PowerOn( )
{
	PNALDebugLog( "%s ", __FUNCTION__);
	VEN_ON();
	RST_OFF(); // in case RST is pulled HIGH

	PNALDebugLog( "%s SLEEP-START", __FUNCTION__);
	usleep( VEN_ON_WAIT_TIME);
	PNALDebugLog( "%s SLEEP-END", __FUNCTION__);
	return 0;
}

int MS_PowerOff( )
{

	VEN_OFF();
	PNALDebugLog( "%s SLEEP-START", __FUNCTION__);
	usleep( VEN_OFF_WAIT_TIME);
	PNALDebugLog( "%s SLEEP-END", __FUNCTION__);

	return 0;
}

int MS_Reset( )
{
	VEN_OFF();
	PNALDebugLog( "%s SLEEP-START", __FUNCTION__);
	usleep( VEN_OFF_WAIT_TIME);
	PNALDebugLog( "%s SLEEP-END", __FUNCTION__);

	RST_ON();	
	VEN_ON();
	PNALDebugLog( "%s SLEEP-START", __FUNCTION__);
	usleep( RST_ON_WAIT_TIME);
	PNALDebugLog( "%s SLEEP-END", __FUNCTION__);
	RST_OFF();
	PNALDebugLog( "%s SLEEP-START", __FUNCTION__);
	usleep( VEN_ON_WAIT_TIME);
	PNALDebugLog( "%s SLEEP-END", __FUNCTION__);
	
	return 0;
}


/*
    return 1 for RF field is detected.  otherwise return 0;
*/
int MS_GetRFStatus()
{
    int val=0;
    int fd=MS_GetControlFD();

    if( fd < 0){
        PNALDebugError( "[MS_GetRFStatus] open '/dev/nfc-control' error");
        return 0;
    }

    //ioctl(fd,  MSR3110_IOCTL_GET_GPIO6_STATE, &val);
    if( val == 1){
        return 0;
    }else{
        return 1;
    }
}

U8 SPI_A3Portocol_ReadByte(U8 RegPos)
{
    //struct spi_device	*spi;
    int status;
    U8  u8write;

    u8write = ( RegPos | A3_PROTOCOL_READ_ACTION );

	//spi = spi_dev_get(cur_spi);
	//status = spi_write_then_read(cur_spi, &u8write, 1, &u8read, 1);

	status = read(spi_fd, &u8write, 1);      //the SPI will send the 'u8Write' content, then put the received data on u8Write

	if( status != 0){
	    PNALDebugError( "[spi wr] err: write:0x%02x, 0x%02x", u8write, status);
	}
    spi_errorno = status;

    return u8write;
}

void SPI_A3Portocol_WriteByte(U8 RegPos, U8 Val)
{
    int status = 0;
    U8 buffer[2];
    //int status;
    /* Add SPI write procedure here */
    buffer[0] = RegPos | A3_PROTOCOL_WRITE_ACTION;
    buffer[1] = Val;
    //status = spi_write(cur_spi, buffer, 2);
    status  = write(spi_fd, buffer, 2);
    if( status != 2){
        PNALDebugError( "[spi w] err: write:0x%02x, write:0x%02X, status:%d", buffer[0], buffer[1], status);
    }
    spi_errorno = status;

}

void SPI_A3Portocol_APSetReady( void )
{
    SPI_A3Portocol_WriteByte(A3_PROTOCOL_AP_READY_POS,0x01);
}

void SPI_A3Portocol_APClearFwReady( void )
{
    SPI_A3Portocol_WriteByte(A3_PROTOCOL_FW_READY_POS,0x01);
}

int SPI_SEND(U32 len,U8 *buf)
{
    U32 HaveSendByteCnt = 0;
    U8 ThisTimeSendByteCnt;
    U8 GetByte = 0xFF;
    U8 cnt1;
    int delay;


    SPI_DUMPBUF("SPI_SEND:", buf, len);
    //mutex_lock(&spi_mutex);
    while(HaveSendByteCnt < len)
    {
        ThisTimeSendByteCnt = MIN((len - HaveSendByteCnt), A3_PROTOCOL_BURST_BYTECNT);

        /* Wait Fw Ready to Rcv..... */
        for(delay = WAITING_PROCESSING_MAX_TIMES;delay;--delay)
        {
            GetByte = 0xFF;
            if((GetByte = SPI_A3Portocol_ReadByte(A3_PROTOCOL_AP_READY_POS)) == 0x00)    // Fw has clear this byte , so can send....
            {
                break;
            }
        }
        //PNALDebugLog( "[SPI_SEND] GetByte = %2x", GetByte);
        if(GetByte == 0x00) // go out because Fw ready
        {
            for( cnt1=0; cnt1 < ThisTimeSendByteCnt; cnt1++ )
            {
                 /* Start Send data */
                SPI_A3Portocol_WriteByte(cnt1+A3_PROTOCOL_DATA_POS, buf[HaveSendByteCnt + cnt1]);
            }
            /* OK, send buf full, so set ready to info Fw read ...  */
            SPI_A3Portocol_APSetReady();

            HaveSendByteCnt += ThisTimeSendByteCnt;
        }
        else    // go out because time out
        {
            PNALDebugLog( "[SPI_SEND] Recv fail...");
            break;
        }
        if( spi_errorno < 0){
            HaveSendByteCnt= spi_errorno;
            break;
        }
    }
    //mutex_unlock(&spi_mutex);
    return HaveSendByteCnt;
}

/* return time difference in mini-second */
static int timeDiff(struct timeval *starttv, struct timeval *endtv)
{
	int start,end;
	if( starttv==NULL || endtv==NULL)
		return 0;

	start = starttv->tv_sec * 1000 + starttv->tv_usec/1000;
    end = endtv->tv_sec*1000 + endtv->tv_usec/1000;

    return (end-start);
}

int SPI_RECV( unsigned int len,U8 *buf, int timeout )
{
    U32 HaveReadByteCnt;
    U8 ThisTimeRcvByteCnt;
    U8 GetByte;
    U8 cnt1;
    int delay;
    int retry_cnt = 0;
    struct timeval start_tv, cur_tv;
    int showByteCnt=0;


spi_recv_start:
    HaveReadByteCnt = 0;
    GetByte = 0xFE;
	//Get time value for timeout
	gettimeofday(&start_tv,NULL);

    PNALDebugLog( "[SPI_RECV] start...");
    //mutex_lock(&spi_mutex);
    while(HaveReadByteCnt < len)
    {
        if(len == AP_USE_ANTARES3_FORMAT_TO_DECIDE_RCV_CNT)
        {
            ThisTimeRcvByteCnt = 2;    // temp read two byte to decide Len
        }
        else
        {
            ThisTimeRcvByteCnt = MIN((len - HaveReadByteCnt), A3_PROTOCOL_BURST_BYTECNT);
        }
        PNALDebugLog( "[SPI_RECV] start to GetByte...");
        #if 0
        /* Wait Fw Ready to Send..... */
        for(delay = WAITING_PROCESSING_MAX_TIMES;delay;--delay)
        {
            GetByte = 0xFF;
            if((GetByte = SPI_A3Portocol_ReadByte(A3_PROTOCOL_FW_READY_POS)) == 0x01)    // Fw has set this byte , so can // read....
            {
                break;
            }
            //msleep(1);
            //usleep(1000);
            //mysleep(3);
        }
        #else
        /* Wait Fw Ready to Send..... */
        while(1)
        {
            GetByte = 0xFD;
            if((GetByte = SPI_A3Portocol_ReadByte(A3_PROTOCOL_FW_READY_POS)) == 0x01)    // Fw has set this byte , so can // read....
            {
                break;
            }
            else{
                #if 0
                showByteCnt++;
                showByteCnt%= 1000;
    			if( showByteCnt == 0){
    			    PNALDebugLog( "[SPI_RECV] GetByte=%02X", GetByte);
    			}
    	    	#endif
            }
			gettimeofday(&cur_tv,NULL);
			if( timeDiff(&start_tv, &cur_tv)>timeout){
				break;
			}
        }
        #endif
        PNALDebugLog( "[SPI_RECV] A3_PROTOCOL_FW_READY_POS done, GetByte= %x", GetByte);
        if(GetByte == 0x01) // go out because Fw ready
        {
            for( cnt1=0; cnt1 < ThisTimeRcvByteCnt; cnt1++ )
            {
                 /* Start Rcv data */
                buf[HaveReadByteCnt + cnt1] = SPI_A3Portocol_ReadByte(cnt1+A3_PROTOCOL_DATA_POS);
                //msleep(1);
                //mysleep(1);
            }

            if(len == AP_USE_ANTARES3_FORMAT_TO_DECIDE_RCV_CNT)
            {
                len = buf[1] + 4;   // Get len , so update variable len...
                ThisTimeRcvByteCnt = MIN((len - HaveReadByteCnt), A3_PROTOCOL_BURST_BYTECNT); //update variable ThisTimeRcvByteCnt...
                for( ; cnt1 < ThisTimeRcvByteCnt; cnt1++ )
                {
                    buf[HaveReadByteCnt + cnt1] = SPI_A3Portocol_ReadByte(cnt1+1);
                }
            }

            /* OK, Rcv all, so clear ready to info Fw send next ...  */
            SPI_A3Portocol_APClearFwReady();

            HaveReadByteCnt += ThisTimeRcvByteCnt;
        }
        else    // go out because time out
        {
            spi_rx_hlt_cnt++;
            //MS_PowerOff();
            //usleep(10000);      //wait 10ms for hardware down time
            //MS_PowerOn();
            //usleep(300000);     //wait 100ms for MSR3110 to bringup
            SPI_Reset();
            buf[0] = 0xaa;      //reponse with INTERNAL_RESET: 0xaa,0x01,0x00
            buf[1] = 0x01;
            buf[2] = 0x00;
            buf[3] = 0xab;
            buf[4] = 0x00;
            HaveReadByteCnt = 5;

            break;
        }
        if( spi_errorno < 0){
            HaveReadByteCnt = spi_errorno;
            break;
        }
    }
    //mutex_unlock(&spi_mutex);

    SPI_DUMPBUF("SPI_RECV:", buf, HaveReadByteCnt);
    return HaveReadByteCnt;

}

void SPI_ResetFD(void)
{
    int                 ret;
    /* add SPI initialization here. */
    close(spi_fd);
    spi_fd = open(DEV_NAME_SPI_DEV, O_RDWR);

	PNALDebugLog("Reset SPI port id : %d", spi_fd);
	if (spi_fd < 0)
	{
        PNALDebugError("can't open SPI device");
	}


    close(controlfd);
    controlfd = open(DEV_NAME_SPI_CON, O_RDWR);
	PNALDebugLog("Reset control id : %d", controlfd);
	if (controlfd < 0)
	{
        PNALDebugError("can't open control device");
	}



}
void SPI_Reset( void )
{
	PNALDebugLog("SPI_Reset()");
    spi_errorno = 0;
    spi_rx_cnt = 0;
    spi_rx_err_cnt = 0;
    spi_rx_hlt_cnt = 0;

    MS_PowerOn();
	usleep(100000); //wait for 100ms
    MS_PowerOff();
	usleep(100000); //wait for 100ms
    MS_PowerOn();
    usleep(300000); //wait for 300ms
    SPI_ResetFD();
}

int SPI_Init( int index )
{
    SPI_Reset();
    return 0;
}


void SPI_Close( void )
{
    PNALDebugLog("SPI_Close fd : %d", spi_fd);
    close(spi_fd);
    MS_PowerOff();
}
int SPI_State( void )
{
    return spi_errorno;
}

#if 0
//=========  public functions  ========================
int ms_interfaceTranseive( unsigned int len, unsigned char *buf, int timeout )
{
    int slen=0, rlen=0;
    int retrycnt=3;

	int spiRetryCount = 0;


SpiSending:
    //slen = SPI_SEND(_SPI_TX_Len, _SPI_TX_BUFFER);
    //if( slen != _SPI_TX_Len){
    //    PNALDebugError("[Interface] ERROR!!!! data not sent!!! slen: %d, _SPI_TX_Len: %d", slen, _SPI_TX_Len);
    //    return 0;
    //}
    slen = SPI_SEND(_SPI_TX_Len, _SPI_TX_BUFFER);
	while( ( slen != _SPI_TX_Len) && ( spiRetryCount < 3))
	{
        PNALDebugError("[Interface] ERROR!!!! data not sent!!! slen: %d, _SPI_TX_Len: %d", slen, _SPI_TX_Len);
		PNALDebugError("SPI Reset and Resend. spiRetryCount: %d", spiRetryCount);
		SPI_Reset();
		slen = SPI_SEND(_SPI_TX_Len, _SPI_TX_BUFFER);
		spiRetryCount++;
    }
	if( ( slen != _SPI_TX_Len) && ( spiRetryCount >= 3))
	{
		PNALDebugError("[Interface] ERROR!!!! data not sent & spiRetryCount >= 3!!! slen: %d, _SPI_TX_Len: %d", slen, _SPI_TX_Len);
		return 0;
	}

    spi_rx_cnt++;
    rlen = SPI_RECV(len, buf, timeout);

    if( rlen >=2 && Checksum_is_correct(buf, (unsigned short )rlen-2) ){
        PNALDebugLog( "[IO]recv checksum ok, len:%d\n", rlen-2);
        if( (buf[0] == 0x02) && (buf[1] == 0x01) && (buf[2] == 0x09) ){ //check sum error
            PNALDebugLog( "[IO]Firmware: check sum error, retry:%d\n", retrycnt);
            retrycnt--;
            if( retrycnt >0 ){
                goto SpiSending;
            }
        }
        if( (buf[0] == 0x02) && (buf[1] == 0x01) && (buf[2] == 0x17) ){ //check sum error
            PNALDebugLog( "[IO]Firmware: data len error, retry:%d\n", retrycnt);
            retrycnt--;
            if( retrycnt >0 ){
                goto SpiSending;
            }
        }
    }else{
        PNALDebugLog( "[IO]err crc, recvlen = %d\n", rlen);
        spi_rx_err_cnt++;
        retrycnt--;
        if( retrycnt >0 ){
            goto SpiSending;
        }

    }
    PNALDebugLog( "[IO]=========Err/Hlt/Total %d/%d/%d ================", spi_rx_err_cnt,spi_rx_hlt_cnt,spi_rx_cnt);

    return rlen;
}
#endif
int ms_interfaceInit(int i)
{
	PNALDebugLog("%s -- start \n", __FUNCTION__);
	
    int retVal = 0;
	
    switch( i)
    {
    	case 0:
			PNALDebugLog("%s power on mode. \n", __FUNCTION__);
			MS_PowerOn();
			retVal = I2C_Init( 0);
			goto end;
			
			break;

		case 1:
			PNALDebugLog("%s non power on mode. \n", __FUNCTION__);
			retVal = I2C_Init( 0);
			goto end;
			
			break;
			
		default:
			PNALDebugLog("%s error mode \n", __FUNCTION__);
			retVal = 0;
			goto end;

			break;
			
			
    }

end:
	PNALDebugLog("%s -- end \n", __FUNCTION__);
    return retVal;
}

void ms_interfaceClose( void )
{
    SPI_Close();
}
#if 0
struct spidev_data {
	dev_t			devt;
	spinlock_t		spi_lock;
	struct spi_device	*spi;
	struct list_head	device_entry;

	/* buffer is NULL unless this device is open (users > 0) */
	struct mutex		buf_lock;
	unsigned		users;
	u8			*buffer;
};
#endif
//int ms_interfaceSend(U8 *buf, int len)
//int ms_interfaceSend(struct spidev_data *spidev, U8 *buf, int len)
//int ms_interfaceSend(struct spidev_data *spidev, int len)
int ms_interfaceSend(tNALMsrComInstance  *dev, int len)
{
//    unsigned char buf[260];
    u16 chk =0;
    //CNALSyncEnterCriticalSection(&dev->mutex);
    int i=0;


    if( dev->ant_recv_started ){
        PNALDebugError("[Interface] ERROR!!!! interface BUSY!!!");
        return 0;
    }

    memcpy( _SPI_TX_BUFFER, dev->ant_send_buffer, len);
    _SPI_TX_Len = len+2;
    for(i=0;i<len;i++)
        chk += _SPI_TX_BUFFER[i];
    _SPI_TX_BUFFER[len] = chk & 0xff;
    _SPI_TX_BUFFER[len+1] = (chk>>8)&0xff;

    dev->ant_recv_started = W_TRUE;
    //i = SPI_SEND(_SPI_TX_Len, _SPI_TX_BUFFER);
    sem_post(&dev->ant_rx_sem);   //wakeup io thread
    //CNALSyncLeaveCriticalSection(&dev->mutex);
    //PNALDebugLog( "[ms_interfaceSend]dev->ant_rx_sem = %x", &dev->ant_rx_sem);
    return i;
}

void ms_DeviceReset()
{
	PNALDebugWarning("[ms_DeviceRest]RFID device reset.");
	SPI_Reset();
}

bool_t Checksum_is_correct( unsigned char *buf, unsigned short len )
{
    int i;
    unsigned short sum1=0, sum2;

    for( i=0; i<len; i++ )
    {
        sum1 += *(buf+i);
    }

    sum2 = *(buf+len+1);
    sum2 <<= 8;
    sum2 += *(buf+len);

    return (sum1 == sum2);
}

//int ms_interfaceRecv(U8 *buf, int maxlen, int timeout)
int ms_interfaceRecv(tNALMsrComInstance  *dev, int maxlen, int timeout)
{
    while( dev->nb_available_bytes <=0 && dev->shutdown== W_FALSE){
        //mutex_unlock(&dev->mutex);
        PNALDebugLog( "[INT_R]waits for ant_rx_done_sem");
        sem_wait(&dev->ant_rx_done_sem);
        //if (down_interruptible(&dev->ant_rx_done_sem) == -EINTR){
        //    PNALDebugError( "down_interruptible failed (due to reception of a signal)");
        //}
        //mutex_lock(&dev->mutex);
    }
    PNALDebugLog( "ms_interfaceRecv return %d", dev->nb_available_bytes);
    return dev->nb_available_bytes;
}

//I2C Func
#if 1
void I2C_ResetFD(void)
{
    int                 ret;
    /* add I2C initialization here. */
    close(i2c_fd);
    i2c_fd = open(DEV_NAME_I2C_DEV, O_RDWR);

	PNALDebugLog("Reset I2C port id : %d", i2c_fd);
	if (i2c_fd < 0)
	{
        PNALDebugError("can't open I2C device");
	}
}

void I2C_Reset( void )
{
	PNALDebugLog("I2C_Reset()");
    spi_errorno = 0;
    spi_rx_cnt = 0;
    spi_rx_err_cnt = 0;
    spi_rx_hlt_cnt = 0;
    I2C_ResetFD();

	//MS_PowerOff();
	//usleep(100000); //wait for 100ms
    //MS_PowerOn();
    //usleep(300000); //wait for 300ms

	MS_Reset();
    
}

int I2C_Init( int index )
{
	PNALDebugLog("I2C_Init()");
    spi_errorno = 0;
    spi_rx_cnt = 0;
    spi_rx_err_cnt = 0;
    spi_rx_hlt_cnt = 0;
	i2c_reset_cnt = 0;
    I2C_ResetFD();
    //MS_PowerOn();
    //usleep(300000); //wait for 300ms
    return 0;
}

int I2C_SEND(U32 len,U8 *buf)
{
    U32 HaveSendByteCnt = 0;
    U8 ThisTimeSendByteCnt;
    int cnt1;
	int status = 0;
    struct timeval start_tv, cur_tv;
    SPI_DUMPBUF("I2C_SEND:", buf, len);
#if 1
    //Get time value for timeout
    gettimeofday(&start_tv,NULL);
	while(1)
	{
		status	= write(i2c_fd, buf, len);
		if( status != (int)len){
			PNALDebugLog( "[i2c w] err: write:0x%02x, i2c_fd:%d, status:%d", buf[0], i2c_fd, status);
		}
		else
		{
			PNALDebugLog( "[i2c w] wrtie done");
			break;
		}
		gettimeofday(&cur_tv,NULL);
		if( timeDiff(&start_tv, &cur_tv) > I2C_SEND_TRY_CNT_MAX){
			return status;
		}
		//usleep(500); //wait for 0.5ms
	}
#else
	for(cnt1=0; cnt1<I2C_SEND_TRY_CNT_MAX; cnt1++)
	{
		status	= write(i2c_fd, buf, len);
		if( status != (int)len){
			PNALDebugLog( "[i2c w] err: write:0x%02x, i2c_fd:%d, status:%d", buf[0], i2c_fd, status);
		}
		else
		{
			PNALDebugLog( "[i2c w] wrtie done");
			break;
		}
		usleep(500); //wait for 0.5ms
	}

#endif	
    spi_errorno = status;
	
    return status;
}

int I2C_RECV( unsigned int len,U8 *buf, int timeout )
{
    U32 HaveReadByteCnt;
    U8 ThisTimeRcvByteCnt;
    U8 cnt1;
    int delay;
    int retry_cnt = 0;
    struct timeval start_tv, cur_tv;
    int showByteCnt=0;
    int status = 0;

spi_recv_start:
    HaveReadByteCnt = 0;
	//Get time value for timeout
	gettimeofday(&start_tv,NULL);

    PNALDebugLog( "[I2C_RECV] start...");
    //mutex_lock(&spi_mutex);
    while(HaveReadByteCnt < len)
    {
        if(len == AP_USE_ANTARES3_FORMAT_TO_DECIDE_RCV_CNT)
        {
            ThisTimeRcvByteCnt = 2;    // temp read two byte to decide Len
        }
        else
        {
            ThisTimeRcvByteCnt = MIN((len - HaveReadByteCnt), A3_I2C_RECV_MAX);
        }
        PNALDebugLog( "[I2C_RECV] start to GetByte...");
        /* Wait Fw Ready to Send..... */
        while(1)
        {
            if((status = read(i2c_fd, buf, ThisTimeRcvByteCnt))== ThisTimeRcvByteCnt)    // Fw has set this byte , so can // read....
            {
                break;
            }
			gettimeofday(&cur_tv,NULL);
			if( timeDiff(&start_tv, &cur_tv)>timeout){
				//PNALDebugLog( "[I2C_RECV] recv first 2 byte time out");
				return status;
			}
        }		 
		//PNALDebugLog( "[I2C_RECV] recv first 2 byte, status=%d", status);
		//if( status < 0){
        //    return status;            
        //}
		HaveReadByteCnt+=ThisTimeRcvByteCnt;
        if(len == AP_USE_ANTARES3_FORMAT_TO_DECIDE_RCV_CNT)
        {
            len = buf[1] + 4;   // Get len , so update variable len...
            ThisTimeRcvByteCnt = len - HaveReadByteCnt; //update variable ThisTimeRcvByteCnt...
            status  = read(i2c_fd, buf+status, ThisTimeRcvByteCnt);
			PNALDebugLog( "[I2C_RECV] recv data, status=%d, ThisTimeRcvByteCnt=%x", status, ThisTimeRcvByteCnt);
			if( status < 0){
                return status;            
            }
        }
        HaveReadByteCnt += ThisTimeRcvByteCnt;
    }

    SPI_DUMPBUF("I2C_RECV:", buf, HaveReadByteCnt);
    return HaveReadByteCnt;

}

//=========  public functions  ========================
int ms_interfaceTranseive( unsigned int len, unsigned char *buf, int timeout )
{
    int slen=0, rlen=0;
    int retrycnt=3;

	int spiRetryCount = 0;


SpiSending:
	WT_ResentDetectCmd = W_FALSE;
    slen = I2C_SEND(_SPI_TX_Len, _SPI_TX_BUFFER);
	while( ( slen != _SPI_TX_Len) && ( spiRetryCount < 3))
	{
        PNALDebugError("[Interface] ERROR!!!! data not sent!!! slen: %d, _I2C_TX_Len: %d", slen, _SPI_TX_Len);
		PNALDebugError("I2C Reset and Resend. spiRetryCount: %d", spiRetryCount);
		i2c_reset_cnt++;
		PNALDebugError("[Interface] I2C Reset Cnt: %d", i2c_reset_cnt);
		I2C_Reset();
		WT_ResentDetectCmd = W_TRUE;
		slen = I2C_SEND(_SPI_TX_Len, _SPI_TX_BUFFER);
		spiRetryCount++;
    }
	if( ( slen != _SPI_TX_Len) && ( spiRetryCount >= 3))
	{
		PNALDebugError("[Interface] ERROR!!!! data not sent & spiRetryCount >= 3!!! slen: %d, _I2C_TX_Len: %d", slen, _SPI_TX_Len);
		return 0;
	}

    spi_rx_cnt++;
    rlen = I2C_RECV(len, buf, timeout);

    if( rlen >=2 && Checksum_is_correct(buf, (unsigned short )rlen-2) ){
        PNALDebugLog( "[IO]recv checksum ok, len:%d\n", rlen-2);
        if( (buf[0] == 0x02) && (buf[1] == 0x01) && (buf[2] == 0x09) ){ //check sum error
            PNALDebugLog( "[IO]Firmware: check sum error, retry:%d\n", retrycnt);
            retrycnt--;
            if( retrycnt >0 ){
                goto SpiSending;
            }
        }
        if( (buf[0] == 0x02) && (buf[1] == 0x01) && (buf[2] == 0x17) ){ //check sum error
            PNALDebugLog( "[IO]Firmware: data len error, retry:%d\n", retrycnt);
            retrycnt--;
            if( retrycnt >0 ){
                goto SpiSending;
            }
        }
    }else{
        PNALDebugLog( "[IO]err crc, recvlen = %d\n", rlen);
        spi_rx_err_cnt++;
        retrycnt--;
        if( retrycnt >0 ){
            goto SpiSending;
        }
        else
        {
            PNALDebugError("[Interface]I2C Recv Retrycnt: %d", spi_rx_err_cnt);
            i2c_reset_cnt++;
            PNALDebugError("[Interface] I2C Reset Cnt: %d", i2c_reset_cnt);
            I2C_Reset();
        }
    }
    PNALDebugLog( "[IO]=========Err/Hlt/Total %d/%d/%d ================", spi_rx_err_cnt,spi_rx_hlt_cnt,spi_rx_cnt);

    return rlen;
}

int ms_chk_dev_IRQ()
{
    int res = 1;
	res = IRQ_ON();
	PNALDebugLog("[ms_chk_dev_IRQ]res= %d]", res);
	return res;
}

int ms_IRQ_abort()
{
    int res = 0;
	int i = 0;
	for (i=0; i<IRQ_ABORT_TRY_CNT_MAX; i++)
	{
	    res = IRQ_ABORT();
	    PNALDebugLog("[ms_IRQ_abort]res= %d]", res);
		if (res == 0x01)
		{
		    return res;
		}	
		usleep(1000); //wait for 1ms
	}
	return res;
}

#endif


