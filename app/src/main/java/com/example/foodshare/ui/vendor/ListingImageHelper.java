package com.example.foodshare.ui.vendor;

import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.foodshare.R;

import java.io.File;
import java.io.IOException;

public class ListingImageHelper {
    private final AppCompatActivity activity;
    private final ImageView imageView;
    private Uri selectedImageUri;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher;
    private final ActivityResultLauncher<Uri> cameraLauncher;

    public ListingImageHelper(AppCompatActivity activity, ImageView imageView) {
        this.activity = activity;
        this.imageView = imageView;

        imagePickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                        Toast.makeText(activity, R.string.image_selected, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                captured -> {
                    if (captured && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        imageView.setImageURI(cameraImageUri);
                        Toast.makeText(activity, R.string.image_selected, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void showImageSourceDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_image_source)
                .setItems(new String[]{
                        activity.getString(R.string.choose_from_device),
                        activity.getString(R.string.take_photo)
                }, (dialog, which) -> {
                    if (which == 0) {
                        imagePickerLauncher.launch("image/*");
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile("foodshare_camera_", ".jpg", activity.getCacheDir());
            cameraImageUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    imageFile
            );
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException exception) {
            Toast.makeText(activity, R.string.camera_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    public Uri getSelectedImageUri() {
        return selectedImageUri;
    }

    public boolean hasSelectedImage() {
        return selectedImageUri != null;
    }
}