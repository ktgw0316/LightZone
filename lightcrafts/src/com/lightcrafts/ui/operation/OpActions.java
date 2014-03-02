/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.Engine;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.OperationType;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/** This is a factory for all the add-operation actions that can be performed
  * on the OpStack.  It's auto-generated from the set of GenericOperations
  * discovered in an Engine.
  */

public class OpActions {

    // Key to icon images in the Action property tables
    public final static String IconImageKey = "IconImageKey";

    // Key to Operation keys in the Action property tables
    private final static String OpKeyPropertyKey = "OpKey";

    // Ordered list of Operation resource keys
    private final static List<String> OpKeys;

    private final static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/ui/operation/OpActions"
    );
    private final static ResourceBundle Resources_ALL = ResourceBundle.getBundle(
        "com/lightcrafts/ui/operation/OpActions_ALL"
    );

    static {
        OpKeys = new LinkedList<String>();
        String names = Resources_ALL.getString("Operations");
        StringTokenizer tokens = new StringTokenizer(names, ",");
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            OpKeys.add(token);
        }
    }

    private static Comparator<Action> OpComparator = new Comparator<Action>() {
        public int compare(Action a1, Action a2) {
            String n1 = (String) a1.getValue(OpKeyPropertyKey);
            String n2 = (String) a2.getValue(OpKeyPropertyKey);
            int i1 = OpKeys.indexOf(n1);
            int i2 = OpKeys.indexOf(n2);
            return i1 - i2;
        }
    };

    private List<Action> actions;

    OpActions(Engine engine, final OpStack stack) {

        actions = new LinkedList<Action>();

        Action action;
        String key;
        String name;
        BufferedImage image;
        Icon icon;
        String tooltip;

        // Add an Action for the ZoneOperation:

        key = "ZoneMapper";
        name = getName(key);
        image = getImage(key);
        icon = new ImageIcon(image);
        tooltip = Resources.getString(key + "_Tooltip");
        action = new AbstractAction(name, icon) {
            public void actionPerformed(ActionEvent event) {
                stack.addZoneControl();
            }
        };
        action.putValue(IconImageKey, image);
        action.putValue(Action.SHORT_DESCRIPTION, tooltip);
        action.putValue(OpKeyPropertyKey, key);
        actions.add(action);

        // Add an Action for the CloneOperation:

        key = "Clone";
        name = getName(key);
        image = getImage(key);
        icon = new ImageIcon(image);
        tooltip = Resources.getString(key + "_Tooltip");
        action = new AbstractAction(name, icon) {
            public void actionPerformed(ActionEvent event) {
                stack.addCloneControl();
            }
        };
        action.putValue(IconImageKey, image);
        action.putValue(Action.SHORT_DESCRIPTION, tooltip);
        action.putValue(OpKeyPropertyKey, key);
        actions.add(action);

        // Add an Action for the SpotOperation:

        key = "Spot";
        name = getName(key);
        image = getImage(key);
        icon = new ImageIcon(image);
        tooltip = Resources.getString(key + "_Tooltip");
        action = new AbstractAction(name, icon) {
            public void actionPerformed(ActionEvent event) {
                stack.addSpotControl();
            }
        };
        action.putValue(IconImageKey, image);
        action.putValue(Action.SHORT_DESCRIPTION, tooltip);
        action.putValue(OpKeyPropertyKey, key);
        actions.add(action);

        // Add an Action for each OperationType supported by the Engine:

        Collection<OperationType> types = engine.getGenericOperationTypes();
        for (final OperationType type : types) {

            key = getOperationKey(type);

            // Maybe this OperationType is excluded:
            int index = OpKeys.indexOf(key);
            if (index < 0) {
                continue;
            }
            name = getName(type);
            image = getImage(key);
            icon = new ImageIcon(image);
            tooltip = getToolTip(key);
            action = new AbstractAction(name, icon) {
                public void actionPerformed(ActionEvent event) {
                    stack.addGenericControl(type);
                }
            };
            action.putValue(IconImageKey, image);
            action.putValue(Action.SHORT_DESCRIPTION, tooltip);
            action.putValue(OpKeyPropertyKey, key);
            actions.add(action);
        }
        // Sort the actions by their order in opNames:

        Collections.sort(actions, OpComparator);
    }

    // Get placeholder Actions from resources, instead of inspecting an
    // Engine.  Useful for the no-Document display mode.
    static List<Action> createStaticAddActions() {
        LinkedList<Action> actions = new LinkedList<Action>();
        for (String key : OpKeys) {
            String name = getName(key);
            BufferedImage image = getImage(key);
            Icon icon = new ImageIcon(image);
            String tooltip = Resources.getString(key + "_Tooltip");
            Action action = new AbstractAction(name, icon) {
                public void actionPerformed(ActionEvent event) {
                    // Do nothing.  (Always disabled.)
                }
            };
            action.putValue(IconImageKey, image);
            action.putValue(Action.SHORT_DESCRIPTION, tooltip);
            action.putValue(OpKeyPropertyKey, key);
            action.setEnabled(false);
            actions.add(action);
        }
        return actions;
    }

    List<Action> getActions() {
        return new LinkedList<Action>(actions);
    }

    static String getName(Operation op) {
        OperationType type = op.getType();
        return getName(type);
    }

    static String getName(OperationType type) {
        String key = getOperationKey(type);
        String name = getName(key);
        return (name != null) ? name : type.getName();
    }

    static BufferedImage getIcon(Operation op) {
        OperationType type = op.getType();
        return getIcon(type);
    }

    static BufferedImage getIcon(OperationType type) {
        String key = getOperationKey(type);
        return getImage(key);
    }

    // Translate an OperationType into a key String for resources
    private static String getOperationKey(OperationType type) {
        String key = type.getName();
        key = key.replaceAll(" ", "");
        return key;
    }

    // Return a user-presentable name for the given resource key, or null
    private static String getName(String key) {
        try {
            return Resources.getString(key + "_Name");
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static BufferedImage getImage(String key) {
        String iconName;
        try {
            iconName = Resources_ALL.getString(key + "_Icon");
        } catch (MissingResourceException e) {
            iconName = "generic";
        }
        return getIconFromResources(iconName);
    }

    // Get an icon from resources by its resource name,
    // or if that doesn't work, get the generic icon
    private static BufferedImage getIconFromResources(String name) {
        String path = "resources/" + name + ".png";
        URL url = OpActions.class.getResource(path);
        try {
            return ImageIO.read(url);
        }
        catch (IOException e) {
            return null;
        }
    }

    // Return the configured tooltip for the given key, or null
    private String getToolTip(String key) {
        try {
            return Resources.getString(key + "_Tooltip");
        }
        catch (MissingResourceException e) {
            return null;
        }
    }
}
