/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __WME_MIFARE_H
#define __WME_MIFARE_H

/*******************************************************************************
   Contains the declaration of the Mifare functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_MIFARE_CLASSIC_ACCESS_BITS_OFFSET           6

#define P_MIFARE_CLASSIC_1K_NUMBER_OF_SECTOR          16
#define P_MIFARE_CLASSIC_4K_NUMBER_OF_SECTOR          40

#define P_MIFARE_CLASSIC_BLOCK_SIZE                   16
#define P_MIFARE_CLASSIC_BLOCK_NUMBER_PER_SECTOR      4
#define P_MIFARE_CLASSIC_BLOCK_SECTOR_TRAILER         3

#define P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR                     32
#define P_MIFARE_CLASSIC_4K_BLOCK_NUMBER_PER_EXTENDED_SECTOR    16 /* for sector 32-39 */
#define P_MIFARE_CLASSIC_4K_BLOCK_EXTENDED_SECTOR_TRAILER       15 /* for sector 32-39 */

#define P_MIFARE_CLASSIC_DEFAULT_TIMEOUT 6 /* 19,8 ms*/


/* Return the number of block for a sector */
#define P_MIFARE_CLASSIC_GET_NUMBER_OF_BLOCK( nSector ) ( (nSector < 32)?  P_MIFARE_CLASSIC_BLOCK_NUMBER_PER_SECTOR \
                                                                        :  P_MIFARE_CLASSIC_4K_BLOCK_NUMBER_PER_EXTENDED_SECTOR)

/* Return the block trailer of a sector */
#define P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(nSector) (P_MIFARE_CLASSIC_GET_NUMBER_OF_BLOCK( nSector ) - 1)

/* Return the block number */
#define P_MIFARE_CLASSIC_GET_BLOCK(nSector, nBlock) ( (nSector < 32) ? (nSector * P_MIFARE_CLASSIC_BLOCK_NUMBER_PER_SECTOR) + nBlock \
                                                                     : (128 + (nSector * P_MIFARE_CLASSIC_4K_BLOCK_NUMBER_PER_EXTENDED_SECTOR) + nBlock))

#define P_MIFARE_CLASSIC_ACCESS_READ_KEY_A            0x01
#define P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_A           0x02
#define P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE      0x04
#define P_MIFARE_CLASSIC_ACCESS_WRITE_ACCESS_BYTE     0x08
#define P_MIFARE_CLASSIC_ACCESS_READ_KEY_B            0x10
#define P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_B           0x20
#define P_MIFARE_CLASSIC_ACCESS_READ                  0x40
#define P_MIFARE_CLASSIC_ACCESS_WRITE                 0x80

/**
 * @brief   Create the connection at 14443-3 A level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nProtocol  The connection property.
 **/
void PMifareCreateConnection3A(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/** See tPReaderUserRemoveSecondaryConnection */
void PMifareRemoveConnection3A(
            tContext* pContext,
            W_HANDLE hUserConnection );

/**
 * @brief   Create the connection at 14443-4 A level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nProtocol  The connection property.
 **/
void PMifareCreateConnection4A(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty );

/**
 * @brief   Checks if a mifare card can be formatted as a Type 2 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pnMaximumTagSize  The maximum tag size.
 *
 * @param[in]  pnSectorSize  The sector size.
 *
 * @param[in]  pbIsLocked  The card is locked or not.
 *
 * @param[in]  pbIsLockable  The card is lockable or not.
 *
 * @param[in] pbIsFormattable The card is formattable or not
 **/
W_ERROR PMifareCheckType2(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumTagSize,
            uint8_t* pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable);


/**
 * @brief   Updates the Mifare data when a NDEF Type 2 tag has been locked.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 **/
W_ERROR PMifareNDEF2Lock(tContext * pContext,
                         W_HANDLE hConnection);


/**
 * @brief   Initializes the access rights of a Mifare UL-C according to the CC lock status.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  bLocked  Is the card locked (according to CC)
 *
 * @param[in]  pCallback  The callback
 *
 * @param[in]  pCallback  The callback parameter
 **/

void PMifareULInitializeAccessRightsAccordingToType2TagCC(
      tContext * pContext,
      W_HANDLE hConnection,
      bool_t bLocked,
      tPBasicGenericCallbackFunction *pCallback,
      void *pCallbackParameter);

/* ------------------- Mifare Classic ---------------------*/

typedef struct __tPMifareClassicAccessConditions{
   uint8_t nSectorTrailer;
   uint8_t aBlock[15];
   uint8_t nNumberOfBlock;
}tPMifareClassicAccessConditions;


/**
 * @brief   Analyze access condition contained in a Sector Trailer
 *
 * @param[in]  nSectorNumber        The sector authenticated
 * @param[in]  bUseKeyA             true if the key was used to authenticate
 * @param[in]  pAccessCondition     a byte array which contains the 3 byte used as access conditions
 * @param[in]  nAccessConditionLength The length of the pAccessCondition (must be 3)
 * @param[out] pMifareClassicAccess The structure where access conditions will be stored
 *
 * @return W_ERROR_TAG_DATA_INTEGRITY  if conditions access are invalid
 * @return W_SUCCESS                   if no error occured
 **/
W_ERROR PMifareClassicGetAccessConditions(
      uint8_t  nSectorNumber,
      bool_t   bUseKeyA,
      const uint8_t * pAccessCondition,
      uint32_t nAccessContionLength,
      tPMifareClassicAccessConditions * pMifareClassicAccess);


/**
 * @brief   Checks if a mifare card can be formatted as a Type 2 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pnMaximumTagSize  The maximum tag size.
 *
 * @param[in]  pbIsLocked  The card is locked or not.
 *
 * @param[in]  pbIsLockable  The card is lockable or not.
 *
 * @param[in] pbIsFormattable The card is formattable or not
 **/
W_ERROR PMifareClassicCheckType7(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumTagSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_MIFARE_H */
