package com.matejdro.pebblenotificationcenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import org.json.JSONException;
import timber.log.Timber;

import java.util.UUID;

import static com.getpebble.android.kit.Constants.*;


public class DataReceiver extends BroadcastReceiver {

	public final static UUID pebbleAppUUID = UUID.fromString("0a7575eb-e5b9-456b-8701-3eacb62d74f1");
	
	public void receiveData(final Context context, final int transactionId, final PebbleDictionary data)
	{
		PebbleKit.sendAckToPebble(context, transactionId);

		int id = data.getUnsignedInteger(0).intValue() & 0xFF;

		Timber.d("Got packet %d", id);

		PebbleTalkerService.gotPacket(context, id, data);
	}
	
	

	public void onReceive(final Context context, final Intent intent) {				
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

		try {
			final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
			receiveData(context, transactionId, data);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	}

}
