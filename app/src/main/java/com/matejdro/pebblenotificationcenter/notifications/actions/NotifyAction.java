package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.os.Parcel;

import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;

/**
 * Created by Matej on 22.9.2014.
 */
public class NotifyAction extends NotificationAction
{
    private PebbleNotification notification;
    public NotifyAction(String actionText, PebbleNotification notification)
    {
        super(actionText);
        this.notification = notification;
    }

    @Override
    public boolean executeAction(NCTalkerService service, ProcessedNotification activeNotification)
    {
        NotificationSendingModule.notify(this.notification, service);
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

    public static final Creator<NotifyAction> CREATOR = new Creator<NotifyAction>()
    {
        @Override
        public NotifyAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            PebbleNotification notification = (PebbleNotification) parcel.readValue(getClass().getClassLoader());

            return new NotifyAction(text, notification);
        }

        @Override
        public NotifyAction[] newArray(int i)
        {
            return new NotifyAction[0];
        }
    };
}
