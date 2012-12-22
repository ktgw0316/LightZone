/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx.sheets;

/**
 * An <code>AlertListener</code> listens for events from native code that has
 * displayed a Mac&nbsp;OS&nbsp;X alert &quot;sheet&quot; to the user.  It's
 * based on sample code provided by Apple Computer.
 * <p>
 * @see <a href="http://developer.apple.com/samplecode/JSheets/JSheets.html">JSheets
 * sample code</a>.
 */
public interface AlertListener {

    /**
     * This is called when the user clicks a button.
     *
     * @param buttonClicked The index of the button the user clicked with 0
     * being the right-most button (for a left-to-right language).
     */
    void sheetDone( int buttonClicked );

}
/* vim:set et sw=4 ts=4: */
