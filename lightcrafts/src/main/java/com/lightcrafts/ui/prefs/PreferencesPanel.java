/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.application.prefs.PreferencesModel;
import com.lightcrafts.domain.prefs.InteractiveUpdateFrequency;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

public class PreferencesPanel extends JPanel {
    private static final String DefaultLanguageItem = "(System Default)";

    private final Consumer<PreferencesMessage> dispatch;
    private final HelpArea help;
    private final PreferencesHelpController helpController;
    private final JComboBox<String> languageCombo = new JComboBox<>();
    private final Map<String, String> languageByItem = new LinkedHashMap<>();
    private final JSlider memorySlider;
    private final JLabel memoryText = new JLabel();
    private final Dimension memoryTextSize;
    private final JComboBox<String> interactiveUpdateCombo = new JComboBox<>();
    private final JTextField scratchDirectory = new JTextField();
    private final JRadioButton localCacheButton = new JRadioButton();
    private final JRadioButton globalCacheButton = new JRadioButton();
    private final JCheckBox checkForUpdate = new JCheckBox();
    private final JRadioButton exportOriginalButton = new JRadioButton();
    private final JRadioButton exportRecentButton = new JRadioButton();
    private final JRadioButton saveOriginalButton = new JRadioButton();
    private final JRadioButton saveRecentButton = new JRadioButton();
    private final JCheckBox autoSave = new JCheckBox();
    private final JCheckBox integration = new JCheckBox();
    private final JCheckBox showToolTitles = new JCheckBox();
    private JTextField displayProfile;
    private JButton displayProfileChooser;
    private boolean rendering;

    PreferencesPanel(
        PreferencesModel initialModel,
        Consumer<PreferencesMessage> dispatch
    ) {
        this.dispatch = dispatch;
        setLayout(new GridBagLayout());
        help = new HelpArea();
        helpController = new PreferencesHelpController(help);

        int minimum = initialModel.general().minimumMemoryLimit();
        int maximum = initialModel.general().maximumMemoryLimit();
        if (maximum > minimum) {
            memorySlider = new JSlider(minimum, maximum);
        } else {
            memorySlider = new JSlider(0, 1000);
            memorySlider.setEnabled(false);
        }
        memorySlider.setMajorTickSpacing(maximum / 5);
        memorySlider.setMinorTickSpacing(maximum / 20);
        memorySlider.setPaintTicks(true);
        memorySlider.setFocusable(false);

        memoryText.setText(Integer.toString(maximum) + " MB");
        memoryText.setHorizontalAlignment(SwingConstants.RIGHT);
        memoryTextSize = memoryText.getPreferredSize();

        createLanguageItems(initialModel.availableLanguages());
        addControls(initialModel.general().displayProfileAvailable());
        addFootnote();
        render(initialModel);
    }

    private void createLanguageItems(Iterable<String> availableLanguages) {
        var items = new LinkedHashMap<String, String>();
        for (String language : availableLanguages) {
            String item = languageToItem(language);
            if (!items.containsKey(item)) {
                items.put(item, language);
            }
        }
        items.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .forEach(entry -> languageByItem.put(
                entry.getKey(), entry.getValue()
            ));
        languageByItem.keySet().forEach(languageCombo::addItem);
        languageCombo.setEditable(false);
    }

    @NotNull
    private static String languageToItem(@NotNull String language) {
        String normalized = language.trim();
        if (normalized.isEmpty()) {
            return DefaultLanguageItem;
        }
        var locale = new java.util.Locale(normalized);
        return locale.getDisplayLanguage(locale);
    }

    private void addControls(boolean displayProfileAvailable) {
        int row = 0;

        addRow(row++, "Language", languageCombo,
            new PreferencesHelpController.HelpMetadata(
                "Change the user interface language.", true
            ));

        Box memoryBox = Box.createHorizontalBox();
        memoryBox.add(memorySlider);
        memoryBox.add(Box.createHorizontalStrut(3));
        memoryBox.add(memoryText);
        addRow(row++, LOCALE.get("HeapLimitItemLabel"), memoryBox,
            help("HeapLimitItemHelp", true));

        addInteractiveUpdateItems();
        Box updateBox = Box.createHorizontalBox();
        updateBox.add(interactiveUpdateCombo);
        updateBox.add(Box.createHorizontalGlue());
        addRow(row++, LOCALE.get("UpdateInteractiveLabel"), updateBox,
            help("UpdateInteractiveHelp", false));

        scratchDirectory.setEditable(false);
        JButton scratchChooser = new JButton(
            LOCALE.get("ScratchFileItemButton")
        );
        scratchChooser.setToolTipText(
            LOCALE.get("ScratchFileItemToolTip")
        );
        scratchChooser.addActionListener(event ->
            dispatch.accept(new PreferencesMessage.ChooseScratchDirectory())
        );
        Box scratchBox = Box.createHorizontalBox();
        scratchBox.add(scratchDirectory);
        scratchBox.add(Box.createHorizontalStrut(6));
        scratchBox.add(scratchChooser);
        addRow(row++, LOCALE.get("ScratchFileItemLabel"), scratchBox,
            help("ScratchFileItemHelp", true));

        if (displayProfileAvailable) {
            displayProfile = new JTextField(20);
            displayProfile.setEditable(false);
            displayProfileChooser = new JButton(
                LOCALE.get("DisplayProfileItemButton")
            );
            displayProfileChooser.setToolTipText(
                LOCALE.get("DisplayProfileItemToolTip")
            );
            displayProfileChooser.addActionListener(event ->
                dispatch.accept(new PreferencesMessage.ChooseDisplayProfile())
            );
            Box displayBox = Box.createHorizontalBox();
            displayBox.add(displayProfile);
            displayBox.add(Box.createHorizontalStrut(6));
            displayBox.add(displayProfileChooser);
            addRow(row++, LOCALE.get("DisplayProfileItemLabel"), displayBox,
                help("DisplayProfileItemHelp", true));
        }

        ButtonGroup cacheGroup = new ButtonGroup();
        cacheGroup.add(localCacheButton);
        cacheGroup.add(globalCacheButton);
        localCacheButton.setText(LOCALE.get("BrowserCacheItemLocalOption"));
        globalCacheButton.setText(LOCALE.get("BrowserCacheItemGlobalOption"));
        localCacheButton.setFocusable(false);
        globalCacheButton.setFocusable(false);
        JButton clearCache = new JButton(
            LOCALE.get("BrowserCacheItemClearButton")
        );
        clearCache.setToolTipText(
            LOCALE.get("BrowserCacheItemClearToolTip")
        );
        clearCache.addActionListener(event ->
            dispatch.accept(new PreferencesMessage.ClearBrowserCache())
        );
        Box cacheBox = Box.createHorizontalBox();
        cacheBox.add(localCacheButton);
        cacheBox.add(globalCacheButton);
        cacheBox.add(Box.createHorizontalGlue());
        cacheBox.add(clearCache);
        cacheBox.add(Box.createHorizontalGlue());
        addRow(row++, LOCALE.get("BrowserCacheItemLabel"), cacheBox,
            help("BrowserCacheItemHelp", false));

        checkForUpdate.setFocusable(false);
        addRow(row++, LOCALE.get("CheckForUpdateItemLabel"), checkForUpdate,
            help("CheckForUpdateItemHelp", false));

        addDirectoryRow(
            row++, "ExportDirectoryItemLabel",
            "ExportDirectoryItemHelp", "ExportDirectoryOriginalButton",
            "ExportDirectoryRecentButton", true
        );
        addDirectoryRow(
            row++, "SaveDirectoryItemLabel",
            "SaveDirectoryItemHelp", "SaveDirectoryOriginalButton",
            "SaveDirectoryRecentButton", false
        );

        autoSave.setFocusable(false);
        addRow(row++, LOCALE.get("AutoSaveItemLabel"), autoSave,
            help("AutoSaveItemHelp", false));
        integration.setFocusable(false);
        addRow(row++, LOCALE.get("IntegrationLabel"), integration,
            help("IntegrationHelp", false));
        showToolTitles.setFocusable(false);
        addRow(row++, LOCALE.get("ShowToolTitlesItemLabel"), showToolTitles,
            help("ShowToolTitlesItemHelp", false));

        JButton clearPreferences = new JButton(
            LOCALE.get("ClearPrefsItemButton")
        );
        clearPreferences.addActionListener(event ->
            dispatch.accept(new PreferencesMessage.ClearPreferences())
        );
        Box clearBox = Box.createHorizontalBox();
        clearBox.add(clearPreferences);
        clearBox.add(Box.createHorizontalGlue());
        addRow(row, LOCALE.get("ClearPrefsItemLabel"), clearBox,
            help("ClearPrefsItemHelp", false));

        languageCombo.addItemListener(event -> {
            if (!rendering && event.getStateChange() == ItemEvent.SELECTED) {
                dispatch.accept(new PreferencesMessage.LanguageChanged(
                    languageForItem((String) event.getItem())
                ));
            }
        });
        memorySlider.addChangeListener(event -> {
            int value = memorySlider.getValue();
            memoryText.setText(value + " MB");
            memoryText.setPreferredSize(memoryTextSize);
            if (!rendering && memorySlider.isEnabled()) {
                dispatch.accept(new PreferencesMessage.MemoryLimitChanged(value));
            }
        });
        interactiveUpdateCombo.addItemListener(event -> {
            if (!rendering && event.getStateChange() == ItemEvent.SELECTED) {
                dispatch.accept(new PreferencesMessage.InteractiveUpdateChanged(
                    frequencyForLabel((String) event.getItem())
                ));
            }
        });
        localCacheButton.addActionListener(event -> {
            if (!rendering) {
                dispatch.accept(new PreferencesMessage.BrowserCacheScopeChanged(true));
            }
        });
        globalCacheButton.addActionListener(event -> {
            if (!rendering) {
                dispatch.accept(new PreferencesMessage.BrowserCacheScopeChanged(false));
            }
        });
        checkForUpdate.addActionListener(event -> dispatch.accept(
            new PreferencesMessage.CheckForUpdateChanged(
                checkForUpdate.isSelected()
            )
        ));
        autoSave.addActionListener(event -> dispatch.accept(
            new PreferencesMessage.AutoSaveChanged(autoSave.isSelected())
        ));
        integration.addActionListener(event -> dispatch.accept(
            new PreferencesMessage.IntegrationChanged(integration.isSelected())
        ));
        showToolTitles.addActionListener(event -> dispatch.accept(
            new PreferencesMessage.ToolTitlesChanged(
                showToolTitles.isSelected()
            )
        ));
    }

    private void addInteractiveUpdateItems() {
        interactiveUpdateCombo.addItem(LOCALE.get("UpdateFrequentLabel"));
        interactiveUpdateCombo.addItem(LOCALE.get("UpdateNormalLabel"));
        interactiveUpdateCombo.addItem(LOCALE.get("UpdateInfrequentLabel"));
        interactiveUpdateCombo.setEditable(false);
    }

    private void addDirectoryRow(
        int row,
        String labelKey,
        String helpKey,
        String originalKey,
        String recentKey,
        boolean export
    ) {
        var original = export ? exportOriginalButton : saveOriginalButton;
        var recent = export ? exportRecentButton : saveRecentButton;
        original.setText(LOCALE.get(originalKey));
        recent.setText(LOCALE.get(recentKey));
        original.setFocusable(false);
        recent.setFocusable(false);
        ButtonGroup group = new ButtonGroup();
        group.add(original);
        group.add(recent);
        original.addActionListener(event -> {
            if (!rendering) {
                dispatch.accept(export
                    ? new PreferencesMessage.ExportDirectoryModeChanged(true)
                    : new PreferencesMessage.SaveDirectoryModeChanged(true));
            }
        });
        recent.addActionListener(event -> {
            if (!rendering) {
                dispatch.accept(export
                    ? new PreferencesMessage.ExportDirectoryModeChanged(false)
                    : new PreferencesMessage.SaveDirectoryModeChanged(false));
            }
        });
        Box box = Box.createHorizontalBox();
        box.add(original);
        box.add(recent);
        addRow(row, LOCALE.get(labelKey), box, help(helpKey, false));
    }

    private void addRow(
        int row, String labelText, JComponent component,
        PreferencesHelpController.HelpMetadata metadata
    ) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.ipadx = 2;
        constraints.ipady = 4;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;
        JLabel label = new JLabel(labelText + ":");
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        add(label, constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        add(component, constraints);
        helpController.register(component, metadata);
    }

    private void addFootnote() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = getComponentCount() / 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 5, 0, 5);

        Dimension size = new Dimension(help.getPreferredSize().width, 70);
        help.setPreferredSize(size);
        add(help, constraints);
    }

    void render(PreferencesModel model) {
        rendering = true;
        try {
            var general = model.general();
            languageCombo.setSelectedItem(itemForLanguage(model.language()));
            if (memorySlider.isEnabled()) {
                memorySlider.setValue(general.memoryLimit());
            }
            interactiveUpdateCombo.setSelectedItem(
                labelForFrequency(general.interactiveUpdateFrequency())
            );
            scratchDirectory.setText(general.scratchDirectory());
            scratchDirectory.setEnabled(general.scratchDirectoryEnabled());
            if (displayProfile != null) {
                displayProfile.setText(general.displayProfilePath());
                displayProfile.setEnabled(general.displayProfileEnabled());
            }
            localCacheButton.setSelected(general.localBrowserCache());
            globalCacheButton.setSelected(!general.localBrowserCache());
            checkForUpdate.setSelected(general.checkForUpdate());
            exportOriginalButton.setSelected(general.exportByOriginal());
            exportRecentButton.setSelected(!general.exportByOriginal());
            saveOriginalButton.setSelected(general.saveByOriginal());
            saveRecentButton.setSelected(!general.saveByOriginal());
            autoSave.setSelected(general.autoSave());
            integration.setSelected(general.integrationEnabled());
            showToolTitles.setSelected(general.showToolTitles());
        } finally {
            rendering = false;
        }
    }

    private String itemForLanguage(String language) {
        return languageByItem.entrySet().stream()
            .filter(entry -> entry.getValue().equals(language))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unsupported language selection: " + language
            ));
    }

    private String languageForItem(String item) {
        String language = languageByItem.get(item);
        if (language == null) {
            throw new IllegalArgumentException(
                "Unsupported language selection: " + item
            );
        }
        return language;
    }

    private static InteractiveUpdateFrequency frequencyForLabel(String label) {
        if (label.equals(LOCALE.get("UpdateFrequentLabel"))) {
            return InteractiveUpdateFrequency.FREQUENT;
        }
        if (label.equals(LOCALE.get("UpdateNormalLabel"))) {
            return InteractiveUpdateFrequency.NORMAL;
        }
        if (label.equals(LOCALE.get("UpdateInfrequentLabel"))) {
            return InteractiveUpdateFrequency.INFREQUENT;
        }
        throw new IllegalArgumentException(
            "Unsupported interactive update choice: " + label
        );
    }

    private static String labelForFrequency(
        InteractiveUpdateFrequency frequency
    ) {
        return switch (frequency) {
            case FREQUENT -> LOCALE.get("UpdateFrequentLabel");
            case NORMAL -> LOCALE.get("UpdateNormalLabel");
            case INFREQUENT -> LOCALE.get("UpdateInfrequentLabel");
        };
    }

    private static PreferencesHelpController.HelpMetadata help(
        String key, boolean restart
    ) {
        return new PreferencesHelpController.HelpMetadata(
            LOCALE.get(key), restart
        );
    }
}
