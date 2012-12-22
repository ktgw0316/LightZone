/*
 * $RCSfile: VectorState.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:55 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.util.Iterator;

/**
 * This class is a serializable proxy for a Vector object.
 * <br>(entries which are neither <code>Serializable</code> nor supported by
 * <code>SerializerFactory</code> are omitted);
 *
 *
 * @since 1.1
 */
public class VectorState extends SerializableStateImpl {
    /** 
     * Returns the classes supported by this SerializableState.
     */
    public static Class[] getSupportedClasses() {
            return new Class[] {Vector.class};
    }

    /**
      * Constructs a <code>VectorState</code> from a
      * <code>Vector</code> object.
      *
      * @param c The <code>Class</code> of the object to be serialized.
      * @param o The <code>Vector</code> object to be serialized.
      * @param h The <code>RebderingHint</code> for this serialization.
      */
    public VectorState(Class c, Object o, RenderingHints h) {
        super(c, o, h);
    }

    /**
     * Serialize the VectorState.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // -- Create a serializable form of the Vector object. --
	Vector vector = (Vector)theObject;
	Vector serializableVector = new Vector();
	Iterator iterator = vector.iterator();

        // If there are hints, add them to the vector.
        while (iterator.hasNext()){
            Object object = iterator.next();
	    Object serializableObject = getSerializableForm(object);
            serializableVector.add(serializableObject);
        }

        // Write serialized form to the stream.
        out.writeObject(serializableVector);
    }

    /**
     * Deserialize the VectorState.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Read serialized form from the stream.
        Vector serializableVector = (Vector)in.readObject();

        // Create an empty Vector object.
        Vector vector = new Vector();
	theObject = vector;

        // If the vector is empty just return.
        if (serializableVector.isEmpty()) {
            return;
        }

        // Get an enumeration of the vector keys.
        Iterator iterator = serializableVector.iterator();

        // Loop over the vector keys.
        while (iterator.hasNext()) {
            // Get the next key element.
            Object serializableObject = iterator.next();
            Object object = getDeserializedFrom(serializableObject);

            // Add an entry to the vector.
            vector.add(object);
        }
    }
}
