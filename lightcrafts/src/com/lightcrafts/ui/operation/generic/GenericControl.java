/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation.generic;

import com.lightcrafts.model.GenericOperation;
import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GenericControl extends OpControl {

    // This resource bundle holds the user presentable forms of the
    // GenericOperation slider and checkbox keys.  (Choice keys are still
    // not localizable.)
    //
    // The format for the properties file is:
    //
    //     operationTypeName-keyString=userPresentableKeyString
    //
    // where "operationTypeName" is the String returned by
    // GenericOperation.getOperationType().getName().replaceAll(" ", "").

    private final static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/ui/operation/generic/GenericControl"
    );

    protected GenericOperation op;

    // GenericOperation settings keys mapped to their control Components:

    protected Map<String, GenericSlider> sliders = new HashMap<String, GenericSlider>();
    protected Map<String, JCheckBox> checkboxes = new HashMap<String, JCheckBox>();
    protected Map<String, JComboBox> choices = new HashMap<String, JComboBox>();

    public GenericControl(GenericOperation op, OpStack stack) {
        super(op, stack);
        operationChanged(op);
        readyForUndo();
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);

        this.op = (GenericOperation) operation;

        final var box = Box.createVerticalBox();

        box.add(Box.createVerticalStrut(6));

        // Add all the choices:

        final var choiceKeys = op.getChoiceKeys();
        for (final var key : choiceKeys) {
            final var values = new Vector<String>(op.getChoiceValues(key));
            final var choice = new JComboBox(values);
            choice.addActionListener(choiceActionListener(key, choice));
            choice.addMouseWheelListener(
                    new MouseWheelListener() {
                        @Override
                        public void mouseWheelMoved(MouseWheelEvent e) {
                            final var source = (JComboBox) e.getComponent();
                            if (!source.hasFocus()) {
                                return;
                            }
                            final var ni = source.getSelectedIndex() + e.getWheelRotation();
                            if (ni >= 0 && ni < source.getItemCount()) {
                                source.setSelectedIndex(ni);
                            }
                        }
                    }
            );
            final var oldChoice = choices.get(key);
            if (oldChoice != null) {
                choice.setSelectedItem(oldChoice.getSelectedItem());
            }
            choice.setBackground(Background);
            choice.setFont(ControlFont);
            choice.setPreferredSize(new Dimension(280, 15));

            final var panel = new JPanel();
            final var label = new JLabel(key + ":", JLabel.CENTER);
            label.setPreferredSize(new Dimension(50, 15));
            label.setHorizontalAlignment(JLabel.RIGHT);
            label.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
            panel.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
            panel.add(label);
            panel.add(choice);
            box.add(panel);

            choices.put(key, choice);
        }

        // Add all the checkboxes:

        final var checkboxKeys = op.getCheckboxKeys();
        for (final var key : checkboxKeys) {
            final var userKey = getUserPresentableKey(key);
            final var checkbox = new JCheckBox(userKey);
            checkbox.addItemListener(checkboxItemListener(key, checkbox));
            final var oldCheckbox = checkboxes.get(key);
            if (oldCheckbox != null) {
                checkbox.setSelected(oldCheckbox.isSelected());
            }
            checkbox.setBackground(Background);
            checkbox.setFont(ControlFont);
            box.add(checkbox);

            checkboxes.put(key, checkbox);
        }

        // Add all the sliders:

        // A special layout that aligns the GenericSlider pieces in rows
        // and columns:
        final var sliderContainer = new GenericSliderContainer();

        final var sliderKeys = op.getSliderKeys();
        for (final var key : sliderKeys) {
            final var userKey = getUserPresentableKey(key);
            final var config = op.getSliderConfig(key);
            final var slider = new GenericSlider(userKey, config);
            slider.addChangeListener(sliderChangeListener(key, slider));
            final var oldSlider = sliders.get(key);
            if (oldSlider != null) {
                slider.setConfiguredValue(oldSlider.getConfiguredValue());
            }
            slider.addSliderMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent event) {
                            op.changeBatchStarted();
                        }

                        @Override
                        public void mouseReleased(MouseEvent event) {
                            op.changeBatchEnded();
                            undoSupport.postEdit(key + " Slider");
                        }
                    }
            );
            slider.setBackgroundRecurse(Background);
            slider.setFontRecurse(ControlFont);
            sliderContainer.addGenericSlider(slider);

            sliders.put(key, slider);
        }
        sliderContainer.setBackground(Background);
        box.add(sliderContainer);

        box.add(Box.createVerticalStrut(6));

        setContent(box);

        undoSupport.initialize();
    }

    protected ChangeListener sliderChangeListener(
            final String key, final GenericSlider slider) {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                final var value = slider.getConfiguredValue();
                op.setSliderValue(key, value);
            }
        };
    }

    protected ItemListener checkboxItemListener(
            final String key, final JCheckBox checkbox) {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                final var value = checkbox.isSelected();
                op.setCheckboxValue(key, value);
                undoSupport.postEdit(key + " Checkbox");
            }
        };
    }

    protected ActionListener choiceActionListener(
            final String key, final JComboBox choice) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final var value = (String) choice.getSelectedItem();
                op.setChoiceValue(key, value);
                undoSupport.postEdit(key + " Choice");
            }
        };
    }

    protected void slewSlider(String key, double value) {
        final var slider = sliders.get(key);
        if (slider != null) {
            slider.setConfiguredValue(value);
        }
    }

    // Find the user presentable version of the given slider or
    // checkbox key in the properties.  If none is configured, just
    // return the given String.
    private String getUserPresentableKey(String key) {
        final var type = op.getType();
        final var name = type.getName().replaceAll(" ", "").replaceAll("V[0-9]+\\Z", "");
        try {
            final var propKey = name + "-" + key;
            return Resources.getString(propKey);
        }
        catch (MissingResourceException e) {
            return key;
        }
    }

    private final static String SliderTag = "Slider";
    private final static String CheckBoxTag = "Checkbox";
    private final static String ChoiceTag = "Choice";

    @Override
    public void save(XmlNode node) {
        super.save(node);
        final var sliderNode = node.addChild(SliderTag);
        final var sliderKeys = sliders.keySet();
        for (final var key : sliderKeys) {
            final var slider = sliders.get(key);
            final var value = slider.getConfiguredValue();
            sliderNode.setAttribute(key, Double.toString(value));
        }
        final var checkboxNode = node.addChild(CheckBoxTag);
        final var checkboxKeys = checkboxes.keySet();
        for (final var key : checkboxKeys) {
            final var checkbox = checkboxes.get(key);
            final var value = checkbox.isSelected();
            checkboxNode.setAttribute(key, value ? "True" : "False");
        }
        final var choiceNode = node.addChild(ChoiceTag);
        final var choiceKeys = choices.keySet();
        for (final var key : choiceKeys) {
            final var choice = choices.get(key);
            final var value = (String) choice.getSelectedItem();
            choiceNode.setAttribute(key, value);
        }
    }

    @Override
    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        undoSupport.restoreStart();
        op.changeBatchStarted();
        if (node.hasChild(SliderTag)) {
            final var sliderNode = node.getChild(SliderTag);
            final var keys = sliders.keySet();
            for (final var key : keys) {
                final var slider = sliders.get(key);
                try {
                    final var version = sliderNode.getVersion();
                    if ((version >= 3) || (version < 0)) {
                        final var value = Double.parseDouble(sliderNode.getAttribute(key));
                        slider.setConfiguredValue(value);
                    } else {
                        final var value = Integer.parseInt(sliderNode.getAttribute(key));
                        slider.setSliderPosition(value);
                    }
                } catch (NumberFormatException e) {
                    throw new XMLException(
                            "Value at attribute \"" + key + "\" is not a number", e
                    );
                }
            }
        }
        if (node.hasChild(CheckBoxTag)) {
            final var checkboxNode = node.getChild(CheckBoxTag);
            final var keys = checkboxes.keySet();
            for (final var key : keys) {
                final var checkbox = checkboxes.get(key);
                final var value = checkboxNode.getAttribute(key);
                checkbox.setSelected(value.equals("True"));
            }
        }
        if (node.hasChild(ChoiceTag)) {
            final var choiceNode = node.getChild(ChoiceTag);
            final var keys = choices.keySet();
            for (final var key : keys) {
                final var choice = choices.get(key);
                final var value = choiceNode.getAttribute(key);
                choice.setSelectedItem(value);
            }
        }
        op.changeBatchEnded();
        undoSupport.restoreEnd();
    }

    @Override
    protected String getHelpTopic() {
        return op.getHelpTopic();
    }
}
