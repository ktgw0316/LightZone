/*
 * $RCSfile: SerializableRenderableImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:54 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.rmi;

/*
XXX
See if the SerializableRenderedImage can be sent by requests instead of 
deep copy. 
*/

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
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
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderFactory;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A serializable wrapper class for classes which implement the
 * <code>RenderableImage</code> interface.
 *
 * <p> A <code>SerializableRenderableImage</code> provides a means to
 * serialize a <code>RenderableImage</code>.  Transient fields are handled
 * using <code>Serializer</code>s registered with the
 * <code>SerializerFactory</code>. Since no data is associated with a
 * <code>RenderableImage</code>, <code>SerializableRenderableImage</code>
 * does not provide any renderable image data. The only way to access image
 * data from a <code>SerializableRenderableImage</code> is by calling any
 * one of the <code>createDefaultRendering</code>, 
 * <code>createRendering</code>, or <code>createScaledRendering</code>
 * methods. The resultant <code>RenderedImage</code> is created on the remote
 * host and provided via deep copy of the image data. If the request is
 * made on the local host, the image data are provided by forwarding
 * the request to the wrapped <code>RenderableImage</code>.  Note that a single
 * <code>SerializableRenderableImage</code> object should be able to service
 * multiple remote hosts.
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
 *     protected transient RenderableImage image;
 *
 *     // Fields omitted.
 *
 *     public SomeSerializableClass(RenderableImage image) {
 *         this.image = image;
 *     }
 *
 *     // Methods omitted.
 *
 *     // Serialization method.
 *     private void writeObject(ObjectOutputStream out) throws IOException {
 *         out.defaultWriteObject();
 *         out.writeObject(new SerializableRenderableImage(image));
 *     }
 *
 *     // Deserialization method.
 *     private void readObject(ObjectInputStream in)
 *         throws IOException, ClassNotFoundException {
 *         in.defaultReadObject();
 *         image = (RenderableImage)in.readObject();
 *     }
 * }
 * </pre>
 *
 * @see java.awt.image.renderable.RenderableImage
 * @see com.lightcrafts.mediax.jai.RenderableOp
 */
public final class SerializableRenderableImage implements RenderableImage,
    Serializable {
    
    /** Value to indicate the server socket timeout period (milliseconds). */
    private static final int SERVER_TIMEOUT = 60000; // XXX 1 minute?

    /** Message indicating that a client will not connect again. */
    private static final String CLOSE_MESSAGE = "CLOSE";

    /** Flag indicating whether this is a data server. */
    private transient boolean isServer;

    /** The RenderableImage source of this object (server only). */
    private transient RenderableImage source;

    /** The X coordinate of the image's upper-left pixel. */
    private float minX;

    /** The Y coordinate of the image's upper-left pixel. */
    private float minY;

    /** The image's width in pixels. */
    private float width;

    /** The image's height in pixels. */
    private float height;

    /** The image's sources, stored in a Vector. */
    private transient Vector sources = null;

    /** A Hashtable containing the image properties. */
    private transient Hashtable properties = null;

    /** */
    private boolean isDynamic;

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

    /**
     * A table of counts of remote references to instances of this class
     * (server only).
     *
     * <p> This table consists of entries with the keys being instances of
     * <code>SerializableRenderableImage</code> and the values being
     * <code>Integer</code>s the int value of which represents the number
     * of remote <code>SerializableRenderableImage</code> objects which could
     * potentially request a socket connection with the associated key. This
     * table is necessary to prevent the garbage collector of the interpreter
     * in which the server <code>SerializableRenderableImage</code> object is
     * instantiated from finalizing the object - and thereby closing its
     * server socket - when that object could still receive socket connection
     * requests from its remote clients. The reference to the object in the
     * static class variable ensures that the object will not be prematurely
     * finalized.
     */
    private static transient Hashtable remoteReferenceCount;

    /** Indicate that tilecodec is used in the transfering or not */
    private boolean useTileCodec = false;

    /** 
     * The <code>OperationRegistry</code> to be used to find the 
     * <code>TileEncoderFactory</code> and <code>TileDecoderFactory</code>
     */
    private OperationRegistry registry = null;

    /** The name of the TileCodec format. */
    private String formatName = null;

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
    SerializableRenderableImage() {}

    /**
     * Constructs a <code>SerializableRenderableImage</code> wrapper for a
     * <code>RenderableImage</code> source.  The image data of the rendering
     * will be serialized via a single deep copy.  Tile encoding and
     * decoding may be effected via a <code>TileEncoder</code> and
     * <code>TileDecoder</code> specified by format name.
     *
     * @param source The <code>RenderableImage</code> source.
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
     *            or <code>formatName</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>encodingParam</code> and
     * <code>decodingParam</code> do not have the same format name as the
     * supplied <code>formatName</code>. 
     */
    public SerializableRenderableImage(RenderableImage source,
				       OperationRegistry registry,
				       String formatName,
				       TileCodecParameterList encodingParam,
				       TileCodecParameterList decodingParam) {

        this(source);
	
	this.registry = registry;
	this.formatName = formatName;
	this.encodingParam = encodingParam;
	this.decodingParam = decodingParam;

	if (formatName == null) {
	    throw new IllegalArgumentException(
			   JaiI18N.getString("SerializableRenderableImage2"));
	}

        if (!formatName.equals(encodingParam.getFormatName())) {
            throw new IllegalArgumentException(
					  JaiI18N.getString("UseTileCodec0"));
        }

        if (!formatName.equals(decodingParam.getFormatName())) {
            throw new IllegalArgumentException(
					  JaiI18N.getString("UseTileCodec1"));
        }

        TileEncoderFactory tileEncoderFactory =
            (TileEncoderFactory)registry.getFactory("tileEncoder", formatName);
        TileDecoderFactory tileDecoderFactory =
            (TileDecoderFactory)registry.getFactory("tileDecoder", formatName);
        if (tileEncoderFactory == null || tileDecoderFactory == null)
            throw new RuntimeException(JaiI18N.getString("UseTileCodec2"));

        useTileCodec = true;
    }

    /**
     * Constructs a <code>SerializableRenderableImage</code> wrapper for a
     * <code>RenderableImage</code> source.  Image data of the rendering of
     * the <code>RenderableImage</code> will be serialized via a single deep
     * copy.  No <code>TileCodec</code> will be used, i.e., data will be
     * transmitted using the serialization protocol for <code>Raster</code>s.
     *
     * @param source The <code>RenderableImage</code> source.
     * @exception IllegalArgumentException if <code>source</code>
     *            or <code>formatName</code> is <code>null</code>.
     */
    public SerializableRenderableImage(RenderableImage source) {

	if (source == null) 
	    throw new IllegalArgumentException(
			    JaiI18N.getString("SerializableRenderableImage1"));

        // Set server flag.
        isServer = true;

        // Cache the parameter.
        this.source = source;

        // Initialize RenderableImage fields.
        minX = source.getMinX();
        minY = source.getMinY();
        width = source.getWidth();
        height = source.getHeight();
	isDynamic = source.isDynamic();

        sources = new Vector();
        sources.add(source);

        properties = new Hashtable();
        String[] propertyNames = source.getPropertyNames();
	String propertyName;
        if (propertyNames != null) {
            for (int i = 0; i < propertyNames.length; i++) {
		propertyName = propertyNames[i];
                properties.put(new CaselessStringKey(propertyName),
                               source.getProperty(propertyName));
            }
        }

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
    private class RenderingServer implements Runnable {

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
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
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
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }

                // Read the Object from the object stream.
                Object obj = null;
                try {
                    obj = objectIn.readObject();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }

		RenderedImage ri = null;
		SerializableRenderedImage sri;
                // Switch according to object class; ignore unsupported types.
                if (obj instanceof String) {
		    String str = (String)obj;

                    if (str.equals(CLOSE_MESSAGE)) {
			// Decrement the remote reference count.
			decrementRemoteReferenceCount(this);
			
		    } else {
			if (str.equals("createDefaultRendering")) {
			    
			    ri = source.createDefaultRendering();
			    
			} else if (str.equals("createRendering")) {
			    
			    // Read the Object from the object stream.
			    obj = null;
			    try {
				obj = objectIn.readObject();
			    } catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			    }
			    
			    SerializableState ss = (SerializableState)obj;
			    RenderContext rc = (RenderContext)ss.getObject();
			    
			    ri = source.createRendering(rc);

			} else if (str.equals("createScaledRendering")) {
			    
			    // Read the Object from the object stream.
			    obj = null;
			    try {
				obj = objectIn.readObject();
			    } catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			    }
			    
			    int w = ((Integer)obj).intValue();
			    
			    try {
				obj = objectIn.readObject();
			    } catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			    }
			    
			    int h = ((Integer)obj).intValue();
			    
			    try {
				obj = objectIn.readObject();
			    } catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			    }
			    
			    SerializableState ss = (SerializableState)obj;
			    RenderingHints rh = (RenderingHints)ss.getObject();
			    
			    ri = source.createScaledRendering(w, h, rh);
			}

			if (useTileCodec) {
			    try {
				sri = new SerializableRenderedImage(ri, 
								    true, 
								    registry,
								    formatName, 
								    encodingParam,
								    decodingParam);
			    } catch (java.io.NotSerializableException nse) {
				throw new RuntimeException(nse.getMessage());
			    }
			} else {
			    sri = new SerializableRenderedImage(ri, true);
			}
			
			try {
			    objectOut.writeObject(sri);
			} catch (Exception e) {
			    throw new RuntimeException(e.getMessage());
			}
		    }
                } else {
		    throw new RuntimeException(
			    JaiI18N.getString("SerializableRenderableImage0"));
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
                    objectOut.close();
                    objectIn.close();
                    out.close();
                    in.close();
                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    // --- Begin implementation of java.awt.image.RenderableImage. ---

    /**
     * Returns a <code>RenderedImage</code> which is the result of
     * calling <code>createDefaultRendering</code> on the wrapped
     * <code>RenderableImage</code>. 
     */
    public RenderedImage createDefaultRendering() {
	
	if (isServer) {
	    return source.createDefaultRendering();
	}

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
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}

	// Write the name of the method to the object output stream.
	try {
	    objectOut.writeObject("createDefaultRendering");
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}
	
	// Read serialized form of the RenderedImage from object output stream.
	Object object = null;
	try {
	    object = objectIn.readObject();
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}    

	RenderedImage ri;
	if (object instanceof SerializableRenderedImage) {
	    ri = (RenderedImage)object;
	} else {
	    ri = null;
	}

	// Close the various streams and the socket.
	try {
	    out.close();
	    objectOut.close();
	    in.close();
	    objectIn.close();
	    socket.close();
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}

	return ri;
    }


    public RenderedImage createRendering(RenderContext renderContext) {
	
	if (isServer) {
	    return source.createRendering(renderContext);
	}

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
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}

	// Write the name of the method and the RenderContext to the
	// object output stream.
	try {
	    objectOut.writeObject("createRendering");
	    objectOut.writeObject(SerializerFactory.getState(renderContext, 
							     null));
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}
	
	// Read serialized form of the RenderedImage from object output stream.
	Object object = null;
	try {
	    object = objectIn.readObject();
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}    

	RenderedImage ri = (RenderedImage)object;

	// Close the various streams and the socket.
	try {
	    out.close();
	    objectOut.close();
	    in.close();
	    objectIn.close();
	    socket.close();
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}

	return ri;
    }


    public RenderedImage createScaledRendering(int w, int h, 
					       RenderingHints hints) {
	
	if (isServer) {
	    return source.createScaledRendering(w, h, hints);
	}

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
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}

	// Write the name of the method and the necessary method argument
	// to the object output stream.
	try {
	    objectOut.writeObject("createScaledRendering");
	    objectOut.writeObject(new Integer(w));
	    objectOut.writeObject(new Integer(h));
	    objectOut.writeObject(SerializerFactory.getState(hints, null));
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}
	
	// Read serialized form of the RenderedImage from object output stream.
	Object object = null;
	try {
	    object = objectIn.readObject();
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}    

	RenderedImage ri = (RenderedImage)object;

	// Close the various streams and the socket.
	try {
	    out.close();
	    objectOut.close();
	    in.close();
	    objectIn.close();
	    socket.close();
	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage());
	}

	return ri;
    }

    public float getHeight() {
        return height;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    // XXX Should getProperty() request property values over a socket
    // connection also?
    public Object getProperty(String name) {
        Object property = properties.get(new CaselessStringKey(name));
	return property == null ? Image.UndefinedProperty : property;
    }

    public String[] getPropertyNames() {
	String[] names = null;
        if (!properties.isEmpty()) {
            names = new String[properties.size()];
            Enumeration keys = properties.keys();
            int index = 0;
	    CaselessStringKey key;
            while (keys.hasMoreElements()) {
		key = (CaselessStringKey)keys.nextElement();
                names[index++] = key.getName();
            }
        }
        return names;
    }

    /**
     * If this <code>SerializableRenderableImage</code> has not been
     * serialized, this method returns a <code>Vector</code> containing
     * only the <code>RenderableImage</code> passed to the constructor; if
     * this image has been deserialized, it returns <code>null</code>.
     */
    public Vector getSources() {
	return sources;
    }

    /**
     *
     */
    public boolean isDynamic() {
	return isDynamic;
    }

    public float getWidth() {
	return width;
    }

    // --- End implementation of java.awt.image.RenderableImage. ---

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
            serverThread = new Thread(new RenderingServer());
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
        try {
            out = socket.getOutputStream();
            objectOut = new ObjectOutputStream(out);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // Write CLOSE_MESSAGE to the object output stream.
        try {
            objectOut.writeObject(CLOSE_MESSAGE);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // Close the streams and the socket.
        try {
            out.close();
            objectOut.close();
            socket.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
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
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return socket;
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
     * <p> <code>SerializableRenderableImage</code> defines this method to
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
	}

        // Write non-static and non-transient fields.
        out.defaultWriteObject();

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

        // Write the properties table.
        out.writeObject(propertyTable);
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

        // Read non-static and non-transient fields.
        in.defaultReadObject();

        // Read the properties table.
        properties = (Hashtable)in.readObject();
    }
}
