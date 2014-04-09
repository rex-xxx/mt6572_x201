/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
/*
 * Copyright (C) 2010 The Android Open Source Project
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

#define LOG_TAG "SampleIterator"
//#define LOG_NDEBUG 0
#include <utils/Log.h>

#include "include/SampleIterator.h"

#include <arpa/inet.h>

#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/Utils.h>

#include "include/SampleTable.h"

namespace android {

SampleIterator::SampleIterator(SampleTable *table)
    : mTable(table),
      mInitialized(false),
      mTimeToSampleIndex(0),
      mTTSSampleIndex(0),
      mTTSSampleTime(0),
      mTTSCount(0),
      mTTSDuration(0) {
    reset();
}

void SampleIterator::reset() {
    mSampleToChunkIndex = 0;
    mFirstChunk = 0;
    mFirstChunkSampleIndex = 0;
    mStopChunk = 0;
    mStopChunkSampleIndex = 0;
    mSamplesPerChunk = 0;
    mChunkDesc = 0;
#ifndef ANDROID_DEFAULT_CODE
	mCurrentSampleIndex = 0xffffffff;
#endif
}

status_t SampleIterator::seekTo(uint32_t sampleIndex) {
    ALOGV("seekTo(%d)", sampleIndex);
#ifndef ANDROID_DEFAULT_CODE
	bool isNewChunk = false;
#endif

    if (sampleIndex >= mTable->mNumSampleSizes) {
        return ERROR_END_OF_STREAM;
    }

    if (mTable->mSampleToChunkOffset < 0
            || mTable->mChunkOffsetOffset < 0
            || mTable->mSampleSizeOffset < 0
            || mTable->mTimeToSampleCount == 0) {

        return ERROR_MALFORMED;
    }

    if (mInitialized && mCurrentSampleIndex == sampleIndex) {
        return OK;
    }

    if (!mInitialized || sampleIndex < mFirstChunkSampleIndex) {
#ifndef ANDROID_DEFAULT_CODE
		isNewChunk = true;
#endif
        reset();
    }

    if (sampleIndex >= mStopChunkSampleIndex) {
#ifndef ANDROID_DEFAULT_CODE
		isNewChunk = true;
#endif
        status_t err;
        if ((err = findChunkRange(sampleIndex)) != OK) {
            ALOGE("findChunkRange failed");
            return err;
        }
    }

    CHECK(sampleIndex < mStopChunkSampleIndex);

    uint32_t chunk =
        (sampleIndex - mFirstChunkSampleIndex) / mSamplesPerChunk
        + mFirstChunk;

    if (!mInitialized || chunk != mCurrentChunkIndex) {
#ifndef ANDROID_DEFAULT_CODE
		isNewChunk = true;
#endif
        mCurrentChunkIndex = chunk;

        status_t err;
        if ((err = getChunkOffset(chunk, &mCurrentChunkOffset)) != OK) {
            ALOGE("getChunkOffset return error");
            return err;
        }

        mCurrentChunkSampleSizes.clear();

        uint32_t firstChunkSampleIndex =
            mFirstChunkSampleIndex
                + mSamplesPerChunk * (mCurrentChunkIndex - mFirstChunk);

        for (uint32_t i = 0; i < mSamplesPerChunk; ++i) {
            size_t sampleSize;
            if ((err = getSampleSizeDirect(
                            firstChunkSampleIndex + i, &sampleSize)) != OK) {
                ALOGE("getSampleSizeDirect return error");
#ifndef ANDROID_DEFAULT_CODE
				if (err == ERROR_OUT_OF_RANGE)
				{
				ALOGW("Sample Index(from stsc) > Sample Count(from stsz), Set mSamplesPerChunk to %d according to stsz", mSamplesPerChunk);
					mSamplesPerChunk = i;
					break;
				}
				else
					return err;
#else
                return err;
#endif
            }

            mCurrentChunkSampleSizes.push(sampleSize);
        }
    }

    uint32_t chunkRelativeSampleIndex =
        (sampleIndex - mFirstChunkSampleIndex) % mSamplesPerChunk;
#ifndef ANDROID_DEFAULT_CODE
	if (!isNewChunk && (sampleIndex == mCurrentSampleIndex + 1) && (chunkRelativeSampleIndex > 0)) {
		mCurrentSampleOffset += mCurrentChunkSampleSizes[chunkRelativeSampleIndex-1];
	}
	else {
#endif

    mCurrentSampleOffset = mCurrentChunkOffset;
    for (uint32_t i = 0; i < chunkRelativeSampleIndex; ++i) {
        mCurrentSampleOffset += mCurrentChunkSampleSizes[i];
    }
#ifndef ANDROID_DEFAULT_CODE
	}
#endif
    mCurrentSampleSize = mCurrentChunkSampleSizes[chunkRelativeSampleIndex];
    if (sampleIndex < mTTSSampleIndex) {
        mTimeToSampleIndex = 0;
        mTTSSampleIndex = 0;
        mTTSSampleTime = 0;
        mTTSCount = 0;
        mTTSDuration = 0;
    }

    status_t err;
    if ((err = findSampleTime(sampleIndex, &mCurrentSampleTime)) != OK) {
        ALOGE("findSampleTime return error");
        return err;
    }

    mCurrentSampleIndex = sampleIndex;

    mInitialized = true;

    return OK;
}

status_t SampleIterator::findChunkRange(uint32_t sampleIndex) {
    CHECK(sampleIndex >= mFirstChunkSampleIndex);

    while (sampleIndex >= mStopChunkSampleIndex) {
        if (mSampleToChunkIndex == mTable->mNumSampleToChunkOffsets) {
            return ERROR_OUT_OF_RANGE;
        }

        mFirstChunkSampleIndex = mStopChunkSampleIndex;

        const SampleTable::SampleToChunkEntry *entry =
            &mTable->mSampleToChunkEntries[mSampleToChunkIndex];

        mFirstChunk = entry->startChunk;
        mSamplesPerChunk = entry->samplesPerChunk;
        mChunkDesc = entry->chunkDesc;

        if (mSampleToChunkIndex + 1 < mTable->mNumSampleToChunkOffsets) {
            mStopChunk = entry[1].startChunk;

            mStopChunkSampleIndex =
                mFirstChunkSampleIndex
                    + (mStopChunk - mFirstChunk) * mSamplesPerChunk;
        } else {
            mStopChunk = 0xffffffff;
            mStopChunkSampleIndex = 0xffffffff;
        }

        ++mSampleToChunkIndex;
    }

    return OK;
}

status_t SampleIterator::getChunkOffset(uint32_t chunk, off64_t *offset) {
    *offset = 0;

    if (chunk >= mTable->mNumChunkOffsets) {
        return ERROR_OUT_OF_RANGE;
    }

    if (mTable->mChunkOffsetType == SampleTable::kChunkOffsetType32) {
        uint32_t offset32;

        if (mTable->mDataSource->readAt(
                    mTable->mChunkOffsetOffset + 8 + 4 * chunk,
                    &offset32,
                    sizeof(offset32)) < (ssize_t)sizeof(offset32)) {
            return ERROR_IO;
        }

        *offset = ntohl(offset32);
    } else {
        CHECK_EQ(mTable->mChunkOffsetType, SampleTable::kChunkOffsetType64);

        uint64_t offset64;
        if (mTable->mDataSource->readAt(
                    mTable->mChunkOffsetOffset + 8 + 8 * chunk,
                    &offset64,
                    sizeof(offset64)) < (ssize_t)sizeof(offset64)) {
            return ERROR_IO;
        }

        *offset = ntoh64(offset64);
    }

    return OK;
}

status_t SampleIterator::getSampleSizeDirect(
        uint32_t sampleIndex, size_t *size) {
    *size = 0;

    if (sampleIndex >= mTable->mNumSampleSizes) {
        return ERROR_OUT_OF_RANGE;
    }

    if (mTable->mDefaultSampleSize > 0) {
        *size = mTable->mDefaultSampleSize;
        return OK;
    }

    switch (mTable->mSampleSizeFieldSize) {
        case 32:
        {
            if (mTable->mDataSource->readAt(
                        mTable->mSampleSizeOffset + 12 + 4 * sampleIndex,
                        size, sizeof(*size)) < (ssize_t)sizeof(*size)) {
                return ERROR_IO;
            }

            *size = ntohl(*size);
            break;
        }

        case 16:
        {
            uint16_t x;
            if (mTable->mDataSource->readAt(
                        mTable->mSampleSizeOffset + 12 + 2 * sampleIndex,
                        &x, sizeof(x)) < (ssize_t)sizeof(x)) {
                return ERROR_IO;
            }

            *size = ntohs(x);
            break;
        }

        case 8:
        {
            uint8_t x;
            if (mTable->mDataSource->readAt(
                        mTable->mSampleSizeOffset + 12 + sampleIndex,
                        &x, sizeof(x)) < (ssize_t)sizeof(x)) {
                return ERROR_IO;
            }

            *size = x;
            break;
        }

        default:
        {
            CHECK_EQ(mTable->mSampleSizeFieldSize, 4);

            uint8_t x;
            if (mTable->mDataSource->readAt(
                        mTable->mSampleSizeOffset + 12 + sampleIndex / 2,
                        &x, sizeof(x)) < (ssize_t)sizeof(x)) {
                return ERROR_IO;
            }

            *size = (sampleIndex & 1) ? x & 0x0f : x >> 4;
            break;
        }
    }

    return OK;
}

status_t SampleIterator::findSampleTime(
        uint32_t sampleIndex, uint32_t *time) {
    if (sampleIndex >= mTable->mNumSampleSizes) {
        return ERROR_OUT_OF_RANGE;
    }

    while (sampleIndex >= mTTSSampleIndex + mTTSCount) {
        if (mTimeToSampleIndex == mTable->mTimeToSampleCount) {
            return ERROR_OUT_OF_RANGE;
        }

        mTTSSampleIndex += mTTSCount;
        mTTSSampleTime += mTTSCount * mTTSDuration;

        mTTSCount = mTable->mTimeToSample[2 * mTimeToSampleIndex];
        mTTSDuration = mTable->mTimeToSample[2 * mTimeToSampleIndex + 1];

        ++mTimeToSampleIndex;
    }

    *time = mTTSSampleTime + mTTSDuration * (sampleIndex - mTTSSampleIndex);

    *time += mTable->getCompositionTimeOffset(sampleIndex);

    return OK;
}

}  // namespace android

