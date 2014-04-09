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

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>

#include <common.h>
#include <miniui.h>
#include <ftm.h>

#include "mtkfb.h"
#include "tv_out.h"

#ifdef FEATURE_FTM_TVOUT

#define FB_FILE_PATH "/dev/TV-out"

#define TAG	"[TVOUT] "
#define mod_to_tvout(p)	(tvout_module *)((char *)(p) + sizeof(struct ftm_module))

#define ARY_SIZE(x)     (sizeof((x)) / sizeof((x[0])))

enum {
	ITEM_PASS,
	ITEM_FAIL
};

static item_t tvout_item[] = {
	item(ITEM_PASS,             uistr_pass),
	item(ITEM_FAIL,             uistr_fail),
	item(-1, NULL),
};


typedef struct {
	struct ftm_module *module;
    char info[512];

    pthread_t polling_thread;
    bool      exit_thread;

    /* item view */
	struct itemview *itm_view;
    text_t ctext_title;
    text_t ctext_info;
} tvout_module;


static void init_main_menu(struct ftm_param *param, tvout_module *tvout)
{
    struct itemview *iv = tvout->itm_view;

    init_text(&tvout->ctext_title, param->name, COLOR_YELLOW);
    init_text(&tvout->ctext_info, &tvout->info[0], COLOR_YELLOW);

    sprintf(tvout->info, "\n");

    iv->set_title(iv, &tvout->ctext_title);
    iv->set_items(iv, tvout_item, ITEM_PASS);
    iv->set_text(iv, &tvout->ctext_info);
}


static void *polling_cable_plugin_thread(void *priv)
{
	tvout_module *tvout = (tvout_module *)priv;
    struct itemview *iv = tvout->itm_view;

    int is_cable_plugged_in = 0;
    
	int fb = 0;

	fb = open(FB_FILE_PATH, O_RDONLY);
	if (fb < 0) {
        LOGE("tv_color_bar_test failed to open %s\n", FB_FILE_PATH);
        goto End;
	}
        
    while(!tvout->exit_thread)
    {
        if (ioctl(fb, TVOUT_IS_TV_CABLE_PLUG_IN, &is_cable_plugged_in) < 0) {
            LOGE("ioctl MTKFB_IS_TV_CABLE_PLUG_IN failed\n");
            goto End;
        }

        if (is_cable_plugged_in) {
            tvout->ctext_info.color = COLOR_GREEN;
            sprintf(tvout->info, uistr_info_tvout_plugin"\n"
                                 uistr_info_tvout_checkifplugin"\n");
        } else {
            tvout->ctext_info.color = COLOR_RED;
            sprintf(tvout->info, uistr_info_tvout_notplugin"\n");
        }

        LOGD(TAG "%d, %s", is_cable_plugged_in, tvout->info);

        iv->redraw(iv);
        
        usleep(300 * 1000);    // delay 300ms
    }

End:
    if (fb) close(fb);
    pthread_exit(NULL);

    return NULL;
}


static int init_accdet(void)
{
#define ACCDET_IOC_MAGIC 'A'
#define ACCDET_INIT      _IO(ACCDET_IOC_MAGIC,0)
#define ACCDET_PATH      "/dev/accdet"

    int fd = open(ACCDET_PATH, O_RDONLY);

    if (fd < 0) {
        LOGD(TAG "open %s failed, fd = %d", ACCDET_PATH, fd);
        return -1;
    }

    if (ioctl(fd, ACCDET_INIT, 0) < 0) {
        LOGE("ioctl ACCDET_INIT failed\n");
        goto End;
    }

End:
    if (fd) close(fd);
    return 0;
}


static int tvout_entry(struct ftm_param *param, void *priv)
{
	tvout_module *tvout = (tvout_module *)priv;

    bool exit = false;

    init_accdet();

    init_main_menu(param, tvout);

    tvout->exit_thread = false;
    pthread_create(&tvout->polling_thread,
                   NULL, polling_cable_plugin_thread, priv);

    while(!exit)
    {
        switch(tvout->itm_view->run(tvout->itm_view, NULL))
        {
        case ITEM_PASS:
            tvout->module->test_result = FTM_TEST_PASS;
            exit = true;
            break;
        case ITEM_FAIL:
            tvout->module->test_result = FTM_TEST_FAIL;
            exit = true;
            break;
        case -1:
            exit = true;
            break;
        default:
            break;
        }
    }

    tvout->exit_thread = true;
    pthread_join(tvout->polling_thread, NULL);

	return 0;
}


int tvout_init(void)
{
	int r;
	struct ftm_module *mod;
	tvout_module *tvout;

	mod = ftm_alloc(ITEM_TVOUT, sizeof(tvout_module));
	if (!mod)
		return -ENOMEM;

	tvout = mod_to_tvout(mod);

	tvout->module = mod;
	tvout->itm_view = ui_new_itemview();
	if (!tvout->itm_view)
		return -ENOMEM;

	r = ftm_register(mod, tvout_entry, (void*)tvout);
	if (r) {
		LOGD(TAG "register TVOUT failed (%d)\n", r);
		return r;
	}

	return 0;
}

#endif  // FEATURE_FTM_TVOUT
