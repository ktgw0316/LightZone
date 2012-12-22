/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.jsheet;

import java.util.EventListener;

/*
 * @(#)SheetListener.java  1.0  26. September 2005
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

import java.util.*;
/**
 * SheetListener.
 *
 * @author  Werner Randelshofer
 * @version 1.0 26. September 2005 Created.
 */
public interface SheetListener extends EventListener {
    /**
     * This method is invoked, when the user selected an option on the
     * JOptionPane or the JFileChooser pane on the JSheet.
     */
    public void optionSelected(SheetEvent evt);
}
