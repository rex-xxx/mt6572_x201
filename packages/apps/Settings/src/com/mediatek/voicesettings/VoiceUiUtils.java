package com.mediatek.voicesettings;

import android.util.Log;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class VoiceUiUtils {

    public static final String TAG = "VoiceUiUtils";

    private final int mAlarmId = 3;
    private final String mAlarmProcessName = "com.android.deskclock";
    
    private String mKeyWord = "KeyWord";
    private String mKeyId = "ID";
    private String mVoiceProcessName = "ProcessName";

    private List<String> mAlarmKeywords;

    public VoiceUiUtils() {
        //todo some initialize
        mAlarmKeywords = new ArrayList<String>();
        Log.v(TAG, "VoiceUiUtils constructor ");
    }

    public void readKeyWordFromXml(HashMap<String, String[]> voiceKeyWordInfos,
            String filename) {
        final File keyWordFile = new File(filename);
        FileReader keyWordReader = null;
        try {
            keyWordReader = new FileReader(keyWordFile);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Couldn't find or open file " + keyWordFile);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(keyWordReader);
            XmlUtils.beginDocument(parser, "KeyWordInfos");
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                String name = parser.getName();
                if ("KeyWordInfo".equals(name)) {
                    String processName = parser.getAttributeValue(null,
                            mVoiceProcessName);
                    String procId = parser.getAttributeValue(null, mKeyId);
                    String keyWord = parser.getAttributeValue(null, mKeyWord);

                    if (processName != null && keyWord != null) {
                        Log.v(TAG, "readEnglishKeyWordFromXml id = " + procId + " processName   = "
                                + processName + "  KeyWord =" + keyWord);
                        String[] keyWordArray = keyWord.split(",");
                        if (Integer.parseInt(procId) == mAlarmId) {
                            mAlarmKeywords.addAll(Arrays.asList(keyWordArray));
                            Log.v(TAG, mAlarmKeywords.toString());
                            voiceKeyWordInfos.put(mAlarmProcessName, mAlarmKeywords.toArray(new String[]{}));
                        } else {
                            voiceKeyWordInfos.put(processName, keyWordArray);
                        }
                    } else {
                        Log.v(TAG, "Error processName or keyWord " + keyWord);
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.v(TAG, "Got execption parsing permissions.", e);
        } catch (IOException e) {
            Log.v(TAG, "Got execption parsing permissions.", e);
        } catch (NumberFormatException e) {
            Log.v(TAG, "Got NumberFormatException parsing number error.", e);
        } finally {
            try {
                keyWordReader.close();
            } catch (IOException e) {
                Log.v(TAG, "Got execption parsing permissions.", e);
            }
        }
    }
}
