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

#include <sys/mman.h>

#include <dlfcn.h>

#include <cutils/ashmem.h>
#include <cutils/log.h>

#include <hardware/hardware.h>
#include <hardware/gralloc.h>

#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <string.h>
#include <stdlib.h>
#include <sched.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <linux/fb.h>

#include <cutils/log.h>
#include <cutils/atomic.h>
#include "mtkfb.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "zlib.h"
#include "cust_display.h"
#include <sys/reboot.h>
#include "ipodmain.h"


#include <ui/Rect.h>
#include <ui/Region.h>
#include <ui/DisplayInfo.h>

#include <private/gui/LayerState.h>


#include <binder/IPCThreadState.h>

#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>
#include <gui/ISurfaceComposer.h> 

#include <utils/RefBase.h>
#include <utils/StrongPointer.h>



using namespace android;

sp<SurfaceComposerClient> client;
sp<SurfaceControl> surfaceControl;

// data structure to access surface content
Surface::SurfaceInfo info;
sp<Surface>          surface;
DisplayInfo dinfo;

int dinfo_width,dinfo_height;
int old_dinfo_Orientation;

#define ALIGN_TO(x, n)  \
    (((x) + ((n) - 1)) & ~((n) - 1))
    
typedef enum {
   DISP_VERTICAL_PROG_BAR = 0,
   DISP_HORIZONTAL_PROG_BAR,
} DISP_PROG_BAR_DIRECT;


const char LOGO_PATH[] = "/system/media/images/boot_logo";

#define RGB565_TO_ARGB8888(x)   \
    ((((x) &   0x1F) << 3) |    \
     (((x) &  0x7E0) << 5) |    \
     (((x) & 0xF800) << 8) |    \
     (0xFF << 24)) // opaque

#define LOGO_BUFFER_SIZE    0x300000
static const unsigned int VOLTAGE_SCALE = 4;

typedef struct {
    unsigned int left, top, right, bottom;
} RECT;

static RECT bar_rect = {BAR_LEFT, BAR_TOP, BAR_RIGHT, BAR_BOTTOM};

/*
static unsigned int bar_occupied_color = RGB565_TO_ARGB8888(BAR_OCCUPIED_COLOR);
static unsigned int bar_empty_color    = RGB565_TO_ARGB8888(BAR_EMPTY_COLOR);
static unsigned int bar_bg_color       = RGB565_TO_ARGB8888(BAR_BG_COLOR);
*/
static unsigned int bar_occupied_color = BAR_OCCUPIED_COLOR;
static unsigned int bar_empty_color    = BAR_EMPTY_COLOR;
static unsigned int bar_bg_color       = BAR_BG_COLOR;

static int fb_fd = 0;


static unsigned int *logo_addr = NULL;

unsigned short *rgb565_logo = NULL;

static unsigned int *fb_addr = NULL;
static unsigned int logo_fd = 0;

// use double fb address
static unsigned int *front_fb_addr = NULL;
static unsigned int *back_fb_addr = NULL;
unsigned int use_double_addr = 0;



static struct fb_var_screeninfo vinfo;
static struct fb_fix_screeninfo finfo;

// test code
//static unsigned int x_virtual = 480;
//static unsigned int fb_size = 480*800*4;
//static unsigned int fb_size = 153600;

static unsigned int x_virtual = 0;
static unsigned int fb_size = 0;

// for new
#define UINT32 unsigned int
#define UINT16 unsigned short
#define UINT8 unsigned char

#define INT32 int

UINT32 animation_index = 0;
int charging_index = -1;
static char animation_addr[82*16*2] = {0x0};
int surfaceRectIndex = 0;


static int number_pic_width = NUMBER_RIGHT - NUMBER_LEFT;       //width
static int number_pic_height = NUMBER_BOTTOM - NUMBER_TOP;       //height
int number_pic_size = (NUMBER_RIGHT - NUMBER_LEFT)*(NUMBER_BOTTOM - NUMBER_TOP)*2;   //size
char number_pic_addr[(NUMBER_RIGHT - NUMBER_LEFT)*(NUMBER_BOTTOM - NUMBER_TOP)*2] = {0x0}; //addr
RECT number_location_rect = {NUMBER_LEFT,NUMBER_TOP,NUMBER_RIGHT,NUMBER_BOTTOM};
static int number_pic_start_0 = 4;
static int number_pic_percent = 14;


static int line_width = CAPACITY_RIGHT - CAPACITY_LEFT;
static int line_height = 1;
int line_pic_size = (TOP_ANIMATION_RIGHT - TOP_ANIMATION_LEFT)*2;
char line_pic_addr[(TOP_ANIMATION_RIGHT - TOP_ANIMATION_LEFT)*2] = {0x0};
RECT battery_rect = {CAPACITY_LEFT,CAPACITY_TOP,CAPACITY_RIGHT,CAPACITY_BOTTOM};


static int percent_pic_width = PERCENT_RIGHT - PERCENT_LEFT;
static int percent_pic_height = PERCENT_BOTTOM - PERCENT_TOP;
int percent_pic_size = (PERCENT_RIGHT - PERCENT_LEFT)*(PERCENT_BOTTOM - PERCENT_TOP)*2;
char percent_pic_addr[(PERCENT_RIGHT - PERCENT_LEFT)*(PERCENT_BOTTOM - PERCENT_TOP)*2] = {0x0};
RECT percent_location_rect = {PERCENT_LEFT,PERCENT_TOP,PERCENT_RIGHT,PERCENT_BOTTOM};

static int top_animation_width = TOP_ANIMATION_RIGHT - TOP_ANIMATION_LEFT;
static int top_animation_height = TOP_ANIMATION_BOTTOM - TOP_ANIMATION_TOP;
int  top_animation_size = (TOP_ANIMATION_RIGHT - TOP_ANIMATION_LEFT)*(TOP_ANIMATION_BOTTOM - TOP_ANIMATION_TOP)*2;
char top_animation_addr[(TOP_ANIMATION_RIGHT - TOP_ANIMATION_LEFT)*(TOP_ANIMATION_BOTTOM - TOP_ANIMATION_TOP)*2] = {0x0};
RECT top_animation_rect = {TOP_ANIMATION_LEFT,TOP_ANIMATION_TOP,TOP_ANIMATION_RIGHT,TOP_ANIMATION_BOTTOM};

int charging_low_index = 0;
int charging_animation_index = 0;
int firtstInitIndex = 0;

// if use old_logo set old_logo_bin=1,and show old animation
int old_logo_bin = 0;
// add for check old logo.bin
// for kpoc running mode running_kpoc_mode = 1   
// for IPO mode running_kpoc_mode = 0
int running_kpoc_mode = 0;

void show_animation_number(UINT32 index,UINT32 number_position);
void show_animation_line(UINT32 index,UINT32 capacity_grids);
void show_animation(UINT32 index, RECT rect, char* addr);
void mt65xx_disp_show_black_logo(void);

void cust_show_battery_capacity(unsigned int capacity);
void cust_show_battery_capacity_new(unsigned int capacity);

// get charging mode  : PARAM_CHARGING_MODE == 1 kpoc mode

void getChargingMode()
{
    running_kpoc_mode = 0;
    XLOGD("[ChargingAnimation %s %d]kpoc flag == 1 ? : params[PARAM_CHARGING_MODE] = %d \n ",__FUNCTION__,__LINE__ ,params[PARAM_CHARGING_MODE]);
    if(params[PARAM_CHARGING_MODE] == 1){
        running_kpoc_mode = 1;
        XLOGD("[ChargingAnimation %s %d]IPOD under kernel power off charging mode!",__FUNCTION__,__LINE__ );
    }
}
  // judge the logo bin
 void syncAnimationVersion()
{
    unsigned int logonum;
    unsigned int *db_addr = logo_addr;

    unsigned int *pinfo = (unsigned int*)db_addr;
    logonum = pinfo[0];
    XLOGD("[ChargingAnimation %s %d]pinfo[0]=0x%08x, pinfo[1]=0x%08x, pinfo[2]=%d\n", __FUNCTION__,__LINE__,
                pinfo[0], pinfo[1], pinfo[2]);
/*
    //old logo bin has only 5 logos, as new version has more than 27 logos
    // hide new animation and test the old aniamtion
#ifdef ANIMATION_NEW
#undef ANIMATION_NEW
    XLOGD("[ChargingAnimation %s %d]undefine ANIMATION_NEW:show old animation \n",__FUNCTION__,__LINE__); 
    XLOGD("[ChargingAnimation %s %d]BAR_LEFT=%d, BAR_RIGHT=%d \n",__FUNCTION__,__LINE__,(BAR_LEFT),(BAR_RIGHT)); 
    XLOGD("[ChargingAnimation %s %d]BAR_TOP=%d, BAR_BOTTOM=%d \n",__FUNCTION__,__LINE__,(BAR_TOP),(BAR_BOTTOM)); 
#endif
*/
#ifdef ANIMATION_NEW
   XLOGD("[ChargingAnimation %s %d]define ANIMATION_NEW:show new animation with capacity num\n",__FUNCTION__,__LINE__); 
   XLOGD("[ChargingAnimation %s %d]CAPACITY_LEFT =%d, CAPACITY_TOP =%d \n",__FUNCTION__,__LINE__,(CAPACITY_LEFT) ,(CAPACITY_TOP) ); 
   XLOGD("[ChargingAnimation %s %d]LCM_HEIGHT=%d, LCM_WIDTH=%d \n",__FUNCTION__,__LINE__,(CAPACITY_RIGHT),(CAPACITY_BOTTOM)); 
    if(logonum < 6)
    {
        XLOGD("[ChargingAnimation %s %d]logonum = %d,undefine ANIMATION_NEW:show old animation \n",__FUNCTION__,__LINE__,logonum); 
        old_logo_bin = 1;
    } else {

        old_logo_bin = 0;   
        XLOGD("[ChargingAnimation %s %d]logonum = %d,old_logo_bin = %d, logo.bin is new:show new animation \n",__FUNCTION__,__LINE__,logonum,old_logo_bin); 
    }
    
#else
    XLOGD("[ChargingAnimation %s %d]not define ANIMATION_NEW:show old animation \n",__FUNCTION__,__LINE__); 
#endif
}
void mt65xx_logo_init(void)
{
    // read and de-compress logo data here
    int fd = 0;
    int len = 0;
    int mtdid = 0;

    fd = open("/dev/logo", O_RDONLY);
    if(fd < 0)
    {
        XLOGD("[ChargingAnimation %s %d]open logo partition device file fail",__FUNCTION__,__LINE__ );
        return;
    }

    logo_addr = (unsigned int*)malloc(LOGO_BUFFER_SIZE);
    if(logo_addr == NULL)
    {
        XLOGD("[ChargingAnimation %s %d]allocate logo buffer fail, size=0x%08x \n",__FUNCTION__,__LINE__ ,LOGO_BUFFER_SIZE);
        goto error_reboot;
    }

    // (1) skip the image header
    len = read(fd, logo_addr, 512);
    if (len < 0)
    {

        XLOGD("[ChargingAnimation %s %d]read from logo addr for 512B is failed! \n",__FUNCTION__,__LINE__);
        goto error_reboot;
    }
    // get the image 
    len = read(fd, logo_addr, LOGO_BUFFER_SIZE);
    if (len < 0)
    {

        XLOGD("[ChargingAnimation %s %d]read from logo addr for buffer is failed! \n",__FUNCTION__,__LINE__);
        goto error_reboot;
    }
    close(fd);
    // judge the IPO /KPOC charging mode 
    
    XLOGD("[ChargingAnimation %s %d]getChargingMode \n",__FUNCTION__,__LINE__);
    getChargingMode();
    // test kpoc mode: use kpoc_mode---running_kpoc_mode =1, IPO_mode--- running_kpoc_mode = 0
    //running_kpoc_mode = 0;


    XLOGD("[ChargingAnimation %s %d]running_kpoc_mode = 1, run mode %d\n",__FUNCTION__,__LINE__,running_kpoc_mode);
    
    syncAnimationVersion();

    return;

error_reboot:
    close(fd);
    if (inCharging)
        reboot(RB_POWER_OFF);
    else //reboot
    {
        XLOGD("reboot after 3sec"); // to prevent interlace operation with MD reset
        sleep(3);
        reboot(LINUX_REBOOT_CMD_RESTART);
    }

}

void mt65xx_boot_logo_updater_init(void)
{

    int fd = -1;
    ssize_t rdsize = 0;

    unsigned int rgb565_logo_size = vinfo.xres * vinfo.yres * 2;
    // (3) open logo file

    if ((fd = open(LOGO_PATH, O_RDONLY)) < 0) {
        XLOGD("[ChargingAnimation %s %d]ailed to open logo file: %s\n", __FUNCTION__,__LINE__,LOGO_PATH);
        goto done;
    }

    // (5) copy the 2nd logo to surface info 

    rgb565_logo = (unsigned short *)malloc(rgb565_logo_size);
    if (!rgb565_logo) {
    XLOGD("[ChargingAnimation %s %d]allocate %d bytes memory for boot logo failed\n",__FUNCTION__,__LINE__,
        rgb565_logo_size);
    goto done;
    //reboot(LINUX_REBOOT_CMD_RESTART);
    }

    rdsize = read(fd, rgb565_logo, rgb565_logo_size);

    if (rdsize < (ssize_t)rgb565_logo_size) {
    XLOGD("[ChargingAnimation %s %d]logo file size: %ld bytes, while expected size: %d bytes\n", __FUNCTION__,__LINE__,
                rdsize, rgb565_logo_size);
    XLOGD("[ChargingAnimation %s %d]ERROR,rdsize=%d, rgb565_logo_size=%d \n",__FUNCTION__,__LINE__,rdsize,rgb565_logo_size);        
    goto done;
    }
    XLOGD("[ChargingAnimation %s %d]OK,rdsize=%d, rgb565_logo_size=%d \n",__FUNCTION__,__LINE__,rdsize,rgb565_logo_size);        

    done:

    if (fd >= 0) close(fd);
    return;

}

void mt65xx_boot_logo_updater_deinit(void)
{
    if (rgb565_logo) free(rgb565_logo);


}

void* mt65xx_get_boot_logo_updater_addr(void)
{
    if(rgb565_logo == NULL)
    {
        XLOGD("BOOT LOGO ADDR NULL\n");
    }
    return rgb565_logo;
}

void* mt65xx_get_fb_addr(void)
{
    return fb_addr;
}

void change_fb_addr(void)
{
    XLOGD("[ChargingAnimation %s %d]use_double_addr =%d \n",__FUNCTION__,__LINE__,use_double_addr);
    if(use_double_addr == 0) {
        use_double_addr++;
        fb_addr = front_fb_addr;
    } else {
        use_double_addr = 0;
        fb_addr = back_fb_addr;
    }
    XLOGD("[ChargingAnimation %s %d]fb_addr =%d \n",__FUNCTION__,__LINE__,fb_addr);
}

void* mt65xx_get_logo_db_addr(void)
{
    if(logo_addr == NULL)
    {
        XLOGD("LOGO ADDR NULL\n");
    }
    return logo_addr;
}

void mt65xx_disp_update(void)
{
    XLOGD("[ChargingAnimation %s %d]\n",__FUNCTION__,__LINE__);

    // use ioctl to framebuffer to udpate screen

    if (fb_addr == back_fb_addr)
    {
        vinfo.yoffset = vinfo.yres;
    }
    else
    {
        vinfo.yoffset = 0;
    }
    
    vinfo.activate |= (FB_ACTIVATE_FORCE | FB_ACTIVATE_NOW);

    if (ioctl(fb_fd, FBIOPUT_VSCREENINFO, &vinfo) < 0) {
        XLOGD("ioctl FBIOPUT_VSCREENINFO flip failed\n");
    }
}




// this should be the ut case entry
int mt65xx_fb_init(void)
{

    fb_fd = open("/dev/graphics/fb0", O_RDWR);
    if(fb_fd < 0)
    {
        XLOGD("open dev file fail\n");
        goto error;
    }

    ioctl(fb_fd,FBIOGET_VSCREENINFO,&vinfo);
    ioctl(fb_fd,FBIOGET_FSCREENINFO,&finfo);


    x_virtual = finfo.line_length/(vinfo.bits_per_pixel / 8);
    fb_size  = finfo.line_length * vinfo.yres;
    
    front_fb_addr =(unsigned int*)mmap (0, fb_size*2, PROT_READ | PROT_WRITE, MAP_SHARED, fb_fd,0);
    back_fb_addr = (unsigned int*)((unsigned int)front_fb_addr + fb_size);

    fb_addr = front_fb_addr;

    XLOGD("[ChargingAnimation %s %d]vinfo.xres  = %d, vinfo.yres = %d, x_virtual = %d, fb_size =%d, fb_addr = %d,back_fb_addr=%d\n"
            ,__FUNCTION__,__LINE__,vinfo.xres,vinfo.yres,x_virtual,fb_size,fb_addr,back_fb_addr);

    if(fb_addr == NULL || back_fb_addr == NULL)
    {
        XLOGD("ChargingAnimation mmap fail\n");
        goto error;
    }

    return 0;

error:
    munmap(front_fb_addr, fb_size*2);
    close(fb_fd);

    return -1;
}

void mt65xx_fb_deinit(void)
{
    munmap(front_fb_addr, fb_size*2);
    close(fb_fd);
}

void mt65xx_surface_init(void)
{
    status_t status;

    client = new SurfaceComposerClient();  

    sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain));  //MR1 ADDED
    XLOGD("[ChargingAnimation %s %d]ChargingAnimation getDisplayInfo()...",__FUNCTION__,__LINE__);
    status = SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
    if (status)
        XLOGD("[ChargingAnimation %s %d]error=%x %d",__FUNCTION__,__LINE__,status,status);//return -1;


    XLOGD("[ChargingAnimation %s %d]dinfo.w=%d,dinfo.h=%d,dinfo.orientation=%d",__FUNCTION__,__LINE__,dinfo.w,dinfo.h,dinfo.orientation);
    XLOGD("[ChargingAnimation %s %d]set default orientation",__FUNCTION__,__LINE__);
    XLOGD("[ChargingAnimation %s %d]mt65xx_surface_init  x_virtual = %d, vinfo.yres = %d\n",__FUNCTION__,__LINE__,x_virtual,vinfo.yres);

    SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientationDefault, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));          
 
   // SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientationDefault, Rect(x_virtual, ALIGN_TO(vinfo.yres,32)), Rect(x_virtual, ALIGN_TO(vinfo.yres,32)));  
    old_dinfo_Orientation = dinfo.orientation;
    dinfo_width = dinfo.w;
    dinfo_height = dinfo.h;
    //for case with hwrotation
    char property[PROPERTY_VALUE_MAX];
    if (property_get("ro.sf.hwrotation", property, NULL) > 0) {

    XLOGD("[ChargingAnimation %s %d]ro.sf.hwrotation= %s",__FUNCTION__,__LINE__,property);

     
        int width = dinfo.w;
        int height = dinfo.h;
        switch (atoi(property)) {
        case 90:
            dinfo.w = height;
            dinfo.h = width;
            SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientation90, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));
            XLOGD("[ChargingAnimation %s %d]set hw rotation 90",__FUNCTION__,__LINE__);
            break;
        case 180:
            SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientation180, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));
            XLOGD("[ChargingAnimation %s %d]set hw rotation 180",__FUNCTION__,__LINE__);
            break;            
        case 270:
            dinfo.w = height;
            dinfo.h = width;
            SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientation270, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));          
            XLOGD("[ChargingAnimation %s %d]set hw rotation 270",__FUNCTION__,__LINE__);

            break;
        default:
            break;
        }

        
    }

    XLOGD("[ChargingAnimation %s %d]dinfo.w=%d, dinfo.h=%d \n",__FUNCTION__,__LINE__,dinfo.w,dinfo.h);
    XLOGD("[ChargingAnimation %s %d]dinfo_width=%d, dinfo_height=%d, dinfo.orientation=%d",__FUNCTION__,__LINE__,dinfo_width,dinfo_height,dinfo.orientation);
    XLOGD("[ChargingAnimation %s %d]mt65xx_surface_init  x_virtual = %d, vinfo.yres = %d\n",__FUNCTION__,__LINE__,x_virtual,vinfo.yres);

    // create a client to connect to surfaceflinger
    dinfo_width = dinfo.w;
    dinfo_height = dinfo.h;

    XLOGD("[ChargingAnimation %s %d]dinfo_width=%d, dinfo_height=%d \n",__FUNCTION__,__LINE__,dinfo_width,dinfo_height);
//  surfaceControl = client->createSurface(String8("charging-surface"), ALIGN_TO(vinfo.xres_virtual,32),  ALIGN_TO(vinfo.yres,32), PIXEL_FORMAT_RGB_565, 0);
    surfaceControl = client->createSurface(String8("charging-surface"), dinfo_width,  dinfo_height, PIXEL_FORMAT_RGB_565);
    XLOGD("[ChargingAnimation %s %d]set layer geometry",__FUNCTION__,__LINE__);
    // set layer geometry
    SurfaceComposerClient::openGlobalTransaction();
    {  
        surfaceControl->setLayer(2000000);  
    }
    SurfaceComposerClient::closeGlobalTransaction();

    // data structure to access surface content 
    surfaceRectIndex = 0;
    surface = surfaceControl->getSurface();
    XLOGD("[ChargingAnimation %s %d]show_black_logo",__FUNCTION__,__LINE__);
//  mt65xx_disp_show_black_logo();
        
}

void mt65xx_surface_deinit(void)
{
    XLOGD("[ChargingAnimation %s %d]\n",__FUNCTION__,__LINE__);

    // disconnect
//\\ AAMTF, Twen, Fix Build Error : make the function
//mediatek/external/ipod/bootlogo.cpp:430:13: error: 'class android::SurfaceComposerClient' has no member named 'setOrientation'
//    client->setOrientation(0, old_dinfo_Orientation,0);
    surfaceControl->clear();
    client->dispose();
}

void mt65xx_logo_deinit(void)
{
    //close(logo_fd);
    free(logo_addr);
    logo_addr = NULL;
}

int mt65xx_logo_decompress(void *in, void *out, int inlen, int outlen)
{
    XLOGD("[ChargingAnimation %s %d]in=0x%08x, out=0x%08x, inlen=%d, logolen=%d\n",__FUNCTION__,__LINE__,
                in, out, inlen, outlen);
    int ret;
    unsigned have;
    z_stream strm;

    memset(&strm, 0, sizeof(z_stream));
    /* allocate inflate state */
    strm.zalloc = Z_NULL;
    strm.zfree = Z_NULL;
    strm.opaque = Z_NULL;
    strm.avail_in = 0;
    strm.next_in = Z_NULL;
    ret = inflateInit(&strm);
    if (ret != Z_OK)
        return ret;

    /* decompress until deflate stream ends or end of file */
    do {
        strm.avail_in = inlen;
        if (strm.avail_in <= 0)
            break;
        strm.next_in = (Bytef*)in;

        /* run inflate() on input until output buffer not full */
        do {
            strm.avail_out = outlen;
            strm.next_out = (Bytef*)out;
            ret = inflate(&strm, Z_NO_FLUSH);
            switch (ret) {
            case Z_NEED_DICT:
                ret = Z_DATA_ERROR;     /* and fall through */
            case Z_DATA_ERROR:
            case Z_MEM_ERROR:
                (void)inflateEnd(&strm);
                return ret;
            }
            have = outlen - strm.avail_out;
        } while (strm.avail_out == 0);


        /* done when inflate() says it's done */
    } while (ret != Z_STREAM_END);
    if (ret == Z_STREAM_END)
    /* clean up and return */
    (void)inflateEnd(&strm);


    return ret == Z_STREAM_END ? Z_OK : Z_DATA_ERROR;
}



static void show_logo_fb(unsigned int index)
{

    XLOGD("[ChargingAnimation %s %d]index : %d\n",__FUNCTION__,__LINE__, index);

    unsigned int logonum;
    unsigned int logolen;
    unsigned int inaddr;


    void *temp = NULL;

    void  *db_addr = mt65xx_get_logo_db_addr();

    unsigned int *pinfo = (unsigned int*)db_addr;
    logonum = pinfo[0];

    temp = malloc(fb_size);
    if(temp == NULL)
    {
        XLOGD("allocate buffer fail\n");
        //return;
        if (!index) // index==0, draw logo
            reboot(LINUX_REBOOT_CMD_RESTART);
        else //draw charging animation.
            reboot(RB_POWER_OFF);

    }

    if(index < logonum)
        logolen = pinfo[3+index] - pinfo[2+index];
    else
        logolen = pinfo[1] - pinfo[2+index];

    inaddr = (unsigned int)db_addr+pinfo[2+index];

    XLOGD("[ChargingAnimation %s %d]pinfo[0]=0x%08x, pinfo[1]=0x%08x, pinfo[2]=%d\n", __FUNCTION__,__LINE__,
        pinfo[0], pinfo[1], pinfo[2]);
        
    mt65xx_logo_decompress((void*)inaddr, (void*)temp, logolen, fb_size);
    
    XLOGD("[ChargingAnimation %s %d]vinfo.bits_per_pixel = %d\n", __FUNCTION__,__LINE__,vinfo.bits_per_pixel);
        
    if (16 == vinfo.bits_per_pixel) // RGB565
    {
        memcpy(fb_addr, temp, fb_size);
    }
    else if (32 == vinfo.bits_per_pixel) // ARGB8888
    {
        unsigned short src_rgb565 = 0;

        unsigned short *s = (unsigned short *)temp;
        unsigned short *fb_tmp_addr = (unsigned short *)fb_addr;
        unsigned int   *d = fb_addr;
        unsigned int j = 0;
        unsigned int k = 0;
        unsigned int l = 0;
        XLOGD("[ChargingAnimation %s %d]MTK_LCM_PHYSICAL_ROTATION = %s\n", __FUNCTION__,__LINE__,MTK_LCM_PHYSICAL_ROTATION);
    
    
        if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "270", 3))
        {
            for (j=0; j<vinfo.xres; j++){
                for (k=0, l=vinfo.yres-1; k<vinfo.yres; k++, l--)
                {
                        src_rgb565 = *s++;
                        d = fb_addr + ((x_virtual * l + j) << 2);
                        *d = RGB565_TO_ARGB8888(src_rgb565);
                }
            }
        }
        else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "90", 2))
        {
    
                for (j=vinfo.xres - 1; j>=0; j--){
                    for (k=0, l=0; k<vinfo.yres; k++, l++)
                    {
                        src_rgb565 = *s++;
                        d = fb_addr + ((x_virtual * l + j) << 2);
                        *d = RGB565_TO_ARGB8888(src_rgb565);
                    }
                }
        }
        else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "180", 2))
        {
            unsigned int width = vinfo.xres;
            unsigned int height = vinfo.yres;



            unsigned short *src = (unsigned short*)s + ((height - 1) * width);
            unsigned int *dst = d;
            //UINT16 *pLine2 = (UINT16*)addr;
            for (j = 0; j < height; ++ j) {
              for (k = 0; k < width; ++ k) {
                    src_rgb565 = *(src+width-k);
                    *(dst+k) = RGB565_TO_ARGB8888(src_rgb565);
              }
              for (k = width; k < x_virtual; ++ k) {
                  *(dst+k) = 0xFFFFFFFF;
              }
              dst += x_virtual;
              src -= width;
           }

        
        }
        else
        {
            for (j = 0; j < vinfo.yres; ++ j){
                    for(k = 0; k < vinfo.xres; ++ k)
                    {
                        src_rgb565 = *s++;
                        *d++ = RGB565_TO_ARGB8888(src_rgb565);
                    }
                    for(k = vinfo.xres; k < x_virtual; ++ k)
                        *d++ = 0xFFFFFFFF;
                }
            }
        }
 }

static void show_logo_surface(unsigned int index)
{

    XLOGD("[ChargingAnimation %s %d]index : %d\n",__FUNCTION__,__LINE__, index);

    unsigned int logonum;
    unsigned int logolen;
    unsigned int inaddr;


    void *temp = NULL;

    void  *db_addr = mt65xx_get_logo_db_addr();

    unsigned int *pinfo = (unsigned int*)db_addr;
    logonum = pinfo[0];

    temp = malloc(fb_size);
    if(temp == NULL)
    {
        XLOGD("allocate buffer fail\n");
        //return;
        if (!index) // index==0, draw logo
            reboot(LINUX_REBOOT_CMD_RESTART);
        else //draw charging animation.
            reboot(RB_POWER_OFF);

    }

    if(index < logonum)
        logolen = pinfo[3+index] - pinfo[2+index];
    else
        logolen = pinfo[1] - pinfo[2+index];

    inaddr = (unsigned int)db_addr+pinfo[2+index];

    XLOGD("[ChargingAnimation %s %d]pinfo[0]=0x%08x, pinfo[1]=0x%08x, pinfo[2]=%d\n", __FUNCTION__,__LINE__,
        pinfo[0], pinfo[1], pinfo[2]);
        
    mt65xx_logo_decompress((void*)inaddr, (void*)temp, logolen, fb_size);


    XLOGD("[ChargingAnimation %s %d]info.bits = 0x%08x\n",__FUNCTION__,__LINE__, info.bits);
    XLOGD("[ChargingAnimation %s %d]info.w = %d info.h = %d \n",__FUNCTION__,__LINE__, info.w ,info.h);

    UINT16 *ptr = (UINT16 *)info.bits;

    unsigned short *s = (unsigned short *)temp;
    unsigned short src_rgb565 = 0;

    unsigned int   *d = (unsigned int *)info.bits;
    int j = 0;
    int k = 0;
    int l = 0;
    XLOGD("[ChargingAnimation %s %d]MTK_LCM_PHYSICAL_ROTATION = %s\n", __FUNCTION__,__LINE__,MTK_LCM_PHYSICAL_ROTATION);
    XLOGD("[ChargingAnimation %s %d]vinfo.yres= %d,vinfo.xres = %d, x_virtual = %d, vinfo.bits_per_pixel = %d , info.format=%d \n",__FUNCTION__,__LINE__,
         vinfo.yres, vinfo.xres , x_virtual, vinfo.bits_per_pixel , info.format);


    int tw= ALIGN_TO(dinfo_width,32);
    int th= dinfo_height;
    XLOGD("[ChargingAnimation %s %d]tw=%d,th=%d", __FUNCTION__,__LINE__,tw,th);
    
    for (j = 0; j < th; ++ j)
    {
        for(k=0;k<dinfo_width;k++) 
            ptr[j*tw+k] = *s++;
    }

    free(temp);
}
static void show_logo(unsigned int index)
{
    
    XLOGD("[ChargingAnimation %s %d]running_kpoc_mode=%d, show  index =  %d\n",__FUNCTION__,__LINE__,running_kpoc_mode,index);
    
    if (running_kpoc_mode == 1){
        show_logo_fb(index);
    } else {
        show_logo_surface(index);
    
    }

}


void mt_show_logo_fb(int index)
{
    XLOGD("[ChargingAnimation %s %d]fb_addr= 0x%08x\n",__FUNCTION__,__LINE__, fb_addr);

    show_logo_fb(index);
    mt65xx_disp_update();
}

void mt_show_logo_surface(int index)
{
    Region    region(Rect(0, 0, x_virtual, vinfo.yres));
    status_t  lockResult = surface->lock(&info, &region);  
    XLOGD("[ChargingAnimation %s %d]info.bits = 0x%08x\n",__FUNCTION__,__LINE__, info.bits);
    XLOGD("[ChargingAnimation %s %d]surface->lock return =  0x%08x,  %d\n",__FUNCTION__,__LINE__,lockResult,lockResult);
    if (0 == lockResult)
    {
        show_logo_surface(index);
        surface->unlockAndPost();
    }
    return;
}

void mt_show_logo(int index){
 
     XLOGD("[ChargingAnimation %s %d]running_kpoc_mode=%d, show  index =  %d\n",__FUNCTION__,__LINE__,running_kpoc_mode,index);
 
     if (running_kpoc_mode == 1){
         mt_show_logo_fb(index);
     } else {
         mt_show_logo_surface(index);
 
     }
     
 }
 
void mt65xx_disp_show_boot_logo(void)
{
    XLOGD("[ChargingAnimation %s %d]show boot logo, index = 0 \n",__FUNCTION__,__LINE__);
    mt_show_logo(0);

}






void mt65xx_disp_show_low_battery(void){
    XLOGD("[ChargingAnimation %s %d]show low battery logo, index = 2 \n",__FUNCTION__,__LINE__);
    mt_show_logo(2);    
}

void mt65xx_disp_show_charger_ov_logo(void)
{
    XLOGD("[ChargingAnimation %s %d]show charger_ov logo, index = 3 \n",__FUNCTION__,__LINE__);
    mt_show_logo(3);

}

//TODO use fb
 void mt65xx_disp_show_black_logo(void)
{
     // draw a black screen first to avoid landscape ghost image
    XLOGD("[ChargingAnimation %s %d]draw a black screen first to avoid landscape ghost image\n",__FUNCTION__,__LINE__);
  

    if (running_kpoc_mode == 1){
        unsigned int   *d = fb_addr;
        XLOGD("[ChargingAnimation %s %d]fb_addr= 0x%08x\n",__FUNCTION__,__LINE__, fb_addr);
    //  unsigned int   *d  =  (unsigned int   *) mt65xx_get_fb_addr();
        memset((uint16_t*)d, 0x00, fb_size);
        mt65xx_disp_update();
    } else {
        status_t  lockResult = surface->lock(&info);  
        XLOGD("[ChargingAnimation %s %d]info.bits = 0x%08x\n",__FUNCTION__,__LINE__, info.bits);
        XLOGD("[ChargingAnimation %s %d]surface->lock return =  0x%08x,  %d\n",__FUNCTION__,__LINE__,lockResult,lockResult);
        if (0 == lockResult)
        {
            ssize_t bpr = info.s * bytesPerPixel(info.format);
            memset((uint16_t*)info.bits, 0x00, bpr*info.h);
            surface->unlockAndPost();
        }

    }

}
 

void mt65xx_disp_show_battery_capacity(unsigned int capacity)
{
#ifdef ANIMATION_NEW
    if(old_logo_bin > 0) 
    {
     //add by @mtk54040 logo size is  small, show old aniamtion 
        cust_show_battery_capacity(capacity);   
    }else {
        cust_show_battery_capacity_new(capacity);
    }
#else
    cust_show_battery_capacity(capacity);
#endif

    return;
}

void mt_disp_fill_rect_fb(unsigned int left, unsigned int top,
                           unsigned int right, unsigned int bottom,
                           unsigned int color)
{

    const UINT32 WIDTH = x_virtual;
    const UINT32 HEIGHT = vinfo.yres; 
    UINT32 *pLine = (UINT32 *)fb_addr + top * WIDTH + left;
    UINT32 COLOR = RGB565_TO_ARGB8888(color);
    UINT32 x, y;
    UINT32 i = 0;

    XLOGD("[ChargingAnimation]WIDTH = %d, HEIGHT = %d, vinfo.xres = %d, fb_addr=0x%08x, pLine=0x%08x\n", WIDTH, HEIGHT, vinfo.xres,fb_addr,pLine);
    if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "270", 3))
    {
        XLOGD("[ChargingAnimation]  MTK_LCM_PHYSICAL_ROTATION = 270\n");
        unsigned int l;
            UINT32 *d = (UINT32 *)fb_addr;
            for (x=top; x<bottom; x++) {
                for (y=left, l= HEIGHT - left; y<right; y++, l--)
                        {
                            d = (UINT32 *)fb_addr + ((WIDTH * l + x) << 2);
                            *d = COLOR;
                        }
            }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "90", 2))
    {
        XLOGD("[ChargingAnimation] MTK_LCM_PHYSICAL_ROTATION = 90\n");
        unsigned int l;
        UINT32 *d = (UINT32 *)fb_addr;
        for (x=WIDTH - top + 1; x > WIDTH - bottom; x--) {
            for (y=left, l=left; y<right; y++, l++)
            {
                d = (UINT32 *)fb_addr + ((WIDTH * l + x) << 2);
                *d = COLOR;
            }
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "180", 3))
    {
        XLOGD("[ChargingAnimation %s %d]180\n",__FUNCTION__,__LINE__); 

        int j = 0;
        int k = 0;
        int l = 0;

        unsigned int height = (bottom - top);
        unsigned int width = (right - left);
        XLOGD("[ChargingAnimation %s %d]height = %d,width = %d\n",__FUNCTION__,__LINE__,height,width);  
        int tmp_bottom = vinfo.yres - top ;
        int tmp_top = vinfo.yres - bottom ;
        int tmp_right = vinfo.xres - left ;
        int tmp_left = vinfo.xres - right ;   
        XLOGD("[ChargingAnimation %s %d]tmp_bottom = %d,tmp_top = %d,tmp_right = %d,tmp_left = %d\n",__FUNCTION__,__LINE__,tmp_bottom,tmp_top,tmp_right,tmp_left);    
    
        unsigned int *dst = (unsigned int *)fb_addr + tmp_top * x_virtual + tmp_left + 1; // fix wrong edge
//        unsigned int *dst = (unsigned int *)fb_addr + tmp_top * x_virtual + tmp_left;
        for (j = 0; j < height; ++ j) {
            for (k = 0; k < width; ++ k) {
                 *(dst+k) = COLOR;
            }
            dst += x_virtual;

        }

    }
   else {
       for (y = top; y < bottom; ++ y) {
          UINT32 *pPixel = pLine;
          for (x = left; x < right; ++ x) {
             *pPixel = COLOR;
              pPixel++;
          }
          pLine += WIDTH; 
       }
   }
}
void mt_disp_fill_rect_surface(unsigned int left, unsigned int top,
                           unsigned int right, unsigned int bottom,
                           unsigned int color)
{
    const unsigned int WIDTH = x_virtual;

    const unsigned short COLOR = (unsigned short)color;
    unsigned short *ptr = (unsigned short *)info.bits;
    

    int tw= ALIGN_TO(dinfo_width,32);
    int th= dinfo_height;
    XLOGD("[ChargingAnimation %s %d]tw=%d,th=%d", __FUNCTION__,__LINE__,tw,th);//600,1024,608,1024

    for (int j = top; j < bottom; ++ j)
    {
        for(int k=left;k<right;k++) 
        ptr[j*tw+k] = COLOR;
    }

}
void mt65xx_disp_fill_rect(unsigned int left, unsigned int top,
                           unsigned int right, unsigned int bottom,
                           unsigned int color)
{
    XLOGD("[ChargingAnimation %s %d]running_kpoc_mode=%d,\n",__FUNCTION__,__LINE__,running_kpoc_mode);
    
    if (running_kpoc_mode == 1){
        mt_disp_fill_rect_fb( left,  top,  right,  bottom, color);
    } else {
        mt_disp_fill_rect_surface( left,  top,  right,  bottom, color);
    }

}

void mt65xx_disp_draw_prog_bar(DISP_PROG_BAR_DIRECT direct,
                               unsigned int left, unsigned int top,
                               unsigned int right, unsigned int bottom,
                               unsigned int fgColor, unsigned int bgColor,
                               unsigned int start_div, unsigned int total_div,
                               unsigned int occupied_div)
{
    const unsigned int PADDING = 3;
    unsigned int div_size  = (bottom - top) / total_div;
    unsigned int draw_size = div_size - PADDING;

    unsigned int i;

    if (DISP_HORIZONTAL_PROG_BAR == direct)
    {
        div_size = (right - left) / total_div;
        draw_size = div_size - PADDING;
        for (i = start_div; i < start_div + occupied_div; ++ i)
        {
            unsigned int draw_left = left + div_size * i + PADDING;
            unsigned int draw_right = draw_left + draw_size;

            mt65xx_disp_fill_rect(draw_left, top, draw_right, bottom, fgColor);

        }
    }
    else if(DISP_VERTICAL_PROG_BAR == direct)
    {
        div_size  = (bottom - top) / total_div;
        draw_size = div_size - PADDING;

        for (i = start_div; i < start_div + occupied_div; ++ i)
        {
            unsigned int draw_bottom = bottom - div_size * i - PADDING;
            unsigned int draw_top    = draw_bottom - draw_size;

            // fill one division of the progress bar
            mt65xx_disp_fill_rect(left, draw_top, right, draw_bottom, fgColor);
        }
    }
    else
    {
        XLOGD("direction not implemented");
    }
}

void cust_show_battery_capacity(unsigned int capacity)
{
    DISP_PROG_BAR_DIRECT direct;
#if MTK_QVGA_LANDSCAPE_SUPPORT
    if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "270", 3)
       || 0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "90", 2))
//  if((MTK_LCM_PHYSICAL_ROTATION == 90 || MTK_LCM_PHYSICAL_ROTATION == 270))
    {
        direct = DISP_HORIZONTAL_PROG_BAR;
    }
    else
    {
        direct = DISP_VERTICAL_PROG_BAR;
    }
#else
    direct = DISP_VERTICAL_PROG_BAR;
#endif
    direct = DISP_VERTICAL_PROG_BAR;
    unsigned int capacity_grids = 0;

    if (capacity > 100) capacity = 100;

    capacity_grids = (capacity * VOLTAGE_SCALE) / 100;

    if (running_kpoc_mode == 1){
        change_fb_addr();
    } else {
        Region    region1(Rect(0, 0, x_virtual, vinfo.yres));
        status_t  lockResult = surface->lock(&info, &region1);
        XLOGD("[ChargingAnimation %s %d]info.bits = 0x%08x\n",__FUNCTION__,__LINE__, info.bits);
        XLOGD("[ChargingAnimation %s %d]lock surface=%d\n",__FUNCTION__,__LINE__,lockResult);

    }
    
    show_logo(1);

    // Fill Occupied Color

    mt65xx_disp_draw_prog_bar(direct,
                              bar_rect.left + 1, bar_rect.top + 1,
                              bar_rect.right, bar_rect.bottom,
                              bar_occupied_color, bar_bg_color,
                              0, VOLTAGE_SCALE, capacity_grids);

    // Fill Empty Color

    mt65xx_disp_draw_prog_bar(direct,
                              bar_rect.left + 1, bar_rect.top + 1,
                              bar_rect.right, bar_rect.bottom,
                              bar_empty_color, bar_bg_color,
                              capacity_grids, VOLTAGE_SCALE,
                              VOLTAGE_SCALE - capacity_grids);

    if (running_kpoc_mode == 1){
        mt65xx_disp_update();
    } else {
        surface->unlockAndPost();
        XLOGD("[ChargingAnimation %s %d]unlock surface\n",__FUNCTION__,__LINE__);
    }


}

void cust_show_battery_capacity_new(UINT32 capacity)
{
    XLOGD("[ChargingAnimation %s %d]capacity : %d\n",__FUNCTION__,__LINE__, capacity);
    if (capacity <= 0)
      {
         return;
      }
    surfaceRectIndex = 0;
    if (running_kpoc_mode == 1){
        change_fb_addr();
    } else {
        Region    region1(Rect(0, 0, x_virtual, vinfo.yres));
        status_t  lockResult = surface->lock(&info, &region1);
        XLOGD("[ChargingAnimation %s %d]info.bits = 0x%08x\n",__FUNCTION__,__LINE__, info.bits);
        XLOGD("[ChargingAnimation %s %d]lock surface=%d\n",__FUNCTION__,__LINE__,lockResult);

    }
    
    if (capacity >= 100)
    {
        show_logo(37); // battery 100
     
    }else if (capacity < 10){
        XLOGD("[ChargingAnimation %s %d]charging_low_index = %d\n",__FUNCTION__,__LINE__, charging_low_index);  
        charging_low_index ++ ;
        show_logo(25 + charging_low_index);
        show_animation_number(number_pic_start_0+capacity,1);   
        show_animation(14, percent_location_rect, percent_pic_addr);
        if (charging_low_index >= 9) charging_low_index = 0;

    }else{

        UINT32 capacity_grids = 0;
        capacity_grids = battery_rect.bottom - (battery_rect.bottom - battery_rect.top) * (capacity - 10) / 90;
        XLOGD("[ChargingAnimation %s %d]capacity_grids : %d,charging_animation_index = %d\n",__FUNCTION__,__LINE__, capacity_grids,charging_animation_index);   

        //background 
        show_logo(35);   

        // fill number and line
        show_animation_line(36,capacity_grids);
        show_animation_number(number_pic_start_0+(capacity/10),0);
        show_animation_number(number_pic_start_0+(capacity%10),1);
        show_animation(14, percent_location_rect, percent_pic_addr);

         if (capacity <= 90)
         {
            top_animation_rect.bottom = capacity_grids;
            top_animation_rect.top = capacity_grids - top_animation_height;
            charging_animation_index++;        
            show_animation(15 + charging_animation_index, top_animation_rect, top_animation_addr);
            if (charging_animation_index >= 9) charging_animation_index = 0;
         }
    }
    if (running_kpoc_mode == 1){
        mt65xx_disp_update();
    } else {
        surface->unlockAndPost();
        XLOGD("[ChargingAnimation %s %d]unlock surface\n",__FUNCTION__,__LINE__);
    }
}

void fill_rect_flow_fb(UINT32 left, UINT32 top, UINT32 right, UINT32 bottom, char *addr)
{

    const UINT32 WIDTH = x_virtual;
    const UINT32 HEIGHT = vinfo.yres; 
    UINT32 *pLine = (UINT32 *)fb_addr + top * WIDTH + left;
    UINT16 *pLine2 = (UINT16*)addr; 
    UINT32 x, y;
    UINT32 i = 0;
    UINT16 s = 0;
    XLOGD("[ChargingAnimation]fill_rect_flow: WIDTH = %d, HEIGHT = %d, vinfo.xres = %d, fb_addr=0x%08x, pLine=0x%08x\n", WIDTH, HEIGHT, vinfo.xres,fb_addr,pLine);
    if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "270", 3))
    {
    XLOGD("[ChargingAnimation]fill_rect_flow : MTK_LCM_PHYSICAL_ROTATION = 270\n");
    unsigned int l;
        UINT32 *d = fb_addr;
        for (x=top; x<bottom; x++) {
            for (y=left, l= HEIGHT - left; y<right; y++, l--)
                    {
                        d = fb_addr + ((WIDTH * l + x) << 2);
                        s = pLine2[i++];
                        if(s != 0)
                          *d = RGB565_TO_ARGB8888(s);
                    }
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "90", 2))
    {
   XLOGD("[ChargingAnimation]fill_rect_flow : MTK_LCM_PHYSICAL_ROTATION = 90\n");
        unsigned int l;
        UINT32 *d = fb_addr;
        for (x=WIDTH - top + 1; x > WIDTH - bottom; x--) {
            for (y=left, l=left; y<right; y++, l++)
            {
                d = fb_addr + ((WIDTH * l + x) << 2);
                s = pLine2[i++];
                            if(s != 0)
                              *d = RGB565_TO_ARGB8888(s);
            }
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "180", 3))
    {
        XLOGD("[ChargingAnimation %s %d]180\n",__FUNCTION__,__LINE__); 

        unsigned int j = 0;
        unsigned int k = 0;
        unsigned int l = 0;

        unsigned int height = (bottom - top);
        unsigned int width = (right - left);
        XLOGD("[ChargingAnimation %s %d]height = %d,width = %d\n",__FUNCTION__,__LINE__,height,width);  
        unsigned int tmp_bottom = vinfo.yres - top ;
        unsigned int tmp_top = vinfo.yres - bottom ;
        unsigned int tmp_right = vinfo.xres - left ;
        unsigned int tmp_left = vinfo.xres - right ;   
        XLOGD("[ChargingAnimation %s %d]tmp_bottom = %d,tmp_top = %d,tmp_right = %d,tmp_left = %d\n",__FUNCTION__,__LINE__,tmp_bottom,tmp_top,tmp_right,tmp_left);    
    
        unsigned short *src = pLine2 + ((height - 1) * width);
        unsigned int *dst = (unsigned int *)fb_addr + tmp_top * x_virtual + tmp_left + 1; // fix wrong edge
//        unsigned int *dst = (unsigned int *)fb_addr + tmp_top * x_virtual + tmp_left;
        for (j = 0; j < height; ++ j) {
            for (k = 0; k < width; ++ k) {
                 s = *(src+width-k-1);
                 *(dst+k) = RGB565_TO_ARGB8888(s);
            }
            dst += x_virtual;
            src -= width;
        }

    }
   else 
        {
           for (y = top; y < bottom; ++ y) {
              UINT32 *pPixel = pLine;
              for (x = left; x < right; ++ x) {
                s = pLine2[i++];
              if(s != 0)
                 *pPixel = RGB565_TO_ARGB8888(s);
                 pPixel++;
              }
              pLine += WIDTH; 
              XLOGD("[ChargingAnimation]fill_rect_flow: addr=0x%08x, pLine=0x%08x\n", addr, pLine);
           }
        }
}
void fill_rect_flow_surface(UINT32 left, UINT32 top, UINT32 right, UINT32 bottom, char *addr)
{
    XLOGD("[ChargingAnimation %s %d]left = %d,top = %d,right = %d,bottom = %d  addr = 0x%08x\n\n",__FUNCTION__,__LINE__,
            left,top,right,bottom,addr);
    const UINT32 WIDTH = x_virtual;
    const UINT32 HEIGHT = vinfo.yres; 
    UINT16 *pLine2 = (UINT16*)addr; 
    UINT32 x, y;
    UINT32 i = 0;
    UINT16 s = 0;

    UINT16 *ptr = (UINT16 *)info.bits;
    ptr += top*WIDTH + left;

    XLOGD("[ChargingAnimation %s %d]fill_rect_flow: addr=0x%08x, ptr=0x%08x, WIDTH =0x%08x\n", __FUNCTION__,__LINE__,addr, ptr, WIDTH);
    for (y = top; y < bottom; ++ y)
    {
        UINT16 *pPixel = ptr;
        for (x = left; x < right; ++ x)
        {
            s = pLine2[i++];
            if(s != 0)
            {
//              XLOGD("[ChargingAnimation %s %d]fill_rect_flow: pPixel=0x%08x, s =0x%08x\n",__FUNCTION__,__LINE__, pPixel, s);
                *pPixel = s;
            }
            pPixel++;
        }
        ptr += WIDTH;

    }

    if(++surfaceRectIndex == 4)
        surfaceRectIndex = 0;
}
void fill_rect_flow(UINT32 left, UINT32 top, UINT32 right, UINT32 bottom, char *addr)
{
    XLOGD("[ChargingAnimation %s %d]running_kpoc_mode=%d,\n",__FUNCTION__,__LINE__,running_kpoc_mode);
    
    if (running_kpoc_mode == 1){
         fill_rect_flow_fb( left,  top,  right,  bottom, addr);
    } else {
        fill_rect_flow_surface( left,  top, right,  bottom, addr);

    
    }

}

void fill_line_flow_fb(UINT32 left, UINT32 top, UINT32 right, UINT32 bottom, char *addr)
{
   // void * fb_addr = mt65xx_get_fb_addr();
    const UINT32 WIDTH = x_virtual;
    const UINT32 HEIGHT = vinfo.yres;
    UINT32 *pLine = (UINT32 *)fb_addr + top * WIDTH + left;
    UINT16 *pLine2 = (UINT16*)addr;     
    UINT32 x, y;
    UINT32 i = 0;
    UINT16 s = 0;
    if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "270", 3))
    {
        unsigned int l;
        UINT32 *d = fb_addr;
        for (x=top; x<bottom; x++) {
            for (y=left, l= HEIGHT - left; y<right; y++, l--)
                    {
                d = fb_addr + ((WIDTH * l + x) << 2);
                            s = pLine2[i++];
                            if(s != 0)
                              *d = RGB565_TO_ARGB8888(s);
                    }
                    i = 0;
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "90", 2))
    {
        unsigned int l;
        UINT32 *d = fb_addr;
        for (x=WIDTH - top + 1; x > WIDTH - bottom; x--) {
            for (y=left, l=left; y<right; y++, l++)
            {
                d = fb_addr + ((WIDTH * l + x) << 2);
                            s = pLine2[i++];
                            if(s != 0)
                              *d = RGB565_TO_ARGB8888(s);
            }
                    i = 0;
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "180", 3))
    {

        XLOGD("[ChargingAnimation %s %d]180\n",__FUNCTION__,__LINE__); 

        unsigned int j = 0;
        unsigned int k = 0;
        unsigned int l = 0;

        unsigned int height = (bottom - top);
        unsigned int width = (right - left);

        XLOGD("[ChargingAnimation %s %d]height = %d,width = %d\n",__FUNCTION__,__LINE__,height,width);  
        unsigned int tmp_bottom = vinfo.yres - top ;
        unsigned int tmp_top = vinfo.yres - bottom ;
        unsigned int tmp_right = vinfo.xres - left ;
        unsigned int tmp_left = vinfo.xres - right ;       
          
        XLOGD("[ChargingAnimation %s %d]tmp_bottom = %d,tmp_top = %d,tmp_right = %d,tmp_left = %d\n",__FUNCTION__,__LINE__,tmp_bottom,tmp_top,tmp_right,tmp_left);    
        
         unsigned short src_rgb565 = 0;
         //unsigned short *src = pLine2 + ((height - 1) * width);
         unsigned int *dst = (unsigned int *)fb_addr + tmp_top * WIDTH + tmp_left+1;
         //unsigned int *dst = (unsigned int *)fb_addr + tmp_top * WIDTH + tmp_left;
         for (j = 0; j < height; ++ j) {
           for (k = 0; k < width; ++ k) {
                 s = pLine2[width-k-1];
                 //src_rgb565 = *(src+width-k);
                 *(dst+k) = RGB565_TO_ARGB8888(s);
           }
           dst += x_virtual;
        //   src -= width;
        }
        
    }
   else 
        {
           for (y = top; y < bottom; ++ y) {
              UINT32 *pPixel = pLine;
              for (x = left; x < right; ++ x) {
                  s = pLine2[i++];
              if(s != 0)
                  *pPixel = RGB565_TO_ARGB8888(s);
                  pPixel++;
              }
              pLine += WIDTH;
              i = 0;
           }

        }
}

void fill_line_flow_surface(UINT32 left, UINT32 top, UINT32 right, UINT32 bottom, char *addr)
{
    XLOGD("[ChargingAnimation %s %d]left = %d,top = %d,right = %d,bottom = %d  addr = 0x%08x\n\n",__FUNCTION__,__LINE__,
            left,top,right,bottom,addr);

    const UINT32 WIDTH = x_virtual;
    const UINT32 HEIGHT = vinfo.yres;
    UINT16 *pLine2 = (UINT16*)addr;     
    UINT32 x, y;
    UINT32 i = 0;
    UINT16 s = 0;
    UINT16 *ptr;

    ptr = (UINT16 *)info.bits;


    ptr += top*WIDTH + left;

    XLOGD("[ChargingAnimation %s %d]ptr = 0x%08x\n",__FUNCTION__,__LINE__, ptr);

    
    for (y = top; y < bottom; ++ y)
    {
        UINT16 *  pPixel = ptr;
        for (x = left; x < right; ++ x)
        {
            s = pLine2[i++];
            if(s != 0)
            {
                *pPixel = s;
            }
            pPixel++;
        }
        ptr += WIDTH;
        i = 0;
    }

}

void fill_line_flow(UINT32 left, UINT32 top, UINT32 right, UINT32 bottom, char *addr)
{
    XLOGD("[ChargingAnimation %s %d]running_kpoc_mode=%d,\n",__FUNCTION__,__LINE__,running_kpoc_mode);
    
    if (running_kpoc_mode == 1){
        fill_line_flow_fb( left,  top,  right,  bottom, addr);
    } else {
        fill_line_flow_surface( left,  top, right,  bottom, addr);

    
    }

}

void show_animation(UINT32 index, RECT rect, char* addr){
        UINT32 logonum;
        UINT32 logolen;
        UINT32 inaddr;
        UINT32 i;

        void  *db_addr = mt65xx_get_logo_db_addr();

        unsigned int *pinfo = (unsigned int*)db_addr;
        logonum = pinfo[0];

        XLOGD("[ChargingAnimation %s %d]index = %d, logonum = %d\n", __FUNCTION__,__LINE__,index, logonum);
        //ASSERT(index < logonum);
        if(index < logonum)
            logolen = pinfo[3+index] - pinfo[2+index];
        else
            logolen = pinfo[1] - pinfo[2+index];

        inaddr = (unsigned int)db_addr+pinfo[2+index];
        XLOGD("[ChargingAnimation %s %d]in_addr=0x%08x, dest_addr=0x%08x, logolen=%d\n",__FUNCTION__,__LINE__,
                    inaddr, logolen, logolen);

        mt65xx_logo_decompress((void*)inaddr, (void*)addr, logolen, (rect.right-rect.left)*(rect.bottom-rect.top)*2);

        fill_rect_flow(rect.left,rect.top,rect.right,rect.bottom,addr);

    }


// number_position: 0~1st number, 1~2nd number ,2~%
void show_animation_number(UINT32 index,UINT32 number_position){
    XLOGD("[ChargingAnimation %s %d]index= %d, number_position = %d\n",__FUNCTION__,__LINE__, index, number_position);

    UINT32 logonum;
    UINT32 logolen;
    UINT32 inaddr;
    UINT32 i;

    //void  *fb_addr = mt65xx_get_fb_addr();
    //UINT32 fb_size = mt65xx_get_fb_size();
    void  *db_addr = mt65xx_get_logo_db_addr();

    unsigned int *pinfo = (unsigned int*)db_addr;
    logonum = pinfo[0];

    XLOGD("[ChargingAnimation %s %d]index= %d, logonum = %d\n", __FUNCTION__,__LINE__,index, logonum);
    //ASSERT(index < logonum);

    if(index < logonum)
        logolen = pinfo[3+index] - pinfo[2+index];
    else
        logolen = pinfo[1] - pinfo[2+index];

    inaddr = (unsigned int)db_addr+pinfo[2+index];
    XLOGD("[ChargingAnimation %s %d]in_addr=0x%08x, dest_addr=0x%08x, logolen=%d\n",__FUNCTION__,__LINE__,
                inaddr, logolen, logolen);

    // draw default number rect,
    mt65xx_logo_decompress((void*)inaddr, (void*)number_pic_addr, logolen, number_pic_size);

    fill_rect_flow(number_location_rect.left+ number_pic_width*number_position,
                        number_location_rect.top,
                        number_location_rect.right+number_pic_width*number_position,
                        number_location_rect.bottom,number_pic_addr);
}


void show_animation_line(UINT32 index,UINT32 capacity_grids){
    UINT32 logonum;
    UINT32 logolen;
    UINT32 inaddr;
    UINT32 i;

    //void  *fb_addr = mt65xx_get_fb_addr();
    //UINT32 fb_size = mt65xx_get_fb_size();
    void  *db_addr = mt65xx_get_logo_db_addr();

    unsigned int *pinfo = (unsigned int*)db_addr;
    logonum = pinfo[0];

    XLOGD("[ChargingAnimation %s %d]index= %d, logonum = %d\n", __FUNCTION__,__LINE__,index, logonum);
    //ASSERT(index < logonum);

    if(index < logonum)
        logolen = pinfo[3+index] - pinfo[2+index];
    else
        logolen = pinfo[1] - pinfo[2+index];

    inaddr = (unsigned int)db_addr+pinfo[2+index];
    XLOGD("[ChargingAnimation %s %d]in_addr=0x%08x, dest_addr=0x%08x, logolen=%d\n",__FUNCTION__,__LINE__,
                inaddr, logolen, logolen);

    //windows draw default 160 180,
    mt65xx_logo_decompress((void*)inaddr, (void*)line_pic_addr, logolen, line_pic_size);

    fill_line_flow(battery_rect.left, capacity_grids, battery_rect.right, battery_rect.bottom, line_pic_addr);

}

void bootlogo_fb_init()
{
    XLOGD("[ChargingAnimation %s %d]firtstInitIndex = %d\n",__FUNCTION__,__LINE__,firtstInitIndex);

    firtstInitIndex++;
    mt65xx_logo_init();
    mt65xx_fb_init();
    if (running_kpoc_mode == 1){
    // may put fb_addr into here
    } else {
        // add for surface flinger
        mt65xx_surface_init();
    }
    mt65xx_boot_logo_updater_init();
}

void bootlogo_fb_deinit()
{
    XLOGD("[ChargingAnimation %s %d]\n",__FUNCTION__,__LINE__);
    mt65xx_boot_logo_updater_deinit();
    mt65xx_logo_deinit();
    
    mt65xx_fb_deinit();
    if (running_kpoc_mode == 1){
        // may add fb_addr deinit
    } else {
        // deinit surface flinger
        mt65xx_surface_deinit();
    }

}


void bootlogo_show_boot()
{
    mt65xx_disp_show_boot_logo();
}

void bootlogo_show_charging(int capacity, int cnt)
{
    XLOGD("[ChargingAnimation %s %d]%d, %d",__FUNCTION__,__LINE__, capacity, cnt);

    static int bc_offset = 0;
    int bc, base;

    if (get_ov_status()) {
        mt65xx_disp_show_charger_ov_logo();
        return;
    }

    if (showLowBattLogo) {
        XLOGD("show low battery logo");
        mt65xx_disp_show_low_battery();
        return;
    }
    
    if (cnt == 1) {
        if (capacity < 25)
            bc_offset = 0;
        else if (capacity < 50)
            bc_offset = 25;
        else if (capacity < 75)
            bc_offset = 50;
        else if (capacity < 100)
            bc_offset = 75;
        else
            bc_offset = 100;
    }

    base = (int) (cnt / 5);
    bc = bc_offset + (base*25)%(125-bc_offset);

    XLOGD("[ChargingAnimation %s %d]base: %d, bc: %d",__FUNCTION__,__LINE__, base, bc);

#ifdef ANIMATION_NEW

    if(old_logo_bin >0) 
    {
        // add by @mtk54040 logo size is  small, show old aniamtion 
        mt65xx_disp_show_battery_capacity(bc);  
    } else {
        mt65xx_disp_show_battery_capacity(capacity);
    }

#else
        mt65xx_disp_show_battery_capacity(bc);
#endif
    //LOGI("bootlogo_show_charging, done");
}




void show_kernel_logo_surface()

{
    status_t  lockResult;
    Region    region(Rect(0, 0, x_virtual, vinfo.yres));
    lockResult = surface->lock(&info, &region);  
    XLOGD("[ChargingAnimation %s %d]info.bits = 0x%08x\n", __FUNCTION__,__LINE__,info.bits);
    XLOGD("[ChargingAnimation %s %d]surface->lock return = 0x%08x, %d\n",__FUNCTION__,__LINE__,lockResult,lockResult);
    if (0 == lockResult)
    {
        unsigned short *s = rgb565_logo;
        UINT16 *ptr = (UINT16 *)info.bits;  

        int j = 0;
        int k = 0;

        int tw= ALIGN_TO(dinfo_width,32);
        int th= dinfo_height;
        XLOGD("[ChargingAnimation %s %d]tw=%d,th=%d", __FUNCTION__,__LINE__,tw,th);

        for (j = 0; j < th; ++ j)
        {
            for(k=0;k<dinfo_width;k++) 
                ptr[j*tw+k] = *s++;
        }
    }
    surface->unlockAndPost();

}
void show_kernel_logo_fb()
{

    XLOGD("[ChargingAnimation %s %d]vinfo.yres= %d,vinfo.xres = %d, x_virtual = %d\n",__FUNCTION__,__LINE__,
         vinfo.yres, vinfo.xres , x_virtual);

    unsigned short src_rgb565 = 0;
    change_fb_addr();

    unsigned short *s = rgb565_logo;
    unsigned int   *d = (unsigned int *)fb_addr;
    unsigned int  *fbbuf = d;
    unsigned int j = 0;
    unsigned int k = 0;

    if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "270", 3))
    {
        XLOGD("[ChargingAnimation %s %d]270",__FUNCTION__,__LINE__);
        unsigned int l;
        for (j=0; j<vinfo.xres; j++){
            for (k=0, l=vinfo.yres-1; k<vinfo.yres; k++, l--)
            {
                src_rgb565 = *s++;
                d = fbbuf + ((x_virtual * l + j) << 2);
                *d = RGB565_TO_ARGB8888(src_rgb565);
            }
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "90", 2))
    {
        XLOGD("[ChargingAnimation %s %d]90\n",__FUNCTION__,__LINE__);
        unsigned int l;
        for (j=vinfo.xres - 1; j>=0; j--){
            for (k=0, l=0; k<vinfo.yres; k++, l++)
            {
                src_rgb565 = *s++;
                d = fbbuf + ((x_virtual * l + j) << 2);
                *d = RGB565_TO_ARGB8888(src_rgb565);
            }
        }
    }
    else if(0 == strncmp(MTK_LCM_PHYSICAL_ROTATION, "180", 3))
    {
        XLOGD("[ChargingAnimation %s %d]180\n",__FUNCTION__,__LINE__); 
        unsigned int l;
        unsigned int height = vinfo.yres;      
        unsigned int width = vinfo.xres;

        unsigned short *src = (unsigned short*)s + ((height - 1) * width);
        unsigned int *dst = d;
        //UINT16 *pLine2 = (UINT16*)addr;
        for (j = 0; j < height; ++ j) {
          for (k = 0; k < width; ++ k) {
                src_rgb565 = *(src+width-k);
             *(dst+k) = RGB565_TO_ARGB8888(src_rgb565);
          }
          for (k = width; k < x_virtual; ++ k) {
            *(dst+k) = 0xFFFFFFFF;
          }
          dst += x_virtual;
          src -= width;
       }
    }
    else

    {
        XLOGD("[ChargingAnimation %s %d]normal 0 \n",__FUNCTION__,__LINE__);
        unsigned int l;
        for (j = 0; j < vinfo.yres; ++ j){
            for(k = 0,l=0; k < vinfo.xres; ++ k,l++)
            {
                src_rgb565 = *s++;
                d = fbbuf + ((x_virtual * l + k) << 2);
                *d = RGB565_TO_ARGB8888(src_rgb565);

            }
        }
        XLOGD("[[ChargingAnimation %s %d] loop copy color over\n",__FUNCTION__,__LINE__);
    }

    mt65xx_disp_update();


}

void boot_logo_updater()
{
    XLOGD("[ChargingAnimation %s %d]running_kpoc_mode=%d, show  kernel logo \n",__FUNCTION__,__LINE__,running_kpoc_mode);
    
    if (running_kpoc_mode == 1){
        show_kernel_logo_fb();
    } else {
        show_kernel_logo_surface(); 
    }
}
