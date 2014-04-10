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

#ifndef __WME_NDEF_ACTIVATE_H
#define __WME_NDEF_ACTIVATE_H

/*******************************************************************************
   Contains the declaration of the NDEF API functions
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
   Generic NDEF functions
*******************************************************************************/

/* Maximum NDEF file size */
#define P_NDEF_MAX_LENGTH                    0x2800 /* Use 10K instead of 32K (0x80FE) */
#define P_NDEF_COMMAND_MAX_LENGTH            0x105  /* (5 + 255 + 1) */
#define P_NDEF_MAX_CC_LENGTH                 256
#define P_NDEF_MAX_STRING_LENGTH             256

/* Generic command types */
#define P_NDEF_COMMAND_READ_CC               0x00
#define P_NDEF_COMMAND_WRITE_CC              0x01
#define P_NDEF_COMMAND_READ_NDEF             0x02
#define P_NDEF_COMMAND_WRITE_NDEF            0x03
#define P_NDEF_COMMAND_WRITE_NDEF_LENGTH     0x04
#define P_NDEF_COMMAND_LOCK_TAG              0x05
#define P_NDEF_COMMAND_MOVE_READ             0x06
#define P_NDEF_COMMAND_MOVE_WRITE            0x07

/* NDEF read or write actions */
#define P_NDEF_ACTION_RESET                  0x00
#define P_NDEF_ACTION_READ                   0x01
#define P_NDEF_ACTION_WRITE                  0x02

/* NFC Type 1 Tag defines */
#define P_NDEF_1_TLV_TERMINATOR              0xFE

/* NFC Type 2 Tag defines */
#define P_NDEF_2_TLV_TERMINATOR              0xFE

/* NFC Type 4 Tag defines */
#define P_NDEF_4_EXTENDED_LENGTH             0x00

#define P_NDEF_4_VERSION_10                     1
#define P_NDEF_4_VERSION_10_NON_STANDARD        2
#define P_NDEF_4_VERSION_20                     3
#define P_NDEF_4_VERSION_20_NON_STANDARD        4
/* NFC Type 6 Tag defines */
#define P_NDEF_6_TLV_TERMINATOR              0xFE


#define W_NDEF_ACTION_BITMASK                0x1F
#define W_NDEF_ACTION_FORMAT_BITMASK         0x03

struct __tNDEFTypeEntry;

typedef struct sNDEFOperation
{
   W_HANDLE                      hConnection;
   W_HANDLE                      hMessage;
   W_HANDLE                      hOperation;
   uint32_t                      nActionMask;
   uint8_t                       nNDEFType;
   uint8_t                       nTagType;
   uint32_t                      nTagSize;
   tDFCCallbackContext           sCallbackContext;

} tNDEFOperation;

/* Declare a NDEF connection structure */
typedef struct __tNDEFConnection
{

   tHandleObjectHeader        sObjectHeader;

   W_HANDLE                   hConnection;                     /* Current connection */
   W_HANDLE                   hCurrentOperation;               /* The current operation */
   W_ERROR                    nError;                          /* Error code */
   tDFCCallbackContext        sCallbackContext;                /* Callback context */

   /* NDEF Connection information */

   bool_t                       bIsLocked;                       /* TAG is locked ? */
   bool_t                       bIsLockable;                     /* TAG is lockable ? */

   uint8_t                    aCCFile[P_NDEF_MAX_CC_LENGTH];   /* CC contents */
   uint32_t                   nCCLength;                       /* CC length */

   uint32_t                   nMaximumSpaceSize;               /* maximum NDEF data size */
   uint32_t                   nNDEFFileLength;                 /* Current length of the NDEF file */
   uint32_t                   nFreeSpaceSize;                  /* Current available space for NDEF data */


   const struct __tNDEFTypeEntry* pTagType;                    /* pointer to the current NDEF type operation descriptor */

   uint8_t                    nByteLength;                     /* For type 1 Type 2 and Type 7, contains the size in bytes of the length item of the NDEF TLV (1 or 3 bytes) */

   uint16_t                   nNDEFId;                         /* For type 1 and Type 2, contains the offset in the TLV area of the NDEF TLV
                                                                  For type 4,            contains the file identifier that contains the NDEF data
                                                                  For type 5 and Type 6, contains the offset of the NDEF area in the TAG
                                                                  For Type 7 it is not used
                                                                   */

   uint32_t                   nOffset;
   uint32_t                   nIndex;
   uint8_t                    nCommandType;
   uint8_t                    nCommandState;


   /* Command buffer */
   uint8_t*                   pSendBuffer;
   /* Response buffer */
   uint8_t*                   pReceivedBuffer;
   /* Response data length in bytes */
   uint32_t                   nReceivedDataLength;


   /* read opeation */

   uint8_t*                   pBuffer;                                        /* used for NDEF storage */

   uint32_t                   nBufferLength;                                  /* Current buffer length */

   uint8_t                    nTNF;                                           /* TNF to found */
   char16_t                      aTypeString[P_NDEF_MAX_STRING_LENGTH];          /* Type string to find */
   char16_t                      aIdentifierString[P_NDEF_MAX_STRING_LENGTH];    /* Identifier string to found */

   bool_t                       bIsTNFFound;         /* when using READ_NDEF_SPLIT, indicates we have found a record with the correct TNF,
                                                      used for UNCHANGED TNF value */
   /* write operation */

   W_HANDLE                   hMessage;                  /* Message handle */
   uint32_t                   nMessageLength;            /* Length of the message to be written */
   uint32_t                   nActionMask;               /* Specific actions to be performed */
   uint32_t                   nUpdatedNDEFFileLength;    /* NDEF file length after the write operation */

   uint8_t *                  pMessageBuffer;            /* buffer that contains the data to be written in the tag */

   /* Hold data of the queued operation which will be executed after the polling completion */
   struct __tNDEFQueuedOperation
   {
      /* Type of operation: Read, Write... */
      uint32_t             nType;
      /* Read data */
      uint8_t nTNF;
      char16_t aTypeString[P_NDEF_MAX_STRING_LENGTH];
      /* Write data */
      uint32_t nActionMask;
      W_HANDLE hMessage;
      uint8_t* pMessageBuffer;
      uint32_t nOffset;
      uint32_t nLength;
      uint32_t nActualLength;
      uint32_t nUpdatedNDEFFileLength;
      /* Format data */
      tNDEFOperation * pNDEFOperation;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hCurrentOperation;
   } sQueuedOperation;

   union
   {
      /* type 1 Tag */
      struct
      {
         uint8_t                    nTagState;
         uint8_t                    nSectorSize;
         uint8_t                    nMagicNumber;
         uint32_t                   nDynamicLockAddress;             /* start address of the dynamic lock area */
         uint16_t                   nDynamicLockSize;                /* size in bytes if the dynamic lock area */
         uint16_t                   nLockedBytesPerBit;
         uint32_t                   nDynamicReservedMemoryAddress;   /* start address of the dynamic reserved area */
         uint16_t                   nDynamicReservedMemorySize;      /* size in bytes of the dynamic reserved area */

         uint32_t                   nCommandWriteState;  /* current state of the write / lock automaton */

         /* current read / write operation */
         uint8_t                    nCurrentOperation;   /* type of the current operation :READ_NDEF, WRITE_NDEF, WRITE_NDEF_LENGTH */
         uint32_t                   nBegin;              /* start address of the block to be read / written */
         uint32_t                   nBytesToProcess;     /* remaining number of bytes to read / write */
         uint32_t                   nBytesProcessed;     /* number of bytes already read / written */
         uint32_t                   nBytesPending;       /* number of bytes processsed by the current operation */
         uint8_t                  * pBuffer;             /* buffer */

         uint32_t                   nPendingWriteOffset;
         uint32_t                   nPendingWriteLength;
      } t1;

      /* type 2 Tag */
      struct
      {
         uint32_t                   nTagSize;                        /* total number of bytes in the tag */
         uint32_t                   nDynamicLockAddress;             /* start address of the dynamic lock area */
         uint16_t                   nDynamicLockBits;                /* number of dynamic lock bits */
         uint16_t                   nDynamicLockSize;                /* size in bytes if the dynamic lock area */
         uint16_t                   nLockedBytesPerBit;
         uint32_t                   nDynamicReservedMemoryAddress;   /* start address of the dynamic reserved area */
         uint16_t                   nDynamicReservedMemorySize;      /* size in bytes of the dynamic reserved area */

         uint32_t                   nCommandWriteState;  /* current state of the lock automaton */

         /* current read / write operation */
         uint8_t                    nCurrentOperation;   /* type of the current operation :READ_NDEF, WRITE_NDEF, WRITE_NDEF_LENGTH */
         uint32_t                   nBegin;              /* start address of the block to be read / written */
         uint32_t                   nBytesToProcess;     /* remaining number of bytes to read / write */
         uint32_t                   nBytesProcessed;     /* number of bytes already read / written */
         uint32_t                   nBytesPending;       /* number of bytes processsed by the current operation */
         uint8_t                  * pBuffer;             /* buffer */

         uint32_t                   nPendingWriteOffset;
         uint32_t                   nPendingWriteLength;
      } t2;

      /* type 3 Tag */
      struct
      {
         uint8_t                    nTagState;
         uint8_t                    nSectorSize;
         uint8_t                    nVersion;
         uint8_t                    nNumberofBlockforCheckCommand;
         uint8_t                    nNumberofBlockforUpdateCommand;
         uint16_t                   nMaximumNumberofBlock;
         uint16_t                   nNumberofBlockContainingNDEFMsg;
         uint8_t                    nWritingState;
         uint8_t                    nAccessAttribute;
         uint32_t                   nNDEFActualSize;
         uint16_t                   nChecksum;
         bool_t                       bIsChecksumCorrect;

         /* current read / write operation */
         uint8_t                    nCurrentOperation;   /* type of the current operation :READ_NDEF, WRITE_NDEF  */
         uint8_t                  * pBlockElement;
         uint32_t                   nBegin;              /* start address of the block to be read / written */
         uint32_t                   nBytesToProcess;     /* remaining number of bytes to read / write */
         uint32_t                   nBytesProcessed;     /* number of bytes already read / written */
         uint32_t                   nBytesPending;       /* number of bytes processsed by the current operation */
      } t3;

      /* type 4 Tag */
      struct
      {
         uint8_t                     aNDEFLength[2];                 /* Buffer used to write ndef length */

         uint8_t                     nVariant;
         uint16_t                    nMaxCAPDUSize;                  /* maximum data size in a UPDATE BINARY command */
         uint16_t                    nMaxRAPDUSize;                  /* maximum data size in a READ BINARY command */
         uint8_t                     nReadCCAutomatonState;
         uint32_t                    nBytesToProcess;
         uint32_t                    nBytesProcessed;
         uint32_t                    nBytesPending;
         tPBasicGenericDataCallbackFunction * pCallback;
         void                        * pCallbackParameter;
         uint8_t                     * pBuffer;
         uint32_t                    nOffset;

         uint16_t                     nCurrentFileID;
         uint8_t                    * pCCCache;                        /* Cache management */
         uint32_t                     nCCCacheLength;
         uint8_t                    * pNDEFCache;
         uint32_t                     nNDEFCacheLength;

         W_HANDLE                    h7816Channel;    /* 7816 raw channel used to exchange APDU */
         tPBasicGenericDataCallbackFunction * pChannelCallback;
         void                        * pChannelCallbackParameter;
         
      } t4;

      /* type 5 Tag */
      struct
      {
         uint32_t                   nLen;
      } t5;

      /* type 6 Tag */
      struct
      {
         bool_t                       bICodeFormat;     /* W_TRUE for ICODE format, W_FALSE for INSIDE TYPE 6 format */
         uint8_t                    nSectorSize;      /* Number of sectors */
         uint32_t                   nLockState;       /* current state of the lock automaton */
         uint32_t                   nPendingWriteOffset; /* Used during write when length switch betwen 1 and 3 bytes */
         uint32_t                   nPendingWriteLength; /* Used during write when length switch betwen 1 and 3 bytes */
      } t6;
      /* type 7 Tag */
      struct
      {
         uint8_t                    nNdefStartSectorNumber; /* The first sector where the NDEF Message start */
         uint8_t                    nNdefNumberOfSector;    /* The maximum number of sector reserved for storing a Message NDEF */
         bool_t                     bAuthenticationDone;    /* Authentication indicator */
         bool_t                     bSearchNdefAidCompleted; /* Use to indicate the end of the NDEF Sector container search*/
         uint8_t                    nBlockIndex;
         uint8_t                    nSectorOffset;
         uint8_t                    nState;                 /* state used by automatons */

      } t7;

   } sType;

} tNDEFConnection;

/* Create a connection */
typedef W_ERROR tPNDEFCreateConnection(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection );
/* Read the CC file */
typedef W_ERROR tPNDEFReadCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection );
/* Write the CC file */
typedef W_ERROR tPNDEFWriteCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection );
/* Send a command */
typedef W_ERROR tPNDEFSendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength );


typedef W_ERROR tPNDEFInvalidateCache(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength);

typedef const struct __tNDEFTypeEntry
{
   uint8_t nProperty;
   tPNDEFCreateConnection* pCreateConnection;
   tPNDEFSendCommand*      pSendCommand;
   tPNDEFInvalidateCache*  pInvalidateCache;
} tNDEFTypeEntry;

/**
 * @brief   Creates the connection at NDEF activate level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nNDEFType  The NDEF type.
 **/
void PNDEFCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nNDEFType );

/**
 * @brief   Update buffers size if nLength is greater than pNDEFConnection->nBufferLength
 *
 * @param[in]  pNDEFConnection  The NDEF connection.
 *
 * @param[in]  nLength          The desired buffers size.
 *
 * @return  W_SUCCESS if buffers size is updated, W_ERROR_OUT_OF_RESOURCES if size cannot be updated.
 **/
W_ERROR PNDEFUpdateBufferSize(
            tNDEFConnection* pNDEFConnection,
            uint32_t nLength);


/**
 * @brief   Removes the connection at NDEF activate level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 **/

void PNDEFRemoveConnection(
            tContext* pContext,
            W_HANDLE hConnection);


/**
 * @brief   Checkes the read parameters.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nTNF  The message TNF value.
 *
 * @param[in]  pTypeString  The type string.
 *
 * @return  W_SUCCESS if the parameters are correct, an error otherwise.
 **/
W_ERROR PNDEFCheckReadParameters(
            tContext* pContext,
            uint8_t nTNF,
            const char16_t* pTypeString );

/**
 * @brief   Receives the answer to a generic command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The NDEF connection.
 *
 * @param[in]  pBuffer  The reception buffer provided to the function called.
 *
 * @param[in]  nLength  The actual length in bytes of the data received and stored in the reception buffer.
 *
 * @param[in]  nError  The error code of the operation.
 **/
W_ERROR PNDEFSendCommandCompleted(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint32_t nError );

/**
 * @brief   Sends a NFC/Inside Tag command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The NDEF connection structure.
 *
 * @param[in]  nCommandType  The command type.
 *
 * @param[in]  nOffset  The offset to use for the read or write command.
 *
 * @param[in]  nLength  The length to use for the read or write command.
 **/
void PNDEFSendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t nCommandType,
            uint32_t nOffset,
            uint32_t nLength );

/**
 * @brief   Sends an error to the process which has asked for the NDEF exchange.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The NDEF connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
void PNDEFSendError(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            W_ERROR nError );

/**
 * @brief  Reads the NDEF CC for the type 4 tags
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The NDEF connection.
 *
 * @param[in]  nLength  The length in bytes of the CC.
 *
 * @return  W_TRUE in case of success, W_FALSE otherwise.
 **/
bool_t PNDEFType4GetCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nLength );

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_NDEF_ACTIVATE_H */
