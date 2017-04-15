/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.prefs.Preferences;

public class SizeSlider extends Box {

    // The slider position is globally sticky.
    private final static Preferences Prefs =
        Preferences.userRoot().node("/com/lightcrafts/ui/browser/ctrls");
    private final static String SizeKey = "SizeSlider";

//    private final static Icon SmallIcon =
//        ButtonFactory.getIconByName("thumbshrink", 12);
//    private final static Icon BigIcon =
//        ButtonFactory.getIconByName("thumbgrow", 12);

    private final static int MinSize = 80;
    private final static int MaxSize = 320;
    private final static int DefaultSize = 144;

//    private JLabel small;
    private JSlider slider;
//    private JLabel big;

    public SizeSlider(final AbstractImageBrowser browser) {
        super(BoxLayout.X_AXIS);
//        small = new JLabel(SmallIcon);
//        big = new JLabel(BigIcon);
        slider = new JSlider(MinSize, MaxSize);
//        add(small);
        // add(new JLabel("Thumbnails"));
        add(slider);
//        add(big);

        // Don't let the slider stretch in wide layout environments.
        setMaximumSize(new Dimension(150, 150));
        setPreferredSize(new Dimension(150, 0));

        int size = Prefs.getInt(SizeKey, DefaultSize);
        slider.setValue(size);
        slider.setToolTipText(LOCALE.get("SizeToolTip"));
        slider.addChangeListener(
            new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int size = slider.getValue();
                    boolean isAdjusting = slider.getValueIsAdjusting();
                    browser.setCharacteristicSize(size, isAdjusting);
                    if (! isAdjusting) {
                        Prefs.putInt(SizeKey, size);
                    }
                }
            }
        );
        slider.setFocusable(false);
        slider.addMouseWheelListener(
                new MouseWheelListener() {
                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        slider.setValue(slider.getValue()
                                + (e.getWheelRotation() < 0 ? 1 : -1));
                    }
                }
        );
    }

    public int getValue() {
        return slider.getValue();
    }
}
