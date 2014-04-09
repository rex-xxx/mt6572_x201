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

#define LOG_TAG "OpenGLRenderer"

#include <cutils/xlog.h>
#include <cutils/properties.h>
#include <GLES2/gl2.h>
#include <SkImageEncoder.h>
#include "MTKDebug.h"

// Turn on to check for OpenGL errors on each frame
int g_HWUI_debug_opengl = 1;
// Turn on to display informations about the GPU
int g_HWUI_debug_extensions = 0;
// Turn on to enable initialization information
int g_HWUI_debug_init = 0;
// Turn on to enable memory usage summary on each frame
int g_HWUI_debug_memory_usage = 0;
// Turn on to enable debugging of cache flushes
int g_HWUI_debug_cache_flush = 1;
// Turn on to enable layers debugging when rendered as regions
int g_HWUI_debug_layers_as_regions = 0;
// Turn on to display debug info about vertex/fragment shaders
int g_HWUI_debug_programs = 0;
// Turn on to display info about layers
int g_HWUI_debug_layers = 1;
// Turn on to display debug info about 9patch objects
int g_HWUI_debug_patches = 0;
// Turn on to "explode" 9patch objects
int g_HWUI_debug_explode_patches = 0;
// Turn on to display vertex and tex coords data about 9patch objects
// This flag requires DEBUG_PATCHES to be turned on
int g_HWUI_debug_patches_vertices = 0;
// Turn on to display vertex and tex coords data used by empty quads
// in 9patch objects
// This flag requires DEBUG_PATCHES to be turned on
int g_HWUI_debug_patches_empty_vertices = 0;
// Turn on to display debug info about shapes
int g_HWUI_debug_shapes = 0;
// Turn on to display debug info about textures
int g_HWUI_debug_textures = 1;
// Turn on to display debug info about the layer renderer
int g_HWUI_debug_layer_renderer = 1;
// Turn on to enable additional debugging in the font renderers
int g_HWUI_debug_font_renderer = 0;
// Turn on to dump display list state
int g_HWUI_debug_display_list = 0;
// Turn on to insert an event marker for each display list op
int g_HWUI_debug_display_ops_as_events = 0;

//MTK debug dump functions
int g_HWUI_debug_dumpDisplayList = 0;
int g_HWUI_debug_dumpDraw = 0;
int g_HWUI_debug_dumpTexture = 0;
int g_HWUI_debug_dumpAlphaTexture = 0;

void setDebugLog(bool enable) {
    int* pDebugArray[e_HWUI_DEBUG_END] = {
        &g_HWUI_debug_opengl,
        &g_HWUI_debug_extensions,
        &g_HWUI_debug_init,
        &g_HWUI_debug_memory_usage,
        &g_HWUI_debug_cache_flush,
        &g_HWUI_debug_layers_as_regions,
        &g_HWUI_debug_programs,
        &g_HWUI_debug_layers,
        &g_HWUI_debug_patches,
        &g_HWUI_debug_explode_patches,
        &g_HWUI_debug_patches_vertices,
        &g_HWUI_debug_patches_empty_vertices,
        &g_HWUI_debug_shapes,
        &g_HWUI_debug_textures,
        &g_HWUI_debug_layer_renderer,
        &g_HWUI_debug_font_renderer,
        &g_HWUI_debug_display_list,
        &g_HWUI_debug_display_ops_as_events,
        &g_HWUI_debug_dumpDisplayList,
        &g_HWUI_debug_dumpDraw,
        &g_HWUI_debug_dumpTexture,
        &g_HWUI_debug_dumpAlphaTexture,
    };
    const char* properties[e_HWUI_DEBUG_END] = {
        "debug.hwui.log.opengl",
        "debug.hwui.log.ext",
        "debug.hwui.log.init",
        "debug.hwui.log.mem",
        "debug.hwui.log.cache_flush",
        "debug.hwui.log.layers_as_regions",
        "debug.hwui.log.programs",
        "debug.hwui.log.layers",
        "debug.hwui.log.patches",
        "debug.hwui.log.explode_patches",
        "debug.hwui.log.patches_vtx",
        "debug.hwui.log.patches_empty_vtx",
        "debug.hwui.log.shapes",
        "debug.hwui.log.tex",
        "debug.hwui.log.layer_renderer",
        "debug.hwui.log.font_renderer",
        "debug.hwui.log.displaylist",
        "debug.hwui.log.display_events",
        "debug.hwui.dump.displaylist",
        "debug.hwui.dump.draw",
        "debug.hwui.dump.tex",
        "debug.hwui.dump.alphatex",
    };
    char value[PROPERTY_VALUE_MAX];
    uint32_t flags;

    if (enable) {
        property_get("debug.hwui.log", value, "");
        if (value[0] != '\0') {
            flags = (unsigned)atoi(value);
            for (uint32_t i = 0; i < e_HWUI_DEBUG_END; i++) {
                if ((flags >> i) & 0x1) {
                    ALOGD("setDebugLog: %s=1", properties[i]);
                    *pDebugArray[i] = 1;
                } else {
                    *pDebugArray[i] = 0;
                }
            }
        } else {
            for (uint32_t i = 0; i < e_HWUI_DEBUG_END; i++) {
                property_get(properties[i], value, "");
                if (value[0] != '\0') {
                    ALOGD("setDebugLog: %s=%s", properties[i], value);
                    *pDebugArray[i] = (strcmp(value, "0") == 0) ? 0 : 1;
                }
            }
        }
    } else {
        for (uint32_t i = 0; i < e_HWUI_DEBUG_END; i++) {
            *pDebugArray[i] = 0;
        }
    }
}

#if defined(MTK_DEBUG_RENDERER)

static bool getProcessName(char* psProcessName, int size)
{
    FILE *f;
    char *slash;

    if (!psProcessName)
        return false;

    f = fopen("/proc/self/cmdline", "r");
    if (!f)
    {
        XLOGE("Can't get application name");
        return false;
    }

    if (fgets(psProcessName, size, f) == NULL)
    {
        XLOGE("ame : fgets failed");
        fclose(f);
        return false;
    }

    fclose(f);

    if ((slash = strrchr(psProcessName, '/')) != NULL)
    {
        memmove(psProcessName, slash+1, strlen(slash));
    }

    return true;
}

static bool dumpImage(int width, int height, const char *filename)
{
    size_t size = width * height * 4;
    GLbyte *buf = (GLbyte*)malloc(size);
    GLenum error;
    bool bRet = true;

    if (!buf)
    {
        XLOGE("%s: failed to allocate buffer (%d bytes)\n", __FUNCTION__, size);
        return false;
    }

    SkBitmap bitmap;
    bitmap.setConfig(SkBitmap::kARGB_8888_Config, width, height);
    bitmap.setPixels(buf, NULL);

    XLOGI("%s: %dx%d, %s\n", __FUNCTION__, width, height, filename);
    glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, bitmap.getPixels());

    if ((error = glGetError()) != GL_NO_ERROR)
    {
        XLOGE("%s: get GL error 0x%x \n", __FUNCTION__, error);
        bRet = false;
        goto Exit;
    }

    if (!SkImageEncoder::EncodeFile(filename, bitmap, SkImageEncoder::kPNG_Type, 100))
    {
        XLOGE("%s: Failed to encode image %s\n", __FUNCTION__, filename);
        bRet = false;
        goto Exit;
    }

Exit:
    free(buf);
    return bRet;
}


bool dumpDisplayList(int width, int height, int level)
{
    static int frame = 0;
    static int count = 0;
    char procName[256];
    char file[512];

    if (!g_HWUI_debug_dumpDisplayList)
        return false;

    if (!getProcessName(procName, sizeof(procName)))
        return false;

    if (level == 0)
    {
        count = 0;
        frame++;
    }
    sprintf(file, "/data/data/%s/dp_%04d_%04d.png", procName, frame, count++);
    return dumpImage(width, height, file);
}

bool dumpDraw(int width, int height, int level)
{
    static int frame = 0;
    static int count = 0;
    char procName[256];
    char file[512];

    if (!g_HWUI_debug_dumpDraw)
        return false;

    if (!getProcessName(procName, sizeof(procName)))
        return false;

    if (level == 0)
    {
        count = 0;
        frame++;
    }
    sprintf(file, "/data/data/%s/draw_%04d_%04d.png", procName, frame, count++);
    return dumpImage(width, height, file);
}

bool dumpTexture(int texture, int width, int height, SkBitmap *bitmap)
{
    char procName[256];
    char file[512];

    if (!g_HWUI_debug_dumpTexture)
        return false;

    if (!getProcessName(procName, sizeof(procName)))
        return false;

    sprintf(file, "/data/data/%s/tex_%d_%d_%d_%p.png", procName, texture, width, height, bitmap);
    if (!SkImageEncoder::EncodeFile(file, *bitmap, SkImageEncoder::kPNG_Type, 100))
    {
        XLOGE("%s: Fail to dump texture: %s", __FUNCTION__, file);
        return false;
    }

    XLOGI("%s: %dx%d, %s", __FUNCTION__, width, height, file);
    return true;
}

bool dumpAlphaTexture(int width, int height, uint8_t *data, const char *prefix)
{
    static int count = 0;
    char procName[256];
    char file[512];
    SkBitmap bitmap;
    SkBitmap bitmapCopy;

    if (!getProcessName(procName, sizeof(procName)))
        return false;

    sprintf(file, "/data/data/%s/%s_%04d.png", procName, prefix, count++);
    XLOGI("%s: %dx%d %s\n", __FUNCTION__, width, height, file);

    bitmap.setConfig(SkBitmap::kA8_Config, width, height);
    bitmap.setPixels(data, NULL);

    if (!bitmap.copyTo(&bitmapCopy, SkBitmap::kARGB_8888_Config))
    {
        XLOGD("%s: Failed to copy data", __FUNCTION__);
        return false;
    }

    if (!SkImageEncoder::EncodeFile(file, bitmapCopy, SkImageEncoder::kPNG_Type, 100))
    {
        XLOGE("%s: Failed to encode image %s\n", __FUNCTION__, file);
        return false;
    }

    return true;
}

#endif
