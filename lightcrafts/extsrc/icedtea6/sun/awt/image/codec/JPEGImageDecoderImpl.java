/* JPEGImageDecoderImpl.java -- JPEG decoder implementation
Copyright (C) 2011 Red Hat

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sun.awt.image.codec;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGImageReadParam;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.imageio.plugins.jpeg.JPEGImageReader;

/**
 * This class provides the implementation for a JPEG decoder.
 */
public class JPEGImageDecoderImpl implements JPEGImageDecoder {
    private static final String JPGMime = "image/jpeg";

    private JPEGImageReader JPGReader;
    private InputStream in;
    private JPEGDecodeParam param;

    public JPEGImageDecoderImpl(InputStream in) {
        this(in, null);
    }

    public JPEGImageDecoderImpl(InputStream in, JPEGDecodeParam param) {
        this.in = in;
        setJPEGDecodeParam(param);

        Iterator<ImageReader> JPGReaderIter = ImageIO
                .getImageReadersByMIMEType(JPGMime);
        if (JPGReaderIter.hasNext()) {
            JPGReader = (JPEGImageReader) JPGReaderIter.next();
        }

        JPGReader.setInput(new MemoryCacheImageInputStream(in));
    }

    public BufferedImage decodeAsBufferedImage() throws IOException,
            ImageFormatException {
        JPEGImageReadParam irp = null;

        if (param != null) {
            // We should do more than this, but it's a start.
            JPEGQTable[] qTables = new JPEGQTable[4];
            JPEGHuffmanTable[] DCHuffmanTables = new JPEGHuffmanTable[4];
            JPEGHuffmanTable[] ACHuffmanTables = new JPEGHuffmanTable[4];

            for (int i = 0; i < 4; i++) {
                qTables[i] = new JPEGQTable(param.getQTable(i).getTable());
                com.sun.image.codec.jpeg.JPEGHuffmanTable dcHuffman = param.getDCHuffmanTable(i);
                com.sun.image.codec.jpeg.JPEGHuffmanTable acHuffman = param.getACHuffmanTable(i);
                DCHuffmanTables[i] = new JPEGHuffmanTable(dcHuffman.getLengths(),
                                                          dcHuffman.getSymbols());
                ACHuffmanTables[i] = new JPEGHuffmanTable(acHuffman.getLengths(),
                                                          dcHuffman.getSymbols());
            }

            irp = new JPEGImageReadParam();
            irp.setDecodeTables(qTables, DCHuffmanTables, ACHuffmanTables);
        }

        return JPGReader.read(0, irp);
    }

    public Raster decodeAsRaster() throws IOException, ImageFormatException {
        return JPGReader.readRaster(0, null);
    }

    public InputStream getInputStream() {
        return in;
    }

    public JPEGDecodeParam getJPEGDecodeParam() {
        if (param == null) return null;
        return (JPEGDecodeParam) param.clone();
    }

    public void setJPEGDecodeParam(JPEGDecodeParam jdp) {
        param = jdp;
    }
}
