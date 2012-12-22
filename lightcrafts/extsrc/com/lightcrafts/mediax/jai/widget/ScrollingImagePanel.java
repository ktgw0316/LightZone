/*
 * $RCSfile: ScrollingImagePanel.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:59 $
 * $State: Exp $
 */ 
package com.lightcrafts.mediax.jai.widget;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.RenderedImage;
import java.util.Vector;

import java.awt.ScrollPane;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * An extension of java.awt.Panel that contains an ImageCanvas and
 * vertical and horizontal scrollbars.  The origin of the ImageCanvas
 * is set according to the value of the scrollbars.  Additionally, the
 * origin may be changed by dragging the mouse.  The window cursor
 * will be changed to Cursor.MOVE_CURSOR for the duration of the
 * drag.
 *
 * <p> Due to the limitations of BufferedImage, only TYPE_BYTE of band
 * 1, 2, 3, 4, and TYPE_USHORT of band 1, 2, 3 images can be displayed
 * using this widget.
 *
 *
 * <p>
 * This class has been deprecated.  The source
 * code has been moved to the samples/widget
 * directory.  These widgets are no longer
 * supported.
 *
 * @deprecated as of JAI 1.1
 */ 

public class ScrollingImagePanel extends ScrollPane
                                 implements AdjustmentListener,
                                            ComponentListener,
                                            MouseListener,
                                            MouseMotionListener {
     
  /** The ImageCanvas we are controlling. */
  protected ImageCanvas ic;
  /** The RenderedImage displayed by the ImageCanvas. */
  protected RenderedImage im;
  /** The width of the panel. */
  protected int panelWidth;
  /** The height of the panel. */
  protected int panelHeight; 
  /** Vector of ViewportListeners. */	
  protected Vector viewportListeners = new Vector();

  /** 
   * Constructs a ScrollingImagePanel of a given size for a 
   * given RenderedImage.
   */
  public ScrollingImagePanel(RenderedImage im, int width, int height) {
    super();
    this.im = im;
    this.panelWidth  = width; 
    this.panelHeight = height;
    
    ic = new ImageCanvas(im);

    getHAdjustable().addAdjustmentListener(this);
    getVAdjustable().addAdjustmentListener(this);
    
    super.setSize(width, height);
    addComponentListener(this);
    add("Center",ic); 
  }

  /** Adds the specified ViewportListener to the panel */    
  public void addViewportListener(ViewportListener l) {
    viewportListeners.addElement(l);
    l.setViewport(getXOrigin(), getYOrigin(),
		  panelWidth, panelHeight);
  }
      
  /** Removes the specified ViewportListener  */
  public void removeViewportListener(ViewportListener l) {
    viewportListeners.removeElement(l);
  }
  
  private void notifyViewportListeners(int x, int y, int w, int h) {
    int i;
    int numListeners = viewportListeners.size();
    for (i = 0; i < numListeners; i++) {
      ViewportListener l =
	(ViewportListener)(viewportListeners.elementAt(i));
      l.setViewport(x, y, w, h);
    }
  }

  /**
   *  Returns the image canvas.  Allows mouse listeners
   *  to be used on the image canvas.
   *
   * @since JAI 1.1
   */
  public ImageCanvas getImageCanvas() {
      return ic;
  }
	
  /** Returns the XOrigin of the image */
  public int getXOrigin() {
         return ic.getXOrigin();
    }
    
  /** Returns the YOrigin of the image */
  public int getYOrigin() {
    return ic.getYOrigin();
  }
	
  /**
   * Sets the image origin to a given (x, y) position.
   * The scrollbars are updated appropriately.
   */
  public void setOrigin(int x, int y) {
    ic.setOrigin(x, y);
    notifyViewportListeners(x, y, panelWidth, panelHeight);
  }

  /**
   * Set the center of the image to the given coordinates
   * of the scroll window.
   */
  public synchronized void setCenter(int x, int y) {

    // scrollbar position
    int sx = 0;
    int sy = 0;

    // bounds
    int iw = im.getWidth();
    int ih = im.getHeight();
    int vw = getViewportSize().width;
    int vh = getViewportSize().height;

    // scrollbar lengths (proportional indicator)
    int fx = getHAdjustable().getBlockIncrement();
    int fy = getVAdjustable().getBlockIncrement();

    if ( x < (vw - iw/2) ) {
        sx = 0;
    } else if ( x > iw/2 ) {
        sx = iw - vw;
    } else {
        sx = x + (iw-vw-fx)/2;
    }

    if ( y < (vh - ih/2) ) {
        sy = 0;
    } else if ( y > ih/2 ) {
        sy = ih - vh;
    } else {
        sy = y + (ih-vh-fy)/2;
    }

    getHAdjustable().setValue(sx);
    getVAdjustable().setValue(sy);

    notifyViewportListeners(getXOrigin(), getYOrigin(), panelWidth, panelHeight);
  }

  /** Sets the panel to display the specified image */  
  public void set(RenderedImage im) {
    this.im = im;
    ic.set(im);
  }

  /** Returns the X co-ordinate of the image center. */  
  public int getXCenter() {
    return getXOrigin() + panelWidth/2;
  }
   
  /** Returns the Y co-ordinate of the image center. */
  public int getYCenter() {
    return getYOrigin() + panelHeight/2;
  }
     
  /** Called by the AWT when instantiating the component. */
  public Dimension getPreferredSize() {
    return new Dimension(panelWidth, panelHeight);
  }

  /** Called by the AWT during instantiation and
   * when events such as resize occur. 
   */   
  public void setBounds(int x, int y, int width, int height) {

    // must set this first
    super.setBounds(x, y, width, height);

    int vpw = getViewportSize().width;
    int vph = getViewportSize().height;
    int imw = im.getWidth();
    int imh = im.getHeight();

    if ( vpw >= imw && vph >= imh ) {
        ic.setBounds(x, y, width, height); 
    } else {
        //  BUG
        //  This fixes bad image positions during resize
        //  but breaks tiles (causes them all to load)
        //  Somehow the graphics context clip area gets
        //  changed in the ImageCanvas update(g) call.
        //  This occurs when the image size is greater
        //  than the viewport when the display first
        //  comes up.  Removing the repaint in the
        //  ImageCanvas fixes this, but breaks ImageCanvas.
        //  Should have a new ImageCanvas call that sets
        //  the origin without a repaint.

        /* causes all tiles to be loaded, breaks scrolling, fixes resize */
        //setOrigin(0, 0);

        /* causes image to shift incorrectly on resize event */
        ic.setBounds(x, y, vpw, vph);
    }

    this.panelWidth  = width;
    this.panelHeight = height;
  }
      
  /** Called by the AWT when either scrollbar changes. */
  public void adjustmentValueChanged(AdjustmentEvent e) {
  }
    
      /// ComponentListener Interface     

  /** Called when the ImagePanel is resized */
  public void  componentResized(ComponentEvent e) {
    notifyViewportListeners(getXOrigin(), getYOrigin(), panelWidth, panelHeight);
  }
   
  /** Ignored   */ 
  public void  componentHidden(ComponentEvent e) {}
	
  /** Ignored   */ 
  public void  componentMoved(ComponentEvent e) {}
      
  /** Ignored   */ 
  public void  componentShown(ComponentEvent e) {}
 
   
    
    //
    // Mouse drag interface
    //

  /** The initial Point of a mouse drag. */
  protected Point moveSource;
  /** True if we are in the middle of a mouse drag. */
  protected boolean beingDragged = false;
  /** A place to save the cursor. */
  protected Cursor defaultCursor =  null;
	
  /** Called at the beginning of a mouse drag. */
  private synchronized void startDrag(Point p) {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    beingDragged = true;
    moveSource = p;
  }

  /** Called for each point of a mouse drag. */
  protected synchronized void updateDrag(Point moveTarget) {
    if (beingDragged) {
      int dx = moveSource.x - moveTarget.x;
      int dy = moveSource.y - moveTarget.y;
      moveSource = moveTarget;
      
      int x = getHAdjustable().getValue() + dx;
      int y = getVAdjustable().getValue() + dy;
      setOrigin(x, y);
    }
  }
    
  /** Called at the end of a mouse drag. */
  private synchronized void endDrag() {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    beingDragged = false;
  }
    
  /** Called by the AWT when the mouse button is pressed. */
  public void mousePressed(MouseEvent me) {
    startDrag(me.getPoint());
  }
    
  /** Called by the AWT as the mouse is dragged. */
  public void mouseDragged(MouseEvent me) {
    updateDrag(me.getPoint());
  }
    
  /** Called by the AWT when the mouse button is released. */
  public void mouseReleased(MouseEvent me) {
    endDrag();
  }
    
  /** Called by the AWT when the mouse leaves the component. */
  public void mouseExited(MouseEvent me) {
    endDrag();
  }
    
  /** Ignored. */
  public void mouseClicked(MouseEvent me) {}

  /** Ignored. */
  public void mouseMoved(MouseEvent me) {}

  /** Ignored. */
  public void mouseEntered(MouseEvent me) {}
}
