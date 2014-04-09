/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>
#include <pthread.h>
#include <semaphore.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <sys/queue.h>

#include <getopt.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <string.h>
#include <signal.h>
#include <ctype.h>


#include "com_android_nfc.h"


#define TRACE_THIS_MODULE 1

#if( TRACE_THIS_MODULE == 1)

  #define FUNC_START() LogInformation( "%s - START.\n", __FUNCTION__)
  #define FUNC_END() LogInformation( "%s END.\n", __FUNCTION__)
	
#else

  #define FUNC_START()
  #define FUNC_END()
  
#endif

/***************************************************************************** 
 * macro for /dev/msr3110
 *****************************************************************************/ 
#define MSR3110_DEV_NAME    "/dev/msr3110"     
#define MSR3110_DEV_MAGIC_ID 0xCD
#define MSR3110_IOCTL_FW_UPGRADE _IOW( MSR3110_DEV_MAGIC_ID, 0x00, int)
#define MSR3110_IOCTL_SET_VEN _IOW( MSR3110_DEV_MAGIC_ID, 0x01, int)
#define MSR3110_IOCTL_SET_RST _IOW( MSR3110_DEV_MAGIC_ID, 0x02, int)
#define MSR3110_IOCTL_IRQ _IOW( MSR3110_DEV_MAGIC_ID, 0x03, int)
#define MSR3110_IOCTL_IRQ_ABORT _IOW( MSR3110_DEV_MAGIC_ID, 0x04, int)

#define MSR3110_WRITE_RETRY_MAX 20
#define MSR3110_READ_RETRY_MAX 10
#define MSR3110_READ_BUF_SIZE_MAX 256


extern void nfc_msr3110_print_buf(char *message ,unsigned char *print_buf, int length);
extern int nfc_msr3110_open();
extern int nfc_msr3110_close();
extern int nfc_msr3110_interface_send( unsigned char *SendBuf, int SendLen);
extern int nfc_msr3110_interface_recv( unsigned char *RecvBuf, int RecvBufLen);
extern int nfc_msr3110_interface_send_recv( unsigned char *SendBuf, int SendLen, unsigned char *RecvBuf, int RecvBufLen);



void nfc_msr3110_print_buf(char *message ,unsigned char *print_buf, int length)
{
    char temp[300] = {0};
    char ascii_chars[] = "0123456789ABCDEF";
    int i, j;
    for(i=0, j=0; i < length; i++) {
        temp[j++] = ascii_chars[ ( print_buf[i] >> 4 ) & 0x0F ];
        temp[j++] = ascii_chars[ print_buf[i] & 0x0F ];
        temp[j++] = ' ';
    }
    temp[j] = '\0';
    LogInformation("%s : %s", message, temp);
}



int nfc_msr3110_open()
{
	FUNC_START();
	int retVal = 0;
	
	int fd = -2;
	int pinVal = 0;

	fd = open( MSR3110_DEV_NAME, O_RDWR);
	if( fd < 0)
	{
		LogError( "%s FAIL: open msr3110, fd: %d. \n", __FUNCTION__, fd);
		retVal = -1;		
		goto end;
	}

	pinVal = 1;
    ioctl( fd,  MSR3110_IOCTL_SET_VEN, ( int)&pinVal);      
    usleep( 300000);
    //----------------------------------------------------//


end:
	FUNC_END();
	return retVal;
	
	
}

int nfc_msr3110_close()
{
	FUNC_START();
	int retVal = 0;
	
	int fd = -2;
	int pinVal = 0;

	fd = open( MSR3110_DEV_NAME, O_RDWR);
	if( fd < 0)
	{
		LogError( "%s FAIL: open msr3110, fd: %d. \n", __FUNCTION__, fd);
		retVal = -1;		
		goto end;
	}

	pinVal = 0;
    ioctl( fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
	usleep( 200000);

end:
	FUNC_END();
	return retVal;
	
	
}


int nfc_msr3110_interface_send( unsigned char *SendBuf, int SendLen)
{
	FUNC_START();
	
	int fd = -2;
	int retVal = 0;

	int loopIdx = 0;
		
	fd = open( MSR3110_DEV_NAME, O_RDWR);
	if( fd < 0)
	{
		LogError( "%s FAIL: open msr3110, fd: %d. \n", __FUNCTION__, fd);
		retVal = -1;		
		goto end;
	}

	nfc_msr3110_print_buf( (char*)__FUNCTION__, SendBuf, SendLen);

	for( loopIdx = 0; loopIdx < MSR3110_WRITE_RETRY_MAX; loopIdx++)
	{
		LogInformation( "%s loopIdx: %d \n", __FUNCTION__, loopIdx);

		retVal = write( fd, SendBuf, SendLen);
		LogInformation( "%s write msr3110, retVal %d. \n", __FUNCTION__, retVal);
		if( retVal == SendLen)
		{
			LogError( "%s SUCCESS: write msr3110, retVal %d. \n", __FUNCTION__, retVal);
			break;			
		}
	}
	if( retVal != SendLen)
	{
		LogError( "%s FAIL: write msr3110, retVal %d. \n", __FUNCTION__, retVal);
		retVal = -1;		
		goto end;		
	}

end:
	if( fd)
	{
		close( fd);
	}
	
	FUNC_END();
	return retVal;
	
	
}


int nfc_msr3110_interface_recv( unsigned char *RecvBuf, int RecvBufLen)
{
	FUNC_START();

	int retVal = 0;
	
	int fd = -2;
	int loopIdx = 0;

	fd = open( MSR3110_DEV_NAME, O_RDWR);
	if( fd < 0)
	{
		LogError( "FAIL: open msr3110, fd: %d. \n", fd);
		retVal = -1;		
		goto end;
	}

	for( loopIdx = 0; loopIdx < MSR3110_READ_RETRY_MAX; loopIdx++)
	{
		retVal = read( fd, RecvBuf, 2);
		if( retVal == 2)
		{
			LogInformation( "SUCCESS: read len. \n");
			break;
		}

		if( retVal < 0)
		{
			LogInformation( "CONTINUE. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
			continue;		
		}
		LogError( "ERROR: read len. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
		retVal = -1;
		goto end;
	}
	if( retVal < 0)
	{
		LogError( "ERROR: read len, out of retry. retVal: %d.\n", retVal);
		retVal = -1;
		goto end;
	}
	if( RecvBuf[ 0] != 0x02)
	{
		LogError( "ERROR: RecvBuf[ 0]: %02X != 0x02.\n", RecvBuf[ 0]);
		retVal = -1;
		goto end;		
	}

	LogInformation( "RecvBuf, len: %d. \n", RecvBuf[1]);
	
	for( loopIdx = 0; loopIdx < MSR3110_READ_RETRY_MAX; loopIdx++)
	{
		retVal = read( fd, &RecvBuf[2], RecvBuf[1] + 2);
		if( retVal == ( RecvBuf[1] + 2))
		{
			LogInformation( "SUCCESS: read data. \n");
			break;
		}

		if( retVal < 0)
		{
			LogInformation( "CONTINUE. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
			continue;		
		}
		LogError( "ERROR: read data. loopIdx: %d, retVal: %d.\n", loopIdx, retVal);
		retVal = -1;
		goto end;
	}
	if( retVal < 0)
	{
		LogError( "ERROR: read len out of retry. retVal: %d.\n", retVal);
		retVal = -1;
		goto end;
	}
	retVal = RecvBuf[1] + 4;

	nfc_msr3110_print_buf( (char *)__FUNCTION__, RecvBuf, RecvBuf[1] + 4);
	
end:
	if( fd)
	{
		close( fd);
	}
	
	FUNC_END();	
	return retVal;
}

int nfc_msr3110_interface_send_recv( unsigned char *SendBuf, int SendLen, unsigned char *RecvBuf, int RecvBufLen)
{
	FUNC_START();	
	int retVal = 0;

	retVal = nfc_msr3110_interface_send( SendBuf, SendLen);
	
	if( retVal < 0)
	{
		LogError( "%s FAIL: send, retVal: %d", __FUNCTION__, retVal);
		goto end;
	}

	retVal = nfc_msr3110_interface_recv( RecvBuf, RecvBufLen);
	if( retVal < 0)
	{
		LogError( "%s FAIL: recv, retVal: %d", __FUNCTION__, retVal);
		goto end;		
	}		

end:
	FUNC_END();
	return retVal;
}



