项目名称：ncnn-androidX-detection-demo

功能：使用ncnn在安卓手机上进行目标检测，利用OpenMMLab的MMDeploy的SDK进行检测模型部署。

注：本项目基于nihui的nanodetncnn项目(https://github.com/nihui/ncnn-android-nanodet)以及EdVince的AndroidLearning项目(https://github.com/EdVince/Android_learning/tree/main/ncnnnanodetCameraX)进行二次开发。


特点：

1. assets部分变成了MMDeploy SDK中需要的dump info，并复制到sdk卡上用以提供绝对路径被SDK中的API调用(TODO: 模型可选)。
2. 使用ncnn进行网络推理、使用opencv-mobile进行图像处理、使用CameraX API进行摄像头交互。
3. 检测过程通过MMDeploy SDK中的API实现。
