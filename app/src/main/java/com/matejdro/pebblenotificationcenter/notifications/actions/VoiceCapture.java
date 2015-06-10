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
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.util.BluetoothHeadsetListener;
import java.util.ArrayList;
import timber.log.Timber;

/**
 * Created by Matej on 28.9.2014.
 */
public class VoiceCapture implements RecognitionListener
{
    private static NotificationKey VOICE_NOTIFICATION_KEY = new NotificationKey(PebbleNotificationCenter.PACKAGE, 12345, null);

    private WearVoiceAction voiceAction;
    private PebbleTalkerService service;
    private SpeechRecognizer recognizer;
    private boolean waitingForBluetooth;

    public VoiceCapture(WearVoiceAction voiceAction, PebbleTalkerService service)
    {
        this.voiceAction = voiceAction;
        this.service = service;

        waitingForBluetooth = false;
    }

    public void startVoice()
    {
        Timber.d("startVoice");

        if (waitingForBluetooth)
            return;

        if (recognizer != null)
        {
            recognizer.stopListening();
            recognizer.destroy();
        }

        if (BluetoothHeadsetListener.isHeadsetConnected(service))
        {
            Timber.d("BT Wait");

            sendStatusNotification(service.getString(R.string.voiceInputBluetoothWait));

            service.registerReceiver(new BluetoothAudioListener(), new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            waitingForBluetooth = true;

            AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        }
        else
        {
            Timber.d("Regular voice start");

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
        Timber.d("startRecognizing");

        recognizer = SpeechRecognizer.createSpeechRecognizer(service);
        Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, service.getPackageName());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SECURE, true);

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
        Timber.d("voiceError " + i);

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

        Timber.d("voiceResults " + size);

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
            actions.add(new VoiceConfirmAction(service.getString(R.string.voiceInputResultActionName, i + 1), matches.get(i), voiceAction));
        }
        actions.add(new RetryAction(voiceAction));
        actions.add(new DismissOnPebbleAction(service.getString(R.string.cancel)));
        notification.setActions(actions);

        NotificationSendingModule.notify(notification, service);
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
        actions.add(new RetryAction(voiceAction));
        actions.add(new DismissOnPebbleAction(service.getString(R.string.cancel)));
        notification.setActions(actions);

        NotificationSendingModule.notify(notification, service);
    }

    private void sendStatusNotification(String text)
    {
        PebbleNotification notification = new PebbleNotification(service.getString(R.string.voiceInputNotificationTitle), text, VOICE_NOTIFICATION_KEY);
        notification.setForceSwitch(true);

        NotificationSendingModule.notify(notification, service);
    }

    private static class RetryAction extends NotificationAction
    {
        WearVoiceAction voiceAction;

        public RetryAction(WearVoiceAction voiceAction)
        {
            super("Retry");
            this.voiceAction = voiceAction;
        }

        @Override
        public boolean executeAction(PebbleTalkerService service, ProcessedNotification notification)
        {
            voiceAction.showVoicePrompt(service);
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
            parcel.writeValue(voiceAction);
        }

        public static final Creator<RetryAction> CREATOR = new Creator<RetryAction>()
        {
            @Override
            public RetryAction createFromParcel(Parcel parcel)
            {
                WearVoiceAction voiceAction = (WearVoiceAction) parcel.readValue(getClass().getClassLoader());

                return new RetryAction(voiceAction);
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
        private WearVoiceAction voiceAction;

        public VoiceConfirmAction(String title, String text, WearVoiceAction voiceAction)
        {
            super(title);
            this.text = text;
            this.voiceAction = voiceAction;
        }

        @Override
        public boolean executeAction(PebbleTalkerService service, ProcessedNotification notification)
        {
            DismissUpwardsModule.dismissNotification(service, VOICE_NOTIFICATION_KEY);
            voiceAction.sendReply(text, service);
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
            parcel.writeValue(text);
            parcel.writeValue(voiceAction);
        }

        public static final Creator<VoiceConfirmAction> CREATOR = new Creator<VoiceConfirmAction>()
        {
            @Override
            public VoiceConfirmAction createFromParcel(Parcel parcel)
            {
                String title = (String) parcel.readValue(String.class.getClassLoader());
                String text = (String) parcel.readValue(String.class.getClassLoader());
                WearVoiceAction voiceAction = (WearVoiceAction) parcel.readValue(WearVoiceAction.class.getClassLoader());

                return new VoiceConfirmAction(title, text, voiceAction);
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
