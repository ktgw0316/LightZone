/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.prefs;

import com.lightcrafts.ui.base.BasePresenter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

class LocalePresenter extends BasePresenter<LocaleContract.View>
        implements LocaleContract.ViewActions {

    @Override
    public void restore() {
        val lang = LocaleModel.setDefaultFromPreference();
        if (isViewAttached()) {
            val item = LanguageItemConverter.languageToItem(lang);
            mView.setSelectedItem(item);
        }
    }

    @Override
    public void commit() {
        if (! isViewAttached()) {
            return;
        }
        val item = mView.getSelectedItem();
        if (item == null || item.equals(LanguageItemConverter.DefaultItem)) {
            LocaleModel.removePreference();
        } else {
            val lang = LanguageItemConverter.itemToLanguage(item);
            LocaleModel.setPreference(lang);
        }
    }

    static class LanguageItemConverter {

        private static final String DefaultItem = "(System Default)";

        static final Map<String, String> availableLanguageItems;
        static {
            availableLanguageItems = LocaleModel.getAvailableLanguages().stream()
                    .map(l -> Map.entry(languageToItem(l), l))
                    .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (e1, e2) -> e1, LinkedHashMap::new));
        }

        @NotNull
        private static String languageToItem(@NotNull String language) {
            if (language.isEmpty()) {
                return DefaultItem;
            }
            val locale = new Locale(language);
            return locale.getDisplayLanguage(locale);
        }

        @NotNull
        private static String itemToLanguage(@NotNull String item) {
            return availableLanguageItems.get(item);
        }
    }
}
