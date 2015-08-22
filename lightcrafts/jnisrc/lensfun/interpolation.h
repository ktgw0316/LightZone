#include <limits>


template<typename T>
inline T interp1D
(const T p0, const T p1, const T p2, const T p3, const int** wait, const float d)
{
    float value = ((((7.0/18.0)*(p3 - p0) + (7.0/6.0)*(p1 - p2)) * d +
                    ((5.0/6.0)*p0 - 2*p1 + 1.5*p2 -1/3.0*p3)) * d +
                   0.5*(p2 -p0)) * d +
                  1/18.0*(p0 + p2) + (8.0/9.0)*p1;

    /*
    const idx = d * sizeof(wait) / sizeof(wait[0]);
    T value;
    value  = wait[idx][0] * p0 >> 8;
    value += wait[idx][1] * p1 >> 8;
    value += wait[idx][2] * p2 >> 8;
    value += wait[idx][3] * p3 >> 8;
    */

    const T limit = std::numeric_limits<T>::max();
    return value < 0 ? 0 : value < limit ? T(value) : limit;
}

template<typename T>
inline T MitchellInterp
(const T *data, const int pixelStride, const int offset, const int lineStride,
 const float x, const float y)
{
    // Using int is faster than using float
    // Each wait is 256 times actual wait
    //static const int wait[128][4] =
        //{{}
        //};
    static const int** wait = NULL; // TODO:

    const float x_floor = floor(x);
    const float y_floor = floor(y); 
    const float dx = x - x_floor;
    const float dy = y - y_floor;

    // Get the 4x4 pixel values
    T p[4][4];
    // top-left position
    const int pos00 = offset + pixelStride * (x_floor - 1) + lineStride * (y_floor - 1);

    for (int i = 0; i < 4; ++i) {
        int pos = pos00 + lineStride * i;
        for (int j = 0; j < 4; ++j, pos += pixelStride) {
            p[i][j] = data[pos];
        }
    }

    return interp1D(interp1D(p[0][0], p[0][1], p[0][2], p[0][3], wait, dx),
                    interp1D(p[1][0], p[1][1], p[1][2], p[1][3], wait, dx),
                    interp1D(p[2][0], p[2][1], p[2][2], p[2][3], wait, dx),
                    interp1D(p[3][0], p[3][1], p[3][2], p[3][3], wait, dx),
                    wait, dy);
}

template<typename T>
inline T BilinearInterp
(const T *data, const int pixelStride, const int offset, const int lineStride,
 const float x, const float y)
{
    const float x_floor = floor(x);
    const float y_floor = floor(y); 

    const int pos_tl = pixelStride * x_floor + y_floor * lineStride; // top-left
    const int pos_tr = pos_tl + pixelStride;                         // top-right
    const int pos_bl = pos_tl + lineStride;                          // bottom-left
    const int pos_br = pos_bl + pixelStride;                         // bottom-right

    const T data_tl = data[pos_tl + offset]; 
    const T data_tr = data[pos_tr + offset]; 
    const T data_bl = data[pos_bl + offset]; 
    const T data_br = data[pos_br + offset];

    // Using int is faster than using float
    const int wait_b = 256.f * (y - y_floor);
    const int wait_t = 256 - wait_b;
    const int wait_r = 256.f * (x - x_floor);
    const int wait_l = 256 - wait_r;
    
    const T value = (wait_t * (wait_l * data_tl + wait_r * data_tr) +
                     wait_b * (wait_l * data_bl + wait_r * data_br)) >> 8 >> 8;
    const T limit = std::numeric_limits<T>::max();
    return value < limit ? value : limit;
}

