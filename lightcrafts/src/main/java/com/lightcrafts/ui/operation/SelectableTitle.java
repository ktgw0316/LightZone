/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.CurveTypeButtons;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.net.URL;

import static com.lightcrafts.ui.operation.Locale.LOCALE;

class SelectableTitle extends JPanel implements MouseListener {

    private final static int TitleLeftInset = 2;

    private final static int TitleRightInset = 2;

    private final static int IconSpace = 3;

    private final static float TitleFontSize = 12f;

    private final static Color TitleTextColor = LightZoneSkin.Colors.ToolTitleTextColor;

    SelectableControl control;
    Box buttonBox;
    JTextField label;       // give access for the title editor in OpTitle

    private final Box buttonBoxBox;  // either the buttonBox or the lockButton
    private final JButton lockButton;
    private BufferedImage icon;
    private final CollapseExpandAction collapseAction;
    private final CollapseExpandButton collapseButton;
    private boolean selected;
    private boolean hasRegions = false;

    SelectableTitle(final SelectableControl control) {
        this.control = control;

        setLayout(null);

        resetTitle("Untitled");

        // Put a placeholder border where the rollover highlight will appear:
        label.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        collapseAction = new CollapseExpandAction(this);
        if (! control.isContentShown()) {
            collapseAction.setState(true);
        }
        collapseButton = new CollapseExpandButton(collapseAction);
        collapseButton.addMouseListener(this);

        lockButton = createLockButton();
        lockButton.addActionListener(e -> {
            JPopupMenu menu = getPopupMenu();
            menu.show(lockButton, 0, 0);
        });
        HelpButton help = new HelpButton(control);
        // Needs a vertical nudge for centered appearance:
        help.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

        buttonBox = Box.createHorizontalBox();
        buttonBox.add(help);
        // buttonBox.add(Box.createHorizontalStrut(ButtonSpace));

        buttonBoxBox = Box.createHorizontalBox();
        buttonBoxBox.add(buttonBox);
        add(buttonBoxBox);
        
        add(collapseButton);

        addMouseListener(this);
    }

    // Set the text of the current title component.
    void setTitleText(String s) {
        label.setText(s);
        revalidate();
    }

    String getTitleText() {
        return label.getText();
    }

    // Set the title component to the given component.
    void setTitle(JTextField text) {
        if (label != null) {
            remove(label);
        }
        label = text;
        label.setOpaque(false);
        label.setForeground(TitleTextColor);
        Font font = getTitleFont();
        label.setFont(font);
        add(label);
        revalidate();
        repaint();  // Fixes mysterious paint glitches after title editing.
    }

    // Initialize the title component to its default.
    void resetTitle(String s) {
        JTextField newLabel = new JTextField(s);
        newLabel.setEditable(false);
        newLabel.setEnabled(false);
        newLabel.setDisabledTextColor(TitleTextColor);
        setTitle(newLabel);
    }

    public void setRegionIndicator(boolean hasRegions) {
        if (this.hasRegions != hasRegions) {
            this.hasRegions = hasRegions;
            revalidate();
            repaint();
        }
    }

    void setIcon(BufferedImage icon) {
        this.icon = icon;
        revalidate();
    }

    void setSelected(boolean selected) {
        if (selected != this.selected) {
            this.selected = selected;
            repaint();
        }
    }

    void addLock() {
        buttonBoxBox.removeAll();
        buttonBoxBox.add(lockButton);
        // buttonBoxBox.add(Box.createHorizontalStrut(ButtonSpace));
        invalidate();
        repaint();
    }

    void removeLock() {
        buttonBoxBox.removeAll();
        buttonBoxBox.add(buttonBox);
        invalidate();
        repaint();
    }

    private static final RenderingHints aliasingRenderHints;

    static {
        aliasingRenderHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        RenderingHints hints = g.getRenderingHints();
        g.setRenderingHints(aliasingRenderHints);

        // Paint the background image:
        Dimension size = getSize();

        JButton button = new JButton("");
        button.setSize(size.width, size.height);
        
        button.paint(g);

        if (hasRegions) {
            Icon curveIcon = CurveTypeButtons.RegionGenericIcon;
            int iconH = curveIcon.getIconHeight();
            int iconX = TitleLeftInset + collapseButton.getSize().width;
            int iconY = (size.height - iconH) / 2;
            curveIcon.paintIcon(this, g, iconX, iconY);
        }

        // Paint the icon:
        if (icon != null) {
            int iconH = icon.getHeight();
            int iconX = TitleLeftInset + collapseButton.getSize().width + (hasRegions ? CurveTypeButtons.RegionGenericIcon.getIconWidth() : 0);
            int iconY = (size.height - iconH) / 2;
            g.drawImage(icon, iconX + 3, iconY + 3, icon.getWidth()-6, icon.getHeight()-6, null);
        }

        g.setRenderingHints(hints);
    }

    @Override
    public void doLayout() {

        Dimension size = getSize();

        // Place the button at its preferred size on the left:
        Dimension buttonSize = collapseButton.getPreferredSize();
        collapseButton.setLocation(
            TitleLeftInset, (size.height - buttonSize.height) / 2
        );
        collapseButton.setSize(buttonSize);

        // Center the label vertically, and put it after the icon:

        int iconW = ((icon != null) ? icon.getWidth() : 0) + (hasRegions ? CurveTypeButtons.BezierIcon.getIconWidth() : 0);

        Dimension titleSize = label.getPreferredSize();

        int titleX = TitleLeftInset + buttonSize.width + iconW + IconSpace;
        int titleY = (size.height - titleSize.height) / 2;
        label.setLocation(titleX, titleY);

        int titleW = Math.min(titleSize.width,  size.width - titleX);
        int titleH = Math.min(titleSize.height, size.height);
        label.setSize(new Dimension(titleW, titleH));

        // Center buttons vertically, on the right:
        Dimension buttonBoxSize = buttonBoxBox.getPreferredSize();
        buttonBoxBox.setLocation(
            size.width - TitleRightInset - buttonBoxSize.width,
            (size.height - buttonBoxSize.height) / 2
        );
        buttonBoxBox.setSize(buttonBoxSize);
    }

    private static JButton createLockButton() {
        Icon normalIcon = IconFactory.invertIcon(getTitleIcon("lock"));
        Icon pressedIcon = IconFactory.invertIcon(getTitleIcon("lock_pressed"));
        return new ImageOnlyButton(normalIcon, pressedIcon);
    }

    private final static String TitleTag = "Name";
    private final static String CollapsedTag = "Collapsed";

    void save(XmlNode node) {
        node.setAttribute(TitleTag, getTitleText());
        boolean collapsed = ! control.isContentShown();
        node.setAttribute(CollapsedTag, Boolean.toString(collapsed));
    }

    void restore(XmlNode node) throws XMLException {
        if (node.hasAttribute(TitleTag)) {
            String text = node.getAttribute(TitleTag);
            setTitleText(text);
        }
        // Default title is OK.
        if (node.hasAttribute(CollapsedTag)) {
            String value = node.getAttribute(CollapsedTag);
            boolean collapsed = Boolean.parseBoolean(value);
            control.setShowContent(! collapsed);
            if (collapsed) {
                collapseAction.setState(true);
            }
        }
    }

    private static Font getTitleFont() {
        Font font = OpControl.ControlFont;
        font = font.deriveFont(Font.BOLD);
        font = font.deriveFont(TitleFontSize);
        return font;
    }

    // Icons for buttons:
    static Icon getTitleIcon(String name) {
        String path = "resources/" + name + ".png";
        URL url = OpTitle.class.getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int count = e.getClickCount();
        if (count == 2) {
            if (! control.isContentShown()) {
                doExpand();
            }
            else {
                doCollapse();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            handlePopup(e);
        }
        else {
            OpStack stack = findOpStack();
            if (stack.getAutoExpand() && ! control.isContentShown()) {
                stack.collapseAll();
                doExpand();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            handlePopup(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private void handlePopup(MouseEvent e) {
        JPopupMenu menu = getPopupMenu();
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    void doExpand() {
        OpStack stack = findOpStack();
        stack.expand(control);
        collapseAction.setState(false);
    }

    void doCollapse() {
        OpStack stack = findOpStack();
        stack.collapse(control);
        collapseAction.setState(true);
    }

    JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        final OpStack stack = findOpStack();

        // A long list of JMenuItems whose actions point to the OpStack:

        JMenuItem collapseItem = new JMenuItem(LOCALE.get("CollapseMenuItem"));
        collapseItem.addActionListener(e -> doCollapse());
        JMenuItem collapseOthers = new JMenuItem(LOCALE.get("CollapseOthersMenuItem"));
        collapseOthers.addActionListener(e -> {
            stack.setAutoExpand(false);
            stack.collapseAll();
            doExpand();
        });
        JMenuItem expandItem = new JMenuItem(LOCALE.get("ExpandMenuItem"));
        expandItem.addActionListener(e -> doExpand());
        JMenuItem autoItem = new JMenuItem(LOCALE.get("AutoExpandMenuItem"));
        autoItem.addActionListener(e -> {
            stack.setAutoExpand(true);
            stack.collapseAll();
            doExpand();
        });
        JMenuItem noAutoItem = new JMenuItem(LOCALE.get("DontAutoExpandMenuItem"));
        noAutoItem.addActionListener(e -> stack.setAutoExpand(false));
        JMenuItem collapseAllItem = new JMenuItem(LOCALE.get("CollapseAllMenuItem"));
        collapseAllItem.addActionListener(e -> {
            stack.setAutoExpand(false);
            stack.collapseAll();
        });
        JMenuItem expandAllItem = new JMenuItem(LOCALE.get("ExpandAllMenuItem"));
        expandAllItem.addActionListener(e -> {
            stack.setAutoExpand(false);
            stack.expandAll();
        });
        if (control.isContentShown()) {
            menu.add(collapseItem);
            menu.add(collapseAllItem);
            menu.add(collapseOthers );
        } else {
            menu.add(collapseAllItem);
        }
        menu.add(new JSeparator());
        menu.add(expandItem);
        menu.add(expandAllItem);
        menu.add(new JSeparator());

        if (! stack.getAutoExpand()) {
            menu.add(autoItem);
        } else {
            menu.add(noAutoItem);
        }
        return menu;
    }

    // Get a reference to any enclosing OpStack, so we can trigger global
    // actions like collapse-all and expand-all.
    OpStack findOpStack() {
        return (OpStack) SwingUtilities.getAncestorOfClass(OpStack.class, this);
    }
}
