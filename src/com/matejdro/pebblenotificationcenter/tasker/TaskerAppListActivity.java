package com.matejdro.pebblenotificationcenter.tasker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.ui.AppListFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskerAppListActivity extends Activity
{
	private ListView listView;
	private AppListAdapter listViewAdapter;

	private List<AppListFragment.AppInfoStorage> apps;
	private boolean showOnResume = false;
    private boolean resumed = true;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

		apps = Collections.synchronizedList(new ArrayList<AppListFragment.AppInfoStorage>());

		new AppLoadingTask().execute();

		setContentView(R.layout.fragment_app_list);
        loadIntent();
    }

    public void loadIntent()
    {
        Intent intent = getIntent();

        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        int action = bundle.getInt("action");
        if (action != 2)
            return;

        String pkg = bundle.getString("appPackage");
        if (pkg == null)
            return;

        String name = bundle.getString("appName");
        if (pkg == null)
            return;

        loadAppSettingsScreen(pkg, name, bundle);
    }

    private void loadAppSettingsScreen(String pkg, String name, Bundle bundle)
    {
        Intent intent = new Intent(this, TaskerAppSettingsActivity.class);
        intent.putExtra("appName", name);
        intent.putExtra("appPackage", pkg);

        if (bundle != null)
            intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }



    @Override
    public void onResume() {
        super.onResume();

        resumed = true;
        if (showOnResume && apps.size() > 0)
        {
            showOnResume = false;
            showList();
        }
    }

    @Override
    protected void onPause()
    {
        resumed = false;
        super.onPause();
    }


    private void showList()
	{
		if (!resumed)
		{
			showOnResume = true;
			return;
		}

		View view = findViewById(R.id.loadingBar);

		view.setVisibility(View.GONE);

		listView = (ListView) findViewById(R.id.appList);
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


			if (convertView == null)
            {
                convertView = getLayoutInflater().inflate(R.layout.tasker_app_list_item, null);
            }

			final AppListFragment.AppInfoStorage appInfo = apps.get(position);

            ((TextView) convertView).setText(appInfo.label);

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
                    loadAppSettingsScreen(appInfo.packageName, appInfo.label.toString(), null);
				}
			});

			return convertView;
		}


	}

	private class AppLoadingTask extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected Void doInBackground(Void... params) {
            Context context = getApplicationContext();

            final PackageManager pm = context.getPackageManager();
			List<PackageInfo> packages = pm.getInstalledPackages(0);

			for (PackageInfo packageInfo : packages) {
                if (packageInfo.packageName.equals(PebbleNotificationCenter.PACKAGE))
                    continue;

                try
				{
					ApplicationInfo appInfo = pm.getApplicationInfo(packageInfo.packageName, 0);
					
                    AppListFragment.AppInfoStorage storage = new AppListFragment.AppInfoStorage();

                    storage.packageName = appInfo.packageName;
                    storage.label = pm.getApplicationLabel(appInfo);

                    apps.add(storage);
				}
				catch (NameNotFoundException e)
				{
					continue;
				}
				
			}

			Collections.sort(apps, new AppListFragment.AppInfoComparator());

            apps.addAll(0, AppListFragment.getVirtualApps(context));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			showList();
		}
	}
}
