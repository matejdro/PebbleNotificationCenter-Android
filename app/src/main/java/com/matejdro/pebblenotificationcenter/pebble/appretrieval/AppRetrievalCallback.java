package com.matejdro.pebblenotificationcenter.pebble.appretrieval;

import com.matejdro.pebblecommons.pebble.PebbleApp;

import java.util.Collection;
import java.util.List;

public interface AppRetrievalCallback
{
    void addApps(Collection<PebbleApp> apps);
}
