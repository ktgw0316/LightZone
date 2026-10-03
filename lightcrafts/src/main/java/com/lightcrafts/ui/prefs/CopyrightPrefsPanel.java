/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.application.prefs.PreferencesModel;
import com.lightcrafts.ui.toolkit.TextAreaFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

class CopyrightPrefsPanel extends JPanel {
    private static final int TextFieldWidth = 250;
    private static final Dimension TextAreaSize =
        new Dimension(TextFieldWidth, 150);

    private final JTextArea copyrightText =
        TextAreaFactory.createTextArea("", 30);
    private final JTextField creatorText = new JTextField();
    private final Consumer<PreferencesMessage> dispatch;
    private boolean placeholderCopyrightTextShown;
    private boolean placeholderCreatorTextShown;
    private boolean rendering;

    CopyrightPrefsPanel(
        PreferencesModel.CopyrightSettings initialSettings,
        Consumer<PreferencesMessage> dispatch
    ) {
        this.dispatch = dispatch;
        copyrightText.setEditable(true);
        creatorText.setEditable(true);
        Dimension creatorSize = creatorText.getPreferredSize();
        creatorText.setPreferredSize(
            new Dimension(TextFieldWidth, creatorSize.height)
        );

        JButton revert = new JButton(LOCALE.get("CopyrightRevertButtonText"));
        revert.setHorizontalAlignment(SwingConstants.CENTER);
        revert.setAlignmentX(.5f);
        revert.addActionListener(event ->
            dispatch.accept(new PreferencesMessage.RevertCopyrightDefaults())
        );

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 4, 8, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        fields.add(new JLabel(LOCALE.get("CreatorLabel") + ':'), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        fields.add(creatorText, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        fields.add(
            new JLabel(LOCALE.get("CopyrightLabel") + ':'), constraints
        );
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        JScrollPane scrollPane = new JScrollPane(copyrightText);
        scrollPane.setPreferredSize(TextAreaSize);
        fields.add(scrollPane, constraints);

        HelpArea help = new HelpArea();
        help.setText(LOCALE.get("CopyrightHelp"));
        Dimension helpSize = new Dimension(help.getPreferredSize().width, 70);
        help.setPreferredSize(helpSize);
        help.setMaximumSize(helpSize);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(8));
        add(fields);
        add(Box.createVerticalStrut(8));
        add(revert);
        add(Box.createVerticalStrut(8));
        add(Box.createVerticalGlue());
        add(help);

        copyrightText.getDocument().addDocumentListener(
            documentListener(() -> {
                if (!rendering) {
                    dispatch.accept(new PreferencesMessage.CopyrightChanged(
                        placeholderCopyrightTextShown
                            ? null
                            : copyrightText.getText()
                    ));
                }
            })
        );
        creatorText.getDocument().addDocumentListener(
            documentListener(() -> {
                if (!rendering) {
                    dispatch.accept(new PreferencesMessage.CreatorChanged(
                        placeholderCreatorTextShown
                            ? null
                            : creatorText.getText()
                    ));
                }
            })
        );
        copyrightText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (placeholderCopyrightTextShown) {
                    copyrightText.selectAll();
                }
            }
        });
        creatorText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (placeholderCreatorTextShown) {
                    creatorText.selectAll();
                }
            }
        });
        copyrightText.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (placeholderCopyrightTextShown) {
                    placeholderCopyrightTextShown = false;
                    copyrightText.setText("");
                    copyrightText.setFont(
                        copyrightText.getFont().deriveFont(Font.PLAIN)
                    );
                }
            }
        });
        creatorText.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (placeholderCreatorTextShown) {
                    placeholderCreatorTextShown = false;
                    creatorText.setText("");
                    creatorText.setFont(
                        creatorText.getFont().deriveFont(Font.PLAIN)
                    );
                }
            }
        });
        render(initialSettings);
    }

    void render(PreferencesModel.CopyrightSettings settings) {
        rendering = true;
        try {
            renderCopyright(settings.copyrightText());
            renderCreator(settings.creatorText());
        } finally {
            rendering = false;
        }
    }

    private void renderCopyright(String value) {
        if (value == null) {
            String placeholder = LOCALE.get("CopyrightPlaceholderText");
            if (!copyrightText.getText().equals(placeholder)) {
                copyrightText.setText(placeholder);
            }
            copyrightText.setFont(
                copyrightText.getFont().deriveFont(Font.ITALIC)
            );
            placeholderCopyrightTextShown = true;
        } else {
            if (!copyrightText.getText().equals(value)) {
                copyrightText.setText(value);
            }
            copyrightText.setFont(
                copyrightText.getFont().deriveFont(Font.PLAIN)
            );
            placeholderCopyrightTextShown = false;
        }
    }

    private void renderCreator(String value) {
        if (value == null) {
            String placeholder = LOCALE.get("CreatorPlaceholderText");
            if (!creatorText.getText().equals(placeholder)) {
                creatorText.setText(placeholder);
            }
            creatorText.setFont(
                creatorText.getFont().deriveFont(Font.ITALIC)
            );
            placeholderCreatorTextShown = true;
        } else {
            if (!creatorText.getText().equals(value)) {
                creatorText.setText(value);
            }
            creatorText.setFont(
                creatorText.getFont().deriveFont(Font.PLAIN)
            );
            placeholderCreatorTextShown = false;
        }
        Dimension size = creatorText.getPreferredSize();
        creatorText.setPreferredSize(new Dimension(TextFieldWidth, size.height));
    }

    private static DocumentListener documentListener(Runnable changed) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                changed.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed.run();
            }
        };
    }
}
