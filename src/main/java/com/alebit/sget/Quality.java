package com.alebit.sget;

public enum Quality {
    NONE(0),
    MIN(-1),
    MAX(-2);

    private int value;

    Quality(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
