package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.lists.actions.ActionList;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 22.9.2014.
 */

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
public class WearVoiceAction extends NotificationAction
{
    private PendingIntent actionIntent;
    private String voiceKey;
    private String[] appProvidedChoices;
    private List<String> cannedResponseList;
    private ProcessedNotification parent;
    private boolean firstItemIsVoice;

    public WearVoiceAction(String actionText, PendingIntent intent, String voiceKey, String[] appProvidedChoices)
    {
        super(actionText);
        this.actionIntent = intent;
        this.voiceKey = voiceKey;
        this.appProvidedChoices = appProvidedChoices;
        firstItemIsVoice = false;
    }

    public static NotificationAction parseFromBundle(Bundle bundle)
    {
        if (bundle.get("title") == null)
            return null;

        String title = bundle.getCharSequence("title").toString() + " (Wear)";
        PendingIntent actionIntent = bundle.getParcelable("actionIntent");

        if (actionIntent == null)
            return null;

        Parcelable[] remoteInputs = (Parcelable[]) bundle.getParcelableArray("remoteInputs");
        if (remoteInputs == null || remoteInputs.length == 0)
        {
            return new PendingIntentAction(title, actionIntent);
        }

        Bundle firstRemoteInput = (Bundle) remoteInputs[0];
        String key = firstRemoteInput.getString("resultKey");
        CharSequence[] choices = firstRemoteInput.getCharSequenceArray("choices");

        String[] choicesString = new String[0];
        if (choices != null)
        {
            choicesString = new String[choices.length];
            for (int i = 0; i < choices.length; i++)
                choicesString[i] = choices[i].toString();
        }

        return new WearVoiceAction(title, actionIntent, key, choicesString);
    }

    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    public static NotificationAction parseFromAction(Notification.Action action)
    {
        String title = action.title.toString() + " (Wear)";
        PendingIntent actionIntent = action.actionIntent;

        if (actionIntent == null)
            return null;

        RemoteInput[] remoteInputs = action.getRemoteInputs();
        if (remoteInputs == null || remoteInputs.length == 0)
        {
            return new PendingIntentAction(title, actionIntent);
        }

        RemoteInput firstRemoteInput =  remoteInputs[0];
        String key = firstRemoteInput.getResultKey();
        CharSequence[] choices = firstRemoteInput.getChoices();

        String[] choicesString = new String[0];
        if (choices != null)
        {
            choicesString = new String[choices.length];
            for (int i = 0; i < choices.length; i++)
                choicesString[i] = choices[i].toString();
        }

        return new WearVoiceAction(title, actionIntent, key, choicesString);
    }

    @Override
    public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
    {
        parent = notification;
        cannedResponseList = new ArrayList<String>();

        if (notification.source.getSettingStorage(service).getBoolean(AppSetting.ENABLE_VOICE_REPLY))
        {
            cannedResponseList.add("Voice");
            firstItemIsVoice = true;
        }

        ArrayList<String> userProvidedChoices = (ArrayList<String>) notification.source.getSettingStorage(service).getStringList(AppSetting.CANNED_RESPONSES);
        if (userProvidedChoices != null)
        {
            for (String choice : userProvidedChoices)
            {
                cannedResponseList.add(choice);
                if (cannedResponseList.size() >= 20)
                    break;
            }
        }

        if (cannedResponseList.size() < 20 && appProvidedChoices != null)
        {
            for (CharSequence choice : appProvidedChoices)
            {
                cannedResponseList.add(choice.toString());
                if (cannedResponseList.size() >= 20)
                    break;
            }
        }

        notification.activeActionList = new WearCannedResponseList();
        notification.activeActionList.showList(service, notification);
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
        parcel.writeString(voiceKey);
        parcel.writeStringArray(appProvidedChoices);
    }


    public static final Creator<WearVoiceAction> CREATOR = new Creator<WearVoiceAction>()
    {
        @Override
        public WearVoiceAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            PendingIntent intent = (PendingIntent) parcel.readValue(PendingIntent.class.getClassLoader());
            String key = parcel.readString();
            String[] choices = parcel.createStringArray();

            return new WearVoiceAction(text, intent, key, choices);
        }

        @Override
        public WearVoiceAction[] newArray(int i)
        {
            return new WearVoiceAction[0];
        }
    };

    protected static void sendWearReply(String text, Context context, PendingIntent actionIntent, String voiceKey)
    {
        try
        {
            Bundle messageTextBundle = new Bundle();
            messageTextBundle.putCharSequence(voiceKey, text);

            Intent messageDataIntent = new Intent();
            messageDataIntent.putExtra("android.remoteinput.resultsData", messageTextBundle);

            ClipData clipData = new ClipData("android.remoteinput.results", new String[] { ClipDescription.MIMETYPE_TEXT_INTENT }, new ClipData.Item(messageDataIntent));
            Intent replyIntent = new Intent();
            replyIntent.setClipData(clipData);
            actionIntent.send(context, 0, replyIntent);
        } catch (PendingIntent.CanceledException e)
        {
            e.printStackTrace();
        }

    }

    public class WearCannedResponseList extends ActionList
    {
        @Override
        public int getNumberOfItems()
        {
            return cannedResponseList.size();
        }

        @Override
        public String getItem(int id)
        {
            return cannedResponseList.get(id);
        }

        @Override
        public void itemPicked(PebbleTalkerService service, int id)
        {
            if (id == 0 && firstItemIsVoice)
                new VoiceCapture(actionIntent, voiceKey, service).startVoice();
            else
            {
                if (cannedResponseList.size() <= id)
                    return;

                sendWearReply(cannedResponseList.get(id), service, actionIntent, voiceKey);
                service.processDismissUpwards(parent.source.getKey(), false);
                if (NotificationHandler.isNotificationListenerSupported())
                    JellybeanNotificationListener.dismissNotification(parent.source.getKey());
            }
        }
    }
}
