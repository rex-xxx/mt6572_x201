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

#include "wme_context.h"

/* =========================================
   Key Permutation
   =========================================  */

static const uint8_t table_PC1[] = { 57,49,41,33,25,17,9,
                             1,58,50,42,34,26,18,
                             10,2,59,51,43,35,27,
                             19,11,03,60,52,44,36,
                             63,55,47,39,31,23,15,
                             07,62,54,46,38,30,22,
                             14,06,61,53,45,37,29,
                             21,13,05,28,20,12,04 } ;

/* =========================================
   Compression Permutaion
   ========================================= */

static const uint8_t table_PC2[] = { 14,17,11,24,1,5,
                             03,28,15,06,21,10,
                             23,19,12,04,26,8,
                             16,07,27,20,13,02,
                             41,52,31,37,47,55,
                             30,40,51,45,33,48,
                             44,49,39,56,34,53,
                             46,42,50,36,29,32 };

/* =========================================
   Initial Permutation
   ========================================= */

static const uint8_t table_PI[] = { 58,50,42,34,26,18,10,2,
                             60,52,44,36,28,20,12,4,
                             62,54,46,38,30,22,14,6,
                             64,56,48,40,32,24,16,8,
                             57,49,41,33,25,17, 9,1,
                             59,51,43,35,27,19,11,3,
                             61,53,45,37,29,21,13,5,
                             63,55,47,39,31,23,15,7 } ;

/* =========================================
   Final Permtuation
   ========================================= */

static const uint8_t table_PF[] = { 40,8,48,16,56,24,64,32,
                             39,7,47,15,55,23,63,31,
                             38,6,46,14,54,22,62,30,
                             37,5,45,13,53,21,61,29,
                             36,4,44,12,52,20,60,28,
                             35,3,43,11,51,19,59,27,
                             34,2,42,10,50,18,58,26,
                             33,1,41, 9,49,17,57,25 } ;


/* =========================================
   Expansion Permutation
   ========================================= */

static const uint8_t table_EX[] = { 32, 1, 2, 3, 4, 5,
                              4, 5, 6, 7, 8, 9,
                              8, 9,10,11,12,13,
                             12,13,14,15,16,17,
                             16,17,18,19,20,21,
                             20,21,22,23,24,25,
                             24,25,26,27,28,29,
                             28,29,30,31,32, 1  } ;


/* =========================================
   P-Box Permutation
   ========================================= */

static const uint8_t table_P[] = { 16, 7,20,21,
                             29,12,28,17,
                              1,15,23,26,
                              5,18,31,10,
                              2, 8,24,14,
                             32,27, 3, 9,
                             19,13,30, 6,
                             22,11, 4,25 } ;


/*  =========================================
    S-Boxes
    ========================================= */

static const uint8_t table_S_Box[8][64] =
{
   { 14, 4,13, 1, 2,15,11, 8, 3,10, 6,12, 5, 9, 0, 7,
   0,15, 7, 4,14, 2,13, 1,10, 6,12,11, 9, 5, 3, 8,
   4, 1,14, 8,13, 6, 2,11,15,12, 9, 7, 3,10, 5, 0,
   15,12, 8, 2, 4, 9, 1, 7, 5,11, 3,14,10, 0, 6,13 },

   { 15, 1, 8,14, 6,11, 3, 4, 9, 7, 2,13,12, 0, 5,10,
   3,13, 4, 7,15, 2, 8,14,12, 0, 1,10, 6, 9,11, 5,
   0,14, 7,11,10, 4,13, 1, 5, 8,12, 6, 9, 3, 2,15,
   13, 8,10, 1, 3,15, 4, 2,11, 6, 7,12, 0, 5,14, 9 },

   { 10, 0, 9,14, 6, 3,15, 5, 1,13,12, 7,11, 4, 2, 8,
   13, 7, 0, 9, 3, 4, 6,10, 2, 8, 5,14,12,11,15, 1,
   13, 6, 4, 9, 8,15, 3, 0,11, 1, 2,12, 5,10,14, 7,
   1,10,13, 0, 6, 9, 8, 7, 4,15,14, 3,11, 5, 2,12 },

   {  7,13,14, 3, 0, 6, 9,10, 1, 2, 8, 5,11,12, 4,15,
   13, 8,11, 5, 6,15, 0, 3, 4, 7, 2,12, 1,10,14, 9,
   10, 6, 9, 0,12,11, 7,13,15, 1, 3,14, 5, 2, 8, 4,
   3,15, 0, 6,10, 1,13, 8, 9, 4, 5,11,12, 7, 2,14 },

   {  2,12, 4, 1, 7,10,11, 6, 8, 5, 3,15,13, 0,14, 9,
   14,11, 2,12, 4, 7,13, 1, 5, 0,15,10, 3, 9, 8, 6,
   4, 2, 1,11,10,13, 7, 8,15, 9,12, 5, 6, 3, 0,14,
   11, 8,12, 7, 1,14, 2,13, 6,15, 0, 9,10, 4, 5, 3 },

   { 12, 1,10,15, 9, 2, 6, 8, 0,13, 3, 4,14, 7, 5,11,
   10,15, 4, 2, 7,12, 9, 5, 6, 1,13,14, 0,11, 3, 8,
   9,14,15, 5, 2, 8,12, 3, 7, 0, 4,10, 1,13,11, 6,
   4, 3, 2,12, 9, 5,15,10,11,14, 1, 7, 6, 0, 8,13 },

   {  4,11, 2,14,15, 0, 8,13, 3,12, 9, 7, 5,10, 6, 1,
   13, 0,11, 7, 4, 9, 1,10,14, 3, 5,12, 2,15, 8, 6,
   1, 4,11,13,12, 3, 7,14,10,15, 6, 8, 0, 5, 9, 2,
   6,11,13, 8, 1, 4,10, 7, 9, 5, 0,15,14, 2, 3,12 },

   { 13, 2, 8, 4, 6,15,11, 1,10, 9, 3,14, 5, 0,12, 7,
   1,15,13, 8,10, 3, 7, 4,12, 5, 6,11, 0,14, 9, 2,
   7,11, 4, 1, 9,12,14, 2, 0, 6,10,13,15, 3, 5, 8,
   2, 1,14, 7, 4,10, 8,13,15,12, 9, 0, 3, 5, 6,11 } };


static void static_PCrypto3DesPermutationCompressive1(uint8_t * );
static void static_PCrypto3DesKeyExtension (uint8_t *, uint8_t *, uint8_t);
static void static_PCrypto3DesKeyExtensionInverse (uint8_t *, uint8_t *, uint8_t);
static void static_PCrypto3DesCircularShiftLeft(uint8_t * );
static void static_PCrypto3DesCircularShiftRight(uint8_t * );
static void static_PCrypto3DesRoundOfDes( uint8_t *, uint8_t * );

/** Resets buffer content
   *
   * @param[out] buffer The buffer
   *
   * @param[in] length The length
   */
static void static_PCryptoReset(uint8_t * input, uint8_t length)
{
   CMemoryFill(input, 0, length);
}

/** Converts a byte array into its binary representation
  *
  * @param[in]   input  The buffer to be converted
  *
  * @param[out]  output The buffer that will contain the binary representation of the input buffer
  *
  * @param[in]   length The input buffer length
  */
static void static_PCryptoHexToBin ( const uint8_t * input, uint8_t *output, uint8_t length )
{
   uint8_t i,j;

   for (i=0; i<length*8; i++)
   {
      j=i/8;
      output[i]=(input[j] & (1 << (7-(i%8)))) >> (7-(i%8));
   }
}

/** Converts a binary representation of a byte array into byte array
  *
  * @param[in]   input  The buffer to be converted
  *
  * @param[out]  output The buffer that will contain the representation of the input buffer
  *
  * @param[in]   length The input buffer length
  */
static void static_PCryptoBinToHex ( uint8_t *input, uint8_t *output, uint8_t length )
{
   uint8_t i;

   for (i=0; i<8*length; i++)
   {
      output[i/8] |= (input[i] << (7-(i%8)));
   }
}


/* See header file */
void PCryptoDesCipher(
   const uint8_t * input,
   const uint8_t * key,
   uint8_t * output)
{
   uint8_t i,temp[64], key_ext[64], key_K_shift[64], mess_bin[64];

   static_PCryptoReset(key_K_shift,64);
   static_PCryptoReset(output,8);

   static_PCryptoHexToBin ( key, key_K_shift, 8 );
   static_PCrypto3DesPermutationCompressive1(key_K_shift);
   static_PCryptoHexToBin ( input, mess_bin, 8 );

   /* Apply initial permutation */
   for (i=0; i<64; i++)
   {
      temp[i]=mess_bin[table_PI[i]-1];
   }

   for (i=0; i<64; i++)
   {
      mess_bin[i]=temp[i];
   }

   /* Perform the 16 rounds */
   for (i=1; i<17; i++)
   {
      /* compute subbkey keyf Ki */
      static_PCrypto3DesKeyExtension(key_K_shift, key_ext, i);
      static_PCrypto3DesRoundOfDes(mess_bin, key_ext);
   }

   /* apply final permutation on R16-L16 */
   for (i=0; i<32; i++)
   {
      temp[i]=mess_bin[i+32];
      temp[i+32]=mess_bin[i];
   }

   for (i=0; i<64; i++)
   {
      mess_bin[i]=temp[table_PF[i]-1];
   }

   /* Save the output */
   static_PCryptoBinToHex ( mess_bin, output, 8 );
}

/* See header file */
void PCryptoDesDecipher(
   const uint8_t * input,
   const uint8_t * key,
   uint8_t * output)
{
   uint8_t i,temp[64], key_ext[64], key_K_shift[64], mess_bin[64];
   static_PCryptoReset(key_K_shift,64);
   static_PCryptoReset(output,8);

   static_PCryptoHexToBin ( key, key_K_shift, 8 );
   static_PCrypto3DesPermutationCompressive1(key_K_shift);
   static_PCryptoHexToBin ( input, mess_bin, 8 );

   /* Apply initial permutation */
   for (i=0; i<64; i++)
   {
      temp[i]=mess_bin[table_PI[i]-1];
   }

   for (i=0; i<64; i++)
   {
      mess_bin[i]=temp[i];
   }

   /* Perform the 16 rounds */
   for (i=1; i<17; i++)
   {
      static_PCrypto3DesKeyExtensionInverse(key_K_shift, key_ext, i);
      static_PCrypto3DesRoundOfDes(mess_bin, key_ext);
   }

   /* apply final permutation on R16-L16 */
   for (i=0; i<32; i++)
   {
      temp[i] = mess_bin[i+32];
      temp[i+32] = mess_bin[i];
   }

   for (i=0; i<64; i++)
   {
      mess_bin[i] = temp[table_PF[i]-1];
   }

   /* Save the output */
   static_PCryptoBinToHex ( mess_bin, output, 8 );
}

/* See header file */
void PCrypto3DesCipher(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * output)
{
   uint8_t res[8];

   PCryptoDesCipher(input, key1, output);
   PCryptoDesDecipher(output, key2, res);
   PCryptoDesCipher(res, key3, output);
}

/* See header file */
void PCrypto3DesDecipher(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t * output)
{
   uint8_t res[8];

   PCryptoDesDecipher(input, key3, output);
   PCryptoDesCipher(output, key2, res);
   PCryptoDesDecipher(res, key1, output);
}

/* See header file */
void PCrypto3DesCipherCbc(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * ivec,
   uint8_t       * output)
{
   uint8_t i;
   uint8_t  aTemp[8];

   /* first, perform XOR between the IVEC and the plain text */
   for (i=0; i<8; i++)
   {
      aTemp[i] = input[i] ^ ivec[i];
   }

   /* Perform the 3DES ciphering */
   PCrypto3DesCipher(aTemp, key1, key2, key3, output);

   /* Update the IVEC */
   CMemoryCopy(ivec, output, 8);
}

/* See header file */
void PCrypto3DesDecipherCbc(
   const uint8_t * input,
   const uint8_t * key1,
   const uint8_t * key2,
   const uint8_t * key3,
   uint8_t       * ivec,
   uint8_t       * output)
{
   uint8_t i;
   uint8_t aTemp[8];

   /* Perform the 3DES deciphering */
   PCrypto3DesDecipher(input, key1, key2, key3, aTemp);

   /* Perform XOR between the IVEC and the output */
   for (i=0; i<8; i++)
   {
      output[i] = aTemp[i] ^ ivec[i];
   }

  /* Update the IVEC */
   CMemoryCopy(ivec, input, 8);
}

/** Performs a DES round */
static void static_PCrypto3DesRoundOfDes(
   uint8_t * mess_bin,
   uint8_t * key_ext)
{
   uint8_t i,j, save_Ri[32], lig, col, temp1[48], temp2[48];

   for (j=32; j<64; j++)
   {
      save_Ri[j-32] = mess_bin[j];
   }

   for (i=0; i<48; i++)
   {
      temp1[i] = mess_bin[31+table_EX[i]];
   }

   /*  XOR with subkey key_ext */
   for (i=0; i<48; i++)
   {
      temp1[i] ^= key_ext[i];
   }

   /* Walk through the S_box */
   for (i=0; i<8; i++)
   {
      lig = 2*temp1[i*6] + temp1[i*6+5];
      col = 8*temp1[i*6+1] + 4*temp1[i*6+2] + 2*temp1[i*6+3] + temp1[i*6+4];
      temp2[i] = table_S_Box[i][16*lig+col];   /*   2^7 > value => 0 on 6 bits */
   }

   for (i=0; i<8; i++)
   {
      temp1[4*i]   = (temp2[i] >> 3) & 1;
      temp1[4*i+1] = (temp2[i] >> 2) & 1;
      temp1[4*i+2] = (temp2[i] >> 1) & 1;
      temp1[4*i+3] = temp2[i] & 1;
   }

   /* Apply P permutation */
   for (i=0; i<32; i++)
   {
      temp2[i] = temp1[table_P[i]-1];
   }

   /* XOR with mess_bin[0..31]= L[i-1] and L[i] = R[i-1] */
   for (i=0; i<32; i++)
   {
      mess_bin[i+32]= temp2[i] ^ mess_bin[i];
      mess_bin[i]=save_Ri[i];
   }
}

/** Performs compressive permutation */
static void static_PCrypto3DesPermutationCompressive1(
   uint8_t * vect)
{
   uint8_t i, temp[56];

   for (i=0; i<56; i++)
   {
      temp[i] = vect[table_PC1[i]-1];
   }

   for (i=0; i<56; i++)
   {
      vect[i]=temp[i];
   }
}

/** Performs key extension */
static void static_PCrypto3DesKeyExtension(
   uint8_t * vect,
   uint8_t * subkey,
   uint8_t RoundNumber)
{
   uint8_t i, temp[48];

   switch( RoundNumber )
   {
      case 1 :
      case 2 :
      case 9 :
      case 16:    static_PCrypto3DesCircularShiftLeft(vect); break ;
       default  :  static_PCrypto3DesCircularShiftLeft(vect); static_PCrypto3DesCircularShiftLeft(vect);
   }
   /* Apply compressive permutation 2 : PC-2 */
   for (i=0; i<48; i++)
   {
      temp[i] = vect[table_PC2[i]-1];
   }

   for (i=0; i<48; i++)
   {
      subkey[i]=temp[i];
   }

}

/** Performs inverse key extension */
static void static_PCrypto3DesKeyExtensionInverse(
   uint8_t * vect,
   uint8_t * subkey,
   uint8_t RoundNumber)
{
   uint8_t i, temp[48];

   switch(RoundNumber)
   {
      case 1 :    break;
      case 2 :
      case 9 :
      case 16:    static_PCrypto3DesCircularShiftRight(vect); break ;
      default  :  static_PCrypto3DesCircularShiftRight(vect); static_PCrypto3DesCircularShiftRight(vect);
   }
   /* apply permutation compressive 2: PC-2 */
   for (i=0; i<48; i++)
   {
      temp[i]=vect[table_PC2[i]-1];
   }
   for (i=0; i<48; i++)
   {
      subkey[i]=temp[i];
   }
}


static void static_PCrypto3DesCircularShiftLeft(uint8_t * vect){
   uint8_t j, temp1, temp2;
   temp1=vect[0];temp2=vect[28];
   for (j=0; j<27; j++){
      vect[j]=vect[j+1];
      vect[j+28]=vect[j+29];}
   vect[27]=temp1; vect[55]=temp2;
}

static void static_PCrypto3DesCircularShiftRight(uint8_t * vect){
   uint8_t j, temp1, temp2;
   temp1=vect[27];   temp2=vect[55];
   for (j=0; j<27; j++){
      vect[27-j]=vect[26-j];
      vect[55-j]=vect[54-j];}
   vect[0]=temp1; vect[28]=temp2;
}



