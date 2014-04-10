/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_NFCC_INTERFACE_H
#define ANDROID_NFCC_INTERFACE_H

#include <stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#include <hardware/hardware.h>
#include "nfc_hal.h"

__BEGIN_DECLS

/**
 * The id of this module
 */
#define NFCC_HARDWARE_MODULE_ID "nfcc"
#define NFCC_HARDWARE_DEVICE_ID "nfcc"

/*****************************************************************************/

/**
 * Every hardware module must have a data structure named HAL_MODULE_INFO_SYM
 * and the fields of this data structure must begin with hw_module_t
 * followed by module specific information.
 */
typedef struct nfcc_module_t {

	struct hw_module_t common;

} nfcc_module_t;


/*****************************************************************************/

/**
 * Every device data structure must begin with hw_device_t
 * followed by module specific public methods and attributes.
 */

typedef struct nfcc_device_t {

	struct hw_device_t common;

	/**
     * retreive the NFC HAL Binding structure
	  *
	  * return the address of the binding on success,
	  * null on failure
	  */

	tNALBinding * (* get_binding) (struct nfcc_device_t * dev, void * param);

} nfcc_device_t;


/** convenience API for opening and closing a device */

static inline int nfcc_device_open(const struct hw_module_t* module,
        struct nfcc_device_t** device) {

	return module->methods->open(module,
            NFCC_HARDWARE_DEVICE_ID, (struct hw_device_t**)device);
}

static inline int nfcc_device_close(struct nfcc_device_t* device) {

    return device->common.close(&device->common);
}


__END_DECLS

#endif  // ANDROID_NFCC_INTERFACE_H

