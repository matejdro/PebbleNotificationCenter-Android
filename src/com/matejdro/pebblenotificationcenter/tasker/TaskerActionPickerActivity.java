package com.matejdro.pebblenotificationcenter.tasker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.matejdro.pebblenotificationcenter.R;

/**
 * Created by Matej on 18.9.2014.
 */
public class TaskerActionPickerActivity extends Activity
{
    private static final Class[] actions = new Class[] {TaskerNotificationActivity.class, TaskerGlobalSettingsActivity.class, TaskerAppListActivity.class};
    private static final int TASKER_ACTION_REQUEST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasker_action_picker);

        loadIntent();
    }

    public void loadIntent()
    {
        Intent intent = getIntent();

        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        int action = bundle.getInt("action");
        if (action < 0 || action >= actions.length)
            return;

        loadNextScreen(actions[action], bundle);
    }

    public void notification(View view)
    {
        loadNextScreen(TaskerNotificationActivity.class, null);
    }

    public void globalSetting(View view)
    {
        loadNextScreen(TaskerGlobalSettingsActivity.class, null);
    }

    public void appSetting(View view)
    {
        loadNextScreen(TaskerAppListActivity.class, null);
    }

    private void loadNextScreen(Class cls, Bundle existingData)
    {
        Intent intent = new Intent(this, cls);
        if (existingData != null)
            intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", existingData);
        startActivityForResult(intent, TASKER_ACTION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}