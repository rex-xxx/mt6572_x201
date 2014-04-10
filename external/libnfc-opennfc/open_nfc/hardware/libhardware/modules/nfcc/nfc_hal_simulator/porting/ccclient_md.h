/*
 * Copyright (c) 2009-2011 Inside Secure, All Rights Reserved.
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

#ifndef __CCCLIENT_MD_H
#define __CCCLIENT_MD_H

#ifdef __cplusplus
extern "C" {
#endif

typedef struct __CCCriticalSection
{
   uint8_t aDummy[64];   /* Anything large enough to contain a critical section on any OS */
} CCCriticalSection;

/**
 * Check and set a flag in atomically.
 *
 * @return W_TRUE if the flag was already set, W_FALSE If the flag was not set.
 */
bool_t CCInterlockedEnter(void);

/**
 * Creates the critical section.
 *
 * @param[in]  pCS  The critical section.
 */
void CCCreateCriticalSection(
         CCCriticalSection* pCS);

/**
 * Destroyes the critical section.
 *
 * @param[in]  pCS  The critical section.
 */
void CCDestroyCriticalSection(
         CCCriticalSection* pCS);

/**
 * Enters the critical section.
 *
 * @param[in]  pCS  The critical section.
 */
void CCEnterCriticalSection(
         CCCriticalSection* pCS);

/**
 * Leaves the critical section.
 *
 * @param[in]  pCS  The critical section.
 */
void CCLeaveCriticalSection(
         CCCriticalSection* pCS);

/**
 * Initializes the time.
 */
void CCInitializeTime(void);

/**
 * Gets the absolute raw time.
 *
 * The function append the character 0 at the end of the string.
 *
 * @param[out]  pBuffer  The buffer where is stored the decimal value representing the time.
 *
 * @param[in]  nMaxLength  The maximum length in byte of the buffer.
 */
void CCGetAbsoluteRawTime(
               char* pBuffer,
               uint32_t nMaxLength);

/**
 * Gets the relative time in milliseconds.
 *
 * The origin is the call to CCInitializeTime().
 * The function append the character 0 at the end of the string.
 *
 * @param[out]  pBuffer  The buffer where is stored the decimal value representing the time.
 *
 * @param[in]  nMaxLength  The maximum length in byte of the buffer.
 */
void CCGetRelativeTime(
               char* pBuffer,
               uint32_t nMaxLength);

/**
 * Prints a string on the default output.
 *
 * @param[in]  nTraceLevel  The trace level.
 *
 * @param[in]  pString  The zero ended string to print.
 */
void CCDefaultPrintf(
               uint32_t nTraceLevel,
               const char* pString);
/**
 * Returns the lengths in bytes of a zero ended string.
 *
 * @param[in]  pString  The string.
 *
 * @param[in]  nMaxLength  The string max length in bytes.
 *
 * @return  The length of the string.
 **/
uint32_t CCStrLen(
               const char* pString,
               uint32_t nMaxLength);

/**
 * Concatenates a string at the end of another string.
 *
 * @param[in]  pString  The string.
 *
 * @param[in]  nMaxLength  The string max length in bytes.
 *
 * @param[in]  pString  The string to append.
 **/
void CCStrCat(
               char* pString,
               uint32_t nMaxLength,
               const char* pAppend);

/**
 * Formats a string.
 *
 * @param[in]  pString  The string buffer.
 *
 * @param[in]  nMaxLength  The string max length in bytes.
 *
 * @param[in]  pFormat  The format of the string.
 *
 * @param[in]  list  The list of parameters
 *
 * @return  The length of the formatted string.
 **/
uint32_t CCVSPrintf(
               char* pString,
               uint32_t nMaxLength,
               const char* pFormat,
               va_list list);

typedef struct __CCSocket
{
   uint8_t aDummy[64];   /* Anything large enough to contain a socket and an event on any OS */
} CCSocket;

/**
 * Creates a socket for the connection center.
 *
 * @param[in]  pAddress The CC address, use the local-loop address if null.
 *
 * @param[in]  pSocket  The socket structure.
 *
 * @return  CC_SUCCESS in case of success, an error cod otherwise.
 **/
uint32_t CCSocketCreate(
               const char* pAddress,
               CCSocket* pSocket);

/**
 * Shutdown and close a socket.
 *
 * @param[in]  pSocket  The socket structure.
 **/
void CCSocketShutdownClose(
               CCSocket* pSocket);

/**
 * Receives data.
 *
 * If bWait is W_TRUE, the function blocks until the specified amount of data is received or an error occurs.
 *
 * If bWait is W_FALSE, if no data is available, the function returns 0.
 * If some data is available, the function blocks until the specified amount of data is received or an error occurs.
 *
 * @param[in] pSocket The socket.
 *
 * @param[out] pBuffer  The buffer receiving the data.
 *
 * @param[in) nLength  The lenght in bytes of the buffer.
 *
 * @param[in] bWait  A boolean indicating if the function shall wait for the data.
 *
 * @return  0 no data available (bWait is W_FALSE).
 *          >0 nLength bytes received.
 *          <0 an error occured.
 **/
int32_t CCSocketReceive(
            CCSocket* pSocket,
            uint8_t* pBuffer,
            uint32_t nLength,
            bool_t bWait);

/**
 * Sends data.
 *
 * The function blocks until the specified amount of data is sent or an error occurs.
 *
 * @param[in] pSocket The socket.
 *
 * @param[in] pBuffer  The buffer to send.
 *
 * @param[in) nLength  The lenght in bytes of the buffer.
 *
 * @return  W_TRUE if the data is sent. W_FALSE if an error occured.
 **/
bool_t CCSocketSend(
            CCSocket* pSocket,
            const uint8_t* pBuffer,
            uint32_t nLength);

/**
 * Returns the reception event of the socket.
 *
 * @param[in] pSocket The socket.
 *
 * @return  The reception event of the socket.
 **/
void* CCSocketGetReceptionEvent(
            CCSocket* pSocket);

/**
 * Signals the reception event of the socket.
 *
 * @param[in] pSocket The socket.
 **/
void CCSocketSignalReceptionEvent(
            CCSocket* pSocket);

/**
 * Returns the application name as a string.
 *
 * @param[out]  pBuffer  The application name encoded as a zero-ended unicode string.
 **/
void CCGetApplicationName(
            char16_t* pBuffer);

/**
 * Returns the application identifier (ID) as a string.
 *
 * @param[out]  pBuffer  The application identifier encoded as a zero-ended unicode string.
 **/
void CCGetApplicationIdentifier(
            char16_t* pBuffer);

/**
 * Prints an error message comming from the CC client library.
 *
 * @param[in] pString  The message to print. A zero-ended string without end of line.
 */
void CCPrintError(
            const char* pString);

/**
 * Allocates a memory area of specified length bytes
 *
 * @param[in] nSize  The size of the memory area to allocate
 */
void * CCMalloc(
            uint32_t nSize);

/**
 * Frees a memory area previously allocated by CCMalloc()
 *
 * @param[in] pMemory  The address of the memory area to be freed
 */
void  CCFree(void * pMemory);

/**
 * Copy nLength bytes from memory area pSrc to memory area pDest
 *
 * @param[out] pDest  The address of the memory area to be filled
 *
 * @param[in] pSrc  The address of the memory area to be copied
 *
 * @param[in] nLength  The number of bytes to copy
 */
void CCMemcpy(void * pDest, const void * pSrc, uint32_t nLength);


#ifdef __cplusplus
}
#endif

#endif /* __CCCLIENT_MD_H */
