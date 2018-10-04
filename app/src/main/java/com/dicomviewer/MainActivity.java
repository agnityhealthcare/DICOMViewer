package com.dicomviewer;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    private final static int PERMISSIONS_ALL = 1;
    private final static int IMAGE_SELECTOR = 2;
    ImageView imageView;
    TextView name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        name = findViewById(R.id.name);
        String [] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_ALL);
        }
        System.loadLibrary("imebra_lib");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IMAGE_SELECTOR){
            if(resultCode == RESULT_OK){
                String path = data.getStringExtra("path");
                displayImage(path);
            }
            else{
                Toast.makeText(this, "Error loading image", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_selector_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.select_image:
                Intent in = new Intent(this, FileList.class);
                startActivityForResult(in, IMAGE_SELECTOR);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayImage(String directoryPath){
        Log.d("Path of file: ", directoryPath);
        DataSet dicomDataSet = CodecFactory.load(directoryPath);
        String n = dicomDataSet.getString(new TagId(0x10, 0x10), 0);
        if(n != null){
           name.setText(n);
        }
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
