package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.content.Context;
import android.os.Parcel;

import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;

public class MuteAppAction extends NotificationAction {
    public MuteAppAction(Context context) {
        super(context.getString(R.string.action_mute_app));
    }

    public MuteAppAction(String text) {
        super(text);
    }

    @Override
    public boolean executeAction(NCTalkerService service, ProcessedNotification notification) {
        DismissUpwardsModule.dismissPebbleID(service, notification.id);

        String appPackage = notification.source.getKey().getPackage();
        if (appPackage == null)
            return true;

        boolean includingMode = service.getGlobalSettings().getBoolean(PebbleNotificationCenter.APP_INCLUSION_MODE, false);
        notification.source.getSettingStorage(service).setAppChecked(!includingMode);

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

    public static final Creator<MuteAppAction> CREATOR = new Creator<MuteAppAction>()
    {
        @Override
        public MuteAppAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());

            return new MuteAppAction(text);
        }

        @Override
        public MuteAppAction[] newArray(int i)
        {
            return new MuteAppAction[0];
        }
    };
}
