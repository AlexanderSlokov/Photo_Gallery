package com.example.photogallery;


import android.annotation.SuppressLint;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageAdapter imageAdapter;
    private final List<Uri> imageUris = new ArrayList<>();
    private final List<String> uploadImageUris = new ArrayList<>();

    private ActivityResultLauncher<String> mGetContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(this, imageUris);
        recyclerView.setAdapter(imageAdapter);

        // Setup the activity result launcher
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageUris.add(uri.toString());
                        scheduleBatchUploads(uploadImageUris, 10); // Ví dụ: batchSize là 5
                    }
                });

        // downloadAllButton listener
        Button downloadAllButton = findViewById(R.id.downloadAllButton);
        downloadAllButton.setOnClickListener(v -> {
            // Fetch and schedule downloads
            fetchImageUrisFromFirebase();
        });

        Button uploadButton = findViewById(R.id.UploadButton);
        uploadButton.setOnClickListener(v -> openImageSelector());

        // Gọi phương thức để tải ảnh
        loadImagesFromFirebase();
    }

    private void openImageSelector() {
        mGetContent.launch("image/*");
    }
    public void scheduleBatchUploads(@NotNull List<String> imageUris, int batchSize) {
        for (int i = 0; i < imageUris.size(); i += batchSize) {
            List<String> batch = imageUris.subList(i, Math.min(i + batchSize, imageUris.size()));
            for (String uri : batch) {
                Data data = new Data.Builder()
                        .putString("IMAGE_URI", uri)
                        .build();

                OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(ImageUploadWorker.class)
                        .setInputData(data)
                        .build();

                WorkManager.getInstance(getApplicationContext()).enqueue(uploadRequest);
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private void loadImagesFromFirebase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("images");

        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    // Xóa danh sách cũ trước khi thêm các hình ảnh mới
                    imageUris.clear();

                    for (StorageReference item : listResult.getItems()) {
                        // Lấy URL tải xuống và thêm vào danh sách
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUris.add(uri);
                            // Thông báo cho adapter rằng dữ liệu đã thay đổi để cập nhật giao diện
                            imageAdapter.notifyDataSetChanged();
                        }).addOnFailureListener(e -> {
                            // Xử lý lỗi khi tải URL thất bại
                            Log.e("Firebase", "Error getting download url", e);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi khi không thể liệt kê các tập tin
                    Log.e("Firebase", "Error listing images", e);
                });
    }
    public void scheduleImageDownloads(@NotNull List<String> imageUris, int batchSize) {
        for (int i = 0; i < imageUris.size(); i += batchSize) {
            List<String> batch = imageUris.subList(i, Math.min(i + batchSize, imageUris.size()));
            for (String uri : batch) {
                Data data = new Data.Builder()
                        .putString("IMAGE_URI", uri)
                        .build();

                OneTimeWorkRequest downloadRequest = new OneTimeWorkRequest.Builder(ImageDownloadWorker.class)
                        .setInputData(data)
                        .build();

                WorkManager.getInstance(getApplicationContext()).enqueue(downloadRequest);
            }
        }
    }
    private void fetchImageUrisFromFirebase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("images");

        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    List<String> uris = new ArrayList<>();
                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            uris.add(uri.toString());
                            // Once all URIs are collected, you can call your scheduling method
                            if (uris.size() == listResult.getItems().size()) {
                                scheduleImageDownloads(uris, 10);
                            }
                        }).addOnFailureListener(e -> Log.e("Firebase", "Error getting download URL", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error listing images", e));
    }
}
