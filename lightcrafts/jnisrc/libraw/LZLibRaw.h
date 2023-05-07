#include <libraw.h>

class LZLibRaw : public LibRaw
{
public:
    LZLibRaw() = default;
    ~LZLibRaw() = default;

    void set_interpolate_bayer_handler(process_step_callback cb) {
        callbacks.interpolate_bayer_cb = cb;
    }

    void border_interpolate(int border) {
        LibRaw::border_interpolate(border);
    }
};
