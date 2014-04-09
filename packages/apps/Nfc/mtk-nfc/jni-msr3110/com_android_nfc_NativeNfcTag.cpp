/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modified by: Inside Secure
 */

#include <semaphore.h>
#include <errno.h>

#include "com_android_nfc.h"

#define TRANSCEIVE_MAX_BUFFER_SIZE 					1024
#define W_ERROR_FAILED 								0x000000FF

namespace android {

extern void setInitialTag();
extern void CloseRemovalHandle();
extern void CloseConnectionHandle();
extern bool get_isCheckNdefMessageOK();
extern bool get_isMifareDesfire();
extern bool set_isCheckNdefMessageOK(bool CheckNdefMessageOK);
extern int  get_tagAccessStatus();
extern int  get_maxNdefMessageLength();
extern bool get_presentTag();
extern techListForTransceive get_tTransceive();
extern uint8_t * getNdefMessagebuffer();
extern uint32_t getNdefMessageLength();


/**
 * Get Ndef message already read
 */
static jbyteArray com_android_nfc_NativeNfcTag_doRead(JNIEnv *e,
   jobject o)
{
	   LogInformation("com_android_nfc_NativeNfcTag_doRead");
	 jbyteArray buf = NULL;
	 buf = e->NewByteArray(getNdefMessageLength());
	 e->SetByteArrayRegion(buf, 0, getNdefMessageLength(),
	      (jbyte *)getNdefMessagebuffer());
	 return buf;
}

/**
 * Do write Ndef message
 */
static jboolean com_android_nfc_NativeNfcTag_doWrite(JNIEnv *e,
   jobject o, jbyteArray buf)
{
	W_HANDLE msg_handle = W_NULL_HANDLE;
	W_ERROR result = W_ERROR_TAG_WRITE;
	uint32_t handle = nfc_jni_get_connected_handle(e, o);

	uint32_t ndefMessageLength = (uint32_t)e->GetArrayLength(buf);
	uint8_t *buffer = (uint8_t *)e->GetByteArrayElements(buf, NULL);

	if(ndefMessageLength==0){
		LogError("com_android_nfc_NativeNfcTag_doWrite: Nothing to write");
		return JNI_FALSE;
	}else{
		   LogInformation(" com_android_nfc_NativeNfcTag_doWrite");
	    WNDEFBuildMessage(buffer, ndefMessageLength, &msg_handle);

	}

	result = WNDEFWriteMessageSync (handle, msg_handle, W_NDEF_ACTION_BIT_FORMAT_NON_NDEF_TAG | W_NDEF_ACTION_BIT_ERASE);
	if (result == W_ERROR_RF_COMMUNICATION)
		{
		setInitialTag();
		}
	    LogInformation(" com_android_nfc_NativeNfcTag_doWrite exit with result = %d", result);
	 e->ReleaseByteArrayElements(buf, (jbyte *)buffer, JNI_ABORT);
	 
	 if (msg_handle != W_NULL_HANDLE)
	 	WBasicCloseHandle(msg_handle);
	 
	return (result==W_SUCCESS ? JNI_TRUE:JNI_FALSE);
}

/**
 * Do not use for OpenNFC
 */
static jint com_android_nfc_NativeNfcTag_doConnect(JNIEnv *e,
   jobject o, jint handle)
{
	return 0;
}

/**
 * Do not use for OpenNFC
 */
static jint com_android_nfc_NativeNfcTag_doHandleReconnect(JNIEnv *e,
   jobject o, jint handle)
{
	return 0;
}

/**
 * Do not use for OpenNFC
 */
static jint com_android_nfc_NativeNfcTag_doReconnect(JNIEnv *e,
   jobject o)
{
    return 0;
}


static jboolean com_android_nfc_NativeNfcTag_doDisconnect(JNIEnv *e, jobject o)
{
	CloseRemovalHandle();
	CloseConnectionHandle();
	setInitialTag();
	return JNI_TRUE;
}

/**
 * Do Transceive
 */
static jbyteArray com_android_nfc_NativeNfcTag_doTransceive(JNIEnv *e,
   jobject o, jbyteArray data, jboolean raw, jintArray statusTargetLost)
{
	uint8_t *dataArray= NULL;
	uint32_t dataLen;
    uint8_t *transceiveResult= (uint8_t*)malloc(1024);
	uint32_t transceiveResultLen = 0;
    jbyteArray result = NULL;
    uint32_t handle;
    W_ERROR transceiveStatus = W_ERROR_FAILED;
    techListForTransceive tTransceive;
    bool isMifareDesfire;
    jint *targetLost;
    int cmdFelica;

    if (statusTargetLost != NULL) {
        targetLost = e->GetIntArrayElements(statusTargetLost, 0);
        if (targetLost != NULL) {
            *targetLost = 0;
        }
    } else {
        targetLost = NULL;
    }

    handle = nfc_jni_get_connected_handle(e, o);
       LogInformation("com_android_nfc_NativeNfcTag_doTransceive handle = %d", handle);
	dataArray = (uint8_t *)e->GetByteArrayElements(data, NULL);
	dataLen= (uint32_t)e->GetArrayLength(data);
	tTransceive = get_tTransceive();
	isMifareDesfire = get_isMifareDesfire();

	if(handle == 0)
	{
		transceiveStatus = W_ERROR_BAD_HANDLE;
		goto END;
	}

	if(tTransceive == T_NOT_SUPPORT_TYPE){
		   LogInformation("case T_NOT_SUPPORT_TYPE");
		setInitialTag();
		//WBasicCloseHandle(handle);
		CloseRemovalHandle();
		CloseConnectionHandle();
		goto END;
	}

	if(dataArray!=NULL){
		char *uid = dumpUnit8Array(dataArray, 5);
		LogInformation( "Transceive data: %s", uid);
		free(uid);
		}

   switch (tTransceive)
   {
	case T_W_PROP_MIFARE:
	  if((raw == true)||(dataArray[0]==(uint8_t) 0x00)){
		    LogInformation("Transceive T_W_PROP_MIFARE Raw");
		  transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
				  transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
	  }
	  else{
		    LogInformation("Transceive T_W_PROP_MIFARE NOT Raw");
			switch (dataArray[0]) {
				case (uint8_t) 0x30: // Read Mode
						if(dataLen>=2){
						transceiveStatus = WMifareReadSync(handle, transceiveResult, (uint32_t) ((dataArray[1] & 0xFF) * 4), (uint32_t) 16);
						transceiveResultLen = 16;
						}
						break;
				case (uint8_t) 0xA2: // Write mode 4 bytes
						if(dataLen >= 2+4){ // should >= (2 head + 4 bytes to write)
						transceiveStatus = WMifareWriteSync(handle, dataArray+2, (dataArray[1] & 0xFF) * 4, 4, W_FALSE);
						transceiveResultLen = 0;
						}
						break;
				case (uint8_t) 0xA0: // Write mode 16 bytes
						if(dataLen >= 2+16){ // should >= (2 head + 16 bytes to write)
						transceiveStatus = WMifareWriteSync(handle, dataArray+2, (dataArray[1] & 0xFF) * 4, 16, W_FALSE);
						transceiveResultLen = 0;
						}
						break;
				default:
						transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
						  transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
						break;
				}

	   }
	  break;

	case T_W_PROP_ISO_7816_4_A:
		if (isMifareDesfire)
		{
			   LogInformation("ENTER W_PROP_ISO_7816_4_A: isMifareDesfire.");

			if ((dataArray[0] == (uint8_t) 0x00) || (dataArray[0] == (uint8_t) 0x90))
			{
				   LogInformation("ENTER W_PROP_ISO_7816_4_A: 00 09 mode. Default mode.");
				transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
								  transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
			}
			else
			{
				uint32_t resLength;
				   LogInformation("ENTER W_PROP_ISO_7816_4_A: NOT 00 09 mode");
				uint8_t *backData;
				backData = (uint8_t *) malloc(TRANSCEIVE_MAX_BUFFER_SIZE);
				uint8_t *temp = (uint8_t *) malloc(dataLen + 4);
				temp[0] = (uint8_t) 0x90;
				temp[1] = (uint8_t) dataArray[0];
				temp[2] = (uint8_t) 0x00;
				temp[3] = (uint8_t) 0x00;
				memcpy(temp + 4, dataArray + 1, dataLen - 1);
				temp[dataLen + 3] = (uint8_t) 0x00;

				transceiveStatus = WReaderExchangeDataSync( handle, temp, dataLen+ 4,
						backData,TRANSCEIVE_MAX_BUFFER_SIZE, &resLength);

				*(transceiveResult) = backData[resLength - 1];
				memcpy(transceiveResult + 1, backData, resLength - 2);
				   LogInformation("OK after memcpy");
				transceiveResultLen = resLength - 1;
				free(temp);

				//  Pass dummy information
				uint8_t * desireAskDummyData = (uint8_t *) malloc(5);
				desireAskDummyData[0] = (uint8_t) 0x90;
				desireAskDummyData[1] = (uint8_t) 0xAF;
				desireAskDummyData[2] = (uint8_t) 0x00;
				desireAskDummyData[3] = (uint8_t) 0x00;
				desireAskDummyData[4] = (uint8_t) 0x00;
				while (backData[resLength - 1] == (uint8_t) 0xAF)
				{
					WReaderExchangeDataSync(handle, desireAskDummyData, 5, backData, TRANSCEIVE_MAX_BUFFER_SIZE,
							&resLength);
					   LogInformation("In cycle for passing dummy information");
				}

				free(desireAskDummyData);
				free(backData);
			}
		}
		else
		{
			   LogInformation("ENTER W_PROP_ISO_7816_4_A:  NOT MifareDesfire. Default mode.");
			transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
											  transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
		}
		break;

	case T_W_PROP_FELICA:
		   LogInformation("case T_W_PROP_FELICA");
		// check the command for the Felica card
		cmdFelica = dataArray[0];
		if ((cmdFelica <= 16) && ((cmdFelica & 0x01) == 0) && (cmdFelica != 0x0A))
		{
			transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
							   transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
		}
		break;

	case T_W_PROP_ISO_15693_3:
		   LogInformation("ENTER W_PROP_ISO_15693_3.");
		dataArray[0] |= (uint8_t) 0x02;
	case T_W_PROP_ISO_7816_4_B:
	case T_W_PROP_BPRIME:
	case T_W_PROP_ISO_14443_3_B:
	case T_W_PROP_ISO_14443_3_A:
	case T_W_PROP_ISO_14443_4_B:
	case T_W_PROP_ISO_14443_4_A:
	case T_W_PROP_NFC_TAG_TYPE_1:
	case T_W_PROP_NFC_TAG_TYPE_4_A:
	case T_W_PROP_NFC_TAG_TYPE_4_B:
	case T_W_PROP_NFC_TAG_TYPE_3:
	case T_W_PROP_TOPAZ:
		   LogInformation("ENTER one of the T_W_X mode. Default mode.");
		transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
						  transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
		break;

	case T_W_PROP_MY_D_NFC:
		   LogInformation("ENTER T_W_PROP_MY_D_NFC.");
		switch (dataArray[0])
		{
			case (uint8_t) 0x30: // Read mode
					transceiveStatus = WMyDReadSync(handle, transceiveResult, (uint32_t) ((dataArray[1] & 0xFF) * 4), (uint32_t) 16);
					transceiveResultLen = 16;
					break;
			case (uint8_t) 0xA2: // Write mode 4 bytes
					if(dataLen >= 2+4){ // should >= (2 head + 4 bytes to write)
					transceiveStatus = WMyDWriteSync(handle, dataArray+2, (dataArray[1] & 0xFF) * 4, 4, W_FALSE);
					transceiveResultLen = 0;
					}
					break;
			case (uint8_t) 0xA0: // Write mode 16 bytes
					if(dataLen >= 2+16){ // should >= (2 head + 16 bytes to write)
					transceiveStatus = WMyDWriteSync(handle, dataArray+2, (dataArray[1] & 0xFF) * 4, 16, W_FALSE);
					transceiveResultLen = 0;
					}
					break;
			default:
				   LogInformation("ENTER T_W_PROP_MY_D_NFC: Default mode.");
				transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
								transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
				break;
		}

   default:
			   LogInformation("ENTER Transceive default case.");
			transceiveStatus = WReaderExchangeDataSync(handle, dataArray, dataLen,
							transceiveResult,TRANSCEIVE_MAX_BUFFER_SIZE, &transceiveResultLen);
			break;

   }
   	   if(transceiveStatus == W_ERROR_RF_COMMUNICATION){
   		  setInitialTag();
   	   }

END:
		if(transceiveStatus != W_SUCCESS){
			transceiveResultLen = 0;
			if ((targetLost != NULL) && (transceiveStatus == W_ERROR_BAD_HANDLE)) {
		          *targetLost = 1;
		      }
			}
		else{
		 result = e->NewByteArray(transceiveResultLen);
		 if (result != NULL) {
		      e->SetByteArrayRegion(result, 0,transceiveResultLen,
		      (jbyte *)transceiveResult);
		 	 }
		}

		 e->ReleaseByteArrayElements(data,
		      (jbyte *)dataArray, JNI_ABORT);

		 if (targetLost != NULL) {
		        e->ReleaseIntArrayElements(statusTargetLost, targetLost, 0);
		    }

		 free (transceiveResult);
		 return result;
}

static jint com_android_nfc_NativeNfcTag_doGetNdefType(JNIEnv *e, jobject o,
        jint libnfcType, jint javaType)
{

    jint ndefType =  NDEF_UNKNOWN_TYPE;

    switch (libnfcType) {
          case phNfc_eJewel_PICC:
              ndefType = NDEF_TYPE1_TAG;
              break;
          case phNfc_eISO14443_3A_PICC:
              ndefType = NDEF_TYPE2_TAG;;
              break;
          case phNfc_eFelica_PICC:
              ndefType = NDEF_TYPE3_TAG;
              break;
          case phNfc_eISO14443_A_PICC:
          case phNfc_eISO14443_4A_PICC:
          case phNfc_eISO14443_B_PICC:
          case phNfc_eISO14443_4B_PICC:
              ndefType = NDEF_TYPE4_TAG;
              break;
          case phNfc_eMifare_PICC:
              if (javaType == TARGET_TYPE_MIFARE_UL) {
                  ndefType = NDEF_TYPE2_TAG;
              } else {
                  ndefType = NDEF_MIFARE_CLASSIC_TAG;
              }
              break;
          case phNfc_eISO15693_PICC:
              ndefType = NDEF_ICODE_SLI_TAG;
              break;
          default:
              ndefType = NDEF_UNKNOWN_TYPE;
              break;
    }
    return ndefType;

}

static jint com_android_nfc_NativeNfcTag_doCheckNdef(JNIEnv *e, jobject o, jintArray ndefinfo)
{
	if (get_isCheckNdefMessageOK()==true ){
	   LogInformation("get_isCheckNdefMessageOK, get_maxNdefMessageLength()=%d \n", get_maxNdefMessageLength());}
	else{
		   LogInformation("get_isCheckNdefMessageOK NOT \n");}

   jint status = get_isCheckNdefMessageOK()==true ? 0:-1;
   jint *ndef = e->GetIntArrayElements(ndefinfo, 0);
   ndef[0] = get_maxNdefMessageLength();
   ndef[1] = get_tagAccessStatus();
   e->ReleaseIntArrayElements(ndefinfo, ndef, 0);
   return status;
}

static jboolean com_android_nfc_NativeNfcTag_doPresenceCheck(JNIEnv *e, jobject o)
{
	 return (get_presentTag() == true ? JNI_TRUE:JNI_FALSE);

}

static jboolean com_android_nfc_NativeNfcTag_doIsIsoDepNdefFormatable(JNIEnv *e,
        jobject o, jbyteArray pollBytes, jbyteArray actBytes)
{
   return JNI_FALSE;
}

static jboolean com_android_nfc_NativeNfcTag_doNdefFormat(JNIEnv *e, jobject o, jbyteArray key)
{
	W_ERROR result;
	uint32_t handle = nfc_jni_get_connected_handle(e, o);
	   LogInformation("com_android_nfc_NativeNfcTag_doNdefFormat");
	result = WNDEFWriteMessageSync (handle, W_NULL_HANDLE, W_NDEF_ACTION_BIT_FORMAT_ALL);
	if (result == W_ERROR_RF_COMMUNICATION)
		{
		setInitialTag();
		}
	if (result == W_SUCCESS)
	{
		set_isCheckNdefMessageOK(true);
		   LogInformation("com_android_nfc_NativeNfcTag_doNdefFormat OK");
	}
	   LogInformation(" com_android_nfc_NativeNfcTag_doNdefFormat exit with result = %d", result);
	return (result==W_SUCCESS ? JNI_TRUE:JNI_FALSE);
}

static jboolean com_android_nfc_NativeNfcTag_doMakeReadonly(JNIEnv *e, jobject o, jbyteArray key)
{
		uint8_t *keyArray= NULL;
		uint32_t keyLen;
		keyLen= (uint32_t)e->GetArrayLength(key);
		if (keyLen > 0){
			return JNI_FALSE;
		}

	// if keyLen == 0, do the following. do not need key
			W_HANDLE msg_handle = W_NULL_HANDLE;
			W_ERROR  result;
			uint32_t handle = nfc_jni_get_connected_handle(e, o);
			uint32_t ndefMessageLength = getNdefMessageLength();
			if(ndefMessageLength==0){
				result = WNDEFWriteMessageSync (handle, W_NULL_HANDLE, W_NDEF_ACTION_BIT_LOCK);

			}else{
				   LogInformation(" com_android_nfc_NativeNfcTag_doMakeReadonly: phLibNfc_Ndef_Write");
			    WNDEFBuildMessage(getNdefMessagebuffer(), getNdefMessageLength(), &msg_handle);
			    result = WNDEFWriteMessageSync (handle, msg_handle, W_NDEF_ACTION_BIT_LOCK);
			    WBasicCloseHandle(msg_handle);
			}

			if (result == W_ERROR_RF_COMMUNICATION)
				{
				setInitialTag();
				}
			   LogInformation(" com_android_nfc_NativeNfcTag_doMakeReadonly exit with result = %d", result);

			return (result==W_SUCCESS ? JNI_TRUE:JNI_FALSE);
}
/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] =
{
   {"doConnect", "(I)I",
      (void *)com_android_nfc_NativeNfcTag_doConnect},
   {"doDisconnect", "()Z",
      (void *)com_android_nfc_NativeNfcTag_doDisconnect},
   {"doReconnect", "()I",
      (void *)com_android_nfc_NativeNfcTag_doReconnect},
   {"doHandleReconnect", "(I)I",
      (void *)com_android_nfc_NativeNfcTag_doHandleReconnect},
   {"doTransceive", "([BZ[I)[B",
      (void *)com_android_nfc_NativeNfcTag_doTransceive},
   {"doGetNdefType", "(II)I",
      (void *)com_android_nfc_NativeNfcTag_doGetNdefType},
   {"doCheckNdef", "([I)I",
      (void *)com_android_nfc_NativeNfcTag_doCheckNdef},
   {"doRead", "()[B",
      (void *)com_android_nfc_NativeNfcTag_doRead},
   {"doWrite", "([B)Z",
      (void *)com_android_nfc_NativeNfcTag_doWrite},
   {"doPresenceCheck", "()Z",
      (void *)com_android_nfc_NativeNfcTag_doPresenceCheck},
   {"doIsIsoDepNdefFormatable", "([B[B)Z",
      (void *)com_android_nfc_NativeNfcTag_doIsIsoDepNdefFormatable},
   {"doNdefFormat", "([B)Z",
      (void *)com_android_nfc_NativeNfcTag_doNdefFormat},
   {"doMakeReadonly", "([B)Z",
      (void *)com_android_nfc_NativeNfcTag_doMakeReadonly},
};

int register_com_android_nfc_NativeNfcTag(JNIEnv *e)
{
   return jniRegisterNativeMethods(e,
      "com/android/nfc/dhimpl/NativeNfcTag",
      gMethods, NELEM(gMethods));
}

} // namespace android
