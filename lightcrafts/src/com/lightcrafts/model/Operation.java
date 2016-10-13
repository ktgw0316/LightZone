/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import com.lightcrafts.ui.editor.EditorMode;

import java.awt.geom.Point2D;

/**
 * An Operation is the configurable unit of an Engine's processing pipeline.
 * Each Operation represents a transformation on Image data, and is
 * independently configurable.  Operations may be randomly inserted and
 * removed from the pipeline.  Each type of Operation gets its own control
 * panel in the user interface.
  */
public interface Operation {

    EditorMode getPreferredMode();

    /**
     * Get the OperationType corresponding to this Operation.
     * @return The OperationType of this Operation.
     */
    OperationType getType();

    /**
     * Operations may be individually enabled and disabled.
     * @param active True if this Operation should do its job, false if it
     * should be bypassed.
     */
    void setActivated(boolean active);

    /**
     * Query if the operation is active
     * @return whether it is active or not
     */
    boolean isActive();

    /**
     * Operations may be flagged to remain active, even when Engine.setActive()
     * is set to false.
     * @param active
     */
    void setEngineDeActivatable(boolean active);

    /**
     * Any Operation may be configured to have zero or more regions of
     * interest.  The default is zero regions, i.e. a global transformation.
     *
     * @param region A Region defining a region of interest, or null to
     *               indicate that there is no such region.
     */
    void setRegion(Region region);

    RGBColorSelection getColorSelection();

    /**
     * A ColorSelection can be specified explicitly as above, or  by a dropper
     * operation, specifying a point in the current image bounds.
     * @param p A point in the image to use for a color selection.
     * @return The ColorSelection implied by this choice.
     */
    RGBColorSelection getColorSelectionAt( Point2D p );

    /**
     * Operations can be configured to act only on selected colors.  The
     * default is to act on all colors, which you also get by calling this
     * method with null.
     * @param colors A color interval for this Operation, or null to make
     * the Operation act globally.
     */
    void setColorSelection(RGBColorSelection colors);

    /**
     * The Region of an Operation may be declared to be "inverted", meaning
     * that the Operation should apply to the image on the Region's exterior,
     * rather than its interior.  Default is to apply on the interior.
     * @param inverted True for exterior, false for interior.
     */
    void setRegionInverted(boolean inverted);

    /**
     * Set the LayerConfig of this Operation for blending with Operations
     * under it in the Engine.  If this method is never called, the
     * LayerConfig used will be the one returned by
     * <code>getDefaultLayerConfig()</code>.
     * @param layer A LayerConfig instance, compatible with
     * <code>Engine.getLayerModes()</code>.
     */
    void setLayerConfig(LayerConfig layer);

    /**
     * Get the default LayerConfig for this Operation.  The returned
     * LayerConfig will be consistent with the LayerModes returned by
     * <code>Engine.getLayerModes()</code>.
     * @return The LayerConfig used by this Operation before any calls to
     * <code>setLayerConfig()</code>.
     */
    LayerConfig getDefaultLayerConfig();

    /**
     * Users can notify implementations that they are making frequent
     * changes, like continuous updates during user interaction.  An
     * implementation may choose to optimize or skip updates in response to
     * changes during batches.
     */
    void changeBatchStarted();

    /**
     * Users can notify implementations that they are making frequent
     * changes, like continuous updates during user interaction.  An
     * implementation may choose to optimize or skip updates in response to
     * changes during batches.
     */
    void changeBatchEnded();

    /**
     * Some Operations can not be added, removed, disabled, reordered, or
     * blended.  Such Operations return true here.
     */
    boolean isSingleton();

    /**
     * Some Operations can not be added, removed, disabled, reordered, or
     * blended.  Such Operations return true here.
     * @return if it takes a footer or not
     */
    boolean hasFooter();

    void dispose();
}
