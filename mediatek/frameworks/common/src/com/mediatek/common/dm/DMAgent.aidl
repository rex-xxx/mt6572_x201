package com.mediatek.common.dm;

interface DMAgent {
    byte[] readDMTree();
    boolean writeDMTree(in byte[] tree);
    boolean isLockFlagSet();
    boolean setLockFlag(in byte[] lockType);
    boolean clearLockFlag();
    byte[] readIMSI();
    boolean writeIMSI(in byte[] imsi);
    byte[] readOperatorName();
    byte[] readCTA();
    boolean writeCTA(in byte[] cta);

    boolean setRebootFlag();
    int getLockType();
    int getOperatorID();
    byte[] getOperatorName();
    boolean isHangMoCallLocking();
    boolean isHangMtCallLocking();
    boolean clearRebootFlag();
    boolean isBootRecoveryFlag();
    int getUpgradeStatus();
    int restartAndroid();
    boolean isWipeSet();
    boolean setWipeFlag();
    boolean clearWipeFlag();
    int readOtaResult();
    int clearOtaResult();
}
