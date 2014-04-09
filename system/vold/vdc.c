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
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>

#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/un.h>

#include <cutils/sockets.h>
#include <private/android_filesystem_config.h>

#ifdef MTK_FAT_ON_NAND
#include "fat_on_nand.h"
#include <sys/statfs.h>
#include <cutils/properties.h>

#define RESERVE_SIZE    (1*1024*1024)//actually maybe 70KB, but reserve more for safety
#endif 

static void usage(char *progname);
static int do_monitor(int sock, int stop_after_cmd);
static int do_cmd(int sock, int argc, char **argv);

int main(int argc, char **argv) {
    int sock;

    if (argc < 2)
        usage(argv[0]);
#ifdef MTK_FAT_ON_NAND
    if (!strcmp(argv[1], "fatcreation"))
    {
        char fatimgfilepath[200];
        struct statfs st;
        char first_boot[PROPERTY_VALUE_MAX];
        const char *fat_mnt;
        const char *fat_filename;
        pid_t   pid;
        
        fat_mnt = FAT_PARTITION_MOUNT_POINT;
        fat_filename = FAT_IMG_NAME;
        
        snprintf(fatimgfilepath, sizeof(fatimgfilepath), "%s/%s", fat_mnt, fat_filename);
        memset(first_boot, 0x0, sizeof(first_boot));
        property_get("persist.fat_first_boot", first_boot, "1");
        printf("first_boot:%s\n", first_boot);
        if (!strcmp(first_boot, "1")) {
            if(access(fatimgfilepath, F_OK) != 0)
            {
                printf("%s does not exist, create FAT image\n", fatimgfilepath);
                if (statfs(fat_mnt, &st) < 0) {
                    printf("get fs stat fail(%s) %s(%d)\n", fat_mnt, strerror(errno), errno);
                }
                else{
                    printf("%s free space:%llu f_bfree:%llu f_bsize:%u\n", fat_mnt, (long long)((long long)st.f_bfree * (long long)st.f_bsize), st.f_bfree, st.f_bsize);
                    if((long long)((long long)st.f_bfree * (long long)st.f_bsize) < (long long)FAT_IMG_MIN){
                        printf("WARNING:%s free space:%llu, FAT_IMG_MIN:%d\n", fat_mnt, (long long)((long long)st.f_bfree * (long long)st.f_bsize), FAT_IMG_MIN);
                    }
                    else{

                        int flag = O_RDWR|O_CREAT;
                        int fatimgfd;
CREAT_FAT_IMG:
                        fatimgfd = open(fatimgfilepath, flag, S_IRUSR|S_IWUSR);
                        if(fatimgfd < 0){
                            printf("Fail to create %s %s(%d)", fatimgfilepath, strerror(errno), errno);
                        }
                        
#ifdef FAT_TEST_IMG
                        if(ftruncate(fatimgfd, FAT_TEST_IMG) < 0){
#else
                        if(ftruncate(fatimgfd, (st.f_bfree*st.f_bsize-RESERVE_SIZE)) < 0){
#endif
                            printf("Fail to enlarge %s %s(%d)", fatimgfilepath, strerror(errno), errno);
                        }
                        fsync(fatimgfd);
                        close(fatimgfd);
                    }
                }
            }
            else{
                printf("First boot but %s has been created. Maybe power lost last time?\n", fatimgfilepath);
            }
        }
        else{
            if(access(fatimgfilepath, F_OK) != 0){
                printf("oops! FAT image has to be alreay created.Something wrong!Create again!\n");
                property_set("persist.fat_first_boot", "1");
                goto CREAT_FAT_IMG;
            }
            else
                printf("%s has been created\n", fatimgfilepath);
        }
        return 0;
    }
#endif

#if defined (ENG_BUILD_ENG)
    if (!strcmp(argv[1], "silkroad"))
   {
	    if ((sock = socket_local_client("silk",
	                                     ANDROID_SOCKET_NAMESPACE_RESERVED,
	                                     SOCK_STREAM)) < 0) {
	        fprintf(stderr, "Error connecting (%s)\n", strerror(errno));
	        exit(4);
	    }
    }	
    else
#endif		
   {
    if ((sock = socket_local_client("vold",
                                     ANDROID_SOCKET_NAMESPACE_RESERVED,
                                     SOCK_STREAM)) < 0) {
        fprintf(stderr, "Error connecting (%s)\n", strerror(errno));
        exit(4);
    }
    }

    if (!strcmp(argv[1], "monitor"))
        exit(do_monitor(sock, 0));
    exit(do_cmd(sock, argc, argv));
}

static int do_cmd(int sock, int argc, char **argv) {
    char final_cmd[255] = "0 "; /* 0 is a (now required) sequence number */
    int i;
    int ret;

    for (i = 1; i < argc; i++) {
        char *cmp;

        if (!index(argv[i], ' '))
            asprintf(&cmp, "%s%s", argv[i], (i == (argc -1)) ? "" : " ");
        else
            asprintf(&cmp, "\"%s\"%s", argv[i], (i == (argc -1)) ? "" : " ");

        ret = strlcat(final_cmd, cmp, sizeof(final_cmd));
        if (ret >= sizeof(final_cmd))
            abort();
        free(cmp);
    }

    if (write(sock, final_cmd, strlen(final_cmd) + 1) < 0) {
        perror("write");
        return errno;
    }

    return do_monitor(sock, 1);
}

static int do_monitor(int sock, int stop_after_cmd) {
    char *buffer = malloc(4096);

    if (!stop_after_cmd)
        printf("[Connected to Vold]\n");

    while(1) {
        fd_set read_fds;
        struct timeval to;
        int rc = 0;

        to.tv_sec = 10;
        to.tv_usec = 0;

        FD_ZERO(&read_fds);
        FD_SET(sock, &read_fds);

        if ((rc = select(sock +1, &read_fds, NULL, NULL, &to)) < 0) {
            fprintf(stderr, "Error in select (%s)\n", strerror(errno));
            free(buffer);
            return errno;
        } else if (!rc) {
            continue;
            fprintf(stderr, "[TIMEOUT]\n");
            return ETIMEDOUT;
        } else if (FD_ISSET(sock, &read_fds)) {
            memset(buffer, 0, 4096);
            if ((rc = read(sock, buffer, 4096)) <= 0) {
                if (rc == 0)
                    fprintf(stderr, "Lost connection to Vold - did it crash?\n");
                else
                    fprintf(stderr, "Error reading data (%s)\n", strerror(errno));
                free(buffer);
                if (rc == 0)
                    return ECONNRESET;
                return errno;
            }
            
            int offset = 0;
            int i = 0;

            for (i = 0; i < rc; i++) {
                if (buffer[i] == '\0') {
                    int code;
                    char tmp[4];

                    strncpy(tmp, buffer + offset, 3);
                    tmp[3] = '\0';
                    code = atoi(tmp);

                    printf("%s\n", buffer + offset);
                    if (stop_after_cmd) {
                        if (code >= 200 && code < 600)
                            return 0;
                    }
                    offset = i + 1;
                }
            }
        }
    }
    free(buffer);
    return 0;
}

static void usage(char *progname) {
    fprintf(stderr, "Usage: %s <monitor>|<cmd> [arg1] [arg2...]\n", progname);
    exit(1);
}

