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

#include <cutils/xlog.h>
#include <cutils/properties.h>

#include <SkImageEncoder.h>
#include <SkBitmap.h>

#include <gui/ISurfaceComposer.h>
#include <gui/BufferQueue.h>

#include "SurfaceFlinger.h"
#include "Layer.h"

namespace android {

// dump current using buffer in Layer
void Layer::dumpActiveBuffer() const {
    XLOGV("[dumpActiveBuffer] + id=%d", getIdentity());

    if (mActiveBuffer != NULL) {
        char     value[PROPERTY_VALUE_MAX];
        bool     raw;
        uint32_t identity;

        property_get("debug.sf.layerdump.raw", value, "0");
        raw = (0 != atoi(value));
        identity = getIdentity();

        char             fname[128];
        void*            ptr;
        uint32_t         size;
        SkBitmap         b;
        SkBitmap::Config c;

        size = 0;
        c = SkBitmap::kNo_Config;
        switch (mActiveBuffer->format) {
            case PIXEL_FORMAT_RGBA_8888:
            case PIXEL_FORMAT_RGBX_8888:
                if (false == raw) {
                    c = SkBitmap::kARGB_8888_Config;
                    sprintf(fname, "/data/SF_dump/%d.png", identity);
                } else {
                    size = mActiveBuffer->stride * mActiveBuffer->height * 4;
                    sprintf(fname, "/data/SF_dump/%d.RGBA", identity);
                }
                break;
            case PIXEL_FORMAT_BGRA_8888:
            case 0x1ff:                     // tricky format for SGX_COLOR_FORMAT_BGRX_8888 in fact
                if (false == raw) {
                    c = SkBitmap::kARGB_8888_Config;
                    sprintf(fname, "/data/SF_dump/%d(RBswapped).png", identity);
                } else {
                    size = mActiveBuffer->stride * mActiveBuffer->height * 4;
                    sprintf(fname, "/data/SF_dump/%d.BGRA", identity);
                }
                break;
            case PIXEL_FORMAT_RGB_565:
                if (false == raw) {
                    c = SkBitmap::kRGB_565_Config;
                    sprintf(fname, "/data/SF_dump/%d.png", identity);
                } else {
                    size = mActiveBuffer->stride * mActiveBuffer->height * 2;
                    sprintf(fname, "/data/SF_dump/%d.RGB565", identity);
                }
                break;
            case HAL_PIXEL_FORMAT_I420:
                size = mActiveBuffer->stride * mActiveBuffer->height * 3 / 2;
                sprintf(fname, "/data/SF_dump/%d.i420", identity);
                break;
            case HAL_PIXEL_FORMAT_NV12_BLK:
                size = mActiveBuffer->stride * mActiveBuffer->height * 3 / 2;
                sprintf(fname, "/data/SF_dump/%d.nv12_blk", identity);
                break;
            case HAL_PIXEL_FORMAT_NV12_BLK_FCM:
                size = mActiveBuffer->stride * mActiveBuffer->height * 3 / 2;
                sprintf(fname, "/data/SF_dump/%d.nv12_blk_fcm", identity);
                break;
            case HAL_PIXEL_FORMAT_YV12:
                // android YV12 need align to 16 for UV plane
                size = (mActiveBuffer->stride * mActiveBuffer->height) + 
                       ((mActiveBuffer->stride / 2 + 0xf) & (~0xf)) * (mActiveBuffer->height / 2) * 2;
                sprintf(fname, "/data/SF_dump/%d.yv12", identity);
                break;
            default:
                XLOGE("[%s] cannot dump format:%d for identity:%d",
                      __func__, mActiveBuffer->format, identity);
                return;
        }

        mActiveBuffer->lock(GraphicBuffer::USAGE_SW_READ_OFTEN, &ptr);
        {
            XLOGI("[%s] %s", __func__, getName().string());
            XLOGI("    %s (config:%d, stride:%d, height:%d, ptr:%p)",
                fname, c, mActiveBuffer->stride, mActiveBuffer->height, ptr);

            if (SkBitmap::kNo_Config != c) {
                b.setConfig(c, mActiveBuffer->stride, mActiveBuffer->height);
                b.setPixels(ptr);
                SkImageEncoder::EncodeFile(fname, b, SkImageEncoder::kPNG_Type,
                                           SkImageEncoder::kDefaultQuality);
            } else {
                FILE *f = fopen(fname, "wb");
                fwrite(ptr, size, 1, f);
                fclose(f);
            }
        }
        mActiveBuffer->unlock();

        dumpContinuousBuffer();
    }
    XLOGV("[dumpActiveBuffer] - id=%d", getIdentity());
}

void Layer::setContBufsDumpById(int identity) {
    XLOGV("setContBufsDumpById, id=%d", identity);
    mContBufsDumpById = identity;
}

void Layer::activeBufferBackup() {
    if (mActiveBuffer == NULL) {
        XLOGW("[Layer::activeBufferBackup] mActiveBuffer=%p not initialized", mActiveBuffer.get());
        return;
    }
    
    if (mContBufsDumpById == (int)getIdentity() || mContBufsDumpById == -1) {
        XLOGV("[Layer::activeBufferBackup] +, req=%d, id=%d", mContBufsDumpById, getIdentity());
        // check bpp
        float bpp = 0.0f;
        uint32_t width  = mActiveBuffer->width;
        uint32_t height = mActiveBuffer->height;
        uint32_t format = mActiveBuffer->format;
        uint32_t usage  = mActiveBuffer->usage;
        uint32_t stride = mActiveBuffer->stride;
        status_t err;
        
        switch (mActiveBuffer->format) {
            case PIXEL_FORMAT_RGBA_8888:
            case PIXEL_FORMAT_BGRA_8888:
            case PIXEL_FORMAT_RGBX_8888:
            case 0x1ff:
                // tricky format for SGX_COLOR_FORMAT_BGRX_8888 in fact
                bpp = 4.0;
                break;
            case PIXEL_FORMAT_RGB_565:
                bpp = 2.0;
                break;
            case HAL_PIXEL_FORMAT_I420:
                bpp = 1.5;
                break;
            case HAL_PIXEL_FORMAT_YV12:
                bpp = 1.5;
                break;
            default:
                XLOGE("[%s] cannot dump format:%d for identity:%d", __func__, mActiveBuffer->format, getIdentity());
                break;
        }

#define MAX_DEFAULT_BUFFERS 10
        // initialize backup buffer max size
        char value[PROPERTY_VALUE_MAX];

        property_get("debug.sf.contbufsmax", value, "0");
        uint32_t max = atoi(value);
        if (max <= 0)
            max = MAX_DEFAULT_BUFFERS;

        if (mBackupBufsMax != max) {
            mBackupBufsMax = max;
            XLOGI("==>  ring buffer max size, max = %d", max);

            mBackBufs.clear();
            mBackupBufsIndex = 0;
        }

        // create new GraphicBuffer
        if (mBackBufs.size() < mBackupBufsMax) {
            sp<GraphicBuffer> buf;
            buf = new GraphicBuffer(width, height, format, usage);
            mBackBufs.push(buf);

            XLOGI("[id=%d] new buffer for cont. dump, size = %d", getIdentity(), mBackBufs.size());
        }
        
        // detect geometry changed
        if (mBackBufs[mBackupBufsIndex]->width != mActiveBuffer->width || 
            mBackBufs[mBackupBufsIndex]->height != mActiveBuffer->height ||
            mBackBufs[mBackupBufsIndex]->format != mActiveBuffer->format) {
            XLOGI("[id=%d] geometry changed, backup=(%d, %d, %d) ==> active=(%d, %d, %d)",
                getIdentity(),
                mBackBufs[mBackupBufsIndex]->width,
                mBackBufs[mBackupBufsIndex]->height,
                mBackBufs[mBackupBufsIndex]->format,
                mActiveBuffer->width,
                mActiveBuffer->height,
                mActiveBuffer->format);

            sp<GraphicBuffer> buf;
            buf = new GraphicBuffer(width, height, format, usage);
            mBackBufs.replaceAt(buf, mBackupBufsIndex);
        }

        if (mActiveBuffer.get() == NULL || mBackBufs[mBackupBufsIndex] == NULL) {
            XLOGW("[Layer::activeBufferBackup] backup fail, mActiveBuffer=%p, mBackBufs[%d]=%p",
                mActiveBuffer.get(), mBackupBufsIndex, mBackBufs[mBackupBufsIndex].get());
            return;
        }
        
        // backup
        nsecs_t now = systemTime();        

        void* src;
        void* dst;
        err = mActiveBuffer->lock(GraphicBuffer::USAGE_SW_READ_OFTEN, &src);
        if (err != NO_ERROR) {
            XLOGW("[Layer::activeBufferBackup] lock active buffer failed");
            return;
        }

        XLOGV("[Layer::activeBufferBackup] lock +, req=%d, id=%d", mContBufsDumpById, getIdentity());
        err = mBackBufs[mBackupBufsIndex]->lock(GraphicBuffer::USAGE_SW_READ_OFTEN | GraphicBuffer::USAGE_SW_WRITE_OFTEN, &dst);
        if (err != NO_ERROR) {
            XLOGW("[Layer::activeBufferBackup] lock backup buffer failed");
            return;
        }

        backupProcess(dst, src, stride*height*bpp);

        mBackBufs[mBackupBufsIndex]->unlock();
        mActiveBuffer->unlock();

        if (mContBufsDumpById == -1) {
            XLOGI("[Layer::activeBufferBackup] req=%d, id=%d, buf=%d, time=%lld", 
                mContBufsDumpById, getIdentity(), mBackupBufsIndex, ns2ms(systemTime() - now));
        } else {
            XLOGI("[Layer::activeBufferBackup] id=%d, buf=%d, time=%lld", 
                getIdentity(), mBackupBufsIndex, ns2ms(systemTime() - now));
        }
        mBackupBufsIndex ++;
        if (mBackupBufsIndex >= mBackupBufsMax)
            mBackupBufsIndex = 0;
    }
}

void Layer::backupProcess(void* dst, void* src, size_t size) const {
    XLOGV("[Layer::backupProcess] +, req=%d, id=%d", mContBufsDumpById, getIdentity());

    // backup 
    memcpy(dst, src, size);

    XLOGV("[Layer::backupProcess] -, req=%d, id=%d", mContBufsDumpById, getIdentity());
}

void Layer::dumpContinuousBuffer() const {
    char tmp[PROPERTY_VALUE_MAX];
    int  identity = getIdentity();

    if (mContBufsDumpById <= 0 && mContBufsDumpById != -1)
        return;
        
    if (mBackupBufsMax <= 0) {
        XLOGW("[Layer::dumpContinuousBuffer] mBackupBufsMax not updated");
        return;
    }

    XLOGD("[Layer::dumpContinuousBuffer] +, req=%d, id=%d, size=%d",
        mContBufsDumpById, getIdentity(), mBackBufs.size());

    if (mContBufsDumpById == (int)getIdentity() || mContBufsDumpById == -1) {
        int start = (mBackupBufsIndex + mBackupBufsMax - 1) % mBackupBufsMax;
        for (uint32_t i = 0; i < mBackupBufsMax; i++) {
            if (i >= mBackBufs.size()) {
                XLOGW("[Layer::dumpContinuousBuffer] overflow i=%d, max=%d", i, mBackBufs.size());
                return;
            }

            int index = (start + mBackupBufsMax - i) % mBackupBufsMax;
            XLOGD("[Layer::dumpContinuousBuffer] i=%d, index=%d", mBackupBufsMax - i, index);
            sp<GraphicBuffer> buf = mBackBufs[index];
            dumpGraphicBuffer(buf, mBackupBufsMax - i);
        }
    }

    XLOGD("[Layer::dumpContinuousBuffer] -");
}

void Layer::dumpGraphicBuffer(sp<GraphicBuffer> buf, int index) const {
    char             fname[128];
    void*            ptr;
    SkBitmap         b;
    SkBitmap::Config c;
    int              identity = getIdentity();
    float            bpp;
    
    
    c = SkBitmap::kNo_Config;
    switch (buf->format) {
        case PIXEL_FORMAT_RGBA_8888:
        case PIXEL_FORMAT_BGRA_8888:
        case PIXEL_FORMAT_RGBX_8888:
        case 0x1ff:                     // tricky format for SGX_COLOR_FORMAT_BGRX_8888 in fact
            c = SkBitmap::kARGB_8888_Config;
            sprintf(fname, "/data/SF_dump/%d_%03d.png", identity, index);
            break;
        case PIXEL_FORMAT_RGB_565:
            c = SkBitmap::kRGB_565_Config;
            sprintf(fname, "/data/SF_dump/%d_%03d.png", identity, index);
            break;
        case HAL_PIXEL_FORMAT_I420:
            bpp = 1.5;
            sprintf(fname, "/data/SF_dump/%d_%03d.i420", identity, index);
            break;
        case HAL_PIXEL_FORMAT_YV12:
            bpp = 1.5;
            sprintf(fname, "/data/SF_dump/%d_%03d.yv12", identity, index);
            break;
        default:
            XLOGE("[%s] cannot dump format:%d for identity:%d", __func__, buf->format, identity);
            return;
    }

    buf->lock(GraphicBuffer::USAGE_SW_READ_OFTEN, &ptr);
    {
        XLOGI("[Layer::dumpGraphicBuffer] %s", getName().string());
        XLOGI("    %s (config:%d, stride:%d, height:%d, ptr:%p)",
            fname, c, buf->stride, buf->height, ptr);

        if (SkBitmap::kNo_Config != c) {
            b.setConfig(c, buf->stride, buf->height);
            b.setPixels(ptr);
            SkImageEncoder::EncodeFile(
                fname, b, SkImageEncoder::kPNG_Type, SkImageEncoder::kDefaultQuality);
        } else {
            uint32_t size = buf->stride * buf->height * bpp;
            FILE *f = fopen(fname, "wb");
            fwrite(ptr, size, 1, f);
            fclose(f);
        }
    }
    buf->unlock();
}

};
