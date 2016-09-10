package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class ResetDefaultsButtonItem extends BaseSettingItem
{
    private int textResource;
    private int descriptionResource;
    private int buttonResource;

    protected PerAppActivity activity;

    private TextView nameText;
    private TextView descriptionText;
    private Button button;

    private boolean enabled = true;

    public ResetDefaultsButtonItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource, int buttonResource)
    {
        super(settingsStorage, null);
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
        this.buttonResource = buttonResource;
    }



    @Override
    public View getView(final PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_button, null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        button = (Button) view.findViewById(R.id.button);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);
        button.setText(buttonResource);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!(settingsStorage instanceof SharedPreferencesAppStorage))
                    return;

                String pkg = ((SharedPreferencesAppStorage) settingsStorage).getAppPackage();
                SharedPreferences.Editor editor = activity.getSharedPreferences(SharedPreferencesAppStorage.getSharedPreferencesName(pkg), Context.MODE_MULTI_PROCESS).edit();
                editor.clear();
                editor.apply();

                activity.finish();
            }
        });

        setEnabled(enabled);

        return view;

    }

    @Override
    public boolean onClose()
    {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;

        if (activity == null)
            return;

        button.setEnabled(enabled);

        if (enabled)
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
        }
        else
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
        }
    }
}
