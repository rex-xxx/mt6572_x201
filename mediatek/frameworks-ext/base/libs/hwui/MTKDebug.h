/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#ifndef MTK_HWUI_DEBUG_H
#define MTK_HWUI_DEBUG_H

#include <SkBitmap.h>

enum {
    /*
        Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!Critical!!
        If want to modify the below log index, you SHALL also update the responding the array in the MTKDebug.cpp"
        Or, it would cuase a build failure!!!
            int* pDebugArray[e_HWUI_DEBUG_END] = {
                &g_HWUI_debug_opengl,
                &g_HWUI_debug_extensions,
                ...

    */
    e_HWUI_DEBUG_BEGIN = 0,
    e_HWUI_DEBUG_OPENGL = e_HWUI_DEBUG_BEGIN,
    e_HWUI_DEBUG_EXTENSIONS,
    e_HWUI_DEBUG_INIT,
    e_HWUI_DEBUG_MEMORY_USAGE,
    e_HWUI_DEBUG_CACHE_FLUSH,
    e_HWUI_DEBUG_LAYERS_AS_REGIONS,
    e_HWUI_DEBUG_PROGRAMS,
    e_HWUI_DEBUG_LAYERS,
    e_HWUI_DEBUG_PATCHES,
    e_HWUI_DEBUG_EXPLODE_PATCHES,
    e_HWUI_DEBUG_PATCHES_VERTICES,
    e_HWUI_DEBUG_PATCHES_EMPTY_VERTICES,
    e_HWUI_DEBUG_SHAPES,
    e_HWUI_DEBUG_TEXTURES,
    e_HWUI_DEBUG_LAYER_RENDERER,
    e_HWUI_DEBUG_FONT_RENDERER,
    e_HWUI_DEBUG_DISPLAY_LIST,
    e_HWUI_DEBUG_DISPLAY_OPS_AS_EVENTS,

    e_HWUI_DEBUG_DUMPDISPLAYLIST,
    e_HWUI_DEBUG_DUMPDRAW,
    e_HWUI_DEBUG_DUMPTEXTURE,
    e_HWUI_DEBUG_DUMPALPHATEXTURE,
    e_HWUI_DEBUG_END
};

void setDebugLog(bool enable);

#if defined(MTK_DEBUG_RENDERER)
    bool dumpDisplayList(int width, int height, int level);
    bool dumpDraw(int width, int height, int level);
    bool dumpTexture(int texture, int width, int height, SkBitmap *bitmap);
    bool dumpAlphaTexture(int width, int height, uint8_t *data, const char *prefix);
#endif

#endif // MTK_HWUI_DEBUG_H
