package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.content.Context;
import android.os.Parcel;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.R;

/**
 * Created by Matej on 22.9.2014.
 */
public class DismissOnPhoneAction extends NotificationAction
{
    private DismissOnPhoneAction(String text)
    {
        super(text);
    }

    public DismissOnPhoneAction(Context context)
    {
        super(context.getString(R.string.dismissOnPhone));
    }

    @Override
    public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
    {
        service.dismissOnPhone(notification);
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
    }

    public static final Creator<DismissOnPhoneAction> CREATOR = new Creator<DismissOnPhoneAction>()
    {
        @Override
        public DismissOnPhoneAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());

            return new DismissOnPhoneAction(text);
        }

        @Override
        public DismissOnPhoneAction[] newArray(int i)
        {
            return new DismissOnPhoneAction[0];
        }
    };
}
