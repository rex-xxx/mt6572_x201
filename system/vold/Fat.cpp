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
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <sys/mount.h>

#include <linux/kdev_t.h>
#include <linux/fs.h>

#define LOG_TAG "Vold"

#include <cutils/log.h>
#include <cutils/properties.h>

#include "Fat.h"

static char FSCK_MSDOS_PATH[] = "/system/bin/fsck_msdos";
static char MKDOSFS_PATH[] = "/system/bin/newfs_msdos";
extern "C" int logwrap(int argc, const char **argv, int background);
extern "C" int mount(const char *, const char *, const char *, unsigned long, const void *);

#define MTK_FORMAT_NOT_PARAM_CLUSTER

#ifdef 	MTK_FSCK_MSDOS_MTK
#define FSCK_MSDOS_MTK
#else
#undef FSCK_MSDOS_MTK
#endif

static char FSCK_MSDOS_MTK_PATH[] = "/system/bin/fsck_msdos_mtk";
/*
 * architecture independent description of all the info stored in a
 * FAT boot block.
 */
struct bootblock {
	unsigned int	BytesPerSec;		/* bytes per sector */
	unsigned int	SecPerClust;		/* sectors per cluster */
	unsigned int	ResSectors;		/* number of reserved sectors */
	unsigned int	FATs;			/* number of FATs */
	unsigned int	RootDirEnts;		/* number of root directory entries */
	unsigned int	Media;			/* media descriptor */
	unsigned int	FATsmall;		/* number of sectors per FAT */
	unsigned int	SecPerTrack;		/* sectors per track */
	unsigned int	Heads;			/* number of heads */
	unsigned int Sectors;		/* total number of sectors */
	unsigned int HiddenSecs;		/* # of hidden sectors */
	unsigned int HugeSectors;		/* # of sectors if bpbSectors == 0 */
	unsigned int	FSInfo;			/* FSInfo sector */
	unsigned int	Backup;			/* Backup of Bootblocks */
	unsigned long	RootCl;			/* Start of Root Directory */
	unsigned long	FSFree;			/* Number of free clusters acc. FSInfo */
	unsigned long	FSNext;			/* Next free cluster acc. FSInfo */

	/* and some more calculated values */
	unsigned int	flags;			/* some flags: */
#define	FAT32		1		/* this is a FAT32 file system */
					/*
					 * Maybe, we should separate out
					 * various parts of FAT32?	XXX
					 */
	int	ValidFat;		/* valid fat if FAT32 non-mirrored */
	unsigned long	ClustMask;		/* mask for entries in FAT */
	unsigned long	NumClusters;		/* # of entries in a FAT */
	unsigned int NumSectors;		/* how many sectors are there */
	unsigned int FATsecs;		/* how many sectors are in FAT */
	unsigned int NumFatEntries;	/* how many entries really are there */
	unsigned int	ClusterOffset;		/* at what sector would sector 0 start */
	unsigned int	ClusterSize;		/* Cluster size in bytes */

	/* Now some statistics: */
	unsigned int	NumFiles;		/* # of plain files */
	unsigned int	NumFree;		/* # of free clusters */
	unsigned int	NumBad;			/* # of bad clusters */
};

#define	CLUST12_MASK	0xfff
#define	CLUST16_MASK	0xffff
#define	CLUST32_MASK	0xfffffff
#define	CLUST_FREE	0		/* 0 means cluster is free */
#define	CLUST_FIRST	2		/* 2 is the minimum valid cluster number */
#define	CLUST_RSRVD	0xfffffff6	/* start of reserved clusters */
#define	CLUST_BAD	0xfffffff7	/* a cluster with a defect */
#define	CLUST_EOFS	0xfffffff8	/* start of EOF indicators */
#define	CLUST_EOF	0xffffffff	/* standard value for last cluster */
#define DOSBOOTBLOCKSIZE 512
#define FAT32 1
int readboot(	int dosfs, struct bootblock *boot)
{
	u_char block[DOSBOOTBLOCKSIZE];
	u_char fsinfo[2 * DOSBOOTBLOCKSIZE];
	u_char backup[DOSBOOTBLOCKSIZE];
	int ret = 0;
	int n;

	if ((n = read(dosfs, block, sizeof block)) == -1 || n != sizeof block) {
		SLOGE("could not read boot block");
		return -1;
	}

	if (block[510] != 0x55 || block[511] != 0xaa) {
		SLOGE("Invalid signature in boot block: %02x%02x\n", block[511], block[510]);
		return -1;
	}

	memset(boot, 0, sizeof *boot);
	boot->ValidFat = -1;

	/* decode bios parameter block */
	boot->BytesPerSec = block[11] + (block[12] << 8);
	boot->SecPerClust = block[13];
	boot->ResSectors = block[14] + (block[15] << 8);
	boot->FATs = block[16];
	boot->RootDirEnts = block[17] + (block[18] << 8);
	boot->Sectors = block[19] + (block[20] << 8);
	boot->Media = block[21];
	boot->FATsmall = block[22] + (block[23] << 8);
	boot->SecPerTrack = block[24] + (block[25] << 8);
	boot->Heads = block[26] + (block[27] << 8);
	boot->HiddenSecs = block[28] + (block[29] << 8) + (block[30] << 16) + (block[31] << 24);
	boot->HugeSectors = block[32] + (block[33] << 8) + (block[34] << 16) + (block[35] << 24);

	boot->FATsecs = boot->FATsmall;

        if(boot->BytesPerSec == 0){
                SLOGE("Invalid sector size: %u", boot->BytesPerSec);
                return -1;
        }

	if (!boot->RootDirEnts)
		boot->flags |= FAT32;
	if (boot->flags & FAT32) {
		boot->FATsecs = block[36] + (block[37] << 8)
				+ (block[38] << 16) + (block[39] << 24);
		if (block[40] & 0x80) {
			boot->ValidFat = block[40] & 0x0f;
		}

		/* check version number: */
		if (block[42] || block[43]) {
			/* Correct?				XXX */
			SLOGE("Unknown filesystem version: %x.%x\n",
			       block[43], block[42]);
			return -1;
		}
		boot->RootCl = block[44] + (block[45] << 8)
			       + (block[46] << 16) + (block[47] << 24);
		boot->FSInfo = block[48] + (block[49] << 8);
		boot->Backup = block[50] + (block[51] << 8);

		if ((n = lseek(dosfs, boot->FSInfo * boot->BytesPerSec, SEEK_SET)) == -1
		    || n != boot->FSInfo * boot->BytesPerSec
		    || (n = read(dosfs, fsinfo, sizeof fsinfo)) == ((ssize_t)-1)
		    || n != sizeof fsinfo) {
			SLOGE("could not read fsinfo block");
			return -1;
		}
		if (memcmp(fsinfo, "RRaA", 4)
		    || memcmp(fsinfo + 0x1e4, "rrAa", 4)
		    || fsinfo[0x1fc]
		    || fsinfo[0x1fd]
		    || fsinfo[0x1fe] != 0x55
		    || fsinfo[0x1ff] != 0xaa
		    || fsinfo[0x3fc]
		    || fsinfo[0x3fd]
		    || fsinfo[0x3fe] != 0x55
		    || fsinfo[0x3ff] != 0xaa) {
			SLOGE("Invalid signature in fsinfo block\n");
			
			boot->FSInfo = 0;
		}
		if (boot->FSInfo) {
			boot->FSFree = fsinfo[0x1e8] + (fsinfo[0x1e9] << 8)
				       + (fsinfo[0x1ea] << 16)
				       + (fsinfo[0x1eb] << 24);
			boot->FSNext = fsinfo[0x1ec] + (fsinfo[0x1ed] << 8)
				       + (fsinfo[0x1ee] << 16)
				       + (fsinfo[0x1ef] << 24);
		}

		if ((n = lseek(dosfs, boot->Backup * boot->BytesPerSec, SEEK_SET)) == -1
		    || n != boot->Backup * boot->BytesPerSec
		    || (n = read(dosfs, backup, sizeof backup)) == ((ssize_t)-1)
		    || n != sizeof backup) {
			SLOGE("could not read backup bootblock");
			return -1;
		}
		backup[65] = block[65];				/* XXX */
		
		if (memcmp(block + 11, backup + 11, 79)) {
                        char tmp[255];
                        int i;

			/*
			 * For now, lets not bail out if they don't match
			 * It seems a lot of sdcards are formatted with
			 * the backup either empty or containing garbage.
			 */

			SLOGE("Primary/Backup bootblock miscompare\n");

                        strcpy(tmp, "");
                        SLOGE("Primary:\n");
			for (i = 0; i < 79; i++) {
				char tmp2[16];
                                snprintf(tmp2, sizeof(tmp2), "%.2x ", block[11 + i]);
				strcat(tmp, tmp2);
                        }
                        SLOGE("%s\n", tmp);

			strcpy(tmp, "");
                        SLOGE("Backup:\n");
			for (i = 0; i < 79; i++) {
				char tmp2[16];
                                snprintf(tmp2, sizeof(tmp2), "%.2x ", backup[11 + i]);
				strcat(tmp, tmp2);
                        }
                        SLOGE("%s\n", tmp);
		}
		/* Check backup FSInfo?					XXX */
	}

	boot->ClusterOffset = (boot->RootDirEnts * 32 + boot->BytesPerSec - 1)
	    / boot->BytesPerSec
	    + boot->ResSectors
	    + boot->FATs * boot->FATsecs
	    - CLUST_FIRST * boot->SecPerClust;

	if (boot->BytesPerSec % DOSBOOTBLOCKSIZE != 0) {
		SLOGE("Invalid sector size: %u", boot->BytesPerSec);
		return -1;
	}
	if (boot->SecPerClust == 0) {
		SLOGE("Invalid cluster size: %u", boot->SecPerClust);
		return -1;
	}
	if (boot->Sectors) {
		boot->HugeSectors = 0;
		boot->NumSectors = boot->Sectors;
	} else
		boot->NumSectors = boot->HugeSectors;
	boot->NumClusters = (boot->NumSectors - boot->ClusterOffset) / boot->SecPerClust;

	if (boot->flags&FAT32)
		boot->ClustMask = CLUST32_MASK;
	else if (boot->NumClusters < (CLUST_RSRVD&CLUST12_MASK))
		boot->ClustMask = CLUST12_MASK;
	else if (boot->NumClusters < (CLUST_RSRVD&CLUST16_MASK))
		boot->ClustMask = CLUST16_MASK;
	else {
		SLOGE("Filesystem too big (%u clusters) for non-FAT32 partition\n",
		       boot->NumClusters);
		return -1;
	}

	switch (boot->ClustMask) {
	case CLUST32_MASK:
		boot->NumFatEntries = (boot->FATsecs * boot->BytesPerSec) / 4;
		break;
	case CLUST16_MASK:
		boot->NumFatEntries = (boot->FATsecs * boot->BytesPerSec) / 2;
		break;
	default:
		boot->NumFatEntries = (boot->FATsecs * boot->BytesPerSec * 2) / 3;
		break;
	}

	if (boot->NumFatEntries < boot->NumClusters) {
		SLOGE("FAT size too small, %u entries won't fit into %u sectors\n",
		       boot->NumClusters, boot->FATsecs);
		return -1;
	}
	boot->ClusterSize = boot->BytesPerSec * boot->SecPerClust;

	boot->NumFiles = 1;
	boot->NumFree = 0;
	
	SLOGI("\n\n-----------------------------------------------------------");
	SLOGI("\n   Boot Sector ");
	SLOGI("\n------------------------------------------------------------");
	SLOGI("\n BytesPerSec (bytes per sector) : %u ", boot->BytesPerSec);
	SLOGI("\n SecPerClust (sectors per cluster) : %u ", boot->SecPerClust);
	SLOGI("\n ResSectors (number of reserved sectors) : %u ", boot->ResSectors);
	SLOGI("\n FATs (number of FATs) : %u ", boot->FATs);
	SLOGI("\n RootDirEnts (number of root directory entries) : %u ", boot->RootDirEnts);
	SLOGI("\n Media (media descriptor) : 0x%x ", boot->Media);
	SLOGI("\n FATsmall (number of sectors per FAT16) : %u ", boot->FATsmall);
	SLOGI("\n SecPerTrack (sectors per track) : %u ", boot->SecPerTrack);
	SLOGI("\n Heads (number of heads) : %u ", boot->Heads);
	SLOGI("\n Sectors (total number of sectors) : %u ", boot->Sectors);
	SLOGI("\n HiddenSecs (# of hidden sectors) : %u ", boot->HiddenSecs);
	SLOGI("\n HugeSectors (# of sectors if bpbSectors == 0) : 0x%x ", boot->HugeSectors);
	SLOGI("\n FSInfo (FSInfo sector) : 0x%x ", boot->FSInfo);
	SLOGI("\n Backup (Backup of Bootblocks) : 0x%x ", boot->Backup);
	SLOGI("\n RootCl (Start of Root Directory) : 0x%x ", boot->RootCl);
	SLOGI("\n FSFree (Number of free clusters acc. FSInfo) : 0x%x ", boot->FSFree);
	SLOGI("\n FSNext (Next free cluster acc. FSInfo) : 0x%x ", boot->FSNext);
	SLOGI("\n------------------------------------------------------------");
	SLOGI("\n ClustMask (FAT12:fff, FAT16:ffff, FAT32:fffffff) : 0x%x ", boot->ClustMask);
	SLOGI("\n NumClusters (cluster numbers) : %u ", boot->NumClusters);
	SLOGI("\n NumSectors (how many sectors are there) : %u ", boot->NumSectors);
	SLOGI("\n FATsecs (how many sectors are in FAT) : %u ", boot->FATsecs);
	SLOGI("\n NumFatEntries (Max entry # in the FAT) : %u ", boot->NumFatEntries);
	SLOGI("\n ClusterOffset (at what sector would sector 0 start) : %u ", boot->ClusterOffset);
	SLOGI("\n ClusterSize (Cluster size in bytes) : %u ", boot->ClusterSize);
	SLOGI("\n------------------------------------------------------------\n");

	return ret;
}

bool Fat::checkFatMeta(	int dosfs) {
     struct bootblock boot;
     return (readboot(dosfs, &boot) == 0);
}

bool Fat::isFat32(int dosfs) {
     struct bootblock boot;
     if (!readboot(dosfs, &boot)) {
       return boot.flags & FAT32;
     }
     else {
       SLOGI("Not valid BPB. return NOT FAT32.");
       return false;
     }     
}

int Fat::check(const char *fsPath) {
    bool rw = true;
    if (access(FSCK_MSDOS_PATH, X_OK)) {
        SLOGW("Skipping fs checks\n");
        return 0;
    }

#ifdef FSCK_MSDOS_MTK
	SLOGI("-- MTK_FSCK_MSDOS_MTK enabled--");

	int fsck_enhanced = 0 ; // 0 : original ver(fsck_msdos), 1 : enhanced ver(fsck_msdos_mtk)
	if (access(FSCK_MSDOS_MTK_PATH, X_OK)) {
        SLOGW("Because %s does not exist, we just use fsck_msdos (original ver.)", FSCK_MSDOS_MTK_PATH) ;		
        fsck_enhanced = 0 ;
    }
	else {	
		SLOGI("vold:fat:check fs = %s\n", fsPath) ;
		int fd = open(fsPath, O_RDONLY);
		if(fd < 0) {
			SLOGW("Because cannot read dev, we just use fsck_msdos (original ver.)") ;		
			fsck_enhanced = 0 ;
		}
		else {
			struct bootblock boot ;
			if(readboot(fd, &boot) == 0) {
				if(boot.ClustMask == 0xfff) {
					SLOGW("Because fsck_msdos_mtk only supports FAT32, but this is FAT12!") ;		
					SLOGW("We still use fsck_msdos for FAT12!") ;		
					fsck_enhanced = 0 ;
				}
				else if(boot.ClustMask == 0xffff) {
					SLOGW("Because fsck_msdos_mtk only supports FAT32, but this is FAT16!") ;		
					SLOGW("We still use fsck_msdos for FAT16!") ;		
					fsck_enhanced = 0 ;
				}
				else {
					SLOGW("We always use fsck_msdos_mtk for FAT32 now!") ;		
					fsck_enhanced = 1 ;
				}

				/*if(boot.NumClusters * 16 > 8*1024*1024) {
					SLOGI("It may need %d bytes ! It is too much ! Try enhanced fsck -- fsck_msdos_mtk !", boot.NumClusters * 16) ;
					fsck_enhanced = 1 ;
				}
				else
					SLOGW("Use fsck_msdos (original ver.)") ;	
				*/
			}
			close(fd) ;
		}	
	}
#endif

    int pass = 1;
    int rc = 0;
    do {
        const char *args[5];
#ifdef FSCK_MSDOS_MTK
		if(fsck_enhanced)
			args[0] = FSCK_MSDOS_MTK_PATH;
		else
#endif
        args[0] = FSCK_MSDOS_PATH;
        args[1] = "-p";
        args[2] = "-f";
        args[3] = fsPath;
        args[4] = NULL;

        rc = logwrap(4, args, 1);

        switch(rc) {
        case 0:
            SLOGI("Filesystem check completed OK");
            return 0;

        case 2:
            SLOGE("Filesystem check failed (not a FAT filesystem)");
            errno = ENODATA;
            return -1;

        case 4:
            if (pass++ <= 3) {
                SLOGW("Filesystem modified - rechecking (pass %d)",
                        pass);
                continue;
            }
            SLOGE("Failing check after too many rechecks");
            errno = EIO;
            return -1;

        default:
            SLOGE("Filesystem check failed (unknown exit code %d)", rc);
            errno = EIO;
            return -1;
        }
    } while (1);

    return 0;
}

#ifdef MTK_EMMC_DISCARD     

int Fat::doMount(const char *fsPath, const char *mountPoint,
                 bool ro, bool remount, bool executable,
                 int ownerUid, int ownerGid, int permMask, bool createLost) {
	return doMount(fsPath, mountPoint, ro, remount, executable, ownerUid, ownerGid, permMask, createLost, false);
}

int Fat::doMount(const char *fsPath, const char *mountPoint,
                 bool ro, bool remount, bool executable,
                 int ownerUid, int ownerGid, int permMask, bool createLost, bool discard) {
#else  //MTK_EMMC_DISCARD     
int Fat::doMount(const char *fsPath, const char *mountPoint,
                 bool ro, bool remount, bool executable,
                 int ownerUid, int ownerGid, int permMask, bool createLost) {
#endif //MTK_EMMC_DISCARD     
                 	
    int rc;
    unsigned long flags;
    char mountData[255];

    flags = MS_NODEV | MS_NOSUID | MS_DIRSYNC;

    flags |= (executable ? 0 : MS_NOEXEC);
    flags |= (ro ? MS_RDONLY : 0);
    flags |= (remount ? MS_REMOUNT : 0);

    /*
     * Note: This is a temporary hack. If the sampling profiler is enabled,
     * we make the SD card world-writable so any process can write snapshots.
     *
     * TODO: Remove this code once we have a drop box in system_server.
     */
    char value[PROPERTY_VALUE_MAX];
    property_get("persist.sampling_profiler", value, "");
    if (value[0] == '1') {
        SLOGW("The SD card is world-writable because the"
            " 'persist.sampling_profiler' system property is set to '1'.");
        permMask = 0;
    }

#ifdef MTK_EMMC_DISCARD     

    sprintf(mountData,
            "utf8,uid=%d,gid=%d,fmask=%o,dmask=%o,shortname=mixed%s",
            ownerUid, ownerGid, permMask, permMask, (discard? ",discard": "") );

#else //MTK_EMMC_DISCARD    

    sprintf(mountData,
            "utf8,uid=%d,gid=%d,fmask=%o,dmask=%o,shortname=mixed",
            ownerUid, ownerGid, permMask, permMask);

#endif //MTK_EMMC_DISCARD    

    
    rc = mount(fsPath, mountPoint, "vfat", flags, mountData);

    if (rc && errno == EROFS) {
        SLOGE("%s appears to be a read only filesystem - retrying mount RO", fsPath);
        flags |= MS_RDONLY;
        rc = mount(fsPath, mountPoint, "vfat", flags, mountData);
    }

    if (rc == 0 && createLost) {
        char *lost_path;
        asprintf(&lost_path, "%s/LOST.DIR", mountPoint);
        if (access(lost_path, F_OK)) {
            /*
             * Create a LOST.DIR in the root so we have somewhere to put
             * lost cluster chains (fsck_msdos doesn't currently do this)
             */
            if (mkdir(lost_path, 0755)) {
                SLOGE("Unable to create LOST.DIR (%s)", strerror(errno));
            }
        }
        free(lost_path);
    }

    return rc;
}


int Fat::format(const char *fsPath, unsigned int numSectors) {
    int fd;
    const char *args[11];
    int rc;
#ifndef MTK_FORMAT_NOT_PARAM_CLUSTER
    unsigned int bps;
    unsigned int bsize;
#endif
    if (-1 == (fd = open(fsPath, O_RDONLY, 0644)) )
    {
        SLOGE("failed to open %s\n", fsPath);
        errno = EIO;
        return -1;
    }
    args[0] = MKDOSFS_PATH;
    
#ifdef MTK_FORMAT_NOT_PARAM_CLUSTER
    args[1] = "-O";
    args[2] = "android";
    close(fd);
    if (numSectors) {
        char tmp[32];
        snprintf(tmp, sizeof(tmp), "%u", numSectors);
        const char *size = tmp;
        args[3] = "-s";
        args[4] = size;
        args[5] = fsPath;
        args[6] = NULL;
        SLOGD("%s %s %s %s %s %s", args[0], args[1], args[2], args[3], args[4], args[5]);
        rc = logwrap(7, args, 1);
    } else {
        args[3] = fsPath;
        args[4] = NULL;
        SLOGD("%s %s %s %s", args[0], args[1], args[2], args[3]);
        rc = logwrap(5, args, 1);
    }
    
#else
    args[1] = "-F";
   // args[2] = "32";
    if (ioctl(fd, BLKSSZGET, &bps))
    {
        bps = 0;
        SLOGE("failed to get %s bytes/sector\n", fsPath);
    }
    if (ioctl(fd, BLKGETSIZE, &bsize))
    {
        bsize = 0;
        SLOGE("failed to get %s device size\n", fsPath);
    }

    close(fd);
    SLOGD("total cluster is %d", ( (unsigned long long)bsize * 512) / (bps * 8));
    
    if (!numSectors && bps && bsize)
    {
        if ( (((unsigned long long)bsize * 512) / (bps * 8)) > 65536 )
            args[2] = "32";
        else
            args[2] = "16";
    }
    else
        args[2] = "32";

    args[3] = "-O";
    args[4] = "android";
    args[5] = "-c";
    args[6] = "8";

    SLOGD("%s %s %s %s %s %s %s", args[0], args[1], args[2], args[3], args[4], args[5], args[6]);

    if (numSectors) {
        char tmp[32];
        snprintf(tmp, sizeof(tmp), "%u", numSectors);
        const char *size = tmp;
        args[7] = "-s";
        args[8] = size;
        args[9] = fsPath;
        args[10] = NULL;
        rc = logwrap(11, args, 1);
    } else {
        args[7] = fsPath;
        args[8] = NULL;
        rc = logwrap(9, args, 1);
    }
#endif
    if (rc == 0) {
        SLOGI("Filesystem formatted OK");
        return 0;
    } else {
        SLOGE("Format failed (unknown exit code %d)", rc);
        errno = EIO;
        return -1;
    }
    return 0;
}
