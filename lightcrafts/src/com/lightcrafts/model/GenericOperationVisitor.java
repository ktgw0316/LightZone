package com.lightcrafts.model;

public interface GenericOperationVisitor {
    void visitColorPickerDropperOperation(ColorPickerDropperOperation op);

    void visitRawAdjustmentOperation(RawAdjustmentOperation op);

    void visitColorDropperOperation(ColorDropperOperation op);

    void visitColorPickerOperation(ColorPickerOperation op);

    void visitGenericOperation(GenericOperation op);
}
