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

#include "SA_Phys_Linux.h"
#include "SHA_Status.h"


/** \brief Sends a command to the device. (stub)
 * \param[in] count number of bytes to send
 * \param[in] buffer pointer to command buffer
 * \return status of the operation
 */
int8_t SHAP_SendCommand(uint8_t count, uint8_t *buffer) {
    return SHAP_SendBytes(count, buffer);
}


/** \brief Receives a response from the device. (stub)
 * \param[in] count number of bytes to receive
 * \param[in] buffer pointer to response buffer
 * \return status of the operation
 */
int8_t SHAP_ReceiveResponse(uint8_t count, uint8_t *buffer) {
    return SHAP_ReceiveBytes(count, buffer);
}


/** \brief Puts the device into idle state. (stub)
 * \return status of the operation
 */
int8_t SHAP_Idle() {
    return SHA_GEN_FAIL;
}


/** \brief Puts device into low-power state. (stub)
 *  \return status of the operation
 */
int8_t SHAP_Sleep() {
    return SHA_GEN_FAIL;
}
