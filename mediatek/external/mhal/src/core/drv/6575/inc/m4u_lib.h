/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#ifndef _MTK_M4U_LIB_H
#define _MTK_M4U_LIB_H

#include <linux/ioctl.h>

#define DEFAULT_PAGE_SIZE   0x1000                                  //4KB
#define MODULE_WITH_INDEPENDENT_PORT_ID  36

#define M4U_CLIENT_MODULE_NUM M4U_CLNTMOD_MAX
#define TOTAL_MVA_RANGE       0x40000000                              //total virtual address range: 1GB

#define ACCESS_TYPE_TRANSLATION_FAULT  0
#define ACCESS_TYPE_64K_PAGE           1
#define ACCESS_TYPE_4K_PAGE            2
#define ACCESS_TYPE_4K_EXTEND_PAGE     3

#define PT_TOTAL_ENTRY_NUM    (TOTAL_MVA_RANGE/DEFAULT_PAGE_SIZE)              //total page table entries
#define MODULE_MVA_RANGE      (TOTAL_MVA_RANGE/M4U_CLIENT_MODULE_NUM)     //the virtual address range per port
#define PT_MODULE_ENTRY_NUM   (MODULE_MVA_RANGE/DEFAULT_PAGE_SIZE)            //number of page table entries for each port
#define PT_MODULE_PA_SZ       (PT_MODULE_ENTRY_NUM*4)                      //the physical memory size of page table per port
#define MMU_TAG_HW_NUM        24
#define MASTER_PORT_NUM       (15+20+6+3)

#define TOTAL_M4U_NUM         4
#define RT_RANGE_NUM          4
#define SEQ_RANGE_NUM         4
#define TOTAL_RANGE_NUM       (RT_RANGE_NUM+SEQ_RANGE_NUM)*TOTAL_M4U_NUM

#define M4U_GET_PTE_OFST_TO_PT_SA(MVA)    ((MVA >> 12) << 2)

#define SEGMENT_SIZE 16
typedef enum
{
    M4U_PORT_DEFECT          = 0,
    M4U_PORT_JPEG_ENC        = 1,
    M4U_PORT_ROT_DMA0_OUT0   = 2,
    M4U_PORT_ROT_DMA1_OUT0   = 3,
    M4U_PORT_TV_ROT_OUT0     = 4,
    M4U_PORT_CAM             = 5,
    M4U_PORT_FD0             = 6,
    M4U_PORT_FD2             = 7,
    M4U_PORT_JPEG_DEC        = 8,
    M4U_PORT_R_DMA0_OUT0     = 9,
    M4U_PORT_R_DMA0_OUT1     = 10,
    M4U_PORT_R_DMA0_OUT2     = 11,    
    M4U_PORT_FD1             = 12,
    M4U_PORT_PCA             = 13,
                            
    M4U_PORT_OVL_MASK        = (SEGMENT_SIZE + 0),
    M4U_PORT_OVL_DCP         = (SEGMENT_SIZE + 1),
    M4U_PORT_DPI             = (SEGMENT_SIZE + 2),
    M4U_PORT_ROT_DMA2_OUT0   = (SEGMENT_SIZE + 3),
    M4U_PORT_ROT_DMA3_OUT0   = (SEGMENT_SIZE + 4),
    M4U_PORT_ROT_DMA4_OUT0   = (SEGMENT_SIZE + 5),
    M4U_PORT_TVC             = (SEGMENT_SIZE + 6),
    M4U_PORT_LCD_R           = (SEGMENT_SIZE + 7),
    M4U_PORT_LCD_W           = (SEGMENT_SIZE + 8),
    M4U_PORT_R_DMA1_OUT0     = (SEGMENT_SIZE + 9),
    M4U_PORT_R_DMA1_OUT1     = (SEGMENT_SIZE + 10),
    M4U_PORT_R_DMA1_OUT2     = (SEGMENT_SIZE + 11),
    M4U_PORT_SPI             = (SEGMENT_SIZE + 12),                     
    
    M4U_PORT_VENC_MC         = (SEGMENT_SIZE*2 + 0),
    M4U_PORT_VENC_BSDMA      = (SEGMENT_SIZE*2 + 1),
    M4U_PORT_VENC_MVQP       = (SEGMENT_SIZE*2 + 2),
    M4U_PORT_VDEC_DMA        = (SEGMENT_SIZE*2 + 3),
    M4U_PORT_VDEC_REC        = (SEGMENT_SIZE*2 + 4),
    M4U_PORT_VDEC_POST0      = (SEGMENT_SIZE*2 + 5),
                             
    M4U_PORT_G2D_W           = (SEGMENT_SIZE*3 + 0),
    M4U_PORT_G2D_R           = (SEGMENT_SIZE*3 + 1),
    M4U_PORT_AUDIO           = (SEGMENT_SIZE*3 + 2),

} M4U_PORT_ID_ENUM;

typedef enum
{
    M4U_CLNTMOD_DEFECT = 0,    // 0
    M4U_CLNTMOD_CAM,
    M4U_CLNTMOD_PCA, 
    M4U_CLNTMOD_JPEG_DEC,        
    M4U_CLNTMOD_JPEG_ENC,  
    M4U_CLNTMOD_ROT0,          // 5
    M4U_CLNTMOD_ROT1,    
    M4U_CLNTMOD_TVROT,        
    M4U_CLNTMOD_RDMA0,
    M4U_CLNTMOD_FD,
    M4U_CLNTMOD_FD_INPUT,
    M4U_CLNTMOD_ROT2,          // 10
    M4U_CLNTMOD_ROT3,
    M4U_CLNTMOD_ROT4,
    M4U_CLNTMOD_OVL,        
    M4U_CLNTMOD_LCDC,         // 15
    M4U_CLNTMOD_LCDC_UI,       
    M4U_CLNTMOD_RDMA1,         
    M4U_CLNTMOD_TVC,           
    M4U_CLNTMOD_SPI,
    M4U_CLNTMOD_DPI,           // 20                 
    M4U_CLNTMOD_VDEC_DMA,      
    M4U_CLNTMOD_VENC_DMA,    
    M4U_CLNTMOD_G2D,           
    M4U_CLNTMOD_AUDIO,
    M4U_CLNTMOD_RDMA_GENERAL,  // 25           
    M4U_CLNTMOD_ROT_GENERAL,    
    M4U_CLNTMOD_UNKNOWN,
    M4U_CLNTMOD_MAX
} M4U_MODULE_ID_ENUM;


typedef struct _M4U_RANGE_DES  //sequential entry range
{
    unsigned int Enabled;
    M4U_MODULE_ID_ENUM eModuleID;
    unsigned int MVAStart;
    unsigned int MVAEnd;
    unsigned int entryCount;
} M4U_RANGE_DES_T;

typedef struct _M4U_MVA_SLOT
{
    unsigned int BaseAddr;      //slot MVA start address
    unsigned int Size;          //slot size
    unsigned int Offset;        //current offset of the slot
    unsigned int BufCnt;        //how many buffer has been allocated from this slot
} M4U_MVA_SLOT_T;

typedef enum
{
	M4U_DESC_MAIN_TLB=0,
	M4U_DESC_PRE_TLB_LSB,
	M4U_DESC_PRE_TLB_MSB
} M4U_DESC_TLB_SELECT_ENUM;


typedef enum
{
	RT_RANGE_HIGH_PRIORITY=0,
	SEQ_RANGE_LOW_PRIORITY=1
} M4U_RANGE_PRIORITY_ENUM;

typedef enum
{
	M4U_DMA_READ_WRITE = 0,
	M4U_DMA_READ = 1,
	M4U_DMA_WRITE = 2,
	M4U_DMA_NONE_OP = 3,

} M4U_DMA_DIR_ENUM;


// native logic
// port related: virtuality, security, distance
typedef struct _M4U_PORT
{  
	M4U_PORT_ID_ENUM ePortID;		   //hardware port ID, defined in M4U_PORT_ID_ENUM
	unsigned int Virtuality;						   
	unsigned int Security;
	unsigned int Distance;
	unsigned int Direction;
}M4U_PORT_STRUCT;

typedef enum
{
	ROTATE_0=0,
	ROTATE_90,
	ROTATE_180,
	ROTATE_270,
	ROTATE_HFLIP_0,
	ROTATE_HFLIP_90,
	ROTATE_HFLIP_180,
	ROTATE_HFLIP_270
} M4U_ROTATOR_ENUM;

typedef struct _M4U_PORT_ROTATOR
{  
	M4U_PORT_ID_ENUM ePortID;		   // hardware port ID, defined in M4U_PORT_ID_ENUM
	unsigned int Virtuality;						   
	unsigned int Security;
	// unsigned int Distance;      // will be caculated actomatically inside M4U driver
	// unsigned int Direction;
  unsigned int MVAStart; 
  unsigned int BufAddr;
  unsigned int BufSize;  
  M4U_ROTATOR_ENUM angle;	
}M4U_PORT_STRUCT_ROTATOR;

// module related:  alloc/dealloc MVA buffer
typedef struct _M4U_MOUDLE
{
	// MVA alloc / dealloc
	M4U_MODULE_ID_ENUM eModuleID;	// module ID used inside M4U driver, defined in M4U_PORT_MODULE_ID_ENUM
	unsigned int BufAddr;				// buffer virtual address
	unsigned int BufSize;				// buffer size in byte

	// TLB range invalidate or set uni-upadte range
	unsigned int MVAStart;						 // MVA start address
	unsigned int MVAEnd;							 // MVA end address
	M4U_RANGE_PRIORITY_ENUM ePriority;						 // range priority, 0:high, 1:normal
	unsigned int entryCount;

    // manually insert page entry
	unsigned int EntryMVA;						 // manual insert entry MVA
	unsigned int Lock;							 // manual insert lock or not
	void* tsk;
}M4U_MOUDLE_STRUCT;

typedef struct _M4U_WRAP_DES
{
    unsigned int Enabled;
    M4U_MODULE_ID_ENUM eModuleID;
    M4U_PORT_ID_ENUM ePortID;    
    unsigned int MVAStart;
    unsigned int MVAEnd;
} M4U_WRAP_DES_T;

typedef enum
{
    M4U_CACHE_FLUSH_BEFORE_HW_READ_MEM = 0,  // optimized, recommand to use
    M4U_CACHE_FLUSH_BEFORE_HW_WRITE_MEM = 1, // optimized, recommand to use
    M4U_CACHE_CLEAN_BEFORE_HW_READ_MEM = 2,
    M4U_CACHE_INVALID_AFTER_HW_WRITE_MEM = 3,
    M4U_NONE_OP = 4,
} M4U_CACHE_SYNC_ENUM;

typedef struct _M4U_CACHE
{
    // MVA alloc / dealloc
    M4U_MODULE_ID_ENUM eModuleID;             // module ID used inside M4U driver, defined in M4U_MODULE_ID_ENUM
    M4U_CACHE_SYNC_ENUM eCacheSync;
    unsigned int BufAddr;                  // buffer virtual address
    unsigned int BufSize;                     // buffer size in byte
}M4U_CACHE_STRUCT;

typedef enum _M4U_STATUS
{
	M4U_STATUS_OK = 0,
	M4U_STATUS_INVALID_CMD,
	M4U_STATUS_INVALID_HANDLE,
	M4U_STATUS_NO_AVAILABLE_RANGE_REGS,
	M4U_STATUS_KERNEL_FAULT,
	M4U_STATUS_MVA_OVERFLOW,
	M4U_STATUS_INVALID_PARAM
} M4U_STATUS_ENUM;


//IOCTL commnad
#define MTK_M4U_MAGICNO 'g'
#define MTK_M4U_T_POWER_ON            _IOW(MTK_M4U_MAGICNO, 0, int)
#define MTK_M4U_T_POWER_OFF           _IOW(MTK_M4U_MAGICNO, 1, int)
#define MTK_M4U_T_DUMP_REG            _IOW(MTK_M4U_MAGICNO, 2, int)
#define MTK_M4U_T_DUMP_INFO           _IOW(MTK_M4U_MAGICNO, 3, int)
#define MTK_M4U_T_ALLOC_MVA           _IOWR(MTK_M4U_MAGICNO,4, int)
#define MTK_M4U_T_DEALLOC_MVA         _IOW(MTK_M4U_MAGICNO, 5, int)
#define MTK_M4U_T_INSERT_TLB_RANGE    _IOW(MTK_M4U_MAGICNO, 6, int)
#define MTK_M4U_T_INVALID_TLB_RANGE   _IOW(MTK_M4U_MAGICNO, 7, int)
#define MTK_M4U_T_INVALID_TLB_ALL     _IOW(MTK_M4U_MAGICNO, 8, int)
#define MTK_M4U_T_MANUAL_INSERT_ENTRY _IOW(MTK_M4U_MAGICNO, 9, int)
#define MTK_M4U_T_CACHE_SYNC          _IOW(MTK_M4U_MAGICNO, 10, int)
#define MTK_M4U_T_CONFIG_PORT         _IOW(MTK_M4U_MAGICNO, 11, int)
#define MTK_M4U_T_CONFIG_ASSERT       _IOW(MTK_M4U_MAGICNO, 12, int)
#define MTK_M4U_T_INSERT_WRAP_RANGE   _IOW(MTK_M4U_MAGICNO, 13, int)
#define MTK_M4U_T_MONITOR_START       _IOW(MTK_M4U_MAGICNO, 14, int)
#define MTK_M4U_T_MONITOR_STOP        _IOW(MTK_M4U_MAGICNO, 15, int)
#define MTK_M4U_T_RESET_MVA_RELEASE_TLB  _IOW(MTK_M4U_MAGICNO, 16, int)
#define MTK_M4U_T_CONFIG_PORT_ROTATOR _IOW(MTK_M4U_MAGICNO, 17, int)
#define MTK_M4U_T_QUERY_MVA           _IOW(MTK_M4U_MAGICNO, 18, int)
#define MTK_M4U_T_M4UDrv_CONSTRUCT    _IOW(MTK_M4U_MAGICNO, 19, int)
#define MTK_M4U_T_M4UDrv_DECONSTRUCT  _IOW(MTK_M4U_MAGICNO, 20, int)
#define MTK_M4U_T_DUMP_PAGETABLE      _IOW(MTK_M4U_MAGICNO, 21, int)
#define MTK_M4U_T_REGISTER_BUFFER     _IOW(MTK_M4U_MAGICNO, 22, int)
#define MTK_M4U_T_CACHE_FLUSH_ALL     _IOW(MTK_M4U_MAGICNO, 23, int)
#define MTK_M4U_T_GET_TSK_STRUCT     _IOW(MTK_M4U_MAGICNO, 24, int)

class MTKM4UDrv
{
public:
    MTKM4UDrv(void);
    ~MTKM4UDrv(void);
    
    M4U_STATUS_ENUM m4u_power_on(M4U_MODULE_ID_ENUM eModuleID);
    M4U_STATUS_ENUM m4u_power_off(M4U_MODULE_ID_ENUM eModuleID);
    M4U_STATUS_ENUM m4u_alloc_mva(M4U_MODULE_ID_ENUM eModuleID, 
		                          const unsigned int BufAddr, 
		                          const unsigned int BufSize, 
		                          unsigned int *pRetMVABuf);
    M4U_STATUS_ENUM m4u_alloc_mva(M4U_MODULE_ID_ENUM eModuleID, 
                                  void * tsk,
								  const unsigned int BufAddr, 
								  const unsigned int BufSize, 
								  unsigned int *pRetMVAAddr);

    M4U_STATUS_ENUM m4u_dealloc_mva(M4U_MODULE_ID_ENUM eModuleID, 
		                          const unsigned int BufAddr, 
		                          const unsigned int BufSize, 
		                          const unsigned int MVAStart);
    M4U_STATUS_ENUM m4u_dealloc_mva(M4U_MODULE_ID_ENUM eModuleID,
                                    void* tsk,
									const unsigned int BufAddr, 
									const unsigned int BufSize,
                                    const unsigned int MVAStart);


    M4U_STATUS_ENUM m4u_insert_wrapped_range(M4U_MODULE_ID_ENUM eModuleID, 
                  M4U_PORT_ID_ENUM portID, 
								  const unsigned int MVAStart, 
								  const unsigned int MVAEnd); //0:disable, 1~4 is valid
								  		                            
    M4U_STATUS_ENUM m4u_insert_tlb_range(M4U_MODULE_ID_ENUM eModuleID, 
		                          unsigned int MVAStart, 
		                          const unsigned int MVAEnd, 
		                          M4U_RANGE_PRIORITY_ENUM ePriority,
		                          unsigned int entryCount);	
		                                
    M4U_STATUS_ENUM m4u_invalid_tlb_range(M4U_MODULE_ID_ENUM eModuleID,
		                          unsigned int MVAStart, 
		                          unsigned int MVAEnd);
		                                  
    M4U_STATUS_ENUM m4u_manual_insert_entry(M4U_MODULE_ID_ENUM eModuleID,
		                          unsigned int EntryMVA, 
		                          bool Lock);	
    M4U_STATUS_ENUM m4u_invalid_tlb_all(M4U_MODULE_ID_ENUM eModuleID);
    M4U_STATUS_ENUM m4u_config_port(M4U_PORT_STRUCT* pM4uPort);

    M4U_STATUS_ENUM m4u_config_port_rotator(M4U_PORT_STRUCT_ROTATOR* pM4uPort);
        
    M4U_STATUS_ENUM m4u_cache_sync(M4U_MODULE_ID_ENUM eModuleID,
		                          M4U_CACHE_SYNC_ENUM eCacheSync,
		                          unsigned int BufAddr, 
		                          unsigned int BufSize);
		                          
    M4U_STATUS_ENUM m4u_reset_mva_release_tlb(M4U_MODULE_ID_ENUM eModuleID);
    
    ///> ------- helper function
    M4U_STATUS_ENUM m4u_dump_reg(M4U_MODULE_ID_ENUM eModuleID);
    M4U_STATUS_ENUM m4u_dump_info(M4U_MODULE_ID_ENUM eModuleID);
    M4U_STATUS_ENUM m4u_monitor_start(M4U_PORT_ID_ENUM PortID);
    M4U_STATUS_ENUM m4u_monitor_stop(M4U_PORT_ID_ENUM PortID);	

private:		                          		                          
    int mFileDescriptor;
    
public:
	
    // used for those looply used buffer
    // will check link list for mva rather than re-build pagetable by get_user_pages()
    // if can not find the VA in link list, will call m4u_alloc_mva() internally		  
    M4U_STATUS_ENUM m4u_query_mva(M4U_MODULE_ID_ENUM eModuleID, 
		                          const unsigned int BufAddr, 
		                          const unsigned int BufSize, 
		                          unsigned int *pRetMVABuf);

    M4U_STATUS_ENUM m4u_query_mva(M4U_MODULE_ID_ENUM eModuleID, 
                                  void * tsk,
								  const unsigned int BufAddr, 
								  const unsigned int BufSize, 
								  unsigned int *pRetMVAAddr);

    M4U_STATUS_ENUM m4u_dump_pagetable(M4U_MODULE_ID_ENUM eModuleID, 
								  const unsigned int BufAddr, 
								  const unsigned int BufSize, 
								  unsigned int MVAStart);

    M4U_STATUS_ENUM m4u_register_buffer(M4U_MODULE_ID_ENUM eModuleID, 
								  const unsigned int BufAddr, 
								  const unsigned int BufSize, 
								  unsigned int *pRetMVAAddr);

    M4U_STATUS_ENUM m4u_cache_flush_all(M4U_MODULE_ID_ENUM eModuleID);

    void* m4u_get_task_struct(void);

};

#endif	/* __M4U_H_ */

