package com.android.exchange;

import com.android.exchange.CommandStatusException.CommandStatus;

import android.test.AndroidTestCase;

/**
 * M: Test the CommandStatusException and CommandStatusException.CommandStatus
 */
public class CommandStatusExceptionTest extends AndroidTestCase {

    public void testIsNeedsProvisioning() {
        CommandStatusException ex = new CommandStatusException(CommandStatus.NEEDS_PROVISIONING);
        assertTrue(CommandStatus.isNeedsProvisioning(ex.mStatus));
        ex = new CommandStatusException(CommandStatus.NEEDS_PROVISIONING_WIPE, "1");
        assertTrue(CommandStatus.isNeedsProvisioning(ex.mStatus));
        assertFalse(CommandStatus.isNeedsProvisioning(CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE));
        assertFalse(CommandStatus.isNeedsProvisioning(CommandStatus.ACCESS_DENIED));
    }

    public void testIsBadSyncKey() {
        CommandStatusException ex = new CommandStatusException(CommandStatus.SYNC_STATE_CORRUPT);
        assertTrue(CommandStatus.isBadSyncKey(ex.mStatus));
        ex = new CommandStatusException(CommandStatus.SYNC_STATE_INVALID, null);
        assertTrue(CommandStatus.isBadSyncKey(ex.mStatus));
        assertFalse(CommandStatus.isBadSyncKey(CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE));
        assertFalse(CommandStatus.isBadSyncKey(CommandStatus.ACCESS_DENIED));
    }

    public void testIsDeniedAccess() {
        CommandStatusException ex = new CommandStatusException(CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE);
        assertTrue(CommandStatus.isDeniedAccess(ex.mStatus));
        ex = new CommandStatusException(CommandStatus.ACCESS_DENIED, null);
        assertTrue(CommandStatus.isDeniedAccess(ex.mStatus));
        assertTrue(CommandStatus.isDeniedAccess(CommandStatus.USERS_DISABLED_FOR_SYNC));
        assertTrue(CommandStatus.isDeniedAccess(CommandStatus.TOO_MANY_PARTNERSHIPS));
        assertFalse(CommandStatus.isDeniedAccess(CommandStatus.SYNC_STATE_CORRUPT));
        assertFalse(CommandStatus.isDeniedAccess(CommandStatus.NEEDS_PROVISIONING));
    }

    public void testIsTransientError() {
        CommandStatusException ex = new CommandStatusException(CommandStatus.SYNC_STATE_NOT_FOUND);
        assertTrue(CommandStatus.isTransientError(ex.mStatus));
        ex = new CommandStatusException(CommandStatus.SERVER_ERROR_RETRY, null);
        assertTrue(CommandStatus.isTransientError(ex.mStatus));
        assertFalse(CommandStatus.isTransientError(CommandStatus.USERS_DISABLED_FOR_SYNC));
        assertFalse(CommandStatus.isTransientError(CommandStatus.ACCESS_DENIED));
    }

    public void testToString() {
        assertEquals(CommandStatus.ACCESS_DENIED + " (AccessDenied)",
                CommandStatus.toString(CommandStatus.ACCESS_DENIED));
        assertEquals(CommandStatus.SYNC_STATE_NOT_FOUND + " (SyncStateNF)",
                CommandStatus.toString(CommandStatus.SYNC_STATE_NOT_FOUND));
        assertEquals(CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE + " (LegacyDevice)",
                CommandStatus.toString(CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE));
        assertEquals(CommandStatus.SYNC_STATE_CORRUPT + " (SyncStateCorrupt)",
                CommandStatus.toString(CommandStatus.SYNC_STATE_CORRUPT));
        assertEquals(CommandStatus.NEEDS_PROVISIONING + " (NotProvisioned)",
                CommandStatus.toString(CommandStatus.NEEDS_PROVISIONING));

        assertNotSame(CommandStatus.ACCESS_DENIED + " (AccessDenied)",
                CommandStatus.toString(CommandStatus.DEVICE_QUARANTINED));
        assertNotSame(CommandStatus.SYNC_STATE_NOT_FOUND + " (SyncStateNF)",
                CommandStatus.toString(CommandStatus.SYNC_STATE_EXISTS));
        assertNotSame(CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE + " (LegacyDevice)",
                CommandStatus.toString(CommandStatus.SYNC_STATE_CORRUPT));
        assertNotSame(CommandStatus.WTF_DEVICE_CLAIMS_EXTERNAL_MANAGEMENT + " (SyncStateCorrupt)",
                CommandStatus.toString(CommandStatus.NEEDS_PROVISIONING));
        assertNotSame(CommandStatus.WTF_REQUIRES_PROXY_WITHOUT_SSL + " (NotProvisioned)",
                CommandStatus.toString(CommandStatus.WTF_REQUIRES_PROXY_WITHOUT_SSL));

        assertEquals("0 (unknown)",
                CommandStatus.toString(0));
        assertNotSame(CommandStatus.ITEM_NOT_FOUND + " (unknown)",
                CommandStatus.toString(CommandStatus.ITEM_NOT_FOUND));
        assertEquals(CommandStatus.TOO_MANY_PARTNERSHIPS + " (unknown)",
                CommandStatus.toString(CommandStatus.TOO_MANY_PARTNERSHIPS));
        assertEquals("100 (unknown)", CommandStatus.toString(100));
    }
}
