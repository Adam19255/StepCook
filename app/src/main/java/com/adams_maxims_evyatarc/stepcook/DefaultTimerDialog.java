package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DefaultTimerDialog extends Dialog {
    private Context context;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private Button setButton;
    private Button cancelButton;

    public DefaultTimerDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.default_timer); // or your renamed layout

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        hourPicker = findViewById(R.id.hourPicker);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);

        minutePicker = findViewById(R.id.minutePicker);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        User user = UserManager.getInstance().getCurrentUser();
        if (user != null) {
            hourPicker.setValue(user.getDefaultTimerHours());
            minutePicker.setValue(user.getDefaultTimerMinutes());
        } else {
            // Fallback values if user isn't loaded
            hourPicker.setValue(0);
            minutePicker.setValue(5);
        }

        setButton = findViewById(R.id.setTimerButton);
        cancelButton = findViewById(R.id.cancelTimerButton);

        setButton.setOnClickListener(v -> {
            int hours = hourPicker.getValue();
            int minutes = minutePicker.getValue();

            UserManager manager = UserManager.getInstance();
            manager.updateUserPreference("defaultTimerHours", hours, new UserManager.UserOperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d("DefaultTimerDialog", "Hours saved");
                }

                @Override
                public void onError(Exception e) {
                    Log.e("DefaultTimerDialog", "Failed to save hours", e);
                }
            });

            manager.updateUserPreference("defaultTimerMinutes", minutes, new UserManager.UserOperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d("DefaultTimerDialog", "Minutes saved");
                }

                @Override
                public void onError(Exception e) {
                    Log.e("DefaultTimerDialog", "Failed to save minutes", e);
                }
            });

            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }
}
