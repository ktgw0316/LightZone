////////////////////////////////////////////////////////
// Name     : globalFnc.cpp
// Purpose  : Global function definitions for EDISON
//            command prompt.
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#include "defs.h"
#include "error.h"
#include "parser.h"
#include "imfilt.h"
#include "edison.h"
#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

////////////////////////////////////////////////////
//define global flags
////////////////////////////////////////////////////

//segmentation flags
bool CmCSynergistic        = false;
bool CmCGradWinChanged     = false;
bool CmCMixtureChanged     = false;
bool CmCUseCustomWeightMap = false;

//system flags
bool CmCImageDefined       = false;
bool CmCMapDefined         = false;
bool CmCDisplayProgress    = false;

//edge detection flags
bool CmCRankMapDefined     = false;
bool CmCConfMapDefined     = false;

////////////////////////////////////////////////////////
//Declare global EDISON wrapper class object
////////////////////////////////////////////////////////

EDISON edison;

////////////////////////////////////////////////////////
//Display prompt function
////////////////////////////////////////////////////////

void CmCPrompt(const char *PromptStr, ...)
{
  //obtain argument list using ANSI standard...
  va_list	argList;
  va_start(argList, PromptStr);

  //print the output string to stderr using
  if(CmCDisplayProgress) vfprintf(stdout, PromptStr, argList);
  va_end(argList);

  //done.
  return;
}  

////////////////////////////////////////////////////////
//Identity (Look-up Table Functions) Functions
////////////////////////////////////////////////////////

//************************
//identify command type
//************************

int Command(char *cmd)
{
  int i;
  for(i = 0; i < CMD_NUM; i++)
    {
      if(!strcmp(cmd, CMD_LIST[i])) return i;
    }
  return UNKNOWN_COMMAND;
}

//************************
//identify command type
//************************

int CommandType(int cmdId)
{
  return CMD_TYPE_LIST[cmdId];
}

//************************
//identify parameter
//************************

int Parameter(char *param)
{
  int i;
  for(i = 0; i < PARAM_NUM; i++)
    {
      if(!strcmp(param, PARAM_LIST[i])) return i;
    }
  return UNKNOWN_PARAMETER;
}

//************************
//identify custom curve list
//************************

int CustomCurve(char *curve)
{
  int i;
  for(i = 0; i < CUST_CURVE_NUM; i++)
    {
      if(!strcmp(curve, CUST_CURVE_LIST[i])) return i;
    }
  return UNKNOWN_CURVE;
}

//************************
//identify input type
//************************

int InputType(char *inputtype)
{
  int i;
  for(i = 0; i < INPUTTYPE_NUM; i++) { 
    if(!strcmp(inputtype, INPUTTYPE_LIST[i])) return i;
  }
  return INPUT_UNKNOWN;
}

//************************
//identify output type
//************************

int OutputType(char *outputtype)
{
  int i;
  for(i = 0; i < OUTPUTTYPE_NUM; i++) { 
    if(!strcmp(outputtype, OUTPUTTYPE_LIST[i])) return i;
  }
  return OUTPUT_UNKNOWN;
}

//************************
//identify file type
//************************

int FileType(char *filetype)
{
  int i;
  for(i = 0; i < FILETYPE_NUM; i++) { 
    if(!strcmp(filetype, FILETYPE_LIST[i])) return i;
  }
  return FILE_UNKNOWN;
}

////////////////////////////////////////////////////////
//Report Errors
////////////////////////////////////////////////////////

void Report(CmCError *error, char *filename, CmCParser *srcParser)
{

  //obtain error code
  int errorCode = error->errorCode_;

  //report invalid file error
  if(errorCode == SYN_INVALID_FILE) {
    fprintf(stderr, "EDISON Fatal Error: Cannot open file '%s'.\n", filename);
    fprintf(stderr, "Permission denied or file does not exist.\n");
    return;
  }

  //obtain line number and token string of bad token
  int  lineNumber = error->token_->lineNumber_;
  char *token     = error->token_->token_;  

  //***************************************
  //report syntax error
  //***************************************  

  //check if out of bounds error
  if(errorCode == SYN_BOUNDS_ERROR) {
    fprintf(stderr, "Out of bounds error in file '%s'.\n", filename);
    fprintf(stderr, "Line %d (parameter %s) Error: %s\n", lineNumber, token, 
	    boundsTable[BOUNDS_TABLE[Parameter(token)]]);
    return;
  }

  //check for unsupported file formats
  if(errorCode == SYN_UNSUPPORTED_FILETYPE) {
    fprintf(stderr, "Version incompatability error:\n");
    fprintf(stderr, "Line %d (argument '%s') Error: %s\n", lineNumber, token, ErrorMsgTable[errorCode-1]);
    return;
  }  

  //decrement error code to index the error message
  //table correctly (starting from 0 instead of 1)
  errorCode--;

  if(errorCode < SYN_INVALID_PARAMCMD) {
    //report syntax error
    fprintf(stderr, "Syntax error in file '%s':\n", filename);
    fprintf(stderr, "Line %d (confused by '%s') Error: %s\n", lineNumber, token, ErrorMsgTable[errorCode]);
  } else if (errorCode < SYN_ASSIGN_INVALID_ARG) {
    //report assignment error
    fprintf(stderr, "Assignment error in file '%s':\n", filename);
    fprintf(stderr, "Line %d (parameter %s) Error: %s\n", lineNumber, token, ErrorMsgTable[errorCode]);
  }

  //***************************************
  //report run-time error
  //***************************************

  //check if parameter is missing
  if(errorCode == (EXE_MISSING_PARAM-1)) {
    //report missing parameter
    fprintf(stderr, "Run-time error cause while running script '%s':\n", filename);
    fprintf(stderr, "Line %d Fatal Error: Parameter '%s' is not specified.\n", lineNumber, token);
    delete error->token_;
    return;
  }  

  if(errorCode >= SYN_ASSIGN_INVALID_ARG) {
    //report run-time error
    fprintf(stderr, "Run-time error caused while running script '%s':\n", filename);
    fprintf(stderr, "Line %d (at '%s') Fatal Error: %s\n", lineNumber, token, ErrorMsgTable[errorCode]);
  }

  //done.
  return;
}

////////////////////////////////////////////////////////
//Check Syntax
////////////////////////////////////////////////////////

//************************
//get token set for a specified command type
//************************

int GetTokenSet(int cmdId, CmCToken **tokenSet, CmCParser *synParser)
{
  //obtain token count
  int tokenCount, cmdTypeId = CommandType(cmdId);
  switch(cmdTypeId)
    {
    case CMD_IO       : 
      switch(cmdId)
	{
	case CMD_SAVE : tokenCount = 10; break;
	case CMD_LOAD : tokenCount =  8; break;
	default       : tokenCount =  4; break;
	}
      break;
    case CMD_EXECUTION: tokenCount = 1; break;
    case CMD_FLAGS    : tokenCount = 2; break;
    default           : return UNKNOWN_COMMAND;
    }
  
  //populate token set
  int i;
  for(i = 0; i < tokenCount; i++)
    {
      if(!(tokenSet[i] = synParser->GetToken()))
	return SYN_ERROR;
    }
  
  //Appropriate number of tokens read.
  return NO_ERRORS;
}      

//************************
//get token set for a parameter
//************************

int GetParamTokenSet(CmCToken **tokenSet, CmCParser *synParser)
{
  int i;
  for(i = 0; i < 3; i++)
    {
      if(!(tokenSet[i] = synParser->GetToken()))
	return SYN_ERROR;
    }
  return NO_ERRORS;
}

//************************
//check for valid file type
//************************

bool validFileType(char *fileType)
{
  int i;
  for(i = 0; i < FILETYPE_NUM; i++)
    if(!strcmp(fileType, FILETYPE_LIST[i])) return true;
  return false;
}

//************************
//check for valid output type
//************************

bool validOutputType(char *outputType)
{
  int i;
  for(i = 0; i < OUTPUTTYPE_NUM; i++)
    if(!strcmp(outputType, OUTPUTTYPE_LIST[i])) return true;
  return false;
}

//************************
//check for valid intput type
//************************

bool validInputType(char *inputType)
{
  int i;
  for(i = 0; i < INPUTTYPE_NUM; i++)
    if(!strcmp(inputType, INPUTTYPE_LIST[i])) return true;
  return false;
}

//************************
//check for valid flag
//************************

bool validFlag(char *flag)
{
  return !(strcmp(flag, "ON") && strcmp(flag, "OFF"));
}

//************************
//check for valid data type and value
//************************

int checkInteger(char *data)
{
  char *dptr = data, ch;
  while(ch = *(dptr++)) if(((ch < 48)||(ch > 57))&&(ch != '-')) return SYN_ASSIGN_INTEGER;
  return NO_ERRORS;
}

int checkFloatingPoint(char *data)
{
  char *dptr = data, ch;
  while(ch = *(dptr++)) if(((ch < 48)||(ch > 57))&&(ch != '.')&&(ch != '-')) return SYN_ASSIGN_FLOAT;
  return NO_ERRORS;
}

int checkSpeedup(char *data)
{
  if(strcmp(data, "NONE")&&strcmp(data, "MEDIUM")&&strcmp(data, "HIGH"))
    return SYN_ASSIGN_SPEEDUP;
  else
    return NO_ERRORS;
}

int checkCurveType(int paramId, char *data)
{
  int i;
  for(i = 0; i < CURVE_NUM; i++) {
    if(paramId == PARAM_NMX_TYPE && i == CURVE_CUSTOM) break;
    if(!strcmp(data, CURVETYPE_LIST[i])) return NO_ERRORS;    
  }
  if(i == CURVE_CUSTOM) return SYN_ASSIGN_NMX_CUSTOM;
  return SYN_ASSIGN_CURVE_TYPE;
}

int checkBounds(int paramId, char *data)
{
  float dataVal = atof(data);
  if(UBOUND_LIST[paramId] > 0) {      
    if((dataVal <= 0)||(dataVal < LBOUND_LIST[paramId])||(dataVal > UBOUND_LIST[paramId]))
      return SYN_BOUNDS_ERROR;
  } else {
    if((dataVal <= 0)||(dataVal < LBOUND_LIST[paramId]))
      return SYN_BOUNDS_ERROR;
  }
  return NO_ERRORS;
}

int checkBounds(char *data, float lower, float upper) 
{
  float dataVal = atof(data);
  if((dataVal < lower)||(dataVal > upper)) return SYN_BOUNDS_ERROR;
  return NO_ERRORS;
}  

int CheckParameter(int paramId, char *data)
{
  int error;
  switch(PARAMTYPE_LIST[paramId])
    {
    case PARAM_INTEGER:
      error = checkInteger(data);
      if(!error) error = checkBounds(paramId, data);
      break;
    case PARAM_FLOAT:
      error = checkFloatingPoint(data);
      if(!error) error = checkBounds(paramId, data);
      break;
    case PARAM_SPEEDUP_TYPE:
      error = checkSpeedup(data);
      break;
    case PARAM_CURVE_TYPE:
      error = checkCurveType(paramId, data);
      break;
    }
  return error;
} 

int CheckList(CmCParser *synParser, CmCToken **token)
{
  //check for initial left curl bracket
  (*token) = synParser->GetToken();  
  if((*token)->token_[0] != '{') return SYN_MISSING_LEFT_CURL;

  //read in each data point
  do {
    (*token) = synParser->GetToken();
    if((*token)->token_[0] != '(') return SYN_MISSING_LEFT_PARENTHESIS;
    (*token) = synParser->GetToken();
    if(checkFloatingPoint((*token)->token_)) return SYN_INCOMPLETE_LIST;
    if(checkBounds((*token)->token_, 0, 1)) return SYN_ARG_BOUNDS_ERROR;
    (*token) = synParser->GetToken();
    if((*token)->token_[0] != ',') return SYN_MISSING_COMMA;
    (*token) = synParser->GetToken();
    if(checkFloatingPoint((*token)->token_)) return SYN_INCOMPLETE_LIST;
    if(checkBounds((*token)->token_, 0, 1)) return SYN_ARG_BOUNDS_ERROR;
    (*token) = synParser->GetToken();
    if((*token)->token_[0] != ')') return SYN_MISSING_RIGHT_PARENTHESIS;
    (*token) = synParser->GetToken();
    if(((*token)->token_[0] != ',') && ((*token)->token_[0] != '}')) return SYN_MISSING_RIGHT_CURL;
  } while((*token)->token_[0] != '}');

  //the point list has proper structure!
  return NO_ERRORS;
}

int checkSupported(int filetype)
{
  int i;
  for(i = 0; i < SUPPORT_NUM; i++) {
    if(filetype == SUPPORTED_FILETYPE_LIST[i]) return NO_ERRORS;
  }  
  return SYN_UNSUPPORTED_FILETYPE;
}

//************************
//check syntax
//************************

CmCError CheckSyntax(char *filename, CmCParser *synParser)
{

  //*****************************************
  //parse the file
  //*****************************************

  int error;
  synParser->SetDelimiters("=(){},;'");
  synParser->StoreDelimiters(true);
  error = synParser->Parse(filename);
  if(error) return (CmCError((CmCToken *) NULL, SYN_INVALID_FILE));

  //*****************************************
  //check syntax
  //*****************************************

  int  curveId;
  int  parameterId;
  int  commandTypeId, commandId;
  CmCToken *token, *(tokenSet)[MAX_TOKEN_NUM];
  while(token = synParser->GetToken())
    {
      //identify the command...
      commandId     = Command(token->token_);
      commandTypeId = CommandType(commandId);

      //based on the command, identify the number of tokens
      //associated with it and obtain those tokens
      error = GetTokenSet(commandId, tokenSet, synParser);

      //if the specified number of tokens does not exist
      //then return an error
      if(error == SYN_ERROR) return CmCError((CmCToken *) NULL, error);

      //if the specified tokens exist, then check them for
      //their validity

      switch(commandTypeId)
	{
	case CMD_IO:
	  switch(commandId)
	    {
	    case CMD_SAVE:
	      //check structure
	      if(strcmp(tokenSet[0]->token_, "(")) return CmCError(tokenSet[0], SYN_MISSING_LEFT_PARENTHESIS);
	      if(strcmp(tokenSet[1]->token_, "'")) return CmCError(tokenSet[1],SYN_MISSING_QUOTATION);
	      if(strcmp(tokenSet[3]->token_, "'")) return CmCError(tokenSet[3],SYN_MISSING_QUOTATION);
	      if(strcmp(tokenSet[4]->token_, ",")) return CmCError(tokenSet[4],SYN_MISSING_COMMA);
	      if(strcmp(tokenSet[6]->token_, ",")) return CmCError(tokenSet[6],SYN_MISSING_COMMA);
	      if(strcmp(tokenSet[8]->token_, ")")) return CmCError(tokenSet[8],SYN_MISSING_RIGHT_PARENTHESIS);
	      if(strcmp(tokenSet[9]->token_, ";")) return CmCError(tokenSet[9],SYN_MISSING_SEMICOLON);

	      //check constants
	      if(!validFileType(tokenSet[5]->token_))  return CmCError(tokenSet[5],SYN_INVALID_FILETYPE);
	      if(!validOutputType(tokenSet[7]->token_))return CmCError(tokenSet[7],SYN_INVALID_OUTPUTTYPE);
	      if(checkSupported(FileType(tokenSet[5]->token_))) 
		 return CmCError(tokenSet[5],SYN_UNSUPPORTED_FILETYPE);
	      break;

	    case CMD_LOAD:
	      //check structure
	      if(strcmp(tokenSet[0]->token_, "(")) return CmCError(tokenSet[0],SYN_MISSING_LEFT_PARENTHESIS);
	      if(strcmp(tokenSet[1]->token_, "'")) return CmCError(tokenSet[1],SYN_MISSING_QUOTATION);
	      if(strcmp(tokenSet[3]->token_, "'")) return CmCError(tokenSet[3],SYN_MISSING_QUOTATION);
	      if(strcmp(tokenSet[4]->token_, ",")) return CmCError(tokenSet[4],SYN_MISSING_COMMA);
	      if(strcmp(tokenSet[6]->token_, ")")) return CmCError(tokenSet[6],SYN_MISSING_RIGHT_PARENTHESIS);
	      if(strcmp(tokenSet[7]->token_, ";")) return CmCError(tokenSet[7],SYN_MISSING_SEMICOLON);

	      //check constants
	      if(!validInputType(tokenSet[5]->token_)) return CmCError(tokenSet[5],SYN_INVALID_INPUTTYPE);
	      break;
	      
	    case CMD_USE_RESULT:
	      if(strcmp(tokenSet[0]->token_, "(")) return CmCError(tokenSet[0], SYN_MISSING_LEFT_PARENTHESIS);
	      if(strcmp(tokenSet[2]->token_, ")")) return CmCError(tokenSet[2], SYN_MISSING_RIGHT_PARENTHESIS);
	      if(strcmp(tokenSet[3]->token_, ";")) return CmCError(tokenSet[3], SYN_MISSING_SEMICOLON);
	      if((OutputType(tokenSet[1]->token_) != OUTPUT_SEGM_IMAGE) &&
		 (OutputType(tokenSet[1]->token_) != OUTPUT_FILT_IMAGE)) {
		if(OutputType(tokenSet[1]->token_) != OUTPUT_UNKNOWN) {
		  return CmCError(tokenSet[1], SYN_ASSIGN_INVALID_ARG);
		} else {
		  return CmCError(tokenSet[1], SYN_INVALID_OUTPUTTYPE);
		}
	      }
	      break;
	    }
	  break;
       
	case CMD_EXECUTION:
	  if(tokenSet[0]->token_[0] != ';') return CmCError(tokenSet[0],SYN_MISSING_SEMICOLON);
	  break;
	case CMD_FLAGS:
	  if(!validFlag(tokenSet[0]->token_)) return CmCError(tokenSet[0],SYN_INVALID_FLAG);
	  if(tokenSet[1]->token_[0] != ';')   return CmCError(tokenSet[1],SYN_MISSING_SEMICOLON);
	  break;
	//unknown command
	default:
	  break;
	}

      //if its not a command, then maybe its a parameter
      if(commandTypeId == UNKNOWN_COMMAND) {
	//get the parameter type
	parameterId = Parameter(token->token_);

	if(parameterId != UNKNOWN_PARAMETER) {
	  
	  //retreive tokens expected given a parameter
	  error = GetParamTokenSet(tokenSet, synParser);
	  if(error == SYN_ERROR) return CmCError(token, SYN_ERROR);
	  
	  //check those tokens for validity
	  if(strcmp(tokenSet[0]->token_, "=")) return CmCError(tokenSet[0],SYN_MISSING_EQUALS);
	  if(strcmp(tokenSet[2]->token_, ";")) return CmCError(tokenSet[2],SYN_MISSING_SEMICOLON);

	  //make sure parameter is of the right type
	  error = CheckParameter(parameterId, tokenSet[1]->token_);
	  if(error) return CmCError(token, error);

 	//if its an unknown parameter then maybe its a curve list
	} else {
	  //get custom curve
	  curveId = CustomCurve(token->token_);
	    
	  //if its not a custom curve list then flag an error
	  if(curveId == UNKNOWN_CURVE) return CmCError(token,SYN_INVALID_PARAMCMD);

	  //check for equals
	  token = synParser->GetToken();
	  if(token->token_[0] != '=') return CmCError(token,SYN_MISSING_EQUALS);

	  //if its a curve list, then check that a proper point list
	  //is provided
	  error = CheckList(synParser, &token);
	  if(error) return CmCError(token, error);

	  //check for semicolon
	  token = synParser->GetToken();
	  if(token->token_[0] != ';') return CmCError(token,SYN_MISSING_SEMICOLON);
	}
      }
      
      //command/parameter identified and verified for
      //its validity, next command/parameter
    }

  //reset parser
  synParser->StartOver();
  
  //file is syntaxically correct
  return CmCError((CmCToken *) NULL, NO_ERRORS);
  
}

////////////////////////////////////////////////////////
//Run Script
////////////////////////////////////////////////////////

void *convert(int paramId, char *data)
{
  switch(PARAMTYPE_LIST[paramId]) {
  case PARAM_INTEGER: {
    int *idata = new int;
    if(!idata) return (void *) NULL;
    *idata     = atoi(data);
    return (void *) idata;
  }
  break;
  case PARAM_FLOAT: {
    float *fdata = new float;
    if(!fdata) return (void *) NULL;
    *fdata       = atof(data);
    return (void *) fdata;
  }
  break;
  case PARAM_SPEEDUP_TYPE: {
    int *speed = new int;
    if(!speed) return (void *) NULL;
    int i;
    for(i = 0; i < SPEED_NUM; i++)
      if(!strcmp(data, SPEEDTYPE_LIST[i])) {
	*speed = i;
	break;
      }
    return (void *) speed;
  }
  break;
  default: {
    int *curve = new int;
    if(!curve) return (void *) NULL;
    int i;
    for(i = 0; i < CURVE_NUM; i++)
      if(!strcmp(data, CURVETYPE_LIST[i])) {
	*curve = i;
	break;
      }
    return (void *) curve;
  }
  break;
  }
}

int findMissing(void **paramTable, int cmd)
{
  switch(cmd) {
  case CMD_SEGMENT:
    if(!paramTable[PARAM_SPATIAL_BANDWIDTH]) return PARAM_SPATIAL_BANDWIDTH;
    if(!paramTable[PARAM_RANGE_BANDWIDTH]) return PARAM_RANGE_BANDWIDTH;
    if(!paramTable[PARAM_MINIMUM_REGION_AREA]) return PARAM_MINIMUM_REGION_AREA;
    if(!paramTable[PARAM_SPEEDUP]) return PARAM_SPEEDUP;
    break;
  case CMD_FILTER:
    if(!paramTable[PARAM_SPATIAL_BANDWIDTH]) return PARAM_SPATIAL_BANDWIDTH;
    if(!paramTable[PARAM_RANGE_BANDWIDTH]) return PARAM_RANGE_BANDWIDTH;
    if(!paramTable[PARAM_SPEEDUP]) return PARAM_SPEEDUP;
    break;
  case CMD_FUSE:
    if(!paramTable[PARAM_RANGE_BANDWIDTH]) return PARAM_RANGE_BANDWIDTH;
    if(!paramTable[PARAM_MINIMUM_REGION_AREA]) return PARAM_MINIMUM_REGION_AREA;
    if(!paramTable[PARAM_SPEEDUP]) return PARAM_SPEEDUP;
    break;
  case CMD_EDGE_DETECT: {
    if(!paramTable[PARAM_GRADIENT_WINDOW_RADIUS]) return PARAM_GRADIENT_WINDOW_RADIUS;
    int paramId;
    for(paramId = PARAM_MINIMUM_LENGTH; paramId <= PARAM_HYSTERISIS_LOW_TYPE; paramId++) {
      if(!paramTable[paramId]) return paramId;
    }
    break;
  }  
  default:
    break;
  }

  if(CmCSynergistic) {
    int paramId;
    for(paramId = PARAM_GRADIENT_WINDOW_RADIUS; paramId <= PARAM_EDGE_STRENGTH_THRESHOLD; paramId++) {
      if(!paramTable[paramId]) return paramId;
    }
  } 
  return UNKNOWN_PARAMETER;
}

int SET(int paramId, CmCParser *script, void **paramTable)
{
  //get data
  script->GetToken(); //skip equals
  CmCToken *token = script->GetToken(); //get data
  char *data = token->token_;
  script->GetToken(); //skip semi-colon

  //monitor synergistic parameters
  if(paramId == PARAM_GRADIENT_WINDOW_RADIUS) {
    int *idata = (int *) convert(paramId, data);
    if(!idata) return EXE_OUT_OF_MEMORY;
    int *old   = (int *)(paramTable[paramId]);
    if((!old) || (*idata != *old)) {
      if(old) delete old;
      paramTable[paramId] = (void *) idata;
      CmCGradWinChanged   = true;
      return NO_ERRORS;
    }
    delete idata;
    return NO_ERRORS;
  }
  if(paramId == PARAM_MIXTURE_PARAMETER) {
    float *fdata = (float *) convert(paramId, data);
    if(!fdata) return EXE_OUT_OF_MEMORY;
    float *old   = (float *)(paramTable[paramId]);
    if((!old) || (*fdata != *old)) {
      if(old) delete old;
      paramTable[paramId] = (void *) fdata;
      CmCMixtureChanged   = true;
      return NO_ERRORS;
    }
    delete fdata;
    return NO_ERRORS;
  }

  //de-allocate memory used for old data
  if(paramTable[paramId]) {
    if(PARAMTYPE_LIST[paramId] == PARAM_FLOAT) {
      delete (float *) paramTable[paramId];
    } else {
      delete (int *) paramTable[paramId];
    }
  }
  
  //convert data to appropriate data type
  //and place it onto the parameter table
  if(!(paramTable[paramId] = convert(paramId, data))) return EXE_OUT_OF_MEMORY;

  //parameter set successfully!
  return NO_ERRORS;
} 

const int MAX_SIZE = 1000;
int READLIST(int curveId, CmCParser *script, void **paramTable)
{
  //traverse the and read the point list into
  //the temporary buffer (NOTE: a maximum of MAX_SIZE points
  //may be read into the point list)
  float tempBuf[2*MAX_SIZE];
  CmCToken *token;
  script->GetToken(); //skip "={"
  script->GetToken();
  int n = 0;
  do {
    if(n >= MAX_SIZE) return EXE_LONG_POINT_LIST;
    script->GetToken(); //skip '('
    token = script->GetToken(); //read x-coordinate
    tempBuf[2*n] = (float) atof(token->token_);
    script->GetToken(); //skip ','
    token = script->GetToken(); //read y-coordinate
    tempBuf[2*n+1] = (float) atof(token->token_);
    script->GetToken(); //skip '),'
    token = script->GetToken();
    n++; //next data point
  } while(token->token_[0] != '}');
  script->GetToken(); //skip ';'

  //create point list
  float *data = new float[2*n];
  if(!data) return EXE_OUT_OF_MEMORY;
  memcpy(data, tempBuf, 2*n*sizeof(float));
  
  //place point list into parameter table
  paramTable[PARAM_NUM + 2*curveId] = (void *) data;
  int *nPts = new int;
  if(!nPts) return EXE_OUT_OF_MEMORY;
  *nPts = n;
  paramTable[PARAM_NUM + 2*curveId + 1] = (void *) nPts;

  //point list has been read successfully!
  return NO_ERRORS;
}

int EXECUTE(int cmdId, CmCParser *script, void **paramTable)
{
  switch(cmdId) {

    //***************************************************
    //set global flags
    //***************************************************

  case CMD_SYNERGISTIC: {
    CmCToken *token;
    token = script->GetToken(); //get flag
    if(!strcmp(token->token_, "ON")) {
      CmCSynergistic = true;
      CmCPrompt("Synergistic Segmentation ENABLED.\n");
    } else {
      CmCSynergistic = false;
      CmCPrompt("Synergistic Segmentation DISABLED.\n");
    }
    script->GetToken(); //skip ';'
    break;
  }

  case CMD_DISPLAY_PROGRESS: {
    CmCToken *token;
    token = script->GetToken(); //get flag
    if(!strcmp(token->token_, "ON"))
      CmCDisplayProgress = true;
    else
      CmCDisplayProgress = false;
    script->GetToken(); //skip ';'
    CmCPrompt("Display progress ENABLED.\n");
    break;
  }

  case CMD_USE_CUSTOM_WEIGHT_MAP: {
    CmCToken *token;
    token = script->GetToken(); //get flag
    if(!strcmp(token->token_, "ON")) {
      CmCUseCustomWeightMap = true;
      CmCPrompt("Custum weight map IN-USE (if defined).\n");
    } else {
      CmCUseCustomWeightMap = false;
      CmCPrompt("Custum weight map IN-ACTIVE.\n");
    }
    script->GetToken(); //skip ';'
    break;
  }

    //***************************************************
    //load a file
    //***************************************************

  case CMD_LOAD: {
    CmCToken *token;
    script->GetToken(); //skip "('"
    script->GetToken();
    token = script->GetToken(); //get filename
    char *filename = new char[strlen(token->token_)+1];
    strcpy(filename, token->token_);
    script->GetToken(); //skip "',"
    script->GetToken();
    token = script->GetToken(); //get input type
    int inputtype = InputType(token->token_);
    script->GetToken(); //skip ");"
    script->GetToken();    

    //load file
    int error = edison.Load(filename, inputtype);
    if(!error) CmCPrompt("File '%s' has been successfully loaded!\n", filename);
    delete [] filename;
   
    //return any errors
    if(error) return error;
    
    break;
  }

    //***************************************************
    //save a file
    //***************************************************

  case CMD_SAVE: {
    CmCToken *token;
    script->GetToken(); //skip "('"
    script->GetToken();
    token = script->GetToken(); //get filename
    char *filename = new char [strlen(token->token_) + 1];
    strcpy(filename, token->token_);
    script->GetToken(); //skip "',"
    script->GetToken();
    token = script->GetToken(); //filetype
    int filetype = FileType(token->token_);
    script->GetToken(); //skip ','
    token = script->GetToken(); //get output type
    int outputtype = OutputType(token->token_);
    script->GetToken(); //skip ");"
    script->GetToken();
    
    //save file
    int error = edison.Save(filename, filetype, outputtype);
    if(!error) CmCPrompt("File '%s' has been successfully saved!\n", filename);
    delete [] filename;
    
    //return any errors
    if(error) return error;
    break;
  }

    //***************************************************
    //route output to input
    //***************************************************

  case CMD_USE_RESULT: {      
    CmCToken *token;
    script->GetToken(); //skip '('
    token = script->GetToken(); //get output type
    int outputtype = OutputType(token->token_);
    script->GetToken(); //skip ");"
    script->GetToken();

    //route output to input
    int error = edison.UseResult(outputtype);
    if(!error) {
      if(outputtype == OUTPUT_SEGM_IMAGE) {
	CmCPrompt("Segmented image result has been set as input.\n");
      } else {
	CmCPrompt("Filtered image result has been set as input.\n");
      }
    }      
    if(error) return error;
    break;
  }

    //***************************************************
    //edge detect the input image
    //***************************************************

  case CMD_EDGE_DETECT: {
    edison.SetParameters(paramTable);
    int error = edison.EdgeDetect();
    if(error) return error;
    script->GetToken(); //skip ';'
    break;
  }    

    //***************************************************
    //filter the input image
    //***************************************************

  case CMD_FILTER: {
    edison.SetParameters(paramTable);
    int error = edison.Filter();    
    if(error) return error;
    script->GetToken(); //skip ';'
    break;
  }

    //***************************************************
    //fuse the regions of the input image
    //***************************************************

  case CMD_FUSE: {    
    edison.SetParameters(paramTable);
    int error = edison.Fuse();
    if(error) return error;
    script->GetToken(); //skip ';'
    break;
  }
    //***************************************************
    //segment the input image
    //***************************************************

  case CMD_SEGMENT: {    
    edison.SetParameters(paramTable);
    int error = edison.Segment();
    if(error) return error;
    script->GetToken(); //skip ';'
    break;
  }

    //***************************************************
    //does nothing
    //***************************************************

  default:
    break;
  }

  //command executed succesfully!
  return NO_ERRORS;

}    

//de-allocates the parameter table
void DEALLOCATE(void **paramTable)
{
  int i;
  for(i = 0; i < PARAM_NUM + 2*CUST_CURVE_NUM; i++) {
    if(paramTable[i]) {
      switch(PARAMTYPE_LIST[i]) {
      case PARAM_FLOAT: {
	float *fdata = (float *) paramTable[i];
	delete fdata;
	paramTable[i] = (void *) NULL;
	break;
      } 
      default: {
	  int *idata = (int *) paramTable[i];
	  delete idata;
	  paramTable[i] = (void *) NULL;
	  break;
	}
      }
    }
  }
}     

CmCError Run(CmCParser *script)
{
  
  //define and initialize parameter table
  void *(paramTable)[PARAM_NUM + 2*CUST_CURVE_NUM];
  int i;
  for(i = 0; i < PARAM_NUM + 2*CUST_CURVE_NUM; i++)
    paramTable[i] = (void *) NULL;
  
  //parse script, setting parameters and
  //running commands along the way...
  int      tokenId, error;
  CmCToken *token;
  while(token = script->GetToken()) { 
    //determine type of token (parameter or command)
    tokenId = Command(token->token_);

    //token is a command, so run it
    if(tokenId != UNKNOWN_COMMAND) {
      error = EXECUTE(tokenId, script, paramTable);
      if(error) {
	if(error == EXE_MISSING_PARAM) {
	  CmCToken *ertok = new CmCToken((char *) PARAM_LIST[findMissing(paramTable, Command(token->token_))],
					 token->lineNumber_);
	  return CmCError(ertok, error);
	} else {
	  return CmCError(token, error);
	}
      }	
    }
    //token is a parameter, so set its value
    else {
      tokenId = Parameter(token->token_);
      if(tokenId != UNKNOWN_PARAMETER) {
	error = SET(tokenId, script, paramTable);
	if(error) return CmCError(token, error);
      //token is custum curve list, so read it
      } else {
	tokenId = CustomCurve(token->token_);
	error = READLIST(tokenId, script, paramTable);
	if(error) return CmCError(token, error);
      }
    }
  }
  
  //done running script...

  //de-allocate parameter table
  DEALLOCATE(paramTable);
  
  //reset script parser
  script->StartOver();
  
  //script has been run succesfully!
  return CmCError((CmCToken *) NULL, NO_ERRORS);

}

////////////////////////////////////////////////////////
//I/O Filters
////////////////////////////////////////////////////////

//get the fileformat of an image
int CmCGetImageFormat(char *filename)
{
  if(!filename) return EXE_NULL_PTR;
  FILE *fp = fopen(filename, "rb");
  if(!fp) return EXE_FILE_READ_ERROR;
  char buf[3];
  if(!fgets(buf, 3, fp)) return EXE_FILE_READ_ERROR;  
  fclose(fp);
  int i;
  for(i = 0; i < FILETYPE_NUM-1; i++) {
    if(!strncmp(buf, FILETYPE_HEADER_LIST[i], 2)) return i;
  }
  return FILE_UNKNOWN;
}

//converts an image into floating point format
float *CmCConvertToFloat(unsigned char *image, int height, int width, int dim)
{
  float *data = new float[height * width * dim];
  if(!data) return (float *) NULL;
  int i;
  for(i = 0; i < height * width * dim; i++) {
    data[i] = (float)(image[i]);
  }
  return data;
}  

//converts an image from color to grayscale
const double W1 = 0.299;
const double W2 = 0.587;
const double W3 = 0.114;
unsigned char *CmCConvertToGrayscale(unsigned char *image, int height, int width)
{
  unsigned char *data = new unsigned char [height * width];
  if(!data) return (unsigned char *) NULL;
  int i;
  double r, g, b;
  for(i = 0; i < height * width; i++) {
    r = (double)(image[3*i]);
    g = (double)(image[3*i+1]);
    b = (double)(image[3*i+2]);
    data[i] = (unsigned char)(r*W1 + g*W2 + b*W3);
  }
  return data;
} 

//write image data
int CmCWriteImage(char *filename, unsigned char *image, int height, int width, int dim, int filetype)
{
  int error, i;
  unsigned char *data;
  switch(filetype) {
  case FILE_PPM:
    data = image;
    if(dim == 1) {
      data = new unsigned char [height * width * 3];
      if(!data) return EXE_OUT_OF_MEMORY;
      for(i = 0; i < height*width; i++) {
	data[3*i] = data[3*i+1] = data[3*i+2] = image[i];
      }
    }      
    error = writePPMImage(filename, data, height, width, 255, (char *) NULL);
    if(data != image) delete [] data;
    break;
  case FILE_PGM:
    data = image;
    if(dim == 3) {
      data = CmCConvertToGrayscale(image, height, width);
      if(!data) return EXE_OUT_OF_MEMORY;
    }
    error = writePGMImage(filename, data, height, width, 255, (char *) NULL);
    if(data != image) delete [] data;
    break;
  case FILE_PNM: {
    bool color;
    if(dim == 3) {
      color = true;
    } else {
      color = false;
    } 
    error = writePNMImage(filename, image, height, width, 255, color, (char *) NULL);
    break;
  }
  case FILE_MATLAB_ASCII: {    
    float *fdata = CmCConvertToFloat(image, height, width, dim);
    if(!fdata) return EXE_OUT_OF_MEMORY;    
    error = CmCWriteMFile(filename, fdata, height, width, dim);
    delete [] fdata;
    break;
  }      
  default:
     return EXE_UNSUPPORTED_FILE_FORMAT;
  }
  if(!error)
    return NO_ERRORS;
  else
    return EXE_FILE_WRITE_ERROR;
}

//write Matlab ASCII file
int CmCWriteMFile(char *filename, float *data, int rows, int cols, int n)
{
  if(!filename || !data) return EXE_NULL_PTR;
  FILE *fp = fopen(filename, "wb");
  if(!fp) return EXE_FILE_WRITE_ERROR;
  int i, j, k;  
  for(i = 0; i < rows; i++) {
    for(j = 0; j < cols-1; j++) {
      for(k = 0; k < n; k++) {	
	fprintf(fp, "%f ", data[n*(rows*i+j)+k]);
      }
    }
    for(k = 0; k < n-1; k++) {
      fprintf(fp, "%f ", data[n*(rows*i+cols-1)+k]);
    }
    fprintf(fp, "%f\n", data[n*(rows*i+cols-1)+n-1]);
    }
  fclose(fp);  
  return NO_ERRORS;
}

//read image data
int CmCReadImage(char *filename, unsigned char **image, int& height, int& width, int& dim)
{  
  if(!filename) return EXE_NULL_PTR;
  int filetype = CmCGetImageFormat(filename);
  int error = filetype; //possible error in reading file header
  switch(filetype) {
  case FILE_PPM:
    error = readPPMImage(filename, image, height, width, dim);
    dim   = 3;
    break;
  case FILE_PGM:
    error = readPGMImage(filename, image, height, width, dim);
    dim   = 1;
    break;
  case FILE_PNM: {
    bool color;
    error = readPNMImage(filename, image, height, width, dim, color);
    if(color) {
      dim = 3;
    } else {
      dim = 1;
    }
    break;
  }
  default:
    if(filetype == FILE_UNKNOWN) return EXE_UNKNOWN_FILE_FORMAT;
  }

  if(!error)
    {
      //due to LUV conversion must always work in 5D
      if(dim == 1) {
	dim = 3;
	unsigned char *rgbImage = new unsigned char [height * width * dim];
	int i;
	for(i = 0; i < height * width; i++) {
	  rgbImage[3*i] = rgbImage[3*i+1] = rgbImage[3*i+2] = (*image)[i];
	}
	delete [] *image;
	*image = rgbImage;
      }
      return NO_ERRORS;
    }
  else
    return EXE_FILE_READ_ERROR;
}

//read Matlab ASCII file
int CmCReadMFile(char *filename, float **data, int rows, int cols)
{
  if(!filename || !data) return EXE_NULL_PTR;
  FILE *fp = fopen(filename, "rb");
  if(!fp) return EXE_FILE_READ_ERROR;
  *data = new float [rows * cols];
  if(!data) return EXE_OUT_OF_MEMORY;
  int i;
  for(i = 0; i < rows*cols; i++) {
    if(fscanf(fp, "%f", &((*data)[i])) == EOF) return EXE_FILE_READ_ERROR;
  }
  fclose(fp);
  return NO_ERRORS;
}


