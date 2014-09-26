package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;

/**
 * Created by Matej on 16.9.2014.
 */
public class CannedResponseList extends ListSetting
{
    public CannedResponseList(Activity activity, int listLayoutId, int listAddButtonId, int listEmptyTextId)
    {
        super(activity, listLayoutId, listAddButtonId, listEmptyTextId);
    }

    @Override
    protected void openAddDialog(String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final EditText editField = new EditText(activity);
        editField.setText(text);

        builder.setTitle(R.string.addCannedResponse);
        builder.setView(editField);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                add(editField.getText().toString());
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    protected void openEditDialog(final int id, String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final EditText editField = new EditText(activity);
        editField.setText(text);

        builder.setTitle(R.string.editCannedResponse);
        builder.setView(editField);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                update(id, editField.getText().toString());
            }
        });

        builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                remove(id);
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();

    }
}

