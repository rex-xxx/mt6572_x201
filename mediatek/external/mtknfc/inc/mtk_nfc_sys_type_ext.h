/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
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

/******************************************************************************
*[File] mtk_nfc_sys_type_ext.h
*[Version] v1.0
*[Revision Date] 2012-05-31
*[Author] LiangChi Huang, LiangChi.Huang@mediatek.com, 25609
*[Description]
*[Copyright]
*    Copyright (C) 2008 MediaTek Incorporation. All Rights Reserved.
******************************************************************************/

#ifndef MTK_NFC_TYPE_EXT_H
#define MTK_NFC_TYPE_EXT_H


#define SUPPORT_EM
#define SUPPORT_FM

/* MESSAGE TYPE*/
typedef enum{
    /* FOR DEMO TOOL*/
    MTK_NFC_CHIP_CONNECT_REQ = 0,
    MTK_NFC_CHIP_CONNECT_RSP,
    MTK_NFC_GET_SELIST_REQ,
    MTK_NFC_GET_SELIST_RSP,
    MTK_NFC_DISCOVERY_REQ,
    MTK_NFC_DISCOVERY_RSP,
    MTK_NFC_DISCOVERY_NTF,
    MTK_NFC_TAG_READ_REQ,
    MTK_NFC_TAG_READ_RSP,
    MTK_NFC_TAG_WRITE_REQ,
    MTK_NFC_TAG_WRITE_RSP,
    MTK_NFC_TAG_DISCONNECT_REQ,
    MTK_NFC_TAG_DISCONNECT_RSP,
    MTK_NFC_TAG_FORMAT_REQ,
    MTK_NFC_TAG_FORMAT_RSP,
    MTK_NFC_P2P_START_REQ,
    MTK_NFC_P2P_START_RSP,
    MTK_NFC_P2P_CONN_REQ,
    MTK_NFC_P2P_RECEV_RSP,
    MTK_NFC_P2P_DISC_REQ,
    MTK_NFC_P2P_DISC_RSP, //20
    //MTK_NFC_SE_SET_MODE_REQ,
    //MTK_NFC_SE_SET_MODE_RSP,
    MTK_NFC_SE_NTF,
    MTK_NFC_MULTIPLE_TAG_NTF,
    MTK_NFC_MULTIPLE_TAG_SELECT_REQ,
    MTK_NFC_MULTIPLE_TAG_SELECT_RSP,
    MTK_NFC_STOP_REQ,
    MTK_NFC_STOP_RSP,

    MTK_NFC_P2P_CHK_LLCP_REQ,
    MTK_NFC_P2P_CHK_LLCP_RSP,
    MTK_NFC_P2P_LINK_REQ,
    MTK_NFC_P2P_ACTIVATE_REQ,
    MTK_NFC_P2P_ACTIVATE_RSP,
    MTK_NFC_P2P_LINK_NTF,    
    MTK_NFC_P2P_CREATE_SERVER_REQ,
    MTK_NFC_P2P_CREATE_SERVER_RSP,    
    MTK_NFC_P2P_CREATE_SERVER_NTF,
    MTK_NFC_P2P_ACCEPT_SERVER_REQ,
    MTK_NFC_P2P_ACCEPT_SERVER_RSP,    
    MTK_NFC_P2P_CREATE_CLIENT_REQ,
    MTK_NFC_P2P_CREATE_CLIENT_RSP,     
    MTK_NFC_P2P_CONNECT_CLIENT_REQ,
    MTK_NFC_P2P_CONNECT_CLIENT_RSP,
    MTK_NFC_P2P_CONNECTION_NTF,
    MTK_NFC_P2P_SOCKET_STATUS_NTF,
    MTK_NFC_P2P_GET_REM_SETTING_REQ,
    MTK_NFC_P2P_GET_REM_SETTING_RSP,
    MTK_NFC_P2P_SEND_DATA_REQ,
    MTK_NFC_P2P_SEND_DATA_RSP,
    MTK_NFC_P2P_RECV_DATA_REQ,
    MTK_NFC_P2P_RECV_DATA_RSP,    

    /* FOR EM*/
    #ifdef SUPPORT_EM
    MTK_NFC_EM_START_CMD = 100,
    MTK_NFC_EM_ALS_READER_MODE_REQ,
    MTK_NFC_EM_ALS_READER_MODE_RSP,
    MTK_NFC_EM_ALS_READER_MODE_OPT_REQ,
    MTK_NFC_EM_ALS_READER_MODE_OPT_RSP,
    MTK_NFC_EM_ALS_P2P_MODE_REQ,
    MTK_NFC_EM_ALS_P2P_MODE_RSP,
    MTK_NFC_EM_ALS_CARD_MODE_REQ,
    MTK_NFC_EM_ALS_CARD_MODE_RSP,
    MTK_NFC_EM_POLLING_MODE_REQ,
    MTK_NFC_EM_POLLING_MODE_RSP,
    MTK_NFC_EM_TX_CARRIER_ALS_ON_REQ,
    MTK_NFC_EM_TX_CARRIER_ALS_ON_RSP,
    MTK_NFC_EM_VIRTUAL_CARD_REQ,
    MTK_NFC_EM_VIRTUAL_CARD_RSP,
    MTK_NFC_EM_PNFC_CMD_REQ,
    MTK_NFC_EM_PNFC_CMD_RSP,
    MTK_NFC_EM_POLLING_MODE_NTF,
    MTK_NFC_EM_ALS_READER_MODE_NTF,
    MTK_NFC_EM_ALS_P2P_MODE_NTF,
    MTK_NFC_EM_STOP_CMD,
    MTK_NFC_EM_POLLING_MODE_UPT,
    MTK_NFC_EM_READER_MODE_UPT,
    MTK_NFC_EM_ALS_CARD_MODE_PROC,
    MTK_NFC_EM_VIRTUAL_CARD_PROC,
    MTK_NFC_EM_TX_CARRIER_ALS_ON_PROC,
    MTK_NFC_EM_PNFC_CMD_PROC,
    #endif
    /* FOR EM*/
    #ifdef SUPPORT_FM
    MTK_NFC_FM_SETART_CMD = 200,
    MTK_NFC_FM_SWP_TEST_REQ,
    MTK_NFC_FM_SWP_TEST_NTF,
    MTK_NFC_FM_SWP_TEST_RSP,
    MTK_NFC_FM_READ_UID_TEST_REQ,
    MTK_NFC_FM_READ_UID_TEST_RSP,
    MTK_NFC_FM_READ_DEP_TEST_REQ,
    MTK_NFC_FM_READ_DEP_TEST_RSP,
    MTK_NFC_FM_CARD_MODE_TEST_REQ,
    MTK_NFC_FM_CARD_MODE_TEST_RSP,
    MTK_NFC_FM_PNFC_CMD_PROC,
    MTK_NFC_FM_STOP_CMD,
    #endif

    #if 1//test by hiki
    MTK_NFC_TEST_HAL_OPEN = 900,
    MTK_NFC_TEST_HAL_CLOSE,
    MTK_NFC_TEST_HAL_SELECT,
    MTK_NFC_TEST_HAL_DEACTIVATE,
    MTK_NFC_TEST_HAL_GET_CONFIG,
    MTK_NFC_TEST_TIMER_ONESHOT,
    MTK_NFC_TEST_TIMER_PERIODICAL,
    MTK_NFC_TEST_T2T_DATA_EXCHANGE,
    MTK_NFC_TEST_LOOPBACK_CONN_CREATE,
    MTK_NFC_TEST_LOOPBACK_CONN_SEND,     
    MTK_NFC_TEST_LOOPBACK_CONN_CLOSE,
    MTK_NFC_TEST_PNFC,
    #endif   
    
    MTK_NFC_MESSAGE_TYPE_ENUM_END
} MTK_NFC_MESSAGE_TYPE;


/*ENUM OF COMPORT_TYPE*/
typedef enum{
   CONN_TYPE_UART=0,
   CONN_TYPE_I2C,
   CONN_TYPE_SPI,
   CONN_TYPE_ENUM_END
}CONN_TYPE;


/* ENUM OF TRANSMISSION_BITS_RATE*/
typedef enum{
   TRANSMISSION_BR_106 = 0,
   TRANSMISSION_BR_212,
   TRANSMISSION_BR_424,
   TRANSMISSION_BR_ALL,
   TRANSMISSION_BITS_RATE_ENUM_END    
}TRANSMISSION_BITS_RATE;

/* ENUM OF SE_TYPE*/
typedef enum{
   SE_OFF = 0,
   SE_CONTACTLESS,
   SE_HOST_ACCESS,
   SE_ALL,
   SE_CONNECT_ENUM_END    
}SE_CONNECT_TYPE;

/* ENUM OF SE_PBF*/
typedef enum{
   DISABLE = 0,
   ENABLE,
   SE_STATUS_ENUM_END    
}SE_STATUS;

typedef enum{
    UICC =0,
    EMBEDDED_SE,
    uSD_CARD,
    SE_TYPE_ENUM_END
}SE_TYPE;

/* ENUM OF DETECTION TYPE*/
typedef enum{
   DT_READ_MODE = 0,
   DT_P2P_MODE,
   DT_CARD_MODE,
   DT_ENUM_END 
}DETECTION_TYPE;

/*ENUM OF TAG_LIFECYCLE*/
typedef enum{
	TAG_LC_INITIALIZE = 0,
	TAG_LC_READWRITE,
	TAG_LC_READONLY,
	TAG_LC_INVAILD,
	TAG_LC_ENUM_END,
} TAG_LIFECYCLE;

/*ENUM OF NDEF_FORMAT*/
typedef enum{
	TAG_NDEF_FORMAT = 0,
	TAG_NONNDEF_FORMAT,
	TAG_NDEF_ENUM_END,
} e_TAG_NDEF_FORMAT;

/*ENUM OF TAG_INFOTYPE*/
typedef enum{
    TAG_INFOTYPE1 = 0,
    TAG_INFOTYPE2,
    TAG_INFOTYPE3,
    TAG_INFOTYPE4,
    TAG_INFOTYPEV,
    TAG_INFOTYPEK,
    TAG_INFOTYPEBP,
    TAG_INFOTYPE_UNKNOWN,
    TAG_INFO_ENUM_END
} TAG_INFOTYPE;

/*ENUM OF P2P_CONNECT_TYPE*/
typedef enum{
   P2P_CONNECT_TYPE_DEFAULT = 0,
   P2P_CONNECT_TYPE_CONNECTLESS,
   P2P_CONNECT_TYPE_CONNECTORIENT
}P2P_CONNECT_TYPE;


/*ENUM OF P2P_MODE*/
typedef enum{
   P2P_MODE_ACTIVE = 0,
   P2P_MODE_PASSIVE,
   P2P_MODE_ENUM_END
}P2P_MODE;


/* Tag TYPE */
typedef enum mtk_nfc_tag_type{
    nfc_tag_DEFAULT    = 0,
    nfc_tag_MIFARE_UL  = 1,
    nfc_tag_MIFARE_STD = 2,
    nfc_tag_ISO1443_4A = 3,
    nfc_tag_ISO1443_4B = 4,
    nfc_tag_JEWEL      = 5,
    nfc_tag_NFC        = 6, //P2P mode
    nfc_tag_FELICA     = 7,
    nfc_tag_ISO15693   = 8,
    nfc_NDEF           = 9,
    nfc_raw_cmd        = 10
}e_mtk_nfc_tag_type;

typedef enum mtk_nfc_p2p_role{
  nfc_p2p_Initiator   =0x00U,
  nfc_p2p_Target      =0x01U,           
  nfc_p2p_All         =0x02U
}e_mtk_nfc_p2p_role;

typedef enum mtk_nfc_p2p_type{
  nfc_p2p_DefaultP2PMode  =0x00U,
  nfc_p2p_Passive106      =0x01U, // A          
  nfc_p2p_Passive212      =0x02U, // F            
  nfc_p2p_Passive424      =0x04U, // F          
  nfc_p2p_Active          =0x08U,                 
  nfc_p2p_P2P_ALL         =0x0FU,                
  nfc_p2p_InvalidP2PMode  =0xFFU         
}e_mtk_nfc_p2p_type; 

typedef enum mtk_nfc_ndef_lang_type{
    nfc_ndef_lang_DEFAULT = 0,
    nfc_ndef_lang_DE = 1,
    nfc_ndef_lang_EN = 2,
    nfc_ndef_lang_FR = 3
}e_mtk_nfc_ndef_lang_type;

typedef enum mtk_nfc_ndef_type{
   nfc_ndef_type_uri = 0,
   nfc_ndef_type_text,
   nfc_ndef_type_sp,
   nfc_ndef_type_others,
}e_mtk_nfc_ndef_type;


#define TAG_UID_MAX_LEN (10)

/* BITMAP OF NFC_TECHNOLOGY*/
#define TYPE_A  (1 << 0) 
#define TYPE_B  (1 << 1)
#define TYPE_V  (1 << 2)
#define TYPE_F  (1 << 3) 

/* BITMAP OF NFC_PROTOCOL*/
#define PROTOCOL_ISO14443A  (1 << 0) 
#define PROTOCOL_ISO14443B  (1 << 1)
#define PROTOCOL_ISO15693   (1 << 2)
#define PROTOCOL_FELICA212  (1 << 3) 
#define PROTOCOL_FELICA424  (1 << 4) 
#define PROTOCOL_JEWEL      (1 << 5) 
#define PROTOCOL_KOVIO      (1 << 6) 
#define PROTOCOL_BP         (1 << 7) 

/* BITMAP OF SECURE ELEMENT*/
#define SE_SIM1  (1 << 0) 
#define SE_SIM2  (1 << 1)
#define SE_SIM3  (1 << 2)

/* BITMAP OF P2P_ROLE*/
#define P2P_ROLE_INITIATOR (1<<0)
#define P2P_ROLE_TARGET    (1<<1)


//#define MTK_NFC_SUCCESS   (0)
//#define MTK_NFC_FAIL      (1)

#define TAG_WRITE_MAXDATA (512)



#define MIFARE4K_LEN        (16)
#define MIFARE1K_LEN        (4)
#define ISO15693_LEN        (4)

#define NDEF_DATA_LEN       (512)
#define MTK_DEMO_TOOL_SE_NUM (3)

#ifdef SUPPORT_EM

/* BITMAP OF EM_ALS_READER_M_TYPE*/
#define EM_ALS_READER_M_TYPE_A        (1 << 0) 
#define EM_ALS_READER_M_TYPE_B        (1 << 1)
#define EM_ALS_READER_M_TYPE_F        (1 << 2)
#define EM_ALS_READER_M_TYPE_V        (1 << 3) 
#define EM_ALS_READER_M_TYPE_BPrime   (1 << 4) 
#define EM_ALS_READER_M_TYPE_KOVIO    (1 << 5) 

/* BITMAP OF EM_ALS_CARD_M_TYPE*/
#define EM_ALS_CARD_M_TYPE_A        (1 << 0) 
#define EM_ALS_CARD_M_TYPE_B        (1 << 1)
#define EM_ALS_CARD_M_TYPE_BPrime   (1 << 2)
#define EM_ALS_CARD_M_TYPE_F212     (1 << 3) 
#define EM_ALS_CARD_M_TYPE_F424     (1 << 4)


/* BITMAP OF EM_ALS_READER_M_SPDRATE*/
#define EM_ALS_READER_M_SPDRATE_106        (1 << 0) 
#define EM_ALS_READER_M_SPDRATE_212        (1 << 1)
#define EM_ALS_READER_M_SPDRATE_424        (1 << 2)
#define EM_ALS_READER_M_SPDRATE_848        (1 << 3) 
#define EM_ALS_READER_M_SPDRATE_662        (1 << 4) 
#define EM_ALS_READER_M_SPDRATE_2648       (1 << 5) 

/* BITMAP OF POLLING MODE STATE MACHINE */
#define EM_POLLING_STATE_IDLE                   (0)
#define EM_POLLING_STATE_WAIT_INIT_DONE         (1)
#define EM_POLLING_STATE_INIT_DONE              (2)
#define EM_POLLING_STATE_WAIT_DISCOVER_RESP     (3)
#define EM_POLLING_STATE_GET_DISCOVER_RESP      (4)
#define EM_POLLING_STATE_WAIT_DEVICE_FOUND      (5)
#define EM_POLLING_STATE_DEVICE_FOUND           (6)
#define EM_POLLING_STATE_NDEF_CHECK_DONE        (7)
#define EM_POLLING_STATE_NOTIFY_DEV_INFO        (8)
#define EM_POLLING_STATE_ERROR                  (9)
#define EM_POLLING_STATE_WAIT_DEINIT_DONE       (0xFE)
#define EM_POLLING_STATE_DEINIT_DONE            (0xFF)

/* BITMAP OF EM_ALS_CARD_M_SW_NUM*/
#define EM_ALS_CARD_M_SW_NUM_SWIO1        (1 << 0) 
#define EM_ALS_CARD_M_SW_NUM_SWIO2        (1 << 1)
#define EM_ALS_CARD_M_SW_NUM_SWIOSE       (1 << 2)

/* BITMAP OF EM_ENABLE_FUNC*/
#define EM_ENABLE_FUNC_READER_MODE        (1 << 0) 
#define EM_ENABLE_FUNC_CARD_MODE          (1 << 1)
#define EM_ENABLE_FUNC_P2P_MODE           (1 << 2)

/* BITMAP OF P2P CONNECTION*/
#define MTK_NFC_P2P_CONNECTION_DEFAULT          (0)
#define MTK_NFC_P2P_CONNECTION_ACTIVE           (1)
#define MTK_NFC_P2P_CONNECTION_CREATE_SERVICE   (2)
#define MTK_NFC_P2P_CONNECTION_CREATE_CLIENT    (3)
#define MTK_NFC_P2P_CONNECTION_LISTENED         (4)
#define MTK_NFC_P2P_CONNECTION_ACCEPTED         (5)
#define MTK_NFC_P2P_CONNECTION_CONNECTED        (6)
#define MTK_NFC_P2P_CONNECTION_SEND             (7)
#define MTK_NFC_P2P_CONNECTION_RECEIVE          (8)

/* ENUM OF EM_ACTION*/
typedef enum{
   NFC_EM_ACT_START = 0,
   NFC_EM_ACT_STOP,
   NFC_EM_ACT_RUNINGB,   
}EM_ACTION;

/* ENUM OF EM_ACTION*/
typedef enum{
   NFC_EM_OPT_ACT_READ = 0,
   NFC_EM_OPT_ACT_WRITE,
   NFC_EM_OPT_ACT_FORMAT, 
   NFC_EM_OPT_ACT_WRITE_RAW
}EM_OPT_ACTION;

#endif

#pragma pack(1)
/* NFC Main structure*/
typedef struct mtk_nfc_main_msg_struct {
  unsigned int    msg_type;           /* message identifier */
  unsigned int    msg_length;        /* length of 'data' */
} s_mtk_nfc_main_msg;

/* NFC_CONN_TYPE_UART*/
typedef struct nfc_conn_type_uart {
  unsigned int    comport;
  unsigned int    buardrate;
} s_nfc_conn_type_uart;

/* NFC_CONN_TYPE_I2C*/
typedef struct nfc_conn_type_i2c {
  unsigned int    comport;
} s_nfc_conn_type_i2c;

/* NFC_CONN_TYPE_SPI*/
typedef struct nfc_conn_type_spi {
  unsigned int    comport;
} s_nfc_conn_type_spi;

   typedef union {
      s_nfc_conn_type_uart type_uart;
      s_nfc_conn_type_i2c type_i2c;
      s_nfc_conn_type_spi type_spi;
   } connid_u; 

/*Connect structure*/
typedef struct mtk_nfc_dev_conn_req{
   unsigned int isconnect; /* 1 : do connect, 0: do disconnect*/	
   unsigned int conntype; /*Please refer enum of CONN_TYPE*/     
   connid_u connid;
   unsigned int dumpdata; /* 1: do dump debug data, 0 : don't dump debug data*/
}s_mtk_nfc_dev_conn_req;

typedef struct mtk_nfc_dev_conn_rsp{
   unsigned int result;     /* return connect result, 0: SUCCESS, 1 : FAIL*/	
   unsigned int status;     /* return connect status, 1: connect, 0 : disconnect*/
   unsigned int isdumpdata; /* 1: enable debug data dump function, 0: disable*/
   unsigned int sw_ver;                      /* return software version*/ 
   unsigned int chipID;                     /* return chip id*/
   unsigned int fw_ver;                       /* return firmware version*/
   unsigned int bm_technology; /*support techonlogy, please refer enum of NFC_TECHNOLOGY*/
   unsigned int bm_protocol;   /*support protocol, please refer enum of NFC_PROTOCOL*/
}s_mtk_nfc_dev_conn_rsp;


typedef struct mtk_nfc_tag_listing{
    unsigned int status;      /*1 : enable tag listing function, 0: disable*/
    unsigned int bm_protocol; /*bitmaps, please refer PROTOCOL related define*/
    unsigned int checkndef;   /*1: enable check NDEF function, 0 disable*/
    unsigned int autopolling;   /*1: enable auto polling tag remove function, 0 disable*/
    unsigned int supportmultupletag;   /*1: enable support multuple tag function, 0 disable*/
    
}s_mtk_nfc_tag_listing;

typedef struct mtk_nfc_tag_speed_designate{
    unsigned int ISO_14443A;/* please refer ENUM OF TRANSMISSION_BITS_RATE*/
    unsigned int ISO_14443B;/* please refer ENUM OF TRANSMISSION_BITS_RATE*/
    unsigned int ISO_15693;/* 0 for 6Kbits/s, 1 : for 26Kbits/s, 3 : for no limit*/
}s_mtk_nfc_tag_speed_designate;

typedef struct mtk_nfc_p2p{
	 unsigned int status; /*1 : enable P2P function, 0: disable*/
         unsigned int role;   /* 0: initiator, 1: target, 2: all  */
	 unsigned int bm_mode; /* device mode, please refer BITMAP of P2P_MODE*/	
}s_mtk_nfc_p2p;

/*Securacy element*/
typedef struct mtk_nfc_tool_se_info {
   //unsigned int      seid;
   SE_STATUS         status;        /* 1: enable, 0 : disable*/
   SE_TYPE           type; 
   SE_CONNECT_TYPE   connecttype;
   SE_STATUS         lowpowermode;  /* 1: enable low power mode, 0 : disable*/
   SE_STATUS         pbf;           /* Each SE current status of Power by field*/     
}s_mtk_nfc_tool_se_info ;

typedef struct s_mtk_nfc_tool_set_se{
   unsigned int status;  /*1 : enable se function, 0: disable*/
   unsigned int senum;  /*total number of se */
   s_mtk_nfc_tool_se_info SE[MTK_DEMO_TOOL_SE_NUM];
   unsigned int notifyapp; /* 1: enable , 0 : disable*/
   unsigned int clf; /* 1: enable , 0 : disable*/    
}s_mtk_nfc_tool_set_se;

typedef struct mtk_nfc_get_selist_req{
   unsigned int status; /* 1: enable se function, 0 : disable se function*/
}s_mtk_nfc_get_selist_req;

typedef struct mtk_nfc_get_selist_rsp{
   unsigned int result; /* return get se result, 0: SUCCESS, 1 : FAIL*/
   unsigned int status; /* return se status, 0: have not SE, 1 : have SE*/
   unsigned int senum;  /*total number of se */
   s_mtk_nfc_tool_set_se sedata; 
}s_mtk_nfc_get_selist_rsp;


/*Discovery structure*/
typedef struct mtk_nfc_discovery_req{
   unsigned int action;                /*  discovery action, 1: start discovery, 0 : stop discovery*/
   s_mtk_nfc_tag_listing  tag_setting; /* Tag related setting, please refer structure of "s_mtk_tag_listing"*/ 
   s_mtk_nfc_tool_set_se  se_setting;  /* Secure element related setting, please refer structure of "s_mtk_secureEle"*/ 
   s_mtk_nfc_p2p          p2p_setting; /* p2p related setting, please refer structure of "s_mtk_p2p"*/    
}s_mtk_nfc_discovery_req;

typedef struct mtk_nfc_discovery_rsp{
   unsigned int result;     /* return discovery result, 0: SUCCESS, 1 : FAIL*/	
   unsigned int status;     /* return discovery status, 1: start discovery, 0 : stop discovery*/
}s_mtk_nfc_discovery_rsp;


/* TAG RYPE 1 INFORMATION Setucture*/
typedef struct cardinfo_type1{
    unsigned int cardtype;
    unsigned int uid_length;        /* uid length*/
    unsigned char uid[TAG_UID_MAX_LEN];
    unsigned int sak;
    unsigned int atqa;
    unsigned int appdata;
    unsigned int maxdatarate;
}s_cardinfo_type1;

/* TAG RYPE 2 INFORMATION Setucture*/
typedef struct cardinfo_type2{
    unsigned int cardtype;
    unsigned int pupi;
    unsigned int afi;
    unsigned int atqb;
    unsigned int appdata;
    unsigned int maxdatarate;
}s_cardinfo_type2;

/* TAG RYPE 2 INFORMATION Setucture*/
typedef struct cardinfo_type3{
    unsigned int cardtype;
    unsigned int uid_length;        /* uid length*/
    unsigned char uid[TAG_UID_MAX_LEN];
    unsigned int dsfid;
    unsigned int flags;
    unsigned int afi;
}s_cardinfo_type3;

/* TAG RYPE 2 INFORMATION Setucture*/
typedef struct cardinfo_type4{
    unsigned int cardtype;
    unsigned int uid_length;        /* uid length*/
    unsigned char uid[TAG_UID_MAX_LEN];
    unsigned int dsfid;
    unsigned int flags;
    unsigned int afi;
}s_cardinfo_type4;

/* TAG RYPE 2 INFORMATION Setucture*/
typedef struct cardinfo_typev{
    unsigned int cardtype;
    unsigned int uid_length;        /* uid length*/
    unsigned char uid[TAG_UID_MAX_LEN];
    unsigned int dsfid;
    unsigned int flags;
    unsigned int afi;
}s_cardinfo_typev;

/* TAG RYPE 2 INFORMATION Setucture*/
typedef struct cardinfo_typek{
    unsigned int cardtype;
    unsigned int uid_length;        /* uid length*/
    unsigned char uid[TAG_UID_MAX_LEN];
    unsigned int dsfid;
    unsigned int flags;
    unsigned int afi;
}s_cardinfo_typek;

/* TAG RYPE 2 INFORMATION Setucture*/
typedef struct cardinfo_typebp{
    unsigned int cardtype;
    unsigned int uid_length;        /* uid length*/
    unsigned char uid[TAG_UID_MAX_LEN];
    unsigned int dsfid;
    unsigned int flags;
    unsigned int afi;
}s_cardinfo_typebp;

typedef union cardinfo{
    s_cardinfo_type1 type1;
    s_cardinfo_type2 type2;
    s_cardinfo_type3 type3;
    s_cardinfo_type4 type4;
    s_cardinfo_typev typev;
    s_cardinfo_typek typek;
    s_cardinfo_typebp typebp;
}u_cardinfo;

/* TAG INFO structure*/
typedef struct mtk_nfc_tag_info{
    unsigned int lifecycle;     /* please refer ENUM OF TAG_LIFECYCLE*/
    unsigned int isndef;        /*please refer ENUM OF TAG_NDEF_FORMAT*/
    unsigned int bmprotocol;    /*please refer BITMAP OF NFC_PROTOCOL*/
    unsigned int cardinfotype;  /* please refer ENUM OF TAG_INFOTYPE*/   
     
    u_cardinfo cardinfo;
   
}s_mtk_nfc_tag_info;


/* P2P INFO Setucture*/
typedef struct mtk_nfc_p2p_info{
    unsigned int    p2p_role;               /* please refer BITMAP OF P2P_ROLE*/
    unsigned int    p2p_mode;               /* please refer ENUM OF P2P_MODE*/
    unsigned int    p2p_speed;               /* please refer ENUM OF TRANSMISSION_BITS_RATE*/
}s_mtk_nfc_p2p_info;


  typedef union{
       s_mtk_nfc_tag_info taginfo; /* TAG Information*/
       s_mtk_nfc_p2p_info p2pinfo; /* P2P Information*/
       //s_mtk_nfc_se_info  seinfo /* SE Information*/   	
   }u_discovery_type;

/*Discovery detect response*/
typedef struct mtk_nfc_discovery_detect_rsp{
   unsigned int detectiontype; /* please refer ENUM OF DETECTION TYPE*/
   
   u_discovery_type discovery_type;
}s_mtk_nfc_discovery_detect_rsp;




#if 1
/*NFC Tag Read Request*/
//nfc_tag_read_request
typedef struct mtk_nfc_tag_read_request{
  e_mtk_nfc_tag_type    read_type;              /*  which type want to read*/            
  unsigned int    sector;                 /* for Mifare STD used*/
  unsigned int    block;                  /* for Mifare STD/MifareUL/ISO15693 used*/
  unsigned char   AuthentificationKey;    /* for Mifare STD, KEY_A:0 , KEY_B:1 */
} s_mtk_nfc_tag_read_req;

typedef struct mtk_nfc_tag_read_Mifare {
  unsigned int    Length;
  unsigned char   data[MIFARE4K_LEN];    
} s_mtk_nfc_tag_read_Mifare;

typedef struct mtk_nfc_tag_read_ndef {
  e_mtk_nfc_ndef_type  ndef_type;
  unsigned  char lang[3];
  unsigned  char recordFlags;
  unsigned  char recordId[32];
  unsigned  char recordTnf;
  unsigned  int  length;      
  unsigned  char data[NDEF_DATA_LEN];
} s_mtk_nfc_tag_read_ndef;

typedef struct mtk_nfc_tag_read_ISO15693{
  unsigned int  Length;
  unsigned char data[ISO15693_LEN];
} s_mtk_nfc_tag_read_ISO15693;


typedef struct mtk_nfc_tag_write_rawdata{
   unsigned int length;
   unsigned char data[TAG_WRITE_MAXDATA]; //TAG_WRITE_MAXDATA = 512  
}s_mtk_nfc_tag_write_rawdata,s_mtk_nfc_tag_read_rawdata;

typedef union mtk_nfc_tag_read_result_response_u{
   s_mtk_nfc_tag_read_Mifare nfc_tag_read_Mifare_resp;
   s_mtk_nfc_tag_read_ndef nfc_tag_read_ndef_resp;
   s_mtk_nfc_tag_read_ISO15693 nfc_tag_read_ISO15693_resp;
   s_mtk_nfc_tag_read_rawdata      nfc_tag_read_raw_data;
}mtk_nfc_tag_read_result_response_u;


/*NFC Tag Read Response*/
//nfc_tag_read_response
typedef struct mtk_nfc_tag_read_response{
  unsigned int    status;               /*  return read status, 0 success*/
  e_mtk_nfc_tag_type    type;                 /*  Check nfc_tag_type */
  mtk_nfc_tag_read_result_response_u nfc_tag_read_result;
} s_mtk_nfc_tag_read_rsp;

/*NFC Tag Write Request*/
typedef struct mtk_nfc_tag_write_typeMifare{
  unsigned int    sector;               /* Mifare STD */
  unsigned int    block;                /* Mifare STD, Mifare UL */
  unsigned int    length;               /* Mifare STD, Mifare UL */
  unsigned char   data[MIFARE4K_LEN];   /* Mifare STD, Mifare UL */
  unsigned char   AuthentificationKey;  /* Mifare STD, Mifare UL,KEY_A:0 , KEY_B:1 */
} s_mtk_nfc_tag_write_typeMifare;

typedef struct mtk_nfc_tag_write_typeISO15693{
  unsigned int    block;
  unsigned int    length;
  unsigned char   data[ISO15693_LEN];
} s_mtk_nfc_tag_write_typeISO15693;

typedef struct Vcard
{
    char Name[64];
    char Compagny[64];
    char Titlep[64];
    char Tel[32];
    char Email[64];
    char Adress[128];
    char PostalCode[32];
    char City[64];
    char CompagnyUrl[64];
}Vcard_t;

typedef struct SmartPoster
{
    unsigned char  Compagny[64];
    unsigned short CompagnyLength;
    unsigned char CompagnyUrl[64];
    unsigned short CompagnyUrlLength;
}SmartPoster_t;

typedef struct Text
{
    unsigned char data[TAG_WRITE_MAXDATA];
    //unsigned char data[128];
    unsigned short DataLength;
}Text_t;

typedef struct URL
{
    //ndef_url_type URLtype;
    unsigned char URLData[64];
    unsigned short URLLength;
}URL_t;

typedef struct EXTTag
{
    char EXTTagType[64];
    char EXTData[TAG_WRITE_MAXDATA];
    unsigned short EXTLength;
}EXTTag_t;


typedef union mtk_nfc_tag_write_ndef_data
{
    SmartPoster_t  SP_Data;
    Vcard_t        VC_Data;
    Text_t         TX_Data;
    URL_t          URL_Data;
    EXTTag_t       EXT_Data;
}s_mtk_nfc_tag_write_ndef_data;

typedef struct mtk_nfc_tag_write_ndef{
  e_mtk_nfc_ndef_type      ndef_type;
  e_mtk_nfc_ndef_lang_type language;
  unsigned  int  length;
  s_mtk_nfc_tag_write_ndef_data ndef_data;
} s_mtk_nfc_tag_write_ndef;

typedef union mtk_nfc_tag_write_data_request_u{
  s_mtk_nfc_tag_write_typeMifare   nfc_tag_write_typeMifare_data;
  s_mtk_nfc_tag_write_typeISO15693 nfc_tag_write_typeISO15693_data;
  s_mtk_nfc_tag_write_ndef         nfc_tag_write_ndef_data;
  s_mtk_nfc_tag_write_rawdata      nfc_tag_write_raw_data;
}mtk_nfc_tag_write_data_request_u;

//nfc_tag_write_request
typedef struct mtk_nfc_tag_write_request{
  e_mtk_nfc_tag_type    write_type;               /*  which type want to write*/
  mtk_nfc_tag_write_data_request_u  nfc_tag_write_data;
} s_mtk_nfc_tag_write_req;

/*NFC Tag Write Response*/
//nfc_tag_write_response
typedef struct mtk_nfc_tag_write_response{
  e_mtk_nfc_tag_type    type;      /*  return writed type*/
  unsigned int    status;                /*  return read status, 0 success*/               
} s_mtk_nfc_tag_write_rsp;
#endif

typedef union sapvalue{
     unsigned int  value;
     unsigned char string[64]; /*TBD*/
}sapvalue_u; 

/* p2p service start*/

typedef struct mtk_nfc_p2p_service_start_req{
	P2P_CONNECT_TYPE connecttype; /* connect method, please refer ENUM OF P2P_CONNECT_TYPE*/
	unsigned int sapsettype;  /*0 is value type , 1 is string type */
	sapvalue_u   sap;
    unsigned int windoesize; /**/
	unsigned int datasize;
}s_mtk_nfc_p2p_service_start_req;

/* p2p service connect*/

typedef struct mtk_nfc_p2p_service_conn_req{
	unsigned int sapsettype;  /*0 is value type , 1 is string type */    
	sapvalue_u                    sap;
	s_mtk_nfc_tag_write_ndef  ndef_data;    
}s_mtk_nfc_p2p_service_conn_req;

/* p2p service disconnect*/

typedef struct mtk_nfc_p2p_service_disconn_req{
    	unsigned int action; /* 1: disconnection */
}s_mtk_nfc_p2p_service_disconn_req;

/* p2p command resp, read data*/

typedef struct mtk_nfc_p2p_rsp{
   unsigned int command; /* Read = 0, Write =1, Connect = 2,Start = 3*/
   unsigned int result; /* return p2p_setting result, 0: SUCCESS, 1 : FAIL*/
   s_mtk_nfc_tag_read_ndef nfc_tag_read_ndef_resp;
}s_mtk_nfc_p2p_setting_rsp;

/* SE Notify*/
typedef struct mtk_nfc_SE_notify{
    unsigned int  SEId;
	  unsigned int  trigger;
	  unsigned int  notifylen;
    unsigned char message[256];
}s_mtk_nfc_SE_notify;

/* Multiple Tag Notify*/
typedef struct mtk_nfc_MultipleTag_notify{
	  unsigned int  tagnumber;   /* tag detected number, MAX support 10 Tags*/
	  unsigned int  uidlength[10];
    unsigned char uid[10][10];
}s_mtk_nfc_MultipleTag_notify;

/* Multiple Tag Select*/
typedef struct mtk_nfc_MultipleTag_select_req{
	  unsigned int  uidlength;
	  unsigned char  data[10];
}s_mtk_nfc_MultipleTag_select_req;

typedef struct mtk_nfc_MultipleTag_select_rsp{
   unsigned int result;     /* return result, 0: SUCCESS, 1 : FAIL*/	
	 unsigned int status;     /* return status, 1: Success, 0 : fail, not support*/
}s_mtk_nfc_MultipleTag_select_rsp;


typedef struct mtk_nfc_general_rsp{
   unsigned int result;     /* return result, 0: SUCCESS, 1 : FAIL*/	
	 unsigned int status;     /* return status, 1: Success, 0 : fail, not support*/
}s_mtk_nfc_general_rsp;

typedef struct mtk_nfc_stop_rsp{
   unsigned int tupe;     /* TBD, now always set 0*/	
   unsigned int action;     /* TBD, now always set 0*/
}s_mtk_nfc_stop_rsp;






#ifdef SUPPORT_EM

/* OTHER_FUNCTION*/
/*--------------------------------------------------------*/

typedef struct mtk_nfc_em_pnfc_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
   unsigned int datalen;
   unsigned char data[256];
}s_mtk_nfc_em_pnfc_req;


typedef struct mtk_nfc_em_pnfc_rsp{
    unsigned int result;           /*0:Success,1:Fail*/
}s_mtk_nfc_em_pnfc_rsp;

typedef struct mtk_nfc_pnfc_req{   
   unsigned int            u4ReqMsg;           /*MTK_NFC_MESSAGE_TYPE*/
   unsigned int            u4action;
   s_mtk_nfc_em_pnfc_req rEmPnfcReq;
}s_mtk_nfc_pnfc_req;

typedef struct mtk_nfc_pnfc_rsq{  
   unsigned int u4ReqMsg;
   unsigned int u4action;            
   unsigned int u4result;
}s_mtk_nfc_pnfc_rsq;

typedef struct mtk_nfc_em_virtual_card_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
   unsigned int supporttype;      /* supporttype, please refer BITMAP of EM_ALS_READER_M_TYPE*/
   //unsigned int typeA_datarate;  /* TypeA,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   //unsigned int typeB_datarate;  /* TypeB,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   unsigned int typeF_datarate;  /* TypeF,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/ 
}s_mtk_nfc_em_virtual_card_req;


typedef struct mtk_nfc_em_virtual_card_rsp{    
    unsigned int result;           /*0:Success,1:Fail*/
}s_mtk_nfc_em_virtual_card_rsp;

typedef struct mtk_nfc_em_tx_carr_als_on_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
}s_mtk_nfc_em_tx_carr_als_on_req;


typedef struct mtk_nfc_em_tx_carr_als_on_rsp{
   unsigned int result;           /*0:Success,1:Fail*/
}s_mtk_nfc_em_tx_carr_als_on_rsp;
/*--------------------------------------------------------*/

/*--------------------------------------------------------*/



/*P2P_MODE_RELATED*/
/*--------------------------------------------------------*/

typedef struct mtk_nfc_em_als_p2p_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
   unsigned int supporttype;      /* supporttype, please refer BITMAP of EM_ALS_READER_M_TYPE*/
   unsigned int typeA_datarate;  /* TypeA,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   unsigned int typeF_datarate;  /* TypeV,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   unsigned int mode;             /* BITMAPS bit0: Passive mode, bit1: Active mode*/
   unsigned int role;             /* BITMAPS bit0: Initator, bit1: Target*/
   unsigned int isDisableCardM;   /* 0: , 1: disable card mode*/
}s_mtk_nfc_em_als_p2p_req;

typedef struct mtk_nfc_em_als_p2p_ntf{   
   int           link_status;           /*1:llcp link is up,0:llcp link is down*/
 //  unsigned int  datalen;
 //  unsigned char data[256];
}s_mtk_nfc_em_als_p2p_ntf;

typedef struct mtk_nfc_em_als_p2p_rsp{   
   int           result;           /*0:Success,1:Fail*/
}s_mtk_nfc_em_als_p2p_rsp;
/*--------------------------------------------------------*/




/*CARD_MODE_RELATED*/
/*--------------------------------------------------------*/

typedef struct mtk_nfc_em_als_cardm_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
   unsigned int SWNum;            /* SWNum, please refer BITMAP of EM_ALS_CARD_M_SW_NUM*/
   unsigned int supporttype;      /* supporttype, please refer BITMAP of EM_ALS_READER_M_TYPE*/
   unsigned int fgvirtualcard;    /* 1:enable virtual card, 0:disable virtual card(default)   */
}s_mtk_nfc_em_als_cardm_req;

typedef struct mtk_nfc_em_als_cardm_rsp{   
   int           result;           /*0:Success,1:Fail*/
}s_mtk_nfc_em_als_cardm_rsp;

/*--------------------------------------------------------*/




/*READER_MODE_RELATED*/
/*--------------------------------------------------------*/

typedef struct mtk_nfc_em_als_readerm_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
   unsigned int supporttype;      /* supporttype, please refer BITMAP of EM_ALS_READER_M_TYPE*/
   unsigned int typeA_datarate;  /* TypeA,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   unsigned int typeB_datarate;  /* TypeB,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   unsigned int typeV_datarate;  /* TypeV,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/
   unsigned int typeF_datarate;  /* TypeF,datarate, please refer BITMAP of EM_ALS_READER_M_SPDRATE*/    
   unsigned int typeV_subcarrier;  /* 0: subcarrier, 1 :dual subcarrier*/
}s_mtk_nfc_em_als_readerm_req;

typedef struct mtk_nfc_em_als_readerm_ntf{
   int           result;      /*0:Success,Tag connected, 1: Fail, 2:Tag disconnected*/
   unsigned int  isNDEF;      /*1:NDEF, 0: Non-NDEF*/
   unsigned int  UidLen;
   unsigned char Uid[10];
}s_mtk_nfc_em_als_readerm_ntf;

typedef struct mtk_nfc_em_als_readerm_rsp{
   int           result;      /*0:Success, 1: Fail,*/
}s_mtk_nfc_em_als_readerm_rsp;

typedef struct mtk_nfc_em_als_readerm_opt_req{
   int           action;      /*Action, please refer ENUM of EM_OPT_ACTION*/   
   s_mtk_nfc_tag_write_ndef         ndef_write;
}s_mtk_nfc_em_als_readerm_opt_req;

typedef struct mtk_nfc_em_als_readerm_opt_rsp{
   int           result;           /*0:Success,1:Fail*/
   s_mtk_nfc_tag_read_ndef         ndef_read;
}s_mtk_nfc_em_als_readerm_opt_rsp;

/*--------------------------------------------------------*/


/*POLLING_MODE_RELATED*/
/*--------------------------------------------------------*/

typedef struct mtk_nfc_em_polling_req{
   unsigned int action;           /*Action, please refer ENUM of EM_ACTION*/
   unsigned int phase;            /*0:Listen phase, 1:Pause phase*/
   unsigned int Period;
   unsigned int enablefunc;       /*enablefunc, please refer BITMAP of EM_ENABLE_FUNC*/
   s_mtk_nfc_em_als_p2p_req     p2pM;
   s_mtk_nfc_em_als_cardm_req   cardM;
   s_mtk_nfc_em_als_readerm_req readerM;
}s_mtk_nfc_em_polling_req;


typedef struct mtk_nfc_em_polling_rsp{   
   int           result;           /*0:Success,1:Fail*/
}s_mtk_nfc_em_polling_rsp;

typedef union {
   s_mtk_nfc_em_als_p2p_ntf         p2p;
   s_mtk_nfc_em_als_cardm_rsp       card;
   s_mtk_nfc_em_als_readerm_ntf reader;
} s_mtk_nfc_em_polling_func_ntf;


typedef struct mtk_nfc_em_polling_ntf{   
   int           detecttype;           /*enablefunc, please refer ENUM of EM_ENABLE_FUNC*/
   s_mtk_nfc_em_polling_func_ntf ntf;
}s_mtk_nfc_em_polling_ntf;

/*--------------------------------------------------------*/
#endif

/*--------------------------------------------------------*/
/********************FACTORY MODE*************************/
/*--------------------------------------------------------*/
#ifdef SUPPORT_FM
typedef struct mtk_nfc_fm_swp_test_req{   
   int           action;
}s_mtk_nfc_fm_swp_test_req;

typedef struct mtk_nfc_fm_swp_test_rsp{   
   int           result;
}s_mtk_nfc_fm_swp_test_rsp;

#endif
    
#pragma pack()

#endif /*MTK_NFC_DATA_EXTRACTOR_H*/

