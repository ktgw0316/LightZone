/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import static com.lightcrafts.prefs.Locale.LOCALE;
import com.lightcrafts.ui.metadata2.CopyrightDefaults;
import com.lightcrafts.ui.toolkit.TextAreaFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;

class CopyrightPrefsPanel extends JPanel {

    private final static int TextFieldWidth = 250;
    private final static Dimension TextAreaSize = new Dimension(TextFieldWidth, 150);

    // The copyright text
    private JTextArea copyrightText;

    // The creator text
    private JTextField creatorText;

    // The revert button
    private JButton revert;

    // Explanatory text
    private HelpArea help;

    // If the copyright text has never been set, show a placeholder.
    private boolean isPlaceholderCopyrightTextShown;

    // If the creator text has never been set, show a placeholder.
    private boolean isPlaceholderCreatorTextShown;

    CopyrightPrefsPanel() {
        initCreatorText();
        initCopyrightText();

        String helpText = LOCALE.get("CopyrightHelp");
        help = new HelpArea();
        help.setText(helpText);

        Dimension helpSize = help.getPreferredSize();
        helpSize = new Dimension(helpSize.width, 70);
        help.setPreferredSize(helpSize);
        help.setMaximumSize(helpSize);

        revert = new JButton(LOCALE.get("CopyrightRevertButtonText"));
        revert.setHorizontalAlignment(SwingConstants.CENTER);
        revert.setAlignmentX(.5f);
        revert.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    revertCreatorText();
                    revertCopyrightText();
                }
            }
        );
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 4, 8, 4);

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        fields.add(new JLabel(LOCALE.get("CreatorLabel") + ':'), c);

        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        fields.add(creatorText, c);

        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        fields.add(new JLabel(LOCALE.get("CopyrightLabel") + ':'), c);

        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        JScrollPane scrollpane = new JScrollPane(copyrightText);
        scrollpane.setPreferredSize(TextAreaSize);
        fields.add(scrollpane, c);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(8));
        add(fields);
        add(Box.createVerticalStrut(8));
        add(revert);
        add(Box.createVerticalStrut(8));
        add(Box.createVerticalGlue());
        add(help);
    }

    private void initCopyrightText() {
        String text = CopyrightDefaults.getDefaultCopyright();
        if ((text != null) && ! text.equals("")) {
            copyrightText = TextAreaFactory.createTextArea(text, 30);
        }
        else {
            text = LOCALE.get("CopyrightPlaceholderText");
            copyrightText = TextAreaFactory.createTextArea(text, 30);
            copyrightText.setFont(copyrightText.getFont().deriveFont(Font.ITALIC));
            copyrightText.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent event) {
                        if (isPlaceholderCopyrightTextShown) {
                            copyrightText.setText("");
                            copyrightText.setFont(
                                copyrightText.getFont().deriveFont(Font.PLAIN)
                            );
                        }
                        isPlaceholderCopyrightTextShown = false;
                    }
                }
            );
            isPlaceholderCopyrightTextShown = true;
        }
        copyrightText.setEditable(true);
    }

    private void initCreatorText() {
        String text = CopyrightDefaults.getDefaultCreator();
        if ((text != null) && ! text.equals("")) {
            creatorText = new JTextField(text);
        }
        else {
            text = LOCALE.get("CreatorPlaceholderText");
            creatorText = new JTextField(text);
            creatorText.setFont(creatorText.getFont().deriveFont(Font.ITALIC));
            creatorText.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent event) {
                        if (isPlaceholderCreatorTextShown) {
                            creatorText.setText("");
                            creatorText.setFont(
                                creatorText.getFont().deriveFont(Font.PLAIN)
                            );
                        }
                        isPlaceholderCreatorTextShown = false;
                    }
                }
            );
            isPlaceholderCreatorTextShown = true;
        }
        creatorText.setEditable(true);

        Dimension size = creatorText.getPreferredSize();
        creatorText.setPreferredSize(
            new Dimension(TextFieldWidth, size.height)
        );
    }

    private void revertCopyrightText() {
        String copyText = CopyrightDefaults.getDefaultCopyright();
        if ((copyText != null) && ! copyText.equals("")) {
            copyrightText.setText(copyText);
        }
        else if (!isPlaceholderCopyrightTextShown) {
            copyText = LOCALE.get("CopyrightPlaceholderText");
            copyrightText.setText(copyText);
            copyrightText.setFont(
                copyrightText.getFont().deriveFont(Font.ITALIC)
            );
            isPlaceholderCopyrightTextShown = true;
        }
    }

    private void revertCreatorText() {
        String createText = CopyrightDefaults.getDefaultCreator();
        if ((createText != null) && ! createText.equals("")) {
            creatorText.setText(createText);
        }
        else if (!isPlaceholderCreatorTextShown) {
            createText = LOCALE.get("CreatorPlaceholderText");
            creatorText.setText(createText);
            creatorText.setFont(
                creatorText.getFont().deriveFont(Font.ITALIC)
            );
            isPlaceholderCreatorTextShown = true;
        }
        Dimension size = creatorText.getPreferredSize();
        creatorText.setPreferredSize(
            new Dimension(TextFieldWidth, size.height)
        );
    }

    void commit() {
        String s = copyrightText.getText();
        if (s.equals("") || isPlaceholderCopyrightTextShown) {
            CopyrightDefaults.setDefaultCopyright(null);
        }
        else {
            CopyrightDefaults.setDefaultCopyright(s);
        }
        s = creatorText.getText();
        if (s.equals("") || isPlaceholderCreatorTextShown) {
            CopyrightDefaults.setDefaultCreator(null);
        }
        else {
            CopyrightDefaults.setDefaultCreator(s);
        }
    }
}
