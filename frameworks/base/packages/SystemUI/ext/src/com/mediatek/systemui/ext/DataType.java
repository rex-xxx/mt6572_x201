package com.mediatek.systemui.ext;

/**
 * M: This enum defines the type of data connection type.
 */
public enum DataType {

    Type_1X(0), Type_3G(1), Type_4G(2), Type_E(3), Type_G(4), Type_H(5), Type_H_PLUS(6);

    private int mTypeId;

    private DataType(int typeId) {
        mTypeId = typeId;
    }

    public int getTypeId() {
        return mTypeId;
    }

}
