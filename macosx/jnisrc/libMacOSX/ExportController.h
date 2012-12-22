/* Copyright (C) 2005-2011 Fabio Riccardi */

#import <Cocoa/Cocoa.h>

@interface ExportController : NSObject {
@public
    IBOutlet NSPopUpButton* bitsPerChannelPopUp;
    IBOutlet NSButton*      blackPointCheckBox;
    IBOutlet NSPopUpButton* colorProfilePopUp;
    IBOutlet NSPopUpButton* fileTypePopUp;
    IBOutlet NSTabView*     imageOptionsTabView;
    IBOutlet NSTabView*     imageTabView;
    IBOutlet NSButton*      lzwCompressionCheckBox;
    IBOutlet NSView*        mainView;
    IBOutlet NSButton*      multilayerCheckBox;
    IBOutlet NSSlider*      qualitySlider;
    IBOutlet NSTextField*   qualityField;
    IBOutlet NSPopUpButton* renderingIntentPopUp;
    IBOutlet NSButton*      resetResizeButton;
    IBOutlet NSTextField*   resizeXField;
    IBOutlet NSTextField*   resizeYField;
    IBOutlet NSTextField*   resolutionField;
    IBOutlet NSPopUpButton* resolutionUnitPopUp;

    NSSavePanel*            m_panel;

    NSArray*                m_jpgExtensions;
    NSArray*                m_lznExtensions;
    NSArray*                m_pngExtensions;
    NSArray*                m_tifExtensions;

    int                     m_originalWidth;
    int                     m_originalHeight;
}

- (void) dealloc;

- (IBAction) fileTypeSelection:
    (id)sender;

- (id) init;

- (IBAction) resetResize:
    (id)sender;

- (void) setImageType:
    (int)imageTypeTagID;

- (void) tabView:
    (NSTabView*)tabView
    didSelectTabViewItem:(NSTabViewItem*)tabViewItem;

@end
/* vim:set et sw=4 ts=4: */
