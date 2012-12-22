////////////////////////////////////////////////////////
// Name     : main.cpp
// Purpose  : Edge Detection and Image SegmentatiON
//            (EDISON) Command Prompt System
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

//include local and system libraries and definitions
#include "defs.h"
#include "error.h"
#include "parser.h"
#include <stdio.h>
#include <stdlib.h>

//*******************************************************
//define prototypes of global functions
//*******************************************************

//check syntax of source file
CmCError CheckSyntax(char *filename, CmCParser *synParser);

//run the script
CmCError Run(CmCParser *script);

//report system errors
void Report(CmCError *error, char *srcFilename, CmCParser *srcParser);

//*******************************************************

int main(int argc, char **argv)
{
  //check usage
  if(argc != 2) {
      fprintf(stderr, "--------------------------------------------------\n");
      fprintf(stderr, "Usage: %s <filename>\n", argv[0]);
      fprintf(stderr, "--------------------------------------------------\n");
      fprintf(stderr, "filename: File containing EDISON execution script.\n");
      fprintf(stderr, "--------------------------------------------------\n");
      exit(1);
  }
  
  //parse the source file and check for
  //syntaxical errors
  CmCParser *srcParser = new CmCParser;
  CmCError error = CheckSyntax(argv[1], srcParser);
  if(error.errorCode_) {
    Report(&error, argv[1], srcParser);
    exit(1);
  }

  //execute script
  error = Run(srcParser);
  if(error.errorCode_) {
    Report(&error, argv[1], srcParser);
    exit(1);
  }

  //de-allocate parser
  delete srcParser;

  return 0;
}

