/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.model.ColorSelection;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.RGBColorSelection;
import com.lightcrafts.model.RGBColorSelectionPreset;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.ui.mode.DropperMode;
import com.lightcrafts.ui.swing.ColorSwatch;
import com.lightcrafts.ui.swing.RangeSelector;
import com.lightcrafts.ui.swing.RangeSelectorZoneTrack;
import com.lightcrafts.ui.toolkit.DropperButton;
import com.lightcrafts.ui.toolkit.LCSliderUI;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.AbstractUndoableEdit;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeSupport;

import static com.lightcrafts.ui.LightZoneSkin.Colors.LZOrange;
import static com.lightcrafts.ui.operation.Locale.LOCALE;

/**
 * A <code>ColorSelectionControls</code> is-a {@link Box} that contains all
 * the controls to do color-based selection.
 */
final class ColorSelectionControls extends Box {
    private final PropertyChangeSupport pcs;

    public static final String COLOR_SELECTION = "Color Selection";

    private static final LCMS.Transform ts = new LCMS.Transform(
        new LCMS.Profile( JAIContext.linearProfile ), LCMS.TYPE_RGB_8,
        new LCMS.Profile( JAIContext.systemProfile ), LCMS.TYPE_RGB_8,
        LCMS.INTENT_PERCEPTUAL, 0
    );

    public static JRadioButton getSelection( ButtonGroup group ) {
        for ( final var e = group.getElements(); e.hasMoreElements(); ) {
            final JRadioButton b = (JRadioButton)e.nextElement();
            if ( b.getModel() == group.getSelection() ) {
                return b;
            }
        }
        return null;
    }

    private final class ColorPresets extends Box {
        private final class ColorButton extends JRadioButton {
            final RGBColorSelectionPreset m_preset;

            ColorButton( RGBColorSelectionPreset preset ) {
                m_preset = preset;
                setFocusable( false );

                if (preset.equals(RGBColorSelectionPreset.AllColors)) {
                    setText( LOCALE.get( "AllLabel" ) );
                    setSelected(true);
                } else {
                    final var cs = new RGBColorSelection(preset, false);
                    final byte[] systemColor = new byte[3];
                    ts.doTransform(
                            new byte[]{
                                    (byte)(0xff * cs.red),
                                    (byte)(0xff * cs.green),
                                    (byte)(0xff * cs.blue)
                            },
                            systemColor
                    );
                    final var color = new Color(
                            0xff & systemColor[0],
                            0xff & systemColor[1],
                            0xff & systemColor[2]);
                    setIcon(new DefaultIcon(color, false, false));
                    setRolloverIcon(new DefaultIcon(color, true, false));
                    setSelectedIcon(new DefaultIcon(color, false, true));
                    setRolloverSelectedIcon(new DefaultIcon(color, true, true));
                }
                setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 3));
            }

            private class DefaultIcon implements Icon {

                private final boolean rollover;
                private final boolean selected;
                private final Color color;

                public DefaultIcon(Color color, boolean rollover, boolean selected) {
                    this.color = color;
                    this.rollover = rollover;
                    this.selected = selected;
                }

                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    final var oldColor = g.getColor();
                    g.setColor(color);
                    g.fillOval(x, y, getIconWidth(), getIconHeight());
                    if (rollover) {
                        g.setColor(Color.LIGHT_GRAY);
                        g.drawOval(x, y, getIconWidth(), getIconHeight());
                    }
                    if (selected) {
                        g.setColor(LZOrange);
                        g.fillOval(x + 4, y + 4,
                                getIconWidth() - 8, getIconHeight() - 8);
                    }
                    g.setColor(oldColor);
                }

                @Override
                public int getIconWidth() {
                    return 14;
                }

                @Override
                public int getIconHeight() {
                    return 14;
                }
            }
        }

        ButtonGroup group = new ButtonGroup();

        ColorPresets() {
            super(BoxLayout.X_AXIS);

            for ( final RGBColorSelectionPreset p : RGBColorSelectionPreset.values() ) {
                if (!p.equals(RGBColorSelectionPreset.SampledColors)) {
                    final ColorButton button = new ColorButton(p);
                    group.add(button);
                    add(button);
                    button.addItemListener(ie -> {
                        if ( ie.getStateChange() == ItemEvent.SELECTED ) {
                            final ColorButton b =
                                    (ColorButton)ie.getItem();
                            selectPreset( b.m_preset );
                        }
                    });
                }
            }

            add(Box.createHorizontalGlue());
        }

        RGBColorSelectionPreset getSelectedItem() {
            final ColorButton selection = (ColorButton) getSelection(group);
            if (selection == null)
                return RGBColorSelectionPreset.SampledColors;
            return selection.m_preset;
        }

        void setSelectedItem( RGBColorSelectionPreset p ) {
            if ( !p.equals( RGBColorSelectionPreset.SampledColors ) ) {
                for (final var e = group.getElements(); e.hasMoreElements(); ) {
                    final ColorButton b = (ColorButton)e.nextElement();
                    if ( b.m_preset.equals( p ) ) {
                        b.setSelected( true );
                        break;
                    }
                }
            } else {
                final ColorButton selection = (ColorButton)getSelection( group );
                if ( selection != null ) {
                    group.remove( selection );
                    selection.setSelected( false );
                    group.add( selection );
                }
            }
        }
    }

    ColorSelectionControls( OpControl control, PropertyChangeSupport pcs ) {
        super(BoxLayout.X_AXIS);

        this.pcs = pcs;

        m_op = control.getOperation();
        m_undoSupport = control.undoSupport;

        m_hueEnabled = new JCheckBox();
        m_hueEnabled.setFocusable( false );
        m_hueEnabled.setSelected( true );
        m_hueEnabled.setToolTipText(
            LOCALE.get( "HueDisableToolTip" )
        );
        m_hueEnabled.addItemListener(
            new EnablerListener(
                LOCALE.get( "HueDisableToolTip" ),
                LOCALE.get( "HueEnableToolTip" )
            )
        );
        m_luminosityEnabled = new JCheckBox();
        m_luminosityEnabled.setFocusable( false );
        m_luminosityEnabled.setSelected( true );
        m_luminosityEnabled.setToolTipText(
            LOCALE.get( "BrightnessDisableToolTip" )
        );
        m_luminosityEnabled.addItemListener(
            new EnablerListener(
                LOCALE.get( "BrightnessDisableToolTip" ),
                LOCALE.get( "BrightnessEnableToolTip" )
            )
        );

        final LocalListener localListener = new LocalListener();

        m_colorSwatch = new ColorSwatch( COLOR_SWATCH_DEFAULT );
        final JLabel colorRadiusLabel =
            new JLabel( LOCALE.get( "ColorRangeLabel" ) + ':' );
        colorRadiusLabel.setBackground( OpControl.Background );
        colorRadiusLabel.setFocusable( false );
        colorRadiusLabel.setFont(LightZoneSkin.LightZoneFontSet.SmallFont);
        m_colorRangeSlider = new JSlider(
            SLIDER_RADIUS_MIN, SLIDER_RADIUS_MAX,
            COLOR_RADIUS_DEFAULT
        );
        m_colorRangeSlider.setBackground( OpControl.Background );
        m_colorRangeSlider.setFocusable( false );
        m_colorRangeSlider.setUI( new LCSliderUI( m_colorRangeSlider ) );
        m_colorRangeSlider.setToolTipText( LOCALE.get( "ColorRangeToolTip" ) );
        m_colorRangeSlider.addChangeListener( localListener );
        m_colorRangeSlider.addMouseListener( localListener );

        m_colorPresets = new ColorPresets();

        m_luminositySelector = new RangeSelector(
            SELECTOR_LUMINOSITY_MIN, SELECTOR_LUMINOSITY_MAX
        );
        m_luminositySelector.setTrack( new RangeSelectorZoneTrack() );
        m_luminositySelector.addChangeListener( localListener );
        m_luminositySelector.addMouseListener( localListener );
        m_luminositySelector.setToolTipText(
            LOCALE.get( "BrightnessSelectorToolTip" )
        );

        initDropper( control );

        m_invertSelection = new JCheckBox(
            LOCALE.get( "InvertColorSelectionLabel" )
        );
        m_invertSelection.setFont(LightZoneSkin.LightZoneFontSet.SmallFont);
        m_invertSelection.setFocusable( false );
        m_invertSelection.setToolTipText(
            LOCALE.get( "InvertColorSelectionEnableToolTip" )
        );
        m_invertSelection.addItemListener(
            new EnablerListener(
                LOCALE.get( "InvertColorSelectionDisableToolTip" ),
                LOCALE.get( "InvertColorSelectionEnableToolTip" ),
                LOCALE.get( "InvertColorSelectionEditName" ),
                LOCALE.get( "NormalColorSelectionEditName" )
            )
        );

        final ResetColorSelectionButton resetButton =
            new ResetColorSelectionButton(ae -> resetColorSelection());

        setBackground( LightZoneSkin.Colors.ToolPanesBackground );

        ////////// Color controls /////////////////////////////////////////////

        final JPanel presets = new JPanel();
        presets.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        presets.setLayout(
            new BoxLayout( presets, BoxLayout.X_AXIS )
        );
        presets.add( Box.createHorizontalStrut( 10 ) );
        presets.add( m_colorPresets );
        presets.add( Box.createHorizontalGlue() );

        final JPanel radius = new JPanel();
        radius.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        radius.setLayout( new BoxLayout( radius, BoxLayout.X_AXIS ) );
        radius.add( Box.createHorizontalStrut( 5 ) );
        radius.add( colorRadiusLabel );
        radius.add( m_colorRangeSlider );

        final JPanel presetsAndRadius = new JPanel();
        presetsAndRadius.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        presetsAndRadius.setLayout(
            new BoxLayout( presetsAndRadius, BoxLayout.Y_AXIS )
        );
        presetsAndRadius.add( Box.createVerticalStrut( 10 ) );
        presetsAndRadius.add( presets );
        presetsAndRadius.add( radius );

        final JPanel colorControls = new JPanel();
        colorControls.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        colorControls.setLayout(
            new BoxLayout( colorControls, BoxLayout.X_AXIS )
        );
        colorControls.add( m_hueEnabled );
        colorControls.add( m_colorSwatch );
        colorControls.add( presetsAndRadius );

        ////////// Luminosity controls ////////////////////////////////////////

        final JPanel luminosityControls = new JPanel();
        luminosityControls.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        luminosityControls.setLayout(
            new BoxLayout( luminosityControls, BoxLayout.X_AXIS )
        );
        luminosityControls.add( m_luminosityEnabled );
        luminosityControls.add( m_luminositySelector );

        ////////// Color & Luminosity /////////////////////////////////////////

        final JPanel colorAndLuminosity = new JPanel();
        colorAndLuminosity.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        colorAndLuminosity.setLayout(
            new BoxLayout( colorAndLuminosity, BoxLayout.Y_AXIS )
        );
        colorAndLuminosity.add( colorControls );
        colorAndLuminosity.add( luminosityControls );

        ////////// Reset & Dropper ////////////////////////////////////////////

        final JPanel resetAndDropper = new JPanel();
        resetAndDropper.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        resetAndDropper.setLayout(
            new BoxLayout( resetAndDropper, BoxLayout.X_AXIS )
        );
        resetAndDropper.add( m_dropperButton );
        resetAndDropper.add( resetButton );

        ////////// Invert, Reset & Dropper ////////////////////////////////////

        final JPanel invertEtc = new JPanel();
        invertEtc.setBackground( LightZoneSkin.Colors.ToolPanesBackground );
        invertEtc.setLayout( new BoxLayout( invertEtc, BoxLayout.Y_AXIS ) );
        invertEtc.add( Box.createVerticalGlue() );
        invertEtc.add( resetAndDropper );
        invertEtc.add( Box.createVerticalStrut(8) );
        invertEtc.add( m_invertSelection );
        m_invertSelection.setAlignmentX( Component.LEFT_ALIGNMENT );
        resetAndDropper.setAlignmentX( Component.LEFT_ALIGNMENT );

        ////////// This component itself //////////////////////////////////////

        add( invertEtc );
        add( Box.createHorizontalStrut(4) );
        add( colorAndLuminosity );
        add( Box.createHorizontalGlue() );

        m_currentEdit = new ColorSelectionEdit();
    }

    void operationChanged( Operation op ) {
        m_op = op;
    }

    void save( XmlNode node ) {
        final RGBColorSelection cs = m_op.getColorSelection();

        final XmlNode colorNode = node.addChild( ColorSelectionKey );

        colorNode.setAttribute(
            HueRedKey, Float.toString( cs.red )
        );
        colorNode.setAttribute(
            HueGreenKey, Float.toString( cs.green )
        );
        colorNode.setAttribute(
            HueBlueKey, Float.toString( cs.blue )
        );
        colorNode.setAttribute(
            HueRadiusKey, Float.toString( cs.radius )
        );
        colorNode.setAttribute(
            HueEnabledKey, Boolean.toString( cs.isColorEnabled )
        );
        colorNode.setAttribute(
            LuminosityLowerKey,
            Float.toString( cs.luminosityLower )
        );
        colorNode.setAttribute(
            LuminosityLowerFeatherKey,
            Float.toString( cs.luminosityLowerFeather )
        );
        colorNode.setAttribute(
            LuminosityUpperKey,
            Float.toString( cs.luminosityUpper )
        );
        colorNode.setAttribute(
            LuminosityUpperFeatherKey,
            Float.toString( cs.luminosityUpperFeather )
        );
        colorNode.setAttribute(
            LuminosityEnabledKey,
            Boolean.toString( cs.isLuminosityEnabled )
        );
        colorNode.setAttribute(
            InvertedKey,
            Boolean.toString( cs.isInverted )
        );
    }

    void restore( XmlNode node ) throws XMLException {
        if (!node.hasChild(ColorSelectionKey)) {
            return;
        }
        node = node.getChild( ColorSelectionKey );
        try {
            final float red = Float.parseFloat(
                node.getAttribute( HueRedKey )
            );
            final float green = Float.parseFloat(
                node.getAttribute( HueGreenKey )
            );
            final float blue = Float.parseFloat(
                node.getAttribute( HueBlueKey )
            );
            final float radius = Float.parseFloat(
                node.getAttribute( HueRadiusKey )
            );
            final boolean isHueEnabled = Boolean.parseBoolean(
                node.getAttribute( HueEnabledKey )
            );

            final float blv = Float.parseFloat(
                node.getAttribute( LuminosityLowerKey )
            );
            final float blfv = Float.parseFloat(
                node.getAttribute( LuminosityLowerFeatherKey )
            );
            final float buv = Float.parseFloat(
                node.getAttribute( LuminosityUpperKey )
            );
            final float bufv = Float.parseFloat(
                node.getAttribute( LuminosityUpperFeatherKey )
            );
            final boolean isLuminosityEnabled = Boolean.parseBoolean(
                node.getAttribute( LuminosityEnabledKey )
            );

            final boolean isInverted = Boolean.parseBoolean(
                node.getAttribute( InvertedKey )
            );

            final RGBColorSelection cs = new RGBColorSelection(
                red, green, blue, radius, blv, blfv, buv, bufv,
                isInverted, isHueEnabled, isLuminosityEnabled
            );
            colorSelectionToControls( cs, false );
            m_op.setColorSelection( cs );
            m_currentEdit = new ColorSelectionEdit();
        }
        catch ( IllegalArgumentException e ) {
            throw new XMLException( e );
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    private class ColorSelectionEdit extends AbstractUndoableEdit {

        ColorSelectionEdit() {
            m_beforeHueModel = controlsToColorSelection();
        }

        void end( String name ) {
            m_name = name;
            m_afterHueModel = controlsToColorSelection();
        }

        @Override
        public String getPresentationName() {
            return m_name;
        }

        @Override
        public void undo() {
            super.undo();
            colorSelectionToControls( m_beforeHueModel, true );
            m_op.setColorSelection( m_beforeHueModel );
            m_currentEdit = new ColorSelectionEdit();
        }

        @Override
        public void redo() {
            super.redo();
            colorSelectionToControls( m_afterHueModel, true );
            m_op.setColorSelection( m_afterHueModel );
        }

        private RGBColorSelection m_afterHueModel;
        private final RGBColorSelection m_beforeHueModel;
        private String m_name = "";
    }

    // Handle ColorSelection updates and undo for events coming from the
    // enable/disable checkboxes.
    private final class EnablerListener implements ItemListener {

        @Override
        public void itemStateChanged( ItemEvent ie ) {
            if (m_isUpdatingControls) {
                return;
            }
            m_op.setColorSelection( controlsToColorSelection() );
            final JComponent comp = (JComponent)ie.getSource();
            if ( ie.getStateChange() == ItemEvent.SELECTED ) {
                postEdit( m_enabledEditName );
                comp.setToolTipText( m_selectedTip );
            } else {
                postEdit( m_disabledEditName );
                comp.setToolTipText( m_unselectedTip );
            }
        }

        EnablerListener( String selectedTip, String unselectedTip ) {
            this(
                selectedTip, unselectedTip,
                LOCALE.get( "ColorSelectorEnabledEditName" ),
                LOCALE.get( "ColorSelectorDisabledEditName" )
            );
        }

        EnablerListener( String selectedTip, String unselectedTip,
                         String enabledEditName, String disabledEditName ) {
            m_enabledEditName = enabledEditName;
            m_disabledEditName = disabledEditName;
            m_selectedTip = selectedTip;
            m_unselectedTip = unselectedTip;
        }

        private final String m_enabledEditName, m_disabledEditName;
        private final String m_selectedTip, m_unselectedTip;
    }

    /**
     * This is listener for both the color feathering radius slider and the
     * luminosity selector.
     */
    private final class LocalListener
        extends MouseAdapter implements ChangeListener
    {
        @Override
        public void mousePressed( MouseEvent me ) {
            m_op.changeBatchStarted();
        }

        @Override
        public void mouseReleased( MouseEvent me ) {
            m_op.changeBatchEnded();
            postEdit( LOCALE.get( "ColorSelectorEditName" ) );
        }

        @Override
        public void stateChanged( ChangeEvent ce ) {
            if (m_isUpdatingControls) {
                return;
            }
            final RGBColorSelection cs = controlsToColorSelection();
            m_lowerLuminosityFeather = cs.luminosityLowerFeather;
            m_upperLuminosityFeather = cs.luminosityUpperFeather;
            m_op.setColorSelection( cs );
            m_prevCS = cs;
        }
    }

    /**
     * Sets the values of the color selection controls from the given
     * {@link ColorSelection}.
     *
     * @param cs The {@link ColorSelection}.
     * @param force If <code>true</code>, force updating of controls.
     * (This is used during undo/redo.)
     * @see #controlsToColorSelection()
     */
    private void colorSelectionToControls( RGBColorSelection cs,
                                           boolean force ) {
        m_isUpdatingControls = true;
        try {
            if ( m_hueEnabled.isSelected() && cs.isColorEnabled || force ) {
                m_colorSwatch.setColor( cs.toColor() );
                final int radius;
                if ( cs.getPreset() == RGBColorSelectionPreset.AllColors )
                    radius = COLOR_RADIUS_DEFAULT;
                else
                    radius = (int)(cs.radius * SLIDER_RADIUS_MAX);
                m_colorRangeSlider.setValue( radius );
                m_hueEnabled.setSelected( cs.isColorEnabled );
            }

            if ( m_luminosityEnabled.isSelected() && cs.isLuminosityEnabled ||
                 force ) {
                final float cblv = cs.luminosityLower;
                final float cblfv = cs.luminosityLowerFeather;
                final float cbuv = cs.luminosityUpper;
                final float cbufv = cs.luminosityUpperFeather;
                final int bMin = m_luminositySelector.getMinimumThumbValue();
                final int bMax = m_luminositySelector.getMaximumThumbValue();
                final int bWidth = bMax - bMin;

                m_luminositySelector.setProperties(
                    bMin, bMax,
                    (int)(cblv * bWidth), (int)((cblv - cblfv) * bWidth),
                    (int)(cbuv * bWidth), (int)((cbuv + cbufv) * bWidth),
                    m_luminositySelector.getMinimumTrackValue(),
                    m_luminositySelector.getMaximumTrackValue(),
                    m_luminositySelector.getTrackValue(),
                    m_luminositySelector.getTrackValueWraps()
                );
                m_luminosityEnabled.setSelected( cs.isLuminosityEnabled );
            }

            m_invertSelection.setSelected( cs.isInverted );

            m_skipSelectPresetCode = true;
            m_colorPresets.setSelectedItem( cs.getPreset() );
            m_skipSelectPresetCode = false;

            if (cs.isAllSelected())
                pcs.firePropertyChange(
                        COLOR_SELECTION, Boolean.TRUE, Boolean.FALSE
                );
            else
                pcs.firePropertyChange(
                        COLOR_SELECTION, Boolean.FALSE, Boolean.TRUE
                );
        }
        finally {
            m_isUpdatingControls = false;
        }
    }

    /**
     * Converts the current values of the color selection controls into a
     * {@link ColorSelection} that is used by the imaging engine.
     *
     * @return Returns a {@link ColorSelection} that represents the current
     * values of the color selection controls.
     * @see #colorSelectionToControls(RGBColorSelection,boolean)
     */
    private RGBColorSelection controlsToColorSelection() {
        final Color c = m_colorSwatch.getColor();

        final int hMin = m_colorRangeSlider.getMinimum();
        final int hMax = m_colorRangeSlider.getMaximum();
        final float hWidth = hMax - hMin;
        final float radius = m_colorPresets.getSelectedItem() == RGBColorSelectionPreset.AllColors ? -1 : m_colorRangeSlider.getValue() / hWidth;

        final int lMin = m_luminositySelector.getMinimumThumbValue();
        final int lMax = m_luminositySelector.getMaximumThumbValue();
        final float lWidth = lMax - lMin;
        final int llv = m_luminositySelector.getLowerThumbValue();
        final int llfv = m_luminositySelector.getLowerThumbFeatheringValue();
        final int luv = m_luminositySelector.getUpperThumbValue();
        final int lufv = m_luminositySelector.getUpperThumbFeatheringValue();

        final float cllv = llv / lWidth;
        final float cllfv = (llv - llfv) / lWidth;
        final float cluv = luv / lWidth;
        final float clufv = (lufv - luv) / lWidth;

        final RGBColorSelection cs = new RGBColorSelection(
            c.getRed() / 255F, c.getGreen() / 255F, c.getBlue() / 255F,
            radius,
            cllv, cllfv, cluv, clufv,
            m_invertSelection.isSelected(),
            m_hueEnabled.isSelected(), m_luminosityEnabled.isSelected()
        );

        if ( cs.isAllSelected() )
            pcs.firePropertyChange(
                COLOR_SELECTION, Boolean.TRUE, Boolean.FALSE
            );
        else
            pcs.firePropertyChange(
                COLOR_SELECTION, Boolean.FALSE, Boolean.TRUE
            );

        return cs;
    }

    public ComboFrame getComboFrame() {
        return (ComboFrame)SwingUtilities.getAncestorOfClass(
            ComboFrame.class, this
        );
    }

    private void initDropper( final OpControl control ) {
        m_dropperButton = new DropperButton();
        m_dropperButton.setToolTips(
            ColorSelectStartToolTip, ColorSelectEndToolTip
        );
        m_dropperButton.addItemListener(ie -> {
            if ( ie.getStateChange() == ItemEvent.SELECTED ) {
                getComboFrame().getEditor().setMode( EditorMode.ARROW );
                control.notifyListenersEnterMode( m_dropperMode );
            } else if ( !m_isDropperModeCancelling )
                control.notifyListenersExitMode( m_dropperMode );
        });

        m_dropperMode = new DropperMode( control );
        m_dropperMode.addListener(
            new DropperMode.Listener() {
                @Override
                public void pointSelected( Point2D p ) {
                    selectColorAt( p );
                }

                @Override
                public void modeCancelled() {
                    // Reset the toggle button, without firing notifications:
                    m_isDropperModeCancelling = true;
                    m_dropperButton.setSelected( false );
                    m_isDropperModeCancelling = false;
                }
            }
        );
    }

    private RGBColorSelection mergeColorSelections( RGBColorSelection cs ) {
        if ( cs.getPreset() != RGBColorSelectionPreset.AllColors ) {
            final float radius;
            final float luminosityLowerFeather;
            final float luminosityUpperFeather;

            if ( m_prevCS != null ) {
                radius = m_prevCS.radius;
                luminosityLowerFeather =
                    Math.min( cs.luminosityLower, m_lowerLuminosityFeather );
                luminosityUpperFeather =
                    Math.min( 1-cs.luminosityUpper, m_upperLuminosityFeather );
            } else {
                radius = cs.radius;
                luminosityLowerFeather =
                    Math.min( cs.luminosityLower, cs.luminosityLowerFeather );
                luminosityUpperFeather =
                    Math.min( 1-cs.luminosityUpper, cs.luminosityUpperFeather );
            }

            cs = new RGBColorSelection(
                cs.red, cs.green, cs.blue, radius,
                cs.luminosityLower, luminosityLowerFeather,
                cs.luminosityUpper, luminosityUpperFeather,
                false,
                m_hueEnabled.isSelected(),
                m_luminosityEnabled.isSelected()
            );
            m_prevCS = cs;
            m_lowerLuminosityFeather = cs.luminosityLowerFeather;
            m_upperLuminosityFeather = cs.luminosityUpperFeather;
        }
        return cs;
    }

    private void postEdit( String name ) {
        m_currentEdit.end( name );
        m_undoSupport.postEdit( m_currentEdit );
        m_currentEdit = new ColorSelectionEdit();
    }

    private void resetColorSelection() {
        m_isUpdatingControls = true;
        m_op.changeBatchStarted();
        boolean undoable = false;
        try {
            if ( m_hueEnabled.isSelected() ) {
                m_prevCS = null;
                m_colorPresets.setSelectedItem( RGBColorSelectionPreset.AllColors );
                undoable = true;
            }
            if ( m_luminosityEnabled.isSelected() ) {
                m_luminositySelector.setProperties(
                    m_luminositySelector.getMinimumThumbValue(),
                    m_luminositySelector.getMaximumThumbValue(),
                    m_luminositySelector.getMinimumThumbValue(),
                    m_luminositySelector.getMinimumThumbValue(),
                    m_luminositySelector.getMaximumThumbValue(),
                    m_luminositySelector.getMaximumThumbValue(),
                    m_luminositySelector.getMinimumTrackValue(),
                    m_luminositySelector.getMaximumTrackValue(),
                    m_luminositySelector.getTrackValue(),
                    m_luminositySelector.getTrackValueWraps()
                );
                undoable = true;
            }
            if ( undoable )
                postEdit( LOCALE.get( "ResetColorSelectionEditName" ) );
        }
        finally {
            m_op.changeBatchEnded();
            m_isUpdatingControls = false;
        }
    }

    private void selectColorAt( Point2D p ) {
        RGBColorSelection cs = m_op.getColorSelectionAt( p );
        cs = mergeColorSelections( cs );
        m_op.setColorSelection( cs );
        colorSelectionToControls( cs, false );
        postEdit( LOCALE.get( "ColorDropperEditName" ) );
    }

    private void selectPreset( RGBColorSelectionPreset p ) {
        if ( !m_skipSelectPresetCode ) {
            final boolean wasUpdatingControls = m_isUpdatingControls;
            m_skipSelectPresetCode = true;
            m_isUpdatingControls = true;
            m_hueEnabled.setSelected( true );
            RGBColorSelection cs =
                new RGBColorSelection( p, m_invertSelection.isSelected() );
            //cs = mergeColorSelections( cs );
            m_op.setColorSelection( cs );
            colorSelectionToControls( cs, false );
            m_skipSelectPresetCode = false;
            if ( !wasUpdatingControls )
                postEdit(
                    LOCALE.get( "SelectColorPresetEditName", p.toString() )
                );
            m_prevCS = null;
            m_isUpdatingControls = wasUpdatingControls;
        }
    }

    private boolean m_skipSelectPresetCode;

    private final ColorSwatch m_colorSwatch;

    private final JSlider m_colorRangeSlider;
    private final ColorPresets m_colorPresets;

    private ColorSelectionEdit m_currentEdit;

    private DropperButton m_dropperButton;
    private DropperMode m_dropperMode;

    private final JCheckBox m_invertSelection;

    private Operation m_op;

    private final RangeSelector m_luminositySelector;

    private float m_lowerLuminosityFeather, m_upperLuminosityFeather;

    private RGBColorSelection m_prevCS;

    private final JCheckBox m_hueEnabled;
    private final JCheckBox m_luminosityEnabled;

    // Flag dropper button state changes that just synchronize the button
    // when the dropper Mode is externally cancelled, so OpControlModeListener
    // notifications won't fire:
    private boolean m_isDropperModeCancelling;

    // Disable callbacks from the selectors and the checkboxes when these
    // controls are being slewed to a new ColorSelection.
    private boolean m_isUpdatingControls;

    private final OpControl.OpControlUndoSupport m_undoSupport;

    private final static String ColorSelectStartToolTip =
        LOCALE.get( "ColorSelectStartToolTip" );

    private final static String ColorSelectEndToolTip =
        LOCALE.get( "ColorSelectEndToolTip" );

    private final static String ColorSelectionKey = "ColorSelection";
    private final static String HueRedKey = "HueRed";
    private final static String HueGreenKey = "HueGreen";
    private final static String HueBlueKey = "HueBlue";
    private final static String HueRadiusKey = "HueRadius";
    private final static String HueEnabledKey = "HueEnabled";
    private final static String LuminosityLowerKey = "LuminosityLower";
    private final static String LuminosityUpperKey = "LuminosityUpper";
    private final static String LuminosityLowerFeatherKey = "LuminosityLowerFeather";
    private final static String LuminosityUpperFeatherKey = "LuminosityUpperFeather";
    private final static String LuminosityEnabledKey = "LuminosityEnabled";
    private final static String InvertedKey = "Inverted";

    private static final int SLIDER_RADIUS_MIN = 0;
    private static final int SLIDER_RADIUS_MAX = 100;
    private static final int SELECTOR_LUMINOSITY_MIN = 0;
    private static final int SELECTOR_LUMINOSITY_MAX = 1000;

    private static final Color COLOR_SWATCH_DEFAULT = Color.GRAY;
    private static final int COLOR_RADIUS_DEFAULT = 20;
}
/* vim:set et sw=4 ts=4: */
