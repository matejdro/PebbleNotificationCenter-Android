package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.app.PendingIntent;
import android.os.Parcel;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;

/**
 * Created by Matej on 22.9.2014.
 */
public class PendingIntentAction extends NotificationAction
{
    private PendingIntent actionIntent;
    public PendingIntentAction(String actionText, PendingIntent intent)
    {
        super(actionText);
        this.actionIntent = intent;
    }

    @Override
    public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
    {
        try
        {
            actionIntent.send();
        } catch (PendingIntent.CanceledException e)
        {
            e.printStackTrace();
        }
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

        parcel.writeValue(actionIntent);
    }

    public static final Creator<PendingIntentAction> CREATOR = new Creator<PendingIntentAction>()
    {
        @Override
        public PendingIntentAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            PendingIntent intent = (PendingIntent) parcel.readValue(PendingIntent.class.getClassLoader());

            return new PendingIntentAction(text, intent);
        }

        @Override
        public PendingIntentAction[] newArray(int i)
        {
            return new PendingIntentAction[0];
        }
    };
}
