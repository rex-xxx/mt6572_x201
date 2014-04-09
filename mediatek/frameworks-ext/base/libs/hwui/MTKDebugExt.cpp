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
int g_HWUI_debug_layers = 0;
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
int g_HWUI_debug_textures = 0;
// Turn on to display debug info about the layer renderer
int g_HWUI_debug_layer_renderer = 0;
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
    return;
}

bool dumpDisplayList(int width, int height, int level)
{
    return true;
}

bool dumpDraw(int width, int height, int level)
{
    return true;
}

bool dumpTexture(int texture, int width, int height, SkBitmap *bitmap)
{
    return true;
}

bool dumpAlphaTexture(int width, int height, uint8_t *data, const char *prefix)
{
    return true;
}
