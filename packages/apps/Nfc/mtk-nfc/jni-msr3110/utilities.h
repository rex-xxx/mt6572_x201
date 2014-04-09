/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
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

#ifndef WRAPPER_HANDLE
#	define WRAPPER_HANDLE

#	include <semaphore.h>
#	include <android/log.h>
#	include <open_nfc.h>

#	define	SERVER_SOCKET_HEADER		0x10000
#	define	CLIENT_SOCKET_HEADER		0x20000
#	define	CREATE_SERVER_SOCKET(index)	(SERVER_SOCKET_HEADER | index)
#	define	CREATE_CLIENT_SOCKET(index)	(CLIENT_SOCKET_HEADER | index)
#	define	SOCKET_TYPE_MASK			0xFFFF0000
#	define	SOCKET_INDEX_MASK			0xFFFF
#	define	IS_SERVER_SOCKET(handle)	((handle & SOCKET_TYPE_MASK)==SERVER_SOCKET_HEADER)
#	define	IS_CLIENT_SOCKET(handle)	((handle & SOCKET_TYPE_MASK)==CLIENT_SOCKET_HEADER)
#	define	GET_SOCKET_INDEX(handle)	(handle & SOCKET_INDEX_MASK)

#	define DEBUG_MODE

	int semaphore_init(sem_t * semaphore, const char * file, int line);
	int semaphore_wait(sem_t * semaphore, const char * file, int line);
	int semaphore_post(sem_t * semaphore, const char * file, int line);
	int semaphore_destroy(sem_t * semaphore, const char * file, int line);
	char * dumpUnit8Array(uint8_t * array, int length);

#	define LogWarning(format, ...)           __android_log_print(ANDROID_LOG_WARN,    __FILE__, format,  ##__VA_ARGS__)
#	define LogError(format, ...)                 __android_log_print(ANDROID_LOG_ERROR,  __FILE__, format, ##__VA_ARGS__)

#	ifdef DEBUG_MODE
#		define LogDebug(format, ...)              __android_log_print(ANDROID_LOG_DEBUG,  __FILE__, format,  ##__VA_ARGS__)
#		define LogInformation(format, ...)       __android_log_print(ANDROID_LOG_INFO,      __FILE__, format, ##__VA_ARGS__)
#		define SEMAPHORE_CREATE(semaphore)	semaphore_init(semaphore, __FILE__, __LINE__)
#		define SEMAPHORE_WAIT(semaphore)	semaphore_wait(semaphore, __FILE__, __LINE__)
#		define SEMAPHORE_POST(semaphore)	semaphore_post(semaphore, __FILE__, __LINE__)
#		define SEMAPHORE_DESTROY(semaphore)	semaphore_destroy(semaphore, __FILE__, __LINE__)
#	else
#		define LogDebug(format, ...)
#		define LogInformation(format, ...)
#		define SEMAPHORE_CREATE(semaphore)	sem_init(semaphore, 0, 0)
#		define SEMAPHORE_WAIT(semaphore)	sem_wait(semaphore)
#		define SEMAPHORE_POST(semaphore)	sem_post(semaphore)
#		define SEMAPHORE_DESTROY(semaphore)	sem_destroy(semaphore)
#	endif

#	define	LogPosition(information)		LogInformation("LINE %d : %s", __LINE__, information)

#endif
