/*
 * $RCSfile: SerializableRenderedImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:53 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.remote;

/*
XXX: RFE (from Bob):
If the SM can't be serialized perhaps a different SM know to be serializable
could be created and the data copied.
*/

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RemoteImage;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoder;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoder;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderFactory;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A serializable wrapper class for classes which implement the
 * <code>RenderedImage</code> interface.
 *
 * <p> A <code>SerializableRenderedImage</code> provides a means to serialize
 * a <code>RenderedImage</code>.  Transient fields are handled using
 * <code>Serializer</code>s registered with <code>SerializerFactory</code>.
 * Two means are available for providing the wrapped
 * <code>RenderedImage</code> data to a remote version of a
 * <code>SerializableRenderedImage</code> object: either via deep copy or by
 * "on-demand" copying.  If a deep copy is requested, the entire image
 * <code>Raster</code> is copied during object serialization and tiles are
 * extracted from it as needed using the <code>Raster.createChild()</code>
 * method.  If a deep copy is not used, the image data are transmitted
 * "on-demand" using socket communications.  If the request is made on the
 * local host, the image data are provided in both cases merely by forwarding
 * the request to the wrapped <code>RenderedImage</code>.  Note that a single
 * <code>SerializableRenderedImage</code> object should be able to service
 * multiple remote hosts.
 *
 * <p> The properties associated with the <code>RenderedImage</code> being 
 * wrapped are serialized and accessible to a remote version of a
 * <code>SerializableRenderedImage</code> object. However it should be noted
 * that only those properties which are serializable are available to the
 * <code>SerializableRenderedImage</code> object.
 *
 * <p> This class makes no guarantee as to the stability of the data of the
 * wrapped image, at least in the case where a deep copy is not made.
 * Consequently if the data of a <code>RenderedImage</code> change but
 * affected tiles have already been transmitted then the modifications will
 * not be visible remotely.  For example, this implies that a
 * <code>SerializableRenderedImage</code> should not be used to wrap a
 * <code>RenderedOp</code> the data of which are subject to change if the
 * chain in which the node is present is edited.  Instead the
 * <code>SerializableRenderedImage</code> should be used to wrap the image
 * returned by invoking either <code>getRendering()</code> or
 * <code>createInstance()</code> on the <code>RenderedOp</code>.  A similar
 * situation will obtain if the wrapped image is a
 * <code>WritableRenderedImage</code>.  If in this case the wrapped image
 * is also a <code>PlanarImage</code>, then the image returned by
 * <code>createSnapshot()</code> should be wrapped instead.
 *
 * <p> An example of the usage of this class is as follows:
 *
 * <pre>
 * import java.io.IOException;
 * import java.io.ObjectInputStream;
 * import java.io.ObjectOutputStream;
 * import java.io.Serializable;
 *
 * public class SomeSerializableClass implements Serializable {
 *     protected transient RenderedImage image;
 *
 *     // Fields omitted.
 *
 *     public SomeSerializableClass(RenderedImage image) {
 *         this.image = image;
 *     }
 *
 *     // Methods omitted.
 *
 *     // Serialization method.
 *     private void writeObject(ObjectOutputStream out) throws IOException {
 *         out.defaultWriteObject();
 *         out.writeObject(new SerializableRenderedImage(image));
 *     }
 *
 *     // Deserialization method.
 *     private void readObject(ObjectInputStream in)
 *         throws IOException, ClassNotFoundException {
 *         in.defaultReadObject();
 *         image = (RenderedImage)in.readObject();
 *     }
 * }
 * </pre>
 *
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.WritableRenderedImage
 * @see com.lightcrafts.mediax.jai.PlanarImage
 * @see com.lightcrafts.mediax.jai.RenderedOp
 *
 *
 * @since JAI 1.1
 */
// NB: This class was added in EA3 to com.lightcrafts.media.jai.rmi and made
// public only in JAI 1.1.
public final class SerializableRenderedImage
    implements RenderedImage, Serializable {
    /** Value to indicate the server socket timeout period (milliseconds). */
    private static final int SERVER_TIMEOUT = 60000; // XXX 1 minute?

    /** Message indicating that a client will not connect again. */
    private static final String CLOSE_MESSAGE = "CLOSE";

    /** Message indicating that the server read the client's close message. */
    private static final String CLOSE_ACK = "CLOSE_ACK";

    /** The unique ID of this image. */
    private Object UID;

    /** Flag indicating whether this is a data server. */
    private transient boolean isServer;

    /** Flag indicating whether the source image is a RemoteImage. */
    private boolean isSourceRemote;

    /** The RenderedImage source of this object (server only). */
    private transient RenderedImage source;

    /** The X coordinate of the image's upper-left pixel. */
    private int minX;

    /** The Y coordinate of the image's upper-left pixel. */
    private int minY;

    /** The image's width in pixels. */
    private int width;

    /** The image's height in pixels. */
    private int height;

    /** The horizontal index of the leftmost column of tiles. */
    private int minTileX;

    /** The vertical index of the uppermost row of tiles. */
    private int minTileY;

    /** The number of tiles along the tile grid in the horizontal direction. */
    private int numXTiles;

    /** The number of tiles along the tile grid in the vertical direction. */
    private int numYTiles;

    /** The width of a tile. */
    private int tileWidth;

    /** The height of a tile. */
    private int tileHeight;

    /** The X coordinate of the upper-left pixel of tile (0, 0). */
    private int tileGridXOffset;

    /** The Y coordinate of the upper-left pixel of tile (0, 0). */
    private int tileGridYOffset;

    /** The image's SampleModel. */
    private transient SampleModel sampleModel = null;

    /** The image's ColorModel. */
    private transient ColorModel colorModel = null;

    /** The image's sources, stored in a Vector. */
    private transient Vector sources = null;

    /** A Hashtable containing the image properties. */
    private transient Hashtable properties = null;

    /** Flag indicating whether to use a deep copy of the source image. */
    private boolean useDeepCopy;

    /** A Rectangle indicating the entire image bounds. */
    private Rectangle imageBounds;

    /** The entire image Raster (client only). */
    private transient Raster imageRaster;

    /** The Internet Protocol (IP) address of the instantiating host. */
    private InetAddress host;

    /** The port on which the data server is listening. */
    private int port;

    /** Flag indicating that the server is available for connections. */
    private transient boolean serverOpen = false;

    /** The server socket for image data transfer (server only). */
    private transient ServerSocket serverSocket = null;

    /** The thread in which the data server is running (server only). */
    private transient Thread serverThread;

    /** The tile codec format name is TileCodec is used */
    private String formatName;

    /** The specified <code>OperationRegistry</code> when TileCodec is used */
    private transient OperationRegistry registry;

    /**
     * A table of counts of remote references to instances of this class
     * (server only).
     *
     * <p> This table consists of entries with the keys being instances of
     * <code>SerializableRenderedImage</code> and the values being
     * <code>Integer</code>s the int value of which represents the number
     * of remote <code>SerializableRenderedImage</code> objects which could
     * potentially request a socket connection with the associated key. This
     * table is necessary to prevent the garbage collector of the interpreter
     * in which the server <code>SerializableRenderedImage</code> object is
     * instantiated from finalizing the object - and thereby closing its
     * server socket - when that object could still receive socket connection
     * requests from its remote clients. The reference to the object in the
     * static class variable ensures that the object will not be prematurely
     * finalized.
     */
    private static transient Hashtable remoteReferenceCount;

    /** Indicate that tilecodec is used in the transfering or not */
    private boolean useTileCodec = false;

    /** Cache the encoder factory */
    private transient TileDecoderFactory tileDecoderFactory = null;

    /** Cache the decoder factory */
    private transient TileEncoderFactory tileEncoderFactory = null;

    /** Cache the encoding/decoding parameters */
    private TileCodecParameterList encodingParam = null;
    private TileCodecParameterList decodingParam = null;

    /**
     * Increment the remote reference count of the argument.
     *
     * <p> If the argument is not already in the remote reference table,
     * add it to the table with a count value of unity. If it exists in
     * table, increment its count value.
     *
     * @parameter o The object the count value of which is to be incremented.
     */
    private static synchronized void incrementRemoteReferenceCount(Object o) {
        if (remoteReferenceCount == null) {
            remoteReferenceCount = new Hashtable();
            remoteReferenceCount.put(o, new Integer(1));
        } else {
            Integer count = (Integer)remoteReferenceCount.get(o);
            if (count == null) {
                remoteReferenceCount.put(o, new Integer(1));
            } else {
                remoteReferenceCount.put(o,
                                         new Integer(count.intValue()+1));
            }
        }
    }

    /**
     * Decrement the remote reference count of the argument.
     *
     * <p> If the count value of the argument exists in the table its count
     * value is decremented unless the count value is unity in which case the
     * entry is removed from the table.
     *
     * @parameter o The object the count value of which is to be decremented.
     */
    private static synchronized void decrementRemoteReferenceCount(Object o) {
        if (remoteReferenceCount != null) {
            Integer count = (Integer)remoteReferenceCount.get(o);
            if (count != null) {
                if (count.intValue() == 1) {
                    remoteReferenceCount.remove(o);
                } else {
                    remoteReferenceCount.put(o,
                                             new Integer(count.intValue()-1));
                }
            }
        }
    }

    /**
     * The default constructor.
     */
    SerializableRenderedImage() {}

    /**
     * Constructs a <code>SerializableRenderedImage</code> wrapper for a
     * <code>RenderedImage</code> source.  Image data may be serialized
     * tile-by-tile or via a single deep copy.  Tile encoding and
     * decoding may be effected via a <code>TileEncoder</code> and
     * <code>TileDecoder</code> specified by format name.
     *
     * <p> It may be noted that if the <code>TileCodec</code> utilizes
     * <code>Serializer</code>s for encoding the image data, and none
     * is available for the <code>DataBuffer</code> of the supplied
     * image, an error/exception may be encountered.
     *
     * @param source The <code>RenderedImage</code> source.
     * @param useDeepCopy Whether a deep copy of the entire image Raster
     *                    will be made during object serialization.
     * @param registry The <code>OperationRegistry</code> to use in
     *                 creating the <code>TileEncoder</code>.  The
     *                 <code>TileDecoder</code> will of necessity be
     *                 created using the default <code>OperationRegistry</code>
     *                 as the specified <code>OperationRegistry</code> is not
     *                 serialized.  If <code>null</code> the default registry
     *                 will be used.
     * @param formatName The name of the format used to encode the data.
     *                   If <code>null</code> simple tile serialization will
     *                   be performed either directly or by use of a "raw"
     *                   <code>TileCodec</code>.
     * @param encodingParam The parameters to be used for data encoding.  If
     *                      <code>null</code> the default encoding
     *                      <code>TileCodecParameterList</code> for this
     *                      format will be used.  Ignored if
     *                      <code>formatName</code> is <code>null</code>.
     * @param decodingParam The parameters to be used for data decoding.  If
     *                      <code>null</code> a complementary
     *                      <code>TileCodecParameterList</code> will be
     *                      derived from <code>encodingParam</code>.  Ignored
     *                      if <code>formatName</code> is <code>null</code>.
     *
     * @exception IllegalArgumentException if <code>source</code>
     *            is <code>null</code>.
     * @exception IllegalArgumentException if no <code>Serializer</code>s
     *            are available for the types of
     *            <code>SampleModel</code>, and <code>ColorModel</code>
     *            contained in the specified image.
     */
    public SerializableRenderedImage(RenderedImage source,
                                     boolean useDeepCopy,
                                     OperationRegistry registry,
                                     String formatName,
                                     TileCodecParameterList encodingParam,
                                     TileCodecParameterList decodingParam)
        throws NotSerializableException {
        this(source, useDeepCopy, false);

	// When the provided format name is null, return to directly serialize
	// this image
	if (formatName == null)
	    return;

	this.formatName = formatName;

	// When the provided registry is null, use the default one
	if (registry == null)
	    registry = JAI.getDefaultInstance().getOperationRegistry();
	this.registry = registry;

	// Fix 4640094: When the provided encodingParam is null, use the default one
	if (encodingParam == null) {
	    TileCodecDescriptor tcd =
		getTileCodecDescriptor("tileEncoder", formatName);
	    encodingParam = tcd.getDefaultParameters("tileEncoder");
	} else if (!formatName.equals(encodingParam.getFormatName())) {
            throw new IllegalArgumentException(JaiI18N.getString("UseTileCodec0"));
        }

	// Fix 4640094: When the provided decodingParam is null, use the default one
	if (decodingParam == null) {
	    TileCodecDescriptor tcd =
		getTileCodecDescriptor("tileDecoder", formatName);
	    decodingParam = tcd.getDefaultParameters("tileDecoder");
	} else if (!formatName.equals(decodingParam.getFormatName())) {
            throw new IllegalArgumentException(JaiI18N.getString("UseTileCodec1"));
        }

        tileEncoderFactory =
            (TileEncoderFactory)registry.getFactory("tileEncoder", formatName);
        tileDecoderFactory =
            (TileDecoderFactory)registry.getFactory("tileDecoder", formatName);
        if (tileEncoderFactory == null || tileDecoderFactory == null)
            throw new RuntimeException(JaiI18N.getString("UseTileCodec2"));

        this.encodingParam = encodingParam;
        this.decodingParam = decodingParam;
        useTileCodec = true;
    }

    /**
     * Constructs a <code>SerializableRenderedImage</code> wrapper for a
     * <code>RenderedImage</code> source.  Image data may be serialized
     * tile-by-tile or via a single deep copy.  No <code>TileCodec</code>
     * will be used, i.e., data will be transmitted using the serialization
     * protocol for <code>Raster</code>s.
     *
     * @param source The <code>RenderedImage</code> source.
     * @param useDeepCopy Whether a deep copy of the entire image Raster
     * will be made during object serialization.
     *
     * @exception IllegalArgumentException if <code>source</code>
     * is <code>null</code>.
     * @exception IllegalArgumentException if no <code>Serializer</code>s
     *            are available for the types of <code>DataBuffer</code>,
     *            <code>SampleModel</code>, and <code>ColorModel</code>
     *            contained in the specified image.
     */
    public SerializableRenderedImage(RenderedImage source,
                                     boolean useDeepCopy) {
	this(source, useDeepCopy, true);
    }

    /**
     * Constructs a <code>SerializableRenderedImage</code> wrapper for a
     * <code>RenderedImage</code> source.  Image data will be serialized
     * tile-by-tile if possible.  No <code>TileCodec</code>
     * will be used, i.e., data will be transmitted using the serialization
     * protocol for <code>Raster</code>s.
     *
     * @param source The <code>RenderedImage</code> source.
     * @exception IllegalArgumentException if <code>source</code>
     *            is <code>null</code>.
     * @exception IllegalArgumentException if no <code>Serializer</code>s
     *            are available for the types of <code>DataBuffer</code>,
     *            <code>SampleModel</code>, and <code>ColorModel</code>
     *            contained in the specified image.
     */
    public SerializableRenderedImage(RenderedImage source) {
        this(source, false, true);
    }

    /**
     * Constructs a <code>SerializableRenderedImage</code> wrapper for a
     * <code>RenderedImage</code> source.
     *
     * @param source The <code>RenderedImage</code> source.
     * @param useDeepCopy Whether a deep copy of the entire image Raster
     * will be made during object serialization.
     * @param checkDataBuffer Whether checking serializable for DataBuffer
     * or not. If no <code>TileCodec</code> will be used, set it to true.
     * If <code>TileCodec</code> will be used, it is set to false.
     */

    private SerializableRenderedImage(RenderedImage source,
				      boolean useDeepCopy,
				      boolean checkDataBuffer) {

        UID = ImageUtil.generateID(this);

	if (source == null){
	    throw new IllegalArgumentException(JaiI18N.getString("SerializableRenderedImage0"));
	}

	SampleModel sm = source.getSampleModel();
        if (sm != null &&
	    SerializerFactory.getSerializer(sm.getClass()) == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SerializableRenderedImage2"));
        }

	ColorModel cm = source.getColorModel();
        if (cm != null &&
	    SerializerFactory.getSerializer(cm.getClass()) == null) {
            throw new IllegalArgumentException(JaiI18N.getString("SerializableRenderedImage3"));
        }

	if (checkDataBuffer) {
	    Raster ras = source.getTile(source.getMinTileX(), source.getMinTileY());
	    if (ras != null) {
		DataBuffer db = ras.getDataBuffer();
		if (db != null &&
		    SerializerFactory.getSerializer(db.getClass()) == null)
		    throw new IllegalArgumentException(JaiI18N.getString("SerializableRenderedImage4"));
	    }
	}

        // Set server flag.
        isServer = true;

        // Cache the deep copy flag.
        this.useDeepCopy = useDeepCopy;

        // Cache the parameter.
        this.source = source;

        // Set remote source flag.
        this.isSourceRemote = source instanceof RemoteImage;

        // Initialize RenderedImage fields.
        minX = source.getMinX();
        minY = source.getMinY();
        width = source.getWidth();
        height = source.getHeight();
        minTileX = source.getMinTileX();
        minTileY = source.getMinTileY();
        numXTiles = source.getNumXTiles();
        numYTiles = source.getNumYTiles();
        tileWidth = source.getTileWidth();
        tileHeight = source.getTileHeight();
        tileGridXOffset = source.getTileGridXOffset();
        tileGridYOffset = source.getTileGridYOffset();
        sampleModel = source.getSampleModel();
        colorModel = source.getColorModel();
        sources = new Vector();
        sources.add(source);
        properties = new Hashtable();
        // XXX Property names should use CaselessStringKey for the
        // keys so that case is preserved.
        String[] propertyNames = source.getPropertyNames();
        if (propertyNames != null) {
            for (int i = 0; i < propertyNames.length; i++) {
                properties.put(propertyNames[i],
                               source.getProperty(propertyNames[i]));
            }
        }

        // Initialize the image bounds.
        imageBounds = new Rectangle(minX, minY, width, height);

        // Initialize the host field.
        try {
            host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        }

        // Unset the server availability flag.
        serverOpen = false;
    }

    /**
     * Private implementation of tile server.
     */
    private class TileServer implements Runnable {
        /**
         * Provide Rasters to clients on request.
         *
         * <p> This method is called by the data server thread when a deep copy
         * of the source image Raster is not being used. A socket connection is
         * set up at a well known address to which clients may connect. After a
         * client connects it transmits a Rectangle object which is read by
         * this method. The Raster corresponding to this Rectangle is then
         * retrieved from the source image and transmitted back over the
         * socket connection.
         *
         * <p> The server loop will continue until this object is garbage
         * collected.
         */
        public void run() {
            // Loop while the server availability flag is set.
            while (serverOpen) {
                // Wait for a client connection request.
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
		    socket.setSoLinger(true,1);
                } catch (InterruptedIOException e) {
                    // accept() timeout: restart loop to check
                    // availability flag.
                    continue;
                } catch (SocketException e) {
                    sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage5"),
                                            new ImagingException(JaiI18N.getString("SerializableRenderedImage5"), e));
//                    throw new RuntimeException(e.getMessage());
                } catch (IOException e) {
                    sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage6"),
                                            new ImagingException(JaiI18N.getString("SerializableRenderedImage6"), e));
                }

                // Get the socket input and output streams and wrap object
                // input and output streams around them, respectively.
                InputStream in = null;
                OutputStream out = null;
                ObjectInputStream objectIn = null;
                ObjectOutputStream objectOut = null;
                try {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    objectIn = new ObjectInputStream(in);
                    objectOut = new ObjectOutputStream(out);
                } catch (IOException e) {
                    sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage7"),
                                            new ImagingException(JaiI18N.getString("SerializableRenderedImage7"), e));
//                    throw new RuntimeException(e.getMessage());
                }

                // Read the Object from the object stream.
                Object obj = null;
                try {
                    obj = objectIn.readObject();
                } catch (IOException e) {
                    sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage8"),
                                            new ImagingException(JaiI18N.getString("SerializableRenderedImage8"), e));
//                    throw new RuntimeException(e.getMessage());
                } catch (ClassNotFoundException e) {
                    sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage9"),
                                            new ImagingException(JaiI18N.getString("SerializableRenderedImage9"), e));
                }

                // Switch according to object class; ignore unsupported types.
                if (obj instanceof String &&
                    ((String)obj).equals(CLOSE_MESSAGE)) {

		    try {
			objectOut.writeObject(CLOSE_ACK);
		    } catch (IOException e) {
			sendExceptionToListener(JaiI18N.getString(
						  "SerializableRenderedImage17"),
			                      new ImagingException(JaiI18N.getString(
						  "SerializableRenderedImage17"), e));
			// throw new RuntimeException(e.getMessage());
		    }

                    // Decrement the remote reference count.
                    decrementRemoteReferenceCount(this);
                } else if (obj instanceof Rectangle) {

                    // Retrieve the Raster of data from the source image.
                    Raster raster = source.getData((Rectangle)obj);
                    // Write the serializable Raster to the
                    // object output stream.

                    if (useTileCodec) {
                        byte[] buf = encodeRasterToByteArray(raster);
                        try {
                            objectOut.writeObject(buf);
                        } catch (IOException e) {
                            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage10"),
                                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage10"), e));
//                            throw new RuntimeException(e.getMessage());
                        }
                    }
                    else {
                        try {
                            objectOut.writeObject(SerializerFactory.getState(raster, null));
                        } catch (IOException e) {
                            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage10"),
                                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage10"), e));
//                            throw new RuntimeException(e.getMessage());
                        }
                    }
                }

                // XXX Concerning serialization of properties, perhaps the
                // best approach would be to serialize all the properties up
                // front if a deep copy were being made but otherwise to wait
                // until the first property request was received before
                // transmitting any property values. When the first request
                // was made, all property values would be transmitted and then
                // cached. Up front serialization might in both cases include
                // transmitting all names. If property serialization were
                // deferred, then a new message branch would be added here
                // to retrieve the properties which could be obtained as
                // a PropertySourceImpl. If properties are also served up
                // then this inner class should be renamed "DataServer".

                // Close the various streams and the socket itself.
                try {
		    objectOut.flush();
		    socket.shutdownOutput();
		    socket.shutdownInput();
                    objectOut.close();
                    objectIn.close();
                    out.close();
                    in.close();
                    socket.close();
                } catch (IOException e) {
                    sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage10"),
                                            new ImagingException(JaiI18N.getString("SerializableRenderedImage10"), e));
//                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    // --- Begin implementation of java.awt.image.RenderedImage. ---

    public WritableRaster copyData(WritableRaster dest) {
        if (isServer || isSourceRemote) {
            return source.copyData(dest);
        }

        Rectangle region;
        if(dest == null) {
            region = imageBounds;
            SampleModel destSM =
                getSampleModel().createCompatibleSampleModel(region.width,
                                                             region.height);
            dest = Raster.createWritableRaster(destSM,
                                               new Point(region.x,
                                                         region.y));
        } else {
            region = dest.getBounds().intersection(imageBounds);
        }

        if(!region.isEmpty()) {
            int startTileX = PlanarImage.XToTileX(region.x,
                                                  tileGridXOffset,
                                                  tileWidth);
            int startTileY = PlanarImage.YToTileY(region.y,
                                                  tileGridYOffset,
                                                  tileHeight);
            int endTileX = PlanarImage.XToTileX(region.x + region.width - 1,
                                                tileGridXOffset,
                                                tileWidth);
            int endTileY = PlanarImage.YToTileY(region.y + region.height - 1,
                                                tileGridYOffset,
                                                tileHeight);

            SampleModel[] sampleModels = { getSampleModel() };
            int tagID =
                RasterAccessor.findCompatibleTag(sampleModels,
                                                 dest.getSampleModel());

            RasterFormatTag srcTag =
                new RasterFormatTag(getSampleModel(),tagID);
            RasterFormatTag dstTag =
                new RasterFormatTag(dest.getSampleModel(),tagID);

            for (int ty = startTileY; ty <= endTileY; ty++) {
                for (int tx = startTileX; tx <= endTileX; tx++) {
                    Raster tile = getTile(tx, ty);
                    Rectangle subRegion =
                        region.intersection(tile.getBounds());

                    RasterAccessor s =
                        new RasterAccessor(tile, subRegion,
                                           srcTag, getColorModel());
                    RasterAccessor d =
                        new RasterAccessor(dest, subRegion,
                                           dstTag, null);
                    ImageUtil.copyRaster(s, d);
                }
            }
        }

        return dest;
    }

    public ColorModel getColorModel() {
	return colorModel;
    }

    public Raster getData() {
        if (isServer || isSourceRemote) {
            return source.getData();
        }

        return getData(imageBounds);
    }

    public Raster getData(Rectangle rect) {
        Raster raster = null;

        // Branch according to whether the object is a data server or, if not,
        // according to whether it is a data client and using a deep copy of
        // the source data or pulling the data as needed over a socket.
        if (isServer || isSourceRemote) {
            raster = source.getData(rect);
        } else if (useDeepCopy) {
            raster = imageRaster.createChild(rect.x, rect.y,
                                             rect.width, rect.height,
                                             rect.x, rect.y,
                                             null);
        } else {
            // TODO: Use a Hashtable to store Rasters as they are pulled over
            // the network and look them up here using "rect" as key?

            // Connect to the data server.
            Socket socket = connectToServer();

            // Get the socket input and output streams and wrap object
            // input and output streams around them, respectively.
            OutputStream out = null;
            ObjectOutputStream objectOut = null;
            InputStream in = null;
            ObjectInputStream objectIn = null;
            try {
                out = socket.getOutputStream();
                objectOut = new ObjectOutputStream(out);
                in = socket.getInputStream();
                objectIn = new ObjectInputStream(in);
            } catch (IOException e) {
                sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage7"),
                                        new ImagingException(JaiI18N.getString("SerializableRenderedImage7"), e));
//                throw new RuntimeException(e.getMessage());
            }

            // Write the Rectangle to the object output stream.
            try {
                objectOut.writeObject(rect);
            } catch (IOException e) {
                sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage10"),
                                        new ImagingException(JaiI18N.getString("SerializableRenderedImage10"), e));
//                throw new RuntimeException(e.getMessage());
            }

            // Read serialized form of the Raster from object output stream.
            Object object = null;
            try {
                object = objectIn.readObject();
            } catch (IOException e) {
                sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage8"),
                                        new ImagingException(JaiI18N.getString("SerializableRenderedImage8"), e));
//                throw new RuntimeException(e.getMessage());
            } catch (ClassNotFoundException e) {
                sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage9"),
                                        new ImagingException(JaiI18N.getString("SerializableRenderedImage9"), e));
	    }

            if (useTileCodec) {
                byte[] buf = (byte[])object;
                raster = decodeRasterFromByteArray(buf);
            }
            else {
                if (!(object instanceof SerializableState))
                    raster = null;
                // Reconstruct the Raster from the serialized form.
                SerializableState ss = (SerializableState)object;
                Class c = ss.getObjectClass();
                if (Raster.class.isAssignableFrom(c)) {
                    raster = (Raster)ss.getObject();
                }
                else raster = null;
            }

	    // Close the various streams and the socket.
            try {
		objectOut.flush();
		socket.shutdownOutput();
		socket.shutdownInput();
                objectOut.close();
                out.close();
                objectIn.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                String message =
                    JaiI18N.getString("SerializableRenderedImage11");
                sendExceptionToListener(message,
                                        new ImagingException(message, e));
//                throw new RuntimeException(e.getMessage());
            }

            // If the rectangle equals the image bounds, cache the Raster,
            // switch to "deep copy" mode, and notify the data server.
            if (imageBounds.equals(rect)) {

                closeClient();

                imageRaster = raster;
                useDeepCopy = true;
            }
        }

        return raster;
    }

    public int getHeight() {
        return height;
    }

    public int getMinTileX() {
        return minTileX;
    }

    public int getMinTileY() {
        return minTileY;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getNumXTiles() {
        return numXTiles;
    }

    public int getNumYTiles() {
        return numYTiles;
    }

    // XXX Should getProperty() request property values over a socket
    // connection also?
    public Object getProperty(String name) {
        // XXX Use CaselessStringKeys for the property name.
        Object property = properties.get(name);
	return property == null ? Image.UndefinedProperty : property;
    }

    public String[] getPropertyNames() {
	String[] names = null;
        if (!properties.isEmpty()) {
            names = new String[properties.size()];
            Enumeration keys = properties.keys();
            int index = 0;
            while (keys.hasMoreElements()) {
                // XXX If CaselessStringKey keys are used then
                // getName() would have to be called here to get the
                // prop name from the key.
                names[index++] = (String)keys.nextElement();
            }
        }
        return names;
    }

    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * If this <code>SerializableRenderedImage</code> has not been
     * serialized, this method returns a <code>Vector</code> containing
     * only the <code>RenderedImage</code> passed to the constructor; if
     * this image has been deserialized, it returns <code>null</code>.
     */
    public Vector getSources() {
	return sources;
    }

    public Raster getTile(int tileX, int tileY) {
        if (isServer || isSourceRemote) {
            return source.getTile(tileX, tileY);
        }

	TileCache cache = JAI.getDefaultInstance().getTileCache();
	if (cache != null) {
	    Raster tile = cache.getTile(this, tileX, tileY);
	    if (tile != null)
		return tile;
	}

	// Determine the active area; tile intersects with image's bounds.
	Rectangle imageBounds = new Rectangle(getMinX(), getMinY(),
					      getWidth(), getHeight());
	Rectangle destRect =
	    imageBounds.intersection(new Rectangle(tileXToX(tileX),
						   tileYToY(tileY),
						   getTileWidth(),
						   getTileHeight()));

	Raster tile = getData(destRect);

	if (cache != null) {
	    cache.add(this, tileX, tileY, tile);
	}

	return tile;
    }

    /**
     * Returns a unique identifier (UID) for this <code>RenderedImage</code>.
     * This UID may be used when the potential redundancy of the value
     * returned by the <code>hashCode()</code> method is unacceptable.
     * An example of this is in generating a key for storing image tiles
     * in a cache.
     */
    public Object getImageID() {
        return UID;
    }

    /**
     * Converts a horizontal tile index into the X coordinate of its
     * upper left pixel.  No attempt is made to detect out-of-range
     * indices.
     *
     * <p> This method is implemented in terms of the <code>PlanarImage</code>
     * static method <code>tileXToX()</code> applied to the values returned
     * by primitive layout accessors.
     *
     * @param tx the horizontal index of a tile.
     * @return the X coordinate of the tile's upper left pixel.
     */
    private int tileXToX(int tx) {
	return PlanarImage.tileXToX(tx, getTileGridXOffset(), getTileWidth());
    }

    /**
     * Converts a vertical tile index into the Y coordinate of its
     * upper left pixel.  No attempt is made to detect out-of-range
     * indices.
     *
     * <p> This method is implemented in terms of the
     * <code>PlanarImage</code> static method <code>tileYToY()</code>
     * applied to the values returned by primitive layout accessors.
     *
     * @param ty the vertical index of a tile.
     * @return the Y coordinate of the tile's upper left pixel.
     */
    private int tileYToY(int ty) {
	return PlanarImage.tileYToY(ty, getTileGridYOffset(), getTileHeight());
    }

    public int getTileGridXOffset() {
	return tileGridXOffset;
    }

    public int getTileGridYOffset() {
	return tileGridYOffset;
    }

    public int getTileHeight() {
	return tileHeight;
    }

    public int getTileWidth() {
	return tileWidth;
    }

    public int getWidth() {
	return width;
    }

    // --- End implementation of java.awt.image.RenderedImage. ---

    /**
     * Create a server socket and start the server in a separate thread.
     *
     * <p> Note that this method should be called only the first time this
     * object is serialized and only if a deep copy is not being used. If
     * a deep copy is used there is no need to serve clients data on demand.
     * However if data service is being provided, there is no need to create
     * multiple threads for the single object as a single server thread
     * should be able to service multiple remote objects.
     */
    private synchronized void openServer()
        throws IOException, SocketException {
        if (!serverOpen) {
            // Create a ServerSocket.
            serverSocket = new ServerSocket(0);

            // Set the ServerSocket accept() method timeout period.
            serverSocket.setSoTimeout(SERVER_TIMEOUT);

            // Initialize the port field.
           port = serverSocket.getLocalPort();

            // Set the server availability flag.
            serverOpen = true;

            // Spawn a child thread and return the parent thread to the caller.
            serverThread = new Thread(new TileServer());
	    serverThread.setDaemon(true);
            serverThread.start();

            // Increment the remote reference count.
            incrementRemoteReferenceCount(this);
        }
    }

    /**
     * Transmit a message to the data server to indicate that the client
     * will no longer request socket connections.
     */
    private void closeClient() {

        // Connect to the data server.
        Socket socket = connectToServer();

        // Get the socket output stream and wrap an object
        // output stream around it.
        OutputStream out = null;
        ObjectOutputStream objectOut = null;
	ObjectInputStream objectIn = null;
        try {
            out = socket.getOutputStream();
            objectOut = new ObjectOutputStream(out);
	    objectIn = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage7"),
                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage7"), e));
//            throw new RuntimeException(e.getMessage());
        }

        // Write CLOSE_MESSAGE to the object output stream.
        try {
            objectOut.writeObject(CLOSE_MESSAGE);
        } catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage13"),
                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage13"), e));
//            throw new RuntimeException(e.getMessage());
        }

	try {
	    objectIn.readObject();
	} catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString(
					    "SerializableRenderedImage8"),
                                    new ImagingException(JaiI18N.getString(
					    "SerializableRenderedImage8"), e));
	} catch (ClassNotFoundException cnfe) {
            sendExceptionToListener(JaiI18N.getString(
					 "SerializableRenderedImage9"),
                                    new ImagingException(JaiI18N.getString(
					 "SerializableRenderedImage9"), cnfe));
	}

        // Close the streams and the socket.
        try {
	    objectOut.flush();
	    socket.shutdownOutput();
            objectOut.close();
            out.close();
	    objectIn.close();
            socket.close();
        } catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage11"),
                                    new ImagingException(JaiI18N.getString(
						  "SerializableRenderedImage11"), e));
//            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Obtain a connection to the data server socket. This is used only if a
     * deep copy of the image Raster has not been made.
     */
    private Socket connectToServer() {
        // Open a connection to the data server.
        Socket socket = null;
        try {
            socket = new Socket(host, port);
	    socket.setSoLinger(true,1);
        } catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage14"),
                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage14"), e));
//            throw new RuntimeException(e.getMessage());
        }

        return socket;
    }

    /**
     * When useTileCodec is set, encode the provided raster into
     * a byte array.
     */
    private byte[] encodeRasterToByteArray(Raster raster) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TileEncoder encoder =
            tileEncoderFactory.createEncoder(bos,
                                             encodingParam,
                                             raster.getSampleModel());
        try {
            encoder.encode(raster);
            return bos.toByteArray();
        } catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage15"),
                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage15"), e));
//            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    /**
     * When useTileCodec is set, decode the raster from a byte array.
     */
     private Raster decodeRasterFromByteArray(byte[] buf) {
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);

	// Fix 4640094 Tilecodec doesn't work well in SerializableRenderedImage
	// Currently, ParameterListDescriptor is singleton to a specific
	// tile codec and mode.  After deserialization this property is gone.
	// So need to copy the parameter values into the newly created object
	if (tileDecoderFactory == null) {
	    // Use the default operation registry as described in the spec
	    // of the constructor.
	    if (registry == null)
		registry = JAI.getDefaultInstance().getOperationRegistry();
	    tileDecoderFactory =
	                (TileDecoderFactory)registry.getFactory("tileDecoder", formatName);

	    TileCodecParameterList temp = decodingParam;

	    if (temp != null) {
		TileCodecDescriptor tcd =
		    getTileCodecDescriptor("tileDecoder", formatName);
		ParameterListDescriptor pld =
		    tcd.getParameterListDescriptor("tileDecoder");
		decodingParam =
		    new TileCodecParameterList(formatName,
					       new String[]{"tileDecoder"},
					       pld);
		String[] names = pld.getParamNames();

		if (names != null)
		    for (int i = 0; i < names.length; i++)
			decodingParam.setParameter(names[i],
						   temp.getObjectParameter(names[i]));

	    } else
		decodingParam = getTileCodecDescriptor("tileDecoder", formatName).
				getDefaultParameters("tileDecoder");
	}

        TileDecoder decoder =
            tileDecoderFactory.createDecoder(bis, decodingParam);
        try {
            return decoder.decode();
        } catch (IOException e) {
            sendExceptionToListener(JaiI18N.getString("SerializableRenderedImage16"),
                                    new ImagingException(JaiI18N.getString("SerializableRenderedImage16"), e));
//            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    /**
     * If a deep copy is not being used, unset the data server availability
     * flag and wait for the server thread to rejoin the current thread.
     */
    protected void finalize() throws Throwable {
        dispose();

        // Forward to the parent class.
        super.finalize();
    }

    /**
     * Provides a hint that an image will no longer be accessed from a
     * reference in user space.  The results are equivalent to those
     * that occur when the program loses its last reference to this
     * image, the garbage collector discovers this, and finalize is
     * called.  This can be used as a hint in situations where waiting
     * for garbage collection would be overly conservative, e.g., there
     * are a large number of socket connections which may be opened to
     * transmit tile data.
     *
     * <p> <code>SerializableRenderedImage</code> defines this method to
     * behave as follows:
     * <ul>
     * <li>if the image is acting as a server, i.e., has never been
     * serialized and may be providing data to serialized
     * versions of itself, it makes itself unavailable to further
     * client requests and closes its socket;</li>
     * <li>if the image is acting as a client, i.e., has been serialized
     * and may be requesting data from a remote, pre-serialization version
     * of itself, it sends a message to its remote self indicating that it
     * will no longer be making requests.</li>
     * </ul>
     *
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.
     */
    public void dispose() {
        // Rejoin the server thread if using a socket-based server.
        if (isServer) {
            if (serverOpen) {
                // Unset availability flag so server loop exits.
                serverOpen = false;

                // Wait for the server (child) thread to die.
                try {
                    serverThread.join(2*SERVER_TIMEOUT);
                } catch (Exception e) {
                    // Ignore the Exception.
                }

                // Close the server socket.
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    // Ignore the Exception.
                }
            }
        } else { // client
            // Transmit a message to the server to indicate the child's exit.
	    closeClient();
        }
    }

    /**
     * Custom serialization method. In addition to all non-transient fields,
     * the SampleModel, source vector, and properties table are serialized.
     * If a deep copy of the source image Raster is being used this is also
     * serialized.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        if (!useDeepCopy) {
            // Start the data server.
            try {
		openServer();
            } catch (Exception e1) {
                if (e1 instanceof SocketException) { // setSoTimeout() failed.
                    if (serverSocket != null) { // XXX Facultative
                        try {
                            serverSocket.close();
                        } catch (IOException e2) {
                            // Ignore the exception.
                        }
                    }
                }

                // Since server socket creation failed, use a deep copy.
                serverOpen = false; // XXX Facultative
                useDeepCopy = true;
            }
        }

        // Write non-static and non-transient fields.
        out.defaultWriteObject();

        // Write RMI properties of RemoteImage.
        if (isSourceRemote) {
            String remoteClass = source.getClass().getName();
            out.writeObject(source.getProperty(remoteClass+".serverName"));
            out.writeObject(source.getProperty(remoteClass+".id"));
        }

        // Remove non-serializable elements from table of properties.
        Hashtable propertyTable = properties;
        boolean propertiesCloned = false;
        Enumeration keys = propertyTable.keys();
        while(keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (!(properties.get(key) instanceof Serializable)) {
                if (!propertiesCloned) {
                    propertyTable = (Hashtable)properties.clone();
                    propertiesCloned = true;
                }
                propertyTable.remove(key);
            }
        }

        // Write the source vector and properties table.
        out.writeObject(SerializerFactory.getState(sampleModel, null));
        out.writeObject(SerializerFactory.getState(colorModel, null));
        out.writeObject(propertyTable);

        // Make a deep copy of the image raster.
        if (useDeepCopy) {
            if (useTileCodec)
                out.writeObject(encodeRasterToByteArray(source.getData()));
            else {
                out.writeObject(SerializerFactory.getState(source.getData(),
							   null));
            }
        }
    }

    /**
     * Custom deserialization method. In addition to all non-transient fields,
     * the SampleModel, source vector, and properties table are deserialized.
     * If a deep copy of the source image Raster is being used this is also
     * deserialized.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        isServer = false;
        source = null;
        serverOpen = false;
        serverSocket = null;
        serverThread = null;
        colorModel = null;

        // Read non-static and non-transient fields.
        in.defaultReadObject();

        if (isSourceRemote) {
            // Read RMI properties of RemoteImage.
            String serverName = (String)in.readObject();
            Long id = (Long)in.readObject();

            // Recreate remote source using the ID directly.
            source = new RemoteImage(serverName+"::"+id.longValue(),
                                     (RenderedImage)null);
        }

        // Read the source vector and properties table.
        SerializableState smState = (SerializableState)in.readObject();
        sampleModel = (SampleModel)smState.getObject();
        SerializableState cmState = (SerializableState)in.readObject();
        colorModel = (ColorModel)cmState.getObject();
        properties = (Hashtable)in.readObject();

        // Read the image Raster.
        if (useDeepCopy) {
            if (useTileCodec)
                imageRaster =
		    decodeRasterFromByteArray((byte[])in.readObject());
            else {
                SerializableState rasState =
		    (SerializableState)in.readObject();
                imageRaster = (Raster)rasState.getObject();
            }
        }
    }

    private TileCodecDescriptor getTileCodecDescriptor(String registryMode, String formatName) {
	if (registry == null)
	    return (TileCodecDescriptor)JAI.getDefaultInstance().
					    getOperationRegistry().
					    getDescriptor(registryMode, formatName);
	return (TileCodecDescriptor)registry.getDescriptor(registryMode, formatName);
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener= JAI.getDefaultInstance().getImagingListener();
        listener.errorOccurred(message, e, this, false);
    }
}
