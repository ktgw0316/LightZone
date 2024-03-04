#! /usr/bin/perl

use Getopt::Std;
use Encode;

getopts( 'c:e:h:i:l:r:' );
$LANG           = $opt_l || 'English';
$ISO_LANG_CODE  = $opt_i || 'en';
$HELP_TITLE     = decode_utf8($opt_h) || 'LightZone Help';
$W32_LANG_CODE  = $opt_c || '0x409';
$W32_REGION     = $opt_r || 'United States';
$HTML_CHARSET   = $opt_e || 'ISO-8859-1';

while ( <> ) {
    s/\@LANG\@/$LANG/g;
    s/\@HELP_TITLE\@/$HELP_TITLE/g;
    s/\@ISO_LANG_CODE\@/$ISO_LANG_CODE/g;
    s/\@W32_LANG_CODE\@/$W32_LANG_CODE/g;
    s/\@W32_REGION\@/$W32_REGION/g;
    print encode($HTML_CHARSET, $_);
}

# vim:set et sw=4 ts=4:
