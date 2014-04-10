/*
 * Copyright (c) 2007-2010 Inside Contactless, All Rights Reserved.
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
 * See the License for the specific language govern permissions and
 * limitations under the License.
 */

#include "nfc_hal.h"

tNALBinding  * GetNALBinding(void);

typedef struct __tLinuxPortingConfig
{
   uint32_t      nInetAddr;               /* IPV4 address of the Connection center */
   char *        pSocketName;            /* Name of the AF_UNIX socket */

} tLinuxPortingConfig;




/* EOF */
