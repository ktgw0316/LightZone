#include <iostream>
#include <lensfun.h>
#include <jni.h>

class LC_lensfun
{
public:
    lfModifier* mod;

    LC_lensfun();
    ~LC_lensfun();

    void initModifier( 
            int fullWidth, int fullHeight,
            const char* cameraMaker, const char* cameraModel,
            const char* lensMaker, const char* lensModel,
            float focal, float aperture);
    const lfCamera* findCamera(
            const char *cameraMaker, const char *cameraModel) const;
    const lfLens* findLens(
            const lfCamera* camera, const char *lensMaker, const char *lensModel) const;
    const lfCamera* const* getCameras() const;
    const lfLens* const* getLenses() const;

private:
    JNIEnv* env;
    static jclass cls;

    lfDatabase* ldb;
};

