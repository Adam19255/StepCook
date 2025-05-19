package com.adams_maxims_evyatarc.stepcook;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Class to manage dialogs in the application with simplified camera handling
 */
public class DialogManager {
    private Context context;
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_PICK_IMAGE = 2;
    public static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final int REQUEST_STORAGE_PERMISSION = 101;

    private ImageDeleteListener imageDeleteListener;

    public DialogManager(Context context) {
        this.context = context;
    }

    public interface ImageDeleteListener {
        void onImageDeleted();
    }

    public void setImageDeleteListener(ImageDeleteListener listener) {
        this.imageDeleteListener = listener;
    }

    public void showEditProfileDialog(){
        EditProfileDialog dialog = new EditProfileDialog(context);
        dialog.show();
    }

    public void showAboutDialog() {
        AboutUsDialog dialog = new AboutUsDialog(context);
        dialog.show();
    }

    public void showImageSourceDialog(){
        ImageSourceDialog dialog = new ImageSourceDialog(context);

        // Set callbacks for camera and gallery options
        dialog.setCameraClickListener(v -> {
            dialog.dismiss();
            requestCameraPermission();
        });

        dialog.setGalleryClickListener(v -> {
            dialog.dismiss();
            requestStoragePermission();
        });

        dialog.setDeleteImageClickListener(v -> {
            dialog.dismiss();
            if (imageDeleteListener != null) {
                imageDeleteListener.onImageDeleted();
            }
        });

        dialog.show();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openGallery();
        }
    }

    public void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Just launch the camera directly without file creation
            ((Activity) context).startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public void openGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        ((Activity) context).startActivityForResult(pickPhoto, REQUEST_PICK_IMAGE);
    }

    /**
     * Show confirmation dialog before exiting the app
     */
    public void showExitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Exit App");
        builder.setMessage("Are you sure you want to exit?");

        // Add the buttons
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked Yes button
                if (context instanceof AppCompatActivity) {
                    ((AppCompatActivity) context).finish(); // Close the app
                }
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}