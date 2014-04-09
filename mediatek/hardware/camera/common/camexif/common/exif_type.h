/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
#ifndef _EXIF_TYPE_H_
#define _EXIF_TYPE_H_

/*******************************************************************************
*
********************************************************************************/
// Data types
typedef int             INT32;
typedef void            VOID;

/*******************************************************************************
*
********************************************************************************/
#define IFD_MAX_ZEROIFD_CNT  0x10
#define IFD_MAX_EXIFIFD_CNT  0x30
#define IFD_MAX_GPSIFD_CNT   0x10
#define IFD_MAX_FIRSTIFD_CNT 0x10
#define IFD_MAX_ITOPIFD_CNT  0x08

enum ifdDataType {
    IFD_DATATYPE_BYTE = 1,
    IFD_DATATYPE_ASCII,
    IFD_DATATYPE_SHORT,
    IFD_DATATYPE_LONG,
    IFD_DATATYPE_RATIONAL,
    IFD_DATATYPE_UNDEFINED = 7,
    IFD_DATATYPE_SLONG = 9,
    IFD_DATATYPE_SRATIONAL = 10
};

enum idfTypeID {
    IFD_TYPE_ZEROIFD = 1,
    IFD_TYPE_EXIFIFD,
    IFD_TYPE_GPSIFD,
    IFD_TYPE_FIRSTIFD,
    IFD_TYPE_ITOPIFD
};

enum markerID{
    SOF_MARKER  = 0xc0,
    SOF2_MARKER = 0xc2,
    DHT_MARKER  = 0xc4,

    SOI_MARKER  = 0xd8,
    EOI_MARKER  = 0xd9,
    SOS_MARKER  = 0xda,
    DQT_MARKER  = 0xdb,
    DRI_MARKER  = 0xdd,

    APP0_MARKER = 0xe0,
    APP1_MARKER = 0xe1,
    APP2_MARKER = 0xe2,
    APP3_MARKER = 0xe3,
    APP4_MARKER = 0xe4,
    APP5_MARKER = 0xe5,
    APP6_MARKER = 0xe6,
    APP7_MARKER = 0xe7,
    APP8_MARKER = 0xe8,
    APP9_MARKER = 0xe9,

    COM_MARKER = 0xfe,
    PREFIX_MARKER = 0xff
};

enum ECapTypeId
{
    eCapTypeId_Standard   = 0,
    eCapTypeId_Landscape  = 1,
    eCapTypeId_Portrait   = 2,
    eCapTypeId_Night      = 3
};

enum EExpProgramId
{
    eExpProgramId_NotDefined    = 0,
    eExpProgramId_Manual        = 1,
    eExpProgramId_Normal        = 2,
    eExpProgramId_Portrait      = 7,
    eExpProgramId_Landscape     = 8
};

enum ELightSourceId
{
    eLightSourceId_Daylight     = 1,
    eLightSourceId_Fluorescent  = 2,
    eLightSourceId_Tungsten     = 3,
    eLightSourceId_Cloudy       = 10,
    eLightSourceId_Shade        = 11,
    eLightSourceId_Other        = 255
};

enum EMeteringModeId
{
    eMeteringMode_Average   = 1,
    eMeteringMode_Center    = 2,
    eMeteringMode_Spot      = 3,
    eMeteringMode_Other     = 255
};

#define IFD0_TAG_IMGDESC        0x010E
#define IFD0_TAG_MAKE           0x010F
#define IFD0_TAG_MODEL          0x0110
#define IFD0_TAG_ORIENT         0x0112
#define IFD0_TAG_XRES           0x011A
#define IFD0_TAG_YRES           0x011B
#define IFD0_TAG_RESUNIT        0x0128
#define IFD0_TAG_SOFTWARE       0x0131
#define IFD0_TAG_DATETIME       0x0132
#define IFD0_TAG_YCBCRPOS       0x0213
#define IFD0_MTK_IMGINDEX       0x0220  //new definition: the index of continuous shot image.
#define IFD0_MTK_GROUPID        0x0221  //new definition: group ID for continuous shot.
#define IFD0_MTK_BESTFOCUSH     0x0222  //new definition: focus value (H) for best shot.
#define IFD0_MTK_BESTFOCUSL     0x0223  //new definition: focus value (L) for best shot.
#define IFD0_TAG_EXIFPTR        0x8769
#define IFD0_TAG_GPSINFO        0x8825
#define IFD0_TAG_IPM2           0xC4A5

#define EXIF_TAG_EXPTIME        0x829A
#define EXIF_TAG_FNUM           0x829D
#define EXIF_TAG_EXPPROG        0x8822
#define EXIF_TAG_ISOSPEEDRATE   0x8827
#define EXIF_TAG_EXIFVER        0x9000
#define EXIF_TAG_DATETIMEORIG   0x9003
#define EXIF_TAG_DATETIMEDITI   0x9004
#define EXIF_TAG_COMPCONFIGURE  0x9101
#define EXIF_TAG_COMPRESSBPP    0x9102
#define EXIF_TAG_EXPBIAS        0x9204
#define EXIF_TAG_MAXAPTURE      0x9205
#define EXIF_TAG_METERMODE      0x9207
#define EXIF_TAG_LIGHTSOURCE    0x9208
#define EXIF_TAG_FLASH          0x9209
#define EXIF_TAG_FOCALLEN       0x920A
#define EXIF_TAG_MAKERNOTE      0x927C
#define EXIF_TAG_USRCOMMENT     0x9286

#define EXIF_TAG_FLRESHPIXVER   0xA000
#define EXIF_TAG_COLORSPACE     0xA001
#define EXIF_TAG_PEXELXDIM      0xA002
#define EXIF_TAG_PEXELYDIM      0xA003
#define EXIF_TAG_AUDIOFILE      0xA004
#define EXIF_TAG_ITOPIFDPTR     0xA005
#define EXIF_TAG_FILESOURCE     0xA300
#define EXIF_TAG_SENCETYPE      0xA301
#define EXIF_TAG_EXPOSUREMODE   0xA402
#define EXIF_TAG_WHITEBALANCEMODE   0xA403
#define EXIF_TAG_DIGITALZOOMRATIO   0xA404
#define EXIF_TAG_SCENECAPTURETYPE   0xA406

#define GPS_TAG_VERSIONID       0x0000
#define GPS_TAG_LATITUDEREF     0x0001
#define GPS_TAG_LATITUDE        0x0002
#define GPS_TAG_LONGITUDEREF    0x0003
#define GPS_TAG_LONGITUDE       0x0004
#define GPS_TAG_ALTITUDEREF     0x0005
#define GPS_TAG_ALTITUDE        0x0006
#define GPS_TAG_TIMESTAMP       0x0007
#define GPS_TAG_PROCESSINGMETHOD    0x001B
#define GPS_TAG_DATESTAMP       0x001D

#define IFD1_TAG_COMPRESS           0x0103
#define IFD1_TAG_ORIENT             0x0112
#define IFD1_TAG_XRES               0x011A
#define IFD1_TAG_YRES               0x011B
#define IFD1_TAG_RESUINT            0x0128
#define IFD1_TAG_JPG_INTERCHGFMT    0x0201
#define IFD1_TAG_JPG_INTERCHGFMTLEN 0x0202
#define IFD1_TAG_YCBCRPOS           0x0213

#define ITOP_TAG_ITOPINDEX      0x0001
#define ITOP_TAG_ITOPVERSION    0x0002

#define INVALID_TAG             0xFFFF

/*******************************************************************************
*
********************************************************************************/
typedef struct IFD {
    unsigned short tag;
    unsigned short type;   
    unsigned int count;
    unsigned int valoff;
} IFD_t;
 
typedef struct ifdNode {
    IFD_t ifd;
    struct ifdNode *next;
} ifdNode_t;

typedef struct zeroIFDList {
    ifdNode_t *pheadNode;
    unsigned int nodeCnt;
    ifdNode_t ifdNodePool[IFD_MAX_ZEROIFD_CNT];
    unsigned char valBuf[0x20 * IFD_MAX_ZEROIFD_CNT];
    unsigned int valBufPos;
} zeroIFDList_t;

typedef struct exifIFDList {
    ifdNode_t* pheadNode;
    unsigned int nodeCnt;
    ifdNode_t ifdNodePool[IFD_MAX_EXIFIFD_CNT];
    unsigned char* pvalBuf;
    unsigned char valBuf[0x40 * IFD_MAX_EXIFIFD_CNT];
    unsigned int valBufPos;
} exifIFDList_t;

typedef struct gpsIFDList {
    ifdNode_t* pheadNode;
    unsigned int nodeCnt;
    ifdNode_t ifdNodePool[IFD_MAX_GPSIFD_CNT];
    unsigned char* pvalBuf;
    unsigned char valBuf[0x20 * IFD_MAX_GPSIFD_CNT];
    unsigned int valBufPos;
} gpsIFDList_t;

typedef struct firstIFDList {
    ifdNode_t* pheadNode;
    unsigned int nodeCnt;
    ifdNode_t ifdNodePool[IFD_MAX_FIRSTIFD_CNT];
    unsigned char* pvalBuf;
    unsigned char valBuf[0x20 * IFD_MAX_FIRSTIFD_CNT];
    unsigned int valBufPos;
} firstIFDList_t;

typedef struct itopIFDList {
    ifdNode_t* pheadNode;
    unsigned int nodeCnt;
    ifdNode_t ifdNodePool[IFD_MAX_ITOPIFD_CNT];
    unsigned char* pvalBuf;
    unsigned char valBuf[0x20 * IFD_MAX_ITOPIFD_CNT];
    unsigned int valBufPos;
} itopIFDList_t;

typedef struct TiffHeader_s {
    unsigned short byteOrder;
    unsigned short fixed;
    unsigned int ifdOffset;
} TiffHeader_t;

#endif /* _EXIF_TYPE_H_ */


