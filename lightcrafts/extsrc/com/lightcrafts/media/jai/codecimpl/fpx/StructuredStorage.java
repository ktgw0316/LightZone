/*
 * $RCSfile: StructuredStorage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:41 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl.fpx;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import com.lightcrafts.media.jai.codec.ByteArraySeekableStream;
import com.lightcrafts.media.jai.codec.FileSeekableStream;
import com.lightcrafts.media.jai.codec.SeekableStream;
import com.lightcrafts.media.jai.codec.SegmentedSeekableStream;

//
// NOTE -- all 'long' variables are really at most 32 bits,
// corresponding to Microsoft 'ULONG' variables.
//

// Temporary (?) assumptions:
//
// All streams, including the ministream, are shorter than 2GB (size < 2GB)
//
// There are < 2^31 directory entries (#streams < 2^31)
//

class SSDirectoryEntry {

    int index;
    String name;
    long size;
    long startSector;
    long SIDLeftSibling;
    long SIDRightSibling;
    long SIDChild;

    public SSDirectoryEntry(int index,
                            String name,
                            long size,
                            long startSector,
                            long SIDLeftSibling,
                            long SIDRightSibling,
                            long SIDChild) {
        this.name = name;
        this.index = index;
        this.size = size;
        this.startSector = startSector;
        this.SIDLeftSibling = SIDLeftSibling;
        this.SIDRightSibling = SIDRightSibling;
        this.SIDChild = SIDChild;

        // System.out.println("Got a directory entry named " + name +
        //                    " (index " + index + ")");
        // System.out.println("Start sector = " + startSector);
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getStartSector() {
        return startSector;
    }

    public long getSIDLeftSibling() {
        return SIDLeftSibling;
    }

    public long getSIDRightSibling() {
        return SIDRightSibling;
    }

    public long getSIDChild() {
        return SIDChild;
    }
}

public class StructuredStorage {

    // Chain terminator
    private static final long FAT_ENDOFCHAIN = 0xFFFFFFFEL;

    // Free sector
    private static final long FAT_FREESECT   = 0xFFFFFFFFL;

    SeekableStream file;

    // Header fields
    private int sectorShift; // ULONG -- must be between 1 and 31
    private int miniSectorShift;
    private long csectFat;
    private long sectDirStart;
    private long miniSectorCutoff;
    private long sectMiniFatStart;
    private long csectMiniFat;
    private long sectDifStart;
    private long csectDif;
    private long[] sectFat;

    // FAT, MiniFAT, and ministream in unrolled format
    // private long[] FAT; // ULONG -- only 2G entries max
    private long[] MINIFAT; // ULONG -- only 2G entries max
    private SSDirectoryEntry[] DIR;

    private SeekableStream miniStream;
    private SeekableStream FATStream;

    // The index of the current directory
    long cwdIndex = -1L;

    public StructuredStorage(SeekableStream file) throws IOException {
        this.file = file;

        // Read fields from the header
        getHeader();

        // Read the FAT
        getFat();

        // Read the MiniFAT
        getMiniFat();

        // Read the directory
        getDirectory();

        // Read the MiniStream
        getMiniStream();
    }

    private void getHeader() throws IOException {
        file.seek(0x1e);
        this.sectorShift = file.readUnsignedShortLE();
        // System.out.println("sectorShift = " + sectorShift);

        file.seek(0x20);
        this.miniSectorShift = file.readUnsignedShortLE();
        // System.out.println("miniSectorShift = " + miniSectorShift);

        file.seek(0x2c);
        this.csectFat = file.readUnsignedIntLE();
        // System.out.println("csectFat = " + csectFat);

        file.seek(0x30);
        this.sectDirStart = file.readUnsignedIntLE();
        // System.out.println("sectDirStart = " + sectDirStart);

        file.seek(0x38);
        this.miniSectorCutoff = file.readUnsignedIntLE();
        // System.out.println("miniSectorCutoff = " + miniSectorCutoff);

        file.seek(0x3c);
        this.sectMiniFatStart = file.readUnsignedIntLE();
        // System.out.println("sectMiniFatStart = " + sectMiniFatStart);

        file.seek(0x40);
        this.csectMiniFat = file.readUnsignedIntLE();
        // System.out.println("csectMiniFat = " + csectMiniFat);

        file.seek(0x44);
        this.sectDifStart = file.readUnsignedIntLE();
        // System.out.println("sectDifStart = " + sectDifStart);

        file.seek(0x48);
        this.csectDif = file.readUnsignedIntLE();
        // System.out.println("csectDif = " + csectDif);

        this.sectFat = new long[109];
        file.seek(0x4c);
        for (int i = 0; i < 109; i++) {
            this.sectFat[i] = file.readUnsignedIntLE();
        }
    }

    private void getFat() throws IOException {
        int size = getSectorSize();
        int sectsPerFat = size/4;
        int fatsPerDif = size/4 - 1;
        // int index = 0;

        // this.FAT =
        // new long[(int)((csectFat + csectDif*fatsPerDif)*sectsPerFat)];

        /*
        System.out.println("FAT has " +
                           ((int)((csectFat + csectDif*fatsPerDif)*sectsPerFat)) + " entries.");

        System.out.println("csectFat = " + csectFat);
        System.out.println("csectDif = " + csectDif);
        System.out.println("fatsPerDif = " + fatsPerDif);
        System.out.println("sectsPerFat = " + sectsPerFat);
        */

        int numFATSectors = (int)(csectFat + csectDif*fatsPerDif);
        long[] FATSectors = new long[numFATSectors];
        int count = 0;

        for (int i = 0; i < 109; i++) {
            long sector = sectFat[i];
            if (sector == FAT_FREESECT) {
                break;
            }

            FATSectors[count++] = getOffsetOfSector(sectFat[i]);
            // readFatSector(sector, index);
            // index += sectsPerFat;
        }

        if (csectDif > 0) {
            long dif = sectDifStart;
            byte[] difBuf = new byte[size];

            for (int i = 0; i < csectDif; i++) {
                readSector(dif, difBuf, 0);
                for (int j = 0; j < fatsPerDif; j++) {
                    int sec = FPXUtils.getIntLE(difBuf, 4*j);
                    FATSectors[count++] = getOffsetOfSector(sec);
                    // readFatSector(sec, index);
                    // index += sectsPerFat;
                }

                dif = FPXUtils.getIntLE(difBuf, size - 4);
            }
        }

        FATStream = new SegmentedSeekableStream(file,
                                                FATSectors,
                                                size,
                                                numFATSectors*size,
                                                true);
    }

    private void getMiniFat() throws IOException {
        int size = getSectorSize();
        int sectsPerFat = size/4;
        int index = 0;

        this.MINIFAT = new long[(int)(csectMiniFat*sectsPerFat)];

        long sector = sectMiniFatStart;
        // System.out.println("minifat start sector = " + sector);
        byte[] buf = new byte[size];
        while (sector != FAT_ENDOFCHAIN) {
            // System.out.println("minifat sector = " + sector);
            readSector(sector, buf, 0);
            for (int j = 0; j < sectsPerFat; j++) {
                MINIFAT[index++] = FPXUtils.getIntLE(buf, 4*j);
            }
            sector = getFATSector(sector);
        }
    }

    private void getDirectory() throws IOException {
        int size = getSectorSize();
        long sector = sectDirStart;

        // Count the length of the directory in sectors
        int numDirectorySectors = 0;
        while (sector != FAT_ENDOFCHAIN) {
            sector = getFATSector(sector);
            ++numDirectorySectors;
        }

        int directoryEntries = 4*numDirectorySectors;
        this.DIR = new SSDirectoryEntry[directoryEntries];

        sector = sectDirStart;
        byte[] buf = new byte[size];
        int index = 0;
        while (sector != FAT_ENDOFCHAIN) {
            readSector(sector, buf, 0);

            int offset = 0;
            for (int i = 0; i < 4; i++) { // 4 dirents per sector
                // We divide the length by 2 for now even though
                // the spec says not to...
                int length = FPXUtils.getShortLE(buf, offset + 0x40);
                // System.out.println("\n\nDirent name length = " + length);

                /*
                FPXUtils.dumpBuffer(buf, offset, 128, 0);

                for (int j = 0; j < 32; j++) {
                    int c = FPXUtils.getShortLE(buf, offset + 2*j);
                    System.out.println("name[" + (2*j) + "] = " + c +
                                       " '" + (char)c + "'");
                }
                */

                String name = FPXUtils.getString(buf, offset + 0x00, length);
                long SIDLeftSibling =
                    FPXUtils.getUnsignedIntLE(buf, offset + 0x44);
                long SIDRightSibling =
                    FPXUtils.getUnsignedIntLE(buf, offset + 0x48);
                long SIDChild = FPXUtils.getUnsignedIntLE(buf, offset + 0x4c);
                long startSector =
                    FPXUtils.getUnsignedIntLE(buf, offset + 0x74);
                long streamSize = FPXUtils.getUnsignedIntLE(buf, offset + 0x78);

                DIR[index] = new SSDirectoryEntry(index,
                                                  name,
                                                  streamSize,
                                                  startSector,
                                                  SIDLeftSibling,
                                                  SIDRightSibling,
                                                  SIDChild);
                ++index;
                offset += 128;
            }

            sector = getFATSector(sector);
        }
    }

    private void getMiniStream() throws IOException {
        int length = getLength(0L);
        int sectorSize = getSectorSize();
        int sectors = (int)((length + sectorSize - 1)/sectorSize);

        long[] segmentPositions = new long[sectors];

        long sector = getStartSector(0);
        // int offset = 0;
        for (int i = 0; i < sectors - 1; i++) {
            segmentPositions[i] = getOffsetOfSector(sector);
            sector = getFATSector(sector);
            if(sector == FAT_ENDOFCHAIN) break;
        }
        segmentPositions[sectors - 1] = getOffsetOfSector(sector);

        miniStream = new SegmentedSeekableStream(file,
                                                 segmentPositions,
                                                 sectorSize,
                                                 length,
                                                 true);
    }

    /*
    private void readFatSector(long sector, int index) throws IOException {
        int sectsPerFat = getSectorSize()/4;
        long offset = getOffsetOfSector(sector);

        file.seek(offset);
        for (int i = 0; i < sectsPerFat; i++) {
            FAT[index] = file.readUnsignedIntLE();
            // System.out.println("FAT[" + index + "] = " + FAT[index]);
            index++;
        }
    }
    */

    private int getSectorSize() {
        return 1 << sectorShift;
    }

    private long getOffsetOfSector(long sector) {
        return sector*getSectorSize() + 512;
    }

    private int getMiniSectorSize() {
        return 1 << miniSectorShift;
    }

    private long getOffsetOfMiniSector(long sector) {
        return sector*getMiniSectorSize();
    }

    private void readMiniSector(long sector, byte[] buf,
                                int offset, int length)
        throws IOException {
        miniStream.seek(getOffsetOfMiniSector(sector));
        miniStream.read(buf, offset, length);
    }

    private void readMiniSector(long sector, byte[] buf, int offset)
        throws IOException {
        readMiniSector(sector, buf, offset, getMiniSectorSize());
    }

    private void readSector(long sector, byte[] buf, int offset, int length)
        throws IOException {
        file.seek(getOffsetOfSector(sector));
        file.read(buf, offset, length);
    }

    private void readSector(long sector, byte[] buf, int offset)
        throws IOException {
        readSector(sector, buf, offset, getSectorSize());
    }

    private SSDirectoryEntry getDirectoryEntry(long index) {
        // Assume #streams < 2^31
        return DIR[(int)index];
    }

    private long getStartSector(long index) {
        // Assume #streams < 2^31
        return DIR[(int)index].getStartSector();
    }

    private int getLength(long index) {
        // Assume #streams < 2^31
        // Assume size < 2GB
        return (int)DIR[(int)index].getSize();
    }

    private long getFATSector(long sector) throws IOException {
        FATStream.seek(4*sector);
        return FATStream.readUnsignedIntLE();
        // return FAT[(int)sector];
    }

    private long getMiniFATSector(long sector) {
        return MINIFAT[(int)sector];
    }

    private int getCurrentIndex() {
        return -1;
    }

    private int getIndex(String name, int index) {
        return -1;
    }

    private long searchDirectory(String name, long index) {
        if (index == FAT_FREESECT) {
            return -1L;
        }

        SSDirectoryEntry dirent = getDirectoryEntry(index);
        /*
        System.out.println("Comparing " + name + " (" + name.length() +
                           ") against " +
                           dirent.getName() + " (" +
                           (dirent.getName()).length() + ") index " +
                           index);
        */

        if (name.equals(dirent.getName())) {
            // System.out.println("Matched!");
            return index;
        } else {
            long lindex =
                searchDirectory(name, dirent.getSIDLeftSibling());
            if (lindex != -1L) {
                return lindex;
            }

            long rindex =
                searchDirectory(name, dirent.getSIDRightSibling());
            if (rindex != -1L) {
                return rindex;
            }
        }

        return -1L;
    }

    // Public methods

    public void changeDirectoryToRoot() {
        cwdIndex = getDirectoryEntry(0L).getSIDChild();
    }

    public boolean changeDirectory(String name) {
        long index = searchDirectory(name, cwdIndex);
        if (index != -1L) {
            cwdIndex = getDirectoryEntry(index).getSIDChild();
            // System.out.println("changeDirectory: setting cwdIndex to " +
            // cwdIndex);
            return true;
        } else {
            return false;
        }
    }

    /*
    public SSDirectory[] getDirectoryEntries() {
    }
    */

    private long getStreamIndex(String name) {
        // Move down the directory hierarchy
        long index = cwdIndex;
        // System.out.println("start index = " + index);

        StringTokenizer st = new StringTokenizer(name, "/");
        boolean firstTime = true;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();

            // System.out.println("Token = " + tok);
            if (!firstTime) {
                index = getDirectoryEntry(index).getSIDChild();
            } else {
                firstTime = false;
            }
            index = searchDirectory(tok, index);
            // System.out.println("index = " + index);
        }

        return index;
    }

    public byte[] getStreamAsBytes(String name) throws IOException {
        long index = getStreamIndex(name);
        if (index == -1L) {
            return null;
        }

        // Cast index to int (streams < 2^31) and cast stream size to an
        // int (size < 2GB)
        int length = getLength(index);
        byte[] buf = new byte[length];

        if (length > miniSectorCutoff) {
            int sectorSize = getSectorSize();
            int sectors = (int)((length + sectorSize - 1)/sectorSize);

            long sector = getStartSector(index);
            int offset = 0;
            for (int i = 0; i < sectors - 1; i++) {
                readSector(sector, buf, offset, sectorSize);
                offset += sectorSize;
                sector = getFATSector(sector);
                // System.out.println("next sector = " + sector);
                if(sector == FAT_ENDOFCHAIN) break;
            }

            readSector(sector, buf, offset, length - offset);
        } else {
            int sectorSize = getMiniSectorSize();
            int sectors = (int)((length + sectorSize - 1)/sectorSize);

            long sector = getStartSector(index);

            // Assume ministream size < 2GB
            int offset = 0;
            for (int i = 0; i < sectors - 1; i++) {
                long miniSectorOffset = getOffsetOfMiniSector(sector);
                readMiniSector(sector, buf, offset, sectorSize);
                offset += sectorSize;
                sector = getMiniFATSector(sector);
            }
            readMiniSector(sector, buf, offset, length - offset);
        }

        return buf;
    }

    public SeekableStream getStream(String name) throws IOException {
        long index = getStreamIndex(name);
        if (index == -1L) {
            return null;
        }

        // Cast index to int (streams < 2^31) and cast stream size to an
        // int (size < 2GB)
        int length = getLength(index);

        long[] segmentPositions;
        int sectorSize, sectors;

        if (length > miniSectorCutoff) {
            sectorSize = getSectorSize();
            sectors = (int)((length + sectorSize - 1)/sectorSize);
            segmentPositions = new long[sectors];

            long sector = getStartSector(index);
            for (int i = 0; i < sectors - 1; i++) {
                segmentPositions[i] = getOffsetOfSector(sector);
                sector = getFATSector(sector);
                if(sector == FAT_ENDOFCHAIN) break;
            }
            segmentPositions[sectors - 1] = getOffsetOfSector(sector);

            return new SegmentedSeekableStream(file,
                                               segmentPositions,
                                               sectorSize,
                                               length,
                                               true);
        } else {
            sectorSize = getMiniSectorSize();
            sectors = (int)((length + sectorSize - 1)/sectorSize);
            segmentPositions = new long[sectors];

            long sector = getStartSector(index);
            for (int i = 0; i < sectors - 1; i++) {
                segmentPositions[i] = getOffsetOfMiniSector(sector);
                sector = getMiniFATSector(sector);
            }
            segmentPositions[sectors - 1] = getOffsetOfMiniSector(sector);

            return new SegmentedSeekableStream(miniStream,
                                               segmentPositions,
                                               sectorSize,
                                               length,
                                               true);
        }
    }

    public static void main(String[] args) {
        try {
            RandomAccessFile f = new RandomAccessFile(args[0], "r");
            SeekableStream sis = new FileSeekableStream(f);
            StructuredStorage ss = new StructuredStorage(sis);

            ss.changeDirectoryToRoot();

            byte[] s = ss.getStreamAsBytes("SummaryInformation");

            PropertySet ps = new PropertySet(new ByteArraySeekableStream(s));

            // Get the thumbnail property
            byte[] thumb = ps.getBlob(17);

            // Emit it as a BMP file
            System.out.print("BM");
            int fs = (thumb.length - 8) + 14 + 40;
            System.out.print((char)(fs & 0xff));
            System.out.print((char)((fs >>  8) & 0xff));
            System.out.print((char)((fs >> 16) & 0xff));
            System.out.print((char)((fs >> 24) & 0xff));
            System.out.print((char)0);
            System.out.print((char)0);
            System.out.print((char)0);
            System.out.print((char)0);
            System.out.print('6');
            System.out.print((char)0);
            System.out.print((char)0);
            System.out.print((char)0);
            for (int i = 8; i < thumb.length; i++) {
                System.out.print((char)(thumb[i] & 0xff));
            }

            /*
            ss.changeDirectory("Data Object Store 000001");
            SeekableStream imageContents =
                ss.getStream("Image Contents");
            */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
