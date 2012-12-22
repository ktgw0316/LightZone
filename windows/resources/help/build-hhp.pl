#! /usr/bin/perl

use Getopt::Std;

getopts( 'c:h:i:l:r:' );
$LANG           = $opt_l || 'English';
$ISO_LANG_CODE  = $opt_i || 'en';
$HELP_TITLE     = $opt_h || 'LightZone Help';
$W32_LANG_CODE  = $opt_c || '0x409';
$W32_REGION     = $opt_r || 'United States';

while ( <> ) {
    s/\@LANG\@/$LANG/g;
    s/\@HELP_TITLE\@/$HELP_TITLE/g;
    s/\@ISO_LANG_CODE\@/$ISO_LANG_CODE/g;
    s/\@W32_LANG_CODE\@/$W32_LANG_CODE/g;
    s/\@W32_REGION\@/$W32_REGION/g;
    print;
}

# vim:set et sw=4 ts=4:
