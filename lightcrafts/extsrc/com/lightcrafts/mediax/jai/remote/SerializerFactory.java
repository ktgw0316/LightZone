/*
 * $RCSfile: SerializerFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:54 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.remote;

import java.awt.RenderingHints;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;
import com.lightcrafts.media.jai.rmi.SerializerImpl;
import com.lightcrafts.media.jai.rmi.InterfaceState;

/**
 * A utility class which provides factory methods for obtaining
 * <code>Serializer</code> instances.
 *
 * <p> The <code>Serializer</code>s are maintained in a centralized repository
 * which is organized based on the classes supported by the
 * <code>Serializer</code>s and the order in which the <code>Serializer</code>s
 * were registered.  Convenience methods similar to those defined in the
 * <code>Serializer</code> class are also provided.  These enable
 * functionality equivalent to a single <code>Serializer</code> which
 * supports all the classes supported by the aggregate of all
 * <code>Serializer</code>s resident in the repository.
 *
 * <p> By default <code>Serializer</code>s for the following classes
 * are registered by JAI:
 *
 * <ul>
 * <li><code>java.awt.RenderingHints</code>
 * <br>(entries which are neither <code>Serializable</code> nor supported by
 * <code>SerializerFactory</code> are omitted; support for specific
 * <code>RenderingHints.Key</code> subclasses may be added by new
 * <code>Serializer</code>s);</li>
 * <li><code>java.awt.RenderingHints.Key</code>
 * <br>(limited to <code>RenderingHints.Key</code>s defined in
 * <code>java.awt.RenderingHints</code> and <code>com.lightcrafts.mediax.jai.JAI</code>);
 * </li>
 * <li><code>java.awt.Shape</code>;</li>
 * <li><code>java.awt.image.DataBufferByte</code>;</li>
 * <li><code>java.awt.image.DataBufferShort</code>;</li>
 * <li><code>java.awt.image.DataBufferUShort</code>;</li>
 * <li><code>java.awt.image.DataBufferInt</code>;</li>
 * <li><code>com.lightcrafts.mediax.jai.DataBufferFloat</code>;</li>
 * <li><code>com.lightcrafts.mediax.jai.DataBufferDouble</code>;</li>
 * <li><code>java.awt.image.ComponentSampleModel</code>;</li>
 * <li><code>java.awt.image.BandedSampleModel</code>;</li>
 * <li><code>java.awt.image.PixelInterleavedSampleModel</code>;</li>
 * <li><code>java.awt.image.SinglePixelPackedSampleModel</code>;</li>
 * <li><code>java.awt.image.MultiPixelPackedSampleModel</code>;</li>
 * <li><code>com.lightcrafts.mediax.jai.ComponentSampleModelJAI</code>;</li>
 * <li><code>java.awt.image.Raster</code>
 * <br>(limited to <code>Raster</code>s which have a <code>DataBuffer</code>
 * and <code>SampleModel</code> supported by a <code>Serializer</code>);</li>
 * <li><code>java.awt.image.WritableRaster</code>
 * <br>(limited to <code>WritableRaster</code>s which have a
 * <code>DataBuffer</code> and <code>SampleModel</code> supported by a
 * <code>Serializer</code>);</li>
 * <li><code>java.awt.image.ComponentColorModel</code>;</li>
 * <li><code>java.awt.image.IndexColorModel</code>;</li>
 * <li><code>java.awt.image.DirectColorModel</code>;</li>
 * <li><code>com.lightcrafts.mediax.jai.FloatColorModel</code>;</li>
 * <li><code>java.awt.image.renderable.RenderContext</code>;</li>
 * <br>(constrained by the aforementioned limitations of
 * the <code>RenderingHints</code> <code>Serializer</code>);</li>
 * <li><code>java.awt.image.RenderedImage</code>
 * <br>(limited to <code>RenderedImage</code>s which have <code>Raster</code>s
 * and a <code>ColorModel</code> supported by a <code>Serializer</code>);</li>
 * <li><code>java.awt.image.WritableRenderedImage</code>
 * <br>(limited to <code>WritableRenderedImage</code>s which have
 * <code>Raster</code>s and a <code>ColorModel</code> supported by a
 * <code>Serializer</code>);</li>
 * <li><code>java.io.Serializable</code>;</li>
 * <li><code>java.util.HashSet</code>
 * <br>(elements which are neither <code>Serializable</code> nor supported by
 * <code>SerializerFactory</code> are omitted);</li>
 * <li><code>java.util.Hashtable</code>
 * <br>(entries which are neither <code>Serializable</code> nor supported by
 * <code>SerializerFactory</code> are omitted);</li>
 * <li><code>java.util.Vector</code>
 * <br>(elements which are neither <code>Serializable</code> nor supported by
 * <code>SerializerFactory</code> are omitted);</li>
 * </ul>
 *
 * @see SerializableState
 * @see Serializer
 * @see java.io.Serializable
 *
 * @since JAI 1.1
 */
public final class SerializerFactory {

    /**
     * <code>Serializer</code> hashed by supported <code>Class</code>.
     * The value is a <code>Serializer</code> if there is only one for the
     * given <code>Class</code> or a <code>Vector</code> if there are more.
     */
    private static Hashtable repository = new Hashtable();

    /**
     * Singleton instance of <code>Serializer</code> for use with already
     * <code>Serializable</code> classes.
     */
    private static Serializer serializableSerializer = new SerSerializer();

    static final SerializableState NULL_STATE =
        new SerializableState() {
                public Class getObjectClass() {
                    return Object.class;
                }

                public Object getObject() {
                    return null;
                }
            };

    static {
        // Load all <code>Serializer</code>s defined in com.lightcrafts.media.jai.rmi.
        SerializerImpl.registerSerializers();
    }

    protected SerializerFactory() {}

    /**
     * Adds a <code>Serializer</code> to the repository.
     *
     * @param s The <code>Serializer</code>s to be added to the repository.
     * @exception IllegalArgumentException if <code>s</code> is
     *            <code>null</code>
     */
    public static synchronized void registerSerializer(Serializer s) {
        if(s == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Class c = s.getSupportedClass();

        if(repository.containsKey(c)) {
            Object value = repository.get(c);
            if(value instanceof Vector) {
                ((Vector)value).add(0, s);
            } else {
                Vector v = new Vector(2);
                v.add(0, s);
                v.add(1, value);
                repository.put(c, v);
            }
        } else {
            repository.put(c, s);
        }
    }

    /**
     * Removes a <code>Serializer</code> from the repository.
     *
     * @param s The <code>Serializer</code>s to be removed from the repository.
     * @exception IllegalArgumentException if <code>s</code> is
     *            <code>null</code>
     */
    public static synchronized void unregisterSerializer(Serializer s) {
        if(s == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Class c = s.getSupportedClass();
        Object value = repository.get(c);
        if(value != null) {
            if(value instanceof Vector) {
                Vector v = (Vector)value;
                v.remove(s);
                if(v.size() == 1) {
                    repository.put(c, v.get(0));
                }
            } else {
                repository.remove(c);
            }
        }
    }

    /**
     * Retrieves an array of all <code>Serializer</code>s currently
     * resident in the repository which directly support the specified
     * <code>Class</code>.  <code>Serializer</code>s which support
     * a superclass of the specified class and permit subclass
     * serialization will not be included.
     *
     * @param c The class for which <code>Serializer</code>s will be
     *          retrieved.
     * @exception IllegalArgumentException if <code>c</code> is
     *            <code>null</code>.
     */
    public static synchronized Serializer[] getSerializers(Class c) {
        if(c == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        Object value = repository.get(c);
        Serializer[] result = null;
        if(value == null && Serializable.class.isAssignableFrom(c)) {
            result = new Serializer[] {serializableSerializer};
        } else if(value instanceof Vector) {
            result =  (Serializer[])((Vector)value).toArray(new Serializer[0]);
        } else if(value != null) {
            result = new Serializer[] {(Serializer)value};
        }
        return result;
    }

    /**
     * Retrieves a <code>Serializer</code> for a given class <code>c</code>.
     * If more than one <code>Serializer</code> is available for the class
     * then the most recently registered <code>Serializer</code> will be
     * returned.  If no registered <code>Serializer</code> exists which
     * directly supports the specified class, i.e., one for which the
     * <code>getSupportedClass()</code> returns a value equal to the
     * specified class, then a <code>Serializer</code> may be returned
     * which is actually registered against a superclass but permits
     * subclass serialization.
     *
     * @param c The class for which <code>Serializer</code>s will be
     *          retrieved.
     * @return A <code>Serializer</code> which supports the specified class.
     *         or <code>null</code> if none is available.
     * @exception IllegalArgumentException if <code>c</code> is
     *            <code>null</code>.
     *
     * @see java.awt.image.BandedSampleModel
     * @see java.awt.image.ComponentSampleModel
     */
    public static synchronized Serializer getSerializer(Class c) {
        if(c == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Get the value from the repository.
        Object value = repository.get(c);

        // If null, attempt to find a superclass Serializer.
        if(value == null) {
            Class theClass = c;
            while(theClass != java.lang.Object.class) {
                Class theSuperclass = theClass.getSuperclass();
                if(isSupportedClass(theSuperclass)) {
                    Serializer s = getSerializer(theSuperclass);
                    if(s.permitsSubclasses()) {
                        value = s;
                        break;
                    }
                }
                theClass = theSuperclass;
            }
        }

        if(value == null && Serializable.class.isAssignableFrom(c)) {
            value = serializableSerializer;
        }

        // Return the highest priority Serializer or null.
        return value instanceof Vector ?
            (Serializer)((Vector)value).get(0) : (Serializer)value;
    }

    /**
     * Whether there is currently resident in the repository a
     * <code>Serializer</code> the <code>getSupportedClass()</code>
     * method of which returns a value equal to the parameter supplied
     * to this method according to <code>equals()</code>.
     *
     * @param c The class to be tested for compatibility.
     * @return Whether the specified class is directly supported.
     * @exception IllegalArgumentException if <code>c</code> is
     *            <code>null</code>
     */
    public static boolean isSupportedClass(Class c) {
        if(c == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if(Serializable.class.isAssignableFrom(c)) {
            return true;
        }
        return repository.containsKey(c);
    }

    /**
     * Returns an array listing all classes and interfaces on which the
     * <code>isSupportedClass()</code> method of this class may be invoked
     * and return <code>true</code>.
     *
     * @return An array of all supported classes and interfaces.
     */
    public static Class[] getSupportedClasses() {
        Class[] classes = new Class[repository.size() + 1];
        repository.keySet().toArray(classes);
        classes[classes.length-1] = Serializable.class;
        return classes;
    }

    /**
     * Determines the <code>Class</code> of which the deserialized form of the
     * supplied <code>Class</code> will be an instance.  Specifically, this
     * method returns the <code>Class</code> of the <code>Object</code>
     * returned by invoking <code>getObject()</code> on the
     * <code>SerializableState</code> returned by <code>getState()</code>
     * after the state object has been serialized and deserialized.  The
     * returned value will equal the supplied argument unless there is no
     * <code>Serializer</code> explicitly registered for this class but there
     * is a <code>Serializer</code> registered for a superclass with a
     * <code>permitsSubclasses()</code> method that returns
     * <code>true</code>.
     *
     * @param The <code>Class</code> for which the deserialized class type is
     *        requested.
     * @return The deserialized <code>Class</code> or <code>null</code>.
     * @exception IllegalArgumentException if <code>c</code> is
     *            <code>null</code>
     */
    public static Class getDeserializedClass(Class c) {
        if(c == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Class deserializedClass = null;

        // Try to find a superclass Serializer.
        if(isSupportedClass(c)) {
            deserializedClass = c;
        } else {
            Class theClass = c;
            while(theClass != java.lang.Object.class) {
                Class theSuperclass = theClass.getSuperclass();
                if(isSupportedClass(theSuperclass)) {
                    Serializer s = getSerializer(theSuperclass);
                    if(s.permitsSubclasses()) {
                        deserializedClass = theSuperclass;
                        break;
                    }
                }
                theClass = theSuperclass;
            }
        }

        return deserializedClass;
    }

    /**
     * Converts an object into a state-preserving object which may
     * be serialized.  If the class of the object parameter is supported
     * explicitly, i.e., <code>isSupportedClass(o.getClass())</code>
     * returns <code>true</code>, then the object will be converted into
     * a form which may be deserialized into an instance of the same class.
     * If the class is not supported explicitly but implements one or
     * more supported interfaces, then it will be converted into a
     * form which may be deserialized into an instance of an unspecified
     * class which implements all interfaces which are both implemented by
     * the class of the object and supported by some <code>Serializer</code>
     * currently resident in the repository.  If the object is
     * <code>null</code>, the returned <code>SerializableState</code> will
     * return <code>null</code> from its <code>getObject()</code> method
     * and <code>java.lang.Object.class</code> from its
     * <code>getObjectClass()</code> method.
     *
     * @param o The object to be converted into a serializable form.
     * @param h Configuration parameters the exact nature of which is
     *          <code>Serializer</code>-dependent.  If <code>null</code>,
     *          reasonable default settings should be used.
     * @return A serializable form of the supplied object.
     * @exception IllegalArgumentException if <code>o</code> is
     *            non-<code>null</code> and either
     *            <code>isSupportedClass(o.getClass())</code> returns
     *            <code>false</code>, or <code>o</code>
     *            is not an instance of a class supported by a
     *            <code>Serializer</code> in the repository or which
     *            implements at least one interface supported by some
     *            <code>Serializer</code>s in the repository.
     */
    public static SerializableState getState(Object o, RenderingHints h) {
        if(o == null) {
            return NULL_STATE;
        }

        Class c = o.getClass();
        SerializableState state = null;
        if(isSupportedClass(c)) {
            // Found an explicit Serializer.
            Serializer s = getSerializer(c);
            state = s.getState(o, h);
        } else {
            // Try to find a superclass Serializer.
            Class theClass = c;
            while(theClass != java.lang.Object.class) {
                Class theSuperclass = theClass.getSuperclass();
                if(isSupportedClass(theSuperclass)) {
                    Serializer s = getSerializer(theSuperclass);
                    if(s.permitsSubclasses()) {
                        state = s.getState(o, h);
                        break;
                    }
                }
                theClass = theSuperclass;
            }

            if(state == null) {

                // Try an interface Serializer.
                Class[] interfaces = getInterfaces(c);
                Vector serializers = null;
                int numInterfaces = (interfaces == null) ? 0: interfaces.length;
                for(int i = 0; i < numInterfaces; i++) {
                    Class iface = interfaces[i];
                    if(isSupportedClass(iface)) {
                        if(serializers == null) {
                            serializers = new Vector();
                        }
                        serializers.add(getSerializer(iface));
                    }
                }

                int numSupportedInterfaces =
                    serializers == null ? 0 : serializers.size();
                if(numSupportedInterfaces == 0) {
                    throw new IllegalArgumentException(
                                  JaiI18N.getString("SerializerFactory1"));
                } else if(numSupportedInterfaces == 1) {
                    state = ((Serializer)serializers.get(0)).getState(o, h);
                } else {
                    Serializer[] sArray =
                        (Serializer[])serializers.toArray(new Serializer[0]);
                    state = new InterfaceState(o, sArray, h);
                }
            }
        }

        return state;
    }

    /**
     * Retrieve the interfaces implemented by the specified class and all
     * its superclasses.
     */
    private static Class[] getInterfaces(Class c) {
        if(c == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        ArrayList interfaces = new ArrayList();
        Class laClasse = c;
        while(!(laClasse == java.lang.Object.class)) {
            Class[] iFaces = laClasse.getInterfaces();
            if(iFaces != null) {
                for(int i = 0; i < iFaces.length; i++) {
                    interfaces.add(iFaces[i]);
                }
            }
            laClasse = laClasse.getSuperclass();
        }

        return interfaces.size() == 0 ?
            null : (Class[])interfaces.toArray(new Class[interfaces.size()]);
    }

    /**
     * A convenience wrapper around
     * <code>getState(Object o,&nbsp;RenderingHints h)</code> with
     * the <code>RenderingHints</code> parameter <code>h</code> set
     * to <code>null</code>.
     */
    public static final SerializableState getState(Object o) {
        return getState(o, null);
    }
}

/**
 * A <code>Serializer</code> for <code>Serializable</code> objects.
 */
class SerSerializer implements Serializer {
    SerSerializer() {}

    public Class getSupportedClass() {
        return Serializable.class;
    }

    public boolean permitsSubclasses() {
        return true;
    }

    public SerializableState getState(Object o, RenderingHints h) {
        if(o == null) {
            return SerializerFactory.NULL_STATE;
        } else if(!(o instanceof Serializable)) {
            throw new IllegalArgumentException(JaiI18N.getString("SerializerFactory2"));
        }
        return new SerState((Serializable)o);
    }
}

/**
 * <code>SerializableState</code> which simply wraps an object that is
 * already an instance of <code>Serializable</code>.
 */
class SerState implements SerializableState {
    private Serializable object;

    SerState(Serializable object) {
        if(object == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        this.object = object;
    }

    public Class getObjectClass() {
        return object.getClass();
    }

    public Object getObject() {
        return object;
    }
}
