/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.util.List;

/** A GenericOperation is an Operation where the configurable parameters may
 * be inspected at runtime, to allow a UI to be constructed dynamically.
 * This is useful if an Operation should be under user control, but it does
 * not yet have a custom control.
 * <p>
 * Every GenericOperation is configured by setting key-value pairs.  The keys
 * are always Strings, and there are three kinds of values:
 * <ol>
 *   <li>"Slider" values, which are floating point numbers between 0 and 1;</li>
 *   <li>"Checkbox" values, which are booleans;</li>
 *   <li>"Choice" values, which are Strings chosen from an inspected List.</li>
 * </ol>
 */

public interface GenericOperation extends Operation {

    /** Get the keys for slider values.  These Strings will label slider
      * controls in the UI for this Operation.
      * @return A List of Strings.
      */
    List<String> getSliderKeys();

    /** Get the keys for checkbox values.  These Strings will label checkbox
      * controls in the UI for this Operation.
      * @return A List of Strings.
      */
    List<String> getCheckboxKeys();

    /** Get the keys for choice values.  These Strings will label choice
      * controls in the UI for this Operation.
      * @return A List of Strings.
      */
    List<String> getChoiceKeys();

    /** Get the Strings to use as choices for the given choice key.
      * @param key A choice key returned from <code>getChoiceKeys()</code>
      * @return A List of Strings.
      */
    List<String> getChoiceValues(String key);

    /** Set the value for a slider key.
      * @param key A key from <code>getSliderKeys().</code>
      * @param value A number between 0 and 1 inclusive.
      */
    void setSliderValue(String key, double value);

    /** Set the value for a checkbox key.
      * @param key A key from <code>getCheckboxKeys()</code>
      * @param value True or false.
      */
    void setCheckboxValue(String key, boolean value);

    /** Set the value for a choice key.
      * @param key A key from <code>getChoiceKeys()</code>
      * @param value A String from <code>getChoiceValues()</code> with the
      * same key.
      */
    void setChoiceValue(String key, String value);

    /** Each slider key must have an associated SliderConfig to
      * define the corresponding user interface to be generated for it.
      * @param key A key from <code>getSliderKeys()</code>
      * @return A SliderConfig with user interface details, or null if a
      * default slider configuration is OK.
      */
    SliderConfig getSliderConfig(String key);

    /**
      * Get a help topic name, as defined in HelpConstants.
      */
    String getHelpTopic();

    default void accept(GenericOperationVisitor visitor) {
        visitor.visitGenericOperation(this);
    }
}
