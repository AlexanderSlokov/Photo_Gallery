package com.example.photogallery;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.concurrent.Semaphore;


public class ImageUploadWorker extends Worker {
    public ImageUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String imageUriString = getInputData().getString("IMAGE_URI");
        if (imageUriString == null) {
            return Result.failure();
        }

        Uri fileUri = Uri.parse(imageUriString);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("uploaded_images");
        StorageReference fileRef = storageRef.child(fileUri.getLastPathSegment());

        // Tạo một semaphore để chờ upload hoàn thành
        final Semaphore semaphore = new Semaphore(0);

        fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> semaphore.release()).addOnFailureListener(e -> {
            Log.e("UploadWorker", "Upload failed", e);
            semaphore.release();
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure();
        }

        return Result.success();
    }
}
