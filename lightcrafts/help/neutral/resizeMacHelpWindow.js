/**
 * Resize the Mac OS X Help Viewer window if it's less than a minimum size that
 * looks good.
 */

var minWidth  = 590;
var minHeight = 445;

var agent = navigator.userAgent.toLowerCase();
var isMacHelpViewer = ( agent.indexOf( 'help' ) >= 0 );

function resizeMacHelpWindow() {
    if ( isMacHelpViewer )
        if ( window.innerWidth < minWidth || window.innerHeight < minHeight )
            window.resizeTo( minWidth, minHeight );
}

/* vim:set et sw=4 ts=4: */
