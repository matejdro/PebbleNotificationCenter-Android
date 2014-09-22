package com.matejdro.pebblenotificationcenter.tasker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;
import net.dinglisch.android.tasker.TaskerPlugin;

public class TaskerNotificationActivity extends Activity {

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         setContentView(R.layout.activity_tasker_notification);

         loadIntent();
     }

     protected void loadIntent() {
         Intent intent = getIntent();

         if (intent == null)
             return;

         Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
         if (bundle == null)
             return;

         int action = bundle.getInt("action");
         if (action != 0)
             return;

         ((EditText) findViewById(R.id.titleText)).setText(bundle.getString("title"));
         ((EditText) findViewById(R.id.subtitleText)).setText(bundle.getString("subtitle"));
         ((EditText) findViewById(R.id.bodyText)).setText(bundle.getString("body"));

         ((CheckBox) findViewById(R.id.storeInHistoryCheck)).setChecked(bundle.getBoolean("storeInHistory"));
     }

     @Override
     public void onBackPressed()
     {
         Intent intent = new Intent();

         Bundle bundle = new Bundle();

         bundle.putInt("action", 0);

         String title = ((EditText) findViewById(R.id.titleText)).getText().toString();
         String subtitle = ((EditText) findViewById(R.id.subtitleText)).getText().toString();
         String body = ((EditText) findViewById(R.id.bodyText)).getText().toString();
         boolean storeInHistory = ((CheckBox) findViewById(R.id.storeInHistoryCheck)).isChecked();

         String description = getString(R.string.taskerDescriptionNotification, title);

         bundle.putString("title", title);
         bundle.putString("subtitle", subtitle);
         bundle.putString("body", body);
         bundle.putBoolean("storeInHistory", storeInHistory);

         TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[] { "title", "subtitle", "body" });

         intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
         intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle);

         setResult(RESULT_OK, intent);

         super.onBackPressed();
     }
 }
