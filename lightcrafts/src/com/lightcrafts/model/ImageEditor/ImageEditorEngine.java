/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.image.export.BitsPerChannelOption;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.AuxiliaryImageInfo;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.RawImageInfo;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.LCTileCache;
import com.lightcrafts.model.*;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;

import javax.media.jai.*;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ImageEditorEngine implements Engine {
    private ImageInfo m_imageInfo;
    private ImageInfo m_exportInfo;

    @Getter(AccessLevel.PACKAGE)
    private PlanarImage sourceImage;

    private PlanarImage processedImage;

    @Getter
    private Rendering rendering;

    private ImageEditorDisplay canvas = null;

    private boolean engineActive = true;

    @Getter
    private List<Preview> previews = new LinkedList<Preview>();

    private static final List<Scale> preferredScales = Arrays.asList(
            new Scale(1, 32),
            new Scale(1, 16),
            new Scale(1, 8),
            new Scale(1, 4),
            new Scale(1, 2),
            new Scale(1, 1),
            new Scale(2, 1),
            new Scale(4, 1)
    );

    private LinkedList<EngineListener> listeners = null;

    @Getter
    private ImageMetadata metadata = null;

    @Getter
    private AuxiliaryImageInfo auxInfo = null;

    private RenderedImage backgroundImage;

    private boolean addFirstPaintLatency;

    @Getter
    private Scale scale;

    @Override
    public AffineTransform getTransform() {
        return rendering.getTransform();
    }

    CropBounds getCropBounds() {
        return rendering.getCropBounds();
    }

    @Override
    public Dimension getNaturalSize() {
        return rendering.getRenderingSize();
    }

    @Override
    public synchronized Component getComponent() {
        if (canvas == null) {
            listeners = new LinkedList<EngineListener>();

            canvas = new ImageEditorDisplay(this, null);
            canvas.setEngineListeners(listeners);
            canvas.setPaintListener(new CanvasPaintListener());

            if (addFirstPaintLatency) {
                canvas.setFirstTime();
            }
            if (backgroundImage != null) {
                canvas.setBackgroundImage(backgroundImage);
            }
            // Always have these Operations, to return from getPreviews().
            previews.add(new PassThroughPreview(this));
            previews.add(new ZoneFinder(this));
            previews.add(new ColorSelectionPreview(this));
            // previews.add(new ZoneFinder(this, true)); // Color Zone Finder
            previews.add(new HistogramPreview(this));
            previews.add(new DropperPreview(this));
        }
        return canvas;
    }

    @Override
    public List<LayerMode> getLayerModes() {
        return BlendedOperation.blendingModes;
    }

    @Override
    public List<Scale> getPreferredScales() {
        return preferredScales;
    }

    void setFocusedZone(int index, double[][] controlPoints) {
        for (val preview : previews){
            if (preview.isShowing()) {
                if (preview instanceof ZoneFinder) {
                    ((ZoneFinder) preview).setFocusedZone(index);
                } else if (preview instanceof HistogramPreview) {
                    ((HistogramPreview) preview).setFocusedZone(index, controlPoints);
                }
            }
        }
    }

    /*
        Constructors
    */

    public ImageEditorEngine( ImageMetadata imageMetadata,
                              ImageInfo exportInfo,
                              ProgressThread thread )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException
    {
        val imagePath = imageMetadata.getPath();
        val imageFile = new File(imagePath).getCanonicalFile();
        m_imageInfo = ImageInfo.getInstanceFor(imageFile);
        System.out.println("Opening " + imageFile);

        m_exportInfo = exportInfo;
        metadata = imageMetadata; // imageInfo.getMetadata();
        sourceImage = m_imageInfo.getImage( thread, true );
        auxInfo = m_imageInfo.getAuxiliaryInfo();

        if (sourceImage == null)
            throw new IOException("Something wrong with opening " + metadata.getFile().getName());

        val orientation = metadata.getOrientation();
        if (orientation != null) {
            val transposeAngle = orientation.getCorrection();
            if (transposeAngle != null) {
                val pb = new ParameterBlock();
                pb.addSource(sourceImage);
                pb.add(transposeAngle);
                val transposed = JAI.create("Transpose", pb, null);
                transposed.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                sourceImage = copyImageDataFrom(transposed);
                transposed.dispose();
            }
        }

        rendering = new Rendering(sourceImage, this);
        addFirstPaintLatency = true;
    }

    private static CachedImage copyImageDataFrom(PlanarImage src) {
        val dst = new CachedImage(new ImageLayout(src), JAIContext.fileCache);

        // Fast hack for data copy, assumes that images have identical layout
        val maxTileX = dst.getMaxTileX();
        val maxTileY = dst.getMaxTileY();
        switch(src.getSampleModel().getDataType()){
        case DataBuffer.TYPE_USHORT:
            for (int x = 0; x <= maxTileX; x++) {
                for (int y = 0; y <= maxTileY; y++) {
                    val srcData = ((DataBufferUShort) src.getTile(x, y).getDataBuffer()).getData();
                    val dstData = ((DataBufferUShort) dst.getWritableTile(x, y).getDataBuffer()).getData();
                    System.arraycopy(srcData, 0, dstData, 0, srcData.length);
                }
            }
            break;
        case DataBuffer.TYPE_BYTE:
            for (int x = 0; x <= maxTileX; x++) {
                for (int y = 0; y <= maxTileY; y++) {
                    val srcData = ((DataBufferByte) src.getTile(x, y).getDataBuffer()).getData();
                    val dstData = ((DataBufferByte) dst.getWritableTile(x, y).getDataBuffer()).getData();
                    System.arraycopy(srcData, 0, dstData, 0, srcData.length);
                }
            }
            break;
        default:
            throw new IllegalArgumentException(
                    "Unknown image data type: " + src.getSampleModel().getDataType());
        }
        return dst;
    }

    public ImageEditorEngine( RenderedImage image ) {
        if (! (image instanceof PlanarImage)) {
            image = new RenderedImageAdapter(image);
        }
        sourceImage = (PlanarImage) image;
        rendering = new Rendering(sourceImage, this);
        backgroundImage = image;
    }

    private boolean disposed = false;

    @Override
    public void dispose() {
        if (disposed)
            return;
        disposed = true;

        System.out.println("Disposing Engine");

        if (swingTimer != null) {
            swingTimer.stop();
            ActionListener[] als = swingTimer.getActionListeners();
            for (ActionListener al : als)
                swingTimer.removeActionListener(al);

            swingTimer = null;
        }

        m_imageInfo = null;
        m_exportInfo = null;

        rendering.dispose();

        canvas.dispose();
        canvas = null;
        metadata = null;
        rendering = null;
        listeners = null;
        previews = null;

        if (sourceImage != null) {
            sourceImage.dispose();
            sourceImage = null;
        }
        if (processedImage != null) {
            processedImage.dispose();
            processedImage = null;
        }
        if (previewImage != null) {
            previewImage.dispose();
            previewImage = null;
        }
    }

    /*
        Operations definition
    */

    private static Map<OperationType, Class<? extends BlendedOperation>> operationsSet =
            new HashMap<OperationType, Class<? extends BlendedOperation>>();

    static {
        operationsSet.put(UnSharpMaskOperation.typeV1, UnSharpMaskOperation.class);
        operationsSet.put(UnSharpMaskOperation.typeV2, UnSharpMaskOperation.class);
        operationsSet.put(UnSharpMaskOperation.typeV3, UnSharpMaskOperation.class);
        operationsSet.put(NoiseReductionOperation.type, NoiseReductionOperation.class);
        operationsSet.put(AdvancedNoiseReductionOperation.typeV1, AdvancedNoiseReductionOperation.class);
        operationsSet.put(AdvancedNoiseReductionOperation.typeV2, AdvancedNoiseReductionOperation.class);
        operationsSet.put(AdvancedNoiseReductionOperation.typeV3, AdvancedNoiseReductionOperation.class);
        operationsSet.put(AdvancedNoiseReductionOperationV4.type, AdvancedNoiseReductionOperationV4.class);
        operationsSet.put(AdvancedNoiseReductionOperationV5.type, AdvancedNoiseReductionOperationV5.class);
        operationsSet.put(HiPassFilterOperation.type, HiPassFilterOperation.class);
        operationsSet.put(HueSaturationOperation.typeV1, HueSaturationOperation.class);
        operationsSet.put(HueSaturationOperation.typeV2, HueSaturationOperation.class);
        operationsSet.put(HueSaturationOperation.typeV3, HueSaturationOperation.class);
        operationsSet.put(GaussianBlurOperation.type, GaussianBlurOperation.class);
        operationsSet.put(HDROperation.type, HDROperation.class);
        operationsSet.put(HDROperationV2.typeV2, HDROperationV2.class);
        operationsSet.put(HDROperationV2.typeV3, HDROperationV2.class);
        operationsSet.put(HDROperationV2.typeV4, HDROperationV2.class);
        operationsSet.put(HDROperationV3.typeV5, HDROperationV3.class);
        operationsSet.put(ContrastMaskOperation.type, ContrastMaskOperation.class);
        operationsSet.put(WhiteBalanceV2.typeV2, WhiteBalanceV2.class);
        operationsSet.put(WhiteBalanceV2.typeV3, WhiteBalanceV2.class);
        operationsSet.put(WhiteBalance.type, WhiteBalance.class);
        operationsSet.put(ChannelMixer.type, ChannelMixer.class);
        operationsSet.put(ChannelMixerV2.typeV2, ChannelMixerV2.class);
        operationsSet.put(ChannelMixerV2.typeV3, ChannelMixerV2.class);
        operationsSet.put(ChannelMixerV2.typeV4, ChannelMixerV2.class);
        operationsSet.put(ColorBalanceOperationV2.typeV2, ColorBalanceOperationV2.class);
        operationsSet.put(ColorBalanceOperationV2.typeV3, ColorBalanceOperationV2.class);
        operationsSet.put(ColorBalanceOperation.type, ColorBalanceOperation.class);
        operationsSet.put(RedEyesOperation.type, RedEyesOperation.class);
        operationsSet.put(RawAdjustmentsOperation.typeV1, RawAdjustmentsOperation.class);
        operationsSet.put(RawAdjustmentsOperation.typeV2, RawAdjustmentsOperation.class);
        operationsSet.put(FilmGrainOperation.type, FilmGrainOperation.class);
    }

    @Override
    public Collection<OperationType> getGenericOperationTypes() {
        return operationsSet.keySet();
    }

    @Override
    public com.lightcrafts.model.Operation insertOperation(OperationType type, int position) {
        val opClass = operationsSet.get(type);

        if (opClass.equals(RawAdjustmentsOperation.class)) {
            if (! (getAuxInfo() instanceof RawImageInfo)) {
                throw new RuntimeException("RAW adjustments can not be applied to an image that is not in RAW format");
            }
        }
        OperationImpl op = null;

        /*
            NOTE: this code depends on the signature of the constructor being BlendedOperation(Rendering)
            smart, isn't it?
        */

        try {
            try {
                val c = opClass.getConstructor(Rendering.class, OperationType.class, ImageMetadata.class);
                op = c.newInstance(rendering, type, metadata);
            } catch (NoSuchMethodException e3) {
                try {
                    val c = opClass.getConstructor(Rendering.class, OperationType.class);
                    op = c.newInstance(rendering, type);
                } catch (NoSuchMethodException e2) {
                    val c = opClass.getConstructor(Rendering.class);
                    op = c.newInstance(rendering);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: what to do here?
        }

        if (op != null) {
            rendering.addOperation(position, op);
            if (!op.neutralDefault())
                update(op, false);
        }

        return op;
    }

    /*
        Pipeline Modification
    */

    @Override
    public ZoneOperation insertZoneOperation(int position) {
        val op = new ZoneOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public CloneOperation insertCloneOperation(int position) {
        val op = new CloneOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public SpotOperation insertSpotOperation(int position) {
        val op = new SpotOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public WhitePointOperation insertWhitePointOperation(int position) {
        val op = new WhitePointOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public LensCorrectionsOperation insertLensCorrectionsOperation(int position) {
        val type = LensCorrectionsOperation.type;
        val op = new LensCorrectionsOperation(rendering, type, metadata);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public OperationType getRawAdjustmentsOperationType() {
        return RawAdjustmentsOperation.typeV2;
    }

    @Override
    public OperationType getGenericRawAdjustmentsOperationType() {
        return RawAdjustmentsOperation.typeV1;
    }

    @Override
    public void removeOperation(int position) {
        val currentSelection = selectedOperation >= 0
                ? rendering.getOperation(selectedOperation)
                : null;

        val op = (OperationImpl) rendering.removeOperation(position);
        op.dispose();

        if (currentSelection != null)
            selectedOperation = rendering.indexOf(currentSelection);

        update(op, false);
    }

    @Override
    public void swap(int position) {
        val currentSelection = selectedOperation >= 0
                ? rendering.getOperation(selectedOperation)
                : null;

        val op = (OperationImpl) rendering.removeOperation(position);
        rendering.addOperation(position + 1, op);

        if (currentSelection != null)
            selectedOperation = rendering.indexOf(currentSelection);

        update(op, false);
    }

    @Override
    public void setCropBounds(CropBounds crop) {
        rendering.setCropBounds(crop);
        // canvas.setShowPreview(crop.equals(new CropBounds()));
        update(null, false);
    }

    @Override
    public void setScale(Scale scale) {
        this.scale = scale;
        rendering.setScaleFactor(scale.getFactor());
        update(null, false);
    }

    @Override
    public Scale setScale(Rectangle rect) {
        val dimension = getNaturalSize();

        val hScale = rect.height / (float) dimension.height;
        val wScale = rect.width  / (float) dimension.width;

        rendering.setScaleFactor(Math.min(hScale, wScale));
        update(null, false);
        scale = new Scale(rendering.getScaleFactor());
        return scale;
    }

    PlanarImage scaleFinal(PlanarImage image) {
        val scale = rendering.getScaleFactor() > 1 ? rendering.getScaleFactor() : 1f;

        if (scale == 1)
            return image;

        val scaleX = (float) Math.floor(scale * image.getWidth())  / image.getWidth();
        val scaleY = (float) Math.floor(scale * image.getHeight()) / image.getHeight();

        val xform = AffineTransform.getScaleInstance(scaleX, scaleY);

        val formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        val interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        val params = new ParameterBlock();
        params.addSource(image);
        params.add(xform);
        params.add(interp);
        // NOTE: we cache this for the screen
        return JAI.create("Affine", params, formatHints);
    }

    /*
        Do something special when the current operation is selected
     */

    private int selectedOperation = -1;

    @Override
    public synchronized void setSelectedOperation(int position, boolean selected) {
        val op = (OperationImpl) rendering.getOperation(position);
        if (op != null)
            op.setSelected(selected);

        selectedOperation = selected && op != null ? position : -1;

        update(null, false);

        // System.out.println((selected ? "selecting " : "unselecting ") + "operation " + position
                // + ", current: " + selectedOperation);
    }

    public synchronized Operation getSelectedOperation() {
        return (selectedOperation >= 0) ? rendering.getOperation(selectedOperation) : null;
    }

    synchronized int getSelectedOperationIndex() {
        return selectedOperation;
    }

    public PlanarImage getRendering(int stopBefore) {
        return (stopBefore >= 0) ? rendering.getRendering(stopBefore) : null;
    }

    public void update(OperationImpl op, boolean isLive) {
        update(op, isLive, null);
    }

    /*
        Main Pipeline update routine
    */

    private PlanarImage previewImage = null;

    public synchronized void update(OperationImpl op, boolean isLive, Object updater) {
        // This gets called whenever the parameters of any Operation change.
        // Rerun the pipeline now, and queue a repaint on the AWT thread.

        if (canvas == null || !event_filter(isLive, updater))
            return;

        val oldProcessedImage = processedImage;

        // TODO: use disconnected cached images instead of PERSISTENT_CACHE_TAG

        processedImage = rendering.getRendering(!engineActive);
        processedImage.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

        // if (oldProcessedImage != processedImage) {
            if (oldProcessedImage != null)
                oldProcessedImage.dispose();

            if (previewImage != null)
                previewImage.dispose();

            previewImage = Functions.fromUShortToByte(Functions.toColorSpace(processedImage,
                                                                             JAIContext.systemColorSpace,
                                                                             this.proofProfile,
                                                                             null,
                                                                             this.proofIntent,
                                                                             null),
                                                      null); // Cache this for the preview

            previewImage.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
        // }

        val finalImage = scaleFinal(previewImage);
        canvas.set(finalImage, isLive);
    }

    /*
        CanvasPaintListener get notifications from the Image Editor Display when the image is recomputed
    */

    private long synchImageRepaintTime = 300; // initial estimate 300ms

    private long tilesRead = 0;
    private long tilesWritten = 0;
    private long tilesOnDisk = 0;

    class CanvasPaintListener implements PaintListener {
        @Override
        public void paintDone(PlanarImage image, Rectangle visibleRect, boolean synchronous, long time) {
            if (synchronous) {
                synchImageRepaintTime = (synchImageRepaintTime + time) / 2;
                // System.out.println("fast repaint done in " + time
                        // + "ms, average: "
                        // + synchImageRepaintTime + "ms");
            }
            // else {
                // System.out.println("slow repaint done");
            // }

            val tileCache = JAI.getDefaultInstance().getTileCache();
            if (tileCache instanceof LCTileCache) {
                val tc = (LCTileCache) tileCache;
                if (tilesRead != tc.tilesRead()
                        || tilesWritten != tc.tilesWritten()
                        || tilesOnDisk != tc.tilesOnDisk()) {
                    tilesRead = tc.tilesRead();
                    tilesWritten = tc.tilesWritten();
                    tilesOnDisk = tc.tilesOnDisk();
                    System.out.println("Tile Cache Statistics r: " + tilesRead
                            + ", w: " + tilesWritten
                            + ", on disk: " + tilesOnDisk);
                }
            }

            for (val preview : previews) {
                // TODO: Java8 Stream filter
                if (!preview.isShowing() || !(preview instanceof PaintListener))
                    continue;

                val renderingScale = rendering.getScaleFactor();

                val previewVisibleRect = (renderingScale > 1)
                        ? new Rectangle((int) (visibleRect.x / renderingScale),
                                        (int) (visibleRect.y / renderingScale),
                                        (int) (visibleRect.width  / renderingScale),
                                        (int) (visibleRect.height / renderingScale))
                        : visibleRect;

                ((PaintListener) preview).paintDone(
                        preview instanceof ZoneFinder ? previewImage : processedImage,
                        previewVisibleRect, synchronous, time);
            }
        }
    }

    /*
        BIG NOTE: JAI has all sorts of deadlocks in its notification management,
        we just avoid doing any pipeline setup off the main event thread.
        For this reason we use javax.swing.Timer instead of java.util.Timer
        to make sure that everything happens on the AWT thread.
    */

    class UpdateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            update(null, true, this);
            // currentTask = null;
        }
    }

    private javax.swing.Timer swingTimer = null;
    private UpdateActionListener currentTask = null;

    private long lastTime = -1;

    private boolean event_filter(boolean isLive, Object updater) {
        if (updater != null && updater != currentTask)
            return false; // obsolete update operation

        if (!isLive) {
            if (swingTimer != null && swingTimer.isRunning()) {
                swingTimer.stop();
                swingTimer.removeActionListener(currentTask);
                currentTask = null;
            }
            lastTime = -1;
            return true;
        }

        // During live updates wait until the user stays put for at least the current average repaint time
        val timeNow = System.currentTimeMillis();
        val timeDiff = (lastTime == -1) ? 0 : timeNow - lastTime;
        lastTime = timeNow;

        val delay = Math.min(Math.max(synchImageRepaintTime, 300), 1000);

        if (timeDiff < delay) {
            if (swingTimer != null) {
                if (currentTask != null)
                    swingTimer.removeActionListener(currentTask);

                currentTask = new UpdateActionListener();
                swingTimer.addActionListener(currentTask);
                swingTimer.setInitialDelay((int) delay);

                if (swingTimer.isRunning())
                    swingTimer.restart();
                else
                    swingTimer.start();
            }
            else {
                currentTask = new UpdateActionListener();
                swingTimer = new javax.swing.Timer((int) delay, currentTask);
                swingTimer.setRepeats(false);
                swingTimer.start();
            }
            return false;
        }

        swingTimer.removeActionListener(currentTask);
        currentTask = null;
        lastTime = -1;
        return true;
    }

    @Override
    public void print(ProgressThread thread, PageFormat format, PrintSettings settings) throws PrinterException {
        Platform.getPlatform().getPrinterLayer().print(this, thread, format, settings);
    }

    @Override
    public void cancelPrint() {
        Platform.getPlatform().getPrinterLayer().cancelPrint();
    }

    private static final Map<RenderingIntent, LCMSColorConvertDescriptor.RenderingIntent> renderingIntentMap =
            new HashMap<RenderingIntent, LCMSColorConvertDescriptor.RenderingIntent>();

    static {
        renderingIntentMap.put(RenderingIntent.RELATIVE_COLORIMETRIC,
                    LCMSColorConvertDescriptor.RELATIVE_COLORIMETRIC);

        renderingIntentMap.put(RenderingIntent.PERCEPTUAL,
                    LCMSColorConvertDescriptor.PERCEPTUAL);

        renderingIntentMap.put(RenderingIntent.SATURATION,
                    LCMSColorConvertDescriptor.SATURATION);

        renderingIntentMap.put(RenderingIntent.ABSOLUTE_COLORIMETRIC,
                    LCMSColorConvertDescriptor.ABSOLUTE_COLORIMETRIC);

        renderingIntentMap.put(RenderingIntent.RELATIVE_COLORIMETRIC_BP,
                    LCMSColorConvertDescriptor.RELATIVE_COLORIMETRIC_BP);
    }

    public static LCMSColorConvertDescriptor.RenderingIntent getLCMSIntent(RenderingIntent intent) {
        return renderingIntentMap.get(intent);
    }

    @Getter
    private ICC_Profile proofProfile = null;

    @Getter
    private LCMSColorConvertDescriptor.RenderingIntent proofIntent = null;

    @Override
    public void preview(PrintSettings settings) {
        if (settings != null) {
            proofProfile = settings.getColorProfile();
            proofIntent = renderingIntentMap.get(settings.getRenderingIntent());
        } else {
            proofProfile = null;
            proofIntent = null;
        }
        update(null, false);
    }

    @Override
    public PlanarImage getRendering(Dimension bounds) {
        return getRendering(bounds, JAIContext.sRGBColorProfile, true);
    }

    public PlanarImage getRendering(Dimension bounds, ICC_Profile profile, boolean isEightBits) {
        return getRendering(bounds, profile, null, isEightBits);
    }

    public PlanarImage getRendering(Dimension bounds, ICC_Profile profile,
                                    LCMSColorConvertDescriptor.RenderingIntent intent,
                                    boolean isEightBits) {
        val scale = (bounds != null) ? rendering.getScaleToFit(bounds) : 1;

        val newRendering = canvas != null ? rendering.clone() : rendering;

        newRendering.setCropAndScale(getCropBounds(), scale);

        PlanarImage image = newRendering.getRendering();

        if (profile != null) {
            val exportColorSpace = (profile == JAIContext.sRGBColorProfile)
                ? JAIContext.sRGBColorSpace
                : new ICC_ColorSpace(profile);
            image = Functions.toColorSpace(image, exportColorSpace, intent, null);
        }

        return isEightBits ? Functions.fromUShortToByte(image, null) : image;
    }

    public void prefetchRendering(Rectangle area) {
        rendering.prefetch(area);
    }

    // Export an image rendering to a file
    @Override
    public void write( ProgressThread thread,
                       ImageExportOptions exportOptions ) throws IOException {
        val fileOptions = (ImageFileExportOptions)exportOptions;
        val exportType = exportOptions.getImageType();
        val exportWidth = fileOptions.resizeWidth.getValue();
        val exportHeight = fileOptions.resizeHeight.getValue();

        val exportProfileName = fileOptions.colorProfile.getValue();
        ICC_Profile profile =
            ColorProfileInfo.getExportICCProfileFor( exportProfileName );
        if ( profile == null )
            profile = JAIContext.sRGBExportColorProfile;

        PlanarImage exportImage = getRendering(
            new Dimension( exportWidth, exportHeight ), profile,
            exportType instanceof JPEGImageType ||
                exportOptions.getIntValueOf(BitsPerChannelOption.NAME) == 8
        );

        // Uprez output images

        val scale = Math.min(exportWidth / (double) exportImage.getWidth(),
                             exportHeight / (double) exportImage.getHeight());

        if (scale > 1) {
            val xform = AffineTransform.getScaleInstance(scale, scale);
            val formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                    BorderExtender.createInstance(BorderExtender.BORDER_COPY));

            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
            ParameterBlock params = new ParameterBlock();
            params.addSource(exportImage);
            params.add(xform);
            params.add(interp);
            exportImage = JAI.create("Affine", params, formatHints);
        }

        // Make sure that if uprezzing was requested and denied, the metadata
        // reflect the actual output image size
        if (fileOptions.resizeWidth.getValue() > exportImage.getWidth()) {
            fileOptions.resizeWidth.setValue(exportImage.getWidth());
        }
        if (fileOptions.resizeHeight.getValue() > exportImage.getHeight()) {
            fileOptions.resizeHeight.setValue(exportImage.getHeight());
        }

        if (exportImage instanceof RenderedOp) {
            val rop = (RenderedOp) exportImage;
            rop.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
        }

        // LZN editor state data
        val lzn = exportOptions.getAuxData();
        val imageInfo = (m_exportInfo != null) ? m_exportInfo : m_imageInfo;
        exportType.putImage(imageInfo, exportImage, exportOptions, lzn, thread);
    }

    Color getPixelValue(final int x, final int y) {
        // return getAveragedPixelValue(x, y);
        return getExactPixelValue(x, y);
    }

    private Color getExactPixelValue(final int _x, final int _y) {
        val p = rendering.getInputTransform().transform(new Point(_x, _y), null);
        val x = (int) p.getX();
        val y = (int) p.getY();

        val bounds = processedImage.getBounds();
        if (!bounds.contains(x, y))
            return null;

        int[] rgb = new int[3];
        val tile = processedImage.getTile(processedImage.XToTileX(x), processedImage.YToTileY(y));
        rgb = tile.getPixel(x, y, rgb);
        return new Color(rgb[0] / (float) 0xffff, rgb[1] / (float) 0xffff, rgb[2] / (float) 0xffff);
    }

    private Color getAveragedPixelValue(final int _x, final int _y) {
        val p = rendering.getInputTransform().transform(new Point(_x, _y), null);
        val x = (int) p.getX();
        val y = (int) p.getY();

        val bounds = processedImage.getBounds();
        if (!bounds.contains(x, y))
            return null;

        val rgb = new int[3];
        int numSamples = 0;
        int red = 0;
        int green = 0;
        int blue = 0;

        //Take a 3x3 sample centered at pointer location, and average the samples.
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                val sampleX = x + i;
                val sampleY = y + j;
                val tile = processedImage.getTile(processedImage.XToTileX(x), processedImage.YToTileY(y));
                val tileBounds = tile.getBounds();

                //max and min (x,y) coordinates of the bounds
                val minX = tileBounds.x;
                val maxX = tileBounds.x + tileBounds.width - 1;
                val minY = tileBounds.y;
                val maxY = tileBounds.y + tileBounds.height - 1;
                // Check bounds, if in bounds take sample and increment numSamples, else do nothing
                if (sampleX >= minX && sampleX <= maxX && sampleY >= minY && sampleY <= maxY) {
                    tile.getPixel(sampleX, sampleY, rgb);
                    red   += rgb[0];
                    green += rgb[1];
                    blue  += rgb[2];
                    numSamples++;
                }
            }
        }

        red   /= numSamples;
        green /= numSamples;
        blue  /= numSamples;

        return new Color(red / (float) 0xffff, green / (float) 0xffff, blue / (float) 0xffff);
    }

    @Override
    public List<JMenuItem> getDebugItems() {
        val items = new ArrayList<JMenuItem>();

        /*
        JMenuItem tctool = new JMenuItem("TCTool");
        tctool.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    new TCTool();
                }
            }
        );
        items.add(tctool);

        JMenuItem thrashItem = new JMenuItem("Repaint Thrasher");
        thrashItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    RepaintThrasher thrasher = new RepaintThrasher(canvas);
                    thrasher.setLocation(100, 100);
                    thrasher.pack();
                    thrasher.setVisible(true);
                }
            }
        );
        items.add(thrashItem);
        */

        return items;
    }

    @Override
    public void setActive(boolean active) {
        if (engineActive != active) {
            engineActive = active;
            update(null, false);
        }
    }

    @Override
    public void addEngineListener(EngineListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEngineListener(EngineListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(int level) {
        for (val listener : listeners) {
            listener.engineActive(level);
        }
    }

    // Since Anton keeps forgetting to dispose documents, I add a finalizer
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        dispose();
    }
}
/* vim: set et sw=4 ts=4: */
