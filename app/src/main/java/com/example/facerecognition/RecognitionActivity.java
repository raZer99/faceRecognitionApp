package com.example.facerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.List;

public class RecognitionActivity extends AppCompatActivity {

    private static final String TAG = "RecognitionActivity";
    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;

    private ImageView imageView, imageView2;
    private TextView recognitionResult;
    private Button recognizeButton;
    private CardView galleryCard, cameraCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        // New UI components
        imageView2 = findViewById(R.id.imageView2);
        recognitionResult = findViewById(R.id.recognitionResult);
        recognizeButton = findViewById(R.id.recognizeButton);
        galleryCard = findViewById(R.id.gallerycard);
        cameraCard = findViewById(R.id.cameracard);

        // Image selection listeners
        galleryCard.setOnClickListener(v -> pickImageFromGallery());
        cameraCard.setOnClickListener(v -> captureImageFromCamera());

        // Face recognition button
        recognizeButton.setOnClickListener(v -> processFaceRecognition());
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void captureImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = null;
            try {
                if (requestCode == PICK_IMAGE && data.getData() != null) {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                } else if (requestCode == CAPTURE_IMAGE && data.getExtras() != null) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                }

                if (bitmap != null) {
                    imageView2.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
            }
        }
    }

    private void processFaceRecognition() {
        imageView2.setDrawingCacheEnabled(true);
        Bitmap bitmap = imageView2.getDrawingCache();

        if (bitmap == null) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        recognitionResult.setText("No faces detected!");
                        return;
                    }

                    for (Face face : faces) {
                        Bitmap croppedFace = cropFace(bitmap, face.getBoundingBox());
                        float[] faceVector = convertBitmapToVector(croppedFace);
                        recognitionResult.setText("Face detected. Vector length: " + faceVector.length);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Face recognition failed", e));
    }

    private Bitmap cropFace(Bitmap original, Rect boundingBox) {
        return Bitmap.createBitmap(original, boundingBox.left, boundingBox.top, boundingBox.width(), boundingBox.height());
    }

    private float[] convertBitmapToVector(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float[] faceVector = new float[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            faceVector[i] = (pixels[i] & 0xFF) / 255.0f; // Normalize pixel values
        }
        return faceVector;
    }
}