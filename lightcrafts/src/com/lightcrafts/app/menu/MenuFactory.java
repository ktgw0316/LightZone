/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** A factory for menu items that uses localized resources to define names,
  * mnemonics, and accelerators for menus and menu items.
  * <p>
  * The configuration data are all in a properties file at
  * com/lightcrafts/app/menu/resources/MenuFactory.properties (or a
  * locale-dependent variant).  This file has a syntax which is documented in
  * comments within the file.
  * <p>
  * Note that mnemonics may be turned off globally or for a specific platform.
  */

class MenuFactory {

    private static boolean useMacMnemonics = false;

    static void configureMenuItem(String key, JMenuItem item) {
        String name = getName(key);
        KeyStroke accel = getAccelerator(key);
        int mnem = getMnemonic(key);
        if (name == null) {
            throw new RuntimeException(
                "Can't configure menu item \"" + key + "\""
            );
        }
        item.setText(name);
        if (accel != null) {
            item.setAccelerator(accel);
        }
        if (mnem > 0) {
            item.setMnemonic(mnem);
        }
    }

    static void configureMenu(String key, JMenu menu) {
        String name = getName(key);
        int mnem = getMnemonic(key);
        if (name == null) {
            throw new RuntimeException(
                "Can't configure menu item \"" + key + "\""
            );
        }
        menu.setText(name);
        if (mnem > 0) {
            menu.setMnemonic(mnem);
        }
    }

    static JMenuItem createMenuItem(String key) {
        String name = getName(key);
        KeyStroke accel = getAccelerator(key);
        int mnem = getMnemonic(key);
        if (name == null) {
            throw new RuntimeException(
                "Can't configure menu item \"" + key + "\""
            );
        }
        JMenuItem item = new JMenuItem(name);
        if (accel != null) {
            item.setAccelerator(accel);
        }
        if (mnem > 0) {
            item.setMnemonic(mnem);
        }
        return item;
    }

    static JRadioButtonMenuItem createRadioButtonMenuItem(String key) {
        String name = getName(key);
        KeyStroke accel = getAccelerator(key);
        int mnem = getMnemonic(key);
        if (name == null) {
            throw new RuntimeException(
                "Can't configure menu item \"" + key + "\""
            );
        }
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
        if (accel != null) {
            item.setAccelerator(accel);
        }
        if (mnem > 0) {
            item.setMnemonic(mnem);
        }
        return item;
    }

    static JMenu createMenu(String key) {
        String name = getName(key);
        int mnem = getMnemonic(key);
        if (name == null) {
            throw new RuntimeException(
                "Can't configure menu item \"" + key + "\""
            );
        }
        JMenu menu = new JMenu(name);
        if (mnem > 0) {
            menu.setMnemonic(mnem);
        }
        return menu;
    }

    final static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/app/menu/MenuFactory"
    );

    private static String PlatformSuffix = "";

    static {
        if ( Platform.getType() == Platform.Linux ) {
            PlatformSuffix = "_Linux";
        }
        else if (Platform.getType() == Platform.MacOSX) {
            PlatformSuffix = "_Mac";
        }
        else if (Platform.getType() == Platform.Windows) {
            PlatformSuffix = "_Win";
        }
    }

    private final static String NameSuffix = "_Name";

    private final static String AcceleratorSuffix = "_Accelerator";

    private final static String MnemonicSuffix = "_Mnemonic";

    private static final String AcceleratorModifier =
        Platform.getType() == Platform.MacOSX ? "meta" : "ctrl";
    // I didn't use Toolkit.getMenuShortcutKeyMask(),
    // because I couldn't find a way to combine integer key masks with
    // the modifier syntax of KeyStroke ("shift", "ctrl", etc.).

    private static String getName(String key) {
        String s = getPlatformString(key + NameSuffix);
        if ((! useMacMnemonics) && Platform.getType() == Platform.MacOSX) {
            s = s.replaceAll("\\([A-Z0-9]\\)$", "");
        }
        return s;
    }

    private static KeyStroke getAccelerator(String key) {
        String s = getPlatformString(key + AcceleratorSuffix);
        KeyStroke stroke = KeyStroke.getKeyStroke(
            AcceleratorModifier + " " + s
        );
        return stroke;
    }

    private static int getMnemonic(String key) {
        if ((! useMacMnemonics) && Platform.getType() == Platform.MacOSX) {
            return -1;
        }
        String s = getPlatformString(key + MnemonicSuffix);
        int code = getKeyCode(s);
        return code;
    }

    private static String getPlatformString(String key) {
        String s = getStringOrNull(key + PlatformSuffix);
        if (s == null) {
            s = getStringOrNull(key);
        }
        return s;
    }

    private static String getStringOrNull(String key) {
        try {
            return Resources.getString(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static int getKeyCode(String vkString) {
        try {
            Class clazz = KeyEvent.class;
            Field field = clazz.getField(vkString);
            Integer virtualKeyCode = (Integer) field.get(null);
            return virtualKeyCode.intValue();
        }
        catch (NoSuchFieldException e) {
            return -1;
        }
        catch (IllegalAccessException e) {
            return -1;
        }
        catch (NullPointerException e) {
            return -1;
        }
    }
}
