#ifndef _MT_GPIO_CORE_H_
#define _MT_GPIO_CORE_H_

#include <mach/mt_gpio_ext.h>

#if defined (CONFIG_MT_GPIO_FPGA_ENABLE)
#include <mach/mt_gpio_fpga.h>
#else
#include <mach/mt_gpio_base.h>
#endif

/******************************************************************************
 MACRO Definition
******************************************************************************/
#define GPIO_DEVICE "mt-gpio"
#define VERSION     GPIO_DEVICE
/*---------------------------------------------------------------------------*/
//#define GPIOTAG                "[GPIO] "
//#define GPIOLOG(fmt, arg...)   printk(GPIOTAG fmt, ##arg)
//#define GPIOMSG(fmt, arg...)   printk(fmt, ##arg)
//#define GPIOERR(fmt, arg...)   printk(KERN_ERR GPIOTAG "%5d: "fmt, __LINE__, ##arg)
//#define GPIOFUC(fmt, arg...)   //printk(GPIOTAG "%s\n", __FUNCTION__)
#define GPIO_RETERR(res, fmt, args...)                                               \
    do {                                                                             \
        printk(KERN_ERR GPIOTAG "%s:%04d: " fmt"\n", __FUNCTION__, __LINE__, ##args);\
        return res;                                                                  \
    } while(0)
#define GIO_INVALID_OBJ(ptr)   ((ptr) != mt_gpio)

static enum{
	MT_BASE = 0,
	MT_EXT,
	MT_NOT_SUPPORT,
}; 
#define MT_GPIO_PLACE(pin) ({\
	int ret = -1;\
	if((pin >= MT_GPIO_BASE_START) && (pin < MT_GPIO_BASE_MAX)){\
		ret = MT_BASE;\
		/*GPIOLOG("pin in base is %d\n",pin);*/ \
	}else if((pin >= MT_GPIO_EXT_START) && (pin < MT_GPIO_EXT_MAX)){\
		ret = MT_EXT;\
		GPIOLOG("pin in ext is %d\n",pin);\
	}else{\
		GPIOERR("Pin number error %d\n",pin);	\
		ret = -1;\
	}\
	ret;})
//int where_is(unsigned long pin) 
//{
//	int ret = -1;
////	GPIOLOG("pin is %d\n",pin);
//	if((pin >= MT_GPIO_BASE_START) && (pin < MT_GPIO_BASE_MAX)){
//		ret = MT_BASE;
//		//GPIOLOG("pin in base is %d\n",pin);
//	}else if((pin >= MT_GPIO_EXT_START) && (pin < MT_GPIO_EXT_MAX)){
//		ret = MT_EXT;
//		//GPIOLOG("pin in ext is %d\n",pin);
//	}else{
//		GPIOERR("Pin number error %d\n",pin);	
//		ret = -1;
//	}
//	return ret;	
//}
/*decrypt pin*/

#endif //_MT_GPIO_CORE_H_
