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

/**
 * @hide
 */
 
interface IOpenNfcExtCardEmulationEventHandler { 
	
	/**
     * Method called when an event is received from a reader.
     *
     * @param event the event value: {@link #SELECTION} or {@link #DEACTIVATE}.
     */
    void onEventReceived(int event);

    /**
     * Method called when a command is received from a reader.
     *
     * Use  {@link CardEmulation#sendResponse CardEmulation.sendResponse()}
     * to send an answer to the reader.
     *
     * @param command the command received from the reader.
     */
    void onCommandReceived(in byte[] command);
}