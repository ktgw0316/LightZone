////////////////////////////////////////////////////////
// Name     : edison.h
// Purpose  : Wrapper class used for segmenation and
//            edge detection.
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#ifndef EDISON_H
#define EDISON_H

//define mean shift actions
enum {
  EDISON_FILTER,
  EDISON_FUSE,
  EDISON_SEGMENT
};

class EDISON 
{
 public:

  //class constructor/destructor
  EDISON( void );
  ~EDISON( void );
  
  // *** Input/Output ***
  int Save(char *filename, int filetype, int outputtype);
  int Load(char *filename, int inputtype);
  int UseResult(int outputtype);

  // *** Set Parameters ***
  void SetParameters(void **parameterList);

  // *** Image Segmentation ***
  int Filter( void );
  int Fuse( void );
  int Segment( void );
  
  // *** Edge Detection ***
  int EdgeDetect( void );
  
 private:

  //store input image
  bool inputDefined_;
  int height_, width_, dim_;
  unsigned char *inputImage_;

  //store weight maps
  bool gradMapDefined_;
  bool confMapDefined_;
  bool weightMapDefined_;
  bool custMapDefined_;
  float *gradMap_;
  float *confMap_;
  float *weightMap_;
  float *custMap_;

  //store the output images
  bool filtImageDefined_;
  bool segmImageDefined_;
  unsigned char *filtImage_;
  unsigned char *segmImage_;
  
  //store the output edges and boundaries
  bool edgesDefined_;
  bool boundariesDefined_;
  int *edges_, numEdges_;
  int *boundaries_, numBoundaries_;

  //store the parameters
  void **parameterList_;

  int writeImage(char *filename, unsigned char *image, int *dataPoints, int n, int filetype);
  int saveData(char *filename, float *data, int filetype);
  int loadImage(char *filename);
  int loadMap(char *filename);
  int ComputeWeightMap( void );
  int meanShift(int action);
  void Refresh( void );

};

#endif
