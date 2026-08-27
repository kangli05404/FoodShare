package com.example.foodshare.util;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;

/** Small wrapper for unsigned Cloudinary image uploads. */
public final class CloudinaryUploader {

    private static final String UPLOAD_PRESET = "foodshare_unsigned";

    private CloudinaryUploader() {
    }

    public static void upload(Uri imageUri, UploadCallback callback) {
        MediaManager.get()
                .upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(callback)
                .dispatch();
    }
}
