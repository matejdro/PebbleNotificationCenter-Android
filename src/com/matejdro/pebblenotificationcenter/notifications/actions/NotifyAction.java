package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.os.Parcel;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;

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
    public void executeAction(PebbleTalkerService service, ProcessedNotification activeNotification)
    {
        service.processNotification(this.notification);
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
