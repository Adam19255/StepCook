package com.adams_maxims_evyatarc.stepcook;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

public class AboutUsDialog {

    private Context context;
    private AlertDialog dialog;

    public AboutUsDialog(Context context) {
        this.context = context;
    }

    public void show() {
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);

        // Inflate the custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.about_us_dialog_layout, null);
        builder.setView(dialogView);

        // Get references to TextView fields
        ImageView closeButton = dialogView.findViewById(R.id.closeButton);
        TextView appIdTextView = dialogView.findViewById(R.id.app_id);
        TextView appVersionTextView = dialogView.findViewById(R.id.app_version);
        TextView osInfoTextView = dialogView.findViewById(R.id.os_info);
        AppCompatButton okButton = dialogView.findViewById(R.id.ok_button);

        // Get application package name and version
        String packageName = context.getPackageName();
        String versionName = "1.0";

        // Try to get the actual version name
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AboutUsDialog", "Package name not found", e);
        }

        // Get OS information
        String osInfo = "Android " + Build.VERSION.RELEASE + " (API level " + Build.VERSION.SDK_INT + ")";

        // Set values to TextViews
        appIdTextView.setText(context.getString(R.string.application_id_format, packageName));
        appVersionTextView.setText(context.getString(R.string.version_format, versionName));
        osInfoTextView.setText(context.getString(R.string.os_info_format, osInfo));

        // Create and show the dialog
        dialog = builder.create();

        // Set window animation and remove default background
        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}