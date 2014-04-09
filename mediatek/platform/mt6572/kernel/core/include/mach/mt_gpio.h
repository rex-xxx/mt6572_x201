#ifndef __MT_GPIO_H__
#define __MT_GPIO_H__

//mark for delete, need remove other driver build error
#include <linux/ioctl.h>
#include <linux/fs.h>

#if (defined(CONFIG_MT6572_FPGA))
#define CONFIG_MT_GPIO_FPGA_ENABLE
#else
// FIX-ME: marked for early porting
#include <cust_gpio_usage.h>
#endif

//For autogen[Lomen]
#include <mach/gpio_const.h>


#define GPIOTAG                "[GPIO] "
#define GPIOLOG(fmt, arg...)   printk(GPIOTAG fmt, ##arg)
#define GPIOMSG(fmt, arg...)   printk(fmt, ##arg)
#define GPIOERR(fmt, arg...)   printk(KERN_ERR GPIOTAG "%5d: "fmt, __LINE__, ##arg)
#define GPIOFUC(fmt, arg...)   //printk(GPIOTAG "%s\n", __FUNCTION__)
/*----------------------------------------------------------------------------*/
//  Error Code No.
#define RSUCCESS        0
#define ERACCESS        1
#define ERINVAL         2
#define ERWRAPPER	3
    
#define MT_GPIO_MAX_PIN GPIO_MAX
/******************************************************************************
* Enumeration for GPIO pin
******************************************************************************/
/* GPIO MODE CONTROL VALUE*/
typedef enum {
    GPIO_MODE_UNSUPPORTED = -1,
    GPIO_MODE_GPIO  = 0,
    GPIO_MODE_00    = 0,
    GPIO_MODE_01    = 1,
    GPIO_MODE_02    = 2,
    GPIO_MODE_03    = 3,
    GPIO_MODE_04    = 4,
    GPIO_MODE_05    = 5,
    GPIO_MODE_06    = 6,
    GPIO_MODE_07    = 7,

    GPIO_MODE_MAX,
    GPIO_MODE_DEFAULT = GPIO_MODE_01,
} GPIO_MODE;
/*----------------------------------------------------------------------------*/
/* GPIO DIRECTION */
typedef enum {
    GPIO_DIR_UNSUPPORTED = -1,
    GPIO_DIR_IN     = 0,
    GPIO_DIR_OUT    = 1,

    GPIO_DIR_MAX,
    GPIO_DIR_DEFAULT = GPIO_DIR_IN,
} GPIO_DIR;
/*----------------------------------------------------------------------------*/
/* GPIO PULL ENABLE*/
typedef enum {
    GPIO_PULL_EN_UNSUPPORTED = -1,
    GPIO_PULL_DISABLE = 0,
    GPIO_PULL_ENABLE  = 1,

    GPIO_PULL_EN_MAX,
    GPIO_PULL_EN_DEFAULT = GPIO_PULL_ENABLE,
} GPIO_PULL_EN;
/*----------------------------------------------------------------------------*/
/* GPIO IES*/
typedef enum {
    GPIO_IES_UNSUPPORTED = -1,
    GPIO_IES_DISABLE = 0,
    GPIO_IES_ENABLE  = 1,

    GPIO_IES_MAX,
    GPIO_IES_DEFAULT = GPIO_IES_ENABLE,
} GPIO_IES;
/*----------------------------------------------------------------------------*/
/* GPIO PULL-UP/PULL-DOWN*/
typedef enum {
    GPIO_PULL_UNSUPPORTED = -1,
    GPIO_PULL_DOWN  = 0,
    GPIO_PULL_UP    = 1,

    GPIO_PULL_MAX,
    GPIO_PULL_DEFAULT = GPIO_PULL_DOWN
} GPIO_PULL;
/*----------------------------------------------------------------------------*/
/* GPIO INVERSION */
typedef enum {
    GPIO_DATA_INV_UNSUPPORTED = -1,
    GPIO_DATA_UNINV = 0,
    GPIO_DATA_INV   = 1,

    GPIO_DATA_INV_MAX,
    GPIO_DATA_INV_DEFAULT = GPIO_DATA_UNINV
} GPIO_INVERSION;
/*----------------------------------------------------------------------------*/
/* GPIO OUTPUT */
typedef enum {
    GPIO_OUT_UNSUPPORTED = -1,
    GPIO_OUT_ZERO = 0,
    GPIO_OUT_ONE  = 1,

    GPIO_OUT_MAX,
    GPIO_OUT_DEFAULT = GPIO_OUT_ZERO,
    GPIO_DATA_OUT_DEFAULT = GPIO_OUT_ZERO,  /*compatible with DCT*/
} GPIO_OUT;
/*----------------------------------------------------------------------------*/
/* GPIO INPUT */
typedef enum {
    GPIO_IN_UNSUPPORTED = -1,
    GPIO_IN_ZERO = 0,
    GPIO_IN_ONE  = 1,

    GPIO_IN_MAX,
} GPIO_IN;
/*----------------------------------------------------------------------------*/
/* GPIO POWER*/
typedef enum {
    GPIO_VIO28 = 0,
    GPIO_VIO18 = 1,

    GPIO_VIO_MAX,
} GPIO_POWER;


/******************************************************************************
* Enumeration for Clock output
******************************************************************************/
/*CLOCK OUT*/
typedef enum {
    CLK_OUT_UNSUPPORTED = -1,
    CLK_OUT0,
    CLK_OUT1,
    CLK_OUT2,
    CLK_OUT3,
    CLK_OUT4,
    CLK_OUT5,
    CLK_OUT6,
    CLK_MAX	
}GPIO_CLKOUT;
typedef enum {
    CLKM_UNSUPPORTED = -1,
    CLKM0,
    CLKM1,
    CLKM2,
    CLKM3,
    CLKM4,
    CLKM5,
    CLKM6,
}GPIO_CLKM;
/*----------------------------------------------------------------------------*/
typedef enum CLK_SRC
{
    CLK_SRC_UNSUPPORTED = -1,	
    CLK_SRC_GATE 	= 0x0,
    CLK_SRC_SYS_26M,
    CLK_SRC_FRTC,
    CLK_SRC_WHPLL_250P25M,
    CLK_SRC_WPLL_245P76M,
    CLK_SRC_MDPLL2_416,
    CLK_SRC_MDPLL1_416,
    CLK_SRC_MCUPLL2_H481M,
    CLK_SRC_MCUPLL1_H481M,
    CLK_SRC_MSDC_H208M,
    CLK_SRC_ISP_208M,
    CLK_SRC_LVDS_H180M,
    CLK_SRC_TVHDMI_H,
    CLK_SRC_UPLL_178P3M,
    CLK_SRC_MAIN_H230P3M,
    CLK_SRC_MM_DIV7,

    CLK_SRC_MAX
}GPIO_CLKSRC;
    
/*----------------------------------------------------------------------------*/
//typedef struct {        /*FIXME: check GPIO spec*/
//    unsigned int no     : 16;
//    unsigned int mode   : 3;    
//    unsigned int pullsel: 1;
//    unsigned int din    : 1;
//    unsigned int dout   : 1;
//    unsigned int pullen : 1;
//    unsigned int dir    : 1;
//    unsigned int dinv   : 1;
//    unsigned int _align : 7; 
//} GPIO_CFG; 
/******************************************************************************
* GPIO Driver interface 
******************************************************************************/
/*direction*/
int mt_set_gpio_dir(unsigned long pin, unsigned long dir);
int mt_get_gpio_dir(unsigned long pin);

/*pull enable*/
int mt_set_gpio_pull_enable(unsigned long pin, unsigned long enable);
int mt_get_gpio_pull_enable(unsigned long pin);
/*IES*/
int mt_set_gpio_ies(unsigned long pin, unsigned long enable);
int mt_get_gpio_ies(unsigned long pin);
/*pull select*/
int mt_set_gpio_pull_select(unsigned long pin, unsigned long select);    
int mt_get_gpio_pull_select(unsigned long pin);

/*data inversion*/
int mt_set_gpio_inversion(unsigned long pin, unsigned long enable);
int mt_get_gpio_inversion(unsigned long pin);

/*input/output*/
int mt_set_gpio_out(unsigned long pin, unsigned long output);
int mt_get_gpio_out(unsigned long pin);
int mt_get_gpio_in(unsigned long pin);

/*mode control*/
int mt_set_gpio_mode(unsigned long pin, unsigned long mode);
int mt_get_gpio_mode(unsigned long pin);

/*clock output setting*/
int mt_set_clock_output(unsigned long num, unsigned long src, unsigned long div);
int mt_get_clock_output(unsigned long num, unsigned long *src, unsigned long *div);

/*For MD GPIO customization only, can be called by CCCI driver*/
int mt_get_md_gpio(char * gpio_name, int len);

/*misc functions for protect GPIO*/
//void mt_gpio_dump(GPIO_REGS *regs,GPIOEXT_REGS *regs_ext);
void gpio_dump_regs(void);

#endif //__MT_GPIO_H__
