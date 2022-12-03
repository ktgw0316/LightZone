/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.assoc.test;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.RawImageType;
import com.lightcrafts.image.metadata.ImageMetadata;

import java.io.File;
import java.io.IOException;

public class CameraMakeTest {

    private static void printCameraMake(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int n=0; n<files.length; n++) {
                printCameraMake(files[n]);
            }
        }
        if (file.isFile()) {
            try {
                ImageInfo info = ImageInfo.getInstanceFor(file);
                ImageType type = info.getImageType();
                if (type instanceof RawImageType) {
                    ImageMetadata meta = info.getMetadata();
                    String make = meta.getCameraMake(true);
                    System.out.println(file.getName() + ": |" + make + "|");
                }
            }
            catch (BadImageFileException e) {
                handleError(file, e);
            }
            catch (IOException e) {
                handleError(file, e);
            }
            catch (UnknownImageTypeException e) {
                handleError(file, e);
            }
        }
    }

    private static void handleError(File file, Exception e) {
        System.out.print(file.getName());
        System.out.print(": ");
        System.out.print(e.getClass().getName());
        System.out.println(" ");
        System.out.println(e.getMessage());
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: CameraMakeTest (directory)");
            return;
        }
        File file = new File(args[0]);
        if (! file.isDirectory()) {
            System.err.println("\"" + args[0] + "\" is not a directory");
            return;
        }
        System.loadLibrary("DCRaw");
        printCameraMake(file);
    }
}
