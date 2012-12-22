// standard
#include <cstdlib>
#include <iostream>

// eSellerate
#include "EWSLib.h"
#define SUCCEEDED( errorCode ) ( (errorCode) >= 0 )

using namespace std;

//
// eSellerate constants for the Light Crafts account.
//
#define ES_PUBLISHER_ID     "PUB8462586885"
#define ES_ACTIVATION_ID    "ACT424162180"

////////// Local functions ////////////////////////////////////////////////////

/**
 * In some cases, eSellerate library functions return codes >= 0 for success
 * and < 0 for failure.  This is dumb.  If the function succeeds, we don't
 * really care what the code is, so just change it to E_SUCCESS.
 */
inline OSStatus esError( OSStatus errorCode ) {
    return SUCCEEDED( errorCode ) ? E_SUCCESS : errorCode;
}

////////// main() /////////////////////////////////////////////////////////////

int main( int argc, char *argv[] ) {
    char const *me = argv[0];
    if ( --argc != 1 ) {
        cerr << "usage: " << me << " key" << endl;
        ::exit( 1 );
    }
    OSStatus errorCode = eWeb_ActivateSerialNumber(
        ES_PUBLISHER_ID, ES_ACTIVATION_ID, argv[1],
        0 // do not do manual activation
    );
    cout << errorCode << endl;

    errorCode = esError( errorCode );
    return errorCode ? 2 : 0;
}

/* vim:set et sw=4 ts=4: */
