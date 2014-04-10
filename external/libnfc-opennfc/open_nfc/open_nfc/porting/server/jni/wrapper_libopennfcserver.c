/*
 * Copyright (c) 2010 Inside Secure, All Rights Reserved.
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

#include <android/log.h>
#include <hardware/hardware.h>
#include <hardware/nfcc.h>


#include "org_opennfc_service_Interface.h"

#include "open_nfc.h"
/* #include "porting_driver.h" */
#include "porting_startup.h"

static const char tag[] = "NFCService";

#define LogD(format, ...)  	__android_log_print(ANDROID_LOG_DEBUG, tag, format, ##__VA_ARGS__)
#define LogE(format, ...)		__android_log_print(ANDROID_LOG_ERROR, tag, format, ##__VA_ARGS__)


typedef struct tNFCServiceInstance {

	nfcc_device_t 			 * pNFCController;
	tNALBinding  			 * pNALBinding;
	void 		 		     * pContext;
	char 			         * pConfig;

} tNFCServiceInstance;


static tNFCServiceInstance g_Instance;

/*
 * Class:     org__opennfc_service_Interface
 * Method:    OpenNFCServerStart
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_opennfc_service_Interface_OpenNFCServerStart
  (JNIEnv * env, jclass class, jbyteArray config)
{
	nfcc_module_t * module;
	int ret = -1;
	jsize  size;
	int nVariant = 0;
	int nOffset = 0;

	LogD("Java_org_opennfc_service_Interface_OpenNFCServerStart");

	if (config)
	{
		size = (*env)->GetArrayLength(env, config);
	}
	else
	{
		size = 0;
	}


	g_Instance.pConfig = malloc(size);

	if (g_Instance.pConfig == NULL)
	{
		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart : malloc failed");

		goto return_function;
	}

	(*env)->GetByteArrayRegion(env, config, 0, size, (jbyte *) g_Instance.pConfig);


	if (strncmp(g_Instance.pConfig, "Hardware:", 9 )  == 0)
	{
		LogD("Java_org_opennfc_service_Interface_OpenNFCServerStart : Hardware has been selected");

		nVariant = 0;
		nOffset = 9;
	}
	else if (strncmp(g_Instance.pConfig, "Simulator:", 10 )  == 0)
	{
		LogD("Java_org_opennfc_service_Interface_OpenNFCServerStart : Simulator has been selected");
		nVariant = 1;
		nOffset = 10;
	}
	else
	{
		/* unknown device ... */
		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart : unknown device has been selected");

		goto return_function;
	}


	/*ok, we've retrieved the config, boot the NFC controller... */

	if (hw_get_module(NFCC_HARDWARE_MODULE_ID, (const hw_module_t**) & module) != 0) {

		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart : hw_get_module failed");

		goto return_function;
	}

	if (nfcc_device_open(&module->common, &g_Instance.pNFCController) != 0) {

		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart : nfcc_device_open failed");

		goto return_function;
	}

	if ((g_Instance.pNALBinding = g_Instance.pNFCController->get_binding(g_Instance.pNFCController, (void *) nVariant )) == NULL) {

		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart : get_binding() failed");

		goto return_function;
	}

	g_Instance.pNFCController = StartNFCC(g_Instance.pNALBinding, g_Instance.pConfig + nOffset);

	if (g_Instance.pNFCController == null)
	{
		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart: StartNFCC failed\n");
		return -1;
	}

	if (WaitForNFCCBoot(g_Instance.pNFCController))
	{
		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart: WaitForNFCCBoot failed\n");
		return -1;
	}

	if (StartAcceptingClientConnections(g_Instance.pNFCController))
	{
		LogE("Java_org_opennfc_service_Interface_OpenNFCServerStart: StartAcceptingClientConnections failed\n");
		return -1;
	}

	LogD("Java_org_opennfc_service_Interface_OpenNFCServerStart: Open NFC stack is now running\n");

	ret = 0;

return_function:

	/* @todo : perform cleanup */

	return ret;
}

/*
 * Class:     org__opennfc_service_Interface
 * Method:    OpenNFCServerStop
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_opennfc_service_Interface_OpenNFCServerStop
  (JNIEnv * env, jclass instance)
{

	LogD("Java_org_opennfc_service_Interface_OpenNFCServerStop");

	StopAcceptingClientConnections(g_Instance.pContext);

	CloseClientConnections(g_Instance.pContext);

	StopNFCC(g_Instance.pContext);


	LogD("Java_org_opennfc_service_Interface_OpenNFCServerStop : the open NFC stack has been terminated");
	return 0;
}


/* EOF */
