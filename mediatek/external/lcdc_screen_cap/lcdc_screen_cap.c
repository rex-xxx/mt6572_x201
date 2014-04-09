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

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>

#include <linux/fb.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <cutils/properties.h>

#include "mtkfb.h"
#define LOG_TAG "lcdc_cap"
#include <utils/Log.h>
#define FBIOLOCK_FB             0x4630
#define FBIOUNLOCK_FB           0x4631
#define FBIOLOCKED_IOCTL        0x4632

int vinfoToPixelFormat(struct fb_var_screeninfo* vinfo,
                              uint32_t* bytespp, uint32_t* f)
{

    switch (vinfo->bits_per_pixel) {
        case 16:
            *f = 4; //PIXEL_FORMAT_RGB_565
            *bytespp = 2;
            break;
        case 24:
            *f = 3; //PIXEL_FORMAT_RGB_888
            *bytespp = 3;
            break;
        case 32:
            // TODO: do better decoding of vinfo here
            //*f = PIXEL_FORMAT_RGBX_8888;
            *f = 5; //PIXEL_FORMAT_BGRA_8888
            *bytespp = 4;
            break;
        default:
            return -1;
    }
    return 0;
}

int rotate90(uint32_t w,uint32_t h,int f,uint32_t bytespp,void *base,size_t size,int capture_buffer_size,int fd)
{
    uint32_t w_r,h_r;
    uint32_t x,y;
    void *base_r = NULL;
    uint32_t r_pointer,w_pointer;

    w_r = h;
    h_r = w;
	
    base_r = malloc(capture_buffer_size);

    if(base_r == NULL)
    {
        ALOGE("[DDMSCap]alloc size 0x%08x failed", capture_buffer_size);
        return -1;
    }

    w_pointer = (uint32_t)base_r;

    switch(bytespp)
    {
        case 2:
        {
            for(y=0; y<h_r; y++)
              for(x=0; x<w_r; x++)
              {
                  r_pointer = (uint32_t)base + (x * w + (w - y - 1)) * 2;
  
                  *((uint16_t *)w_pointer) = *((uint16_t *)r_pointer);
  
                  w_pointer += 2;
              }
        }
        break;
        case 3:
        {
            for(y=0; y<h_r; y++)
                for(x=0; x<w_r; x++)
                {
                    r_pointer = (uint32_t)base + (x * w + (w - y - 1)) * 3;

                    *((uint16_t *)w_pointer) = *((uint16_t *)r_pointer);
                    *((uint8_t *)(w_pointer+2)) = *((uint8_t *)(r_pointer+2));

                    w_pointer += 3;
                }
        }
        break;
        case 4:
        {
            for(y=0; y<h_r; y++) 
                for(x=0; x<w_r; x++)
                {
                    r_pointer = (uint32_t)base + (x * w + (w - y - 1)) * 4;

                    *((uint32_t *)w_pointer) = *((uint32_t *)r_pointer);

                    w_pointer += 4;
            	}
        }
        break;
    }
		
    write(fd, &w_r, 4);
    write(fd, &h_r, 4);
    write(fd, &f, 4);
    write(fd, base_r, size);
    
    free(base_r);   
    
    return 0;        
}


int rotate180(uint32_t w,uint32_t h,int f,uint32_t bytespp,void *base,size_t size,int capture_buffer_size,int fd)
{
        uint32_t w_r,h_r;
        uint32_t x,y;
        void *base_r = NULL;
        uint32_t r_pointer,w_pointer;
        
        w_r = w;
        h_r = h;
        
        base_r = malloc(capture_buffer_size);
        
        if(base_r == NULL)
        {
            ALOGE("[DDMSCap]alloc size 0x%08x failed", capture_buffer_size);
            return -1;
        }
        
        w_pointer = (uint32_t)base_r;
        
        switch(bytespp)
        {
            case 2:
            {
                for(y=0; y<h_r; y++)
                    for(x=0; x<w_r; x++)
                    {
                        r_pointer = (uint32_t)base + ((h - y - 1) * w + (w - x - 1)) * 2;

                        *((uint16_t *)w_pointer) = *((uint16_t *)r_pointer);
    					
                        w_pointer += 2;
                    }
            }
            break;
            case 3:
            {
                for(y=0; y<h_r; y++)
                    for(x=0; x<w_r; x++)
                    {
                        r_pointer = (uint32_t)base + ((h - y - 1) * w + (w - x - 1)) * 3;

                        *((uint16_t *)w_pointer) = *((uint16_t *)r_pointer);
                        *((uint8_t *)(w_pointer+2)) = *((uint8_t *)(r_pointer+2));
									
                        w_pointer += 3;
                    }
			}
            break;
            case 4:
            {
                for(y=0; y<h_r; y++)
                    for(x=0; x<w_r; x++)
                    {
                    r_pointer = (uint32_t)base + ((h - y - 1) * w + (w - x - 1)) * 4;
        
                    *((uint32_t *)w_pointer) = *((uint32_t *)r_pointer);
        										
                    w_pointer += 4;
                    }
            }
            break;
        }

    ALOGI("[DDMSCap]rotate 180 finished, begin write to fd\n");
    write(fd, &w_r, 4);
    write(fd, &h_r, 4);
    write(fd, &f, 4);
    ALOGI("[DDMSCap]write header finished, will write size:%d from 0x%08x\n", size, base_r);
    write(fd, base_r, size);
    ALOGI("[DDMSCap]write finished\n");
    
    free(base_r);    
    
    return 0;       
}


int rotate270(uint32_t w,uint32_t h,int f,uint32_t bytespp,void *base,size_t size,int capture_buffer_size,int fd)
{
    uint32_t w_r,h_r;
    uint32_t x,y;
    void *base_r = NULL;
    uint32_t r_pointer,w_pointer;

    w_r = h;
    h_r = w;
		
    base_r = malloc(capture_buffer_size);
		
    if(base_r == NULL)
    {
    ALOGE("[DDMSCap]alloc size 0x%08x failed", capture_buffer_size);
    return -1;
    }
		
    w_pointer = (uint32_t)base_r;
		
    switch(bytespp)
    {
        case 2:
        {
            for(y=0; y<h_r; y++)
                for(x=0; x<w_r; x++)
                {
                    r_pointer = (uint32_t)base + ((h - x - 1) * w + y) * 2;
                
                    *((uint16_t *)w_pointer) = *((uint16_t *)r_pointer);
                					
                    w_pointer += 2;
                }
        }
        break;
        case 3:
        {
            for(y=0; y<h_r; y++)
                for(x=0; x<w_r; x++)
                {
                    r_pointer = (uint32_t)base + ((h - x - 1) * w + y) * 3;
                    					
                    *((uint16_t *)w_pointer) = *((uint16_t *)r_pointer);
                    *((uint8_t *)(w_pointer+2)) = *((uint8_t *)(r_pointer+2));
                    w_pointer += 3;
                }
        }
        break;
        case 4:
        {
            for(y=0; y<h_r; y++)
                for(x=0; x<w_r; x++)
                {
                    r_pointer = (uint32_t)base + ((h - x - 1) * w + y) * 4;
                					
                    *((uint32_t *)w_pointer) = *((uint32_t *)r_pointer);
                															
                    w_pointer += 4;
                }
        }
        break;
    }
		
    write(fd, &w_r, 4);
    write(fd, &h_r, 4);
    write(fd, &f, 4);
    write(fd, base_r, size);
    
    free(base_r);  
    
    return 0;         
}

void main(void)
{
    char property[PROPERTY_VALUE_MAX];
    int step = 0;
    int fd = dup(STDOUT_FILENO);
    unsigned long fb_lock[2]   = {MTKFB_LOCK_FRONT_BUFFER,   (unsigned long)NULL};
    unsigned long fb_unlock[2] = {MTKFB_UNLOCK_FRONT_BUFFER, (unsigned long)NULL};
    unsigned long fb_capture[2] = {MTKFB_CAPTURE_FRAMEBUFFER, (unsigned long)NULL};
    void *base = NULL, *base_align = NULL;
    int capture_buffer_size = 0, capture_buffer_size_align = 0;
    struct fb_var_screeninfo vinfo;
    int fb;
    uint32_t bytespp;
    uint32_t w, h, f;
    size_t size = 0;
	ALOGI("Enter lcdc_screen_cap.");
    if (0 > (fb = open("/dev/graphics/fb0", O_RDONLY))) goto done;
    ALOGI("[DDMSCap]Open /dev/graphics/fb0\n");
    fcntl(fb, F_SETFD, FD_CLOEXEC);
    if(ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) < 0) goto done;
    ALOGI("[DDMSCap]FBIOGET_VSCREENINFO\n");
    if(ioctl(fb, FBIOLOCK_FB, NULL) < 0) goto done;
    ALOGI("[DDMSCap]FBIOLOCK_FB\n");
    ++ step; //1
    if(ioctl(fb, FBIOLOCKED_IOCTL, fb_lock) < 0) goto done;
    ALOGI("[DDMSCap]FBIOLOCKED_IOCTL\n");
    ++ step; //2

    if (vinfoToPixelFormat(&vinfo, &bytespp, &f) == 0) 
    {
        w = vinfo.xres;
        h = vinfo.yres;
        size = w * h * bytespp;
        ALOGI("[DDMSCap]screen_width = %d, screen_height = %d, bpp = %d, format = %d, size = %d\n", w, h, bytespp, f, size);
    }
    {
        capture_buffer_size = w * h * bytespp;
        capture_buffer_size_align = capture_buffer_size + 32; //for M4U 32-byte alignment
        base_align = malloc(capture_buffer_size_align);

        if(base_align == NULL)
        {
            ALOGE("[DDMSCap]pmem_alloc size 0x%08x failed", capture_buffer_size_align);
            goto done;
        }
        else
        {
            ALOGE("[DDMSCap]pmem_alloc size = 0x%08x, addr = 0x%08x", capture_buffer_size_align, base_align);
        }

        base = (void *)((unsigned long)base_align + 32 - ((unsigned long)base_align & 0x1F)); //for M4U 32-byte alignment
        ALOGI("[DDMSCap]pmem_alloc base = 0x%08x", base);
        fb_capture[1] = (unsigned long)&base;
    	ALOGI("[DDMSCap]start capture\n");
        if(ioctl(fb, FBIOLOCKED_IOCTL, fb_capture) < 0)
        {
            ALOGE("[DDMSCap]ioctl of MTKFB_CAPTURE_FRAMEBUFFER fail\n");
            goto done;
        }

    	ALOGI("[DDMSCap]capture finished\n");
        if (step > 1) 
            ioctl(fb, FBIOLOCKED_IOCTL, fb_unlock);
        if (step > 0) 
            ioctl(fb, FBIOUNLOCK_FB, NULL);

    	ALOGI("[DDMSCap]unlock FB Driver\n");
        ++ step; //3
    }

    if (base) 
    {
        int displayOrientation = 0;
        
        if (property_get("ro.sf.hwrotation", property, NULL) > 0) {
	    displayOrientation = atoi(property);
	}    	

        switch(displayOrientation)
        {
            case 0:
            //case 180:
            {
                write(fd, &w, 4);
                write(fd, &h, 4);
                write(fd, &f, 4);
                write(fd, base, size);
            }
            break;

            case 90:
          	    if(rotate90(w,h,f,bytespp,base,size,capture_buffer_size,fd) < 0)
          			goto done;
        	break;

        	case 180:
    		ALOGI("[DDMSCap]rotate 180 degree\n");
      		if(rotate180(w,h,f,bytespp,base,size,capture_buffer_size,fd) < 0)
      			goto done;
        	break;
        	
        	case 270:
          		if(rotate270(w,h,f,bytespp,base,size,capture_buffer_size,fd) < 0)
          			goto done;
        	break;
        	
        }
    }
done:

    if (NULL != base_align)
        free(base_align);
    if (step < 3)
    {
        if (step > 1) 
            ioctl(fb, FBIOLOCKED_IOCTL, fb_unlock);
        if (step > 0) 
            ioctl(fb, FBIOUNLOCK_FB, NULL);
    }
    if(fb >= 0)  close(fb);
    close(fd);
	
   	ALOGI("[DDMSCap]all capture procedure finished\n");
    return 0;
}

