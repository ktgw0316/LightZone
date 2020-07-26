/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SpotOperation;
import com.lightcrafts.ui.editor.EditorMode;

import javax.media.jai.PlanarImage;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_SPOT;

public class SpotOperationImpl extends BlendedOperation implements SpotOperation {

    public SpotOperationImpl(Rendering rendering) {
        super(rendering, type);
        setHelpTopic(HELP_TOOL_SPOT);
    }

    @Override
    public boolean neutralDefault() {
        return true;
    }

    public void setRegionInverted(boolean inverted) {
        // Inverted regions have no meaning for the Spot Tool
        // super.setRegionInverted(inverted);
    }

    static final OperationType type = new OperationTypeImpl("Spot");

    class Cloner extends BlendedTransform {
        Cloner(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            if (getRegion() != null)
                return CloneOperationImpl.buildCloner(getRegion(), rendering, back);
            else
                return back;
        }
    }

    @Override
    public EditorMode getPreferredMode() {
        return EditorMode.REGION;
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new Cloner(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
