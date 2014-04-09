package com.android.internal.telephony;

public class EtwsNotification {
    public int warningType;
    public int messageId;
    public int serialNumber;
    public String plmnId;
    public String securityInfo;
    
    public String toString() {
        return "EtwsNotification: " + warningType + ", " + messageId + ", " + serialNumber
                + ", " + plmnId + ", " + securityInfo;
    }
    
    public boolean isDuplicatedEtws(EtwsNotification other) {
        if(this.warningType == other.warningType
                && this.messageId == other.messageId
                && this.serialNumber == other.serialNumber
                && this.plmnId.equals(other.plmnId)) {
            return true;
        }
        
        return false;
    }
    
    public byte[] getEtwsPdu() {
    	byte[] etwsPdu = new byte[56];
    	byte[] serialNumberBytes = EtwsUtils.intToBytes(serialNumber);
    	System.arraycopy(serialNumberBytes, 2, etwsPdu, 0, 2);
    	byte[] messageIdBytes = EtwsUtils.intToBytes(messageId);
    	System.arraycopy(messageIdBytes, 2, etwsPdu, 2, 2);
    	byte[] warningTypeBytes = EtwsUtils.intToBytes(warningType);
    	System.arraycopy(warningTypeBytes, 2, etwsPdu, 4, 2);
    	if(securityInfo != null) {
    		System.arraycopy(IccUtils.hexStringToBytes(securityInfo), 0, etwsPdu, 6, 50);
    	}
    	
    	return etwsPdu;
    }
}