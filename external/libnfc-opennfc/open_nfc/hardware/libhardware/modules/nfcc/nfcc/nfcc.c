/*
 * Copyright (c) 2009-2010 Inside Secure, All Rights Reserved.
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

#include <hardware/hardware.h>
#include <hardware/nfcc.h>

#include <errno.h>
#include <stdlib.h>
#include <pthread.h>
#include <dlfcn.h>

#include <android/log.h>

#define LogD(format, ...)  	__android_log_print(ANDROID_LOG_DEBUG, tag, format, ##__VA_ARGS__)
#define LogE(format, ...)		__android_log_print(ANDROID_LOG_ERROR, tag, format, ##__VA_ARGS__)

static const char tag[] = "Microread";

extern tNALBinding* g_pNALBinding;
static void * library = NULL;

static int 				static_nfcc_device_open(const struct hw_module_t* module, const char* name, struct hw_device_t** device);
static int 				static_nfcc_device_close(struct hw_device_t* device);
static tNALBinding * static_nfcc_device_get_binding (struct nfcc_device_t * dev, void *);


static struct hw_module_methods_t nfcc_module_methods = {
    open: static_nfcc_device_open
};

struct nfcc_module_t HAL_MODULE_INFO_SYM = {
    common: {
        tag: HARDWARE_MODULE_TAG,
        version_major: 1,
        version_minor: 0,
        id: NFCC_HARDWARE_MODULE_ID,
        name: "NFC Controller module",
        author: "Inside Secure",
        methods: &nfcc_module_methods,
    }
};



/*****************************************************************************/

static int static_nfcc_device_open(const struct hw_module_t* module, const char* name, struct hw_device_t** device)
{
    int status = -EINVAL;

	 LogD("static_nfcc_device_open");

    if (!strcmp(name, NFCC_HARDWARE_DEVICE_ID)) {

		struct nfcc_device_t *dev;
		dev = (nfcc_device_t*)malloc(sizeof(*dev));

		/* initialize our state here */
		memset(dev, 0, sizeof(*dev));

		/* initialize the procs */
		dev->common.tag = HARDWARE_DEVICE_TAG;
		dev->common.version = 0;
		dev->common.module = (hw_module_t*) module;
		dev->common.close = static_nfcc_device_close;

		dev->get_binding = static_nfcc_device_get_binding;

		*device = &dev->common;
		status = 0;
	 }

    return status;
}

/*****************************************************************************/

static int static_nfcc_device_close(struct hw_device_t * device)
{
	if (library != NULL)
	{
		dlclose(library);
		library = NULL;
	}

	return 0;
}

/*****************************************************************************/

typedef tNALBinding * (* tGetNALBinding) (void);

tGetNALBinding GetNALBinding;


static tNALBinding * static_nfcc_device_get_binding (struct nfcc_device_t * dev, void * arg)
{
	LogD("static_nfcc_device_get_binding");

	int nVariant = (int) arg;

	if (nVariant == 0)
	{
		//library = dlopen("/system/lib/libnfc_hal_microread.so", RTLD_NOW);
		library = dlopen("/system/lib/libnfc_hal_msr3110.so", RTLD_NOW);

	}
	else
	{
		library = dlopen("/system/lib/nfc_hal_simulator.so", RTLD_NOW);
	}

	if (library == NULL)
	{
		LogE("static_nfcc_device_open : unable to load /system/lib/libnfc_hal_microread.so");
		LogE("%s",dlerror());
		return NULL;
	}


	GetNALBinding = dlsym(library, "GetNALBinding");

	if (GetNALBinding == NULL)
	{
		LogE("static_nfcc_device_open : unable to get GetNALBinding() function");
		LogE("%s",dlerror());

		return NULL;
	}

	return GetNALBinding();
}





