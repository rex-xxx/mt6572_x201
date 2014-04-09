/*
 * Copyright (C) 2008 The Android Open Source Project
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

#ifndef _VOLUMEMANAGER_H
#define _VOLUMEMANAGER_H

#include <pthread.h>

#ifdef __cplusplus
#include <utils/List.h>
#include <sysutils/SocketListener.h>
#include <cutils/properties.h>

#include "Volume.h"
#include "CFG_OMADMUSB_File.h"

/* The length of an MD5 hash when encoded into ASCII hex characters */
#define MD5_ASCII_LENGTH_PLUS_NULL ((MD5_DIGEST_LENGTH*2)+1)

#include <unistd.h>
#define EXT_SDCARD_TOOL "/data/ext_sdcard_tool"
extern int is_meta_boot(void) ;
extern void create_link_in_meta(const char *ext_sd_path) ;

typedef enum { ASEC, OBB } container_type_t;

class ContainerData {
public:
    ContainerData(char* _id, container_type_t _type)
            : id(_id)
            , type(_type)
    {}

    ~ContainerData() {
        if (id != NULL) {
            free(id);
            id = NULL;
        }
    }

    char *id;
    container_type_t type;
};

typedef android::List<ContainerData*> AsecIdCollection;

class VolumeManager {
private:
    static VolumeManager *sInstance;

private:
    SocketListener        *mBroadcaster;

    VolumeCollection      *mVolumes;
    AsecIdCollection      *mActiveContainers;
    bool				   mUseBackupContainers;

    bool                   mDebug;

    // for adjusting /proc/sys/vm/dirty_ratio when UMS is active
    int                    mUmsSharingCount;
    int                    mSavedDirtyRatio;
    int                    mUmsDirtyRatio;
    int                    mVolManagerDisabled;
    bool                   mIsFirstBoot;
    bool                   mIsHotPlug;

#ifdef MTK_2SDCARD_SWAP
    bool                   mIs2sdSwapped;
    bool                   bSdCardSwapBootComplete;
	bool						bNeedSwapAfterUnmount ;
	bool						bNeedSwapAfterMount ;
#endif

public:
    virtual ~VolumeManager();

    int start();
    int stop();

    void handleBlockEvent(NetlinkEvent *evt);

    int addVolume(Volume *v);

    int listVolumes(SocketClient *cli);
    int mountVolume(const char *label);
    int unmountVolume(const char *label, bool force, bool revert);
    int shareVolume(const char *label, const char *method);
    //M{
    int USBEnable(bool enable);
    static int NvramAccessForOMADM(OMADMUSB_CFG_Struct *st, bool isRead);
    //}M
    int unshareVolume(const char *label, const char *method);
    int shareEnabled(const char *path, const char *method, bool *enabled);
    int formatVolume(const char *label);
    void disableVolumeManager(void) { mVolManagerDisabled = 1; }

    /* ASEC */
    int listBackupAsec(SocketClient *cli);
    int waitForAfCleanupAsec(Volume *v);

    int findAsec(const char *id, char *asecPath = NULL, size_t asecPathLen = 0,
            const char **directory = NULL) const;
    int createAsec(const char *id, unsigned numSectors, const char *fstype,
                   const char *key, const int ownerUid, bool isExternal);
    int finalizeAsec(const char *id);

    /**
     * Fixes ASEC permissions on a filesystem that has owners and permissions.
     * This currently means EXT4-based ASEC containers.
     *
     * There is a single file that can be marked as "private" and will not have
     * world-readable permission. The group for that file will be set to the gid
     * supplied.
     *
     * Returns 0 on success.
     */
    int fixupAsecPermissions(const char *id, gid_t gid, const char* privateFilename);
    int destroyAsec(const char *id, bool force);
    int mountAsec(const char *id, const char *key, int ownerUid);
    int unmountAsec(const char *id, bool force);
    int renameAsec(const char *id1, const char *id2);
    int getAsecMountPath(const char *id, char *buffer, int maxlen);
    int getAsecFilesystemPath(const char *id, char *buffer, int maxlen);

    /* Loopback images */
    int listMountedObbs(SocketClient* cli);
    int mountObb(const char *fileName, const char *key, int ownerUid);
    int unmountObb(const char *fileName, bool force);
    int getObbMountPath(const char *id, char *buffer, int maxlen);

    Volume* getVolumeForFile(const char *fileName);

    /* Shared between ASEC and Loopback images */
    int unmountLoopImage(const char *containerId, const char *loopId,
            const char *fileName, const char *mountPoint, bool force);

    void setDebug(bool enable);

    // XXX: Post froyo this should be moved and cleaned up
    int cleanupAsec(Volume *v, bool force);

    void setBroadcaster(SocketListener *sl) { mBroadcaster = sl; }
    SocketListener *getBroadcaster() { return mBroadcaster; }

    static VolumeManager *Instance();

    static char *asecHash(const char *id, char *buffer, size_t len);

    Volume *lookupVolume(const char *label);
    int getNumDirectVolumes(void);
    int getDirectVolumeList(struct volume_info *vol_list);
    int unmountAllAsecsInDir(const char *directory);

    bool isFirstBoot() {return mIsFirstBoot;}
    bool getHotPlug() {return mIsHotPlug;}
    void setHotPlug(bool isPlug) { mIsHotPlug = isPlug;}


    void setIpoState(int state){mIpoState=state;}
    int getIpoState(void){return mIpoState;}
    static const int State_Ipo_Shutdown    = 1;
    static const int State_Ipo_Start       = 2;
    int reinitExternalSD(); /* to re-init external sd for the project without sd dection pin */

    
#ifdef MTK_SHARED_SDCARD
    void setSharedSdState(int state);
#endif

#ifdef MTK_2SDCARD_SWAP
	void rollbackToOnlyInternalSd();
	void swap2Sdcard();
	bool is2SdcardSwapped() {return mIs2sdSwapped;};
	void setNeedSwapAfterUnmount(bool value) { bNeedSwapAfterUnmount = value ;} ;
	bool getNeedSwapAfterUnmount(void) {return bNeedSwapAfterUnmount;} ;
	void setNeedSwapAfterMount(bool value) { bNeedSwapAfterMount = value ;} ;
	bool getNeedSwapAfterMount(void) {return bNeedSwapAfterMount;} ;
	void set2SdcardSwapped(bool isSwapped) {		
		char int_path[50] ;
		char ext_path[50] ;
		char swap_state_str[2];

		property_get("internal_sd_path", int_path, ""); 
		property_get("external_sd_path", ext_path, ""); 
                SLOGD("the property of internal storage before property_set... = %s", int_path);
                SLOGD("the property of external storage before property_set... = %s", ext_path);

        mIs2sdSwapped = isSwapped;
        sprintf(swap_state_str, "%d", mIs2sdSwapped);
        property_set("vold_swap_state", swap_state_str);

		if(mIs2sdSwapped) {
			property_set("internal_sd_path", "/storage/sdcard1");
			property_set("external_sd_path", "/storage/sdcard0");
		}
		else {
			property_set("internal_sd_path", "/storage/sdcard0");
			property_set("external_sd_path", "/storage/sdcard1");
		}
		
		if(mIs2sdSwapped) 
			create_link_in_meta("/storage/sdcard0");
		else
			create_link_in_meta("/storage/sdcard1");


		property_get("internal_sd_path", int_path, ""); 
		property_get("external_sd_path", ext_path, ""); 
                SLOGD("the property of internal storage after property_set... = %s", int_path);
                SLOGD("the property of external storage after property_set... = %s", ext_path);

	}
    
    void setSwapOnGoing(bool bIsSwapOnGoing) {		
        SLOGD("set is_swap_ongoing = %d", bIsSwapOnGoing);

		char swapongoing_str[2];
        sprintf(swapongoing_str, "%d", bIsSwapOnGoing);
        property_set("is_swap_ongoing", swapongoing_str);
        
        char result[10] ;
        property_get("is_swap_ongoing", result, ""); 
        SLOGD("get is_swap_ongoing = %s", result);
	}

#endif
	void mountallVolumes();
        bool isSomeVolumeShared();


private:
    VolumeManager();
    int mIpoState;
    void readInitialState();
    bool isMountpointMounted(const char *mp);
    bool isAsecInDirectory(const char *dir, const char *asec) const;
    char *replace(const char *src, char *token, char *target);
    int dirtyRatio();
};

extern "C" {
#endif /* __cplusplus */
#define UNMOUNT_NOT_MOUNTED_ERR -2
    int vold_disableVol(const char *label);
    int vold_getNumDirectVolumes(void);
    int vold_getDirectVolumeList(struct volume_info *v);
    int vold_unmountAllAsecs(void);
#ifdef __cplusplus
}
#endif

#endif
