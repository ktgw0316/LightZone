/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Maintain a selectable list of Templates and associated controls.  Update
 * controls in an Editor as the selection changes.
 */
public class TemplateTree extends JTree {

    // Namespace node collapsed/expanded states are sticky.
    private static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/templates"
    );
    private static String ExpandedKey = "TemplateNamespaceExpanded";

    private Editor editor;

    private TemplateTreePopup popup;

    private TemplateTreeNode selected;

    /**
     * A disabled TemplateTree, for the no-Document display mode.
     */
    public TemplateTree(TemplateRootNode root) {
        super(root);
        setSelectionModel(EmptySelectionModel.sharedInstance());
        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setEnabled(false);
        setFocusable(false);
        setRootVisible(false);

        setCellRenderer(new TemplateCellRenderer());

        initExpandedStates();

        addExpansionListener();

        addSingleClickListener();
    }

    public TemplateTree(TemplateRootNode root, Editor editor) {
        super(root);

        this.editor = editor;

        setSelectionModel(EmptySelectionModel.sharedInstance());
        setBackground(LightZoneSkin.Colors.ToolPanesBackground);

        popup = new TemplateTreePopup(this);
        addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        handlePopup(event);
                    }
                    else {
                        TreePath path =
                            getPathForLocation(event.getX(), event.getY());
                        if (path != null) {
                            Object last = path.getLastPathComponent();
                            if (last instanceof TemplateTreeNode) {
                                TemplateTreeNode node = (TemplateTreeNode) last;
                                boolean hadFocus = isFocusOwner();
                                setTemplate(node);
                                if (hadFocus) {
                                    // Adding tools snatches keyboard focus.
                                    requestFocusInWindow();
                                }
                            }
                        }
                    }
                }
                public void mouseReleased(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        handlePopup(event);
                    }
                }
                private void handlePopup(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        Point p = event.getPoint();
                        TreePath path = getClosestPathForLocation(p.x, p.y);
                        if (path != null) {
                            Object last = path.getLastPathComponent();
                            TemplateKey key = null;
                            if (last instanceof TemplateTreeNode) {
                                TemplateTreeNode node = (TemplateTreeNode) last;
                                key = node.key;
                            }
                            popup.show(p, key);
                        }
                    }
                }
            }
        );
        setFocusable(false);
        setRootVisible(false);

        setCellRenderer(new TemplateCellRenderer());

        initExpandedStates();

        addExpansionListener();

        addSingleClickListener();
    }

    // Ensure that the template namespace tree node for the given namespace
    // is in its expanded state.
    void setNamespace(String namespace) {
        TreeNode root = (TreeNode) getModel().getRoot();
        Enumeration e = root.children();
        while (e.hasMoreElements()) {
            TemplateNamespaceTreeNode child =
                (TemplateNamespaceTreeNode) e.nextElement();
            String ns = child.toString();
            if (ns.equals(namespace)) {
                TreePath path = new TreePath(new Object[] { root, child } );
                setExpandedState(path, true);
            }
        }
    }

    private void setTemplate(TemplateTreeNode template) {
        if (selected != null) {
            List<OpControl> controls = selected.opControls;
            if (controls != null) {
                editor.removeControls(controls);
            }
        }
        if (template != null) {
            selected = template;
            XmlNode node = template.node;
            try {
                selected.opControls = editor.addControls(node);
            }
            catch (Throwable t) {
                // Let the control just do nothing.
                System.err.println(
                    "Could not apply template \"" + template.key + "\""
                );
                t.printStackTrace();
            }
        }
        else {
            selected = null;
        }
    }

    void commitSelection() {
        if (selected != null) {
            selected.opControls = null;
        }
    }

    TemplateKey getSelectedTemplateKey() {
        if (selected != null) {
            return selected.key;
        }
        return null;
    }

    // Initialize expanded states for namespace nodes
    private void initExpandedStates() {
        TreeNode root = (TreeNode) getModel().getRoot();
        Enumeration e = root.children();
        while (e.hasMoreElements()) {
            Object node = e.nextElement();
            String namespace = node.toString();
            if (Prefs.getBoolean(ExpandedKey + namespace, true)) {
                TreePath path = new TreePath(new Object[] { root, node });
                setExpandedState(path, true);
            }
        }
    }

    // Remember expanded states for namespace nodes
    private void addExpansionListener() {
        addTreeExpansionListener(
            new TreeExpansionListener() {
                public void treeExpanded(TreeExpansionEvent event) {
                    setPreference(event, true);
                }
                public void treeCollapsed(TreeExpansionEvent event) {
                    setPreference(event, false);
                }
                void setPreference(TreeExpansionEvent event, boolean expanded) {
                    TreePath path = event.getPath();
                    if (path.getPathCount() == 2) {
                        Object node = path.getLastPathComponent();
                        String namespace = node.toString();
                        Prefs.putBoolean(ExpandedKey + namespace, expanded);
                    }
                }
            }
        );
    }

    // Allow a single click on a namespace node to collapse and expand
    // the node.
    private void addSingleClickListener() {
        addMouseListener(
            new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    Point p = event.getPoint();
                    TreePath path = getPathForLocation(p.x, p.y);
                    if (path != null) {
                        TreeNode node = (TreeNode) path.getLastPathComponent();
                        if (! node.isLeaf()) {
                            if (isExpanded(path)) {
                                collapsePath(path);
                            }
                            else {
                                expandPath(path);
                            }
                        }
                        else {
                            commitSelection();
                        }
                    }
                }
            }
        );
    }
}
