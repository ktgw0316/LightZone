////////////////////////////////////////////////////////
// Name     : edison.cpp
// Purpose  : Wrapper class used for segmenation and
//            edge detection.
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#include "defs.h"
#include "flags.h"
#include "edison.h"
#include "../segm/msImageProcessor.h"
#include "../edge/BgImage.h"
#include "../edge/BgEdge.h"
#include "../edge/BgEdgeList.h"
#include "../edge/BgEdgeDetect.h"
#include "../edge/BgDefaults.h"
#include <stdio.h>
#include <string.h>

////////////////////////////////////////////////////////
//Constructor/Destructor
////////////////////////////////////////////////////////

EDISON::EDISON( void )
{
  height_            = -1;
  width_             = -1;
  numEdges_          = -1;
  numBoundaries_     = -1;
  inputImage_        = (unsigned char *) NULL;
  filtImage_         = (unsigned char *) NULL;
  segmImage_         = (unsigned char *) NULL;
  gradMap_           = (float *) NULL;
  confMap_           = (float *) NULL;
  weightMap_         = (float *) NULL;
  custMap_           = (float *) NULL;
  edges_             = (int *)   NULL;
  boundaries_        = (int *)   NULL;
  parameterList_     = (void **) NULL;
  inputDefined_      = false;
  gradMapDefined_    = false;
  confMapDefined_    = false;
  weightMapDefined_  = false;
  custMapDefined_    = false;
  filtImageDefined_  = false;
  segmImageDefined_  = false;
  edgesDefined_      = false;
  boundariesDefined_ = false;
}

EDISON::~EDISON( void )
{
  if(inputImage_) delete [] inputImage_;
  if(filtImage_)  delete [] filtImage_;
  if(segmImage_)  delete [] segmImage_;
  if(gradMap_)    delete [] gradMap_;
  if(confMap_)    delete [] confMap_;
  if(weightMap_)  delete [] weightMap_;
  if(custMap_)    delete [] custMap_;
  if(edges_)      delete [] edges_;
  if(boundaries_) delete [] boundaries_;
}

////////////////////////////////////////////////////////
//Input/Output
////////////////////////////////////////////////////////

int EDISON::Save(char *filename, int filetype, int outputtype)
{
  if(!filename) return EXE_NULL_PTR;
  int error;
  switch(outputtype)
    {
    case OUTPUT_SEGM_BOUNDARIES:
      if(!boundariesDefined_) return EXE_OUTPUT_UNDEFINED;
      error = writeImage(filename, (unsigned char *) NULL, boundaries_, numBoundaries_, filetype);
      break;
    case OUTPUT_SEGM_IMAGE:
      if(!segmImageDefined_) return EXE_OUTPUT_UNDEFINED;
      error = writeImage(filename, segmImage_, (int *) NULL, -1, filetype);
      break;
    case OUTPUT_SEGM_IMAGE_BOUNDARIES:
      if(!(boundariesDefined_ && segmImageDefined_)) return EXE_OUTPUT_UNDEFINED;
      error = writeImage(filename, segmImage_, boundaries_, numBoundaries_, filetype);
      break;
    case OUTPUT_FILT_IMAGE:
      if(!filtImageDefined_) return EXE_OUTPUT_UNDEFINED;
      error = writeImage(filename, filtImage_, (int *) NULL, -1, filetype);
      break;
    case OUTPUT_FILT_IMAGE_BOUNDARIES:
      if(!(boundariesDefined_ && filtImageDefined_)) return EXE_OUTPUT_UNDEFINED;      
      error = writeImage(filename, filtImage_, boundaries_, numBoundaries_, filetype);
      break;
    case OUTPUT_GRADIENT_MAP:
      if(!gradMapDefined_) return EXE_OUTPUT_UNDEFINED;
      error = saveData(filename, gradMap_, filetype);
      break;
    case OUTPUT_CONFIDENCE_MAP:
      if(!confMapDefined_) return EXE_OUTPUT_UNDEFINED;
      error = saveData(filename, confMap_, filetype);
      break;
    case OUTPUT_WEIGHT_MAP:
      if(!weightMapDefined_) return EXE_OUTPUT_UNDEFINED;
      error = saveData(filename, weightMap_, filetype);
      break;
    case OUTPUT_EDGES:
      if(!edgesDefined_) return EXE_OUTPUT_UNDEFINED;
      error = writeImage(filename, (unsigned char *) NULL, edges_, numEdges_, filetype);
      break;
    }
  return error;
}

int EDISON::Load(char *filename, int inputtype)
{
  if(!filename) return EXE_NULL_PTR;
  int error;
  switch(inputtype)
    {
    case INPUT_IMAGE:
      error = loadImage(filename);
      if(!error) Refresh();
      break;
    case INPUT_MAP:
      error = loadMap(filename);
      break;
    }
  return error;
}

int EDISON::UseResult(int outputtype)
{
  switch(outputtype)
    {
    case OUTPUT_SEGM_IMAGE:
      if(!segmImageDefined_) return EXE_OUTPUT_UNDEFINED;
      memcpy(inputImage_, segmImage_, height_*width_*dim_*sizeof(unsigned char));
      break;
    case OUTPUT_FILT_IMAGE:
      if(!filtImageDefined_) return EXE_OUTPUT_UNDEFINED;
      memcpy(inputImage_, filtImage_, height_*width_*dim_*sizeof(unsigned char));
      break;
    default:
      break;
    }
  Refresh();
  return NO_ERRORS;
}
	
int EDISON::writeImage(char *filename, unsigned char *image, int *dataPoints, int n, int filetype)
{
  unsigned char *data = new unsigned char [height_ * width_ * dim_];
  if(!data) return EXE_OUT_OF_MEMORY;
  memset(data, 0, height_*width_*dim_*sizeof(unsigned char));
  if(image) memcpy(data, image, height_*width_*dim_*sizeof(unsigned char));
  if(dataPoints) {
    int i, j, x, y;
    for(i = 0; i < n; i++) {
      for(j = 0; j < dim_; j++) {
	data[dim_*dataPoints[i]+j] = 255;
      }
    }
  }
  int error = CmCWriteImage(filename, data, height_, width_, dim_, filetype);
  delete [] data;
  return error;
}

int EDISON::saveData(char *filename, float *data, int filetype)
{
  if(filetype != FILE_MATLAB_ASCII) {
    unsigned char *imData = new unsigned char[height_*width_];
    if(!imData) return EXE_OUT_OF_MEMORY;
    int i;
    for(i = 0; i < height_*width_; i++) {
      imData[i] = (int)(255*data[i] + 0.5);
    }
    int error = CmCWriteImage(filename, imData, height_, width_, 1, filetype);
    return error;
  }
  int error = CmCWriteMFile(filename, data, height_, width_, dim_);
  return error;
}

int EDISON::loadImage(char *filename)
{  
  int error = CmCReadImage(filename, &inputImage_, height_, width_, dim_);
  if(!error) inputDefined_ = true;
  return error;
}

int EDISON::loadMap(char *filename)
{
  if(!inputDefined_) return EXE_INPUT_UNDEFINED;
  int error = CmCReadMFile(filename, &custMap_, height_, width_);
  if(!error) custMapDefined_ = true;
  return error;
}   

void EDISON::Refresh( void )
{
  //de-allocate all memory
  if(filtImage_)  delete [] filtImage_;
  if(segmImage_)  delete [] segmImage_;
  if(gradMap_)    delete [] gradMap_;
  if(confMap_)    delete [] confMap_;
  if(weightMap_)  delete [] weightMap_;
  if(custMap_)    delete [] custMap_;
  if(edges_)      delete [] edges_;
  if(boundaries_) delete [] boundaries_;

  //reset flags
  gradMapDefined_    = false;
  confMapDefined_    = false;
  weightMapDefined_  = false;
  custMapDefined_    = false;
  filtImageDefined_  = false;
  segmImageDefined_  = false;
  edgesDefined_      = false;
  boundariesDefined_ = false;
}

////////////////////////////////////////////////////////
//Set Parameters
////////////////////////////////////////////////////////

void EDISON::SetParameters(void **parameterList)
{
  parameterList_ = parameterList;
}

////////////////////////////////////////////////////////
//Edge Detection
////////////////////////////////////////////////////////

int EDISON::EdgeDetect( void )
{

  //make sure an input image was defined
  if(!inputDefined_) return EXE_INPUT_UNDEFINED;

  //prompt the user
  CmCPrompt("\n-------------------------------------------------------------------------\n");
  CmCPrompt("Performing EDGE DETECTION:\n\n");
  CmCPrompt("\tUsing Parameters:\n\n");  

  //make sure all necessary parameters are available
  int i;
  if(!parameterList_[PARAM_GRADIENT_WINDOW_RADIUS]) return EXE_MISSING_PARAM;
  for(i = 7; i < PARAM_NUM; i++) {
    if(!parameterList_[i]) return EXE_MISSING_PARAM;
  }

  //obtain parameters
  int gradWindRad     = *((int *)(parameterList_[PARAM_GRADIENT_WINDOW_RADIUS]));
  int minLength       = *((int *)(parameterList_[PARAM_MINIMUM_LENGTH]));
  float nmxRank       = *((float *)(parameterList_[PARAM_NMX_RANK]));
  float nmxConf       = *((float *)(parameterList_[PARAM_NMX_CONF]));
  int   nmxType       = *((int *)(parameterList_[PARAM_NMX_TYPE]));
  float hystHighRank  = *((float *)(parameterList_[PARAM_HYSTERISIS_HIGH_RANK]));
  float hystHighConf  = *((float *)(parameterList_[PARAM_HYSTERISIS_HIGH_CONF]));
  int   hystHighType  = *((int *)(parameterList_[PARAM_HYSTERISIS_HIGH_TYPE]));
  float hystLowRank   = *((float *)(parameterList_[PARAM_HYSTERISIS_LOW_RANK]));
  float hystLowConf   = *((float *)(parameterList_[PARAM_HYSTERISIS_LOW_CONF]));
  int   hystLowType   = *((int *)(parameterList_[PARAM_HYSTERISIS_LOW_TYPE]));

  //prompt the user
  CmCPrompt("\t\tGradient Window Radius\t= %6d\n\t\tMinimum Length\t\t= %6d\n\n", gradWindRad, minLength);
  CmCPrompt("\t\tNmx. Rank\t\t= %6.4f\n\t\tNmx. Conf\t\t= %6.4f\n\t\tNmx. Curve Type\t\t= %s\n\n",
	    nmxRank, nmxConf, CURVETYPE_LIST[nmxType]);
  CmCPrompt("\t\tHyst. High Rank\t\t= %6.4f\n\t\tHyst. High Conf\t\t= %6.4f\n\t\tHyst. High Curve Type\t= %s",
	    hystHighRank, hystHighConf, CURVETYPE_LIST[hystHighType]);
  CmCPrompt("\n\n");
  CmCPrompt("\t\tHyst. Low Rank\t\t= %6.4f\n\t\tHyst. Low Conf\t\t= %6.4f\n\t\tHyst. Low Curve Type\t= %s",
	    hystLowRank, hystLowConf, CURVETYPE_LIST[hystLowType]);
  CmCPrompt("\n\n");  

  //convert the input image to grayscale if necessary...
  BgImage inputImage;
  if(dim_ == 3) {
    unsigned char *data = CmCConvertToGrayscale(inputImage_, height_, width_);
    inputImage.SetImage(data, width_, height_, false);
    delete [] data;
  } else
    inputImage.SetImage(inputImage_, width_, height_, false);

  //if there are any custom curves attempt to read in there point lists
  int   n;
  float *pts;
  double *ptsX, *ptsY;
  BgEdgeDetect edgeDetector(gradWindRad);
  if(hystHighType == CURVE_CUSTOM) {
    pts = (float *)(parameterList_[PARAM_NUM + 2*CUST_CURVE_HYST_HIGH]);
    if(!pts) return EXE_POINT_LIST_HIGH;
    n = *((int *)(parameterList_[PARAM_NUM + 2*CUST_CURVE_HYST_HIGH + 1]));
    ptsX = new double [n];
    ptsY = new double [n];
    for(i = 0; i < n; i++) {
      ptsX[i] = (double) pts[2*i];
      ptsY[i] = (double) pts[2*i+1];
    }
    edgeDetector.SetCustomHigh(ptsX, ptsY, n);
    delete [] ptsX;
    delete [] ptsY;
  }
  if(hystLowType == CURVE_CUSTOM) {
    pts = (float *)(parameterList_[PARAM_NUM + 2*CUST_CURVE_HYST_LOW]);
    if(!pts) return EXE_POINT_LIST_LOW;
    n = *((int *)(parameterList_[PARAM_NUM + 2*CUST_CURVE_HYST_LOW + 1]));
    ptsX = new double [n];
    ptsY = new double [n];
    for(i = 0; i < n; i++) {
      ptsX[i] = (double) pts[2*i];
      ptsY[i] = (double) pts[2*i+1];
    }
    edgeDetector.SetCustomLow(ptsX, ptsY, n);
    delete [] ptsX;
    delete [] ptsY;
  }

  //edge detect the input image
  BgEdgeList   edgeList;
  edgeDetector.DoEdgeDetect(&inputImage, &edgeList, nmxRank, nmxConf, hystHighRank, hystHighConf,
			    hystLowRank, hystLowConf, minLength, nmxType, hystHighType, hystLowType);  
  if(edgesDefined_) delete [] edges_;
  edges_ = new int [height_ * width_];
  int *edgex, *edgey;
  edgex = new int [height_ * width_];
  edgey = new int [height_ * width_];
  edgeList.GetAllEdgePoints(edgex, edgey, &numEdges_);
  for(i = 0; i < numEdges_; i++) {
    edges_[i] = edgey[i]*width_ + edgex[i];
  }
  edgesDefined_ = true;
  delete [] edgex;
  delete [] edgey;

  //obtain rank and confidence maps
  if(gradMapDefined_) delete [] gradMap_;
  if(confMapDefined_) delete [] confMap_;
  gradMap_ = new float [height_ * width_];
  confMap_ = new float [height_ * width_];
  if(!gradMap_ || !confMap_) return EXE_OUT_OF_MEMORY;
  memcpy(gradMap_, edgeDetector.permRank_, height_ * width_ * sizeof(float));
  memcpy(confMap_, edgeDetector.permConf_, height_ * width_ * sizeof(float));
  gradMapDefined_ = true;
  confMapDefined_ = true;

  //prompt the user
  CmCPrompt("-------------------------------------------------------------------------\n");
  
  //done.
  return NO_ERRORS;
  
} 

////////////////////////////////////////////////////////
//Image Segmenation
////////////////////////////////////////////////////////

int EDISON::Filter( void )
{
  return meanShift(EDISON_FILTER);
}

int EDISON::Fuse( void )
{
  return meanShift(EDISON_FUSE);
}

int EDISON::Segment( void )
{
  return meanShift(EDISON_SEGMENT);
}

int EDISON::meanShift(int action)
{
  
  //make sure input has been defined
  if(!inputDefined_) return EXE_INPUT_UNDEFINED;

  //obtain parameters...
  int   sigmaS, minRegion;
  float sigmaR;
  char action_str[80];
  action_str[0] = 0; //initialize string
  if(CmCSynergistic) strcpy(action_str, "SYNERGISTIC ");
  if(!parameterList_[PARAM_SPEEDUP]) return EXE_MISSING_PARAM;
  switch(action) {
  case EDISON_FILTER:
    strcat(action_str, "IMAGE FILTERING");
    if(parameterList_[PARAM_SPATIAL_BANDWIDTH]) {
      sigmaS = *((int *)(parameterList_[PARAM_SPATIAL_BANDWIDTH]));
      if(parameterList_[PARAM_RANGE_BANDWIDTH]) {
	sigmaR = *((float *)(parameterList_[PARAM_RANGE_BANDWIDTH]));
	break;
      }
    }
    return EXE_MISSING_PARAM;
  case EDISON_FUSE:
    strcat(action_str, "IMAGE REGION FUSION");
    if(parameterList_[PARAM_RANGE_BANDWIDTH]) {
      sigmaR = *((float *)(parameterList_[PARAM_RANGE_BANDWIDTH]));
      if(parameterList_[PARAM_MINIMUM_REGION_AREA]) {
	minRegion = *((int *)(parameterList_[PARAM_MINIMUM_REGION_AREA]));
	break;
      }
    }
    return EXE_MISSING_PARAM;
  case EDISON_SEGMENT:
    strcat(action_str, "IMAGE SEGMENTATION");
    if(parameterList_[PARAM_SPATIAL_BANDWIDTH]) {
      sigmaS = *((int *)(parameterList_[PARAM_SPATIAL_BANDWIDTH]));
      if(parameterList_[PARAM_RANGE_BANDWIDTH]) {
	sigmaR = *((float *)(parameterList_[PARAM_RANGE_BANDWIDTH]));
	if(parameterList_[PARAM_MINIMUM_REGION_AREA]) {
	  minRegion = *((int *)(parameterList_[PARAM_MINIMUM_REGION_AREA]));
	  break;
	}
      }
    }
    return EXE_MISSING_PARAM;
  }

  //check for synergistic parameters
  if(CmCSynergistic) {
    int i;
    for(i = PARAM_GRADIENT_WINDOW_RADIUS; i <= PARAM_EDGE_STRENGTH_THRESHOLD; i++) {
      if(!parameterList_[i]) return EXE_MISSING_PARAM;
    }
  }

  //prompt the user
  CmCPrompt("\n-------------------------------------------------------------------------\n");
  CmCPrompt("Performing %s:\n\n", action_str);
  CmCPrompt("\tUsing Parameters:\n\n");
  CmCPrompt("\t\tSpatial Bandwidth\t= %6d\n\t\tRange Bandwidth\t\t= %6.4f\n\t\tMinimum Region Area\t= %6d",
	    sigmaS, sigmaR, minRegion);
  if(CmCSynergistic) {
    int   gradWindRad = *((int *)(parameterList_[PARAM_GRADIENT_WINDOW_RADIUS]));
    float threshold   = *((float *)(parameterList_[PARAM_EDGE_STRENGTH_THRESHOLD]));
    float mixture     = *((float *)(parameterList_[PARAM_MIXTURE_PARAMETER]));
    CmCPrompt("\n\n\t\tGradient Window Radius\t= %6d\n\t\tEdge Strength Threshold\t= %6.4f\n\t\t",
	      gradWindRad, threshold);
    CmCPrompt("Mixture Parameter\t= %6.4f\n\n", mixture);
  }

  //create image processing object
  msImageProcessor iProc;

  //define the image to be processed as the input image
  if(dim_ == 3)
    iProc.DefineImage(inputImage_, COLOR, height_, width_);
  else
    iProc.DefineImage(inputImage_, GRAYSCALE, height_, width_);
  if(iProc.ErrorStatus) {
    return EXE_ERROR;
  }

  //compute and set weight map if synergistic segmentation is requested
  if(CmCSynergistic) {
    if(CmCUseCustomWeightMap) {
      if(!custMapDefined_) return EXE_INPUT_UNDEFINED;
      iProc.SetWeightMap(custMap_, *((float *)(parameterList_[PARAM_EDGE_STRENGTH_THRESHOLD])));
    } else {    
      int error = ComputeWeightMap();
      if(error) return error;
      iProc.SetWeightMap(weightMap_, *((float *)(parameterList_[PARAM_EDGE_STRENGTH_THRESHOLD])));
    }
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }
  }
  
  switch(action) {
  case EDISON_FILTER:
    //filter the input image
    iProc.Filter(sigmaS, sigmaR, (SpeedUpLevel)(*(int *)(parameterList_[PARAM_SPEEDUP])));
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }

    //obtain the output
    if(filtImageDefined_) delete [] filtImage_;
    filtImage_ = new unsigned char [height_ * width_ * dim_];
    iProc.GetResults(filtImage_);
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }
    filtImageDefined_ = true;
    break;
  case EDISON_FUSE:
    //re-define input image if filtered image is defined
    if(filtImageDefined_) {
      if(dim_ == 3)
	iProc.DefineImage(filtImage_, COLOR, height_, width_);
      else
	iProc.DefineImage(filtImage_, GRAYSCALE, height_, width_);
    } 

    //fuse the regions of input image
    iProc.FuseRegions(sigmaR, minRegion);
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }

    //obtain the output
    if(segmImageDefined_) delete [] segmImage_;
    segmImage_ = new unsigned char [height_ * width_ * dim_];
    iProc.GetResults(segmImage_);
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }
    segmImageDefined_ = true;
    break;
  case EDISON_SEGMENT:
    //filter the image
    iProc.Filter(sigmaS, sigmaR, (SpeedUpLevel)(*(int *)(parameterList_[PARAM_SPEEDUP])));
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }

    //obtain the filtered image
    if(filtImageDefined_) delete [] filtImage_;
    filtImage_ = new unsigned char [height_ * width_ * dim_];
    iProc.GetResults(filtImage_);
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }
    filtImageDefined_ = true;

    //fuse regions
    iProc.FuseRegions(sigmaR, minRegion);
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }
    
    //obtain the segmented image
    if(segmImageDefined_) delete [] segmImage_;
    segmImage_ = new unsigned char [height_ * width_ * dim_];
    iProc.GetResults(segmImage_);
    if(iProc.ErrorStatus) {
      return EXE_ERROR;
    }
    segmImageDefined_ = true;
    break; 
  }

  //define the boundaries
  RegionList *regionList        = iProc.GetBoundaries();
  int        *regionIndeces     = regionList->GetRegionIndeces(0);
  int        numRegions         = regionList->GetNumRegions();
  numBoundaries_ = 0;
  int i;
  for(i = 0; i < numRegions; i++) {
    numBoundaries_ += regionList->GetRegionCount(i);
  }
  if(boundariesDefined_) delete [] boundaries_;
  boundaries_ = new int [numBoundaries_];
  for(i = 0; i < numBoundaries_; i++) {
    boundaries_[i] = regionIndeces[i];
  }
  boundariesDefined_ = true;

  //prompt the user
  CmCPrompt("-------------------------------------------------------------------------\n\n");

  //done.
  return NO_ERRORS;

}

int EDISON::ComputeWeightMap( void )
{

  //do not do un-necessary computation
  if(weightMapDefined_ && !CmCGradWinChanged && !CmCMixtureChanged) return NO_ERRORS;

  //attain necessary parameters...
  int   gradWindowRadius = *((int *)(parameterList_[PARAM_GRADIENT_WINDOW_RADIUS]));
  float mixtureParam     = *((float *)(parameterList_[PARAM_MIXTURE_PARAMETER]));
  
  //compute gradient and confidence maps
  if(!gradMapDefined_ || !confMapDefined_ || CmCGradWinChanged) {
    if(gradMapDefined_) delete [] gradMap_;
    if(confMapDefined_) delete [] confMap_;
    gradMap_ = new float [height_ * width_];
    confMap_ = new float [height_ * width_];
    if(!gradMap_ || !confMap_) return EXE_OUT_OF_MEMORY;
    BgEdgeDetect edgeDetector(gradWindowRadius);
    BgImage inputImage;
    if(dim_ == 3)
      inputImage.SetImage(inputImage_, width_, height_, true);
    else
      inputImage.SetImage(inputImage_, width_, height_, false);
    edgeDetector.ComputeEdgeInfo(&inputImage, confMap_, gradMap_);
  }

  //compute weight map
  if(!weightMapDefined_ || !gradMapDefined_ || !confMapDefined_ || CmCGradWinChanged || CmCMixtureChanged) {
    CmCPrompt("Computing weight map...");
    if(weightMapDefined_) delete [] weightMap_;
    weightMap_ = new float [height_ * width_];
    if(!weightMap_) return EXE_OUT_OF_MEMORY;
    int i;
    for(i = 0; i < width_*height_; i++) {
      if(gradMap_[i] > 0.02) {
	weightMap_[i] = mixtureParam*gradMap_[i] + (1 - mixtureParam)*confMap_[i];
      } else {
	weightMap_[i] = 0;
      }
    }
    CmCPrompt("done.\n");
  }

  //indicate that maps are now defined
  gradMapDefined_   = true;
  confMapDefined_   = true;
  weightMapDefined_ = true;

  //indicate that the changed parameters have been accounted for
  CmCGradWinChanged = false;
  CmCMixtureChanged = false;
  
  //done.
  return NO_ERRORS;
}






