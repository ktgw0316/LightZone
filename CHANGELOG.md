# Changelog

## [5.0.1](https://github.com/ktgw0316/LightZone/compare/v5.0.0...v5.0.1) (2026-07-18)


### Bug Fixes

* add buffer-length check in dcraw_lz.c ([20f47d9](https://github.com/ktgw0316/LightZone/commit/20f47d974f67a22a5945fbf76955014a4a745225))
* **mac:** Some emojis in file path cause crush ([1f43130](https://github.com/ktgw0316/LightZone/commit/1f43130fc770d5feefe7733e84d1da1cac68cd4d))

## [5.0.0](https://github.com/ktgw0316/LightZone/compare/5.0.0...v5.0.0) (2026-05-22)


### Features

* Update FlatLaf to 3.6 ([23d2916](https://github.com/ktgw0316/LightZone/commit/23d2916686abe7f61bbcd1cc4f11ccbdef86306e))
* Update ImageN, EJML, FlatLaf ([e4a6617](https://github.com/ktgw0316/LightZone/commit/e4a6617718a0fa9c5f6f8200ead552e2d28442e2))
* Use slf4j for logging ([756ff3d](https://github.com/ktgw0316/LightZone/commit/756ff3d167537e67914b0abd2ec838691e0c7de7))


### Bug Fixes

* "luminosity" is not used in BilateralFilterOpImage/RIF ([28a7518](https://github.com/ktgw0316/LightZone/commit/28a751804366b337006e61e9b351c5d1ca591c8a))
* "luminosity" is not used in BilateralFilterOpImage/RIF ([6d65200](https://github.com/ktgw0316/LightZone/commit/6d65200a522deba5ebc8d40795e713462d42bbba))
* Add java.naming module that is required by logback ([015fc17](https://github.com/ktgw0316/LightZone/commit/015fc1774b14397b210c6b9a44924019ec3f0029))
* Add missing o.e.imagen.media.convolve dependency ([283ab3c](https://github.com/ktgw0316/LightZone/commit/283ab3cb61848e0194cd871c073e0c8b4a80eaab))
* Add missing o.e.imagen.media.format dependency ([4c17d87](https://github.com/ktgw0316/LightZone/commit/4c17d87105aae34d0d81c7832edd4a92b669a4df))
* Argument number of BilateralFilterDescriptor ([28a7518](https://github.com/ktgw0316/LightZone/commit/28a751804366b337006e61e9b351c5d1ca591c8a))
* Correct & re-enable CheckForUpdate ([f8fc9a2](https://github.com/ktgw0316/LightZone/commit/f8fc9a2f284d80ca29f185aa1a5cdb2e247c36cd)), closes [#376](https://github.com/ktgw0316/LightZone/issues/376)
* Correct update URI ([4bf627d](https://github.com/ktgw0316/LightZone/commit/4bf627d1c85b079db8bbe56df64943a4454e080c))
* Crash with IllegalArgumentException ([319351b](https://github.com/ktgw0316/LightZone/commit/319351b06c3e094e40566e11620a6976fc03aa36))
* Create log under $HOME/.lightzone/logs/ ([dcd9a3f](https://github.com/ktgw0316/LightZone/commit/dcd9a3ff46e761db5af3db4b7553e3187c5ced23))
* **github:** Windows build failure ([ade851f](https://github.com/ktgw0316/LightZone/commit/ade851fa3d19d6f7b26e6daa154eba47e2b75bed))
* **gradle:** Do not run the app twice when gradle run ([f171d45](https://github.com/ktgw0316/LightZone/commit/f171d45bc51114439d79cf659ba9ff7abf731fa6))
* **gradle:** Show version on splash screen ([da65daf](https://github.com/ktgw0316/LightZone/commit/da65daf966b4d04587ea76a0549dbcc0a4210666))
* **gradle:** Unsupported Kotlin plugin version ([0f98989](https://github.com/ktgw0316/LightZone/commit/0f989892bf318cdaf90e8d27d8e1f5a6b024df82))
* **gradle:** Unsupported Kotlin plugin version ([507b4da](https://github.com/ktgw0316/LightZone/commit/507b4da18106d69d1a5ac8eec3134c7c47c64b12))
* **gradle:** Update dependencies ([b154484](https://github.com/ktgw0316/LightZone/commit/b154484766360f21f00e6d343b75f37fca7ccffb))
* ImageN descriptor registration error ([f09230b](https://github.com/ktgw0316/LightZone/commit/f09230b039c785425bfb73a1a355be649b20fb66))
* **linux:** libLCJNI.so load error ([451ef76](https://github.com/ktgw0316/LightZone/commit/451ef76f67e05a0ba734e174badec0f4cf3bf0ef))
* **linux:** libLCJNI.so load error on libJAI.so ([c134251](https://github.com/ktgw0316/LightZone/commit/c1342514fd49b0bd31a77725df80eeaa03f53c6b))
* **linux:** Place dcraw_lz under PATH ([b273b83](https://github.com/ktgw0316/LightZone/commit/b273b83ace176468d2163999920bcddcd3898258))
* **linux:** Remove lightzonehelp.jar ([1785a23](https://github.com/ktgw0316/LightZone/commit/1785a23b2f28f666e76ae1f31c668b4709c564c4))
* **mac:** CFURLGetFSRef is deprecated ([1c31f58](https://github.com/ktgw0316/LightZone/commit/1c31f5825d45b7ecbb4165e8c22ce4527c421d5c))
* **mac:** CMProfileLocation is deprecated ([137c50a](https://github.com/ktgw0316/LightZone/commit/137c50a5aab1f4ec2ce3c03f51439f5c354e2e5c))
* **mac:** Format string is not a string literal ([b773599](https://github.com/ktgw0316/LightZone/commit/b773599a4bad068370963e7b2437bf751fd8bf8f))
* **mac:** Incompatible pointer types ([244a1d6](https://github.com/ktgw0316/LightZone/commit/244a1d6f51dc523012307281a614fcdb0f2f4e29))
* **mac:** non-void function does not return a value ([b6b0247](https://github.com/ktgw0316/LightZone/commit/b6b0247c76c3d0afe32fe038667d54553fa6bb1a))
* **mac:** NSCancelButton is deprecated ([aa440b8](https://github.com/ktgw0316/LightZone/commit/aa440b8d916c918890691054119039462461ab79))
* **macos:** focus existing window on dock-icon reopen ([efa25e0](https://github.com/ktgw0316/LightZone/commit/efa25e02a99bbb57cafb2930bbb13fd26354b8c6))
* **macos:** use most-recently-focused frame on dock reopen ([4a82637](https://github.com/ktgw0316/LightZone/commit/4a8263740bc6f693688e48a6c4fce64bbda50fb2))
* RejectedExecutionException ([02b0ecb](https://github.com/ktgw0316/LightZone/commit/02b0ecb36adacff8316735ce0f4754a4baea50b7))
* Remove unsupported ImageN Transpose ([a132a0f](https://github.com/ktgw0316/LightZone/commit/a132a0f19b6b7f051d91ed58b5480a8902b4bf9b))
* Resource keys for LCMSColorConvertDescriptor ([fc1bfb1](https://github.com/ktgw0316/LightZone/commit/fc1bfb1531d4b0f7ac406d67ca9d220731163f05))
* Segm build errors ([04c0a95](https://github.com/ktgw0316/LightZone/commit/04c0a95b6b3ae1c3651fdf32d69ef5585c83eadc))
* **segm:** Remove 'register' to fix compiler warning ([08435da](https://github.com/ktgw0316/LightZone/commit/08435dad9afe0699d2dac11541fd3eb58d83d12c))
* Set valid params for LCMSColorConvert ([df97c80](https://github.com/ktgw0316/LightZone/commit/df97c808889dd504423b01a6016ba29866d11942))
* Tile update artifact ([9d38898](https://github.com/ktgw0316/LightZone/commit/9d388980ee007cef676a906813b14ec23309f4a2))
* Typo in RawAdjustmentsDescriptor ([8529654](https://github.com/ktgw0316/LightZone/commit/8529654e03fbead8336fd246ced4e39dbf293cfe))
* Use MaxFilter instead of legacy Dilate operation ([b9bab7a](https://github.com/ktgw0316/LightZone/commit/b9bab7ad6125ae49b7c92aecfb4849b3ae458047))
* **win:** Add iconv dependency to build help files ([5209f7d](https://github.com/ktgw0316/LightZone/commit/5209f7d39f975f51b0e2a8048ea4df9449aa951e))
* **win:** Missing dependency ([5c33bd0](https://github.com/ktgw0316/LightZone/commit/5c33bd065a5b4bb0b7cf831f12079ebf2f0f2ccf))


### Miscellaneous Chores

* Set version 5.0.0.rc1 ([bf4a998](https://github.com/ktgw0316/LightZone/commit/bf4a9986ce543374ee272d7f0b183adac9579651))
