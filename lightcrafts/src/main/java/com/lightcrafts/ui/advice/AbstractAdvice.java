/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.advice;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractAdvice implements Advice {

    private JFrame parent;

    protected AbstractAdvice(JFrame parent) {
        this.parent = parent;
    }

    public JFrame getOwner() {
        return parent;
    }

    public Point getLocation() {
        return null;
    }
}
