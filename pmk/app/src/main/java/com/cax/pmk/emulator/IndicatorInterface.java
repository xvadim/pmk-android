package com.cax.pmk.emulator;

public interface IndicatorInterface {
    int registerXIndex();
    int registerYIndex();
    void displayIndicator(final int registerNum, final String text);
    boolean isYIndicatorVisible();
}
