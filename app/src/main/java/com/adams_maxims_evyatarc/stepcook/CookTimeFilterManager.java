package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Class to manage cook time filter functionality
 */
public class CookTimeFilterManager extends FilterManager {
    private Context context;
    private String selectedCookTime = "All"; // Default option
    private final String[] COOK_TIME_OPTIONS = {"All", "Fast", "Medium", "Long"};

    public CookTimeFilterManager(Context context, Button cookTimeFilterButton) {
        super(cookTimeFilterButton, new UIHelper(context));
        this.context = context;
    }

    @Override
    public void showFilterDialog() {
        uiHelper.changeButtonColor(true, filterButton);

        // If we already have a dialog, just show it if not showing
        if (filterDialog != null) {
            if (!filterDialog.isShowing()) {
                filterDialog.show();
            }
            return;
        }

        // Create a new dialog if we don't have one
        filterDialog = new Dialog(context);
        filterDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        filterDialog.setContentView(R.layout.time_filter_layout);

        // Set up dialog window properties
        Window window = filterDialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.BOTTOM);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Find UI elements
        LinearLayout allCookTimes = filterDialog.findViewById(R.id.allCookTimes);
        LinearLayout fastCookTime = filterDialog.findViewById(R.id.fastCookTime);
        LinearLayout mediumCookTime = filterDialog.findViewById(R.id.mediumCookTime);
        LinearLayout longCookTime = filterDialog.findViewById(R.id.longCookTime);
        ImageView closeDialog = filterDialog.findViewById(R.id.closeDialog);
        ImageView closeButton = filterDialog.findViewById(R.id.closeButton);

        // Highlight the currently selected cook time
        uiHelper.highlightSelectedOption(allCookTimes, fastCookTime, mediumCookTime,
                longCookTime, selectedCookTime, COOK_TIME_OPTIONS);

        setupClickListeners(allCookTimes, fastCookTime, mediumCookTime, longCookTime,
                closeDialog, closeButton);

        filterDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Reset button color when dialog is dismissed by clicking outside
                uiHelper.changeButtonColor(false, filterButton);
            }
        });

        filterDialog.show();
    }

    private void setupClickListeners(LinearLayout allCookTimes, LinearLayout fastCookTime,
                                     LinearLayout mediumCookTime, LinearLayout longCookTime,
                                     ImageView closeDialog, ImageView closeButton) {
        allCookTimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[0]; // "All"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
                Toast.makeText(context, "All cook times selected", Toast.LENGTH_SHORT).show();
            }
        });

        fastCookTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[1]; // "Fast"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
                Toast.makeText(context, "Fast cook time selected", Toast.LENGTH_SHORT).show();
            }
        });

        mediumCookTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[2]; // "Medium"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
                Toast.makeText(context, "Medium cook time selected", Toast.LENGTH_SHORT).show();
            }
        });

        longCookTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[3]; // "Long"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
                Toast.makeText(context, "Long cook time selected", Toast.LENGTH_SHORT).show();
            }
        });

        closeDialog.setOnClickListener(v -> closeDialog());
        closeButton.setOnClickListener(v -> closeDialog());
    }

    private void handleCookTimeSelection(LinearLayout allCookTimes, LinearLayout fastCookTime,
                                         LinearLayout mediumCookTime, LinearLayout longCookTime) {
        // Update the highlight
        uiHelper.highlightSelectedOption(allCookTimes, fastCookTime, mediumCookTime,
                longCookTime, selectedCookTime, COOK_TIME_OPTIONS);

        // Apply the filter
        applyFilter(selectedCookTime);

        // Close dialog and reset button
        closeDialog();
    }

    @Override
    public void applyFilter(String filterValue) {
        // Implement actual filtering logic
    }
}