////////////////////////////////////////////////////////
// Name     : error.h
// Purpose  : Command Prompt Error Object Definition
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#ifndef ERROR_H
#define ERROR_H

#include "parser.h"

//error class
class CmCError
{
 public:

  CmCToken *token_;
  int      errorCode_;
  
  CmCError(CmCToken *token, int errorCode) {
    token_ = token;
    errorCode_ = errorCode;
  }
  ~CmCError( void ) { return; }
};

#endif
