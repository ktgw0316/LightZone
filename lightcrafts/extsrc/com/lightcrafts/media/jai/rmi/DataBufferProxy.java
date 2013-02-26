/*
 * $RCSfile: DataBufferProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:49 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.lightcrafts.media.jai.util.DataBufferUtils;

/**
 * This class is a serializable proxy for a DataBuffer from which the
 * DataBuffer may be reconstituted.
 *
 *
 * @since EA3
 */
public class DataBufferProxy implements Serializable {
    /** The DataBuffer. */
    private transient DataBuffer dataBuffer;

    /**
      * Constructs a <code>DataBufferProxy</code> from a
      * <code>DataBuffer</code>.
      *
      * @param source The <code>DataBuffer</code> to be serialized.
      */
    public DataBufferProxy(DataBuffer source) {
        dataBuffer = source;
    }

    /**
      * Retrieves the associated <code>DataBuffer</code>.
      * @return The (perhaps reconstructed) <code>DataBuffer</code>.
      */
    public DataBuffer getDataBuffer() {
        return dataBuffer;
    }

    // XXX Note that there is potential for some form of data compression in
    // the readObject() and writeObject() methods.

    /**
      * Serialize the <code>DataBufferProxy</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
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
            throw new RuntimeException(JaiI18N.getString("DataBufferProxy0"));
        }
        out.writeObject(dataArray);
    }

    /**
      * Deserialize the <code>DataBufferProxy</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
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
            throw new RuntimeException(JaiI18N.getString("DataBufferProxy0"));
        }
    }
}
