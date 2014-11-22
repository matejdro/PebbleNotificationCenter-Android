package com.matejdro.pebblenotificationcenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import java.util.UUID;
import org.json.JSONException;

import static com.getpebble.android.kit.Constants.APP_UUID;
import static com.getpebble.android.kit.Constants.MSG_DATA;
import static com.getpebble.android.kit.Constants.TRANSACTION_ID;


public class DataReceiver extends BroadcastReceiver {

	public final static UUID pebbleAppUUID = UUID.fromString("0a7575eb-e5b9-456b-8701-3eacb62d74f1");
	
	public void receiveData(final Context context, final int transactionId, final String jsonPacket)
	{
		PebbleKit.sendAckToPebble(context, transactionId);

        Intent intent = new Intent(context, PebbleTalkerService.class);
        intent.putExtra("packet", jsonPacket);
        context.startService(intent);
	}
	
	

	public void onReceive(final Context context, final Intent intent) {
        if ("com.getpebble.action.PEBBLE_CONNECTED".equals(intent.getAction()))
        {
            Intent startIntent = new Intent(context, PebbleTalkerService.class);
            startIntent.putExtra("PebbleConnected", true);
            context.startService(startIntent);

            return;
        }

		final UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);

		// Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
		if (!pebbleAppUUID.equals(receivedUuid)) {
			return;
		}

		final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
		final String jsonData = intent.getStringExtra(MSG_DATA);
		if (jsonData == null || jsonData.isEmpty()) {
			return;
		}

        receiveData(context, transactionId, jsonData);

        try {
			final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	}

}
