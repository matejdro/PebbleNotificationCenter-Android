package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.app.PendingIntent;
import android.os.Parcel;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;

/**
 * Created by Matej on 22.9.2014.
 */
public class IntentAction extends NotificationAction
{
    private PendingIntent actionIntent;
    public IntentAction(String actionText,  PendingIntent intent)
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

    public static final Creator<IntentAction> CREATOR = new Creator<IntentAction>()
    {
        @Override
        public IntentAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            PendingIntent intent = (PendingIntent) parcel.readValue(PendingIntent.class.getClassLoader());

            return new IntentAction(text, intent);
        }

        @Override
        public IntentAction[] newArray(int i)
        {
            return new IntentAction[0];
        }
    };
}
