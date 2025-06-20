package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;

public class TimerDialog extends Dialog {
    private Context context;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private Button setButton;
    private Button cancelButton;
    private OnTimeSetListener listener;
    private View associatedStepView;

    public interface OnTimeSetListener {
        void onTimeSet(int hours, int minutes, View stepView);
    }

    public TimerDialog(Context context, View stepView, OnTimeSetListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.associatedStepView = stepView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_timer);

        // Make the dialog window background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set up the hour picker (0-23 hours)
        hourPicker = findViewById(R.id.hourPicker);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);

        // Set up the minute picker (0-59 minutes)
        minutePicker = findViewById(R.id.minutePicker);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        // Check if there's an existing timer value
        ImageView timerIcon = associatedStepView.findViewById(R.id.timerIcon);
        if (timerIcon.getTag() != null) {
            try {
                // Get the total minutes from the tag
                int totalMinutes = (Integer) timerIcon.getTag();
                int hours = totalMinutes / 60;
                int minutes = totalMinutes % 60;

                // Set the pickers to the existing values
                hourPicker.setValue(hours);
                minutePicker.setValue(minutes);
            } catch (ClassCastException e) {
                // If tag is not an integer, set default values
                hourPicker.setValue(0);
                minutePicker.setValue(0);
            }
        } else {
            // Default values from preference
            // Load default timer from UserManager if available
            User user = UserManager.getInstance().getCurrentUser();
            if (user != null) {
                hourPicker.setValue(user.getDefaultTimerHours());
                minutePicker.setValue(user.getDefaultTimerMinutes());
            } else {
                hourPicker.setValue(0);
                minutePicker.setValue(0);
            }
        }

        setButton = findViewById(R.id.setTimerButton);
        cancelButton = findViewById(R.id.cancelTimerButton);

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int hours = hourPicker.getValue();
                    int minutes = minutePicker.getValue();
                    listener.onTimeSet(hours, minutes, associatedStepView);
                }
                dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }
}