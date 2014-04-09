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

#include <linux/kdev_t.h>

#define LOG_TAG "DirectVolume"

#include <cutils/log.h>
#include <sysutils/NetlinkEvent.h>

#include "DirectVolume.h"
#include "VolumeManager.h"
#include "ResponseCode.h"
#include "cryptfs.h"
#include <sys/stat.h> 
#include <fcntl.h>
#include <cutils/xlog.h>
#include "Fat.h"
#include <linux/fs.h>
#include <pthread.h>
#include <sys/time.h>
#include <errno.h>

#ifdef MTK_FAT_ON_NAND
#include "fat_on_nand.h"
#include <linux/loop.h>
#endif 
#define PARTITION_DEBUG
/* Max wait for uevent timeout */
#define PART_RESCAN_UEVENT_TO		5

#define WAIT_ON_NO_EVENT				0
#define WAIT_ON_ADD_EVENT				1
#define WAIT_ON_CHG_EVENT				2

static pthread_mutex_t part_rescan_mutex = PTHREAD_MUTEX_INITIALIZER;  
static pthread_cond_t part_rescan_cond = PTHREAD_COND_INITIALIZER;

DirectVolume::DirectVolume(VolumeManager *vm, const char *label,
                           const char *mount_point, int partIdx) :
              Volume(vm, label, mount_point) {
    mPartIdx = partIdx;

    mPaths = new PathCollection();
    for (int i = 0; i < MAX_PARTITIONS; i++)
        mPartMinors[i] = -1;
    mPendingPartMap = 0;
    mDiskMajor = -1;
    mDiskMinor = -1;
    mDiskNumParts = 0;

    setState(Volume::State_NoMedia);
}

DirectVolume::~DirectVolume() {
    PathCollection::iterator it;

    for (it = mPaths->begin(); it != mPaths->end(); ++it)
        free(*it);
    delete mPaths;
}

int DirectVolume::addPath(const char *path) {
    mPaths->push_back(strdup(path));

    char *p = strstr(path, "mtk-sd.0");
	if (p) {
        SLOGI("This is emmc storage (%s)", path);
		mIsEmmcStorage = true;
    }
#ifdef MTK_FAT_ON_NAND
    if(!strncmp(path, "/devices/virtual/block/loop", 27)){
        mLoopDeviceIdx = atoi((path+27));
        /* Bind loop device */
        char devicepath[100];
        char fatimgfilepath[200];
        int fd, ffd, mode;
        struct loop_info loopinfo;
        const char *fat_mnt;
        const char *fat_filename;
    
        fat_mnt = FAT_PARTITION_MOUNT_POINT;
        fat_filename = FAT_IMG_NAME;
        snprintf(devicepath, sizeof(devicepath), "/dev/block/loop%d", mLoopDeviceIdx);
        snprintf(fatimgfilepath, sizeof(fatimgfilepath), "%s/%s", fat_mnt, fat_filename);
        mode = O_RDWR;
        if ((ffd = open (fatimgfilepath, mode)) < 0) {
          if (ffd < 0) {
              SLOGE("[FON]Fail to open %s %s(%d)", fatimgfilepath, strerror(errno), errno);
          }
        }
        if ((fd = open (devicepath, mode)) < 0) {
            SLOGE("[FON]Fail to open %s %s(%d)", devicepath, strerror(errno), errno);
        }
        /* Determine loop device is available or not */
        if (ioctl(fd, LOOP_GET_STATUS, &loopinfo) < 0 && errno == ENXIO) {
                if (ioctl (fd, LOOP_SET_FD, ffd) < 0) {
                SLOGE("[FON]Fail to ioctl: LOOP_SET_FD %s(%d)", strerror(errno), errno);
            }
        }
        else{
            SLOGE("[FON]Fail to bind FAT image %s to %s.Loop device is busy!", fatimgfilepath, devicepath);
        }
        close (fd);
        close (ffd);
    }
    else
        mLoopDeviceIdx = -1;
#endif
    return 0;
}

void DirectVolume::setFlags(int flags) {
    mFlags = flags;
}

dev_t DirectVolume::getDiskDevice() {
    return MKDEV(mDiskMajor, mDiskMinor);
}

dev_t DirectVolume::getShareDevice() {
    if (mPartIdx != -1) {
        return MKDEV(mDiskMajor, mPartIdx);
    } else {
        return MKDEV(mDiskMajor, mDiskMinor);
    }
}

void DirectVolume::handleVolumeShared() {
    setState(Volume::State_Shared);
}
static volatile int part_rescan_wait = WAIT_ON_NO_EVENT;

static int
sync_ptable(int fd)
{
    struct stat stat;
    int rv;
    int ret;
    struct timeval now;
  	struct timespec outtime;
		
    if (fstat(fd, &stat)) {
       ALOGE("Cannot stat, errno=%d.", errno);
       return -1;
    }

		ALOGI("Direct sync_ptable() : S_ISBLK") ;
    if (S_ISBLK(stat.st_mode)) {
		ALOGI("Direct sync_ptable() : ioctl(BLKRRPART)") ;
		if((rv = ioctl(fd, BLKRRPART, NULL)) < 0) {
        	ALOGE("Could not re-read partition table. REBOOT!. (errno=%d)", errno);
        	return -1;
		}
    }

    gettimeofday(&now, NULL);
    outtime.tv_sec = now.tv_sec + PART_RESCAN_UEVENT_TO;
		
		
		if((ret = pthread_cond_timedwait(&part_rescan_cond, &part_rescan_mutex, &outtime))){
			SLOGE("wait for partition rescan uevent %d seconds timeout. ret:%d, error:%s", PART_RESCAN_UEVENT_TO, ret, strerror(ret));
		}

		ALOGI("Direct sync_ptable() : after ioctl(BLKRRPART)") ;

    return 0;
}

bool rescan_part = false;
void DirectVolume::handleVolumeUnshared() {

	//No need for internal storage, becuase BLKRRPART ioctl needs to apply on block device.BLKRRPART also needs this block device's all partitions be unmounted.
	//Apply ioctl on internal storage does not make sense becuase we do not unmount /data, /system during runtime.
	if (!IsEmmcStorage()) {
	  int fd;
	  dev_t diskNode =getDiskDevice(); 
	  char devicePath[255];
	  sprintf(devicePath, "/dev/block/vold/%d:%d",
	                MAJOR(diskNode), MINOR(diskNode));
	                
		rescan_part = true;
	  sync();
	  SLOGD("%s will be rescan", devicePath);
	  if ((fd = open(devicePath, O_RDWR)) < 0) {
	    SLOGE("Cannot open device '%s' (errno=%d)", devicePath, errno);                
	  }
	  else {
			pthread_mutex_lock(&part_rescan_mutex);
			if(mDiskNumParts == 0){
				if( Fat::checkFatMeta(fd) == false) {
				    SLOGI("[%s]Check FAT metadata fail. Maybe PC has re-partitioned this storage. Need to re-scan partitions.", devicePath);
				    part_rescan_wait = WAIT_ON_ADD_EVENT;
				    sync_ptable(fd);
				}
				else
					SLOGI("[%s]Check FAT metadata success.", devicePath);
			}else{
				if( Fat::checkFatMeta(fd) == true) {
				    SLOGI("[%s has MBR]Check FAT metadata success. PC MUST re-partitioned this storage. Need to re-scan partitions.", devicePath);
				    part_rescan_wait = WAIT_ON_CHG_EVENT;
				}
				else{
						SLOGI("[%s has MBR]Check FAT metadata fail. PC MAYBE re-partitioned this storage. Need to re-scan partitions.", devicePath);
				    part_rescan_wait = WAIT_ON_ADD_EVENT;
				}
				/* If sd card original has MBR, we need to reread ptable */
				sync_ptable(fd);
			}
			part_rescan_wait = WAIT_ON_NO_EVENT;
			pthread_mutex_unlock(&part_rescan_mutex);
			close(fd);
	  }
	}		
	rescan_part = false;
	setState(Volume::State_Idle);
}

int DirectVolume::handleBlockEvent(NetlinkEvent *evt) {
    const char *dp = evt->findParam("DEVPATH");

    PathCollection::iterator  it;
    for (it = mPaths->begin(); it != mPaths->end(); ++it) {
        if (!strncmp(dp, *it, strlen(*it))) {
#ifdef MTK_FAT_ON_NAND
            if((-1 != mLoopDeviceIdx))
            {
                /* If this is loop device, then check this loop device is one of the volume, use exact path length */
                if (strncmp(dp, *it, strlen(dp))) {
                    goto End;
                }
            }
#endif
            /* We can handle this disk */
            int action = evt->getAction();
            const char *devtype = evt->findParam("DEVTYPE");

            if (action == NetlinkEvent::NlActionAdd) {
                int major = atoi(evt->findParam("MAJOR"));
                int minor = atoi(evt->findParam("MINOR"));
                char nodepath[255];

                snprintf(nodepath,
                         sizeof(nodepath), "/dev/block/vold/%d:%d",
                         major, minor);
                if (createDeviceNode(nodepath, major, minor)) {
                    SLOGE("Error making device node '%s' (%s)", nodepath,
                                                               strerror(errno));
                }
                if (!strcmp(devtype, "disk")) {
                    handleDiskAdded(dp, evt);
                } else {
                    handlePartitionAdded(dp, evt);
                    if(part_rescan_wait == WAIT_ON_ADD_EVENT)
                    	pthread_cond_signal(&part_rescan_cond);
                }
                /* Send notification iff disk is ready (ie all partitions found) */
                if (getState() == Volume::State_Idle) {
                    char msg[255];

                    snprintf(msg, sizeof(msg),
                             "Volume %s %s disk inserted (%d:%d)", getLabel(),
                             getMountpoint(), mDiskMajor, mDiskMinor);
                    mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskInserted,
                                                         msg, false);
                }
            } else if (action == NetlinkEvent::NlActionRemove) {
                if (!strcmp(devtype, "disk")) {
                    handleDiskRemoved(dp, evt);
                } else {
                    handlePartitionRemoved(dp, evt);
                }
            } else if (action == NetlinkEvent::NlActionChange) {
                if (!strcmp(devtype, "disk")) {
                    handleDiskChanged(dp, evt);
                    if(part_rescan_wait == WAIT_ON_CHG_EVENT)
                    	pthread_cond_signal(&part_rescan_cond);
                } else {
                    handlePartitionChanged(dp, evt);
                }
            } else {
                    SLOGW("Ignoring non add/remove/change event");
            }

            return 0;
        }
    }
#ifdef MTK_FAT_ON_NAND
End:
#endif
    errno = ENODEV;
    return -1;
}

void DirectVolume::handleDiskAdded(const char *devpath, NetlinkEvent *evt) {
    mDiskMajor = atoi(evt->findParam("MAJOR"));
    mDiskMinor = atoi(evt->findParam("MINOR"));

    const char *tmp = evt->findParam("NPARTS");
    if (tmp) {
        mDiskNumParts = atoi(tmp);
    } else {
        SLOGW("Kernel block uevent missing 'NPARTS'");
        mDiskNumParts = 1;
    }

    /*
    int partmask = 0;
    int i;
    for (i = 1; i <= mDiskNumParts; i++) {
        partmask |= (1 << i);
    }
    */
    mPendingPartMap = mDiskNumParts;
    for (int i = 0; i < MAX_PARTITIONS; i++)
        mPartMinors[i] = -1;

    if (mDiskNumParts == 0) {
#ifdef PARTITION_DEBUG
        SLOGD("Dv::diskIns - No partitions - good to go son!");
#endif
        setState(Volume::State_Idle);
    } else {
#ifdef PARTITION_DEBUG
        SLOGD("Dv::diskIns - waiting for %d partitions (pending partitions: %d)",
             mDiskNumParts, mPendingPartMap);
#endif
        setState(Volume::State_Pending);
    }
}

void DirectVolume::handlePartitionAdded(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));

    int part_num;

    const char *tmp = evt->findParam("PARTN");

    if (tmp) {
        part_num = atoi(tmp);
    } else {
        SLOGW("Kernel block uevent missing 'PARTN'");
        part_num = 1;
    }

    if (part_num > MAX_PARTITIONS || part_num < 1) {
        SLOGE("Invalid 'PARTN' value");
        return;
    }

    if (part_num > mDiskNumParts) {
        mDiskNumParts = part_num;
    }

    if (major != mDiskMajor) {
        SLOGE("Partition '%s' has a different major than its disk!", devpath);
        return;
    }
#ifdef PARTITION_DEBUG
    SLOGD("Dv:partAdd: part_num = %d, minor = %d\n", part_num, minor);
#endif
    if (part_num > MAX_PARTITIONS) {
        SLOGE("Dv:partAdd: ignoring part_num = %d (max: %d)\n", part_num, MAX_PARTITIONS-1);
    } else {
    	if ((mPartMinors[part_num - 1] == -1) && mPendingPartMap)
       	 	mPendingPartMap--;
        mPartMinors[part_num -1] = minor;
    }
    // mPendingPartMap &= ~(1 << part_num);

    if (!mPendingPartMap) {
#ifdef PARTITION_DEBUG
        SLOGD("Dv:partAdd: Got all partitions - ready to rock!");
#endif
        if (getState() != Volume::State_Formatting && (part_rescan_wait == WAIT_ON_NO_EVENT)) {
            setState(Volume::State_Idle);
            if(mVm->getIpoState() == VolumeManager::State_Ipo_Start){
                if (mRetryMount == true) {
                    mRetryMount = false;
                    mountVol();
                }
            }
        }
    } else {
#ifdef PARTITION_DEBUG
        SLOGD("Dv:partAdd: pending %d disk", mPendingPartMap);
#endif
    }
}

void DirectVolume::handleDiskChanged(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    char nodepath[255];
    int fd = 0;

    if ((major != mDiskMajor) || (minor != mDiskMinor)) {
        return;
    }

    SLOGI("Volume %s disk has changed", getLabel());
    const char *tmp = evt->findParam("NPARTS");
    if (tmp) {
        mDiskNumParts = atoi(tmp);
    } else {
        SLOGW("Kernel block uevent missing 'NPARTS'");
        mDiskNumParts = 1;
    }

    /*
    int partmask = 0;
    int i;
    for (i = 1; i <= mDiskNumParts; i++) {
        partmask |= (1 << i);
    }
    */
    mPendingPartMap = mDiskNumParts;
    for (int i = 0; i < MAX_PARTITIONS; i++)
        mPartMinors[i] = -1;

    if (getState() != Volume::State_Formatting) {
        if(evt->findParam("DISK_MEDIA_CHANGE")) {     
            xlog_printf(ANDROID_LOG_INFO, LOG_TAG, "UsbPatch: disk change with DISK_MEDIA_CHANGE");
      	    snprintf(nodepath, sizeof(nodepath), "/dev/block/vold/%d:%d", major, minor);
            xlog_printf(ANDROID_LOG_INFO, LOG_TAG, "UsbPatch: Check device node (%s)existence in (%s)!", nodepath, __func__);	
	    
	    if (-1 == (fd = open(nodepath, O_RDONLY, 0644)) ) {
                xlog_printf(ANDROID_LOG_WARN, LOG_TAG, "UsbPatch: Node Path (%s) open fail, do unmount, mDiskNumParts=%d!", nodepath, mDiskNumParts);
	        unmountVol(true, false);	
	    } else {
                xlog_printf(ANDROID_LOG_INFO, LOG_TAG, "UsbPatch: Node Path (%s) open success with mDiskNumParts=(%d)!", nodepath, mDiskNumParts);
		close(fd);
	    }				
        } else if(part_rescan_wait == WAIT_ON_NO_EVENT){
            if (mDiskNumParts == 0) {  
                xlog_printf(ANDROID_LOG_INFO, LOG_TAG, "UsbPatch: possible no partition media inserted, do mount first, mDiskNumParts=%d getState():%d", mDiskNumParts, getState());
                if(getState() == Volume::State_Idle){
                    if(mountVol() != 0) {
      		        mRetryMount = true;
      		    }
                } else {
                    mRetryMount = true;
      	            setState(Volume::State_Idle);
  	        }
            } else {
                xlog_printf(ANDROID_LOG_INFO, LOG_TAG, "UsbPatch: possible need next PartitionAdd uevent comes");
                setState(Volume::State_Pending);
            }
        }
    }
}

void DirectVolume::handlePartitionChanged(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    SLOGD("Volume %s %s partition %d:%d changed\n", getLabel(), getMountpoint(), major, minor);
}

void DirectVolume::handleDiskRemoved(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    char msg[255];
    bool enabled;

    if (mVm->shareEnabled(getLabel(), "ums", &enabled) == 0 && enabled) {
        mVm->unshareVolume(getLabel(), "ums");
    }

    if(0 == mDiskNumParts) {
      handlePartitionRemoved(devpath, evt);
    }
    SLOGD("Volume %s %s disk %d:%d removed\n", getLabel(), getMountpoint(), major, minor);
      if(mVm->getIpoState() == VolumeManager::State_Ipo_Start){
        snprintf(msg, sizeof(msg), "Volume %s %s disk removed (%d:%d)",
                 getLabel(), getMountpoint(), major, minor);
#ifdef MTK_2SDCARD_SWAP
        if(!strcmp(getLabel(), "sdcard2") &&
           (getState() != Volume::State_Formatting) &&
           (getState() != Volume::State_Shared)
          ) {
                snprintf(msg, sizeof(msg), "Volume %s %s disk removed (%d:%d)", getLabel(), "/storage/sdcard1", major, minor);
                SLOGD("[SWAP] to force sdcard2 disk removed msg uses correct path : %s", msg) ;
          }
#endif

        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskRemoved,
                                                 msg, false);
    }
    setState(Volume::State_NoMedia);
}

void DirectVolume::handlePartitionRemoved(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    char msg[255];
    int state;

    SLOGD("Volume %s %s partition %d:%d removed\n", getLabel(), getMountpoint(), major, minor);

    /*
     * The framework doesn't need to get notified of
     * partition removal unless it's mounted. Otherwise
     * the removal notification will be sent on the Disk
     * itself
     */
    state = getState();
    if (state != Volume::State_Mounted && state != Volume::State_Shared) {
        return;
    }
        
    if ((dev_t) MKDEV(major, minor) == mCurrentlyMountedKdev) {
        /*
         * Yikes, our mounted partition is going away!
         */

        snprintf(msg, sizeof(msg), "Volume %s %s bad removal (%d:%d)",
                 getLabel(), getMountpoint(), major, minor);
        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeBadRemoval,
                                             msg, false);
        
        if (mVm->waitForAfCleanupAsec(this)) {
            SLOGE("Failed to waitf for PMS to umount ASEC - Some resource in AP layer may NOT be released!");
        }
        
        if (mVm->cleanupAsec(this, true)) {
            SLOGE("Failed to cleanup ASEC - unmount will probably fail!");
        }

        if (Volume::unmountVol(true, false)) {
            SLOGE("Failed to unmount volume on bad removal (%s)", 
                 strerror(errno));
            // XXX: At this point we're screwed for now
        } else {
            SLOGD("Crisis averted");
        }

    } else if (state == Volume::State_Shared && (part_rescan_wait == WAIT_ON_NO_EVENT)) {
        /* removed during mass storage */
        snprintf(msg, sizeof(msg), "Volume %s %s bad removal (%d:%d)",
                 getLabel(), getMountpoint(), major, minor);
        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeBadRemoval,
                                             msg, false);

        if (mVm->unshareVolume(getLabel(), "ums")) {
            SLOGE("Failed to unshare volume on bad removal (%s)",
                strerror(errno));
        } else {
            SLOGD("Crisis averted");
        }
    }
}

/*
 * Called from base to get a list of devicenodes for mounting
 */
int DirectVolume::getDeviceNodes(dev_t *devs, int max) {

    if (mPartIdx == -1) {
        // If the disk has no partitions, try the disk itself
        if (!mDiskNumParts) {
            devs[0] = MKDEV(mDiskMajor, mDiskMinor);
            return 1;
        }

        int i;
        for (i = 0; i < mDiskNumParts; i++) {
            if (i == max)
                break;
            devs[i] = MKDEV(mDiskMajor, mPartMinors[i]);
        }
        return mDiskNumParts;
    }
    devs[0] = MKDEV(mDiskMajor, mPartMinors[mPartIdx -1]);
    return 1;
}

/*
 * Called from base to get number of partitions in Disk 
 */
int DirectVolume::getDeviceNumParts() {
    return mDiskNumParts;
}


/*
 * Called from base to update device info,
 * e.g. When setting up an dm-crypt mapping for the sd card.
 */
int DirectVolume::updateDeviceInfo(char *new_path, int new_major, int new_minor)
{
    PathCollection::iterator it;

    if (mPartIdx == -1) {
        SLOGE("Can only change device info on a partition\n");
        return -1;
    }

    /*
     * This is to change the sysfs path associated with a partition, in particular,
     * for an internal SD card partition that is encrypted.  Thus, the list is
     * expected to be only 1 entry long.  Check that and bail if not.
     */
    if (mPaths->size() != 1) {
        SLOGE("Cannot change path if there are more than one for a volume\n");
        return -1;
    }

    it = mPaths->begin();
    free(*it); /* Free the string storage */
    mPaths->erase(it); /* Remove it from the list */
    addPath(new_path); /* Put the new path on the list */

    /* Save away original info so we can restore it when doing factory reset.
     * Then, when doing the format, it will format the original device in the
     * clear, otherwise it just formats the encrypted device which is not
     * readable when the device boots unencrypted after the reset.
     */
    mOrigDiskMajor = mDiskMajor;
    mOrigDiskMinor = mDiskMinor;
    mOrigPartIdx = mPartIdx;
    memcpy(mOrigPartMinors, mPartMinors, sizeof(mPartMinors));

    mDiskMajor = new_major;
    mDiskMinor = new_minor;
    /* Ugh, virual block devices don't use minor 0 for whole disk and minor > 0 for
     * partition number.  They don't have partitions, they are just virtual block
     * devices, and minor number 0 is the first dm-crypt device.  Luckily the first
     * dm-crypt device is for the userdata partition, which gets minor number 0, and
     * it is not managed by vold.  So the next device is minor number one, which we
     * will call partition one.
     */
    mPartIdx = new_minor;
    mPartMinors[new_minor-1] = new_minor;

    mIsDecrypted = 1;

    return 0;
}

/*
 * Called from base to revert device info to the way it was before a
 * crypto mapping was created for it.
 */
void DirectVolume::revertDeviceInfo(void)
{
    if (mIsDecrypted) {
        mDiskMajor = mOrigDiskMajor;
        mDiskMinor = mOrigDiskMinor;
        mPartIdx = mOrigPartIdx;
        memcpy(mPartMinors, mOrigPartMinors, sizeof(mPartMinors));

        mIsDecrypted = 0;
    }

    return;
}

/*
 * Called from base to give cryptfs all the info it needs to encrypt eligible volumes
 */
int DirectVolume::getVolInfo(struct volume_info *v)
{
    strcpy(v->label, mLabel);
    strcpy(v->mnt_point, mMountpoint);
    v->flags=mFlags;
    /* Other fields of struct volume_info are filled in by the caller or cryptfs.c */

    return 0;
}
