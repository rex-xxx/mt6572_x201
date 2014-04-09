package com.mediatek.providers.contacts.dialersearchtestcase;


import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.android.providers.contacts.HanziToPinyin;

/**
 * Test getTokensForDialerSearch fuction with specail language
 * which on HanziToPinyin.java.
 * 
 */
@SmallTest
public class GetTokensForDialerSearchTest extends AndroidTestCase {

    private static final String TAG = "GetTokensForDialerSearchTest";

    private final String mRussianUpper = "БВАГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";
    private final String mRussianLower = "абвгдежзийклмнопрстуфхцчшщъыьэюЁяё";
    private final String mRussianCommix = "ежзГДОПРпрстуфКЛфК";
    private final String mArabic = "ذمروزهبىحغلنكدجطقخةعفاظتءثسشصض";
    private final String mNoSpaceCommix = "ДОлПРصп数ртуфхתשц联系人.?";
    private final String mSpaceCommix = "ДОлПРصп 数ртуфх תשц联系人.?";
    private final String mHebrew = "כחףזלצמקגבראעובדדנטתשס";
    private final String mChinese = "联系人数据库";
    private final String mSpecialChar = "!@#$%^&*()~_+-{}'/?;:<>\"";

    private final String mRussianUpperMapping = "22223333444455556666777788889999";
    private final String mRussianLowerMapping = "22223333444455556666777788889999";
    private final String mHebrewMapping = "5696584833739232246779";
    private final String mArabicMapping = "5857572769878569862989224444";
    private final String mSpecialCharMapping = "!@#$%^&*()~_+{}'/?;:<>\"";

    private final String[] mRussianCommixMapping = new String[] {
            "333", "23556566667", "447", "4"
    };

    private final String[] mNoSpaceCommixMapping = new String[] {
            "354", "56", "4", "5", "SHU", "66677", "77", "7", "LIAN", "XI", "REN", ".?"
    };

    private final String[] mChineseMapping = new String[] {
            "LIAN", "XI", "REN", "SHU", "JU", "KU"
    };

    private final String[] mSpaceCommix1Mapping = new String[] {
            "77", "7", "LIAN", "XI", "REN", ".?"
    };

    private final String[] mSpaceCommix2Mapping = new String[] {
            "SHU", "66677"
    };

    private final String[] mSpaceCommix3Mapping = new String[] {
            "354", "56", "4", "5"
    };

    /**
     * Test mRussianUpper result
     */
    public void test01Russian() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mRussianUpper,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        int len = mRussianUpperMapping.length();
        mShortSubStr.insert(0, mRussianUpperMapping);
        mShortSubStr.insert(0, (char) len);
        assertEquals(mSearchName.toString(), mShortSubStr.toString());
    }

    /**
     * Test mRussianLower result
     */
    public void test02Russian() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mRussianLower,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        int len = mRussianLowerMapping.length();
        mShortSubStr.insert(0, mRussianLowerMapping);
        mShortSubStr.insert(0, (char) len);
        assertEquals(mSearchName.toString(), mShortSubStr.toString());
    }

    /**
     * Test mRussianCommix result
     */
    public void test03Russian() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mRussianCommix,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        StringBuilder mShortSubStrSet = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();
        int size = mRussianCommixMapping.length;
        int len = 0;
        for (int i = size - 1; i >= 0; i--) {
            if (mShortSubStr.length() > 0) {
                mShortSubStr.deleteCharAt(0);
            }
            len += mRussianCommixMapping[i].length();
            mShortSubStr.insert(0, mRussianCommixMapping[i]);
            mShortSubStr.insert(0, (char) len);
            mShortSubStrSet.insert(0, mShortSubStr);
        }
        subStrSet.append(mShortSubStrSet);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }

    /**
     * Test mArabic result
     */
    public void test04Arabic() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mArabic,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        StringBuilder mShortSubStrSet = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();
        int size = mArabicMapping.length();
        mShortSubStr.insert(0, mArabicMapping);
        mShortSubStr.insert(0, (char) size);
        mShortSubStrSet.insert(0, mShortSubStr);
        subStrSet.append(mShortSubStrSet);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }

    /**
     * Test mHebrew result
     */
    public void test05Hebrew() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mHebrew,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        StringBuilder mShortSubStrSet = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();
        int size = mHebrewMapping.length();
        mShortSubStr.insert(0, mHebrewMapping);
        mShortSubStr.insert(0, (char) size);
        mShortSubStrSet.insert(0, mShortSubStr);
        subStrSet.append(mShortSubStrSet);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }

    /**
     * Test mChinese result
     */
    public void test06Chinese() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mChinese,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        StringBuilder mShortSubStrSet = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();
        int size = mChineseMapping.length;
        int len = 0;
        for (int i = size - 1; i >= 0; i--) {
            if (mShortSubStr.length() > 0) {
                mShortSubStr.deleteCharAt(0);
            }
            len += mChineseMapping[i].length();
            mShortSubStr.insert(0, mChineseMapping[i]);
            mShortSubStr.insert(0, (char) len);
            mShortSubStrSet.insert(0, mShortSubStr);
        }
        subStrSet.append(mShortSubStrSet);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }

    /**
     * Test mSpecialChar result
     */
    public void test07SpecialChar() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mSpecialChar,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        StringBuilder mShortSubStrSet = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();
        int size = mSpecialCharMapping.length();
        mShortSubStr.insert(0, mSpecialCharMapping);
        mShortSubStr.insert(0, (char) size);
        mShortSubStrSet.insert(0, mShortSubStr);
        subStrSet.append(mShortSubStrSet);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }

    /**
     * Test mNoSpaceCommix result
     */
    public void test08NoSpaceCommix() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mNoSpaceCommix,
                mSearchNameOffsets);

        StringBuilder mShortSubStr = new StringBuilder();
        StringBuilder mShortSubStrSet = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();
        int size = mNoSpaceCommixMapping.length;
        int len = 0;
        for (int i = size - 1; i >= 0; i--) {
            if (mShortSubStr.length() > 0) {
                mShortSubStr.deleteCharAt(0);
            }
            len += mNoSpaceCommixMapping[i].length();
            mShortSubStr.insert(0, mNoSpaceCommixMapping[i]);
            mShortSubStr.insert(0, (char) len);
            mShortSubStrSet.insert(0, mShortSubStr);
        }
        subStrSet.append(mShortSubStrSet);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }

    /**
     * Test mSpaceCommix result
     */
    public void test09SpaceCommix() {
        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance().getTokensForDialerSearch(mSpaceCommix,
                mSearchNameOffsets);

        StringBuilder mShortSubStr1 = new StringBuilder();
        StringBuilder mShortSubStrSet1 = new StringBuilder();
        StringBuilder mShortSubStr2 = new StringBuilder();
        StringBuilder mShortSubStrSet2 = new StringBuilder();
        StringBuilder mShortSubStr3 = new StringBuilder();
        StringBuilder mShortSubStrSet3 = new StringBuilder();
        StringBuilder subStrSet = new StringBuilder();

        int size3 = mSpaceCommix3Mapping.length;
        int len3 = 0;
        for (int i = size3 - 1; i >= 0; i--) {
            if (mShortSubStr3.length() > 0) {
                Log.i(TAG, "len , mSpaceCommix2Mapping[i]");
                mShortSubStr3.deleteCharAt(0);
            }
            len3 += mSpaceCommix3Mapping[i].length();
            mShortSubStr3.insert(0, mSpaceCommix3Mapping[i]);
            mShortSubStr3.insert(0, (char) len3);
            mShortSubStrSet3.insert(0, mShortSubStr3);
        }
        subStrSet.append(mShortSubStrSet3);

        int size2 = mSpaceCommix2Mapping.length;
        int len2 = 0;
        for (int i = size2 - 1; i >= 0; i--) {
            if (i != (size2 - 1) && mShortSubStr2.length() > 0) {
                Log.i(TAG, "len , mSpaceCommix2Mapping[i]");
                mShortSubStr2.deleteCharAt(0);
            }
            len2 += mSpaceCommix2Mapping[i].length();
            mShortSubStr2.insert(0, mSpaceCommix2Mapping[i]);
            mShortSubStr2.insert(0, (char) len2);
            mShortSubStrSet2.insert(0, mShortSubStr2);
        }
        subStrSet.append(mShortSubStrSet2);

        int size1 = mSpaceCommix1Mapping.length;
        int len1 = 0;
        for (int i = size1 - 1; i >= 0; i--) {
            if (i != (size1 - 1) && mShortSubStr1.length() > 0) {
                Log.i(TAG, "len , mSpaceCommix2Mapping[i]");
                mShortSubStr1.deleteCharAt(0);
            }
            len1 += mSpaceCommix1Mapping[i].length();
            mShortSubStr1.insert(0, mSpaceCommix1Mapping[i]);
            mShortSubStr1.insert(0, (char) len1);
            Log.i(TAG, "len , mSpaceCommix2Mapping[i] : " + len1 + " , "
                    + mSpaceCommix1Mapping[i]);
            mShortSubStrSet1.insert(0, mShortSubStr1);
        }
        subStrSet.append(mShortSubStrSet1);
        assertEquals(mSearchName.toString(), subStrSet.toString());
    }
}
