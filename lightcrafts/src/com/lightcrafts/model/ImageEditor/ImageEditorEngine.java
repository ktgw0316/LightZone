/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.export.BitsPerChannelOption;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageOrientation;
import com.lightcrafts.image.types.AuxiliaryImageInfo;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.RawImageInfo;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.operator.LCMSColorConvertDescriptor;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.utils.LCTileCache;
import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.mediax.jai.operator.TransposeType;
import com.lightcrafts.model.*;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.ColorProfileInfo;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

public class ImageEditorEngine implements Engine {
    private ImageInfo m_imageInfo;
    private ImageInfo m_exportInfo;
    private PlanarImage sourceImage;
    private PlanarImage processedImage;

    private Rendering rendering;

    private ImageEditorDisplay canvas = null;

    private boolean engineActive = true;

    private LinkedList<Preview> previews = new LinkedList<Preview>();

    private static final Scale[] scales = new Scale[] {
        new Scale(1, 40),
        new Scale(1, 20),
        new Scale(1, 10),
        new Scale(1, 8),
        new Scale(1, 6),
        new Scale(1, 4),
        new Scale(1, 3),
        new Scale(1, 2),
        new Scale(2, 3),
        new Scale(1, 1),
        new Scale(2, 1),
        new Scale(3, 1),
        new Scale(4, 1)
    };

    private LinkedList<EngineListener> listeners = null;

    private static boolean DEBUG = false;

    private ImageMetadata metadata = null;
    private AuxiliaryImageInfo auxInfo = null;

    private TransposeType transposeAngle = null;

    private RenderedImage backgroundImage;

    private boolean addFirstPaintLatency;

    public Rendering getRendering() {
        return rendering;
    }

    public AuxiliaryImageInfo getAuxInfo() {
        return auxInfo;
    }

    public ImageMetadata getMetadata() {
        return metadata;
    }

    @Override
    public AffineTransform getTransform() {
        return rendering.getTransform();
    }

    CropBounds getCropBounds() {
        return rendering.getCropBounds();
    }

    PlanarImage getSourceImage() {
        return sourceImage;
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
    public List<Preview> getPreviews() {
        return previews;
    }

    @Override
    public List getLayerModes() {
        return BlendedOperation.blendingModes;
    }

    @Override
    public List getPreferredScales() {
        return Arrays.asList(scales);
    }

    public void setFocusedZone(int index, double[][] controlPoints) {
        for (Preview preview : previews){
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
        String imagePath = imageMetadata.getPath();
        File imageFile = new File(imagePath).getCanonicalFile();
        m_imageInfo = ImageInfo.getInstanceFor(imageFile);

        m_exportInfo = exportInfo;

        System.out.println("Opening " + imageFile);

        metadata = imageMetadata; // imageInfo.getMetadata();

        sourceImage = m_imageInfo.getImage( thread, true );

        auxInfo = m_imageInfo.getAuxiliaryInfo();

        if (sourceImage == null)
            throw new IOException("Something wrong with opening " + metadata.getFile().getName());

        ImageOrientation orientation = metadata.getOrientation();
        if (orientation != null) {
            transposeAngle = orientation.getCorrection();
            if (transposeAngle != null) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(sourceImage);
                pb.add(transposeAngle);
                RenderedOp transposed = JAI.create("Transpose", pb, null);
                transposed.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                // sourceImage = transposed;

                CachedImage cache = new CachedImage(new ImageLayout(transposed), JAIContext.fileCache);

                // Fast hack for daya copy, assumes that images have identical layout
                for (int x = 0; x <= cache.getMaxTileX(); x++)
                    for (int y = 0; y <= cache.getMaxTileY(); y++) {
                        if (transposed.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT) {
                            short[] srcData = ((DataBufferUShort) transposed.getTile(x, y).getDataBuffer()).getData();
                            short[] dstData = ((DataBufferUShort) cache.getWritableTile(x, y).getDataBuffer()).getData();
                            System.arraycopy(srcData, 0, dstData, 0, srcData.length);
                        } else if (transposed.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
                            byte[] srcData = ((DataBufferByte) transposed.getTile(x, y).getDataBuffer()).getData();
                            byte[] dstData = ((DataBufferByte) cache.getWritableTile(x, y).getDataBuffer()).getData();
                            System.arraycopy(srcData, 0, dstData, 0, srcData.length);
                        } else
                            throw new IllegalArgumentException("Unknown image data type: " + transposed.getSampleModel().getDataType());
                        // transposed.copyData(cache.getWritableTile(x, y));
                    }

                sourceImage = cache;

                transposed.dispose();
            }
        }

        rendering = new Rendering(sourceImage, this);

        addFirstPaintLatency = true;
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
    }

    @Override
    public Collection<OperationType> getGenericOperationTypes() {
        return operationsSet.keySet();
    }

    @Override
    public com.lightcrafts.model.Operation insertOperation(OperationType type, int position) {
        Class<? extends BlendedOperation> opClass = operationsSet.get(type);

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
                Constructor<? extends BlendedOperation> c = opClass.getConstructor(Rendering.class, OperationType.class);
                op = c.newInstance(rendering, type);
            } catch (NoSuchMethodException e) {
                Constructor<? extends BlendedOperation> c = opClass.getConstructor(Rendering.class);
                op = c.newInstance(rendering);
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
        ZoneOperation op = new ZoneOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public CloneOperation insertCloneOperation(int position) {
        CloneOperation op = new CloneOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public SpotOperation insertSpotOperation(int position) {
        SpotOperation op = new SpotOperationImpl(rendering);
        rendering.addOperation(position, op);
        return op;
    }

    @Override
    public WhitePointOperation insertWhitePointOperation(int position) {
        WhitePointOperation op = new WhitePointOperationImpl(rendering);
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
        Operation currentSelection = selectedOperation >= 0 ? rendering.getOperation(selectedOperation) : null;

        OperationImpl op = (OperationImpl) rendering.removeOperation(position);
        op.dispose();

        if (currentSelection != null)
            selectedOperation = rendering.indexOf(currentSelection);

        update(op, false);
    }

    @Override
    public void swap(int position) {
        Operation currentSelection = selectedOperation >= 0 ? rendering.getOperation(selectedOperation) : null;

        OperationImpl op = (OperationImpl) rendering.removeOperation(position);
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
        rendering.setScaleFactor(scale.getFactor());
        update(null, false);
    }

    @Override
    public Scale setScale(Rectangle rect) {
        Dimension dimension = getNaturalSize();

        double hScale = rect.height / (double) dimension.height;
        double wScale = rect.width / (double) dimension.width;

        rendering.setScaleFactor((float) Math.min(hScale, wScale));
        update(null, false);
        return new Scale(rendering.getScaleFactor());
    }

    public void update(OperationImpl op, boolean isLive) {
        update(op, isLive, null);
    }

    PlanarImage scaleFinal(PlanarImage image) {
        float scale = rendering.getScaleFactor() > 1 ? rendering.getScaleFactor() : 1;

        if (scale == 1)
            return image;

        float scaleX = (float) Math.floor(scale * image.getWidth()) / (float) image.getWidth();
        float scaleY = (float) Math.floor(scale * image.getHeight()) / (float) image.getHeight();

        AffineTransform xform = AffineTransform.getScaleInstance(scaleX, scaleY);

        RenderingHints formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        ParameterBlock params = new ParameterBlock();
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
        OperationImpl op = (OperationImpl) rendering.getOperation(position);

        if (op != null)
            op.setSelected(selected);

        selectedOperation = selected && op != null ? position : -1;

        update(null, false);

        // System.out.println((selected ? "selecting " : "unselecting ") + "operation " + position + ", current: " + selectedOperation);
    }

    public synchronized Operation getSelectedOperation() {
        if (selectedOperation >= 0)
            return rendering.getOperation(selectedOperation);
        else
            return null;
    }

    public synchronized int getSelectedOperationIndex() {
        return selectedOperation;
    }

    public PlanarImage getRendering(int stopBefore) {
        if (stopBefore >= 0) {
            return rendering.getRendering(stopBefore);
        }
        return null;
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

        PlanarImage oldProcessedImage = processedImage;

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

        PlanarImage finalImage = scaleFinal(previewImage);

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
                // System.out.println("fast repaint done in " + time + "ms, average: " + synchImageRepaintTime + "ms");
            } else {
                // System.out.println("slow repaint done");
            }
            TileCache tileCache = JAI.getDefaultInstance().getTileCache();
            if (tileCache instanceof LCTileCache) {
                LCTileCache tc = (LCTileCache) tileCache;
                if (tilesRead != tc.tilesRead() || tilesWritten != tc.tilesWritten() || tilesOnDisk != tc.tilesOnDisk()) {
                    tilesRead = tc.tilesRead();
                    tilesWritten = tc.tilesWritten();
                    tilesOnDisk = tc.tilesOnDisk();
                    System.out.println("Tile Cache Statistics r: " + tilesRead + ", w: " + tilesWritten + ", on disk: " + tilesOnDisk);
                }
            }

            for (Preview preview : previews) {
                if (!preview.isShowing() || !(preview instanceof PaintListener))
                    continue;

                float renderingScale = rendering.getScaleFactor();
                Rectangle previewVisibleRect = visibleRect;

                if (renderingScale > 1) {
                    previewVisibleRect = new Rectangle((int) (visibleRect.x/renderingScale),
                                                       (int) (visibleRect.y/renderingScale),
                                                       (int) (visibleRect.width/renderingScale),
                                                       (int) (visibleRect.height/renderingScale));
                }

                ((PaintListener) preview).paintDone(preview instanceof ZoneFinder
                                                    ? previewImage
                                                    : processedImage, previewVisibleRect, synchronous, time);
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

        if (isLive) {
            // During live updates wait until the user stays put for at least the current average repaint time

            long timeNow = System.currentTimeMillis();

            long timeDiff = lastTime == -1 ? 0 : timeNow - lastTime;

            lastTime = timeNow;

            final long delay = Math.min(Math.max(synchImageRepaintTime, 300), 1000);

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
                } else {
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
        } else {
            if (swingTimer != null && swingTimer.isRunning()) {
                swingTimer.stop();
                swingTimer.removeActionListener(currentTask);
                currentTask = null;
            }
            lastTime = -1;
        }
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
        renderingIntentMap.put( RenderingIntent.RELATIVE_COLORIMETRIC,
                               LCMSColorConvertDescriptor.RELATIVE_COLORIMETRIC);

        renderingIntentMap.put( RenderingIntent.PERCEPTUAL,
                               LCMSColorConvertDescriptor.PERCEPTUAL);

        renderingIntentMap.put( RenderingIntent.SATURATION,
                               LCMSColorConvertDescriptor.SATURATION);

        renderingIntentMap.put( RenderingIntent.ABSOLUTE_COLORIMETRIC,
                               LCMSColorConvertDescriptor.ABSOLUTE_COLORIMETRIC);

        renderingIntentMap.put( RenderingIntent.RELATIVE_COLORIMETRIC_BP,
                               LCMSColorConvertDescriptor.RELATIVE_COLORIMETRIC_BP);
    }

    public static LCMSColorConvertDescriptor.RenderingIntent getLCMSIntent(RenderingIntent intent) {
        return renderingIntentMap.get(intent);
    }

    private ICC_Profile proofProfile = null;
    private LCMSColorConvertDescriptor.RenderingIntent proofIntent = null;

    public ICC_Profile getProofProfile() {
        return proofProfile;
    }

    public LCMSColorConvertDescriptor.RenderingIntent getProofIntent() {
        return proofIntent;
    }

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

    public PlanarImage getRendering(Dimension bounds, ICC_Profile profile, boolean eightBits) {
        return getRendering(bounds, profile, null, eightBits);
    }

    public PlanarImage getRendering(Dimension bounds, ICC_Profile profile,
                                    LCMSColorConvertDescriptor.RenderingIntent intent, boolean eightBits) {
        Dimension dimension = getNaturalSize();

        float scale = bounds != null
                      ? Math.min(bounds.width / (float) dimension.getWidth(),
                                 bounds.height / (float) dimension.getHeight())
                      : 1;

        Rendering newRendering = canvas != null ? rendering.clone() : rendering;

        newRendering.setCropAndScale(getCropBounds(), scale);

        PlanarImage image = newRendering.getRendering();

        if (profile != null) {
            final ICC_ColorSpace exportColorSpace =
                profile == JAIContext.sRGBColorProfile
                ? JAIContext.sRGBColorSpace
                : new ICC_ColorSpace(profile);
            if (intent != null)
                image = Functions.toColorSpace(image, exportColorSpace, intent, null);
            else
                image = Functions.toColorSpace(image, exportColorSpace, null);
        }

        if (eightBits)
            image = Functions.fromUShortToByte(image, null);

        return image;
    }

    public void prefetchRendering(Rectangle area) {
        rendering.prefetch(area);
    }

    // Export an image rendering to a file
    @Override
    public void write( ProgressThread thread,
                       ImageExportOptions exportOptions ) throws IOException {
        final ImageFileExportOptions fileOptions =
            (ImageFileExportOptions)exportOptions;
        final ImageType exportType = exportOptions.getImageType();
        final int exportWidth = fileOptions.resizeWidth.getValue();
        final int exportHeight = fileOptions.resizeHeight.getValue();

        final String exportProfileName = fileOptions.colorProfile.getValue();
        ICC_Profile profile =
            ColorProfileInfo.getExportICCProfileFor( exportProfileName );
        if ( profile == null )
            profile = JAIContext.sRGBExportColorProfile;

        // LZN editor state data
        final byte[] lzn = exportOptions.getAuxData();

        PlanarImage exportImage = getRendering(
            new Dimension( exportWidth, exportHeight ), profile,
            exportType instanceof JPEGImageType ||
                exportOptions.getIntValueOf(BitsPerChannelOption.NAME) == 8
        );

        // Uprez output images

        double scale = Math.min(exportWidth / (double) exportImage.getWidth(),
                                exportHeight / (double) exportImage.getHeight());

        if (scale > 1) {
            AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);

            RenderingHints formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

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

        if ( exportImage instanceof RenderedOp ) {
            final RenderedOp rop = (RenderedOp) exportImage;
            rop.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
        }

        if (m_exportInfo != null) {
            exportType.putImage(
                m_exportInfo, exportImage, exportOptions, lzn, thread
            );
        }
        else {
            exportType.putImage(
                m_imageInfo, exportImage, exportOptions, lzn, thread
            );
        }
    }

    public Color getPixelValue(int x, int y) {
        // destUShortScaled.getData().getPixel(x, y, rgb);

        //Take a 3x3 sample centered at pointer location, and average the samples.
        int rgb[] = new int[3];
        int numSamples = 0;
        int red = 0;
        int green = 0;
        int blue = 0;

        //max and min (x,y) coordinates of the bounds
        int minX, maxX, minY, maxY;
        int sampleX, sampleY;

        Point2D p = rendering.getInputTransform().transform(new Point(x, y), null);

        x = (int) p.getX();
        y = (int) p.getY();

        Rectangle bounds = processedImage.getBounds();

        if (!bounds.contains(x, y))
            return null;

        if (true) {
            Raster tile = processedImage.getTile(processedImage.XToTileX(x), processedImage.YToTileY(y));
            rgb = tile.getPixel(x, y, rgb);
            return new Color(rgb[0] / (float) 0xffff, rgb[1] / (float) 0xffff, rgb[2] / (float) 0xffff);
        } else {
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    sampleX = x + i;
                    sampleY = y + j;
                    Raster tile = processedImage.getTile(processedImage.XToTileX(x), processedImage.YToTileY(y));
                    bounds = tile.getBounds();
                    minX = bounds.x;
                    maxX = bounds.x + bounds.width - 1;
                    minY = bounds.y;
                    maxY = bounds.y + bounds.height - 1;
                    // Check bounds, if in bounds take sample and increment numSamples, else do nothing
                    if (sampleX >= minX && sampleX <= maxX && sampleY >= minY && sampleY <= maxY) {
                        tile.getPixel(sampleX, sampleY, rgb);
                        red += rgb[0];
                        green += rgb[1];
                        blue += rgb[2];
                        numSamples++;
                    }
                }
            }

            red /= numSamples;
            green /= numSamples;
            blue /= numSamples;

            return new Color(red / (float) 0xffff, green / (float) 0xffff, blue / (float) 0xffff);
        }
    }

    class GrayPatchesImage extends BufferedImage {
        static final int height = 512;
        static final int width = 256;

        GrayPatchesImage(int steps) {
            super(JAIContext.colorModel_sRGB8,
                  JAIContext.colorModel_sRGB8.createCompatibleWritableRaster(width, height),
                  false, null);

            Graphics g = this.getGraphics();

            if (DEBUG) System.out.print("Colors: ");
            for (int i = 0; i < steps; i++) {
                float color = (float) (Math.pow(2, i * 8.0 / (steps - 1)) - 1) / 255.0f;

                float[] srgbColor = Functions.fromLinearToCS(JAIContext.systemColorSpace, new float[] {color, color, color});

                if (DEBUG) System.out.print(", " + i + ":" + (int) (255 * color) + " -> " + (int) (255 * srgbColor[0]));

                g.setColor(new Color((int) (255 * srgbColor[0]), (int) (255 * srgbColor[1]), (int) (255 * srgbColor[2])));
                g.fillRect(0, i * height / steps, width, (i + 1) * height / steps - i * height / steps);
            }
            if (DEBUG) System.out.println();
        }
    }

    @Override
    public List getDebugItems() {
        ArrayList<JMenuItem> items = new ArrayList<JMenuItem>();

        /* JMenuItem tctool = new JMenuItem("TCTool");
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
        items.add(thrashItem);*/

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
        for (EngineListener listener : listeners)
            listener.engineActive(level);
    }

    // Since Anton keeps forgetting to dispose documents, I add a finalizer
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        dispose();
    }
}
/* vim: set et sw=4 ts=4: */
