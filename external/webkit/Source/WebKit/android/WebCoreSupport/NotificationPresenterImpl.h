/*
 * Copyright 2007, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef NotificationPresenterImpl_h
#define NotificationPresenterImpl_h

#include "NotificationPresenter.h"
#include "VoidCallback.h"
#include <wtf/HashMap.h>
#include <wtf/PassRefPtr.h>
#include "PlatformString.h"

#if ENABLE(NOTIFICATIONS)

namespace WebCore {
    class SQLiteDatabase;
}

using namespace WebCore;


namespace android {

class WebNotificationPresenter;
class WebViewCore;


class NotificationPresenterImpl : public WebCore::NotificationPresenter {
public:

    enum NotificationState {
            AlwaysON, // User has allowed notifications
            OnDemand, // User has not yet allowed
            Off // User has explicitly denied permission
        };

    NotificationPresenterImpl( WebViewCore*  );

    // WebCore::NotificationPresenter implementation.
    virtual bool show(WTF::PassRefPtr<WebCore::Notification> object);
    virtual void cancel(WebCore::Notification* object);
    virtual void notificationObjectDestroyed(WebCore::Notification* object);
    virtual WebCore::NotificationPresenter::Permission checkPermission(WebCore::ScriptExecutionContext*);
    virtual void requestPermission(WebCore::ScriptExecutionContext* , WTF::PassRefPtr<WebCore::VoidCallback> callback);
    virtual void cancelRequestsForPermission(WebCore::ScriptExecutionContext*) ;

    //Callback functions from UI
    void providePermissionState(const WTF::String& origin, bool allow);
    void dispatchNotificationEvents(const WTF::String& eventName, int counter);
    void recordNotificationID(int notificationID, int counter);
    static void setSettingsValue(int);

    //Database related
    static void setDatabasePath(WTF::String path);
    static void maybeLoadPermanentPermissions();
    static bool openDatabase(SQLiteDatabase*);
    static void maybeStorePermanentPermissions();
    static void clearAll();
    static void deleteDatabase();

private:
    WebViewCore* m_webViewCore;
    String m_url;
    int m_result;
    RefPtr<WebCore::VoidCallback> m_callback;
    static int s_counter;
    static String s_databasePath;

    typedef WTF::HashMap<WTF::String, bool> PermissionsMap;
    static PermissionsMap s_notificationPermissions;
    typedef  HashMap< int,RefPtr<WebCore::Notification> > Notificationmap;
    static Notificationmap s_notificationMap;
    typedef  HashMap< RefPtr<WebCore::Notification>, int > NotificationIDmap;
    static NotificationIDmap s_notificationIDMap;
    static int m_notificationState;
    bool selectFromDB();

};

} // namespace android


#endif // ENABLE(NOTIFICATIONS)

#endif
