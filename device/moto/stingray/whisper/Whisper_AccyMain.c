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

#include <signal.h>
#include <string.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/stat.h>
#include <errno.h>
#include <linux/netlink.h>
#include <cutils/properties.h>
#include <hardware_legacy/power.h>
#include <stdlib.h>
#include <time.h>
#include <linux/capability.h>
#include <linux/prctl.h>
#include <private/android_filesystem_config.h>

#include <linux/spi/cpcap.h>
#include <linux/hidraw.h>


#include "SA_Phys_Linux.h"
#include "SHA_CommMarshalling.h"
#include "SHA_Status.h"
#include "SHA_Comm.h"
#include "SHA_TimeUtils.h"
#include "Whisper_AccyMain.h"


/*==================================================================================================
                                          LOCAL CONSTANTS
==================================================================================================*/
static const char xintToChar[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
static const char wakeLockString[] = "ACCYDET";
static const uint8_t hidStatusQuery[] = {0x3f, 0x30, 0x00, 0x00, 0x42, 0x99}; 
static const uint8_t hidIdQuery[] = {0x3f, 0x12, 0x00, 0x01, 0xf1, 0xe2};


/*==================================================================================================
                                            LOCAL MACROS
==================================================================================================*/

#define MAX_TRY_WAKEUP                  4
#define MAX_TRY_COMM                    2
#define UART_SWITCH_STATE_PATH          "/sys/class/switch/whisper/state"
#define HID_SWITCH_STATE_PATH           "/sys/class/switch/whisper_hid/state"
#define DOCK_TYPE_OFFSET                27

#define DOCK_ATTACHED                   '1'
#define DOCK_NOT_ATTACHED               '0'

#define HID_SUCCESS                     1
#define HID_FAILURE                     0

#define ID_SUCCESS                      1

#define IOCTL_SUCCESS                   0
#define MAX_TRY_IOCTL                   5

#define HID_STATUS_QUERY_LENGTH         64
#define HID_ID_QUERY_LENGTH             64
#define HID_MAC_QUERY_LENGTH            64
#define HID_STATUS_MSG_LENGTH		64
#define HID_ID_MSG_LENGTH		64
#define HID_MAC_MSG_LENGTH		64

#define LOG_FILE_NAME                   "/data/whisper/whisperd.log"
#define LOG_FILE_PATH                   "/data/whisper"

/*==================================================================================================
                                          EXTERNAL VARIABLES
==================================================================================================*/

/*==================================================================================================
                                     LOCAL FUNCTION PROTOTYPES
==================================================================================================*/
static void copyResults(int8_t cmdSize, uint8_t *out);
static int  accyInit(void);
static void accySigHandler(signed int signal);
static void accyProtDaemon(void *arg);
static void readSwitchState(int);
static int  accySpawnThread();
static void createOutput(uint8_t *inp, char *out, int bytes);
static void waitForUevents();
static void doIoctl(int cmd, unsigned int data, char *dev_id, char *dev_prop);

/*==================================================================================================
                                          LOCAL VARIABLES
==================================================================================================*/
static SHA_CommParameters*    mainparms;
static sem_t SigAccyProtStart;
static int ueventFd;
static int wakeLock = 0;
static int globalState;
static int globalProtocol;
static int cpcapFd = -1;
static int hidFd = -1;

/*==================================================================================================
                                          GLOBAL VARIABLES
==================================================================================================*/
FILE *logFp = NULL;
int  currLogSize = 0;

/*==================================================================================================
                                          LOCAL FUNCTIONS
==================================================================================================*/


static void readSwitchState(int protocolType) {
    const int SIZE = 16;
    int switchFd = -1;
    char buf[SIZE];
    int count;

    if (protocolType == PROTOCOL_UART) {
        switchFd = open(UART_SWITCH_STATE_PATH, O_RDONLY, 0);
        if (switchFd == -1) {
            DBG_ERROR("Failed opening %s, errno = %s", UART_SWITCH_STATE_PATH, strerror(errno));
            return;
        }
    }
    else if (protocolType == PROTOCOL_HID) {
        switchFd = open(HID_SWITCH_STATE_PATH, O_RDONLY, 0);
        if (switchFd == -1) {
            DBG_ERROR("Failed opening %s, errno = %s", HID_SWITCH_STATE_PATH, strerror(errno));
            return;
        }
    }

    do {
      count = read(switchFd, buf, SIZE);
    } while (count < 0 && errno == EINTR);

    if (count < 1) {
        DBG_ERROR("Error reading switch, returned %d", count);
        close(switchFd);
        return;
    }

    if (buf[0] == DOCK_ATTACHED) {
        globalState = GLOBAL_STATE_DOCKED;
        if (protocolType == PROTOCOL_HID) {
            char hidDevice[] = "/dev/hidraw0";
            int i;
            struct hidraw_devinfo hiddevinfo;
            int status;

            globalProtocol = PROTOCOL_HID;
            DBG_TRACE("HID Dock Attached");

            for (i = 0; i < HIDRAW_MAX_DEVICES; i++) {
                hidFd = open(hidDevice, O_RDWR);

                if(hidFd < 0) {
                    DBG_ERROR("Failed to open HID Device:%s", hidDevice);
                    break;
                }
                else {
                    status = ioctl(hidFd, HIDIOCGRAWINFO, &hiddevinfo);

                    if (status != IOCTL_SUCCESS) {
                        DBG_ERROR("ioctl cmd:HIDIOCGRAWINFO failed for %s", hidDevice);
                    }
                    else {
                        if(hiddevinfo.vendor == 0x22b8 && hiddevinfo.product == 0x0938) {
                            DBG_TRACE("Found HD Dock: %s", hidDevice);
                            break;
                        }
                        else {
                            DBG_ERROR("Device: %s not a HD dock", hidDevice);
                            close(hidDevice);
                        }
                    }
                    // TODO: You cant do this.
                    hidDevice[11]++;
                }
            }

            hidDevice[11] = '0';
        }
        else if (protocolType == PROTOCOL_UART) {
            globalProtocol = PROTOCOL_UART;
        }
    }
    else if (buf[0] == DOCK_NOT_ATTACHED) {
        globalState = GLOBAL_STATE_UNDOCKED;
    }

    close(switchFd);
}


static void waitForUevents() {
    fd_set accySet;
    char msg[1024];
    int nready, status;

    FD_ZERO(&accySet);
    FD_SET(ueventFd, &accySet);

    /* at powerup, we might have missed the uevent. So, read switch */
    readSwitchState(PROTOCOL_HID);

    if (globalState == GLOBAL_STATE_DOCKED) {
        DBG_TRACE("HID Dock attached at Power up");
        sem_post(&SigAccyProtStart);
    }
    else {
        readSwitchState(PROTOCOL_UART);
        if (globalState == GLOBAL_STATE_DOCKED) {
            DBG_TRACE("UART Dock attached at Power up");
            sem_post(&SigAccyProtStart);
        }
    }

    while(1) {
        nready = select(ueventFd+1, &accySet, NULL, NULL, NULL);

        if (nready > 0) {
            if (FD_ISSET(ueventFd, &accySet)) {
                status = recv(ueventFd, msg, sizeof(msg), MSG_DONTWAIT);
                if ((status > 0) && (strcasestr(msg, "whisper_hid")) && (strcasestr(msg, "switch"))) {
                    readSwitchState(PROTOCOL_HID);
                    DBG_TRACE("HID: SEM POST after readSwitchState %d", globalState);
                    sem_post(&SigAccyProtStart);
                }
                else if ((status > 0) && (strcasestr(msg, "whisper")) && (strcasestr(msg, "switch"))) {
                    readSwitchState(PROTOCOL_UART);
                    DBG_TRACE("UART: SEM POST after readSwitchState %d", globalState);
                    sem_post(&SigAccyProtStart);
                }
            }
        }
        else {
            DBG_ERROR("Select errored out. nready = %d, errno = %s", nready, strerror(errno));
        }
    }
}


static int accySpawnThread() {
    pthread_t id;

    if (pthread_create(&id, NULL,  (void *(*)(void *))accyProtDaemon, NULL) != 0) {
        DBG_ERROR("Pthread create failed. errno = %s", strerror(errno));
        return 0;
    }

    return 1;
}

/** Responsible for protocol communication */
static void accyProtDaemon(void *arg) {
    struct sched_param sched;
    int currentPolicy, tryWakeup, tryComm;
    pthread_t threadId;
    uint8_t  wakeupSuccess;
    unsigned int dockDetails;
    uint8_t dockType = NO_DOCK;
    int status;
    struct timespec ts;
    struct timeval tv;
    uint8_t statusFuse[8], FSNo[8], RomSN[8], RomRNo[8];
    char devInfo[32];
    char devProp[8];

    threadId = pthread_self();
    status = pthread_getschedparam(threadId, &currentPolicy, &sched);

    if (status != 0) {
       DBG_ERROR("pthread_getschedparam error. erno = %s", strerror(status));
       return;
    }

    currentPolicy = SCHED_RR;
    sched.sched_priority = 70;

    status = pthread_setschedparam(threadId, currentPolicy, &sched);
    if (status != 0) {
        DBG_ERROR("pthread_setschedparam error. erno = %s", strerror(status));
        return;
    }

    switchUser();

    while(1) {
        do {
            status = sem_wait(&SigAccyProtStart);
        } while (status < 0 && errno == EINTR);

        if (status == -1) {
            DBG_ERROR("SEM WAIT failed with -1. errno = %s", strerror(errno));
            break;
        }

        /* If already undocked, why do anything */
        if (globalState == GLOBAL_STATE_UNDOCKED)  {
            DBG_TRACE("Thread. GLOBAL_STATE_UNDOCKED");
            continue;
        }

        if (globalProtocol == PROTOCOL_UART) {
            doIoctl(CPCAP_IOCTL_ACCY_WHISPER, CPCAP_WHISPER_ENABLE_UART, NULL, NULL);
        }

        wakeLock = acquire_wake_lock(PARTIAL_WAKE_LOCK, wakeLockString);
        tryComm = 1;
        while (tryComm && (globalState == GLOBAL_STATE_DOCKED)) {
            if (globalProtocol == PROTOCOL_UART) {
                if (SHA_SUCCESS == SHAP_OpenChannel()) {
                    tryWakeup = 1;
                    wakeupSuccess = 0;
                    while (tryWakeup) {
                        if (SHAC_Wakeup() == SHA_SUCCESS) {
                            DBG_TRACE("WAKEUP SUCCESS %d ", tryWakeup);
                            tryWakeup = 0;
                            wakeupSuccess = 1;
                        }
                        else {
                            if (tryWakeup == MAX_TRY_WAKEUP) {
                                DBG_ERROR("GIVING UP WAKEUP after %d tries", tryWakeup);
                                tryWakeup = 0;
                            }
                            else {
                                DBG_TRACE("TRYING WAKEUP ONCE MORE");
                                ts.tv_sec = 0;
                                ts.tv_nsec = 10000000; // 10 ms
                                nanosleep(&ts, NULL);
                                tryWakeup++;
                            }
                        }

                        if (globalState == GLOBAL_STATE_UNDOCKED) {
                            tryWakeup = 0;
                        }
                    }

                    if ((wakeupSuccess)  && (globalState == GLOBAL_STATE_DOCKED)) {
                        DBG_TRACE("Reading Status & MfgId");
                        // Read Status fuses and MfgId fuses
                        status = SHAC_Read(0x01, 0x0002);
                        if (status == SHA_SUCCESS) {
                            copyResults(8, statusFuse);
                            // TODO: bytes in wrong order for some reason??
                            uint8_t temp[2];
                            temp[0] = statusFuse[1];
                            temp[1] = statusFuse[3];
                            statusFuse[1] = temp[1];
                            statusFuse[3] = temp[0];
                        }

                        if (globalState == GLOBAL_STATE_DOCKED && status == SHA_SUCCESS) {
                            DBG_TRACE("Reading Serial Number");
                            // Read Fuse Serial number value
                            status = SHAC_Read(0x01, 0x0003); 
                            if (status == SHA_SUCCESS) {
                                copyResults(8, FSNo);
                            }
                        }

                        if (globalState == GLOBAL_STATE_DOCKED && status == SHA_SUCCESS) {
                            DBG_TRACE("Reading ROM MfgId and ROM SN");
                            // ROM MfgId and ROM SN
                            status = SHAC_Read(0x00, 0x0000); 
                            if (status == SHA_SUCCESS) {
                                copyResults(8, RomSN);
				DBG_TRACE("Authentication succeed");
                                globalState = GLOBAL_STATE_DOCKED_IDSUCC;
                            }
                        }
                    }
                }

                SHAP_CloseChannel();
            }
            else if (globalProtocol == PROTOCOL_HID) {
                uint8_t writebuff[65] = {0x0};
                uint8_t readbuff[65] = {0x0};
                uint8_t displaybuff[65] = {0x0};
                int hidStatus;

                DBG_TRACE("HID: Sending status query");
                hidStatus = HID_FAILURE;
                memset(writebuff,0x00,sizeof(writebuff));
                memcpy(writebuff, hidStatusQuery, sizeof(hidStatusQuery));

                status = write(hidFd, writebuff, HID_STATUS_QUERY_LENGTH);
                if (status != HID_STATUS_QUERY_LENGTH) {
                    DBG_ERROR("Failed writing status query (errno = %s)", strerror(errno));
                }
                else {
                    hidStatus = HID_SUCCESS;
                }

                if (globalState == GLOBAL_STATE_DOCKED && hidStatus == HID_SUCCESS) {
                    DBG_TRACE("HID: Reading status query response");
                    status = read(hidFd, readbuff, HID_STATUS_MSG_LENGTH);

                    if (status != HID_STATUS_MSG_LENGTH) {
                        DBG_ERROR("HID: Failed reading status query response, errno = %s", strerror(errno));
                        hidStatus = HID_FAILURE;
                    }
                    else {
                        DBG_TRACE("Contents of receive buffer");
                        DBG_TRACE("first 3 bytes: %02X%02X%02X", readbuff[0], readbuff[1], readbuff[2]);
                        DBG_TRACE("Status: %02X%02X", readbuff[3], readbuff[4]);
                        DBG_TRACE("Ref Num: %02X%02X", readbuff[5], readbuff[6]);
                        DBG_TRACE("Version: %s", &readbuff[6]);
                    }
                }

                if (globalState == GLOBAL_STATE_DOCKED && hidStatus == HID_SUCCESS) {
                    DBG_TRACE("HID: Sending ID query");
                    memset(writebuff,0x00,sizeof(writebuff));
                    memcpy(writebuff, hidIdQuery, sizeof(hidIdQuery));

                    status = write(hidFd, writebuff, HID_ID_QUERY_LENGTH);
                    if (status != HID_ID_QUERY_LENGTH) {
                        DBG_ERROR("HID: Error writing ID query, %d", status);
                        hidStatus = HID_FAILURE;
                    }
                }

                if (globalState == GLOBAL_STATE_DOCKED && hidStatus == HID_SUCCESS) {
                    DBG_TRACE("Reading ID query response");
                    status = read(hidFd, readbuff, HID_ID_MSG_LENGTH);

                    if (status != HID_ID_MSG_LENGTH) {
                        DBG_ERROR("HID: Error reading ID query response, errno = %s", strerror(errno));
                        hidStatus = HID_FAILURE;
                    }
                    else {
                        DBG_TRACE("Contents of receive buffer");
                        DBG_TRACE("first 3-2 bytes: %02X%02X%02X", readbuff[0], readbuff[1], readbuff[2]);
                        DBG_TRACE("Status: %02X%02X", readbuff[3], readbuff[4]);
                        DBG_TRACE("Ref Num: %02X%02X", readbuff[5], readbuff[6]);
                        DBG_TRACE("SEMU ID: %02X%02X%02X", readbuff[7], readbuff[8], readbuff[9]);
                        DBG_TRACE("Manufacturer ID: %02X", readbuff[10]);
                        DBG_TRACE("ROM Revision: %02X%02X%02X%02X", readbuff[11], readbuff[12], readbuff[13], readbuff[14]);
                        DBG_TRACE("Fuse SN: %02X%02X%02X%02X", readbuff[15], readbuff[16], readbuff[17], readbuff[18]);
                        DBG_TRACE("ROM SN: %02X%02X", readbuff[19], readbuff[20]);

                        statusFuse[1] = readbuff[6];
                        statusFuse[2] = readbuff[7];
                        statusFuse[3] = readbuff[8];
                        FSNo[1] = readbuff[14];
                        FSNo[2] = readbuff[15];
                        FSNo[3] = readbuff[16];
                        FSNo[4] = readbuff[17];
                        RomSN[3] = readbuff[18];
                        RomSN[4] = readbuff[19];

                        globalState = GLOBAL_STATE_DOCKED_IDSUCC;
                    }
                }
            }

            if (globalState == GLOBAL_STATE_DOCKED_IDSUCC) {
                dockType = NO_DOCK;
                if (statusFuse[1] == 0x0A && statusFuse[2] == 0xC0) {
                    dockType = LE_DOCK;
                    DBG_TRACE("It's a Low End Dock");
                }
                else if (statusFuse[1] == 0x12 && statusFuse[2] == 0xC0 && statusFuse[3] == 0x00) {
                    dockType = CAR_DOCK;
                    DBG_TRACE("It's a Car Dock");
                }
                else if (statusFuse[1] == 0x1A && statusFuse[2] == 0x80 && statusFuse[3] == 0x00) {
                    dockType = HE_DOCK;
                    DBG_TRACE("It's a High End Dock");
                }

                /* Format the output */
                createOutput(&FSNo[1],&devInfo[0], 4);
                createOutput(&RomSN[3],&devInfo[8], 2);
                devInfo[12] = 0;

                createOutput(&statusFuse[1], &devProp[0], 3);
                devProp[6] = 0;

                DBG_TRACE("ID SUCCESS %s", devInfo);
                if(dockType == NO_DOCK)
                    DBG_TRACE("UNKNOWN STATUS FUSES <%d><%d><%d>\n", statusFuse[1], statusFuse[2], statusFuse[3]);

                tryComm = 0;
                dockDetails = ID_SUCCESS;
                dockDetails |= (dockType << DOCK_TYPE_OFFSET);
                doIoctl(CPCAP_IOCTL_ACCY_WHISPER, dockDetails, devInfo, devProp);
                globalState = GLOBAL_STATE_DOCKED_IDSUCC;
                memset(devInfo,0x00,sizeof(devInfo));
                memset(devProp,0x00,sizeof(devProp));
            }

            /* if the global state is still docked, then increment the retry counter */
            if (globalState == GLOBAL_STATE_DOCKED) {
                if (tryComm == MAX_TRY_COMM) {
                    DBG_ERROR("GIVING UP AFTER %d tries", tryComm);
                    tryComm = 0;

                    dockDetails = 0; // set bit 0, for AUTH to be failure
                    doIoctl(CPCAP_IOCTL_ACCY_WHISPER, dockDetails, NULL, NULL);

                    globalState = GLOBAL_STATE_DOCKED_IDFAIL;
                }
                else {
                    ts.tv_sec = 0;
                    ts.tv_nsec = 100000000; // 100 ms
                    nanosleep(&ts, NULL);

                    tryComm++;
                    DBG_TRACE("Trying COMM %d time", tryComm);
                }
            }
        }

        if (wakeLock) {
            release_wake_lock(wakeLockString);
            wakeLock = 0;
        }
    }
}


static void createOutput(uint8_t *inp, char *out, int bytes) {
    int i, j;

    for (i = 0; i < bytes; i++) {
        j = (inp[i]  & 0x0F);
        out[i*2+1] = xintToChar[j];
        j = ((inp[i] >> 4) & 0xFF);
        out[i*2] = xintToChar[j];
    }
}


static void accySigHandler(signed int signal) {
    switch(signal) {
        case SIGINT:
        case SIGKILL:
        case SIGTERM:
             /* cose fds */
            if (cpcapFd > 0)
                close(cpcapFd);
            if (ttyFd > 0)
                close(ttyFd);
            if (hidFd > 0)
                close(hidFd);
            exit(0);
            break;
        default:
            break;
    }
}


static int accyInit(void) {
    struct sockaddr_nl addr;
    int sz = 64*1024;
    int s;
    struct sigaction shutdownAction;
    struct stat statBuf;

#if defined(LOG_ACCY_FS)
    if(stat(LOG_FILE_PATH, &statBuf) == 0) {
        logFp = fopen(LOG_FILE_NAME, "w");
        if (logFp == NULL) {
            ALOGE("whisperd: Unable to open the Logfile %s", LOG_FILE_NAME);
        }
    }
#endif

    cpcapFd = open("/dev/cpcap", O_RDWR);

    if (cpcapFd == -1) {
        DBG_ERROR("/dev/cpcap could not be opened. err = %s", strerror(errno));
    }
    else {
        DBG_TRACE("/dev/cpcap opened: %d", cpcapFd);
    }

    /* Setup the shutdown action. */
    shutdownAction.sa_handler = accySigHandler;
    sigemptyset(&shutdownAction.sa_mask);
    shutdownAction.sa_flags = 0;

    /* Setup the signal handler. */
    sigaction(SIGINT, &shutdownAction, NULL);
    sigaction(SIGKILL, &shutdownAction, NULL);
    sigaction(SIGTERM, &shutdownAction, NULL);

    globalState = GLOBAL_STATE_UNDOCKED;

    memset(&addr, 0, sizeof(addr));
    addr.nl_family = AF_NETLINK;
    addr.nl_pid = getpid();
    addr.nl_groups = 0xffffffff;

    s = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (s < 0) {
        DBG_ERROR("Socket failed. err = %s", strerror(errno));
        return 0;
    }

    setsockopt(s, SOL_SOCKET, SO_RCVBUFFORCE, &sz, sizeof(sz));

    if (bind(s, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        DBG_ERROR("Bind failed. errno = %d", errno);
        close(s);
        return 0;
    }

    ueventFd = s;

    return (ueventFd > 0);
}



void doIoctl(int cmd, unsigned int data, char *dev_id, char *dev_prop) {
    int i, status = -1;
    struct timespec ts;
    struct cpcap_whisper_request req;

    memset(req.dock_id, 0, CPCAP_WHISPER_ID_SIZE);
    req.cmd = data;
    if(dev_id != NULL)
      strcpy(req.dock_id, dev_id);
    if(dev_prop != NULL)
      strcpy(req.dock_prop, dev_prop);

    for (i = 0; i < MAX_TRY_IOCTL; i++) {
        DBG_TRACE("ioctl cmd %d: %d,", cmd, data);
        if(dev_id == NULL) {
            DBG_TRACE("ioctl id = NULL");
        }
        else {
            DBG_TRACE("ioctl id = <%s>\n", req.dock_id);
        }

        status = ioctl(cpcapFd, cmd, &req);

        if (status != IOCTL_SUCCESS) {
            DBG_ERROR("ioctl returned %d with error: %d", status, errno);
            ts.tv_sec = 0;
            ts.tv_nsec = 50000000; // 50 ms
            DBG_TRACE("Wait 50ms.");
            nanosleep(&ts, NULL);
            globalState = GLOBAL_STATE_UNDOCKED;
        }
        else {
            DBG_TRACE("ioctl success");
            globalState = GLOBAL_STATE_DOCKED;
            break;
        }
    }

    if (hidFd > 0) {
        close(hidFd);
        hidFd = -1;
    }

    return;
}


static void copyResults(int8_t cmdSize, uint8_t *out) {
    int i;
    char charOut[128];

    mainparms = SHAC_GetData();

    createOutput(mainparms->txBuffer, charOut, cmdSize);
    charOut[cmdSize*2] = '\0';

    DBG_TRACE("Send Value: %s", charOut);

    for(i = 0; i < mainparms->rxSize; i++) {
            out[i] = mainparms->rxBuffer[i];
    }

    createOutput(mainparms->rxBuffer, charOut, cmdSize);
    charOut[cmdSize*2] = '\0';
    DBG_TRACE("Receive Value: = %s", charOut);
}



int main(int argc, char *argv[]) {
    int retVal;

    retVal = accyInit();

    if (retVal <= 0) {
       DBG_ERROR("accyInit failed");
    }

    if (sem_init(&SigAccyProtStart, 0, 0) != 0) {
        DBG_ERROR("Sem_init failed. errno = %d", errno);
    }

    //TODO:  First time failure to set parameters
    SHAP_OpenChannel();
    SHAP_CloseFile();

    retVal = accySpawnThread();

    switchUser();
    waitForUevents();

    return 1;
}


int switchUser( void ) {
    int status;

    status = prctl(PR_SET_KEEPCAPS, 1, 0, 0, 0);
    if (status < 0) {
        return status;
    }

    status = setuid(AID_RADIO);
    if (status < 0) {
         return status;
    }

    struct __user_cap_header_struct header;
    struct __user_cap_data_struct cap;
    header.version = _LINUX_CAPABILITY_VERSION;
    header.pid = 0;
    cap.effective = cap.permitted = 1 << CAP_NET_ADMIN;
    cap.inheritable = 0;
    status = capset(&header, &cap);

    return status;
}

