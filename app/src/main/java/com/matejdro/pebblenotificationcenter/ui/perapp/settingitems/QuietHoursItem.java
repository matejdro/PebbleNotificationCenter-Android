package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.TimePickerDialog;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public class QuietHoursItem extends BaseSettingItem
{
    private int textResource;
    private int descriptionResource;

    private TextView nameText;
    private TextView descriptionText;
    private TextView enabledText;
    private CheckBox enabledCheckBox;
    private TextView startText;
    private Button startButton;
    private TextView endText;
    private Button endButton;

    private boolean enabled = true;

    protected PerAppActivity activity;

    public QuietHoursItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource)
    {
        super(settingsStorage, AppSetting.QUIET_TIME_ENABLED);

        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
    }

    @Override
    public View getView(final PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_quiethours, null);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);
        enabledText = (TextView) view.findViewById(R.id.enabledText);
        enabledCheckBox = (CheckBox) view.findViewById(R.id.enabledCheck);
        startText = (TextView) view.findViewById(R.id.startText);
        startButton = (Button) view.findViewById(R.id.startButton);
        endText = (TextView) view.findViewById(R.id.endText);
        endButton = (Button) view.findViewById(R.id.endButton);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    enabledCheckBox.toggle();
            }
        });

        enabledCheckBox.setChecked(settingsStorage.getBoolean(AppSetting.QUIET_TIME_ENABLED));
        enabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                settingsStorage.setBoolean(AppSetting.QUIET_TIME_ENABLED, isChecked);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int startHour = settingsStorage.getInt(AppSetting.QUIET_TIME_START_HOUR);
                int startMinute = settingsStorage.getInt(AppSetting.QUIET_TIME_START_MINUTE);

                TimePickerDialog dialog = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        settingsStorage.setInt(AppSetting.QUIET_TIME_START_HOUR, hourOfDay);
                        settingsStorage.setInt(AppSetting.QUIET_TIME_START_MINUTE, minute);

                        updateStartButtonText();
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(activity));
                dialog.show();
            }
        });

        endButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int endHour = settingsStorage.getInt(AppSetting.QUIET_TIME_END_HOUR);
                int endMinute = settingsStorage.getInt(AppSetting.QUIET_TIME_END_MINUTE);

                TimePickerDialog dialog = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        settingsStorage.setInt(AppSetting.QUIET_TIME_END_HOUR, hourOfDay);
                        settingsStorage.setInt(AppSetting.QUIET_TIME_END_MINUTE, minute);

                        updateEndButtonText();
                    }
                }, endHour, endMinute, DateFormat.is24HourFormat(activity));
                dialog.show();

            }
        });

        updateStartButtonText();
        updateEndButtonText();

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

        if (enabled)
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            enabledText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            startText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
            endText.setTextColor(activity.getResources().getColor(R.color.text_enabled));
        }
        else
        {
            nameText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            descriptionText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            enabledText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            startText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
            endText.setTextColor(activity.getResources().getColor(R.color.text_disabled));
        }

        enabledCheckBox.setEnabled(enabled);
        startButton.setEnabled(enabled);
        endButton.setEnabled(enabled);

    }

    public void updateStartButtonText()
    {
        int hours = settingsStorage.getInt(AppSetting.QUIET_TIME_START_HOUR);
        int minutes = settingsStorage.getInt(AppSetting.QUIET_TIME_START_MINUTE);
        startButton.setText(formatTime(hours, minutes));
    }

    public void updateEndButtonText()
    {
        int hours = settingsStorage.getInt(AppSetting.QUIET_TIME_END_HOUR);
        int minutes = settingsStorage.getInt(AppSetting.QUIET_TIME_END_MINUTE);
        endButton.setText(formatTime(hours, minutes));
    }

    public String formatTime(int hours, int minutes)
    {
        boolean hour24 = DateFormat.is24HourFormat(activity);


        int displayHours;

        if (!hour24)
            displayHours = hours % 12;
        else
            displayHours = hours;

        StringBuilder builder = new StringBuilder(hour24 ? 5 : 8);

        if (displayHours < 10)
            builder.append('0');
        builder.append(displayHours);

        builder.append(":");

        if (minutes < 10)
            builder.append('0');
        builder.append(minutes);

        if (!hour24)
        {
            if (hours > 12)
                builder.append(" PM");
            else
                builder.append(" AM");
        }

        return builder.toString();

    }

}
