/* Copyright (C) 2005-2011 Fabio Riccardi */

/*! \file
  \verbatim
  
    Copyright (c) 2006, Sylvain Paris and Frédo Durand

    Permission is hereby granted, free of charge, to any person
    obtaining a copy of this software and associated documentation
    files (the "Software"), to deal in the Software without
    restriction, including without limitation the rights to use, copy,
    modify, merge, publish, distribute, sublicense, and/or sell copies
    of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
    HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.

  \endverbatim
*/

#ifndef __FAST_COLOR_BF__
#define __FAST_COLOR_BF__

#include "mixed_vector.h"

#ifdef CHRONO
#  include <iostream>
#  include "chrono.h"
#endif

#include "array.h"
#include "array_n.h"
#include "math_tools.h"

namespace Image_filter{

  template <typename Base_array,typename Data_array,typename Weight_array,typename Real>
  void fast_color_BF(const Data_array&   input,
		     const Base_array&   base,
		     const Real          space_sigma,
		     const Real          range_sigma,
		     Weight_array* const weight,
		     Data_array* const   result);

  template <typename Data_array,typename Real>
  void fast_color_BF(const Data_array& input,
		     const Real        space_sigma,
		     const Real        range_sigma,
		     Data_array* const result);


  
/*
  
  #############################################
  #############################################
  #############################################
  ######                                 ######
  ######   I M P L E M E N T A T I O N   ######
  ######                                 ######
  #############################################
  #############################################
  #############################################
  
*/


  

  template <typename Data_array,typename Real>
  void fast_color_BF(const Data_array& input,
		     const Real        space_sigma,
		     const Real        range_sigma,
		     Data_array* const result){

    Array_2D<Real> weight;
    fast_color_BF(input,input,
		  space_sigma,range_sigma,
		  &weight,result);
  }


  template <typename Base_array,typename Data_array,typename Weight_array,typename Real>
  void fast_color_BF(const Data_array&   input,
		     const Base_array&   base,
		     const Real          space_sigma,
		     const Real          range_sigma,
		     Weight_array* const weight,
		     Data_array* const   result){

    using namespace std;
    typedef typename Base_array::value_type   base_type;
    typedef float                             real_type;
    typedef typename Data_array::value_type   data_type;
    typedef Mixed_vector<data_type,real_type> mixed_type;
    typedef unsigned int                      size_type;

    const size_type dimension = 5;
    
    typedef Base_array                             base_array_2D_type;
    typedef Array_ND<dimension,mixed_type>         mixed_array_ND_type;
    typedef typename mixed_array_ND_type::key_type key_type;

    const size_type width  = input.x_size();
    const size_type height = input.y_size();

    const size_type padding_s = 2;
    const size_type padding_r = 2;

    key_type padding;
    padding[0] = padding_s;
    padding[1] = padding_s;
    padding[2] = padding_r;
    padding[3] = padding_r;
    padding[4] = padding_r;
    
    base_type base_min;
    key_type size,index = key_type();
    
    for(size_type c = 0;c < 3;++c){
      
      real_type m = base[index][c];
      real_type M = m;
      
      for(typename base_array_2D_type::const_iterator
	    b     = base.begin(),
	    b_end = base.end();
	  b != b_end;++b){
	
	const real_type r = (*b)[c];
	m = min(m,r);
	M = max(M,r);
      }
      
      base_min[c] = m;
      size[2 + c] = static_cast<size_type>((M - m) / range_sigma)
	+ 1 + 2 * padding[2 + c];
    }
    size[0] = static_cast<size_type>((width  - 1) / space_sigma) + 1 + 2*padding[0]; 
    size[1] = static_cast<size_type>((height - 1) / space_sigma) + 1 + 2*padding[1]; 

#ifdef CHRONO
    Chrono chrono("filter");
    chrono.start();

    Chrono chrono_down("downsampling");
    chrono_down.start();
#endif

  
    mixed_array_ND_type data(size,mixed_type());

    for(size_type x = 0,x_end = width;x < x_end;++x){

      index[0] = static_cast<size_type>(1.0*x / space_sigma + 0.5) + padding[0];

      for(size_type y = 0,y_end = height;y < y_end;++y){
      
	index[1] = static_cast<size_type>(1.0*y / space_sigma + 0.5) + padding[1];

	for(size_type c = 0;c < 3;++c){
	  index[2 + c] =
	    static_cast<size_type>((base(x,y)[c] - base_min[c]) / range_sigma + 0.5)
	    + padding[2 + c];
	}

	mixed_type& d = data[index];
	d.first  += input(x,y);
	d.second += 1.0;
      } // END OF for y
    } // END OF for x
  
#ifdef CHRONO
  chrono_down.stop();
  cout<<"  "<<chrono_down.report()<<endl;
  
  Chrono chrono_convolution("convolution");
  chrono_convolution.start();
#endif  

  mixed_array_ND_type buffer(size);

  const mixed_type* const origin = &(data[key_type()]);
  key_type start_index,end_index;
  
  for(size_type dim = 0;dim < dimension;++dim){
    start_index[dim] = 1;
    end_index[dim]   = (dim == dimension - 1) ? 2 : (size[dim] - 1);
  }

  for(size_type dim = 0;dim < dimension;++dim){

    key_type offset_proxy;
    offset_proxy[dim] = 1;
    const int off = &(data[offset_proxy]) - origin;  
    
    for(size_type n_iter = 0;n_iter < 2;++n_iter){

      swap(buffer,data);

      key_type index = start_index;
      do{
	mixed_type* d_ptr = &(data[index]);
	mixed_type* b_ptr = &(buffer[index]);
	
	for(size_type
	      z     = 1,
	      z_end = size[dimension-1] - 1;
	    z < z_end;
	    ++z,++d_ptr,++b_ptr){
	      
	  *d_ptr = (*(b_ptr - off) + *(b_ptr + off) + 2.0 * (*b_ptr)) / 4.0;

	} // END OF for z
      }
      while(data.advance(&index,start_index,end_index));      
    } // END OF for n_iter
  } // END OF for dim

#ifdef CHRONO
  chrono_convolution.stop();
  cout<<"  "<<chrono_convolution.report()<<endl;

  Chrono chrono_nonlinearities("nonlinearities");
  chrono_nonlinearities.start();
#endif

  result->resize(width,height);

  vector<real_type> pos(dimension);
  

  weight->resize(width,height);
      
  for(size_type x = 0,x_end = width;x < x_end;++x){

    pos[0] = static_cast<real_type>(x) / space_sigma + padding[0];
	
    for(size_type y = 0,y_end = height;y < y_end;++y){
	  
      pos[1] = static_cast<real_type>(y) / space_sigma + padding[1];
	  
      for(size_type c = 0;c < 3;++c){
	pos[2 + c] = (base(x,y)[c] - base_min[c]) / range_sigma + padding[2 + c];
      }
      const mixed_type D = Math_tools::Nlinear_interpolation(data,pos);	  
	  
      (*weight)(x,y) = D.second;
      (*result)(x,y) = D.first / D.second;
    } // END OF for y
  } // END OF for x
  
#ifdef CHRONO
  chrono_nonlinearities.stop();
  cout<<"  "<<chrono_nonlinearities.report()<<endl;
    
  chrono.stop();    
  cout<<"  "<<chrono.report()<<endl;
#endif

  }
} // END OF namespace Image_filter

#endif
