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

package com.mediatek.ngin3d;

import com.mediatek.util.JSON;

/**
 * The color class for storing color setting
 */
public class Color implements Cloneable, JSON.ToJson {

    /**
     * Transparent.
     */
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    /**
     * Color of Black
     */
    public static final Color BLACK = new Color();
    /**
     * Color of White
     */
    public static final Color WHITE = new Color(255, 255, 255);
    /**
     * Color of Blue
     */
    public static final Color BLUE = new Color(0, 0, 255);
    /**
     * Color of Red
     */
    public static final Color RED = new Color(255, 0, 0);
    /**
     * Color of Green
     */
    public static final Color GREEN = new Color(0, 255, 0);
    /**
     * Color of Yellow
     */
    public static final Color YELLOW = new Color(255, 255, 0);
    /**
     * Color of Cyan
     */
    public static final Color CYAN = new Color(0, 255, 255);
    /**
     * Color of Magenta
     */
    public static final Color MAGENTA = new Color(255, 0, 255);

    /**
     * R setting of RGB
     */
    public int red;
    /**
     * G setting of RGB
     */
    public int green;
    /**
     * B setting of RGB
     */
    public int blue;
    /**
     * Alpha setting
     */
    public int alpha = 255;

    private static final double FACTOR = 0.7;

    /**
     * Initialize color to opaque black by default.
     */
    public Color() {
        // Do nothing by default
    }

    /**
     * Initialize opaque color with specified R, G, B values.
     *
     * @param r R 0-255
     * @param g B 0-255
     * @param b G 0-255
     */
    public Color(int r, int g, int b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    /**
     * Initialize opaque color with RGB combination value.
     *
     * @param rgb RGB combination value
     */
    public Color(int rgb) {
        setRgb(rgb);
    }

    /**
     * Convert RGB combination value to normal RGB and Alpha value.
     *
     * @param rgb RGB combination value
     */
    public final void setRgb(int rgb) {
        red = (rgb >> 16) & 0xFF;
        green = (rgb >> 8) & 0xFF;
        blue = (rgb >> 0) & 0xFF;
        alpha = (rgb >> 24) & 0xFF;
    }

    /**
     * Initialize color with RGB and A value.
     *
     * @param r red argument
     * @param g green argument
     * @param b blue argument
     * @param a alpha argument
     */
    public Color(int r, int g, int b, int a) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.alpha = a;
    }

    /**
     * Get the RGBA combination value.
     *
     * @return RGBA combination value
     */
    public int getRgb() {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16)
            | ((green & 0xFF) << 8) | ((blue & 0xFF) << 0);
    }

    public static void main(String[] args) {
        int r = 0xFF;
        int g = 0xDD;
        int b = 0xEE;
        int a = 0x00;
        int i = (((r << 24)) | ((g << 16)) | ((b << 8)) | a);
        String s = Integer.toHexString(i);
        System.out.println(s);
    }

    /**
     * Copy a color setting
     *
     * @return a new color object
     */
    public Color copy() {
        try {
            return (Color) clone();
        } catch (CloneNotSupportedException e) {
            return new Color(red, green, blue, alpha);
        }
    }

    /**
     * Set the red value of the color.
     *
     * @param r red argument
     * @return this color object
     */
    public Color red(int r) {
        this.red = r;
        return this;
    }

    /**
     * Set the green value of the color.
     *
     * @param g green argument
     * @return this color object
     */
    public Color green(int g) {
        this.green = g;
        return this;
    }

    /**
     * Set the blue value of the color.
     *
     * @param b blue argument
     * @return this color object
     */
    public Color blue(int b) {
        this.blue = b;
        return this;
    }

    /**
     * Set the alpha value of the color.
     *
     * @param a alpha argument
     * @return this color object
     */
    public Color alpha(int a) {
        this.alpha = a;
        return this;
    }

    /**
     * Return the color setting of this color object.
     *
     * @return a string of color setting
     */
    public String toString() {
        return "Color:[" + red + "," + green + "," + blue + "," + alpha + "]";
    }

    /**
     * Convert the color property to JSON formatted String
     * @return   output JSON formatted String
     */
    public String toJson() {
        return "{Color:[" + red + "," + green + "," + blue + "," + alpha + "]}";
    }

    /**
     * Set HIS color setting
     *
     * @param hue        hue argument
     * @param luminance  luminance argument
     * @param saturation saturation argument
     */
    public void setHls(float hue, float luminance, float saturation) {
        if (saturation == 0) {
            red = (int) (luminance * 255.0f + 0.5f);
            green = red;
            blue = red;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = luminance * (1.0f - saturation);
            float q = luminance * (1.0f - saturation * f);
            float t = luminance * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
            case 0:
                red = (int) (luminance * 255.0f + 0.5f);
                green = (int) (t * 255.0f + 0.5f);
                blue = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                red = (int) (q * 255.0f + 0.5f);
                green = (int) (luminance * 255.0f + 0.5f);
                blue = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                red = (int) (p * 255.0f + 0.5f);
                green = (int) (luminance * 255.0f + 0.5f);
                blue = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                red = (int) (p * 255.0f + 0.5f);
                green = (int) (q * 255.0f + 0.5f);
                blue = (int) (luminance * 255.0f + 0.5f);
                break;
            case 4:
                red = (int) (t * 255.0f + 0.5f);
                green = (int) (p * 255.0f + 0.5f);
                blue = (int) (luminance * 255.0f + 0.5f);
                break;
            case 5:
                red = (int) (luminance * 255.0f + 0.5f);
                green = (int) (p * 255.0f + 0.5f);
                blue = (int) (q * 255.0f + 0.5f);
                break;

            default:
                break;
            }
        }

    }

    /**
     * Make the color setting of this color object darker
     *
     * @return a new color object
     */
    public Color darker() {
        return new Color(Math.max((int) (red * FACTOR), 0), Math.max(
            (int) (green * FACTOR), 0), Math.max((int) (blue * FACTOR), 0));
    }

    /**
     * Make the color setting of this color object brighter
     *
     * @return a new color object
     */
    public Color brighter() {
        int r = red;
        int g = green;
        int b = blue;

        /*
         * From 2D group: 1. black.brighter() should return grey 2. applying
         * brighter to blue will always return blue, brighter 3. non pure color
         * (non zero rgb) will eventually return white
         */
        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i);
        }
        if (r > 0 && r < i)
            r = i;
        if (g > 0 && g < i)
            g = i;
        if (b > 0 && b < i)
            b = i;

        return new Color(Math.min((int) (r / FACTOR), 255), Math.min(
            (int) (g / FACTOR), 255), Math.min((int) (b / FACTOR), 255));
    }

    /**
     * Check if the input object is equal to this color object or compare their property if they are different objects.
     *
     * @param o the object to be compared
     * @return true if the object is the same as this color object
     */
    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof Color) {
            Color c = (Color) o;
            return this.red == c.red && this.green == c.green && this.blue == c.blue && this.alpha == c.alpha;
        }
        return false;
    }

    /**
     * Create a new hash code.
     *
     * @return a new hash code
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + red;
        result = 37 * result + green;
        result = 37 * result + blue;
        result = 37 * result + alpha;
        return result;
    }
}
