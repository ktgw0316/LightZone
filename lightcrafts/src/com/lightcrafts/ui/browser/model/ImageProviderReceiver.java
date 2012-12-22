/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.libs.LCImageDataProvider;
import com.lightcrafts.image.libs.LCImageDataReceiver;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.IOException;

/**
 * A buffer for image data that works with the LCJPEG I/O classes by
 * implementing LCImageDataProvider and LCImageDataReceiver.
 */
class ImageProviderReceiver
    implements LCImageDataProvider, LCImageDataReceiver
{
    private List<ByteBuffer> data = new LinkedList<ByteBuffer>();
    private Iterator<ByteBuffer> iter;
    private ByteBuffer current;

    void fill(InputStream in) throws IOException {
        while (in.available() > 0) {
            int available = in.available();
            int chunkSize = Math.max(1024, available);
            byte[] chunk = new byte[chunkSize];
            int read = in.read(chunk);
            ByteBuffer buf = ByteBuffer.wrap(chunk, 0, read);
            putImageData(buf);
            if (read < chunkSize) {
                break;
            }
        }
    }

    public int putImageData(ByteBuffer buf) {
        buf.rewind();
        ByteBuffer chunk = ByteBuffer.allocate(buf.remaining());
        chunk.put(buf);
        data.add(chunk);
        return chunk.capacity();
    }

    public int getImageData(ByteBuffer buf) {
        buf.clear();
        if (iter == null) {
            iter = data.iterator();
        }
        int written = 0;
        while (buf.remaining() > 0 && ((current != null && current.remaining() > 0) || iter.hasNext())) {
            if (current == null || current.remaining() == 0) {
                current = iter.next().duplicate();
                current.rewind();
            }
            if (current.remaining() <= buf.remaining()) {
                written += current.remaining();
                buf.put(current);
            }
            else {
                byte[] data = current.array();
                int amount = buf.remaining();
                written += amount;
                buf.put(data, current.position(), amount);
                current.position(current.position() + amount);
            }
        }
        return written;
    }
}
