#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>

#include "ext.h"

// ------------------------------------------
// Time Info
// ------------------------------------------
struct timeval fsck_total_time, fsck_p1_time, fsck_p2_time, fsck_p3_time, fsck_p4_time ;

unsigned long print_time(struct timeval *time) 
{
	unsigned long used_time = 1000000 * (time->tv_sec) + (time->tv_usec) ;
	return used_time ;
}

void start_count(struct timeval *start_time)
{
	gettimeofday(start_time,NULL); 
}

void end_count(const char *str, struct timeval *start_time)
{
	struct timeval end_time ;
	gettimeofday(&end_time, NULL) ;
	unsigned long used_time = 1000000 * (end_time.tv_sec - start_time->tv_sec) + (end_time.tv_usec - start_time->tv_usec) ;
	//used_time /= 1000000 ;
	printf("%s took %ld usecs\n", str, used_time) ;

	// Store the value for the final summary.
	start_time->tv_sec = (end_time.tv_sec - start_time->tv_sec) ;
	start_time->tv_usec = (end_time.tv_usec - start_time->tv_usec) ;
}


