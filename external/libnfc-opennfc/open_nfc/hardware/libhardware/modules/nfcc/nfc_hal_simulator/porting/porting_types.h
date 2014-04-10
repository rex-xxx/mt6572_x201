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

 /*******************************************************************************

  This header files contains the basic types used by Open NFC.

*******************************************************************************/

#ifndef __PORTING_TYPES_H
#define __PORTING_TYPES_H

#include "porting_types_md.h"

typedef uint16_t char16_t;

typedef uint8_t bool_t;
#define  W_FALSE ((bool_t)0)
#define  W_TRUE ((bool_t)1)

#ifndef null
#ifdef nullptr
#define null nullptr
#else
#define null ((void*)0)
#endif
#endif

#endif /* __PORTING_TYPES_H */
