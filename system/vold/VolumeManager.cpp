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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <fts.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mount.h>
#include <dirent.h>

#include <linux/kdev_t.h>

#define LOG_TAG "Vold"

#include <openssl/md5.h>

#include <cutils/log.h>

#include <sysutils/NetlinkEvent.h>

#include <private/android_filesystem_config.h>

#include "VolumeManager.h"
#include "DirectVolume.h"
#include "ResponseCode.h"
#include "Loop.h"
#include "Ext4.h"
#include "Fat.h"
#include "Devmapper.h"
#include "Process.h"
#include "Asec.h"
#include "cryptfs.h"
#include "libnvram.h"
#include <cutils/properties.h>

#undef LOG_TAG
#define LOG_TAG "VolumeManager"

#define NETLINK_DEBUG


#define MASS_STORAGE_FILE_PATH  "/sys/class/android_usb/android0/f_mass_storage/lun/file"

#ifdef MTK_SHARED_SDCARD
   #define MASS_STORAGE_EXTERNAL_FILE_PATH  "/sys/class/android_usb/android0/f_mass_storage/lun/file"
#else
   #define MASS_STORAGE_EXTERNAL_FILE_PATH  "/sys/class/android_usb/android0/f_mass_storage/lun1/file"
#endif

#define MEMINFO_PATH "/proc/meminfo"
#define DIRTY_RATIO_PATH "/proc/sys/vm/dirty_ratio"

extern int iFileOMADMUSBLID;


#ifdef MTK_2SDCARD_SWAP
const char* dv1_old_mountpoint;
const char* dv2_old_mountpoint;
#endif
VolumeManager *VolumeManager::sInstance = NULL;

VolumeManager *VolumeManager::Instance() {
    if (!sInstance)
        sInstance = new VolumeManager();
    return sInstance;
}

VolumeManager::VolumeManager() {
    mDebug = false;
    mVolumes = new VolumeCollection();
    mActiveContainers = new AsecIdCollection();
    mBroadcaster = NULL;
    mUmsSharingCount = 0;
    mSavedDirtyRatio = -1;
    mUmsDirtyRatio = dirtyRatio();
    mVolManagerDisabled = 0;

    char first_boot[PROPERTY_VALUE_MAX];
    property_get("persist.first_boot", first_boot, "1");

    mIsFirstBoot = false;
	if (!strcmp(first_boot, "1")) {
        SLOGI("*** This is first boot!");
        mIsFirstBoot = true;
        
        #ifdef MTK_SHARED_SDCARD
           SLOGI("*** MTK_SHARED_SDCARD is enabled. We don't need persist.first_boot for formatting the internal_sd");
           property_set("persist.first_boot", "0");
        #endif
    }
    mIpoState = State_Ipo_Start;
#ifdef MTK_2SDCARD_SWAP
	bSdCardSwapBootComplete = false;
    setSwapOnGoing(false) ;
#endif
}

VolumeManager::~VolumeManager() {
    delete mVolumes;
    delete mActiveContainers;
}

char *VolumeManager::asecHash(const char *id, char *buffer, size_t len) {
    static const char* digits = "0123456789abcdef";

    unsigned char sig[MD5_DIGEST_LENGTH];

    if (buffer == NULL) {
        SLOGE("Destination buffer is NULL");
        errno = ESPIPE;
        return NULL;
    } else if (id == NULL) {
        SLOGE("Source buffer is NULL");
        errno = ESPIPE;
        return NULL;
    } else if (len < MD5_ASCII_LENGTH_PLUS_NULL) {
        SLOGE("Target hash buffer size < %d bytes (%d)",
                MD5_ASCII_LENGTH_PLUS_NULL, len);
        errno = ESPIPE;
        return NULL;
    }

    MD5(reinterpret_cast<const unsigned char*>(id), strlen(id), sig);

    char *p = buffer;
    for (int i = 0; i < MD5_DIGEST_LENGTH; i++) {
        *p++ = digits[sig[i] >> 4];
        *p++ = digits[sig[i] & 0x0F];
    }
    *p = '\0';

    return buffer;
}

int VolumeManager::dirtyRatio() {
    int dirtyRatioVal = 0;
    int memTotalSize = 0;
    FILE *fp;

    if ((fp = fopen(MEMINFO_PATH, "r+"))) {
        char line[128];
        if (fgets(line, sizeof(line), fp)) {
            if (sscanf(line, "MemTotal: %d kB", &memTotalSize) == 1 && memTotalSize != 0) {
                /*
                 *  When RAM is 512MB, the dirty ratio as 20 is OK. We also want to spend the same time when flushing the cached
                 *  data under any condition. So under this logic, The bigger RAM, the smaller ratio should be used.
                 *  512MB * 20 = [ram size] * [IDEAL DIRTY RATIO] -> [IDEAL DIRTY RATIO] = 512MB * 20 / [ram size]
                 */
                dirtyRatioVal = (524288 * 20) / memTotalSize;
            }
        } else
            SLOGE("Failed to read %s (%s)", MEMINFO_PATH , strerror(errno));

        fclose(fp);
    } else {
        SLOGE("Failed to open %s (%s)", MEMINFO_PATH , strerror(errno));
    }

	return dirtyRatioVal;
}

void VolumeManager::setDebug(bool enable) {
    mDebug = enable;
    VolumeCollection::iterator it;
    for (it = mVolumes->begin(); it != mVolumes->end(); ++it) {
        (*it)->setDebug(enable);
    }
}

int VolumeManager::start() {
    return 0;
}

int VolumeManager::stop() {
    return 0;
}

int VolumeManager::addVolume(Volume *v) {
    mVolumes->push_back(v);
    return 0;
}

// ALPS00475374
bool isUsb_Otg(const char *dev_path)
{
   SLOGD("isUsb_Otg : dev_path = %s", dev_path) ;
   return (strstr(dev_path, "mt_usb") != NULL) ;
}
// ALPS00475374

extern bool rescan_part;
void VolumeManager::handleBlockEvent(NetlinkEvent *evt) {
    const char *devpath = evt->findParam("DEVPATH");
#ifdef MTK_2SDCARD_SWAP
    Volume *cfgVol2 = NULL;
    Volume *cfgVol1 = NULL;
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    int devNum1;
    int devNum2;
    int action = evt->getAction();
    int remember_to_swap = 0 ;
    bool just_update_status = false ;
#ifndef MTK_SHARED_SDCARD
    //We do not need unshare/reshare internal sd if SharedSD + SWAP are both enabled
    bool needShareInternalSdAgain = false ;
#endif    
#endif
    /* Lookup a volume to handle this device */
    VolumeCollection::iterator it;
    bool hit = false;
    setHotPlug(true);

    evt->dump();
    if (strcasestr(devpath, "boot") != NULL) {
        SLOGI("boot disk, skip!!!");
        goto out;
    }
    if (strcasestr(devpath, "rpmb") != NULL) {
        SLOGI("RPMB disk, skip!!!");
        goto out;
    }
#ifdef MTK_2SDCARD_SWAP
    if(bSdCardSwapBootComplete){
        cfgVol1 = lookupVolume("sdcard");
        cfgVol2 = lookupVolume("sdcard2");

	if(cfgVol1 == NULL)
		SLOGD("[ERR!!] In VolumeManager::handleBlockEvent -- label \"sdcard\" should exist in vold.fstab !!");
	if(cfgVol2 == NULL) 
		SLOGD("[ERR!!] In VolumeManager::handleBlockEvent -- label \"sdcard2\" should exist in vold.fstab !!");	
        
        devNum1 = cfgVol1->getDiskDevice();
        devNum2 = cfgVol2->getDiskDevice();

        //ALPS00475374
        if(isUsb_Otg(evt->findParam("DEVPATH"))) {
            SLOGD("[SWAP] This is a USB OTG!") ;
            just_update_status = true ;
            goto do_handleblk_event ;
        }
        //ALPS00475374

        
        /* Ignore uevent during formating. SD card with MBR will issue 'remove' and 'add' uevent while formatting in VOLD */
        if(cfgVol2->getState() != Volume::State_Formatting)
        {
#ifdef MTK_FAT_ON_NAND
            if((major == 7) || (major == 179))
#else
            if((MAJOR(devNum1) == major) || 
                    (MAJOR(devNum2) == major))
#endif
            {
                if (action == NetlinkEvent::NlActionAdd) {
                    const char *devtype = evt->findParam("DEVTYPE");
                    if(!strcmp(devtype, "disk")) {
                        SLOGD("[SWAP] It is a disk !\n") ;
                        int nparts = atoi(evt->findParam("NPARTS"));
                        SLOGD("[SWAP] %s : %d\n", __func__, nparts) ;
                        if(nparts != 0) {
                            //this is the card with at least one partition.
                            //we just need to check partition. 
                            SLOGD("[SWAP] this is the card with at least one partition, we just need to check partition.") ;
                            
                            just_update_status = true ;
                            goto do_handleblk_event ;
                        }
                    }
                    else if(!strcmp(devtype, "partition")){
                    	if(rescan_part){
                    			just_update_status = true;
                          goto do_handleblk_event;
											}
										}

                    SLOGD("[SWAP] cfgVol1->getState() = %d\n", cfgVol1->getState()) ;
                    if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Mounted)) {
                        SLOGD("[SWAP] The insert-card happens in normal mode.\n") ;
                        setSwapOnGoing(true) ;
                        cfgVol1->unmountVol(true, false);
                    }
#ifndef MTK_SHARED_SDCARD
                    //We do not need unshare/reshare internal sd if SharedSD + SWAP are both enabled
                    else if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Shared)) {
                        SLOGD("[SWAP] The insert-card happens in UMS mode.\n") ;
                        SLOGD("[SWAP] unshare internal card at first\n") ;
                        unshareVolume(cfgVol1->getLabel(), "ums") ;
                        needShareInternalSdAgain = true ;
                    }
#endif                    

                    if((cfgVol1->getState() != Volume::State_Mounted &&
                                cfgVol1->getState() != Volume::State_Unmounting &&
                                cfgVol1->getState() != Volume::State_Formatting &&
                                cfgVol1->getState() != Volume::State_Shared &&
                                cfgVol1->getState() != Volume::State_SharedMnt
                       )
                            && (cfgVol2->getState() != Volume::State_Mounted &&
                                cfgVol2->getState() != Volume::State_Unmounting &&
                                cfgVol2->getState() != Volume::State_Formatting &&
                                cfgVol2->getState() != Volume::State_Shared &&
                                cfgVol2->getState() != Volume::State_SharedMnt
                               )
                      )
                    {
                        SLOGD("swap path add");
                        SLOGD("[SWAP] cfgVol1 state = %d, cfgVol2 state = %d, Now swap mount point !\n", cfgVol1->getState(), cfgVol2->getState()) ;
                        cfgVol1->setMountpoint((char *)dv2_old_mountpoint);
                        cfgVol2->setMountpoint((char *)dv1_old_mountpoint);
			set2SdcardSwapped(true) ;
                        #ifdef MTK_SHARED_SDCARD
                            cfgVol1->unmountPath(dv1_old_mountpoint, true);
                            property_set("ctl.restart", "sdcard");
                        #endif    
                    }
                    else{
                        SLOGD("[skip swap add]cfgVol1:state[%d] cfgVol2:state[%d]", cfgVol1->getState(), cfgVol2->getState());
                    }
#ifndef MTK_SHARED_SDCARD
                    //We do not need unshare/reshare internal sd if SharedSD + SWAP are both enabled
                    if(needShareInternalSdAgain) {
                        SLOGD("[SWAP] We need to share internal-SD to PC again.\n") ;
                        needShareInternalSdAgain = false ;
                        shareVolume(cfgVol1->getLabel(), "ums") ;
                    }
#endif                    
                } else if (action == NetlinkEvent::NlActionRemove) {
                    const char *devtype = evt->findParam("DEVTYPE");
                    if(!strcmp(devtype, "partition")) {
                        SLOGD("[SWAP] It is a partition !\n") ;
                        //this is the card with at least one partition.
                        //we just need to check partition. 
                        SLOGD("[SWAP] this is the partition remove uenent, we just need to check disk.") ;
                        just_update_status = true ;
                        goto do_handleblk_event ;                        
                    }
                    if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Mounted) && mIs2sdSwapped) {
                        SLOGD("[SWAP] The remove-card happens in normal mode.\n") ;
                        setSwapOnGoing(true) ;
                        /*
                        SLOGD("[SWAP] Send VolumeEjectBeforeSwap") ;
                        char msg[255] ;
                        snprintf(msg, sizeof(msg), "Volume %s %s will get ejected before unmount", cfgVol1->getLabel(), cfgVol1->getMountpoint());
                        getBroadcaster()->sendBroadcast(ResponseCode::VolumeEjectBeforeSwap, msg, false);
                        */
                        cfgVol1->unmountVol(true, false);
                    }
#ifndef MTK_SHARED_SDCARD
                    //We do not need unshare/reshare internal sd if SharedSD + SWAP are both enabled
                    else if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Shared)) {
                        SLOGD("[SWAP] The remove-card happens in UMS mode.\n") ;
                        SLOGD("[SWAP] unshare internal card at first\n") ;
                        unshareVolume(cfgVol1->getLabel(), "ums") ;
                        needShareInternalSdAgain = true ;
                    }
#endif                    

                    if(mIs2sdSwapped)
                        remember_to_swap = 1 ;
                }
            }
        }
    }
#endif
do_handleblk_event :
    for (it = mVolumes->begin(); it != mVolumes->end(); ++it) {
        if (!(*it)->handleBlockEvent(evt)) {
#ifdef NETLINK_DEBUG
            SLOGD("Device '%s' event handled by volume %s\n", devpath, (*it)->getLabel());
#endif
            hit = true;
            break;
        }
    }
#ifdef MTK_2SDCARD_SWAP
    if(remember_to_swap) {
            SLOGD("[SWAP] remember to swap");
        if((cfgVol1->getState() == Volume::State_Idle || cfgVol1->getState() == Volume::State_NoMedia) &&
                (cfgVol2->getState() == Volume::State_Idle || cfgVol2->getState() == Volume::State_NoMedia)
                && mIs2sdSwapped)
        {
            SLOGD("[SWAP] swap mount point -- reset to the original path settings");

            /* At this moment, two volume are both in idle state, so we can directly change mount point */
                        cfgVol1->setMountpoint((char *)dv1_old_mountpoint);
            cfgVol2->setMountpoint((char *)dv2_old_mountpoint);
            set2SdcardSwapped(false) ;
#ifdef MTK_SHARED_SDCARD
              cfgVol1->unmountPath(dv2_old_mountpoint, true);
              property_set("ctl.restart", "sdcard");
#endif  

                    }
                    else{
                        SLOGD("[skip swap remove]cfgVol1:state[%d] cfgVol2:state[%d]", cfgVol1->getState(), cfgVol2->getState());
                    }
                }
#endif

out:
    if (!hit) {
#ifdef NETLINK_DEBUG
        SLOGW("No volumes handled block event for '%s'", devpath);
#endif
    }

#ifdef MTK_2SDCARD_SWAP
    if(bSdCardSwapBootComplete && !just_update_status){
        SLOGD("[SWAP] At last, we should remount internal sd.") ;

        cfgVol1 = lookupVolume("sdcard");
        cfgVol2 = lookupVolume("sdcard2");

	if(cfgVol1 == NULL) 
		SLOGD("[ERR!!] In VolumeManager::handleBlockEvent -- label \"sdcard\" should exist in vold.fstab !!");
	if(cfgVol2 == NULL) 
		SLOGD("[ERR!!] In VolumeManager::handleBlockEvent -- label \"sdcard2\" should exist in vold.fstab !!");	
        
        devNum1 = cfgVol1->getDiskDevice();
        devNum2 = cfgVol2->getDiskDevice();

        SLOGD("[SWAP] cfgVol2->getState() = %d\n", cfgVol2->getState()) ;
        if(cfgVol2->getState() != Volume::State_Formatting)
        {
#ifdef MTK_FAT_ON_NAND
            if((major == 7) || (major == 179))
#else
            if((MAJOR(devNum1) == major) || 
                    (MAJOR(devNum2) == major))
#endif
            {
                if (action == NetlinkEvent::NlActionAdd) {
                    SLOGD("[SWAP] The action is Insert-SD") ;
                    if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Idle)) 
                    {
                        SLOGD("[SWAP] After insert-SD, we need to mount internal-SD.\n") ;
                        cfgVol1->mountVol();
                    }
                } else if (action == NetlinkEvent::NlActionRemove) {
                    SLOGD("[SWAP] The action is Remove-SD") ;
                    if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Idle)) 
                    {
#ifndef MTK_SHARED_SDCARD
                        //We do not need unshare/reshare internal sd if SharedSD + SWAP are both enabled
                        if(needShareInternalSdAgain) 
                        {   
                            SLOGD("[SWAP] After remove sd, we need to share internal-SD to PC again, not mount\n") ;
                            needShareInternalSdAgain = false ;
                            shareVolume(cfgVol1->getLabel(), "ums") ;
                        }
                        else 
                        {
                            SLOGD("[SWAP] After remove sd, we need to mount internal-SD again, not share to PC\n") ;
                            cfgVol1->mountVol();
                        }
#else
                        SLOGD("[SWAP] After remove sd, we need to mount internal-SD again, not share to PC\n") ;
                        cfgVol1->mountVol();

#endif
                    }
                }
            }
        }
        SLOGD("[SWAP] %s() : SWAP finished.\n", __func__) ;
        setSwapOnGoing(false) ;
      }
#endif
     setHotPlug(false);
}

int VolumeManager::listVolumes(SocketClient *cli) {
    VolumeCollection::iterator i;

    for (i = mVolumes->begin(); i != mVolumes->end(); ++i) {
        char *buffer;
        #ifdef MTK_SHARED_SDCARD
        if ((*i)->IsEmmcStorage()) {
            continue;
        }
        #endif
 
        asprintf(&buffer, "%s %s %d",
                 (*i)->getLabel(), (*i)->getMountpoint(),
                 (*i)->getState());
        cli->sendMsg(ResponseCode::VolumeListResult, buffer, false);
        free(buffer);
    }
    cli->sendMsg(ResponseCode::CommandOkay, "Volumes listed.", false);
    return 0;
}

int VolumeManager::formatVolume(const char *label) {
    Volume *v = lookupVolume(label);

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    if (mVolManagerDisabled) {
        errno = EBUSY;
        return -1;
    }

    return v->formatVol();
}

int VolumeManager::getObbMountPath(const char *sourceFile, char *mountPath, int mountPathLen) {
    char idHash[33];
    if (!asecHash(sourceFile, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", sourceFile, strerror(errno));
        return -1;
    }

    memset(mountPath, 0, mountPathLen);
    snprintf(mountPath, mountPathLen, "%s/%s", Volume::LOOPDIR, idHash);

    if (access(mountPath, F_OK)) {
        errno = ENOENT;
        return -1;
    }

    return 0;
}

int VolumeManager::getAsecMountPath(const char *id, char *buffer, int maxlen) {
    char asecFileName[255];

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    memset(buffer, 0, maxlen);
    if (access(asecFileName, F_OK)) {
        errno = ENOENT;
        return -1;
    }

    snprintf(buffer, maxlen, "%s/%s", Volume::ASECDIR, id);
    return 0;
}

int VolumeManager::getAsecFilesystemPath(const char *id, char *buffer, int maxlen) {
    char asecFileName[255];

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    memset(buffer, 0, maxlen);
    if (access(asecFileName, F_OK)) {
        errno = ENOENT;
        return -1;
    }

    snprintf(buffer, maxlen, "%s", asecFileName);
    return 0;
}

int VolumeManager::createAsec(const char *id, unsigned int numSectors, const char *fstype,
        const char *key, const int ownerUid, bool isExternal) {
    struct asec_superblock sb;
    memset(&sb, 0, sizeof(sb));

    const bool wantFilesystem = strcmp(fstype, "none");
    bool usingExt4 = false;
    if (wantFilesystem) {
        usingExt4 = !strcmp(fstype, "ext4");
        if (usingExt4) {
            sb.c_opts |= ASEC_SB_C_OPTS_EXT4;
        } else if (strcmp(fstype, "fat")) {
            SLOGE("Invalid filesystem type %s", fstype);
            errno = EINVAL;
            return -1;
        }
    }

    sb.magic = ASEC_SB_MAGIC;
    sb.ver = ASEC_SB_VER;

    if (numSectors < ((1024*1024)/512)) {
        SLOGE("Invalid container size specified (%d sectors)", numSectors);
        errno = EINVAL;
        return -1;
    }

    if (lookupVolume(id)) {
        SLOGE("ASEC id '%s' currently exists", id);
        errno = EADDRINUSE;
        return -1;
    }

    char asecFileName[255];

    if (!findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("ASEC file '%s' currently exists - destroy it first! (%s)",
                asecFileName, strerror(errno));
        errno = EADDRINUSE;
        return -1;
    }

    const char *asecDir = isExternal ? Volume::SEC_ASECDIR_EXT : Volume::SEC_ASECDIR_INT;

    snprintf(asecFileName, sizeof(asecFileName), "%s/%s.asec", asecDir, id);

    if (!access(asecFileName, F_OK)) {
        SLOGE("ASEC file '%s' currently exists - destroy it first! (%s)",
                asecFileName, strerror(errno));
        errno = EADDRINUSE;
        return -1;
    }

    /*
     * Add some headroom
     */
    unsigned fatSize = (((numSectors * 4) / 512) + 1) * 2;
    unsigned numImgSectors = numSectors + fatSize + 2;

    if (numImgSectors % 63) {
        numImgSectors += (63 - (numImgSectors % 63));
    }

    if (usingExt4) {
       /* make_ext4fs has be modified by MTK. it will reserve extra 1 M for encryption */
       SLOGI("For ext4, need extra 1M for encryption");
       numImgSectors += 2048;
    }
    SLOGI("fatSize(%d), numImgSectors(%d)", fatSize, numImgSectors);


    // Add +1 for our superblock which is at the end
    if (Loop::createImageFile(asecFileName, numImgSectors + 1)) {
        SLOGE("ASEC image file creation failed (%s)", strerror(errno));
        return -1;
    }

    char idHash[33];
    if (!asecHash(id, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", id, strerror(errno));
        unlink(asecFileName);
        return -1;
    }

    char loopDevice[255];
    if (Loop::create(idHash, asecFileName, loopDevice, sizeof(loopDevice))) {
        SLOGE("ASEC loop device creation failed (%s)", strerror(errno));
        unlink(asecFileName);
        return -1;
    }

    char dmDevice[255];
    bool cleanupDm = false;

    if (strcmp(key, "none")) {
        // XXX: This is all we support for now
        sb.c_cipher = ASEC_SB_C_CIPHER_TWOFISH;
        if (Devmapper::create(idHash, loopDevice, key, numImgSectors, dmDevice,
                             sizeof(dmDevice))) {
            SLOGE("ASEC device mapping failed (%s)", strerror(errno));
            Loop::destroyByDevice(loopDevice);
            unlink(asecFileName);
            return -1;
        }
        cleanupDm = true;
    } else {
        sb.c_cipher = ASEC_SB_C_CIPHER_NONE;
        strcpy(dmDevice, loopDevice);
    }

    /*
     * Drop down the superblock at the end of the file
     */

    int sbfd = open(loopDevice, O_RDWR);
    if (sbfd < 0) {
        SLOGE("Failed to open new DM device for superblock write (%s)", strerror(errno));
        if (cleanupDm) {
            Devmapper::destroy(idHash);
        }
        Loop::destroyByDevice(loopDevice);
        unlink(asecFileName);
        return -1;
    }

    if (lseek(sbfd, (numImgSectors * 512), SEEK_SET) < 0) {
        close(sbfd);
        SLOGE("Failed to lseek for superblock (%s)", strerror(errno));
        if (cleanupDm) {
            Devmapper::destroy(idHash);
        }
        Loop::destroyByDevice(loopDevice);
        unlink(asecFileName);
        return -1;
    }

    if (write(sbfd, &sb, sizeof(sb)) != sizeof(sb)) {
        close(sbfd);
        SLOGE("Failed to write superblock (%s)", strerror(errno));
        if (cleanupDm) {
            Devmapper::destroy(idHash);
        }
        Loop::destroyByDevice(loopDevice);
        unlink(asecFileName);
        return -1;
    }
    close(sbfd);

    /*
     * The device mapper node needs to be created. Sometimes it takes a
     * while. Wait for up to 1 second. We could also inspect incoming uevents,
     * but that would take more effort.
     */
    int tries = 25;
    while (tries--) {
        if (!access(dmDevice, F_OK) || errno != ENOENT) {
            break;
        }
        usleep(40 * 1000);
    }

    if (wantFilesystem) {
        int formatStatus;
        if (usingExt4) {
            formatStatus = Ext4::format(dmDevice);
        } else {
            formatStatus = Fat::format(dmDevice, numImgSectors);
        }

        if (formatStatus < 0) {
            SLOGE("ASEC fs format failed (%s)", strerror(errno));
            if (cleanupDm) {
                Devmapper::destroy(idHash);
            }
            Loop::destroyByDevice(loopDevice);
            unlink(asecFileName);
            return -1;
        }

        char mountPoint[255];

        snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id);
        if (mkdir(mountPoint, 0000)) {
            if (errno != EEXIST) {
                SLOGE("Mountpoint creation failed (%s)", strerror(errno));
                if (cleanupDm) {
                    Devmapper::destroy(idHash);
                }
                Loop::destroyByDevice(loopDevice);
                unlink(asecFileName);
                return -1;
            }
        }

        int mountStatus;
        if (usingExt4) {
            mountStatus = Ext4::doMount(dmDevice, mountPoint, false, false, false);
        } else {
            mountStatus = Fat::doMount(dmDevice, mountPoint, false, false, false, ownerUid, 0, 0000,
                    false);
        }

        if (mountStatus) {
            SLOGE("ASEC FAT mount failed (%s)", strerror(errno));
            if (cleanupDm) {
                Devmapper::destroy(idHash);
            }
            Loop::destroyByDevice(loopDevice);
            unlink(asecFileName);
            return -1;
        }

        if (usingExt4) {
            int dirfd = open(mountPoint, O_DIRECTORY);
            if (dirfd >= 0) {
                if (fchown(dirfd, ownerUid, AID_SYSTEM)
                        || fchmod(dirfd, S_IRUSR | S_IWUSR | S_IXUSR | S_ISGID | S_IRGRP | S_IXGRP)) {
                    SLOGI("Cannot chown/chmod new ASEC mount point %s", mountPoint);
                }
                close(dirfd);
            }
        }
    } else {
        SLOGI("Created raw secure container %s (no filesystem)", id);
    }

    mActiveContainers->push_back(new ContainerData(strdup(id), ASEC));
    return 0;
}

int VolumeManager::finalizeAsec(const char *id) {
    char asecFileName[255];
    char loopDevice[255];
    char mountPoint[255];

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    char idHash[33];
    if (!asecHash(id, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", id, strerror(errno));
        return -1;
    }

    if (Loop::lookupActive(idHash, loopDevice, sizeof(loopDevice))) {
        SLOGE("Unable to finalize %s (%s)", id, strerror(errno));
        return -1;
    }

    unsigned int nr_sec = 0;
    struct asec_superblock sb;

    if (Loop::lookupInfo(loopDevice, &sb, &nr_sec)) {
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id);

    int result = 0;
    if (sb.c_opts & ASEC_SB_C_OPTS_EXT4) {
        result = Ext4::doMount(loopDevice, mountPoint, true, true, true);
    } else {
        result = Fat::doMount(loopDevice, mountPoint, true, true, true, 0, 0, 0227, false);
    }

    if (result) {
        SLOGE("ASEC finalize mount failed (%s)", strerror(errno));
        return -1;
    }

    if (mDebug) {
        SLOGD("ASEC %s finalized", id);
    }
    return 0;
}

int VolumeManager::fixupAsecPermissions(const char *id, gid_t gid, const char* filename) {
    char asecFileName[255];
    char loopDevice[255];
    char mountPoint[255];

    if (gid < AID_APP) {
        SLOGE("Group ID is not in application range");
        return -1;
    }

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    char idHash[33];
    if (!asecHash(id, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", id, strerror(errno));
        return -1;
    }

    if (Loop::lookupActive(idHash, loopDevice, sizeof(loopDevice))) {
        SLOGE("Unable fix permissions during lookup on %s (%s)", id, strerror(errno));
        return -1;
    }

    unsigned int nr_sec = 0;
    struct asec_superblock sb;

    if (Loop::lookupInfo(loopDevice, &sb, &nr_sec)) {
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id);

    int result = 0;
    if ((sb.c_opts & ASEC_SB_C_OPTS_EXT4) == 0) {
        return 0;
    }

    int ret = Ext4::doMount(loopDevice, mountPoint,
            false /* read-only */,
            true  /* remount */,
            false /* executable */);
    if (ret) {
        SLOGE("Unable remount to fix permissions for %s (%s)", id, strerror(errno));
        return -1;
    }

    char *paths[] = { mountPoint, NULL };

    FTS *fts = fts_open(paths, FTS_PHYSICAL | FTS_NOCHDIR | FTS_XDEV, NULL);
    if (fts) {
        // Traverse the entire hierarchy and chown to system UID.
        for (FTSENT *ftsent = fts_read(fts); ftsent != NULL; ftsent = fts_read(fts)) {
            // We don't care about the lost+found directory.
            if (!strcmp(ftsent->fts_name, "lost+found")) {
                continue;
            }

            /*
             * There can only be one file marked as private right now.
             * This should be more robust, but it satisfies the requirements
             * we have for right now.
             */
            const bool privateFile = !strcmp(ftsent->fts_name, filename);

            int fd = open(ftsent->fts_accpath, O_NOFOLLOW);
            if (fd < 0) {
                SLOGE("Couldn't open file %s: %s", ftsent->fts_accpath, strerror(errno));
                result = -1;
                continue;
            }

            result |= fchown(fd, AID_SYSTEM, privateFile? gid : AID_SYSTEM);

            if (ftsent->fts_info & FTS_D) {
                result |= fchmod(fd, 0755);
            } else if (ftsent->fts_info & FTS_F) {
                result |= fchmod(fd, privateFile ? 0640 : 0644);
            }
            close(fd);
        }
        fts_close(fts);

        // Finally make the directory readable by everyone.
        int dirfd = open(mountPoint, O_DIRECTORY);
        if (dirfd < 0 || fchmod(dirfd, 0755)) {
            SLOGE("Couldn't change owner of existing directory %s: %s", mountPoint, strerror(errno));
            result |= -1;
        }
        close(dirfd);
    } else {
        result |= -1;
    }

    result |= Ext4::doMount(loopDevice, mountPoint,
            true /* read-only */,
            true /* remount */,
            true /* execute */);

    if (result) {
        SLOGE("ASEC fix permissions failed (%s)", strerror(errno));
        return -1;
    }

    if (mDebug) {
        SLOGD("ASEC %s permissions fixed", id);
    }
    return 0;
}

int VolumeManager::renameAsec(const char *id1, const char *id2) {
    char asecFilename1[255];
    char *asecFilename2;
    char mountPoint[255];

    const char *dir;

    if (findAsec(id1, asecFilename1, sizeof(asecFilename1), &dir)) {
        SLOGE("Couldn't find ASEC %s", id1);
        return -1;
    }

    asprintf(&asecFilename2, "%s/%s.asec", dir, id2);

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id1);
    if (isMountpointMounted(mountPoint)) {
        SLOGW("Rename attempt when src mounted");
        errno = EBUSY;
        goto out_err;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id2);
    if (isMountpointMounted(mountPoint)) {
        SLOGW("Rename attempt when dst mounted");
        errno = EBUSY;
        goto out_err;
    }

    if (!access(asecFilename2, F_OK)) {
        SLOGE("Rename attempt when dst exists");
        errno = EADDRINUSE;
        goto out_err;
    }

    if (rename(asecFilename1, asecFilename2)) {
        SLOGE("Rename of '%s' to '%s' failed (%s)", asecFilename1, asecFilename2, strerror(errno));
        goto out_err;
    }

    free(asecFilename2);
    return 0;

out_err:
    free(asecFilename2);
    return -1;
}

#define UNMOUNT_RETRIES 5
#define UNMOUNT_SLEEP_BETWEEN_RETRY_MS (1000 * 1000)


int VolumeManager::listBackupAsec(SocketClient *cli) {
   if (!mUseBackupContainers) {
       SLOGD("%s: Don't use backup Asec list", __func__);
       return (-1);
   }

   AsecIdCollection::iterator it;
   for (it = mActiveContainers->begin(); it != mActiveContainers->end(); ++it) {
         ContainerData* cd = *it;
         cli->sendMsg(ResponseCode::AsecListResult, cd->id, false);
   }
   return 0;
}


#define UNMOUNT_MSG_LAG_IN_TRY 15
int VolumeManager::waitForAfCleanupAsec(Volume *v) {
    const char* externalStorage = getenv("EXTERNAL_STORAGE");
    bool primaryStorage = externalStorage && !strcmp(v->getMountpoint(), externalStorage);

    if (!primaryStorage) {
        SLOGD("External sd card, skip waitForAfCleanupAsec()");
        return 0;
    } else {
        SLOGD("Primary sd card, do waitForAfCleanupAsec()");      
    }         

   if (mActiveContainers->size() == 0) {
       SLOGD("%s: No ASEC", __func__);
       return 0;
   }

   SLOGD("%s: Start to wait for AF to cleanup ASEC", __func__);

   mUseBackupContainers = true;
   int timeout = mActiveContainers->size() * UNMOUNT_RETRIES + UNMOUNT_MSG_LAG_IN_TRY;
   for(int i = 0; i < timeout; i++ ) {
         if(mActiveContainers->size() == 0) {
            SLOGI("%s: Success: wait for AF to cleanup ASEC", __func__);
            mUseBackupContainers = false;
            return 0;
         }
         usleep(1000*1000);
   }
   SLOGE("%s: FAIL: wait for AF to cleanup ASEC", __func__);
   mUseBackupContainers = false;
   return (-2);
}

int VolumeManager::unmountAsec(const char *id, bool force) {
    char asecFileName[255];
    char mountPoint[255];

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id);

    char idHash[33];
    if (!asecHash(id, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", id, strerror(errno));
        return -1;
    }

    return unmountLoopImage(id, idHash, asecFileName, mountPoint, force);
}

int VolumeManager::unmountObb(const char *fileName, bool force) {
    char mountPoint[255];

    char idHash[33];
    if (!asecHash(fileName, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", fileName, strerror(errno));
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::LOOPDIR, idHash);

    return unmountLoopImage(fileName, idHash, fileName, mountPoint, force);
}

int VolumeManager::unmountLoopImage(const char *id, const char *idHash,
        const char *fileName, const char *mountPoint, bool force) {
    if (!isMountpointMounted(mountPoint)) {
        SLOGE("Unmount request for %s when not mounted", id);
        errno = ENOENT;
        return -1;
    }

    int i, rc;
    for (i = 1; i <= UNMOUNT_RETRIES; i++) {
        rc = umount(mountPoint);
        if (!rc) {
            break;
        }
        if (rc && (errno == EINVAL || errno == ENOENT)) {
            SLOGI("Container %s unmounted OK", id);
            rc = 0;
            break;
        }
        SLOGW("%s unmount attempt %d failed (%s)",
              id, i, strerror(errno));

        int action = 0; // default is to just complain

        if (force) {
            if (i > (UNMOUNT_RETRIES - 2))
                action = 2; // SIGKILL
            else if (i > (UNMOUNT_RETRIES - 3))
                action = 1; // SIGHUP
        }

        Process::killProcessesWithOpenFiles(mountPoint, action);
        usleep(UNMOUNT_SLEEP_BETWEEN_RETRY_MS);
    }

    if (rc) {
        errno = EBUSY;
        SLOGE("Failed to unmount container %s (%s)", id, strerror(errno));
		Process::FindProcessesWithOpenFiles(mountPoint);
        return -1;
    }

    int retries = 10;

    while(retries--) {
        if (!rmdir(mountPoint)) {
            break;
        }

        SLOGW("Failed to rmdir %s (%s)", mountPoint, strerror(errno));
        usleep(UNMOUNT_SLEEP_BETWEEN_RETRY_MS);
    }

    if (!retries) {
        SLOGE("Timed out trying to rmdir %s (%s)", mountPoint, strerror(errno));
    }

    if (Devmapper::destroy(idHash) && errno != ENXIO) {
        SLOGE("Failed to destroy devmapper instance (%s)", strerror(errno));
    }

    char loopDevice[255];
    if (!Loop::lookupActive(idHash, loopDevice, sizeof(loopDevice))) {
        Loop::destroyByDevice(loopDevice);
    } else {
        SLOGW("Failed to find loop device for {%s} (%s)", fileName, strerror(errno));
    }

    AsecIdCollection::iterator it;
    for (it = mActiveContainers->begin(); it != mActiveContainers->end(); ++it) {
        ContainerData* cd = *it;
        if (!strcmp(cd->id, id)) {
            free(*it);
            mActiveContainers->erase(it);
            break;
        }
    }
    if (it == mActiveContainers->end()) {
        SLOGW("mActiveContainers is inconsistent!");
    }
    return 0;
}

int VolumeManager::destroyAsec(const char *id, bool force) {
    char asecFileName[255];
    char mountPoint[255];

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id);

    if (isMountpointMounted(mountPoint)) {
        if (mDebug) {
            SLOGD("Unmounting container before destroy");
        }
        if (unmountAsec(id, force)) {
            SLOGE("Failed to unmount asec %s for destroy (%s)", id, strerror(errno));
            return -1;
        }
    }

    if (unlink(asecFileName)) {
        SLOGE("Failed to unlink asec '%s' (%s)", asecFileName, strerror(errno));
        return -1;
    }

    if (mDebug) {
        SLOGD("ASEC %s destroyed", id);
    }
    return 0;
}

bool VolumeManager::isAsecInDirectory(const char *dir, const char *asecName) const {
    int dirfd = open(dir, O_DIRECTORY);
    if (dirfd < 0) {
        SLOGE("Couldn't open internal ASEC dir (%s)", strerror(errno));
        return -1;
    }

    bool ret = false;

    if (!faccessat(dirfd, asecName, F_OK, AT_SYMLINK_NOFOLLOW)) {
        ret = true;
    }

    close(dirfd);

    return ret;
}

int VolumeManager::findAsec(const char *id, char *asecPath, size_t asecPathLen,
        const char **directory) const {
    int dirfd, fd;
    const int idLen = strlen(id);
    char *asecName;

    if (asprintf(&asecName, "%s.asec", id) < 0) {
        SLOGE("Couldn't allocate string to write ASEC name");
        return -1;
    }

    const char *dir;
    if (isAsecInDirectory(Volume::SEC_ASECDIR_INT, asecName)) {
        dir = Volume::SEC_ASECDIR_INT;
    } else if (isAsecInDirectory(Volume::SEC_ASECDIR_EXT, asecName)) {
        dir = Volume::SEC_ASECDIR_EXT;
    } else {
        free(asecName);
        return -1;
    }

    if (directory != NULL) {
        *directory = dir;
    }

    if (asecPath != NULL) {
        int written = snprintf(asecPath, asecPathLen, "%s/%s", dir, asecName);
        if (written < 0 || static_cast<size_t>(written) >= asecPathLen) {
            free(asecName);
            return -1;
        }
    }

    free(asecName);
    return 0;
}

int VolumeManager::mountAsec(const char *id, const char *key, int ownerUid) {
    char asecFileName[255];
    char mountPoint[255];

    if (findAsec(id, asecFileName, sizeof(asecFileName))) {
        SLOGE("Couldn't find ASEC %s", id);
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::ASECDIR, id);

    if (isMountpointMounted(mountPoint)) {
        SLOGE("ASEC %s already mounted", id);
        errno = EBUSY;
        return -1;
    }

    char idHash[33];
    if (!asecHash(id, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", id, strerror(errno));
        return -1;
    }

    char loopDevice[255];
    if (Loop::lookupActive(idHash, loopDevice, sizeof(loopDevice))) {
        if (Loop::create(idHash, asecFileName, loopDevice, sizeof(loopDevice))) {
            SLOGE("ASEC loop device creation failed (%s)", strerror(errno));
            return -1;
        }
        if (mDebug) {
            SLOGD("New loop device created at %s", loopDevice);
        }
    } else {
        if (mDebug) {
            SLOGD("Found active loopback for %s at %s", asecFileName, loopDevice);
        }
    }

    char dmDevice[255];
    bool cleanupDm = false;
    int fd;
    unsigned int nr_sec = 0;
    struct asec_superblock sb;

    if (Loop::lookupInfo(loopDevice, &sb, &nr_sec)) {
        return -1;
    }

    if (mDebug) {
        SLOGD("Container sb magic/ver (%.8x/%.2x)", sb.magic, sb.ver);
    }
    if (sb.magic != ASEC_SB_MAGIC || sb.ver != ASEC_SB_VER) {
        SLOGE("Bad container magic/version (%.8x/%.2x)", sb.magic, sb.ver);
        Loop::destroyByDevice(loopDevice);
        errno = EMEDIUMTYPE;
        return -1;
    }
    nr_sec--; // We don't want the devmapping to extend onto our superblock

    if (strcmp(key, "none")) {
        if (Devmapper::lookupActive(idHash, dmDevice, sizeof(dmDevice))) {
            if (Devmapper::create(idHash, loopDevice, key, nr_sec,
                                  dmDevice, sizeof(dmDevice))) {
                SLOGE("ASEC device mapping failed (%s)", strerror(errno));
                Loop::destroyByDevice(loopDevice);
                return -1;
            }
            if (mDebug) {
                SLOGD("New devmapper instance created at %s", dmDevice);
            }
        } else {
            if (mDebug) {
                SLOGD("Found active devmapper for %s at %s", asecFileName, dmDevice);
            }
        }
        cleanupDm = true;
    } else {
        strcpy(dmDevice, loopDevice);
    }

    if (mkdir(mountPoint, 0000)) {
        if (errno != EEXIST) {
            SLOGE("Mountpoint creation failed (%s)", strerror(errno));
            if (cleanupDm) {
                Devmapper::destroy(idHash);
            }
            Loop::destroyByDevice(loopDevice);
            return -1;
        }
    }

    /*
     * The device mapper node needs to be created. Sometimes it takes a
     * while. Wait for up to 1 second. We could also inspect incoming uevents,
     * but that would take more effort.
     */
    int tries = 25;
    while (tries--) {
        if (!access(dmDevice, F_OK) || errno != ENOENT) {
            break;
        }
        usleep(40 * 1000);
    }

    int result;
    if (sb.c_opts & ASEC_SB_C_OPTS_EXT4) {
        result = Ext4::doMount(dmDevice, mountPoint, true, false, true);
    } else {
        result = Fat::doMount(dmDevice, mountPoint, true, false, true, ownerUid, 0, 0222, false);
    }

    if (result) {
        SLOGE("ASEC mount failed (%s)", strerror(errno));
        if (cleanupDm) {
            Devmapper::destroy(idHash);
        }
        Loop::destroyByDevice(loopDevice);
        return -1;
    }

    mActiveContainers->push_back(new ContainerData(strdup(id), ASEC));
    if (mDebug) {
        SLOGD("ASEC %s mounted", id);
    }
    return 0;
}

Volume* VolumeManager::getVolumeForFile(const char *fileName) {
    VolumeCollection::iterator i;

    for (i = mVolumes->begin(); i != mVolumes->end(); ++i) {
        const char* mountPoint = (*i)->getMountpoint();
        if (!strncmp(fileName, mountPoint, strlen(mountPoint))) {
            return *i;
        }
    }

    return NULL;
}

/**
 * Mounts an image file <code>img</code>.
 */
int VolumeManager::mountObb(const char *img, const char *key, int ownerGid) {
    char mountPoint[255];

    char idHash[33];
    if (!asecHash(img, idHash, sizeof(idHash))) {
        SLOGE("Hash of '%s' failed (%s)", img, strerror(errno));
        return -1;
    }

    snprintf(mountPoint, sizeof(mountPoint), "%s/%s", Volume::LOOPDIR, idHash);

    if (isMountpointMounted(mountPoint)) {
        SLOGE("Image %s already mounted", img);
        errno = EBUSY;
        return -1;
    }

    char loopDevice[255];
    if (Loop::lookupActive(idHash, loopDevice, sizeof(loopDevice))) {
        if (Loop::create(idHash, img, loopDevice, sizeof(loopDevice))) {
            SLOGE("Image loop device creation failed (%s)", strerror(errno));
            return -1;
        }
        if (mDebug) {
            SLOGD("New loop device created at %s", loopDevice);
        }
    } else {
        if (mDebug) {
            SLOGD("Found active loopback for %s at %s", img, loopDevice);
        }
    }

    char dmDevice[255];
    bool cleanupDm = false;
    int fd;
    unsigned int nr_sec = 0;

    if ((fd = open(loopDevice, O_RDWR)) < 0) {
        SLOGE("Failed to open loopdevice (%s)", strerror(errno));
        Loop::destroyByDevice(loopDevice);
        return -1;
    }

    if (ioctl(fd, BLKGETSIZE, &nr_sec)) {
        SLOGE("Failed to get loop size (%s)", strerror(errno));
        Loop::destroyByDevice(loopDevice);
        close(fd);
        return -1;
    }

    close(fd);

    if (strcmp(key, "none")) {
        if (Devmapper::lookupActive(idHash, dmDevice, sizeof(dmDevice))) {
            if (Devmapper::create(idHash, loopDevice, key, nr_sec,
                                  dmDevice, sizeof(dmDevice))) {
                SLOGE("ASEC device mapping failed (%s)", strerror(errno));
                Loop::destroyByDevice(loopDevice);
                return -1;
            }
            if (mDebug) {
                SLOGD("New devmapper instance created at %s", dmDevice);
            }
        } else {
            if (mDebug) {
                SLOGD("Found active devmapper for %s at %s", img, dmDevice);
            }
        }
        cleanupDm = true;
    } else {
        strcpy(dmDevice, loopDevice);
    }

    if (mkdir(mountPoint, 0755)) {
        if (errno != EEXIST) {
            SLOGE("Mountpoint creation failed (%s)", strerror(errno));
            if (cleanupDm) {
                Devmapper::destroy(idHash);
            }
            Loop::destroyByDevice(loopDevice);
            return -1;
        }
    }

    if (Fat::doMount(dmDevice, mountPoint, true, false, true, 0, ownerGid,
                     0227, false)) {
        SLOGE("Image mount failed (%s)", strerror(errno));
        if (cleanupDm) {
            Devmapper::destroy(idHash);
        }
        Loop::destroyByDevice(loopDevice);
        return -1;
    }

    mActiveContainers->push_back(new ContainerData(strdup(img), OBB));
    if (mDebug) {
        SLOGD("Image %s mounted", img);
    }
    return 0;
}

int VolumeManager::mountVolume(const char *label) {
#ifndef MTK_2SDCARD_SWAP	
    Volume *v = lookupVolume(label);

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    return v->mountVol();
#else
Volume *v = lookupVolume(label);
        int result ;

        SLOGW("VolumeManager::mountVolume -- SWAP enabled") ;

    if (!v) {
        errno = ENOENT;
        return -1;
    }

        SLOGW("VolumeManager::mountVolume -- SWAP enabled %d", getNeedSwapAfterMount()) ;
    if(getNeedSwapAfterMount()) {
                SLOGW("VolumeManager::mountVolume -- getNeedSwapAfterMount") ;
                //Does this time unmountVolume() try to unmount external - sd ? 
                if(v->isExternalSD()) {
                        SLOGW("VolumeManager::mountVolume -- it means that we will mount external sd") ;
                        Volume *external_sd = v ; 
                        Volume *phone_storage = lookupVolume("sdcard");

                        //if phone_storage is mounted  &&
                        //   phone_Stroage is mounted on /mnt/sdcard2
                        if((phone_storage->getState() == Volume::State_Mounted) &&              
                          (!strcmp(phone_storage->getMountpoint(), dv1_old_mountpoint)) 
                        ) { 
                                //Unmount 
                                SLOGW("VolumeManager::mountVolume -- unmount phone storage") ;
                                phone_storage->unmountVol(true, false) ;

                                //SWAP mount point again
                                SLOGW("VolumeManager::mountVolume -- set mountpoint") ;
                                phone_storage->setMountpoint((char*) dv2_old_mountpoint) ;
                                external_sd->setMountpoint((char*) dv1_old_mountpoint) ;
                                //Remount phone_storage to /mnt/sdcard
                                SLOGW("VolumeManager::mountVolume -- mount phone storage") ;                                                                                                   
                                set2SdcardSwapped(true) ;
             
                if(phone_storage->getState() == Volume::State_Idle) {
                       #ifdef MTK_SHARED_SDCARD
                            phone_storage->unmountPath(dv1_old_mountpoint, true);             
                            property_set("ctl.restart", "sdcard");
                       #endif  
                       result = phone_storage->mountVol();  
                }   
                        }   

                        if(phone_storage->getState() != Volume::State_Mounted)
                                SLOGW("VolumeManager::mountVolume -- phone_storage is note mounted(state = %d)", phone_storage->getState()) ;                       
                        if(strcmp(phone_storage->getMountpoint(), dv1_old_mountpoint)) 
                                SLOGW("VolumeManager::mountVolume -- phone_storage\' mount point is %s, not %s", phone_storage->getMountpoint(), dv1_old_mountpoint) ;                                          
                }   
                  
                setNeedSwapAfterMount(false) ;
        }   
                 
    return v->mountVol() ;
#endif
}

int VolumeManager::listMountedObbs(SocketClient* cli) {
    char device[256];
    char mount_path[256];
    char rest[256];
    FILE *fp;
    char line[1024];

    if (!(fp = fopen("/proc/mounts", "r"))) {
        SLOGE("Error opening /proc/mounts (%s)", strerror(errno));
        return -1;
    }

    // Create a string to compare against that has a trailing slash
    int loopDirLen = strlen(Volume::LOOPDIR);
    char loopDir[loopDirLen + 2];
    strcpy(loopDir, Volume::LOOPDIR);
    loopDir[loopDirLen++] = '/';
    loopDir[loopDirLen] = '\0';

    while(fgets(line, sizeof(line), fp)) {
        line[strlen(line)-1] = '\0';

        /*
         * Should look like:
         * /dev/block/loop0 /mnt/obb/fc99df1323fd36424f864dcb76b76d65 ...
         */
        sscanf(line, "%255s %255s %255s\n", device, mount_path, rest);

        if (!strncmp(mount_path, loopDir, loopDirLen)) {
            int fd = open(device, O_RDONLY);
            if (fd >= 0) {
                struct loop_info64 li;
                if (ioctl(fd, LOOP_GET_STATUS64, &li) >= 0) {
                    cli->sendMsg(ResponseCode::AsecListResult,
                            (const char*) li.lo_file_name, false);
                }
                close(fd);
            }
        }
    }

    fclose(fp);
    return 0;
}

int VolumeManager::shareEnabled(const char *label, const char *method, bool *enabled) {
    Volume *v = lookupVolume(label);

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    if (strcmp(method, "ums")) {
        errno = ENOSYS;
        return -1;
    }

    if (v->getState() != Volume::State_Shared) {
        *enabled = false;
    } else {
        *enabled = true;
    }
    return 0;
}

//M{
int VolumeManager::USBEnable(bool enable) {
    const bool READFILE = true;
	const bool WRITEFILE = false;
	int fd;
	int nvram_fd;
	int enableInt;
	int one = 1, zero = 0;
	char value[20];
	char Rvalue[20];
	int count;
	int Ret = 0;
	int rec_size;
	int rec_num;
	int file_lid = iFileOMADMUSBLID;
	
	OMADMUSB_CFG_Struct mStGet;
	memset(&mStGet, 0, sizeof(OMADMUSB_CFG_Struct));

	Ret = NvramAccessForOMADM(&mStGet, READFILE);
	if (Ret < 0) {
		return -1;//error log already print
	}
	//nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, true);//read NVRAM
    //Ret = read(nvram_fd, &mStGet, rec_size*rec_num);
    //NVM_CloseFileDesc(nvram_fd);
	SLOGD("OMADM NVRAM read  Ret=%d, IsEnable=%d, Usb=%d, Adb=%d, Rndis=%d", Ret, mStGet.iIsEnable, mStGet.iUsb, mStGet.iAdb, mStGet.iRndis);
	
	if (enable && 0==mStGet.iIsEnable) 
	{
		SLOGD("OMADM USB Enable");
		mStGet.iIsEnable = 1;
		Ret = NvramAccessForOMADM(&mStGet, WRITEFILE);
		if (Ret < 0) {
			return -1;//error log already print
		}
		//nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, false);
		//Ret = write(nvram_fd, &mStGet, rec_size*rec_num);
	    //NVM_CloseFileDesc(nvram_fd);
		SLOGD("OMADM NVRAM write  Ret=%d, IsEnable=%d, Usb=%d, Adb=%d, Rndis=%d", Ret, mStGet.iIsEnable, mStGet.iUsb, mStGet.iAdb, mStGet.iRndis);

		if ((fd = open("/sys/devices/platform/mt_usb/cmode", O_WRONLY)) < 0) {
			SLOGE("Unable to open /sys/devices/platform/mt_usb/cmode");
			return -1;
	    }
		count = snprintf(value, sizeof(value), "%d\n", mStGet.iIsEnable);
	    Ret = write(fd, value, count);
	    close(fd);
		if (Ret < 0) {
			SLOGE("Unable to write /sys/devices/platform/mt_usb/cmode");
			return -1;
		}
        return 0;
	}
	else if(!enable && 1==mStGet.iIsEnable)
	{		
		SLOGD("OMADM USB Disable");
		mStGet.iIsEnable = 0;
		int RCount = snprintf(Rvalue, sizeof(Rvalue), "%d\n", 0);
    	
        Ret = NvramAccessForOMADM(&mStGet, WRITEFILE);
		if (Ret < 0) {
			return -1;//error log already print
		}

		if ((fd = open("/sys/devices/platform/mt_usb/cmode", O_WRONLY)) < 0) {
			SLOGE("Unable to open /sys/devices/platform/mt_usb/cmode");
			return -1;
	    }
		count = snprintf(value, sizeof(value), "%d\n", mStGet.iIsEnable);
	    Ret = write(fd, value, count);
	    close(fd);
		if (Ret < 0) {
			SLOGE("Unable to write /sys/devices/platform/mt_usb/cmode");
			return -1;
		}
		return 0;
	}
	else
	{
		return 0;
	}
}

int VolumeManager::NvramAccessForOMADM(OMADMUSB_CFG_Struct *st, bool isRead) {
	if (!st) return -1;
	
	int file_lid = iFileOMADMUSBLID;
	F_ID nvram_fid;
	int rec_size;
	int rec_num;
	int Ret = 0;
	
	nvram_fid = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, isRead);
	if (nvram_fid.iFileDesc < 0) {
		SLOGE("Unable to open NVRAM file!");
		return -1;
	}
	
	if (isRead)	Ret = read(nvram_fid.iFileDesc, st, rec_size*rec_num);//read NVRAM
	else		Ret = write(nvram_fid.iFileDesc, st, rec_size*rec_num);//write NVRAM
	
	if (Ret < 0) {
		SLOGE("access NVRAM error!");
		return -1;
	}

	NVM_CloseFileDesc(nvram_fid);
	SLOGD("read st  nvram_fd=%d   Ret=%d  usb=%d  adb=%d  rndis=%d  rec_size=%d  rec_num=%d", nvram_fid.iFileDesc, Ret, st->iUsb, st->iAdb, st->iRndis, rec_size, rec_num);

	return 0;
}
//}M

int VolumeManager::shareVolume(const char *label, const char *method) {
    Volume *v = lookupVolume(label);

    SLOGD("VolumeManager::shareVolume , label = %s, method = %s\n", label, method) ;

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    const char* externalStorage = getenv("EXTERNAL_STORAGE");
    bool primaryStorage = externalStorage && !strcmp(v->getMountpoint(), externalStorage);


    /*
     * Eventually, we'll want to support additional share back-ends,
     * some of which may work while the media is mounted. For now,
     * we just support UMS
     */
    if (strcmp(method, "ums")) {
        errno = ENOSYS;
        return -1;
    }

    if (v->getState() == Volume::State_NoMedia) {
        errno = ENODEV;
        return -1;
    }

    if (v->getState() != Volume::State_Idle) {
        // You need to unmount manually befoe sharing
        errno = EBUSY;
        return -1;
    }

    if (mVolManagerDisabled) {
        errno = EBUSY;
        return -1;
    }

    dev_t d = v->getShareDevice();
    if ((MAJOR(d) == 0) && (MINOR(d) == 0)) {
        // This volume does not support raw disk access
        errno = EINVAL;
        return -1;
    }

    int fd;
    char nodepath[255];
    snprintf(nodepath,
             sizeof(nodepath), "/dev/block/vold/%d:%d",
             MAJOR(d), MINOR(d));

    if(primaryStorage) {
        if ((fd = open(MASS_STORAGE_FILE_PATH, O_WRONLY)) < 0) {
            SLOGE("Unable to open ums lunfile (%s)", strerror(errno));
            return -1;
        }
    } 
    else 
    {
        if ((fd = open(MASS_STORAGE_EXTERNAL_FILE_PATH, O_WRONLY)) < 0) {
            SLOGE("Unable to open ums lunfile (%s)", strerror(errno));
            return -1;
        }
    }

    if (write(fd, nodepath, strlen(nodepath)) < 0) {
        SLOGE("Unable to write to ums lunfile (%s)", strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    v->handleVolumeShared();
    if (mUmsSharingCount++ == 0) {
        FILE* fp;
        mSavedDirtyRatio = -1; // in case we fail
        if ((fp = fopen(DIRTY_RATIO_PATH, "r+"))) {
            char line[16];
            if (fgets(line, sizeof(line), fp) && sscanf(line, "%d", &mSavedDirtyRatio)) {
                fprintf(fp, "%d\n", mUmsDirtyRatio);
            } else {
                SLOGE("Failed to read dirty_ratio (%s)", strerror(errno));
            }
            fclose(fp);
        } else {
            SLOGE("Failed to open %s (%s)", DIRTY_RATIO_PATH, strerror(errno));
        }
    }
    return 0;
}

int VolumeManager::unshareVolume(const char *label, const char *method) {
    Volume *v = lookupVolume(label);

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    const char* externalStorage = getenv("EXTERNAL_STORAGE");
    bool primaryStorage = externalStorage && !strcmp(v->getMountpoint(), externalStorage);

    if (strcmp(method, "ums")) {
        errno = ENOSYS;
        return -1;
    }

    if (v->getState() != Volume::State_Shared) {
        errno = EINVAL;
        return -1;
    }

    int fd;
    if(primaryStorage) {
        if ((fd = open(MASS_STORAGE_FILE_PATH, O_WRONLY)) < 0) {
            SLOGE("Unable to open ums lunfile (%s)", strerror(errno));
            return -1;
        }
    }
    else
    {
        if ((fd = open(MASS_STORAGE_EXTERNAL_FILE_PATH, O_WRONLY)) < 0) {
            SLOGE("Unable to open ums lunfile (%s)", strerror(errno));
            return -1;
        }
    }
    char ch = 0;
    #define UNSHARE_RETRIES 300
    #define UNSHARE_RETRY_GAP_MS 200     
    for(int i=0; i < UNSHARE_RETRIES; i++) {          
	    if (write(fd, &ch, 1) < 0) {
            if (i%10 == 0) {
	           SLOGE("Unable to write to ums lunfile (%s), retry(%d)", strerror(errno), i);
            }
            
            if ( i >= (UNSHARE_RETRIES -1)) {
               SLOGE("Retry Fail: Unable to write to ums lunfile (%s)", strerror(errno));
               close(fd);
               return -1;
            }
	        usleep(UNSHARE_RETRY_GAP_MS*1000);
	    }
        else {
           break;
        }        
    }  

    close(fd);
    v->handleVolumeUnshared();
    if (--mUmsSharingCount == 0 && mSavedDirtyRatio != -1) {
        FILE* fp;
        if ((fp = fopen(DIRTY_RATIO_PATH, "r+"))) {
            fprintf(fp, "%d\n", mSavedDirtyRatio);
            fclose(fp);
        } else {
            SLOGE("Failed to open %s (%s)", DIRTY_RATIO_PATH, strerror(errno));
        }
        mSavedDirtyRatio = -1;
    }
    return 0;
}

extern "C" int vold_disableVol(const char *label) {
    VolumeManager *vm = VolumeManager::Instance();
    vm->disableVolumeManager();
    vm->unshareVolume(label, "ums");
    return vm->unmountVolume(label, true, false);
}

extern "C" int vold_getNumDirectVolumes(void) {
    VolumeManager *vm = VolumeManager::Instance();
    return vm->getNumDirectVolumes();
}

int VolumeManager::getNumDirectVolumes(void) {
    VolumeCollection::iterator i;
    int n=0;

    for (i = mVolumes->begin(); i != mVolumes->end(); ++i) {
        if ((*i)->getShareDevice() != (dev_t)0) {
            n++;
        }
    }
    return n;
}

extern "C" int vold_getDirectVolumeList(struct volume_info *vol_list) {
    VolumeManager *vm = VolumeManager::Instance();
    return vm->getDirectVolumeList(vol_list);
}

int VolumeManager::getDirectVolumeList(struct volume_info *vol_list) {
    VolumeCollection::iterator i;
    int n=0;
    dev_t d;

    for (i = mVolumes->begin(); i != mVolumes->end(); ++i) {
        if ((d=(*i)->getShareDevice()) != (dev_t)0) {
            (*i)->getVolInfo(&vol_list[n]);
            snprintf(vol_list[n].blk_dev, sizeof(vol_list[n].blk_dev),
                     "/dev/block/vold/%d:%d",MAJOR(d), MINOR(d));
            n++;
        }
    }

    return 0;
}

int VolumeManager::unmountVolume(const char *label, bool force, bool revert) {
#ifndef MTK_2SDCARD_SWAP	
    Volume *v = lookupVolume(label);

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    if (v->getState() == Volume::State_NoMedia) {
        errno = ENODEV;
        return -1;
    }

    if (v->getState() != Volume::State_Mounted) {
        SLOGW("Attempt to unmount volume which isn't mounted (%d)\n",
             v->getState());
        errno = EBUSY;
        return UNMOUNT_NOT_MOUNTED_ERR;
    }

    cleanupAsec(v, force);

    return v->unmountVol(force, revert);
#else	
	Volume *v = lookupVolume(label);
	int result ;

	SLOGW(" VolumeManager::unmountVolume -- SWAP enabled") ;

    if (!v) {
        errno = ENOENT;
        return -1;
    }

    if (v->getState() == Volume::State_NoMedia) {
        errno = ENODEV;
        return -1;
    }

    if (v->getState() != Volume::State_Mounted) {
        SLOGW("Attempt to unmount volume which isn't mounted (%d)\n",
             v->getState());
        errno = EBUSY;
        return UNMOUNT_NOT_MOUNTED_ERR;
    }

    cleanupAsec(v, force);

	SLOGW(" VolumeManager::unmountVolume -- unmount label(%s) storage", label) ;
    result = v->unmountVol(force, revert);
	if(result < 0) 
		return result ;

	if(getNeedSwapAfterUnmount()) {
		SLOGW(" VolumeManager::unmountVolume -- getNeedSwapAfterUnmount") ;
		//Does this time unmountVolume() try to unmount external - sd ? 		
		SLOGW(" VolumeManager::unmountVolume -- getlael = %s", v->getLabel()) ;
		if(v->isExternalSD()) {
			SLOGW(" VolumeManager::unmountVolume -- it means that we just unmount external sd") ;
			Volume *external_sd = v ;
			Volume *phone_storage = lookupVolume("sdcard");

			//if phone_storage is mounted  &&
			//   phone_Stroage is mounted on /mnt/sdcard2
			if((phone_storage->getState() == Volume::State_Mounted) &&          
			  (!strcmp(phone_storage->getMountpoint(), dv2_old_mountpoint)) 
			) {
				//Unmount 
				SLOGW("VolumeManager::unmountVolume -- unmount phone storage") ;
				phone_storage->unmountVol(force, revert) ;
				//SWAP mount point again
				SLOGW("VolumeManager::unmountVolume -- set mountpoint") ;
				phone_storage->setMountpoint((char*) dv1_old_mountpoint) ;
				external_sd->setMountpoint((char*) dv2_old_mountpoint) ;
				//Remount phone_storage to /mnt/sdcard
				SLOGW("VolumeManager::unmountVolume -- mount phone storage") ;

				set2SdcardSwapped(false) ;
                if(phone_storage->getState() == Volume::State_Idle) {
                   #ifdef MTK_SHARED_SDCARD
                      phone_storage->unmountPath(dv2_old_mountpoint, true);
                      property_set("ctl.restart", "sdcard");
                   #endif  
                   result = phone_storage->mountVol(); 
                }                    
			}

			if(phone_storage->getState() != Volume::State_Mounted) 
				SLOGW("VolumeManager::mountVolume -- phone_storage is note mounted(state = %d)", phone_storage->getState()) ;			
			if(strcmp(phone_storage->getMountpoint(), dv2_old_mountpoint)) 
				SLOGW("VolumeManager::mountVolume -- phone_storage\' mount point is %s, not %s", phone_storage->getMountpoint(), dv1_old_mountpoint) ;
		}
		setNeedSwapAfterUnmount(false) ;
	}
	
	return result ;
	
#endif
}

#ifdef MTK_2SDCARD_SWAP	
void VolumeManager::rollbackToOnlyInternalSd() {
    Volume *cfgVol2 = NULL;
    Volume *cfgVol1 = NULL;

    cfgVol1 = lookupVolume("sdcard");
    cfgVol2 = lookupVolume("sdcard2");
  if(cfgVol1 == NULL) 
       SLOGE("[ERR!!] In VolumeManager::handleBlockEvent -- label \"sdcard\" should exist in vold.fstab !!");
  if(cfgVol2 == NULL) 
       SLOGE("[ERR!!] In VolumeManager::handleBlockEvent -- label \"sdcard2\" should exist in vold.fstab !!"); 
               
  SLOGI("[SWAP]rollbackToOnlyInternalSd, cfgVol1->getState()=%d, cfgVol2->getState()=%d", cfgVol1->getState(), cfgVol2->getState());

    if ((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Checking)) {
     int retries= 10;
         while (retries--) {
               usleep(1000*1000);                       
               if (cfgVol1->getState() == Volume::State_Mounted) {
                   break;
               }
               else {
                 if(retries == 0) {
                   SLOGI("[SWAP]rollbackToOnlyInternalSd: Can NOT wait internal sd to 'Mounted', try(%d), state(%d)", retries, cfgVol1->getState());
                 }
                 else {
                   SLOGI("[SWAP]rollbackToOnlyInternalSd: Try to wait internal sd to 'Mounted, try(%d), state(%d)", retries, cfgVol1->getState());
                 } 
               }                
         }
   }

     if ((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Mounted)) {               
         SLOGI("[SWAP] %s() : roll back flow starts !\n", __func__) ;
         /* At this moment, two volume are both in idle state, so we can directly change mount point */
         SLOGI("[SWAP] %s() : roll back flow starts -- umount phone stroage\n", __func__) ;
         cfgVol1->unmountVol(true, false);
         SLOGI("[SWAP] %s() : roll back flow starts -- reset mount point\n", __func__) ;
         cfgVol1->setMountpoint((char *)dv1_old_mountpoint);
         cfgVol2->setMountpoint((char *)dv2_old_mountpoint);
         SLOGI("[SWAP] %s() : roll back flow starts -- reset property\n", __func__) ;
         set2SdcardSwapped(false);
         if((getIpoState() == State_Ipo_Start) && (cfgVol1->getState() == Volume::State_Idle)) {
             SLOGI("[SWAP] %s() : roll back flow starts -- remount \n", __func__) ;
             cfgVol1->mountVol();
#ifdef MTK_SHARED_SDCARD
               cfgVol1->unmountPath(dv2_old_mountpoint, true);
               property_set("ctl.restart", "sdcard");
#endif  
         }   

         //ALPS00456955 
         SLOGI("[SWAP] %s() : %s %s volume corrupted", __func__, cfgVol2->getLabel(), cfgVol2->getMountpoint());
         char msg[255] ;
         snprintf(msg, sizeof(msg), "Volume %s %s volume corrupted", cfgVol2->getLabel(), cfgVol2->getMountpoint());
         getBroadcaster()->sendBroadcast(ResponseCode::VolumeUnmountable, msg, false);
         //ALPS00456955 
     }  
     else {
         SLOGI("[SWAP] %s() : do not need to do roll back flow !\n", __func__) ;
     }
     
     SLOGD("[SWAP] %s() : SWAP finished.\n", __func__) ;
     setSwapOnGoing(false) ;
}
#endif


extern "C" int vold_unmountAllAsecs(void) {
    int rc;

    VolumeManager *vm = VolumeManager::Instance();
    rc = vm->unmountAllAsecsInDir(Volume::SEC_ASECDIR_EXT);
    if (vm->unmountAllAsecsInDir(Volume::SEC_ASECDIR_INT)) {
        rc = -1;
    }
    return rc;
}

#define ID_BUF_LEN 256
#define ASEC_SUFFIX ".asec"
#define ASEC_SUFFIX_LEN (sizeof(ASEC_SUFFIX) - 1)
int VolumeManager::unmountAllAsecsInDir(const char *directory) {
    DIR *d = opendir(directory);
    int rc = 0;

    if (!d) {
        SLOGE("Could not open asec dir %s", directory);
        return -1;
    }

    size_t dirent_len = offsetof(struct dirent, d_name) +
            pathconf(directory, _PC_NAME_MAX) + 1;

    struct dirent *dent = (struct dirent *) malloc(dirent_len);
    if (dent == NULL) {
        SLOGE("Failed to allocate memory for asec dir");
        return -1;
    }

    struct dirent *result;
    while (!readdir_r(d, dent, &result) && result != NULL) {
        if (dent->d_name[0] == '.')
            continue;
        if (dent->d_type != DT_REG)
            continue;
        size_t name_len = strlen(dent->d_name);
        if (name_len > 5 && name_len < (ID_BUF_LEN + ASEC_SUFFIX_LEN - 1) &&
                !strcmp(&dent->d_name[name_len - 5], ASEC_SUFFIX)) {
            char id[ID_BUF_LEN];
            strlcpy(id, dent->d_name, name_len - 4);
            if (unmountAsec(id, true)) {
                /* Register the error, but try to unmount more asecs */
                rc = -1;
            }
        }
    }
    closedir(d);

    free(dent);

    return rc;
}

/*
 * Looks up a volume by it's label or mount-point
 */
Volume *VolumeManager::lookupVolume(const char *label) {
    VolumeCollection::iterator i;

    for (i = mVolumes->begin(); i != mVolumes->end(); ++i) {
        if (label[0] == '/') {
            if (!strcmp(label, (*i)->getMountpoint()))
                return (*i);
        } else {
            if (!strcmp(label, (*i)->getLabel()))
                return (*i);
        }
    }
    return NULL;
}

bool VolumeManager::isMountpointMounted(const char *mp)
{
    char device[256];
    char mount_path[256];
    char rest[256];
    FILE *fp;
    char line[1024];

    if (!(fp = fopen("/proc/mounts", "r"))) {
        SLOGE("Error opening /proc/mounts (%s)", strerror(errno));
        return false;
    }

    while(fgets(line, sizeof(line), fp)) {
        line[strlen(line)-1] = '\0';
        sscanf(line, "%255s %255s %255s\n", device, mount_path, rest);
        if (!strcmp(mount_path, mp)) {
            fclose(fp);
            return true;
        }
    }

    fclose(fp);
    return false;
}

int VolumeManager::cleanupAsec(Volume *v, bool force) {
    const char* externalStorage = getenv("EXTERNAL_STORAGE");
    bool primaryStorage = externalStorage && !strcmp(v->getMountpoint(), externalStorage);

    if (!primaryStorage) {
        SLOGD("External sd card, skip cleanupAsec()");
        return 0;
    } else {
        SLOGD("Primary sd card, do cleanupAsec()");      
    }    

    int rc = unmountAllAsecsInDir(Volume::SEC_ASECDIR_EXT);

    AsecIdCollection toUnmount;
    // Find the remaining OBB files that are on external storage.
    for (AsecIdCollection::iterator it = mActiveContainers->begin(); it != mActiveContainers->end();
            ++it) {
        ContainerData* cd = *it;

        if (cd->type == ASEC) {
            // nothing
        } else if (cd->type == OBB) {
            if (v == getVolumeForFile(cd->id)) {
                toUnmount.push_back(cd);
            }
        } else {
            SLOGE("Unknown container type %d!", cd->type);
        }
    }

    for (AsecIdCollection::iterator it = toUnmount.begin(); it != toUnmount.end(); ++it) {
        ContainerData *cd = *it;
        SLOGI("Unmounting ASEC %s (dependant on %s)", cd->id, v->getMountpoint());
        if (unmountObb(cd->id, force)) {
            SLOGE("Failed to unmount OBB %s (%s)", cd->id, strerror(errno));
            rc = -1;
        }
    }

    return rc;

}

#define MSDC_REINIT_SDCARD            _IOW('r', 9, int)
#define MSDC_DEVICE_PATH "/dev/misc-sd"

int VolumeManager::reinitExternalSD(){
	int inode = 0;
	int result = 0;

	SLOGI("start to reinitExternalSD()");

	inode = open(MSDC_DEVICE_PATH, O_RDONLY);
	if (inode < 0) {
		SLOGI("open device error!\n");
		return -1;
	}
	result = ioctl(inode, MSDC_REINIT_SDCARD, NULL);
	if(result){
		SLOGI("ioctl error!\n");
		close(inode);
		return result;
	}
	else
	    SLOGI("reinitExternalSD success!\n");
	close(inode);	
	/* Wait a minute to let kernel has a change to issue UEVENT */
	sleep(1);
    return 0;
}

#ifdef MTK_SHARED_SDCARD
void VolumeManager::setSharedSdState(int state) {
    int i=0;
    VolumeCollection::iterator it;
    for (i = 0, it = mVolumes->begin(); it != mVolumes->end(); ++it, i++) {
        if ((*it)->IsEmmcStorage()) {            
            (*it)->setState(state);
            break;
        }
    }    
}
#endif

#ifdef MTK_2SDCARD_SWAP
void VolumeManager::swap2Sdcard() {
    int i=0;
    int numVolume_inConfigFile =0;
        int numVolume_onBoot =0;

    SLOGD("swap2Sdcard: The SWAP feature option is enabled in this load.");

    VolumeCollection::iterator it;
        Volume* first_volume = NULL;
        Volume* second_volume = NULL;
    for (i = 0, it = mVolumes->begin(); it != mVolumes->end(); ++it, i++) {
         numVolume_inConfigFile++;

         if (i == 0)
                    first_volume = *it;
                 if (i == 1)
                        second_volume = *it;

         if ((*it)->getState() == Volume::State_Idle) {
             numVolume_onBoot++;
         }
    }
        SLOGD("swap2Sdcard: numVolume_inConfigFil=%d, numVolume_onBoot=%d", numVolume_inConfigFile, numVolume_onBoot);
    dv1_old_mountpoint = first_volume->getMountpoint();
    dv2_old_mountpoint = second_volume->getMountpoint();

    if (numVolume_inConfigFile >= 2 && numVolume_onBoot >= 2){
        char* dv1_old_mountpoint = strdup (first_volume->getMountpoint());
                char* dv2_old_mountpoint = strdup (second_volume->getMountpoint());
                SLOGD("swap2Sdcard: dv1_old_mountpoint=%s, dv2_old_mountpoint=%s", dv1_old_mountpoint, dv2_old_mountpoint);

        first_volume->setMountpoint(dv2_old_mountpoint);
                second_volume->setMountpoint(dv1_old_mountpoint);

        free(dv1_old_mountpoint);
                free(dv2_old_mountpoint);
		set2SdcardSwapped(true) ;

        SLOGD("swap2Sdcard: dv1_mountpoint=%s, dv2_mountpoint=%s", first_volume->getMountpoint(), second_volume->getMountpoint());
    }
    else
    	set2SdcardSwapped(false) ;
    bSdCardSwapBootComplete = true;
}
#endif
void VolumeManager::mountallVolumes() {
	int i=0;
    VolumeCollection::iterator it;
    for (i = 0, it = mVolumes->begin(); it != mVolumes->end(); ++it, i++) {
#ifdef MTK_SHARED_SDCARD
        if ((*it)->IsEmmcStorage()) {
			continue;
        }
#endif
		(*it)->mountVol();
    }
}

bool VolumeManager::isSomeVolumeShared() {
    int i=0;
    VolumeCollection::iterator it;
    for (i = 0, it = mVolumes->begin(); it != mVolumes->end(); ++it, i++) {
        if ((*it)->getState() == Volume::State_Shared) {
           return true;
        }
    }
    return false;
}

char *VolumeManager::replace(const char *src, char *token, char *target) {
    static char buffer[1024];
    char *pch;
    if( !(pch = strstr(src, token)))
    {
        strcpy(buffer, src);
        return buffer;
    }
    strncpy( buffer, src, pch-src);
    buffer[pch-src]=0;
    sprintf( buffer+(pch-src), "%s%s", target, pch+strlen(token));
    return buffer;

}
