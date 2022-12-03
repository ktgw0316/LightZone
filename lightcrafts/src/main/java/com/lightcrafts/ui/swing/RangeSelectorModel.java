/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.swing;

/**
 * A <code>RangeSelectorModel</code> is the data model for a
 * {@link RangeSelector}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class RangeSelectorModel implements Cloneable {

    /**
     * Constructs a <code>RangeSelectorModel</code>.
     */
    public RangeSelectorModel() {
        // do nothing
    }

    /**
     * Constructs a <code>RangeSelectorModel</code>.
     *
     * @param minimumThumbValue The minimum thumb value.
     * @param maximumThumbValue The maximum thumb value.
     * @param lowerThumbValue The lower thumb value.
     * @param lowerThumbFeatheringValue The lower thumb feathering value.
     * @param upperThumbValue The upper thumb value.
     * @param upperThumbFeatheringValue The upper thumb feathering value.
     * @param minimumTrackValue The minumum track value.
     * @param maximumTrackValue The maximum track value.
     * @param trackValue The track value.
     */
    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public RangeSelectorModel( int minimumThumbValue, int maximumThumbValue,
                               int lowerThumbValue,
                               int lowerThumbFeatheringValue,
                               int upperThumbValue,
                               int upperThumbFeatheringValue,
                               int minimumTrackValue, int maximumTrackValue,
                               int trackValue ) {
        setThumbProperties(
            minimumThumbValue, maximumThumbValue,
            lowerThumbValue, lowerThumbFeatheringValue,
            upperThumbValue, upperThumbFeatheringValue
        );
        setTrackProperties( minimumTrackValue, maximumTrackValue, trackValue );
    }

    /**
     * {@inheritDoc}
     */
    public RangeSelectorModel clone() {
        try {
            return (RangeSelectorModel)super.clone();
        }
        catch ( CloneNotSupportedException e ) {
            throw new IllegalStateException( e );
        }
    }

    /**
     * Gets the lower thumb feathering value.
     *
     * @return Returns said value.
     * @see #getUpperThumbFeatheringValue()
     */
    public int getLowerThumbFeatheringValue() {
        return m_lowerThumbFeatheringValue;
    }

    /**
     * Gets the lower thumb value.
     *
     * @return Returns said value.
     * @see #getUpperThumbValue()
     */
    public int getLowerThumbValue() {
        return m_lowerThumbValue;
    }

    /**
     * Gets the maximum thumb value.
     *
     * @return Returns said value.
     */
    public int getMaximumThumbValue() {
        return m_maximumThumbValue;
    }

    /**
     * Gets the maximum track value.
     *
     * @return Returns said value.
     */
    public int getMaximumTrackValue() {
        return m_maximumTrackValue;
    }

    /**
     * Gets the minimum value.
     *
     * @return Returns said value.
     */
    public int getMinimumThumbValue() {
        return m_minimumThumbValue;
    }

    /**
     * Gets the minimum track value.
     *
     * @return Returns said value.
     */
    public int getMinimumTrackValue() {
        return m_minimumTrackValue;
    }

    /**
     * Gets the track value.
     *
     * @return Returns said value.
     */
    public int getTrackValue() {
        return m_trackValue;
    }

    /**
     * Gets the upper thumb feathering value.
     *
     * @return Returns said value.
     * @see #getLowerThumbFeatheringValue()
     */
    public int getUpperThumbFeatheringValue() {
        return m_upperThumbFeatheringValue;
    }

    /**
     * Gets the upper thumb value.
     *
     * @return Returns said value.
     * @see #getLowerThumbValue()
     */
    public int getUpperThumbValue() {
        return m_upperThumbValue;
    }

    /**
     * Sets the track value.
     *
     * @param newTrackValue The new track value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     */
    public boolean setTrackValue( int newTrackValue ) {
        return setTrackProperties(
            m_minimumTrackValue, m_maximumTrackValue, newTrackValue
        );
    }

    /**
     * Sets the lower thumb feathering value.  The following constraints must
     * always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newLowerThumbFeatheringValue The new lower thumb feathering value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public boolean setLowerFeatheringValue( int newLowerThumbFeatheringValue ) {
        return setThumbProperties(
            m_minimumThumbValue, m_maximumThumbValue,
            m_lowerThumbValue, newLowerThumbFeatheringValue,
            m_upperThumbValue, m_upperThumbFeatheringValue
        );
    }

    /**
     * Sets the lower thumb value.  The following constraints must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newLowerThumbValue The new lower thumb value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public boolean setLowerThumbValue( int newLowerThumbValue ) {
        return setThumbProperties(
            m_minimumThumbValue, m_maximumThumbValue,
            newLowerThumbValue, m_lowerThumbFeatheringValue,
            m_upperThumbValue, m_upperThumbFeatheringValue
        );
    }

    /**
     * Sets the maximum value.  The following constraints must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newMaximumThumbValue The new maximum thumb value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public boolean setMaximumThumbValue( int newMaximumThumbValue ) {
        return setThumbProperties(
            m_minimumThumbValue, newMaximumThumbValue,
            m_lowerThumbValue, m_lowerThumbFeatheringValue,
            m_upperThumbValue, m_upperThumbFeatheringValue
        );
    }

    /**
     * Sets the minimum thumb value.  The following constraints must always
     * hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newMinimumThumbValue The new minimum thumb value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public boolean setMinimumThumbValue( int newMinimumThumbValue ) {
        return setThumbProperties(
            newMinimumThumbValue, m_maximumThumbValue,
            m_lowerThumbValue, m_lowerThumbFeatheringValue,
            m_upperThumbValue, m_upperThumbFeatheringValue
        );
    }

    /**
     * Sets the various properties of the slider.  The following constraints
     * must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param minimumThumbValue The minimum thumb value.
     * @param maximumThumbValue The maximum thumb value.
     * @param lowerThumbValue The lower thumb value.
     * @param lowerThumbFeatheringValue The lower thumb feathering value.
     * @param upperThumbValue The upper thumb value.
     * @param upperThumbFeatheringValue The upper thumb feathering value.
     * @return Returns <code>true</code> only if any of the new values is
     * different from its old value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #setTrackProperties(int,int,int)
     */
    public boolean setThumbProperties( int minimumThumbValue,
                                       int maximumThumbValue,
                                       int lowerThumbValue,
                                       int lowerThumbFeatheringValue,
                                       int upperThumbValue,
                                       int upperThumbFeatheringValue ) {
        if ( minimumThumbValue > maximumThumbValue ||
             lowerThumbFeatheringValue < minimumThumbValue ||
             lowerThumbValue < lowerThumbFeatheringValue ||
             lowerThumbValue > upperThumbValue ||
             upperThumbValue > upperThumbFeatheringValue ||
             upperThumbFeatheringValue > maximumThumbValue )
            throw new IllegalArgumentException();

        final boolean changed =
            minimumThumbValue != m_minimumThumbValue ||
            maximumThumbValue != m_maximumThumbValue ||
            lowerThumbValue != m_lowerThumbValue ||
            upperThumbValue != m_upperThumbValue ||
            lowerThumbFeatheringValue != m_lowerThumbFeatheringValue ||
            upperThumbFeatheringValue != m_upperThumbFeatheringValue;

        if ( changed ) {
            m_minimumThumbValue = minimumThumbValue;
            m_maximumThumbValue = maximumThumbValue;
            m_lowerThumbValue = lowerThumbValue;
            m_upperThumbValue = upperThumbValue;
            m_lowerThumbFeatheringValue = lowerThumbFeatheringValue;
            m_upperThumbFeatheringValue = upperThumbFeatheringValue;
        }
        return changed;
    }

    /**
     * Sets the various properties of the track.  The following constraints
     * must always hold:
     * <blockquote>
     * min &lt;= LTV &lt;= TV &lt;= UTV &lt;= max
     * </blockquote>
     *
     * @param minimumTrackValue The minumum track value.
     * @param maximumTrackValue The maximum track value.
     * @param trackValue The track value.
     * @return Returns <code>true</code> only if any of the new values is
     * different from its old value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #setThumbProperties(int,int,int,int,int,int)
     */
    public boolean setTrackProperties( int minimumTrackValue,
                                       int maximumTrackValue, int trackValue ) {
        if ( minimumTrackValue > maximumTrackValue ||
             trackValue < minimumTrackValue ||
             trackValue > maximumTrackValue )
            throw new IllegalArgumentException();

        final boolean changed =
            minimumTrackValue != m_minimumTrackValue ||
            maximumTrackValue != m_maximumTrackValue ||
            trackValue != m_trackValue;

        if ( changed ) {
            m_minimumTrackValue = minimumTrackValue;
            m_maximumTrackValue = maximumTrackValue;
            m_trackValue = trackValue;
        }
        return changed;
    }

    /**
     * Sets the upper thumb feathering value.  The following constraints must
     * always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     * @param newUpperFeatheringValue The new upper thumb feathering value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public boolean setUpperThumbFeatheringValue( int newUpperFeatheringValue ) {
        return setThumbProperties(
            m_minimumThumbValue, m_maximumThumbValue,
            m_lowerThumbValue, m_lowerThumbFeatheringValue,
            m_upperThumbValue, newUpperFeatheringValue
        );
    }

    /**
     * Sets the upper thumb value.  The following constraints must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     * @param newUpperThumbValue The new upper value.
     * @return Returns <code>true</code> only if the new value is different
     * from the old value.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public boolean setUpperThumbValue( int newUpperThumbValue ) {
        return setThumbProperties(
            m_minimumThumbValue, m_maximumThumbValue,
            m_lowerThumbValue, m_lowerThumbFeatheringValue,
            newUpperThumbValue, m_upperThumbFeatheringValue
        );
    }

    ////////// private ////////////////////////////////////////////////////////

    private int m_minimumThumbValue;
    private int m_maximumThumbValue = 100;
    private int m_lowerThumbValue;
    private int m_lowerThumbFeatheringValue;
    private int m_upperThumbValue = m_maximumThumbValue;
    private int m_upperThumbFeatheringValue = m_upperThumbValue;

    private int m_minimumTrackValue;
    private int m_maximumTrackValue = Integer.MAX_VALUE;
    private int m_trackValue;
}
/* vim:set et sw=4 ts=4: */
