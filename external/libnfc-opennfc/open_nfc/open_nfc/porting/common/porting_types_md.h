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

#ifndef __PORTING_TYPES_MD_H
#define __PORTING_TYPES_MD_H

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <stdint.h>
#include <wchar.h>

/*typedef unsigned char uint8_t; */   /* conflict <stdint.h> */
/*typedef unsigned short uint16_t; */ /* conflict <stdint.h> */
/*typedef unsigned int uint32_t; */ /* conflict <stdint.h> */

typedef int8_t    int8_t;
typedef int16_t    int16_t;
typedef int32_t    int32_t;

#define INFINITE   (~(0))

#define P_INLINE  __inline

#endif /* __PORTING_TYPES_MD_H */
