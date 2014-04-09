/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2012
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

/*******************************************************************************
 * Filename:
 * ---------
 *  mtk_nfc_android_main.c
 *
 * Project:
 * --------
 *
 * Description:
 * ------------
 *
 * Author:
 * -------
 *  LiangChi Huang, ext 25609, liangchi.huang@mediatek.com, 2012-08-14
 * 
 *******************************************************************************/
/***************************************************************************** 
 * Include
 *****************************************************************************/ 
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/fs.h>

#include <termios.h> 
#include <pthread.h>

#include <sys/types.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <netinet/in.h>
#include <arpa/inet.h>



#include <cutils/log.h> // For Debug
#include <utils/Log.h> // For Debug

#include "mtk_nfc_sys_type.h"
//#include "mtk_nfc_sys_type.h"
//#include "mtk_nfc_dal.h"
//#include "mtk_nfc_bin.h"
//#include "mtk_nfc_hal.h"
#include "mtk_nfc_sys_type_ext.h"
#include "mtk_nfc_sys.h"


//mtkNfcHal_sHwReference_t g_HwRef;

#if 0
uint32_t g_TempCtxt = 0;
#endif
extern int gconn_fd_tmp;
extern NFCSTATUS mtk_nfc_sys_i2c_write(UINT8* data, UINT16 len);
/***************************************************************************** 
 * Define
 *****************************************************************************/
#define C_INVALID_TID  (-1)   /*invalid thread id*/
#define C_INVALID_FD   (-1)   /*invalid file handle*/
#define C_INVALID_SOCKET (-1)
#define THREAD_NUM     (3)    /*MAIN/DATA READ/SERVICE THREAD*/

//

#define DSP_UART_IN_BUFFER_SIZE (1024)
#define DEBUG_LOG


//#define SELF_TEST // for test


/***************************************************************************** 
 * Data Structure
 *****************************************************************************/

typedef enum MTK_NFC_THREAD_NUM
{
   MTK_NFC_THREAD_MAIN = 0,
   MTK_NFC_THREAD_READ,
   MTK_NFC_THREAD_SERVICE,
   MTK_NFC_THREAD_END
} MTK_NFC_THREAD_NUM_e;

typedef struct MTK_NFC_THREAD
{
    int                     snd_fd;
    MTK_NFC_THREAD_NUM_e    thread_id;
    pthread_t               thread_handle;
    //void (*thread_body)(void *arg);
    int (*thread_exit)(struct MTK_NFC_THREAD *arg);
    int (*thread_active)(struct MTK_NFC_THREAD *arg);
} MTK_NFC_THREAD_T;


/***************************************************************************** 
 * Extern Area
 *****************************************************************************/
int mtk_nfc_sys_init_android(void);
int mtk_nfc_sys_deinit_android(void);
int mtk_nfc_threads_create(int threadId);
int mtk_nfc_threads_release(void);
void *data_read_thread_func(void * arg);
void *nfc_main_proc_func(void * arg);
#ifdef SELF_TEST
void *nfc_service_thread(void * arg);
#else
void nfc_service_thread(void );
#endif
int mtk_nfc_exit_thread_normal(MTK_NFC_THREAD_T *arg);
int mtk_nfc_thread_active_notify(MTK_NFC_THREAD_T *arg);




/***************************************************************************** 
 * GLobal Variable
 *****************************************************************************/ 
static MTK_NFC_THREAD_T g_mtk_nfc_thread[THREAD_NUM] = {
{C_INVALID_FD, MTK_NFC_THREAD_MAIN, C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify},
{C_INVALID_FD, MTK_NFC_THREAD_READ, C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify},
{C_INVALID_FD, MTK_NFC_THREAD_SERVICE, C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify}

};


static volatile int g_ThreadExitReadFunc = FALSE;
static volatile int g_ThreadExitMainProcFunc = FALSE;
static volatile int g_ThreadExitService = FALSE;

int gInterfaceHandle;


//mtkNfcDal_sContext_t _sDalContext;
//mtkNfcDal_sLinkFunc_t _gLinkFunc;


void PRINT_INFO2FILE(char buf[])
{
#if 0
FILE *fp = fopen("/data/misc/NFC_FM_RESULT.txt","w");
if (fp != NULL)
{

   {
      fprintf(fp,"%s",&buf[0]);
   }
}
fclose(fp);
#else
printf("%s\n",buf);
#endif
}

/***************************************************************************** 
 * Function
 *****************************************************************************/ 
int main (int argc, char** argv)
{
   int result = MTK_NFC_ERROR;

   #ifdef DEBUG_LOG
   ALOGD("MTK_NFC_MIDDLEWARE main\n");
   #endif
   result = mtk_nfc_sys_init_android();
   
   if (result == MTK_NFC_ERROR)
   {
      //Print Debug
      #ifdef DEBUG_LOG
      ALOGD("mtk_nfc_sys_init_android Err\n");
      #endif
      return (result);
   }


   #ifndef SELF_TEST
   // CREATE SERVICE THREAD
   nfc_service_thread();
   
   mtk_nfc_sys_deinit_android();
   
   #ifdef DEBUG_LOG
   ALOGD("EXIT, libmtknfc_mw\n");
   #endif
   
   #if 0
   if(nfc_service_thread() == MTK_NFC_ERROR)
   {
      //Print Debug
      #ifdef DEBUG_LOG
      ALOGD("nfc_service_thread Err\n");
      #endif
      return (result);

   }
   #endif
   #else
   {
      //SELF TEST CODE, send message to service thread by socket!!
      {
            //-----------------------------------------------//
            //--------------SOCKET CONNECT------------------//
            struct sockaddr_in serv_addr;    
            struct hostent *server;  
            int nfc_sockfd = C_INVALID_SOCKET;
      
            char bug[10]= "TEST CODE";
            int ret=0;
         
            nfc_sockfd = socket(AF_INET, SOCK_STREAM, 0);    
            if (nfc_sockfd < 0)     
            {        
                printf("nfc_open: ERROR opening socket");        
                return (-4);    
            }       
            
            bzero((char *) &serv_addr, sizeof(serv_addr));    
            serv_addr.sin_family = AF_INET; 
            
            serv_addr.sin_addr.s_addr = inet_addr("127.0.0.1");
            
            //bcopy((const char *)server->h_addr, (char *)&serv_addr.sin_addr.s_addr, server->h_length);   
            
            serv_addr.sin_port = htons(7500);    
            sleep(2);  // sleep 5sec for libmnlp to finish initialization        
      
            
            printf("connecting...\r\n");
            
            /* Now connect to the server */    
            if (connect(nfc_sockfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0)     
            {         
               printf("NFC_Open: ERROR connecting");         
               return (-6);    
            } 
            printf("connecting.done\r\n");
            
            //-----------------------------------------------//
            //--------------TEST CODE FROM HERE------------------//
            if(1)  // 
            {
                int ret,rec_bytes = 0;
                char tmpbuf[1024], *ptr;
                
                s_mtk_nfc_main_msg *p_nfc_main;
                 
                 
                p_nfc_main = malloc(sizeof(s_mtk_nfc_main_msg));  
                p_nfc_main->msg_type = 201;
                p_nfc_main->msg_length = 0; 
                
                            
                ret = write(nfc_sockfd, p_nfc_main, sizeof(s_mtk_nfc_main_msg)); 
                
                printf("write.done,%d\r\n",ret);
                  
                //Free memory 
            
            while(1)
            {
               rec_bytes = read(nfc_sockfd, &tmpbuf[0], sizeof(tmpbuf));  
               printf("rec_bytes,%d\r\n",rec_bytes);
               if (rec_bytes > 0)
               {
                  char *p;
                  s_mtk_nfc_main_msg *p_nfc_main_rec;
                  p_nfc_main_rec = (s_mtk_nfc_main_msg*) tmpbuf;
            
                  
                  printf("p_nfc_main_rec,%d,%d\r\n",p_nfc_main_rec->msg_type,p_nfc_main_rec->msg_length);
            
                  break;
                }
                else
                {            
                  usleep(1000);//1ms 
                }
            } 

            
                printf("exit");
                free(p_nfc_main);
                p_nfc_main= NULL;
            
                
                
            }
      else
      {
            //ret = write(nfc_sockfd, (const char*)bug, sizeof(bug)); 
            //printf("GPS_Open: success connecting,ret,%d",ret); 
      #if 0
            while(1)
            {
               usleep(1000000);  // sleep 1000 ms 
            }
      #endif
      }
         }




   }
   #endif


   return (MTK_NFC_SUCCESS);
   
#if 0
    //PRINT_INFO2FILE("TEST");
        
   // BOOT DSP
   printf("BOOT DSP...\r\n");
   if(0)   
   {
      MTK_NFC_MSG_T p_nfc_msg;
      gNfcBootUpStage = TRUE;
      //p_nfc_msg = mtk_nfc_sys_msg_alloc(sizeof(MTK_NFC_MSG_T));
      //if (p_nfc_msg != NULL)
      {
          p_nfc_msg.type = MTK_NFC_MSG_BOOTDSP;
          p_nfc_msg.length = 0;
          mtk_nfc_sys_msg_send(MTK_NFC_TASKID_MAIN, &p_nfc_msg);
      }
      
      while(gNfcBootUpStage == TRUE){usleep(1000);  }// sleep 1 ms 
   }

   {usleep(10000000);  }// sleep 10s 
   // HAL OPEN
   
   printf("HAL OPEN...\r\n");
   if(0)
   {
     MTK_NFC_MSG_T p_nfc_msg;
     //gNfcBootUpStage = FALSE;
     //p_nfc_msg = mtk_nfc_sys_msg_alloc(sizeof(MTK_NFC_MSG_T));
     //if (p_nfc_msg != NULL)
     {
         p_nfc_msg.type = MTK_NFC_MSG_HAL_OPEN;
         p_nfc_msg.length = 0;
         mtk_nfc_sys_msg_send(MTK_NFC_TASKID_MAIN, &p_nfc_msg);
     }
     
      //while(1){usleep(1000000);  }// sleep 1000 ms 
      {usleep(1000000);  }// sleep 1000 ms 
   }

    #if 0
    {
    FILE *fp = fopen("/data/misc/NFC_FM_RESULT.txt","w");
    if (fp != NULL)
    {
       fprintf(fp,"TEST...");
    }
    fclose(fp);
    }
    #endif
   
   {usleep(1000000);  }// sleep 1000 ms 
   
   
   //Create Service thread!!
   //mtk_nfc_service_thread();
   

   //Release all thread!!
   //mtk_nfc_threads_release();
   #ifdef DEBUG_LOG
   ALOGD("main.NFC\n");
   #endif
   
   return (result);
#endif
}


int mtk_nfc_sys_deinit_android(void)
{
   int result = MTK_NFC_SUCCESS;
   result = mtk_nfc_threads_release();
   
   #ifdef DEBUG_LOG
   ALOGD("EXIT, mtk_nfc_sys_deinit_android,%d\n",result);
   #endif
   return result;
}


int mtk_nfc_sys_init_android(void)
{
   int result = MTK_NFC_SUCCESS;

   //Varialbe initial
   gInterfaceHandle = FALSE;
   g_ThreadExitMainProcFunc = FALSE;
   g_ThreadExitService = FALSE;

   mtk_nfc_sys_mutex_inialize();
   mtk_nfc_sys_msg_inialize();
   

   

   // initial physical interface
   result = mtk_nfc_interface_init(0);

   
   // Create Thread
   result = mtk_nfc_threads_create(MTK_NFC_THREAD_READ);
   if (result != MTK_NFC_SUCCESS)
   {
      //Print Debug
      //Create Thread Fail!!
      #ifdef DEBUG_LOG
      ALOGD("mtk_nfc_threads_create, read thread ERR\n");
      #endif
      return (result);
   }


   result = mtk_nfc_threads_create(MTK_NFC_THREAD_MAIN);
   if (result != MTK_NFC_SUCCESS)
   {
      //Print Debug
      //Create Thread Fail!!
      #ifdef DEBUG_LOG
      ALOGD("mtk_nfc_threads_create, main thread ERR\n");
      #endif
      return (result);
   }


   #ifdef SELF_TEST
   result = mtk_nfc_threads_create(MTK_NFC_THREAD_SERVICE);
   if (result != MTK_NFC_SUCCESS)
   {
      //Print Debug
      //Create Thread Fail!!
      #ifdef DEBUG_LOG
      ALOGD("mtk_nfc_threads_create, service thread ERR\n");
      #endif
      return (result);
   }
   #endif

   #ifdef DEBUG_LOG  
   ALOGD("mtk_nfc_sys_init_android\n");
   #endif
   return (result);
   
}




int mtk_nfc_threads_create(int threadId)
{
   int result = MTK_NFC_SUCCESS;

   if ( threadId >= MTK_NFC_THREAD_END)
   {
      #ifdef DEBUG_LOG
      ALOGD("mtk_nfc_threads_create ERR1\n");
      #endif 
      result = MTK_NFC_ERROR;
   }

   //ALOGD("MTK_NFC_THREAD_READ\n");
   if(MTK_NFC_THREAD_READ == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle, NULL, data_read_thread_func, (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         #ifdef DEBUG_LOG
         ALOGD("mtk_nfc_threads_create ERR2\n");
         #endif 
         result = MTK_NFC_ERROR;
      }
   }

   
   //ALOGD("MTK_NFC_THREAD_MAIN\n");
   if(MTK_NFC_THREAD_MAIN == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle, NULL, nfc_main_proc_func, (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         #ifdef DEBUG_LOG
         ALOGD("mtk_nfc_threads_create ERR2\n");
         #endif 
         result = MTK_NFC_ERROR;
      }
   }

#ifdef SELF_TEST
   //ALOGD("MTK_NFC_THREAD_SERVICE\n");
   if(MTK_NFC_THREAD_SERVICE == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle, NULL, nfc_service_thread, (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         #ifdef DEBUG_LOG
         ALOGD("mtk_nfc_threads_create ERR2\n");
         #endif 
         result = MTK_NFC_ERROR;
      }
   }
#endif

   return (result);
}

int mtk_nfc_threads_release(void)
{
   int result = MTK_NFC_SUCCESS;
   int idx;
   
   //g_mtk_nfc_thread[threadId].thread_handle = C_INVALID_TID;
   //result = g_mtk_nfc_thread[threadId].thread_exit(&g_mtk_nfc_thread[threadId]);
   #ifdef DEBUG_LOG
   ALOGD("mtk_nfc_threads_release\n");
   #endif
   
   g_ThreadExitReadFunc = TRUE;
   g_ThreadExitMainProcFunc = TRUE;
   g_ThreadExitService = TRUE;
   
   for (idx = 0; idx < THREAD_NUM; idx++) 
   {     
       if (g_mtk_nfc_thread[idx].thread_handle == C_INVALID_TID)
       {
           continue;
       }
       if (!g_mtk_nfc_thread[idx].thread_exit)
       {
           continue;
       }
       if ((g_mtk_nfc_thread[idx].thread_exit(&g_mtk_nfc_thread[idx])))
       {
        // Error handler
           result = MTK_NFC_ERROR;
       }
       
   }

    for (idx = 0; idx < MTK_NFC_MUTEX_MAX_NUM; idx++)
    {
        mtk_nfc_sys_mutex_destory(idx);
    }

   return (result);
}



void *data_read_thread_func(void * arg)
{
    UINT32 ret = MTK_NFC_SUCCESS,i;
    MTK_NFC_THREAD_T *ptr = (MTK_NFC_THREAD_T*)arg;

    
    if (!arg) 
    {
        pthread_exit(NULL);        
        printf("data_read_thread_func, Create ERR !arg\n");
        return NULL;
    }

    //printf("data_read_thread_func, Create\n");

    while (!g_ThreadExitReadFunc)
    {
        int32_t i4ReadLen = 0;
        char pBuffer[DSP_UART_IN_BUFFER_SIZE];
        int8_t chReadByteOfOnce = 32;

        // blocking read
        if ((i4ReadLen = mtk_nfc_sys_interface_read(pBuffer, chReadByteOfOnce)) == MTK_NFC_ERROR)
            
        {
           //printf("data_read_thread_func, read ERR\n");
           #ifdef DEBUG_LOG
           //NFC_LOGD(DBG_MOD_BIN, DBG_TYP_INF, "READ THREAD_ERR,%d\r\n",i4ReadLen);
           ALOGD("mtk_nfc_threads_release\n");
           #endif
           break;
        }        
        
        //break; // TEST CODE
        ALOGD("READ THREAD,%d \r\n",i4ReadLen);
        
        //NFC_LOGD(DBG_MOD_BIN, DBG_TYP_INF, "READ THREAD,%d \r\n",i4ReadLen);
        if (i4ReadLen > 0)
        {
           //printf("data_read_thread_func, length,%d\n",i4ReadLen);

           ALOGD("[RX],");
           for( i =0;i < i4ReadLen;i++)
           {
              ALOGD("%02x,",pBuffer[i]);
           }
           ALOGD("\n");

           
           //ret = mtkNfcDal_Rx(pBuffer, i4ReadLen);
           ret = mtk_nfc_data_input(pBuffer, i4ReadLen);
           if ( ret != MTK_NFC_SUCCESS)
           {
               ALOGD("mtkNfcDal_Rx: fail\r\n");
           }
        }
        else
        {
           usleep(10000);  // sleep 10 ms        
        }

    }
    
    g_ThreadExitReadFunc = TRUE;
    pthread_exit((void *)ret);
    
    return NULL;
}


void *nfc_main_proc_func(void * arg)
{
    UINT32 ret = MTK_NFC_SUCCESS;
    MTK_NFC_MSG_T *nfc_msg;
    MTK_NFC_THREAD_T *ptr = (MTK_NFC_THREAD_T*)arg;

    
    if (!arg) 
    {
        pthread_exit(NULL);  
        #ifdef DEBUG_LOG
        ALOGD("nfc_main_proc_func, Create ERR !arg\n");
        #endif
        return NULL;
    }

    #ifdef DEBUG_LOG
    ALOGD("nfc_main_proc_func, Create\n");
    #endif
    
    while (!g_ThreadExitMainProcFunc)
    {
        // - recv msg      
        ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_MAIN, &nfc_msg);
        if (ret == MTK_NFC_SUCCESS && (!g_ThreadExitMainProcFunc))
        {
            mtk_nfc_main_proc(nfc_msg);
            // - free msg
            mtk_nfc_sys_msg_free(nfc_msg);
        }
        else
        {
            //read msg fail...
            usleep(1000);//sleep 1msec
        }    

    }
    #ifdef DEBUG_LOG
    ALOGD("nfc_main_proc_func, exit\n");
    #endif
    
    g_ThreadExitMainProcFunc = TRUE;
    pthread_exit((void *)ret);
    
    return NULL;
}


#ifdef SELF_TEST
void *nfc_service_thread(void * arg)
#else
void nfc_service_thread(void )
#endif
{
   NFCSTATUS result = 0x00;
   int server_fd = C_INVALID_SOCKET, conn_fd = C_INVALID_SOCKET, on;
   struct sockaddr_in server_addr;
   struct sockaddr_in client_addr;
   socklen_t size;
   int socket_port = 7500;

   if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == -1)
   {
       printf("socket error\r\n");
       pthread_exit(NULL);
       return NULL;
   }
   /* Enable address reuse */
   on = 1;
   if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)))
   {
       printf("setsockopt error\r\n");
       close(server_fd);
       pthread_exit(NULL);
       return NULL;
   }

   server_addr.sin_family = AF_INET;   /*host byte order*/
   server_addr.sin_port = htons(socket_port); /*short, network byte order*/
   server_addr.sin_addr.s_addr = INADDR_ANY; /*automatically fill with my IP*/
   memset(server_addr.sin_zero, 0x00, sizeof(server_addr.sin_zero));

   if (bind(server_fd, (struct sockaddr*)&server_addr, sizeof(server_addr)) == -1)
   {
       printf("bind error\r\n");
       close(server_fd);
       pthread_exit(NULL);
       return NULL;
   }

   if (listen(server_fd, 5) == -1)
   {
       #ifdef DEBUG_LOG
       ALOGD("listen error\r\n");
       #endif
       close(server_fd);
       pthread_exit(NULL);
       return NULL;
   }

   #ifdef DEBUG_LOG
   ALOGD("socket listen success");
   #endif

   //PRINT_INFO2FILE("socket listen success");
   
   while(!g_ThreadExitService)
   {
      MTK_NFC_MSG_T *nfc_msg;
      size = sizeof(client_addr);

      #ifdef DEBUG_LOG
      ALOGD("socket waiting accept!!\r\n");
      #endif

      gconn_fd_tmp = accept(server_fd, (struct sockaddr*)&client_addr, &size);

      if (1) // config Socket read function to non-blocking type
      {
      int x;
      x=fcntl(gconn_fd_tmp,F_GETFL,0);
      fcntl(gconn_fd_tmp,F_SETFL,x | O_NONBLOCK);
      }



      #ifdef DEBUG_LOG
      ALOGD("socket accept,%x\r\n",gconn_fd_tmp);
      #endif

      if (gconn_fd_tmp <= 0)
      {
          continue;
      }
      else
      {
         char buffer[1024];
         int bufsize=1024, readlen;
         int ret;

         while(!g_ThreadExitService)
         {          
             readlen = read(gconn_fd_tmp, &buffer[0], bufsize);
             
             ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_SERVICE, &nfc_msg);
            
             #ifdef DEBUG_LOG
             //ALOGD("conn_fd_read, %d\r\n",readlen);
             #endif

             
             if(readlen > 0 || ret == MTK_NFC_SUCCESS)
             {
                if(readlen > 0)
                {
                    MTK_NFC_MSG_T *nfc_msg_loc = (MTK_NFC_MSG_T *)buffer;
                    #ifdef DEBUG_LOG
                    ALOGD("from,Socket,read,%d\r\n",readlen);
                    #endif

                    /**/
                    //mtk_nfc_service_proc(&buffer);
                    if(mtk_nfc_service_proc(&buffer) == MTK_NFC_ERROR)
                    {
                        /*EXIT Service thread*/
                        ALOGD("TRIGGER EXIT Service thread");
                        g_ThreadExitService = TRUE;
                        break;
                    }
                }
                if (ret == MTK_NFC_SUCCESS)
                {
                
                    #ifdef DEBUG_LOG
                    ALOGD("from,Msg,ret,%d\r\n",ret);
                    #endif                

                    if(0)
                    {
                        MTK_NFC_MSG_T loc_msg;
                        printf("pMainMsg->msg_type,%x,%x \r\n",nfc_msg->type,nfc_msg->length); 
                    }
                    
                    mtk_nfc_service_proc(nfc_msg);
                    // - free msg
                    mtk_nfc_sys_msg_free(nfc_msg);
                    //usleep(100000);//sleep 100msec

                }
                    
                
             }
             else
             {
                usleep(1000);//sleep 1msec
                //usleep(100000);//sleep 10msec
             }
         }
         
      }


   }
  
   close(gconn_fd_tmp);
   
   gconn_fd_tmp = C_INVALID_SOCKET;   
   g_ThreadExitService = TRUE;
   #ifdef SELF_TEST
   pthread_exit(NULL);  
   #endif

   
   #ifdef DEBUG_LOG
   ALOGD("socket EXIT success");
   #endif
   printf("socket EXIT success");
   
   return result;
}




int mtk_nfc_exit_thread_normal(MTK_NFC_THREAD_T *arg)
{   /* exit thread by pthread_kill -> pthread_join*/
    #ifdef DEBUG_LOG
    ALOGD("mtk_nfc_exit_thread_normal\n");
    #endif
    return 0;
}

int mtk_nfc_thread_active_notify(MTK_NFC_THREAD_T *arg)
{
    #ifdef DEBUG_LOG
    ALOGD("mtk_nfc_thread_active_notify\n");
    #endif
    
    if (!arg)
    {
       // MNL_MSG("fatal error: null pointer!!\n");
       // return -1;
    }
    if (arg->snd_fd != C_INVALID_FD)
    {
        //char buf[] = {MNL_CMD_ACTIVE};
        //return mnl_sig_send_cmd(arg->snd_fd, buf, sizeof(buf));
    }
    return 0;
}



#if 0//def NFC_BOOT_UP_TEST

//extern mtkNfcHal_sContext_t _sHalContext;



#if 0
static const uint8_t nfc_dsp_bin[] =
{
   #include "NFCDSP.bin"
};
#endif

VOID mtkBootNTF(
    VOID *pContext, 
    UINT8 type, 
    VOID *pInfo)
{
    printf(" mtkBootNTF, ok");
}
NFCSTATUS mtkBootInit(void )
{
    uint16_t result;
    mtkNfcHal_sContext_t *pHalCtxt = &_sHalContext;
    mtkNfcIF_sLowerIF_t *psHalLowerIf;   // HAL Lower Layer Functions 
    mtkNfcIF_sCallback_t sHalCallback; 
    
    memset(pHalCtxt, 0, sizeof(mtkNfcHal_sContext_t));
    psHalLowerIf = &pHalCtxt->lower_if;
    sHalCallback.pcontext = pHalCtxt;
    sHalCallback.notify = &mtkBootNTF;
    // Call HAL Lower Layer Register
    result = mtkNfcNci_Register(psHalLowerIf, &sHalCallback);
    result = mtkNfcNci_Init(psHalLowerIf->pcontext);

    return result;
}
#endif



#if 0 // test code for NFC main proc API
NFCSTATUS 
mtkNfcDal_Init ( 
    void *pContext
)
{    
    NFCSTATUS result = NFC_STATUS_SUCCESS;
    mtkNfcDal_sContext_t *pDalCtxt = NULL;    

    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Init called\r\n");

    if ( NULL == pContext )
    {
        result =  NFC_STATUS_INVALID_PARAMS;
    }
    else
    {
        pDalCtxt = (mtkNfcDal_sContext_t *)pContext;
        
        // Register lower/upper layer function callback
        pDalCtxt->fpLowerLayerFunc = mtk_nfc_sys_i2c_write;
        pDalCtxt->fpUpperLayerFunc = mtkNfcBin_Rx;
    
        /* Register link interface */
        #if 0 // please use porting layer directly
        _gLinkFunc.init  = mtk_nfc_sys_uart_init;    
        _gLinkFunc.close = mtk_nfc_sys_uart_uninit;
        _gLinkFunc.read  = mtk_nfc_sys_uart_read;
        _gLinkFunc.write = mtk_nfc_sys_uart_write;
        _gLinkFunc.flush = mtk_nfc_sys_uart_flush;
        #endif        
    }
    
    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Init result : 0x%x\r\n", result);
    
    return result;
}
                          
NFCSTATUS 
mtkNfcDal_Uninit ( 
    void *pContext
)
{
    NFCSTATUS result = NFC_STATUS_SUCCESS;
    mtkNfcDal_sContext_t *pDalCtxt = NULL;      
        
    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Bin Uninit called\r\n");    

    if ( NULL == pContext )
    {
        result =  NFC_STATUS_INVALID_PARAMS;
    }
    else
    {
        pDalCtxt = (mtkNfcDal_sContext_t *)pContext;

        // clear DAL context
        memset(pDalCtxt, 0, sizeof(mtkNfcDal_sContext_t));
    }    
        
    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Uninit result : 0x%x\r\n", result);
    
    return result;
}

NFCSTATUS 
mtkNfcDal_Tx ( 
    uint8_t *pDalBuf,         // Binary payload
    uint16_t u2BufLen         // Binary payload length
)
{
    NFCSTATUS result = NFC_STATUS_SUCCESS;
    int32_t nNbOfBytesWritten = 0;
    
    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Tx called\r\n");
    printf("mtkNfcDal_Tx,in %d,%d, \r\n",nNbOfBytesWritten, u2BufLen);
    nNbOfBytesWritten = _sDalContext.fpLowerLayerFunc(pDalBuf, u2BufLen);

    printf("mtkNfcDal_Tx,out %d,%d, \r\n",nNbOfBytesWritten, u2BufLen);
    if (nNbOfBytesWritten != u2BufLen)
    {
        result = NFC_STATUS_FAIL;
        
        //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "tx length:%d, written:%d\r\n", 
        //        u2BufLen, nNbOfBytesWritten);
    }

    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Tx result : 0x%x\r\n", result);
    
    return result;
}
 
NFCSTATUS 
mtkNfcDal_Rx ( 
    uint8_t *pDalPkt,         // Binary packet
    uint16_t u2PktLen         // Binary packet len
)
{
    NFCSTATUS result = NFC_STATUS_SUCCESS;
    
    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Rx called\r\n");

    result = _sDalContext.fpUpperLayerFunc(pDalPkt, u2PktLen);
    

    //result = _sDalContext.fpUpperLayerFunc(pDalPkt, u2PktLen);

    //NFC_LOGD(DBG_MOD_DAL, DBG_TYP_INF, "Rx result : 0x%x\r\n", result);
    
    return result;
}

NFCSTATUS 
mtkNfcDal_Register ( 
    mtkNfcIF_sLowerIF_t *psLowerIf, 
    mtkNfcIF_sCallback_t *psUpperCb
)
{
    NFCSTATUS result = NFC_STATUS_SUCCESS;

    if ( (NULL == psLowerIf) || 
         (NULL == psUpperCb) || 
         (NULL == psUpperCb->pcontext) /* || 
         (NULL == psUpperCb->notify) */ )
    {
        result = NFC_STATUS_INVALID_PARAMS;
    }
    else
    {
        /* Clear DAL context */
        memset(&_sDalContext, 0, sizeof(mtkNfcDal_sContext_t));
    
        /* Register the DAL context and functions to the upper layer */
        psLowerIf->pcontext = &_sDalContext;        
        psLowerIf->init    = (mtkNfcIF_pInterface_t)&mtkNfcDal_Init;
        psLowerIf->release = (mtkNfcIF_pInterface_t)&mtkNfcDal_Uninit;
        psLowerIf->send    = (mtkNfcIF_pTransact_t)&mtkNfcDal_Tx;
        psLowerIf->receive = (mtkNfcIF_pTransact_t)&mtkNfcDal_Rx;

        /* Register the context and callback function from the upper layer */
        _sDalContext.upper_if.pcontext = psUpperCb->pcontext;        
        _sDalContext.upper_if.notify = psUpperCb->notify;
    }    

    return result;
}


/*****************************************************************************
 * FUNCTION
 *  mtkNfcDal_DevCtrl
 * DESCRIPTION
 *  MT6605 HW reset (ENB & SYSRST_B)
 * PARAMETERS
 *  eState      [IN] MTK_NFC_POWER_STATE_ON      (0)
 *                   MTK_NFC_POWER_STATE_OFF     (1)
 *                   MTK_NFC_POWER_STATE_RESET   (2)
 *                   MTK_NFC_POWER_STATE_SUSPEND (3)
 * RETURNS
 *  NONE
 *****************************************************************************/
NFCSTATUS 
mtkNfcDal_DevCtrl(MTK_NFC_POWER_STATE_E eState)
{   
    NFCSTATUS RetVal = NFC_STATUS_SUCCESS;
        
    if (eState >= MTK_NFC_POWER_STATE_MAX_NUM)
    {
        printf("Unsupported Power State : %d\r\n", eState);
        RetVal = NFC_STATUS_FAIL;
    }
    else
    {
        MTK_NFC_PULL_E ENB;
        MTK_NFC_PULL_E SYSRST_B;

        // read 1st time
        ENB = mtk_nfc_sys_gpio_read(MTK_NFC_GPIO_EN_B);
        SYSRST_B = mtk_nfc_sys_gpio_read(MTK_NFC_GPIO_SYSRST_B);
        printf("Previous ENB %d, SYSRST_B %d\r\n", ENB, SYSRST_B);

        // config gpio by power state    
        switch(eState)
        {             
            case MTK_NFC_POWER_STATE_ON:
                // ------------------------------------------------------------
                // 1. assert ENB
                // 2. assert SYSRST_B
                // 3. delay 1us
                // 4. de-assert SYSRST_B
                // ------------------------------------------------------------ 
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_EN_B, MTK_NFC_PULL_LOW);
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_SYSRST_B, MTK_NFC_PULL_LOW);
                mtk_nfc_sys_sleep(1);
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_SYSRST_B, MTK_NFC_PULL_HIGH);
                break;

            case MTK_NFC_POWER_STATE_OFF:
                // ------------------------------------------------------------
                // 1. assert SYSRST_B
                // 2. de-assert ENB
                // ------------------------------------------------------------                   
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_SYSRST_B, MTK_NFC_PULL_LOW);
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_EN_B, MTK_NFC_PULL_HIGH);
                break;

            case MTK_NFC_POWER_STATE_RESET:
                // ------------------------------------------------------------
                // 1. assert SYSRST_B
                // 2. delay 1us
                // 3. de-assert SYSRST_B
                // ------------------------------------------------------------ 
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_SYSRST_B, MTK_NFC_PULL_LOW);
                mtk_nfc_sys_sleep(1);
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_SYSRST_B, MTK_NFC_PULL_HIGH);
                break;

            case MTK_NFC_POWER_STATE_SUSPEND:
                // ------------------------------------------------------------
                // Full power suspend
                // 1. No need to control gpio
                // ------------------------------------------------------------
                // Do-nothing
                break;

            case MTK_NFC_POWER_STATE_STANDBY:
                // ------------------------------------------------------------
                // Full power standby
                // 1. assert SYSRST_B
                // ------------------------------------------------------------
                mtk_nfc_sys_gpio_write(MTK_NFC_GPIO_SYSRST_B, MTK_NFC_PULL_LOW);                
                break;
                
            default:
                printf( "Unsupported Power State : %d\r\n", eState);
                RetVal = NFC_STATUS_FAIL;
                break;
        }

        // read 2nd time for verification
        ENB = mtk_nfc_sys_gpio_read(MTK_NFC_GPIO_EN_B);
        SYSRST_B = mtk_nfc_sys_gpio_read(MTK_NFC_GPIO_SYSRST_B);                
        printf("Current ENB %d, SYSRST_B %d\r\n", ENB, SYSRST_B);        
    }
    
    return RetVal;
}


#if 0

void mtkNfcLib_DeinitCb(void *pContext, NFCSTATUS status)
{
    if(status == NFC_STATUS_SUCCESS)
    {
        #ifdef DEBUG_LOG
        ALOGD("HAL Close Success !!!\r\n");
        #endif
    }
    else
    {
        #ifdef DEBUG_LOG
        ALOGD("HAL Close Fail !!!\r\n");        
        #endif
    }
}


void mtkNfcLib_InitCb(void *pContext, NFCSTATUS status)
{
    if(status == NFC_STATUS_SUCCESS)
    {
        #ifdef DEBUG_LOG
        ALOGD("HAL Open Success !!!\r\n");        
        #endif       
    }
    else
    {
        #ifdef DEBUG_LOG
        ALOGD("HAL Open Fail !!!\r\n");        
        #endif       
    }
     printf("mtkNfcLib_InitCb");
    mtkNfc_FM_SWP_test(1);
}


INT32 mtk_nfc_main_proc (MTK_NFC_MSG_T *prmsg)
{
    if (prmsg == NULL)
    {
        
        #ifdef DEBUG_LOG
        ALOGD("mtk_nfc_main_proc, recv prmsg is null pointer\r\n");
        #endif
        return MTK_NFC_ERROR;
    }
    
    #ifdef DEBUG_LOG
    ALOGD("mtk_nfc_main_proc, recv prmsg,%x\r\n",prmsg->type);
    #endif
    switch (prmsg->type)
    {
        #if 1
        case MTK_NFC_MSG_RECV_PKT:
            // Receive Binary packet
            if (NFC_STATUS_SUCCESS == u1ControlBinaryPacket())
            {
                //NFC_LOGD(DBG_MOD_BIN, DBG_TYP_INF, "Proc Bin Packet - Success\r\n");        
            }
            else
            {
                //NFC_LOGD(DBG_MOD_BIN, DBG_TYP_INF, "Proc Bin Packet - Fail\r\n");                
            }
            break;
        #endif
        
        case MTK_NFC_MSG_HAL_OPEN:            
            {
                 mtkNfcHal_sOpenReq_t sOpenReq;
                        sOpenReq.reset_type = 0x01; //mtkHal_eReset_all
                        mtkNfcHal_Open( 
                                        &g_HwRef,
                                        &sOpenReq,
                                        &mtkNfcLib_InitCb,
                                        &g_TempCtxt);
            }
            break;

        case MTK_NFC_MSG_HAL_CLOSE:
            mtkNfcHal_Close( NULL,
                             &mtkNfcLib_DeinitCb,
                             NULL);
            break;
            
        case MTK_NFC_MSG_BOOTDSP: 
            {
               uint16_t result;
               const uint8_t  *pDSPBinaryFile=NULL;
               MTK_NFC_BOOT_CONFIG cfg={0};
               
               gNfcBootUpStage = TRUE;
               
               result = mtkBootInit();               
   
               printf("result , %d\n",result);
   
               printf("\n");
               printf("-------------------------------------------\n");
               printf(" NFC Console Boot up sequence              \n");
               printf("-------------------------------------------\n");    
   
               pDSPBinaryFile = nfc_dsp_bin;
               cfg.eLinkType = MTK_NFC_INTERFACE_I2C;
               cfg.eRefClkRateId = MTK_NFC_REFCLKRATE_26;
               cfg.forceDNL = TRUE;
               cfg.u4LinkSpeed = 115200;
                   
               result = mtkNfcHal_bootup_sequence(pDSPBinaryFile,&cfg);
               
               printf(" Boot up sequence result: %d\n", result);
                
            }
            break;

        #if 1 // FOR Factory mode
        case MTK_NFC_FM_SWP: 
            {
            FILE *fp = fopen("/data/misc/NFC_FM_RESULT.txt","w");
            if (fp != NULL)
            {
        
               {
                  fprintf(fp,"TEST");
               }
            }
            fclose(fp);
            }
        
            {
               uint16_t result;
               const uint8_t  *pDSPBinaryFile=NULL;
               MTK_NFC_BOOT_CONFIG cfg={0};
               
               gNfcBootUpStage = TRUE;
               
               result = mtkBootInit();               
   
               printf("result , %d\n",result);
   
               printf("\n");
               printf("-------------------------------------------\n");
               printf(" NFC Console Boot up sequence              \n");
               printf("-------------------------------------------\n");    
   
               pDSPBinaryFile = nfc_dsp_bin;
               cfg.eLinkType = MTK_NFC_INTERFACE_I2C;
               cfg.eRefClkRateId = MTK_NFC_REFCLKRATE_26;
               cfg.forceDNL = TRUE;
               cfg.u4LinkSpeed = 115200;
                   
               result = mtkNfcHal_bootup_sequence(pDSPBinaryFile,&cfg);
               
               printf(" Boot up sequence result: %d\n", result);
                
            }
            {
            FILE *fp = fopen("/data/misc/NFC_FM_RESULT.txt","w");
            if (fp != NULL)
            {
        
               {
                  fprintf(fp,"BootupDONE\n");
               }
            }
            fclose(fp);
            }
            
            //{usleep(5000000);  }// sleep 5s 
            //{usleep(500000);  }// sleep 0.5s 
            
            {
            FILE *fp = fopen("/data/misc/NFC_FM_RESULT.txt","w");
            if (fp != NULL)
            {
        
               {
                  fprintf(fp,"HAL OPEN\n");
               }
            }
            fclose(fp);
            }

            {
                 mtkNfcHal_sOpenReq_t sOpenReq;
                        sOpenReq.reset_type = 0x01; //mtkHal_eReset_all
                        mtkNfcHal_Open( 
                                        &g_HwRef,
                                        &sOpenReq,
                                        &mtkNfcLib_InitCb,
                                        &g_TempCtxt);
            }

            //mtkNfc_FM_SWP_test(1);
            
            break;
        case MTK_NFC_FM_READ_UID:
            
            break;
        case MTK_NFC_FM_CARD_MODE:
            
            break;
        #endif

        default:
            break;
    }

    return MTK_NFC_SUCCESS;
}
#endif



#endif

#define LOG_TAG "NFC-MW"

VOID PrintDBG(CH *data, ...)
{
    va_list  args; 
    char szBuf[512]; 
    //int dwWritten = 0; 
    
    va_start(args, data); 
    vsnprintf(szBuf, 512, data, args); 
    va_end(args); 
    
    ALOGD("%s",szBuf); 
}








 
