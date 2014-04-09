/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#define LOG_TAG "EMCPUSTRESS"
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <cutils/xlog.h>
#include "ModuleCpuStress.h"
#include "RPCClient.h"

void * apmcu_test(void * argvoid) {
	struct thread_params_t * arg = (struct thread_params_t *) argvoid;
	int fd = -1;
	char value[10] = { 0 };
	size_t s = 0;
	do {
		fd = open(arg->file, O_RDWR);
		XLOGD("open file: %s", arg->file);
		if (fd < 0) {
			snprintf(arg->result, sizeof(arg->result), "%s",
					"fail to open device");
			XLOGE("fail to open device");
			break;
		}
		snprintf(value, sizeof(value), "%d", 1);
		write(fd, value, strlen(value));
		lseek(fd, 0, SEEK_SET);
		s = read(fd, arg->result, sizeof(arg->result));
		if (s <= 0) {
			snprintf(arg->result, sizeof(arg->result), "%s",
					"could not read response");
			break;
		}
	} while (0);
		if (fd >= 0) {
			close(fd);
		}
	pthread_exit(NULL);
	return NULL;
}

void * swcodec_test(void * argvoid) {
	struct thread_params_t * arg = (struct thread_params_t *) argvoid;
	int tid = gettid();
	XLOGD("tid: %d, Enter swcodec_test: file: %s", tid, arg->file);
	FILE * fp;
	struct timeval timeout;
	struct timeval delay;
	delay.tv_sec = 0;
	delay.tv_usec = 100 * 1000;
	do {
		XLOGD("tid: %d, before lock1", tid);
		pthread_mutex_lock(&lock);
		fp = popen(arg->file, "r");
		pthread_mutex_unlock(&lock);
		XLOGD("tid: %d, after unlock1", tid);
		select(0, NULL, NULL, NULL, &delay);
		if (fp == NULL) {
			XLOGE("popen fail: %s, errno: %d", arg->file, errno);
			strcpy(arg->result, "POPEN FAIL\n");
			break;
		}
		XLOGD("tid: %d, begin to get result", tid);
		char *ret;
		XLOGD("tid: %d, enter lock2", tid);
		while(1) {
			pthread_mutex_lock(&lock);
			ret = fgets(arg->result, sizeof(arg->result), fp);
			pthread_mutex_unlock(&lock);
			select(0, NULL, NULL, NULL, &delay);
			if (ret == NULL) {
				XLOGD("tid: %d, get result is null", tid);
				break;
			}
		}
	} while(0);
	if (fp != NULL) {
		XLOGD("tid: %d, before lock3", tid);
		pthread_mutex_lock(&lock);
		int closeRet = pclose(fp);
		pthread_mutex_unlock(&lock);
		XLOGD("tid: %d, after unlock3", tid);
		select(0, NULL, NULL, NULL, &delay);
		while (closeRet == -1) {
			XLOGD("tid: %d, before lock4", tid);
			pthread_mutex_lock(&lock);
			closeRet = pclose(fp);
			pthread_mutex_unlock(&lock);
			XLOGD("tid: %d, after unlock4", tid);
			select(0, NULL, NULL, NULL, &delay);
		}
		XLOGD("after pclose, tid: %d, errno: %d", tid, errno);
	}
	pthread_exit(NULL);
	return NULL;
}

void doApMcuTest(int index, RPCClient* msgSender) {
	struct thread_status_t test_thread1 = {
		pid : 0,
		create_result : -1,
	};
	struct thread_status_t test_thread2 = {
		pid : 0,
		create_result : -1,
	};
	struct thread_status_t test_thread3 = {
		pid : 0,
		create_result : -1,
	};
	struct thread_status_t test_thread4 = {
		pid : 0,
		create_result : -1,
	};
	switch (index) {
	case INDEX_TEST_NEON_3:
		strcpy(test_thread4.param.file, (char *) FILE_NEON_3);
		test_thread4.create_result = pthread_create(&test_thread4.pid, NULL,
				apmcu_test, (void *) &test_thread4.param);
	case INDEX_TEST_NEON_2:
		strcpy(test_thread3.param.file, (char *) FILE_NEON_2);
		test_thread3.create_result = pthread_create(&test_thread3.pid, NULL,
				apmcu_test, (void *) &test_thread3.param);
	case INDEX_TEST_NEON_1:
		strcpy(test_thread2.param.file, (char *) FILE_NEON_1);
		test_thread2.create_result = pthread_create(&test_thread2.pid, NULL,
				apmcu_test, (void *) &test_thread2.param);
	case INDEX_TEST_NEON_0:
		strcpy(test_thread1.param.file, (char *) FILE_NEON_0);
		test_thread1.create_result = pthread_create(&test_thread1.pid, NULL,
				apmcu_test, (void *) &test_thread1.param);
		break;
	case INDEX_TEST_CA9_3:
		strcpy(test_thread4.param.file, (char *) FILE_CA9_3);
		test_thread4.create_result = pthread_create(&test_thread4.pid, NULL,
				apmcu_test, (void *) &test_thread4.param);
	case INDEX_TEST_CA9_2:
		strcpy(test_thread3.param.file, (char *) FILE_CA9_2);
		test_thread3.create_result = pthread_create(&test_thread3.pid, NULL,
				apmcu_test, (void *) &test_thread3.param);
	case INDEX_TEST_CA9_1:
		strcpy(test_thread2.param.file, (char *) FILE_CA9_1);
		test_thread2.create_result = pthread_create(&test_thread2.pid, NULL,
				apmcu_test, (void *) &test_thread2.param);
	case INDEX_TEST_CA9_0:
		strcpy(test_thread1.param.file, (char *) FILE_CA9_0);
		test_thread1.create_result = pthread_create(&test_thread1.pid, NULL,
				apmcu_test, (void *) &test_thread1.param);
		break;
	case INDEX_TEST_DHRY_3:
		strcpy(test_thread4.param.file, (char *) FILE_DHRY_3);
		test_thread4.create_result = pthread_create(&test_thread4.pid, NULL,
				apmcu_test, (void *) &test_thread4.param);
	case INDEX_TEST_DHRY_2:
		strcpy(test_thread3.param.file, (char *) FILE_DHRY_2);
		test_thread3.create_result = pthread_create(&test_thread3.pid, NULL,
				apmcu_test, (void *) &test_thread3.param);
	case INDEX_TEST_DHRY_1:
		strcpy(test_thread2.param.file, (char *) FILE_DHRY_1);
		test_thread2.create_result = pthread_create(&test_thread2.pid, NULL,
				apmcu_test, (void *) &test_thread2.param);
	case INDEX_TEST_DHRY_0:
		strcpy(test_thread1.param.file, (char *) FILE_DHRY_0);
		test_thread1.create_result = pthread_create(&test_thread1.pid, NULL,
				apmcu_test, (void *) &test_thread1.param);
		break;
	case INDEX_TEST_MEMCPY_3:
		strcpy(test_thread4.param.file, (char *) FILE_MEMCPY_3);
		test_thread4.create_result = pthread_create(&test_thread4.pid, NULL,
				apmcu_test, (void *) &test_thread4.param);
	case INDEX_TEST_MEMCPY_2:
		strcpy(test_thread3.param.file, (char *) FILE_MEMCPY_2);
		test_thread3.create_result = pthread_create(&test_thread3.pid, NULL,
				apmcu_test, (void *) &test_thread3.param);
	case INDEX_TEST_MEMCPY_1:
		strcpy(test_thread2.param.file, (char *) FILE_MEMCPY_1);
		test_thread2.create_result = pthread_create(&test_thread2.pid, NULL,
				apmcu_test, (void *) &test_thread2.param);
	case INDEX_TEST_MEMCPY_0:
		strcpy(test_thread1.param.file, (char *) FILE_MEMCPY_0);
		test_thread1.create_result = pthread_create(&test_thread1.pid, NULL,
				apmcu_test, (void *) &test_thread1.param);
		break;
		case INDEX_TEST_FDCT_3:
			strcpy(test_thread4.param.file, (char *) FILE_FDCT_3);
			test_thread4.create_result = pthread_create(&test_thread4.pid, NULL,
					apmcu_test, (void *) &test_thread4.param);
		case INDEX_TEST_FDCT_2:
			strcpy(test_thread3.param.file, (char *) FILE_FDCT_2);
			test_thread3.create_result = pthread_create(&test_thread3.pid, NULL,
					apmcu_test, (void *) &test_thread3.param);
		case INDEX_TEST_FDCT_1:
			strcpy(test_thread2.param.file, (char *) FILE_FDCT_1);
			test_thread2.create_result = pthread_create(&test_thread2.pid, NULL,
					apmcu_test, (void *) &test_thread2.param);
		case INDEX_TEST_FDCT_0:
			strcpy(test_thread1.param.file, (char *) FILE_FDCT_0);
			test_thread1.create_result = pthread_create(&test_thread1.pid, NULL,
					apmcu_test, (void *) &test_thread1.param);
			break;
		case INDEX_TEST_IMDCT_3:
			strcpy(test_thread4.param.file, (char *) FILE_IMDCT_3);
			test_thread4.create_result = pthread_create(&test_thread4.pid, NULL,
					apmcu_test, (void *) &test_thread4.param);
		case INDEX_TEST_IMDCT_2:
			strcpy(test_thread3.param.file, (char *) FILE_IMDCT_2);
			test_thread3.create_result = pthread_create(&test_thread3.pid, NULL,
					apmcu_test, (void *) &test_thread3.param);
		case INDEX_TEST_IMDCT_1:
			strcpy(test_thread2.param.file, (char *) FILE_IMDCT_1);
			test_thread2.create_result = pthread_create(&test_thread2.pid, NULL,
					apmcu_test, (void *) &test_thread2.param);
		case INDEX_TEST_IMDCT_0:
			strcpy(test_thread1.param.file, (char *) FILE_IMDCT_0);
			test_thread1.create_result = pthread_create(&test_thread1.pid, NULL,
					apmcu_test, (void *) &test_thread1.param);
			break;
		default:
			break;
	}
	if (test_thread1.pid) {
		pthread_join(test_thread1.pid, NULL);
	}
	if (test_thread2.pid) {
		pthread_join(test_thread2.pid, NULL);
	}
	if (test_thread3.pid) {
		pthread_join(test_thread3.pid, NULL);
	}
	if (test_thread4.pid) {
		pthread_join(test_thread4.pid, NULL);
	}
	char result[CPUTEST_RESULT_SIZE] = { 0 };
	strncat(result, test_thread1.param.result, strlen(test_thread1.param.result)-1);
	strncat(result, ";", 1);
	strncat(result, test_thread2.param.result, strlen(test_thread2.param.result)-1);
	strncat(result, ";", 1);
	strncat(result, test_thread3.param.result, strlen(test_thread3.param.result)-1);
	strncat(result, ";", 1);
	strncat(result, test_thread4.param.result, strlen(test_thread4.param.result)-1);
	XLOGD("apmcu result is %s", result);
	msgSender->PostMsg(result);
}

int ModuleCpuStress::ApMcu(RPCClient* msgSender) {
	int paraNum = msgSender->ReadInt();
	int index = 0;
	if (paraNum != 1) {
		msgSender->PostMsg((char*) ERROR);
		return -1;
	}
	int T = msgSender->ReadInt();
	if (T != PARAM_TYPE_INT) {
		return -1;
	}
	int L = msgSender->ReadInt();
	index = msgSender->ReadInt();
	switch (index) {
	case INDEX_TEST_NEON_0:
	case INDEX_TEST_NEON_1:
	case INDEX_TEST_NEON_2:
	case INDEX_TEST_NEON_3:
	case INDEX_TEST_CA9_0:
	case INDEX_TEST_CA9_1:
	case INDEX_TEST_CA9_2:
	case INDEX_TEST_CA9_3:
	case INDEX_TEST_DHRY_0:
	case INDEX_TEST_DHRY_1:
	case INDEX_TEST_DHRY_2:
	case INDEX_TEST_DHRY_3:
	case INDEX_TEST_MEMCPY_0:
	case INDEX_TEST_MEMCPY_1:
	case INDEX_TEST_MEMCPY_2:
	case INDEX_TEST_MEMCPY_3:
	case INDEX_TEST_FDCT_0:
	case INDEX_TEST_FDCT_1:
	case INDEX_TEST_FDCT_2:
	case INDEX_TEST_FDCT_3:
	case INDEX_TEST_IMDCT_0:
	case INDEX_TEST_IMDCT_1:
	case INDEX_TEST_IMDCT_2:
	case INDEX_TEST_IMDCT_3:
		doApMcuTest(index, msgSender);
		break;
	default:
		XLOGE("apmcu unknown index: %d", index);
		break;
	}
	return 0;
}




void doSwCodecTest(int index, int iteration, RPCClient* msgSender) {
	XLOGD("Enter doSwCodecTest");
	struct thread_status_t swcodec_test_thread1 = {
		pid : 0,
		create_result : -1,
	};
	struct thread_status_t swcodec_test_thread2 = {
		pid : 0,
		create_result : -1,
	};
	struct thread_status_t swcodec_test_thread3 = {
		pid : 0,
		create_result : -1,
	};
	struct thread_status_t swcodec_test_thread4 = {
		pid : 0,
		create_result : -1,
	};
	char buf[10];
	snprintf(buf, sizeof(buf), "%d", iteration);
	switch(index) {
	case INDEX_SWCODEC_TEST_SINGLE:
		strcpy(swcodec_test_thread1.param.file, (char *) COMMAND_SWCODEC_TEST_SINGLE);
		// strcat(swcodec_test_thread1.param.file, buf);
		swcodec_test_thread1.create_result = pthread_create(&swcodec_test_thread1.pid, NULL,
			swcodec_test, (void *) &swcodec_test_thread1.param);
		break;
	case INDEX_SWCODEC_TEST_DUAL:
		strcpy(swcodec_test_thread1.param.file, (char *) COMMAND_SWCODEC_TEST_DUAL_0);
		// strcat(swcodec_test_thread1.param.file, buf);
		XLOGD("thread1 param.file: %s", swcodec_test_thread1.param.file);
		swcodec_test_thread1.create_result = pthread_create(&swcodec_test_thread1.pid, NULL,
			swcodec_test, (void *) &swcodec_test_thread1.param);
		strcpy(swcodec_test_thread2.param.file, (char *) COMMAND_SWCODEC_TEST_DUAL_1);
		// strcat(swcodec_test_thread2.param.file, buf);
		XLOGD("thread2 param.file: %s", swcodec_test_thread2.param.file);
		swcodec_test_thread2.create_result = pthread_create(&swcodec_test_thread2.pid, NULL,
			swcodec_test, (void *) &swcodec_test_thread2.param);
		break;
	case INDEX_SWCODEC_TEST_TRIPLE:
		strcpy(swcodec_test_thread1.param.file, (char *) COMMAND_SWCODEC_TEST_TRIPLE_0);
		// strcat(swcodec_test_thread1.param.file, buf);
		XLOGD("thread1 param.file: %s", swcodec_test_thread1.param.file);
		swcodec_test_thread1.create_result = pthread_create(&swcodec_test_thread1.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread1.param);
		strcpy(swcodec_test_thread2.param.file, (char *) COMMAND_SWCODEC_TEST_TRIPLE_1);
		// strcat(swcodec_test_thread2.param.file, buf);
		XLOGD("thread2 param.file: %s", swcodec_test_thread2.param.file);
		swcodec_test_thread2.create_result = pthread_create(&swcodec_test_thread2.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread2.param);
		strcpy(swcodec_test_thread3.param.file, (char *) COMMAND_SWCODEC_TEST_TRIPLE_2);
		// strcat(swcodec_test_thread3.param.file, buf);
		XLOGD("thread3 param.file: %s", swcodec_test_thread3.param.file);
		swcodec_test_thread3.create_result = pthread_create(&swcodec_test_thread3.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread3.param);
		break;
	case INDEX_SWCODEC_TEST_QUAD:
		strcpy(swcodec_test_thread1.param.file, (char *) COMMAND_SWCODEC_TEST_QUAD_0);
		// strcat(swcodec_test_thread1.param.file, buf);
		XLOGD("thread1 param.file: %s", swcodec_test_thread1.param.file);
		swcodec_test_thread1.create_result = pthread_create(&swcodec_test_thread1.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread1.param);
		strcpy(swcodec_test_thread2.param.file, (char *) COMMAND_SWCODEC_TEST_QUAD_1);
		// strcat(swcodec_test_thread2.param.file, buf);
		XLOGD("thread2 param.file: %s", swcodec_test_thread2.param.file);
		swcodec_test_thread2.create_result = pthread_create(&swcodec_test_thread2.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread2.param);
		strcpy(swcodec_test_thread3.param.file, (char *) COMMAND_SWCODEC_TEST_QUAD_2);
		// strcat(swcodec_test_thread3.param.file, buf);
		XLOGD("thread3 param.file: %s", swcodec_test_thread3.param.file);
		swcodec_test_thread3.create_result = pthread_create(&swcodec_test_thread3.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread3.param);
		strcpy(swcodec_test_thread4.param.file, (char *) COMMAND_SWCODEC_TEST_QUAD_3);
		// strcat(swcodec_test_thread4.param.file, buf);
		XLOGD("thread4 param.file: %s", swcodec_test_thread4.param.file);
		swcodec_test_thread4.create_result = pthread_create(&swcodec_test_thread4.pid, NULL,
				swcodec_test, (void *) &swcodec_test_thread4.param);
		break;
	default:
		break;
	}
	if (swcodec_test_thread1.pid) {
		pthread_join(swcodec_test_thread1.pid, NULL);
	}
	if (swcodec_test_thread2.pid) {
		pthread_join(swcodec_test_thread2.pid, NULL);
	}
	if (swcodec_test_thread3.pid) {
		pthread_join(swcodec_test_thread3.pid, NULL);
	}
	if (swcodec_test_thread4.pid) {
		pthread_join(swcodec_test_thread4.pid, NULL);
	}
	char result[CPUTEST_RESULT_SIZE] = { 0 };
	strncat(result, swcodec_test_thread1.param.result, strlen(swcodec_test_thread1.param.result)-1);
	strncat(result, ";", 1);
	strncat(result, swcodec_test_thread2.param.result, strlen(swcodec_test_thread2.param.result)-1);
	strncat(result, ";", 1);
	strncat(result, swcodec_test_thread3.param.result, strlen(swcodec_test_thread3.param.result)-1);
	strncat(result, ";", 1);
	strncat(result, swcodec_test_thread4.param.result, strlen(swcodec_test_thread4.param.result)-1);
	XLOGD("doSwCodecTest result is %s", result);
	msgSender->PostMsg(result);
}

int ModuleCpuStress::SwCodec(RPCClient* msgSender) {
	int paraNum = msgSender->ReadInt();
	int index = 0;
	int iteration = 0;
	if (paraNum != 2) {
		msgSender->PostMsg((char*) ERROR);
		return -1;
	}
	int T = msgSender->ReadInt();
	if (T != PARAM_TYPE_INT) {
		//error
		return -1;
	}
	int L = msgSender->ReadInt();
	index = msgSender->ReadInt();
	XLOGD("ModuleCpuStress:SwCodec index: %d", index);
	T = msgSender->ReadInt();
	if (T != PARAM_TYPE_INT) {
		return -1;
	}
	L = msgSender->ReadInt();
	iteration = msgSender->ReadInt();
	XLOGD("ModuleCpuStress:SwCodec iterate: %d", iteration);
	switch (index) {
		case INDEX_SWCODEC_TEST_SINGLE:
		case INDEX_SWCODEC_TEST_DUAL:
		case INDEX_SWCODEC_TEST_TRIPLE:
		case INDEX_SWCODEC_TEST_QUAD:
			doSwCodecTest(index, iteration, msgSender);
			break;
		default:
			XLOGE("SwCodec unknown index: %d", index);
			break;
		}
	return 0;
}

void doBackupRestore(int index) {
	char command[CPUTEST_RESULT_SIZE] = { 0 };
	FILE * fp;
	switch(index) {
		case INDEX_TEST_BACKUP:
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_first, sizeof(backup_first), fp);
			XLOGD("backup_first: %s", backup_first);
			pclose(fp);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_BACKUP: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_DISABLE);
			break;
		case INDEX_TEST_BACKUP_TEST:
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU1_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_TEST: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU2_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_TEST: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU3_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_TEST: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_BACKUP_TEST: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_DISABLE);
			break;
		case INDEX_TEST_BACKUP_SINGLE:
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_SINGLE popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_first, sizeof(backup_first), fp);
			XLOGD("backup_first: %s", backup_first);
			pclose(fp);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_BACKUP_SINGLE: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_CPU1_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_SINGLE: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_BACKUP_SINGLE: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_DISABLE);
			break;
		case INDEX_TEST_BACKUP_DUAL:
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_DUAL popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_first, sizeof(backup_first), fp);
			XLOGD("backup_first: %s", backup_first);
			pclose(fp);
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU1_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_DUAL popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_second, sizeof(backup_second), fp);
			XLOGD("backup_second: %s", backup_second);
			pclose(fp);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU1_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
			system(command);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU1_SCAL);
			XLOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_BACKUP_DUAL: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_DISABLE);
			break;
		case INDEX_TEST_BACKUP_TRIPLE:
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_TRIPLE popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_first, sizeof(backup_first), fp);
			XLOGD("backup_first: %s", backup_first);
			pclose(fp);
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU1_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_TRIPLE popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_second, sizeof(backup_second), fp);
			XLOGD("backup_second: %s", backup_second);
			pclose(fp);
			fp = popen(strcat(command, FILE_CPU2_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_TRIPLE popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_third, sizeof(backup_third), fp);
			XLOGD("backup_third: %s", backup_third);
			pclose(fp);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU1_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
			system(command);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU1_SCAL);
			XLOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU2_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
			system(command);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU2_SCAL);
			XLOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_BACKUP_TRIPLE: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_DISABLE);
			break;
		case INDEX_TEST_BACKUP_QUAD:
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU0_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_first, sizeof(backup_first), fp);
			XLOGD("backup_first: %s", backup_first);
			pclose(fp);
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU1_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_second, sizeof(backup_second), fp);
			XLOGD("backup_second: %s", backup_second);
			pclose(fp);
			fp = popen(strcat(command, FILE_CPU2_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_third, sizeof(backup_third), fp);
			XLOGD("backup_third: %s", backup_third);
			pclose(fp);
			strcpy(command, "cat ");
			fp = popen(strcat(command, FILE_CPU3_SCAL), "r");
			if (fp == NULL) {
				XLOGE("INDEX_TEST_BACKUP_QUAD popen fail, errno: %d", errno);
				return;
			}
			fgets(backup_fourth, sizeof(backup_fourth), fp);
			XLOGD("backup_fourth: %s", backup_fourth);
			pclose(fp);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU1_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU1_SCAL);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU2_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU2_SCAL);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_CPU3_ONLINE);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo performance > ");
			strcat(command, FILE_CPU3_SCAL);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_BACKUP_QUAD: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_DISABLE);
			break;
		case INDEX_TEST_RESTORE:
			strcpy(command, "echo ");
			strncat(command, backup_first, strlen(backup_first) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_RESTORE: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_ENABLE);
			break;
		case INDEX_TEST_RESTORE_TEST:
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_CPU1_ONLINE);
			XLOGD("INDEX_TEST_RESTORE_TEST: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_CPU2_ONLINE);
			XLOGD("INDEX_TEST_RESTORE_TEST: %s", command);
			system(command);
			strcpy(command, "echo 0 > ");
			strcat(command, FILE_CPU3_ONLINE);
			XLOGD("INDEX_TEST_RESTORE_TEST: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_RESTORE_TEST: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_ENABLE);
			break;
		case INDEX_TEST_RESTORE_SINGLE:
			strcpy(command, "echo ");
			strncat(command, backup_first, strlen(backup_first) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_RESTORE_SINGLE: %s", command);
			system(command);
			//strcpy(command, "echo 1 > ");
			//strcat(command, FILE_CPU1_ONLINE);
			//XLOGD("INDEX_TEST_RESTORE_SINGLE: %s", command);
			//system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_RESTORE_SINGLE: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_ENABLE);
			break;
		case INDEX_TEST_RESTORE_DUAL:
			strcpy(command, "echo ");
			strncat(command, backup_first, strlen(backup_first) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo ");
			strncat(command, backup_second, strlen(backup_second) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU1_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_ENABLE);
			break;
		case INDEX_TEST_RESTORE_TRIPLE:
			strcpy(command, "echo ");
			strncat(command, backup_first, strlen(backup_first) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo ");
			strncat(command, backup_second, strlen(backup_second) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU1_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo ");
			strncat(command, backup_third, strlen(backup_third) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU2_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_ENABLE);
			break;
		case INDEX_TEST_RESTORE_QUAD:
			strcpy(command, "echo ");
			strncat(command, backup_first, strlen(backup_first) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU0_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo ");
			strncat(command, backup_second, strlen(backup_second) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU1_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo ");
			strncat(command, backup_third, strlen(backup_third) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU2_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo ");
			strncat(command, backup_fourth, strlen(backup_fourth) - 1);
			strcat(command, " > ");
			strcat(command, FILE_CPU3_SCAL);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			strcpy(command, "echo 1 > ");
			strcat(command, FILE_HOTPLUG);
			XLOGD("INDEX_TEST_RESTORE_DUAL: %s", command);
			system(command);
			system(COMMAND_HOTPLUG_ENABLE);
			break;
		default:
			break;
	}
}

int ModuleCpuStress::BackupRestore(RPCClient* msgSender) {
	int paraNum = msgSender->ReadInt();
	int index = 0;
	if (paraNum != 1) {
		msgSender->PostMsg((char*) ERROR);
		return -1;
	}
	int T = msgSender->ReadInt();
	if (T != PARAM_TYPE_INT) {
		return -1;
	}
	int L = msgSender->ReadInt();
	index = msgSender->ReadInt();
	switch (index) {
	case INDEX_TEST_BACKUP:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_BACKUP");
		break;
	case INDEX_TEST_BACKUP_TEST:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_TEST");
		break;
	case INDEX_TEST_BACKUP_SINGLE:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_SINGLE");
		break;
	case INDEX_TEST_BACKUP_DUAL:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_DUAL");
		break;
	case INDEX_TEST_BACKUP_TRIPLE:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_TRIPLE");
		break;
	case INDEX_TEST_BACKUP_QUAD:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_BACKUP_QUAD");
		break;
	case INDEX_TEST_RESTORE:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_RESTORE");
		break;
	case INDEX_TEST_RESTORE_TEST:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_TEST");
		break;
	case INDEX_TEST_RESTORE_SINGLE:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_SINGLE");
		break;
	case INDEX_TEST_RESTORE_DUAL:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_DUAL");
		break;
	case INDEX_TEST_RESTORE_TRIPLE:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_TRIPLE");
		break;
	case INDEX_TEST_RESTORE_QUAD:
		doBackupRestore(index);
		msgSender->PostMsg((char *)"INDEX_TEST_RESTORE_QUAD");
		break;
	default:
		XLOGE("BackupRestore unknown index: %d", index);
		msgSender->PostMsg((char *)"BackRestore unknown index");
		break;
	}
	return 0;
}


int ModuleCpuStress::ThermalUpdate(RPCClient* msgSender) {
	int paraNum = msgSender->ReadInt();
	int index = 0;
	if (paraNum != 1) {
		msgSender->PostMsg((char*) ERROR);
		return -1;
	}
	int T = msgSender->ReadInt();
	if (T != PARAM_TYPE_INT) {
		return -1;
	}
	int L = msgSender->ReadInt();
	index = msgSender->ReadInt();
	switch(index) {
	case INDEX_THERMAL_DISABLE:
		system(THERMAL_DISABLE_COMMAND);
		XLOGD("disable thermal: %s", THERMAL_DISABLE_COMMAND);
		msgSender->PostMsg((char *)"INDEX_THERMAL_DISABLE");
		break;
	case INDEX_THERMAL_ENABLE:
		system(THERMAL_ENABLE_COMMAND);
		XLOGD("enable thermal: %s", THERMAL_ENABLE_COMMAND);
		msgSender->PostMsg((char *)"INDEX_THERMAL_ENABLE");
		break;
	default:
		break;
	}
	return 0;
}

ModuleCpuStress::ModuleCpuStress(void) {
	pthread_mutex_init(&lock, NULL);
}
ModuleCpuStress::~ModuleCpuStress(void) {
	pthread_mutex_destroy(&lock);
}

