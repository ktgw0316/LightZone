/**
 * Correctly handle PNG transparency in IE 5.5 & 6.
 * http://homepage.ntlworld.com/bobosola/
 * Updated: 18-Jan-2006.
 * 
 * Use in <HEAD> with DEFER keyword wrapped in conditional comments:
 * <!--[if lt IE 7]>
 * <script defer type="text/javascript" src="IE_PNG.js"></script>
 * <![endif]-->
 */

var versionString = navigator.appVersion.split( "MSIE" );
var version = parseFloat( versionString[1] );

if ( (version >= 5.5) && (version < 7) && (document.body.filters) ) {
    for ( var i = 0; i < document.images.length; ++i ) {
        var img = document.images[i];
        if ( img.src.substring( img.src.length-3, img.src.length ) == "png" ) {
            var imgClass = img.className ?
                "class='" + img.className + "' " : "";

            var imgStyle = "display:inline-block;" + img.style.cssText;

            var newHTML = "<span " + imgClass
                + " style=\"" + imgStyle + ";"
                + "width:" + img.width + "px; height:" + img.height + "px;"
                + "filter:progid:DXImageTransform.Microsoft.AlphaImageLoader"
                + "(src='" + img.src + "');\"></span>";

            img.outerHTML = newHTML;

            i = i - 1;
        }
    }
}    
/* vim:set et sw=4 ts=4: */
