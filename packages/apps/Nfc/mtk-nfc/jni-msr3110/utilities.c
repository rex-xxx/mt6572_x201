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

#include "utilities.h"
#include <errno.h>

int semaphore_init(sem_t * semaphore, const char * file, int line)
{
	int status;
	LogInformation("Initialization of semaphore in %s at %d", file, line);
	if ((status = sem_init(semaphore, 0, 0)) == -1)
	{
		LogError("sem_init() failed in %s at %d (errno=0x%X)", file, line, errno);
	}
	return status;
}

int semaphore_wait(sem_t * semaphore, const char * file, int line)
{
	int status;
	LogInformation("Wait on semaphore in %s at %d", file, line);

	if ((status = sem_wait(semaphore)) == -1)
	{
		LogError("sem_wait() failed in %s at %d (errno=0x%X)", file, line, errno);
	}
	return status;
}

int semaphore_post(sem_t * semaphore, const char * file, int line)
{
	int status;
	LogInformation("Post on semaphore in %s at %d", file, line);

	if ((status = sem_post(semaphore)) == -1)
	{
		LogError("sem_post() failed in %s at %d (errno=0x%X)", file, line, errno);
	}
	return status;
}


int semaphore_destroy(sem_t * semaphore, const char * file, int line)
{
	int status;
	LogInformation("Close semaphore in %s at %d", file, line);

	if ((status = sem_destroy(semaphore)) == -1)
	{
		LogError("sem_destroy() failed in %s at %d (errno=0x%X)", file, line, errno);
	}
	return status;
}

static const char HEXA_CHAR[16] =
{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

char * dumpUnit8Array(uint8_t * array, int length)
{
	char * dump = (char*) malloc(length * 3 + 1);

	int i;
	char * write = dump;
	uint8_t * read = array;
	uint8_t element;

	for (i = 0; i < length; i++)
	{
		element = *(read);
		read++;

		*(write) = HEXA_CHAR[(element >> 4) & 0xF];
		write++;

		*(write) = HEXA_CHAR[element & 0xF];
		write++;

		*(write) = ' ';
		write++;
	}

	*(write) = 0;

	return dump;
}

