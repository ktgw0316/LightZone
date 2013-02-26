/*
 * $RCSfile: InterfaceState.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:51 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.Serializer;

/**
 * Class enabling serialization of an object which implements multiple
 * interfacs supported by SerializerFactory.
 *
 * @since 1.1
 */
public class InterfaceState implements SerializableState {
    // The object before serialization, the Proxy afterwards.
    private transient Object theObject;
    private transient Serializer[] theSerializers; // Not deserialized.
    private transient RenderingHints hints; // Not deserialized.

    public InterfaceState(Object o,
                          Serializer[] serializers,
                          RenderingHints h) {
        if(o == null || serializers == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        theObject = o;
        theSerializers = serializers;
        hints = h == null ? null : (RenderingHints)h.clone();
    }

    public Object getObject() {
        return theObject;
    }

    public Class getObjectClass() {
        return theObject.getClass();
    }

    /**
      * Serialize the <code>InterfaceState</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {

        int numSerializers = theSerializers.length;
        out.writeInt(numSerializers);
        for(int i = 0; i < numSerializers; i++) {
            Serializer s = theSerializers[i];
            out.writeObject(s.getSupportedClass());
            out.writeObject(s.getState(theObject, hints));
        }
    }

    /**
      * Deserialize the <code>InterfaceState</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        int numInterfaces = in.readInt();
        Class[] interfaces = new Class[numInterfaces];
        SerializableState[] implementations =
            new SerializableState[numInterfaces];
        for(int i = 0; i < numInterfaces; i++) {
            interfaces[i] = (Class)in.readObject();
            implementations[i] = (SerializableState)in.readObject();
        }

        InvocationHandler handler =
            new InterfaceHandler(interfaces, implementations);

        theObject =
            Proxy.newProxyInstance(JAI.class.getClassLoader(),
                                   interfaces,
                                   handler);
    }
}

class InterfaceHandler implements InvocationHandler {
    private Hashtable interfaceMap;

    public InterfaceHandler(Class[] interfaces,
                            SerializableState[] implementations) {
        if(interfaces == null || implementations == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if(interfaces.length != implementations.length) {
            throw new IllegalArgumentException(JaiI18N.getString("InterfaceHandler0"));
        }

        int numInterfaces = interfaces.length;
        interfaceMap = new Hashtable(numInterfaces);
        for(int i = 0; i < numInterfaces; i++) {
            Class iface = interfaces[i];
            SerializableState state = implementations[i];

            if(!iface.isAssignableFrom(state.getObjectClass())) {
                throw new RuntimeException(JaiI18N.getString("InterfaceHandler1"));
            }

            Object impl = state.getObject();
            interfaceMap.put(iface, impl);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args)
        throws IllegalAccessException, InvocationTargetException {
        Class key = method.getDeclaringClass();
        if(!interfaceMap.containsKey(key)) {
            Class[] classes =
                (Class[])interfaceMap.keySet().toArray(new Class[0]);
            for(int i = 0; i < classes.length; i++) {
                Class aClass = classes[i];
                if(key.isAssignableFrom(aClass)) {
                    interfaceMap.put(key, interfaceMap.get(aClass));
                    break;
                }
            }
            if(!interfaceMap.containsKey(key)) {
                throw new RuntimeException(key.getName()+
                                           JaiI18N.getString("InterfaceHandler2"));
            }
        }

        Object result= null;
        try {
            Object impl = interfaceMap.get(key);
            result = method.invoke(impl, args);
        } catch(IllegalAccessException e) {
            throw new RuntimeException(method.getName()+
                                       JaiI18N.getString("InterfaceHandler3"));
        }

        return result;
    }
}
