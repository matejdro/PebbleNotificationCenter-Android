package com.matejdro.pebblenotificationcenter.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.matejdro.pebblecommons.userprompt.UserPrompter;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;

import java.util.ArrayList;

public class NCUserPrompter implements UserPrompter
{
    private static NotificationKey NC_PROMPTER_KEY = new NotificationKey(PebbleNotificationCenter.PACKAGE, 12345, null);

    private Context context;

    public NCUserPrompter(Context context)
    {
        this.context = context;
    }

    @Override
    public void promptUser(String title, @Nullable String subtitle, String body, PromptAnswer... answers)
    {
        PebbleNotification notification = new PebbleNotification(title, body, NC_PROMPTER_KEY);
        ArrayList<NotificationAction> actionList = new ArrayList<>();
        for (PromptAnswer answer : answers)
        {
            actionList.add(new DismissIntentAction(answer.getText(), answer.getAction()));
        }

        notification.setSubtitle(subtitle);
        notification.setDismissable(false);
        notification.setForceActionMenu(true);
        notification.setForceSwitch(true);
        notification.setActions(actionList);
        notification.setNoHistory(true);

        NotificationSendingModule.notify(notification, context);
    }

    private static class DismissIntentAction extends NotificationAction
    {
        private Intent intent;

        public DismissIntentAction(String actionName, Intent intent)
        {
            super(actionName);
            this.intent = intent;
        }

        @Override
        public boolean executeAction(NCTalkerService service, ProcessedNotification notification)
        {
            service.sendBroadcast(intent);
            DismissUpwardsModule.dismissPebbleID(service, notification.id);
            return false;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i)
        {
            parcel.writeValue(actionText);
            parcel.writeParcelable(intent, i);
        }

        public static final Creator<DismissIntentAction> CREATOR = new Creator<DismissIntentAction>()
        {
            @Override
            public DismissIntentAction createFromParcel(Parcel parcel)
            {
                String name = (String) parcel.readValue(String.class.getClassLoader());
                Intent intent = (Intent) parcel.readParcelable(String.class.getClassLoader());

                return new DismissIntentAction(name, intent);
            }

            @Override
            public DismissIntentAction[] newArray(int i)
            {
                return new DismissIntentAction[0];
            }
        };
    }
}
