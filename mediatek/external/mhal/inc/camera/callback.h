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
#ifndef _MHAL_INC_CAMERA_CALLBACK_H_
#define _MHAL_INC_CAMERA_CALLBACK_H_


/******************************************************************************
*
*******************************************************************************/


/**
 * The defination of Callback function
 */
typedef void (*pfMediaHalCallback)(void *);


/**
 * @par Structure
 *   mhalEISInfo_t
 * @par Description
 *   This is a structure which store the EIS Info
 */
typedef struct mhalEISInfo_s {
    MINT32 en; 
    MINT32 w; 
    MINT32 h; 
    MINT32 gmvX; 
    MINT32 gmvY;     
}mhalEISInfo_t; 


/**
 * @par Structure
 *   mhalCamBufCBInfo
 * @par Description
 *   This is a structure to store the buffer callback info 
 */
struct mhalCamBufCBInfo 
{
public: 
    	MUINT32 u4BufIndex; 
public:
	mhalCamBufCBInfo (MUINT32 const _u4BufIndex = 0) 
		: u4BufIndex (_u4BufIndex) 
		{
		}
}; 

/**
 * @par Structure
 *   mhalCamTimeStampBufCBInfo
 * @par Description
 *   This is a structure to store the time stamp buf cb info 
 */
struct mhalCamTimeStampBufCBInfo  : public mhalCamBufCBInfo
{
public:
	MUINT32 u4TimStampS;
	MUINT32 u4TimStampUs; 
	
public: 
	mhalCamTimeStampBufCBInfo(MUINT32 const _u4BufIndex =0,  MUINT32 _u4TimStampS = 0, MUINT32 const _u4TimStampUs = 0)
		: mhalCamBufCBInfo(_u4BufIndex)
		, u4TimStampS(_u4TimStampS) 
		, u4TimStampUs(_u4TimStampUs) 
		{
		}
}; 

/**
 *
 */
enum MHAL_CAM_CB_ENUM
{
    MHAL_CAM_CB_ERR, 
    MHAL_CAM_CB_PREVIEW,
    MHAL_CAM_CB_SHUTTER,
    MHAL_CAM_CB_RAW,
    MHAL_CAM_CB_JPEG,
    MHAL_CAM_CB_CAPTURE_DONE, 
    MHAL_CAM_CB_AF,
    MHAL_CAM_CB_AF_MOVE, 
    MHAL_CAM_CB_FD,
    MHAL_CAM_CB_ZOOM,
    MHAL_CAM_CB_POSTVIEW,
    MHAL_CAM_CB_SMILE,
    MHAL_CAM_CB_PANORAMA,
    MHAL_CAM_CB_MAV,
    MHAL_CAM_CB_ATV_DISP,
    MHAL_CAM_CB_AUTORAMA,
    MHAL_CAM_CB_AUTORAMAMV,
    MHAL_CAM_CB_SCALADO,
    MHAL_CAM_CB_ASD,
    MHAL_CAM_CB_VIDEO_RECORD,
    MHAL_CAM_CB_VSS_JPG_ENC,
    MHAL_CAM_CB_VSS_JPG,
	MHAL_CAM_CB_FLASHON,
    MHAL_CAM_CB_ZSD_PREVIEW_DONE
};


/**
 *  mHalCam Callback Information.
 */
struct mHalCamCBInfo
{
    void*       mCookie;
    MUINT32     mType;
    void*       mpData;
    MUINT32     mDataSize;
    //
    mHalCamCBInfo(
        void*   cookie, 
        MUINT32 u4Type, 
        void*   pData = 0, 
        MUINT32 u4DataSize = 0
    )
        : mCookie(cookie)
        , mType(u4Type)
        , mpData(pData)
        , mDataSize(u4DataSize)
    {}
};


/**
 *  mHalCam Observer.
 */
struct mHalCamObserver
{
    typedef pfMediaHalCallback  CallbackFunc_t;
    //
    CallbackFunc_t  mpfCallback;
    void*           mCookie;
    //
    mHalCamObserver(
        CallbackFunc_t  pfCallback = 0, 
        void*           cookie = 0
    )
        : mpfCallback(pfCallback)
        , mCookie(cookie)
    {}
    //
    inline  bool operator!() const
    {
        return  (0 == mpfCallback);
    }
    //
    inline  void notify(MUINT32 u4Type, void* pData = 0, MUINT32 u4DataSize = 0)
    {
        if  ( mpfCallback )
        {
            mHalCamCBInfo info(mCookie, u4Type, pData, u4DataSize);
            mpfCallback(&info);
        }
    }
};


////////////////////////////////////////////////////////////////////////////////
#endif  //  _MHAL_INC_CAMERA_CALLBACK_H_

