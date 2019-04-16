package com.example.machinelearning_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;




public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE1 = 1;
    static final int REQUEST_IMAGE_CAPTURE2 = 2;


    ImageView imView;
    RecyclerView Rview;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private List<String> myDataset = new ArrayList<>();
    MyRecyclerViewAdapter adapter;
    private ImageClassifier classifier;
    private Bitmap mSelectedImage;
    private Button Textify;
    private Button classify;
    private Integer mImageMaxWidth;
    // Max height (portrait mode)
    private Integer mImageMaxHeight;
//    String[] blockLines = new String[20];
    private ImageReader imageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            classifier = new ImageClassifier(MainActivity.this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        imView = (ImageView) findViewById(R.id.imageView);
        recyclerView = (RecyclerView) findViewById(R.id.RView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        int orientation = new LinearLayoutManager(this).getOrientation();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                orientation);
        recyclerView.addItemDecoration(dividerItemDecoration);

//        setUpCameraOutputs(imView.getWidth(), imView.getHeight());
//        configureTransform(imView.getWidth(), imView.getHeight());

        myDataset.add(" ");
        myDataset.add(" ");
        myDataset.add(" ");
        myDataset.add(" ");

        adapter = new MyRecyclerViewAdapter(this, myDataset);
        recyclerView.setAdapter(adapter);

        Textify = findViewById(R.id.button1);
        classify = findViewById(R.id.button2);

    }

    public void captureText(View view) {
        myDataset.clear();
//        imView.setImageBitmap(null);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE2);
        }
    }


    public void ClassifyObj(View view) {
        myDataset.clear();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE1);
        }

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE1 && resultCode == RESULT_OK)
        {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            ImageView picture = (ImageView) findViewById(R.id.imageView);
            picture.setImageBitmap(bitmap);


            String text = classifier.classifyFrame(bitmap);


            String[] arrOfString = text.split("\n");

            for (int i = 0; i < arrOfString.length; i++) {
                myDataset.add(arrOfString[i]);
            }

            adapter = new MyRecyclerViewAdapter(this, myDataset);
            recyclerView.setAdapter(adapter);

        }

        else if (requestCode == REQUEST_IMAGE_CAPTURE2 && resultCode == RESULT_OK)
        {

            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            ImageView picture = (ImageView) findViewById(R.id.imageView);
            picture.setImageBitmap(bitmap);


            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            recognizer.processImage(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText texts) {

                                    List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
                                    if (blocks.size() == 0) {
                                        myDataset.add("No text found");
                                        return;
                                    }

//                                    myDataset.clear();
                                    String message = Integer.toString(blocks.size()) + " Found blocks";

//                                    myDataset.add(message);
                                    for (int i = 0; i < blocks.size(); i++) {
                                        List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
                                        for (int j = 0; j < lines.size(); j++) {

//                                            blockLines[j] = lines.get(j).getText();
                                            myDataset.add(lines.get(j).getText());

                                        }
                                    }

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    myDataset.add("task failed");
                                }
                            });

            adapter = new MyRecyclerViewAdapter(this, myDataset);
            recyclerView.setAdapter(adapter);
//

        }
    }


    private Integer getImageMaxWidth () {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = imView.getWidth();
        }

        return mImageMaxWidth;
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxHeight () {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight =
                    imView.getHeight();
        }

        return mImageMaxHeight;
    }

    private Pair<Integer, Integer> getTargetedWidthHeight () {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = getImageMaxWidth();
        int maxHeightForPortraitMode = getImageMaxHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }

    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private static Size chooseOptimalSize(
            Size[] choices,
            int textureViewWidth,
            int textureViewHeight,
            int maxWidth,
            int maxHeight,
            Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth
                    && option.getHeight() <= maxHeight
                    && option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = this;
        if (null == imView || null == previewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        imView.setImageMatrix(matrix);
    }


    private static final String TAG = "TfLiteCameraDemo";
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /** Max preview height that is guaranteed by Camera2 API */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private Size previewSize;
    private String cameraId;

}
