/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.thread.ProgressThread;

class ProgressIndicatorFactory {

    static ProgressIndicator create(ProgressThread thread, int maxValue) {
        if (thread != null) {
            final ProgressIndicator indicator = thread.getProgressIndicator();
            if (indicator != null) {
                indicator.setMaximum(maxValue);
                return indicator;
            }
        }
        return dummyIndicator;
    }

    private static final ProgressIndicator dummyIndicator = new ProgressIndicator() {
        @Override
        public void incrementBy(int delta) {
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {
        }

        @Override
        public void setMaximum(int maxValue) {
        }

        @Override
        public void setMinimum(int minValue) {
        }
    };
}
