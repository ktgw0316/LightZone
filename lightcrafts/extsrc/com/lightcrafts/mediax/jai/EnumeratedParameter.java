/*
 * $RCSfile: EnumeratedParameter.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:08 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.io.Serializable;

/**
 * This class provides a mechanism by which enumerated parameters may be
 * added to subclasses of <code>OperationDescriptorImpl</code> while
 * retaining the ability to perform introspection on the allowable range of
 * values of the enumeration.  An example of an enumerated parameter is the
 * <i>type</i> parameter of the "Transpose" operation which is defined in
 * <code>TransposeDescriptor</code> to accept only the values defined by the
 * <code>FLIP_*</code> and <code>ROTATE_*</code> fields of the descriptor.
 *
 * <p> This class may be used to create enumerated parameters in an
 * <code>OperationDescriptor</code> as follows:
 *
 * <ul>
 *
 * <li> For each constrained-value parameter create a final class extending
 * <code>EnumeratedParameter</code>.  This class should consist of only a
 * package private constructor with a single <code>String</code> parameter
 * called <i>name</i> and a single statement invoking the superclass
 * constructor with <i>name</i> as the parameter.</li>
 *
 * <li> Define the class of the parameter in question to be the
 * subclass of <code>EnumeratedParameter</code> which was created for it in
 * in the previous step.</li>
 *
 * <li> For each possible value of the parameter, define in the
 * <code>OperationDescriptor</code> of the operator a public static final
 * field of type equal to the class defined in the first step.  Each field
 * should be assigned an instance of the subclass defined in the first step.
 * The <i>name</i> and <code>value</code> used for each of these
 * <code>EnumeratedParameter</code> subclass instances should be distinct
 * for each distinct field or an error will occur.</li>
 *
 * </ul>
 *
 * With respect to <code>TransposeDescriptor</code>, the three steps above
 * would be to 1) create a final class <code>TransposeType</code> in the
 * <code>com.lightcrafts.mediax.jai.operator</code> package; 2) define the
 * type of the "type" parameter as <code>TransposeType.class</code>; and
 * 3) define a static final field of class <code>TransposeType</code> for
 * each of the enumerated values with each field being initialized to an
 * instance of <code>TransposeType</code> with <i>name</i> equal to the name
 * of the field and <i>value</i> to its integral (enumerated) value.
 *
 * @see OperationDescriptorImpl
 * @see com.lightcrafts.mediax.jai.operator.TransposeDescriptor
 *
 * @since JAI 1.1
 */
public class EnumeratedParameter implements Serializable {
    private String name;
    private int value;

    /**
     * Constructs an <code>EnumeratedParameter</code> with the indicated name
     * and value.
     */
    public EnumeratedParameter(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the name assigned to this <code>EnumeratedParameter</code>
     * when it was constructed.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value assigned to this <code>EnumeratedParameter</code>
     * when it was constructed.
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns a hash code value for the object.
     */
    public int hashCode() {
	return (getClass().getName() + (new Integer(value))).hashCode();
    }

    /**
     * Returns <code>true</code> if and only if the parameter is an instance
     * of the class on which this method is invoked and has either the same
     * name or the same value.
     */
    public boolean equals(Object o) {
        return o != null &&
            this.getClass().equals(o.getClass()) &&
            (name.equals(((EnumeratedParameter)o).getName()) ||
             value == ((EnumeratedParameter)o).getValue());
    }

    /**
     * Returns a <code>String</code> representation of this
     * <code>EnumeratedParameter</code> as a concatentation of the form
     * <pre>
     * [class name]:[parameter name]=[parameter value]
     * </pre>
     * For example, for an instance of a subclass
     * <code>org.foobar.jai.EnumParam</code> with name "SomeValue" and
     * value "2" the returned <code>String</code> would be
     * <pre>
     * org.foobar.jai.EnumParam:SomeValue=2
     * </pre>
     */
    public String toString() {
        return getClass().getName()+":"+name+"="+String.valueOf(value);
    }
}
