package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;

/**
 * Created by Matej on 19.10.2014.
 */
public class WritingPhrasesItem extends ListItem
{
    public WritingPhrasesItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int textResource, int descriptionResource)
    {
        super(settingsStorage, associatedSetting, textResource, descriptionResource);
    }

    @Override
    protected void openAddDialog(String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final EditText editField = new EditText(activity);
        editField.setText(text);

        builder.setTitle(R.string.addWritingPhrase);
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

        builder.setTitle(R.string.editWritingPhrase);
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
