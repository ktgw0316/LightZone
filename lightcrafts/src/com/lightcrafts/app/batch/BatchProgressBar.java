/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;

import org.jvnet.substance.SubstanceLookAndFeel;

class BatchProgressBar extends ProgressThread {

    static class BatchProgressIndicator implements ProgressIndicator {

        JProgressBar bar = new JProgressBar();

        BatchProgressIndicator() {
            bar.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);
            bar.setBorder(null);
        }

        public void incrementBy(final int delta) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        int value = bar.getValue();
                        bar.setValue(value + delta);
                    }
                }
            );
        }
        public void setIndeterminate(final boolean indeterminate) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        // If you make a determinate progress bar
                        // indeterminate, the "barber pole" is
                        // partially "frozen" from where the old
                        // value was to the right.  Set the value to
                        // the maximum value first as a workaround.
                        if (indeterminate) {
                            int max = bar.getMaximum();
                            bar.setValue(max);
                        }
                        else {
                            int min = bar.getMinimum();
                            bar.setValue(min);
                        }
                        bar.setIndeterminate(indeterminate);
                    }
                }
            );
        }
        public void setMaximum(final int maxValue) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        bar.setMaximum(maxValue);
                    }
                }
            );
        }
        public void setMinimum(final int minValue) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        bar.setMinimum(minValue);
                    }
                }
            );
        }
    }

    BatchProgressBar() {
        super(new BatchProgressIndicator());
    }

    JProgressBar getComponent() {
        BatchProgressIndicator indicator =
            (BatchProgressIndicator) getProgressIndicator();
        return indicator.bar;
    }

    void reset() {
        BatchProgressIndicator indicator =
            (BatchProgressIndicator) getProgressIndicator();
        indicator.setIndeterminate(false);
    }

    // ProgressThread requires this method, even though we're only extending
    // ProgressThread so we can provide a ProgressIndicator to Engine.write().
    public void run() {
    }
}
