package com.matejdro.pebblenotificationcenter.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.util.ListSerialization;

public class AppListFragment extends Fragment {

	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;

	private ListView listView;
	private AppListAdapter listViewAdapter;

	private Set<String> checkedApps;
	private List<AppInfoStorage> apps;

	private boolean showOnResume = false;

	public static AppListFragment newInstance(boolean showSystemApps) {
		AppListFragment fragment = new AppListFragment();
		
		Bundle args = new Bundle();
		args.putBoolean("showSystemApps", showSystemApps);
		fragment.setArguments(args);

		return fragment;
	}

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		checkedApps = Collections.synchronizedSet(new HashSet<String>());
		apps = Collections.synchronizedList(new ArrayList<AppInfoStorage>());

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();

		new AppLoadingTask().execute();

		return inflater.inflate(R.layout.fragment_app_list, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	private void showList()
	{
		if (!isResumed())
		{
			showOnResume = true;
			return;
		}

		View view = getView().findViewById(R.id.loadingBar);

		view.setVisibility(View.GONE);

		listView = (ListView) getView().findViewById(R.id.appList);
		listViewAdapter = new AppListAdapter();
		listView.setAdapter(listViewAdapter);
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
				convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_app_list_item, null);
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

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					holder.check.toggle();
				}
			});

			holder.check.setOnCheckedChangeListener(null);

			holder.check.setChecked(checkedApps.contains(appInfo.info.packageName));

			holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						checkedApps.add(appInfo.info.packageName);
						Timber.d("Added application %s to checkedApps, checkedApps.size() = %d", appInfo.info.packageName, checkedApps.size());
					}
					else
					{
						checkedApps.remove(appInfo.info.packageName);
						Timber.d("Removed application %s from checkedApps, checkedApps.size() = %d", appInfo.info.packageName, checkedApps.size());
					}
					ListSerialization.saveCollection(editor, checkedApps, PebbleNotificationCenter.SELECTED_PACKAGES);
					PebbleNotificationCenter.getInMemorySettings().markDirty();
				}
			});

			return convertView;
		}


	}

	private class AppLoadingTask extends AsyncTask<Void, Void, Void>
	{
		boolean showSystemApps = false;

		@Override
		protected void onPreExecute() {
			showSystemApps = getArguments().getBoolean("showSystemApps", false);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (getActivity() == null)
				return null;
			
			ListSerialization.loadCollection(preferences, checkedApps, PebbleNotificationCenter.SELECTED_PACKAGES);

			final PackageManager pm = getActivity().getPackageManager();
			List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

			for (ApplicationInfo packageInfo : packages) {
				boolean isSystemApp = ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
				if (isSystemApp == showSystemApps )
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
