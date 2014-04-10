/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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

#ifndef __WME_ASN1_H
#define __WME_ASN1_H

/** Tag Value : Empty tag */
#define P_ASN1_TAG_EMPTY ((uint8_t)0x00)

/** Tag Value : End of content tag */
#define P_ASN1_TAG_EOC ((uint8_t)0x00)

/** Tag Value : Boolean tag */
#define P_ASN1_TAG_BOOL ((uint8_t)0x01)

/** Tag Value : Integer tag */
#define P_ASN1_TAG_INTEGER ((uint8_t)0x02)

/** Tag Value : BitString tag */
#define P_ASN1_TAG_BIT_STRING ((uint8_t)0x03)

/** Tag Value : Octet string tag */
#define P_ASN1_TAG_OCTET_STRING ((uint8_t)0x04)

/** Tag Value : NULL tag */
#define P_ASN1_TAG_NULL ((uint8_t)0x05)

/** Tag Value : OID tag */
#define P_ASN1_TAG_OID ((uint8_t)0x06)

/** Tag Value : Object Descriptor tag */
#define P_ASN1_TAG_OBJECT_DESCRIPTOR ((uint8_t)0x07)

/** Tag Value : External tag */
#define P_ASN1_TAG_EXTERNAL ((uint8_t)0x08)

/** Tag Value : Real tag */
#define P_ASN1_TAG_REAL ((uint8_t)0x09)

/** Tag Value : Enumerated tag */
#define P_ASN1_TAG_ENUM ((uint8_t)0x0A)

/** Tag Value : Embedded Presentation Data Value tag */
#define P_ASN1_TAG_EMBEDDED_PDV ((uint8_t)0x0B)

/** Tag Value : UTF8STRING tag */
#define P_ASN1_TAG_UTF8_STRING ((uint8_t)0x0C)

/** Tag Value : Sequence tag */
#define P_ASN1_TAG_SEQUENCE ((uint8_t)0x10)

/** Tag Value : Set tag */
#define P_ASN1_TAG_SET ((uint8_t)0x11)

/** Tag Value : Numeric string tag */
#define P_ASN1_TAG_NUMERIC_STRING ((uint8_t)0x12)

/** Tag Value : Printable string (ASCII subset) tag */
#define P_ASN1_TAG_PRINTABLE_STRING  ((uint8_t)0x13)

/** Tag Value : T61/Teletex string tag */
#define P_ASN1_TAG_T61_STRING     ((uint8_t)0x14)

/** Tag Value : Videotex string tag */
#define P_ASN1_TAG_VIDEOTEXT_STRING   ((uint8_t)0x15)

/** Tag Value : IA5/ASCII string tag */
#define P_ASN1_TAG_IA5_STRING     ((uint8_t)0x16)

/** Tag Value : UTC time tag */
#define P_ASN1_TAG_UTC_TIME       ((uint8_t)0x17)

/** Tag Value : Generalized time tag */
#define P_ASN1_TAG_GENERALIZED_TIME  ((uint8_t)0x18)

/** Tag Value : Graphic string tag */
#define P_ASN1_TAG_GRAPHIC_STRING ((uint8_t)0x19)

/** Tag Value : Visible string (ASCII subset) tag */
#define P_ASN1_TAG_VISIBLE_STRING ((uint8_t)0x1A)

/** Tag Value : General string tag */
#define P_ASN1_TAG_GENERAL_STRING ((uint8_t)0x1B)

/** Tag Value : Universal string tag */
#define P_ASN1_TAG_UNIVERSAL_STRING  ((uint8_t)0x1C)

/** Tag Value : Basic Multilingual Plane/Unicode string tag */
#define P_ASN1_TAG_BMP_STRING     ((uint8_t)0x1E)

/* ASN.1 tags (1-byte long) */
#define P_ASN1_TAG(_class, _porc, _number)\
  ((uint8_t)(((_class) & 0xC0)|((_porc) & 0x20)|((_number) & 0x1F)))

#define P_ASN1_TAG_UNIVERSAL ((uint8_t)0x00)
#define P_ASN1_TAG_CONTEXT_SPECIFIC ((uint8_t)0x80)
#define P_ASN1_TAG_PRIMITIVE ((uint8_t)0x00)
#define P_ASN1_TAG_CONSTRUCTED ((uint8_t)0x20)

#define P_ASN1_TAG_UNIVERSAL_INTEGER\
  P_ASN1_TAG(P_ASN1_TAG_UNIVERSAL, P_ASN1_TAG_PRIMITIVE, P_ASN1_TAG_INTEGER)

#define P_ASN1_TAG_UNIVERSAL_OCTET_STRING\
  P_ASN1_TAG(P_ASN1_TAG_UNIVERSAL, P_ASN1_TAG_PRIMITIVE, P_ASN1_TAG_OCTET_STRING)

#define P_ASN1_TAG_UNIVERSAL_SEQUENCE\
  P_ASN1_TAG(P_ASN1_TAG_UNIVERSAL, P_ASN1_TAG_CONSTRUCTED, P_ASN1_TAG_SEQUENCE)

/** ASN1 parser */
typedef struct __tAsn1Parser
{
   const uint8_t* pBuffer; /* The ASN1 buffer */
   uint32_t nBufferLength; /* The buffer length in bytes */
   uint32_t nCursor; /* Cursor on the current TLV */
   const uint8_t* pCurrentValue;
   uint32_t nCurrentLength;
} tAsn1Parser;

/**
 * @brief Initializes a parser.
 *
 * The cursor of the parser is set to the first TLV.
 *
 * @param[in]  pParser  The parser to initialize.
 *
 * @param[in]  pBuffer  The buffer of the TLV.
 *
 * @param[in]  nBufferLength  The length in bytes of the buffer.
 *
 * @return  W_SUCCESS in case of success
 **/
W_ERROR PAsn1InitializeParser(
         tAsn1Parser* pParser,
         uint8_t const* pBuffer,
         uint32_t nBufferLength);

/**
 * @brief Moves the cursor of a parser to the next TLV.
 *
 * @param[inout] pParser  The parser.
 *
 * @return  W_SUCCESS in case of success
 **/
W_ERROR PAsn1MoveToNextTlv(
         tAsn1Parser* pParser);

/**
 * @brief Sets the cursor of a parser to the first TLV.
 *
 * @param[inout] pParser  The parser to rewind.
 **/
void PAsn1RewindParser(
         tAsn1Parser* pParser);

/**
 * @brief Checks if there is a TLV after the current TLV.
 *
 * The cursor is left unchanged.
 *
 * @param[in] pParser  The parser.
 *
 * @return  fasle if there is another TLV after the current TLV.
 **/
bool_t PAsn1CheckEndOfTlv(
         const tAsn1Parser* pParser);

/**
 * @brief Returns the tag value of the current TLV.
 *
 * The cursor is left unchanged.
 *
 * @param[in]  pParser  The parser including the TLV.
 *
 * @return The tag value.
 **/
uint8_t PAsn1GetTagValue(
         const tAsn1Parser* pParser);

/**
 * @brief Returns the length of the current TLV.
 *
 * The cursor is left unchanged.
 *
 * @param[in]  pParser  The parser including the TLV.
 *
 * @return The length value.
 **/
uint32_t PAsn1GetTagLength(
         const tAsn1Parser* pParser);

/**
 * @brief Compares two TLVs.
 *
 * The function compares the T, L and V values of the TLV at the current position in the parsers.
 *
 * The current cursors are not compared and they are left unchanged.
 *
 * @param[in]  pParser1  The first parser.
 *
 * @param[in]  pParser2  The second parser.
 *
 * @return  W_TRUE if the TLVs are identical, W_FALSE otherwise.
 **/
bool_t PAsn1CompareTlv(
         const tAsn1Parser* pParser1,
         const tAsn1Parser* pParser2);

/**
 * @brief Parses the content of the enclosing TLV and look for an included TLV.
 *
 * The cursor of the enclosing TLV is left unchanged.
 *
 * @param[in]   pEnclosingParser  The enclosing parser containing the included TLV.
 *
 * @param[out]  pIncludedParser  The parser initialized with the included TLV.
 *
 * @return  W_SUCCESS in case of success
 **/
W_ERROR PAsn1ParseIncludedTlv(
         const tAsn1Parser* pEnclosingParser,
         tAsn1Parser* pIncludedParser);

/**
 * @brief Parses an integer TLV without checking tag.
 *
 * The cursor is left unchanged.
 *
 * The integers larger than 32 bits long are rejected.
 *
 * @param[in] pParser  The parser encoding the integer.
 *
 * @param[out] pnValue  A pointer on a variable valued with the integer value.
 *
 * @return  W_SUCCESS in case of success
 **/
W_ERROR PAsn1ParseIntegerValue(
         const tAsn1Parser* pParser,
         int32_t* pnValue);

/**
 * @brief Parses an integer TLV.
 *
 * The cursor is left unchanged.
 *
 * The tag must be of type UNIVERSAL_INTEGER. The IMPLICIT case is not accepted.
 * The integers larger than 32 bits long are rejected.
 *
 * @param[in] pParser  The parser encoding the integer.
 *
 * @param[out] pnValue  A pointer on a variable valued with the integer value.
 *
 * @return  W_SUCCESS in case of success
 **/
W_ERROR PAsn1ParseInteger(
         const tAsn1Parser* pParser,
         int32_t* pnValue);

/**
 * @brief Parses an octet string TLV.
 *
 * The cursor is left unchanged.
 *
 * @param[in]  pParser  The parser.
 *
 * @param[out] ppContent  A pointer on a pointer variable valued with the
 *             pointer on the octet string content.
 *
 * @param[out] pnLength  A pointer on a variable valued with the length in bytes
 *             of the octet string.
 *
 * @return  W_SUCCESS in case of success
 **/
W_ERROR PAsn1ParseOctetString(
         const tAsn1Parser* pParser,
         const uint8_t** ppContent,
         uint32_t* pnLength);

/**
 * @brief Compares the current TLV with a buffer.
 *
 * The function checks if the current TLV is encoded by given buffer.
 * The cursor is left unchanged.
 *
 * @param[in] pParser  The TLV to check.
 *
 * @param[in] pBuffer  The buffer
 *
 * @param[in] nBufferLength  The length in bytes of the buffer
 *
 * @return  W_TRUE if the content are identical, W_FALSE otherwise.
 **/
bool_t PAsn1CompareTlvToBuffer(
         const tAsn1Parser* pParser,
         const uint8_t* pBuffer,
         uint32_t nBufferLength);

/**
 * @brief Returns the pointer on the binary content of the current TLV.
 *
 * The cursor is left unchanged.
 *
 * @param[in]  pParser  The parser.
 *
 * @param[out] ppContent  A pointer on a pointer variable valued with the
 *             pointer on the binary content of the TLV.
 *
 * @param[out] pnLength  A pointer on a variable valued with the length in bytes
 *             of the content.
 **/
void PAsn1GetPointerOnBinaryContent(
         const tAsn1Parser* pParser,
         const uint8_t** ppContent,
         uint32_t* pnLength);

/**
 * @brief Returns the raw buffer of the current TLV.
 *
 * The cursor is left unchanged.
 *
 * @param[in]  pParser  The parser.
 *
 * @param[out] ppContent  A pointer on a pointer variable valued with the
 *             pointer on the binary content of the TLV.
 *
 * @param[out] pnLength  A pointer on a variable valued with the length in bytes
 *             of the content.
 **/
void PAsn1GetPointerOnTlvBuffer(
         const tAsn1Parser* pParser,
         const uint8_t** ppContent,
         uint32_t* pnLength);

#endif /* __WME_ASN1_H */
