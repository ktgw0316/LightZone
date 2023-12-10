/* Copyright (C) 2005-2011 Fabio Riccardi */

package painting;

import com.lightcrafts.model.ImageEditor.ImageEditorDisplay;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.image.ImageInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.media.jai.RasterFactory;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.event.WindowAdapter;
import java.awt.*;
import java.awt.image.*;
import java.awt.color.ICC_Profile;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.util.Arrays;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 23, 2005
 * Time: 2:53:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class PaintingTest extends WindowAdapter {

    PaintingTest() {
        Frame frame = new Frame();
        frame.addWindowListener(this);

        File imageFile = null;
        if (false) {
            JFileChooser chooser = new JFileChooser();
            chooser.showOpenDialog(null);
            imageFile = chooser.getSelectedFile();
        } else {
            FileDialog fileDialog = new FileDialog(frame, "Open");
            fileDialog.show();
            String fileName = fileDialog.getDirectory() + fileDialog.getFile();
            imageFile = new File(fileName);
        }

        RenderedImage source = null;
        RenderedImage dest = null;
        if ( imageFile.exists() && imageFile.canRead() ) {
            ICC_Profile profile = null;
            RenderedImage ri = null;

            try {
                ImageInfo imageInfo = ImageInfo.getInstanceFor( imageFile );
                ri = imageInfo.getImage( null );
            }
            catch ( Exception e ) {
                e.printStackTrace();
                System.exit( -1 );
            }

            Raster data = ri.getTile(0, 0);
            System.out.print("Pixels:");
            for (int i = 0; i < 20; i++)
                for (int b = 0; b < ri.getColorModel().getNumComponents(); b++) {
                    int sample = data.getSample(i, i, b);
                    System.out.print(" " + sample);
                }
            System.out.println();


            ColorSpace defaultCs;
            if (ri.getColorModel().getNumComponents() == 1)
                defaultCs = JAIContext.linearGrayColorSpace;
            else
                defaultCs = JAIContext.sRGBColorSpace;

            ColorSpace cs = (profile == null) ?
                    defaultCs :
                    new ICC_ColorSpace(profile);
            ColorModel colorModel =
                    RasterFactory.createComponentColorModel(ri.getSampleModel().getDataType(),
                            cs,
                            false,
                            false,
                            Transparency.OPAQUE);

            source = new BufferedImage(colorModel, (WritableRaster) ri.getData(), false, null);

            if (source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
                dest = Functions.toColorSpace(PlanarImage.wrapRenderedImage(source), JAIContext.sRGBColorSpace, null);
            } else {
                dest = Functions.fromUShortToByte(
                       Functions.toColorSpace(PlanarImage.wrapRenderedImage(source), JAIContext.sRGBColorSpace, JAIContext.noCacheHint), null);
            }

            data = dest.getTile(0, 0);
            System.out.print("Pixels:");
            for (int i = 0; i < 20; i++)
                for (int b = 0; b < dest.getColorModel().getNumComponents(); b++) {
                    int sample = data.getSample(i, i, b);
                    System.out.print(" " + sample);
                }
            System.out.println();

            // DisplayJAI canvas = new DisplayJAI(dest);

            // LCPlanarImage planarSource = new LCPlanarImage(dest);
            ImageEditorDisplay canvas = new ImageEditorDisplay(null, (RenderedOp) dest);
            Component pane = new JScrollPane(canvas);
            frame.add(pane, BorderLayout.CENTER);
        }
        frame.pack();
        frame.setSize(new Dimension(1024, 800));
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        String readFormats[] = ImageIO.getReaderMIMETypes();
        String writeFormats[] = ImageIO.getWriterMIMETypes();
        System.out.println("Readers: " +
                Arrays.asList(readFormats));
        System.out.println("Writers: " +
                Arrays.asList(writeFormats));

        new PaintingTest();
    }
}
