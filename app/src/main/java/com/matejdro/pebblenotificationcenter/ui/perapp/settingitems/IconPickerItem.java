package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PictureDrawable;
import android.os.AsyncTask;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.common.io.ByteStreams;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.pebble.NativeNotificationIcon;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by Matej on 19.10.2014.
 */
public class IconPickerItem extends BaseSettingItem
{
    private static Pattern SVG_FIX_REPLACEMENT_PATTERN = Pattern.compile("<svg ([^>]*) display=\"none\"");

    private Map<String, SVG> svgCache = new ArrayMap<>();
    private Set<ImageView> currentlyRetrieving = Collections.synchronizedSet(new HashSet<ImageView>());

    private int textResource;
    private int descriptionResource;

    private TextView nameText;
    private TextView descriptionText;

    private ImageView iconView;
    private Button resetButton;
    private AlertDialog iconPickerDialog;
    private boolean enabled = true;

    protected PerAppActivity activity;

    private NativeNotificationIcon curIcon;

    public IconPickerItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource)
    {
        super(settingsStorage);

        this.textResource = textResource;
        this.descriptionResource = descriptionResource;
    }

    @Override
    public View getView(final PerAppActivity activity)
    {
        this.activity = activity;

        View view = activity.getLayoutInflater().inflate(R.layout.setting_icon, null);

        iconView = (ImageView) view.findViewById(R.id.iconDisplay);
        resetButton = (Button) view.findViewById(R.id.resetButton);

        nameText = (TextView) view.findViewById(R.id.name);
        descriptionText = (TextView) view.findViewById(R.id.description);

        nameText.setText(textResource);
        descriptionText.setText(descriptionResource);

        view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (enabled)
                    iconView.performClick();
            }
        });

        curIcon = settingsStorage.getEnum(AppSetting.NATIVE_NOTIFICATION_ICON);
        updateImageView();

        iconView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openImagePicker(activity);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                set((NativeNotificationIcon) AppSetting.NATIVE_NOTIFICATION_ICON.getDefault());
            }
        });

        setEnabled(enabled);

        return view;
    }

    private void openImagePicker(Activity activity)
    {
        GridView gridView = (GridView) activity.getLayoutInflater().inflate(R.layout.dialog_iconpicker, null);
        gridView.setAdapter(new IconAdapter());

        iconPickerDialog = new AlertDialog.Builder(activity)
                .setNegativeButton(R.string.cancel, null)
                .setView(gridView)
                .create();
        iconPickerDialog.show();
    }

    private void set(NativeNotificationIcon icon)
    {
        curIcon = icon;

        settingsStorage.setEnum(AppSetting.NATIVE_NOTIFICATION_ICON, icon);
        updateImageView();
    }

    private void updateImageView()
    {
        if (curIcon == NativeNotificationIcon.AUTOMATIC)
            iconView.setImageDrawable(new ColorDrawable(0xffdddddd));
        else
            loadSvgIcon(curIcon.name(), iconView);
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

        iconView.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    private class IconAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            //Ignore AUTOMATIC entry
            return NativeNotificationIcon.values().length - 1;
        }

        @Override
        public Object getItem(int position)
        {
            return null;
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
                convertView = activity.getLayoutInflater().inflate(R.layout.item_icon, parent, false);

            final NativeNotificationIcon icon = NativeNotificationIcon.values()[position + 1];

            ImageView image = (ImageView) convertView.findViewById(R.id.image);
            loadSvgIcon(icon.name(), image);

            convertView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    iconPickerDialog.dismiss();
                    set(icon);
                }
            });

            return convertView;
        }
    }

    private void loadSvgIcon(String imageName, ImageView imageView)
    {
        SVG cachedSvg = svgCache.get(imageName);
        if (cachedSvg != null)
        {
            imageView.setImageDrawable(new PictureDrawable(cachedSvg.renderToPicture()));
        }
        else
        {
            if (currentlyRetrieving.contains(imageView))
                return;
            currentlyRetrieving.add(imageView);

            imageView.setImageDrawable(null);
            new IconRetrievalTask(imageName, imageView).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    private class IconRetrievalTask extends AsyncTask<Void, Void, SVG>
    {
        private String imageName;
        private ImageView imageView;

        public IconRetrievalTask(String imageName, ImageView imageView)
        {
            this.imageName = imageName;
            this.imageView = imageView;
        }

        @Override
        protected SVG doInBackground(Void... params)
        {
            File cacheFolder = new File(activity.getCacheDir(), "pebbleicons");
            if (!cacheFolder.exists())
                //noinspection ResultOfMethodCallIgnored
                cacheFolder.mkdir();

            File cachedFile = new File(cacheFolder, imageName);

            if (cachedFile.exists())
            {
                try
                {
                    FileInputStream fileInputStream = new FileInputStream(cachedFile);
                    SVG svg = SVG.getFromInputStream(fileInputStream);
                    fileInputStream.close();

                    return svg;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    //Something is wrong with cached file. Lets clear it and retry downloading
                    //noinspection ResultOfMethodCallIgnored
                    cachedFile.delete();
                }
            }

            try
            {
                URL imageLinkUrl = new URL("http://developer.getpebble.com/assets/images/guides/timeline/" + imageName + ".svg");
                URLConnection connection = imageLinkUrl.openConnection();



                InputStream inputStream = connection.getInputStream();
                byte[] svgData = ByteStreams.toByteArray(inputStream);
                inputStream.close();

                //Some pebble icons have display="none" on the root node for some reason. Lets remove that.
                String svgString = new String(svgData);
                svgString = SVG_FIX_REPLACEMENT_PATTERN.matcher(svgString).replaceAll("<svg $1");
                svgData = svgString.getBytes();

                SVG svg = SVG.getFromInputStream(new ByteArrayInputStream(svgData));

                FileOutputStream cacheStream = new FileOutputStream(cachedFile);
                cacheStream.write(svgData);
                cacheStream.close();

                return svg;
            }
            catch (IOException ignored)
            {
                ignored.printStackTrace();
            }
            catch (SVGParseException e)
            {
                e.printStackTrace();
                CrashlyticsCore.getInstance().logException(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(SVG svg)
        {
            if (svg != null)
            {
                try
                {
                    imageView.setImageDrawable(new PictureDrawable(svg.renderToPicture()));
                    svgCache.put(imageName, svg);

                }
                catch (NullPointerException e)
                {
                    //renderToPicture() sometimes throws NPE.

                    Timber.e("SVG Error", e);
                    e.printStackTrace();
                }
            }

            currentlyRetrieving.remove(imageView);
        }
    }
}
