# MMDeployX-prototype
The prototype of MMDeployX: An object detection Android demo using [MMDeploy](https://github.com/open-mmlab/mmdeploy).

## Android apk file download
[Download here](https://media.githubusercontent.com/media/hanrui1sensetime/MMDeployX-APK/master/MMDeployX-prototype-release/release/app-release.apk)

## Preparation
- Android API >= 30
- cpu architecture `arm64-v8a`
If you are using other platforms, you should build mmdeploy by yourself. See [Build for Android](https://github.com/open-mmlab/mmdeploy/blob/master/docs/en/01-how-to-build/android.md).
Then, replace [Lib folder](https://github.com/hanrui1sensetime/MMDeployX-prototype/tree/master/mmdeployxdetector/app/libs) with your build libs in the folder `${YOUR_ARM_ABI}`.

## Build and run!
Open this project with Android Studio, build it and enjoy!
