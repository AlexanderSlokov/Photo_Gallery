package com.example.photogallery;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ImageDownloadWorker extends Worker {
    public ImageDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String imageUriString = getInputData().getString("IMAGE_URI");
        if (imageUriString == null) {
            return Result.failure();
        }

        Uri uri = Uri.parse(imageUriString);
        return downloadImage(uri) ? Result.success() : Result.failure();
    }

    private boolean downloadImage(Uri uri) {
        try {
            DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                return false;
            }

            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle("Downloading image")
                    .setDescription("Downloading " + uri.getLastPathSegment())
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());

            downloadManager.enqueue(request);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e("DownloadWorker", "Invalid URI or Download Manager not available", e);
            return false;
        }
    }
}

