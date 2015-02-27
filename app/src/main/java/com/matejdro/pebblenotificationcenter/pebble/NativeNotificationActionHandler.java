package com.matejdro.pebblenotificationcenter.pebble;

import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
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
    private PebbleTalkerService service;

    public NativeNotificationActionHandler(PebbleTalkerService service)
    {
        this.service = service;
    }

    public void handle(ByteBuffer buffer)
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

        Timber.d("native action " + notificationId + " " + actionId + " " + replyText);

        ProcessedNotification notification = service.sentNotifications.get(notificationId);
        if (notification == null)
            return;

        if (notification.source.getActions().size() <= actionId)
            return;

        NotificationAction action = notification.source.getActions().get(actionId);

        if (service.getGlobalSettings().getBoolean("nativeSendSuccessMessage", false))
        {
            service.getDeveloperConnection().sendActionACKNACKCheckmark(notificationId, actionId + 1, "Done");
        }

        if (action instanceof DismissOnPebbleAction)
        {
            //Do nothing, any action from Pebble already dismisses notification.
        }
        else if (action instanceof WearVoiceAction)
        {
            if (replyText == null)
                return;

            WearVoiceAction voiceAction = (WearVoiceAction) action;
            if (voiceAction.containsVoiceOption() && replyText.equals("Voice"))
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
    }
}
