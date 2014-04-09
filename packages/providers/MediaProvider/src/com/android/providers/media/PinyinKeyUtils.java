package com.android.providers.media;

import android.database.DatabaseUtils;
import android.provider.MediaStore;

import com.android.providers.media.HanziToPinyin.Token;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Helper class used to converts name to its Pinyin form.
 * 
 * @hide
 */
public class PinyinKeyUtils {
    /**
     * Converts a name to a "pinyin key" that can be used for grouping, sorting
     * and searching.
     * The rules that govern this conversion are:
     * - remove 'special' characters like ()[]'!?.,
     * - remove leading/trailing spaces
     * - convert everything to lowercase
     * - remove leading "the ", "an " and "a "
     * - remove trailing ", the|an|a"
     * - remove accents. This step leaves us with pinyin key data,
     *   which is human readable
     *
     * @param name The artist or album name to convert
     * @return The "pinyin key" for the given name.
     */
    public static String keyFor(String name) {
        if (!hasChina()) {
            return MediaStore.Audio.keyFor(name);
        }

        if (name != null) {
            boolean sortfirst = false;
            if (name.equals(MediaStore.UNKNOWN_STRING)) {
                return "\001";
            }
            // Check if the first character is \001. We use this to
            // force sorting of certain special files, like the silent ringtone.
            if (name.startsWith("\001")) {
                sortfirst = true;
            }
            name = name.trim().toLowerCase();
            if (name.startsWith("the ")) {
                name = name.substring(4);
            }
            if (name.startsWith("an ")) {
                name = name.substring(3);
            }
            if (name.startsWith("a ")) {
                name = name.substring(2);
            }
            if (name.endsWith(", the") || name.endsWith(",the") ||
                name.endsWith(", an") || name.endsWith(",an") ||
                name.endsWith(", a") || name.endsWith(",a")) {
                name = name.substring(0, name.lastIndexOf(','));
            }
            name = name.replaceAll("[\\[\\]\\(\\)\"'.,?!]", "").trim();
            if (name.length() > 0) {
                // Insert a separator between the characters to avoid
                // matches on a partial character. If we ever change
                // to start-of-word-only matches, this can be removed.
                StringBuilder b = new StringBuilder();
                b.append('.');
                int nl = name.length();
                for (int i = 0; i < nl; i++) {
                    b.append(name.charAt(i));
                    b.append('.');
                }
                name = b.toString();
                String key = getPinyin(name);
                if (sortfirst) {
                    key = "\001" + key;
                }
                return key;
           } else {
                return "";
            }
        }
        return null;
    }

    private static String getPinyin(String name) {
        // parse pinyin for title.
        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
        if (tokens.size() > 0) {
            Token token = tokens.get(0);
            if (token != null && token.mType == Token.UNKNOWN) {
                return DatabaseUtils.getCollationKey(name);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(token.mTarget);
        }
        return sb.toString().toUpperCase();
    }

    private static boolean hasChina() {
        if (sHasChina == null) {
            final Locale locale[] = Collator.getAvailableLocales();
            for (int i = 0; i < locale.length; i++) {
                if (locale[i].equals(Locale.CHINA)) {
                    sHasChina = Boolean.valueOf(true);
                    break;
                }
            }
            if (sHasChina == null) {
                sHasChina = Boolean.valueOf(false);
            }
        }
        return sHasChina.booleanValue();
    }

    private static Boolean sHasChina;
}
