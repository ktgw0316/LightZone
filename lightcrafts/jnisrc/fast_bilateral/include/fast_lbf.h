/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef __FAST_LINEAR_BF__
#define __FAST_LINEAR_BF__

#include <array>
#include "array.h"
#include "math_tools.h"
#include "mixed_vector.h"
#include "omp.h"

namespace Image_filter{

  template <typename Base_array,typename Data_array,typename Real>
  void fast_LBF(const Data_array&  input,
        const Base_array&  base,
        const Real         space_sigma,
        const Real         range_sigma,
        Base_array* const  weight,
        Data_array* const  result);

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


  template <typename Base_array,typename Data_array,typename Real>
  void fast_LBF(const Data_array&  input,
        const Base_array&  base,
        const Real         space_sigma,
        const Real         range_sigma,
        Base_array* const  weight,
        Data_array* const  result){

    using namespace std;

    typedef typename Base_array::value_type   base_type;
    typedef base_type                         real_type;
    typedef typename Data_array::value_type   data_type;
    typedef Mixed_vector<data_type,base_type> mixed_type;
    typedef unsigned int                      size_type;
    
    typedef Array_3D<mixed_type> mixed_array_3D_type;

    const size_type width  = result->x_size();
    const size_type height = result->y_size();

    constexpr size_type padding_xy = 2;
    constexpr size_type padding_z  = 2;
    
    const int padding = 2 * ceil(space_sigma);

    constexpr base_type base_min   = 0; // *min_element(base.begin(),base.end());
    constexpr base_type base_max   = 1; // *max_element(base.begin(),base.end());
    constexpr base_type base_delta = base_max - base_min;
    
    const size_type small_width  = static_cast<size_type>((width  - 1) / space_sigma) + 2 + 2 * padding_xy;
    const size_type small_height = static_cast<size_type>((height - 1) / space_sigma) + 2 + 2 * padding_xy; 
    const size_type small_depth  = static_cast<size_type>(base_delta / range_sigma)   + 1 + 2 * padding_z; 

    mixed_array_3D_type data(small_width,
                 small_height,
                 small_depth,mixed_type());

#pragma omp parallel for
    for(size_type x = 0;x < width+2*padding; ++x){
      for(size_type y = 0; y < height+2*padding; ++y){

        const base_type z = base(x,y) - base_min;

        const size_type small_x = static_cast<size_type>(x / space_sigma + 0.5);
        const size_type small_y = static_cast<size_type>(y / space_sigma + 0.5);

        if (small_x < small_width && small_y < small_height) {
            const size_type small_z = static_cast<size_type>(z / range_sigma + 0.5) + padding_z;
            mixed_type& d = data(small_x,small_y,small_z);
            d.first  += input(x,y);
            d.second += 1.0;
        }
      } // END OF for y
    } // END OF for x

    const array<intptr_t, 3> offset {
        &(data(1,0,0)) - &(data(0,0,0)),
        &(data(0,1,0)) - &(data(0,0,0)),
        &(data(0,0,1)) - &(data(0,0,0))
    };
    
    mixed_array_3D_type buffer(small_width,small_height,small_depth);

    for(size_type dim = 0;dim < 3;++dim){

      const int off = offset[dim];
      
      for(size_type n_iter = 0;n_iter < 2;++n_iter){

        swap(buffer,data);
      
        for(size_type
            x     = 1,
            x_end = small_width - 1;
            x < x_end;++x){

          for(size_type
              y     = 1,
              y_end = small_height - 1;
              y < y_end;++y){

            mixed_type* d_ptr = &(data(x,y,1));
            mixed_type* b_ptr = &(buffer(x,y,1));

            for(size_type
                z     = 1,
                z_end = small_depth - 1;
                z < z_end;
                ++z,++d_ptr,++b_ptr){

              *d_ptr = (*(b_ptr - off) + *(b_ptr + off) + 2.0 * (*b_ptr)) / 4.0;

            } // END OF for z
          } // END OF for y
        } // END OF for x
      } // END OF for n_iter
    } // END OF for dim

    result->resize(width,height);
    
    weight->resize(width,height);

#pragma omp parallel for
    for(size_type y = 0; y < height; ++y){
      for(size_type x = 0; x < width; ++x){

        const base_type z = base(x + padding,y + padding) - base_min;

        const mixed_type D =
          Math_tools::trilinear_interpolation(data,
                          static_cast<real_type>(x) / space_sigma + padding_xy,
                          static_cast<real_type>(y) / space_sigma + padding_xy,
                          z / range_sigma + padding_z);

        (*weight)(x,y) = D.second;
        (*result)(x,y) = D.first / D.second;
      } // END OF for x
    } // END OF for y
  }

} // END OF namespace Image_filter



#endif
