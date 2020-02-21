#include <limits>

// cf. https://de.wikipedia.org/wiki/Mitchell-Netravali-Filter
template<int N>
struct MitchellLut {
    float weights[N][4];
    constexpr MitchellLut() : weights() {
        constexpr float b = 3;
        constexpr float c = 3;
        for (int i = 0; i < N; ++i) {
            const auto d = i / float(N); // 0 <= d < 1.
            const auto d2 = d * d;
            const auto d3 = d * d2;

            // coeffs for |x| < 1
            constexpr auto m3 = 12 - 9 / b - 6 / c;
            constexpr auto m2 = -18 + 12 / b + 6 / c;
            constexpr auto m0 = 6 - 2 / b;

            // coeffs for 1 <= |x| < 2
            constexpr auto n3 = -1 / b - 6 / c;
            constexpr auto n2 = 6 / b + 30 / c;
            constexpr auto n1 = -12 / b - 48 / c;
            constexpr auto n0 = 8 / b + 24 / c;

            const auto w0 = (n3 * (1 + 3 * d + 3 * d2 + d3) + n2 * (1 + 2 * d + d2) + n1 * (1 + d) + n0) / 6; // p0: x = d + 1
            const auto w1 = (m3 * d3 + m2 * d2 + m0) / 6; // p1: x = d
            const auto w2 = (m3 * (1 - 3 * d + 3 * d2 - d3) + m2 * (1 - 2 * d + d2) + m0) / 6; // p2: x = 1 - d
            const auto w3 = 1 - w0 - w1 - w2; // p3: x = 2 - d

            weights[i][0] = w0;
            weights[i][1] = w1;
            weights[i][2] = w2;
            weights[i][3] = w3;
        }
    }
};

template<typename T>
inline T interp1D
(const T p[4], const float d)
{
    constexpr int N = 128;
    constexpr auto mitchellLut = MitchellLut<N>();
    const int d_idx = int((N - 1) * d + 0.5); // 0 <= d_idx < N

    const auto lut = mitchellLut.weights[d_idx];
    const auto value = lut[0] * p[0] + lut[1] * p[1] + lut[2] * p[2] + lut[3] * p[3];

    constexpr T limit = std::numeric_limits<T>::max();
    return value < 0 ? 0 : value < limit ? T(value) : limit;
}

template<typename T>
inline T MitchellInterp
(const T *data, const int pixelStride, const int offset, const int lineStride,
 const float x, const float y)
{
    const float x_floor = floor(x);
    const float y_floor = floor(y);
    const float dx = x - x_floor;
    const float dy = y - y_floor;

    // Get the 4x4 pixel values
    T p[4][4];
    // top-left position
    const int pos_00 = offset + pixelStride * (x_floor - 1) + lineStride * (y_floor - 1);

    for (int i = 0, pos_i0 = pos_00; i < 4; ++i, pos_i0 += lineStride) {
        for (int j = 0, pos_ij = pos_i0; j < 4; ++j, pos_ij += pixelStride) {
            p[i][j] = data[pos_ij];
        }
    }

    const T p_interp_x[4] = {
        interp1D(p[0], dx), interp1D(p[1], dx), interp1D(p[2], dx), interp1D(p[3], dx)
    };
    return interp1D(p_interp_x, dy);
}

template<typename T>
inline T BilinearInterp
(const T *data, const int pixelStride, const int offset, const int lineStride,
 const float x, const float y)
{
    const int x_floor = floor(x);
    const int y_floor = floor(y);

    const int pos_tl = pixelStride * x_floor + y_floor * lineStride; // top-left
    const int pos_tr = pos_tl + pixelStride;                         // top-right
    const int pos_bl = pos_tl + lineStride;                          // bottom-left
    const int pos_br = pos_bl + pixelStride;                         // bottom-right

    const T data_tl = data[pos_tl + offset];
    const T data_tr = data[pos_tr + offset];
    const T data_bl = data[pos_bl + offset];
    const T data_br = data[pos_br + offset];

    // Using int is faster than using float
    const int weight_b = 256.f * (y - y_floor);
    const int weight_t = 256 - weight_b;
    const int weight_r = 256.f * (x - x_floor);
    const int weight_l = 256 - weight_r;

    const T value = (weight_t * (weight_l * data_tl + weight_r * data_tr) +
                     weight_b * (weight_l * data_bl + weight_r * data_br)) / 65536;
    constexpr T limit = std::numeric_limits<T>::max();
    return value < limit ? value : limit;
}

