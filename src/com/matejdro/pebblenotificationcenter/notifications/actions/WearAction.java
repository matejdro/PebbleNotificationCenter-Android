package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;

/**
 * Created by Matej on 22.9.2014.
 */

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
public class WearAction extends NotificationAction
{
    private PendingIntent actionIntent;
    private String voiceKey;

    public WearAction(String actionText, PendingIntent intent, String voiceKey)
    {
        super(actionText);
        this.actionIntent = intent;
        this.voiceKey = voiceKey;
    }

    public static WearAction parseFromBundle(Bundle bundle)
    {
        String title = bundle.getCharSequence("title").toString() + " (Wear)";
        PendingIntent actionIntent = bundle.getParcelable("actionIntent");

        Parcelable[] remoteInputs = (Parcelable[]) bundle.getParcelableArray("remoteInputs");
        if (remoteInputs == null || remoteInputs.length == 0)
            return null;

        Bundle firstRemoteInput = (Bundle) remoteInputs[0];
        String key = firstRemoteInput.getString("resultKey");

        return new WearAction(title, actionIntent, key);
    }

    @Override
    public void executeAction(PebbleTalkerService service)
    {
        try
        {
            Bundle messageTextBundle = new Bundle();
            messageTextBundle.putCharSequence(voiceKey, "Replying from my Pebble!");

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
    }

    public static final Creator<WearAction> CREATOR = new Creator<WearAction>()
    {
        @Override
        public WearAction createFromParcel(Parcel parcel)
        {
            String text = (String) parcel.readValue(String.class.getClassLoader());
            PendingIntent intent = (PendingIntent) parcel.readValue(PendingIntent.class.getClassLoader());
            String key = parcel.readString();
            return new WearAction(text, intent, key);
        }

        @Override
        public WearAction[] newArray(int i)
        {
            return new WearAction[0];
        }
    };
}
