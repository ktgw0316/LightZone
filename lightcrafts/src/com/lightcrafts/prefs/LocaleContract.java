/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.prefs;

interface LocaleContract {
    interface ViewActions {
    }

    interface LocaleView {
        void setSelectedItem(String item);
        String getSelectedItem();
    }
}
