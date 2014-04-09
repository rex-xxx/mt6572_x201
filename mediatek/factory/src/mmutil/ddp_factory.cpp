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

#define LOG_TAG "DDP"

#include <stdio.h>



#include <DpDataType.h>
#include "DpBlitStream.h"



#include <string.h>         //memcpy
#include <errno.h>          //strerror
#include <sys/time.h>       //gettimeofday


typedef struct
{
    unsigned    width;
    unsigned    height;
    char*       buff_addr;

} Buffer_t;

Buffer_t    g_ImageBuffer[4];
#define     IMG_COUNT   ( sizeof(g_ImageBuffer)/sizeof(Buffer_t) )

#define IMG_WIDTH   (640)
#define IMG_HEIGH   (480)



int DdpFactoryModeSingleTest(void * a_pArg)
{
    DpBlitStream blit_stream;
    
    // fill parameter
    int src_width  = g_ImageBuffer[0].width;
    int src_height = g_ImageBuffer[0].height;
    int src_pitch  = g_ImageBuffer[0].width;
    int dst_width  = g_ImageBuffer[1].width;
    int dst_height = g_ImageBuffer[1].height;
    int dst_pitch  = g_ImageBuffer[1].width;
    int src_format = (int)eABGR8888;

    const unsigned int src_size_base = src_width * src_height;
    unsigned int src_addr_list[3] = {0, 0, 0};
    unsigned int src_size_list[3] = {0, 0, 0};
    unsigned int src_plane_num = 1;
    
    unsigned int dst_addr_list[3] = {0, 0, 0};
    unsigned int dst_size_list[3] = {0, 0, 0};    
    unsigned int dst_plane_num = 1;
    
    DpInterlaceFormat src_interlace = eInterlace_None;
    DpInterlaceFormat dst_interlace = eInterlace_None;

    DpColorFormat dst_format = eABGR8888;
    
    // input buffer configuration
/*    int ion_fd = m_layer->handle->ionFd;
    if (-1 != ion_fd)
    {
        blit_stream.setSrcBuffer(ion_fd, src_size_list, src_plane_num);
    }
    else
*/    

    {
        src_addr_list[0] = (unsigned int)g_ImageBuffer[0].buff_addr;
        src_addr_list[1] = 0;
        src_addr_list[2] = 0;

        src_size_list[0] = 4 * src_size_base;
        src_size_list[1] = 0;
        src_size_list[2] = 0;

        dst_addr_list[0] = (unsigned int)g_ImageBuffer[1].buff_addr;
        dst_addr_list[1] = 0;
        dst_addr_list[2] = 0;

        dst_size_list[0] = 4 * src_size_base;
        dst_size_list[1] = 0;
        dst_size_list[2] = 0;

//        DPFD_LOGD("src_addr_list[0] = 0x%08X(kernel)\n", (int)src_addr_list[0]);
//        DPFD_LOGD("src_addr_list[1] = 0x%08X(kernel)\n", (int)src_addr_list[1]);
//        DPFD_LOGD("src_addr_list[2] = 0x%08X(kernel)\n", (int)src_addr_list[2]);
        
        blit_stream.setSrcBuffer((void**) src_addr_list, src_size_list, src_plane_num);
    }

    blit_stream.setSrcConfig(src_width, src_height,
                               (DpColorFormat)src_format,
                               src_interlace);

    blit_stream.setRotate(0);

    // display buffer configuration
    blit_stream.setDstBuffer((void**)dst_addr_list, dst_size_list, dst_plane_num);

    // DpBlitStream only fill dst from (0, 0)
    blit_stream.setDstConfig(dst_width, dst_height,
                               dst_format, 
                               dst_interlace);

    if (!blit_stream.invalidate())
    {
        printf("blit stream fail\n");
        return false;
    };

#if 0    
    printf("src[%d, %d]=(%d, %d, %d, %d)\n", 0, 0,
        g_ImageBuffer[0].buff_addr[0*IMG_WIDTH+0],
        g_ImageBuffer[0].buff_addr[0*IMG_WIDTH+1],
        g_ImageBuffer[0].buff_addr[0*IMG_WIDTH+2],
        g_ImageBuffer[0].buff_addr[0*IMG_WIDTH+3]);

    printf("dst[%d, %d]=(%d, %d, %d, %d)\n", 0, 0,
        g_ImageBuffer[1].buff_addr[0*IMG_WIDTH+0],
        g_ImageBuffer[1].buff_addr[0*IMG_WIDTH+1],
        g_ImageBuffer[1].buff_addr[0*IMG_WIDTH+2],
        g_ImageBuffer[1].buff_addr[0*IMG_WIDTH+3]);

    printf("src[%d, %d]=(%d, %d, %d, %d)\n", IMG_WIDTH-1, IMG_HEIGH-1,
        g_ImageBuffer[0].buff_addr[4*IMG_HEIGH*IMG_WIDTH-4],
        g_ImageBuffer[0].buff_addr[4*IMG_HEIGH*IMG_WIDTH-3],
        g_ImageBuffer[0].buff_addr[4*IMG_HEIGH*IMG_WIDTH-2],
        g_ImageBuffer[0].buff_addr[4*IMG_HEIGH*IMG_WIDTH-1]);

    printf("dst[%d, %d]=(%d, %d, %d, %d)\n", IMG_WIDTH-1, IMG_HEIGH-1,
        g_ImageBuffer[1].buff_addr[4*IMG_HEIGH*IMG_WIDTH-4],
        g_ImageBuffer[1].buff_addr[4*IMG_HEIGH*IMG_WIDTH-3],
        g_ImageBuffer[1].buff_addr[4*IMG_HEIGH*IMG_WIDTH-2],
        g_ImageBuffer[1].buff_addr[4*IMG_HEIGH*IMG_WIDTH-1]);
#endif

    return 0;
}


#define DDP_TEST_LOOP_TIMEOUT   5000000     //5 sec
//#define DDP_TEST_LOOP_TIMEOUT   600000000   //10mins


int DdpFactoryModeTest(void * a_pArg)
{
    struct timeval t1 , t2;
    unsigned long time1, time2;
    int ret = 0;
    int i, j;
    char* psrc;
    
    memset(g_ImageBuffer, 0, sizeof( g_ImageBuffer ) );

    /*Allocate Image Memory*/
    for( i = 0; i < (int)IMG_COUNT; i++ )
    {
        g_ImageBuffer[i].width = IMG_WIDTH;
        g_ImageBuffer[i].height = IMG_HEIGH;
        g_ImageBuffer[i].buff_addr = (char*)malloc( IMG_WIDTH * IMG_HEIGH * 4 );

        if( g_ImageBuffer[i].buff_addr == NULL )
        {
            printf("[DDP] g_ImageBuffer[%d] memory allocate failed!\n", i );
            ret = -1;
            goto exit_test_loop;
        }
    }

    psrc = (char*)g_ImageBuffer[0].buff_addr;
    for (j = 0; j < IMG_HEIGH; j++)
    {
        for (i = 0; i < IMG_WIDTH; i++)
        {
            if (j < IMG_HEIGH/2)
            {
                psrc[4*(j*IMG_WIDTH+i)+0] = 255;
                psrc[4*(j*IMG_WIDTH+i)+1] = 0;
                psrc[4*(j*IMG_WIDTH+i)+2] = 0;
                psrc[4*(j*IMG_WIDTH+i)+3] = 255;
            }
            else
            {
                psrc[4*(j*IMG_WIDTH+i)+0] = 0;
                psrc[4*(j*IMG_WIDTH+i)+1] = 255;
                psrc[4*(j*IMG_WIDTH+i)+2] = 0;
                psrc[4*(j*IMG_WIDTH+i)+3] = 255;
            }
        }
    }
        
    i = 0;

    gettimeofday(&t1,NULL);
    time1 = (t1.tv_sec*1000000 + t1.tv_usec);

    while( 1 )
    {
        printf("[DDP] DDP Test Loop (%d).\n", i );
        if( DdpFactoryModeSingleTest(a_pArg) < 0 )
        {
            printf("[DDP ERROR] DDP Test Loop (%d) failed!\n", i );
            ret = -1;
            goto exit_test_loop;
        }

        i++;

        gettimeofday(&t2,NULL);
        time2 = (t2.tv_sec*1000000 + t2.tv_usec);
        
        if(time2 > time1 + DDP_TEST_LOOP_TIMEOUT )
        {
            printf("[DDP] DDP Test Loop time up (%dus)!\n", DDP_TEST_LOOP_TIMEOUT );
            break;
        }
    }



exit_test_loop:

    /*Free memory*/
    for( i = 0; i < (int)IMG_COUNT; i++ )
    {
        if( g_ImageBuffer[i].buff_addr != NULL )
            free( (void*)g_ImageBuffer[i].buff_addr );
    } 

    if( ret == 0 )
    {
        printf("[DDP] All Test Pass!\n");
    }else
    {
        printf("[DDP] Test Failed!\n");
    }


    return 0;
}





















