/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.layout.ToggleTitleBorder;
import com.lightcrafts.ui.toolkit.BoxedButton;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * A container for all the header elements in a ComboFrame, and a path through
 * which the frame can update all the header controls at once.  See update()
 * and setBrowseSelected()/setEditSelected().
 */
class LayoutHeader extends Box {

    private BrowseEditButtons modes;
    private BoxedButton help;
    private HelpButton helpButton;

    private BoxedButton browse;          // editor
    private BoxedButton printDoc;    // editor
    private BoxedButton exportDoc;  // editor
    private BoxedButton save;            // editor
    private BoxedButton revert;        // editor

    private BoxedButton undoRedo;    // holds undo and redo

    private BoxedButton edit;            // browser
    private BoxedButton send;            // browser
    private BoxedButton print;          // browser
    private BoxedButton export;        // browser
    private BoxedButton styles;        // browser

    private BoxedButton open;            // browser and editor

    private List<BoxedButton> buttonList; // all of the above

    private Box buttonBox;    // container for them

    static class LogoComponent extends JComponent {
        private final static Icon lzLogo;

        static {
            Icon tmp = null;

            try {
                URL url = LayoutHeader.class.getResource("resources/LZLogo.png");
                BufferedImage image = ImageIO.read(url);
                tmp = new ImageIcon(image);
            } catch (Exception e) {
            }

            lzLogo = tmp;
        }

        LogoComponent() {
            setBorder(new EmptyBorder(8, 2, 8, 2));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        protected void paintComponent(Graphics g) {
            lzLogo.paintIcon(this, g, 0, 8);
        }

        public Dimension getPreferredSize() {
            return new Dimension(lzLogo.getIconWidth(), lzLogo.getIconHeight() + 12);
        }
    }

    LayoutHeader(ComboFrame frame) {
        super(BoxLayout.X_AXIS);

        modes = new BrowseEditButtons(frame);
        buttonBox = Box.createHorizontalBox();
        help = new BoxedButton("Help", helpButton = new HelpButton());

        Component logoButton = new LogoComponent();

        add(Box.createHorizontalStrut(22));
        add(modes);
        add(Box.createHorizontalStrut(64));
        add(buttonBox);
        add(Box.createHorizontalGlue());
        add(logoButton);
        add(Box.createHorizontalStrut(16));
        add(help.box);
        add(Box.createHorizontalStrut(4));

        buttonList = new LinkedList<BoxedButton>();

        buttonList.add(browse = new BoxedButton(LOCALE.get("DoneLabel"), new DoneButton(frame)));
        buttonList.add(printDoc = new BoxedButton(LOCALE.get("PrintLabel"), new PrintDocButton(frame)));
        buttonList.add(exportDoc = new BoxedButton(LOCALE.get("ExportLabel"), new ExportDocButton(frame)));
        buttonList.add(save = new BoxedButton(LOCALE.get("SaveLabel"), new SaveButton(frame)));
        buttonList.add(revert = new BoxedButton(LOCALE.get("RevertLabel"), new RevertButton(frame)));
        buttonList.add(undoRedo = new BoxedButton(LOCALE.get("UndoRedoLabel"), new UndoButton(frame), new RedoButton(frame)));

        buttonList.add(edit = new BoxedButton(LOCALE.get("EditLabel"), new EditButton(frame)));
        buttonList.add(send = new BoxedButton(LOCALE.get("SendLabel"), new SendButton(frame)));
        buttonList.add(print = new BoxedButton(LOCALE.get("PrintLabel"), new PrintButton(frame)));
        buttonList.add(export = new BoxedButton(LOCALE.get("ConvertLabel"), new ExportButton(frame)));
        buttonList.add(styles = new BoxedButton(LOCALE.get("StylesLabel"), new StylesButton(frame)));

        buttonList.add(open = new BoxedButton(LOCALE.get("OpenLabel"), new OpenButton(frame)));
    }

    void setBrowseSelected() {
        modes.setBrowseSelected();

        buttonBox.removeAll();
        addButton(open.box);
        addButton(edit.box);
        addButton(print.box);

        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setMaximumSize(new Dimension(3, 32));
        buttonBox.add(separator);
        buttonBox.add(Box.createHorizontalStrut(8));

        addButton(styles.box);
        addButton(send.box);
        addButton(export.box);

        helpButton.setMode(HelpButton.Mode.Browse);
    }

    void setEditSelected() {
        modes.setEditSelected();

        buttonBox.removeAll();
        addButton(undoRedo.box);
        addButton(revert.box);
        addButton(browse.box);

        helpButton.setMode(HelpButton.Mode.Edit);
    }

    void addButtons(Container container) {
        buttonBox.add(container);
    }

    void removeButtons() {
        int index = buttonBox.getComponentCount() - 1;
        if (index >= 0) {
            buttonBox.remove(index);
        }
    }

    private void addButton(JComponent button) {
        buttonBox.add(button);
        buttonBox.add(Box.createHorizontalStrut(8));
    }

    // Called when things in the ComboFrame change (browser, document, etc.)
    void update() {
        for (BoxedButton boxedButton : buttonList) {
            for (AbstractButton button : boxedButton.buttons)
                ((FrameButton) button).updateButton();
        }
    }

    void dispose() {
        ToggleTitleBorder.unsetAllBorders(this);

        for (BoxedButton button : buttonList) {
            ToggleTitleBorder.unsetBorder(button.box);
        }
    }
}
