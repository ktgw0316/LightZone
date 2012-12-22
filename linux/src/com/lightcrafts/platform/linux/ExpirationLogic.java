/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

import static com.lightcrafts.platform.linux.Locale.LOCALE;
import com.lightcrafts.utils.TextUtil;

import javax.swing.*;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class ExpirationLogic {

    public static int DaysBeforeExpiration =
        Integer.parseInt(LOCALE.get("DaysBeforeExpiration"));

    // Return true if the expiration date has passed.
    public static boolean showExpirationDialog() {
        Date expiration = getExpirationDate();
        if (expiration == null) {
            return false;
        }
        Date trialRefDate = new Date();
        boolean expired = trialRefDate.after(expiration);
        final String messageText;
        if (expired) {
            messageText = LOCALE.get("TrialExpired");
        }
        else {
            Date midnight = getPrecedingMidnight(expiration);
            if (trialRefDate.compareTo(midnight) > 0) {
                final String timeString = getTimeOfDayString(expiration);
                messageText = LOCALE.get(
                    "TrialExpiresToday", timeString
                );
            }
            else {
                midnight = getPrecedingMidnight(midnight);
                if (trialRefDate.compareTo(midnight) > 0) {
                    final String timeString = getTimeOfDayString(expiration);
                    messageText = LOCALE.get(
                        "TrialExpiresTomorrow", timeString
                    );
                }
                else {
                    final long days = getDaysUntil(expiration);
                    messageText = LOCALE.get(
                        "TrialExpiresDays", Long.toString(days)
                    );
                }
            }
        }
        JOptionPane.showMessageDialog(null, messageText);

        return expired;
    }

    private static long getDaysUntil(Date date) {
        final Date givenMidnight = getPrecedingMidnight(date);
        final Date currentMidnight = getPrecedingMidnight(new Date());
        final long interval =
            givenMidnight.getTime() - currentMidnight.getTime();
        return interval / (24L * 60L * 60L * 1000L);
    }

    private static Date getPrecedingMidnight(Date date) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime() - 1000);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date getExpirationDate() {
        // Just make the thing die on May 1, 2008.
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date may_1_08 = cal.getTime();
        return may_1_08;
//        final Calendar cal = Calendar.getInstance();
//        final Date changeDate = Version.getChangeDate();
//        if (changeDate != null) {
//            cal.setTime(changeDate);
//            cal.add(Calendar.DATE, DaysBeforeExpiration);
//            return cal.getTime();
//        }
//        return null;
    }

    private static String getTimeOfDayString(Date date) {
        return TextUtil.dateFormat(
            DateFormat.getTimeInstance(DateFormat.SHORT), date
        );
    }
}
