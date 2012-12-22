////////////////////////////////////////////////////////
// Name     : parser.cpp
// Purpose  : General purpose parser
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#include "parser.h"
#include <stdio.h>
#include <string.h>

////////////////////////////////////////////////////////
// CmCToken
////////////////////////////////////////////////////////

CmCToken::CmCToken(char *token, int lineNumber)
{
  lineNumber_ = lineNumber;
  tokenSize_  = strlen(token);
  token_      = new char[tokenSize_ + 1];
  strcpy(token_, token);
  next_       = (CmCToken *) NULL;
}

CmCToken::~CmCToken( void )
{
  lineNumber_ = -1;
  tokenSize_  = 0;
  next_       = (CmCToken *) NULL;
  if(token_) delete [] token_;
}

////////////////////////////////////////////////////////
// CmCParser
////////////////////////////////////////////////////////

CmCParser::CmCParser( void )
{
  head_            = (CmCToken *) NULL;
  tail_            = (CmCToken *) NULL;
  cur_             = (CmCToken *) NULL;
  delimiters_      = (char *)     NULL;
  delstr_          = (char *)     NULL;
  storeDelimiters_ = false; 
}

CmCParser::~CmCParser( void )
{
  ClearList();
  if(delimiters_) delete [] delimiters_;
  if(delstr_)     delete [] delstr_;
}

int CmCParser::Parse(char *filename)
{

  //make sure file name exists
  if(!filename) return 1;
  
  //open file for reading and make sure
  //the file exists before proceeding
  FILE *fp = fopen(filename, "rb");
  if(!fp) return 1;

  //if a token list exists, destroy it
  ClearList();

  bool blockComment = false;
  char buf[80], *ptr1, *ptr2, ch, cur = 0;
  int i, curLineNumber = 1;
  while(cur != EOF)
    {

      //****************************************************
      //Read in and parse the file one line at a time
      //****************************************************

      i   = 0;
      cur = getc(fp);
      while((cur != EOF)&&(cur != '\n'))
	{
	  if(cur != 13) {
	    buf[i] = cur;
	    i++;
	  }
	  cur    = getc(fp);
	}
      buf[i] = 0;

      //****************************************************
      //Parse the line buffer
      //****************************************************  

      //initialize pointers to beginning of line buffer
      ptr1 = buf;

      //if in a block comment section, find its
      //end
      if(blockComment)
	{
	  ptr1 = strchr(buf, '*');
	  if((ptr1)&&(*(ptr1+1) == '/'))
	    blockComment = false;
	  ptr1 += 2;
	}

      while(!blockComment)
	{
	  //skip spaces and tabs
	  while((*ptr1 == ' ')||(*ptr1 == '\t')) ptr1++;

	  //skip comments
	  if((*ptr1)&&(*ptr1 == '/')&&(*(ptr1+1) == '/'))
	    break;

	  //skip block comments
	  if((*ptr1)&&(*ptr1 == '/')&&(*(ptr1+1) == '*'))
	    {
	      blockComment = true;
	      break;
	    }    

	  //get token
	  ptr2 = ptr1;
	  while((!IsDelimiter(*ptr2))&&(*ptr2 != ' ')&&(*ptr2 != '\t')&&(*ptr2 != 0))
	    ptr2++;
	  ch    = *ptr2;
	  *ptr2 = 0;
	  if(*ptr1) InsertToken(ptr1, curLineNumber);

	  //store delimiters as tokens
	  if((storeDelimiters_)&&(IsDelimiter(ch)))
	    InsertToken(ch, curLineNumber);

	  //next token
	  if(ch)
	    ptr1 = ptr2 + 1;
	  else
	    break;
	}

      //next line
      curLineNumber++;

    }
  
  //done.
  return 0;  
}

void CmCParser::SetDelimiters(char *delimiters)
{
  if(delimiters_) delete delimiters_;
  delimiters_ = new char[strlen(delimiters)+1];
  strcpy(delimiters_,delimiters);
  char *delptr = delimiters_;
}

void CmCParser::StoreDelimiters(bool flag)
{
  storeDelimiters_ = flag;
}

void CmCParser::StartOver( void )
{
  cur_ = head_;
}

CmCToken *CmCParser::GetToken( void )
{
  CmCToken *token = (CmCToken *) NULL;
  if(cur_)
    {
      token = cur_;
      cur_  = cur_->next_;
    }
  return token;
}

void CmCParser::ClearList( void )
{
  CmCToken *temp;
  while(head_)
    {
      temp  = head_;
      head_ = head_->next_;
      delete temp;
    }
  tail_ = (CmCToken *) NULL;
  cur_  = (CmCToken *) NULL;
}

void CmCParser::InsertToken(char *token, int lineNumber)
{
  //create new token entry
  CmCToken *newToken   = new CmCToken(token, lineNumber);
  
  //add new token entry to the token list
  if(!head_)
    {
      head_ = tail_ = cur_ = newToken;
    }
  else
    {
      tail_->next_ = newToken;
      tail_        = newToken;
    }
}

void CmCParser::InsertToken(char ch, int lineNumber)
{
  if(!delstr_) delstr_ = new char [2];
  delstr_[0] = ch;
  delstr_[1] = 0;
  InsertToken(delstr_, lineNumber);
}

bool CmCParser::IsDelimiter(char ch)
{
  char *delptr = delimiters_;
  while(*delptr)
    {
      if(ch == *delptr) return true;
      delptr++;
    }
  return false;
}

	  
      
      


