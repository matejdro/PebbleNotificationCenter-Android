package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.os.Parcel;

import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;

/**
 * Created by Matej on 22.9.2014.
 */
public class ReplaceNotificationAction extends NotificationAction
{
    private ProcessedNotification notification;

    public ReplaceNotificationAction(String actionText, ProcessedNotification notification)
    {
        super(actionText);
        this.notification = notification;
    }

    @Override
    public boolean executeAction(PebbleTalkerService service, ProcessedNotification activeNotification)
    {
        DismissUpwardsModule.dismissPebbleID(service, activeNotification.id);
        NotificationSendingModule.get(service).sendNotification(this.notification);
        return true;
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
        parcel.writeValue(notification);
    }

    public static final Creator<ReplaceNotificationAction> CREATOR = new Creator<ReplaceNotificationAction>()
    {
        @Override
        public ReplaceNotificationAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            ProcessedNotification notification = (ProcessedNotification) parcel.readValue(getClass().getClassLoader());

            return new ReplaceNotificationAction(text, notification);
        }

        @Override
        public ReplaceNotificationAction[] newArray(int i)
        {
            return new ReplaceNotificationAction[0];
        }
    };
}
