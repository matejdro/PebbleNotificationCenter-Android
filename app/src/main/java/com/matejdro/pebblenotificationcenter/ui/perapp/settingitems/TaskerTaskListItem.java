package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;
import net.dinglisch.android.tasker.TaskerIntent;

/**
 * Created by Matej on 19.10.2014.
 */
public class TaskerTaskListItem extends ListItem implements ActivityResultItem
{
    public TaskerTaskListItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int textResource, int descriptionResource)
    {
        super(settingsStorage, associatedSetting, textResource, descriptionResource);
    }

    private boolean waitingForResult = false;
    private Button variablesButton;

    @Override
    protected void openAddDialog(String text)
    {
        try
        {
            activity.startActivityForResult(TaskerIntent.getTaskSelectIntent(), 0);
        } catch (ActivityNotFoundException e)
        {
            Toast.makeText(activity, R.string.errorNoTasker, Toast.LENGTH_LONG).show();
        }
        waitingForResult = true;
    }

    @Override
    protected void openEditDialog(final int id, String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(activity.getString(R.string.removeTaskConfirmation, text));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                remove(id);
            }
        });

        builder.setNegativeButton(R.string.no, null);
        builder.show();

    }

    @Override
    public View getView(final PerAppActivity activity)
    {
        View view = super.getView(activity);

        variablesButton = (Button) view.findViewById(R.id.variablesButton);
        variablesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setMessage(R.string.taskerVariablesDescription);
                builder.setPositiveButton(R.string.ok, null);

                builder.show();

            }
        });

        if (!enabled)
            setEnabled(false);

        return view;
    }

    @Override
    protected int getLayout()
    {
        return R.layout.setting_tasklist;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        if (activity != null && variablesButton != null)
            variablesButton.setEnabled(enabled);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (!waitingForResult)
            return;
        waitingForResult = false;

        if (resultCode != Activity.RESULT_OK || data == null)
            return;

        String task = data.getDataString();
        if (task != null)
            add(task);

    }
}
