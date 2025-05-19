package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ImageSourceDialog extends Dialog {
    private Context context;
    private LinearLayout cameraOption;
    private LinearLayout galleryOption;
    private LinearLayout deleteImageOption;
    private View.OnClickListener cameraClickListener;
    private View.OnClickListener galleryClickListener;
    private View.OnClickListener deleteImageClickListener;

    public ImageSourceDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.image_source_dialog);

        // Make the dialog background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize views
        cameraOption = findViewById(R.id.cameraOption);
        galleryOption = findViewById(R.id.galleryOption);
        deleteImageOption = findViewById(R.id.deleteUploadedImage);

        // Set click listeners
        if (cameraClickListener != null) {
            cameraOption.setOnClickListener(cameraClickListener);
        } else {
            cameraOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Camera option selected", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }

        if (galleryClickListener != null) {
            galleryOption.setOnClickListener(galleryClickListener);
        } else {
            galleryOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Gallery option selected", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }

        if (deleteImageClickListener != null) {
            deleteImageOption.setOnClickListener(deleteImageClickListener);
        } else {
            deleteImageOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Delete image option selected", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }

    public void setCameraClickListener(View.OnClickListener listener) {
        this.cameraClickListener = listener;

        // If the dialog is already showing, update the listener
        if (cameraOption != null) {
            cameraOption.setOnClickListener(listener);
        }
    }

    public void setGalleryClickListener(View.OnClickListener listener) {
        this.galleryClickListener = listener;

        // If the dialog is already showing, update the listener
        if (galleryOption != null) {
            galleryOption.setOnClickListener(listener);
        }
    }

    public void setDeleteImageClickListener(View.OnClickListener listener) {
        this.deleteImageClickListener = listener;

        // If the dialog is already showing, update the listener
        if (deleteImageOption != null) {
            deleteImageOption.setOnClickListener(listener);
        }
    }
}