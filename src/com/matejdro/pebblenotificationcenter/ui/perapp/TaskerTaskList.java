package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import com.matejdro.pebblenotificationcenter.R;
import net.dinglisch.android.tasker.TaskerIntent;

/**
 * Created by Matej on 16.9.2014.
 */
public class TaskerTaskList extends ListSetting
{
    public TaskerTaskList(final Activity activity, int listLayoutId, int listAddButtonId, int listEmptyTextId)
    {
        super(activity, listLayoutId, listAddButtonId, listEmptyTextId);

        activity.findViewById(R.id.taskerActionsVariablesButton).setOnClickListener(new View.OnClickListener()
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
    }

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
    }

    public void onActivityResult(Intent data)
    {
        String task = data.getDataString();
        if (task != null)
            add(task);
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
}

