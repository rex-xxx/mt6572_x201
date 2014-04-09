package src.com.mediatek.todos.tests;

import android.test.InstrumentationTestCase;
import android.text.format.Time;

import com.mediatek.todos.Utils;

import java.sql.Date;

public class TodosUtilsTest extends InstrumentationTestCase {

    public void testDateCompare() {
        Time time1 = new Time();
        time1.year = 2000;
        time1.month = 10;
        time1.monthDay = 10;

        Time time2 = new Time();
        time2.year = 2000;
        time2.month = 10;
        time2.monthDay = 10;

        assertEquals(Utils.dateCompare(time1, time2), 0);
        time2.year = 2001;
        assertEquals(Utils.dateCompare(time1, time2), -1);
        time2.year = 1999;
        assertEquals(Utils.dateCompare(time1, time2), 1);
    }

}
