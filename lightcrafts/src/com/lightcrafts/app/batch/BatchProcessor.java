/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.app.batch;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.DocumentWriter;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.UnsupportedColorProfileException;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.model.Engine;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.export.ExportNameUtility;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

import static com.lightcrafts.app.batch.Locale.LOCALE;

/**
 * This encapsulates the procedures applied when LightZone processes multiple
 * images: browser export, browser template application, paste tools, and
 * the send action.
 */
public class BatchProcessor {

    private static JDialog Dialog;  // Blocks UI during batch processing
    private static BatchText Text;  // Where log messages stream to
    private static Thread Thread;   // Where the work is done
    private static BatchProgressBar Progress;   // Image export progress
    private static BatchImageComponent Image;   // The current Engine image
    private static JLabel Label;    // File counts and time estimates
    private static JButton Button;  // Either "Cancel" or "Done"
    private static long Start;      // Time started, for time estimates
    private static boolean Interrupted; // Flag to halt background work
    private static boolean Finished;    // Flag to indicate work is halted
    private static boolean Canceled;    // Flag to indicate work is canceled by the user

    private static RuntimeException Error;  // Propagate unchecked exceptions

    public static void process(
        final ComboFrame frame,
        final File[] files,
        final XmlDocument template,
        final BatchConfig conf
    ) {
        Thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    frame.pause();
                    try {
                        processTemplate(files, template, conf);
                    }
                    catch (RuntimeException e) {
                        System.err.println(
                            "Unchecked batch processing exception:"
                        );
                        e.printStackTrace();
                        Error = e;
                        Dialog.dispose();
                    }
                    finally {
                        frame.resume();
                    }
                }
            },
            "Template Applicator"
        );
        Text = new BatchText();

        JScrollPane textScroll = new JScrollPane(Text);
        textScroll.setPreferredSize(new Dimension(400, 300));
        textScroll.setBorder(BorderFactory.createLineBorder(Color.gray));

        Label = new JLabel();
        Label.setAlignmentX(.5f);
        initLabel(files.length);

        Button = new JButton(LOCALE.get("BatchCancelButton"));
        Button.setAlignmentX(.5f);

        Image = new BatchImageComponent();

        Progress = new BatchProgressBar();

        // Don't let the progress bar get wider than the image above it:
        Dimension progSize = Progress.getComponent().getPreferredSize();
        progSize.width = Image.getPreferredSize().width;
        Progress.getComponent().setMaximumSize(progSize);

        Box imageBox = Box.createVerticalBox();
        imageBox.add(Box.createVerticalGlue());
        imageBox.add(Image);
        imageBox.add(Box.createVerticalStrut(4));
        imageBox.add(Progress.getComponent());
        imageBox.add(Box.createVerticalGlue());

        Box textBox = Box.createVerticalBox();
        textBox.add(textScroll);
        textBox.add(Box.createVerticalStrut(8));
        textBox.add(Label);
        textBox.add(Box.createVerticalStrut(8));
        textBox.add(Button);

        Box content = Box.createHorizontalBox();
        content.add(imageBox);
        content.add(Box.createHorizontalStrut(8));
        content.add(textBox);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // A root JPanel is needed to control the dialog's background color.
        JPanel background = new JPanel(new BorderLayout());
        background.setBackground(LightZoneSkin.Colors.FrameBackground);
        background.setOpaque(true);
        background.add(content);

        Dialog = new JDialog(frame);

        ActionListener disposeAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!Finished) {
                    Canceled = true;
                    Button.setText("Canceling...");
                } else {
                    Dialog.dispose();
                }
            }
        };
        Button.addActionListener(disposeAction);

        Dialog.setContentPane(background);
        Dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Dialog.setModal(true);
        Dialog.setTitle(LOCALE.get("BatchDialogTitle"));
        Dialog.getRootPane().setDefaultButton(Button);
        Dialog.pack();
        Dialog.setResizable(false);
        Dialog.setLocationRelativeTo(frame);

        Dialog.addComponentListener(
            new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    Interrupted = true;
                }
            }
        );

        Interrupted = false;
        Finished = false;
        Canceled = false;

        Thread.start();
        Dialog.setVisible(true);

        // If they clicked "Cancel", and the thread is live, then shut it down:
        synchronized(Thread) {
            if ((!Finished) && (Error == null)) {
                Interrupted = true;
                try {
                    Thread.wait();
                }
                catch (InterruptedException e) {
                    // Just continue.
                }
            }
            Interrupted = false;
            Finished = false;
            Canceled = false;
        }
        if (Error != null) {
            RuntimeException e = Error;
            Error = null;
            throw e;
        }
    }

    /**
     * Conduct the export and template processes, in the background under the dialog.
     */
    private static void processTemplate(
            File[] files, XmlDocument template, BatchConfig conf
    ) {
        final ImageFileExportOptions export = conf.export;
        final boolean ignoreResize =
            export.resizeWidth.getValue() == 0 &&
                export.resizeHeight.getValue() == 0;

        // Remember the requested output width and height, because they may get
        // mutated each time a file is processed.  See these methods:
        //     createTemplateSaveOptions()
        //     conformExportOptions()
        //     Engine.write()
        final int exportWidth = export.resizeWidth.getValue();
        final int exportHeight = export.resizeHeight.getValue();

        int n = 0;
        for (final File file : files) {

            if (Canceled)
                break;

            if (Interrupted) {
                synchronized(Thread) {
                    Thread.notifyAll();
                    return;
                }
            }

            try {
                Image.setCachedFile(file);

                logStart(file);
                final Document doc = Application.createDocumentHeadless(file);
                final File outFile;
                final String outName;

                // Enforce the original requested output dimensions, since
                // the ImageExportOptions may have been mutated on a previous
                // iteration.
                export.resizeWidth.setValue(exportWidth);
                export.resizeHeight.setValue(exportHeight);

                if (template != null) {
                    XmlNode root = template.getRoot();

                    doc.applyTemplate(root);

                    SaveOptions save = doc.getSaveOptions();
                    if (save == null) {
                        save = createTemplateSaveOptions(doc, export, ignoreResize);
                    }
                    doc.setSaveOptions(save);

                    ComboFrame frame = (ComboFrame) Dialog.getOwner();
                    DocumentWriter.save(doc, frame, false, Progress);
                    outFile = save.getFile();
                    outName = outFile.getName();
                    DocumentDatabase.addDocumentFile(outFile);
                } else {
                    conformExportOptions(doc, conf, ignoreResize);

                    Engine engine = doc.getEngine();
                    DocumentWriter.export(engine, export, Progress);
                    outFile = export.getExportFile();
                    outName = outFile.getName();
                }

                doc.dispose();

                logEnd(LOCALE.get("BatchLogSavedMessage", outName));

                Image.setFile(outFile);

                Progress.reset();
            }
            catch (XMLException e) {
                logError(LOCALE.get("BatchLogXmlError"), e);
            }
            catch (BadImageFileException e) {
                logError(LOCALE.get("BatchLogBadImageError"), e);
            }
            catch (IOException e) {
                logError(LOCALE.get("BatchLogIOError"), e);
            }
            catch (OutOfMemoryError e) {
                logError(LOCALE.get("BatchLogMemoryError"), e);
            }
            catch (UnknownImageTypeException e) {
                logError(LOCALE.get("BatchLogImageTypeError"), e);
            }
            catch (UnsupportedColorProfileException e) {
                logError(LOCALE.get("BatchLogCameraError"), e);
            }
            catch (ColorProfileException e) {
                logError(LOCALE.get("BatchLogColorError"), e);
            }
            catch (Throwable e) {
                logError(LOCALE.get("BatchLogUnknownError"), e);
                e.printStackTrace();
            }
            updateLabel(++n, files.length);
        }
        synchronized(Thread) {
            Finished = true;
            Thread.notifyAll();
            Button.setText(LOCALE.get("BatchDoneButton"));
        }
    }

    /**
     * Construct SaveOptions for processed images that have never been saved.
     * Save back to the same directory as the original image,
     * with a unique file name, with the given export options, except
     * the resize dimensions, which are set to the document's "natural"
     * dimensions.
     */
    private static SaveOptions createTemplateSaveOptions(
        Document doc, ImageFileExportOptions export, boolean ignoreResize
    ) {
        final ImageMetadata meta = doc.getMetadata();
        File file = meta.getFile();
        final ImageType type = export.getImageType();
        final String ext = type.getExtensions()[0];
        if (type == LZNImageType.INSTANCE) {
            file = ExportNameUtility.setFileExtension(file, ext);
            file = ExportNameUtility.ensureNotExists(file);
            return SaveOptions.createLzn(file);
        }

        final SaveOptions options;
        if (type instanceof TIFFImageType) {
            options = SaveOptions.createSidecarTiff(export);
        }
        else if (type instanceof JPEGImageType) {
            options = SaveOptions.createSidecarJpeg(export);
        }
        else {
            throw new IllegalArgumentException(
                "Can't save to image type \"" + type.getName() + "\""
            );
        }
        final Engine engine = doc.getEngine();
        final Dimension size = engine.getNaturalSize();
        if (ignoreResize) {
            export.resizeWidth.setValue(size.width);
            export.resizeHeight.setValue(size.height);
        }
        file = new File(ExportNameUtility.getBaseName(file) + "_lzn." + ext);
        file = ExportNameUtility.ensureNotExists(file);
        options.setFile(file);

        return options;
    }

    /**
     * Ensure that the given ImageExportOptions agrees with the given
     * TemplateBatchConfigurator about the output folder, the batch name,
     * and the output file type extension, and agrees with the given Document
     * and the configurator about the output image size.
     */
    private static void conformExportOptions(
        Document doc, BatchConfig conf, boolean ignoreResize
    ) {
        final ImageMetadata meta = doc.getMetadata();
        final File file = meta.getFile();
        final String name = file.getName();
        final File directory = conf.directory;
        File outFile = new File(directory, name);

        // Mutate the default file into a conformant name:
        final String outLabel = conf.name;
        final String outName = ExportNameUtility.trimFileExtension(
            outFile.getName()
        );
        final ImageFileExportOptions export = conf.export;

        final String outSuffix = export.getImageType().getExtensions()[0];
        outFile = (outLabel.length() > 0)
                ? new File(directory, outName + outLabel + "." + outSuffix)
                : new File(directory, outName + "." + outSuffix);
        outFile = ExportNameUtility.ensureNotExists(outFile);
        export.setExportFile(outFile);

        if (ignoreResize) {
            final Engine engine = doc.getEngine();
            final Dimension size = engine.getNaturalSize();
            export.resizeWidth.setValue(size.width);
            export.resizeHeight.setValue(size.height);
        }
    }

    private static void logStart(File file) {
        Text.appendStart(file.getName());
    }

    private static void logEnd(String message) {
        Text.appendEnd(message);
    }

    private static void logError(String message, Throwable e) {
        final StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append("--")
              .append(message);
        }
        if (e != null) {
            sb.append(": ")
              .append(e.getClass().getName())
              .append(" ")
              .append(e.getMessage());
        }
        sb.append("\n");
        Text.appendError(sb.toString());
    }

    private static void initLabel(int max) {
        final String text = (max == 1)
                ? LOCALE.get("BatchInitEstimateMessageSingular")
                : LOCALE.get("BatchInitEstimateMessagePlural", Integer.toString(max));
        Label.setText(text);
        Start = System.currentTimeMillis();
    }

    private static void updateLabel(final int count, final int max) {
        final long now = System.currentTimeMillis();
        final long end = Start + max * (now - Start) / count;
        final long remaining = end - now;
        EventQueue.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    int remainingSeconds = (int) (remaining / 1000);
                    int remainingMinutes = remainingSeconds / 60;
                    remainingSeconds -= remainingMinutes * 60;
                    String message = "" + count + " of " + max + " files processed, ";

                    message += (remainingMinutes > 0 ? remainingMinutes + " minutes and " : "") + remainingSeconds + " seconds remaining.";

                    Label.setText(message);
//                    Label.setText(
//                        LOCALE.get(
//                            "BatchEstimateMessage",
//                            Long.toString(count),
//                            Long.toString(max),
//                            Long.toString(remaining / 1000)
//                        )
//                    );
                }
            }
        );
    }
}
