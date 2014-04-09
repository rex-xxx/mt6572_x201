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

package com.mediatek.vt;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class CameraParamters {

    private static final String TAG = "VT Camera Parameters";
    // Parameter keys to communicate with the camera driver.
    private static final String KEY_PREVIEW_SIZE = "preview-size";
    private static final String KEY_PREVIEW_FORMAT = "preview-format";
    private static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
    private static final String KEY_PICTURE_SIZE = "picture-size";
    private static final String KEY_PICTURE_FORMAT = "picture-format";
    private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
    private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
    private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
    private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
    private static final String KEY_JPEG_QUALITY = "jpeg-quality";
    private static final String KEY_ROTATION = "rotation";
    private static final String KEY_GPS_LATITUDE = "gps-latitude";
    private static final String KEY_GPS_LONGITUDE = "gps-longitude";
    private static final String KEY_GPS_ALTITUDE = "gps-altitude";
    private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
    private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
    private static final String KEY_WHITE_BALANCE = "whitebalance";
    private static final String KEY_EFFECT = "effect";
    private static final String KEY_ANTIBANDING = "antibanding";
    private static final String KEY_SCENE_MODE = "scene-mode";
    private static final String KEY_FLASH_MODE = "flash-mode";
    private static final String KEY_FOCUS_MODE = "focus-mode";
    private static final String KEY_FOCAL_LENGTH = "focal-length";
    private static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
    private static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
    private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
    private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
    private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
    private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
    private static final String KEY_ZOOM = "zoom";
    private static final String KEY_MAX_ZOOM = "max-zoom";
    private static final String KEY_ZOOM_RATIOS = "zoom-ratios";
    private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
    private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
    private static final String KEY_FOCUS_METER = "focus-meter";
    private static final String KEY_ISOSPEED_MODE = "iso-speed";
    private static final String KEY_EXPOSURE = "exposure";
    private static final String KEY_EXPOSURE_METER = "exposure-meter";
    private static final String KEY_FD_MODE = "fd-mode";
    private static final String KEY_EDGE_MODE = "edge";
    private static final String KEY_HUE_MODE = "hue";
    private static final String KEY_SATURATION_MODE = "saturation";
    private static final String KEY_BRIGHTNESS_MODE = "brightness";
    private static final String KEY_CONTRAST_MODE = "contrast";
    private static final String KEY_CAPTURE_MODE = "cap-mode";
    private static final String KEY_CAPTURE_PATH = "capfname";
    private static final String KEY_BURST_SHOT_NUM = "burst-num";

    // Parameter key suffix for supported values.
    private static final String SUPPORTED_VALUES_SUFFIX = "-values";

    private static final String TRUE = "true";

    // Values for white balance settings.
    public static final String WHITE_BALANCE_AUTO = "auto";
    public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
    public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
    public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
    public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
    public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
    public static final String WHITE_BALANCE_TWILIGHT = "twilight";
    public static final String WHITE_BALANCE_SHADE = "shade";

    // Values for color effect settings.
    public static final String EFFECT_NONE = "none";
    public static final String EFFECT_MONO = "mono";
    public static final String EFFECT_NEGATIVE = "negative";
    public static final String EFFECT_SOLARIZE = "solarize";
    public static final String EFFECT_SEPIA = "sepia";
    public static final String EFFECT_POSTERIZE = "posterize";
    public static final String EFFECT_WHITEBOARD = "whiteboard";
    public static final String EFFECT_BLACKBOARD = "blackboard";
    public static final String EFFECT_AQUA = "aqua";

    // Values for antibanding settings.
    public static final String ANTIBANDING_AUTO = "auto";
    public static final String ANTIBANDING_50HZ = "50hz";
    public static final String ANTIBANDING_60HZ = "60hz";
    public static final String ANTIBANDING_OFF = "off";

    public static final String CONTRAST_HIGH = "high";
    public static final String CONTRAST_MIDDLE = "middle";
    public static final String CONTRAST_LOW = "low";

    // Values for flash mode settings.
    /**
     * Flash will not be fired.
     */
    public static final String FLASH_MODE_OFF = "off";

    /**
     * Flash will be fired automatically when required. The flash may be fired
     * during preview, auto-focus, or snapshot depending on the driver.
     */
    public static final String FLASH_MODE_AUTO = "auto";

    /**
     * Flash will always be fired during snapshot. The flash may also be fired
     * during preview or auto-focus depending on the driver.
     */
    public static final String FLASH_MODE_ON = "on";

    /**
     * Flash will be fired in red-eye reduction mode.
     */
    public static final String FLASH_MODE_RED_EYE = "red-eye";

    /**
     * Constant emission of light during preview, auto-focus and snapshot. This
     * can also be used for video recording.
     */
    public static final String FLASH_MODE_TORCH = "torch";

    // Values for scene mode settings.
    public static final String SCENE_MODE_AUTO = "auto";
    public static final String SCENE_MODE_ACTION = "action";
    public static final String SCENE_MODE_PORTRAIT = "portrait";
    public static final String SCENE_MODE_LANDSCAPE = "landscape";
    public static final String SCENE_MODE_NIGHT = "night";
    public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
    public static final String SCENE_MODE_THEATRE = "theatre";
    public static final String SCENE_MODE_BEACH = "beach";
    public static final String SCENE_MODE_SNOW = "snow";
    public static final String SCENE_MODE_SUNSET = "sunset";
    public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
    public static final String SCENE_MODE_FIREWORKS = "fireworks";
    public static final String SCENE_MODE_SPORTS = "sports";
    public static final String SCENE_MODE_PARTY = "party";
    public static final String SCENE_MODE_CANDLELIGHT = "candlelight";

    /**
     * Applications are looking for a barcode. Camera driver will be optimized
     * for barcode reading.
     */
    public static final String SCENE_MODE_BARCODE = "barcode";

    // Values for focus mode settings.
    /**
     * Auto-focus mode.
     */
    public static final String FOCUS_MODE_AUTO = "auto";

    /**
     * Focus is set at infinity. Applications should not call
     * {@link #autoFocus(AutoFocusCallback)} in this mode.
     */
    public static final String FOCUS_MODE_INFINITY = "infinity";
    public static final String FOCUS_MODE_MACRO = "macro";

    /**
     * Focus is fixed. The camera is always in this mode if the focus is not
     * adjustable. If the camera has auto-focus, this mode can fix the focus,
     * which is usually at hyperfocal distance. Applications should not call
     * {@link #autoFocus(AutoFocusCallback)} in this mode.
     */
    public static final String FOCUS_MODE_FIXED = "fixed";

    /**
     * Extended depth of field (EDOF). Focusing is done digitally and
     * continuously. Applications should not call
     * {@link #autoFocus(AutoFocusCallback)} in this mode.
     */
    public static final String FOCUS_MODE_EDOF = "edof";

    // Values for capture mode settings.
    public static final String CAPTURE_MODE_NORMAL = "normal";
    public static final String CAPTURE_MODE_BEST_SHOT = "bestshot";
    public static final String CAPTURE_MODE_EV_BRACKET_SHOT = "evbracketshot";
    public static final String CAPTURE_MODE_BURST_SHOT = "burstshot";

    // Formats for setPreviewFormat and setPictureFormat.
    private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
    private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
    private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
    private static final String PIXEL_FORMAT_RGB565 = "rgb565";
    private static final String PIXEL_FORMAT_JPEG = "jpeg";

    private HashMap<String, String> mMap;

    /**
     * Handles the picture size (dimensions).
     */
    public static class Size {
        /**
         * Sets the dimensions for pictures.
         * 
         * @param w the photo width (pixels)
         * @param h the photo height (pixels)
         */
        public Size(int w, int h) {
            width = w;
            height = h;
        }

        /**
         * Compares {@code obj} to this size.
         * 
         * @param obj the object to compare this size with.
         * @return {@code true} if the width and height of {@code obj} is the
         *         same as those of this size. {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            return width == s.width && height == s.height;
        }

        @Override
        public int hashCode() {
            return width * 32713 + height;
        }

        /** width of the picture */
        public int width;
        /** height of the picture */
        public int height;
    };

    public CameraParamters() {
        mMap = new HashMap<String, String>();
    }

    /**
     * Writes the current Parameters to the log.
     * 
     * @hide
     * 
     */
    public void dump() {
        Log.e(TAG, "dump: size=" + mMap.size());
        for (String k : mMap.keySet()) {
            Log.e(TAG, "dump: " + k + "=" + mMap.get(k));
        }
    }

    /**
     * Creates a single string with all the parameters set in this Parameters
     * object.
     * <p>
     * The {@link #unflatten(String)} method does the reverse.
     * </p>
     * 
     * @return a String with all values from this Parameters object, in
     *         semi-colon delimited key-value pairs
     */
    public String flatten() {
        StringBuilder flattened = new StringBuilder();
        for (String k : mMap.keySet()) {
            flattened.append(k);
            flattened.append("=");
            flattened.append(mMap.get(k));
            flattened.append(";");
        }
        // chop off the extra semicolon at the end
        flattened.deleteCharAt(flattened.length() - 1);
        return flattened.toString();
    }

    /**
     * Takes a flattened string of parameters and adds each one to this
     * Parameters object.
     * <p>
     * The {@link #flatten()} method does the reverse.
     * </p>
     * 
     * @param flattened a String of parameters (key-value paired) that are
     *            semi-colon delimited
     */
    public void unflatten(String flattened) {
        mMap.clear();

        StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
        while (tokenizer.hasMoreElements()) {
            String kv = tokenizer.nextToken();
            int pos = kv.indexOf('=');
            if (pos == -1) {
                continue;
            }
            String k = kv.substring(0, pos);
            String v = kv.substring(pos + 1);
            mMap.put(k, v);
        }
    }

    public void remove(String key) {
        mMap.remove(key);
    }

    /**
     * Sets a String parameter.
     * 
     * @param key the key name for the parameter
     * @param value the String value of the parameter
     */
    public void set(String key, String value) {
        if (key.indexOf('=') != -1 || key.indexOf(';') != -1) {
            Log.e(TAG, "Key \"" + key + "\" contains invalid character (= or ;)");
            return;
        }
        if (value.indexOf('=') != -1 || value.indexOf(';') != -1) {
            Log.e(TAG, "Value \"" + value + "\" contains invalid character (= or ;)");
            return;
        }

        mMap.put(key, value);
    }

    /**
     * Sets an integer parameter.
     * 
     * @param key the key name for the parameter
     * @param value the int value of the parameter
     */
    public void set(String key, int value) {
        mMap.put(key, Integer.toString(value));
    }

    /**
     * Returns the value of a String parameter.
     * 
     * @param key the key name for the parameter
     * @return the String value of the parameter
     */
    public String get(String key) {
        return mMap.get(key);
    }

    /**
     * Returns the value of an integer parameter.
     * 
     * @param key the key name for the parameter
     * @return the int value of the parameter
     */
    public int getInt(String key) {
        return Integer.parseInt(mMap.get(key));
    }

    /**
     * Sets the dimensions for preview pictures.
     * 
     * @param width the width of the pictures, in pixels
     * @param height the height of the pictures, in pixels
     */
    public void setPreviewSize(int width, int height) {
        String v = Integer.toString(width) + "x" + Integer.toString(height);
        set(KEY_PREVIEW_SIZE, v);
    }

    /**
     * Returns the dimensions setting for preview pictures.
     * 
     * @return a Size object with the height and width setting for the preview
     *         picture
     */
    public Size getPreviewSize() {
        String pair = get(KEY_PREVIEW_SIZE);
        return strToSize(pair);
    }

    /**
     * Gets the supported preview sizes.
     * 
     * @return a list of Size object. This method will always return a list with
     *         at least one element.
     */
    public List<Size> getSupportedPreviewSizes() {
        String str = get(KEY_PREVIEW_SIZE + SUPPORTED_VALUES_SUFFIX);
        return splitSize(str);
    }

    /**
     * Sets the dimensions for EXIF thumbnail in Jpeg picture. If applications
     * set both width and height to 0, EXIF will not contain thumbnail.
     * 
     * @param width the width of the thumbnail, in pixels
     * @param height the height of the thumbnail, in pixels
     */
    public void setJpegThumbnailSize(int width, int height) {
        set(KEY_JPEG_THUMBNAIL_WIDTH, width);
        set(KEY_JPEG_THUMBNAIL_HEIGHT, height);
    }

    /**
     * Returns the dimensions for EXIF thumbnail in Jpeg picture.
     * 
     * @return a Size object with the height and width setting for the EXIF
     *         thumbnails
     */
    public Size getJpegThumbnailSize() {
        return new Size(getInt(KEY_JPEG_THUMBNAIL_WIDTH),
                        getInt(KEY_JPEG_THUMBNAIL_HEIGHT));
    }

    /**
     * Gets the supported jpeg thumbnail sizes.
     * 
     * @return a list of Size object. This method will always return a list with
     *         at least two elements. Size 0,0 (no thumbnail) is always
     *         supported.
     */
    public List<Size> getSupportedJpegThumbnailSizes() {
        String str = get(KEY_JPEG_THUMBNAIL_SIZE + SUPPORTED_VALUES_SUFFIX);
        return splitSize(str);
    }

    /**
     * Sets the quality of the EXIF thumbnail in Jpeg picture.
     * 
     * @param quality the JPEG quality of the EXIF thumbnail. The range is 1 to
     *            100, with 100 being the best.
     */
    public void setJpegThumbnailQuality(int quality) {
        set(KEY_JPEG_THUMBNAIL_QUALITY, quality);
    }

    /**
     * Returns the quality setting for the EXIF thumbnail in Jpeg picture.
     * 
     * @return the JPEG quality setting of the EXIF thumbnail.
     */
    public int getJpegThumbnailQuality() {
        return getInt(KEY_JPEG_THUMBNAIL_QUALITY);
    }

    /**
     * Sets Jpeg quality of captured picture.
     * 
     * @param quality the JPEG quality of captured picture. The range is 1 to
     *            100, with 100 being the best.
     */
    public void setJpegQuality(int quality) {
        set(KEY_JPEG_QUALITY, quality);
    }

    /**
     * Returns the quality setting for the JPEG picture.
     * 
     * @return the JPEG picture quality setting.
     */
    public int getJpegQuality() {
        return getInt(KEY_JPEG_QUALITY);
    }

    /**
     * Sets the rate at which preview frames are received. This is the target
     * frame rate. The actual frame rate depends on the driver.
     * 
     * @param fps the frame rate (frames per second)
     */
    public void setPreviewFrameRate(int fps) {
        set(KEY_PREVIEW_FRAME_RATE, fps);
    }

    /**
     * Returns the setting for the rate at which preview frames are received.
     * This is the target frame rate. The actual frame rate depends on the
     * driver.
     * 
     * @return the frame rate setting (frames per second)
     */
    public int getPreviewFrameRate() {
        return getInt(KEY_PREVIEW_FRAME_RATE);
    }

    /**
     * Gets the supported preview frame rates.
     * 
     * @return a list of supported preview frame rates. null if preview frame
     *         rate setting is not supported.
     */
    public List<Integer> getSupportedPreviewFrameRates() {
        String str = get(KEY_PREVIEW_FRAME_RATE + SUPPORTED_VALUES_SUFFIX);
        return splitInt(str);
    }

    /**
     * Sets the image format for preview pictures.
     * <p>
     * If this is never called, the default format will be
     * {@link android.graphics.ImageFormat#NV21}, which uses the NV21 encoding
     * format.
     * </p>
     * 
     * @param pixel_format the desired preview picture format, defined by one of
     *            the {@link android.graphics.ImageFormat} constants. (E.g.,
     *            <var>ImageFormat.NV21</var> (default),
     *            <var>ImageFormat.RGB_565</var>, or
     *            <var>ImageFormat.JPEG</var>)
     * @see android.graphics.ImageFormat
     */
    // public void setPreviewFormat(int pixel_format) {
    // String s = cameraFormatForPixelFormat(pixel_format);
    // if (s == null) {
    // throw new IllegalArgumentException(
    // "Invalid pixel_format=" + pixel_format);
    // }
    //
    // set(KEY_PREVIEW_FORMAT, s);
    // }
    //
    // /**
    // * Returns the image format for preview frames got from
    // * {@link PreviewCallback}.
    // *
    // * @return the preview format.
    // * @see android.graphics.ImageFormat
    // */
    // public int getPreviewFormat() {
    // return pixelFormatForCameraFormat(get(KEY_PREVIEW_FORMAT));
    // }

    /**
     * Gets the supported preview formats.
     * 
     * @return a list of supported preview formats. This method will always
     *         return a list with at least one element.
     * @see android.graphics.ImageFormat
     */
    // public List<Integer> getSupportedPreviewFormats() {
    // String str = get(KEY_PREVIEW_FORMAT + SUPPORTED_VALUES_SUFFIX);
    // ArrayList<Integer> formats = new ArrayList<Integer>();
    // for (String s : split(str)) {
    // int f = pixelFormatForCameraFormat(s);
    // if (f == ImageFormat.UNKNOWN) continue;
    // formats.add(f);
    // }
    // return formats;
    // }

    /**
     * Sets the dimensions for pictures.
     * 
     * @param width the width for pictures, in pixels
     * @param height the height for pictures, in pixels
     */
    public void setPictureSize(int width, int height) {
        String v = Integer.toString(width) + "x" + Integer.toString(height);
        set(KEY_PICTURE_SIZE, v);
    }

    /**
     * Returns the dimension setting for pictures.
     * 
     * @return a Size object with the height and width setting for pictures
     */
    public Size getPictureSize() {
        String pair = get(KEY_PICTURE_SIZE);
        return strToSize(pair);
    }

    /**
     * Gets the supported picture sizes.
     * 
     * @return a list of supported picture sizes. This method will always return
     *         a list with at least one element.
     */
    // public List<Size> getSupportedPictureSizes() {
    // String str = get(KEY_PICTURE_SIZE + SUPPORTED_VALUES_SUFFIX);
    // return splitSize(str);
    // }
    //
    // /**
    // * Sets the image format for pictures.
    // *
    // * @param pixel_format the desired picture format
    // * (<var>ImageFormat.NV21</var>,
    // * <var>ImageFormat.RGB_565</var>, or
    // * <var>ImageFormat.JPEG</var>)
    // * @see android.graphics.ImageFormat
    // */
    // public void setPictureFormat(int pixel_format) {
    // String s = cameraFormatForPixelFormat(pixel_format);
    // if (s == null) {
    // throw new IllegalArgumentException(
    // "Invalid pixel_format=" + pixel_format);
    // }
    //
    // set(KEY_PICTURE_FORMAT, s);
    // }

    /**
     * Returns the image format for pictures.
     * 
     * @return the picture format
     * @see android.graphics.ImageFormat
     */
    // public int getPictureFormat() {
    // return pixelFormatForCameraFormat(get(KEY_PICTURE_FORMAT));
    // }
    //
    // /**
    // * Gets the supported picture formats.
    // *
    // * @return supported picture formats. This method will always return a
    // * list with at least one element.
    // * @see android.graphics.ImageFormat
    // */
    // public List<Integer> getSupportedPictureFormats() {
    // String str = get(KEY_PICTURE_FORMAT + SUPPORTED_VALUES_SUFFIX);
    // ArrayList<Integer> formats = new ArrayList<Integer>();
    // for (String s : split(str)) {
    // int f = pixelFormatForCameraFormat(s);
    // if (f == ImageFormat.UNKNOWN) continue;
    // formats.add(f);
    // }
    // return formats;
    // }

    // private String cameraFormatForPixelFormat(int pixel_format) {
    // switch(pixel_format) {
    // case ImageFormat.NV16: return PIXEL_FORMAT_YUV422SP;
    // case ImageFormat.NV21: return PIXEL_FORMAT_YUV420SP;
    // case ImageFormat.YUY2: return PIXEL_FORMAT_YUV422I;
    // case ImageFormat.RGB_565: return PIXEL_FORMAT_RGB565;
    // case ImageFormat.JPEG: return PIXEL_FORMAT_JPEG;
    // default: return null;
    // }
    // }

    // private int pixelFormatForCameraFormat(String format) {
    // if (format == null)
    // return ImageFormat.UNKNOWN;
    //
    // if (format.equals(PIXEL_FORMAT_YUV422SP))
    // return ImageFormat.NV16;
    //
    // if (format.equals(PIXEL_FORMAT_YUV420SP))
    // return ImageFormat.NV21;
    //
    // if (format.equals(PIXEL_FORMAT_YUV422I))
    // return ImageFormat.YUY2;
    //
    // if (format.equals(PIXEL_FORMAT_RGB565))
    // return ImageFormat.RGB_565;
    //
    // if (format.equals(PIXEL_FORMAT_JPEG))
    // return ImageFormat.JPEG;
    //
    // return ImageFormat.UNKNOWN;
    // }

    /**
     * Sets the orientation of the device in degrees. For example, suppose the
     * natural position of the device is landscape. If the user takes a picture
     * in landscape mode in 2048x1536 resolution, the rotation should be set to
     * 0. If the user rotates the phone 90 degrees clockwise, the rotation
     * should be set to 90. Applications can use
     * {@link android.view.OrientationEventListener} to set this parameter. The
     * camera driver may set orientation in the EXIF header without rotating the
     * picture. Or the driver may rotate the picture and the EXIF thumbnail. If
     * the Jpeg picture is rotated, the orientation in the EXIF header will be
     * missing or 1 (row #0 is top and column #0 is left side).
     * 
     * @param rotation The orientation of the device in degrees. Rotation can
     *            only be 0, 90, 180 or 270.
     * @throws IllegalArgumentException if rotation value is invalid.
     * @see android.view.OrientationEventListener
     */
    public void setRotation(int rotation) {
        if (rotation == 0 || rotation == 90 || rotation == 180
                || rotation == 270) {
            set(KEY_ROTATION, Integer.toString(rotation));
        } else {
            throw new IllegalArgumentException(
                    "Invalid rotation=" + rotation);
        }
    }

    /**
     * Sets GPS latitude coordinate. This will be stored in JPEG EXIF header.
     * 
     * @param latitude GPS latitude coordinate.
     */
    public void setGpsLatitude(double latitude) {
        set(KEY_GPS_LATITUDE, Double.toString(latitude));
    }

    /**
     * Sets GPS longitude coordinate. This will be stored in JPEG EXIF header.
     * 
     * @param longitude GPS longitude coordinate.
     */
    public void setGpsLongitude(double longitude) {
        set(KEY_GPS_LONGITUDE, Double.toString(longitude));
    }

    /**
     * Sets GPS altitude. This will be stored in JPEG EXIF header.
     * 
     * @param altitude GPS altitude in meters.
     */
    public void setGpsAltitude(double altitude) {
        set(KEY_GPS_ALTITUDE, Double.toString(altitude));
    }

    /**
     * Sets GPS timestamp. This will be stored in JPEG EXIF header.
     * 
     * @param timestamp GPS timestamp (UTC in seconds since January 1, 1970).
     */
    public void setGpsTimestamp(long timestamp) {
        set(KEY_GPS_TIMESTAMP, Long.toString(timestamp));
    }

    /**
     * Sets GPS processing method. It will store up to 32 characters in JPEG
     * EXIF header.
     * 
     * @param processing_method The processing method to get this location.
     */
    public void setGpsProcessingMethod(String processingMethod) {
        set(KEY_GPS_PROCESSING_METHOD, processingMethod);
    }

    /**
     * Removes GPS latitude, longitude, altitude, and timestamp from the
     * parameters.
     */
    public void removeGpsData() {
        remove(KEY_GPS_LATITUDE);
        remove(KEY_GPS_LONGITUDE);
        remove(KEY_GPS_ALTITUDE);
        remove(KEY_GPS_TIMESTAMP);
        remove(KEY_GPS_PROCESSING_METHOD);
    }

    /**
     * Gets the current white balance setting.
     * 
     * @return current white balance. null if white balance setting is not
     *         supported.
     * @see #WHITE_BALANCE_AUTO
     * @see #WHITE_BALANCE_INCANDESCENT
     * @see #WHITE_BALANCE_FLUORESCENT
     * @see #WHITE_BALANCE_WARM_FLUORESCENT
     * @see #WHITE_BALANCE_DAYLIGHT
     * @see #WHITE_BALANCE_CLOUDY_DAYLIGHT
     * @see #WHITE_BALANCE_TWILIGHT
     * @see #WHITE_BALANCE_SHADE
     */
    public String getWhiteBalance() {
        return get(KEY_WHITE_BALANCE);
    }

    /**
     * Sets the white balance.
     * 
     * @param value new white balance.
     * @see #getWhiteBalance()
     */
    public void setWhiteBalance(String value) {
        set(KEY_WHITE_BALANCE, value);
    }

    /**
     * Gets the supported white balance.
     * 
     * @return a list of supported white balance. null if white balance setting
     *         is not supported.
     * @see #getWhiteBalance()
     */
    public List<String> getSupportedWhiteBalance() {
        String str = get(KEY_WHITE_BALANCE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * Gets the current color effect setting.
     * 
     * @return current color effect. null if color effect setting is not
     *         supported.
     * @see #EFFECT_NONE
     * @see #EFFECT_MONO
     * @see #EFFECT_NEGATIVE
     * @see #EFFECT_SOLARIZE
     * @see #EFFECT_SEPIA
     * @see #EFFECT_POSTERIZE
     * @see #EFFECT_WHITEBOARD
     * @see #EFFECT_BLACKBOARD
     * @see #EFFECT_AQUA
     */
    public String getColorEffect() {
        return get(KEY_EFFECT);
    }

    /**
     * Sets the current color effect setting.
     * 
     * @param value new color effect.
     * @see #getColorEffect()
     */
    public void setColorEffect(String value) {
        set(KEY_EFFECT, value);
    }

    /**
     * Gets the supported color effects.
     * 
     * @return a list of supported color effects. null if color effect setting
     *         is not supported.
     * @see #getColorEffect()
     */
    public List<String> getSupportedColorEffects() {
        String str = get(KEY_EFFECT + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * Gets the current antibanding setting.
     * 
     * @return current antibanding. null if antibanding setting is not
     *         supported.
     * @see #ANTIBANDING_AUTO
     * @see #ANTIBANDING_50HZ
     * @see #ANTIBANDING_60HZ
     * @see #ANTIBANDING_OFF
     */
    public String getAntibanding() {
        return get(KEY_ANTIBANDING);
    }

    /**
     * Sets the antibanding.
     * 
     * @param antibanding new antibanding value.
     * @see #getAntibanding()
     */
    public void setAntibanding(String antibanding) {
        set(KEY_ANTIBANDING, antibanding);
    }

    /**
     * Gets the supported antibanding values.
     * 
     * @return a list of supported antibanding values. null if antibanding
     *         setting is not supported.
     * @see #getAntibanding()
     */
    public List<String> getSupportedAntibanding() {
        String str = get(KEY_ANTIBANDING + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * Gets the current scene mode setting.
     * 
     * @return one of SCENE_MODE_XXX string constant. null if scene mode setting
     *         is not supported.
     * @see #SCENE_MODE_AUTO
     * @see #SCENE_MODE_ACTION
     * @see #SCENE_MODE_PORTRAIT
     * @see #SCENE_MODE_LANDSCAPE
     * @see #SCENE_MODE_NIGHT
     * @see #SCENE_MODE_NIGHT_PORTRAIT
     * @see #SCENE_MODE_THEATRE
     * @see #SCENE_MODE_BEACH
     * @see #SCENE_MODE_SNOW
     * @see #SCENE_MODE_SUNSET
     * @see #SCENE_MODE_STEADYPHOTO
     * @see #SCENE_MODE_FIREWORKS
     * @see #SCENE_MODE_SPORTS
     * @see #SCENE_MODE_PARTY
     * @see #SCENE_MODE_CANDLELIGHT
     */
    public String getSceneMode() {
        return get(KEY_SCENE_MODE);
    }

    /**
     * Sets the scene mode. Changing scene mode may override other parameters
     * (such as flash mode, focus mode, white balance). For example, suppose
     * originally flash mode is on and supported flash modes are on/off. In
     * night scene mode, both flash mode and supported flash mode may be changed
     * to off. After setting scene mode, applications should call getParameters
     * to know if some parameters are changed.
     * 
     * @param value scene mode.
     * @see #getSceneMode()
     */
    public void setSceneMode(String value) {
        set(KEY_SCENE_MODE, value);
    }

    /**
     * Gets the supported scene modes.
     * 
     * @return a list of supported scene modes. null if scene mode setting is
     *         not supported.
     * @see #getSceneMode()
     */
    public List<String> getSupportedSceneModes() {
        String str = get(KEY_SCENE_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * Gets the current flash mode setting.
     * 
     * @return current flash mode. null if flash mode setting is not supported.
     * @see #FLASH_MODE_OFF
     * @see #FLASH_MODE_AUTO
     * @see #FLASH_MODE_ON
     * @see #FLASH_MODE_RED_EYE
     * @see #FLASH_MODE_TORCH
     */
    public String getFlashMode() {
        return get(KEY_FLASH_MODE);
    }

    /**
     * Sets the flash mode.
     * 
     * @param value flash mode.
     * @see #getFlashMode()
     */
    public void setFlashMode(String value) {
        set(KEY_FLASH_MODE, value);
    }

    /**
     * Gets the supported flash modes.
     * 
     * @return a list of supported flash modes. null if flash mode setting is
     *         not supported.
     * @see #getFlashMode()
     */
    public List<String> getSupportedFlashModes() {
        String str = get(KEY_FLASH_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * Gets the current focus mode setting.
     * 
     * @return current focus mode. If the camera does not support auto-focus,
     *         this should return {@link #FOCUS_MODE_FIXED}. If the focus mode
     *         is not FOCUS_MODE_FIXED or {@link #FOCUS_MODE_INFINITY},
     *         applications should call {@link #autoFocus(AutoFocusCallback)} to
     *         start the focus.
     * @see #FOCUS_MODE_AUTO
     * @see #FOCUS_MODE_INFINITY
     * @see #FOCUS_MODE_MACRO
     * @see #FOCUS_MODE_FIXED
     */
    public String getFocusMode() {
        return get(KEY_FOCUS_MODE);
    }

    /**
     * Sets the focus mode.
     * 
     * @param value focus mode.
     * @see #getFocusMode()
     */
    public void setFocusMode(String value) {
        set(KEY_FOCUS_MODE, value);
    }

    /**
     * Gets the supported focus modes.
     * 
     * @return a list of supported focus modes. This method will always return a
     *         list with at least one element.
     * @see #getFocusMode()
     */
    public List<String> getSupportedFocusModes() {
        String str = get(KEY_FOCUS_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * Gets the focal length (in millimeter) of the camera.
     * 
     * @return the focal length. This method will always return a valid value.
     */
    public float getFocalLength() {
        return Float.parseFloat(get(KEY_FOCAL_LENGTH));
    }

    /**
     * Gets the horizontal angle of view in degrees.
     * 
     * @return horizontal angle of view. This method will always return a valid
     *         value.
     */
    public float getHorizontalViewAngle() {
        return Float.parseFloat(get(KEY_HORIZONTAL_VIEW_ANGLE));
    }

    /**
     * Gets the vertical angle of view in degrees.
     * 
     * @return vertical angle of view. This method will always return a valid
     *         value.
     */
    public float getVerticalViewAngle() {
        return Float.parseFloat(get(KEY_VERTICAL_VIEW_ANGLE));
    }

    /**
     * Gets the current exposure compensation index.
     * 
     * @return current exposure compensation index. The range is
     *         {@link #getMinExposureCompensation} to
     *         {@link #getMaxExposureCompensation}. 0 means exposure is not
     *         adjusted.
     */
    public int getExposureCompensation() {
        return getInt(KEY_EXPOSURE_COMPENSATION, 0);
    }

    /**
     * Sets the exposure compensation index.
     * 
     * @param value exposure compensation index. The valid value range is from
     *            {@link #getMinExposureCompensation} (inclusive) to
     *            {@link #getMaxExposureCompensation} (inclusive). 0 means
     *            exposure is not adjusted. Application should call
     *            getMinExposureCompensation and getMaxExposureCompensation to
     *            know if exposure compensation is supported.
     */
    public void setExposureCompensation(int value) {
        set(KEY_EXPOSURE_COMPENSATION, value);
    }

    /**
     * Gets the maximum exposure compensation index.
     * 
     * @return maximum exposure compensation index (>=0). If both this method
     *         and {@link #getMinExposureCompensation} return 0, exposure
     *         compensation is not supported.
     */
    public int getMaxExposureCompensation() {
        return getInt(KEY_MAX_EXPOSURE_COMPENSATION, 0);
    }

    /**
     * Gets the minimum exposure compensation index.
     * 
     * @return minimum exposure compensation index (<=0). If both this method
     *         and {@link #getMaxExposureCompensation} return 0, exposure
     *         compensation is not supported.
     */
    public int getMinExposureCompensation() {
        return getInt(KEY_MIN_EXPOSURE_COMPENSATION, 0);
    }

    /**
     * Gets the exposure compensation step.
     * 
     * @return exposure compensation step. Applications can get EV by
     *         multiplying the exposure compensation index and step. Ex: if
     *         exposure compensation index is -6 and step is 0.333333333, EV is
     *         -2.
     */
    public float getExposureCompensationStep() {
        return getFloat(KEY_EXPOSURE_COMPENSATION_STEP, 0);
    }

    /**
     * Gets current zoom value. This also works when smooth zoom is in progress.
     * Applications should check {@link #isZoomSupported} before using this
     * method.
     * 
     * @return the current zoom value. The range is 0 to {@link #getMaxZoom}. 0
     *         means the camera is not zoomed.
     */
    public int getZoom() {
        return getInt(KEY_ZOOM, 0);
    }

    /**
     * Sets current zoom value. If the camera is zoomed (value > 0), the actual
     * picture size may be smaller than picture size setting. Applications can
     * check the actual picture size after picture is returned from
     * {@link PictureCallback}. The preview size remains the same in zoom.
     * Applications should check {@link #isZoomSupported} before using this
     * method.
     * 
     * @param value zoom value. The valid range is 0 to {@link #getMaxZoom}.
     */
    public void setZoom(int value) {
        set(KEY_ZOOM, value);
    }

    /**
     * Returns true if zoom is supported. Applications should call this before
     * using other zoom methods.
     * 
     * @return true if zoom is supported.
     */
    public boolean isZoomSupported() {
        String str = get(KEY_ZOOM_SUPPORTED);
        return TRUE.equals(str);
    }

    /**
     * Gets the maximum zoom value allowed for snapshot. This is the maximum
     * value that applications can set to {@link #setZoom(int)}. Applications
     * should call {@link #isZoomSupported} before using this method. This value
     * may change in different preview size. Applications should call this again
     * after setting preview size.
     * 
     * @return the maximum zoom value supported by the camera.
     */
    public int getMaxZoom() {
        return getInt(KEY_MAX_ZOOM, 0);
    }

    /**
     * Gets the zoom ratios of all zoom values. Applications should check
     * {@link #isZoomSupported} before using this method.
     * 
     * @return the zoom ratios in 1/100 increments. Ex: a zoom of 3.2x is
     *         returned as 320. The number of elements is {@link #getMaxZoom} +
     *         1. The list is sorted from small to large. The first element is
     *         always 100. The last element is the zoom ratio of the maximum
     *         zoom value.
     */
    public List<Integer> getZoomRatios() {
        return splitInt(get(KEY_ZOOM_RATIOS));
    }

    /**
     * Returns true if smooth zoom is supported. Applications should call this
     * before using other smooth zoom methods.
     * 
     * @return true if smooth zoom is supported.
     */
    public boolean isSmoothZoomSupported() {
        String str = get(KEY_SMOOTH_ZOOM_SUPPORTED);
        return TRUE.equals(str);
    }

    // wilson@
    // ISO
    /**
     * @hide
     */
    public String getISOSpeed() {
        return get(KEY_ISOSPEED_MODE);
    }

    /**
     * @hide
     */
    public void setISOSpeed(String value) {
        set(KEY_ISOSPEED_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedISOSpeed() {
        String str = get(KEY_ISOSPEED_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // FocusMeter
    /**
     * @hide
     */
    public String getFocusMeter() {
        return get(KEY_FOCUS_METER);
    }

    /**
     * @hide
     */
    public void setFocusMeter(String value) {
        set(KEY_FOCUS_METER, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedFocusMeter() {
        String str = get(KEY_FOCUS_METER + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Exposure
    /**
     * @hide
     */
    public String getExposure() {
        return get(KEY_EXPOSURE);
    }

    /**
     * @hide
     */
    public void setExposure(String value) {
        set(KEY_EXPOSURE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedExposure() {
        String str = get(KEY_EXPOSURE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // ExposureMeter
    /**
     * @hide
     */
    public String getExposureMeter() {
        return get(KEY_EXPOSURE_METER);
    }

    /**
     * @hide
     */
    public void setExposureMeter(String value) {
        set(KEY_EXPOSURE_METER, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedExposureMeter() {
        String str = get(KEY_EXPOSURE_METER + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // FDMode
    /**
     * @hide
     */
    public String getFDMode() {
        return get(KEY_FD_MODE);
    }

    /**
     * @hide
     */
    public void setFDMode(String value) {
        set(KEY_FD_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedFDMode() {
        String str = get(KEY_FD_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Edge
    /**
     * @hide
     */
    public String getEdgeMode() {
        return get(KEY_EDGE_MODE);
    }

    /**
     * @hide
     */
    public void setEdgeMode(String value) {
        set(KEY_EDGE_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedEdgeMode() {
        String str = get(KEY_EDGE_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Hue
    /**
     * @hide
     */
    public String getHueMode() {
        return get(KEY_HUE_MODE);
    }

    /**
     * @hide
     */
    public void setHueMode(String value) {
        set(KEY_HUE_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedHueMode() {
        String str = get(KEY_HUE_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Saturation
    /**
     * @hide
     */
    public String getSaturationMode() {
        return get(KEY_SATURATION_MODE);
    }

    /**
     * @hide
     */
    public void setSaturationMode(String value) {
        set(KEY_SATURATION_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedSaturationMode() {
        String str = get(KEY_SATURATION_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Brightness
    /**
     * @hide
     */
    public String getBrightnessMode() {
        return get(KEY_BRIGHTNESS_MODE);
    }

    /**
     * @hide
     */
    public void setBrightnessMode(String value) {
        set(KEY_BRIGHTNESS_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedBrightnessMode() {
        String str = get(KEY_BRIGHTNESS_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Contrast
    /**
     * @hide
     */
    public String getContrastMode() {
        return get(KEY_CONTRAST_MODE);
    }

    /**
     * @hide
     */
    public void setContrastMode(String value) {
        set(KEY_CONTRAST_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedContrastMode() {
        String str = get(KEY_CONTRAST_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    // Capture mode
    /**
     * @hide
     */
    public String getCaptureMode() {
        return get(KEY_CAPTURE_MODE);
    }

    /**
     * @hide
     */
    public void setCaptureMode(String value) {
        set(KEY_CAPTURE_MODE, value);
    }

    /**
     * @hide
     */
    public List<String> getSupportedCaptureMode() {
        String str = get(KEY_CAPTURE_MODE + SUPPORTED_VALUES_SUFFIX);
        return split(str);
    }

    /**
     * @hide
     */
    public void setCapturePath(String value) {
        set(KEY_CAPTURE_PATH, value);
    }

    /**
     * @hide
     */
    public void setBurstShotNum(int value) {
        set(KEY_BURST_SHOT_NUM, value);
    }

    //

    // Splits a comma delimited string to an ArrayList of String.
    // Return null if the passing string is null or the size is 0.
    private ArrayList<String> split(String str) {
        if (str == null) {
            return null;
        }
        // Use StringTokenizer because it is faster than split.
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        ArrayList<String> substrings = new ArrayList<String>();
        while (tokenizer.hasMoreElements()) {
            substrings.add(tokenizer.nextToken());
        }
        return substrings;
    }

    // Splits a comma delimited string to an ArrayList of Integer.
    // Return null if the passing string is null or the size is 0.
    private ArrayList<Integer> splitInt(String str) {
        if (str == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        ArrayList<Integer> substrings = new ArrayList<Integer>();
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            substrings.add(Integer.parseInt(token));
        }
        if (substrings.size() == 0) {
            return null;
        }
        return substrings;
    }

    // Returns the value of a float parameter.
    private float getFloat(String key, float defaultValue) {
        try {
            return Float.parseFloat(mMap.get(key));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    // Returns the value of a integer parameter.
    private int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(mMap.get(key));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    // Splits a comma delimited string to an ArrayList of Size.
    // Return null if the passing string is null or the size is 0.
    private ArrayList<Size> splitSize(String str) {
        if (str == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        ArrayList<Size> sizeList = new ArrayList<Size>();
        while (tokenizer.hasMoreElements()) {
            Size size = strToSize(tokenizer.nextToken());
            if (size != null) {
                sizeList.add(size);
            }
        }
        if (sizeList.size() == 0) {
            return null;
        }
        return sizeList;
    }

    // Parses a string (ex: "480x320") to Size object.
    // Return null if the passing string is null.
    private Size strToSize(String str) {
        if (str == null) {
            return null;
        }
        int pos = str.indexOf('x');
        if (pos != -1) {
            String width = str.substring(0, pos);
            String height = str.substring(pos + 1);
            return new Size(Integer.parseInt(width),
                            Integer.parseInt(height));
        }
        Log.e(TAG, "Invalid size parameter string=" + str);
        return null;
    }
}
