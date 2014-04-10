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

package com.opennfc.extension;

/**
 * An application implements this interface to receive notifications from a
 * {@link VirtualTag}. This instance of event handler is registered with a
 * Virtual Tag using the method {@link VirtualTag#start VirtualTag.start()}.
 * 
 * @since Open NFC 4.0
 */
public interface VirtualTagEventHandler {

	/** The tag is selected by a remote reader. */
	static final int EVENT_SELECTION = ConstantAutogen.W_VIRTUAL_TAG_EVENT_SELECTION;

	/**
	 * The remote reader deactivated the tag without reading or writing the
	 * message.
	 */
	static final int EVENT_READER_LEFT = ConstantAutogen.W_VIRTUAL_TAG_EVENT_READER_LEFT;

	/** The remote reader deactivated the tag after reading the message. */
	static final int EVENT_READER_READ_ONLY = ConstantAutogen.W_VIRTUAL_TAG_EVENT_READER_READ_ONLY;

	/** The remote reader deactivated the tag after writing a new the message. */
	static final int EVENT_READER_WRITE = ConstantAutogen.W_VIRTUAL_TAG_EVENT_READER_WRITE;

	/**
	 * Event handler method receiving the Virtual Tag events.
	 * 
	 * @param event
	 *            the event detected.
	 */
	void onTagEventDetected(int event);

}