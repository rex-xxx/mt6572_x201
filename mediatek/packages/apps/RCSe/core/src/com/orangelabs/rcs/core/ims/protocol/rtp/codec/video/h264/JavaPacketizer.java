/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.VideoCodec;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Reassembles H264 RTP packets into H264 frames, as per RFC 3984
 *
 * @author Deutsche Telekom
 */
public class JavaPacketizer extends VideoCodec {

    /**
     * Enable/disable packetization mode 1
     */
    public static boolean H264_ENABLED_PACKETIZATION_MODE_1 = true;

    /**
     * Max frame size to H264
     */
    public static int H264_MAX_FRAME_SIZE = 1300; // TODO remove the rtp size...

    /**
     * Max number of packets to H264
     */
    public static int H264_MAX_RTP_PKTS = 32;

    /**
     * Buffer size for FU Indicator and Header
     */
    public static final int H264_FU_HEADER_SIZE = 2;

    /**
     * AVC NAL sequence parameter
     */
    public static final int AVC_NALTYPE_SPS = 7;

    /**
     * AVC NAL picture parameter
     */
    public static final int AVC_NALTYPE_PPS = 8;

    /**
     * Because packets can come out of order, it is possible that some packets
     * for a newer frame may arrive while an older frame is still incomplete.
     * However, in the case where we get nothing but incomplete frames, we don't
     * want to keep all of them around forever.
     */
    public JavaPacketizer() {
    }

    public int process(Buffer input, Buffer output) {

        if (input == null || output == null) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (!input.isDiscard()) {

            if (input.getLength() < H264_MAX_FRAME_SIZE || !H264_ENABLED_PACKETIZATION_MODE_1) {

                byte[] bufferData = (byte[]) input.getData();
                byte data[] = new byte[bufferData.length];

                System.arraycopy(bufferData, 0, data, 0, bufferData.length);

                if (data.length > 0) {
                    // Copy to buffer
                    output.setFormat(input.getFormat());
                    output.setData(data);
                    /**
                     * M: Added for share progress control @{
                     */
                    output.setExtensionHeader(input.getExtensionHeader());
                    /** @} */
                    output.setLength(data.length);
                    output.setOffset(0);
                    output.setTimeStamp(input.getTimeStamp());
                    output.setFlags(Buffer.FLAG_RTP_MARKER);
                }

                return BUFFER_PROCESSED_OK;
            }

            List<Buffer> outputs = new ArrayList<Buffer>();
            output.setFragments(outputs);

            byte[] bufferData = (byte[]) input.getData();

            // buffer for FU Indicator and Header
            byte[] h264FU = new byte[H264_FU_HEADER_SIZE];

            /*
             * First Header - The FU indicator octet has the following format:
             * +---------------+
             * |0|1|2|3|4|5|6|7|
             * +-+-+-+-+-+-+-+-+
             * |F|NRI|  Type   |
             * +---------------+
             */

            // FU Indicator pos = 0
            h264FU[0] = 0;
            h264FU[0] |= (bufferData[0] & 0xe0);// F | NRI
            h264FU[0] |= H264RtpHeaders.AVC_NALTYPE_FUA;

            /*
             * Second Header - The FU header has the following format:
             * +---------------+
             * |0|1|2|3|4|5|6|7|
             * +-+-+-+-+-+-+-+-+
             * |S|E|R|  Type   |
             * +---------------+
             */

            // FU Header pos = 1
            h264FU[1] = 0;
            h264FU[1] |= 0x80;// for the first pkt, the start bit is on
            // copy the original nal type from the stream
            h264FU[1] |= (bufferData[0] & 0x1f);

            // Split frame into pkts
            // for FU-A, we need to consume the first byte with the NAL header
            int startPosBufferData = 1;
            int available = bufferData.length - 1;// see comment above
            // define max size (not counting with the fuIndicator and fuHeader)
            int maxSize = H264_MAX_FRAME_SIZE - h264FU.length;
            int numberOfRtpPkts = 0;
            while (available > maxSize) {

                // >>>>>>>>>>>> create packet >>>>>>>>>>>>
                byte data[] = new byte[maxSize + h264FU.length];

                // write h264 payload
                System.arraycopy(h264FU, 0, data, 0, h264FU.length);

                // write frame data
                System.arraycopy(bufferData, startPosBufferData, data, h264FU.length, maxSize);

                if (data.length > 0) {
                    // copy to buffer
                    Buffer buffer = new Buffer();
                    buffer.setFormat(input.getFormat());
                    buffer.setData(data);
                    buffer.setLength(data.length);
                    buffer.setOffset(0);
                    buffer.setTimeStamp(input.getTimeStamp());

                    // add data buffer to outputs
                    outputs.add(buffer);

                    // increment number of rtp pkts
                    numberOfRtpPkts++;
                }
                // <<<<<<<<<<<< create packet <<<<<<<<<<<<

                // -1 to leave room for the last pkt
                if (numberOfRtpPkts >= H264_MAX_RTP_PKTS - 1) {
                    outputs.clear();
                    output.setData(null);
                    output.setDiscard(true);
                    return OUTPUT_BUFFER_NOT_FILLED;
                    // this frame is too big and needs to be split into more
                    // pkts than we can buffer
                }

                // reset the start bit
                // FU Header pos = 1
                // we need to switch the start bit off
                h264FU[1] &= 0x3f; // 0x7f

                // update variables
                startPosBufferData += maxSize;
                available -= maxSize;
            }

            // write the last chunk of the FU-A

            // set the end bit
            // FU Header pos = 1
            h264FU[1] |= 0x40;// we need to switch the end bit on

            // >>>>>>>>>>>> create packet >>>>>>>>>>>>
            byte data[] = new byte[available + h264FU.length];

            // write h264 payload
            System.arraycopy(h264FU, 0, data, 0, h264FU.length);

            // write frame data
            System.arraycopy(bufferData, startPosBufferData, data, h264FU.length, available);

            if (data.length > 0) {
                // copy to buffer
                Buffer buffer = new Buffer();
                buffer.setFormat(input.getFormat());
                buffer.setData(data);
                buffer.setLength(data.length);
                buffer.setOffset(0);
                buffer.setTimeStamp(input.getTimeStamp());
                buffer.setFlags(Buffer.FLAG_RTP_MARKER);

                // add data buffer to outputs
                outputs.add(buffer);
            }
            // <<<<<<<<<<<< create packet <<<<<<<<<<<<

            return BUFFER_PROCESSED_OK;
        } else {
            output.setDiscard(true);
            return OUTPUT_BUFFER_NOT_FILLED;
        }
    }
}
