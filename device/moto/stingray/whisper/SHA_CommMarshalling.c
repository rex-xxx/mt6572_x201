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

#include <ctype.h>
#include <stdio.h>
#include <string.h>
#include "SHA_Comm.h"
#include "SHA_CommMarshalling.h"
#include "SHA_Status.h"




// Data definitions
uint8_t sendbuf[SENDBUF_SIZE];
uint8_t receicebuf[RECEIVEBUF_SIZE];
SHA_CommParameters commparms;




SHA_CommParameters* SHAC_GetData(void) {
    return &commparms;
}
/**
 *
 * \brief Sends an MAC command to the device.
 *
 * \param[in]  Mode
 * \param[in]  KeyID key id
 * \param[in]  Challenge
 * \param[out] 32 bytes of response
 * \return status of the operation
 */
uint8_t SHAC_Mac(uint8_t Mode, uint16_t KeyID, uint8_t *Challenge) {
    sendbuf[COUNT_IDX] = MAC_COUNT_SHORT;
    sendbuf[CMD_ORDINAL_IDX] = MAC;
    sendbuf[MAC_MODE_IDX] = Mode;
    memcpy(&sendbuf[MAC_KEYID_IDX], &KeyID, 2);
    if ((Challenge != NULL) && ((Mode & 0x01) == 0))
    {
        memcpy(&sendbuf[MAC_CHALL_IDX], Challenge, 32);
        sendbuf[COUNT_IDX] = MAC_COUNT_LARGE;
    }

    commparms.txBuffer = &sendbuf[0];
    commparms.rxBuffer = &receicebuf[0];
    commparms.rxSize = 35;
    commparms.executionDelay = MACDELAY;
    // Transfer the command to the chip
    //
    return SHAC_SendAndReceive(&commparms);

}



/**
 *
 * \brief Sends an Read command to the device.
 *
 * \param[in]  Zone
 * \param[in]  Address
 * \param[out] 4 or 32 bytes of response
 * \return status of the operation
 */
uint8_t SHAC_Read(uint8_t Zone, uint16_t Address) {
    sendbuf[COUNT_IDX] = READ_COUNT;
    sendbuf[CMD_ORDINAL_IDX] = READ;
    sendbuf[READ_ZONE_IDX] = Zone;
    memcpy(&sendbuf[READ_ADDR_IDX], &Address, 2);

    commparms.txBuffer = &sendbuf[0];
    commparms.rxBuffer = &receicebuf[0];
    if (Zone & 0x80)            // if bit 7 = 1, 32 bytes
        commparms.rxSize = 35;
    else
        commparms.rxSize = 7;
    // The execution delay will have to increased for clear text & enc data
    commparms.executionDelay = GENERALCMDDELAY;
    // Transfer the command to the chip
    //
    return SHAC_SendAndReceive(&commparms);

}

