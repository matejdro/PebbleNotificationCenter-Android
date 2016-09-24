package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.content.Context;
import android.os.Parcel;

import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;

public class MuteAppTemporarilyAction extends NotificationAction {
    private int muteDurationMinutes;

    public MuteAppTemporarilyAction(Context context, int muteDurationMinutes) {
        this(context.getString(R.string.action_mute_app_temporarily_minutes, muteDurationMinutes), muteDurationMinutes);
    }

    public MuteAppTemporarilyAction(String text, int muteDurationMinutes) {
        super(text);

        this.muteDurationMinutes = muteDurationMinutes;
    }

    @Override
    public boolean executeAction(NCTalkerService service, ProcessedNotification notification) {
        DismissUpwardsModule.dismissPebbleID(service, notification.id);

        String appPackage = notification.source.getKey().getPackage();
        if (appPackage == null)
            return true;

        NotificationSendingModule.muteApp(service, appPackage, System.currentTimeMillis() + muteDurationMinutes * 60 * 1000);
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
        parcel.writeInt(muteDurationMinutes);
    }

    public static final Creator<MuteAppTemporarilyAction> CREATOR = new Creator<MuteAppTemporarilyAction>()
    {
        @Override
        public MuteAppTemporarilyAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            int muteDuration = parcel.readInt();

            return new MuteAppTemporarilyAction(text, muteDuration);
        }

        @Override
        public MuteAppTemporarilyAction[] newArray(int i)
        {
            return new MuteAppTemporarilyAction[0];
        }
    };
}
