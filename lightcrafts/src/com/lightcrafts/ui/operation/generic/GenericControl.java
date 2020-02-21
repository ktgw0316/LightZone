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

import lombok.val;

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

        val box = Box.createVerticalBox();

        box.add(Box.createVerticalStrut(6));

        // Add all the choices:

        val choiceKeys = op.getChoiceKeys();
        for (val key : choiceKeys) {
            val values = new Vector<String>(op.getChoiceValues(key));
            val choice = new JComboBox(values);
            choice.addActionListener(choiceActionListener(key, choice));
            choice.addMouseWheelListener(
                    new MouseWheelListener() {
                        @Override
                        public void mouseWheelMoved(MouseWheelEvent e) {
                            val source = (JComboBox) e.getComponent();
                            if (!source.hasFocus()) {
                                return;
                            }
                            val ni = source.getSelectedIndex() + e.getWheelRotation();
                            if (ni >= 0 && ni < source.getItemCount()) {
                                source.setSelectedIndex(ni);
                            }
                        }
                    }
            );
            val oldChoice = choices.get(key);
            if (oldChoice != null) {
                choice.setSelectedItem(oldChoice.getSelectedItem());
            }
            choice.setBackground(Background);
            choice.setFont(ControlFont);
            choice.setPreferredSize(new Dimension(280, 15));

            val panel = new JPanel();
            val label = new JLabel(key + ":", JLabel.CENTER);
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

        val checkboxKeys = op.getCheckboxKeys();
        for (val key : checkboxKeys) {
            val userKey = getUserPresentableKey(key);
            val checkbox = new JCheckBox(userKey);
            checkbox.addItemListener(checkboxItemListener(key, checkbox));
            val oldCheckbox = checkboxes.get(key);
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
        val sliderContainer = new GenericSliderContainer();

        val sliderKeys = op.getSliderKeys();
        for (val key : sliderKeys) {
            val userKey = getUserPresentableKey(key);
            val config = op.getSliderConfig(key);
            val slider = new GenericSlider(userKey, config);
            slider.addChangeListener(sliderChangeListener(key, slider));
            val oldSlider = sliders.get(key);
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
                val value = slider.getConfiguredValue();
                op.setSliderValue(key, value);
            }
        };
    }

    protected ItemListener checkboxItemListener(
            final String key, final JCheckBox checkbox) {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                val value = checkbox.isSelected();
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
                val value = (String) choice.getSelectedItem();
                op.setChoiceValue(key, value);
                undoSupport.postEdit(key + " Choice");
            }
        };
    }

    protected void slewSlider(String key, double value) {
        val slider = sliders.get(key);
        if (slider != null) {
            slider.setConfiguredValue(value);
        }
    }

    // Find the user presentable version of the given slider or
    // checkbox key in the properties.  If none is configured, just
    // return the given String.
    private String getUserPresentableKey(String key) {
        val type = op.getType();
        val name = type.getName().replaceAll(" ", "").replaceAll("V[0-9]+\\Z", "");
        try {
            val propKey = name + "-" + key;
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
        val sliderNode = node.addChild(SliderTag);
        val sliderKeys = sliders.keySet();
        for (val key : sliderKeys) {
            val slider = sliders.get(key);
            val value = slider.getConfiguredValue();
            sliderNode.setAttribute(key, Double.toString(value));
        }
        val checkboxNode = node.addChild(CheckBoxTag);
        val checkboxKeys = checkboxes.keySet();
        for (val key : checkboxKeys) {
            val checkbox = checkboxes.get(key);
            val value = checkbox.isSelected();
            checkboxNode.setAttribute(key, value ? "True" : "False");
        }
        val choiceNode = node.addChild(ChoiceTag);
        val choiceKeys = choices.keySet();
        for (val key : choiceKeys) {
            val choice = choices.get(key);
            val value = (String) choice.getSelectedItem();
            choiceNode.setAttribute(key, value);
        }
    }

    @Override
    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        undoSupport.restoreStart();
        op.changeBatchStarted();
        if (node.hasChild(SliderTag)) {
            val sliderNode = node.getChild(SliderTag);
            val keys = sliders.keySet();
            for (val key : keys) {
                val slider = sliders.get(key);
                try {
                    val version = sliderNode.getVersion();
                    if ((version >= 3) || (version < 0)) {
                        val value = Double.parseDouble(sliderNode.getAttribute(key));
                        slider.setConfiguredValue(value);
                    } else {
                        val value = Integer.parseInt(sliderNode.getAttribute(key));
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
            val checkboxNode = node.getChild(CheckBoxTag);
            val keys = checkboxes.keySet();
            for (val key : keys) {
                val checkbox = checkboxes.get(key);
                val value = checkboxNode.getAttribute(key);
                checkbox.setSelected(value.equals("True"));
            }
        }
        if (node.hasChild(ChoiceTag)) {
            val choiceNode = node.getChild(ChoiceTag);
            val keys = choices.keySet();
            for (val key : keys) {
                val choice = choices.get(key);
                val value = choiceNode.getAttribute(key);
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
