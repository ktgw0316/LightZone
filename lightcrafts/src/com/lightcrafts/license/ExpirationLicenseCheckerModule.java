/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.Date;
import java.util.Calendar;

import com.lightcrafts.utils.Version;

/**
 * A <code>ExpirationLicenseChecker</code> checks that the current date
 * is within a limited time after the Version change date.
 */
final class ExpirationLicenseCheckerModule extends LicenseCheckerModule {

    private final static ResourceBundle Resources =
        ResourceBundle.getBundle(
            "com/lightcrafts/license/resources/ExpirationLicense"
        );

    private final static int DaysBeforeExpiration =
        Integer.parseInt(Resources.getString("DaysBeforeExpiration"));

    private final static String NeverExpireMessage =
        Resources.getString("NeverExpireMessage");

    private final static String DaysRemainingMessage =
        Resources.getString("DaysRemainingMessage");

    private final static String ExpiredMessage =
        Resources.getString("ExpiredMessage");

    public void run() {
        final Date expirationDate = getExpirationDate();
        if ( expirationDate != null ) {
            final Date now = new Date();
            final boolean expired = expirationDate.before( now );
            if ( expired ) {
                JOptionPane.showMessageDialog( null, ExpiredMessage );
                System.exit( 0 );
            }
        }
    }

    public String getMessage() {
        final Date expirationDate = getExpirationDate();
        if ( expirationDate == null ) {
            return NeverExpireMessage;
        }
        final Date now = new Date();
        final boolean expired = expirationDate.before( now );
        if ( expired ) {
            return ExpiredMessage;
        } else {
            final int remaining = getDaysRemaining();
            final String days = Integer.toString( remaining );
            return days.replaceFirst( "(.*)", DaysRemainingMessage );
        }
    }

    private static Date getExpirationDate() {
        final Calendar cal = Calendar.getInstance();
        final Date changeDate = Version.getChangeDate();
        if ( changeDate != null ) {
            cal.setTime( changeDate );
            cal.add( Calendar.DATE, DaysBeforeExpiration );
            return cal.getTime();
        }
        return null;
    }

    private static int getDaysRemaining() {
        final Date exp = getExpirationDate();
        final Date now = new Date();
        final long millis = exp.getTime() - now.getTime();
        return (int)Math.ceil( millis / (double)(1000 * 60 * 60 * 24) );
    }
}
/* vim:set et sw=4 ts=4: */
