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
#include "SHA_TimeUtils.h"


// The values below are valid for an AVR 8-bit processor running at 16 MHz.
// Code is compiled with -O1.

//!< Offsets time needed to prepare microseconds loop.
#define SHA_TIME_OFFSET_US      (7)

//!< Fill a while loop with these CPU instructions to achieve 1 us per iteration.
#define SHA_TIME_LOOP_FILLER    asm volatile ("nop\n\tnop\n\tnop\n\tnop\n\tnop\n\tnop\n\tnop")


extern void SA_Delay(uint32_t delay);






/** \brief Delays for a certain amount of time.
 *
 * \param[in] delay Delay for this number of microseconds (us).
 */
void SHAP_Delay(uint32_t delay) {
    if (delay <= SHA_TIME_OFFSET_US)
        // We spent already this much time entering this function.
        // With other words: A value <= SHA_TIME_OFFSET_US will not produce the desired delay.
        return;

    delay -= SHA_TIME_OFFSET_US;


    SA_Delay(delay);

    return;
}


void loop_delay(int8_t multp) {
    int i, base = 2000000 *multp;
    for(i=0;i<base;i++) {
        double value = 10.0; //((i * 33.3) - 12.1) / 10.0;
        value += 1.0;
    }
}
