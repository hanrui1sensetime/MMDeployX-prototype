// Copyright (c) OpenMMLab. All rights reserved.
// This file is modified from https://github.com/nihui/ncnn-android-nanodet and
// https://github.com/EdVince/Android_learning/tree/main/ncnnnanodetCameraX

package com.openmmlab.mmdeployxdetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.res.AssetManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.Type;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private PointerWrapper handle;
    private Spinner spinnerModel;
    private int current_model = 0;
    ImageView ivBitmap;

    ImageAnalysis imageAnalysis;

    // Create an instance of MMDeptloyDetector.
    private MMDeployDetector mmdeployxdetector = new MMDeployDetector();

    // 查询是否满足当前所有权限需求
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("MainActivity", "debugging java mainactivity start!");
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // 将app最上面那个带名字的title隐藏掉
        setContentView(R.layout.activity_main); // 设置控件的布局
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 锁定竖屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 保持屏幕常亮

        ivBitmap = findViewById(R.id.ivBitmap);         // 绑定bitmap

        // 检查权限并启动摄像头
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        String workDir = getDumpInfoPath("dump_info", this);

        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_model)
                {
                    current_model = position;
                    reload(workDir, current_model);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
        reload(workDir, current_model);
    }

    private void reload(String workDir, int modelID)
    {
        this.handle = new PointerWrapper("mm_handle_t", (long)0);
        String [] modelTypes = {"mobilessd", "yolo", "yolox"};
        String modelPath=workDir + "/" + modelTypes[(int)modelID];
        String deviceName="cpu";
        int deviceID = 0;
        this.handle = mmdeployxdetector.mmdeployDetectorCreateByPath(modelPath, deviceName, deviceID, this.handle);
        // will this.handle changes apparently?
        if (this.handle.address == 0)
        {
            Log.e("MainActivity", "addModel failed");
        }
    }


    private static String testAssetmanagerList(String dir, Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] modelNames = assetManager.list(dir);
        } catch (IOException ex) {
            Log.i("打开失败", "无法打开此文件");
        }

        return "";
    }


    private static String getDumpInfoPath(String dir, Context context) {

        AssetManager assetManager = context.getAssets();
        try {
            String[] modelNames = assetManager.list(dir);
            String separator = File.separator;
            Log.d("MainActivity", "modelName.length: " + modelNames.length);
            if (modelNames.length > 0) {
                for (String modeldir: modelNames) {
                    try {
                        File modelfolder = new File(dir + separator + modeldir);
                        String[] fileNames = assetManager.list(dir + separator + modeldir);
                        if (fileNames.length == 0) {
                            continue;
                        }
                        File outmodeldir = new File(context.getFilesDir() + separator + modeldir);
                        outmodeldir.mkdir();
                        // Log.e("debugging outmodeldir:", outmodeldir.getAbsolutePath());
                        for (String fileName: fileNames) {
                            BufferedInputStream inputStream = null;
                            try {
                                inputStream = new BufferedInputStream(assetManager.open(dir + separator + modeldir+ separator + fileName)); //打开文件放入输入流中
                                byte[] data = new byte[inputStream.available()]; //输入流信息放入data中
                                inputStream.read(data);  //输入流读取data
                                inputStream.close();  //输入流关闭
                                // Create copy file in storage.
                                File outFile = new File(outmodeldir, fileName);  //新文件(夹)创建,应用文件目录
                                FileOutputStream os = new FileOutputStream(outFile);  //输出流
                                os.write(data);  //写入数据.即完成了拷贝
                                os.close();  //关闭
                                // Return a path to file which may be read in common way.
                                Log.d("filePath:",outFile.getAbsolutePath());
                            } catch (IOException ex) {
                                Log.e("打开失败", "无法打开此文件");
                            }
                        }
                    } catch (IOException ex) {
                        Log.e("打开失败", "无法打开子文件夹");
                    }

                }
            }
        } catch (IOException ex) {
            Log.e("打开失败", "无法打开文件夹");
        }


        return context.getFilesDir().getAbsolutePath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 权限请求回调函数
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        CameraX.unbindAll(); // 解绑CameraX
        imageAnalysis = setImageAnalysis(); // 设置图像分析
        // 绑定CameraX到生命周期，并喂入分析
        CameraX.bindToLifecycle(this,imageAnalysis);
    }

    private ImageAnalysis setImageAnalysis() {
        // 设置用来做图像分析的线程
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();

        // 配置图像分析并生成imageanalysis
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE) // 接收最新的图
                .setCallbackHandler(new Handler(analyzerThread.getLooper())) // 设置回调
                .setImageQueueDepth(1).build(); // 设置图像队列深度为1
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        // 设置分析器
        imageAnalysis.setAnalyzer(
            new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(ImageProxy image, int rotationDegrees) {

                    // Get image data by ImageProxy in CameraX.
                    // 优点：是摄像头获取的真实分辨率
                    // 缺点：提供的是YUV格式的Image，转Bitmap比较困难
                    Image img = image.getImage();
                    final Bitmap bitmap = onImageAvailable(img);
                    if(bitmap==null) return;
                    Matrix matrix = new Matrix();
                    matrix.setRotate(90);
                    final Bitmap result = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

                    // run ncnn model here.
                    int width = result.getWidth();
                    int height = result.getHeight();
                    int[] pixArr = new int[width*height];
                    // bitmap to arr
                    result.getPixels(pixArr,0,width,0,0,width,height);
                    // inference
                    PointerWrapper [] pImageArray = DetectorTools.pixArrToMat(width, height, pixArr);
                    PointerWrapper pMat = pImageArray[0];
                    PointerWrapper pSrcMat = pImageArray[1];
                    PointerWrapper pRgb = pImageArray[2];
                    PointerWrapper pBboxes = DetectorTools.createCppObject("mm_detect_t*");
                    PointerWrapper pResultCount = DetectorTools.createCppObject("int*");
                    mmdeployxdetector.mmdeployDetectorApply(handle, pMat, 1, pBboxes, pResultCount);
                    String [] classNames = {"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
                                            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
                                            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
                                            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
                                            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
                                            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
                                            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
                                            "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
                                            "hair drier", "toothbrush"};
                    int [] colors = {54, 67, 244, 99,  30, 233, 176, 39, 156, 183, 58, 103, 181, 81, 63, 243, 150, 33,
                                     244, 169, 3, 212, 188, 0, 136, 150, 0, 80, 175, 76, 74, 195, 139, 57, 220, 205, 59,
                                     235, 255, 7, 193, 255, 0, 152, 255, 34, 87, 255, 72, 85, 121, 158, 158, 158, 139, 125,
                                     96};
                    DetectorTools.drawResult(classNames, colors, pSrcMat, pRgb, pBboxes, pResultCount);
                    mmdeployxdetector.mmdeployDetectorReleaseResult(pBboxes, pResultCount, 1);
                    Bitmap newBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    newBitmap.setPixels(pixArr,0,width,0,0,width,height);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ivBitmap.setImageBitmap(newBitmap); // 将推理后的bitmap喂回去
                        }
                    });
                }
            });
        return imageAnalysis;
    }

    public Bitmap onImageAvailable(Image image) {
        ByteArrayOutputStream outputbytes = new ByteArrayOutputStream();
        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
        byte[] data0 = new byte[bufferY.remaining()];
        bufferY.get(data0);
        ByteBuffer bufferU = image.getPlanes()[1].getBuffer();
        byte[] data1 = new byte[bufferU.remaining()];
        bufferU.get(data1);
        ByteBuffer bufferV = image.getPlanes()[2].getBuffer();
        byte[] data2 = new byte[bufferV.remaining()];
        bufferV.get(data2);
        try {
            outputbytes.write(data0);
            outputbytes.write(data2);
            outputbytes.write(data1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final YuvImage yuvImage = new YuvImage(outputbytes.toByteArray(), ImageFormat.NV21, image.getWidth(),image.getHeight(), null);
        ByteArrayOutputStream outBitmap = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 95, outBitmap);
        Bitmap bitmap = BitmapFactory.decodeByteArray(outBitmap.toByteArray(), 0, outBitmap.size());
        image.close();
        return bitmap;
    }
}
