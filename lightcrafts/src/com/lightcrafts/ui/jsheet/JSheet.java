/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * @(#)JSheet.java  1.0  September 25, 2005
 *
 * Copyright (c) 2005 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package com.lightcrafts.ui.jsheet;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;

/**
 * JSheet is a document modal dialog which is displayed below the title bar
 * of a JFrame.
 * <p>
 * A JSheet blocks input on its owner window, while it is visible.
 * <p>
 * Unlike application modal dialogs, the show method of a JSheet does return
 * immediately, when the JSheet has become visible. Applications need to use
 * a SheetListener to get the return value of a JSheet.
 * <p>
 * Requires Java 1.4.
 * <p>
 * Caveats: We are using an unsupported API call to make the JSheet translucent.
 * This API may go away in future versions of the Macintosh Runtime for Java.
 * In such a case, we (hopefully) just end up with a non-opaque sheet.
 *
 * @author  Werner Randelshofer
 * @version 1.0 September 25, 2005 Created.
 */
public class JSheet extends JDialog {
    /**
     * Event listener list.
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * This handler is used to handle movements of the owner.
     * If the owner moves, we have to change the location of the sheet as well.
     */
    private ComponentListener ownerMovementHandler;

    /**
     * If this is non-null, we put the owner to the specified location,
     * when the sheet is hidden.
     */
    private Point shiftBackLocation;

    /**
     * We need to keep track of the old owner position, in order to avoid
     * processing duplicate owner moved events.
     */
    private Point oldLocation;

    /**
     * Focus owner on the owner window, before the sheet is shown.
     */
    private Component oldFocusOwner;

    private boolean isInstalled;

    /**
     * Creates a new JSheet.
     */
    public JSheet(Frame owner) {
        super(owner);
        init();
    }
    /**
     * Creates a new JSheet.
     */
    public JSheet(Dialog owner) {
        super(owner);
        init();
    }

    private void init() {
        if (getOwner() != null && UIManager.getBoolean("Sheet.showAsSheet")) {
            Utilities.invokeIfExists(this,"setUndecorated", true);
        }

        // We move the sheet when the user moves the owner, so that it
        // will always stay centered below the title bar of the owner.
        // If the user has moved the owner, we 'forget' the shift back location,
        // and don't shift the owner back to the place it was, when we opened
        // the sheet.
        ownerMovementHandler = new ComponentAdapter() {
            public void componentMoved(ComponentEvent evt) {
                Window owner = getOwner();
                Point newLocation = owner.getLocation();
                if (! newLocation.equals(oldLocation)) {
                    setLocation(
                    newLocation.x + (owner.getWidth() - getWidth()) / 2,
                    newLocation.y + owner.getInsets().top
                    );
                    shiftBackLocation = null;
                    oldLocation = newLocation;
                }
            }
        };
    }

    /**
     * Installs the sheet on the owner.
     * This method is invoked just before the JSheet is shown.
     */
    protected void installSheet() {
        if (! isInstalled) {
            Window owner = getOwner();
            if (owner != null) {

                // Determine the location for the sheet and its owner while
                // the sheet will be visible.
                // In case we have to shift the owner to fully display the
                // dialog, we remember the shift back position.
                Point ownerLoc = owner.getLocation();
                Point sheetLoc;
                if (UIManager.getBoolean("Sheet.showAsSheet")) {
                    sheetLoc = new Point(
                    ownerLoc.x + (owner.getWidth() - getWidth()) / 2,
                    ownerLoc.y + owner.getInsets().top
                    );

                    if (sheetLoc.x < 0) {
                        owner.setLocation(ownerLoc.x - sheetLoc.x, ownerLoc.y);
                        sheetLoc.x = 0;
                        shiftBackLocation = ownerLoc;
                        oldLocation = owner.getLocation();
                    } else {
                        shiftBackLocation = null;
                        oldLocation = ownerLoc;
                    }
                } else {
                    sheetLoc = new Point(
                    ownerLoc.x + (owner.getWidth() - getWidth()) / 2,
                    ownerLoc.y + (owner.getHeight() - getHeight()) / 2
                    );
                }
                setLocation(sheetLoc);

                oldFocusOwner = owner.getFocusOwner();

                owner.setFocusableWindowState(false);
                owner.setEnabled(false);
                // ((JFrame) owner).setResizable(false);
                if (UIManager.getBoolean("Sheet.showAsSheet")) {
                    owner.addComponentListener(ownerMovementHandler);
                }
            }
            isInstalled = true;
        }
    }
    /**
     * Uninstalls the sheet on the owner.
     * This method is invoked immediately after the JSheet is hidden.
     */
    protected void uninstallSheet() {
        if (isInstalled) {
            Window owner = getOwner();
            if (owner != null) {
                owner.setFocusableWindowState(true);
                owner.setEnabled(true);
                //((JFrame) owner).setResizable(true);
                owner.removeComponentListener(ownerMovementHandler);

                if (shiftBackLocation != null) {
                    owner.setLocation(shiftBackLocation);
                }
                if (oldFocusOwner != null) {
                    owner.toFront();
                    oldFocusOwner.requestFocus();
                }
            }
            isInstalled = false;
        }
    }

    public void addNotify() {
        super.addNotify();
        if (UIManager.getBoolean("Sheet.showAsSheet")) {
            Utilities.setWindowAlpha(this, 240);
        }
    }

    public void dispose() {
        super.dispose();
        uninstallSheet();
    }
    public void hide() {
        super.hide();
        uninstallSheet();
    }

    public void show() {
        installSheet();
        super.show();
    }

    /**
     * Adds a sheet listener.
     */
    public void addSheetListener(SheetListener l) {
        listenerList.add(SheetListener.class, l);
    }

    /**
     * Removes a sheet listener.
     */
    public void removeSheetListener(SheetListener l) {
        listenerList.remove(SheetListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for
     *   notification on this event type.  The event instance
     *   is lazily created using the parameters passed into
     *   the fire method.
     */
    protected void fireOptionSelected(JOptionPane pane) {
        Object value = pane.getValue();
        int option;

        if(value == null) {
            option = JOptionPane.CLOSED_OPTION;
        } else {
            if (pane.getOptions() == null) {
                if(value instanceof Integer) {
                    option = ((Integer) value).intValue();
                } else {
                    option = JOptionPane.CLOSED_OPTION;
                }
            } else {
                option = JOptionPane.CLOSED_OPTION;
                Object[] options = pane.getOptions();
                for (int i = 0, n = options.length; i < n; i++) {
                    if(options[i].equals(value)) {
                        option = i;
                        break;
                    }
                }
            }
        }

        fireOptionSelected(pane, option, value, pane.getInputValue());
    }
    /**
     * Notify all listeners that have registered interest for
     *   notification on this event type.  The event instance
     *   is lazily created using the parameters passed into
     *   the fire method.
     */
    protected void fireOptionSelected(JOptionPane pane, int option, Object value, Object inputValue) {
        SheetEvent sheetEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==SheetListener.class) {
                // Lazily create the event:
                if (sheetEvent == null)
                    sheetEvent = new SheetEvent(this, pane, option, value, inputValue);
                ((SheetListener)listeners[i+1]).optionSelected(sheetEvent);
            }
        }
    }
    /**
     * Notify all listeners that have registered interest for
     *   notification on this event type.  The event instance
     *   is lazily created using the parameters passed into
     *   the fire method.
     */
    protected void fireOptionSelected(JFileChooser pane, int option) {
        SheetEvent sheetEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==SheetListener.class) {
                // Lazily create the event:
                if (sheetEvent == null)
                    sheetEvent = new SheetEvent(this, pane, option, null);
                ((SheetListener)listeners[i+1]).optionSelected(sheetEvent);
            }
        }
    }
    /**
     * Displays an option pane as a sheet on its parent window.
     *
     * @param pane The option pane.
     * @param parent The parent of the option pane.
     * @param listener The listener for SheetEvents.
     */
    public static void showSheet(JOptionPane pane, Component parentComponent, SheetListener listener) {
        JSheet sheet = createSheet(pane, parentComponent, styleFromMessageType(pane.getMessageType()));
        sheet.addSheetListener(listener);
        sheet.show();
    }

    /**
     * Brings up a sheet with the options <i>Yes</i>,
     * <i>No</i> and <i>Cancel</i>.
     *
     * @param parentComponent determines the <code>Frame</code> in which the
     *			sheet is displayed; if <code>null</code>,
     *			or if the <code>parentComponent</code> has no
     *			<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     * @param listener The listener for SheetEvents.
     */
    public static void showConfirmSheet(Component parentComponent, Object message, SheetListener listener) {
        showConfirmSheet(parentComponent, message,
        JOptionPane.YES_NO_CANCEL_OPTION, listener);
    }
    /**
     * Brings up a sheet where the number of choices is determined
     * by the <code>optionType</code> parameter.
     *
     * @param parentComponent determines the <code>Frame</code> in which the
     *			sheet is displayed; if <code>null</code>,
     *			or if the <code>parentComponent</code> has no
     *			<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     * @param optionType an int designating the options available on the dialog:
     *                  <code>YES_NO_OPTION</code>, or
     *			<code>YES_NO_CANCEL_OPTION</code>
     * @param listener The listener for SheetEvents.
     */
    public static void showConfirmSheet(Component parentComponent,
    Object message, int optionType, SheetListener listener) {
        showConfirmSheet(parentComponent, message, optionType,
        JOptionPane.QUESTION_MESSAGE, listener);
    }
    /**
     * Brings up a sheet where the number of choices is determined
     * by the <code>optionType</code> parameter, where the
     * <code>messageType</code>
     * parameter determines the icon to display.
     * The <code>messageType</code> parameter is primarily used to supply
     * a default icon from the Look and Feel.
     *
     * @param parentComponent determines the <code>Frame</code> in
     *			which the dialog is displayed; if <code>null</code>,
     *			or if the <code>parentComponent</code> has no
     *			<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     * @param optionType an integer designating the options available
     *			on the dialog: <code>YES_NO_OPTION</code>,
     *			or <code>YES_NO_CANCEL_OPTION</code>
     * @param messageType an integer designating the kind of message this is;
     *                  primarily used to determine the icon from the pluggable
     *                  Look and Feel: <code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *                  <code>JOptionPane.WARNING_MESSAGE</code>,
     *                  <code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     * @param listener The listener for SheetEvents.
     */
    public static void showConfirmSheet(Component parentComponent,
    Object message, int optionType, int messageType, SheetListener listener) {
        showConfirmSheet(parentComponent, message, optionType,
        messageType, null, listener);
    }

    /**
     * Brings up a sheet with a specified icon, where the number of
     * choices is determined by the <code>optionType</code> parameter.
     * The <code>messageType</code> parameter is primarily used to supply
     * a default icon from the look and feel.
     *
     * @param parentComponent determines the <code>Frame</code> in which the
     *			dialog is displayed; if <code>null</code>,
     *			or if the <code>parentComponent</code> has no
     *			<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the Object to display
     * @param optionType an int designating the options available on the dialog:
     *                  <code>YES_NO_OPTION</code>,
     *			or <code>YES_NO_CANCEL_OPTION</code>
     * @param messageType an int designating the kind of message this is,
     *                  primarily used to determine the icon from the pluggable
     *                  Look and Feel: <code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *                  <code>JOptionPane.WARNING_MESSAGE</code>,
     *                  <code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     * @param icon      the icon to display in the dialog
     * @param listener The listener for SheetEvents.
     */
    public static void showConfirmSheet(Component parentComponent,
    Object message, int optionType,
    int messageType, Icon icon, SheetListener listener) {
        showOptionSheet(parentComponent, message, optionType,
        messageType, icon, null, null, listener);
    }

    /**
     * Shows a question-message sheet requesting input from the user
     * parented to <code>parentComponent</code>.
     *
     * @param parentComponent  the parent <code>Component</code> for the
     *		dialog
     * @param listener The listener for SheetEvents.
     */
    public static void showInputSheet(Component parentComponent,
    Object message, SheetListener listener) {
        showInputSheet(parentComponent, message, JOptionPane.QUESTION_MESSAGE, listener);
    }

    /**
     * Shows a question-message sheet requesting input from the user and
     * parented to <code>parentComponent</code>. The input value will be
     * initialized to <code>initialSelectionValue</code>.
     *
     * @param parentComponent  the parent <code>Component</code> for the
     *		dialog
     * @param message the <code>Object</code> to display
     * @param initialSelectionValue the value used to initialize the input
     *                 field
     * @param listener The listener for SheetEvents.
     */
    public static void showInputSheet(Component parentComponent, Object message,
    Object initialSelectionValue, SheetListener listener) {
        showInputSheet(parentComponent, message,
        JOptionPane.QUESTION_MESSAGE, null, null,
        initialSelectionValue, listener);
    }

    /**
     * Shows a sheet requesting input from the user parented to
     * <code>parentComponent</code> and message type <code>messageType</code>.
     *
     * @param parentComponent  the parent <code>Component</code> for the
     *			dialog
     * @param message  the <code>Object</code> to display
     * @param messageType the type of message that is to be displayed:
     *                 	<code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *			<code>JOptionPane.WARNING_MESSAGE</code>,
     *                 	<code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     * @param listener The listener for SheetEvents.
     */
    public static void showInputSheet(Component parentComponent,
    Object message, int messageType, SheetListener listener) {
        showInputSheet(parentComponent, message,
        messageType, null, null, null, listener);
    }

    /**
     * Prompts the user for input in a sheet where the
     * initial selection, possible selections, and all other options can
     * be specified. The user will able to choose from
     * <code>selectionValues</code>, where <code>null</code> implies the
     * user can input
     * whatever they wish, usually by means of a <code>JTextField</code>.
     * <code>initialSelectionValue</code> is the initial value to prompt
     * the user with. It is up to the UI to decide how best to represent
     * the <code>selectionValues</code>, but usually a
     * <code>JComboBox</code>, <code>JList</code>, or
     * <code>JTextField</code> will be used.
     *
     * @param parentComponent  the parent <code>Component</code> for the
     *			dialog
     * @param message  the <code>Object</code> to display
     * @param messageType the type of message to be displayed:
     *                  <code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *			<code>JOptionPane.WARNING_MESSAGE</code>,
     *                  <code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     * @param icon     the <code>Icon</code> image to display
     * @param selectionValues an array of <code>Object</code>s that
     *			gives the possible selections
     * @param initialSelectionValue the value used to initialize the input
     *                 field
     * @param listener The listener for SheetEvents.
     */
    public static void showInputSheet(Component parentComponent,
    Object message, int messageType, Icon icon,
    Object[] selectionValues, Object initialSelectionValue, SheetListener listener) {

        JOptionPane    pane = new JOptionPane(message, messageType,
        JOptionPane.OK_CANCEL_OPTION, icon,
        null, null);

        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        pane.setComponentOrientation(((parentComponent == null) ?
        JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

        int style = styleFromMessageType(messageType);
        JSheet sheet = createSheet(pane, parentComponent, style);

        pane.selectInitialValue();

        /*
        sheet.addWindowListener(new WindowAdapter() {
           public void windowClosed(WindowEvent evt) {
               sheet.dispose();
           }
        });*/
        sheet.addSheetListener(listener);
        sheet.show();
    }
    /**
     * Brings up an information-message sheet.
     *
     * @param parentComponent determines the <code>Frame</code> in
     *		which the dialog is displayed; if <code>null</code>,
     *		or if the <code>parentComponent</code> has no
     *		<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     */
    public static void showMessageSheet(Component parentComponent,
    Object message) {
        showMessageSheet(parentComponent, message,
        JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Brings up a sheet that displays a message using a default
     * icon determined by the <code>messageType</code> parameter.
     *
     * @param parentComponent determines the <code>Frame</code>
     *		in which the dialog is displayed; if <code>null</code>,
     *		or if the <code>parentComponent</code> has no
     *		<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     * @param messageType the type of message to be displayed:
     *                  <code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *			<code>JOptionPane.WARNING_MESSAGE</code>,
     *                  <code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     */
    public static void showMessageSheet(Component parentComponent,
    Object message, int messageType) {
        showMessageSheet(parentComponent, message,  messageType, null);
    }

    /**
     * Brings up a sheet displaying a message, specifying all parameters.
     *
     * @param parentComponent determines the <code>Frame</code> in which the
     *			sheet is displayed; if <code>null</code>,
     *			or if the <code>parentComponent</code> has no
     *			<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     * @param messageType the type of message to be displayed:
     *                  <code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *			<code>JOptionPane.WARNING_MESSAGE</code>,
     *                  <code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     * @param icon      an icon to display in the sheet that helps the user
     *                  identify the kind of message that is being displayed
     */
    public static void showMessageSheet(Component parentComponent,
    Object message, int messageType, Icon icon) {
        showOptionSheet(parentComponent, message, JOptionPane.DEFAULT_OPTION,
        messageType, icon, null, null, null);
    }
    /**
     * Brings up a sheet with a specified icon, where the initial
     * choice is determined by the <code>initialValue</code> parameter and
     * the number of choices is determined by the <code>optionType</code>
     * parameter.
     * <p>
     * If <code>optionType</code> is <code>YES_NO_OPTION</code>,
     * or <code>YES_NO_CANCEL_OPTION</code>
     * and the <code>options</code> parameter is <code>null</code>,
     * then the options are
     * supplied by the look and feel.
     * <p>
     * The <code>messageType</code> parameter is primarily used to supply
     * a default icon from the look and feel.
     *
     * @param parentComponent determines the <code>Frame</code>
     *			in which the dialog is displayed;  if
     *                  <code>null</code>, or if the
     *			<code>parentComponent</code> has no
     *			<code>Frame</code>, the sheet is displayed as a dialog.
     * @param message   the <code>Object</code> to display
     * @param optionType an integer designating the options available on the
     *			dialog: <code>YES_NO_OPTION</code>,
     *			or <code>YES_NO_CANCEL_OPTION</code>
     * @param messageType an integer designating the kind of message this is,
     *                  primarily used to determine the icon from the
     *			pluggable Look and Feel: <code>JOptionPane.ERROR_MESSAGE</code>,
     *			<code>JOptionPane.INFORMATION_MESSAGE</code>,
     *                  <code>JOptionPane.WARNING_MESSAGE</code>,
     *                  <code>JOptionPane.QUESTION_MESSAGE</code>,
     *			or <code>JOptionPane.PLAIN_MESSAGE</code>
     * @param icon      the icon to display in the dialog
     * @param options   an array of objects indicating the possible choices
     *                  the user can make; if the objects are components, they
     *                  are rendered properly; non-<code>String</code>
     *			objects are
     *                  rendered using their <code>toString</code> methods;
     *                  if this parameter is <code>null</code>,
     *			the options are determined by the Look and Feel
     * @param initialValue the object that represents the default selection
     *                  for the dialog; only meaningful if <code>options</code>
     *			is used; can be <code>null</code>
     * @param listener The listener for SheetEvents.
     */
    public static void showOptionSheet(Component parentComponent,
    Object message, int optionType, int messageType,
    Icon icon, Object[] options, Object initialValue, SheetListener listener) {

        JOptionPane pane = new JOptionPane(message, messageType,
        optionType, icon,
        options, initialValue);

        pane.setInitialValue(initialValue);
        pane.setComponentOrientation(((parentComponent == null) ?
        JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

        int style = styleFromMessageType(messageType);
        JSheet sheet = createSheet(pane, parentComponent, style);
        pane.selectInitialValue();
        sheet.addSheetListener(listener);
        sheet.show();
    }

    private static int styleFromMessageType(int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                return JRootPane.ERROR_DIALOG;
            case JOptionPane.QUESTION_MESSAGE:
                return JRootPane.QUESTION_DIALOG;
            case JOptionPane.WARNING_MESSAGE:
                return JRootPane.WARNING_DIALOG;
            case JOptionPane.INFORMATION_MESSAGE:
                return JRootPane.INFORMATION_DIALOG;
            case JOptionPane.PLAIN_MESSAGE:
            default:
                return JRootPane.PLAIN_DIALOG;
        }
    }
    private static JSheet createSheet(final JOptionPane pane, Component parentComponent,
    int style) {
        Window window = getWindowForComponent(parentComponent);
        final JSheet sheet;
        if (window instanceof Frame) {
            sheet = new JSheet((Frame) window);
        } else {
            sheet = new JSheet((Dialog) window);
        }

        Container contentPane = sheet.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(pane, BorderLayout.CENTER);
        sheet.setResizable(false);
        sheet.addWindowListener(new WindowAdapter() {
            private boolean gotFocus = false;
            int count;
            public void windowClosing(WindowEvent we) {
                pane.setValue(null);
            }
            public void windowClosed(WindowEvent we) {
                if (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
                    sheet.fireOptionSelected(pane);
                }
            }
            public void windowGainedFocus(WindowEvent we) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    //Ugly dirty hack: JOptionPane.selectInitialValue() is protected.
                    //So we call directly into the UI. This may cause mayhem,
                    //because we override the encapsulation.
                    //pane.selectInitialValue();
                    OptionPaneUI  ui = pane.getUI();
                    if (ui != null) {
                        ui.selectInitialValue(pane);
                    }
                    gotFocus = true;
                }
            }
        });
        sheet.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
                // reset value to ensure closing works properly
                pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            }
        });
        pane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // Let the defaultCloseOperation handle the closing
                // if the user closed the window without selecting a button
                // (newValue = null in that case).  Otherwise, close the sheet.
                if (sheet.isVisible() && event.getSource() == pane &&
                (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) &&
                event.getNewValue() != null &&
                event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                    sheet.setVisible(false);
                    sheet.fireOptionSelected(pane);
                }
            }
        });
        sheet.pack();
        return sheet;
    }
    /**
     * Returns the specified component's toplevel <code>Frame</code> or
     * <code>Dialog</code>.
     *
     * @param parentComponent the <code>Component</code> to check for a
     *		<code>Frame</code> or <code>Dialog</code>
     * @return the <code>Frame</code> or <code>Dialog</code> that
     *		contains the component, or the default
     *         	frame if the component is <code>null</code>,
     *		or does not have a valid
     *         	<code>Frame</code> or <code>Dialog</code> parent
     */
    static Window getWindowForComponent(Component parentComponent) {
        if (parentComponent == null)
            return JOptionPane.getRootFrame();
        if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
            return (Window)parentComponent;
        return getWindowForComponent(parentComponent.getParent());
    }

    /**
     * Displays a "Save File" file chooser sheet. Note that the
     * text that appears in the approve button is determined by
     * the L&F.
     *
     * @param    parent  the parent component of the dialog,
     *			can be <code>null</code>.
     * @param listener The listener for SheetEvents.
     */
    public static void showSaveSheet(JFileChooser chooser, Component parent, SheetListener listener)  {
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        showSheet(chooser, parent, null, listener);
    }
    /**
     * Displays an "Open File" file chooser sheet. Note that the
     * text that appears in the approve button is determined by
     * the L&F.
     *
     * @param    parent  the parent component of the dialog,
     *			can be <code>null</code>.
     * @param listener The listener for SheetEvents.
     */
    public static void showOpenSheet(JFileChooser chooser, Component parent, SheetListener listener) {
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        showSheet(chooser, parent, null, listener);
    }
    /**
     * Displays a custom file chooser sheet with a custom approve button.
     *
     * @param   parent  the parent component of the dialog;
     *			can be <code>null</code>
     * @param   approveButtonText the text of the <code>ApproveButton</code>
     * @param listener The listener for SheetEvents.
     */
    public static void showSheet(final JFileChooser chooser, Component parent,
    String approveButtonText, SheetListener listener) {
        if(approveButtonText != null) {
            chooser.setApproveButtonText(approveButtonText);
            chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
        }

        // Begin Create Dialog
        Frame frame = parent instanceof Frame ? (Frame) parent
        : (Frame)SwingUtilities.getAncestorOfClass(Frame.class, parent);

        String title = chooser.getUI().getDialogTitle(chooser);
        chooser.getAccessibleContext().setAccessibleDescription(title);

        final JSheet sheet = new JSheet(frame);
        sheet.addSheetListener(listener);

        Container contentPane = sheet.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(chooser, BorderLayout.CENTER);
        // End Create Dialog

        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int option;
                if (evt.getActionCommand().equals("ApproveSelection")) {
                    option = JFileChooser.APPROVE_OPTION;
                } else {
                    option = JFileChooser.CANCEL_OPTION;
                }
                sheet.hide();
                sheet.fireOptionSelected(chooser, option);
                chooser.removeActionListener(this);
            }
        };
        chooser.addActionListener(actionListener);
        sheet.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                sheet.fireOptionSelected(chooser, JFileChooser.CANCEL_OPTION);
                chooser.removeActionListener(actionListener);
            }
        });
        chooser.rescanCurrentDirectory();
        sheet.pack();
        sheet.show();
    }
}
