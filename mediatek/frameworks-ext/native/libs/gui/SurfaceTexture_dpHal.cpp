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

#include <gui/SurfaceTexture.h>

#include <utils/Trace.h>

#include <cutils/xlog.h>
#include <cutils/properties.h>

#include <DpBlitStream.h>

#define LOCK_FOR_DP (GRALLOC_USAGE_SW_READ_RARELY | GRALLOC_USAGE_SW_WRITE_NEVER | GRALLOC_USAGE_HW_TEXTURE)


namespace android {

status_t SurfaceTexture::checkPixelFormatSupported(
    sp<GraphicBuffer> graphicBuffer) const {

    if (graphicBuffer != NULL) {
        PixelFormat format = graphicBuffer->format;
        if ((HAL_PIXEL_FORMAT_I420 == format) ||
            (HAL_PIXEL_FORMAT_NV12_BLK == format) ||
            (HAL_PIXEL_FORMAT_NV12_BLK_FCM == format)) {
            return OK;
        }
    }
    return INVALID_OPERATION;
}

status_t SurfaceTexture::freeAuxSlotLocked(AuxSlot &bs) {
    if (EGL_NO_IMAGE_KHR != bs.eglSlot.mEglImage || bs.slot.mGraphicBuffer != NULL) {

        // destroying fence sync
        if (EGL_NO_SYNC_KHR != bs.eglSlot.mEglFence) {
            eglDestroySyncKHR(mEglDisplay, bs.eglSlot.mEglFence);
            bs.eglSlot.mEglFence = EGL_NO_SYNC_KHR;
        }

        XLOGI("[%s] this:%p", __func__, this);
        XLOGD("    GraphicBuffer: gb=%p handle=%p", bs.slot.mGraphicBuffer.get(), bs.slot.mGraphicBuffer->handle);
        XLOGD("    EGLImage: dpy=%p, img=%p", mEglDisplay, bs.eglSlot.mEglImage);

        bs.slot.mGraphicBuffer = NULL;
        eglDestroyImageKHR(mEglDisplay, bs.eglSlot.mEglImage);
        bs.eglSlot.mEglImage = EGL_NO_IMAGE_KHR;
    }

    return NO_ERROR;
}

// conversion function should format by format, chip by chip
// currently the input is I420, YV12, and MTKYUV; the output is ABGR
status_t SurfaceTexture::convertToAuxSlotLocked(bool isForce) {
    // check invalid buffer
    if (BufferQueue::INVALID_BUFFER_SLOT == mCurrentTexture) {
        mAuxSlotConvert = false;
        return INVALID_OPERATION;
    }

    ATRACE_CALL();

    // 1) normal BufferQueue needs conversion now
    // 2) SurfaceTextureLayer neesd conversion aftern HWC
    bool isNeedConversionNow =
        (BufferQueue::TYPE_BufferQueue == mBufferQueue->getType()) ||
        ((true == isForce) && (BufferQueue::TYPE_SurfaceTextureLayer == mBufferQueue->getType()));

    if (true == isNeedConversionNow) {
        XLOGI("do convertToAuxSlot...");

        Slot &src = mSlots[mCurrentTexture];
        AuxSlot &dst = *mBackAuxSlot;

        // fence sync here for buffer not used by G3D
        EGLSyncKHR fence = mFrontAuxSlot->eglSlot.mEglFence;
        if (fence != EGL_NO_SYNC_KHR) {
            EGLint result = eglClientWaitSyncKHR(mEglDisplay, fence, 0, 1000000000);
            if (result == EGL_FALSE) {
                XLOGW("[%s] FAILED waiting for front fence: %#x, tearing risk", __func__, eglGetError());
            } else if (result == EGL_TIMEOUT_EXPIRED_KHR) {
                XLOGW("[%s] TIMEOUT waiting for front fence, tearing risk", __func__);
            }
            eglDestroySyncKHR(mEglDisplay, fence);
            mFrontAuxSlot->eglSlot.mEglFence = EGL_NO_SYNC_KHR;
        }

        // source graphic buffer
        sp<GraphicBuffer> sg = src.mGraphicBuffer;

        // destination graphic buffer
        sp<GraphicBuffer> dg = dst.slot.mGraphicBuffer;

        // force to convert to ABGR8888
        uint32_t hal_out_fmt = HAL_PIXEL_FORMAT_RGBA_8888;
        DpColorFormat dp_out_fmt = eABGR8888;
        int dst_bitperpixel = 32;

        // free if current aux slot exist and not fit
        if ((EGL_NO_IMAGE_KHR != dst.eglSlot.mEglImage && dg != NULL) &&
            ((sg->width != dg->width) || (sg->height != dg->height) || (hal_out_fmt != (uint32_t)dg->format))) {

            XLOGI("[%s] free old aux slot ", __func__);
            XLOGI("    src[w:%d, h:%d, f:0x%x] dst[w:%d, h:%d, f:0x%x]",
                sg->width, sg->height, sg->format,
                dg->width, dg->height, dg->format);
            XLOGI("    required format:0x%x", hal_out_fmt);

            freeAuxSlotLocked(dst);
        }

        // create aux buffer if current is NULL
        if ((EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) && (dst.slot.mGraphicBuffer == NULL)) {
            XLOGI("[%s] create dst buffer and image", __func__);

            XLOGI("    before create new aux buffer: %p", dg.get());
            dg = dst.slot.mGraphicBuffer = new GraphicBuffer(sg->width,
                                                        sg->height,
                                                        hal_out_fmt,
                                                        sg->usage);
            if ((dg == NULL) || (dg->handle == NULL)) {
                XLOGE("    create aux GraphicBuffer FAILED");
                freeAuxSlotLocked(dst);
                return BAD_VALUE;
            } else {
                XLOGI("    [NEW AUX] gb=%p, handle=%p, w=%d, h=%d, s=%d, fmt=%d",
                    dg.get(), dg->handle,
                    dg->width, dg->height, dg->stride,
                    dg->format);
            }

            dst.eglSlot.mEglImage = createImage(mEglDisplay, dg);
            if (EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) {
                XLOGE("[%s] create aux eglImage FAILED", __func__);
                freeAuxSlotLocked(dst);
                return BAD_VALUE;
            }

            XLOGI("[%s] create aux slot success", __func__);
            XLOGI("    src[w:%d, h:%d, f:0x%x], dst[w:%d, h:%d, f:0x%x]",
                sg->width, sg->height, sg->format,
                dg->width, dg->height, dg->format);
        }

        status_t lockret;
        uint8_t *dst_yp;
        bool is_use_ion = false;

        lockret = dg->lock(LOCK_FOR_DP, (void**)&dst_yp);
        if (NO_ERROR != lockret) {
            XLOGE("[%s] buffer lock fail: %s", __func__, strerror(lockret));
            return INVALID_OPERATION;
        }

        {
            DpBlitStream bltStream;

            unsigned int src_offset[2];
            unsigned int src_size[3];

            int src_stride = (sg->format != HAL_PIXEL_FORMAT_YV12) ? sg->width : sg->stride;
            int src_size_luma = src_stride * sg->height;
            int src_size_chroma = src_size_luma / 4;

            // default set as YV12
            DpColorFormat dp_in_fmt = eYUV_420_3P_YVU;
            int plane_num = 3;

            // set & register src buffer
            switch (sg->format) {
                case HAL_PIXEL_FORMAT_I420:
                    plane_num = 3;
                    src_offset[0] = src_size_luma;
                    src_offset[1] = src_size_luma + src_size_chroma;
                    src_size[0] = src_size_luma;
                    src_size[1] = src_size_chroma;
                    src_size[2] = src_size_chroma;
                    dp_in_fmt = eYUV_420_3P;
                    break;

                case HAL_PIXEL_FORMAT_YV12:
                    plane_num = 3;
                    src_offset[0] = src_size_luma;
                    src_offset[1] = src_size_luma + src_size_chroma;
                    src_size[0] = src_size_luma;
                    src_size[1] = src_size_chroma;
                    src_size[2] = src_size_chroma;
                    dp_in_fmt = eYUV_420_3P_YVU;
                    break;

                case HAL_PIXEL_FORMAT_NV12_BLK:
                    plane_num = 2;
                    src_offset[0] = src_size_luma;
                    src_size[0] = src_size_luma;
                    src_size[1] = src_size_chroma * 2;
                    dp_in_fmt = eNV12_BLK;
                    break;

                case HAL_PIXEL_FORMAT_NV12_BLK_FCM:
                    plane_num = 2;
                    src_offset[0] = src_size_luma;
                    src_size[0] = src_size_luma;
                    src_size[1] = src_size_chroma * 2;
                    dp_in_fmt = eNV12_BLK_FCM;
                    break;

                default:
                    XLOGD("unexpected format for dp in:%d", sg->format);
                    dg->unlock();
                    return INVALID_OPERATION;
            }

#ifdef MTK_PQ_SUPPORT
            bltStream.setTdshp(1);
#endif

            // set src buffer
            int idx = 0;
            int num = 0;
            sg->getIonFd(&idx, &num);
            if (num > 0 && sg->handle->data[idx] != -1) {
                bltStream.setSrcBuffer(sg->handle->data[idx], src_size, plane_num);
                is_use_ion = true;
            } else {
                uint8_t *src_yp;
                lockret = sg->lock(LOCK_FOR_DP, (void**)&src_yp);
                if (NO_ERROR != lockret) {
                    XLOGE("[%s] buffer lock fail: %s", __func__, strerror(lockret));
                    dg->unlock();
                    return INVALID_OPERATION;
                }

                unsigned int src_addr[3];
                src_addr[0] = (unsigned int)src_yp;
                src_addr[1] = src_addr[0] + src_offset[0];
                src_addr[2] = src_addr[0] + src_offset[1];
                bltStream.setSrcBuffer((void**)src_addr, src_size, plane_num);
            }

            DpRect src_roi;
            src_roi.x = 0;
            src_roi.y = 0;
            src_roi.w = sg->width;
            src_roi.h = sg->height;

            bltStream.setSrcConfig(src_stride, sg->height, dp_in_fmt,
                                   eInterlace_None, &src_roi);

            // set dst buffer
            bltStream.setDstBuffer((void *)dst_yp, dg->stride * dg->height * dst_bitperpixel / 8);

            DpRect dst_roi;
            dst_roi.x = 0;
            dst_roi.y = 0;
            dst_roi.w = dg->width;
            dst_roi.h = dg->height;

            bltStream.setDstConfig(dg->stride, dg->height, dp_out_fmt,
                                    eInterlace_None, &dst_roi);

            if (!bltStream.invalidate()) {
                XLOGE("DpBlitStream invalidate failed");
                dg->unlock();
                if (!is_use_ion) sg->unlock();
                return INVALID_OPERATION;
            }
        }

        dg->unlock();
        if (!is_use_ion) sg->unlock();

        mAuxSlotConvert = false;
        mAuxSlotDirty = true;

        // draw grey debug line to aux
        if (true == mLine) {
            BufferQueue::DrawDebugLineToGraphicBuffer(dg, mLineCnt, 0x80);
            mLineCnt += 1;
        }
    }

    return NO_ERROR;
}


}

