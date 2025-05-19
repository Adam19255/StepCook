package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

/**
 * Helper class to manage UI-related operations
 */
public class UIHelper {
    private Context context;
    private LayoutInflater inflater;

    public UIHelper(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Change the color of a filter button based on its state
     */
    public void changeButtonColor(boolean isActive, Button filterButton) {
        if (isActive) {
            filterButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.orange)));
            filterButton.setTextColor(ContextCompat.getColor(
                    context, android.R.color.white));
        } else {
            filterButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.gray)));
            filterButton.setTextColor(ContextCompat.getColor(
                    context, R.color.black));
        }
    }

    /**
     * Highlight the selected filter option in the dialog
     */
    public void highlightSelectedOption(LinearLayout allLayout, LinearLayout option1Layout,
                                        LinearLayout option2Layout, LinearLayout option3Layout,
                                        String selectedOption, String[] options) {
        // Reset all backgrounds first
        allLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        option1Layout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        option2Layout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        option3Layout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

        // Determine which layout to highlight
        LinearLayout selectedLayout = null;

        if (selectedOption.equals(options[0])) {
            selectedLayout = allLayout;
        } else if (selectedOption.equals(options[1])) {
            selectedLayout = option1Layout;
        } else if (selectedOption.equals(options[2])) {
            selectedLayout = option2Layout;
        } else if (selectedOption.equals(options[3])) {
            selectedLayout = option3Layout;
        }

        // Apply highlight if found
        if (selectedLayout != null) {
            int highlightColor = ContextCompat.getColor(context, R.color.orange);
            highlightColor = Color.argb(50, Color.red(highlightColor),
                    Color.green(highlightColor),
                    Color.blue(highlightColor));
            selectedLayout.setBackgroundColor(highlightColor);
        }
    }

    /**
     * Add a new step to the recipe
     */
    public View addNewStep(LinearLayout stepsContainer, View.OnClickListener removeStepListener) {
        View stepView = inflater.inflate(R.layout.step_field, stepsContainer, false);

        // Calculate the step number (count existing step fields + 1)
        int stepNumber = getStepCount(stepsContainer) + 1;

        // Set the hint with step number
        EditText stepDetail = stepView.findViewById(R.id.stepDetail);
        stepDetail.setHint("Step " + stepNumber + ": " + context.getString(R.string.step_description));

        // Store the step number as a tag
        stepView.setTag("step_" + stepNumber);

        // Add listener for remove button
        ImageView removeButton = stepView.findViewById(R.id.removeStepButton);
        removeButton.setOnClickListener(removeStepListener);

        stepsContainer.addView(stepView);
        return stepView;
    }

    /**
     * Validate all steps to ensure they have content
     * @param stepsContainer The container holding all step views
     * @return true if all steps are valid, false otherwise
     */
    public boolean validateSteps(LinearLayout stepsContainer) {
        boolean allValid = true;
        boolean atLeastOneStep = false;

        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View stepView = stepsContainer.getChildAt(i);

            // Skip if not a step view
            if (stepView.getTag() == null || !stepView.getTag().toString().startsWith("step_")) {
                continue;
            }

            atLeastOneStep = true;
            EditText stepDetail = stepView.findViewById(R.id.stepDetail);
            String content = stepDetail.getText().toString().trim();

            if (content.isEmpty()) {
                stepDetail.setError("Please add step description or remove this step");
                allValid = false;
            } else {
                stepDetail.setError(null);
            }
        }

        if (!atLeastOneStep) {
            Toast.makeText(context, "Please add at least one step", Toast.LENGTH_SHORT).show();
            return false;
        }

        return allValid;
    }

    /**
     * Count the actual step views (not headers or other views)
     */
    public int getStepCount(LinearLayout stepsContainer) {
        int count = 0;
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View child = stepsContainer.getChildAt(i);
            if (child.getTag() != null && child.getTag().toString().startsWith("step_")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Update step numbers after removing a step
     */
    public void updateStepNumbers(LinearLayout stepsContainer) {
        int stepCount = 0;

        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View child = stepsContainer.getChildAt(i);

            if (child.getTag() != null && child.getTag().toString().startsWith("step_")) {
                stepCount++;
                child.setTag("step_" + stepCount);

                // Update the hint text
                EditText stepDetail = child.findViewById(R.id.stepDetail);
                stepDetail.setHint("Step " + stepCount + ": " + context.getString(R.string.step_description));
            }
        }
    }

    /**
     * Calculate the total time from all step timers
     */
    public int calculateTotalTime(LinearLayout stepsContainer) {
        int totalMinutes = 0;

        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View stepView = stepsContainer.getChildAt(i);

            // Find timer container or icon
            LinearLayout timerContainer = stepView.findViewWithTag("timerContainer");
            if (timerContainer != null) {
                ImageView timerIcon = timerContainer.findViewById(R.id.timerIcon);
                if (timerIcon != null && timerIcon.getTag() != null) {
                    totalMinutes += (int) timerIcon.getTag();
                }
            } else {
                ImageView timerIcon = stepView.findViewById(R.id.timerIcon);
                if (timerIcon != null && timerIcon.getTag() != null) {
                    totalMinutes += (int) timerIcon.getTag();
                }
            }
        }

        return totalMinutes;
    }

    /**
     * Format total minutes into a readable string with hours and minutes
     */
    public String formatTotalTime(int totalMinutes) {
        // Calculate days
        int days = totalMinutes / (24 * 60);

        // Calculate remaining hours after removing days
        int remainingMinutes = totalMinutes % (24 * 60);
        int hours = remainingMinutes / 60;

        // Calculate remaining minutes
        int minutes = remainingMinutes % 60;

        return formatTime(days, hours, minutes);
    }

    /**
     * Format time components into a readable string
     */
    public String formatTime(int days, int hours, int minutes) {
        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d ");
        }

        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }

        if (minutes > 0 || (hours == 0 && days == 0)) {
            // Show minutes if there are any or if both hours and days are 0
            sb.append(minutes).append("m");
        }

        return sb.toString().trim();
    }

    /**
     * Update the total cooking time displayed
     */
    public void updateTotalCookTime(LinearLayout stepsContainer, TextView cookTimeText) {
        int totalMinutes = calculateTotalTime(stepsContainer);
        String formattedTime = formatTotalTime(totalMinutes);
        cookTimeText.setText(context.getString(R.string.cook_time_format, formattedTime));
    }

    /**
     * Add or update time label for a step
     */
    public void addTimeLabel(View stepView, String timeText) {
        // Check if time label already exists
        TextView timeLabel = stepView.findViewWithTag("timeLabel");
        ImageView timerIcon = stepView.findViewById(R.id.timerIcon);

        if (timeLabel == null) {
            // Create a new time label
            timeLabel = new TextView(context);
            timeLabel.setTag("timeLabel");
            timeLabel.setGravity(android.view.Gravity.CENTER);

            // Create a vertical container for the timer icon and label if it doesn't exist
            LinearLayout timerContainer = new LinearLayout(context);
            timerContainer.setOrientation(LinearLayout.VERTICAL);
            timerContainer.setGravity(android.view.Gravity.CENTER);
            timerContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            timerContainer.setTag("timerContainer");

            // Set the timer icon to match parent width so it centers properly
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            iconParams.gravity = android.view.Gravity.CENTER;

            // Save the original parent and position
            LinearLayout originalParent = (LinearLayout) timerIcon.getParent();
            int originalIndex = originalParent.indexOfChild(timerIcon);

            // Remove the timer icon from its current position
            originalParent.removeView(timerIcon);

            // Add the timer icon to the container with proper layout
            timerIcon.setLayoutParams(iconParams);
            timerContainer.addView(timerIcon);

            // Add the label to the container below the timer icon
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            timeLabel.setLayoutParams(labelParams);
            timerContainer.addView(timeLabel);

            // Add the container to the original parent at the original position
            originalParent.addView(timerContainer, originalIndex);
        }

        // Set or update the time text
        timeLabel.setText(timeText);
        timeLabel.setTextColor(ContextCompat.getColor(context, R.color.orange));
        timeLabel.setTextSize(12);
    }

    /**
     * Clear the timer from a step
     * @param stepView The step view containing the timer
     * @param timerClickListener The click listener to set on the timer icon after clearing
     */
    public void clearTimer(View stepView, View.OnClickListener timerClickListener) {
        // Find the timer container
        LinearLayout timerContainer = (LinearLayout) stepView.findViewWithTag("timerContainer");

        if (timerContainer != null) {
            // Get the parent layout
            LinearLayout parentLayout = (LinearLayout) timerContainer.getParent();
            int containerIndex = parentLayout.indexOfChild(timerContainer);

            // Find the timer icon inside the container
            ImageView timerIcon = timerContainer.findViewById(R.id.timerIcon);

            // Reset timer icon properties
            timerIcon.setTag(null);
            timerIcon.setImageResource(R.drawable.timer);

            // Remove the timer icon from the container
            timerContainer.removeView(timerIcon);

            // Remove the container from the parent
            parentLayout.removeView(timerContainer);

            // Add the timer icon back to the parent at the same position
            parentLayout.addView(timerIcon, containerIndex);

            // Set the provided click listener
            if (timerClickListener != null) {
                timerIcon.setOnClickListener(timerClickListener);
            }
        }
    }
}