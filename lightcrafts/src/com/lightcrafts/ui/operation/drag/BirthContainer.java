/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.*;
import java.awt.*;

class BirthContainer extends JPanel {

    private class BirthThread extends Thread {
        public void run() {
            int maxHeight = targetSize.height;
            for (int n=0; n<maxHeight; n+=20) {
                try {
                    Thread.sleep(50);
                    setPreferredSize(new Dimension(targetSize.width, n));
                    revalidate();
                }
                catch (InterruptedException e) {
                }
            }
            born();
        }
    }

    private Dimension targetSize;

    BirthContainer(JComponent child) {
        setLayout(new BorderLayout());
        add(child);
        targetSize = getPreferredSize();
        setPreferredSize(new Dimension(targetSize.width, 0));
        revalidate();
        Thread thread = new BirthThread();
        thread.start();
    }

    private void born() {
        JComponent parent = (JComponent) getParent();
        Component[] comps = parent.getComponents();
        Component child = getComponents()[0];
        for (int n=0; n<comps.length; n++) {
            Component comp = comps[n];
            if (comp == this) {
                parent.remove(comp);
                remove(child);
                parent.add(child);
            }
        }
        parent.revalidate();
    }
}
