package com.matejdro.pebblenotificationcenter.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.LruCache;
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
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;
import com.matejdro.pebblenotificationcenter.util.PreferencesUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListFragment extends Fragment {

	private static SharedPreferences preferences;
	private static SharedPreferences.Editor editor;
    private DefaultAppSettingsStorage settingsStorage;
    private SharedPreferences.OnSharedPreferenceChangeListener changeListener;

	private ListView listView;
	private AppListAdapter listViewAdapter;

	private List<AppInfoStorage> apps;
	private LruCache<String, Bitmap> iconCache;
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
		apps = Collections.synchronizedList(new ArrayList<AppInfoStorage>());

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();
		settingsStorage = new DefaultAppSettingsStorage(preferences, editor);

        changeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String s)
            {
                if (listViewAdapter != null)
                    listViewAdapter.notifyDataSetChanged();
            }
        };

        preferences.registerOnSharedPreferenceChangeListener(changeListener);

		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		iconCache = new LruCache<String, Bitmap>(maxMemory / 16) // 1/16th of device's RAM should be far enough for all icons
		{

			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount() / 1024;
			} 
			
		};
		new AppLoadingTask().execute();

		return inflater.inflate(R.layout.fragment_app_list, null);
	}

    @Override
    public void onDestroy()
    {
        preferences.unregisterOnSharedPreferenceChangeListener(changeListener);
        super.onDestroy();
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

        TextView header = new TextView(getActivity());
        header.setText(getString(R.string.appListDescription));
        int padding = (int)((10 * getResources().getDisplayMetrics().density) + 0.5);
        header.setPadding(padding, padding, padding, padding);
        listView.addHeaderView(header);

        listViewAdapter = new AppListAdapter();
		listView.setAdapter(listViewAdapter);
		listView.setVisibility(View.VISIBLE);
		listView.setScrollingCacheEnabled(true);
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

			holder.lastIndex = position;
			holder.name.setText(appInfo.label);
			
			Bitmap icon = iconCache.get(appInfo.packageName);
			if (icon == null)
			{
				new IconLoadingTask().execute(appInfo.packageName, holder, position);
				holder.icon.setImageDrawable(null);
			}
			else
				holder.icon.setImageBitmap(icon);

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
				    Intent activityIntent = new Intent(getActivity(), PerAppActivity.class);
                    activityIntent.putExtra("appName", appInfo.label);
                    activityIntent.putExtra("appPackage", appInfo.packageName);
                    startActivity(activityIntent);
				}
			});

            holder.check.setVisibility(appInfo.packageName.equals(AppSetting.VIRTUAL_APP_DEFAULT_SETTINGS) ? View.GONE : View.VISIBLE);

            holder.check.setOnCheckedChangeListener(null);
			holder.check.setChecked(settingsStorage.isAppChecked(appInfo.packageName));
			holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settingsStorage.setAppChecked(appInfo.packageName, isChecked);
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

            Context context = getActivity();

            tryMigratingCheckboxList();

            if (getActivity() == null)
                return null;

            final PackageManager pm = context.getPackageManager();
			List<PackageInfo> packages = pm.getInstalledPackages(0);

			for (PackageInfo packageInfo : packages) {
				try
				{
                    if (packageInfo.packageName.equals(PebbleNotificationCenter.PACKAGE))
                        continue;

					ApplicationInfo appInfo = pm.getApplicationInfo(packageInfo.packageName, 0);

					boolean isSystemApp = ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
					if (isSystemApp == showSystemApps )
					{
						AppInfoStorage storage = new AppInfoStorage();

						storage.packageName = appInfo.packageName;
						storage.label = pm.getApplicationLabel(appInfo);

						apps.add(storage);
					}
				}
				catch (NameNotFoundException e)
				{
					continue;
				}
				
			}

			Collections.sort(apps, new AppInfoComparator());

            apps.addAll(0, getVirtualApps(context));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			showList();
		}
	}
	
	private class IconLoadingTask extends AsyncTask<Object, Void, IconData>
	{

		@Override
		protected void onPostExecute(IconData result) {
			if (result != null && result.holder.lastIndex == result.position)
				result.holder.icon.setImageBitmap(iconCache.get(result.pkg));
		}

		@Override
		protected IconData doInBackground(Object... params) {
			if (getActivity() == null)
                return null;

            final PackageManager pm = getActivity().getPackageManager();
			
			try {
				Drawable icon = pm.getApplicationIcon((String) params[0]);
				if (!(icon instanceof BitmapDrawable))
					icon = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
				
				iconCache.put((String) params[0], ((BitmapDrawable) icon).getBitmap());					
			} catch (NameNotFoundException e) {
			}
			
			return new IconData((String) params[0], (ViewHolder) params[1], (Integer) params[2]);
		}
	}

    public static List<AppInfoStorage> getVirtualApps(Context context)
    {
        List<AppInfoStorage> virtualApps = new ArrayList<AppInfoStorage>(3);
        if (context == null)
            return virtualApps;


        AppInfoStorage app = new AppInfoStorage();
        app.label = context.getString(R.string.virtualAppDefaultSettings);
        app.packageName = AppSetting.VIRTUAL_APP_DEFAULT_SETTINGS;
        virtualApps.add(app);

        app = new AppInfoStorage();
        app.label = context.getString(R.string.virtualAppThirdParty);
        app.packageName = AppSetting.VIRTUAL_APP_THIRD_PARTY;
        virtualApps.add(app);

        app = new AppInfoStorage();
        app.label = context.getString(R.string.virtualAppTaskerReceiver);
        app.packageName = AppSetting.VIRTUAL_APP_TASKER_RECEIVER;
        virtualApps.add(app);

        return virtualApps;
    }

    public static class IconData
	{
		String pkg;
		ViewHolder holder;
		int position;
		
		public IconData(String pkg, ViewHolder holder, int position)
		{
			this.pkg = pkg;
			this.holder = holder;
			this.position = position;
		}
	}

	public static class AppInfoStorage
	{
		public String packageName;
		public CharSequence label;
	}

    public static class AppInfoComparator implements Comparator<AppInfoStorage>
	{

		@Override
		public int compare(AppInfoStorage lhs, AppInfoStorage rhs) {
			return lhs.label.toString().compareTo(rhs.label.toString());
		}

	}

	private static class ViewHolder
	{
		int lastIndex;
		ImageView icon;
		TextView name;
		CheckBox check;
	}

    private void tryMigratingCheckboxList()
    {
        if (!preferences.contains(PebbleNotificationCenter.SELECTED_PACKAGES))
            return;

        List<String> list = new ArrayList<String>();
        PreferencesUtil.loadCollection(preferences, list, PebbleNotificationCenter.SELECTED_PACKAGES);
        for (String pkg : list)
            settingsStorage.setAppChecked(pkg, true);

        String[] spammyPackageList = preferences.getAll().keySet().toArray(new String[0]);
        for (String s : spammyPackageList)
        {
            if (s.startsWith(PebbleNotificationCenter.SELECTED_PACKAGES))
            {
                editor.remove(s);
            }
        }

    }
}
