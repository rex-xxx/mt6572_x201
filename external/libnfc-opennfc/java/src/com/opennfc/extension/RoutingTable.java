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
 * The class to represent a Routing Table.
 * The routing table determines the routing target of commands coming from an external reader when the system is in card
 * emulation mode.
 * <p>
 * <p>The new RoutingTable can be created by calling {@link RoutingTableManager#createTable()}
 * <p>The current RoutingTable that is used by the NFC Controller can be retrieved by calling {@link RoutingTableManager#readTable()}
 * <p>After finishing operations with a routing table a user application MUST call {@link RoutingTable#close()}
 * @see
 * <p>{@link RoutingTableManager}
 * <p>{@link RoutingTableEntry}
 */
public final class RoutingTable {

	/** Enable/disable debug */
	private static final boolean DEBUG = true;
	
	/** Tag use in debug */
	private static final String TAG = RoutingTable.class.getSimpleName();

	/** Open NFC Routing Table's handle */
	private int handle;
	
	private RoutingTableManager manager;

	/**
	 * Creates the RoutingTable instance
	 * @param manager the Routing Table Manager instance that should be used to access the Routing Table API
	 * @param handle Open NFC's handle for the routing table
	 */
	RoutingTable(int handle, RoutingTableManager manager) {
		this.handle = handle;
		this.manager = manager;
	}
	
	/**
	 * Writes a routing table to the NFC controller
	 * @throws RoutingTableException if can't write the routing table to the NFC controller
	 */
	public void apply() throws RoutingTableException {
			manager.applyTable(handle);
	}

	/**
	 * Returns the entries of the routing table
	 * @return the array of the entries of the routing table
	 */
	public RoutingTableEntry[] getEntries() throws RoutingTableException {
		return manager.getTableEntries(handle);
	}

	/** Resetting the contents of the routing table  */
	final private static int RESET_TABLE_CMD = ConstantAutogen.W_ROUTING_TABLE_OPERATION_RESET;  
	
	/** Appending an entry to a routing table  */
	final private static int APPEND_ENTRY_CMD = ConstantAutogen.W_ROUTING_TABLE_OPERATION_APPEND;  

	/** Inserting an entry into a routing table */
	final private static int INSERT_ENTRY_CMD = ConstantAutogen.W_ROUTING_TABLE_OPERATION_INSERT;  

	/** Deleting a routing table entry */
	final private static int DELETE_ENTRY_CMD = ConstantAutogen.W_ROUTING_TABLE_OPERATION_DELETE;  
			
	/** Moving a routing table entry */
	final private static int MOVE_ENTRY_CMD = ConstantAutogen.W_ROUTING_TABLE_OPERATION_MOVE_CMD;  
	
	/**
	 * Appends a routing table entry to the end of the routing table
	 * @param newEntry an entry to add to the routing table
	 * @throws RoutingTableException if the entry can't be appended
	 */
	public void appendEntry(RoutingTableEntry newEntry) throws RoutingTableException {
		manager.modifyTable(handle, APPEND_ENTRY_CMD, 0, newEntry);
	}

	/**
	 * Inserts a routing table entry at the position specified in the index parameter
	 * 
	 * @param index a position to insert the routing table entry at
	 * @param newEntry the routing table entry to insert
	 * @throws RoutingTableException if if the entry can't be inserted at the position
	 */
	public void insertEntry(int index, RoutingTableEntry newEntry) throws RoutingTableException {
		manager.modifyTable(handle, INSERT_ENTRY_CMD, index, newEntry);
	}

	/**
	 * Resets the contents of the routing table, i.e. removes all entries of the routing table.
	 * 
	 * @param index a position to insert the routing table entry at
	 * @param newEntry the routing table entry to insert
	 * @throws RoutingTableException if if the entry can't be inserted at the position
	 */
	public void reset() throws RoutingTableException {
		manager.modifyTable(handle, RESET_TABLE_CMD, 0, null);
	}

	/**
	 * Deletes a routing table entry at the specified position
	 * @param index  a position of the entry to be removed 
	 * @throws RoutingTableException if if the entry can't be deleted
	 */
	public void deleteEntry(int index) throws RoutingTableException {
		manager.modifyTable(handle, DELETE_ENTRY_CMD, index, null);
	}

	/**
	 * Moves a routing table entry at the position specified in the index parameter
	 * Entries between the old and new indexes are shifted accordingly.
	 * 
	 * @param oldIndex a position of the routing table entry to be moved
	 * @param newIndex a new position of the routing table entry
	 * @throws RoutingTableException if if the entry can't be moved
	 */
	public void moveEntry(int oldIndex, int newIndex) throws RoutingTableException {
		manager.modifyTable(handle, getMoveOperationValue(newIndex), oldIndex, null);
	}

	/**
	 * Gets the value for the operation parameter for OpenNFC modify command to move a routing table entry
	 * @param newIndex a new position of the routing table entry
	 * @return the operation parameter for OpenNFC modify command
	 */
	private int getMoveOperationValue(int newIndex) {
		return MOVE_ENTRY_CMD | newIndex;
	}
	
	/**
	 * Closes access to the routing table. 
	 * This method must be called by an application when it finishes using the routing table.
	 * @throws RoutingTableException if can't close the routing table
	 */
	public void close() throws RoutingTableException {
		manager.closeTable(handle);
	}
}