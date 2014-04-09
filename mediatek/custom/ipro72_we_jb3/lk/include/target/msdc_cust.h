/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2010
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

#ifndef _MSDC_CUST_H_
#define _MSDC_CUST_H_

#include "msdc_cfg.h"

#define MSDC_CD_PIN_EN      (1 << 0)  /* card detection pin is wired   */
#define MSDC_WP_PIN_EN      (1 << 1)  /* write protection pin is wired */
#define MSDC_RST_PIN_EN     (1 << 2)  /* emmc reset pin is wired       */
#define MSDC_SDIO_IRQ       (1 << 3)  /* use internal sdio irq (bus)   */
#define MSDC_EXT_SDIO_IRQ   (1 << 4)  /* use external sdio irq           */
#define MSDC_REMOVABLE      (1 << 5)  /* removable slot            */
#define MSDC_SYS_SUSPEND    (1 << 6)  /* suspended by system           */
#define MSDC_HIGHSPEED      (1 << 7)  /* high-speed mode support       */
#define MSDC_UHS1           (1 << 8)  /* uhs-1 mode support        */
#define MSDC_DDR            (1 << 9)  /* ddr mode support          */
#define MSDC_HS200          (1 << 10)  /* hs200 mode support(eMMC4.5)        */

#define MSDC_SMPL_RISING    (0)
#define MSDC_SMPL_FALLING   (1)

#define MSDC_CUST_VDD       (MMC_VDD_32_33)

//For DDR2-533
typedef enum
{
                                    //DDR2-533  Source-PLL   Hopping
    MSDC_CLKSRC_133MHZ = 0,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_160MHZ = 1,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_200MHZ = 2,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_178MHZ = 3,         //    Y     UNIVPLL        N
    MSDC_CLKSRC_189MHZ = 4,         //    N     MAINVPLL       Y
    MSDC_CLKSRC_NONE   = 5,         //    Y     MAINVPLL       Y //Actually, HW use this index to 200MHz too
    MSDC_CLKSRC_26MHZ  = 6,         //    N     MAINVPLL       Y
    MSDC_CLKSRC_208MHZ = 7,         //    Y     UNIVPLL        N
    MSDC_CLKSRC_MAX
} clk_source_t;

// For DDR3-663
/*
typedef enum
{
                                    //DDR3-663  Source-PLL   Hopping
    MSDC_CLKSRC_NONE1  = 0,         //    N     MAINVPLL       Y
    MSDC_CLKSRC_133MHZ = 1,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_166MHZ = 2,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_178MHZ = 3,         //    Y     UNIVPLL        N
    MSDC_CLKSRC_189MHZ = 4,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_NONE   = 5,         //    N     MAINVPLL       Y //Actually, HW use this index to 200MHz too
    MSDC_CLKSRC_26MHZ  = 6,         //    Y     MAINVPLL       Y
    MSDC_CLKSRC_208MHZ = 7,         //    Y     UNIVPLL        N
    MSDC_CLKSRC_MAX
} clk_source_t;
*/

#define MSDC_CLKSRC_DEFAULT     MSDC_CLKSRC_200MHZ
extern unsigned int msdc_src_clks[];

struct msdc_cust {
    unsigned char  clk_src;       /* host clock source         */
    unsigned char  cmd_edge;      /* command latch edge        */
    unsigned char  data_edge;     /* data latch edge           */
    unsigned char  clk_drv;       /* clock pad driving         */
    unsigned char  cmd_drv;       /* command pad driving       */
    unsigned char  dat_drv;       /* data pad driving          */
    unsigned char  clk_18v_drv;   /* clock pad driving         */
    unsigned char  cmd_18v_drv;   /* command pad driving on 1.8V   */
    unsigned char  dat_18v_drv;   /* data pad driving on 1.8V  */
    unsigned char  data_pins;     /* data pins on 1.8V         */
    unsigned int   data_offset;   /* data address offset       */
    unsigned int   flags;         /* hardware capability flags */
};

extern struct msdc_cust msdc_cap;
#if defined(MMC_MSDC_DRV_CTP) || defined(FEATURE_MMC_MEM_PRESERVE_MODE)
struct msdc_cust msdc_cap_removable;
#endif

#define CMD_RETRIES         (5)
#define CMD_TIMEOUT         (100)           /* 100ms */
#define TMO_IN_CLK_2POWER   20          //2^20=1048576 For MT6572/89
//#define TMO_IN_CLK_2POWER    16       //2^16=65536   For MT6575/77

#define EMMC_BOOT_TMO_IN_CLK_2POWER    16 //2^16=65536   For MT6572/897

/* Tuning Parameter */
#define DEFAULT_DEBOUNCE    (8)     /* 8 cycles */
#define DEFAULT_DTOC        (3)     /* data timeout counter. 3xTMO_IN_CLK sclk*/
#define DEFAULT_WDOD        (0)     /* write data output delay. no delay. */
#define DEFAULT_BSYDLY      (8)     /* card busy delay. 8 extend sclk */

#if defined(MMC_MSDC_DRV_CTP)
    #define MAX_GPD_POOL_SZ     (512)
    #define MAX_BD_POOL_SZ      (1024)

#else
    #define MAX_GPD_POOL_SZ     (2) /* include null gpd */
    #define MAX_BD_POOL_SZ      (4)
#endif


#define MAX_DMA_CNT         (32768)
#define MAX_SG_POOL_SZ      (MAX_BD_POOL_SZ)
#define MAX_SG_BUF_SZ       (MAX_DMA_CNT)    /* sg size = DMA size */
#define MAX_BD_PER_GPD      (MAX_BD_POOL_SZ) /* only one gpd for all bd */
//#define MAX_BD_PER_GPD      (MAX_BD_POOL_SZ/(MAX_GPD_POOL_SZ-1)) /* except null gpd */
#define MAX_DMA_TRAN_SIZE   (MAX_SG_POOL_SZ*MAX_SG_BUF_SZ)

#if MAX_SG_BUF_SZ > MAX_DMA_CNT
#error "incorrect max sg buffer size"
#endif


#endif /* end of _MSDC_CUST_H_ */

