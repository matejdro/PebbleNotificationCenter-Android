package com.matejdro.pebblenotificationcenter.util;

import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
  static public boolean isBetweenTimes(Calendar current, Calendar start, Calendar stop) {
    if (start == null || stop == null)
        return false;

    int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);
    int currentTime = currentHour * 100 + currentMinute;
    int startTime = start.get(Calendar.HOUR_OF_DAY) * 100 + start.get(Calendar.MINUTE);
    int stopTime = stop.get(Calendar.HOUR_OF_DAY) * 100 + stop.get(Calendar.MINUTE);

    if (currentTime < startTime || (currentTime > stopTime && currentTime < startTime)) {
      return false;
    }
    return true;
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
