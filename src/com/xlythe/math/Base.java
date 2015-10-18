package com.xlythe.math;

/**
 * Represents changing the number of characters available when writing numbers.
 */
public enum Base {
    BINARY(2),
    DECIMAL(10),
    HEXADECIMAL(16);

    int quickSerializable;

    Base(int num) {
        this.quickSerializable = num;
    }

    public int getQuickSerializable() {
        return quickSerializable;
    }
}
