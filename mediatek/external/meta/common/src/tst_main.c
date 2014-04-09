/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/


/*******************************************************************************
 *
 * Filename:
 * ---------
 *   tst_main.c
 *
 * Project:
 * --------
 *   YUSU
 *
 * Description:
 * ------------
 *    driver main function
 *
 * Author:
 * -------
 *   Lu.Zhang (MTK80251) 09/11/2009
 *
 *------------------------------------------------------------------------------
 * $Revision:$
 * $Modtime:$
 * $Log:$
 *
 * 03 12 2012 vend_am00076
 * [ALPS00251394] [Patch Request]
 * trunk ics
 *
 * 03 02 2012 vend_am00076
 * NULL
 * .
 *
 * 05 27 2010 lu.zhang
 * [ALPS00005327]CCAP
 * .
 *
 * 05 11 2010 lu.zhang
 * [ALPS00005327]CCAP
 * .
 *
 * 04 30 2010 lu.zhang
 * [ALPS00005327]CCAP
 * .
 *
 * 04 24 2010 lu.zhang
 * [ALPS00005327]CCAP
 * [ALPS00005327] CCAP
 * .
 *
 * 04 01 2010 lu.zhang
 * [ALPS00004362]CCAP APIs
 * .
 *
 * 03 18 2010 lu.zhang
 * [ALPS00004362]CCAP APIs
 * for CCAP APIs
 *
 * 01 21 2010 lu.zhang
 * [ALPS00004332]Create META
 * .
 *
 * 01 20 2010 lu.zhang
 * [ALPS00004332]Create META
 * .
 * u1rwduu`wvpghlqg|ip+mdkb
 *
 *
 *
 *
 *
 *
 *******************************************************************************/


//
// TST stream device driver.
//


#include "tst_main.h"


#include "WM2Linux.h"
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <limits.h>
#include <errno.h>
#include <string.h>
#include <dirent.h>
#include <termios.h> /* POSIX terminal control definitions */

#include <pthread.h>
#include <DfoDefines.h>


// the function declare.
int  TST_SerInit(void );
int  TST_Deinit( void );
void * UsbComRxThread( void* lpParameter );
void * MciComRxThread1( void* lpParameter );
void * MciComRxThread2( void* lpParameter );

void TSTParseReceiveData(kal_uint8 *buf_ptr,
                         kal_uint16 input_len,
                         kal_uint8 *frame_type,
                         kal_uint8 *checksum,
                         kal_uint8 **cmd_ptr,
                         kal_uint8 **local_ptr,
                         kal_uint8 **peer_ptr,
                         kal_uint16 *pFrameLength);
void TSTParseMDData(void *pdata, kal_int16 len, int modemNum );

void TSTMuxPrimitiveMDData(void *pdata, kal_int16 len);
void MD_SIM_CHECK_REQ(unsigned char *pdata, kal_int16 len, int modemIndex);
void MD_SIM_CHECK_CNF(META_RX_DATA *pMuxBuf,int i, int modemIndex);
void MD_SIM_CHECK_PARSE_FRAME(META_RX_DATA *pMuxBuf, int modemIndex);

unsigned char g_AP_RECEIVE_MD_SIN_CHECK[MODEM_COUNT] = {0};
int gFlag[MODEM_COUNT] = {0};
int comPortType = 0;
int gModemType = 0;
int USBFlag = 0;
int g_nSendModemIndex = 0;
int g_nModemDataType = 0; //0:PS data, 1:L1 data

kal_uint8 frameHeader[MODEM_COUNT][FrameMaxSize];

kal_uint16   u16ModemframeLength[MODEM_COUNT]={0};
kal_uint16 ModemFrameLength[MODEM_COUNT] = {0};
kal_uint8	Modemchecksum[MODEM_COUNT] = {0};



pthread_mutex_t META_USBPort_Mutex = PTHREAD_MUTEX_INITIALIZER;


pthread_mutex_t META_ComPortMD_Mutex = PTHREAD_MUTEX_INITIALIZER;



int meta_exit_thread(pthread_t arg)
{   /* exit thread by pthread_kill -> pthread_join*/
    int err;
    if ((err = pthread_kill(arg, SIGUSR1)))
        return err;

    if ((err = pthread_join(arg, NULL)))
        return err;
    return 0;
}

void dumpData(const unsigned char* con, int length)
{
	META_LOG("Dump data is:  ");
	int i = 0;
	for(i = 0; i < length; i++)
		printf(" (%02x) ",con[i]);
	META_LOG("Dump finished!");


}

void dumpDataInHexString(const unsigned char* con, int length, unsigned int bytesPerRow)
{
	int i = 0;
	unsigned int j = 0;
	unsigned int rowLength = 3 * bytesPerRow + 1;
	unsigned char hex[rowLength];
	unsigned char high;
	unsigned char low;
	for(i = 0; i < length; i++)
	{
		high = (con[i] >> 4);
		low = (con[i] & 0x0f);
		
		if(high < 0x0a)
            high += 0x30;
        else
            high += 0x37;
        
        if(low < 0x0a)
            low += 0x30;
        else
            low += 0x37;
        
        hex[j++] = high;
        hex[j++] = low;
        hex[j++] = ' ';

		if (j == rowLength - 1 || i == length - 1)
		{
			hex[j] = '\0';
			j = 0;
			META_LOG("%s", hex);
		}
	}
	META_LOG("Dump finished!");	
}

static int is_USB_State_PlusIn(void)
{
    int type = 0;
    char buf[11];
    int bytes_read = 0;
    int res = 0;
    int fd = open("/sys/class/android_usb/android0/state", O_RDONLY);
    if (fd != -1)
    {
        memset(buf, 0, 11);
        while (bytes_read < 10)
        {
            res = read(fd, buf + bytes_read, 10);
            if (res > 0)
                bytes_read += res;
            else
                break;
        }
        close(fd);
        type = strcmp(buf,"CONFIGURED");

        META_LOG("[TST_DRV]Query USB State OK.");
    }
    else
    {
        META_LOG("[TST_DRV]Failed to open:/sys/class/android_usb/android0/state");
    }
         
	return (type == 0);     
}




/********************************************************************************
//FUNCTION:
//		WinMain
//DESCRIPTION:
//		this function is called when tst module is loading. it will create all thread and get system resources.
//
//PARAMETERS:
//		None
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/
int main(void)
{
	
	META_LOG("[TST_Drv:] Enter meta_tst init flow!");
	META_LOG("[TST_Drv:] Support Dual Modem!");	
	umask(000);

    // init the uart, usb and mci port
    TST_SerInit();

	if(pthread_create(&g_hUSBComTh,NULL,UsbComRxThread,NULL) != 0)
	{
		META_LOG("[TST_Drv:] main:Create USB thread failed");
		return 0;
	}
    //create the uart/usb RX thread from PC and MCI thread from modem

	if(gModemType == DUALMODEM ||gModemType == MODEMONEONLY)
	{
		if(pthread_create(&g_hMCIComTh,NULL,MciComRxThread1,NULL) != 0)
		{
		     META_LOG("[TST_Drv:] main:Create MCICom1 thread failed");
		     return 0;
	        }
	}

	if(gModemType == DUALMODEM ||gModemType == MODEMTWOONLY)
	{
		if(pthread_create(&g_hMCIComThModem2,NULL,MciComRxThread2,NULL) != 0)
	        {
		      META_LOG("[TST_Drv:] main:Create MCICom2 thread failed");
		      return 0;
	        }
	}

  	META_LOG("[TST_Drv:] TstMainThread create success ");

    //the main thread. it will recived the data from cci Rx thread.
    while (!g_bTerminateAll)
    {	
		sleep(5);

		if(comPortType == META_USB_COM)
		{
			
		
			if(is_USB_State_PlusIn())
			{
				if(!USBFlag)
				{
					sleep(1);
					if (pthread_mutex_lock (&META_USBPort_Mutex))
					{
						META_LOG( "META_MAIN META_USBPort_Mutex Lock ERROR!\n"); 
					}
						//get the USB port fd
					g_hUsbComPort = open("/dev/ttyGS0",O_RDWR | O_NOCTTY | O_NDELAY);
					if (g_hUsbComPort == -1) 
					{
						META_LOG("TST_SerInit:open_port: Unable to open USB!");
						META_LOG("error code is %d",errno);
					} 
					else 
					{
						initTermIO(g_hUsbComPort);
						USBFlag = 1;
						META_LOG("Create and open USB port success!");
						FTT_Init(g_hUsbComPort);
					}
						
					if (pthread_mutex_unlock (&META_USBPort_Mutex))
					{
						META_LOG( "META_Main META_USBPort_Mutex Unlock ERROR!\n"); 
					}
						
				}
			}
			else
			{
				if(USBFlag)
				{
					if (pthread_mutex_lock (&META_USBPort_Mutex))
					{
						META_LOG( "META_MAIN META_USBPort_Mutex Lock ERROR!\n"); 
					}
					close(g_hUsbComPort);
					USBFlag = 0;
					META_LOG("USB cable plus out!");
					if (pthread_mutex_unlock (&META_USBPort_Mutex))
					{
						META_LOG( "META_Main META_USBPort_Mutex Unlock ERROR!\n"); 
					}
				}
			}			
			
		}
		
		
    }

    TST_Deinit();  // error exit
    return 1;


}
/********************************************************************************
//FUNCTION:
//		TST_Deinit
//DESCRIPTION:
//		this function is release the system resource when exit.
//
//PARAMETERS:
//		None
//
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/
int  TST_Deinit( void )
{
    META_LOG("[TST_Drv:] TST_Deinit");

    //release the handle
    if (g_hUsbComPort != -1)
    {
        close(g_hUsbComPort);
		g_hUsbComPort = -1;
    }
    if (g_hUsbComPort2 != -1)
    {
        close(g_hUsbComPort2);
		g_hUsbComPort2 = -1;
    }
    if (g_hMciComPort != -1)
    {
        close(g_hMciComPort);
		g_hMciComPort = -1;
    }
	
	if (g_hMciComPort2 != -1)
    {
        close(g_hMciComPort2);
		g_hMciComPort2 = -1;
    }
	
    if (g_hUSBComTh != -1)
        meta_exit_thread(g_hUSBComTh);
	if (g_hMCIComTh != -1)
		meta_exit_thread(g_hMCIComTh);

    return 1;

}

int getBootMode(void)
{
	int mode = -1;
	char buf[BOOT_MODE_STR_LEN + 1];
	int bytes_read = 0;
	int res = 0;
	int fd = open(BOOT_MODE_INFO_FILE, O_RDONLY);
	if (fd != -1)
	{
		memset(buf, 0, BOOT_MODE_STR_LEN + 1);
		while (bytes_read < BOOT_MODE_STR_LEN)
		{
			res = read(fd, buf + bytes_read, BOOT_MODE_STR_LEN);
			if (res > 0)
				bytes_read += res;
			else
				break;
		}
		close(fd);
		mode = atoi(buf);
	}
	else
	{
		META_LOG("Failed to open boot mode file %s", BOOT_MODE_INFO_FILE);
	}
	return mode;
}

int getComportType(void)
{
	int type = 0;
	char buf[COM_PORT_TYPE_STR_LEN + 1];
	int bytes_read = 0;
	int res = 0;
	int fd = open(COM_PORT_TYPE_FILE, O_RDONLY);
	if (fd != -1)
	{
		memset(buf, 0, COM_PORT_TYPE_STR_LEN + 1);
		while (bytes_read < COM_PORT_TYPE_STR_LEN)
		{
			res = read(fd, buf + bytes_read, COM_PORT_TYPE_STR_LEN);
			if (res > 0)
				bytes_read += res;
			else
				break;
		}
		close(fd);
		type = atoi(buf);
	}
	else
	{
		META_LOG("Failed to open com port type file %s", COM_PORT_TYPE_FILE);
	}
	META_LOG("com port type: %d", type);
	return type;	

//	META_LOG("Always return 2 for USB mode");
//    return 2;
}

int getModemType()
{
    if (MTK_ENABLE_MD1)
    {
        if (MTK_ENABLE_MD2)
	    {
	return DUALMODEM;
        }
        else
	    {
	return MODEMONEONLY;
        }
    }
	
    if (MTK_ENABLE_MD2)
	{
	return MODEMTWOONLY;
    }
	
    return 0;
	
}

void MutexLock(pthread_mutex_t* pmutex)
{
    if (pmutex != NULL)
    {
        if (pthread_mutex_lock (pmutex))
		{
			META_LOG( "Err: MutexLock ERROR!\n"); 
        }
    }
}

void MutexUnLock(pthread_mutex_t* pmutex)
{
    if (pmutex != NULL)
    {
        if (pthread_mutex_unlock (pmutex))
		{
			META_LOG( "Err: MutexUnLock ERROR!\n"); 
		}
    }
}


void initTermIO(int portFd)
{
	struct termios termOptions;
	fcntl(portFd, F_SETFL, 0);
	// Get the current options:
	tcgetattr(portFd, &termOptions);

	// Set 8bit data, No parity, stop 1 bit (8N1):
	termOptions.c_cflag &= ~PARENB;
	termOptions.c_cflag &= ~CSTOPB;
	termOptions.c_cflag &= ~CSIZE;
	termOptions.c_cflag |= CS8 | CLOCAL | CREAD;

	// Raw mode
	termOptions.c_iflag &= ~(INLCR | ICRNL | IXON | IXOFF | IXANY);
	termOptions.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);  /*raw input*/
	termOptions.c_oflag &= ~OPOST;  /*raw output*/


	tcflush(portFd,TCIFLUSH);//clear input buffer
	termOptions.c_cc[VTIME] = 100; /* inter-character timer unused */
	termOptions.c_cc[VMIN] = 0; /* blocking read until 0 character arrives */


	cfsetispeed(&termOptions, B921600);
    cfsetospeed(&termOptions, B921600);
	/*
	* Set the new options for the port...
	*/
	tcsetattr(portFd, TCSANOW, &termOptions);
}

/********************************************************************************
//FUNCTION:
//		TST_SerInit
//DESCRIPTION:
//		this function is called initial the uart port and mci port
//
//PARAMETERS:
//		None
//RETURN VALUE:
//		TRUE is success, otherwise is fail
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		g_hMciComPort, g_hMciComPort will be got from here.
********************************************************************************/
int  TST_SerInit(void )
{
    int 	iErrorCode = 0;
    g_AP_RECEIVE_MD_SIN_CHECK[0] = 0;
    g_AP_RECEIVE_MD_SIN_CHECK[1] = 0;
    g_cTstFrameState = RS232_FRAME_STX;
    g_cMDFrameState[0] = RS232_FRAME_STX;
    g_cMDFrameState[1] = RS232_FRAME_STX;
    memset(frameHeader,'\0',FrameMaxSize*MODEM_COUNT);
    memset(&g_stPortCfg, 0, sizeof(g_stPortCfg));
    strcpy(g_stPortCfg.wcComPort, "/dev/ttyMT0");
    g_bLogEnable = g_stPortCfg.bLogEnable;

    META_LOG("TST_SerInit");

	int bootMode = getBootMode();
	META_LOG("Boot mode = %d", bootMode);
	if(bootMode != BOOT_MODE_META && bootMode != BOOT_MODE_ADV_META)
	{
		META_LOG("Boot mode is ivalid %d", bootMode);
		goto error;
	}
	
	comPortType= getComportType();
	META_LOG("Boot mode is %d", bootMode);
	META_LOG("Com port type is %d",comPortType);
        gModemType = getModemType();
	META_LOG("Modem type = %d",gModemType);
	if(gModemType != DUALMODEM && gModemType != MODEMONEONLY && gModemType != MODEMTWOONLY)
	{
		META_LOG("Modem type is ivalid %d", gModemType);
	}
	// If boot mode is 1(meta mode) or 5(advanced meta mode), try to open UART1
	if(comPortType == META_UART_COM)
	{
		if (bootMode == BOOT_MODE_META|| bootMode == BOOT_MODE_ADV_META)
		{
	    	//get the UART port fd
    		g_hUsbComPort = open(g_stPortCfg.wcComPort,O_RDWR | O_NOCTTY | O_NDELAY);
			if (g_hUsbComPort == -1) 
			{
        		/*the process should exit if fail to open UART device*/
				META_LOG("TST_SerInit:open_port: Unable to open UART1!");
				META_LOG("error code is %d",errno);
				iErrorCode= 1;
        		goto error;
			} 
			else 
			{
				initTermIO(g_hUsbComPort);
				META_LOG("Create and open UART1 port success!");
			}
		}
		else
		{
			g_hUsbComPort = -1;
		}
	}
	else if(comPortType == META_USB_COM)
	{
		//get the USB port fd
		while(!is_USB_State_PlusIn())
		{
			sleep(1);
		}
		
		g_hUsbComPort = open("/dev/ttyGS0",O_RDWR | O_NOCTTY | O_NDELAY);
		if (g_hUsbComPort == -1) 
		{
			/*the process should exit if fail to open USB device*/
			META_LOG("TST_SerInit:open_port: Unable to open USB!");
			META_LOG("error code is %d",errno);
			iErrorCode= 1;
			goto error;
		} 
		else 
		{
			initTermIO(g_hUsbComPort);
			META_LOG("Create and open USB port success!");
			USBFlag = 1;
		}
	}
	else
	{
		META_LOG("com port type invalid");	
		iErrorCode= 1;
		goto error;
	}

	

	if(gModemType == DUALMODEM ||gModemType == MODEMONEONLY)//Modem1 CCCI port
	{
	g_hMciComPort = open("/dev/ttyC1",O_RDWR | O_NOCTTY | O_NDELAY);

	if (g_hMciComPort == -1) 
	{
	     META_LOG("TST_SerInit:open_port: Unable to open Modem1 CCCI port!");
	     META_LOG("error code is %d",errno);
             goto error;
	} 
        else 
        {
		fcntl(g_hMciComPort, F_SETFL, 0);
		META_LOG("Create and open CCCI1 port success!");
	}
	}

	if(gModemType == DUALMODEM ||gModemType == MODEMTWOONLY)//Modem2 CCCI port
	{
		g_hMciComPort2 = open("/dev/ccci2_tty1",O_RDWR | O_NOCTTY | O_NDELAY);

		if (g_hMciComPort2 == -1) 
	        {
			META_LOG("TST_SerInit:open_port: Unable to open Modem2 CCCI port!");
			META_LOG("error code is %d",errno);
                        goto error;
	        } 
                else 
                {
			fcntl(g_hMciComPort2, F_SETFL, 0);
		        META_LOG("Create and open CCCI2 port success!");
	        }
	}

    return 1;

error:
    META_LOG("TST_SerInit failed");
    return 0;

}

/********************************************************************************
//FUNCTION:
//		TSTParseReceiveData
//DESCRIPTION:
//		this function is called to parset the data from PC side
//
//PARAMETERS:
//		buf_ptr: 			[IN]			the lid of nvram file
//		input_len:		[IN]			the buf size
//		frame_type: 		[OUT]		the type of frame
//		checksum:		[OUT]		the check sum
//		cmd_ptr: 		[OUT]		it is reserved.
//		local_ptr:			[OUT]		the local and peer data buffer
//		pFrameLength: 	[OUT]		the length of frame
//
//RETURN VALUE:
//		Nones
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		g_cTstFrameState will change according the recieved state
********************************************************************************/
void TSTParseReceiveData(kal_uint8 *buf_ptr,
                         kal_uint16 input_len,
                         kal_uint8 *frame_type,
                         kal_uint8 *checksum,
                         kal_uint8 **cmd_ptr,
                         kal_uint8 **local_ptr,
                         kal_uint8 **peer_ptr,
                         kal_uint16 *pFrameLength)
{

    kal_uint16	u16Length=0;
    kal_uint8	*src=buf_ptr;
    kal_uint8	ch;
    kal_uint32 KAL_Max_Ctrl_Buf_Size=MAX_QUEUELEN; 	//the max ctrl buf size, queried by APIs
    kal_uint32 discard_word=0;  					//record the discard characters to check instable HW
    META_RX_DATA sTempRxbuf ;
    char *pTmpbuf = NULL;
    static int local_len = 0;
    static int peer_len = 0;
    static kal_uint16 data_cnt=0;
    static kal_uint16 data_total=0;
    int cbWriten = 0;
    int i=0;
    int SIMmodemIndex =0; 

    if((buf_ptr == NULL)||(src == NULL))
    {
        META_LOG("Err: TSTParseReceiveData buf_ptr is NULL");
        return;
    }

    while ( u16Length != input_len )
    {

        ch = *src;
        u16Length ++;

		// Siyang Miao added for data dumping
		g_cMetaFrameRxBuffer[g_iMetaFrameRxBufIndex++] = ch;
		
        if ( ch == STX_OCTET )
        {
            /* STX_OCTET only shows up in the header.
            * If the STX_OCTEX occurs in the frame content, PC will perform escaping.
            * When target receives this STX_OCTEX, it will enter MUX state.
            */
            /*  handle the data lost condition */
            if ( g_cTstFrameState != RS232_FRAME_CHECKSUM)
            {
                if (g_cTstFrameState ==RS232_FRAME_COMMAND_DATA)
                {
                }
                else if ((g_cTstFrameState == RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA)
                         || (g_cTstFrameState ==RS232_FRAME_AP_PRIM_PEER_DATA))
                {
                    if (g_sRs232Frame.buf_ptr!=NULL)
                    {
                        free(g_sRs232Frame.buf_ptr);
                        g_sRs232Frame.buf_ptr = NULL;
                    }
                }
                else if (g_cTstFrameState==RS232_FRAME_UT_DATA)
                {

                }
            }

			// Siyang Miao added for data dumping
			g_cMetaFrameRxBuffer[0] = ch;
			g_iMetaFrameRxBufIndex = 1;

            g_cTstFrameState = RS232_FRAME_LENHI;
            src++;
            ch = *src;
            *pFrameLength = 0;
            *checksum = STX_OCTET;

            /* Check if the command buffer is NULL or not. If the command buffer is not NULL,
            the command buffer should be release back to the debug partition pool
            Spancer: this part is not finish yet!*/

            { //Print if there's discarding characters to check instable HW issue
                char buf[32]={0};
                if (discard_word > 0)
                {
                    META_LOG("TR: Discards %d chars.", discard_word);
                    discard_word = 0;
                }
            }

            continue;

        }
        else
        {
            if ((*src == MUX_KEY_WORD ) && (g_cTstFrameState != RS232_FRAME_KEYWORD) && (g_cTstFrameState != RS232_FRAME_MD_DATA))
            { // enter MUX state(0x5A) and save the old

                g_cOldTstFrameState = g_cTstFrameState;
                g_cTstFrameState = RS232_FRAME_KEYWORD;

                src++;

                continue;
            }
            else if (g_cTstFrameState == RS232_FRAME_KEYWORD)
            {
                if (*src== MUX_KEY_WORD)
                    ch = MUX_KEY_WORD;
                else if (*src == 0x01)
                    ch=STX_OCTET; //0x55 escaping

                //leave MUX state and restore the state
                g_cTstFrameState = g_cOldTstFrameState;

            }
            else if (g_cTstFrameState == RS232_FRAME_STX)
            {   // The read-in char is not header, find the next one directly
                discard_word++; //check the discard chars
                src++;
                continue;
            }
        }

        switch ( g_cTstFrameState)
        {
            /*the state is RS232_FRAME_LENHI*/
        case RS232_FRAME_LENHI:
            META_LOG("[TST_Drv:] parse state: RS232_FRAME_LENHI: %x", ch);
            (*pFrameLength) = ch << 8;
            g_cTstFrameState = RS232_FRAME_LENLO;
            break;

            /*the state is RS232_FRAME_LENLO*/
        case RS232_FRAME_LENLO:
            (*pFrameLength) += ch;
            data_total = (*pFrameLength);
            META_LOG("[TST_Drv:] parse state: RS232_FRAME_LENLO: %x, total: %d", ch, *pFrameLength);
            if ((*pFrameLength +5) > FrameMaxSize)
            {
                g_cTstFrameState = RS232_FRAME_TYPE;
                META_LOG("[TST_Drv:] parse state: Error: Frame size is %d+5, exceeds limit of %d." , *pFrameLength, FrameMaxSize);
                return;
            }
            else
                g_cTstFrameState = RS232_FRAME_TYPE;

            break;

            /*the state is RS232_FRAME_TYPE*/
        case RS232_FRAME_TYPE:
            META_LOG("[TST_Drv:] parse state: RS232_FRAME_TYPE: %x ", ch);

            *frame_type = ch;
            if ( ( ch == RS232_INJECT_PRIMITIVE_OCTET ) || (ch == RS232_COMMAND_TYPE_OCTET) )	/* Inject primitive to module frame  */
            {
                /* the parsed frame is modem side */
                g_cTstFrameState = RS232_FRAME_MD_DATA ;
		        //send to modem1
		        if (gModemType == MODEMTWOONLY)
		        {
		            g_nSendModemIndex = 2;
		        }
                else
		        {
		            g_nSendModemIndex = 1;
                }

            }
	    else if( (ch == RS232_INJECT_PRIMITIVE_OCTETMODEM2) || (ch == RS232_COMMAND_TYPE_MD2_MEMORY_DUMP))
	        {
		        g_cTstFrameState = RS232_FRAME_MD_DATA ;
		        //send to modem2
		        g_nSendModemIndex = 2;
	        }
            else if (ch ==RS232_INJECT_APPRIMITIVE_OCTET)
            {
                /* the parsed frame is ap side */
                g_cTstFrameState = RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER;
                g_sRs232Frame.received_prig_header_length = 0;
                g_sRs232Frame.received_buf_para_length = 0;
                g_sRs232Frame.inject_prim.local_len = 0;
                g_sRs232Frame.inject_prim.peer_len = 0;
                g_sRs232Frame.header_ptr = (kal_uint8*)&g_sRs232Frame.inject_prim;

            }
            else
            {
                g_cTstFrameState = RS232_FRAME_STX; //error reset
            }

            break;

            /*the state is RS232_FRAME_MD_DATA*/
        case RS232_FRAME_MD_DATA:
            /* if the frame is modem side, we just write whole data to ccci port */
            TSTParseMDData((void *)buf_ptr, input_len, g_nSendModemIndex );
            META_LOG("[TST_Drv:] parse state:  nRS232_FRAME_MD_DATA--: %d, %d, total %d, %d",input_len, cbWriten, data_total, data_cnt);

            return;

            /*the state is RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER*/
        case RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER:
            /* fill data to tst_primitive_header_struct */
            g_sRs232Frame.received_prig_header_length ++;
            *g_sRs232Frame.header_ptr++=ch;

            if (g_sRs232Frame.received_prig_header_length
                    == sizeof(TST_PRIMITIVE_HEADER_STRUCT))
            {

                if ((g_sRs232Frame.inject_prim.local_len != 0)||(g_sRs232Frame.inject_prim.local_len != 0))
                {
                    g_cTstFrameState = RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA;
                    META_LOG("[TST_Drv:] RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER: LOCAL len: %d peer_len : %d ", g_sRs232Frame.inject_prim.local_len, g_sRs232Frame.inject_prim.peer_len);

                    g_sRs232Frame.buf_ptr= (kal_uint8 *)malloc(g_sRs232Frame.inject_prim.local_len + g_sRs232Frame.inject_prim.peer_len);

                    *local_ptr =g_sRs232Frame.buf_ptr;
                }

            }

            break;

            /*the state is RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA*/
        case RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA:
            /* fill the primitive body to local parameter buffer and peer buffer */
            if ((*local_ptr != NULL))
            {
                **local_ptr = ch;
                (*local_ptr) = (*local_ptr+1);
            }

            g_sRs232Frame.received_buf_para_length++;

            if ((g_sRs232Frame.inject_prim.local_len + g_sRs232Frame.inject_prim.peer_len)
                    == g_sRs232Frame.received_buf_para_length)
            {
                g_cTstFrameState = RS232_FRAME_CHECKSUM;
            }
			//META_LOG("[TST_Drv:] RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA: LOCAL len: %d peer_len : %d ", g_sRs232Frame.inject_prim.local_len, g_sRs232Frame.inject_prim.peer_len);

            break;


        case RS232_FRAME_CHECKSUM:
            /* recieve the check sum */
            META_LOG("[TST_Drv:] parse state: RS232_FRAME_CHECKSUM: checksum: %d, ch: %d ", *checksum, ch);
            if (*frame_type == RS232_INJECT_APPRIMITIVE_OCTET)
            {
                g_cTstFrameState = RS232_FRAME_STX;
            }

            g_cTstFrameState = RS232_FRAME_STX;

            sTempRxbuf.eFrameType = AP_FRAME;
            sTempRxbuf.LocalLen= g_sRs232Frame.inject_prim.local_len;
            sTempRxbuf.PeerLen= g_sRs232Frame.inject_prim.peer_len;

            memcpy(sTempRxbuf.uData, g_sRs232Frame.buf_ptr,g_sRs232Frame.received_buf_para_length);
            META_LOG("[TST_Drv:] AP side RX %d bytes",g_iMetaFrameRxBufIndex);
            /* send data FT task */
            if(*checksum == ch)
            {
				//dumpDataInHexString(g_cMetaFrameRxBuffer,g_iMetaFrameRxBufIndex,16);
                if(*(unsigned short*)(sTempRxbuf.uData + AP_FRAME_TOKEN_LENGTH) == AP_CHECK_SIM_REQ_ID)
            	{
            	                if (gModemType == MODEMTWOONLY)
            	                {
            	                       SIMmodemIndex = 2;
            	                }
            	               else
            	               	{
            	               	       META_LOG("[TST_Drv:] RS232_FRAME_CHECKSUM SIM check ch:%x",
					sTempRxbuf.uData[AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH + 
									   sizeof(short) + MD_FRAME_TREACE_OFFSITE]);
                                      
							    
            	               	       if(sTempRxbuf.uData[AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH + 
									   	 sizeof(short)+ MD_FRAME_TREACE_OFFSITE]== RS232_INJECT_PRIMITIVE_OCTETMODEM2 )
            	               	       {
            	               	           SIMmodemIndex = 2;
            	               	       }
				       else
				       {
            	               	           SIMmodemIndex = 1;
            	               	       }
            	               	}
		    		MD_SIM_CHECK_REQ(sTempRxbuf.uData,sTempRxbuf.LocalLen, SIMmodemIndex);
		    	}
                else
				{
		     		char* pLocalBuf = (char *)sTempRxbuf.uData;
    		     	char* pPeerBuf = (char *)&sTempRxbuf.uData[sTempRxbuf.LocalLen];
				    FT_DispatchMessage(pLocalBuf, pPeerBuf,sTempRxbuf.LocalLen, sTempRxbuf.PeerLen);
                    break;
                }
            }
            else
            {
				META_LOG("CheckSum error. Dumping META frame: ");
				// Dump data when checksum error
				dumpDataInHexString(g_cMetaFrameRxBuffer,g_iMetaFrameRxBufIndex,16);
            }
            free(g_sRs232Frame.buf_ptr);
            g_sRs232Frame.buf_ptr = NULL;
            g_sRs232Frame.received_buf_para_length = 0;
            *local_ptr = NULL;
            *checksum = STX_OCTET;

            break;

        default:
            /* exception of g_cTstFrameState */
            break;

        }

        *checksum ^= ch;  //add the check sum
        src++;

    }
}





/********************************************************************************
//FUNCTION:
//		TSTParseMDData
//DESCRIPTION:
//		this function is called to handle modem data's escape after being recieved from PC
//
//PARAMETERS:
//		pdata: 	[IN]		data buffer
//		len:		[IN]		data length
//
//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/
void TSTParseMDData(void *pdata, kal_int16 len, int modemNum )
{

    unsigned char *pTempBuf = NULL;
    unsigned char *pTempDstBuf = NULL;
    unsigned char *pMamptrBase = (unsigned char *)pdata;
    unsigned char *pDestptrBase = NULL;
    int iCheckNum = 0;
    int dest_index=0;
    int cbWriten = 0;
    int cbTxBuffer = len;
    int i=0;

    if(pMamptrBase == NULL)
    {
        META_LOG("Err: TSTParseMDData pMamptrBase is NULL");
        return;
    }
    if( modemNum <1 || modemNum >2 )
    {
        META_LOG("Err: TSTParseMDData modemNum is valid modemNum:%d",modemNum);
        return;
    }
    //Wayne replace 2048 by MAX_TST_RECEIVE_BUFFER_LENGTH
    pDestptrBase = (unsigned char *)malloc(MAX_TST_RECEIVE_BUFFER_LENGTH);//2048);
    if(pDestptrBase == NULL)
    {
        META_LOG("Err: TSTParseMDData pDestptrBase malloc Fail");
        return;
    }

    pTempDstBuf = pDestptrBase;
    pTempBuf = pMamptrBase;

    /* if the data is 0x77 and 0x01, escape to 0x11
        if the data is 0x77 and 0x02, escape to 0x13
        if the data is 0x77 and 0x77, eccapse to 0x77
    */
    while (iCheckNum != cbTxBuffer)
    {
        iCheckNum++;
		 if (comPortType==META_UART_COM)
	 	{
			if(iCheckNum == cbTxBuffer)
			{
           META_LOG("root cause1:cnt:%d",iCheckNum);
           if(*pTempBuf ==0x77)
           	{
           		gFlag[modemNum-1] = 1;
           		break;
           	}
        }
			
        if(gFlag[modemNum-1] == 1)
        {
        		gFlag[modemNum-1] = 0;
            if (*pTempBuf ==0x01)
           	{
	               		*pTempDstBuf = 0x11;
            }
            else if (*pTempBuf ==0x02 )
            {
              *pTempDstBuf = 0x13;
            }
            else if (*pTempBuf ==0x03 )
            {
              *pTempDstBuf = 0x77;
            }
            else
            {
            	META_LOG("root cause3: cnt:%d",iCheckNum);
              return;
            }
        }
        else
        {
        if (*pTempBuf ==0x77 )
        {
            pTempBuf++;
            iCheckNum++;		//do the escape, dest_index should add for write to uart or usb
            if (*pTempBuf ==0x01)
            {
	                			*pTempDstBuf = 0x11;
            }
            else if (*pTempBuf ==0x02 )
            {
                *pTempDstBuf = 0x13;
            }
            else if (*pTempBuf ==0x03 )
            {
                *pTempDstBuf = 0x77;
            }
            else
            {
            	 META_LOG("root cause2: cnt:%d",iCheckNum);
                 return;
            }
        }
        else
            *pTempDstBuf = *pTempBuf;
				}
		 }
		 else
		 {
		 	// No escaping in USB mode
	            	*pTempDstBuf = *pTempBuf;
		 }
        dest_index++;
        pTempDstBuf++;
        pTempBuf++;
    }
    META_LOG("TST_Drv:]Try to write %d bytes to CCCI port...", dest_index);
    dumpDataInHexString(pDestptrBase, dest_index, 16);
    cbWriten = -1;
    //after handle, send to ccci*************************
    if(modemNum == 1 && (gModemType == DUALMODEM ||gModemType == MODEMONEONLY))
    {
        META_LOG("[TST_Drv:] Write CCCI1 port----------->");
        cbWriten = write(g_hMciComPort, (void *)pDestptrBase, dest_index);
    }
    else if(modemNum == 2 && (gModemType == DUALMODEM ||gModemType == MODEMTWOONLY))
    {
        META_LOG("[TST_Drv:] Write CCCI2 port---------->");
	cbWriten = write(g_hMciComPort2, (void *)pDestptrBase, dest_index);
    }
	else
	{
		META_LOG("Frame Type is invalid gModemType = %d ,modemNum = %d",gModemType,modemNum);	
	}

    if (cbWriten < 0)
    {
	META_LOG("[TST_Drv:] Write CCCI Failed, return %d, errno=%d ", cbWriten, errno);
    }
    else
    {
        META_LOG("[TST_Drv:] Write %d bytes to CCCI modemNum = %d",cbWriten,modemNum);
    }
    META_LOG("[TST_Drv:] TSTParseMDData ReadFile end!!: %d ",cbWriten);
    META_LOG("[TST_Drv:] TSTMuxPrimitiveMDData: %d  %d %d   ",cbWriten, cbTxBuffer, dest_index);

    free(pDestptrBase);

}




/********************************************************************************
//FUNCTION:
//		TSTMuxPrimitiveMDData
//DESCRIPTION:
//		this function is called to add the escape key for modem side which is used to differential data catch or meta
//		before sending to PC
//
//PARAMETERS:
//		pdata: 	[IN]		the data buffer
//		len:		[IN]		the data length
//
//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/
void TSTMuxPrimitiveMDData(void *pdata, kal_int16 len)
{
    /* This primitive is logged by TST */
    unsigned char *pTempBuf = NULL;
    unsigned char *pTempDstBuf = NULL;
    unsigned char *pMamptrBase = (unsigned char *)pdata;
    unsigned char *pDestptrBase = NULL;
    int iCheckNum = 0;
    int dest_index=0;
    int cbWriten = 0;
    int cbTxBuffer = len;
    int i=0;

    if(pMamptrBase == NULL)
    {
        META_LOG("Err: TSTMuxPrimitiveData pMamptrBase is NULL");
        return;
    }
    //Wayne add MAX_TST_TX_BUFFER_LENGTH
    pDestptrBase = (unsigned char *)malloc(MAX_TST_TX_BUFFER_LENGTH);//2048);
    if(pDestptrBase == NULL)
    {
        META_LOG("Err: TSTMuxPrimitiveMDData pDestptrBase is NULL");
        return;
    }
    pTempDstBuf = pDestptrBase;
    pTempBuf = pMamptrBase;

    /*so we use 0x77 and 0x01 inidcate 0xa5, use 0x77 and 0x03 indicate 0x77, use 0x77 and 0x13 indicate 0x13
     the escape is just for campatiable with feature phone */
    while (iCheckNum != cbTxBuffer)
    {
        *pTempDstBuf = *pTempBuf;
        iCheckNum++;

	 if (comPortType==META_UART_COM)
	 {
        if (*pTempBuf ==0x11 )
        {
            *pTempDstBuf++ = 0x77;
            *pTempDstBuf++ = 0x01;
            dest_index++;	//do the escape, dest_index should add for write to uart or usb
        }
        else if (*pTempBuf ==0x13 )
        {
            *pTempDstBuf++ = 0x77;
            *pTempDstBuf++ = 0x02;
            dest_index++;	//do the escape, dest_index should add for write to uart or usb
        }
        else if (*pTempBuf ==0x77 )
        {
            *pTempDstBuf++ = 0x77;
            *pTempDstBuf++ = 0x03;
            dest_index++;	//do the escape, dest_index should add for write to uart or usb
        }
        else
            pTempDstBuf++;
	 }
        else
        {
        	// No escaping in USB mode
        	pTempDstBuf++;
        }

        dest_index++;

        // if the data length more than the size of a frame, send it to PC first
        #if 0
        if ((dest_index)==2048)
        {
            Write(g_hUsbComPort, (void *)pDestptrBase, dest_index);
            META_LOG(g_bLogEnable, (TEXT("[TST_Drv:] TSTMuxPrimitiveData: index-%d cbTxBuffer-%d "),dest_index, cbTxBuffer));
            pTempDstBuf = pDestptrBase;
            dest_index=0;
        }
				#endif
        pTempBuf++;

    }


    cbWriten = -1;
    META_LOG("[TST_Drv:] Try to write data back to COM port:");
    dumpDataInHexString(pDestptrBase, dest_index, 16);
    MutexLock(&META_ComPortMD_Mutex);
    cbWriten = write(g_hUsbComPort, (void *)pDestptrBase, dest_index);
    if(cbWriten == -1)
	{
	META_LOG("MciComRxThread:Write CCCI pipe failed! errno=%d", errno);	
    }
    else
	{
        META_LOG("[TST_Drv:] TSTMuxPrimitiveData: %d %d %d",cbWriten, cbTxBuffer, dest_index);
	}
    MutexUnLock(&META_ComPortMD_Mutex);
    META_LOG("[TST_Drv:] TSTMuxPrimitiveData: %d  %d %d  ",cbWriten, cbTxBuffer, dest_index);


    free(pDestptrBase);

}

/********************************************************************************
//FUNCTION:
//		TSTCheckMDData
//DESCRIPTION:
//		this function is called to parset the data from Modem side
//
//PARAMETERS:
//		buf_ptr: 			[IN]			the lid of nvram file
//		input_len:		[IN]			the buf size		
//
//RETURN VALUE:
//		Nones
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		g_cMDFrameState will change according the recieved state
********************************************************************************/
void TSTCheckMDData(kal_uint8 *buf_ptr,
					kal_uint16 input_len, int modemIndex)
{

	
    kal_uint16	u16Length=0;
	//static kal_uint16	u16ModemframeLength=0;
    kal_uint8	*src=buf_ptr;
    kal_uint8	ch=0;	
	META_RX_DATA sTempRxbuf ;
	
	
    if((buf_ptr == NULL)||(src == NULL))
    {
        META_LOG("Err: TSTCheckMDData buf_ptr is NULL");
        return;
    }
	
    if(modemIndex <1 ||modemIndex> 2)
    {
           META_LOG("Err: TSTCheckMDData modemIndex is invalid");
           return;
    }

     META_LOG("u16Length:%d, input_len:%d,modemIndex:%d",u16Length,input_len,modemIndex);
	
    while ( u16Length != input_len )
    {
		
        ch = *src;
        u16Length ++;
		u16ModemframeLength[modemIndex-1]++;
		if( u16ModemframeLength[modemIndex-1] >= 1)
		{
			frameHeader[modemIndex-1][(u16ModemframeLength[modemIndex-1])-1] = ch;
		}
		
		
		//META_LOG("[TST_Drv:] ch: %x", ch);
		//META_LOG("[TST_Drv:] g_cMDFrameState: %d", g_cMDFrameState[modemIndex-1]);
		
		// Siyang Miao added for data dumping
		g_cModemFrameRxBuffer[modemIndex-1][g_iModemFrameRxBufIndex[modemIndex-1]] = ch;
		g_iModemFrameRxBufIndex[modemIndex-1]++;
		
		if( (ch == STX_OCTET || ch == STX_L1HEADER ) && ( (g_cMDFrameState[modemIndex-1] != RS232_FRAME_MD_CONFIRM_DATA)
			&& (g_cMDFrameState[modemIndex-1] != RS232_FRAME_LENHI) && (g_cMDFrameState[modemIndex-1] != RS232_FRAME_LENLO))  )
		{
			
			if( ch == STX_L1HEADER )
			{
			    g_nModemDataType = 1;
			}
			else
			{
			    g_nModemDataType = 0;
			}
			if ( g_cMDFrameState[modemIndex-1] != RS232_FRAME_CHECKSUM)
			{			
			    //for data dumping
			     g_cModemFrameRxBuffer[modemIndex-1][0] = ch;
			     g_iModemFrameRxBufIndex[modemIndex-1] = 1;
			
			     
			     g_cMDFrameState[modemIndex-1] = RS232_FRAME_LENHI;
			     META_LOG("[TST_Drv:] Flag change to RS232_FRAME_LENHI");
			      src++;
			     Modemchecksum[modemIndex-1] = ch;
			      ch = *src;
			     ModemFrameLength[modemIndex-1] = 0;
			     continue;
			}		
		}
		else
        {
            if( g_nModemDataType == 0 )
            {
                if ((*src == MUX_KEY_WORD ) && (g_cMDFrameState[modemIndex-1] != RS232_FRAME_KEYWORD))
                { // enter MUX state(0x5A) and save the old
				
                    g_cOldMDFrameState[modemIndex-1] = g_cMDFrameState[modemIndex-1];
                    g_cMDFrameState[modemIndex-1] = RS232_FRAME_KEYWORD;
				
                    src++;
				
				    META_LOG("0x5A");
				
                    continue;
                }
                else if(g_cMDFrameState[modemIndex-1] == RS232_FRAME_KEYWORD)
                {
                    if (*src== MUX_KEY_WORD)
                        ch = MUX_KEY_WORD;
                    else if (*src == 0x01)
					    ch=STX_L1HEADER; //0xA5 escaping
				
				
                    //leave MUX state and restore the state
                    g_cMDFrameState[modemIndex-1] = g_cOldMDFrameState[modemIndex-1];
		            ModemFrameLength[modemIndex-1]++;				
		            META_LOG("0x01");
				
                }
            }
			
        }
		
		
		
		switch ( g_cMDFrameState[modemIndex-1])
        {
            /*the state is RS232_FRAME_LENHI*/
		case RS232_FRAME_LENHI:
			{
			    META_LOG("[TST_Drv:] parse state: RS232_FRAME_LENHI: %x,g_nModemDataType:%d", ch,g_nModemDataType);
			    if( g_nModemDataType == 1 )  //for L1 data
			    {			       
				ModemFrameLength[modemIndex-1] = ch;
			    }
			    else
			    {
			        ModemFrameLength[modemIndex-1] = ch << 8;
			    }
			    g_cMDFrameState[modemIndex-1] = RS232_FRAME_LENLO;
			    META_LOG("[TST_Drv:] Flag change to RS232_FRAME_LENLO");
			    break;
			}
			
                /*the state is RS232_FRAME_LENLO*/
		case RS232_FRAME_LENLO:
			{
				 if( g_nModemDataType == 1 )   //for L1 data
				 {
				     ModemFrameLength[modemIndex-1] += ch << 8;
				 }
				 else
				 {
			             ModemFrameLength[modemIndex-1] += ch;
				 }
			         META_LOG("[TST_Drv:] parse state: RS232_FRAME_LENLO: %x, g_nModemDataType:%d, total: %d", ch, g_nModemDataType, ModemFrameLength[modemIndex-1]);
			         if ((ModemFrameLength[modemIndex-1] +4) > FrameMaxSize)
			         {
				     g_cMDFrameState[modemIndex-1] = RS232_FRAME_STX;
				     META_LOG("[TST_Drv:] parse state: Error: Frame size is %d+4, exceeds limit of %d." , ModemFrameLength[modemIndex-1], FrameMaxSize);
				     return;
			         }
			         else
			         {
				     g_cMDFrameState[modemIndex-1] = RS232_FRAME_MD_CONFIRM_DATA;
				     META_LOG("[TST_Drv:] Flag change to RS232_FRAME_MD_CONFIRM_DATA");
			         }
			         break;
			}
			
		case RS232_FRAME_MD_CONFIRM_DATA:
			if(u16ModemframeLength[modemIndex-1] == (ModemFrameLength[modemIndex-1]+3))
			{
				g_cMDFrameState[modemIndex-1] = RS232_FRAME_CHECKSUM;
				META_LOG("[TST_Drv:] Flag change to RS232_FRAME_CHECKSUM");
			}
			
			break;
			
			
		case  RS232_FRAME_CHECKSUM:
			g_cMDFrameState[modemIndex-1] = RS232_FRAME_STX;
			
			sTempRxbuf.eFrameType = MD_FRAME;
			sTempRxbuf.LocalLen = (kal_int16)(ModemFrameLength[modemIndex-1]+4);
			memcpy(sTempRxbuf.uData, frameHeader[modemIndex-1], sTempRxbuf.LocalLen);
			META_LOG("[TST_Drv:] Modem side RX %d bytes",g_iMetaFrameRxBufIndex);
			META_LOG("RS232_FRAME_CHECKSUM Modem checksum:%x, ch:%x",Modemchecksum[modemIndex-1],ch);
			if( Modemchecksum[modemIndex-1] == ch)
			{
				if(g_AP_RECEIVE_MD_SIN_CHECK[modemIndex-1])
				{
					MD_SIM_CHECK_PARSE_FRAME(&sTempRxbuf, modemIndex);
				}
				else
					TSTMuxPrimitiveMDData(&(sTempRxbuf.uData), sTempRxbuf.LocalLen);
				//g_cMDFrameState = RS232_FRAME_STX;
				buf_ptr = src;
				u16ModemframeLength[modemIndex-1] = 0;
				ModemFrameLength[modemIndex-1] = 0;
				Modemchecksum[modemIndex-1] = 0;
				memset(frameHeader[modemIndex-1],'\0',FrameMaxSize);
			}
			else
			{

                               META_LOG("CheckSum error. Dumping META frame: ");
                               // Dump data when checksum error
                               dumpDataInHexString(g_cModemFrameRxBuffer[modemIndex-1], g_iModemFrameRxBufIndex[modemIndex-1], 16);
			       u16ModemframeLength[modemIndex-1] = 0;
			       ModemFrameLength[modemIndex-1] = 0;
			       Modemchecksum[modemIndex-1] = ch;
			       memset(frameHeader[modemIndex-1],'\0',FrameMaxSize);
                        }
			
			META_LOG("[TST_Drv:] u16Length: %d", u16Length);
			META_LOG("[TST_Drv:] u16ModemframeLength: %d", u16ModemframeLength[modemIndex-1]);
			break;
		case RS232_FRAME_STX:
			u16ModemframeLength[modemIndex-1] = 0;
			ModemFrameLength[modemIndex-1] = 0;
			Modemchecksum[modemIndex-1] = ch;
			memset(frameHeader[modemIndex-1],'\0',FrameMaxSize);
			break;
		default:
			/* exception of g_cTstFrameState */
			break;
			
        }
	Modemchecksum[modemIndex-1] ^= ch;
		src++;
    }
}


/********************************************************************************
//FUNCTION:
//		MciComRxThread
//DESCRIPTION:
//		this function is MCI thread. it is used to recieved from modem side.
//
//PARAMETERS:
//		None
//
//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/


void * MciComRxThread1( void* lpParameter )
{
    int dwMask;
    int dwErrors;
    int bNeedTimeOut = 0;
    int cbRxBuffer = MAX_TST_RECEIVE_BUFFER_LENGTH ;
    int cbRecvd = 0;
    int nModemIndex = 1;
	
    int i=0;



    META_LOG("[TST_Drv:] MciComRxThread1 create success! ");

    while (!g_bTerminateAll)
    {
		int bytes;
		bytes = read(g_hMciComPort,g_cMCIRxBuffer, cbRxBuffer);
		if(bytes == -1)
		{
			META_LOG("[TST_Drv:] MciComRxThread1: read CCCI1 port error!");
			goto Exit;
		}
		if(bytes)
		{
			// add the header of frame type, and then send to mainthead. mainthread will add the header of dll frame,
			// finally send to PC
			META_LOG("[TST_Drv:] MciComRxThread1: read data from CCCI1 port!");
			dumpDataInHexString(g_cMCIRxBuffer, bytes, 16);
			
			 TSTCheckMDData(g_cMCIRxBuffer, (kal_uint16)bytes, nModemIndex);

			
		}
			
    }
		
Exit:
	
	pthread_exit(NULL);
	return NULL;

}


void * MciComRxThread2( void* lpParameter )
{
	int dwMask;
    int dwErrors;
    int bNeedTimeOut = 0;
    int cbRxBuffer = MAX_TST_RECEIVE_BUFFER_LENGTH ;
    int cbRecvd = 0;
    int nModemIndex = 2;
	
    int i=0;
		
	
    META_LOG("[TST_Drv:] MciComRxThread2 create success! ");
	
    while (!g_bTerminateAll)
    {
		int bytes;
		bytes = read(g_hMciComPort2,g_cMCIRxBuffer2, cbRxBuffer);
		if(bytes == -1)
		{
			META_LOG("[TST_Drv:] MciComRxThread2: read CCCI2 port error!");
			goto Exit;
	        }
		if(bytes)
		{
			// add the header of frame type, and then send to mainthead. mainthread will add the header of dll frame,
			// finally send to PC
			META_LOG("[TST_Drv:] MciComRxThread2: read data from CCCI2 port!");
			dumpDataInHexString(g_cMCIRxBuffer2, bytes, 16);
			
			TSTCheckMDData(g_cMCIRxBuffer2, (kal_uint16)bytes, nModemIndex);

                }

    }

Exit:

	pthread_exit(NULL);
	return NULL;
}

/********************************************************************************
//FUNCTION:
//		UsbComRxThread
//DESCRIPTION:
//		this function is uart/usb thread. it is used to recieved from uart/usb side.
//
//PARAMETERS:
//		None
//
//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/
void * UsbComRxThread( void* lpParameter )
{

    //int dwMask;
    int dwErrors;
    int  bNeedTimeOut = 0;
    //BYTE* pBuf = NULL;
    int cbRxBuffer = MAX_TST_RECEIVE_BUFFER_LENGTH ; ///20;
    int cbRecvd = 0;
    int i=0;
    kal_uint8	checksum;
    kal_uint8	*command_data_ptr=NULL;
    kal_uint8	*local_ptr=NULL, *peer_ptr=NULL;
    kal_uint8	rs232_frame_type;
    kal_uint16	rframe_u16Length;

    META_LOG("[TST_Drv:] UsbComRxThread create success");
	int bytes;

	int bHasChosenPort = 0;

    while (!g_bTerminateAll)
    {
		if (g_hUsbComPort != -1)
		{
			if (pthread_mutex_lock (&META_USBPort_Mutex))
			{
				META_LOG( "UsbComRxThread META_USBPort_Mutex Lock ERROR!\n"); 
			}
			bytes = read(g_hUsbComPort,g_cUSBRxBuffer, cbRxBuffer);
			if (pthread_mutex_unlock (&META_USBPort_Mutex))
			{
				META_LOG( "UsbComRxThread META_USBPort_Mutex Unlock ERROR!\n"); 
			}
			
			if(bytes == -1)
			{
				META_LOG("[TST_Drv:] UsbComRxThread: read COM port error!");
				if(is_USB_State_PlusIn()&USBFlag)
				goto Exit;
				else 
				{
					sleep(1);
					continue;
				}
			}
			if(bytes)
			{
				if (!bHasChosenPort)
				{
					FTT_Init(g_hUsbComPort);
					bHasChosenPort = 1;
				}

				META_LOG("[TST_Drv:] COM port have data<%d>:",bytes);
				TSTParseReceiveData(g_cUSBRxBuffer, (kal_uint16)bytes, &rs232_frame_type, &checksum, &command_data_ptr, &local_ptr, &peer_ptr,&rframe_u16Length);
			}
		}
		else
		{
			META_LOG("com port type invalid");	
			goto Exit;	
		}
    }


Exit:

    META_LOG("UsbComRxThread: error exit");
	pthread_exit(NULL);

    return NULL;
}


/********************************************************************************
//FUNCTION:
//		MD_SIM_CHECK_REQ
//DESCRIPTION:
//		it is used to recieve the sim check req from ap.
//
//PARAMETERS:
//		pdata: 	[IN]		the data buffer
//		len:		[IN]		the data length

//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/

void MD_SIM_CHECK_REQ(unsigned char *pdata, kal_int16 len, int modemIndex)
{
       if (pdata == NULL)
       	{
       	       META_LOG("[TST_Drv:] MD_SIM_CHECK_REQ pdata is NULL");
       	       return;
       	}
       if (modemIndex < 1 || modemIndex > 2)
       	{
       	       META_LOG("[TST_Drv:] MD_SIM_CHECK_REQ modemIndex is invalid, modemIndex:%d",modemIndex);
       	       return;
       	}
	unsigned char* pframe;
	short frame_len;
	g_AP_RECEIVE_MD_SIN_CHECK[modemIndex-1] = 1;
	g_AP_SIM_CHECK_TOKEN[modemIndex-1] = *(unsigned short*)pdata;
	g_AP_SIM_CHECK_REQ_ID[modemIndex-1] = *(unsigned short*)(pdata + AP_FRAME_TOKEN_LENGTH);
	frame_len = *(unsigned short*)(pdata + AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH);
	g_MD_SIM_CHECK_TOKEN[modemIndex-1] = *(unsigned short*)(pdata
											  + AP_FRAME_TOKEN_LENGTH
											  + AP_FRAME_REQ_ID_LENGTH
											  + sizeof(short)
											  + MD_FRAME_HREADER_LENGTH
											  + MD_FRAME_TST_INJECT_PRIMITIVE_LENGTH
											  + MD_FRAME_REF_LENGTH
											  + MD_FRAME_MSG_LEN_LENGTH
											  );
	pframe = pdata + AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH + sizeof(short);
	META_LOG("[TST_Drv:] MD_SIM_CHECK_REQ Receive ap token: %d , ap req id %d,md token %d",g_AP_SIM_CHECK_TOKEN[modemIndex-1],
		   g_AP_SIM_CHECK_REQ_ID[modemIndex-1] ,g_MD_SIM_CHECK_TOKEN[modemIndex-1]);
	TSTParseMDData(pframe,frame_len,modemIndex);
}
/********************************************************************************
//FUNCTION:
//		MD_SIM_CHECK_CNF
//DESCRIPTION:
//		 it is used to recieved the sim check cnf from md side.
//
//PARAMETERS:
//		META_RX_DATA: [IN]  the frame address
//
//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/

void MD_SIM_CHECK_CNF(META_RX_DATA *pMuxBuf,int i, int modemIndex)
{

        if (pMuxBuf == NULL)
        {
               META_LOG("[TST_Drv:] MD_SIM_CHECK_CNF pMuxBuf is NULL");
               return;
        }
        if (modemIndex < 1 || modemIndex > 2)
        {
               META_LOG("[TST_Drv:] MD_SIM_CHECK_CNF modemIndex is invalid, modemIndex:%d",modemIndex);
               return;
        }
	META_RX_DATA DestFrame;
	unsigned char* ptmp;
	memset(&DestFrame,0,sizeof(META_RX_DATA));
	ptmp = DestFrame.uData;
	*(unsigned short*)ptmp = g_AP_SIM_CHECK_TOKEN[modemIndex-1];
	*(unsigned short*)(ptmp + AP_FRAME_TOKEN_LENGTH) = g_AP_SIM_CHECK_REQ_ID[modemIndex-1] + 1;
	*(unsigned short*)(ptmp + AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH) = pMuxBuf->LocalLen - i;
	memcpy(DestFrame.uData + AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH + sizeof(short),pMuxBuf->uData + i,pMuxBuf->LocalLen -i);
	*(ptmp + AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH + sizeof(short) + MD_FRAME_MAX_LENGTH) = 0; //status
	DestFrame.eFrameType = AP_FRAME;
	DestFrame.LocalLen = AP_FRAME_TOKEN_LENGTH + AP_FRAME_REQ_ID_LENGTH + sizeof(short) + MD_FRAME_MAX_LENGTH + sizeof(unsigned char);
	g_AP_RECEIVE_MD_SIN_CHECK[modemIndex-1] = 0;
	META_LOG("[TST_Drv:] MD_SIM_CHECK_CNF Receive ap token: %d , ap cnf id %d,md token %d",g_AP_SIM_CHECK_TOKEN[modemIndex-1], 
		          g_AP_SIM_CHECK_REQ_ID[modemIndex-1] + 1, g_MD_SIM_CHECK_TOKEN[modemIndex-1]);
	FTMuxPrimitiveData(&DestFrame);
}
/********************************************************************************
//FUNCTION:
//		MD_SIM_CHECK_PARSE_FRAME
//DESCRIPTION:
//		 it is used to parse frame  from md side.
//
//PARAMETERS:
//		META_RX_DATA: [IN]  the frame address
//
//RETURN VALUE:
//		None
//
//DEPENDENCY:
//		None
//
//GLOBALS AFFECTED
//		None
********************************************************************************/
void MD_SIM_CHECK_PARSE_FRAME(META_RX_DATA *pMuxBuf, int modemIndex)
{
        if (pMuxBuf == NULL)
        {
               META_LOG("[TST_Drv:] MD_SIM_CHECK_PARSE_FRAME pMuxBuf is NULL"); 
               return;
        }
        if (modemIndex < 1 || modemIndex > 2)
        {
               META_LOG("[TST_Drv:] MD_SIM_CHECK_PARSE_FRAME modemIndex is invalid, modemIndex:%d",modemIndex); 
               return;
        }
	int i;
	META_LOG("[TST_Drv:] MD_SIM_CHECK_PARSE_FRAME!");
	for(i = 0;i < pMuxBuf->LocalLen - MD_FRAME_FAILED_TST_LOG_PRIMITIVE_LENGTH;i++)
	{
		if(pMuxBuf->uData[i] == 0x55 && 
			(( modemIndex == 1 && pMuxBuf->uData[i + MD_FRAME_TREACE_OFFSITE] == 0x60) || 
			( modemIndex == 2 && ((pMuxBuf->uData[i + MD_FRAME_TREACE_OFFSITE] == 0xB0) || (pMuxBuf->uData[i + MD_FRAME_TREACE_OFFSITE] == 0x60))) ))
		{
			if(*(unsigned short*)(pMuxBuf->uData + i + MD_FRAME_HREADER_LENGTH
											 + MD_FRAME_FAILED_TST_LOG_PRIMITIVE_LENGTH
											 + MD_FRAME_REF_LENGTH
											 + MD_FRAME_MSG_LEN_LENGTH) == g_MD_SIM_CHECK_TOKEN[modemIndex-1])
			{
					META_LOG("[TST_Drv:] MD_SIM_CHECK_CNF offsite is %d!",i);
				  MD_SIM_CHECK_CNF(pMuxBuf,i,modemIndex);
			}
		}
		else
		{
		    TSTMuxPrimitiveMDData(pMuxBuf->uData, pMuxBuf->LocalLen);
			return;
		}
	}
}

