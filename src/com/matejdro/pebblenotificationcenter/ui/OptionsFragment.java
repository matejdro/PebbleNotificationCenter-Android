package com.matejdro.pebblenotificationcenter.ui;

import timber.log.Timber;
import android.support.v4.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;

/**
 * Created by jbergler on 25/11/2013.
 */
public class OptionsFragment extends Fragment {
	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();

		return inflater.inflate(R.layout.fragment_options, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		Spinner appModeSpinner = (Spinner) getActivity().findViewById(R.id.appModePickerSpinner);
		ArrayAdapter<CharSequence> appModeAdapter = ArrayAdapter.createFromResource(getActivity(),
				R.array.appModePickerModes, android.R.layout.simple_spinner_item);
		appModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		appModeSpinner.setAdapter(appModeAdapter);

		boolean appMode = preferences.getBoolean(PebbleNotificationCenter.APP_INCLUSION_MODE, false);
		appModeSpinner.setSelection(appMode ? 1 : 0);

		appModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				boolean appMode = position == 1;
				Timber.d("Configuring %s to %b", PebbleNotificationCenter.APP_INCLUSION_MODE, appMode);
				editor.putBoolean(PebbleNotificationCenter.APP_INCLUSION_MODE, appMode);
				editor.apply();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
		});

		super.onActivityCreated(savedInstanceState);
	}
}
