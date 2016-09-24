#pragma once

#if _OPENMP >= 201307
#  define OMP_SIMD              _Pragma("omp simd")
#  define OMP_FOR_SIMD          _Pragma("omp for simd")
#  define OMP_PARALLEL_FOR_SIMD _Pragma("omp parallel for simd")
#else
#  define OMP_SIMD
#  define OMP_FOR_SIMD          _Pragma("omp for")
#  define OMP_PARALLEL_FOR_SIMD _Pragma("omp parallel for")
#endif

