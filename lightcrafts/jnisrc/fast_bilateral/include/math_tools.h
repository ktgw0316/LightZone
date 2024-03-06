/* Copyright (C) 2005-2011 Fabio Riccardi */

/*! \file

\verbatim

Copyright (c) 2004, Sylvain Paris and Francois Sillion
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following
    disclaimer in the documentation and/or other materials provided
    with the distribution.

    * Neither the name of ARTIS, GRAVIR-IMAG nor the names of its
    contributors may be used to endorse or promote products derived
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

\endverbatim


 *  This file contains code made by Sylvain Paris under supervision of
 * Fran�ois Sillion for his PhD work with <a
 * href="http://www-artis.imag.fr">ARTIS project</a>. ARTIS is a
 * research project in the GRAVIR/IMAG laboratory, a joint unit of
 * CNRS, INPG, INRIA and UJF.
 *
 *  Use <a href="http://www.stack.nl/~dimitri/doxygen/">Doxygen</a>
 * with DISTRIBUTE_GROUP_DOC option to produce an nice html
 * documentation.
 *
 *  The file defines several common mathematical functions.
 */

#ifndef __MATH_TOOLS__
#define __MATH_TOOLS__

#include <algorithm>
#include "array.h"

namespace Math_tools{

  template<typename Array,typename Real>
  inline
  typename Array::value_type
  trilinear_interpolation(const Array& array,
			  const Real x,
			  const Real y,
			  const Real z){

    typedef unsigned int size_type;
    typedef float real_type;

    const size_type x_size = array.x_size();
    const size_type y_size = array.y_size();
    const size_type z_size = array.z_size();

    const size_type x_index  = std::clamp(static_cast<size_type>(x), size_type(0), x_size-1);
    const size_type xx_index = std::clamp(x_index+1, size_type(0), x_size-1);

    const size_type y_index  = std::clamp(static_cast<size_type>(y), size_type(0), y_size-1);
    const size_type yy_index = std::clamp(y_index+1, size_type(0), y_size-1);

    const size_type z_index  = std::clamp(static_cast<size_type>(z), size_type(0), z_size-1);
    const size_type zz_index = std::clamp(z_index+1, size_type(0), z_size-1);

    const real_type x_alpha = x - x_index;
    const real_type y_alpha = y - y_index;
    const real_type z_alpha = z - z_index;

    return
      (1.0f-x_alpha) * (1.0f-y_alpha) * (1.0f-z_alpha) * array(x_index, y_index, z_index) +
      x_alpha        * (1.0f-y_alpha) * (1.0f-z_alpha) * array(xx_index,y_index, z_index) +
      (1.0f-x_alpha) * y_alpha        * (1.0f-z_alpha) * array(x_index, yy_index,z_index) +
      x_alpha        * y_alpha        * (1.0f-z_alpha) * array(xx_index,yy_index,z_index) +
      (1.0f-x_alpha) * (1.0f-y_alpha) * z_alpha        * array(x_index, y_index, zz_index) +
      x_alpha        * (1.0f-y_alpha) * z_alpha        * array(xx_index,y_index, zz_index) +
      (1.0f-x_alpha) * y_alpha        * z_alpha        * array(x_index, yy_index,zz_index) +
      x_alpha        * y_alpha        * z_alpha        * array(xx_index,yy_index,zz_index);
  }

} // end of namespace


#endif
