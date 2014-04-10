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

#ifndef __WME_SECURE_ELEMENT_H
#define __WME_SECURE_ELEMENT_H

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Gets the ATR of the Secure Element.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  hUserConnection  The SE connection.
 *
 * @param[out] pAtrBuffer  A pointer on the buffer receiving the ATR value.
 *
 * @param[in]  nAtrBufferLength  The length in bytes of the ATR buffer.
 *
 * @param[out] pnAtrLength  The length in bytes of the ATR stored in the buffer.
 *
 * @return The result code.
 **/
W_ERROR PSEGetAtr(
            tContext* pContext,
            W_HANDLE hUserConnection,
            uint8_t* pAtrBuffer,
            uint32_t nAtrBufferLength,
            uint32_t* pnAtrLength );



#endif /* P_CONFIG_USER ||P_CONFIG_MONOLITHIC */

#endif /* __WME_SECURE_ELEMENT_H */
