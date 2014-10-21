package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class SpinnerItem extends BaseSettingItem
{
    private AppSetting associatedSetting;
    private int textResource;
    private int descriptionResource;
    private int spinnerItemsList;

    private TextView nameText;
    private TextView descriptionText;
    private Spinner spinner;
    private boolean enabled = true;

    protected PerAppActivity activity;

    public SpinnerItem(AppSettingStorage settingsStorage, AppSetting associatedSetting, int spinnerItemsList, int textResource, int descriptionResource)
    {
        super(settingsStorage);

        this.spinnerItemsList = spinnerItemsList;
        this.associatedSetting = associatedSetting;
        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
    }

    @Override
    public View getView(PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_spinner, null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        spinner = (Spinner) view.findViewById(R.id.spinner);

        nameText.setText(textResource);
        if (descriptionResource == 0)
            descriptionText.setVisibility(View.GONE);
        else
            descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    spinner.performClick();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, spinnerItemsList, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(settingsStorage.getInt(associatedSetting));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                settingsStorage.setInt(associatedSetting, i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        setEnabled(enabled);

        return view;
    }

    protected boolean getSavedValue()
    {
        return settingsStorage.getBoolean(associatedSetting);
    }

    protected void settingChanged(boolean change)
    {
        settingsStorage.setBoolean(associatedSetting, change);
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

        spinner.setEnabled(enabled);
    }
}
