/*
 * $RCSfile: BandMergeCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:15 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.util.Vector;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.ImageLayout;

/**
 * A <code>CRIF</code> supporting the "BandMerge" operation in the
 * rendered and renderable image layers.
 *
 * @see com.lightcrafts.mediax.jai.operator.BandMergeDescriptor
 * @see BandMergeOpImage
 *
 */
public class BandMergeCRIF extends CRIFImpl {

     /** Constructor. */
    public BandMergeCRIF() {
        super("bandmerge");
    }

    /**
     * Creates a new instance of <code>BandMergeOpImage</code> in the
     * rendered layer. This method satisifies the implementation of RIF.
     *
     * @param paramBlock   The two or more source images to be "BandMerged"
     * together, and their corresponding float array vector. 
     * @param renderHints  Optionally contains destination image layout.     
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        
    
	// get vector of RenderedImage sources and parameters
	Vector sources = paramBlock.getSources();
	//Vector params  = paramBlock.getParameters();

	return new BandMergeOpImage(sources,
				    renderHints,
				    layout);

    }
}
