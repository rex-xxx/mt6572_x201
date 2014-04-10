#ifndef __MEDIATEK_ENHANCEMENT_H__
#define __MEDIATEK_ENHANCEMENT_H__

#define _USE_X86_DEBUG_ 0
#if _USE_X86_DEBUG_
typedef long long	off64_t;
#define lseek64 lseek

//Set Parameter
#define NUMARG 5
#define PARAM1 "-p"
#define PARAM2 "-f"
#define PARAM3 "-d"
#define PARAM4 "/proj/mtk04301/Perforce/fsck_msdos/img/fat8g.img"

#endif
// ------------------------------------------
// xLog Info
// ------------------------------------------
#include <cutils/xlog.h>
#define FSCK_XLOG_TAG "FSCK"

// ------------------------------------------
// Time Info
// ------------------------------------------
#include <sys/time.h>
extern struct timeval fsck_total_time, fsck_p1_time, fsck_p2_time, fsck_p3_time, fsck_p4_time ;
unsigned long print_time(struct timeval *time) ;
void start_count(struct timeval *) ;
void end_count(const char *, struct timeval *) ;

#endif

