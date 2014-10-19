package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.content.Intent;

/**
 * Created by Matej on 19.10.2014.
 */
public interface ActivityResultItem
{
    public void onActivityResult(int requestCode, int resultCode, Intent data);
}
