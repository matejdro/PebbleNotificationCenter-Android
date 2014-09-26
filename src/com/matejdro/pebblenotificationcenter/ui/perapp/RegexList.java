package com.matejdro.pebblenotificationcenter.ui.perapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Matej on 16.9.2014.
 */
public class RegexList extends ListSetting
{
    public RegexList(Activity activity, int listLayoutId, int listAddButtonId, int listEmptyTextId)
    {
        super(activity, listLayoutId, listAddButtonId, listEmptyTextId);
    }

    @Override
    protected void openAddDialog(String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final EditText editField = new EditText(activity);
        editField.setText(text);

        builder.setTitle(R.string.regexAddDialogTitle);
        builder.setView(editField);

        builder.setMessage(R.string.regexAddDialogText);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                validateAndAdd(editField.getText().toString());
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

        builder.setTitle(R.string.regexEditingDialogTItle);
        builder.setView(editField);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                validateAndUpdate(id, editField.getText().toString());
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

    public void validateAndAdd(final String text)
    {
        if (isRegexValid(text))
        {
            Pattern pattern = Pattern.compile(text);
            add(text);
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.invalidRegexPattern);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    openAddDialog(text);
                }
            });

            builder.show();
        }
    }

    public void validateAndUpdate(final int id, final String text)
    {
        if (isRegexValid(text))
        {

            Pattern pattern = Pattern.compile(text);
            update(id, text);
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.invalidRegexPattern);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    openEditDialog(id, text);
                }
            });

            builder.show();
        }
    }


    private static boolean isRegexValid(String text)
    {
        if (text.trim().isEmpty())
            return false;

        try
        {
            Pattern pattern = Pattern.compile(text);

            return true;
        }
        catch (PatternSyntaxException e)
        {
            return false;
        }
    }
}

