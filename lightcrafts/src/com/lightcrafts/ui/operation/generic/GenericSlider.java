/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.generic;

import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

/** This class takes a SliderConfig and combines a ConfiguredSlider and a
  * ConfiguredTextField based on a common ConfiguredBoundedRangeModel into an
  * aggregate control.  It provides some passthrough API for various parts of
  * JSlider, so it can be used a lot like that class.
  * <p>
  * GenericSlider is a Component, but its Components can also be used as
  * children in a GenericSliderContainer.  It works like this:
  * <br>
  * <code>
  *     GenericSlider slider = new GenericSlider(key, config);
  *     GenericSliderContainer container = new GenericSliderContainer();
  *     container.addGenericSlider(slider);
  * </code>
  * <br>
  * which has the side effect of removing all children from the
  * GenericSlider.
  */

public class GenericSlider extends JPanel {

    private ConfiguredBoundedRangeModel model;
    private JLabel label;
    private ConfiguredSlider slider;
    private ConfiguredTextField text;

    GenericSlider(String key, SliderConfig config) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        label = new JLabel(key + ":", JLabel.CENTER);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        slider = new ConfiguredSlider(config);
        slider.setPaintTicks(true);

        model = slider.getConfiguredModel();
        add(label);
        add(slider);
        if (config.hasText()) {
            DecimalFormat format = config.getDecimalFormat();
            text = new ConfiguredTextField(model, format);
            add(text);

            // Make mouse clicks on the slider give keyboard focus to
            // the text field:
            slider.setFocusable(false);
            slider.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent event) {
                        // This call is enqueued so it can defeat the
                        // focus-changing logic in the OpStack global
                        // AWTEventListener:
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    text.requestFocusInWindow();
                                }
                            }
                        );
                    }
                }
            );
            // Forward mouse wheel events to the text field, but only if it
            // has focus:
            text.addFocusListener(
                new FocusAdapter() {
                    public void focusGained(FocusEvent event) {
                        slider.addMouseWheelListener(text);
                    }
                    public void focusLost(FocusEvent event) {
                        slider.removeMouseWheelListener(text);
                    }
                }
            );
        }
    }

    // Exposed for GenericSliderContainer's specialized layout:
    Component getLabel() {
        return label;
    }

    // Exposed for GenericSliderContainer's specialized layout:
    Component getSlider() {
        return slider;
    }

    // Exposed for GenericSliderContainer's specialized layout:
    Component getText() {
        return text;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        slider.setEnabled(b);
        text.setEnabled(b);
    }

    void setConfiguredValue(double value) {
        model.setConfiguredValue(value);
    }

    double getConfiguredValue() {
        return model.getConfiguredValue();
    }

    void setSliderPosition(int value) {
        model.setValue(value);
    }

    int getSliderPosition() {
        return model.getValue();
    }

    void setBackgroundRecurse(Color color) {
        super.setBackground(color);
        label.setBackground(color);
        slider.setBackground(color);
        if (text != null) {
            text.setBackground(color);
        }
    }

    void setFontRecurse(Font font) {
        super.setFont(font);
        label.setFont(font);
        slider.setFont(font);
        if (text != null) {
            text.setFont(font);
        }
    }

    void addChangeListener(ChangeListener listener) {
        model.addChangeListener(listener);
    }

    void removeChangeListener(ChangeListener listener) {
        model.removeChangeListener(listener);
    }

    void addSliderMouseListener(MouseListener listener) {
        slider.addMouseListener(listener);
    }

    void removeSliderMouseListener(MouseListener listener) {
        slider.removeMouseListener(listener);
    }

    public static void main(String[] args) {
        SliderConfig config = new SliderConfig(
            0., 1., .5, .01, false, new DecimalFormat()
        );
        final GenericSlider slider = new GenericSlider("name", config);
        slider.addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    double value = slider.getConfiguredValue();
                    System.out.println(value);
                }
            }
        );
        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(slider);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 50);
        frame.setVisible(true);
    }
}
