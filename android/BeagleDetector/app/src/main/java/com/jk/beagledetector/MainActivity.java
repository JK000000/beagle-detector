package com.jk.beagledetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private final String TAG = "beagledetector";

    PreviewView viewFinder;
    TextView scoretv;
    ImageButton btn_settings;
    TextView ipstv;

    float[] scores = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);
        scoretv = findViewById(R.id.textView);
        btn_settings = findViewById(R.id.btn_settings);
        ipstv = findViewById(R.id.textViewIPS);

        scoretv.setText("No Beagle");
        scoretv.setTextColor(0xffff0000);

        for(int i=0;i<3;++i)
            scores[i] = 0;

        if(allPermissionsGranted())
        {
            init();
        }
        else
        {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    void update_ui(float new_score)
    {
        for(int i=0;i<2;++i)
        {
            scores[i] = scores[i+1];
        }

        scores[2] = new_score;

        float avg = 0;

        for(float i : scores)
        {
            avg += i;
        }

        avg /= 3;

        float red = 1-avg;
        float green = avg;

        int ired = Math.round(0xff * red);
        int igreen = Math.round(0xff * green);

        int color = 0xff000000;
        color += ired * 0x10000;
        color += igreen * 0x100;

        if(avg >= TmpAppData.getInstance().threshlod)
        {
            if(TmpAppData.getInstance().debug_mode)
                scoretv.setText("B "+new_score);
            else
                scoretv.setText("Beagle");

            scoretv.setTextColor(color);
        }
        else
        {
            if(TmpAppData.getInstance().debug_mode)
                scoretv.setText("N "+new_score);
            else
                scoretv.setText("No Beagle");
            scoretv.setTextColor(color);
        }
    }

    private static Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private Interpreter getInterpreter(int id)
    {
        MappedByteBuffer tfliteModel = null;

        try {
            tfliteModel = FileUtil.loadMappedFile(this, "beagle_model"+id+".tflite");
        }
        catch (Exception e)
        {

        }

        // Initialize interpreter with GPU delegate
        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatList = new CompatibilityList();

        if(compatList.isDelegateSupportedOnThisDevice()){
            // if the device has a supported GPU, add the GPU delegate
            GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            // if the GPU is not supported, run on 4 threads
            options.setNumThreads(4);
        }


        Interpreter tflite = new Interpreter(tfliteModel, options);

        return tflite;
    }

    private void init()
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        Interpreter tflite0 = getInterpreter(0);
       // Interpreter tflite1 = getInterpreter(1);
        final long[] last_time = {0};

        final YuvToRgbConverter[] conv = {new YuvToRgbConverter(this)};

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {

                ProcessCameraProvider cameraProvider;

                try {
                    cameraProvider = cameraProviderFuture.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetAspectRatio(new Rational(480, 270).intValue())
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(ThreadManager.getInstance().cameraExecutor, new ImageAnalysis.Analyzer() {
                    @SuppressLint("UnsafeOptInUsageError")
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                      //  Log.d(TAG, "starting "+image.getWidth() + " " + image.getHeight() + " " + image.getFormat());

                        Bitmap bmp = toBitmap(image.getImage());
                        conv[0].yuvToRgb(image.getImage(), bmp);


                      //  Log.d(TAG, bmp.getWidth() + " " + bmp.getHeight() + " " + bmp.getConfig().toString());

                        image.close();

                        bmp = Bitmap.createScaledBitmap(bmp, 480, 270, true);

                        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                        tensorImage.load(bmp);

                        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                                .add(new Rot90Op(3))
                                .add(new NormalizeOp(0, 255))
                                .build();

                        tensorImage = imageProcessor.process(tensorImage);



                        TensorBuffer output = TensorBuffer.createFixedSize( new int[]{1,1} , DataType.FLOAT32 );


                        tflite0.run(tensorImage.getBuffer(), output.getBuffer());



                        float out = output.getFloatArray()[0];

                        long time= System.currentTimeMillis();

                        long delta = time- last_time[0];

                        double ips = 1000;
                        ips /= delta;

                        last_time[0] = time;

                        double finalIps = ips;
                        ContextCompat.getMainExecutor(MainActivity.this).execute(new Runnable() {
                            @Override
                            public void run() {

                                float nout = out;
                                if(nout < 0.01)
                                    nout = 0;
                                update_ui(nout);
                                ipstv.setText("IPS: "+ String.format("%.3f",finalIps));
                            }
                        });

                    }
                });

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageAnalysis);


                } catch(Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                }

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted()
    {
        boolean ok = true;

        for (String i : REQUIRED_PERMISSIONS)
        {
            if(ContextCompat.checkSelfPermission(getBaseContext(), i) != PackageManager.PERMISSION_GRANTED)
            {
                ok = false;
                break;
            }
        }

        return ok;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                init();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }
}