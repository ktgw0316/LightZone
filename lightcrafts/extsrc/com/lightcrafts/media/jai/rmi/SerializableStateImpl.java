/*
 * $RCSfile: SerializableStateImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:54 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.io.Serializable;
import java.lang.reflect.Method;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * Framework class for adding <code>Serializer</code>s based on
 * <code>SerializableState</code> implementations which support one or
 * more classes or interfaces.
 *
 * <p> Extending classes MUST:
 * <ol>
 * <li> be public;</li>
 * <li> provide a single public constructor with exactly the same signature as
 * the protected constructor of this class;</li>
 * <li> provide a static override of <code>getSupportedClasses()</code>;</li>
 * <li> implement the (de)serialization methods <code>writeObject()</code>
 * and <code>readObject()</code>; and</li>
 * <li> add the class to <code>SerializerImpl.registerSerializers()</code> as
 * <pre>
 *       registerSerializers(MySerializableState.class);
 * </pre></li>
 * </ol>
 *
 * @since 1.1
 */
public abstract class SerializableStateImpl implements SerializableState {
    protected Class<?> theClass;
    protected transient Object theObject;

    /**
     * Returns the classes supported by this SerializableState.
     * Subclasses MUST override this method with their own STATIC method.
     */
    public static Class[] getSupportedClasses() {
        throw new RuntimeException(JaiI18N.getString("SerializableStateImpl0"));
    }

    /**
     * Whether the SerializableStateImpl permits its Serializer to
     * serialize subclasses of the supported class(es).
     * Subclasses SHOULD override this method to return "true" with their
     * own STATIC method IF AND ONLY IF they support subclass serialization.
     */
    public static boolean permitsSubclasses() {
        return false;
    }

    /**
     * Constructor.  All subclasses MUST have exactly ONE constructor with
     * the SAME signature as this constructor.
     */
    protected SerializableStateImpl(Class<?> c, Object o, RenderingHints h) {
        if (c == null || o == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SerializableStateImpl1"));
        } else {
            boolean isInterface = c.isInterface();
            if (isInterface && !c.isInstance(o)) {
                throw new IllegalArgumentException(JaiI18N.getString("SerializableStateImpl2"));
            } else if (!isInterface) {
                boolean permitsSubclasses = false;
                try {
                    Method m =
                        this.getClass().getMethod("permitsSubclasses", (Class<?>[]) null);
                    permitsSubclasses 
                        = ((Boolean)m.invoke(null, (Object[]) null)).booleanValue();
                } catch (Exception e){
                    throw new IllegalArgumentException(JaiI18N.getString("SerializableStateImpl5"));
                }

                if (!permitsSubclasses && !c.equals(o.getClass())) {
                    throw new IllegalArgumentException(JaiI18N.getString("SerializableStateImpl3"));
                } else if (permitsSubclasses &&
                           !c.isAssignableFrom(o.getClass())) {
                    throw new IllegalArgumentException(JaiI18N.getString("SerializableStateImpl4"));
                }
            }
        }
        theClass = c;
        theObject = o;
    }

    public Class<?> getObjectClass() {
        return theClass;
    }

    public Object getObject() {
        return theObject;
    }

    protected Object getSerializableForm(Object object) {
        if (object instanceof Serializable)
            return object;
	if (object != null)
	    try {
		object = SerializerFactory.getState(object, null);
	    } catch (Exception e) {
		object = null;
	    }
        return object;
    }

    protected Object getDeserializedFrom(Object object) {
        if (object instanceof SerializableState)
            object = ((SerializableState)object).getObject();
        return object;
    }
}
