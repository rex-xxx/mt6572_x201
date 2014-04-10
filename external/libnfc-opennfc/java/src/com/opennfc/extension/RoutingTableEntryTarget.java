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
 * A class that represents the destination for the Routing Table Entry which used by a Routing Table to route data 
 * to this target
 * @see
 * <p>{@link RoutingTable}
 * <p>{@link RoutingTableEntry}
 */
public abstract class RoutingTableEntryTarget {
	private int targetId;
	private String description;
	
	RoutingTableEntryTarget (int targetId, String description) {
		this.targetId = targetId;
		this.description = description;
	}
	
	/** 
	 * Gets the identifier which is used by a Routing Table to route data to this target
	 * @return the target identifier
	 */
	int getTargetId() {
		return this.targetId;
	}
	
	/**
	 * Gets the name of the target for a  Routing Table Entry
	 * @return the name of the target. It's a name of the Secure Element or the NFC Controller
	 */
	public String getTargetName() {
		return this.description;
	}

	@Override
	public String toString() {
		return "RoutingTableEntryTarget [" + description + "]";
	}
	
	
}
