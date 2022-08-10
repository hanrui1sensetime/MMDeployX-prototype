package mmdeploy;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ResourceUtils;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.markers.DefaultAutoFocusMarker;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import mmdeploy.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private MultiBoxTracker tracker;


    private int current_model = 0;

    private Detector detector;

    private final int width = 480;

    private final int height = 640;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initMMDeploy();
        initCamera();
    }

    private void initCamera() {
        binding.detectCamera.setLifecycleOwner(this);
        binding.detectCamera.setAutoFocusMarker(new DefaultAutoFocusMarker());
        binding.detectCamera.setPlaySounds(false);
        binding.detectCamera.setUseDeviceOrientation(false);

        binding.detectCamera.setPreviewStreamSize(source -> Collections.singletonList(new Size(width, height)));
        binding.detectCamera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                super.onCameraOpened(options);
                tracker = new MultiBoxTracker(MainActivity.this);
                binding.detectPainting.setListeners(canvas -> {
                    tracker.draw(canvas);
                });
                tracker.setFrameConfiguration(width, height, 0);
            }
        });

        binding.detectCamera.addFrameProcessor(frame -> {

            if (frame.getDataClass() == byte[].class) {
                byte[] data = frame.getData();
                YuvImage yuvImage = new YuvImage(data, frame.getFormat(), frame.getSize().getWidth(), frame.getSize().getHeight(), null);

                ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();

                yuvImage.compressToJpeg(new Rect(0,0,frame.getSize().getWidth(),frame.getSize().getHeight()), 100, jpegStream);

                byte[] jpegByteArray = jpegStream.toByteArray();

                Bitmap bitmap = ImageUtils.bytes2Bitmap(jpegByteArray);

                Bitmap rotate = ImageUtils.rotate(bitmap, frame.getRotation(), 0, 0);

                drawDetection(rotate);


            } else if (frame.getDataClass() == Image.class) {
                Image data = frame.getData();
                // Process android.media.Image...
            }
        });
    }

    private void initMMDeploy() {

        String workDir = getBasePath();

        if (ResourceUtils.copyFileFromAssets("dump_info", workDir)){
            reload(workDir, current_model);
        }

    }

    private void reload(String workDir, int modelID)
    {
        String [] modelTypes = {"mobilessd", "yolo", "yolox"};
        String modelPath=workDir + "/" + modelTypes[(int)modelID];
        String deviceName="cpu";
        int deviceID = 0;
        this.detector = new Detector(modelPath, deviceName, deviceID);

    }

    private void drawDetection(Bitmap srcImg){
        int width = srcImg.getWidth();
        int height = srcImg.getHeight();
        int[] pixArr = new int[width*height];
        // bitmap to arr
        srcImg.getPixels(pixArr,0,width,0,0,width,height);
        byte [] bgra = pixArrToBgra(pixArr, width, height);
        byte [] data = BgraToBgr(bgra, width, height);
        Mat rgb  = new Mat(srcImg.getHeight(), srcImg.getWidth(), 3,
                PixelFormat.BGR, DataType.INT8, data);
        Detector.Result[] result = detector.apply(rgb);

        List<Detector.Result> results = Arrays.asList(result);

        List<Detector.Result> collect = results.parallelStream().filter(result1 -> result1.getScore() >= 0.3).collect(Collectors.toList());

        tracker.trackResults(collect);
        binding.detectPainting.postInvalidate();
    }

    public static byte[] pixArrToBgra(int [] pixArray, int w, int h) {
        byte[] bgra = new byte [w * h * 4];
        for (int i = 0; i < w * h; i++) {
            byte a = (byte)(pixArray[i] >> 24);
            byte r = (byte)((pixArray[i] >> 16) & 0x000000ff);
            byte g = (byte)((pixArray[i] >> 8) & 0x000000ff);
            byte b = (byte)(pixArray[i] & 0x000000ff);
            bgra[i * 4] = b;
            bgra[i * 4 + 1] = g;
            bgra[i * 4 + 2] = r;
            bgra[i * 4 + 3] = a;
        }
        return bgra;
    }

    public static byte[] BgraToBgr(byte[] bgra, int w, int h) {
        byte[] pixels = new byte[(bgra.length / 4) * 3];
        int count = bgra.length / 4;

        for (int i = 0; i < count; i++) {
            pixels[i * 3] = bgra[i * 4];    //B
            pixels[i * 3 + 1] = bgra[i * 4 + 1];//G
            pixels[i * 3 + 2] = bgra[i * 4 + 2];    //R
        }
        return pixels;
    }

    public static String getBasePath() {
        return PathUtils.getExternalAppFilesPath() + File.separator
                + "file";
    }
}
