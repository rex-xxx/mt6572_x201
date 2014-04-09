#include <mach/mtk_musb.h>
#include <mach/mt_clkmgr.h>
#include <linux/jiffies.h>
#include <linux/delay.h>
#include <asm/io.h>
#include <linux/spinlock.h>
//#include <drivers/devinfo/devinfo.h>

extern u32 get_devinfo_with_index(u32 index);

/*
// PMIC will prepare the dummy functions for us
#ifdef CONFIG_MT6589_FPGA
CHARGER_TYPE mt_charger_type_detection(void){return 0;}
bool upmu_is_chr_det(void){return 0;}
void BATTERY_SetUSBState(int usb_state){}
void upmu_interrupt_chrdet_int_en(kal_uint32 val){}
#endif
*/
#define FRA (48)
#define PARA (28)

#define USB_MODE (0)
#define UART_MODE (1)
static int cur_usb_mode = USB_MODE;

#ifdef CONFIG_EARLY_LINUX_PORTING
bool usb_enable_clock(bool enable)
{
}

void usb_phy_poweron(void)
{
}

void usb_phy_savecurrent(void)
{
}

void usb_phy_recover(void)
{
}

// BC1.2
void Charger_Detect_Init(void)
{
}

void Charger_Detect_Release(void)
{
}	

void musb_phy_context_restore(void)
{
}

void usb_phy_switch_to_uart(void)
{
}

void usb_phy_switch_to_usb(void)
{
}


#else

static DEFINE_SPINLOCK(musb_reg_clock_lock);

bool usb_enable_clock(bool enable) 
{
	static int count = 0;
	bool res = TRUE;
	unsigned long flags;

	spin_lock_irqsave(&musb_reg_clock_lock, flags);

	if (enable && count == 0) {
		/*
		 * USB CG may default on. 
		 * To prevent clk_mgr reference count error. MUST CHECK is clock on?
		 */
		if(!clock_is_on(MT_CG_USB_SW_CG)){
			res = enable_clock(MT_CG_USB_SW_CG, "PERI_USB");
		}
	} else if (!enable && count == 1) {
		res = disable_clock(MT_CG_USB_SW_CG, "PERI_USB");
	}

	if (enable)
		count++;
	else
		count = (count==0) ? 0 : (count-1);

	spin_unlock_irqrestore(&musb_reg_clock_lock, flags);

	printk(KERN_DEBUG "enable(%d), count(%d) res=%d\n", enable, count, res);

	return 1;
}

void hs_slew_rate_cal(void){
  
  unsigned char valid_reg;
  unsigned long data;
  unsigned long x;
  unsigned char value;
  unsigned long start_time, timeout;
  unsigned int timeout_flag = 0;
  //4 s1:enable usb ring oscillator.
  USBPHY_WRITE8(0x15,0x80);

  //4 s2:wait 1us.
  udelay(1);

  //4 s3:enable free run clock
  USBPHY_WRITE8 (0xf00-0x800+0x11,0x01);
  //4 s4:setting cyclecnt.
  USBPHY_WRITE8 (0xf00-0x800+0x01,0x04);
  //4 s5:enable frequency meter
  USBPHY_SET8 (0xf00-0x800+0x03,0x01);

  //4 s6:wait for frequency valid.
  start_time = jiffies;
  timeout = jiffies + 3 * HZ;

  while(!(USBPHY_READ8(0xf00-0x800+0x10)&0x1)){
    if(time_after(jiffies, timeout)){
        timeout_flag = 1;
        break;
        }
    }

  //4 s7: read result.
  if(timeout_flag){
    printk("[USBPHY] Slew Rate Calibration: Timeout\n");
    value = 0x4;
    }
  else{
      data = USBPHY_READ32 (0xf00-0x800+0x0c);
      x = ((1024*FRA*PARA)/data);
      value = (unsigned char)(x/1000);
      if((x-value*1000)/100>=5)
        value += 1;
        printk("[USBPHY]slew calibration:FM_OUT =%lu,x=%lu,value=%d\n",data,x,value);
    }

  //4 s8: disable Frequency and run clock.
  USBPHY_CLR8 (0xf00-0x800+0x03,0x01);//disable frequency meter
  USBPHY_CLR8 (0xf00-0x800+0x11,0x01);//disable free run clock

  //4 s9:
  //USBPHY_WRITE8(0x15,value);
  USBPHY_WRITE8(0x15, (value<<4));

  //4 s10:disable usb ring oscillator.
  USBPHY_CLR8(0x15,0x80);
}


bool usb_phy_check_in_uart_mode()
{
	bool ret;

	//usb_enable_clock(true);
	if ((USBPHY_READ8(0x6B)&0x5C) == 0x5C)
	{
		cur_usb_mode = UART_MODE;
		ret = true;
	}
	else
	{
		ret = false;
	}

	//usb_enable_clock(false);
	return ret;
}

void usb_phy_switch_to_uart(void)
{
	usb_enable_clock(true);
	if (usb_phy_check_in_uart_mode())
		return;
	
	cur_usb_mode = UART_MODE;
	
	/* RG_USB20_BC11_SW_EN = 1'b0 */
	USBPHY_CLR8(0x1a, 0x80);
	
	/* Set ru_uart_mode to 2'b01 */
	USBPHY_SET8(0x6B, 0x5C);

	/* Set RG_UART_EN to 1 */
	USBPHY_SET8(0x6E, 0x07);

	/* Set RG_USB20_DM_100K_EN to 1 */
	USBPHY_SET8(0x22, 0x02);
	
	/* Set RG_SUSPENDM to 1 */
	USBPHY_SET8(0x68, 0x08);
	
	/* force suspendm = 1 */
	USBPHY_SET8(0x6a, 0x04);
	
	/* EN_PU_DM = 1*/
	USBPHY_SET8(0x1d, 0x18);
	usb_enable_clock(false);
	
	/* GPIO Selection */
	mdelay(100);
	DRV_WriteReg32(GPIO_BASE + 0x504, 0x20);	//set
}


void usb_phy_switch_to_usb(void)
{
	usb_enable_clock(true);
	if (!usb_phy_check_in_uart_mode())
		return;
		
	cur_usb_mode = USB_MODE;
		
	/* GPIO Selection */
	DRV_WriteReg32(GPIO_BASE + 0x508, 0x20);	//clear
	mdelay(200);
	
	/*cler force_uart_xxx*/  
	USBPHY_WRITE8(0x6B, 0x00);
	
	/* EN_PU_DM = 0*/
	USBPHY_CLR8(0x1d, 0x18);
	usb_enable_clock(false);
	
	udelay(1000);
	usb_phy_poweron();
}

void usb_phy_poweron()
{	
  //turn on USB reference clock. 
  usb_enable_clock(true);
  
  if (usb_phy_check_in_uart_mode())
		return;

  //wait 50 usec.
  udelay(50);    
  
	/*
	 * swtich to USB function.
	 * (system register, force ip into usb mode).
	 */
	USBPHY_CLR8(0x6b, 0x04);
	USBPHY_CLR8(0x6e, 0x01);

	/* RG_USB20_BC11_SW_EN = 1'b0 */
	USBPHY_CLR8(0x1a, 0x80);

  /*DP, DM 100K disable*/
	USBPHY_CLR8(0x22, 0x03);

	/* release force suspendm */
	USBPHY_CLR8(0x6a, 0x04);

	udelay(800);
	
// force enter device mode
	USBPHY_CLR8(0x6c, 0x10);
	USBPHY_SET8(0x6c, 0x2E);
	USBPHY_SET8(0x6d, 0x3E);
			
  printk("usb power on success\n");
  return;
}

void usb_phy_savecurrent(){
	if (usb_phy_check_in_uart_mode())
		return;
	
	/*
	 * swtich to USB function.
	 * (system register, force ip into usb mode).
	 */
	USBPHY_CLR8(0x6b, 0x04);
	USBPHY_CLR8(0x6e, 0x01);

	/* release force suspendm */
	USBPHY_CLR8(0x6a, 0x04);
	/* RG_DPPULLDOWN./RG_DMPULLDOWN. */
	USBPHY_SET8(0x68, 0xc0);
	/* RG_XCVRSEL[1:0] = 2'b01 */
	USBPHY_CLR8(0x68, 0x30);
	USBPHY_SET8(0x68, 0x10);
	/* RG_TERMSEL = 1'b1 */
	USBPHY_SET8(0x68, 0x04);
	/* RG_DATAIN[3:0] = 4'b0000 */
	USBPHY_CLR8(0x69, 0x3c);

	/*
	 * force_dp_pulldown, force_dm_pulldown, 
	 * force_xcversel, force_termsel.
	 */
	USBPHY_SET8(0x6a, 0xba);

	/* RG_USB20_BC11_SW_EN = 1'b0 */
	USBPHY_CLR8(0x1a, 0x80);
	/* RG_USB20_OTG_VBUSSCMP_EN = 1'b0 */
	USBPHY_CLR8(0x1a, 0x10);

	udelay(800);

	/* rg_usb20_pll_stable = 1 */
	USBPHY_SET8(0x63, 0x02);

	udelay(1);

	/* force suspendm = 1 */
	USBPHY_SET8(0x6a, 0x04);

	udelay(1);

// force enter device mode
	USBPHY_CLR8(0x6c, 0x10);
	USBPHY_SET8(0x6c, 0x2E);
	USBPHY_SET8(0x6d, 0x3E);
	
    //4 14. turn off internal 48Mhz PLL.
    usb_enable_clock(false);

  printk("usb save current success\n");
  return;
}

void usb_phy_recover(){

  //turn on USB reference clock. 
  usb_enable_clock(true);
  
  if (usb_phy_check_in_uart_mode())
		return;
		
  //wait 50 usec.
  udelay(50);

	/* force_uart_en = 1'b0 */
	USBPHY_CLR8(0x6b, 0x04);
	/* RG_UART_EN = 1'b0 */
	USBPHY_CLR8(0x6e, 0x01);
	/* force_uart_en = 1'b0 */
	USBPHY_CLR8(0x6a, 0x04);
  /* RG_DPPULLDOWN./RG_DMPULLDOWN. */
  /* RG_XCVRSEL[1:0] = 2'b00 */
  /* RG_TERMSEL = 1'b0 */
	USBPHY_CLR8(0x68, 0xf4);
	/* RG_DATAIN[3:0] = 4'b0000 */
	USBPHY_CLR8(0x69, 0x3c);
	/*
	 * force_dp_pulldown, force_dm_pulldown, 
	 * force_xcversel, force_termsel.
	 */
	USBPHY_CLR8(0x6a, 0xba);

	/* RG_USB20_BC11_SW_EN = 1'b0 */
	USBPHY_CLR8(0x1a, 0x80);
	/* RG_USB20_OTG_VBUSSCMP_EN = 1'b1 */
	USBPHY_SET8(0x1a, 0x10);

	udelay(800);
    
// force enter device mode
	USBPHY_CLR8(0x6c, 0x10);
	USBPHY_SET8(0x6c, 0x2E);
	USBPHY_SET8(0x6d, 0x3E);
  
  hs_slew_rate_cal();
  
  //RG_USB20_VRT_VREF_SEL[2:0]=5 (ori:4) (0x11110804[14:12])
  USBPHY_SET8(0x05, 0x10);
  //RG_USB20_TERM_VREF_SEL[2:0]=5 (ori:4) (0x11110804[10:8])
  USBPHY_SET8(0x05, 0x01);
  

	printk("USB HW reg: index18=0x%x, index7=0x%x\n", get_devinfo_with_index(18), get_devinfo_with_index(7));  
  if (get_devinfo_with_index(18) & (0x01<<14))
	{
		USBPHY_CLR8(0x00, 0x20);
		printk("USB HW reg: write RG_USB20_INTR_EN 0x%x\n", USBPHY_READ8(0x00));
	}
	
	if (get_devinfo_with_index(7) & (0x07<<8))
	{
		//RG_USB20_VRT_VREF_SEL[2:0]=5 (ori:4) (0x11110804[14:12])
		USBPHY_CLR8(0x05, 0x70);
  	USBPHY_SET8(0x05, ((get_devinfo_with_index(7)>>8)<<4)&0x70);
  	printk("USB HW reg: overwrite RG_USB20_VRT_VREF_SEL 0x%x\n", USBPHY_READ8(0x05));
	}

  printk("usb recovery success\n");
  return;
}

// BC1.2
void Charger_Detect_Init(void)
{
	//turn on USB reference clock. 
  usb_enable_clock(true);
  //wait 50 usec.
  udelay(50);
  
	/* RG_USB20_BC11_SW_EN = 1'b1 */
  USBPHY_SET8(0x1a, 0x80);
}

void Charger_Detect_Release(void)
{
  /* RG_USB20_BC11_SW_EN = 1'b0 */
  USBPHY_CLR8(0x1a, 0x80);
  udelay(1);
  //4 14. turn off internal 48Mhz PLL.
  usb_enable_clock(false);
}	


#define DBG_PRB0 (USB_BASE + 0x0620)    /* USB Debug Probe Register 0 */
#define DBG_PRB4 (USB_BASE + 0x0630)    /* USB Debug Probe Register 4 */
void musb_phy_context_restore(void)
{
	unsigned char tmpReg8;
	
  enable_pll(UNIVPLL, "USB_PLL");
  
 
	if(cur_usb_mode == UART_MODE)
	{
		usb_phy_switch_to_uart();
	}
	else
	{
		usb_phy_savecurrent();
	}  
  disable_pll(UNIVPLL,"USB_PLL");
}

#endif

