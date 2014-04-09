#include <linux/types.h>
#include <mach/mt_pm_ldo.h>
#include <cust_alsps.h>


static struct alsps_hw cust_alsps_hw = {
	.i2c_num    = 0,
	.polling_mode_ps =0, //1,
	.power_id   = MT65XX_POWER_NONE,    /*LDO is not used*/
	.power_vol  = VOL_DEFAULT,          /*LDO is not used*/
	.i2c_addr   = {0x0C, 0x48, 0x78, 0x00},
	.als_level	= {9,  27, 54, 72, 90, 180, 360, 540, 720, 900, 1800, 3600, 5400, 7200, 10800},
	.als_value	= {10, 30, 60, 80, 100, 200, 400, 600, 800, 1000,  2000,  4000, 6000, 8000, 12000, 20000},
	.ps_threshold_low = 100,
	.ps_threshold_high = 500,
};
struct alsps_hw *get_cust_alsps_hw(void) {
    return &cust_alsps_hw;
}

