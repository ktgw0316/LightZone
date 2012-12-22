/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.layout;

import com.lightcrafts.ui.toolkit.FadingContainer;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

/**
 * Combines a FadingContainer with some toggle buttons that control the
 * transitions among components.
 * <p>
 * FadingTabbedPanelListeners can find out about two particular transitions:
 * from something selected to nothing selected; and from nothing to something.
 */
public class FadingTabbedPanel extends JPanel {

    // The tab button labels can read upwards or downwards
    public enum Orientation { Up, Down }

    // When the first tab is opened or the last tab is closed, this is the
    // opposite transition color.
    private final static Color FadeAwayColor = LightZoneSkin.Colors.FrameBackground;

    // Respond to toggle button state changes by swapping components in the
    // FadingContainer, deselecting other toggle buttons, and making listener
    // callbacks.
    class TabListener implements ItemListener {

        private FadingContainer fader;
        boolean isDeselecting;

        public void itemStateChanged(ItemEvent event) {
            JToggleButton button = (JToggleButton) event.getSource();
            String name = button.getName();
            if (event.getStateChange() == ItemEvent.SELECTED) {
                JComponent comp = confs.get(name).comp;
                if (fader == null) {
                    fader = new FadingContainer(FadeAwayColor);
                    add(fader);
                    panelListener.somethingSelected();
                    validate();
                }
                fader.nextComponent(comp, null);
                if (selected != null) {
                    isDeselecting = true;
                    selected.setSelected(false);
                    isDeselecting = false;
                }
                selected = button;
                panelListener.tabSelected(name);
            }
            else {
                if (! isDeselecting) {
                    fader.nextComponent(
                        FadeAwayColor,
                        new ActionListener() {
                            public void actionPerformed(ActionEvent event) {
                                panelListener.nothingSelected();
                                remove(fader);
                                fader = null;
                            }
                        }
                    );
                    selected = null;
                }
            }
        }
    }

    private Map<String, FadingTabConfiguration> confs;
    private Collection<JToggleButton> buttons;
    private JToggleButton selected;
    private FadingTabbedPanelListener panelListener;
    private Box buttonBox;

    public FadingTabbedPanel(
        FadingTabConfiguration conf,
        Orientation orient,
        FadingTabbedPanelListener panelListener
    ) {
        this(Collections.singletonList(conf), orient, panelListener);
    }

    public JComponent getButtonContainer() {
        return buttonBox;
    }

    public String getSelected() {
        return (selected != null) ? selected.getName() : null;
    }

    public void setSelected(String name) {
        for (JToggleButton button : buttons) {
            if (button.getName().equals(name)) {
                button.setSelected(true);
            }
        }
    }

    public FadingTabbedPanel(
        List<FadingTabConfiguration> confs,
        Orientation orient,
        FadingTabbedPanelListener panelListener
    ) {
        this.panelListener = panelListener;

        buttons = new LinkedList<JToggleButton>();
        
        buttonBox = Box.createVerticalBox();
        // buttonBox.add(Box.createVerticalGlue());
        // buttonBox.add(Box.createVerticalStrut(8));

        TabListener tabListener = new TabListener();

        this.confs = new HashMap<String, FadingTabConfiguration>();

        for (FadingTabConfiguration conf : confs) {
            this.confs.put(conf.name, conf);
            JToggleButton button = new VerticalToggleButton(conf.name, orient);
            button.setToolTipText(conf.tip);
            button.setAlignmentY(.5f);
            button.setName(conf.name);
            button.addItemListener(tabListener);
            buttons.add(button);
            buttonBox.add(button);
        }
        buttonBox.add(Box.createVerticalGlue());

        setLayout(new BorderLayout());
    }

//    public Dimension getMinimumSize() {
//        Dimension min = new Dimension();
//        for (JComponent comp : comps.values()) {
//            Dimension size = comp.getMinimumSize();
//            min.width = Math.max(min.width, size.width);
//            min.height = Math.max(min.height, size.height);
//        }
//        return min;
//    }
//
//    public Dimension getMaximumSize() {
//        Dimension max = new Dimension();
//        for (JComponent comp : comps.values()) {
//            Dimension size = comp.getMinimumSize();
//            max.width = Math.min(max.width, size.width);
//            max.height = Math.min(max.height, size.height);
//        }
//        return max;
//    }
}
