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
#ifndef _MHAL_INC_AREAS_H_
#define _MHAL_INC_AREAS_H_


/**
 * The information of a area to camera driver
 * 
 *  Metering areas are relative to the current field of view
 *  No matter what the zoom level is, (-1000,-1000)
 * represents the top of the currently visible camera frame. The
 * metering area cannot be set to be outside the current field of view,
 * even when using zoom
 */
struct camera_area_t {
    /**
     * Bounds of the area [left, top, right, bottom]. (-1000, -1000) represents
     * the top-left of the camera field of view, and (1000, 1000) represents the
     * bottom-right of the field of view. The width and height cannot be 0 or
     * negative. 
     */
    MINT32 left;
 
    MINT32 top; 
 
    MINT32 right; 

    MINT32 bottom; 
    /**
     * The weight must range from 1 to 1000, and represents a weight for
     * every pixel in the area. This means that a large metering area with
     * the same weight as a smaller area will have more effect in the
     * metering result. Metering areas can partially overlap and the driver
     * will add the weights in the overlap region
     */
    MINT32 weight; 
} ;


#define MAX_FOCUS_AREAS  9 
#define MAX_METERING_AREAS 9 
/**
 * The information of focus area to AF driver
 * 
 */
struct camera_focus_areas_t {
	camera_area_t areas[MAX_FOCUS_AREAS]; 
	MUINT32 count; 
}; 

/**
 * The information of focus area to AE driver
 * 
 */
struct camera_metering_areas_t {
	camera_area_t areas[MAX_METERING_AREAS]; 
	MUINT32 count; 
}; 


#endif

