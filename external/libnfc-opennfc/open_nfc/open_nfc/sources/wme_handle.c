/*
 * Copyrightss (c) 2007 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*******************************************************************************
   Contains the Handle list implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( HANDL )

#include "wme_context.h"

/* The variable P_DFC_HANDLE_INITIAL_SIZE defines the initial size of the handle list in handle entries. */
#define P_HANDLE_LIST_INITIAL_SIZE  32

/* The variable P_HANDLE_LIST_DELTA defines the increment of the handle queue in handle entries. */
#define P_HANDLE_LIST_DELTA  16

typedef struct __tPHandleListElt
{
#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
   tUserInstance* pUserInstance;
#endif /* P_CONFIG_DRIVER */
   uint32_t nState;
   tHandleObjectHeader* pObjectHeader;
   tDFCCallbackContext* pCloseSafeCallbackContext;
} tPHandleListElt;

/*
   Description of the states of a handle entry

    4 States           pObjectHeader    pCloseSafeCallbackContext
     Free                 null             null
     Used                 object           null
     Used+Weak            object           null
     Destroy Pending      object           destroy context
*/


#define P_HANDLE_DRIVER_MARKER_BIT      0x80000000
#define P_HANDLE_WEAK_BIT               0x00000001
#define P_HANDLE_DESTROY_PENDING_BIT    0x00000002
#define P_HANDLE_COUNTER_INCREMENT      0x00010000

#define P_HANDLE_BUILD_HANDLE( nPositionX, nStateX ) \
            (((nPositionX) & 0x0000FFFF) | ((nStateX) & 0xFFFF0000))

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
#define P_HANDLE_SET_STATE_VALUE(pnStateX, nCounterValueX) \
   { *(pnStateX) = (((nCounterValueX) & 0x7FFF0000) | P_HANDLE_DRIVER_MARKER_BIT); }
#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
#define P_HANDLE_SET_STATE_VALUE(pnStateX, nCounterValueX) \
   { *(pnStateX) = ((nCounterValueX) & 0x7FFF0000); }
#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#define P_HANDLE_IS_NULL_VALUE(nCounterValueX) \
   (((nCounterValueX) & 0x7FFF0000) == 0)

#define P_HANDLE_SET_STATE_FREE(pnStateX) \
   { *(pnStateX) = 0; }

#define P_HANDLE_IS_STATE_FREE(nStateX) \
   (( (nStateX) == 0 )?W_TRUE:W_FALSE)

#define P_HANDLE_SET_STATE_DESTROY_PENDING(pnStateX) \
   { *(pnStateX) = P_HANDLE_DESTROY_PENDING_BIT; }

#define P_HANDLE_IS_STATE_DESTROY_PENDING(nStateX) \
   (( (nStateX) == P_HANDLE_DESTROY_PENDING_BIT )?W_TRUE:W_FALSE)

#define P_HANDLE_SET_STATE_WEAK(pnStateX) \
   { *(pnStateX) |= P_HANDLE_WEAK_BIT; }

#define P_HANDLE_IS_STATE_WEAK(nStateX) \
  ((((nStateX) & P_HANDLE_WEAK_BIT) != 0)?W_TRUE:W_FALSE)

/** See header file **/
bool_t PHandleCreate(
         tHandleList* pHandleList )
{
   CMemoryFill(pHandleList, 0, sizeof(tHandleList));

   pHandleList->nCurrentCounter = P_HANDLE_COUNTER_INCREMENT;
   pHandleList->nListSize = P_HANDLE_LIST_INITIAL_SIZE;

   if((pHandleList->pElementList = (tPHandleListElt*)CMemoryAlloc(
      sizeof(tPHandleListElt)*P_HANDLE_LIST_INITIAL_SIZE)) != null)
   {
      CMemoryFill(pHandleList->pElementList, 0,
         (sizeof(tPHandleListElt)*P_HANDLE_LIST_INITIAL_SIZE));
   }
   else
   {
      PDebugError("PHandleCreate: Cannot create the handle list");
      return W_FALSE;
   }

   return W_TRUE;
}


/** See header file **/
void PHandleDestroy(
         tHandleList* pHandleList )
{
   if(pHandleList != null)
   {
      uint32_t nPosition;
      for(nPosition = 0; nPosition < pHandleList->nListSize; nPosition++)
      {
         tPHandleListElt* pListElement = &pHandleList->pElementList[nPosition];
         uint32_t nState = pListElement->nState;

         if(P_HANDLE_IS_STATE_DESTROY_PENDING(nState))
         {
            tHandleObjectHeader* pObjectHeader = pListElement->pObjectHeader;

            CDebugAssert(pObjectHeader != null);
            CDebugAssert(pListElement->pCloseSafeCallbackContext != null);

            CMemoryFree(pListElement->pCloseSafeCallbackContext);

            PDebugTrace("Dead Handle ref=%d type=%p pObject=%p",
                  pObjectHeader->nReferenceCount,
                  pObjectHeader->pType,
                  pObjectHeader);
         }
         else if(P_HANDLE_IS_STATE_FREE(nState) == W_FALSE)
         {
            tHandleObjectHeader* pObjectHeader = pListElement->pObjectHeader;

            CDebugAssert(pListElement->pCloseSafeCallbackContext == null);
            CDebugAssert(pObjectHeader != null);

            PDebugTrace("Handle %08X ref=%d type=%p pObject=%p",
                  P_HANDLE_BUILD_HANDLE(nPosition, nState),
                  pObjectHeader->nReferenceCount,
                  pObjectHeader->pType,
                  pObjectHeader);
         }

      }

      CMemoryFree(pHandleList->pElementList);

      CMemoryFill(pHandleList, 0, sizeof(tHandleList));
   }
}

/**
 * Adds a new handle for an object.
 *
 * The reference count of the object is increased by one.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObjectHeader  The pointer on the object header.
 *
 * @param[in]  bIsWeak The weak flag.
 *
 * @param[out]  phHandle  A pointer on a variable valued with the new handle value.
 *             This value is set to W_NULL_HANDLE upon error.
 *
 * @return  One of the following values:
 *            - W_SUCCESS  The new handle is created.
 *            - W_ERROR_OUT_OF_RESOURCE  There is not enough resource to create the handle.
 **/
static W_ERROR static_PHandleAddEntry(
            tContext* pContext,
            tHandleObjectHeader* pObjectHeader,
            bool_t bIsWeak,
            W_HANDLE* phHandle)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition;
   uint32_t nNewCounter;
   tPHandleListElt* pElementList;
   uint32_t nReferenceCount;

   CDebugAssert(pObjectHeader != null);

   nReferenceCount = pObjectHeader->nReferenceCount;

   if(nReferenceCount == 0xFFFFFFFF)
   {
      PDebugError("static_PHandleAddEntry: reference count overflow");
      *phHandle = W_NULL_HANDLE;
      return W_ERROR_OUT_OF_RESOURCE;
   }

   for(nPosition = 0; nPosition < pHandleList->nListSize; nPosition++)
   {
      if(P_HANDLE_IS_STATE_FREE(pHandleList->pElementList[nPosition].nState))
      {
         CDebugAssert(pHandleList->pElementList[nPosition].pCloseSafeCallbackContext == null);
         goto set_handle;
      }
   }

   pElementList = (tPHandleListElt*)CMemoryAlloc( (pHandleList->nListSize + P_HANDLE_LIST_DELTA)*sizeof(tPHandleListElt));
   if (pElementList != null)
   {
      CMemoryCopy(pElementList, pHandleList->pElementList,
          pHandleList->nListSize * sizeof(tPHandleListElt));
      CMemoryFree( pHandleList->pElementList );
      pHandleList->pElementList = pElementList;
   }
   else
   {
      PDebugError("static_PHandleAddEntry: Cannot enlarge the handle list");
      *phHandle = W_NULL_HANDLE;
      return W_ERROR_OUT_OF_RESOURCE;

   }

   nPosition = pHandleList->nListSize;
   pHandleList->nListSize = nPosition + P_HANDLE_LIST_DELTA;

   pHandleList->pElementList = pElementList;

   CMemoryFill(&(pHandleList->pElementList[nPosition]), 0,
      P_HANDLE_LIST_DELTA*sizeof(tPHandleListElt));

set_handle:

   nNewCounter = pHandleList->nCurrentCounter;
   if(P_HANDLE_IS_NULL_VALUE(nNewCounter))
   {
      nNewCounter = P_HANDLE_COUNTER_INCREMENT;
   }
   pHandleList->nCurrentCounter = nNewCounter + P_HANDLE_COUNTER_INCREMENT;

   P_HANDLE_SET_STATE_VALUE(&pHandleList->pElementList[nPosition].nState, nNewCounter);
   if(bIsWeak != W_FALSE)
   {
      P_HANDLE_SET_STATE_WEAK(&pHandleList->pElementList[nPosition].nState);
   }

   *phHandle = P_HANDLE_BUILD_HANDLE(nPosition, pHandleList->pElementList[nPosition].nState);

   pHandleList->pElementList[nPosition].pObjectHeader = pObjectHeader;
   CDebugAssert(pHandleList->pElementList[nPosition].pCloseSafeCallbackContext == null);

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
   pHandleList->pElementList[nPosition].pUserInstance = PContextGetCurrentUserInstance(pContext);
#endif /* P_CONFIG_DRIVER */

   if(bIsWeak == W_FALSE)
   {
      /* Increase the reference count */
      pObjectHeader->nReferenceCount = nReferenceCount + 1;
   }

   return W_SUCCESS;
}

/** See header file **/
W_ERROR PHandleRegister(
            tContext* pContext,
            void* pObject,
            tHandleType* pType,
            W_HANDLE* phHandle)
{
   if(pObject == null)
   {
      *phHandle = W_NULL_HANDLE;
      return W_SUCCESS;
   }
   ((tHandleObjectHeader*)pObject)->pType = pType;
   ((tHandleObjectHeader*)pObject)->nReferenceCount = 0;
   ((tHandleObjectHeader*)pObject)->pParentObject = null;
   ((tHandleObjectHeader*)pObject)->pChildObject = null;

   return static_PHandleAddEntry( pContext, (tHandleObjectHeader*)pObject, W_FALSE, phHandle);
}

static uint32_t static_PHandleGetObjectPosition(
                     tContext* pContext,
                     W_HANDLE hObject )
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition = hObject & 0x0000FFFF;
   uint32_t nState;

   if( nPosition >= pHandleList->nListSize)
   {
      return ((uint32_t)-1);
   }

   nState = pHandleList->pElementList[nPosition].nState;

   if(P_HANDLE_IS_STATE_FREE(nState) || P_HANDLE_IS_STATE_DESTROY_PENDING(nState))
   {
      return ((uint32_t)-1);
   }

   if(hObject != P_HANDLE_BUILD_HANDLE(nPosition, nState))
   {
      return ((uint32_t)-1);
   }

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

   if (PContextGetCurrentUserInstance(pContext) != null) {

      if(pHandleList->pElementList[nPosition].pUserInstance != PContextGetCurrentUserInstance(pContext))
      {
         return ((uint32_t)-1);
      }
   }
#endif /* P_CONFIG_DRIVER */
   return nPosition;
}

static uint32_t static_PHandleGetNextDestroyPendingPosition(tContext* pContext, tHandleObjectHeader * pObjectHeader, uint32_t nIndexStart)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);

   for(; nIndexStart < pHandleList->nListSize ; ++nIndexStart)
   {
      tPHandleListElt* pElement = &pHandleList->pElementList[nIndexStart];
      if(P_HANDLE_IS_STATE_DESTROY_PENDING(pElement->nState)
      || P_HANDLE_IS_STATE_WEAK(pElement->nState))
      {
         if(pElement->pObjectHeader == pObjectHeader)
         {
            return nIndexStart;
         }
      }
      else
      {
         CDebugAssert(pElement->pObjectHeader != pObjectHeader);
      }
   }
   return (uint32_t)-1;
}

static bool_t static_PHandleIsValid(
                     tContext* pContext,
                     W_HANDLE hObject)
{
   uint32_t nPosition = static_PHandleGetObjectPosition(pContext, hObject);

   return (nPosition == ((uint32_t)-1)) ? W_FALSE : W_TRUE;
}


static tHandleObjectHeader* static_PHandleGetObject(
                     tContext* pContext,
                     W_HANDLE hObject,
                     tHandleType* pExpectedType )
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition = static_PHandleGetObjectPosition(pContext, hObject);
   tHandleObjectHeader* pObjectHeader;

   if( nPosition == ((uint32_t)-1))
   {
      return null;
   }

   pObjectHeader = pHandleList->pElementList[nPosition].pObjectHeader;

   if(pExpectedType != P_HANDLE_TYPE_ANY)
   {
      do
      {
         if(pObjectHeader->pType == pExpectedType)
         {
            return pObjectHeader;
         }

         pObjectHeader = pObjectHeader->pParentObject;
      } while(pObjectHeader != null);

      return null;
   }

   return pObjectHeader;
}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/** See header file **/
W_ERROR PHandleSetUserInstance(
            tContext* pContext,
            W_HANDLE hObject,
         tUserInstance* pUserInstance)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition = static_PHandleGetObjectPosition(pContext, hObject);

   if(nPosition == ((uint32_t)-1))
   {
      PDebugError("PHandleSetUserInstance: Illegal value for the handle %08X", hObject);
      return W_ERROR_BAD_HANDLE;
   }

   if((pHandleList->pElementList[nPosition].pUserInstance != null)
   && (pHandleList->pElementList[nPosition].pUserInstance != pUserInstance))
   {
      PDebugError("PHandleSetUserInstance: User instance already set for %08X", hObject);
      return W_ERROR_BAD_HANDLE;
   }

   pHandleList->pElementList[nPosition].pUserInstance = pUserInstance;

   return W_SUCCESS;
}

#endif/* P_CONFIG_DRIVER */

/** See header file **/
W_ERROR PHandleAddHeir(
            tContext* pContext,
            W_HANDLE hObject,
            void* pObject,
            tHandleType* pType)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition;
   tHandleObjectHeader* pParentObject;
   tHandleObjectHeader* pChildObject = (tHandleObjectHeader*)pObject;

   nPosition = static_PHandleGetObjectPosition(pContext, hObject);

   if(nPosition == ((uint32_t)-1))
   {
      PDebugError("PHandleAddHeir: Illegal value for the handle %08X", hObject);
      return W_ERROR_BAD_HANDLE;
   }

   pParentObject = pHandleList->pElementList[nPosition].pObjectHeader;
   CDebugAssert(pParentObject->nReferenceCount != ((uint32_t)-1));

   pHandleList->pElementList[nPosition].pObjectHeader = pChildObject;

   pChildObject->pType = pType;
   pChildObject->nReferenceCount = pParentObject->nReferenceCount;
   pChildObject->pParentObject = pParentObject;
   pChildObject->pChildObject = null;
   pParentObject->pChildObject = pChildObject;

   pParentObject->nReferenceCount = ((uint32_t)-1);

   /* Set the value of the handle pointers */
   for(nPosition = 0; nPosition < pHandleList->nListSize ; ++nPosition)
   {
      if((P_HANDLE_IS_STATE_FREE(pHandleList->pElementList[nPosition].nState) == W_FALSE)
      && (P_HANDLE_IS_STATE_DESTROY_PENDING(pHandleList->pElementList[nPosition].nState) == W_FALSE)
      && (pHandleList->pElementList[nPosition].pObjectHeader == pParentObject))
      {
         pHandleList->pElementList[nPosition].pObjectHeader = pChildObject;
      }
   }

   return W_SUCCESS;
}

/** See header file **/
void* PHandleRemoveLastHeir(
            tContext* pContext,
            W_HANDLE hObject,
            tHandleType* pType)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition;
   tHandleObjectHeader* pChildObject;
   tHandleObjectHeader* pParentObject;

   nPosition = static_PHandleGetObjectPosition(pContext, hObject);

   if(nPosition == ((uint32_t)-1))
   {
      PDebugError("PHandleRemoveLastHeir: Illegal value for the handle %08X", hObject);
      return null;
   }

   pChildObject = pHandleList->pElementList[nPosition].pObjectHeader;

   if((pChildObject == null) || (pChildObject->pType != pType))
   {
      PDebugError("PHandleRemoveLastHeir: Wrong type for the handle %08X", hObject);
      return null;
   }

   CDebugAssert(pChildObject->nReferenceCount != ((uint32_t)-1));

   pParentObject = pChildObject->pParentObject;
   if(pParentObject == null)
   {
      PDebugError("PHandleRemoveLastHeir: No parent object for the handle %08X", hObject);
      return null;
   }
   CDebugAssert(pParentObject->nReferenceCount == ((uint32_t)-1));
   CDebugAssert(pParentObject->pChildObject == pChildObject);

   pParentObject->pChildObject = null;
   pParentObject->nReferenceCount = pChildObject->nReferenceCount;

   CMemoryFill(pChildObject, 0, sizeof(tHandleObjectHeader));

   /* Set the value of the handle pointers */
   for(nPosition = 0; nPosition < pHandleList->nListSize ; ++nPosition)
   {
      if((P_HANDLE_IS_STATE_FREE(pHandleList->pElementList[nPosition].nState) == W_FALSE)
      && (P_HANDLE_IS_STATE_DESTROY_PENDING(pHandleList->pElementList[nPosition].nState) == W_FALSE)
      && (pHandleList->pElementList[nPosition].pObjectHeader == pChildObject))
      {
         pHandleList->pElementList[nPosition].pObjectHeader = pParentObject;
      }
   }

   return pChildObject;
}

/** See header file **/
W_ERROR PHandleGetObject(
            tContext* pContext,
            W_HANDLE hObject,
            tHandleType* pExpectedType,
            void** ppObject)
{
   if(hObject == W_NULL_HANDLE)
   {
      *ppObject = null;
      return W_SUCCESS;
   }

   *ppObject = static_PHandleGetObject(pContext, hObject, pExpectedType);

   if(*ppObject == null)
   {
      return W_ERROR_BAD_HANDLE;
   }

   return W_SUCCESS;
}


/* See header file */
W_ERROR PHandleGetConnectionObject(
            tContext* pContext,
            W_HANDLE hHandle,
            tHandleType* pExpectedType,
            void** ppObject)
{
   if(hHandle == W_NULL_HANDLE)
   {
      *ppObject = null;
      return W_ERROR_BAD_HANDLE;
   }

   if (static_PHandleIsValid(pContext, hHandle) == W_FALSE)
   {
     *ppObject = null;
      return W_ERROR_BAD_HANDLE;
   }

   * ppObject = static_PHandleGetObject(pContext, hHandle, pExpectedType);

   if (* ppObject == null)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   return W_SUCCESS;
}

/** See header file **/
static W_ERROR static_PHandleDuplicate(
            tContext* pContext,
            W_HANDLE hObject,
            W_HANDLE* phNewHandle,
            bool_t bWeak)
{
   tHandleObjectHeader* pObjectHeader;

   if(phNewHandle == null)
   {
      PDebugError("PHandleDuplicate/Weak: Bad value for phNewHandle");
      return W_ERROR_BAD_PARAMETER;
   }

   if(hObject == W_NULL_HANDLE)
   {
      *phNewHandle = W_NULL_HANDLE;
      return W_SUCCESS;
   }

   pObjectHeader = static_PHandleGetObject(pContext, hObject, P_HANDLE_TYPE_ANY);

   if(pObjectHeader == null)
   {
      PDebugError("PHandleDuplicate/Weak: Illegal value for the handle %08X", hObject);
      *phNewHandle = W_NULL_HANDLE;
      return W_ERROR_BAD_HANDLE;
   }

   return static_PHandleAddEntry( pContext, pObjectHeader, bWeak, phNewHandle);
}

/** See header file **/
W_ERROR PHandleDuplicate(
            tContext* pContext,
            W_HANDLE hObject,
            W_HANDLE* phNewHandle )
{
   return static_PHandleDuplicate(pContext, hObject, phNewHandle, W_FALSE);
}

/** See header file **/
W_ERROR PHandleDuplicateWeak(
            tContext* pContext,
            W_HANDLE hObject,
            W_HANDLE* phNewHandle )
{
   return static_PHandleDuplicate(pContext, hObject, phNewHandle, W_TRUE);
}

typedef struct __tPHandleDestroyParameters
{
   tHandleObjectHeader * pTopObjectHeader;
   tHandleObjectHeader * pCurrentObjectHeader;
   W_ERROR nError;
} tPHandleDestroyParameters;

static void static_PHandleDestroy(
            tContext* pContext,
            tHandleObjectHeader* pObjectHeader,
            tPHandleDestroyParameters* pDestroyParameters);

static void static_PHandleDestroyCompletion(
            tContext* pContext,
            void* pCallbackParameters,
            W_ERROR nError)
{
   tPHandleDestroyParameters * pDestroyParameters = (tPHandleDestroyParameters *) pCallbackParameters;

   CDebugAssert(pDestroyParameters != null);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PHandleDestroyCompletion: Error %s", PUtilTraceError(nError));

      if(pDestroyParameters->nError == W_SUCCESS)
      {
         pDestroyParameters->nError = nError;
      }
   }

   static_PHandleDestroy(pContext, pDestroyParameters->pCurrentObjectHeader, pDestroyParameters);

}

static void static_PHandleCallCloseSafeCallbackFunction(
            tContext* pContext,
            tHandleObjectHeader* pObjectHeader,
            W_ERROR nError)
{
   tPHandleListElt * pElement;
   tHandleList * pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition = static_PHandleGetNextDestroyPendingPosition(pContext, pObjectHeader, 0);

   while( nPosition != ((uint32_t) -1 ))
   {
      pElement = &(pHandleList->pElementList[nPosition]);

      if(pElement->pCloseSafeCallbackContext != null)
      {
         PDFCPostContext2(pElement->pCloseSafeCallbackContext, nError);
         CMemoryFree(pElement->pCloseSafeCallbackContext);
         pElement->pCloseSafeCallbackContext = null;
      }
      pElement->pObjectHeader = null;
      P_HANDLE_SET_STATE_FREE(&pHandleList->pElementList[nPosition].nState);

      nPosition = static_PHandleGetNextDestroyPendingPosition(pContext, pObjectHeader, nPosition + 1);
   }
}

static void static_PHandleDestroy(
            tContext* pContext,
            tHandleObjectHeader* pObjectHeader,
            tPHandleDestroyParameters* pDestroyParameters)
{
   tHandleObjectHeader* pTopObjectHeader = pObjectHeader;

   while(pObjectHeader != null)
   {
      uint32_t nResult = P_HANDLE_DESTROY_DONE;
      tHandleObjectHeader* pParentObjectHeader = pObjectHeader->pParentObject;

      if ( pObjectHeader->pType->pDestroyObject != null )
      {
         nResult = pObjectHeader->pType->pDestroyObject(pContext, pObjectHeader);
      }
      else if ( pObjectHeader->pType->pDestroyObjectAsync != null )
      {
         CDebugAssert(pDestroyParameters != null);
         pDestroyParameters->pCurrentObjectHeader = pParentObjectHeader;

         nResult = pObjectHeader->pType->pDestroyObjectAsync(
               pContext,
               static_PHandleDestroyCompletion, pDestroyParameters,
               pObjectHeader);
      }

      if(nResult == P_HANDLE_DESTROY_PENDING)
      {
         return;
      }
      else if(nResult == P_HANDLE_DESTROY_LATER)
      {
         /* Store the object at the end of the list to call it once again
            when the other are already called once */
         if(pParentObjectHeader == null)
         {
            pParentObjectHeader = pObjectHeader;
         }
         else
         {
            tHandleObjectHeader* pPrevHeader = pParentObjectHeader;
            while(pPrevHeader->pParentObject != null)
            {
               pPrevHeader = pPrevHeader->pParentObject;
            }
            pPrevHeader->pParentObject = pObjectHeader;
            pObjectHeader->pParentObject = null;
         }
      }
      else
      {
         CDebugAssert(nResult == P_HANDLE_DESTROY_DONE);
      }

      pObjectHeader = pParentObjectHeader;
   }

   if(pDestroyParameters != null)
   {
      static_PHandleCallCloseSafeCallbackFunction(pContext,
         pDestroyParameters->pTopObjectHeader,
         pDestroyParameters->nError);

      CMemoryFree(pDestroyParameters);
   }
   else
   {
      static_PHandleCallCloseSafeCallbackFunction(pContext,
         pTopObjectHeader,
         W_SUCCESS);
   }
}

/** See header file **/
void PHandleDecrementReferenceCount(
            tContext* pContext,
            void* pObject)
{
   tHandleObjectHeader* pObjectHeader = (tHandleObjectHeader*)pObject;
   uint32_t nReferenceCount;

   while(pObjectHeader->pChildObject != null)
   {
      CDebugAssert(pObjectHeader->nReferenceCount == (uint32_t)-1);
      pObjectHeader = pObjectHeader->pChildObject;
   }

   nReferenceCount = pObjectHeader->nReferenceCount;
   CDebugAssert(nReferenceCount != 0);
   CDebugAssert(nReferenceCount != (uint32_t)-1);

   if((pObjectHeader->nReferenceCount = nReferenceCount - 1) == 0)
   {
      tPHandleDestroyParameters* pDestroyParameters = null;
      tHandleObjectHeader* pCurrentHeader;

      for(pCurrentHeader = pObjectHeader;
         pCurrentHeader != null;
         pCurrentHeader = pCurrentHeader->pParentObject)
      {
         if(pCurrentHeader->pType->pDestroyObjectAsync != null)
         {
            pDestroyParameters = (tPHandleDestroyParameters *) CMemoryAlloc(sizeof(tPHandleDestroyParameters));

            CDebugAssert(pDestroyParameters != null);
            if(pDestroyParameters == null)
            {
               PDebugError("PHandleDecrementReferenceCount : No enough memory to allocate needed ressources  for the asynchronous destroy");
               break;
            }


            pDestroyParameters->pTopObjectHeader = pObjectHeader;
            pDestroyParameters->pCurrentObjectHeader = pObjectHeader;
            pDestroyParameters->nError = W_SUCCESS;
            break;
         }
      }

      static_PHandleDestroy( pContext, pObjectHeader, pDestroyParameters );
   }
}

/** See header file **/
void PHandleIncrementReferenceCount(
            void* pObject )
{
   tHandleObjectHeader* pObjectHeader = (tHandleObjectHeader*)pObject;

   while(pObjectHeader->pChildObject != null)
   {
      CDebugAssert(pObjectHeader->nReferenceCount == (uint32_t)-1);
      pObjectHeader = pObjectHeader->pChildObject;
   }

   CDebugAssert(pObjectHeader->nReferenceCount != (uint32_t)-1);

   pObjectHeader->nReferenceCount++;
}


#if (P_BUILD_CONFIG == P_CONFIG_USER)

bool_t PHandleIsUser(
            tContext* pContext,
            W_HANDLE hObject )
{
   return (hObject & P_HANDLE_DRIVER_MARKER_BIT) == 0 ? W_TRUE : W_FALSE;
}

#else

bool_t PHandleIsUser(
            tContext* pContext,
            W_HANDLE hObject )
{
   return (W_TRUE);
}

#endif /* P_CONFIG_USER */

/** See header file **/
void PHandleClose(
            tContext* pContext,
            W_HANDLE hObject )
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition;
   tPHandleListElt* pListElement;
   tHandleObjectHeader* pObjectHeader;
   uint32_t nState;

   if(hObject != W_NULL_HANDLE)
   {
#if (P_BUILD_CONFIG == P_CONFIG_USER)

      /* Specific case for USER / KERNEL porting, when a driver handle is closed from user */

      if (PHandleIsUser(pContext, hObject) == W_FALSE)
      {
         PHandleCloseDriver(pContext, hObject);

         /* @todo what can we do here if IOCTL failed */
         return;
      }
#endif /* P_CONFIG_USER */

      /* Generic case */

      nPosition = static_PHandleGetObjectPosition(pContext, hObject);

      if(nPosition == ((uint32_t)-1))
      {

         PDebugError("PHandleClose: Illegal value for the handle %08X", hObject);
         return;
      }

      pListElement = &pHandleList->pElementList[nPosition];
      CDebugAssert(pListElement->pCloseSafeCallbackContext == null);

      pObjectHeader = pListElement->pObjectHeader;
      CDebugAssert(pObjectHeader != null);
      CDebugAssert(pObjectHeader->nReferenceCount != 0);

      nState = pListElement->nState;
      P_HANDLE_SET_STATE_FREE(&pListElement->nState);
      pListElement->pObjectHeader = null;

      if(P_HANDLE_IS_STATE_WEAK(nState) == W_FALSE)
      {
         PHandleDecrementReferenceCount(pContext, pObjectHeader);
      }
   }
}

#if (P_BUILD_CONFIG == P_CONFIG_USER)

static void static_PHandleCloseSafeDriverCompleted(
            tContext * pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) pCallbackParameter;

   PDFCPostContext2(pCallbackContext, nError);
   CMemoryFree(pCallbackContext);
}

#endif /* P_CONFIG_USER */

/** See header file **/
void PHandleCloseSafe(
            tContext * pContext,
            W_HANDLE hObject,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   tDFCCallbackContext sCallbackContext;
   tPHandleListElt * pListElement;
   uint32_t nPosition, nState;
   W_ERROR nError = W_SUCCESS;

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   if(hObject != W_NULL_HANDLE)
   {
#if (P_BUILD_CONFIG == P_CONFIG_USER)

      /* Specific case for USER / KERNEL porting, when a driver handle is closed from user */

      if (PHandleIsUser(pContext, hObject) == W_FALSE)
      {
         tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));

         if (pCallbackContext != null)
         {
            * pCallbackContext = sCallbackContext;

            PHandleCloseSafeDriver(pContext, hObject, static_PHandleCloseSafeDriverCompleted, pCallbackContext);

            nError = PContextGetLastIoctlError(pContext);
         }
         else
         {
            nError = W_ERROR_OUT_OF_RESOURCE;
         }

         if(nError != W_SUCCESS)
         {
            goto send_result;
         }

         return;
      }
#endif

      /* Generic case */

      nPosition = static_PHandleGetObjectPosition(pContext, hObject);

      if(nPosition == ((uint32_t)-1))
      {
         PDebugError("PHandleCloseSafe: Illegal value for the handle %08X", hObject);
         nError = W_ERROR_BAD_HANDLE;
         goto send_result;
      }
      pListElement = &pHandleList->pElementList[nPosition];

      pListElement->pCloseSafeCallbackContext = (tDFCCallbackContext *)CMemoryAlloc(sizeof(tDFCCallbackContext));
      if(pListElement->pCloseSafeCallbackContext != null)
      {
         *(pListElement->pCloseSafeCallbackContext) = sCallbackContext;
      }
      else
      {
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto send_result;
      }

      nState = pListElement->nState;
      P_HANDLE_SET_STATE_DESTROY_PENDING(&pListElement->nState);

      if(P_HANDLE_IS_STATE_WEAK(nState) == W_FALSE)
      {
         PHandleDecrementReferenceCount(pContext, pListElement->pObjectHeader);
      }

      return;
   }

send_result:

   if(nError != W_SUCCESS)
   {
      PDebugError("PHandleCloseSafe: returning error %s", PUtilTraceError(nError));
   }

   PDFCPostContext2(&sCallbackContext, nError);
}

/** See header file **/
void PHandleGetCount(
         tContext* pContext,
         uint32_t* pnUserHandleNumber,
         uint32_t* pnDriverHandleNumber)
{
   if(pnUserHandleNumber != null)
   {
      tHandleList* pHandleList = PContextGetHandleList(pContext);
      uint32_t nPosition;
      uint32_t nCounter = 0;

      PDebugTrace("PHandleGetCount() handle list:");

      for(nPosition = 0; nPosition < pHandleList->nListSize; nPosition++)
      {
         if(P_HANDLE_IS_STATE_FREE(pHandleList->pElementList[nPosition].nState) == W_FALSE)
         {
            PDebugTrace("  - handle %08X",
               P_HANDLE_BUILD_HANDLE(nPosition, pHandleList->pElementList[nPosition].nState));
            nCounter++;
         }
      }
      *pnUserHandleNumber = nCounter;
   }

   if(pnDriverHandleNumber != null)
   {
#if (P_BUILD_CONFIG == P_CONFIG_USER)
      * pnDriverHandleNumber = PHandleGetCountDriver(pContext);
      /* @todo If the IOCTL failed, the handle number is set to 0 */
#else
      *pnDriverHandleNumber = 0;
#endif /* P_CONFIG_USER */
   }
}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/** See header file **/
uint32_t PHandleGetCountDriver(
            tContext* pContext )
{
   uint32_t nCount;

   PHandleGetCount(pContext, &nCount, null);

   return nCount;
}

/** See header file **/
void PHandleCloseDriver(
            tContext* pContext,
            W_HANDLE hObject )
{
   PHandleClose(pContext, hObject);
}

typedef struct __tPHandleCloseSafeDriverParams
{
   tDFCDriverCCReference pDriverCC;

} tPHandleCloseSafeDriverParams;

static void static_PHandleCloseSafeDriverCompletion(
            tContext * pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tPHandleCloseSafeDriverParams* pParams =
      (tPHandleCloseSafeDriverParams*)pCallbackParameter;

   /* Send the error */
   PDFCDriverPostCC2(
      pParams->pDriverCC,
      nError);

   CMemoryFree(pParams);
}

/** See header file **/
void PHandleCloseSafeDriver(
            tContext * pContext,
            W_HANDLE hObject,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tDFCDriverCCReference pDriverCC;
   tPHandleCloseSafeDriverParams* pParams =
      (tPHandleCloseSafeDriverParams*)CMemoryAlloc(sizeof(tPHandleCloseSafeDriverParams));

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pDriverCC);

   if(pParams != null)
   {
      pParams->pDriverCC = pDriverCC;
      PHandleCloseSafe(pContext, hObject, static_PHandleCloseSafeDriverCompletion, pParams);
   }
   else
   {
      /* Send the error */
      PDFCDriverPostCC2(
         pDriverCC,
         W_ERROR_OUT_OF_RESOURCE);
   }
}

/** See header file **/
void PHandleCloseAll(
            tContext* pContext,
            tUserInstance* pUserInstance)
{
   tHandleList* pHandleList = PContextGetHandleList(pContext);
   uint32_t nPosition;
   for(nPosition = 0; nPosition < pHandleList->nListSize; nPosition++)
   {
      tPHandleListElt* pListElement = &pHandleList->pElementList[nPosition];

      if(pListElement->pUserInstance == pUserInstance)
      {
         uint32_t nState = pListElement->nState;

         if(P_HANDLE_IS_STATE_DESTROY_PENDING(nState))
         {
            CDebugAssert(pListElement->pObjectHeader != null);
            CDebugAssert(pListElement->pCloseSafeCallbackContext != null);

            /* Do not call the callback function, just erase the reference */
            CMemoryFree(pListElement->pCloseSafeCallbackContext);
            pListElement->pCloseSafeCallbackContext = null;

            P_HANDLE_SET_STATE_FREE(&pListElement->nState);
            pListElement->pObjectHeader = null;
            pListElement->pUserInstance = null;
         }
         else if(P_HANDLE_IS_STATE_FREE(nState) == W_FALSE)
         {
            tHandleObjectHeader* pObjectHeader = pListElement->pObjectHeader;

            CDebugAssert(pListElement->pCloseSafeCallbackContext == null);
            CDebugAssert(pObjectHeader != null);
            CDebugAssert(pObjectHeader->nReferenceCount != 0);

            P_HANDLE_SET_STATE_FREE(&pListElement->nState);
            pListElement->pObjectHeader = null;
            pListElement->pUserInstance = null;

            if(P_HANDLE_IS_STATE_WEAK(nState) == W_FALSE)
            {
               PHandleDecrementReferenceCount(pContext, pObjectHeader);
            }
         }
      }
   }
}

#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Returns the number and the values of visible properties.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   pObjectHeader  The pointer to the object header.
 *
 * @param[out]  pnPropertyNumber  A pointer on a variable set with the number of properties.
 *              The value is set to zero in case of error.
 *
 * @param[out]  pPropertyArray  A pointer on a buffer receiving the property values.
 *              It could be null if only the number of properties is needed.
 *              The content of the array is left unchanged if an error is returned.
 *
 * @retval  W_SUCCESS  The properties are returned.
 * @retval  W_ERROR_BAD_HANDLE If the value of pObjectHeader is null.
 * @retval  W_ERROR_BAD_STATE If an error is returned by the get property function of pObjectHeader.
 * @retval  W_ERROR_OUT_OF_RESOURCE Not enough memory.
 **/
static W_ERROR static_PHandleGetVisibleProperties(tContext* pContext,
                                                  tHandleObjectHeader* pObjectHeader,
                                                  uint32_t* pnPropertyNumber,
                                                  uint8_t* pPropertyArray)
{
   uint32_t nNumber = 0;
   uint8_t* pProperties = null;
   uint32_t i;

   /* Init the number of properties */
   *pnPropertyNumber = 0;

   if (null == pObjectHeader)
   {
      PDebugError("static_PHandleGetVisibleProperties: pObjectHeader == null");
      return W_ERROR_BAD_HANDLE;
   }

   if (null != pObjectHeader->pType->pGetPropertyNumber)
   {
      nNumber = pObjectHeader->pType->pGetPropertyNumber(pContext, pObjectHeader);

      if ( (nNumber > 0) && (null != pObjectHeader->pType->pGetProperties) )
      {
         pProperties = CMemoryAlloc(nNumber * sizeof(uint8_t));
         if (null == pProperties)
         {
            PDebugError("static_PHandleGetVisibleProperties: Error returned by the get property function");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         if (W_FALSE == pObjectHeader->pType->pGetProperties(pContext, pObjectHeader, pProperties))
         {
            PDebugError("static_PHandleGetVisibleProperties: Error returned by the get property function");
            return W_ERROR_BAD_STATE;
         }

         for (i = 0; i < nNumber; i++)
         {
            if (W_TRUE == PReaderUserIsPropertyVisible(pProperties[i]))
            {
               /* If this property is visible and pPropertyArray is not null, copy it in the output buffer */
               if (null != pPropertyArray)
               {
                  pPropertyArray[*pnPropertyNumber] = pProperties[i];
               }
               /* If this property is visible, increment the number of properties */
               (*pnPropertyNumber)++;
            }
         }

         CMemoryFree(pProperties);
      }
      else
      {
         /* Only the number of properties is retrieved */
         *pnPropertyNumber = nNumber;
      }
   }

   return W_SUCCESS;
}

/* See WReaderGetIdentifier */
W_ERROR PHandleGetIdentifier(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t* pIdentifierBuffer,
         uint32_t nIdentifierBufferMaxLength,
         uint32_t* pnIdentifierActualLength)
{
   tHandleObjectHeader* pObjectHeader;
   W_ERROR nError = W_SUCCESS;
   uint32_t nIdentifierActualLength = 0;

   if((pIdentifierBuffer == null) && (nIdentifierBufferMaxLength != 0))
   {
      PDebugError("PHandleGetIdentifier: Bad parameters");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

#if (P_BUILD_CONFIG == P_CONFIG_USER)
   /* Specific case for USER / KERNEL porting, when accessing a driver handle from user */
   if (PHandleIsUser(pContext, hConnection) == W_FALSE)
   {
      PDebugError("PHandleGetIdentifier: Illegal user handle value %08X", hConnection);
      nError = W_ERROR_BAD_HANDLE;
      goto return_function;
   }
#endif /* #if (P_BUILD_CONFIG == P_CONFIG_USER) */

   pObjectHeader = static_PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_ANY);

   if(pObjectHeader == null)
   {
      PDebugError("PHandleGetIdentifier: Illegal value for the handle %08X", hConnection);
      nError = W_ERROR_BAD_HANDLE;
      goto return_function;
   }

   do
   {
      if(pObjectHeader->pType->pGetIdentifierLength != null)
      {
         CDebugAssert(pObjectHeader->pType->pGetIdentifier != null);

         nIdentifierActualLength = pObjectHeader->pType->pGetIdentifierLength(pContext, pObjectHeader);

         if(nIdentifierActualLength > nIdentifierBufferMaxLength)
         {
            PDebugError("PHandleGetIdentifier: Buffer too short");
            nError = W_ERROR_BUFFER_TOO_SHORT;
            goto return_function;
         }

         if(nIdentifierActualLength != 0)
         {
            pObjectHeader->pType->pGetIdentifier(pContext, pObjectHeader, pIdentifierBuffer);
         }

         nError = W_SUCCESS;
         goto return_function;
      }

      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   PDebugError("PHandleGetIdentifier: No identifier defined for this object");
   nError = W_ERROR_CONNECTION_COMPATIBILITY;

return_function:

   if ((nError != W_SUCCESS) && (nError != W_ERROR_BUFFER_TOO_SHORT))
   {
      PDebugError("PHandleGetIdentifier: returning error %s", PUtilTraceError(nError));
      nIdentifierActualLength = 0;
   }

   if(pnIdentifierActualLength != null)
   {
      *pnIdentifierActualLength = nIdentifierActualLength;
   }
   return nError;
}

/* See WReaderExchangeData */
void PHandleExchangeData(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t nPropertyIdentifer,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pReaderToCardBuffer,
         uint32_t nReaderToCardBufferLength,
         uint8_t* pCardToReaderBuffer,
         uint32_t nCardToReaderBufferMaxLength,
         W_HANDLE* phOperation )
{
   tHandleObjectHeader* pObjectHeader;
   W_ERROR nError = W_SUCCESS;
   tDFCCallbackContext sCallbackContext;

   if(((pReaderToCardBuffer == null) && (nReaderToCardBufferLength != 0))
   || ((pCardToReaderBuffer == null) && (nCardToReaderBufferMaxLength != 0)))
   {
      PDebugError("PHandleExchangeData: Bad parameters");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

#if (P_BUILD_CONFIG == P_CONFIG_USER)
   /* Specific case for USER / KERNEL porting, when accessing a driver handle from user */
   if (PHandleIsUser(pContext, hConnection) == W_FALSE)
   {
      PDebugError("PHandleExchangeData: Illegal user handle value %08X", hConnection);
      nError = W_ERROR_BAD_HANDLE;
      goto return_function;
   }
#endif /* #if (P_BUILD_CONFIG == P_CONFIG_USER) */

   pObjectHeader = static_PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_ANY);

   if(pObjectHeader == null)
   {
      PDebugError("PHandleExchangeData: Illegal value for the handle %08X", hConnection);
      nError = W_ERROR_BAD_HANDLE;
      goto return_function;
   }

   do
   {
      if(pObjectHeader->pType->pExchangeData != null)
      {
         /* if the desired property is ANY the first exchange data function function is executed,
            otherwise, the property must match with the current pObject property*/
         if(nPropertyIdentifer == W_PROP_ANY
         || ((pObjectHeader->pType->pCheckProperties != null) &&
                  (pObjectHeader->pType->pCheckProperties(pContext, pObjectHeader, nPropertyIdentifer) != W_FALSE)))
         {
            pObjectHeader->pType->pExchangeData(
               pContext, pObjectHeader,
               pCallback, pCallbackParameter,
               pReaderToCardBuffer, nReaderToCardBufferLength,
               pCardToReaderBuffer, nCardToReaderBufferMaxLength,
               phOperation );
            return;
         }
      }

      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   PDebugError("PHandleExchangeData: No raw exchange function defined for this object");
   nError = W_ERROR_CONNECTION_COMPATIBILITY;

return_function:

   PDebugError("PHandleExchangeData: returning error %s", PUtilTraceError(nError));

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext3(&sCallbackContext, 0, nError);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

/** See header file **/
W_ERROR PHandleGetAllPropertyNumber(
            tContext* pContext,
            W_HANDLE hObject,
            uint32_t* pnPropertyNumber,
            bool_t bOnlyVisible)
{
   tHandleObjectHeader* pObjectHeader;
   uint32_t nPropertyNumber;

#  if (P_BUILD_CONFIG == P_CONFIG_USER)

   /* Specific case for USER / KERNEL porting, when accessing a  driver handle from user */

   if (PHandleIsUser(pContext, hObject) == W_FALSE)
   {
      return PHandleGetPropertyNumberDriver(pContext, hObject, pnPropertyNumber);

   }
#  endif

   pObjectHeader = static_PHandleGetObject(pContext, hObject, P_HANDLE_TYPE_ANY);
   nPropertyNumber = 0;

   if(pObjectHeader == null)
   {
      PDebugError("PHandleGetPropertyNumber: Illegal value for the handle %08X", hObject);
      *pnPropertyNumber = 0;
      return W_ERROR_BAD_HANDLE;
   }

   do
   {
#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
      if (W_TRUE == bOnlyVisible)
      {
         uint32_t n;
         W_ERROR nError;

         if ( (nError = static_PHandleGetVisibleProperties(pContext, pObjectHeader, &n, null)) == W_SUCCESS)
         {
            nPropertyNumber += n;
         }
         else
         {
            PDebugError("PHandleGetPropertyNumber: Error returned by static_PHandleGetVisibleProperties");
            *pnPropertyNumber = 0;
            return nError;
         }
      }
      else
#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
         if (pObjectHeader->pType->pGetPropertyNumber != null)
      {
         nPropertyNumber += pObjectHeader->pType->pGetPropertyNumber(pContext, pObjectHeader);
      }
      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   *pnPropertyNumber = nPropertyNumber;

   if(nPropertyNumber == 0)
   {
      /* Not an error, just a trace */
      PDebugTrace("PHandleGetPropertyNumber: No properties defined for this object");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   return W_SUCCESS;
}

/** See header file **/
W_ERROR PHandleGetPropertyNumber(
            tContext* pContext,
            W_HANDLE hObject,
            uint32_t* pnPropertyNumber)
{
   /* Retrieve all properties (visible and internal) */
   return PHandleGetAllPropertyNumber(
      pContext,
      hObject,
      pnPropertyNumber,
      W_FALSE);  /* bOnlyVisible = W_FALSE */
}

/** See header file **/
W_ERROR PHandleGetAllProperties(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t* pPropertyArray,
            uint32_t nPropertyArrayLength,
            bool_t bOnlyVisible)
{
   tHandleObjectHeader* pObjectHeader;
   uint32_t nPropertyNumber;

#  if (P_BUILD_CONFIG == P_CONFIG_USER)

   /* Specific case for USER / KERNEL porting, when accessing a  driver handle from user */

   if (PHandleIsUser(pContext, hObject) == W_FALSE)
   {
      return PHandleGetPropertiesDriver(pContext, hObject, pPropertyArray, nPropertyArrayLength);
   }
#  endif

   pObjectHeader = static_PHandleGetObject(pContext, hObject, P_HANDLE_TYPE_ANY);
   nPropertyNumber = 0;

   if(pObjectHeader == null)
   {
      PDebugError("PHandleGetProperties: Illegal value for the handle %08X", hObject);
      return W_ERROR_BAD_HANDLE;
   }

   do
   {
      uint32_t n = 0;

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)
      W_ERROR nError;

      if (W_TRUE == bOnlyVisible)
      {
         if ( (nError = static_PHandleGetVisibleProperties(pContext, pObjectHeader, &n, pPropertyArray)) != W_SUCCESS)
         {
            PDebugError("PHandleGetProperties: Error returned by static_PHandleGetVisibleProperties");
            return nError;
         }
      }
      else
#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
      {
         if (pObjectHeader->pType->pGetProperties != null)
         {
            if(pObjectHeader->pType->pGetProperties(pContext, pObjectHeader, pPropertyArray) == W_FALSE)
            {
               PDebugError("PHandleGetProperties: Error returned by the get property function");
               return W_ERROR_BAD_STATE;
            }
         }

         if (pObjectHeader->pType->pGetPropertyNumber != null)
         {
            n = pObjectHeader->pType->pGetPropertyNumber(pContext, pObjectHeader);
         }
      }

      pPropertyArray += n;
      nPropertyNumber += n;

      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   if(nPropertyNumber == 0)
   {
      PDebugError("PHandleGetProperties: No properties defined for this object");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   return W_SUCCESS;
}

/** See header file **/
W_ERROR PHandleGetProperties(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t* pPropertyArray,
            uint32_t nPropertyArrayLength)
{
   /* Retrieve all properties (visible and internal) */
   return PHandleGetAllProperties(
      pContext,
      hObject,
      pPropertyArray,
      nPropertyArrayLength,
      W_FALSE);  /* bOnlyVisible = W_FALSE */
}

/** See header file **/
W_ERROR PHandleCheckProperty(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t nPropertyValue)
{
   tHandleObjectHeader* pObjectHeader;
   uint32_t nPropertyNumber;

#  if (P_BUILD_CONFIG == P_CONFIG_USER)

   /* Specific case for USER / KERNEL porting, when accessing a driver handle from user */

   if (PHandleIsUser(pContext, hObject) == W_FALSE)
   {
      return PHandleCheckPropertyDriver(pContext, hObject, nPropertyValue);
   }
#  endif

   pObjectHeader = static_PHandleGetObject(pContext, hObject, P_HANDLE_TYPE_ANY);
   nPropertyNumber = 0;

   if(pObjectHeader == null)
   {
      PDebugError("PHandleCheckProperty: Illegal value for the handle %08X", hObject);
      return W_ERROR_BAD_HANDLE;
   }

   do
   {
      if(pObjectHeader->pType->pCheckProperties != null)
      {
         if(pObjectHeader->pType->pCheckProperties(pContext, pObjectHeader, nPropertyValue) != W_FALSE)
         {
            return W_SUCCESS;
         }
      }

      if (pObjectHeader->pType->pGetPropertyNumber != null)
      {
         nPropertyNumber += pObjectHeader->pType->pGetPropertyNumber(pContext,pObjectHeader);
      }

      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   if(nPropertyNumber == 0)
   {
      PDebugError("PHandleGetProperties: No properties defined for this object");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   return W_ERROR_ITEM_NOT_FOUND;
}

/* See header file */
bool_t PHandleIsPollingSupported(
            tContext* pContext,
            W_HANDLE hConnection)
{
   tHandleObjectHeader* pObjectHeader;

#if (P_BUILD_CONFIG == P_CONFIG_USER)
   /* Specific case for USER / KERNEL porting, when accessing a driver handle from user */
   if (PHandleIsUser(pContext, hConnection) == W_FALSE)
   {
      PDebugError("PHandlePoll: Illegal user handle value %08X", hConnection);
      return W_FALSE;
   }
#endif /* #if (P_BUILD_CONFIG == P_CONFIG_USER) */

   pObjectHeader = static_PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_ANY);
   if(pObjectHeader == null)
   {
      PDebugError("PHandlePoll: Illegal value for the handle %08X", hConnection);
      return W_FALSE;
   }

   do
   {
      if (pObjectHeader->pType->pPoll != null)
      {
         PDebugTrace("PHandlePoll: polling function is defined for this object");
         return W_TRUE;
      }

      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   PDebugTrace("PHandleIsPollingSupported: No polling function defined for this object");
   return W_FALSE;
}

/* See header file */
void PHandlePoll(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction * pCallback,
            void * pCallbackParameter)
{
   tHandleObjectHeader* pObjectHeader;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

#if (P_BUILD_CONFIG == P_CONFIG_USER)
   /* Specific case for USER / KERNEL porting, when accessing a driver handle from user */
   if (PHandleIsUser(pContext, hConnection) == W_FALSE)
   {
      PDebugError("PHandlePoll: Illegal user handle value %08X", hConnection);
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }
#endif /* #if (P_BUILD_CONFIG == P_CONFIG_USER) */

   pObjectHeader = static_PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_ANY);
   if(pObjectHeader == null)
   {
      PDebugError("PHandlePoll: Illegal value for the handle %08X", hConnection);
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   do
   {
      if(pObjectHeader->pType->pPoll != null)
      {
         pObjectHeader->pType->pPoll(pContext, pObjectHeader, pCallback, pCallbackParameter);
         return;
      }

      pObjectHeader = pObjectHeader->pParentObject;
   } while(pObjectHeader != null);

   PDebugError("PHandlePoll: No raw function defined for this object");
   nError = W_ERROR_CONNECTION_COMPATIBILITY;

return_error:
   PDebugError("PHandlePoll: returning error %s", PUtilTraceError(nError));

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, nError);
}


#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

W_ERROR PHandleGetPropertyNumberDriver(
            tContext* pContext,
            W_HANDLE hObject,
            uint32_t* pnPropertyNumber)
{
   return PHandleGetPropertyNumber(pContext, hObject, pnPropertyNumber);
}


W_ERROR PHandleGetPropertiesDriver(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t* pPropertyArray,
            uint32_t  nPropertyArrayLength)
{
   return PHandleGetProperties(pContext, hObject, pPropertyArray, nPropertyArrayLength);
}


W_ERROR PHandleCheckPropertyDriver(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t nPropertyValue)
{
   return PHandleCheckProperty(pContext, hObject, nPropertyValue);
}


#endif /* #if (P_BUILD_CONFIG == P_CONFIG_DRIVER) */
