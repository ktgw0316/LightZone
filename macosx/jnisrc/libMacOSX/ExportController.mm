// local
#import "ExportController.h"
#import "LC_CocoaUtils.h"

using namespace std;

@implementation ExportController

/**
 * Awake from NIB.
 */
- (void) awakeFromNib
{
    if ( [[self superclass] instancesRespondToSelector:@selector(awakeFromNib)] )
        [super awakeFromNib];

    [imageTabView setDelegate:self];
}

/**
 * Deallocate an ExportController.
 */
- (void) dealloc
{
    [m_jpgExtensions release];
    [m_lznExtensions release];
    [m_tifExtensions release];
    [super dealloc];
}

/**
 * This is called by the Cocoa framework whenever the user selects an item from
 * the "File Type" pop-up menu.
 */
- (IBAction) fileTypeSelection:
    (id)sender
{
    [self setImageType:[[fileTypePopUp selectedItem] tag]];
}

/**
 * Initialize an ExportController.
 */
- (id) init
{
    self = [super init];

    static char const *const jpgExtensions[] = { "jpg", "jpe", "jpeg" };
    static char const *const lznExtensions[] = { "lzn" };
    static char const *const tifExtensions[] = { "tif", "tiff" };

    m_jpgExtensions = [LC_cStringArrayToNSArray( jpgExtensions, 3 ) retain];
    m_lznExtensions = [LC_cStringArrayToNSArray( lznExtensions, 1 ) retain];
    m_tifExtensions = [LC_cStringArrayToNSArray( tifExtensions, 2 ) retain];

    return self;
}

/**
 * Reset the resize X & Y fields to the original image size.
 */
- (IBAction) resetResize:
    (id)sender
{
    [resizeXField abortEditing];
    [resizeYField abortEditing];
    [resizeXField setIntValue:m_originalWidth];
    [resizeYField setIntValue:m_originalHeight];
}

/**
 * Change the "File Type" tab based on the selected file type.
 */
- (void) setImageType:
    (int)fileTypeTagID
{
    int const tabID =
        [(NSNumber*)[[imageTabView selectedTabViewItem] identifier] intValue];

    int const newTabID = fileTypeTagID != 'L';

    if ( tabID != newTabID ) {

        [imageTabView selectTabViewItemAtIndex:newTabID];

#if 0
        if ( newTabID && ![imageTabView superview] ) {
            [mainView addSubview:imageTabView];
            [imageTabView release];
        } else if ( !newTabID && [imageTabView superview] ) {
            [imageTabView retain];
            [imageTabView removeFromSuperview];
        }
#endif

#if 0
        static NSRect imageTabViewRect[2];
        if ( !imageTabViewRect[1].size.height ) {
            imageTabViewRect[1] = [imageTabView frame];
            imageTabViewRect[0] = imageTabViewRect[1];
            //imageTabViewRect[0].origin.y += imageTabViewRect[0].size.height;
            imageTabViewRect[0].size.height = 0;
        }
        [imageTabView setFrame:imageTabViewRect[ newTabID ]];
        [imageTabView setNeedsDisplay:YES];
#endif

#if 0
        static NSRect mainViewRect[2];
        if ( !mainViewRect[1].size.height ) {
            mainViewRect[1] = [mainView frame];
            mainViewRect[0] = mainViewRect[1];
            mainViewRect[0].size.height -= imageTabViewRect[1].size.height;
            mainViewRect[0].origin.y += imageTabViewRect[1].size.height;
        }
        [mainView setFrame:mainViewRect[ newTabID ]];
        [mainView display];
#endif

        //[m_panel setAccessoryView:mainView];

#if 0
        NSWindow *const window = [mainView window];
        NSRect windowRect = [window frame];
        windowRect.origin.y += dy;
        windowRect.size.height -= dy;

        [window setFrame:windowRect display:YES animate:YES];
#endif
    }

    switch ( fileTypeTagID ) {

        case 'L':   // LZN
            [m_panel setAllowedFileTypes:m_lznExtensions];
            break;

        case 'J':   // JPEG
            [m_panel setAllowedFileTypes:m_jpgExtensions];
            [imageOptionsTabView selectTabViewItemAtIndex:0];
            break;

        case 'T':   // TIFF
            [m_panel setAllowedFileTypes:m_tifExtensions];
            [imageOptionsTabView selectTabViewItemAtIndex:1];
            [lzwCompressionCheckBox setEnabled:TRUE];
            [multilayerCheckBox setEnabled:TRUE];
            break;

        default:
            [NSException raise:@"IllegalArgumentException"
                format:@"invalid fileTypeTagID: %d", fileTypeTagID
            ];
    }
}

/**
 * TODO
 */
- (void) tabView:
    (NSTabView*)tabView
    didSelectTabViewItem:(NSTabViewItem*)tabViewItem
{
    NSInteger const ident = (NSInteger)[tabViewItem identifier];
}

@end
/* vim:set et sw=4 ts=4: */
