#ifndef __CCCI_PLATFORM_CFG_H__
#define __CCCI_PLATFORM_CFG_H__

//-------------ccci driver configure------------------------//
#define MD1_DEV_MAJOR		(184)
#define MD2_DEV_MAJOR		(169)

//#define CCCI_PLATFORM_L 		0x3536544D
//#define CCCI_PLATFORM_H 		0x31453537
#define CCCI_PLATFORM 			"MT6572E1"
#define CCCI1_DRIVER_VER 		0x20121001
#define CCCI2_DRIVER_VER 		0x20121001

#define CURR_SEC_CCCI_SYNC_VER			(1)	// Note: must sync with sec lib, if ccci and sec has dependency change

#define CCMNI_V1            (1)
#define CCMNI_V2            (2)

#define MD_HEADER_VER_NO    (2)
#define GFH_HEADER_VER_NO   (1)


//-------------md share memory configure----------------//
//Common configuration
#define MD_EX_LOG_SIZE					(2*1024)
#define CCCI_MISC_INFO_SMEM_SIZE		(1*1024)
#define CCCI_SHARED_MEM_SIZE 			UL(0x200000) // 2M align for share memory

#define MD_IMG_DUMP_SIZE				(1<<8)
#define DSP_IMG_DUMP_SIZE				(1<<9)

#define CCMNI_V1_PORT_NUM               (3)          //For V1 CCMNI
#define CCMNI_V2_PORT_NUM               (3) 		 // For V2 CCMNI


// MD SYS1 configuration
#define CCCI1_PCM_SMEM_SIZE				(16 * 2 * 1024)				// PCM
#define CCCI1_MD_LOG_SIZE				(137*1024*4+64*1024+112*1024)	// MD Log

#define RPC1_MAX_BUF_SIZE				2048 //(6*1024)
#define RPC1_REQ_BUF_NUM				2 			 //support 2 concurrently request	

#define CCCI1_TTY_BUF_SIZE			    (16 * 1024)
#define CCCI1_CCMNI_BUF_SIZE			(16*1024)
#define CCCI1_TTY_PORT_NUM    			(3)
//#define CCCI1_CCMNI_V1_PORT_NUM			(3) 		 // For V1 CCMNI


// MD SYS2 configuration
#define CCCI2_PCM_SMEM_SIZE				(16 * 2 * 1024)					// PCM 
#define CCCI2_MD_LOG_SIZE				(137*1024*4+64*1024+112*1024)	// MD Log

#define RPC2_MAX_BUF_SIZE				2048 //(6*1024)
#define RPC2_REQ_BUF_NUM				2 			 //support 2 concurrently request	

#define CCCI2_TTY_BUF_SIZE			    (16 * 1024)
#define CCCI2_CCMNI_BUF_SIZE			(16*1024)
#define CCCI2_TTY_PORT_NUM  			(3)
//#define CCCI2_CCMNI_V1_PORT_NUM			(3) 		 // For V1 CCMNI




//-------------feature enable/disable configure----------------//
/******security feature configure******/
//#define  ENCRYPT_DEBUG                            	//enable debug log for SECURE_ALGO_OP, always disable
#define  ENABLE_MD_IMG_SECURITY_FEATURE 	//disable for bring up, need enable by security owner after security feature ready

/******share memory configure******/
#define CCCI_STATIC_SHARED_MEM           //using ioremap to allocate share memory, not dma_alloc_coherent
//#define  MD_IMG_SIZE_ADJUST_BY_VER        //md region can be adjusted by 2G/3G, ex, 2G: 10MB for md+dsp, 3G: 22MB for md+dsp

/******md header check configure******/
//#define  ENABLE_CHIP_VER_CHECK
//#define  ENABLE_2G_3G_CHECK
#define  ENABLE_MEM_SIZE_CHECK


/******EMI MPU protect configure******/
#define  ENABLE_EMI_PROTECTION  			//disable for bring up           

/******md memory remap configure******/
#define  ENABLE_MEM_REMAP_HW

/******md wake up workaround******/
//#define  ENABLE_MD_WAKE_UP        			//only enable for mt6589 platform         

//******other feature configure******//
//#define  ENABLE_LOCK_MD_SLP_FEATURE
#define ENABLE_32K_CLK_LESS					//disable for bring up
#define  ENABLE_MD_WDT_PROCESS			//disable for bring up for md not enable wdt at bring up
//#define ENABLE_MD_WDT_DBG					//disable on official branch, only for local debug
#define  ENABLE_AEE_MD_EE					//disable for bring up
#define  ENABLE_DRAM_API						//awlays enable for bring up


#endif

