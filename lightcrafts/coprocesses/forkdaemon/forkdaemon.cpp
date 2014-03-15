/* Copyright (C) 2005-2011 Fabio Riccardi */

#if USE_FORKER

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

using namespace std;

enum daemon_status {
    idle, collecting, executing
};

enum exit_codes {
    Exit_Success    = 0,
    Exit_No_Exec    = 1,
    Exit_No_Fork    = 2,
    Exit_No_Pipe    = 3,
    Exit_No_Wait    = 4,
    Exit_No_Write   = 5
};

static const char CMDSTART[] = "CMDSTART";
static const char CMDEND[]   = "CMDEND";

static char const*  me;                 // executable name

static char *readln(FILE *f) {
    static char readln_buffer[256];
    int i = 0;
    int c;
    while ((c = getc(f)) != EOF && c != '\n')
        readln_buffer[i++] = c;
    if (c == EOF && i == 0)
        return NULL;

    readln_buffer[i] = '\0';
    return readln_buffer;
}

int main( int argc, char const *argv[] ) {
    me = ::strchr( argv[0], '/' );      // determine base name...
    me = me ? me + 1 : argv[0];         // ...of executable

    // FILE *log = fopen("/tmp/forker.log", "w");

    // fprintf(log, "starting\n"); fflush(log);

    daemon_status execution_state = idle;
    static char *arguments[100];
    int nargs = 0;

    while ( true ) {
        switch ( execution_state ) {

            case idle: {
                // fprintf(log, "idle\n"); fflush(log);
                char *line = readln(stdin);
                if ( line == NULL )
                    ::exit( Exit_Success );
                if ( ::strcmp( line, CMDSTART ) == 0 ) {
                    // make sure we don't keep unwanted history...
                    memset(arguments, 0, sizeof(arguments));
                    execution_state = collecting;
                }
                continue;
            }

            case collecting: {
                // fprintf(log, "collecting\n"); fflush(log);
                const char *const line = readln(stdin);
                if (line == NULL)
                    return 0;          // if the input file ever gets closed exit
                if ( strlen(line) > 0 ) {
                    if ( ::strcmp( line, CMDEND ) == 0 )
                        execution_state = executing;
                    else
                        arguments[nargs++] = ::strdup( line );
                }
                continue;
            }

            case executing: {
                // fprintf(log, "executing\n"); fflush(log);

                pid_t pid;
                int pipe_out[2], pipe_err[2];

                if ( ::pipe( pipe_out ) == -1 )
                    ::exit( Exit_No_Pipe );
                if ( ::pipe( pipe_err ) == -1 )
                    ::exit( Exit_No_Pipe );

                switch ( pid = ::fork() ) {
                    case -1:
                        // fprintf(log, "can't fork\n"); fflush(log);
                        ::exit( Exit_No_Fork );

                    case 0:             // child
                        ::close( 1 );
                        ::dup( pipe_out[1] );   // rebind stdout
                        ::close( 2 );
                        ::dup( pipe_err[1] );   // rebind stderr

                        ::close( pipe_out[0] ); // close unneeded descriptors
                        ::close( pipe_err[0] );

                        // fprintf(log, "child started\n"); fflush(log);

                        ::execvp( arguments[0], arguments );
                        ::exit( Exit_No_Exec );
                }

                // fprintf(log, "parent started\n"); fflush(log);

                ::close( pipe_out[1] );         // close unneeded descriptors
                ::close( pipe_err[1] );

                bool stdout_closed = false;
                bool stderr_closed = false;

                while (!stdout_closed || !stderr_closed) {
                    static char read_line[256];
                    if ( !stdout_closed ) {
                        int nchars = read( pipe_out[0], read_line, 256 );
                        if ( nchars <= 0 )
                            stdout_closed = true;
                        else
                            ::write( 1, read_line, nchars );
                    }
                    if ( !stderr_closed ) {
                        int nchars = read( pipe_err[0], read_line, 256 );
                        if ( nchars <= 0 )
                            stderr_closed = true;
                        else
                            ::write( 2, read_line, nchars );
                    }
                }

                ::close( pipe_out[0] );
                ::close( pipe_err[0] );

                int status;
                if ( ::wait( &status ) == -1 )
                    ::exit( Exit_No_Wait );
                /*
                if ( WIFEXITED(status) )
                    fprintf( log, "child %d returned: %d\n", pid, WEXITSTATUS(status) ); fflush(log);
                else
                    fprintf( log, "child %d signaled with: %d\n", pid, WTERMSIG(status) ); fflush(log);
                */

                static char const EOC = 255;

                ::write( 1, &EOC, 1 );
                ::write( 2, &EOC, 1 );

                for ( int i = 0; i < nargs; i++ )
                    ::free( arguments[i] );
                nargs = 0;
                execution_state = idle;
            }
        }
    }

    // fprintf(log, "terminating\n"); fflush(log);
    return Exit_Success;
}
#else
main() {
}
#endif
/* vim:set et sw=4 ts=4: */
