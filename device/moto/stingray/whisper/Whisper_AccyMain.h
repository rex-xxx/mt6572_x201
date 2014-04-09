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

#ifndef ACCY_DET_H
#define ACCY_DET_H

#define LOG_TAG "whisperd"

#include <cutils/log.h>
#include <stdint.h>

#define MAX_IO_TIMEOUT     85   //85 ms
#define LOG_SIZE_LIMIT 10000

#define LOG_ACCY_ANDROID

#if defined(LOG_ACCY_ANDROID)
#define DBG_TRACE(fmt,x...) \
                do \
                { \
                    ALOGD(fmt" from %s() in %s(%d)\n",## x,__FUNCTION__,__FILE__,__LINE__); \
                }while(0)
#define DBG_ERROR(fmt,x...) \
                do \
                { \
                    ALOGE(fmt" from %s() in %s(%d)\n",## x,__FUNCTION__,__FILE__,__LINE__); \
                }while(0)

#elif defined(LOG_ACCY_FS)

#define DBG_TRACE(fmt,x...) \
        if(logFp != NULL) {if(currLogSize++ > LOG_SIZE_LIMIT){ currLogSize = 0; fseek(logFp, SEEK_SET, 0);} \
          fprintf(logFp,"ACCYDET :"fmt" from %s() in %s(%d)\n",## x,__FUNCTION__,__FILE__,__LINE__); fflush(logFp);} \
        else { } 


#define DBG_ERROR(fmt,x...) \
        if(logFp != NULL) {fprintf(logFp,"whisperd : ERROR = "fmt" from %s() in %s(%d)\n",## x,__FUNCTION__,__FILE__,__LINE__); fflush(logFp);} \
        else { } 

#else

#define DBG_TRACE(fmt,x...) do{} while (0)
#define DBG_ERROR(fmt,x...) do{} while (0)

#endif

enum    {
        GLOBAL_STATE_UNDOCKED,
        GLOBAL_STATE_DOCKED,
        GLOBAL_STATE_DOCKED_IDSUCC,
        GLOBAL_STATE_DOCKED_IDFAIL
        };

enum    {
        NO_DOCK,
        DESK_DOCK,
        CAR_DOCK,
        LE_DOCK,
        HE_DOCK
        };
        
enum    {
        PROTOCOL_UART,
        PROTOCOL_HID
        };        

extern int ttyFd;
extern FILE *logFp;
extern int currLogSize;

#endif
