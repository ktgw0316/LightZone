/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This component takes a Template, shows the list of tools to a user, performs
 * surgery on the XML according to the user's selections, and returns the XML.
 * <p>
 * This logic depends completely on the LZN schema.
 */
class TemplateToolSelector extends Box implements Scrollable {

    private XmlDocument xml;
    
    private JCheckBox[] checks;

    TemplateToolSelector(XmlDocument template) throws XMLException {
        super(BoxLayout.Y_AXIS);

        xml = template;

        TemplateJig jig = new TemplateJig(xml);
        String[] names = jig.getToolNames();

        checks = new JCheckBox[names.length];

        for (int n=0; n<names.length; n++) {
            String name = names[n];
            JCheckBox check = new JCheckBox(name);
            // Leave out default RAW tools by default, since their behavior
            // in Styles features can be confusing.  (They replace existing
            // RAW tools, rather than adding new ones.)
            if (! name.startsWith("RAW")) {
                check.setSelected(true);
            }
            check.setOpaque(false);
            checks[n] = check;
        }
        Box checkBoxes = Box.createVerticalBox();

        // Add to layout in reverse order, like the tool stack:
        for (int n=checks.length-1; n>=0; n--) {
            checkBoxes.add(checks[n]);
        }
        JScrollPane scroll = new JScrollPane(checkBoxes);
        scroll.setPreferredSize(new Dimension(200, 150));
        scroll.setBorder(
            BorderFactory.createLineBorder(Color.darkGray)
        );
        add(scroll);

        JButton allButton = new JButton(
            LOCALE.get("TemplateSelectorAllButton")
        );
        allButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectAll();
                }
            }
        );
        JButton noneButton = new JButton(
            LOCALE.get("TemplateSelectorNoneButton")
        );
        noneButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectNone();
                }
            }
        );
        Box allNoneBox = Box.createHorizontalBox();
        allNoneBox.add(allButton);
        allNoneBox.add(Box.createHorizontalStrut(4));
        allNoneBox.add(noneButton);
        allNoneBox.add(Box.createHorizontalGlue());

        add(Box.createVerticalStrut(8));
        
        add(allNoneBox);
    }

    // Get a Template that matches the original Template with unsselected tools
    // omitted.
    XmlDocument getModifiedTemplate() {
        XmlDocument clone = new XmlDocument(xml);
        try {
            TemplateJig jig = new TemplateJig(clone);
            for (int n=checks.length-1; n>=0; n--) {
                JCheckBox check = checks[n];
                if (! check.isSelected()) {
                    jig.removeTool(n);
                }
            }
        }
        catch (XMLException e) {
            // Assume no modifications
            return new XmlDocument(xml);
        }
        return clone;
    }

    XmlDocument getOriginalTemplate() {
        return new XmlDocument(xml);
    }

    private void selectAll() {
        for (JCheckBox check : checks) {
            check.setSelected(true);
        }
    }

    private void selectNone() {
        for (JCheckBox check : checks) {
            check.setSelected(false);
        }
    }

    // Start Scrollable

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        if (checks.length > 0) {
            if (orientation == SwingConstants.VERTICAL) {
                return checks[0].getPreferredSize().height;
            }
        }
        return 1;
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    // End Scrollable
}
