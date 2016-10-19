package com.matejdro.pebblenotificationcenter.ui;

import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matejdro.pebblecommons.vibration.VibrationPatternPicker;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;

public class DialogVibrationPatternPicker extends DialogFragment
{
    private VibrationPatternPicker vibrationPatternPicker;
    private SharedPreferences sharedPreferences;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sharedPreferences = ((PreferenceActivity) getActivity()).getPreferenceManager().getSharedPreferences();

        String currentPattern = sharedPreferences.getString(PebbleNotificationCenter.PERIODIC_VIBRATION_PATTERN, "100");
        vibrationPatternPicker.setCurrentPattern(currentPattern);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.dialog_vibration_pattern, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {

        vibrationPatternPicker = (VibrationPatternPicker) view.findViewById(R.id.vibration_pattern_picker);

        view.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onOkPressed();
            }
        });

        view.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onCancelPressed();
            }
        });
    }

    private void onCancelPressed()
    {
        dismiss();
    }

    private void onOkPressed()
    {
        String newPattern = vibrationPatternPicker.validateAndGetCurrentPattern();
        if (newPattern != null)
        {
            dismiss();
            sharedPreferences.edit().putString(PebbleNotificationCenter.PERIODIC_VIBRATION_PATTERN, newPattern).apply();
        }
    }
}
