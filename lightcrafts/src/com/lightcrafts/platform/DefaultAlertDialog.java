/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.ui.toolkit.UICompliance;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;

/**
 * A <code>DefaultAlertDialog</code> implements a cross-platform version of
 * {@link AlertDialog}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DefaultAlertDialog implements AlertDialog {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton static instance. */
    public static final DefaultAlertDialog INSTANCE = new DefaultAlertDialog();

    /**
     * {@inheritDoc}
     */

    public int showAlert( Frame parentFrame, String msgText, String infoText,
                          int alertType, String... buttons ) {
        return showAlert( parentFrame, msgText,  infoText, alertType, -1, buttons );
    }

    public int showAlert( Frame parentFrame, String msgText, String infoText,
                          int alertType, int destructive, String... buttons ) {
        final int messageType;
        switch ( alertType ) {
            case WARNING_ALERT:
                messageType = JOptionPane.WARNING_MESSAGE;
                break;
            case ERROR_ALERT:
                messageType = JOptionPane.ERROR_MESSAGE;
                break;
            default:
                messageType = JOptionPane.PLAIN_MESSAGE;
        }

        final int optionType;
        switch ( buttons.length ) {
            case 1:
                optionType = JOptionPane.DEFAULT_OPTION;
                break;
            case 2:
                optionType = JOptionPane.OK_CANCEL_OPTION;
                break;
            case 3:
                optionType = JOptionPane.YES_NO_CANCEL_OPTION;
                break;
            default:
                optionType = JOptionPane.DEFAULT_OPTION;
        }
        Box msgBox = Box.createVerticalBox();

        // Initialize text areas by the text area constructor or the text area
        // factory, depending on whether the text appears to be preformatted
        // (with newlines) or presumably requires line wrapping.
        JTextArea msgArea;
        if ( msgText.contains( "\n" ) ) {
            msgArea = new JTextArea( msgText );
            msgArea.setLineWrap(false);
            msgArea.setEditable(false);
        }
        else {
            msgArea = TextAreaFactory.createTextArea( msgText, 30 );
        }
        msgArea.setFont(msgArea.getFont().deriveFont(Font.BOLD));
        // msgArea.setBackground( BackgroundColor );
        msgArea.setOpaque(false);
        msgArea.setBorder(null);
        msgBox.add(msgArea);

        if ( infoText != null ) {
            JTextArea infoArea;
            if ( infoText.contains( "\n" ) ) {
                infoArea = new JTextArea( infoText );
                infoArea.setLineWrap(false);
                infoArea.setEditable(false);
            }
            else {
                infoArea = TextAreaFactory.createTextArea( infoText, 30 );
            }
            // infoArea.setBackground( BackgroundColor );
            infoArea.setOpaque(false);
            infoArea.setBorder(null);
            msgBox.add( Box.createVerticalStrut(6) );
            msgBox.add( infoArea );
        }

        return UICompliance.showOptionDialog(
            parentFrame, msgBox, "Confirm", optionType, messageType, null,
            buttons, buttons[0], destructive
        );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton static instance.
     */
    private DefaultAlertDialog() {
        // do nothing
    }

    /**
     * The backgorund color of the dialog contents shown by
     * JOptionPane.showOptionDialog(), which we would like to match in
     * our JTextAreas.
     */
    private final static Color BackgroundColor = LightZoneSkin.Colors.FrameBackground; // (new JPanel()).getBackground();

    ////////// main (for testing) /////////////////////////////////////////////

    public static void main(String[] args) {
        System.setProperty( "apple.awt.antialiasing"    , "false" );
        System.setProperty( "apple.awt.showGrowBox"     , "true" );
        System.setProperty( "apple.awt.textantialiasing", "true" );
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        System.setProperty( "swing.aatext", "true" );

        try {
            UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        int result = INSTANCE.showAlert(
            null,
            "LightZone file \"test.lzn\" can't locate its image file, " +
            "\"oldImage.jpg\", but it found \"newImage.jpg\" instead.",
            "Is this right?",
            AlertDialog.ERROR_ALERT,
            "Use This", "Search", "Cancel"
        );
        System.out.println("result=" + result);
        System.exit(0);
    }
}
/* vim:set et sw=4 ts=4: */
