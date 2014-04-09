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

#include "dm_agent.h"
#include <cutils/properties.h>
#include <sys/types.h>
#include <sys/stat.h>

#define DM_ROOT_PATH "/data/nvram/dm/"
#define DM_TREE_PATH DM_ROOT_PATH"tree"
#define DM_TREE_PATH_BACKUP DM_ROOT_PATH"tree~"
#define DM_LOCK_PATH DM_ROOT_PATH"lock"
#define DM_WIPE_PATH DM_ROOT_PATH"wipe"
#define DM_IMSI_PATH DM_ROOT_PATH"imsi"
#define DM_OPERATOR_PATH DM_ROOT_PATH"operator"
#define DM_CTA_PATH DM_ROOT_PATH"cta_cmcc"
#define RECOVERY_COMMAND "/cache/recovery/command"
#define MISC_PATH "/dev/misc"

#define MAX_FILE_SIZE 300*1024

#define DM_READ_NO_EXCEPTION 0

#define OPERATOR_LEN 10
//#define START_BLOCK 0x10C0000
#define START_BLOCK 0
#define BLOCK_SIZE 0x20000
//#define BOOT_PARTITION 7
//#define UPGRADE_PARTITION 1


#define OTA_RESULT_OFFSET    (2560)

//for eMMC ONLY.
int get_ota_result(int *i) {

	int dev = -1;
	char dev_name[32];
	int count;
	int result;

	strcpy(dev_name, MISC_PATH);

	dev = open(dev_name, O_RDONLY);
	if (dev < 0) {
		printf("Can't open %s\n(%s)\n", dev_name, strerror(errno));
		return -1;
	}

	if (lseek(dev, OTA_RESULT_OFFSET, SEEK_SET) == -1) {
		printf("Failed seeking %s\n(%s)\n", dev_name, strerror(errno));
		close(dev);
		return -1;
	}

	count = read(dev, &result, sizeof(result));
	if (count != sizeof(result)) {
		printf("Failed reading %s\n(%s)\n", dev_name, strerror(errno));
		return -1;
	}
	if (close(dev) != 0) {
		printf("Failed closing %s\n(%s)\n", dev_name, strerror(errno));
		return -1;
	}

	*i = result;
	return 0;
}

int file_exist(const char * path) {
	struct stat lock_stat;
	bzero(&lock_stat, sizeof(lock_stat));
	int ret = stat(path, &lock_stat);
	return ret + 1; // 1 if exists,0 if not
}

void DMAgent::instantiate() {
	while (true) {
		DMAgent *agent = new DMAgent();
		status_t ret = defaultServiceManager()->addService(descriptor, agent);
		if (ret == OK) {
			XLOGI("[DMAgent]register OK.");
			break;
		}

		XLOGD("[DMAgent]register FAILED. retrying in 5sec.");

		sleep(5);
	}
}

DMAgent::DMAgent() {
	XLOGI("DMAgent created");
}

status_t BnDMAgent::onTransact(uint32_t code, const Parcel &data,
		Parcel *reply, uint32_t flags) {

	XLOGI("OnTransact   (%u,%u)", code, flags);
	reply->writeInt32(DM_READ_NO_EXCEPTION);//used for readException

	switch (code) {
	case TRANSACTION_setLockFlag: {
		/*	XLOGI("setLockFlag\n");
		 data.enforceInterface (descriptor);
		 reply->writeInt32 (setLockFlag ());
		 // XLOGI("locked\n");
		 return NO_ERROR;
		 */
		XLOGI("setLockFlag\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		XLOGD("setLockFlag len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			XLOGD("setLockFlag buff  = %s\n", buff);
			reply->writeInt32(setLockFlag(buff, len));
		}
		XLOGI("setLockFlag done\n");
		return NO_ERROR;

	}
		break;
	case TRANSACTION_clearLockFlag: {
		XLOGI("clearLockFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearLockFlag());
		XLOGI("cleared\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readDMTree: {
		XLOGI("readDMTree\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readDMTree(size);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		XLOGI("DMTree read done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_writeDMTree: {
		XLOGI("writeDMTree\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			reply->writeInt32(writeDMTree(buff, len));
		}
		XLOGI("DMTree wrote\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isLockFlagSet: {
		XLOGI("isLockFlagSet\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isLockFlagSet());
		XLOGI("isLockFlagSet done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readIMSI: {
		XLOGI("readIMSI\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readIMSI(size);
		XLOGD("readIMSI = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		XLOGI("readIMSI done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_writeIMSI: {
		XLOGI("writeIMSI\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		XLOGD("writeIMSI len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			XLOGD("writeIMSI buff  = %s\n", buff);
			reply->writeInt32(writeIMSI(buff, len));
		}
		XLOGI("writeIMSI done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readCTA: {
		XLOGI("readCTA\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readCTA(size);
		XLOGD("readCTA = %s\n", ret);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		XLOGI("readCTA done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_writeCTA: {
		XLOGI("writeCTA\n");
		data.enforceInterface(descriptor);
		int len = data.readInt32();
		XLOGD("writeCTA len  = %d\n", len);
		if (len == -1) { // array is null
			reply->writeInt32(0);
		} else {
			char buff[len];
			data.read(buff, len);
			XLOGD("writeCTA buff  = %s\n", buff);
			reply->writeInt32(writeCTA(buff, len));
		}
		XLOGI("writeCTA done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readOperatorName: {
		XLOGI("readOperatorName\n");
		data.enforceInterface(descriptor);
		int size = 0;
		char * ret = readOperatorName(size);
		if (ret == NULL) {
			reply->writeInt32(-1);
		} else {
			reply->writeInt32(size);
			reply->write(ret, size);
			free(ret);
		}
		XLOGI("readOperatorName done\n");
		return NO_ERROR;
	}
		break;

	case TRANSACTION_setRebootFlag: {
		XLOGI("setRebootFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(setRebootFlag());
		XLOGI("setRebootFlag done\n");
		return NO_ERROR;
	}
		break;

	case TRANSACTION_getLockType: {
		XLOGI("getLockType\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(getLockType());
		XLOGI("getLockType done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getOperatorID: {
		XLOGI("getOperatorID\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(getOperatorID());
		XLOGI("getOperatorID done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getOperatorName: {
		XLOGI("getOperatorName\n");
		data.enforceInterface(descriptor);
		char * ret = getOperatorName();
		if (ret == NULL)
			reply->writeInt32(-1);
		else
			reply->writeInt32(0);
		XLOGI("getOperatorName done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isHangMoCallLocking: {
		XLOGI("isHangMoCallLocking\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isHangMoCallLocking());
		XLOGI("isHangMoCallLocking done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isHangMtCallLocking: {
		XLOGI("isHangMtCallLocking\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isHangMtCallLocking());
		XLOGI("isHangMtCallLocking\n");
		return NO_ERROR;
	}
		break;

	case TRANSACTION_clearRebootFlag: {
		XLOGI("clearRebootFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearRebootFlag());
		XLOGI("clearRebootFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isBootRecoveryFlag: {
		XLOGI("isBootRecoveryFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isBootRecoveryFlag());
		XLOGI("isBootRecoveryFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_isWipeSet: {
		XLOGI("isWipeset\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(isWipeSet());
		XLOGI("isWipeset done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_setWipeFlag: {
		XLOGI("setWipeFlag\n");
		data.enforceInterface(descriptor);
		//int len=data.readInt32 ();
		reply->writeInt32(setWipeFlag("FactoryReset", sizeof("FactoryReset")));
		XLOGI("setWipeFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_clearWipeFlag: {
		XLOGI("clearWipeFlag\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearWipeFlag());
		XLOGI("clearWipeFlag done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_getUpgradeStatus: {
		XLOGI("getUpgradeStatus\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(getUpgradeStatus());
		XLOGI("getUpgradeStatus done\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_restartAndroid: {
		XLOGI("restartAndroid\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(restartAndroid());
		XLOGI("restartAndroid\n");
		return NO_ERROR;
	}
		break;
	case TRANSACTION_readOtaResult: {
		XLOGI("readOtaResult\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(readOtaResult());
		return NO_ERROR;
	}
		break;
	case TRANSACTION_clearOtaResult: {
		XLOGI("clearOtaResult\n");
		data.enforceInterface(descriptor);
		reply->writeInt32(clearOtaResult());
		return NO_ERROR;
	}
		break;
	default:
		return BBinder::onTransact(code, data, reply, flags);
	}

	return NO_ERROR;
}

char* DMAgent::readDMTree(int & size) {
	int dm_fd = open(DM_TREE_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_TREE_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		return buff;
	}
}

int DMAgent::writeDMTree(char* tree, int size) {
	if (tree == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_backup_fd = open(DM_TREE_PATH_BACKUP, O_CREAT | O_WRONLY | O_TRUNC,
			0775);
	if (dm_backup_fd == -1) {
		return 0;
	}
	write(dm_backup_fd, tree, size);
	sync();
	close(dm_backup_fd);
	int dm_fd = open(DM_TREE_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	dm_backup_fd = open(DM_TREE_PATH_BACKUP, O_RDONLY);
	if (dm_fd == -1 || dm_backup_fd == -1) {
		close(dm_fd);
		close(dm_backup_fd);
		return 0;
	} else {
		int count = 0;
		char buff[512];
		while ((count = read(dm_backup_fd, buff, 512)) > 0) {
			write(dm_fd, buff, count);
		}
		sync();
		close(dm_fd);
		close(dm_backup_fd);
		unlink(DM_TREE_PATH_BACKUP);
		FileOp_BackupToBinRegionForDM();
	}
	return 1;
}

int DMAgent::isLockFlagSet() {
	return ::file_exist(DM_LOCK_PATH);
}

int DMAgent::setLockFlag(char *lockType, int len) {

	XLOGD("the lockType  is %s  len = %d\n", lockType, len);
	if (lockType == NULL || len == 0 || len > MAX_FILE_SIZE) {
		return 0;
	}

	int fd = open(DM_LOCK_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (fd == -1) {
		XLOGE("Open LOCK FILE error\n");
		return 0;
	}
	int count = write(fd, lockType, len);
	sync();
	close(fd);
	FileOp_BackupToBinRegionForDM();
	property_set("persist.dm.lock", "true");
	if (!::file_exist(DM_LOCK_PATH) && count == len) {
		return 0;
	}

	return 1;
}

int DMAgent::clearLockFlag() {
	if (::file_exist(DM_LOCK_PATH)) {
		unlink(DM_LOCK_PATH);
		property_set("persist.dm.lock", "false");
		FileOp_BackupToBinRegionForDM();
		if (::file_exist(DM_LOCK_PATH)) {
			return 0;
		}
	}
	return 1;
}
int DMAgent::isWipeSet() {
	return ::file_exist(DM_WIPE_PATH);
}

int DMAgent::setWipeFlag(char *wipeType, int len) {

	XLOGD("the wipeType  is %s  len = %d\n", wipeType, len);
	if (wipeType == NULL || len == 0 || len > MAX_FILE_SIZE) {
		return 0;
	}

	int fd = open(DM_WIPE_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (fd == -1) {
		XLOGE("Open WIPE FILE error\n");
		return 0;
	}
	int count = write(fd, wipeType, len);
	sync();
	close(fd);
	FileOp_BackupToBinRegionForDM();
	if (!::file_exist(DM_WIPE_PATH) && count == len) {
		return 0;
	}

	return 1;
}

int DMAgent::clearWipeFlag() {
	if (::file_exist(DM_WIPE_PATH)) {
		unlink(DM_WIPE_PATH);
		FileOp_BackupToBinRegionForDM();
		if (::file_exist(DM_WIPE_PATH)) {
			return 0;
		}
	}
	return 1;
}
char * DMAgent::readIMSI(int & size) {
	int dm_fd = open(DM_IMSI_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_IMSI_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		XLOGD("the readIMSI buffer = %s\n", buff);
		return buff;
	}
}

int DMAgent::writeIMSI(char * imsi, int size) {
	XLOGD("the imsi want to save is %s\n", imsi);
	if (imsi == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_IMSI_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, imsi, size);
	sync();
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}
char * DMAgent::readCTA(int & size) {
	int dm_fd = open(DM_CTA_PATH, O_RDONLY);
	if (dm_fd == -1) {
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_CTA_PATH, &file_stat);
		size = file_stat.st_size;
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		XLOGD("the readCTA buffer = %s\n", buff);
		return buff;
	}
}

int DMAgent::writeCTA(char * cta, int size) {
	XLOGD("the cta want to save is %s\n", cta);
	if (cta == NULL || size == 0 || size > MAX_FILE_SIZE) {
		return 0;
	}
	int dm_fd = open(DM_CTA_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775);
	if (dm_fd == -1) {
		return 0;
	}
	int count = write(dm_fd, cta, size);
	sync();
	close(dm_fd);
	FileOp_BackupToBinRegionForDM();
	if (count == size) {
		return 1;
	} else {
		return 0;
	}
}
char * DMAgent::readOperatorName(int & size) {
	int dm_fd = open(DM_OPERATOR_PATH, O_RDONLY);
	if (dm_fd == -1) {
		XLOGE("readopertorname fd is -1");
		return NULL;
	} else {
		// get file size
		struct stat file_stat;
		bzero(&file_stat, sizeof(file_stat));
		stat(DM_OPERATOR_PATH, &file_stat);
		size = file_stat.st_size - 1;
		XLOGD("readopertorname size is %d", size);
		char *buff = (char *) malloc(size);
		read(dm_fd, buff, size);
		close(dm_fd);
		return buff;
	}
}

int DMAgent::setRecoveryCommand() {
	XLOGD("Enter to save recovery command");
	if (::file_exist(RECOVERY_COMMAND)) {
		unlink(RECOVERY_COMMAND);
	}

	int fd = open(RECOVERY_COMMAND, O_CREAT | O_WRONLY | O_TRUNC, 0746);
	if (fd == -1) {
		XLOGE("Open RECOVERY_COMMAND error: [%d]\n",errno);
		return 0;
	}
	char command[] = "--fota_delta_path=/data/delta";
	int len = sizeof(command);
	XLOGD("recovery command lenth is [%d]\n", len);
	int count = write(fd, command, len);
	sync();
	XLOGD("--recovery command sync--");
	close(fd);
	if (count < 0 || count != len) {
		XLOGE("Recovery command write error or the count =[%d] is not the len",
				count);
		return 0;
	}
	return 1;

}

int DMAgent::getLockType() {
	//0 -partially lock 1- fully lock
	if (::file_exist(DM_LOCK_PATH)) {
		//if file exist then get the type
		int lock_fd = open(DM_LOCK_PATH, O_RDONLY);
		if (lock_fd == -1) {
			XLOGE("read lock file fd is -1");
			return -1;
		} else {
			// get file size
			struct stat file_stat;
			bzero(&file_stat, sizeof(file_stat));
			stat(DM_LOCK_PATH, &file_stat);
			//int size=file_stat.st_size-1;
			int size = file_stat.st_size;
			XLOGD("read lock file size is %d", size);
			char *buff = (char *) malloc(size);
			read(lock_fd, buff, size);
			close(lock_fd);

			XLOGD("Read lock file buff = [%s]\n", buff);
			if (strncmp(buff, "partially", 9) == 0) {
				XLOGD("Partially lock");
				return 0;
			} else if (strncmp(buff, "fully", 5) == 0) {
				XLOGD("fully lock");
				return 1;
			} else {
				XLOGE("Not partially lock and fully lock, error!");
				return -1;
			}
		}
	} else
		return NO_ERROR;
}

int DMAgent::getOperatorID() {
	//0 -partially lock 1- fully lock
	return 46002;
}

char* DMAgent::getOperatorName() {
	int len = OPERATOR_LEN;
	return readOperatorName(len);
}

int DMAgent::isHangMoCallLocking() {
	//0 -ture 1 -false 
	//if the lock file is exist then Mo call is NOT allow
	if (!::file_exist(DM_LOCK_PATH)) {
		return 0;
	}
	return 1;
}

int DMAgent::isHangMtCallLocking() {
	//0 -ture 1 -false 
	if (!::file_exist(DM_LOCK_PATH)) {
		return 0;
	}
	return 1;
	/*if(getLockType()==0){
	 return 1;
	 }else if(getLockType()==1){
	 return 0;
	 }else{
	 XLOGE("error cmd\n");
	 return -1;
	 }*/

}

int DMAgent::setRebootFlag() {
	XLOGD("[REBOOT_FLAG] : enter setRebootFlag");
	char cmd[] = "boot-recovery";
	int ret = writeRebootFlash(cmd);
	if (ret < 1) {
		XLOGE("Write boot-recovery to misc error");
		return ret;
	}

	ret = setRecoveryCommand();
	if (ret < 1) {
		XLOGE("Wirte recovery command error");
	}

	return ret;
	//	char cmd[] = "boot-recovery";
	//	return writeRebootFlash(cmd);
}

int DMAgent::clearRebootFlag() {
	XLOGD("[REBOOT_FLAG] : enter clearRebootFlag");
	//boot to android the command is null
	char cmd[] = "";
	return writeRebootFlash(cmd);
}

int DMAgent::isBootRecoveryFlag() {
	int fd;
	int readSize = 0;
	int result = 0;
	//    int miscNumber = 0; //we can get this num from SS6
	int bootEndBlock = 2048;
	//    miscNumber = get_partition_numb("misc");

	readSize = sizeof("boot-recovery");
	char *readResult = readMiscPartition(readSize);
	if (readResult == NULL) {
		XLOGE("[isBootRecoveryFlag] : read misc partition recovery is error");
	} else if (strcmp(readResult, "boot-recovery") == 0) {
		//the next reboot is recovery
		result = 1;
	} else if (strcmp(readResult, "") == 0) {
		//the next reboot is normal mode
		result = 0;
		free(readResult);
		readResult = NULL;
	}
	return result;
}

char* DMAgent::readMiscPartition(int readSize) {
	int fd;
	int result;
	int iRealReadSize = readSize;
	char *readBuf = (char *) malloc(iRealReadSize);
	if (NULL == readBuf) {
		XLOGE("[readMiscPartition] : malloc error");
		return NULL;
	}

	memset(readBuf, '\0', iRealReadSize);
	//    int miscPartition = miscNum; //we can get this num from SS6
	int readEndBlock = 2048;

//	XLOGD("[ReadMiscPartion]:misc number is [%d] read size is  [%d]\r\n",
//			miscPartition, iRealReadSize);
	struct mtd_info_user info;
	char devName[32];
	memset(devName, '\0', sizeof(devName));
	//sprintf(devName,"/dev/mtd/mtd%d",miscPartition);
	sprintf(devName, MISC_PATH);
	fd = open(devName, O_RDWR);
	if (fd < 0) {
		XLOGD("[ReadMiscPartition]:mtd open error\r\n");
		return NULL;
	}

#ifndef MTK_EMMC_SUPPORT
	//need lseek 2048 for NAND only
	result = lseek(fd, readEndBlock, SEEK_SET);
	if (result != (readEndBlock)) {
		XLOGE("[ReadMiscPartition]:mtd lseek error\r\n");
		return NULL;
	}
#endif

	//read from misc partition to make sure it is correct
	result = read(fd, readBuf, iRealReadSize);
	if (result != iRealReadSize) {
		XLOGE("[ReadMiscPartition]:mtd read error\r\n");
		free(readBuf);
		readBuf = NULL;
		close(fd);
		return NULL;
	}

	XLOGD("[ReadMiscPartition]:end to read  readbuf = %s\r\n", readBuf);
	close(fd);
	return readBuf;

}

//int DMAgent::getUpgradeStatus()
int DMAgent::getUpgradeStatus() {
	int fd;
	int readSize = 32;
	//int miscNumber = UPGRADE_PARTITION;
	//    int miscNumber = 0;
	//    miscNumber = get_partition_numb("misc");
	int iWriteSize = 512;
	int result;
	int statusEndBlock = 2048;
	char *readBuf = NULL;

	int iRealWriteSize = 0;
	//    int miscPartition = get_partition_numb("misc"); //we can get this num from SS6

	// for test  
	char *tempBuf = NULL;

	struct mtd_info_user info;
	struct erase_info_user erase_info;
	XLOGD("[getUpgradeStatus]:enter write flash\r\n");
	char devName[32];
	memset(devName, '\0', sizeof(devName));
	//sprintf(devName,"/dev/mtd/mtd%d",miscPartition);
	sprintf(devName, MISC_PATH);
	fd = open(devName, O_RDWR);
	if (fd < 0) {
		XLOGE("[getUpgradeStatus]:mtd open error\r\n");
		return 0;
	}

	XLOGD("[getUpgradeStatus]:before memget ioctl fd = %d\r\n", fd);

	result = ioctl(fd, MEMGETINFO, &info);
	if (result < 0) {
		XLOGE("[getUpgradeStatus]:mtd get info error\r\n");
		return 0;
	}
	iWriteSize = info.writesize;

	XLOGD("[getUpgradeStatus]:after memget ioctl fd = %d\r\n", fd);

	XLOGI("[getUpgradeStatus]:start to earse\r\n");
	erase_info.start = __u64(START_BLOCK);
	erase_info.length = __u64(BLOCK_SIZE);
	XLOGD("[getUpgradeStatus]:before erase ioctl u64 convert fd = %d\r\n", fd);
	result = ioctl(fd, MEMERASE, &erase_info);
	if (result < 0) {
		XLOGE(
				"[getUpgradeStatus]:mtd erase error result = %d errorno = [%d] err =[%s] \r\n",
				result, errno, strerror(errno));
		close(fd);
		free(tempBuf);
		return 0;
	}

	XLOGI("[getUpgradeStatus]:end to earse\r\n");

	tempBuf = (char *) malloc(iWriteSize);

	if (tempBuf == NULL) {
		XLOGE("[getUpgradeStatus]:malloc error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
	memset(tempBuf, 0, iWriteSize);
	iRealWriteSize = sizeof("-12");
	memcpy(tempBuf, "-12", iRealWriteSize);

	XLOGI("[getUpgradeStatus]:start to write\r\n");

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, statusEndBlock, SEEK_SET);
	if (result != (statusEndBlock)) {
		XLOGE("[getUpgradeStatus]:mtd first lseek error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
#endif

	result = write(fd, tempBuf, iWriteSize);
	sync();
	if (result != iWriteSize) {
		XLOGE("[getUpgradeStatus]:mtd write error,iWriteSize:%d\r\n",
				iWriteSize);
		close(fd);
		free(tempBuf);
		return 0;
	}
	memset(tempBuf, 0, iWriteSize);

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, statusEndBlock, SEEK_SET);
	if (result != (statusEndBlock)) {
		XLOGE("[getUpgradeStatus]:mtd second lseek error\r\n");
		free(tempBuf);
		return 0;
	}
#endif

	XLOGI("[getUpgradeStatus]:end to write\r\n");
	//for test end 


	readBuf = readMiscPartition(readSize);
	if (readBuf == NULL) {
		XLOGE("[getUpgradeStatus] read Misc paartition error");
		result = 1;
	} else {
		//tranfer char * to int
		XLOGD("[getUpgradeStatus] : the readbuf is [%s]", readBuf);
		result = atoi(readBuf);
	}

	return result;

}

//int DMAgent::writeRebootFlash(unsigned int iMagicNum)
int DMAgent::writeRebootFlash(char *rebootCmd) {
	int fd;
	int iWriteSize = 512;
	int iRealWriteSize = 0;
	int result;
	//    int miscPartition = 0; //we can get this num from SS6, not used
	int bootEndBlock = 2048;
	char *tempBuf = NULL;
	//    miscPartition = get_partition_numb("misc");
	struct mtd_info_user info;
	struct erase_info_user erase_info;
	XLOGD("[REBOOT_FLAG]:enter write flash  the cmd is [%s]\r\n", rebootCmd);
	char devName[32];
	memset(devName, '\0', sizeof(devName));
	//sprintf(devName,"/dev/mtd/mtd%d",miscPartition);
	sprintf(devName, MISC_PATH);
	fd = open(devName, O_RDWR);
	if (fd < 0) {
		XLOGD("[REBOOT_FLAG]:mtd open error\r\n");
		return 0;
	}

	XLOGD("[REBOOT_FLAG]:before memget ioctl fd = %d\r\n", fd);

	result = ioctl(fd, MEMGETINFO, &info);
	if (result < 0) {
		XLOGE("[REBOOT_FLAG]:mtd get info error\r\n");
		close(fd);
		return 0;
	}
	iWriteSize = info.writesize;

	XLOGD("[REBOOT_FLAG]:after memget ioctl fd = %d\r\n", fd);

	XLOGI("[REBOOT_FLAG]:start to earse\r\n");
	erase_info.start = __u64(START_BLOCK);
	erase_info.length = __u64(BLOCK_SIZE);
	XLOGD("[REBOOT_FLAG]:before erase ioctl u64 convert fd = %d\r\n", fd);
	result = ioctl(fd, MEMERASE, &erase_info);
	if (result < 0) {
		XLOGE(
				"[REBOOT_FLAG]:mtd erase error result = %d errorno = [%d] err =[%s] \r\n",
				result, errno, strerror(errno));
		close(fd);
		free(tempBuf);
		return 0;
	}

	XLOGI("[REBOOT_FLAG]:end to earse\r\n");

	tempBuf = (char *) malloc(iWriteSize);

	if (tempBuf == NULL) {
		XLOGE("[REBOOT_FLAG]:malloc error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
	memset(tempBuf, 0, iWriteSize);
	iRealWriteSize = strlen(rebootCmd);
	memcpy(tempBuf, rebootCmd, iRealWriteSize);

	XLOGD("[REBOOT_FLAG]:start to write tempBuff = %s\r\n", tempBuf);

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, bootEndBlock, SEEK_SET);
	if (result != (bootEndBlock)) {
		XLOGE("[REBOOT_FLAG]:mtd first lseek error\r\n");
		close(fd);
		free(tempBuf);
		return 0;
	}
#endif

	result = write(fd, tempBuf, iWriteSize);
	sync();
	if (result != iWriteSize) {
		XLOGE("[REBOOT_FLAG]:mtd write error,iWriteSize:%d\r\n", iWriteSize);
		close(fd);
		free(tempBuf);
		return 0;
	}

#ifndef MTK_EMMC_SUPPORT
	result = lseek(fd, bootEndBlock, SEEK_SET);
	if (result != (bootEndBlock)) {
		XLOGE("[REBOOT_FLAG]:mtd second lseek error\r\n");
		free(tempBuf);
		return 0;
	}
#endif

	XLOGD("[REBOOT_FLAG]:end to write iRealWriteSize = %d \r\n", iRealWriteSize);

	free(tempBuf);
	close(fd);
	return 1;
}

int DMAgent::restartAndroid() {
	XLOGI(
			"Before restart android DM is going to restart Andorid sleep 10 seconds");
	sleep(10);
	property_set("ctl.stop", "runtime");
	property_set("ctl.stop", "zygote");
	property_set("ctl.start", "zygote");
	property_set("ctl.start", "runtime");
	XLOGI("DM has restarting Andoird");
	return 1;
}

int DMAgent::clearOtaResult()
{
    int dev = -1;
    char dev_name[32];
    int count;
	int i = 0;

    strcpy(dev_name, MISC_PATH);

    dev = open(dev_name, O_WRONLY);
    if (dev < 0)  {
        XLOGE("[clearUpgradeResult]Can't open %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }

    if (lseek(dev, OTA_RESULT_OFFSET, SEEK_SET) == -1) {
        XLOGE("[clearUpgradeResult]Failed seeking %s\n(%s)\n", dev_name, strerror(errno));
        close(dev);
        return 0;
    }

    count = write(dev, &i, sizeof(i));
	sync();
    if (count != sizeof(i)) {
        XLOGE("[clearUpgradeResult]Failed writing %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }
    if (close(dev) != 0) {
        XLOGE("[clearUpgradeResult]Failed closing %s\n(%s)\n", dev_name, strerror(errno));
        return 0;
    }

    return 1;
}

int DMAgent::readOtaResult() {
	int result;

	get_ota_result(&result);
	XLOGD("ota_result=%d\n", result);

	return result;
}

int main(int argc, char *argv[]) {
	//    daemon (0,0);
	umask(000);
	mkdir(DM_ROOT_PATH, 0775);

	DMAgent::instantiate();
	if (::file_exist(DM_LOCK_PATH)) {
		property_set("persist.dm.lock", "true");
	} else {
		property_set("persist.dm.lock", "false");
	}
	ProcessState::self()->startThreadPool();
	XLOGD("DMAgent Service is now ready");
	IPCThreadState::self()->joinThreadPool();
	return (0);
}

