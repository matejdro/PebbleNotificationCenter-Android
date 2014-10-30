package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;
import java.util.List;

/**
 * Created by Matej on 19.10.2014.
 */
public class IntentActionsItem extends ListItem
{
    public IntentActionsItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int textResource, int descriptionResource)
    {
        super(settingsStorage, associatedSetting, textResource, descriptionResource);
    }

    private Button variablesButton;

    private List<String> actions;

    @Override
    protected void openAddDialog(String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final View editView = activity.getLayoutInflater().inflate(R.layout.dialog_add_action_intent, null);
        final EditText nameField = (EditText) editView.findViewById(R.id.nameField);
        final EditText actionField = (EditText) editView.findViewById(R.id.actionField);

        builder.setTitle("Add intent action");
        builder.setView(editView);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String name = nameField.getText().toString();
                String action = actionField.getText().toString();

                if (action.trim().isEmpty())
                    return;
                if (name.trim().isEmpty())
                    name = action;

                add(name);
                actions.add(action);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    protected void load()
    {
        actions = settingsStorage.getStringList(AppSetting.INTENT_ACTIONS_ACTIONS);
        super.load();
    }

    @Override
    public boolean onClose()
    {
        if (changed)
            settingsStorage.setStringList(AppSetting.INTENT_ACTIONS_ACTIONS, actions);
        return super.onClose();
    }

    @Override
    protected void openEditDialog(final int id, String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final View editView = activity.getLayoutInflater().inflate(R.layout.dialog_add_action_intent, null);
        final EditText nameField = (EditText) editView.findViewById(R.id.nameField);
        final EditText actionField = (EditText) editView.findViewById(R.id.actionField);
        nameField.setText(text);
        actionField.setText(actions.get(id));
        builder.setTitle("Edit intent action");
        builder.setView(editView);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String name = nameField.getText().toString();
                String action = actionField.getText().toString();

                if (action.trim().isEmpty())
                    return;
                if (name.trim().isEmpty())
                    name = action;

                update(id, name);
                actions.set(id, action);
            }
        });

        builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                remove(id);
                actions.remove(id);
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
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

                builder.setMessage(R.string.intentExtrasDescription);
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
        return R.layout.setting_intent_list;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        if (activity != null && variablesButton != null)
            variablesButton.setEnabled(enabled);
    }
}
