package com.matejdro.pebblenotificationcenter.pebble;

import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.notifications.actions.DismissOnPebbleAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.WearVoiceAction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import timber.log.Timber;

/**
 * Created by Matej on 16.12.2014.
 */
public class NativeNotificationActionHandler
{
    private NCTalkerService service;

    public NativeNotificationActionHandler(NCTalkerService service)
    {
        this.service = service;
    }

    public void handleSdk2(ByteBuffer buffer)
    {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int messageType = buffer.get();
        if (messageType != 2) //Invoke action message
            return;

        int notificationId = buffer.getInt();
        int actionId = buffer.get() - 1;
        String replyText = null;

        boolean additionalData = buffer.hasRemaining();

        if (additionalData)
        {
            int n = buffer.get();
            if (n > 0)
            {
                int k = buffer.get() & 0xFF;
                int limit = buffer.getShort() & 0xFFFF;
                replyText = PebbleDeveloperConnection.getPebbleStringFromByteBuffer(buffer, limit);
            }
        }

        boolean handled = handle(notificationId, actionId, replyText);
        if (handled && service.getGlobalSettings().getBoolean("nativeSendSuccessMessage", false))
        {
            service.getDeveloperConnection().sendActionACKNACKCheckmark(notificationId, actionId + 1, "Done");
        }


    }

    public void handleSdk3(ByteBuffer buffer)
    {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int command = buffer.get();
        if (command != 0x02) //InvokeAction command
            return;

        long idFirstLong = buffer.getLong();
        long idSecondLong = buffer.getLong();
        if (idFirstLong != idSecondLong)
            return; //NC stores id as int so both of these must be the same int

        int actionId = buffer.get() - 1;

        String replyText = null;
        int numAttributes = buffer.get();
        for (int i = 0; i < numAttributes; i++)
        {
            int attributeId = buffer.get();
            int attributeSize = buffer.getShort();

            if (attributeId != 0x01) //Reply text attribute
            {
                buffer.position(buffer.position() + attributeSize);
                continue;
            }

            replyText = PebbleDeveloperConnection.getPebbleStringFromByteBuffer(buffer, attributeSize);
        }

        final int notificationId = (int) idFirstLong;
        if (handle(notificationId, actionId, replyText))
        {
            NotificationCenterDeveloperConnection.fromDevConn(service.getDeveloperConnection()).sendSDK3ActionACK(notificationId);
        }
    }

    private boolean handle(int notificationId, int actionId, String replyText)
    {
        Timber.d("native action %d %d %s", notificationId, actionId, replyText);

        final ProcessedNotification notification = service.sentNotifications.get(notificationId);
        if (notification == null)
            return false;

        if (notification.source.getActions().size() <= actionId)
            return false;

        NotificationAction action = notification.source.getActions().get(actionId);

        //noinspection StatementWithEmptyBody
        if (action instanceof DismissOnPebbleAction)
        {
            //Do nothing, any action from Pebble already dismisses notification.
        }
        else if (action instanceof WearVoiceAction)
        {
            if (replyText == null)
                return false;

            WearVoiceAction voiceAction = (WearVoiceAction) action;
            if (voiceAction.containsVoiceOption() && replyText.equals("Phone Voice"))
            {
                voiceAction.showVoicePrompt(service);
            }
            else
            {
                voiceAction.sendReply(replyText, service);
            }
        }
        else
        {
            action.executeAction(service, notification);
        }

        return true;
    }
}
