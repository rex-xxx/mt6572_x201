/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

#ifndef __WME_HANDLE_H
#define __WME_HANDLE_H

/*******************************************************************************
   Contains the declaration of the handle registry functions
*******************************************************************************/

/* Retrieve the address of the main structure from the address of one of the object header */
#define P_HANDLE_GET_STRUCTURE( type, object, field ) \
            (type*)((uint8_t*)(object) - (uint8_t*)&(((type*)0)->field))

#define P_HANDLE_DESTROY_DONE       0
#define P_HANDLE_DESTROY_PENDING    1
#define P_HANDLE_DESTROY_LATER      2

/* Type of the destroy object function registered with the handle type */
typedef uint32_t tHandleDestroyObject(
         tContext* pContext,
         void* pObject );

/* Type of the destroy object function registered with the handle type */
typedef uint32_t tHandleDestroyObjectAsync(
         tContext* pContext,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter,
         void* pObject );

typedef uint32_t tHandleGetPropertyNumber(
         tContext* pContext,
         void* pObject);

typedef bool_t tHandleGetProperties(
         tContext* pContext,
         void* pObject,
         uint8_t* pPropertyArray );

typedef bool_t tHandleCheckProperty(
         tContext* pContext,
         void* pObject,
         uint8_t nPropertyValue );

typedef uint32_t tHandleGetIdentifierLength(
         tContext* pContext,
         void* pObject);

typedef void tHandleGetIdentifier(
         tContext* pContext,
         void* pObject,
         uint8_t* pIdentifierBuffer);

typedef void tHandleExchangeData(
         tContext * pContext,
         void* pObject,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pReaderToCardBuffer,
         uint32_t nReaderToCardBufferLength,
         uint8_t* pCardToReaderBuffer,
         uint32_t nCardToReaderBufferMaxLength,
         W_HANDLE* phOperation );

typedef void tHandlePoll(
         tContext * pContext,
         void* pObject,
         tPBasicGenericCallbackFunction * pCallback,
         void * pCallbackParameter);

/**
 * The type of the structure defining a handle type.
 **/
typedef const struct __tHandleType
{
   tHandleDestroyObject* pDestroyObject;
   tHandleDestroyObjectAsync* pDestroyObjectAsync;
   tHandleGetPropertyNumber *pGetPropertyNumber;
   tHandleGetProperties* pGetProperties;
   tHandleCheckProperty* pCheckProperties;
   tHandleGetIdentifierLength* pGetIdentifierLength;
   tHandleGetIdentifier* pGetIdentifier;
   tHandleExchangeData* pExchangeData;
   tHandlePoll* pPoll;
} tHandleType;

/*
 * List of the handle types.
 */
#define P_HANDLE_TYPE_ANY ((tHandleType*)null)  /* Special value for any type */

/**
 * Header structure to be added to any object registered with a handle.
 * The content of the structure should be modified only by the implementation of
 * the handle registry.
 **/
struct __tHandleObjectHeader;

typedef struct __tHandleObjectHeader
{
   uint32_t nReferenceCount;
   tHandleType* pType;
   struct __tHandleObjectHeader* pParentObject;
   struct __tHandleObjectHeader* pChildObject;
} tHandleObjectHeader;

struct __tPHandleListElt;

/* The handle list - Do not use directly */
typedef struct __tHandleList
{
   uint32_t nCurrentCounter;
   uint32_t nListSize;

   struct __tPHandleListElt* pElementList;
} tHandleList;


/**
 * @brief Creates a handle list instance.
 *
 * @param[out]  pHandleList  The handle list to initialize.
 *
 * @return  W_TRUE if the handle list is initialized, W_FALSE otherwise.
 **/
bool_t PHandleCreate(
         tHandleList* pHandleList );

/**
 * @brief Destroyes a handle list instance.
 *
 * This function should be called when the registry is no longer needed.
 * All the resources of the tregistry are freed. The object pointers are not freed.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every handle is erased.
 *
 * @post  PHandleDestroy() does not return any error. The caller should always
 *        assume that the handle list instance is destroyed after this call.
 *
 * @post  The caller should never re-use the handle list instance value.
 *
 * @param[in]  pHandleList  The handle list to destroy.
 **/
void PHandleDestroy(
         tHandleList* pHandleList );

/**
 * Registers a new object pointer and creates a handle on it.
 *
 * The initial reference count of the object is set to one.
 *
 * If pObject is null, no handle is created. The value pointed by phHandle is set
 * to W_NULL_HANDLE and the function retruns W_SUCCESS.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The pointer on the object instance.
 *
 * @param[in]  pType   The object type.
 *
 * @param[out]  phHandle  A pointer on a variable valued with the new handle value.
 *             This value is set to W_NULL_HANDLE upon error.
 *
 * @return  One of the following values:
 *            - W_SUCCESS  The new handle is created.
 *            - W_ERROR_OUT_OF_RESOURCE  There is not enough resource to create the handle.
 **/
W_ERROR PHandleRegister(
            tContext* pContext,
            void* pObject,
            tHandleType* pType,
            W_HANDLE* phHandle);

/**
 * Adds a heir object to an object.
 *
 * The handle value is unchanged and the reference count is unchanged.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The object handle.
 *
 * @param[in]  pObject  The pointer on the heir object instance.
 *
 * @param[in]  pType   The object type.
 *
 * @return     One of the following value:
 *              - W_SUCCESS The operation is performed.
 *              - W_ERROR_BAD_HANDLE  The handle is not valid.
 **/
W_ERROR PHandleAddHeir(
            tContext* pContext,
            W_HANDLE hObject,
            void* pObject,
            tHandleType* pType);

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

/**
 * Set the user instance for a handle.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The object handle.
 *
 * @param[in]  pUserInstance The current user instance.
 *
 * @return     One of the following value:
 *              - W_SUCCESS The operation is performed.
 *              - W_ERROR_BAD_HANDLE  The handle is not valid.
 **/
W_ERROR PHandleSetUserInstance(
            tContext* pContext,
            W_HANDLE hObject,
            tUserInstance* pUserInstance);

#endif/* P_CONFIG_DRIVER */

/**
 * Removes the last heir object.
 *
 * The handle value is unchanged and the reference count is unchanged.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The object handle.
 *
 * @param[in]  pType   The object type.
 *
 * @return  The pointer on the heir object instance or null if the object is not found.
 **/
void* PHandleRemoveLastHeir(
            tContext* pContext,
            W_HANDLE hObject,
            tHandleType* pType);

/**
 * Returns the pointer on the object instance corresponding to a handle value.
 *
 * This function should be called to check the validity of a handle value and
 * to obtain the pointer on the object.
 *
 * If the parameter hObject is set to W_NULL_HANDLE, PHandleGetObject()
 * returns W_SUCCESS and the value pointed by ppObject is set to null whatever
 * expected type nExpectedType value.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle value to check.
 *
 * @param[in]  pExpectedType  The expected object type.
 *
 * @param[out] ppObject  A pointer on a variable valued with the pointer on the
 *             corresponding object is the handle is valid and the object is of
 *             the expected type. Otherwise, the value is set to null.
 *
 * @return     One of the following value:
 *              - W_SUCCESS The handle is valid and of the specified type.
 *              - W_ERROR_BAD_HANDLE  The handle is not valid or the object is
 *                not of the specified type.
 **/
W_ERROR PHandleGetObject(
            tContext* pContext,
            W_HANDLE hObject,
            tHandleType* pExpectedType,
            void** ppObject);

/**
 * Returns the pointer on the object instance corresponding to a handle value.
 *
 * This function should be called to check the validity of a handle value and
 * to obtain the pointer on the object.
 *
 * If the parameter hObject is set to W_NULL_HANDLE, PHandleGetObject()
 * returns W_ERROR_BAD_HANDLE and the value pointed by ppObject is set to null whatever
 * expected type nExpectedType value.
 *
 * If the parameter hObject is set to W_NULL_HANDLE, PHandleGetObject()
 * returns W_ERROR_BAD_HANDLE and the value pointed by ppObject is set to null whatever
 * expected type nExpectedType value.
 *
 * If the parameter hObject is set to a valid handle, but not of the specified type, PHandleGetObject()
 * returns W_ERROR_CONNECTION_COMPATIBILITY and the value pointed by ppObject is set to null whatever
 * expected type nExpectedType value.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle value to check.
 *
 * @param[in]  pExpectedType  The expected object type.
 *
 * @param[out] ppObject  A pointer on a variable valued with the pointer on the
 *             corresponding object is the handle is valid and the object is of
 *             the expected type. Otherwise, the value is set to null.
 *
 * @return     One of the following value:
 *              - W_SUCCESS The handle is valid and of the specified type.
 *              - W_ERROR_BAD_HANDLE  The handle is not valid or null
 *              - W_ERROR_CONNECTION_COMPATIBILITY The handle is not of the specified type
 **/

W_ERROR PHandleGetConnectionObject(
            tContext * pContext,
            W_HANDLE hObject,
            tHandleType* pExpectedType,
            void**ppObject);

/**
 * Duplicates the handle on an object.
 *
 * The reference count of the object is increased by one.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  The handle value of the object.
 *
 * @param[out]  phNewHandle  A pointer on a variable valued with the new handle value.
 *             This value is set to W_NULL_HANDLE upon error.
 *
 * @return  One of the following values:
 *            - W_SUCCESS  The new handle is created.
 *            - W_ERROR_BAD_HANDLE  The handle is not valid.
 *            - W_ERROR_OUT_OF_RESOURCE  There is not enough resource to create the handle.
 **/
W_ERROR PHandleDuplicate(
            tContext* pContext,
            W_HANDLE hObject,
            W_HANDLE* phNewHandle );

/**
 * Duplicates the handle on an object.
 *
 * The new handle is "weak", i.e. the reference count of the object is not increased.
 * The new handle value may become invalid at any time if the object is destroyed.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  The handle value of the object.
 *
 * @param[out]  phNewHandle  A pointer on a variable valued with the new handle value.
 *             This value is set to W_NULL_HANDLE upon error.
 *
 * @return  One of the following values:
 *            - W_SUCCESS  The new handle is created.
 *            - W_ERROR_BAD_HANDLE  The handle is not valid.
 *            - W_ERROR_OUT_OF_RESOURCE  There is not enough resource to create the handle.
 **/
W_ERROR PHandleDuplicateWeak(
            tContext* pContext,
            W_HANDLE hObject,
            W_HANDLE* phNewHandle );

/**
 * Closes the handle on an object.
 *
 * The handle is removed from the registry.
 * The reference count of the object is decreased by one. If the reference count
 * is zero, the object is destroyed.
 *
 * @pre  The handle value should be valid. the handle value should have been checked with
 *       PHandleGetObject() before calling PHandleClose().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle on the object.
 **/
void PHandleClose(
            tContext* pContext,
            W_HANDLE hObject );

/**
 * Closes the handle on an object.
 *
 * This function is the asynchronous version of PHandleClose() waiting for
 * the completion of the destroy process before calling the callback function.
 *
 * The handle is removed from the registry.
 * The reference count of the object is decreased by one. If the reference count
 * is zero, the object is destroyed.
 *
 * @pre  The handle value should be valid. the handle value should have been checked with
 *       PHandleGetObject() before calling PHandleCloseSafe().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle on the object.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 **/
 void PHandleCloseSafe(
            tContext * pContext,
            W_HANDLE hObject,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * Returns the type of a handle
 *
 * For monolithic porting,  always return W_TRUE since there's no user / kernel separation
 * For user/kernel porting, returns W_TRUE if the handle belongs to the user handle range
 *                          returns W_FALSE if the handle belongs to the kernel handle range
 *
 * remark  No check is done on the handle validity.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle on the object.

 * @return W_TRUE / W_FALSE
 *
 **/
bool_t PHandleIsUser(
            tContext * pContext,
            W_HANDLE hObject);


/**
 * Returns the number of open handles.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pnUserHandleNumber  A pointer on a variable valued with the
 *             number of user handles.
 *
 * @param[in]  pnDriverHandleNumber  A pointer on a variable valued with the
 *             number of driver handles.
 **/
void PHandleGetCount(
         tContext* pContext,
         uint32_t* pnUserHandleNumber,
         uint32_t* pnDriverHandleNumber);

/**
 * Decrements the reference count of an object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]   pObject  The object.
 **/
void PHandleDecrementReferenceCount(
            tContext* pContext,
            void* pObject );

/**
 * Increments the reference count of an object.
 *
 * @param[in]   pObject  The object.
 **/
void PHandleIncrementReferenceCount(
            void* pObject );

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
/**
 * Closes all the handles created for the current client.
 *
 * The handles are removed from the registry.
 * The reference count of the objects are decreased by one. If the reference count
 * is zero, the object are destroyed.
 *
 * @param[in]  pContext  The context.
 *
 * param[in]  pUserInstance  The user instance
 **/
void PHandleCloseAll(
            tContext* pContext,
            tUserInstance* pUserInstance);
#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See WReaderGetIdentifier */
W_ERROR PHandleGetIdentifier(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t* pIdentifierBuffer,
         uint32_t nIdentifierBufferMaxLength,
         uint32_t* pnIdentifierActualLength);

/* See WReaderExchangeDataEx */
void PHandleExchangeData(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t  nPropertyIdentifier,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pReaderToCardBuffer,
         uint32_t nReaderToCardBufferLength,
         uint8_t* pCardToReaderBuffer,
         uint32_t nCardToReaderBufferMaxLength,
         W_HANDLE* phOperation );

/**
 * Returns the number of all object properties (visible and internal).
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hObject  The object handle.
 *
 * @param[out]  pnPropertyNumber  A pointer on a variable set with the number of properties.
 *              The value is set to zero in case of error.
 *
 * @param[in]   bOnlyVisible  W_TRUE to retrieve only the visible properties (internal properties are filtered).
 *                            W_FALSE to retrieve all visible and internal properties.
 *
 * @retval  W_SUCCESS  The properties are returned.
 * @retval  W_ERROR_BAD_HANDLE If the value of hObject is not a valid handle.
 * @retval  W_ERROR_BAD_STATE If an error is returned by the get property function of hObject.
 * @retval  W_ERROR_OUT_OF_RESOURCE Not enough memory.
 * @retval  W_ERROR_CONNECTION_COMPATIBILITY The handle is not a connection handle.
 **/
W_ERROR PHandleGetAllPropertyNumber(
            tContext* pContext,
            W_HANDLE hObject,
            uint32_t* pnPropertyNumber,
            bool_t bOnlyVisible);

/**
 * Returns the number of object properties.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hObject  The object handle.
 *
 * @param[out]  pnPropertyNumber  A pointer on a variable set with the number of properties.
 *              The value is set to zero in case of error.
 *
 * @retval  W_SUCCESS  The properties are returned.
 * @retval  W_ERROR_BAD_HANDLE If the value of hObject is not a valid handle.
 * @retval  W_ERROR_BAD_STATE If an error is returned by the get property function of hObject.
 * @retval  W_ERROR_OUT_OF_RESOURCE Not enough memory.
 * @retval  W_ERROR_CONNECTION_COMPATIBILITY The handle is not a connection handle.
 **/
W_ERROR PHandleGetPropertyNumber(
            tContext* pContext,
            W_HANDLE hObject,
            uint32_t* pnPropertyNumber);

/**
 * Returns the value of the object properties.
 *
 * The number of properties is given by PHandleGetPropertyNumber().
 *
 * If the object has no properties, the content of the array is left unchanged.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hObject  The object handle.
 *
 * @param[out]  pPropertyArray  A pointer on a buffer receiving the property values.
 *              The content of the array is left unchanged if an error is returned.
 *
 * @param[in]   nPropertyArrayLength The length of pPropertyArray
 *
 * @param[in]   bOnlyVisible  W_TRUE to retrieve only the visible properties (internal properties are filtered).
 *                            W_FALSE to retrieve all visible and internal properties.
 *
 * @retval  W_SUCCESS  The properties are returned.
 * @retval  W_ERROR_BAD_HANDLE If the value of hObject is not a valid handle.
 * @retval  W_ERROR_BAD_STATE If an error is returned by the get property function of hObject.
 * @retval  W_ERROR_OUT_OF_RESOURCE Not enough memory.
 * @retval  W_ERROR_CONNECTION_COMPATIBILITY The handle is not a connection handle.
 **/
W_ERROR PHandleGetAllProperties(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t* pPropertyArray,
            uint32_t nPropertyArrayLength,
            bool_t bOnlyVisible);

/**
 * Returns the value of the object properties.
 *
 * The number of properties is given by PHandleGetPropertyNumber().
 *
 * If the object has no properties, the content of the array is left unchanged.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hObject  The object handle.
 *
 * @param[out]  pPropertyArray  A pointer on a buffer receiving the property values.
 *              The content of the array is left unchanged if an error is returned.
 *
 * @param[in]   nPropertyArrayLength The length of pPropertyArray
 *
 * @retval  W_SUCCESS  The properties are returned.
 * @retval  W_ERROR_BAD_HANDLE If the value of hObject is not a valid handle.
 * @retval  W_ERROR_BAD_STATE If an error is returned by the get property function of hObject.
 * @retval  W_ERROR_OUT_OF_RESOURCE Not enough memory.
 * @retval  W_ERROR_CONNECTION_COMPATIBILITY The handle is not a connection handle.
**/
W_ERROR PHandleGetProperties(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t* pPropertyArray,
            uint32_t nPropertyArrayLength);

/**
 * Checks the presence of an object property.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hObject  The object handle.
 *
 * @param[in]   nPropertyValue  The value of the property to check.
 *
 * @retval  W_SUCCESS  The property is present.
 * @retval  W_ERROR_ITEM_NOT_FOUND If the specified property is not found.
 * @retval  W_ERROR_BAD_HANDLE If the value of hObject is not a valid handle.
 * @retval  W_ERROR_CONNECTION_COMPATIBILITY The handle is not a connection handle.
 **/
W_ERROR PHandleCheckProperty(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t nPropertyValue);

/**
 * Returns if polling functions are defined for this connection and if card removal detection is possible.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hConnection  The connection handle.
 *
 * @retval  W_TRUE  The polling functions are defined and card removal detection is possible.
 */
bool_t PHandleIsPollingSupported(
            tContext* pContext,
            W_HANDLE hConnection);

/**
 * Executes the polling function.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   hConnection  The connection handle.
 *
 * @param[in]   pCallback  The callback function.
 *
 * @param[in]   pCallbackParameter The callback parameter.
 */
void PHandlePoll(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction * pCallback,
            void * pCallbackParameter);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_HANDLE_H */
