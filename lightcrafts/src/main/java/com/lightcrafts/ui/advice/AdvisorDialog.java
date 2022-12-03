/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.advice;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class AdvisorDialog extends JDialog {

    // Make sure no more than one advice dialog shows at a time.
    private static AdvisorDialog Recent;

    private static Cursor OpenHand;
    private static Cursor ClosedHand;

    // Load cursors from resources
    static {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image;
        String path;
        URL url;

        Point hot = new Point(8, 8);

        path = "resources/OpenHand.png";
        url = AdvisorDialog.class.getResource(path);
        image = toolkit.createImage(url);
        OpenHand = toolkit.createCustomCursor(image, hot, "Drag");

        path = "resources/ClosedHand.png";
        url = AdvisorDialog.class.getResource(path);
        image = toolkit.createImage(url);
        ClosedHand = toolkit.createCustomCursor(image, hot, "Dragging");
    }

    // Preferences keep track of the nag counts, so the user isn't bothered
    // to many times with the same advice.
    private static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/advice"
    );
    private final static String AdviceHashKey = "AdviceHash";

    private Advice advice;

    public AdvisorDialog(Advice advice) {
        super(advice.getOwner());

        this.advice = advice;

        final AdviceContent content = new AdviceContent(advice);
        setContentPane(content);

        MouseInputListener listener = new MouseInputAdapter() {
            Point start;
            public void mousePressed(MouseEvent event) {
                start = event.getPoint();
                content.setCursorRecurse(ClosedHand);
            }
            public void mouseDragged(MouseEvent event) {
                Point loc = getLocation();
                loc.x += event.getPoint().x - start.x;
                loc.y += event.getPoint().y - start.y;
                setLocation(loc);
            }
            public void mouseReleased(MouseEvent event) {
                start = null;
                content.setCursorRecurse(OpenHand);
            }
            public void mouseEntered(MouseEvent event) {
                // wish we could set the cursor to OpenHand
            }
            public void mouseExited(MouseEvent event) {
                // wish we could put the cursor back how it was
            }
        };
        content.setCursorRecurse(OpenHand);
        content.addMouseInputListenerRecurse(listener);

        setModal(false);
        setFocusable(false);
        setUndecorated(true);
        // Prevent the Alloy L&F from adding its own window decoration:
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);

        pack();
    }

    public Advice getAdvice() {
        return advice;
    }

    public void advise() {
        int hash = advice.getMessage().hashCode();
        int count = Prefs.getInt(AdviceHashKey + hash, 0);
        int max = advice.getMaxCount();
        if ((max < 0) || (count < max)) {
            if (Recent != null) {
                Recent.setVisible(false);
            }
            Recent = this;
            Prefs.putInt(AdviceHashKey + hash, ++count);
            Point loc = advice.getLocation();
            if (loc != null) {
                setLocation(loc);
            }
            else {
                setLocationRelativeTo(getOwner());
            }
            setVisible(true);
        }
    }

    public static void advise(Advice advice) {
        AdvisorDialog dialog = new AdvisorDialog(advice);
        dialog.advise();
    }
    
    /**
     * Reset the counters that limit the number of times a piece of advice is
     * shown to users in an installation.  Returns true if there was no
     * BackingStoreException from preferences.
     */
    public static boolean clearNagCounts() {
        try {
            Prefs.removeNode();
        }
        catch (BackingStoreException e) {
            // Not a big deal.
            return false;
        }
        Prefs = Preferences.userRoot().node(
            "/com/lightcrafts/ui/advice"
        );
        return true;
    }

    public static void main(String[] args) {
        JFrame owner = new JFrame("owner");
        owner.getContentPane().setPreferredSize(new Dimension(640, 480));
        owner.pack();
        owner.setLocationRelativeTo(null);
        owner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        owner.setVisible(true);

        AbstractAdvice advice = new AbstractAdvice(owner) {
            public String getMessage() {
                return
                "The adapter which receives mouse events and mouse motion " +
                "events.  The methods in this class are empty; this class is " +
                "provided as a convenience for easily creating listeners by " +
                "extending this class and overriding only the methods of " +
                "interest.";
            }
            public int getMaxCount() {
                return -1;
            }
        };
        clearNagCounts();
        advise(advice);
    }
}
