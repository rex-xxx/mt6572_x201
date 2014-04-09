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

package com.mediatek.stereo3d;

import com.mediatek.common.stereo3d.IJpsParser;
import com.mediatek.xlog.Xlog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class JpsParser implements IJpsParser {
    private static final String TAG = "JpsParser";
    private static final int MARKER_SOI = 0;
    private static final int MARKER_APP3 = 3;
    private static final int MARKER_APPN = 16;
    private static final int HEADER_MIN_LENGTH = 14;
    private static final int HEADER_MAX_LENGTH = 40;

    // type
    public static final int MONOSCOPIC = 0;
    public static final int STEREOSCOPIC = 1;

    // display for monoscopic
    public static final int S3D_EYE_BOTH = 0;
    public static final int S3D_EYE_LEFT = 1;
    public static final int S3D_EYE_RIGHT = 2;

    // layout for stereoscopic
    public static final int S3D_LAYOUT_INTERLEAVED = 1;
    public static final int S3D_LAYOUT_SIDE_BY_SIDE = 2;
    public static final int S3D_LAYOUT_TOP_AND_BOTTOM = 3;
    public static final int S3D_LAYOUT_ANAGLYPH = 4;

    // misc flags
    public static final int S3D_FULL_HEIGHT = 0;
    public static final int S3D_HALF_HEIGHT = 1;
    public static final int S3D_FULL_WIDTH = 0;
    public static final int S3D_HALF_WIDTH = 1;
    public static final int S3D_RIGHT_FIELD_FIRST = 0;
    public static final int S3D_LEFT_FIELD_FIRST = 1;

    private int mType = -1;
    private int mDisplay = -1;
    private int mLayout = -1;
    private int mHeightType = -1;
    private int mWidthType = -1;
    private int mFieldOrder = -1;

    private JpsParser() {
        // private constructor;
    }

    /**
     * This method reads streams of bytes from an image file
     *
     * @param imgFile the name of the image file
     * @return the byte array
     * @hide
     */
    private byte[] readBytesFromFileHeader(File imgFile) {
        FileInputStream in = null;
        byte[] imgBytes = null;

        try {
            in = new FileInputStream(imgFile);

            if (in != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buffer = new byte[(int)imgFile.length()];

                try {
                    int readBytes = 0;
                    if ((readBytes = in.read(buffer)) != -1) {
                        bout.write(buffer, 0, readBytes);  // write to ByteArrayOutputStream
                    }
                    imgBytes = bout.toByteArray(); // convert to image byte array
                    bout.close();
                } catch (IOException ex) {
                    Xlog.e(TAG, "IOException: " + ex.toString());
                }

                in.close();
            }
        } catch (FileNotFoundException ex) {
            Xlog.e(TAG, "FileNotFoundException: " + ex.toString());
        } catch (IOException ex) {
            Xlog.e(TAG, "IOException: " + ex.toString());
        }
        return imgBytes;
    }

    /**
     * This method extracts the JPS file header metadata
     *
     * @param file the image file
     * @return the FileMetadata object that contains the metadata
     * @hide
     */
    public static JpsParser parse(File file) {
        boolean foundApp3 = false;
        int offset = 0;

        JpsParser parser = new JpsParser();

        // read image into byte array
        byte[] imgBytes = parser.readBytesFromFileHeader(file);

        if (imgBytes != null) {
            Xlog.i(TAG, "Parse JPS header with image size: " + imgBytes.length);

            MarkerPair pair = new MarkerPair();

            // find SOI APPn first
            while (offset < imgBytes.length) {
                parser.findStartOfImageMarker(imgBytes, offset, pair);

                if (pair.getMarker() == MARKER_APP3) {
                    // +6 to skip SOI, APP3, and the length info bytes
                    parser.processStereoscopicDescriptor(imgBytes, pair.getOffset() + 6);
                    foundApp3 = true;
                    break;
                } else if (pair.getMarker() == MARKER_APPN) {
                    // +4 to skip SOI, and APPn bytes
                    int length = parser.getBlockLength(imgBytes, pair.getOffset() + 4);
                    offset = pair.getOffset() + 4 + length;
                    break;
                }

                offset += 4;

                if (offset > HEADER_MAX_LENGTH) {
                    Xlog.i(TAG, "Break marker searching");
                    break;
                }
            }

            // if SOI APP3 is not found, try to find SOI APPn...APP3
            if (!foundApp3) {
                while (offset < imgBytes.length) {
                    if (parser.findMarker(imgBytes, offset, MARKER_APP3)) {
                        // +4 to skip APP3, and the length info bytes
                        parser.processStereoscopicDescriptor(imgBytes, offset + 4);
                        break;
                    }

                    offset += 2;

                    if (offset > HEADER_MAX_LENGTH + 10) { // search extra 10 bytes
                        Xlog.i(TAG, "Break marker APP3 searching");
                        break;
                    }
                }
            }
        }

        return parser;
    }

    /**
     * This method extracts the first appeared marker, e.g. SOI APP1
     *
     * @param fileBytes the byte array
     * @param startOffset the starting position to search for the marker
     * @param pair the storage for the result
     */
    private void findStartOfImageMarker(byte[] fileBytes, int startOffset, MarkerPair pair) {
        int offset = startOffset;

        while (offset < fileBytes.length) {
            if (findMarker(fileBytes, offset, MARKER_SOI)) {
                if (findMarker(fileBytes, offset + 2, MARKER_APP3)) {
                    pair.setMarker(MARKER_APP3);
                    pair.setOffset(offset);
                    break;
                } else if (findMarker(fileBytes, offset + 2, MARKER_APPN)) {
                    pair.setMarker(MARKER_APPN);
                    pair.setOffset(offset);
                    break;
                }
            }

            offset += 2;
        }
    }

    /**
     * This method gets the length of a block for the marker in the specified offset
     *
     * @param fileBytes the byte array
     * @param offset the position to extract the length information
     * @return the length of a block
     */
    private int getBlockLength(byte[] fileBytes, int offset) {
        // read two bytes into integer
        return ((int)(fileBytes[offset] & 0xFF) * 256) + (int)(fileBytes[offset + 1] & 0xFF);
    }

    /**
     * This method processes the stereoscopic descriptor embedded in the JPS file header
     *
     * @param fileBytes the byte array of the file
     * @param offset    the starting position to search for the stereoscopic descriptor
     * @return the FileMetadata object that contains the metadata
     * @hide
     */
    private void processStereoscopicDescriptor(byte[] fileBytes, int pos) {
        int offset = pos;

        if (offset + HEADER_MIN_LENGTH <= fileBytes.length) {
            if ((fileBytes[offset] == (byte) 0x5F) && (fileBytes[offset + 1] == (byte) 0x4A)
                    && (fileBytes[offset + 2] == (byte) 0x50) && (fileBytes[offset + 3] == (byte) 0x53)
                    && (fileBytes[offset + 4] == (byte) 0x4A) && (fileBytes[offset + 5] == (byte) 0x50)
                    && (fileBytes[offset + 6] == (byte) 0x53) && (fileBytes[offset + 7] == (byte) 0x5F)) {
                // found _JPSJPS_ identifier for a stereoscopic descriptor
                offset += 8;
                offset += 2; // skip 2 bytes value that determines the length of a block

                setType(fileBytes[offset++]);

                if (mType == MONOSCOPIC) {
                    setDisplay(fileBytes[offset++]);
                } else if (mType == STEREOSCOPIC) {
                    setLayout(fileBytes[offset++]);
                }

                setMiscFlags(fileBytes[offset]);
            }
        }
    }

    /**
     * This method reads two bytes at once and determines the type of the marker
     *
     * @param fileBytes the byte array
     * @param offset the starting position to search for the marker
     * @param type the type of the marker
     * @return true if the specified marker is found, false, otherwise.
     * @hide
     */
    private boolean findMarker(byte[] fileBytes, int offset, int type) {
        boolean result = false;

        switch (type) {
        case MARKER_SOI:
            result = ((fileBytes[offset] == (byte) 0xFF) && (fileBytes[offset + 1] == (byte) 0xD8));
            break;
        case MARKER_APP3:
            result = ((fileBytes[offset] == (byte) 0xFF) && (fileBytes[offset + 1] == (byte) 0xE3));
            break;
        case MARKER_APPN:
            result = ((fileBytes[offset] == (byte) 0xFF) && (fileBytes[offset + 1] >= (byte) 0xE1)
                      && (fileBytes[offset + 1] <= (byte) 0xEF));
            break;
        default:
            break;
        }

        return result;
    }

    /**
     * The method sets the image type according to the JPEG's header
     *
     * @param b the byte value
     * @hide
     */
    private void setType(byte b) {
        if (b == (byte) 0x00) {
            mType = MONOSCOPIC;
        } else if (b == (byte) 0x01) {
            mType = STEREOSCOPIC;
        }
    }

    /**
     * The method sets the layout type according to the JPEG's header
     * Layout is only for stereoscopic
     *
     * @param b the byte value
     * @hide
     */
    private void setLayout(byte b) {
        if (b == (byte) 0x01) {
            mLayout = S3D_LAYOUT_INTERLEAVED;
        } else if (b == (byte) 0x02) {
            mLayout = S3D_LAYOUT_SIDE_BY_SIDE;
        } else if (b == (byte) 0x03) {
            mLayout = S3D_LAYOUT_TOP_AND_BOTTOM;
        } else if (b == (byte) 0x04) {
            mLayout = S3D_LAYOUT_ANAGLYPH;
        }
    }

    /**
     * The method sets the display type according to the JPEG's header
     * Display is only for monoscopic
     *
     * @param b the byte value
     * @hide
     */
    private void setDisplay(byte b) {
        if (b == (byte) 0x00) {
            mDisplay = S3D_EYE_BOTH;
        } else if (b == (byte) 0x01) {
            mDisplay = S3D_EYE_LEFT;
        } else if (b == (byte) 0x02) {
            mDisplay = S3D_EYE_RIGHT;
        }
    }

    /**
     * The method sets the image dimension type according to the JPEG's header
     *
     * @param b the byte value
     * @hide
     */
    private void setMiscFlags(byte b) {
        // height bit
        int bit = 1 & b >> 0;
        if (bit == 0) {
            mHeightType = S3D_FULL_HEIGHT;
        } else {
            mHeightType = S3D_HALF_HEIGHT;
        }

        // width bit
        bit = 1 & b >> 1;
        if (bit == 0) {
            mWidthType = S3D_FULL_WIDTH;
        } else {
            mWidthType = S3D_HALF_WIDTH;
        }

        // field order bit
        bit = 1 & b >> 2;
        if (bit == 0) {
            mFieldOrder = S3D_RIGHT_FIELD_FIRST;
        } else {
            mFieldOrder = S3D_LEFT_FIELD_FIRST;
        }
    }

    /**
     * The method retrieves the image type
     *
     * @return the image type
     * @hide
     */
    public int getType() {
        return mType;
    }

    /**
     * The method retrieves the image layout
     * Layout is only for stereoscopic
     *
     * @return the image layout
     * @hide
     */
    public int getLayout() {
        return mLayout;
    }

    /**
     * The method retrieves the display type of the image
     * Display is only for monoscopic
     *
     * @return the image display
     * @hide
     */
    public int getDisplay() {
        return mDisplay;
    }

    /**
     * The method retrieves the height type of the image
     *
     * @return the height type
     * @hide
     */
    public int getHeightType() {
        return mHeightType;
    }

    /**
     * The method retrieves the width type of the image
     *
     * @return the width type
     * @hide
     */
    public int getWidthType() {
        return mWidthType;
    }

    /**
     * The method retrieves the field order of the image
     *
     * @return the field order
     * @hide
     */
    public int getFieldOrder() {
        return mFieldOrder;
    }
}

class MarkerPair {
    private int mMarker;
    private int mOffset;

    MarkerPair() {
        mMarker = -1;
        mOffset = -1;
    }

    /**
     * This method sets the marker type
     *
     * @param type the type of the marker
     * @hide
     */
    protected void setMarker(int type) {
        mMarker = type;
    }

    /**
     * This method sets the position where the marker is located
     *
     * @param offset the position of the marker
     * @hide
     */
    protected void setOffset(int offset) {
        mOffset = offset;
    }

    /**
     * This method gets the type of the marker
     *
     * @return the type of the marker
     * @hide
     */
    protected int getMarker() {
        return mMarker;
    }

    /**
     * This method gets the position of the marker
     *
     * @return the position of the marker
     * @hide
     */
    protected int getOffset() {
        return mOffset;
    }
}