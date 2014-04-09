package com.mediatek.calendar.ext;

public class DefaultLunarExtension implements ILunarExtension {

    @Override
    public String getLunarFestival(int lunarMonth, int lunarDay) {
        return null;
    }

    @Override
    public String getSolarTermNameByIndex(int index) {
        return null;
    }

    @Override
    public String getGregFestival(int gregorianMonth, int gregorianDay) {
        return null;
    }

    @Override
    public String getSpecialWord(int index) {
        return null;
    }

    @Override
    public boolean canShowLunarCalendar() {
        return false;
    }

}
