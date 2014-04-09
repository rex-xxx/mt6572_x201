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
#include "exif_internal.h"

/*******************************************************************************
*
********************************************************************************/
static unsigned char exifVersion[] = { 
    '0', '2', '2', '0'
};

static unsigned char gpsVersion[] = { 
    2, 2, 0, 0
};

static unsigned short zeroTagID[] = {
    IFD0_TAG_IMGDESC,
    IFD0_TAG_MAKE,
    IFD0_TAG_MODEL,
    IFD0_TAG_ORIENT,
    IFD0_TAG_XRES,
    IFD0_TAG_YRES,
    IFD0_TAG_RESUNIT,
    IFD0_TAG_SOFTWARE,
    IFD0_TAG_DATETIME,
    IFD0_TAG_YCBCRPOS,
    IFD0_MTK_IMGINDEX,      // new definition
    IFD0_MTK_GROUPID,       // new definition
    IFD0_MTK_BESTFOCUSH,    // new definition
    IFD0_MTK_BESTFOCUSL,    // new definition    
    IFD0_TAG_EXIFPTR, 
    IFD0_TAG_GPSINFO
};

static unsigned short exifTagID[] = {
    EXIF_TAG_EXPTIME,
    EXIF_TAG_FNUM,
    EXIF_TAG_EXPPROG,
    EXIF_TAG_ISOSPEEDRATE,
    EXIF_TAG_EXIFVER,
    EXIF_TAG_DATETIMEORIG,
    EXIF_TAG_DATETIMEDITI,
    EXIF_TAG_COMPCONFIGURE,
    /*EXIF_TAG_COMPRESSBPP,*/
    EXIF_TAG_EXPBIAS,
    /*EXIF_TAG_MAXAPTURE,*/
    EXIF_TAG_METERMODE,
    EXIF_TAG_LIGHTSOURCE,
    EXIF_TAG_FLASH,
    EXIF_TAG_FOCALLEN,
    /*EXIF_TAG_USRCOMMENT,*/
    EXIF_TAG_FLRESHPIXVER,
    EXIF_TAG_COLORSPACE,
    EXIF_TAG_PEXELXDIM,
    EXIF_TAG_PEXELYDIM, 
    /*EXIF_TAG_AUDIOFILE,*/ 
    EXIF_TAG_ITOPIFDPTR,
    /*EXIF_TAG_FILESOURCE,*/
    /*EXIF_TAG_SENCETYPE, */
    EXIF_TAG_DIGITALZOOMRATIO,
    EXIF_TAG_SCENECAPTURETYPE,
    EXIF_TAG_EXPOSUREMODE,
    EXIF_TAG_WHITEBALANCEMODE
};

static unsigned short gpsTagID[] = {
    GPS_TAG_VERSIONID,
    GPS_TAG_LATITUDEREF,
    GPS_TAG_LATITUDE,
    GPS_TAG_LONGITUDEREF,
    GPS_TAG_LONGITUDE,
    GPS_TAG_ALTITUDEREF,
    GPS_TAG_ALTITUDE,
    GPS_TAG_TIMESTAMP,
    GPS_TAG_PROCESSINGMETHOD,
    GPS_TAG_DATESTAMP
};

static unsigned short firstTagID[] = {
    IFD1_TAG_COMPRESS,
    IFD1_TAG_ORIENT,
    IFD1_TAG_XRES,
    IFD1_TAG_YRES,
    IFD1_TAG_RESUINT ,
    IFD1_TAG_JPG_INTERCHGFMT,
    IFD1_TAG_JPG_INTERCHGFMTLEN,
    IFD1_TAG_YCBCRPOS  
};

static unsigned short itopTagID[] = {
    ITOP_TAG_ITOPINDEX,
    ITOP_TAG_ITOPVERSION 
};

/*******************************************************************************
*
********************************************************************************/
unsigned int 
ifdValueInit(
)
{
    unsigned int err = 0;
    unsigned int idx;
    zeroIFDList_t *pzeroList;
    exifIFDList_t *pexifList;
    gpsIFDList_t *pgpsList;
    firstIFDList_t *pfirstList;
    itopIFDList_t *pitopList;
    ifdNode_t *pnode;
    
    pzeroList = ifdZeroListGet();
    for (idx = 0; idx < (sizeof(zeroTagID) >> 1); idx++) {
        if ((zeroTagID[idx] == IFD0_TAG_GPSINFO) && (exifIsGpsOnFlag() == 0)) {
            continue;
        }
        
        pnode = ifdListNodeAlloc(IFD_TYPE_ZEROIFD);
        if (!pnode) {
            return LIBEXIF_IFD_ERR0004;
        }
        
        pnode->ifd.tag = zeroTagID[idx];
        if ((err = ifdZeroIFDValInit(pnode, pzeroList)) == 0) {
            ifdListNodeInsert(IFD_TYPE_ZEROIFD, pnode, 0);
        }
    }
    
    pexifList = ifdExifListGet();
    for (idx = 0; idx < (sizeof(exifTagID) >> 1); idx++) {
        pnode = ifdListNodeAlloc(IFD_TYPE_EXIFIFD);
        if (!pnode) {
            return LIBEXIF_IFD_ERR0004;
        }
        
        pnode->ifd.tag = exifTagID[idx];
        if ((err = ifdExifIFDValInit(pnode, pexifList)) == 0) {
            ifdListNodeInsert(IFD_TYPE_EXIFIFD, pnode, 0);
        }
    }

    pgpsList = ifdGpsListGet();
    for (idx = 0; idx < (sizeof(gpsTagID) >> 1); idx++) {
        pnode = ifdListNodeAlloc(IFD_TYPE_GPSIFD);
        if (!pnode) {
            return LIBEXIF_IFD_ERR0004;
        }
        
        pnode->ifd.tag = gpsTagID[idx];
        if ((err = ifdGpsIFDValInit(pnode, pgpsList)) == 0) {
            ifdListNodeInsert(IFD_TYPE_GPSIFD, pnode, 0);
        }
    }
    
    pfirstList= ifdFirstListGet();
    for (idx = 0; idx < (sizeof(firstTagID) >> 1); idx++) {
        pnode = ifdListNodeAlloc(IFD_TYPE_FIRSTIFD);
        if (!pnode) {
            return LIBEXIF_IFD_ERR0004;
        }
        
        pnode->ifd.tag = firstTagID[idx];
        if ((err = ifdFirstIFDValInit(pnode, pfirstList)) == 0) {
            ifdListNodeInsert(IFD_TYPE_FIRSTIFD, pnode, 0);
        }
    }

    pitopList= ifdItopListGet();
    for (idx = 0; idx < (sizeof(itopTagID) >> 1); idx++) {
        pnode = ifdListNodeAlloc(IFD_TYPE_ITOPIFD);
        if (!pnode) {
            return LIBEXIF_IFD_ERR0004;
        }
        
        pnode->ifd.tag = itopTagID[idx];
        if ((err = ifdItopIFDValInit(pnode, pitopList)) == 0) {
            ifdListNodeInsert(IFD_TYPE_ITOPIFD, pnode, 0);
        }
    }
    
    exifErrPrint((unsigned char *) "ifdValueInit", err);

    return err;
}

/*******************************************************************************
*
********************************************************************************/
unsigned int 
ifdZeroIFDValInit(
    ifdNode_t *pnode, 
    struct zeroIFDList *plist
)
{   
    unsigned int err = 0;
    unsigned char *pdata;
    IFD_t *pifd;
    unsigned int idx = 0;
    
    pdata = plist->valBuf + plist->valBufPos;
    pifd = &pnode->ifd;

    while (idx < plist->nodeCnt) {
        if (plist->ifdNodePool[idx].ifd.tag == pifd->tag) {
            err = LIBEXIF_IFD_ERR0005;
            return err;
        }
        idx ++;
    }
    
    switch (pifd->tag) {
    case IFD0_TAG_IMGDESC:
        strcpy((char *) pdata , "Unknown Image Title            ");
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->valoff = plist->valBufPos;
        pifd->count = 0x20;
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    case IFD0_TAG_MAKE:
        strcpy((char *) pdata , "Unknown Manufacturer Name");
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->valoff = plist->valBufPos;
        pifd->count = 0x20;
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    case IFD0_TAG_MODEL:
        strcpy((char *) pdata , "Unknown Model Name ");
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->valoff = plist->valBufPos;
        pifd->count = 0x20;
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    case IFD0_TAG_ORIENT:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff , 1); /* no rotatation */
        break;
    case IFD0_TAG_XRES:
    case IFD0_TAG_YRES:
        pifd->type = IFD_DATATYPE_RATIONAL;
        pifd->count = 1;
        pifd->valoff = plist->valBufPos;
        write32( pdata , 72);
        pdata += sizeof(unsigned int);
        write32( pdata , 1);
        pdata += sizeof(unsigned int);
        plist->valBufPos += (sizeof(unsigned int) << 1);
        break;
    case IFD0_TAG_RESUNIT:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff , 2);/* inches */
        break;
    case IFD0_TAG_SOFTWARE:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 32;
        pifd->valoff = plist->valBufPos;
        strcpy((char *) pdata , "MediaTek Camera Application");
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    case IFD0_TAG_DATETIME:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 20;
        pifd->valoff = plist->valBufPos;
        strcpy((char *) pdata , "2002:01:24 17:35:30"); /* get date/time from RTC */
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    case IFD0_TAG_YCBCRPOS:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 2); /* cosite */
        break;
    case IFD0_MTK_IMGINDEX:     // new definition: the index of continuous shot image. (1~n)
    case IFD0_MTK_BESTFOCUSH:   // new definition: focus value (H) for best shot.
    case IFD0_MTK_BESTFOCUSL:   // new definition: focus value (L) for best shot.
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 0);
        break;
    case IFD0_MTK_GROUPID:      // new definition: group ID for continuous shot.
    case IFD0_TAG_EXIFPTR:
    case IFD0_TAG_GPSINFO:
        pifd->type = IFD_DATATYPE_LONG;
        pifd->count = 1;
        break;        
    default:
        err = LIBEXIF_IFD_ERR0002;
        break;
    }

    exifErrPrint((unsigned char *) "ifdZeroIFDValInit", err);

    return err;
}

/*******************************************************************************
*
********************************************************************************/
unsigned int 
ifdExifIFDValInit(
    ifdNode_t *pnode, 
    struct exifIFDList *plist
)
{
    unsigned int err = 0;
    unsigned char *pdata;
    IFD_t *pifd;
    unsigned int idx = 0;
    /*unsigned char timeBuf[20];*/
    
    pdata = plist->valBuf + plist->valBufPos;
    pifd = &pnode->ifd;
    
    while (idx < plist->nodeCnt) {
        if (plist->ifdNodePool[idx].ifd.tag == pifd->tag) {
            err = LIBEXIF_IFD_ERR0005;
            return err;
        }
        idx ++;
    }

    switch (pifd->tag) {        
    case EXIF_TAG_EXPTIME:
    case EXIF_TAG_FNUM:
    case EXIF_TAG_COMPRESSBPP:
    /*case EXIF_TAG_EXPBIAS: */
    case EXIF_TAG_FOCALLEN:
    case EXIF_TAG_MAXAPTURE:
        pifd->type = IFD_DATATYPE_RATIONAL;
        pifd->count = 1;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += (sizeof(unsigned int) << 1);
        pdata += (sizeof(unsigned int) << 1);
        break;
    case EXIF_TAG_EXPBIAS:
        pifd->type = IFD_DATATYPE_SRATIONAL;
        pifd->count = 1;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += (sizeof(INT32) << 1);
        pdata += (sizeof(INT32) << 1);
        break;
    case EXIF_TAG_USRCOMMENT:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 256;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += 256;
        pdata += 256;
        break;
    case EXIF_TAG_EXPPROG:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 2);/* normal mode */
        break;
    case EXIF_TAG_ISOSPEEDRATE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 0x64);
        break;
    case EXIF_TAG_EXIFVER:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 4;
        memcpy(&pifd->valoff, exifVersion, 4); /* No null for termination */
        break;
    case EXIF_TAG_DATETIMEORIG:
    case EXIF_TAG_DATETIMEDITI:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 20;
        pifd->valoff = plist->valBufPos;
        #if 0
        /*exifAscRTCGet(timeBuf);*/
        strncpy(&timeBuf[10], " ", 1);
        strcpy(pdata, timeBuf);
        #else
        strcpy((char *) pdata , "2002:01:24 17:35:30"); 
        #endif
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    case EXIF_TAG_COMPCONFIGURE:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 4;
        write32((unsigned char*)&pifd->valoff, 0x00030201);
        break;
    case EXIF_TAG_METERMODE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff , 2);/* CenterWeightedAverage */
        break;
    #if 0   
    case EXIF_TAG_AUDIOFILE:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 13;
        pifd->valoff = plist->valBufPos;
/*      strcpy(pdata, "DSC_0047.WAV");*/
        plist->valBufPos += pifd->count + 1;
        pdata += pifd->count;
        break;
    #endif  
    case EXIF_TAG_ITOPIFDPTR:
        pifd->type = IFD_DATATYPE_LONG;
        pifd->count = 1;
        write32((unsigned char*)&pifd->valoff, 0x00000000);
        break;
    case EXIF_TAG_LIGHTSOURCE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 2);
        break;
    case EXIF_TAG_FLASH:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 0); /* strobe return light detected */
        break;
    case EXIF_TAG_FLRESHPIXVER:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 4;
        memcpy((unsigned char*)&pifd->valoff, "0100", 4); /* No null for termination */
        break;
    case EXIF_TAG_COLORSPACE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 1); /*srgb */
        break;
    case EXIF_TAG_PEXELXDIM:
    case EXIF_TAG_PEXELYDIM:
        pifd->type = IFD_DATATYPE_LONG;
        pifd->count = 1;
        write32((unsigned char*)&pifd->valoff, 1024); /*srgb */
        break;
    /*case IDF_EXIF_INTEROPIFDPTR:
        break;*/
    case EXIF_TAG_FILESOURCE:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 3); /* DSC */
        break;
    case EXIF_TAG_SENCETYPE:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 1); /* directly photographed */
        break;
    case EXIF_TAG_DIGITALZOOMRATIO:
        pifd->type = IFD_DATATYPE_RATIONAL;
        pifd->count = 1;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += (sizeof(unsigned int) << 1);
        pdata += (sizeof(unsigned int) << 1);
        break;
    case EXIF_TAG_SCENECAPTURETYPE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 0); /*scenecapturetype*/
        break;
    case EXIF_TAG_EXPOSUREMODE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 0); /*exposureMode*/
        break;
    case EXIF_TAG_WHITEBALANCEMODE:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 0); /*whiteBalanceMode*/
        break;
    default:
        err = LIBEXIF_IFD_ERR0002;
        break;
    }

    exifErrPrint((unsigned char *) "ifdExifIFDValInit", err);
    
    return err;
}

/*******************************************************************************
*
********************************************************************************/
unsigned int 
ifdGpsIFDValInit(
    ifdNode_t *pnode, 
    struct gpsIFDList *plist
)
{
    unsigned int err = 0;
    unsigned char *pdata;
    IFD_t *pifd;
    unsigned int idx = 0;
    /*unsigned char timeBuf[20];*/
    
    pdata = plist->valBuf + plist->valBufPos;
    pifd = &pnode->ifd;
    
    while (idx < plist->nodeCnt) {
        if (plist->ifdNodePool[idx].ifd.tag == pifd->tag) {
            err = LIBEXIF_IFD_ERR0005;
            return err;
        }
        idx ++;
    }

    switch (pifd->tag) {
    case GPS_TAG_VERSIONID:
        pifd->type = IFD_DATATYPE_BYTE;
        pifd->count = 4;
        memcpy(&pifd->valoff, gpsVersion, 4); /* No null for termination */
        break;
    case GPS_TAG_ALTITUDEREF:
        pifd->type = IFD_DATATYPE_BYTE;
        pifd->count = 1;
        pifd->valoff = 0;
        break;
    case GPS_TAG_LATITUDEREF:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 2;
        memcpy(&pifd->valoff, "N", 2); // Give default value
        break;
    case GPS_TAG_LONGITUDEREF:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 2;
        memcpy(&pifd->valoff, "E", 2); // Give default value
        break;        
    case GPS_TAG_LATITUDE:
    case GPS_TAG_LONGITUDE:
    case GPS_TAG_TIMESTAMP:
        pifd->type = IFD_DATATYPE_RATIONAL;
        pifd->count = 3;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += (sizeof(unsigned int) << 1) * 3;
        pdata += (sizeof(unsigned int) << 1) * 3;
        break;
    case GPS_TAG_ALTITUDE:
        pifd->type = IFD_DATATYPE_RATIONAL;
        pifd->count = 1;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += (sizeof(unsigned int) << 1) * 1;
        pdata += (sizeof(unsigned int) << 1) * 1;
        break;
    case GPS_TAG_PROCESSINGMETHOD:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 64;
        pifd->valoff = plist->valBufPos;
        plist->valBufPos += 64;
        pdata += 64;    
        break;
    case GPS_TAG_DATESTAMP:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->valoff = plist->valBufPos;
        pifd->count = 11;
        plist->valBufPos += pifd->count;
        pdata += pifd->count;
        break;
    default:
        err = LIBEXIF_IFD_ERR0002;
        break;
    }

    exifErrPrint((unsigned char *) "ifdGpsIFDValInit", err);
    
    return err;
}

/*******************************************************************************
*
********************************************************************************/
unsigned int 
ifdFirstIFDValInit(
    ifdNode_t *pnode, 
    struct firstIFDList *plist
)
{   
    unsigned int err = 0;
    unsigned char* pdata;
    IFD_t* pifd;
    unsigned int idx = 0;

    pdata = plist->valBuf + plist->valBufPos;
    pifd = &pnode->ifd;
    
    while (idx < plist->nodeCnt) {
        if (plist->ifdNodePool[idx].ifd.tag == pifd->tag) {
            err = LIBEXIF_IFD_ERR0005;
            return err;
        }
        idx ++;
    }
    
    switch (pifd->tag) {
    case IFD1_TAG_COMPRESS:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 6);/* JPEG thumbnail compress */
        break;
    case IFD1_TAG_ORIENT:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 1); /* no rotatation */
        break;
    case IFD1_TAG_XRES:
    case IFD1_TAG_YRES:
        pifd->type = IFD_DATATYPE_RATIONAL;
        pifd->count = 1;
        pifd->valoff = plist->valBufPos;
        write32(pdata , 0x48);
        pdata += 4;
        write32(pdata , 0x01);
        pdata += 4;
        plist->valBufPos += (sizeof(unsigned int) << 1);
        break;
    case IFD1_TAG_RESUINT:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 2); /* inches */
        break;
    case IFD1_TAG_JPG_INTERCHGFMT: /*thumbnail offset from TIFF header */
        pifd->type = IFD_DATATYPE_LONG;
        pifd->count = 1;
        break;
    case IFD1_TAG_JPG_INTERCHGFMTLEN: /*thumbnail length (from SOI to EOI) */
        pifd->type = IFD_DATATYPE_LONG;
        pifd->count = 1;
        break;
    case IFD1_TAG_YCBCRPOS:
        pifd->type = IFD_DATATYPE_SHORT;
        pifd->count = 1;
        write16((unsigned char*)&pifd->valoff, 2);/* cosite */
        break;
    default:
        err = LIBEXIF_IFD_ERR0002;
        break;
    }
    
    exifErrPrint((unsigned char *) "ifdFirstIFDValInit", err);
    
    return err;
}

/*******************************************************************************
*
********************************************************************************/
unsigned int 
ifdItopIFDValInit(
    ifdNode_t *pnode, 
    struct itopIFDList *plist
)
{   
    unsigned int err = 0;
    unsigned char* pdata;
    IFD_t* pifd;
    unsigned int idx = 0;

    pdata = plist->valBuf + plist->valBufPos;
    pifd = &pnode->ifd;
    
    while (idx < plist->nodeCnt) {
        if (plist->ifdNodePool[idx].ifd.tag == pifd->tag) {
            err = LIBEXIF_IFD_ERR0005;
            return err;
        }
        idx ++;
    }

    switch (pifd->tag) {
    case ITOP_TAG_ITOPINDEX:
        pifd->type = IFD_DATATYPE_ASCII;
        pifd->count = 4;
        strcpy((char *)&pifd->valoff, "R98\0");/* JPEG thumbnail compress */
        break;
    case ITOP_TAG_ITOPVERSION:
        pifd->type = IFD_DATATYPE_UNDEFINED;
        pifd->count = 4;
        memcpy((unsigned char*)&pifd->valoff, "0100", 4); /* No null for termination */
        break;
    default:
        err = LIBEXIF_IFD_ERR0002;
        break;
    }
    
    exifErrPrint((unsigned char *) "ifditopIFDValInit", err);
    
    return err;
}


