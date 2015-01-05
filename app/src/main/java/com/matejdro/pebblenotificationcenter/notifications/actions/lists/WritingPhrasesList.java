package com.matejdro.pebblenotificationcenter.notifications.actions.lists;

import android.content.Context;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.WearVoiceAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 19.12.2014.
 */
public class WritingPhrasesList extends ActionList
{
    private WearVoiceAction action;
    private PebbleTalkerService service;
    private List<String> phrases;

    public WritingPhrasesList(WearVoiceAction action, PebbleTalkerService service)
    {
        this.action = action;
        this.service = service;

        populateList(service);
    }

    private void populateList(PebbleTalkerService service)
    {
        phrases = new ArrayList<String>();

        phrases.add("Send");

        ArrayList<String> userProvidedPhrases = (ArrayList<String>) action.getNotification(service).source.getSettingStorage(service).getStringList(AppSetting.WRITING_PHRASES);
        if (userProvidedPhrases != null)
        {
            for (String choice : userProvidedPhrases)
            {
                phrases.add(choice);
                if (phrases.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                    break;
            }
        }

        if (phrases.size() < NotificationAction.MAX_NUMBER_OF_ACTIONS)
        {
            for (String cannedResponse : action.getCannedResponseList())
            {
                phrases.add(cannedResponse);
            }
        }
    }


    @Override
    public int getNumberOfItems()
    {
        return phrases.size();
    }

    @Override
    public String getItem(int id)
    {
        return phrases.get(id);
    }

    @Override
    public boolean itemPicked(PebbleTalkerService service, int id)
    {
        return false;
    }

    public void reply(String text)
    {
        action.sendReply(text, service);
    }

    @Override
    public boolean isTertiaryTextList()
    {
        return true;
    }
}
