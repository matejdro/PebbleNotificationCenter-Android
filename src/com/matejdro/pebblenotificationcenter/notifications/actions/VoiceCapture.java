package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Parcel;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.util.BluetoothHeadsetListener;
import java.util.ArrayList;

/**
 * Created by Matej on 28.9.2014.
 */
public class VoiceCapture implements RecognitionListener
{
    private static NotificationKey VOICE_NOTIFICATION_KEY = new NotificationKey(PebbleNotificationCenter.PACKAGE, 12345, null);

    private PendingIntent resultIntent;
    private String resultKey;
    private PebbleTalkerService service;
    private SpeechRecognizer recognizer;
    private boolean waitingForBluetooth;

    public VoiceCapture(PendingIntent resultIntent, String resultKey, PebbleTalkerService service)
    {
        this.resultIntent = resultIntent;
        this.resultKey = resultKey;
        this.service = service;

        waitingForBluetooth = false;
    }

    public void startVoice()
    {
        if (waitingForBluetooth)
            return;

        if (recognizer != null)
        {
            recognizer.stopListening();
            recognizer.destroy();
        }

        if (BluetoothHeadsetListener.isHeadsetConnected(service))
        {
            sendStatusNotification(service.getString(R.string.voiceInputBluetoothWait));

            AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);

            service.registerReceiver(new BluetoothAudioListener(), new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            waitingForBluetooth = true;
        }
        else
        {
            sendStatusNotification(service.getString(R.string.voiceInputSpeakInstructions));
            startRecognizing();
        }
    }

    public void stopVoice()
    {
        recognizer.stopListening();
        recognizer.destroy();

        AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isBluetoothScoOn())
        {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }
    }

    public void startRecognizing()
    {
        recognizer = SpeechRecognizer.createSpeechRecognizer(service);
        Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, service.getPackageName());
        recognizer.setRecognitionListener(this);
        recognizer.startListening(speechRecognizerIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle bundle)
    {
    }

    @Override
    public void onBeginningOfSpeech()
    {
    }

    @Override
    public void onRmsChanged(float v)
    {

    }

    @Override
    public void onBufferReceived(byte[] bytes)
    {

    }

    @Override
    public void onEndOfSpeech()
    {
    }

    @Override
    public void onError(int i)
    {
        stopVoice();

        switch (i)
        {
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sendErrorNotification(service.getString(R.string.voiceErrorNoInternet));
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sendErrorNotification(service.getString(R.string.voiceErrorNoSpeech));
                break;
            default:
                sendErrorNotification(service.getString(R.string.voiceErrorUnknown));
                break;
        }
    }

    @Override
    public void onResults(Bundle bundle)
    {
        stopVoice();

        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        int size = Math.min(matches.size(), 10);

        if (size == 0)
        {
            sendErrorNotification(service.getString(R.string.voiceErrorNoSpeech));
            return;
        }

        String resultsText = "";
        for (int i = 0; i < size; i++)
        {
            resultsText = resultsText.concat(service.getString((R.string.voiceInputResultTitle), i + 1));
            resultsText = resultsText.concat(matches.get(i));
            if (i != size - 1)
                resultsText = resultsText.concat("\n\n");
        }

        PebbleNotification notification = new PebbleNotification(service.getString(R.string.voiceInputNotificationTitle), service.getString(R.string.voiceInputResultNotificationText, resultsText), VOICE_NOTIFICATION_KEY);
        notification.setSubtitle(service.getString(R.string.voiceInputResultNotificationSubtitle));
        notification.setForceSwitch(true);
        notification.setForceActionMenu(true);

        ArrayList<NotificationAction> actions = new ArrayList<NotificationAction>(1);
        for (int i = 0; i < size; i++)
        {
            actions.add(new VoiceConfirmAction(service.getString(R.string.voiceInputResultActionName, i + 1), matches.get(i), resultIntent, resultKey));
        }
        actions.add(new RetryAction(resultIntent, resultKey));
        actions.add(new DismissOnPebbleAction(service.getString(R.string.cancel)));
        notification.setActions(actions);

        service.processNotification(notification);
    }

    @Override
    public void onPartialResults(Bundle bundle)
    {

    }

    @Override
    public void onEvent(int i, Bundle bundle)
    {

    }

    private void sendErrorNotification(String error)
    {
        PebbleNotification notification = new PebbleNotification(service.getString(R.string.voiceInputNotificationTitle), service.getString(R.string.voiceInputErrorNotificationText, error), VOICE_NOTIFICATION_KEY);
        notification.setSubtitle(service.getString(R.string.voiceInputErrorNotificationSubtitle));
        notification.setForceSwitch(true);
        notification.setForceActionMenu(true);

        ArrayList<NotificationAction> actions = new ArrayList<NotificationAction>(1);
        actions.add(new RetryAction(resultIntent, resultKey));
        actions.add(new DismissOnPebbleAction(service.getString(R.string.cancel)));
        notification.setActions(actions);

        service.processNotification(notification);
    }

    private void sendStatusNotification(String text)
    {
        PebbleNotification notification = new PebbleNotification(service.getString(R.string.voiceInputNotificationTitle), text, VOICE_NOTIFICATION_KEY);
        notification.setForceSwitch(true);

        service.processNotification(notification);
    }

    private static class RetryAction extends NotificationAction
    {
        private PendingIntent resultIntent;
        private String resultKey;

        public RetryAction(PendingIntent resultIntent, String resultKey)
        {
            super("Retry");
            this.resultIntent = resultIntent;
            this.resultKey = resultKey;
        }

        @Override
        public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
        {
            new VoiceCapture(resultIntent, resultKey, service).startVoice();
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i)
        {
            parcel.writeValue(resultIntent);
            parcel.writeString(resultKey);
        }

        public static final Creator<RetryAction> CREATOR = new Creator<RetryAction>()
        {
            @Override
            public RetryAction createFromParcel(Parcel parcel)
            {
                PendingIntent intent = (PendingIntent) parcel.readValue(PendingIntent.class.getClassLoader());
                String key = parcel.readString();

                return new RetryAction(intent, key);
            }

            @Override
            public RetryAction[] newArray(int i)
            {
                return new RetryAction[0];
            }
        };
    }

    private static class VoiceConfirmAction extends NotificationAction
    {
        private String text;
        private PendingIntent resultIntent;
        private String resultKey;

        public VoiceConfirmAction(String title, String text, PendingIntent resultIntent, String resultKey)
        {
            super(title);
            this.text = text;
            this.resultIntent = resultIntent;
            this.resultKey = resultKey;
        }

        @Override
        public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
        {
            service.processDismissUpwards(VOICE_NOTIFICATION_KEY, false);
            WearVoiceAction.sendWearReply(text, service, resultIntent, resultKey);
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
            parcel.writeValue(text);
            parcel.writeValue(resultIntent);
            parcel.writeString(resultKey);
        }

        public static final Creator<VoiceConfirmAction> CREATOR = new Creator<VoiceConfirmAction>()
        {
            @Override
            public VoiceConfirmAction createFromParcel(Parcel parcel)
            {
                String title = parcel.readString();
                String text = parcel.readString();
                PendingIntent intent = (PendingIntent) parcel.readValue(PendingIntent.class.getClassLoader());
                String key = parcel.readString();

                return new VoiceConfirmAction(title, text, intent, key);
            }

            @Override
            public VoiceConfirmAction[] newArray(int i)
            {
                return new VoiceConfirmAction[0];
            }
        };
    }

    private class BluetoothAudioListener extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED)
            {
                startRecognizing();
                context.unregisterReceiver(this);
                sendStatusNotification(service.getString(R.string.voiceInputSpeakNow));
            }
        }
    }

}
