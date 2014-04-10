/*
 * Copyright (C) 1995, 1997 Wolfgang Solfrank
 * Copyright (c) 1995 Martin Husemann
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by Martin Husemann
 *	and Wolfgang Solfrank.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include <sys/cdefs.h>
#ifndef lint
__RCSID("$NetBSD: boot.c,v 1.9 2003/07/24 19:25:46 ws Exp $");
static const char rcsid[] =
  "$FreeBSD: src/sbin/fsck_msdosfs/boot.c,v 1.4.28.1 2009/04/15 03:14:26 kensmith Exp $";
#endif /* not lint */

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

#include "ext.h"
#include "fsutil.h"

int
readboot(dosfs, boot)
	int dosfs;
	struct bootblock *boot;
{
	u_char block[DOSBOOTBLOCKSIZE];
	u_char fsinfo[2 * DOSBOOTBLOCKSIZE];
	u_char backup[DOSBOOTBLOCKSIZE];
	int ret = FSOK;
	int n;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "readboot() :");
	
	if ((n = read(dosfs, block, sizeof block)) == -1 || n != sizeof block) {
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - could not read boot block.");
		perror("could not read boot block");
		return FSFATAL;
	}

	if (block[510] != 0x55 || block[511] != 0xaa) {
		pfatal("Invalid signature in boot block: %02x%02x\n", block[511], block[510]);
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "Invalid signature in boot block: %02x%02x\n", block[511], block[510]);
		return FSFATAL;
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
		pfatal("Invalid sector size: %u", boot->BytesPerSec);
		return FSFATAL;
	}	
	
	if (boot->FATs == 0) {
		pfatal("Invalid number of FATs: %u", boot->FATs);
		return FSFATAL;
	}
	

	if (!boot->RootDirEnts)
		boot->flags |= FAT32;
	if (boot->flags & FAT32) {
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "This is FAT32! Now check some FAT32-specific features.");
		boot->FATsecs = block[36] + (block[37] << 8)
				+ (block[38] << 16) + (block[39] << 24);
		if (block[40] & 0x80) {
			boot->ValidFat = block[40] & 0x0f;
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "boot->ValidFat = %d", boot->ValidFat );
		}

		/* check version number: */
		if (block[42] || block[43]) {
			/* Correct?				XXX */
			pfatal("Unknown filesystem version: %x.%x\n",
			       block[43], block[42]);
			return FSFATAL;
		}
		boot->RootCl = block[44] + (block[45] << 8)
			       + (block[46] << 16) + (block[47] << 24);
		boot->FSInfo = block[48] + (block[49] << 8);
		boot->Backup = block[50] + (block[51] << 8);

		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Read FS-Info block.");
		if ((n = lseek(dosfs, boot->FSInfo * boot->BytesPerSec, SEEK_SET)) == -1
		    || n != boot->FSInfo * boot->BytesPerSec
		    || (n = read(dosfs, fsinfo, sizeof fsinfo)) == ((ssize_t)-1)
		    || n != sizeof fsinfo) {
			perror("could not read fsinfo block");
			return FSFATAL;
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
			pwarn("Invalid signature in fsinfo block\n");
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Invalid signature in fsinfo :" );
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0] = %x", fsinfo );
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x1e4] = %x", fsinfo[0x1e4] );
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x1fc] = %x", fsinfo[0x1fc] );
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x1fd] = %x", fsinfo[0x1fd] );			
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x1fe] = %x", fsinfo[0x1fe] );			
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x1ff] = %x", fsinfo[0x1ff] );			
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x3fc] = %x", fsinfo[0x3fc] );
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x3fd] = %x", fsinfo[0x3fd] );			
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x3fe] = %x", fsinfo[0x3fe] );			
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "  fsinfo[0x3ff] = %x", fsinfo[0x3ff] );	

			if (ask(1, "fix")) {
				memcpy(fsinfo, "RRaA", 4);
				memcpy(fsinfo + 0x1e4, "rrAa", 4);
				fsinfo[0x1fc] = fsinfo[0x1fd] = 0;
				fsinfo[0x1fe] = 0x55;
				fsinfo[0x1ff] = 0xaa;
				fsinfo[0x3fc] = fsinfo[0x3fd] = 0;
				fsinfo[0x3fe] = 0x55;
				fsinfo[0x3ff] = 0xaa;
				if ((n = lseek(dosfs, boot->FSInfo * boot->BytesPerSec, SEEK_SET)) == -1
				    || n != boot->FSInfo * boot->BytesPerSec
				    || (n = write(dosfs, fsinfo, sizeof fsinfo)) == ((int)-1)
				    || n != sizeof fsinfo) {
					perror("Unable to write FSInfo");
					return FSFATAL;
				}
				ret = FSBOOTMOD;
			} else
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
			perror("could not read backup bootblock");
			return FSFATAL;
		}
		backup[65] = block[65];				/* XXX */
		
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Compare backup..");		
		if (memcmp(block + 11, backup + 11, 79)) {
                        char tmp[255];
                        int i;

			/*
			 * For now, lets not bail out if they don't match
			 * It seems a lot of sdcards are formatted with
			 * the backup either empty or containing garbage.
			 */

			pwarn("Primary/Backup bootblock miscompare\n");

                        strcpy(tmp, "");
                        pwarn("Primary:\n");
			for (i = 0; i < 79; i++) {
				char tmp2[16];
                                snprintf(tmp2, sizeof(tmp2), "%.2x ", block[11 + i]);
				strcat(tmp, tmp2);
                        }
                        pwarn("%s\n", tmp);

			strcpy(tmp, "");
                        pwarn("Backup:\n");
			for (i = 0; i < 79; i++) {
				char tmp2[16];
                                snprintf(tmp2, sizeof(tmp2), "%.2x ", backup[11 + i]);
				strcat(tmp, tmp2);
                        }
                        pwarn("%s\n", tmp);
		}
		/* Check backup FSInfo?					XXX */
	}

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - fill other info to struct bootblock") ;		

	boot->ClusterOffset = (boot->RootDirEnts * 32 + boot->BytesPerSec - 1)
	    / boot->BytesPerSec
	    + boot->ResSectors
	    + boot->FATs * boot->FATsecs
	    - CLUST_FIRST * boot->SecPerClust;

	if (boot->BytesPerSec % DOSBOOTBLOCKSIZE != 0) {
		pfatal("Invalid sector size: %u", boot->BytesPerSec);
		return FSFATAL;
	}
	if (boot->SecPerClust == 0) {
		pfatal("Invalid cluster size: %u", boot->SecPerClust);
		return FSFATAL;
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
		pfatal("Filesystem too big (%u clusters) for non-FAT32 partition\n",
		       boot->NumClusters);
		return FSFATAL;
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
		pfatal("FAT size too small, %u entries won't fit into %u sectors\n",
		       boot->NumClusters, boot->FATsecs);
		return FSFATAL;
	}
	boot->ClusterSize = boot->BytesPerSec * boot->SecPerClust;

	boot->NumFiles = 1;
	boot->NumFree = 0;
	
	if (debugmessage) {
		printf("\n\n-----------------------------------------------------------");
		printf("\n   Boot Sector ");
		printf("\n------------------------------------------------------------");
		printf("\n BytesPerSec (bytes per sector) : %u ", boot->BytesPerSec);
		printf("\n SecPerClust (sectors per cluster) : %u ", boot->SecPerClust);
		printf("\n ResSectors (number of reserved sectors) : %u ", boot->ResSectors);
		printf("\n FATs (number of FATs) : %u ", boot->FATs);
		printf("\n RootDirEnts (number of root directory entries) : %u ", boot->RootDirEnts);
		printf("\n Media (media descriptor) : 0x%x ", boot->Media);
		printf("\n FATsmall (number of sectors per FAT16) : %u ", boot->FATsmall);
		printf("\n SecPerTrack (sectors per track) : %u ", boot->SecPerTrack);
		printf("\n Heads (number of heads) : %u ", boot->Heads);
		printf("\n Sectors (total number of sectors) : %u ", boot->Sectors);
		printf("\n HiddenSecs (# of hidden sectors) : %u ", boot->HiddenSecs);
		printf("\n HugeSectors (# of sectors if bpbSectors == 0) : 0x%x ", boot->HugeSectors);
		printf("\n FSInfo (FSInfo sector) : 0x%x ", boot->FSInfo);
		printf("\n Backup (Backup of Bootblocks) : 0x%x ", boot->Backup);
		printf("\n RootCl (Start of Root Directory) : 0x%x ", boot->RootCl);
		printf("\n FSFree (Number of free clusters acc. FSInfo) : 0x%x ", boot->FSFree);
		printf("\n FSNext (Next free cluster acc. FSInfo) : 0x%x ", boot->FSNext);
		printf("\n------------------------------------------------------------");
		printf("\n ClustMask (FAT12:fff, FAT16:ffff, FAT32:fffffff) : 0x%x ", boot->ClustMask);
		printf("\n NumClusters (cluster numbers) : %u ", boot->NumClusters);
		printf("\n NumSectors (how many sectors are there) : %u ", boot->NumSectors);
		printf("\n FATsecs (how many sectors are in FAT) : %u ", boot->FATsecs);
		printf("\n NumFatEntries (Max entry # in the FAT) : %u ", boot->NumFatEntries);
		printf("\n ClusterOffset (at what sector would sector 0 start) : %u ", boot->ClusterOffset);
		printf("\n ClusterSize (Cluster size in bytes) : %u ", boot->ClusterSize);
		printf("\n------------------------------------------------------------\n");
	}

	return ret;
}

int
writefsinfo(dosfs, boot)
	int dosfs;
	struct bootblock *boot;
{
	u_char fsinfo[2 * DOSBOOTBLOCKSIZE];
	int n;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "writefsinfo()"); 

	if ((n = lseek(dosfs, boot->FSInfo * boot->BytesPerSec, SEEK_SET)) == -1
	    || n != boot->FSInfo * boot->BytesPerSec
	    || (n = read(dosfs, fsinfo, sizeof fsinfo)) == ((ssize_t)-1)
	    || n != sizeof fsinfo) {
		perror("could not read fsinfo block");
		return FSFATAL;
	}
	fsinfo[0x1e8] = (u_char)boot->FSFree;
	fsinfo[0x1e9] = (u_char)(boot->FSFree >> 8);
	fsinfo[0x1ea] = (u_char)(boot->FSFree >> 16);
	fsinfo[0x1eb] = (u_char)(boot->FSFree >> 24);
	fsinfo[0x1ec] = (u_char)boot->FSNext;
	fsinfo[0x1ed] = (u_char)(boot->FSNext >> 8);
	fsinfo[0x1ee] = (u_char)(boot->FSNext >> 16);
	fsinfo[0x1ef] = (u_char)(boot->FSNext >> 24);
	if ((n = lseek(dosfs, boot->FSInfo * boot->BytesPerSec, SEEK_SET)) == -1
	    || n != boot->FSInfo * boot->BytesPerSec
	    || (n = write(dosfs, fsinfo, sizeof fsinfo)) == ((int)-1)
	    || n != sizeof fsinfo) {
		perror("Unable to write FSInfo");
		return FSFATAL;
	}
	/*
	 * Technically, we should return FSBOOTMOD here.
	 *
	 * However, since Win95 OSR2 (the first M$ OS that has
	 * support for FAT32) doesn't maintain the FSINFO block
	 * correctly, it has to be fixed pretty often.
	 *
	 * Therefor, we handle the FSINFO block only informally,
	 * fixing it if necessary, but otherwise ignoring the
	 * fact that it was incorrect.
	 */
	return 0;
}
