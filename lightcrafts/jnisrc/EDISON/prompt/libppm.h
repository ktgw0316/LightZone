////////////////////////////////////////////////////////
// Name     : libppm.h
// Purpose  : Read/Write Portable Pixel Map images
// Author   : Chris M. Christoudias
// Modified by
// Created  : 03/20/2002
// Copyright: (c) Chris M. Christoudias
// Version  : v0.1
////////////////////////////////////////////////////////

#ifndef LIBPPM_H
#define LIBPPM_H

//define error constants
enum {
  PPM_NO_ERRORS,
  PPM_NULL_PTR,
  PPM_FILE_ERROR,
  PPM_UNKNOWN_FORMAT,
  PPM_OUT_OF_MEMORY
};

int writePPMImage(char *filename, unsigned char *image, int height, int width, int depth, char *comments);
int writePGMImage(char *filename, unsigned char *image, int height, int width, int depth, char *comments);
int writePNMImage(char *filename, unsigned char *image, int height, int width, int depth, bool color,
		  char *comments);
int readPPMImage(char *filename, unsigned char **image, int& height, int& width, int& depth);
int readPGMImage(char *filename, unsigned char **image, int& height, int& width, int& depth);
int readPNMImage(char *filename, unsigned char **image, int& height, int& width, int& depth, bool& color);

#endif
