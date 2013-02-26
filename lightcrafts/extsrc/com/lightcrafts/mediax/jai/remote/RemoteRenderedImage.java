/*
 * $RCSfile: RemoteRenderedImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:53 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.remote.NegotiableCapabilitySet;

/**
 * <code>RemoteRenderedImage</code> is an interface commonly used to 
 * represent objects which contain or can produce image data in the form of
 * <code>Raster</code>s from locations that are remote. The image data may be
 * stored/produced as a single tile or a regular array of tiles.
 *
 * <p>This class is the remote equivalent of the <code>RenderedImage</code>
 * interface and adds methods that deal with the remote location aspect of
 * the image.
 *
 * @see RenderedImage
 *
 * @since JAI 1.1
 */
public interface RemoteRenderedImage extends RenderedImage {

    /**
     * Returns the <code>String</code> that identifies the server.
     */
    String getServerName();

    /**
     * Returns the <code>String</code> that identifies the remote imaging
     * protocol.
     */
    String getProtocolName();

    /**
     * Returns the amount of time between retries in milliseconds.
     */
    int getRetryInterval();

    /**
     * Sets the amount of time between retries in milliseconds.
     *
     * @param retryInterval The amount of time (in milliseconds) to wait 
     *                      between retries. 
     * @throws IllegalArgumentException if retryInterval is negative.
     */
    void setRetryInterval(int retryInterval);

    /**
     * Returns the number of retries.
     */
    int getNumRetries();

    /**
     * Sets the number of retries.
     *
     * @param numRetries The number of times an operation should be retried
     *                   in case of a network error. 
     * @throws IllegalArgumentException if numRetries is negative.
     */
    void setNumRetries(int numRetries);

    /**
     * Returns the current negotiation preferences or null, if none were
     * set previously.
     */
    NegotiableCapabilitySet getNegotiationPreferences();

    /**
     * Sets the preferences to be used in the client-server
     * communication. These preferences are utilized in the negotiation 
     * process. Note that preferences for more than one category can be
     * specified using this method. Also each preference can be a list
     * of values in decreasing order of preference, each value specified
     * as a <code>NegotiableCapability</code>. The 
     * <code>NegotiableCapability</code> first (for a particular category)
     * in this list is given highest priority in the negotiation process
     * (for that category).
     *
     * <p> It may be noted that this method allows for multiple negotiation
     * cycles. Everytime this method is called, new preferences are
     * specified for the negotiation, which can be utilized to perform
     * a new round of negotiation to produce new negotiated values to be
     * used in the remote communication.
     *
     * @param preferences The preferences to be used in the negotiation
     * process.
     * @throws IllegalArgumentException if preferences is null.
     */
    void setNegotiationPreferences(NegotiableCapabilitySet preferences);

    /**
     * Returns the results of the negotiation process. This will return null
     * if no negotiation preferences were set, and no negotiation was
     * performed, or if the negotiation failed.
     */
    NegotiableCapabilitySet getNegotiatedValues() 
	throws RemoteImagingException;

    /**
     * Returns the results of the negotiation process for the given category.
     * This will return null if no negotiation preferences were set, and no
     * negotiation was performed, or if the negotiation failed.
     *
     * @param String category The category to get the negotiated results for.
     * @throws IllegalArgumentException if category is null.
     */
    NegotiableCapability getNegotiatedValue(String category)
	throws RemoteImagingException;

    /**
     * Informs the server of the negotiated values that are the result of
     * a successful negotiation.
     *
     * @param negotiatedValues    The result of the negotiation.
     */
    void setServerNegotiatedValues(NegotiableCapabilitySet negotiatedValues) 
	throws RemoteImagingException;
}
