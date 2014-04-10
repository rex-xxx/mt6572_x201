/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
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

package com.opennfc.extension.engine;
import com.opennfc.extension.CardEmulation; 
import com.opennfc.extension.engine.IOpenNfcExtCardEmulationEventHandler;
 
/**
 * @hide
 */
interface IOpenNfcExtCardEmulation
{
	int getNextIndice();
	void registerCEListener(IOpenNfcExtCardEmulationEventHandler listener, int indice);
	void unRegisterCEListener(IOpenNfcExtCardEmulationEventHandler listener, int indice);
	int emulOpenConnection(int cardType, in byte[] identifier, int randomIdentifierLength, int cardEmulationIndice);
	boolean readerIsPropertySupported(int cardType);
	int stopCardEmulation(int cardEmulationHandle);
	int sendAnswer(int handle, in byte[] response);
	boolean setCardEmulationMode(boolean enable);
	void setMappingIndiceHandle(int cardEmulationIndice, int cardEmulationHandle);
}
