#include <iostream>
#include <memory>
#include <lensfun.h>
#include <jni.h>

class LC_lensfun {
public:
  std::unique_ptr<lfModifier> mod;

  LC_lensfun(const char *path);
  ~LC_lensfun() = default;

  const lfCamera *findCamera(const char *cameraMaker,
                             const char *cameraModel) const;
  const lfLens *findLens(const lfCamera *camera, const char *lensMaker,
                         const char *lensModel) const;
  const lfCamera *const *getCameras() const;
  const lfLens *const *getLenses() const;

  lfLens *getDefaultLens();

  void initModifier(int fullWidth, int fullHeight, const char *cameraMaker,
                    const char *cameraModel, const char *lensMaker,
                    const char *lensModel, float focal, float aperture);
  void initModifier(int fullWidth, int fullHeight, float crop,
                    const lfLens *lens, float focal, float aperture);
  void applyModifier(const unsigned short *srcData, unsigned short *dstData,
                     int srcRectX, int srcRectY, int srcRectWidth,
                     int srcRectHeight, int dstRectX, int dstRectY,
                     int dstRectWidth, int dstRectHeight, int srcPixelStride,
                     int dstPixelStride, int srcROffset, int srcGOffset,
                     int srcBOffset, int dstROffset, int dstGOffset,
                     int dstBOffset, int srcLineStride,
                     int dstLineStride) const;
  void backwardMapRect(int *srcRectParams, int dstRectX, int dstRectY,
                       int dstRectWidth, int dstRectHeight) const;

private:
  JNIEnv *env;
  static jclass cls;

  std::unique_ptr<lfDatabase> ldb;
  lfLens *default_lens = nullptr;
};
