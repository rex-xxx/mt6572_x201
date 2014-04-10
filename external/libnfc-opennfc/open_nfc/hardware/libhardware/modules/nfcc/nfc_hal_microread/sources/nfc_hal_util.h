/*
 * Copyright (c) 2007-2011 Inside Secure, All Rights Reserved.
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

/*******************************************************************************
   Contains the declaration of some utility functions
*******************************************************************************/

#ifndef __NFC_HAL_UTIL_H
#define __NFC_HAL_UTIL_H

/**
 * Writes the hexadimal string representation of a byte.
 *
 * @param[out] pStringBuffer  The buffer where to write the string.
 *
 * @param[in]  nValue  The value to write.
 *
 * @return The length in characters of the string.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilWriteHexaUint8(
               uint8_t* pStringBuffer,
               uint8_t nValue);

/**
 * Writes the decimal string representation of a 32 bit integer.
 *
 * @param[out] pStringBuffer  The buffer where to write the string.
 *
 * @param[in]  nValue  The value to write.
 *
 * @return The length in characters of the string.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilWriteDecimalUint32(
               uint8_t* pStringBuffer,
               uint32_t nValue);

/**
 * Writes the string representation of a version.
 *
 * @param[out] pStringBuffer  The buffer where to write the string.
 *
 * @param[in]  pVersion  The pointer on the binary version.
 *
 * @return The length in characters of the string.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilWriteVersion(
               uint8_t* pStringBuffer,
               const uint8_t* pVersion);

/**
 * @brief  Converts a pointer value into a 32-bit integer value.
 *
 * The pointer value should have been computed with PNALUtilConvertUint32ToPointer().
 *
 * @param[in]   pPointer  The pointer value to convert.
 *
 * @return  The 32-bit integer value.
 **/
#define PNALUtilConvertPointerToUint32( pPointer ) \
         ((uint32_t)(((uint8_t*)(pPointer)) - ((uint8_t*)0)))

/**
 * @brief  Converts a 32-bit integer value into a pointer value.
 *
 * @param[in]   nValue  The 32-bit integer value to convert.
 *
 * @return  The pointer value.
 **/
#define PNALUtilConvertUint32ToPointer( nValue ) \
         ((void*)(((uint8_t*)0) + (nValue)))

/**
 * @brief  Reads a value from a Big Endian buffer.
 *
 * @param[in]  pBuffer  The buffer of at least 4 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilReadUint32FromBigEndianBuffer(
         const uint8_t* pBuffer);

/**
 * @brief  Reads an address value from a Big Endian buffer.
 *
 * @param[in]  pBuffer  The buffer of at least size of nValue bytes containing the value.
 *
 * @return  The corresponding value.
 **/
NFC_HAL_INTERNAL uintptr_t PNALUtilReadAddressFromBigEndianBuffer(
         const uint8_t* pBuffer);

/**
 * @brief  Reads a value from a Little Endian buffer.
 *
 * @param[in]  pBuffer  The buffer of at least 4 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilReadUint32FromLittleEndianBuffer(
         const uint8_t* pBuffer);

#ifdef P_NAL_TRACE_ACTIVE

/**
 * Writes the hexadecimal value of a byte.
 *
 * @param[in]  pTraceBuffer  A pointer on the buffer where to write the log.
 *
 * @param[in]  nValue  The byte value.
 *
 * @return The number of characters added to the buffer.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilLogUint8(
         char* pTraceBuffer,
         uint32_t nValue);

/**
 * Writes the hexadecimal value of a uint 16.
 *
 * @param[in]  pTraceBuffer  A pointer on the buffer where to write the log.
 *
 * @param[in]  nValue  The value.
 *
 * @return The number of characters added to the buffer.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilLogUint16(
                  char* pTraceBuffer,
                  uint32_t nValue);

/**
 * Writes the content of a byte array.
 *
 * @param[in]  pTraceBuffer  A pointer on the buffer where to write the log.
 *
 * @param[in]  pBuffer  The pointer on the buffer to dump.
 *
 * @param[in]  nLength  The length in bytes of the buffer to dump.
 *
 * @return The number of characters added to the buffer.
 **/
NFC_HAL_INTERNAL uint32_t PNALUtilLogArray(
         char* pTraceBuffer,
         uint8_t* pBuffer,
         uint32_t nLength);

/**
 * Copies a string at the end of another string.
 *
 * @param[in]  pBuffer  The buffer receiving the string.
 *
 * @param[inout] nPos  The initial zero based position in bytes where to copy the string.
 *               This value is updated with the new position of the end of the string.
 *
 * @param[in]  pString  The zero-ended ASCII string to copy.
 */
NFC_HAL_INTERNAL void PNALUtilTraceASCIIStringCopy(
         char* pBuffer,
         uint32_t* pPos,
         const char* pString);

/**
 * Returns the error string corresponding to an error code.
 *
 * @param[in]  nError  The error code.
 *
 * @return The error string.
 **/
NFC_HAL_INTERNAL const char* PNALUtilTraceErrorWrapper(
         W_ERROR nError);

#define PNALUtilTraceError(X)   PNALUtilTraceErrorWrapper(X)

#else /* #ifdef P_NAL_TRACE_ACTIVE */

/* @note: this is needeed by Microsoft Visual C++ 2008 Express ; even if functions are not used (always called inside PNALDebugTrace() */
#ifndef PNALUtilTraceError
#define PNALUtilTraceError(X)   ""
#endif

#endif /* #ifdef P_NAL_TRACE_ACTIVE */

#endif /* #ifdef __NFC_HAL_UTIL_H */
