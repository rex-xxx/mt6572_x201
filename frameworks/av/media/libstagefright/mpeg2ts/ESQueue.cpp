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

//#define LOG_NDEBUG 0
#define LOG_TAG "ESQueue"
#include <media/stagefright/foundation/ADebug.h>

#include "ESQueue.h"

#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/foundation/ABitReader.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>

#include "include/avc_utils.h"

#include <netinet/in.h>

#if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_STAGEFRIGHT_USE_XLOG)
#include <cutils/xlog.h>
#endif

namespace android {

ElementaryStreamQueue::ElementaryStreamQueue(Mode mode, uint32_t flags)
    : mMode(mode),
#ifndef ANDROID_DEFAULT_CODE     
      mMP3Header(0),
      mSeeking(false),
      mAudioFrameDuration(20000),
      mVorbisStatus(0),
      mH264UsePPs(false),
      #ifdef MTK_OGM_PLAYBACK_SUPPORT
      mfgSearchStartCodeOptimize(0),
      #endif
      mfgFirstFrmAfterSeek(false),
#endif
      mFlags(flags) {
}

sp<MetaData> ElementaryStreamQueue::getFormat() {
    return mFormat;
}

void ElementaryStreamQueue::clear(bool clearFormat) {
    if (mBuffer != NULL) {
        mBuffer->setRange(0, 0);

        #if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_OGM_PLAYBACK_SUPPORT)  
        if(mfgSearchStartCodeOptimize)
        {
            mBuffer->meta()->setInt32("LPos", 0);
        }
        #endif
    }

    mRangeInfos.clear();
#ifndef ANDROID_DEFAULT_CODE
    if (mMode == H264) {
    accessUnits.clear();
	}
	
    mfgFirstFrmAfterSeek = false;
#endif
    if (clearFormat) {
        mFormat.clear();
    }
}

static bool IsSeeminglyValidADTSHeader(const uint8_t *ptr, size_t size) {
    if (size < 3) {
        // Not enough data to verify header.
        return false;
    }

    if (ptr[0] != 0xff || (ptr[1] >> 4) != 0x0f) {
        return false;
    }

    unsigned layer = (ptr[1] >> 1) & 3;

    if (layer != 0) {
        return false;
    }

    unsigned ID = (ptr[1] >> 3) & 1;
    unsigned profile_ObjectType = ptr[2] >> 6;

    if (ID == 1 && profile_ObjectType == 3) {
        // MPEG-2 profile 3 is reserved.
        return false;
    }
#ifndef ANDROID_DEFAULT_CODE
	//in case of find the wrong error
	uint8_t number_of_raw_data_blocks_in_frame;
	number_of_raw_data_blocks_in_frame = (ptr[6]) & 0x03;
	if(number_of_raw_data_blocks_in_frame !=0)
	{
	ALOGE("Error: fake header here number_of_raw_data_blocks_in_frame=%d",number_of_raw_data_blocks_in_frame);
		 return false;
	}
#endif	

    return true;
}

static bool IsSeeminglyValidMPEGAudioHeader(const uint8_t *ptr, size_t size) {
    if (size < 3) {
        // Not enough data to verify header.
        return false;
    }

    if (ptr[0] != 0xff || (ptr[1] >> 5) != 0x07) {
        return false;
    }

    unsigned ID = (ptr[1] >> 3) & 3;

    if (ID == 1) {
        return false;  // reserved
    }

    unsigned layer = (ptr[1] >> 1) & 3;

    if (layer == 0) {
        return false;  // reserved
    }

    unsigned bitrateIndex = (ptr[2] >> 4);

    if (bitrateIndex == 0x0f) {
        return false;  // reserved
    }

    unsigned samplingRateIndex = (ptr[2] >> 2) & 3;

    if (samplingRateIndex == 3) {
        return false;  // reserved
    }

    return true;
}

status_t ElementaryStreamQueue::appendData(
        const void *data, size_t size, int64_t timeUs, bool fgInvalidPTS) {
    if (mBuffer == NULL || mBuffer->size() == 0) {
        switch (mMode) {
            case H264:
#ifdef ANDROID_DEFAULT_CODE
            case MPEG_VIDEO:
#endif
            {
#if 0
                if (size < 4 || memcmp("\x00\x00\x00\x01", data, 4)) {
                    return ERROR_MALFORMED;
                }
#else
                uint8_t *ptr = (uint8_t *)data;

                ssize_t startOffset = -1;
                for (size_t i = 0; i + 3 < size; ++i) {
                    if (!memcmp("\x00\x00\x00\x01", &ptr[i], 4)) {
                        startOffset = i;
                        break;
                    }
                }

                if (startOffset < 0) {
                    return ERROR_MALFORMED;
                }

                if (startOffset > 0) {
                    ALOGI("found something resembling an H.264/MPEG syncword at "
                         "offset %ld",
                         startOffset);
                }

                data = &ptr[startOffset];
                size -= startOffset;
#endif
                break;
            }
#ifndef ANDROID_DEFAULT_CODE
            case MPEG_VIDEO:
#endif
            case MPEG4_VIDEO:
            {
#if 0
                if (size < 3 || memcmp("\x00\x00\x01", data, 3)) {
                    return ERROR_MALFORMED;
                }
#else
                uint8_t *ptr = (uint8_t *)data;

                ssize_t startOffset = -1;
                for (size_t i = 0; i + 2 < size; ++i) {
                    if (!memcmp("\x00\x00\x01", &ptr[i], 3)) {
                        startOffset = i;
                        break;
                    }
                }

                if (startOffset < 0) {
                    return ERROR_MALFORMED;
                }

                if (startOffset > 0) {
                    ALOGI("found something resembling an H.264/MPEG syncword at "
                         "offset %ld",
                         startOffset);
                }

                data = &ptr[startOffset];
                size -= startOffset;
#endif
                break;
            }

#ifndef ANDROID_DEFAULT_CODE
            case VC1_VIDEO:
            {
                uint8_t *ptr = (uint8_t *)data;

                ssize_t startOffset = -1;
                for (size_t i = 0; i + 2 < size; ++i) {
                    if (!memcmp("\x00\x00\x01", &ptr[i], 3)) {
                        startOffset = i;
                        break;
                    }
                }

                if (startOffset < 0) {
                    return ERROR_MALFORMED;
                }

                if (startOffset > 0) {
                    ALOGD("found something resembling an AVS/VC1 VIDEO syncword at "
                         "offset %ld",
                         startOffset);
                }

                data = &ptr[startOffset];
                size -= startOffset;
                break;
            }
#endif
            case AAC:
            {
                uint8_t *ptr = (uint8_t *)data;

#if 0
                if (size < 2 || ptr[0] != 0xff || (ptr[1] >> 4) != 0x0f) {
                    return ERROR_MALFORMED;
                }
#else
                ssize_t startOffset = -1;
                for (size_t i = 0; i < size; ++i) {
                    if (IsSeeminglyValidADTSHeader(&ptr[i], size - i)) {
                        startOffset = i;
                        break;
                    }
                }

                if (startOffset < 0) {
                    return ERROR_MALFORMED;
                }

                if (startOffset > 0) {
                    ALOGI("found something resembling an AAC syncword at offset %ld",
                         startOffset);
                }

                data = &ptr[startOffset];
                size -= startOffset;
#endif
                break;
            }

            case MPEG_AUDIO:
            {
                uint8_t *ptr = (uint8_t *)data;

                ssize_t startOffset = -1;
                for (size_t i = 0; i < size; ++i) {
                    if (IsSeeminglyValidMPEGAudioHeader(&ptr[i], size - i)) {
                        startOffset = i;
                        break;
                    }
                }

                if (startOffset < 0) {
                    return ERROR_MALFORMED;
                }

                if (startOffset > 0) {
                    ALOGI("found something resembling an MPEG audio "
                         "syncword at offset %ld",
                         startOffset);
                }

                data = &ptr[startOffset];
                size -= startOffset;
                break;
            }
#ifndef ANDROID_DEFAULT_CODE
            case PSLPCM:
	    case LPCM:
	    case BDLPCM:

		break;
            case VORBIS_AUDIO:
            {
                uint8_t *ptr = (uint8_t *)data;
                if (!memcmp("\x76\x6f\x72\x62\x69\x73", &ptr[1], 6))
                {
                    if(mVorbisStatus > 6)
                    {
                        ALOGI("SKIP VORBIS header, type %d", ptr[0]);
                        data = &ptr[size];
                	   size -= size;
                	   break;
                    }
                    ALOGI("found VORBIS header, type %d", ptr[0]);
                }
                break;
            }
#endif
            case PCM_AUDIO:
            {
                break;
            }
            default:
                TRESPASS();
                break;
        }
    }

    size_t neededSize = (mBuffer == NULL ? 0 : mBuffer->size()) + size;
    if (mBuffer == NULL || neededSize > mBuffer->capacity()) {
        neededSize = (neededSize + 65535) & ~65535;

        ALOGV("resizing buffer to size %d", neededSize);

        sp<ABuffer> buffer = new ABuffer(neededSize);
        if (mBuffer != NULL) {
            memcpy(buffer->data(), mBuffer->data(), mBuffer->size());
            buffer->setRange(0, mBuffer->size());
        } else {
            buffer->setRange(0, 0);
        }

        mBuffer = buffer;
    }

#if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_OGM_PLAYBACK_SUPPORT)  
    if(mfgSearchStartCodeOptimize && ((mBuffer->size() == 0)))
    {
        mBuffer->meta()->setInt32("LPos", 0);
    }
#endif

    memcpy(mBuffer->data() + mBuffer->size(), data, size);
    mBuffer->setRange(0, mBuffer->size() + size);

    RangeInfo info;
    info.mLength = size;
    info.mTimestampUs = timeUs;
#ifndef ANDROID_DEFAULT_CODE
    info.mInvalidTimestamp = fgInvalidPTS;
#endif
    mRangeInfos.push_back(info);

#if 0
    if (mMode == AAC) {
        ALOGI("size = %d, timeUs = %.2f secs", size, timeUs / 1E6);
        hexdump(data, size);
    }
#endif

    return OK;
}

sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnit() {
    if ((mFlags & kFlag_AlignedData) && mMode == H264) {
        if (mRangeInfos.empty()) {
            return NULL;
        }

        RangeInfo info = *mRangeInfos.begin();
        mRangeInfos.erase(mRangeInfos.begin());

        sp<ABuffer> accessUnit = new ABuffer(info.mLength);
        memcpy(accessUnit->data(), mBuffer->data(), info.mLength);
        accessUnit->meta()->setInt64("timeUs", info.mTimestampUs);

        memmove(mBuffer->data(),
                mBuffer->data() + info.mLength,
                mBuffer->size() - info.mLength);

        mBuffer->setRange(0, mBuffer->size() - info.mLength);

        if (mFormat == NULL) {
            mFormat = MakeAVCCodecSpecificData(accessUnit);
        }

        return accessUnit;
    }

    switch (mMode) {
        case H264:
            return dequeueAccessUnitH264();
        case AAC:
            return dequeueAccessUnitAAC();
#ifndef ANDROID_DEFAULT_CODE
        case PSLPCM:
            return dequeueAccessUnitPSLPCM();
        case VORBIS_AUDIO:
            return dequeueAccessUnitVORBISAudio();
        case LPCM:         
            return dequeueAccessUnitLPCM();
        case BDLPCM:       
            return dequeueAccessUnitBDLPCM();
        case VC1_VIDEO:
            return dequeueAccessUnitVC1Video();
#endif
        case MPEG_VIDEO:
            return dequeueAccessUnitMPEGVideo();
        case MPEG4_VIDEO:
            return dequeueAccessUnitMPEG4Video();
        case PCM_AUDIO:
            return dequeueAccessUnitPCMAudio();
        default:
            CHECK_EQ((unsigned)mMode, (unsigned)MPEG_AUDIO);
            return dequeueAccessUnitMPEGAudio();
    }
}

sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitPCMAudio() {
    if (mBuffer->size() < 4) {
        return NULL;
    }

    ABitReader bits(mBuffer->data(), 4);
    CHECK_EQ(bits.getBits(8), 0xa0);
    unsigned numAUs = bits.getBits(8);
    bits.skipBits(8);
    unsigned quantization_word_length = bits.getBits(2);
    unsigned audio_sampling_frequency = bits.getBits(3);
    unsigned num_channels = bits.getBits(3);

    CHECK_EQ(audio_sampling_frequency, 2);  // 48kHz
    CHECK_EQ(num_channels, 1u);  // stereo!

    if (mFormat == NULL) {
        mFormat = new MetaData;
        mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_RAW);
        mFormat->setInt32(kKeyChannelCount, 2);
        mFormat->setInt32(kKeySampleRate, 48000);
    }

    static const size_t kFramesPerAU = 80;
    size_t frameSize = 2 /* numChannels */ * sizeof(int16_t);

    size_t payloadSize = numAUs * frameSize * kFramesPerAU;

    if (mBuffer->size() < 4 + payloadSize) {
        return NULL;
    }

    sp<ABuffer> accessUnit = new ABuffer(payloadSize);
    memcpy(accessUnit->data(), mBuffer->data() + 4, payloadSize);

    int64_t timeUs = fetchTimestamp(payloadSize + 4);
    CHECK_GE(timeUs, 0ll);
    accessUnit->meta()->setInt64("timeUs", timeUs);

    int16_t *ptr = (int16_t *)accessUnit->data();
    for (size_t i = 0; i < payloadSize / sizeof(int16_t); ++i) {
        ptr[i] = ntohs(ptr[i]);
    }

    memmove(
            mBuffer->data(),
            mBuffer->data() + 4 + payloadSize,
            mBuffer->size() - 4 - payloadSize);

    mBuffer->setRange(0, mBuffer->size() - 4 - payloadSize);

    return accessUnit;
}

#if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_OGM_PLAYBACK_SUPPORT)  
//Search start code optimiztion
//fgEnable == true: memorize last scanned buffer potition, start to search at last stop position 
//fgEnable == false: always start to search at the beginning of mbuffer queue 
void ElementaryStreamQueue::setSearchSCOptimize(bool fgEnable)
{
    mfgSearchStartCodeOptimize = fgEnable;
}
#endif

#ifndef ANDROID_DEFAULT_CODE
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitAAC() {
    Vector<size_t> ranges;
    Vector<size_t> frameOffsets;
    Vector<size_t> frameSizes;
    size_t auSize = 0;

    size_t offset = 0;
#ifndef ANDROID_DEFAULT_CODE
    bool fristGetFormat=false;
    bool fristGetFormatError=false;
#endif

    while (offset + 7 <= mBuffer->size()) {
        ABitReader bits(mBuffer->data() + offset, mBuffer->size() - offset);

        // adts_fixed_header = 7bytes

#ifndef ANDROID_DEFAULT_CODE
		uint8_t *data = mBuffer->data() + offset;
		size_t size = mBuffer->size() - offset;
		size_t invalidLength = 0;
		bool hasInvalidADTSHeader = false;
		uint32_t startCode = bits.getBits(12);
		//adts sync code: 0xFFF
		
		//[qian] find the sync code to make sure it the start of a aac frame
		while (!((Compare_EQ(startCode, 0xfffu)).empty())) {
			ABitReader tempBits(data, size);
			hasInvalidADTSHeader = true;
			uint8_t *ptr = data;
			ssize_t startOffset = -1;

			for (size_t i = 1; i < size; ++i) {
				if (IsSeeminglyValidADTSHeader(&ptr[i], size - i)) {
					startOffset = i;
					break;
				}
			}
			if (startOffset < 0) {
			ALOGE("error here , no header???, lefte byte=%d",mBuffer->size() - offset);
				return NULL;
			}

			data = &ptr[startOffset];
			size -= startOffset;
			offset += startOffset;
			invalidLength += startOffset;

			bits.skipBits(startOffset * 8);
			tempBits.skipBits(startOffset * 8);
			startCode = tempBits.getBits(12);
		}
#else
        CHECK_EQ(bits.getBits(12), 0xfffu);
#endif

/*

		adts_fixed_header()//7bytes
		{
			syncword; 12 bslbf
			ID; 1 bslbf
			layer; 2 uimsbf
			protection_absent; 1 bslbf
			profile; 2 uimsbf
			sampling_frequency_index; 4 uimsbf
			private_bit; 1 bslbf
			channel_configuration; 3 uimsbf
			original_copy; 1 bslbf
			home; 1 bslbf
		}

*/
        bits.skipBits(3);  // ID, layer
        bool protection_absent = bits.getBits(1) != 0;

        if (mFormat == NULL) {

            unsigned profile = bits.getBits(2);
#ifndef ANDROID_DEFAULT_CODE
		if(profile==3)
		{
		ALOGE("error in check aac profile");
			fristGetFormatError=true;
		}		
#else
		CHECK_NE(profile, 3u);
#endif
            unsigned sampling_freq_index = bits.getBits(4);
            bits.getBits(1);  // private_bit
            unsigned channel_configuration = bits.getBits(3);
			
#ifndef ANDROID_DEFAULT_CODE
		if(channel_configuration==0)
		{
			
			fristGetFormatError=true;
		ALOGE("error in check aac channel_configuration ");
		}	
#else
		 CHECK_NE(channel_configuration, 0u);
#endif           
            bits.skipBits(2);  // original_copy, home

            mFormat = MakeAACCodecSpecificData(
                    profile, sampling_freq_index, channel_configuration);

            int32_t sampleRate;
            int32_t numChannels;
            CHECK(mFormat->findInt32(kKeySampleRate, &sampleRate));
            CHECK(mFormat->findInt32(kKeyChannelCount, &numChannels));
	      
#ifndef ANDROID_DEFAULT_CODE
            fristGetFormat=true;
		if(sampleRate>0)
			mAudioFrameDuration = 1024*1000000ll/sampleRate;//us
	ALOGE("AACmAudioFrameDuration %lld sampleRate=%d",mAudioFrameDuration,sampleRate);
		
#endif

           ALOGE("found AAC codec config (%d Hz, %d channels)",
                 sampleRate, numChannels);
        } else {
            // profile_ObjectType, sampling_frequency_index, private_bits,
            // channel_configuration, original_copy, home
            bits.skipBits(12);
        }

        // adts_variable_header

/*
		adts_variable_header()//7bytes
		{
			copyright_identification_bit; 1 bslbf
			copyright_identification_start; 1 bslbf
			aac_frame_length; 13 bslbf//Length of the frame including headers and error_check in bytes
			adts_buffer_fullness; 11 bslbf
			number_of_raw_data_blocks_in_frame; 2 uimsfb
		}

*/

        // copyright_identification_bit, copyright_identification_start
        bits.skipBits(2);

        unsigned aac_frame_length = bits.getBits(13);

        bits.skipBits(11);  // adts_buffer_fullness

        unsigned number_of_raw_data_blocks_in_frame = bits.getBits(2);
		//[qian]Number of raw_data_block()¡¯s that are multiplexed in the
         //adts_frame() is equal to number_of_raw_data_blocks_in_frame
			//+ 1. The minimum value is 0 indicating 1 raw_data_block()
        if (number_of_raw_data_blocks_in_frame != 0) {
            // To be implemented.
           ALOGE("[TS_ERROR]only support number_of_raw_data_blocks_in_frame=0, realy=%d\n",number_of_raw_data_blocks_in_frame);
            TRESPASS();
        }

        if (offset + aac_frame_length > mBuffer->size()) {
		ALOGD("break aac_frame_length=%d,mBuffer->size()=%d",aac_frame_length,mBuffer->size());
			//[qian] notice this
            break;
        }

        size_t headerSize = protection_absent ? 7 : 9;


/*
		adts_error_check()
		{
			if (protection_absent == ¡®0¡¯)
				crc_check; 16 rpchof
		}
*/

		
#ifndef ANDROID_DEFAULT_CODE

      if(fristGetFormatError){
	  	 ALOGE("Error skip this AAC frame");
		  fetchTimestamp(aac_frame_length + invalidLength);
	        offset += aac_frame_length;
		  if(mFormat != NULL)  mFormat =NULL;
		   fristGetFormat=false;
		   fristGetFormatError=false;
      	}
	else //error skip this frame
	{
		if (hasInvalidADTSHeader) {
	  			ranges.push(aac_frame_length + invalidLength);
				hasInvalidADTSHeader = false;
			} else {
				ranges.push(aac_frame_length);
			}
		  frameOffsets.push(offset + headerSize);
	        frameSizes.push(aac_frame_length - headerSize);
	        auSize += aac_frame_length - headerSize;
	        offset += aac_frame_length;
	}
#else
        ranges.push(aac_frame_length);

        frameOffsets.push(offset + headerSize);
        frameSizes.push(aac_frame_length - headerSize);
        auSize += aac_frame_length - headerSize;

        offset += aac_frame_length;
#endif		
    }

    if (offset == 0) {
        return NULL;
    }

	//[qian] audio a ts packet should be a frame?
    int64_t timeUs = -1;

    for (size_t i = 0; i < ranges.size(); ++i) {
        int64_t tmpUs = fetchTimestamp(ranges.itemAt(i));

        if (i == 0) {
            timeUs = tmpUs;
        }
    }

    sp<ABuffer> accessUnit = new ABuffer(auSize);
    size_t dstOffset = 0;
    for (size_t i = 0; i < frameOffsets.size(); ++i) {
        size_t frameOffset = frameOffsets.itemAt(i);

        memcpy(accessUnit->data() + dstOffset,
               mBuffer->data() + frameOffset,
               frameSizes.itemAt(i));

        dstOffset += frameSizes.itemAt(i);
    }
#ifndef ANDROID_DEFAULT_CODE
    if (fristGetFormat) {//[qian]?
		mFormat->setInt32(kKeyMaxInputSize, offset * 1.3);
	ALOGE("AAC kKeyMaxInputSize=%d ", offset);
    	}
#endif
    memmove(mBuffer->data(), mBuffer->data() + offset,
            mBuffer->size() - offset);
    mBuffer->setRange(0, mBuffer->size() - offset);

    if (timeUs >= 0) {
        accessUnit->meta()->setInt64("timeUs", timeUs);
    } else {
       ALOGW("no time for AAC access unit");
    }
#ifndef ANDROID_DEFAULT_CODE

	if(mBuffer->size()>0 && frameOffsets.size()>0  && mRangeInfos.size() ==1)
	{
		RangeInfo *info =  &*mRangeInfos.begin();
	ALOGD("qian AAC correct the timestamp from %lld to %lld,mAudioFrameDuration=%lld",info->mTimestampUs,info->mTimestampUs + mAudioFrameDuration * frameOffsets.size(),mAudioFrameDuration);
		info->mTimestampUs = info->mTimestampUs + (mAudioFrameDuration * frameOffsets.size());	

		 info =  &*mRangeInfos.begin();
	ALOGE("qian AAC correct the timestamp is =%lld",info->mTimestampUs);		
	}
	
#endif	

    return accessUnit;
}
#else
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitAAC() {
    int64_t timeUs;

    size_t offset = 0;
    while (offset + 7 <= mBuffer->size()) {
        ABitReader bits(mBuffer->data() + offset, mBuffer->size() - offset);

        // adts_fixed_header

        CHECK_EQ(bits.getBits(12), 0xfffu);
        bits.skipBits(3);  // ID, layer
        bool protection_absent = bits.getBits(1) != 0;

        if (mFormat == NULL) {
            unsigned profile = bits.getBits(2);
            CHECK_NE(profile, 3u);
            unsigned sampling_freq_index = bits.getBits(4);
            bits.getBits(1);  // private_bit
            unsigned channel_configuration = bits.getBits(3);
            CHECK_NE(channel_configuration, 0u);
            bits.skipBits(2);  // original_copy, home

            mFormat = MakeAACCodecSpecificData(
                    profile, sampling_freq_index, channel_configuration);

            mFormat->setInt32(kKeyIsADTS, true);

            int32_t sampleRate;
            int32_t numChannels;
            CHECK(mFormat->findInt32(kKeySampleRate, &sampleRate));
            CHECK(mFormat->findInt32(kKeyChannelCount, &numChannels));

            ALOGI("found AAC codec config (%d Hz, %d channels)",
                 sampleRate, numChannels);
        } else {
            // profile_ObjectType, sampling_frequency_index, private_bits,
            // channel_configuration, original_copy, home
            bits.skipBits(12);
        }

        // adts_variable_header

        // copyright_identification_bit, copyright_identification_start
        bits.skipBits(2);

        unsigned aac_frame_length = bits.getBits(13);

        bits.skipBits(11);  // adts_buffer_fullness

        unsigned number_of_raw_data_blocks_in_frame = bits.getBits(2);

        if (number_of_raw_data_blocks_in_frame != 0) {
            // To be implemented.
            TRESPASS();
        }

        if (offset + aac_frame_length > mBuffer->size()) {
            break;
        }

        size_t headerSize = protection_absent ? 7 : 9;

        int64_t tmpUs = fetchTimestamp(aac_frame_length);
        CHECK_GE(tmpUs, 0ll);

        if (offset == 0) {
            timeUs = tmpUs;
        }

        offset += aac_frame_length;
    }

    if (offset == 0) {
        return NULL;
    }

    sp<ABuffer> accessUnit = new ABuffer(offset);
    memcpy(accessUnit->data(), mBuffer->data(), offset);

    memmove(mBuffer->data(), mBuffer->data() + offset,
            mBuffer->size() - offset);
    mBuffer->setRange(0, mBuffer->size() - offset);

    accessUnit->meta()->setInt64("timeUs", timeUs);

    return accessUnit;
}
#endif
int64_t ElementaryStreamQueue::fetchTimestamp(size_t size, bool* pfgInvalidPTS) {
    int64_t timeUs = -1;
    bool first = true;

    while (size > 0) {
        CHECK(!mRangeInfos.empty());

        RangeInfo *info = &*mRangeInfos.begin();
	 #ifndef ANDROID_DEFAULT_CODE
	 //Add for Special MPEG File
	 if ((first == false) && (timeUs == 0xFFFFFFFF) && (info->mTimestampUs != 0x0))
	 {
	  ALOGE("fetchTimestamp - Change: %lld  %lld \n", timeUs, info->mTimestampUs);
	   timeUs = info->mTimestampUs;
	 }
	 #endif //#ifndef ANDROID_DEFAULT_CODE

        if (first) {
            timeUs = info->mTimestampUs;
#ifndef ANDROID_DEFAULT_CODE
            if(pfgInvalidPTS)
            {
                *pfgInvalidPTS = info->mInvalidTimestamp;
                #ifdef MTK_STAGEFRIGHT_USE_XLOG
                // open by "adb shell xlog filter-add ESQueue verbose"
                if(info->mInvalidTimestamp || info->mLength > size)
                {
                	XLOGV("set mInvalidTimestamp=true\n");
                }
                #endif
                info->mInvalidTimestamp = true;
            }
#endif
            first = false;
        }

        if (info->mLength > size) {
            info->mLength -= size;

            if (first) {
                info->mTimestampUs = -1;
            }

            size = 0;
        } else {
            size -= info->mLength;

            mRangeInfos.erase(mRangeInfos.begin());
            info = NULL;
        }
    }

    if (timeUs == 0ll) {
        ALOGV("Returning 0 timestamp");
    }

    return timeUs;
}

struct NALPosition {
    size_t nalOffset;
    size_t nalSize;
};

#ifndef ANDROID_DEFAULT_CODE
void ElementaryStreamQueue::setSeeking(bool h264UsePPs) {
	mSeeking = true;
        mH264UsePPs = h264UsePPs;
}
bool ElementaryStreamQueue::IsIFrame(uint8_t *nalStart, size_t nalSize) {
    unsigned nalType = nalStart[0] & 0x1f;
    unsigned slice_type =0;		

    if((nalType >0 && nalType <6) || nalType == 19 )
    {
	ABitReader br(nalStart + 1, nalSize);
	unsigned first_mb_in_slice = parseUE(&br);
	slice_type = parseUE(&br);
    }  

    if ((mH264UsePPs && nalType == 7 ) ||        //PPS
	    (!mH264UsePPs && (nalType == 5 || slice_type ==2 ||slice_type==7))) {   // I frame
	ALOGI("%s() nalType=%d slice_type=%d ,nalSize:%d",__FUNCTION__,nalType,slice_type, nalSize);
	return true;
    }
    return false;
}

sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitH264() {
	if (accessUnits.empty()) {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    Vector<NALPosition> nals;

    size_t totalSize = 0;

    status_t err;
    const uint8_t *nalStart;
    size_t nalSize;
    bool foundSlice = false;
    bool over = false;
      while (!over && (err = getNextNALUnit(&data, &size, &nalStart, &nalSize)) == OK)  
	{
        	CHECK_GT(nalSize, 0u);

		 unsigned nalType = nalStart[0] & 0x1f;

	        bool flush = false;

	        if (nalType == 1 || nalType == 5) {
	            if (foundSlice) {
	                ABitReader br(nalStart + 1, nalSize);
	                unsigned first_mb_in_slice = parseUE(&br);

	                if (first_mb_in_slice == 0) {
	                    // This slice starts a new frame.

	                    flush = true;
	                }
	            }

	            foundSlice = true;
	        } else if ((nalType == 9 || nalType == 7) && foundSlice) {
	            // Access unit delimiter and SPS will be associated with the
	            // next frame.

	            flush = true;
	        }

	        if (flush) {
	            // The access unit will contain all nal units up to, but excluding
	            // the current one, separated by 0x00 0x00 0x00 0x01 startcodes.

	            size_t auSize = 4 * nals.size() + totalSize;
	            sp<ABuffer> MultiAccessUnit = new ABuffer(auSize);


            size_t dstOffset = 0;

	ALOGV("accessUnit contains nal types %s", out.c_str());

            const NALPosition &pos0 = nals.itemAt(nals.size() - 1);
            size_t nextScan = pos0.nalOffset + pos0.nalSize;

            int64_t timeUs = fetchTimestamp(nextScan);
            CHECK_GE(timeUs, 0ll);
			
            for (size_t i = 0; i < nals.size(); ++i) {
		  
                const NALPosition &pos = nals.itemAt(i);
		                unsigned nalType = mBuffer->data()[pos.nalOffset] & 0x1f;

			if (!mSeeking || (mSeeking && IsIFrame(mBuffer->data()+pos.nalOffset, pos.nalSize)))
		    {
			mSeeking = false;          // found I frame, seek complete

			    sp<ABuffer> accessUnit = new ABuffer(pos.nalSize);
	                      memcpy(accessUnit->data()  ,mBuffer->data() + pos.nalOffset,pos.nalSize);
				  accessUnit->meta()->setInt64("timeUs", timeUs);
				 ALOGV("i=%d,timeUs=%lld",i,timeUs);
				  accessUnits.push_back(accessUnit);
			}
			else
			{
			  //Drop AU
			}  
			memcpy(MultiAccessUnit->data() + dstOffset,"\x00\x00\x00\x01", 4);
		       memcpy(MultiAccessUnit->data() + dstOffset + 4,
							mBuffer->data() + pos.nalOffset, pos.nalSize);
			 dstOffset += pos.nalSize + 4;
                 
            }

           ALOGV("accessUnit contains nal types %s", out.c_str());

            memmove(mBuffer->data(),
                    mBuffer->data() + nextScan,
                    mBuffer->size() - nextScan);

            mBuffer->setRange(0, mBuffer->size() - nextScan);

            if (mFormat == NULL) {
                mFormat = MakeAVCCodecSpecificData(MultiAccessUnit);
            }
            over = true;
			
        }

        NALPosition pos;
        pos.nalOffset = nalStart - mBuffer->data();
        pos.nalSize = nalSize;

        nals.push(pos);

        totalSize += nalSize;
    }
	}
	if (!accessUnits.empty()) {
		sp<ABuffer> accessUnit = *accessUnits.begin();
		accessUnits.erase(accessUnits.begin());
		return accessUnit;
	} else {
		return NULL;
	}
    
}

#else
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitH264() {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    Vector<NALPosition> nals;

    size_t totalSize = 0;

    status_t err;
    const uint8_t *nalStart;
    size_t nalSize;
    bool foundSlice = false;
    while ((err = getNextNALUnit(&data, &size, &nalStart, &nalSize)) == OK) {
        CHECK_GT(nalSize, 0u);

        unsigned nalType = nalStart[0] & 0x1f;
        bool flush = false;

        if (nalType == 1 || nalType == 5) {
            if (foundSlice) {
                ABitReader br(nalStart + 1, nalSize);
                unsigned first_mb_in_slice = parseUE(&br);

                if (first_mb_in_slice == 0) {
                    // This slice starts a new frame.

                    flush = true;
                }
            }

            foundSlice = true;
        } else if ((nalType == 9 || nalType == 7) && foundSlice) {
            // Access unit delimiter and SPS will be associated with the
            // next frame.

            flush = true;
        }

        if (flush) {
            // The access unit will contain all nal units up to, but excluding
            // the current one, separated by 0x00 0x00 0x00 0x01 startcodes.

            size_t auSize = 4 * nals.size() + totalSize;
            sp<ABuffer> accessUnit = new ABuffer(auSize);

#if !LOG_NDEBUG
            AString out;
#endif

            size_t dstOffset = 0;
            for (size_t i = 0; i < nals.size(); ++i) {
                const NALPosition &pos = nals.itemAt(i);

                unsigned nalType = mBuffer->data()[pos.nalOffset] & 0x1f;

#if !LOG_NDEBUG
                char tmp[128];
                sprintf(tmp, "0x%02x", nalType);
                if (i > 0) {
                    out.append(", ");
                }
                out.append(tmp);
#endif

                memcpy(accessUnit->data() + dstOffset, "\x00\x00\x00\x01", 4);

                memcpy(accessUnit->data() + dstOffset + 4,
                       mBuffer->data() + pos.nalOffset,
                       pos.nalSize);

                dstOffset += pos.nalSize + 4;
            }

            ALOGV("accessUnit contains nal types %s", out.c_str());

            const NALPosition &pos = nals.itemAt(nals.size() - 1);
            size_t nextScan = pos.nalOffset + pos.nalSize;

            memmove(mBuffer->data(),
                    mBuffer->data() + nextScan,
                    mBuffer->size() - nextScan);

            mBuffer->setRange(0, mBuffer->size() - nextScan);

            int64_t timeUs = fetchTimestamp(nextScan);
            CHECK_GE(timeUs, 0ll);

            accessUnit->meta()->setInt64("timeUs", timeUs);

            if (mFormat == NULL) {
                mFormat = MakeAVCCodecSpecificData(accessUnit);
            }

            return accessUnit;
        }

        NALPosition pos;
        pos.nalOffset = nalStart - mBuffer->data();
        pos.nalSize = nalSize;

        nals.push(pos);

        totalSize += nalSize;
    }
    CHECK_EQ(err, (status_t)-EAGAIN);

    return NULL;
}
#endif
#ifndef ANDROID_DEFAULT_CODE
static const uint32_t kMP3HeaderMask = 0xfffe0c00;//0xfffe0cc0 add by zhihui zhang no consider channel mode
const size_t kMaxBytesChecked = 128 * 1024;
static int mp3HeaderStartAt(const uint8_t *start, int length, int header) {
    uint32_t code = 0;
    int i = 0;

    for(i=0; i<length; i++){
        code = (code<<8) + start[i];
        if ((code & kMP3HeaderMask) == (header & kMP3HeaderMask)) {
            // some files has no seq start code
            return i - 3;
        }
    }

    return -1;
}
status_t findMP3Header(const uint8_t* buf, ssize_t size, ssize_t *offset, int *pHeader) {
    uint32_t header1 = 0, header2 = 0;
    size_t frameSize = 0, frameSize2 = 0;
    bool retb = false;
    //header1 = U32_AT(buf+*offset);
    while (*offset+4 < size) {
	//bool retb = GetMPEGAudioFrameSize(header1, &frameSize,NULL,NULL,NULL,NULL); 
	//if(!retb)
	{
        //find 1st header and verify	
		for (size_t i = *offset; i < size - 4; i++) {
			if (IsSeeminglyValidMPEGAudioHeader(&buf[i], size-i)) {
			    *offset = i;
			    header1 = U32_AT(buf+*offset);
			    retb = GetMPEGAudioFrameSize(header1, &frameSize,NULL,NULL,NULL,NULL); 
			    if(!retb || (frameSize == 0))
			    {
			    	//ALOGI("1.%s err 0x%x, ofst/retb/fSz=%d/%d/%d\n", __FUNCTION__, header1, *offset, retb, frameSize);
			    	continue;
			    }
			    else
			    {
			    	//ALOGI("2.%s 0x%x, ofst/retb/fSz=%d/%d/%d\n", __FUNCTION__, header1, *offset, retb, frameSize);
			    	break;
			    }
			}
		}
		if(!retb || (frameSize == 0)){
			break;
		}
	}
	//find 2nd header and verify
	if (*offset+frameSize < size)
	{
	    *offset += frameSize;
	    header2 = U32_AT(buf+*offset);
	    if ((header2 & kMP3HeaderMask) == (header1 & kMP3HeaderMask)) {
		*pHeader = header1;
		return OK;
	    }
	    else if(GetMPEGAudioFrameSize(header2, &frameSize2,NULL,NULL,NULL,NULL) && (frameSize2 > 0)){
		header1 = header2;
		//ALOGI("3.%s 2nd 0x%x, ofst/fSz/Sz %d/%d/%d\n", __FUNCTION__, header2, *offset, frameSize2, size);	
	    }
	    else //header1's frameSize has problem, re-find header1
	    {
	    *offset -= (frameSize - 1);
		//ALOGI("4.%s 2nd err 0x%x, new ofst/fSz/sz %d/%d/%d\n", __FUNCTION__, header2, *offset, frameSize2, size);	
	    }
	}
	else {
             ALOGI("frame overflow buffer");
             break;
	}
    }
    ALOGI("%s():size:%d,Not found MP3Headr,buf:%2x %2x %2x %2x %2x %2x %2x %2x",__FUNCTION__, size,
          buf[0],buf[1],buf[2],buf[3],buf[4],buf[5],buf[6],buf[7]);
    return UNKNOWN_ERROR;
}
#endif
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitMPEGAudio() {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    if (size < 4) {
        return NULL;
    }

    uint32_t header = U32_AT(data);

    size_t frameSize;
    int samplingRate, numChannels, bitrate, numSamples;
#ifndef ANDROID_DEFAULT_CODE
    ssize_t offset = 0;
    // when mMP3Header is not set, set it
    if (mMP3Header == 0) {
	if (findMP3Header(data, size, &offset, &mMP3Header) != OK){
	    if(size > kMaxBytesChecked) {
		ALOGE("findMP3Header fail size>%d, skip this buffer",kMaxBytesChecked);
		mBuffer->setRange(0, 0);
		return NULL;
	    }
	    else {
		ALOGW("Not get mMP3Header, size:%d", size);
		return NULL;
	    }
	}
	else {
	    ALOGI("mMP3Header:%x", mMP3Header);
	}
    }
    // mMP3Header is set, check header
    int start = mp3HeaderStartAt(data+offset, size-offset, mMP3Header);
    if (start>= 0 && start+offset+3<size) {
	offset += start;
	header = U32_AT(data+offset);
	ALOGV("header:%x",header);
	bool retb = GetMPEGAudioFrameSize(header, &frameSize, &samplingRate,
		&numChannels, &bitrate, &numSamples);
	if (!retb){
	    ALOGE("GetMPEGAudioFrameSize fail, skip this buffer");
	    mBuffer->setRange(0, 0);
	    return NULL;
	}
	size -= offset;
    }
    else {
	ALOGW("not found mMP3Header,skip");
	mBuffer->setRange(0, 0);
	return NULL;
    }
#else
    CHECK(GetMPEGAudioFrameSize(
                header, &frameSize, &samplingRate, &numChannels,
                &bitrate, &numSamples));
#endif

    if (size < frameSize) {
        return NULL;
    }

    unsigned layer = 4 - ((header >> 17) & 3);

    sp<ABuffer> accessUnit = new ABuffer(frameSize);
#ifndef ANDROID_DEFAULT_CODE
    memcpy(accessUnit->data(), data+offset, frameSize);
    memmove(mBuffer->data(), mBuffer->data() + offset + frameSize, mBuffer->size() - offset - frameSize);

    mBuffer->setRange(0, mBuffer->size() - offset - frameSize);

    int64_t timeUs = fetchTimestamp(offset+frameSize);
#else
    memcpy(accessUnit->data(), data, frameSize);

    memmove(mBuffer->data(),
            mBuffer->data() + frameSize,
            mBuffer->size() - frameSize);

    mBuffer->setRange(0, mBuffer->size() - frameSize);

    int64_t timeUs = fetchTimestamp(frameSize);
#endif
    CHECK_GE(timeUs, 0ll);

    accessUnit->meta()->setInt64("timeUs", timeUs);

    if (mFormat == NULL) {
        mFormat = new MetaData;

        switch (layer) {
            case 1:
                mFormat->setCString(
                        kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_MPEG_LAYER_I);
                break;
            case 2:
#ifndef ANDROID_DEFAULT_CODE
                mFormat->setCString(
                        kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_MPEG);
#else                    
                mFormat->setCString(
                        kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_MPEG_LAYER_II);
#endif
                break;
            case 3:
                mFormat->setCString(
                        kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_MPEG);
                break;
            default:
                TRESPASS();
        }

        mFormat->setInt32(kKeySampleRate, samplingRate);
        mFormat->setInt32(kKeyChannelCount, numChannels);
#ifndef ANDROID_DEFAULT_CODE		
#ifdef MTK_OGM_PLAYBACK_SUPPORT
        mFormat->setInt32(kKeyMPEGAudLayer, (int32_t)layer);
#endif
#endif
    }

    return accessUnit;
}
#ifndef ANDROID_DEFAULT_CODE
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitPSLPCM() {
	/*	http://www.mpucoder.com/DVD/ass-hdr.html
		10100DDD|NNNNNNNN|LLLLLLLL|LLLLLLLL|E M R NNNNN|BB SS R CCC|XXX YYYYY
		10000DDD(8bits) : sub stream ID 0xA0-A7 (DDD: id)
		NNNNNNNN(8bits) : number of frame headers
		LLL...(16bits) : first access unit pointer
		E : audio emphasis flag (0:off, 1:on)
		M : audio mute flag (0:off, 1:on)
		R : reserved
		NNNNN(5bits) : audio frame number
		BB(2bits) : Bits per sample[bits] (0:16, 1:20, 2:24, 3:reserved)
		SS(2bits) : Sampling frequency[kHz] (0:48, 1:96, etc:reserved)
		CCC(3bits) : number of channels[ch] (CCC + 1)
		XXX(3bits) : dynamic range X (linear gain = 2^(4-(X+(Y/30))))
		YYYYY(5bits) : dynamic range Y (dB gain = 24.082 - 6.0206 X - 0.2007 Y)
	*/
	uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();
	size_t offset = 0;	
	size_t frameSize;	
	int samplingRate, numChannels, bitrate, numSamples;

	if(mBuffer->size()< 7){
		return NULL;
	}
	ABitReader bits(mBuffer->data() + offset, mBuffer->size() - offset);		
	unsigned streamID = bits.getBits(8);		
	while( streamID < 0xa0 || streamID > 0xa7 ){
		streamID = bits.getBits(8);
		offset++;
	}
	/*unsigned frame_header_count=*/bits.getBits(8);
	unsigned first_access_unit_pointer=bits.getBits(16);
	//offset = offset  + 3 + first_access_unit_pointer;
	/*bool audio_emphasis_flag=*/bits.getBits(1);
	/*bool audio_mute_flag=*/bits.getBits(1);
	bits.getBits(1);
	unsigned audio_frame_num = bits.getBits(5);
	unsigned bits_per_sample=bits.getBits(2);
	if(bits_per_sample == 0){
		numSamples = 16;
	}
	else if(bits_per_sample == 1){
		numSamples = 20;
	}	
	else{
		numSamples = 24;
	}
	unsigned sampling_frequency= bits.getBits(2);
	if(sampling_frequency == 0){
		samplingRate = 48000;
	}
	else{
		samplingRate = 96000;
	}
	bits.getBits(1);
	numChannels = bits.getBits(3) + 1;
	/*unsigned dynamic_range_X=*/bits.getBits(3);
	/*unsigned dynamic_range_Y=*/bits.getBits(5);
	uint16_t framesize;
	//framesize = samplingRate * numSamples * numChannels * audio_frame_num / 8;
	//if (framesize > size) {
		if ((size-7)%2 != 0) {
			framesize = (size-7) - 1;
		}
		else {
			framesize = (size-7);
		}
	//}
	ALOGD("Warning: framesize:%d size:%d", framesize, size);

    sp<ABuffer> accessUnit = new ABuffer(framesize);
    uint8_t *ptr = accessUnit->data();

#ifdef MTK_AUDIO_RAW_SUPPORT
	memcpy(ptr, data+7, framesize);
#else
	// 16 bit big endian to little endian 
    int j = 0;
    for (int i=0; i < (framesize/2); i++) {
        j = 2*i; 
        ptr[j] = data[j+8];
        ptr[j+1] = data[j+7]; 
    }
#endif

    mBuffer->setRange(0, 0);

    int64_t timeUs = fetchTimestamp(size);
    ALOGD("PCM DEQUEUE timeUs=%d framesize is %d buffer size is %d size is %d offset is %d",timeUs,framesize,mBuffer->size(),size,offset);
    accessUnit->meta()->setInt64("timeUs", timeUs);

	if(numSamples!= 16 
#ifdef MTK_AUDIO_RAW_SUPPORT		
		&& numSamples!= 24
#endif
	)
	{
		ALOGD("PCM BitWidth %d not support\n", numSamples);
		return accessUnit;
	}
    else if (mFormat == NULL) {
	mFormat = new MetaData;

	mFormat->setCString(
                 kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_RAW);

	mFormat->setInt32(kKeySampleRate, samplingRate);
	mFormat->setInt32(kKeyChannelCount, numChannels);
#ifdef MTK_AUDIO_RAW_SUPPORT
	mFormat->setInt32(kKeyEndian, 1);	//1: big endian, 2: little endia
	mFormat->setInt32(kKeyBitWidth, numSamples);
	mFormat->setInt32(kKeyPCMType, 3);	//1: WAV file, 2: BD file, 3: DVD_VOB file, 4: DVD_AOB file
	mFormat->setInt32(kKeyChannelAssignment, 0x1b);	// 2 channels
	ALOGD("PCM SampleRate %d, ChannelCount %d, Big endian, BitWidth %d, PCMType:DVD_VOB\n", 
		samplingRate, numChannels, numSamples);

#endif
    }

    return accessUnit;
}
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitLPCM() {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();
    const int64_t framesize = 1920;

    if (size < 4) {
	return NULL;
    }

    PCM_Header *pcmHeader = (PCM_Header *)data;
    if (pcmHeader->sub_stream_id != 0xA0 || pcmHeader->number_of_frame_header != 0x6) {
	ALOGE("pcmHeader incorrent, subid:%d, numHeader:%d", pcmHeader->sub_stream_id, pcmHeader->number_of_frame_header);
        mBuffer->setRange(0, 0);
        return NULL;
    }

    sp<ABuffer> accessUnit = new ABuffer(framesize);
    uint8_t *ptr = accessUnit->data();
#if 1
    int j = 0;
    for (int i=0; i < (framesize/2); i++) {
        j = 2*i; 
        ptr[j] = data[4+j+1];
        ptr[j+1] = data[4+j]; 
    }
#else
    memcpy(ptr, data+4, framesize);
#endif
    mBuffer->setRange(0, 0);

    int64_t timeUs = fetchTimestamp(size);
    CHECK_GE(timeUs, 0ll);
    accessUnit->meta()->setInt64("timeUs", timeUs);
    ALOGV("PCM: size:%d, timeUs:%lld", size, timeUs);

    if (mFormat == NULL) {
	mFormat = new MetaData;

	mFormat->setCString(
                 kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_RAW);
	if (pcmHeader->audio_sampling_frequency == 1) {
	    ALOGW("SampleRate 44100");
	    mFormat->setInt32(kKeySampleRate, 44100);
	}
	else if (pcmHeader->audio_sampling_frequency == 2) {
	    ALOGW("SampleRate 48000");
	    mFormat->setInt32(kKeySampleRate, 48000);
	}
    else {
	    mFormat.clear();
	    ALOGW("SampleRate is uncorrect");
	}

	if (((pcmHeader->number_of_audio_channel != 1) && 
		    (pcmHeader->number_of_audio_channel == 0)) ||
		pcmHeader-> quantization_word_length !=0){ 
	    mFormat.clear();
	    ALOGW("channel is uncorrect");
	}
	else {
	    mFormat->setInt32(kKeyChannelCount, 2);
	}
    }

    return accessUnit;
}
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitBDLPCM() {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    if (size < 4) {
	return NULL;
    }

    uint16_t framesize = U16_AT(data);
    data += 4;
    size -= 4;
	ALOGI("framesize:%d size:%d", framesize, size);
    if (framesize > size) {
	ALOGI("Warning: framesize:%d size:%d", framesize, size);
	if (size%2 != 0) {
	    framesize = size - 1;
	}
	else {
	    framesize = size;
	}
    }

    sp<ABuffer> accessUnit = new ABuffer(framesize);
    uint8_t *ptr = accessUnit->data();

    int j = 0;
    for (int i=0; i < (framesize/2); i++) {
        j = 2*i; 
        ptr[j] = data[j+1];
        ptr[j+1] = data[j]; 
    }


    mBuffer->setRange(0, 0);

    int64_t timeUs = fetchTimestamp(size+4);
    CHECK_GE(timeUs, 0ll);
    accessUnit->meta()->setInt64("timeUs", timeUs);

    if (mFormat == NULL) {
	mFormat = new MetaData;

	mFormat->setCString(
                 kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_RAW);

	mFormat->setInt32(kKeySampleRate, 48000);
	mFormat->setInt32(kKeyChannelCount, 2);
    }

    return accessUnit;
}

#endif  // ANDROID_DEFAULT_CODE

static void EncodeSize14(uint8_t **_ptr, size_t size) {
    CHECK_LE(size, 0x3fff);

    uint8_t *ptr = *_ptr;

    *ptr++ = 0x80 | (size >> 7);
    *ptr++ = size & 0x7f;

    *_ptr = ptr;
}

static sp<ABuffer> MakeMPEGVideoESDS(const sp<ABuffer> &csd) {
    sp<ABuffer> esds = new ABuffer(csd->size() + 25);

    uint8_t *ptr = esds->data();
    *ptr++ = 0x03;
    EncodeSize14(&ptr, 22 + csd->size());

    *ptr++ = 0x00;  // ES_ID
    *ptr++ = 0x00;

    *ptr++ = 0x00;  // streamDependenceFlag, URL_Flag, OCRstreamFlag

    *ptr++ = 0x04;
    EncodeSize14(&ptr, 16 + csd->size());

    *ptr++ = 0x40;  // Audio ISO/IEC 14496-3

    for (size_t i = 0; i < 12; ++i) {
        *ptr++ = 0x00;
    }

    *ptr++ = 0x05;
    EncodeSize14(&ptr, csd->size());

    memcpy(ptr, csd->data(), csd->size());

    return esds;
}

sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitMPEGVideo() {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    bool sawPictureStart = false;
    int pprevStartCode = -1;
    int prevStartCode = -1;
    int currentStartCode = -1;

    size_t offset = 0;
#ifndef ANDROID_DEFAULT_CODE 
    #ifdef MTK_OGM_PLAYBACK_SUPPORT //for tablet only
    
    int prev_sawPictureStart = 0;

    //Search start code optimiztion
    //mfgSearchStartCodeOptimize == true: memorize last scanned buffer potition, start to search at last stop position 
    //mfgSearchStartCodeOptimize == false: always start to search at the beginning of mbuffer queue 
    int32_t prev_offset = 0;
    if(!mSeeking)
    {
    if(mBuffer->meta()->findInt32("LPos", &prev_offset))
    {
    	offset = (mfgSearchStartCodeOptimize && (mFormat != NULL) && (prev_offset > 4))?(prev_offset - 4): 0;
            if((offset>0) && mBuffer->meta()->findInt32("PicS", &prev_sawPictureStart))
            {
                sawPictureStart = (mfgSearchStartCodeOptimize && (mFormat != NULL))?(prev_sawPictureStart>0):false;
            }
        }
        ALOGV("offset %d/sawPictureStart %d\n", offset, sawPictureStart);
    }
    #endif

    size_t lastGOPOff= -1;
#endif

    while (offset + 3 < size) {
        if (memcmp(&data[offset], "\x00\x00\x01", 3)) {
            ++offset;
            continue;
        }

        pprevStartCode = prevStartCode;
        prevStartCode = currentStartCode;
        currentStartCode = data[offset + 3];

        if (currentStartCode == 0xb3 && mFormat == NULL) {
            memmove(mBuffer->data(), mBuffer->data() + offset, size - offset);
            size -= offset;
            (void)fetchTimestamp(offset);
            offset = 0;
            mBuffer->setRange(0, size);
        }

        if ((prevStartCode == 0xb3 && currentStartCode != 0xb5)
                || (pprevStartCode == 0xb3 && prevStartCode == 0xb5)) {
            // seqHeader without/with extension

            if (mFormat == NULL) {
                CHECK_GE(size, 7u);

                unsigned width =
                    (data[4] << 4) | data[5] >> 4;

                unsigned height =
                    ((data[5] & 0x0f) << 8) | data[6];

                mFormat = new MetaData;
                mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG2);
                mFormat->setInt32(kKeyWidth, width);
                mFormat->setInt32(kKeyHeight, height);

                ALOGI("found MPEG2 video codec config (%d x %d)", width, height);

                sp<ABuffer> csd = new ABuffer(offset);
                memcpy(csd->data(), data, offset);

                memmove(mBuffer->data(),
                        mBuffer->data() + offset,
                        mBuffer->size() - offset);

                mBuffer->setRange(0, mBuffer->size() - offset);
                size -= offset;
                (void)fetchTimestamp(offset);
                offset = 0;

                // hexdump(csd->data(), csd->size());

                sp<ABuffer> esds = MakeMPEGVideoESDS(csd);
                mFormat->setData(
                        kKeyESDS, kTypeESDS, esds->data(), esds->size());

                return NULL;
            }
        }
#ifndef ANDROID_DEFAULT_CODE 
        // save the GOP, send to decode when seek. if Format is NULL, lastGOPOff make no sense
        if (mSeeking && currentStartCode == 0xB8 && mFormat != NULL) {
            lastGOPOff = offset; 
        }
#endif
        if (mFormat != NULL && (currentStartCode == 0x00 || (sawPictureStart && currentStartCode == 0xB7))) { //ALPS00473447
#ifndef ANDROID_DEFAULT_CODE 
	    if (mSeeking) { 
		if (((data[offset+5]>>3) & 0x7) == 1) {          // I frame
		    mSeeking = false;

		    size_t tmpOff = offset;
		    if (lastGOPOff != -1) {
			tmpOff = lastGOPOff;
			ALOGI("Send GOP when seeking, offset:%x lastGOPOff:%x", offset, lastGOPOff);
		    }
		    memmove(mBuffer->data(), mBuffer->data() + tmpOff, size - tmpOff);
		    size -= tmpOff;
		    (void)fetchTimestamp(tmpOff);
		    offset = offset - tmpOff;
		    mBuffer->setRange(0, size);
		    ALOGI("Found I Frame when seeking");
		    mfgFirstFrmAfterSeek = true;
		}
		else {
		    offset++;
		    continue;
		}
	    }
#endif
            // Picture start

            if (!sawPictureStart) {
                sawPictureStart = true;
            } else {
                sp<ABuffer> accessUnit = new ABuffer(offset);
                memcpy(accessUnit->data(), data, offset);

                memmove(mBuffer->data(),
                        mBuffer->data() + offset,
                        mBuffer->size() - offset);

                mBuffer->setRange(0, mBuffer->size() - offset);

                bool fgInvalidTimeUs = false;
                int64_t timeUs = fetchTimestamp(offset, &fgInvalidTimeUs);
                CHECK_GE(timeUs, 0ll);

                offset = 0;
                #if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_OGM_PLAYBACK_SUPPORT)  
                if(mfgSearchStartCodeOptimize)
                {
                      mBuffer->meta()->setInt32("LPos", 0);
                      mBuffer->meta()->setInt32("PicS", 0);
                }
                #endif

                accessUnit->meta()->setInt64("timeUs", timeUs);
#ifndef ANDROID_DEFAULT_CODE
                accessUnit->meta()->setInt32("invt", (int32_t)fgInvalidTimeUs);
                ALOGV("AU time %lld/%d,FrmType %d,%x %x %x %x %x %x,sz %d\n", timeUs, fgInvalidTimeUs, (*(accessUnit->data()+5)>>3) & 0x7,
                      *(accessUnit->data()+0), *(accessUnit->data()+1), *(accessUnit->data()+2), *(accessUnit->data()+3),
                      *(accessUnit->data()+4), *(accessUnit->data()+5),
                      accessUnit->size());
                if(mfgFirstFrmAfterSeek)
                {
                      if(fgInvalidTimeUs)
                      {
                            ALOGI("Keep search I with valid PTS\n");
                            mSeeking = true;
                            lastGOPOff = -1;
                            size = mBuffer->size();
                            sawPictureStart = false;
                            continue;
                      }
                      else
                      {
                            mfgFirstFrmAfterSeek = false;
                      }
                }
#endif
                ALOGV("returning MPEG video access unit at time %lld us",
                      timeUs);

                // hexdump(accessUnit->data(), accessUnit->size());

                return accessUnit;
            }
        }

        ++offset;
    }

#ifndef ANDROID_DEFAULT_CODE
    if (mSeeking) {                         // discard buffer
	(void)fetchTimestamp(offset);
	memmove(mBuffer->data(),
		mBuffer->data() + offset,
		mBuffer->size() - offset);

	mBuffer->setRange(0, mBuffer->size() - offset);
	offset = 0;
    }
#endif
#if !defined(ANDROID_DEFAULT_CODE) && defined(MTK_OGM_PLAYBACK_SUPPORT)  
    if(mfgSearchStartCodeOptimize)
    {
		mBuffer->meta()->setInt32("LPos", ((lastGOPOff != -1) && (offset > 0))?(int)lastGOPOff:(int)offset);
		mBuffer->meta()->setInt32("PicS", ((lastGOPOff != -1) || (offset == 0))?0:(int)sawPictureStart);
    }
#endif
    return NULL;
}

static ssize_t getNextChunkSize(
        const uint8_t *data, size_t size) {
    static const char kStartCode[] = "\x00\x00\x01";

    if (size < 3) {
        return -EAGAIN;
    }

    if (memcmp(kStartCode, data, 3)) {
        TRESPASS();
    }

    size_t offset = 3;
    while (offset + 2 < size) {
        if (!memcmp(&data[offset], kStartCode, 3)) {
            return offset;
        }

        ++offset;
    }

    return -EAGAIN;
}

sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitMPEG4Video() {
    uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    enum {
        SKIP_TO_VISUAL_OBJECT_SEQ_START,
        EXPECT_VISUAL_OBJECT_START,
        EXPECT_VO_START,
        EXPECT_VOL_START,
        WAIT_FOR_VOP_START,
        SKIP_TO_VOP_START,
        #ifndef ANDROID_DEFAULT_CODE 
		WAIT_FOR_VOL_START,
        #endif
    } state;

    if (mFormat == NULL) {
		#ifndef ANDROID_DEFAULT_CODE
		state = WAIT_FOR_VOL_START;
		#else
		state = SKIP_TO_VISUAL_OBJECT_SEQ_START;
		#endif
    } else {
        state = SKIP_TO_VOP_START;
    }

    int32_t width = -1, height = -1;

    size_t offset = 0;
    ssize_t chunkSize;
    while ((chunkSize = getNextChunkSize(
                    &data[offset], size - offset)) > 0) {
        bool discard = false;

        unsigned chunkType = data[offset + 3];

        switch (state) {
            case SKIP_TO_VISUAL_OBJECT_SEQ_START:
            {
                if (chunkType == 0xb0) {
                    // Discard anything before this marker.

                    state = EXPECT_VISUAL_OBJECT_START;
                } else {
#ifndef ANDROID_DEFAULT_CODE
                    offset += chunkSize;
#endif
                    discard = true;
                }
                break;
            }

            case EXPECT_VISUAL_OBJECT_START:
            {
                CHECK_EQ(chunkType, 0xb5);
                state = EXPECT_VO_START;
                break;
            }

            case EXPECT_VO_START:
            {
                CHECK_LE(chunkType, 0x1f);
                state = EXPECT_VOL_START;
                break;
            }

            case EXPECT_VOL_START:
            {
                CHECK((chunkType & 0xf0) == 0x20);

                CHECK(ExtractDimensionsFromVOLHeader(
                            &data[offset], chunkSize,
                            &width, &height));

                state = WAIT_FOR_VOP_START;
                break;
            }
#ifndef ANDROID_DEFAULT_CODE
						case WAIT_FOR_VOL_START:
						{
							if((chunkType & 0xf0) == 0x20)
							{
								CHECK(ExtractDimensionsFromVOLHeader(
								&data[offset], chunkSize,
								&width, &height));
			
								state = WAIT_FOR_VOP_START;
								break;
							}
							else
							{
								offset += chunkSize;
								discard = true;
							}
							break;
						}
#endif

            case WAIT_FOR_VOP_START:
            {
                if (chunkType == 0xb3 || chunkType == 0xb6) {
                    // group of VOP or VOP start.

                    mFormat = new MetaData;
                    mFormat->setCString(
                            kKeyMIMEType, MEDIA_MIMETYPE_VIDEO_MPEG4);

                    mFormat->setInt32(kKeyWidth, width);
                    mFormat->setInt32(kKeyHeight, height);

                    ALOGI("found MPEG4 video codec config (%d x %d)",
                         width, height);

                    sp<ABuffer> csd = new ABuffer(offset);
                    memcpy(csd->data(), data, offset);

                    // hexdump(csd->data(), csd->size());

                    sp<ABuffer> esds = MakeMPEGVideoESDS(csd);
                    mFormat->setData(
                            kKeyESDS, kTypeESDS,
                            esds->data(), esds->size());

                    discard = true;
                    state = SKIP_TO_VOP_START;
                }

                break;
            }

            case SKIP_TO_VOP_START:
            {
                if (chunkType == 0xb6) {
                    offset += chunkSize;					
#ifndef ANDROID_DEFAULT_CODE
					if (mSeeking) {
						switch (data[4] & 0xC0) {
							case 0x00:
								mSeeking = false;
								ALOGI("I frame");
								break;
							case 0x40:
								ALOGI("P frame");
								break;
								//continue;
							case 0x80:
								ALOGI("B frame");
								break;
								//continue;
							default:
								ALOGI("default");
								break;
								//continue;
						}
					}

					if(mSeeking)
						{
							discard = true;
							break;
						}
#endif
					sp<ABuffer> accessUnit = new ABuffer(offset);
                    memcpy(accessUnit->data(), data, offset);

                    memmove(data, &data[offset], size - offset);
                    size -= offset;
                    mBuffer->setRange(0, size);

                    int64_t timeUs = fetchTimestamp(offset);
                    CHECK_GE(timeUs, 0ll);

                    offset = 0;

                    accessUnit->meta()->setInt64("timeUs", timeUs);

                    ALOGV("returning MPEG4 video access unit at time %lld us",
                         timeUs);

                    // hexdump(accessUnit->data(), accessUnit->size());

                    return accessUnit;
                } else if (chunkType != 0xb3) {
                    offset += chunkSize;
                    discard = true;
                }

                break;
            }

            default:
                TRESPASS();
        }

        if (discard) {
            (void)fetchTimestamp(offset);
            memmove(data, &data[offset], size - offset);
            size -= offset;
            offset = 0;
            mBuffer->setRange(0, size);
        } else {
            offset += chunkSize;
        }
    }

    return NULL;
}

#ifndef ANDROID_DEFAULT_CODE
//=========================================================================================================//
//Vorbis Header Parsing Funcitons
//=========================================================================================================//
status_t vorbis_parse_infoheader(const uint8_t *data, uint8_t *ChannelCount, uint32_t *SampleRate, uint32_t *Bitrate)
{
	uint8_t vorbisAudioChannels;
	uint32_t vorbisSampleRate;
	uint32_t vorbisBitrateMax;
	uint32_t vorbisBitrateMin;
	uint32_t vorbisBitrateNominal;
	data += 7;	// Skip Type and vorbis syncword
	data += 4;	// Skip Vorbis Version
	vorbisAudioChannels = data[0];
	data += 1;
	vorbisSampleRate = U32LE_AT(data);
	data += 4;
	vorbisBitrateMax = U32LE_AT(data);
	data += 4;
	vorbisBitrateNominal = U32LE_AT(data);
	data += 4;
	vorbisBitrateMin = U32LE_AT(data);
	data += 4;
	data += 2;	// useless size field  

	*ChannelCount = vorbisAudioChannels;
	*SampleRate = vorbisSampleRate;
	*Bitrate = (vorbisBitrateNominal == 0)?(vorbisBitrateMax + vorbisBitrateMin)/2 : vorbisBitrateNominal;
	return OK;
}

sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitVORBISAudio()
{
	uint8_t *data = mBuffer->data();
	size_t size = mBuffer->size();
	sp<ABuffer> accessUnit = NULL;

	if(size == 0)	return NULL;

	//In this case, all data in should be a header buffer
	if(mVorbisStatus < 5)
	{
		if(data[0] == 1 && mVorbisStatus == 0)
		{
			if(mFormat == NULL)
			{
				uint8_t ChannelCount;
				uint32_t SampleRate;
				uint32_t Bitrate;
				vorbis_parse_infoheader(data, &ChannelCount, &SampleRate, &Bitrate);
				ALOGI("Vorbis Create Formate with %d channels, %d samplerate", ChannelCount, SampleRate);
				mFormat = new MetaData;
				mFormat->setCString(kKeyMIMEType, MEDIA_MIMETYPE_AUDIO_VORBIS);
				mFormat->setData(kKeyVorbisInfo, 0, data, size);
				mFormat->setInt32(kKeySampleRate, SampleRate);
				mFormat->setInt32(kKeyChannelCount, ChannelCount);
				mFormat->setInt32(kKeyBitRate, Bitrate);
			}
			mVorbisStatus = 2;
			mBuffer->setRange(0, 0);
			return NULL;
		}
		else if(data[0] == 3 && mVorbisStatus == 2)
		{
			ALOGI("Parsing Comment header?");
			mBuffer->setRange(0, 0);
			mVorbisStatus = 4;
			return NULL;
		}
		else if(data[0] == 5 && mVorbisStatus == 4)
		{
			ALOGI("Parsing Books header, size = %d", size);
			mFormat->setData(kKeyVorbisBooks, 0, data, size);
			mBuffer->setRange(0, 0);
			mVorbisStatus = 6;
			return NULL;
		}
		else
		{
			ALOGI("Header info not completed, drop data, size = %d", size);
			mBuffer->setRange(0, 0);
			return NULL;
		}
	}
	else
	{
		size_t checksize = size;
		while(checksize >= 0xFF)	checksize -= 0xFF;
		if(checksize)
		{
			sp<ABuffer> accessUnit = new ABuffer(size);
			memcpy(accessUnit->data(), data, size);
			mBuffer->setRange(0, 0);
			int64_t timeUs = fetchTimestamp(size);
			if( timeUs < 0)	
			{
				ALOGI("timeUs error");
				timeUs = 0;
			}
			CHECK_GE(timeUs, 0ll);
			accessUnit->meta()->setInt64("timeUs", timeUs);
			//hexdump(accessUnit->data(), accessUnit->size());
			return accessUnit;
		}
		else
			return NULL;
	}
}
#endif
#ifndef ANDROID_DEFAULT_CODE
struct asf_bitmapinfoheader_s {
        uint32_t biSize;
        uint32_t biWidth;
        uint32_t biHeight;
        uint16_t biPlanes;
        uint16_t biBitCount;
        uint32_t biCompression;
        uint32_t biSizeImage;
        uint32_t biXPelsPerMeter;
        uint32_t biYPelsPerMeter;
        uint32_t biClrUsed;
        uint32_t biClrImportant;
        uint8_t *data;
};
#if 0     // check Frame Type
static uint32_t glace = (uint32_t)"lace";
#endif 
#define ASF_BITMAPINFOHEADER_SIZE 40
sp<ABuffer> ElementaryStreamQueue::dequeueAccessUnitVC1Video() {
    const uint8_t *data = mBuffer->data();
    size_t size = mBuffer->size();

    int  prevStartCode    = -1;
    int  currentStartCode = -1;
    int  pprevStartCode = -1;

    size_t offset = 0;
    bool sawPicture = false;
    while (offset + 3 < size) {
        if (memcmp(&data[offset], "\x00\x00\x01", 3)) {
            ++offset;
            continue;
        }
        ALOGV("%02x %02x %02x %02x", data[offset+0], data[offset+1], data[offset+2], data[offset+3]);
        pprevStartCode = prevStartCode;
        prevStartCode = currentStartCode;
        currentStartCode = data[offset + 3];

        /*video sequence start, video_edit_code*/
        if (currentStartCode == 0x0F && mFormat == NULL) {
            if (offset) {
                memmove(mBuffer->data(), mBuffer->data() + offset, size - offset);
                size -= offset;
		(void)fetchTimestamp(offset);
                mBuffer->setRange(0, size);
                offset = 0;
            }
        }

        /* parse sequence_header */
        if ((prevStartCode == 0x0F && currentStartCode != 0x0E)
                || (pprevStartCode == 0x0F && prevStartCode == 0x0E)) {
            if (mFormat == NULL) {
                ABitReader bits(data, offset);

                bits.skipBits(32);/*start code 00 00 01 0F*/
                uint8_t profile =  bits.getBits(2);
                ALOGD("dequeueAccessUnitVC1Video:   profile is:%u", profile);
		if (profile != 3 ) {
		    return NULL;
		}

                uint8_t level =  bits.getBits(3);
                ALOGD("dequeueAccessUnitVC1Video:   level is:%u", level);

                bits.skipBits(2); /*COLORDIFF_FORMAT*/
                bits.skipBits(3); /*FRMRTQ_POSTPROC*/
                bits.skipBits(5); /*BITRTQ_POSTPROC*/
                bits.skipBits(1); /*POSTPROCFLAG*/

                uint32_t pictureWidth = (bits.getBits(12) * 2) + 2;
                ALOGD("dequeueAccessUnitVC1Video:   pictureWidth:%u", pictureWidth);

                uint32_t pictureHeight = (bits.getBits(12) * 2) + 2;
                ALOGD("dequeueAccessUnitVC1Video:   pictureHeight:%u", pictureHeight);
#if 0     // check Frame Type
                bits.skipBits(1);     /* */
                uint32_t interlace = bits.getBits(1);
#endif
                
                sp<ABuffer> WMVCData = new ABuffer(offset + ASF_BITMAPINFOHEADER_SIZE);
		uint8_t  codecProfile = 8;/*codec specific data*/
		struct asf_bitmapinfoheader_s header;
                memset(&header, 0, ASF_BITMAPINFOHEADER_SIZE);
                header.biSize = offset + ASF_BITMAPINFOHEADER_SIZE;
                header.biWidth = pictureWidth;
                header.biHeight = pictureHeight;
                memcpy(&(header.biCompression), "WVC1", 4);
#if 0
                memcpy(WMVCData->data(), (void *)&codecProfile, 1);
                memcpy(WMVCData->data() + 1, (void *)&pictureWidth, 2);
                memcpy(WMVCData->data() + 3, (void *)&pictureHeight, 2);
#else
                memcpy(WMVCData->data(), (void *)&header, ASF_BITMAPINFOHEADER_SIZE);
#endif

                memcpy(WMVCData->data() + ASF_BITMAPINFOHEADER_SIZE, data, offset);

                mFormat = new MetaData;
                mFormat->setData(kKeyWMVC, 0, WMVCData->data(), WMVCData->size());
                mFormat->setCString(kKeyMIMEType, "video/x-ms-wmv");
                mFormat->setInt32(kKeyWidth, pictureWidth);
                mFormat->setInt32(kKeyHeight, pictureHeight);
#if 0     // check Frame Type
                mFormat->setInt32(glace, (int32_t)interlace);
#endif

		memmove(mBuffer->data(), mBuffer->data() + offset, size - offset);
		size -= offset;
		(void)fetchTimestamp(offset);
		mBuffer->setRange(0, mBuffer->size() - offset);
		offset = 0;
                return NULL;
            }

        }
	if (currentStartCode == 0x0E && mFormat != NULL && mSeeking) {
		mSeeking = false;
		memmove(mBuffer->data(), mBuffer->data() + offset, size - offset);
		size -= offset;
		(void)fetchTimestamp(offset);
		offset = 0;
		mBuffer->setRange(0, size);
		ALOGI("Found entry header when seeking");
	}
        if (currentStartCode == 0x0D && mFormat != NULL && !mSeeking) {
            /*frame start code*/
#if 0     // check Frame Type
            int32_t lace = 0;
            ALOGV("%2x %2x %2x %2x %2x", data[0], data[1], data[2], data[3], data[4]);
            uint8_t data0 = data[0];
	    if (mFormat->findInt32(glace, &lace) && lace == 1) {
                  ALOGI("It is interlace");
		  if (data0 & 0x80 == 0x80) {
		      data0 = data0 << 2;
		  }
		  else {
		      data0 = data0 << 1;
		  }
	    }
	    if ((data[4] & 0xE0) == 0xC0) {
                 ALOGI("I frame");
	    }
	    else if ((data[4] & 0x80) == 0x0) {
                 ALOGI("P frame");
	    }
	    else if ((data[4] & 0xC0) == 0x80) {
                 ALOGI("B frame");
	    }
	    else if ((data[4] & 0xF0) == 0xE0) {
                 ALOGI("BI frame");
	    }
	    else if ((data[4] & 0xF0) == 0xF0) {
                 ALOGI("Skipped frame");
	    }
#endif

	    if (!sawPicture) {
		sawPicture = true;
	    }
	    else {
		sp<ABuffer> accessUnit = new ABuffer(offset);
		memcpy(accessUnit->data(), data, offset);

		memmove(mBuffer->data(),
			mBuffer->data() + offset,
			mBuffer->size() - offset);

		mBuffer->setRange(0, mBuffer->size() - offset);

		int64_t timeUs = fetchTimestamp(offset);
		CHECK_GE(timeUs, 0ll);
		offset = 0;
		accessUnit->meta()->setInt64("timeUs", timeUs);

		return accessUnit;
	    }
        }
#if 0 
        else if (prevStartCode == 0x0E/* && currentStartCode == 0x0D*/) {
            /*entry point header*/
            ALOGI("entry point header");
            memmove(mBuffer->data(), mBuffer->data() + offset, size - offset);
            (void)fetchTimestamp(offset);
            size -= offset;
            offset = 0;
            mBuffer->setRange(0, size);
        }
#endif

        ++offset;
    }/*while*/
    if (mSeeking) {                         // discard buffer
	(void)fetchTimestamp(offset);
	memmove(mBuffer->data(),
		mBuffer->data() + offset,
		mBuffer->size() - offset);
        size -= offset;
	mBuffer->setRange(0, mBuffer->size() - offset);
	offset = 0;
    }

    return NULL;
}
#endif
}  // namespace android
