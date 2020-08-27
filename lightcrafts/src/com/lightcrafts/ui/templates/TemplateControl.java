/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateDatabaseListener;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.ui.HorizontalMouseWheelSupport;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.toolkit.PaneTitle;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.event.MouseWheelEvent;

import static com.lightcrafts.ui.templates.Locale.LOCALE;

/**
 * Maintain a selectable list of Templates and associated controls.  Update
 * controls in an Editor as the selection changes.
 */
public class TemplateControl
    extends JPanel implements TemplateDatabaseListener, HorizontalMouseWheelSupport
{
    private final static String ControlTitle =
        LOCALE.get("TemplateControlTitle");

    private Editor editor;

    private JScrollPane scroll;
    private TemplatePreview preview;
    private TemplateTree tree;
    private PlusButton plus;

    private TemplateControlListener listener;

    /**
     * The Editor argument may be null, for a disabled control.
     */
    public TemplateControl(Editor editor, TemplateControlListener listener) {
        this.editor = editor;
        initialize();
        TemplateDatabase.addListener(this);
        this.listener = listener;
        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setOpaque(true);
        setBorder(LightZoneSkin.getPaneBorder());
        setEnabled(true);
    }

    public void clearSelection() {
        tree.clearSelection();
    }

    // Exposed only so the tree's bounds can be determined for the purpose of
    // dispatching our custom horizontal-scroll mouse wheel events.
    public JComponent getScrollPane() {
        return scroll;
    }

    @Override
    public JComponent getHorizontalMouseWheelSupportComponent() {
        return getScrollPane();
    }

    // Special handling for Mighty Mouse and two-finger trackpad
    // horizontal scroll events
    @Override
    public void horizontalMouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() >= 2) {
            if (scroll.isWheelScrollingEnabled()) {
                JScrollBar bar = scroll.getHorizontalScrollBar();
                int dir = e.getWheelRotation() < 0 ? -1 : 1;
                int inc = bar.getUnitIncrement(dir);
                int value = bar.getValue() - e.getWheelRotation() * inc;
                bar.setValue(value);
            }
        }
    }

    public void dispose() {
        TemplateDatabase.removeListener(this);
    }

    public void templatesChanged() {
        refresh();
    }

    // Reinitialize the list of Templates, for instance after detecting a
    // file modification.
    public void refresh() {
        removeAll();
        initialize();
        revalidate();
    }

    static class TemplatesPaneTitle extends PaneTitle {
        TemplatesPaneTitle(PlusButton plus, ManageButton manage) {
            Box labelBox = createLabelBox(ControlTitle);
            labelBox.add(manage);
            labelBox.add(Box.createHorizontalStrut(4));
            labelBox.add(plus);
            labelBox.add(Box.createHorizontalStrut(4));

            assembleTitle(labelBox);
        }
    }

    private void initialize() {
        try {
            TemplateRootNode root = new TemplateRootNode();
            if (editor != null) {
                tree = new TemplateTree(root, editor);
            }
            else {
                tree = new TemplateTree(root);
            }
        }
        catch (TemplateDatabase.TemplateException e) {
            e.printStackTrace();
        }
        if (tree != null) {
            scroll = new JScrollPane(tree);
        }
        else {
            String message = LOCALE.get("TemplateDatabaseErrorMessage");
            JLabel label = new JLabel(message);
            scroll = new JScrollPane(label);
        }
        scroll.getViewport().setBackground(
            LightZoneSkin.Colors.ToolPanesBackground
        );
        scroll.setBorder(null);

        plus = new PlusButton(this);

        ManageButton manage = new ManageButton();
        manage.setPreferredSize(plus.getPreferredSize());

        preview = (editor != null) ?
            new TemplatePreview(editor.getEngine()) : new TemplatePreview();

        MouseInputListener previewListener =
            new TemplatePreviewMouseListener(tree, preview);
        tree.addMouseListener(previewListener);
        tree.addMouseMotionListener(previewListener);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(preview);
        add(new TemplatesPaneTitle(plus, manage));
        add(scroll);
    }

    /**
     * Report the currently active Template namespace.  This is used to
     * initialize the save-Template dialog.
     */
    public String getNamespace() {
        TemplateKey key = tree.getSelectedTemplateKey();
        return (key != null) ? key.getNamespace() : null;
    }

    /**
     * Set the active namespace to the given namespace.  This is used after
     * the save-Template dialog has accepted a new Template.
     */
    public void setNamespace(String namespace) {
        tree.setNamespace(namespace);
    }

    void plusButtonPressed() {
        if (listener != null) {
            listener.addTemplate();
        }
    }

    public static void main(String[] args) throws Exception {

        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        TemplateControl control = new TemplateControl(
            null,
            new TemplateControlListener() {
                public void addTemplate() {
                    System.out.println("add template");
                }
            }
        );
        JFrame frame = new JFrame("TemplateControl Test");
        frame.setContentPane(control);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
