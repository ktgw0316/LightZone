/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.model;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.model.ImageEditor.LensCorrectionsOperation;
import com.lightcrafts.utils.thread.ProgressThread;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * An Engine is the basic image interpretation and manipulation object.
 * Each Engine instance maintains a single Image.  The Engine's behavior is
 * defined by a pipeline of Operations that successively transform the Image.
 * <p>
 * An Engine must support certain specialized Operations, like ZoneOperation
 * and CloneOperationImpl, but it may also provide any number of other Operations
 * implemented as GenericOperations.  The GenericOperations supported by an
 * Engine can be discovered through <code>getOperationTypes()</code>.
 * <p>
 * @see com.lightcrafts.model.EngineFactory
 */

public interface Engine {

    /**
     * Get the natural dimensions of the image, without any scaling.
     * @return The width and height of the current image, if it were
     * rendered 1-to-1.
     */
    Dimension getNaturalSize();

    /**
     * Get a RenderedImage copy of this Engine's Image, scaled to fit
     * within the given bounds.
     */
    RenderedImage getRendering(Dimension bounds);

    /**
     * Each Engine has an Image, and can paint it into a Component.
     * @return A Component which renders this Engine's Image.
     */
    Component getComponent();

    /**
     * Engines may provide Previews, which are alternative rendering of
     * their Image data.  Currently, this includes the ZoneFinder, a
     * histogram, and a text readout of color data under the cursor.
     * @return A List of Previews.
     */
    List<Preview> getPreviews();

    /**
     * Get a Collection of the OperationTypes of all GenericOperations
     * supported by this Engine.  These OperationTypes are suitable for use
     * in calls to <code>insertOperation()</code>.
     * @return A Collection of distinct OperationTypes, defining the set of
     * acceptable arguments for <code>insertOperation()</code>.
     */
    Collection<OperationType> getGenericOperationTypes();

    /**
     * Add any Operation to the current pipeline at the given position.
     * @param position A number from zero up to the current pipeline size.
     * @param type An OperationType specifying which supported Operation
     * should be inserted.
     * @return A GenericOperation instance that can control the inserted
     * transformation, or null if the given OperationType is invalid.
     */
    Operation insertOperation(OperationType type, int position);

    /**
     * Add a ZoneOperation to the current pipeline at the given position.
     * @param position A number from zero up to the current pipeline size.
     * @return A ZoneOperation instance that can control the inserted
     * transformation.
     */
    ZoneOperation insertZoneOperation(int position);

    /**
     * Add a CloneOperation to the current pipeline at the given position.
     * @param position A number from zero up to the current pipeline size.
     * @return A CloneOperation instance that can control the inserted
     * transformation.
     */
    CloneOperation insertCloneOperation(int position);

    /**
     * Add a SpotOperation to the current pipeline at the given position.
     * @param position A number from zero up to the current pipeline size.
     * @return A CloneOperation instance that can control the inserted
     * transformation.
     */
    SpotOperation insertSpotOperation(int position);

    /**
     * Add a WhitePointOperation to the current pipeline at the give position.
     * @param position A number from zero up to the current pipeline size.
     * @return A WhitePointOperation instance that can control the inserted
     * transformation.
     */
    WhitePointOperation insertWhitePointOperation(int position);

    /**
     * Add a LensCorrectionsOperation to the current pipeline at the bottom.
     * @return A LensCorrectionsOperation instance that can control the inserted
     * transformation.
     */
    LensCorrectionsOperation insertLensCorrectionsOperation(int position);

    /**
     * Get the special OperationType of the "RAW Adjustments" singleton
     * Operation that controls RAW conversion settings.
     */
    OperationType getRawAdjustmentsOperationType();

    // Account for Raw Adjustments versioning

    OperationType getGenericRawAdjustmentsOperationType();

    /**
     * Remove the Operation at the given position in the pipeline, or do
     * nothing if the given position number is out of range.
     * @param position A number from zero up to the current pipeline size
     * minus one.
     */
    void removeOperation(int position);

    /**
     * Get a List of LayerMode references that define the set of legal
     * arguments for the LayerConfig constructor.
     * @return A List of LayerModes.
     */
    List<LayerMode> getLayerModes();

    /**
     * Interchange the Operation at the given position with its neighbor
     * Operation at position plus one.  If the given position number is out
     * of bounds (less than zero, or greater than the pipeline size minus
     * two), then do nothing.
     * @param position The lower of two position indices to interchange in
     * the pipeline.
     */
    void swap(int position);

    /**
     * tell the engine that this operation is now selected by the user
     * @param position the operation index
     * @param selected selected or not
     */
    void setSelectedOperation(int position, boolean selected);

    /**
     * Find out an ordered collection of Scales supported by this Engine.
     * These are not the only Scales this Engine can render, but they are
     * values it prefers for some reason.
     * @return A List of Scale objects.
     */
    List<Scale> getPreferredScales();

    Scale getScale();

    /**
     * Force the Engine to adopt a given Scale.
     * @param scale A Scale defining a new size for this Engine's image.
     */
    void setScale(Scale scale);

    /**
     * Allow the Engine to select a Scale based on a given Rectangle where
     * its Component will be rendered.  The resulting Scale is likely to
     * cause the underlying Image to fit into the Rectangle's bounds.
     * @param rect A Rectangle where the Engine's Component should fit.
     * @return A Scale appropriate to the given Rectangle.
     */
    Scale setScale(Rectangle rect);

    /**
     * Switch all Engine Operations on and off.  When they're off, the
     * output image is the same as the input image except for the Scale.
     * All state is maintained during the off state, so that the when the
     * Operations come on again the image becomes consistent with the
     * Operations and the Scale at that time.
     * @param active True for normal operation, false to temporarily show
     * the unmodified image.
     */
    void setActive(boolean active);

    /**
     * Crop the image to the given CropBounds.  Like Regions, the CropBounds
     * argument is interpreted in the image coordinates, without any prior
     * crop, rotation, or scaling.  The new CropBounds specifies a rotation
     * angle and vertices for the new image.
     * @param bounds A crop for the image.
     */
    void setCropBounds(CropBounds bounds);

    /**
     * An image may have an AffineTransform if it has been cropped or rotated.
     * This transform maps the original image coordinates to screen
     * coordinates, in the absence of scaling.
     * @return An AffineTransform which, if used in Graphics2D.setTransform(),
     * would render drawing in the original (uncropped, unrotated) coordinates
     * correctly on the current (cropped, rotated) image.
     */
    AffineTransform getTransform();

    /**
     * Add a listener to learn about Engine activity changes.
     * @param listener An EngineListener to receive events.
     */
    void addEngineListener(EngineListener listener);

    /**
     * Remove a listener that was added through
     * <code>addEngineListener()</code>.
     * @param listener An EngineListener previously added.
     */
    void removeEngineListener(EngineListener listener);

    /**
     * Write the current image to a File, using the settings in an
     * {@link ImageExportOptions} object.
     * @param thread The thread that is doing the writing.
     * @param options Complete specification for the export, including the
     * File.
     * @throws IOException If anything goes wrong whatsoever.
     */
    void write(ProgressThread thread, ImageExportOptions options)
        throws IOException;

    /**
     * Print the current image.
     * @param thread A ProgressIndicator to provide user feedback during
     * rendering.
     * @param format The orientation, paper, and margin info for PrinterJob.
     * @param settings Layout information for the image within the paper's
     * imageable area.
     */
    void print(
        ProgressThread thread,
        PageFormat format,
        PrintSettings settings
    ) throws PrinterException;

    /**
     * Update the Component display to suggest what printed output would
     * look like if the given PrintSettings were applied.
     * @param settings A color profile and a rendering intent to apply to
     * the displayed image, or null to indicate that defaults should be
     * applied.
     */
    void preview(PrintSettings settings);

    /**
     * Interrupt <code>print()</code>.  This just calls PrinterJob.cancel().
     */
    void cancelPrint();

    /**
     * Engines can provide a List of undifferentiated JMenuItems for
     * development and testing.
     * @return A List of JMenuItems.
     */
    List<JMenuItem> getDebugItems();

    /**
     * Clean up whatever resources this Engine is holding.  Call this only
     * after this Engine's Component has been removed from any display.
     */
    void dispose();
}
