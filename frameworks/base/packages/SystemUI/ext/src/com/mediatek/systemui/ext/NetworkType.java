package com.mediatek.systemui.ext;

/**
 * M: This enum defines the type of network type.
 */
public enum NetworkType {

    Type_G(0), Type_3G(1), Type_1X(2), Type_1X3G(3);

    private int mTypeId;

    private NetworkType(int typeId) {
        mTypeId = typeId;
    }

    public int getTypeId() {
        return mTypeId;
    }

}
