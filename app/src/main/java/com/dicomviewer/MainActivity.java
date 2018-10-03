package com.dicomviewer;

import android.Manifest;
import android.content.Context;
import android.content.EntityIterator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.imebra.ColorTransformsFactory;
import com.imebra.*;
import com.imebra.DrawBitmap;
import com.imebra.Image;
import com.imebra.LUT;
import com.imebra.TransformsChain;
import com.imebra.VOILUT;
import com.imebra.VOIs;
import com.imebra.drawBitmapType_t;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int PERMISSIONS_ALL = 1;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        String [] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_ALL);
        }
        System.loadLibrary("imebra_lib");
        File file = new File(getFilesDir(), "/images");
        Log.d("Directory external ", Environment.getExternalStorageState());
        File external = new File(Environment.getExternalStorageDirectory() + "/DICOMViewer");
        Log.d("Directory exists ", String.valueOf(external.exists()));
        if(external.exists()){
            Log.d("Directory Path ", external.getPath());
            File[] dirFiles = external.listFiles();
            String path = dirFiles[0].getPath();
            Log.d("Directory file Path: ", path);
            displayImage(path);
        }
    }

    private void displayImage(String directoryPath){
        Log.d("Path of file: ", directoryPath);
        DataSet dicomDataSet = CodecFactory.load(directoryPath, 2048);
        Image image = dicomDataSet.getImageApplyModalityTransform(0);

        String colorSpace = image.getColorSpace();

        long width = image.getWidth();
        long height = image.getHeight();

        TransformsChain chain = new TransformsChain();

        if(ColorTransformsFactory.isMonochrome(colorSpace)){
            VOILUT voilutTransform = new VOILUT();

            // Retrieve the VOIs (center/width pairs)
            VOIs vois = dicomDataSet.getVOIs();

            // Retrieve the LUTs
            List<LUT> luts = new ArrayList<>();
            for(long scanLUTs = 0; ; scanLUTs++) {
                try {
                    luts.add(dicomDataSet.getLUT(new com.imebra.TagId(0x0028,0x3010), scanLUTs));
                }
                catch(Exception e) {
                    break;
                }
            }

            if(!vois.isEmpty()) {
                voilutTransform.setCenterWidth(vois.get(0).getCenter(), vois.get(0).getWidth());
            }
            else if(!luts.isEmpty()) {
                voilutTransform.setLUT(luts.get(0));
            }
            else {
                voilutTransform.applyOptimalVOI(image, 0, 0, width, height);
            }

            chain.addTransform(voilutTransform);
        }
        DrawBitmap draw = new DrawBitmap();
        long requestedBufferSize = draw.getBitmap(image, drawBitmapType_t.drawBitmapRGBA, 4, new byte[0]);

        byte buffer[] = new byte[(int) requestedBufferSize]; // Ideally you want to reuse this in subsequent calls to getBitmap()
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        // Now fill the buffer with the image data and create a bitmap from it
        draw.getBitmap(image, drawBitmapType_t.drawBitmapRGBA, 4, buffer);
        Bitmap renderBitmap = Bitmap.createBitmap((int) image.getWidth(), (int) image.getHeight(), Bitmap.Config.ARGB_8888);
        renderBitmap.copyPixelsFromBuffer(byteBuffer);

        imageView.setImageBitmap(renderBitmap);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
