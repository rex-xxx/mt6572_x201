#ifndef _DDP_OVL_H_
#define _DDP_OVL_H_


#define OVL_MAX_WIDTH  1920
#define OVL_MAX_HEIGHT 1920
#define OVL_LAYER_NUM  4
enum OVL_LAYER_SOURCE {
    OVL_LAYER_SOURCE_MEM    = 0,
    OVL_LAYER_SOURCE_RESERVED = 1,
    OVL_LAYER_SOURCE_SCL     = 2,
    OVL_LAYER_SOURCE_PQ     = 3,
};

#define OVL_COLOR_BASE 30
enum OVL_INPUT_FORMAT {
    OVL_INPUT_FORMAT_RGB888     = 0,
    OVL_INPUT_FORMAT_RGB565     = 1,
    OVL_INPUT_FORMAT_ARGB8888   = 2,
    OVL_INPUT_FORMAT_PARGB8888  = 3,
    OVL_INPUT_FORMAT_xRGB8888  = 4,
    OVL_INPUT_FORMAT_YUYV       = 8,
    OVL_INPUT_FORMAT_UYVY       = 9,
    OVL_INPUT_FORMAT_YVYU       = 10,
    OVL_INPUT_FORMAT_VYUY       = 11,
    OVL_INPUT_FORMAT_YUV444     = 15,
    
    OVL_INPUT_FORMAT_ABGR8888   = OVL_INPUT_FORMAT_ARGB8888 +OVL_COLOR_BASE,
    OVL_INPUT_FORMAT_BGR888     = OVL_INPUT_FORMAT_RGB888   +OVL_COLOR_BASE,
    OVL_INPUT_FORMAT_BGR565     = OVL_INPUT_FORMAT_RGB565   +OVL_COLOR_BASE,
    OVL_INPUT_FORMAT_PABGR8888  = OVL_INPUT_FORMAT_PARGB8888+OVL_COLOR_BASE,
    OVL_INPUT_FORMAT_xBGR8888   = OVL_INPUT_FORMAT_xRGB8888 +OVL_COLOR_BASE,
};

typedef struct _OVL_CONFIG_STRUCT
{
    unsigned int layer;
	unsigned int layer_en;
    enum OVL_LAYER_SOURCE source;
    enum OVL_INPUT_FORMAT fmt;
    unsigned int addr; 
    unsigned int vaddr;
    unsigned int src_x;
    unsigned int src_y;
    unsigned int src_w;
    unsigned int src_h;
    unsigned int src_pitch;
    unsigned int dst_x;
    unsigned int dst_y;
    unsigned int dst_w;
    unsigned int dst_h;                  // clip region
    unsigned int keyEn;
    unsigned int key; 
    unsigned int aen; 
    unsigned char alpha;  

    unsigned int isTdshp;
    unsigned int isDirty;

    int buff_idx;
    int identity;
    int connected_type;
    unsigned int security;
}OVL_CONFIG_STRUCT;


// start overlay module
int OVLStart(void);

// stop overlay module
int OVLStop(void);

// reset overlay module
int OVLReset(void);

// set region of interest
int OVLROI(unsigned int bgW, 
               unsigned int bgH,                                                      // region size
               unsigned int bgColor); // border color

// switch layer on/off
int OVLLayerSwitch(unsigned layer, bool en);

// configure layer property
int OVLLayerConfig(unsigned layer,
                   enum OVL_LAYER_SOURCE source, 
                   unsigned int fmt, 
                   unsigned int addr, 
                   unsigned int src_x,     // ROI x offset
                   unsigned int src_y,     // ROI y offset
                   unsigned int src_w,     // ROI width
                   unsigned int src_h,     // ROI height
                   unsigned int src_pitch,
                   unsigned int dst_x,     // ROI x offset
                   unsigned int dst_y,     // ROI y offset
                   unsigned int dst_w,     // ROT width
                   unsigned int dst_h,     // ROI height
                   bool keyEn,
                   unsigned int key, 
                   bool aen, 
                   unsigned char alpha);                    // trancparency

int OVL3DConfig(unsigned int layer_id, 
                unsigned int en_3d,
                unsigned int landscape,
                unsigned int r_first);
                
void OVLLayerTdshpEn(unsigned layer, bool en);


//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------
#define STA_FLD_RDMA3_RST_PERIOD                               REG_FLD(1, 4)
#define STA_FLD_RDMA2_RST_PERIOD                               REG_FLD(1, 3)
#define STA_FLD_RDMA1_RST_PERIOD                               REG_FLD(1, 2)
#define STA_FLD_RDMA0_RST_PERIOD                               REG_FLD(1, 1)
#define STA_FLD_OVL_RUN                                        REG_FLD(1, 0)

#define INTEN_FLD_RDMA3_FIFO_UNDERFLOW_INTEN                   REG_FLD(1, 11)
#define INTEN_FLD_RDMA2_FIFO_UNDERFLOW_INTEN                   REG_FLD(1, 10)
#define INTEN_FLD_RDMA1_FIFO_UNDERFLOW_INTEN                   REG_FLD(1, 9)
#define INTEN_FLD_RDMA0_FIFO_UNDERFLOW_INTEN                   REG_FLD(1, 8)
#define INTEN_FLD_RDMA3_EOF_ABNORMAL_INTEN                     REG_FLD(1, 7)
#define INTEN_FLD_RDMA2_EOF_ABNORMAL_INTEN                     REG_FLD(1, 6)
#define INTEN_FLD_RDMA1_EOF_ABNORMAL_INTEN                     REG_FLD(1, 5)
#define INTEN_FLD_RDMA0_EOF_ABNORMAL_INTEN                     REG_FLD(1, 4)
#define INTEN_FLD_OVL_FME_SWRST_DONE_INTEN                     REG_FLD(1, 3)
#define INTEN_FLD_OVL_FME_UND_INTEN                            REG_FLD(1, 2)
#define INTEN_FLD_OVL_FME_CPL_INTEN                            REG_FLD(1, 1)
#define INTEN_FLD_OVL_REG_CMT_INTEN                            REG_FLD(1, 0)

#define INTSTA_FLD_RDMA3_FIFO_UNDERFLOW_INTSTA                 REG_FLD(1, 11)
#define INTSTA_FLD_RDMA2_FIFO_UNDERFLOW_INTSTA                 REG_FLD(1, 10)
#define INTSTA_FLD_RDMA1_FIFO_UNDERFLOW_INTSTA                 REG_FLD(1, 9)
#define INTSTA_FLD_RDMA0_FIFO_UNDERFLOW_INTSTA                 REG_FLD(1, 8)
#define INTSTA_FLD_RDMA3_EOF_ABNORMAL_INTSTA                   REG_FLD(1, 7)
#define INTSTA_FLD_RDMA2_EOF_ABNORMAL_INTSTA                   REG_FLD(1, 6)
#define INTSTA_FLD_RDMA1_EOF_ABNORMAL_INTSTA                   REG_FLD(1, 5)
#define INTSTA_FLD_RDMA0_EOF_ABNORMAL_INTSTA                   REG_FLD(1, 4)
#define INTSTA_FLD_OVL_FME_SWRST_DONE_INTSTA                   REG_FLD(1, 3)
#define INTSTA_FLD_OVL_FME_UND_INTSTA                          REG_FLD(1, 2)
#define INTSTA_FLD_OVL_FME_CPL_INTSTA                          REG_FLD(1, 1)
#define INTSTA_FLD_OVL_REG_CMT_INTSTA                          REG_FLD(1, 0)

#define EN_FLD_OVL_EN                                          REG_FLD(1, 0)

#define TRIG_FLD_OVL_SW_TRIG                                   REG_FLD(1, 0)

#define RST_FLD_OVL_RSTB                                       REG_FLD(1, 0)

#define ROI_SIZE_FLD_ROI_H                                     REG_FLD(12, 16)
#define ROI_SIZE_FLD_ROI_W                                     REG_FLD(12, 0)

#define ROI_BGCLR_FLD_ALPHA                                    REG_FLD(8, 24)
#define ROI_BGCLR_FLD_RED                                      REG_FLD(8, 16)
#define ROI_BGCLR_FLD_GREEN                                    REG_FLD(8, 8)
#define ROI_BGCLR_FLD_BLUE                                     REG_FLD(8, 0)

#define SRC_CON_FLD_L3_EN                                      REG_FLD(1, 3)
#define SRC_CON_FLD_L2_EN                                      REG_FLD(1, 2)
#define SRC_CON_FLD_L1_EN                                      REG_FLD(1, 1)
#define SRC_CON_FLD_L0_EN                                      REG_FLD(1, 0)

#define L0_CON_FLD_DSTKEY_EN                                   REG_FLD(1, 31)
#define L0_CON_FLD_SRCKEY_EN                                   REG_FLD(1, 30)
#define L0_CON_FLD_LAYER_SRC                                   REG_FLD(2, 28)
#define L0_CON_FLD_RGB_SWAP                                    REG_FLD(1, 25)
#define L0_CON_FLD_BYTE_SWAP                                   REG_FLD(1, 24)
#define L0_CON_FLD_R_FIRST                                     REG_FLD(1, 22)
#define L0_CON_FLD_LANDSCAPE                                   REG_FLD(1, 21)
#define L0_CON_FLD_EN_3D                                       REG_FLD(1, 20)
#define L0_CON_FLD_C_CF_SEL                                    REG_FLD(3, 16)
#define L0_CON_FLD_CLRFMT                                      REG_FLD(4, 12)
#define L0_CON_FLD_H_FLIP_EN                                    REG_FLD(1, 10)
#define L0_CON_FLD_V_FLIP_EN                                    REG_FLD(1, 9)
#define L0_CON_FLD_ALPHA_EN                                    REG_FLD(1, 8)
#define L0_CON_FLD_ALPHA                                       REG_FLD(8, 0)

#define L0_SRCKEY_FLD_SRCKEY                                   REG_FLD(32, 0)

#define L0_SRC_SIZE_FLD_L0_SRC_H                               REG_FLD(12, 16)
#define L0_SRC_SIZE_FLD_L0_SRC_W                               REG_FLD(12, 0)

#define L0_OFFSET_FLD_L0_YOFF                                  REG_FLD(12, 16)
#define L0_OFFSET_FLD_L0_XOFF                                  REG_FLD(12, 0)

#define L0_ADDR_FLD_L0_ADDR                                    REG_FLD(32, 0)

#define L0_PITCH_FLD_L0_SRC_PITCH                              REG_FLD(16, 0)

#define L1_CON_FLD_DSTKEY_EN                                   REG_FLD(1, 31)
#define L1_CON_FLD_SRCKEY_EN                                   REG_FLD(1, 30)
#define L1_CON_FLD_LAYER_SRC                                   REG_FLD(2, 28)
#define L1_CON_FLD_RGB_SWAP                                    REG_FLD(1, 25)
#define L1_CON_FLD_BYTE_SWAP                                   REG_FLD(1, 24)
#define L1_CON_FLD_R_FIRST                                     REG_FLD(1, 22)
#define L1_CON_FLD_LANDSCAPE                                   REG_FLD(1, 21)
#define L1_CON_FLD_EN_3D                                       REG_FLD(1, 20)
#define L1_CON_FLD_C_CF_SEL                                    REG_FLD(3, 16)
#define L1_CON_FLD_CLRFMT                                      REG_FLD(4, 12)
#define L1_CON_FLD_H_FLIP_EN                                    REG_FLD(1, 10)
#define L1_CON_FLD_V_FLIP_EN                                    REG_FLD(1, 9)
#define L1_CON_FLD_ALPHA_EN                                    REG_FLD(1, 8)
#define L1_CON_FLD_ALPHA                                       REG_FLD(8, 0)

#define L1_SRCKEY_FLD_SRCKEY                                   REG_FLD(32, 0)

#define L1_SRC_SIZE_FLD_L1_SRC_H                               REG_FLD(12, 16)
#define L1_SRC_SIZE_FLD_L1_SRC_W                               REG_FLD(12, 0)

#define L1_OFFSET_FLD_L1_YOFF                                  REG_FLD(12, 16)
#define L1_OFFSET_FLD_L1_XOFF                                  REG_FLD(12, 0)

#define L1_ADDR_FLD_L1_ADDR                                    REG_FLD(32, 0)

#define L1_PITCH_FLD_L1_SRC_PITCH                              REG_FLD(16, 0)

#define L2_CON_FLD_DSTKEY_EN                                   REG_FLD(1, 31)
#define L2_CON_FLD_SRCKEY_EN                                   REG_FLD(1, 30)
#define L2_CON_FLD_LAYER_SRC                                   REG_FLD(2, 28)
#define L2_CON_FLD_RGB_SWAP                                    REG_FLD(1, 25)
#define L2_CON_FLD_BYTE_SWAP                                   REG_FLD(1, 24)
#define L2_CON_FLD_R_FIRST                                     REG_FLD(1, 22)
#define L2_CON_FLD_LANDSCAPE                                   REG_FLD(1, 21)
#define L2_CON_FLD_EN_3D                                       REG_FLD(1, 20)
#define L2_CON_FLD_C_CF_SEL                                    REG_FLD(3, 16)
#define L2_CON_FLD_CLRFMT                                      REG_FLD(4, 12)
#define L2_CON_FLD_H_FLIP_EN                                    REG_FLD(1, 10)
#define L2_CON_FLD_V_FLIP_EN                                    REG_FLD(1, 9)
#define L2_CON_FLD_ALPHA_EN                                    REG_FLD(1, 8)
#define L2_CON_FLD_ALPHA                                       REG_FLD(8, 0)

#define L2_SRCKEY_FLD_SRCKEY                                   REG_FLD(32, 0)

#define L2_SRC_SIZE_FLD_L2_SRC_H                               REG_FLD(12, 16)
#define L2_SRC_SIZE_FLD_L2_SRC_W                               REG_FLD(12, 0)

#define L2_OFFSET_FLD_L2_YOFF                                  REG_FLD(12, 16)
#define L2_OFFSET_FLD_L2_XOFF                                  REG_FLD(12, 0)

#define L2_ADDR_FLD_L2_ADDR                                    REG_FLD(32, 0)

#define L2_PITCH_FLD_L2_SRC_PITCH                              REG_FLD(16, 0)

#define L3_CON_FLD_DSTKEY_EN                                   REG_FLD(1, 31)
#define L3_CON_FLD_SRCKEY_EN                                   REG_FLD(1, 30)
#define L3_CON_FLD_LAYER_SRC                                   REG_FLD(2, 28)
#define L3_CON_FLD_RGB_SWAP                                    REG_FLD(1, 25)
#define L3_CON_FLD_BYTE_SWAP                                   REG_FLD(1, 24)
#define L3_CON_FLD_R_FIRST                                     REG_FLD(1, 22)
#define L3_CON_FLD_LANDSCAPE                                   REG_FLD(1, 21)
#define L3_CON_FLD_EN_3D                                       REG_FLD(1, 20)
#define L3_CON_FLD_C_CF_SEL                                    REG_FLD(3, 16)
#define L3_CON_FLD_CLRFMT                                      REG_FLD(4, 12)
#define L3_CON_FLD_H_FLIP_EN                                    REG_FLD(1, 10)
#define L3_CON_FLD_V_FLIP_EN                                    REG_FLD(1, 9)
#define L3_CON_FLD_ALPHA_EN                                    REG_FLD(1, 8)
#define L3_CON_FLD_ALPHA                                       REG_FLD(8, 0)

#define L3_SRCKEY_FLD_SRCKEY                                   REG_FLD(32, 0)

#define L3_SRC_SIZE_FLD_L3_SRC_H                               REG_FLD(12, 16)
#define L3_SRC_SIZE_FLD_L3_SRC_W                               REG_FLD(12, 0)

#define L3_OFFSET_FLD_L3_YOFF                                  REG_FLD(12, 16)
#define L3_OFFSET_FLD_L3_XOFF                                  REG_FLD(12, 0)

#define L3_ADDR_FLD_L3_ADDR                                    REG_FLD(32, 0)

#define L3_PITCH_FLD_L3_SRC_PITCH                              REG_FLD(16, 0)

#define RDMA0_CTRL_FLD_RDMA0_TRIG_TYPE                         REG_FLD(1, 8)
#define RDMA0_CTRL_FLD_RDMA0_EN                                REG_FLD(1, 0)

#define RDMA0_MEM_START_TRIG_FLD_RDMA0_START_TRIG              REG_FLD(1, 0)

#define RDMA0_MEM_GMC_SETTING_FLD_RDMA0_DISEN_THRD             REG_FLD(10, 16)
#define RDMA0_MEM_GMC_SETTING_FLD_RDMA0_EN_THRD                REG_FLD(10, 0)

#define RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_CNT                  REG_FLD(16, 16)
#define RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_EN                   REG_FLD(1, 0)

#define RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_UND_EN                  REG_FLD(1, 31)
#define RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_SIZE                    REG_FLD(10, 16)
#define RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_THRD                    REG_FLD(10, 0)

#define RDMA1_CTRL_FLD_RDMA1_TRIG_TYPE                         REG_FLD(1, 8)
#define RDMA1_CTRL_FLD_RDMA1_EN                                REG_FLD(1, 0)

#define RDMA1_MEM_START_TRIG_FLD_RDMA1_START_TRIG              REG_FLD(1, 0)

#define RDMA1_MEM_GMC_SETTING_FLD_RDMA1_DISEN_THRD             REG_FLD(10, 16)
#define RDMA1_MEM_GMC_SETTING_FLD_RDMA1_EN_THRD                REG_FLD(10, 0)

#define RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_CNT                  REG_FLD(16, 16)
#define RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_EN                   REG_FLD(1, 0)

#define RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_UND_EN                  REG_FLD(1, 31)
#define RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_SIZE                    REG_FLD(10, 16)
#define RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_THRD                    REG_FLD(10, 0)

#define RDMA2_CTRL_FLD_RDMA2_TRIG_TYPE                         REG_FLD(1, 8)
#define RDMA2_CTRL_FLD_RDMA2_EN                                REG_FLD(1, 0)

#define RDMA2_MEM_START_TRIG_FLD_RDMA2_START_TRIG              REG_FLD(1, 0)

#define RDMA2_MEM_GMC_SETTING_FLD_RDMA2_DISEN_THRD             REG_FLD(10, 16)
#define RDMA2_MEM_GMC_SETTING_FLD_RDMA2_EN_THRD                REG_FLD(10, 0)

#define RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_CNT                  REG_FLD(16, 16)
#define RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_EN                   REG_FLD(1, 0)

#define RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_UND_EN                  REG_FLD(1, 31)
#define RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_SIZE                    REG_FLD(10, 16)
#define RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_THRD                    REG_FLD(10, 0)

#define RDMA3_CTRL_FLD_RDMA3_TRIG_TYPE                         REG_FLD(1, 8)
#define RDMA3_CTRL_FLD_RDMA3_EN                                REG_FLD(1, 0)

#define RDMA3_MEM_START_TRIG_FLD_RDMA3_START_TRIG              REG_FLD(1, 0)

#define RDMA3_MEM_GMC_SETTING_FLD_RDMA3_DISEN_THRD             REG_FLD(10, 16)
#define RDMA3_MEM_GMC_SETTING_FLD_RDMA3_EN_THRD                REG_FLD(10, 0)

#define RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_CNT                  REG_FLD(16, 16)
#define RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_EN                   REG_FLD(1, 0)

#define RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_UND_EN                  REG_FLD(1, 31)
#define RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_SIZE                    REG_FLD(10, 16)
#define RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_THRD                    REG_FLD(10, 0)

#define L0_Y2R_PARA_R0_FLD_C_CF_RMU                            REG_FLD(13, 16)
#define L0_Y2R_PARA_R0_FLD_C_CF_RMY                            REG_FLD(13, 0)

#define L0_Y2R_PARA_R1_FLD_C_CF_RMV                            REG_FLD(13, 0)

#define L0_Y2R_PARA_G0_FLD_C_CF_GMU                            REG_FLD(13, 16)
#define L0_Y2R_PARA_G0_FLD_C_CF_GMY                            REG_FLD(13, 0)

#define L0_Y2R_PARA_G1_FLD_C_CF_GMV                            REG_FLD(13, 0)

#define L0_Y2R_PARA_B0_FLD_C_CF_BMU                            REG_FLD(13, 16)
#define L0_Y2R_PARA_B0_FLD_C_CF_BMY                            REG_FLD(13, 0)

#define L0_Y2R_PARA_B1_FLD_C_CF_BMV                            REG_FLD(13, 0)

#define L0_Y2R_PARA_YUV_A_0_FLD_C_CF_UA                        REG_FLD(13, 16)
#define L0_Y2R_PARA_YUV_A_0_FLD_C_CF_YA                        REG_FLD(9, 0)

#define L0_Y2R_PARA_YUV_A_1_FLD_C_CF_VA                        REG_FLD(9, 0)

#define L0_Y2R_PARA_RGB_A_0_FLD_C_CF_GA                        REG_FLD(13, 16)
#define L0_Y2R_PARA_RGB_A_0_FLD_C_CF_RA                        REG_FLD(9, 0)

#define L0_Y2R_PARA_RGB_A_1_FLD_C_CF_BA                        REG_FLD(9, 0)

#define L1_Y2R_PARA_R0_FLD_C_CF_RMU                            REG_FLD(13, 16)
#define L1_Y2R_PARA_R0_FLD_C_CF_RMY                            REG_FLD(13, 0)

#define L1_Y2R_PARA_R1_FLD_C_CF_RMV                            REG_FLD(13, 0)

#define L1_Y2R_PARA_G0_FLD_C_CF_GMU                            REG_FLD(13, 16)
#define L1_Y2R_PARA_G0_FLD_C_CF_GMY                            REG_FLD(13, 0)

#define L1_Y2R_PARA_G1_FLD_C_CF_GMV                            REG_FLD(13, 0)

#define L1_Y2R_PARA_B0_FLD_C_CF_BMU                            REG_FLD(13, 16)
#define L1_Y2R_PARA_B0_FLD_C_CF_BMY                            REG_FLD(13, 0)

#define L1_Y2R_PARA_B1_FLD_C_CF_BMV                            REG_FLD(13, 0)

#define L1_Y2R_PARA_YUV_A_0_FLD_C_CF_UA                        REG_FLD(13, 16)
#define L1_Y2R_PARA_YUV_A_0_FLD_C_CF_YA                        REG_FLD(9, 0)

#define L1_Y2R_PARA_YUV_A_1_FLD_C_CF_VA                        REG_FLD(9, 0)

#define L1_Y2R_PARA_RGB_A_0_FLD_C_CF_GA                        REG_FLD(13, 16)
#define L1_Y2R_PARA_RGB_A_0_FLD_C_CF_RA                        REG_FLD(9, 0)

#define L1_Y2R_PARA_RGB_A_1_FLD_C_CF_BA                        REG_FLD(9, 0)

#define L2_Y2R_PARA_R0_FLD_C_CF_RMU                            REG_FLD(13, 16)
#define L2_Y2R_PARA_R0_FLD_C_CF_RMY                            REG_FLD(13, 0)

#define L2_Y2R_PARA_R1_FLD_C_CF_RMV                            REG_FLD(13, 0)

#define L2_Y2R_PARA_G0_FLD_C_CF_GMU                            REG_FLD(13, 16)
#define L2_Y2R_PARA_G0_FLD_C_CF_GMY                            REG_FLD(13, 0)

#define L2_Y2R_PARA_G1_FLD_C_CF_GMV                            REG_FLD(13, 0)

#define L2_Y2R_PARA_B0_FLD_C_CF_BMU                            REG_FLD(13, 16)
#define L2_Y2R_PARA_B0_FLD_C_CF_BMY                            REG_FLD(13, 0)

#define L2_Y2R_PARA_B1_FLD_C_CF_BMV                            REG_FLD(13, 0)

#define L2_Y2R_PARA_YUV_A_0_FLD_C_CF_UA                        REG_FLD(13, 16)
#define L2_Y2R_PARA_YUV_A_0_FLD_C_CF_YA                        REG_FLD(9, 0)

#define L2_Y2R_PARA_YUV_A_1_FLD_C_CF_VA                        REG_FLD(9, 0)

#define L2_Y2R_PARA_RGB_A_0_FLD_C_CF_GA                        REG_FLD(13, 16)
#define L2_Y2R_PARA_RGB_A_0_FLD_C_CF_RA                        REG_FLD(9, 0)

#define L2_Y2R_PARA_RGB_A_1_FLD_C_CF_BA                        REG_FLD(9, 0)

#define L3_Y2R_PARA_R0_FLD_C_CF_RMU                            REG_FLD(13, 16)
#define L3_Y2R_PARA_R0_FLD_C_CF_RMY                            REG_FLD(13, 0)

#define L3_Y2R_PARA_R1_FLD_C_CF_RMV                            REG_FLD(13, 0)

#define L3_Y2R_PARA_G0_FLD_C_CF_GMU                            REG_FLD(13, 16)
#define L3_Y2R_PARA_G0_FLD_C_CF_GMY                            REG_FLD(13, 0)

#define L3_Y2R_PARA_G1_FLD_C_CF_GMV                            REG_FLD(13, 0)

#define L3_Y2R_PARA_B0_FLD_C_CF_BMU                            REG_FLD(13, 16)
#define L3_Y2R_PARA_B0_FLD_C_CF_BMY                            REG_FLD(13, 0)

#define L3_Y2R_PARA_B1_FLD_C_CF_BMV                            REG_FLD(13, 0)

#define L3_Y2R_PARA_YUV_A_0_FLD_C_CF_UA                        REG_FLD(13, 16)
#define L3_Y2R_PARA_YUV_A_0_FLD_C_CF_YA                        REG_FLD(9, 0)

#define L3_Y2R_PARA_YUV_A_1_FLD_C_CF_VA                        REG_FLD(9, 0)

#define L3_Y2R_PARA_RGB_A_0_FLD_C_CF_GA                        REG_FLD(13, 16)
#define L3_Y2R_PARA_RGB_A_0_FLD_C_CF_RA                        REG_FLD(9, 0)

#define L3_Y2R_PARA_RGB_A_1_FLD_C_CF_BA                        REG_FLD(9, 0)

#define FLOW_CTRL_DBG_FLD_FLOW_DBG                             REG_FLD(32, 0)

#define ADDCON_DBG_FLD_ROI_Y                                   REG_FLD(13, 16)
#define ADDCON_DBG_FLD_ROI_X                                   REG_FLD(13, 0)

#define OUTMUX_DBG_FLD_OUT_DATA                                REG_FLD(24, 8)
#define OUTMUX_DBG_FLD_OUT_VALID                               REG_FLD(1, 1)
#define OUTMUX_DBG_FLD_OUT_READY                               REG_FLD(1, 0)

#define RDMA0_DBG_FLD_CUR_Y0                                   REG_FLD(16, 16)
#define RDMA0_DBG_FLD_CUR_X0                                   REG_FLD(16, 0)

#define RDMA1_DBG_FLD_CUR_Y1                                   REG_FLD(16, 16)
#define RDMA1_DBG_FLD_CUR_X1                                   REG_FLD(16, 0)

#define RDMA2_DBG_FLD_CUR_Y2                                   REG_FLD(16, 16)
#define RDMA2_DBG_FLD_CUR_X2                                   REG_FLD(16, 0)

#define RDMA3_DBG_FLD_CUR_Y3                                   REG_FLD(16, 16)
#define RDMA3_DBG_FLD_CUR_X3                                   REG_FLD(16, 0)

#define STA_GET_RDMA3_RST_PERIOD(reg32)                        REG_FLD_GET(STA_FLD_RDMA3_RST_PERIOD, (reg32))
#define STA_GET_RDMA2_RST_PERIOD(reg32)                        REG_FLD_GET(STA_FLD_RDMA2_RST_PERIOD, (reg32))
#define STA_GET_RDMA1_RST_PERIOD(reg32)                        REG_FLD_GET(STA_FLD_RDMA1_RST_PERIOD, (reg32))
#define STA_GET_RDMA0_RST_PERIOD(reg32)                        REG_FLD_GET(STA_FLD_RDMA0_RST_PERIOD, (reg32))
#define STA_GET_OVL_RUN(reg32)                                 REG_FLD_GET(STA_FLD_OVL_RUN, (reg32))

#define INTEN_GET_RDMA3_FIFO_UNDERFLOW_INTEN(reg32)            REG_FLD_GET(INTEN_FLD_RDMA3_FIFO_UNDERFLOW_INTEN, (reg32))
#define INTEN_GET_RDMA2_FIFO_UNDERFLOW_INTEN(reg32)            REG_FLD_GET(INTEN_FLD_RDMA2_FIFO_UNDERFLOW_INTEN, (reg32))
#define INTEN_GET_RDMA1_FIFO_UNDERFLOW_INTEN(reg32)            REG_FLD_GET(INTEN_FLD_RDMA1_FIFO_UNDERFLOW_INTEN, (reg32))
#define INTEN_GET_RDMA0_FIFO_UNDERFLOW_INTEN(reg32)            REG_FLD_GET(INTEN_FLD_RDMA0_FIFO_UNDERFLOW_INTEN, (reg32))
#define INTEN_GET_RDMA3_EOF_ABNORMAL_INTEN(reg32)              REG_FLD_GET(INTEN_FLD_RDMA3_EOF_ABNORMAL_INTEN, (reg32))
#define INTEN_GET_RDMA2_EOF_ABNORMAL_INTEN(reg32)              REG_FLD_GET(INTEN_FLD_RDMA2_EOF_ABNORMAL_INTEN, (reg32))
#define INTEN_GET_RDMA1_EOF_ABNORMAL_INTEN(reg32)              REG_FLD_GET(INTEN_FLD_RDMA1_EOF_ABNORMAL_INTEN, (reg32))
#define INTEN_GET_RDMA0_EOF_ABNORMAL_INTEN(reg32)              REG_FLD_GET(INTEN_FLD_RDMA0_EOF_ABNORMAL_INTEN, (reg32))
#define INTEN_GET_OVL_FME_SWRST_DONE_INTEN(reg32)              REG_FLD_GET(INTEN_FLD_OVL_FME_SWRST_DONE_INTEN, (reg32))
#define INTEN_GET_OVL_FME_UND_INTEN(reg32)                     REG_FLD_GET(INTEN_FLD_OVL_FME_UND_INTEN, (reg32))
#define INTEN_GET_OVL_FME_CPL_INTEN(reg32)                     REG_FLD_GET(INTEN_FLD_OVL_FME_CPL_INTEN, (reg32))
#define INTEN_GET_OVL_REG_CMT_INTEN(reg32)                     REG_FLD_GET(INTEN_FLD_OVL_REG_CMT_INTEN, (reg32))

#define INTSTA_GET_RDMA3_FIFO_UNDERFLOW_INTSTA(reg32)          REG_FLD_GET(INTSTA_FLD_RDMA3_FIFO_UNDERFLOW_INTSTA, (reg32))
#define INTSTA_GET_RDMA2_FIFO_UNDERFLOW_INTSTA(reg32)          REG_FLD_GET(INTSTA_FLD_RDMA2_FIFO_UNDERFLOW_INTSTA, (reg32))
#define INTSTA_GET_RDMA1_FIFO_UNDERFLOW_INTSTA(reg32)          REG_FLD_GET(INTSTA_FLD_RDMA1_FIFO_UNDERFLOW_INTSTA, (reg32))
#define INTSTA_GET_RDMA0_FIFO_UNDERFLOW_INTSTA(reg32)          REG_FLD_GET(INTSTA_FLD_RDMA0_FIFO_UNDERFLOW_INTSTA, (reg32))
#define INTSTA_GET_RDMA3_EOF_ABNORMAL_INTSTA(reg32)            REG_FLD_GET(INTSTA_FLD_RDMA3_EOF_ABNORMAL_INTSTA, (reg32))
#define INTSTA_GET_RDMA2_EOF_ABNORMAL_INTSTA(reg32)            REG_FLD_GET(INTSTA_FLD_RDMA2_EOF_ABNORMAL_INTSTA, (reg32))
#define INTSTA_GET_RDMA1_EOF_ABNORMAL_INTSTA(reg32)            REG_FLD_GET(INTSTA_FLD_RDMA1_EOF_ABNORMAL_INTSTA, (reg32))
#define INTSTA_GET_RDMA0_EOF_ABNORMAL_INTSTA(reg32)            REG_FLD_GET(INTSTA_FLD_RDMA0_EOF_ABNORMAL_INTSTA, (reg32))
#define INTSTA_GET_OVL_FME_SWRST_DONE_INTSTA(reg32)            REG_FLD_GET(INTSTA_FLD_OVL_FME_SWRST_DONE_INTSTA, (reg32))
#define INTSTA_GET_OVL_FME_UND_INTSTA(reg32)                   REG_FLD_GET(INTSTA_FLD_OVL_FME_UND_INTSTA, (reg32))
#define INTSTA_GET_OVL_FME_CPL_INTSTA(reg32)                   REG_FLD_GET(INTSTA_FLD_OVL_FME_CPL_INTSTA, (reg32))
#define INTSTA_GET_OVL_REG_CMT_INTSTA(reg32)                   REG_FLD_GET(INTSTA_FLD_OVL_REG_CMT_INTSTA, (reg32))

#define EN_GET_OVL_EN(reg32)                                   REG_FLD_GET(EN_FLD_OVL_EN, (reg32))

#define TRIG_GET_OVL_SW_TRIG(reg32)                            REG_FLD_GET(TRIG_FLD_OVL_SW_TRIG, (reg32))

#define RST_GET_OVL_RSTB(reg32)                                REG_FLD_GET(RST_FLD_OVL_RSTB, (reg32))

#define ROI_SIZE_GET_ROI_H(reg32)                              REG_FLD_GET(ROI_SIZE_FLD_ROI_H, (reg32))
#define ROI_SIZE_GET_ROI_W(reg32)                              REG_FLD_GET(ROI_SIZE_FLD_ROI_W, (reg32))

#define ROI_BGCLR_GET_ALPHA(reg32)                             REG_FLD_GET(ROI_BGCLR_FLD_ALPHA, (reg32))
#define ROI_BGCLR_GET_RED(reg32)                               REG_FLD_GET(ROI_BGCLR_FLD_RED, (reg32))
#define ROI_BGCLR_GET_GREEN(reg32)                             REG_FLD_GET(ROI_BGCLR_FLD_GREEN, (reg32))
#define ROI_BGCLR_GET_BLUE(reg32)                              REG_FLD_GET(ROI_BGCLR_FLD_BLUE, (reg32))

#define SRC_CON_GET_L3_EN(reg32)                               REG_FLD_GET(SRC_CON_FLD_L3_EN, (reg32))
#define SRC_CON_GET_L2_EN(reg32)                               REG_FLD_GET(SRC_CON_FLD_L2_EN, (reg32))
#define SRC_CON_GET_L1_EN(reg32)                               REG_FLD_GET(SRC_CON_FLD_L1_EN, (reg32))
#define SRC_CON_GET_L0_EN(reg32)                               REG_FLD_GET(SRC_CON_FLD_L0_EN, (reg32))

#define L0_CON_GET_DSTKEY_EN(reg32)                            REG_FLD_GET(L0_CON_FLD_DSTKEY_EN, (reg32))
#define L0_CON_GET_SRCKEY_EN(reg32)                            REG_FLD_GET(L0_CON_FLD_SRCKEY_EN, (reg32))
#define L0_CON_GET_LAYER_SRC(reg32)                            REG_FLD_GET(L0_CON_FLD_LAYER_SRC, (reg32))
#define L0_CON_GET_RGB_SWAP(reg32)                             REG_FLD_GET(L0_CON_FLD_RGB_SWAP, (reg32))
#define L0_CON_GET_BYTE_SWAP(reg32)                            REG_FLD_GET(L0_CON_FLD_BYTE_SWAP, (reg32))
#define L0_CON_GET_R_FIRST(reg32)                              REG_FLD_GET(L0_CON_FLD_R_FIRST, (reg32))
#define L0_CON_GET_LANDSCAPE(reg32)                            REG_FLD_GET(L0_CON_FLD_LANDSCAPE, (reg32))
#define L0_CON_GET_EN_3D(reg32)                                REG_FLD_GET(L0_CON_FLD_EN_3D, (reg32))
#define L0_CON_GET_C_CF_SEL(reg32)                             REG_FLD_GET(L0_CON_FLD_C_CF_SEL, (reg32))
#define L0_CON_GET_CLRFMT(reg32)                               REG_FLD_GET(L0_CON_FLD_CLRFMT, (reg32))
#define L0_CON_GET_ALPHA_EN(reg32)                             REG_FLD_GET(L0_CON_FLD_ALPHA_EN, (reg32))
#define L0_CON_GET_ALPHA(reg32)                                REG_FLD_GET(L0_CON_FLD_ALPHA, (reg32))

#define L0_SRCKEY_GET_SRCKEY(reg32)                            REG_FLD_GET(L0_SRCKEY_FLD_SRCKEY, (reg32))

#define L0_SRC_SIZE_GET_L0_SRC_H(reg32)                        REG_FLD_GET(L0_SRC_SIZE_FLD_L0_SRC_H, (reg32))
#define L0_SRC_SIZE_GET_L0_SRC_W(reg32)                        REG_FLD_GET(L0_SRC_SIZE_FLD_L0_SRC_W, (reg32))

#define L0_OFFSET_GET_L0_YOFF(reg32)                           REG_FLD_GET(L0_OFFSET_FLD_L0_YOFF, (reg32))
#define L0_OFFSET_GET_L0_XOFF(reg32)                           REG_FLD_GET(L0_OFFSET_FLD_L0_XOFF, (reg32))

#define L0_ADDR_GET_L0_ADDR(reg32)                             REG_FLD_GET(L0_ADDR_FLD_L0_ADDR, (reg32))

#define L0_PITCH_GET_L0_SRC_PITCH(reg32)                       REG_FLD_GET(L0_PITCH_FLD_L0_SRC_PITCH, (reg32))

#define L1_CON_GET_DSTKEY_EN(reg32)                            REG_FLD_GET(L1_CON_FLD_DSTKEY_EN, (reg32))
#define L1_CON_GET_SRCKEY_EN(reg32)                            REG_FLD_GET(L1_CON_FLD_SRCKEY_EN, (reg32))
#define L1_CON_GET_LAYER_SRC(reg32)                            REG_FLD_GET(L1_CON_FLD_LAYER_SRC, (reg32))
#define L1_CON_GET_RGB_SWAP(reg32)                             REG_FLD_GET(L1_CON_FLD_RGB_SWAP, (reg32))
#define L1_CON_GET_BYTE_SWAP(reg32)                            REG_FLD_GET(L1_CON_FLD_BYTE_SWAP, (reg32))
#define L1_CON_GET_R_FIRST(reg32)                              REG_FLD_GET(L1_CON_FLD_R_FIRST, (reg32))
#define L1_CON_GET_LANDSCAPE(reg32)                            REG_FLD_GET(L1_CON_FLD_LANDSCAPE, (reg32))
#define L1_CON_GET_EN_3D(reg32)                                REG_FLD_GET(L1_CON_FLD_EN_3D, (reg32))
#define L1_CON_GET_C_CF_SEL(reg32)                             REG_FLD_GET(L1_CON_FLD_C_CF_SEL, (reg32))
#define L1_CON_GET_CLRFMT(reg32)                               REG_FLD_GET(L1_CON_FLD_CLRFMT, (reg32))
#define L1_CON_GET_ALPHA_EN(reg32)                             REG_FLD_GET(L1_CON_FLD_ALPHA_EN, (reg32))
#define L1_CON_GET_ALPHA(reg32)                                REG_FLD_GET(L1_CON_FLD_ALPHA, (reg32))

#define L1_SRCKEY_GET_SRCKEY(reg32)                            REG_FLD_GET(L1_SRCKEY_FLD_SRCKEY, (reg32))

#define L1_SRC_SIZE_GET_L1_SRC_H(reg32)                        REG_FLD_GET(L1_SRC_SIZE_FLD_L1_SRC_H, (reg32))
#define L1_SRC_SIZE_GET_L1_SRC_W(reg32)                        REG_FLD_GET(L1_SRC_SIZE_FLD_L1_SRC_W, (reg32))

#define L1_OFFSET_GET_L1_YOFF(reg32)                           REG_FLD_GET(L1_OFFSET_FLD_L1_YOFF, (reg32))
#define L1_OFFSET_GET_L1_XOFF(reg32)                           REG_FLD_GET(L1_OFFSET_FLD_L1_XOFF, (reg32))

#define L1_ADDR_GET_L1_ADDR(reg32)                             REG_FLD_GET(L1_ADDR_FLD_L1_ADDR, (reg32))

#define L1_PITCH_GET_L1_SRC_PITCH(reg32)                       REG_FLD_GET(L1_PITCH_FLD_L1_SRC_PITCH, (reg32))

#define L2_CON_GET_DSTKEY_EN(reg32)                            REG_FLD_GET(L2_CON_FLD_DSTKEY_EN, (reg32))
#define L2_CON_GET_SRCKEY_EN(reg32)                            REG_FLD_GET(L2_CON_FLD_SRCKEY_EN, (reg32))
#define L2_CON_GET_LAYER_SRC(reg32)                            REG_FLD_GET(L2_CON_FLD_LAYER_SRC, (reg32))
#define L2_CON_GET_RGB_SWAP(reg32)                             REG_FLD_GET(L2_CON_FLD_RGB_SWAP, (reg32))
#define L2_CON_GET_BYTE_SWAP(reg32)                            REG_FLD_GET(L2_CON_FLD_BYTE_SWAP, (reg32))
#define L2_CON_GET_R_FIRST(reg32)                              REG_FLD_GET(L2_CON_FLD_R_FIRST, (reg32))
#define L2_CON_GET_LANDSCAPE(reg32)                            REG_FLD_GET(L2_CON_FLD_LANDSCAPE, (reg32))
#define L2_CON_GET_EN_3D(reg32)                                REG_FLD_GET(L2_CON_FLD_EN_3D, (reg32))
#define L2_CON_GET_C_CF_SEL(reg32)                             REG_FLD_GET(L2_CON_FLD_C_CF_SEL, (reg32))
#define L2_CON_GET_CLRFMT(reg32)                               REG_FLD_GET(L2_CON_FLD_CLRFMT, (reg32))
#define L2_CON_GET_ALPHA_EN(reg32)                             REG_FLD_GET(L2_CON_FLD_ALPHA_EN, (reg32))
#define L2_CON_GET_ALPHA(reg32)                                REG_FLD_GET(L2_CON_FLD_ALPHA, (reg32))

#define L2_SRCKEY_GET_SRCKEY(reg32)                            REG_FLD_GET(L2_SRCKEY_FLD_SRCKEY, (reg32))

#define L2_SRC_SIZE_GET_L2_SRC_H(reg32)                        REG_FLD_GET(L2_SRC_SIZE_FLD_L2_SRC_H, (reg32))
#define L2_SRC_SIZE_GET_L2_SRC_W(reg32)                        REG_FLD_GET(L2_SRC_SIZE_FLD_L2_SRC_W, (reg32))

#define L2_OFFSET_GET_L2_YOFF(reg32)                           REG_FLD_GET(L2_OFFSET_FLD_L2_YOFF, (reg32))
#define L2_OFFSET_GET_L2_XOFF(reg32)                           REG_FLD_GET(L2_OFFSET_FLD_L2_XOFF, (reg32))

#define L2_ADDR_GET_L2_ADDR(reg32)                             REG_FLD_GET(L2_ADDR_FLD_L2_ADDR, (reg32))

#define L2_PITCH_GET_L2_SRC_PITCH(reg32)                       REG_FLD_GET(L2_PITCH_FLD_L2_SRC_PITCH, (reg32))

#define L3_CON_GET_DSTKEY_EN(reg32)                            REG_FLD_GET(L3_CON_FLD_DSTKEY_EN, (reg32))
#define L3_CON_GET_SRCKEY_EN(reg32)                            REG_FLD_GET(L3_CON_FLD_SRCKEY_EN, (reg32))
#define L3_CON_GET_LAYER_SRC(reg32)                            REG_FLD_GET(L3_CON_FLD_LAYER_SRC, (reg32))
#define L3_CON_GET_RGB_SWAP(reg32)                             REG_FLD_GET(L3_CON_FLD_RGB_SWAP, (reg32))
#define L3_CON_GET_BYTE_SWAP(reg32)                            REG_FLD_GET(L3_CON_FLD_BYTE_SWAP, (reg32))
#define L3_CON_GET_R_FIRST(reg32)                              REG_FLD_GET(L3_CON_FLD_R_FIRST, (reg32))
#define L3_CON_GET_LANDSCAPE(reg32)                            REG_FLD_GET(L3_CON_FLD_LANDSCAPE, (reg32))
#define L3_CON_GET_EN_3D(reg32)                                REG_FLD_GET(L3_CON_FLD_EN_3D, (reg32))
#define L3_CON_GET_C_CF_SEL(reg32)                             REG_FLD_GET(L3_CON_FLD_C_CF_SEL, (reg32))
#define L3_CON_GET_CLRFMT(reg32)                               REG_FLD_GET(L3_CON_FLD_CLRFMT, (reg32))
#define L3_CON_GET_ALPHA_EN(reg32)                             REG_FLD_GET(L3_CON_FLD_ALPHA_EN, (reg32))
#define L3_CON_GET_ALPHA(reg32)                                REG_FLD_GET(L3_CON_FLD_ALPHA, (reg32))

#define L3_SRCKEY_GET_SRCKEY(reg32)                            REG_FLD_GET(L3_SRCKEY_FLD_SRCKEY, (reg32))

#define L3_SRC_SIZE_GET_L3_SRC_H(reg32)                        REG_FLD_GET(L3_SRC_SIZE_FLD_L3_SRC_H, (reg32))
#define L3_SRC_SIZE_GET_L3_SRC_W(reg32)                        REG_FLD_GET(L3_SRC_SIZE_FLD_L3_SRC_W, (reg32))

#define L3_OFFSET_GET_L3_YOFF(reg32)                           REG_FLD_GET(L3_OFFSET_FLD_L3_YOFF, (reg32))
#define L3_OFFSET_GET_L3_XOFF(reg32)                           REG_FLD_GET(L3_OFFSET_FLD_L3_XOFF, (reg32))

#define L3_ADDR_GET_L3_ADDR(reg32)                             REG_FLD_GET(L3_ADDR_FLD_L3_ADDR, (reg32))

#define L3_PITCH_GET_L3_SRC_PITCH(reg32)                       REG_FLD_GET(L3_PITCH_FLD_L3_SRC_PITCH, (reg32))

#define RDMA0_CTRL_GET_RDMA0_TRIG_TYPE(reg32)                  REG_FLD_GET(RDMA0_CTRL_FLD_RDMA0_TRIG_TYPE, (reg32))
#define RDMA0_CTRL_GET_RDMA0_EN(reg32)                         REG_FLD_GET(RDMA0_CTRL_FLD_RDMA0_EN, (reg32))

#define RDMA0_MEM_START_TRIG_GET_RDMA0_START_TRIG(reg32)       REG_FLD_GET(RDMA0_MEM_START_TRIG_FLD_RDMA0_START_TRIG, (reg32))

#define RDMA0_MEM_GMC_SETTING_GET_RDMA0_DISEN_THRD(reg32)      REG_FLD_GET(RDMA0_MEM_GMC_SETTING_FLD_RDMA0_DISEN_THRD, (reg32))
#define RDMA0_MEM_GMC_SETTING_GET_RDMA0_EN_THRD(reg32)         REG_FLD_GET(RDMA0_MEM_GMC_SETTING_FLD_RDMA0_EN_THRD, (reg32))

#define RDMA0_MEM_SLOW_CON_GET_RDMA0_SLOW_CNT(reg32)           REG_FLD_GET(RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_CNT, (reg32))
#define RDMA0_MEM_SLOW_CON_GET_RDMA0_SLOW_EN(reg32)            REG_FLD_GET(RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_EN, (reg32))

#define RDMA0_FIFO_CTRL_GET_RDMA0_FIFO_UND_EN(reg32)           REG_FLD_GET(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_UND_EN, (reg32))
#define RDMA0_FIFO_CTRL_GET_RDMA0_FIFO_SIZE(reg32)             REG_FLD_GET(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_SIZE, (reg32))
#define RDMA0_FIFO_CTRL_GET_RDMA0_FIFO_THRD(reg32)             REG_FLD_GET(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_THRD, (reg32))

#define RDMA1_CTRL_GET_RDMA1_TRIG_TYPE(reg32)                  REG_FLD_GET(RDMA1_CTRL_FLD_RDMA1_TRIG_TYPE, (reg32))
#define RDMA1_CTRL_GET_RDMA1_EN(reg32)                         REG_FLD_GET(RDMA1_CTRL_FLD_RDMA1_EN, (reg32))

#define RDMA1_MEM_START_TRIG_GET_RDMA1_START_TRIG(reg32)       REG_FLD_GET(RDMA1_MEM_START_TRIG_FLD_RDMA1_START_TRIG, (reg32))

#define RDMA1_MEM_GMC_SETTING_GET_RDMA1_DISEN_THRD(reg32)      REG_FLD_GET(RDMA1_MEM_GMC_SETTING_FLD_RDMA1_DISEN_THRD, (reg32))
#define RDMA1_MEM_GMC_SETTING_GET_RDMA1_EN_THRD(reg32)         REG_FLD_GET(RDMA1_MEM_GMC_SETTING_FLD_RDMA1_EN_THRD, (reg32))

#define RDMA1_MEM_SLOW_CON_GET_RDMA1_SLOW_CNT(reg32)           REG_FLD_GET(RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_CNT, (reg32))
#define RDMA1_MEM_SLOW_CON_GET_RDMA1_SLOW_EN(reg32)            REG_FLD_GET(RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_EN, (reg32))

#define RDMA1_FIFO_CTRL_GET_RDMA1_FIFO_UND_EN(reg32)           REG_FLD_GET(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_UND_EN, (reg32))
#define RDMA1_FIFO_CTRL_GET_RDMA1_FIFO_SIZE(reg32)             REG_FLD_GET(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_SIZE, (reg32))
#define RDMA1_FIFO_CTRL_GET_RDMA1_FIFO_THRD(reg32)             REG_FLD_GET(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_THRD, (reg32))

#define RDMA2_CTRL_GET_RDMA2_TRIG_TYPE(reg32)                  REG_FLD_GET(RDMA2_CTRL_FLD_RDMA2_TRIG_TYPE, (reg32))
#define RDMA2_CTRL_GET_RDMA2_EN(reg32)                         REG_FLD_GET(RDMA2_CTRL_FLD_RDMA2_EN, (reg32))

#define RDMA2_MEM_START_TRIG_GET_RDMA2_START_TRIG(reg32)       REG_FLD_GET(RDMA2_MEM_START_TRIG_FLD_RDMA2_START_TRIG, (reg32))

#define RDMA2_MEM_GMC_SETTING_GET_RDMA2_DISEN_THRD(reg32)      REG_FLD_GET(RDMA2_MEM_GMC_SETTING_FLD_RDMA2_DISEN_THRD, (reg32))
#define RDMA2_MEM_GMC_SETTING_GET_RDMA2_EN_THRD(reg32)         REG_FLD_GET(RDMA2_MEM_GMC_SETTING_FLD_RDMA2_EN_THRD, (reg32))

#define RDMA2_MEM_SLOW_CON_GET_RDMA2_SLOW_CNT(reg32)           REG_FLD_GET(RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_CNT, (reg32))
#define RDMA2_MEM_SLOW_CON_GET_RDMA2_SLOW_EN(reg32)            REG_FLD_GET(RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_EN, (reg32))

#define RDMA2_FIFO_CTRL_GET_RDMA2_FIFO_UND_EN(reg32)           REG_FLD_GET(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_UND_EN, (reg32))
#define RDMA2_FIFO_CTRL_GET_RDMA2_FIFO_SIZE(reg32)             REG_FLD_GET(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_SIZE, (reg32))
#define RDMA2_FIFO_CTRL_GET_RDMA2_FIFO_THRD(reg32)             REG_FLD_GET(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_THRD, (reg32))

#define RDMA3_CTRL_GET_RDMA3_TRIG_TYPE(reg32)                  REG_FLD_GET(RDMA3_CTRL_FLD_RDMA3_TRIG_TYPE, (reg32))
#define RDMA3_CTRL_GET_RDMA3_EN(reg32)                         REG_FLD_GET(RDMA3_CTRL_FLD_RDMA3_EN, (reg32))

#define RDMA3_MEM_START_TRIG_GET_RDMA3_START_TRIG(reg32)       REG_FLD_GET(RDMA3_MEM_START_TRIG_FLD_RDMA3_START_TRIG, (reg32))

#define RDMA3_MEM_GMC_SETTING_GET_RDMA3_DISEN_THRD(reg32)      REG_FLD_GET(RDMA3_MEM_GMC_SETTING_FLD_RDMA3_DISEN_THRD, (reg32))
#define RDMA3_MEM_GMC_SETTING_GET_RDMA3_EN_THRD(reg32)         REG_FLD_GET(RDMA3_MEM_GMC_SETTING_FLD_RDMA3_EN_THRD, (reg32))

#define RDMA3_MEM_SLOW_CON_GET_RDMA3_SLOW_CNT(reg32)           REG_FLD_GET(RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_CNT, (reg32))
#define RDMA3_MEM_SLOW_CON_GET_RDMA3_SLOW_EN(reg32)            REG_FLD_GET(RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_EN, (reg32))

#define RDMA3_FIFO_CTRL_GET_RDMA3_FIFO_UND_EN(reg32)           REG_FLD_GET(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_UND_EN, (reg32))
#define RDMA3_FIFO_CTRL_GET_RDMA3_FIFO_SIZE(reg32)             REG_FLD_GET(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_SIZE, (reg32))
#define RDMA3_FIFO_CTRL_GET_RDMA3_FIFO_THRD(reg32)             REG_FLD_GET(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_THRD, (reg32))

#define L0_Y2R_PARA_R0_GET_C_CF_RMU(reg32)                     REG_FLD_GET(L0_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32))
#define L0_Y2R_PARA_R0_GET_C_CF_RMY(reg32)                     REG_FLD_GET(L0_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32))

#define L0_Y2R_PARA_R1_GET_C_CF_RMV(reg32)                     REG_FLD_GET(L0_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32))

#define L0_Y2R_PARA_G0_GET_C_CF_GMU(reg32)                     REG_FLD_GET(L0_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32))
#define L0_Y2R_PARA_G0_GET_C_CF_GMY(reg32)                     REG_FLD_GET(L0_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32))

#define L0_Y2R_PARA_G1_GET_C_CF_GMV(reg32)                     REG_FLD_GET(L0_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32))

#define L0_Y2R_PARA_B0_GET_C_CF_BMU(reg32)                     REG_FLD_GET(L0_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32))
#define L0_Y2R_PARA_B0_GET_C_CF_BMY(reg32)                     REG_FLD_GET(L0_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32))

#define L0_Y2R_PARA_B1_GET_C_CF_BMV(reg32)                     REG_FLD_GET(L0_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32))

#define L0_Y2R_PARA_YUV_A_0_GET_C_CF_UA(reg32)                 REG_FLD_GET(L0_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32))
#define L0_Y2R_PARA_YUV_A_0_GET_C_CF_YA(reg32)                 REG_FLD_GET(L0_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32))

#define L0_Y2R_PARA_YUV_A_1_GET_C_CF_VA(reg32)                 REG_FLD_GET(L0_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32))

#define L0_Y2R_PARA_RGB_A_0_GET_C_CF_GA(reg32)                 REG_FLD_GET(L0_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32))
#define L0_Y2R_PARA_RGB_A_0_GET_C_CF_RA(reg32)                 REG_FLD_GET(L0_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32))

#define L0_Y2R_PARA_RGB_A_1_GET_C_CF_BA(reg32)                 REG_FLD_GET(L0_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32))

#define L1_Y2R_PARA_R0_GET_C_CF_RMU(reg32)                     REG_FLD_GET(L1_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32))
#define L1_Y2R_PARA_R0_GET_C_CF_RMY(reg32)                     REG_FLD_GET(L1_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32))

#define L1_Y2R_PARA_R1_GET_C_CF_RMV(reg32)                     REG_FLD_GET(L1_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32))

#define L1_Y2R_PARA_G0_GET_C_CF_GMU(reg32)                     REG_FLD_GET(L1_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32))
#define L1_Y2R_PARA_G0_GET_C_CF_GMY(reg32)                     REG_FLD_GET(L1_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32))

#define L1_Y2R_PARA_G1_GET_C_CF_GMV(reg32)                     REG_FLD_GET(L1_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32))

#define L1_Y2R_PARA_B0_GET_C_CF_BMU(reg32)                     REG_FLD_GET(L1_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32))
#define L1_Y2R_PARA_B0_GET_C_CF_BMY(reg32)                     REG_FLD_GET(L1_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32))

#define L1_Y2R_PARA_B1_GET_C_CF_BMV(reg32)                     REG_FLD_GET(L1_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32))

#define L1_Y2R_PARA_YUV_A_0_GET_C_CF_UA(reg32)                 REG_FLD_GET(L1_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32))
#define L1_Y2R_PARA_YUV_A_0_GET_C_CF_YA(reg32)                 REG_FLD_GET(L1_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32))

#define L1_Y2R_PARA_YUV_A_1_GET_C_CF_VA(reg32)                 REG_FLD_GET(L1_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32))

#define L1_Y2R_PARA_RGB_A_0_GET_C_CF_GA(reg32)                 REG_FLD_GET(L1_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32))
#define L1_Y2R_PARA_RGB_A_0_GET_C_CF_RA(reg32)                 REG_FLD_GET(L1_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32))

#define L1_Y2R_PARA_RGB_A_1_GET_C_CF_BA(reg32)                 REG_FLD_GET(L1_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32))

#define L2_Y2R_PARA_R0_GET_C_CF_RMU(reg32)                     REG_FLD_GET(L2_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32))
#define L2_Y2R_PARA_R0_GET_C_CF_RMY(reg32)                     REG_FLD_GET(L2_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32))

#define L2_Y2R_PARA_R1_GET_C_CF_RMV(reg32)                     REG_FLD_GET(L2_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32))

#define L2_Y2R_PARA_G0_GET_C_CF_GMU(reg32)                     REG_FLD_GET(L2_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32))
#define L2_Y2R_PARA_G0_GET_C_CF_GMY(reg32)                     REG_FLD_GET(L2_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32))

#define L2_Y2R_PARA_G1_GET_C_CF_GMV(reg32)                     REG_FLD_GET(L2_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32))

#define L2_Y2R_PARA_B0_GET_C_CF_BMU(reg32)                     REG_FLD_GET(L2_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32))
#define L2_Y2R_PARA_B0_GET_C_CF_BMY(reg32)                     REG_FLD_GET(L2_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32))

#define L2_Y2R_PARA_B1_GET_C_CF_BMV(reg32)                     REG_FLD_GET(L2_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32))

#define L2_Y2R_PARA_YUV_A_0_GET_C_CF_UA(reg32)                 REG_FLD_GET(L2_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32))
#define L2_Y2R_PARA_YUV_A_0_GET_C_CF_YA(reg32)                 REG_FLD_GET(L2_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32))

#define L2_Y2R_PARA_YUV_A_1_GET_C_CF_VA(reg32)                 REG_FLD_GET(L2_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32))

#define L2_Y2R_PARA_RGB_A_0_GET_C_CF_GA(reg32)                 REG_FLD_GET(L2_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32))
#define L2_Y2R_PARA_RGB_A_0_GET_C_CF_RA(reg32)                 REG_FLD_GET(L2_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32))

#define L2_Y2R_PARA_RGB_A_1_GET_C_CF_BA(reg32)                 REG_FLD_GET(L2_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32))

#define L3_Y2R_PARA_R0_GET_C_CF_RMU(reg32)                     REG_FLD_GET(L3_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32))
#define L3_Y2R_PARA_R0_GET_C_CF_RMY(reg32)                     REG_FLD_GET(L3_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32))

#define L3_Y2R_PARA_R1_GET_C_CF_RMV(reg32)                     REG_FLD_GET(L3_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32))

#define L3_Y2R_PARA_G0_GET_C_CF_GMU(reg32)                     REG_FLD_GET(L3_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32))
#define L3_Y2R_PARA_G0_GET_C_CF_GMY(reg32)                     REG_FLD_GET(L3_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32))

#define L3_Y2R_PARA_G1_GET_C_CF_GMV(reg32)                     REG_FLD_GET(L3_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32))

#define L3_Y2R_PARA_B0_GET_C_CF_BMU(reg32)                     REG_FLD_GET(L3_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32))
#define L3_Y2R_PARA_B0_GET_C_CF_BMY(reg32)                     REG_FLD_GET(L3_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32))

#define L3_Y2R_PARA_B1_GET_C_CF_BMV(reg32)                     REG_FLD_GET(L3_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32))

#define L3_Y2R_PARA_YUV_A_0_GET_C_CF_UA(reg32)                 REG_FLD_GET(L3_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32))
#define L3_Y2R_PARA_YUV_A_0_GET_C_CF_YA(reg32)                 REG_FLD_GET(L3_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32))

#define L3_Y2R_PARA_YUV_A_1_GET_C_CF_VA(reg32)                 REG_FLD_GET(L3_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32))

#define L3_Y2R_PARA_RGB_A_0_GET_C_CF_GA(reg32)                 REG_FLD_GET(L3_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32))
#define L3_Y2R_PARA_RGB_A_0_GET_C_CF_RA(reg32)                 REG_FLD_GET(L3_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32))

#define L3_Y2R_PARA_RGB_A_1_GET_C_CF_BA(reg32)                 REG_FLD_GET(L3_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32))

#define FLOW_CTRL_DBG_GET_FLOW_DBG(reg32)                      REG_FLD_GET(FLOW_CTRL_DBG_FLD_FLOW_DBG, (reg32))

#define ADDCON_DBG_GET_ROI_Y(reg32)                            REG_FLD_GET(ADDCON_DBG_FLD_ROI_Y, (reg32))
#define ADDCON_DBG_GET_ROI_X(reg32)                            REG_FLD_GET(ADDCON_DBG_FLD_ROI_X, (reg32))

#define OUTMUX_DBG_GET_OUT_DATA(reg32)                         REG_FLD_GET(OUTMUX_DBG_FLD_OUT_DATA, (reg32))
#define OUTMUX_DBG_GET_OUT_VALID(reg32)                        REG_FLD_GET(OUTMUX_DBG_FLD_OUT_VALID, (reg32))
#define OUTMUX_DBG_GET_OUT_READY(reg32)                        REG_FLD_GET(OUTMUX_DBG_FLD_OUT_READY, (reg32))

#define RDMA0_DBG_GET_CUR_Y0(reg32)                            REG_FLD_GET(RDMA0_DBG_FLD_CUR_Y0, (reg32))
#define RDMA0_DBG_GET_CUR_X0(reg32)                            REG_FLD_GET(RDMA0_DBG_FLD_CUR_X0, (reg32))

#define RDMA1_DBG_GET_CUR_Y1(reg32)                            REG_FLD_GET(RDMA1_DBG_FLD_CUR_Y1, (reg32))
#define RDMA1_DBG_GET_CUR_X1(reg32)                            REG_FLD_GET(RDMA1_DBG_FLD_CUR_X1, (reg32))

#define RDMA2_DBG_GET_CUR_Y2(reg32)                            REG_FLD_GET(RDMA2_DBG_FLD_CUR_Y2, (reg32))
#define RDMA2_DBG_GET_CUR_X2(reg32)                            REG_FLD_GET(RDMA2_DBG_FLD_CUR_X2, (reg32))

#define RDMA3_DBG_GET_CUR_Y3(reg32)                            REG_FLD_GET(RDMA3_DBG_FLD_CUR_Y3, (reg32))
#define RDMA3_DBG_GET_CUR_X3(reg32)                            REG_FLD_GET(RDMA3_DBG_FLD_CUR_X3, (reg32))

#define STA_SET_RDMA3_RST_PERIOD(reg32, val)                   REG_FLD_SET(STA_FLD_RDMA3_RST_PERIOD, (reg32), (val))
#define STA_SET_RDMA2_RST_PERIOD(reg32, val)                   REG_FLD_SET(STA_FLD_RDMA2_RST_PERIOD, (reg32), (val))
#define STA_SET_RDMA1_RST_PERIOD(reg32, val)                   REG_FLD_SET(STA_FLD_RDMA1_RST_PERIOD, (reg32), (val))
#define STA_SET_RDMA0_RST_PERIOD(reg32, val)                   REG_FLD_SET(STA_FLD_RDMA0_RST_PERIOD, (reg32), (val))
#define STA_SET_OVL_RUN(reg32, val)                            REG_FLD_SET(STA_FLD_OVL_RUN, (reg32), (val))

#define INTEN_SET_RDMA3_FIFO_UNDERFLOW_INTEN(reg32, val)       REG_FLD_SET(INTEN_FLD_RDMA3_FIFO_UNDERFLOW_INTEN, (reg32), (val))
#define INTEN_SET_RDMA2_FIFO_UNDERFLOW_INTEN(reg32, val)       REG_FLD_SET(INTEN_FLD_RDMA2_FIFO_UNDERFLOW_INTEN, (reg32), (val))
#define INTEN_SET_RDMA1_FIFO_UNDERFLOW_INTEN(reg32, val)       REG_FLD_SET(INTEN_FLD_RDMA1_FIFO_UNDERFLOW_INTEN, (reg32), (val))
#define INTEN_SET_RDMA0_FIFO_UNDERFLOW_INTEN(reg32, val)       REG_FLD_SET(INTEN_FLD_RDMA0_FIFO_UNDERFLOW_INTEN, (reg32), (val))
#define INTEN_SET_RDMA3_EOF_ABNORMAL_INTEN(reg32, val)         REG_FLD_SET(INTEN_FLD_RDMA3_EOF_ABNORMAL_INTEN, (reg32), (val))
#define INTEN_SET_RDMA2_EOF_ABNORMAL_INTEN(reg32, val)         REG_FLD_SET(INTEN_FLD_RDMA2_EOF_ABNORMAL_INTEN, (reg32), (val))
#define INTEN_SET_RDMA1_EOF_ABNORMAL_INTEN(reg32, val)         REG_FLD_SET(INTEN_FLD_RDMA1_EOF_ABNORMAL_INTEN, (reg32), (val))
#define INTEN_SET_RDMA0_EOF_ABNORMAL_INTEN(reg32, val)         REG_FLD_SET(INTEN_FLD_RDMA0_EOF_ABNORMAL_INTEN, (reg32), (val))
#define INTEN_SET_OVL_FME_SWRST_DONE_INTEN(reg32, val)         REG_FLD_SET(INTEN_FLD_OVL_FME_SWRST_DONE_INTEN, (reg32), (val))
#define INTEN_SET_OVL_FME_UND_INTEN(reg32, val)                REG_FLD_SET(INTEN_FLD_OVL_FME_UND_INTEN, (reg32), (val))
#define INTEN_SET_OVL_FME_CPL_INTEN(reg32, val)                REG_FLD_SET(INTEN_FLD_OVL_FME_CPL_INTEN, (reg32), (val))
#define INTEN_SET_OVL_REG_CMT_INTEN(reg32, val)                REG_FLD_SET(INTEN_FLD_OVL_REG_CMT_INTEN, (reg32), (val))

#define INTSTA_SET_RDMA3_FIFO_UNDERFLOW_INTSTA(reg32, val)     REG_FLD_SET(INTSTA_FLD_RDMA3_FIFO_UNDERFLOW_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA2_FIFO_UNDERFLOW_INTSTA(reg32, val)     REG_FLD_SET(INTSTA_FLD_RDMA2_FIFO_UNDERFLOW_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA1_FIFO_UNDERFLOW_INTSTA(reg32, val)     REG_FLD_SET(INTSTA_FLD_RDMA1_FIFO_UNDERFLOW_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA0_FIFO_UNDERFLOW_INTSTA(reg32, val)     REG_FLD_SET(INTSTA_FLD_RDMA0_FIFO_UNDERFLOW_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA3_EOF_ABNORMAL_INTSTA(reg32, val)       REG_FLD_SET(INTSTA_FLD_RDMA3_EOF_ABNORMAL_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA2_EOF_ABNORMAL_INTSTA(reg32, val)       REG_FLD_SET(INTSTA_FLD_RDMA2_EOF_ABNORMAL_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA1_EOF_ABNORMAL_INTSTA(reg32, val)       REG_FLD_SET(INTSTA_FLD_RDMA1_EOF_ABNORMAL_INTSTA, (reg32), (val))
#define INTSTA_SET_RDMA0_EOF_ABNORMAL_INTSTA(reg32, val)       REG_FLD_SET(INTSTA_FLD_RDMA0_EOF_ABNORMAL_INTSTA, (reg32), (val))
#define INTSTA_SET_OVL_FME_SWRST_DONE_INTSTA(reg32, val)       REG_FLD_SET(INTSTA_FLD_OVL_FME_SWRST_DONE_INTSTA, (reg32), (val))
#define INTSTA_SET_OVL_FME_UND_INTSTA(reg32, val)              REG_FLD_SET(INTSTA_FLD_OVL_FME_UND_INTSTA, (reg32), (val))
#define INTSTA_SET_OVL_FME_CPL_INTSTA(reg32, val)              REG_FLD_SET(INTSTA_FLD_OVL_FME_CPL_INTSTA, (reg32), (val))
#define INTSTA_SET_OVL_REG_CMT_INTSTA(reg32, val)              REG_FLD_SET(INTSTA_FLD_OVL_REG_CMT_INTSTA, (reg32), (val))

#define EN_SET_OVL_EN(reg32, val)                              REG_FLD_SET(EN_FLD_OVL_EN, (reg32), (val))

#define TRIG_SET_OVL_SW_TRIG(reg32, val)                       REG_FLD_SET(TRIG_FLD_OVL_SW_TRIG, (reg32), (val))

#define RST_SET_OVL_RSTB(reg32, val)                           REG_FLD_SET(RST_FLD_OVL_RSTB, (reg32), (val))

#define ROI_SIZE_SET_ROI_H(reg32, val)                         REG_FLD_SET(ROI_SIZE_FLD_ROI_H, (reg32), (val))
#define ROI_SIZE_SET_ROI_W(reg32, val)                         REG_FLD_SET(ROI_SIZE_FLD_ROI_W, (reg32), (val))

#define ROI_BGCLR_SET_ALPHA(reg32, val)                        REG_FLD_SET(ROI_BGCLR_FLD_ALPHA, (reg32), (val))
#define ROI_BGCLR_SET_RED(reg32, val)                          REG_FLD_SET(ROI_BGCLR_FLD_RED, (reg32), (val))
#define ROI_BGCLR_SET_GREEN(reg32, val)                        REG_FLD_SET(ROI_BGCLR_FLD_GREEN, (reg32), (val))
#define ROI_BGCLR_SET_BLUE(reg32, val)                         REG_FLD_SET(ROI_BGCLR_FLD_BLUE, (reg32), (val))

#define SRC_CON_SET_L3_EN(reg32, val)                          REG_FLD_SET(SRC_CON_FLD_L3_EN, (reg32), (val))
#define SRC_CON_SET_L2_EN(reg32, val)                          REG_FLD_SET(SRC_CON_FLD_L2_EN, (reg32), (val))
#define SRC_CON_SET_L1_EN(reg32, val)                          REG_FLD_SET(SRC_CON_FLD_L1_EN, (reg32), (val))
#define SRC_CON_SET_L0_EN(reg32, val)                          REG_FLD_SET(SRC_CON_FLD_L0_EN, (reg32), (val))

#define L0_CON_SET_DSTKEY_EN(reg32, val)                       REG_FLD_SET(L0_CON_FLD_DSTKEY_EN, (reg32), (val))
#define L0_CON_SET_SRCKEY_EN(reg32, val)                       REG_FLD_SET(L0_CON_FLD_SRCKEY_EN, (reg32), (val))
#define L0_CON_SET_LAYER_SRC(reg32, val)                       REG_FLD_SET(L0_CON_FLD_LAYER_SRC, (reg32), (val))
#define L0_CON_SET_RGB_SWAP(reg32, val)                        REG_FLD_SET(L0_CON_FLD_RGB_SWAP, (reg32), (val))
#define L0_CON_SET_BYTE_SWAP(reg32, val)                       REG_FLD_SET(L0_CON_FLD_BYTE_SWAP, (reg32), (val))
#define L0_CON_SET_R_FIRST(reg32, val)                         REG_FLD_SET(L0_CON_FLD_R_FIRST, (reg32), (val))
#define L0_CON_SET_LANDSCAPE(reg32, val)                       REG_FLD_SET(L0_CON_FLD_LANDSCAPE, (reg32), (val))
#define L0_CON_SET_EN_3D(reg32, val)                           REG_FLD_SET(L0_CON_FLD_EN_3D, (reg32), (val))
#define L0_CON_SET_C_CF_SEL(reg32, val)                        REG_FLD_SET(L0_CON_FLD_C_CF_SEL, (reg32), (val))
#define L0_CON_SET_CLRFMT(reg32, val)                          REG_FLD_SET(L0_CON_FLD_CLRFMT, (reg32), (val))
#define L0_CON_SET_ALPHA_EN(reg32, val)                        REG_FLD_SET(L0_CON_FLD_ALPHA_EN, (reg32), (val))
#define L0_CON_SET_ALPHA(reg32, val)                           REG_FLD_SET(L0_CON_FLD_ALPHA, (reg32), (val))

#define L0_SRCKEY_SET_SRCKEY(reg32, val)                       REG_FLD_SET(L0_SRCKEY_FLD_SRCKEY, (reg32), (val))

#define L0_SRC_SIZE_SET_L0_SRC_H(reg32, val)                   REG_FLD_SET(L0_SRC_SIZE_FLD_L0_SRC_H, (reg32), (val))
#define L0_SRC_SIZE_SET_L0_SRC_W(reg32, val)                   REG_FLD_SET(L0_SRC_SIZE_FLD_L0_SRC_W, (reg32), (val))

#define L0_OFFSET_SET_L0_YOFF(reg32, val)                      REG_FLD_SET(L0_OFFSET_FLD_L0_YOFF, (reg32), (val))
#define L0_OFFSET_SET_L0_XOFF(reg32, val)                      REG_FLD_SET(L0_OFFSET_FLD_L0_XOFF, (reg32), (val))

#define L0_ADDR_SET_L0_ADDR(reg32, val)                        REG_FLD_SET(L0_ADDR_FLD_L0_ADDR, (reg32), (val))

#define L0_PITCH_SET_L0_SRC_PITCH(reg32, val)                  REG_FLD_SET(L0_PITCH_FLD_L0_SRC_PITCH, (reg32), (val))

#define L1_CON_SET_DSTKEY_EN(reg32, val)                       REG_FLD_SET(L1_CON_FLD_DSTKEY_EN, (reg32), (val))
#define L1_CON_SET_SRCKEY_EN(reg32, val)                       REG_FLD_SET(L1_CON_FLD_SRCKEY_EN, (reg32), (val))
#define L1_CON_SET_LAYER_SRC(reg32, val)                       REG_FLD_SET(L1_CON_FLD_LAYER_SRC, (reg32), (val))
#define L1_CON_SET_RGB_SWAP(reg32, val)                        REG_FLD_SET(L1_CON_FLD_RGB_SWAP, (reg32), (val))
#define L1_CON_SET_BYTE_SWAP(reg32, val)                       REG_FLD_SET(L1_CON_FLD_BYTE_SWAP, (reg32), (val))
#define L1_CON_SET_R_FIRST(reg32, val)                         REG_FLD_SET(L1_CON_FLD_R_FIRST, (reg32), (val))
#define L1_CON_SET_LANDSCAPE(reg32, val)                       REG_FLD_SET(L1_CON_FLD_LANDSCAPE, (reg32), (val))
#define L1_CON_SET_EN_3D(reg32, val)                           REG_FLD_SET(L1_CON_FLD_EN_3D, (reg32), (val))
#define L1_CON_SET_C_CF_SEL(reg32, val)                        REG_FLD_SET(L1_CON_FLD_C_CF_SEL, (reg32), (val))
#define L1_CON_SET_CLRFMT(reg32, val)                          REG_FLD_SET(L1_CON_FLD_CLRFMT, (reg32), (val))
#define L1_CON_SET_ALPHA_EN(reg32, val)                        REG_FLD_SET(L1_CON_FLD_ALPHA_EN, (reg32), (val))
#define L1_CON_SET_ALPHA(reg32, val)                           REG_FLD_SET(L1_CON_FLD_ALPHA, (reg32), (val))

#define L1_SRCKEY_SET_SRCKEY(reg32, val)                       REG_FLD_SET(L1_SRCKEY_FLD_SRCKEY, (reg32), (val))

#define L1_SRC_SIZE_SET_L1_SRC_H(reg32, val)                   REG_FLD_SET(L1_SRC_SIZE_FLD_L1_SRC_H, (reg32), (val))
#define L1_SRC_SIZE_SET_L1_SRC_W(reg32, val)                   REG_FLD_SET(L1_SRC_SIZE_FLD_L1_SRC_W, (reg32), (val))

#define L1_OFFSET_SET_L1_YOFF(reg32, val)                      REG_FLD_SET(L1_OFFSET_FLD_L1_YOFF, (reg32), (val))
#define L1_OFFSET_SET_L1_XOFF(reg32, val)                      REG_FLD_SET(L1_OFFSET_FLD_L1_XOFF, (reg32), (val))

#define L1_ADDR_SET_L1_ADDR(reg32, val)                        REG_FLD_SET(L1_ADDR_FLD_L1_ADDR, (reg32), (val))

#define L1_PITCH_SET_L1_SRC_PITCH(reg32, val)                  REG_FLD_SET(L1_PITCH_FLD_L1_SRC_PITCH, (reg32), (val))

#define L2_CON_SET_DSTKEY_EN(reg32, val)                       REG_FLD_SET(L2_CON_FLD_DSTKEY_EN, (reg32), (val))
#define L2_CON_SET_SRCKEY_EN(reg32, val)                       REG_FLD_SET(L2_CON_FLD_SRCKEY_EN, (reg32), (val))
#define L2_CON_SET_LAYER_SRC(reg32, val)                       REG_FLD_SET(L2_CON_FLD_LAYER_SRC, (reg32), (val))
#define L2_CON_SET_RGB_SWAP(reg32, val)                        REG_FLD_SET(L2_CON_FLD_RGB_SWAP, (reg32), (val))
#define L2_CON_SET_BYTE_SWAP(reg32, val)                       REG_FLD_SET(L2_CON_FLD_BYTE_SWAP, (reg32), (val))
#define L2_CON_SET_R_FIRST(reg32, val)                         REG_FLD_SET(L2_CON_FLD_R_FIRST, (reg32), (val))
#define L2_CON_SET_LANDSCAPE(reg32, val)                       REG_FLD_SET(L2_CON_FLD_LANDSCAPE, (reg32), (val))
#define L2_CON_SET_EN_3D(reg32, val)                           REG_FLD_SET(L2_CON_FLD_EN_3D, (reg32), (val))
#define L2_CON_SET_C_CF_SEL(reg32, val)                        REG_FLD_SET(L2_CON_FLD_C_CF_SEL, (reg32), (val))
#define L2_CON_SET_CLRFMT(reg32, val)                          REG_FLD_SET(L2_CON_FLD_CLRFMT, (reg32), (val))
#define L2_CON_SET_ALPHA_EN(reg32, val)                        REG_FLD_SET(L2_CON_FLD_ALPHA_EN, (reg32), (val))
#define L2_CON_SET_ALPHA(reg32, val)                           REG_FLD_SET(L2_CON_FLD_ALPHA, (reg32), (val))

#define L2_SRCKEY_SET_SRCKEY(reg32, val)                       REG_FLD_SET(L2_SRCKEY_FLD_SRCKEY, (reg32), (val))

#define L2_SRC_SIZE_SET_L2_SRC_H(reg32, val)                   REG_FLD_SET(L2_SRC_SIZE_FLD_L2_SRC_H, (reg32), (val))
#define L2_SRC_SIZE_SET_L2_SRC_W(reg32, val)                   REG_FLD_SET(L2_SRC_SIZE_FLD_L2_SRC_W, (reg32), (val))

#define L2_OFFSET_SET_L2_YOFF(reg32, val)                      REG_FLD_SET(L2_OFFSET_FLD_L2_YOFF, (reg32), (val))
#define L2_OFFSET_SET_L2_XOFF(reg32, val)                      REG_FLD_SET(L2_OFFSET_FLD_L2_XOFF, (reg32), (val))

#define L2_ADDR_SET_L2_ADDR(reg32, val)                        REG_FLD_SET(L2_ADDR_FLD_L2_ADDR, (reg32), (val))

#define L2_PITCH_SET_L2_SRC_PITCH(reg32, val)                  REG_FLD_SET(L2_PITCH_FLD_L2_SRC_PITCH, (reg32), (val))

#define L3_CON_SET_DSTKEY_EN(reg32, val)                       REG_FLD_SET(L3_CON_FLD_DSTKEY_EN, (reg32), (val))
#define L3_CON_SET_SRCKEY_EN(reg32, val)                       REG_FLD_SET(L3_CON_FLD_SRCKEY_EN, (reg32), (val))
#define L3_CON_SET_LAYER_SRC(reg32, val)                       REG_FLD_SET(L3_CON_FLD_LAYER_SRC, (reg32), (val))
#define L3_CON_SET_RGB_SWAP(reg32, val)                        REG_FLD_SET(L3_CON_FLD_RGB_SWAP, (reg32), (val))
#define L3_CON_SET_BYTE_SWAP(reg32, val)                       REG_FLD_SET(L3_CON_FLD_BYTE_SWAP, (reg32), (val))
#define L3_CON_SET_R_FIRST(reg32, val)                         REG_FLD_SET(L3_CON_FLD_R_FIRST, (reg32), (val))
#define L3_CON_SET_LANDSCAPE(reg32, val)                       REG_FLD_SET(L3_CON_FLD_LANDSCAPE, (reg32), (val))
#define L3_CON_SET_EN_3D(reg32, val)                           REG_FLD_SET(L3_CON_FLD_EN_3D, (reg32), (val))
#define L3_CON_SET_C_CF_SEL(reg32, val)                        REG_FLD_SET(L3_CON_FLD_C_CF_SEL, (reg32), (val))
#define L3_CON_SET_CLRFMT(reg32, val)                          REG_FLD_SET(L3_CON_FLD_CLRFMT, (reg32), (val))
#define L3_CON_SET_ALPHA_EN(reg32, val)                        REG_FLD_SET(L3_CON_FLD_ALPHA_EN, (reg32), (val))
#define L3_CON_SET_ALPHA(reg32, val)                           REG_FLD_SET(L3_CON_FLD_ALPHA, (reg32), (val))

#define L3_SRCKEY_SET_SRCKEY(reg32, val)                       REG_FLD_SET(L3_SRCKEY_FLD_SRCKEY, (reg32), (val))

#define L3_SRC_SIZE_SET_L3_SRC_H(reg32, val)                   REG_FLD_SET(L3_SRC_SIZE_FLD_L3_SRC_H, (reg32), (val))
#define L3_SRC_SIZE_SET_L3_SRC_W(reg32, val)                   REG_FLD_SET(L3_SRC_SIZE_FLD_L3_SRC_W, (reg32), (val))

#define L3_OFFSET_SET_L3_YOFF(reg32, val)                      REG_FLD_SET(L3_OFFSET_FLD_L3_YOFF, (reg32), (val))
#define L3_OFFSET_SET_L3_XOFF(reg32, val)                      REG_FLD_SET(L3_OFFSET_FLD_L3_XOFF, (reg32), (val))

#define L3_ADDR_SET_L3_ADDR(reg32, val)                        REG_FLD_SET(L3_ADDR_FLD_L3_ADDR, (reg32), (val))

#define L3_PITCH_SET_L3_SRC_PITCH(reg32, val)                  REG_FLD_SET(L3_PITCH_FLD_L3_SRC_PITCH, (reg32), (val))

#define RDMA0_CTRL_SET_RDMA0_TRIG_TYPE(reg32, val)             REG_FLD_SET(RDMA0_CTRL_FLD_RDMA0_TRIG_TYPE, (reg32), (val))
#define RDMA0_CTRL_SET_RDMA0_EN(reg32, val)                    REG_FLD_SET(RDMA0_CTRL_FLD_RDMA0_EN, (reg32), (val))

#define RDMA0_MEM_START_TRIG_SET_RDMA0_START_TRIG(reg32, val)  REG_FLD_SET(RDMA0_MEM_START_TRIG_FLD_RDMA0_START_TRIG, (reg32), (val))

#define RDMA0_MEM_GMC_SETTING_SET_RDMA0_DISEN_THRD(reg32, val) REG_FLD_SET(RDMA0_MEM_GMC_SETTING_FLD_RDMA0_DISEN_THRD, (reg32), (val))
#define RDMA0_MEM_GMC_SETTING_SET_RDMA0_EN_THRD(reg32, val)    REG_FLD_SET(RDMA0_MEM_GMC_SETTING_FLD_RDMA0_EN_THRD, (reg32), (val))

#define RDMA0_MEM_SLOW_CON_SET_RDMA0_SLOW_CNT(reg32, val)      REG_FLD_SET(RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_CNT, (reg32), (val))
#define RDMA0_MEM_SLOW_CON_SET_RDMA0_SLOW_EN(reg32, val)       REG_FLD_SET(RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_EN, (reg32), (val))

#define RDMA0_FIFO_CTRL_SET_RDMA0_FIFO_UND_EN(reg32, val)      REG_FLD_SET(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_UND_EN, (reg32), (val))
#define RDMA0_FIFO_CTRL_SET_RDMA0_FIFO_SIZE(reg32, val)        REG_FLD_SET(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_SIZE, (reg32), (val))
#define RDMA0_FIFO_CTRL_SET_RDMA0_FIFO_THRD(reg32, val)        REG_FLD_SET(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_THRD, (reg32), (val))

#define RDMA1_CTRL_SET_RDMA1_TRIG_TYPE(reg32, val)             REG_FLD_SET(RDMA1_CTRL_FLD_RDMA1_TRIG_TYPE, (reg32), (val))
#define RDMA1_CTRL_SET_RDMA1_EN(reg32, val)                    REG_FLD_SET(RDMA1_CTRL_FLD_RDMA1_EN, (reg32), (val))

#define RDMA1_MEM_START_TRIG_SET_RDMA1_START_TRIG(reg32, val)  REG_FLD_SET(RDMA1_MEM_START_TRIG_FLD_RDMA1_START_TRIG, (reg32), (val))

#define RDMA1_MEM_GMC_SETTING_SET_RDMA1_DISEN_THRD(reg32, val) REG_FLD_SET(RDMA1_MEM_GMC_SETTING_FLD_RDMA1_DISEN_THRD, (reg32), (val))
#define RDMA1_MEM_GMC_SETTING_SET_RDMA1_EN_THRD(reg32, val)    REG_FLD_SET(RDMA1_MEM_GMC_SETTING_FLD_RDMA1_EN_THRD, (reg32), (val))

#define RDMA1_MEM_SLOW_CON_SET_RDMA1_SLOW_CNT(reg32, val)      REG_FLD_SET(RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_CNT, (reg32), (val))
#define RDMA1_MEM_SLOW_CON_SET_RDMA1_SLOW_EN(reg32, val)       REG_FLD_SET(RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_EN, (reg32), (val))

#define RDMA1_FIFO_CTRL_SET_RDMA1_FIFO_UND_EN(reg32, val)      REG_FLD_SET(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_UND_EN, (reg32), (val))
#define RDMA1_FIFO_CTRL_SET_RDMA1_FIFO_SIZE(reg32, val)        REG_FLD_SET(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_SIZE, (reg32), (val))
#define RDMA1_FIFO_CTRL_SET_RDMA1_FIFO_THRD(reg32, val)        REG_FLD_SET(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_THRD, (reg32), (val))

#define RDMA2_CTRL_SET_RDMA2_TRIG_TYPE(reg32, val)             REG_FLD_SET(RDMA2_CTRL_FLD_RDMA2_TRIG_TYPE, (reg32), (val))
#define RDMA2_CTRL_SET_RDMA2_EN(reg32, val)                    REG_FLD_SET(RDMA2_CTRL_FLD_RDMA2_EN, (reg32), (val))

#define RDMA2_MEM_START_TRIG_SET_RDMA2_START_TRIG(reg32, val)  REG_FLD_SET(RDMA2_MEM_START_TRIG_FLD_RDMA2_START_TRIG, (reg32), (val))

#define RDMA2_MEM_GMC_SETTING_SET_RDMA2_DISEN_THRD(reg32, val) REG_FLD_SET(RDMA2_MEM_GMC_SETTING_FLD_RDMA2_DISEN_THRD, (reg32), (val))
#define RDMA2_MEM_GMC_SETTING_SET_RDMA2_EN_THRD(reg32, val)    REG_FLD_SET(RDMA2_MEM_GMC_SETTING_FLD_RDMA2_EN_THRD, (reg32), (val))

#define RDMA2_MEM_SLOW_CON_SET_RDMA2_SLOW_CNT(reg32, val)      REG_FLD_SET(RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_CNT, (reg32), (val))
#define RDMA2_MEM_SLOW_CON_SET_RDMA2_SLOW_EN(reg32, val)       REG_FLD_SET(RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_EN, (reg32), (val))

#define RDMA2_FIFO_CTRL_SET_RDMA2_FIFO_UND_EN(reg32, val)      REG_FLD_SET(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_UND_EN, (reg32), (val))
#define RDMA2_FIFO_CTRL_SET_RDMA2_FIFO_SIZE(reg32, val)        REG_FLD_SET(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_SIZE, (reg32), (val))
#define RDMA2_FIFO_CTRL_SET_RDMA2_FIFO_THRD(reg32, val)        REG_FLD_SET(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_THRD, (reg32), (val))

#define RDMA3_CTRL_SET_RDMA3_TRIG_TYPE(reg32, val)             REG_FLD_SET(RDMA3_CTRL_FLD_RDMA3_TRIG_TYPE, (reg32), (val))
#define RDMA3_CTRL_SET_RDMA3_EN(reg32, val)                    REG_FLD_SET(RDMA3_CTRL_FLD_RDMA3_EN, (reg32), (val))

#define RDMA3_MEM_START_TRIG_SET_RDMA3_START_TRIG(reg32, val)  REG_FLD_SET(RDMA3_MEM_START_TRIG_FLD_RDMA3_START_TRIG, (reg32), (val))

#define RDMA3_MEM_GMC_SETTING_SET_RDMA3_DISEN_THRD(reg32, val) REG_FLD_SET(RDMA3_MEM_GMC_SETTING_FLD_RDMA3_DISEN_THRD, (reg32), (val))
#define RDMA3_MEM_GMC_SETTING_SET_RDMA3_EN_THRD(reg32, val)    REG_FLD_SET(RDMA3_MEM_GMC_SETTING_FLD_RDMA3_EN_THRD, (reg32), (val))

#define RDMA3_MEM_SLOW_CON_SET_RDMA3_SLOW_CNT(reg32, val)      REG_FLD_SET(RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_CNT, (reg32), (val))
#define RDMA3_MEM_SLOW_CON_SET_RDMA3_SLOW_EN(reg32, val)       REG_FLD_SET(RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_EN, (reg32), (val))

#define RDMA3_FIFO_CTRL_SET_RDMA3_FIFO_UND_EN(reg32, val)      REG_FLD_SET(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_UND_EN, (reg32), (val))
#define RDMA3_FIFO_CTRL_SET_RDMA3_FIFO_SIZE(reg32, val)        REG_FLD_SET(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_SIZE, (reg32), (val))
#define RDMA3_FIFO_CTRL_SET_RDMA3_FIFO_THRD(reg32, val)        REG_FLD_SET(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_THRD, (reg32), (val))

#define L0_Y2R_PARA_R0_SET_C_CF_RMU(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32), (val))
#define L0_Y2R_PARA_R0_SET_C_CF_RMY(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32), (val))

#define L0_Y2R_PARA_R1_SET_C_CF_RMV(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32), (val))

#define L0_Y2R_PARA_G0_SET_C_CF_GMU(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32), (val))
#define L0_Y2R_PARA_G0_SET_C_CF_GMY(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32), (val))

#define L0_Y2R_PARA_G1_SET_C_CF_GMV(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32), (val))

#define L0_Y2R_PARA_B0_SET_C_CF_BMU(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32), (val))
#define L0_Y2R_PARA_B0_SET_C_CF_BMY(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32), (val))

#define L0_Y2R_PARA_B1_SET_C_CF_BMV(reg32, val)                REG_FLD_SET(L0_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32), (val))

#define L0_Y2R_PARA_YUV_A_0_SET_C_CF_UA(reg32, val)            REG_FLD_SET(L0_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32), (val))
#define L0_Y2R_PARA_YUV_A_0_SET_C_CF_YA(reg32, val)            REG_FLD_SET(L0_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32), (val))

#define L0_Y2R_PARA_YUV_A_1_SET_C_CF_VA(reg32, val)            REG_FLD_SET(L0_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32), (val))

#define L0_Y2R_PARA_RGB_A_0_SET_C_CF_GA(reg32, val)            REG_FLD_SET(L0_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32), (val))
#define L0_Y2R_PARA_RGB_A_0_SET_C_CF_RA(reg32, val)            REG_FLD_SET(L0_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32), (val))

#define L0_Y2R_PARA_RGB_A_1_SET_C_CF_BA(reg32, val)            REG_FLD_SET(L0_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32), (val))

#define L1_Y2R_PARA_R0_SET_C_CF_RMU(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32), (val))
#define L1_Y2R_PARA_R0_SET_C_CF_RMY(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32), (val))

#define L1_Y2R_PARA_R1_SET_C_CF_RMV(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32), (val))

#define L1_Y2R_PARA_G0_SET_C_CF_GMU(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32), (val))
#define L1_Y2R_PARA_G0_SET_C_CF_GMY(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32), (val))

#define L1_Y2R_PARA_G1_SET_C_CF_GMV(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32), (val))

#define L1_Y2R_PARA_B0_SET_C_CF_BMU(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32), (val))
#define L1_Y2R_PARA_B0_SET_C_CF_BMY(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32), (val))

#define L1_Y2R_PARA_B1_SET_C_CF_BMV(reg32, val)                REG_FLD_SET(L1_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32), (val))

#define L1_Y2R_PARA_YUV_A_0_SET_C_CF_UA(reg32, val)            REG_FLD_SET(L1_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32), (val))
#define L1_Y2R_PARA_YUV_A_0_SET_C_CF_YA(reg32, val)            REG_FLD_SET(L1_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32), (val))

#define L1_Y2R_PARA_YUV_A_1_SET_C_CF_VA(reg32, val)            REG_FLD_SET(L1_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32), (val))

#define L1_Y2R_PARA_RGB_A_0_SET_C_CF_GA(reg32, val)            REG_FLD_SET(L1_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32), (val))
#define L1_Y2R_PARA_RGB_A_0_SET_C_CF_RA(reg32, val)            REG_FLD_SET(L1_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32), (val))

#define L1_Y2R_PARA_RGB_A_1_SET_C_CF_BA(reg32, val)            REG_FLD_SET(L1_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32), (val))

#define L2_Y2R_PARA_R0_SET_C_CF_RMU(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32), (val))
#define L2_Y2R_PARA_R0_SET_C_CF_RMY(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32), (val))

#define L2_Y2R_PARA_R1_SET_C_CF_RMV(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32), (val))

#define L2_Y2R_PARA_G0_SET_C_CF_GMU(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32), (val))
#define L2_Y2R_PARA_G0_SET_C_CF_GMY(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32), (val))

#define L2_Y2R_PARA_G1_SET_C_CF_GMV(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32), (val))

#define L2_Y2R_PARA_B0_SET_C_CF_BMU(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32), (val))
#define L2_Y2R_PARA_B0_SET_C_CF_BMY(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32), (val))

#define L2_Y2R_PARA_B1_SET_C_CF_BMV(reg32, val)                REG_FLD_SET(L2_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32), (val))

#define L2_Y2R_PARA_YUV_A_0_SET_C_CF_UA(reg32, val)            REG_FLD_SET(L2_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32), (val))
#define L2_Y2R_PARA_YUV_A_0_SET_C_CF_YA(reg32, val)            REG_FLD_SET(L2_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32), (val))

#define L2_Y2R_PARA_YUV_A_1_SET_C_CF_VA(reg32, val)            REG_FLD_SET(L2_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32), (val))

#define L2_Y2R_PARA_RGB_A_0_SET_C_CF_GA(reg32, val)            REG_FLD_SET(L2_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32), (val))
#define L2_Y2R_PARA_RGB_A_0_SET_C_CF_RA(reg32, val)            REG_FLD_SET(L2_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32), (val))

#define L2_Y2R_PARA_RGB_A_1_SET_C_CF_BA(reg32, val)            REG_FLD_SET(L2_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32), (val))

#define L3_Y2R_PARA_R0_SET_C_CF_RMU(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_R0_FLD_C_CF_RMU, (reg32), (val))
#define L3_Y2R_PARA_R0_SET_C_CF_RMY(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_R0_FLD_C_CF_RMY, (reg32), (val))

#define L3_Y2R_PARA_R1_SET_C_CF_RMV(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_R1_FLD_C_CF_RMV, (reg32), (val))

#define L3_Y2R_PARA_G0_SET_C_CF_GMU(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_G0_FLD_C_CF_GMU, (reg32), (val))
#define L3_Y2R_PARA_G0_SET_C_CF_GMY(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_G0_FLD_C_CF_GMY, (reg32), (val))

#define L3_Y2R_PARA_G1_SET_C_CF_GMV(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_G1_FLD_C_CF_GMV, (reg32), (val))

#define L3_Y2R_PARA_B0_SET_C_CF_BMU(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_B0_FLD_C_CF_BMU, (reg32), (val))
#define L3_Y2R_PARA_B0_SET_C_CF_BMY(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_B0_FLD_C_CF_BMY, (reg32), (val))

#define L3_Y2R_PARA_B1_SET_C_CF_BMV(reg32, val)                REG_FLD_SET(L3_Y2R_PARA_B1_FLD_C_CF_BMV, (reg32), (val))

#define L3_Y2R_PARA_YUV_A_0_SET_C_CF_UA(reg32, val)            REG_FLD_SET(L3_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (reg32), (val))
#define L3_Y2R_PARA_YUV_A_0_SET_C_CF_YA(reg32, val)            REG_FLD_SET(L3_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (reg32), (val))

#define L3_Y2R_PARA_YUV_A_1_SET_C_CF_VA(reg32, val)            REG_FLD_SET(L3_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (reg32), (val))

#define L3_Y2R_PARA_RGB_A_0_SET_C_CF_GA(reg32, val)            REG_FLD_SET(L3_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (reg32), (val))
#define L3_Y2R_PARA_RGB_A_0_SET_C_CF_RA(reg32, val)            REG_FLD_SET(L3_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (reg32), (val))

#define L3_Y2R_PARA_RGB_A_1_SET_C_CF_BA(reg32, val)            REG_FLD_SET(L3_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (reg32), (val))

#define FLOW_CTRL_DBG_SET_FLOW_DBG(reg32, val)                 REG_FLD_SET(FLOW_CTRL_DBG_FLD_FLOW_DBG, (reg32), (val))

#define ADDCON_DBG_SET_ROI_Y(reg32, val)                       REG_FLD_SET(ADDCON_DBG_FLD_ROI_Y, (reg32), (val))
#define ADDCON_DBG_SET_ROI_X(reg32, val)                       REG_FLD_SET(ADDCON_DBG_FLD_ROI_X, (reg32), (val))

#define OUTMUX_DBG_SET_OUT_DATA(reg32, val)                    REG_FLD_SET(OUTMUX_DBG_FLD_OUT_DATA, (reg32), (val))
#define OUTMUX_DBG_SET_OUT_VALID(reg32, val)                   REG_FLD_SET(OUTMUX_DBG_FLD_OUT_VALID, (reg32), (val))
#define OUTMUX_DBG_SET_OUT_READY(reg32, val)                   REG_FLD_SET(OUTMUX_DBG_FLD_OUT_READY, (reg32), (val))

#define RDMA0_DBG_SET_CUR_Y0(reg32, val)                       REG_FLD_SET(RDMA0_DBG_FLD_CUR_Y0, (reg32), (val))
#define RDMA0_DBG_SET_CUR_X0(reg32, val)                       REG_FLD_SET(RDMA0_DBG_FLD_CUR_X0, (reg32), (val))

#define RDMA1_DBG_SET_CUR_Y1(reg32, val)                       REG_FLD_SET(RDMA1_DBG_FLD_CUR_Y1, (reg32), (val))
#define RDMA1_DBG_SET_CUR_X1(reg32, val)                       REG_FLD_SET(RDMA1_DBG_FLD_CUR_X1, (reg32), (val))

#define RDMA2_DBG_SET_CUR_Y2(reg32, val)                       REG_FLD_SET(RDMA2_DBG_FLD_CUR_Y2, (reg32), (val))
#define RDMA2_DBG_SET_CUR_X2(reg32, val)                       REG_FLD_SET(RDMA2_DBG_FLD_CUR_X2, (reg32), (val))

#define RDMA3_DBG_SET_CUR_Y3(reg32, val)                       REG_FLD_SET(RDMA3_DBG_FLD_CUR_Y3, (reg32), (val))
#define RDMA3_DBG_SET_CUR_X3(reg32, val)                       REG_FLD_SET(RDMA3_DBG_FLD_CUR_X3, (reg32), (val))

#define STA_VAL_RDMA3_RST_PERIOD(val)                          REG_FLD_VAL(STA_FLD_RDMA3_RST_PERIOD, (val))
#define STA_VAL_RDMA2_RST_PERIOD(val)                          REG_FLD_VAL(STA_FLD_RDMA2_RST_PERIOD, (val))
#define STA_VAL_RDMA1_RST_PERIOD(val)                          REG_FLD_VAL(STA_FLD_RDMA1_RST_PERIOD, (val))
#define STA_VAL_RDMA0_RST_PERIOD(val)                          REG_FLD_VAL(STA_FLD_RDMA0_RST_PERIOD, (val))
#define STA_VAL_OVL_RUN(val)                                   REG_FLD_VAL(STA_FLD_OVL_RUN, (val))

#define INTEN_VAL_RDMA3_FIFO_UNDERFLOW_INTEN(val)              REG_FLD_VAL(INTEN_FLD_RDMA3_FIFO_UNDERFLOW_INTEN, (val))
#define INTEN_VAL_RDMA2_FIFO_UNDERFLOW_INTEN(val)              REG_FLD_VAL(INTEN_FLD_RDMA2_FIFO_UNDERFLOW_INTEN, (val))
#define INTEN_VAL_RDMA1_FIFO_UNDERFLOW_INTEN(val)              REG_FLD_VAL(INTEN_FLD_RDMA1_FIFO_UNDERFLOW_INTEN, (val))
#define INTEN_VAL_RDMA0_FIFO_UNDERFLOW_INTEN(val)              REG_FLD_VAL(INTEN_FLD_RDMA0_FIFO_UNDERFLOW_INTEN, (val))
#define INTEN_VAL_RDMA3_EOF_ABNORMAL_INTEN(val)                REG_FLD_VAL(INTEN_FLD_RDMA3_EOF_ABNORMAL_INTEN, (val))
#define INTEN_VAL_RDMA2_EOF_ABNORMAL_INTEN(val)                REG_FLD_VAL(INTEN_FLD_RDMA2_EOF_ABNORMAL_INTEN, (val))
#define INTEN_VAL_RDMA1_EOF_ABNORMAL_INTEN(val)                REG_FLD_VAL(INTEN_FLD_RDMA1_EOF_ABNORMAL_INTEN, (val))
#define INTEN_VAL_RDMA0_EOF_ABNORMAL_INTEN(val)                REG_FLD_VAL(INTEN_FLD_RDMA0_EOF_ABNORMAL_INTEN, (val))
#define INTEN_VAL_OVL_FME_SWRST_DONE_INTEN(val)                REG_FLD_VAL(INTEN_FLD_OVL_FME_SWRST_DONE_INTEN, (val))
#define INTEN_VAL_OVL_FME_UND_INTEN(val)                       REG_FLD_VAL(INTEN_FLD_OVL_FME_UND_INTEN, (val))
#define INTEN_VAL_OVL_FME_CPL_INTEN(val)                       REG_FLD_VAL(INTEN_FLD_OVL_FME_CPL_INTEN, (val))
#define INTEN_VAL_OVL_REG_CMT_INTEN(val)                       REG_FLD_VAL(INTEN_FLD_OVL_REG_CMT_INTEN, (val))

#define INTSTA_VAL_RDMA3_FIFO_UNDERFLOW_INTSTA(val)            REG_FLD_VAL(INTSTA_FLD_RDMA3_FIFO_UNDERFLOW_INTSTA, (val))
#define INTSTA_VAL_RDMA2_FIFO_UNDERFLOW_INTSTA(val)            REG_FLD_VAL(INTSTA_FLD_RDMA2_FIFO_UNDERFLOW_INTSTA, (val))
#define INTSTA_VAL_RDMA1_FIFO_UNDERFLOW_INTSTA(val)            REG_FLD_VAL(INTSTA_FLD_RDMA1_FIFO_UNDERFLOW_INTSTA, (val))
#define INTSTA_VAL_RDMA0_FIFO_UNDERFLOW_INTSTA(val)            REG_FLD_VAL(INTSTA_FLD_RDMA0_FIFO_UNDERFLOW_INTSTA, (val))
#define INTSTA_VAL_RDMA3_EOF_ABNORMAL_INTSTA(val)              REG_FLD_VAL(INTSTA_FLD_RDMA3_EOF_ABNORMAL_INTSTA, (val))
#define INTSTA_VAL_RDMA2_EOF_ABNORMAL_INTSTA(val)              REG_FLD_VAL(INTSTA_FLD_RDMA2_EOF_ABNORMAL_INTSTA, (val))
#define INTSTA_VAL_RDMA1_EOF_ABNORMAL_INTSTA(val)              REG_FLD_VAL(INTSTA_FLD_RDMA1_EOF_ABNORMAL_INTSTA, (val))
#define INTSTA_VAL_RDMA0_EOF_ABNORMAL_INTSTA(val)              REG_FLD_VAL(INTSTA_FLD_RDMA0_EOF_ABNORMAL_INTSTA, (val))
#define INTSTA_VAL_OVL_FME_SWRST_DONE_INTSTA(val)              REG_FLD_VAL(INTSTA_FLD_OVL_FME_SWRST_DONE_INTSTA, (val))
#define INTSTA_VAL_OVL_FME_UND_INTSTA(val)                     REG_FLD_VAL(INTSTA_FLD_OVL_FME_UND_INTSTA, (val))
#define INTSTA_VAL_OVL_FME_CPL_INTSTA(val)                     REG_FLD_VAL(INTSTA_FLD_OVL_FME_CPL_INTSTA, (val))
#define INTSTA_VAL_OVL_REG_CMT_INTSTA(val)                     REG_FLD_VAL(INTSTA_FLD_OVL_REG_CMT_INTSTA, (val))

#define EN_VAL_OVL_EN(val)                                     REG_FLD_VAL(EN_FLD_OVL_EN, (val))

#define TRIG_VAL_OVL_SW_TRIG(val)                              REG_FLD_VAL(TRIG_FLD_OVL_SW_TRIG, (val))

#define RST_VAL_OVL_RSTB(val)                                  REG_FLD_VAL(RST_FLD_OVL_RSTB, (val))

#define ROI_SIZE_VAL_ROI_H(val)                                REG_FLD_VAL(ROI_SIZE_FLD_ROI_H, (val))
#define ROI_SIZE_VAL_ROI_W(val)                                REG_FLD_VAL(ROI_SIZE_FLD_ROI_W, (val))

#define ROI_BGCLR_VAL_ALPHA(val)                               REG_FLD_VAL(ROI_BGCLR_FLD_ALPHA, (val))
#define ROI_BGCLR_VAL_RED(val)                                 REG_FLD_VAL(ROI_BGCLR_FLD_RED, (val))
#define ROI_BGCLR_VAL_GREEN(val)                               REG_FLD_VAL(ROI_BGCLR_FLD_GREEN, (val))
#define ROI_BGCLR_VAL_BLUE(val)                                REG_FLD_VAL(ROI_BGCLR_FLD_BLUE, (val))

#define SRC_CON_VAL_L3_EN(val)                                 REG_FLD_VAL(SRC_CON_FLD_L3_EN, (val))
#define SRC_CON_VAL_L2_EN(val)                                 REG_FLD_VAL(SRC_CON_FLD_L2_EN, (val))
#define SRC_CON_VAL_L1_EN(val)                                 REG_FLD_VAL(SRC_CON_FLD_L1_EN, (val))
#define SRC_CON_VAL_L0_EN(val)                                 REG_FLD_VAL(SRC_CON_FLD_L0_EN, (val))

#define L0_CON_VAL_DSTKEY_EN(val)                              REG_FLD_VAL(L0_CON_FLD_DSTKEY_EN, (val))
#define L0_CON_VAL_SRCKEY_EN(val)                              REG_FLD_VAL(L0_CON_FLD_SRCKEY_EN, (val))
#define L0_CON_VAL_LAYER_SRC(val)                              REG_FLD_VAL(L0_CON_FLD_LAYER_SRC, (val))
#define L0_CON_VAL_RGB_SWAP(val)                               REG_FLD_VAL(L0_CON_FLD_RGB_SWAP, (val))
#define L0_CON_VAL_BYTE_SWAP(val)                              REG_FLD_VAL(L0_CON_FLD_BYTE_SWAP, (val))
#define L0_CON_VAL_R_FIRST(val)                                REG_FLD_VAL(L0_CON_FLD_R_FIRST, (val))
#define L0_CON_VAL_LANDSCAPE(val)                              REG_FLD_VAL(L0_CON_FLD_LANDSCAPE, (val))
#define L0_CON_VAL_EN_3D(val)                                  REG_FLD_VAL(L0_CON_FLD_EN_3D, (val))
#define L0_CON_VAL_C_CF_SEL(val)                               REG_FLD_VAL(L0_CON_FLD_C_CF_SEL, (val))
#define L0_CON_VAL_CLRFMT(val)                                 REG_FLD_VAL(L0_CON_FLD_CLRFMT, (val))
#define L0_CON_VAL_ALPHA_EN(val)                               REG_FLD_VAL(L0_CON_FLD_ALPHA_EN, (val))
#define L0_CON_VAL_ALPHA(val)                                  REG_FLD_VAL(L0_CON_FLD_ALPHA, (val))

#define L0_SRCKEY_VAL_SRCKEY(val)                              REG_FLD_VAL(L0_SRCKEY_FLD_SRCKEY, (val))

#define L0_SRC_SIZE_VAL_L0_SRC_H(val)                          REG_FLD_VAL(L0_SRC_SIZE_FLD_L0_SRC_H, (val))
#define L0_SRC_SIZE_VAL_L0_SRC_W(val)                          REG_FLD_VAL(L0_SRC_SIZE_FLD_L0_SRC_W, (val))

#define L0_OFFSET_VAL_L0_YOFF(val)                             REG_FLD_VAL(L0_OFFSET_FLD_L0_YOFF, (val))
#define L0_OFFSET_VAL_L0_XOFF(val)                             REG_FLD_VAL(L0_OFFSET_FLD_L0_XOFF, (val))

#define L0_ADDR_VAL_L0_ADDR(val)                               REG_FLD_VAL(L0_ADDR_FLD_L0_ADDR, (val))

#define L0_PITCH_VAL_L0_SRC_PITCH(val)                         REG_FLD_VAL(L0_PITCH_FLD_L0_SRC_PITCH, (val))

#define L1_CON_VAL_DSTKEY_EN(val)                              REG_FLD_VAL(L1_CON_FLD_DSTKEY_EN, (val))
#define L1_CON_VAL_SRCKEY_EN(val)                              REG_FLD_VAL(L1_CON_FLD_SRCKEY_EN, (val))
#define L1_CON_VAL_LAYER_SRC(val)                              REG_FLD_VAL(L1_CON_FLD_LAYER_SRC, (val))
#define L1_CON_VAL_RGB_SWAP(val)                               REG_FLD_VAL(L1_CON_FLD_RGB_SWAP, (val))
#define L1_CON_VAL_BYTE_SWAP(val)                              REG_FLD_VAL(L1_CON_FLD_BYTE_SWAP, (val))
#define L1_CON_VAL_R_FIRST(val)                                REG_FLD_VAL(L1_CON_FLD_R_FIRST, (val))
#define L1_CON_VAL_LANDSCAPE(val)                              REG_FLD_VAL(L1_CON_FLD_LANDSCAPE, (val))
#define L1_CON_VAL_EN_3D(val)                                  REG_FLD_VAL(L1_CON_FLD_EN_3D, (val))
#define L1_CON_VAL_C_CF_SEL(val)                               REG_FLD_VAL(L1_CON_FLD_C_CF_SEL, (val))
#define L1_CON_VAL_CLRFMT(val)                                 REG_FLD_VAL(L1_CON_FLD_CLRFMT, (val))
#define L1_CON_VAL_ALPHA_EN(val)                               REG_FLD_VAL(L1_CON_FLD_ALPHA_EN, (val))
#define L1_CON_VAL_ALPHA(val)                                  REG_FLD_VAL(L1_CON_FLD_ALPHA, (val))

#define L1_SRCKEY_VAL_SRCKEY(val)                              REG_FLD_VAL(L1_SRCKEY_FLD_SRCKEY, (val))

#define L1_SRC_SIZE_VAL_L1_SRC_H(val)                          REG_FLD_VAL(L1_SRC_SIZE_FLD_L1_SRC_H, (val))
#define L1_SRC_SIZE_VAL_L1_SRC_W(val)                          REG_FLD_VAL(L1_SRC_SIZE_FLD_L1_SRC_W, (val))

#define L1_OFFSET_VAL_L1_YOFF(val)                             REG_FLD_VAL(L1_OFFSET_FLD_L1_YOFF, (val))
#define L1_OFFSET_VAL_L1_XOFF(val)                             REG_FLD_VAL(L1_OFFSET_FLD_L1_XOFF, (val))

#define L1_ADDR_VAL_L1_ADDR(val)                               REG_FLD_VAL(L1_ADDR_FLD_L1_ADDR, (val))

#define L1_PITCH_VAL_L1_SRC_PITCH(val)                         REG_FLD_VAL(L1_PITCH_FLD_L1_SRC_PITCH, (val))

#define L2_CON_VAL_DSTKEY_EN(val)                              REG_FLD_VAL(L2_CON_FLD_DSTKEY_EN, (val))
#define L2_CON_VAL_SRCKEY_EN(val)                              REG_FLD_VAL(L2_CON_FLD_SRCKEY_EN, (val))
#define L2_CON_VAL_LAYER_SRC(val)                              REG_FLD_VAL(L2_CON_FLD_LAYER_SRC, (val))
#define L2_CON_VAL_RGB_SWAP(val)                               REG_FLD_VAL(L2_CON_FLD_RGB_SWAP, (val))
#define L2_CON_VAL_BYTE_SWAP(val)                              REG_FLD_VAL(L2_CON_FLD_BYTE_SWAP, (val))
#define L2_CON_VAL_R_FIRST(val)                                REG_FLD_VAL(L2_CON_FLD_R_FIRST, (val))
#define L2_CON_VAL_LANDSCAPE(val)                              REG_FLD_VAL(L2_CON_FLD_LANDSCAPE, (val))
#define L2_CON_VAL_EN_3D(val)                                  REG_FLD_VAL(L2_CON_FLD_EN_3D, (val))
#define L2_CON_VAL_C_CF_SEL(val)                               REG_FLD_VAL(L2_CON_FLD_C_CF_SEL, (val))
#define L2_CON_VAL_CLRFMT(val)                                 REG_FLD_VAL(L2_CON_FLD_CLRFMT, (val))
#define L2_CON_VAL_ALPHA_EN(val)                               REG_FLD_VAL(L2_CON_FLD_ALPHA_EN, (val))
#define L2_CON_VAL_ALPHA(val)                                  REG_FLD_VAL(L2_CON_FLD_ALPHA, (val))

#define L2_SRCKEY_VAL_SRCKEY(val)                              REG_FLD_VAL(L2_SRCKEY_FLD_SRCKEY, (val))

#define L2_SRC_SIZE_VAL_L2_SRC_H(val)                          REG_FLD_VAL(L2_SRC_SIZE_FLD_L2_SRC_H, (val))
#define L2_SRC_SIZE_VAL_L2_SRC_W(val)                          REG_FLD_VAL(L2_SRC_SIZE_FLD_L2_SRC_W, (val))

#define L2_OFFSET_VAL_L2_YOFF(val)                             REG_FLD_VAL(L2_OFFSET_FLD_L2_YOFF, (val))
#define L2_OFFSET_VAL_L2_XOFF(val)                             REG_FLD_VAL(L2_OFFSET_FLD_L2_XOFF, (val))

#define L2_ADDR_VAL_L2_ADDR(val)                               REG_FLD_VAL(L2_ADDR_FLD_L2_ADDR, (val))

#define L2_PITCH_VAL_L2_SRC_PITCH(val)                         REG_FLD_VAL(L2_PITCH_FLD_L2_SRC_PITCH, (val))

#define L3_CON_VAL_DSTKEY_EN(val)                              REG_FLD_VAL(L3_CON_FLD_DSTKEY_EN, (val))
#define L3_CON_VAL_SRCKEY_EN(val)                              REG_FLD_VAL(L3_CON_FLD_SRCKEY_EN, (val))
#define L3_CON_VAL_LAYER_SRC(val)                              REG_FLD_VAL(L3_CON_FLD_LAYER_SRC, (val))
#define L3_CON_VAL_RGB_SWAP(val)                               REG_FLD_VAL(L3_CON_FLD_RGB_SWAP, (val))
#define L3_CON_VAL_BYTE_SWAP(val)                              REG_FLD_VAL(L3_CON_FLD_BYTE_SWAP, (val))
#define L3_CON_VAL_R_FIRST(val)                                REG_FLD_VAL(L3_CON_FLD_R_FIRST, (val))
#define L3_CON_VAL_LANDSCAPE(val)                              REG_FLD_VAL(L3_CON_FLD_LANDSCAPE, (val))
#define L3_CON_VAL_EN_3D(val)                                  REG_FLD_VAL(L3_CON_FLD_EN_3D, (val))
#define L3_CON_VAL_C_CF_SEL(val)                               REG_FLD_VAL(L3_CON_FLD_C_CF_SEL, (val))
#define L3_CON_VAL_CLRFMT(val)                                 REG_FLD_VAL(L3_CON_FLD_CLRFMT, (val))
#define L3_CON_VAL_ALPHA_EN(val)                               REG_FLD_VAL(L3_CON_FLD_ALPHA_EN, (val))
#define L3_CON_VAL_ALPHA(val)                                  REG_FLD_VAL(L3_CON_FLD_ALPHA, (val))

#define L3_SRCKEY_VAL_SRCKEY(val)                              REG_FLD_VAL(L3_SRCKEY_FLD_SRCKEY, (val))

#define L3_SRC_SIZE_VAL_L3_SRC_H(val)                          REG_FLD_VAL(L3_SRC_SIZE_FLD_L3_SRC_H, (val))
#define L3_SRC_SIZE_VAL_L3_SRC_W(val)                          REG_FLD_VAL(L3_SRC_SIZE_FLD_L3_SRC_W, (val))

#define L3_OFFSET_VAL_L3_YOFF(val)                             REG_FLD_VAL(L3_OFFSET_FLD_L3_YOFF, (val))
#define L3_OFFSET_VAL_L3_XOFF(val)                             REG_FLD_VAL(L3_OFFSET_FLD_L3_XOFF, (val))

#define L3_ADDR_VAL_L3_ADDR(val)                               REG_FLD_VAL(L3_ADDR_FLD_L3_ADDR, (val))

#define L3_PITCH_VAL_L3_SRC_PITCH(val)                         REG_FLD_VAL(L3_PITCH_FLD_L3_SRC_PITCH, (val))

#define RDMA0_CTRL_VAL_RDMA0_TRIG_TYPE(val)                    REG_FLD_VAL(RDMA0_CTRL_FLD_RDMA0_TRIG_TYPE, (val))
#define RDMA0_CTRL_VAL_RDMA0_EN(val)                           REG_FLD_VAL(RDMA0_CTRL_FLD_RDMA0_EN, (val))

#define RDMA0_MEM_START_TRIG_VAL_RDMA0_START_TRIG(val)         REG_FLD_VAL(RDMA0_MEM_START_TRIG_FLD_RDMA0_START_TRIG, (val))

#define RDMA0_MEM_GMC_SETTING_VAL_RDMA0_DISEN_THRD(val)        REG_FLD_VAL(RDMA0_MEM_GMC_SETTING_FLD_RDMA0_DISEN_THRD, (val))
#define RDMA0_MEM_GMC_SETTING_VAL_RDMA0_EN_THRD(val)           REG_FLD_VAL(RDMA0_MEM_GMC_SETTING_FLD_RDMA0_EN_THRD, (val))

#define RDMA0_MEM_SLOW_CON_VAL_RDMA0_SLOW_CNT(val)             REG_FLD_VAL(RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_CNT, (val))
#define RDMA0_MEM_SLOW_CON_VAL_RDMA0_SLOW_EN(val)              REG_FLD_VAL(RDMA0_MEM_SLOW_CON_FLD_RDMA0_SLOW_EN, (val))

#define RDMA0_FIFO_CTRL_VAL_RDMA0_FIFO_UND_EN(val)             REG_FLD_VAL(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_UND_EN, (val))
#define RDMA0_FIFO_CTRL_VAL_RDMA0_FIFO_SIZE(val)               REG_FLD_VAL(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_SIZE, (val))
#define RDMA0_FIFO_CTRL_VAL_RDMA0_FIFO_THRD(val)               REG_FLD_VAL(RDMA0_FIFO_CTRL_FLD_RDMA0_FIFO_THRD, (val))

#define RDMA1_CTRL_VAL_RDMA1_TRIG_TYPE(val)                    REG_FLD_VAL(RDMA1_CTRL_FLD_RDMA1_TRIG_TYPE, (val))
#define RDMA1_CTRL_VAL_RDMA1_EN(val)                           REG_FLD_VAL(RDMA1_CTRL_FLD_RDMA1_EN, (val))

#define RDMA1_MEM_START_TRIG_VAL_RDMA1_START_TRIG(val)         REG_FLD_VAL(RDMA1_MEM_START_TRIG_FLD_RDMA1_START_TRIG, (val))

#define RDMA1_MEM_GMC_SETTING_VAL_RDMA1_DISEN_THRD(val)        REG_FLD_VAL(RDMA1_MEM_GMC_SETTING_FLD_RDMA1_DISEN_THRD, (val))
#define RDMA1_MEM_GMC_SETTING_VAL_RDMA1_EN_THRD(val)           REG_FLD_VAL(RDMA1_MEM_GMC_SETTING_FLD_RDMA1_EN_THRD, (val))

#define RDMA1_MEM_SLOW_CON_VAL_RDMA1_SLOW_CNT(val)             REG_FLD_VAL(RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_CNT, (val))
#define RDMA1_MEM_SLOW_CON_VAL_RDMA1_SLOW_EN(val)              REG_FLD_VAL(RDMA1_MEM_SLOW_CON_FLD_RDMA1_SLOW_EN, (val))

#define RDMA1_FIFO_CTRL_VAL_RDMA1_FIFO_UND_EN(val)             REG_FLD_VAL(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_UND_EN, (val))
#define RDMA1_FIFO_CTRL_VAL_RDMA1_FIFO_SIZE(val)               REG_FLD_VAL(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_SIZE, (val))
#define RDMA1_FIFO_CTRL_VAL_RDMA1_FIFO_THRD(val)               REG_FLD_VAL(RDMA1_FIFO_CTRL_FLD_RDMA1_FIFO_THRD, (val))

#define RDMA2_CTRL_VAL_RDMA2_TRIG_TYPE(val)                    REG_FLD_VAL(RDMA2_CTRL_FLD_RDMA2_TRIG_TYPE, (val))
#define RDMA2_CTRL_VAL_RDMA2_EN(val)                           REG_FLD_VAL(RDMA2_CTRL_FLD_RDMA2_EN, (val))

#define RDMA2_MEM_START_TRIG_VAL_RDMA2_START_TRIG(val)         REG_FLD_VAL(RDMA2_MEM_START_TRIG_FLD_RDMA2_START_TRIG, (val))

#define RDMA2_MEM_GMC_SETTING_VAL_RDMA2_DISEN_THRD(val)        REG_FLD_VAL(RDMA2_MEM_GMC_SETTING_FLD_RDMA2_DISEN_THRD, (val))
#define RDMA2_MEM_GMC_SETTING_VAL_RDMA2_EN_THRD(val)           REG_FLD_VAL(RDMA2_MEM_GMC_SETTING_FLD_RDMA2_EN_THRD, (val))

#define RDMA2_MEM_SLOW_CON_VAL_RDMA2_SLOW_CNT(val)             REG_FLD_VAL(RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_CNT, (val))
#define RDMA2_MEM_SLOW_CON_VAL_RDMA2_SLOW_EN(val)              REG_FLD_VAL(RDMA2_MEM_SLOW_CON_FLD_RDMA2_SLOW_EN, (val))

#define RDMA2_FIFO_CTRL_VAL_RDMA2_FIFO_UND_EN(val)             REG_FLD_VAL(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_UND_EN, (val))
#define RDMA2_FIFO_CTRL_VAL_RDMA2_FIFO_SIZE(val)               REG_FLD_VAL(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_SIZE, (val))
#define RDMA2_FIFO_CTRL_VAL_RDMA2_FIFO_THRD(val)               REG_FLD_VAL(RDMA2_FIFO_CTRL_FLD_RDMA2_FIFO_THRD, (val))

#define RDMA3_CTRL_VAL_RDMA3_TRIG_TYPE(val)                    REG_FLD_VAL(RDMA3_CTRL_FLD_RDMA3_TRIG_TYPE, (val))
#define RDMA3_CTRL_VAL_RDMA3_EN(val)                           REG_FLD_VAL(RDMA3_CTRL_FLD_RDMA3_EN, (val))

#define RDMA3_MEM_START_TRIG_VAL_RDMA3_START_TRIG(val)         REG_FLD_VAL(RDMA3_MEM_START_TRIG_FLD_RDMA3_START_TRIG, (val))

#define RDMA3_MEM_GMC_SETTING_VAL_RDMA3_DISEN_THRD(val)        REG_FLD_VAL(RDMA3_MEM_GMC_SETTING_FLD_RDMA3_DISEN_THRD, (val))
#define RDMA3_MEM_GMC_SETTING_VAL_RDMA3_EN_THRD(val)           REG_FLD_VAL(RDMA3_MEM_GMC_SETTING_FLD_RDMA3_EN_THRD, (val))

#define RDMA3_MEM_SLOW_CON_VAL_RDMA3_SLOW_CNT(val)             REG_FLD_VAL(RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_CNT, (val))
#define RDMA3_MEM_SLOW_CON_VAL_RDMA3_SLOW_EN(val)              REG_FLD_VAL(RDMA3_MEM_SLOW_CON_FLD_RDMA3_SLOW_EN, (val))

#define RDMA3_FIFO_CTRL_VAL_RDMA3_FIFO_UND_EN(val)             REG_FLD_VAL(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_UND_EN, (val))
#define RDMA3_FIFO_CTRL_VAL_RDMA3_FIFO_SIZE(val)               REG_FLD_VAL(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_SIZE, (val))
#define RDMA3_FIFO_CTRL_VAL_RDMA3_FIFO_THRD(val)               REG_FLD_VAL(RDMA3_FIFO_CTRL_FLD_RDMA3_FIFO_THRD, (val))

#define L0_Y2R_PARA_R0_VAL_C_CF_RMU(val)                       REG_FLD_VAL(L0_Y2R_PARA_R0_FLD_C_CF_RMU, (val))
#define L0_Y2R_PARA_R0_VAL_C_CF_RMY(val)                       REG_FLD_VAL(L0_Y2R_PARA_R0_FLD_C_CF_RMY, (val))

#define L0_Y2R_PARA_R1_VAL_C_CF_RMV(val)                       REG_FLD_VAL(L0_Y2R_PARA_R1_FLD_C_CF_RMV, (val))

#define L0_Y2R_PARA_G0_VAL_C_CF_GMU(val)                       REG_FLD_VAL(L0_Y2R_PARA_G0_FLD_C_CF_GMU, (val))
#define L0_Y2R_PARA_G0_VAL_C_CF_GMY(val)                       REG_FLD_VAL(L0_Y2R_PARA_G0_FLD_C_CF_GMY, (val))

#define L0_Y2R_PARA_G1_VAL_C_CF_GMV(val)                       REG_FLD_VAL(L0_Y2R_PARA_G1_FLD_C_CF_GMV, (val))

#define L0_Y2R_PARA_B0_VAL_C_CF_BMU(val)                       REG_FLD_VAL(L0_Y2R_PARA_B0_FLD_C_CF_BMU, (val))
#define L0_Y2R_PARA_B0_VAL_C_CF_BMY(val)                       REG_FLD_VAL(L0_Y2R_PARA_B0_FLD_C_CF_BMY, (val))

#define L0_Y2R_PARA_B1_VAL_C_CF_BMV(val)                       REG_FLD_VAL(L0_Y2R_PARA_B1_FLD_C_CF_BMV, (val))

#define L0_Y2R_PARA_YUV_A_0_VAL_C_CF_UA(val)                   REG_FLD_VAL(L0_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (val))
#define L0_Y2R_PARA_YUV_A_0_VAL_C_CF_YA(val)                   REG_FLD_VAL(L0_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (val))

#define L0_Y2R_PARA_YUV_A_1_VAL_C_CF_VA(val)                   REG_FLD_VAL(L0_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (val))

#define L0_Y2R_PARA_RGB_A_0_VAL_C_CF_GA(val)                   REG_FLD_VAL(L0_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (val))
#define L0_Y2R_PARA_RGB_A_0_VAL_C_CF_RA(val)                   REG_FLD_VAL(L0_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (val))

#define L0_Y2R_PARA_RGB_A_1_VAL_C_CF_BA(val)                   REG_FLD_VAL(L0_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (val))

#define L1_Y2R_PARA_R0_VAL_C_CF_RMU(val)                       REG_FLD_VAL(L1_Y2R_PARA_R0_FLD_C_CF_RMU, (val))
#define L1_Y2R_PARA_R0_VAL_C_CF_RMY(val)                       REG_FLD_VAL(L1_Y2R_PARA_R0_FLD_C_CF_RMY, (val))

#define L1_Y2R_PARA_R1_VAL_C_CF_RMV(val)                       REG_FLD_VAL(L1_Y2R_PARA_R1_FLD_C_CF_RMV, (val))

#define L1_Y2R_PARA_G0_VAL_C_CF_GMU(val)                       REG_FLD_VAL(L1_Y2R_PARA_G0_FLD_C_CF_GMU, (val))
#define L1_Y2R_PARA_G0_VAL_C_CF_GMY(val)                       REG_FLD_VAL(L1_Y2R_PARA_G0_FLD_C_CF_GMY, (val))

#define L1_Y2R_PARA_G1_VAL_C_CF_GMV(val)                       REG_FLD_VAL(L1_Y2R_PARA_G1_FLD_C_CF_GMV, (val))

#define L1_Y2R_PARA_B0_VAL_C_CF_BMU(val)                       REG_FLD_VAL(L1_Y2R_PARA_B0_FLD_C_CF_BMU, (val))
#define L1_Y2R_PARA_B0_VAL_C_CF_BMY(val)                       REG_FLD_VAL(L1_Y2R_PARA_B0_FLD_C_CF_BMY, (val))

#define L1_Y2R_PARA_B1_VAL_C_CF_BMV(val)                       REG_FLD_VAL(L1_Y2R_PARA_B1_FLD_C_CF_BMV, (val))

#define L1_Y2R_PARA_YUV_A_0_VAL_C_CF_UA(val)                   REG_FLD_VAL(L1_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (val))
#define L1_Y2R_PARA_YUV_A_0_VAL_C_CF_YA(val)                   REG_FLD_VAL(L1_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (val))

#define L1_Y2R_PARA_YUV_A_1_VAL_C_CF_VA(val)                   REG_FLD_VAL(L1_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (val))

#define L1_Y2R_PARA_RGB_A_0_VAL_C_CF_GA(val)                   REG_FLD_VAL(L1_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (val))
#define L1_Y2R_PARA_RGB_A_0_VAL_C_CF_RA(val)                   REG_FLD_VAL(L1_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (val))

#define L1_Y2R_PARA_RGB_A_1_VAL_C_CF_BA(val)                   REG_FLD_VAL(L1_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (val))

#define L2_Y2R_PARA_R0_VAL_C_CF_RMU(val)                       REG_FLD_VAL(L2_Y2R_PARA_R0_FLD_C_CF_RMU, (val))
#define L2_Y2R_PARA_R0_VAL_C_CF_RMY(val)                       REG_FLD_VAL(L2_Y2R_PARA_R0_FLD_C_CF_RMY, (val))

#define L2_Y2R_PARA_R1_VAL_C_CF_RMV(val)                       REG_FLD_VAL(L2_Y2R_PARA_R1_FLD_C_CF_RMV, (val))

#define L2_Y2R_PARA_G0_VAL_C_CF_GMU(val)                       REG_FLD_VAL(L2_Y2R_PARA_G0_FLD_C_CF_GMU, (val))
#define L2_Y2R_PARA_G0_VAL_C_CF_GMY(val)                       REG_FLD_VAL(L2_Y2R_PARA_G0_FLD_C_CF_GMY, (val))

#define L2_Y2R_PARA_G1_VAL_C_CF_GMV(val)                       REG_FLD_VAL(L2_Y2R_PARA_G1_FLD_C_CF_GMV, (val))

#define L2_Y2R_PARA_B0_VAL_C_CF_BMU(val)                       REG_FLD_VAL(L2_Y2R_PARA_B0_FLD_C_CF_BMU, (val))
#define L2_Y2R_PARA_B0_VAL_C_CF_BMY(val)                       REG_FLD_VAL(L2_Y2R_PARA_B0_FLD_C_CF_BMY, (val))

#define L2_Y2R_PARA_B1_VAL_C_CF_BMV(val)                       REG_FLD_VAL(L2_Y2R_PARA_B1_FLD_C_CF_BMV, (val))

#define L2_Y2R_PARA_YUV_A_0_VAL_C_CF_UA(val)                   REG_FLD_VAL(L2_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (val))
#define L2_Y2R_PARA_YUV_A_0_VAL_C_CF_YA(val)                   REG_FLD_VAL(L2_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (val))

#define L2_Y2R_PARA_YUV_A_1_VAL_C_CF_VA(val)                   REG_FLD_VAL(L2_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (val))

#define L2_Y2R_PARA_RGB_A_0_VAL_C_CF_GA(val)                   REG_FLD_VAL(L2_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (val))
#define L2_Y2R_PARA_RGB_A_0_VAL_C_CF_RA(val)                   REG_FLD_VAL(L2_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (val))

#define L2_Y2R_PARA_RGB_A_1_VAL_C_CF_BA(val)                   REG_FLD_VAL(L2_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (val))

#define L3_Y2R_PARA_R0_VAL_C_CF_RMU(val)                       REG_FLD_VAL(L3_Y2R_PARA_R0_FLD_C_CF_RMU, (val))
#define L3_Y2R_PARA_R0_VAL_C_CF_RMY(val)                       REG_FLD_VAL(L3_Y2R_PARA_R0_FLD_C_CF_RMY, (val))

#define L3_Y2R_PARA_R1_VAL_C_CF_RMV(val)                       REG_FLD_VAL(L3_Y2R_PARA_R1_FLD_C_CF_RMV, (val))

#define L3_Y2R_PARA_G0_VAL_C_CF_GMU(val)                       REG_FLD_VAL(L3_Y2R_PARA_G0_FLD_C_CF_GMU, (val))
#define L3_Y2R_PARA_G0_VAL_C_CF_GMY(val)                       REG_FLD_VAL(L3_Y2R_PARA_G0_FLD_C_CF_GMY, (val))

#define L3_Y2R_PARA_G1_VAL_C_CF_GMV(val)                       REG_FLD_VAL(L3_Y2R_PARA_G1_FLD_C_CF_GMV, (val))

#define L3_Y2R_PARA_B0_VAL_C_CF_BMU(val)                       REG_FLD_VAL(L3_Y2R_PARA_B0_FLD_C_CF_BMU, (val))
#define L3_Y2R_PARA_B0_VAL_C_CF_BMY(val)                       REG_FLD_VAL(L3_Y2R_PARA_B0_FLD_C_CF_BMY, (val))

#define L3_Y2R_PARA_B1_VAL_C_CF_BMV(val)                       REG_FLD_VAL(L3_Y2R_PARA_B1_FLD_C_CF_BMV, (val))

#define L3_Y2R_PARA_YUV_A_0_VAL_C_CF_UA(val)                   REG_FLD_VAL(L3_Y2R_PARA_YUV_A_0_FLD_C_CF_UA, (val))
#define L3_Y2R_PARA_YUV_A_0_VAL_C_CF_YA(val)                   REG_FLD_VAL(L3_Y2R_PARA_YUV_A_0_FLD_C_CF_YA, (val))

#define L3_Y2R_PARA_YUV_A_1_VAL_C_CF_VA(val)                   REG_FLD_VAL(L3_Y2R_PARA_YUV_A_1_FLD_C_CF_VA, (val))

#define L3_Y2R_PARA_RGB_A_0_VAL_C_CF_GA(val)                   REG_FLD_VAL(L3_Y2R_PARA_RGB_A_0_FLD_C_CF_GA, (val))
#define L3_Y2R_PARA_RGB_A_0_VAL_C_CF_RA(val)                   REG_FLD_VAL(L3_Y2R_PARA_RGB_A_0_FLD_C_CF_RA, (val))

#define L3_Y2R_PARA_RGB_A_1_VAL_C_CF_BA(val)                   REG_FLD_VAL(L3_Y2R_PARA_RGB_A_1_FLD_C_CF_BA, (val))

#define FLOW_CTRL_DBG_VAL_FLOW_DBG(val)                        REG_FLD_VAL(FLOW_CTRL_DBG_FLD_FLOW_DBG, (val))

#define ADDCON_DBG_VAL_ROI_Y(val)                              REG_FLD_VAL(ADDCON_DBG_FLD_ROI_Y, (val))
#define ADDCON_DBG_VAL_ROI_X(val)                              REG_FLD_VAL(ADDCON_DBG_FLD_ROI_X, (val))

#define OUTMUX_DBG_VAL_OUT_DATA(val)                           REG_FLD_VAL(OUTMUX_DBG_FLD_OUT_DATA, (val))
#define OUTMUX_DBG_VAL_OUT_VALID(val)                          REG_FLD_VAL(OUTMUX_DBG_FLD_OUT_VALID, (val))
#define OUTMUX_DBG_VAL_OUT_READY(val)                          REG_FLD_VAL(OUTMUX_DBG_FLD_OUT_READY, (val))

#define RDMA0_DBG_VAL_CUR_Y0(val)                              REG_FLD_VAL(RDMA0_DBG_FLD_CUR_Y0, (val))
#define RDMA0_DBG_VAL_CUR_X0(val)                              REG_FLD_VAL(RDMA0_DBG_FLD_CUR_X0, (val))

#define RDMA1_DBG_VAL_CUR_Y1(val)                              REG_FLD_VAL(RDMA1_DBG_FLD_CUR_Y1, (val))
#define RDMA1_DBG_VAL_CUR_X1(val)                              REG_FLD_VAL(RDMA1_DBG_FLD_CUR_X1, (val))

#define RDMA2_DBG_VAL_CUR_Y2(val)                              REG_FLD_VAL(RDMA2_DBG_FLD_CUR_Y2, (val))
#define RDMA2_DBG_VAL_CUR_X2(val)                              REG_FLD_VAL(RDMA2_DBG_FLD_CUR_X2, (val))

#define RDMA3_DBG_VAL_CUR_Y3(val)                              REG_FLD_VAL(RDMA3_DBG_FLD_CUR_Y3, (val))
#define RDMA3_DBG_VAL_CUR_X3(val)                              REG_FLD_VAL(RDMA3_DBG_FLD_CUR_X3, (val))



#endif
