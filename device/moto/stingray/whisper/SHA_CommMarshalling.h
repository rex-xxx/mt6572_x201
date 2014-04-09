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

#ifndef SHA_COMMMARSHALLING_H
#define SHA_COMMMARSHALLING_H

#include <ctype.h>
#include "SHA_Comm.h"

// General Definitions
#define SENDBUF_SIZE            128
#define RECEIVEBUF_SIZE         128


// Command ordinal definitions
#define DERIVE_KEY              0x1C
#define DEVREV                  0x30
#define GENDIG                  0x15
#define HMAC                    0x11
#define HOSTHMAC                0x21
#define HOSTMAC                 0x28
#define LOCK                    0x17
#define MAC                     0x08
#define NONCE                   0x16
#define PAUSE                   0x01
#define RANDOM                  0x1B
#define READ                    0x02
#define TEMPSENSE               0x18
#define WRITE                   0x12




/* Command Packet definition

Byte #            Name            Meaning
------            ----            -------
0                Count            Number of bytes in the packet, includes the byte count, body and the checksum
1                Ordinal            Command Opcode (Ordinal)
2 -> n            Cmd Parameters    Parameters for specific command
n+1 & n+2        Checksum        Checksum of the command packet

 */

//////////////////////////////////////////////////////////////////////
// General command indexes
#define COUNT_IDX               0
#define CMD_ORDINAL_IDX         1

//////////////////////////////////////////////////////////////////////
// DriveKey command
#define DK_RANDOM_IDX           2
#define DK_TARGETKEY_IDX        3
#define DK_MAC_IDX              5

#define DK_COUNT_SMALL          7
#define DK_COUNT_LARGE          39

//////////////////////////////////////////////////////////////////////
// DevRev command
#define DR_PARAM1_IDX           2
#define DR_PARAM2_IDX           3

#define DR_COUNT                7

//////////////////////////////////////////////////////////////////////
// GenDig command
#define GD_ZONE_IDX             2
#define GD_KEYID_IDX            3
#define GD_DATA_IDX             5

#define GD_COUNT                7
#define GD_COUNT_DATA           11

//////////////////////////////////////////////////////////////////////
// HMAC command
#define HM_MODE_IDX             2
#define HM_KEYID_IDX            3

#define HM_COUNT                7

//////////////////////////////////////////////////////////////////////
// HostHMAC command
#define HHMAC_MODE_IDX          2
#define HHMAC_KEYID_IDX         3
#define HHMAC_CLIENTRSP         5
#define HHMAC_DATA              37
#define HHMAC_COUNT             52

//////////////////////////////////////////////////////////////////////
// HostMAC command
#define HOSTMAC_MODE_IDX        2
#define HOSTMAC_KEYID_IDX       3
#define HOSTMAC_CLIENCHALL      5
#define HOSTMAC_CLIENTRSP       37
#define HOSTMAC_DATA            69
#define HOSTMAC_COUNT           84

//////////////////////////////////////////////////////////////////////
// Lock command
#define LK_ZONE_IDX             2
#define LK_SUMMARY_IDX          3

#define LK_COUNT                5

//////////////////////////////////////////////////////////////////////
// Mac command
#define MAC_MODE_IDX            2
#define MAC_KEYID_IDX           3
#define MAC_CHALL_IDX           5

#define MAC_COUNT_SHORT         7
#define MAC_COUNT_LARGE         39

//////////////////////////////////////////////////////////////////////
// Nonce command
#define N_MODE_IDX              2
#define N_ZERO_IDX              3
#define N_NUMIN_IDX             5

#define N_COUNT_SHORT           27
#define N_COUNT_LONG            39

//////////////////////////////////////////////////////////////////////
// Pause command
#define P_SELECT_IDX            2
#define P_ZERO_IDX              3

#define P_COUNT                 7

//////////////////////////////////////////////////////////////////////
// Random command
#define RAN_MODE_IDX            2
#define RAN_ZERO_IDX            3

#define RAN_COUNT               7

//////////////////////////////////////////////////////////////////////
// Read command
#define READ_ZONE_IDX           2
#define READ_ADDR_IDX           3

#define READ_COUNT              7

//////////////////////////////////////////////////////////////////////
// TempSense command
#define TS_ZERO1_IDX            2
#define TS_ZERO2_IDX            3

#define TS_COUNT                7

//////////////////////////////////////////////////////////////////////
// Write command
#define W_ZONE_IDX              2
#define W_ADDR_IDX              3
#define W_VALUE_IDX             5
#define W_MAC_VS_IDX            9
#define W_MAC_VL_IDX            37

#define W_COUNT_SHORT           11
#define W_COUNT_LONG            39
#define W_COUNT_SHORT_MAC       43
#define W_COUNT_LONG_MAC        71

//////////////////////////////////////////////////////////////////////
// Command Timming definitions
#define MACDELAY                30000
#define READDELAY               100000
#define ENCREADDELAY            100000
#define GENDIGDELAY             15000
#define WRITEDELAY              15000
#define ENCWRITEDELAY           15000
#define HMACDELAY               30000
#define SHORTRANDDELAY          15000
#define LONGRANDDELAY           15000
#define LOCKDELAY               15000
#define TEMPDELAY               10000
#define GENERALCMDDELAY         1000


//////////////////////////////////////////////////////////////////////
// Function definitions
uint8_t SHAC_DeriveKey(uint8_t Random, uint16_t TargetKey, uint8_t *Data);
uint8_t SHAC_DevRev();
uint8_t SHAC_GenDig(uint8_t Zone, uint16_t KeyID, uint8_t *Data);
uint8_t SHAC_HMAC(uint8_t Mode, uint16_t KeyID);
uint8_t SHAC_HostHMAC(uint8_t Mode, uint16_t KeyID, uint8_t *ClietResponse, uint8_t *OtherData);
uint8_t SHAC_HostMAC(uint8_t Mode, uint16_t KeyID, uint8_t *ClietChallenge, uint8_t *ClietResponse, uint8_t *OtherData);
uint8_t SHAC_Lock(uint8_t Zone, uint16_t Summary);
uint8_t SHAC_Mac(uint8_t Mode, uint16_t KeyID, uint8_t *Challenge);
uint8_t SHAC_Nonce(uint8_t Mode, uint8_t *Numin);
uint8_t SHAC_Pause(uint8_t Selector);
uint8_t SHAC_Random(uint8_t Mode);
uint8_t SHAC_Read(uint8_t Zone, uint16_t Address);
uint8_t SHAC_TempSense(uint8_t *Temp);
uint8_t SHAC_Write(uint8_t Zone, uint16_t Address, uint8_t *Value, uint8_t *MACData);

SHA_CommParameters* SHAC_GetData(void);

#endif
