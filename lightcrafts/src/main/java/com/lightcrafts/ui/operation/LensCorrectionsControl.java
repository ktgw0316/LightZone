/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.GenericOperation;
import com.lightcrafts.ui.operation.generic.GenericControl;

import lombok.val;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static com.lightcrafts.model.ImageEditor.LensCorrectionsOperation.CAMERA_NAME;
import static com.lightcrafts.model.ImageEditor.LensCorrectionsOperation.LENS_NAME;

/**
 * Created by Masahiro Kitagawa on 2017/05/07.
 */
public class LensCorrectionsControl extends GenericControl {
    public LensCorrectionsControl(GenericOperation op, OpStack stack) {
        super(op, stack);
    }

    @Override
    protected ActionListener choiceActionListener(
            final String key, final JComboBox choice) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                val value = (String) choice.getSelectedItem();
                op.setChoiceValue(key, value);
                undoSupport.postEdit(key + " Choice");

                // Dynamically update lenses based on camera choice
                if (key.equals(CAMERA_NAME)) {
                    val _values = op.getChoiceValues(LENS_NAME);
                    val _choice = choices.get(LENS_NAME);
                    val _oldSelection = (String)_choice.getSelectedItem();
                    _choice.removeAllItems();
                    String _selection = null;
                    for (val _value : _values) {
                        _choice.addItem(_value);
                        if (_value.equals(_oldSelection)) {
                            _selection = _value;
                        }
                    }
                    _choice.setSelectedItem(_selection);
                }
            }
        };
    }

    @Override
    protected ItemListener checkboxItemListener(
            final String key, final JCheckBox checkbox) {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                val value = checkbox.isSelected();
                op.setCheckboxValue(key, value);
                undoSupport.postEdit(key + " Checkbox");

                // Enable/disable the choices and the sliders
                for (val key : choices.keySet()) {
                    choices.get(key).setEnabled(! value);
                }
                for (val key : sliders.keySet()) {
                    sliders.get(key).setEnabled(value);
                }
            }
        };
    }
}
