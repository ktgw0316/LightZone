/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.layout.ToggleTitleBorder;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class ModeButtons extends Box {

    private final static Icon ArrowIcon = IconFactory.createInvertedIcon(
        ModeButtons.class, "arrow.png"
    );
    private final static Icon CropIcon = IconFactory.createInvertedIcon(
        ModeButtons.class, "crop.png"
    );
    private final static Icon RotateIcon = IconFactory.createInvertedIcon(
        ModeButtons.class, "rotate.png"
    );
    private final static Icon RegionIcon = IconFactory.createInvertedIcon(
        ModeButtons.class, "regiongeneric.png"
    );

    private final static String ArrowTip = LOCALE.get("ArrowToolTip");
    private final static String CropTip = LOCALE.get("CropToolTip");
    private final static String RotateTip = LOCALE.get("RotateToolTip");
    private final static String RegionTip = LOCALE.get("RegionToolTip");

    private CoolToggleButton arrowButton;
    private CoolToggleButton cropButton;
    private CoolToggleButton rotateButton;
    private CoolToggleButton regionButton;

    // Each mode has a few extra controls that are visible only in the mode.
    private JComponent extrasContainer;

    ModeButtons(final ModeManager manager) {
        super(BoxLayout.X_AXIS);

        arrowButton = new CoolToggleButton();
        cropButton = new CoolToggleButton();
        rotateButton = new CoolToggleButton();
        regionButton = new CoolToggleButton();

        arrowButton.setIcon(ArrowIcon);
        cropButton.setIcon(CropIcon);
        rotateButton.setIcon(RotateIcon);
        regionButton.setIcon(RegionIcon);

        arrowButton.setToolTipText(ArrowTip);
        cropButton.setToolTipText(CropTip);
        rotateButton.setToolTipText(RotateTip);
        regionButton.setToolTipText(RegionTip);

        arrowButton.setStyle(CoolButton.ButtonStyle.LEFT);
        cropButton.setStyle(CoolButton.ButtonStyle.CENTER);
        rotateButton.setStyle(CoolButton.ButtonStyle.CENTER);
        regionButton.setStyle(CoolButton.ButtonStyle.RIGHT);

//        arrowButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
//        arrowButton.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);
//        cropButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
//        cropButton.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);
//        rotateButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
//        rotateButton.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);
//        regionButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
//        regionButton.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);

        final ButtonGroup group = new ButtonGroup();
        group.add(arrowButton);
        group.add(cropButton);
        group.add(rotateButton);
        group.add(regionButton);

        arrowButton.setSelected(true);

        arrowButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        removeExtras();
                        final JComponent extras = manager.setNoMode();
                        addExtras(extras);
                    }
                }
            }
        );
        cropButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        removeExtras();
                        manager.setNoMode();
                        final JComponent extras = manager.setCropMode();
                        addExtras(extras);
                    }
                }
            }
        );
        rotateButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        removeExtras();
                        manager.setNoMode();
                        final JComponent extras = manager.setRotateMode();
                        addExtras(extras);
                    }
                }
            }
        );
        regionButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        removeExtras();
                        manager.setNoMode();
                        final JComponent extras = manager.setRegionMode();
                        addExtras(extras);
                    }
                }
            }
        );
        final Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(arrowButton);
        buttonBox.add(cropButton);
        buttonBox.add(rotateButton);
        buttonBox.add(regionButton);

        ToggleTitleBorder.setBorder(buttonBox, LOCALE.get("ModeBorderTitle"));

        add(buttonBox);

        // The "extras" come and go; leave some space for them.
        extrasContainer = Box.createHorizontalBox();
/*
        extrasContainer.setBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED)
        );
*/
        // An outer holder for the "extras", just to bear a titled border that
        // can enforce matching vertical alignments in the editor toolbar.
        final JPanel extrasOuter = new JPanel(new BorderLayout());
        extrasOuter.add(extrasContainer);
        ToggleTitleBorder.setBorder(extrasOuter, " ");

        add(extrasOuter);

        final Dimension size = getPreferredSize();
        size.width = 350;
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }

    // Disabled buttons, for the no-Document display mode.
    ModeButtons() {
        this(null);
        arrowButton.setEnabled(false);
        cropButton.setEnabled(false);
        rotateButton.setEnabled(false);
        regionButton.setEnabled(false);
    }

    boolean isCropSelected() {
        return cropButton.isSelected();
    }

    boolean isRotateSelected() {
        return rotateButton.isSelected();
    }

    // The crop mode likes to pop itself, from the "Commit" popup menu item.
    void clickNoMode() {
        arrowButton.doClick();
    }

    void clickCropButton() {
        cropButton.doClick();
    }

    void clickRotateButton() {
        rotateButton.doClick();
    }

    void clickRegionButton() {
        regionButton.doClick();
    }

    // Locked tools and RAW adjustment tools don't allow regions.
    void setRegionsEnabled(boolean enabled) {
        regionButton.setEnabled(enabled);
    }

    private void removeExtras() {
        extrasContainer.removeAll();
        revalidate();
        repaint();
    }

    private void addExtras(JComponent extras) {
        if (extras != null) {
            extrasContainer.add(extras);
            revalidate();
            repaint();
        }
    }
}
