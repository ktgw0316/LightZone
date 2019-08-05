/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Scale;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

class CropBoundsControls extends Box implements ChangeListener, MouseListener {

    interface Listener {
        void cropBoundsChanged(
            CropBounds bounds, Scale scale, boolean isChanging
        );
    }

    private JSlider x;
    private JSlider y;
    private JSlider width;
    private JSlider height;
    private JSlider angle;
    private JSlider zoom;

    private boolean isChanging;

    private LinkedList<Listener> listeners;

    CropBoundsControls() {
        super(BoxLayout.Y_AXIS);

        listeners = new LinkedList<Listener>();

        x = addSlider(0, 100);
        y = addSlider(0, 100);
        width = addSlider(0, 100);
        height = addSlider(0, 100);
        angle = addSlider(-180, +180);
        zoom = addSlider(1, 100);
        zoom.setValue(10);
    }

    void addListener(Listener listener) {
        listeners.add(listener);
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        Rectangle2D rect = new Rectangle2D.Double(
            x.getValue(), y.getValue(), width.getValue(), height.getValue()
        );
        double radians = angle.getValue() * Math.PI / 180;
        CropBounds bounds = new CropBounds(rect, radians);

        Scale scale = new Scale(zoom.getValue() / 10f);

        for (Listener listener : listeners) {
            listener.cropBoundsChanged(bounds, scale, isChanging);
        }
    }

    private JSlider addSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.addChangeListener(this);
        slider.addMouseListener(this);
        add(slider);
        return slider;
    }

    public void stateChanged(ChangeEvent e) {
        notifyListeners();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        isChanging = true;
    }

    public void mouseReleased(MouseEvent e) {
        isChanging = false;
        notifyListeners();
    }
}
