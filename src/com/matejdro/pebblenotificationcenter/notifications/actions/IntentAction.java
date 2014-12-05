package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.content.Intent;
import android.os.Parcel;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 22.9.2014.
 */
public class IntentAction extends NotificationAction
{
    private String intentAction;

    public IntentAction(String actionName, String intentAction)
    {
        super(actionName);
        this.intentAction = intentAction;
    }

    @Override
    public boolean executeAction(PebbleTalkerService service, ProcessedNotification notification)
    {
        Intent intent = new Intent(intentAction);

        intent.putExtra("ncappname", NotificationHandler.getAppName(service, notification.source.getKey().getPackage()));
        intent.putExtra("ncapppkg", notification.source.getKey().getPackage());
        intent.putExtra("ncid", Integer.toString(notification.source.getKey().getAndroidId()));
        intent.putExtra("nctitle", notification.source.getTitle());
        intent.putExtra("ncsubtitle", notification.source.getSubtitle());
        intent.putExtra("nctext", notification.source.getText());

        if (notification.source.getKey().getTag() != null)
            intent.putExtra("nctag", notification.source.getKey().getTag());

        service.sendBroadcast(intent);
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
        parcel.writeValue(intentAction);
    }

    public static final Creator<IntentAction> CREATOR = new Creator<IntentAction>()
    {
        @Override
        public IntentAction createFromParcel(Parcel parcel)
        {
            String name = (String) parcel.readValue(String.class.getClassLoader());
            String action = (String) parcel.readValue(String.class.getClassLoader());

            return new IntentAction(name, action);
        }

        @Override
        public IntentAction[] newArray(int i)
        {
            return new IntentAction[0];
        }
    };

    public static void addIntentActions(AppSettingStorage settings, ArrayList<NotificationAction> storage)
    {
        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
            return;

        List<String> names = settings.getStringList(AppSetting.INTENT_ACTIONS_NAMES);
        List<String> actions = settings.getStringList(AppSetting.INTENT_ACTIONS_ACTIONS);

        for (int i = 0; i < names.size(); i++)
        {
            storage.add(new IntentAction(names.get(i), actions.get(i)));

            if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                return;
        }
    }
}
