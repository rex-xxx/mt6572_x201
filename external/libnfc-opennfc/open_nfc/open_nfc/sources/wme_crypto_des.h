/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

#ifndef __WME_CRYPTO_DES_H
#define __WME_CRYPTO_DES_H

/**
 * Performs DES encryption
 *
 * @param[in]   input  The buffer to be encrypted
 *
 * @param[in]   key  The key used for encryption
 *
 * @param[out]  output  The encryped buffer
 *
 **/
void PCryptoDesCipher(
      const uint8_t * input,
      const uint8_t * key ,
      uint8_t * output);


/**
 * Performs DES decyption
 *
 * @param[in]   input  The buffer to be decrypted
 *
 * @param[in]   key  The key used for decryption
 *
 * @param[out]  output  The decryped buffer
 *
 **/

void PCryptoDesDecipher(
      const uint8_t * input,
      const uint8_t * key,
      uint8_t * output);

/**
 * Performs 3DES encryption
 *
 * @param[in]   input  The buffer to be encrypted
 *
 * @param[in]   key1  The key used for initial encryption
 *
 * @param[in]   key2  The key used for decryption
 *
 * @param[in]   key3  The key used for final encryption
 *
 * @param[out]  output  The encryped buffer
 *
 **/
void PCrypto3DesCipher(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * output);

/**
 * Performs 3DES decryption
 *
 * @param[in]   input  The buffer to be decrypted
 *
 * @param[in]   key1  The key used for initial decryption
 *
 * @param[in]   key2  The key used for encryption
 *
 * @param[in]   key3  The key used for final decryption
 *
 * @param[out]  output  The decryped buffer
 *
 **/
void PCrypto3DesDecipher(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * output);

/**
 * Performs 3DES encryption (CBC mode)
 *
 * @param[in]   input  The buffer to be encrypted
 *
 * @param[in]   key1  The key used for initial encryption
 *
 * @param[in]   key2  The key used for decryption
 *
 * @param[in]   key3  The key used for final encryption
 *
 * @param[in/out]  ivec  The initial vector
 *
 * @param[out]  output  The encryped buffer
 *
 **/
void PCrypto3DesCipherCbc(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * ivec,
   uint8_t       * output);

/**
 * Performs 3DES decryption (CBC mode)
 *
 * @param[in]   input  The buffer to be decrypted
 *
 * @param[in]   key1  The key used for initial decryption
 *
 * @param[in]   key2  The key used for encryption
 *
 * @param[in]   key3  The key used for final decryption
 *
 * @param[in/out]  ivec  The initial vector
 *
 * @param[out]  output  The decryped buffer
 *
 **/
void PCrypto3DesDecipherCbc(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * ivec,
   uint8_t       * output);




#endif
