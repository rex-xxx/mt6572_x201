/*
 * Copyright (C) 1995, 1996, 1997 Wolfgang Solfrank
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
__RCSID("$NetBSD: fat.c,v 1.12 2000/10/10 20:24:52 is Exp $");
static const char rcsid[] =
  "$FreeBSD: src/sbin/fsck_msdosfs/fat.c,v 1.9 2008/01/31 13:22:13 yar Exp $";
#endif /* not lint */

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

#include "ext.h"
#include "fsutil.h"

static int checkclnum(struct bootblock *, int, cl_t, cl_t *);
static int clustdiffer(cl_t, cl_t *, cl_t *, int);
static int tryclear(struct bootblock *, struct fatEntry *, cl_t, cl_t *);

/*-
 * The first 2 FAT entries contain pseudo-cluster numbers with the following
 * layout:
 *
 * 31...... ........ ........ .......0
 * rrrr1111 11111111 11111111 mmmmmmmm         FAT32 entry 0
 * rrrrsh11 11111111 11111111 11111xxx         FAT32 entry 1
 * 
 *                   11111111 mmmmmmmm         FAT16 entry 0
 *                   sh111111 11111xxx         FAT16 entry 1
 * 
 * r = reserved
 * m = BPB media ID byte
 * s = clean flag (1 = dismounted; 0 = still mounted)
 * h = hard error flag (1 = ok; 0 = I/O error)
 * x = any value ok
 */

int
checkdirty(int fs, struct bootblock *boot)
{
	off_t off;
	u_char *buffer;
	int ret = 0;

	if (boot->ClustMask != CLUST16_MASK && boot->ClustMask != CLUST32_MASK)
		return 0;

	off = boot->ResSectors;
	off *= boot->BytesPerSec;

	buffer = malloc(boot->BytesPerSec);
	if (buffer == NULL) {
		perror("checkdirty: No space for FAT");
		return 1;
	}

	if (lseek(fs, off, SEEK_SET) != off) {
		perror("checkdirty: lseek error");
		goto err;
	}

	if (read(fs, buffer, boot->BytesPerSec) != (int)boot->BytesPerSec) {
		perror("checkdirty: Unable to read FAT");
		goto err;
	}

	/*
	 * If we don't understand the FAT, then the file system must be
	 * assumed to be unclean.
	 */
	if (buffer[0] != boot->Media || buffer[1] != 0xff) {
		printf("checkdirty() : buffer[0](%x) != boot->Media(%x) || buffer[1](%x) != 0xff\n", buffer[0], boot->Media, buffer[1]) ;
		goto err;
	}
	if (boot->ClustMask == CLUST16_MASK) {
		if ((buffer[2] & 0xf8) != 0xf8 || (buffer[3] & 0x3f) != 0x3f) {
			printf("checkdirty() : (buffer[2](%x) & 0xf8) != 0xf8 || (buffer[3](%x) & 0x3f) != 0x3f\n", buffer[2], buffer[3]) ;
			goto err;
		}
	} else {
		if (buffer[2] != 0xff || (buffer[3] & 0x0f) != 0x0f
		    || (buffer[4] & 0xf8) != 0xf8 || buffer[5] != 0xff
		    || buffer[6] != 0xff || (buffer[7] & 0x03) != 0x03) {
		    printf("checkdirty() : buffer[2](%x) != 0xff || (buffer[3](%x) & 0x0f) != 0x0f"
				"|| (buffer[4](%x) & 0xf8) != 0xf8 || buffer[5](%x) != 0xff"
				"|| buffer[6](%x) != 0xff || (buffer[7](%x) & 0x03) != 0x03\n", 
				buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]) ;
			goto err;
		}
	}

	/*
	 * Now check the actual clean flag (and the no-error flag).
	 */
	if (boot->ClustMask == CLUST16_MASK) {
		if ((buffer[3] & 0xc0) == 0xc0)
			ret = 1;
		else
			printf("buffer[3](%x) & 0xC0 != 0xC0\n", buffer[3]) ;
	} else {
		if ((buffer[7] & 0x0c) == 0x0c)
			ret = 1;
		else
			printf("buffer[7](%x) & 0x0C != 0x0C\n", buffer[7]) ;
	}

err:
	free(buffer);
	return ret;
}

/*
 * Check a cluster number for valid value
 */
static int
checkclnum(struct bootblock *boot, int fat, cl_t cl, cl_t *next)
{
	if (*next >= (CLUST_RSRVD&boot->ClustMask))
		*next |= ~boot->ClustMask;
	if (*next == CLUST_FREE) {
		boot->NumFree++;
		return FSOK;
	}
	if (*next == CLUST_BAD) {
		boot->NumBad++;
		return FSOK;
	}
	if (*next < CLUST_FIRST
	    || (*next >= boot->NumClusters && *next < CLUST_EOFS)) {
		pwarn("Cluster %u in FAT %d continues with %s cluster number %u\n",
		      cl, fat,
		      *next < CLUST_RSRVD ? "out of range" : "reserved",
		      *next&boot->ClustMask);
		if (ask(1, "Truncate")) {
			*next = CLUST_EOF;
			return FSFATMOD;
		}
		perror("[ERR] without any fix !\n") ;
		return FSERROR;
	}
	return FSOK;
}

/*
 * Get system free memory
 */
static int getfreemem(void)
{
	FILE *meminfo;
	char aux[256];
	unsigned long free, cached;
 
	if(!(meminfo = fopen("/proc/meminfo", "r")))
		printf("\n open /proc/meminfo failed ! \n");

	free = cached = 0;
	while( !feof(meminfo) && !fscanf(meminfo, "MemFree: %lu kB", &free) )
            fgets(aux,sizeof(aux),meminfo);
	while( !feof(meminfo) && !fscanf(meminfo, "Cached: %lu kB", &cached) )
            fgets(aux,sizeof(aux),meminfo);
	fclose(meminfo);

	printf("System MemFree: %lu kB, Cached: %lu kB \n", free, cached);
	return (free + cached); // size in KB
}

// 6144 x 512 = 3MB, this need to be multiple of 3 because FAT12 use 1.5 byte per entry
#define FAT_READ_MAX_SEC	6144
#define MORE_SAVE_MEMORY    30000 //Size in KB
/*
 * Read a FAT and decode it into internal format
 */
int
readfat(int fs, struct bootblock *boot, int no, struct fatEntry **fp)
{
	struct fatEntry *fat;
	u_char *buffer, *p;
	cl_t cl;
	off_t off;
	int ret = FSOK;
	int firstFAT=1, curFATsec=0, readFATsec;
	cl_t cl_start, cl_end;
	int FATSize, fatEntrySize;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "readfat()") ;

	boot->NumFree = boot->NumBad = 0;
	// Check if allocate too many memory
	if (boot->FATsecs > FAT_READ_MAX_SEC) {
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - boot->FATsecs(%d) > FAT_READ_MAX_SEC(%d)", boot->FATsecs, FAT_READ_MAX_SEC);
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - So readFATsec uses FAT_READ_MAX_SEC(%d)", FAT_READ_MAX_SEC);
		readFATsec = FAT_READ_MAX_SEC;
	} else {				
		readFATsec = boot->FATsecs;
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - readFATsec = %d", readFATsec) ;
	}

	FATSize = (readFATsec * boot->BytesPerSec) >> 10;
	fatEntrySize = (boot->NumClusters * sizeof(struct fatEntry)) >> 10;
	printf("\nAttempting to allocate %d KB for FAT \n", FATSize);
	printf("Allocate for fatEntry (%u x %u) = %u KB \n", 
	        boot->NumClusters, sizeof(struct fatEntry), fatEntrySize);

	if ( (FATSize+fatEntrySize) > (getfreemem() - MORE_SAVE_MEMORY)) {
		printf("\nSystem memory is not enough for reading FAT !!! \n");
		printf("WARNING : fsck_msdos is skipped ! This has potential risk !!! \n");
		exit(0);
	}

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Try to allocate buffer/fat.") ;
	buffer = calloc(readFATsec, boot->BytesPerSec);
	fat = calloc(boot->NumClusters, sizeof(struct fatEntry));
	if (fat == NULL || buffer == NULL) {
		if (fat != NULL)
			free(fat);
		else
			perror("Allocate fatEntry[] -- failed !\n") ;
		
		if (buffer != NULL)
			free(buffer);
		else
			perror("Allocate a buffer for FAT table -- failed !\n") ;
		
		return FSFATAL;
	}
	
	while(curFATsec < (int)boot->FATsecs) 
	{
		off = boot->ResSectors + no * boot->FATsecs + curFATsec;
		off *= boot->BytesPerSec;
		
		if (debugmessage)
			printf("\n readFATsec=%d, curFATsec=%d, off=%d ", readFATsec, curFATsec, (int)off);

		if (lseek(fs, off, SEEK_SET) != off) {
			perror("readfat: lseek error");
			free(buffer);
			free(fat);
			return FSFATAL;
		}

		if (read(fs, buffer, readFATsec * boot->BytesPerSec)
		    != (int)(readFATsec * boot->BytesPerSec)) {
			perror("readfat: read FAT error");
			free(buffer);
			free(fat);
			return FSFATAL;
		}

		// only check for first FAT entry for media type and dirty bits
		if (firstFAT) {
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "1st FAT, check media-type and dirty-bits") ;;
			if (buffer[0] != boot->Media
			    || buffer[1] != 0xff || buffer[2] != 0xff
			    || (boot->ClustMask == CLUST16_MASK && buffer[3] != 0xff)
			    || (boot->ClustMask == CLUST32_MASK
				&& ((buffer[3]&0x0f) != 0x0f
		    	|| buffer[4] != 0xff || buffer[5] != 0xff
		    	|| buffer[6] != 0xff || (buffer[7]&0x0f) != 0x0f))) {

				/* Windows 95 OSR2 (and possibly any later) changes
				 * the FAT signature to 0xXXffff7f for FAT16 and to
				 * 0xXXffff0fffffff07 for FAT32 upon boot, to know that the
				 * file system is dirty if it doesn't reboot cleanly.
				 * Check this special condition before errorring out.
				 */
				if (buffer[0] == boot->Media && buffer[1] == 0xff
				    && buffer[2] == 0xff
				    && ((boot->ClustMask == CLUST16_MASK && buffer[3] == 0x7f)
					|| (boot->ClustMask == CLUST32_MASK
				    && buffer[3] == 0x0f && buffer[4] == 0xff
				    && buffer[5] == 0xff && buffer[6] == 0xff
				    && buffer[7] == 0x07))) {
						ret |= FSDIRTY;
						printf("Dirty media-type info\n") ;
				} else {
					/* just some odd byte sequence in FAT */
				
					switch (boot->ClustMask) {
					case CLUST32_MASK:
						pwarn("%s (%02x%02x%02x%02x%02x%02x%02x%02x)\n",
						      "FAT starts with odd byte sequence",
						      buffer[0], buffer[1], buffer[2], buffer[3],
						      buffer[4], buffer[5], buffer[6], buffer[7]);
						break;
					case CLUST16_MASK:
						pwarn("%s (%02x%02x%02x%02x)\n",
						    "FAT starts with odd byte sequence",
						    buffer[0], buffer[1], buffer[2], buffer[3]);
						break;
					default:
						pwarn("%s (%02x%02x%02x)\n",
						    "FAT starts with odd byte sequence",
						    buffer[0], buffer[1], buffer[2]);
						break;
					}
	
					if (ask(1, "Correct"))
						ret |= FSFIXFAT;
				}
			}

			cl_start = CLUST_FIRST;
			switch (boot->ClustMask) {
			case CLUST32_MASK:
				p = buffer + 8;
				cl_end = readFATsec * 128;	// 512/4=128
				break;
			case CLUST16_MASK:
				p = buffer + 4;
				cl_end = readFATsec * 256;	// 512/2=256
				break;
			default:
				p = buffer + 3;
				cl_end = readFATsec * 1024 / 3;	// readFATsec must be multiple of 3
				break;
			}
			firstFAT = 0;
		} else {
			// not first time to read FAT, do not have to skip two entry
			p = buffer;
			
			switch (boot->ClustMask) {
			case CLUST32_MASK:
				cl_start = curFATsec * 128;
				cl_end = (curFATsec + readFATsec) * 128;
				break;
			case CLUST16_MASK:
				cl_start = curFATsec * 256;
				cl_end = (curFATsec + readFATsec) * 256;
				break;
			default:
				cl_start = curFATsec * 1024 / 3;
				cl_end = (curFATsec + readFATsec) * 1024 / 3;
				break;
			}
		}
		if (cl_end > boot->NumClusters)
			cl_end = boot->NumClusters;
		
		if (debugmessage)
			printf("\n cl_start=%u, cl_end=%u \n", cl_start, cl_end);

		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Read next-value to fatEntry[] %x ~ %x", cl_start, cl_end) ;
		for (cl = cl_start; cl < cl_end;) {
			switch (boot->ClustMask) {
			case CLUST32_MASK:
				fat[cl].next = p[0] + (p[1] << 8)
				       + (p[2] << 16) + (p[3] << 24);
				fat[cl].next &= boot->ClustMask;
				ret |= checkclnum(boot, no, cl, &fat[cl].next);
				cl++;
				p += 4;
				break;
			case CLUST16_MASK:
				fat[cl].next = p[0] + (p[1] << 8);
				ret |= checkclnum(boot, no, cl, &fat[cl].next);
				cl++;
				p += 2;
				break;
			default:
				fat[cl].next = (p[0] + (p[1] << 8)) & 0x0fff;
				ret |= checkclnum(boot, no, cl, &fat[cl].next);
				cl++;
				if (cl >= boot->NumClusters)
					break;
				fat[cl].next = ((p[1] >> 4) + (p[2] << 4)) & 0x0fff;
				ret |= checkclnum(boot, no, cl, &fat[cl].next);
				cl++;
				p += 3;
				break;
			}
		}	// end of for loop
		
		curFATsec += FAT_READ_MAX_SEC;
		if ((curFATsec+readFATsec) > (int)boot->FATsecs) {
			readFATsec = boot->FATsecs - curFATsec;
		}
		
	}	// end of while(curFATsec < boot->FATsecs) 

	free(buffer);
	if (ret & FSFATAL) {
		free(fat);
		*fp = NULL;
	} else
		*fp = fat;
	
	return ret;
}

/*
 * Get type of reserved cluster
 */
char *
rsrvdcltype(cl_t cl)
{
	if (cl == CLUST_FREE)
		return ("free");
	if (cl < CLUST_BAD)
		return ("reserved");
	if (cl > CLUST_BAD)
		return ("as EOF");
	return ("bad");
}

static int
clustdiffer(cl_t cl, cl_t *cp1, cl_t *cp2, int fatnum)
{
	if (*cp1 == CLUST_FREE || *cp1 >= CLUST_RSRVD) {
		if (*cp2 == CLUST_FREE || *cp2 >= CLUST_RSRVD) {
			if ((*cp1 != CLUST_FREE && *cp1 < CLUST_BAD
			     && *cp2 != CLUST_FREE && *cp2 < CLUST_BAD)
			    || (*cp1 > CLUST_BAD && *cp2 > CLUST_BAD)) {
				pwarn("Cluster %u is marked %s with different indicators\n",
				      cl, rsrvdcltype(*cp1));
				if (ask(1, "Fix")) {
					*cp2 = *cp1;
					return FSFATMOD;
				}
				printf("[ERR] Without any fix !\n") ;
				return FSFATAL;
			}
			pwarn("Cluster %u is marked %s in FAT 0, %s in FAT %d\n",
			      cl, rsrvdcltype(*cp1), rsrvdcltype(*cp2), fatnum);
			if (ask(1, "Use FAT 0's entry")) {
				*cp2 = *cp1;
				return FSFATMOD;
			}
			if (ask(1, "Use FAT %d's entry", fatnum)) {
				*cp1 = *cp2;
				return FSFATMOD;
			}
			printf("[ERR] Without any fix !\n") ;
			return FSFATAL;
		}
		pwarn("Cluster %u is marked %s in FAT 0, but continues with cluster %u in FAT %d\n",
		      cl, rsrvdcltype(*cp1), *cp2, fatnum);
		if (ask(1, "Use continuation from FAT %d", fatnum)) {
			*cp1 = *cp2;
			return FSFATMOD;
		}
		if (ask(1, "Use mark from FAT 0")) {
			*cp2 = *cp1;
			return FSFATMOD;
		}
		printf("[ERR] Without any fix !\n") ;
		return FSFATAL;
	}
	if (*cp2 == CLUST_FREE || *cp2 >= CLUST_RSRVD) {
		pwarn("Cluster %u continues with cluster %u in FAT 0, but is marked %s in FAT %d\n",
		      cl, *cp1, rsrvdcltype(*cp2), fatnum);
		if (ask(1, "Use continuation from FAT 0")) {
			*cp2 = *cp1;
			return FSFATMOD;
		}
		if (ask(1, "Use mark from FAT %d", fatnum)) {
			*cp1 = *cp2;
			return FSFATMOD;
		}
		printf("[ERR] Without any fix !\n") ;
		return FSERROR;
	}
	pwarn("Cluster %u continues with cluster %u in FAT 0, but with cluster %u in FAT %d\n",
	      cl, *cp1, *cp2, fatnum);
	if (ask(1, "Use continuation from FAT 0")) {
		*cp2 = *cp1;
		return FSFATMOD;
	}
	if (ask(1, "Use continuation from FAT %d", fatnum)) {
		*cp1 = *cp2;
		return FSFATMOD;
	}
	printf("[ERR] Without any fix !\n") ;
	return FSERROR;
}

/*
 * Compare two FAT copies in memory. Resolve any conflicts and merge them
 * into the first one.
 */
int
comparefat(struct bootblock *boot, struct fatEntry *first, 
    struct fatEntry *second, int fatnum)
{
	cl_t cl;
	int ret = FSOK;

	for (cl = CLUST_FIRST; cl < boot->NumClusters; cl++)
		if (first[cl].next != second[cl].next) {
			xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "first[cl(%x)].next(%x) != second[cl].next(%x)", cl, first[cl].next, second[cl].next);
			ret |= clustdiffer(cl, &first[cl].next, &second[cl].next, fatnum);
		}
	return ret;
}

void
clearchain(struct bootblock *boot, struct fatEntry *fat, cl_t head)
{
	cl_t p, q;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "clearchain() is called! Clear from head(%x)..", head);

	for (p = head; p >= CLUST_FIRST && p < boot->NumClusters; p = q) {
		xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - clear cluster#%x", p);
		if (fat[p].head != head)
			break;
		q = fat[p].next;
		fat[p].next = fat[p].head = CLUST_FREE;
		fat[p].length = 0;
	}
}

int
tryclear(struct bootblock *boot, struct fatEntry *fat, cl_t head, cl_t *trunc)
{
	if (ask(1, "Clear chain starting at %u", head)) {
		clearchain(boot, fat, head);
		return FSFATMOD;
	} else if (ask(1, "Truncate")) {
		*trunc = CLUST_EOF;
		return FSFATMOD;
	} else {
		printf("tryclear() : err! neither clearhchain() nor truncate!\n") ;
		return FSERROR;
	}
}

/*
 * Check a complete FAT in-memory for crosslinks
 */
int
checkfat(struct bootblock *boot, struct fatEntry *fat)
{
	cl_t head, p, h, n;
	u_int len;
	int ret = 0;
	int conf;
	cl_t loop_cnt;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "checkfat()") ;

	/*
	 * pass 1: figure out the cluster chains.
	 */
	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Figure out the cluster chains.") ;
	for (head = CLUST_FIRST; head < boot->NumClusters; head++) {
		/* find next untravelled chain */
		if (fat[head].head != 0		/* cluster already belongs to some chain */
		    || fat[head].next == CLUST_FREE
		    || fat[head].next == CLUST_BAD)
			continue;		/* skip it. */

		/* follow the chain and mark all clusters on the way */
		for (len = 0, p = head;
		     p >= CLUST_FIRST && p < boot->NumClusters && fat[p].head != head;
			 p = fat[p].next) {
				/* we have to check the len, to avoid infinite loop */
				if (len > boot->NumClusters) {
					printf("detect cluster chain loop: head %u for p %u\n", head, p);
					break;
			}

			fat[p].head = head;
			len++;
		}

		/* the head record gets the length */
		fat[head].length = fat[head].next == CLUST_FREE ? 0 : len;
	}

	/*
	 * pass 2: check for crosslinked chains (we couldn't do this in pass 1 because
	 * we didn't know the real start of the chain then - would have treated partial
	 * chains as interlinked with their main chain)
	 */
	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - Check for crosslinked chains") ;
	for (head = CLUST_FIRST; head < boot->NumClusters; head++) {
		/* find next untravelled chain */
		if (fat[head].head != head)
			continue;

		loop_cnt = 0;
		/* follow the chain to its end (hopefully) */
		for (p = head;
		     (n = fat[p].next) >= CLUST_FIRST && n < boot->NumClusters;
		     p = n) {
			// Check for incorrect backward link which may cause infinite loop
			if ((n == head) || (n == p)) {
				pwarn("Incorrect link : head = %u, fat[%u].next=%u \n",
			      	head, p, n);
				break;
			}
			loop_cnt++;
			if (loop_cnt > boot->NumClusters) {
				pwarn("ERROR : infinite loop : head = %u \n", head);
				break;
			}
			if (fat[n].head != head)
				break;
		}
		if (n >= CLUST_EOFS)
			continue;

		if (n == CLUST_FREE || n >= CLUST_RSRVD) {
			pwarn("Cluster chain starting at %u ends with cluster marked %s\n",
			      head, rsrvdcltype(n));
			ret |= tryclear(boot, fat, head, &fat[p].next);
			continue;
		}
		if (n < CLUST_FIRST || n >= boot->NumClusters) {
			pwarn("Cluster chain starting at %u ends with cluster out of range (%u)\n",
			      head, n);
			ret |= tryclear(boot, fat, head, &fat[p].next);
			continue;
		}
		pwarn("Cluster chains starting at %u and %u are linked at cluster %u\n",
		      head, fat[n].head, n);
		conf = tryclear(boot, fat, head, &fat[p].next);
		if (ask(1, "Clear chain starting at %u", h = fat[n].head)) {
			if (conf == FSERROR) {
				/*
				 * Transfer the common chain to the one not cleared above.
				 */
				for (p = n;
				     p >= CLUST_FIRST && p < boot->NumClusters;
				     p = fat[p].next) {
					if (h != fat[p].head) {
						/*
						 * Have to reexamine this chain.
						 */
						printf("Re-examine this chain!\n") ; 
						head--;
						break;
					}
					fat[p].head = head;
				}
			}
			clearchain(boot, fat, h);
			conf |= FSFATMOD;
		}
		ret |= conf;
	}

	return ret;
}

/*
 * Write out FATs encoding them from the internal format
 */
int
writefat(int fs, struct bootblock *boot, struct fatEntry *fat, int correct_fat)
{
	u_char *buffer, *p;
	cl_t cl;
	u_int i;
	size_t fatsz;
	off_t off;
	int ret = FSOK;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "writefat()") ;
	
	if(!correct_fat)
	{
		pwarn("corrct_fat = 0 \n");
	}

	buffer = malloc(fatsz = boot->FATsecs * boot->BytesPerSec);
	if (buffer == NULL) {
		perror("No space for FAT");
		return FSFATAL;
	}
	memset(buffer, 0, fatsz);
	boot->NumFree = 0;
	p = buffer;
	*p++ = (u_char)boot->Media;
	*p++ = 0xff;
	*p++ = 0xff;
	switch (boot->ClustMask) {
	case CLUST16_MASK:
		*p++ = 0xff;
		break;
	case CLUST32_MASK:
		*p++ = 0x0f;
		*p++ = 0xff;
		*p++ = 0xff;
		*p++ = 0xff;
		*p++ = 0x0f;
		break;
	}
	for (cl = CLUST_FIRST; cl < boot->NumClusters; cl++) {
		switch (boot->ClustMask) {
		case CLUST32_MASK:
			if (fat[cl].next == CLUST_FREE)
				boot->NumFree++;
			*p++ = (u_char)fat[cl].next;
			*p++ = (u_char)(fat[cl].next >> 8);
			*p++ = (u_char)(fat[cl].next >> 16);
			*p &= 0xf0;
			*p++ |= (fat[cl].next >> 24)&0x0f;
			break;
		case CLUST16_MASK:
			if (fat[cl].next == CLUST_FREE)
				boot->NumFree++;
			*p++ = (u_char)fat[cl].next;
			*p++ = (u_char)(fat[cl].next >> 8);
			break;
		default:
			if (fat[cl].next == CLUST_FREE)
				boot->NumFree++;
			if (cl + 1 < boot->NumClusters
			    && fat[cl + 1].next == CLUST_FREE)
				boot->NumFree++;
			*p++ = (u_char)fat[cl].next;
			*p++ = (u_char)((fat[cl].next >> 8) & 0xf)
			       |(u_char)(fat[cl+1].next << 4);
			*p++ = (u_char)(fat[++cl].next >> 4);
			break;
		}
	}
	for (i = 0; i < boot->FATs; i++) {
		off = boot->ResSectors + i * boot->FATsecs;
		off *= boot->BytesPerSec;
		if (lseek(fs, off, SEEK_SET) != off
		    || write(fs, buffer, (int)fatsz) != (int)fatsz) {
			perror("Unable to write FAT");
			ret = FSFATAL; /* Return immediately?		XXX */
		}
	}
	free(buffer);
	return ret;
}

/*
 * Check a complete in-memory FAT for lost cluster chains
 */
int
checklost(int dosfs, struct bootblock *boot, struct fatEntry *fat)
{
	cl_t head;
	int mod = FSOK;
	int ret;

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, "checklost()") ;
	
	for (head = CLUST_FIRST; head < boot->NumClusters; head++) {
		/* find next untravelled chain */
		if (fat[head].head != head
		    || fat[head].next == CLUST_FREE
		    || (fat[head].next >= CLUST_RSRVD
			&& fat[head].next < CLUST_EOFS)
		    || (fat[head].flags & FAT_USED))
			continue;

		pwarn("Lost cluster chain at cluster %u\n%d Cluster(s) lost\n",
		      head, fat[head].length);
		mod |= ret = reconnect(dosfs, boot, fat, head);
		if (mod & FSFATAL) {
			/* If the reconnect failed, then just clear the chain */
			pwarn("Error reconnecting chain - clearing\n");
			mod &= ~FSFATAL;
			clearchain(boot, fat, head);
			mod |= FSFATMOD;
			continue;
		}
		if (ret == FSERROR && ask(1, "Clear")) {
			pwarn("Error in reconnect - clearing\n");
			mod &= ~FSERROR;
			clearchain(boot, fat, head);
			mod |= FSFATMOD;
		}
	}
	finishlf();

	xlog_printf(ANDROID_LOG_INFO, FSCK_XLOG_TAG, " - check boot->FSInfo") ;
	if (boot->FSInfo) {
		ret = 0;
		if (boot->FSFree != boot->NumFree) {
			pwarn("Free space in FSInfo block (%d) not correct (%d)\n",
			      boot->FSFree, boot->NumFree);
			if (ask(1, "Fix")) {
				boot->FSFree = boot->NumFree;
				ret = 1;
			}
		}

		if (boot->NumFree) {
			if ((boot->FSNext >= boot->NumClusters) || (fat[boot->FSNext].next != CLUST_FREE)) {
				pwarn("Next free cluster in FSInfo block (%u) not free\n",
				      boot->FSNext);
				if (ask(1, "Fix"))
					for (head = CLUST_FIRST; head < boot->NumClusters; head++)
						if (fat[head].next == CLUST_FREE) {
							boot->FSNext = head;
							ret = 1;
							break;
						}
			}
        }


		if (boot->FSNext > boot->NumClusters  ) {
			pwarn("FSNext block (%d) not correct NumClusters (%d)\n",
					boot->FSNext, boot->NumClusters);
			boot->FSNext=CLUST_FIRST; // boot->FSNext can have -1 value.
		}

		if (boot->NumFree && fat[boot->FSNext].next != CLUST_FREE) {
			pwarn("Next free cluster in FSInfo block (%u) not free\n",
					boot->FSNext);
			if (ask(1, "Fix"))
				for (head = CLUST_FIRST; head < boot->NumClusters; head++)
					if (fat[head].next == CLUST_FREE) {
						boot->FSNext = head;
						ret = 1;
						break;
					}
		}
		if (ret)
			mod |= writefsinfo(dosfs, boot);
	}

	return mod;
}
