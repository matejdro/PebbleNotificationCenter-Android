package com.matejdro.pebblenotificationcenter.pebble.appretrieval;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.matejdro.pebblecommons.pebble.PebbleApp;
import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblenotificationcenter.R;

import java.net.URISyntaxException;
import java.util.List;

public class Sdk2AppRetrieval
{
    private Context context;
    private AppRetrievalCallback appRetrievalCallback;

    public Sdk2AppRetrieval(Context context, AppRetrievalCallback appRetrievalCallback)
    {
        this.context = context;
        this.appRetrievalCallback = appRetrievalCallback;
    }

    public void retrieveApps()
    {
        new GetAppsTask().execute((Void[]) null);

    }

    private class GetAppsTask extends AsyncTask<Void, Void, List>
    {
        private ProgressDialog progressDialog;


        @Override
        protected void onPreExecute()
        {
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(context.getString(R.string.retrieving_apps));
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    cancel(false);
                }
            });

            progressDialog.show();
        }

        @Override
        protected List doInBackground(Void... params)
        {
            PebbleDeveloperConnection developerConnection = null;
            try
            {
                developerConnection = new PebbleDeveloperConnection(context);
                developerConnection.connectBlocking();

                if (developerConnection.isOpen())
                {
                    List<PebbleApp> installedPebbleApps = developerConnection.getInstalledPebbleApps();
                    if (installedPebbleApps == null)
                        return null;

                    installedPebbleApps.addAll(PebbleApp.getSystemApps(context));
                    developerConnection.close();

                    return installedPebbleApps;
                }
            } catch (URISyntaxException e)
            {
                e.printStackTrace();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (developerConnection != null)
                    developerConnection.close();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final List list)
        {
            progressDialog.hide();

            if (list == null)
            {
                new AlertDialog.Builder(context).setMessage(R.string.error_developer_connection).setPositiveButton(R.string.ok, null).show();
                return;
            }
            appRetrievalCallback.addApps(list);
        }
    }
}
