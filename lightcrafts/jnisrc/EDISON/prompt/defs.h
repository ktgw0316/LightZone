////////////////////////////////////////////////////////
// Name     : defs.h
// Purpose  : Command/Parameter Definitions
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#ifndef DEFS_H
#define DEFS_H

//define constants
#define MAX_TOKEN_NUM  10

//define counts
#define SPEED_NUM       3
#define CUST_CURVE_NUM  2
#define CURVE_NUM       6
#define CMD_NUM        10
#define PARAM_NUM      17
#define OUTPUTTYPE_NUM  9
#define INPUTTYPE_NUM   2
#define FILETYPE_NUM    7
#define SUPPORT_NUM     4

//define command types
enum {
  CMD_IO,
  CMD_EXECUTION,
  CMD_FLAGS,
};

//define commands
enum {
  CMD_SAVE,
  CMD_LOAD,
  CMD_USE_RESULT,
  CMD_EDGE_DETECT,
  CMD_FILTER,
  CMD_FUSE,
  CMD_SEGMENT,
  CMD_SYNERGISTIC,
  CMD_DISPLAY_PROGRESS,
  CMD_USE_CUSTOM_WEIGHT_MAP,
  UNKNOWN_COMMAND
};

//define parameters
enum {
  PARAM_SPATIAL_BANDWIDTH,
  PARAM_RANGE_BANDWIDTH,
  PARAM_MINIMUM_REGION_AREA,
  PARAM_GRADIENT_WINDOW_RADIUS,
  PARAM_MIXTURE_PARAMETER,
  PARAM_EDGE_STRENGTH_THRESHOLD,
  PARAM_SPEEDUP,
  PARAM_MINIMUM_LENGTH,
  PARAM_NMX_RANK,
  PARAM_NMX_CONF,
  PARAM_NMX_TYPE,
  PARAM_HYSTERISIS_HIGH_RANK,
  PARAM_HYSTERISIS_HIGH_CONF,
  PARAM_HYSTERISIS_HIGH_TYPE,
  PARAM_HYSTERISIS_LOW_RANK,
  PARAM_HYSTERISIS_LOW_CONF,
  PARAM_HYSTERISIS_LOW_TYPE,
  UNKNOWN_PARAMETER
};

//define parameter types
enum {
  PARAM_INTEGER,
  PARAM_FLOAT,
  PARAM_SPEEDUP_TYPE,
  PARAM_CURVE_TYPE
};

//define file types
enum {
  FILE_PPM,
  FILE_PGM,
  FILE_PNG,
  FILE_PCX,
  FILE_PNM,
  FILE_JPG,
  FILE_MATLAB_ASCII,
  FILE_UNKNOWN
};

//define supported file types list
const int SUPPORTED_FILETYPE_LIST[SUPPORT_NUM] = {
  FILE_PPM,
  FILE_PGM,
  FILE_PNM,
  FILE_MATLAB_ASCII
};

//define input types
enum {
  INPUT_IMAGE,
  INPUT_MAP,
  INPUT_UNKNOWN
};

//define output types
enum {
  OUTPUT_SEGM_BOUNDARIES,
  OUTPUT_SEGM_IMAGE,
  OUTPUT_SEGM_IMAGE_BOUNDARIES,
  OUTPUT_FILT_IMAGE,
  OUTPUT_FILT_IMAGE_BOUNDARIES,
  OUTPUT_GRADIENT_MAP,
  OUTPUT_CONFIDENCE_MAP,
  OUTPUT_WEIGHT_MAP,
  OUTPUT_EDGES,
  OUTPUT_UNKNOWN
};

//define curve types
enum {
  CURVE_ARC,
  CURVE_VERTICAL_LINE,
  CURVE_HORIZONTAL_LINE,
  CURVE_LINE,
  CURVE_BOX,
  CURVE_CUSTOM
};

//define custum curve parameters
enum {
  CUST_CURVE_HYST_HIGH,
  CUST_CURVE_HYST_LOW,
  UNKNOWN_CURVE
};

//define custom curve list
const char CUST_CURVE_LIST[CUST_CURVE_NUM][25] = {
  "CustomCurveHystHigh",
  "CustomCurveHystLow"
};

//define command type list
const int CMD_TYPE_LIST[CMD_NUM+1] = {
  CMD_IO,
  CMD_IO,
  CMD_IO,
  CMD_EXECUTION,
  CMD_EXECUTION,
  CMD_EXECUTION,
  CMD_EXECUTION,
  CMD_FLAGS,
  CMD_FLAGS,
  CMD_FLAGS,
  UNKNOWN_COMMAND,
};

//define command list
const char CMD_LIST[CMD_NUM][25] = {
  "Save",
  "Load",
  "UseResult",
  "EdgeDetect",
  "Filter",
  "Fuse",
  "Segment",
  "Synergistic",
  "DisplayProgress",
  "UseCustomWeightMap"
};

//define parameter list
const char PARAM_LIST[PARAM_NUM][25] = {
  "SpatialBandwidth",
  "RangeBandwidth",
  "MinimumRegionArea",
  "GradientWindowRadius",
  "MixtureParameter",
  "EdgeStrengthThreshold",
  "Speedup",
  "MinimumLength",
  "NmxRank",
  "NmxConf",
  "NmxType",
  "HysterisisHighRank",
  "HysterisisHighConf",
  "HysterisisHighType",
  "HysterisisLowRank",
  "HysterisisLowConf",
  "HysterisisLowType"
};

//define parameter type list
const int PARAMTYPE_LIST[PARAM_NUM + 2*CUST_CURVE_NUM] = {
  PARAM_INTEGER,
  PARAM_FLOAT,
  PARAM_INTEGER,
  PARAM_INTEGER,
  PARAM_FLOAT,
  PARAM_FLOAT,
  PARAM_SPEEDUP_TYPE,
  PARAM_INTEGER,
  PARAM_FLOAT,
  PARAM_FLOAT,
  PARAM_CURVE_TYPE,
  PARAM_FLOAT,
  PARAM_FLOAT,
  PARAM_CURVE_TYPE,
  PARAM_FLOAT,
  PARAM_FLOAT,
  PARAM_CURVE_TYPE,
  PARAM_FLOAT,
  PARAM_INTEGER,
  PARAM_FLOAT,
  PARAM_INTEGER
};

//define parameter lower bound list
const float LBOUND_LIST[PARAM_NUM] = {
  0, 0, 1, 1, 0,
  0, 0, 1, 0, 0,
  0, 0, 0, 0, 0,
  0, 0 
};

//define parameter lower bound list
const float UBOUND_LIST[PARAM_NUM] = {
  -1, -1, -1, -1, 1,
   1, -1, -1,  1, 1,
  -1,  1,  1, -1, 1,
   1, -1 
};

//define bounds message constants
enum {
  GREATER_THAN_ZERO,
  GREATER_THAN_ONE,
  BETWEEN_ZERO_ONE,
  NOT_APPLICABLE
};

//define parameter bounds error table
const char boundsTable[4][40] = {
  "Must take a value > 0.",
  "Must take a value >= 1.",
  "Must take a value between 0 and 1.",
  ""
};

//define parameter bounds error lookup table
const int BOUNDS_TABLE[PARAM_NUM] = {
  GREATER_THAN_ZERO,
  GREATER_THAN_ZERO,
  GREATER_THAN_ONE,
  GREATER_THAN_ONE,
  BETWEEN_ZERO_ONE,
  BETWEEN_ZERO_ONE,
  NOT_APPLICABLE,
  GREATER_THAN_ONE,
  BETWEEN_ZERO_ONE,
  BETWEEN_ZERO_ONE,
  NOT_APPLICABLE,
  BETWEEN_ZERO_ONE,
  BETWEEN_ZERO_ONE,
  NOT_APPLICABLE,
  BETWEEN_ZERO_ONE,
  BETWEEN_ZERO_ONE,
  NOT_APPLICABLE
};

//define file type list
const char FILETYPE_LIST[FILETYPE_NUM][15] = {
  "PPM",
  "PGM",
  "PNG",
  "PCX",
  "PNM",
  "JPG",
  "MATLAB_ASCII"
};

//define file type header list
const char FILETYPE_HEADER_LIST[FILETYPE_NUM-1][3] = {
  "P6", "P5", "XX", "XX", "XX", "XX"
};

//define output type list
const char OUTPUTTYPE_LIST[OUTPUTTYPE_NUM][25] = {
  "SEGM_BOUNDARIES",
  "SEGM_IMAGE",
  "SEGM_IMAGE_BOUNDARIES",
  "FILT_IMAGE",
  "FILT_IMAGE_BOUNDARIES",
  "GRADIENT_MAP",
  "CONFIDENCE_MAP",
  "WEIGHT_MAP",
  "EDGES"
};

//define input type list
const char INPUTTYPE_LIST[INPUTTYPE_NUM][6] = {
  "IMAGE",
  "MAP"
};

//define speed type list
const char SPEEDTYPE_LIST[SPEED_NUM][7] = {
  "NONE",
  "MEDIUM",
  "HIGH"
};

//define curve type list
const char CURVETYPE_LIST[CURVE_NUM][20] = {
  "ARC",
  "VERTICAL_LINE",
  "HORIZONTAL_LINE",
  "LINE",
  "BOX",
  "CUSTOM"
};

//define syntax and execution error flags
enum {
  NO_ERRORS,
  SYN_ERROR,
  SYN_MISSING_SEMICOLON,
  SYN_MISSING_LEFT_PARENTHESIS,
  SYN_MISSING_RIGHT_PARENTHESIS,
  SYN_MISSING_LEFT_CURL,
  SYN_MISSING_RIGHT_CURL,
  SYN_MISSING_QUOTATION,
  SYN_MISSING_COMMA,
  SYN_MISSING_EQUALS,
  SYN_INCOMPLETE_LIST,
  SYN_ARG_BOUNDS_ERROR,
  SYN_INVALID_FILETYPE,
  SYN_INVALID_INPUTTYPE,
  SYN_INVALID_OUTPUTTYPE,
  SYN_INVALID_FLAG,
  SYN_INVALID_PARAMCMD,
  SYN_ASSIGN_INTEGER,
  SYN_ASSIGN_FLOAT,
  SYN_ASSIGN_SPEEDUP,
  SYN_ASSIGN_CURVE_TYPE,
  SYN_ASSIGN_NMX_CUSTOM,
  SYN_UNSUPPORTED_FILETYPE,
  SYN_ASSIGN_INVALID_ARG,
  EXE_ERROR,
  EXE_LONG_POINT_LIST,
  EXE_MISSING_PARAM,
  EXE_OUTPUT_UNDEFINED,
  EXE_INPUT_UNDEFINED,
  EXE_UNSUPPORTED_FILE_FORMAT,
  EXE_UNKNOWN_FILE_FORMAT,
  EXE_OUT_OF_MEMORY,
  EXE_FILE_WRITE_ERROR,
  EXE_FILE_READ_ERROR,
  EXE_NULL_PTR,
  EXE_POINT_LIST_HIGH,
  EXE_POINT_LIST_LOW,
  SYN_BOUNDS_ERROR,
  SYN_INVALID_FILE,
};

//define syntax error lookup table
const char ErrorMsgTable[36][50] = {
  "Unknown parameter or command format.",
  "Expected: ;",
  "Expected: (",
  "Expected: )",
  "Expected: {",
  "Expected: }",
  "Expected: '",
  "Expected: ,",
  "Expected: =",
  "Incomplete point list.",
  "Argument must be between 0 and 1.",
  "Unknown file type.",
  "Unknown input type.",
  "Unknown output type.",
  "Unknown flag.",
  "Invalid parameter or command.",
  "Integer data type expected.",
  "Floating point data type expected.",
  "Speedup enumeration expected.",
  "Curve type enumeration expected.",
  "Nmx suppr. curve may not be of a custom type.",
  "Unsupported file format.",
  "Invalid argument to function.",
  "Aborting Execution.",
  "Point list too large.",
  "Needed parameter undefined.",
  "Output un-defined",
  "Input image un-defined.",
  "Unsupported file format.",
  "Unknown file format.",
  "Out of memory.",
  "Un-able to write file.",
  "Un-able to read file.",
  "Internal error. (Please contact vendor)",
  "Hysterisis high custom curve is un-specified.",
  "Hysterisis low custom curve is un-specified."
};

////////////////////////////////////////////////////
//define global function prototypes
////////////////////////////////////////////////////

void CmCPrompt(const char *, ...);
unsigned char *CmCConvertToGrayscale(unsigned char *image, int height, int width);
int CmCWriteImage(char *filename, unsigned char *image, int height, int width, int dim, int filetype);
int CmCWriteMFile(char *filename, float *data, int rows, int cols, int dim);
int CmCReadImage(char *filename, unsigned char **image, int& height, int& width, int& dim);
int CmCReadMFile(char *filename, float **data, int rows, int cols);

#endif






