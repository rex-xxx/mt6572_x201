/**
 Copyright (C) 2012 Inside Contactless

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

#ifndef	OPEN_NFC_EXTENSION_H
#	define	OPEN_NFC_EXTENSION_H

#define NFC_EXT_SOCKET "nfcext"

#include <unistd.h>
#include <stdint.h>
#include <open_nfc.h>
#include <pthread.h>
#include <utilities.h>

#include <JNIHelp.h>
#include <jni.h>

#define TRANSCEIVE_MAX_BUFFER_SIZE 1024
#define RETURN_MESSAGE_MAX_BUFFER_SIZE 1024
#define MAXIMUM_READ_NDEF_BUFFER_SIZE 1024*512

int initalize_com_opennfc_extension_engine_SecureElementAdapter(JNIEnv * environmentJNI);
void deinitalize_com_opennfc_extension_engine_SecureElementAdapter(JNIEnv * environmentJNI);

int initalize_com_opennfc_extension_engine_FirmwareUpdateAdapter(JNIEnv * environmentJNI);
void deinitalize_com_opennfc_extension_engine_FirmwareUpdateAdapter(JNIEnv * environmentJNI);

JNIEnv* attachJniEnv();
void detachJniEnv();

int initalize_com_opennfc_extension_engine_CardEmulationAdapter(JNIEnv * environmentJNI);
void deinitalize_com_opennfc_extension_engine_CardEmulationAdapter(JNIEnv * environmentJNI);

int initalize_com_opennfc_extension_engine_RoutingTableAdapter(JNIEnv * environmentJNI);
void deinitalize_com_opennfc_extension_engine_RoutingTableAdapter(JNIEnv * environmentJNI);

JNIEnv* attachJniEnv();
void detachJniEnv();

#endif	//OPEN_NFC_EXTENTION
