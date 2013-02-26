/*
 * $RCSfile: LookupOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:31 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ColormapOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "Lookup" operation.
 *
 * <p>This <code>OpImage</code> performs the general table lookup on
 * a source image by passing it through a lookup table. The source
 * image may be single- or multi-banded and of any integral data types.
 * The lookup table may be single- or multi-banded of any JAI supported
 * data types. The destination image must have the same data type as
 * the lookup table, and its number of bands is determined based on
 * the number of bands of the source and the table.
 *
 * <p>If both the source and the lookup table are multi-banded, they
 * should have the same number of bands. In case their band numbers
 * are different, if the source's number of bands is less than the
 * table's number of bands, the first <code>n</code> bands of the table
 * data is used, where <code>n</code> is the source's number of bands;
 * otherwise, the first band (band 0) of the table data is applied to
 * all the bands of the source.
 *
 * <p>The application programs may specify the layout of the
 * destination image via the <code>ImageLayout</code> parameter. If
 * a <code>SampleModel</code> is supplied, it should be created in
 * accordance with the source image and the actual lookup table
 * used in a specific case. It is strongly recommended that a
 * <code>ComponentSampleModel</code> is be used whenever possible
 * for better performance.
 *
 * <p>If the supplied <code>SampleModel</code> is unsuitable for
 * the source image and the lookup table type, or if no
 * <code>SampleModel</code> is specified, a default suitable
 * <code>SampleModel</code> is chosen for this operation based on
 * the type of source image and lookup table. In this case, a new
 * <code>ColorModel</code> is chosen based on the <code>SampleModel</code>.
 *
 * <p>Special case lookup operators should extend this class.
 *
 * <p>The destination pixel values are determined as:
 * <pre>
 *     if (srcNumBands == 1) {
 *         // dst[y][x] has the same number of bands as the lookup table.
 *         for (b = 0; b < dstNumBands; b++) {
 *             dst[y][x][b] = tableData[b][src[y][x][0] + offsets[b]];
 *         }
 *     } else {
 *         // src[y][x] is multi-banded, dst[y][x] has the same
 *         // number of bands as src[y][x].
 *         if (tableNumBands == 1) {
 *             for (b = 0; b < dstNumBands; b++) {
 *                 dst[y][x][b] = tableData[0][src[y][x][b] + offsets[0]];
 *             }
 *         } else {
 *             for (b = 0; b < dstNumBands; b++) {
 *                 dst[y][x][b] = tableData[b][src[y][x][b] + offsets[b]];
 *             }
 *         }
 *     }
 * </pre>
 *
 * @see com.lightcrafts.mediax.jai.operator.LookupDescriptor
 * @see com.lightcrafts.mediax.jai.LookupTableJAI
 * @see LookupCRIF
 *
 */
final class LookupOpImage extends ColormapOpImage {

    /**
     * The lookup table associated with this operation.
     * The source image is passed through this table.
     */
    protected LookupTableJAI table;

    /**
     * Constructor.
     *
     * <p>If a <code>SampleModel</code> is supplied in the <code>layout</code>
     * parameter, it may be ignored in case it's unsuitable for this
     * operation. In this case the default <code>SampleModel</code>
     * and <code>ColorModel</code> are used.
     *
     * @param source  The source image.
     * @param layout  The destination image layout.
     * @param table   The table used to perform the lookup operation,
     *                stored by reference.
     */
    public LookupOpImage(RenderedImage source,
                         Map config,
                         ImageLayout layout,
                         LookupTableJAI table) {
        super(source, layout, config, true);

        this.table = table;

        SampleModel sm = source.getSampleModel();	// source sample model

        if (sampleModel.getTransferType() != table.getDataType() ||
            sampleModel.getNumBands() !=
                table.getDestNumBands(sm.getNumBands())) {
            /*
             * The current SampleModel is not suitable for the supplied
             * source and lookup table. Create a suitable SampleModel
             * and ColorModel for the destination image.
             */
            sampleModel = table.getDestSampleModel(sm, tileWidth, tileHeight);
            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    /**
     * Transform the colormap via the lookup table.
     */
    protected void transformColormap(byte[][] colormap) {
        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            int band = table.getNumBands() < 3 ? 0 : b;

            for(int i = 0; i < mapSize; i++) {
                int result = table.lookup(band, map[i] & 0xFF);
                map[i] = ImageUtil.clampByte(result);
            }
        }
    }

    /**
     * Performs the table lookup operation within a specified rectangle.
     *
     * @param sources   Cobbled source, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        table.lookup(sources[0], dest, destRect);
    }
}
