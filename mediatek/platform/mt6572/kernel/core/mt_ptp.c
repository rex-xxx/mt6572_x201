#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/proc_fs.h>
#include <linux/spinlock.h>
#include <linux/interrupt.h>
#include <linux/syscore_ops.h>
#include <linux/platform_device.h>

#include "mach/mt_typedefs.h"
#include "mach/irqs.h"
#include "mach/mt_ptp.h"
#include "mach/mt_reg_base.h"
#include "mach/mt_cpufreq.h"
#include "mach/mt_thermal.h"
#include "devinfo.h"
#include "mach/mt_clkmgr.h"

#define PTP_Get_Real_Val 0
#define En_PTPOD         0

static unsigned int val_0 = 0x14f76907;
static unsigned int val_1 = 0xf6AAAAAA;
static unsigned int val_2 = 0x14AAAAAA;
static unsigned int val_3 = 0x60260000;

static unsigned int PTP_DCVOFFSET = 0;
static unsigned int PTP_AGEVOFFSET = 0;
static unsigned int CPU_Freq[MAX_OPP_NUM];
static unsigned int hw_calc_start = true;
static unsigned int PTP_Enable = 1;
static unsigned int PTP_INIT_FLAG = 0;
unsigned int PTP_V[MAX_OPP_NUM];
unsigned int PTP_MON_V[MAX_OPP_NUM];
static PTP_Init_T PTP_Init_value;

static int ptp_init_setup(enum init_type mode, PTP_Init_T* PTP_Init_val);
irqreturn_t PTP_ISR(int irq, void *dev_id);
static void ptp_mon(void);


unsigned int get_ptp_level(){
    unsigned int ver;
    ver = get_devinfo_with_index(PTP_LEVEL_INDEX);
    ver = (ver >> 8) & 0x00000003;
    return ver;
}
static void PTP_set_ptp_volt(unsigned int volt, unsigned int freq)
{
	  if(freq != 0){
	  	  ptp_print("set voltage: 0x%x frequency: %d\n", volt, freq);
        set_spm_tbl(volt, freq);
    }
}


irqreturn_t PTP_ISR(int irq, void *dev_id)
{
    unsigned int PTPINTSTS, temp, ptpen, temperature, i, loop;
    unsigned int dcvalues, agecount;
    bool PTP_Change[MAX_OPP_NUM];           

    PTPINTSTS = ptp_read(PTP_PTPINTSTS);
    dcvalues = ptp_read(PTP_DCVALUES);
    agecount = ptp_read(PTP_AGECOUNT);
    ptpen = ptp_read(PTP_PTPEN);

    ptp_print("[ISR Entry Point]\n");
    ptp_print("PTPINTSTS = 0x%x\n", PTPINTSTS);
    ptp_print("PTP_DCVALUES = 0x%x\n", dcvalues);
    ptp_print("PTP_AGECOUNT = 0x%x\n", agecount);
    ptp_print("PTP_PTPEN = 0x%x\n", ptpen);

    if( PTPINTSTS == 0x1 ) // PTP init1 or init2
    {
        if((ptpen & 0x7) == 0x1){ // init1

            ptp_print("Enter PTPOD Init 1\n");
            ptp_print("shouldn't happen in linux\n");
            //PTP_DCVOFFSET = ~(ptp_read(PTP_DCVALUES) & 0xffff)+1;  // hw bug, workaround
            //PTP_AGEVOFFSET = ptp_read(PTP_AGEVALUES) & 0xffff;
        
            //PTP_INIT_FLAG = 1;

            // Set PTPEN.PTPINITEN/PTPEN.PTPINIT2EN = 0x0 & Clear PTP INIT interrupt PTPINTSTS = 0x00000001
            //ptp_write(PTP_PTPEN, 0x0);
            //ptp_write(PTP_PTPINTSTS, 0x1);
            // switch block clock source to MAINPLL/12. bclk should be 66.625 (DDR2)
            //clkmux_sel(MT_CLKMUX_AXIBUS_GFMUX_SEL, MT_CG_MPLL_D12, "PTP_BCLK");

            /*for(loop = 0x0; loop <= 0x7c; loop += 0x4){
                ptp_print("Reg[0x%x]: 0x%x\n", PTP_ctr_reg_addr+loop, ptp_read(PTP_ctr_reg_addr+loop));
            }*/
            ptp_print("[ISR Entry End]\n");
            // trigger init2
            //ptp_init2_standalone(DCVoffset, AGEVoffset);
        }
        else if((ptpen & 0x7) == 0x5){ // init2
            // read PTP_VO_0 ~ PTP_VO_3
            temp = ptp_read( PTP_VOP30 );
            PTP_V[0] = temp & 0xff;
            PTP_V[1] = (temp>>8) & 0xff;
            PTP_V[2] = (temp>>16) & 0xff;
            PTP_V[3] = (temp>>24) & 0xff;

            // read PTP_VO_3 ~ PTP_VO_7
            temp = ptp_read( PTP_VOP74 );
            PTP_V[4] = temp & 0xff;
            PTP_V[5] = (temp>>8) & 0xff;
            PTP_V[6] = (temp>>16) & 0xff;
            PTP_V[7] = (temp>>24) & 0xff;

            for(i = 0; i < MAX_OPP_NUM; i++){
                PTP_set_ptp_volt(PTP_V[i], CPU_Freq[i]);
            }
            // set PTP_VO_0 ~ PTP_VO_7 to PMIC
            ptp_print("Init2 PTP_V_0 = 0x%x\n", PTP_V[0]);
            ptp_print("Init2 PTP_V_1 = 0x%x\n", PTP_V[1]);
            ptp_print("Init2 PTP_V_2 = 0x%x\n", PTP_V[2]);
            ptp_print("Init2 PTP_V_3 = 0x%x\n", PTP_V[3]);
            ptp_print("Init2 PTP_V_4 = 0x%x\n", PTP_V[4]);
            ptp_print("Init2 PTP_V_5 = 0x%x\n", PTP_V[5]);
            ptp_print("Init2 PTP_V_6 = 0x%x\n", PTP_V[6]);
            ptp_print("Init2 PTP_V_7 = 0x%x\n", PTP_V[7]); 
            
            //PTP_set_ptp_volt();

            ptp_write(PTP_PTPEN, 0x0);
            ptp_write(PTP_PTPINTSTS, 0x1);

#ifdef CONFIG_THERMAL
            ptp_mon();
#endif
        }
        else {  // error handler
            // disable PTP
            ptp_print("Error interrupt type: not init1 nor init2 \n");
            ptp_print("====================================================\n");
            ptp_print("PTP error_0 (0x%x) : PTPINTSTS = 0x%x\n", ptpen, PTPINTSTS);
            ptp_print("====================================================\n");
            
            ptp_write(PTP_PTPEN, 0x0);            
            // Clear PTP interrupt PTPINTSTS
            ptp_write(PTP_PTPINTSTS, 0x00ffffff);
        }
    }
    else if( (PTPINTSTS & 0x00ff0002) != 0x0 )  // PTP Monitor mode
    {   
        temperature = (ptp_read(PTP_TEMP)&0xff); // temp_0
        if( (temperature > 0x4b) && (temperature < 0xd3) ){
            ptp_print("temperature error\n");
        }
        else{
            ptp_print("[ISR Entry Begin]\n");
            temp = ptp_read( PTP_VOP30 );
            PTP_MON_V[0] = temp & 0xff;
            PTP_MON_V[1] = (temp>>8) & 0xff;
            PTP_MON_V[2] = (temp>>16) & 0xff;
            PTP_MON_V[3] = (temp>>24) & 0xff;

            // read PTP_VO_3 ~ PTP_VO_7
            temp = ptp_read( PTP_VOP74 );
            PTP_MON_V[4] = temp & 0xff;
            PTP_MON_V[5] = (temp>>8) & 0xff;
            PTP_MON_V[6] = (temp>>16) & 0xff;
            PTP_MON_V[7] = (temp>>24) & 0xff;

            for(i = 0; i < MAX_OPP_NUM; i++){
                PTP_Change[i] = (PTPINTSTS >> 16 + i) & 0x1;
                if(PTP_Change[i] != 0){
                    PTP_set_ptp_volt(PTP_MON_V[i], CPU_Freq[i]);
                }
            }

            // set PTP_VO_0 ~ PTP_VO_7 to PMIC
            ptp_print("Init2 Change? [%d]. PTP_MON_V0 = 0x%x\n", PTP_Change[0], PTP_MON_V[0]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V1 = 0x%x\n", PTP_Change[1], PTP_MON_V[1]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V2 = 0x%x\n", PTP_Change[2], PTP_MON_V[2]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V3 = 0x%x\n", PTP_Change[3], PTP_MON_V[3]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V4 = 0x%x\n", PTP_Change[4], PTP_MON_V[4]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V5 = 0x%x\n", PTP_Change[5], PTP_MON_V[5]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V6 = 0x%x\n", PTP_Change[6], PTP_MON_V[6]);
            ptp_print("Init2 Change? [%d]. PTP_MON_V7 = 0x%x\n", PTP_Change[7], PTP_MON_V[7]);
            ptp_print("TEMPSPARE1 = 0x%x\n", ptp_read(TEMPSPARE1));
            ptp_print("[ISR Entry End]\n");

            // Clear PTP INIT interrupt PTPINTSTS = 0x00ff0002
            ptp_write(PTP_PTPINTSTS, 0x00ff0000);
        }
    }
    else // PTP error
    {
        ptp_print("ISR init type error\n");
        // disable PTP
        ptp_write(PTP_PTPEN, 0x0);
        // Clear PTP interrupt PTPINTSTS
        ptp_write(PTP_PTPINTSTS, 0xffffffff);
    }
    hw_calc_start = false;
    //IRQUnmask(PTP_FSM_IRQ_ID);  
    
    return IRQ_HANDLED;
}

static int ptp_init_setup(enum init_type mode, PTP_Init_T* PTP_Init_val)
{
    // config PTP register
    ptp_write(PTP_DESCHAR, ((((PTP_Init_val->BDES)<<8)&0xff00)|((PTP_Init_val->MDES)&0xff)));
    ptp_write(PTP_TEMPCHAR, ((((PTP_Init_val->VCO)<<16)&0xff0000) | (((PTP_Init_val->MTDES)<<8)&0xff00) | ((PTP_Init_val->DVTFIXED)&0xff)));
    ptp_write(PTP_DETCHAR, ((((PTP_Init_val->DCBDET)<<8)&0xff00) | ((PTP_Init_val->DCMDET)&0xff)));
    ptp_write(PTP_AGECHAR, ((((PTP_Init_val->AGEDELTA)<<8)&0xff00)  | ((PTP_Init_val->AGEM)&0xff)));
    ptp_write(PTP_DCCONFIG, ((PTP_Init_val->DCCONFIG)));
    ptp_write(PTP_AGECONFIG, ((PTP_Init_val->AGECONFIG)));
    if(mode == MONITOR_MODE){
        ptp_write(PTP_TSCALCS, ((((PTP_Init_val->BTS)<<12)&0xfff000) | ((PTP_Init_val->MTS)&0xfff)));
    }

    if( PTP_Init_val->AGEM == 0x0 )
    {
        ptp_write(PTP_RUNCONFIG, 0x80000000);
    }
    else
    {
        unsigned int temp_i, temp_filter, temp_value;
       
        temp_value = 0x0; 
        for (temp_i = 0 ; temp_i < 24 ; temp_i += 2 )
        {
            temp_filter = 0x3 << temp_i;
            	
            if( ((PTP_Init_val->AGECONFIG) & temp_filter) == 0x0 ){
                temp_value |= (0x1 << temp_i);
            }
            else{
                temp_value |= ((PTP_Init_val->AGECONFIG) & temp_filter);
            }
        }
        ptp_write(PTP_RUNCONFIG, temp_value);
    }

    ptp_write(PTP_FREQPCT30, ((((PTP_Init_val->FREQPCT[3])<<24)&0xff000000) | (((PTP_Init_val->FREQPCT[2])<<16)&0xff0000) | (((PTP_Init_val->FREQPCT[1])<<8)&0xff00) | ((PTP_Init_val->FREQPCT[0]) & 0xff)));
    ptp_write(PTP_FREQPCT74, ((((PTP_Init_val->FREQPCT[7])<<24)&0xff000000) | (((PTP_Init_val->FREQPCT[6])<<16)&0xff0000) | (((PTP_Init_val->FREQPCT[5])<<8)&0xff00) | ((PTP_Init_val->FREQPCT[4]) & 0xff)));
    ptp_write(PTP_LIMITVALS, ((((PTP_Init_val->VMAX)<<24)&0xff000000) | (((PTP_Init_val->VMIN)<<16)&0xff0000) | (((PTP_Init_val->DTHI)<<8)&0xff00) | ((PTP_Init_val->DTLO) & 0xff)));
    ptp_write(PTP_VBOOT, (((PTP_Init_val->VBOOT)&0xff)));
    ptp_write(PTP_DETWINDOW, (((PTP_Init_val->DETWINDOW)&0xffff)));
    ptp_write(PTP_PTPCONFIG, (((PTP_Init_val->DETMAX)&0xffff)));

    // clear all pending PTP interrupt & config PTPINTEN
    ptp_write(PTP_PTPINTSTS, 0xffffffff);

    // enable PTP INIT measurement
    hw_calc_start = true;
    if(mode == INIT1_MODE){
        ptp_write(PTP_PTPINTEN, 0x00005f01);
        ptp_write(PTP_PTPEN, 0x00000001);
        //ptp_print("init1 start!!!!!\n");
        return 0;
    }
    else if(mode == INIT2_MODE){
        ptp_write(PTP_PTPINTEN, 0x00005f01);
        ptp_write(PTP_INIT2VALS, ((((PTP_Init_val->AGEVOFFSETIN)<<16)&0xffff0000) | ((PTP_Init_val->DCVOFFSETIN)&0xffff)));
        ptp_write(PTP_PTPEN, 0x00000005); 
        return 0;
    }
    else if(mode == MONITOR_MODE){
        ptp_write(PTP_PTPINTEN, 0x00FF0000);
        ptp_write(PTP_PTPEN, 0x00000002);
        return 0;
    }
    else{
        ptp_print("[ERROR]ptp_init_setup: unknown type\n");
        return -1;
    }
}


void PTP_disable_ptp(void)
{
    unsigned long flags;  
    
    // Mask ARM i bit
    local_irq_save(flags);
    
    // disable PTP
    ptp_write(PTP_PTPEN, 0x0);
            
    // Clear PTP interrupt PTPINTSTS
    ptp_write(PTP_PTPINTSTS, 0x00ffffff);
            
    // restore default DVFS table (PMIC)
    // mt_cpufreq_return_default_DVS_by_ptpod();

    PTP_Enable = 0;
    PTP_INIT_FLAG = 0;
    ptp_print("Disable PTP-OD done.\n");

    // Un-Mask ARM i bit
    local_irq_restore(flags);
}

static void ptp_mon(void)
{
    unsigned int i;
    struct TS_PTPOD ts_info;

    ptp_print("ptp_mon : ptp monitor mode start.\r\n");
    
    // PTP test code ================================
    /*PTP_Init_value.PTPINITEN = (val_0) & 0x1;
    PTP_Init_value.PTPMONEN = (val_0 >> 1) & 0x1;
    PTP_Init_value.ADC_CALI_EN = (val_0 >> 2) & 0x1;
    PTP_Init_value.MDES = (val_0 >> 8) & 0xff;
    PTP_Init_value.BDES = (val_0 >> 16) & 0xff;
    PTP_Init_value.DCMDET = (val_0 >> 24) & 0xff;
    
    PTP_Init_value.DCCONFIG = (val_1) & 0xffffff;
    PTP_Init_value.DCBDET = (val_1 >> 24) & 0xff;
    
    PTP_Init_value.AGECONFIG = (val_2) & 0xffffff;
    PTP_Init_value.AGEM = (val_2 >> 24) & 0xff;
    
    //PTP_Init_value.AGEDELTA = (val_3) & 0xff;
    PTP_Init_value.AGEDELTA = 0x88;    
    PTP_Init_value.DVTFIXED = (val_3 >> 8) & 0xff;
    PTP_Init_value.MTDES = (val_3 >> 16) & 0xff;
    PTP_Init_value.VCO = (val_3 >> 24) & 0xff;*/
    
    // (thermal need to provide get_thermal_slope_intercept)
    get_thermal_slope_intercept(&ts_info);
    PTP_Init_value.MTS = ts_info.ts_MTS; // (2048 * TS_SLOPE) + 2048; 
    PTP_Init_value.BTS = ts_info.ts_BTS; // 4 * TS_INTERCEPT; 

    for(i = 0; i < MAX_OPP_NUM; i++){
        PTP_Init_value.FREQPCT[i] = CPU_Freq[i] * 100 / STANDARD_FREQ; // max freq 1200 x 100%	
    }

    PTP_Init_value.DETWINDOW = 0x514;  //50 us. Detector sampling time as represented in cycles of bclk_ck
    PTP_Init_value.VMAX = 0x58; // 1.3125v (700mv + n * 6.25mv)    
    PTP_Init_value.VMIN = 0x48; // 1.15v (700mv + n * 6.25mv)    
    PTP_Init_value.DTHI = 0x01; // positive
    PTP_Init_value.DTLO = 0xff; // negative (2's compliment) 0xff for monitor, but 0xfe for init1/2
    PTP_Init_value.VBOOT = 0x48; // 115v  (700mv + n * 6.25mv)    
    PTP_Init_value.DETMAX = 0xffff; // This timeout value is in cycles of bclk_ck.

    // start test ============================================
    ptp_print("[Start Monitor mode Test]\n");
    ptp_print("DTHI = 0x%x\n", PTP_Init_value.DTHI);
    ptp_print("DTLO = 0x%x\n", PTP_Init_value.DTLO);
    ptp_print("PTP_TEMP: 0x%x\n", ptp_read(PTP_TEMP));
    ptp_init_setup(MONITOR_MODE, &PTP_Init_value);
}

static void ptp_init2()
{
    unsigned int i;

    ptp_print("ptp_init2: ptp init2 start.\r\n");

    //init_PTP_interrupt();
    
    //ptp_write(PTP_PTPEN, 0x0);        
    //PTP_Init_value.MDES = 0x69;
    //PTP_Init_value.BDES = 0xf7;
    //PTP_Init_value.DCMDET = 0x14;    
    //PTP_Init_value.DCBDET = 0xf6;    

    PTP_Init_value.PTPINITEN = (val_0) & 0x1;
    PTP_Init_value.PTPMONEN = (val_0 >> 1) & 0x1;
    PTP_Init_value.MDES = (val_0 >> 8) & 0xff;
    PTP_Init_value.BDES = (val_0 >> 16) & 0xff;
    PTP_Init_value.DCMDET = (val_0 >> 24) & 0xff;
    
    PTP_Init_value.DCCONFIG = (val_1) & 0xffffff;
    PTP_Init_value.DCBDET = (val_1 >> 24) & 0xff;
    
    PTP_Init_value.AGECONFIG = (val_2) & 0xffffff;
    PTP_Init_value.AGEM = (val_2 >> 24) & 0xff;
    
    PTP_Init_value.AGEDELTA = (val_3) & 0xff;
    PTP_Init_value.DVTFIXED = (val_3 >> 8) & 0xff;
    PTP_Init_value.MTDES = (val_3 >> 16) & 0xff;
    PTP_Init_value.VCO = (val_3 >> 24) & 0xff;

    /*PTP_Init_value.PTPINITEN = 0x1;
    PTP_Init_value.PTPMONEN = 0x0;
    PTP_Init_value.AGEM = 0x14;
    PTP_Init_value.DCCONFIG = 0xaaaaaa;
    PTP_Init_value.AGECONFIG = 0xaaaaaa;    
    PTP_Init_value.AGEDELTA = 0x0;
    PTP_Init_value.DVTFIXED = 0x0;
    PTP_Init_value.MTDES = 0x26;
    PTP_Init_value.VCO = 0x60;*/

    for(i = 0; i < MAX_OPP_NUM; i++){
        PTP_Init_value.FREQPCT[i] = CPU_Freq[i] * 100 / STANDARD_FREQ; // max freq 1200 x 100%	
    }
    

    PTP_Init_value.DETWINDOW = 0x514; //40 us. Detector sampling time as represented in cycles of bclk_ck 
    PTP_Init_value.VMAX = 0x58; // 1.25v (700mv + n * 6.25mv)    
    PTP_Init_value.VMIN = 0x48; // 1.15v (700mv + n * 6.25mv)    
    PTP_Init_value.DTHI = 0x01; // positive
    PTP_Init_value.DTLO = 0xfe; // negative (2's compliment)
    PTP_Init_value.VBOOT = 0x48; // 115v  (700mv + n * 6.25mv)    
    PTP_Init_value.DETMAX = 0xffff; // This timeout value is in cycles of bclk_ck.

    PTP_Init_value.DCVOFFSETIN = PTP_DCVOFFSET;
    PTP_Init_value.AGEVOFFSETIN = PTP_AGEVOFFSET;
    ptp_print("[Start Init2]\n");
    ptp_print("PTP_Init_value.DCVOFFSETIN = 0x%x\n", PTP_Init_value.DCVOFFSETIN);
    ptp_print("PTP_Init_value.AGEVOFFSETIN = 0x%x\n", PTP_Init_value.AGEVOFFSETIN);
    // set register for init2
    ptp_init_setup(INIT2_MODE, &PTP_Init_value);

}

void init_PTP_interrupt(void)
{
    int r;
      
    // Set PTP IRQ =========================================
    r = request_irq(PTP_FSM_IRQ_ID, PTP_ISR, IRQF_TRIGGER_LOW, "PTP", NULL);
    if (r)
    {
        ptp_print("PTP IRQ register failed (%d)\n", r);
        WARN_ON(1);
    }
        
    ptp_print("init_PTP_interrupt: Set PTP IRQ OK.\r\n");
}

static int ptp_probe(struct platform_device *pdev)
{
    unsigned int i;
    
    enable_clock(MT_CG_THEM_SW_CG, "PTPOD");
    // get ptpod value
#if PTP_Get_Real_Val
    val_0 = get_devinfo_with_index(8);
    val_1 = get_devinfo_with_index(9);
    val_2 = get_devinfo_with_index(21);
    val_3 = get_devinfo_with_index(22);
#endif

    if( (val_0 & 0x1) == 0x0 )
    {
        ptp_print("PTPINITEN = 0x%x \n", (val_0 & 0x1));
        return 0;
    }

    // Set PTP IRQ =========================================
    init_PTP_interrupt();

    // Get DVFS frequency table ================================
    for(i = 0; i < MAX_OPP_NUM; i++){
        CPU_Freq[i] = (unsigned int)(mt_cpufreq_max_frequency_by_DVS(i));
    }
    
    for(i = 0; i < MAX_OPP_NUM; i++){
        ptp_print("OPP[%d] for PTPOD: %d", i, CPU_Freq[i]);	
    }
    //ptp_level = PTP_get_ptp_level();
    
    //read PTP_DCVOFFSET & PTP_AGEVOFFSET
    PTP_DCVOFFSET = ~(ptp_read(PTP_DCVALUES) & 0xffff) + 1;
    PTP_AGEVOFFSET = ptp_read(PTP_AGEVALUES) & 0xffff;
    //ptp_init2
    ptp_init2();    

    return 0;
}

static int ptp_suspend(struct platform_device *pdev)
{
	  disable_clock(MT_CG_THEM_SW_CG, "PTPOD");
    ptp_print("Disable PTPOD clock\n");
    return 0;
}

static int ptp_resume(struct platform_device *pdev)
{
	  enable_clock(MT_CG_THEM_SW_CG, "PTPOD");
	  ptp_print("Enable PTPOD clock\n");
    ptp_init2();
    return 0;
}

static struct platform_driver mtk_ptp_driver = {
    .remove     = NULL,
    .shutdown   = NULL,
    .probe      = ptp_probe,
    .suspend	  = ptp_suspend,
    .resume		  = ptp_resume,
    .driver     = {
        .name = "mtk-ptp",
    },
};

/***************************
* show current PTP stauts
****************************/
static int ptp_debug_read(char *buf, char **start, off_t off, int count, int *eof, void *data)
{
    int len = 0;
    char *p = buf;

    if (PTP_Enable)
        p += sprintf(p, "PTP enabled\n");
    else
        p += sprintf(p, "PTP disabled\n");

    len = p - buf;
    return len;
}

/************************************
* set PTP stauts by sysfs interface
*************************************/
static ssize_t ptp_debug_write(struct file *file, const char *buffer, unsigned long count, void *data)
{
    int enabled = 0;

    if (sscanf(buffer, "%d", &enabled) == 1)
    {
        if (enabled == 0)
        {            
            // Disable PTP and Restore default DVFS table (PMIC)
            PTP_disable_ptp();
        }
        else
        {
            ptp_print("bad argument_0!! argument should be \"0\"\n");
        }
    }
    else
    {
        ptp_print("bad argument_1!! argument should be \"0\"\n");
    }

    return count;
}

static int __init ptp_init(void)
{
    struct proc_dir_entry *mt_entry = NULL;
    struct proc_dir_entry *mt_ptp_dir = NULL;
    int ptp_err = 0;


    mt_ptp_dir = proc_mkdir("ptp", NULL);
    if (!mt_ptp_dir)
    {
        ptp_print("[%s]: mkdir /proc/ptp failed\n", __FUNCTION__);
    }
    else
    {
        mt_entry = create_proc_entry("ptp_debug", S_IRUGO | S_IWUSR | S_IWGRP, mt_ptp_dir);
        if (mt_entry)
        {
            mt_entry->read_proc = ptp_debug_read;
            mt_entry->write_proc = ptp_debug_write;
        }
    }

    ptp_err = platform_driver_register(&mtk_ptp_driver);
    
    if (ptp_err)
    {
        ptp_print("PTP driver callback register failed..\n");
        return ptp_err;
    }
    
    return 0;
}

static void __exit ptp_exit(void)
{
    ptp_print("Exit PTP\n");
}

#if En_PTPOD
late_initcall(ptp_init);
#endif
