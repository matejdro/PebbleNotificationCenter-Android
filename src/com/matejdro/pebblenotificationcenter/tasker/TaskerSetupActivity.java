package com.matejdro.pebblenotificationcenter.tasker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.dinglisch.android.tasker.TaskerPlugin;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.matejdro.pebblenotificationcenter.R;

public class TaskerSetupActivity extends Activity {

	private List<String> settingNames = new ArrayList<String>();
	private List<String> settingKeys = new ArrayList<String>();
	
	private List<String[]> settingCaptions = new ArrayList<String[]>();
	private List<String[]> settingValues = new ArrayList<String[]>();
	
	private SettingListAdapter settingAdapter; 
	private SettingCaptionListAdapter settingCaptionAdapter;
	
	private int pickedSetting = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_tasker_setup);

		loadSettings();
		
		Spinner spinner = (Spinner) findViewById(R.id.taskerActionPickerSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.tasker_actions, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				updateVisibility(arg2);

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});

		settingAdapter = new SettingListAdapter();
		spinner = (Spinner) findViewById(R.id.taskerSettingPickerSpinner);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(settingAdapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				pickedSetting = arg2;
				settingCaptionAdapter.notifyDataSetChanged();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {				
			}
		});
		
		settingCaptionAdapter = new SettingCaptionListAdapter();
		spinner = (Spinner) findViewById(R.id.taskerSettingValuePickerSpinner);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(settingCaptionAdapter);

		loadIntent();

	}

	protected void loadIntent() {
		Intent intent = getIntent();

		if (intent == null)
			return;

		Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
		if (bundle == null)
			return;

		int action = bundle.getInt("action");

		updateVisibility(action);
		((Spinner) findViewById(R.id.taskerActionPickerSpinner)).setSelection(action);

		if (action == 0)
		{
			((EditText) findViewById(R.id.titleText)).setText(bundle.getString("title"));
			((EditText) findViewById(R.id.subtitleText)).setText(bundle.getString("subtitle"));
			((EditText) findViewById(R.id.bodyText)).setText(bundle.getString("body"));

			((CheckBox) findViewById(R.id.storeInHistoryCheck)).setChecked(bundle.getBoolean("storeInHistory"));
		}
		else if (action == 1)
		{			
			boolean enable = bundle.getBoolean("value");
			((Spinner) findViewById(R.id.taskerSettingValuePickerSpinner)).setSelection(enable ? 0 : 1);

			String settingKey = bundle.getString("key");
			for (int i = 0; i < settingKeys.size(); i++)
			{
				if (settingKey.equals(settingKeys.get(i))){
					((Spinner) findViewById(R.id.taskerSettingPickerSpinner)).setSelection(i);
					break;
				}
			}

		}
	}

	public void cancel(View view)
	{
		setResult(RESULT_CANCELED);
		finish();
	}

	public void ok(View view)
	{
		Intent intent = new Intent();

		String description;
		Bundle bundle = new Bundle();

		if (((Spinner) findViewById(R.id.taskerActionPickerSpinner)).getSelectedItemPosition() == 0)
		{
			bundle.putInt("action", 0);

			description = populateNotifyIntent(bundle);
		}
		else
		{
			bundle.putInt("action", 1);

			description = populateSettingIntent(bundle);
		}

		intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
		intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle);

		setResult(RESULT_OK, intent);

		finish();
	}

	private void updateVisibility(int action)
	{
		if (action == 0)
		{
			findViewById(R.id.notifyLayout).setVisibility(View.VISIBLE);
			findViewById(R.id.settingLayout).setVisibility(View.GONE);
		}
		else
		{
			findViewById(R.id.notifyLayout).setVisibility(View.GONE);
			findViewById(R.id.settingLayout).setVisibility(View.VISIBLE);
		}
	}

	public String populateNotifyIntent(Bundle bundle)
	{
		String title = ((EditText) findViewById(R.id.titleText)).getText().toString();
		String subtitle = ((EditText) findViewById(R.id.subtitleText)).getText().toString();
		String body = ((EditText) findViewById(R.id.bodyText)).getText().toString(); 
		boolean storeInHistory = ((CheckBox) findViewById(R.id.storeInHistoryCheck)).isChecked();

		bundle.putString("title", title);
		bundle.putString("subtitle", subtitle);
		bundle.putString("body", body);
		bundle.putBoolean("storeInHistory", storeInHistory);

		if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this))
		{
			TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[] { "title", "subtitle", "body" });
		}
		
		String description = "Send notification - \"" + title + "\"";
		return description;
	}

	public String populateSettingIntent(Bundle bundle)
	{
		int pickedSettingValue = ((Spinner) findViewById(R.id.taskerSettingValuePickerSpinner)).getSelectedItemPosition();
		
		String settingName = settingNames.get(pickedSetting);
		String settingKey = settingKeys.get(pickedSetting);
		String settingValue = settingValues.get(pickedSetting)[pickedSettingValue];

		bundle.putString("key", settingKey);
		bundle.putString("value", settingValue);

		String description = settingName + " - " + settingCaptions.get(pickedSetting)[pickedSettingValue];

		return description;
	}

	private void loadSettings()
	{
		XmlResourceParser parser = getResources().getXml(R.xml.settings);
		try
		{
			while (true)
			{
				int element = parser.next();
				if (element == XmlPullParser.END_DOCUMENT)
					break;

				if (element != XmlPullParser.START_TAG)
					continue;

				if (parser.getName().equals("CheckBoxPreference"))
				{
					settingCaptions.add(new String[] { "Enabled", "Disabled" });
					settingValues.add(new String[] { "1", "0" });
				}
				else if (!parser.getName().equals("ListPreference"))
					continue;

				for (int i = 0; i < parser.getAttributeCount(); i++)
				{
					if (parser.getAttributeName(i).equals("title"))
					{
						settingNames.add(parser.getAttributeValue(i));
					}
					else if (parser.getAttributeName(i).equals("key"))
					{
						settingKeys.add(parser.getAttributeValue(i));
					}
					else if (parser.getAttributeName(i).equals("entries"))
					{
						int id = Integer.parseInt(parser.getAttributeValue(i).substring(1));
						settingCaptions.add(getResources().getStringArray(id));
					}
					else if (parser.getAttributeName(i).equals("entryValues"))
					{
						int id = Integer.parseInt(parser.getAttributeValue(i).substring(1));
						settingValues.add(getResources().getStringArray(id));
					}
				}
			}

		}
		catch (XmlPullParserException e)
		{
			e.printStackTrace();
			return;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public class SettingListAdapter extends BaseAdapter
	{

		@Override
		public int getCount() {
			return settingNames.size();
		}

		@Override
		public Object getItem(int position) {
			return settingNames.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = getLayoutInflater().inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

			((TextView) convertView).setText(settingNames.get(position));

			return convertView;
		}

	}
	
	public class SettingCaptionListAdapter extends BaseAdapter
	{																																																																																														
		@Override
		public int getCount() {
			return settingCaptions.get(pickedSetting).length;
		}

		@Override
		public Object getItem(int position) {
			return settingCaptions.get(pickedSetting);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = getLayoutInflater().inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

			((TextView) convertView).setText(settingCaptions.get(pickedSetting)[position]);

			return convertView;
		}

	}
}
