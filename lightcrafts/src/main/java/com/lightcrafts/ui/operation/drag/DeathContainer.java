/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.*;
import java.awt.*;

class DeathContainer extends JPanel {

    private class DeathThread extends Thread {
        public void run() {
            int maxHeight = startSize.height;
            for (int n=maxHeight; n>=0; n-=20) {
                try {
                    Thread.sleep(50);
                    setPreferredSize(new Dimension(startSize.width, n));
                    revalidate();
                }
                catch (InterruptedException e) {
                }
            }
            die();
        }
    }

    private Dimension startSize;

    DeathContainer(JComponent child) {
        setLayout(new BorderLayout());
        add(child);
        startSize = child.getSize();
        setPreferredSize(startSize);
        Thread thread = new DeathThread();
        thread.start();
    }

    private void die() {
        JComponent parent = (JComponent) getParent();
        parent.remove(this);
        parent.revalidate();
    }
}
