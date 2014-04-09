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

#ifndef SHA_COMM_H
#define SHA_COMM_H

#include <stdint.h>

#define SHA_WATCHDOG_TIMEOUT    (21)    //!< maximum watchdog timeout of device in s

#define SHA_COMMAND_SIZE_MIN    (6)        //!< minimum number of bytes in command (from count byte to second CRC byte)
#define SHA_RESPONSE_SIZE_MIN   (4)        //!< minimum number of bytes in response

#define SHA_BUFFER_POS_COUNT    (0)        //!< buffer index of count byte in command or response
#define SHA_BUFFER_POS_STATUS   (1)        //!< buffer index of status byte in response

#define SHA_STATUS_BYTE_WAKEUP  (0x11)    //!< status byte after wakeup
#define SHA_STATUS_BYTE_PARSE   (0x03)    //!< command parse error
#define SHA_STATUS_BYTE_EXEC    (0x0F)    //!< command execution error
#define SHA_STATUS_BYTE_COMM    (0xFF)    //!< communication error

#define SHA_RETRY_COUNT         (3)        //!< number of rx retries

#define WATCHDOG_TIMEOUT        (21)    //!< maximum watchdog timeout of device in s

/** \brief used as parameter group for communication functions */
typedef struct {
    uint8_t *txBuffer;
    uint8_t *rxBuffer;
    uint8_t rxSize;
    uint32_t executionDelay;
} SHA_CommParameters;


uint8_t SHAC_Wakeup();
int8_t SHAC_SendAndReceive(SHA_CommParameters *params);
#endif
