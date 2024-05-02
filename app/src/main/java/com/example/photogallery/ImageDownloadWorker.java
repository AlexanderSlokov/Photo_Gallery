package com.example.photogallery;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.jetbrains.annotations.NotNull;

public class ImageDownloadWorker extends Worker {
    public ImageDownloadWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String imageUriString = getInputData().getString("IMAGE_URI");
        if (imageUriString != null) {
            return downloadImage(Uri.parse(imageUriString)) ? Result.success() : Result.failure();
        }
        return Result.failure();
    }

    private boolean downloadImage(Uri uri) {
        // Sử dụng DownloadManager để tải ảnh
        DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("Downloading image")
                .setDescription("Downloading " + uri.getLastPathSegment())
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());

        downloadManager.enqueue(request);
        return true;
    }

}

