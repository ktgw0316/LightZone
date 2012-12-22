/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * @(#)SheetEvent.java  1.0  26. September 2005
 *
 * Copyright (c) 2005 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package com.lightcrafts.ui.jsheet;

import java.util.*;
import javax.swing.*;
/**
 * SheetEvent.
 *
 * @author  Werner Randelshofer
 * @version 1.0 26. September 2005 Created.
 */
public class SheetEvent extends EventObject {
    private JComponent pane;
    private int option;
    private Object value;
    private Object inputValue;

    /**
     * Creates a new instance.
     */
    public SheetEvent(JSheet source) {
        super(source);
    }
    /**
     * Creates a new instance.
     */
    public SheetEvent(JSheet source, JFileChooser fileChooser, int option, Object value) {
        super(source);
        this.pane = fileChooser;
        this.option = option;
        this.value = value;
    }
    /**
     * Creates a new instance.
     */
    public SheetEvent(JSheet source, JOptionPane optionPane, int option, Object value, Object inputValue) {
        super(source);
        this.pane = optionPane;
        this.option = option;
        this.value = value;
        this.inputValue = inputValue;
    }

    /**
     * Returns the pane on the sheet. This is either a JFileChooser or a
     * JOptionPane.
     */
    public JComponent getPane() {
        return pane;
    }
    /**
     * Returns the JFileChooser pane on the sheet.
     */
    public JFileChooser getFileChooser() {
        return (JFileChooser) pane;
    }
    /**
     * Returns the JOptionPane pane on the sheet.
     */
    public JOptionPane getOptionPane() {
        return (JOptionPane) pane;
    }
    /**
     * Returns the option that the JFileChooser or JOptionPane returned.
     */
    public int getOption() {
        return option;
    }
    /**
     * Returns the value that the JFileChooser or JOptionPane returned.
     */
    public Object getValue() {
        return value;
    }
    /**
     * Returns the input value that the JOptionPane returned, if it wants input.
     */
    public Object getInputValue() {
        return inputValue;
    }
}
