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

/*******************************************************************************

  This header file should be included only by the test implementation.

*******************************************************************************/

#ifndef __WME_TEST_STUB_H
#define __WME_TEST_STUB_H

#ifndef __OPEN_NFC_H
#define P_IN_TEST_IMPLEMENTATION

#ifndef __GNUC__
#error this code is designed to be compiled with GCC
#endif /* __GNUC__ */

typedef signed char int8_t;
typedef unsigned char uint8_t;
typedef signed short int16_t;
typedef unsigned short uint16_t;
typedef signed long int32_t;
typedef unsigned long uint32_t;

typedef uint16_t char16_t;

#ifdef x86_64
typedef unsigned long long uintptr_t;
#else
typedef unsigned long uintptr_t;
#endif

typedef uint8_t bool_t;
#define  W_FALSE ((bool_t)0)
#define  W_TRUE ((bool_t)1)

#ifndef null
#define null ((void*)0)
#endif

/* Backward compatible types */

#define tchar char16_t
#define sint8_t   int8_t
#define sint16_t int16_t
#define sint32_t int32_t

#ifndef bool
#define  bool bool_t
#define  false W_FALSE
#define  true W_TRUE
#endif


/* List of processor types used for the test bundles
 *    - 0x01 for ix86 processor family
 *    - 0x02 for any processor compatible with ARM7 Thumb little-endian
 *    - 0x03 for any processor compatible with ARM7 ARM little-endian
 *    - 0x04 for any processor compatible with MIPS32 little-endian
 *    - 0x05 for x86 64-bit processor family
 */
#ifdef __i386
#  define P_TEST_PROCESSOR       0x01
#endif
#ifdef __x86_64
#  define P_TEST_PROCESSOR       0x05
#endif
#ifdef __arm__
#  ifdef __thumb__
#     define P_TEST_PROCESSOR       0x02
#  else
#     define P_TEST_PROCESSOR       0x03
#  endif
#endif
#ifdef __mips__
#  define P_TEST_PROCESSOR       0x04
#endif
#ifdef x86_64
#  define P_TEST_PROCESSOR       0x05
#endif


   /* Utility macro to be used in tests bundle to retrieve the address of
      a const data (const variable, callback function) instead of
      WTestConstAddress() to allow proper checking of argument type.
   */

#define WCONSTADDRESS(expected_type, const_address)                  \
   (                                                                 \
      {                                                              \
         expected_type ptr = const_address; ptr = ptr;               \
        (expected_type) pAPI->WTestGetConstAddress(const_address);   \
      }                                                              \
   )

   /* Utility macro to be used in tests bundle to retrieved the address
      of a char * object (strings) */

#define WTEXT(const_address)        WCONSTADDRESS(char *, (const_address))

#define WUINT8(const_address)        WCONSTADDRESS(uint8_t *, (const_address))

#include "open_nfc.h"

#endif /* __OPEN_NFC_H */

/**
 * Notifies the test engine of the end of the test execution.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestNotifyEnd(void);

/* Test result code: test passed */
#define P_TEST_RESULT_PASSED                 0

/* Test result code: test failed */
#define P_TEST_RESULT_FAILED                 1

/* Test result code: test cancelled */
#define P_TEST_RESULT_CANCELLED              2

/* Test result code: test not relevant */
#define P_TEST_RESULT_NOT_RELEVANT           3

/* Test result code: error in the test implementation detected by the test itself. */
#define P_TEST_RESULT_ERROR_IN_TEST          4

/* Test result code: error in external interaction */
#define P_TEST_RESULT_ERROR_INTERACTION      5

/* Test result code: error during the initalization of the test */
#define P_TEST_RESULT_ERROR_INITIALIZATION   6

/* Test result code: error in the test implementation detected by the test infrastructure. */
#define P_TEST_RESULT_ERROR_TEST_IMPLEMENTATION   7

/**
 * Define a test global contex
 *
 **/
typedef struct __tTestExecuteContext
{
  uint32_t nInputParameter;
  uint8_t *pInputData;
  uint32_t nInputDataLength;
  void *pApplicationData;
}tTestExecuteContext;


/**
 * Gets the execution context for the current test.
 *
 * @return  The execution context.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
tTestExecuteContext* WTestGetExecuteContext(void);

/**
 * Sets the result of the test.
 *
 * This function should be called once during the execution of the test to set
 * the result.
 *
 * @param[in]  nResult  One of the result code value.
 *
 * @param[in]  pResultData  A buffer with the result data if any.
 *             Set to null if there is no result data.
 *
 * @param[in]  nResultDataLength  The length in bytes of the result data.
 *             Set to zero if there is no result data.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestSetResult(
         uint32_t nResult,
         const void* pResultData,
         uint32_t nResultDataLength);

/**
 * Sets the result of the test in error.
 *
 * This function should be called once during the execution of the test to set
 * the error code and an explanation.
 *
 * @param[in]  nResult  One of the result code value in error.
 *
 * @param[in]  pMessage  A zero ended string with some explanation on the error.
 *             The text is encoded in ASCII, with characters in the range [0x20, 0x1E] or 0x0A.
 *             The 0x0A character should be interpreted as an end of line.
 *             Set to null if there is no message.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestSetErrorResult(
         uint32_t nResult,
         const char* pMessage);

/**
 * Returns the address of a constant data.
 *
 * This function should be used to retrieve the address of block of constant data.
 *
 * @param[in]  pConstData  The supposed address of the constant data.
 *
 * @return  The true address of the constant data.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
const void* WTestGetConstAddress(
         const void* pConstData );

/**
 * Checks if the test is executed in automatic mode.
 *
 * @return  true if the test is executed in automatic mode, false otherwise.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
bool_t WTestIsInAutomaticMode(void);

/**
 * Prints a trace on the debug output if any.
 *
 * @param[in]  pMessage  The message formated as a printf string.
 *
 * @param[in]  ...  The arguments
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestTraceInfo(const char* pMessage, ...);

/**
 * Prints a warning on the debug output if any.
 *
 * @param[in]  pMessage  The message formated as a printf string.
 *
 * @param[in]  ...  The arguments
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestTraceWarning(const char* pMessage, ...);

/**
 * Prints an error on the debug output if any.
 *
 * @param[in]  pMessage  The message formated as a printf string.
 *
 * @param[in]  ...  The arguments
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestTraceError(const char* pMessage, ...);

/**
 * Prints a buffer on the debug output.
 *
 * @param[in]  pBuffer  The buffer to print.
 *
 * @param[in]  nLength  The length of the buffer to print.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestTraceBuffer(
         const uint8_t* pBuffer,
         uint32_t nLength );

/**
 * Message box icon: The message box contains no icon.
 **/
#define  P_TEST_MSG_BOX_ICON_NONE            0x00000000

/**
 * Message box icon: The message box contains a symbol consisting of
 * a lowercase letter i in a circle.
 **/
#define  P_TEST_MSG_BOX_ICON_INFORMATION     0x00000001

/**
 * Message box icon: The message box contains a symbol consisting of
 * an exclamation point in a triangle with a yellow background.
 **/
#define  P_TEST_MSG_BOX_ICON_WARNING         0x00000002

/**
 * Message box icon: The message box contains a symbol consisting of
 * a white X in a circle with a red background.
 **/
#define  P_TEST_MSG_BOX_ICON_ERROR           0x00000003

/**
 * Message box icon: The message box contains a symbol consisting of
 * a question mark in a circle.
 **/
#define  P_TEST_MSG_BOX_ICON_QUESTION        0x00000004

/** Message box button type: No button only wait **/
#define  P_TEST_MSG_BOX_WAIT                       0x00001000

/** Message box button type: Retry and Cancel buttons. **/
#define  P_TEST_MSG_BOX_BUTTON_RETRY_CANCEL           0x00001200

/** Message box button type: Abort, Retry, and Ignore buttons. **/
#define  P_TEST_MSG_BOX_BUTTON_ABORT_RETRY_IGNORE     0x00002300

/** Message box button type: Yes, No, and Cancel buttons. **/
#define  P_TEST_MSG_BOX_BUTTON_YES_NO_CANCEL          0x00003300

/** Message box button type: OK button. **/
#define  P_TEST_MSG_BOX_BUTTON_OK                     0x00004100

/** Message box button type: OK and Cancel buttons. **/
#define  P_TEST_MSG_BOX_BUTTON_OK_CANCEL              0x00005200

/** Message box button type: Yes and No buttons. **/
#define  P_TEST_MSG_BOX_BUTTON_YES_NO                 0x00006200

/** Message box default button: First button. **/
#define  P_TEST_MSG_BOX_DEFAULT_1      0x00000000

/** Message box default button: Second button. **/
#define  P_TEST_MSG_BOX_DEFAULT_2      0x00010000

/** Message box default button: Third button. **/
#define  P_TEST_MSG_BOX_DEFAULT_3      0x00020000

/** Message box result: "OK" or "Yes" button pressed. **/
#define  P_TEST_MSG_BOX_RESULT_OK_YES        0

/** Message box result: "Cancel" or "Abort" button pressed. **/
#define  P_TEST_MSG_BOX_RESULT_CANCEL        1

/** Message box result: "No" button pressed. **/
#define  P_TEST_MSG_BOX_RESULT_NO            2

/** Message box result: "Retry" button pressed. **/
#define  P_TEST_MSG_BOX_RESULT_RETRY         3

/** Message box result: "Ignore" button pressed. **/
#define  P_TEST_MSG_BOX_RESULT_IGNORE        4


/**
 * Type of the function to implement to receive the result of a message box.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to WTestMessageBox().
 *
 * @param[in]  nResult  The code of the button pressed by the operator.
 *
 * @see WTestMessageBox().
 **/
typedef void tWTestMessageBoxCompleted(
            void* pCallbackParameter,
            uint32_t nResult );

/**
 * Displays a message box for the test operator.
 *
 * @param[in]   nFlags  The flag should be a bitwise or combination of:
 *                - An icon type (if any is needed),
 *                - A button type,
 *                - A default button (if any is needed)
 *
 * @param[in]   pMessage  The message displayed in the message box.
 *
 * @param[in]   nAutomaticResult  The result to return if the test is executed
 *              in automatic mode.
 *
 * @param[in]   pCallback  The callback function.
 *
 * @param[in]   pCallbackParameter  A blind parameter given to the callback function.
 *
 * @see tWTestMessageBoxCompleted.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestMessageBox(
            uint32_t nFlags,
            const char* pMessage,
            uint32_t nAutomaticResult,
            tWTestMessageBoxCompleted* pCallback,
            void* pCallbackParameter );

/**
 * Asks the operator or the presentation robot to present an object in front of the
 * NFC Device antenae.
 *
 * The callback function \a pCallback receives one of the following result codes:
 *    - W_SUCCESS in case of success.
 *    - W_ERROR_CANCEL in case of cancellation. The test should return
 *      the error P_TEST_RESULT_CANCELLED.
 *    - Another error code in case of error. The test should return
 *      the error P_TEST_RESULT_ERROR_INTERACTION.
 *
 * @param[in]   pObjectName The object name. This value shall not be null or empty.
 *
 * @param[in]   pOperatorMessage  The message displayed in the message box or in the log
 *              of the test machine. The message should contain the string "<object>".
 *              This string is replaced by the string pObjectName on the display.
 *              Replaced by a default message if null.
 *
 * @param[in]   nDistance  The distance in mm to leave between the object and
 *              the antenae.
 *
 * @param[in]   pCallback  The callback function.
 *
 * @param[in]   pCallbackParameter  A blind parameter given to the callback function.
 *
 * @see tWBasicGenericCallbackFunction.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestPresentObject(
            const char* pObjectName,
            const char* pOperatorMessage,
            uint32_t nDistance,
            tWBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter );

/**
 * Asks the operator or the presentation robot to remove an object from the
 * NFC Device antenae.
 *
 * The callback function \a pCallback receives one of the following result codes:
 *    - W_SUCCESS in case of success.
 *    - W_ERROR_CANCEL in case of cancellation. The test should return
 *      the error P_TEST_RESULT_CANCELLED.
 *    - Another error code in case of error. The test should return
 *      the error P_TEST_RESULT_ERROR_INTERACTION.
 *
 * @param[in]   pOperatorMessage  The message displayed in the message box
 *              or in the log of the test machine. Replaced by a default message if null.
 *
 * @param[in]   bSaveState  A request to save the state of the removed object.
 *              This flag is only meaningful for virtual object presentation.
 *
 * @param[in]   bCheckUnmodifiedState  A request to check if the state of the
 *              removed object is unmodified compared to the state when
 *              the object was presented. This flag is only meaningful
 *              for virtual object presentation.
 *
 * @param[in]   pCallback  The callback function.
 *
 * @param[in]   pCallbackParameter  A blind parameter given to the callback function.
 *
 * @see tWBasicGenericCallbackFunction.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestRemoveObject(
            const char* pOperatorMessage,
            bool_t bSaveState,
            bool_t bCheckUnmodifiedState,
            tWBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter );

/**
 * Causes the Test Server to execute a remote function.
 *
 * Only one remote call may be executed at a given time.
 *
 * If any of the parameter is invalid (unkown function identifier or wrong parameter),
 * the Test Server displays an error message box and the callback function is called
 * with all the parameters set to zero.
 *
 * Example of function identifier:
 *  - "slave://TST-10002" to execute synchronously the test TST-10002
 *  - "slave://TST-10002?sync=no" to execute asynchronously the test TST-10002
 *  - "assembly://ReaderActivation" to execute the special function ReaderActivation()
 *
 * @param[in]   pFunctionIdentifier  The identifier of the function to execute.
 *              This string describes the location of the function and identifier of the function.
 *
 * @param[in]   nParameter  A parameter given to the function.
 *              The meaning of the parameter depends on the function.
 *
 * @param[in]   pParameterBuffer  A pointer on a buffer sent to the function.
 *              The meaning of the buffer content depends on the function.
 *              If the function does not requires a buffer, set this value to null.
 *
 * @param[in]   nParameterBufferLength  The length in bytes of the buffer sent to the function.
 *              If the function does not requires a buffer, set this value to zero.
 *
 * @param[out]  pResultBuffer  A pointer on a buffer receiving the result of the function.
 *              The meaning of the buffer content depends on the function.
 *              If the function does not requires a buffer for the result, set this value to null.
 *
 * @param[in]   nResultBufferLength  The length in bytes of the result buffer.
 *              If the function does not requires a result buffer, set this value to zero.
 *
 * @param[in]   pCallback  The callback function.
 *
 * @param[in]   pCallbackParameter  A blind parameter given to the callback function.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestExecuteRemoteFunction(
            const char* pFunctionIdentifier,
            uint32_t nParameter,
            const uint8_t* pParameterBuffer,
            uint32_t nParameterBufferLength,
            uint8_t* pResultBuffer,
            uint32_t nResultBufferLength,
            tWBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter );

/**
 * @brief Allocates a block of memory.
 *
 * The WTestFree() function allocates a memory block of at least \a nSize bytes.
 * The caller should never assume that bytes of data are usable
 * in lower addresses or in higher addresses than the allocated buffer.
 * Attempting to access such areas of memory may result in unspecified behavior.
 *
 * Memory allocated with WTestAlloc() should be freed with WTestFree().
 *
 * @param[in]  nSize  The size in bytes of the memory to allocate.
 *
 * @return     WTestAlloc() returns a void pointer to the allocated space,
 *             or null if there is insufficient memory available.
 *             To return a pointer to a type other than void, use a type cast on the return value.
 *             The storage space pointed to by the return value is aligned on a boundary of 4 bytes.
 *             If \a nSize is 0, WTestAlloc() allocates a zero-length item in the heap and returns a valid pointer to that item.
 *             Always check the return from WTestAlloc(), even if the amount of memory requested is small.
 *
 * @see WTestFree().
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void* WTestAlloc(
               uint32_t nSize );

/**
 * @brief Frees a block of memory.
 *
 * The free function deallocates a memory block that was previously allocated by
 * a call to WTestAlloc() or V(). The number of freed bytes is
 * equivalent to the number of bytes requested when the block was allocated.
 * If \a pBuffer is null,
 * the pointer is ignored and free immediately returns.
 *
 * @pre Attempting to free an invalid pointer (a pointer to a memory block that was
 * not allocated by WTestAlloc()) may cause unspecified behavior.
 *
 * @post The caller should never try to access the content of a block freed by WTestFree().
 * Attempting to do so may cause unspecified behavior.
 *
 * @param[in]   pBuffer  The pointer on the block to free.
 *
 * @see  WTestAlloc().
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestFree(
               void* pBuffer );

/**
 * @brief Gets the current time in ms.
 *
 * @return  The current time value.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
uint32_t WTestGetCurrentTime( void );

/**
 * @brief Sets the current timer.
 *
 * Only one timer can be pending at a given time.
 *
 * @param[in]  nTimeout  The timeout in ms. Set to zero to cancel the timer.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter given to the callback function.
 *
 * @keyword  P_FUNCTION, TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestSetTimer(
                  uint32_t nTimeout,
                  tWBasicGenericCompletionFunction* pCallback,
                  void* pCallbackParameter );

/**
 * @brief  Copies memory regions.
 *
 * Copies \a nLength bytes of \a pSource to \a pDestination.
 * If the source and destination overlap, the behavior of WTestCopy() is undefined.
 * Use WTestMove() to handle overlapping regions.
 *
 * @pre  The caller must ensure that at least \a nLength bytes are accessibles
 * in the source and in the destiantion region.
 *
 * @param[in]   pDestination  The address of the destiantion region.
 *
 * @param[in]   pSource  The address of the source region.
 *
 * @param[in]  nLength  The size in bytes of the region to copy.
 *             If this value is zero, the function does nothing and returns.
 *
 * @return  The value of \a pDestination.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void* WTestCopy(
               void* pDestination,
               void* pSource,
               uint32_t nLength );

/**
 * @brief  Copies overlapping memory regions.
 *
 * Copies \a nLength bytes of \a pSource to \a pDestination.
 * The source and destination may overlap.
 * Using WTestCopy() to copy non-overlapping regions is more efficient.
 *
 * @pre  The caller must ensure that at least \a nLength bytes are accessibles
 * in the source and in the destiantion region.
 *
 * @param[in]   pDestination  The address of the destiantion region.
 *
 * @param[in]   pSource  The address of the source region.
 *
 * @param[in]  nLength  The size in bytes of the region to copy.
 *             If this value is zero, the function does nothing and returns.
 *
 * @return  The value of \a pDestination.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void* WTestMove(
               void* pDestination,
               void* pSource,
               uint32_t nLength );

/**
 * @brief  Fills a buffer with a byte value.
 *
 * @pre  The caller must ensure that at least \a nLength bytes are accessibles
 * in the buffer.
 *
 * @param[in]  pBuffer  The address of the buffer to fill.
 *
 * @param[in]  nValue  The byte value used to fill the buffer.
 *
 * @param[in]  nLength  The length in bytes of the buffer to fill.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
void WTestFill(
               void* pBuffer,
               uint8_t nValue,
               uint32_t nLength );

/**
 * @brief  Compares the content of two buffers.
 *
 * The comparison is performed byte by byte from the first byte of the buffer to
 * the last byte.
 *
 * if \a nLength is zero, the function returns zero.
 *
 * @param[in]  pBuffer1  The pointer on the first buffer.
 *             The length of the buffer must be of at least \a nLength bytes.
 *
 * @param[in]  pBuffer2  The pointer on the second buffer.
 *             The length of the buffer must be of at least \a nLength bytes.
 *
 * @param[in]  nLength  The length in bytes of the buffers to compare.
 *
 * @return  A value r defined as follows:
 *            -  r < 0 if \a pBuffer1 is less than \a pBuffer2
 *            -  r = 0 if \a pBuffer1 is identical to \a pBuffer2
 *            -  r > 0 if \a pBuffer1 is greater than \a pBuffer2
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
int32_t WTestCompare(
               const void* pBuffer1,
               const void* pBuffer2,
               uint32_t nLength);

/**
 * Converts a Utf-16 buffer (Little Endian) into a Utf-8 buffer.
 *
 * If pDestUtf8 = null, the length in bytes of the Utf-8 string is returned.
 *
 * @param[out] pDestUtf8  The buffer receiving the Utf-8 string.
 *
 * @param[in]  pSourceUtf16  The buffer containing the Utf-16 buffer.
 *
 * @param[in]  nSourceCharLength  The length in character of the Utf-16 buffer.
 *
 * @return  The length in bytes of the Utf-8 string.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
uint32_t WTestConvertUTF16ToUTF8(
                  uint8_t* pDestUtf8,
                  const char16_t* pSourceUtf16,
                  uint32_t nSourceCharLength );

/**
 * Writes the hexadimal string representation of a byte.
 *
 * @param[out] pStringBuffer  The buffer where to write the string.
 *
 * @param[in]  nValue  The value to write.
 *
 * @return The length in characters of the string.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
uint32_t WTestWriteHexaUint8(
               char16_t* pStringBuffer,
               uint8_t nValue);

/**
 * Writes the hexadimal string representation of a 32 bit integer.
 *
 * @param[out] pStringBuffer  The buffer where to write the string.
 *
 * @param[in]  nValue  The value to write.
 *
 * @return The length in characters of the string.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
uint32_t WTestWriteHexaUint32(
               char16_t* pStringBuffer,
               uint32_t nValue);

/**
 * Writes the decimal string representation of a 32 bit integer.
 *
 * @param[out] pStringBuffer  The buffer where to write the string.
 *
 * @param[in]  nValue  The value to write.
 *
 * @return The length in characters of the string.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
uint32_t WTestWriteDecimalUint32(
               char16_t* pStringBuffer,
               uint32_t nValue);

/**
 * @brief  Returns the length of a string.
 *
 * @pre  The string \a pString should be a valid zero-ended string.
 *
 * @param[in]  pString  The zero-ended string to check.
 *
 * @return  The length in characters of the string.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
uint32_t WTestStringLength(
               const char16_t* pString );

/**
 * @brief  Compares two strings.
 *
 * @pre  The strings \a pString1 and \a pString2 should be a valid zero-ended string.
 *
 * @param[in]  pString1  The first zero-ended string to compare.
 *
 * @param[in]  pString2  The second zero-ended string to compare.
 *
 * @return  One of the following values:
 *            - 0 if \a pString1 is equal to \a pString2
 *            - < 0 if \a pString1 is inferior to \a pString2
 *            - > 0 if \a pString1 is superior to \a pString2
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 **/
int32_t WTestStringCompare(
               const char16_t* pString1,
               const char16_t* pString2 );

/**
 * @brief  Copies a string at the end of another string.
 *
 * @param[in]  pBuffer  The buffer receiving the string.
 *
 * @param[in,out] pnPos  The initial zero based position in bytes where to copy the string.
 *               This value is updated with the new position of the zero at the end of the string.
 *
 * @param[in]  pString  The zero-ended string to copy.
 *
 * @return  The pointer on the new position of the zero at the end of the string.
 *
 * @keyword  TEST_VECTOR
 *
 * @ifdef P_INCLUDE_TEST_ENGINE
 */
char16_t* WTestStringCopy(
               char16_t* pBuffer,
               uint32_t* pnPos,
               const char16_t* pString);


#include "wme_test_stub_autogen.h"

/**
 * Type of the function to implement for a test entry point.
 *
 * @param[in]  pAPI  The pointer on the API.
 **/
typedef void tWTestEntryPoint(
         tTestAPI* pAPI );

/* Definition of a test entry */
typedef struct __tTestEntry
{
   uint32_t nIdentifier;
   tWTestEntryPoint* pEntryPoint;
   char aAPIType[100];
   char aFunction[50];
   char aDescription[300];
   char aFunctionReturn[30];
   char aCardName[50];
   bool_t bIsInteractive;

} tTestEntry;

/* Definition of a link table */
typedef struct __tTestLinkTable
{
   uint8_t aInterfaceUUID[P_TEST_INTERFACE_UUID_LENGTH];
   uint32_t nProcessorCode;
   uintptr_t nGlobalVariableSize;
   uintptr_t nDeltaReference;
   uint32_t nTestBuildNumber;
   uint32_t nTestNumber;
#ifdef P_TEST_NUMBER
   tTestEntry aTestList[P_TEST_NUMBER];
#else
   tTestEntry aTestList[1];
#endif

} tTestLinkTable;

#ifdef P_TEST_NUMBER

extern uint32_t global_variable_size;

#define P_TEST_LINK_TABLE \
   static const tTestLinkTable g_sLinkTable = { \
      P_TEST_INTERFACE_UUID, P_TEST_PROCESSOR, \
      (uintptr_t) ((uint8_t *) &global_variable_size - (uint8_t *)0),\
      (uintptr_t) ((uint8_t *) &g_sLinkTable - (uint8_t*) 0), \
      OPEN_NFC_BUILD_NUMBER, P_TEST_NUMBER, {

#define P_TEST_LINK_TABLE_END  }};

#endif /* #ifdef P_TEST_NUMBER */

#endif /* __WME_TEST_STUB_H */
