/* Copyright (C) 2005-2011 Fabio Riccardi */

package printing;

import java.awt.print.*;
import java.awt.*;

public class PrintTest implements Printable {
    PrinterJob printJob;
    PageFormat pageFormat;

    PrintTest() {

    }

    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0)
            return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) g;

        Rectangle clip = g2d.getClipBounds();

        g2d.setColor(Color.orange);
        g2d.drawRect(clip.x+1, clip.y+1, clip.width-2, clip.height-2);

        g2d.setColor(Color.blue);
        g2d.drawRect((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY(), (int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight());

        g2d.setColor(Color.black);
        g2d.drawRect((int) pageFormat.getImageableX() + 3, (int) pageFormat.getImageableY() + 3, (int) pageFormat.getImageableWidth() - 6, (int) pageFormat.getImageableHeight() - 6);

        g2d.setColor(Color.red);
        g2d.fillRect(0, 0, 100, 100);

        g2d.setColor(Color.yellow);
        g2d.fillRect(50, 50, 100, 100);

        dumpPage(pageFormat);

        return PAGE_EXISTS;
    }

    public static void dumpPage(PageFormat pageFormat) {
        System.out.println("page area w:" + pageFormat.getWidth() + ", h: " + pageFormat.getHeight() + ", o: " + pageFormat.getOrientation());

        System.out.println("imageable area x: " +
                           pageFormat.getImageableX() + ", y: " + pageFormat.getImageableY() + ", w: " +
                           pageFormat.getImageableWidth() + ", h: " + pageFormat.getImageableHeight());

        Paper paper = pageFormat.getPaper();

        System.out.println("paper area w:" + paper.getWidth() + ", h: " + paper.getHeight());
        System.out.println("imageable area x: " +
                           paper.getImageableX() + ", y: " + paper.getImageableY() + ", w: " +
                           paper.getImageableWidth() + ", h: " + paper.getImageableHeight());

    }

    public static PageFormat fixPageFormat(PageFormat format) {
        Paper paper = format.getPaper();
        paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight()); //no margins
        format.setPaper(paper);
        format = PrinterJob.getPrinterJob().validatePage(format);
        return format;
    }

    static boolean workaround = true;

    void doPrint() {
        boolean keepGoing = true;
        printJob = PrinterJob.getPrinterJob();
        pageFormat = fixPageFormat(printJob.defaultPage());
        if (workaround) {
            Paper paper = pageFormat.getPaper();
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight()); //no margins
            pageFormat.setPaper(paper);
            pageFormat = printJob.validatePage(pageFormat);
        }
        while (keepGoing) {
            pageFormat = printJob.pageDialog(pageFormat);

            System.out.println("--- Before ---"); dumpPage(pageFormat);

            /*
                The following workaround resets print margin to
                some minimum reasonable amount and "fixes" the
                odd format in landscape mode
            */
            boolean workaround = true;
            if (workaround)
                pageFormat = fixPageFormat(pageFormat);

            System.out.println("--- After ---"); dumpPage(pageFormat);

            printJob.setPrintable(this, pageFormat);
            if (printJob.printDialog())
                try {
                    printJob.print();
                } catch (PrinterException pe) {
                    System.out.println("Error printing: " + pe);
                }
            else
                break;
        }

        System.exit(0);
    }

    public static void main(String[] args) {
        System.out.println("printing test");

        PrintTest pt = new PrintTest();
        pt.doPrint();
    }
}
