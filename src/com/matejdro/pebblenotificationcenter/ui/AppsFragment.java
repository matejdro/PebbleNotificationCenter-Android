package com.matejdro.pebblenotificationcenter.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.util.ListSerialization;

public class AppsFragment extends Fragment {
	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;
	private ListView listView;
	private AppListAdapter adapter;
	private boolean bulkEdit = false;

	private Set<String> checkedApps;
	private List<AppInfoStorage> apps;

	private boolean showOnResume = false;
	

	@Override
	public void onResume() {
		super.onResume();

		if (showOnResume && apps.size() > 0)
		{
			showOnResume = false;
			showList();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		checkedApps = Collections.synchronizedSet(new HashSet<String>());
		apps = Collections.synchronizedList(new ArrayList<AppInfoStorage>());

		this.setHasOptionsMenu(true);

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();

		new AppLoadingTask().execute();
		
		return inflater.inflate(R.layout.fragment_app_list, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		Spinner spinner = (Spinner) getActivity().findViewById(R.id.modePickerSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
				R.array.app_modes, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		boolean includingMode = preferences.getBoolean("includingMode", false);
		spinner.setSelection(includingMode ? 1 : 0);
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				boolean includingMode = arg2 == 1;
				
				editor.putBoolean("includingMode", includingMode);
				editor.apply();
				updateButton(includingMode);
				
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		updateButton(includingMode);
		
		super.onActivityCreated(savedInstanceState);
	}
	
	private void updateButton(boolean includingMode)
	{
		TextView view = (TextView) getActivity().findViewById(R.id.regexIntroductionTest);
	
		if (includingMode)
			view.setText("Pick apps you want to receive notifications from:");
		else
			view.setText("Pick apps you don't want to receive notifications from:");
	}




	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.checklist, menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (listView == null)
		{
			return false;
		}

		switch (item.getItemId())
		{
		case R.id.action_select_all:
			bulkEdit = true;
			for (int i = 0; i < apps.size(); i++)
			{
				checkedApps.add(apps.get(i).info.packageName);
			}
			adapter.notifyDataSetChanged();
			ListSerialization.saveCollection(editor, checkedApps, "CheckedApps");
			bulkEdit = false;
			return true;
		case R.id.action_select_none:
			bulkEdit = true;
			for (int i = 0; i < apps.size(); i++)
			{
				checkedApps.remove(apps.get(i).info.packageName);
			}
			adapter.notifyDataSetChanged();
			ListSerialization.saveCollection(editor, checkedApps, "CheckedApps");
			bulkEdit = false;
			return true;
		}

		return false;
	}

	private void showList()
	{
		if (!isResumed())
		{
			showOnResume = true;
			return;
		}
		
		View view = getActivity().findViewById(R.id.loadingBar);
		
		view.setVisibility(View.GONE);

		listView = (ListView) getActivity().findViewById(R.id.appList);
		adapter = new AppListAdapter();
		listView.setAdapter(adapter);
		listView.setVisibility(View.VISIBLE);		
	}


	private class AppListAdapter extends BaseAdapter
	{

		@Override
		public int getCount() {
			return apps.size();
		}

		@Override
		public Object getItem(int position) {
			return apps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			final ViewHolder holder;
			
			if (convertView == null)
			{
				convertView = getActivity().getLayoutInflater().inflate(R.layout.application_list_item, null);
				holder = new ViewHolder();
				
				holder.icon = (ImageView) convertView.findViewById(R.id.appImage);
				holder.name = (TextView) convertView.findViewById(R.id.appName);
				holder.check = (CheckBox) convertView.findViewById(R.id.appChecked);
				
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}

			final AppInfoStorage appInfo = apps.get(position);

			holder.name.setText(appInfo.label);
			holder.icon.setImageDrawable(appInfo.icon);
			
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					holder.check.toggle();
				}
			});

			holder.check.setOnCheckedChangeListener(null);
			
			holder.check.setChecked(checkedApps.contains(appInfo.info.packageName));
			
			holder.check.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked)
						checkedApps.add(appInfo.info.packageName);
					else
						checkedApps.remove(appInfo.info.packageName);

					if (!bulkEdit)
						ListSerialization.saveCollection(editor, checkedApps, "CheckedApps");
				}
			});


			return convertView;
		}


	}

	private class AppLoadingTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			ListSerialization.loadCollection(preferences, checkedApps, "CheckedApps");

			final PackageManager pm = getActivity().getPackageManager();
			List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

			boolean showSystem = preferences.getBoolean("showSystem", false);
			
			for (ApplicationInfo packageInfo : packages) {
				if (((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) || showSystem)
				{
					AppInfoStorage storage = new AppInfoStorage();

					storage.info = packageInfo;
					storage.icon = pm.getApplicationIcon(packageInfo);
					storage.label = pm.getApplicationLabel(packageInfo);

					apps.add(storage);
					//android.R.drawable.sym_def_app_icon
				}
			}

			Collections.sort(apps, new AppInfoComparator());

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			showList();
		}
	}	

	private static class AppInfoStorage
	{
		ApplicationInfo info;
		CharSequence label;
		Drawable icon;
	}

	private static class AppInfoComparator implements Comparator<AppInfoStorage>
	{

		@Override
		public int compare(AppInfoStorage lhs, AppInfoStorage rhs) {
			return lhs.label.toString().compareTo(rhs.label.toString());
		}

	}
	
	private static class ViewHolder
	{
		ImageView icon;
		TextView name;
		CheckBox check;
	}
}
