/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.app.batch;

import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.thread.ProgressThread;

import javax.swing.*;
import java.awt.*;

class BatchProgressBar extends ProgressThread {

    static class BatchProgressIndicator implements ProgressIndicator {

        final JProgressBar bar = new JProgressBar();

        @Override
        public void incrementBy(final int delta) {
            EventQueue.invokeLater(() -> {
                final int value = bar.getValue();
                bar.setValue(value + delta);
            });
        }

        @Override
        public void setIndeterminate(final boolean indeterminate) {
            EventQueue.invokeLater(() -> {
                // If you make a determinate progress bar
                // indeterminate, the "barber pole" is
                // partially "frozen" from where the old
                // value was to the right.  Set the value to
                // the maximum value first as a workaround.
                final int value = indeterminate ? bar.getMaximum() : bar.getMinimum();
                bar.setValue(value);
                bar.setIndeterminate(indeterminate);
            });
        }

        @Override
        public void setMaximum(final int maxValue) {
            EventQueue.invokeLater(() -> bar.setMaximum(maxValue));
        }

        @Override
        public void setMinimum(final int minValue) {
            EventQueue.invokeLater(() -> bar.setMinimum(minValue));
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
    @Override
    public void run() {
    }
}
