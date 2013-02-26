/*
 * $RCSfile: DataBufferState.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:50 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.lightcrafts.media.jai.util.DataBufferUtils;

/**
 * This class is a serializable proxy for a DataBuffer from which the
 * DataBuffer may be reconstituted.
 *
 *
 * @since 1.1
 */
public class DataBufferState extends SerializableStateImpl {

    /** DataBufferFloat and DataBufferDouble core classes or null. */
    private static Class[] J2DDataBufferClasses = null;

    /** The DataBuffer. */
    private transient DataBuffer dataBuffer;

    // Initialize J2DDataBufferClasses.
    static {
        try {
            Class dbfClass = Class.forName("java.awt.image.DataBufferFloat");
            Class dbdClass = Class.forName("java.awt.image.DataBufferDouble");
            J2DDataBufferClasses = new Class[] {dbfClass, dbdClass};
        } catch(ClassNotFoundException e) {
            // Ignore the exception.
        }
    }

    public static Class[] getSupportedClasses() {
        Class[] supportedClasses = null;
        if(J2DDataBufferClasses != null) {
            // Java 2 1.4.0 and higher.
            supportedClasses = new Class[] {
                DataBufferByte.class,
                DataBufferShort.class,
                DataBufferUShort.class,
                DataBufferInt.class,
                J2DDataBufferClasses[0],
                J2DDataBufferClasses[1],
                com.lightcrafts.mediax.jai.DataBufferFloat.class,
                com.lightcrafts.mediax.jai.DataBufferDouble.class,
                com.lightcrafts.media.jai.codecimpl.util.DataBufferFloat.class,
                com.lightcrafts.media.jai.codecimpl.util.DataBufferDouble.class
            };
        } else {
            // Java 2 pre-1.4.0.
            supportedClasses = new Class[] {
                DataBufferByte.class,
                DataBufferShort.class,
                DataBufferUShort.class,
                DataBufferInt.class,
                com.lightcrafts.mediax.jai.DataBufferFloat.class,
                com.lightcrafts.mediax.jai.DataBufferDouble.class,
                com.lightcrafts.media.jai.codecimpl.util.DataBufferFloat.class,
                com.lightcrafts.media.jai.codecimpl.util.DataBufferDouble.class
            };
        }

        return supportedClasses;
    }

    /**
      * Constructs a <code>DataBufferState</code> from a
      * <code>DataBuffer</code>.
      *
      * @param source The <code>DataBuffer</code> to be serialized.
      * @param o The <code>SampleModel</code> to be serialized.
      * @param h The <code>RenderingHints</code> (ignored).
      */
    public DataBufferState(Class c, Object o, RenderingHints h) {
        super(c, o, h);
    }

    // XXX Note that there is potential for some form of data compression in
    // the readObject() and writeObject() methods.

    /**
      * Serialize the <code>DataBufferState</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        DataBuffer dataBuffer = (DataBuffer)theObject;

        // Write serialized form to the stream.
        int dataType = dataBuffer.getDataType();
        out.writeInt(dataType);
        out.writeObject(dataBuffer.getOffsets());
        out.writeInt(dataBuffer.getSize());
        Object dataArray = null;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataArray = ((DataBufferByte)dataBuffer).getBankData();
            break;
        case DataBuffer.TYPE_SHORT:
            dataArray = ((DataBufferShort)dataBuffer).getBankData();
            break;
        case DataBuffer.TYPE_USHORT:
            dataArray = ((DataBufferUShort)dataBuffer).getBankData();
            break;
        case DataBuffer.TYPE_INT:
            dataArray = ((DataBufferInt)dataBuffer).getBankData();
            break;
        case DataBuffer.TYPE_FLOAT:
            dataArray = DataBufferUtils.getBankDataFloat(dataBuffer);
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataArray = DataBufferUtils.getBankDataDouble(dataBuffer);
            break;
        default:
            throw new RuntimeException(JaiI18N.getString("DataBufferState0"));
        }
        out.writeObject(dataArray);
    }

    /**
      * Deserialize the <code>DataBufferState</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        DataBuffer dataBuffer = null;

        // Read serialized form from the stream.
        int dataType = -1;
        int[] offsets = null;
        int size = -1;
        Object dataArray = null;
        dataType = in.readInt();
        offsets = (int[])in.readObject();
        size = in.readInt();
        dataArray = in.readObject();

        // Restore the transient DataBuffer.
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataBuffer =
                new DataBufferByte((byte[][])dataArray, size, offsets);
            break;
        case DataBuffer.TYPE_SHORT:
            dataBuffer =
                new DataBufferShort((short[][])dataArray, size, offsets);
            break;
        case DataBuffer.TYPE_USHORT:
            dataBuffer =
                new DataBufferUShort((short[][])dataArray, size, offsets);
            break;
        case DataBuffer.TYPE_INT:
            dataBuffer =
                new DataBufferInt((int[][])dataArray, size, offsets);
            break;
        case DataBuffer.TYPE_FLOAT:
            dataBuffer =
                DataBufferUtils.createDataBufferFloat((float[][])dataArray, size, offsets);
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataBuffer =
                DataBufferUtils.createDataBufferDouble((double[][])dataArray, size, offsets);
            break;
        default:
            throw new RuntimeException(JaiI18N.getString("DataBufferState0"));
        }

        theObject = dataBuffer;
    }
}
