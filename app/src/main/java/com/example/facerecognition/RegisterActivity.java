package com.example.facerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private EditText editTextName, editTextUSN;
    private Bitmap selectedBitmap, croppedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        imageView = findViewById(R.id.imageView);
        editTextName = findViewById(R.id.editTextName);
        editTextUSN = findViewById(R.id.editTextUSN);
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        Button buttonUploadData = findViewById(R.id.buttonUploadData);

        buttonSelectImage.setOnClickListener(v -> openGallery());
        buttonUploadData.setOnClickListener(v -> uploadDataToFirestore());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                detectAndCropFace(selectedBitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Image selection failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void detectAndCropFace(Bitmap bitmap) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        Rect bounds = face.getBoundingBox();

                        int left = Math.max(0, bounds.left);
                        int top = Math.max(0, bounds.top);
                        int width = Math.min(bitmap.getWidth() - left, bounds.width());
                        int height = Math.min(bitmap.getHeight() - top, bounds.height());

                        if (width > 0 && height > 0) {
                            croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);
                            imageView.setImageBitmap(croppedBitmap);
                        } else {
                            Toast.makeText(this, "Face cropping failed. Try another image", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "No face detected, try another image", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Face detection failed", Toast.LENGTH_SHORT).show());
    }

    private void uploadDataToFirestore() {
        String name = editTextName.getText().toString().trim();
        String usn = editTextUSN.getText().toString().trim();

        if (name.isEmpty() || usn.isEmpty() || croppedBitmap == null) {
            Toast.makeText(this, "Please fill all fields and select a valid face image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert Bitmap to Base64 string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // Save data to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("usn", usn);
        user.put("imageBase64", base64Image); // Store the image as a Base64 string

        db.collection("users").document(usn).set(user)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
