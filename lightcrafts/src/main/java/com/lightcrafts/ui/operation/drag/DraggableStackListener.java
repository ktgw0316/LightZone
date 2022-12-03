/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

/** A listener can hear when two Components in a DraggableStack have been
  * swapped by dragging, and also hear when dragging starts and stops.
  */
public interface DraggableStackListener {

    /** This method is called from DraggableStack when a drag gesture starts.
     */
    void dragStarted();

    /** This method is called during a drag gesture when the DraggableStack
      * has determined that the current cursor location implies an interchange
      * of the order of two adjacent stack Components.
      * @param index The lower of the two indices of the stack Components
      * that have been swapped.
      */
    void swapped(int index);

    /** This method is called from DraggableStack when a drag gesture ends.
      */
    void dragStopped();
}
