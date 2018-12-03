/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit.journal;

import com.lightcrafts.ui.toolkit.TextAreaFactory;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.io.*;

/**
 * A modeless dialog to control and apply the InputEventJournal.
 * <p>
 * When this dialog is shown, the journal starts, and when the dialog is
 * hidden, the journal stops.  It provides controls to save and replay
 * journals.  It also provides a little explanatory help.
 * <p>
 * Replay always will apply to the JFrame specified in showJournalDialog().
 */
public class JournalDialog extends JDialog implements JournalListener {

    private static InputEventJournal Journal = InputEventJournal.Instance;

    private static JournalDialog Instance = new JournalDialog();

    private static JFrame Owner;

    private JTextArea text;
    private JButton save;
    private JButton load;
    private JButton replay;
    private JButton print;
    private JLabel counter;
    private JTextField name;
    private JLabel status;

    private File file;

    private JournalDialog() {
        setTitle("Input Event Journal");

        JPanel panel = new JPanel(new BorderLayout());
        setContentPane(panel);

        Box content = Box.createVerticalBox();
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(content);

        text = createTextArea();
        content.add(text);

        content.add(Box.createVerticalStrut(8));

        replay = createReplayButton();
        print = createPrintButton();
        save = createSaveButton();
        load = createLoadButton();
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(replay);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(print);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(save);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(load);
        buttons.add(Box.createHorizontalGlue());
        content.add(buttons);

        content.add(Box.createVerticalStrut(8));

        name = createName();
        Box nameBox = Box.createHorizontalBox();
        nameBox.add(new JLabel("File:"));
        nameBox.add(Box.createHorizontalStrut(3));
        nameBox.add(name);
        content.add(nameBox);

        content.add(Box.createVerticalStrut(8));

        counter = createCounter();
        status = createStatus();
        Box readout = Box.createHorizontalBox();
        readout.add(status);
        readout.add(Box.createHorizontalGlue());
        readout.add(counter);
        content.add(readout);

        try {
            setFile(File.createTempFile("LZJournal", ".xml"));
            file.deleteOnExit();
        }
        catch (IOException e) {
            System.err.println(
                "Couldn't create journal temp file: " + e.getMessage()
            );
        }
        setStatus("Stopped");
        setCounter(0);

        pack();
        setResizable(false);

        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent event) {
                    Journal.start();
                }

                @Override
                public void windowClosed(WindowEvent event) {
                    Journal.stop();
                }
            }
        );
        Journal.addJournalListener(this);
    }

    /**
     * Implementing the package-private JournalListener interface.
     */
    @Override
    public void journalStarted(boolean replaying) {
        if (replaying) {
            setStatus("Replaying");
        }
        else {
            setStatus("Recording");
        }
        setCounter(0);
    }

    /**
     * Implementing the package-private JournalListener interface.
     */
    @Override
    public void journalEvent(int count, boolean replaying) {
        setCounter(count);
    }

    /**
     * Implementing the package-private JournalListener interface.
     */
    @Override
    public void journalEnded(boolean replaying) {
        setStatus("Stopped");
        try (OutputStream out = new FileOutputStream(file)) {
            Journal.write(out);
        }
        catch (IOException e) {
            System.err.println("Error writing journal: " + e.getMessage());
        }
    }

    /**
     * Show the modeless dialog that gives access to the InputEventJournal
     * features.
     * @param owner The JFrame where replay will be applied.
     */
    public static void showJournalDialog(JFrame owner) {
        if (Instance.isVisible()) {
            return;
        }
        Owner = owner;
        Instance.setVisible(true);
    }

    private JTextArea createTextArea() {
        JTextArea text = TextAreaFactory.createTextArea(
            "How to use:\n" +
            "\n" +
            "    1. Focus the frame you want to record.\n" +
            "    2. Press \"\\\" to start recording.\n" +
            "    3. Do your thing.\n" +
            "    4. Press \"\\\" again to end recording.\n" +
            "    5. Come back here to replay and save your work.",
            35
        );
        text.setBackground(getContentPane().getBackground());
        return text;
    }

    private JButton createSaveButton() {
        JButton save = new JButton("Save");
        save.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(Instance);
            if (result == JFileChooser.APPROVE_OPTION) {
                setFile(chooser.getSelectedFile());
                try (OutputStream out = new FileOutputStream(file)) {
                    Journal.write(out);
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        });
        return save;
    }

    private JButton createLoadButton() {
        JButton save = new JButton("Load");
        save.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(Instance);
            if (result == JFileChooser.APPROVE_OPTION) {
                Journal.clear();
                File file = chooser.getSelectedFile();
                try (InputStream in = new FileInputStream(file)) {
                    Journal.read(in);
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        });
        return save;
    }

    private JButton createReplayButton() {
        JButton replay = new JButton("Replay");
        replay.addActionListener(event -> {
            Owner.toFront();
            Journal.replay(Owner);
        });
        return replay;
    }

    private JButton createPrintButton() {
        JButton print = new JButton("Print");
        print.addActionListener(event -> {
            try {
                Journal.write(System.out);
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
        return print;
    }

    private JLabel createCounter() {
        JLabel counter = new JLabel("Events: 000000");
        counter.setHorizontalAlignment(SwingConstants.RIGHT);
        Dimension size = counter.getPreferredSize();
        counter.setMinimumSize(size);
        counter.setPreferredSize(size);
        counter.setMaximumSize(size);
        return counter;
    }

    private JLabel createStatus() {
        return new JLabel();
    }

    private JTextField createName() {
        JTextField name = new JTextField(20);
        name.setEditable(false);
        return name;
    }

    private void setCounter(int count) {
        counter.setText(Integer.toString(count) + " Events");
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private void setFile(File file) {
        this.file = file;
        name.setText(file.getAbsolutePath());
    }
}
