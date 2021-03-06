package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.content.Context;
import android.os.Parcel;

import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;

/**
 * Created by Matej on 22.9.2014.
 */
public class DismissOnPebbleAction extends NotificationAction
{
    public DismissOnPebbleAction(String text)
    {
        super(text);
    }

    public DismissOnPebbleAction(Context context)
    {
        super(context.getString(R.string.dismissOnPebble));
    }

    @Override
    public boolean executeAction(NCTalkerService service, ProcessedNotification notification)
    {
        DismissUpwardsModule.dismissPebbleID(service, notification.id);
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
    }

    public static final Creator<DismissOnPebbleAction> CREATOR = new Creator<DismissOnPebbleAction>()
    {
        @Override
        public DismissOnPebbleAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());

            return new DismissOnPebbleAction(text);
        }

        @Override
        public DismissOnPebbleAction[] newArray(int i)
        {
            return new DismissOnPebbleAction[0];
        }
    };
}
