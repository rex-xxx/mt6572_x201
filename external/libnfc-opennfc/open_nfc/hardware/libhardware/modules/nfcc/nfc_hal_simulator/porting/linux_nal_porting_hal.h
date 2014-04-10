/*******************************************************************************

  Communication Port Definition

*******************************************************************************/

#define P_MAX_RX_BUFFER_SIZE  300

/* The com port instance structure */
struct __tNALComInstance
{
   void* pNFCCConnection;

   uint8_t aRXBuffer[P_MAX_RX_BUFFER_SIZE];

   uint32_t nRXDataLength;

   uint8_t* pRXData;

   bool_t bInitialResetDone;
   uint32_t nResetPending;
};

#define P_COM_RESET_FLAG  0x00
#define P_COM_DATA_FLAG   0x01

/*******************************************************************************

  Timer Definition

*******************************************************************************/


#define TickToMs(x)     x
#define MsToTicks(x)   x

/* The timer instance structure */
struct __tNALTimerInstance
{
   uint32_t nTimerValue;      /* The current timer expiration value, 0 if no timer is pending */
   bool_t bIsInitialized;       /* The initalization flag */
};

/*******************************************************************************

  Context definition

*******************************************************************************/

#define P_THREAD_NOT_STARTED        0
#define P_THREAD_STARTED            1
#define P_THREAD_STOP_REQUESTED     3


struct __tNALInstance
{
   char16_t       aNFCCDeviceURI[256];

#if 0
   HANDLE      hKickEvent;
   HANDLE      hTimerEvent;

   HANDLE      hThread;
   uint8_t     nThreadState;
#endif
};