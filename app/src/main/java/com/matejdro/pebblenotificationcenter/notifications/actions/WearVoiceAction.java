package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.matejdro.pebblecommons.messages.MessageTextProviderListener;
import com.matejdro.pebblecommons.messages.PhoneVoiceProvider;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NCUserPrompter;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.notifications.actions.lists.ActionList;
import com.matejdro.pebblenotificationcenter.notifications.actions.lists.WritingPhrasesList;
import com.matejdro.pebblenotificationcenter.pebble.modules.ActionsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by Matej on 22.9.2014.
 */

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
public class WearVoiceAction extends NotificationAction implements MessageTextProviderListener
{
    private PendingIntent actionIntent;
    private String voiceKey;
    private String[] appProvidedChoices;
    private List<String> cannedResponseList;
    private int notificationId;
    private int voiceItemIndex;
    private int timeVoiceItemIndex;
    private int writeItemIndex;
    private NCTalkerService lastUsedService;

    public WearVoiceAction(String actionText, PendingIntent intent, String voiceKey, String[] appProvidedChoices)
    {
        super(actionText);
        this.actionIntent = intent;
        this.voiceKey = voiceKey;
        this.appProvidedChoices = appProvidedChoices;
        voiceItemIndex = -1;
        timeVoiceItemIndex = -1;
        writeItemIndex = -1;
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
            {
                if (choices[i] != null)
                    choicesString[i] = choices[i].toString();
            }
        }

        return new WearVoiceAction(title, actionIntent, key, choicesString);
    }

    public void populateCannedList(NCTalkerService service, ProcessedNotification notification, boolean nativeMode)
    {
        lastUsedService = service;
        notificationId = notification.id;

        cannedResponseList = new ArrayList<String>();

        if (!nativeMode && notification.source.getSettingStorage(service).getBoolean(AppSetting.ENABLE_TIME_VOICE_REPLY) && lastUsedService.getPebbleCommunication().getConnectedPebblePlatform().hasMicrophone())
        {
            cannedResponseList.add("Time Voice");
            timeVoiceItemIndex = cannedResponseList.size() - 1;
        }


        if (notification.source.getSettingStorage(service).getBoolean(AppSetting.ENABLE_VOICE_REPLY))
        {
            cannedResponseList.add("Phone Voice");
            voiceItemIndex = cannedResponseList.size() - 1;
        }

        if (!nativeMode && notification.source.getSettingStorage(service).getBoolean(AppSetting.ENABLE_WRITING_REPLY))
        {
            cannedResponseList.add("Write");
            writeItemIndex = cannedResponseList.size() - 1;
        }

        ArrayList<String> userProvidedChoices = (ArrayList<String>) notification.source.getSettingStorage(service).getStringList(AppSetting.CANNED_RESPONSES);
        if (userProvidedChoices != null)
        {
            for (String choice : userProvidedChoices)
            {
                cannedResponseList.add(choice);
                if (cannedResponseList.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                    break;
            }
        }

        if (cannedResponseList.size() < NotificationAction.MAX_NUMBER_OF_ACTIONS && appProvidedChoices != null)
        {
            for (CharSequence choice : appProvidedChoices)
            {
                cannedResponseList.add(choice.toString());
                if (cannedResponseList.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                    break;
            }
        }
    }

    public List<String> getCannedResponseList()
    {
        return cannedResponseList;
    }

    public void showVoicePrompt(final NCTalkerService service)
    {
        service.runOnMainThread(new Runnable()
        {
            @Override
            public void run()
            {
                new PhoneVoiceProvider(new NCUserPrompter(service), service).startRetrievingText(WearVoiceAction.this);
            }
        });
    }

    public void showTimeVoicePrompt(final NCTalkerService service)
    {
        ActionsModule.get(service).startTimeVoice(this);
    }

    public void showTertiaryText(NCTalkerService service)
    {
        ActionsModule.get(service).showList(new WritingPhrasesList(this, service));
    }

    public boolean containsVoiceOption()
    {
        return voiceItemIndex != -1;
    }

    public boolean containsWriteOption()
    {
        return writeItemIndex != -1;
    }

    public ProcessedNotification getNotification(NCTalkerService service)
    {
        return service.sentNotifications.get(notificationId);
    }

    @Override
    public boolean executeAction(NCTalkerService service, ProcessedNotification notification)
    {
        lastUsedService = service;

        populateCannedList(service, notification, false);

        ActionsModule.get(service).showList(new WearCannedResponseList());
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
        parcel.writeValue(actionIntent);
        parcel.writeString(voiceKey);
        parcel.writeStringArray(appProvidedChoices);
        parcel.writeInt(notificationId);
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

            WearVoiceAction action = new WearVoiceAction(text, intent, key, choices);
            action.notificationId = parcel.readInt();

            return action;
        }

        @Override
        public WearVoiceAction[] newArray(int i)
        {
            return new WearVoiceAction[0];
        }
    };

    public void sendReply(String text, NCTalkerService service)
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
            actionIntent.send(service, 0, replyIntent);
        } catch (PendingIntent.CanceledException e)
        {
            e.printStackTrace();
        }

        ProcessedNotification notification = getNotification(service);
        if (notification == null)
        {
            Timber.w("Sending voice reply for non-existing notification.");
            return;
        }

        if (notification.source.getSettingStorage(service).getBoolean(AppSetting.DISMISS_AFTER_REPLY))
        {
            DismissUpwardsModule.dismissNotification(service, notification.source.getKey());
            if (NotificationHandler.isNotificationListenerSupported())
                JellybeanNotificationListener.dismissNotification(notification.source.getKey());
        }
    }

    @Override
    public void gotText(String text)
    {
        sendReply(text, lastUsedService);
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
        public boolean itemPicked(NCTalkerService service, int id)
        {
            if (id == voiceItemIndex)
            {
                showVoicePrompt(service);
            }
            else if (id == writeItemIndex)
            {
                showTertiaryText(service);
            }
            else if (id == timeVoiceItemIndex)
            {
                showTimeVoicePrompt(service);
            }
            else
            {
                if (cannedResponseList.size() <= id)
                    return false;

                sendReply(cannedResponseList.get(id), service);
            }

            return true;
        }
    }
}
