/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */


#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>

#include "common.h"
#include "miniui.h"
#include "ftm.h"
#include "utils.h"
#include <pthread.h>
#include <termios.h> 
#include <DfoDefines.h>



#define TAG "[AT Command]"


#define BUF_SIZE 128
#define HALT_INTERVAL 20000

pthread_mutex_t ccci_mutex = PTHREAD_MUTEX_INITIALIZER;

#define CCCI_IOC_MAGIC 'C'
#define CCCI_IOC_ENTER_DEEP_FLIGHT _IO(CCCI_IOC_MAGIC, 14) //CCI will not kill muxd/rild
#define CCCI_IOC_LEAVE_DEEP_FLIGHT _IO(CCCI_IOC_MAGIC, 15) //CCI will kill muxd/rild

typedef enum {
	FALSE = 0,
	TRUE,
} _BOOL;

static speed_t baud_bits[] = {
	0, B9600, B19200, B38400, B57600, B115200, B230400, B460800, B921600
};

void initTermIO(int portFd, int cmux_port_speed)
{
	struct termios uart_cfg_opt;
	tcgetattr(portFd, &uart_cfg_opt);
	tcflush(portFd, TCIOFLUSH);
	
	/*set standard buadrate setting*/
	speed_t speed = baud_bits[cmux_port_speed];
	cfsetospeed(&uart_cfg_opt, speed);
	cfsetispeed(&uart_cfg_opt, speed);
	
	uart_cfg_opt.c_cflag &= ~(CSIZE | CSTOPB | PARENB | PARODD);   
	uart_cfg_opt.c_cflag |= CREAD | CLOCAL | CS8 ;
	
	/* Raw data */
	uart_cfg_opt.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
	uart_cfg_opt.c_iflag &= ~(INLCR | IGNCR | ICRNL | IXON | IXOFF);
	uart_cfg_opt.c_oflag &=~(INLCR|IGNCR|ICRNL);
	uart_cfg_opt.c_oflag &=~(ONLCR|OCRNL);
	
	/* Non flow control */
	//uart_cfg_opt.c_cflag &= ~CRTSCTS; 			   /*clear flags for hardware flow control*/
	uart_cfg_opt.c_cflag |= CRTSCTS;
	uart_cfg_opt.c_iflag &= ~(IXON | IXOFF | IXANY); /*clear flags for software flow control*/		  
	
	// Set time out
	uart_cfg_opt.c_cc[VMIN] = 1;
	uart_cfg_opt.c_cc[VTIME] = 0;
	
	/* Apply new settings */
	if(tcsetattr(portFd, TCSANOW, &uart_cfg_opt)<0)
	{
		LOGD(TAG "set terminal parameter fail");
	}
	
	int status = TIOCM_DTR | TIOCM_RTS;
	ioctl(portFd, TIOCMBIS, &status);

}

bool is_support_modem(int modem)
{

    if(modem == 1)
    {
        if(MTK_ENABLE_MD1)
        {
            return true;
        }
    }else if(modem == 2)
    {
        if(MTK_ENABLE_MD2)
        {
            return true;
        }
    }else if(modem == 3){
        if(MTK_DT_SUPPORT)
        {
            return true;
        }
    }
    return false;
}


int send_at (const int fd, const char *pCMD)
{
    int ret = 0, sent_len = 0;
    LOGD(TAG "Send AT CMD: %s\n", pCMD);
		
    while (sent_len != strlen(pCMD))
    {
    	//LOGD("send_at ccci_mutex try lock\n");
	   	if (pthread_mutex_lock (&ccci_mutex))
		{
			LOGE( "send_at pthread_mutex_lock ERROR!\n"); 
		}
		//LOGD("send_at ccci_mutex lock done\n");

		ret = write(fd, pCMD, strlen(pCMD));


		//LOGD("send_at ccci_mutex try unlock\n");
		if (pthread_mutex_unlock (&ccci_mutex))
		{
			LOGE( "send_at pthread_mutex_unlock ERROR!\n"); 
		}
		//LOGD("send_at ccci_mutex unlock done\n");
		
        if (ret<0)
        {
        	LOGE("ccci write fail! Error code = 0x%x\n", errno); 
            return ret;
        }
        else
        {	
            sent_len += ret;
            LOGD(TAG "[send_at] lenth = %d\n", sent_len);
        }
    }
    return 0;
		
}

int read_ack (const int fd, char *rbuff, int length)
{
	unsigned int has_read = 0;
	ssize_t      ret_val;

	if(-1 == fd)
		return -1;
	
	LOGD("Enter read_ack(): uart = %d\n", fd);
	memset (rbuff, 0, length);
	
#if 1
	while(has_read<length)
	{

loop:
		usleep(HALT_INTERVAL);
		LOGD("read_ack ccci_mutex try lock\n");
		if (pthread_mutex_lock (&ccci_mutex))
		{
			LOGE( "read_ack pthread_mutex_lock ERROR!\n"); 
		}
		LOGD("read_ack ccci_mutex lock done\n");
		
		ret_val = read(fd, &rbuff[has_read], length);
		LOGD("read_ack ccci_mutex try unlock\n");
		if (pthread_mutex_unlock (&ccci_mutex))
		{
			LOGE( "read_ack pthread_mutex_unlock ERROR!\n"); 
		}
		LOGD("read_ack ccci_mutex unlock done\n");
		LOGD("ret_val %d",ret_val);
		if(-1 == ret_val)
		{
			if (errno == EAGAIN)
			{
                LOGD("ccci can't read a byte!\n"); 
			}
			else
				LOGE("ccci read fail! Error code = 0x%x\n", errno); 
			
			//continue;  
			goto loop;
		}
		
		//if( (rbuff[has_read]!='\r')&&(rbuff[has_read]!='\n') )
		if(ret_val>2)
		{
		has_read += (unsigned int)ret_val;
		if (strstr(rbuff, "\r\n"))  break;
	}
		

	}
	LOGD("read_ack %s",rbuff);
	return has_read;
#endif

}


int wait4_ack (const int fd, char *pACK, int timeout)
{
    char buf[BUF_SIZE] = {0};
	char *  p = NULL;
    int rdCount = 0, LOOP_MAX;
    int ret = -1;

    LOOP_MAX = timeout*1000/HALT_INTERVAL;

    LOGD(TAG "Wait for AT ACK...: %s; Special Pattern: %s\n", buf, (pACK==NULL)?"NULL":pACK);

    for(rdCount = 0; rdCount < LOOP_MAX; ++rdCount) 
    {
		memset(buf,'\0',BUF_SIZE);    
		if (pthread_mutex_lock (&ccci_mutex))
		{
			LOGE( "read_ack pthread_mutex_lock ERROR!\n"); 
		}
		ret = read(fd, buf, BUF_SIZE);
		if (pthread_mutex_unlock (&ccci_mutex))
		{
			LOGE( "read_ack pthread_mutex_unlock ERROR!\n"); 
		}
		
        LOGD(TAG "AT CMD ACK: %s\n", buf);
        p = NULL;


		if (pACK != NULL)  
        {
	          p = strstr(buf, pACK);
              if(p) {
              	ret = 0; break; 
              }
			  p = strstr(buf, "ERROR");
        	  if(p) {
            	ret = -1; break;
        	  }
			  p = strstr(buf, "NO CARRIER");
        	  if(p) {
            	ret = -1; break;
        	  }
	
        }
		else
		{
			p = strstr(buf, "OK");
        	if(p) {
            	ret = 0; break;
        	}
        	p = strstr(buf, "ERROR");
        	if(p) {
            	ret = -1; break;
        	}
			p = strstr(buf, "NO CARRIER");
        	if(p) {
            	ret = -1; break;
        	}
		}
	
        usleep(HALT_INTERVAL);

    }
	LOGD("ret = %d",ret);
    return ret;
}

#ifdef MTK_ENABLE_MD1
int openDevice(void)
{
    int fd;
  
    fd = open("/dev/ttyC0", O_RDWR | O_NONBLOCK);
    if(fd < 0) {
        LOGD(TAG "Fail to open ttyC0: %s\n", strerror(errno));
        return -1;
    }
    // +EIND will always feedback +EIND when open device,
    // so move this to openDevice.	
    // +EIND
    //wait4_ack (fd, "+EIND", 3000);
    return fd;
}
#endif

int openDeviceWithDeviceName(char *deviceName)
{
    LOGD(TAG "%s - %s\n", __FUNCTION__, deviceName);
    int fd;
    fd = open(deviceName, O_RDWR | O_NONBLOCK);
    if(fd < 0) {
        LOGD(TAG "Fail to open %s: %s\n", deviceName, strerror(errno));
        return -1;
    }

    return fd;
}

int ExitFlightMode_DualTalk(int fd,_BOOL bON)
{

	if (bON)
	{
		
		LOGD(TAG "[AT]Disable Sleep Mode:\n");
		if (send_at (fd, "AT+ESLP=0\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;

		LOGD(TAG "[AT]Reset SIM1:\n");
		if (send_at (fd, "AT+ESIMS\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;
		if (send_at (fd, "AT+CFUN=1\r\n")) goto err;
		wait4_ack (fd, NULL, 5000);
		if (send_at (fd, "AT+CREG=1\r\n")) goto err;
		wait4_ack (fd, "+CREG", 15000);

	}else
	{
		
		send_at (fd, "ATH\r\n");
		wait4_ack (fd, NULL, 3000);

		LOGD(TAG "[AT]Enable Sleep Mode:\n");
		if (send_at (fd, "AT+ESLP=1\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;
		if (send_at (fd, "AT+CFUN=4\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;
	}
	return 0;
	err:
	return -1;

}


int ExitFlightMode (int fd, _BOOL bON)
{
	static bInit = FALSE;

	LOGD(TAG "[AT]Detect MD active status:\n");
	do
	{
		send_at (fd, "AT\r\n");
	} while (wait4_ack (fd, NULL, 300));


	if (bON)
	{		
		LOGD(TAG "[AT]Disable Sleep Mode:\n");
		if (send_at (fd, "AT+ESLP=0\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;

#ifdef GEMINI
		if (bInit == FALSE)
		{
			LOGD(TAG "[AT]Reset SIM1:\n");
			if (send_at (fd, "AT+ESIMS\r\n")) goto err;
			if (wait4_ack (fd, NULL, 3000)) goto err;

			LOGD(TAG "[AT]Switch to UART2:\n");
			if (send_at (fd, "AT+ESUO=5\r\n")) goto err;
			if (wait4_ack (fd, NULL, 3000)) goto err;

			LOGD(TAG "[AT]Reset SIM2:\n");
			if (send_at (fd, "AT+ESIMS\r\n")) goto err;
			if (wait4_ack (fd, NULL, 2000)) goto err;
			
	
			LOGD(TAG "[AT]Switch to UART1:\n");
			if (send_at (fd, "AT+ESUO=4\r\n")) goto err;
			if (wait4_ack (fd, NULL, 3000)) goto err;
			bInit = TRUE;
}

		LOGD(TAG "[AT]Turn ON RF:\n");
		#if defined(MTK_GEMINI_3SIM_SUPPORT)

		if (send_at (fd, "AT+EFUN=7\r\n")) goto err;

		#elif defined(MTK_GEMINI_4SIM_SUPPORT)

		if (send_at (fd, "AT+EFUN=f\r\n")) goto err;

		#else
		if (send_at (fd, "AT+EFUN=3\r\n")) goto err;
		#endif
		if (send_at (fd, "AT+CREG=1\r\n")) goto err;

		
#else
		if (bInit == FALSE)
		{
			LOGD(TAG "[AT]Reset SIM1:\n");
			if (send_at (fd, "AT+ESIMS\r\n")) goto err;
			if (wait4_ack (fd, NULL, 3000)) goto err;
			bInit = TRUE;
		}
		
		LOGD(TAG "[AT]Turn ON RF:\n");
		if (send_at (fd, "AT+CFUN=1\r\n")) goto err;
		if (send_at (fd, "AT+CREG=1\r\n")) goto err;

#endif

		wait4_ack (fd, "+CREG", 15000);

	}else
	{
		LOGD(TAG "[AT]Enable Sleep Mode:\n");
		if (send_at (fd, "AT+ESLP=1\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;

#ifdef GEMINI
		LOGD(TAG "[AT]Switch to UART1:\n");
		if (send_at (fd, "AT+ESUO=4\r\n")) goto err;
		if (wait4_ack (fd, NULL, 3000)) goto err;
	
		LOGD(TAG "[AT]Turn OFF RF:\n");
		if (send_at (fd, "AT+EFUN=0\r\n")) goto err;
#else
		LOGD(TAG "[AT]Turn OFF RF:\n");
		if (send_at (fd, "AT+CFUN=4\r\n")) goto err;
#endif
		if (wait4_ack (fd, NULL, 3000)) goto err;
	}
	return 0;
err:
	return -1;
}


/* turn on/off flight mode function, this function is for flight mode power-off modem feature */
int ExitFlightMode_PowerOffModem(int fd, int ioctl_fd, _BOOL bON){
    LOGD(TAG "[AT]ExitFlightMode_PowerOffModem\n");

    if(bON) {		
        LOGD(TAG "[AT]CCCI_IOC_LEAVE_DEEP_FLIGHT \n");		
        int ret_ioctl_val = ioctl(ioctl_fd, CCCI_IOC_LEAVE_DEEP_FLIGHT);
        LOGD("[AT]CCCI ioctl result: ret_val=%d, request=%d", ret_ioctl_val, CCCI_IOC_LEAVE_DEEP_FLIGHT);
		
    } else {
        do
        {
            send_at (fd, "AT\r\n");
        } while (wait4_ack (fd, NULL, 300));

        LOGD(TAG "[AT]Enable Sleep Mode:\n");		
        if (send_at (fd, "AT+ESLP=1\r\n")) goto err;
        if (wait4_ack (fd, NULL, 3000)) goto err;

        LOGD(TAG "[AT]Power OFF Modem:\n");
        if (send_at (fd, "AT+EFUN=0\r\n")) goto err;
        wait4_ack (fd, NULL, 15000); 
        if (send_at (fd, "AT+EPOF\r\n")) goto err;
        wait4_ack (fd, NULL, 10000);
		
        LOGD(TAG "[AT]CCCI_IOC_ENTER_DEEP_FLIGHT \n");		

        int ret_ioctl_val = ioctl(ioctl_fd, CCCI_IOC_ENTER_DEEP_FLIGHT);
        LOGD("[AT]CCCI ioctl result: ret_val=%d, request=%d", ret_ioctl_val, CCCI_IOC_ENTER_DEEP_FLIGHT);
    }

err:
    return -1;
}

void closeDevice(int fd)
{
    close(fd);
}



/********************
       ATD112;
********************/
const char* dial112(const int fd)
{
    LOGD(TAG "%s start\n", __FUNCTION__);
	static _BOOL bCMD_Sent = FALSE;



    if(fd < 0)
	{
    	LOGD(TAG "Invalid fd\n");
    	return STR_ERROR;
    }
     

	LOGD(TAG "[AT]Dail Up 112:\n");
	send_at (fd, "ATH\r\n");
	wait4_ack (fd, NULL, 3000);

	LOGD(TAG ">>>>>>>>> ATH done:\n");


	if (send_at (fd, "ATD112;\r\n"))
	{
		
			LOGD(TAG ">>>>>>>>> send ATD112 error\n");
		
			goto err;
		
	}
	LOGD(TAG ">>>>>>>>> send ATD112 done:\n");


	if (wait4_ack (fd, NULL, 15000)) 
		{
		
			LOGD(TAG ">>>>>>>>> wait for OK error\n");
		
			goto err;
		
	}
	LOGD(TAG ">>>>>>>>> wait for OK done:\n");
	
	
	if (wait4_ack (fd, "+ESPEECH", 15000)) 
		{
		
			LOGD(TAG ">>>>>>>>> wait for +ESPEECH error\n");
		
			goto err;
		
		}

	send_at (fd, "ATH\r\n");
	
	if(wait4_ack (fd, NULL, 3000))
	{
		
		LOGD(TAG ">>>>>>>>> wait for hang up error\n");

		goto err;
		

	}

	LOGD(TAG "%s end\n", __FUNCTION__);
	return STR_OK;
err:
	return STR_ERROR;
}

#if 0


const char * at_command_set(char *pCommand, const int fd)
{
	LOGD(TAG "%s start\n", __FUNCTION__);
	if(fd < 0) {
	  LOGD(TAG "Invalid fd\n");
	  return "ERROR";
	}
	//const int sz = 64;
	const int sz = 100;
	char buf[sz];
	memset(buf, 0, sz);
	const int HALT = 100000;
	
	sprintf(buf, "%s",pCommand);
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[AT]Set command: %s\n", buf);
	
	char *p = NULL;
	p = strstr(buf, "OK");
	if(p) {
	  LOGD(TAG "%s end with OK\n", __FUNCTION__);
	  return STR_OK;
	} else {
	  LOGD(TAG "%s end with ERROR\n", __FUNCTION__);
	  return STR_ERROR;
	}
}


const char * at_command_get(char *pCommand, char *pRet, const int len, const int fd)
{
	LOGD(TAG "%s start\n", __FUNCTION__);
	
	if(fd < 0) {
		LOGD(TAG "Invalid fd\n");
		return STR_ERROR;
	}
	
	// const int sz = 64;
	const int sz = 100;
	char buf[sz];
	memset(buf, 0, sz);
	const int HALT = 100000;
	
	sprintf(buf, pCommand);
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	LOGD(TAG "[AT]SN: %s\n", buf);
	
	const char *tok = "+EGMR: ";
	char *p = NULL;
	p = strstr(buf, tok);
	if(p) {
		if(len >= strlen(p)) {
		  strcpy(pRet, p);
		  LOGD(TAG "%s end with OK\n", __FUNCTION__);
		  return STR_OK;
		} else {
		  LOGD(TAG "Buffer is not enough\n");
		}
	} else {
		LOGD(TAG "Fail to get SN\n");
	}
	
	LOGD(TAG "%s end with ERROR\n", __FUNCTION__);
	return STR_ERROR;	
}

/********************
       AT+ESLP=0
********************/
const char* setSleepMode(const int fd)
{
	LOGD(TAG "%s start\n", __FUNCTION__);
	
	if(fd < 0) {
	  LOGD(TAG "Invalid fd\n");
	  return STR_ERROR;
	}
	
	const int sz = 64;
	char buf[sz];
	memset(buf, 0, sz);
	const int HALT = 100000;
	
	sprintf(buf, "AT+ESLP=0\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[AT]Set sleep mode: %s\n", buf);
	
	char *p = NULL;
	p = strstr(buf, "OK");
	if(p) {
	  LOGD(TAG "%s end with OK\n", __FUNCTION__);
	  return STR_OK;
	} else {
	  LOGD(TAG "%s end with ERROR\n", __FUNCTION__);
	  return STR_ERROR;
	}
}


/********************
       AT+EGMR=0,5
       param sn:   the buffer to save the serial number
       param len:  the length of buffer
********************/
const char* getSN(char *sn, const unsigned int len, const int fd)
{
	LOGD(TAG "%s start\n", __FUNCTION__);
	
	if(fd < 0) {
		LOGD(TAG "Invalid fd\n");
		return STR_ERROR;
	}
	
	// const int sz = 64;
	const int sz = 100;
	char buf[sz];
	memset(buf, 0, sz);
	const int HALT = 100000;
	
	sprintf(buf, "AT+EGMR=0,5\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	LOGD(TAG "[AT]SN: %s\n", buf);
	
	const char *tok = "+EGMR: ";
	char *p = NULL;
	p = strstr(buf, tok);
	if(p) {
		if(len >= strlen(p)) {
		  strcpy(sn, p);
		  LOGD(TAG "%s end with OK\n", __FUNCTION__);
		  return STR_OK;
		} else {
		  LOGD(TAG "Buffer is not enough\n");
		}
	} else {
		LOGD(TAG "Fail to get SN\n");
	}
	
	LOGD(TAG "%s end with ERROR\n", __FUNCTION__);
	return STR_ERROR;
}

/********************
       AT+EGMR=1,5
       param sn: new serial number
********************/
const char* setSN(const char* sn, const int fd)
{
	LOGD(TAG "%s start\n", __FUNCTION__);
	
	if(fd < 0) {
	  LOGD(TAG "Invalid fd\n");
	  return STR_ERROR;
	}
	
	// const int sz = 64;
	const int sz = 100;
	char buf[sz];
	memset(buf, 0, sz);
	const int HALT = 100000;
	
	sprintf(buf, "AT+EGMR=1,5,\"%s\"\r\n", sn);
	LOGD(TAG "[AT]%s\n", buf);
	write(fd, buf, strlen(buf));
	usleep(100000);
	read(fd, buf, sz);
	LOGD(TAG "[AT]%s\n", buf);
	
	char *p = NULL;
	p = strstr(buf, "OK");
	if(p) {
	  LOGD(TAG "%s end with OK\n", __FUNCTION__);
	  return STR_OK;
	} else {
	  LOGD(TAG "%s end with Error\n", __FUNCTION__);
	  return STR_ERROR;
	}
}

/********************
  to dial a specific number
********************/
const char* dial(const int fd, const char *number)
{
    LOGD(TAG "%s start\n", __FUNCTION__);

    if(fd < 0) {
    LOGD(TAG "Invalid fd\n");
    return STR_ERROR;
    }

    const int sz = 64 * 2;
    char buf[sz];
    memset(buf, 0, sz);
    const int HALT = 200000;
    char *p = NULL;
    const int rdTimes = 3;
    int rdCount = 0;

                
#if 0
    // +EIND will always feedback +EIND when open device,
    // so move this to openDevice.	
    // +EIND
    read(fd, buf, sz);
    usleep(HALT);
    LOGD(TAG "[at]%s\n", buf);
#endif
	
	sprintf(buf, "AT+ESIMS\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[at]Reset SIM1: %s\n", buf);
#ifdef GEMINI
	sprintf(buf, "AT+ESUO=5\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[at]Switch to UART2: %s\n", buf);
	
	sprintf(buf, "AT+ESIMS\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[at]Reset SIM2: %s\n", buf);
	
	sprintf(buf, "AT+ESUO=4\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[at]Switch to UART1: %s\n", buf);
	
	sprintf(buf, "AT+EFUN=3\r\n");
#else
	sprintf(buf, "AT+CFUN=1\r\n");
#endif
	write(fd, buf, strlen(buf));
	usleep(HALT);
	// read(fd, buf, sz);
	// usleep(HALT);
	// LOGD(TAG "[at]Turn on radio: %s\n", buf);
	for(rdCount = 0; rdCount < rdTimes; ++rdCount) 
	{
		read(fd, buf, sz);
		p = NULL;
		p = strstr(buf, "OK");
		if(p) {
		  break;
		}
		usleep(HALT);
	}
	if(!p) {
		LOGD(TAG "%s error in +EFUN\n",__FUNCTION__);
		return STR_ERROR;
	}
	
  sprintf(buf, "AT+CREG=2\r\n");
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[at]AT+CREG=2: %s\n", buf);
	
	int retry = 5;
	while(retry > 0) {
	    read(fd, buf, sz);
	    if(strstr(buf, "+CREG:")) {
	        LOGD(TAG "%s SIM 1 creg status: %s\n", __FUNCTION__, buf);
	        break;
	    } else {
	        LOGD(TAG "%s SIM 1 has not get creg status yet\n", __FUNCTION__);
	        retry -= 1;
	        usleep(1000000);
	    }
	}
	if(retry <= 0) {
	    LOGD(TAG "%s Fail to get network registration status\n", __FUNCTION__);
	    // return STR_ERROR;
	}
	
	sprintf(buf, "ATD%s;\r\n", number);
	write(fd, buf, strlen(buf));
	usleep(HALT);
	read(fd, buf, sz);
	usleep(HALT);
	LOGD(TAG "[at]ATD: %s\n", buf);
	for(rdCount = 0; rdCount < rdTimes; ++rdCount) 
	{
		p = NULL;
		p = strstr(buf, "OK");
		if(p) {
		  break;
		}
		usleep(HALT);
	}
	if(!p) {
		LOGD(TAG "%s error in ATD\n", __FUNCTION__);
		return STR_ERROR;
	}
	
	LOGD(TAG "%s end\n", __FUNCTION__);
	return STR_OK;
}
#endif
