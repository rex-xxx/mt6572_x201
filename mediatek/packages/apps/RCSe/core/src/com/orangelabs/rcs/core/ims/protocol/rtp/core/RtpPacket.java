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

package com.orangelabs.rcs.core.ims.protocol.rtp.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.orangelabs.rcs.core.ims.protocol.rtp.media.RtpExtensionHeader;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Packet;

/**
 * Abstract RTP packet
 * 
 * @author jexa7410
 */
public class RtpPacket extends Packet {
	public Packet base;
	public int marker;
	public int payloadType;
	public int seqnum;
	public long timestamp;
	public int ssrc;
	public int payloadoffset;
	public int payloadlength;
    /**
     * M: Added for share progress control @{
     */
    public static final int OFFSET_WITHOUT_EXTENSION = 12;
    public static final int OFFSET_WITH_EXTENSION = 22;
    private static final int FIRST_BYTE_WITH_EXTENSION = 144; // 10010000
    private static final int FIRST_BYTE_WITHOUT_EXTENSION = 128; // 10000000
    private static final int EXTENSION_HEADER_LENGTH = 2;
    public RtpExtensionHeader extensionHeader;

    /** @} */

	public RtpPacket() {
		super();
	}

	public RtpPacket(Packet packet) {
		super(packet);
		
		base = packet;
	}

	public void assemble(int length) throws IOException {
		this.length = length;
		this.offset = 0;

		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(length);
		DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
        /**
         * M: Added for share progress control @{
         */
        if (extensionHeader != null) {
            dataoutputstream.writeByte(FIRST_BYTE_WITH_EXTENSION);
        } else {
            dataoutputstream.writeByte(FIRST_BYTE_WITHOUT_EXTENSION);
        }
        /** @} */
		int i = payloadType;
		if (marker == 1) {
			i = payloadType | 0x80;
		}
		dataoutputstream.writeByte((byte) i);
		dataoutputstream.writeShort(seqnum);
		dataoutputstream.writeInt((int) timestamp);
		dataoutputstream.writeInt(ssrc);
        /**
         * M: Added for share progress control @{
         */
        if (extensionHeader != null) {
            dataoutputstream.writeShort(EXTENSION_HEADER_LENGTH);
            dataoutputstream.writeInt(extensionHeader.isStarted() ? 1 : 0);
            dataoutputstream.writeInt((int) extensionHeader.getDuration());
        }
        /** @} */
		dataoutputstream.write(base.data, payloadoffset, payloadlength);
		data = bytearrayoutputstream.toByteArray();
	}

    /**
     * M: Added for share progress control @{
     */
    public int calcLength() {
        if (extensionHeader == null) {
            return payloadlength + OFFSET_WITHOUT_EXTENSION;
        } else {
            return payloadlength + OFFSET_WITH_EXTENSION;
        }
    }
    /** @} */
}
