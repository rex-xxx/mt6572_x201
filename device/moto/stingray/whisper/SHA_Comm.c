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

#include <stdint.h>
#include "SHA_Comm.h"
#include "SHA_CommInterface.h"
#include "SHA_TimeUtils.h"
#include "SHA_Status.h"


/** \brief Calculates CRC
 *
 * \param[in] data pointer to data for which CRC should be calculated
 * \param[in] count number of bytes in buffer
 * \return
 */
uint16_t SHAC_CalculateCrc(uint8_t *data, uint8_t count) {
    uint8_t counter;
    uint16_t crc = 0x0000;
    uint16_t poly = 0x8005;
    uint16_t i;
    uint8_t nbit, cbit;

    for (counter = 0; counter < count; counter++) {
        for (i = 0x1; i < 0x100; i <<= 1) {
            nbit = (data[counter] & (uint8_t) i) ? 1 : 0;
            cbit = (0x8000 & crc) ? 1 : 0;
            crc <<= 1;
            if (nbit ^ cbit)
                crc ^= poly;
        }
    }
#ifdef BIGENDIAN
   crc = (crc << 8) | (crc >> 8);  // flip byte order
#endif

   return crc;
}


uint8_t SHAC_Wakeup() {
    return(SHAP_WakeDevice());
}


/** \brief Runs a communication sequence: Append CRC to tx buffer, send command, delay, receive and verify response.
 *
 * The first byte in tx buffer must be its byte count.
 * If CRC or count of the response is incorrect, or a command byte got "nacked" (TWI),
 * this function requests to re-send the response.
 * If the response contains an error status, this function resends the command.
 *
 * \param[in] params pointer to parameter structure
 * \return status of the operation
 */
int8_t SHAC_SendAndReceive(SHA_CommParameters *params) {
    uint8_t rxSize = params->rxSize;
    uint8_t *rxBuffer = params->rxBuffer;
    uint8_t *txBuffer = params->txBuffer;
    uint8_t count;
    uint8_t countMinusCrc;
    int8_t status;
    uint8_t nRetries;
    uint8_t i;
    uint8_t statusByte;

    if (!params)
        return SHA_BAD_PARAM;
    if (!params->txBuffer)
        return SHA_BAD_PARAM;

    count = txBuffer[SHA_BUFFER_POS_COUNT];
    countMinusCrc = count - 2;
    if (count < SHA_COMMAND_SIZE_MIN || rxSize < SHA_RESPONSE_SIZE_MIN || !rxBuffer)
        return SHA_BAD_PARAM;

    // Append CRC and send command.
    *((uint16_t *) (txBuffer + countMinusCrc)) = SHAC_CalculateCrc(txBuffer, countMinusCrc);
    status = SHAP_SendCommand(count, txBuffer);

    if (status != SHA_SUCCESS) {
        // Re-send command.
        status = SHAP_SendCommand(count, txBuffer);
        if (status != SHA_SUCCESS) {
            // We lost communication. Wait until device goes to sleep.
            //SHAP_Delay(WATCHDOG_TIMEOUT * 1000000);
            return status;
        }
    }

    // Wait for device to finish command execution.
    SHAP_Delay(params->executionDelay);

    // Receive response.
    nRetries = SHA_RETRY_COUNT;
    do {
        // Reset response buffer.
        for (i = 0; i < rxSize; i++)
            rxBuffer[i] = 0;

        status = SHAP_ReceiveResponse(rxSize, rxBuffer);
        if (status != SHA_SUCCESS && !rxBuffer[SHA_BUFFER_POS_COUNT]) {
            // We lost communication. Wait until device goes to sleep.
            //SHAP_Delay(WATCHDOG_TIMEOUT * 1000000);
            return status;
        }

        // Check whether we received a status packet instead of a full response.
        if (rxSize != SHA_RESPONSE_SIZE_MIN && rxBuffer[SHA_BUFFER_POS_COUNT] == SHA_RESPONSE_SIZE_MIN) {
            statusByte = rxBuffer[SHA_BUFFER_POS_STATUS];
            if (statusByte == SHA_STATUS_BYTE_PARSE)
                return SHA_PARSE_ERROR;
            if (statusByte == SHA_STATUS_BYTE_EXEC)
                return SHA_CMD_FAIL;
            if (statusByte != SHA_STATUS_BYTE_COMM)
                return SHA_STATUS_UNKNOWN;

            // Communication error. Request device to re-transmit response.
            continue;
        }

        // Received response. Verify count and CRC.
        if (rxBuffer[SHA_BUFFER_POS_COUNT] != rxSize) {
            status = SHA_BAD_SIZE;
            continue;
        }
        countMinusCrc = rxSize - 2;
        status = (*((uint16_t *) (rxBuffer + countMinusCrc)) == SHAC_CalculateCrc(rxBuffer, countMinusCrc))
            ? SHA_SUCCESS : SHA_BAD_CRC;
    } while (nRetries-- && status != SHA_SUCCESS);

    return status;
}
