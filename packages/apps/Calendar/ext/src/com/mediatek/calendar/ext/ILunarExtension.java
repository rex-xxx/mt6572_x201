package com.mediatek.calendar.ext;

public interface ILunarExtension {

    /**
     * M: get the 1-based index number of the solar term name.
     * @param index from the 1st to 24th
     * @return the string
     */
    String getSolarTermNameByIndex(int index);

    /**
     * M: Check whether the updated date is lunar festival
     * if is festival, return the text, otherwise return null
     * 
     * @param lunarMonth
     * @param lunarDay
     * @return null for not the festival. if it's the festival
     * return the festival String
     */
    String getLunarFestival(int lunarMonth, int lunarDay);

    /**
     * M: get Gregorian festival.
     * @param gregorianMonth gregorian month
     * @param gregorianDay gregorian day
     * @return null for not found
     */
    String getGregFestival(int gregorianMonth, int gregorianDay);

    /**
     * M: Get the special word, If local language is SC, return the needed word in SC,
     * else return the TC word.
     * @param index refer to the special word.
     * @return the word needed
     */
    String getSpecialWord(int index);

    /**
     * M: Whether luanr can be shown in current system env
     * @return true if yes.
     */
    boolean canShowLunarCalendar();

}
