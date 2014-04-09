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
#ifndef _MHAL_INC_CAMERA_IOCTL_H_
#define _MHAL_INC_CAMERA_IOCTL_H_


/******************************************************************************
*   Function Prototypes.
*******************************************************************************/


/**
 * @par Function
 *   mHalOpen
 * @par Description
 *   Open a mHal instance
 * @par Returns
 *   return a created instance.
 */
void*
mHalOpen();


/**
 * @par Function
 *   mHalClose
 * @par Description
 *   Close a mHal instance
 * @param
 *   fd     [I]     file descriptor returned from mHalOpen()

 * @par Returns
 *   none.
 */
void
mHalClose(void* fd);


/**
 * @par Function
 *   mHalIoctl
 * @par Description
 *   mHal ioctl function with fd
 * @param
 *   uCtrlCode          [I] The IO Control Code
 * @param
 *   pvInBuf            [I] The input parameter
 * @param
 *   uInBufSize         [I] The size of input parameter structure
 * @param
 *   pvOutBuf           [O] The output parameter
 * @param
 *   uOutBufSize        [I] The size of output parameter structure
  * @param
 *   puBytesReturned    [O] The number of bytes of return value
 * @par Returns
 *   error code
 */
int
mHalIoctl(
    void*           fd, 
    unsigned int    uCtrlCode, 
    void*           pvInBuf, 
    unsigned int    uInBufSize, 
    void*           pvOutBuf, 
    unsigned int    uOutBufSize, 
    unsigned int*   puBytesReturned
);


/******************************************************************************
*
*******************************************************************************/
/*
#define MHAL_IOCTL_CAMERA_GROUP_MASK                0x1000  ///< Camera group mask
*/
enum
{
    MHAL_IOCTL_CAMERA_GROUP_START                   =   0x1000, 
    //
    MHAL_IOCTL_SEARCH_CAMERA,                       ///< To search camera
    MHAL_IOCTL_INIT_CAMERA,                         ///< To init camera
    MHAL_IOCTL_UNINIT_CAMERA,                       ///< To un-init camera
    //
    MHAL_IOCTL_SET_CAM_FEATURE_MODE,                ///< To set camera feature mode
    MHAL_IOCTL_GET_CAM_FEATURE_ENUM,                ///< To get camera feature
    //
    MHAL_IOCTL_PREVIEW_START,                       ///< To start the camera preview
    MHAL_IOCTL_PREVIEW_STOP,                        ///< To stop the camera preview
    //
    MHAL_IOCTL_CAPTURE_INIT,                        ///< To init the camera capture function
    MHAL_IOCTL_CAPTURE_UNINIT,                      ///< To uninit the camera capture function
    MHAL_IOCTL_CAPTURE_START,                       ///< To start the camera capture function
    MHAL_IOCTL_CAPTURE_CANCEL,                      ///< To cancel capture
    MHAL_IOCTL_PRE_CAPTURE,                         ///< To start the camera pre-capture function
    MHAL_IOCTL_SET_SHOT_MODE,                       ///< To set the current shot mode.
    //
    MHAL_IOCTL_VIDEO_START_RECORD,                  ///< To start record video
    MHAL_IOCTL_VIDEO_STOP_RECORD,                   ///< To stop record video
    MAHL_IOCTL_RELEASE_VDO_FRAME,                   ///< To release record frame 
    //
    MHAL_IOCTL_DO_FOCUS,                            ///< To do focus
    MHAL_IOCTL_CANCEL_FOCUS,                        ///< To cancel focus
    //
    MHAL_IOCTL_SET_ZOOM,                            ///< To set zoom
    //
    MHAL_IOCTL_SET_CAM_DISP_PARAMETER,              ///< To set camera display info
    MHAL_IOCTL_SET_CAM_3A_PARAMETER,                ///< To set camera parameter while previewing
    MHAL_IOCTL_SET_FLASHLIGHT_PARAMETER,            ///< To set  flashlight parameter
    //
    MHAL_IOCTL_START_SD_PREVIEW,                    ///< To start smile detection preview
    MHAL_IOCTL_CANCEL_SD_PREVIEW,                   ///< To cancel smile detection preview
    //
    MHAL_IOCTL_START_MAV,                           ///< To start mav
    MHAL_IOCTL_STOP_MAV,                            ///< To stop mav
    //
    MHAL_IOCTL_DO_PANORAMA,                         ///< To do panorama
    MHAL_IOCTL_CANCEL_PANORAMA,                     ///< To cancel panorama
    //
    MHAL_IOCTL_START_AUTORAMA,                      ///< To start autorama
    MHAL_IOCTL_STOP_AUTORAMA,                       ///< To stop autorama
    //
    MHAL_IOCTL_START_3DSHOT,                        ///< To start autorama
    MHAL_IOCTL_STOP_3DSHOT ,                        ///< To stop autorama
    //
    MHAL_IOCTL_START_FACE_DETECTION,                ///< To start face detection
    MHAL_IOCTL_STOP_FACE_DETECTION,                 ///< To stop face detection
    //
    MHAL_IOCTL_SET_ATV_DISP,                        ///< To set atv display
    MHAL_IOCTL_GET_ATV_DISP_DELAY,                  ///< To get atv display delay
    //
    MHAL_IOCTL_GET_BUF_SUPPORT_FORMAT,              ///< To get preview support format 
    MHAL_IOCTL_GET_CAM_BUF_MEM_INFO,                ///< To get cam buffer info 
    MHAL_IOCTL_GET_BS_INFO,                         ///< To get bitstream buffer info
    MHAL_IOCTL_GET_RAW_IMAGE_INFO,                  ///< To get sensor resolution
     //
    MHAL_IOCTL_GET_3A_SUPPORT_FEATURE,              ///< To get the 3A support feature 
    //
    MHAL_IOCTL_VIDEO_SNAPSHOT                       ///< To take pic when video recording.
};


////////////////////////////////////////////////////////////////////////////////
#endif  //  _MHAL_INC_CAMERA_IOCTL_H_

