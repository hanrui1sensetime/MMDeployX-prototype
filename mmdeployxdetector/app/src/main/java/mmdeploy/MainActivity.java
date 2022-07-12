// Copyright (c) OpenMMLab. All rights reserved.
// This file is modified from https://github.com/nihui/ncnn-android-nanodet and
// https://github.com/EdVince/Android_learning/tree/main/ncnnnanodetCameraX

package mmdeploy;

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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
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

    static {
        System.loadLibrary("mmdeploy_java");
    }

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private Spinner spinnerModel;
    private int current_model = 0;
    private String [] classNames = {"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
                                    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
                                    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
                                    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
                                    "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
                                    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
                                    "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
                                    "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
                                    "hair drier", "toothbrush"};
    private int [] colors = {54, 67, 244, 99,  30, 233, 176, 39, 156, 183, 58, 103, 181, 81, 63, 243, 150, 33,
                             244, 169, 3, 212, 188, 0, 136, 150, 0, 80, 175, 76, 74, 195, 139, 57, 220, 205, 59,
                             235, 255, 7, 193, 255, 0, 152, 255, 34, 87, 255, 72, 85, 121, 158, 158, 158, 139, 125,
                             96};
    ImageView ivBitmap;

    ImageAnalysis imageAnalysis;

    // Create an instance of MMDeptloyDetector.
    private Detector detector;

    public static byte[] bitmapToBgr(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buffer);

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[(rgba.length / 4) * 3];

        int count = rgba.length / 4;

        for (int i = 0; i < count; i++) {

            pixels[i * 3 + 2] = rgba[i * 4];    //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];//G
            pixels[i * 3] = rgba[i * 4 + 2];    //B

        }

        return pixels;
    }
    public static double t0 = 0.f;
    public static double [] fps_history = new double[10];
    public static int drawFPS(Canvas canvas, Bitmap srcImg) {
        // resolve moving average
        float avg_fps = 0.f;

        double t1 = System.currentTimeMillis();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        double fps = 1000.0 / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
        String text = String.format("%dx%d FPS=%.2f", srcImg.getWidth(), srcImg.getHeight(), avg_fps);
        int baseLine = 0;
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        Rect textBound = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBound);

        int labelH = textBound.height();
        int labelW = textBound.width();

        int y = 0;
        int x = srcImg.getWidth() - labelW;
        paint.setColor(Color.rgb(255, 255, 255));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(x, y, srcImg.getWidth(), labelH + baseLine, paint);
        paint.setColor(Color.rgb(0, 0, 0));
        canvas.drawText(text, x, y + labelH, paint);

        return 0;
    }

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
        String [] modelTypes = {"mobilessd", "yolo", "yolox"};
        String modelPath=workDir + "/" + modelTypes[(int)modelID];
        String deviceName="cpu";
        int deviceID = 0;
        this.detector = new Detector(modelPath, deviceName, deviceID);
        if (this.detector == null)
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

    private void drawDetectResult(Bitmap srcImg, Detector.Result[] result) {
        Canvas canvas = new Canvas(srcImg);
        for (int i = 0; i < result.length; i++) {
            Detector.Result value = result[i];
            if ((value.bbox.right - value.bbox.left) < 1 || (value.bbox.bottom - value.bbox.top) < 1) {
                continue;
            }
            // skip detections less than specified score threshold
            if (value.score < 0.2) {
                continue;
            }
            Log.e("MainActivity", "debugging result " + i + " label: " + value.label_id + "classname: " + this.classNames[value.label_id] + "score: " + value.score);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(this.colors[(i % 19) * 3], this.colors[(i % 19) * 3 + 1], this.colors[(i % 19) * 3 + 2]));
            canvas.drawRect(value.bbox.left, value.bbox.top, value.bbox.right, value.bbox.bottom, paint);
            // Really need + 1 ?
            String labelText = String.format("%s %.1f%%", this.classNames[value.label_id], value.score * 100);
            int baseLine = 0;

            Rect textBound = new Rect();
            paint.getTextBounds(labelText, 0, labelText.length(), textBound);

            int labelH = textBound.height();
            int labelW = textBound.width();

            double x = value.bbox.left;
            double y = value.bbox.top - labelH - baseLine;
            if (y < 0)
                y = 0;
            if (x + labelW > srcImg.getWidth())
                x = srcImg.getWidth() - labelW;

            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect((float)x, (float) y, (float) x + labelW, (float)y + labelH + baseLine, paint);
            paint.setStyle(Paint.Style.STROKE);
            if (this.colors[(i % 19) * 3] + this.colors[(i % 19) * 3 + 1] + this.colors[(i % 19) * 3 + 2] >= 381) {
                paint.setColor(Color.rgb(0, 0, 0));
            }
            else {
                paint.setColor(Color.rgb(255, 255, 255));
            }
            canvas.drawText(labelText, (float)x, (float)y + labelH, paint);
        }
        drawFPS(canvas, srcImg);

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
                    final Bitmap srcImg = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                    int width = srcImg.getWidth();
                    int height = srcImg.getHeight();
                    int[] pixArr = new int[width*height];
                    // bitmap to arr
                    srcImg.getPixels(pixArr,0,width,0,0,width,height);
                    byte [] data = bitmapToBgr(srcImg);
                    Mat rgb  = new Mat(bitmap.getHeight(), bitmap.getWidth(), 3,
                                       PixelFormat.BGR, DataType.INT8, data);
                    Detector.Result[] result = detector.apply(rgb);
                    Log.e("MainActivity", "debugging after detector.apply! result length: " + result.length);
                    drawDetectResult(srcImg, result);

                    Bitmap newBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    newBitmap.setPixels(pixArr,0,width,0,0,width,height);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ivBitmap.setImageBitmap(srcImg); // 将推理后的bitmap喂回去
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
