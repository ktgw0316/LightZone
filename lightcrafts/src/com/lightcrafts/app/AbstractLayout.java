/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.browser.ctrls.FolderCtrl;
import com.lightcrafts.ui.browser.view.ImageBrowserScrollPane;
import com.lightcrafts.ui.editor.DocUndoHistory;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.layout.FadingTabConfiguration;
import com.lightcrafts.ui.layout.FadingTabbedPanel;
import com.lightcrafts.ui.layout.FadingTabbedPanelListener;
import com.lightcrafts.ui.layout.SmartSplitPane;
import com.lightcrafts.ui.metadata2.MetadataScroll;
import com.lightcrafts.ui.templates.TemplateControl;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.prefs.Preferences;

public abstract class AbstractLayout extends JPanel {

    public enum LayoutType { Combo, Browser, Editor }

    private LayoutType type;

    // Remember the current layout so it can be restored on restart.
    final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/app"
    );

    private final static String LayoutKey = "Layout";
    private final static String ComboValue = "Combo";
    private final static String BrowserValue = "Browser";
    private final static String EditorValue = "Editor";

    // Preference keys to remember FadingTabbedPanel selections:
    private final static String LeftFaderTabKey = "LeftFaderTab";
    private final static String RightFaderTabKey = "RightFaderTab";

    // Default values for split pane divider loations.
    // (Negative numbers mean use preferred sizes.)
    final static int LeftDividerLoc = -1;
    final static int RightDividerLoc = -1;

    // Preference keys, for sticky divider locations.
    final static String LeftDividerKey = "leftDividerLocation";
    final static String RightDividerKey = "rightDividerLocation";

    // Minimum and maximum values of the split pane divider locations.
    final static int MinRightFaderWidth = 330;
    final static int MaxRightFaderWidth = 480;
    final static int MinLeftFaderWidth  = 200;
    final static int MaxLeftFaderWidth  = 400;

    // Placeholder fader tab name, to indicate "no selection" in preferences:
    final String NoLabel = "NoTabSelected";

    // Split panes hold everything.
    SmartSplitPane leftSplit;
    SmartSplitPane rightSplit;

    // Faders to on the left and right.
    FadingTabbedPanel leftFader;
    FadingTabbedPanel rightFader;

    // Store the layout elements, so they can be updated now and then.
    // (Document initialization, folder selection, browser rotation...)
    TemplateControl templates;
    Editor editor;
    DocUndoHistory history;
    FolderCtrl folders;
    ImageBrowserScrollPane browser;
    MetadataScroll info;
    LayoutHeader header;

    // Track whether the browser has been rotated, for persistence.
    boolean isBrowserRotated;

    static AbstractLayout createRecentLayout(
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        FolderCtrl folders,
        ImageBrowserScrollPane browser,
        MetadataScroll info,
        LayoutHeader header,
        ComboFrame frame
    ) {
        switch (getRecentLayoutType()) {
            case Browser:
                return new BrowserLayout(
                    templates, editor, history, folders, browser, info, header, frame
                );
            case Editor:
                return new EditorLayout(
                    templates, editor, history, folders, browser, info, header
                );
            case Combo:
                return new ComboLayout(
                    templates, editor, history, folders, browser, info, header, frame
                );
            default:
                return new BrowserLayout(
                    templates, editor, history, folders, browser, info, header, frame
                );
        }
    }

    static LayoutType getRecentLayoutType() {
        String recent = Prefs.get(LayoutKey, BrowserValue);

        if (recent.equals(BrowserValue)) {
            return LayoutType.Browser;
        }
        if (recent.equals(EditorValue)) {
            return LayoutType.Editor;
        }
        if (recent.equals(ComboValue)) {
            return LayoutType.Editor;
        }
        return LayoutType.Browser;
    }

    // Base classes must declare themselves either Combo, Browser, or Editor.
    AbstractLayout(
        LayoutType type,
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        FolderCtrl folders,
        ImageBrowserScrollPane browser,
        MetadataScroll info,
        LayoutHeader header
    ) {
        this.type = type;

        this.templates = templates;
        this.editor = editor;
        this.history = history;
        this.folders = folders;
        this.browser = browser;
        this.info = info;
        this.header = header;

        switch (type) {
            case Browser :
                Prefs.put(LayoutKey, BrowserValue);
                break;
            case Editor :
                Prefs.put(LayoutKey, EditorValue);
                break;
            default:
                Prefs.put(LayoutKey, ComboValue);
        }
        leftSplit = new SmartSplitPane(LeftDividerKey, SmartSplitPane.LEFT);
        rightSplit = new SmartSplitPane(RightDividerKey, SmartSplitPane.RIGHT);
    }

    void restoreLayout() {
        leftSplit.restoreFromPrefs(
            LeftDividerLoc, SmartSplitPane.HORIZONTAL_SPLIT
        );
        rightSplit.restoreFromPrefs(
            RightDividerLoc, SmartSplitPane.HORIZONTAL_SPLIT
        );
        restoreFaders();
    }

    LayoutType getLayoutType() {
        return type;
    }

    /**
     * Called when a Document is opened or closed.  The ImageMetadata object
     * is that of the new Document, or null if a Document has been closed.
     */
    void updateEditor(
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        MetadataScroll info
    ) {
        this.templates = templates;
        this.editor = editor;
        this.history = history;
        this.info = info;
    }

    /**
     * Called when the browsed folder changes.  The new AbstractImageBrowser
     * reference is accessible through the ImageBrowserScrollPane given to
     * the constructor.
     */
    abstract void updateBrowser();

    abstract List<FadingTabConfiguration> getLeftFaderConfs();

    abstract List<FadingTabConfiguration> getRightFaderConfs();

    abstract String getDefaultLeftFaderTab();

    abstract String getDefaultRightFaderTab();

    void initFaders() {
        List<FadingTabConfiguration> leftTabs = getLeftFaderConfs();

        leftFader = new FadingTabbedPanel(
            leftTabs,
            FadingTabbedPanel.Orientation.Up,
            new FadingTabbedPanelListener() {
                public void somethingSelected() {
                    leftSplit.unhide();
                }
                public void tabSelected(String name) {
                    Prefs.put(type + LeftFaderTabKey, name);
                }
                public void nothingSelected() {
                    leftSplit.hideLeft();
                    Prefs.put(type + LeftFaderTabKey, NoLabel);
                }
            }
        );
        List<FadingTabConfiguration> rightTabs = getRightFaderConfs();

        rightFader = new FadingTabbedPanel(
            rightTabs,
            FadingTabbedPanel.Orientation.Down,
            new FadingTabbedPanelListener() {
                public void somethingSelected() {
                    rightSplit.unhide();
                }
                public void tabSelected(String name) {
                    Prefs.put(type + RightFaderTabKey, name);
                }
                public void nothingSelected() {
                    rightSplit.hideRight();
                    Prefs.put(type + RightFaderTabKey, NoLabel);
                }
            }
        );
    }

    // The layouts use ToggleTitleBorders, the metadata editor needs to
    // shutdown, and every instance has a global preferenceChangeListener.
    void dispose() {
        info.endEditing();
    }

    // Initialize fader tabs from preferences or apply defaults.
    //
    // When switching between layouts, two sticky behaviors are in
    // competition with each other: sticky split pane show/hide states, and
    // sticky fader tab selections.
    //
    // The rule is: tab selections always win.  If a split pane was hidden
    // but a tab was selected, then unhide the split pane.  If a split pane
    // was hidden but no tab was selected, hide the split pane.
    private void restoreFaders() {
        String leftDefault = getDefaultLeftFaderTab();
        String left = Prefs.get(
            type + LeftFaderTabKey, leftDefault
        );
        if (left.equals(NoLabel)) {
            leftSplit.hideLeft();
        }
        else {
            leftFader.setSelected(left);
        }
        String rightDefault = getDefaultRightFaderTab();
        String right = Prefs.get(
            type + RightFaderTabKey, rightDefault
        );
        if (right.equals(NoLabel)) {
            rightSplit.hideRight();
        }
        else {
            rightFader.setSelected(right);
        }
        constrainFaderWidths();
    }

    private void constrainFaderWidths() {
        Dimension size;

        size = leftFader.getMinimumSize();
        size = new Dimension(Math.max(MinLeftFaderWidth, size.width), size.height);
        leftFader.setMinimumSize(size);

        size = leftFader.getMaximumSize();
        size = new Dimension(Math.min(MaxLeftFaderWidth, size.width), size.height);
        leftFader.setMaximumSize(size);

        size = rightFader.getMinimumSize();
        size = new Dimension(Math.max(MinRightFaderWidth, size.width), size.height);
        rightFader.setMinimumSize(size);

        size = rightFader.getMaximumSize();
        size = new Dimension(Math.min(MaxRightFaderWidth, size.width), size.height);
        rightFader.setMaximumSize(size);
    }
}
