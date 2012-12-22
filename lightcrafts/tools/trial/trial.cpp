/* Copyright (C) 2005-2011 Fabio Riccardi */

#include <cstdlib>
#include <cstring>
#include <ctime>
#include <fstream>
#include <iostream>

using namespace std;

int const   OldTrialLicenseSize = 1 + sizeof( time_t );
int const   TrialLicenseSize = OldTrialLicenseSize + sizeof( short );
long const  TrialLicenseDuration = 60*60*24 /* seconds/day */ * 30 /* days */;

enum cpu_t { cpu_none, cpu_intel, cpu_powerpc };

cpu_t       cpu;
char const* me;
bool        opt_print_start;
bool        opt_write_expired;

/**
 * Flip a time_t for the right CPU.
 */
void flip( time_t *t ) {
#ifdef __POWERPC__
    if ( cpu == cpu_intel ) {
#else
    if ( cpu == cpu_powerpc ) {
#endif
        *t = (*t << 24) & 0xFF000000 |
             (*t <<  8) & 0x00FF0000 |
             (*t >>  8) & 0x0000FF00 |
             (*t >> 24) & 0x000000FF ;
    }
}

/**
 * Read an existing trial license file.
 */
void read_trial( char const *file ) {
    ifstream in( file, ios::binary );
    if ( !in ) {
        cerr << me << ": could not open " << file << endl;
        ::exit( 2 );
    }

    char buf[ 64 ];
    int const bytes_read = in.readsome( buf, sizeof( buf ) );
    in.close();

    switch ( bytes_read ) {
        case OldTrialLicenseSize:
        case TrialLicenseSize:
            if ( buf[0] != 'T' ) {
                cerr << me << ": " << file
                     << " isn't a trial license file (first byte is 0x"
                     << hex << (int)buf[0] << ")\n";
                ::exit( 3 );
            }
            break;
        default:
            cerr << me << ": " << file << " isn't either "
                 << OldTrialLicenseSize << " or " << TrialLicenseSize
                 << " bytes\n";
            ::exit( 4 );
    }

    time_t t;
    ::memcpy( &t, buf + 1, sizeof( time_t ) );
    flip( &t );

    if ( !opt_print_start )
        t += TrialLicenseDuration;

    cout << ::ctime( &t );
}

/**
 * Write a new trial license file.
 */
void write_trial( char const *file ) {
    ofstream out( file, ios::out | ios::binary );
    if ( !out ) {
        cerr << me << ": could not write to " << file << endl;
        ::exit( 5 );
    }
    char buf[ TrialLicenseSize ];
    buf[0] = 'T';
    time_t now;
    if ( opt_write_expired )
        now = 0;
    else {
        now = ::time( 0 );
        flip( &now );
    }
    ::memcpy( buf + 1, &now, sizeof( time_t ) );
    short revision = 0;
    ::memcpy( buf + 5, &revision, sizeof( short ) );
    out.write( buf, sizeof( buf ) );
    out.close();
}

/**
 * Print usage message and exit.
 */
void usage() {
    cerr << "usage: " << me << "[options] trial_file\n"
            "------\n"
            "   -e: write an expired license for testing (implies -w)\n"
            "   -i: read/write Intel\n"
            "   -p: read/write PowerPC\n"
            "   -s: print start date instead of expiration\n"
            "   -w: write new file instead of read\n";
    ::exit( 1 );
}

/**
 * Main.
 */
int main( int argc, char *argv[] ) {
    if ( me = ::strrchr( argv[0], '/' ) )
        ++me;
    else
        me = argv[0];

    bool opt_write_new = false;

    extern char *optarg;
    extern int optind, opterr;
    for ( int c; (c = ::getopt( argc, argv, "eipsw" )) != EOF; )
        switch ( c ) {
            case 'i':
                cpu = cpu_intel;
                break;
            case 'p':
                cpu = cpu_powerpc;
                break;
            case 's':
                opt_print_start = true;
                break;
            case 'e':
                opt_write_expired = true;
                cpu = cpu_intel;        // doesn't matter for expired
                // no break;
            case 'w':
                opt_write_new = true;
                break;
            case '?':
                usage();
        }
    argc -= optind, argv += optind;
    if ( argc != 1 )
        usage();
    if ( cpu == cpu_none ) {
        cerr << me << ": either -i or -p is required\n";
        ::exit( 1 );
    }

    if ( opt_write_new )
        write_trial( argv[0] );
    else
        read_trial( argv[0] );
}
/* vim:set et sw=4 ts=4: */
