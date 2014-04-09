package com.mediatek.calendar.extension;

import android.app.Activity;
import android.content.Context;

import com.mediatek.calendar.MTKUtils;
import com.mediatek.calendar.edittext.EditTextExtImpl;
import com.mediatek.calendar.edittext.IEditTextExt;

/**
 * M: the class to produce plug-in extensions
 * it will consider the situation to determine which plugin should be produced
 */
public final class ExtensionFactory {

    /**
     * Because the mCalendarThemeExt is only use to get Theme,
     * to improve performance,make it as singleton
     */
    private static CalendarThemeExt sCalendarThemeExt;

    /**
     * M: produce the monthview extension plug-in to caller
     * @param context context the view runs
     * @param view the view to run the plug-in
     * @return the extension plug-in(maybe the default one)
     */
    public static IMonthViewExt getMonthViewPlugin(Context context, IMonthViewForExt view) {
        return new LunarMonthViewExt(context, view);
    }

    /**
     * M: produce the AllInOneOptionsMenu extension plug-in to caller
     * @param context context of AllInOneActivity
     * @return the extension plug-in(maybe the default one)
     */
    public static IOptionsMenuExt getAllInOneOptionMenuExt(Context context) {
        return new ClearAllEventsExt(context);
    }

    /**
     * M: produce the EventInfoFragment option menu extension plug-in to caller
     * @param context the context of the host
     * @param eventId the current event this view showing
     * @return the extension plug-in(maybe the default one)
     */
    public static IOptionsMenuExt getEventInfoOptionsMenuExt(Context context, long eventId) {
        if (MTKUtils.isEventShareAvailable(context)) {
            return new EventInfoOptionsMenuExt(context, eventId);
        } else {
            return new DefaultOptionsMenuExt();
        }
    }

    /**
     * M: PC Sync Account extension is a common feature
     * @param activity Activity to be extended
     * @return Extension object
     */
    public static IAccountExt getAccountExt(Activity activity) {
        return new PCSyncExtension(activity);
    }

    /**
     * M: Get The extension to enrich the function of EditEventView
     * @param activity the activity contains the EditEventView
     * @param editEventView the interface which can the host serve the plug-in
     * @return The Extension Object
     */
    public static IEditEventViewExt getEditEventViewExt(Activity activity, IEditEventView editEventView) {
        return new EditEventViewExt(activity, editEventView);
    }
    
    /**
     * M:Get the extension of Calendar Theme.
     * This is use to change the Calendar's Theme attribute.
     * @return  The ICalendarThemeExt Extension Object
     */
    public static synchronized ICalendarThemeExt getCalendarTheme(Context context) {
        if (sCalendarThemeExt == null) {
            sCalendarThemeExt = new CalendarThemeExt(context.getApplicationContext());
        }
        return sCalendarThemeExt;
    }
    
    /**
     * M:get the EditText extension feature object
     * @return The Extension for the EditText
     */
    public static IEditTextExt getEditTextExt() {
        IEditTextExt extension = new EditTextExtImpl();
        return extension;
    }
}
