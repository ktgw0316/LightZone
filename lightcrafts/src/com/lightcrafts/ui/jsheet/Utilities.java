/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.jsheet;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Oct 9, 2005
 * Time: 11:04:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class Utilities {
    static final void setWindowAlpha(Window w, int value) {
        if (w == null) return;

        if (w.getPeer() == null) {
            w.pack();
        }
        java.awt.peer.ComponentPeer peer = w.getPeer();

        try {
            // Alpha API for Apple's Java 1.4 + 1.5 on Mac OS X 10.4 Tiger.
            invoke(peer, "setAlpha", (float) (value / 255f));
        } catch (NoSuchMethodException e) {
            // Alpha API for Apple's Java 1.3.
            try {
                invoke(peer, "_setAlpha", value);
            } catch (NoSuchMethodException e2) {
                // Platform neutral API
                w.setBackground(new Color(255,255,255,value));
                if (w instanceof RootPaneContainer) {
                    ((RootPaneContainer) w).getContentPane().setBackground(new Color(255,255,255,0));
                }
            }
        }
    }

    public static Object invoke(Object obj, String methodName, boolean newValue)
    throws NoSuchMethodException {
        try {
            Method method =  obj.getClass().getMethod(methodName,  new Class[] { Boolean.TYPE} );
           return method.invoke(obj, new Object[] { new Boolean(newValue)});
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(methodName+" is not accessible");
        } catch (InvocationTargetException e) {
            // The method is not supposed to throw exceptions
            throw new InternalError(e.getMessage());
        }
    }

    public static Object invoke(Object obj, String methodName, int newValue)
    throws NoSuchMethodException {
        try {
            Method method =  obj.getClass().getMethod(methodName,  new Class[] { Integer.TYPE} );
            return method.invoke(obj, new Object[] { new Integer(newValue)});
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(methodName+" is not accessible");
        } catch (InvocationTargetException e) {
            // The method is not supposed to throw exceptions
            throw new InternalError(e.getMessage());
        }
    }

    public static Object invoke(Object obj, String methodName, float newValue)
    throws NoSuchMethodException {
        try {
            Method method =  obj.getClass().getMethod(methodName,  new Class[] { Float.TYPE} );
            return method.invoke(obj, new Object[] { new Float(newValue)});
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(methodName+" is not accessible");
        } catch (InvocationTargetException e) {
            // The method is not supposed to throw exceptions
            throw new InternalError(e.getMessage());
        }
    }

    public static void invokeIfExists(Object obj, String methodName, boolean newValue) {
        try {
             invoke(obj, methodName, newValue);
        } catch (NoSuchMethodException e) {
            // ignore
        }
    }
}
