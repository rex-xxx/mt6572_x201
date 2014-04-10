/**
 Copyright (C) 2010 Inside Contactless

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <android/log.h>

#include <pthread.h>

#include <hardware/hardware.h>
#include <hardware/nfcc.h>
#include "open_nfc.h"
#include "porting_startup.h"
#include "ccclient.h"
#include "linux_porting_hal.h"

#include <arpa/inet.h>
#include <sys/time.h>

/**
 * --------------------------
 * --- Stand alone server ---
 * --------------------------
 */

/**TAG used in logs*/
static const char TAG[] = "OpenNFCServer";

const char DEFAULT_IP[] = "10.0.2.2";

/**Log for debugging message*/
#define LogDebug(format, ...)		__android_log_print(ANDROID_LOG_DEBUG,	TAG, format, ##__VA_ARGS__)
/**Log for warning message*/
#define LogWarning(format, ...)		__android_log_print(ANDROID_LOG_WARN,	TAG, format, ##__VA_ARGS__)
/**Log for information message*/
#define LogInformation(format, ...)	__android_log_print(ANDROID_LOG_INFO,	TAG, format, ##__VA_ARGS__)
/**Log for error message*/
#define LogError(format, ...)		__android_log_print(ANDROID_LOG_ERROR,	TAG, format, ##__VA_ARGS__)

#define	MICROREAD_CHAR	'M'
#define	SIMULATOR_CHAR	'S'

#define	MICROREAD_VARIANT	0
#define	SIMULATOR_VARIANT	1

#define FILE_CONFIGURATION_PATH "/data/connection_center_access"

typedef struct tNFCServiceInstance
{

	nfcc_device_t * pNFCController;
	tNALBinding * pNALBinding;
	void * pContext;
	char * pConfig;

} tNFCServiceInstance;

static tNFCServiceInstance g_Instance;

#define IP_LENGTH 18

typedef struct ConfigurationTarget
{
	char target;
	char configuration[IP_LENGTH];
} ConfigurationTarget;

int serverStart(ConfigurationTarget* configurationTarget)
{
	nfcc_module_t * module;
	int variant;

	switch (configurationTarget->target)
	{
		case MICROREAD_CHAR:
			variant = MICROREAD_VARIANT;
			break;
		case SIMULATOR_CHAR:
			variant = SIMULATOR_VARIANT;
			break;
		default:
			LogError("Invalid target !");
			goto endingWithError;
	}

	g_Instance.pConfig = configurationTarget->configuration;
	if (g_Instance.pConfig == NULL)
	{
		LogError("Null configuration !");
		goto endingWithError;
	}

	LogInformation("serverStart : hw_get_module");
	if (hw_get_module(NFCC_HARDWARE_MODULE_ID, (const hw_module_t**) &module) != 0)
	{
		LogError("hw_get_module failed !");
		goto endingWithError;
	}

	LogInformation("serverStart : nfcc_device_open");
	if (nfcc_device_open(&module->common, &g_Instance.pNFCController) != 0)
	{
		LogError("nfcc_device_open failed !");
		goto endingWithError;
	}

	LogInformation("serverStart : get_binding");
	if ((g_Instance.pNALBinding = g_Instance.pNFCController->get_binding(g_Instance.pNFCController, (void *) variant)) == NULL)
	{
		LogError("get_binding failed !");
		goto endingWithError;
	}

	LogInformation("serverStart : StartNFCC");
	g_Instance.pNFCController = StartNFCC(g_Instance.pNALBinding, g_Instance.pConfig);

	if (g_Instance.pNFCController == null)
	{
		LogError("StartNFCC failed !");
		goto endingWithError;
	}

	LogInformation("serverStart : WaitForNFCCBoot");
	if (WaitForNFCCBoot(g_Instance.pNFCController))
	{
		LogError("WaitForNFCCBoot failed !");
		goto endingWithError;
	}

	LogInformation("serverStart : StartAcceptingClientConnections");
	if (StartAcceptingClientConnections(g_Instance.pNFCController))
	{
		LogError("StartAcceptingClientConnections failed !");
		goto endingWithError;
	}

	LogInformation("Open NFC server started !");

	return 0;

	endingWithError:
	LogInformation("Starting method error");

	return -1;
}

/**Default socket name*/
const char * aDefaultSocketName = "opennfc";

/* NFC HAL and CORE use it, to have nice time display starting at zero */
struct timeval g_timeStart;

tLinuxPortingConfig sPortingConfig;

static void fillConfigurationTarget(ConfigurationTarget * configurationTarget)
{

	FILE* file;

	file = fopen(FILE_CONFIGURATION_PATH, "r");
	if (file != NULL)
	{
		configurationTarget->target = (char) fgetc(file);
		fgetc(file);

		if(fgets(configurationTarget->configuration, IP_LENGTH, file) != NULL)
		{
			LogInformation("configuration: target=%c, ip=%s", configurationTarget->target, configurationTarget->configuration);
		}
		else
		{
			LogWarning("Failed to read settings from the configuration file %s", FILE_CONFIGURATION_PATH);
			configurationTarget->target = (char) 0;
		}
		fclose(file);
	}
	else
	{
		LogWarning("Failed to open configuration file %s", FILE_CONFIGURATION_PATH);
		configurationTarget->target = (char) 0;
	}

	if (configurationTarget->target == 0)
	{
		configurationTarget->target = MICROREAD_CHAR;
		memcpy(configurationTarget->configuration, DEFAULT_IP, sizeof(DEFAULT_IP));
	}
}

/**
 * Launch the server
 * @param	nArgc	Number of arguments
 * @param	pArgv	Arguments give
 */
int main(int nArgc, char* pArgv[])
{
	int resultStart;
	ConfigurationTarget configurationTarget;
	pthread_t threadStartServer;
	int threadStartServerID;

	LogInformation("Starting Open NFC server !");

	resultStart = -1;
	while(resultStart == -1)
	{
		fillConfigurationTarget(&configurationTarget);

		resultStart = serverStart(&configurationTarget);

		sleep(10);
	}

	for (;;)
	{
		sleep(1);
	}

	return 0;
}

/* EOF */
