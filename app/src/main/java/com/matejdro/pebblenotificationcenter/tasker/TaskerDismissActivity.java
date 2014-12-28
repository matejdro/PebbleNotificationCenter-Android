package com.matejdro.pebblenotificationcenter.tasker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;
import net.dinglisch.android.tasker.TaskerPlugin;

public class TaskerDismissActivity extends Activity {

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         setContentView(R.layout.activity_tasker_dismiss);

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
         if (action != 3)
             return;

         ((EditText) findViewById(R.id.packageText)).setText(bundle.getString("package"));
         ((EditText) findViewById(R.id.idText)).setText(bundle.getString("id"));
         ((EditText) findViewById(R.id.tagText)).setText(bundle.getString("tag"));
     }

     @Override
     public void onBackPressed()
     {
         Intent intent = new Intent();

         Bundle bundle = new Bundle();

         bundle.putInt("action", 3);

         String pkg = ((EditText) findViewById(R.id.packageText)).getText().toString();
         String id = ((EditText) findViewById(R.id.idText)).getText().toString();
         String tag = ((EditText) findViewById(R.id.tagText)).getText().toString();

         String description = getString(R.string.taskerDescriptionDismiss, pkg);

         bundle.putString("package", pkg);
         bundle.putString("id", id);
         bundle.putString("tag", tag);

         TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[] { "package", "id", "tag" });

         intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
         intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle);

         setResult(RESULT_OK, intent);

         super.onBackPressed();
     }
 }
