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


#include <utils/KeyedVector.h>
#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <utils/String16.h>
#include <utils/threads.h>

#include <sys/socket.h>
#include <sys/un.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <utils/Log.h>
#include <cutils/xlog.h>

#include<sys/mount.h>
#include "../../../../kernel/include/mtd/mtd-abi.h"
#include "../../external/nvram/libfile_op/libfile_op.h"

#include <cutils/atomic.h>
#include <utils/Errors.h>
#include <binder/IServiceManager.h>
#include <utils/String16.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <utils/Vector.h>
#undef LOG_TAG
#define LOG_TAG "DMAgent"

using namespace android;
enum {
    TRANSACTION_readDMTree = IBinder::FIRST_CALL_TRANSACTION,
    TRANSACTION_writeDMTree,
    TRANSACTION_isLockFlagSet,
    TRANSACTION_setLockFlag,
    TRANSACTION_clearLockFlag,
    TRANSACTION_readIMSI,
    TRANSACTION_writeIMSI,
    TRANSACTION_readOperatorName,
    TRANSACTION_readCTA,
    TRANSACTION_writeCTA,
    TRANSACTION_setRebootFlag,
    TRANSACTION_getLockType,
    TRANSACTION_getOperatorID,
    TRANSACTION_getOperatorName,
    TRANSACTION_isHangMoCallLocking,
    TRANSACTION_isHangMtCallLocking,
    TRANSACTION_clearRebootFlag,
    TRANSACTION_isBootRecoveryFlag,
    TRANSACTION_getUpgradeStatus,
    TRANSACTION_restartAndroid,
    TRANSACTION_isWipeSet,
    TRANSACTION_setWipeFlag,
    TRANSACTION_clearWipeFlag,
    TRANSACTION_readOtaResult,
    TRANSACTION_clearOtaResult,
};

class IDMAgent:public IInterface {
public:
    DECLARE_META_INTERFACE(DMAgent)
    virtual char * readDMTree(int & size)=0;
    virtual int writeDMTree(char* tree,int size)=0;
    virtual int  isLockFlagSet()=0;
    virtual int  setLockFlag(char *lockType, int len)=0;
    virtual int  clearLockFlag()=0;
    virtual char *  readIMSI(int & size)=0;
    virtual int writeIMSI (char * imsi, int size)=0;
    virtual char *  readCTA(int & size)=0;
    virtual int writeCTA (char * cta, int size)=0;
    virtual char * readOperatorName (int & size)=0;
    virtual int setRebootFlag()=0;    
    virtual int getLockType()=0;    
    virtual char * getOperatorName()=0;    
    virtual int getOperatorID()=0;    
    virtual int isHangMtCallLocking()=0;    
    virtual int isHangMoCallLocking()=0;    
    virtual int isBootRecoveryFlag()=0;    
    virtual int clearRebootFlag()=0;    
    virtual int getUpgradeStatus()=0;
    virtual int restartAndroid()=0;
    virtual int  isWipeSet()=0;
    virtual int  setWipeFlag(char *wipeType, int len)=0;
    virtual int  clearWipeFlag()=0;

    virtual int readOtaResult()=0;
    virtual int clearOtaResult()=0;
};

class BpDMAgent: public android::BpInterface<IDMAgent>
{
public:
    BpDMAgent(const android::sp<android::IBinder>& impl)
	: android::BpInterface<IDMAgent>(impl)
        {
        }
    char* readDMTree(int & size) {return 0;}
    int writeDMTree(char* tree,int size) {return 1;}
    int isLockFlagSet() {return 1;}
    int setLockFlag(char *lockType, int len) {return 1;}
    int clearLockFlag() {return 1;}
    char * readIMSI (int & size) {return 0;}
    int writeIMSI (char * imsi,int size) {return 0;}
    char * readCTA (int & size) {return 0;}
    int writeCTA (char * cta,int size) {return 0;}
    char * readOperatorName (int & size) {return 0;}


    int setRebootFlag() {return 1;}
    int getLockType() {return 0;};    
    char * getOperatorName() {return 0;};    
    int getOperatorID() {return 0;};  
    int isHangMtCallLocking() {return 0;};
    int isHangMoCallLocking() {return 0;};
    int isBootRecoveryFlag() {return 1;}
    int clearRebootFlag() {return 1;}
    int getUpgradeStatus() {return 1;}
    int restartAndroid(){return 1;}
    int isWipeSet() {return 1;}
    int setWipeFlag(char *wipeType, int len) {return 1;}
    int clearWipeFlag() {return 1;}

    int readOtaResult() { return 0; }
    int clearOtaResult() { return 0; }
};

class BnDMAgent : public BnInterface<IDMAgent>
{
public:
    status_t onTransact(uint32_t code,
			const Parcel &data,
			Parcel *reply,
			uint32_t flags);
    
};

class DMAgent : public BnDMAgent
{

public:
    static  void instantiate();
    DMAgent();
    ~DMAgent() {}
    virtual char* readDMTree(int & size);
    virtual int writeDMTree(char* tree,int size);
    virtual int isLockFlagSet();
    virtual int setLockFlag(char *lockType, int len);
    virtual int clearLockFlag();
    virtual char * readIMSI (int & size);
    virtual int writeIMSI (char * imsi,int size);
    virtual char * readCTA (int & size);
    virtual int writeCTA (char * cta,int size);
    virtual char * readOperatorName (int & size);

    virtual int setRebootFlag();
    int writeRebootFlash();
    virtual int getLockType();    
    virtual char * getOperatorName();    
    virtual int getOperatorID();    
    virtual int isHangMtCallLocking();    
    virtual int isHangMoCallLocking();  
    virtual int clearRebootFlag();
    virtual int isBootRecoveryFlag();
    virtual int getUpgradeStatus();
    int writeRebootFlash(char *rebootCmd);
    int setRecoveryCommand();
    char * readMiscPartition(int readSize);
    virtual int restartAndroid();

    virtual int isWipeSet();
    virtual int setWipeFlag(char *wipeType, int len);
    virtual int clearWipeFlag();

    virtual int readOtaResult();
    virtual int clearOtaResult();
};


IMPLEMENT_META_INTERFACE(DMAgent, "DMAgent")

