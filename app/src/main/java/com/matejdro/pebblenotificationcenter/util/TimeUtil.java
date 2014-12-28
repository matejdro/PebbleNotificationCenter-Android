package com.matejdro.pebblenotificationcenter.util;

import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
  static public boolean isBetweenTimes(Calendar current, Calendar start, Calendar stop) {
    if (start == null || stop == null)
        return false;

    return current.getTimeInMillis() >= start.getTimeInMillis() && current.getTimeInMillis() <= stop.getTimeInMillis();
  }

  static public boolean hasTimePassed(Date last, long timePassed) {
    if (last==null){
      return true;
    }
    Date now = new Date();
    long diffInMillis = now.getTime() - last.getTime();
    return diffInMillis > timePassed;
  }

}
