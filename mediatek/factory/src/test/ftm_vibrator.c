/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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


#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/mount.h>
#include <sys/statfs.h>

#include "cust.h"
#include "common.h"
#include "miniui.h"
#include "ftm.h"

//add by chipeng
#if defined(MTK_VIBSPK_SUPPORT)
#include "ftm_audio_Common.h"
#include "SineWave_156Hz.h"
#endif

#define TAG   "[Vibrator] "
static uint32_t vibrator_time = 0;
extern int status;

#ifdef FEATURE_FTM_VIBRATOR

#define VIBRATOR_ENABLE "/sys/class/timed_output/vibrator/enable"

bool vibrator_test_exit = false;
static pthread_t vibrator_thread;

enum {
    ITEM_PASS,
    ITEM_FAIL
};

static item_t items[] = {
    item(ITEM_PASS,   uistr_pass),
    item(ITEM_FAIL,   uistr_fail),
    item(-1, NULL),
};

#if defined(MTK_VIBSPK_SUPPORT)

static void *update_vibrator_thread(void *mPtr)
{
    Audio_VSCurrent_Enable(true);
    Audio_VSCurrent_GetFrequency();
    
    while (1) {
        char *ptr;
        if (vibrator_test_exit){
            break;
        }
        Audio_VSCurrent_WriteRoutine();
    }
    
    Audio_VSCurrent_Enable(false);
    ALOGD(TAG "VibSpkFactory Thread Exit \n");
    pthread_exit(NULL); // thread exit
    return NULL;
}

#else
static int
write_int(char const* path, int value)
{
    int fd;

    if (path == NULL)
        return -1;

    fd = open(path, O_RDWR);
    if (fd >= 0) {
        char buffer[20];
        int bytes = sprintf(buffer, "%d\n", value);
        int amt = write(fd, buffer, bytes);
        close(fd);
        return amt == -1 ? -errno : 0;
    }

    LOGE("write_int failed to open %s\n", path);
    return -errno;
}

static void *update_vibrator_thread(void *priv)
{
    LOGD("%s: Start\n", __FUNCTION__);

    if(vibrator_time == 0)
    {
    do {
        write_int(VIBRATOR_ENABLE, 1000); // 1 seconds
        if (vibrator_test_exit)
            break;
        sleep(1);
        } while (1);    
        write_int(VIBRATOR_ENABLE, 0);
    }
    else
    {
        LOGD("%s: write vibrator_enable=%d\n", __FUNCTION__, vibrator_time);
        write_int(VIBRATOR_ENABLE, vibrator_time);
        sleep(1);
        write_int(VIBRATOR_ENABLE, 0);
        LOGD("%s: write vibrator_enable=0\n", __FUNCTION__);
    }

    pthread_exit(NULL);

    LOGD("%s: Exit\n", __FUNCTION__);

    return NULL;
}
#endif

int vibrator_entry(struct ftm_param *param, void *priv)
{
    int chosen;
    bool exit = false;
    struct itemview *iv;
    text_t    title;
    char* vibr_time = NULL;
    struct ftm_module *mod = (struct ftm_module *)priv;

    LOGD("%s\n", __FUNCTION__);

    vibrator_test_exit = false;

    iv = ui_new_itemview();
    if (!iv) {
        LOGD("No memory");
        return -1;
    }
    init_text(&title, param->name, COLOR_YELLOW);

    iv->set_title(iv, &title);
    iv->set_items(iv, items, 0);

    vibr_time = ftm_get_prop("Vibrator_Last_Time");
    LOGD("%s: get vibrator last time=%s!\n", __FUNCTION__, vibr_time);
    if(vibr_time != NULL)
    {
        vibrator_time = (uint32_t)atoi(vibr_time);
        LOGD("%s: get vibrator last time=%d!\n", __FUNCTION__, vibrator_time);
    }
    else
    {
        LOGD("%s: get vibrator last time fail!\n", __FUNCTION__);
    }
   #if defined(MTK_VIBSPK_SUPPORT)
    Common_Audio_init();
   #endif
    pthread_create(&vibrator_thread, NULL, update_vibrator_thread, priv);
    
    do {
        if(status == 0){
        chosen = iv->run(iv, &exit);
        switch (chosen) {
        case ITEM_PASS:
        case ITEM_FAIL:
            if (chosen == ITEM_PASS) {
                mod->test_result = FTM_TEST_PASS;
            } else if (chosen == ITEM_FAIL) {
                mod->test_result = FTM_TEST_FAIL;
            }
            exit = true;
            break;
        }

        if (exit) {
            vibrator_test_exit = true;
            break;
        }
        }
        else{
            iv->start_menu(iv, 0);
            iv->redraw(iv);
            if(vibrator_test_exit)
                break;
        }
    } while (1);
    pthread_join(vibrator_thread, NULL);
   #if defined(MTK_VIBSPK_SUPPORT)
    Common_Audio_deinit();
   #endif
    return 0;
}

int vibrator_init(void)
{
    int ret = 0;
    struct ftm_module *mod;

    LOGD("%s\n", __FUNCTION__);

    mod = ftm_alloc(ITEM_VIBRATOR, sizeof(struct ftm_module));
    if (!mod)
        return -ENOMEM;

    ret = ftm_register(mod, vibrator_entry, (void*)mod);

    return ret;
}

#endif // FEATURE_FTM_VIBRATOR
