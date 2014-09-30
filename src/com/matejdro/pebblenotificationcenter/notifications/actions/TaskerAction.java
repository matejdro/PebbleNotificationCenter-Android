package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.os.Parcel;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import java.util.ArrayList;
import java.util.List;
import net.dinglisch.android.tasker.TaskerIntent;

/**
 * Created by Matej on 22.9.2014.
 */
public class TaskerAction extends NotificationAction
{
    public TaskerAction(String taskName)
    {
        super(taskName);
    }

    @Override
    public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
    {
        TaskerIntent.Status status = TaskerIntent.testStatus(service);
        if (status != TaskerIntent.Status.OK)
        {
            sendErrorNotification(service, status);
            return;
        }

        TaskerIntent intent = new TaskerIntent(actionText);
        intent.addLocalVariable("%ncappname", NotificationHandler.getAppName(service, notification.source.getPackage()));
        intent.addLocalVariable("%ncapppkg", notification.source.getPackage());
        intent.addLocalVariable("%ncid", Integer.toString(notification.source.getAndroidID()));
        intent.addLocalVariable("%nctitle", notification.source.getTitle());
        intent.addLocalVariable("%ncsubtitle", notification.source.getSubtitle());
        intent.addLocalVariable("%nctext", notification.source.getText());

        if (notification.source.getTag() != null)
            intent.addLocalVariable("%nctag", notification.source.getTag());
        else
            intent.addLocalVariable("%nctag", "");

        service.sendBroadcast(intent);
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

    public static final Creator<TaskerAction> CREATOR = new Creator<TaskerAction>()
    {
        @Override
        public TaskerAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());

            return new TaskerAction(text);
        }

        @Override
        public TaskerAction[] newArray(int i)
        {
            return new TaskerAction[0];
        }
    };

    private static void sendErrorNotification(PebbleTalkerService service, TaskerIntent.Status error)
    {
        String text;

        switch (error)
        {
            case AccessBlocked:
                text = service.getString(R.string.taskerActionErrorNoExternalAccess);
                break;
            case NotEnabled:
                return; //Tasker disabling is likely intentional by user. Lets just silently fail here.
            case NotInstalled:
                text = service.getString(R.string.taskerActionErrorNotInstalled);
                break;
            default:
                text = service.getString(R.string.taskerActionErrorUnknown);
                break;
        }

        PebbleNotification notification = new PebbleNotification(service.getString(R.string.taskerActionErrorNotificationTitle), text, service.getPackageName());
        notification.setSubtitle(service.getString(R.string.taskerActionErrorNotificationSubtitle));
        notification.setForceSwitch(true);

        service.processNotification(notification);
    }


    public static void addTaskerTasks(AppSettingStorage settings, ArrayList<NotificationAction> storage)
    {
        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
            return;

        List<String> tasks = settings.getStringList(AppSetting.TASKER_ACTIONS);
        for (String task : tasks)
        {
            storage.add(new TaskerAction(task));

            if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                return;
        }
    }
}
