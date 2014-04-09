// Copyright (c) 2010, Atmel Corporation.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//     * Neither the name of Atmel nor the
//       names of its contributors may be used to endorse or promote products
//       derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <termios.h>
#include <time.h>
#include <unistd.h>

#include "SA_Phys_Linux.h"
#include "SHA_Status.h"
#include "SHA_TimeUtils.h"
#include "Whisper_AccyMain.h"



#define MAX_BUF_LEN     512
#define M_ONE_BIT       0x7F
#define M_ZERO_BIT      0x7D
#define OPPBAUD         B230400
#define WAKEBAUD        B115200


static void configTtyParams();
static int8_t setBaudRate(speed_t Inspeed);
static int8_t writeToDevice(uint8_t *data, uint8_t len);
static int8_t readFromDevice(uint8_t *readBuf, uint16_t readLen, 
                             uint8_t CmdOfset, uint16_t *retBytes);
static int8_t sleepDevice(void);
static int16_t formatBytes(uint8_t *ByteData, uint8_t *ByteDataRaw, 
                           int16_t lenData);


static const char ttyPort[] = "/dev/ttyHS0";
static uint8_t readwriteBuf[MAX_BUF_LEN];
static uint8_t WakeStr = {0x00};
static uint8_t* pWake = &WakeStr;
static uint8_t TransmitStr = {0x88};
static uint8_t* pTrm = &TransmitStr;
static uint8_t CmdStr = {0x77};
static uint8_t* pCmd = &CmdStr;
static uint8_t SleepStr= {0xCC};
static uint8_t InStr[41];
static uint8_t* pIStr = InStr;
static fd_set readfs;
static struct termios termOptions;

int ttyFd = -1;


 /*  Sets up and configures the UART for use */
int8_t SHAP_OpenChannel(void) {
    struct termios tty;
    speed_t speed;
    struct timespec ts;

    ttyFd = open(ttyPort, O_RDWR);
    if (ttyFd == -1) {
        DBG_ERROR("Error unable to open device: %s", ttyPort);
        return SHA_COMM_FAIL;
    }

    DBG_TRACE("%s opened with port %d", ttyPort, ttyFd);
    if (tcflush(ttyFd, TCIOFLUSH) == 0) {
        DBG_TRACE("The input and output queues have been flushed");
    }
    else {
       DBG_ERROR("tcflush() error");
    }

    configTtyParams();

    ts.tv_sec = 0;
    ts.tv_nsec = MAX_IO_TIMEOUT * 1000000; 
    nanosleep(&ts, NULL);

    return SHA_SUCCESS;
}



int8_t SHAP_CloseChannel(void) {
    int8_t ret = sleepDevice();
    close(ttyFd);
    return ret;
}

int8_t SHAP_SendBytes(uint8_t count, uint8_t *buffer) {
    uint16_t bytesRead;
    int8_t i, retVal;

    if (!count || !buffer) {
        DBG_ERROR("Bad input");
        return SHA_BAD_PARAM;
    }

    if (tcflush(ttyFd, TCIOFLUSH) == 0) {
        DBG_TRACE("The input and output queues have been flushed");
    }
    else {
        DBG_ERROR("tcflush() error");
    }

    memmove(&buffer[1], buffer, count);
    buffer[0] = CmdStr;

    writeToDevice(buffer, count+1);

    // Read the echo back ...
    readFromDevice(readwriteBuf, 8*(count+1), 0, &bytesRead);  

    if (tcflush(ttyFd, TCIFLUSH) == 0) {
       DBG_TRACE("The input queue has been flushed");
    }
    else {
       DBG_ERROR("tcflush() error");
    }

    return SHA_SUCCESS;
}


int8_t SHAP_ReceiveBytes(uint8_t recCommLen, uint8_t *dataBuf) {
    struct timespec ts;
    uint16_t bytesRead;
    int8_t i,iResVal, cmdLen = recCommLen;

    if (!recCommLen || !dataBuf) {
        return SHA_BAD_PARAM;
    }

    if (writeToDevice(pTrm, 1) == 1) {
        DBG_TRACE("Test Write to %s successful", ttyPort);
    }
    else {
        DBG_ERROR("Test Write to %s unsuccessful", ttyPort);
    }

    iResVal = readFromDevice(dataBuf, (cmdLen+1)*8, 8, &bytesRead);

    if (iResVal == SHA_COMM_FAIL) {
        DBG_ERROR("Read Error unable to read port: %d from device: %s", ttyFd, ttyPort);
        return SHA_COMM_FAIL;
    }

    return SHA_SUCCESS;
}




void SHAP_CloseFile(void) {
    struct timespec ts;
    close(ttyFd);

    ts.tv_sec = 0;
    ts.tv_nsec = MAX_IO_TIMEOUT * 1000000; 
    nanosleep(&ts, NULL);
}




/*  Reads the message from device. returns NULL if error */
static int8_t readFromDevice(uint8_t *readBuf, uint16_t readLen, 
                             uint8_t CmdOfset, uint16_t *retBytes) {
    int8_t goOn = 1;
    struct timeval Timeout;
    uint16_t numBytesRead = 0;
    int retVal;
    uint16_t i;

    Timeout.tv_usec = 200000;
    Timeout.tv_sec = 0;
    *retBytes = 0;

    for (i = 0; i < sizeof(readwriteBuf); i++) {
        readwriteBuf[i]= 0x00;
    }

    while (goOn) {
        FD_SET(ttyFd, &readfs);
        retVal = select(ttyFd+1, &readfs, NULL, NULL, &Timeout);

        if (retVal == 0) {
            DBG_ERROR("Timeout on select() occurred on port %d. Receive <%d> bytes", ttyFd, numBytesRead);
            if (numBytesRead > 0) {
                return SHA_SUCCESS;
            }
            else {
                return SHA_COMM_FAIL;
            }
        }

        if (retVal < 0 && errno == EINTR) {
            DBG_ERROR("SELECT returned EINTR ");
            continue;
        }

        if (FD_ISSET(ttyFd, &readfs)) {
            do {
                retVal = read(ttyFd, &readwriteBuf[numBytesRead], MAX_BUF_LEN);
            } while (retVal < 0 && errno == EINTR);

            if (retVal > 0) {
                numBytesRead += retVal;
                *retBytes = numBytesRead;

                DBG_TRACE("REQ READ LEN = %d, NUM BYT READ = %d, retVal = %d offset = %d", 
                           readLen, numBytesRead, retVal, CmdOfset);

                if (numBytesRead >= (readLen)) {
                    DBG_TRACE("Read Success");
                    break;
                }
            }
        }
        else {
            DBG_ERROR("Select Error. ERRNO = %d", errno);
        }
    }

    formatBytes(readBuf, &readwriteBuf[CmdOfset], readLen-CmdOfset);

    return SHA_SUCCESS;
}




/* Transmits a message to be sent over tty */
static int8_t writeToDevice(uint8_t *data, uint8_t len) {
    uint16_t i,j;
    int nbytes, nwritten;
    uint8_t *byteptr;

    // Every byte gets transferred into 8 bytes
    if (len*8 > MAX_BUF_LEN) {
        return SHA_COMM_FAIL;
    }

    for(i = 0; i < len; i++) {
        for(j = 0; j < 8; j++) {
            if (data[i] & (1 << j)) {
                readwriteBuf[(i*8)+j] = M_ONE_BIT;
            }
            else {
                readwriteBuf[(i*8)+j] = M_ZERO_BIT;
            }
        }
    }

    do {
        nwritten = write(ttyFd, readwriteBuf, len*8);
    } while (nwritten < 0 && errno == EINTR);

    if (nwritten == -1) {
        DBG_ERROR("Write Failed with errno = %d", errno);
        return SHA_COMM_FAIL;
    }
    else if (nwritten != len*8)   {
        DBG_ERROR("ERROR. write less than requested<%d>. written: %i",nwritten, len*8);
    }

    nbytes = nwritten / 8;

    return nbytes;
}




/* Formats the data received from UART to byte data */
static int16_t formatBytes(uint8_t *ByteData, uint8_t *ByteDataRaw,
                           int16_t lenData) {
    uint16_t i,j;
    int16_t retLen = lenData;

    for(j = 0; j < retLen/8;j++) {
        for (i = 0; i < 8; i++) {
            if ((ByteDataRaw[(8 *j)+i] ^ 0x7F) & 0x7C) {
                ByteData[j] &= ~(1 << i);
            }
            else {
                ByteData[j] |= (1 << i);
            }
        }
    }

    return SHA_SUCCESS;
}



/* Wakes the device */ 
int8_t SHAP_WakeDevice(void) {
    int iResVal;
    struct timespec ts;
    uint16_t bytes_read;
    uint8_t bytes_written;
    ssize_t osize;

    tcflush(ttyFd, TCIOFLUSH);

    // Set Start Token Speed
    setBaudRate(WAKEBAUD);

    // Send Start Token
    do {
        osize = write(ttyFd, pWake, 1);
    } while (osize < 0 && errno == EINTR);

    if (osize == -1) {
        DBG_ERROR("Write Failed with errno = %d", errno);
        return SHA_COMM_FAIL;
    }

    ts.tv_sec = 0;
    ts.tv_nsec = 3000000;
    nanosleep(&ts, NULL);

    // set the Baud Rate to Comm speed
    setBaudRate(OPPBAUD);
    if (writeToDevice(pTrm, 1) == 1) {
        DBG_TRACE("Wakeup Write to %s successful", ttyPort);
    }
    else {
        DBG_TRACE("Wakeup Write to %s unsuccessful", ttyPort);
    }


    iResVal = readFromDevice(pIStr, 41, 9, &bytes_read);

    if (iResVal == SHA_COMM_FAIL || bytes_read < 41) {
        sleepDevice();
        DBG_ERROR("WakeUp Error unable to read port: %d, Bytes Read = %d", ttyFd, bytes_read);
        return SHA_COMM_FAIL;
    }

    if (tcflush(ttyFd, TCIOFLUSH) == 0) {
       DBG_TRACE("The input and output queues have been flushed.");
    }
    else {
       DBG_ERROR("tcflush() error");
    }

    if (pIStr[0] == 0x04 && pIStr[1] == 0x11) {
        DBG_TRACE("WakeUp Done");
        return SHA_SUCCESS;
    }
    else {
        DBG_ERROR("WakeUp Fail");
        sleepDevice();
        return SHA_CMD_FAIL;
    }
}



/**
 * Transmits a message to be sent over tty
 *
 * \param[out] Success or fail flag
 * \return status of the operation
 */
static int8_t sleepDevice(void)
{
    uint8_t *byteptr = &SleepStr;
    ssize_t osize;
    struct timespec ts;
    do {
        osize = write(ttyFd, byteptr, 1);
    } while (osize < 0 && errno == EINTR);

    if (osize == -1) {
        DBG_ERROR("Write Failed errno = %d", errno);
        return SHA_COMM_FAIL;
    }
    ts.tv_sec = 0;
    ts.tv_nsec = 100000000; // sleep for 100ms
    nanosleep(&ts, NULL);

    return SHA_SUCCESS;
}

void SA_Delay(uint32_t delay)
{
    struct timespec ts;

    ts.tv_sec = 0;
    ts.tv_nsec = delay*1000; // convert us to ns
    nanosleep(&ts, NULL);
}

/*  Sets the baudrate of the tty port */
static int8_t setBaudRate(speed_t Inspeed) {
    int8_t ret;

    ret = tcgetattr( ttyFd, &termOptions );

    if (ret == -1) {
        DBG_ERROR("Error returned by tcgetattr. errno = %d", errno);
        return SHA_COMM_FAIL;
    }

    cfsetospeed(&termOptions, Inspeed); 
    cfsetispeed(&termOptions, Inspeed); 
    ret = tcsetattr(ttyFd, TCSANOW, &termOptions );
    if (ret == -1) {
        DBG_ERROR("Error returned by tcsetattr. errno = %d", errno);
        return SHA_COMM_FAIL;
    }

    return SHA_SUCCESS;
}

static void configTtyParams()
{

    struct termios tty;

    // Get the existing options //
    tcgetattr(ttyFd, &tty);

    // Reset Control mode to 0. And enable just what you need //
    tty.c_cflag = 0;
    tty.c_cflag |= CLOCAL;     // ignore modem control lines //
    tty.c_cflag |= CREAD;      // enable receiver
    tty.c_cflag |= CS7;        // use 7 data bits
    tty.c_cflag &= ~CRTSCTS;   // do not use RTS and CTS handshake

    tty.c_iflag = INPCK;
    tty.c_cc[VINTR]    = 0;     // Ctrl-c
    tty.c_cc[VQUIT]    = 0;     /* Ctrl-\ */
    tty.c_cc[VERASE]   = 0;     // del
    tty.c_cc[VKILL]    = 0;     // @
    tty.c_cc[VEOF]     = 0;     // Ctrl-d
    tty.c_cc[VTIME]    = 0;     /// inter-character timer unused
    tty.c_cc[VMIN]     = 1;     // blocking read until 1 character arrives
    tty.c_cc[VSWTC]    = 0;     // '\0'
    tty.c_cc[VSTART]   = 0;     // Ctrl-q
    tty.c_cc[VSTOP]    = 0;     // Ctrl-s
    tty.c_cc[VSUSP]    = 0;     // Ctrl-z
    tty.c_cc[VEOL]     = 0;     // '\0'
    tty.c_cc[VREPRINT] = 0;     // Ctrl-r
    tty.c_cc[VDISCARD] = 0;     // Ctrl-u
    tty.c_cc[VWERASE]  = 0;     // Ctrl-w
    tty.c_cc[VLNEXT]   = 0;     // Ctrl-v
    tty.c_cc[VEOL2]    = 0;     // '\0'


    // Reset Input mode to 0. And enalbe just what you need //
    tty.c_iflag = 0;
    tty.c_iflag |= IGNBRK; 
    tty.c_iflag |= INPCK;   // Enable input parity checking //


    // Reset output mode to 0. And enable just what you need //
    tty.c_oflag = 0;

    // Reset local mode to 0. And enable just what you need //
    tty.c_lflag = 0;

    tcflush(ttyFd, TCIFLUSH);
    tcsetattr(ttyFd, TCSANOW, &tty);
}


