/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef _MHAL_INC_CAMERA_GRAPHICS_H_
#define _MHAL_INC_CAMERA_GRAPHICS_H_


/*******************************************************************************
*
*******************************************************************************/
namespace NSCamera
{
////////////////////////////////////////////////////////////////////////////////


/**
 *  Pixel Format
 */
enum EPixelFormat
{
    ePIXEL_FORMAT_UNKNOWN           = 0,                    //unknow 
    //
    ePIXEL_FORMAT_BAYER8            = 0x00000001,           //Bayer format, 8-bit 
    ePIXEL_FORMAT_BAYER10           = 0x00000002,           //Bayer format, 10-bit
    ePIXEL_FORMAT_BAYER12           = 0x00000004,           //Bayer format, 12-bit 
    //
    ePIXEL_FORMAT_I420              = 0x00000010,           //420 format, 3 plane.
                                                            //      8 bit Y plane followed by 8 bit 2x2 subsampled U and V planes.
                                                            //      These formats are identical to YV12 except that the U and V plane order is reversed.
    ePIXEL_FORMAT_YV12              = 0x00000020,           //420 format, 3 plane  
                                                            //      8 bit Y plane followed by 8 bit 2x2 subsampled V and U planes.
    ePIXEL_FORMAT_NV21              = 0x00000040,           //420 format, 2 plane (VU)
    ePIXEL_FORMAT_NV21_BLK          = 0x00000080,           //420 format block mode, 2 plane (UV)
    ePIXEL_FORMAT_NV12              = 0x00000100,           //420 format, 2 plane (UV)
    ePIXEL_FORMAT_NV12_BLK          = 0x00000200,           //420 format block mode, 2 plane (VU)        
    ePIXEL_FORMAT_YUY2              = 0x00000400,           //422 format, 1 plane (YUYV)
    ePIXEL_FORMAT_UYVY              = 0x00000800,           //422 format, 1 plane (UYVY)
    //
    //  Notes:
    //      Pixel Order: LSB -> MSB
    //      For example, LSB of RGBA8888 is 'R'
    ePIXEL_FORMAT_RGB565            = 0x00010000,           //RGB 565 (16-bit), 1 plane
    ePIXEL_FORMAT_RGB888            = 0x00020000,           //RGB 888 (24-bit), 1 plane
    ePIXEL_FORMAT_RGBA8888          = 0x00040000,           //RGBA (32-bit), 1 plane
    ePIXEL_FORMAT_ARGB8888          = 0x00080000,           //ARGB (32-bit), 1 plane
    //
    ePIXEL_FORMAT_JPEG              = 0x80000000,           //JPEG format 
    //

    ePIXEL_FORMAT_Y800              = 0x10000000,           //Only Y, 8-bit.
};


////////////////////////////////////////////////////////////////////////////////
};  //namespace NSCamera
#endif  //  _MHAL_INC_CAMERA_GRAPHICS_H_

