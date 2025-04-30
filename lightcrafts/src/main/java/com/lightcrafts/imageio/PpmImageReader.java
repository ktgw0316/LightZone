package com.lightcrafts.imageio;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import com.lightcrafts.utils.raw.DCRaw;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class PpmImageReader extends ImageReader {

    private int width;
    private int height;
    private int bands;
    private int dataType;
    private ImageInputStream stream;
    private Object data;

    public PpmImageReader(PpmImageReaderSpi spi) {
        super(spi);
    }

    @Override
    public int getNumImages(boolean allowSearch) {
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) {
        return width;
    }

    @Override
    public int getHeight(int imageIndex) {
        return height;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
        // TODO
        return null;
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        if (!(input instanceof ImageInputStream)) {
            return null;
        }
        stream = (ImageInputStream) input;

        readHeader();
        readData();

        final var cm = getColorModel(mode, bands, dataType);
        final var bufSize = bands * width * height;
        final DataBuffer buf = dataType == DataBuffer.TYPE_BYTE
                ? new DataBufferByte(   (byte[]) data, bufSize)
                : new DataBufferUShort((short[]) data, bufSize);
        final var bandOffsets = bands == 3 ? new int[]{0, 1, 2} : new int[]{0};
        final var raster = Raster.createInterleavedRaster(
                buf, width, height, bands * width, bands, bandOffsets, null);
        return new BufferedImage(cm, raster, false, null);
    }

    private String readln() throws IOException {
        int c = '\n';
        while (c == '\n' || c == '\r') {
            // Skip carriage returns and line feeds
            c = stream.read();
        }
        final var sb = new StringBuilder();
        while (c > 0 && c != 255 && c != '\n' && c != '\r') {
            sb.append((char) c);
            c = stream.read();
        }
        return ((c == -1 || c == 255) && sb.isEmpty()) ? null : new String(sb);
    }

    private void readHeader() throws IOException {
        final var S1 = readln();
        if (S1 == null) {
            throw new BadImageFileException(file);
        }

        switch (S1) {
            case "P5":
            case "P6":
                bands = S1.equals("P5") ? 1 : 3;
                final var S2 = readln();
                final var S3 = readln();
                if (S2 == null || S3 == null) {
                    throw new BadImageFileException(file);
                }
                final var dimensions = S2.split("\\s");
                width = Integer.parseInt(dimensions[0]);
                height = Integer.parseInt(dimensions[1]);
                dataType = S3.equals("255") ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;
                break;
            case "P7":
                final var SWIDTH = readln();
                final var SHEIGHT = readln();
                final var SDEPTH = readln();
                final var SMAXVAL = readln();
                if (SWIDTH == null || SHEIGHT == null || SDEPTH == null || SMAXVAL == null) {
                    throw new BadImageFileException(file);
                }
                final var WIDTH = "WIDTH ";
                width = Integer.parseInt(SWIDTH.substring(WIDTH.length()));
                final var HEIGHT = "HEIGHT ";
                height = Integer.parseInt(SHEIGHT.substring(HEIGHT.length()));
                final var DEPTH = "DEPTH ";
                bands = Integer.parseInt(SDEPTH.substring(DEPTH.length()));
                final var MAXVAL = "MAXVAL ";
                dataType = SMAXVAL.substring(MAXVAL.length()).equals("65535")
                        ? DataBuffer.TYPE_USHORT
                        : DataBuffer.TYPE_BYTE;
                break;
            default:
                throw new BadImageFileException(file);
        }
    }

    private void readData() throws IOException {
        data = dataType == DataBuffer.TYPE_BYTE
                ? new byte[bands * width * height]
                : new short[bands * width * height];
        final var totalData = width * height * bands * (dataType == DataBuffer.TYPE_BYTE ? 1 : 2);

        try (FileChannel c = stream.getChannel()) {
            if (file.length() != totalData + c.position()) {
                throw new BadImageFileException(file);
            }

            ByteBuffer bb = c.map(FileChannel.MapMode.READ_ONLY, c.position(), totalData);

            if (dataType == DataBuffer.TYPE_USHORT) {
                bb.order(ByteOrder.nativeOrder());
                bb.asShortBuffer().get((short[]) data);

                // Dirty hack to prevent crash on Arch Linux (issue #125)
                if (ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN) {
                    for (int i = 0; i < ((short[]) data).length; ++i) {
                        ((short[]) data)[i] = Short.reverseBytes(((short[]) data)[i]);
                    }
                }
            } else {
                bb.get((byte[]) data);
            }

            ByteBufferUtil.clean(bb);
        }
    }

    private ColorModel getColorModel(DCRaw.dcrawMode mode, int bands, int dataType)
            throws UnknownImageTypeException
    {
        final ColorModel cm;
        if (mode == DCRaw.dcrawMode.full) {
            if (bands == 1) {
                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
            } else {
                cm = JAIContext.colorModel_linear16;
            }
        } else {
            if (bands == 3) {
                cm = new ComponentColorModel(JAIContext.sRGBColorSpace,
                        false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            } else if (bands == 4) {
                cm = new ComponentColorModel(JAIContext.CMYKColorSpace,
                        false, false, Transparency.OPAQUE, dataType);
            } else {
                throw new UnknownImageTypeException("Weird number of bands: " + bands);
            }
        }
        return cm;
    }
}
