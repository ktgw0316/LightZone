/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.prefs;

interface LocaleContract {
    interface ViewActions {
        /**
         * Take the current values from the view and push them to preferences.
         */
        void commit();

        /**
         * Read the current preference values and use them to initialize the view.
         */
        void restore();
    }

    interface View {
        void setSelectedItem(String item);
        String getSelectedItem();
    }
}
