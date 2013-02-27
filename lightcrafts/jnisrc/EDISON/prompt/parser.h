////////////////////////////////////////////////////////
// Name     : parser.h
// Purpose  : General purpose parser
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#ifndef PARSER_H
#define PARSER_H

class CmCToken 
{
 public:

  int lineNumber_, tokenSize_;
  char *token_;
  CmCToken *next_;

  CmCToken(char *, int);
  ~CmCToken( void );
};

class CmCParser 
{
 public:

  CmCParser( void );
  ~CmCParser( void );
  
  //parses a file having specified filename
  int Parse (char *);

  //sets the token pointer to the beginning of
  //the token list
  void StartOver( void );

  //retrievs token pointed to by current token pointer
  //and increments the current token pointer to point
  //to the next token; NULL is returned if the current
  //token pointer is NULL (i.e. nothing has been parsed
  //or the end of the token list has been reached)
  CmCToken *GetToken( void );

  //indicate any delimiters
  void SetDelimiters(char *);

  //indicate whether delimiters should be stored as tokens
  void StoreDelimiters(bool);

 private:

  int tokenCount_;
  bool storeDelimiters_;
  CmCToken *head_, *tail_, *cur_;
  char *delimiters_, *delstr_;  

  void ClearList( void );
  void InsertToken(char *, int);
  void InsertToken(char, int);
  bool IsDelimiter(char);  
};

#endif
